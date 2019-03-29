package net.florianschoppmann.issuetracking.asana;

import net.florianschoppmann.issuetracking.util.ReferencingIssue;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AsanaExportWarnings {
    @XmlElementWrapper
    @XmlElement(name = "missingTask")
    public final List<MissingId> missingTasks = new ArrayList<>();

    @XmlElementWrapper
    @XmlElement(name = "missingUser")
    public final List<MissingId> missingUsers = new ArrayList<>();

    @XmlElementWrapper
    @XmlElement(name = "taskWithIncompleteAttachment")
    public final List<TaskWithIncompleteAttachments> taskWithIncompleteAttachments = new ArrayList<>();

    public static class MissingId {
        public @Nullable String asanaId;

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

            MissingId other = (MissingId) otherObject;
            return Objects.equals(asanaId, other.asanaId)
                && Objects.equals(referencingIssues, other.referencingIssues);
        }

        @Override
        public int hashCode() {
            return Objects.hash(asanaId, referencingIssues);
        }
    }

    public static class TaskWithIncompleteAttachments {
        @XmlAttribute
        public @Nullable Integer taskNumberInProject;

        @XmlAttribute
        public @Nullable String asanaTaskId;

        public @Nullable String name;

        @XmlElementWrapper
        @XmlElement(name = "attachmentWithoutCreator")
        public final List<String> attachmentWithoutCreators = new ArrayList<>();

        @Override
        public boolean equals(Object otherObject) {
            if (this == otherObject) {
                return true;
            } else if (otherObject == null || getClass() != otherObject.getClass()) {
                return false;
            }

            TaskWithIncompleteAttachments other = (TaskWithIncompleteAttachments) otherObject;
            return Objects.equals(taskNumberInProject, other.taskNumberInProject)
                && Objects.equals(asanaTaskId, other.asanaTaskId)
                && Objects.equals(name, other.name)
                && Objects.equals(attachmentWithoutCreators, other.attachmentWithoutCreators);
        }

        @Override
        public int hashCode() {
            return Objects.hash(taskNumberInProject, asanaTaskId, name, attachmentWithoutCreators);
        }
    }
}
