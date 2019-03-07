package net.florianschoppmann.asana;

import com.asana.models.Story;
import net.florianschoppmann.util.LazyString;

import java.time.Instant;
import java.util.Objects;

public final class WrappedComment implements Comparable<WrappedComment> {
    private final String storyId;
    private final Instant createdAt;
    private final Story story;

    private int numberInTask;
    private LazyString markdownText;

    WrappedComment(Story story) {
        storyId = Objects.requireNonNull(story.id);
        createdAt = Instant.ofEpochMilli(story.createdAt.getValue());
        // This is the default. For user comments, this will be overridden later.
        markdownText = LazyString.of(story.text);
        this.story = story;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        WrappedComment other = (WrappedComment) otherObject;
        return createdAt.equals(other.createdAt)
            && storyId.equals(other.storyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdAt, storyId);
    }

    @Override
    public int compareTo(WrappedComment other) {
        int result = createdAt.compareTo(other.createdAt);
        if (result == 0) {
            result = storyId.compareTo(other.storyId);
        }
        return result;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    void setMarkdownText(LazyString markdownText) {
        this.markdownText = markdownText;
    }

    public LazyString getMarkdownText() {
        return markdownText;
    }

    public Story getStory() {
        return story;
    }

    void setNumberInTask(int numberInTask) {
        this.numberInTask = numberInTask;
    }

    public int getNumberInTask() {
        return numberInTask;
    }
}
