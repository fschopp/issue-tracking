package net.florianschoppmann.issuetracking.jira.rest;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public class RemoteIssueLink {
    public @Nullable RemoteObject object;

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        RemoteIssueLink other = (RemoteIssueLink) otherObject;
        return Objects.equals(object, other.object);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(object);
    }
}
