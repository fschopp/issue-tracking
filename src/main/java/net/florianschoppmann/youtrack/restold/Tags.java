package net.florianschoppmann.youtrack.restold;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Tags {
    private final List<Search> search = new ArrayList<>();

    @XmlElement
    public List<Search> getSearch() {
        return search;
    }
}
