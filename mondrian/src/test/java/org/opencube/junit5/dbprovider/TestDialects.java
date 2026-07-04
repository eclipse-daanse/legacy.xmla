/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 */
package org.opencube.junit5.dbprovider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.jdbc.db.dialect.api.DialectInitData;
import org.eclipse.daanse.jdbc.db.dialect.db.clickhouse.ClickHouseDialectFactory;
import org.eclipse.daanse.jdbc.db.dialect.db.derby.DerbyDialectFactory;
import org.eclipse.daanse.jdbc.db.dialect.db.duckdb.DuckDbDialectFactory;
import org.eclipse.daanse.jdbc.db.dialect.db.h2.H2DialectFactory;
import org.eclipse.daanse.jdbc.db.dialect.db.mariadb.MariaDBDialectFactory;
import org.eclipse.daanse.jdbc.db.dialect.db.mssqlserver.MicrosoftSqlServerDialectFactory;
import org.eclipse.daanse.jdbc.db.dialect.db.mysql.MySqlDialectFactory;
import org.eclipse.daanse.jdbc.db.dialect.db.oracle.OracleDialectFactory;
import org.eclipse.daanse.jdbc.db.dialect.db.postgresql.PostgreSqlDialectFactory;
import org.eclipse.daanse.jdbc.db.dialect.db.sqlite.SqliteDialectFactory;

/**
 * The ONE dialect-decision point for the legacy mondrian TCK.
 *
 * <p>Maps the JDBC {@code DatabaseMetaData#getDatabaseProductName()} to the
 * matching jdbc.db dialect factory POJO ({@code @Component} is inert outside
 * OSGi, the factories are plain classes). Database providers must never pick
 * a dialect class themselves — they call {@link #resolve(Connection)}.
 *
 * <p>Order matters: "MariaDB" is checked BEFORE "MySQL" (a MariaDB server
 * reached through a MySQL-family driver can report both tokens).
 *
 */
public final class TestDialects {

    private TestDialects() {
    }

    public static Dialect resolve(Connection connection) {
        DialectInitData init;
        try {
            init = DialectInitData.fromConnection(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("TestDialects: could not read database metadata", e);
        }
        String product = init.productName();
        String p = product == null ? "" : product.toLowerCase(Locale.ROOT);

        // order matters: MariaDB before MySQL
        if (p.contains("mariadb")) {
            return new MariaDBDialectFactory().createDialect(init);
        }
        if (p.contains("mysql")) {
            return new MySqlDialectFactory().createDialect(init);
        }
        if (p.contains("h2")) {
            return new H2DialectFactory().createDialect(init);
        }
        if (p.contains("postgres")) {
            return new PostgreSqlDialectFactory().createDialect(init);
        }
        if (p.contains("oracle")) {
            return new OracleDialectFactory().createDialect(init);
        }
        if (p.contains("microsoft sql server")) {
            return new MicrosoftSqlServerDialectFactory().createDialect(init);
        }
        if (p.contains("derby")) {
            return new DerbyDialectFactory().createDialect(init);
        }
        if (p.contains("sqlite")) {
            return new SqliteDialectFactory().createDialect(init);
        }
        if (p.contains("clickhouse")) {
            return new ClickHouseDialectFactory().createDialect(init);
        }
        if (p.contains("duckdb")) {
            return new DuckDbDialectFactory().createDialect(init);
        }
        throw new IllegalStateException("TestDialects: unknown database product '" + product + "'");
    }
}
