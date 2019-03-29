package net.florianschoppmann.issuetracking.youtrack.rest;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class Issue {
    public @Nullable String id;
    public @Nullable String description;
    public @Nullable Integer numberInProject;
    public @Nullable String summary;

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

        Issue other = (Issue) otherObject;
        return Objects.equals(id, other.id)
            && Objects.equals(description, other.description)
            && Objects.equals(numberInProject, other.numberInProject)
            && Objects.equals(summary, other.summary)
            && Objects.equals(tags, other.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, description, numberInProject, summary, tags);
    }
}
