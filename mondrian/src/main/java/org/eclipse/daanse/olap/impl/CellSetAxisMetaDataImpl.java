package org.eclipse.daanse.olap.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.daanse.olap.api.SubtotalVisibility;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.query.component.AxisOrdinal;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Id;
import org.eclipse.daanse.olap.api.query.component.LevelExpression;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.result.CellSetAxisMetaData;
import org.eclipse.daanse.olap.api.result.CellSetMetaData;
import org.eclipse.daanse.olap.api.result.IAxis;
import org.eclipse.daanse.olap.api.result.Property;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.query.component.QueryAxisImpl;
import org.eclipse.daanse.olap.query.component.UnresolvedFunCallImpl;
import org.eclipse.daanse.olap.util.type.TypeUtil;

public class CellSetAxisMetaDataImpl implements CellSetAxisMetaData {

    private final QueryAxis queryAxis;
    private final CellSetMetaData cellSetMetaData;
    private final List<Property> propertyList = new ArrayList<>();
    static final Map<String, Property> MEMBER_EXTENSIONS =
        new LinkedHashMap<>();



    public CellSetAxisMetaDataImpl(CellSetMetaDataImpl cellSetMetaData, QueryAxis queryAxis) {
        if (queryAxis == null) {
            queryAxis = new QueryAxisImpl(
                false, null, AxisOrdinal.StandardAxisOrdinal.SLICER,
                SubtotalVisibility.Undefined);
        }
        this.queryAxis = queryAxis;
        this.cellSetMetaData = cellSetMetaData;

        // populate property list
        for (Id id : queryAxis.getDimensionProperties()) {
            final String[] names = id.toStringArray();
            Property olap4jProperty = null;
            if (names.length == 1) {
                if(names[0].equals("MEMBER_VALUE")) {
                    //TODO:
                    continue;
                }
                olap4jProperty =
                    Util.lookup(
                        Property.StandardMemberProperty.class, names[0]);
                if (olap4jProperty == null) {
                    olap4jProperty =
                        MEMBER_EXTENSIONS.get(names[0]);
                }
            }
            if (olap4jProperty == null) {
                final UnresolvedFunCallImpl call =
                    (UnresolvedFunCallImpl)
                        Util.lookup(
                            cellSetMetaData.getQuery(), id.getSegments(), true);
                Level level = ((LevelExpression) call.getArg(0)).getLevel();
                olap4jProperty =
                    new PropertyImpl(
                        Util.lookupProperty(level, call.getOperationAtom().name()), level);
            }
            propertyList.add(olap4jProperty);
        }


    }

    @Override
    public IAxis getAxisOrdinal() {
        return IAxis.Factory.forOrdinal(
            queryAxis.getAxisOrdinal().logicalOrdinal());
    }

    @Override
    public List<Hierarchy> getHierarchies() {
        return getHierarchiesNonFilter();
    }

    @Override
    public List<Property> getProperties() {
        return propertyList;
    }


    /**
     * Returns the hierarchies on a non-filter axis.
     *
     * @return List of hierarchies, never null
     */
    private List<Hierarchy> getHierarchiesNonFilter() {
        final Expression exp = queryAxis.getSet();
        if (exp == null) {
            return Collections.emptyList();
        }
        List<Hierarchy> hierarchyList = new ArrayList<>();
        hierarchyList.addAll(TypeUtil.getHierarchies(exp.getType()));
        return hierarchyList;
    }

}
