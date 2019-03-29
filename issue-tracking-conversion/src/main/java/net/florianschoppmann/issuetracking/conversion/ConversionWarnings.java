package net.florianschoppmann.issuetracking.conversion;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConversionWarnings {
    /**
     * List of email addresses that cannot be mapped to login names in the target issue tracking system.
     */
    @XmlElementWrapper
    @XmlElement(name = "missingUser")
    public final List<MissingUser> missingUsers = new ArrayList<>();

    /**
     * List of issue keys that were already expected to be present in the target issue tracking system.
     */
    @XmlElementWrapper
    @XmlElement(name = "missingIssueKey")
    public final List<String> missingIssueKeys = new ArrayList<>();

    /**
     * List of issues where the issue summary has changed from the source to the target issue tracking system.
     */
    @XmlElementWrapper
    @XmlElement(name = "changedIssueSummary")
    public final List<ChangedIssueSummary> changedIssueSummaries = new ArrayList<>();

    /**
     * List of missing projects in the target issue tracking system.
     */
    @XmlElementWrapper
    @XmlElement(name = "missingProject")
    public final List<MissingProject> missingProjects = new ArrayList<>();
}
