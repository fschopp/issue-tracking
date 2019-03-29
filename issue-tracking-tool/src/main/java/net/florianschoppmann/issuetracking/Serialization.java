package net.florianschoppmann.issuetracking;

import net.florianschoppmann.issuetracking.asana.AsanaExportWarnings;
import net.florianschoppmann.issuetracking.conversion.ConversionWarnings;
import net.florianschoppmann.issuetracking.youtrack.Attachments;
import net.florianschoppmann.issuetracking.youtrack.CommentUpdates;
import net.florianschoppmann.issuetracking.youtrack.Events;
import net.florianschoppmann.issuetracking.youtrack.IssueUpdates;
import net.florianschoppmann.issuetracking.youtrack.restold.ImportReport;
import net.florianschoppmann.issuetracking.youtrack.restold.Issues;
import net.florianschoppmann.issuetracking.youtrack.restold.List;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.JAXBContextProperties;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.eclipse.persistence.jaxb.UnmarshallerProperties;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

final class Serialization {
    private static final String XML_SUFFIX = ".xml";
    private static final String JSON_SUFFIX = ".json";

    private final Path basePath;
    private final Marshaller xmlMarshaller;
    private final Marshaller jsonMarshaller;
    private final Unmarshaller xmlUnmarshaller;
    private final Unmarshaller jsonUnmarshaller;

    private Serialization(Path basePath, Marshaller xmlMarshaller, Marshaller jsonMarshaller,
            Unmarshaller xmlUnmarshaller, Unmarshaller jsonUnmarshaller) {
        this.basePath = basePath;
        this.xmlMarshaller = xmlMarshaller;
        this.jsonMarshaller = jsonMarshaller;
        this.xmlUnmarshaller = xmlUnmarshaller;
        this.jsonUnmarshaller = jsonUnmarshaller;
    }

    static Serialization defaultSerialization(Path basePath) {
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put(JAXBContextProperties.JSON_INCLUDE_ROOT, false);
            properties.put(JAXBContextProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);
            JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class<?>[]{Issues.class, IssueUpdates.class,
                CommentUpdates.class, List.class, Attachments.class, AsanaExportWarnings.class,
                ConversionWarnings.class, Events.class, ImportReport.class, ImportSettings.class}, properties);

            Marshaller xmlMarshaller = jaxbContext.createMarshaller();
            xmlMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            Marshaller jsonMarshaller = jaxbContext.createMarshaller();
            jsonMarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
            jsonMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            Unmarshaller xmlUnmarshaller = jaxbContext.createUnmarshaller();
            Unmarshaller jsonUnmarshaller = jaxbContext.createUnmarshaller();
            jsonUnmarshaller.setProperty(UnmarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);

            return new Serialization(basePath, xmlMarshaller, jsonMarshaller, xmlUnmarshaller, jsonUnmarshaller);
        } catch (JAXBException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private <T> T readResult(Unmarshaller unmarshaller, String fileSuffix, Class<? extends T> clazz)
            throws JAXBException {
        Object object = unmarshaller.unmarshal(basePath.resolve(clazz.getSimpleName() + fileSuffix).toFile());
        if (clazz.isInstance(object)) {
            @SuppressWarnings("unchecked")
            T typedObject = (T) object;
            return typedObject;
        }
        throw new JAXBException(String.format("Result (%s) was not of the expected type (%s).", object, clazz));
    }

    <T> T readResultXml(Class<? extends T> clazz) throws JAXBException  {
        return readResult(xmlUnmarshaller, XML_SUFFIX, clazz);
    }

    <T> T readResultJson(Class<? extends T> clazz) throws JAXBException  {
        return readResult(jsonUnmarshaller, JSON_SUFFIX, clazz);
    }

    private <T> void writeResult(Marshaller marshaller, String fileSuffix, T result) throws JAXBException {
        marshaller.marshal(result, basePath.resolve(result.getClass().getSimpleName() + fileSuffix).toFile());
    }

    <T> void writeResultXml(T result) throws JAXBException  {
        writeResult(xmlMarshaller, XML_SUFFIX, result);
    }

    <T> void writeResultJson(T result) throws JAXBException  {
        writeResult(jsonMarshaller, JSON_SUFFIX, result);
    }
}
