package net.florianschoppmann.youtrack.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;

public class Issue {
    private String identifier;

    private String summary;
    private final List<IssueTag> tags = new ArrayList<>();

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        Issue other = (Issue) otherObject;
        return Objects.equals(identifier, other.identifier)
            && Objects.equals(summary, other.summary)
            && Objects.equals(tags, other.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, summary, tags);
    }

    @Nullable
    public String getId() {
        return identifier;
    }

    public void setId(@Nullable String id) {
        identifier = id;
    }

    @Nullable
    public String getSummary() {
        return summary;
    }

    public void setSummary(@Nullable String summary) {
        this.summary = summary;
    }

    @XmlElement
    public List<IssueTag> getTags() {
        return tags;
    }
}
