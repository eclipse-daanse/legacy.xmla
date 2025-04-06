package org.eclipse.daanse.olap.api.result;

import java.util.List;

import org.eclipse.daanse.olap.api.element.Hierarchy;

public interface CellSetAxisMetaData {
    IAxis getAxisOrdinal();

    List<Hierarchy> getHierarchies();

    List<Property> getProperties();
}
