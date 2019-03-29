package net.florianschoppmann.issuetracking.youtrack.rest;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public class Project {
    public @Nullable String id;
    public @Nullable String shortName;

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        Project other = (Project) otherObject;
        return Objects.equals(id, other.id)
            && Objects.equals(shortName, other.shortName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, shortName);
    }
}
