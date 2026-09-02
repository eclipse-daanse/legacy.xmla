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
package org.eclipse.daanse.rolap.poc;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Objects;

import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.common.ConfigConstants;
import org.eclipse.daanse.rolap.element.RolapCube;
import org.eclipse.daanse.sql.dialect.api.Dialect;

import mondrian.enums.DatabaseProduct;
import mondrian.test.SqlPattern;

/**
 * Fluent SQL-generation assertions:
 * {@code SqlAssert.forQuery(connection, mdx).expectSql(SqlPattern.mysql("...")).verify()}.
 *
 * <p>
 * Replaces the legacy {@code TestUtil.assertQuerySql} / {@code assertNoQuerySql} /
 * {@code assertQuerySqlOrNot} family. This class's own job is narrow: resolve {@link SqlPattern} /
 * {@link DatabaseProduct} dialect bookkeeping - the bit that can't live below this module, since
 * both types live here in {@code legacy.xmla} - down to one concrete {@code (sql, triggerSql)}
 * pair per matching-dialect pattern, then hand each pair to
 * {@link org.eclipse.daanse.rolap.testkit.assertions.SqlAssert} in the testkit module, which does
 * the actual query-execution-hook/interrupt mechanics and datasource verification. Same split as
 * {@code CellRequestFixture.RequestSqlAssert} makes for cell-request SQL assertions, and for the
 * same reason - see that testkit class's javadoc.
 *
 * <p>
 * {@link #verify()} is the terminal call - {@code expectSql}/{@code expectNoSql} only record
 * what's expected, exactly as in the sample call shapes this class was modeled on.
 */
public final class SqlAssert {

    private SqlAssert() {
    }

    /** Starts a fluent SQL-generation assertion for {@code mdxQuery} run over {@code connection}. */
    public static QuerySqlAssert forQuery(Connection connection, String mdxQuery) {
        return new QuerySqlAssert(connection, mdxQuery);
    }

    /**
     * Checks that {@code actualSql} - typically a drill-through SQL string, not one captured via
     * {@link #forQuery} - matches {@code expectedSql} once both are dialectized and stripped of
     * quotes, then runs {@code actualSql} against {@code connection}'s datasource and checks it
     * returns {@code expectedRows} rows. Replaces the legacy {@code TestUtil.assertSqlEquals}.
     */
    public static void assertSqlEquals(Connection connection,
            String expectedSql,
            String actualSql,
            int expectedRows) {
        assertSqlEquals(connection, expectedSql, actualSql, expectedRows, false);
    }

    /**
     * Like {@link #assertSqlEquals(Connection, String, String, int)}, but compares the SQL
     * whitespace-insensitively (every run of whitespace - including newlines - collapsed to a
     * single space, then trimmed). Use this for queries produced by the generic statement
     * builder, whose {@code DialectSqlRenderer} emits compact single-line SQL that is
     * token-for-token equal to the legacy {@code SqlSelectQuery} output but not format-equal. The
     * datasource row-count check still runs against the actual SQL.
     */
    public static void assertSqlEqualsIgnoreFormatting(Connection connection,
            String expectedSql,
            String actualSql,
            int expectedRows) {
        assertSqlEquals(connection, expectedSql, actualSql, expectedRows, true);
    }

    /** Wraps {@code sql} as a single-dialect {@link SqlPattern} for the MySQL/MariaDB dialect. */
    public static SqlPattern[] mysqlPattern(String sql) {
        return sqlPattern(DatabaseProduct.MYSQL, sql);
    }

    private static SqlPattern[] sqlPattern(DatabaseProduct db, String sql) {
        return new SqlPattern[]{new SqlPattern(db, sql, sql.length())};
    }

    private static void assertSqlEquals(Connection connection,
            String expectedSql,
            String actualSql,
            int expectedRows,
            boolean ignoreFormatting) {
        // if the actual SQL isn't in the current dialect we have some
        // problems... probably with the dialectize method
        assertEquals(dialectize(connection, actualSql), actualSql);

        String dialectizedExpectedSql = dialectize(connection, expectedSql);
        if (ignoreFormatting) {
            org.eclipse.daanse.rolap.testkit.assertions.SqlAssert.assertSqlEqualsIgnoreFormatting(
                    connection, dialectizedExpectedSql, actualSql, expectedRows);
        } else {
            org.eclipse.daanse.rolap.testkit.assertions.SqlAssert.assertSqlEquals(
                    connection, dialectizedExpectedSql, actualSql, expectedRows);
        }
    }

    /**
     * Converts a SQL string into the current dialect.
     *
     * <p>
     * This is not intended to be a general purpose method: it looks for specific patterns known to
     * occur in tests, in particular "=as=" and "fname + ' ' + lname".
     *
     * @param sql SQL string in generic dialect
     * @return SQL string converted into current dialect
     */
    private static String dialectize(Connection connection, String sql) {
        final String search = "fname \\+ ' ' \\+ lname";
        final Dialect dialect = connection.getContext().getDialect();
        final DatabaseProduct databaseProduct = getDatabaseProduct(dialect.name());
        switch (databaseProduct) {
            case MYSQL:
            case MARIADB:
                // Mysql would generate "CONCAT(...)"
                sql = sql.replaceAll(
                        search,
                        "CONCAT(`customer`.`fname`, ' ', `customer`.`lname`)");
                break;
            case POSTGRES:
            case ORACLE:
            case LUCIDDB:
            case TERADATA:
                sql = sql.replaceAll(
                        search,
                        "`fname` || ' ' || `lname`");
                break;
            case DERBY:
                sql = sql.replaceAll(
                        search,
                        "`customer`.`fullname`");
                break;
            case INGRES:
                sql = sql.replaceAll(
                        search,
                        "fullname");
                break;
            case DB2:
            case DB2_AS400:
            case DB2_OLD_AS400:
                sql = sql.replaceAll(
                        search,
                        "CONCAT(CONCAT(`customer`.`fname`, ' '), `customer`.`lname`)");
                break;
            default:
                break;
        }

        if (databaseProduct == DatabaseProduct.ORACLE) {
            // " + tableQualifier + "
            sql = sql.replaceAll(" =as= ", " ");
        } else {
            sql = sql.replaceAll(" =as= ", " as ");
        }
        return sql;
    }

    private static String dialectize(DatabaseProduct d, String sql) {
        sql = sql.replaceAll("\r\n", "\n");
        switch (d) {
            case ORACLE:
                return sql.replaceAll(" =as= ", " ");
            case GREENPLUM:
            case POSTGRES:
            case TERADATA:
                return sql.replaceAll(" =as= ", " as ");
            case DERBY:
                return sql.replaceAll("`", "\"");
            case ACCESS:
                return sql.replaceAll(
                        "ISNULL\\(([^)]*)\\)",
                        "Iif($1 IS NULL, 1, 0)");
            default:
                return sql;
        }
    }

    public static final class QuerySqlAssert {

        private final Connection connection;
        private final String mdxQuery;
        private boolean bypassSchemaCache;
        // null = use the testkit delegate's own default (true). Tri-state so bypassSchemaCache()/
        // clearCacheFirst()/keepCache() calls (or none at all) map onto the delegate one-for-one.
        private Boolean clearCacheFirst;
        private SqlPattern[] patterns;
        private boolean negative;

        private QuerySqlAssert(Connection connection, String mdxQuery) {
            this.connection = Objects.requireNonNull(connection, "connection");
            this.mdxQuery = Objects.requireNonNull(mdxQuery, "mdxQuery");
        }

        /**
         * Matches the legacy 6-arg form's {@code bypassSchemaCache} flag - which is itself
         * currently a no-op there too (the "grab a fresh, schema-pool-bypassing connection" step
         * is commented out in {@code TestUtil.assertQuerySqlOrNot}). Kept for call-site parity;
         * wire it up for real once that gap is closed upstream.
         */
        public QuerySqlAssert bypassSchemaCache() {
            this.bypassSchemaCache = true;
            return this;
        }

        /**
         * Clears the query's cube's aggregation/member cache before executing, so its SQL runs as
         * if for the first time. On by default; calling this is only ever needed to undo a prior
         * {@link #keepCache()} on the same builder.
         */
        public QuerySqlAssert clearCacheFirst() {
            this.clearCacheFirst = true;
            return this;
        }

        /**
         * Opts out of the default cache-clearing - matches passing {@code clearCache=false} to the
         * legacy 6-arg {@code assertQuerySqlOrNot}. Use when the test deliberately wants to
         * observe cache-hit behavior (e.g. asserting that a warm cache produces no SQL at all).
         */
        public QuerySqlAssert keepCache() {
            this.clearCacheFirst = false;
            return this;
        }

        /**
         * Sets the cache-clearing flag from a caller-supplied value, for call sites that decide
         * at runtime rather than picking {@link #clearCacheFirst()} or {@link #keepCache()}
         * literally.
         */
        public QuerySqlAssert clearCacheFirst(boolean clearCacheFirst) {
            this.clearCacheFirst = clearCacheFirst;
            return this;
        }

        /** Records that {@link #verify()} must see each matching-dialect pattern's SQL get executed. */
        public QuerySqlAssert expectSql(SqlPattern... patterns) {
            this.patterns = Objects.requireNonNull(patterns, "patterns");
            this.negative = false;
            return this;
        }

        /** Records that {@link #verify()} must NOT see any matching-dialect pattern's SQL get executed. */
        public QuerySqlAssert expectNoSql(SqlPattern... patterns) {
            this.patterns = Objects.requireNonNull(patterns, "patterns");
            this.negative = true;
            return this;
        }

        /**
         * Runs the query once per pattern whose dialect matches the connection's, and checks each
         * against {@link #expectSql}/{@link #expectNoSql}'s expectation. A dialect with no matching
         * pattern at all is skipped - the assertion is trivially satisfied for it, same as legacy.
         */
        public void verify() {
            if (patterns == null) {
                throw new IllegalStateException("call expectSql(...) or expectNoSql(...) before verify()");
            }

            Dialect dialect = connection.getContext().getDialect();
            DatabaseProduct databaseProduct = getDatabaseProduct(dialect.name());
            boolean patternFound = false;

            for (SqlPattern sqlPattern : patterns) {
                if (!sqlPattern.hasDatabaseProduct(databaseProduct)) {
                    continue;
                }
                patternFound = true;
                verifyOne(sqlPattern, databaseProduct);
            }

            if (!patternFound) {
                warnNoPatternForDialect(dialect, databaseProduct);
            }
        }

        private void verifyOne(SqlPattern sqlPattern, DatabaseProduct databaseProduct) {
            String sql = dialectize(databaseProduct, sqlPattern.getSql());
            String trigger = dialectize(databaseProduct, sqlPattern.getTriggerSql());

            org.eclipse.daanse.rolap.testkit.assertions.SqlAssert.QuerySqlAssert delegate =
                    org.eclipse.daanse.rolap.testkit.assertions.SqlAssert.forQuery(connection, mdxQuery);
            if (bypassSchemaCache) {
                delegate.bypassSchemaCache();
            }
            if (clearCacheFirst != null) {
                delegate.clearCacheFirst(clearCacheFirst);
            }
            if (negative) {
                delegate.expectNoSql(trigger);
            } else {
                delegate.expectSql(sql, trigger);
            }
            delegate.verify();
        }

        private void warnNoPatternForDialect(Dialect dialect, DatabaseProduct databaseProduct) {
            String warnDialect = connection.getContext().getConfigValue(
                    ConfigConstants.WARN_IF_NO_PATTERN_FOR_DIALECT,
                    ConfigConstants.WARN_IF_NO_PATTERN_FOR_DIALECT_DEFAULT_VALUE, String.class);
            if (warnDialect.equals(databaseProduct.toString())) {
                System.out.println(
                        "[No expected SQL statements found for dialect \"" + dialect + "\" and test not run]");
            }
        }
    }

    /** Clears {@code cube}'s member/aggregation caches so its next query's SQL runs as if for the first time. */
    public static void clearCache(Connection connection, RolapCube cube) {
        org.eclipse.daanse.rolap.testkit.assertions.SqlAssert.clearCache(connection, cube);
    }
}
