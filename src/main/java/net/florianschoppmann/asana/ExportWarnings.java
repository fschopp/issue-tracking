package net.florianschoppmann.asana;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlRootElement
public class ExportWarnings {
    private final List<MissingId> missingTask = new ArrayList<>();
    private final List<MissingId> missingUser = new ArrayList<>();
    private final List<TaskWithIncompleteAttachments> taskWithIncompleteAttachments = new ArrayList<>();

    @XmlElement
    public List<MissingId> getMissingTask() {
        return missingTask;
    }

    @XmlElement
    public List<MissingId> getMissingUser() {
        return missingUser;
    }

    @XmlElement
    public List<TaskWithIncompleteAttachments> getTaskWithIncompleteAttachments() {
        return taskWithIncompleteAttachments;
    }

    public static class MissingId {
        private String asanaId;
        private final List<ReferencingTask> referencingTask = new ArrayList<>();

        @Override
        public boolean equals(Object otherObject) {
            if (this == otherObject) {
                return true;
            } else if (otherObject == null || getClass() != otherObject.getClass()) {
                return false;
            }

            MissingId other = (MissingId) otherObject;
            return Objects.equals(asanaId, other.asanaId)
                && Objects.equals(referencingTask, other.referencingTask);
        }

        @Override
        public int hashCode() {
            return Objects.hash(asanaId, referencingTask);
        }

        public String getAsanaId() {
            return asanaId;
        }

        public void setAsanaId(String asanaId) {
            this.asanaId = asanaId;
        }

        @XmlElement
        public List<ReferencingTask> getReferencingTask() {
            return referencingTask;
        }
    }

    @XmlType(propOrder = {"asanaId", "name"})
    public static class ReferencingTask {
        private String name;
        private String asanaId;

        @Override
        public boolean equals(Object otherObject) {
            if (this == otherObject) {
                return true;
            } else if (otherObject == null || getClass() != otherObject.getClass()) {
                return false;
            }

            ReferencingTask other = (ReferencingTask) otherObject;
            return Objects.equals(name, other.name)
                && Objects.equals(asanaId, other.asanaId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, asanaId);
        }

        @XmlValue
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @XmlAttribute
        public String getAsanaId() {
            return asanaId;
        }

        public void setAsanaId(String asanaId) {
            this.asanaId = asanaId;
        }
    }

    @XmlType(propOrder = {"taskNumberInProject", "asanaTaskId", "name", "attachmentWithoutCreator"})
    public static class TaskWithIncompleteAttachments {
        private Integer taskNumberInProject;
        private String asanaTaskId;
        private String name;
        private List<String> attachmentWithoutCreator;

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
                && Objects.equals(attachmentWithoutCreator, other.attachmentWithoutCreator);
        }

        @Override
        public int hashCode() {
            return Objects.hash(taskNumberInProject, asanaTaskId, name, attachmentWithoutCreator);
        }

        @XmlAttribute
        public Integer getTaskNumberInProject() {
            return taskNumberInProject;
        }

        public void setTaskNumberInProject(Integer taskNumberInProject) {
            this.taskNumberInProject = taskNumberInProject;
        }

        @XmlAttribute
        public String getAsanaTaskId() {
            return asanaTaskId;
        }

        public void setAsanaTaskId(String asanaTaskId) {
            this.asanaTaskId = asanaTaskId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @XmlElement
        public List<String> getAttachmentWithoutCreator() {
            if (attachmentWithoutCreator == null) {
                attachmentWithoutCreator = new ArrayList<>();
            }
            return attachmentWithoutCreator;
        }
    }
}
