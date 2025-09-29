/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2004-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.rolap.sql;

import static mondrian.enums.DatabaseProduct.MYSQL;
import static mondrian.enums.DatabaseProduct.POSTGRES;
import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.getDialect;
import static org.opencube.junit5.TestUtil.withSchemaEmf;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.jdbc.db.dialect.db.common.JdbcDialectImpl;
import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.connection.ConnectionProps;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.rolap.common.sql.SqlQuery;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.model.AccessCatalogGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessCubeGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessHierarchyGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessMemberGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessRole;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.CatalogAccess;
import org.eclipse.daanse.rolap.mapping.model.ColumnInternalDataType;
import org.eclipse.daanse.rolap.mapping.model.ColumnType;
import org.eclipse.daanse.rolap.mapping.model.Cube;
import org.eclipse.daanse.rolap.mapping.model.CubeAccess;
import org.eclipse.daanse.rolap.mapping.model.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.Hierarchy;
import org.eclipse.daanse.rolap.mapping.model.HierarchyAccess;
import org.eclipse.daanse.rolap.mapping.model.Level;
import org.eclipse.daanse.rolap.mapping.model.MeasureGroup;
import org.eclipse.daanse.rolap.mapping.model.MemberAccess;
import org.eclipse.daanse.rolap.mapping.model.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.RollupPolicy;
import org.eclipse.daanse.rolap.mapping.model.SQLExpressionColumn;
import org.eclipse.daanse.rolap.mapping.model.SqlStatement;
import org.eclipse.daanse.rolap.mapping.model.StandardDimension;
import org.eclipse.daanse.rolap.mapping.model.SumMeasure;
import org.eclipse.daanse.rolap.mapping.model.TableQuery;
import org.eclipse.daanse.rolap.mapping.model.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.enums.DatabaseProduct;
import mondrian.rolap.BatchTestCase;
import mondrian.rolap.SchemaModifiersEmf;
import mondrian.test.SqlPattern;

/**
 * Test for <code>SqlQuery</code>.
 *
 * @author Thiyagu
 * @since 06-Jun-2007
 */
class SqlQueryTest  extends BatchTestCase {
    //private String origWarnIfNoPatternForDialect;

    @BeforeEach
    public void beforeEach() {

        //origWarnIfNoPatternForDialect = prop.WarnIfNoPatternForDialect.get();
    }

    private void prepareContext(Connection connection) {
        // This test warns of missing sql patterns for MYSQL.
        final Dialect dialect = getDialect(connection);
        if (connection.getContext()
                .getConfigValue(ConfigConstants.WARN_IF_NO_PATTERN_FOR_DIALECT, ConfigConstants.WARN_IF_NO_PATTERN_FOR_DIALECT_DEFAULT_VALUE, String.class)
                .equals("ANY")
                || getDatabaseProduct(dialect.getDialectName()) == MYSQL)
        {
            ((TestContextImpl)(connection.getContext())).setWarnIfNoPatternForDialect(
                    getDatabaseProduct(dialect.getDialectName()).toString());
        } else {
            // Do not warn unless the dialect is "MYSQL", or
            // if the test chooses to warn regardless of the dialect.
            ((TestContextImpl)(connection.getContext())).setWarnIfNoPatternForDialect("NONE");
        }

    }

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
        //prop.WarnIfNoPatternForDialect.set(origWarnIfNoPatternForDialect);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testToStringForSingleGroupingSetSql(Context<?> context) {
        Connection connection = context.getConnectionWithDefaultRole();
        prepareContext(connection);
        if (!isGroupingSetsSupported(connection)) {
            return;
        }
        for (boolean b : new boolean[]{false, true}) {
            Dialect dialect = getDialect(connection);
            SqlQuery sqlQuery = new SqlQuery(dialect, b);
            sqlQuery.addSelect("c1", null);
            sqlQuery.addSelect("c2", null);
            sqlQuery.addGroupingFunction("gf0");
            sqlQuery.addFromTable("s", "t1", "t1alias", null, null, true);
            sqlQuery.addWhere("a=b");
            ArrayList<String> groupingsetsList = new ArrayList<>();
            groupingsetsList.add("gs1");
            groupingsetsList.add("gs2");
            groupingsetsList.add("gs3");
            sqlQuery.addGroupingSet(groupingsetsList);
            String expected;
            String lineSep = System.getProperty("line.separator");
            if (!b) {
                expected =
                    "select c1 as \"c0\", c2 as \"c1\", grouping(gf0) as \"g0\" "
                    + "from \"s\".\"t1\" =as= \"t1alias\" where a=b "
                    + "group by grouping sets ((gs1, gs2, gs3))";
            } else {
                expected =
                    "select" + lineSep
                    + "    c1 as \"c0\"," + lineSep
                    + "    c2 as \"c1\"," + lineSep
                    + "    grouping(gf0) as \"g0\"" + lineSep
                    + "from" + lineSep
                    + "    \"s\".\"t1\" =as= \"t1alias\"" + lineSep
                    + "where" + lineSep
                    + "    a=b" + lineSep
                    + "group by grouping sets (" + lineSep
                    + "    (gs1, gs2, gs3))";
            }
            assertEquals(
                dialectize(getDatabaseProduct(dialect.getDialectName()), expected),
                dialectize(
                    getDatabaseProduct(sqlQuery.getDialect().getDialectName()),
                    sqlQuery.toString()));
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOrderBy(Context<?> context) throws SQLException {
        Connection connection = context.getConnectionWithDefaultRole();
        prepareContext(connection);
        // Test with requireAlias = true
        assertEquals(
            queryUnixString("expr", "alias", true, true, true, true),
            "\norder by\n"
            + "    CASE WHEN alias IS NULL THEN 1 ELSE 0 END, alias ASC");
        // requireAlias = false
        assertEquals(
            "\norder by\n"
            + "    CASE WHEN expr IS NULL THEN 1 ELSE 0 END, expr ASC",
            queryUnixString("expr", "alias", true, true, true, false));
        //  nullable = false
        assertEquals(
            "\norder by\n"
            + "    expr ASC",
            queryUnixString("expr", "alias", true, false, true, false));
        //  ascending=false, collateNullsLast=false
        assertEquals(
            "\norder by\n"
            + "    CASE WHEN alias IS NULL THEN 0 ELSE 1 END, alias DESC",
            queryUnixString("expr", "alias", false, true, false, true));
    }

    /**
     * Builds a SqlQuery with flags set according to params.
     * Uses a Mockito spy to construct a dialect which will give the desired
     * boolean value for reqOrderByAlias.
     */

    private SqlQuery makeTestSqlQuery(
        String expr, String alias, boolean ascending,
        boolean nullable, boolean collateNullsLast, boolean reqOrderByAlias)
    {
        JdbcDialectImpl dialect = spy(new JdbcDialectImplForTest());
        when(dialect.requiresOrderByAlias()).thenReturn(reqOrderByAlias);
        SqlQuery query = new SqlQuery(dialect, true);
        query.addOrderBy(
            expr, alias, ascending, true, nullable, collateNullsLast);
        return query;
    }

    private String queryUnixString(
        String expr, String alias, boolean ascending,
        boolean nullable, boolean collateNullsLast, boolean reqOrderByAlias)
    {
        String sql = makeTestSqlQuery(
            expr, alias, ascending, nullable, collateNullsLast,
            reqOrderByAlias).toString();
        return sql.replaceAll("\\r", "");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testToStringForForcedIndexHint(Context<?> context) {
        Connection connection = context.getConnectionWithDefaultRole();
        prepareContext(connection);
        Map<String, String> hints = new HashMap<>();
        hints.put("force_index", "myIndex");

        String unformattedMysql =
            "select c1 as `c0`, c2 as `c1` "
            + "from `s`.`t1` as `t1alias`"
            + " FORCE INDEX (myIndex)"
            + " where a=b";
        String formattedMysql =
            "select\n"
            + "    c1 as `c0`,\n"
            + "    c2 as `c1`\n"
            + "from\n"
            + "    `s`.`t1` as `t1alias` FORCE INDEX (myIndex)\n"
            + "where\n"
            + "    a=b";

        SqlPattern[] unformattedSqlPatterns = {
            new SqlPattern(
                MYSQL,
                unformattedMysql,
                null)};
        SqlPattern[] formattedSqlPatterns = {
            new SqlPattern(
                MYSQL,
                formattedMysql,
                null)};
        for (boolean formatted : new boolean[]{false, true}) {
            Dialect dialect = getDialect(connection);
            SqlQuery sqlQuery = new SqlQuery(dialect, formatted);
            sqlQuery.setAllowHints(true);
            sqlQuery.addSelect("c1", null);
            sqlQuery.addSelect("c2", null);
            sqlQuery.addGroupingFunction("gf0");
            sqlQuery.addFromTable("s", "t1", "t1alias", null, hints, true);
            sqlQuery.addWhere("a=b");
            SqlPattern[] expected;
            if (!formatted) {
                expected = unformattedSqlPatterns;
            } else {
                expected = formattedSqlPatterns;
            }
            assertSqlQueryToStringMatches(connection, sqlQuery, expected);
        }
    }

    private void assertSqlQueryToStringMatches(Connection connection,
        SqlQuery query,
        SqlPattern[] patterns)
    {
        Dialect dialect = getDialect(connection);
        DatabaseProduct d = getDatabaseProduct(dialect.getDialectName());
        boolean patternFound = false;
        for (SqlPattern sqlPattern : patterns) {
            if (!sqlPattern.hasDatabaseProduct(d)) {
                // If the dialect is not one in the pattern set, skip the
                // test. If in the end no pattern is located, print a warning
                // message if required.
                continue;
            }

            patternFound = true;

            String trigger = sqlPattern.getTriggerSql();

            trigger = dialectize(d, trigger);

            assertEquals(
                dialectize(getDatabaseProduct(dialect.getDialectName()), trigger),
                dialectize(
                    getDatabaseProduct(query.getDialect().getDialectName()),
                    query.toString()));
        }

        // Print warning message that no pattern was specified for the current
        // dialect.
        if (!patternFound) {
            String warnDialect =
                connection.getContext().getConfigValue(ConfigConstants.WARN_IF_NO_PATTERN_FOR_DIALECT, ConfigConstants.WARN_IF_NO_PATTERN_FOR_DIALECT_DEFAULT_VALUE, String.class);

            if (warnDialect.equals(d.toString())) {
                System.out.println(
                    "[No expected SQL statements found for dialect \""
                    + dialect.toString()
                    + "\" and test not run]");
            }
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testPredicatesAreOptimizedWhenPropertyIsTrue(Context<?> context) {
        Connection connection = context.getConnectionWithDefaultRole();
        prepareContext(connection);
        if (context.getConfigValue(ConfigConstants.READ_AGGREGATES, ConfigConstants.READ_AGGREGATES_DEFAULT_VALUE ,Boolean.class) && context.getConfigValue(ConfigConstants.USE_AGGREGATES, ConfigConstants.USE_AGGREGATES_DEFAULT_VALUE ,Boolean.class)) {
            // Sql pattner will be different if using aggregate tables.
            // This test cover predicate generation so it's sufficient to
            // only check sql pattern when aggregate tables are not used.
            return;
        }

        String mdx =
            "select {[Time].[1997].[Q1],[Time].[1997].[Q2],"
            + "[Time].[1997].[Q3]} on 0 from sales";

        String accessSql =
            "select `time_by_day`.`the_year` as `c0`, "
            + "`time_by_day`.`quarter` as `c1`, "
            + "sum(`sales_fact_1997`.`unit_sales`) as `m0` "
            + "from `sales_fact_1997` as `sales_fact_1997`, "
            + "`time_by_day` as `time_by_day` "
            + "where `sales_fact_1997`.`time_id` = "
            + "`time_by_day`.`time_id` and "
            + "`time_by_day`.`the_year` = 1997 group by "
            + "`time_by_day`.`the_year`, `time_by_day`.`quarter`";

        String mysqlSql =
            "select "
            + "`time_by_day`.`the_year` as `c0`, `time_by_day`.`quarter` as `c1`, "
            + "sum(`sales_fact_1997`.`unit_sales`) as `m0` "
            + "from "
            + "`sales_fact_1997` as `sales_fact_1997`, `time_by_day` as `time_by_day` "
            + "where "
            + "`sales_fact_1997`.`time_id` = `time_by_day`.`time_id` and "
            + "`time_by_day`.`the_year` = 1997 "
            + "group by `time_by_day`.`the_year`, `time_by_day`.`quarter`";

        SqlPattern[] sqlPatterns = {
            new SqlPattern(
                DatabaseProduct.ACCESS, accessSql, accessSql),
            new SqlPattern(MYSQL, mysqlSql, mysqlSql)};

        assertSqlEqualsOptimzePredicates(context, true, mdx, sqlPatterns);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTableNameIsIncludedWithParentChildQuery(Context<?> context) {
        Connection connection = context.getConnectionWithDefaultRole();
        prepareContext(connection);
        String sql =
            "select `employee`.`employee_id` as `c0`, "
            + "`employee`.`full_name` as `c1`, "
            + "`employee`.`marital_status` as `c2`, "
            + "`employee`.`position_title` as `c3`, "
            + "`employee`.`gender` as `c4`, "
            + "`employee`.`salary` as `c5`, "
            + "`employee`.`education_level` as `c6`, "
            + "`employee`.`management_role` as `c7` "
            + "from `employee` as `employee` "
            + "where `employee`.`supervisor_id` = 0 "
            + "group by `employee`.`employee_id`, `employee`.`full_name`, "
            + "`employee`.`marital_status`, `employee`.`position_title`, "
            + "`employee`.`gender`, `employee`.`salary`,"
            + " `employee`.`education_level`, `employee`.`management_role`"
            + " order by Iif(`employee`.`employee_id` IS NULL, 1, 0),"
            + " `employee`.`employee_id` ASC";

        final String mdx =
            "SELECT "
            + "  GENERATE("
            + "    {[Employees].[All Employees].[Sheri Nowmer]},"
            + "{"
            + "  {([Employees].CURRENTMEMBER)},"
            + "  HEAD("
            + "    ADDCALCULATEDMEMBERS([Employees].CURRENTMEMBER.CHILDREN), 51)"
            + "},"
            + "ALL"
            + ") DIMENSION PROPERTIES PARENT_LEVEL, CHILDREN_CARDINALITY, PARENT_UNIQUE_NAME ON AXIS(0) \n"
            + "FROM [HR]  CELL PROPERTIES VALUE, FORMAT_STRING";
        SqlPattern[] sqlPatterns = {
            new SqlPattern(DatabaseProduct.ACCESS, sql, sql)
        };
        assertQuerySql(context.getConnectionWithDefaultRole(), mdx, sqlPatterns);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testPredicatesAreNotOptimizedWhenPropertyIsFalse(Context<?> context) {
        Connection connection = context.getConnectionWithDefaultRole();
        prepareContext(connection);
        if (context.getConfigValue(ConfigConstants.READ_AGGREGATES, ConfigConstants.READ_AGGREGATES_DEFAULT_VALUE ,Boolean.class) && context.getConfigValue(ConfigConstants.USE_AGGREGATES, ConfigConstants.USE_AGGREGATES_DEFAULT_VALUE ,Boolean.class)) {
            // Sql pattner will be different if using aggregate tables.
            // This test cover predicate generation so it's sufficient to
            // only check sql pattern when aggregate tables are not used.
            return;
        }

        String mdx =
            "select {[Time].[1997].[Q1],[Time].[1997].[Q2],"
            + "[Time].[1997].[Q3]} on 0 from sales";
        String accessSql =
            "select `time_by_day`.`the_year` as `c0`, "
            + "`time_by_day`.`quarter` as `c1`, "
            + "sum(`sales_fact_1997`.`unit_sales`) as `m0` "
            + "from `sales_fact_1997` as `sales_fact_1997`, "
            + "`time_by_day` as `time_by_day` "
            + "where `sales_fact_1997`.`time_id` = "
            + "`time_by_day`.`time_id` and `time_by_day`.`the_year` "
            + "= 1997 and `time_by_day`.`quarter` in "
            + "('Q1', 'Q2', 'Q3') group by "
            + "`time_by_day`.`the_year`, `time_by_day`.`quarter`";

        String mysqlSql =
            "select "
            + "`time_by_day`.`the_year` as `c0`, `time_by_day`.`quarter` as `c1`, "
            + "sum(`sales_fact_1997`.`unit_sales`) as `m0` "
            + "from "
            + "`sales_fact_1997` as `sales_fact_1997`, `time_by_day` as `time_by_day` "
            + "where "
            + "`sales_fact_1997`.`time_id` = `time_by_day`.`time_id` and "
            + "`time_by_day`.`the_year` = 1997 and "
            + "`time_by_day`.`quarter` in ('Q1', 'Q2', 'Q3') "
            + "group by `time_by_day`.`the_year`, `time_by_day`.`quarter`";

        SqlPattern[] sqlPatterns = {
            new SqlPattern(
                DatabaseProduct.ACCESS, accessSql, accessSql),
            new SqlPattern(MYSQL, mysqlSql, mysqlSql)};

        assertSqlEqualsOptimzePredicates(context, false, mdx, sqlPatterns);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testPredicatesAreOptimizedWhenAllTheMembersAreIncluded(Context<?> context) {
        Connection connection = context.getConnectionWithDefaultRole();
        prepareContext(connection);
        if (context.getConfigValue(ConfigConstants.READ_AGGREGATES, ConfigConstants.READ_AGGREGATES_DEFAULT_VALUE ,Boolean.class) && context.getConfigValue(ConfigConstants.USE_AGGREGATES, ConfigConstants.USE_AGGREGATES_DEFAULT_VALUE ,Boolean.class)) {
            // Sql pattner will be different if using aggregate tables.
            // This test cover predicate generation so it's sufficient to
            // only check sql pattern when aggregate tables are not used.
            return;
        }

        String mdx =
            "select {[Time].[1997].[Q1],[Time].[1997].[Q2],"
            + "[Time].[1997].[Q3],[Time].[1997].[Q4]} on 0 from sales";

        String accessSql =
            "select `time_by_day`.`the_year` as `c0`, "
            + "`time_by_day`.`quarter` as `c1`, "
            + "sum(`sales_fact_1997`.`unit_sales`) as `m0` from "
            + "`sales_fact_1997` as `sales_fact_1997,` `time_by_day` as `time_by_day`"
            + " where `sales_fact_1997`.`time_id`"
            + " = `time_by_day`.`time_id` and `time_by_day`."
            + "`the_year` = 1997 group by `time_by_day`.`the_year`,"
            + " `time_by_day`.`quarter`";

        String mysqlSql =
            "select "
            + "`time_by_day`.`the_year` as `c0`, `time_by_day`.`quarter` as `c1`, "
            + "sum(`sales_fact_1997`.`unit_sales`) as `m0` "
            + "from "
            + "`sales_fact_1997` as `sales_fact_1997`, `time_by_day` as `time_by_day` "
            + "where "
            + "`sales_fact_1997`.`time_id` = `time_by_day`.`time_id` and "
            + "`time_by_day`.`the_year` = 1997 "
            + "group by `time_by_day`.`the_year`, `time_by_day`.`quarter`";

        SqlPattern[] sqlPatterns = {
            new SqlPattern(
                DatabaseProduct.ACCESS, accessSql, accessSql),
            new SqlPattern(MYSQL, mysqlSql, mysqlSql)};

        assertSqlEqualsOptimzePredicates(context, true, mdx, sqlPatterns);
        assertSqlEqualsOptimzePredicates(context, false, mdx, sqlPatterns);
    }

    private void assertSqlEqualsOptimzePredicates(Context<?> context,
        boolean optimizePredicatesValue,
        String inputMdx,
        SqlPattern[] sqlPatterns)
    {
        boolean intialValueOptimize =
            context.getConfigValue(ConfigConstants.OPTIMIZE_PREDICATES, ConfigConstants.OPTIMIZE_PREDICATES_DEFAULT_VALUE, Boolean.class);

        try {
            ((TestContextImpl)context).setOptimizePredicates(optimizePredicatesValue);
            assertQuerySql(context.getConnectionWithDefaultRole(), inputMdx, sqlPatterns);
        } finally {
            ((TestContextImpl)context).setOptimizePredicates(intialValueOptimize);
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testToStringForGroupingSetSqlWithEmptyGroup(Context<?> context) {
        Connection connection = context.getConnectionWithDefaultRole();
        prepareContext(connection);
        if (!isGroupingSetsSupported(connection)) {
            return;
        }
        final Dialect dialect = getDialect(connection);
        for (boolean b : new boolean[]{false, true}) {
            SqlQuery sqlQuery = new SqlQuery(dialect, b);
            sqlQuery.addSelect("c1", null);
            sqlQuery.addSelect("c2", null);
            sqlQuery.addFromTable("s", "t1", "t1alias", null, null, true);
            sqlQuery.addWhere("a=b");
            sqlQuery.addGroupingFunction("g1");
            sqlQuery.addGroupingFunction("g2");
            ArrayList<String> groupingsetsList = new ArrayList<>();
            groupingsetsList.add("gs1");
            groupingsetsList.add("gs2");
            groupingsetsList.add("gs3");
            sqlQuery.addGroupingSet(new ArrayList<String>());
            sqlQuery.addGroupingSet(groupingsetsList);
            String expected;
            if (b) {
                expected =
                    "select\n"
                    + "    c1 as \"c0\",\n"
                    + "    c2 as \"c1\",\n"
                    + "    grouping(g1) as \"g0\",\n"
                    + "    grouping(g2) as \"g1\"\n"
                    + "from\n"
                    + "    \"s\".\"t1\" =as= \"t1alias\"\n"
                    + "where\n"
                    + "    a=b\n"
                    + "group by grouping sets (\n"
                    + "    (),\n"
                    + "    (gs1, gs2, gs3))";
            } else {
                expected =
                    "select c1 as \"c0\", c2 as \"c1\", grouping(g1) as \"g0\", "
                    + "grouping(g2) as \"g1\" from \"s\".\"t1\" =as= \"t1alias\" where a=b "
                    + "group by grouping sets ((), (gs1, gs2, gs3))";
            }
            assertEquals(
                dialectize(getDatabaseProduct(dialect.getDialectName()), expected),
                dialectize(
                    getDatabaseProduct(sqlQuery.getDialect().getDialectName()),
                    sqlQuery.toString()));
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testToStringForMultipleGroupingSetsSql(Context<?> context) {
        Connection connection = context.getConnectionWithDefaultRole();
        prepareContext(connection);
        if (!isGroupingSetsSupported(connection)) {
            return;
        }
        final Dialect dialect = getDialect(connection);
        for (boolean b : new boolean[]{false, true}) {
            SqlQuery sqlQuery = new SqlQuery(dialect, b);
            sqlQuery.addSelect("c0", null);
            sqlQuery.addSelect("c1", null);
            sqlQuery.addSelect("c2", null);
            sqlQuery.addSelect("m1", null, "m1");
            sqlQuery.addFromTable("s", "t1", "t1alias", null, null, true);
            sqlQuery.addWhere("a=b");
            sqlQuery.addGroupingFunction("c0");
            sqlQuery.addGroupingFunction("c1");
            sqlQuery.addGroupingFunction("c2");
            ArrayList<String> groupingSetlist1 = new ArrayList<>();
            groupingSetlist1.add("c0");
            groupingSetlist1.add("c1");
            groupingSetlist1.add("c2");
            sqlQuery.addGroupingSet(groupingSetlist1);
            ArrayList<String> groupingsetsList2 = new ArrayList<>();
            groupingsetsList2.add("c1");
            groupingsetsList2.add("c2");
            sqlQuery.addGroupingSet(groupingsetsList2);
            String expected;
            if (b) {
                expected =
                    "select\n"
                    + "    c0 as \"c0\",\n"
                    + "    c1 as \"c1\",\n"
                    + "    c2 as \"c2\",\n"
                    + "    m1 as \"m1\",\n"
                    + "    grouping(c0) as \"g0\",\n"
                    + "    grouping(c1) as \"g1\",\n"
                    + "    grouping(c2) as \"g2\"\n"
                    + "from\n"
                    + "    \"s\".\"t1\" =as= \"t1alias\"\n"
                    + "where\n"
                    + "    a=b\n"
                    + "group by grouping sets (\n"
                    + "    (c0, c1, c2),\n"
                    + "    (c1, c2))";
            } else {
                expected =
                    "select c0 as \"c0\", c1 as \"c1\", c2 as \"c2\", m1 as \"m1\", "
                    + "grouping(c0) as \"g0\", grouping(c1) as \"g1\", grouping(c2) as \"g2\" "
                    + "from \"s\".\"t1\" =as= \"t1alias\" where a=b "
                    + "group by grouping sets ((c0, c1, c2), (c1, c2))";
            }
            assertEquals(
                dialectize(getDatabaseProduct(dialect.getDialectName()), expected),
                dialectize(
                    getDatabaseProduct(sqlQuery.getDialect().getDialectName()),
                    sqlQuery.toString()));
        }
    }

    /**
     * Verifies that the correct SQL string is generated for literals of
     * SQL type "double".
     *
     * <p>Mondrian only generates SQL DOUBLE values in a special format for
     * LucidDB; therefore, this test is a no-op on other databases.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDoubleInList(Context<?> context) {
        Connection connection = context.getConnectionWithDefaultRole();
        prepareContext(connection);
        final Dialect dialect = getDialect(context.getConnectionWithDefaultRole());
        if (getDatabaseProduct(dialect.getDialectName()) != DatabaseProduct.LUCIDDB) {
            return;
        }

        ((TestContextImpl)context).setIgnoreInvalidMembers(true);
        ((TestContextImpl)context).setIgnoreInvalidMembersDuringQuery(true);

        // assertQuerySql(testContext, query, patterns);

        // Test when the double value itself cotnains "E".
        String dimensionSqlExpression =
            "cast(cast(\"salary\" as double)*cast(1000.0 as double)/cast(3.1234567890123456 as double) as double)\n";

        String cubeFirstPart =
            "<Cube name=\"Sales 3\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <Dimension name=\"StoreEmpSalary\" foreignKey=\"store_id\">\n"
            + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Salary\" primaryKey=\"store_id\">\n"
            + "      <Table name=\"employee\"/>\n"
            + "      <Level name=\"Salary\" column=\"salary\" type=\"Numeric\" uniqueMembers=\"true\" approxRowCount=\"10000000\">\n"
            + "        <KeyExpression>\n"
            + "          <SQL dialect=\"luciddb\">\n";

        String cubeSecondPart =
            "          </SQL>\n"
            + "        </KeyExpression>\n"
            + "      </Level>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>"
            + "  <Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\"/>\n"
            + "</Cube>";

        String cube =
            cubeFirstPart
            + dimensionSqlExpression
            + cubeSecondPart;

        String query =
            "select "
            + "{[StoreEmpSalary].[All Salary].[6403.162057613773],[StoreEmpSalary].[All Salary].[1184584.980658548],[StoreEmpSalary].[All Salary].[1344664.0320988924], "
            + " [StoreEmpSalary].[All Salary].[1376679.8423869612],[StoreEmpSalary].[All Salary].[1408695.65267503],[StoreEmpSalary].[All Salary].[1440711.462963099], "
            + " [StoreEmpSalary].[All Salary].[1456719.3681071333],[StoreEmpSalary].[All Salary].[1472727.2732511677],[StoreEmpSalary].[All Salary].[1488735.1783952022], "
            + " [StoreEmpSalary].[All Salary].[1504743.0835392366],[StoreEmpSalary].[All Salary].[1536758.8938273056],[StoreEmpSalary].[All Salary].[1600790.5144034433], "
            + " [StoreEmpSalary].[All Salary].[1664822.134979581],[StoreEmpSalary].[All Salary].[1888932.806996063],[StoreEmpSalary].[All Salary].[1952964.4275722008], "
            + " [StoreEmpSalary].[All Salary].[1984980.2378602696],[StoreEmpSalary].[All Salary].[2049011.8584364073],[StoreEmpSalary].[All Salary].[2081027.6687244761], "
            + " [StoreEmpSalary].[All Salary].[2113043.479012545],[StoreEmpSalary].[All Salary].[2145059.289300614],[StoreEmpSalary].[All Salary].[2.5612648230455093E7]} "
            + " on rows, {[Measures].[Store Cost]} on columns from [Sales 3]";

        // Notice there are a few members missing in this sql. This is a LucidDB
        // bug wrt comparison involving "approximate number literals".
        // Mondrian properties "IgnoreInvalidMembers" and
        // "IgnoreInvalidMembersDuringQuery" are required for this MDX to
        // finish, even though the the generated sql(below) and the final result
        // are both incorrect.
        String loadSqlLucidDB =
            "select cast(cast(\"salary\" as double)*cast(1000.0 as double)/cast(3.1234567890123456 as double) as double) as \"c0\", "
            + "sum(\"sales_fact_1997\".\"store_cost\") as \"m0\" "
            + "from \"employee\" as \"employee\", \"sales_fact_1997\" as \"sales_fact_1997\" "
            + "where \"sales_fact_1997\".\"store_id\" = \"employee\".\"store_id\" and "
            + "cast(cast(\"salary\" as double)*cast(1000.0 as double)/cast(3.1234567890123456 as double) as double) in "
            + "(6403.162057613773E0, 1184584.980658548E0, 1344664.0320988924E0, "
            + "1376679.8423869612E0, 1408695.65267503E0, 1440711.462963099E0, "
            + "1456719.3681071333E0, 1488735.1783952022E0, "
            + "1504743.0835392366E0, 1536758.8938273056E0, "
            + "1664822.134979581E0, 1888932.806996063E0, 1952964.4275722008E0, "
            + "1984980.2378602696E0, 2049011.8584364073E0, "
            + "2113043.479012545E0, 2145059.289300614E0, 2.5612648230455093E7) "
            + "group by cast(cast(\"salary\" as double)*cast(1000.0 as double)/cast(3.1234567890123456 as double) as double)";

        SqlPattern[] patterns = {
            new SqlPattern(
                DatabaseProduct.LUCIDDB,
                loadSqlLucidDB,
                loadSqlLucidDB)
        };
        /*
        class TestDoubleInListModifier extends PojoMappingModifier {

            public TestDoubleInListModifier(CatalogMapping catalog) {
                super(catalog);
            }
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                result.add(PhysicalCubeMappingImpl.builder()
                    .withName("Sales 3")
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("StoreEmpSalary")
                            .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("StoreEmpSalary")
                            	.withHierarchies(List.of(
                                ExplicitHierarchyMappingImpl.builder()
                                    .withHasAll(true)
                                    .withAllMemberName("All Salary")
                                    .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_EMPLOYEE)
                                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.EMPLOYEE_TABLE).build())
                                    .withLevels(List.of(
                                        LevelMappingImpl.builder()
                                            .withName("Salary").withColumn(FoodmartMappingSupplier.SALARY_COLUMN_IN_EMPLOYEE)
                                            .withType(InternalDataType.NUMERIC).withUniqueMembers(true)
                                            .withApproxRowCount("10000000")
                                            .withCaptionColumn(
                                                SQLExpressionMappingColumnImpl.builder()
                                                    .withSqls(List.of(
                                                        SqlStatementMappingImpl.builder()
                                                            .withDialects(List.of("luciddb"))
                                                            .withSql("cast(cast(\"salary\" as double)*cast(1000.0 as double)/cast(3.1234567890123456 as double) as double)")
                                                            .build()
                                                    ))
                                                    .withDataType(ColumnDataType.DECIMAL)
                                                    .build()
                                            )
                                            .build()
                                    )).build())).build())
                                    .build()))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                            SumMeasureMappingImpl.builder()
                            .withName("Store Cost")
                            .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_SALES_FACT_1997)
                            .build()
                    )).build()))
                    .build());
                return result;
            }
        }
        */
        /**
         * EMF version of TestDoubleInListModifier
         * Creates Sales 3 cube with StoreEmpSalary dimension containing SQL expression for caption
         */
        class TestDoubleInListModifierEmf implements CatalogMappingSupplier {

            private CatalogImpl catalog;

            public TestDoubleInListModifierEmf(Catalog cat) {
                // Copy catalog using EcoreUtil
                catalog = org.opencube.junit5.EmfUtil.copy((CatalogImpl) cat);

                // Create cube
                PhysicalCube cube =
                    RolapMappingFactory.eINSTANCE.createPhysicalCube();
                cube.setName("Sales 3");

                // Set up query
                TableQuery tableQuery =
                    RolapMappingFactory.eINSTANCE.createTableQuery();
                tableQuery.setTable(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.TABLE_SALES_FACT);
                cube.setQuery(tableQuery);

                // Create SQL expression for caption column
                SQLExpressionColumn captionExpression =
                    RolapMappingFactory.eINSTANCE.createSQLExpressionColumn();
                captionExpression.setType(ColumnType.DECIMAL);

                // Create SQL statement for LucidDB
                SqlStatement sqlStatement =
                    RolapMappingFactory.eINSTANCE.createSqlStatement();
                sqlStatement.getDialects().add("luciddb");
                sqlStatement.setSql("cast(cast(\"salary\" as double)*cast(1000.0 as double)/cast(3.1234567890123456 as double) as double)");
                captionExpression.getSqls().add(sqlStatement);

                // Create Salary level
                Level salaryLevel =
                    RolapMappingFactory.eINSTANCE.createLevel();
                salaryLevel.setName("Salary");
                salaryLevel.setColumn(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.COLUMN_SALARY_EMPLOYEE);
                salaryLevel.setColumnType(ColumnInternalDataType.NUMERIC);
                salaryLevel.setUniqueMembers(true);
                salaryLevel.setApproxRowCount("10000000");
                salaryLevel.setCaptionColumn(captionExpression);

                // Create hierarchy
                ExplicitHierarchy hierarchy =
                    RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
                hierarchy.setHasAll(true);
                hierarchy.setAllMemberName("All Salary");
                hierarchy.setPrimaryKey(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.COLUMN_STORE_ID_EMPLOYEE);

                TableQuery employeeTableQuery =
                    RolapMappingFactory.eINSTANCE.createTableQuery();
                employeeTableQuery.setTable(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.TABLE_EMPLOYEE);
                hierarchy.setQuery(employeeTableQuery);

                hierarchy.getLevels().add(salaryLevel);

                // Create dimension
                StandardDimension dimension =
                    RolapMappingFactory.eINSTANCE.createStandardDimension();
                dimension.setName("StoreEmpSalary");
                dimension.getHierarchies().add(hierarchy);

                // Create dimension connector
                DimensionConnector dimConnector =
                    RolapMappingFactory.eINSTANCE.createDimensionConnector();
                dimConnector.setOverrideDimensionName("StoreEmpSalary");
                dimConnector.setForeignKey(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.COLUMN_STORE_ID_SALESFACT);
                dimConnector.setDimension(dimension);

                cube.getDimensionConnectors().add(dimConnector);

                // Create measure
                SumMeasure storeCostMeasure =
                    RolapMappingFactory.eINSTANCE.createSumMeasure();
                storeCostMeasure.setName("Store Cost");
                storeCostMeasure.setColumn(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.COLUMN_STORE_COST_SALESFACT);

                // Create measure group
                MeasureGroup measureGroup =
                    RolapMappingFactory.eINSTANCE.createMeasureGroup();
                measureGroup.getMeasures().add(storeCostMeasure);

                cube.getMeasureGroups().add(measureGroup);

                // Add cube to catalog
                catalog.getCubes().add(cube);
            }

            @Override
            public Catalog get() {
                return catalog;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
                null,
                cube,
                null,
                null,
                null,
                null);
        withSchema(context, schema);
         */
        withSchemaEmf(context, TestDoubleInListModifierEmf::new);
        assertQuerySql(context.getConnectionWithDefaultRole(), query, patterns);
    }

    /**
     * Testcase for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-457">bug MONDRIAN-457,
     * "Strange SQL condition appears when executing query"</a>. The fix
     * implemented MatchType.EXACT_SCHEMA, which only
     * queries known schema objects. This prevents SQL such as
     * "UPPER(`store`.`store_country`) = UPPER('Time.Weekly')" from being
     * generated.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testInvalidSqlMemberLookup(Context<?> context) {
        Connection connection = context.getConnectionWithDefaultRole();
        prepareContext(connection);
        String sqlMySql =
            "select `store`.`store_type` as `c0` from `store` as `store` "
            + "where UPPER(`store`.`store_type`) = UPPER('Time.Weekly') "
            + "group by `store`.`store_type` "
            + "order by ISNULL(`store`.`store_type`), `store`.`store_type` ASC";
        String sqlOracle =
            "select \"store\".\"store_type\" as \"c0\" from \"store\" \"store\" "
            + "where UPPER(\"store\".\"store_type\") = UPPER('Time.Weekly') "
            + "group by \"store\".\"store_type\" "
            + "order by \"store\".\"store_type\" ASC";

        SqlPattern[] patterns = {
            new SqlPattern(MYSQL, sqlMySql, sqlMySql),
            new SqlPattern(
                DatabaseProduct.ORACLE, sqlOracle, sqlOracle),
        };

        assertNoQuerySql(connection,
            "select {[Time].[Weekly].[All Weeklys]} ON COLUMNS from [Sales]",
            patterns);
    }

    /**
     * This test makes sure that a level which specifies an
     * approxRowCount property prevents Mondrian from executing a
     * count() sql query. It was discovered in bug MONDRIAN-711
     * that the aggregate tables predicates optimization code was
     * not considering the approxRowCount property. It is fixed and
     * this test will ensure it won't happen again.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testApproxRowCountOverridesCount(Context<?> context) {
        Connection connection = context.getConnectionWithDefaultRole();
        prepareContext(connection);
        final String cubeSchema =
            "<Cube name=\"ApproxTest\"> \n"
            + "  <Table name=\"sales_fact_1997\"/> \n"
            + "  <Dimension name=\"Gender\" foreignKey=\"customer_id\">\n"
            + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Gender\" primaryKey=\"customer_id\">\n"
            + "      <Table name=\"customer\"/>\n"
            + "      <Level name=\"Gender\" column=\"gender\" uniqueMembers=\"true\" approxRowCount=\"2\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"/> \n"
            + "</Cube>";

        final String mdxQuery =
            "SELECT {[Gender].[Gender].Members} ON ROWS, {[Measures].[Unit Sales]} ON COLUMNS FROM [ApproxTest]";

        final String forbiddenSqlOracle =
            "select count(distinct \"customer\".\"gender\") as \"c0\" from \"customer\" \"customer\"";

        final String forbiddenSqlMysql =
            "select count(distinct `customer`.`gender`) as `c0` from `customer` `customer`;";

        SqlPattern[] patterns = {
            new SqlPattern(
                DatabaseProduct.ORACLE, forbiddenSqlOracle, null),
            new SqlPattern(
                MYSQL, forbiddenSqlMysql, null)
        };
        /*
        class TestApproxRowCountOverridesCountModifier extends PojoMappingModifier {

            public TestApproxRowCountOverridesCountModifier(CatalogMapping catalog) {
                super(catalog);
            }

            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                result.add(PhysicalCubeMappingImpl.builder()
                    .withName("ApproxTest")
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Gender")
                            .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            		.withName("Gender")
                            		.withHierarchies(List.of(
                            			ExplicitHierarchyMappingImpl.builder()
                            			.withHasAll(true)
                            			.withAllMemberName("All Gender")
                            			.withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                            			.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                            			.withLevels(List.of(
                            				LevelMappingImpl.builder()
                                            .withName("Gender").withColumn(FoodmartMappingSupplier.GENDER_COLUMN_IN_CUSTOMER)
                                            .withType(InternalDataType.NUMERIC).withUniqueMembers(true)
                                            .withApproxRowCount("2")
                                            .build()
                                    ))
                                    .build()
                            )).build())
                            .build()
                    ))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                        SumMeasureMappingImpl.builder()
                            .withName("Unit Sales")
                            .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                            .build()
                    )).build()))
                    .build());
                return result;
            }
        }
        */
        /**
         * EMF version of TestApproxRowCountOverridesCountModifier
         * Creates ApproxTest cube with Gender dimension having approxRowCount
         */
        class TestApproxRowCountOverridesCountModifierEmf implements CatalogMappingSupplier {

            private CatalogImpl catalog;

            public TestApproxRowCountOverridesCountModifierEmf(Catalog cat) {
                // Copy catalog using EcoreUtil
                catalog = org.opencube.junit5.EmfUtil.copy((CatalogImpl) cat);

                // Create cube
                PhysicalCube cube =
                    RolapMappingFactory.eINSTANCE.createPhysicalCube();
                cube.setName("ApproxTest");

                // Set up query
                TableQuery tableQuery =
                    RolapMappingFactory.eINSTANCE.createTableQuery();
                tableQuery.setTable(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.TABLE_SALES_FACT);
                cube.setQuery(tableQuery);

                // Create Gender level
                Level genderLevel =
                    RolapMappingFactory.eINSTANCE.createLevel();
                genderLevel.setName("Gender");
                genderLevel.setColumn(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.COLUMN_GENDER_CUSTOMER);
                genderLevel.setColumnType(ColumnInternalDataType.NUMERIC);
                genderLevel.setUniqueMembers(true);
                genderLevel.setApproxRowCount("2");

                // Create hierarchy
                ExplicitHierarchy hierarchy =
                    RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
                hierarchy.setHasAll(true);
                hierarchy.setAllMemberName("All Gender");
                hierarchy.setPrimaryKey(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.COLUMN_CUSTOMER_ID_CUSTOMER);

                TableQuery customerTableQuery =
                    RolapMappingFactory.eINSTANCE.createTableQuery();
                customerTableQuery.setTable(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.TABLE_CUSTOMER);
                hierarchy.setQuery(customerTableQuery);

                hierarchy.getLevels().add(genderLevel);

                // Create dimension
                StandardDimension dimension =
                    RolapMappingFactory.eINSTANCE.createStandardDimension();
                dimension.setName("Gender");
                dimension.getHierarchies().add(hierarchy);

                // Create dimension connector
                DimensionConnector dimConnector =
                    RolapMappingFactory.eINSTANCE.createDimensionConnector();
                dimConnector.setOverrideDimensionName("Gender");
                dimConnector.setForeignKey(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.COLUMN_CUSTOMER_ID_SALESFACT);
                dimConnector.setDimension(dimension);

                cube.getDimensionConnectors().add(dimConnector);

                // Create measure
                SumMeasure unitSalesMeasure =
                    RolapMappingFactory.eINSTANCE.createSumMeasure();
                unitSalesMeasure.setName("Unit Sales");
                unitSalesMeasure.setColumn(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.COLUMN_UNIT_SALES_SALESFACT);

                // Create measure group
                MeasureGroup measureGroup =
                    RolapMappingFactory.eINSTANCE.createMeasureGroup();
                measureGroup.getMeasures().add(unitSalesMeasure);

                cube.getMeasureGroups().add(measureGroup);

                // Add cube to catalog
                catalog.getCubes().add(cube);
            }

            @Override
            public Catalog get() {
                return catalog;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
                null,
                cubeSchema,
                null,
                null,
                null,
                null);
        withSchema(context, schema);
         */
        withSchemaEmf(context, TestApproxRowCountOverridesCountModifierEmf::new);
        assertQuerySqlOrNot(
        	context.getConnectionWithDefaultRole(),
            mdxQuery,
            patterns,
            true,
            true,
            true);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLimitedRollupMemberRetrievableFromCache(Context<?> context) throws Exception {
        Connection connection = context.getConnectionWithDefaultRole();
        prepareContext(connection);
        final String mdx =
            "select NON EMPTY { [Store].[Store].[Store State].members } on 0 from [Sales]";
        /*
        class TestLimitedRollupMemberRetrievableFromCacheModifier extends PojoMappingModifier {

            public TestLimitedRollupMemberRetrievableFromCacheModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends AccessRoleMapping> catalogAccessRoles(CatalogMapping schema) {
                List<AccessRoleMapping> result = new ArrayList<>();
                result.addAll(super.catalogAccessRoles(schema));
                result.add(AccessRoleMappingImpl.builder()
                    .withName("justCA")
                    .withAccessCatalogGrants(List.of(
                    	AccessCatalogGrantMappingImpl.builder()
                            .withAccess(AccessCatalog.ALL)
                            .withCubeGrant(List.of(
                            	AccessCubeGrantMappingImpl.builder()
                                    .withCube((CubeMappingImpl) look(FoodmartMappingSupplier.CUBE_SALES))
                                    .withAccess(AccessCube.ALL)
                                    .withHierarchyGrants(List.of(
                                    	AccessHierarchyGrantMappingImpl.builder()
                                            .withHierarchy((HierarchyMappingImpl) look(FoodmartMappingSupplier.storeHierarchy))
                                            .withAccess(AccessHierarchy.CUSTOM)
                                            .withRollupPolicyType(RollupPolicyType.PARTIAL)
                                            .withMemberGrants(List.of(
                                            	AccessMemberGrantMappingImpl.builder()
                                                    .withMember("[Store].[USA].[CA]")
                                                    .withAccess(AccessMember.ALL)
                                                    .build()
                                            ))
                                            .build()
                                    ))
                                    .build()
                            ))
                            .build()
                    ))
                    .build());
                return result;
            }
        }
        */
        /**
         * EMF version of TestLimitedRollupMemberRetrievableFromCacheModifier
         * Creates access role 'justCA' with custom hierarchy access and partial rollup policy
         */
        class TestLimitedRollupMemberRetrievableFromCacheModifierEmf implements CatalogMappingSupplier {

            private CatalogImpl catalog;

            public TestLimitedRollupMemberRetrievableFromCacheModifierEmf(Catalog cat) {
                // Copy catalog using EcoreUtil
                EcoreUtil.Copier copier = org.opencube.junit5.EmfUtil.copier((CatalogImpl) cat);

                catalog = (CatalogImpl) copier.get(cat);

                // Find Sales cube
                //PhysicalCube salesCube = null;
                //Hierarchy storeHierarchy = null;

                /*
                for (Cube cube : catalog.getCubes()) {
                    if ("Sales".equals(cube.getName()) && cube instanceof PhysicalCube) {
                        salesCube = (PhysicalCube) cube;
                        // Find Store hierarchy
                        for (DimensionConnector dc : salesCube.getDimensionConnectors()) {
                            if (dc.getDimension() != null) {
                                for (Hierarchy h : dc.getDimension().getHierarchies()) {
                                    if ("Store".equals(h.getName())) {
                                        storeHierarchy = h;
                                        break;
                                    }
                                }
                            }
                            if (storeHierarchy != null) break;
                        }
                        break;
                    }
                }
                */
                // Create member grant
                AccessMemberGrant memberGrant =
                    RolapMappingFactory.eINSTANCE.createAccessMemberGrant();
                memberGrant.setMember("[Store].[USA].[CA]");
                memberGrant.setMemberAccess(MemberAccess.ALL);

                // Create hierarchy grant
                AccessHierarchyGrant hierarchyGrant =
                    RolapMappingFactory.eINSTANCE.createAccessHierarchyGrant();
                hierarchyGrant.setHierarchy((Hierarchy) copier.get(CatalogSupplier.HIERARCHY_STORE));
                hierarchyGrant.setHierarchyAccess(HierarchyAccess.CUSTOM);
                hierarchyGrant.setRollupPolicy(RollupPolicy.PARTIAL);
                hierarchyGrant.getMemberGrants().add(memberGrant);

                // Create cube grant
                AccessCubeGrant cubeGrant =
                    RolapMappingFactory.eINSTANCE.createAccessCubeGrant();
                cubeGrant.setCube((Cube) copier.get(CatalogSupplier.CUBE_SALES));
                cubeGrant.setCubeAccess(CubeAccess.ALL);
                cubeGrant.getHierarchyGrants().add(hierarchyGrant);

                // Create catalog grant
                AccessCatalogGrant catalogGrant =
                    RolapMappingFactory.eINSTANCE.createAccessCatalogGrant();
                catalogGrant.setCatalogAccess(CatalogAccess.ALL);
                catalogGrant.getCubeGrants().add(cubeGrant);

                // Create role
                AccessRole role =
                    RolapMappingFactory.eINSTANCE.createAccessRole();
                role.setName("justCA");
                role.getAccessCatalogGrants().add(catalogGrant);

                // Add role to catalog
                catalog.getAccessRoles().add(role);
            }

            @Override
            public Catalog get() {
                return catalog;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
                null, null, null, null, null,
                " <Role name='justCA'>\n"
                + " <SchemaGrant access='all'>\n"
                + " <CubeGrant cube='Sales' access='all'>\n"
                + " <HierarchyGrant hierarchy='[Store]' access='custom' rollupPolicy='partial'>\n"
                + " <MemberGrant member='[Store].[USA].[CA]' access='all'/>\n"
                + " </HierarchyGrant>\n"
                + " </CubeGrant>\n"
                + " </SchemaGrant>\n"
                + " </Role>\n");
        withSchema(context, schema);
         */
        withSchemaEmf(context, TestLimitedRollupMemberRetrievableFromCacheModifierEmf::new);

        String pgSql =
            "select \"store\".\"store_country\" as \"c0\","
            + " \"store\".\"store_state\" as \"c1\""
            + " from \"sales_fact_1997\" as \"sales_fact_1997\","
            + " \"store\" as \"store\" "
            + "where (\"store\".\"store_country\" = 'USA') "
            + "and (\"store\".\"store_state\" = 'CA') "
            + "and \"sales_fact_1997\".\"store_id\" = \"store\".\"store_id\" "
            + "group by \"store\".\"store_country\", \"store\".\"store_state\" "
            + "order by \"store\".\"store_country\" ASC NULLS LAST,"
            + " \"store\".\"store_state\" ASC NULLS LAST";
        SqlPattern pgPattern =
            new SqlPattern(POSTGRES, pgSql, pgSql.length());
        String mySql =
            "select `store`.`store_country` as `c0`,"
            + " `store`.`store_state` as `c1`"
            + " from `store` as `store`, `sales_fact_1997` as `sales_fact_1997` "
            + "where `sales_fact_1997`.`store_id` = `store`.`store_id` "
            + "and `store`.`store_country` = 'USA' "
            + "and `store`.`store_state` = 'CA' "
            + "group by `store`.`store_country`, `store`.`store_state` "
            + "order by ISNULL(`store`.`store_country`) ASC,"
            + " `store`.`store_country` ASC,"
            + " ISNULL(`store`.`store_state`) ASC, `store`.`store_state` ASC";
        SqlPattern myPattern = new SqlPattern(MYSQL, mySql, mySql.length());
        SqlPattern[] patterns = {pgPattern, myPattern};
        connection = ((TestContext)context).getConnection(new ConnectionProps(List.of("justCA")));
        executeQuery(mdx, connection);
        assertQuerySqlOrNot(connection, mdx, patterns, true, false, false);
    }

    /**
     * This is a test for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1869">MONDRIAN-1869</a>
     *
     * <p>Avg Aggregates need to be computed in SQL to get correct values.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAvgAggregator(Context<?> context) {
        Connection connection = context.getConnectionWithDefaultRole();
        prepareContext(connection);
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            null,
            " <Measure name=\"Avg Sales\" column=\"unit_sales\" aggregator=\"avg\"\n"
            + " formatString=\"#.###\"/>",
            null,
            null));
         */
        withSchemaEmf(context, SchemaModifiersEmf.SqlQueryTestModifier::new);
        String mdx = "select measures.[avg sales] on 0 from sales"
                       + " where { time.[1997].q1, time.[1997].q2.[4] }";
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{[Time].[Time].[1997].[Q1]}\n"
            + "{[Time].[Time].[1997].[Q2].[4]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Avg Sales]}\n"
            + "Row #0: 3.069\n");
        String sql =
            "select\n"
            + "    avg(`sales_fact_1997`.`unit_sales`) as `m0`\n"
            + "from\n"
            + "    `sales_fact_1997` as `sales_fact_1997`,\n"
            + "    `time_by_day` as `time_by_day`\n"
            + "where\n"
            + "    `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`\n"
            + "and\n"
            + "    ((`time_by_day`.`quarter` = 'Q1' and `time_by_day`.`the_year` = 1997) "
            + "or (`time_by_day`.`month_of_year` = 4 and `time_by_day`.`quarter` = 'Q2' "
            + "and `time_by_day`.`the_year` = 1997))";
        SqlPattern mySqlPattern =
            new SqlPattern(DatabaseProduct.MYSQL, sql, sql.length());
        assertQuerySql(context.getConnectionWithDefaultRole(), mdx, new SqlPattern[]{mySqlPattern});
    }

    private boolean isGroupingSetsSupported(Connection connection) {
        return connection.getContext().getConfigValue(ConfigConstants.ENABLE_GROUPING_SETS, ConfigConstants.ENABLE_GROUPING_SETS_DEFAULT_VALUE, Boolean.class)
                && getDialect(connection).supportsGroupingSets();
    }

    public class JdbcDialectImplForTest extends JdbcDialectImpl{

        public JdbcDialectImplForTest() {

        }

        @Override
        public String getDialectName() {
            return null;
        }
    }
}
