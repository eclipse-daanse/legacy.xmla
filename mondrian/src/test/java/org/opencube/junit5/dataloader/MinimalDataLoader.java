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
package org.opencube.junit5.dataloader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.opencube.junit5.Constants;

public class MinimalDataLoader implements DataLoader {
    public static List<DataLoaderUtil.Table> minimalTables = List.of(
        //1
        new DataLoaderUtil.Table(null, "OnlyCubeFact",
            List.of(
                new DataLoaderUtil.Constraint("KEY", false, new String[] { "KEY" })
            ),
            new DataLoaderUtil.Column("KEY", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("KEY_NAME", DataLoaderUtil.Type.Varchar255, false),
            new DataLoaderUtil.Column("KEY_ORDER", DataLoaderUtil.Type.Integer, false)),
        //2
        new DataLoaderUtil.Table(null, "Cube1Fact",
            List.of(
                new DataLoaderUtil.Constraint("D2H1L1", false, new String[] { "D1H1L1" })
            ),
            new DataLoaderUtil.Column("D1", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("VALUE_COUNT", DataLoaderUtil.Type.Varchar255, false),
            new DataLoaderUtil.Column("VALUE", DataLoaderUtil.Type.Integer, false)
        )
    );

    @Override
    public boolean loadData(Map.Entry<DataSource, Dialect> dataBaseInfo) throws Exception {
        try (Connection connection = dataBaseInfo.getKey().getConnection()) {

            Dialect dialect = dataBaseInfo.getValue();

            List<String> dropTableSQLs = dropTableSQLs(dialect);
            DataLoaderUtil.executeSql(connection, dropTableSQLs,true);

            List<String> createTablesSqls = createTablesSQLs(dialect);
            DataLoaderUtil.executeSql(connection, createTablesSqls,true);

            List<String> createIndexesSqls = createIndexSQLs(dialect);
            DataLoaderUtil.executeSql(connection, createIndexesSqls,true);

            Path dir= Paths.get(Constants.TESTFILES_DIR+"loader/minimal/data");

            DataLoaderUtil.importCSV(dataBaseInfo.getKey(), dialect, minimalTables, dir);


        }
        return true;
    }
    /**
     * create indexes for the ExpressiveNames database.
     * <p/>
     *
     * @param dialect
     *
     */
    private List<String> createIndexSQLs(Dialect dialect) {

        return minimalTables.stream().flatMap(t -> DataLoaderUtil.createIndexSqls(t, dialect).stream()).toList();
    }

    /**
     * drop all existing tables for the FoodMart database.
     * <p/>
     *
     * @param dialect
     *
     */
    private List<String> dropTableSQLs(Dialect dialect) {

        return minimalTables.stream().map(t -> DataLoaderUtil.dropTableSQL(t, dialect)).toList();

    }

    /**
     * Defines all tables for the FoodMart database.
     * <p/>
     *
     * @param dialect
     *
     */
    private List<String> createTablesSQLs(Dialect dialect) {

        return minimalTables.stream().map(t -> DataLoaderUtil.createTableSQL(t, dialect)).toList();

    }

}
