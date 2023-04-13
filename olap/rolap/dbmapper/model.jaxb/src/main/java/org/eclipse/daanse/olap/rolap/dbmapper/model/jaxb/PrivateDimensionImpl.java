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

import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Annotation;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Hierarchy;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.PrivateDimension;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.enums.DimensionTypeEnum;
import org.eclipse.daanse.olap.rolap.dbmapper.model.jaxb.adapter.DimensionTypeAdaptor;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PrivateDimension", propOrder = { "annotations", "hierarchy" })
@XmlRootElement(name = "Dimension")
public class PrivateDimensionImpl implements PrivateDimension {

    @XmlElement(name = "Annotation", type = AnnotationImpl.class)
    @XmlElementWrapper(name = "Annotations")
    protected List<Annotation> annotations;
    @XmlElement(name = "Hierarchy", required = true, type = HierarchyImpl.class)
    protected List<Hierarchy> hierarchy;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "type")
    @XmlJavaTypeAdapter(DimensionTypeAdaptor.class)
    protected DimensionTypeEnum type;
    @XmlAttribute(name = "caption")
    protected String caption;
    @XmlAttribute(name = "description")
    protected String description;
    @XmlAttribute(name = "foreignKey")
    protected String foreignKey;
    @XmlAttribute(name = "highCardinality")
    protected Boolean highCardinality;

    @XmlAttribute(name = "visible")
    private Boolean visible = true;
    @XmlAttribute(name = "usagePrefix")
    private String usagePrefix;

    @Override
    public List<Annotation> annotations() {
        if (annotations == null) {
            annotations = new ArrayList<>();
        }
        return this.annotations;
    }

    @Override
    public List<Hierarchy> hierarchy() {
        if (hierarchy == null) {
            hierarchy = new ArrayList<>();
        }
        return this.hierarchy;
    }

    @Override
    public String name() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    @Override
    public DimensionTypeEnum type() {
        return type;
    }

    public void setType(DimensionTypeEnum value) {
        this.type = value;
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
    public String foreignKey() {
        return foreignKey;
    }

    public void setForeignKey(String value) {
        this.foreignKey = value;
    }

    @Override
    public boolean highCardinality() {
        if (highCardinality == null) {
            return false;
        } else {
            return highCardinality;
        }
    }

    public void setHighCardinality(Boolean value) {
        this.highCardinality = value;
    }

    @Override
    public boolean visible() {
        return visible;
    }

    @Override
    public String usagePrefix() {
        return usagePrefix;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public void setHierarchy(List<Hierarchy> hierarchy) {
        this.hierarchy = hierarchy;
    }
}
