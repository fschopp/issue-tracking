package net.florianschoppmann.issuetracking.conversion;

import net.florianschoppmann.issuetracking.util.ReferencingIssue;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class MissingProject {
    private @Nullable String project;

    @XmlElementWrapper
    @XmlElement(name = "referencingIssue")
    private @Nullable List<ReferencingIssue> referencingIssues;

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        MissingProject other = (MissingProject) otherObject;
        return Objects.equals(project, other.project)
            && Objects.equals(referencingIssues, other.referencingIssues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, referencingIssues);
    }
}
