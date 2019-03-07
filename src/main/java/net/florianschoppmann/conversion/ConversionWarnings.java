package net.florianschoppmann.conversion;

import net.florianschoppmann.asana.ExportWarnings.ReferencingTask;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConversionWarnings {
    private final List<MissingEmail> missingLoginMapping = new ArrayList<>();
    private final List<ReferencingTask> tasksWithoutCreator = new ArrayList<>();

    @XmlElement
    public List<MissingEmail> getMissingLoginMapping() {
        return missingLoginMapping;
    }

    @XmlElement
    public List<ReferencingTask> getTasksWithoutCreator() {
        return tasksWithoutCreator;
    }
}
