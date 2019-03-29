package net.florianschoppmann.issuetracking.youtrack.rest;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public class IssueComment {
    public @Nullable String id;
    public @Nullable String text;

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        IssueComment other = (IssueComment) otherObject;
        return Objects.equals(id, other.id)
            && Objects.equals(text, other.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, text);
    }
}
