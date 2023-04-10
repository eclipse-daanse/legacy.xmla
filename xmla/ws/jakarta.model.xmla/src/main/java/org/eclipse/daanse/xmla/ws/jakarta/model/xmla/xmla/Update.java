/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Update", propOrder = {

})
public class Update {

    @XmlElement(name = "Object", required = true)
    protected Object object;
    @XmlElement(name = "Attributes")
    protected Update.Attributes attributes;
    @XmlElement(name = "MoveWithDescendants")
    protected Boolean moveWithDescendants;
    @XmlElement(name = "MoveToRoot")
    protected Boolean moveToRoot;
    @XmlElement(name = "Where", required = true)
    protected Where where;

    public Object getObject() {
        return object;
    }

    public void setObject(Object value) {
        this.object = value;
    }

    public Update.Attributes getAttributes() {
        return attributes;
    }

    public void setAttributes(Update.Attributes value) {
        this.attributes = value;
    }

    public Boolean isMoveWithDescendants() {
        return moveWithDescendants;
    }

    public void setMoveWithDescendants(Boolean value) {
        this.moveWithDescendants = value;
    }

    public Boolean isMoveToRoot() {
        return moveToRoot;
    }

    public void setMoveToRoot(Boolean value) {
        this.moveToRoot = value;
    }

    public Where getWhere() {
        return where;
    }

    public void setWhere(Where value) {
        this.where = value;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"attribute"})
    public static class Attributes {

        @XmlElement(name = "Attribute")
        protected List<AttributeInsertUpdate> attribute;

        public List<AttributeInsertUpdate> getAttribute() {
            return this.attribute;
        }

        public void setAttribute(List<AttributeInsertUpdate> attribute) {
            this.attribute = attribute;
        }
    }

}
