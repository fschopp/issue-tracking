package net.florianschoppmann;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import net.florianschoppmann.youtrack.Attachments;
import net.florianschoppmann.youtrack.Attachments.Attachment;
import net.florianschoppmann.youtrack.TagsForIssues;
import net.florianschoppmann.youtrack.YouTrackClient;
import net.florianschoppmann.youtrack.rest.Issue;
import net.florianschoppmann.youtrack.rest.IssueTag;
import net.florianschoppmann.youtrack.rest.UserGroup;
import net.florianschoppmann.youtrack.restold.Error;
import net.florianschoppmann.youtrack.restold.ImportReport;
import net.florianschoppmann.youtrack.restold.ImportReport.Item;
import net.florianschoppmann.youtrack.restold.Issues;
import net.florianschoppmann.youtrack.restold.Search;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.moxy.json.MoxyJsonConfig;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.xml.bind.JAXBException;

public class YouTrackImport {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Serialization serialization;
    private final YouTrackClient youTrackClient;
    private final Path attachmentBasePath;
    private final boolean dryRun;

    private YouTrackImport(Serialization serialization, YouTrackClient youTrackClient, Path attachmentBasePath,
            boolean dryRun) {
        this.serialization = serialization;
        this.youTrackClient = youTrackClient;
        this.attachmentBasePath = attachmentBasePath;
        this.dryRun = dryRun;
    }

    private static void ensureSuccessful(String kind, ImportReport importReport) throws ImportException {
        if (!importReport.getItem().stream().allMatch(Item::isImported)) {
            throw new ImportException(kind, importReport);
        }
    }

    private void addMissingTags(String projectAbbrev, TagsForIssues tagsForIssues) {
        // Gather all YouTrack tags that are available to us (the current user).
        SortedMap<String, IssueTag> availableTags = youTrackClient.getAllAvailableTags();

        // Gather all tags that are missing and need to be created
        SortedSet<String> requiredTags = tagsForIssues.getIssues().stream()
            .flatMap(issue -> issue.getTags().stream().map(IssueTag::getName))
            .collect(Collectors.toCollection(TreeSet::new));
        requiredTags.removeAll(availableTags.keySet());

        // Finally, create the tags. The YouTrackClient needs the project team name, so we need to obtain that, too.
        List<UserGroup> userGroups = youTrackClient.getUserGroups("id,name,allUsersGroup,teamForProject(id,shortName)");
        Optional<UserGroup> projectTeam = userGroups.stream()
            .filter(userGroup -> userGroup.getTeamForProject() != null)
            .filter(userGroup -> projectAbbrev.equals(userGroup.getTeamForProject().getShortName()))
            .findAny();
        if (projectTeam.isEmpty()) {
            throw new IllegalStateException(String.format("Could not find team for project %s.", projectAbbrev));
        }
        String projectTeamName = projectTeam.get().getName();
        for (String requiredTag : requiredTags) {
            Search tag = new Search();
            tag.setName(requiredTag);
            tag.setVisibleForGroup(projectTeamName);
            tag.setUpdatableByGroup(projectTeamName);
            Error error = youTrackClient.addTag(tag);
            if (error.getValue() != null) {
                throw new IllegalStateException(
                    String.format("Could not add tag '%s'. YouTrack reported: %s", requiredTag, error.getValue()));
            }
        }
    }

    private void run() throws JAXBException {
        if (dryRun) {
            log.info("Dry run. Import will not be saved...");
        }

        ExportSettings exportSettings = serialization.readResult(ExportSettings.class);
        String projectAbbrev = exportSettings.getYouTrackProjectAbbrev();
        Issues issues = serialization.readResult(Issues.class);
        net.florianschoppmann.youtrack.restold.List links = serialization.readResult(net.florianschoppmann.youtrack.restold.List.class);
        Attachments attachments = serialization.readResult(Attachments.class);
        TagsForIssues tagsForIssues = serialization.readResult(TagsForIssues.class);

        // Note that addMissingTags() also calls youTrackClient.getAllAvailableTags() initially. However, we need to
        // call again in order to retrieve the meta data of the tags created in addMissingTags().
        addMissingTags(projectAbbrev, tagsForIssues);
        SortedMap<String, IssueTag> availableTags = youTrackClient.getAllAvailableTags();

        try {
            ensureSuccessful("issues", youTrackClient.importIssues(issues, projectAbbrev, dryRun));
            ensureSuccessful("links", youTrackClient.importLinks(links, dryRun));
            for (Attachment attachment : attachments.getAttachment()) {
                ensureSuccessful("attachment",
                    youTrackClient.importAttachment(attachment, projectAbbrev, attachmentBasePath, dryRun));
            }
        } catch (ImportException exception) {
            log.error("{} Check the file size, and see import report.", exception.getMessage());
            serialization.writeResult(exception.importReport);
            return;
        }

        for (Issue issue : tagsForIssues.getIssues()) {
            // Fill in the tag IDs. They were obviously not known before.
            for (IssueTag tag : issue.getTags()) {
                tag.setId(availableTags.get(tag.getName()).getId());
            }
            Issue updatedIssue = youTrackClient.updateIssue(issue);
            if (updatedIssue.getTags().size() != issue.getTags().size()) {
                log.error("Failed to import tags for issue {}.", issue.getId());
                return;
            }
        }
    }

    private static void start(URI baseUri, Path attachmentBasePath, String youTrackAccessToken, boolean dryRun)
            throws JAXBException {
        // We need to keep a reference to julLogger, because it could be garbage collected otherwise
        // We want to make it possible to log JAX-RS traffic by setting the slf4j log level to debug or higher
        var julLogger = java.util.logging.Logger.getLogger(LoggingFeature.class.getPackageName());
        julLogger.setLevel(Level.ALL);
        SLF4JBridgeHandler.install();

        MoxyJsonConfig moxyJsonConfig = new MoxyJsonConfig()
            .setIncludeRoot(false);
        Client jaxrsClient = ClientBuilder.newClient(
            new ClientConfig()
                .property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true)
                .register(MultiPartFeature.class)
                .register(new MoxyXmlFeature(
                    Issues.class, List.class, ImportReport.class)))
                .register(MoxyJsonFeature.class)
                .register(moxyJsonConfig.resolver())
                .register(LoggingFeature.class)
                .property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_CLIENT, LoggingFeature.Verbosity.PAYLOAD_ANY)
                .property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_CLIENT, Level.FINE.getName());
        Serialization serialization = Serialization.defaultSerialization(attachmentBasePath);
        var youTrackClient = new YouTrackClient(jaxrsClient, baseUri, youTrackAccessToken);
        var youTrackImport = new YouTrackImport(serialization, youTrackClient, attachmentBasePath, dryRun);

        youTrackImport.run();
    }

    private static URI uriFromString(String string) {
        return URI.create(string.endsWith("/")
            ? string
            : (string + '/')
        );
    }

    public static void main(String[] args) throws IOException, JAXBException {
        final Logger log = LoggerFactory.getLogger(YouTrackImport.class);

        OptionParser parser = new OptionParser();
        OptionSpec<Void> helpOption = parser.accepts("help").forHelp();
        OptionSpec<String> instanceUrlOpt = parser
            .accepts("url",
                "URL of the YouTrack instance (of form https://<name>.myjetbrains.com/youtrack for InCloud instances)")
            .withRequiredArg().required();
        OptionSpec<Path> inputOpt = parser.accepts("io", "path where to read input and write output")
            .withRequiredArg().withValuesConvertedBy(new PathConverter()).required();
        OptionSpec<Void> dryRun = parser
            .accepts("dry-run", "if given, nothing is saved, but imported data is validated by YouTrack");
        OptionSet options = parser.parse(args);
        if (options.has(helpOption)) {
            parser.printHelpOn(System.out);
            System.exit(0);
            return;
        }

        String youTrackAccessToken = System.getenv("YOUTRACK_ACCESS_TOKEN");
        if (youTrackAccessToken == null) {
            log.error("Environment variable YOUTRACK_ACCESS_TOKEN must be defined and contain a valid access token "
                + "for YouTrack.");
            System.exit(1);
            return;
        }

        start(uriFromString(options.valueOf(instanceUrlOpt)),options.valueOf(inputOpt), youTrackAccessToken,
            options.has(dryRun));
    }

    private static final class ImportException extends Exception {
        private static final long serialVersionUID = 4848411093498437136L;

        private final ImportReport importReport;

        private ImportException(String kind, ImportReport importReport) {
            super(String.format("Failed to import %s.", kind));
            this.importReport = importReport;
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            throw new NotSerializableException(getClass().getName());
        }
    }
}
