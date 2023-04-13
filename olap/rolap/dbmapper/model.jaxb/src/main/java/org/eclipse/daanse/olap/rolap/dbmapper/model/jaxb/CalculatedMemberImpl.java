
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
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.CalculatedMember;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.CalculatedMemberProperty;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.CellFormatter;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Formula;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CalculatedMember", propOrder = { "annotations", "calculatedMemberProperty", "cellFormatter", "formulaElement" })
@XmlRootElement(name = "CalculatedMember")
public class CalculatedMemberImpl implements CalculatedMember {

    @XmlElement(name = "Annotation", type = AnnotationImpl.class)
    @XmlElementWrapper(name = "Annotations")
    protected List<Annotation> annotations;
    @XmlAttribute(name = "formula")
    protected String formula;
    @XmlElement(name = "CalculatedMemberProperty", type = CalculatedMemberPropertyImpl.class)
    protected List<CalculatedMemberProperty> calculatedMemberProperty;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "formatString")
    protected String formatString;
    @XmlAttribute(name = "caption")
    protected String caption;
    @XmlAttribute(name = "description")
    protected String description;
    @XmlAttribute(name = "dimension", required = true)
    protected String dimension;
    @XmlAttribute(name = "visible")
    protected Boolean visible;
    @XmlAttribute(name = "displayFolder")
    protected String displayFolder;
    @XmlAttribute(name = "hierarchy")
    protected  String hierarchy;
    @XmlAttribute(name = "parent")
    protected String parent;
    @XmlElement(name = "CellFormatter")
    protected CellFormatterImpl cellFormatter;
    @XmlElement(name = "Formula")
    protected FormulaImpl formulaElement;



    @Override
    public List<Annotation> annotations() {
        return annotations;
    }

    /**
     * Sets the value of the annotations property.
     *
     * @param value allowed object is {@link Annotation }
     *
     */
    public void setAnnotations(List<Annotation> value) {
        this.annotations = value;
    }

    /**
     * Gets the value of the formula property.
     *
     * @return possible object is {@link Object }
     *
     */
    @Override
    public String formula() {
        return formula;
    }

    /**
     * Sets the value of the formula property.
     *
     * @param formula allowed object is {@link Object }
     *
     */
    public void setFormula(String formula) {
        this.formula = formula;
    }

    /**
     * Gets the value of the calculatedMemberProperty property.
     *
     * <p>
     * This accessor method returns a reference to the live list, not a snapshot.
     * Therefore any modification you make to the returned list will be present
     * inside the Jakarta XML Binding object. This is why there is not a
     * <CODE>set</CODE> method for the calculatedMemberProperty property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getCalculatedMemberProperty().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CalculatedMemberPropertyImpl }
     *
     *
     */
    @Override
    public List<CalculatedMemberProperty> calculatedMemberProperty() {
        if (calculatedMemberProperty == null) {
            calculatedMemberProperty = new ArrayList<>();
        }
        return this.calculatedMemberProperty;
    }

    /**
     * Gets the value of the name property.
     *
     * @return possible object is {@link String }
     *
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is {@link String }
     *
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the formatString property.
     *
     * @return possible object is {@link String }
     *
     */
    @Override
    public String formatString() {
        return formatString;
    }

    /**
     * Sets the value of the formatString property.
     *
     * @param value allowed object is {@link String }
     *
     */
    public void setFormatString(String value) {
        this.formatString = value;
    }

    /**
     * Gets the value of the caption property.
     *
     * @return possible object is {@link String }
     *
     */
    @Override
    public String caption() {
        return caption;
    }

    /**
     * Sets the value of the caption property.
     *
     * @param value allowed object is {@link String }
     *
     */
    public void setCaption(String value) {
        this.caption = value;
    }

    /**
     * Gets the value of the description property.
     *
     * @return possible object is {@link String }
     *
     */
    @Override
    public String description() {
        return description;
    }

    /**
     * Sets the value of the description property.
     *
     * @param value allowed object is {@link String }
     *
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the dimension property.
     *
     * @return possible object is {@link String }
     *
     */
    @Override
    public String dimension() {
        return dimension;
    }

    /**
     * Sets the value of the dimension property.
     *
     * @param value allowed object is {@link String }
     *
     */
    public void setDimension(String value) {
        this.dimension = value;
    }

    /**
     * Gets the value of the visible property.
     *
     * @return possible object is {@link Boolean }
     *
     */
    @Override
    public boolean visible() {
        if (visible == null) {
            return true;
        } else {
            return visible;
        }
    }

    /**
     * Sets the value of the visible property.
     *
     * @param value allowed object is {@link Boolean }
     *
     */
    public void setVisible(Boolean value) {
        this.visible = value;
    }

    /**
     * Gets the value of the displayFolder property.
     *
     * @return possible object is {@link String }
     *
     */
    @Override
    public String displayFolder() {
        return displayFolder;
    }

    @Override
    public String hierarchy() {
        return hierarchy;
    }

    @Override
    public String parent() {
        return parent;
    }

    @Override
    public CellFormatter cellFormatter() {
        return cellFormatter;
    }

    public void setHierarchy(String hierarchy) {
        this.hierarchy = hierarchy;
    }

    /**
     * Sets the value of the displayFolder property.
     *
     * @param value allowed object is {@link String }
     *
     */
    public void setDisplayFolder(String value) {
        this.displayFolder = value;
    }

    @Override
    public Formula formulaElement() {
        return formulaElement;
    }
}
