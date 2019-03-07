package net.florianschoppmann.youtrack;

import net.florianschoppmann.youtrack.rest.Issue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TagsForIssues {
    private List<Issue> issue;

    @Override
    public boolean equals(Object otherObject) {
        return this == otherObject
            || (otherObject instanceof TagsForIssues
                && Objects.equals(issue, ((TagsForIssues) otherObject).issue));
    }

    @Override
    public int hashCode() {
        return Objects.hash(issue);
    }

    @XmlElement
    public List<Issue> getIssues() {
        if (issue == null) {
            issue = new ArrayList<>();
        }
        return issue;
    }
}
