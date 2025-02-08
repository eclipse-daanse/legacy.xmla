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
package mondrian.rolap;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencube.junit5.TestUtil.assertQueriesReturnSimilarResults;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertQueryThrows;
import static org.opencube.junit5.TestUtil.getDialect;
import static org.opencube.junit5.TestUtil.withSchema;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Position;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.DataType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.MeasureAggregatorType;
import org.eclipse.daanse.rolap.mapping.instance.rec.complex.foodmart.FoodmartMappingSupplier;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.CalculatedMemberMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.CalculatedMemberPropertyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MemberMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.VirtualCubeMappingImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestConfig;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.enums.DatabaseProduct;
import mondrian.olap.Property;
import mondrian.olap.SystemWideProperties;
import mondrian.test.SqlPattern;

/**
 * Unit tests for virtual cubes.
 *
 * @author remberson
 * @since Feb 14, 2003
 */
class VirtualCubeTest extends BatchTestCase {



    @BeforeEach
    public void beforeEach() {

    }

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }
    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-163">
     * MONDRIAN-163, "VirtualCube SegmentArrayQuerySpec.addMeasure assert"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNoTimeDimension(Context context) {
        class TestNoTimeDimensionModifier extends PojoMappingModifier {

            public TestNoTimeDimensionModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends CubeMapping> schemaCubes(CatalogMapping schema) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.schemaCubes(schema));
                result.add(VirtualCubeMappingImpl.builder()
                    .withName("Sales vs Warehouse")
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_PRODUCT))
                    		.withOverrideDimensionName("Product")
                            .build()
                    ))
                    .withReferencedMeasures(List.of(
                    	look(FoodmartMappingSupplier.MEASURE_WAREHOUSE_SALES),
                        look(FoodmartMappingSupplier.MEASURE_UNIT_SALES)
                    ))
                    .build());
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            null,
            "<VirtualCube name=\"Sales vs Warehouse\">\n"
            + "<VirtualCubeDimension name=\"Product\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"Warehouse\" name=\"[Measures].[Warehouse Sales]\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Unit Sales]\"/>\n"
            + "</VirtualCube>",
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestNoTimeDimensionModifier::new);
        checkXxx(context.getConnectionWithDefaultRole());
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCalculatedMeasureAsDefaultMeasureInVC(Context context) {
        class TestCalculatedMeasureAsDefaultMeasureInVCModifier extends PojoMappingModifier {

            public TestCalculatedMeasureAsDefaultMeasureInVCModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends CubeMapping> schemaCubes(CatalogMapping schema) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.schemaCubes(schema));
                result.add(VirtualCubeMappingImpl.builder()
                    .withName("Sales vs Warehouse")
                    .withDefaultMeasure((MemberMappingImpl) look(FoodmartMappingSupplier.CALCULATED_MEMBER_PROFIT))
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_PRODUCT))
                    		.withOverrideDimensionName("Product")
                            .build()
                    ))
                    .withReferencedMeasures(List.of(
                    		look(FoodmartMappingSupplier.MEASURE_UNIT_SALES)
                    ))
                    .withReferencedCalculatedMembers(List.of(
                    		look(FoodmartMappingSupplier.CALCULATED_MEMBER_PROFIT)
                    ))
                    .build());
                return result;
            }

        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            null,
            "<VirtualCube name=\"Sales vs Warehouse\" defaultMeasure=\"Profit\">\n"
            + "<VirtualCubeDimension name=\"Product\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"Warehouse\" "
            + "name=\"[Measures].[Warehouse Sales]\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"Sales\" "
            + "name=\"[Measures].[Unit Sales]\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"Sales\" "
            + "name=\"[Measures].[Profit]\"/>\n"
            + "</VirtualCube>",
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestCalculatedMeasureAsDefaultMeasureInVCModifier::new);
        String query1 = "select from [Sales vs Warehouse]";
        String query2 =
            "select from [Sales vs Warehouse] where measures.profit";
        assertQueriesReturnSimilarResults(context.getConnectionWithDefaultRole(), query1, query2);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDefaultMeasureInVCForIncorrectMeasureName(Context context) {
        class TestDefaultMeasureInVCForIncorrectMeasureNameModifier extends PojoMappingModifier {

            public TestDefaultMeasureInVCForIncorrectMeasureNameModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends CubeMapping> schemaCubes(CatalogMapping schema) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.schemaCubes(schema));
                result.add(VirtualCubeMappingImpl.builder()
                    .withName("Sales vs Warehouse")
                    //.withDefaultMeasure("Profit Error")
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Product")
                            .build()
                    ))
                    .withReferencedMeasures(List.of(
                    		look(FoodmartMappingSupplier.MEASURE_WAREHOUSE_SALES),
                    		look(FoodmartMappingSupplier.MEASURE_UNIT_SALES)
                    ))
                    .withReferencedCalculatedMembers(List.of(
                    		look(FoodmartMappingSupplier.CALCULATED_MEMBER_PROFIT)
                    ))
                    .build());
                return result;
            }

        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            null,
            "<VirtualCube name=\"Sales vs Warehouse\" defaultMeasure=\"Profit Error\">\n"
            + "<VirtualCubeDimension name=\"Product\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"Warehouse\" "
            + "name=\"[Measures].[Warehouse Sales]\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"Sales\" "
            + "name=\"[Measures].[Unit Sales]\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"Sales\" "
            + "name=\"[Measures].[Profit]\"/>\n"
            + "</VirtualCube>",
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestDefaultMeasureInVCForIncorrectMeasureNameModifier::new);
        String query1 = "select from [Sales vs Warehouse]";
        String query2 =
            "select from [Sales vs Warehouse] "
            + "where measures.[Warehouse Sales]";
        assertQueriesReturnSimilarResults(context.getConnectionWithDefaultRole(), query1, query2);
    }

    @Disabled // cube name not a string. we use reference to cube. we not able to set "Bad cube". this test will delete in future
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testVirtualCubeMeasureInvalidCubeName(Context context) {
        class TestVirtualCubeMeasureInvalidCubeNameModifier extends PojoMappingModifier {

            public TestVirtualCubeMeasureInvalidCubeNameModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends CubeMapping> schemaCubes(CatalogMapping schema) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.schemaCubes(schema));
                result.add(VirtualCubeMappingImpl.builder()
                    .withName("Sales vs Warehouse")
                    //.withDefaultMeasure("Profit Error")
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Product")
                            .build()
                    ))
                    .withReferencedMeasures(List.of(
                    	look(FoodmartMappingSupplier.MEASURE_WAREHOUSE_SALES),
                    	look(FoodmartMappingSupplier.MEASURE_UNIT_SALES) //.cubeName("Bad cube")
                    ))
                    .build());
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            null,
            "<VirtualCube name=\"Sales vs Warehouse\">\n"
            + "<VirtualCubeDimension name=\"Product\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"Warehouse\" "
            + "name=\"[Measures].[Warehouse Sales]\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"Bad cube\" "
            + "name=\"[Measures].[Unit Sales]\"/>\n"
            + "</VirtualCube>",
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestVirtualCubeMeasureInvalidCubeNameModifier::new);
        assertQueryThrows(context,
            "select from [Sales vs Warehouse]",
            "Cube 'Bad cube' not found");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDefaultMeasureInVCForCaseSensitivity(Context context) {
        class TestDefaultMeasureInVCForCaseSensitivityModifier extends PojoMappingModifier {

            public TestDefaultMeasureInVCForCaseSensitivityModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends CubeMapping> schemaCubes(CatalogMapping schema) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.schemaCubes(schema));
                result.add(VirtualCubeMappingImpl.builder()
                    .withName("Sales vs Warehouse")
                    .withDefaultMeasure((MemberMappingImpl) look(FoodmartMappingSupplier.CALCULATED_MEMBER_PROFIT))
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_PRODUCT))
                    		.withOverrideDimensionName("Product")
                            .build()
                    ))
                    .withReferencedMeasures(List.of(
                    	look(FoodmartMappingSupplier.MEASURE_WAREHOUSE_SALES),
                    	look(FoodmartMappingSupplier.MEASURE_UNIT_SALES)
                    ))
                    .withReferencedCalculatedMembers(List.of(
                    		look(FoodmartMappingSupplier.CALCULATED_MEMBER_PROFIT)
                    ))
                    .build());
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            null,
            "<VirtualCube name=\"Sales vs Warehouse\" defaultMeasure=\"PROFIT\">\n"
            + "<VirtualCubeDimension name=\"Product\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"Warehouse\" "
            + "name=\"[Measures].[Warehouse Sales]\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"Sales\" "
            + "name=\"[Measures].[Unit Sales]\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"Sales\" "
            + "name=\"[Measures].[Profit]\"/>\n"
            + "</VirtualCube>",
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestDefaultMeasureInVCForCaseSensitivityModifier::new);
        String queryWithoutFilter = "select from [Sales vs Warehouse]";
        String queryWithFirstMeasure =
            "select from [Sales vs Warehouse] "
            + "where measures.[Warehouse Sales]";
        String queryWithDefaultMeasureFilter =
            "select from [Sales vs Warehouse] "
            + "where measures.[Profit]";

        Connection connection = context.getConnectionWithDefaultRole();
        if (SystemWideProperties.instance().CaseSensitive) {
            assertQueriesReturnSimilarResults(connection,
                queryWithoutFilter, queryWithFirstMeasure);
        } else {
            assertQueriesReturnSimilarResults(connection,
                queryWithoutFilter, queryWithDefaultMeasureFilter);
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testWithTimeDimension(Context context) {
        class TestWithTimeDimensionModifier extends PojoMappingModifier {

            public TestWithTimeDimensionModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends CubeMapping> schemaCubes(CatalogMapping schema) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.schemaCubes(schema));
                result.add(VirtualCubeMappingImpl.builder()
                    .withName("Sales vs Warehouse")
                    .withDimensionConnectors(List.of(
                        DimensionConnectorMappingImpl.builder()
                        	.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                    		.withOverrideDimensionName("Time")
                            .build(),
                    	DimensionConnectorMappingImpl.builder()
                    		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_PRODUCT))
                    		.withOverrideDimensionName("Product")
                            .build()
                    ))
                    .withReferencedMeasures(List.of(
                    	look(FoodmartMappingSupplier.MEASURE_WAREHOUSE_SALES),
                    	look(FoodmartMappingSupplier.MEASURE_UNIT_SALES)
                    ))
                    .withReferencedCalculatedMembers(List.of(
                    		look(FoodmartMappingSupplier.CALCULATED_MEMBER_PROFIT)
                    ))
                    .build());
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            null,
            "<VirtualCube name=\"Sales vs Warehouse\">\n"
            + "<VirtualCubeDimension name=\"Time\"/>\n"
            + "<VirtualCubeDimension name=\"Product\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"Warehouse\" name=\"[Measures].[Warehouse Sales]\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Unit Sales]\"/>\n"
            + "</VirtualCube>",
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestWithTimeDimensionModifier::new);
        checkXxx(context.getConnectionWithDefaultRole());
    }


    private void checkXxx(Connection connection) {
        // I do not know/believe that the return values are correct.
        assertQueryReturns(connection,
            "select\n"
            + "{ [Measures].[Warehouse Sales], [Measures].[Unit Sales] }\n"
            + "ON COLUMNS,\n"
            + "{[Product].[All Products]}\n"
            + "ON ROWS\n"
            + "from [Sales vs Warehouse]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Warehouse Sales]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Product].[All Products]}\n"
            + "Row #0: 196,770.888\n"
            + "Row #0: 266,773\n");
    }

    /**
     * Query a virtual cube that contains a non-conforming dimension that
     * does not have ALL as its default member.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonDefaultAllMember(Context context) {
        // Create a virtual cube with a non-conforming dimension (Warehouse)
        // that does not have ALL as its default member.
        createContextWithNonDefaultAllMember(context);

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Warehouse].defaultMember} on columns, "
            + "{[Measures].[Warehouse Cost]} on rows from [Warehouse (Default USA)]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Warehouse].[USA]}\n"
            + "Axis #2:\n"
            + "{[Measures].[Warehouse Cost]}\n"
            + "Row #0: 89,043.253\n");

        // There is a value for [USA] -- because it is the default member and
        // the hierarchy has no all member -- but not for [USA].[CA].
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Warehouse].defaultMember, [Warehouse].[USA].[CA]} on columns, "
            + "{[Measures].[Warehouse Cost], [Measures].[Sales Count]} on rows "
            + "from [Warehouse (Default USA) and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Warehouse].[USA]}\n"
            + "{[Warehouse].[USA].[CA]}\n"
            + "Axis #2:\n"
            + "{[Measures].[Warehouse Cost]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "Row #0: 89,043.253\n"
            + "Row #0: 25,789.087\n"
            + "Row #1: 86,837\n"
            + "Row #1: \n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonDefaultAllMember2(Context context) {
        createContextWithNonDefaultAllMember(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select { measures.[unit sales] } on 0 \n"
            + "from [warehouse (Default USA) and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Row #0: 266,773\n");
    }

    /**
     * Creates a TestContext containing a cube
     * "Warehouse (Default USA) and Sales".
     *
     * @return test context with a cube where the default member in the
     *     Warehouse dimension is USA
     */
    private void createContextWithNonDefaultAllMember(Context context) {
        class CreateContextWithNonDefaultAllMemberModifier extends PojoMappingModifier {

            public CreateContextWithNonDefaultAllMemberModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends CubeMapping> schemaCubes(CatalogMapping schema) {
            	PhysicalCubeMappingImpl warehouseDefaultUSA;
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.schemaCubes(schema));
                result.add(
                  warehouseDefaultUSA = PhysicalCubeMappingImpl.builder()
                    .withName("Warehouse (Default USA)")
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.INVENTORY_FACKT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(
                        DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Time")
                    		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                    		.withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                            .build(),
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Product")
                    		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_PRODUCT))
                    		.withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                            .build(),
                        DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Store")
                    		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_STORE_WITH_QUERY_STORE))
                    		.withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                            .build(),
                        DimensionConnectorMappingImpl.builder()
                        	.withOverrideDimensionName("Warehouse")
                            .withForeignKey(FoodmartMappingSupplier.WAREHOUSE_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("Warehouse")
                            	.withHierarchies(List.of(
                                HierarchyMappingImpl.builder()
                                    .withHasAll(false)
                                    .withDefaultMember("[USA]")
                                    .withPrimaryKey(FoodmartMappingSupplier.WAREHOUSE_ID_COLUMN_IN_WAREHOUSE)
                                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.WAREHOUSE_TABLE).build())
                                    .withLevels(List.of(
                                        LevelMappingImpl.builder()
                                            .withName("Country")
                                            .withColumn(FoodmartMappingSupplier.WAREHOUSE_COUNTRY_COLUMN_IN_WAREHOUSE)
                                            .withUniqueMembers(true)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("State Province")
                                            .withColumn(FoodmartMappingSupplier.WAREHOUSE_STATE_PROVINCE_COLUMN_IN_WAREHOUSE)
                                            .withUniqueMembers(true)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("City")
                                            .withColumn(FoodmartMappingSupplier.WAREHOUSE_CITY_COLUMN_IN_WAREHOUSE)
                                            .withUniqueMembers(false)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("Warehouse Name")
                                            .withColumn(FoodmartMappingSupplier.WAREHOUSE_NAME_COLUMN_IN_WAREHOUSE)
                                            .withUniqueMembers(true)
                                            .build()

                                    ))
                                    .build()
                            )).build())
                            .build()
                    ))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                        MeasureMappingImpl.builder()
                            .withName("Warehouse Cost")
                            .withColumn(FoodmartMappingSupplier.WAREHOUSE_COST_COLUMN_IN_INVENTORY_FACKT_1997)
                            .withAggregatorType(MeasureAggregatorType.SUM)
                            .build(),
                        MeasureMappingImpl.builder()
                            .withName("Warehouse Sales")
                            .withColumn(FoodmartMappingSupplier.WAREHOUSE_SALES_COLUMN_IN_INVENTORY_FACKT_1997)
                            .withAggregatorType(MeasureAggregatorType.SUM)
                            .build()
                    )).build()))
                    .build());
                result.add(
                	VirtualCubeMappingImpl.builder()
                        .withName("Warehouse (Default USA) and Sales")
                        .withDimensionConnectors(List.of(
                            	DimensionConnectorMappingImpl.builder()
                            		.withOverrideDimensionName("Product")
                                    .build(),
                                DimensionConnectorMappingImpl.builder()
                            		.withOverrideDimensionName("Store")
                                    .build(),
                                DimensionConnectorMappingImpl.builder()
                                	.withOverrideDimensionName("Time")
                                    .build(),
                                DimensionConnectorMappingImpl.builder()
                                	.withOverrideDimensionName("Warehouse")
                                	.withPhysicalCube(warehouseDefaultUSA)
                                    .build()
                        ))
                        .withReferencedMeasures(List.of(
                        	look(FoodmartMappingSupplier.MEASURE_SALES_COUNT),
                        	look(FoodmartMappingSupplier.MEASURE_STORE_COST),
                        	look(FoodmartMappingSupplier.MEASURE_STORE_SALES),
                        	look(FoodmartMappingSupplier.MEASURE_UNIT_SALES),
                        	look(FoodmartMappingSupplier.MEASURE_STORE_INVOICE),
                        	look(FoodmartMappingSupplier.MEASURE_SUPPLY_TIME),
                        	look(FoodmartMappingSupplier.MEASURE_UNITS_ORDERED),
                        	look(FoodmartMappingSupplier.MEASURE_UNITS_SHIPPED),
                        	look(FoodmartMappingSupplier.MEASURE_WAREHOUSE_COST),
                        	look(FoodmartMappingSupplier.MEASURE_WAREHOUSE_SALES)
                        ))
                        .build()
                );
                return result;
            }
        }

        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,

            // Warehouse cube where the default member in the Warehouse
            // dimension is USA.
            "<Cube name=\"Warehouse (Default USA)\">\n"
            + "  <Table name=\"inventory_fact_1997\"/>\n"
            + "\n"
            + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "  <DimensionUsage name=\"Product\" source=\"Product\" foreignKey=\"product_id\"/>\n"
            + "  <DimensionUsage name=\"Store\" source=\"Store\" foreignKey=\"store_id\"/>\n"
            + "  <Dimension name=\"Warehouse\" foreignKey=\"warehouse_id\">\n"
            + "    <Hierarchy hasAll=\"false\" defaultMember=\"[USA]\" primaryKey=\"warehouse_id\"> \n"
            + "      <Table name=\"warehouse\"/>\n"
            + "      <Level name=\"Country\" column=\"warehouse_country\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"State Province\" column=\"warehouse_state_province\"\n"
            + "          uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"City\" column=\"warehouse_city\" uniqueMembers=\"false\"/>\n"
            + "      <Level name=\"Warehouse Name\" column=\"warehouse_name\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Measure name=\"Warehouse Cost\" column=\"warehouse_cost\" aggregator=\"sum\"/>\n"
            + "  <Measure name=\"Warehouse Sales\" column=\"warehouse_sales\" aggregator=\"sum\"/>\n"
            + "</Cube>",

            // Virtual cube based on [Warehouse (Default USA)]
            "<VirtualCube name=\"Warehouse (Default USA) and Sales\">\n"
            + "  <VirtualCubeDimension name=\"Product\"/>\n"
            + "  <VirtualCubeDimension name=\"Store\"/>\n"
            + "  <VirtualCubeDimension name=\"Time\"/>\n"
            + "  <VirtualCubeDimension cubeName=\"Warehouse (Default USA)\" name=\"Warehouse\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Sales 2\" name=\"[Measures].[Sales Count]\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Sales 2\" name=\"[Measures].[Store Cost]\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Sales 2\" name=\"[Measures].[Store Sales]\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Sales 2\" name=\"[Measures].[Unit Sales]\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Warehouse\" name=\"[Measures].[Store Invoice]\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Warehouse\" name=\"[Measures].[Supply Time]\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Warehouse\" name=\"[Measures].[Units Ordered]\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Warehouse\" name=\"[Measures].[Units Shipped]\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Warehouse\" name=\"[Measures].[Warehouse Cost]\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Warehouse\" name=\"[Measures].[Warehouse Sales]\"/>\n"
            + "</VirtualCube>",
            null, null, null);
            withSchema(context, schema);
         */
        withSchema(context, CreateContextWithNonDefaultAllMemberModifier::new);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMemberVisibility(Context context) {
        class TestMemberVisibilityModifier extends PojoMappingModifier {

            public TestMemberVisibilityModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends CubeMapping> schemaCubes(CatalogMapping schema) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.schemaCubes(schema));
                result.add(VirtualCubeMappingImpl.builder()
                    .withName("Warehouse and Sales Member Visibility")
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withPhysicalCube((PhysicalCubeMappingImpl) look(FoodmartMappingSupplier.CUBE_SALES))
                    		.withOverrideDimensionName("Customers")
                            .build(),
                        DimensionConnectorMappingImpl.builder()
                        	.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                        	.withOverrideDimensionName("Time")
                            .build()
                    ))
                    .withReferencedMeasures(List.of(
                    		look(FoodmartMappingSupplier.MEASURE_SALES_COUNT),
                    		look(FoodmartMappingSupplier.MEASURE_STORE_COST),
                    		look(FoodmartMappingSupplier.MEASURE_STORE_SALES),
                    		look(FoodmartMappingSupplier.MEASURE_UNITS_SHIPPED)
                    ))
                    .withReferencedCalculatedMembers(List.of(
                    		look(FoodmartMappingSupplier.CALCULATED_MEMBER_PROFIT_LAST_PERIOD),
                    		look(FoodmartMappingSupplier.CALCULATED_MEMBER_AVERAGE_WAREHOUSE_SALE)
                    ))
                    .withCalculatedMembers(List.of(
                        CalculatedMemberMappingImpl.builder()
                            .withName("Profit")
                            //.withDimension("Measures")
                            .withVisible(false)
                            .withFormula("[Measures].[Store Sales] - [Measures].[Store Cost]")
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
            null,
            "<VirtualCube name=\"Warehouse and Sales Member Visibility\">\n"
            + "  <VirtualCubeDimension cubeName=\"Sales\" name=\"Customers\"/>\n"
            + "  <VirtualCubeDimension name=\"Time\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Sales Count]\" visible=\"true\" />\n"
            + "  <VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Store Cost]\" visible=\"false\" />\n"
            + "  <VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Store Sales]\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Profit last Period]\" visible=\"true\" />\n"
            + "  <VirtualCubeMeasure cubeName=\"Warehouse\" name=\"[Measures].[Units Shipped]\" visible=\"false\" />\n"
            + "  <VirtualCubeMeasure cubeName=\"Warehouse\" name=\"[Measures].[Average Warehouse Sale]\" visible=\"false\" />\n"
            + "  <CalculatedMember name=\"Profit\" dimension=\"Measures\" visible=\"false\" >\n"
            + "    <Formula>[Measures].[Store Sales] - [Measures].[Store Cost]</Formula>\n"
            + "  </CalculatedMember>\n"
            + "</VirtualCube>",
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestMemberVisibilityModifier::new);
        Result result = executeQuery(
            "select {[Measures].[Sales Count],\n"
            + " [Measures].[Store Cost],\n"
            + " [Measures].[Store Sales],\n"
            + " [Measures].[Units Shipped],\n"
            + " [Measures].[Profit],\n"
            + " [Measures].[Profit last Period],\n"
            + " [Measures].[Average Warehouse Sale]} on columns\n"
            + "from [Warehouse and Sales Member Visibility]", context.getConnectionWithDefaultRole());
        assertVisibility(result, 0, "Sales Count", true); // explicitly visible
        assertVisibility(
            result, 1, "Store Cost", true); // explicitly invisible
        assertVisibility(result, 2, "Store Sales", true); // visible by default
        assertVisibility(
            result, 3, "Units Shipped", true); // explicitly visible
        assertVisibility(result, 4, "Profit", false); // explicitly visible
        assertVisibility(result, 5, "Profit last Period", true); // explicitly visible
        assertVisibility(result, 6, "Average Warehouse Sale", true); // explicitly visible

        // check that visibilities in the base cubes are still the same
        result = executeQuery(
          "select {[Measures].[Profit last Period]} on columns from [Sales]", context.getConnectionWithDefaultRole());
        assertVisibility(result, 0, "Profit last Period", true); // explicitly visible in base cube

        result = executeQuery(
          "select {[Measures].[Units Shipped],\n"
            + " [Measures].[Average Warehouse Sale]} on columns\n"
            + " from [Warehouse]", context.getConnectionWithDefaultRole());
        assertVisibility(result, 0, "Units Shipped", true); // implicitly visible in base cube
        assertVisibility(result, 1, "Average Warehouse Sale", true); // implicitly visible in base cube
    }

    private void assertVisibility(
        Result result,
        int ordinal,
        String expectedName,
        boolean expectedVisibility)
    {
        List<Position> columnPositions = result.getAxes()[0].getPositions();
        Member measure = columnPositions.get(ordinal).get(0);
        assertEquals(expectedName, measure.getName());
        assertEquals(
            expectedVisibility,
            measure.getPropertyValue(Property.VISIBLE.name));
    }

    /**
     * Test an expression for the format_string of a calculated member that
     * evaluates calculated members based on a virtual cube.  One cube has cache
     * turned on, the other cache turned off.
     *
     * <p>Since evaluation of the format_string used to happen after the
     * aggregate cache was cleared, this used to fail, this should be solved
     * with the caching of the format string.
     *
     * <p>Without caching of format string, the query returns green for all
     * styles.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFormatStringExpressionCubeNoCache(Context context) {
        class TestFormatStringExpressionCubeNoCacheModifier extends PojoMappingModifier {

            public TestFormatStringExpressionCubeNoCacheModifier(CatalogMapping catalog) {
                super(catalog);
            }

            protected List<? extends CubeMapping> schemaCubes(CatalogMapping schema) {
            	MeasureMappingImpl unitsShipped;
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.schemaCubes(schema));
                result.add(PhysicalCubeMappingImpl.builder()
                    .withName("Warehouse No Cache")
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.INVENTORY_FACKT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Time")
                    		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                            .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                            .build(),
                        DimensionConnectorMappingImpl.builder()
                            .withOverrideDimensionName("Store")
                            .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_STORE_WITH_QUERY_STORE))
                            .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                            .build()
                    ))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                    	unitsShipped = MeasureMappingImpl.builder()
                            .withName("Units Shipped")
                            .withColumn(FoodmartMappingSupplier.UNITS_SHIPPED_COLUMN_IN_INVENTORY_FACKT_1997)
                            .withAggregatorType(MeasureAggregatorType.SUM)
                            .withFormatString("#.0")
                            .build()
                    )).build()))
                    .build());

                result.add(VirtualCubeMappingImpl.builder()
                        .withName("Warehouse and Sales Format Expression Cube No Cache")
                        .withDimensionConnectors(List.of(
                        	DimensionConnectorMappingImpl.builder()
                        		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_STORE_WITH_QUERY_STORE))
                        		.withOverrideDimensionName("Store")
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                            	.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                            	.withOverrideDimensionName("Time")
                                .build()
                        ))
                        .withReferencedMeasures(List.of(
                        	look(FoodmartMappingSupplier.MEASURE_STORE_COST),
                        	look(FoodmartMappingSupplier.MEASURE_STORE_SALES),
                        	unitsShipped
                        ))
                        .withCalculatedMembers(List.of(
                            CalculatedMemberMappingImpl.builder()
                                .withName("Profit")
                                //.withDimension("Measures")
                                .withFormula("[Measures].[Store Sales] - [Measures].[Store Cost]")
                                .build(),
                            CalculatedMemberMappingImpl.builder()
                                .withName("Profit Per Unit Shipped")
                                //.withDimension("Measures")
                                .withFormula("[Measures].[Profit] / [Measures].[Units Shipped]")
                                .withCalculatedMemberProperties(List.of(
                                	CalculatedMemberPropertyMappingImpl.builder()
                                        .withName("FORMAT_STRING")
                                        .withExpression("IIf(([Measures].[Profit Per Unit Shipped] > 2.0), '|0.#|style=green', '|0.#|style=red')")
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
            null,
            "<Cube name=\"Warehouse No Cache\" cache=\"false\">\n"
            + "  <Table name=\"inventory_fact_1997\"/>\n"
            + "\n"
            + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "  <DimensionUsage name=\"Store\" source=\"Store\" foreignKey=\"store_id\"/>\n"
            + "  <Measure name=\"Units Shipped\" column=\"units_shipped\" aggregator=\"sum\" formatString=\"#.0\"/>\n"
            + "</Cube>\n"
            + "<VirtualCube name=\"Warehouse and Sales Format Expression Cube No Cache\">\n"
            + "  <VirtualCubeDimension name=\"Store\"/>\n"
            + "  <VirtualCubeDimension name=\"Time\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Store Cost]\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Store Sales]\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Warehouse No Cache\" name=\"[Measures].[Units Shipped]\"/>\n"
            + "  <CalculatedMember name=\"Profit\" dimension=\"Measures\">\n"
            + "    <Formula>[Measures].[Store Sales] - [Measures].[Store Cost]</Formula>\n"
            + "  </CalculatedMember>\n"
            + "  <CalculatedMember name=\"Profit Per Unit Shipped\" dimension=\"Measures\">\n"
            + "    <Formula>[Measures].[Profit] / [Measures].[Units Shipped]</Formula>\n"
            + "    <CalculatedMemberProperty name=\"FORMAT_STRING\" expression=\"IIf(([Measures].[Profit Per Unit Shipped] > 2.0), '|0.#|style=green', '|0.#|style=red')\"/>\n"
            + "  </CalculatedMember>\n"
            + "</VirtualCube>",
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestFormatStringExpressionCubeNoCacheModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures].[Profit Per Unit Shipped]} ON COLUMNS, "
            + "{[Store].[All Stores].[USA].[CA], [Store].[All Stores].[USA].[OR], [Store].[All Stores].[USA].[WA]} ON ROWS "
            + "from [Warehouse and Sales Format Expression Cube No Cache] "
            + "where [Time].[1997]",
            "Axis #0:\n"
            + "{[Time].[1997]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Profit Per Unit Shipped]}\n"
            + "Axis #2:\n"
            + "{[Store].[USA].[CA]}\n"
            + "{[Store].[USA].[OR]}\n"
            + "{[Store].[USA].[WA]}\n"
            + "Row #0: |1.6|style=red\n"
            + "Row #1: |2.1|style=green\n"
            + "Row #2: |1.5|style=red\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCalculatedMeasure(Context context) {
        // calculated measures reference measures defined in the base cube
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select\n"
            + "{[Measures].[Profit Growth], "
            + "[Measures].[Profit], "
            + "[Measures].[Average Warehouse Sale] }\n"
            + "ON COLUMNS\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Profit Growth]}\n"
            + "{[Measures].[Profit]}\n"
            + "{[Measures].[Average Warehouse Sale]}\n"
            + "Row #0: 0.0%\n"
            + "Row #0: $339,610.90\n"
            + "Row #0: $2.21\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLostData(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Time].[Time].Members} on columns,\n"
            + " {[Product].Children} on rows\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Time].[1997]}\n"
            + "{[Time].[1997].[Q1]}\n"
            + "{[Time].[1997].[Q1].[1]}\n"
            + "{[Time].[1997].[Q1].[2]}\n"
            + "{[Time].[1997].[Q1].[3]}\n"
            + "{[Time].[1997].[Q2]}\n"
            + "{[Time].[1997].[Q2].[4]}\n"
            + "{[Time].[1997].[Q2].[5]}\n"
            + "{[Time].[1997].[Q2].[6]}\n"
            + "{[Time].[1997].[Q3]}\n"
            + "{[Time].[1997].[Q3].[7]}\n"
            + "{[Time].[1997].[Q3].[8]}\n"
            + "{[Time].[1997].[Q3].[9]}\n"
            + "{[Time].[1997].[Q4]}\n"
            + "{[Time].[1997].[Q4].[10]}\n"
            + "{[Time].[1997].[Q4].[11]}\n"
            + "{[Time].[1997].[Q4].[12]}\n"
            + "{[Time].[1998]}\n"
            + "{[Time].[1998].[Q1]}\n"
            + "{[Time].[1998].[Q1].[1]}\n"
            + "{[Time].[1998].[Q1].[2]}\n"
            + "{[Time].[1998].[Q1].[3]}\n"
            + "{[Time].[1998].[Q2]}\n"
            + "{[Time].[1998].[Q2].[4]}\n"
            + "{[Time].[1998].[Q2].[5]}\n"
            + "{[Time].[1998].[Q2].[6]}\n"
            + "{[Time].[1998].[Q3]}\n"
            + "{[Time].[1998].[Q3].[7]}\n"
            + "{[Time].[1998].[Q3].[8]}\n"
            + "{[Time].[1998].[Q3].[9]}\n"
            + "{[Time].[1998].[Q4]}\n"
            + "{[Time].[1998].[Q4].[10]}\n"
            + "{[Time].[1998].[Q4].[11]}\n"
            + "{[Time].[1998].[Q4].[12]}\n"
            + "Axis #2:\n"
            + "{[Product].[Drink]}\n"
            + "{[Product].[Food]}\n"
            + "{[Product].[Non-Consumable]}\n"
            + "Row #0: 24,597\n"
            + "Row #0: 5,976\n"
            + "Row #0: 1,910\n"
            + "Row #0: 1,951\n"
            + "Row #0: 2,115\n"
            + "Row #0: 5,895\n"
            + "Row #0: 1,948\n"
            + "Row #0: 2,039\n"
            + "Row #0: 1,908\n"
            + "Row #0: 6,065\n"
            + "Row #0: 2,205\n"
            + "Row #0: 1,921\n"
            + "Row #0: 1,939\n"
            + "Row #0: 6,661\n"
            + "Row #0: 1,898\n"
            + "Row #0: 2,344\n"
            + "Row #0: 2,419\n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #1: 191,940\n"
            + "Row #1: 47,809\n"
            + "Row #1: 15,604\n"
            + "Row #1: 15,142\n"
            + "Row #1: 17,063\n"
            + "Row #1: 44,825\n"
            + "Row #1: 14,393\n"
            + "Row #1: 15,055\n"
            + "Row #1: 15,377\n"
            + "Row #1: 47,440\n"
            + "Row #1: 17,036\n"
            + "Row #1: 15,741\n"
            + "Row #1: 14,663\n"
            + "Row #1: 51,866\n"
            + "Row #1: 14,232\n"
            + "Row #1: 18,278\n"
            + "Row #1: 19,356\n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #2: 50,236\n"
            + "Row #2: 12,506\n"
            + "Row #2: 4,114\n"
            + "Row #2: 3,864\n"
            + "Row #2: 4,528\n"
            + "Row #2: 11,890\n"
            + "Row #2: 3,838\n"
            + "Row #2: 3,987\n"
            + "Row #2: 4,065\n"
            + "Row #2: 12,343\n"
            + "Row #2: 4,522\n"
            + "Row #2: 4,035\n"
            + "Row #2: 3,786\n"
            + "Row #2: 13,497\n"
            + "Row #2: 3,828\n"
            + "Row #2: 4,648\n"
            + "Row #2: 5,021\n"
            + "Row #2: \n"
            + "Row #2: \n"
            + "Row #2: \n"
            + "Row #2: \n"
            + "Row #2: \n"
            + "Row #2: \n"
            + "Row #2: \n"
            + "Row #2: \n"
            + "Row #2: \n"
            + "Row #2: \n"
            + "Row #2: \n"
            + "Row #2: \n"
            + "Row #2: \n"
            + "Row #2: \n"
            + "Row #2: \n"
            + "Row #2: \n"
            + "Row #2: \n");
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select\n"
            + " {[Measures].[Unit Sales]} on 0,\n"
            + " {[Product].Children} on 1\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Product].[Drink]}\n"
            + "{[Product].[Food]}\n"
            + "{[Product].[Non-Consumable]}\n"
            + "Row #0: 24,597\n"
            + "Row #1: 191,940\n"
            + "Row #2: 50,236\n");
    }

    /**
     * Tests a calc measure which combines a measures from the Sales cube with a
     * measures from the Warehouse cube.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCalculatedMeasureAcrossCubes(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Measures].[Shipped per Ordered] as ' [Measures].[Units Shipped] / [Measures].[Unit Sales] ', format_string='#.00%'\n"
            + " member [Measures].[Profit per Unit Shipped] as ' [Measures].[Profit] / [Measures].[Units Shipped] '\n"
            + "select\n"
            + " {[Measures].[Unit Sales], \n"
            + "  [Measures].[Units Shipped],\n"
            + "  [Measures].[Shipped per Ordered],\n"
            + "  [Measures].[Profit per Unit Shipped]} on 0,\n"
            + " NON EMPTY Crossjoin([Product].Children, [Time].[1997].Children) on 1\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Units Shipped]}\n"
            + "{[Measures].[Shipped per Ordered]}\n"
            + "{[Measures].[Profit per Unit Shipped]}\n"
            + "Axis #2:\n"
            + "{[Product].[Drink], [Time].[1997].[Q1]}\n"
            + "{[Product].[Drink], [Time].[1997].[Q2]}\n"
            + "{[Product].[Drink], [Time].[1997].[Q3]}\n"
            + "{[Product].[Drink], [Time].[1997].[Q4]}\n"
            + "{[Product].[Food], [Time].[1997].[Q1]}\n"
            + "{[Product].[Food], [Time].[1997].[Q2]}\n"
            + "{[Product].[Food], [Time].[1997].[Q3]}\n"
            + "{[Product].[Food], [Time].[1997].[Q4]}\n"
            + "{[Product].[Non-Consumable], [Time].[1997].[Q1]}\n"
            + "{[Product].[Non-Consumable], [Time].[1997].[Q2]}\n"
            + "{[Product].[Non-Consumable], [Time].[1997].[Q3]}\n"
            + "{[Product].[Non-Consumable], [Time].[1997].[Q4]}\n"
            + "Row #0: 5,976\n"
            + "Row #0: 4637.0\n"
            + "Row #0: 77.59%\n"
            + "Row #0: $1.50\n"
            + "Row #1: 5,895\n"
            + "Row #1: 4501.0\n"
            + "Row #1: 76.35%\n"
            + "Row #1: $1.60\n"
            + "Row #2: 6,065\n"
            + "Row #2: 6258.0\n"
            + "Row #2: 103.18%\n"
            + "Row #2: $1.15\n"
            + "Row #3: 6,661\n"
            + "Row #3: 5802.0\n"
            + "Row #3: 87.10%\n"
            + "Row #3: $1.38\n"
            + "Row #4: 47,809\n"
            + "Row #4: 37153.0\n"
            + "Row #4: 77.71%\n"
            + "Row #4: $1.64\n"
            + "Row #5: 44,825\n"
            + "Row #5: 35459.0\n"
            + "Row #5: 79.11%\n"
            + "Row #5: $1.62\n"
            + "Row #6: 47,440\n"
            + "Row #6: 41545.0\n"
            + "Row #6: 87.57%\n"
            + "Row #6: $1.47\n"
            + "Row #7: 51,866\n"
            + "Row #7: 34706.0\n"
            + "Row #7: 66.91%\n"
            + "Row #7: $1.91\n"
            + "Row #8: 12,506\n"
            + "Row #8: 9161.0\n"
            + "Row #8: 73.25%\n"
            + "Row #8: $1.76\n"
            + "Row #9: 11,890\n"
            + "Row #9: 9227.0\n"
            + "Row #9: 77.60%\n"
            + "Row #9: $1.65\n"
            + "Row #10: 12,343\n"
            + "Row #10: 9986.0\n"
            + "Row #10: 80.90%\n"
            + "Row #10: $1.59\n"
            + "Row #11: 13,497\n"
            + "Row #11: 9291.0\n"
            + "Row #11: 68.84%\n"
            + "Row #11: $1.86\n");
    }

    /**
     * Tests a calc member defined in the cube.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCalculatedMemberInSchema(Context context) {
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Warehouse and Sales",
            null,
            "  <CalculatedMember name=\"Shipped per Ordered\" dimension=\"Measures\">\n"
            + "    <Formula>[Measures].[Units Shipped] / [Measures].[Unit Sales]</Formula>\n"
            + "    <CalculatedMemberProperty name=\"FORMAT_STRING\" value=\"#.0%\"/>\n"
            + "  </CalculatedMember>\n"));
         */
        withSchema(context, SchemaModifiers.VirtualCubeTestModifier1::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select\n"
            + " {[Measures].[Unit Sales], \n"
            + "  [Measures].[Shipped per Ordered]} on 0,\n"
            + " NON EMPTY Crossjoin([Product].Children, [Time].[1997].Children) on 1\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Shipped per Ordered]}\n"
            + "Axis #2:\n"
            + "{[Product].[Drink], [Time].[1997].[Q1]}\n"
            + "{[Product].[Drink], [Time].[1997].[Q2]}\n"
            + "{[Product].[Drink], [Time].[1997].[Q3]}\n"
            + "{[Product].[Drink], [Time].[1997].[Q4]}\n"
            + "{[Product].[Food], [Time].[1997].[Q1]}\n"
            + "{[Product].[Food], [Time].[1997].[Q2]}\n"
            + "{[Product].[Food], [Time].[1997].[Q3]}\n"
            + "{[Product].[Food], [Time].[1997].[Q4]}\n"
            + "{[Product].[Non-Consumable], [Time].[1997].[Q1]}\n"
            + "{[Product].[Non-Consumable], [Time].[1997].[Q2]}\n"
            + "{[Product].[Non-Consumable], [Time].[1997].[Q3]}\n"
            + "{[Product].[Non-Consumable], [Time].[1997].[Q4]}\n"
            + "Row #0: 5,976\n"
            + "Row #0: 77.6%\n"
            + "Row #1: 5,895\n"
            + "Row #1: 76.4%\n"
            + "Row #2: 6,065\n"
            + "Row #2: 103.2%\n"
            + "Row #3: 6,661\n"
            + "Row #3: 87.1%\n"
            + "Row #4: 47,809\n"
            + "Row #4: 77.7%\n"
            + "Row #5: 44,825\n"
            + "Row #5: 79.1%\n"
            + "Row #6: 47,440\n"
            + "Row #6: 87.6%\n"
            + "Row #7: 51,866\n"
            + "Row #7: 66.9%\n"
            + "Row #8: 12,506\n"
            + "Row #8: 73.3%\n"
            + "Row #9: 11,890\n"
            + "Row #9: 77.6%\n"
            + "Row #10: 12,343\n"
            + "Row #10: 80.9%\n"
            + "Row #11: 13,497\n"
            + "Row #11: 68.8%\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAllMeasureMembers(Context context) {
        // result should exclude measures that are not explicitly defined
        // in the virtual cube (e.g., [Profit last Period])
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select\n"
            + "{[Measures].allMembers} on columns\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Sales Count]}\n"
            + "{[Measures].[Store Cost]}\n"
            + "{[Measures].[Store Sales]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Store Invoice]}\n"
            + "{[Measures].[Supply Time]}\n"
            + "{[Measures].[Units Ordered]}\n"
            + "{[Measures].[Units Shipped]}\n"
            + "{[Measures].[Warehouse Cost]}\n"
            + "{[Measures].[Warehouse Profit]}\n"
            + "{[Measures].[Warehouse Sales]}\n"
            + "{[Measures].[Profit]}\n"
            + "{[Measures].[Profit Growth]}\n"
            + "{[Measures].[Average Warehouse Sale]}\n"
            + "{[Measures].[Profit Per Unit Shipped]}\n"
            + "Row #0: 86,837\n"
            + "Row #0: 225,627.23\n"
            + "Row #0: 565,238.13\n"
            + "Row #0: 266,773\n"
            + "Row #0: 102,278.409\n"
            + "Row #0: 10,425\n"
            + "Row #0: 227238.0\n"
            + "Row #0: 207726.0\n"
            + "Row #0: 89,043.253\n"
            + "Row #0: 107,727.635\n"
            + "Row #0: 196,770.888\n"
            + "Row #0: $339,610.90\n"
            + "Row #0: 0.0%\n"
            + "Row #0: $2.21\n"
            + "Row #0: $1.63\n");
    }

    /**
     * Test a virtual cube where one of the dimensions contains an
     * ordinalColumn property
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOrdinalColumn(Context context) {
        class TestOrdinalColumnModifier extends PojoMappingModifier {

            public TestOrdinalColumnModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends CubeMapping> schemaCubes(CatalogMapping schema) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.schemaCubes(schema));
                result.add(VirtualCubeMappingImpl.builder()
                    .withName("Sales vs HR")
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withPhysicalCube((PhysicalCubeMappingImpl) look(FoodmartMappingSupplier.CUBE_HR))
                    		.withOverrideDimensionName("Store")
                            .build(),
                        DimensionConnectorMappingImpl.builder()
                        	.withPhysicalCube((PhysicalCubeMappingImpl) look(FoodmartMappingSupplier.CUBE_HR))
                            .withOverrideDimensionName("Position")
                            .build()
                    ))
                    .withReferencedMeasures(List.of(look(FoodmartMappingSupplier.MEASURE_ORG_SALARY)))
                    .build());
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            null,
            "<VirtualCube name=\"Sales vs HR\">\n"
            + "<VirtualCubeDimension name=\"Store\"/>\n"
            + "<VirtualCubeDimension cubeName=\"HR\" name=\"Position\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"HR\" name=\"[Measures].[Org Salary]\"/>\n"
            + "</VirtualCube>",
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestOrdinalColumnModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures].[Org Salary]} on columns, "
            + "non empty "
            + "crossjoin([Store].[Store Country].members, [Position].[Store Management].children) "
            + "on rows from [Sales vs HR]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Org Salary]}\n"
            + "Axis #2:\n"
            + "{[Store].[Canada], [Position].[Store Management].[Store Manager]}\n"
            + "{[Store].[Canada], [Position].[Store Management].[Store Assistant Manager]}\n"
            + "{[Store].[Canada], [Position].[Store Management].[Store Shift Supervisor]}\n"
            + "{[Store].[Mexico], [Position].[Store Management].[Store Manager]}\n"
            + "{[Store].[Mexico], [Position].[Store Management].[Store Assistant Manager]}\n"
            + "{[Store].[Mexico], [Position].[Store Management].[Store Shift Supervisor]}\n"
            + "{[Store].[USA], [Position].[Store Management].[Store Manager]}\n"
            + "{[Store].[USA], [Position].[Store Management].[Store Assistant Manager]}\n"
            + "{[Store].[USA], [Position].[Store Management].[Store Shift Supervisor]}\n"
            + "Row #0: $462.86\n"
            + "Row #1: $394.29\n"
            + "Row #2: $565.71\n"
            + "Row #3: $13,254.55\n"
            + "Row #4: $11,443.64\n"
            + "Row #5: $17,705.46\n"
            + "Row #6: $4,069.80\n"
            + "Row #7: $3,417.72\n"
            + "Row #8: $5,145.96\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDefaultMeasureProperty(Context context) {
        class TestDefaultMeasurePropertyModifier extends PojoMappingModifier {

            public TestDefaultMeasurePropertyModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends CubeMapping> schemaCubes(CatalogMapping schema) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.schemaCubes(schema));
                result.add(VirtualCubeMappingImpl.builder()
                    .withName("Sales vs Warehouse")
                    .withDefaultMeasure((MemberMappingImpl) look(FoodmartMappingSupplier.MEASURE_UNIT_SALES))
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_PRODUCT))
                    		.withOverrideDimensionName("Product")
                            .build()
                    ))
                    .withReferencedMeasures(List.of(
                    		look(FoodmartMappingSupplier.MEASURE_WAREHOUSE_SALES),
                    		look(FoodmartMappingSupplier.MEASURE_UNIT_SALES)
                    ))
                    .withReferencedCalculatedMembers(List.of(
                    	look(FoodmartMappingSupplier.CALCULATED_MEMBER_PROFIT)
                    ))
                    .build());
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            null,
            "<VirtualCube name=\"Sales vs Warehouse\" defaultMeasure=\"Unit Sales\">\n"
            + "<VirtualCubeDimension name=\"Product\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"Warehouse\" "
            + "name=\"[Measures].[Warehouse Sales]\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"Sales\" "
            + "name=\"[Measures].[Unit Sales]\"/>\n"
            + "<VirtualCubeMeasure cubeName=\"Sales\" "
            + "name=\"[Measures].[Profit]\"/>\n"
            + "</VirtualCube>",
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestDefaultMeasurePropertyModifier::new);
        String queryWithoutFilter =
            "select"
            + " from [Sales vs Warehouse]";
        String queryWithDeflaultMeasureFilter =
            "select "
            + "from [Sales vs Warehouse] where measures.[Unit Sales]";
        assertQueriesReturnSimilarResults(context.getConnectionWithDefaultRole(),
            queryWithoutFilter, queryWithDeflaultMeasureFilter);
    }

    /**
     * Checks that native set caching considers base cubes in the cache key.
     * Native sets referencing different base cubes do not share the cached
     * result.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNativeSetCaching(Context context) {
        // Only need to run this against one db to verify caching
        // behavior is correct.
        final Dialect dialect = getDialect(context.getConnectionWithDefaultRole());
        if (getDatabaseProduct(dialect.getDialectName()) != DatabaseProduct.DERBY) {
            return;
        }

        if (!context.getConfig().enableNativeCrossJoin()
            && !SystemWideProperties.instance().EnableNativeNonEmpty)
        {
            // Only run the tests if either native CrossJoin or native NonEmpty
            // is enabled.
            return;
        }

        String query1 =
            "With "
            + "Set [*NATIVE_CJ_SET] as 'NonEmptyCrossJoin([Product].[Product Family].Members, [Store].[Store Country].Members)' "
            + "Select "
            + "{[Store Sales]} on columns, "
            + "Non Empty Generate([*NATIVE_CJ_SET], {([Product].CurrentMember,[Store].CurrentMember)}) on rows "
            + "From [Warehouse and Sales]";

        String query2 =
            "With "
            + "Set [*NATIVE_CJ_SET] as 'NonEmptyCrossJoin([Product].[Product Family].Members, [Store].[Store Country].Members)' "
            + "Select "
            + "{[Warehouse Sales]} on columns, "
            + "Non Empty Generate([*NATIVE_CJ_SET], {([Product].CurrentMember,[Store].CurrentMember)}) on rows "
            + "From [Warehouse and Sales]";

        String derbyNecjSql1, derbyNecjSql2;

        if (context.getConfig().enableNativeCrossJoin()) {
            derbyNecjSql1 =
                "select "
                + "\"product_class\".\"product_family\", "
                + "\"store\".\"store_country\" "
                + "from "
                + "\"product\" as \"product\", "
                + "\"product_class\" as \"product_class\", "
                + "\"sales_fact_1997\" as \"sales_fact_1997\", "
                + "\"store\" as \"store\" "
                + "where "
                + "\"product\".\"product_class_id\" = \"product_class\".\"product_class_id\" "
                + "and \"sales_fact_1997\".\"product_id\" = \"product\".\"product_id\" "
                + "and \"sales_fact_1997\".\"store_id\" = \"store\".\"store_id\" "
                + "group by \"product_class\".\"product_family\", \"store\".\"store_country\" "
                + "order by 1 ASC, 2 ASC";

            derbyNecjSql2 =
                "select "
                + "\"product_class\".\"product_family\", "
                + "\"store\".\"store_country\" "
                + "from "
                + "\"product\" as \"product\", "
                + "\"product_class\" as \"product_class\", "
                + "\"inventory_fact_1997\" as \"inventory_fact_1997\", "
                + "\"store\" as \"store\" "
                + "where "
                + "\"product\".\"product_class_id\" = \"product_class\".\"product_class_id\" "
                + "and \"inventory_fact_1997\".\"product_id\" = \"product\".\"product_id\" "
                + "and \"inventory_fact_1997\".\"store_id\" = \"store\".\"store_id\" "
                + "group by \"product_class\".\"product_family\", \"store\".\"store_country\" "
                + "order by 1 ASC, 2 ASC";
        } else {
            // NECJ is truend off so native NECJ SQL will not be generated;
            // however, because the NECJ set should not find match in the cache,
            // each NECJ input will still be joined with the correct
            // fact table if NonEmpty condition is natively evaluated.
            derbyNecjSql1 =
                "select "
                + "\"store\".\"store_country\" "
                + "from "
                + "\"store\" as \"store\", "
                + "\"sales_fact_1997\" as \"sales_fact_1997\" "
                + "where "
                + "\"sales_fact_1997\".\"store_id\" = \"store\".\"store_id\" "
                + "group by \"store\".\"store_country\" "
                + "order by 1 ASC";

            derbyNecjSql2 =
                "select "
                + "\"store\".\"store_country\" "
                + "from "
                + "\"store\" as \"store\", "
                + "\"inventory_fact_1997\" as \"inventory_fact_1997\" "
                + "where "
                + "\"inventory_fact_1997\".\"store_id\" = \"store\".\"store_id\" "
                + "group by \"store\".\"store_country\" "
                + "order by 1 ASC";
        }

        SqlPattern[] patterns1 = {
            new SqlPattern(
                DatabaseProduct.DERBY, derbyNecjSql1, derbyNecjSql1)
        };

        SqlPattern[] patterns2 = {
            new SqlPattern(
                DatabaseProduct.DERBY, derbyNecjSql2, derbyNecjSql2)
        };

        // Run query 1 with cleared cache;
        // Make sure NECJ 1 is evaluated natively.
        assertQuerySql(context.getConnectionWithDefaultRole(), query1, patterns1, true);

        // Now run query 2 with warm cache;
        // Make sure NECJ 2 does not reuse the cache result from NECJ 1, and
        // NECJ 2 is evaluated natively.
        assertQuerySql(context.getConnectionWithDefaultRole(), query2, patterns2, false);
    }

    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-322">
     * MONDRIAN-322, "cube.getStar() throws NullPointerException"</a>.
     * Happens when you aggregate distinct-count measures in a virtual cube.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBugMondrian322(Context context) {
        class TestBugMondrian322Modifier extends PojoMappingModifier {

            public TestBugMondrian322Modifier(CatalogMapping catalog) {
                super(catalog);
            }

            protected List<? extends CubeMapping> schemaCubes(CatalogMapping schema) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.schemaCubes(schema));
                result.add(VirtualCubeMappingImpl.builder()
                    .withName("Warehouse and Sales2")
                    .withDefaultMeasure((MemberMappingImpl) look(FoodmartMappingSupplier.MEASURE_STORE_SALES))
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withPhysicalCube((PhysicalCubeMappingImpl) look(FoodmartMappingSupplier.CUBE_SALES))
                    		.withOverrideDimensionName("Customers")
                            .build(),
                        DimensionConnectorMappingImpl.builder()
                        	.withOverrideDimensionName("Time")
                            .build(),
                        DimensionConnectorMappingImpl.builder()
                            .withPhysicalCube((PhysicalCubeMappingImpl) look(FoodmartMappingSupplier.CUBE_WAREHOUSE))
                            .withOverrideDimensionName("Warehouse")
                            .build()

                    ))
                    .withReferencedMeasures(List.of(
                    	look(FoodmartMappingSupplier.MEASURE_CUSTOMER_COUNT),
                    	look(FoodmartMappingSupplier.MEASURE_STORE_SALES)
                    ))
                    .build());
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            null,
            "<VirtualCube name=\"Warehouse and Sales2\" defaultMeasure=\"Store Sales\">\n"
            + "  <VirtualCubeDimension cubeName=\"Sales\" name=\"Customers\"/>\n"
            + "  <VirtualCubeDimension name=\"Time\"/>\n"
            + "  <VirtualCubeDimension cubeName=\"Warehouse\" name=\"Warehouse\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Customer Count]\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Store Sales]\"/>\n"
            + "</VirtualCube>",
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestBugMondrian322Modifier::new);

//       This test case does not actually reject the dimension constraint from
//       an unrelated base cube. The reason is that the constraint contains an
//       AllLevel member. Even though semantically constraining Cells using an
//       non-existent dimension perhaps does not make sense; however, in the
//       case where the constraint contains AllLevel member, the constraint
//       can be considered "always true".
//
//       See the next test case for a constraint that does not contain
//       AllLevel member and hence cannot be satisfied. The cell should be
//       empty.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Warehouse].[x] as 'Aggregate([Warehouse].members)'\n"
            + "member [Measures].[foo] AS '([Warehouse].[x],[Measures].[Customer Count])'\n"
            + "select {[Measures].[foo]} on 0 from [Warehouse And Sales2]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[foo]}\n"
            + "Row #0: 5,581\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBugMondrian322a(Context context) {
        class TestBugMondrian322aModifier extends PojoMappingModifier {

            public TestBugMondrian322aModifier(CatalogMapping catalog) {
                super(catalog);
            }
            protected List<? extends CubeMapping> schemaCubes(CatalogMapping schema) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.schemaCubes(schema));
                result.add(VirtualCubeMappingImpl.builder()
                    .withName("Warehouse and Sales2")
                    .withDefaultMeasure((MemberMappingImpl) look(FoodmartMappingSupplier.MEASURE_STORE_SALES))
                    .withDimensionConnectors(List.of(
                        DimensionConnectorMappingImpl.builder()
                        	.withPhysicalCube((PhysicalCubeMappingImpl) look(FoodmartMappingSupplier.CUBE_SALES))
                        	.withOverrideDimensionName("Customers")
                            .build(),
                        DimensionConnectorMappingImpl.builder()
                        	.withOverrideDimensionName("Time")
                            .build(),
                        DimensionConnectorMappingImpl.builder()
                        	.withPhysicalCube((PhysicalCubeMappingImpl) look(FoodmartMappingSupplier.CUBE_WAREHOUSE))
                            .withOverrideDimensionName("Warehouse")
                            .build()

                    ))
                    .withReferencedMeasures(List.of(
                    		look(FoodmartMappingSupplier.MEASURE_CUSTOMER_COUNT),
                    		look(FoodmartMappingSupplier.MEASURE_STORE_SALES)
                    ))
                    .build());
                return result;
            }

        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            null,
            "<VirtualCube name=\"Warehouse and Sales2\" defaultMeasure=\"Store Sales\">\n"
            + "  <VirtualCubeDimension cubeName=\"Sales\" name=\"Customers\"/>\n"
            + "  <VirtualCubeDimension name=\"Time\"/>\n"
            + "  <VirtualCubeDimension cubeName=\"Warehouse\" name=\"Warehouse\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Customer Count]\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Store Sales]\"/>\n"
            + "</VirtualCube>",
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestBugMondrian322aModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Warehouse].[x] as 'Aggregate({[Warehouse].[Canada], [Warehouse].[USA]})'\n"
            + "member [Measures].[foo] AS '([Warehouse].[x],[Measures].[Customer Count])'\n"
            + "select {[Measures].[foo]} on 0 from [Warehouse And Sales2]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[foo]}\n"
            + "Row #0: \n");
    }

    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-352">
     * MONDRIAN-352, "Caption is not set on RolapVirtualCubeMesure"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testVirtualCubeMeasureCaption(Context context) {
        class TestVirtualCubeMeasureCaptionModifier extends PojoMappingModifier {

            public TestVirtualCubeMeasureCaptionModifier(CatalogMapping catalog) {
                super(catalog);
            }

            protected List<? extends CubeMapping> schemaCubes(CatalogMapping schema) {
            	PhysicalCubeMappingImpl testStore;
            	MeasureMappingImpl storeSqftMeasure;
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.schemaCubes(schema));
                result.add(
                	testStore = PhysicalCubeMappingImpl.builder()
                    .withName("TestStore")
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("HCB")
                    		.withDimension(StandardDimensionMappingImpl.builder()
                    			//.withCaption("Has coffee bar caption")
                    			.withHierarchies(List.of(
                                HierarchyMappingImpl.builder()
                                    .withHasAll(true)
                                    .withLevels(List.of(
                                        LevelMappingImpl.builder()
                                            .withName("Has coffee bar")
                                            .withColumn(FoodmartMappingSupplier.COFFEE_BAR_COLUMN_IN_STORE)
                                            .withUniqueMembers(true)
                                            .withType(DataType.BOOLEAN)
                                            .build()
                                    ))
                                    .build()
                            )).build())
                            .build()
                    ))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                    	storeSqftMeasure = MeasureMappingImpl.builder()
                            .withName("Store Sqft")
                            //.withCaption("Store Sqft Caption")
                            .withColumn(FoodmartMappingSupplier.STORE_SQFT_COLUMN_IN_STORE)
                            .withAggregatorType(MeasureAggregatorType.SUM)
                            .withFormatString("#,###")
                            .build()
                    )).build()))
                    .build());

                result.add(VirtualCubeMappingImpl.builder()
                        .withName("VirtualTestStore")
                        .withDimensionConnectors(List.of(
                            DimensionConnectorMappingImpl.builder()
                            	.withPhysicalCube(testStore)
                            	.withOverrideDimensionName("HCB")
                                .build()

                        ))
                        .withReferencedMeasures(List.of(
                        	storeSqftMeasure
                        ))
                        .build());


                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"TestStore\">\n"
            + "  <Table name=\"store\"/>\n"
            + "  <Dimension name=\"HCB\" caption=\"Has coffee bar caption\">\n"
            + "    <Hierarchy hasAll=\"true\">\n"
            + "      <Level name=\"Has coffee bar\" column=\"coffee_bar\" uniqueMembers=\"true\"\n"
            + "          type=\"Boolean\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Measure name=\"Store Sqft\" caption=\"Store Sqft Caption\" column=\"store_sqft\" aggregator=\"sum\" formatString=\"#,###\"/>\n"
            + "</Cube>\n",

            "<VirtualCube name=\"VirtualTestStore\">\n"
            + "  <VirtualCubeDimension cubeName=\"TestStore\" name=\"HCB\"/>\n"
            + "  <VirtualCubeMeasure   cubeName=\"TestStore\" name=\"[Measures].[Store Sqft]\"/>\n"
            + "</VirtualCube>",
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestVirtualCubeMeasureCaptionModifier::new);
        Result result = executeQuery(
            "select {[Measures].[Store Sqft]} ON COLUMNS,"
            + "{[HCB]} ON ROWS "
            + "from [VirtualTestStore]", context.getConnectionWithDefaultRole());

        Axis[] axes = result.getAxes();
        List<Position> positions = axes[0].getPositions();
        Member m0 = positions.get(0).get(0);
        String caption = m0.getCaption();
        assertEquals("Store Sqft", caption);
    }

    /**
     * Test that RolapCubeLevel is used correctly in the context of virtual
     * cube.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRolapCubeLevelInVirtualCube(Context context) {
        String query1 =
            "With "
            + "Set [*NATIVE_CJ_SET] as 'NonEmptyCrossJoin([*BASE_MEMBERS_Warehouse],[*BASE_MEMBERS_Time])' "
            + "Set [*NATIVE_MEMBERS_Warehouse] as 'Generate([*NATIVE_CJ_SET], {[Warehouse].CurrentMember})' "
            + "Set [*BASE_MEMBERS_Warehouse] as '[Warehouse].[Country].Members' "
            + "Set [*NATIVE_MEMBERS_Time] as 'Generate([*NATIVE_CJ_SET], {[Time].[Time].CurrentMember})' "
            + "Set [*BASE_MEMBERS_Time] as '[Time].[Month].Members' "
            + "Set [*BASE_MEMBERS_Measures] as '{[Measures].[*FORMATTED_MEASURE_0]}' Member [Measures].[*FORMATTED_MEASURE_0] as '[Measures].[Warehouse Sales]', FORMAT_STRING = '#,##0', SOLVE_ORDER=400 "
            + "Select [*BASE_MEMBERS_Measures] on columns, Non Empty Generate([*NATIVE_CJ_SET], {([Warehouse].currentMember,[Time].[Time].currentMember)}) on rows From [Warehouse and Sales] ";

        String query2 =
            "With "
            + "Set [*NATIVE_CJ_SET] as 'NonEmptyCrossJoin([*BASE_MEMBERS_Warehouse],[*BASE_MEMBERS_Time])' "
            + "Set [*NATIVE_MEMBERS_Warehouse] as 'Generate([*NATIVE_CJ_SET], {[Warehouse].CurrentMember})' "
            + "Set [*BASE_MEMBERS_Warehouse] as '[Warehouse].[Country].Members' "
            + "Set [*NATIVE_MEMBERS_Time] as 'Generate([*NATIVE_CJ_SET], {[Time].[Time].CurrentMember})' "
            + "Set [*BASE_MEMBERS_Time] as 'Filter([Time].[Month].Members,[Time].[Time].CurrentMember Not In {[Time].[1997].[Q1].[2]})' "
            + "Set [*BASE_MEMBERS_Measures] as '{[Measures].[*FORMATTED_MEASURE_0]}' Member [Measures].[*FORMATTED_MEASURE_0] as '[Measures].[Warehouse Sales]', FORMAT_STRING = '#,##0', SOLVE_ORDER=400 "
            + "Select [*BASE_MEMBERS_Measures] on columns, Non Empty Generate([*NATIVE_CJ_SET], {([Warehouse].currentMember,[Time].[Time].currentMember)}) on rows From [Warehouse and Sales]";

        executeQuery(query1, context.getConnectionWithDefaultRole());

        /* The query with the filter should now succeed without NPE */
        String result =
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Warehouse].[USA], [Time].[1997].[Q1].[1]}\n"
            + "{[Warehouse].[USA], [Time].[1997].[Q1].[3]}\n"
            + "{[Warehouse].[USA], [Time].[1997].[Q2].[4]}\n"
            + "{[Warehouse].[USA], [Time].[1997].[Q2].[5]}\n"
            + "{[Warehouse].[USA], [Time].[1997].[Q2].[6]}\n"
            + "{[Warehouse].[USA], [Time].[1997].[Q3].[7]}\n"
            + "{[Warehouse].[USA], [Time].[1997].[Q3].[8]}\n"
            + "{[Warehouse].[USA], [Time].[1997].[Q3].[9]}\n"
            + "{[Warehouse].[USA], [Time].[1997].[Q4].[10]}\n"
            + "{[Warehouse].[USA], [Time].[1997].[Q4].[11]}\n"
            + "{[Warehouse].[USA], [Time].[1997].[Q4].[12]}\n"
            + "Row #0: 21,762\n"
            + "Row #1: 13,775\n"
            + "Row #2: 15,938\n"
            + "Row #3: 15,649\n"
            + "Row #4: 14,629\n"
            + "Row #5: 18,626\n"
            + "Row #6: 15,833\n"
            + "Row #7: 21,393\n"
            + "Row #8: 17,100\n"
            + "Row #9: 15,356\n"
            + "Row #10: 13,948\n";

        assertQueryReturns(context.getConnectionWithDefaultRole(), query2, result);
    }

    /**
     * Tests that the logic to apply non empty context constraint in virtual
     * cube is correct.  The joins shouldn't be cartesian product.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonEmptyCJConstraintOnVirtualCube(Context context) {
        if (!context.getConfig().enableNativeCrossJoin()) {
            // Generated SQL is different if NonEmptyCrossJoin is evaluated in
            // memory.
            return;
        }
        ((TestConfig)context.getConfig()).setGenerateFormattedSql(true);
        String query =
            "with "
            + "set [foo] as [Time].[Month].members "
            + "set [bar] as {[Store].[USA]} "
            + "Select {[Measures].[Warehouse Sales],[Measures].[Store Sales]} on columns, "
            + "nonemptycrossjoin([foo],[bar]) on rows "
            + "From [Warehouse and Sales] "
            + "Where ([Product].[All Products].[Food])";

        // Note that for MySQL (because MySQL sorts NULLs first), because there
        // is a UNION (which prevents us from sorting on column names or
        // expressions) the ORDER BY clause should be something like
        //   ORDER BY ISNULL(1), 1 ASC, ISNULL(2), 2 ASC, ISNULL(3), 3 ASC,
        //   ISNULL(4), 4 ASC
        // but ISNULL(1) isn't valid SQL, so we forego correct ordering of NULL
        // values.
        String mysqlSQL =
            context.getConfig().useAggregates()
            ? "select\n"
            + "    *\n"
            + "from\n"
            + "    (select\n"
            + "    `agg_c_14_sales_fact_1997`.`the_year` as `c0`,\n"
            + "    `agg_c_14_sales_fact_1997`.`quarter` as `c1`,\n"
            + "    `agg_c_14_sales_fact_1997`.`month_of_year` as `c2`,\n"
            + "    `store`.`store_country` as `c3`\n"
            + "from\n"
            + "    `agg_c_14_sales_fact_1997` as `agg_c_14_sales_fact_1997`,\n"
            + "    `store` as `store`,\n"
            + "    `product_class` as `product_class`,\n"
            + "    `product` as `product`\n"
            + "where\n"
            + "    `agg_c_14_sales_fact_1997`.`store_id` = `store`.`store_id`\n"
            + "and\n"
            + "    `agg_c_14_sales_fact_1997`.`product_id` = `product`.`product_id`\n"
            + "and\n"
            + "    `product`.`product_class_id` = `product_class`.`product_class_id`\n"
            + "and\n"
            + "    `product_class`.`product_family` = 'Food'\n"
            + "and\n"
            + "    (`store`.`store_country` = 'USA')\n"
            + "group by\n"
            + "    `agg_c_14_sales_fact_1997`.`the_year`,\n"
            + "    `agg_c_14_sales_fact_1997`.`quarter`,\n"
            + "    `agg_c_14_sales_fact_1997`.`month_of_year`,\n"
            + "    `store`.`store_country`\n"
            + "union\n"
            + "select\n"
            + "    `time_by_day`.`the_year` as `c0`,\n"
            + "    `time_by_day`.`quarter` as `c1`,\n"
            + "    `time_by_day`.`month_of_year` as `c2`,\n"
            + "    `store`.`store_country` as `c3`\n"
            + "from\n"
            + "    `time_by_day` as `time_by_day`,\n"
            + "    `inventory_fact_1997` as `inventory_fact_1997`,\n"
            + "    `store` as `store`,\n"
            + "    `product_class` as `product_class`,\n"
            + "    `product` as `product`\n"
            + "where\n"
            + "    `inventory_fact_1997`.`time_id` = `time_by_day`.`time_id`\n"
            + "and\n"
            + "    `inventory_fact_1997`.`store_id` = `store`.`store_id`\n"
            + "and\n"
            + "    `inventory_fact_1997`.`product_id` = `product`.`product_id`\n"
            + "and\n"
            + "    `product`.`product_class_id` = `product_class`.`product_class_id`\n"
            + "and\n"
            + "    `product_class`.`product_family` = 'Food'\n"
            + "and\n"
            + "    (`store`.`store_country` = 'USA')\n"
            + "group by\n"
            + "    `time_by_day`.`the_year`,\n"
            + "    `time_by_day`.`quarter`,\n"
            + "    `time_by_day`.`month_of_year`,\n"
            + "    `store`.`store_country`) as `unionQuery`\n"
            + "order by\n"
            + "    ISNULL(1) ASC, 1 ASC,\n"
            + "    ISNULL(2) ASC, 2 ASC,\n"
            + "    ISNULL(3) ASC, 3 ASC,\n"
            + "    ISNULL(4) ASC, 4 ASC"
            : "select\n"
            + "    *\n"
            + "from\n"
            + "    (select\n"
            + "    `time_by_day`.`the_year` as `c0`,\n"
            + "    `time_by_day`.`quarter` as `c1`,\n"
            + "    `time_by_day`.`month_of_year` as `c2`,\n"
            + "    `store`.`store_country` as `c3`\n"
            + "from\n"
            + "    `time_by_day` as `time_by_day`,\n"
            + "    `sales_fact_1997` as `sales_fact_1997`,\n"
            + "    `store` as `store`,\n"
            + "    `product_class` as `product_class`,\n"
            + "    `product` as `product`\n"
            + "where\n"
            + "    `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`\n"
            + "and\n"
            + "    `sales_fact_1997`.`store_id` = `store`.`store_id`\n"
            + "and\n"
            + "    `sales_fact_1997`.`product_id` = `product`.`product_id`\n"
            + "and\n"
            + "    `product`.`product_class_id` = `product_class`.`product_class_id`\n"
            + "and\n"
            + "    `product_class`.`product_family` = 'Food'\n"
            + "and\n"
            + "    (`store`.`store_country` = 'USA')\n"
            + "group by\n"
            + "    `time_by_day`.`the_year`,\n"
            + "    `time_by_day`.`quarter`,\n"
            + "    `time_by_day`.`month_of_year`,\n"
            + "    `store`.`store_country`\n"
            + "union\n"
            + "select\n"
            + "    `time_by_day`.`the_year` as `c0`,\n"
            + "    `time_by_day`.`quarter` as `c1`,\n"
            + "    `time_by_day`.`month_of_year` as `c2`,\n"
            + "    `store`.`store_country` as `c3`\n"
            + "from\n"
            + "    `time_by_day` as `time_by_day`,\n"
            + "    `inventory_fact_1997` as `inventory_fact_1997`,\n"
            + "    `store` as `store`,\n"
            + "    `product_class` as `product_class`,\n"
            + "    `product` as `product`\n"
            + "where\n"
            + "    `inventory_fact_1997`.`time_id` = `time_by_day`.`time_id`\n"
            + "and\n"
            + "    `inventory_fact_1997`.`store_id` = `store`.`store_id`\n"
            + "and\n"
            + "    `inventory_fact_1997`.`product_id` = `product`.`product_id`\n"
            + "and\n"
            + "    `product`.`product_class_id` = `product_class`.`product_class_id`\n"
            + "and\n"
            + "    `product_class`.`product_family` = 'Food'\n"
            + "and\n"
            + "    (`store`.`store_country` = 'USA')\n"
            + "group by\n"
            + "    `time_by_day`.`the_year`,\n"
            + "    `time_by_day`.`quarter`,\n"
            + "    `time_by_day`.`month_of_year`,\n"
            + "    `store`.`store_country`) as `unionQuery`\n"
            + "order by\n"
            + "    ISNULL(1) ASC, 1 ASC,\n"
            + "    ISNULL(2) ASC, 2 ASC,\n"
            + "    ISNULL(3) ASC, 3 ASC,\n"
            + "    ISNULL(4) ASC, 4 ASC";

        SqlPattern[] mysqlPattern = {
            new SqlPattern(DatabaseProduct.MYSQL, mysqlSQL, mysqlSQL)
        };

        String result =
            "Axis #0:\n"
            + "{[Product].[Food]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Warehouse Sales]}\n"
            + "{[Measures].[Store Sales]}\n"
            + "Axis #2:\n"
            + "{[Time].[1997].[Q1].[1], [Store].[USA]}\n"
            + "{[Time].[1997].[Q1].[2], [Store].[USA]}\n"
            + "{[Time].[1997].[Q1].[3], [Store].[USA]}\n"
            + "{[Time].[1997].[Q2].[4], [Store].[USA]}\n"
            + "{[Time].[1997].[Q2].[5], [Store].[USA]}\n"
            + "{[Time].[1997].[Q2].[6], [Store].[USA]}\n"
            + "{[Time].[1997].[Q3].[7], [Store].[USA]}\n"
            + "{[Time].[1997].[Q3].[8], [Store].[USA]}\n"
            + "{[Time].[1997].[Q3].[9], [Store].[USA]}\n"
            + "{[Time].[1997].[Q4].[10], [Store].[USA]}\n"
            + "{[Time].[1997].[Q4].[11], [Store].[USA]}\n"
            + "{[Time].[1997].[Q4].[12], [Store].[USA]}\n"
            + "Row #0: 16,083.015\n"
            + "Row #0: 32,993.12\n"
            + "Row #1: 9,298.379\n"
            + "Row #1: 32,139.91\n"
            + "Row #2: 10,129.659\n"
            + "Row #2: 36,128.29\n"
            + "Row #3: 11,415.462\n"
            + "Row #3: 30,747.21\n"
            + "Row #4: 11,358.086\n"
            + "Row #4: 31,896.24\n"
            + "Row #5: 10,425.768\n"
            + "Row #5: 32,792.55\n"
            + "Row #6: 13,684.193\n"
            + "Row #6: 36,324.76\n"
            + "Row #7: 11,332.797\n"
            + "Row #7: 33,842.75\n"
            + "Row #8: 15,667.978\n"
            + "Row #8: 31,640.09\n"
            + "Row #9: 11,902.18\n"
            + "Row #9: 30,337.12\n"
            + "Row #10: 10,144.841\n"
            + "Row #10: 38,709.15\n"
            + "Row #11: 9,705.561\n"
            + "Row #11: 41,484.40\n";

        Connection connection = context.getConnectionWithDefaultRole();
        assertQuerySql(connection, query, mysqlPattern, true);
        assertQueryReturns(connection, query, result);
    }

    /**
     * Tests that the logic to apply non empty context constraint in virtual
     * cube is correct.  The joins shouldn't be cartesian product.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonEmptyConstraintOnVirtualCubeWithCalcMeasure(Context context) {
        if (!SystemWideProperties.instance().EnableNativeNonEmpty) {
            // Generated SQL is different if NON EMPTY is evaluated in memory.
            return;
        }
        // we want to make sure a SqlConstraint is used for retrieving
        // [Product Family].members
        context.getCatalogCache().clear();
        ((TestConfig)context.getConfig()).setLevelPreCacheThreshold(0);

        ((TestConfig)context.getConfig()).setGenerateFormattedSql(true);
        String query =
            "with "
            + "set [bar] as {[Store].[USA]} "
            + "member [Measures].[CalcMeasure] as '[Measures].[Warehouse Sales] / [Measures].[Store Sales]' "
            + "Select "
            + "{[Measures].[CalcMeasure]} on columns, "
            + "non empty([Product].[Product Family].Members) on rows "
            + "From [Warehouse and Sales] "
            + "where [bar]";

        // Comments as for testNonEmptyCJConstraintOnVirtualCube. The ORDER BY
        // clause should be "order by ISNULL(1), 1 ASC" but we will settle for
        // "order by 1 ASC" and forego correct sorting of NULL values.
        String mysqlSQL =
            context.getConfig().useAggregates()
                ? "select\n"
                + "    *\n"
                + "from\n"
                + "    (select\n"
                + "    `product_class`.`product_family` as `c0`\n"
                + "from\n"
                + "    `product` as `product`,\n"
                + "    `product_class` as `product_class`,\n"
                + "    `agg_c_14_sales_fact_1997` as `agg_c_14_sales_fact_1997`,\n"
                + "    `store` as `store`\n"
                + "where\n"
                + "    `product`.`product_class_id` = `product_class`.`product_class_id`\n"
                + "and\n"
                + "    `agg_c_14_sales_fact_1997`.`product_id` = `product`.`product_id`\n"
                + "and\n"
                + "    `agg_c_14_sales_fact_1997`.`store_id` = `store`.`store_id`\n"
                + "and\n"
                + "    `store`.`store_country` = 'USA'\n"
                + "group by\n"
                + "    `product_class`.`product_family`\n"
                + "union\n"
                + "select\n"
                + "    `product_class`.`product_family` as `c0`\n"
                + "from\n"
                + "    `product` as `product`,\n"
                + "    `product_class` as `product_class`,\n"
                + "    `inventory_fact_1997` as `inventory_fact_1997`,\n"
                + "    `store` as `store`\n"
                + "where\n"
                + "    `product`.`product_class_id` = `product_class`.`product_class_id`\n"
                + "and\n"
                + "    `inventory_fact_1997`.`product_id` = `product`.`product_id`\n"
                + "and\n"
                + "    `inventory_fact_1997`.`store_id` = `store`.`store_id`\n"
                + "and\n"
                + "    `store`.`store_country` = 'USA'\n"
                + "group by\n"
                + "    `product_class`.`product_family`) as `unionQuery`\n"
                + "order by\n"
                + "    ISNULL(1) ASC, 1 ASC"
                : "select\n"
                + "    *\n"
                + "from\n"
                + "    (select\n"
                + "    `product_class`.`product_family` as `c0`\n"
                + "from\n"
                + "    `product` as `product`,\n"
                + "    `product_class` as `product_class`,\n"
                + "    `sales_fact_1997` as `sales_fact_1997`,\n"
                + "    `store` as `store`\n"
                + "where\n"
                + "    `product`.`product_class_id` = `product_class`.`product_class_id`\n"
                + "and\n"
                + "    `sales_fact_1997`.`product_id` = `product`.`product_id`\n"
                + "and\n"
                + "    `sales_fact_1997`.`store_id` = `store`.`store_id`\n"
                + "and\n"
                + "    `store`.`store_country` = 'USA'\n"
                + "group by\n"
                + "    `product_class`.`product_family`\n"
                + "union\n"
                + "select\n"
                + "    `product_class`.`product_family` as `c0`\n"
                + "from\n"
                + "    `product` as `product`,\n"
                + "    `product_class` as `product_class`,\n"
                + "    `inventory_fact_1997` as `inventory_fact_1997`,\n"
                + "    `store` as `store`\n"
                + "where\n"
                + "    `product`.`product_class_id` = `product_class`.`product_class_id`\n"
                + "and\n"
                + "    `inventory_fact_1997`.`product_id` = `product`.`product_id`\n"
                + "and\n"
                + "    `inventory_fact_1997`.`store_id` = `store`.`store_id`\n"
                + "and\n"
                + "    `store`.`store_country` = 'USA'\n"
                + "group by\n"
                + "    `product_class`.`product_family`) as `unionQuery`\n"
                + "order by\n"
                + "    ISNULL(1) ASC, 1 ASC";

        String result =
            "Axis #0:\n"
            + "{[Store].[USA]}\n"
            + "Axis #1:\n"
            + "{[Measures].[CalcMeasure]}\n"
            + "Axis #2:\n"
            + "{[Product].[Drink]}\n"
            + "{[Product].[Food]}\n"
            + "{[Product].[Non-Consumable]}\n"
            + "Row #0: 0.369\n"
            + "Row #1: 0.345\n"
            + "Row #2: 0.35\n";

        SqlPattern[] mysqlPattern = {
            new SqlPattern(
                DatabaseProduct.MYSQL, mysqlSQL, mysqlSQL)
        };

        Connection connection = context.getConnectionWithDefaultRole();
        assertQuerySql(connection, query, mysqlPattern, true);
        assertQueryReturns(connection, query, result);
    }

    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-902">
     * MONDRIAN-902, "mondrian populating the same members on both axes"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBugMondrian902(Context context) {
        Result result = executeQuery(
            "SELECT\n"
            + "NON EMPTY CrossJoin(\n"
            + "  [Education Level].[Education Level].Members,\n"
            + "  CrossJoin(\n"
            + "    [Product].[Product Family].Members,\n"
            + "    [Store].[Store State].Members)) ON COLUMNS,\n"
            + "NON EMPTY CrossJoin(\n"
            + "  [Promotions].[Promotion Name].Members,\n"
            + "  [Marital Status].[Marital Status].Members) ON ROWS\n"
            + "FROM [Warehouse and Sales]", context.getConnectionWithDefaultRole());
        assertEquals(
            "[[Education Level].[Bachelors Degree], [Product].[Drink], [Store].[USA].[CA]]",
            result.getAxes()[0].getPositions().get(0).toString());
        assertEquals(45, result.getAxes()[0].getPositions().size());
        // With bug MONDRIAN-902, this gave the same result as for axis #0:
        assertEquals(
            "[[Promotions].[Bag Stuffers], [Marital Status].[M]]",
            result.getAxes()[1].getPositions().get(0).toString());
        assertEquals(96, result.getAxes()[1].getPositions().size());
    }

    /**
     * <p>MONDRIAN-1061</p>
     * <p>The idea is that [recurse] is a calculated member uses
     * <br>
     * <code>CoalesceEmpty((Measures.[Unit Sales], [Time].CurrentMember ) ,
     * (Measures.[recurse],[Time].CurrentMember.PrevMember)))</code>
     * <br>
     *  ...calculation.
     * Food mart have no data for 1998 quarter,
     * So this way we expect:
     * <ul>
     * <li>not to fall into StackOverflow for recursive calculation when member
     * is referenced in VirtualCube.
     * <li>check that CoalesceEmpty calculated correctly (repeatable values from
     * previous not null result)
     * </ul></p>
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testVirtualCubeRecursiveMember(Context context) {
       /*
      final String schema = "<Schema name=\"FoodMart\">"
          + "<Dimension type=\"TimeDimension\" highCardinality=\"false\" name=\"Time\">"
          + "<Hierarchy visible=\"true\" hasAll=\"false\" primaryKey=\"time_id\">"
          + "<Table name=\"time_by_day\">"
          + "</Table>"
          + "<Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\" levelType=\"TimeYears\">"
          + "</Level>"
          + "<Level name=\"Quarter\" column=\"quarter\" type=\"String\" uniqueMembers=\"false\" levelType=\"TimeQuarters\">"
          + "</Level>"
          + "</Hierarchy>"
          + "</Dimension>"
          + "<Cube name=\"Sales\" visible=\"true\" defaultMeasure=\"Unit Sales\" >"
          + "<Table name=\"sales_fact_1997\">"
          + "<AggName name=\"agg_c_special_sales_fact_1997\">"
          + "<AggFactCount column=\"FACT_COUNT\">"
          + "</AggFactCount>"
          + "<AggMeasure column=\"UNIT_SALES_SUM\" name=\"[Measures].[Unit Sales]\">"
          + "</AggMeasure>"
          + "<AggLevel column=\"TIME_YEAR\" name=\"[Time].[Year]\">"
          + "</AggLevel>"
          + "</AggName>"
          + "</Table>"
          + "<DimensionUsage source=\"Time\" name=\"Time\" foreignKey=\"time_id\" highCardinality=\"false\">"
          + "</DimensionUsage>"
          + "<Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\">"
          + "</Measure>"
          + "<CalculatedMember name=\"recurse\" dimension=\"Measures\" visible=\"true\">"
          + "<Formula>"
          + "<![CDATA[(CoalesceEmpty((Measures.[Unit Sales], [Time].CurrentMember ) ,"
          + "(Measures.[recurse],[Time].CurrentMember.PrevMember)))]]>"
          + "</Formula>"
          + "</CalculatedMember>"
          + "</Cube>"
          + "<VirtualCube enabled=\"true\" name=\"Warehouse and Sales\" defaultMeasure=\"Store Sales\" visible=\"true\">"
          + "<VirtualCubeDimension visible=\"true\" highCardinality=\"false\" name=\"Time\">"
          + "</VirtualCubeDimension>"
          + "<VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[recurse]\" visible=\"true\">"
          + "</VirtualCubeMeasure>"
          + "</VirtualCube>"
          + "</Schema>";
      withSchema(context, schema);
        */
      withSchema(context, SchemaModifiers.VirtualCubeTestModifier3::new);
      final String query = "SELECT {[Time].[1998].Children} on columns,"
          + " {[recurse]} on rows "
          + "FROM [Warehouse and Sales]";
      final String expected = "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Time].[1998].[Q1]}\n"
        + "{[Time].[1998].[Q2]}\n"
        + "{[Time].[1998].[Q3]}\n"
        + "{[Time].[1998].[Q4]}\n"
        + "Axis #2:\n"
        + "{[Measures].[recurse]}\n"
        + "Row #0: 72,024\n"
        + "Row #0: 72,024\n"
        + "Row #0: 72,024\n"
        + "Row #0: 72,024\n";
      assertQueryReturns(context.getConnectionWithDefaultRole(), query, expected);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinOptimizerWithVirtualCube(Context context) {
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                "Warehouse and Sales",
                null,
                "<VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Customer Count]\"/>",
                null,
                null));
         */
        withSchema(context, SchemaModifiers.VirtualCubeTestModifier2::new);

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH member measures.ratio as 'measures.[Store Cost]/measures.[warehouse cost]' "
            + " member [marital status].agg as 'aggregate({[marital status].M})' "
            + " select non empty [Warehouse].[USA] "
            + " * {[marital status].[marital status].members, [marital status].agg }  on 0 "
            + "FROM [warehouse and sales] where [measures].[Customer Count]",
            "Axis #0:\n"
            + "{[Measures].[Customer Count]}\n"
            + "Axis #1:\n");
    }
}
