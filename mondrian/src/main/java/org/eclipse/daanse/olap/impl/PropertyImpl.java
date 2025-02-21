package org.eclipse.daanse.olap.impl;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.result.Datatype;
import org.eclipse.daanse.olap.api.result.IMondrianOlap4jProperty;
import org.eclipse.daanse.olap.api.result.Property;

public class PropertyImpl implements IMondrianOlap4jProperty {

    /**
     * Map of member properties that are built into Mondrian but are not in the
     * olap4j standard.
     */
    static final Map<String, Property> MEMBER_EXTENSIONS =
        new LinkedHashMap<>();

    /**
     * Map of cell properties that are built into Mondrian but are not in the
     * olap4j standard.
     */
    static final Map<String, Property> CELL_EXTENSIONS =
        new LinkedHashMap<>();

    static {
        // Build set of names of olap4j standard member properties.
        final Set<String> memberNames = new HashSet<>();
        for (Property property : Property.StandardMemberProperty.values()) {
            memberNames.add(property.getName());
        }

        final Set<String> cellNames = new HashSet<>();
        for (Property property : Property.StandardCellProperty.values()) {
            cellNames.add(property.getName());
        }

        for (Map.Entry<String, mondrian.olap.Property> e
            : mondrian.olap.Property.properiesMap.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey()).toList()
            )
        {
            mondrian.olap.Property o = e.getValue();
            if (o.isMemberProperty()
                && !memberNames.contains(o.getName()))
            {
                MEMBER_EXTENSIONS.put(
                    o.getName(),
                    new PropertyImpl(o));
            }
            if (o.isCellProperty()
                && !cellNames.contains(o.getName()))
            {
                CELL_EXTENSIONS.put(
                    o.getName(),
                    new PropertyImpl(o));
            }
        }
    }

    final mondrian.olap.Property property;

    org.eclipse.daanse.olap.api.element.Level level;

    PropertyImpl(mondrian.olap.Property property) {
        this.property = property;
    }

    PropertyImpl(mondrian.olap.Property property, org.eclipse.daanse.olap.api.element.Level level) {
        this(property);
        this.level = level;
    }


    @Override
    public String getName() {
        return property.name;
    }

    @Override
    public Datatype getDatatype() {
        switch (property.getType()) {
            case TYPE_BOOLEAN:
                return Datatype.BOOLEAN;
            case TYPE_NUMERIC:
                return Datatype.DOUBLE;
            case TYPE_INTEGER:
                return Datatype.INTEGER;
            case TYPE_LONG:
                return Datatype.LARGE_INTEGER;
            case TYPE_STRING:
                return Datatype.STRING;
            case TYPE_OTHER:
                return Datatype.VARIANT;
            default:
                throw new RuntimeException("unexpected: " + property.getType());
        }
    }

    @Override
    public Set<TypeFlag> getType() {
        return property.isCellProperty()
            ?Property.TypeFlag.CELL_TYPE_FLAG
            :Property.TypeFlag.MEMBER_TYPE_FLAG;
    }

    @Override
    public String getCaption() {
        // todo: i18n
        return property.getCaption();
    }

    @Override
    public Level getLevel() {
        return level;
    }
}
