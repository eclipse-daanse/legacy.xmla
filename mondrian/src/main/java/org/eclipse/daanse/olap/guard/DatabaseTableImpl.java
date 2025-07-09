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
package org.eclipse.daanse.olap.guard;

import java.util.List;

import org.eclipse.daanse.sql.guard.api.elements.DatabaseColumn;
import org.eclipse.daanse.sql.guard.api.elements.DatabaseTable;

public class DatabaseTableImpl implements DatabaseTable{

    private List<DatabaseColumn> columns;
    private String name;

    public DatabaseTableImpl(org.eclipse.daanse.olap.api.element.DatabaseTable table) {
        this.name = table.getName();
        if (table.getDbColumns() != null) {
            columns = table.getDbColumns().stream().map(c -> (DatabaseColumn)new DatabaseColumnImpl(c)).toList();
        } else {
            columns = List.of();
        }
    }

    @Override
    public List<DatabaseColumn> getDatabaseColumns() {
        return this.columns;
    }

    @Override
    public String getName() {
        return this.name;
    }

}
