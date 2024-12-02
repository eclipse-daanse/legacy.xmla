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

package mondrian.rolap;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.getDialect;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestConfig;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.enums.DatabaseProduct;
import mondrian.olap.SystemWideProperties;
import mondrian.rolap.agg.CellRequest;
import mondrian.test.SqlPattern;

/**
 * Test support for generating SQL queries with the <code>GROUPING SETS</code>
 * construct, if the DBMS supports it.
 *
 * @author Thiyagu
 * @since 08-Jun-2007
 */
class GroupingSetQueryTest extends BatchTestCase{

    private SystemWideProperties prop = SystemWideProperties.instance();

    private static final String cubeNameSales2 = "Sales 2";
    private static final String measureStoreSales = "[Measures].[Store Sales]";
    private static final String fieldNameMaritalStatus = "marital_status";
    private static final String measureCustomerCount =
        "[Measures].[Customer Count]";

    private static final Set<DatabaseProduct> ORACLE_TERADATA =
    		EnumSet.of(
            DatabaseProduct.ORACLE,
            DatabaseProduct.TERADATA);

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

    private void pripareContext(Context context) {
        // This test warns of missing sql patterns for
        //
        // ACCESS
        // ORACLE
        final Dialect dialect = getDialect(context.getConnection());
        if (context.getConfig().warnIfNoPatternForDialect().equals("ANY")
                || getDatabaseProduct(dialect.getDialectName()) == DatabaseProduct.ACCESS
                || getDatabaseProduct(dialect.getDialectName()) == DatabaseProduct.ORACLE)
        {
            ((TestConfig)context.getConfig()).setWarnIfNoPatternForDialect(getDatabaseProduct(dialect.getDialectName()).toString());
        } else {
            // Do not warn unless the dialect is "ACCESS" or "ORACLE", or
            // if the test chooses to warn regardless of the dialect.
            ((TestConfig)context.getConfig()).setWarnIfNoPatternForDialect("NONE");
        }

    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGroupingSetsWithAggregateOverDefaultMember(Context context) {
        pripareContext(context);
        // testcase for MONDRIAN-705
        Connection connection = context.getConnection();
        if (getDialect(connection).supportsGroupingSets()) {
            ((TestConfig)context.getConfig()).setEnableGroupingSets(true);
        }
        assertQueryReturns(connection,
            "with member [Gender].[agg] as ' "
            + "  Aggregate({[Gender].DefaultMember}, [Measures].[Store Cost])' "
            + "select "
            + "  {[Measures].[Store Cost]} ON COLUMNS, "
            + "  {[Gender].[Gender].Members, [Gender].[agg]} ON ROWS "
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Store Cost]}\n"
            + "Axis #2:\n"
            + "{[Gender].[F]}\n"
            + "{[Gender].[M]}\n"
            + "{[Gender].[agg]}\n"
            + "Row #0: 111,777.48\n"
            + "Row #1: 113,849.75\n"
            + "Row #2: 225,627.23\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGroupingSetForSingleColumnConstraint(Context context) {
        pripareContext(context);
        ((TestConfig)context.getConfig()).setDisableCaching(false);
        Connection connection = context.getConnection();
        CellRequest request1 = createRequest(connection,
            cubeNameSales2, measureUnitSales, tableCustomer, fieldGender, "M");

        CellRequest request2 = createRequest(connection,
            cubeNameSales2, measureUnitSales, tableCustomer, fieldGender, "F");

        CellRequest request3 = createRequest(connection,
            cubeNameSales2, measureUnitSales, null, "", "");

        SqlPattern[] patternsWithGsets = {
            new SqlPattern(
                ORACLE_TERADATA,
                "select \"customer\".\"gender\" as \"c0\", sum(\"sales_fact_1997\".\"unit_sales\") as \"m0\", "
                + "grouping(\"customer\".\"gender\") as \"g0\" "
                + "from \"customer\" =as= \"customer\", \"sales_fact_1997\" =as= \"sales_fact_1997\" "
                + "where \"sales_fact_1997\".\"customer_id\" = \"customer\".\"customer_id\" "
                + "group by grouping sets ((\"customer\".\"gender\"), ())",
                26)
        };

        // If aggregates are enabled, mondrian should use them. Results should
        // be the same with or without grouping sets enabled.
        SqlPattern[] patternsWithAggs = {
            new SqlPattern(
                ORACLE_TERADATA,
                "select sum(\"agg_c_10_sales_fact_1997\".\"unit_sales\") as \"m0\""
                + " from \"agg_c_10_sales_fact_1997\" \"agg_c_10_sales_fact_1997\"",
                null),
            new SqlPattern(
                ORACLE_TERADATA,
                "select \"agg_g_ms_pcat_sales_fact_1997\".\"gender\" as \"c0\","
                + " sum(\"agg_g_ms_pcat_sales_fact_1997\".\"unit_sales\") as \"m0\" "
                + "from \"agg_g_ms_pcat_sales_fact_1997\" \"agg_g_ms_pcat_sales_fact_1997\" "
                + "group by \"agg_g_ms_pcat_sales_fact_1997\".\"gender\"",
                null)
        };

        SqlPattern[] patternsWithoutGsets = {
            new SqlPattern(
                DatabaseProduct.ACCESS,
                "select \"customer\".\"gender\" as \"c0\", sum(\"sales_fact_1997\".\"unit_sales\") as \"m0\" "
                + "from \"customer\" as \"customer\", \"sales_fact_1997\" as \"sales_fact_1997\" "
                + "where \"sales_fact_1997\".\"customer_id\" = \"customer\".\"customer_id\" "
                + "group by \"customer\".\"gender\"",
                26),
            new SqlPattern(
                ORACLE_TERADATA,
                "select \"customer\".\"gender\" as \"c0\", sum(\"sales_fact_1997\".\"unit_sales\") as \"m0\" "
                + "from \"customer\" =as= \"customer\", \"sales_fact_1997\" =as= \"sales_fact_1997\" "
                + "where \"sales_fact_1997\".\"customer_id\" = \"customer\".\"customer_id\" "
                + "group by \"customer\".\"gender\"",
                26)
        };

        ((TestConfig)context.getConfig()).setEnableGroupingSets(true);
        connection = context.getConnection();

        if (context.getConfig().readAggregates() && context.getConfig().useAggregates()) {
            assertRequestSql(connection,
                new CellRequest[] {request3, request1, request2},
                patternsWithAggs);
        } else {
            assertRequestSql(connection,
                new CellRequest[] {request3, request1, request2},
                patternsWithGsets);
        }

        ((TestConfig)context.getConfig()).setEnableGroupingSets(false);

        if (context.getConfig().readAggregates() && context.getConfig().useAggregates()) {
            assertRequestSql(connection,
                new CellRequest[] {request3, request1, request2},
                patternsWithAggs);
        } else {
            assertRequestSql(connection,
                new CellRequest[] {request3, request1, request2},
                patternsWithoutGsets);
        }
    }
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNotUsingGroupingSetWhenGroupUsesDifferentAggregateTable(Context context) {
        pripareContext(context);
        if (!(context.getConfig().useAggregates()
              && context.getConfig().readAggregates()))
        {
            return;
        }

        Connection connection = context.getConnection();
        CellRequest request1 = createRequest(connection,
            cubeNameSales,
            measureUnitSales, tableCustomer, fieldGender, "M");

        CellRequest request2 = createRequest(connection,
            cubeNameSales,
            measureUnitSales, tableCustomer, fieldGender, "F");

        CellRequest request3 = createRequest(connection,
            cubeNameSales,
            measureUnitSales, null, "", "");

        ((TestConfig)context.getConfig()).setEnableGroupingSets(true);

        SqlPattern[] patternsWithoutGsets = {
            new SqlPattern(
                DatabaseProduct.ACCESS,
                "select \"agg_g_ms_pcat_sales_fact_1997\".\"gender\" as \"c0\", "
                + "sum(\"agg_g_ms_pcat_sales_fact_1997\".\"unit_sales\") as \"m0\" "
                + "from \"agg_g_ms_pcat_sales_fact_1997\" as \"agg_g_ms_pcat_sales_fact_1997\" "
                + "group by \"agg_g_ms_pcat_sales_fact_1997\".\"gender\"",
                26),
            new SqlPattern(
                ORACLE_TERADATA,
                "select \"agg_g_ms_pcat_sales_fact_1997\".\"gender\" as \"c0\", "
                + "sum(\"agg_g_ms_pcat_sales_fact_1997\".\"unit_sales\") as \"m0\" "
                + "from \"agg_g_ms_pcat_sales_fact_1997\" \"agg_g_ms_pcat_sales_fact_1997\" "
                + "group by \"agg_g_ms_pcat_sales_fact_1997\".\"gender\"",
                26)
        };
        assertRequestSql(context.getConnection(),
            new CellRequest[] {request3, request1, request2},
            patternsWithoutGsets);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNotUsingGroupingSet(Context context) {
        pripareContext(context);
        if (context.getConfig().readAggregates() && context.getConfig().useAggregates()) {
            return;
        }
        Connection connection = context.getConnection();
        ((TestConfig)context.getConfig()).setEnableGroupingSets(true);
        CellRequest request1 = createRequest(connection,
            cubeNameSales2,
            measureUnitSales, tableCustomer, fieldGender, "M");

        CellRequest request2 = createRequest(connection,
            cubeNameSales2,
            measureUnitSales, tableCustomer, fieldGender, "F");

        SqlPattern[] patternsWithGsets = {
            new SqlPattern(
                ORACLE_TERADATA,
                "select \"customer\".\"gender\" as \"c0\", sum(\"sales_fact_1997\".\"unit_sales\") as \"m0\" "
                + "from \"customer\" =as= \"customer\", \"sales_fact_1997\" =as= \"sales_fact_1997\" "
                + "where \"sales_fact_1997\".\"customer_id\" = \"customer\".\"customer_id\" "
                + "group by \"customer\".\"gender\"", 72)
            };
        assertRequestSql(connection,
            new CellRequest[] {request1, request2},
            patternsWithGsets);

        ((TestConfig)context.getConfig()).setEnableGroupingSets(false);

        SqlPattern[] patternsWithoutGsets = {
            new SqlPattern(
                DatabaseProduct.ACCESS,
                "select \"customer\".\"gender\" as \"c0\", sum(\"sales_fact_1997\".\"unit_sales\") as \"m0\" "
                + "from \"customer\" as \"customer\", \"sales_fact_1997\" as \"sales_fact_1997\" "
                + "where \"sales_fact_1997\".\"customer_id\" = \"customer\".\"customer_id\" "
                + "group by \"customer\".\"gender\"", 72),
            new SqlPattern(
                ORACLE_TERADATA,
                "select \"customer\".\"gender\" as \"c0\", sum(\"sales_fact_1997\".\"unit_sales\") as \"m0\" "
                + "from \"customer\" =as= \"customer\", \"sales_fact_1997\" =as= \"sales_fact_1997\" "
                + "where \"sales_fact_1997\".\"customer_id\" = \"customer\".\"customer_id\" "
                + "group by \"customer\".\"gender\"", 72)
        };
        assertRequestSql(connection,
            new CellRequest[] {request1, request2},
            patternsWithoutGsets);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGroupingSetForMultipleMeasureAndSingleConstraint(Context context) {
        pripareContext(context);
        if (context.getConfig().readAggregates() && context.getConfig().useAggregates()) {
            return;
        }
        ((TestConfig)context.getConfig()).setEnableGroupingSets(true);
        Connection connection = context.getConnection();
        CellRequest request1 = createRequest(connection,
            cubeNameSales2,
            measureUnitSales, tableCustomer, fieldGender, "M");
        CellRequest request2 = createRequest(connection,
            cubeNameSales2,
            measureUnitSales, tableCustomer, fieldGender, "F");
        CellRequest request3 = createRequest(connection,
            cubeNameSales2,
            measureUnitSales, null, "", "");
        CellRequest request4 = createRequest(connection,
            cubeNameSales2,
            measureStoreSales, tableCustomer, fieldGender, "M");
        CellRequest request5 = createRequest(connection,
            cubeNameSales2,
            measureStoreSales, tableCustomer, fieldGender, "F");
        CellRequest request6 = createRequest(connection,
            cubeNameSales2,
            measureStoreSales, null, "", "");

        SqlPattern[] patternsWithGsets = {
            new SqlPattern(
                ORACLE_TERADATA,
                "select \"customer\".\"gender\" as \"c0\", sum(\"sales_fact_1997\".\"unit_sales\") as \"m0\", "
                + "sum(\"sales_fact_1997\".\"store_sales\") as \"m1\", grouping(\"customer\".\"gender\") as \"g0\" "
                + "from \"customer\" =as= \"customer\", \"sales_fact_1997\" =as= \"sales_fact_1997\" "
                + "where \"sales_fact_1997\".\"customer_id\" = \"customer\".\"customer_id\" "
                + "group by grouping sets ((\"customer\".\"gender\"), ())",
                26)
        };
        assertRequestSql(connection,
            new CellRequest[] {
                request1, request2, request3, request4, request5, request6},
            patternsWithGsets);

        ((TestConfig)context.getConfig()).setEnableGroupingSets(false);

        SqlPattern[] patternsWithoutGsets = {
            new SqlPattern(
                DatabaseProduct.ACCESS,
                "select \"customer\".\"gender\" as \"c0\", sum(\"sales_fact_1997\".\"unit_sales\") as \"m0\", "
                + "sum(\"sales_fact_1997\".\"store_sales\") as \"m1\" "
                + "from \"customer\" as \"customer\", \"sales_fact_1997\" as \"sales_fact_1997\" "
                + "where \"sales_fact_1997\".\"customer_id\" = \"customer\".\"customer_id\" "
                + "group by \"customer\".\"gender\"", 26),
            new SqlPattern(
                ORACLE_TERADATA,
                "select \"customer\".\"gender\" as \"c0\", sum(\"sales_fact_1997\".\"unit_sales\") as \"m0\", "
                + "sum(\"sales_fact_1997\".\"store_sales\") as \"m1\" "
                + "from \"customer\" =as= \"customer\", \"sales_fact_1997\" =as= \"sales_fact_1997\" "
                + "where \"sales_fact_1997\".\"customer_id\" = \"customer\".\"customer_id\" "
                + "group by \"customer\".\"gender\"", 26)
        };
        assertRequestSql(connection,
            new CellRequest[] {
                request1, request2, request3, request4, request5, request6},
            patternsWithoutGsets);
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGroupingSetForASummaryCanBeGroupedWith2DetailBatch(Context context) {
        pripareContext(context);
        if (context.getConfig().readAggregates() && context.getConfig().useAggregates()) {
            return;
        }
        ((TestConfig)context.getConfig()).setEnableGroupingSets(true);
        Connection connection = context.getConnection();
        CellRequest request1 = createRequest(connection,
            cubeNameSales2,
            measureUnitSales, tableCustomer, fieldGender, "M");
        CellRequest request2 = createRequest(connection,
            cubeNameSales2,
            measureUnitSales, tableCustomer, fieldGender, "F");
        CellRequest request3 = createRequest(connection,
            cubeNameSales2,
            measureUnitSales, null, "", "");
        CellRequest request4 = createRequest(connection,
            cubeNameSales2,
            measureUnitSales, tableCustomer, fieldNameMaritalStatus, "M");
        CellRequest request5 = createRequest(connection,
            cubeNameSales2,
            measureUnitSales, tableCustomer, fieldNameMaritalStatus, "S");
        CellRequest request6 = createRequest(connection,
            cubeNameSales2,
            measureUnitSales, null, "", "");

        SqlPattern[] patternWithGsets = {
            new SqlPattern(
                ORACLE_TERADATA,
                "select \"customer\".\"gender\" as \"c0\", sum(\"sales_fact_1997\".\"unit_sales\") as \"m0\", "
                + "grouping(\"customer\".\"gender\") as \"g0\" "
                + "from \"customer\" =as= \"customer\", \"sales_fact_1997\" =as= \"sales_fact_1997\" "
                + "where \"sales_fact_1997\".\"customer_id\" = \"customer\".\"customer_id\" "
                + "group by grouping sets ((\"customer\".\"gender\"), ())",
                26),

            new SqlPattern(
                ORACLE_TERADATA,
                "select \"customer\".\"marital_status\" as \"c0\", sum(\"sales_fact_1997\".\"unit_sales\") as \"m0\" "
                + "from \"customer\" =as= \"customer\", \"sales_fact_1997\" =as= \"sales_fact_1997\" "
                + "where \"sales_fact_1997\".\"customer_id\" = \"customer\".\"customer_id\" "
                + "group by \"customer\".\"marital_status\"",
                26),
            };

        assertRequestSql(connection,
            new CellRequest[] {
                request1, request2, request3, request4, request5, request6},
            patternWithGsets);

        ((TestConfig)context.getConfig()).setEnableGroupingSets(false);

        SqlPattern[] patternWithoutGsets = {
            new SqlPattern(
                DatabaseProduct.ACCESS,
                "select sum(\"sales_fact_1997\".\"unit_sales\") as \"m0\" "
                + "from \"sales_fact_1997\" as \"sales_fact_1997\"",
                40),
            new SqlPattern(
                ORACLE_TERADATA,
                "select sum(\"sales_fact_1997\".\"unit_sales\") as \"m0\" "
                + "from \"sales_fact_1997\" =as= \"sales_fact_1997\"",
                40)
        };

        assertRequestSql(connection,
            new CellRequest[] {
                request1, request2, request3, request4, request5, request6},
            patternWithoutGsets);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGroupingSetForMultipleColumnConstraint(Context context) {
        pripareContext(context);
        if (context.getConfig().readAggregates() && context.getConfig().useAggregates()) {
            return;
        }
        ((TestConfig)context.getConfig()).setEnableGroupingSets(true);
        Connection connection = context.getConnection();
        CellRequest request1 = createRequest(connection,
            cubeNameSales2,
            measureUnitSales, new String[]{tableCustomer, tableTime},
            new String[]{fieldGender, fieldYear},
            new String[]{"M", "1997"});

        CellRequest request2 = createRequest(connection,
            cubeNameSales2,
            measureUnitSales, new String[]{tableCustomer, tableTime},
            new String[]{fieldGender, fieldYear},
            new String[]{"F", "1997"});

        CellRequest request3 = createRequest(connection,
            cubeNameSales2,
            measureUnitSales, tableTime, fieldYear, "1997");

        SqlPattern[] patternsWithGsets = {
            new SqlPattern(
                ORACLE_TERADATA,
                "select \"time_by_day\".\"the_year\" as \"c0\", \"customer\".\"gender\" as \"c1\", "
                + "sum(\"sales_fact_1997\".\"unit_sales\") as \"m0\", grouping(\"customer\".\"gender\") as \"g0\" "
                + "from \"time_by_day\" =as= \"time_by_day\", \"sales_fact_1997\" =as= \"sales_fact_1997\", \"customer\" =as= \"customer\" "
                + "where \"sales_fact_1997\".\"time_id\" = \"time_by_day\".\"time_id\" and \"time_by_day\".\"the_year\" = 1997 "
                + "and \"sales_fact_1997\".\"customer_id\" = \"customer\".\"customer_id\" "
                + "group by grouping sets ((\"time_by_day\".\"the_year\", \"customer\".\"gender\"), (\"time_by_day\".\"the_year\"))",
            150)
        };

        // Sometimes this query causes Oracle 10.2 XE to give
        //   ORA-12516, TNS:listener could not find available handler with
        //   matching protocol stack
        //
        // You need to configure Oracle:
        //  $ su - oracle
        //  $ sqlplus / as sysdba
        //  SQL> ALTER SYSTEM SET sessions=320 SCOPE=SPFILE;
        //  SQL> SHUTDOWN
        assertRequestSql(connection,
            new CellRequest[] {request3, request1, request2},
            patternsWithGsets);

        ((TestConfig)context.getConfig()).setEnableGroupingSets(false);

        SqlPattern[] patternsWithoutGsets = {
            new SqlPattern(
                DatabaseProduct.ACCESS,
                "select \"time_by_day\".\"the_year\" as \"c0\", \"customer\".\"gender\" as \"c1\", "
                + "sum(\"sales_fact_1997\".\"unit_sales\") as \"m0\" "
                + "from \"time_by_day\" as \"time_by_day\", \"sales_fact_1997\" as \"sales_fact_1997\", "
                + "\"customer\" as \"customer\" "
                + "where \"sales_fact_1997\".\"time_id\" = \"time_by_day\".\"time_id\" and "
                + "\"time_by_day\".\"the_year\" = 1997 and "
                + "\"sales_fact_1997\".\"customer_id\" = \"customer\".\"customer_id\" "
                + "group by \"time_by_day\".\"the_year\", \"customer\".\"gender\"",
                50),
            new SqlPattern(
                ORACLE_TERADATA,
                "select \"time_by_day\".\"the_year\" as \"c0\", \"customer\".\"gender\" as \"c1\", "
                + "sum(\"sales_fact_1997\".\"unit_sales\") as \"m0\" "
                + "from \"time_by_day\" =as= \"time_by_day\", \"sales_fact_1997\" =as= \"sales_fact_1997\", "
                + "\"customer\" =as= \"customer\" "
                + "where \"sales_fact_1997\".\"time_id\" = \"time_by_day\".\"time_id\" and "
                + "\"time_by_day\".\"the_year\" = 1997 "
                + "and \"sales_fact_1997\".\"customer_id\" = \"customer\".\"customer_id\" "
                + "group by \"time_by_day\".\"the_year\", \"customer\".\"gender\"",
                    50)
            };
        assertRequestSql(connection,
            new CellRequest[]{request3, request1, request2},
            patternsWithoutGsets);
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    public void
        testGroupingSetForMultipleColumnConstraintAndCompoundConstraint(Context context)
    {
        pripareContext(context);
        if (context.getConfig().readAggregates() && context.getConfig().useAggregates()) {
            return;
        }
        List<String[]> compoundMembers = new ArrayList<>();
        compoundMembers.add(new String[] {"USA", "OR"});
        compoundMembers.add(new String[] {"CANADA", "BC"});
        CellRequestConstraint constraint =
            makeConstraintCountryState(compoundMembers);
        Connection connection = context.getConnection();
        CellRequest request1 = createRequest(connection,
            cubeNameSales2,
            measureCustomerCount, new String[]{tableCustomer, tableTime},
            new String[]{fieldGender, fieldYear},
            new String[]{"M", "1997"}, constraint);

        CellRequest request2 = createRequest(connection,
            cubeNameSales2,
            measureCustomerCount, new String[]{tableCustomer, tableTime},
            new String[]{fieldGender, fieldYear},
            new String[]{"F", "1997"}, constraint);

        CellRequest request3 = createRequest(connection,
            cubeNameSales2,
            measureCustomerCount, tableTime, fieldYear, "1997", constraint);

        String sqlWithoutGS =
            "select \"time_by_day\".\"the_year\" as \"c0\", \"customer\".\"gender\" as \"c1\", "
            + "count(distinct \"sales_fact_1997\".\"customer_id\") as \"m0\" from \"time_by_day\" =as= \"time_by_day\", "
            + "\"sales_fact_1997\" =as= \"sales_fact_1997\", \"customer\" =as= \"customer\", \"store\" =as= \"store\" "
            + "where \"sales_fact_1997\".\"time_id\" = \"time_by_day\".\"time_id\" and \"time_by_day\".\"the_year\" = 1997 "
            + "and \"sales_fact_1997\".\"customer_id\" = \"customer\".\"customer_id\" and "
            + "\"sales_fact_1997\".\"store_id\" = \"store\".\"store_id\" and "
            + "((\"store\".\"store_country\" = 'USA' and \"store\".\"store_state\" = 'OR') or "
            + "(\"store\".\"store_country\" = 'CANADA' and \"store\".\"store_state\" = 'BC')) "
            + "group by \"time_by_day\".\"the_year\", \"customer\".\"gender\"";

        SqlPattern[] patternsGSDisabled = {
            new SqlPattern(ORACLE_TERADATA, sqlWithoutGS, sqlWithoutGS)
        };
        // as of change 12310 GS has been removed from distinct count queries,
        // since there is little or no performance benefit and there is a bug
        // related to it (2207515)
        SqlPattern[] patternsGSEnabled = patternsGSDisabled;

        ((TestConfig)context.getConfig()).setEnableGroupingSets(true);

        assertRequestSql(connection,
            new CellRequest[] {request3, request1, request2},
            patternsGSEnabled);

        ((TestConfig)context.getConfig()).setEnableGroupingSets(false);

        assertRequestSql(connection,
            new CellRequest[]{request3, request1, request2},
            patternsGSDisabled);
    }

    /**
     * Testcase for bug 2004202, "Except not working with grouping sets".
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBug2004202(Context context) {
        pripareContext(context);
        assertQueryReturns(context.getConnection(),
            "with member store.allbutwallawalla as\n"
            + " 'aggregate(\n"
            + "    except(\n"
            + "        store.[store name].members,\n"
            + "        { [Store].[All Stores].[USA].[WA].[Walla Walla].[Store 22]}))'\n"
            + "select {\n"
            + "          store.[store name].members,\n"
            + "         store.allbutwallawalla,\n"
            + "         store.[all stores]} on 0,\n"
            + "  {measures.[customer count]} on 1\n"
            + "from sales",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[Canada].[BC].[Vancouver].[Store 19]}\n"
            + "{[Store].[Canada].[BC].[Victoria].[Store 20]}\n"
            + "{[Store].[Mexico].[DF].[Mexico City].[Store 9]}\n"
            + "{[Store].[Mexico].[DF].[San Andres].[Store 21]}\n"
            + "{[Store].[Mexico].[Guerrero].[Acapulco].[Store 1]}\n"
            + "{[Store].[Mexico].[Jalisco].[Guadalajara].[Store 5]}\n"
            + "{[Store].[Mexico].[Veracruz].[Orizaba].[Store 10]}\n"
            + "{[Store].[Mexico].[Yucatan].[Merida].[Store 8]}\n"
            + "{[Store].[Mexico].[Zacatecas].[Camacho].[Store 4]}\n"
            + "{[Store].[Mexico].[Zacatecas].[Hidalgo].[Store 12]}\n"
            + "{[Store].[Mexico].[Zacatecas].[Hidalgo].[Store 18]}\n"
            + "{[Store].[USA].[CA].[Alameda].[HQ]}\n"
            + "{[Store].[USA].[CA].[Beverly Hills].[Store 6]}\n"
            + "{[Store].[USA].[CA].[Los Angeles].[Store 7]}\n"
            + "{[Store].[USA].[CA].[San Diego].[Store 24]}\n"
            + "{[Store].[USA].[CA].[San Francisco].[Store 14]}\n"
            + "{[Store].[USA].[OR].[Portland].[Store 11]}\n"
            + "{[Store].[USA].[OR].[Salem].[Store 13]}\n"
            + "{[Store].[USA].[WA].[Bellingham].[Store 2]}\n"
            + "{[Store].[USA].[WA].[Bremerton].[Store 3]}\n"
            + "{[Store].[USA].[WA].[Seattle].[Store 15]}\n"
            + "{[Store].[USA].[WA].[Spokane].[Store 16]}\n"
            + "{[Store].[USA].[WA].[Tacoma].[Store 17]}\n"
            + "{[Store].[USA].[WA].[Walla Walla].[Store 22]}\n"
            + "{[Store].[USA].[WA].[Yakima].[Store 23]}\n"
            + "{[Store].[allbutwallawalla]}\n"
            + "{[Store].[All Stores]}\n"
            + "Axis #2:\n"
            + "{[Measures].[Customer Count]}\n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: 1,059\n"
            + "Row #0: 1,147\n"
            + "Row #0: 962\n"
            + "Row #0: 296\n"
            + "Row #0: 563\n"
            + "Row #0: 474\n"
            + "Row #0: 190\n"
            + "Row #0: 179\n"
            + "Row #0: 906\n"
            + "Row #0: 84\n"
            + "Row #0: 278\n"
            + "Row #0: 96\n"
            + "Row #0: 95\n"
            + "Row #0: 5,485\n"
            + "Row #0: 5,581\n");
    }
}
