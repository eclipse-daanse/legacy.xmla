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
package org.eclipse.daanse.xmla.ws.jakarta.model.xmla.enums;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Applies only to a single member:
 */
@XmlType(name = "TreeOp")
@XmlEnum
public enum TreeOpEnum {

    /**
     * Returns all of the ancestors.
     */
    @XmlEnumValue("0x20")
    all(0x20),

    /**
     *  Returns only the immediate children.
     */
    @XmlEnumValue("0x01")
    children(0x01),

    /**
     * Returns members on the same level.
     */
    @XmlEnumValue("0x02")
    level(0x02),

    /**
     * Returns only the immediate parent.
     */
    @XmlEnumValue("0x04")
    parent(0x04),

    /**
     * Returns only itself.
     */
    @XmlEnumValue("0x08")
    itself(0x08),

    /**
     * Returns all of the descendants.
     */
    @XmlEnumValue("0x10")
    all_descendants(0x10);

    private final int value;

    TreeOpEnum(int v) {
        this.value = v;
    }

    public int getValue() {
        return value;
    }

    public static TreeOpEnum fromValue(String v) {
        if (v == null) {
            return null;
        }
        int vi = Integer.decode(v);
        for (TreeOpEnum c : TreeOpEnum.values()) {
            if (c.value == vi) {
                return c;
            }
        }
        throw new IllegalArgumentException(new StringBuilder("TreeOpEnum Illegal argument ")
            .append(v).toString());
    }
}