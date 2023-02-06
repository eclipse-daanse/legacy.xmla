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

public enum InstanceSelectionEnum {

    DROPDOWN(1), // DROPDOWN type of display is suggested.
    LIST(2), // LIST type of display is suggested.
    FILTERED_LIST(3), // FILTERED LIST type of display is suggested.
    MANDATORY_FILTER(4); // MANDATORY FILTER type of display is suggested

    private final int value;

    InstanceSelectionEnum(int v) {
        this.value = v;
    }

    public int getValue() {
        return value;
    }

    public static InstanceSelectionEnum fromValue(String v) {
        int vi = Integer.valueOf(v);
        for (InstanceSelectionEnum c : InstanceSelectionEnum.values()) {
            if (c.value == vi) {
                return c;
            }
        }
        throw new IllegalArgumentException(new StringBuilder("InstanceSelectionEnum Illegal argument ")
            .append(v).toString());
    }
}
