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

import java.util.List;
import java.util.Objects;

import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.common.ConfigConstants;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.rolap.common.RolapUtil;
import org.eclipse.daanse.rolap.element.RolapCube;
import org.eclipse.daanse.sql.dialect.api.Dialect;
import org.opencube.junit5.TestUtil;
import org.opentest4j.AssertionFailedError;

import mondrian.enums.DatabaseProduct;
import mondrian.test.SqlPattern;

/**
 * Fluent SQL-generation assertions:
 * {@code SqlAssert.forQuery(connection, mdx).expectSql(SqlPattern.mysql("...")).verify()}.
 *
 * <p>
 * Replaces the legacy {@code TestUtil.assertQuerySql} / {@code assertNoQuerySql} /
 * {@code assertQuerySqlOrNot} family: parses and executes {@code mdx} once per pattern whose
 * dialect matches the connection's, and - via a query-execution hook, same trick as the legacy
 * code - interrupts execution the instant a SQL statement's prefix matches the pattern's
 * trigger, then checks whether that (or the absence of that) is what {@link #expectSql} /
 * {@link #expectNoSql} called for.
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

    public static final class QuerySqlAssert {

        private final Connection connection;
        private final String mdxQuery;
        private boolean bypassSchemaCache;
        // Defaults to true: the legacy assertQuerySql/assertNoQuerySql convenience wrappers this
        // class replaces always clear the cache before executing - it's not a flag callers set,
        // it's baked into the wrapper. Matching that default here avoids a warm cache silently
        // turning "did the query run this SQL" into "was this SQL ever run, maybe minutes ago".
        private boolean clearCacheFirst = true;
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
         * if for the first time. On by default (see field comment); calling this is only ever
         * needed to undo a prior {@link #keepCache()} on the same builder.
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
            String sql = TestUtil.dialectize(databaseProduct, sqlPattern.getSql());
            String trigger = TestUtil.dialectize(databaseProduct, sqlPattern.getTriggerSql());

            TriggerHook hook = new TriggerHook(trigger);
            RolapUtil.setHook(connection.getContext(), hook);
            Bomb bomb = null;
            try {
                Query query = connection.parseQuery(mdxQuery);
                if (clearCacheFirst) {
                    TestUtil.clearCache(connection, (RolapCube) query.getCube());
                }
                connection.execute(query);
            } catch (Bomb caught) {
                bomb = caught;
            } catch (RuntimeException e) {
                bomb = Util.getMatchingCause(e, Bomb.class);
                if (bomb == null) {
                    throw e;
                }
            } finally {
                RolapUtil.setHook(connection.getContext(), null);
            }

            if (negative) {
                if (bomb != null || hook.foundMatch()) {
                    throw new AssertionFailedError("forbidden query [" + sql + "] detected"
                            + System.lineSeparator() + "MDX:" + System.lineSeparator() + mdxQuery);
                }
                return;
            }

            if (bomb == null && !hook.foundMatch()) {
                StringBuilder seen = new StringBuilder();
                for (String s : hook.seen()) {
                    seen.append(System.lineSeparator()).append("--- actual ---").append(System.lineSeparator())
                            .append(s);
                }
                throw new AssertionFailedError(
                        "expected query [" + sql + "] did not occur; statements seen:" + seen
                                + System.lineSeparator() + "MDX:" + System.lineSeparator() + mdxQuery);
            }
            if (bomb != null) {
                String expected = replaceQuotes(sql.replaceAll("\r\n", "\n"));
                String actual = replaceQuotes(bomb.sql.replaceAll("\r\n", "\n"));
                if (!expected.equals(actual)) {
                    throw new AssertionFailedError(
                            "SQL did not match pattern" + System.lineSeparator() + "MDX:" + System.lineSeparator()
                                    + mdxQuery,
                            expected, actual);
                }
            }
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

    private static String replaceQuotes(String s) {
        return s.replace('`', '"').replace('\'', '"');
    }

    /** Fake exception used to interrupt execution the instant the trigger SQL is seen. */
    private static final class Bomb extends Error {
        final String sql;

        Bomb(String sql) {
            this.sql = sql;
        }
    }

    private static final class TriggerHook implements RolapUtil.ExecuteQueryHook {

        private final String trigger;
        private boolean foundMatch;
        private final List<String> seen = new java.util.ArrayList<>();

        TriggerHook(String trigger) {
            this.trigger = trigger.replaceAll("\r\n", "").replaceAll("\r", "").replaceAll("\n", "");
        }

        private boolean matchTrigger(String sql) {
            String normalizedSql = sql.replaceAll("\r\n", "").replaceAll("\r", "").replaceAll("\n", "");
            String s = replaceQuotes(normalizedSql);
            String t = replaceQuotes(trigger);
            if (s.startsWith(t) && !foundMatch) {
                foundMatch = true;
            }
            return s.startsWith(t);
        }

        @Override
        public void onExecuteQuery(String sql) {
            seen.add(sql);
            if (matchTrigger(sql)) {
                throw new Bomb(sql);
            }
        }

        boolean foundMatch() {
            return foundMatch;
        }

        List<String> seen() {
            return seen;
        }
    }
}
