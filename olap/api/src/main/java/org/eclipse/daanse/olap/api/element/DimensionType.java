/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2004-2005 Galt Johnson
 * Copyright (C) 2004-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */

package org.eclipse.daanse.olap.api.element;

/**
 * Enumerates the types of dimensions.
 *
 * @author Galt Johnson
 * @since 5 April, 2004
 */
public enum DimensionType {
    /**
     * Indicates that the dimension is not related to time.
     */
    STANDARD_DIMENSION("StandardDimension"),

    /**
     * Indicates that a dimension is a time dimension.
     */
    TIME_DIMENSION("TimeDimension"),

    /**
     * Indicates the a dimension is the measures dimension.
     */
    MEASURES_DIMENSION("MeasuresDimension");

    private final String value;

    DimensionType(String v) {
        value = v;
    }

    public String getValue() {
        return value;
    }

    public static DimensionType fromValue(String v) {
        for (DimensionType c : DimensionType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
