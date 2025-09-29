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
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.Column;
import org.eclipse.daanse.rolap.mapping.model.ColumnType;
import org.eclipse.daanse.rolap.mapping.model.PhysicalColumn;
import org.eclipse.daanse.rolap.mapping.model.PhysicalTable;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
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
    private static PhysicalColumn theYearAggCAvgSalesFact1997 = createColumn("the_year", ColumnType.INTEGER, null, null, null);
    private static PhysicalColumn quarterAggCAvgSalesFact1997 = createColumn("quarter", ColumnType.VARCHAR, 30, null, null);
    private static PhysicalColumn monthOfYearAggCAvgSalesFact1997 = createColumn("month_of_year", ColumnType.INTEGER, null, null, null);
    private static PhysicalColumn genderAggCAvgSalesFact1997 = createColumn("gender", ColumnType.VARCHAR, 30, null, null);
    private static PhysicalColumn unitSalesAggCAvgSalesFact1997 = createColumn("unit_sales", ColumnType.INTEGER, null, null, null);
    private static PhysicalColumn factCountAggCAvgSalesFact1997 = createColumn("fact_count", ColumnType.INTEGER, null, null, null);

    private static PhysicalTable aggCAvgSalesFact1997 = RolapMappingFactory.eINSTANCE.createPhysicalTable();
    static {
        aggCAvgSalesFact1997.setName("agg_c_avg_sales_fact_1997");
        aggCAvgSalesFact1997.getColumns().add(theYearAggCAvgSalesFact1997);
        aggCAvgSalesFact1997.getColumns().add(quarterAggCAvgSalesFact1997);
        aggCAvgSalesFact1997.getColumns().add(monthOfYearAggCAvgSalesFact1997);
        aggCAvgSalesFact1997.getColumns().add(genderAggCAvgSalesFact1997);
        aggCAvgSalesFact1997.getColumns().add(unitSalesAggCAvgSalesFact1997);
        aggCAvgSalesFact1997.getColumns().add(factCountAggCAvgSalesFact1997);
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

    private static PhysicalColumn createColumn(String name, ColumnType dataType, Integer charOctetLength, Integer columnSize, Integer decimalDigits) {
        PhysicalColumn column = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        column.setName(name);

        column.setType(dataType);

        if (charOctetLength != null) {
            column.setCharOctetLength(charOctetLength);
        }
        if (columnSize != null) {
            column.setColumnSize(columnSize);
        }
        if (decimalDigits != null) {
            column.setDecimalDigits(decimalDigits);
        }

        return column;
    }
}
