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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.rdb.structure.api.model.DatabaseSchema;
import org.eclipse.daanse.rdb.structure.api.model.Table;
import org.eclipse.daanse.rdb.structure.pojo.ColumnImpl;
import org.eclipse.daanse.rdb.structure.pojo.PhysicalTableImpl;
import org.eclipse.daanse.rdb.structure.pojo.PhysicalTableImpl.Builder;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.DataType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.LevelType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.MeasureAggregatorType;
import org.eclipse.daanse.rolap.mapping.instance.rec.complex.foodmart.FoodmartMappingSupplier;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationExcludeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationTableMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MemberPropertyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.CatalogMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TimeDimensionMappingImpl;

public class AggMeasureFactCountTestModifier extends PojoMappingModifier {
	private static DimensionMappingImpl storeDimension = StandardDimensionMappingImpl.builder()
            .withName("Store")
            .withHierarchies(List.of(
            	HierarchyMappingImpl.builder()
                    .withHasAll(true)
                    .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                    .withLevels(List.of(
                    	LevelMappingImpl.builder()
                            .withName("Store Country")
                            .withColumn(FoodmartMappingSupplier.STORE_COUNTRY_COLUMN_IN_STORE)
                            .withUniqueMembers(true)
                            .build(),
                        LevelMappingImpl.builder()
                            .withName("Store State")
                            .withColumn(FoodmartMappingSupplier.STORE_STATE_COLUMN_IN_STORE)
                            .withUniqueMembers(true)
                            .build(),
                        LevelMappingImpl.builder()
                            .withName("Store City")
                            .withColumn(FoodmartMappingSupplier.STORE_CITY_COLUMN_IN_STORE)
                            .withUniqueMembers(false)
                            .build(),
                        LevelMappingImpl.builder()
                            .withName("Store Name")
                            .withColumn(FoodmartMappingSupplier.STORE_NAME_COLUMN_IN_STORE)
                            .withUniqueMembers(true)
                            .withMemberProperties(List.of(
                                MemberPropertyMappingImpl.builder().withName("Store Type").withColumn(FoodmartMappingSupplier.STORE_TYPE_COLUMN_IN_STORE).build(),
                                MemberPropertyMappingImpl.builder().withName("Store Manager").withColumn(FoodmartMappingSupplier.STORE_MANAGER_COLUMN_IN_STORE).build(),
                                MemberPropertyMappingImpl.builder().withName("Store Sqft").withColumn(FoodmartMappingSupplier.STORE_SQFT_COLUMN_IN_STORE)
                                    .withDataType(DataType.NUMERIC).build(),
                                MemberPropertyMappingImpl.builder().withName("Grocery Sqft").withColumn(FoodmartMappingSupplier.GROCERY_SQFT_COLUMN_IN_STORE)
                                    .withDataType(DataType.NUMERIC).build(),
                                MemberPropertyMappingImpl.builder().withName("Frozen Sqft").withColumn(FoodmartMappingSupplier.FROZEN_SQFT_COLUMN_IN_STORE)
                                    .withDataType(DataType.NUMERIC).build(),
                                MemberPropertyMappingImpl.builder().withName("Meat Sqft").withColumn(FoodmartMappingSupplier.MEAT_SQFT_COLUMN_IN_STORE)
                                    .withDataType(DataType.NUMERIC).build(),
                                MemberPropertyMappingImpl.builder().withName("Has coffee bar").withColumn(FoodmartMappingSupplier.COFFEE_BAR_COLUMN_IN_STORE)
                                    .withDataType(DataType.BOOLEAN).build(),
                                MemberPropertyMappingImpl.builder().withName("Street address").withColumn(FoodmartMappingSupplier.STREET_ADDRESS_COLUMN_IN_STORE)
                                .withDataType(DataType.STRING).build()
                                ))
                            .build()
                    ))
                    .build()
            ))
			.build();

    public static final ColumnImpl TIME_ID_COLUMN_IN_TIME_CSV = ColumnImpl.builder().withName("time_id").withType("INTEGER").build();
    public static final ColumnImpl THE_YEAR_COLUMN_IN_TIME_CSV = ColumnImpl.builder().withName("the_year").withType("SMALLINT").build();
    public static final ColumnImpl MONTH_OF_YEAR_COLUMN_IN_TIME_CSV = ColumnImpl.builder().withName("month_of_year").withType("SMALLINT").build();
    public static final ColumnImpl QUARTER_COLUMN_IN_TIME_CSV = ColumnImpl.builder().withName("quarter").withType("VARCHAR").withCharOctetLength(30).build();
    public static final ColumnImpl DAY_OF_MONTH_COLUMN_TIME_CSV = ColumnImpl.builder().withName("day_of_month").withType("SMALLINT").build();
    public static final ColumnImpl WEEK_OF_YEAR_COLUMN_IN_TIME_CSV = ColumnImpl.builder().withName("week_of_year").withType("INTEGER").build();
    public static final PhysicalTableImpl TIME_CSV_TABLE = ((org.eclipse.daanse.rdb.structure.pojo.PhysicalTableImpl.Builder) PhysicalTableImpl.builder().withName("time_csv")
            .withColumns(List.of(
                    TIME_ID_COLUMN_IN_TIME_CSV,
                    THE_YEAR_COLUMN_IN_TIME_CSV,
                    MONTH_OF_YEAR_COLUMN_IN_TIME_CSV,
                    QUARTER_COLUMN_IN_TIME_CSV,
                    WEEK_OF_YEAR_COLUMN_IN_TIME_CSV,
                    DAY_OF_MONTH_COLUMN_TIME_CSV
                    ))).build();

    //## TableName: fact_csv_2016
    //## ColumnNames: product_id,time_id,customer_id,promotion_id,store_id,store_sales,store_cost,unit_sales
    //## ColumnTypes: INTEGER,INTEGER,INTEGER,INTEGER,INTEGER,DECIMAL(10,4):null,DECIMAL(10,4):null,DECIMAL(10,4):null
    public static final ColumnImpl PRODUCT_ID_COLUMN_IN_FACT_CSV_2016 = ColumnImpl.builder().withName("product_id").withType("INTEGER").build();
    public static final ColumnImpl TIME_ID_COLUMN_IN_FACT_CSV_2016 = ColumnImpl.builder().withName("time_id").withType("INTEGER").build();
    public static final ColumnImpl CUSTOMER_ID_COLUMN_IN_FACT_CSV_2016 = ColumnImpl.builder().withName("customer_id").withType("INTEGER").build();
    public static final ColumnImpl PROMOTION_ID_COLUMN_IN_FACT_CSV_2016 = ColumnImpl.builder().withName("promotion_id").withType("INTEGER").build();
    public static final ColumnImpl STORE_ID_COLUMN_IN_FACT_CSV_2016 = ColumnImpl.builder().withName("store_id").withType("INTEGER").build();
    public static final ColumnImpl STORE_SALES_COLUMN_IN_FACT_CSV_2016 = ColumnImpl.builder().withName("store_sales").withType("DECIMAL").withColumnSize(10).withDecimalDigits(4).withNullable(true).build();
    public static final ColumnImpl STORE_COST_COLUMN_IN_FACT_CSV_2016 = ColumnImpl.builder().withName("store_cost").withType("DECIMAL").withColumnSize(10).withDecimalDigits(4).withNullable(true).build();
    public static final ColumnImpl UNIT_SALES_COLUMN_IN_FACT_CSV_2016 = ColumnImpl.builder().withName("unit_sales").withType("DECIMAL").withColumnSize(10).withDecimalDigits(4).withNullable(true).build();
    public static final PhysicalTableImpl FACT_CSV_2016_TABLE = ((org.eclipse.daanse.rdb.structure.pojo.PhysicalTableImpl.Builder) PhysicalTableImpl.builder().withName("fact_csv_2016")
            .withColumns(List.of(
            		PRODUCT_ID_COLUMN_IN_FACT_CSV_2016,
                    TIME_ID_COLUMN_IN_FACT_CSV_2016,
                    CUSTOMER_ID_COLUMN_IN_FACT_CSV_2016,
                    PROMOTION_ID_COLUMN_IN_FACT_CSV_2016,
                    STORE_ID_COLUMN_IN_FACT_CSV_2016,
                    STORE_SALES_COLUMN_IN_FACT_CSV_2016,
                    STORE_COST_COLUMN_IN_FACT_CSV_2016,
                    UNIT_SALES_COLUMN_IN_FACT_CSV_2016

                    ))).build();

    //## TableName: agg_c_6_fact_csv_2016
    //## ColumnNames: month_of_year,quarter,the_year,store_sales,store_cost,unit_sales,customer_count,fact_count,store_sales_fact_count,store_cost_fact_count,unit_sales_fact_count
    //## ColumnTypes: SMALLINT,VARCHAR(30),SMALLINT,DECIMAL(10,4):null,DECIMAL(10,4):null,DECIMAL(10,4):null,INTEGER,INTEGER,INTEGER,INTEGER,INTEGER
    public static final ColumnImpl monthOfYearAggC6FactCsv2016 = ColumnImpl.builder().withName("month_of_year").withType("INTEGER").build();
    public static final ColumnImpl quarterAggC6FactCsv2016 = ColumnImpl.builder().withName("quarter").withType("VARCHAR").withCharOctetLength(30).build();
    public static final ColumnImpl theYearAggC6FactCsv2016 = ColumnImpl.builder().withName("the_year").withType("INTEGER").build();
    public static final ColumnImpl storeSalesAggC6FactCsv2016 = ColumnImpl.builder().withName("store_sales").withType("DECIMAL").withColumnSize(10).withDecimalDigits(4).withNullable(true).build();
    public static final ColumnImpl storeCostAggC6FactCsv2016 = ColumnImpl.builder().withName("store_cost").withType("DECIMAL").withColumnSize(10).withDecimalDigits(4).withNullable(true).build();
    public static final ColumnImpl unitSalesAggC6FactCsv2016 = ColumnImpl.builder().withName("unit_sales").withType("DECIMAL").withColumnSize(10).withDecimalDigits(4).withNullable(true).build();
    public static final ColumnImpl customerCountAggC6FactCsv2016 = ColumnImpl.builder().withName("customer_count").withType("INTEGER").build();
    public static final ColumnImpl factCountAggC6FactCsv2016 = ColumnImpl.builder().withName("fact_count").withType("INTEGER").build();
    public static final ColumnImpl storeSalesFactCountAggC6FactCsv2016 = ColumnImpl.builder().withName("store_sales_fact_count").withType("INTEGER").build();
    public static final ColumnImpl storeCostFactCountAggC6FactCsv2016 = ColumnImpl.builder().withName("store_cost_fact_count").withType("INTEGER").build();
    public static final ColumnImpl unitSalesFactCountAggC6FactCsv2016 = ColumnImpl.builder().withName("unit_sales_fact_count").withType("INTEGER").build();
    public static final PhysicalTableImpl aggC6FactCsv2016 = ((Builder) PhysicalTableImpl.builder().withName("agg_c_6_fact_csv_2016")
            .withColumns(List.of(
                    monthOfYearAggC6FactCsv2016,
                    quarterAggC6FactCsv2016,
                    theYearAggC6FactCsv2016,
                    storeSalesAggC6FactCsv2016,
                    storeCostAggC6FactCsv2016,
                    unitSalesAggC6FactCsv2016,
                    customerCountAggC6FactCsv2016,
                    factCountAggC6FactCsv2016,
                    storeSalesFactCountAggC6FactCsv2016,
                    storeCostFactCountAggC6FactCsv2016,
                    unitSalesFactCountAggC6FactCsv2016
            ))).build();

    //## TableName: agg_csv_different_column_names
    //## ColumnNames: month_of_year,quarter,the_year,store_sales,store_cost,unit_sales,customer_count,fact_count,ss_fc,sc_fc,us_fc
    //## ColumnTypes: SMALLINT,VARCHAR(30),SMALLINT,DECIMAL(10,4):null,DECIMAL(10,4):null,DECIMAL(10,4):null,INTEGER,INTEGER,INTEGER,INTEGER,INTEGER
    public static final ColumnImpl monthOfYearAggCsvDifferentColumnNames = ColumnImpl.builder().withName("month_of_year").withType("INTEGER").build();
    public static final ColumnImpl quarterAggCsvDifferentColumnNames = ColumnImpl.builder().withName("quarter").withType("VARCHAR").withCharOctetLength(30).build();
    public static final ColumnImpl theYearAggCsvDifferentColumnNames = ColumnImpl.builder().withName("the_year").withType("INTEGER").build();
    public static final ColumnImpl storeSalesAggCsvDifferentColumnNames = ColumnImpl.builder().withName("store_sales").withType("DECIMAL").withColumnSize(10).withDecimalDigits(4).withNullable(true).build();
    public static final ColumnImpl storeCostAggCsvDifferentColumnNames = ColumnImpl.builder().withName("store_cost").withType("DECIMAL").withColumnSize(10).withDecimalDigits(4).withNullable(true).build();
    public static final ColumnImpl unitSalesAggCsvDifferentColumnNames = ColumnImpl.builder().withName("unit_sales").withType("DECIMAL").withColumnSize(10).withDecimalDigits(4).withNullable(true).build();
    public static final ColumnImpl customerCountAggCsvDifferentColumnNames = ColumnImpl.builder().withName("customer_count").withType("INTEGER").build();
    public static final ColumnImpl factCountAggCsvDifferentColumnNames = ColumnImpl.builder().withName("fact_count").withType("INTEGER").build();
    public static final ColumnImpl ssFcAggCsvDifferentColumnNames = ColumnImpl.builder().withName("ss_fc").withType("INTEGER").build();
    public static final ColumnImpl scFcAggCsvDifferentColumnNames = ColumnImpl.builder().withName("sc_fc").withType("INTEGER").build();
    public static final ColumnImpl usFcAggCsvDifferentColumnNames = ColumnImpl.builder().withName("us_fc").withType("INTEGER").build();
    public static final PhysicalTableImpl aggCsvDifferentColumnNames = ((Builder) PhysicalTableImpl.builder().withName("agg_csv_different_column_names")
            .withColumns(List.of(
                    monthOfYearAggCsvDifferentColumnNames,
                    quarterAggCsvDifferentColumnNames,
                    theYearAggCsvDifferentColumnNames,
                    storeSalesAggCsvDifferentColumnNames,
                    storeCostAggCsvDifferentColumnNames,
                    unitSalesAggCsvDifferentColumnNames,
                    customerCountAggCsvDifferentColumnNames,
                    factCountAggCsvDifferentColumnNames,
                    ssFcAggCsvDifferentColumnNames,
                    scFcAggCsvDifferentColumnNames,
                    usFcAggCsvDifferentColumnNames
            ))).build();

    //## TableName: agg_csv_divide_by_zero
    //## ColumnNames: month_of_year,quarter,the_year,store_sales,store_cost,unit_sales,customer_count,fact_count,store_sales_fact_count,store_cost_fact_count,unit_sales_fact_count
    //## ColumnTypes: SMALLINT,VARCHAR(30),SMALLINT,DECIMAL(10,4):null,DECIMAL(10,4):null,DECIMAL(10,4):null,INTEGER,INTEGER,INTEGER,INTEGER,INTEGER
    public static final ColumnImpl monthOfYearAggCsvDivideByZero = ColumnImpl.builder().withName("month_of_year").withType("INTEGER").build();
    public static final ColumnImpl quarterAggCsvDivideByZero = ColumnImpl.builder().withName("quarter").withType("VARCHAR").withCharOctetLength(30).build();
    public static final ColumnImpl theYearAggCsvDivideByZero = ColumnImpl.builder().withName("the_year").withType("INTEGER").build();
    public static final ColumnImpl storeSalesAggCsvDivideByZero = ColumnImpl.builder().withName("store_sales").withType("DECIMAL").withColumnSize(10).withDecimalDigits(4).withNullable(true).build();
    public static final ColumnImpl storeCostAggCsvDivideByZero = ColumnImpl.builder().withName("store_cost").withType("DECIMAL").withColumnSize(10).withDecimalDigits(4).withNullable(true).build();
    public static final ColumnImpl unitSalesAggCsvDivideByZero = ColumnImpl.builder().withName("unit_sales").withType("DECIMAL").withColumnSize(10).withDecimalDigits(4).withNullable(true).build();
    public static final ColumnImpl customerCountAggCsvDivideByZero = ColumnImpl.builder().withName("customer_count").withType("INTEGER").build();
    public static final ColumnImpl factCountAggCsvDivideByZero = ColumnImpl.builder().withName("fact_count").withType("INTEGER").build();
    public static final ColumnImpl storeSalesFactCountAggCsvDivideByZero = ColumnImpl.builder().withName("store_sales_fact_count").withType("INTEGER").build();
    public static final ColumnImpl storeCostFactCountAggCsvDivideByZero = ColumnImpl.builder().withName("store_cost_fact_count").withType("INTEGER").build();
    public static final ColumnImpl unitSalesFactCountAggCsvDivideByZero = ColumnImpl.builder().withName("unit_sales_fact_count").withType("INTEGER").build();
    public static final PhysicalTableImpl aggCsvDivideByZero = ((Builder) PhysicalTableImpl.builder().withName("agg_csv_divide_by_zero")
            .withColumns(List.of(
                    monthOfYearAggCsvDivideByZero,
                    quarterAggCsvDivideByZero,
                    theYearAggCsvDivideByZero,
                    storeSalesAggCsvDivideByZero,
                    storeCostAggCsvDivideByZero,
                    unitSalesAggCsvDivideByZero,
                    customerCountAggCsvDivideByZero,
                    factCountAggCsvDivideByZero,
                    storeSalesFactCountAggCsvDivideByZero,
                    storeCostFactCountAggCsvDivideByZero,
                    unitSalesFactCountAggCsvDivideByZero
            ))).build();

	private static DimensionMappingImpl timeDimension = TimeDimensionMappingImpl.builder()
            .withName("Time")
            .withHierarchies(List.of(
                HierarchyMappingImpl.builder()
                    .withHasAll(false)
                    .withPrimaryKey(TIME_ID_COLUMN_IN_TIME_CSV)
                    .withQuery(TableQueryMappingImpl.builder().withTable(TIME_CSV_TABLE).build())
                    .withLevels(List.of(
                        LevelMappingImpl.builder()
                            .withName("Year")
                            .withColumn(THE_YEAR_COLUMN_IN_TIME_CSV)
                            .withType(DataType.NUMERIC)
                            .withUniqueMembers(true)
                            .withLevelType(LevelType.TIME_YEARS)
                            .build(),
                        LevelMappingImpl.builder()
                            .withName("Quarter")
                            .withColumn(QUARTER_COLUMN_IN_TIME_CSV)
                            .withUniqueMembers(false)
                            .withLevelType(LevelType.TIME_QUARTERS)
                            .build(),
                        LevelMappingImpl.builder()
                            .withName("Month")
                            .withColumn(MONTH_OF_YEAR_COLUMN_IN_TIME_CSV)
                            .withUniqueMembers(false)
                            .withType(DataType.NUMERIC)
                            .withLevelType(LevelType.TIME_MONTHS)
                            .build()
                    ))
                    .build(),
                HierarchyMappingImpl.builder()
                    .withHasAll(true)
                    .withName("Weekly")
                    .withPrimaryKey(TIME_ID_COLUMN_IN_TIME_CSV)
                    .withQuery(TableQueryMappingImpl.builder().withTable(TIME_CSV_TABLE).build())
                    .withLevels(List.of(
                        LevelMappingImpl.builder()
                            .withName("Year")
                            .withColumn(THE_YEAR_COLUMN_IN_TIME_CSV)
                            .withType(DataType.NUMERIC)
                            .withUniqueMembers(true)
                            .withLevelType(LevelType.TIME_YEARS)
                            .build(),
                        LevelMappingImpl.builder()
                            .withName("Week")
                            .withColumn(WEEK_OF_YEAR_COLUMN_IN_TIME_CSV)
                            .withType(DataType.NUMERIC)
                            .withUniqueMembers(false)
                            .withLevelType(LevelType.TIME_WEEKS)
                            .build(),
                        LevelMappingImpl.builder()
                            .withName("Day")
                            .withColumn(DAY_OF_MONTH_COLUMN_TIME_CSV)
                            .withType(DataType.NUMERIC)
                            .withUniqueMembers(false)
                            .withLevelType(LevelType.TIME_DAYS)
                            .build()
                    ))
                    .build()
            ))
            .build();

	private static MeasureMappingImpl unitSales = MeasureMappingImpl.builder()
			.withName("Unit Sales")
			.withColumn(UNIT_SALES_COLUMN_IN_FACT_CSV_2016)
			.withAggregatorType(MeasureAggregatorType.AVG)
			.withFormatString("Standard")
			.build();

    public AggMeasureFactCountTestModifier(CatalogMapping catalogMapping) {
        super(catalogMapping);
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
    @Override
    protected List<? extends Table> databaseSchemaTables(DatabaseSchema databaseSchema) {
        List<Table> result = new ArrayList();
        result.addAll(super.databaseSchemaTables(databaseSchema));
        result.addAll(List.of(aggC6FactCsv2016, aggCsvDifferentColumnNames, aggCsvDivideByZero, TIME_CSV_TABLE, FACT_CSV_2016_TABLE));
        return result;
    }

    @Override
    protected CatalogMapping modifyCatalog(CatalogMapping mappingSchemaOriginal) {
        return CatalogMappingImpl.builder()
            .withName("FoodMart")
            .withCubes(List.of(
            	PhysicalCubeMappingImpl.builder()
                    .withName("Sales")
                    .withDefaultMeasure(unitSales)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FACT_CSV_2016_TABLE).withAggregationExcludes(getAggExcludes()).withAggregationTables(getAggTables()).build())
                    .withDimensionConnectors(List.of(
                    		DimensionConnectorMappingImpl.builder().withDimension(timeDimension).withOverrideDimensionName("Time").withForeignKey(TIME_ID_COLUMN_IN_FACT_CSV_2016).build(),
                    		DimensionConnectorMappingImpl.builder().withDimension(storeDimension).withOverrideDimensionName("Store").withForeignKey(STORE_ID_COLUMN_IN_FACT_CSV_2016).build()
                    		))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                    	unitSales,
                        MeasureMappingImpl.builder()
                            .withName("Store Cost")
                            .withColumn(STORE_COST_COLUMN_IN_FACT_CSV_2016)
                            .withAggregatorType(MeasureAggregatorType.AVG)
                            .withFormatString("#,###.00")
                            .build(),
                        MeasureMappingImpl.builder()
                            .withName("Store Sales")
                            .withColumn(STORE_SALES_COLUMN_IN_FACT_CSV_2016)
                            .withAggregatorType(MeasureAggregatorType.AVG)
                            .withFormatString("#,###.00")
                            .build()
                    		))
                    .build()
                    )).build()
                    )).build();

    }

    protected List<AggregationTableMappingImpl> getAggTables() {
        return List.of();
    }

    protected List<AggregationExcludeMappingImpl> getAggExcludes() {
        return List.of();
    }
}
