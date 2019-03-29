package net.florianschoppmann.issuetracking.youtrack.rest;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public class IssueTag {
    public @Nullable String id;
    public @Nullable String name;
    public @Nullable UserGroup visibleFor;

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        IssueTag other = (IssueTag) otherObject;
        return Objects.equals(id, other.id)
            && Objects.equals(name, other.name)
            && Objects.equals(visibleFor, other.visibleFor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, visibleFor);
    }
}
