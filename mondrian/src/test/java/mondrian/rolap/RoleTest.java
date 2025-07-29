/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *
 */
package mondrian.rolap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.DatabaseColumn;
import org.eclipse.daanse.olap.api.element.DatabaseSchema;
import org.eclipse.daanse.olap.api.element.DatabaseTable;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

public class RoleTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDatabaseSchemaWithNoRole(Context<?> context) {
        Connection connection = context.getConnectionWithDefaultRole();
        try {
            CatalogReader schemaReader = connection.getCatalogReader();
            List<? extends DatabaseSchema> dsList = schemaReader.getDatabaseSchemas();
            assertEquals(
                1,
                dsList.size());
            DatabaseSchema ds = dsList.get(0);
            List<? extends DatabaseTable> tList = ds.getDbTables();
            assertEquals(
                    26,
                    tList.size());
            Optional<? extends DatabaseTable> oT = tList.stream().filter(t -> "sales_fact_1997".equals(t.getName())).findFirst();
            List<? extends DatabaseColumn> cList = oT.get().getDbColumns();
            assertEquals(
                    9,
                    cList.size());
        } finally {
            connection.close();
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDatabaseSchemaWithRole(Context<?> context) {
        TestUtil.withSchema(context, RoleTestModifier::new);
        Connection connection = context.getConnection(List.of("Test"));

        try {
            CatalogReader schemaReader = connection.getCatalogReader();
            List<? extends DatabaseSchema> dsList = schemaReader.getDatabaseSchemas();
            assertEquals(
                1,
                dsList.size());
            DatabaseSchema ds = dsList.get(0);
            List<? extends DatabaseTable> tList = ds.getDbTables();
            assertEquals(
                    3,
                    tList.size());

            Optional<? extends DatabaseTable> oT = tList.stream().filter(t -> "sales_fact_1997".equals(t.getName())).findFirst();
            assertTrue(oT.isPresent());
            List<? extends DatabaseColumn> cList = oT.get().getDbColumns();
            assertEquals(
                    9,
                    cList.size());

            oT = tList.stream().filter(t -> "product".equals(t.getName())).findFirst();
            assertTrue(oT.isPresent());
            cList = oT.get().getDbColumns();
            assertEquals(
                    4,
                    cList.size());

            oT = tList.stream().filter(t -> "salary".equals(t.getName())).findFirst();
            assertTrue(oT.isPresent());
            cList = oT.get().getDbColumns();
            assertEquals(
                    2,
                    cList.size());

        } finally {
            connection.close();
        }
    }

    //withSchema(context, TestCachedNativeFilterModifier::new);
    //Connection connection = ((TestContext)context).getConnection(List.of("test"));

}
