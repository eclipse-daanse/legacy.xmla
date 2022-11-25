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
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.opencube.junit5.context;

import org.eclipse.daanse.db.dialect.api.Dialect;
import org.eclipse.daanse.db.dialect.db.mysql.MySqlDialect;
import org.eclipse.daanse.db.statistics.api.StatisticsProvider;
import org.eclipse.daanse.engine.api.Context;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class SQLLiteContext  implements Context {
    //TODO need implementation
    private Dialect dialect;
    private StatisticsProvider statisticsProvider;
    private DataSource dataSource;

    public SQLLiteContext() {

    }

    public SQLLiteContext(DataSource dataSource) {
        this.dataSource = dataSource;
        dialect = new MySqlDialect();
        try (Connection connection = dataSource.getConnection()) {
            dialect.initialize(connection);
        } catch (SQLException e) {
            new RuntimeException(e);
        }
        statisticsProvider = new StatisticsProvider() {
            @Override
            public void initialize(DataSource dataSource, Dialect dialect) {

            }

            @Override
            public long getTableCardinality(String catalog, String schema, String table) {
                return 0;
            }

            @Override
            public long getQueryCardinality(String sql) {
                return 0;
            }

            @Override
            public long getColumnCardinality(String catalog, String schema, String table, String column) {
                return 0;
            }
        };
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Dialect getDialect() {
        return dialect;
    }

    @Override
    public StatisticsProvider getStatisticsProvider() {
        return statisticsProvider;
    }
}
