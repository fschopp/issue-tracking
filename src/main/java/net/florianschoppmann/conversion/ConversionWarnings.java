package net.florianschoppmann.conversion;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConversionWarnings {
    private final List<MissingEmail> missingLoginMapping = new ArrayList<>();

    @XmlElement
    public List<MissingEmail> getMissingLoginMapping() {
        return missingLoginMapping;
    }
}
