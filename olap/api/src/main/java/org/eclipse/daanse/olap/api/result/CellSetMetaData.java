package org.eclipse.daanse.olap.api.result;

import java.util.List;

import org.eclipse.daanse.olap.api.element.Cube;

public interface CellSetMetaData {
    List<Property> getCellProperties();

    /**
     * Returns the Cube which was referenced in this statement.
     *
     * @return cube referenced in this statement
     */
    Cube getCube();

    /**
     * Returns a list of CellSetAxisMetaData describing each result axis.
     *
     * @return list of metadata describing each result axis
     */
    List<CellSetAxisMetaData> getAxesMetaData();

    /**
     * Returns a CellSetAxisMetaData describing the filter axis. Never returns
     * null; if the MDX statement contains no WHERE clause, the description of
     * the filter contains no hierarchies.
     *
     * @return metadata describing filter axis
     */
    CellSetAxisMetaData getFilterAxisMetaData();
}
