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

import org.eclipse.daanse.sql.guard.api.elements.DatabaseCatalog;
import org.eclipse.daanse.sql.guard.api.elements.DatabaseSchema;


public class DatabaseCatalogImpl implements DatabaseCatalog{

    private List<DatabaseSchema> databaseSchemas;
    private String name;


    public DatabaseCatalogImpl(String name, List<org.eclipse.daanse.olap.api.element.DatabaseSchema> databaseSchemas) {
        this.name = name;
        if (databaseSchemas != null) {
            this.databaseSchemas = databaseSchemas.stream().map(ds -> (DatabaseSchema)new DatabaseSchemaImpl(ds)).toList();
        } else {
            this.databaseSchemas = List.of();
        }
    }

    @Override
    public List<DatabaseSchema> getDatabaseSchemas() {
        return this.databaseSchemas;
    }

    @Override
    public String getName() {
        return this.name;
    }

}
