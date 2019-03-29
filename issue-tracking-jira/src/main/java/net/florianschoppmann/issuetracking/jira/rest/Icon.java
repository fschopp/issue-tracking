package net.florianschoppmann.issuetracking.jira.rest;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public class Icon {
    public @Nullable String url16x16;

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        Icon other = (Icon) otherObject;
        return Objects.equals(url16x16, other.url16x16);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url16x16);
    }
}
