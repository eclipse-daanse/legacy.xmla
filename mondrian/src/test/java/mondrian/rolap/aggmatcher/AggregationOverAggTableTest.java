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

import org.eclipse.daanse.cwm.model.cwm.resource.relational.Column;
import org.eclipse.daanse.cwm.model.cwm.resource.relational.Table;
import org.eclipse.daanse.cwm.util.resource.relational.SqlSimpleTypes;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.model.catalog.Catalog;
import org.eclipse.daanse.rolap.mapping.model.catalog.impl.CatalogImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;
/**
 * @author Andrey Khayrutdinov
 */
class AggregationOverAggTableTest extends AggTableTestCase {

	//## TableName:  agg_c_avg_sales_fact_1997
	//## ColumnNames:  the_year,quarter,month_of_year,gender,unit_sales,fact_count
	//## ColumnTypes: INTEGER,VARCHAR(30),INTEGER,VARCHAR(30),INTEGER:NULL,INTEGER
    private static Column theYearAggCAvgSalesFact1997 = createColumn("the_year", SqlSimpleTypes.Sql99.integerType(), null, null, null);
    private static Column quarterAggCAvgSalesFact1997 = createColumn("quarter", SqlSimpleTypes.varcharType(255), 30, null, null);
    private static Column monthOfYearAggCAvgSalesFact1997 = createColumn("month_of_year", SqlSimpleTypes.Sql99.integerType(), null, null, null);
    private static Column genderAggCAvgSalesFact1997 = createColumn("gender", SqlSimpleTypes.varcharType(255), 30, null, null);
    private static Column unitSalesAggCAvgSalesFact1997 = createColumn("unit_sales", SqlSimpleTypes.Sql99.integerType(), null, null, null);
    private static Column factCountAggCAvgSalesFact1997 = createColumn("fact_count", SqlSimpleTypes.Sql99.integerType(), null, null, null);

    private static Table aggCAvgSalesFact1997 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
    static {
        aggCAvgSalesFact1997.setName("agg_c_avg_sales_fact_1997");
        aggCAvgSalesFact1997.getFeature().add(theYearAggCAvgSalesFact1997);
        aggCAvgSalesFact1997.getFeature().add(quarterAggCAvgSalesFact1997);
        aggCAvgSalesFact1997.getFeature().add(monthOfYearAggCAvgSalesFact1997);
        aggCAvgSalesFact1997.getFeature().add(genderAggCAvgSalesFact1997);
        aggCAvgSalesFact1997.getFeature().add(unitSalesAggCAvgSalesFact1997);
        aggCAvgSalesFact1997.getFeature().add(factCountAggCAvgSalesFact1997);
    }


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
    void testAvgMeasureLowestGranularity(Context<?> context) throws Exception {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);
        Catalog catalogMapping = new CatalogSupplier().get();
        EcoreUtil.Copier copier = org.opencube.junit5.EmfUtil.copier((CatalogImpl) catalogMapping);
        Catalog catalog = (Catalog) copier.get(catalogMapping);

        ExplicitRecognizerTest.setupMultiColDimCube(catalog, copier, context,
            List.of(),
            (Column)copier.get(CatalogSupplier.COLUMN_THE_YEAR_TIME_BY_DAY),
            (Column)copier.get(CatalogSupplier.COLUMN_QUARTER_TIME_BY_DAY),
            (Column)copier.get(CatalogSupplier.COLUMN_MONTH_OF_YEAR_TIME_BY_DAY), null, null, null,
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
            + "{[TimeExtra].[TimeExtra].[1997].[Q1].[1], [Gender].[Gender].[M]}\n"
            + "{[TimeExtra].[TimeExtra].[1997].[Q1].[2], [Gender].[Gender].[M]}\n"
            + "{[TimeExtra].[TimeExtra].[1997].[Q1].[3], [Gender].[Gender].[M]}\n"
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
