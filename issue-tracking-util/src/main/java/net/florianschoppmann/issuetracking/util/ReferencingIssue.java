package net.florianschoppmann.issuetracking.util;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

public class ReferencingIssue {
    @XmlValue
    public @Nullable String name;

    @XmlAttribute
    public @Nullable String sourceIssueKey;

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        ReferencingIssue other = (ReferencingIssue) otherObject;
        return Objects.equals(name, other.name)
            && Objects.equals(sourceIssueKey, other.sourceIssueKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, sourceIssueKey);
    }
}
