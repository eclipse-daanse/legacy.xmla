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

import org.eclipse.daanse.cwm.model.cwm.resource.relational.Column;
import org.eclipse.daanse.cwm.model.cwm.resource.relational.Schema;
import org.eclipse.daanse.cwm.model.cwm.resource.relational.Table;
import org.eclipse.daanse.cwm.util.resource.relational.SqlSimpleTypes;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.model.catalog.Catalog;
import org.eclipse.daanse.rolap.mapping.model.catalog.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.database.aggregation.AggregationExclude;
import org.eclipse.daanse.rolap.mapping.model.database.aggregation.AggregationTable;
import org.eclipse.daanse.rolap.mapping.model.database.relational.ColumnInternalDataType;
import org.eclipse.daanse.rolap.mapping.model.database.source.SourceFactory;
import org.eclipse.daanse.rolap.mapping.model.database.source.TableSource;
import org.eclipse.daanse.rolap.mapping.model.olap.cube.CubeFactory;
import org.eclipse.daanse.rolap.mapping.model.olap.cube.MeasureGroup;
import org.eclipse.daanse.rolap.mapping.model.olap.cube.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.olap.cube.measure.AvgMeasure;
import org.eclipse.daanse.rolap.mapping.model.olap.cube.measure.MeasureFactory;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.DimensionFactory;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.StandardDimension;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.TimeDimension;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.hierarchy.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.hierarchy.HierarchyFactory;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.hierarchy.level.Level;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.hierarchy.level.LevelDefinition;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.hierarchy.level.LevelFactory;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.hierarchy.level.MemberProperty;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.opencube.junit5.EmfUtil;
public class AggMeasureFactCountTestModifierEmf implements CatalogMappingSupplier {

    public final Catalog catalog;
    public final EcoreUtil.Copier copier;

    // Time CSV table columns
    public static Column timeIdColumnInTimeCsv = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column theYearColumnInTimeCsv = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column monthOfYearColumnInTimeCsv = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column quarterColumnInTimeCsv = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column dayOfMonthColumnTimeCsv = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column weekOfYearColumnInTimeCsv = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Table timeCsvTable = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();

    // Fact CSV 2016 table columns
    public static Column productIdColumnInFactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column timeIdColumnInFactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column customerIdColumnInFactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column promotionIdColumnInFactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column storeIdColumnInFactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column storeSalesColumnInFactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column storeCostColumnInFactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column unitSalesColumnInFactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Table factCsv2016Table = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();

    // Aggregation table: agg_c_6_fact_csv_2016
    public static Column monthOfYearAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column quarterAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column theYearAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column storeSalesAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column storeCostAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column unitSalesAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public Column customerCountAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column factCountAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column storeSalesFactCountAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column storeCostFactCountAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column unitSalesFactCountAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Table aggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();

    // Aggregation table: agg_csv_different_column_names
    public static Column monthOfYearAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column quarterAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column theYearAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column storeSalesAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column storeCostAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column unitSalesAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column customerCountAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column factCountAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column ssFcAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column scFcAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column usFcAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Table aggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();

    // Aggregation table: agg_csv_divide_by_zero
    public static Column monthOfYearAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column quarterAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column theYearAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column storeSalesAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column storeCostAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column unitSalesAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column customerCountAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column factCountAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column storeSalesFactCountAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column storeCostFactCountAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Column unitSalesFactCountAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
    public static Table aggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();

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
        //timeIdColumnInTimeCsv = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        timeIdColumnInTimeCsv.setName("time_id");
        timeIdColumnInTimeCsv.setType(SqlSimpleTypes.Sql99.integerType());

        //theYearColumnInTimeCsv = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        theYearColumnInTimeCsv.setName("the_year");
        theYearColumnInTimeCsv.setType(SqlSimpleTypes.Sql99.smallintType());

        //monthOfYearColumnInTimeCsv = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        monthOfYearColumnInTimeCsv.setName("month_of_year");
        monthOfYearColumnInTimeCsv.setType(SqlSimpleTypes.Sql99.smallintType());

        //quarterColumnInTimeCsv = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        quarterColumnInTimeCsv.setName("quarter");
        quarterColumnInTimeCsv.setType(SqlSimpleTypes.varcharType(255));
        // quarterColumnInTimeCsv.setCharOctetLength(30);

        //dayOfMonthColumnTimeCsv = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        dayOfMonthColumnTimeCsv.setName("day_of_month");
        dayOfMonthColumnTimeCsv.setType(SqlSimpleTypes.Sql99.smallintType());

        //weekOfYearColumnInTimeCsv = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        weekOfYearColumnInTimeCsv.setName("week_of_year");
        weekOfYearColumnInTimeCsv.setType(SqlSimpleTypes.Sql99.integerType());

        //timeCsvTable = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
        timeCsvTable.setName("time_csv");
        timeCsvTable.getFeature().add(timeIdColumnInTimeCsv);
        timeCsvTable.getFeature().add(theYearColumnInTimeCsv);
        timeCsvTable.getFeature().add(monthOfYearColumnInTimeCsv);
        timeCsvTable.getFeature().add(quarterColumnInTimeCsv);
        timeCsvTable.getFeature().add(weekOfYearColumnInTimeCsv);
        timeCsvTable.getFeature().add(dayOfMonthColumnTimeCsv);

        // Create fact_csv_2016 table columns
        //productIdColumnInFactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        productIdColumnInFactCsv2016.setName("product_id");
        productIdColumnInFactCsv2016.setType(SqlSimpleTypes.Sql99.integerType());

        //timeIdColumnInFactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        timeIdColumnInFactCsv2016.setName("time_id");
        timeIdColumnInFactCsv2016.setType(SqlSimpleTypes.Sql99.integerType());

        //customerIdColumnInFactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        customerIdColumnInFactCsv2016.setName("customer_id");
        customerIdColumnInFactCsv2016.setType(SqlSimpleTypes.Sql99.integerType());

        //promotionIdColumnInFactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        promotionIdColumnInFactCsv2016.setName("promotion_id");
        promotionIdColumnInFactCsv2016.setType(SqlSimpleTypes.Sql99.integerType());

        //storeIdColumnInFactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        storeIdColumnInFactCsv2016.setName("store_id");
        storeIdColumnInFactCsv2016.setType(SqlSimpleTypes.Sql99.integerType());

        //storeSalesColumnInFactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        storeSalesColumnInFactCsv2016.setName("store_sales");
        storeSalesColumnInFactCsv2016.setType(SqlSimpleTypes.decimalType(18, 4));
        // storeSalesColumnInFactCsv2016.setColumnSize(10);
        // storeSalesColumnInFactCsv2016.setDecimalDigits(4);
        // setNullable removed (CWM Column has isNullable enum): storeSalesColumnInFactCsv2016 true

        //storeCostColumnInFactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        storeCostColumnInFactCsv2016.setName("store_cost");
        storeCostColumnInFactCsv2016.setType(SqlSimpleTypes.decimalType(18, 4));
        // storeCostColumnInFactCsv2016.setColumnSize(10);
        // storeCostColumnInFactCsv2016.setDecimalDigits(4);
        // setNullable removed (CWM Column has isNullable enum): storeCostColumnInFactCsv2016 true

        //unitSalesColumnInFactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        unitSalesColumnInFactCsv2016.setName("unit_sales");
        unitSalesColumnInFactCsv2016.setType(SqlSimpleTypes.decimalType(18, 4));
        // unitSalesColumnInFactCsv2016.setColumnSize(10);
        // unitSalesColumnInFactCsv2016.setDecimalDigits(4);
        // setNullable removed (CWM Column has isNullable enum): unitSalesColumnInFactCsv2016 true

        //factCsv2016Table = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
        factCsv2016Table.setName("fact_csv_2016");
        factCsv2016Table.getFeature().add(productIdColumnInFactCsv2016);
        factCsv2016Table.getFeature().add(timeIdColumnInFactCsv2016);
        factCsv2016Table.getFeature().add(customerIdColumnInFactCsv2016);
        factCsv2016Table.getFeature().add(promotionIdColumnInFactCsv2016);
        factCsv2016Table.getFeature().add(storeIdColumnInFactCsv2016);
        factCsv2016Table.getFeature().add(storeSalesColumnInFactCsv2016);
        factCsv2016Table.getFeature().add(storeCostColumnInFactCsv2016);
        factCsv2016Table.getFeature().add(unitSalesColumnInFactCsv2016);

        // Create agg_c_6_fact_csv_2016 table
        //monthOfYearAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        monthOfYearAggC6FactCsv2016.setName("month_of_year");
        monthOfYearAggC6FactCsv2016.setType(SqlSimpleTypes.Sql99.integerType());

        //quarterAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        quarterAggC6FactCsv2016.setName("quarter");
        quarterAggC6FactCsv2016.setType(SqlSimpleTypes.varcharType(255));
        // quarterAggC6FactCsv2016.setCharOctetLength(30);

        //theYearAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        theYearAggC6FactCsv2016.setName("the_year");
        theYearAggC6FactCsv2016.setType(SqlSimpleTypes.Sql99.integerType());

        //storeSalesAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        storeSalesAggC6FactCsv2016.setName("store_sales");
        storeSalesAggC6FactCsv2016.setType(SqlSimpleTypes.decimalType(18, 4));
        // storeSalesAggC6FactCsv2016.setColumnSize(10);
        // storeSalesAggC6FactCsv2016.setDecimalDigits(4);
        // setNullable removed (CWM Column has isNullable enum): storeSalesAggC6FactCsv2016 true

        //storeCostAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        storeCostAggC6FactCsv2016.setName("store_cost");
        storeCostAggC6FactCsv2016.setType(SqlSimpleTypes.decimalType(18, 4));
        // storeCostAggC6FactCsv2016.setColumnSize(10);
        // storeCostAggC6FactCsv2016.setDecimalDigits(4);
        // setNullable removed (CWM Column has isNullable enum): storeCostAggC6FactCsv2016 true

        //unitSalesAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        unitSalesAggC6FactCsv2016.setName("unit_sales");
        unitSalesAggC6FactCsv2016.setType(SqlSimpleTypes.decimalType(18, 4));
        // unitSalesAggC6FactCsv2016.setColumnSize(10);
        // unitSalesAggC6FactCsv2016.setDecimalDigits(4);
        // setNullable removed (CWM Column has isNullable enum): unitSalesAggC6FactCsv2016 true

        //customerCountAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        customerCountAggC6FactCsv2016.setName("customer_count");
        customerCountAggC6FactCsv2016.setType(SqlSimpleTypes.Sql99.integerType());

        //factCountAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        factCountAggC6FactCsv2016.setName("fact_count");
        factCountAggC6FactCsv2016.setType(SqlSimpleTypes.Sql99.integerType());

        //storeSalesFactCountAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        storeSalesFactCountAggC6FactCsv2016.setName("store_sales_fact_count");
        storeSalesFactCountAggC6FactCsv2016.setType(SqlSimpleTypes.Sql99.integerType());

        //storeCostFactCountAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        storeCostFactCountAggC6FactCsv2016.setName("store_cost_fact_count");
        storeCostFactCountAggC6FactCsv2016.setType(SqlSimpleTypes.Sql99.integerType());

        //unitSalesFactCountAggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        unitSalesFactCountAggC6FactCsv2016.setName("unit_sales_fact_count");
        unitSalesFactCountAggC6FactCsv2016.setType(SqlSimpleTypes.Sql99.integerType());

        //aggC6FactCsv2016 = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
        aggC6FactCsv2016.setName("agg_c_6_fact_csv_2016");
        aggC6FactCsv2016.getFeature().add(monthOfYearAggC6FactCsv2016);
        aggC6FactCsv2016.getFeature().add(quarterAggC6FactCsv2016);
        aggC6FactCsv2016.getFeature().add(theYearAggC6FactCsv2016);
        aggC6FactCsv2016.getFeature().add(storeSalesAggC6FactCsv2016);
        aggC6FactCsv2016.getFeature().add(storeCostAggC6FactCsv2016);
        aggC6FactCsv2016.getFeature().add(unitSalesAggC6FactCsv2016);
        aggC6FactCsv2016.getFeature().add(customerCountAggC6FactCsv2016);
        aggC6FactCsv2016.getFeature().add(factCountAggC6FactCsv2016);
        aggC6FactCsv2016.getFeature().add(storeSalesFactCountAggC6FactCsv2016);
        aggC6FactCsv2016.getFeature().add(storeCostFactCountAggC6FactCsv2016);
        aggC6FactCsv2016.getFeature().add(unitSalesFactCountAggC6FactCsv2016);

        // Create agg_csv_different_column_names table
        //monthOfYearAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        monthOfYearAggCsvDifferentColumnNames.setName("month_of_year");
        monthOfYearAggCsvDifferentColumnNames.setType(SqlSimpleTypes.Sql99.integerType());

        //quarterAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        quarterAggCsvDifferentColumnNames.setName("quarter");
        quarterAggCsvDifferentColumnNames.setType(SqlSimpleTypes.varcharType(255));
        // quarterAggCsvDifferentColumnNames.setCharOctetLength(30);

        //theYearAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        theYearAggCsvDifferentColumnNames.setName("the_year");
        theYearAggCsvDifferentColumnNames.setType(SqlSimpleTypes.Sql99.integerType());

        //storeSalesAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        storeSalesAggCsvDifferentColumnNames.setName("store_sales");
        storeSalesAggCsvDifferentColumnNames.setType(SqlSimpleTypes.decimalType(18, 4));
        // storeSalesAggCsvDifferentColumnNames.setColumnSize(10);
        // storeSalesAggCsvDifferentColumnNames.setDecimalDigits(4);
        // setNullable removed (CWM Column has isNullable enum): storeSalesAggCsvDifferentColumnNames true

        //storeCostAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        storeCostAggCsvDifferentColumnNames.setName("store_cost");
        storeCostAggCsvDifferentColumnNames.setType(SqlSimpleTypes.decimalType(18, 4));
        // storeCostAggCsvDifferentColumnNames.setColumnSize(10);
        // storeCostAggCsvDifferentColumnNames.setDecimalDigits(4);
        // setNullable removed (CWM Column has isNullable enum): storeCostAggCsvDifferentColumnNames true

        //unitSalesAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        unitSalesAggCsvDifferentColumnNames.setName("unit_sales");
        unitSalesAggCsvDifferentColumnNames.setType(SqlSimpleTypes.decimalType(18, 4));
        // unitSalesAggCsvDifferentColumnNames.setColumnSize(10);
        // unitSalesAggCsvDifferentColumnNames.setDecimalDigits(4);
        // setNullable removed (CWM Column has isNullable enum): unitSalesAggCsvDifferentColumnNames true

        //customerCountAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        customerCountAggCsvDifferentColumnNames.setName("customer_count");
        customerCountAggCsvDifferentColumnNames.setType(SqlSimpleTypes.Sql99.integerType());

        //factCountAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        factCountAggCsvDifferentColumnNames.setName("fact_count");
        factCountAggCsvDifferentColumnNames.setType(SqlSimpleTypes.Sql99.integerType());

        //ssFcAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        ssFcAggCsvDifferentColumnNames.setName("ss_fc");
        ssFcAggCsvDifferentColumnNames.setType(SqlSimpleTypes.Sql99.integerType());

        //scFcAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        scFcAggCsvDifferentColumnNames.setName("sc_fc");
        scFcAggCsvDifferentColumnNames.setType(SqlSimpleTypes.Sql99.integerType());

        //usFcAggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        usFcAggCsvDifferentColumnNames.setName("us_fc");
        usFcAggCsvDifferentColumnNames.setType(SqlSimpleTypes.Sql99.integerType());

        //aggCsvDifferentColumnNames = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
        aggCsvDifferentColumnNames.setName("agg_csv_different_column_names");
        aggCsvDifferentColumnNames.getFeature().add(monthOfYearAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getFeature().add(quarterAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getFeature().add(theYearAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getFeature().add(storeSalesAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getFeature().add(storeCostAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getFeature().add(unitSalesAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getFeature().add(customerCountAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getFeature().add(factCountAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getFeature().add(ssFcAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getFeature().add(scFcAggCsvDifferentColumnNames);
        aggCsvDifferentColumnNames.getFeature().add(usFcAggCsvDifferentColumnNames);

        // Create agg_csv_divide_by_zero table
        //monthOfYearAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        monthOfYearAggCsvDivideByZero.setName("month_of_year");
        monthOfYearAggCsvDivideByZero.setType(SqlSimpleTypes.Sql99.integerType());

        //quarterAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        quarterAggCsvDivideByZero.setName("quarter");
        quarterAggCsvDivideByZero.setType(SqlSimpleTypes.varcharType(255));
        // quarterAggCsvDivideByZero.setCharOctetLength(30);

        //theYearAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        theYearAggCsvDivideByZero.setName("the_year");
        theYearAggCsvDivideByZero.setType(SqlSimpleTypes.Sql99.integerType());

        //storeSalesAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        storeSalesAggCsvDivideByZero.setName("store_sales");
        storeSalesAggCsvDivideByZero.setType(SqlSimpleTypes.decimalType(18, 4));
        // storeSalesAggCsvDivideByZero.setColumnSize(10);
        // storeSalesAggCsvDivideByZero.setDecimalDigits(4);
        // setNullable removed (CWM Column has isNullable enum): storeSalesAggCsvDivideByZero true

        //storeCostAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        storeCostAggCsvDivideByZero.setName("store_cost");
        storeCostAggCsvDivideByZero.setType(SqlSimpleTypes.decimalType(18, 4));
        // storeCostAggCsvDivideByZero.setColumnSize(10);
        // storeCostAggCsvDivideByZero.setDecimalDigits(4);
        // setNullable removed (CWM Column has isNullable enum): storeCostAggCsvDivideByZero true

        //unitSalesAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        unitSalesAggCsvDivideByZero.setName("unit_sales");
        unitSalesAggCsvDivideByZero.setType(SqlSimpleTypes.decimalType(18, 4));
        // unitSalesAggCsvDivideByZero.setColumnSize(10);
        // unitSalesAggCsvDivideByZero.setDecimalDigits(4);
        // setNullable removed (CWM Column has isNullable enum): unitSalesAggCsvDivideByZero true

        //customerCountAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        customerCountAggCsvDivideByZero.setName("customer_count");
        customerCountAggCsvDivideByZero.setType(SqlSimpleTypes.Sql99.integerType());

        //factCountAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        factCountAggCsvDivideByZero.setName("fact_count");
        factCountAggCsvDivideByZero.setType(SqlSimpleTypes.Sql99.integerType());

        //storeSalesFactCountAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        storeSalesFactCountAggCsvDivideByZero.setName("store_sales_fact_count");
        storeSalesFactCountAggCsvDivideByZero.setType(SqlSimpleTypes.Sql99.integerType());

        //storeCostFactCountAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        storeCostFactCountAggCsvDivideByZero.setName("store_cost_fact_count");
        storeCostFactCountAggCsvDivideByZero.setType(SqlSimpleTypes.Sql99.integerType());

        //unitSalesFactCountAggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        unitSalesFactCountAggCsvDivideByZero.setName("unit_sales_fact_count");
        unitSalesFactCountAggCsvDivideByZero.setType(SqlSimpleTypes.Sql99.integerType());

        //aggCsvDivideByZero = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
        aggCsvDivideByZero.setName("agg_csv_divide_by_zero");
        aggCsvDivideByZero.getFeature().add(monthOfYearAggCsvDivideByZero);
        aggCsvDivideByZero.getFeature().add(quarterAggCsvDivideByZero);
        aggCsvDivideByZero.getFeature().add(theYearAggCsvDivideByZero);
        aggCsvDivideByZero.getFeature().add(storeSalesAggCsvDivideByZero);
        aggCsvDivideByZero.getFeature().add(storeCostAggCsvDivideByZero);
        aggCsvDivideByZero.getFeature().add(unitSalesAggCsvDivideByZero);
        aggCsvDivideByZero.getFeature().add(customerCountAggCsvDivideByZero);
        aggCsvDivideByZero.getFeature().add(factCountAggCsvDivideByZero);
        aggCsvDivideByZero.getFeature().add(storeSalesFactCountAggCsvDivideByZero);
        aggCsvDivideByZero.getFeature().add(storeCostFactCountAggCsvDivideByZero);
        aggCsvDivideByZero.getFeature().add(unitSalesFactCountAggCsvDivideByZero);

        // Add tables to database schema
        if (catalog.getDbschemas().size() > 0) {
            Schema dbSchema = catalog.getDbschemas().get(0);
            dbSchema.getOwnedElement().add(timeCsvTable);
            dbSchema.getOwnedElement().add(factCsv2016Table);
            dbSchema.getOwnedElement().add(aggC6FactCsv2016);
            dbSchema.getOwnedElement().add(aggCsvDifferentColumnNames);
            dbSchema.getOwnedElement().add(aggCsvDivideByZero);
        }
    }

    protected void createDimensions() {

        // Create Store dimension
        storeDimension = DimensionFactory.eINSTANCE.createStandardDimension();
        storeDimension.setName("Store");

        // Create Store hierarchy
        TableSource storeQuery = SourceFactory.eINSTANCE.createTableSource();
        storeQuery.setTable((Table) copier.get(CatalogSupplier.TABLE_STORE));

        // Create member properties for Store Name level
        MemberProperty storeTypeProp = LevelFactory.eINSTANCE.createMemberProperty();
        storeTypeProp.setName("Store Type");
        storeTypeProp.setColumn((Column) copier.get(CatalogSupplier.COLUMN_STORE_TYPE_STORE));

        MemberProperty storeManagerProp = LevelFactory.eINSTANCE.createMemberProperty();
        storeManagerProp.setName("Store Manager");
        storeManagerProp.setColumn((Column) copier.get(CatalogSupplier.COLUMN_STORE_MANAGER_STORE));

        MemberProperty storeSqftProp = LevelFactory.eINSTANCE.createMemberProperty();
        storeSqftProp.setName("Store Sqft");
        storeSqftProp.setColumn((Column) copier.get(CatalogSupplier.COLUMN_STORE_SQFT_STORE));
        storeSqftProp.setPropertyType(ColumnInternalDataType.NUMERIC);

        MemberProperty grocerySqftProp = LevelFactory.eINSTANCE.createMemberProperty();
        grocerySqftProp.setName("Grocery Sqft");
        grocerySqftProp.setColumn((Column) copier.get(CatalogSupplier.COLUMN_GROCERY_SQFT_STORE));
        grocerySqftProp.setPropertyType(ColumnInternalDataType.NUMERIC);

        MemberProperty frozenSqftProp = LevelFactory.eINSTANCE.createMemberProperty();
        frozenSqftProp.setName("Frozen Sqft");
        frozenSqftProp.setColumn((Column) copier.get(CatalogSupplier.COLUMN_FROZEN_SQFT_STORE));
        frozenSqftProp.setPropertyType(ColumnInternalDataType.NUMERIC);

        MemberProperty meatSqftProp = LevelFactory.eINSTANCE.createMemberProperty();
        meatSqftProp.setName("Meat Sqft");
        meatSqftProp.setColumn(CatalogSupplier.COLUMN_MEAT_SQFT_STORE);
        meatSqftProp.setPropertyType(ColumnInternalDataType.NUMERIC);

        MemberProperty coffeeBarProp = LevelFactory.eINSTANCE.createMemberProperty();
        coffeeBarProp.setName("Has coffee bar");
        coffeeBarProp.setColumn((Column) copier.get(CatalogSupplier.COLUMN_COFFEE_BAR_STORE));
        coffeeBarProp.setPropertyType(ColumnInternalDataType.BOOLEAN);

        MemberProperty streetAddressProp = LevelFactory.eINSTANCE.createMemberProperty();
        streetAddressProp.setName("Street address");
        streetAddressProp.setColumn((Column) copier.get(CatalogSupplier.COLUMN_STREET_ADDRESS_STORE));
        streetAddressProp.setPropertyType(ColumnInternalDataType.STRING);

        Level storeCountryLevel = LevelFactory.eINSTANCE.createLevel();
        storeCountryLevel.setName("Store Country");
        storeCountryLevel.setColumn(CatalogSupplier.COLUMN_STORE_COUNTRY_STORE);
        storeCountryLevel.setUniqueMembers(true);

        Level storeStateLevel = LevelFactory.eINSTANCE.createLevel();
        storeStateLevel.setName("Store State");
        storeStateLevel.setColumn(CatalogSupplier.COLUMN_STORE_STATE_STORE);
        storeStateLevel.setUniqueMembers(true);

        Level storeCityLevel = LevelFactory.eINSTANCE.createLevel();
        storeCityLevel.setName("Store City");
        storeCityLevel.setColumn((Column) copier.get(CatalogSupplier.COLUMN_STORE_CITY_STORE));
        storeCityLevel.setUniqueMembers(false);

        Level storeNameLevel = LevelFactory.eINSTANCE.createLevel();
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

        ExplicitHierarchy storeHierarchy = HierarchyFactory.eINSTANCE.createExplicitHierarchy();
        storeHierarchy.setHasAll(true);
        storeHierarchy.setPrimaryKey( (Column) copier.get(CatalogSupplier.COLUMN_STORE_ID_STORE));
        storeHierarchy.setQuery(storeQuery);
        storeHierarchy.getLevels().add(storeCountryLevel);
        storeHierarchy.getLevels().add(storeStateLevel);
        storeHierarchy.getLevels().add(storeCityLevel);
        storeHierarchy.getLevels().add(storeNameLevel);

        storeDimension.getHierarchies().add(storeHierarchy);

        // Create Time dimension
        timeDimension = DimensionFactory.eINSTANCE.createTimeDimension();
        timeDimension.setName("Time");

        TableSource timeQuery = SourceFactory.eINSTANCE.createTableSource();
        timeQuery.setTable(timeCsvTable);

        // First hierarchy (default)
        Level yearLevel1 = LevelFactory.eINSTANCE.createLevel();
        yearLevel1.setName("Year");
        yearLevel1.setColumn(theYearColumnInTimeCsv);
        yearLevel1.setColumnType(ColumnInternalDataType.NUMERIC);
        yearLevel1.setUniqueMembers(true);
        yearLevel1.setType(LevelDefinition.TIME_YEARS);

        Level quarterLevel = LevelFactory.eINSTANCE.createLevel();
        quarterLevel.setName("Quarter");
        quarterLevel.setColumn(quarterColumnInTimeCsv);
        quarterLevel.setUniqueMembers(false);
        quarterLevel.setType(LevelDefinition.TIME_QUARTERS);

        Level monthLevel = LevelFactory.eINSTANCE.createLevel();
        monthLevel.setName("Month");
        monthLevel.setColumn(monthOfYearColumnInTimeCsv);
        monthLevel.setUniqueMembers(false);
        monthLevel.setColumnType(ColumnInternalDataType.NUMERIC);
        monthLevel.setType(LevelDefinition.TIME_MONTHS);

        ExplicitHierarchy timeHierarchy1 = HierarchyFactory.eINSTANCE.createExplicitHierarchy();
        timeHierarchy1.setHasAll(false);
        timeHierarchy1.setPrimaryKey(timeIdColumnInTimeCsv);
        timeHierarchy1.setQuery(timeQuery);
        timeHierarchy1.getLevels().add(yearLevel1);
        timeHierarchy1.getLevels().add(quarterLevel);
        timeHierarchy1.getLevels().add(monthLevel);

        // Second hierarchy (Weekly)
        Level yearLevel2 = LevelFactory.eINSTANCE.createLevel();
        yearLevel2.setName("Year");
        yearLevel2.setColumn(theYearColumnInTimeCsv);
        yearLevel2.setColumnType(ColumnInternalDataType.NUMERIC);
        yearLevel2.setUniqueMembers(true);
        yearLevel2.setType(LevelDefinition.TIME_YEARS);

        Level weekLevel = LevelFactory.eINSTANCE.createLevel();
        weekLevel.setName("Week");
        weekLevel.setColumn(weekOfYearColumnInTimeCsv);
        weekLevel.setColumnType(ColumnInternalDataType.NUMERIC);
        weekLevel.setUniqueMembers(false);
        weekLevel.setType(LevelDefinition.TIME_WEEKS);

        Level dayLevel = LevelFactory.eINSTANCE.createLevel();
        dayLevel.setName("Day");
        dayLevel.setColumn(dayOfMonthColumnTimeCsv);
        dayLevel.setColumnType(ColumnInternalDataType.NUMERIC);
        dayLevel.setUniqueMembers(false);
        dayLevel.setType(LevelDefinition.TIME_DAYS);

        ExplicitHierarchy timeHierarchy2 = HierarchyFactory.eINSTANCE.createExplicitHierarchy();
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
        unitSalesMeasure = MeasureFactory.eINSTANCE.createAvgMeasure();
        unitSalesMeasure.setName("Unit Sales");
        unitSalesMeasure.setColumn(unitSalesColumnInFactCsv2016);
        unitSalesMeasure.setFormatString("Standard");
    }

    protected void createCube() {
        // Create table query
        TableSource tableQuery = SourceFactory.eINSTANCE.createTableSource();
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
        DimensionConnector timeConnector = DimensionFactory.eINSTANCE.createDimensionConnector();
        timeConnector.setDimension(timeDimension);
        timeConnector.setOverrideDimensionName("Time");
        timeConnector.setForeignKey(timeIdColumnInFactCsv2016);

        DimensionConnector storeConnector = DimensionFactory.eINSTANCE.createDimensionConnector();
        storeConnector.setDimension(storeDimension);
        storeConnector.setOverrideDimensionName("Store");
        storeConnector.setForeignKey(storeIdColumnInFactCsv2016);

        // Create measures
        AvgMeasure storeCostMeasure = MeasureFactory.eINSTANCE.createAvgMeasure();
        storeCostMeasure.setName("Store Cost");
        storeCostMeasure.setColumn(storeCostColumnInFactCsv2016);
        storeCostMeasure.setFormatString("#,###.00");

        AvgMeasure storeSalesMeasure = MeasureFactory.eINSTANCE.createAvgMeasure();
        storeSalesMeasure.setName("Store Sales");
        storeSalesMeasure.setColumn(storeSalesColumnInFactCsv2016);
        storeSalesMeasure.setFormatString("#,###.00");

        // Create measure group
        MeasureGroup measureGroup = CubeFactory.eINSTANCE.createMeasureGroup();
        measureGroup.getMeasures().add(unitSalesMeasure);
        measureGroup.getMeasures().add(storeCostMeasure);
        measureGroup.getMeasures().add(storeSalesMeasure);

        // Create cube
        PhysicalCube cube = CubeFactory.eINSTANCE.createPhysicalCube();
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
