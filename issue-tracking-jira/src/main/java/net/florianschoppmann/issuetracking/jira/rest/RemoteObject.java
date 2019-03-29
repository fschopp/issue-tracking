package net.florianschoppmann.issuetracking.jira.rest;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public class RemoteObject {
    public @Nullable String url;
    public @Nullable String title;
    public @Nullable Icon icon;

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        RemoteObject other = (RemoteObject) otherObject;
        return Objects.equals(url, other.url)
            && Objects.equals(title, other.title)
            && Objects.equals(icon, other.icon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, title, icon);
    }
}
