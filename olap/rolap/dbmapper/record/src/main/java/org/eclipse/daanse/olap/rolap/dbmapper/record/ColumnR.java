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
package org.eclipse.daanse.olap.rolap.dbmapper.record;

import org.eclipse.daanse.olap.rolap.dbmapper.api.Column;

import java.util.Objects;

public class ColumnR implements Column {

    private String table;
    private String name;
    private String genericExpression;

    public ColumnR(String table, String name) {
        this.table = table;
        this.name = name;
        this.genericExpression = table == null ? name : (table + "." + name);
    }

    @Override
    public String table() {
        return table;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void setTable(String table) {
        this.table = table;
    }

    @Override
    public String genericExpression() {
        return genericExpression;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Column)) {
            return false;
        }
        Column that = (Column) obj;
        return name().equals(that.name()) &&
            Objects.equals(table(), that.table());
    }
}
