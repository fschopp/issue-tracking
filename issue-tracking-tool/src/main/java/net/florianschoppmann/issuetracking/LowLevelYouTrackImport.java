package net.florianschoppmann.issuetracking;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import net.florianschoppmann.issuetracking.youtrack.Events;
import net.florianschoppmann.issuetracking.youtrack.YouTrackDatabaseClient;
import net.florianschoppmann.issuetracking.youtrack.YouTrackDatabaseClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import javax.xml.bind.JAXBException;

public final class LowLevelYouTrackImport {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Serialization serialization;
    private final YouTrackDatabaseClient youTrackDatabaseClient;
    private final boolean dryRun;

    private LowLevelYouTrackImport(Serialization serialization, YouTrackDatabaseClient youTrackDatabaseClient,
            boolean dryRun) {
        this.serialization = serialization;
        this.youTrackDatabaseClient = youTrackDatabaseClient;
        this.dryRun = dryRun;
    }

    private void run() throws JAXBException {
        if (dryRun) {
            log.info("Dry run. Import will not be saved...");
        }

        ImportSettings importSettings = serialization.readResultXml(ImportSettings.class);
        String projectAbbrev = importSettings.youTrackProjectAbbrev;

        try {
            if (importSettings.importEvents) {
                Events events = serialization.readResultXml(Events.class);
                youTrackDatabaseClient.importEvents(events, projectAbbrev);
            }
        } catch (YouTrackDatabaseClientException exception) {
            log.error("The import did not succeed. No changes were made to the database.", exception);
        }
    }

    private static void start(Path databasePath, Path ioPath, boolean dryRun) throws JAXBException {
        Serialization serialization = Serialization.defaultSerialization(ioPath);
        var youTrackDatabaseClient = new YouTrackDatabaseClient(databasePath);
        var lowLevelYouTrackImport = new LowLevelYouTrackImport(serialization, youTrackDatabaseClient, dryRun);

        lowLevelYouTrackImport.run();
    }


    public static void main(String[] args) throws IOException, JAXBException {
        OptionParser parser = new OptionParser();
        OptionSpec<Void> helpOption = parser.accepts("help").forHelp();
        OptionSpec<Path> databasePathOpt = parser.accepts("db", "path to the YouTrack database")
            .withRequiredArg().withValuesConvertedBy(new PathConverter()).required();
        OptionSpec<Path> inputOpt = parser.accepts("in", "path where to read input")
            .withRequiredArg().withValuesConvertedBy(new PathConverter()).required();
        OptionSpec<Void> dryRun = parser.accepts("dry-run", "if given, the database transaction is not committed");
        OptionSet options = parser.parse(args);
        if (options.has(helpOption)) {
            parser.printHelpOn(System.out);
            System.exit(0);
            return;
        }

        start(options.valueOf(databasePathOpt), options.valueOf(inputOpt), options.has(dryRun));
    }
}
