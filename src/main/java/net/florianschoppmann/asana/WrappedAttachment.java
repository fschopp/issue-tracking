package net.florianschoppmann.asana;

import com.asana.models.Attachment;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public final class WrappedAttachment implements Comparable<WrappedAttachment> {
    private final String attachmentId;
    private final Instant createdAt;
    private final Attachment attachment;

    private int numberInTask;
    @Nullable private WrappedUser createdBy;
    @Nullable private Path downloadPath;
    @Nullable private CompletableFuture<Path> download;

    WrappedAttachment(Attachment attachment) {
        attachmentId = Objects.requireNonNull(attachment.id);
        createdAt = Instant.ofEpochMilli(attachment.createdAt.getValue());
        this.attachment = attachment;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        WrappedAttachment other = (WrappedAttachment) otherObject;
        return createdAt.equals(other.createdAt)
            && attachmentId.equals(other.attachmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdAt, attachmentId);
    }

    @Override
    public int compareTo(WrappedAttachment other) {
        int result = createdAt.compareTo(other.createdAt);
        if (result == 0) {
            result = attachmentId.compareTo(other.attachmentId);
        }
        return result;
    }

    public Attachment getAttachment() {
        return attachment;
    }

    void setNumberInTask(int numberInTask) {
        this.numberInTask = numberInTask;
    }

    public int getNumberInTask() {
        return numberInTask;
    }

    void setCreatedBy(WrappedUser createdBy) {
        this.createdBy = Objects.requireNonNull(createdBy);
    }

    @Nullable
    public WrappedUser getCreatedBy() {
        return createdBy;
    }

    void setDownload(Path downloadPath, CompletableFuture<Path> download) {
        this.downloadPath = downloadPath;
        this.download = download;
    }

    /**
     * Returns the (relative) download path for this attachment.
     */
    @Nullable
    public Path getDownloadPath() {
        return downloadPath;
    }

    /**
     * Returns the future representing the download (which may still be in progress).
     *
     * The future will be completed with the (relative) download path for this attachment.
     */
    @Nullable
    public CompletableFuture<Path> getDownload() {
        return download;
    }
}
