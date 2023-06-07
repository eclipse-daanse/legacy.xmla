/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.olap.rolap.dbmapper.model.jaxb;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Action;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Annotation;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.CalculatedMember;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Cube;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.CubeDimension;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.DrillThroughAction;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Measure;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.NamedSet;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Relation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.WritebackTable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Cube", propOrder = { "fact", "dimensionUsageOrDimensions", "measures", "annotations",
        "calculatedMembers", "namedSets", "drillThroughActions", "writebackTables", "actions"})
@XmlRootElement(name = "Cube")
public class CubeImpl implements Cube {

    @XmlElementWrapper(name = "Annotations")
    @XmlElement(name = "Annotation", type = AnnotationImpl.class)
    protected List<Annotation> annotations;
    @XmlElements({ @XmlElement(name = "DimensionUsage", type = DimensionUsageImpl.class),
            @XmlElement(name = "Dimension", type = PrivateDimensionImpl.class) })
    protected List<CubeDimension> dimensionUsageOrDimensions;
    @XmlElement(name = "Measure", required = true, type = MeasureImpl.class)
    protected List<Measure> measures;
    @XmlElement(name = "CalculatedMember",  type = CalculatedMemberImpl.class)
    protected List<CalculatedMember> calculatedMembers;
    @XmlElement(name = "NamedSet", type = NamedSetImpl.class)
    protected List<NamedSet> namedSets;
    @XmlElement(name = "DrillThroughAction", type = DrillThroughActionImpl.class)
    protected List<DrillThroughAction> drillThroughActions;
    @XmlElement(name = "WritebackTable", type = WritebackTableImpl.class)
    protected List<WritebackTable> writebackTables;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "caption")
    protected String caption;
    @XmlAttribute(name = "description")
    protected String description;
    @XmlAttribute(name = "defaultMeasure")
    protected String defaultMeasure;
    @XmlAttribute(name = "cache")
    protected Boolean cache;
    @XmlAttribute(name = "enabled")
    protected Boolean enabled;
    @XmlAttribute(name = "visible")
    protected boolean visible = true;
    @XmlElements({ @XmlElement(name = "InlineTable", type = InlineTableImpl.class),
        @XmlElement(name = "Table", type = TableImpl.class), @XmlElement(name = "View", type = ViewImpl.class)})
    protected Relation fact;
    @XmlElement(name = "Action", type = ActionImpl.class)
    protected List<Action> actions;

    @Override
    public List<Annotation> annotations() {
        return annotations;
    }

    public void setAnnotations(List<Annotation> value) {
        this.annotations = value;
    }

    @Override
    public List<CubeDimension> dimensionUsageOrDimensions() {
        if (dimensionUsageOrDimensions == null) {
            dimensionUsageOrDimensions = new ArrayList<>();
        }
        return this.dimensionUsageOrDimensions;
    }

    @Override
    public List<Measure> measures() {
        if (measures == null) {
            measures = new ArrayList<>();
        }
        return this.measures;
    }

    @Override
    public List<CalculatedMember> calculatedMembers() {
        if (calculatedMembers == null) {
            calculatedMembers = new ArrayList<>();
        }
        return this.calculatedMembers;
    }

    @Override
    public List<NamedSet> namedSets() {
        if (namedSets == null) {
            namedSets = new ArrayList<>();
        }
        return this.namedSets;
    }

    @Override
    public List<DrillThroughAction> drillThroughActions() {
        if (drillThroughActions == null) {
            drillThroughActions = new ArrayList<>();
        }
        return this.drillThroughActions;
    }

    @Override
    public String name() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    @Override
    public String caption() {
        return caption;
    }

    public void setCaption(String value) {
        this.caption = value;
    }

    @Override
    public String description() {
        return description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    @Override
    public String defaultMeasure() {
        return defaultMeasure;
    }

    public void setDefaultMeasure(String value) {
        this.defaultMeasure = value;
    }

    @Override
    public boolean cache() {
        if (cache == null) {
            return true;
        } else {
            return cache;
        }
    }

    public void setCache(Boolean value) {
        this.cache = value;
    }

    @Override
    public boolean enabled() {
        if (enabled == null) {
            return true;
        } else {
            return enabled;
        }
    }

    public void setEnabled(Boolean value) {
        this.enabled = value;
    }

    @Override
    public List<WritebackTable> writebackTables() {
        if (writebackTables == null) {
            writebackTables = new ArrayList<>();
        }
        return this.writebackTables;
    }

    @Override
    public boolean visible() {
        return visible;
    }

    @Override
    public Relation fact() {
        return fact;
    }

    public void setFact(Relation fact) {
        this.fact = fact;
    }

    @Override
    public List<Action> actions() {
        if (actions == null) {
            actions = new ArrayList<>();
        }
        return actions;
    }

    public void setDimensionUsageOrDimensions(List<CubeDimension> dimensionUsageOrDimensions) {
        this.dimensionUsageOrDimensions = dimensionUsageOrDimensions;
    }

    public void setMeasures(List<Measure> measures) {
        this.measures = measures;
    }

    public void setCalculatedMembers(List<CalculatedMember> calculatedMembers) {
        this.calculatedMembers = calculatedMembers;
    }

    public void setNamedSets(List<NamedSet> namedSets) {
        this.namedSets = namedSets;
    }

    public void setDrillThroughActions(List<DrillThroughAction> drillThroughActions) {
        this.drillThroughActions = drillThroughActions;
    }

    public void setWritebackTables(List<WritebackTable> writebackTables) {
        this.writebackTables = writebackTables;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }
}
