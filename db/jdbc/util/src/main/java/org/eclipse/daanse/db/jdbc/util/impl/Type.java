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
package org.eclipse.daanse.db.jdbc.util.impl;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;

public enum Type {

    INTEGER("INTEGER"),
    NUMERIC("DECIMAL"),
    SMALLINT("SMALLINT"),
    STRING("VARCHAR"),
    BOOLEAN("BOOLEAN"),
    LONG("BIGINT"),
    DATE("DATE"),
    TIMESTAMP("TIMESTAMP"),
    TIME("TIME");

    private static final String MSSQL = "MSSQL";
    /**
     * The name of this type. Immutable, and independent of the RDBMS.
     */
    public final String value;

    Type(String name) {
        this.value = name;
    }

    public static Type fromName(String n) {
        if (n == null){
            return Type.STRING;
        }
        for (Type t : Type.values()) {
            if (t.name().equals(n)) {
                return t;
            }
        }
        throw new IllegalArgumentException(new StringBuilder("Type Illegal name ")
            .append(n).toString());
    }

    /**
     * Returns the physical type which a given RDBMS (dialect) uses to represent
     * this logical type.
     */
    public String toPhysical(Dialect dialect) {
        //TODO move logic to dialect
        if (this == INTEGER ||  this == SMALLINT
            || this == STRING || this == NUMERIC || this == TIME) {
            return value;
        }
        if (this == BOOLEAN) {
            return getBooleanType(dialect);
        }
        if (this == LONG) {
            if ("ORACLE".equals(dialect.getDialectName()) || "FIREBIRD".equals(dialect.getDialectName())) {
                return "DECIMAL(15,0)";
            } else {
                return value;
            }
        }
        if (this == DATE) {
            return getDateType(dialect);
        }
        if (this == TIMESTAMP) {
            return getTymeStampType(dialect);
        }
        throw new AssertionError("unexpected type: " + value);
    }

    private String getBooleanType(Dialect dialect) {
        switch (dialect.getDialectName()) {
            case "POSTGRES", "GREENPLUM", "LUCIDDB", "NETEZZA", "HSQLDB":
                return value;
            case "MARIADB", "MYSQL", "INFOBRIGHT":
                return "TINYINT(1)";
            case MSSQL, "SYBASE":
                return "BIT";
            default:
                return SMALLINT.value;
        }
    }

    private String getDateType(Dialect dialect) {
        switch (dialect.getDialectName()) {
            case MSSQL:
                return "DATETIME";
            case "INGRES":
                return "INGRESDATE";
            default:
                return value;
        }
    }

    private String getTymeStampType(Dialect dialect) {
        switch (dialect.getDialectName()) {
            case MSSQL, "MARIADB", "MYSQL", "INFOBRIGHT", "SYBASE":
                return "DATETIME";
            case "INGRES":
                return "INGRESDATE";
            case "INFORMIX":
                return "DATETIME YEAR TO FRACTION(1)";
            default:
                return value;
        }
    }
}



