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

public enum GroupingBehaviorEnum {

    ENCOURAGED(1), // Client applications are encouraged to group by each member of the hierarchy.
    DISCOURAGED(2); // Client applications are discouraged from grouping by each member of the hierarchy.

    private final int value;

    GroupingBehaviorEnum(int v) {
        this.value = v;
    }

    public int getValue() {
        return value;
    }

    public static GroupingBehaviorEnum fromValue(String v) {
        int vi = Integer.valueOf(v);
        for (GroupingBehaviorEnum c : GroupingBehaviorEnum.values()) {
            if (c.value == vi) {
                return c;
            }
        }
        throw new IllegalArgumentException(new StringBuilder("GroupingBehaviorEnum Illegal argument ")
            .append(v).toString());
    }
}
