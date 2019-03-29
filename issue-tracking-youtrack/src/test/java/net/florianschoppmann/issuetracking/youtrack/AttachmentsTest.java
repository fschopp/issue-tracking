package net.florianschoppmann.issuetracking.youtrack;

import org.testng.annotations.Factory;

import java.time.Instant;
import javax.xml.bind.JAXBException;

public class AttachmentsTest {
    private static Attachments createTestInstance() {
        Attachments attachments = new Attachments();

        Attachments.Attachment firstAttachment = new Attachments.Attachment();
        firstAttachment.taskNumberInProject = 2;
        firstAttachment.authorLogin = "foo";
        firstAttachment.created = Instant.now().toEpochMilli();
        firstAttachment.path = "/path/to/nowhere";
        attachments.attachments.add(firstAttachment);

        Attachments.Attachment secondAttachment = new Attachments.Attachment();
        secondAttachment.taskNumberInProject = 3;
        secondAttachment.authorLogin = "bar";
        secondAttachment.created = (long) 24;
        secondAttachment.path = "/another/path";
        attachments.attachments.add(secondAttachment);

        return attachments;
    }

    @Factory
    public Object[] contractTest() throws JAXBException  {
        return new Object[] {
            new SimpleMarshalingContract(createTestInstance())
        };
    }

}
