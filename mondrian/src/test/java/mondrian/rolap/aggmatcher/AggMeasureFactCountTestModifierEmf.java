/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package mondrian.rolap.aggmatcher;

import java.util.List;

import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.model.AggregationExclude;
import org.eclipse.daanse.rolap.mapping.model.AggregationTable;
import org.eclipse.daanse.rolap.mapping.model.AvgMeasure;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.Column;
import org.eclipse.daanse.rolap.mapping.model.ColumnInternalDataType;
import org.eclipse.daanse.rolap.mapping.model.ColumnType;
import org.eclipse.daanse.rolap.mapping.model.DatabaseSchema;
import org.eclipse.daanse.rolap.mapping.model.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.Level;
import org.eclipse.daanse.rolap.mapping.model.LevelDefinition;
import org.eclipse.daanse.rolap.mapping.model.MeasureGroup;
import org.eclipse.daanse.rolap.mapping.model.MemberProperty;
import org.eclipse.daanse.rolap.mapping.model.PhysicalColumn;
import org.eclipse.daanse.rolap.mapping.model.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.PhysicalTable;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.StandardDimension;
import org.eclipse.daanse.rolap.mapping.model.Table;
import org.eclipse.daanse.rolap.mapping.model.TableQuery;
import org.eclipse.daanse.rolap.mapping.model.TimeDimension;
import org.eclipse.daanse.rolap.mapping.model.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.opencube.junit5.EmfUtil;

public class AggMeasureFactCountTestModifierEmf implements CatalogMappingSupplier {

    public final Catalog catalog;
    public final EcoreUtil.Copier copier;

    // Time CSV table columns
    public static PhysicalColumn timeIdColumnInTimeCsv = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn theYearColumnInTimeCsv = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn monthOfYearColumnInTimeCsv = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn quarterColumnInTimeCsv = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn dayOfMonthColumnTimeCsv = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn weekOfYearColumnInTimeCsv = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalTable timeCsvTable = RolapMappingFactory.eINSTANCE.createPhysicalTable();

    // Fact CSV 2016 table columns
    public static PhysicalColumn productIdColumnInFactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn timeIdColumnInFactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn customerIdColumnInFactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn promotionIdColumnInFactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn storeIdColumnInFactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn storeSalesColumnInFactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn storeCostColumnInFactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn unitSalesColumnInFactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalTable factCsv2016Table = RolapMappingFactory.eINSTANCE.createPhysicalTable();

    // Aggregation table: agg_c_6_fact_csv_2016
    public static PhysicalColumn monthOfYearAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn quarterAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn theYearAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn storeSalesAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn storeCostAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn unitSalesAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public PhysicalColumn customerCountAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn factCountAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn storeSalesFactCountAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn storeCostFactCountAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn unitSalesFactCountAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalTable aggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalTable();

    // Aggregation table: agg_csv_different_column_names
    public static PhysicalColumn monthOfYearAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn quarterAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn theYearAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn storeSalesAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn storeCostAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn unitSalesAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn customerCountAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn factCountAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn ssFcAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn scFcAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn usFcAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalTable aggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalTable();

    // Aggregation table: agg_csv_divide_by_zero
    public static PhysicalColumn monthOfYearAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn quarterAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn theYearAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn storeSalesAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn storeCostAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn unitSalesAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn customerCountAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn factCountAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn storeSalesFactCountAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn storeCostFactCountAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalColumn unitSalesFactCountAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
    public static PhysicalTable aggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalTable();

    // Dimensions
    public static StandardDimension storeDimension;
    public static TimeDimension timeDimension;

    // Measures
    public static AvgMeasure unitSalesMeasure;

    public AggMeasureFactCountTestModifierEmf(Catalog catalogMapping) {
        copier = EmfUtil.copier((CatalogImpl) catalogMapping);
        this.catalog = (CatalogImpl) copier.get(catalogMapping);
        createTables();
        createDimensions();
        createMeasures();
        createCube();
    }

    /*
        + "<Schema name=\"FoodMart\">\n"
        + "<Dimension name=\"Time\" type=\"TimeDimension\">\n"
        + "    <Hierarchy hasAll=\"false\" primaryKey=\"time_id\">\n"
        + "      <Table name=\"time_csv\"/>\n"
        + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
        + "          levelType=\"TimeYears\"/>\n"
        + "      <Level name=\"Quarter\" column=\"quarter\" uniqueMembers=\"false\"\n"
        + "          levelType=\"TimeQuarters\"/>\n"
        + "      <Level name=\"Month\" column=\"month_of_year\" uniqueMembers=\"false\" type=\"Numeric\"\n"
        + "          levelType=\"TimeMonths\"/>\n"
        + "    </Hierarchy>\n"
        + "    <Hierarchy hasAll=\"true\" name=\"Weekly\" primaryKey=\"time_id\">\n"
        + "      <Table name=\"time_csv\"/>\n"
        + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
        + "          levelType=\"TimeYears\"/>\n"
        + "      <Level name=\"Week\" column=\"week_of_year\" type=\"Numeric\" uniqueMembers=\"false\"\n"
        + "          levelType=\"TimeWeeks\"/>\n"
        + "      <Level name=\"Day\" column=\"day_of_month\" uniqueMembers=\"false\" type=\"Numeric\"\n"
        + "          levelType=\"TimeDays\"/>\n"
        + "    </Hierarchy>\n"
        + "  </Dimension>\n"
        + "<Dimension name=\"Store\">\n"
        + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">\n"
        + "      <Table name=\"store\"/>\n"
        + "      <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"/>\n"
        + "      <Level name=\"Store State\" column=\"store_state\" uniqueMembers=\"true\"/>\n"
        + "      <Level name=\"Store City\" column=\"store_city\" uniqueMembers=\"false\"/>\n"
        + "      <Level name=\"Store Name\" column=\"store_name\" uniqueMembers=\"true\">\n"
        + "        <Property name=\"Store Type\" column=\"store_type\"/>\n"
        + "        <Property name=\"Store Manager\" column=\"store_manager\"/>\n"
        + "        <Property name=\"Store Sqft\" column=\"store_sqft\" type=\"Numeric\"/>\n"
        + "        <Property name=\"Grocery Sqft\" column=\"grocery_sqft\" type=\"Numeric\"/>\n"
        + "        <Property name=\"Frozen Sqft\" column=\"frozen_sqft\" type=\"Numeric\"/>\n"
        + "        <Property name=\"Meat Sqft\" column=\"meat_sqft\" type=\"Numeric\"/>\n"
        + "        <Property name=\"Has coffee bar\" column=\"coffee_bar\" type=\"Boolean\"/>\n"
        + "        <Property name=\"Street address\" column=\"store_street_address\" type=\"String\"/>\n"
        + "      </Level>\n"
        + "    </Hierarchy>\n"
        + "  </Dimension>"
        + "<Cube name=\"Sales\" defaultMeasure=\"Unit Sales\"> \n"
        + "<Table name=\"fact_csv_2016\"> \n"

        // add aggregation table here
        + "%AGG_DESCRIPTION_HERE%"

        + "</Table> \n"
        + "<DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/> \n"
        + "<DimensionUsage name=\"Store\" source=\"Store\" foreignKey=\"store_id\"/>"
        + "<Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"avg\"\n"
        + "   formatString=\"Standard\"/>\n"
        + "<Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"avg\"\n"
        + "   formatString=\"#,###.00\"/>\n"
        + "<Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"avg\"\n"
        + "   formatString=\"#,###.00\"/>\n"
        + "</Cube>\n"
        + "</Schema>";

     */

    protected void createTables() {
        // Create time_csv table columns
        //timeIdColumnInTimeCsv = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        timeIdColumnInTimeCsv.setName("time_id");
        timeIdColumnInTimeCsv.setType(ColumnType.INTEGER);

        //theYearColumnInTimeCsv = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        theYearColumnInTimeCsv.setName("the_year");
        theYearColumnInTimeCsv.setType(ColumnType.SMALLINT);

        //monthOfYearColumnInTimeCsv = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        monthOfYearColumnInTimeCsv.setName("month_of_year");
        monthOfYearColumnInTimeCsv.setType(ColumnType.SMALLINT);

        //quarterColumnInTimeCsv = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        quarterColumnInTimeCsv.setName("quarter");
        quarterColumnInTimeCsv.setType(ColumnType.VARCHAR);
        quarterColumnInTimeCsv.setCharOctetLength(30);

        //dayOfMonthColumnTimeCsv = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        dayOfMonthColumnTimeCsv.setName("day_of_month");
        dayOfMonthColumnTimeCsv.setType(ColumnType.SMALLINT);

        //weekOfYearColumnInTimeCsv = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        weekOfYearColumnInTimeCsv.setName("week_of_year");
        weekOfYearColumnInTimeCsv.setType(ColumnType.INTEGER);

        //timeCsvTable = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        timeCsvTable.setName("time_csv");
        timeCsvTable.getColumns().add(timeIdColumnInTimeCsv);
        timeCsvTable.getColumns().add(theYearColumnInTimeCsv);
        timeCsvTable.getColumns().add(monthOfYearColumnInTimeCsv);
        timeCsvTable.getColumns().add(quarterColumnInTimeCsv);
        timeCsvTable.getColumns().add(weekOfYearColumnInTimeCsv);
        timeCsvTable.getColumns().add(dayOfMonthColumnTimeCsv);

        // Create fact_csv_2016 table columns
        //productIdColumnInFactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        productIdColumnInFactCsv2016.setName("product_id");
        productIdColumnInFactCsv2016.setType(ColumnType.INTEGER);

        //timeIdColumnInFactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        timeIdColumnInFactCsv2016.setName("time_id");
        timeIdColumnInFactCsv2016.setType(ColumnType.INTEGER);

        //customerIdColumnInFactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        customerIdColumnInFactCsv2016.setName("customer_id");
        customerIdColumnInFactCsv2016.setType(ColumnType.INTEGER);

        //promotionIdColumnInFactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        promotionIdColumnInFactCsv2016.setName("promotion_id");
        promotionIdColumnInFactCsv2016.setType(ColumnType.INTEGER);

        //storeIdColumnInFactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        storeIdColumnInFactCsv2016.setName("store_id");
        storeIdColumnInFactCsv2016.setType(ColumnType.INTEGER);

        //storeSalesColumnInFactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        storeSalesColumnInFactCsv2016.setName("store_sales");
        storeSalesColumnInFactCsv2016.setType(ColumnType.DECIMAL);
        storeSalesColumnInFactCsv2016.setColumnSize(10);
        storeSalesColumnInFactCsv2016.setDecimalDigits(4);
        storeSalesColumnInFactCsv2016.setNullable(true);

        //storeCostColumnInFactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        storeCostColumnInFactCsv2016.setName("store_cost");
        storeCostColumnInFactCsv2016.setType(ColumnType.DECIMAL);
        storeCostColumnInFactCsv2016.setColumnSize(10);
        storeCostColumnInFactCsv2016.setDecimalDigits(4);
        storeCostColumnInFactCsv2016.setNullable(true);

        //unitSalesColumnInFactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        unitSalesColumnInFactCsv2016.setName("unit_sales");
        unitSalesColumnInFactCsv2016.setType(ColumnType.DECIMAL);
        unitSalesColumnInFactCsv2016.setColumnSize(10);
        unitSalesColumnInFactCsv2016.setDecimalDigits(4);
        unitSalesColumnInFactCsv2016.setNullable(true);

        //factCsv2016Table = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        factCsv2016Table.setName("fact_csv_2016");
        factCsv2016Table.getColumns().add(productIdColumnInFactCsv2016);
        factCsv2016Table.getColumns().add(timeIdColumnInFactCsv2016);
        factCsv2016Table.getColumns().add(customerIdColumnInFactCsv2016);
        factCsv2016Table.getColumns().add(promotionIdColumnInFactCsv2016);
        factCsv2016Table.getColumns().add(storeIdColumnInFactCsv2016);
        factCsv2016Table.getColumns().add(storeSalesColumnInFactCsv2016);
        factCsv2016Table.getColumns().add(storeCostColumnInFactCsv2016);
        factCsv2016Table.getColumns().add(unitSalesColumnInFactCsv2016);

        // Create agg_c_6_fact_csv_2016 table
        //monthOfYearAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        monthOfYearAggC6FactCsv2016.setName("month_of_year");
        monthOfYearAggC6FactCsv2016.setType(ColumnType.INTEGER);

        //quarterAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        quarterAggC6FactCsv2016.setName("quarter");
        quarterAggC6FactCsv2016.setType(ColumnType.VARCHAR);
        quarterAggC6FactCsv2016.setCharOctetLength(30);

        //theYearAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        theYearAggC6FactCsv2016.setName("the_year");
        theYearAggC6FactCsv2016.setType(ColumnType.INTEGER);

        //storeSalesAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        storeSalesAggC6FactCsv2016.setName("store_sales");
        storeSalesAggC6FactCsv2016.setType(ColumnType.DECIMAL);
        storeSalesAggC6FactCsv2016.setColumnSize(10);
        storeSalesAggC6FactCsv2016.setDecimalDigits(4);
        storeSalesAggC6FactCsv2016.setNullable(true);

        //storeCostAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        storeCostAggC6FactCsv2016.setName("store_cost");
        storeCostAggC6FactCsv2016.setType(ColumnType.DECIMAL);
        storeCostAggC6FactCsv2016.setColumnSize(10);
        storeCostAggC6FactCsv2016.setDecimalDigits(4);
        storeCostAggC6FactCsv2016.setNullable(true);

        //unitSalesAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        unitSalesAggC6FactCsv2016.setName("unit_sales");
        unitSalesAggC6FactCsv2016.setType(ColumnType.DECIMAL);
        unitSalesAggC6FactCsv2016.setColumnSize(10);
        unitSalesAggC6FactCsv2016.setDecimalDigits(4);
        unitSalesAggC6FactCsv2016.setNullable(true);

        //customerCountAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        customerCountAggC6FactCsv2016.setName("customer_count");
        customerCountAggC6FactCsv2016.setType(ColumnType.INTEGER);

        //factCountAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        factCountAggC6FactCsv2016.setName("fact_count");
        factCountAggC6FactCsv2016.setType(ColumnType.INTEGER);

        //storeSalesFactCountAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        storeSalesFactCountAggC6FactCsv2016.setName("store_sales_fact_count");
        storeSalesFactCountAggC6FactCsv2016.setType(ColumnType.INTEGER);

        //storeCostFactCountAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        storeCostFactCountAggC6FactCsv2016.setName("store_cost_fact_count");
        storeCostFactCountAggC6FactCsv2016.setType(ColumnType.INTEGER);

        //unitSalesFactCountAggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        unitSalesFactCountAggC6FactCsv2016.setName("unit_sales_fact_count");
        unitSalesFactCountAggC6FactCsv2016.setType(ColumnType.INTEGER);

        //aggC6FactCsv2016 = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        aggC6FactCsv2016.setName("agg_c_6_fact_csv_2016");
        aggC6FactCsv2016.getColumns().add(monthOfYearAggC6FactCsv2016);
        aggC6FactCsv2016.getColumns().add(quarterAggC6FactCsv2016);
        aggC6FactCsv2016.getColumns().add(theYearAggC6FactCsv2016);
        aggC6FactCsv2016.getColumns().add(storeSalesAggC6FactCsv2016);
        aggC6FactCsv2016.getColumns().add(storeCostAggC6FactCsv2016);
        aggC6FactCsv2016.getColumns().add(unitSalesAggC6FactCsv2016);
        aggC6FactCsv2016.getColumns().add(customerCountAggC6FactCsv2016);
        aggC6FactCsv2016.getColumns().add(factCountAggC6FactCsv2016);
        aggC6FactCsv2016.getColumns().add(storeSalesFactCountAggC6FactCsv2016);
        aggC6FactCsv2016.getColumns().add(storeCostFactCountAggC6FactCsv2016);
        aggC6FactCsv2016.getColumns().add(unitSalesFactCountAggC6FactCsv2016);

        // Create agg_csv_different_column_names table
        //monthOfYearAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        monthOfYearAggCsvDifferentColumnNames.setName("month_of_year");
        monthOfYearAggCsvDifferentColumnNames.setType(ColumnType.INTEGER);

        //quarterAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        quarterAggCsvDifferentColumnNames.setName("quarter");
        quarterAggCsvDifferentColumnNames.setType(ColumnType.VARCHAR);
        quarterAggCsvDifferentColumnNames.setCharOctetLength(30);

        //theYearAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        theYearAggCsvDifferentColumnNames.setName("the_year");
        theYearAggCsvDifferentColumnNames.setType(ColumnType.INTEGER);

        //storeSalesAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        storeSalesAggCsvDifferentColumnNames.setName("store_sales");
        storeSalesAggCsvDifferentColumnNames.setType(ColumnType.DECIMAL);
        storeSalesAggCsvDifferentColumnNames.setColumnSize(10);
        storeSalesAggCsvDifferentColumnNames.setDecimalDigits(4);
        storeSalesAggCsvDifferentColumnNames.setNullable(true);

        //storeCostAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        storeCostAggCsvDifferentColumnNames.setName("store_cost");
        storeCostAggCsvDifferentColumnNames.setType(ColumnType.DECIMAL);
        storeCostAggCsvDifferentColumnNames.setColumnSize(10);
        storeCostAggCsvDifferentColumnNames.setDecimalDigits(4);
        storeCostAggCsvDifferentColumnNames.setNullable(true);

        //unitSalesAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        unitSalesAggCsvDifferentColumnNames.setName("unit_sales");
        unitSalesAggCsvDifferentColumnNames.setType(ColumnType.DECIMAL);
        unitSalesAggCsvDifferentColumnNames.setColumnSize(10);
        unitSalesAggCsvDifferentColumnNames.setDecimalDigits(4);
        unitSalesAggCsvDifferentColumnNames.setNullable(true);

        //customerCountAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        customerCountAggCsvDifferentColumnNames.setName("customer_count");
        customerCountAggCsvDifferentColumnNames.setType(ColumnType.INTEGER);

        //factCountAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        factCountAggCsvDifferentColumnNames.setName("fact_count");
        factCountAggCsvDifferentColumnNames.setType(ColumnType.INTEGER);

        //ssFcAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        ssFcAggCsvDifferentColumnNames.setName("ss_fc");
        ssFcAggCsvDifferentColumnNames.setType(ColumnType.INTEGER);

        //scFcAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        scFcAggCsvDifferentColumnNames.setName("sc_fc");
        scFcAggCsvDifferentColumnNames.setType(ColumnType.INTEGER);

        //usFcAggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        usFcAggCsvDifferentColumnNames.setName("us_fc");
        usFcAggCsvDifferentColumnNames.setType(ColumnType.INTEGER);

        //aggCsvDifferentColumnNames = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        aggCsvDifferentColumnNames.setName("agg_csv_different_column_names");
        aggCsvDifferentColumnNames.getColumns().add(monthOfYearAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getColumns().add(quarterAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getColumns().add(theYearAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getColumns().add(storeSalesAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getColumns().add(storeCostAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getColumns().add(unitSalesAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getColumns().add(customerCountAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getColumns().add(factCountAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getColumns().add(ssFcAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getColumns().add(scFcAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getColumns().add(usFcAggCsvDifferentColumnNames);

        // Create agg_csv_divide_by_zero table
        //monthOfYearAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        monthOfYearAggCsvDivideByZero.setName("month_of_year");
        monthOfYearAggCsvDivideByZero.setType(ColumnType.INTEGER);

        //quarterAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        quarterAggCsvDivideByZero.setName("quarter");
        quarterAggCsvDivideByZero.setType(ColumnType.VARCHAR);
        quarterAggCsvDivideByZero.setCharOctetLength(30);

        //theYearAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        theYearAggCsvDivideByZero.setName("the_year");
        theYearAggCsvDivideByZero.setType(ColumnType.INTEGER);

        //storeSalesAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        storeSalesAggCsvDivideByZero.setName("store_sales");
        storeSalesAggCsvDivideByZero.setType(ColumnType.DECIMAL);
        storeSalesAggCsvDivideByZero.setColumnSize(10);
        storeSalesAggCsvDivideByZero.setDecimalDigits(4);
        storeSalesAggCsvDivideByZero.setNullable(true);

        //storeCostAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        storeCostAggCsvDivideByZero.setName("store_cost");
        storeCostAggCsvDivideByZero.setType(ColumnType.DECIMAL);
        storeCostAggCsvDivideByZero.setColumnSize(10);
        storeCostAggCsvDivideByZero.setDecimalDigits(4);
        storeCostAggCsvDivideByZero.setNullable(true);

        //unitSalesAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        unitSalesAggCsvDivideByZero.setName("unit_sales");
        unitSalesAggCsvDivideByZero.setType(ColumnType.DECIMAL);
        unitSalesAggCsvDivideByZero.setColumnSize(10);
        unitSalesAggCsvDivideByZero.setDecimalDigits(4);
        unitSalesAggCsvDivideByZero.setNullable(true);

        //customerCountAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        customerCountAggCsvDivideByZero.setName("customer_count");
        customerCountAggCsvDivideByZero.setType(ColumnType.INTEGER);

        //factCountAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        factCountAggCsvDivideByZero.setName("fact_count");
        factCountAggCsvDivideByZero.setType(ColumnType.INTEGER);

        //storeSalesFactCountAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        storeSalesFactCountAggCsvDivideByZero.setName("store_sales_fact_count");
        storeSalesFactCountAggCsvDivideByZero.setType(ColumnType.INTEGER);

        //storeCostFactCountAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        storeCostFactCountAggCsvDivideByZero.setName("store_cost_fact_count");
        storeCostFactCountAggCsvDivideByZero.setType(ColumnType.INTEGER);

        //unitSalesFactCountAggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        unitSalesFactCountAggCsvDivideByZero.setName("unit_sales_fact_count");
        unitSalesFactCountAggCsvDivideByZero.setType(ColumnType.INTEGER);

        //aggCsvDivideByZero = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        aggCsvDivideByZero.setName("agg_csv_divide_by_zero");
        aggCsvDivideByZero.getColumns().add(monthOfYearAggCsvDivideByZero);
        aggCsvDivideByZero.getColumns().add(quarterAggCsvDivideByZero);
        aggCsvDivideByZero.getColumns().add(theYearAggCsvDivideByZero);
        aggCsvDivideByZero.getColumns().add(storeSalesAggCsvDivideByZero);
        aggCsvDivideByZero.getColumns().add(storeCostAggCsvDivideByZero);
        aggCsvDivideByZero.getColumns().add(unitSalesAggCsvDivideByZero);
        aggCsvDivideByZero.getColumns().add(customerCountAggCsvDivideByZero);
        aggCsvDivideByZero.getColumns().add(factCountAggCsvDivideByZero);
        aggCsvDivideByZero.getColumns().add(storeSalesFactCountAggCsvDivideByZero);
        aggCsvDivideByZero.getColumns().add(storeCostFactCountAggCsvDivideByZero);
        aggCsvDivideByZero.getColumns().add(unitSalesFactCountAggCsvDivideByZero);

        // Add tables to database schema
        if (catalog.getDbschemas().size() > 0) {
            DatabaseSchema dbSchema = catalog.getDbschemas().get(0);
            dbSchema.getTables().add(timeCsvTable);
            dbSchema.getTables().add(factCsv2016Table);
            dbSchema.getTables().add(aggC6FactCsv2016);
            dbSchema.getTables().add(aggCsvDifferentColumnNames);
            dbSchema.getTables().add(aggCsvDivideByZero);
        }
    }

    protected void createDimensions() {

        // Create Store dimension
        storeDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        storeDimension.setName("Store");

        // Create Store hierarchy
        TableQuery storeQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        storeQuery.setTable((Table) copier.get(CatalogSupplier.TABLE_STORE));

        // Create member properties for Store Name level
        MemberProperty storeTypeProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        storeTypeProp.setName("Store Type");
        storeTypeProp.setColumn((Column) copier.get(CatalogSupplier.COLUMN_STORE_TYPE_STORE));

        MemberProperty storeManagerProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        storeManagerProp.setName("Store Manager");
        storeManagerProp.setColumn((Column) copier.get(CatalogSupplier.COLUMN_STORE_MANAGER_STORE));

        MemberProperty storeSqftProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        storeSqftProp.setName("Store Sqft");
        storeSqftProp.setColumn((Column) copier.get(CatalogSupplier.COLUMN_STORE_SQFT_STORE));
        storeSqftProp.setPropertyType(ColumnInternalDataType.NUMERIC);

        MemberProperty grocerySqftProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        grocerySqftProp.setName("Grocery Sqft");
        grocerySqftProp.setColumn((Column) copier.get(CatalogSupplier.COLUMN_GROCERY_SQFT_STORE));
        grocerySqftProp.setPropertyType(ColumnInternalDataType.NUMERIC);

        MemberProperty frozenSqftProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        frozenSqftProp.setName("Frozen Sqft");
        frozenSqftProp.setColumn((Column) copier.get(CatalogSupplier.COLUMN_FROZEN_SQFT_STORE));
        frozenSqftProp.setPropertyType(ColumnInternalDataType.NUMERIC);

        MemberProperty meatSqftProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        meatSqftProp.setName("Meat Sqft");
        meatSqftProp.setColumn(CatalogSupplier.COLUMN_MEAT_SQFT_STORE);
        meatSqftProp.setPropertyType(ColumnInternalDataType.NUMERIC);

        MemberProperty coffeeBarProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        coffeeBarProp.setName("Has coffee bar");
        coffeeBarProp.setColumn((Column) copier.get(CatalogSupplier.COLUMN_COFFEE_BAR_STORE));
        coffeeBarProp.setPropertyType(ColumnInternalDataType.BOOLEAN);

        MemberProperty streetAddressProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        streetAddressProp.setName("Street address");
        streetAddressProp.setColumn((Column) copier.get(CatalogSupplier.COLUMN_STREET_ADDRESS_STORE));
        streetAddressProp.setPropertyType(ColumnInternalDataType.STRING);

        Level storeCountryLevel = RolapMappingFactory.eINSTANCE.createLevel();
        storeCountryLevel.setName("Store Country");
        storeCountryLevel.setColumn(CatalogSupplier.COLUMN_STORE_COUNTRY_STORE);
        storeCountryLevel.setUniqueMembers(true);

        Level storeStateLevel = RolapMappingFactory.eINSTANCE.createLevel();
        storeStateLevel.setName("Store State");
        storeStateLevel.setColumn(CatalogSupplier.COLUMN_STORE_STATE_STORE);
        storeStateLevel.setUniqueMembers(true);

        Level storeCityLevel = RolapMappingFactory.eINSTANCE.createLevel();
        storeCityLevel.setName("Store City");
        storeCityLevel.setColumn((Column) copier.get(CatalogSupplier.COLUMN_STORE_CITY_STORE));
        storeCityLevel.setUniqueMembers(false);

        Level storeNameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        storeNameLevel.setName("Store Name");
        storeNameLevel.setColumn((Column) copier.get(CatalogSupplier.COLUMN_STORE_NAME_STORE));
        storeNameLevel.setUniqueMembers(true);
        storeNameLevel.getMemberProperties().add(storeTypeProp);
        storeNameLevel.getMemberProperties().add(storeManagerProp);
        storeNameLevel.getMemberProperties().add(storeSqftProp);
        storeNameLevel.getMemberProperties().add(grocerySqftProp);
        storeNameLevel.getMemberProperties().add(frozenSqftProp);
        storeNameLevel.getMemberProperties().add(meatSqftProp);
        storeNameLevel.getMemberProperties().add(coffeeBarProp);
        storeNameLevel.getMemberProperties().add(streetAddressProp);

        ExplicitHierarchy storeHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        storeHierarchy.setHasAll(true);
        storeHierarchy.setPrimaryKey( (Column) copier.get(CatalogSupplier.COLUMN_STORE_ID_STORE));
        storeHierarchy.setQuery(storeQuery);
        storeHierarchy.getLevels().add(storeCountryLevel);
        storeHierarchy.getLevels().add(storeStateLevel);
        storeHierarchy.getLevels().add(storeCityLevel);
        storeHierarchy.getLevels().add(storeNameLevel);

        storeDimension.getHierarchies().add(storeHierarchy);

        // Create Time dimension
        timeDimension = RolapMappingFactory.eINSTANCE.createTimeDimension();
        timeDimension.setName("Time");

        TableQuery timeQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        timeQuery.setTable(timeCsvTable);

        // First hierarchy (default)
        Level yearLevel1 = RolapMappingFactory.eINSTANCE.createLevel();
        yearLevel1.setName("Year");
        yearLevel1.setColumn(theYearColumnInTimeCsv);
        yearLevel1.setColumnType(ColumnInternalDataType.NUMERIC);
        yearLevel1.setUniqueMembers(true);
        yearLevel1.setType(LevelDefinition.TIME_YEARS);

        Level quarterLevel = RolapMappingFactory.eINSTANCE.createLevel();
        quarterLevel.setName("Quarter");
        quarterLevel.setColumn(quarterColumnInTimeCsv);
        quarterLevel.setUniqueMembers(false);
        quarterLevel.setType(LevelDefinition.TIME_QUARTERS);

        Level monthLevel = RolapMappingFactory.eINSTANCE.createLevel();
        monthLevel.setName("Month");
        monthLevel.setColumn(monthOfYearColumnInTimeCsv);
        monthLevel.setUniqueMembers(false);
        monthLevel.setColumnType(ColumnInternalDataType.NUMERIC);
        monthLevel.setType(LevelDefinition.TIME_MONTHS);

        ExplicitHierarchy timeHierarchy1 = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        timeHierarchy1.setHasAll(false);
        timeHierarchy1.setPrimaryKey(timeIdColumnInTimeCsv);
        timeHierarchy1.setQuery(timeQuery);
        timeHierarchy1.getLevels().add(yearLevel1);
        timeHierarchy1.getLevels().add(quarterLevel);
        timeHierarchy1.getLevels().add(monthLevel);

        // Second hierarchy (Weekly)
        Level yearLevel2 = RolapMappingFactory.eINSTANCE.createLevel();
        yearLevel2.setName("Year");
        yearLevel2.setColumn(theYearColumnInTimeCsv);
        yearLevel2.setColumnType(ColumnInternalDataType.NUMERIC);
        yearLevel2.setUniqueMembers(true);
        yearLevel2.setType(LevelDefinition.TIME_YEARS);

        Level weekLevel = RolapMappingFactory.eINSTANCE.createLevel();
        weekLevel.setName("Week");
        weekLevel.setColumn(weekOfYearColumnInTimeCsv);
        weekLevel.setColumnType(ColumnInternalDataType.NUMERIC);
        weekLevel.setUniqueMembers(false);
        weekLevel.setType(LevelDefinition.TIME_WEEKS);

        Level dayLevel = RolapMappingFactory.eINSTANCE.createLevel();
        dayLevel.setName("Day");
        dayLevel.setColumn(dayOfMonthColumnTimeCsv);
        dayLevel.setColumnType(ColumnInternalDataType.NUMERIC);
        dayLevel.setUniqueMembers(false);
        dayLevel.setType(LevelDefinition.TIME_DAYS);

        ExplicitHierarchy timeHierarchy2 = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        timeHierarchy2.setHasAll(true);
        timeHierarchy2.setName("Weekly");
        timeHierarchy2.setPrimaryKey(timeIdColumnInTimeCsv);
        timeHierarchy2.setQuery(timeQuery);
        timeHierarchy2.getLevels().add(yearLevel2);
        timeHierarchy2.getLevels().add(weekLevel);
        timeHierarchy2.getLevels().add(dayLevel);

        timeDimension.getHierarchies().add(timeHierarchy1);
        timeDimension.getHierarchies().add(timeHierarchy2);
    }

    protected void createMeasures() {
        unitSalesMeasure = RolapMappingFactory.eINSTANCE.createAvgMeasure();
        unitSalesMeasure.setName("Unit Sales");
        unitSalesMeasure.setColumn(unitSalesColumnInFactCsv2016);
        unitSalesMeasure.setFormatString("Standard");
    }

    protected void createCube() {
        // Create table query
        TableQuery tableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        tableQuery.setTable(factCsv2016Table);

        // Add aggregation tables and excludes from subclass
        List<AggregationTable> aggTables = getAggTables();
        if (aggTables != null) {
            tableQuery.getAggregationTables().addAll(aggTables);
        }

        List<AggregationExclude> aggExcludes = getAggExcludes();
        if (aggExcludes != null) {
            tableQuery.getAggregationExcludes().addAll(aggExcludes);
        }

        // Create dimension connectors
        DimensionConnector timeConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        timeConnector.setDimension(timeDimension);
        timeConnector.setOverrideDimensionName("Time");
        timeConnector.setForeignKey(timeIdColumnInFactCsv2016);

        DimensionConnector storeConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        storeConnector.setDimension(storeDimension);
        storeConnector.setOverrideDimensionName("Store");
        storeConnector.setForeignKey(storeIdColumnInFactCsv2016);

        // Create measures
        AvgMeasure storeCostMeasure = RolapMappingFactory.eINSTANCE.createAvgMeasure();
        storeCostMeasure.setName("Store Cost");
        storeCostMeasure.setColumn(storeCostColumnInFactCsv2016);
        storeCostMeasure.setFormatString("#,###.00");

        AvgMeasure storeSalesMeasure = RolapMappingFactory.eINSTANCE.createAvgMeasure();
        storeSalesMeasure.setName("Store Sales");
        storeSalesMeasure.setColumn(storeSalesColumnInFactCsv2016);
        storeSalesMeasure.setFormatString("#,###.00");

        // Create measure group
        MeasureGroup measureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        measureGroup.getMeasures().add(unitSalesMeasure);
        measureGroup.getMeasures().add(storeCostMeasure);
        measureGroup.getMeasures().add(storeSalesMeasure);

        // Create cube
        PhysicalCube cube = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        cube.setName("Sales");
        cube.setDefaultMeasure(unitSalesMeasure);
        cube.setQuery(tableQuery);
        cube.getDimensionConnectors().add(timeConnector);
        cube.getDimensionConnectors().add(storeConnector);
        cube.getMeasureGroups().add(measureGroup);

        // Set catalog properties
        catalog.setName("FoodMart");
        catalog.getCubes().clear();
        catalog.getAccessRoles().clear();
        catalog.getCubes().add(cube);
    }

    protected List<AggregationTable> getAggTables() {
        return List.of();
    }

    protected List<AggregationExclude> getAggExcludes() {
        return List.of();
    }

    @Override
    public Catalog get() {
        return catalog;
    }
}
