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
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.rolap.aggmatch.jaxb;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

@XmlType(name = "charcase")
@XmlEnum
public enum CharCaseEnum {
    @XmlEnumValue("ignore")
    IGNORE("ignore"),
    @XmlEnumValue("exact")
    EXACT("exact"),
    @XmlEnumValue("upper")
    UPPER("upper"),
    @XmlEnumValue("lower")
    LOWER("lower");

    private final String value;

    CharCaseEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CharCaseEnum fromValue(String v) {
        for (CharCaseEnum c : CharCaseEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}