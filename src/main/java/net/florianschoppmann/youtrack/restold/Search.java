package net.florianschoppmann.youtrack.restold;

import java.util.Objects;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Search {
    private String name;
    private String visibleForGroup;
    private String updatableByGroup;
    private String untagOnResolve;

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

    @XmlAttribute
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlAttribute
    public String getVisibleForGroup() {
        return visibleForGroup;
    }

    public void setVisibleForGroup(String visibleForGroup) {
        this.visibleForGroup = visibleForGroup;
    }

    @XmlAttribute
    public String getUpdatableByGroup() {
        return updatableByGroup;
    }

    public void setUpdatableByGroup(String updatableByGroup) {
        this.updatableByGroup = updatableByGroup;
    }

    @XmlAttribute
    public String getUntagOnResolve() {
        return untagOnResolve;
    }

    public void setUntagOnResolve(String untagOnResolve) {
        this.untagOnResolve = untagOnResolve;
    }
}
