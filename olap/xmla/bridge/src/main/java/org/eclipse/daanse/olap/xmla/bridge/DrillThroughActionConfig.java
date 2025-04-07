package org.eclipse.daanse.olap.xmla.bridge;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.List;

@ObjectClassDefinition()
public interface DrillThroughActionConfig extends AbstractActionConfig {

    @AttributeDefinition(name = "%COLUMNS", required = false)
    default List<String> columns() {
        return List.of();
    }
}
