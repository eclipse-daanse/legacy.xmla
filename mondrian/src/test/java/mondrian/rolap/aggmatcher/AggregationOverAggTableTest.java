/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2015-2017 Hitachi Vantara and others
// All Rights Reserved.
 */
package mondrian.rolap.aggmatcher;

import static org.opencube.junit5.TestUtil.assertQueryReturns;

import java.util.List;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.rdb.structure.pojo.ColumnImpl;
import org.eclipse.daanse.rdb.structure.pojo.PhysicalTableImpl;
import org.eclipse.daanse.rdb.structure.pojo.PhysicalTableImpl.Builder;
import org.eclipse.daanse.rolap.mapping.instance.rec.complex.foodmart.FoodmartMappingSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestConfig;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.SystemWideProperties;

/**
 * @author Andrey Khayrutdinov
 */
class AggregationOverAggTableTest extends AggTableTestCase {

	//## TableName:  agg_c_avg_sales_fact_1997
	//## ColumnNames:  the_year,quarter,month_of_year,gender,unit_sales,fact_count
	//## ColumnTypes: INTEGER,VARCHAR(30),INTEGER,VARCHAR(30),INTEGER:NULL,INTEGER
    ColumnImpl theYearAggCAvgSalesFact1997 = ColumnImpl.builder().withName("the_year").withType("INTEGER").build();
    ColumnImpl quarterAggCAvgSalesFact1997 = ColumnImpl.builder().withName("quarter").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnImpl monthOfYearAggCAvgSalesFact1997 = ColumnImpl.builder().withName("month_of_year").withType("INTEGER").build();
    ColumnImpl genderAggCAvgSalesFact1997 = ColumnImpl.builder().withName("gender").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnImpl unitSalesAggCAvgSalesFact1997 = ColumnImpl.builder().withName("unit_sales").withType("INTEGER").withNullable(true).build();
    ColumnImpl factCountAggCAvgSalesFact1997 = ColumnImpl.builder().withName("fact_count").withType("INTEGER").build();
    PhysicalTableImpl aggCAvgSalesFact1997 = ((Builder) PhysicalTableImpl.builder().withName("agg_c_avg_sales_fact_1997")
            .withColumns(List.of(
                theYearAggCAvgSalesFact1997,
                quarterAggCAvgSalesFact1997,
                monthOfYearAggCAvgSalesFact1997,
                genderAggCAvgSalesFact1997,
                unitSalesAggCAvgSalesFact1997,
                factCountAggCAvgSalesFact1997
            ))).build();


    @Override
    protected String getFileName() {
        return "aggregation-over-agg-table.csv";
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

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testAvgMeasureLowestGranularity(Context context) throws Exception {
        ((TestConfig)context.getConfig()).setGenerateFormattedSql(true);
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        ((TestConfig)context.getConfig()).setDisableCaching(true);
        prepareContext(context);
        ExplicitRecognizerTest.setupMultiColDimCube(context,
            List.of(),
            FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY,
            FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY, null, null, null,
            List.of(), List.of(aggCAvgSalesFact1997));

        String query =
            "select {[Measures].[Avg Unit Sales]} on columns, "
            + "non empty CrossJoin({[TimeExtra].[1997].[Q1].Children},{[Gender].[M]}) on rows "
            + "from [ExtraCol]";

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            query,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Avg Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[TimeExtra].[1997].[Q1].[1], [Gender].[M]}\n"
            + "{[TimeExtra].[1997].[Q1].[2], [Gender].[M]}\n"
            + "{[TimeExtra].[1997].[Q1].[3], [Gender].[M]}\n"
            + "Row #0: 3\n"
            + "Row #1: 3\n"
            + "Row #2: 3\n");

        assertQuerySqlOrNot(
            context.getConnectionWithDefaultRole(),
            query,
            mysqlPattern(
                "select\n"
                + "    `agg_c_avg_sales_fact_1997`.`the_year` as `c0`,\n"
                + "    `agg_c_avg_sales_fact_1997`.`quarter` as `c1`,\n"
                + "    `agg_c_avg_sales_fact_1997`.`month_of_year` as `c2`,\n"
                + "    `agg_c_avg_sales_fact_1997`.`gender` as `c3`,\n"
                + "    (`agg_c_avg_sales_fact_1997`.`unit_sales`) / (`agg_c_avg_sales_fact_1997`.`fact_count`) as `m0`\n"
                + "from\n"
                + "    `agg_c_avg_sales_fact_1997` as `agg_c_avg_sales_fact_1997`\n"
                + "where\n"
                + "    `agg_c_avg_sales_fact_1997`.`the_year` = 1997\n"
                + "and\n"
                + "    `agg_c_avg_sales_fact_1997`.`quarter` = 'Q1'\n"
                + "and\n"
                + "    `agg_c_avg_sales_fact_1997`.`month_of_year` in (1, 2, 3)\n"
                + "and\n"
                + "    `agg_c_avg_sales_fact_1997`.`gender` = 'M'"),
            false, false, true);
    }
}
