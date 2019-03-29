package net.florianschoppmann.issuetracking.youtrack;

import net.florianschoppmann.issuetracking.youtrack.rest.IssueComment;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CommentUpdates {
    @XmlElement(name = "commentUpdate")
    public final List<CommentUpdate> commentUpdates = new ArrayList<>();

    @Override
    public boolean equals(Object otherObject) {
        return this == otherObject
            || (otherObject instanceof CommentUpdates
                && Objects.equals(commentUpdates, ((CommentUpdates) otherObject).commentUpdates));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(commentUpdates);
    }

    public static class CommentUpdate {
        public @Nullable String issueKey;
        public @Nullable String commentId;
        public @Nullable IssueComment issueComment;

        @Override
        public boolean equals(Object otherObject) {
            if (this == otherObject) {
                return true;
            } else if (otherObject == null || getClass() != otherObject.getClass()) {
                return false;
            }

            CommentUpdate other = (CommentUpdate) otherObject;
            return Objects.equals(issueKey, other.issueKey)
                && Objects.equals(commentId, other.commentId)
                && Objects.equals(issueComment, other.issueComment);
        }

        @Override
        public int hashCode() {
            return Objects.hash(issueKey, commentId, issueComment);
        }
    }

}
