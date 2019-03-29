package net.florianschoppmann.issuetracking;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ImportSettings {
    public @Nullable String youTrackProjectAbbrev;
    public boolean importIssues;
    public boolean importLinks;
    public boolean importAttachments;
    public boolean importEvents;
    public boolean updateIssues;
    public boolean updateComments;

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        ImportSettings other = (ImportSettings) otherObject;
        return Objects.equals(youTrackProjectAbbrev, other.youTrackProjectAbbrev)
            && Objects.equals(importIssues, other.importIssues)
            && Objects.equals(importLinks, other.importLinks)
            && Objects.equals(importAttachments, other.importAttachments)
            && Objects.equals(importEvents, other.importEvents)
            && Objects.equals(updateIssues, other.updateIssues)
            && Objects.equals(updateComments, other.updateComments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(youTrackProjectAbbrev, importIssues, importLinks, importAttachments, importEvents,
            updateIssues, updateComments);
    }
}
