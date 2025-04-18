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

import org.eclipse.daanse.olap.rolap.dbmapper.model.api.MappingHierarchyGrant;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.MappingMemberGrant;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.enums.AccessEnum;
import org.eclipse.daanse.olap.rolap.dbmapper.model.jaxb.adapter.AccessAdaptor;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "memberGrants" })
public class HierarchyGrantImpl implements MappingHierarchyGrant {

    @XmlElement(name = "MemberGrant", type = MemberGrantImpl.class)
    protected List<MappingMemberGrant> memberGrants;
    @XmlAttribute(name = "hierarchy", required = true)
    protected String hierarchy;
    @XmlAttribute(name = "access", required = true)
    @XmlJavaTypeAdapter(AccessAdaptor.class)
    protected AccessEnum access;
    @XmlAttribute(name = "topLevel")
    protected String topLevel;
    @XmlAttribute(name = "bottomLevel")
    protected String bottomLevel;
    @XmlAttribute(name = "rollupPolicy")
    protected String rollupPolicy;

    @Override
    public List<MappingMemberGrant> memberGrants() {
        if (memberGrants == null) {
            memberGrants = new ArrayList<>();
        }
        return this.memberGrants;
    }

    @Override
    public String hierarchy() {
        return hierarchy;
    }

    public void setHierarchy(String value) {
        this.hierarchy = value;
    }

    @Override
    public AccessEnum access() {
        return access;
    }

    public void setAccess(AccessEnum value) {
        this.access = value;
    }

    @Override
    public String topLevel() {
        return topLevel;
    }

    public void setTopLevel(String value) {
        this.topLevel = value;
    }

    @Override
    public String bottomLevel() {
        return bottomLevel;
    }

    public void setBottomLevel(String value) {
        this.bottomLevel = value;
    }

    @Override
    public String rollupPolicy() {
        if (rollupPolicy == null) {
            return "full";
        } else {
            return rollupPolicy;
        }
    }

    public void setRollupPolicy(String value) {
        this.rollupPolicy = value;
    }

    public void setMemberGrants(List<MappingMemberGrant> memberGrants) {
        this.memberGrants = memberGrants;
    }

}
