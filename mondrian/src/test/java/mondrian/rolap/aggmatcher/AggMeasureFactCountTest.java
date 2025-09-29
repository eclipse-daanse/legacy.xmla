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

package mondrian.rolap.aggmatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.withSchemaEmf;

import java.util.List;
import java.util.function.Function;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.rolap.mapping.model.AggregationColumnName;
import org.eclipse.daanse.rolap.mapping.model.AggregationExclude;
import org.eclipse.daanse.rolap.mapping.model.AggregationMeasureFactCount;
import org.eclipse.daanse.rolap.mapping.model.AggregationTable;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.ColumnType;
import org.eclipse.daanse.rolap.mapping.model.PhysicalColumn;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.enums.DatabaseProduct;
import mondrian.test.SqlPattern;
import mondrian.test.loader.CsvDBTestCase;

class AggMeasureFactCountTest extends CsvDBTestCase {

    private final String QUERY = ""
            + "select [Time].[Time].[Quarter].Members on columns, \n"
            + "{[Measures].[Store Sales], [Measures].[Store Cost], [Measures].[Unit Sales]} on rows "
            + "from [Sales]";

    @Override
    protected String getFileName() {
        return "agg_measure_fact_count_test.csv";
    }

    @BeforeEach
    public void beforeEach() {
    }

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

    @Override
    protected void prepareContext(Context<?> context) {
        super.prepareContext(context);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testDefaultRecognition(Context<?> context) {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);
        String sqlMysql = ""
                + "select\n"
                + "    `agg_c_6_fact_csv_2016`.`the_year` as `c0`,\n"
                + "    `agg_c_6_fact_csv_2016`.`quarter` as `c1`,\n"
                + "    sum(`agg_c_6_fact_csv_2016`.`unit_sales`) / sum(`agg_c_6_fact_csv_2016`.`unit_sales_fact_count`) as `m0`,\n"
                + "    sum(`agg_c_6_fact_csv_2016`.`store_cost`) / sum(`agg_c_6_fact_csv_2016`.`store_cost_fact_count`) as `m1`,\n"
                + "    sum(`agg_c_6_fact_csv_2016`.`store_sales`) / sum(`agg_c_6_fact_csv_2016`.`store_sales_fact_count`) as `m2`\n"
                + "from\n"
                + "    `agg_c_6_fact_csv_2016` as `agg_c_6_fact_csv_2016`\n"
                + "where\n"
                + "    `agg_c_6_fact_csv_2016`.`the_year` = 1997\n"
                + "group by\n"
                + "    `agg_c_6_fact_csv_2016`.`the_year`,\n"
                + "    `agg_c_6_fact_csv_2016`.`quarter`";

        verifySameAggAndNot(context, QUERY, getAggSchema(List.of(), List.of()), sqlMysql);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testAggName(Context<?> context) {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
    	prepareContext(context);

        // Create aggTables using RolapMappingFactory.eINSTANCE
        var factory = RolapMappingFactory.eINSTANCE;

        // Create AggregationColumnName for fact count
        AggregationColumnName aggFactCount = factory.createAggregationColumnName();
        aggFactCount.setColumn(AggMeasureFactCountTestModifierEmf.factCountAggC6FactCsv2016);

        // Create AggregationMeasureFactCount elements
        AggregationMeasureFactCount storeSalesFactCount = factory.createAggregationMeasureFactCount();
        storeSalesFactCount.setColumn(AggMeasureFactCountTestModifierEmf.storeSalesFactCountAggC6FactCsv2016);
        storeSalesFactCount.setFactColumn(AggMeasureFactCountTestModifierEmf.storeSalesColumnInFactCsv2016);

        var storeCostFactCount = factory.createAggregationMeasureFactCount();
        storeCostFactCount.setColumn(AggMeasureFactCountTestModifierEmf.storeCostFactCountAggC6FactCsv2016);
        storeCostFactCount.setFactColumn(AggMeasureFactCountTestModifierEmf.storeCostColumnInFactCsv2016);

        var unitSalesFactCount = factory.createAggregationMeasureFactCount();
        unitSalesFactCount.setColumn(AggMeasureFactCountTestModifierEmf.unitSalesFactCountAggC6FactCsv2016);
        unitSalesFactCount.setFactColumn(AggMeasureFactCountTestModifierEmf.unitSalesColumnInFactCsv2016);

        // Create AggregationMeasure elements
        var unitSalesMeasure = factory.createAggregationMeasure();
        unitSalesMeasure.setName("[Measures].[Unit Sales]");
        unitSalesMeasure.setColumn(AggMeasureFactCountTestModifierEmf.unitSalesAggC6FactCsv2016);

        var storeCostMeasure = factory.createAggregationMeasure();
        storeCostMeasure.setName("[Measures].[Store Cost]");
        storeCostMeasure.setColumn(AggMeasureFactCountTestModifierEmf.storeCostAggC6FactCsv2016);

        var storeSalesMeasure = factory.createAggregationMeasure();
        storeSalesMeasure.setName("[Measures].[Store Sales]");
        storeSalesMeasure.setColumn(AggMeasureFactCountTestModifierEmf.storeSalesAggC6FactCsv2016);

        // Create AggregationLevel elements
        var yearLevel = factory.createAggregationLevel();
        yearLevel.setName("[Time].[Time].[Year]");
        yearLevel.setColumn(AggMeasureFactCountTestModifierEmf.theYearAggC6FactCsv2016);

        var quarterLevel = factory.createAggregationLevel();
        quarterLevel.setName("[Time].[Time].[Quarter]");
        quarterLevel.setColumn(AggMeasureFactCountTestModifierEmf.quarterAggC6FactCsv2016);

        var monthLevel = factory.createAggregationLevel();
        monthLevel.setName("[Time].[Time].[Month]");
        monthLevel.setColumn(AggMeasureFactCountTestModifierEmf.monthOfYearAggC6FactCsv2016);

        // Create AggregationName
        var aggregationName = factory.createAggregationName();
        aggregationName.setName(AggMeasureFactCountTestModifierEmf.aggC6FactCsv2016);
        aggregationName.setAggregationFactCount(aggFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(storeSalesFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(storeCostFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(unitSalesFactCount);
        aggregationName.getAggregationMeasures().add(unitSalesMeasure);
        aggregationName.getAggregationMeasures().add(storeCostMeasure);
        aggregationName.getAggregationMeasures().add(storeSalesMeasure);
        aggregationName.getAggregationLevels().add(yearLevel);
        aggregationName.getAggregationLevels().add(quarterLevel);
        aggregationName.getAggregationLevels().add(monthLevel);

        List<AggregationTable> aggTables = List.of(aggregationName);

        /*
        String agg = ""
                + "<AggName name=\"agg_c_6_fact_csv_2016\">\n"
                + "    <AggFactCount column=\"fact_count\"/>\n"
                + "    <AggMeasureFactCount column=\"store_sales_fact_count\" factColumn=\"store_sales\" />\n"
                + "    <AggMeasureFactCount column=\"store_cost_fact_count\" factColumn=\"store_cost\" />\n"
                + "    <AggMeasureFactCount column=\"unit_sales_fact_count\" factColumn=\"unit_sales\" />\n"
                + "    <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"UNIT_SALES\" />\n"
                + "    <AggMeasure name=\"[Measures].[Store Cost]\" column=\"STORE_COST\" />\n"
                + "    <AggMeasure name=\"[Measures].[Store Sales]\" column=\"STORE_SALES\" />\n"
                + "    <AggLevel name=\"[Time].[Year]\" column=\"the_year\" />\n"
                + "    <AggLevel name=\"[Time].[Quarter]\" column=\"quarter\" />\n"
                + "    <AggLevel name=\"[Time].[Month]\" column=\"month_of_year\" />\n"
                + "</AggName>\n";
        */
        String aggSql = ""
                + "select\n"
                + "    `agg_c_6_fact_csv_2016`.`the_year` as `c0`,\n"
                + "    `agg_c_6_fact_csv_2016`.`quarter` as `c1`,\n"
                + "    sum(`agg_c_6_fact_csv_2016`.`unit_sales` * `agg_c_6_fact_csv_2016`.`unit_sales_fact_count`) / sum(`agg_c_6_fact_csv_2016`.`unit_sales_fact_count`) as `m0`,\n"
                + "    sum(`agg_c_6_fact_csv_2016`.`store_cost` * `agg_c_6_fact_csv_2016`.`store_cost_fact_count`) / sum(`agg_c_6_fact_csv_2016`.`store_cost_fact_count`) as `m1`,\n"
                + "    sum(`agg_c_6_fact_csv_2016`.`store_sales` * `agg_c_6_fact_csv_2016`.`store_sales_fact_count`) / sum(`agg_c_6_fact_csv_2016`.`store_sales_fact_count`) as `m2`\n"
                + "from\n"
                + "    `agg_c_6_fact_csv_2016` as `agg_c_6_fact_csv_2016`\n"
                + "where\n"
                + "    `agg_c_6_fact_csv_2016`.`the_year` = 1997\n"
                + "group by\n"
                + "    `agg_c_6_fact_csv_2016`.`the_year`,\n"
                + "    `agg_c_6_fact_csv_2016`.`quarter`";

        verifySameAggAndNot(context, QUERY, getAggSchema(List.of(), aggTables), aggSql);
        // Note: aggTablesPojo contains the POJO version, aggTables contains the EMF version using RolapMappingFactory.eINSTANCE
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @DisabledIfSystemProperty(named = "tempIgnoreStrageTests",matches = "true")
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testFactColumnNotExists(Context<?> context) {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);

        // Create aggTables using RolapMappingFactory.eINSTANCE
        var factory = RolapMappingFactory.eINSTANCE;

        // Create AggregationColumnName for fact count
        var aggFactCount = factory.createAggregationColumnName();
        aggFactCount.setColumn(AggMeasureFactCountTestModifierEmf.factCountAggC6FactCsv2016);

        // Create AggregationMeasureFactCount elements WITHOUT factColumn (это ключевое отличие этого теста)
        var storeSalesFactCount = factory.createAggregationMeasureFactCount();
        storeSalesFactCount.setColumn(AggMeasureFactCountTestModifierEmf.storeSalesFactCountAggC6FactCsv2016);
        // НЕ устанавливаем factColumn - это тест на отсутствие factColumn

        var storeCostFactCount = factory.createAggregationMeasureFactCount();
        storeCostFactCount.setColumn(AggMeasureFactCountTestModifierEmf.storeCostFactCountAggC6FactCsv2016);
        // НЕ устанавливаем factColumn

        var unitSalesFactCount = factory.createAggregationMeasureFactCount();
        unitSalesFactCount.setColumn(AggMeasureFactCountTestModifierEmf.unitSalesFactCountAggC6FactCsv2016);
        // НЕ устанавливаем factColumn

        // Create AggregationMeasure elements
        var unitSalesMeasure = factory.createAggregationMeasure();
        unitSalesMeasure.setName("[Measures].[Unit Sales]");
        unitSalesMeasure.setColumn(AggMeasureFactCountTestModifierEmf.unitSalesAggC6FactCsv2016);

        var storeCostMeasure = factory.createAggregationMeasure();
        storeCostMeasure.setName("[Measures].[Store Cost]");
        storeCostMeasure.setColumn(AggMeasureFactCountTestModifierEmf.storeCostAggC6FactCsv2016);

        var storeSalesMeasure = factory.createAggregationMeasure();
        storeSalesMeasure.setName("[Measures].[Store Sales]");
        storeSalesMeasure.setColumn(AggMeasureFactCountTestModifierEmf.storeSalesAggC6FactCsv2016);

        // Create AggregationLevel elements
        var yearLevel = factory.createAggregationLevel();
        yearLevel.setName("[Time].[Year]");
        yearLevel.setColumn(AggMeasureFactCountTestModifierEmf.theYearAggC6FactCsv2016);

        var quarterLevel = factory.createAggregationLevel();
        quarterLevel.setName("[Time].[Quarter]");
        quarterLevel.setColumn(AggMeasureFactCountTestModifierEmf.quarterAggC6FactCsv2016);

        var monthLevel = factory.createAggregationLevel();
        monthLevel.setName("[Time].[Month]");
        monthLevel.setColumn(AggMeasureFactCountTestModifierEmf.monthOfYearAggC6FactCsv2016);

        // Create AggregationName
        var aggregationName = factory.createAggregationName();
        aggregationName.setName(AggMeasureFactCountTestModifierEmf.aggC6FactCsv2016);
        aggregationName.setAggregationFactCount(aggFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(storeSalesFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(storeCostFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(unitSalesFactCount);
        aggregationName.getAggregationMeasures().add(unitSalesMeasure);
        aggregationName.getAggregationMeasures().add(storeCostMeasure);
        aggregationName.getAggregationMeasures().add(storeSalesMeasure);
        aggregationName.getAggregationLevels().add(yearLevel);
        aggregationName.getAggregationLevels().add(quarterLevel);
        aggregationName.getAggregationLevels().add(monthLevel);

        List<AggregationTable> aggTables = List.of(aggregationName);

        /*
        String agg = ""
                + "<AggName name=\"agg_c_6_fact_csv_2016\">\n"
                + "    <AggFactCount column=\"fact_count\"/>\n"
                + "    <AggMeasureFactCount column=\"store_sales_fact_count\" />\n"
                + "    <AggMeasureFactCount column=\"store_cost_fact_count\" />\n"
                + "    <AggMeasureFactCount column=\"unit_sales_fact_count\" />\n"
                + "    <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"UNIT_SALES\" />\n"
                + "    <AggMeasure name=\"[Measures].[Store Cost]\" column=\"STORE_COST\" />\n"
                + "    <AggMeasure name=\"[Measures].[Store Sales]\" column=\"STORE_SALES\" />\n"
                + "    <AggLevel name=\"[Time].[Year]\" column=\"the_year\" />\n"
                + "    <AggLevel name=\"[Time].[Quarter]\" column=\"quarter\" />\n"
                + "    <AggLevel name=\"[Time].[Month]\" column=\"month_of_year\" />\n"
                + "</AggName>\n";
        */
        try {
            verifySameAggAndNot(context, QUERY, getAggSchema(List.of(), aggTables));
            fail("Should throw mondrian exception");
        } catch (OlapRuntimeException e) {
            assertTrue
                    (e.getMessage().startsWith
                            ("Mondrian Error:Internal"
                                    + " error: while parsing catalog"));
        }
        // Note: aggTablesPojo contains the POJO version, aggTables contains the EMF version using RolapMappingFactory.eINSTANCE
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testMeasureFactColumnUpperCase(Context<?> context) {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);

        // Create aggTables using RolapMappingFactory.eINSTANCE
        var factory = RolapMappingFactory.eINSTANCE;

        // Create AggregationColumnName for fact count
        var aggFactCount = factory.createAggregationColumnName();
        aggFactCount.setColumn(AggMeasureFactCountTestModifierEmf.factCountAggC6FactCsv2016);

        // Create AggregationMeasureFactCount elements
        var storeSalesFactCount = factory.createAggregationMeasureFactCount();
        storeSalesFactCount.setColumn(AggMeasureFactCountTestModifierEmf.storeSalesFactCountAggC6FactCsv2016);
        storeSalesFactCount.setFactColumn(AggMeasureFactCountTestModifierEmf.storeSalesColumnInFactCsv2016);

        var storeCostFactCount = factory.createAggregationMeasureFactCount();
        storeCostFactCount.setColumn(AggMeasureFactCountTestModifierEmf.storeCostFactCountAggC6FactCsv2016);
        storeCostFactCount.setFactColumn(AggMeasureFactCountTestModifierEmf.storeCostColumnInFactCsv2016);

        var unitSalesFactCount = factory.createAggregationMeasureFactCount();
        unitSalesFactCount.setColumn(AggMeasureFactCountTestModifierEmf.unitSalesFactCountAggC6FactCsv2016);
        unitSalesFactCount.setFactColumn(AggMeasureFactCountTestModifierEmf.unitSalesColumnInFactCsv2016);

        // Create AggregationMeasure elements
        var unitSalesMeasure = factory.createAggregationMeasure();
        unitSalesMeasure.setName("[Measures].[Unit Sales]");
        unitSalesMeasure.setColumn(AggMeasureFactCountTestModifierEmf.unitSalesAggC6FactCsv2016);

        var storeCostMeasure = factory.createAggregationMeasure();
        storeCostMeasure.setName("[Measures].[Store Cost]");
        storeCostMeasure.setColumn(AggMeasureFactCountTestModifierEmf.storeCostAggC6FactCsv2016);

        var storeSalesMeasure = factory.createAggregationMeasure();
        storeSalesMeasure.setName("[Measures].[Store Sales]");
        storeSalesMeasure.setColumn(AggMeasureFactCountTestModifierEmf.storeSalesAggC6FactCsv2016);

        // Create AggregationLevel elements
        var yearLevel = factory.createAggregationLevel();
        yearLevel.setName("[Time].[Time].[Year]");
        yearLevel.setColumn(AggMeasureFactCountTestModifierEmf.theYearAggC6FactCsv2016);

        var quarterLevel = factory.createAggregationLevel();
        quarterLevel.setName("[Time].[Time].[Quarter]");
        quarterLevel.setColumn(AggMeasureFactCountTestModifierEmf.quarterAggC6FactCsv2016);

        var monthLevel = factory.createAggregationLevel();
        monthLevel.setName("[Time].[Time].[Month]");
        monthLevel.setColumn(AggMeasureFactCountTestModifierEmf.monthOfYearAggC6FactCsv2016);

        // Create AggregationName
        var aggregationName = factory.createAggregationName();
        aggregationName.setName(AggMeasureFactCountTestModifierEmf.aggC6FactCsv2016);
        aggregationName.setAggregationFactCount(aggFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(storeSalesFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(storeCostFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(unitSalesFactCount);
        aggregationName.getAggregationMeasures().add(unitSalesMeasure);
        aggregationName.getAggregationMeasures().add(storeCostMeasure);
        aggregationName.getAggregationMeasures().add(storeSalesMeasure);
        aggregationName.getAggregationLevels().add(yearLevel);
        aggregationName.getAggregationLevels().add(quarterLevel);
        aggregationName.getAggregationLevels().add(monthLevel);

        List<AggregationTable> aggTables = List.of(aggregationName);

        /*
        String agg = ""
                + "<AggName name=\"agg_c_6_fact_csv_2016\">\n"
                + "    <AggFactCount column=\"fact_count\"/>\n"
                + "    <AggMeasureFactCount column=\"store_sales_fact_count\" factColumn=\"STORE_SALES\" />\n"
                + "    <AggMeasureFactCount column=\"store_cost_fact_count\" factColumn=\"StOrE_cosT\" />\n"
                + "    <AggMeasureFactCount column=\"unit_sales_fact_count\" factColumn=\"unit_SALES\" />\n"
                + "    <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"UNIT_SALES\" />\n"
                + "    <AggMeasure name=\"[Measures].[Store Cost]\" column=\"STORE_COST\" />\n"
                + "    <AggMeasure name=\"[Measures].[Store Sales]\" column=\"STORE_SALES\" />\n"
                + "    <AggLevel name=\"[Time].[Year]\" column=\"the_year\" />\n"
                + "    <AggLevel name=\"[Time].[Quarter]\" column=\"quarter\" />\n"
                + "    <AggLevel name=\"[Time].[Month]\" column=\"month_of_year\" />\n"
                + "</AggName>\n";
        */
        // aggregation tables are used, but with general fact count column
        // test uses aggregation column because right now we use reference to column.
        // previous we used column as string and mondriam used "fact_count" because "unit_SALES" != unit_sales "StOrE_cosT" != "store_cost" "STORE_SALES" != "store_sales"
        // right now it un-possible because right now we use reference to column
        String aggSql = ""
                + "select\n"
                + "    `agg_c_6_fact_csv_2016`.`the_year` as `c0`,\n"
                + "    `agg_c_6_fact_csv_2016`.`quarter` as `c1`,\n"
                + "    sum(`agg_c_6_fact_csv_2016`.`unit_sales` * `agg_c_6_fact_csv_2016`.`unit_sales_fact_count`) / sum(`agg_c_6_fact_csv_2016`.`unit_sales_fact_count`) as `m0`,\n"
                + "    sum(`agg_c_6_fact_csv_2016`.`store_cost` * `agg_c_6_fact_csv_2016`.`store_cost_fact_count`) / sum(`agg_c_6_fact_csv_2016`.`store_cost_fact_count`) as `m1`,\n"
                + "    sum(`agg_c_6_fact_csv_2016`.`store_sales` * `agg_c_6_fact_csv_2016`.`store_sales_fact_count`) / sum(`agg_c_6_fact_csv_2016`.`store_sales_fact_count`) as `m2`\n"
                + "from\n"
                + "    `agg_c_6_fact_csv_2016` as `agg_c_6_fact_csv_2016`\n"
                + "where\n"
                + "    `agg_c_6_fact_csv_2016`.`the_year` = 1997\n"
                + "group by\n"
                + "    `agg_c_6_fact_csv_2016`.`the_year`,\n"
                + "    `agg_c_6_fact_csv_2016`.`quarter`";

        assertQuerySql(context, QUERY, getAggSchema(List.of(), aggTables), aggSql);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testMeasureFactColumnNotExist(Context<?> context) {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);
        // Create aggTables using RolapMappingFactory.eINSTANCE
        var factory = RolapMappingFactory.eINSTANCE;

        PhysicalColumn notExist = factory.createPhysicalColumn();
        notExist.setName("not_exist");
        notExist.setType(ColumnType.INTEGER);

        // Create AggregationColumnName for fact count
        var aggFactCount = factory.createAggregationColumnName();
        aggFactCount.setColumn(AggMeasureFactCountTestModifierEmf.factCountAggC6FactCsv2016);

        // Create AggregationMeasureFactCount elements
        var storeSalesFactCount = factory.createAggregationMeasureFactCount();
        storeSalesFactCount.setColumn(AggMeasureFactCountTestModifierEmf.storeSalesFactCountAggC6FactCsv2016);
        storeSalesFactCount.setFactColumn(notExist);

        var storeCostFactCount = factory.createAggregationMeasureFactCount();
        storeCostFactCount.setColumn(AggMeasureFactCountTestModifierEmf.storeCostFactCountAggC6FactCsv2016);
        storeCostFactCount.setFactColumn(notExist);

        var unitSalesFactCount = factory.createAggregationMeasureFactCount();
        unitSalesFactCount.setColumn(AggMeasureFactCountTestModifierEmf.unitSalesFactCountAggC6FactCsv2016);
        unitSalesFactCount.setFactColumn(notExist);

        // Create AggregationMeasure elements
        var unitSalesMeasure = factory.createAggregationMeasure();
        unitSalesMeasure.setName("[Measures].[Unit Sales]");
        unitSalesMeasure.setColumn(AggMeasureFactCountTestModifierEmf.unitSalesAggC6FactCsv2016);

        var storeCostMeasure = factory.createAggregationMeasure();
        storeCostMeasure.setName("[Measures].[Store Cost]");
        storeCostMeasure.setColumn(AggMeasureFactCountTestModifierEmf.storeCostAggC6FactCsv2016);

        var storeSalesMeasure = factory.createAggregationMeasure();
        storeSalesMeasure.setName("[Measures].[Store Sales]");
        storeSalesMeasure.setColumn(AggMeasureFactCountTestModifierEmf.storeSalesAggC6FactCsv2016);

        // Create AggregationLevel elements
        var yearLevel = factory.createAggregationLevel();
        yearLevel.setName("[Time].[Time].[Year]");
        yearLevel.setColumn(AggMeasureFactCountTestModifierEmf.theYearAggC6FactCsv2016);

        var quarterLevel = factory.createAggregationLevel();
        quarterLevel.setName("[Time].[Time].[Quarter]");
        quarterLevel.setColumn(AggMeasureFactCountTestModifierEmf.quarterAggC6FactCsv2016);

        var monthLevel = factory.createAggregationLevel();
        monthLevel.setName("[Time].[Time].[Month]");
        monthLevel.setColumn(AggMeasureFactCountTestModifierEmf.monthOfYearAggC6FactCsv2016);

        // Create AggregationName
        var aggregationName = factory.createAggregationName();
        aggregationName.setName(AggMeasureFactCountTestModifierEmf.aggC6FactCsv2016);
        aggregationName.setAggregationFactCount(aggFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(storeSalesFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(storeCostFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(unitSalesFactCount);
        aggregationName.getAggregationMeasures().add(unitSalesMeasure);
        aggregationName.getAggregationMeasures().add(storeCostMeasure);
        aggregationName.getAggregationMeasures().add(storeSalesMeasure);
        aggregationName.getAggregationLevels().add(yearLevel);
        aggregationName.getAggregationLevels().add(quarterLevel);
        aggregationName.getAggregationLevels().add(monthLevel);

        List<AggregationTable> aggTables = List.of(aggregationName);
/*
        List<AggregationTableMappingImpl> aggTablesPojo = List.of(
            AggregationNameMappingImpl.builder()
                .withName(AggMeasureFactCountTestModifier.aggC6FactCsv2016)
                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder().withColumn(AggMeasureFactCountTestModifier.factCountAggC6FactCsv2016).build())
                .withAggregationMeasureFactCounts(List.of(
                    AggregationMeasureFactCountMappingImpl.builder()
                        .withColumn(AggMeasureFactCountTestModifier.storeSalesFactCountAggC6FactCsv2016)
                        .withFactColumn(notExist)
                        .build(),
                    AggregationMeasureFactCountMappingImpl.builder()
                        .withColumn(AggMeasureFactCountTestModifier.storeCostFactCountAggC6FactCsv2016)
                        .withFactColumn(notExist)
                        .build(),
                    AggregationMeasureFactCountMappingImpl.builder()
                        .withColumn(AggMeasureFactCountTestModifier.unitSalesFactCountAggC6FactCsv2016)
                        .withFactColumn(notExist)
                        .build()
                ))
                .withAggregationMeasures(List.of(
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Unit Sales]")
                        .withColumn(AggMeasureFactCountTestModifier.unitSalesAggC6FactCsv2016)
                        .build(),
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Store Cost]")
                        .withColumn(AggMeasureFactCountTestModifier.storeCostAggC6FactCsv2016)
                        .build(),
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Store Sales]")
                        .withColumn(AggMeasureFactCountTestModifier.storeSalesAggC6FactCsv2016)
                        .build()
                ))
                .withAggregationLevels(List.of(
                    AggregationLevelMappingImpl.builder()
                        .withName("[Time].[Time].[Year]").withColumn(AggMeasureFactCountTestModifier.theYearAggC6FactCsv2016).build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Time].[Time].[Quarter]").withColumn(AggMeasureFactCountTestModifier.quarterAggC6FactCsv2016).build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Time].[Time].[Month]").withColumn(AggMeasureFactCountTestModifier.monthOfYearAggC6FactCsv2016).build()
                ))
                .build()
        );
        */
        /*
        String agg = ""
                + "<AggName name=\"agg_c_6_fact_csv_2016\">\n"
                + "    <AggFactCount column=\"fact_count\"/>\n"
                + "    <AggMeasureFactCount column=\"store_sales_fact_count\" factColumn=\"not_exist\" />\n"
                + "    <AggMeasureFactCount column=\"store_cost_fact_count\" factColumn=\"not_exist\" />\n"
                + "    <AggMeasureFactCount column=\"unit_sales_fact_count\" factColumn=\"not_exist\" />\n"
                + "    <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"UNIT_SALES\" />\n"
                + "    <AggMeasure name=\"[Measures].[Store Cost]\" column=\"STORE_COST\" />\n"
                + "    <AggMeasure name=\"[Measures].[Store Sales]\" column=\"STORE_SALES\" />\n"
                + "    <AggLevel name=\"[Time].[Year]\" column=\"the_year\" />\n"
                + "    <AggLevel name=\"[Time].[Quarter]\" column=\"quarter\" />\n"
                + "    <AggLevel name=\"[Time].[Month]\" column=\"month_of_year\" />\n"
                + "</AggName>\n";
        */
        // aggregation tables are used, but with general fact count column
        String aggSql = ""
                + "select\n"
                + "    `agg_c_6_fact_csv_2016`.`the_year` as `c0`,\n"
                + "    `agg_c_6_fact_csv_2016`.`quarter` as `c1`,\n"
                + "    sum(`agg_c_6_fact_csv_2016`.`unit_sales` * `agg_c_6_fact_csv_2016`.`fact_count`) / sum(`agg_c_6_fact_csv_2016`.`fact_count`) as `m0`,\n"
                + "    sum(`agg_c_6_fact_csv_2016`.`store_cost` * `agg_c_6_fact_csv_2016`.`fact_count`) / sum(`agg_c_6_fact_csv_2016`.`fact_count`) as `m1`,\n"
                + "    sum(`agg_c_6_fact_csv_2016`.`store_sales` * `agg_c_6_fact_csv_2016`.`fact_count`) / sum(`agg_c_6_fact_csv_2016`.`fact_count`) as `m2`\n"
                + "from\n"
                + "    `agg_c_6_fact_csv_2016` as `agg_c_6_fact_csv_2016`\n"
                + "where\n"
                + "    `agg_c_6_fact_csv_2016`.`the_year` = 1997\n"
                + "group by\n"
                + "    `agg_c_6_fact_csv_2016`.`the_year`,\n"
                + "    `agg_c_6_fact_csv_2016`.`quarter`";

        assertQuerySql(context, QUERY, getAggSchema(List.of(), aggTables), aggSql);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testWithoutMeasureFactColumnElement(Context<?> context) {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);


        // Create aggTables using RolapMappingFactory.eINSTANCE
        var factory = RolapMappingFactory.eINSTANCE;

        // Create AggregationColumnName for fact count
        var aggFactCount = factory.createAggregationColumnName();
        aggFactCount.setColumn(AggMeasureFactCountTestModifierEmf.factCountAggC6FactCsv2016);

        // Create AggregationMeasure elements
        var unitSalesMeasure = factory.createAggregationMeasure();
        unitSalesMeasure.setName("[Measures].[Unit Sales]");
        unitSalesMeasure.setColumn(AggMeasureFactCountTestModifierEmf.unitSalesAggC6FactCsv2016);

        var storeCostMeasure = factory.createAggregationMeasure();
        storeCostMeasure.setName("[Measures].[Store Cost]");
        storeCostMeasure.setColumn(AggMeasureFactCountTestModifierEmf.storeCostAggC6FactCsv2016);

        var storeSalesMeasure = factory.createAggregationMeasure();
        storeSalesMeasure.setName("[Measures].[Store Sales]");
        storeSalesMeasure.setColumn(AggMeasureFactCountTestModifierEmf.storeSalesAggC6FactCsv2016);

        // Create AggregationLevel elements
        var yearLevel = factory.createAggregationLevel();
        yearLevel.setName("[Time].[Time].[Year]");
        yearLevel.setColumn(AggMeasureFactCountTestModifierEmf.theYearAggC6FactCsv2016);

        var quarterLevel = factory.createAggregationLevel();
        quarterLevel.setName("[Time].[Time].[Quarter]");
        quarterLevel.setColumn(AggMeasureFactCountTestModifierEmf.quarterAggC6FactCsv2016);

        var monthLevel = factory.createAggregationLevel();
        monthLevel.setName("[Time].[Time].[Month]");
        monthLevel.setColumn(AggMeasureFactCountTestModifierEmf.monthOfYearAggC6FactCsv2016);

        // Create AggregationName
        var aggregationName = factory.createAggregationName();
        aggregationName.setName(AggMeasureFactCountTestModifierEmf.aggC6FactCsv2016);
        aggregationName.setAggregationFactCount(aggFactCount);
        aggregationName.getAggregationMeasures().add(unitSalesMeasure);
        aggregationName.getAggregationMeasures().add(storeCostMeasure);
        aggregationName.getAggregationMeasures().add(storeSalesMeasure);
        aggregationName.getAggregationLevels().add(yearLevel);
        aggregationName.getAggregationLevels().add(quarterLevel);
        aggregationName.getAggregationLevels().add(monthLevel);

        List<AggregationTable> aggTables = List.of(aggregationName);

        /*
        String agg = ""
                + "<AggName name=\"agg_c_6_fact_csv_2016\">\n"
                + "    <AggFactCount column=\"fact_count\"/>\n"
                + "    <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"UNIT_SALES\" />\n"
                + "    <AggMeasure name=\"[Measures].[Store Cost]\" column=\"STORE_COST\" />\n"
                + "    <AggMeasure name=\"[Measures].[Store Sales]\" column=\"STORE_SALES\" />\n"
                + "    <AggLevel name=\"[Time].[Year]\" column=\"the_year\" />\n"
                + "    <AggLevel name=\"[Time].[Quarter]\" column=\"quarter\" />\n"
                + "    <AggLevel name=\"[Time].[Month]\" column=\"month_of_year\" />\n"
                + "</AggName>\n";
        */
        // aggregation tables are used, but with general fact count column
        String aggSql = ""
                + "select\n"
                + "    `agg_c_6_fact_csv_2016`.`the_year` as `c0`,\n"
                + "    `agg_c_6_fact_csv_2016`.`quarter` as `c1`,\n"
                + "    sum(`agg_c_6_fact_csv_2016`.`unit_sales` * `agg_c_6_fact_csv_2016`.`fact_count`) / sum(`agg_c_6_fact_csv_2016`.`fact_count`) as `m0`,\n"
                + "    sum(`agg_c_6_fact_csv_2016`.`store_cost` * `agg_c_6_fact_csv_2016`.`fact_count`) / sum(`agg_c_6_fact_csv_2016`.`fact_count`) as `m1`,\n"
                + "    sum(`agg_c_6_fact_csv_2016`.`store_sales` * `agg_c_6_fact_csv_2016`.`fact_count`) / sum(`agg_c_6_fact_csv_2016`.`fact_count`) as `m2`\n"
                + "from\n"
                + "    `agg_c_6_fact_csv_2016` as `agg_c_6_fact_csv_2016`\n"
                + "where\n"
                + "    `agg_c_6_fact_csv_2016`.`the_year` = 1997\n"
                + "group by\n"
                + "    `agg_c_6_fact_csv_2016`.`the_year`,\n"
                + "    `agg_c_6_fact_csv_2016`.`quarter`";

        assertQuerySql(context, QUERY, getAggSchema(List.of(), aggTables), aggSql);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testMeasureFactColumnAndAggFactCountNotExist(Context<?> context) {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);

        // Create aggTables using RolapMappingFactory.eINSTANCE
        var factory = RolapMappingFactory.eINSTANCE;

        PhysicalColumn notExist = factory.createPhysicalColumn();
        notExist.setName("not_exist");
        notExist.setType(ColumnType.INTEGER);

        // Create AggregationColumnName for fact count
        var aggFactCount = factory.createAggregationColumnName();
        aggFactCount.setColumn(notExist);

        // Create AggregationMeasureFactCount elements
        var storeSalesFactCount = factory.createAggregationMeasureFactCount();
        storeSalesFactCount.setColumn(AggMeasureFactCountTestModifierEmf.storeSalesFactCountAggC6FactCsv2016);
        storeSalesFactCount.setFactColumn(notExist);

        var storeCostFactCount = factory.createAggregationMeasureFactCount();
        storeCostFactCount.setColumn(AggMeasureFactCountTestModifierEmf.storeCostFactCountAggC6FactCsv2016);
        storeCostFactCount.setFactColumn(notExist);

        var unitSalesFactCount = factory.createAggregationMeasureFactCount();
        unitSalesFactCount.setColumn(AggMeasureFactCountTestModifierEmf.unitSalesFactCountAggC6FactCsv2016);
        unitSalesFactCount.setFactColumn(notExist);

        // Create AggregationMeasure elements
        var unitSalesMeasure = factory.createAggregationMeasure();
        unitSalesMeasure.setName("[Measures].[Unit Sales]");
        unitSalesMeasure.setColumn(AggMeasureFactCountTestModifierEmf.unitSalesAggC6FactCsv2016);

        var storeCostMeasure = factory.createAggregationMeasure();
        storeCostMeasure.setName("[Measures].[Store Cost]");
        storeCostMeasure.setColumn(AggMeasureFactCountTestModifierEmf.storeCostAggC6FactCsv2016);

        var storeSalesMeasure = factory.createAggregationMeasure();
        storeSalesMeasure.setName("[Measures].[Store Sales]");
        storeSalesMeasure.setColumn(AggMeasureFactCountTestModifierEmf.storeSalesAggC6FactCsv2016);

        // Create AggregationLevel elements
        var yearLevel = factory.createAggregationLevel();
        yearLevel.setName("[Time].[Time].[Year]");
        yearLevel.setColumn(AggMeasureFactCountTestModifierEmf.theYearAggC6FactCsv2016);

        var quarterLevel = factory.createAggregationLevel();
        quarterLevel.setName("[Time].[Time].[Quarter]");
        quarterLevel.setColumn(AggMeasureFactCountTestModifierEmf.quarterAggC6FactCsv2016);

        var monthLevel = factory.createAggregationLevel();
        monthLevel.setName("[Time].[Time].[Month]");
        monthLevel.setColumn(AggMeasureFactCountTestModifierEmf.monthOfYearAggC6FactCsv2016);

        // Create AggregationName
        var aggregationName = factory.createAggregationName();
        aggregationName.setName(AggMeasureFactCountTestModifierEmf.aggC6FactCsv2016);
        aggregationName.setAggregationFactCount(aggFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(storeSalesFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(storeCostFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(unitSalesFactCount);
        aggregationName.getAggregationMeasures().add(unitSalesMeasure);
        aggregationName.getAggregationMeasures().add(storeCostMeasure);
        aggregationName.getAggregationMeasures().add(storeSalesMeasure);
        aggregationName.getAggregationLevels().add(yearLevel);
        aggregationName.getAggregationLevels().add(quarterLevel);
        aggregationName.getAggregationLevels().add(monthLevel);

        List<AggregationTable> aggTables = List.of(aggregationName);
/*
        PhysicalColumnMappingImpl notExist = PhysicalColumnMappingImpl.builder().withName("not_exist").withDataType(ColumnDataType.INTEGER).build();
        List<AggregationTableMappingImpl> aggTables = List.of(
            AggregationNameMappingImpl.builder()
                .withName(AggMeasureFactCountTestModifier.aggC6FactCsv2016)
                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder().withColumn(notExist).build())
                .withAggregationMeasureFactCounts(List.of(
                    AggregationMeasureFactCountMappingImpl.builder()
                        .withColumn(AggMeasureFactCountTestModifier.storeSalesFactCountAggC6FactCsv2016)
                        .withFactColumn(notExist)
                        .build(),
                    AggregationMeasureFactCountMappingImpl.builder()
                        .withColumn(AggMeasureFactCountTestModifier.storeCostFactCountAggC6FactCsv2016)
                        .withFactColumn(notExist)
                        .build(),
                    AggregationMeasureFactCountMappingImpl.builder()
                        .withColumn(AggMeasureFactCountTestModifier.unitSalesFactCountAggC6FactCsv2016)
                        .withFactColumn(notExist)
                        .build()
                ))
                .withAggregationMeasures(List.of(
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Unit Sales]")
                        .withColumn(AggMeasureFactCountTestModifier.unitSalesAggC6FactCsv2016)
                        .build(),
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Store Cost]")
                        .withColumn(AggMeasureFactCountTestModifier.storeCostAggC6FactCsv2016)
                        .build(),
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Store Sales]")
                        .withColumn(AggMeasureFactCountTestModifier.storeSalesAggC6FactCsv2016)
                        .build()
                ))
                .withAggregationLevels(List.of(
                    AggregationLevelMappingImpl.builder()
                        .withName("[Time].[Year]").withColumn(AggMeasureFactCountTestModifier.theYearAggC6FactCsv2016).build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Time].[Quarter]").withColumn(AggMeasureFactCountTestModifier.quarterAggC6FactCsv2016).build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Time].[Month]").withColumn(AggMeasureFactCountTestModifier.monthOfYearAggC6FactCsv2016).build()
                ))
                .build()
        );
        */
        /*
        String agg = ""
                + "<AggName name=\"agg_c_6_fact_csv_2016\">\n"
                + "    <AggFactCount column=\"not_exist\"/>\n"
                + "    <AggMeasureFactCount column=\"store_sales_fact_count\" factColumn=\"not_exist\" />\n"
                + "    <AggMeasureFactCount column=\"store_cost_fact_count\" factColumn=\"not_exist\" />\n"
                + "    <AggMeasureFactCount column=\"unit_sales_fact_count\" factColumn=\"not_exist\" />\n"
                + "    <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"UNIT_SALES\" />\n"
                + "    <AggMeasure name=\"[Measures].[Store Cost]\" column=\"STORE_COST\" />\n"
                + "    <AggMeasure name=\"[Measures].[Store Sales]\" column=\"STORE_SALES\" />\n"
                + "    <AggLevel name=\"[Time].[Year]\" column=\"the_year\" />\n"
                + "    <AggLevel name=\"[Time].[Quarter]\" column=\"quarter\" />\n"
                + "    <AggLevel name=\"[Time].[Month]\" column=\"month_of_year\" />\n"
                + "</AggName>\n";
        */
        try {
            assertQuerySql(context, QUERY, getAggSchema(List.of(), aggTables), "");
            fail("Should have thrown mondrian exception");
        } catch (OlapRuntimeException e) {
            assertEquals
                    ("Too many errors, '1',"
                                    + " while loading/reloading aggregates.",
                            e.getMessage());
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testAggNameDifferentColumnNames(Context<?> context) {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);


        // Create aggTables using RolapMappingFactory.eINSTANCE
        var factory = RolapMappingFactory.eINSTANCE;

        // Create AggregationColumnName for fact count
        var aggFactCount = factory.createAggregationColumnName();
        aggFactCount.setColumn(AggMeasureFactCountTestModifierEmf.factCountAggCsvDifferentColumnNames);

        // Create AggregationMeasureFactCount elements
        var storeSalesFactCount = factory.createAggregationMeasureFactCount();
        storeSalesFactCount.setColumn(AggMeasureFactCountTestModifierEmf.ssFcAggCsvDifferentColumnNames);
        storeSalesFactCount.setFactColumn(AggMeasureFactCountTestModifierEmf.storeSalesColumnInFactCsv2016);

        var storeCostFactCount = factory.createAggregationMeasureFactCount();
        storeCostFactCount.setColumn(AggMeasureFactCountTestModifierEmf.scFcAggCsvDifferentColumnNames);
        storeCostFactCount.setFactColumn(AggMeasureFactCountTestModifierEmf.storeCostColumnInFactCsv2016);

        var unitSalesFactCount = factory.createAggregationMeasureFactCount();
        unitSalesFactCount.setColumn(AggMeasureFactCountTestModifierEmf.usFcAggCsvDifferentColumnNames);
        unitSalesFactCount.setFactColumn(AggMeasureFactCountTestModifierEmf.unitSalesColumnInFactCsv2016);

        // Create AggregationMeasure elements
        var unitSalesMeasure = factory.createAggregationMeasure();
        unitSalesMeasure.setName("[Measures].[Unit Sales]");
        unitSalesMeasure.setColumn(AggMeasureFactCountTestModifierEmf.unitSalesAggCsvDifferentColumnNames);

        var storeCostMeasure = factory.createAggregationMeasure();
        storeCostMeasure.setName("[Measures].[Store Cost]");
        storeCostMeasure.setColumn(AggMeasureFactCountTestModifierEmf.storeCostAggCsvDifferentColumnNames);

        var storeSalesMeasure = factory.createAggregationMeasure();
        storeSalesMeasure.setName("[Measures].[Store Sales]");
        storeSalesMeasure.setColumn(AggMeasureFactCountTestModifierEmf.storeSalesAggCsvDifferentColumnNames);

        // Create AggregationLevel elements
        var yearLevel = factory.createAggregationLevel();
        yearLevel.setName("[Time].[Time].[Year]");
        yearLevel.setColumn(AggMeasureFactCountTestModifierEmf.theYearAggCsvDifferentColumnNames);

        var quarterLevel = factory.createAggregationLevel();
        quarterLevel.setName("[Time].[Time].[Quarter]");
        quarterLevel.setColumn(AggMeasureFactCountTestModifierEmf.quarterAggCsvDifferentColumnNames);

        var monthLevel = factory.createAggregationLevel();
        monthLevel.setName("[Time].[Time].[Month]");
        monthLevel.setColumn(AggMeasureFactCountTestModifierEmf.monthOfYearAggCsvDifferentColumnNames);

        // Create AggregationName
        var aggregationName = factory.createAggregationName();
        aggregationName.setName(AggMeasureFactCountTestModifierEmf.aggCsvDifferentColumnNames);
        aggregationName.setAggregationFactCount(aggFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(storeSalesFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(storeCostFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(unitSalesFactCount);
        aggregationName.getAggregationMeasures().add(unitSalesMeasure);
        aggregationName.getAggregationMeasures().add(storeCostMeasure);
        aggregationName.getAggregationMeasures().add(storeSalesMeasure);
        aggregationName.getAggregationLevels().add(yearLevel);
        aggregationName.getAggregationLevels().add(quarterLevel);
        aggregationName.getAggregationLevels().add(monthLevel);

        List<AggregationTable> aggTables = List.of(aggregationName);

        AggregationExclude aggregationExclude = factory.createAggregationExclude();
        aggregationExclude.setName("agg_c_6_fact_csv_2016");
        List<AggregationExclude> aggExcludes = List.of(aggregationExclude);

        /*
        List<AggregationExcludeMappingImpl> aggExcludes = List.of(
            AggregationExcludeMappingImpl.builder()
                .withName("agg_c_6_fact_csv_2016")
                .build()
        );
        List<AggregationTableMappingImpl> aggTablesPojo = List.of(
            AggregationNameMappingImpl.builder()
                .withName(AggMeasureFactCountTestModifier.aggCsvDifferentColumnNames)
                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder().withColumn(AggMeasureFactCountTestModifier.factCountAggCsvDifferentColumnNames).build())
                .withAggregationMeasureFactCounts(List.of(
                    AggregationMeasureFactCountMappingImpl.builder()
                        .withColumn(AggMeasureFactCountTestModifier.ssFcAggCsvDifferentColumnNames)
                        .withFactColumn(AggMeasureFactCountTestModifier.STORE_SALES_COLUMN_IN_FACT_CSV_2016)
                        .build(),
                    AggregationMeasureFactCountMappingImpl.builder()
                        .withColumn(AggMeasureFactCountTestModifier.scFcAggCsvDifferentColumnNames)
                        .withFactColumn(AggMeasureFactCountTestModifier.STORE_COST_COLUMN_IN_FACT_CSV_2016)
                        .build(),
                    AggregationMeasureFactCountMappingImpl.builder()
                        .withColumn(AggMeasureFactCountTestModifier.usFcAggCsvDifferentColumnNames)
                        .withFactColumn(AggMeasureFactCountTestModifier.UNIT_SALES_COLUMN_IN_FACT_CSV_2016)
                        .build()
                ))
                .withAggregationMeasures(List.of(
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Unit Sales]")
                        .withColumn(AggMeasureFactCountTestModifier.unitSalesAggCsvDifferentColumnNames)
                        .build(),
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Store Cost]")
                        .withColumn(AggMeasureFactCountTestModifier.storeCostAggCsvDifferentColumnNames)
                        .build(),
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Store Sales]")
                        .withColumn(AggMeasureFactCountTestModifier.storeSalesAggCsvDifferentColumnNames)
                        .build()
                ))
                .withAggregationLevels(List.of(
                    AggregationLevelMappingImpl.builder()
                        .withName("[Time].[Time].[Year]").withColumn(AggMeasureFactCountTestModifier.theYearAggCsvDifferentColumnNames).build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Time].[Time].[Quarter]").withColumn(AggMeasureFactCountTestModifier.quarterAggCsvDifferentColumnNames).build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Time].[Time].[Month]").withColumn(AggMeasureFactCountTestModifier.monthOfYearAggCsvDifferentColumnNames).build()
                ))
                .build()
        );
        */
        /*
            String agg = ""
                + "<AggExclude name=\"agg_c_6_fact_csv_2016\" />"
                + "<AggName name=\"agg_csv_different_column_names\">\n"
                + "    <AggFactCount column=\"fact_count\"/>\n"
                + "    <AggMeasureFactCount column=\"ss_fc\" factColumn=\"store_sales\" />\n"
                + "    <AggMeasureFactCount column=\"sc_fc\" factColumn=\"store_cost\" />\n"
                + "    <AggMeasureFactCount column=\"us_fc\" factColumn=\"unit_sales\" />\n"
                + "    <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"UNIT_SALES\" />\n"
                + "    <AggMeasure name=\"[Measures].[Store Cost]\" column=\"STORE_COST\" />\n"
                + "    <AggMeasure name=\"[Measures].[Store Sales]\" column=\"STORE_SALES\" />\n"
                + "    <AggLevel name=\"[Time].[Year]\" column=\"the_year\" />\n"
                + "    <AggLevel name=\"[Time].[Quarter]\" column=\"quarter\" />\n"
                + "    <AggLevel name=\"[Time].[Month]\" column=\"month_of_year\" />\n"
                + "</AggName>\n";
        */
        String aggSql = ""
                + "select\n"
                + "    `agg_csv_different_column_names`.`the_year` as `c0`,\n"
                + "    `agg_csv_different_column_names`.`quarter` as `c1`,\n"
                + "    sum(`agg_csv_different_column_names`.`unit_sales` * `agg_csv_different_column_names`.`us_fc`) / sum(`agg_csv_different_column_names`.`us_fc`) as `m0`,\n"
                + "    sum(`agg_csv_different_column_names`.`store_cost` * `agg_csv_different_column_names`.`sc_fc`) / sum(`agg_csv_different_column_names`.`sc_fc`) as `m1`,\n"
                + "    sum(`agg_csv_different_column_names`.`store_sales` * `agg_csv_different_column_names`.`ss_fc`) / sum(`agg_csv_different_column_names`.`ss_fc`) as `m2`\n"
                + "from\n"
                + "    `agg_csv_different_column_names` as `agg_csv_different_column_names`\n"
                + "where\n"
                + "    `agg_csv_different_column_names`.`the_year` = 1997\n"
                + "group by\n"
                + "    `agg_csv_different_column_names`.`the_year`,\n"
                + "    `agg_csv_different_column_names`.`quarter`";

        verifySameAggAndNot(context, QUERY, getAggSchema(aggExcludes, aggTables), aggSql);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testAggDivideByZero(Context<?> context) {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);


        // Create aggTables using RolapMappingFactory.eINSTANCE
        var factory = RolapMappingFactory.eINSTANCE;

        // Create AggregationColumnName for fact count
        var aggFactCount = factory.createAggregationColumnName();
        aggFactCount.setColumn(AggMeasureFactCountTestModifierEmf.factCountAggCsvDivideByZero);

        // Create AggregationMeasureFactCount elements
        var storeSalesFactCount = factory.createAggregationMeasureFactCount();
        storeSalesFactCount.setColumn(AggMeasureFactCountTestModifierEmf.storeSalesFactCountAggCsvDivideByZero);
        storeSalesFactCount.setFactColumn(AggMeasureFactCountTestModifierEmf.storeSalesColumnInFactCsv2016);

        var storeCostFactCount = factory.createAggregationMeasureFactCount();
        storeCostFactCount.setColumn(AggMeasureFactCountTestModifierEmf.storeCostFactCountAggCsvDivideByZero);
        storeCostFactCount.setFactColumn(AggMeasureFactCountTestModifierEmf.storeCostColumnInFactCsv2016);

        var unitSalesFactCount = factory.createAggregationMeasureFactCount();
        unitSalesFactCount.setColumn(AggMeasureFactCountTestModifierEmf.unitSalesFactCountAggCsvDivideByZero);
        unitSalesFactCount.setFactColumn(AggMeasureFactCountTestModifierEmf.unitSalesColumnInFactCsv2016);

        // Create AggregationMeasure elements
        var unitSalesMeasure = factory.createAggregationMeasure();
        unitSalesMeasure.setName("[Measures].[Unit Sales]");
        unitSalesMeasure.setColumn(AggMeasureFactCountTestModifierEmf.unitSalesAggCsvDivideByZero);

        var storeCostMeasure = factory.createAggregationMeasure();
        storeCostMeasure.setName("[Measures].[Store Cost]");
        storeCostMeasure.setColumn(AggMeasureFactCountTestModifierEmf.storeCostAggCsvDivideByZero);

        var storeSalesMeasure = factory.createAggregationMeasure();
        storeSalesMeasure.setName("[Measures].[Store Sales]");
        storeSalesMeasure.setColumn(AggMeasureFactCountTestModifierEmf.storeSalesAggCsvDivideByZero);

        // Create AggregationLevel elements
        var yearLevel = factory.createAggregationLevel();
        yearLevel.setName("[Time].[Time].[Year]");
        yearLevel.setColumn(AggMeasureFactCountTestModifierEmf.theYearAggCsvDivideByZero);

        var quarterLevel = factory.createAggregationLevel();
        quarterLevel.setName("[Time].[Time].[Quarter]");
        quarterLevel.setColumn(AggMeasureFactCountTestModifierEmf.quarterAggCsvDivideByZero);

        var monthLevel = factory.createAggregationLevel();
        monthLevel.setName("[Time].[Time].[Month]");
        monthLevel.setColumn(AggMeasureFactCountTestModifierEmf.monthOfYearAggCsvDivideByZero);

        // Create AggregationName
        var aggregationName = factory.createAggregationName();
        aggregationName.setName(AggMeasureFactCountTestModifierEmf.aggCsvDivideByZero);
        aggregationName.setAggregationFactCount(aggFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(storeSalesFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(storeCostFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(unitSalesFactCount);
        aggregationName.getAggregationMeasures().add(unitSalesMeasure);
        aggregationName.getAggregationMeasures().add(storeCostMeasure);
        aggregationName.getAggregationMeasures().add(storeSalesMeasure);
        aggregationName.getAggregationLevels().add(yearLevel);
        aggregationName.getAggregationLevels().add(quarterLevel);
        aggregationName.getAggregationLevels().add(monthLevel);

        List<AggregationTable> aggTables = List.of(aggregationName);

        AggregationExclude aggregationExclude = factory.createAggregationExclude();
        aggregationExclude.setName("agg_c_6_fact_csv_2016");
        List<AggregationExclude> aggExcludes = List.of(aggregationExclude);


        /*
        List<AggregationExcludeMappingImpl> aggExcludesPojo = List.of(
            AggregationExcludeMappingImpl.builder()
                .withName("agg_c_6_fact_csv_2016")
                .build()
        );
        List<AggregationTableMappingImpl> aggTablesPojo = List.of(
            AggregationNameMappingImpl.builder()
                .withName(AggMeasureFactCountTestModifier.aggCsvDivideByZero)
                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder().withColumn(AggMeasureFactCountTestModifier.factCountAggCsvDivideByZero).build())
                .withAggregationMeasureFactCounts(List.of(
                    AggregationMeasureFactCountMappingImpl.builder()
                        .withColumn(AggMeasureFactCountTestModifier.storeSalesFactCountAggCsvDivideByZero)
                        .withFactColumn(AggMeasureFactCountTestModifier.STORE_SALES_COLUMN_IN_FACT_CSV_2016)
                        .build(),
                    AggregationMeasureFactCountMappingImpl.builder()
                        .withColumn(AggMeasureFactCountTestModifier.storeCostFactCountAggCsvDivideByZero)
                        .withFactColumn(AggMeasureFactCountTestModifier.STORE_COST_COLUMN_IN_FACT_CSV_2016)
                        .build(),
                    AggregationMeasureFactCountMappingImpl.builder()
                        .withColumn(AggMeasureFactCountTestModifier.unitSalesFactCountAggCsvDivideByZero)
                        .withFactColumn(AggMeasureFactCountTestModifier.UNIT_SALES_COLUMN_IN_FACT_CSV_2016)
                        .build()
                ))
                .withAggregationMeasures(List.of(
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Unit Sales]")
                        .withColumn(AggMeasureFactCountTestModifier.unitSalesAggCsvDivideByZero)
                        .build(),
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Store Cost]")
                        .withColumn(AggMeasureFactCountTestModifier.storeCostAggCsvDivideByZero)
                        .build(),
                    AggregationMeasureMappingImpl.builder()
                        .withName("[Measures].[Store Sales]")
                        .withColumn(AggMeasureFactCountTestModifier.storeSalesAggCsvDivideByZero)
                        .build()
                ))
                .withAggregationLevels(List.of(
                    AggregationLevelMappingImpl.builder()
                        .withName("[Time].[Time].[Year]").withColumn(AggMeasureFactCountTestModifier.theYearAggCsvDivideByZero).build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Time].[Time].[Quarter]").withColumn(AggMeasureFactCountTestModifier.quarterAggCsvDivideByZero).build(),
                    AggregationLevelMappingImpl.builder()
                        .withName("[Time].[Time].[Month]").withColumn(AggMeasureFactCountTestModifier.monthOfYearAggCsvDivideByZero).build()
                    ))
                .build()
        );
        */
        String result = ""
                + "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Time].[Time].[1997].[Q1]}\n"
                + "{[Time].[Time].[1997].[Q2]}\n"
                + "{[Time].[Time].[1997].[Q3]}\n"
                + "{[Time].[Time].[1997].[Q4]}\n"
                + "Axis #2:\n"
                + "{[Measures].[Store Sales]}\n"
                + "{[Measures].[Store Cost]}\n"
                + "{[Measures].[Unit Sales]}\n"
                + "Row #0: \n"
                + "Row #0: 1.00\n"
                + "Row #0: 1.00\n"
                + "Row #0: 1.00\n"
                + "Row #1: 2.00\n"
                + "Row #1: 2.00\n"
                + "Row #1: 2.00\n"
                + "Row #1: 2.00\n"
                + "Row #2: 3\n"
                + "Row #2: 3\n"
                + "Row #2: 3\n"
                + "Row #2: 3\n";

        withSchemaEmf(context, getAggSchema(aggExcludes, aggTables));
        assertQueryReturns(context.getConnectionWithDefaultRole(), QUERY, result);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testAggPattern(Context<?> context) {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);

        // Create aggTables using RolapMappingFactory.eINSTANCE
        var factory = RolapMappingFactory.eINSTANCE;

        // Create AggregationColumnName for fact count
        var aggFactCount = factory.createAggregationColumnName();
        aggFactCount.setColumn(AggMeasureFactCountTestModifierEmf.factCountAggC6FactCsv2016);

        // Create AggregationMeasureFactCount elements
        var storeSalesFactCount = factory.createAggregationMeasureFactCount();
        storeSalesFactCount.setColumn(AggMeasureFactCountTestModifierEmf.storeSalesFactCountAggC6FactCsv2016);
        storeSalesFactCount.setFactColumn(AggMeasureFactCountTestModifierEmf.storeSalesColumnInFactCsv2016);

        var storeCostFactCount = factory.createAggregationMeasureFactCount();
        storeCostFactCount.setColumn(AggMeasureFactCountTestModifierEmf.storeCostFactCountAggC6FactCsv2016);
        storeCostFactCount.setFactColumn(AggMeasureFactCountTestModifierEmf.storeCostColumnInFactCsv2016);

        var unitSalesFactCount = factory.createAggregationMeasureFactCount();
        unitSalesFactCount.setColumn(AggMeasureFactCountTestModifierEmf.unitSalesFactCountAggC6FactCsv2016);
        unitSalesFactCount.setFactColumn(AggMeasureFactCountTestModifierEmf.unitSalesColumnInFactCsv2016);

        // Create AggregationMeasure elements
        var unitSalesMeasure = factory.createAggregationMeasure();
        unitSalesMeasure.setName("[Measures].[Unit Sales]");
        unitSalesMeasure.setColumn(AggMeasureFactCountTestModifierEmf.unitSalesAggC6FactCsv2016);

        var storeCostMeasure = factory.createAggregationMeasure();
        storeCostMeasure.setName("[Measures].[Store Cost]");
        storeCostMeasure.setColumn(AggMeasureFactCountTestModifierEmf.storeCostAggC6FactCsv2016);

        var storeSalesMeasure = factory.createAggregationMeasure();
        storeSalesMeasure.setName("[Measures].[Store Sales]");
        storeSalesMeasure.setColumn(AggMeasureFactCountTestModifierEmf.storeSalesAggC6FactCsv2016);

        // Create AggregationLevel elements
        var yearLevel = factory.createAggregationLevel();
        yearLevel.setName("[Time].[Time].[Year]");
        yearLevel.setColumn(AggMeasureFactCountTestModifierEmf.theYearAggC6FactCsv2016);

        var quarterLevel = factory.createAggregationLevel();
        quarterLevel.setName("[Time].[Time].[Quarter]");
        quarterLevel.setColumn(AggMeasureFactCountTestModifierEmf.quarterAggC6FactCsv2016);

        var monthLevel = factory.createAggregationLevel();
        monthLevel.setName("[Time].[Time].[Month]");
        monthLevel.setColumn(AggMeasureFactCountTestModifierEmf.monthOfYearAggC6FactCsv2016);

        // Create AggregationName
        var aggregationName = factory.createAggregationPattern();
        aggregationName.setPattern("agg_c_6_fact_csv_2016");
        aggregationName.setAggregationFactCount(aggFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(storeSalesFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(storeCostFactCount);
        aggregationName.getAggregationMeasureFactCounts().add(unitSalesFactCount);
        aggregationName.getAggregationMeasures().add(unitSalesMeasure);
        aggregationName.getAggregationMeasures().add(storeCostMeasure);
        aggregationName.getAggregationMeasures().add(storeSalesMeasure);
        aggregationName.getAggregationLevels().add(yearLevel);
        aggregationName.getAggregationLevels().add(quarterLevel);
        aggregationName.getAggregationLevels().add(monthLevel);

        List<AggregationTable> aggTables = List.of(aggregationName);

        String aggSql = ""
                + "select\n"
                + "    `agg_c_6_fact_csv_2016`.`the_year` as `c0`,\n"
                + "    `agg_c_6_fact_csv_2016`.`quarter` as `c1`,\n"
                + "    sum(`agg_c_6_fact_csv_2016`.`unit_sales` * `agg_c_6_fact_csv_2016`.`unit_sales_fact_count`) / sum(`agg_c_6_fact_csv_2016`.`unit_sales_fact_count`) as `m0`,\n"
                + "    sum(`agg_c_6_fact_csv_2016`.`store_cost` * `agg_c_6_fact_csv_2016`.`store_cost_fact_count`) / sum(`agg_c_6_fact_csv_2016`.`store_cost_fact_count`) as `m1`,\n"
                + "    sum(`agg_c_6_fact_csv_2016`.`store_sales` * `agg_c_6_fact_csv_2016`.`store_sales_fact_count`) / sum(`agg_c_6_fact_csv_2016`.`store_sales_fact_count`) as `m2`\n"
                + "from\n"
                + "    `agg_c_6_fact_csv_2016` as `agg_c_6_fact_csv_2016`\n"
                + "where\n"
                + "    `agg_c_6_fact_csv_2016`.`the_year` = 1997\n"
                + "group by\n"
                + "    `agg_c_6_fact_csv_2016`.`the_year`,\n"
                + "    `agg_c_6_fact_csv_2016`.`quarter`";

        verifySameAggAndNot(context, QUERY, getAggSchema(List.of(), aggTables), aggSql);
    }

    private Function<Catalog, CatalogMappingSupplier> getAggSchema(List<AggregationExclude> aggExcludes, List<AggregationTable> aggTables) {
        class AggMeasureFactCountTestModifierInner extends AggMeasureFactCountTestModifierEmf {

            public AggMeasureFactCountTestModifierInner(Catalog catalogMapping) {
                super(catalogMapping);
            }

            @Override
            protected List<AggregationTable> getAggTables() {
                return aggTables;
            }

            @Override
            protected List<AggregationExclude> getAggExcludes() {
                return aggExcludes;
            }
        }
        return AggMeasureFactCountTestModifierInner::new;
    }

    private void verifySameAggAndNot(Context<?> context, String query, Function<Catalog, CatalogMappingSupplier> mf) {
        withSchemaEmf(context, mf);
        Result resultWithAgg =
                executeQuery(query, context.getConnectionWithDefaultRole());
        ((TestContextImpl)context).setUseAggregates(false);
        ((TestContextImpl)context).setReadAggregates(false);
        Result result = executeQuery(query, context.getConnectionWithDefaultRole());

        String resultStr = TestUtil.toString(result);
        String resultWithAggStr = TestUtil.toString(resultWithAgg);
        assertEquals(
        		resultStr,
        		resultWithAggStr,
        		"Results with and without agg table should be equal");
    }

    private void verifySameAggAndNot
            (Context<?> context, String query, Function<Catalog, CatalogMappingSupplier> mf, String aggSql) {
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        // check that agg tables are used
        assertQuerySql(context, QUERY, mf, aggSql);

        verifySameAggAndNot(context, query, mf);
    }

    private void assertQuerySql
            (Context<?> context, String query, Function<Catalog, CatalogMappingSupplier> mf, String sql) {

        withSchemaEmf(context, mf);
        //withFreshConnection();
        assertQuerySql
                (context.getConnectionWithDefaultRole(), query, new SqlPattern[]
                        {
                                new SqlPattern
                                        (DatabaseProduct.MYSQL,
                                                sql,
                                                sql.length())
                        });
    }



}
