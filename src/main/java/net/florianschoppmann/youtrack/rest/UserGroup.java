package net.florianschoppmann.youtrack.rest;

import java.util.Objects;
import javax.annotation.Nullable;

public class UserGroup {
    private String identifier;

    private boolean allUsersGroup;
    private String name;
    private Project teamForProject;

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        UserGroup other = (UserGroup) otherObject;
        return Objects.equals(identifier, other.identifier)
            && Objects.equals(allUsersGroup, other.allUsersGroup)
            && Objects.equals(name, other.name)
            && Objects.equals(teamForProject, other.teamForProject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, allUsersGroup, name, teamForProject);
    }

    @Nullable
    public String getId() {
        return identifier;
    }

    public void setId(@Nullable String identifier) {
        this.identifier = identifier;
    }

    public boolean isAllUsersGroup() {
        return allUsersGroup;
    }

    public void setAllUsersGroup(boolean allUsersGroup) {
        this.allUsersGroup = allUsersGroup;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Nullable
    public Project getTeamForProject() {
        return teamForProject;
    }

    public void setTeamForProject(@Nullable Project teamForProject) {
        this.teamForProject = teamForProject;
    }
}
