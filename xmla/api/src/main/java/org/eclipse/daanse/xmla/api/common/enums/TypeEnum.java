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
package org.eclipse.daanse.xmla.api.common.enums;

public enum TypeEnum {

    /**
     * Multidimensional. If the other bits
     * are not set, the database is a
     * Multidimensional database
     */
    Multidimensional(0x00),

    /**
     * TabularMetadata. The Tabular
     * model is built by using Tabular metadata.
     */
    TabularMetadata(0x01),

    /**
     * TabularModel. This is a Tabular
     * model, including those built using Tabular
     * or Multidimensional metadata.
     */
    TabularModel(0x02);

    private final int value;

    TypeEnum(int v) {
        this.value = v;
    }

    public int getValue() {
        return value;
    }

    public static TypeEnum fromValue(String v) {
        if (v == null) {
            return null;
        }
        int vi = Integer.decode(v);

        for (TypeEnum c : TypeEnum.values()) {
            if (c.value == vi) {
                return c;
            }
        }
        throw new IllegalArgumentException(new StringBuilder("TypeEnum Illegal argument ")
            .append(v).toString());
    }
}