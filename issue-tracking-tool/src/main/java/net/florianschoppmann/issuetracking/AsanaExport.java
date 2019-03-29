package net.florianschoppmann.issuetracking;

import com.asana.Client;
import com.asana.models.Project;
import com.asana.models.Workspace;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import net.florianschoppmann.issuetracking.asana.AsyncAttachmentDownloader;
import net.florianschoppmann.issuetracking.asana.Export;
import net.florianschoppmann.issuetracking.conversion.AsanaToYouTrack;
import net.florianschoppmann.java.futures.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBException;

public final class AsanaExport {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String workspaceName;
    private final String projectName;
    private final String youTrackProjectAbbrev;
    private final Client asanaClient;
    private final AsanaToYouTrack asanaToYouTrack;
    private final Serialization serialization;
    private final Map<String, String> emailToLoginNameMap;
    private final Path attachmentBasePath;
    private final AsanaToYouTrack.Options options;

    private AsanaExport(String workspaceName, String projectName, String youTrackProjectAbbrev, Client asanaClient,
            AsanaToYouTrack asanaToYouTrack, Serialization serialization, Map<String, String> emailToLoginNameMap,
            Path attachmentBasePath, AsanaToYouTrack.Options options) {
        this.workspaceName = workspaceName;
        this.projectName = projectName;
        this.youTrackProjectAbbrev = youTrackProjectAbbrev;
        this.asanaClient = asanaClient;
        this.asanaToYouTrack = asanaToYouTrack;
        this.serialization = serialization;
        this.emailToLoginNameMap = emailToLoginNameMap;
        this.attachmentBasePath = attachmentBasePath;
        this.options = options;
    }

    private void run() throws IOException, JAXBException {
        Workspace workspace = null;
        for (Workspace currentWorkspace : asanaClient.workspaces.findAll()) {
            if (workspaceName.equals(currentWorkspace.name)) {
                workspace = currentWorkspace;
                break;
            }
        }
        if (workspace == null) {
            log.error("Could not find workspace '{}'.\n", workspaceName);
            System.exit(1);
            return;
        }

        Project project = null;
        for (Project currentProject : asanaClient.projects.findByWorkspace(workspace.id)) {
            if (projectName.equals(currentProject.name)) {
                project = currentProject;
                break;
            }
        }
        if (project == null) {
            log.error("Could not find project '{}'.\n", projectName);
            System.exit(1);
            return;
        }

        Files.createDirectories(attachmentBasePath);
        AsanaToYouTrack.Result result = asanaToYouTrack.youTrackFromAsanaProject(project.id, attachmentBasePath,
            youTrackProjectAbbrev, emailToLoginNameMap, options);

        for (CompletableFuture<Path> download : result.getDownloads()) {
            Futures.unwrapCompletionException(download).join();
        }

        serialization.writeResultXml(result.getIssues());
        serialization.writeResultXml(result.getLinks());
        serialization.writeResultXml(result.getAttachments());
        serialization.writeResultXml(result.getIssueUpdates());
        serialization.writeResultXml(result.getExportWarnings());
        serialization.writeResultXml(result.getConversionWarnings());

        var importSettings = new ImportSettings();
        importSettings.youTrackProjectAbbrev = youTrackProjectAbbrev;
        importSettings.importIssues = true;
        importSettings.importLinks = true;
        importSettings.importAttachments = true;
        importSettings.updateIssues = true;
        serialization.writeResultXml(importSettings);
    }

    private static Map<String, String> userMapping(Path userMappingFile) throws IOException {
        return Files.lines(userMappingFile)
            .filter(line -> line.contains("="))
            .map(line -> line.split("="))
            .collect(Collectors.toMap(array -> array[0].toLowerCase(), array -> array[1]));
    }

    private static void start(String workspaceName, String projectName, String youTrackProjectAbbrev,
            Path userMappingFile, Path attachmentBasePath, boolean estimatesInBrackets, int startId,
            String asanaAccessToken) throws IOException, JAXBException {
        // Create all dependencies
        Client asanaClient = Client.accessToken(asanaAccessToken);
        Serialization serialization = Serialization.defaultSerialization(attachmentBasePath);
        var executor = new ThreadPoolExecutor(4, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        executor.allowCoreThreadTimeOut(true);
        HttpClient httpClient = HttpClient.newBuilder()
            .executor(executor)
            .build();
        var attachmentDownloader = new AsyncAttachmentDownloader(httpClient);
        var export = new Export(asanaClient, attachmentDownloader);
        var asanaToYouTrack = new AsanaToYouTrack(export);
        var options = new AsanaToYouTrack.Options(estimatesInBrackets, startId);
        var asanaDownloader = new AsanaExport(workspaceName, projectName, youTrackProjectAbbrev, asanaClient,
            asanaToYouTrack, serialization, userMapping(userMappingFile), attachmentBasePath, options);

        try {
            asanaDownloader.run();
        } finally {
            executor.shutdown();
        }
    }

    public static void main(String[] args) throws IOException, JAXBException  {
        final Logger log = LoggerFactory.getLogger(AsanaExport.class);

        OptionParser parser = new OptionParser();
        OptionSpec<Void> helpOption = parser.accepts("help").forHelp();
        OptionSpec<String> workspaceOpt
            = parser.accepts("workspace", "name of the Asana workspace").withRequiredArg().required();
        OptionSpec<String> asanaProjectOpt
            = parser.accepts("project", "name of the Asana project").withRequiredArg().required();
        OptionSpec<String> youTrackAbbrevOpt = parser
            .accepts("abbrev", "YouTrack project abbreviation, should not contain hyphen (-)")
            .withRequiredArg().required();
        OptionSpec<Path> userMappingOpt = parser.accepts("user-mapping",
            "path to file with lines of form \"<Asana email>=<YouTrack login name>\"")
            .withRequiredArg().withValuesConvertedBy(new PathConverter()).required();
        OptionSpec<Path> outputOpt = parser.accepts("output", "path where to store output")
            .withRequiredArg().withValuesConvertedBy(new PathConverter()).required();
        OptionSpec<Void> noTimeEstimatesInBracketsOpt = parser
            .accepts("no-estimates", "in task names, do not treat numbers in brackets as time estimates");
        OptionSpec<Integer> startIdOpt = parser.accepts("start-id", "first number in project")
            .withRequiredArg().ofType(Integer.class).defaultsTo(1);
        OptionSet options = parser.parse(args);
        if (options.has(helpOption)) {
            parser.printHelpOn(System.out);
            System.exit(0);
            return;
        }

        String asanaAccessToken = System.getenv("ASANA_ACCESS_TOKEN");
        if (asanaAccessToken == null) {
            log.error(
                "Environment variable ASANA_ACCESS_TOKEN must be defined and contain a valid access token for Asana.");
            System.exit(1);
            return;
        }

        start(options.valueOf(workspaceOpt), options.valueOf(asanaProjectOpt), options.valueOf(youTrackAbbrevOpt),
            options.valueOf(userMappingOpt), options.valueOf(outputOpt), !options.has(noTimeEstimatesInBracketsOpt),
            options.valueOf(startIdOpt), asanaAccessToken);
    }
}
