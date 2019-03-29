package net.florianschoppmann.issuetracking.asana;

import net.florianschoppmann.java.futures.Futures;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class AsyncAttachmentDownloader implements AttachmentDownloader {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final HttpClient httpClient;

    public AsyncAttachmentDownloader(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    private CompletableFuture<Path> downloadUriToPath(String taskId, String attachmentName, URI uri, Path path) {
        HttpRequest httpRequest = HttpRequest.newBuilder(uri).build();
        CompletableFuture<Path> downloadFuture = httpClient.sendAsync(httpRequest, BodyHandlers.ofFile(path))
            .thenApply(HttpResponse::body);

        // In case the download fails, we want to remove the file. Of course, this could itself fail with a checked
        // exception, which are not really supported well by CompletableFuture. We therefore need to do a workaround
        // to get these exceptions across.
        CompletableFuture<Path> returnFuture = new CompletableFuture<>();
        downloadFuture.handle((@Nullable Path ignoredPath, @Nullable Throwable downloadException) -> {
            if (downloadException != null) {
                Throwable unwrappedOriginalException = Futures.unwrapCompletionException(downloadException);
                try {
                    Files.deleteIfExists(path);
                    returnFuture.completeExceptionally(downloadException);
                } catch (IOException exception) {
                    unwrappedOriginalException.addSuppressed(exception);
                }
                returnFuture.completeExceptionally(Futures.wrapInCompletionException(unwrappedOriginalException));
            } else {
                returnFuture.complete(path);
                log.info("Finished downloading of attachment \"{}\" (Asana task ID {}).", attachmentName, taskId);
            }
            return path;
        });
        return returnFuture;
    }

    @Override
    public Path downloadPath(Path basePath, String taskId, String attachmentName) {
        return basePath.resolve(taskId).resolve(attachmentName);
    }

    @Override
    public CompletableFuture<Path> download(Path basePath, String taskId, String attachmentName, URL url) {
        Path targetFile = downloadPath(basePath, taskId, attachmentName);
        if (Files.exists(targetFile)) {
            log.info("Skipping attachment \"{}\" (Asana task ID {}) because it was already downloaded.",
                attachmentName, taskId);
            return CompletableFuture.completedFuture(targetFile);
        } else {
            URI uri;
            try {
                Files.createDirectories(targetFile.getParent());
                uri = url.toURI();
                log.info("Starting download of attachment \"{}\" (Asana task ID {}).", attachmentName, taskId);
                return downloadUriToPath(taskId, attachmentName, uri, targetFile);
            } catch (URISyntaxException | IOException exception) {
                return CompletableFuture.failedFuture(exception);
            }
        }
    }
}
