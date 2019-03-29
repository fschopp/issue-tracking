package net.florianschoppmann.issuetracking.youtrack.rest;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public class UserGroup {
    public @Nullable String identifier;
    public @Nullable String name;
    public @Nullable Project teamForProject;

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        UserGroup other = (UserGroup) otherObject;
        return Objects.equals(identifier, other.identifier)
            && Objects.equals(name, other.name)
            && Objects.equals(teamForProject, other.teamForProject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, name, teamForProject);
    }
}
