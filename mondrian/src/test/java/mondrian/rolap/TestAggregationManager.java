/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2003-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara
// All Rights Reserved.
*/
package mondrian.rolap;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertQueryThrows;
import static org.opencube.junit5.TestUtil.flushSchemaCache;
import static org.opencube.junit5.TestUtil.getDialect;
import static org.opencube.junit5.TestUtil.withSchema;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Locus;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.core.AbstractBasicContext;
import org.eclipse.daanse.olap.rolap.api.RolapContext;
import org.eclipse.daanse.rdb.structure.pojo.ColumnImpl;
import org.eclipse.daanse.rdb.structure.pojo.PhysicalTableImpl;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.MeasureAggregatorType;
import org.eclipse.daanse.rolap.mapping.instance.rec.complex.foodmart.FoodmartMappingSupplier;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationColumnNameMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationExcludeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationLevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationNameMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.JoinQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.JoinedQueryElementMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SQLExpressionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SQLMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestConfig;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.enums.DatabaseProduct;
import mondrian.olap.SystemWideProperties;
import mondrian.olap.Util;
import mondrian.rolap.agg.AggregationManager;
import mondrian.rolap.agg.CellRequest;
import mondrian.rolap.agg.ValueColumnPredicate;
import mondrian.rolap.aggmatcher.AggStar;
import mondrian.server.ExecutionImpl;
import mondrian.server.LocusImpl;
import mondrian.test.SqlPattern;

/**
 * Unit test for {@link AggregationManager}.
 *
 * @author jhyde
 * @since 21 March, 2002
 */
class TestAggregationManager extends BatchTestCase {
    private static final Set<DatabaseProduct> ACCESS_MYSQL =
    		EnumSet.of(
            DatabaseProduct.ACCESS,
            DatabaseProduct.MYSQL);

    private Locus locus;
    private ExecutionImpl execution;
    private AggregationManager aggMgr;



    @BeforeEach
    void beforeEach() {

    }

    @AfterEach
    void afterEach() {
        SystemWideProperties.instance().populateInitial();
        LocusImpl.pop(locus);

        // allow gc
        locus = null;
        execution = null;
        aggMgr = null;
    }

    private void prepareContext(Context context) {
        final Statement statement =
            ((Connection) context.getConnectionWithDefaultRole())
                .getInternalStatement();
        execution = new ExecutionImpl(statement, Optional.empty());
        aggMgr =
            ((AbstractBasicContext)execution.getMondrianStatement()
                .getMondrianConnection()
                .getContext()).getAggregationManager();
        locus = new LocusImpl(execution, "TestAggregationManager", null);
        LocusImpl.push(locus);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFemaleUnitSales(Context context) {
        prepareContext(context);
        Connection connection = context.getConnectionWithDefaultRole();
        final FastBatchingCellReader fbcr =
            new FastBatchingCellReader(execution, getCube(connection,"Sales"), aggMgr);
        CellRequest request = createRequest(connection,
            "Sales", "[Measures].[Unit Sales]", "customer", "gender", "F");
        Object value = aggMgr.getCellFromCache(request);
        assertNull(value); // before load, the cell is not found
        fbcr.recordCellRequest(request);
        fbcr.loadAggregations();
        value = aggMgr.getCellFromCache(request); // after load, cell found
        assertTrue(value instanceof Number);
        assertEquals(131558, ((Number) value).intValue());
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void  testFemaleCustomerCount(Context context) {
        prepareContext(context);
        final FastBatchingCellReader fbcr =
            new FastBatchingCellReader(execution, getCube(context.getConnectionWithDefaultRole(), "Sales"), aggMgr);
        CellRequest request =
            createRequest(context.getConnectionWithDefaultRole(),
                "Sales", "[Measures].[Customer Count]",
                "customer", "gender", "F");
        Object value = aggMgr.getCellFromCache(request);
        assertNull(value); // before load, the cell is not found
        fbcr.recordCellRequest(request);
        fbcr.loadAggregations();
        value = aggMgr.getCellFromCache(request); // after load, cell found
        assertTrue(value instanceof Number);
        assertEquals(2755, ((Number) value).intValue());
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void  testFemaleCustomerCountWithConstraints(Context context) {
        prepareContext(context);
        List<String[]> Q1M1 = new ArrayList<> ();
        Q1M1.add(new String[] {"1997", "Q1", "1"});

        List<String[]> Q2M5 = new ArrayList<> ();
        Q2M5.add(new String[] {"1997", "Q2", "5"});

        List<String[]> Q1M1Q2M5 = new ArrayList<> ();
        Q1M1Q2M5.add(new String[] {"1997", "Q1", "1"});
        Q1M1Q2M5.add(new String[] {"1997", "Q2", "5"});
        Connection connection = context.getConnectionWithDefaultRole();
        CellRequest request1 =
            createRequest(connection,
                "Sales", "[Measures].[Customer Count]",
                "customer", "gender", "F",
                makeConstraintYearQuarterMonth(Q1M1));

        CellRequest request2 =
            createRequest(connection,
                "Sales", "[Measures].[Customer Count]",
                "customer", "gender", "F",
                makeConstraintYearQuarterMonth(Q2M5));

        CellRequest request3 =
            createRequest(connection,
                "Sales", "[Measures].[Customer Count]",
                "customer", "gender", "F",
                makeConstraintYearQuarterMonth(Q1M1Q2M5));

        FastBatchingCellReader fbcr =
            new FastBatchingCellReader(execution, getCube(connection, "Sales"), aggMgr);

        Object value = aggMgr.getCellFromCache(request1);
        assertNull(value); // before load, the cell is not found

        fbcr.recordCellRequest(request1);
        fbcr.recordCellRequest(request2);
        fbcr.recordCellRequest(request3);
        fbcr.loadAggregations();

        value = aggMgr.getCellFromCache(request1); // after load, found
        assertTrue(value instanceof Number);
        assertEquals(694, ((Number) value).intValue());

        value = aggMgr.getCellFromCache(request2); // after load, found
        assertTrue(value instanceof Number);
        assertEquals(672, ((Number) value).intValue());

        value = aggMgr.getCellFromCache(request3); // after load, found
        assertTrue(value instanceof Number);
        assertEquals(1122, ((Number) value).intValue());
        // Note: 1122 != (694 + 672)
    }

    /**
     * Tests that a request for ([Measures].[Unit Sales], [Gender].[F])
     * generates the correct SQL.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFemaleUnitSalesSql(Context context) {
        prepareContext(context);
        if (!(context.getConfig().useAggregates()
              && context.getConfig().readAggregates()))
        {
            return;
        }
        Connection connection = context.getConnectionWithDefaultRole();
        CellRequest request = createRequest(connection,
            "Sales", "[Measures].[Unit Sales]", "customer", "gender", "F");

        SqlPattern[] patterns = {
            new SqlPattern(
                ACCESS_MYSQL,
                "select `agg_g_ms_pcat_sales_fact_1997`.`gender` as `c0`,"
                + " sum(`agg_g_ms_pcat_sales_fact_1997`.`unit_sales`) as `m0` "
                + "from `agg_g_ms_pcat_sales_fact_1997` as `agg_g_ms_pcat_sales_fact_1997` "
                + "where `agg_g_ms_pcat_sales_fact_1997`.`gender` = 'F' "
                + "group by `agg_g_ms_pcat_sales_fact_1997`.`gender`",
                26)
        };

        assertRequestSql(connection, new CellRequest[]{request}, patterns);
    }

    /**
     * As {@link #testFemaleUnitSalesSql()}, but with aggregate tables switched
     * on.
     *
     * TODO: Enable this test.
     */
    private void _testFemaleUnitSalesSql_withAggs(Context context) {
        prepareContext(context);
        Connection connection = context.getConnectionWithDefaultRole();
        CellRequest request = createRequest(connection,
            "Sales", "[Measures].[Unit Sales]", "customer", "gender", "F");

        SqlPattern[] patterns = {
            new SqlPattern(
                ACCESS_MYSQL,
                "select `customer`.`gender` as `c0`,"
                + " sum(`agg_l_03_sales_fact_1997`.`unit_sales`) as `m0` "
                + "from `customer` as `customer`,"
                + " `agg_l_03_sales_fact_1997` as `agg_l_03_sales_fact_1997` "
                + "where `agg_l_03_sales_fact_1997`.`customer_id` = `customer`.`customer_id` "
                + "and `customer`.`gender` = 'F' "
                + "group by `customer`.`gender`",
                26)
        };

        assertRequestSql(connection, new CellRequest[]{request}, patterns);
    }

    /**
     * Test a batch containing multiple measures:
     *   (store_state=CA, gender=F, measure=[Unit Sales])
     *   (store_state=CA, gender=M, measure=[Store Sales])
     *   (store_state=OR, gender=M, measure=[Unit Sales])
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMultipleMeasures(Context context) {
        prepareContext(context);
        if (!(context.getConfig().useAggregates()
              && context.getConfig().readAggregates()))
        {
            return;
        }
        Connection connection = context.getConnectionWithDefaultRole();
        CellRequest[] requests = new CellRequest[] {
            createRequest(connection,
                "Sales",
                "[Measures].[Unit Sales]",
                new String[] {"customer", "store"},
                new String[] {"gender", "store_state"},
                new String[] {"F", "CA"}),
            createRequest(
                    connection,
                "Sales", "[Measures].[Store Sales]",
                new String[] {"customer", "store"},
                new String[] {"gender", "store_state"},
                new String[] {"M", "CA"}),
            createRequest(connection,
                "Sales", "[Measures].[Unit Sales]",
                new String[] {"customer", "store"},
                new String[] {"gender", "store_state"},
                new String[] {"F", "OR"})};

        SqlPattern[] patterns = {
            new SqlPattern(
                ACCESS_MYSQL,
                "select `store`.`store_state` as `c0`,"
                + " `customer`.`gender` as `c1`,"
                + " sum(`agg_l_05_sales_fact_1997`.`unit_sales`) as `m0`,"
                + " sum(`agg_l_05_sales_fact_1997`.`store_sales`) as `m1` "
                + "from `store` as `store`,"
                + " `agg_l_05_sales_fact_1997` as `agg_l_05_sales_fact_1997`,"
                + " `customer` as `customer` "
                + "where `agg_l_05_sales_fact_1997`.`store_id` = `store`.`store_id` "
                + "and `store`.`store_state` in ('CA', 'OR') "
                + "and `agg_l_05_sales_fact_1997`.`customer_id` = `customer`.`customer_id` "
                + "group by `store`.`store_state`, "
                + "`customer`.`gender`",
                29)
        };

        assertRequestSql(connection, requests, patterns);
    }

    /**
     * As {@link #testMultipleMeasures()}, but with aggregate tables switched
     * on.
     *
     * TODO: Enable this test.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    @SuppressWarnings("java:S5810")
    private void _testMultipleMeasures_withAgg(Context context) {
        prepareContext(context);
        Connection connection = context.getConnectionWithDefaultRole();
        CellRequest[] requests = new CellRequest[] {
            createRequest(connection,
                "Sales",
                "[Measures].[Unit Sales]",
                new String[] {"customer", "store"},
                new String[] {"gender", "store_state"},
                new String[] {"F", "CA"}),
            createRequest(connection,
                "Sales",
                "[Measures].[Store Sales]",
                new String[] {"customer", "store"},
                new String[] {"gender", "store_state"},
                new String[] {"M", "CA"}),
            createRequest(connection,
                "Sales",
                "[Measures].[Unit Sales]",
                new String[] {"customer", "store"},
                new String[] {"gender", "store_state"},
                new String[] {"F", "OR"})};

        SqlPattern[] patterns = {
            new SqlPattern(
                ACCESS_MYSQL,
                "select `customer`.`gender` as `c0`,"
                + " `store`.`store_state` as `c1`,"
                + " sum(`agg_l_05_sales_fact_1997`.`unit_sales`) as `m0`,"
                + " sum(`agg_l_05_sales_fact_1997`.`store_sales`) as `m1` "
                + "from `customer` as `customer`,"
                + " `agg_l_05_sales_fact_1997` as `agg_l_05_sales_fact_1997`,"
                + " `store` as `store` "
                + "where `agg_l_05_sales_fact_1997`.`customer_id` = `customer`.`customer_id`"
                + " and `agg_l_05_sales_fact_1997`.`store_id` = `store`.`store_id`"
                + " and `store`.`store_state` in ('CA', 'OR') "
                + "group by `customer`.`gender`, `store`.`store_state`",
                26)
        };

        assertRequestSql(connection, requests, patterns);
    }

    /**
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void createMultipleMeasureCellRequest(Context context) {
        prepareContext(context);
        String cube = "Sales";
        String measure = "[Measures].[Unit Sales]";
        String table = "store";
        String column = "store_state";
        String value = "CA";
        Connection connection = context.getConnectionWithDefaultRole();
        final boolean fail = true;
        Cube salesCube = connection.getCatalog().lookupCube(cube, fail);
        Member storeSqftMeasure =
            salesCube.getCatalogReader(null).getMemberByUniqueName(
                Util.parseIdentifier(measure), fail);
        RolapStar.Measure starMeasure =
            RolapStar.getStarMeasure(storeSqftMeasure);
        CellRequest request = new CellRequest(starMeasure, false, false);
        final RolapStar star = starMeasure.getStar();
        final RolapStar.Column storeTypeColumn =
            star.lookupColumn(table, column);
        request.addConstrainedColumn(
            storeTypeColumn,
            new ValueColumnPredicate(storeTypeColumn, value));
        assertTrue(true);
    }

    // todo: test unrestricted column, (Unit Sales, Gender=*)

    // todo: test one unrestricted, one restricted, (UNit Sales, Gender=*,
    //  State={CA, OR})

    // todo: test with 2 dimension columns on the same table, e.g.
    //  (Unit Sales, Gender={F}, MaritalStatus={S}) and make sure that the
    // table only appears once in the from clause.

    /**
     * Tests that if a level is marked 'unique members', then its parent
     * is not constrained.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUniqueMembers(Context context) {
    	context.getCatalogCache().clear();
        prepareContext(context);
        // [Store].[Store State] is unique, so we don't expect to see any
        // references to country.
        final String mdxQuery =
            "select {[Measures].[Unit Sales]} on columns,"
            + " {[Store].[USA].[CA], [Store].[USA].[OR]} on rows "
            + "from [Sales]";
        SqlPattern[] patterns;
        String accessMysqlSql, derbySql;

        // Note: the following aggregate loading sqls contain no
        // references to the parent level column "store_country".
        if (context.getConfig().useAggregates()
            && context.getConfig().readAggregates())
        {
            accessMysqlSql =
                "select `store`.`store_state` as `c0`,"
                + " `agg_c_14_sales_fact_1997`.`the_year` as `c1`,"
                + " sum(`agg_c_14_sales_fact_1997`.`unit_sales`) as `m0` "
                + "from `store` as `store`,"
                + " `agg_c_14_sales_fact_1997` as `agg_c_14_sales_fact_1997` "
                + "where `agg_c_14_sales_fact_1997`.`store_id` = `store`.`store_id`"
                + " and `store`.`store_state` in ('CA', 'OR')"
                + " and `agg_c_14_sales_fact_1997`.`the_year` = 1997 "
                + "group by `store`.`store_state`,"
                + " `agg_c_14_sales_fact_1997`.`the_year`";

            derbySql =
                "select "
                + "\"store\".\"store_state\" as \"c0\", \"agg_c_14_sales_fact_1997\".\"the_year\" as \"c1\", "
                + "sum(\"agg_c_14_sales_fact_1997\".\"unit_sales\") as \"m0\" "
                + "from "
                + "\"store\" as \"store\", \"agg_c_14_sales_fact_1997\" as \"agg_c_14_sales_fact_1997\" "
                + "where "
                + "\"agg_c_14_sales_fact_1997\".\"store_id\" = \"store\".\"store_id\" and "
                + "\"store\".\"store_state\" in ('CA', 'OR') and "
                + "\"agg_c_14_sales_fact_1997\".\"the_year\" = 1997 "
                + "group by "
                + "\"store\".\"store_state\", \"agg_c_14_sales_fact_1997\".\"the_year\"";

            patterns = new SqlPattern[] {
                new SqlPattern(
                    ACCESS_MYSQL,
                    accessMysqlSql, 50),
                new SqlPattern(
                    DatabaseProduct.DERBY, derbySql, derbySql)
            };
        } else {
            accessMysqlSql =
                "select `store`.`store_state` as `c0`,"
                + " `time_by_day`.`the_year` as `c1`,"
                + " sum(`sales_fact_1997`.`unit_sales`) as `m0` from `sales_fact_1997` as `sales_fact_1997`,"
                + " `store` as `store`,"
                + " `time_by_day` as `time_by_day` "
                + "where `sales_fact_1997`.`store_id` = `store`.`store_id`"
                + " and `store`.`store_state` in ('CA', 'OR')"
                + " and `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`"
                + " and `time_by_day`.`the_year` = 1997 "
                + "group by `store`.`store_state`, `time_by_day`.`the_year`";

            derbySql =
                "select \"store\".\"store_state\" as \"c0\", \"time_by_day\".\"the_year\" as \"c1\", "
                + "sum(\"sales_fact_1997\".\"unit_sales\") as \"m0\" "
                + "from "
                + "\"sales_fact_1997\" as \"sales_fact_1997\", \"store\" as \"store\", "
                + "\"time_by_day\" as \"time_by_day\" "
                + "where "
                + "\"sales_fact_1997\".\"store_id\" = \"store\".\"store_id\" and "
                + "\"store\".\"store_state\" in ('CA', 'OR') and "
                + "\"sales_fact_1997\".\"time_id\" = \"time_by_day\".\"time_id\" and "
                + "\"time_by_day\".\"the_year\" = 1997 "
                + "group by "
                + "\"store\".\"store_state\", \"time_by_day\".\"the_year\"";

            patterns = new SqlPattern[] {
                new SqlPattern(
                    ACCESS_MYSQL,
                    accessMysqlSql, 50),
                new SqlPattern(
                    DatabaseProduct.DERBY, derbySql, derbySql)
            };
        }
        assertQuerySql(context.getConnectionWithDefaultRole(), mdxQuery, patterns);
    }

    /**
     * Tests that a NonEmptyCrossJoin uses the measure referenced by the query
     * (Store Sales) instead of the default measure (Unit Sales) in the case
     * where the query only has one result axis.  The setup here is necessarily
     * elaborate because the original bug was quite arbitrary.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonEmptyCrossJoinLoneAxis(Context context) {
    	context.getCatalogCache().clear();
        prepareContext(context);
        // Not sure what this test is checking.
        // For now, only run it for derby.
        Connection connection = context.getConnectionWithDefaultRole();
        final Dialect dialect = getDialect(connection);
        if (getDatabaseProduct(dialect.getDialectName()) != DatabaseProduct.DERBY) {
            return;
        }
        String mdxQuery =
            "With "
            + "Set [*NATIVE_CJ_SET] as "
            + "'NonEmptyCrossJoin([*BASE_MEMBERS_Store],[*BASE_MEMBERS_Product])' "
            + "Set [*BASE_MEMBERS_Store] as '{[Store].[All Stores].[USA]}' "
            + "Set [*GENERATED_MEMBERS_Store] as "
            + "'Generate([*NATIVE_CJ_SET], {[Store].CurrentMember})' "
            + "Set [*BASE_MEMBERS_Product] as "
            + "'{[Product].[All Products].[Food],[Product].[All Products].[Drink]}' "
            + "Set [*GENERATED_MEMBERS_Product] as "
            + "'Generate([*NATIVE_CJ_SET], {[Product].CurrentMember})' "
            + "Member [Store].[*FILTER_MEMBER] as 'Aggregate ([*GENERATED_MEMBERS_Store])' "
            + "Member [Product].[*FILTER_MEMBER] as 'Aggregate ([*GENERATED_MEMBERS_Product])' "
            + "Select {[Measures].[Store Sales]} on columns "
            + "From [Sales] "
            + "Where ([Store].[*FILTER_MEMBER], [Product].[*FILTER_MEMBER])";

        String derbySql =
            "select "
            + "\"store\".\"store_country\" as \"c0\", "
            + "\"time_by_day\".\"the_year\" as \"c1\", "
            + "\"product_class\".\"product_family\" as \"c2\", "
            + "sum(\"sales_fact_1997\".\"unit_sales\") as \"m0\" "
            + "from "
            + "\"store\" as \"store\", "
            + "\"sales_fact_1997\" as \"sales_fact_1997\", "
            + "\"time_by_day\" as \"time_by_day\", "
            + "\"product_class\" as \"product_class\", "
            + "\"product\" as \"product\" "
            + "where "
            + "\"sales_fact_1997\".\"store_id\" = \"store\".\"store_id\" and "
            + "\"store\".\"store_country\" = 'USA' and "
            + "\"sales_fact_1997\".\"time_id\" = \"time_by_day\".\"time_id\" and "
            + "\"time_by_day\".\"the_year\" = 1997 and "
            + "\"sales_fact_1997\".\"product_id\" = \"product\".\"product_id\" and "
            + "\"product\".\"product_class_id\" = \"product_class\".\"product_class_id\" "
            + "group by "
            + "\"store\".\"store_country\", \"time_by_day\".\"the_year\", "
            + "\"product_class\".\"product_family\"";

        SqlPattern[] patterns = {
            new SqlPattern(DatabaseProduct.DERBY, derbySql, derbySql)};

        // For derby, the TestAggregationManager.testNonEmptyCrossJoinLoneAxis
        // test fails if the non-empty crossjoin optimizer is used.
        // With it on one gets a recursive call coming through the
        //  RolapEvaluator.getCachedResult.
        assertNoQuerySql(connection, mdxQuery, patterns);
    }

    /**
     * If a hierarchy lives in the fact table, we should not generate a join.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHierarchyInFactTable(Context context) {
    	context.getCatalogCache().clear();
        prepareContext(context);
        Connection connection = context.getConnectionWithDefaultRole();
        CellRequest request = createRequest(connection,
            "Store",
            "[Measures].[Store Sqft]",
            "store",
            "store_type",
            "Supermarket");

        String accessMysqlSql =
            "select `store`.`store_type` as `c0`,"
            + " sum(`store`.`store_sqft`) as `m0` "
            + "from `store` as `store` "
            + "where `store`.`store_type` = 'Supermarket' "
            + "group by `store`.`store_type`";

        String derbySql =
            "select "
            + "\"store\".\"store_type\" as \"c0\", "
            + "sum(\"store\".\"store_sqft\") as \"m0\" "
            + "from "
            + "\"store\" as \"store\" "
            + "where "
            + "\"store\".\"store_type\" = 'Supermarket' "
            + "group by \"store\".\"store_type\"";

        SqlPattern[] patterns = {
            new SqlPattern(
                ACCESS_MYSQL, accessMysqlSql, 26),
            new SqlPattern(DatabaseProduct.DERBY, derbySql, derbySql)
        };

        assertRequestSql(connection, new CellRequest[]{request}, patterns);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCountDistinctAggMiss(Context context) {
    	context.getCatalogCache().clear();
        prepareContext(context);
        Connection connection = context.getConnectionWithDefaultRole();
        CellRequest request = createRequest(connection,
            "Sales",
            "[Measures].[Customer Count]",
            new String[]{"time_by_day", "time_by_day"},
            new String[]{"the_year", "quarter"},
            new String[]{"1997", "Q1"});

        String accessSql =
            "select"
            + " `d0` as `c0`,"
            + " `d1` as `c1`,"
            + " count(`m0`) as `c2` "
            + "from ("
            + "select distinct `time_by_day`.`the_year` as `d0`, "
            + "`time_by_day`.`quarter` as `d1`, "
            + "`sales_fact_1997`.`customer_id` as `m0` "
            + "from "
            + "`sales_fact_1997` as `sales_fact_1997`, "
            + "`time_by_day` as `time_by_day` "
            + "where "
            + "`sales_fact_1997`.`time_id` = `time_by_day`.`time_id` and "
            + "`time_by_day`.`the_year` = 1997 and "
            + "`time_by_day`.`quarter` = 'Q1'"
            + ") as `dummyname` "
            + "group by `d0`, `d1`";

        String mysqlSql =
            "select"
            + " `time_by_day`.`the_year` as `c0`,"
            + " `time_by_day`.`quarter` as `c1`,"
            + " count(distinct `sales_fact_1997`.`customer_id`) as `m0` "
            + "from `sales_fact_1997` as `sales_fact_1997`,"
            + " `time_by_day` as `time_by_day` "
            + "where `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`"
            + " and `time_by_day`.`the_year` = 1997"
            + " and `time_by_day`.`quarter` = 'Q1' "
            + "group by `time_by_day`.`the_year`,"
            + " `time_by_day`.`quarter`";

        String derbySql =
            "select "
            + "\"time_by_day\".\"the_year\" as \"c0\", "
            + "\"time_by_day\".\"quarter\" as \"c1\", "
            + "count(distinct \"sales_fact_1997\".\"customer_id\") as \"m0\" "
            + "from "
            + "\"sales_fact_1997\" as \"sales_fact_1997\", "
            + "\"time_by_day\" as \"time_by_day\" "
            + "where "
            + "\"sales_fact_1997\".\"time_id\" = \"time_by_day\".\"time_id\" and "
            + "\"time_by_day\".\"the_year\" = 1997 and "
            + "\"time_by_day\".\"quarter\" = 'Q1' "
            + "group by \"time_by_day\".\"the_year\", \"time_by_day\".\"quarter\"";

        SqlPattern[] patterns = {
            new SqlPattern(DatabaseProduct.ACCESS, accessSql, 26),
            new SqlPattern(DatabaseProduct.MYSQL, mysqlSql, 26),
            new SqlPattern(DatabaseProduct.DERBY, derbySql, derbySql)
        };

        assertRequestSql(connection, new CellRequest[]{request}, patterns);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCountDistinctAggMatch(Context context) {
        prepareContext(context);
        if (!(context.getConfig().useAggregates()
              && context.getConfig().readAggregates()))
        {
            return;
        }
        Connection connection = context.getConnectionWithDefaultRole();
        CellRequest request = createRequest(connection,
            "Sales", "[Measures].[Customer Count]",
            new String[] { "time_by_day", "time_by_day", "time_by_day" },
            new String[] { "the_year", "quarter", "month_of_year" },
            new String[] { "1997", "Q1", "1" });

        String accessSql =
            "select "
            + "`agg_c_10_sales_fact_1997`.`the_year` as `c0`, "
            + "`agg_c_10_sales_fact_1997`.`quarter` as `c1`, "
            + "`agg_c_10_sales_fact_1997`.`month_of_year` as `c2`, "
            + "`agg_c_10_sales_fact_1997`.`customer_count` as `m0` "
            + "from "
            + "`agg_c_10_sales_fact_1997` as `agg_c_10_sales_fact_1997` "
            + "where "
            + "`agg_c_10_sales_fact_1997`.`the_year` = 1997 and "
            + "`agg_c_10_sales_fact_1997`.`quarter` = 'Q1' and "
            + "`agg_c_10_sales_fact_1997`.`month_of_year` = 1";

        SqlPattern[] patterns = {
            new SqlPattern(DatabaseProduct.ACCESS, accessSql, 26)};

        assertRequestSql(connection, new CellRequest[]{request}, patterns);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCountDistinctCannotRollup(Context context) {
    	context.getCatalogCache().clear();
        prepareContext(context);
        // Summary "agg_g_ms_pcat_sales_fact_1997" doesn't match,
        // because we'd need to roll-up the distinct-count measure over
        // "month_of_year".
        Connection connection = context.getConnectionWithDefaultRole();
        CellRequest request = createRequest(connection,
            "Sales", "[Measures].[Customer Count]",
            new String[] { "time_by_day", "time_by_day", "product_class" },
            new String[] { "the_year", "quarter", "product_family" },
            new String[] { "1997", "Q1", "Food" });

        SqlPattern[] patterns = {
            new SqlPattern(
                DatabaseProduct.MYSQL,
                "select"
                + " `time_by_day`.`the_year` as `c0`,"
                + " `time_by_day`.`quarter` as `c1`,"
                + " `product_class`.`product_family` as `c2`,"
                + " count(distinct `sales_fact_1997`.`customer_id`) as `m0` "
                + "from `sales_fact_1997` as `sales_fact_1997`,"
                + " `time_by_day` as `time_by_day`,"
                + " `product_class` as `product_class`,"
                + " `product` as `product` "
                + "where `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`"
                + " and `time_by_day`.`the_year` = 1997"
                + " and `time_by_day`.`quarter` = `Q1`"
                + " and `sales_fact_1997`.`product_id` = `product`.`product_id`"
                + " and `product`.`product_class_id` = `product_class`.`product_class_id`"
                + " and `product_class`.`product_family` = `Food` "
                + "group by `time_by_day`.`the_year`,"
                + " `time_by_day`.`quarter`,"
                + " `product_class`.`product_family`",
                23),
            new SqlPattern(
                DatabaseProduct.ACCESS,
                "select"
                + " `d0` as `c0`,"
                + " `d1` as `c1`,"
                + " `d2` as `c2`,"
                + " count(`m0`) as `c3` "
                + "from ("
                + "select distinct `time_by_day`.`the_year` as `d0`,"
                + " `time_by_day`.`quarter` as `d1`,"
                + " `product_class`.`product_family` as `d2`,"
                + " `sales_fact_1997`.`customer_id` as `m0` "
                + "from `sales_fact_1997` as `sales_fact_1997`,"
                + " `time_by_day` as `time_by_day`,"
                + " `product_class` as `product_class`,"
                + " `product` as `product` "
                + "where `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`"
                + " and `time_by_day`.`the_year` = 1997"
                + " and `time_by_day`.`quarter` = 'Q1'"
                + " and `sales_fact_1997`.`product_id` = `product`.`product_id`"
                + " and `product`.`product_class_id` = `product_class`.`product_class_id`"
                + " and `product_class`.`product_family` = 'Food') as `dummyname` "
                + "group by `d0`, `d1`, `d2`",
                23),
             new SqlPattern(
                 DatabaseProduct.DERBY,
                 "select "
                 + "\"time_by_day\".\"the_year\" as \"c0\", \"time_by_day\".\"quarter\" as \"c1\", "
                 + "\"product_class\".\"product_family\" as \"c2\", "
                 + "count(distinct \"sales_fact_1997\".\"customer_id\") as \"m0\" "
                 + "from "
                 + "\"sales_fact_1997\" as \"sales_fact_1997\", \"time_by_day\" as \"time_by_day\", "
                 + "\"product_class\" as \"product_class\", \"product\" as \"product\" "
                 + "where "
                 + "\"sales_fact_1997\".\"time_id\" = \"time_by_day\".\"time_id\" and "
                 + "\"time_by_day\".\"the_year\" = 1997 and "
                 + "\"time_by_day\".\"quarter\" = 'Q1' and "
                 + "\"sales_fact_1997\".\"product_id\" = \"product\".\"product_id\" and "
                 + "\"product\".\"product_class_id\" = \"product_class\".\"product_class_id\" and "
                 + "\"product_class\".\"product_family\" = 'Food' "
                 + "group by \"time_by_day\".\"the_year\", \"time_by_day\".\"quarter\", "
                 + "\"product_class\".\"product_family\"",
                 23)
        };

        assertRequestSql(connection, new CellRequest[]{request}, patterns);
    }

    /**
     * Now, here's a funny thing. Usually you can't roll up a distinct-count
     * aggregate. But if you're rolling up along the dimension which the
     * count is counting, it's OK. In this case, you know that every member
     * can only belong to one group.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCountDistinctRollupAlongDim(Context context) {
        prepareContext(context);
        if (!(context.getConfig().useAggregates()
              && context.getConfig().readAggregates()))
        {
            return;
        }
        // Request has granularity
        //  [Time].[Month]
        //  [Product].[Category]
        //
        // whereas agg table "agg_g_ms_pcat_sales_fact_1997" has
        // granularity
        //
        //  [Time].[Month]
        //  [Product].[Category]
        //  [Gender].[Gender]
        //  [Marital Status].[Marital Status]
        //
        // Because [Gender] and [Marital Status] come from the [Customer]
        // table (the same as the distinct-count measure), we can roll up.
        Connection connection = context.getConnectionWithDefaultRole();
        CellRequest request = createRequest(connection,
            "Sales", "[Measures].[Customer Count]",
            new String[] {
                "time_by_day", "time_by_day", "time_by_day",
                "product_class", "product_class", "product_class" },
            new String[] {
                "the_year", "quarter", "month_of_year",
                "product_family", "product_department", "product_category" },
            new String[] { "1997", "Q1", "1", "Food", "Deli", "Meat" });

        SqlPattern[] patterns = {
            new SqlPattern(
                ACCESS_MYSQL,
                "select `agg_g_ms_pcat_sales_fact_1997`.`the_year` as `c0`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`quarter` as `c1`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`month_of_year` as `c2`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`product_family` as `c3`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`product_department` as `c4`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`product_category` as `c5`,"
                + " sum(`agg_g_ms_pcat_sales_fact_1997`.`customer_count`) as `m0` "
                + "from `agg_g_ms_pcat_sales_fact_1997` as `agg_g_ms_pcat_sales_fact_1997` "
                + "where `agg_g_ms_pcat_sales_fact_1997`.`the_year` = 1997"
                + " and `agg_g_ms_pcat_sales_fact_1997`.`quarter` = 'Q1'"
                + " and `agg_g_ms_pcat_sales_fact_1997`.`month_of_year` = 1"
                + " and `agg_g_ms_pcat_sales_fact_1997`.`product_family` = 'Food'"
                + " and `agg_g_ms_pcat_sales_fact_1997`.`product_department` = 'Deli'"
                + " and `agg_g_ms_pcat_sales_fact_1997`.`product_category` = 'Meat' "
                + "group by `agg_g_ms_pcat_sales_fact_1997`.`the_year`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`quarter`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`month_of_year`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`product_family`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`product_department`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`product_category`",
                58)
        };

        assertRequestSql(connection, new CellRequest[]{request}, patterns);
    }

    /**
     * As above, but we rollup [Marital Status] but not [Gender].
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCountDistinctRollup2(Context context) {
        prepareContext(context);
        if (!(context.getConfig().useAggregates()
              && context.getConfig().readAggregates()))
        {
            return;
        }
        Connection connection = context.getConnectionWithDefaultRole();
        CellRequest request = createRequest(connection,
            "Sales", "[Measures].[Customer Count]",
            new String[] {
                "time_by_day", "time_by_day", "time_by_day",
                "product_class", "product_class", "product_class", "customer" },
            new String[] {
                "the_year", "quarter", "month_of_year", "product_family",
                "product_department", "product_category", "gender" },
            new String[] { "1997", "Q1", "1", "Food", "Deli", "Meat", "F" });

        SqlPattern[] patterns = {
            new SqlPattern(
                ACCESS_MYSQL,
                "select `agg_g_ms_pcat_sales_fact_1997`.`the_year` as `c0`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`quarter` as `c1`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`month_of_year` as `c2`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`product_family` as `c3`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`product_department` as `c4`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`product_category` as `c5`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`gender` as `c6`,"
                + " sum(`agg_g_ms_pcat_sales_fact_1997`.`customer_count`) as `m0` "
                + "from `agg_g_ms_pcat_sales_fact_1997` as `agg_g_ms_pcat_sales_fact_1997` "
                + "where `agg_g_ms_pcat_sales_fact_1997`.`the_year` = 1997"
                + " and `agg_g_ms_pcat_sales_fact_1997`.`quarter` = 'Q1'"
                + " and `agg_g_ms_pcat_sales_fact_1997`.`month_of_year` = 1"
                + " and `agg_g_ms_pcat_sales_fact_1997`.`product_family` = 'Food'"
                + " and `agg_g_ms_pcat_sales_fact_1997`.`product_department` = 'Deli'"
                + " and `agg_g_ms_pcat_sales_fact_1997`.`product_category` = 'Meat'"
                + " and `agg_g_ms_pcat_sales_fact_1997`.`gender` = 'F' "
                + "group by `agg_g_ms_pcat_sales_fact_1997`.`the_year`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`quarter`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`month_of_year`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`product_family`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`product_department`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`product_category`,"
                + " `agg_g_ms_pcat_sales_fact_1997`.`gender`",
                58)
        };

        assertRequestSql(connection, new CellRequest[]{request}, patterns);
    }

    /**
     * Test that cells with the same compound member constraints are
     * loaded in one Sql statement.
     *
     * Cells [Food] and [Drink] have the same constraint:
     *
     *  {[1997].[Q1].[1], [1997].[Q3].[7]}
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCountDistinctBatchLoading(Context context) {
    	context.getCatalogCache().clear();
        prepareContext(context);
        List<String[]> compoundMembers = new ArrayList<>();
        compoundMembers.add(new String[] {"1997", "Q1", "1"});
        compoundMembers.add(new String[] {"1997", "Q3", "7"});

        CellRequestConstraint aggConstraint =
            makeConstraintYearQuarterMonth(compoundMembers);
        Connection connection = context.getConnectionWithDefaultRole();
        CellRequest request1 = createRequest(connection,
            "Sales", "[Measures].[Customer Count]",
            new String[] {"product_class"},
            new String[] {"product_family"},
            new String[] {"Food"},
            aggConstraint);

        CellRequest request2 = createRequest(connection,
            "Sales", "[Measures].[Customer Count]",
            new String[] {"product_class"},
            new String[] {"product_family"},
            new String[] {"Drink"},
            aggConstraint);

        String mysqlSql =
            "select `product_class`.`product_family` as `c0`, "
            + "count(distinct `sales_fact_1997`.`customer_id`) as `m0` "
            + "from `sales_fact_1997` as `sales_fact_1997`, `product_class` as `product_class`, `product` as `product`, "
            + "`time_by_day` as `time_by_day` "
            + "where `sales_fact_1997`.`product_id` = `product`.`product_id` and "
            + "`product`.`product_class_id` = `product_class`.`product_class_id` and "
            + "`sales_fact_1997`.`time_id` = `time_by_day`.`time_id` and "
            + "(((`time_by_day`.`the_year`, `time_by_day`.`quarter`, `time_by_day`.`month_of_year`) "
            + "in ((1997, 'Q1', 1), (1997, 'Q3', 7)))) "
            + "group by `product_class`.`product_family`";

        String derbySql =
            "select \"product_class\".\"product_family\" as \"c0\", "
            + "count(distinct \"sales_fact_1997\".\"customer_id\") as \"m0\" "
            + "from \"sales_fact_1997\" as \"sales_fact_1997\", \"product_class\" as \"product_class\", \"product\" as \"product\", "
            + "\"time_by_day\" as \"time_by_day\" "
            + "where \"sales_fact_1997\".\"product_id\" = \"product\".\"product_id\" and "
            + "\"product\".\"product_class_id\" = \"product_class\".\"product_class_id\" and "
            + "\"sales_fact_1997\".\"time_id\" = \"time_by_day\".\"time_id\" and "
            + "((\"time_by_day\".\"the_year\" = 1997 and \"time_by_day\".\"quarter\" = 'Q1' and \"time_by_day\".\"month_of_year\" = 1) or "
            + "(\"time_by_day\".\"the_year\" = 1997 and \"time_by_day\".\"quarter\" = 'Q3' and \"time_by_day\".\"month_of_year\" = 7)) "
            + "group by \"product_class\".\"product_family\"";

        SqlPattern[] patterns = {
            new SqlPattern(DatabaseProduct.MYSQL, mysqlSql, mysqlSql),
            new SqlPattern(DatabaseProduct.DERBY, derbySql, derbySql)
        };

        assertRequestSql(connection, new CellRequest[]{request1, request2}, patterns);
    }

    /**
     * Tests that an aggregate table is used to speed up a
     * <code>&lt;Member&gt;.Children</code> expression.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAggMembers(Context context) {
        prepareContext(context);
        if (context.getConfig().testExpDependencies() > 0) {
            return;
        }
        if (!(context.getConfig().useAggregates()
                && context.getConfig().readAggregates()))
        {
            return;
        }
        if (!(context.getConfig().enableNativeCrossJoin())) {
            return;
        }
        SqlPattern[] patterns = {
            new SqlPattern(
                DatabaseProduct.ACCESS,
                "select `store`.`store_country` as `c0` "
                + "from `agg_c_14_sales_fact_1997` as `agg_c_14_sales_fact_1997`,"
                + " `store` as `store` "
                + "where `agg_c_14_sales_fact_1997`.`the_year` = 1998 "
                + "and `agg_c_14_sales_fact_1997`.`store_id` = `store`.`store_id` "
                + "group by `store`.`store_country` "
                + "order by Iif(`store`.`store_country` IS NULL, 1, 0),"
                + " `store`.`store_country` ASC",
                26),
            new SqlPattern(
                DatabaseProduct.MYSQL,
                "select `store`.`store_country` as `c0` "
                + "from `agg_c_14_sales_fact_1997` as `agg_c_14_sales_fact_1997`,"
                + " `store` as `store` "
                + "where `agg_c_14_sales_fact_1997`.`the_year` = 1998 "
                + "and `agg_c_14_sales_fact_1997`.`store_id` = `store`.`store_id` "
                + "group by `store`.`store_country` "
                + "order by ISNULL(`store`.`store_country`) ASC, `store`.`store_country` ASC",
                26)};
        Connection connection = context.getConnectionWithDefaultRole();
        assertQuerySql(connection,
            "select NON EMPTY {[Customers].[USA]} ON COLUMNS,\n"
            + "       NON EMPTY Crossjoin(Hierarchize(Union({[Store].[All Stores]},\n"
            + "           [Store].[All Stores].Children)), {[Product].[All Products]}) \n"
            + "           ON ROWS\n"
            + "    from [Sales]\n"
            + "    where ([Measures].[Unit Sales], [Time].[1998])",
            patterns);
    }

    /**
     * As {@link #testAggMembers()}, but asks for children of a leaf level.
     * Rewrite using an aggregate table is not possible, so just check that it
     * gets the right result.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAggChildMembersOfLeaf(Context context) {
    	context.getCatalogCache().clear();
        prepareContext(context);
        Connection connection = context.getConnectionWithDefaultRole();
        assertQueryReturns(connection,
            "select NON EMPTY {[Time].[1997]} ON COLUMNS,\n"
            + "       NON EMPTY Crossjoin(Hierarchize(Union({[Store].[All Stores]},\n"
            + "           [Store].[USA].[CA].[San Francisco].[Store 14].Children)), {[Product].[All Products]}) \n"
            + "           ON ROWS\n"
            + "    from [Sales]\n"
            + "    where [Measures].[Unit Sales]",
            "Axis #0:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #1:\n"
            + "{[Time].[1997]}\n"
            + "Axis #2:\n"
            + "{[Store].[All Stores], [Product].[All Products]}\n"
            + "Row #0: 266,773\n");
    }

    /**
     * This test case tests for a null pointer that was being thrown
     * inside of CellRequest.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNoNullPtrInCellRequest(Context context) {
        prepareContext(context);
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "<Dimension name=\"Store2\" foreignKey=\"store_id\">\n"
            + "  <Hierarchy hasAll=\"true\" primaryKey=\"store_id\" allMemberName=\"All Stores\">"
            + "    <Table name=\"store\"/>\n"
            + "    <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"/>\n"
            + "    <Level name=\"Store State\"   column=\"store_state\"   uniqueMembers=\"true\"/>\n"
            + "    <Level name=\"Store City\"    column=\"store_city\"    uniqueMembers=\"false\"/>\n"
            + "    <Level name=\"Store Type\"    column=\"store_type\"    uniqueMembers=\"false\"/>\n"
            + "    <Level name=\"Store Name\"    column=\"store_name\"    uniqueMembers=\"true\"/>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>"));
         */
        withSchema(context, SchemaModifiers.TestAggregationManagerModifier1::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures].[Unit Sales]} on columns, "
            + "Filter ({ "
            + "[Store2].[All Stores].[USA].[CA].[Beverly Hills], "
            + "[Store2].[All Stores].[USA].[CA].[Beverly Hills].[Gourmet Supermarket] "
            + "},[Measures].[Unit Sales] > 0) on rows "
            + "from [Sales] "
            + "where [Store Type].[Store Type].[Small Grocery]",
            "Axis #0:\n"
            + "{[Store Type].[Small Grocery]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n");
    }

    /**
     *  Test that once fetched, column cardinality can be shared between
     *  different queries using the same connection.
     *
     *  <p>Test also that expressions with only table alias difference do not
     *  share cardinality result.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testColumnCadinalityCache(Context context) {
        prepareContext(context);
        String query1 =
            "select "
            + "NonEmptyCrossJoin("
            + "[Product].[Product Family].Members, "
            + "[Gender].[Gender].Members) on columns "
            + "from [Sales]";

        String query2 =
            "select "
            + "NonEmptyCrossJoin("
            + "[Store].[Store Country].Members, "
            + "[Product].[Product Family].Members) on columns "
            + "from [Warehouse]";

        String cardinalitySqlDerby =
            "select "
            + "count(distinct \"product_class\".\"product_family\") "
            + "from \"product_class\" as \"product_class\"";

        String cardinalitySqlMySql =
            "select "
            + "count(distinct `product_class`.`product_family`) as `c0` "
            + "from `product_class` as `product_class`";

        SqlPattern[] patterns =
            new SqlPattern[] {
                new SqlPattern(
                    DatabaseProduct.DERBY,
                    cardinalitySqlDerby,
                    cardinalitySqlDerby),
                new SqlPattern(
                    DatabaseProduct.MYSQL,
                    cardinalitySqlMySql,
                    cardinalitySqlMySql)
            };

        //final TestContext context = getTestContext().withFreshConnection();
        Connection connection = context.getConnectionWithDefaultRole();
        try {
            // This MDX gets the [Product].[Product Family] cardinality
            // from the DB.
            executeQuery(query1, connection);

            // This MDX should be able to reuse the cardinality for
            // [Product].[Product Family]; and should not issue a SQL to fetch
            // that from DB again.
            assertQuerySqlOrNot(connection, query2, patterns, true, false, false);
        } finally {
            connection.close();
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testKeyExpressionCardinalityCache(Context context) {
        prepareContext(context);
        /*
        String storeDim1 =
            "<Dimension name=\"Store1\">\n"
            + "  <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">\n"
            + "  <Table name=\"store\"/>\n"
            + "    <Level name=\"Store Country\" uniqueMembers=\"true\">\n"
            + "      <KeyExpression>\n"
            + "        <SQL dialect=\"oracle\">\n"
            + "\"store_country\"\n"
            + "        </SQL>\n"
            + "        <SQL dialect=\"hsqldb\">\n"
            + "\"store_country\"\n"
            + "        </SQL>\n"
            + "        <SQL dialect=\"derby\">\n"
            + "\"store_country\"\n"
            + "        </SQL>\n"
            + "        <SQL dialect=\"luciddb\">\n"
            + "\"store_country\"\n"
            + "        </SQL>\n"
            + "        <SQL dialect=\"mysql\">\n"
            + "`store_country`\n"
            + "        </SQL>\n"
            + "        <SQL dialect=\"netezza\">\n"
            + "\"store_country\"\n"
            + "        </SQL>\n"
            + "        <SQL dialect=\"neoview\">\n"
            + "\"store_country\"\n"
            + "        </SQL>\n"
            + "        <SQL dialect=\"generic\">\n"
            + "store_country\n"
            + "        </SQL>\n"
            + "      </KeyExpression>\n"
            + "    </Level>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>\n";

        String storeDim2 =
            "<Dimension name=\"Store2\">\n"
            + "  <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">\n"
            + "  <Table name=\"store_ragged\"/>\n"
            + "    <Level name=\"Store Country\" uniqueMembers=\"true\">\n"
            + "      <KeyExpression>\n"
            + "        <SQL dialect=\"oracle\">\n"
            + "\"store_country\"\n"
            + "        </SQL>\n"
            + "        <SQL dialect=\"derby\">\n"
            + "\"store_country\"\n"
            + "        </SQL>\n"
            + "        <SQL dialect=\"luciddb\">\n"
            + "\"store_country\"\n"
            + "        </SQL>\n"
            + "        <SQL dialect=\"mysql\">\n"
            + "`store_country`\n"
            + "        </SQL>\n"
            + "        <SQL dialect=\"generic\">\n"
            + "store_country\n"
            + "        </SQL>\n"
            + "      </KeyExpression>\n"
            + "    </Level>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>\n";

        String salesCube1 =
            "<Cube name=\"Sales1\" defaultMeasure=\"Unit Sales\">\n"
            + "  <Table name=\"sales_fact_1997\" />\n"
            + "  <DimensionUsage name=\"Store1\" source=\"Store1\" foreignKey=\"store_id\"/>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" formatString=\"Standard\"/>\n"
            + "  <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\" formatString=\"Standard\"/>\n"
            + "</Cube>\n";

        String salesCube2 =
            "<Cube name=\"Sales2\" defaultMeasure=\"Unit Sales\">\n"
            + "  <Table name=\"sales_fact_1997\" />\n"
            + "  <DimensionUsage name=\"Store2\" source=\"Store2\" foreignKey=\"store_id\"/>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" formatString=\"Standard\"/>\n"
            + "</Cube>\n";
        */
        String query =
            "select {[Measures].[Unit Sales]} ON COLUMNS, {[Store1].members} ON ROWS FROM [Sales1]";

        String query1 =
            "select {[Measures].[Store Sales]} ON COLUMNS, {[Store1].members} ON ROWS FROM [Sales1]";

        String query2 =
            "select {[Measures].[Unit Sales]} ON COLUMNS, {[Store2].members} ON ROWS FROM [Sales2]";

        String cardinalitySqlDerby1 =
            "select count(distinct \"store_country\") from \"store\" as \"store\"";

        String cardinalitySqlMySql1 =
            "select count(distinct `store_country`) as `c0` from `store` as `store`";

        String cardinalitySqlDerby2 =
            "select count(*) from (select distinct \"store_country\" as \"c0\" from \"store_ragged\" as \"store_ragged\") as \"init\"";

        String cardinalitySqlMySql2 =
            "select count(*) from (select distinct `store_country` as `c0` from `store_ragged` as `store_ragged`) as `init`";

        SqlPattern[] patterns1 =
            new SqlPattern[] {
                new SqlPattern(
                    DatabaseProduct.DERBY,
                    cardinalitySqlDerby1,
                    cardinalitySqlDerby1),
                new SqlPattern(
                    DatabaseProduct.MYSQL,
                    cardinalitySqlMySql1,
                    cardinalitySqlMySql1)
            };

        SqlPattern[] patterns2 =
            new SqlPattern[] {
                new SqlPattern(
                    DatabaseProduct.DERBY,
                    cardinalitySqlDerby2,
                    cardinalitySqlDerby2),
                new SqlPattern(
                    DatabaseProduct.MYSQL,
                    cardinalitySqlMySql2,
                    cardinalitySqlMySql2)
            };
        class TestKeyExpressionCardinalityCacheModifier extends PojoMappingModifier {

            public TestKeyExpressionCardinalityCacheModifier(CatalogMapping catalog) {
                super(catalog);
            }

            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
            	StandardDimensionMappingImpl store1Dimension = StandardDimensionMappingImpl.builder()
                .withName("Store1")
                .withHierarchies(List.of(
                    HierarchyMappingImpl.builder()
                        .withHasAll(true)
                        .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                        .withLevels(List.of(
                            LevelMappingImpl.builder()
                                .withName("Store Country")
                                .withUniqueMembers(true)
                                .withKeyExpression(SQLExpressionMappingImpl.builder()
                                		.withSqls(List.of(
                                        SQLMappingImpl.builder()
                                            .withDialects(List.of("oracle"))
                                            .withStatement("\"store_country\"")
                                            .build(),
                                        SQLMappingImpl.builder()
                                            .withDialects(List.of("hsqldb"))
                                            .withStatement("\"store_country\"")
                                            .build(),
                                        SQLMappingImpl.builder()
                                            .withDialects(List.of("derby"))
                                            .withStatement("\"store_country\"")
                                            .build(),
                                        SQLMappingImpl.builder()
                                            .withDialects(List.of("luciddb"))
                                            .withStatement("\"store_country\"")
                                            .build(),
                                        SQLMappingImpl.builder()
                                            .withDialects(List.of("mysql"))
                                            .withStatement("`store_country`")
                                            .build(),
                                        SQLMappingImpl.builder()
                                            .withDialects(List.of("netezza"))
                                            .withStatement("\"store_country\"")
                                            .build(),
                                        SQLMappingImpl.builder()
                                            .withDialects(List.of("neoview"))
                                            .withStatement("\"store_country\"")
                                            .build(),
                                        SQLMappingImpl.builder()
                                            .withDialects(List.of("generic"))
                                            .withStatement("store_country")
                                            .build()
                                        )).build())
                                .build()
                        ))
                        .build()
                ))
                .build();
            	StandardDimensionMappingImpl store2Dimension = StandardDimensionMappingImpl.builder()
                .withName("Store2")
                .withHierarchies(List.of(
                    HierarchyMappingImpl.builder()
                        .withHasAll(true)
                        .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE_RAGGED)
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_RAGGED_TABLE).build())
                        .withLevels(List.of(
                            LevelMappingImpl.builder()
                                .withName("Store Country")
                                .withUniqueMembers(true)
                                .withKeyExpression(SQLExpressionMappingImpl.builder()
                                    .withSqls(List.of(
                                        SQLMappingImpl.builder()
                                            .withDialects(List.of("oracle"))
                                            .withStatement("\"store_country\"")
                                            .build(),
                                        SQLMappingImpl.builder()
                                            .withDialects(List.of("derby"))
                                            .withStatement("\"store_country\"")
                                            .build(),
                                        SQLMappingImpl.builder()
                                            .withDialects(List.of("luciddb"))
                                            .withStatement("\"store_country\"")
                                            .build(),
                                        SQLMappingImpl.builder()
                                            .withDialects(List.of("mysql"))
                                            .withStatement("`store_country`")
                                            .build(),
                                        SQLMappingImpl.builder()
                                            .withDialects(List.of("generic"))
                                            .withStatement("store_country")
                                            .build()
                                    )).build())
                                    .build()))
                                .build()
                        ))
                        .build();

            	MeasureMappingImpl sales1UnitSales = MeasureMappingImpl.builder()
                .withName("Unit Sales")
                .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                .withAggregatorType(MeasureAggregatorType.SUM)
                .withFormatString("Standard")
                .build();

            	MeasureMappingImpl sales2UnitSales = MeasureMappingImpl.builder()
            		.withName("Unit Sales")
                    .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                    .withAggregatorType(MeasureAggregatorType.SUM)
                    .withFormatString("Standard")
                    .build();


                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));

                result.add(PhysicalCubeMappingImpl.builder()
                    .withName("Sales1")
                    .withDefaultMeasure(sales1UnitSales)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Store1")
                    		.withDimension(store1Dimension)
                            .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                            .build()
                    ))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                    	sales1UnitSales,
                        MeasureMappingImpl.builder()
                        .withName("Store Sales")
                        .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_SALES_FACT_1997)
                        .withAggregatorType(MeasureAggregatorType.SUM)
                        .withFormatString("Standard")
                        .build()
                    )).build()))
                    .build());

                result.add(PhysicalCubeMappingImpl.builder()
                    .withName("Sales2")
                    .withDefaultMeasure(sales2UnitSales)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(
                        DimensionConnectorMappingImpl.builder()
                        	.withOverrideDimensionName("Store2")
                        	.withDimension(store2Dimension)
                            .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                            .build()
                    ))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                    		sales2UnitSales)).build()
                    ))
                    .build());
                return result;

            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
                storeDim1 + storeDim2,
                salesCube1 + salesCube2,
                null,
                null,
                null,
                null);
        withSchema(context, schema);
         */
        withSchema(context, TestKeyExpressionCardinalityCacheModifier::new);
        // This query causes "store"."store_country" cardinality to be
        // retrieved.
        Connection connection = context.getConnectionWithDefaultRole();
        executeQuery(query, connection);

        // Query1 will find the "store"."store_country" cardinality in cache.
        assertQuerySqlOrNot(connection, query1, patterns1, true, false, false);

        // Query2 again will not find the "store_ragged"."store_country"
        // cardinality in cache.
        assertQuerySqlOrNot(
            connection, query2, patterns2, false, false, false);
        context.getCatalogCache().clear();
    }

    /**
     * Test that using compound member constrant disables using AggregateTable
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCountDistinctWithConstraintAggMiss(Context context) {
        prepareContext(context);
        if (!(context.getConfig().useAggregates()
              && context.getConfig().readAggregates()))
        {
            return;
        }

        // Request has granularity
        //  [Product].[Category]
        // and the compound constraint on
        //  [Time].[Quarter]
        //
        // whereas agg table "agg_g_ms_pcat_sales_fact_1997" has
        // granularity
        //
        //  [Time].[Quarter]
        //  [Product].[Category]
        //  [Gender].[Gender]
        //  [Marital Status].[Marital Status]
        //
        // The presence of compound constraint causes agg table not used.
        //
        // Note ideally we should also test that non distinct measures could be
        // loaded from Aggregate table; however, the testing framework here uses
        // CellRequest directly which causes any compound constraint to be kept
        // separately. This will cause Aggregate tables not to be used.
        //
        // CellRequest generated by the code form MDX will in this case not
        // separate out the compound constraint from the "regular" constraints
        // and Aggregate tables can still be used.

        List<String[]> compoundMembers = new ArrayList<> ();
        compoundMembers.add(new String[] {"1997", "Q1", "1"});
        Connection connection = context.getConnectionWithDefaultRole();
        CellRequest request = createRequest(connection,
            "Sales", "[Measures].[Customer Count]",
            new String[] { "product_class", "product_class", "product_class" },
            new String[] {
                "product_family", "product_department", "product_category" },
            new String[] { "Food", "Deli", "Meat" },
            makeConstraintYearQuarterMonth(compoundMembers));

        SqlPattern[] patterns = {
            new SqlPattern(
                ACCESS_MYSQL,
                "select "
                + "`product_class`.`product_family` as `c0`, "
                + "`product_class`.`product_department` as `c1`, "
                + "`product_class`.`product_category` as `c2`, "
                + "count(distinct `sales_fact_1997`.`customer_id`) as `m0` "
                + "from "
                + "`product_class` as `product_class`, `product` as `product`, "
                + "`sales_fact_1997` as `sales_fact_1997`, `time_by_day` as `time_by_day` "
                + "where "
                + "`sales_fact_1997`.`product_id` = `product`.`product_id` and "
                + "`product`.`product_class_id` = `product_class`.`product_class_id` and "
                + "`product_class`.`product_family` = 'Food' and "
                + "`product_class`.`product_department` = 'Deli' and "
                + "`product_class`.`product_category` = 'Meat' and "
                + "`sales_fact_1997`.`time_id` = `time_by_day`.`time_id` and "
                + "(`time_by_day`.`the_year` = 1997 and `time_by_day`.`quarter` = 'Q1' and "
                + "`time_by_day`.`month_of_year` = 1) "
                + "group by "
                + "`product_class`.`product_family`, `product_class`.`product_department`, "
                + "`product_class`.`product_category`",
                58)
        };

        assertRequestSql(connection, new CellRequest[]{request}, patterns);
    }

    /**
     * Test case for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-663">bug MONDRIAN-663,
     * "Improve metadata query (TupleReader) support for aggregation tables to
     * include dimensions defining more than one column"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOrdinalExprAggTuplesAndChildren(Context context) {
        prepareContext(context);
        // this verifies that we can load properties, ordinals, etc out of
        // agg tables in member lookups (tuples and children)
        ((TestConfig)context.getConfig()).setGenerateFormattedSql(true);
        if (!(context.getConfig().useAggregates()
                && context.getConfig().readAggregates()))
        {
            return;
        }
        if (!(context.getConfig().enableNativeCrossJoin())) {
            return;
        }
        Connection connection = context.getConnectionWithDefaultRole();
        flushSchemaCache(connection);
        /*
        String cube = "<Cube name=\"Sales_Prod_Ord\">\n"
        + "  <Table name=\"sales_fact_1997\"/>\n"
        + "  <Dimension name=\"Product\" foreignKey=\"product_id\">\n"
        + "    <Hierarchy hasAll=\"true\" primaryKey=\"product_id\" primaryKeyTable=\"product\">\n"
        + "      <Join leftKey=\"product_class_id\" rightKey=\"product_class_id\">\n"
        + "        <Table name=\"product\"/>\n"
        + "        <Table name=\"product_class\"/>\n"
        + "      </Join>\n"
        + "      <Level name=\"Product Family\" table=\"product_class\" column=\"product_family\"\n"
        + "          uniqueMembers=\"true\"/>\n"
        + "      <Level name=\"Product Department\" table=\"product_class\" column=\"product_department\"\n"
        + "          uniqueMembers=\"false\"/>\n"
        + "      <Level name=\"Product Category\" table=\"product_class\" captionColumn=\"product_family\" column=\"product_category\"\n"
        + "          uniqueMembers=\"false\"/>\n"
        + "      <Level name=\"Product Subcategory\" table=\"product_class\" column=\"product_subcategory\"\n"
        + "          uniqueMembers=\"false\"/>\n"
        + "      <Level name=\"Brand Name\" table=\"product\" column=\"brand_name\" uniqueMembers=\"false\"/>\n"
        + "      <Level name=\"Product Name\" table=\"product\" column=\"product_name\"\n"
        + "          uniqueMembers=\"true\"/>\n"
        + "    </Hierarchy>\n"
        + "  </Dimension>\n"
        + "  <Dimension name=\"Gender\" foreignKey=\"customer_id\">\n"
        + "    <Hierarchy hasAll=\"false\" primaryKey=\"customer_id\">\n"
        + "    <Table name=\"customer\"/>\n"
        + "      <Level name=\"Gender\" column=\"gender\" uniqueMembers=\"true\"/>\n"
        + "    </Hierarchy>\n"
        + "  </Dimension>"
        + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
        + "      formatString=\"Standard\" visible=\"false\"/>\n"
        + "  <Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\"\n"
        + "      formatString=\"#,###.00\"/>\n"
        + "</Cube>";
        */
        class TestOrdinalExprAggTuplesAndChildrenModifier extends PojoMappingModifier {

            public TestOrdinalExprAggTuplesAndChildrenModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                result.add(PhysicalCubeMappingImpl.builder()
                    .withName("Sales_Prod_Ord")
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(
                        DimensionConnectorMappingImpl.builder()
                        	.withOverrideDimensionName("Product")
                            .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("Product")
                            	.withHierarchies(List.of(
                                HierarchyMappingImpl.builder()
                                    .withHasAll(true)
                                    .withPrimaryKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                                    .withPrimaryKeyTable(FoodmartMappingSupplier.PRODUCT_TABLE)
                                    .withQuery(JoinQueryMappingImpl.builder()
                                    		.withLeft(JoinedQueryElementMappingImpl.builder()
                                    				.withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT)
                                    				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_TABLE).build())
                                    				.build())
                                    		.withRight(JoinedQueryElementMappingImpl.builder()
                                    				.withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT_CLASS)
                                    				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE).build())
                                    				.build())
                                    		.build())
                                    .withLevels(List.of(
                                        LevelMappingImpl.builder()
                                            .withName("Product Family")
                                            .withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE)
                                            .withColumn(FoodmartMappingSupplier.PRODUCT_FAMILY_COLUMN_IN_PRODUCT_CLASS)
                                            .withUniqueMembers(true)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("Product Department")
                                            .withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE)
                                            .withColumn(FoodmartMappingSupplier.PRODUCT_DEPARTMENT_COLUMN_IN_PRODUCT_CLASS)
                                            .withUniqueMembers(false)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("Product Category")
                                            .withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE)
                                            .withCaptionColumn(FoodmartMappingSupplier.PRODUCT_FAMILY_COLUMN_IN_PRODUCT_CLASS)
                                            .withColumn(FoodmartMappingSupplier.PRODUCT_CATEGORY_COLUMN_IN_PRODUCT_CLASS)
                                            .withUniqueMembers(false)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("Product Subcategory")
                                            .withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE)
                                            .withColumn(FoodmartMappingSupplier.PRODUCT_SUBCATEGORY_COLUMN_IN_PRODUCT_CLASS)
                                            .withUniqueMembers(false)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("Brand Name")
                                            .withTable(FoodmartMappingSupplier.PRODUCT_TABLE)
                                            .withColumn(FoodmartMappingSupplier.BRAND_NAME_COLUMN_IN_PRODUCT)
                                            .withUniqueMembers(false)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("Product Name")
                                            .withTable(FoodmartMappingSupplier.PRODUCT_TABLE)
                                            .withColumn(FoodmartMappingSupplier.PRODUCT_NAME_COLUMN_IN_PRODUCT)
                                            .withUniqueMembers(true)
                                            .build()
                                        ))
                                    .build()
                            )).build())
                            .build(),
                        DimensionConnectorMappingImpl.builder()
                        	.withOverrideDimensionName("Gender")
                            .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("Gender")
                            	.withHierarchies(List.of(
                                HierarchyMappingImpl.builder()
                                    .withHasAll(false)
                                    .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                                    .withLevels(List.of(
                                        LevelMappingImpl.builder()
                                            .withName("Gender")
                                            .withColumn(FoodmartMappingSupplier.GENDER_COLUMN_IN_CUSTOMER)
                                            .withUniqueMembers(true)
                                            .build()
                                    ))
                                    .build()
                            )).build())
                            .build()
                    ))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                    		.withMeasures(List.of(
                                MeasureMappingImpl.builder()
                                    .withName("Unit Sales")
                                    .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                    .withAggregatorType(MeasureAggregatorType.SUM)
                                    .withFormatString("Standard")
                                    .withVisible(false)
                                    .build(),
                                MeasureMappingImpl.builder()
                                    .withName("Store Cost")
                                    .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_SALES_FACT_1997)
                                    .withAggregatorType(MeasureAggregatorType.SUM)
                                    .withFormatString("#,###.00")
                                    .build()
                    		))
                    		.build()))
                    .build());
                return result;
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
        withSchema(context, TestOrdinalExprAggTuplesAndChildrenModifier::new);
        String query =
            "select {[Measures].[Unit Sales]} on columns, "
            + "non empty CrossJoin({[Product].[Food].[Deli].[Meat]},{[Gender].[M]}) on rows "
            + "from [Sales_Prod_Ord] ";

        // first check that the sql is generated correctly

        SqlPattern[] patterns = {
            new SqlPattern(
                ACCESS_MYSQL,
                "select\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`product_family` as `c0`,\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`product_department` as `c1`,\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`product_category` as `c2`,\n"
                + "    `product_class`.`product_family` as `c3`,\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`gender` as `c4`\n"
                + "from\n"
                + "    `agg_g_ms_pcat_sales_fact_1997` as `agg_g_ms_pcat_sales_fact_1997`,\n"
                + "    `product_class` as `product_class`\n"
                + "where\n"
                + "    `product_class`.`product_category` = `agg_g_ms_pcat_sales_fact_1997`.`product_category`\n"
                + "and\n"
                + "    (`agg_g_ms_pcat_sales_fact_1997`.`product_category` = 'Meat' and `agg_g_ms_pcat_sales_fact_1997`.`product_department` = 'Deli' and `agg_g_ms_pcat_sales_fact_1997`.`product_family` = 'Food')\n"
                + "and\n"
                + "    (`agg_g_ms_pcat_sales_fact_1997`.`gender` = 'M')\n"
                + "group by\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`product_family`,\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`product_department`,\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`product_category`,\n"
                + "    `product_class`.`product_family`,\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`gender`\n"
                + "order by\n"
                + "    ISNULL(`agg_g_ms_pcat_sales_fact_1997`.`product_family`) ASC, `agg_g_ms_pcat_sales_fact_1997`.`product_family` ASC,\n"
                + "    ISNULL(`agg_g_ms_pcat_sales_fact_1997`.`product_department`) ASC, `agg_g_ms_pcat_sales_fact_1997`.`product_department` ASC,\n"
                + "    ISNULL(`agg_g_ms_pcat_sales_fact_1997`.`product_category`) ASC, `agg_g_ms_pcat_sales_fact_1997`.`product_category` ASC,\n"
                + "    ISNULL(`agg_g_ms_pcat_sales_fact_1997`.`gender`) ASC, `agg_g_ms_pcat_sales_fact_1997`.`gender` ASC",
                null)
        };

        assertQuerySqlOrNot(
            context.getConnectionWithDefaultRole(), query, patterns, false, false, false);

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            query,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Product].[Food].[Deli].[Meat], [Gender].[M]}\n"
            + "Row #0: 4,705\n");

        Result result = executeQuery(query, context.getConnectionWithDefaultRole());
        // this verifies that the caption for meat is Food
        assertEquals(
            "Meat",
            result.getAxes()[1].getPositions().get(0).get(0).getName());
        assertEquals(
            "Food",
            result.getAxes()[1].getPositions().get(0).get(0).getCaption());

        // Test children
        query =
            "select {[Measures].[Unit Sales]} on columns, "
            + "non empty [Product].[Food].[Deli].Children on rows "
            + "from [Sales_Prod_Ord] ";

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            query,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Product].[Food].[Deli].[Meat]}\n"
            + "{[Product].[Food].[Deli].[Side Dishes]}\n"
            + "Row #0: 4,728\n"
            + "Row #1: 1,262\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAggregatingTuples(Context context) {
        prepareContext(context);
        ((TestConfig)context.getConfig()).setLevelPreCacheThreshold(1);
        if (!(context.getConfig().useAggregates()
                && context.getConfig().readAggregates()))
        {
            return;
        }
        if (!(context.getConfig().enableNativeCrossJoin())) {
            return;
        }
        // flush cache, to be sure sql is executed
        Connection connection = context.getConnectionWithDefaultRole();
        flushSchemaCache(connection);

        // This first query verifies that simple collapsed levels in aggregate
        // tables load as tuples correctly.  The collapsed levels appear
        // in the aggregate table SQL below.

        // also note that at the time of this writing, this exercising the high
        // cardinality tuple reader

        String query =
            "select {[Measures].[Unit Sales]} on columns, "
            + "non empty CrossJoin({[Gender].[M]},{[Marital Status].[M]}) on rows "
            + "from [Sales] ";

        SqlPattern[] patterns = {
            new SqlPattern(
                ACCESS_MYSQL,
                "select "
                + "`agg_g_ms_pcat_sales_fact_1997`.`gender` as `c0`, "
                + "`agg_g_ms_pcat_sales_fact_1997`.`marital_status` as `c1` "
                + "from "
                + "`agg_g_ms_pcat_sales_fact_1997` as `agg_g_ms_pcat_sales_fact_1997` "
                + "where "
                + "(`agg_g_ms_pcat_sales_fact_1997`.`gender` = 'M') "
                + "and (`agg_g_ms_pcat_sales_fact_1997`.`marital_status` = 'M') "
                + "group by "
                + "`agg_g_ms_pcat_sales_fact_1997`.`gender`, "
                + "`agg_g_ms_pcat_sales_fact_1997`.`marital_status` "
                + "order by "
                + "ISNULL(`agg_g_ms_pcat_sales_fact_1997`.`gender`) ASC, "
                + "`agg_g_ms_pcat_sales_fact_1997`.`gender` ASC, "
                + "ISNULL(`agg_g_ms_pcat_sales_fact_1997`.`marital_status`) ASC, "
                + "`agg_g_ms_pcat_sales_fact_1997`.`marital_status` ASC",
                null)
        };

        assertQuerySqlOrNot(
            connection, query, patterns, false, false, false);

        assertQueryReturns(connection,
            query,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Gender].[M], [Marital Status].[M]}\n"
            + "Row #0: 66,460\n");

        // This second query verifies that joined levels on aggregate tables
        // load correctly.

        String query2 =
            "select {[Measures].[Unit Sales]} ON COLUMNS, "
            + "NON EMPTY {[Store].[Store State].Members} ON ROWS "
            + "from [Sales] where [Time].[1997].[Q1]";

        SqlPattern[] patterns2 = {
            new SqlPattern(
                ACCESS_MYSQL,
                "select "
                + "`store`.`store_country` as `c0`, "
                + "`store`.`store_state` as `c1` "
                + "from "
                + "`store` as `store`, "
                + "`agg_c_14_sales_fact_1997` as `agg_c_14_sales_fact_1997` "
                + "where "
                + "`agg_c_14_sales_fact_1997`.`store_id` = `store`.`store_id` and "
                + "`agg_c_14_sales_fact_1997`.`the_year` = 1997 and "
                + "`agg_c_14_sales_fact_1997`.`quarter` = 'Q1' "
                + "group by "
                + "`store`.`store_country`, `store`.`store_state` "
                + "order by "
                + "ISNULL(`store`.`store_country`) ASC, "
                + "`store`.`store_country` ASC, "
                + "ISNULL(`store`.`store_state`) ASC, "
                + "`store`.`store_state` ASC",
                null)
        };

        assertQuerySqlOrNot(
            connection, query2, patterns2, false, false, false);

        assertQueryReturns(connection,
            query2,
            "Axis #0:\n"
            + "{[Time].[1997].[Q1]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[USA].[CA]}\n"
            + "{[Store].[USA].[OR]}\n"
            + "{[Store].[USA].[WA]}\n"
            + "Row #0: 16,890\n"
            + "Row #1: 19,287\n"
            + "Row #2: 30,114\n");
    }

    /**
     * this test verifies the collapsed children code in SqlMemberSource
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCollapsedChildren(Context context) {
        prepareContext(context);
        if (!(context.getConfig().useAggregates()
                && context.getConfig().readAggregates()))
        {
            return;
        }
        if (!(context.getConfig().enableNativeCrossJoin())) {
            return;
        }
        Connection connection = context.getConnectionWithDefaultRole();
        // flush cache to be sure sql is executed
        flushSchemaCache(connection);

        SqlPattern[] patterns = {
            new SqlPattern(
                ACCESS_MYSQL,
                "select "
                + "`agg_g_ms_pcat_sales_fact_1997`.`gender` as `c0` "
                + "from `agg_g_ms_pcat_sales_fact_1997` "
                + "as `agg_g_ms_pcat_sales_fact_1997` "
                + "group by "
                + "`agg_g_ms_pcat_sales_fact_1997`.`gender`"
                + " order by ISNULL(`agg_g_ms_pcat_sales_fact_1997`.`gender`) ASC, `agg_g_ms_pcat_sales_fact_1997`.`gender` ASC",
                null)
        };

        String query =
            "select non empty [Gender].Children on columns\n"
            + "from [Sales]";

        assertQuerySqlOrNot(
            connection, query, patterns, false, false, false);

        assertQueryReturns(connection,
            query,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Gender].[F]}\n"
            + "{[Gender].[M]}\n"
            + "Row #0: 131,558\n"
            + "Row #0: 135,215\n");
    }

    /**
     * Test case for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-812">bug MONDRIAN-812,
     * "Issues with aggregate table recognition when using
     * &lt;KeyExpression&gt;&lt;SQL&gt; ... &lt;/SQL&gt;&lt;/KeyExpression&gt;
     * to define a level"</a>. Using a key expression for a level
     * element would make aggregate tables fail to be used.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLevelKeyAsSqlExpWithAgg(Context context) {
        prepareContext(context);
        Connection connection = context.getConnectionWithDefaultRole();
        final boolean p;
        switch (getDatabaseProduct(getDialect(connection).getDialectName())) {
        case POSTGRES:
            // Results are slightly different order on Postgres. It collates
            // "Sale Winners" before "Sales Days", because " " < "A".
            p = true;
            break;
        default:
            p = false;
            break;
        }
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        final String mdxQuery =
            "select non empty{[Promotions].[All Promotions].Children} ON rows, "
            + "non empty {[Store].[All Stores]} ON columns "
            + "from [Sales] "
            + "where {[Measures].[Unit Sales]}";
        // Provoke an error in the key resolution to prove it uses it.
        final StringBuilder colName =
            getDialect(connection)
                .quoteIdentifier("promotion_name");
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "<Dimension name=\"Promotions\" foreignKey=\"promotion_id\">\n"
            + "  <Hierarchy hasAll=\"true\" allMemberName=\"All Promotions\" primaryKey=\"promotion_id\" defaultMember=\"[All Promotions]\">\n"
            + "    <Table name=\"promotion\"/>\n"
            + "    <Level name=\"Promotion Name\" column=\"promotion_name\" uniqueMembers=\"true\">\n"
            + "      <KeyExpression><SQL>ERROR_TEST_FUNCTION_NAME("
            + colName + ")</SQL></KeyExpression>\n"
            + "    </Level>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>", false));
         */
        context.getCatalogCache().clear();
        CatalogMapping catalog = ((RolapContext) context).getCatalogMapping();
        ((TestContext)context).setCatalogMappingSupplier(new SchemaModifiers.TestAggregationManagerModifier2(catalog, colName));
        assertQueryThrows(context,
            mdxQuery,
            "ERROR_TEST_FUNCTION_NAME");
        // Run for real this time
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "<Dimension name=\"Promotions\" foreignKey=\"promotion_id\">\n"
            + "  <Hierarchy hasAll=\"true\" allMemberName=\"All Promotions\" primaryKey=\"promotion_id\" defaultMember=\"[All Promotions]\">\n"
            + "    <Table name=\"promotion\"/>\n"
            + "    <Level name=\"Promotion Name\" column=\"promotion_name\" uniqueMembers=\"true\">\n"
            + "      <KeyExpression><SQL>RTRIM("
            + colName + ")</SQL></KeyExpression>\n"
            + "    </Level>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>", false));
         */
        context.getCatalogCache().clear();
        catalog = ((RolapContext) context).getCatalogMapping();
        ((TestContext)context).setCatalogMappingSupplier(new SchemaModifiers.TestAggregationManagerModifier10(catalog, colName));
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select non empty{[Promotions].[All Promotions].Children} ON rows, "
            + "non empty {[Store].[All Stores]} ON columns "
            + "from [Sales] "
            + "where {[Measures].[Unit Sales]}",
            "Axis #0:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #1:\n"
            + "{[Store].[All Stores]}\n"
            + "Axis #2:\n"
            + "{[Promotions].[Bag Stuffers]}\n"
            + "{[Promotions].[Best Savings]}\n"
            + "{[Promotions].[Big Promo]}\n"
            + "{[Promotions].[Big Time Discounts]}\n"
            + "{[Promotions].[Big Time Savings]}\n"
            + "{[Promotions].[Bye Bye Baby]}\n"
            + "{[Promotions].[Cash Register Lottery]}\n"
            + "{[Promotions].[Dimes Off]}\n"
            + "{[Promotions].[Dollar Cutters]}\n"
            + "{[Promotions].[Dollar Days]}\n"
            + "{[Promotions].[Double Down Sale]}\n"
            + "{[Promotions].[Double Your Savings]}\n"
            + "{[Promotions].[Free For All]}\n"
            + "{[Promotions].[Go For It]}\n"
            + "{[Promotions].[Green Light Days]}\n"
            + "{[Promotions].[Green Light Special]}\n"
            + "{[Promotions].[High Roller Savings]}\n"
            + "{[Promotions].[I Cant Believe It Sale]}\n"
            + "{[Promotions].[Money Savers]}\n"
            + "{[Promotions].[Mystery Sale]}\n"
            + "{[Promotions].[No Promotion]}\n"
            + "{[Promotions].[One Day Sale]}\n"
            + "{[Promotions].[Pick Your Savings]}\n"
            + "{[Promotions].[Price Cutters]}\n"
            + "{[Promotions].[Price Destroyers]}\n"
            + "{[Promotions].[Price Savers]}\n"
            + "{[Promotions].[Price Slashers]}\n"
            + "{[Promotions].[Price Smashers]}\n"
            + "{[Promotions].[Price Winners]}\n"
            + (p ? "" : "{[Promotions].[Sale Winners]}\n")
            + "{[Promotions].[Sales Days]}\n"
            + "{[Promotions].[Sales Galore]}\n"
            + (!p ? "" : "{[Promotions].[Sale Winners]}\n")
            + "{[Promotions].[Save-It Sale]}\n"
            + "{[Promotions].[Saving Days]}\n"
            + "{[Promotions].[Savings Galore]}\n"
            + "{[Promotions].[Shelf Clearing Days]}\n"
            + "{[Promotions].[Shelf Emptiers]}\n"
            + "{[Promotions].[Super Duper Savers]}\n"
            + "{[Promotions].[Super Savers]}\n"
            + "{[Promotions].[Super Wallet Savers]}\n"
            + "{[Promotions].[Three for One]}\n"
            + "{[Promotions].[Tip Top Savings]}\n"
            + "{[Promotions].[Two Day Sale]}\n"
            + "{[Promotions].[Two for One]}\n"
            + "{[Promotions].[Unbeatable Price Savers]}\n"
            + "{[Promotions].[Wallet Savers]}\n"
            + "{[Promotions].[Weekend Markdown]}\n"
            + "{[Promotions].[You Save Days]}\n"
            + "Row #0: 901\n"
            + "Row #1: 2,081\n"
            + "Row #2: 1,789\n"
            + "Row #3: 932\n"
            + "Row #4: 700\n"
            + "Row #5: 921\n"
            + "Row #6: 4,792\n"
            + "Row #7: 1,219\n"
            + "Row #8: 781\n"
            + "Row #9: 1,652\n"
            + "Row #10: 1,959\n"
            + "Row #11: 843\n"
            + "Row #12: 1,638\n"
            + "Row #13: 689\n"
            + "Row #14: 1,607\n"
            + "Row #15: 436\n"
            + "Row #16: 2,654\n"
            + "Row #17: 253\n"
            + "Row #18: 899\n"
            + "Row #19: 1,021\n"
            + "Row #20: 195,448\n"
            + "Row #21: 1,973\n"
            + "Row #22: 323\n"
            + "Row #23: 1,624\n"
            + "Row #24: 2,173\n"
            + "Row #25: 4,094\n"
            + "Row #26: 1,148\n"
            + "Row #27: 504\n"
            + "Row #28: 1,294\n"
            + (p
                ? ("Row #29: 2,055\n"
                   + "Row #30: 2,572\n"
                   + "Row #31: 444\n")
                : ("Row #29: 444\n"
                   + "Row #30: 2,055\n"
                   + "Row #31: 2,572\n"))
            + "Row #32: 2,203\n"
            + "Row #33: 1,446\n"
            + "Row #34: 1,382\n"
            + "Row #35: 754\n"
            + "Row #36: 2,118\n"
            + "Row #37: 2,628\n"
            + "Row #38: 2,497\n"
            + "Row #39: 1,183\n"
            + "Row #40: 1,155\n"
            + "Row #41: 525\n"
            + "Row #42: 2,053\n"
            + "Row #43: 335\n"
            + "Row #44: 2,100\n"
            + "Row #45: 916\n"
            + "Row #46: 914\n"
            + "Row #47: 3,145\n");
    }

    /**
     * This is a test for MONDRIAN-918 and MONDRIAN-903. We have added
     * an attribute to AggName called approxRowCount so that the
     * aggregation manager can optimize the aggregation tables without
     * having to issue a select count() query.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAggNameApproxRowCount(Context context) {
        prepareContext(context);
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        /*
        withSchema(context,
                "<Schema name=\"FooSchema\"><Cube name=\"Sales_Foo\" defaultMeasure=\"Unit Sales\">\n"
                + "  <Table name=\"sales_fact_1997\">\n"
                + " <AggName name=\"agg_pl_01_sales_fact_1997\" approxRowCount=\"86000\">\n"
                + "     <AggFactCount column=\"FACT_COUNT\"/>\n"
                + "     <AggForeignKey factColumn=\"product_id\" aggColumn=\"PRODUCT_ID\" />\n"
                + "     <AggForeignKey factColumn=\"customer_id\" aggColumn=\"CUSTOMER_ID\" />\n"
                + "     <AggForeignKey factColumn=\"time_id\" aggColumn=\"TIME_ID\" />\n"
                + "     <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"UNIT_SALES_SUM\" />\n"
                + "     <AggMeasure name=\"[Measures].[Store Cost]\" column=\"STORE_COST_SUM\" />\n"
                + "     <AggMeasure name=\"[Measures].[Store Sales]\" column=\"STORE_SALES_SUM\" />\n"
                + " </AggName>\n"
                + "    <AggExclude name=\"agg_c_special_sales_fact_1997\" />\n"
                + "    <AggExclude name=\"agg_lc_100_sales_fact_1997\" />\n"
                + "    <AggExclude name=\"agg_lc_10_sales_fact_1997\" />\n"
                + "    <AggExclude name=\"agg_pc_10_sales_fact_1997\" />\n"
                + "  </Table>\n"
                + "<Dimension name=\"Time\" type=\"TimeDimension\" foreignKey=\"time_id\">\n"
                + "    <Hierarchy hasAll=\"true\" name=\"Weekly\" primaryKey=\"time_id\">\n"
                + "      <Table name=\"time_by_day\"/>\n"
                + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
                + "          levelType=\"TimeYears\"/>\n"
                + "      <Level name=\"Week\" column=\"week_of_year\" type=\"Numeric\" uniqueMembers=\"false\"\n"
                + "          levelType=\"TimeWeeks\"/>\n"
                + "      <Level name=\"Day\" column=\"day_of_month\" uniqueMembers=\"false\" type=\"Numeric\"\n"
                + "          levelType=\"TimeDays\"/>\n"
                + "    </Hierarchy>\n"
                + "</Dimension>\n"
                + "<Dimension name=\"Product\" foreignKey=\"product_id\">\n"
                + "    <Hierarchy hasAll=\"true\" primaryKey=\"product_id\" primaryKeyTable=\"product\">\n"
                + "      <Join leftKey=\"product_class_id\" rightKey=\"product_class_id\">\n"
                + "        <Table name=\"product\"/>\n"
                + "        <Table name=\"product_class\"/>\n"
                + "      </Join>\n"
                + "      <Level name=\"Product Family\" table=\"product_class\" column=\"product_family\"\n"
                + "          uniqueMembers=\"true\"/>\n"
                + "      <Level name=\"Product Department\" table=\"product_class\" column=\"product_department\"\n"
                + "          uniqueMembers=\"false\"/>\n"
                + "      <Level name=\"Product Category\" table=\"product_class\" column=\"product_category\"\n"
                + "          uniqueMembers=\"false\"/>\n"
                + "      <Level name=\"Product Subcategory\" table=\"product_class\" column=\"product_subcategory\"\n"
                + "          uniqueMembers=\"false\"/>\n"
                + "      <Level name=\"Brand Name\" table=\"product\" column=\"brand_name\" uniqueMembers=\"false\"/>\n"
                + "      <Level name=\"Product Name\" table=\"product\" column=\"product_name\"\n"
                + "          uniqueMembers=\"true\"/>\n"
                + "    </Hierarchy>\n"
                + "</Dimension>\n"
                + "  <Dimension name=\"Customers\" foreignKey=\"customer_id\">\n"
                + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Customers\" primaryKey=\"customer_id\">\n"
                + "      <Table name=\"customer\"/>\n"
                + "      <Level name=\"Country\" column=\"country\" uniqueMembers=\"true\"/>\n"
                + "      <Level name=\"State Province\" column=\"state_province\" uniqueMembers=\"true\"/>\n"
                + "      <Level name=\"City\" column=\"city\" uniqueMembers=\"false\"/>\n"
                + "      <Level name=\"Name\" column=\"customer_id\" type=\"Numeric\" uniqueMembers=\"true\">\n"
                + "        <NameExpression>\n"
                + "          <SQL dialect=\"oracle\">\n"
                + "\"fname\" || ' ' || \"lname\"\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"hive\">\n"
                + "`customer`.`fullname`\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"hsqldb\">\n"
                + "\"fname\" || ' ' || \"lname\"\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"access\">\n"
                + "fname + ' ' + lname\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"postgres\">\n"
                + "\"fname\" || ' ' || \"lname\"\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"mysql\">\n"
                + "CONCAT(`customer`.`fname`, ' ', `customer`.`lname`)\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"mssql\">\n"
                + "fname + ' ' + lname\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"derby\">\n"
                + "\"customer\".\"fullname\"\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"db2\">\n"
                + "CONCAT(CONCAT(\"customer\".\"fname\", ' '), \"customer\".\"lname\")\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"luciddb\">\n"
                + "\"fname\" || ' ' || \"lname\"\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"neoview\">\n"
                + "\"customer\".\"fullname\"\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"teradata\">\n"
                + "\"fname\" || ' ' || \"lname\"\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"generic\">\n"
                + "fullname\n"
                + "          </SQL>\n"
                + "        </NameExpression>\n"
                + "        <OrdinalExpression>\n"
                + "          <SQL dialect=\"oracle\">\n"
                + "\"fname\" || ' ' || \"lname\"\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"hsqldb\">\n"
                + "\"fname\" || ' ' || \"lname\"\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"access\">\n"
                + "fname + ' ' + lname\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"postgres\">\n"
                + "\"fname\" || ' ' || \"lname\"\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"mysql\">\n"
                + "CONCAT(`customer`.`fname`, ' ', `customer`.`lname`)\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"mssql\">\n"
                + "fname + ' ' + lname\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"neoview\">\n"
                + "\"customer\".\"fullname\"\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"derby\">\n"
                + "\"customer\".\"fullname\"\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"db2\">\n"
                + "CONCAT(CONCAT(\"customer\".\"fname\", ' '), \"customer\".\"lname\")\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"luciddb\">\n"
                + "\"fname\" || ' ' || \"lname\"\n"
                + "          </SQL>\n"
                + "          <SQL dialect=\"generic\">\n"
                + "fullname\n"
                + "          </SQL>\n"
                + "        </OrdinalExpression>\n"
                + "        <Property name=\"Gender\" column=\"gender\"/>\n"
                + "        <Property name=\"Marital Status\" column=\"marital_status\"/>\n"
                + "        <Property name=\"Education\" column=\"education\"/>\n"
                + "        <Property name=\"Yearly Income\" column=\"yearly_income\"/>\n"
                + "      </Level>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"
                + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
                + "      formatString=\"Standard\"/>\n"
                + "  <Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\"\n"
                + "      formatString=\"#,###.00\"/>\n"
                + "  <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
                + "      formatString=\"#,###.00\"/>\n"
                + "  <Measure name=\"Sales Count\" column=\"product_id\" aggregator=\"count\"\n"
                + "      formatString=\"#,###\"/>\n"
                + "  <Measure name=\"Customer Count\" column=\"customer_id\"\n"
                + "      aggregator=\"distinct-count\" formatString=\"#,###\"/>\n"
                + "</Cube></Schema>\n");
         */
        withSchema(context, SchemaModifiers.TestAggregationManagerModifier5::new);
        final String mdxQuery =
            "select {[Measures].[Unit Sales]} on columns, "
            + "non empty CrossJoin({[Time.Weekly].[1997].[1].[15]},CrossJoin({[Customers].[USA].[CA].[Lincoln Acres].[William Smith]}, {[Product].[Drink].[Beverages].[Carbonated Beverages].[Soda].[Washington].[Washington Diet Cola]})) on rows "
            + "from [Sales_Foo] ";
        final String sqlOracle =
            "select count(*) as \"c0\" from \"agg_pl_01_sales_fact_1997\" \"agg_pl_01_sales_fact_1997\"";
        final String sqlMysql =
            "select count(*) as `c0` from `agg_pl_01_sales_fact_1997` as `agg_pl_01_sales_fact_1997`";
        // If the approxRowcount is used, there should not be
        // a query like : select count(*) from agg_pl_01_sales_fact_1997
        assertQuerySqlOrNot(
            context.getConnectionWithDefaultRole(),
            mdxQuery,
            new SqlPattern[] {
                new SqlPattern(
                    DatabaseProduct.ORACLE,
                    sqlOracle,
                    sqlOracle.length()),
                new SqlPattern(
                    DatabaseProduct.MYSQL,
                    sqlMysql,
                    sqlMysql.length())
            },
            true,
            false,
            false);
        context.getCatalogCache().clear();
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonCollapsedAggregate(Context context) {
        prepareContext(context);
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        /*
        final String cube =
            "<Cube name=\"Foo\" defaultMeasure=\"Unit Sales\">\n"
            + "  <Table name=\"sales_fact_1997\">\n"
            + "    <AggExclude name=\"agg_g_ms_pcat_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_c_14_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_pl_01_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_ll_01_sales_fact_1997\"/>"
            + "    <AggName name=\"agg_l_05_sales_fact_1997\">"
            + "        <AggFactCount column=\"fact_count\"/>\n"
            + "        <AggIgnoreColumn column=\"customer_id\"/>\n"
            + "        <AggIgnoreColumn column=\"store_id\"/>\n"
            + "        <AggIgnoreColumn column=\"promotion_id\"/>\n"
            + "        <AggIgnoreColumn column=\"store_sales\"/>\n"
            + "        <AggIgnoreColumn column=\"store_cost\"/>\n"
            + "        <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"unit_sales\" />\n"
            + "        <AggLevel name=\"[Product].[Product Id]\" column=\"product_id\" collapsed=\"false\"/>\n"
            + "    </AggName>\n"
            + "</Table>\n"
            + "<Dimension foreignKey=\"product_id\" name=\"Product\">\n"
            + "<Hierarchy hasAll=\"true\" primaryKey=\"product_id\" primaryKeyTable=\"product\">\n"
            + "  <Join leftKey=\"product_class_id\" rightKey=\"product_class_id\">\n"
            + " <Table name=\"product\"/>\n"
            + " <Table name=\"product_class\"/>\n"
            + "  </Join>\n"
            + "  <Level name=\"Product Family\" table=\"product_class\" column=\"product_family\"\n"
            + "   uniqueMembers=\"true\"/>\n"
            + "  <Level name=\"Product Department\" table=\"product_class\" column=\"product_department\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Category\" table=\"product_class\" column=\"product_category\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Subcategory\" table=\"product_class\" column=\"product_subcategory\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Brand Name\" table=\"product\" column=\"brand_name\" uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Name\" table=\"product\" column=\"product_name\"\n"
            + "   uniqueMembers=\"true\"/>\n"
            + "  <Level name=\"Product Id\" table=\"product\" column=\"product_id\"\n"
            + "   uniqueMembers=\"true\"/>\n"
            + "</Hierarchy>\n"
            + "</Dimension>\n"
            + "<Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "</Cube>\n";
         */
        class TestNonCollapsedAggregateModifier extends PojoMappingModifier {

        	private static final MeasureMappingImpl m = MeasureMappingImpl.builder()
            .withName("Unit Sales")
            .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
            .withAggregatorType(MeasureAggregatorType.SUM)
            .withFormatString("Standard")
            .withVisible(false)
            .build();


            public TestNonCollapsedAggregateModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                result.add(PhysicalCubeMappingImpl.builder()
                    .withName("Foo")
                    .withDefaultMeasure(m)
                    .withQuery(TableQueryMappingImpl.builder()
                    		.withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
                    		.withAggregationExcludes(List.of(
                    			AggregationExcludeMappingImpl.builder()
                                    .withName("agg_g_ms_pcat_sales_fact_1997")
                                    .build(),
                                AggregationExcludeMappingImpl.builder()
                                    .withName("agg_c_14_sales_fact_1997")
                                    .build(),
                                AggregationExcludeMappingImpl.builder()
                                    .withName("agg_pl_01_sales_fact_1997")
                                    .build(),
                                AggregationExcludeMappingImpl.builder()
                                    .withName("agg_ll_01_sales_fact_1997")
                                    .build()
                    		))
                    		.withAggregationTables(List.of(
                    				AggregationNameMappingImpl.builder()
                                    .withName(FoodmartMappingSupplier.AGG_L_05_SALES_FACT_1997)
                                    .withAggregationFactCount(AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.FACT_COUNT_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build())
                                    .withAggregationIgnoreColumns(List.of(
                                    	AggregationColumnNameMappingImpl.builder()
                                           	.withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                            .build(),
                                        AggregationColumnNameMappingImpl.builder()
                                            .withColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                            .build(),
                                        AggregationColumnNameMappingImpl.builder()
                                            .withColumn(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                            .build(),
                                        AggregationColumnNameMappingImpl.builder()
                                    		.withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                    		.build(),
                                    	AggregationColumnNameMappingImpl.builder()
                                            .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                            .build()
                                    ))
                                    .withAggregationMeasures(List.of(
                                    		AggregationMeasureMappingImpl.builder()
                                            .withName("[Measures].[Unit Sales]")
                                            .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                            .build()
                                    ))
                                    .withAggregationLevels(List.of(
                                    	AggregationLevelMappingImpl.builder()
                                            .withName("[Product].[Product Id]")
                                            .withColumn(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                            .withCollapsed(false)
                                            .build()
                                    ))
                                    .build()
                    				))
                    		.build())
                    .withDimensionConnectors(List.of(
                    		DimensionConnectorMappingImpl
                    		.builder()
                    		.withOverrideDimensionName("Product")
                    		.withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                    		.withDimension(
                    				StandardDimensionMappingImpl.builder()
                                    .withName("Product")
                                    .withHierarchies(List.of(
                                        HierarchyMappingImpl.builder()
                                            .withHasAll(true)
                                            .withPrimaryKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                                            .withPrimaryKeyTable(FoodmartMappingSupplier.PRODUCT_TABLE)
                                            .withQuery(
                                                    JoinQueryMappingImpl.builder()
                                                    .withLeft(
                                                    	JoinedQueryElementMappingImpl.builder()
                                                    		.withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT)
                                                    		.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_TABLE).build())
                                                    		.build())
                                                    .withRight(
                                                    	JoinedQueryElementMappingImpl.builder()
                                                    		.withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT_CLASS)
                                                    		.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE).build())
                                                    		.build())
                                                    .build()
                                                )
                                            .withLevels(List.of(
                                                LevelMappingImpl.builder()
                                                    .withName("Product Family")
                                                    .withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE)
                                                    .withColumn(FoodmartMappingSupplier.PRODUCT_FAMILY_COLUMN_IN_PRODUCT_CLASS)
                                                    .withUniqueMembers(true)
                                                    .build(),
                                                LevelMappingImpl.builder()
                                                    .withName("Product Department")
                                                    .withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE)
                                                    .withColumn(FoodmartMappingSupplier.PRODUCT_DEPARTMENT_COLUMN_IN_PRODUCT_CLASS)
                                                    .withUniqueMembers(false)
                                                    .build(),
                                                LevelMappingImpl.builder()
                                                    .withName("Product Category")
                                                    .withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE)
                                                    .withColumn(FoodmartMappingSupplier.PRODUCT_CATEGORY_COLUMN_IN_PRODUCT_CLASS)
                                                    .withUniqueMembers(false)
                                                    .build(),
                                                LevelMappingImpl.builder()
                                                    .withName("Product Subcategory")
                                                    .withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE)
                                                    .withColumn(FoodmartMappingSupplier.PRODUCT_SUBCATEGORY_COLUMN_IN_PRODUCT_CLASS)
                                                    .withUniqueMembers(false)
                                                    .build(),
                                                LevelMappingImpl.builder()
                                                    .withName("Brand Name")
                                                    .withTable(FoodmartMappingSupplier.PRODUCT_TABLE)
                                                    .withColumn(FoodmartMappingSupplier.BRAND_NAME_COLUMN_IN_PRODUCT)
                                                    .withUniqueMembers(false)
                                                    .build(),
                                                LevelMappingImpl.builder()
                                                    .withName("Product Name")
                                                    .withTable(FoodmartMappingSupplier.PRODUCT_TABLE)
                                                    .withColumn(FoodmartMappingSupplier.PRODUCT_NAME_COLUMN_IN_PRODUCT)
                                                    .withUniqueMembers(true)
                                                    .build(),
                                                LevelMappingImpl.builder()
                                                    .withName("Product Id")
                                                    .withTable(FoodmartMappingSupplier.PRODUCT_TABLE)
                                                    .withColumn(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                                                    .withUniqueMembers(true)
                                                    .build()
                                            ))
                                            .build()
                                    ))
                    				.build())
                    		.build()))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                    		.withMeasures(List.of(m))
                    		.build()))
                    .build());
                result.addAll(super.cubes(cubes));
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
                null, cube, null, null, null, null);
        withSchema(context, schema);
         */
        withSchema(context, TestNonCollapsedAggregateModifier::new);
        final String mdx =
            "select {[Product].[Product Family].Members} on rows, {[Measures].[Unit Sales]} on columns from [Foo]";
        final String sqlOracle =
            "select \"product_class\".\"product_family\" as \"c0\", sum(\"agg_l_05_sales_fact_1997\".\"unit_sales\") as \"m0\" from \"product_class\" \"product_class\", \"product\" \"product\", \"agg_l_05_sales_fact_1997\" \"agg_l_05_sales_fact_1997\" where \"agg_l_05_sales_fact_1997\".\"product_id\" = \"product\".\"product_id\" and \"product\".\"product_class_id\" = \"product_class\".\"product_class_id\" group by \"product_class\".\"product_family\"";
        final String sqlMysql =
            "select `product_class`.`product_family` as `c0`, sum(`agg_l_05_sales_fact_1997`.`unit_sales`) as `m0` from `product_class` as `product_class`, `product` as `product`, `agg_l_05_sales_fact_1997` as `agg_l_05_sales_fact_1997` where `agg_l_05_sales_fact_1997`.`product_id` = `product`.`product_id` and `product`.`product_class_id` = `product_class`.`product_class_id` group by `product_class`.`product_family`";
        assertQuerySqlOrNot(
            context.getConnectionWithDefaultRole(),
            mdx,
            new SqlPattern[] {
                new SqlPattern(
                    DatabaseProduct.ORACLE,
                    sqlOracle,
                    sqlOracle.length()),
                new SqlPattern(
                    DatabaseProduct.MYSQL,
                    sqlMysql,
                    sqlMysql.length())
            },
            false, false, true);
        context.getCatalogCache().clear();
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonCollapsedAggregateAllLevelsPresentInQuerySnowflake(Context context)
        throws Exception
    {
        prepareContext(context);
        // MONDRIAN-1072.
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        /*
        final String cube =
            "<Schema name=\"AMC\"><Cube name=\"Foo\" defaultMeasure=\"Unit Sales\">\n"
            + "  <Table name=\"sales_fact_1997\">\n"
            + "    <AggExclude name=\"agg_g_ms_pcat_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_c_14_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_pl_01_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_ll_01_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_l_03_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_lc_06_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_l_04_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_c_10_sales_fact_1997\"/>"
            + "    <AggName name=\"agg_l_05_sales_fact_1997\">"
            + "        <AggFactCount column=\"fact_count\"/>\n"
            + "        <AggIgnoreColumn column=\"customer_id\"/>\n"
            + "        <AggIgnoreColumn column=\"store_id\"/>\n"
            + "        <AggIgnoreColumn column=\"promotion_id\"/>\n"
            + " <AggForeignKey factColumn=\"product_id\" aggColumn=\"product_id\"/>"
            + "        <AggMeasure name=\"[Measures].[Store Cost]\" column=\"store_cost\" />\n"
            + "        <AggMeasure name=\"[Measures].[Store Sales]\" column=\"store_sales\" />\n"
            + "        <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"unit_sales\" />\n"
            + "    </AggName>\n"
            + "</Table>\n"
            + "  <Dimension name=\"Product\" foreignKey=\"product_id\">\n"
            + "<Hierarchy hasAll=\"true\" primaryKey=\"product_id\" primaryKeyTable=\"product\">\n"
            + "      <Join leftKey=\"product_class_id\" rightKey=\"product_class_id\">\n"
            + "        <Table name=\"product\"/>\n"
            + "        <Table name=\"product_class\"/>\n"
            + "     </Join>\n"
            + "     <Level name=\"Product Family\" table=\"product_class\" column=\"product_family\"\n"
            + "        uniqueMembers=\"true\"/>"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "<Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "<Measure name=\"Customer Count\" column=\"customer_id\" aggregator=\"distinct-count\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "<Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "<Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "</Cube></Schema>\n";
        withSchema(context, cube);
         */
        withSchema(context, SchemaModifiers.TestAggregationManagerModifier::new);

        final String mdx =
            "select \n"
            + "{ "
            + "[Product].[Product Family].members } on rows, "
            + "{[Measures].[Unit Sales]} on columns from [Foo]";

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            +    "{}\n"
            +    "Axis #1:\n"
            +    "{[Measures].[Unit Sales]}\n"
            +    "Axis #2:\n"
            +    "{[Product].[Drink]}\n"
            +    "{[Product].[Food]}\n"
            +    "{[Product].[Non-Consumable]}\n"
            +    "Row #0: 24,597\n"
            +    "Row #1: 191,940\n"
            +    "Row #2: 50,236\n");
        final String sqlMysql =
            "select `product_class`.`product_family` as `c0`, sum(`agg_l_05_sales_fact_1997`.`unit_sales`) as `m0` from `product_class` as `product_class`, `product` as `product`, `agg_l_05_sales_fact_1997` as `agg_l_05_sales_fact_1997` where `agg_l_05_sales_fact_1997`.`product_id` = `product`.`product_id` and `product`.`product_class_id` = `product_class`.`product_class_id` group by `product_class`.`product_family`";
        assertQuerySqlOrNot(
            context.getConnectionWithDefaultRole(),
            mdx,
            new SqlPattern[] {
                new SqlPattern(
                    DatabaseProduct.MYSQL,
                    sqlMysql,
                    sqlMysql.length())
            },
            false, false, true);
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonCollapsedAggregateAllLevelsPresentInQuery(Context context)
        throws Exception
    {
        prepareContext(context);
        // MONDRIAN-1072
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        /*
        final String cube =
            "<Schema name=\"AMC\"><Cube name=\"Foo\" defaultMeasure=\"Unit Sales\">\n"
            + "  <Table name=\"sales_fact_1997\">\n"
            + "    <AggExclude name=\"agg_g_ms_pcat_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_c_14_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_pl_01_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_ll_01_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_l_03_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_lc_06_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_l_04_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_c_10_sales_fact_1997\"/>"
            + "    <AggName name=\"agg_l_05_sales_fact_1997\">"
            + "        <AggFactCount column=\"fact_count\"/>\n"
            + "        <AggIgnoreColumn column=\"customer_id\"/>\n"
            + "        <AggIgnoreColumn column=\"store_id\"/>\n"
            + "        <AggIgnoreColumn column=\"promotion_id\"/>\n"
            + " <AggForeignKey factColumn=\"promotion_id\" aggColumn=\"promotion_id\"/>"
            + "        <AggMeasure name=\"[Measures].[Store Cost]\" column=\"store_cost\" />\n"
            + "        <AggMeasure name=\"[Measures].[Store Sales]\" column=\"store_sales\" />\n"
            + "        <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"unit_sales\" />\n"
            + "    </AggName>\n"
            + "</Table>\n"
            + "  <Dimension name=\"Promotions\" foreignKey=\"promotion_id\">\n"
            + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Promotions\" primaryKey=\"promotion_id\" defaultMember=\"[All Promotions]\">\n"
            + "      <Table name=\"promotion\"/>\n"
            + "      <Level name=\"Media Type\" column=\"media_type\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>"
            + "<Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "<Measure name=\"Customer Count\" column=\"customer_id\" aggregator=\"distinct-count\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "<Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "<Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "</Cube></Schema>\n";
        withSchema(context, cube);
         */
        withSchema(context, SchemaModifiers.TestAggregationManagerModifier8::new);
        final String mdx =
            "select \n"
            + "{ "
            + "[Promotions].[Media Type].members } on rows, {[Measures].[Unit Sales]} on columns from [Foo]";

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Promotions].[Bulk Mail]}\n"
            + "{[Promotions].[Cash Register Handout]}\n"
            + "{[Promotions].[Daily Paper]}\n"
            + "{[Promotions].[Daily Paper, Radio]}\n"
            + "{[Promotions].[Daily Paper, Radio, TV]}\n"
            + "{[Promotions].[In-Store Coupon]}\n"
            + "{[Promotions].[No Media]}\n"
            + "{[Promotions].[Product Attachment]}\n"
            + "{[Promotions].[Radio]}\n"
            + "{[Promotions].[Street Handout]}\n"
            + "{[Promotions].[Sunday Paper]}\n"
            + "{[Promotions].[Sunday Paper, Radio]}\n"
            + "{[Promotions].[Sunday Paper, Radio, TV]}\n"
            + "{[Promotions].[TV]}\n"
            + "Row #0: 4,320\n"
            + "Row #1: 6,697\n"
            + "Row #2: 7,738\n"
            + "Row #3: 6,891\n"
            + "Row #4: 9,513\n"
            + "Row #5: 3,798\n"
            + "Row #6: 195,448\n"
            + "Row #7: 7,544\n"
            + "Row #8: 2,454\n"
            + "Row #9: 5,753\n"
            + "Row #10: 4,339\n"
            + "Row #11: 5,945\n"
            + "Row #12: 2,726\n"
            + "Row #13: 3,607\n");
        final String sqlMysql =
            "select `promotion`.`media_type` as `c0`, sum(`agg_c_special_sales_fact_1997`.`unit_sales_sum`) as `m0` from `promotion` as `promotion`, `agg_c_special_sales_fact_1997` as `agg_c_special_sales_fact_1997` where `agg_c_special_sales_fact_1997`.`promotion_id` = `promotion`.`promotion_id` group by `promotion`.`media_type`";
        assertQuerySqlOrNot(
            context.getConnectionWithDefaultRole(),
            mdx,
            new SqlPattern[] {
                new SqlPattern(
                    DatabaseProduct.MYSQL,
                    sqlMysql,
                    sqlMysql.length())
            },
            false, false, true);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTwoNonCollapsedAggregate(Context context) throws Exception {
        prepareContext(context);
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        ((TestConfig)context.getConfig()).setGenerateFormattedSql(true);
        /*
        final String cube =
            "<Cube name=\"Foo\" defaultMeasure=\"Unit Sales\">\n"
            + "  <Table name=\"sales_fact_1997\">\n"
            + "    <AggExclude name=\"agg_g_ms_pcat_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_c_14_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_pl_01_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_ll_01_sales_fact_1997\"/>"
            + "    <AggName name=\"agg_l_05_sales_fact_1997\">"
            + "        <AggFactCount column=\"fact_count\"/>\n"
            + "        <AggIgnoreColumn column=\"customer_id\"/>\n"
            + "        <AggIgnoreColumn column=\"promotion_id\"/>\n"
            + "        <AggIgnoreColumn column=\"store_sales\"/>\n"
            + "        <AggIgnoreColumn column=\"store_cost\"/>\n"
            + "        <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"unit_sales\" />\n"
            + "        <AggLevel name=\"[Product].[Product Id]\" column=\"product_id\" collapsed=\"false\"/>\n"
            + "        <AggLevel name=\"[Store].[Store Id]\" column=\"store_id\" collapsed=\"false\"/>\n"
            + "    </AggName>\n"
            + "</Table>\n"
            + "<Dimension foreignKey=\"product_id\" name=\"Product\">\n"
            + "<Hierarchy hasAll=\"true\" primaryKey=\"product_id\" primaryKeyTable=\"product\">\n"
            + "  <Join leftKey=\"product_class_id\" rightKey=\"product_class_id\">\n"
            + " <Table name=\"product\"/>\n"
            + " <Table name=\"product_class\"/>\n"
            + "  </Join>\n"
            + "  <Level name=\"Product Family\" table=\"product_class\" column=\"product_family\"\n"
            + "   uniqueMembers=\"true\"/>\n"
            + "  <Level name=\"Product Department\" table=\"product_class\" column=\"product_department\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Category\" table=\"product_class\" column=\"product_category\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Subcategory\" table=\"product_class\" column=\"product_subcategory\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Brand Name\" table=\"product\" column=\"brand_name\" uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Name\" table=\"product\" column=\"product_name\"\n"
            + "   uniqueMembers=\"true\"/>\n"
            + "  <Level name=\"Product Id\" table=\"product\" column=\"product_id\"\n"
            + "   uniqueMembers=\"true\"/>\n"
            + "</Hierarchy>\n"
            + "</Dimension>\n"
            + "  <Dimension name=\"Store\" foreignKey=\"store_id\" >\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\"\n"
            + "        primaryKeyTable=\"store\">\n"
            + "      <Join leftKey=\"region_id\" rightKey=\"region_id\">\n"
            + "        <Table name=\"store\"/>\n"
            + "        <Table name=\"region\"/>\n"
            + "      </Join>\n"
            + "      <Level name=\"Store Region\" table=\"region\" column=\"sales_city\"\n"
            + "          uniqueMembers=\"false\"/>\n"
            + "      <Level name=\"Store Id\" table=\"store\" column=\"store_id\"\n"
            + "          uniqueMembers=\"true\">\n"
            + "      </Level>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "<Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "</Cube>\n";
         */
        class TestTwoNonCollapsedAggregateModifier extends PojoMappingModifier {

            public TestTwoNonCollapsedAggregateModifier(CatalogMapping catalog) {
                super(catalog);
            }
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
            	MeasureMappingImpl m = MeasureMappingImpl.builder()
                        .withName("Unit Sales")
                        .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                        .withAggregatorType(MeasureAggregatorType.SUM)
                        .withFormatString("Standard")
                        .build();
                ColumnImpl salesRegion = ColumnImpl.builder().withName("sales_region").withType("VARCHAR").withCharOctetLength(30).build();
                ColumnImpl salesCity = ColumnImpl.builder().withName("sales_city").withType("VARCHAR").withCharOctetLength(30).build();
                ColumnImpl salesDistrictId = ColumnImpl.builder().withName("sales_district_id").withType("INTEGER").build();
                ColumnImpl regionId = ColumnImpl.builder().withName("region_id").withType("INTEGER").build();
                PhysicalTableImpl region = ((org.eclipse.daanse.rdb.structure.pojo.PhysicalTableImpl.Builder) PhysicalTableImpl.builder().withName("region")
                        .withColumns(List.of(
                                salesRegion, salesCity, salesDistrictId, regionId
                                ))).build();
                List<CubeMapping> result = new ArrayList<>();
                result.add(PhysicalCubeMappingImpl.builder()
                    .withName("Foo")
                    .withDefaultMeasure(m)
                    .withQuery(TableQueryMappingImpl.builder()
                    	.withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
                    	.withAggregationExcludes(List.of(
                    		AggregationExcludeMappingImpl.builder()
                            .withName("agg_g_ms_pcat_sales_fact_1997")
                            .build(),
                            AggregationExcludeMappingImpl.builder()
                            .withName("agg_c_14_sales_fact_1997")
                            .build(),
                            AggregationExcludeMappingImpl.builder()
                            .withName("agg_pl_01_sales_fact_1997")
                            .build(),
                            AggregationExcludeMappingImpl.builder()
                            .withName("agg_ll_01_sales_fact_1997")
                            .build()
                    	))
                    	.withAggregationTables(List.of(
                    		AggregationNameMappingImpl.builder()
                            .withName(FoodmartMappingSupplier.AGG_L_05_SALES_FACT_1997)
                            .withAggregationFactCount(AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.FACT_COUNT_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build())
                            .withAggregationIgnoreColumns(List.of(
                            	AggregationColumnNameMappingImpl.builder()
                                    .withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                    .build(),
                                AggregationColumnNameMappingImpl.builder()
                                    .withColumn(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                    .build(),
                                AggregationColumnNameMappingImpl.builder()
                                    .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                    .build(),
                                AggregationColumnNameMappingImpl.builder()
                                    .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                    .build()
                            ))
                            .withAggregationMeasures(List.of(
                            	AggregationMeasureMappingImpl.builder()
                                    .withName("[Measures].[Unit Sales]")
                                    .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                    .build()
                            ))
                            .withAggregationLevels(List.of(
                            	AggregationLevelMappingImpl.builder()
                                    .withName("[Product].[Product Id]")
                                    .withColumn(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                    .withCollapsed(false)
                                    .build(),
                                AggregationLevelMappingImpl.builder()
                                    .withName("[Store].[Store Id]")
                                    .withColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                    .withCollapsed(false)
                                    .build()
                            ))
                            .build()
                    	)).build())
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Product")
                            .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withHierarchies(List.of(
                                HierarchyMappingImpl.builder()
                                    .withHasAll(true)
                                    .withPrimaryKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                                    .withPrimaryKeyTable(FoodmartMappingSupplier.PRODUCT_TABLE)
                                    .withQuery(
                                            JoinQueryMappingImpl.builder()
                                            .withLeft(
                                            	JoinedQueryElementMappingImpl.builder()
                                            		.withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT)
                                            		.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_TABLE).build())
                                            		.build())
                                            .withRight(
                                            	JoinedQueryElementMappingImpl.builder()
                                            		.withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT_CLASS)
                                            		.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE).build())
                                            		.build())
                                            .build()
                                    )
                                    .withLevels(List.of(
                                        LevelMappingImpl.builder()
                                            .withName("Product Family")
                                            .withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE)
                                            .withColumn(FoodmartMappingSupplier.PRODUCT_FAMILY_COLUMN_IN_PRODUCT_CLASS)
                                            .withUniqueMembers(true)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("Product Department")
                                            .withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE)
                                            .withColumn(FoodmartMappingSupplier.PRODUCT_DEPARTMENT_COLUMN_IN_PRODUCT_CLASS)
                                            .withUniqueMembers(false)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("Product Category")
                                            .withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE)
                                            .withColumn(FoodmartMappingSupplier.PRODUCT_CATEGORY_COLUMN_IN_PRODUCT_CLASS)
                                            .withUniqueMembers(false)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("Product Subcategory")
                                            .withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE)
                                            .withColumn(FoodmartMappingSupplier.PRODUCT_SUBCATEGORY_COLUMN_IN_PRODUCT_CLASS)
                                            .withUniqueMembers(false)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("Brand Name")
                                            .withTable(FoodmartMappingSupplier.PRODUCT_TABLE)
                                            .withColumn(FoodmartMappingSupplier.BRAND_NAME_COLUMN_IN_PRODUCT)
                                            .withUniqueMembers(false)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("Product Name")
                                            .withTable(FoodmartMappingSupplier.PRODUCT_TABLE)
                                            .withColumn(FoodmartMappingSupplier.PRODUCT_NAME_COLUMN_IN_PRODUCT)
                                            .withUniqueMembers(true)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("Product Id")
                                            .withTable(FoodmartMappingSupplier.PRODUCT_TABLE)
                                            .withColumn(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                                            .withUniqueMembers(true)
                                            .build()
                                    ))
                                    .build()
                            )).build())
                            .build(),
                        DimensionConnectorMappingImpl.builder()
                        	.withOverrideDimensionName("Store")
                            .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            		.withName("Store")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                    .withHasAll(true)
                                    .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                                    .withPrimaryKeyTable(FoodmartMappingSupplier.STORE_TABLE)
                                    .withQuery(
                                            JoinQueryMappingImpl.builder()
                                            .withLeft(
                                            	JoinedQueryElementMappingImpl.builder()
                                            		.withKey(FoodmartMappingSupplier.REGION_ID_COLUMN_IN_STORE)
                                            		.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                                            		.build())
                                            .withRight(
                                            	JoinedQueryElementMappingImpl.builder()
                                            		.withKey(regionId)
                                            		.withQuery(TableQueryMappingImpl.builder().withTable(region).build())
                                            		.build())
                                            .build()
                                    )
                                    .withLevels(List.of(
                                        LevelMappingImpl.builder()
                                            .withName("Store Region")
                                            .withTable(region)
                                            .withColumn(salesCity)
                                            .withUniqueMembers(false)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("Store Id")
                                            .withTable(FoodmartMappingSupplier.STORE_TABLE)
                                            .withColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                                            .withUniqueMembers(true)
                                            .build()
                                    ))
                                    .build()
                            )).build())
                            .build()

                    ))
                   .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                	    .withMeasures(List.of(
                	    	MeasureMappingImpl.builder()
                            	.withName("Unit Sales")
                                .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .withFormatString("Standard")
                                .build()

                        )).build()))
                    .build());
                result.addAll(super.cubes(cubes));
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
                null, cube, null, null, null, null);
        withSchema(context, schema);
         */
        withSchema(context, TestTwoNonCollapsedAggregateModifier::new);

        final String mdx =
            "select {Crossjoin([Product].[Product Family].Members, [Store].[Store Id].Members)} on rows, {[Measures].[Unit Sales]} on columns from [Foo]";
        final String sqlOracle =
            "select\n"
            + "    \"product_class\".\"product_family\" as \"c0\",\n"
            + "    \"agg_l_05_sales_fact_1997\".\"store_id\" as \"c1\",\n"
            + "    sum(\"agg_l_05_sales_fact_1997\".\"unit_sales\") as \"m0\"\n"
            + "from\n"
            + "    \"product_class\" \"product_class\",\n"
            + "    \"product\" \"product\",\n"
            + "    \"agg_l_05_sales_fact_1997\" \"agg_l_05_sales_fact_1997\"\n"
            + "where\n"
            + "    \"agg_l_05_sales_fact_1997\".\"product_id\" = \"product\".\"product_id\"\n"
            + "and\n"
            + "    \"product\".\"product_class_id\" = \"product_class\".\"product_class_id\"\n"
            + "group by\n"
            + "    \"product_class\".\"product_family\",\n"
            + "    \"agg_l_05_sales_fact_1997\".\"store_id\"";
        final String sqlMysql =
            "select\n"
            + "    `product_class`.`product_family` as `c0`,\n"
            + "    `agg_l_05_sales_fact_1997`.`store_id` as `c1`,\n"
            + "    sum(`agg_l_05_sales_fact_1997`.`unit_sales`) as `m0`\n"
            + "from\n"
            + "    `product_class` as `product_class`,\n"
            + "    `product` as `product`,\n"
            + "    `agg_l_05_sales_fact_1997` as `agg_l_05_sales_fact_1997`\n"
            + "where\n"
            + "    `agg_l_05_sales_fact_1997`.`product_id` = `product`.`product_id`\n"
            + "and\n"
            + "    `product`.`product_class_id` = `product_class`.`product_class_id`\n"
            + "group by\n"
            + "    `product_class`.`product_family`,\n"
            + "    `agg_l_05_sales_fact_1997`.`store_id`";
        assertQuerySqlOrNot(
            context.getConnectionWithDefaultRole(),
            mdx,
            new SqlPattern[] {
                new SqlPattern(
                    DatabaseProduct.ORACLE,
                    sqlOracle,
                    sqlOracle.length()),
                new SqlPattern(
                    DatabaseProduct.MYSQL,
                    sqlMysql,
                    sqlMysql.length())
            },
            false, false, true);
    }

    /**
     * This is a test for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1221">MONDRIAN-1221</a>
     *
     * When performing a non-empty crossjoin over a virtual cube with agg
     * tables, there was no match with any agg tables.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testVirtualCubeAggBugMondrian1221(Context context) {
        prepareContext(context);
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        ((TestConfig)context.getConfig()).setGenerateFormattedSql(true);
        /*
        final String schema =
            "<?xml version=\"1.0\"?>\n"
            + "<Schema name=\"custom\">\n"
            + "  <Dimension name=\"Store\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">\n"
            + "      <Table name=\"store\"/>\n"
            + "      <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"Store State\" column=\"store_state\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"Store City\" column=\"store_city\" uniqueMembers=\"false\"/>\n"
            + "      <Level name=\"Store Name\" column=\"store_name\" uniqueMembers=\"true\">\n"
            + "      </Level>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Dimension name=\"Time\" type=\"TimeDimension\">\n"
            + "    <Hierarchy hasAll=\"false\" primaryKey=\"time_id\">\n"
            + "      <Table name=\"time_by_day\"/>\n"
            + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
            + "          levelType=\"TimeYears\"/>\n"
            + "      <Level name=\"Quarter\" column=\"quarter\" uniqueMembers=\"false\"\n"
            + "          levelType=\"TimeQuarters\"/>\n"
            + "      <Level name=\"Month\" column=\"month_of_year\" uniqueMembers=\"false\" type=\"Numeric\"\n"
            + "          levelType=\"TimeMonths\"/>\n"
            + "    </Hierarchy>\n"
            + "    <Hierarchy hasAll=\"true\" name=\"Weekly\" primaryKey=\"time_id\">\n"
            + "      <Table name=\"time_by_day\"/>\n"
            + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
            + "          levelType=\"TimeYears\"/>\n"
            + "      <Level name=\"Week\" column=\"week_of_year\" type=\"Numeric\" uniqueMembers=\"false\"\n"
            + "          levelType=\"TimeWeeks\"/>\n"
            + "      <Level name=\"Day\" column=\"day_of_month\" uniqueMembers=\"false\" type=\"Numeric\"\n"
            + "          levelType=\"TimeDays\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Cube name=\"Sales1\" defaultMeasure=\"Unit Sales\">\n"
            + "    <Table name=\"sales_fact_1997\">\n"
            + "      <AggName name=\"agg_c_special_sales_fact_1997\">\n"
            + "        <AggFactCount column=\"FACT_COUNT\"/>\n"
            + "        <AggIgnoreColumn column=\"foo\"/>\n"
            + "        <AggIgnoreColumn column=\"bar\"/>\n"
            + "        <AggIgnoreColumn column=\"PRODUCT_ID\" />\n"
            + "        <AggIgnoreColumn column=\"CUSTOMER_ID\" />\n"
            + "        <AggIgnoreColumn column=\"PROMOTION_ID\" />\n"
            + "        <AggForeignKey factColumn=\"store_id\" aggColumn=\"STORE_ID\" />\n"
            + "        <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"UNIT_SALES_SUM\" />\n"
            + "        <AggMeasure name=\"[Measures].[Store Cost]\" column=\"STORE_COST_SUM\" />\n"
            + "        <AggMeasure name=\"[Measures].[Store Sales]\" column=\"STORE_SALES_SUM\" />\n"
            + "        <AggLevel name=\"[Time].[Year]\" column=\"TIME_YEAR\" />\n"
            + "        <AggLevel name=\"[Time].[Quarter]\" column=\"TIME_QUARTER\" />\n"
            + "        <AggLevel name=\"[Time].[Month]\" column=\"TIME_MONTH\" />\n"
            + "      </AggName>\n"
            + "    </Table>\n"
            + "    <DimensionUsage name=\"Store\" source=\"Store\" foreignKey=\"store_id\"/>\n"
            + "    <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "    <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "    <Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\"\n"
            + "      formatString=\"#,###.00\"/>\n"
            + "    <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"#,###.00\"/>\n"
            + "  </Cube>\n"
            + "  <Cube name=\"Sales2\" defaultMeasure=\"Unit Sales\">\n"
            + "    <Table name=\"sales_fact_1997\">\n"
            + "      <AggName name=\"agg_c_special_sales_fact_1997\">\n"
            + "        <AggFactCount column=\"FACT_COUNT\"/>\n"
            + "        <AggIgnoreColumn column=\"foo\"/>\n"
            + "        <AggIgnoreColumn column=\"bar\"/>\n"
            + "        <AggIgnoreColumn column=\"PRODUCT_ID\" />\n"
            + "        <AggIgnoreColumn column=\"CUSTOMER_ID\" />\n"
            + "        <AggIgnoreColumn column=\"PROMOTION_ID\" />\n"
            + "        <AggForeignKey factColumn=\"store_id\" aggColumn=\"STORE_ID\" />\n"
            + "        <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"UNIT_SALES_SUM\" />\n"
            + "        <AggMeasure name=\"[Measures].[Store Cost]\" column=\"STORE_COST_SUM\" />\n"
            + "        <AggMeasure name=\"[Measures].[Store Sales]\" column=\"STORE_SALES_SUM\" />\n"
            + "        <AggLevel name=\"[Time].[Year]\" column=\"TIME_YEAR\" />\n"
            + "        <AggLevel name=\"[Time].[Quarter]\" column=\"TIME_QUARTER\" />\n"
            + "        <AggLevel name=\"[Time].[Month]\" column=\"TIME_MONTH\" />\n"
            + "      </AggName>\n"
            + "    </Table>\n"
            + "    <DimensionUsage name=\"Store\" source=\"Store\" foreignKey=\"store_id\"/>\n"
            + "    <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "    <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "    <Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\"\n"
            + "      formatString=\"#,###.00\"/>\n"
            + "    <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"#,###.00\"/>\n"
            + "  </Cube>\n"
            + "  <VirtualCube name=\"SuperSales\" defaultMeasure=\"Unit Sales\">\n"
            + "    <VirtualCubeDimension cubeName=\"Sales1\" name=\"Store\"/>\n"
            + " <VirtualCubeDimension cubeName=\"Sales1\" name=\"Time\"/>\n"
            + "    <VirtualCubeMeasure cubeName=\"Sales2\" name=\"[Measures].[Unit Sales]\"/>\n"
            + " <VirtualCubeMeasure cubeName=\"Sales2\" name=\"[Measures].[Store Cost]\"/>\n"
            + " <VirtualCubeMeasure cubeName=\"Sales2\" name=\"[Measures].[Store Sales]\"/>\n"
            + "  </VirtualCube>\n"
            + "</Schema>\n";
         */
        final String mdx =
            "select {NonEmptyCrossJoin([Time].[Month].Members, [Store].[Store Country].Members)} on rows,"
            + "{[Measures].[Unit Sales]} on columns "
            + "from [SuperSales]";

        withSchema(context, SchemaModifiers.TestAggregationManagerModifier9::new);

        if (context.getConfig().enableNativeCrossJoin()) {
            final String sqlMysql =
                "select\n"
                + "    `agg_c_14_sales_fact_1997`.`the_year` as `c0`,\n"
                + "    `agg_c_14_sales_fact_1997`.`quarter` as `c1`,\n"
                + "    `agg_c_14_sales_fact_1997`.`month_of_year` as `c2`,\n"
                + "    `store`.`store_country` as `c3`\n"
                + "from\n"
                + "    `agg_c_14_sales_fact_1997` as `agg_c_14_sales_fact_1997`,\n"
                + "    `store` as `store`\n"
                + "where\n"
                + "    `agg_c_14_sales_fact_1997`.`store_id` = `store`.`store_id`\n"
                + "group by\n"
                + "    `agg_c_14_sales_fact_1997`.`the_year`,\n"
                + "    `agg_c_14_sales_fact_1997`.`quarter`,\n"
                + "    `agg_c_14_sales_fact_1997`.`month_of_year`,\n"
                + "    `store`.`store_country`\n"
                + "order by\n"
                + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                    ? "    ISNULL(`c0`) ASC, `c0` ASC,\n"
                    + "    ISNULL(`c1`) ASC, `c1` ASC,\n"
                    + "    ISNULL(`c2`) ASC, `c2` ASC,\n"
                    + "    ISNULL(`c3`) ASC, `c3` ASC"
                    : "    ISNULL(`agg_c_14_sales_fact_1997`.`the_year`) ASC, `agg_c_14_sales_fact_1997`.`the_year` ASC,\n"
                    + "    ISNULL(`agg_c_14_sales_fact_1997`.`quarter`) ASC, `agg_c_14_sales_fact_1997`.`quarter` ASC,\n"
                    + "    ISNULL(`agg_c_14_sales_fact_1997`.`month_of_year`) ASC, `agg_c_14_sales_fact_1997`.`month_of_year` ASC,\n"
                    + "    ISNULL(`store`.`store_country`) ASC, `store`.`store_country` ASC");

            assertQuerySqlOrNot(
                context.getConnectionWithDefaultRole(),
                mdx,
                new SqlPattern[] {
                    new SqlPattern(
                        DatabaseProduct.MYSQL,
                        sqlMysql,
                        sqlMysql.length())
                },
                false, false, true);
        }
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Time].[1997].[Q1].[1], [Store].[USA]}\n"
            + "{[Time].[1997].[Q1].[2], [Store].[USA]}\n"
            + "{[Time].[1997].[Q1].[3], [Store].[USA]}\n"
            + "{[Time].[1997].[Q2].[4], [Store].[USA]}\n"
            + "{[Time].[1997].[Q2].[5], [Store].[USA]}\n"
            + "{[Time].[1997].[Q2].[6], [Store].[USA]}\n"
            + "{[Time].[1997].[Q3].[7], [Store].[USA]}\n"
            + "{[Time].[1997].[Q3].[8], [Store].[USA]}\n"
            + "{[Time].[1997].[Q3].[9], [Store].[USA]}\n"
            + "{[Time].[1997].[Q4].[10], [Store].[USA]}\n"
            + "{[Time].[1997].[Q4].[11], [Store].[USA]}\n"
            + "{[Time].[1997].[Q4].[12], [Store].[USA]}\n"
            + "Row #0: 21,628\n"
            + "Row #1: 20,957\n"
            + "Row #2: 23,706\n"
            + "Row #3: 20,179\n"
            + "Row #4: 21,081\n"
            + "Row #5: 21,350\n"
            + "Row #6: 23,763\n"
            + "Row #7: 21,697\n"
            + "Row #8: 20,388\n"
            + "Row #9: 19,958\n"
            + "Row #10: 25,270\n"
            + "Row #11: 26,796\n");
    }

    /**
     * This is a test for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1271">MONDRIAN-1271</a>
     *
     * When a non-collapsed AggLevel was used, Mondrian would join on the
     * key column of the lowest level instead of the one it should have.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMondrian1271(Context context) {
        prepareContext(context);
        if (!context.getConfig().enableNativeCrossJoin()) {
            return;
        }
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        ((TestConfig)context.getConfig()).setGenerateFormattedSql(true);
        /*
        final String schema =
            "<?xml version=\"1.0\"?>\n"
            + "<Schema name=\"custom\">\n"
            + "  <Dimension name=\"Store\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">\n"
            + "      <Table name=\"store\"/>\n"
            + "      <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"Store State\" column=\"store_state\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"Store City\" column=\"store_city\" uniqueMembers=\"false\"/>\n"
            + "      <Level name=\"Store Name\" column=\"store_name\" uniqueMembers=\"true\">\n"
            + "      </Level>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Dimension name=\"Time\" type=\"TimeDimension\">\n"
            + "    <Hierarchy hasAll=\"false\" primaryKey=\"time_id\">\n"
            + "      <Table name=\"time_by_day\"/>\n"
            + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
            + "          levelType=\"TimeYears\"/>\n"
            + "      <Level name=\"Quarter\" column=\"quarter\" uniqueMembers=\"false\"\n"
            + "          levelType=\"TimeQuarters\"/>\n"
            + "      <Level name=\"Month\" column=\"month_of_year\" uniqueMembers=\"true\" type=\"Numeric\"\n"
            + "          levelType=\"TimeMonths\"/>\n"
            + "      <Level name=\"Day\" column=\"day_of_month\" uniqueMembers=\"false\" type=\"Numeric\"\n"
            + "          levelType=\"TimeDays\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Cube name=\"Sales1\" defaultMeasure=\"Unit Sales\">\n"
            + "    <Table name=\"sales_fact_1997\">\n"
            + "      <AggExclude name=\"agg_c_special_sales_fact_1997\"/>"
            + "      <AggExclude name=\"agg_c_10_sales_fact_1997\"/>"
            + "      <AggExclude name=\"agg_l_04_sales_fact_1997\"/>"
            + "      <AggExclude name=\"agg_g_ms_pcat_sales_fact_1997\"/>"
            + "      <AggExclude name=\"agg_lc_06_sales_fact_1997\"/>"
            + "      <AggExclude name=\"agg_l_03_sales_fact_1997\"/>"
            + "      <AggExclude name=\"agg_lc_100_sales_fact_1997\"/>"
            + "      <AggExclude name=\"agg_pl_01_sales_fact_1997\"/>"
            + "      <AggExclude name=\"agg_ll_01_sales_fact_1997\"/>"
            + "      <AggExclude name=\"agg_l_05_sales_fact_1997\"/>"
            + "      <AggName name=\"agg_c_14_sales_fact_1997\">\n"
            + "        <AggFactCount column=\"fact_count\"/>\n"
            + "        <AggIgnoreColumn column=\"product_id\" />\n"
            + "        <AggIgnoreColumn column=\"customer_id\" />\n"
            + "        <AggIgnoreColumn column=\"promotion_id\" />\n"
            + "        <AggIgnoreColumn column=\"the_year\" />\n"
            + "        <AggIgnoreColumn column=\"quarter\" />\n"
            + "        <AggForeignKey factColumn=\"store_id\" aggColumn=\"store_id\" />\n"
            + "        <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"unit_sales\" />\n"
            + "        <AggMeasure name=\"[Measures].[Store Cost]\" column=\"store_cost\" />\n"
            + "        <AggMeasure name=\"[Measures].[Store Sales]\" column=\"store_sales\" />\n"
            + "        <AggLevel name=\"[Time].[Month]\" column=\"month_of_year\" collapsed=\"false\" />\n"
            + "      </AggName>\n"
            + "    </Table>\n"
            + "    <DimensionUsage name=\"Store\" source=\"Store\" foreignKey=\"store_id\"/>\n"
            + "    <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "    <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "    <Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\"\n"
            + "      formatString=\"#,###.00\"/>\n"
            + "    <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"#,###.00\"/>\n"
            + "  </Cube>\n"
            + "</Schema>\n";
         */

        final String mdx =
            "select {NonEmptyCrossJoin([Time].[Year].Members, [Store].[Store Country].Members)} on rows,"
            + "{[Measures].[Unit Sales]} on columns "
            + "from [Sales1]";
        final String mdxTooLowForAgg =
            "select {NonEmptyCrossJoin([Time].[Day].Members, [Store].[Store Country].Members)} on rows,"
            + "{[Measures].[Unit Sales]} on columns "
            + "from [Sales1]";

        final String sqlMysqlTupleQuery =
            "select\n"
            + "    `time_by_day`.`the_year` as `c0`,\n"
            + "    `store`.`store_country` as `c1`\n"
            + "from\n"
            + "    `time_by_day` as `time_by_day`,\n"
            + "    `agg_c_14_sales_fact_1997` as `agg_c_14_sales_fact_1997`,\n"
            + "    `store` as `store`\n"
            + "where\n"
            + "    `agg_c_14_sales_fact_1997`.`month_of_year` = `time_by_day`.`month_of_year`\n"
            + "and\n"
            + "    `agg_c_14_sales_fact_1997`.`store_id` = `store`.`store_id`\n"
            + "group by\n"
            + "    `time_by_day`.`the_year`,\n"
            + "    `store`.`store_country`\n"
            + "order by\n"
            + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                ? "    ISNULL(`c0`) ASC, `c0` ASC,\n"
                + "    ISNULL(`c1`) ASC, `c1` ASC"
                : "    ISNULL(`time_by_day`.`the_year`) ASC, `time_by_day`.`the_year` ASC,\n"
                + "    ISNULL(`store`.`store_country`) ASC, `store`.`store_country` ASC");

        final String sqlMysqlSegmentQuery =
            "select\n"
            + "    `store`.`store_country` as `c0`,\n"
            + "    `time_by_day`.`the_year` as `c1`,\n"
            + "    sum(`agg_c_14_sales_fact_1997`.`unit_sales`) as `m0`\n"
            + "from\n"
            + "    `store` as `store`,\n"
            + "    `agg_c_14_sales_fact_1997` as `agg_c_14_sales_fact_1997`,\n"
            + "    `time_by_day` as `time_by_day`\n"
            + "where\n"
            + "    `agg_c_14_sales_fact_1997`.`store_id` = `store`.`store_id`\n"
            + "and\n"
            + "    `store`.`store_country` = 'USA'\n"
            + "and\n"
            + "    `agg_c_14_sales_fact_1997`.`month_of_year` = `time_by_day`.`month_of_year`\n"
            + "group by\n"
            + "    `store`.`store_country`,\n"
            + "    `time_by_day`.`the_year`";

        final String sqlMysqlTooLowTupleQuery =
            "select\n"
            + "    `time_by_day`.`the_year` as `c0`,\n"
            + "    `time_by_day`.`quarter` as `c1`,\n"
            + "    `time_by_day`.`month_of_year` as `c2`,\n"
            + "    `time_by_day`.`day_of_month` as `c3`,\n"
            + "    `store`.`store_country` as `c4`\n"
            + "from\n"
            + "    `time_by_day` as `time_by_day`,\n"
            + "    `sales_fact_1997` as `sales_fact_1997`,\n"
            + "    `store` as `store`\n"
            + "where\n"
            + "    `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`\n"
            + "and\n"
            + "    `sales_fact_1997`.`store_id` = `store`.`store_id`\n"
            + "group by\n"
            + "    `time_by_day`.`the_year`,\n"
            + "    `time_by_day`.`quarter`,\n"
            + "    `time_by_day`.`month_of_year`,\n"
            + "    `time_by_day`.`day_of_month`,\n"
            + "    `store`.`store_country`\n"
            + "order by\n"
            + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                ? "    ISNULL(`c0`) ASC, `c0` ASC,\n"
                + "    ISNULL(`c1`) ASC, `c1` ASC,\n"
                + "    ISNULL(`c2`) ASC, `c2` ASC,\n"
                + "    ISNULL(`c3`) ASC, `c3` ASC,\n"
                + "    ISNULL(`c4`) ASC, `c4` ASC"
                : "    ISNULL(`time_by_day`.`the_year`) ASC, `time_by_day`.`the_year` ASC,\n"
                + "    ISNULL(`time_by_day`.`quarter`) ASC, `time_by_day`.`quarter` ASC,\n"
                + "    ISNULL(`time_by_day`.`month_of_year`) ASC, `time_by_day`.`month_of_year` ASC,\n"
                + "    ISNULL(`time_by_day`.`day_of_month`) ASC, `time_by_day`.`day_of_month` ASC,\n"
                + "    ISNULL(`store`.`store_country`) ASC, `store`.`store_country` ASC");

        final String sqlMysqlTooLowSegmentQuery =
            "select\n"
            + "    `store`.`store_country` as `c0`,\n"
            + "    `time_by_day`.`month_of_year` as `c1`,\n"
            + "    `time_by_day`.`day_of_month` as `c2`,\n"
            + "    sum(`sales_fact_1997`.`unit_sales`) as `m0`\n"
            + "from\n"
            + "    `sales_fact_1997` as `sales_fact_1997`,\n"
            + "    `store` as `store`,\n"
            + "    `time_by_day` as `time_by_day`\n"
            + "where\n"
            + "    `sales_fact_1997`.`store_id` = `store`.`store_id`\n"
            + "and\n"
            + "    `store`.`store_country` = 'USA'\n"
            + "and\n"
            + "    `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`\n"
            + "group by\n"
            + "    `store`.`store_country`,\n"
            + "    `time_by_day`.`month_of_year`,\n"
            + "    `time_by_day`.`day_of_month`";

        withSchema(context, SchemaModifiers.TestAggregationManagerModifier6::new);

        assertQuerySqlOrNot(
            context.getConnectionWithDefaultRole(),
            mdx,
            new SqlPattern[] {
                new SqlPattern(
                    DatabaseProduct.MYSQL,
                    sqlMysqlTupleQuery,
                    sqlMysqlTupleQuery.length())
            },
            false, false, true);

        assertQuerySqlOrNot(
            context.getConnectionWithDefaultRole(),
            mdx,
            new SqlPattern[] {
                new SqlPattern(
                    DatabaseProduct.MYSQL,
                    sqlMysqlSegmentQuery,
                    sqlMysqlSegmentQuery.length())
            },
            false, false, true);

        // Because we have caused a many-to-many relation between the agg table
        // and the dim table, we expect retarded numbers here.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Time].[1997], [Store].[USA]}\n"
            + "{[Time].[1998], [Store].[USA]}\n"
            + "Row #0: 8,119,905\n"
            + "Row #1: 8,119,905\n");

        // Make sure that queries on lower levels don't trigger a
        // false positive with the agg matcher.
        assertQuerySqlOrNot(
            context.getConnectionWithDefaultRole(),
            mdxTooLowForAgg,
            new SqlPattern[] {
                new SqlPattern(
                    DatabaseProduct.MYSQL,
                    sqlMysqlTooLowTupleQuery,
                    sqlMysqlTooLowTupleQuery.length())
            },
            false, false, true);

        assertQuerySqlOrNot(
            context.getConnectionWithDefaultRole(),
            mdxTooLowForAgg,
            new SqlPattern[] {
                new SqlPattern(
                    DatabaseProduct.MYSQL,
                    sqlMysqlTooLowSegmentQuery,
                    sqlMysqlTooLowSegmentQuery.length())
            },
            false, false, true);
        context.getCatalogCache().clear();
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAggStarWithIgnoredColumnsRequiresRollup(Context context) {
        prepareContext(context);
        ((TestConfig)context.getConfig()).setGenerateFormattedSql(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        boolean chooseAggregateByVolume = context.getConfig().chooseAggregateByVolume();
        /*
        withSchema(context,
                "<Schema name=\"FoodMart\">"
                + "  <Dimension name=\"Time\" type=\"TimeDimension\">\n"
                + "    <Hierarchy hasAll=\"false\" primaryKey=\"time_id\">\n"
                + "      <Table name=\"time_by_day\"/>\n"
                + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
                + "          levelType=\"TimeYears\"/>\n"
                + "      <Level name=\"Quarter\" column=\"quarter\" uniqueMembers=\"false\"\n"
                + "          levelType=\"TimeQuarters\"/>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"
                + "<Cube name=\"Sales\" defaultMeasure=\"Unit Sales\">\n"
                + "  <Table name=\"sales_fact_1997\">\n"
                + "    <AggExclude name=\"agg_c_special_sales_fact_1997\" />\n"
                + "    <AggExclude name=\"agg_lc_100_sales_fact_1997\" />\n"
                + "    <AggExclude name=\"agg_lc_10_sales_fact_1997\" />\n"
                + "    <AggExclude name=\"agg_pc_10_sales_fact_1997\" />\n"
                + "    <AggName name=\"agg_c_10_sales_fact_1997\">\n"
                + "        <AggFactCount column=\"FACT_COUNT\"/>\n"
                + "        <AggIgnoreColumn column=\"Quarter\"/>\n"
                + "        <AggIgnoreColumn column=\"MONTH_OF_YEAR\"/>\n"
                + "        <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"unit_sales\" />\n"
                + "        <AggLevel name=\"[Time].[Year]\" column=\"the_year\" />\n"
                + "    </AggName>\n"
                + "  </Table>\n"
                + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
                + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
                + "      formatString=\"Standard\"/>\n"
                + "</Cube>\n"
                + "</Schema>");
         */
        withSchema(context, SchemaModifiers.TestAggregationManagerModifier7::new);
        Connection connection = context.getConnectionWithDefaultRole();
        RolapStar star = connection.getCatalogReader()
            .getCatalog().getRolapStarRegistry().getStar("sales_fact_1997");
        AggStar aggStar1 = getAggStar(star, "agg_c_10_sales_fact_1997");
        AggStar aggStarSpy = spy(
            getAggStar(star, "agg_c_10_sales_fact_1997"));
        // make sure the test AggStar will be prioritized first
        when(aggStarSpy.getSize(chooseAggregateByVolume)).thenReturn(0l);
        connection.getCatalogReader()
            .getCatalog().getRolapStarRegistry().getStar("sales_fact_1997").addAggStar(aggStarSpy);
        boolean[] rollup = { false };
        AggStar returnedStar = AggregationManager
            .findAgg(
                star, aggStarSpy.getLevelBitKey(),
                aggStarSpy.getMeasureBitKey(), rollup);
        assertTrue(rollup[0],
                "Rollup should be true since AggStar has ignored columns ");
        assertEquals(aggStar1.toString(), returnedStar.toString());
        assertTrue(aggStarSpy.hasIgnoredColumns(),
                "Columns marked with AggIgnoreColumn, so AggStar "
                        + ".hasIgnoredColumns() should be true");
        String sqlMysql =
            "select\n"
            + "    `agg_c_10_sales_fact_1997`.`the_year` as `c0`,\n"
            + "    sum(`agg_c_10_sales_fact_1997`.`unit_sales`) as `m0`\n"
            + "from\n"
            + "    `agg_c_10_sales_fact_1997` as `agg_c_10_sales_fact_1997`\n"
            + "where\n"
            + "    `agg_c_10_sales_fact_1997`.`the_year` = 1997\n"
            + "group by\n"
            + "    `agg_c_10_sales_fact_1997`.`the_year`";
        String sqlOra =
            "select\n"
            + "    \"agg_c_10_sales_fact_1997\".\"the_year\" as \"c0\",\n"
            + "    sum(\"agg_c_10_sales_fact_1997\".\"unit_sales\") as \"m0\"\n"
            + "from\n"
            + "    \"agg_c_10_sales_fact_1997\" \"agg_c_10_sales_fact_1997\"\n"
            + "where\n"
            + "    \"agg_c_10_sales_fact_1997\".\"the_year\" = 1997\n"
            + "group by\n"
            + "    \"agg_c_10_sales_fact_1997\".\"the_year\"";
        assertQuerySqlOrNot(
            context.getConnectionWithDefaultRole(),
            "select Time.[1997] on 0 from sales",
            new SqlPattern[]{
                new SqlPattern(
                    DatabaseProduct.MYSQL,
                    sqlMysql,
                    sqlMysql.length()),
                new SqlPattern(
                    DatabaseProduct.ORACLE,
                    sqlOra,
                    sqlOra.length())},
            false, false, true);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAggStarWithUnusedColumnsRequiresRollup(Context context) {
        prepareContext(context);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setGenerateFormattedSql(true);
        /*
        withSchema(context,
                "<Schema name=\"FoodMart\">"
                + "<Cube name=\"Sales\" defaultMeasure=\"Unit Sales\">\n"
                + "  <Table name=\"sales_fact_1997\" />\n"
                + "  <Dimension name=\"Gender\" foreignKey=\"customer_id\">\n"
                + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Gender\" primaryKey=\"customer_id\">\n"
                + "      <Table name=\"customer\"/>\n"
                + "      <Level name=\"Gender\" column=\"gender\" uniqueMembers=\"true\"/>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"
                + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
                + "      formatString=\"Standard\"/>\n"
                + "</Cube>\n"
                + "</Schema>");
        */
        withSchema(context, SchemaModifiers.TestAggregationManagerModifier3::new);

        RolapStar star = context.getConnectionWithDefaultRole().getCatalogReader()
            .getCatalog().getRolapStarRegistry().getStar("sales_fact_1997");
        AggStar aggStarSpy = spy(
            getAggStar(star, "agg_c_special_sales_fact_1997"));
        // make sure the test AggStar will be prioritized first
        when(aggStarSpy.getSize(context.getConfig().chooseAggregateByVolume())).thenReturn(0l);
        context.getConnectionWithDefaultRole().getCatalogReader()
            .getCatalog().getRolapStarRegistry().getStar("sales_fact_1997").addAggStar(aggStarSpy);

        boolean[] rollup = { false };
        AggStar returnedStar = AggregationManager
            .findAgg(
                star, aggStarSpy.getLevelBitKey(),
                aggStarSpy.getMeasureBitKey(), rollup);
        assertTrue(rollup[0],
                "Rollup should be true since AggStar has ignored columns ");
        assertEquals(aggStarSpy, returnedStar);
        assertTrue(aggStarSpy.hasIgnoredColumns(),
                "Unused columns are present, should be marked as "
                        + "having ignored columns.");

        String sqlOra =
            "select\n"
            + "    \"customer\".\"gender\" as \"c0\",\n"
            + "    sum(\"agg_c_special_sales_fact_1997\".\"unit_sales_sum\") as \"m0\"\n"
            + "from\n"
            + "    \"customer\" \"customer\",\n"
            + "    \"agg_c_special_sales_fact_1997\" \"agg_c_special_sales_fact_1997\"\n"
            + "where\n"
            + "    \"agg_c_special_sales_fact_1997\".\"customer_id\" = \"customer\".\"customer_id\"\n"
            + "group by\n"
            + "    \"customer\".\"gender\"";
        String sqlMysql =
            "select\n"
            + "    `customer`.`gender` as `c0`,\n"
            + "    sum(`agg_c_special_sales_fact_1997`.`unit_sales_sum`) as `m0`\n"
            + "from\n"
            + "    `customer` as `customer`,\n"
            + "    `agg_c_special_sales_fact_1997` as `agg_c_special_sales_fact_1997`\n"
            + "where\n"
            + "    `agg_c_special_sales_fact_1997`.`customer_id` = `customer`.`customer_id`\n"
            + "group by\n"
            + "    `customer`.`gender`";
        assertQuerySqlOrNot(
            context.getConnectionWithDefaultRole(),
            "select gender.gender.members on 0 from sales",
            new SqlPattern[]{
                new SqlPattern(
                    DatabaseProduct.MYSQL,
                    sqlMysql,
                    sqlMysql.length()),
                new SqlPattern(
                    DatabaseProduct.ORACLE,
                    sqlOra,
                    sqlOra.length())},
            false, false, true);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAggStarWithIgnoredColumnsAndCountDistinct(Context context) {
        prepareContext(context);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setGenerateFormattedSql(true);
        /*
        withSchema(context,
                "<Schema name=\"FoodMart\">"
                + "  <Dimension name=\"Time\" type=\"TimeDimension\">\n"
                + "    <Hierarchy hasAll=\"false\" primaryKey=\"time_id\">\n"
                + "      <Table name=\"time_by_day\"/>\n"
                + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
                + "          levelType=\"TimeYears\"/>\n"
                + "      <Level name=\"Quarter\" column=\"quarter\" uniqueMembers=\"false\"\n"
                + "          levelType=\"TimeQuarters\"/>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"
                + "<Cube name=\"Sales\" defaultMeasure=\"Unit Sales\">\n"
                + "  <Table name=\"sales_fact_1997\">\n"
                + "    <AggExclude name=\"agg_c_special_sales_fact_1997\" />\n"
                + "    <AggExclude name=\"agg_lc_100_sales_fact_1997\" />\n"
                + "    <AggExclude name=\"agg_lc_10_sales_fact_1997\" />\n"
                + "    <AggExclude name=\"agg_pc_10_sales_fact_1997\" />\n"
                + "    <AggName name=\"agg_g_ms_pcat_sales_fact_1997\">\n"
                + "        <AggFactCount column=\"FACT_COUNT\"/>\n"
                + "        <AggIgnoreColumn column=\"Quarter\"/>\n"
                + "        <AggIgnoreColumn column=\"MONTH_OF_YEAR\"/>\n"
                + "        <AggMeasure name=\"[Measures].[Customer Count]\" column=\"customer_count\" />\n"
                + "        <AggLevel name=\"[Time].[Year]\" column=\"the_year\" />\n"
                + "    </AggName>\n"
                + "  </Table>\n"
                + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
                + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
                + "      formatString=\"Standard\"/>\n"
                + "  <Measure name=\"Customer Count\" column=\"customer_id\" aggregator=\"distinct-count\"\n"
                + "      formatString=\"Standard\"/>\n"
                + "</Cube>\n"
                + "</Schema>");
         */
        withSchema(context, SchemaModifiers.TestAggregationManagerModifier4::new);
        RolapStar star = context.getConnectionWithDefaultRole().getCatalogReader()
            .getCatalog().getRolapStarRegistry().getStar("sales_fact_1997");
        AggStar aggStarSpy = spy(
            getAggStar(star, "agg_g_ms_pcat_sales_fact_1997"));
        // make sure the test AggStar will be prioritized first
        when(aggStarSpy.getSize(context.getConfig().chooseAggregateByVolume())).thenReturn(0l);
        context.getConnectionWithDefaultRole().getCatalogReader()
            .getCatalog().getRolapStarRegistry().getStar("sales_fact_1997").addAggStar(aggStarSpy);
        boolean[] rollup = { false };
        AggStar returnedStar = AggregationManager
            .findAgg(
                star, aggStarSpy.getLevelBitKey(),
                aggStarSpy.getMeasureBitKey(), rollup);
        assertNull(returnedStar,
                "Should not find an agg star given that ignored or unused "
                        + "columns are present, and loading distinct count measure");
        String sqlOra =
            "select\n"
            + "    \"time_by_day\".\"the_year\" as \"c0\",\n"
            + "    count(distinct \"sales_fact_1997\".\"customer_id\") as \"m0\"\n"
            + "from\n"
            + "    \"sales_fact_1997\" \"sales_fact_1997\",\n"
            + "    \"time_by_day\" \"time_by_day\"\n"
            + "where\n"
            + "    \"sales_fact_1997\".\"time_id\" = \"time_by_day\".\"time_id\"\n"
            + "and\n"
            + "    \"time_by_day\".\"the_year\" = 1997\n"
            + "group by\n"
            + "    \"time_by_day\".\"the_year\"";
        String sqlMysql =
            "select\n"
            + "    `time_by_day`.`the_year` as `c0`,\n"
            + "    count(distinct `sales_fact_1997`.`customer_id`) as `m0`\n"
            + "from\n"
            + "    `sales_fact_1997` as `sales_fact_1997`,\n"
            + "    `time_by_day` as `time_by_day`\n"
            + "where\n"
            + "    `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`\n"
            + "and\n"
            + "    `time_by_day`.`the_year` = 1997\n"
            + "group by\n"
            + "    `time_by_day`.`the_year`";
        assertQuerySqlOrNot(
            context.getConnectionWithDefaultRole(),
            "select Time.[1997] on 0 from sales where "
            + "measures.[Customer Count]",
            new SqlPattern[]{
                new SqlPattern(
                    DatabaseProduct.MYSQL,
                    sqlMysql,
                    sqlMysql.length()),
                new SqlPattern(
                    DatabaseProduct.ORACLE,
                    sqlOra,
                    sqlOra.length())},
            false, false, true);
        context.getCatalogCache().clear();
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDisabledReadAggregatesIgnoresDefaultRules(Context context)
        throws Exception
    {
    	context.getCatalogCache().clear();
        prepareContext(context);
        ((TestConfig)context.getConfig()).setReadAggregates(false);
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        String sql =
            "select count(*) as `c0` from `agg_c_10_sales_fact_1997` as `agg_c_10_sales_fact_1997`";
        assertQuerySqlOrNot(
            context.getConnectionWithDefaultRole(),
            "select from sales",
            new SqlPattern[]{
                new SqlPattern(
                    DatabaseProduct.MYSQL, sql, sql.length()) },
            true, true, true);
    }

    private AggStar getAggStar(RolapStar star, String aggStarName) {
        for (AggStar aggStar : star.getAggStars()) {
            if (aggStar.getFactTable().getName().equals(aggStarName)) {
                return aggStar;
            }
        }
        return null;
    }

}
