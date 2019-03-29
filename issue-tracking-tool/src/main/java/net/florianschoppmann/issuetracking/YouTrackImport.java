package net.florianschoppmann.issuetracking;

import static java.util.Collections.singletonMap;
import static net.florianschoppmann.issuetracking.util.StringNode.node;
import static net.florianschoppmann.issuetracking.util.StringNode.nodeOfStrings;
import static net.florianschoppmann.issuetracking.util.StringNode.rootOfNodes;
import static net.florianschoppmann.issuetracking.util.StringNode.rootOfStrings;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import net.florianschoppmann.issuetracking.util.StringNode;
import net.florianschoppmann.issuetracking.youtrack.Attachments;
import net.florianschoppmann.issuetracking.youtrack.Attachments.Attachment;
import net.florianschoppmann.issuetracking.youtrack.CommentUpdates;
import net.florianschoppmann.issuetracking.youtrack.CommentUpdates.CommentUpdate;
import net.florianschoppmann.issuetracking.youtrack.IssueUpdates;
import net.florianschoppmann.issuetracking.youtrack.IssueUpdates.IssueUpdate;
import net.florianschoppmann.issuetracking.youtrack.YouTrackClient;
import net.florianschoppmann.issuetracking.youtrack.rest.Issue;
import net.florianschoppmann.issuetracking.youtrack.rest.IssueComment;
import net.florianschoppmann.issuetracking.youtrack.rest.IssueTag;
import net.florianschoppmann.issuetracking.youtrack.rest.UserGroup;
import net.florianschoppmann.issuetracking.youtrack.restold.Error;
import net.florianschoppmann.issuetracking.youtrack.restold.ImportReport;
import net.florianschoppmann.issuetracking.youtrack.restold.ImportReport.Item;
import net.florianschoppmann.issuetracking.youtrack.restold.Issues;
import net.florianschoppmann.issuetracking.youtrack.restold.Search;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.eclipse.persistence.jaxb.UnmarshallerProperties;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.xml.bind.JAXBException;

public final class YouTrackImport {
    private static final StringNode REQUESTED_FIELDS_FOR_ISSUES
        = rootOfStrings("id", "description", "numberInProject", "summary", "tags");
    private static final StringNode REQUESTED_FIELDS_FOR_COMMENTS = rootOfStrings("id", "text");

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

    private static void ensureSuccessful(String kind, ImportReport importReport) {
        if (!importReport.getItem().stream().allMatch(Item::isImported)) {
            throw new ImportException(kind, importReport);
        }
    }

    private void addMissingTags(String projectAbbrev, IssueUpdates issueUpdates) {
        // Gather all YouTrack tags that are available to us (the current user).
        SortedMap<String, IssueTag> availableTags = youTrackClient.getAllAvailableTags();

        // Gather all tags that are missing and need to be created
        SortedSet<String> requiredTags = issueUpdates.issueUpdates.stream()
            .map(issueUpdate -> issueUpdate.issue)
            .filter(Objects::nonNull)
            .map(issue -> issue.tags)
            .filter(Objects::nonNull)
            .flatMap(tags -> tags.stream().map(tag -> tag.name))
            .collect(Collectors.toCollection(TreeSet::new));
        requiredTags.removeAll(availableTags.keySet());

        // Finally, create the tags. The YouTrackClient needs the project team name, so we need to obtain that, too.
        StringNode fields = rootOfNodes(node("name"), nodeOfStrings("teamForProject", "shortName"));
        List<UserGroup> userGroups = youTrackClient.getUserGroups(fields);
        Optional<UserGroup> projectTeam = userGroups.stream()
            .filter(userGroup -> userGroup.teamForProject != null)
            .filter(userGroup -> projectAbbrev.equals(userGroup.teamForProject.shortName))
            .findAny();
        if (projectTeam.isEmpty()) {
            throw new IllegalStateException(String.format("Could not find team for project %s.", projectAbbrev));
        }
        String projectTeamName = projectTeam.get().name;
        for (String requiredTag : requiredTags) {
            Search tag = new Search();
            tag.name = requiredTag;
            tag.visibleForGroup = projectTeamName;
            tag.updatableByGroup = projectTeamName;
            Error error = youTrackClient.addTag(tag);
            if (error.value != null) {
                throw new IllegalStateException(
                    String.format("Could not add tag '%s'. YouTrack reported: %s", requiredTag, error.value));
            }
        }
    }

    private <T> Optional<T> read(ImportSettings importSettings, Function<ImportSettings, Boolean> flag,
            Class<T> clazz) throws JAXBException {
        return flag.apply(importSettings)
            ? Optional.of(serialization.readResultXml(clazz))
            : Optional.empty();
    }

    private void run() throws JAXBException {
        if (dryRun) {
            log.info("Dry run. Import will not be saved...");
        }

        ImportSettings importSettings = serialization.readResultXml(ImportSettings.class);
        String projectAbbrev = importSettings.youTrackProjectAbbrev;
        Optional<Issues> issuesOptional = read(importSettings, settings -> settings.importIssues, Issues.class);
        Optional<net.florianschoppmann.issuetracking.youtrack.restold.List> linksOptional = read(importSettings,
            settings -> settings.importLinks, net.florianschoppmann.issuetracking.youtrack.restold.List.class);
        Optional<Attachments> attachments
            = read(importSettings, settings -> settings.importAttachments, Attachments.class);
        Optional<IssueUpdates> issueUpdatesOptional
            = read(importSettings, settings -> settings.updateIssues, IssueUpdates.class);
        Optional<CommentUpdates> commentUpdatesOptional
            = read(importSettings, settings -> settings.updateComments, CommentUpdates.class);

        // Note that addMissingTags() also calls youTrackClient.getAllAvailableTags() initially. However, we need to
        // call again in order to retrieve the meta data of the tags created in addMissingTags().
        issueUpdatesOptional.ifPresent(tagsForIssues -> addMissingTags(projectAbbrev, tagsForIssues));
        SortedMap<String, IssueTag> availableTags = youTrackClient.getAllAvailableTags();

        try {
            issuesOptional.ifPresent(issues ->
                ensureSuccessful("issues", youTrackClient.importIssues(issues, projectAbbrev, dryRun)));
            linksOptional.ifPresent(links ->
                 ensureSuccessful("links", youTrackClient.importLinks(links, dryRun)));
            for (Attachment attachment : attachments.orElseGet(Attachments::new).attachments) {
                ensureSuccessful("attachment",
                    youTrackClient.importAttachment(attachment, projectAbbrev, attachmentBasePath, dryRun));
            }
        } catch (ImportException exception) {
            log.error("{} Check the file size, and see import report.", exception.getMessage());
            serialization.writeResultXml(exception.importReport);
            return;
        }

        for (IssueUpdate issueUpdate : issueUpdatesOptional.orElseGet(IssueUpdates::new).issueUpdates) {
            @Nullable Issue issue = issueUpdate.issue;
            if (issue == null) {
                continue;
            }

            // Fill in the tag IDs. They were obviously not known before.
            for (IssueTag tag : Objects.requireNonNullElse(issue.tags, Collections.<IssueTag>emptySet())) {
                tag.id = availableTags.get(tag.name).id;
            }
            Issue updatedIssue
                = youTrackClient.updateIssue(issueUpdate.issueKey, issue, REQUESTED_FIELDS_FOR_ISSUES);
            if (issue.tags != null && (updatedIssue.tags == null || updatedIssue.tags.size() != issue.tags.size())) {
                log.error("Failed to update tags for issue {}.", issue.id);
                return;
            }
            if (issue.description != null
                    && (updatedIssue.description == null || !updatedIssue.description.equals(issue.description))) {
                log.error("Failed to update description for issue {}. Expected:\n{}\n\nActual:\n{}.",
                    issue.id, issue.description, updatedIssue.description);
            }
        }
        for (CommentUpdate commentUpdate : commentUpdatesOptional.orElseGet(CommentUpdates::new).commentUpdates) {
            @Nullable IssueComment issueComment = commentUpdate.issueComment;
            if (issueComment == null) {
                continue;
            }

            IssueComment updatedComment = youTrackClient.updateIssueComment(commentUpdate.issueKey,
                commentUpdate.commentId, commentUpdate.issueComment, REQUESTED_FIELDS_FOR_COMMENTS);
            if (issueComment.text != null && (updatedComment.text == null
                    || !updatedComment.text.equals(issueComment.text))) {
                log.error("Failed to update text for comment {} / {}. Expected:\n{}\n\nActual:\n{}.",
                    commentUpdate.issueKey, commentUpdate.commentId, issueComment.text, updatedComment.text);
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
            .setIncludeRoot(false)
            .setMarshallerProperties(singletonMap(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true))
            .setUnmarshallerProperties(singletonMap(UnmarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true));
        Client jaxrsClient = ClientBuilder.newClient(
            new ClientConfig()
                .property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true)
                .register(MultiPartFeature.class)
                .register(new MoxyXmlFeature(
                    Issues.class, net.florianschoppmann.issuetracking.youtrack.restold.List.class, ImportReport.class))
                .register(MoxyJsonFeature.class)
                .register(moxyJsonConfig.resolver())
                .register(LoggingFeature.class)
                .property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_CLIENT, LoggingFeature.Verbosity.PAYLOAD_ANY)
                .property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_CLIENT, Level.FINE.getName())
        );
        Serialization serialization = Serialization.defaultSerialization(attachmentBasePath);
        var youTrackClient = new YouTrackClient(jaxrsClient, baseUri, youTrackAccessToken);
        var youTrackImport = new YouTrackImport(serialization, youTrackClient, attachmentBasePath, dryRun);

        youTrackImport.run();
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

        start(Common.uriFromString(options.valueOf(instanceUrlOpt)),options.valueOf(inputOpt), youTrackAccessToken,
            options.has(dryRun));
    }

    private static final class ImportException extends RuntimeException {
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
