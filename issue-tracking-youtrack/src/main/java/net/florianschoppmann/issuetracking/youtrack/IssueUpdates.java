package net.florianschoppmann.issuetracking.youtrack;

import net.florianschoppmann.issuetracking.youtrack.rest.Issue;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class IssueUpdates {
    @XmlElement(name = "issueUpdate")
    public final List<IssueUpdate> issueUpdates = new ArrayList<>();

    @Override
    public boolean equals(Object otherObject) {
        return this == otherObject
            || (otherObject instanceof IssueUpdates
                && Objects.equals(issueUpdates, ((IssueUpdates) otherObject).issueUpdates));
    }

    @Override
    public int hashCode() {
        return Objects.hash(issueUpdates);
    }

    public static class IssueUpdate {
        public @Nullable String issueKey;
        public @Nullable Issue issue;

        @Override
        public boolean equals(@Nullable Object otherObject) {
            if (this == otherObject) {
                return true;
            } else if (otherObject == null || getClass() != otherObject.getClass()) {
                return false;
            }

            IssueUpdate other = (IssueUpdate) otherObject;
            return Objects.equals(issueKey, other.issueKey)
                && Objects.equals(issue, other.issue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(issueKey, issue);
        }
    }
}
