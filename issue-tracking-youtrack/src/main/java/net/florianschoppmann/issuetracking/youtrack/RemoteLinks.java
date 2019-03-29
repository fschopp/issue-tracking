package net.florianschoppmann.issuetracking.youtrack;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RemoteLinks {
    @XmlElement(name = "remoteLink")
    public final List<RemoteLink> remoteLinks = new ArrayList<>();

    @Override
    public boolean equals(Object otherObject) {
        return this == otherObject
            || (otherObject instanceof RemoteLinks
                && Objects.equals(remoteLinks, ((RemoteLinks) otherObject).remoteLinks));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(remoteLinks);
    }

    public static class RemoteLink {
        public @Nullable String iconUrl;
        public @Nullable String title;
        public @Nullable String url;

        @Override
        public boolean equals(Object otherObject) {
            if (this == otherObject) {
                return true;
            } else if (otherObject == null || getClass() != otherObject.getClass()) {
                return false;
            }

            RemoteLink other = (RemoteLink) otherObject;
            return Objects.equals(iconUrl, other.iconUrl)
                && Objects.equals(title, other.title)
                && Objects.equals(url, other.url);
        }

        @Override
        public int hashCode() {
            return Objects.hash(iconUrl, title, url);
        }
    }
}
