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
package mondrian.olap.guard;

import java.util.List;

import org.eclipse.daanse.sql.guard.api.elements.DatabaseSchema;
import org.eclipse.daanse.sql.guard.api.elements.DatabaseTable;

public class DatabaseSchemaImpl implements DatabaseSchema{

    private List<DatabaseTable> databaseTables;
    private String name;


    public DatabaseSchemaImpl(org.eclipse.daanse.olap.api.element.DatabaseSchema ds) {
        this.name = ds.getName() == null ? "" : ds.getName();
        if (ds.getDbTables() != null) {
            this.databaseTables = ds.getDbTables().stream().map(t -> (DatabaseTable) new DatabaseTableImpl(t)).toList();
        } else {
            this.databaseTables = List.of();
        }
    }

    @Override
    public List<DatabaseTable> getDatabaseTables() {
        return this.databaseTables;
    }

    @Override
    public String getName() {
        return this.name;
    }

}
