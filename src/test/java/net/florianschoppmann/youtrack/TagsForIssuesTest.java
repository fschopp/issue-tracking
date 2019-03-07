package net.florianschoppmann.youtrack;

import net.florianschoppmann.youtrack.rest.Issue;
import net.florianschoppmann.youtrack.rest.IssueTag;
import org.testng.annotations.Factory;

import java.util.Arrays;
import javax.xml.bind.JAXBException;

public class TagsForIssuesTest {
    private static TagsForIssues createTestInstance() {
        TagsForIssues tagsForIssues = new TagsForIssues();

        var firstIssue = new Issue();
        firstIssue.setId("AB-23");
        for (String tag : Arrays.asList("urgent", "high priority", "immediate")) {
            var issueTag = new IssueTag();
            issueTag.setName(tag);
            firstIssue.getTags().add(issueTag);
        }
        tagsForIssues.getIssues().add(firstIssue);

        var secondIssue = new Issue();
        secondIssue.setId("XY-1");
        for (String tag : Arrays.asList("simple", "easy")) {
            var issueTag = new IssueTag();
            issueTag.setName(tag);
            secondIssue.getTags().add(issueTag);
        }
        tagsForIssues.getIssues().add(secondIssue);

        return tagsForIssues;
    }

    @Factory
    public Object[] contractTest() throws JAXBException  {
        return new Object[] {
            new SimpleMarshalingContract(createTestInstance())
        };
    }
}
