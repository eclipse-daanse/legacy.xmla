/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.api;

import java.util.stream.Stream;

public enum DataTypeJdbc {
    VARCHAR("Varchar"),

    NUMERIC("Numeric"),

    INTEGER("Integer"),
    DECIMAL("Decimal"),

    FLOAT("Float"),

    REAL("Real"),

    BIGINT("BigInt"),

    SMALLINT("SmallInt"),

    DOUBLE("Double"),

    BOOLEAN("Boolean"),

    DATE("Date"),

    TIME("Time"),

    TIMESTAMP("Timestamp");

    private String value;

    DataTypeJdbc(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DataTypeJdbc fromValue(String v) {
        return Stream.of(DataTypeJdbc.values())
            .filter(e -> (e.getValue().equals(v)))
            .findFirst().orElse(NUMERIC);
    }
}
