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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Role", propOrder = {

})
public class Role extends AbstractItem {

    @XmlElement(name = "Members")
    protected Role.Members members;

    public Role.Members getMembers() {
        return members;
    }

    public void setMembers(Role.Members value) {
        this.members = value;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"member"})
    public static class Members {

        @XmlElement(name = "Member")
        protected List<Member> member;

        public List<Member> getMember() {
            return this.member;
        }

        public void setMember(List<Member> member) {
            this.member = member;
        }
    }

}
