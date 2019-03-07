package net.florianschoppmann.youtrack.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

public class IssueTag {
    private String identifier;

    private String name;
    private final List<UserGroup> visibleFor = new ArrayList<>();

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        IssueTag other = (IssueTag) otherObject;
        return Objects.equals(identifier, other.identifier)
            && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, name);
    }

    @Nullable
    public String getId() {
        return identifier;
    }

    public void setId(@Nullable String id) {
        identifier = id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public List<UserGroup> getVisibleFor() {
        return visibleFor;
    }
}
