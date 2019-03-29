package net.florianschoppmann.issuetracking;

import static java.util.Collections.singletonMap;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import net.florianschoppmann.issuetracking.conversion.JiraToYouTrack;
import net.florianschoppmann.issuetracking.jira.JiraClient;
import net.florianschoppmann.issuetracking.youtrack.YouTrackClient;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.eclipse.persistence.jaxb.UnmarshallerProperties;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.moxy.json.MoxyJsonConfig;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.logging.Level;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.xml.bind.JAXBException;

public final class JiraExport {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final JiraToYouTrack jiraToYouTrack;
    private final Serialization serialization;
    private final String projectAbbrev;

    private JiraExport(JiraToYouTrack jiraToYouTrack, Serialization serialization, String projectAbbrev) {
        this.jiraToYouTrack = jiraToYouTrack;
        this.serialization = serialization;
        this.projectAbbrev = projectAbbrev;
    }

    private void run() throws JAXBException {
        JiraToYouTrack.Result result = jiraToYouTrack.youTrackFromJiraProject(projectAbbrev);
        serialization.writeResultXml(result.getLinks());
        serialization.writeResultXml(result.getIssueUpdates());
        serialization.writeResultXml(result.getCommentUpdates());
        serialization.writeResultXml(result.getConversionWarnings());

        var importSettings = new ImportSettings();
        importSettings.youTrackProjectAbbrev = projectAbbrev;
        importSettings.importLinks = true;
        importSettings.updateIssues = true;
        importSettings.updateComments = true;
        serialization.writeResultXml(importSettings);
    }

    private static void start(URI jiraBaseUri, URI youTrackBaseUri, Path filesBasePath, String projectAbbrev,
            String jiraUsername, String jiraPassword, String youTrackAccessToken) throws IOException, JAXBException {
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
                .register(MoxyJsonFeature.class)
                .register(moxyJsonConfig.resolver())
                .register(LoggingFeature.class)
                .property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_CLIENT, LoggingFeature.Verbosity.PAYLOAD_ANY)
                .property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_CLIENT, Level.FINE.getName())
        );
        Serialization serialization = Serialization.defaultSerialization(filesBasePath);
        JiraRestClientFactory clientFactory = new AsynchronousJiraRestClientFactory();
        try (JiraRestClient jiraRestClient
                = clientFactory.createWithBasicHttpAuthentication(jiraBaseUri, jiraUsername, jiraPassword)) {
            JiraClient jiraClient
                = new JiraClient(jiraRestClient, jaxrsClient, jiraBaseUri, jiraUsername, jiraPassword);
            YouTrackClient youTrackClient = new YouTrackClient(jaxrsClient, youTrackBaseUri, youTrackAccessToken);
            var jiraToYouTrack = new JiraToYouTrack(youTrackClient, jiraClient, jiraRestClient);

            var jiraExport = new JiraExport(jiraToYouTrack, serialization, projectAbbrev);
            jiraExport.run();
        }
    }

    public static void main(String[] args) throws IOException, JAXBException  {
        final Logger log = LoggerFactory.getLogger(JiraExport.class);

        OptionParser parser = new OptionParser();
        OptionSpec<Void> helpOption = parser.accepts("help").forHelp();
        OptionSpec<String> jiraInstanceUrlOpt = parser
            .accepts("jira-url",
                "URL of the Jira instance (of form https://<name>.atlassian.com/ for Jira Cloud)")
            .withRequiredArg().required();
        OptionSpec<String> youTrackInstanceUrlOpt = parser
            .accepts("youtrack-url",
                "URL of the YouTrack instance (of form https://<name>.myjetbrains.com/youtrack for InCloud instances)")
            .withRequiredArg().required();
        OptionSpec<String> youTrackAbbrevOpt = parser
            .accepts("abbrev", "JIRA and YouTrack project abbreviation, should not contain hyphen (-)")
            .withRequiredArg().required();
        OptionSpec<Path> outputOpt = parser.accepts("output", "path where to store output")
            .withRequiredArg().withValuesConvertedBy(new PathConverter()).required();
        OptionSet options = parser.parse(args);
        if (options.has(helpOption)) {
            parser.printHelpOn(System.out);
            System.exit(0);
            return;
        }

        String jiraUsername = System.getenv("JIRA_USER_NAME");
        String jiraPassword = System.getenv("JIRA_PASSWORD");
        String youTrackAccessToken = System.getenv("YOUTRACK_ACCESS_TOKEN");
        if (jiraUsername == null || jiraPassword == null || youTrackAccessToken == null) {
            log.error("Environment variables JIRA_USER_NAME, JIRA_PASSWORD, and YOUTRACK_ACCESS_TOKEN must be "
                + "defined.");
            System.exit(1);
            return;
        }

        start(Common.uriFromString(options.valueOf(jiraInstanceUrlOpt)),
            Common.uriFromString(options.valueOf(youTrackInstanceUrlOpt)), options.valueOf(outputOpt),
            options.valueOf(youTrackAbbrevOpt), jiraUsername, jiraPassword, youTrackAccessToken);
    }
}
