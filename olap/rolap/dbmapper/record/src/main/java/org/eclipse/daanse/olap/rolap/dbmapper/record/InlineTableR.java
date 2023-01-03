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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.rolap.dbmapper.api.ColumnDef;
import org.eclipse.daanse.olap.rolap.dbmapper.api.InlineTable;
import org.eclipse.daanse.olap.rolap.dbmapper.api.Relation;
import org.eclipse.daanse.olap.rolap.dbmapper.api.Row;

public record InlineTableR(List<ColumnDef> columnDefs,
                           List<Row> rows, String alias)
        implements InlineTable {

    public InlineTableR(InlineTable inlineTable) {
        this(new ArrayList<>(inlineTable.columnDefs()), new ArrayList<>(inlineTable.rows()), inlineTable.alias());
    }

    public InlineTableR(InlineTable inlineTable, String alias) {
        this(new ArrayList<>(inlineTable.columnDefs()), new ArrayList<>(inlineTable.rows()), alias);
    }

    public boolean equals(Object o) {
        if (o instanceof InlineTable) {
            InlineTable that = (InlineTable) o;
            return alias().equals(that.alias());
        } else {
            return false;
        }
    }

    public String toString() {
        return "<inline data>";
    }

    public int hashCode() {
        return toString().hashCode();
    }
}
