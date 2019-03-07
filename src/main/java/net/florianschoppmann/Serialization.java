package net.florianschoppmann;

import net.florianschoppmann.asana.ExportWarnings;
import net.florianschoppmann.conversion.ConversionWarnings;
import net.florianschoppmann.youtrack.Attachments;
import net.florianschoppmann.youtrack.TagsForIssues;
import net.florianschoppmann.youtrack.restold.ImportReport;
import net.florianschoppmann.youtrack.restold.Issues;
import net.florianschoppmann.youtrack.restold.List;
import org.eclipse.persistence.jaxb.JAXBContextFactory;

import java.nio.file.Path;
import java.util.Objects;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

final class Serialization {
    private final Path basePath;
    private final Marshaller marshaller;
    private final Unmarshaller unmarshaller;

    private Serialization(Path basePath, Marshaller marshaller, Unmarshaller unmarshaller) {
        this.basePath = Objects.requireNonNull(basePath);
        this.marshaller = Objects.requireNonNull(marshaller);
        this.unmarshaller = Objects.requireNonNull(unmarshaller);
    }

    static Serialization defaultSerialization(Path basePath) {
        try {
            JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class<?>[]{Issues.class, List.class,
                Attachments.class, TagsForIssues.class, ExportWarnings.class, ConversionWarnings.class,
                ImportReport.class, ExportSettings.class}, null);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return new Serialization(basePath, marshaller, unmarshaller);
        } catch (JAXBException exception) {
            throw new IllegalStateException(exception);
        }
    }

    <T> T readResult(Class<? extends T> clazz) throws JAXBException {
        Object object = unmarshaller.unmarshal(basePath.resolve(clazz.getSimpleName() + ".xml").toFile());
        if (clazz.isInstance(object)) {
            @SuppressWarnings("unchecked")
            T typedObject = (T) object;
            return typedObject;
        }
        throw new JAXBException(String.format("Result (%s) was not of the expected type (%s).", object, clazz));
    }

    <T> void writeResult(T result) throws JAXBException  {
        marshaller.marshal(result, basePath.resolve(result.getClass().getSimpleName() + ".xml").toFile());
    }
}
