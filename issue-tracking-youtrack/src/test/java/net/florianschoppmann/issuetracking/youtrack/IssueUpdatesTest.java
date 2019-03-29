package net.florianschoppmann.issuetracking.youtrack;

import net.florianschoppmann.issuetracking.youtrack.rest.Issue;
import net.florianschoppmann.issuetracking.youtrack.rest.IssueTag;
import org.testng.annotations.Factory;

import java.util.ArrayList;
import java.util.Arrays;
import javax.xml.bind.JAXBException;

public class IssueUpdatesTest {
    private static IssueUpdates createTestInstance() {
        var issueUpdates = new IssueUpdates();

        var firstIssueUpdate = new IssueUpdates.IssueUpdate();
        firstIssueUpdate.issueKey = "AB-23";
        var firstIssue = new Issue();
        firstIssue.numberInProject = 1;
        firstIssue.description = "Some length description.";
        firstIssue.summary = "Some summary";
        for (String tag : Arrays.asList("urgent", "high priority", "immediate")) {
            var issueTag = new IssueTag();
            issueTag.name = tag;
            firstIssue.tags = new ArrayList<>();
            firstIssue.tags.add(issueTag);
        }
        firstIssueUpdate.issue = firstIssue;
        issueUpdates.issueUpdates.add(firstIssueUpdate);

        var secondIssueUpdate = new IssueUpdates.IssueUpdate();
        secondIssueUpdate.issueKey = "XY-1";
        var secondIssue = new Issue();
        for (String tag : Arrays.asList("simple", "easy")) {
            var issueTag = new IssueTag();
            issueTag.name = tag;
            secondIssue.tags = new ArrayList<>();
            secondIssue.tags.add(issueTag);
        }
        secondIssueUpdate.issue = secondIssue;
        issueUpdates.issueUpdates.add(secondIssueUpdate);

        return issueUpdates;
    }

    @Factory
    public Object[] contractTest() throws JAXBException  {
        return new Object[] {
            new SimpleMarshalingContract(createTestInstance())
        };
    }
}
