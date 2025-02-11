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
import static org.opencube.junit5.TestUtil.withSchema;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.ColumnMapping;
import org.eclipse.daanse.rolap.mapping.api.model.TableMapping;
import org.eclipse.daanse.rolap.mapping.instance.rec.complex.foodmart.FoodmartMappingSupplier;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationColumnNameMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationExcludeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationForeignKeyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationLevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationLevelPropertyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationNameMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationTableMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.ColumnMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MemberPropertyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalTableMappingImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextArgumentsProvider;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.TestConfig;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.SystemWideProperties;

class ExplicitRecognizerTest extends AggTableTestCase {

    //## TableName: exp_agg_test
    //## ColumnNames:  testyear,testqtr,testmonthord,testmonthname,testmonthcap,testmonprop1,testmonprop2,gender,test_unit_sales,test_store_cost,fact_count
    //## ColumnTypes: INTEGER,VARCHAR(30),INTEGER,VARCHAR(30),VARCHAR(30),VARCHAR(30),VARCHAR(30),VARCHAR(30),INTEGER,DECIMAL(10,4),INTEGER
    ColumnMappingImpl testyearExpAggTest = ColumnMappingImpl.builder().withName("testyear").withType("INTEGER").build();
    ColumnMappingImpl testqtrExpAggTest = ColumnMappingImpl.builder().withName("testqtr").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnMappingImpl testmonthordExpAggTest = ColumnMappingImpl.builder().withName("testmonthord").withType("INTEGER").build();
    ColumnMappingImpl testmonthnameExpAggTest = ColumnMappingImpl.builder().withName("testmonthname").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnMappingImpl testmonthcapExpAggTest = ColumnMappingImpl.builder().withName("testmonthcap").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnMappingImpl testmonprop1ExpAggTest = ColumnMappingImpl.builder().withName("testmonprop1").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnMappingImpl testmonprop2ExpAggTest = ColumnMappingImpl.builder().withName("testmonprop2").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnMappingImpl genderExpAggTest = ColumnMappingImpl.builder().withName("gender").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnMappingImpl testUnitSalesExpAggTest = ColumnMappingImpl.builder().withName("test_unit_sales").withType("INTEGER").build();
    ColumnMappingImpl testStoreCostExpAggTest = ColumnMappingImpl.builder().withName("test_store_cost").withType("DECIMAL").withColumnSize(10).withDecimalDigits(4).build();
    ColumnMappingImpl factCountExpAggTest = ColumnMappingImpl.builder().withName("fact_count").withType("INTEGER").build();
    PhysicalTableMappingImpl expAggTest = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("exp_agg_test")
            .withColumns(List.of(
                    testyearExpAggTest,
                    testqtrExpAggTest,
                    testmonthordExpAggTest,
                    testmonthnameExpAggTest,
                    testmonthcapExpAggTest,
                    testmonprop1ExpAggTest,
                    testmonprop2ExpAggTest,
                    genderExpAggTest,
                    testUnitSalesExpAggTest,
                    testStoreCostExpAggTest,
                    factCountExpAggTest
            ))).build();

    //## TableName:  exp_agg_test_distinct_count
    //## ColumnNames:  fact_count,testyear,gender,store_name,store_country,store_st,store_cty,store_add,unit_s,cust_cnt
    //## ColumnTypes: INTEGER,INTEGER,VARCHAR(30),VARCHAR(30),VARCHAR(30),VARCHAR(30),VARCHAR(30),VARCHAR(30),INTEGER,INTEGER
    ColumnMappingImpl factCountExpAggTestDistinctCount = ColumnMappingImpl.builder().withName("fact_count").withType("INTEGER").build();
    ColumnMappingImpl testyearExpAggTestDistinctCount = ColumnMappingImpl.builder().withName("testyear").withType("INTEGER").build();
    ColumnMappingImpl genderExpAggTestDistinctCount = ColumnMappingImpl.builder().withName("gender").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnMappingImpl storeNameExpAggTestDistinctCount = ColumnMappingImpl.builder().withName("store_name").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnMappingImpl storeCountryExpAggTestDistinctCount = ColumnMappingImpl.builder().withName("store_country").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnMappingImpl storeStExpAggTestDistinctCount = ColumnMappingImpl.builder().withName("store_st").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnMappingImpl storeCtyExpAggTestDistinctCount = ColumnMappingImpl.builder().withName("store_cty").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnMappingImpl storeAddExpAggTestDistinctCount = ColumnMappingImpl.builder().withName("store_add").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnMappingImpl unitSExpAggTestDistinctCount = ColumnMappingImpl.builder().withName("unit_s").withType("INTEGER").build();
    ColumnMappingImpl custCntExpAggTestDistinctCount = ColumnMappingImpl.builder().withName("cust_cnt").withType("INTEGER").build();
    PhysicalTableMappingImpl expAggTestDistinctCount = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("exp_agg_test_distinct_count")
            .withColumns(List.of(
                factCountExpAggTestDistinctCount,
                testyearExpAggTestDistinctCount,
                genderExpAggTestDistinctCount,
                storeNameExpAggTestDistinctCount,
                storeCountryExpAggTestDistinctCount,
                storeStExpAggTestDistinctCount,
                storeCtyExpAggTestDistinctCount,
                storeAddExpAggTestDistinctCount,
                unitSExpAggTestDistinctCount,
                custCntExpAggTestDistinctCount
            ))).build();
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
    void testExplicitAggExtraColsRequiringJoin(Context context) throws SQLException {
        ((TestConfig)context.getConfig()).setGenerateFormattedSql(true);
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        ((TestConfig)context.getConfig()).setDisableCaching(true);
        prepareContext(context);

        setupMultiColDimCube(context,
                List.of(AggregationNameMappingImpl.builder()
                        .withName(FoodmartMappingSupplier.AGG_G_MS_PCAT_SALES_FACT_1997)
                        .withAggregationFactCount(AggregationColumnNameMappingImpl.builder()
                        .withColumn(FoodmartMappingSupplier.FACT_COUNT_COLUMN_IN_AGG_G_MS_PCAT_SALES_FACT_1997)
                        .build())
                        .withAggregationMeasures(List.of(
                        AggregationMeasureMappingImpl.builder()
                            .withName("[Measures].[Unit Sales]")
                            .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_AGG_G_MS_PCAT_SALES_FACT_1997)
                            .build()
                        ))
                        .withAggregationLevels(List.of(
                            AggregationLevelMappingImpl.builder()
                                .withName("[Gender].[Gender]")
                                .withColumn(FoodmartMappingSupplier.GENDER_COLUMN_IN_AGG_G_MS_PCAT_SALES_FACT_1997)
                                .build(),
                            AggregationLevelMappingImpl.builder()
                                .withName("[TimeExtra].[Year]")
                                .withColumn(FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_AGG_G_MS_PCAT_SALES_FACT_1997)
                                .build(),
                            AggregationLevelMappingImpl.builder()
                                .withName("[TimeExtra].[Quarter]")
                                .withColumn(FoodmartMappingSupplier.QUARTER_COLUMN_IN_AGG_G_MS_PCAT_SALES_FACT_1997)
                                .build(),
                            AggregationLevelMappingImpl.builder()
                                .withName("[TimeExtra].[Month]")
                                .withColumn(FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_AGG_G_MS_PCAT_SALES_FACT_1997)
                                .build()
                        ))
                        .build()
                    ),
            FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY, FoodmartMappingSupplier.THE_MONTH_COLUMN_IN_TIME_BY_DAY, FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY, null,
            List.of(), List.of(expAggTest, expAggTestDistinctCount));

        String query =
            "select {[Measures].[Unit Sales]} on columns, "
            + "non empty CrossJoin({[TimeExtra].[Month].members},{[Gender].[M]}) on rows "
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
    void testExplicitForeignKey(Context context) {
        ((TestConfig)context.getConfig()).setGenerateFormattedSql(true);
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        ((TestConfig)context.getConfig()).setDisableCaching(true);
        prepareContext(context);
        setupMultiColDimCube(context,
            List.of(AggregationNameMappingImpl.builder()
                .withName(FoodmartMappingSupplier.AGG_C_14_SALES_FACT_1997)
                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder()
                    .withColumn(FoodmartMappingSupplier.FACT_COUNT_COLUMN_IN_AGG_C_14_SALES_FACT_1997)
                    .build())
                .withAggregationForeignKeys(List.of(
                    AggregationForeignKeyMappingImpl.builder()
                        .withFactColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997 )
                        .withAggregationColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_AGG_C_14_SALES_FACT_1997)
                        .build()
                ))
                .withAggregationMeasures(List.of(
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Unit Sales]")
                        .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_AGG_C_14_SALES_FACT_1997)
                        .build(),
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Store Cost]")
                        .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_AGG_C_14_SALES_FACT_1997)
                        .build()
                ))
                .withAggregationLevels(List.of(
                    //AggregationLevelMappingImpl.builder()
                    //    .withName("[Gender].[Gender]")
                    //    .withColumn("gender") //TODO gender is absent
                    //    .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[TimeExtra].[Year]")
                        .withColumn(FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_AGG_C_14_SALES_FACT_1997)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[TimeExtra].[Quarter]")
                        .withColumn(FoodmartMappingSupplier.QUARTER_COLUMN_IN_AGG_C_14_SALES_FACT_1997)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[TimeExtra].[Month]")
                        .withColumn(FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_AGG_C_14_SALES_FACT_1997)
                        .build()
                ))
                .build()
            ),
            FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY, FoodmartMappingSupplier.THE_MONTH_COLUMN_IN_TIME_BY_DAY, FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY, null,
            List.of(), List.of(expAggTest, expAggTestDistinctCount));


        String query =
            "select {[Measures].[Unit Sales]} on columns, "
            + "non empty CrossJoin({[TimeExtra].[Month].members},{[Store].[Store Name].members}) on rows "
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
    void testExplicitAggOrdinalOnAggTable(Context context) throws SQLException {
        ((TestConfig)context.getConfig()).setGenerateFormattedSql(true);
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        ((TestConfig)context.getConfig()).setDisableCaching(true);
        prepareContext(context);
        setupMultiColDimCube(context,
            List.of(AggregationNameMappingImpl.builder()
                .withName(expAggTest)
                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder()
                    .withColumn(factCountExpAggTest)
                    .build())
                .withAggregationMeasures(List.of(
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Unit Sales]")
                        .withColumn(testUnitSalesExpAggTest)
                        .build()
                ))
                .withAggregationLevels(List.of(
                    AggregationLevelMappingImpl.builder()
                        .withName("[Gender].[Gender]")
                        .withColumn(genderExpAggTest)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[TimeExtra].[Year]")
                        .withColumn(testyearExpAggTest)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[TimeExtra].[Quarter]")
                        .withColumn(testqtrExpAggTest)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[TimeExtra].[Month]")
                        .withColumn(testmonthnameExpAggTest)
                        .withOrdinalColumn(testmonthordExpAggTest)
                        .build()
                ))
                .build()
            ),
            FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.THE_MONTH_COLUMN_IN_TIME_BY_DAY, null,  FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY, null,
            List.of(), List.of(expAggTest, expAggTestDistinctCount));

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
                    + "    ISNULL(`c4`) ASC, `c4` ASC"
                    : "    ISNULL(`exp_agg_test`.`testyear`) ASC, `exp_agg_test`.`testyear` ASC,\n"
                    + "    ISNULL(`exp_agg_test`.`testqtr`) ASC, `exp_agg_test`.`testqtr` ASC,\n"
                    + "    ISNULL(`exp_agg_test`.`testmonthord`) ASC, `exp_agg_test`.`testmonthord` ASC,\n"
                    + "    ISNULL(`exp_agg_test`.`gender`) ASC, `exp_agg_test`.`gender` ASC")));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testExplicitAggCaptionOnAggTable(Context context) throws SQLException {
        ((TestConfig)context.getConfig()).setGenerateFormattedSql(true);
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        ((TestConfig)context.getConfig()).setDisableCaching(true);
        prepareContext(context);
        setupMultiColDimCube(context,
            List.of(AggregationNameMappingImpl.builder()
                .withName(expAggTest)
                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder()
                    .withColumn(factCountExpAggTest)
                    .build())
                .withAggregationMeasures(List.of(
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Unit Sales]")
                        .withColumn(testUnitSalesExpAggTest)
                        .build()
                ))
                .withAggregationLevels(List.of(
                    AggregationLevelMappingImpl.builder()
                        .withName("[Gender].[Gender]")
                        .withColumn(genderExpAggTest)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[TimeExtra].[Year]")
                        .withColumn(testyearExpAggTest)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[TimeExtra].[Quarter]")
                        .withColumn(testqtrExpAggTest)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[TimeExtra].[Month]")
                        .withColumn(testmonthnameExpAggTest)
                        .withCaptionColumn(testmonthcapExpAggTest)
                        .build()
                ))
                .build()
            ),
            FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.THE_MONTH_COLUMN_IN_TIME_BY_DAY,  FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY, null, null,
            List.of(), List.of(expAggTest, expAggTestDistinctCount));

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
    void testExplicitAggNameColumnOnAggTable(Context context) throws SQLException {
        ((TestConfig)context.getConfig()).setGenerateFormattedSql(true);
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        ((TestConfig)context.getConfig()).setDisableCaching(true);
        prepareContext(context);
        setupMultiColDimCube(context,
            List.of(AggregationNameMappingImpl.builder()
                .withName(expAggTest)
                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder()
                    .withColumn(factCountExpAggTest)
                    .build())
                .withAggregationMeasures(List.of(
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Unit Sales]")
                        .withColumn(testUnitSalesExpAggTest)
                        .build()
                ))
                .withAggregationLevels(List.of(
                    AggregationLevelMappingImpl.builder()
                        .withName("[Gender].[Gender]")
                        .withColumn(genderExpAggTest)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[TimeExtra].[Year]")
                        .withColumn(testyearExpAggTest)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[TimeExtra].[Quarter]")
                        .withColumn(testqtrExpAggTest)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[TimeExtra].[Month]")
                        .withColumn(testmonthnameExpAggTest)
                        .withNameColumn(testmonthcapExpAggTest)
                        .withAggregationLevelProperties(Stream.of(
                            AggregationLevelPropertyMappingImpl.builder()
                                .withName("aProperty")
                                .withColumn(testmonprop1ExpAggTest)
                                .build()
                        ).collect(Collectors.toList()))
                        .build()
                ))
                .build()
            ),
            FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.THE_MONTH_COLUMN_IN_TIME_BY_DAY, null, null,  FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY,
            List.of(MemberPropertyMappingImpl.builder()
                .withName("aProperty")
                .withColumn(FoodmartMappingSupplier.FISCAL_PERIOD_COLUMN_IN_TIME_BY_DAY)
                .build()), List.of(expAggTest, expAggTestDistinctCount));

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
    void testExplicitAggPropertiesOnAggTable(Context context) throws SQLException {
        ((TestConfig)context.getConfig()).setGenerateFormattedSql(true);
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        ((TestConfig)context.getConfig()).setDisableCaching(true);
        prepareContext(context);
        setupMultiColDimCube(context,
            List.of(AggregationNameMappingImpl.builder()
                .withName(expAggTestDistinctCount)
                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder()
                    .withColumn(factCountExpAggTestDistinctCount)
                    .build())
                .withAggregationMeasures(List.of(
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Unit Sales]")
                        .withColumn(unitSExpAggTestDistinctCount)
                        .build(),
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Customer Count]")
                        .withColumn(custCntExpAggTestDistinctCount)
                        .build()
                    ))
                .withAggregationLevels(List.of(
                    AggregationLevelMappingImpl.builder()
                        .withName("[TimeExtra].[Year]")
                        .withColumn(testyearExpAggTestDistinctCount)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Gender].[Gender]")
                        .withColumn(genderExpAggTestDistinctCount)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Store].[Store Country]")
                        .withColumn(storeCountryExpAggTestDistinctCount)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Store].[Store State]")
                        .withColumn(storeStExpAggTestDistinctCount)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Store].[Store City]")
                        .withColumn(storeCtyExpAggTestDistinctCount)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Store].[Store Name]")
                        .withColumn(storeNameExpAggTestDistinctCount)
                        .withAggregationLevelProperties(List.of(
                            AggregationLevelPropertyMappingImpl.builder()
                                .withName("Street address")
                                .withColumn(storeAddExpAggTestDistinctCount)
                                .build()
                        ))
                        .build()

                        ))
                        .build()
                ),
            FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY, FoodmartMappingSupplier.THE_MONTH_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY, null,
            List.of(), List.of(expAggTest, expAggTestDistinctCount));

        String query =
            "with member measures.propVal as 'Store.CurrentMember.Properties(\"Street Address\")'"
            + "select { measures.[propVal], measures.[Customer Count], [Measures].[Unit Sales]} on columns, "
            + "non empty CrossJoin({[Gender].Gender.members},{[Store].[USA].[WA].[Spokane].[Store 16]}) on rows "
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
            + "{[Gender].[F], [Store].[USA].[WA].[Spokane].[Store 16]}\n"
            + "{[Gender].[M], [Store].[USA].[WA].[Spokane].[Store 16]}\n"
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
    void testCountDistinctAllowableRollup(Context context) throws SQLException {
        ((TestConfig)context.getConfig()).setGenerateFormattedSql(true);
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        ((TestConfig)context.getConfig()).setDisableCaching(true);
        prepareContext(context);
        setupMultiColDimCube(context,
            List.of(AggregationNameMappingImpl.builder()
                .withName(expAggTestDistinctCount)
                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder()
                    .withColumn(factCountExpAggTestDistinctCount)
                    .build())
                .withAggregationMeasures(List.of(
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Unit Sales]")
                        .withColumn(unitSExpAggTestDistinctCount)
                        .build(),
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Customer Count]")
                        .withColumn(custCntExpAggTestDistinctCount)
                        .build()
                ))
                .withAggregationLevels(List.of(
                    AggregationLevelMappingImpl.builder()
                        .withName("[TimeExtra].[Year]")
                        .withColumn(testyearExpAggTestDistinctCount)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Gender].[Gender]")
                        .withColumn(genderExpAggTestDistinctCount)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Store].[Store Country]")
                        .withColumn(storeCountryExpAggTestDistinctCount)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Store].[Store State]")
                        .withColumn(storeStExpAggTestDistinctCount)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Store].[Store City]")
                        .withColumn(storeCtyExpAggTestDistinctCount)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Store].[Store Name]")
                        .withColumn(storeNameExpAggTestDistinctCount)
                        .withAggregationLevelProperties(List.of(
                            AggregationLevelPropertyMappingImpl.builder()
                                .withName("Street address")
                                .withColumn(storeAddExpAggTestDistinctCount)
                                .build()
                        ))
                        .build()

                ))
                .build()),
            FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY, FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY, null,
            List.of(), List.of(expAggTest, expAggTestDistinctCount), "Customer Count");

        // Query brings in Year and Store Name, omitting Gender.
        // It's okay to roll up the agg table in this case
        // since Customer Count is dependent on Gender.
        String query =
            "select { measures.[Customer Count], [Measures].[Unit Sales]} on columns, "
            + "non empty CrossJoin({[TimeExtra].Year.members},{[Store].[USA].[WA].[Spokane].[Store 16]}) on rows "
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
    void testCountDisallowedRollup(Context context) throws SQLException {
        ((TestConfig)context.getConfig()).setGenerateFormattedSql(true);
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        ((TestConfig)context.getConfig()).setDisableCaching(true);
        prepareContext(context);
        setupMultiColDimCube(context,
            List.of(AggregationNameMappingImpl.builder()
                .withName(expAggTestDistinctCount)
                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder()
                    .withColumn(factCountExpAggTestDistinctCount)
                    .build())
                .withAggregationMeasures(List.of(
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Unit Sales]")
                        .withColumn(unitSExpAggTestDistinctCount)
                        .build(),
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Customer Count]")
                        .withColumn(custCntExpAggTestDistinctCount)
                        .build()
                ))
                .withAggregationLevels(List.of(
                    AggregationLevelMappingImpl.builder()
                        .withName("[TimeExtra].[Year]")
                        .withColumn(testyearExpAggTestDistinctCount)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Gender].[Gender]")
                        .withColumn(genderExpAggTestDistinctCount)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Store].[Store Country]")
                        .withColumn(storeCountryExpAggTestDistinctCount)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Store].[Store State]")
                        .withColumn(storeStExpAggTestDistinctCount)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Store].[Store City]")
                        .withColumn(storeCtyExpAggTestDistinctCount)
                        .build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Store].[Store Name]")
                        .withColumn(storeNameExpAggTestDistinctCount)
                        .withAggregationLevelProperties(List.of(
                            AggregationLevelPropertyMappingImpl.builder()
                                .withName("Street address")
                                .withColumn(storeAddExpAggTestDistinctCount)
                                .build()
                        ))
                        .build()

                ))
                .build()
            ),
            FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.THE_MONTH_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY, null,
            List.of(), List.of(expAggTest, expAggTestDistinctCount), "Customer Count");

        String query =
            "select { measures.[Customer Count]} on columns, "
            + "non empty CrossJoin({[TimeExtra].Year.members},{[Gender].[F]}) on rows "
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

    public static void setupMultiColDimCube(
        Context context, List<AggregationTableMappingImpl> aggTables, ColumnMapping yearCols, ColumnMapping qtrCols, ColumnMapping monthCols,
        ColumnMapping monthCaptionCol, ColumnMapping monthOrdinalCol, ColumnMapping monthNameCol, List<MemberPropertyMappingImpl> monthProp, List<TableMapping> tables)
    {
        setupMultiColDimCube(context,
            aggTables, yearCols, qtrCols, monthCols, monthCaptionCol, monthOrdinalCol, monthNameCol, monthProp, tables, "Unit Sales");
    }

    public static void setupMultiColDimCube(
        Context context, List<AggregationTableMappingImpl> aggTables, ColumnMapping yearCol, ColumnMapping qtrCol, ColumnMapping monthCol,
        ColumnMapping monthCaptionCol, ColumnMapping monthOrdinalCol, ColumnMapping monthNameCol,
        List<MemberPropertyMappingImpl> monthProp, List<TableMapping> tables, String defaultMeasure)
    {
        class ExplicitRecognizerTestModifierInner extends ExplicitRecognizerTestModifier {

            public ExplicitRecognizerTestModifierInner(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<MemberPropertyMappingImpl> getMonthProp() {
                return monthProp;
            }

            @Override
            protected ColumnMapping getMonthOrdinalCol() {
                return monthOrdinalCol;
            }

            @Override
            protected ColumnMapping getMonthNameCol() {
                return monthNameCol;
            }

            @Override
            protected ColumnMapping getMonthCaptionCol() {
                return monthCaptionCol;
            }


            @Override
            protected List<AggregationTableMappingImpl> getAggTables() {
                return aggTables;
            }

            @Override
            protected List<AggregationExcludeMappingImpl> getAggExcludes() {
                return List.of();
            }

            @Override
            protected String getDefaultMeasure() {
                return defaultMeasure;
            }

            @Override
            protected ColumnMapping getQuarterCol() {
                return qtrCol;
            }

            @Override
            protected ColumnMapping getMonthCol() {
                return monthCol;
            }

            @Override
            protected ColumnMapping getYearCol() {
                return yearCol;
            }

            @Override
            protected List<TableMapping> getDdatabaseSchemaTables() {
                return tables;
            }
        }

        withSchema(context, ExplicitRecognizerTestModifierInner::new);
    }

}
