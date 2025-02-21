package org.eclipse.daanse.olap.impl;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.result.Datatype;
import org.eclipse.daanse.olap.api.result.IMondrianOlap4jProperty;
import org.eclipse.daanse.olap.api.result.Property;

import mondrian.olap.StandardProperty;

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

         Map<String,StandardProperty> map=    StandardProperty.STANDARD_PROPERTIES
        .stream().sorted((p1,p2)->p1.getName().compareTo(p2.getName())).collect(Collectors.toMap(p-> p.getName(),p->p));
      
        for (Map.Entry<String,StandardProperty> e
            : map.entrySet()
            )
        {
            mondrian.olap.AbstractProperty o = e.getValue();
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

    final org.eclipse.daanse.olap.api.element.Property property;

    org.eclipse.daanse.olap.api.element.Level level;

    PropertyImpl(org.eclipse.daanse.olap.api.element.Property property) {
        this.property = property;
    }

    PropertyImpl(org.eclipse.daanse.olap.api.element.Property property, org.eclipse.daanse.olap.api.element.Level level) {
        this(property);
        this.level = level;
    }


    @Override
    public String getName() {
        return property.getName();
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
