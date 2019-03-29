package net.florianschoppmann.issuetracking.youtrack.rest;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class User {
    public @Nullable String id;
    public @Nullable String email;
    public @Nullable String login;

    @XmlElementWrapper
    @XmlElement(name = "tag")
    public @Nullable List<IssueTag> tags;

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        User other = (User) otherObject;
        return Objects.equals(id, other.id)
            && Objects.equals(email, other.email)
            && Objects.equals(login, other.login)
            && Objects.equals(tags, other.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, login, tags);
    }
}
