package net.florianschoppmann.youtrack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
public class Attachments {
    private List<Attachment> attachment;

    @Override
    public boolean equals(Object otherObject) {
        return this == otherObject
            || (otherObject instanceof Attachments
                && Objects.equals(attachment, ((Attachments) otherObject).attachment));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(attachment);
    }

    @XmlElement
    public List<Attachment> getAttachment() {
        if (attachment == null) {
            attachment = new ArrayList<>();
        }
        return attachment;
    }

    @XmlType(propOrder = {"taskNumberInProject", "authorLogin", "created", "name", "path", "link"})
    public static class Attachment {
        private Integer taskNumberInProject;
        private String authorLogin;
        private Long created;
        private String name;
        private String path;
        private String link;

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

        @XmlAttribute
        public Integer getTaskNumberInProject() {
            return taskNumberInProject;
        }

        public void setTaskNumberInProject(Integer taskNumberInProject) {
            this.taskNumberInProject = taskNumberInProject;
        }

        @XmlAttribute
        public String getAuthorLogin() {
            return authorLogin;
        }

        public void setAuthorLogin(String authorLogin) {
            this.authorLogin = authorLogin;
        }

        @XmlAttribute
        public Long getCreated() {
            return created;
        }

        public void setCreated(Long created) {
            this.created = created;
        }

        @Nullable
        @XmlAttribute
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Nullable
        @XmlAttribute
        public String getPath() {
            return path;
        }

        public void setPath(@Nullable String path) {
            this.path = path;
        }

        @Nullable
        @XmlAttribute
        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }
    }
}
