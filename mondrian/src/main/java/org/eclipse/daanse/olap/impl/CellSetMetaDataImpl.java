package org.eclipse.daanse.olap.impl;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.result.CellSetAxisMetaData;
import org.eclipse.daanse.olap.api.result.CellSetMetaData;
import org.eclipse.daanse.olap.api.result.Property;

public class CellSetMetaDataImpl implements CellSetMetaData {

    private final Statement statement;
    private final Query query;

    private final List<CellSetAxisMetaData> axesMetaData =  new ArrayList<>();
    private final CellSetAxisMetaData filterAxisMetaData;


    public CellSetMetaDataImpl(Statement statement, Query query) {
        this.statement = statement;
        this.query = query;
        for (final QueryAxis queryAxis : query.getAxes()) {
            axesMetaData.add(
                new CellSetAxisMetaDataImpl(
                    this, queryAxis));
        }
        filterAxisMetaData =
            new CellSetAxisMetaDataImpl(
                this, query.getSlicerAxis());

    }

    @Override
    public List<Property> getCellProperties() {
        final List<Property> list = new ArrayList<>();
        for (Property property : Property.StandardCellProperty.values()) {
            if (query.hasCellProperty(property.getName())) {
                list.add(property);
            }
        }
        for (Property property
            : PropertyImpl.CELL_EXTENSIONS.values())
        {
            if (query.hasCellProperty(property.getName())) {
                list.add(property);
            }
        }
        return list;

    }

    @Override
    public Cube getCube() {
        return query.getCube();
    }

    @Override
    public List<CellSetAxisMetaData> getAxesMetaData() {
        return axesMetaData;
    }

    @Override
    public CellSetAxisMetaData getFilterAxisMetaData() {
        return filterAxisMetaData;
    }

    public Query getQuery() {
        return query;
    }
}
