package net.florianschoppmann.youtrack.restold;

import java.util.Objects;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

@XmlRootElement
public class Error {
    @XmlValue
    private String value;

    @Override
    public boolean equals(Object otherObject) {
        return this == otherObject
            || (otherObject instanceof Error && Objects.equals(value, ((Error) otherObject).value));
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Nullable
    public String getValue() {
        return value;
    }

    public void setValue(@Nullable String value) {
        this.value = value;
    }
}
