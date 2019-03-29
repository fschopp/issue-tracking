package net.florianschoppmann.issuetracking.youtrack.restold;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Search {
    public @Nullable String name;
    public @Nullable String visibleForGroup;
    public @Nullable String updatableByGroup;
    public @Nullable String untagOnResolve;

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        Search other = (Search) otherObject;
        return Objects.equals(name, other.name)
            && Objects.equals(visibleForGroup, other.visibleForGroup)
            && Objects.equals(updatableByGroup, other.updatableByGroup)
            && Objects.equals(untagOnResolve, other.untagOnResolve);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, visibleForGroup, updatableByGroup, untagOnResolve);
    }
}
