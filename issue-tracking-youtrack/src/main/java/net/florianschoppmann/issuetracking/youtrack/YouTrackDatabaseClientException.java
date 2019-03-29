package net.florianschoppmann.issuetracking.youtrack;

public class YouTrackDatabaseClientException extends Exception {
    private static final long serialVersionUID = 6124719906182068207L;

    public YouTrackDatabaseClientException(String message) {
        super(message);
    }

    public YouTrackDatabaseClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
