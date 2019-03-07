package net.florianschoppmann;

import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ExportSettings {
    private String asanaWorkspace;
    private String asanaProject;
    private String youTrackProjectAbbrev;

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        ExportSettings other = (ExportSettings) otherObject;
        return Objects.equals(asanaWorkspace, other.asanaWorkspace)
            && Objects.equals(asanaProject, other.asanaProject)
            && Objects.equals(youTrackProjectAbbrev, other.youTrackProjectAbbrev);
    }

    @Override
    public int hashCode() {
        return Objects.hash(asanaWorkspace, asanaProject, youTrackProjectAbbrev);
    }

    public String getAsanaWorkspace() {
        return asanaWorkspace;
    }

    public void setAsanaWorkspace(String asanaWorkspace) {
        this.asanaWorkspace = asanaWorkspace;
    }

    public String getAsanaProject() {
        return asanaProject;
    }

    public void setAsanaProject(String asanaProject) {
        this.asanaProject = asanaProject;
    }

    public String getYouTrackProjectAbbrev() {
        return youTrackProjectAbbrev;
    }

    public void setYouTrackProjectAbbrev(String youTrackProjectAbbrev) {
        this.youTrackProjectAbbrev = youTrackProjectAbbrev;
    }
}
