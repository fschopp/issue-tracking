package net.florianschoppmann.issuetracking.asana;

import com.asana.models.Task;
import net.florianschoppmann.issuetracking.util.LazyString;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public final class WrappedTask implements Comparable<WrappedTask> {
    private final String taskId;
    private final Instant createdAt;
    private final Task task;
    private final @Nullable WrappedTask sectionTitle;
    private final SortedSet<WrappedComment> comments = new TreeSet<>();
    private final SortedSet<WrappedAttachment> attachments = new TreeSet<>();

    private int numberInProject;
    private LazyString markdownDescription;

    WrappedTask(Task task, @Nullable WrappedTask sectionTitle) {
        taskId = Objects.requireNonNull(task.id);
        createdAt = Instant.ofEpochMilli(task.createdAt.getValue());
        this.task = task;
        this.sectionTitle = sectionTitle;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        WrappedTask other = (WrappedTask) otherObject;
        return createdAt.equals(other.createdAt)
            && taskId.equals(other.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdAt, taskId);
    }

    @Override
    public int compareTo(WrappedTask other) {
        int result = createdAt.compareTo(other.createdAt);
        if (result == 0) {
            result = taskId.compareTo(other.taskId);
        }
        return result;
    }

    public String getTaskId() {
        return taskId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    void setMarkdownDescription(LazyString markdownDescription) {
        this.markdownDescription = markdownDescription;
    }

    public LazyString getMarkdownDescription() {
        return markdownDescription;
    }

    public Task getTask() {
        return task;
    }

    public @Nullable WrappedTask getSectionTitle() {
        return sectionTitle;
    }

    void addComment(WrappedComment comment) {
        comments.add(comment);
    }

    public SortedSet<WrappedComment> getComments() {
        return Collections.unmodifiableSortedSet(comments);
    }

    void addAttachment(WrappedAttachment attachment) {
        attachments.add(attachment);
    }

    public SortedSet<WrappedAttachment> getAttachments() {
        return Collections.unmodifiableSortedSet(attachments);
    }

    void setNumberInProject(int numberInProject) {
        this.numberInProject = numberInProject;
    }

    public int getNumberInProject() {
        return numberInProject;
    }
}
