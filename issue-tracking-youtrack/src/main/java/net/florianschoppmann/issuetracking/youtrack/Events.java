package net.florianschoppmann.issuetracking.youtrack;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Events {
    @XmlElements({
        @XmlElement(name = "customFieldEvent", type = CustomFieldEvent.class)
    })
    public final List<Event> events = new ArrayList<>();

    @Override
    public boolean equals(Object otherObject) {
        return this == otherObject
            || (otherObject instanceof Events
                && Objects.equals(events, ((Events) otherObject).events));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(events);
    }

    public abstract static class Event {
        @XmlAttribute
        public @Nullable Long timestamp;

        @Override
        public boolean equals(@Nullable Object otherObject) {
            return otherObject != null && getClass() == otherObject.getClass()
                && Objects.equals(timestamp, ((Event) otherObject).timestamp);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(timestamp);
        }
    }

    public static class CustomFieldEvent extends Event {
        @XmlAttribute
        public @Nullable Long numberInProject;

        @XmlAttribute
        public @Nullable String field;

        @XmlAttribute
        public @Nullable String author;

        public @Nullable String added;

        public @Nullable String removed;

        @Override
        public boolean equals(@Nullable Object otherObject) {
            if (!super.equals(otherObject)) {
                return false;
            }

            CustomFieldEvent other = (CustomFieldEvent) otherObject;
            return Objects.equals(numberInProject, other.numberInProject)
                && Objects.equals(field, other.field)
                && Objects.equals(author, other.author)
                && Objects.equals(added, other.added)
                && Objects.equals(removed, other.removed);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + Objects.hash(numberInProject, field, author, added, removed);
        }

        @Override
        public String toString() {
            return "CustomFieldEvent{" +
                "timestamp=" + timestamp +
                ", numberInProject=" + numberInProject +
                ", field='" + field + '\'' +
                ", author='" + author + '\'' +
                ", added='" + added + '\'' +
                ", removed='" + removed + '\'' +
                '}';
        }
    }
}
