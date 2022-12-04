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

import org.eclipse.daanse.olap.rolap.dbmapper.api.Role;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "annotations", "schemaGrant", "union" })
public class RoleImpl implements Role {

    @XmlElement(name = "Annotation")
    @XmlElementWrapper(name = "Annotations")
    protected List<AnnotationImpl> annotations;
    @XmlElement(name = "SchemaGrant")
    protected List<SchemaGrantImpl> schemaGrant;
    @XmlElement(name = "Union", required = true)
    protected UnionImpl union;
    @XmlAttribute(name = "name", required = true)
    protected String name;

    @Override
    public List<AnnotationImpl> annotations() {
        return annotations;
    }

    public void setAnnotations(List<AnnotationImpl> value) {
        this.annotations = value;
    }

    @Override
    public List<SchemaGrantImpl> schemaGrant() {
        if (schemaGrant == null) {
            schemaGrant = new ArrayList<SchemaGrantImpl>();
        }
        return this.schemaGrant;
    }

    @Override
    public UnionImpl union() {
        return union;
    }

    public void setUnion(UnionImpl value) {
        this.union = value;
    }

    @Override
    public String name() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

}
