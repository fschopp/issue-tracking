package net.florianschoppmann.issuetracking.asana;

import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public interface AttachmentDownloader {
    Path downloadPath(Path basePath, String taskId, String attachmentName);
    CompletableFuture<Path> download(Path basePath, String taskId, String attachmentName, URL url);
}
