
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
package org.eclipse.daanse.olap.rolap.dbmapper.mondrian;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.rolap.dbmapper.api.CalculatedMember;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CalculatedMember", propOrder = { "annotations", "formula", "calculatedMemberProperty" })
public class CalculatedMemberImpl implements CalculatedMember {

    @XmlElement(name = "Annotation")
    @XmlElementWrapper(name = "Annotations")
    protected List<AnnotationImpl> annotations;
    @XmlElement(name = "Formula")
    protected Object formula;
    @XmlElement(name = "CalculatedMemberProperty")
    protected List<CalculatedMemberPropertyImpl> calculatedMemberProperty;
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

    @Override
    public List<AnnotationImpl> annotations() {
        return annotations;
    }

    /**
     * Sets the value of the annotations property.
     * 
     * @param value allowed object is {@link Annotations }
     * 
     */
    public void setAnnotations(List<AnnotationImpl> value) {
        this.annotations = value;
    }

    /**
     * Gets the value of the formula property.
     * 
     * @return possible object is {@link Object }
     * 
     */
    @Override
    public Object formula() {
        return formula;
    }

    /**
     * Sets the value of the formula property.
     * 
     * @param value allowed object is {@link Object }
     * 
     */
    public void setFormula(Object value) {
        this.formula = value;
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
    public List<CalculatedMemberPropertyImpl> calculatedMemberProperty() {
        if (calculatedMemberProperty == null) {
            calculatedMemberProperty = new ArrayList<CalculatedMemberPropertyImpl>();
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

    /**
     * Sets the value of the displayFolder property.
     * 
     * @param value allowed object is {@link String }
     * 
     */
    public void setDisplayFolder(String value) {
        this.displayFolder = value;
    }

}
