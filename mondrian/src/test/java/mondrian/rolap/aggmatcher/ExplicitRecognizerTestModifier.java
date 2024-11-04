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

import org.eclipse.daanse.rdb.structure.api.model.Column;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.SchemaMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.DataType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.LevelType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.MeasureAggregatorType;
import org.eclipse.daanse.rolap.mapping.instance.complex.foodmart.FoodmartMappingSupplier;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationExcludeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationLevelPropertyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationTableMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.JoinQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.JoinedQueryElementMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MemberMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MemberPropertyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SchemaMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TimeDimensionMappingImpl;


public class ExplicitRecognizerTestModifier extends PojoMappingModifier {

    public ExplicitRecognizerTestModifier(CatalogMapping catalog) {
        super(catalog);
    }


    /*
                + "<Schema name=\"FoodMart\">\n"
            + "  <Dimension name=\"Store\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">\n"
            + "      <Table name=\"store\"/>\n"
            + "      <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"Store State\" column=\"store_state\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"Store City\" column=\"store_city\" uniqueMembers=\"false\"/>\n"
            + "      <Level name=\"Store Name\" column=\"store_name\" uniqueMembers=\"true\">\n"
            + "        <Property name=\"Street address\" column=\"store_street_address\" type=\"String\"/>\n"
            + "      </Level>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Dimension name=\"Product\">\n"
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
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "<Cube name=\"ExtraCol\" defaultMeasure='#DEFMEASURE#'>\n"
            + "  <Table name=\"sales_fact_1997\">\n"
            + "           #AGGNAME# "
            + "  </Table>"
            + "  <Dimension name=\"TimeExtra\" foreignKey=\"time_id\">\n"
            + "    <Hierarchy hasAll=\"false\" primaryKey=\"time_id\">\n"
            + "      <Table name=\"time_by_day\"/>\n"
            + "      <Level name=\"Year\" #YEARCOLS#  type=\"Numeric\" uniqueMembers=\"true\""
            + "          levelType=\"TimeYears\">\n"
            + "      </Level>\n"
            + "      <Level name=\"Quarter\" #QTRCOLS#  uniqueMembers=\"false\""
            + "          levelType=\"TimeQuarters\">\n"
            + "      </Level>\n"
            + "      <Level name=\"Month\" #MONTHCOLS# uniqueMembers=\"false\" type=\"Numeric\""
            + "          levelType=\"TimeMonths\">\n"
            + "           #MONTHPROP# "
            + "      </Level>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Dimension name=\"Gender\" foreignKey=\"customer_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
            + "    <Table name=\"customer\"/>\n"
            + "      <Level name=\"Gender\" column=\"gender\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>  "
            + "  <DimensionUsage name=\"Store\" source=\"Store\" foreignKey=\"store_id\"/>"
            + "  <DimensionUsage name=\"Product\" source=\"Product\" foreignKey=\"product_id\"/>"
            + "<Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\" visible=\"false\"/>\n"
            + "<Measure name=\"Avg Unit Sales\" column=\"unit_sales\" aggregator=\"avg\"\n"
            + "      formatString=\"Standard\" visible=\"false\"/>\n"
            + "  <Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\"\n"
            + "      formatString=\"#,###.00\"/>\n"
            + "<Measure name=\"Customer Count\" column=\"customer_id\" aggregator=\"distinct-count\" formatString=\"#,###\"/>"
            + "</Cube>\n"
            + "</Schema>";
     */

    private static final MeasureMappingImpl unitSales = MeasureMappingImpl.builder()
	.withName("Unit Sales")
    .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
    .withAggregatorType(MeasureAggregatorType.SUM)
    .withFormatString("Standard")
    .withVisible(false)
    .build();

    private static final MeasureMappingImpl avgUnitSales = MeasureMappingImpl.builder()
    .withName("Avg Unit Sales")
    .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
    .withAggregatorType(MeasureAggregatorType.AVG)
    .withFormatString("Standard")
    .withVisible(false)
    .build();

    private static final MeasureMappingImpl storeCost = MeasureMappingImpl.builder()
    .withName("Store Cost")
    .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_SALES_FACT_1997)
    .withAggregatorType(MeasureAggregatorType.SUM)
    .withFormatString("#,###.00")
    .build();

    private static final MeasureMappingImpl customerCount = MeasureMappingImpl.builder()
    .withName("Customer Count")
    .withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
    .withAggregatorType(MeasureAggregatorType.DICTINCT_COUNT)
    .withFormatString("#,###")
    .build();


    protected SchemaMapping schema(SchemaMapping schemaMappingOriginal) {

    	StandardDimensionMappingImpl storeDimension = StandardDimensionMappingImpl.builder()
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
                        	MemberPropertyMappingImpl.builder()
                                .withName("Street address")
                                .withColumn(FoodmartMappingSupplier.STREET_ADDRESS_COLUMN_IN_STORE)
                                .withDataType(DataType.STRING)
                                .build()
                        ))
                        .build()

                ))
                .build()
        ))
        .build();

        StandardDimensionMappingImpl productDimension = StandardDimensionMappingImpl.builder()
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
                        .build()
                ))
                .build()
        ))
        .build();

        return SchemaMappingImpl.builder()
                .withName("FoodMart")
                .withCubes(List.of(
                    PhysicalCubeMappingImpl.builder()
                        .withName("ExtraCol")
                        .withDefaultMeasure(resolveMeasure(getDefaultMeasure()))
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
                        		.withAggregationExcludes(getAggExcludes()).withAggregationTables(getAggTables()) .build())
                        .withDimensionConnectors(List.of(
                        	DimensionConnectorMappingImpl.builder()
                        		.withOverrideDimensionName("TimeExtra")
                                .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(TimeDimensionMappingImpl.builder()
                                	.withName("TimeExtra")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(false)
                                        .withPrimaryKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.TIME_BY_DAY_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Year")
                                                .withColumn(getYearCol())
                                                .withType(DataType.NUMERIC)
                                                .withUniqueMembers(true)
                                                .withLevelType(LevelType.TIME_YEARS)
                                                .build(),
                                            LevelMappingImpl.builder()
                                                .withName("Quarter")
                                                .withColumn(getQuarterCol())
                                                .withUniqueMembers(false)
                                                .withLevelType(LevelType.TIME_QUARTERS)
                                                .build(),
                                            LevelMappingImpl.builder()
                                                .withName("Month")
                                                .withColumn(getMonthCol())
                                                .withCaptionColumn(getMonthCaptionCol())
                                                .withOrdinalColumn(getMonthOrdinalCol())
                                                .withNameColumn(getMonthNameCol())
                                                .withUniqueMembers(false)
                                                .withType(DataType.NUMERIC)
                                                .withLevelType(LevelType.TIME_MONTHS)
                                                .withMemberProperties(getMonthProp())
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
                                        .withHasAll(true)
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
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                            	.withOverrideDimensionName("Store")
                            	.withDimension(storeDimension)
                                .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                            	.withOverrideDimensionName("Product")
                            	.withDimension(productDimension)
                                .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                                .build()
                        ))

                        .withMeasureGroups(List.of(
                            	MeasureGroupMappingImpl.builder()
                            		.withMeasures(List.of(unitSales, avgUnitSales, storeCost, customerCount)).build()))
                        .build()
                ))
                .build();
    }

    private MemberMappingImpl resolveMeasure(String defaultMeasure) {
        switch(defaultMeasure) {
        	case "Unit Sales":
        		return unitSales;
        	case "Avg Unit Sales":
        		return avgUnitSales;
        	case "Store Cost":
        		return storeCost;
        	case "Customer Count":
        		return customerCount;
        	default: return null;
        }
	}


	protected List<MemberPropertyMappingImpl> getMonthProp() {
        return List.of();
    }

    protected Column getMonthOrdinalCol() {
        return null;
    }

    protected Column getMonthCaptionCol() {
        return null;
    }

    protected Column getQuarterCol() {
        return null;
    }

    protected Column getMonthNameCol() {
        return null;
    }

    protected Column getMonthCol() {
        return null;
    }

    protected Column getYearCol() {
        return null;
    }

    protected List<AggregationTableMappingImpl> getAggTables() {
        return List.of();
    }

    protected List<AggregationExcludeMappingImpl> getAggExcludes() {
        return List.of();
    }

    protected String getDefaultMeasure() {
        return null;
    }
}
