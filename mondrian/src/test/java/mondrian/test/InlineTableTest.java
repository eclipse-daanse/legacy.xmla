/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.test;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.getDialect;
import static org.opencube.junit5.TestUtil.withSchema;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.ColumnDataType;
import org.eclipse.daanse.rolap.mapping.instance.rec.complex.foodmart.FoodmartMappingSupplier;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalColumnMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.InlineTableMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.InlineTableQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.JoinQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.JoinedQueryElementMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.RowMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.RowValueMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SumMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.enums.DatabaseProduct;

/**
 * Unit test for the InlineTable element, defining tables whose values are held
 * in the Mondrian schema file, not in the database.
 *
 * @author jhyde
 */
class InlineTableTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testInlineTable(Context context) {
        final String cubeName = "Sales_inline";
        class TestInlineTableModifier extends PojoMappingModifier {

            public TestInlineTableModifier(CatalogMapping catalog) {
                super(catalog);
            }

            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                PhysicalColumnMappingImpl promoId = PhysicalColumnMappingImpl.builder().withName("promo_id").withDataType(ColumnDataType.NUMERIC).build();
                PhysicalColumnMappingImpl promoName = PhysicalColumnMappingImpl.builder().withName("promo_name").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
                InlineTableMappingImpl t = InlineTableMappingImpl.builder()
                .withName("alt_promotion")
                .withColumns(List.of(promoId, promoName))
                .withRows(List.of(
                       RowMappingImpl.builder().withRowValues(List.of(
                            RowValueMappingImpl.builder().withColumn(promoId).withValue("0").build(),
                            RowValueMappingImpl.builder().withColumn(promoName).withValue("Promo0").build())).build(),
                       RowMappingImpl.builder().withRowValues(List.of(
                               RowValueMappingImpl.builder().withColumn(promoId).withValue("1").build(),
                               RowValueMappingImpl.builder().withColumn(promoName).withValue("Promo1").build())).build()
                ))
                .build();

                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                result.add(PhysicalCubeMappingImpl.builder()
                    .withName(cubeName)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Time")
                            .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(FoodmartMappingSupplier.DIMENSION_TIME)
                            .build(),
                        DimensionConnectorMappingImpl.builder()
                        	.withOverrideDimensionName("Alternative Promotion")
                        	.withForeignKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997)
                        	.withDimension(
                        		StandardDimensionMappingImpl.builder()
                        			.withName("Alternative Promotion")
                        			.withHierarchies(List.of(
                        				HierarchyMappingImpl.builder()
                        					.withHasAll(true)
                        					.withPrimaryKey(promoId)
                        					.withQuery(InlineTableQueryMappingImpl.builder()
                        							.withAlias("alt_promotion")
                        							.withTable(t)
                        							.build()
                        					)
                        					.withLevels(List.of(
                        						LevelMappingImpl.builder()
                        							.withName("Alternative Promotion").withColumn(promoId)
                        							.withNameColumn(promoName)
                        							.withUniqueMembers(true)
                        							.build()
                        					))
                        					.build()
                            ))
                            .build()
                        ).build()
                    ))
                    .withMeasureGroups(List.of(
                    	MeasureGroupMappingImpl.builder()
                    	.withMeasures(List.of(
                            SumMeasureMappingImpl.builder()
                                .withName("Unit Sales")
                                .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                .withFormatString("Standard")
                                .withVisible(true)
                                .build(),
                            SumMeasureMappingImpl.builder()
                                .withName("Store Sales")
                                .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_SALES_FACT_1997)
                                .withFormatString("#,###.00")
                                .build()
                    	))
                    	.build()
                    ))
                    .build());
                return result;
            }
        }

        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"" + cubeName + "\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "  <Dimension name=\"Alternative Promotion\" foreignKey=\"promotion_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"promo_id\">\n"
            + "      <InlineTable alias=\"alt_promotion\">\n"
            + "        <ColumnDefs>\n"
            + "          <ColumnDef name=\"promo_id\" type=\"Numeric\"/>\n"
            + "          <ColumnDef name=\"promo_name\" type=\"String\"/>\n"
            + "        </ColumnDefs>\n"
            + "        <Rows>\n"
            + "          <Row>\n"
            + "            <Value column=\"promo_id\">0</Value>\n"
            + "            <Value column=\"promo_name\">Promo0</Value>\n"
            + "          </Row>\n"
            + "          <Row>\n"
            + "            <Value column=\"promo_id\">1</Value>\n"
            + "            <Value column=\"promo_name\">Promo1</Value>\n"
            + "          </Row>\n"
            + "        </Rows>\n"
            + "      </InlineTable>\n"
            + "      <Level name=\"Alternative Promotion\" column=\"promo_id\" nameColumn=\"promo_name\" uniqueMembers=\"true\"/> \n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\" visible=\"false\"/>\n"
            + "  <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"#,###.00\"/>\n"
            + "</Cube>",
            null,
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestInlineTableModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Alternative Promotion].[All Alternative Promotions].children} ON COLUMNS\n"
            + "from [" + cubeName + "] ",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Alternative Promotion].[Alternative Promotion].[Promo0]}\n"
            + "{[Alternative Promotion].[Alternative Promotion].[Promo1]}\n"
            + "Row #0: 195,448\n"
            + "Row #0: \n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testInlineTableInSharedDim(Context context) {
        final String cubeName = "Sales_inline_shared";

        class TestInlineTableInSharedDimModifier extends PojoMappingModifier {

            public TestInlineTableInSharedDimModifier(CatalogMapping catalog) {
                super(catalog);
            }

            private static final PhysicalColumnMappingImpl promoId = PhysicalColumnMappingImpl.builder().withName("promo_id").withDataType(ColumnDataType.INTEGER).build();
            private static final PhysicalColumnMappingImpl promoName = PhysicalColumnMappingImpl.builder().withName("promo_name").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
            private static final InlineTableMappingImpl t = InlineTableMappingImpl.builder()
            .withName("alt_promotion")
            .withColumns(List.of(promoId, promoName))
            .withRows(List.of(
                   RowMappingImpl.builder().withRowValues(List.of(
                        RowValueMappingImpl.builder().withColumn(promoId).withValue("0").build(),
                        RowValueMappingImpl.builder().withColumn(promoName).withValue("First promo").build())).build(),
                   RowMappingImpl.builder().withRowValues(List.of(
                           RowValueMappingImpl.builder().withColumn(promoId).withValue("1").build(),
                           RowValueMappingImpl.builder().withColumn(promoName).withValue("Second promo").build())).build()
            ))
            .build();

            private static final StandardDimensionMappingImpl d = StandardDimensionMappingImpl.builder()
                    .withName("Shared Alternative Promotion")
                    .withHierarchies(List.of(
                        HierarchyMappingImpl.builder()
                            .withHasAll(true)
                            .withPrimaryKey(promoId)
                            .withQuery(InlineTableQueryMappingImpl.builder()
                                .withAlias("alt_promotion")
                                .withTable(t)
                                .build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withName("Alternative Promotion")
                                    .withColumn(promoId)
                                    .withNameColumn(promoName)
                                    .withUniqueMembers(true)
                                    .build()
                            ))
                            .build())).build();

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                result.add(PhysicalCubeMappingImpl.builder()
                    .withName(cubeName)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Time")
                    		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                            .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                            .build(),
                        DimensionConnectorMappingImpl.builder()
                            .withOverrideDimensionName("Shared Alternative Promotion")
                            .withDimension(d)
                            .withForeignKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997)
                            .build()
                    ))
                    .withMeasureGroups(List.of(
                    	MeasureGroupMappingImpl.builder()
                    	.withMeasures(List.of(
                            SumMeasureMappingImpl.builder()
                                .withName("Unit Sales")
                                .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                .withFormatString("Standard")
                                .withVisible(false)
                                .build(),
                            SumMeasureMappingImpl.builder()
                                .withName("Store Sales")
                                .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_SALES_FACT_1997)
                                .withFormatString("#,###.00")
                                .build()
                    	))
                    	.build()
                        ))
                    .build());
                return result;
            }

            }

       /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "  <Dimension name=\"Shared Alternative Promotion\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"promo_id\">\n"
            + "      <InlineTable alias=\"alt_promotion\">\n"
            + "        <ColumnDefs>\n"
            + "          <ColumnDef name=\"promo_id\" type=\"Numeric\"/>\n"
            + "          <ColumnDef name=\"promo_name\" type=\"String\"/>\n"
            + "        </ColumnDefs>\n"
            + "        <Rows>\n"
            + "          <Row>\n"
            + "            <Value column=\"promo_id\">0</Value>\n"
            + "            <Value column=\"promo_name\">First promo</Value>\n"
            + "          </Row>\n"
            + "          <Row>\n"
            + "            <Value column=\"promo_id\">1</Value>\n"
            + "            <Value column=\"promo_name\">Second promo</Value>\n"
            + "          </Row>\n"
            + "        </Rows>\n"
            + "      </InlineTable>\n"
            + "      <Level name=\"Alternative Promotion\" column=\"promo_id\" nameColumn=\"promo_name\" uniqueMembers=\"true\"/> \n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "<Cube name=\""
            + cubeName
            + "\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "  <DimensionUsage name=\"Shared Alternative Promotion\" source=\"Shared Alternative Promotion\" foreignKey=\"promotion_id\"/>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\" visible=\"false\"/>\n"
            + "  <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"#,###.00\"/>\n"
            + "</Cube>",
            null,
            null,
            null,
            null);
        withSchema(context, schema);
        */
        withSchema(context, TestInlineTableInSharedDimModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Shared Alternative Promotion].[All Shared Alternative Promotions].children} ON COLUMNS\n"
            + "from [" + cubeName + "] ",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Shared Alternative Promotion].[Shared Alternative Promotion].[First promo]}\n"
            + "{[Shared Alternative Promotion].[Shared Alternative Promotion].[Second promo]}\n"
            + "Row #0: 195,448\n"
            + "Row #0: \n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testInlineTableSnowflake(Context context) {
        if (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())
            == DatabaseProduct.INFOBRIGHT)
        {
            // Infobright has a bug joining an inline table. Gives error
            // "Illegal mix of collations (ascii_bin,IMPLICIT) and
            // (utf8_general_ci,COERCIBLE) for operation '='".
            return;
        }
        final String cubeName = "Sales_inline_snowflake";
        class TestInlineTableSnowflakeModifier extends PojoMappingModifier {

            public TestInlineTableSnowflakeModifier(CatalogMapping catalog) {
                super(catalog);
            }

            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                PhysicalColumnMappingImpl nationName = PhysicalColumnMappingImpl.builder().withName("nation_name").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
                PhysicalColumnMappingImpl nationShortcode = PhysicalColumnMappingImpl.builder().withName("nation_shortcode").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
                InlineTableMappingImpl t = InlineTableMappingImpl.builder()
                .withName("nation")
                .withColumns(List.of(nationName, nationShortcode))
                .withRows(List.of(
                       RowMappingImpl.builder().withRowValues(List.of(
                            RowValueMappingImpl.builder().withColumn(nationName).withValue("USA").build(),
                            RowValueMappingImpl.builder().withColumn(nationShortcode).withValue("US").build())).build(),
                       RowMappingImpl.builder().withRowValues(List.of(
                               RowValueMappingImpl.builder().withColumn(nationName).withValue("Mexico").build(),
                               RowValueMappingImpl.builder().withColumn(nationShortcode).withValue("MX").build())).build(),
                       RowMappingImpl.builder().withRowValues(List.of(
                               RowValueMappingImpl.builder().withColumn(nationName).withValue("Canada").build(),
                               RowValueMappingImpl.builder().withColumn(nationShortcode).withValue("CA").build())).build()
                ))
                .build();
            	List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                JoinQueryMappingImpl j = JoinQueryMappingImpl.builder()
                		.withLeft(JoinedQueryElementMappingImpl.builder()
                				.withKey(FoodmartMappingSupplier.STORE_COUNTRY_COLUMN_IN_STORE)
                				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                				.build())
                		.withRight(JoinedQueryElementMappingImpl.builder()
                				.withKey(nationName)
                				.withQuery(InlineTableQueryMappingImpl.builder()
                                .withAlias("nation")
                                .withTable(t)
                				.build()).build())
                		.build();

                result.add(PhysicalCubeMappingImpl.builder()
                    .withName(cubeName)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Time")
                    		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                            .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                            .build(),
                      	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Store")
                            .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(
                            	StandardDimensionMappingImpl.builder()
                            		.withName("Store")
                            		.withHierarchies(List.of(
                            			HierarchyMappingImpl.builder()
                            				.withHasAll(true)
                            				.withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                            				.withQuery(j)
                                    .withLevels(List.of(
                                        LevelMappingImpl.builder()
                                            .withName("Store Country")
                                            .withColumn(nationName)
                                            .withNameColumn(nationShortcode)
                                            .withUniqueMembers(true)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("Store State")
                                            .withColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
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
                                            .build()
                                        ))
                                    .build()
                            ))
                            .build()
                          ).build()
                    ))
                    .withMeasureGroups(List.of(
                    		MeasureGroupMappingImpl.builder()
                    		.withMeasures(List.of(
                                SumMeasureMappingImpl.builder()
                                    .withName("Unit Sales")
                                    .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                    .withFormatString("Standard")
                                    .withVisible(false)
                                    .build(),
                                SumMeasureMappingImpl.builder()
                                    .withName("Store Sales")
                                    .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_SALES_FACT_1997)
                                    .withFormatString("#,###.00")
                                    .build()
                    				))
                    		.build()
                    		))
                    .build());
                return result;
            }

        }

        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"" + cubeName + "\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "  <Dimension name=\"Store\" foreignKeyTable=\"store\" foreignKey=\"store_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKeyTable=\"store\" primaryKey=\"store_id\">\n"
            + "      <Join leftKey=\"store_country\" rightKey=\"nation_name\">\n"
            + "      <Table name=\"store\"/>\n"
            + "        <InlineTable alias=\"nation\">\n"
            + "          <ColumnDefs>\n"
            + "            <ColumnDef name=\"nation_name\" type=\"String\"/>\n"
            + "            <ColumnDef name=\"nation_shortcode\" type=\"String\"/>\n"
            + "          </ColumnDefs>\n"
            + "          <Rows>\n"
            + "            <Row>\n"
            + "              <Value column=\"nation_name\">USA</Value>\n"
            + "              <Value column=\"nation_shortcode\">US</Value>\n"
            + "            </Row>\n"
            + "            <Row>\n"
            + "              <Value column=\"nation_name\">Mexico</Value>\n"
            + "              <Value column=\"nation_shortcode\">MX</Value>\n"
            + "            </Row>\n"
            + "            <Row>\n"
            + "              <Value column=\"nation_name\">Canada</Value>\n"
            + "              <Value column=\"nation_shortcode\">CA</Value>\n"
            + "            </Row>\n"
            + "          </Rows>\n"
            + "        </InlineTable>\n"
            + "      </Join>\n"
            + "      <Level name=\"Store Country\" table=\"nation\" column=\"nation_name\" nameColumn=\"nation_shortcode\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"Store State\" table=\"store\" column=\"store_state\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"Store City\" table=\"store\" column=\"store_city\" uniqueMembers=\"false\"/>\n"
            + "      <Level name=\"Store Name\" table=\"store\" column=\"store_name\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\" visible=\"false\"/>\n"
            + "  <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"#,###.00\"/>\n"
            + "</Cube>",
            null,
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestInlineTableSnowflakeModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Store].children} ON COLUMNS\n"
            + "from [" + cubeName + "] ",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[Store].[CA]}\n"
            + "{[Store].[Store].[MX]}\n"
            + "{[Store].[Store].[US]}\n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: 266,773\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testInlineTableDate(Context context) {
        final String cubeName = "Sales_Inline_Date";
        class TestInlineTableDateModifier extends PojoMappingModifier {

            public TestInlineTableDateModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                PhysicalColumnMappingImpl nationName = PhysicalColumnMappingImpl.builder().withName("nation_name").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
                PhysicalColumnMappingImpl nationShortcode = PhysicalColumnMappingImpl.builder().withName("nation_shortcode").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
                InlineTableMappingImpl t = InlineTableMappingImpl.builder()
                .withColumns(List.of(nationName, nationShortcode))
                .withRows(List.of(
                       RowMappingImpl.builder().withRowValues(List.of(
                            RowValueMappingImpl.builder().withColumn(nationName).withValue("USA").build(),
                            RowValueMappingImpl.builder().withColumn(nationShortcode).withValue("US").build())).build(),
                       RowMappingImpl.builder().withRowValues(List.of(
                               RowValueMappingImpl.builder().withColumn(nationName).withValue("Mexico").build(),
                               RowValueMappingImpl.builder().withColumn(nationShortcode).withValue("MX").build())).build(),
                       RowMappingImpl.builder().withRowValues(List.of(
                               RowValueMappingImpl.builder().withColumn(nationName).withValue("Canada").build(),
                               RowValueMappingImpl.builder().withColumn(nationShortcode).withValue("CA").build())).build()
                ))
                .build();
                PhysicalColumnMappingImpl id = PhysicalColumnMappingImpl.builder().withName("id").withDataType(ColumnDataType.NUMERIC).build();
                PhysicalColumnMappingImpl date = PhysicalColumnMappingImpl.builder().withName("date").withDataType(ColumnDataType.DATE).build();
                InlineTableMappingImpl tt = InlineTableMappingImpl.builder()
                .withName("inline_promo")
                .withColumns(List.of(id, date))
                .withRows(List.of(
                       RowMappingImpl.builder().withRowValues(List.of(
                            RowValueMappingImpl.builder().withColumn(id).withValue("1").build(),
                            RowValueMappingImpl.builder().withColumn(date).withValue("2008-04-29").build())).build(),
                       RowMappingImpl.builder().withRowValues(List.of(
                               RowValueMappingImpl.builder().withColumn(id).withValue("2").build(),
                               RowValueMappingImpl.builder().withColumn(date).withValue("2007-01-20").build())).build()
                ))
                .build();
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                JoinQueryMappingImpl j = JoinQueryMappingImpl.builder()
                		.withLeft(JoinedQueryElementMappingImpl.builder()
                				.withKey(FoodmartMappingSupplier.STORE_COUNTRY_COLUMN_IN_STORE)
                				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                				.build())
                		.withRight(JoinedQueryElementMappingImpl.builder()
                				.withKey(nationName)
                				.withQuery(InlineTableQueryMappingImpl.builder()
                                        .withAlias("nation")
                                        .withTable(t)
                				.build()).build())
                		.build();

                result.add(PhysicalCubeMappingImpl.builder()
                    .withName(cubeName)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Time")
                    		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                            .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                            .build(),
                        DimensionConnectorMappingImpl.builder()
                        	.withOverrideDimensionName("Alternative Promotion")
                        	.withForeignKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997)
                        	.withDimension(StandardDimensionMappingImpl.builder()
                            .withName("Alternative Promotion")
                            .withHierarchies(List.of(
                                HierarchyMappingImpl.builder()
                                    .withHasAll(true)
                                    .withPrimaryKey(id)
                                    .withQuery(InlineTableQueryMappingImpl.builder()
                                        .withAlias("inline_promo")
                                        .withTable(tt)
                                        .build()
                                    )
                                    .withLevels(List.of(
                                        LevelMappingImpl.builder()
                                            .withName("Alternative Promotion")
                                            .withColumn(id)
                                            .withNameColumn(date)
                                            .withUniqueMembers(true)
                                            .build()
                                    ))
                                    .build()
                            ))
                            .build()
                        ).build()
                    ))
                   .withMeasureGroups(List.of(
                		   MeasureGroupMappingImpl.builder()
                		   .withMeasures(List.of(
                               SumMeasureMappingImpl.builder()
                                   .withName("Unit Sales")
                                   .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                   .withFormatString("Standard")
                                   .withVisible(false)
                                   .build(),
                               SumMeasureMappingImpl.builder()
                                   .withName("Store Sales")
                                   .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_SALES_FACT_1997)
                                   .withFormatString("#,###.00")
                                   .build()
                			))
                		   .build()
                	))
                    .build());
                return result;

            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"" + cubeName + "\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "  <Dimension name=\"Alternative Promotion\" foreignKey=\"promotion_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"id\">\n"
            + "        <InlineTable alias=\"inline_promo\">\n"
            + "          <ColumnDefs>\n"
            + "            <ColumnDef name=\"id\" type=\"Numeric\"/>\n"
            + "            <ColumnDef name=\"date\" type=\"Date\"/>\n"
            + "          </ColumnDefs>\n"
            + "          <Rows>\n"
            + "            <Row>\n"
            + "              <Value column=\"id\">1</Value>\n"
            + "              <Value column=\"date\">2008-04-29</Value>\n"
            + "            </Row>\n"
            + "            <Row>\n"
            + "              <Value column=\"id\">2</Value>\n"
            + "              <Value column=\"date\">2007-01-20</Value>\n"
            + "            </Row>\n"
            + "          </Rows>\n"
            + "        </InlineTable>\n"
            + "      <Level name=\"Alternative Promotion\" column=\"id\" nameColumn=\"date\" uniqueMembers=\"true\"/> \n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\" visible=\"false\"/>\n"
            + "  <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"#,###.00\"/>\n"
            + "</Cube>",
            null,
            null,
            null,
            null);
        withSchema(context, schema);
        */

        // With grouping sets, mondrian will join fact table to the inline
        // dimension table, them sum to compute the 'all' value. That semi-joins
        // away too many fact table rows, and the 'all' value comes out too low
        // (zero, in fact). It causes a test exception, but is valid mondrian
        // behavior. (Behavior is unspecified if schema does not have
        // referential integrity.)
        if (context.getConfigValue(ConfigConstants.ENABLE_GROUPING_SETS, ConfigConstants.ENABLE_GROUPING_SETS_DEFAULT_VALUE, Boolean.class)) {
            return;
        }
        withSchema(context, TestInlineTableDateModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Alternative Promotion].Members} ON COLUMNS\n"
            + "from [" + cubeName + "] ",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Alternative Promotion].[Alternative Promotion].[All Alternative Promotions]}\n"
            + "{[Alternative Promotion].[Alternative Promotion].[2008-04-29]}\n"
            + "{[Alternative Promotion].[Alternative Promotion].[2007-01-20]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: \n"
            + "Row #0: \n");
    }
}
