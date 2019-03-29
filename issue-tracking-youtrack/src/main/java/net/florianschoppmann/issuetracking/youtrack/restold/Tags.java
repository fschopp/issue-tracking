package net.florianschoppmann.issuetracking.youtrack.restold;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Tags {
    @XmlElement
    public final List<Search> search = new ArrayList<>();
}
