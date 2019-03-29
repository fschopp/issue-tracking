package net.florianschoppmann.issuetracking.youtrack;

import net.florianschoppmann.issuetracking.youtrack.Events.CustomFieldEvent;
import org.testng.annotations.Factory;

import java.time.Instant;
import javax.xml.bind.JAXBException;

public class EventsTest {
    private static Events createTestInstance() {
        Events events = new Events();

        CustomFieldEvent firstEvent = new CustomFieldEvent();
        firstEvent.timestamp = 2L;
        firstEvent.numberInProject = 1L;
        firstEvent.field = "State";
        firstEvent.author = "me";
        firstEvent.added = "In Progress";
        firstEvent.removed = "Open";
        events.events.add(firstEvent);

        return events;
    }

    @Factory
    public Object[] contractTest() throws JAXBException  {
        return new Object[] {
            new SimpleMarshalingContract(createTestInstance())
        };
    }

}
