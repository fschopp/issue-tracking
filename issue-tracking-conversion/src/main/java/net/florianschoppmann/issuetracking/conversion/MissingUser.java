package net.florianschoppmann.issuetracking.conversion;

import net.florianschoppmann.issuetracking.util.ReferencingIssue;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class MissingUser {
    public @Nullable String userId;
    public @Nullable String email;

    @XmlElementWrapper
    @XmlElement(name = "referencingIssue")
    public final List<ReferencingIssue> referencingIssues = new ArrayList<>();

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        MissingUser other = (MissingUser) otherObject;
        return Objects.equals(userId, other.userId)
            && Objects.equals(email, other.email)
            && Objects.equals(referencingIssues, other.referencingIssues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email, referencingIssues);
    }
}
