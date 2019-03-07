package net.florianschoppmann.conversion;

import net.florianschoppmann.asana.ExportWarnings.ReferencingTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;

public class MissingEmail {
    private String email;
    private List<ReferencingTask> referencingTask;

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        MissingEmail other = (MissingEmail) otherObject;
        return Objects.equals(email, other.email)
            && Objects.equals(referencingTask, other.referencingTask);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, referencingTask);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @XmlElement
    public List<ReferencingTask> getReferencingTask() {
        if (referencingTask == null) {
            referencingTask = new ArrayList<>();
        }
        return referencingTask;
    }
}
