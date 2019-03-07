package net.florianschoppmann.youtrack;

import net.florianschoppmann.youtrack.Attachments.Attachment;
import org.testng.annotations.Factory;

import java.time.Instant;
import javax.xml.bind.JAXBException;

public class AttachmentsTest {
    private static Attachments createTestInstance() {
        Attachments attachments = new Attachments();

        Attachment firstAttachment = new Attachment();
        firstAttachment.setTaskNumberInProject(2);
        firstAttachment.setAuthorLogin("foo");
        firstAttachment.setCreated(Instant.now().toEpochMilli());
        firstAttachment.setPath("/path/to/nowhere");
        attachments.getAttachment().add(firstAttachment);

        Attachment secondAttachment = new Attachment();
        secondAttachment.setTaskNumberInProject(3);
        secondAttachment.setAuthorLogin("bar");
        secondAttachment.setCreated((long) 24);
        secondAttachment.setPath("/another/path");
        attachments.getAttachment().add(secondAttachment);

        return attachments;
    }

    @Factory
    public Object[] contractTest() throws JAXBException  {
        return new Object[] {
            new SimpleMarshalingContract(createTestInstance())
        };
    }

}
