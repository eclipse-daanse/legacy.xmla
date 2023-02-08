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
package org.eclipse.daanse.xmla.api.discover.dbschema.schemata;

public interface DbSchemaSchemataRestrictions {

    String RESTRICTIONS_CATALOG_NAME = "CATALOG_NAME";
    String RESTRICTIONS_SCHEMA_NAME = "SCHEMA_NAME";

    String RESTRICTIONS_SCHEMA_OWNER = "SCHEMA_OWNER";

    /**
     * @return Catalog name
     */
    String catalogName();

    /**
     * Schema name
     */
    String schemaName();

    /**
     * @return Schema owner
     */
    String schemaOwner();
}
