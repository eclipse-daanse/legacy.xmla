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

public class ExpressiveNamesDataLoader implements DataLoader {
    public static List<DataLoaderUtil.Table> expressiveNamesTables = List.of(
        //1
        new DataLoaderUtil.Table(null, "D1H1L1Table",
            List.of(
                new DataLoaderUtil.Constraint("D1H1L1", false, new String[] { "D1H1L1" })
            ),
            new DataLoaderUtil.Column("D1H1L1", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("D1H1L1_NAME", DataLoaderUtil.Type.Varchar255, false),
            new DataLoaderUtil.Column("D1H1L1_Ordinal", DataLoaderUtil.Type.Integer, false)),
        //2
        new DataLoaderUtil.Table(null, "D2H1L1Table",
            List.of(
                new DataLoaderUtil.Constraint("D2H1L1", false, new String[] { "D2H1L1" })
            ),
            new DataLoaderUtil.Column("D2H1L1", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("D2H1L1_NAME", DataLoaderUtil.Type.Varchar255, false),
            new DataLoaderUtil.Column("D2H1L1_Ordinal", DataLoaderUtil.Type.Integer, false)
        ),
        //3
        new DataLoaderUtil.Table(null, "D2H2L2Table",
            List.of(
                new DataLoaderUtil.Constraint("D2H2L2", false, new String[] { "D2H2L2" })
            ),
            new DataLoaderUtil.Column("D2H2L1", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("D2H2L2", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("D2H2L1_NAME", DataLoaderUtil.Type.Varchar255, false),
            new DataLoaderUtil.Column("D2H2L2_NAME", DataLoaderUtil.Type.Varchar255, false),
            new DataLoaderUtil.Column("D2H2L1_Ordinal", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("D2H2L2_Ordinal", DataLoaderUtil.Type.Integer, false)
        ),
        //4
        new DataLoaderUtil.Table(null, "D3H1L1Table",
            List.of(
                new DataLoaderUtil.Constraint("D3H1L1", false, new String[] { "D3H1L1" })
            ),
            new DataLoaderUtil.Column("D3H1L1", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("D3H1L1_NAME", DataLoaderUtil.Type.Varchar255, false),
            new DataLoaderUtil.Column("D3H1L1_Ordinal", DataLoaderUtil.Type.Integer, false)
        ),
        //5
        new DataLoaderUtil.Table(null, "D3H2L1Table",
            List.of(
                new DataLoaderUtil.Constraint("D3H2L1", false, new String[] { "D3H2L1" })
            ),
            new DataLoaderUtil.Column("D3H2L1", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("D3H2L1_NAME", DataLoaderUtil.Type.Varchar255, false),
            new DataLoaderUtil.Column("D3H2L1_Ordinal", DataLoaderUtil.Type.Integer, false)
        ),
        //6
        new DataLoaderUtil.Table(null, "D3H2L2Table",
            List.of(
                new DataLoaderUtil.Constraint("D3H2L2", false, new String[] { "D3H2L2" })
            ),
            new DataLoaderUtil.Column("D3H2L2", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("D3H2L1_id", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("D3H2L2_NAME", DataLoaderUtil.Type.Varchar255, false),
            new DataLoaderUtil.Column("D3H2L2_Ordinal", DataLoaderUtil.Type.Integer, false)
        ),
        //7
        new DataLoaderUtil.Table(null, "D3H3L1Table",
            List.of(
                new DataLoaderUtil.Constraint("D3H3L1", false, new String[] { "D3H3L1" })
            ),
            new DataLoaderUtil.Column("D3H3L1", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("D3H3L1_NAME", DataLoaderUtil.Type.Varchar255, false),
            new DataLoaderUtil.Column("D3H3L1_Ordinal", DataLoaderUtil.Type.Integer, false)
        ),
        //8
        new DataLoaderUtil.Table(null, "D3H3L2Table",
            List.of(
                new DataLoaderUtil.Constraint("D3H3L2", false, new String[] { "D3H3L2" }),
                new DataLoaderUtil.Constraint("D3H3L1_id", false, new String[] { "D3H3L1_id" })
            ),
            new DataLoaderUtil.Column("D3H3L2", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("D3H3L1_id", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("D3H3L2_NAME", DataLoaderUtil.Type.Varchar255, false),
            new DataLoaderUtil.Column("D3H3L2_Ordinal", DataLoaderUtil.Type.Integer, false)
        ),
        //9
        new DataLoaderUtil.Table(null, "D3H3L3Table",
            List.of(
                new DataLoaderUtil.Constraint("D3H3L3", false, new String[] { "D3H3L3", "D3H3L2_id"})
            ),
            new DataLoaderUtil.Column("D3H3L3", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("D3H3L2_id", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("D3H3L3_NAME", DataLoaderUtil.Type.Varchar255, false),
            new DataLoaderUtil.Column("D3H3L3_Ordinal", DataLoaderUtil.Type.Integer, false)
        ),
        //10
        new DataLoaderUtil.Table(null, "Cube1Fact",
            List.of(
                new DataLoaderUtil.Constraint("D1", false, new String[] { "D1"}),
                new DataLoaderUtil.Constraint("D2", false, new String[] { "D2"}),
                new DataLoaderUtil.Constraint("D3", false, new String[] { "D3"})
            ),
            new DataLoaderUtil.Column("D1", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("D2", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("D3", DataLoaderUtil.Type.Integer, false),
            new DataLoaderUtil.Column("M1", DataLoaderUtil.Type.Integer, false)
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

            Path dir= Paths.get(Constants.TESTFILES_DIR+"loader/expressivenames/data");

            DataLoaderUtil.importCSV(dataBaseInfo.getKey(), dialect, expressiveNamesTables, dir);


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

        return expressiveNamesTables.stream().flatMap(t -> DataLoaderUtil.createIndexSqls(t, dialect).stream()).toList();
    }

    /**
     * drop all existing tables for the FoodMart database.
     * <p/>
     *
     * @param dialect
     *
     */
    private List<String> dropTableSQLs(Dialect dialect) {

        return expressiveNamesTables.stream().map(t -> DataLoaderUtil.dropTableSQL(t, dialect)).toList();

    }

    /**
     * Defines all tables for the FoodMart database.
     * <p/>
     *
     * @param dialect
     *
     */
    private List<String> createTablesSQLs(Dialect dialect) {

        return expressiveNamesTables.stream().map(t -> DataLoaderUtil.createTableSQL(t, dialect)).toList();

    }

}
