/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2006-2017 Hitachi Vantara
// All Rights Reserved.
*/
package mondrian.rolap.aggmatcher;

import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.getDialect;

import java.sql.SQLException;
import java.util.List;

import org.eclipse.daanse.cwm.model.cwm.resource.relational.Column;
import org.eclipse.daanse.cwm.model.cwm.resource.relational.Table;
import org.eclipse.daanse.cwm.util.resource.relational.SqlSimpleTypes;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.model.catalog.Catalog;
import org.eclipse.daanse.rolap.mapping.model.catalog.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.database.aggregation.AggregationColumnName;
import org.eclipse.daanse.rolap.mapping.model.database.aggregation.AggregationExclude;
import org.eclipse.daanse.rolap.mapping.model.database.aggregation.AggregationFactory;
import org.eclipse.daanse.rolap.mapping.model.database.aggregation.AggregationForeignKey;
import org.eclipse.daanse.rolap.mapping.model.database.aggregation.AggregationLevel;
import org.eclipse.daanse.rolap.mapping.model.database.aggregation.AggregationLevelProperty;
import org.eclipse.daanse.rolap.mapping.model.database.aggregation.AggregationMeasure;
import org.eclipse.daanse.rolap.mapping.model.database.aggregation.AggregationName;
import org.eclipse.daanse.rolap.mapping.model.database.aggregation.AggregationTable;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.hierarchy.level.LevelFactory;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.hierarchy.level.MemberProperty;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextArgumentsProvider;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;
class ExplicitRecognizerTest extends AggTableTestCase {

    //## TableName: exp_agg_test
    //## ColumnNames:  testyear,testqtr,testmonthord,testmonthname,testmonthcap,testmonprop1,testmonprop2,gender,test_unit_sales,test_store_cost,fact_count
    //## ColumnTypes: INTEGER,VARCHAR(30),INTEGER,VARCHAR(30),VARCHAR(30),VARCHAR(30),VARCHAR(30),VARCHAR(30),INTEGER,DECIMAL(10,4),INTEGER
    private static Column testyearExpAggTest = createColumn("testyear", SqlSimpleTypes.Sql99.integerType(), null, null, null);
    private static Column testqtrExpAggTest = createColumn("testqtr", SqlSimpleTypes.varcharType(255), 30, null, null);
    private static Column testmonthordExpAggTest = createColumn("testmonthord", SqlSimpleTypes.Sql99.integerType(), null, null, null);
    private static Column testmonthnameExpAggTest = createColumn("testmonthname", SqlSimpleTypes.varcharType(255), 30, null, null);
    private static Column testmonthcapExpAggTest = createColumn("testmonthcap", SqlSimpleTypes.varcharType(255), 30, null, null);
    private static Column testmonprop1ExpAggTest = createColumn("testmonprop1", SqlSimpleTypes.varcharType(255), 30, null, null);
    private static Column testmonprop2ExpAggTest = createColumn("testmonprop2", SqlSimpleTypes.varcharType(255), 30, null, null);
    private static Column genderExpAggTest = createColumn("gender", SqlSimpleTypes.varcharType(255), 30, null, null);
    private static Column testUnitSalesExpAggTest = createColumn("test_unit_sales", SqlSimpleTypes.Sql99.integerType(), null, null, null);
    private static Column testStoreCostExpAggTest = createColumn("test_store_cost", SqlSimpleTypes.decimalType(18, 4), null, 10, 4);
    private static Column factCountExpAggTest = createColumn("fact_count", SqlSimpleTypes.Sql99.integerType(), null, null, null);

    private static Table expAggTest = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
    static {
    expAggTest.setName("exp_agg_test");
    expAggTest.getFeature().add(testyearExpAggTest);
    expAggTest.getFeature().add(testqtrExpAggTest);
    expAggTest.getFeature().add(testmonthordExpAggTest);
    expAggTest.getFeature().add(testmonthnameExpAggTest);
    expAggTest.getFeature().add(testmonthcapExpAggTest);
    expAggTest.getFeature().add(testmonprop1ExpAggTest);
    expAggTest.getFeature().add(testmonprop2ExpAggTest);
    expAggTest.getFeature().add(genderExpAggTest);
    expAggTest.getFeature().add(testUnitSalesExpAggTest);
    expAggTest.getFeature().add(testStoreCostExpAggTest);
    expAggTest.getFeature().add(factCountExpAggTest);
    }

    //## TableName:  exp_agg_test_distinct_count
    //## ColumnNames:  fact_count,testyear,gender,store_name,store_country,store_st,store_cty,store_add,unit_s,cust_cnt
    //## ColumnTypes: INTEGER,INTEGER,VARCHAR(30),VARCHAR(30),VARCHAR(30),VARCHAR(30),VARCHAR(30),VARCHAR(30),INTEGER,INTEGER
    private static Column factCountExpAggTestDistinctCount = createColumn("fact_count", SqlSimpleTypes.Sql99.integerType(), null, null, null);
    private static Column testyearExpAggTestDistinctCount = createColumn("testyear", SqlSimpleTypes.Sql99.integerType(), null, null, null);
    private static Column genderExpAggTestDistinctCount = createColumn("gender", SqlSimpleTypes.varcharType(255), 30, null, null);
    private static Column storeNameExpAggTestDistinctCount = createColumn("store_name", SqlSimpleTypes.varcharType(255), 30, null, null);
    private static Column storeCountryExpAggTestDistinctCount = createColumn("store_country", SqlSimpleTypes.varcharType(255), 30, null, null);
    private static Column storeStExpAggTestDistinctCount = createColumn("store_st", SqlSimpleTypes.varcharType(255), 30, null, null);
    private static Column storeCtyExpAggTestDistinctCount = createColumn("store_cty", SqlSimpleTypes.varcharType(255), 30, null, null);
    private static Column storeAddExpAggTestDistinctCount = createColumn("store_add", SqlSimpleTypes.varcharType(255), 30, null, null);
    private static Column unitSExpAggTestDistinctCount = createColumn("unit_s", SqlSimpleTypes.Sql99.integerType(), null, null, null);
    private static Column custCntExpAggTestDistinctCount = createColumn("cust_cnt", SqlSimpleTypes.Sql99.integerType(), null, null, null);

    private static Table expAggTestDistinctCount = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
    static {
        expAggTestDistinctCount.setName("exp_agg_test_distinct_count");
        expAggTestDistinctCount.getFeature().add(factCountExpAggTestDistinctCount);
        expAggTestDistinctCount.getFeature().add(testyearExpAggTestDistinctCount);
        expAggTestDistinctCount.getFeature().add(genderExpAggTestDistinctCount);
        expAggTestDistinctCount.getFeature().add(storeNameExpAggTestDistinctCount);
        expAggTestDistinctCount.getFeature().add(storeCountryExpAggTestDistinctCount);
        expAggTestDistinctCount.getFeature().add(storeStExpAggTestDistinctCount);
        expAggTestDistinctCount.getFeature().add(storeCtyExpAggTestDistinctCount);
        expAggTestDistinctCount.getFeature().add(storeAddExpAggTestDistinctCount);
        expAggTestDistinctCount.getFeature().add(unitSExpAggTestDistinctCount);
        expAggTestDistinctCount.getFeature().add(custCntExpAggTestDistinctCount);
    }
	@BeforeAll
	public static void beforeAll() {
	      ContextArgumentsProvider.dockerWasChanged = true;
	}

    @Override
	@BeforeEach
    public void beforeEach() {
        super.beforeEach();
    }

    @Override
	@AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

    @Override
    protected String getFileName() {
        return "explicit_aggs.csv";
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testExplicitAggExtraColsRequiringJoin(Context<?> context) throws SQLException {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);

        Catalog catalogMapping = new CatalogSupplier().get();
        EcoreUtil.Copier copier = org.opencube.junit5.EmfUtil.copier((CatalogImpl) catalogMapping);
        Catalog catalog = (Catalog) copier.get(catalogMapping);

        AggregationName aggName = AggregationFactory.eINSTANCE.createAggregationName();
        aggName.setName((Table) copier.get(CatalogSupplier.TABLE_AGG_G_MS_PCAT_SALES_FACT));

        AggregationColumnName factCount = AggregationFactory.eINSTANCE.createAggregationColumnName();
        factCount.setColumn((Column) copier.get(CatalogSupplier.COLUMN_FACT_COUNT_AGG_G_MS_PCAT_SALES_FACT_1997));
        aggName.setAggregationFactCount(factCount);

        AggregationMeasure unitSalesMeasure = AggregationFactory.eINSTANCE.createAggregationMeasure();
        unitSalesMeasure.setName("[Measures].[Unit Sales]");
        unitSalesMeasure.setColumn((Column) copier.get(CatalogSupplier.COLUMN_UNIT_SALES_AGG_G_MS_PCAT_SALES_FACT_1997));
        aggName.getAggregationMeasures().add(unitSalesMeasure);

        AggregationLevel genderLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        genderLevel.setName("[Gender].[Gender].[Gender]");
        genderLevel.setColumn((Column) copier.get(CatalogSupplier.COLUMN_GENDER_AGG_G_MS_PCAT_SALES_FACT_1997));
        aggName.getAggregationLevels().add(genderLevel);

        AggregationLevel yearLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        yearLevel.setName("[TimeExtra].[TimeExtra].[Year]");
        yearLevel.setColumn((Column) copier.get(CatalogSupplier.COLUMN_THE_YEAR_AGG_G_MS_PCAT_SALES_FACT_1997));
        aggName.getAggregationLevels().add(yearLevel);

        AggregationLevel quarterLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        quarterLevel.setName("[TimeExtra].[TimeExtra].[Quarter]");
        quarterLevel.setColumn((Column) copier.get(CatalogSupplier.COLUMN_QUARTER_AGG_G_MS_PCAT_SALES_FACT_1997));
        aggName.getAggregationLevels().add(quarterLevel);

        AggregationLevel monthLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        monthLevel.setName("[TimeExtra].[TimeExtra].[Month]");
        monthLevel.setColumn((Column) copier.get(CatalogSupplier.COLUMN_MONTH_YEAR_AGG_G_MS_PCAT_SALES_FACT_1997));
        aggName.getAggregationLevels().add(monthLevel);

        setupMultiColDimCube(catalog, copier, context,
                List.of(aggName),
                (Column) copier.get(CatalogSupplier.COLUMN_THE_YEAR_TIME_BY_DAY),
                (Column) copier.get(CatalogSupplier.COLUMN_QUARTER_TIME_BY_DAY),
                (Column) copier.get(CatalogSupplier.COLUMN_MONTH_OF_YEAR_TIME_BY_DAY),
                (Column) copier.get(CatalogSupplier.COLUMN_THE_MONTH_TIME_BY_DAY),
                (Column) copier.get(CatalogSupplier.COLUMN_MONTH_OF_YEAR_TIME_BY_DAY), null,
            List.of(), List.of(expAggTest, expAggTestDistinctCount));

        String query =
            "select {[Measures].[Unit Sales]} on columns, "
            + "non empty CrossJoin({[TimeExtra].[TimeExtra].[Month].members},{[Gender].[Gender].[M]}) on rows "
            + "from [ExtraCol] ";
        TestUtil.flushSchemaCache(context.getConnectionWithDefaultRole());
        assertQuerySql(
            context.getConnectionWithDefaultRole(),
            query,
            mysqlPattern(
                "select\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`the_year` as `c0`,\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`quarter` as `c1`,\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`month_of_year` as `c2`,\n"
                + "    `time_by_day`.`the_month` as `c3`,\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`gender` as `c4`\n"
                + "from\n"
                + "    `agg_g_ms_pcat_sales_fact_1997` as `agg_g_ms_pcat_sales_fact_1997`,\n"
                + "    `time_by_day` as `time_by_day`\n"
                + "where\n"
                + "    `time_by_day`.`month_of_year` = `agg_g_ms_pcat_sales_fact_1997`.`month_of_year`\n"
                + "and\n"
                + "    (`agg_g_ms_pcat_sales_fact_1997`.`gender` = 'M')\n"
                + "group by\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`the_year`,\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`quarter`,\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`month_of_year`,\n"
                + "    `time_by_day`.`the_month`,\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`gender`\n"
                + "order by\n"
                + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                    ? "    ISNULL(`c0`) ASC, `c0` ASC,\n"
                    + "    ISNULL(`c1`) ASC, `c1` ASC,\n"
                    + "    ISNULL(`c2`) ASC, `c2` ASC,\n"
                    + "    ISNULL(`c4`) ASC, `c4` ASC"
                    : "    ISNULL(`agg_g_ms_pcat_sales_fact_1997`.`the_year`) ASC, `agg_g_ms_pcat_sales_fact_1997`.`the_year` ASC,\n"
                    + "    ISNULL(`agg_g_ms_pcat_sales_fact_1997`.`quarter`) ASC, `agg_g_ms_pcat_sales_fact_1997`.`quarter` ASC,\n"
                    + "    ISNULL(`agg_g_ms_pcat_sales_fact_1997`.`month_of_year`) ASC, `agg_g_ms_pcat_sales_fact_1997`.`month_of_year` ASC,\n"
                    + "    ISNULL(`agg_g_ms_pcat_sales_fact_1997`.`gender`) ASC, `agg_g_ms_pcat_sales_fact_1997`.`gender` ASC")));
        assertQuerySql(
            context.getConnectionWithDefaultRole(),
            query,
            mysqlPattern(
                "select\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`the_year` as `c0`,\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`quarter` as `c1`,\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`month_of_year` as `c2`,\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`gender` as `c3`,\n"
                + "    sum(`agg_g_ms_pcat_sales_fact_1997`.`unit_sales`) as `m0`\n"
                + "from\n"
                + "    `agg_g_ms_pcat_sales_fact_1997` as `agg_g_ms_pcat_sales_fact_1997`\n"
                + "where\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`the_year` = 1997\n"
                + "and\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`gender` = 'M'\n"
                + "group by\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`the_year`,\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`quarter`,\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`month_of_year`,\n"
                + "    `agg_g_ms_pcat_sales_fact_1997`.`gender`"));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testExplicitForeignKey(Context<?> context) {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);
        Catalog catalogMapping = new CatalogSupplier().get();
        EcoreUtil.Copier copier = org.opencube.junit5.EmfUtil.copier((CatalogImpl) catalogMapping);
        Catalog catalog = (Catalog) copier.get(catalogMapping);

        AggregationName aggName = AggregationFactory.eINSTANCE.createAggregationName();
        aggName.setName(CatalogSupplier.TABLE_AGG_C_14_SALES_FACT);

        AggregationColumnName factCount = AggregationFactory.eINSTANCE.createAggregationColumnName();
        factCount.setColumn(CatalogSupplier.COLUMN_FACT_COUNT_AGG_C_14_SALES_FACT_1997);
        aggName.setAggregationFactCount(factCount);

        AggregationForeignKey foreignKey = AggregationFactory.eINSTANCE.createAggregationForeignKey();
        foreignKey.setFactColumn(CatalogSupplier.COLUMN_STORE_ID_SALESFACT);
        foreignKey.setAggregationColumn(CatalogSupplier.COLUMN_STORE_ID_AGG_C_14_SALES_FACT_1997);
        aggName.getAggregationForeignKeys().add(foreignKey);

        AggregationMeasure unitSalesMeasure = AggregationFactory.eINSTANCE.createAggregationMeasure();
        unitSalesMeasure.setName("[Measures].[Unit Sales]");
        unitSalesMeasure.setColumn(CatalogSupplier.COLUMN_UNIT_SALES_AGG_C_14_SALES_FACT_1997);
        aggName.getAggregationMeasures().add(unitSalesMeasure);

        AggregationMeasure storeCostMeasure = AggregationFactory.eINSTANCE.createAggregationMeasure();
        storeCostMeasure.setName("[Measures].[Store Cost]");
        storeCostMeasure.setColumn(CatalogSupplier.COLUMN_STORE_COST_AGG_C_14_SALES_FACT_1997);
        aggName.getAggregationMeasures().add(storeCostMeasure);

        AggregationLevel yearLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        yearLevel.setName("[TimeExtra].[TimeExtra].[Year]");
        yearLevel.setColumn(CatalogSupplier.COLUMN_THE_YEAR_AGG_C_14_SALES_FACT_1997);
        aggName.getAggregationLevels().add(yearLevel);

        AggregationLevel quarterLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        quarterLevel.setName("[TimeExtra].[TimeExtra].[Quarter]");
        quarterLevel.setColumn(CatalogSupplier.COLUMN_QUARTER_AGG_C_14_SALES_FACT_1997);
        aggName.getAggregationLevels().add(quarterLevel);

        AggregationLevel monthLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        monthLevel.setName("[TimeExtra].[TimeExtra].[Month]");
        monthLevel.setColumn(CatalogSupplier.COLUMN_MONTH_YEAR_AGG_C_14_SALES_FACT_1997);
        aggName.getAggregationLevels().add(monthLevel);

        setupMultiColDimCube(catalog, copier, context,
            List.of(aggName),
            CatalogSupplier.COLUMN_THE_YEAR_TIME_BY_DAY,
            CatalogSupplier.COLUMN_QUARTER_TIME_BY_DAY,
            CatalogSupplier.COLUMN_MONTH_OF_YEAR_TIME_BY_DAY, CatalogSupplier.COLUMN_THE_MONTH_TIME_BY_DAY, CatalogSupplier.COLUMN_MONTH_OF_YEAR_TIME_BY_DAY, null,
            List.of(), List.of(expAggTest, expAggTestDistinctCount));


        String query =
            "select {[Measures].[Unit Sales]} on columns, "
            + "non empty CrossJoin({[TimeExtra].[TimeExtra].[Month].members},{[Store].[Store].[Store Name].members}) on rows "
            + "from [ExtraCol] ";
        // Run the query twice, verifying both the SqlTupleReader and
        // Segment load queries.
        assertQuerySql(
            context.getConnectionWithDefaultRole(),
            query,
            mysqlPattern(
                "select\n"
                + "    `agg_c_14_sales_fact_1997`.`the_year` as `c0`,\n"
                + "    `agg_c_14_sales_fact_1997`.`quarter` as `c1`,\n"
                + "    `agg_c_14_sales_fact_1997`.`month_of_year` as `c2`,\n"
                + "    `time_by_day`.`the_month` as `c3`,\n"
                + "    `store`.`store_country` as `c4`,\n"
                + "    `store`.`store_state` as `c5`,\n"
                + "    `store`.`store_city` as `c6`,\n"
                + "    `store`.`store_name` as `c7`,\n"
                + "    `store`.`store_street_address` as `c8`\n"
                + "from\n"
                + "    `agg_c_14_sales_fact_1997` as `agg_c_14_sales_fact_1997`,\n"
                + "    `time_by_day` as `time_by_day`,\n"
                + "    `store` as `store`\n"
                + "where\n"
                + "    `time_by_day`.`month_of_year` = `agg_c_14_sales_fact_1997`.`month_of_year`\n"
                + "and\n"
                + "    `agg_c_14_sales_fact_1997`.`store_id` = `store`.`store_id`\n"
                + "group by\n"
                + "    `agg_c_14_sales_fact_1997`.`the_year`,\n"
                + "    `agg_c_14_sales_fact_1997`.`quarter`,\n"
                + "    `agg_c_14_sales_fact_1997`.`month_of_year`,\n"
                + "    `time_by_day`.`the_month`,\n"
                + "    `store`.`store_country`,\n"
                + "    `store`.`store_state`,\n"
                + "    `store`.`store_city`,\n"
                + "    `store`.`store_name`,\n"
                + "    `store`.`store_street_address`\n"
                + "order by\n"
                + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                    ? "    ISNULL(`c0`) ASC, `c0` ASC,\n"
                    + "    ISNULL(`c1`) ASC, `c1` ASC,\n"
                    + "    ISNULL(`c2`) ASC, `c2` ASC,\n"
                    + "    ISNULL(`c4`) ASC, `c4` ASC,\n"
                    + "    ISNULL(`c5`) ASC, `c5` ASC,\n"
                    + "    ISNULL(`c6`) ASC, `c6` ASC,\n"
                    + "    ISNULL(`c7`) ASC, `c7` ASC"
                    : "    ISNULL(`agg_c_14_sales_fact_1997`.`the_year`) ASC, `agg_c_14_sales_fact_1997`.`the_year` ASC,\n"
                    + "    ISNULL(`agg_c_14_sales_fact_1997`.`quarter`) ASC, `agg_c_14_sales_fact_1997`.`quarter` ASC,\n"
                    + "    ISNULL(`agg_c_14_sales_fact_1997`.`month_of_year`) ASC, `agg_c_14_sales_fact_1997`.`month_of_year` ASC,\n"
                    + "    ISNULL(`store`.`store_country`) ASC, `store`.`store_country` ASC,\n"
                    + "    ISNULL(`store`.`store_state`) ASC, `store`.`store_state` ASC,\n"
                    + "    ISNULL(`store`.`store_city`) ASC, `store`.`store_city` ASC,\n"
                    + "    ISNULL(`store`.`store_name`) ASC, `store`.`store_name` ASC")));

        assertQuerySql(
            context.getConnectionWithDefaultRole(),
            query,
            mysqlPattern(
                "select\n"
                + "    `agg_c_14_sales_fact_1997`.`the_year` as `c0`,\n"
                + "    `agg_c_14_sales_fact_1997`.`quarter` as `c1`,\n"
                + "    `agg_c_14_sales_fact_1997`.`month_of_year` as `c2`,\n"
                + "    `store`.`store_name` as `c3`,\n"
                + "    sum(`agg_c_14_sales_fact_1997`.`unit_sales`) as `m0`\n"
                + "from\n"
                + "    `agg_c_14_sales_fact_1997` as `agg_c_14_sales_fact_1997`,\n"
                + "    `store` as `store`\n"
                + "where\n"
                + "    `agg_c_14_sales_fact_1997`.`the_year` = 1997\n"
                + "and\n"
                + "    `agg_c_14_sales_fact_1997`.`store_id` = `store`.`store_id`\n"
                + "group by\n"
                + "    `agg_c_14_sales_fact_1997`.`the_year`,\n"
                + "    `agg_c_14_sales_fact_1997`.`quarter`,\n"
                + "    `agg_c_14_sales_fact_1997`.`month_of_year`,\n"
                + "    `store`.`store_name`"));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testExplicitAggOrdinalOnAggTable(Context<?> context) throws SQLException {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);
        Catalog catalogMapping = new CatalogSupplier().get();
        EcoreUtil.Copier copier = org.opencube.junit5.EmfUtil.copier((CatalogImpl) catalogMapping);
        Catalog catalog = (Catalog) copier.get(catalogMapping);

        AggregationName aggName = AggregationFactory.eINSTANCE.createAggregationName();
        aggName.setName(expAggTest);

        AggregationColumnName factCount = AggregationFactory.eINSTANCE.createAggregationColumnName();
        factCount.setColumn((Column) factCountExpAggTest);
        aggName.setAggregationFactCount(factCount);

        AggregationMeasure unitSalesMeasure = AggregationFactory.eINSTANCE.createAggregationMeasure();
        unitSalesMeasure.setName("[Measures].[Unit Sales]");
        unitSalesMeasure.setColumn((Column) testUnitSalesExpAggTest);
        aggName.getAggregationMeasures().add(unitSalesMeasure);

        AggregationLevel genderLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        genderLevel.setName("[Gender].[Gender].[Gender]");
        genderLevel.setColumn((Column) genderExpAggTest);
        aggName.getAggregationLevels().add(genderLevel);

        AggregationLevel yearLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        yearLevel.setName("[TimeExtra].[TimeExtra].[Year]");
        yearLevel.setColumn((Column) testyearExpAggTest);
        aggName.getAggregationLevels().add(yearLevel);

        AggregationLevel quarterLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        quarterLevel.setName("[TimeExtra].[TimeExtra].[Quarter]");
        quarterLevel.setColumn((Column) testqtrExpAggTest);
        aggName.getAggregationLevels().add(quarterLevel);

        AggregationLevel monthLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        monthLevel.setName("[TimeExtra].[TimeExtra].[Month]");
        monthLevel.setColumn((Column) testmonthnameExpAggTest);
        monthLevel.getOrdinalColumns().addAll(List.of((Column) testmonthordExpAggTest));
        aggName.getAggregationLevels().add(monthLevel);

        setupMultiColDimCube(catalog, copier, context,
            List.of(aggName),
            (Column)copier.get(CatalogSupplier.COLUMN_THE_YEAR_TIME_BY_DAY),
            (Column)copier.get(CatalogSupplier.COLUMN_QUARTER_TIME_BY_DAY),
            (Column)copier.get(CatalogSupplier.COLUMN_THE_MONTH_TIME_BY_DAY), null, (Column)copier.get(CatalogSupplier.COLUMN_MONTH_OF_YEAR_TIME_BY_DAY), null,
            List.of(), List.of(expAggTest, expAggTestDistinctCount));

        String query =
            "select {[Measures].[Unit Sales]} on columns, "
            + "non empty CrossJoin({[TimeExtra].[TimeExtra].[Month].members},{[Gender].[Gender].[M]}) on rows "
            + "from [ExtraCol] ";

        assertQuerySql(
            context.getConnectionWithDefaultRole(),
            query,
            mysqlPattern(
                "select\n"
                + "    `exp_agg_test`.`testyear` as `c0`,\n"
                + "    `exp_agg_test`.`testqtr` as `c1`,\n"
                + "    `exp_agg_test`.`testmonthname` as `c2`,\n"
                + "    `exp_agg_test`.`testmonthord` as `c3`,\n"
                + "    `exp_agg_test`.`gender` as `c4`\n"
                + "from\n"
                + "    `exp_agg_test` as `exp_agg_test`\n"
                + "where\n"
                + "    (`exp_agg_test`.`gender` = 'M')\n"
                + "group by\n"
                + "    `exp_agg_test`.`testyear`,\n"
                + "    `exp_agg_test`.`testqtr`,\n"
                + "    `exp_agg_test`.`testmonthname`,\n"
                + "    `exp_agg_test`.`testmonthord`,\n"
                + "    `exp_agg_test`.`gender`\n"
                + "order by\n"
                + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                    ? "    ISNULL(`c0`) ASC, `c0` ASC,\n"
                    + "    ISNULL(`c1`) ASC, `c1` ASC,\n"
                    + "    ISNULL(`c3`) ASC, `c3` ASC,\n"
                    + "    ISNULL(`c2`) ASC, `c2` ASC,\n"
                    + "    ISNULL(`c4`) ASC, `c4` ASC"
                    : "    ISNULL(`exp_agg_test`.`testyear`) ASC, `exp_agg_test`.`testyear` ASC,\n"
                    + "    ISNULL(`exp_agg_test`.`testqtr`) ASC, `exp_agg_test`.`testqtr` ASC,\n"
                    + "    ISNULL(`exp_agg_test`.`testmonthord`) ASC, `exp_agg_test`.`testmonthord` ASC,\n"
                    + "    ISNULL(`exp_agg_test`.`testmonthname`) ASC, `exp_agg_test`.`testmonthname` ASC,\n"
                    + "    ISNULL(`exp_agg_test`.`gender`) ASC, `exp_agg_test`.`gender` ASC")));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testExplicitAggCaptionOnAggTable(Context<?> context) throws SQLException {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);
        Catalog catalogMapping = new CatalogSupplier().get();
        EcoreUtil.Copier copier = org.opencube.junit5.EmfUtil.copier((CatalogImpl) catalogMapping);
        Catalog catalog = (Catalog) copier.get(catalogMapping);

        AggregationName aggName = AggregationFactory.eINSTANCE.createAggregationName();
        aggName.setName(expAggTest);

        AggregationColumnName factCount = AggregationFactory.eINSTANCE.createAggregationColumnName();
        factCount.setColumn((Column) factCountExpAggTest);
        aggName.setAggregationFactCount(factCount);

        AggregationMeasure unitSalesMeasure = AggregationFactory.eINSTANCE.createAggregationMeasure();
        unitSalesMeasure.setName("[Measures].[Unit Sales]");
        unitSalesMeasure.setColumn((Column) testUnitSalesExpAggTest);
        aggName.getAggregationMeasures().add(unitSalesMeasure);

        AggregationLevel genderLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        genderLevel.setName("[Gender].[Gender].[Gender]");
        genderLevel.setColumn((Column) genderExpAggTest);
        aggName.getAggregationLevels().add(genderLevel);

        AggregationLevel yearLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        yearLevel.setName("[TimeExtra].[TimeExtra].[Year]");
        yearLevel.setColumn((Column) testyearExpAggTest);
        aggName.getAggregationLevels().add(yearLevel);

        AggregationLevel quarterLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        quarterLevel.setName("[TimeExtra].[TimeExtra].[Quarter]");
        quarterLevel.setColumn((Column) testqtrExpAggTest);
        aggName.getAggregationLevels().add(quarterLevel);

        AggregationLevel monthLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        monthLevel.setName("[TimeExtra].[TimeExtra].[Month]");
        monthLevel.setColumn((Column) testmonthnameExpAggTest);
        monthLevel.setCaptionColumn((Column) testmonthcapExpAggTest);
        aggName.getAggregationLevels().add(monthLevel);

        setupMultiColDimCube(catalog, copier, context,
            List.of(aggName),
            (Column)copier.get(CatalogSupplier.COLUMN_THE_YEAR_TIME_BY_DAY),
            (Column)copier.get(CatalogSupplier.COLUMN_QUARTER_TIME_BY_DAY),
            (Column)copier.get(CatalogSupplier.COLUMN_THE_MONTH_TIME_BY_DAY), (Column)copier.get(CatalogSupplier.COLUMN_MONTH_OF_YEAR_TIME_BY_DAY), null, null,
            List.of(), List.of(expAggTest, expAggTestDistinctCount));

        String query =
            "select {[Measures].[Unit Sales]} on columns, "
            + "non empty CrossJoin({[TimeExtra].[TimeExtra].[Month].members},{[Gender].[Gender].[M]}) on rows "
            + "from [ExtraCol] ";

        assertQuerySql(
            context.getConnectionWithDefaultRole(),
            query,
            mysqlPattern(
                "select\n"
                + "    `exp_agg_test`.`testyear` as `c0`,\n"
                + "    `exp_agg_test`.`testqtr` as `c1`,\n"
                + "    `exp_agg_test`.`testmonthname` as `c2`,\n"
                + "    `exp_agg_test`.`testmonthcap` as `c3`,\n"
                + "    `exp_agg_test`.`gender` as `c4`\n"
                + "from\n"
                + "    `exp_agg_test` as `exp_agg_test`\n"
                + "where\n"
                + "    (`exp_agg_test`.`gender` = 'M')\n"
                + "group by\n"
                + "    `exp_agg_test`.`testyear`,\n"
                + "    `exp_agg_test`.`testqtr`,\n"
                + "    `exp_agg_test`.`testmonthname`,\n"
                + "    `exp_agg_test`.`testmonthcap`,\n"
                + "    `exp_agg_test`.`gender`\n"
                + "order by\n"
                + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                    ? "    ISNULL(`c0`) ASC, `c0` ASC,\n"
                    + "    ISNULL(`c1`) ASC, `c1` ASC,\n"
                    + "    ISNULL(`c2`) ASC, `c2` ASC,\n"
                    + "    ISNULL(`c4`) ASC, `c4` ASC"
                    : "    ISNULL(`exp_agg_test`.`testyear`) ASC, `exp_agg_test`.`testyear` ASC,\n"
                    + "    ISNULL(`exp_agg_test`.`testqtr`) ASC, `exp_agg_test`.`testqtr` ASC,\n"
                    + "    ISNULL(`exp_agg_test`.`testmonthname`) ASC, `exp_agg_test`.`testmonthname` ASC,\n"
                    + "    ISNULL(`exp_agg_test`.`gender`) ASC, `exp_agg_test`.`gender` ASC")));
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testExplicitAggNameColumnOnAggTable(Context<?> context) throws SQLException {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);
        Catalog catalogMapping = new CatalogSupplier().get();
        EcoreUtil.Copier copier = org.opencube.junit5.EmfUtil.copier((CatalogImpl) catalogMapping);
        Catalog catalog = (Catalog) copier.get(catalogMapping);

        AggregationName aggName = AggregationFactory.eINSTANCE.createAggregationName();
        aggName.setName(expAggTest);

        AggregationColumnName factCount = AggregationFactory.eINSTANCE.createAggregationColumnName();
        factCount.setColumn((Column) factCountExpAggTest);
        aggName.setAggregationFactCount(factCount);

        AggregationMeasure unitSalesMeasure = AggregationFactory.eINSTANCE.createAggregationMeasure();
        unitSalesMeasure.setName("[Measures].[Unit Sales]");
        unitSalesMeasure.setColumn((Column) testUnitSalesExpAggTest);
        aggName.getAggregationMeasures().add(unitSalesMeasure);

        AggregationLevel genderLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        genderLevel.setName("[Gender].[Gender]");
        genderLevel.setColumn((Column) genderExpAggTest);
        aggName.getAggregationLevels().add(genderLevel);

        AggregationLevel yearLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        yearLevel.setName("[TimeExtra].[Year]");
        yearLevel.setColumn((Column) testyearExpAggTest);
        aggName.getAggregationLevels().add(yearLevel);

        AggregationLevel quarterLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        quarterLevel.setName("[TimeExtra].[Quarter]");
        quarterLevel.setColumn((Column) testqtrExpAggTest);
        aggName.getAggregationLevels().add(quarterLevel);

        AggregationLevel monthLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        monthLevel.setName("[TimeExtra].[Month]");
        monthLevel.setColumn((Column) testmonthnameExpAggTest);
        monthLevel.setNameColumn((Column) testmonthcapExpAggTest);

        AggregationLevelProperty property = AggregationFactory.eINSTANCE.createAggregationLevelProperty();
        property.setName("aProperty");
        property.setColumn((Column) testmonprop1ExpAggTest);
        monthLevel.getAggregationLevelProperties().add(property);

        aggName.getAggregationLevels().add(monthLevel);

        MemberProperty memberProperty = LevelFactory.eINSTANCE.createMemberProperty();
        memberProperty.setName("aProperty");
        memberProperty.setColumn((Column) CatalogSupplier.COLUMN_FISCAL_PERIOD_TIME_BY_DAY);

        setupMultiColDimCube(catalog, copier, context,
            List.of(aggName),
            (Column)copier.get(CatalogSupplier.COLUMN_THE_YEAR_TIME_BY_DAY),
            (Column)copier.get(CatalogSupplier.COLUMN_QUARTER_TIME_BY_DAY),
            (Column)copier.get(CatalogSupplier.COLUMN_THE_MONTH_TIME_BY_DAY), null, null, (Column)copier.get(CatalogSupplier.COLUMN_MONTH_OF_YEAR_TIME_BY_DAY),
            List.of(memberProperty), List.of(expAggTest, expAggTestDistinctCount));

        String query =
            "select {[Measures].[Unit Sales]} on columns, "
            + "non empty CrossJoin({[TimeExtra].[Month].members},{[Gender].[M]}) on rows "
            + "from [ExtraCol] ";

        assertQuerySql(
            context.getConnectionWithDefaultRole(),
            query,
            mysqlPattern(
                "select\n"
                + "    `exp_agg_test`.`testyear` as `c0`,\n"
                + "    `exp_agg_test`.`testqtr` as `c1`,\n"
                + "    `exp_agg_test`.`testmonthname` as `c2`,\n"
                + "    `exp_agg_test`.`testmonthcap` as `c3`,\n"
                + "    `exp_agg_test`.`testmonprop1` as `c4`,\n"
                + "    `exp_agg_test`.`gender` as `c5`\n"
                + "from\n"
                + "    `exp_agg_test` as `exp_agg_test`\n"
                + "where\n"
                + "    (`exp_agg_test`.`gender` = 'M')\n"
                + "group by\n"
                + "    `exp_agg_test`.`testyear`,\n"
                + "    `exp_agg_test`.`testqtr`,\n"
                + "    `exp_agg_test`.`testmonthname`,\n"
                + "    `exp_agg_test`.`testmonthcap`,\n"
                + "    `exp_agg_test`.`testmonprop1`,\n"
                + "    `exp_agg_test`.`gender`\n"
                + "order by\n"
                + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                    ? "    ISNULL(`c0`) ASC, `c0` ASC,\n"
                    + "    ISNULL(`c1`) ASC, `c1` ASC,\n"
                    + "    ISNULL(`c2`) ASC, `c2` ASC,\n"
                    + "    ISNULL(`c5`) ASC, `c5` ASC"
                    : "    ISNULL(`exp_agg_test`.`testyear`) ASC, `exp_agg_test`.`testyear` ASC,\n"
                    + "    ISNULL(`exp_agg_test`.`testqtr`) ASC, `exp_agg_test`.`testqtr` ASC,\n"
                    + "    ISNULL(`exp_agg_test`.`testmonthname`) ASC, `exp_agg_test`.`testmonthname` ASC,\n"
                    + "    ISNULL(`exp_agg_test`.`gender`) ASC, `exp_agg_test`.`gender` ASC")));
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testExplicitAggPropertiesOnAggTable(Context<?> context) throws SQLException {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);
        Catalog catalogMapping = new CatalogSupplier().get();
        EcoreUtil.Copier copier = org.opencube.junit5.EmfUtil.copier((CatalogImpl) catalogMapping);
        Catalog catalog = (Catalog) copier.get(catalogMapping);

        AggregationName aggName = AggregationFactory.eINSTANCE.createAggregationName();
        aggName.setName(expAggTestDistinctCount);

        AggregationColumnName factCount = AggregationFactory.eINSTANCE.createAggregationColumnName();
        factCount.setColumn(factCountExpAggTestDistinctCount);
        aggName.setAggregationFactCount(factCount);

        AggregationMeasure unitSalesMeasure = AggregationFactory.eINSTANCE.createAggregationMeasure();
        unitSalesMeasure.setName("[Measures].[Unit Sales]");
        unitSalesMeasure.setColumn(unitSExpAggTestDistinctCount);
        aggName.getAggregationMeasures().add(unitSalesMeasure);

        AggregationMeasure customerCountMeasure = AggregationFactory.eINSTANCE.createAggregationMeasure();
        customerCountMeasure.setName("[Measures].[Customer Count]");
        customerCountMeasure.setColumn(custCntExpAggTestDistinctCount);
        aggName.getAggregationMeasures().add(customerCountMeasure);

        AggregationLevel yearLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        yearLevel.setName("[TimeExtra].[TimeExtra].[Year]");
        yearLevel.setColumn(testyearExpAggTestDistinctCount);
        aggName.getAggregationLevels().add(yearLevel);

        AggregationLevel genderLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        genderLevel.setName("[Gender].[Gender].[Gender]");
        genderLevel.setColumn(genderExpAggTestDistinctCount);
        aggName.getAggregationLevels().add(genderLevel);

        AggregationLevel storeCountryLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        storeCountryLevel.setName("[Store].[Store].[Store Country]");
        storeCountryLevel.setColumn(storeCountryExpAggTestDistinctCount);
        aggName.getAggregationLevels().add(storeCountryLevel);

        AggregationLevel storeStateLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        storeStateLevel.setName("[Store].[Store].[Store State]");
        storeStateLevel.setColumn(storeStExpAggTestDistinctCount);
        aggName.getAggregationLevels().add(storeStateLevel);

        AggregationLevel storeCityLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        storeCityLevel.setName("[Store].[Store].[Store City]");
        storeCityLevel.setColumn(storeCtyExpAggTestDistinctCount);
        aggName.getAggregationLevels().add(storeCityLevel);

        AggregationLevel storeNameLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        storeNameLevel.setName("[Store].[Store].[Store Name]");
        storeNameLevel.setColumn(storeNameExpAggTestDistinctCount);

        AggregationLevelProperty streetAddressProperty = AggregationFactory.eINSTANCE.createAggregationLevelProperty();
        streetAddressProperty.setName("Street address");
        streetAddressProperty.setColumn(storeAddExpAggTestDistinctCount);
        storeNameLevel.getAggregationLevelProperties().add(streetAddressProperty);

        aggName.getAggregationLevels().add(storeNameLevel);

        setupMultiColDimCube(catalog, copier, context,
            List.of(aggName),
            (Column)copier.get(CatalogSupplier.COLUMN_THE_YEAR_TIME_BY_DAY),
            (Column)copier.get(CatalogSupplier.COLUMN_QUARTER_TIME_BY_DAY),
            (Column)copier.get(CatalogSupplier.COLUMN_MONTH_OF_YEAR_TIME_BY_DAY), (Column)copier.get(CatalogSupplier.COLUMN_THE_MONTH_TIME_BY_DAY),
            CatalogSupplier.COLUMN_MONTH_OF_YEAR_TIME_BY_DAY, null,
            List.of(), List.of(expAggTest, expAggTestDistinctCount));

        String query =
            "with member measures.propVal as 'Store.Store.CurrentMember.Properties(\"Street Address\")'"
            + "select { measures.[propVal], measures.[Customer Count], [Measures].[Unit Sales]} on columns, "
            + "non empty CrossJoin({[Gender].[Gender].Gender.members},{[Store].[Store].[USA].[WA].[Spokane].[Store 16]}) on rows "
            + "from [ExtraCol]";
        assertQuerySql(
            context.getConnectionWithDefaultRole(),
            query,
            mysqlPattern(
                "select\n"
                + "    `exp_agg_test_distinct_count`.`gender` as `c0`,\n"
                + "    `exp_agg_test_distinct_count`.`store_country` as `c1`,\n"
                + "    `exp_agg_test_distinct_count`.`store_st` as `c2`,\n"
                + "    `exp_agg_test_distinct_count`.`store_cty` as `c3`,\n"
                + "    `exp_agg_test_distinct_count`.`store_name` as `c4`,\n"
                + "    `exp_agg_test_distinct_count`.`store_add` as `c5`\n"
                + "from\n"
                + "    `exp_agg_test_distinct_count` as `exp_agg_test_distinct_count`\n"
                + "where\n"
                + "    (`exp_agg_test_distinct_count`.`store_name` = 'Store 16')\n"
                + "group by\n"
                + "    `exp_agg_test_distinct_count`.`gender`,\n"
                + "    `exp_agg_test_distinct_count`.`store_country`,\n"
                + "    `exp_agg_test_distinct_count`.`store_st`,\n"
                + "    `exp_agg_test_distinct_count`.`store_cty`,\n"
                + "    `exp_agg_test_distinct_count`.`store_name`,\n"
                + "    `exp_agg_test_distinct_count`.`store_add`\n"
                + "order by\n"
                + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                    ? "    ISNULL(`c0`) ASC, `c0` ASC,\n"
                    + "    ISNULL(`c1`) ASC, `c1` ASC,\n"
                    + "    ISNULL(`c2`) ASC, `c2` ASC,\n"
                    + "    ISNULL(`c3`) ASC, `c3` ASC,\n"
                    + "    ISNULL(`c4`) ASC, `c4` ASC"
                    : "    ISNULL(`exp_agg_test_distinct_count`.`gender`) ASC, `exp_agg_test_distinct_count`.`gender` ASC,\n"
                    + "    ISNULL(`exp_agg_test_distinct_count`.`store_country`) ASC, `exp_agg_test_distinct_count`.`store_country` ASC,\n"
                    + "    ISNULL(`exp_agg_test_distinct_count`.`store_st`) ASC, `exp_agg_test_distinct_count`.`store_st` ASC,\n"
                    + "    ISNULL(`exp_agg_test_distinct_count`.`store_cty`) ASC, `exp_agg_test_distinct_count`.`store_cty` ASC,\n"
                    + "    ISNULL(`exp_agg_test_distinct_count`.`store_name`) ASC, `exp_agg_test_distinct_count`.`store_name` ASC")));

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "Store Address Property should be '5922 La Salle Ct'",
            query,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[propVal]}\n"
            + "{[Measures].[Customer Count]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Gender].[Gender].[F], [Store].[Store].[USA].[WA].[Spokane].[Store 16]}\n"
            + "{[Gender].[Gender].[M], [Store].[Store].[USA].[WA].[Spokane].[Store 16]}\n"
            + "Row #0: 5922 La Salle Ct\n"
            + "Row #0: 45\n"
            + "Row #0: 12,068\n"
            + "Row #1: 5922 La Salle Ct\n"
            + "Row #1: 39\n"
            + "Row #1: 11,523\n");
        // Should use agg table for distinct count measure
        assertQuerySql(
            context.getConnectionWithDefaultRole(),
            query,
            mysqlPattern(
                "select\n"
                + "    `exp_agg_test_distinct_count`.`testyear` as `c0`,\n"
                + "    `exp_agg_test_distinct_count`.`gender` as `c1`,\n"
                + "    `exp_agg_test_distinct_count`.`store_name` as `c2`,\n"
                + "    `exp_agg_test_distinct_count`.`unit_s` as `m0`,\n"
                + "    `exp_agg_test_distinct_count`.`cust_cnt` as `m1`\n"
                + "from\n"
                + "    `exp_agg_test_distinct_count` as `exp_agg_test_distinct_count`\n"
                + "where\n"
                + "    `exp_agg_test_distinct_count`.`testyear` = 1997\n"
                + "and\n"
                + "    `exp_agg_test_distinct_count`.`store_name` = 'Store 16'"));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testCountDistinctAllowableRollup(Context<?> context) throws SQLException {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);
        Catalog catalogMapping = new CatalogSupplier().get();
        EcoreUtil.Copier copier = org.opencube.junit5.EmfUtil.copier((CatalogImpl) catalogMapping);
        Catalog catalog = (Catalog) copier.get(catalogMapping);

        AggregationName aggName = AggregationFactory.eINSTANCE.createAggregationName();
        aggName.setName(expAggTestDistinctCount);

        AggregationColumnName factCount = AggregationFactory.eINSTANCE.createAggregationColumnName();
        factCount.setColumn(factCountExpAggTestDistinctCount);
        aggName.setAggregationFactCount(factCount);

        AggregationMeasure unitSalesMeasure = AggregationFactory.eINSTANCE.createAggregationMeasure();
        unitSalesMeasure.setName("[Measures].[Unit Sales]");
        unitSalesMeasure.setColumn(unitSExpAggTestDistinctCount);
        aggName.getAggregationMeasures().add(unitSalesMeasure);

        AggregationMeasure customerCountMeasure = AggregationFactory.eINSTANCE.createAggregationMeasure();
        customerCountMeasure.setName("[Measures].[Customer Count]");
        customerCountMeasure.setColumn(custCntExpAggTestDistinctCount);
        aggName.getAggregationMeasures().add(customerCountMeasure);

        AggregationLevel yearLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        yearLevel.setName("[TimeExtra].[TimeExtra].[Year]");
        yearLevel.setColumn(testyearExpAggTestDistinctCount);
        aggName.getAggregationLevels().add(yearLevel);

        AggregationLevel genderLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        genderLevel.setName("[Gender].[Gender].[Gender]");
        genderLevel.setColumn(genderExpAggTestDistinctCount);
        aggName.getAggregationLevels().add(genderLevel);

        AggregationLevel storeCountryLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        storeCountryLevel.setName("[Store].[Store].[Store Country]");
        storeCountryLevel.setColumn(storeCountryExpAggTestDistinctCount);
        aggName.getAggregationLevels().add(storeCountryLevel);

        AggregationLevel storeStateLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        storeStateLevel.setName("[Store].[Store].[Store State]");
        storeStateLevel.setColumn(storeStExpAggTestDistinctCount);
        aggName.getAggregationLevels().add(storeStateLevel);

        AggregationLevel storeCityLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        storeCityLevel.setName("[Store].[Store].[Store City]");
        storeCityLevel.setColumn(storeCtyExpAggTestDistinctCount);
        aggName.getAggregationLevels().add(storeCityLevel);

        AggregationLevel storeNameLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        storeNameLevel.setName("[Store].[Store].[Store Name]");
        storeNameLevel.setColumn(storeNameExpAggTestDistinctCount);

        AggregationLevelProperty streetAddressProperty = AggregationFactory.eINSTANCE.createAggregationLevelProperty();
        streetAddressProperty.setName("Street address");
        streetAddressProperty.setColumn(storeAddExpAggTestDistinctCount);
        storeNameLevel.getAggregationLevelProperties().add(streetAddressProperty);

        aggName.getAggregationLevels().add(storeNameLevel);

        setupMultiColDimCube(catalog, copier, context,
            List.of(aggName),
            (Column)copier.get(CatalogSupplier.COLUMN_THE_YEAR_TIME_BY_DAY),
            (Column)copier.get(CatalogSupplier.COLUMN_QUARTER_TIME_BY_DAY),
            (Column)copier.get(CatalogSupplier.COLUMN_MONTH_OF_YEAR_TIME_BY_DAY),
            (Column)copier.get(CatalogSupplier.COLUMN_THE_YEAR_TIME_BY_DAY), (Column)copier.get(CatalogSupplier.COLUMN_MONTH_OF_YEAR_TIME_BY_DAY), null,
            List.of(), List.of(expAggTest, expAggTestDistinctCount), "Customer Count");

        // Query brings in Year and Store Name, omitting Gender.
        // It's okay to roll up the agg table in this case
        // since Customer Count is dependent on Gender.
        String query =
            "select { measures.[Customer Count], [Measures].[Unit Sales]} on columns, "
            + "non empty CrossJoin({[TimeExtra].[TimeExtra].Year.members},{[Store].[Store].[USA].[WA].[Spokane].[Store 16]}) on rows "
            + "from [ExtraCol]";

        assertQuerySql(
            context.getConnectionWithDefaultRole(),
            query,
            mysqlPattern(
                "select\n"
                + "    `exp_agg_test_distinct_count`.`testyear` as `c0`,\n"
                + "    `exp_agg_test_distinct_count`.`store_country` as `c1`,\n"
                + "    `exp_agg_test_distinct_count`.`store_st` as `c2`,\n"
                + "    `exp_agg_test_distinct_count`.`store_cty` as `c3`,\n"
                + "    `exp_agg_test_distinct_count`.`store_name` as `c4`,\n"
                + "    `exp_agg_test_distinct_count`.`store_add` as `c5`\n"
                + "from\n"
                + "    `exp_agg_test_distinct_count` as `exp_agg_test_distinct_count`\n"
                + "where\n"
                + "    (`exp_agg_test_distinct_count`.`store_name` = 'Store 16')\n"
                + "group by\n"
                + "    `exp_agg_test_distinct_count`.`testyear`,\n"
                + "    `exp_agg_test_distinct_count`.`store_country`,\n"
                + "    `exp_agg_test_distinct_count`.`store_st`,\n"
                + "    `exp_agg_test_distinct_count`.`store_cty`,\n"
                + "    `exp_agg_test_distinct_count`.`store_name`,\n"
                + "    `exp_agg_test_distinct_count`.`store_add`\n"
                + "order by\n"
                + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                    ? "    ISNULL(`c0`) ASC, `c0` ASC,\n"
                    + "    ISNULL(`c1`) ASC, `c1` ASC,\n"
                    + "    ISNULL(`c2`) ASC, `c2` ASC,\n"
                    + "    ISNULL(`c3`) ASC, `c3` ASC,\n"
                    + "    ISNULL(`c4`) ASC, `c4` ASC"
                    : "    ISNULL(`exp_agg_test_distinct_count`.`testyear`) ASC, `exp_agg_test_distinct_count`.`testyear` ASC,\n"
                    + "    ISNULL(`exp_agg_test_distinct_count`.`store_country`) ASC, `exp_agg_test_distinct_count`.`store_country` ASC,\n"
                    + "    ISNULL(`exp_agg_test_distinct_count`.`store_st`) ASC, `exp_agg_test_distinct_count`.`store_st` ASC,\n"
                    + "    ISNULL(`exp_agg_test_distinct_count`.`store_cty`) ASC, `exp_agg_test_distinct_count`.`store_cty` ASC,\n"
                    + "    ISNULL(`exp_agg_test_distinct_count`.`store_name`) ASC, `exp_agg_test_distinct_count`.`store_name` ASC")));

        assertQuerySql(
            context.getConnectionWithDefaultRole(),
            query,
            mysqlPattern(
                "select\n"
                + "    `exp_agg_test_distinct_count`.`testyear` as `c0`,\n"
                + "    `exp_agg_test_distinct_count`.`store_name` as `c1`,\n"
                + "    sum(`exp_agg_test_distinct_count`.`unit_s`) as `m0`,\n"
                + "    sum(`exp_agg_test_distinct_count`.`cust_cnt`) as `m1`\n"
                + "from\n"
                + "    `exp_agg_test_distinct_count` as `exp_agg_test_distinct_count`\n"
                + "where\n"
                + "    `exp_agg_test_distinct_count`.`testyear` = 1997\n"
                + "and\n"
                + "    `exp_agg_test_distinct_count`.`store_name` = 'Store 16'\n"
                + "group by\n"
                + "    `exp_agg_test_distinct_count`.`testyear`,\n"
                + "    `exp_agg_test_distinct_count`.`store_name`"));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testCountDisallowedRollup(Context<?> context) throws SQLException {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);
        Catalog catalogMapping = new CatalogSupplier().get();
        EcoreUtil.Copier copier = org.opencube.junit5.EmfUtil.copier((CatalogImpl) catalogMapping);
        Catalog catalog = (Catalog) copier.get(catalogMapping);

        AggregationName aggName = AggregationFactory.eINSTANCE.createAggregationName();
        aggName.setName(expAggTestDistinctCount);

        AggregationColumnName factCount = AggregationFactory.eINSTANCE.createAggregationColumnName();
        factCount.setColumn(factCountExpAggTestDistinctCount);
        aggName.setAggregationFactCount(factCount);

        AggregationMeasure unitSalesMeasure = AggregationFactory.eINSTANCE.createAggregationMeasure();
        unitSalesMeasure.setName("[Measures].[Unit Sales]");
        unitSalesMeasure.setColumn(unitSExpAggTestDistinctCount);
        aggName.getAggregationMeasures().add(unitSalesMeasure);

        AggregationMeasure customerCountMeasure = AggregationFactory.eINSTANCE.createAggregationMeasure();
        customerCountMeasure.setName("[Measures].[Customer Count]");
        customerCountMeasure.setColumn(custCntExpAggTestDistinctCount);
        aggName.getAggregationMeasures().add(customerCountMeasure);

        AggregationLevel yearLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        yearLevel.setName("[TimeExtra].[TimeExtra].[Year]");
        yearLevel.setColumn(testyearExpAggTestDistinctCount);
        aggName.getAggregationLevels().add(yearLevel);

        AggregationLevel genderLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        genderLevel.setName("[Gender].[Gender].[Gender]");
        genderLevel.setColumn(genderExpAggTestDistinctCount);
        aggName.getAggregationLevels().add(genderLevel);

        AggregationLevel storeCountryLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        storeCountryLevel.setName("[Store].[Store].[Store Country]");
        storeCountryLevel.setColumn(storeCountryExpAggTestDistinctCount);
        aggName.getAggregationLevels().add(storeCountryLevel);

        AggregationLevel storeStateLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        storeStateLevel.setName("[Store].[Store].[Store State]");
        storeStateLevel.setColumn(storeStExpAggTestDistinctCount);
        aggName.getAggregationLevels().add(storeStateLevel);

        AggregationLevel storeCityLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        storeCityLevel.setName("[Store].[Store].[Store City]");
        storeCityLevel.setColumn(storeCtyExpAggTestDistinctCount);
        aggName.getAggregationLevels().add(storeCityLevel);

        AggregationLevel storeNameLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        storeNameLevel.setName("[Store].[Store].[Store Name]");
        storeNameLevel.setColumn(storeNameExpAggTestDistinctCount);

        AggregationLevelProperty streetAddressProperty = AggregationFactory.eINSTANCE.createAggregationLevelProperty();
        streetAddressProperty.setName("Street address");
        streetAddressProperty.setColumn(storeAddExpAggTestDistinctCount);
        storeNameLevel.getAggregationLevelProperties().add(streetAddressProperty);

        aggName.getAggregationLevels().add(storeNameLevel);

        setupMultiColDimCube(catalog, copier, context,
            List.of(aggName),
            (Column)copier.get(CatalogSupplier.COLUMN_THE_YEAR_TIME_BY_DAY),
            (Column)copier.get(CatalogSupplier.COLUMN_QUARTER_TIME_BY_DAY),
            (Column)copier.get(CatalogSupplier.COLUMN_MONTH_OF_YEAR_TIME_BY_DAY),
            (Column)copier.get(CatalogSupplier.COLUMN_THE_MONTH_TIME_BY_DAY),
            (Column)copier.get(CatalogSupplier.COLUMN_MONTH_OF_YEAR_TIME_BY_DAY), null,
            List.of(), List.of(expAggTest, expAggTestDistinctCount), "Customer Count");

        String query =
            "select { measures.[Customer Count]} on columns, "
            + "non empty CrossJoin({[TimeExtra].[TimeExtra].Year.members},{[Gender].[Gender].[F]}) on rows "
            + "from [ExtraCol]";


        // Seg load query should not use agg table, since the independent
        // attributes for store are on the aggStar bitkey and not part of the
        // request and rollup is not safe
        assertQuerySql(
            context.getConnectionWithDefaultRole(),
            query,
            mysqlPattern(
                "select\n"
                + "    `time_by_day`.`the_year` as `c0`,\n"
                + "    `customer`.`gender` as `c1`,\n"
                + "    count(distinct `sales_fact_1997`.`customer_id`) as `m0`\n"
                + "from\n"
                + "    `sales_fact_1997` as `sales_fact_1997`,\n"
                + "    `time_by_day` as `time_by_day`,\n"
                + "    `customer` as `customer`\n"
                + "where\n"
                + "    `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`\n"
                + "and\n"
                + "    `time_by_day`.`the_year` = 1997\n"
                + "and\n"
                + "    `sales_fact_1997`.`customer_id` = `customer`.`customer_id`\n"
                + "and\n"
                + "    `customer`.`gender` = 'F'\n"
                + "group by\n"
                + "    `time_by_day`.`the_year`,\n"
                + "    `customer`.`gender`"));
    }

    public static void setupMultiColDimCube(Catalog catalog, EcoreUtil.Copier copier,
        Context<?> context, List<AggregationTable> aggTables, Column yearCols, Column qtrCols, Column monthCols,
        Column monthCaptionCol, Column monthOrdinalCol, Column monthNameCol, List<MemberProperty> monthProp, List<Table> tables)
    {
        setupMultiColDimCube(catalog, copier, context,
            aggTables, yearCols, qtrCols, monthCols, monthCaptionCol, monthOrdinalCol, monthNameCol, monthProp, tables, "Unit Sales");
    }

    public static void setupMultiColDimCube(Catalog catalog, EcoreUtil.Copier copier,
        Context<?> context, List<AggregationTable> aggTables, Column yearCol, Column qtrCol, Column monthCol,
        Column monthCaptionCol, Column monthOrdinalCol, Column monthNameCol,
        List<MemberProperty> monthProp, List<Table> tables, String defaultMeasure)
    {
        class ExplicitRecognizerTestModifierInner extends ExplicitRecognizerTestModifierEmf {

            public ExplicitRecognizerTestModifierInner(Catalog catalog, EcoreUtil.Copier copier) {
                super(catalog, copier);
            }

            @Override
            protected List<MemberProperty> getMonthProp() {
                return monthProp;
            }

            @Override
            protected Column getMonthOrdinalCol() {
                return monthOrdinalCol;
            }

            @Override
            protected Column getMonthNameCol() {
                return monthNameCol;
            }

            @Override
            protected Column getMonthCaptionCol() {
                return monthCaptionCol;
            }


            @Override
            protected List<AggregationTable> getAggTables() {
                return aggTables;
            }

            @Override
            protected List<AggregationExclude> getAggExcludes() {
                return List.of();
            }

            @Override
            protected String getDefaultMeasure() {
                return defaultMeasure;
            }

            @Override
            protected Column getQuarterCol() {
                return qtrCol;
            }

            @Override
            protected Column getMonthCol() {
                return monthCol;
            }

            @Override
            protected Column getYearCol() {
                return yearCol;
            }

            @Override
            protected List<Table> getDatabaseSchemaTables() {
                return tables;
            }
        }
        context.getCatalogCache().clear();
        ((TestContext)context).setCatalogMappingSupplier(new ExplicitRecognizerTestModifierInner(catalog, copier));
    }

    private static Column createColumn(String name, org.eclipse.daanse.cwm.model.cwm.resource.relational.SQLSimpleType dataType, Integer charOctetLength, Integer columnSize, Integer decimalDigits) {
        Column column = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        column.setName(name);

        column.setType(dataType);

        if (charOctetLength != null) {
            // column.setCharOctetLength(charOctetLength);
        }
        if (columnSize != null) {
            // column.setColumnSize(columnSize);
        }
        if (decimalDigits != null) {
            // column.setDecimalDigits(decimalDigits);
        }
        return column;
    }

}
