package net.florianschoppmann.issuetracking.youtrack.restold;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

@XmlRootElement
public class Error {
    @XmlValue
    public @Nullable String value;

    @Override
    public boolean equals(Object otherObject) {
        return this == otherObject
            || (otherObject instanceof Error && Objects.equals(value, ((Error) otherObject).value));
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
