package net.florianschoppmann.issuetracking.youtrack;

import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.JAXBContextProperties;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.eclipse.persistence.jaxb.UnmarshallerProperties;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

public class SimpleMarshalingContract {
    private final Object testInstance;
    private final JAXBContext moxyContext;

    SimpleMarshalingContract(Object testInstance) throws JAXBException {
        this.testInstance = testInstance;
        Map<String, Object> properties = new HashMap<>();
        properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, Boolean.FALSE);
        properties.put(JAXBContextProperties.JSON_WRAPPER_AS_ARRAY_NAME, Boolean.TRUE);
        moxyContext = JAXBContextFactory.createContext(new Class<?>[]{testInstance.getClass()}, properties);
    }

    private void roundtrip(Marshaller marshaller, Unmarshaller unmarshaller) throws JAXBException {
        StringWriter stringWriter = new StringWriter();

        marshaller.marshal(testInstance, stringWriter);
        Object deserializedObject;
        try (StringReader stringReader = new StringReader(stringWriter.toString())) {
            deserializedObject = unmarshaller
                .unmarshal(new StreamSource(stringReader), testInstance.getClass())
                .getValue();
        }
        Assert.assertEquals(deserializedObject, testInstance);
    }

    @Test
    public void xmlTest() throws JAXBException {
        Marshaller xmlMarshaller = moxyContext.createMarshaller();
        xmlMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        Unmarshaller xmlUnmarshaller = moxyContext.createUnmarshaller();
        roundtrip(xmlMarshaller, xmlUnmarshaller);
    }

    @Test
    public void jsonTest() throws JAXBException {
        Marshaller jsonMarshaller = moxyContext.createMarshaller();
        jsonMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jsonMarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
        Unmarshaller jsonUnmarshaller = moxyContext.createUnmarshaller();
        jsonUnmarshaller.setProperty(UnmarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
        roundtrip(jsonMarshaller, jsonUnmarshaller);
    }
}
