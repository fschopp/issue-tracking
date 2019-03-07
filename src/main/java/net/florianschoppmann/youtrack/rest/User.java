package net.florianschoppmann.youtrack.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;

public class User {
    private String identifier;

    private String email;
    private String name;
    private final List<IssueTag> tags = new ArrayList<>();

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        User other = (User) otherObject;
        return Objects.equals(identifier, other.identifier)
            && Objects.equals(email, other.email)
            && Objects.equals(name, other.name)
            && Objects.equals(tags, other.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, email, name, tags);
    }

    @Nullable
    public String getId() {
        return identifier;
    }

    public void setId(@Nullable String id) {
        identifier = id;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    public void setEmail(@Nullable String email) {
        this.email = email;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    @XmlElement
    public List<IssueTag> getTags() {
        return tags;
    }
}
