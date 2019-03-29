package net.florianschoppmann.issuetracking.youtrack;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Attachments {
    @XmlElement(name = "attachment")
    public final List<Attachment> attachments = new ArrayList<>();

    @Override
    public boolean equals(Object otherObject) {
        return this == otherObject
            || (otherObject instanceof Attachments
                && Objects.equals(attachments, ((Attachments) otherObject).attachments));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(attachments);
    }

    public static class Attachment {
        @XmlAttribute
        public @Nullable Integer taskNumberInProject;

        @XmlAttribute
        public @Nullable String authorLogin;

        @XmlAttribute
        public @Nullable Long created;

        @XmlAttribute
        public @Nullable String name;

        @XmlAttribute
        public @Nullable String path;

        @XmlAttribute
        public @Nullable String link;

        @Override
        public boolean equals(Object otherObject) {
            if (this == otherObject) {
                return true;
            } else if (otherObject == null || getClass() != otherObject.getClass()) {
                return false;
            }

            Attachment other = (Attachment) otherObject;
            return Objects.equals(taskNumberInProject, other.taskNumberInProject)
                && Objects.equals(authorLogin, other.authorLogin)
                && Objects.equals(created, other.created)
                && Objects.equals(name, other.name)
                && Objects.equals(path, other.path)
                && Objects.equals(link, other.link);
        }

        @Override
        public int hashCode() {
            return Objects.hash(taskNumberInProject, authorLogin, created, name, path, link);
        }
    }
}
