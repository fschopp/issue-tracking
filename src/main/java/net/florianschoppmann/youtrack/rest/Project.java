package net.florianschoppmann.youtrack.rest;

import java.util.Objects;
import javax.annotation.Nullable;

public class Project {
    private String identifier;

    private String shortName;

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        Project other = (Project) otherObject;
        return Objects.equals(identifier, other.identifier)
            && Objects.equals(shortName, other.shortName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, shortName);
    }

    @Nullable
    public String getId() {
        return identifier;
    }

    public void setId(@Nullable String identifier) {
        this.identifier = identifier;
    }

    @Nullable
    public String getShortName() {
        return shortName;
    }

    public void setShortName(@Nullable String shortName) {
        this.shortName = shortName;
    }
}
