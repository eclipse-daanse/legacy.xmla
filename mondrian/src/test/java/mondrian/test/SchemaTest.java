/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/
package mondrian.test;

import static mondrian.enums.DatabaseProduct.MYSQL;
import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertEqualsVerbose;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertQueryThrows;
import static org.opencube.junit5.TestUtil.assertSimpleQuery;
import static org.opencube.junit5.TestUtil.checkThrowable;
import static org.opencube.junit5.TestUtil.executeQuery;
import static org.opencube.junit5.TestUtil.getDialect;
import static org.opencube.junit5.TestUtil.withSchema;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.connection.ConnectionProps;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.MetaData;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.element.Property;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.formatter.MemberPropertyFormatter;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Position;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.common.StandardProperty;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.olap.common.Util;
import  org.eclipse.daanse.olap.util.Bug;
import org.eclipse.daanse.rolap.api.RolapContext;
import org.eclipse.daanse.rolap.common.RolapConnection;
import org.eclipse.daanse.rolap.common.aggmatcher.AggTableManager;
import org.eclipse.daanse.rolap.mapping.api.model.AccessRoleMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CalculatedMemberMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.DimensionConnectorMapping;
import org.eclipse.daanse.rolap.mapping.api.model.HierarchyMapping;
import org.eclipse.daanse.rolap.mapping.api.model.MeasureGroupMapping;
import org.eclipse.daanse.rolap.mapping.api.model.NamedSetMapping;
import org.eclipse.daanse.rolap.mapping.api.model.PhysicalCubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessCatalog;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessCube;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessHierarchy;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessMember;
import org.eclipse.daanse.rolap.mapping.api.model.enums.ColumnDataType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.HideMemberIfType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.LevelType;
import org.eclipse.daanse.rolap.mapping.instance.rec.complex.foodmart.FoodmartMappingSupplier;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.AccessCatalogGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessCubeGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessHierarchyGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessMemberGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessRoleMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationColumnNameMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationExcludeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationForeignKeyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationLevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationNameMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AnnotationMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.CalculatedMemberMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.CalculatedMemberPropertyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.CatalogMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.CountMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.CubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DatabaseSchemaMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.ExplicitHierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.InlineTableMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.InlineTableQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.JoinQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.JoinedQueryElementMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MemberMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MemberPropertyFormatterMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MemberPropertyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.NamedSetMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.ParentChildHierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.ParentChildLinkMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalColumnMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalTableMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.RowMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.RowValueMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SQLExpressionMappingColumnImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SqlSelectQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SqlStatementMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SqlViewMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SumMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TimeDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.VirtualCubeMappingImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.apache.logging.log4j.spi.LoggerContext;

/**
 * Unit tests for various schema features.
 *
 *
 * @author jhyde
 * @since August 7, 2006
 */
class SchemaTest {

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

    private static final String CUBES_AB =
            "<Cube name=\"CubeA\" defaultMeasure=\"Unit Sales\">\n"
            + " <Table name=\"sales_fact_1997\" alias=\"TableAlias\">\n"
            + "   <!-- count=32 -->\n"
            + "   <SQL dialect=\"mysql\">\n"
            + "     `TableAlias`.`promotion_id` = 108\n"
            + "   </SQL>\n"
            + " </Table>\n"
            + " <DimensionUsage name=\"Store Type\" source=\"Store Type\" foreignKey=\"store_id\"/>\n"
            + " <Measure name=\"Customer Count\" column=\"customer_id\" aggregator=\"distinct-count\" formatString=\"#,###\"/>\n"
            + " <Measure name=\"Fantastic Count for Different Types of Promotion\" column=\"promotion_id\" aggregator=\"count\" formatString=\"Standard\"/>\n"
            + "</Cube>\n"
            + "<Cube name=\"CubeB\" defaultMeasure=\"Unit Sales\">\n"
            + " <Table name=\"sales_fact_1997\" alias=\"TableAlias\">\n"
            + "   <!-- count=22 -->\n"
            + "   <SQL dialect=\"mysql\">\n"
            + "     `TableAlias`.`promotion_id` = 112\n"
            + "   </SQL>\n"
            + " </Table>\n"
            + " <DimensionUsage name=\"Store Type\" source=\"Store Type\" foreignKey=\"store_id\"/>\n"
            + " <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" formatString=\"Standard\"/>\n"
            + " <Measure name=\"Fantastic Count for Different Types of Promotion\" column=\"promotion_id\" aggregator=\"count\" formatString=\"Standard\"/>\n"
            + "</Cube>";



    /**
     * Asserts that a list of exceptions (probably from
     * {@link Catalog#getWarnings()}) contains the expected
     * exception.
     *
     * @param exceptionList List of exceptions
     * @param expected Expected message
     */
    private void assertContains(
        List<Exception> exceptionList,
        String expected)
    {
        StringBuilder buf = new StringBuilder();
        for (Exception exception : exceptionList) {
            if (exception.getMessage().matches(expected)) {
                return;
            }
            if (buf.length() > 0) {
                buf.append(Util.NL);
            }
            buf.append(exception.getMessage());
        }
        fail(
            "Exception list did not contain expected exception '"
                + expected + "'. Exception list is:" + buf.toString());
    }

    // Tests follow...
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSolveOrderInCalculatedMember(Context<?> context) {
        class TestSolveOrderInCalculatedMemberModifier extends PojoMappingModifier{
            public TestSolveOrderInCalculatedMemberModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends CalculatedMemberMapping> cubeCalculatedMembers(CubeMapping cube) {
            	List<CalculatedMemberMapping> cm = new ArrayList<>();
            	cm.addAll(super.cubeCalculatedMembers(cube));
            	if ("Sales".equals(cube.getName())) {
                        CalculatedMemberPropertyMappingImpl cmProperty1 =
                        	CalculatedMemberPropertyMappingImpl.builder()
                            .withName("FORMAT_STRING")
                            .withValue("$#,##0.00")
                            .build();
                        CalculatedMemberMappingImpl calculatedMember1 =
                            CalculatedMemberMappingImpl.builder()
                                .withName("QuantumProfit")
                                //.withDimension("Measures")
                                .withFormula("[Measures].[Store Sales] / [Measures].[Store Cost]")
                                .withCalculatedMemberProperties(List.of(
                                    cmProperty1
                                ))
                                .build();

                        CalculatedMemberPropertyMappingImpl cmProperty21 =
                        		CalculatedMemberPropertyMappingImpl.builder()
                                .withName("FORMAT_STRING")
                                .withValue("$#,##0.00")
                                .build();
                        CalculatedMemberPropertyMappingImpl cmProperty22 =
                            CalculatedMemberPropertyMappingImpl.builder()
                                .withName("SOLVE_ORDER")
                                .withValue("2000")
                                .build();
                        CalculatedMemberMappingImpl calculatedMember2 =
                            CalculatedMemberMappingImpl.builder()
                                .withName("foo")
                                .withHierarchy((HierarchyMappingImpl) look(FoodmartMappingSupplier.genderHierarchy))
                                //.withDimension("Gender")
                                .withFormula("Sum(Gender.Members)")
                                .withCalculatedMemberProperties(List.of(
                                    cmProperty21, cmProperty22
                                ))
                                .build();
                        cm.add(calculatedMember1);
                        cm.add(calculatedMember2);
            	}
            	return cm;
            }

        }
        withSchema(context, TestSolveOrderInCalculatedMemberModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures].[QuantumProfit]} on 0, {(Gender.foo)} on 1 from sales",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[QuantumProfit]}\n"
            + "Axis #2:\n"
            + "{[Gender].[Gender].[foo]}\n"
            + "Row #0: $7.52\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHierarchyDefaultMember(Context<?> context) {
        class TestHierarchyDefaultMemberModifier extends PojoMappingModifier {
            public TestHierarchyDefaultMemberModifier(CatalogMapping catalog) {
                super(catalog);
            }

            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    LevelMappingImpl level = LevelMappingImpl
                        .builder()
                        .withName("Gender")
                        .withColumn(FoodmartMappingSupplier.GENDER_COLUMN_IN_CUSTOMER)
                        .withUniqueMembers(true)
                        .build();
                    HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                        .builder()
                        .withHasAll(true)
                        .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                        .withDefaultMember("[Gender with default].[All Gender with defaults].[M]")
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                        .withLevels(List.of(level))
                        .build();
                    DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl
                        .builder()
                        .withOverrideDimensionName("Gender with default")
                        .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        	.withName("Gender with default")
                        	.withHierarchies(List.of(hierarchy)).build())
                        .build();
                    result.add(dimension);
                }
                return result;
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "  <Dimension name=\"Gender with default\" foreignKey=\"customer_id\">\n"
            + "    <Hierarchy hasAll=\"true\" "
            + "primaryKey=\"customer_id\" "
            // Define a default member's whose unique name includes the
            // 'all' member.
            + "defaultMember=\"[Gender with default].[All Gender with defaults].[M]\" >\n"
            + "      <Table name=\"customer\"/>\n"
            + "      <Level name=\"Gender\" column=\"gender\" uniqueMembers=\"true\" />\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>"));
        */
        withSchema(context, TestHierarchyDefaultMemberModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Gender with default]} on columns from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Gender with default].[Gender with default].[M]}\n"
            + "Row #0: 135,215\n");
    }

    /**
     * Test case for the issue described in
     * <a href="http://forums.pentaho.com/showthread.php?p=190737">Pentaho
     * forum post 'wrong unique name for default member when hasAll=false'</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDefaultMemberName(Context<?> context) {
        class TestDefaultMemberNameModifier extends PojoMappingModifier {
            public TestDefaultMemberNameModifier(CatalogMapping catalog) {
                super(catalog);
            }

            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                	JoinQueryMappingImpl join = JoinQueryMappingImpl.builder()
            		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT)
            				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_TABLE).build())
            				.build())
            		.withRight(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT_CLASS)
            				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE).build())
            				.build())
            		.build();
                    LevelMappingImpl level1 = LevelMappingImpl
                        .builder()
                        .withName("Product Class")
                        .withNameColumn(FoodmartMappingSupplier.PRODUCT_SUBCATEGORY_COLUMN_IN_PRODUCT_CLASS)
                        .withColumn(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT_CLASS)
                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                        .withUniqueMembers(true)
                        .build();
                    LevelMappingImpl level2 = LevelMappingImpl
                        .builder()
                        .withName("Brand Name")
                        .withColumn(FoodmartMappingSupplier.BRAND_NAME_COLUMN_IN_PRODUCT)
                        .withUniqueMembers(false)
                        .build();
                    LevelMappingImpl level3 = LevelMappingImpl
                        .builder()
                        .withName("Product Name")
                        .withColumn(FoodmartMappingSupplier.PRODUCT_NAME_COLUMN_IN_PRODUCT)
                        .withUniqueMembers(true)
                        .build();
                    HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                        .builder()
                        .withHasAll(false)
                        .withPrimaryKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                        .withQuery(join)
                        .withLevels(List.of(level1, level2, level3))
                        .build();
                    DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl
                        .builder()
                        .withOverrideDimensionName("Product with no all")
                        .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        	.withName("Product with no all")
                        	.withHierarchies(List.of(hierarchy)).build())
                        .build();
                    result.add(dimension);
                }
                return result;
            }
        };
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "  <Dimension name=\"Product with no all\" foreignKey=\"product_id\">\n"
            + "    <Hierarchy hasAll=\"false\" primaryKey=\"product_id\" primaryKeyTable=\"product\">\n"
            + "      <Join leftKey=\"product_class_id\" rightKey=\"product_class_id\">\n"
            + "        <Table name=\"product\"/>\n"
            + "        <Table name=\"product_class\"/>\n"
            + "      </Join>\n"
            + "      <Level name=\"Product Class\" table=\"product_class\" nameColumn=\"product_subcategory\"\n"
            + "          column=\"product_class_id\" type=\"Numeric\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"Brand Name\" table=\"product\" column=\"brand_name\" uniqueMembers=\"false\"/>\n"
            + "      <Level name=\"Product Name\" table=\"product\" column=\"product_name\"\n"
            + "          uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"));
        */
        // note that default member name has no 'all' and has a name not an id
        withSchema(context, TestDefaultMemberNameModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Product with no all]} on columns from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Product with no all].[Product with no all].[Nuts]}\n"
            + "Row #0: 4,400\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHierarchyAbbreviatedDefaultMember(Context<?> context) {
        class TestHierarchyAbbreviatedDefaultMemberModifier extends PojoMappingModifier {
            public TestHierarchyAbbreviatedDefaultMemberModifier(CatalogMapping catalog) {
                super(catalog);
            }

            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    LevelMappingImpl level = LevelMappingImpl
                        .builder()
                        .withName("Gender")
                        .withColumn(FoodmartMappingSupplier.GENDER_COLUMN_IN_CUSTOMER)
                        .withUniqueMembers(true)
                        .build();
                    HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                        .builder()
                        .withHasAll(true)
                        .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                        .withDefaultMember("[Gender with default].[F]")
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                        .withLevels(List.of(level))
                        .build();
                    DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl
                        .builder()
                        .withOverrideDimensionName("Gender with default")
                        .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        	.withName("Gender with default")
                        	.withHierarchies(List.of(hierarchy)).build())
                        .build();
                    result.add(dimension);

                }
                return result;
            }
        };
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "  <Dimension name=\"Gender with default\" foreignKey=\"customer_id\">\n"
            + "    <Hierarchy hasAll=\"true\" "
            + "primaryKey=\"customer_id\" "
            // Default member unique name does not include 'All'.
            + "defaultMember=\"[Gender with default].[F]\" >\n"
            + "      <Table name=\"customer\"/>\n"
            + "      <Level name=\"Gender\" column=\"gender\" uniqueMembers=\"true\" />\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>"));
        */
        withSchema(context, TestHierarchyAbbreviatedDefaultMemberModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Gender with default]} on columns from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            // Note that the 'all' member is named according to the rule
            // '[<hierarchy>].[All <hierarchy>s]'.
            + "{[Gender with default].[Gender with default].[F]}\n"
            + "Row #0: 131,558\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHierarchyNoLevelsFails(Context<?> context) {
        class TestHierarchyNoLevelsFailsModifier extends PojoMappingModifier {
            public TestHierarchyNoLevelsFailsModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withHasAll(true)
                            .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                            .build();
                    DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl
                        .builder()
                        .withOverrideDimensionName("Gender no levels")
                        .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        	.withName("Gender no levels")
                        	.withHierarchies(List.of(hierarchy)).build())
                        .build();
                    result.add(dimension);

                }
                return result;
            }
        };
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "  <Dimension name='Gender no levels' foreignKey='customer_id'>\n"
            + "    <Hierarchy hasAll='true' primaryKey='customer_id'>\n"
            + "      <Table name='customer'/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>"));
        */
        withSchema(context, TestHierarchyNoLevelsFailsModifier::new);
        assertQueryThrows(context,
            "select {[Gender no levels]} on columns from [Sales]",
            "Hierarchy '[Gender no levels].[Gender no levels]' must have at least one level.");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHierarchyNonUniqueLevelsFails(Context<?> context) {
        class TestHierarchyNonUniqueLevelsFailsModifier extends PojoMappingModifier {
            public TestHierarchyNonUniqueLevelsFailsModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    LevelMappingImpl level1 = LevelMappingImpl
                            .builder()
                            .withName("Gender")
                            .withColumn(FoodmartMappingSupplier.GENDER_COLUMN_IN_CUSTOMER)
                            .withUniqueMembers(true)
                            .build();
                    LevelMappingImpl level2 = LevelMappingImpl
                            .builder()
                            .withName("Gender")
                            .withColumn(FoodmartMappingSupplier.GENDER_COLUMN_IN_CUSTOMER)
                            .withUniqueMembers(true)
                            .build();
                    HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withHasAll(true)
                            .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE ).build())
                            .withLevels(List.of(level1, level2))
                            .build();

                    DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl
                        .builder()
                        .withOverrideDimensionName("Gender dup levels")
                        .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        	.withName("Gender dup levels")
                        	.withHierarchies(List.of(hierarchy)).build())
                        .build();
                    result.add(dimension);

                }
                return result;
            }
        };
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "  <Dimension name='Gender dup levels' foreignKey='customer_id'>\n"
            + "    <Hierarchy hasAll='true' primaryKey='customer_id'>\n"
            + "      <Table name='customer'/>\n"
            + "      <Level name='Gender' column='gender' uniqueMembers='true' />\n"
            + "      <Level name='Gender' column='gender' uniqueMembers='true' />\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>"));
        */
        withSchema(context, TestHierarchyNonUniqueLevelsFailsModifier::new);
        assertQueryThrows(context,
            "select {[Gender dup levels]} on columns from [Sales]",
            "Level names within hierarchy '[Gender dup levels].[Gender dup levels]' are not unique; there is more than one level with name 'Gender'.");
    }

    /**
     * Tests a measure based on 'count'.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCountMeasure(Context<?> context) {
        class TestCountMeasureModifier extends PojoMappingModifier {
            public TestCountMeasureModifier(CatalogMapping catalog) {
                super(catalog);
            }
            protected List<? extends MeasureGroupMapping> physicalCubeMeasureGroups(PhysicalCubeMapping cube) {
                List<MeasureGroupMapping> result = new ArrayList<>();
                result.addAll(super.physicalCubeMeasureGroups(cube));
                if ("Sales".equals(cube.getName())) {
                    result.add(MeasureGroupMappingImpl.builder()
                    		.withMeasures(List.of(
                                    CountMeasureMappingImpl
                                    .builder()
                                    .withName("Fact Count")
                                    .build()
                    				))
                    		.build()
                    );
                }
                return result;
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            null,
            "<Measure name=\"Fact Count\" aggregator=\"count\"/>\n"));
        */
        withSchema(context, TestCountMeasureModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures].[Fact Count], [Measures].[Unit Sales]} on 0,\n"
            + "[Gender].members on 1\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Fact Count]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Gender].[Gender].[All Gender]}\n"
            + "{[Gender].[Gender].[F]}\n"
            + "{[Gender].[Gender].[M]}\n"
            + "Row #0: 86,837\n"
            + "Row #0: 266,773\n"
            + "Row #1: 42,831\n"
            + "Row #1: 131,558\n"
            + "Row #2: 44,006\n"
            + "Row #2: 135,215\n");
    }

    /**
     * Tests that an error occurs if a hierarchy is based on a non-existent
     * table.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHierarchyTableNotFound(Context<?> context) {
        class TestHierarchyTableNotFoundModifier extends PojoMappingModifier {
            public TestHierarchyTableNotFoundModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                	PhysicalColumnMappingImpl yearly_income = PhysicalColumnMappingImpl.builder().withName("yearly_income").withDataType(ColumnDataType.INTEGER).build();
                	PhysicalColumnMappingImpl customer_id = PhysicalColumnMappingImpl.builder().withName("customer_id").withDataType(ColumnDataType.INTEGER).build();
                    PhysicalTableMappingImpl customer_not_found = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("customer_not_found")
                            .withColumns(List.of(
                                    customer_id, yearly_income
                                    ))).build();
                    LevelMappingImpl level = LevelMappingImpl
                        .builder()
                        .withName("Yearly Income")
                        .withColumn(yearly_income)
                        .withUniqueMembers(true)
                        .build();
                    HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                        .builder()
                        .withHasAll(true)
                        .withPrimaryKey(customer_id)
                        .withQuery(TableQueryMappingImpl.builder().withTable(customer_not_found).build())
                        .withLevels(List.of(level))
                        .build();
                    DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl
                        .builder()
                        .withOverrideDimensionName("Yearly Income3")
                        .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        	.withName("Yearly Income3")
                        	.withHierarchies(List.of(hierarchy)).build())
                        .build();
                    result.add(dimension);
                }
                return result;
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "<Dimension name=\"Yearly Income3\" foreignKey=\"product_id\">\n"
            + "  <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
            + "    <Table name=\"customer_not_found\"/>\n"
            + "    <Level name=\"Yearly Income\" column=\"yearly_income\" uniqueMembers=\"true\"/>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>"));
        */
        // FIXME: This should validate the schema, and fail.
        withSchema(context, TestHierarchyTableNotFoundModifier::new);
        assertSimpleQuery(context.getConnectionWithDefaultRole());
        // FIXME: Should give better error.
        assertQueryThrows(context,
            "select [Yearly Income3].Children on 0 from [Sales]",
            "Internal error: while building member cache");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testPrimaryKeyTableNotFound(Context<?> context) {
        class TestPrimaryKeyTableNotFoundModifier extends PojoMappingModifier {
            public TestPrimaryKeyTableNotFoundModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    PhysicalColumnMappingImpl customer_id = PhysicalColumnMappingImpl.builder().withName("customer_id").withDataType(ColumnDataType.INTEGER).build();
                    PhysicalTableMappingImpl customer_not_found = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("customer_not_found")
                            .withColumns(List.of(
                                    customer_id
                                    ))).build();

                    LevelMappingImpl level = LevelMappingImpl
                            .builder()
                            .withName("Yearly Income")
                            .withColumn(FoodmartMappingSupplier.YEARLY_INCOME_COLUMN_IN_CUSTOMER)
                            .withUniqueMembers(true)
                            .build();
                    HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withHasAll(true)
                            .withPrimaryKey(customer_id)
                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                            .withLevels(List.of(level))
                            .build();
                    DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl
                            .builder()
                            .withOverrideDimensionName("Yearly Income4")
                            .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("Yearly Income4")
                            	.withHierarchies(List.of(hierarchy)).build())
                            .build();
                        result.add(dimension);
                    }
                    return result;
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "<Dimension name=\"Yearly Income4\" foreignKey=\"product_id\">\n"
            + "  <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\" primaryKeyTable=\"customer_not_found\">\n"
            + "    <Table name=\"customer\"/>\n"
            + "    <Level name=\"Yearly Income\" column=\"yearly_income\" uniqueMembers=\"true\"/>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>"));
        */
        withSchema(context, TestPrimaryKeyTableNotFoundModifier::new);
        assertQueryThrows(context,
            "select from [Sales]",
            "no table 'customer_not_found' found in hierarchy [Yearly Income4]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLevelTableNotFound(Context<?> context) {
        class TestLevelTableNotFoundModifier extends PojoMappingModifier {
            public TestLevelTableNotFoundModifier(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    PhysicalColumnMappingImpl yearly_income = PhysicalColumnMappingImpl.builder().withName("yearly_income").withDataType(ColumnDataType.INTEGER).build();
                    PhysicalTableMappingImpl customer_not_found = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("customer_not_found")
                            .withColumns(List.of(
                                    yearly_income
                                    ))).build();
                    LevelMappingImpl level = LevelMappingImpl
                            .builder()
                            .withName("Yearly Income")
                            .withColumn(yearly_income)
                            .withUniqueMembers(true)
                            .build();
                    HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withHasAll(true)
                            .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                            .withLevels(List.of(level))
                            .build();
                    DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl
                            .builder()
                            .withOverrideDimensionName("Yearly Income5")
                            .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("Yearly Income5")
                            	.withHierarchies(List.of(hierarchy)).build())
                            .build();
                        result.add(dimension);
                    }
                    return result;
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "<Dimension name=\"Yearly Income5\" foreignKey=\"product_id\">\n"
            + "  <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
            + "    <Table name=\"customer\"/>\n"
            + "    <Level name=\"Yearly Income\" table=\"customer_not_found\" column=\"yearly_income\" uniqueMembers=\"true\"/>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>"));

         */
        withSchema(context, TestLevelTableNotFoundModifier::new);
        assertQueryThrows(context,
            "select from [Sales]",
            "Table 'customer_not_found' not found");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHierarchyBadDefaultMember(Context<?> context) {
        class TestHierarchyBadDefaultMemberModifier extends PojoMappingModifier {
            public TestHierarchyBadDefaultMemberModifier(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    LevelMappingImpl level = LevelMappingImpl
                            .builder()
                            .withName("Gender")
                            .withColumn(FoodmartMappingSupplier.GENDER_COLUMN_IN_CUSTOMER)
                            .withUniqueMembers(true)
                            .build();
                    HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withHasAll(true)
                            .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                            .withDefaultMember("[Gender with default].[Non].[Existent]")
                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                            .withLevels(List.of(level))
                            .build();
                    DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl
                            .builder()
                            .withOverrideDimensionName("Gender with default")
                            .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("Gender with default")
                            	.withHierarchies(List.of(hierarchy)).build())
                            .build();
                        result.add(dimension);
                    }
                    return result;
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "  <Dimension name=\"Gender with default\" foreignKey=\"customer_id\">\n"
            + "    <Hierarchy hasAll=\"true\" "
            + "primaryKey=\"customer_id\" "
            // Default member unique name does not include 'All'.
            + "defaultMember=\"[Gender with default].[Non].[Existent]\" >\n"
            + "      <Table name=\"customer\"/>\n"
            + "      <Level name=\"Gender\" column=\"gender\" uniqueMembers=\"true\" />\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>"));
         */
        withSchema(context, TestHierarchyBadDefaultMemberModifier::new);
        assertQueryThrows(context,
            "select {[Gender with default]} on columns from [Sales]",
            "Can not find Default Member with name \"[Gender with default].[Non].[Existent]\" in Hierarchy \"Gender with default\"");
    }

    /**
     * WG: Note, this no longer throws an exception with the new RolapCubeMember
     * functionality.
     *
     * <p>Tests that an error is issued if two dimensions use the same table via
     * different drill-paths and do not use a different alias. If this error is
     * not issued, the generated SQL can be missing a join condition, as in
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-236">
     * Bug MONDRIAN-236, "Mondrian generates invalid SQL"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDuplicateTableAlias(Context<?> context) {
        class TestDuplicateTableAliasModifier extends PojoMappingModifier {
            public TestDuplicateTableAliasModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    LevelMappingImpl level = LevelMappingImpl
                            .builder()
                            .withName("Yearly Income")
                            .withColumn(FoodmartMappingSupplier.YEARLY_INCOME_COLUMN_IN_CUSTOMER)
                            .withUniqueMembers(true)
                            .build();
                    HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withHasAll(true)
                            .withPrimaryKey(FoodmartMappingSupplier.CITY_COLUMN_IN_CUSTOMER)
                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                            .withLevels(List.of(level))
                            .build();
                    DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl
                            .builder()
                            .withOverrideDimensionName("Yearly Income2")
                            .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("Yearly Income2")
                            	.withHierarchies(List.of(hierarchy)).build())
                            .build();
                        result.add(dimension);
                    }
                    return result;
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "<Dimension name=\"Yearly Income2\" foreignKey=\"product_id\">\n"
            + "  <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
            + "    <Table name=\"customer\"/>\n"
            + "    <Level name=\"Yearly Income\" column=\"yearly_income\" uniqueMembers=\"true\"/>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>"));
        */
        withSchema(context, TestDuplicateTableAliasModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Yearly Income2]} on columns, {[Measures].[Unit Sales]} on rows from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Yearly Income2].[Yearly Income2].[All Yearly Income2s]}\n"
            + "Axis #2:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Row #0: 266,773\n");
    }

    /**
     * This result is somewhat peculiar. If two dimensions share a foreign key,
     * what is the expected result?  Also, in this case, they share the same
     * table without an alias, and the system doesn't complain.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDuplicateTableAliasSameForeignKey(Context<?> context) {
        class TestDuplicateTableAliasSameForeignKeyModifier extends PojoMappingModifier {
            public TestDuplicateTableAliasSameForeignKeyModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    LevelMappingImpl level = LevelMappingImpl
                            .builder()
                            .withName("Yearly Income")
                            .withColumn(FoodmartMappingSupplier.YEARLY_INCOME_COLUMN_IN_CUSTOMER)
                            .withUniqueMembers(true)
                            .build();
                    HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withHasAll(true)
                            .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                            .withLevels(List.of(level))
                            .build();
                    DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl
                            .builder()
                            .withOverrideDimensionName("Yearly Income2")
                            .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("Yearly Income2")
                            	.withHierarchies(List.of(hierarchy)).build())
                            .build();
                        result.add(dimension);
                    }
                    return result;
            }

        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "<Dimension name=\"Yearly Income2\" foreignKey=\"customer_id\">\n"
            + "  <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
            + "    <Table name=\"customer\"/>\n"
            + "    <Level name=\"Yearly Income\" column=\"yearly_income\" uniqueMembers=\"true\"/>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>")); */
        withSchema(context, TestDuplicateTableAliasSameForeignKeyModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "266,773");

        // NonEmptyCrossJoin Fails
        if (false) {
            assertQueryReturns(context.getConnectionWithDefaultRole(),
                "select NonEmptyCrossJoin({[Yearly Income2].[All Yearly Income2s]},{[Customers].[All Customers]}) on rows,"
                + "NON EMPTY {[Measures].[Unit Sales]} on columns"
                + " from [Sales]",
                "Axis #0:\n"
                + "{}\n"
                + "266,773");
        }
    }

    /**
     * Tests two dimensions using same table (via different join paths).
     * Without the table alias, generates SQL which is missing a join condition.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionsShareTable(Context<?> context) {
        class TestDimensionsShareTableModifier extends PojoMappingModifier {
            public TestDimensionsShareTableModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    LevelMappingImpl level = LevelMappingImpl
                            .builder()
                            .withName("Yearly Income")
                            .withColumn(FoodmartMappingSupplier.YEARLY_INCOME_COLUMN_IN_CUSTOMER)
                            .withUniqueMembers(true)
                            .build();
                    HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withHasAll(true)
                            .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).withAlias("customerx").build())
                            .withLevels(List.of(level))
                            .build();
                    DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl
                            .builder()
                            .withOverrideDimensionName("Yearly Income2")
                            .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("Yearly Income2")
                            	.withHierarchies(List.of(hierarchy)).build())
                            .build();
                        result.add(dimension);
                    }
                    return result;
            }

        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "<Dimension name=\"Yearly Income2\" foreignKey=\"product_id\">\n"
            + "  <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
            + "    <Table name=\"customer\" alias=\"customerx\" />\n"
            + "    <Level name=\"Yearly Income\" column=\"yearly_income\" uniqueMembers=\"true\"/>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>"));
        */

        withSchema(context, TestDimensionsShareTableModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Yearly Income].[$10K - $30K]} on columns,"
            + "{[Yearly Income2].[$150K +]} on rows from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Yearly Income].[Yearly Income].[$10K - $30K]}\n"
            + "Axis #2:\n"
            + "{[Yearly Income2].[Yearly Income2].[$150K +]}\n"
            + "Row #0: 918\n");

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS,\n"
            + "NON EMPTY Crossjoin({[Yearly Income].[All Yearly Incomes].Children},\n"
            + "                     [Yearly Income2].[All Yearly Income2s].Children) ON ROWS\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Yearly Income].[Yearly Income].[$10K - $30K], [Yearly Income2].[Yearly Income2].[$10K - $30K]}\n"
            + "{[Yearly Income].[Yearly Income].[$10K - $30K], [Yearly Income2].[Yearly Income2].[$110K - $130K]}\n"
            + "{[Yearly Income].[Yearly Income].[$10K - $30K], [Yearly Income2].[Yearly Income2].[$130K - $150K]}\n"
            + "{[Yearly Income].[Yearly Income].[$10K - $30K], [Yearly Income2].[Yearly Income2].[$150K +]}\n"
            + "{[Yearly Income].[Yearly Income].[$10K - $30K], [Yearly Income2].[Yearly Income2].[$30K - $50K]}\n"
            + "{[Yearly Income].[Yearly Income].[$10K - $30K], [Yearly Income2].[Yearly Income2].[$50K - $70K]}\n"
            + "{[Yearly Income].[Yearly Income].[$10K - $30K], [Yearly Income2].[Yearly Income2].[$70K - $90K]}\n"
            + "{[Yearly Income].[Yearly Income].[$10K - $30K], [Yearly Income2].[Yearly Income2].[$90K - $110K]}\n"
            + "{[Yearly Income].[Yearly Income].[$110K - $130K], [Yearly Income2].[Yearly Income2].[$10K - $30K]}\n"
            + "{[Yearly Income].[Yearly Income].[$110K - $130K], [Yearly Income2].[Yearly Income2].[$110K - $130K]}\n"
            + "{[Yearly Income].[Yearly Income].[$110K - $130K], [Yearly Income2].[Yearly Income2].[$130K - $150K]}\n"
            + "{[Yearly Income].[Yearly Income].[$110K - $130K], [Yearly Income2].[Yearly Income2].[$150K +]}\n"
            + "{[Yearly Income].[Yearly Income].[$110K - $130K], [Yearly Income2].[Yearly Income2].[$30K - $50K]}\n"
            + "{[Yearly Income].[Yearly Income].[$110K - $130K], [Yearly Income2].[Yearly Income2].[$50K - $70K]}\n"
            + "{[Yearly Income].[Yearly Income].[$110K - $130K], [Yearly Income2].[Yearly Income2].[$70K - $90K]}\n"
            + "{[Yearly Income].[Yearly Income].[$110K - $130K], [Yearly Income2].[Yearly Income2].[$90K - $110K]}\n"
            + "{[Yearly Income].[Yearly Income].[$130K - $150K], [Yearly Income2].[Yearly Income2].[$10K - $30K]}\n"
            + "{[Yearly Income].[Yearly Income].[$130K - $150K], [Yearly Income2].[Yearly Income2].[$110K - $130K]}\n"
            + "{[Yearly Income].[Yearly Income].[$130K - $150K], [Yearly Income2].[Yearly Income2].[$130K - $150K]}\n"
            + "{[Yearly Income].[Yearly Income].[$130K - $150K], [Yearly Income2].[Yearly Income2].[$150K +]}\n"
            + "{[Yearly Income].[Yearly Income].[$130K - $150K], [Yearly Income2].[Yearly Income2].[$30K - $50K]}\n"
            + "{[Yearly Income].[Yearly Income].[$130K - $150K], [Yearly Income2].[Yearly Income2].[$50K - $70K]}\n"
            + "{[Yearly Income].[Yearly Income].[$130K - $150K], [Yearly Income2].[Yearly Income2].[$70K - $90K]}\n"
            + "{[Yearly Income].[Yearly Income].[$130K - $150K], [Yearly Income2].[Yearly Income2].[$90K - $110K]}\n"
            + "{[Yearly Income].[Yearly Income].[$150K +], [Yearly Income2].[Yearly Income2].[$10K - $30K]}\n"
            + "{[Yearly Income].[Yearly Income].[$150K +], [Yearly Income2].[Yearly Income2].[$110K - $130K]}\n"
            + "{[Yearly Income].[Yearly Income].[$150K +], [Yearly Income2].[Yearly Income2].[$130K - $150K]}\n"
            + "{[Yearly Income].[Yearly Income].[$150K +], [Yearly Income2].[Yearly Income2].[$150K +]}\n"
            + "{[Yearly Income].[Yearly Income].[$150K +], [Yearly Income2].[Yearly Income2].[$30K - $50K]}\n"
            + "{[Yearly Income].[Yearly Income].[$150K +], [Yearly Income2].[Yearly Income2].[$50K - $70K]}\n"
            + "{[Yearly Income].[Yearly Income].[$150K +], [Yearly Income2].[Yearly Income2].[$70K - $90K]}\n"
            + "{[Yearly Income].[Yearly Income].[$150K +], [Yearly Income2].[Yearly Income2].[$90K - $110K]}\n"
            + "{[Yearly Income].[Yearly Income].[$30K - $50K], [Yearly Income2].[Yearly Income2].[$10K - $30K]}\n"
            + "{[Yearly Income].[Yearly Income].[$30K - $50K], [Yearly Income2].[Yearly Income2].[$110K - $130K]}\n"
            + "{[Yearly Income].[Yearly Income].[$30K - $50K], [Yearly Income2].[Yearly Income2].[$130K - $150K]}\n"
            + "{[Yearly Income].[Yearly Income].[$30K - $50K], [Yearly Income2].[Yearly Income2].[$150K +]}\n"
            + "{[Yearly Income].[Yearly Income].[$30K - $50K], [Yearly Income2].[Yearly Income2].[$30K - $50K]}\n"
            + "{[Yearly Income].[Yearly Income].[$30K - $50K], [Yearly Income2].[Yearly Income2].[$50K - $70K]}\n"
            + "{[Yearly Income].[Yearly Income].[$30K - $50K], [Yearly Income2].[Yearly Income2].[$70K - $90K]}\n"
            + "{[Yearly Income].[Yearly Income].[$30K - $50K], [Yearly Income2].[Yearly Income2].[$90K - $110K]}\n"
            + "{[Yearly Income].[Yearly Income].[$50K - $70K], [Yearly Income2].[Yearly Income2].[$10K - $30K]}\n"
            + "{[Yearly Income].[Yearly Income].[$50K - $70K], [Yearly Income2].[Yearly Income2].[$110K - $130K]}\n"
            + "{[Yearly Income].[Yearly Income].[$50K - $70K], [Yearly Income2].[Yearly Income2].[$130K - $150K]}\n"
            + "{[Yearly Income].[Yearly Income].[$50K - $70K], [Yearly Income2].[Yearly Income2].[$150K +]}\n"
            + "{[Yearly Income].[Yearly Income].[$50K - $70K], [Yearly Income2].[Yearly Income2].[$30K - $50K]}\n"
            + "{[Yearly Income].[Yearly Income].[$50K - $70K], [Yearly Income2].[Yearly Income2].[$50K - $70K]}\n"
            + "{[Yearly Income].[Yearly Income].[$50K - $70K], [Yearly Income2].[Yearly Income2].[$70K - $90K]}\n"
            + "{[Yearly Income].[Yearly Income].[$50K - $70K], [Yearly Income2].[Yearly Income2].[$90K - $110K]}\n"
            + "{[Yearly Income].[Yearly Income].[$70K - $90K], [Yearly Income2].[Yearly Income2].[$10K - $30K]}\n"
            + "{[Yearly Income].[Yearly Income].[$70K - $90K], [Yearly Income2].[Yearly Income2].[$110K - $130K]}\n"
            + "{[Yearly Income].[Yearly Income].[$70K - $90K], [Yearly Income2].[Yearly Income2].[$130K - $150K]}\n"
            + "{[Yearly Income].[Yearly Income].[$70K - $90K], [Yearly Income2].[Yearly Income2].[$150K +]}\n"
            + "{[Yearly Income].[Yearly Income].[$70K - $90K], [Yearly Income2].[Yearly Income2].[$30K - $50K]}\n"
            + "{[Yearly Income].[Yearly Income].[$70K - $90K], [Yearly Income2].[Yearly Income2].[$50K - $70K]}\n"
            + "{[Yearly Income].[Yearly Income].[$70K - $90K], [Yearly Income2].[Yearly Income2].[$70K - $90K]}\n"
            + "{[Yearly Income].[Yearly Income].[$70K - $90K], [Yearly Income2].[Yearly Income2].[$90K - $110K]}\n"
            + "{[Yearly Income].[Yearly Income].[$90K - $110K], [Yearly Income2].[Yearly Income2].[$10K - $30K]}\n"
            + "{[Yearly Income].[Yearly Income].[$90K - $110K], [Yearly Income2].[Yearly Income2].[$110K - $130K]}\n"
            + "{[Yearly Income].[Yearly Income].[$90K - $110K], [Yearly Income2].[Yearly Income2].[$130K - $150K]}\n"
            + "{[Yearly Income].[Yearly Income].[$90K - $110K], [Yearly Income2].[Yearly Income2].[$150K +]}\n"
            + "{[Yearly Income].[Yearly Income].[$90K - $110K], [Yearly Income2].[Yearly Income2].[$30K - $50K]}\n"
            + "{[Yearly Income].[Yearly Income].[$90K - $110K], [Yearly Income2].[Yearly Income2].[$50K - $70K]}\n"
            + "{[Yearly Income].[Yearly Income].[$90K - $110K], [Yearly Income2].[Yearly Income2].[$70K - $90K]}\n"
            + "{[Yearly Income].[Yearly Income].[$90K - $110K], [Yearly Income2].[Yearly Income2].[$90K - $110K]}\n"
            + "Row #0: 12,824\n"
            + "Row #1: 2,822\n"
            + "Row #2: 2,933\n"
            + "Row #3: 918\n"
            + "Row #4: 18,381\n"
            + "Row #5: 10,436\n"
            + "Row #6: 6,777\n"
            + "Row #7: 2,859\n"
            + "Row #8: 2,432\n"
            + "Row #9: 532\n"
            + "Row #10: 566\n"
            + "Row #11: 177\n"
            + "Row #12: 3,877\n"
            + "Row #13: 2,131\n"
            + "Row #14: 1,319\n"
            + "Row #15: 527\n"
            + "Row #16: 3,331\n"
            + "Row #17: 643\n"
            + "Row #18: 703\n"
            + "Row #19: 187\n"
            + "Row #20: 4,497\n"
            + "Row #21: 2,629\n"
            + "Row #22: 1,681\n"
            + "Row #23: 721\n"
            + "Row #24: 1,123\n"
            + "Row #25: 224\n"
            + "Row #26: 257\n"
            + "Row #27: 109\n"
            + "Row #28: 1,924\n"
            + "Row #29: 1,026\n"
            + "Row #30: 675\n"
            + "Row #31: 291\n"
            + "Row #32: 19,067\n"
            + "Row #33: 4,078\n"
            + "Row #34: 4,235\n"
            + "Row #35: 1,569\n"
            + "Row #36: 28,160\n"
            + "Row #37: 15,368\n"
            + "Row #38: 10,329\n"
            + "Row #39: 4,504\n"
            + "Row #40: 9,708\n"
            + "Row #41: 2,353\n"
            + "Row #42: 2,243\n"
            + "Row #43: 748\n"
            + "Row #44: 14,469\n"
            + "Row #45: 7,966\n"
            + "Row #46: 5,272\n"
            + "Row #47: 2,208\n"
            + "Row #48: 7,320\n"
            + "Row #49: 1,630\n"
            + "Row #50: 1,602\n"
            + "Row #51: 541\n"
            + "Row #52: 10,550\n"
            + "Row #53: 5,843\n"
            + "Row #54: 3,997\n"
            + "Row #55: 1,562\n"
            + "Row #56: 2,722\n"
            + "Row #57: 597\n"
            + "Row #58: 568\n"
            + "Row #59: 193\n"
            + "Row #60: 3,800\n"
            + "Row #61: 2,192\n"
            + "Row #62: 1,324\n"
            + "Row #63: 523\n");
    }

    /**
     * Tests two dimensions using same table (via different join paths).
     * native non empty cross join sql generation returns empty query.
     * note that this works when native cross join is disabled
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionsShareTableNativeNonEmptyCrossJoin(Context<?> context) {
        class TestDimensionsShareTableNativeNonEmptyCrossJoinModifier extends PojoMappingModifier {
            public TestDimensionsShareTableNativeNonEmptyCrossJoinModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    LevelMappingImpl level = LevelMappingImpl
                            .builder()
                            .withName("Yearly Income")
                            .withColumn(FoodmartMappingSupplier.YEARLY_INCOME_COLUMN_IN_CUSTOMER)
                            .withUniqueMembers(true)
                            .build();
                    HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withHasAll(true)
                            .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).withAlias("customerx").build())
                            .withLevels(List.of(level))
                            .build();
                    DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl
                            .builder()
                            .withOverrideDimensionName("Yearly Income2")
                            .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("Yearly Income2")
                            	.withHierarchies(List.of(hierarchy)).build())
                            .build();
                        result.add(dimension);
                    }
                    return result;
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "<Dimension name=\"Yearly Income2\" foreignKey=\"product_id\">\n"
            + "  <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
            + "    <Table name=\"customer\" alias=\"customerx\" />\n"
            + "    <Level name=\"Yearly Income\" column=\"yearly_income\" uniqueMembers=\"true\"/>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>"));
        */
        withSchema(context, TestDimensionsShareTableNativeNonEmptyCrossJoinModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select NonEmptyCrossJoin({[Yearly Income2].[All Yearly Income2s]},{[Customers].[All Customers]}) on rows,"
            + "NON EMPTY {[Measures].[Unit Sales]} on columns"
            + " from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Yearly Income2].[Yearly Income2].[All Yearly Income2s], [Customers].[Customers].[All Customers]}\n"
            + "Row #0: 266,773\n");
    }

    /**
     * Tests two dimensions using same table with same foreign key
     * one table uses an alias.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionsShareTableSameForeignKeys(Context<?> context) {
        class TestDimensionsShareTableSameForeignKeysModifier extends PojoMappingModifier {
            public TestDimensionsShareTableSameForeignKeysModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    LevelMappingImpl level = LevelMappingImpl
                            .builder()
                            .withName("Yearly Income")
                            .withColumn(FoodmartMappingSupplier.YEARLY_INCOME_COLUMN_IN_CUSTOMER)
                            .withUniqueMembers(true)
                            .build();
                    HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withHasAll(true)
                            .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).withAlias("customerx").build())
                            .withLevels(List.of(level))
                            .build();
                    DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl
                            .builder()
                            .withOverrideDimensionName("Yearly Income2")
                            .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("Yearly Income2")
                            	.withHierarchies(List.of(hierarchy)).build())
                            .build();
                        result.add(dimension);
                    }
                    return result;
            }

        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "<Dimension name=\"Yearly Income2\" foreignKey=\"customer_id\">\n"
            + "  <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
            + "    <Table name=\"customer\" alias=\"customerx\" />\n"
            + "    <Level name=\"Yearly Income\" column=\"yearly_income\" uniqueMembers=\"true\"/>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>"));
        */
        withSchema(context, TestDimensionsShareTableSameForeignKeysModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Yearly Income].[$10K - $30K]} on columns,"
            + "{[Yearly Income2].[$150K +]} on rows from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Yearly Income].[Yearly Income].[$10K - $30K]}\n"
            + "Axis #2:\n"
            + "{[Yearly Income2].[Yearly Income2].[$150K +]}\n"
            + "Row #0: \n");

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS,\n"
            + "NON EMPTY Crossjoin({[Yearly Income].[All Yearly Incomes].Children},\n"
            + "                     [Yearly Income2].[All Yearly Income2s].Children) ON ROWS\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Yearly Income].[Yearly Income].[$10K - $30K], [Yearly Income2].[Yearly Income2].[$10K - $30K]}\n"
            + "{[Yearly Income].[Yearly Income].[$110K - $130K], [Yearly Income2].[Yearly Income2].[$110K - $130K]}\n"
            + "{[Yearly Income].[Yearly Income].[$130K - $150K], [Yearly Income2].[Yearly Income2].[$130K - $150K]}\n"
            + "{[Yearly Income].[Yearly Income].[$150K +], [Yearly Income2].[Yearly Income2].[$150K +]}\n"
            + "{[Yearly Income].[Yearly Income].[$30K - $50K], [Yearly Income2].[Yearly Income2].[$30K - $50K]}\n"
            + "{[Yearly Income].[Yearly Income].[$50K - $70K], [Yearly Income2].[Yearly Income2].[$50K - $70K]}\n"
            + "{[Yearly Income].[Yearly Income].[$70K - $90K], [Yearly Income2].[Yearly Income2].[$70K - $90K]}\n"
            + "{[Yearly Income].[Yearly Income].[$90K - $110K], [Yearly Income2].[Yearly Income2].[$90K - $110K]}\n"
            + "Row #0: 57,950\n"
            + "Row #1: 11,561\n"
            + "Row #2: 14,392\n"
            + "Row #3: 5,629\n"
            + "Row #4: 87,310\n"
            + "Row #5: 44,967\n"
            + "Row #6: 33,045\n"
            + "Row #7: 11,919\n");
    }

    /**
     * test hierarchy with completely different join path to fact table than
     * first hierarchy. tables are auto-aliased as necessary to guarantee
     * unique joins to the fact table.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSnowflakeHierarchyValidationNotNeeded(Context<?> context) {
        // this test breaks when using aggregates at the moment
        // due to a known limitation
        if ((context.getConfigValue(ConfigConstants.READ_AGGREGATES, ConfigConstants.READ_AGGREGATES_DEFAULT_VALUE ,Boolean.class)
             || context.getConfigValue(ConfigConstants.USE_AGGREGATES, ConfigConstants.USE_AGGREGATES_DEFAULT_VALUE ,Boolean.class))
            && !Bug.BugMondrian361Fixed)
        {
            return;
        }
        class TestSnowflakeHierarchyValidationNotNeededModifier extends PojoMappingModifier{
            public TestSnowflakeHierarchyValidationNotNeededModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                PhysicalColumnMappingImpl sales_region = PhysicalColumnMappingImpl.builder().withName("sales_region").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(30).build();
                PhysicalColumnMappingImpl sales_district_id = PhysicalColumnMappingImpl.builder().withName("sales_district_id").withDataType(ColumnDataType.INTEGER).build();
                PhysicalColumnMappingImpl region_id = PhysicalColumnMappingImpl.builder().withName("region_id").withDataType(ColumnDataType.INTEGER).build();
                PhysicalTableMappingImpl region = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("region")
                        .withColumns(List.of(
                                sales_region, sales_district_id, region_id
                                ))).build();
            	JoinQueryMappingImpl j1 = JoinQueryMappingImpl.builder()
        		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(sales_district_id)
        				.withQuery(TableQueryMappingImpl.builder().withTable(region).build())
        				.build())
        		.withRight(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_PROMOTION)
        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PROMOTION_TABLE).build())
        				.build())
        		.build();

            	JoinQueryMappingImpl join11 = JoinQueryMappingImpl.builder()
        		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.REGION_ID_COLUMN_IN_STORE)
        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
        				.build())
        		.withRight(JoinedQueryElementMappingImpl.builder().withKey(region_id)
        				.withQuery(j1)
        				.build())
        		.build();

            	JoinQueryMappingImpl join12 = JoinQueryMappingImpl.builder()
        		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.CUSTOMER_REGION_ID_COLUMN_IN_CUSTOMER)
        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
        				.build())
        		.withRight(JoinedQueryElementMappingImpl.builder().withKey(region_id)
        				.withQuery(TableQueryMappingImpl.builder().withTable(region).build())
        				.build())
        		.build();

            	JoinQueryMappingImpl join21 = JoinQueryMappingImpl.builder()
        		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.CUSTOMER_REGION_ID_COLUMN_IN_CUSTOMER)
        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
        				.build())
        		.withRight(JoinedQueryElementMappingImpl.builder().withKey(region_id)
        				.withQuery(TableQueryMappingImpl.builder().withTable(region).build())
        				.build())
        		.build();


                LevelMappingImpl l11 = LevelMappingImpl
                    .builder()
                    .withName("Store Country")
                    .withColumn(FoodmartMappingSupplier.STORE_COUNTRY_COLUMN_IN_STORE)
                    .build();
                LevelMappingImpl l12 = LevelMappingImpl
                    .builder()
                    .withName("Store Region")
                    .withColumn(sales_region)
                    .build();
                LevelMappingImpl l13 = LevelMappingImpl
                    .builder()
                    .withName("Store Name")
                    .withColumn(FoodmartMappingSupplier.STORE_NAME_COLUMN_IN_STORE)
                    .build();
                LevelMappingImpl l21 = LevelMappingImpl
                    .builder()
                    .withName("Country")
                    .withColumn(FoodmartMappingSupplier.COUNTRY_COLUMN_IN_CUSTOMER)
                    .withUniqueMembers(true)
                    .build();
                LevelMappingImpl l22 = LevelMappingImpl
                    .builder()
                    .withName("Region")
                    .withColumn(sales_region)
                    .withUniqueMembers(true)
                    .build();
                LevelMappingImpl l23 = LevelMappingImpl
                    .builder()
                    .withName("City")
                    .withColumn(FoodmartMappingSupplier.CITY_COLUMN_IN_CUSTOMER)
                    .withUniqueMembers(false)
                    .build();
                LevelMappingImpl l24 = LevelMappingImpl
                    .builder()
                    .withName("Name")
                    .withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                    .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                    .withUniqueMembers(true)
                    .build();

                LevelMappingImpl l31 = LevelMappingImpl
                    .builder()
                    .withName("Country")
                    .withColumn(FoodmartMappingSupplier.COUNTRY_COLUMN_IN_CUSTOMER)
                    .withUniqueMembers(true)
                    .build();
                LevelMappingImpl l32 = LevelMappingImpl
                    .builder()
                    .withName("Region")
                    .withColumn(sales_region)
                    .withUniqueMembers(true)
                    .build();
                LevelMappingImpl l33 = LevelMappingImpl
                    .builder()
                    .withName("City")
                    .withColumn(FoodmartMappingSupplier.CITY_COLUMN_IN_CUSTOMER)
                    .withUniqueMembers(true)
                    .build();
                LevelMappingImpl l34 = LevelMappingImpl
                    .builder()
                    .withName("Name")
                    .withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                    .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                    .withUniqueMembers(true)
                    .build();

                HierarchyMappingImpl h11 = ExplicitHierarchyMappingImpl
                    .builder()
                    .withHasAll(true)
                    .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                    .withQuery(join11)
                    .withLevels(List.of(l11, l12, l13))
                    .build();
                HierarchyMappingImpl h12 = ExplicitHierarchyMappingImpl
                    .builder()
                    .withName("MyHierarchy")
                    .withHasAll(true)
                    .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                    .withQuery(join12)
                    .withLevels(List.of(l21, l22, l23, l24))
                    .build();

                HierarchyMappingImpl h21 = ExplicitHierarchyMappingImpl
                    .builder()
                    .withHasAll(true)
                    .withAllMemberName("All Customers")
                    .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                    .withQuery(join21)
                    .withLevels(List.of(l31, l32, l33, l34))
                    .build();

                DimensionConnectorMappingImpl dimension1 = DimensionConnectorMappingImpl
                    .builder()
                    .withOverrideDimensionName("Store")
                    .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                    .withDimension(StandardDimensionMappingImpl.builder()
                    	.withName("Store")
                    	.withHierarchies(List.of(h11, h12)).build())
                    .build();

                DimensionConnectorMappingImpl dimension2 = DimensionConnectorMappingImpl
                    .builder()
                    .withOverrideDimensionName("Customers")
                    .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                    .withDimension(StandardDimensionMappingImpl.builder()
                        .withName("Customers")
                        .withHierarchies(List.of(h21)).build())
                    .build();

                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("AliasedDimensionsTesting")
                    //.withDefaultMeasure("Supply Time") //TODO Supply Time absent
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(dimension1, dimension2))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                    		.withMeasures(List.of(
                                SumMeasureMappingImpl
                                    .builder()
                                    .withName("Unit Sales")
                                    .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                    .withFormatString("Standard")
                                    .build()))
                    		.build()))
                    .build();
                result.add(c);
                result.addAll(super.cubes(cubes));
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"AliasedDimensionsTesting\" defaultMeasure=\"Supply Time\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <Dimension name=\"Store\" foreignKey=\"store_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKeyTable=\"store\" primaryKey=\"store_id\">\n"
            + "      <Join leftKey=\"region_id\" rightKey=\"region_id\">\n"
            + "        <Table name=\"store\"/>\n"
            + "        <Join leftKey=\"sales_district_id\" rightKey=\"promotion_id\">\n"
            + "          <Table name=\"region\"/>\n"
            + "          <Table name=\"promotion\"/>\n"
            + "        </Join>\n"
            + "      </Join>\n"
            + "      <Level name=\"Store Country\" table=\"store\" column=\"store_country\"/>\n"
            + "      <Level name=\"Store Region\" table=\"region\" column=\"sales_region\" />\n"
            + "      <Level name=\"Store Name\" table=\"store\" column=\"store_name\" />\n"
            + "    </Hierarchy>\n"
            + "    <Hierarchy name=\"MyHierarchy\" hasAll=\"true\" primaryKeyTable=\"customer\" primaryKey=\"customer_id\">\n"
            + "      <Join leftKey=\"customer_region_id\" rightKey=\"region_id\">\n"
            + "        <Table name=\"customer\"/>\n"
            + "        <Table name=\"region\"/>\n"
            + "      </Join>\n"
            + "      <Level name=\"Country\" table=\"customer\" column=\"country\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"Region\" table=\"region\" column=\"sales_region\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"City\" table=\"customer\" column=\"city\" uniqueMembers=\"false\"/>\n"
            + "      <Level name=\"Name\" table=\"customer\" column=\"customer_id\" type=\"Numeric\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Dimension name=\"Customers\" foreignKey=\"customer_id\">\n"
            + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Customers\" primaryKeyTable=\"customer\" primaryKey=\"customer_id\">\n"
            + "      <Join leftKey=\"customer_region_id\" rightKey=\"region_id\">\n"
            + "        <Table name=\"customer\"/>\n"
            + "        <Table name=\"region\"/>\n"
            + "      </Join>\n"
            + "      <Level name=\"Country\" table=\"customer\" column=\"country\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"Region\" table=\"region\" column=\"sales_region\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"City\" table=\"customer\" column=\"city\" uniqueMembers=\"false\"/>\n"
            + "      <Level name=\"Name\" table=\"customer\" column=\"customer_id\" type=\"Numeric\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "<Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" formatString=\"Standard\"/>\n"
            + "</Cube>",
            null,
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestSnowflakeHierarchyValidationNotNeededModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select  {[Store].[MyHierarchy].[Mexico]} on rows,"
            + "{[Customers].[Customers].[USA].[South West]} on columns"
            + " from "
            + "AliasedDimensionsTesting",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Customers].[Customers].[USA].[South West]}\n"
            + "Axis #2:\n"
            + "{[Store].[MyHierarchy].[Mexico]}\n"
            + "Row #0: 51,298\n");
    }

    /**
     * test hierarchy with slightly different join path to fact table than
     * first hierarchy. tables from first and second hierarchy should contain
     * the same join aliases to the fact table.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSnowflakeHierarchyValidationNotNeeded2(Context<?> context) {
        class TestSnowflakeHierarchyValidationNotNeeded2Modifier extends PojoMappingModifier {
            public TestSnowflakeHierarchyValidationNotNeeded2Modifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                PhysicalColumnMappingImpl sales_region = PhysicalColumnMappingImpl.builder().withName("sales_region").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(30).build();
                PhysicalColumnMappingImpl sales_district_id = PhysicalColumnMappingImpl.builder().withName("sales_district_id").withDataType(ColumnDataType.INTEGER).build();
                PhysicalColumnMappingImpl region_id = PhysicalColumnMappingImpl.builder().withName("region_id").withDataType(ColumnDataType.INTEGER).build();
                PhysicalTableMappingImpl region = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("region")
                        .withColumns(List.of(
                                sales_region, sales_district_id, region_id
                                ))).build();
            	JoinQueryMappingImpl j1 = JoinQueryMappingImpl.builder()
        		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(sales_district_id)
        				.withQuery(TableQueryMappingImpl.builder().withTable(region).build())
        				.build())
        		.withRight(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_PROMOTION)
        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PROMOTION_TABLE).build())
        				.build())
        		.build();

            	JoinQueryMappingImpl join11 = JoinQueryMappingImpl.builder()
        		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.REGION_ID_COLUMN_IN_STORE)
        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
        				.build())
        		.withRight(JoinedQueryElementMappingImpl.builder().withKey(region_id)
        				.withQuery(j1)
        				.build())
        		.build();

            	JoinQueryMappingImpl join12 = JoinQueryMappingImpl.builder()
        		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.REGION_ID_COLUMN_IN_STORE)
        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
        				.build())
        		.withRight(JoinedQueryElementMappingImpl.builder().withKey(region_id)
        				.withQuery(TableQueryMappingImpl.builder().withTable(region).build())
        				.build())
        		.build();

            	JoinQueryMappingImpl join21 = JoinQueryMappingImpl.builder()
        		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.CUSTOMER_REGION_ID_COLUMN_IN_CUSTOMER)
        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
        				.build())
        		.withRight(JoinedQueryElementMappingImpl.builder().withKey(region_id)
        				.withQuery(TableQueryMappingImpl.builder().withTable(region).build())
        				.build())
        		.build();

                LevelMappingImpl l11 = LevelMappingImpl
                    .builder()
                    .withName("Store Country")
                    .withColumn(FoodmartMappingSupplier.STORE_CITY_COLUMN_IN_STORE)
                    .build();
                LevelMappingImpl l12 = LevelMappingImpl
                    .builder()
                    .withName("Store Region")
                    .withColumn(sales_region)
                    .build();
                LevelMappingImpl l13 = LevelMappingImpl
                    .builder()
                    .withName("Store Name")
                    .withColumn(FoodmartMappingSupplier.STORE_NAME_COLUMN_IN_STORE)
                    .build();
                LevelMappingImpl l21 = LevelMappingImpl
                    .builder()
                    .withName("Store Country")
                    .withColumn(FoodmartMappingSupplier.STORE_COUNTRY_COLUMN_IN_STORE)
                    .build();

                LevelMappingImpl l22 = LevelMappingImpl
                    .builder()
                    .withName("Store Region")
                    .withColumn(sales_region)
                    .build();

                LevelMappingImpl l23 = LevelMappingImpl
                    .builder()
                    .withName("Store Name")
                    .withColumn(FoodmartMappingSupplier.STORE_NAME_COLUMN_IN_STORE)
                    .build();

                LevelMappingImpl l31 = LevelMappingImpl
                    .builder()
                    .withName("Country")
                    .withColumn(FoodmartMappingSupplier.COUNTRY_COLUMN_IN_CUSTOMER)
                    .withUniqueMembers(true)
                    .build();
                LevelMappingImpl l32 = LevelMappingImpl
                    .builder()
                    .withName("Region")
                    .withColumn(sales_region)
                    .withUniqueMembers(true)
                    .build();
                LevelMappingImpl l33 = LevelMappingImpl
                    .builder()
                    .withName("City")
                    .withColumn(FoodmartMappingSupplier.CITY_COLUMN_IN_CUSTOMER)
                    .withUniqueMembers(false)
                    .build();

                LevelMappingImpl l34 = LevelMappingImpl
                    .builder()
                    .withName("Name")
                    .withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                    .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                    .withUniqueMembers(true)
                    .build();
                HierarchyMappingImpl h11 = ExplicitHierarchyMappingImpl
                    .builder()
                    .withHasAll(true)
                    .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                    .withQuery(join11)
                    .withLevels(List.of(l11, l12, l13))
                    .build();
                HierarchyMappingImpl h12 = ExplicitHierarchyMappingImpl
                    .builder()
                    .withName("MyHierarchy")
                    .withHasAll(true)
                    .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                    .withQuery(join12)
                    .withLevels(List.of(l21, l22, l23))
                    .build();

                HierarchyMappingImpl h21 = ExplicitHierarchyMappingImpl
                    .builder()
                    .withHasAll(true)
                    .withAllMemberName("All Customers")
                    .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                    .withQuery(join21)
                    .withLevels(List.of(l31, l32, l33, l34))
                    .build();

                DimensionConnectorMappingImpl dimension1 = DimensionConnectorMappingImpl
                    .builder()
                    .withOverrideDimensionName("Store")
                    .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                    .withDimension(StandardDimensionMappingImpl.builder()
                    	.withName("Store")
                    	.withHierarchies(List.of(h11, h12)).build())
                    .build();

                DimensionConnectorMappingImpl dimension2 = DimensionConnectorMappingImpl
                    .builder()
                    .withOverrideDimensionName("Customers")
                    .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                    .withDimension(StandardDimensionMappingImpl.builder()
                        .withName("Customers")
                        .withHierarchies(List.of(h21)).build())
                    .build();

                /*

                MappingCube c = CubeRBuilder
                    .builder()
                    .name("AliasedDimensionsTesting")
                    .defaultMeasure("Supply Time")
                    .fact(new TableR(
                        null,
                        "sales_fact_1997",
                        null,
                        null,
                        null,
                        List.of(AggExcludeRBuilder.builder().pattern("agg_lc_06_sales_fact_1997").build()),
                        null))
                    .dimensionUsageOrDimensions(List.of(dimension1, dimension2))
                    .measures(List.of(
                        MeasureRBuilder
                            .builder()
                            .name("Unit Sales")
                            .column("unit_sales")
                            .aggregator("sum")
                            .formatString("Standard")
                            .build()))
                    .build();

 */

                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("AliasedDimensionsTesting")
                    //.withDefaultMeasure("Supply Time") //TODO Supply Time absent
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
                    		.withAggregationExcludes(List.of(
                    			AggregationExcludeMappingImpl.builder().withPattern("agg_lc_06_sales_fact_1997").build()
                    		))
                    		.build())
                    .withDimensionConnectors(List.of(dimension1, dimension2))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                    		.withMeasures(List.of(
                                SumMeasureMappingImpl
                                    .builder()
                                    .withName("Unit Sales")
                                    .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                    .withFormatString("Standard")
                                    .build()))
                    		.build()))
                    .build();
                result.add(c);
                result.addAll(super.cubes(cubes));
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"AliasedDimensionsTesting\" defaultMeasure=\"Supply Time\">\n"
            + "  <Table name=\"sales_fact_1997\">\n"
            + "    <AggExclude pattern=\"agg_lc_06_sales_fact_1997\"/>\n"
            + "  </Table>"
            + "  <Dimension name=\"Store\" foreignKey=\"store_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKeyTable=\"store\" primaryKey=\"store_id\">\n"
            + "      <Join leftKey=\"region_id\" rightKey=\"region_id\">\n"
            + "        <Table name=\"store\"/>\n"
            + "        <Join leftKey=\"sales_district_id\" rightKey=\"promotion_id\">\n"
            + "          <Table name=\"region\"/>\n"
            + "          <Table name=\"promotion\"/>\n"
            + "        </Join>\n"
            + "      </Join>\n"
            + "      <Level name=\"Store Country\" table=\"store\" column=\"store_country\"/>\n"
            + "      <Level name=\"Store Region\" table=\"region\" column=\"sales_region\" />\n"
            + "      <Level name=\"Store Name\" table=\"store\" column=\"store_name\" />\n"
            + "    </Hierarchy>\n"
            + "    <Hierarchy name=\"MyHierarchy\" hasAll=\"true\" primaryKeyTable=\"store\" primaryKey=\"store_id\">\n"
            + "      <Join leftKey=\"region_id\" rightKey=\"region_id\">\n"
            + "        <Table name=\"store\"/>\n"
            + "        <Table name=\"region\"/>\n"
            + "      </Join>\n"
            + "      <Level name=\"Store Country\" table=\"store\" column=\"store_country\"/>\n"
            + "      <Level name=\"Store Region\" table=\"region\" column=\"sales_region\" />\n"
            + "      <Level name=\"Store Name\" table=\"store\" column=\"store_name\" />\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Dimension name=\"Customers\" foreignKey=\"customer_id\">\n"
            + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Customers\" primaryKeyTable=\"customer\" primaryKey=\"customer_id\">\n"
            + "    <Join leftKey=\"customer_region_id\" rightKey=\"region_id\">\n"
            + "      <Table name=\"customer\"/>\n"
            + "      <Table name=\"region\"/>\n"
            + "    </Join>\n"
            + "    <Level name=\"Country\" table=\"customer\" column=\"country\" uniqueMembers=\"true\"/>\n"
            + "    <Level name=\"Region\" table=\"region\" column=\"sales_region\" uniqueMembers=\"true\"/>\n"
            + "    <Level name=\"City\" table=\"customer\" column=\"city\" uniqueMembers=\"false\"/>\n"
            + "    <Level name=\"Name\" table=\"customer\" column=\"customer_id\" type=\"Numeric\" uniqueMembers=\"true\"/>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>\n"
            + "<Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" formatString=\"Standard\"/>\n"
            + "</Cube>",
            null,
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestSnowflakeHierarchyValidationNotNeeded2Modifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select  {[Store].[MyHierarchy].[USA].[South West]} on rows,"
            + "{[Customers].[Customers].[USA].[South West]} on columns"
            + " from "
            + "AliasedDimensionsTesting",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Customers].[Customers].[USA].[South West]}\n"
            + "Axis #2:\n"
            + "{[Store].[MyHierarchy].[USA].[South West]}\n"
            + "Row #0: 72,631\n");
    }

    /**
     * WG: This no longer throws an exception, it is now possible
     *
     * Tests two dimensions using same table (via different join paths).
     * both using a table alias.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionsShareJoinTable(Context<?> context) {
        class TestDimensionsShareJoinTableModifier extends PojoMappingModifier {
            public TestDimensionsShareJoinTableModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                PhysicalColumnMappingImpl region_id = PhysicalColumnMappingImpl.builder().withName("region_id").withDataType(ColumnDataType.INTEGER).build();
                PhysicalColumnMappingImpl sales_region = PhysicalColumnMappingImpl.builder().withName("sales_region").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(30).build();
                PhysicalTableMappingImpl region = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("region")
                        .withColumns(List.of(
                                region_id, sales_region
                                ))).build();
            	JoinQueryMappingImpl join1 = JoinQueryMappingImpl.builder()
        		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.REGION_ID_COLUMN_IN_STORE)
        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
        				.build())
        		.withRight(JoinedQueryElementMappingImpl.builder().withKey(region_id)
        				.withQuery(TableQueryMappingImpl.builder().withTable(region).build())
        				.build())
        		.build();
            	LevelMappingImpl l11 = LevelMappingImpl
                        .builder()
                        .withName("Store Country")
                        .withColumn(FoodmartMappingSupplier.STORE_COUNTRY_COLUMN_IN_STORE)
                        .withUniqueMembers(true)
                        .build();
            	LevelMappingImpl l12 = LevelMappingImpl
                        .builder()
                        .withName("Store Region")
                        .withColumn(sales_region)
                        .withUniqueMembers(true)
                        .build();
            	LevelMappingImpl l13 = LevelMappingImpl
                        .builder()
                        .withName("Store Name")
                        .withColumn(FoodmartMappingSupplier.STORE_NAME_COLUMN_IN_STORE)
                        .withUniqueMembers(true)
                        .build();

                HierarchyMappingImpl h1 = ExplicitHierarchyMappingImpl
                        .builder()
                        .withHasAll(true)
                        .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                        .withQuery(join1)
                        .withLevels(List.of(l11, l12, l13))
                        .build();

            	JoinQueryMappingImpl join2 = JoinQueryMappingImpl.builder()
        		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.CUSTOMER_REGION_ID_COLUMN_IN_CUSTOMER)
        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
        				.build())
        		.withRight(JoinedQueryElementMappingImpl.builder().withKey(region_id)
        				.withQuery(TableQueryMappingImpl.builder().withTable(region).build())
        				.build())
        		.build();

            	LevelMappingImpl l21 = LevelMappingImpl
                        .builder()
                        .withName("Country")
                        .withColumn(FoodmartMappingSupplier.COUNTRY_COLUMN_IN_CUSTOMER)
                        .withUniqueMembers(true)
                        .build();
            	LevelMappingImpl l22 = LevelMappingImpl
                        .builder()
                        .withName("Region")
                        .withColumn(sales_region)
                        .withUniqueMembers(true)
                        .build();
            	LevelMappingImpl l23 = LevelMappingImpl
                        .builder()
                        .withName("City")
                        .withColumn(FoodmartMappingSupplier.CITY_COLUMN_IN_CUSTOMER)
                        .withUniqueMembers(false)
                        .build();
            	LevelMappingImpl l24 = LevelMappingImpl
                        .builder()
                        .withName("Name")
                        .withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                        .withUniqueMembers(true)
                        .build();

            	HierarchyMappingImpl h2 = ExplicitHierarchyMappingImpl
                        .builder()
                        .withHasAll(true)
                        .withAllMemberName("All Customers")
                        .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                        .withQuery(join2)
                        .withLevels(List.of(l21, l22, l23, l24))
                        .build();

                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                		.withOverrideDimensionName("Store")
                        .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        	.withName("Store")
                        	.withHierarchies(List.of(h1)).build())
                        .build();
                DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Customers")
                        .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        .withHierarchies(List.of(h2)).build())
                        .build();

                 PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                        .builder()
                        .withName("AliasedDimensionsTesting")
                        //.withDefaultMeasure("Supply Time") //TODO "Supply Time" absent
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
                        		.withAggregationExcludes(List.of(
                        			AggregationExcludeMappingImpl.builder().withPattern("agg_lc_06_sales_fact_1997").build()
                        		))
                        		.build())
                        .withDimensionConnectors(List.of(d1, d2))
                        .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder().withMeasures(
                        			List.of(
                                        SumMeasureMappingImpl
                                            .builder()
                                            .withName("Unit Sales")
                                            .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                            .withFormatString("Standard")
                                            .build(),
                                        SumMeasureMappingImpl
                                            .builder()
                                            .withName("Store Sales")
                                            .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_SALES_FACT_1997)
                                            .withFormatString("#,###.00")
                                            .build()
                                    )
                        	).build()
                        ))
                        .build();
                    result.add(c);
                    result.addAll(super.cubes(cubes));
                    return result;
                }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"AliasedDimensionsTesting\" defaultMeasure=\"Supply Time\">\n"
            + "  <Table name=\"sales_fact_1997\">\n"
            + "    <AggExclude pattern=\"agg_lc_06_sales_fact_1997\"/>\n"
            + "  </Table>"
            + "<Dimension name=\"Store\" foreignKey=\"store_id\">\n"

            + "<Hierarchy hasAll=\"true\" primaryKeyTable=\"store\" primaryKey=\"store_id\">\n"
            + "    <Join leftKey=\"region_id\" rightKey=\"region_id\">\n"
            + "      <Table name=\"store\"/>\n"
            + "      <Table name=\"region\"/>\n"
            + "    </Join>\n"
            + " <Level name=\"Store Country\" table=\"store\"  column=\"store_country\" uniqueMembers=\"true\"/>\n"
            + " <Level name=\"Store Region\"  table=\"region\" column=\"sales_region\"  uniqueMembers=\"true\"/>\n"
            + " <Level name=\"Store Name\"    table=\"store\"  column=\"store_name\"    uniqueMembers=\"true\"/>\n"
            + "</Hierarchy>\n"
            + "</Dimension>\n"
            + "<Dimension name=\"Customers\" foreignKey=\"customer_id\">\n"
            + "<Hierarchy hasAll=\"true\" allMemberName=\"All Customers\" primaryKeyTable=\"customer\" primaryKey=\"customer_id\">\n"
            + "    <Join leftKey=\"customer_region_id\" rightKey=\"region_id\">\n"
            + "      <Table name=\"customer\"/>\n"
            + "      <Table name=\"region\"/>\n"
            + "    </Join>\n"
            + "  <Level name=\"Country\" table=\"customer\" column=\"country\"                      uniqueMembers=\"true\"/>\n"
            + "  <Level name=\"Region\"  table=\"region\"   column=\"sales_region\"                 uniqueMembers=\"true\"/>\n"
            + "  <Level name=\"City\"    table=\"customer\" column=\"city\"                         uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Name\"    table=\"customer\" column=\"customer_id\" type=\"Numeric\" uniqueMembers=\"true\"/>\n"
            + "</Hierarchy>\n"
            + "</Dimension>\n"
            + "<Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" formatString=\"Standard\"/>\n"
            + "<Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\" formatString=\"#,###.00\"/>\n"
            + "</Cube>",
            null,
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestDimensionsShareJoinTableModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select  {[Store].[USA].[South West]} on rows,"
            + "{[Customers].[USA].[South West]} on columns"
            + " from "
            + "AliasedDimensionsTesting",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Customers].[Customers].[USA].[South West]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[South West]}\n"
            + "Row #0: 72,631\n");
    }

    /**
     * Tests two dimensions using same table (via different join paths).
     * both using a table alias.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    @DisabledIfSystemProperty(named = "test.disable.knownFails", matches = "true")
    //NOTE : test have issue with alias and Level. we have hierarchy with inner join .
    //Left join have alias with "customer_region" . Level of  hierarchy use table (reference) without alias with table name "region".
    //After that we have exception that "region" not exist (we should use alias as name of table in sql).
    void testDimensionsShareJoinTableOneAlias(Context<?> context) {
        class TestDimensionsShareJoinTableOneAliasModifier extends PojoMappingModifier {
            public TestDimensionsShareJoinTableOneAliasModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                PhysicalColumnMappingImpl region_id = PhysicalColumnMappingImpl.builder().withName("region_id").withDataType(ColumnDataType.INTEGER).build();
                PhysicalColumnMappingImpl sales_region = PhysicalColumnMappingImpl.builder().withName("sales_region").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(30).build();
                PhysicalTableMappingImpl region = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("region")
                        .withColumns(List.of(
                                region_id, sales_region
                                ))).build();
            	JoinQueryMappingImpl join1 = JoinQueryMappingImpl.builder()
        		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.REGION_ID_COLUMN_IN_STORE)
        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
        				.build())
        		.withRight(JoinedQueryElementMappingImpl.builder().withKey(region_id)
        				.withQuery(TableQueryMappingImpl.builder().withTable(region).build())
        				.build())
        		.build();

                LevelMappingImpl l11 = LevelMappingImpl
                    .builder()
                    .withName("Store Country")
                    .withColumn(FoodmartMappingSupplier.STORE_CITY_COLUMN_IN_STORE)
                    .withUniqueMembers(true)
                    .build();
                LevelMappingImpl l12 = LevelMappingImpl
                    .builder()
                    .withName("Store Region")
                    .withColumn(sales_region)
                    .withUniqueMembers(true)
                    .build();
                LevelMappingImpl l13 = LevelMappingImpl
                        .builder()
                        .withName("Store Name")
                        .withColumn(FoodmartMappingSupplier.STORE_NAME_COLUMN_IN_STORE)
                        .withUniqueMembers(true)
                        .build();

                HierarchyMappingImpl h1 = ExplicitHierarchyMappingImpl
                        .builder()
                        .withHasAll(true)
                        .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                        .withQuery(join1)
                        .withLevels(List.of(l11, l12, l13))
                        .build();

            	JoinQueryMappingImpl join2 = JoinQueryMappingImpl.builder()
        		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.CUSTOMER_REGION_ID_COLUMN_IN_CUSTOMER)
        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
        				.build())
        		.withRight(JoinedQueryElementMappingImpl.builder().withKey(region_id)
        				.withQuery(TableQueryMappingImpl.builder().withTable(region).withAlias("customer_region").build())
        				.build())
        		.build();

                LevelMappingImpl l21 = LevelMappingImpl
                        .builder()
                        .withName("Country")
                        .withColumn(FoodmartMappingSupplier.COUNTRY_COLUMN_IN_CUSTOMER)
                        .withUniqueMembers(true)
                        .build();
                LevelMappingImpl l22 = LevelMappingImpl
                        .builder()
                        .withName("Region")
                        //.withTable("customer_region") //TODO use alias as table
                        .withColumn(sales_region)
                        .withUniqueMembers(true)
                        .build();
                LevelMappingImpl l23 = LevelMappingImpl
                        .builder()
                        .withName("City")
                        .withColumn(FoodmartMappingSupplier.CITY_COLUMN_IN_CUSTOMER)
                        .withUniqueMembers(false)
                        .build();
                LevelMappingImpl l24 = LevelMappingImpl
                        .builder()
                        .withName("Name")
                        .withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                        .withUniqueMembers(true)
                        .build();

                HierarchyMappingImpl h2 = ExplicitHierarchyMappingImpl
                        .builder()
                        .withHasAll(true)
                        .withAllMemberName("All Customers")
                        .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                        .withQuery(join2)
                        .withLevels(List.of(l21, l22, l23, l24))
                        .build();

                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                		.withOverrideDimensionName("Store")
                        .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        	.withName("Store")
                        	.withHierarchies(List.of(h1)).build())
                        .build();

                DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Customers")
                        .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        	.withName("Customers")
                        	.withHierarchies(List.of(h2)).build())
                        .build();

                    PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                        .builder()
                        .withName("AliasedDimensionsTesting")
                        //.withDefaultMeasure("Supply Time") //TODO "Supply Time" absent
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
                        		.withAggregationExcludes(List.of(
                        			AggregationExcludeMappingImpl.builder().withPattern("agg_lc_06_sales_fact_1997").build()
                        		))
                        		.build())
                        .withDimensionConnectors(List.of(d1, d2))
                        .withMeasureGroups(List.of(
                            	MeasureGroupMappingImpl.builder().withMeasures(
                            			List.of(
                            SumMeasureMappingImpl
                                .builder()
                                .withName("Unit Sales")
                                .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                .withFormatString("Standard")
                                .build(),
                            SumMeasureMappingImpl
                                .builder()
                                .withName("Store Sales")
                                .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_SALES_FACT_1997)
                                .withFormatString("#,###.00")
                                .build())).build()
                        ))
                        .build();
                    result.add(c);
                    result.addAll(super.cubes(cubes));
                    return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"AliasedDimensionsTesting\" defaultMeasure=\"Supply Time\">\n"
            + "  <Table name=\"sales_fact_1997\">\n"
            + "    <AggExclude pattern=\"agg_lc_06_sales_fact_1997\"/>\n"
            + "  </Table>"
            + "<Dimension name=\"Store\" foreignKey=\"store_id\">\n"
            + "<Hierarchy hasAll=\"true\" primaryKeyTable=\"store\" primaryKey=\"store_id\">\n"
            + "    <Join leftKey=\"region_id\" rightKey=\"region_id\">\n"
            + "      <Table name=\"store\"/>\n"
            + "      <Table name=\"region\"/>\n"
            + "    </Join>\n"
            + " <Level name=\"Store Country\" table=\"store\"  column=\"store_country\" uniqueMembers=\"true\"/>\n"
            + " <Level name=\"Store Region\"  table=\"region\" column=\"sales_region\"  uniqueMembers=\"true\"/>\n"
            + " <Level name=\"Store Name\"    table=\"store\"  column=\"store_name\"    uniqueMembers=\"true\"/>\n"
            + "</Hierarchy>\n"
            + "</Dimension>\n"
            + "<Dimension name=\"Customers\" foreignKey=\"customer_id\">\n"
            + "<Hierarchy hasAll=\"true\" allMemberName=\"All Customers\" primaryKeyTable=\"customer\" primaryKey=\"customer_id\">\n"
            + "    <Join leftKey=\"customer_region_id\" rightKey=\"region_id\">\n"
            + "      <Table name=\"customer\"/>\n"
            + "      <Table name=\"region\" alias=\"customer_region\"/>\n"
            + "    </Join>\n"
            + "  <Level name=\"Country\" table=\"customer\" column=\"country\"                      uniqueMembers=\"true\"/>\n"
            + "  <Level name=\"Region\"  table=\"customer_region\"   column=\"sales_region\"                 uniqueMembers=\"true\"/>\n"
            + "  <Level name=\"City\"    table=\"customer\" column=\"city\"                         uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Name\"    table=\"customer\" column=\"customer_id\" type=\"Numeric\" uniqueMembers=\"true\"/>\n"
            + "</Hierarchy>\n"
            + "</Dimension>\n"
            + "<Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" formatString=\"Standard\"/>\n"
            + "<Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\" formatString=\"#,###.00\"/>\n"
            + "</Cube>",
            null,
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestDimensionsShareJoinTableOneAliasModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select  {[Store].[USA].[South West]} on rows,"
            + "{[Customers].[USA].[South West]} on columns"
            + " from "
            + "AliasedDimensionsTesting",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Customers].[USA].[South West]}\n"
            + "Axis #2:\n"
            + "{[Store].[USA].[South West]}\n"
            + "Row #0: 72,631\n");
    }

    /**
     * Tests two dimensions using same table (via different join paths).
     * both using a table alias.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    @DisabledIfSystemProperty(named = "test.disable.knownFails", matches = "true")
    //NOTE : test have issue with alias and Level. we have hierarchy with inner join .
    //Left join have alias with "store_region" . Level of  hierarchy use table (reference) without alias with table name "region".
    //After that we have exception that "region" not exist (we should use alias as name of table in sql).
    void testDimensionsShareJoinTableTwoAliases(Context<?> context) {
        class TestDimensionsShareJoinTableTwoAliasesModifier extends PojoMappingModifier {
            public TestDimensionsShareJoinTableTwoAliasesModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                PhysicalColumnMappingImpl region_id = PhysicalColumnMappingImpl.builder().withName("region_id").withDataType(ColumnDataType.INTEGER).build();
                PhysicalColumnMappingImpl sales_region = PhysicalColumnMappingImpl.builder().withName("sales_region").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(30).build();
                PhysicalTableMappingImpl region = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("region")
                        .withColumns(List.of(
                                region_id, sales_region
                                ))).build();
            	JoinQueryMappingImpl join1 = JoinQueryMappingImpl.builder()
        		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.REGION_ID_COLUMN_IN_STORE)
        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
        				.build())
        		.withRight(JoinedQueryElementMappingImpl.builder().withKey(region_id)
        				.withQuery(TableQueryMappingImpl.builder().withTable(region).withAlias("store_region").build())
        				.build())
        		.build();
            	LevelMappingImpl l11 = LevelMappingImpl
                        .builder()
                        .withName("Store Country")
                        .withColumn(FoodmartMappingSupplier.STORE_COUNTRY_COLUMN_IN_STORE)
                        .withUniqueMembers(true)
                        .build();
            	LevelMappingImpl l12 = LevelMappingImpl
                        .builder()
                        .withName("Store Region")
                        //.withTable("store_region") //TODO use alias as table
                        .withColumn(sales_region)
                        .withUniqueMembers(true)
                        .build();
            	LevelMappingImpl l13 = LevelMappingImpl
                        .builder()
                        .withName("Store Name")
                        .withColumn(FoodmartMappingSupplier.STORE_NAME_COLUMN_IN_STORE)
                        .withUniqueMembers(true)
                        .build();

                HierarchyMappingImpl h1 = ExplicitHierarchyMappingImpl
                        .builder()
                        .withHasAll(true)
                        .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                        .withQuery(join1)
                        .withLevels(List.of(l11, l12, l13))
                        .build();

            	JoinQueryMappingImpl join2 = JoinQueryMappingImpl.builder()
        		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.CUSTOMER_REGION_ID_COLUMN_IN_CUSTOMER)
        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
        				.build())
        		.withRight(JoinedQueryElementMappingImpl.builder().withKey(region_id)
        				.withQuery(TableQueryMappingImpl.builder().withTable(region).withAlias("customer_region").build())
        				.build())
        		.build();
            	LevelMappingImpl l21 = LevelMappingImpl
                        .builder()
                        .withName("Country")
                        .withColumn(FoodmartMappingSupplier.COUNTRY_COLUMN_IN_CUSTOMER)
                        .withUniqueMembers(true)
                        .build();
            	LevelMappingImpl l22 = LevelMappingImpl
                        .builder()
                        .withName("Region")
                        //.withTable("customer_region") //TODO use alias as table
                        //TODO use alias as table
                        .withColumn(sales_region)
                        .withUniqueMembers(true)
                        .build();
            	LevelMappingImpl l23 = LevelMappingImpl
                        .builder()
                        .withName("City")
                        .withColumn(FoodmartMappingSupplier.CITY_COLUMN_IN_CUSTOMER)
                        .withUniqueMembers(false)
                        .build();
            	LevelMappingImpl l24 = LevelMappingImpl
                        .builder()
                        .withName("Name")
                        .withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                        .withUniqueMembers(true)
                        .build();

            	HierarchyMappingImpl h2 = ExplicitHierarchyMappingImpl
                        .builder()
                        .withHasAll(true)
                        .withAllMemberName("All Customers")
                        .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                        .withQuery(join2)
                        .withLevels(List.of(l21, l22, l23, l24))
                        .build();

                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                		.withOverrideDimensionName("Store")
                        .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        	.withName("Store")
                        	.withHierarchies(List.of(h1)).build())
                        .build();
                DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Customers")
                        .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        .withHierarchies(List.of(h2)).build())
                        .build();

                 PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                        .builder()
                        .withName("AliasedDimensionsTesting")
                        //.withDefaultMeasure("Supply Time") //TODO "Supply Time" absent
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
                        		.withAggregationExcludes(List.of(
                        			AggregationExcludeMappingImpl.builder().withPattern("agg_lc_06_sales_fact_1997").build()
                        		))
                        		.build())
                        .withDimensionConnectors(List.of(d1, d2))
                        .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder().withMeasures(
                        			List.of(
                                        SumMeasureMappingImpl
                                            .builder()
                                            .withName("Unit Sales")
                                            .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                            .withFormatString("Standard")
                                            .build(),
                                        SumMeasureMappingImpl
                                            .builder()
                                            .withName("Store Sales")
                                            .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_SALES_FACT_1997)
                                            .withFormatString("#,###.00")
                                            .build()
                                    )
                        	).build()
                        ))
                        .build();
                    result.add(c);
                    result.addAll(super.cubes(cubes));
                    return result;
                }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"AliasedDimensionsTesting\" defaultMeasure=\"Supply Time\">\n"
            + "  <Table name=\"sales_fact_1997\">\n"
            + "    <AggExclude pattern=\"agg_lc_06_sales_fact_1997\"/>\n"
            + "  </Table>"
            + "<Dimension name=\"Store\" foreignKey=\"store_id\">\n"
            + "<Hierarchy hasAll=\"true\" primaryKeyTable=\"store\" primaryKey=\"store_id\">\n"
            + "    <Join leftKey=\"region_id\" rightKey=\"region_id\">\n"
            + "      <Table name=\"store\"/>\n"
            + "      <Table name=\"region\" alias=\"store_region\"/>\n"
            + "    </Join>\n"
            + " <Level name=\"Store Country\" table=\"store\"  column=\"store_country\" uniqueMembers=\"true\"/>\n"
            + " <Level name=\"Store Region\"  table=\"store_region\" column=\"sales_region\"  uniqueMembers=\"true\"/>\n"
            + " <Level name=\"Store Name\"    table=\"store\"  column=\"store_name\"    uniqueMembers=\"true\"/>\n"
            + "</Hierarchy>\n"
            + "</Dimension>\n"
            + "<Dimension name=\"Customers\" foreignKey=\"customer_id\">\n"
            + "<Hierarchy hasAll=\"true\" allMemberName=\"All Customers\" primaryKeyTable=\"customer\" primaryKey=\"customer_id\">\n"
            + "    <Join leftKey=\"customer_region_id\" rightKey=\"region_id\">\n"
            + "      <Table name=\"customer\"/>\n"
            + "      <Table name=\"region\" alias=\"customer_region\"/>\n"
            + "    </Join>\n"
            + "  <Level name=\"Country\" table=\"customer\" column=\"country\"                      uniqueMembers=\"true\"/>\n"
            + "  <Level name=\"Region\"  table=\"customer_region\"   column=\"sales_region\"                 uniqueMembers=\"true\"/>\n"
            + "  <Level name=\"City\"    table=\"customer\" column=\"city\"                         uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Name\"    table=\"customer\" column=\"customer_id\" type=\"Numeric\" uniqueMembers=\"true\"/>\n"
            + "</Hierarchy>\n"
            + "</Dimension>\n"
            + "<Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" formatString=\"Standard\"/>\n"
            + "<Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\" formatString=\"#,###.00\"/>\n"
            + "</Cube>",
            null,
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestDimensionsShareJoinTableTwoAliasesModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select  {[Store].[USA].[South West]} on rows,"
            + "{[Customers].[USA].[South West]} on columns"
            + " from "
            + "AliasedDimensionsTesting",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Customers].[USA].[South West]}\n"
            + "Axis #2:\n"
            + "{[Store].[USA].[South West]}\n"
            + "Row #0: 72,631\n");
    }

    /**
     * Tests two dimensions using same table (via different join paths).
     * both using a table alias.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTwoAliasesDimensionsShareTable(Context<?> context) {
        class TestTwoAliasesDimensionsShareTableModifier extends PojoMappingModifier {
            public TestTwoAliasesDimensionsShareTableModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                PhysicalColumnMappingImpl region_id = PhysicalColumnMappingImpl.builder().withName("region_id").withDataType(ColumnDataType.INTEGER).build();
                PhysicalColumnMappingImpl sales_region = PhysicalColumnMappingImpl.builder().withName("sales_region").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(30).build();
                PhysicalTableMappingImpl region = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("region")
                        .withColumns(List.of(
                                region_id, sales_region
                                ))).build();
            	JoinQueryMappingImpl join1 = JoinQueryMappingImpl.builder()
        		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.REGION_ID_COLUMN_IN_STORE)
        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
        				.build())
        		.withRight(JoinedQueryElementMappingImpl.builder().withKey(region_id)
        				.withQuery(TableQueryMappingImpl.builder().withTable(region).withAlias("store_region").build())
        				.build())
        		.build();
            	LevelMappingImpl l11 = LevelMappingImpl
                        .builder()
                        .withName("Store Country")
                        .withColumn(FoodmartMappingSupplier.STORE_COUNTRY_COLUMN_IN_STORE)
                        .withUniqueMembers(true)
                        .build();
            	LevelMappingImpl l12 = LevelMappingImpl
                        .builder()
                        .withName("Store Name")
                        .withColumn(FoodmartMappingSupplier.STORE_NAME_COLUMN_IN_STORE)
                        .withUniqueMembers(true)
                        .build();

            	HierarchyMappingImpl h1 = ExplicitHierarchyMappingImpl
                        .builder()
                        .withHasAll(true)
                        //.withPrimaryKeyTable(FoodmartMappingSupplier.STORE_TABLE)
                        .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).withAlias("storea").build())
                        .withLevels(List.of(l11, l12))
                        .build();

            	LevelMappingImpl l21 = LevelMappingImpl
                        .builder()
                        .withName("Country")
                        .withColumn(FoodmartMappingSupplier.STORE_COUNTRY_COLUMN_IN_STORE)
                        .withUniqueMembers(true)
                        .build();
            	LevelMappingImpl l22 = LevelMappingImpl
                        .builder()
                        .withName("Store Name")
                        .withColumn(FoodmartMappingSupplier.STORE_NAME_COLUMN_IN_STORE)
                        .withUniqueMembers(true)
                        .build();

            	HierarchyMappingImpl h2 = ExplicitHierarchyMappingImpl
                        .builder()
                        .withHasAll(true)
                        .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).withAlias("storeb").build())
                        .withLevels(List.of(l21, l22))
                        .build();

                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                		.withOverrideDimensionName("StoreA")
                        .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        	.withName("StoreA")
                        	.withHierarchies(List.of(h1)).build())
                        .build();
                DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("StoreB")
                        .withForeignKey(FoodmartMappingSupplier.WAREHOUSE_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        	.withName("StoreB")
                        	.withHierarchies(List.of(h2)).build())
                        .build();

                SumMeasureMappingImpl stm = SumMeasureMappingImpl
                        .builder()
                        .withName("Supply Time")
                        .withColumn(FoodmartMappingSupplier.SUPPLY_TIME_COLUMN_IN_INVENTORY_FACKT_1997)

                        .build();

                 PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                        .builder()
                        .withName("AliasedDimensionsTesting")
                        .withDefaultMeasure(stm)
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.INVENTORY_FACKT_1997_TABLE)
                        		.build())
                        .withDimensionConnectors(List.of(d1, d2))
                        .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder().withMeasures(
                        			List.of(
                                        SumMeasureMappingImpl
                                            .builder()
                                            .withName("Store Invoice")
                                            .withColumn(FoodmartMappingSupplier.STORE_INVOICE_COLUMN_IN_INVENTORY_FACKT_1997)

                                            .build(),
                                        stm,
                                        SumMeasureMappingImpl
                                            .builder()
                                            .withName("Warehouse Cost")
                                            .withColumn(FoodmartMappingSupplier.WAREHOUSE_COST_COLUMN_IN_INVENTORY_FACKT_1997)

                                            .build()
                                    )
                        	).build()
                        ))
                        .build();
                    result.add(c);
                    result.addAll(super.cubes(cubes));
                    return result;
                }
         }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"AliasedDimensionsTesting\" defaultMeasure=\"Supply Time\">\n"
            + "  <Table name=\"inventory_fact_1997\"/>\n"
            + "  <Dimension name=\"StoreA\" foreignKey=\"store_id\">"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">"
            + "      <Table name=\"store\" alias=\"storea\"/>"
            + "      <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"/>"
            + "      <Level name=\"Store Name\"  column=\"store_name\" uniqueMembers=\"true\"/>"
            + "    </Hierarchy>"
            + "  </Dimension>"

            + "  <Dimension name=\"StoreB\" foreignKey=\"warehouse_id\">"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">"
            + "      <Table name=\"store\"  alias=\"storeb\"/>"
            + "      <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"/>"
            + "      <Level name=\"Store Name\" column=\"store_name\" uniqueMembers=\"true\"/>"
            + "    </Hierarchy>"
            + "  </Dimension>"
            + "  <Measure name=\"Store Invoice\" column=\"store_invoice\" "
            + "aggregator=\"sum\"/>\n"
            + "  <Measure name=\"Supply Time\" column=\"supply_time\" "
            + "aggregator=\"sum\"/>\n"
            + "  <Measure name=\"Warehouse Cost\" column=\"warehouse_cost\" "
            + "aggregator=\"sum\"/>\n"
            + "</Cube>",
            null,
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestTwoAliasesDimensionsShareTableModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[StoreA].[USA]} on rows,"
            + "{[StoreB].[USA]} on columns"
            + " from "
            + "AliasedDimensionsTesting",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[StoreB].[StoreB].[USA]}\n"
            + "Axis #2:\n"
            + "{[StoreA].[StoreA].[USA]}\n"
            + "Row #0: 10,425\n");
    }

    /**
     * Tests two dimensions using same table with same foreign key.
     * both using a table alias.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTwoAliasesDimensionsShareTableSameForeignKeys(Context<?> context) {
        class TestTwoAliasesDimensionsShareTableSameForeignKeysModifier extends PojoMappingModifier {
            public TestTwoAliasesDimensionsShareTableSameForeignKeysModifier(CatalogMapping catalog) {
                super(catalog);
            }

            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                LevelMappingImpl l11 = LevelMappingImpl
                        .builder()
                        .withName("Store Country")
                        .withColumn(FoodmartMappingSupplier.STORE_COUNTRY_COLUMN_IN_STORE )
                        .withUniqueMembers(true)
                        .build();
                LevelMappingImpl l12 = LevelMappingImpl
                        .builder()
                        .withName("Store Name")
                        .withColumn(FoodmartMappingSupplier.STORE_NAME_COLUMN_IN_STORE)
                        .withUniqueMembers(true)
                        .build();

                HierarchyMappingImpl h1 = ExplicitHierarchyMappingImpl
                        .builder()
                        .withHasAll(true)
                        .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE ).withAlias("storea").build())
                        .withLevels(List.of(l11, l12))
                        .build();

                LevelMappingImpl l21 = LevelMappingImpl
                        .builder()
                        .withName("Store Country")
                        .withColumn(FoodmartMappingSupplier.STORE_COUNTRY_COLUMN_IN_STORE)
                        .withUniqueMembers(true)
                        .build();
                LevelMappingImpl l22 = LevelMappingImpl
                        .builder()
                        .withName("Store Name")
                        .withColumn(FoodmartMappingSupplier.STORE_NAME_COLUMN_IN_STORE)
                        .withUniqueMembers(true)
                        .build();

                HierarchyMappingImpl h2 = ExplicitHierarchyMappingImpl
                        .builder()
                        .withHasAll(true)
                        .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).withAlias("storeb").build())
                        .withLevels(List.of(l21, l22))
                        .build();

                    DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("StoreA")
                        .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        	.withName("StoreA")
                        	.withHierarchies(List.of(h1)).build())
                        .build();

                    DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("StoreB")
                        .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("StoreB")
                        .withHierarchies(List.of(h2)).build())
                        .build();

                    SumMeasureMappingImpl stm = SumMeasureMappingImpl
                            .builder()
                            .withName("Supply Time")
                            .withColumn(FoodmartMappingSupplier.SUPPLY_TIME_COLUMN_IN_INVENTORY_FACKT_1997)

                            .build();

                    PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                        .builder()
                        .withName("AliasedDimensionsTesting")
                        .withDefaultMeasure(stm)
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.INVENTORY_FACKT_1997_TABLE).build())
                        .withDimensionConnectors(List.of(d1, d2))
                        .withMeasureGroups(List.of(
                            	MeasureGroupMappingImpl.builder().withMeasures(
                            			List.of(
                                            SumMeasureMappingImpl
                                                .builder()
                                                .withName("Store Invoice")
                                                .withColumn(FoodmartMappingSupplier.STORE_INVOICE_COLUMN_IN_INVENTORY_FACKT_1997)

                                                .build(),
                                            stm,
                                            SumMeasureMappingImpl
                                                .builder()
                                                .withName("Warehouse Cost")
                                                .withColumn(FoodmartMappingSupplier.WAREHOUSE_COST_COLUMN_IN_INVENTORY_FACKT_1997)

                                                .build()
                                        )
                            	).build()
                            ))
                        .build();
                    result.add(c);
                    result.addAll(super.cubes(cubes));
                    return result;

            }

        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"AliasedDimensionsTesting\" defaultMeasure=\"Supply Time\">\n"
            + "  <Table name=\"inventory_fact_1997\"/>\n"
            + "  <Dimension name=\"StoreA\" foreignKey=\"store_id\">"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">"
            + "      <Table name=\"store\" alias=\"storea\"/>"
            + "      <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"/>"
            + "      <Level name=\"Store Name\" column=\"store_name\" uniqueMembers=\"true\"/>"
            + "    </Hierarchy>"
            + "  </Dimension>"

            + "  <Dimension name=\"StoreB\" foreignKey=\"store_id\">"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">"
            + "      <Table name=\"store\"  alias=\"storeb\"/>"
            + "      <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"/>"
            + "      <Level name=\"Store Name\" column=\"store_name\" uniqueMembers=\"true\"/>"
            + "    </Hierarchy>"
            + "  </Dimension>"
            + "  <Measure name=\"Store Invoice\" column=\"store_invoice\" "
            + "aggregator=\"sum\"/>\n"
            + "  <Measure name=\"Supply Time\" column=\"supply_time\" "
            + "aggregator=\"sum\"/>\n"
            + "  <Measure name=\"Warehouse Cost\" column=\"warehouse_cost\" "
            + "aggregator=\"sum\"/>\n"
            + "</Cube>",
            null,
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestTwoAliasesDimensionsShareTableSameForeignKeysModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[StoreA].[USA]} on rows,"
            + "{[StoreB].[USA]} on columns"
            + " from "
            + "AliasedDimensionsTesting",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[StoreB].[StoreB].[USA]}\n"
            + "Axis #2:\n"
            + "{[StoreA].[StoreA].[USA]}\n"
            + "Row #0: 10,425\n");
    }

    /**
     * Test Multiple DimensionUsages on same Dimension.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMultipleDimensionUsages(Context<?> context) {
        class TestMultipleDimensionUsagesModifier extends PojoMappingModifier {
            public TestMultipleDimensionUsagesModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                		.withOverrideDimensionName("Time")
                		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                        .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                        .build();

                DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Time2")
                        .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                        .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                        .build();

                DimensionConnectorMappingImpl d3 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Store")
                        .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_STORE_WITH_QUERY_STORE))
                        .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                        .build();

                    PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                        .builder()
                        .withName("Sales Two Dimensions")
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
                        		.withAggregationExcludes(List.of(
                        			AggregationExcludeMappingImpl.builder().withName("agg_c_10_sales_fact_1997").build(),
                        			AggregationExcludeMappingImpl.builder().withName("agg_g_ms_pcat_sales_fact_1997").build()
                        		))
                        		.build())
                        .withDimensionConnectors(List.of(d1, d2, d3))
                        .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder()
                        	.withMeasures(List.of(
                                SumMeasureMappingImpl
                                    .builder()
                                    .withName("Unit Sales")
                                    .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                                    .withFormatString("Standard")
                                    .build(),
                                SumMeasureMappingImpl
                                    .builder()
                                    .withName("Store Cost")
                                    .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_SALES_FACT_1997)

                                    .withFormatString("#,###.00")
                                    .build()
                            ))
                        	.build()
                        ))
                        .build();
                    result.add(c);
                    return result;
            }

        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"Sales Two Dimensions\">\n"
            + "  <Table name=\"sales_fact_1997\">\n"
            + "    <AggExclude pattern=\"agg_c_10_sales_fact_1997\"/>\n"
            + "    <AggExclude pattern=\"agg_g_ms_pcat_sales_fact_1997\"/>\n"
            + "  </Table>\n"
            + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "  <DimensionUsage name=\"Time2\" source=\"Time\" foreignKey=\"product_id\"/>\n"
            + "  <DimensionUsage name=\"Store\" source=\"Store\" foreignKey=\"store_id\"/>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" "
            + "   formatString=\"Standard\"/>\n"
            + "  <Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\""
            + "   formatString=\"#,###.00\"/>\n"
            + "</Cube>", null, null, null, null);
        withSchema(context, schema);
         */
        withSchema(context, TestMultipleDimensionUsagesModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select\n"
            + " {[Time2].[1997]} on columns,\n"
            + " {[Time].[1997].[Q3]} on rows\n"
            + "From [Sales Two Dimensions]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Time2].[Time].[1997]}\n"
            + "Axis #2:\n"
            + "{[Time].[Time].[1997].[Q3]}\n"
            + "Row #0: 16,266\n");
    }

    /**
     * Test Multiple DimensionUsages on same Dimension.
     */
    @Disabled // Disabled caption: caption doesn't support now. this test will delete in future
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMultipleDimensionHierarchyCaptionUsages(Context<?> context) {
        class TestMultipleDimensionHierarchyCaptionUsagesModifier extends PojoMappingModifier {
            public TestMultipleDimensionHierarchyCaptionUsagesModifier(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                		.withOverrideDimensionName("Time")
                		//.caption("TimeOne")
                		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                        .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                        .build();

                DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Time2")
                        //.caption("TimeTwo")
                        .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                        .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                        .build();

                DimensionConnectorMappingImpl d3 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Store")
                        .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_STORE_WITH_QUERY_STORE))
                        .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                        .build();

                    PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                        .builder()
                        .withName("Sales Two Dimensions")
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
                        		.withAggregationExcludes(List.of(
                        			AggregationExcludeMappingImpl.builder().withName("agg_c_10_sales_fact_1997").build(),
                        			AggregationExcludeMappingImpl.builder().withName("agg_g_ms_pcat_sales_fact_1997").build()
                        		))
                        		.build())
                        .withDimensionConnectors(List.of(d1, d2, d3))
                        .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder()
                        	.withMeasures(List.of(
                                SumMeasureMappingImpl
                                    .builder()
                                    .withName("Unit Sales")
                                    .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                                    .withFormatString("Standard")
                                    .build(),
                                SumMeasureMappingImpl
                                    .builder()
                                    .withName("Store Cost")
                                    .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_SALES_FACT_1997)

                                    .withFormatString("#,###.00")
                                    .build()
                            ))
                        	.build()
                        ))
                        .build();
                    result.add(c);
                    return result;
            }

        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"Sales Two Dimensions\">\n"
            + "  <Table name=\"sales_fact_1997\">\n"
            + "    <AggExclude pattern=\"agg_c_10_sales_fact_1997\"/>\n"
            + "    <AggExclude pattern=\"agg_g_ms_pcat_sales_fact_1997\"/>\n"
            + "  </Table>\n"
            + "  <DimensionUsage name=\"Time\" caption=\"TimeOne\" source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "  <DimensionUsage name=\"Time2\" caption=\"TimeTwo\" source=\"Time\" foreignKey=\"product_id\"/>\n"
            + "  <DimensionUsage name=\"Store\" source=\"Store\" foreignKey=\"store_id\"/>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" "
            + "   formatString=\"Standard\"/>\n"
            + "  <Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\""
            + "   formatString=\"#,###.00\"/>\n"
            + "</Cube>", null, null, null, null);
        withSchema(context, schema);
         */
        withSchema(context, TestMultipleDimensionHierarchyCaptionUsagesModifier::new);
        String query =
            "select\n"
            + " {[Time2].[1997]} on columns,\n"
            + " {[Time].[1997].[Q3]} on rows\n"
            + "From [Sales Two Dimensions]";

        Result result = executeQuery(context.getConnectionWithDefaultRole(), query);

        // Time2.1997 Member
        Member member1 =
            result.getAxes()[0].getPositions().iterator().next().iterator()
                .next();

        // NOTE: The caption is modified at the dimension, not the hierarchy
        assertEquals(member1.getLevel().getDimension().getCaption(), "TimeTwo");

        Member member2 =
            result.getAxes()[1].getPositions().iterator().next().iterator()
                .next();
        assertEquals("TimeOne", member2.getLevel().getDimension().getCaption());
    }


    /**
     * This test verifies that the createDimension() API call is working
     * correctly.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionCreation(Context<?> context) {
        class TestDimensionCreationModifier extends PojoMappingModifier {
            public TestDimensionCreationModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Store")
                        .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_STORE_WITH_QUERY_STORE))
                        .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                        .build();

                DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                		.withOverrideDimensionName("Time")
                		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                        .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                        .build();

                    PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                        .builder()
                        .withName("Sales Create Dimension")
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
                        		.build())
                        .withDimensionConnectors(List.of(d1, d2))
                        .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder()
                        	.withMeasures(List.of(
                                SumMeasureMappingImpl
                                    .builder()
                                    .withName("Unit Sales")
                                    .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                                    .withFormatString("Standard")
                                    .build(),
                                SumMeasureMappingImpl
                                    .builder()
                                    .withName("Store Cost")
                                    .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_SALES_FACT_1997)

                                    .withFormatString("#,###.00")
                                    .build()
                            ))
                        	.build()
                        ))
                        .build();
                    result.add(c);
                    return result;
            }

        }
        withSchema(context, TestDimensionCreationModifier::new);

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select\n"
            + "NON EMPTY {[Store].[All Stores].children} on columns \n"
            + "From [Sales Create Dimension]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[Store].[USA]}\n"
            + "Row #0: 266,773\n");

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select\n"
            + "NON EMPTY {[Store].[All Stores].children} on columns, \n"
            + "{[Time].[1997].[Q1]} on rows \n"
            + "From [Sales Create Dimension]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[Store].[USA]}\n"
            + "Axis #2:\n"
            + "{[Time].[Time].[1997].[Q1]}\n"
            + "Row #0: 66,291\n");
    }

    /**
     * Test DimensionUsage level attribute
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionUsageLevel(Context<?> context) {
        class TestDimensionUsageLevelModifier extends PojoMappingModifier {
            public TestDimensionUsageLevelModifier(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Store")
                        .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_STORE_WITH_QUERY_STORE))
                        .withLevel(FoodmartMappingSupplier.LEVEL_STORE_STATE_UNIQUE_MEMBERS_TRUE)
                        .withForeignKey(FoodmartMappingSupplier.STATE_PROVINCE_COLUMN_IN_CUSTOMER)
                        .build();

                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("Customer Usage Level")
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE)
                    		.build())
                    .withDimensionConnectors(List.of(d1))
                    .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder()
                        	.withMeasures(List.of(
                        SumMeasureMappingImpl
                            .builder()
                            .withName("Cars")
                            .withColumn(FoodmartMappingSupplier.NUM_CARS_OWNED_COLUMN_IN_CUSTOMER)

                            .build(),
                        SumMeasureMappingImpl
                            .builder()
                            .withName("Children")
                            .withColumn(FoodmartMappingSupplier.TOTAL_CHILDREN_COLUMN_IN_CUSTOMER)

                            .build()
                    ))
                    .build()
                ))
                .build();
                result.add(c);
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,

            "<Cube name=\"Customer Usage Level\">\n"
            + "  <Table name=\"customer\"/>\n"
            // + alias=\"sales_fact_1997_multi\"/>\n"
            + "  <DimensionUsage name=\"Store\" source=\"Store\" level=\"Store State\" foreignKey=\"state_province\"/>\n"
            + "  <Measure name=\"Cars\" column=\"num_cars_owned\" aggregator=\"sum\"/>\n"
            + "  <Measure name=\"Children\" column=\"total_children\" aggregator=\"sum\"/>\n"
            + "</Cube>", null, null, null, null);
        withSchema(context, schema);
         */
        withSchema(context, TestDimensionUsageLevelModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select\n"
            + " {[Store].[Store State].members} on columns \n"
            + "From [Customer Usage Level]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[Store].[Canada].[BC]}\n"
            + "{[Store].[Store].[Mexico].[DF]}\n"
            + "{[Store].[Store].[Mexico].[Guerrero]}\n"
            + "{[Store].[Store].[Mexico].[Jalisco]}\n"
            + "{[Store].[Store].[Mexico].[Veracruz]}\n"
            + "{[Store].[Store].[Mexico].[Yucatan]}\n"
            + "{[Store].[Store].[Mexico].[Zacatecas]}\n"
            + "{[Store].[Store].[USA].[CA]}\n"
            + "{[Store].[Store].[USA].[OR]}\n"
            + "{[Store].[Store].[USA].[WA]}\n"
            + "Row #0: 7,700\n"
            + "Row #0: 1,492\n"
            + "Row #0: 228\n"
            + "Row #0: 206\n"
            + "Row #0: 195\n"
            + "Row #0: 229\n"
            + "Row #0: 1,209\n"
            + "Row #0: 46,965\n"
            + "Row #0: 4,686\n"
            + "Row #0: 32,767\n");

        // BC.children should return an empty list, considering that we've
        // joined Store at the State level.
        if (false) {
            assertQueryReturns(context.getConnectionWithDefaultRole(),
                "select\n"
                + " {[Store].[All Stores].[Canada].[BC].children} on columns \n"
                + "From [Customer Usage Level]",
                "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n");
        }
    }

    /**
     * Test to verify naming of all member with
     * dimension usage name is different then source name
     */
    @Disabled // Disabled caption: caption doesn't support now. this test will delete in future
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAllMemberMultipleDimensionUsages(Context<?> context) {
        class TestAllMemberMultipleDimensionUsagesModifier extends PojoMappingModifier {
            public TestAllMemberMultipleDimensionUsagesModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Store")
                        //.caption("First Store")
                        .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_STORE_WITH_QUERY_STORE))
                        .withForeignKey(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_SALES_FACT_1997)
                        .build();

                HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                        .builder()
                        .withName("Store")
                        .withHasAll(true)
                        .withAllMemberName("All Stores")
                        .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE_RAGGED)
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_RAGGED_TABLE).build())
                        .withLevels(List.of(FoodmartMappingSupplier.LEVEL_STORE_COUNTRY, FoodmartMappingSupplier.LEVEL_STORE_STATE_UNIQUE_MEMBERS_TRUE, FoodmartMappingSupplier.LEVEL_STORE_CYTY,
                        		FoodmartMappingSupplier.LEVEL_STORE_NAME_WITHOUT_TABLE))
                        .build();

                DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl
                        .builder()
                        .withOverrideDimensionName("Store2")
                        //.caption("Second Store")
                        .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        	.withName("Store2")
                        	.withHierarchies(List.of(hierarchy)).build())
                        .build();

                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("Sales Two Sales Dimensions")
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
                    		.build())
                    .withDimensionConnectors(List.of(d1, d2))
                    .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder()
                        	.withMeasures(List.of(
                        SumMeasureMappingImpl
                            .builder()
                            .withName("Unit Sales")
                            .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                            .withFormatString("Standard")
                            .build(),
                        SumMeasureMappingImpl
                            .builder()
                            .withName("Store Cost")
                            .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_SALES_FACT_1997)

                            .withFormatString("#,###.00")
                            .build()
                    ))
                    .build()
                ))
                .build();
                result.add(c);
                return result;
            }
        }


        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"Sales Two Sales Dimensions\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <DimensionUsage name=\"Store\" caption=\"First Store\" source=\"Store\" foreignKey=\"store_id\"/>\n"
            + "  <DimensionUsage name=\"Store2\" caption=\"Second Store\" source=\"Store\" foreignKey=\"product_id\"/>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" "
            + "   formatString=\"Standard\"/>\n"
            + "  <Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\""
            + "   formatString=\"#,###.00\"/>\n"
            + "</Cube>",
            null,
            null,
            null,
            null);
        withSchema(context, schema);
         */

        // If SsasCompatibleNaming (the new behavior), the usages of the
        // [Store] dimension create dimensions called [Store]
        // and [Store2], each with a hierarchy called [Store].
        // Therefore Store2's all member is [Store2].[Store].[All Stores],
        // or [Store2].[All Stores] for short.
        //
        // Under the old behavior, the member is called [Store2].[All Store2s].
        final String store2AllMember = "[Store2].[All Stores]";
        withSchema(context, TestAllMemberMultipleDimensionUsagesModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select\n"
            + " {[Store].[Store].[All Stores]} on columns,\n"
            + " {" + store2AllMember + "} on rows\n"
            + "From [Sales Two Sales Dimensions]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[Store].[All Stores]}\n"
            + "Axis #2:\n"
            + "{[Store2].[Store].[All Stores]}\n"
            + "Row #0: 266,773\n");

        final Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "select ([Store].[All Stores], " + store2AllMember + ") on 0\n"
            + "from [Sales Two Sales Dimensions]");
        final Axis axis = result.getAxes()[0];
        final Position position = axis.getPositions().get(0);
        assertEquals(
            "First Store", position.get(0).getDimension().getCaption());
        assertEquals(
            "Second Store", position.get(1).getDimension().getCaption());
    }

    /**
     * This test displays an informative error message if someone uses
     * an unaliased name instead of an aliased name
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonAliasedDimensionUsage(Context<?> context) {
        class TestNonAliasedDimensionUsageModifier extends PojoMappingModifier {
            public TestNonAliasedDimensionUsageModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                		.withOverrideDimensionName("Time2")
                		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                        .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                        .build();

                DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Store")
                        .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_STORE_WITH_QUERY_STORE))
                        .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                        .build();

                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("Sales Two Dimensions")
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
                    		.build())
                    .withDimensionConnectors(List.of(d1, d2))
                    .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder()
                        	.withMeasures(List.of(
                        SumMeasureMappingImpl
                            .builder()
                            .withName("Unit Sales")
                            .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                            .withFormatString("Standard")
                            .build(),
                        SumMeasureMappingImpl
                            .builder()
                            .withName("Store Cost")
                            .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_SALES_FACT_1997)

                            .withFormatString("#,###.00")
                            .build()
                    ))
                    .build()
                ))
                .build();
                result.add(c);
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,

            "<Cube name=\"Sales Two Dimensions\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <DimensionUsage name=\"Time2\" source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "  <DimensionUsage name=\"Store\" source=\"Store\" foreignKey=\"store_id\"/>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" "
            + "   formatString=\"Standard\"/>\n"
            + "  <Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\""
            + "   formatString=\"#,###.00\"/>\n"
            + "</Cube>", null, null, null, null);
        withSchema(context, schema);
         */
        withSchema(context, TestNonAliasedDimensionUsageModifier::new);
        final String query = "select\n"
                             + " {[Time2].[Time].[1997]} on columns \n"
                             + "From [Sales Two Dimensions]";
            assertQueryReturns(context.getConnectionWithDefaultRole(),
                query,
                "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Time2].[Time].[1997]}\n"
                + "Row #0: 266,773\n");
    }

    /**
     * Tests a cube whose fact table is a &lt;View&gt; element as well as a
     * degenerate dimension.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testViewDegenerateDims(Context<?> context) {
        class TestViewDegenerateDimsModifier extends PojoMappingModifier {
            public TestViewDegenerateDimsModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));

                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                		.withOverrideDimensionName("Time")
                		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                        .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                        .build();

                DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Product")
                        .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_PRODUCT))
                        .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                        .build();

                DimensionConnectorMappingImpl d3 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Store")
                        .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_STORE_WITH_QUERY_STORE))
                        .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                        .build();

                PhysicalColumnMappingImpl WAREHOUSE_ID_COLUMN_IN_INVENTORY_FACKT_1997 = PhysicalColumnMappingImpl.builder().withName("warehouse_id").withDataType(ColumnDataType.INTEGER).build();
                SqlSelectQueryMappingImpl v1 = SqlSelectQueryMappingImpl.builder()
                        .withAlias("FACT").withSql(
                                ((SqlViewMappingImpl.Builder) SqlViewMappingImpl.builder()
                                .withColumns(List.of(
                                        WAREHOUSE_ID_COLUMN_IN_INVENTORY_FACKT_1997
                                 )))
                                .withSqlStatements(
                                    List.of(
                                    SqlStatementMappingImpl.builder().withSql("select * from \"inventory_fact_1997\" as \"FOOBAR\"").withDialects(List.of("generic")).build(),
                                    SqlStatementMappingImpl.builder().withSql("select * from \"inventory_fact_1997\" as \"FOOBAR\"").withDialects(List.of("oracle")).build(),
                                    SqlStatementMappingImpl.builder().withSql("select * from `inventory_fact_1997` as `FOOBAR`").withDialects(List.of("mysql")).build(),
                                    SqlStatementMappingImpl.builder().withSql("select * from `inventory_fact_1997` as `FOOBAR`").withDialects(List.of("infobright")).build()
                                ))
                                .build()
                                )
                        .build();
                    LevelMappingImpl l1 = LevelMappingImpl.builder()
                        .withName("Warehouse ID")
                        .withColumn(WAREHOUSE_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                        .withUniqueMembers(true)
                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                        .build();
                    HierarchyMappingImpl h1 = ExplicitHierarchyMappingImpl.builder()
                        .withQuery(v1)
                        .withLevels(List.of(l1))
                        .build();
                    DimensionConnectorMappingImpl d4 = DimensionConnectorMappingImpl.builder()
                    	.withOverrideDimensionName("Warehouse")
                    	.withDimension(StandardDimensionMappingImpl.builder()
                    		.withName("Warehouse")
                    		.withHierarchies(List.of(h1)).build())
                        .build();
                    PhysicalColumnMappingImpl WAREHOUSE_COST_COLUMN_IN_INVENTORY_FACKT_1997 = PhysicalColumnMappingImpl.builder().withName("warehouse_cost").withDataType(ColumnDataType.DECIMAL).withColumnSize(10).withDecimalDigits(4).build();
                    PhysicalColumnMappingImpl WAREHOUSE_SALES_COLUMN_IN_INVENTORY_FACKT_1997 = PhysicalColumnMappingImpl.builder().withName("warehouse_sales").withDataType(ColumnDataType.DECIMAL).withColumnSize(10).withDecimalDigits(4).build();
                    SqlSelectQueryMappingImpl view = SqlSelectQueryMappingImpl.builder()
                                .withAlias("FACT").withSql(
                                        ((SqlViewMappingImpl.Builder) SqlViewMappingImpl.builder()
                                        .withColumns(List.of(
                                                WAREHOUSE_COST_COLUMN_IN_INVENTORY_FACKT_1997,
                                                WAREHOUSE_SALES_COLUMN_IN_INVENTORY_FACKT_1997
                                         )))
                                        .withSqlStatements(
                                            List.of(
                                            SqlStatementMappingImpl.builder().withSql("select * from \"inventory_fact_1997\" as \"FOOBAR\"").withDialects(List.of("generic")).build(),
                                            SqlStatementMappingImpl.builder().withSql("select * from \"inventory_fact_1997\" as \"FOOBAR\"").withDialects(List.of("oracle")).build(),
                                            SqlStatementMappingImpl.builder().withSql("select * from `inventory_fact_1997` as `FOOBAR`").withDialects(List.of("mysql")).build(),
                                            SqlStatementMappingImpl.builder().withSql("select * from `inventory_fact_1997` as `FOOBAR`").withDialects(List.of("infobright")).build()
                                        ))
                                        .build()
                                        )
                                .build();
                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("Warehouse (based on view)")
                    .withQuery(view)
                    .withDimensionConnectors(List.of(d1, d2, d3, d4))
                    .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder()
                        	.withMeasures(List.of(
                        SumMeasureMappingImpl
                            .builder()
                            .withName("Warehouse Cost")
                            .withColumn(WAREHOUSE_COST_COLUMN_IN_INVENTORY_FACKT_1997)

                            .build(),
                        SumMeasureMappingImpl
                            .builder()
                            .withName("Warehouse Sales")
                            .withColumn(WAREHOUSE_SALES_COLUMN_IN_INVENTORY_FACKT_1997)

                            .build()
                    ))
                    .build()
                ))
                .build();
                result.add(c);

                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,

            // Warehouse cube where the default member in the Warehouse
            // dimension is USA.
            "<Cube name=\"Warehouse (based on view)\">\n"
            + "  <View alias=\"FACT\">\n"
            + "    <SQL dialect=\"generic\">\n"
            + "     <![CDATA[select * from \"inventory_fact_1997\" as \"FOOBAR\"]]>\n"
            + "    </SQL>\n"
            + "    <SQL dialect=\"oracle\">\n"
            + "     <![CDATA[select * from \"inventory_fact_1997\" \"FOOBAR\"]]>\n"
            + "    </SQL>\n"
            + "    <SQL dialect=\"mysql\">\n"
            + "     <![CDATA[select * from `inventory_fact_1997` as `FOOBAR`]]>\n"
            + "    </SQL>\n"
            + "    <SQL dialect=\"infobright\">\n"
            + "     <![CDATA[select * from `inventory_fact_1997` as `FOOBAR`]]>\n"
            + "    </SQL>\n"
            + "  </View>\n"
            + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "  <DimensionUsage name=\"Product\" source=\"Product\" foreignKey=\"product_id\"/>\n"
            + "  <DimensionUsage name=\"Store\" source=\"Store\" foreignKey=\"store_id\"/>\n"
            + "  <Dimension name=\"Warehouse\">\n"
            + "    <Hierarchy hasAll=\"true\"> \n"
            + "      <View alias=\"FACT\">\n"
            + "        <SQL dialect=\"generic\">\n"
            + "         <![CDATA[select * from \"inventory_fact_1997\" as \"FOOBAR\"]]>\n"
            + "        </SQL>\n"
            + "        <SQL dialect=\"oracle\">\n"
            + "         <![CDATA[select * from \"inventory_fact_1997\" \"FOOBAR\"]]>\n"
            + "        </SQL>\n"
            + "        <SQL dialect=\"mysql\">\n"
            + "         <![CDATA[select * from `inventory_fact_1997` as `FOOBAR`]]>\n"
            + "        </SQL>\n"
            + "        <SQL dialect=\"infobright\">\n"
            + "         <![CDATA[select * from `inventory_fact_1997` as `FOOBAR`]]>\n"
            + "        </SQL>\n"
            + "      </View>\n"
            + "      <Level name=\"Warehouse ID\" column=\"warehouse_id\"\n"
            + "          uniqueMembers=\"true\" type=\"Numeric\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Measure name=\"Warehouse Cost\" column=\"warehouse_cost\" aggregator=\"sum\"/>\n"
            + "  <Measure name=\"Warehouse Sales\" column=\"warehouse_sales\" aggregator=\"sum\"/>\n"
            + "</Cube>", null, null, null, null);
        withSchema(context, schema);
         */
        withSchema(context, TestViewDegenerateDimsModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select\n"
            + " NON EMPTY {[Time].[1997], [Time].[1997].[Q3]} on columns,\n"
            + " NON EMPTY {[Store].[USA].Children} on rows\n"
            + "From [Warehouse (based on view)]\n"
            + "where [Warehouse].[2]",
            "Axis #0:\n"
            + "{[Warehouse].[Warehouse].[2]}\n"
            + "Axis #1:\n"
            + "{[Time].[Time].[1997]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[WA]}\n"
            + "Row #0: 917.554\n");
    }

    /**
     * Tests a cube whose fact table is a &lt;View&gt; element.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testViewFactTable(Context<?> context) {
        class TestViewFactTableModifier extends PojoMappingModifier {
            public TestViewFactTableModifier(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                		.withOverrideDimensionName("Time")
                		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                        .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                        .build();

                DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Product")
                        .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_PRODUCT))
                        .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                        .build();

                DimensionConnectorMappingImpl d3 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Store")
                        .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_STORE_WITH_QUERY_STORE))
                        .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                        .build();

                    LevelMappingImpl l1 = LevelMappingImpl.builder()
                        .withName("Country")
                        .withColumn(FoodmartMappingSupplier.WAREHOUSE_COUNTRY_COLUMN_IN_WAREHOUSE)
                        .withUniqueMembers(true)
                        .build();
                    LevelMappingImpl l2 = LevelMappingImpl.builder()
                            .withName("State Province")
                            .withColumn(FoodmartMappingSupplier.WAREHOUSE_STATE_PROVINCE_COLUMN_IN_WAREHOUSE)
                            .withUniqueMembers(true)
                            .build();
                    LevelMappingImpl l3 = LevelMappingImpl.builder()
                            .withName("City")
                            .withColumn(FoodmartMappingSupplier.WAREHOUSE_CITY_COLUMN_IN_WAREHOUSE)
                            .withUniqueMembers(false)
                            .build();
                    LevelMappingImpl l4 = LevelMappingImpl.builder()
                            .withName("Warehouse Name")
                            .withColumn(FoodmartMappingSupplier.WAREHOUSE_NAME_COLUMN_IN_WAREHOUSE)
                            .withUniqueMembers(true)
                            .build();

                    HierarchyMappingImpl h1 = ExplicitHierarchyMappingImpl.builder()
                    	.withHasAll(true)
                        .withDefaultMember("[USA]")
                        .withPrimaryKey(FoodmartMappingSupplier.WAREHOUSE_ID_COLUMN_IN_WAREHOUSE)
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.WAREHOUSE_TABLE).build())
                        .withLevels(List.of(l1, l2, l3, l4))
                        .build();

                    DimensionConnectorMappingImpl d4 = DimensionConnectorMappingImpl.builder()
                    	.withOverrideDimensionName("Warehouse")
                    	.withForeignKey(FoodmartMappingSupplier.WAREHOUSE_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                    	.withDimension(StandardDimensionMappingImpl.builder()
                    		.withName("Warehouse")
                    		.withHierarchies(List.of(h1)).build())
                        .build();

                    SqlSelectQueryMappingImpl view = SqlSelectQueryMappingImpl.builder()
                        .withAlias("FACT").withSql(
                                ((SqlViewMappingImpl.Builder) SqlViewMappingImpl.builder()
                                .withColumns(List.of(
                                        FoodmartMappingSupplier.TIME_ID_COLUMN_IN_INVENTORY_FACKT_1997,
                                        FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_INVENTORY_FACKT_1997,
                                        FoodmartMappingSupplier.STORE_ID_COLUMN_IN_INVENTORY_FACKT_1997,
                                        FoodmartMappingSupplier.WAREHOUSE_COST_COLUMN_IN_INVENTORY_FACKT_1997,
                                        FoodmartMappingSupplier.WAREHOUSE_SALES_COLUMN_IN_INVENTORY_FACKT_1997,
                                        FoodmartMappingSupplier.WAREHOUSE_ID_COLUMN_IN_INVENTORY_FACKT_1997
                                 )))
                                .withSqlStatements(
                                    List.of(
                                    SqlStatementMappingImpl.builder().withSql("select * from \"inventory_fact_1997\" as \"FOOBAR\"").withDialects(List.of("generic", "oracle")).build(),
                                    SqlStatementMappingImpl.builder().withSql("select * from `inventory_fact_1997` as `FOOBAR`").withDialects(List.of("mysql", "infobright")).build()
                                ))
                                .build()
                                )
                        .build();

                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("Warehouse (based on view)")
                    .withQuery(view)
                    .withDimensionConnectors(List.of(d1, d2, d3, d4))
                    .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder()
                        	.withMeasures(List.of(
                        SumMeasureMappingImpl
                            .builder()
                            .withName("Warehouse Cost")
                            .withColumn(FoodmartMappingSupplier.WAREHOUSE_COST_COLUMN_IN_INVENTORY_FACKT_1997)

                            .build(),
                        SumMeasureMappingImpl
                            .builder()
                            .withName("Warehouse Sales")
                            .withColumn(FoodmartMappingSupplier.WAREHOUSE_SALES_COLUMN_IN_INVENTORY_FACKT_1997)

                            .build()
                    ))
                    .build()
                ))
                .build();
                result.add(c);
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,

            // Warehouse cube where the default member in the Warehouse
            // dimension is USA.
            "<Cube name=\"Warehouse (based on view)\">\n"
            + "  <View alias=\"FACT\">\n"
            + "    <SQL dialect=\"generic\">\n"
            + "     <![CDATA[select * from \"inventory_fact_1997\" as \"FOOBAR\"]]>\n"
            + "    </SQL>\n"
            + "    <SQL dialect=\"oracle\">\n"
            + "     <![CDATA[select * from \"inventory_fact_1997\" \"FOOBAR\"]]>\n"
            + "    </SQL>\n"
            + "    <SQL dialect=\"mysql\">\n"
            + "     <![CDATA[select * from `inventory_fact_1997` as `FOOBAR`]]>\n"
            + "    </SQL>\n"
            + "    <SQL dialect=\"infobright\">\n"
            + "     <![CDATA[select * from `inventory_fact_1997` as `FOOBAR`]]>\n"
            + "    </SQL>\n"
            + "  </View>\n"
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
            + "</Cube>", null, null, null, null);
        withSchema(context, schema);
         */
        withSchema(context, TestViewFactTableModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select\n"
            + " {[Time].[1997], [Time].[1997].[Q3]} on columns,\n"
            + " {[Store].[USA].Children} on rows\n"
            + "From [Warehouse (based on view)]\n"
            + "where [Warehouse].[USA]",
            "Axis #0:\n"
            + "{[Warehouse].[Warehouse].[USA]}\n"
            + "Axis #1:\n"
            + "{[Time].[Time].[1997]}\n"
            + "{[Time].[Time].[1997].[Q3]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[CA]}\n"
            + "{[Store].[Store].[USA].[OR]}\n"
            + "{[Store].[Store].[USA].[WA]}\n"
            + "Row #0: 25,789.087\n"
            + "Row #0: 8,624.791\n"
            + "Row #1: 17,606.904\n"
            + "Row #1: 3,812.023\n"
            + "Row #2: 45,647.262\n"
            + "Row #2: 12,664.162\n");
    }

    /**
     * Tests a cube whose fact table is a &lt;View&gt; element, and which
     * has dimensions based on the fact table.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testViewFactTable2(Context<?> context) {
        class TestViewFactTable2Modifier extends PojoMappingModifier {
            public TestViewFactTable2Modifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                PhysicalColumnMappingImpl STORE_TYPE_COLUMN_IN_STORE = PhysicalColumnMappingImpl.builder().withName("store_type").withDataType(ColumnDataType.VARCHAR).withColumnSize(30).build();
                PhysicalColumnMappingImpl STORE_SQFT_COLUMN_IN_STORE = PhysicalColumnMappingImpl.builder().withName("store_sqft").withDataType(ColumnDataType.INTEGER).build();
                PhysicalColumnMappingImpl GROCERY_SQFT_COLUMN_IN_STORE = PhysicalColumnMappingImpl.builder().withName("grocery_sqft").withDataType(ColumnDataType.INTEGER).build();

                    LevelMappingImpl l1 = LevelMappingImpl.builder()
                        .withName("Store Type")
                        .withColumn(STORE_TYPE_COLUMN_IN_STORE)
                        .withUniqueMembers(true)
                        .build();

                    HierarchyMappingImpl h1 = ExplicitHierarchyMappingImpl.builder()
                    	.withHasAll(true)
                        .withLevels(List.of(l1))
                        .build();

                    DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                    	.withOverrideDimensionName("Store Type")
                    	.withDimension(StandardDimensionMappingImpl.builder()
                    		.withName("Store Type")
                    		.withHierarchies(List.of(h1)).build())
                        .build();
                    SqlSelectQueryMappingImpl view = SqlSelectQueryMappingImpl.builder()
                            .withAlias("FACT").withSql(
                                    ((SqlViewMappingImpl.Builder) SqlViewMappingImpl.builder()
                                    .withColumns(List.of(
                                            STORE_TYPE_COLUMN_IN_STORE,
                                            STORE_SQFT_COLUMN_IN_STORE,
                                            GROCERY_SQFT_COLUMN_IN_STORE
                                     )))
                                    .withSqlStatements(
                                        List.of(
                                        SqlStatementMappingImpl.builder().withSql("select * from \"store\" as \"FOOBAR\"").withDialects(List.of("generic", "oracle")).build(),
                                        SqlStatementMappingImpl.builder().withSql("select * from `store` as `FOOBAR`").withDialects(List.of("mysql", "infobright")).build()
                                    ))
                                    .build()
                                    )
                            .build();

                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("Store2")
                    .withQuery(view)
                    .withDimensionConnectors(List.of(d1))
                    .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder()
                        	.withMeasures(List.of(
                        SumMeasureMappingImpl
                            .builder()
                            .withName("Store Sqft")
                            .withColumn(STORE_SQFT_COLUMN_IN_STORE)

                            .withFormatString("#,###")
                            .build(),
                        SumMeasureMappingImpl
                            .builder()
                            .withName("Grocery Sqft")
                            .withColumn(GROCERY_SQFT_COLUMN_IN_STORE)

                            .withFormatString("#,###")
                            .build()
                    ))
                    .build()
                ))
                .build();
                result.add(c);
                result.addAll(super.cubes(cubes));
                return result;
            }

        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            // Similar to "Store" cube in FoodMart.xml.
            "<Cube name=\"Store2\">\n"
            + "  <View alias=\"FACT\">\n"
            + "    <SQL dialect=\"generic\">\n"
            + "     <![CDATA[select * from \"store\" as \"FOOBAR\"]]>\n"
            + "    </SQL>\n"
            + "    <SQL dialect=\"oracle\">\n"
            + "     <![CDATA[select * from \"store\" \"FOOBAR\"]]>\n"
            + "    </SQL>\n"
            + "    <SQL dialect=\"mysql\">\n"
            + "     <![CDATA[select * from `store` as `FOOBAR`]]>\n"
            + "    </SQL>\n"
            + "    <SQL dialect=\"infobright\">\n"
            + "     <![CDATA[select * from `store` as `FOOBAR`]]>\n"
            + "    </SQL>\n"
            + "  </View>\n"
            + "  <!-- We could have used the shared dimension \"Store Type\", but we\n"
            + "     want to test private dimensions without primary key. -->\n"
            + "  <Dimension name=\"Store Type\">\n"
            + "    <Hierarchy hasAll=\"true\">\n"
            + "      <Level name=\"Store Type\" column=\"store_type\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "\n"
            + "  <Measure name=\"Store Sqft\" column=\"store_sqft\" aggregator=\"sum\"\n"
            + "      formatString=\"#,###\"/>\n"
            + "  <Measure name=\"Grocery Sqft\" column=\"grocery_sqft\" aggregator=\"sum\"\n"
            + "      formatString=\"#,###\"/>\n"
            + "\n"
            + "</Cube>", null, null, null, null);
        withSchema(context, schema);
         */
        withSchema(context, TestViewFactTable2Modifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Store Type].Children} on columns from [Store2]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store Type].[Store Type].[Deluxe Supermarket]}\n"
            + "{[Store Type].[Store Type].[Gourmet Supermarket]}\n"
            + "{[Store Type].[Store Type].[HeadQuarters]}\n"
            + "{[Store Type].[Store Type].[Mid-Size Grocery]}\n"
            + "{[Store Type].[Store Type].[Small Grocery]}\n"
            + "{[Store Type].[Store Type].[Supermarket]}\n"
            + "Row #0: 146,045\n"
            + "Row #0: 47,447\n"
            + "Row #0: \n"
            + "Row #0: 109,343\n"
            + "Row #0: 75,281\n"
            + "Row #0: 193,480\n");
    }

    /**
     * Tests that the deprecated "distinct count" value for the
     * Measure@aggregator attribute still works. The preferred value these days
     * is "distinct-count".
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDeprecatedDistinctCountAggregator(Context<?> context) {
        class TestDeprecatedDistinctCountAggregatorModifier extends PojoMappingModifier{

            public TestDeprecatedDistinctCountAggregatorModifier(CatalogMapping catalog) {
                super(catalog);
            }


            protected List<? extends MeasureGroupMapping> physicalCubeMeasureGroups(PhysicalCubeMapping cube) {
                List<MeasureGroupMapping> result = new ArrayList<>();
                result.addAll(super.physicalCubeMeasureGroups(cube));
                if ("Sales".equals(cube.getName())) {
                    result.add(MeasureGroupMappingImpl.builder()
                    		.withMeasures(List.of(CountMeasureMappingImpl
                    	            .builder()
                    	            .withName("Customer Count2")
                    	            .withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                    	            .withDistinct(true)
                    	            .withFormatString("#,###")
                    	            .build()))
                    		.build()
                    );
                }
                return result;
            }
            protected List<? extends CalculatedMemberMapping> cubeCalculatedMembers(CubeMapping cube) {
                List<CalculatedMemberMapping> result = new ArrayList<>();
                result.addAll(super.cubeCalculatedMembers(cube));
                if ("Sales".equals(cube.getName())) {
                    CalculatedMemberMappingImpl calculatedMember =
                    		CalculatedMemberMappingImpl
                            .builder()
                            .withName("Half Customer Count")
                            //.withDimension("Measures")
                            .withVisible(false)
                            .withFormula("[Measures].[Customer Count2] / 2")
                            .build();
                    result.add(calculatedMember);
                }
                return result;
            }
        }
        withSchema(context, TestDeprecatedDistinctCountAggregatorModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures].[Unit Sales],"
            + "    [Measures].[Customer Count], "
            + "    [Measures].[Customer Count2], "
            + "    [Measures].[Half Customer Count]} on 0,\n"
            + " {[Store].[USA].Children} ON 1\n"
            + "FROM [Sales]\n"
            + "WHERE ([Gender].[M])",
            "Axis #0:\n"
            + "{[Gender].[Gender].[M]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Customer Count]}\n"
            + "{[Measures].[Customer Count2]}\n"
            + "{[Measures].[Half Customer Count]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[CA]}\n"
            + "{[Store].[Store].[USA].[OR]}\n"
            + "{[Store].[Store].[USA].[WA]}\n"
            + "Row #0: 37,989\n"
            + "Row #0: 1,389\n"
            + "Row #0: 1,389\n"
            + "Row #0: 695\n"
            + "Row #1: 34,623\n"
            + "Row #1: 536\n"
            + "Row #1: 536\n"
            + "Row #1: 268\n"
            + "Row #2: 62,603\n"
            + "Row #2: 901\n"
            + "Row #2: 901\n"
            + "Row #2: 451\n");
        context.getCatalogCache().clear();
    }

    /**
     * Testcase for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-291">
     * Bug MONDRIAN-291, "'unknown usage' messages"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUnknownUsages(Context<?> context) {
        class TestUnknownUsagesModifier extends PojoMappingModifier {
            public TestUnknownUsagesModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected CatalogMapping modifyCatalog(CatalogMapping catalog2) {
            	TableQueryMappingImpl t = TableQueryMappingImpl.builder()
            			.withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
            			.withAggregationExcludes(
            				List.of(
                        		AggregationExcludeMappingImpl.builder().withName("agg_c_14_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_l_05_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_g_ms_pcat_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_ll_01_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_c_special_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_l_03_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_l_04_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_pl_01_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_lc_06_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_lc_100_sales_fact_1997").build()
                            )
            			)
            			.withAggregationTables(List.of(
            				AggregationNameMappingImpl.builder()
                                .withName(FoodmartMappingSupplier.AGG_C_10_SALES_FACT_1997)
                                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.FACT_COUNT_COLUMN_IN_AGG_C_10_SALES_FACT_1997).build())
                                .withAggregationMeasures(List.of(
                                		AggregationMeasureMappingImpl.builder()
                                        .withName("[Measures].[Store Cost]")
                                        .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_AGG_C_10_SALES_FACT_1997)
                                        .build(),
                                		AggregationMeasureMappingImpl.builder()
                                        .withName("[Measures].[Store Sales]")
                                        .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_AGG_C_10_SALES_FACT_1997)
                                        .build()
                                ))
                                .build()
            			))
            			.build();

                    LevelMappingImpl l11 = LevelMappingImpl.builder()
                        .withName("Year")
                        .withColumn(FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY)
                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                        .withUniqueMembers(true)
                        .withLevelType(LevelType.TIME_YEARS)
                        .build();
                    LevelMappingImpl l12 = LevelMappingImpl.builder()
                        .withName("Quarter")
                        .withColumn(FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY)
                        .withUniqueMembers(false)
                        .withLevelType(LevelType.TIME_QUARTERS)
                        .build();
                    LevelMappingImpl l13 = LevelMappingImpl.builder()
                        .withName("Month")
                        .withColumn(FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY)
                        .withUniqueMembers(false)
                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                        .withLevelType(LevelType.TIME_MONTHS)
                        .build();

                    HierarchyMappingImpl h11 = ExplicitHierarchyMappingImpl.builder()
                        .withHasAll(false)
                        .withPrimaryKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY)
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.TIME_BY_DAY_TABLE).build())
                        .withLevels(List.of(l11, l12, l13))
                        .build();

                    DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                    	.withOverrideDimensionName("Time")
                    	.withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                    	.withDimension(TimeDimensionMappingImpl.builder()
                    		.withName("Time")
                    		.withHierarchies(List.of(h11)).build())
                        .build();

                    LevelMappingImpl l21 = LevelMappingImpl.builder()
                        .withName("day")
                        .withColumn(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                        .build();

                    LevelMappingImpl l22 = LevelMappingImpl.builder()
                        .withName("month")
                        .withColumn(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                        .build();

                    HierarchyMappingImpl h21 = ExplicitHierarchyMappingImpl.builder()
                        .withHasAll(true)
                        .withPrimaryKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                        .withLevels(List.of(l21, l22))
                        .build();

                    DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                    	.withOverrideDimensionName("Time Degenerate")
                    	.withDimension(StandardDimensionMappingImpl.builder()
                    		.withName("Time Degenerate")
                    		.withHierarchies(List.of(h21)).build())
                        .build();

                    PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl.builder()
                        .withName("Sales Degen")
                        .withQuery(t)
                        .withDimensionConnectors(List.of(d1, d2))
                        .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                        		.withMeasures(List.of(
                                    SumMeasureMappingImpl.builder()
                                        .withName("Store Cost").withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_SALES_FACT_1997)

                                        .withFormatString("#,###.00")
                                        .build(),
                                    SumMeasureMappingImpl.builder()
                                        .withName("Store Sales")
                                        .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_SALES_FACT_1997)

                                        .withFormatString("#,###.00")
                                        .build()
                        		))
                        		.build()))
                        .build();

            	 return CatalogMappingImpl.builder()
            			 .withName("FoodMart")
                         .withDbSchemas((List<DatabaseSchemaMappingImpl>) catalogDatabaseSchemas( catalog2))
                         .withCubes(List.of(c))
                         .build();
            }
        }

        if (!context.getConfigValue(ConfigConstants.READ_AGGREGATES, ConfigConstants.READ_AGGREGATES_DEFAULT_VALUE ,Boolean.class)) {
            return;
        }



        final StringWriter sw = new StringWriter();


        /*
            withSchema(context,
                "<?xml version=\"1.0\"?>\n"
                + "<Schema name=\"FoodMart\">\n"
                + "<Cube name=\"Sales Degen\">\n"
                + "  <Table name=\"sales_fact_1997\">\n"
                + "    <AggExclude pattern=\"agg_c_14_sales_fact_1997\"/>\n"
                + "    <AggExclude pattern=\"agg_l_05_sales_fact_1997\"/>\n"
                + "    <AggExclude pattern=\"agg_g_ms_pcat_sales_fact_1997\"/>\n"
                + "    <AggExclude pattern=\"agg_ll_01_sales_fact_1997\"/>\n"
                + "    <AggExclude pattern=\"agg_c_special_sales_fact_1997\"/>\n"
                + "    <AggExclude pattern=\"agg_l_03_sales_fact_1997\"/>\n"
                + "    <AggExclude pattern=\"agg_l_04_sales_fact_1997\"/>\n"
                + "    <AggExclude pattern=\"agg_pl_01_sales_fact_1997\"/>\n"
                + "    <AggExclude pattern=\"agg_lc_06_sales_fact_1997\"/>\n"
                + "    <AggExclude pattern=\"agg_lc_100_sales_fact_1997\"/>\n"
                + "    <AggName name=\"agg_c_10_sales_fact_1997\">\n"
                + "      <AggFactCount column=\"fact_count\"/>\n"
                + "      <AggMeasure name=\"[Measures].[Store Cost]\" column=\"store_cost\" />\n"
                + "      <AggMeasure name=\"[Measures].[Store Sales]\" column=\"store_sales\" />\n"
                + "     </AggName>\n"
                + "  </Table>\n"
                + "  <Dimension name=\"Time\" type=\"TimeDimension\" foreignKey=\"time_id\">\n"
                + "    <Hierarchy hasAll=\"false\" primaryKey=\"time_id\">\n"
                + "      <Table name=\"time_by_day\"/>\n"
                + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
                + "          levelType=\"TimeYears\"/>\n"
                + "      <Level name=\"Quarter\" column=\"quarter\" uniqueMembers=\"false\"\n"
                + "          levelType=\"TimeQuarters\"/>\n"
                + "      <Level name=\"Month\" column=\"month_of_year\" uniqueMembers=\"false\" type=\"Numeric\"\n"
                + "          levelType=\"TimeMonths\"/>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"
                + "  <Dimension name=\"Time Degenerate\">\n"
                + "    <Hierarchy hasAll=\"true\" primaryKey=\"time_id\">\n"
                + "      <Level name=\"day\" column=\"time_id\"/>\n"
                + "      <Level name=\"month\" column=\"product_id\" type=\"Numeric\"/>\n"
                + "    </Hierarchy>"
                + "  </Dimension>"
                + "  <Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\"\n"
                + "      formatString=\"#,###.00\"/>\n"
                + "  <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
                + "      formatString=\"#,###.00\"/>\n"
                + "</Cube>\n"
                + "</Schema>");
         */
            withSchema(context, TestUnknownUsagesModifier::new);
            assertQueryReturns(context.getConnectionWithDefaultRole(),
                "select from [Sales Degen]",
                "Axis #0:\n"
                + "{}\n"
                + "225,627.23");

        // Note that 'product_id' is NOT one of the columns with unknown usage.
        // It is used as a level in the degenerate dimension [Time Degenerate].
        assertEqualsVerbose(
            "WARN - Recognizer.checkUnusedColumns: Candidate aggregate table 'agg_c_10_sales_fact_1997' for fact table 'sales_fact_1997' has a column 'customer_count' with unknown usage.\n"
            + "WARN - Recognizer.checkUnusedColumns: Candidate aggregate table 'agg_c_10_sales_fact_1997' for fact table 'sales_fact_1997' has a column 'month_of_year' with unknown usage.\n"
            + "WARN - Recognizer.checkUnusedColumns: Candidate aggregate table 'agg_c_10_sales_fact_1997' for fact table 'sales_fact_1997' has a column 'quarter' with unknown usage.\n"
            + "WARN - Recognizer.checkUnusedColumns: Candidate aggregate table 'agg_c_10_sales_fact_1997' for fact table 'sales_fact_1997' has a column 'the_year' with unknown usage.\n"
            + "WARN - Recognizer.checkUnusedColumns: Candidate aggregate table 'agg_c_10_sales_fact_1997' for fact table 'sales_fact_1997' has a column 'unit_sales' with unknown usage.\n",
            sw.toString());
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUnknownUsages1(Context<?> context) {
        class TestUnknownUsages1Modifier extends PojoMappingModifier {
            public TestUnknownUsages1Modifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected CatalogMapping modifyCatalog(CatalogMapping catalog2) {
            	TableQueryMappingImpl t = TableQueryMappingImpl.builder()
            			.withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
            			.withAggregationExcludes(
            				List.of(
                        		AggregationExcludeMappingImpl.builder().withName("agg_c_14_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_l_05_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_g_ms_pcat_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_ll_01_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_c_special_sales_fact_1997").build(),

                        		AggregationExcludeMappingImpl.builder().withName("agg_l_04_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_pl_01_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_c_10_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_lc_06_sales_fact_1997").build()
                            )
            			)
            			.withAggregationTables(List.of(
            				AggregationNameMappingImpl.builder()
                                .withName(FoodmartMappingSupplier.AGG_L_03_SALES_FACT_1997)
                                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.FACT_COUNT_COLUMN_IN_AGG_L_03_SALES_FACT_1997).build())
                                .withAggregationMeasures(List.of(
                                		AggregationMeasureMappingImpl.builder()
                                        .withName("[Measures].[Store Cost]")
                                        .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_AGG_L_03_SALES_FACT_1997)
                                        .build(),
                                		AggregationMeasureMappingImpl.builder()
                                        .withName("[Measures].[Unit Sales]")
                                        .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_AGG_L_03_SALES_FACT_1997)
                                        .build()
                                ))
                                .withAggregationLevels(List.of(
                                	AggregationLevelMappingImpl.builder()
                                        .withName("[Customer].[Customer ID]")
                                        .withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_AGG_L_03_SALES_FACT_1997)
                                        .build()
                                ))
                                .withAggregationForeignKeys(List.of(
                                	AggregationForeignKeyMappingImpl.builder()
                                        .withFactColumn(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                                        .withAggregationColumn(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_AGG_L_03_SALES_FACT_1997)
                                        .build()
                                ))
                                .build()
            			))
            			.build();


                    LevelMappingImpl l11 = LevelMappingImpl.builder()
                        .withName("Year")
                        .withColumn(FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY)
                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                        .withUniqueMembers(true)
                        .withLevelType(LevelType.TIME_YEARS)
                        .build();
                    LevelMappingImpl l12 = LevelMappingImpl.builder()
                        .withName("Quarter")
                        .withColumn(FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY)
                        .withUniqueMembers(false)
                        .withLevelType(LevelType.TIME_QUARTERS)
                        .build();
                    LevelMappingImpl l13 = LevelMappingImpl.builder()
                        .withName("Month")
                        .withColumn(FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY)
                        .withUniqueMembers(false)
                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                        .withLevelType(LevelType.TIME_MONTHS)
                        .build();

                    HierarchyMappingImpl h11 = ExplicitHierarchyMappingImpl.builder()
                        .withHasAll(false)
                        .withPrimaryKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY)
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.TIME_BY_DAY_TABLE).build())
                        .withLevels(List.of(l11, l12, l13))
                        .build();

                    DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                    	.withOverrideDimensionName("Time")
                    	.withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                    	.withDimension(TimeDimensionMappingImpl.builder()
                    		.withName("Time")
                    		.withHierarchies(List.of(h11)).build())
                        .build();

                    LevelMappingImpl l21 = LevelMappingImpl.builder()
                        .withName("Customer ID")
                        .withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                        .build();

                    HierarchyMappingImpl h21 = ExplicitHierarchyMappingImpl.builder()
                        .withHasAll(true)
                        .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                        .withLevels(List.of(l21))
                        .build();

                    DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                    	.withOverrideDimensionName("Customer")
                    	.withDimension(StandardDimensionMappingImpl.builder()
                    		.withName("Customer")
                    		.withHierarchies(List.of(h21)).build())
                        .build();

                    LevelMappingImpl l31 = LevelMappingImpl.builder()
                            .withName("Product ID")
                            .withColumn(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                            .build();

                    HierarchyMappingImpl h31 = ExplicitHierarchyMappingImpl.builder()
                            .withHasAll(true)
                            .withPrimaryKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                            .withLevels(List.of(l31))
                            .build();

                    DimensionConnectorMappingImpl d3 = DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Product")
                    		.withDimension(StandardDimensionMappingImpl.builder()
                            .withName("Product")
                            .withHierarchies(List.of(h31)).build())
                            .build();

                    PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl.builder()
                        .withName("Denormalized Sales")
                        .withQuery(t)
                        .withDimensionConnectors(List.of(d1, d2, d3))
                        .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                        		.withMeasures(List.of(
                                    SumMeasureMappingImpl.builder()
                                        .withName("Store Cost").withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_SALES_FACT_1997)

                                        .withFormatString("#,###.00")
                                        .build(),
                                    SumMeasureMappingImpl.builder()
                                        .withName("Store Sales")
                                        .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_SALES_FACT_1997)

                                        .withFormatString("#,###.00")
                                        .build(),
                                    SumMeasureMappingImpl.builder()
                                        .withName("Unit Sales")
                                        .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                                        .withFormatString("#,###")
                                        .build()
                        		))
                        		.build()))
                        .build();

            	 return CatalogMappingImpl.builder()
            			 .withName("FoodMart")
                         .withDbSchemas((List<DatabaseSchemaMappingImpl>) catalogDatabaseSchemas( catalog2))
                         .withCubes(List.of(c))
                         .build();
            }

        }

        if (!context.getConfigValue(ConfigConstants.READ_AGGREGATES, ConfigConstants.READ_AGGREGATES_DEFAULT_VALUE ,Boolean.class)) {
            return;
        }
        final Logger logger = LoggerFactory.getLogger(AggTableManager.class);
        //propSaver.setAtLeast(logger, org.apache.logging.log4j.Level.WARN);

        final StringWriter sw = new StringWriter();
        //final LevelRangeFilter filter = LevelRangeFilter.createFilter(org.apache.logging.log4j.Level.WARN, null, null, null);
        //final Appender appender =
        //    WriterAppender.newBuilder()
        //        .setFilter(filter)
        //        .setLayout(PatternLayout.createDefaultLayout())
        //        .setTarget(sw)
        //        .build();

//        LoggerContext<?> ctx = (LoggerContext) LogManager.getContext( false );
        //Configuration config = ctx.getConfiguration();
        //LoggerConfig loggerConfig = config.getLoggerConfig( logger.getName() );
        //loggerConfig.addAppender( appender, org.apache.logging.log4j.Level.ALL, null );
        //ctx.updateLoggers();

        try {
            /*
            withSchema(context,
                "<?xml version=\"1.0\"?>\n"
                + "<Schema name=\"FoodMart\">\n"
                + "<Cube name=\"Denormalized Sales\">\n"
                + "  <Table name=\"sales_fact_1997\">\n"
                + "    <AggExclude pattern=\"agg_c_14_sales_fact_1997\"/>\n"
                + "    <AggExclude pattern=\"agg_l_05_sales_fact_1997\"/>\n"
                + "    <AggExclude pattern=\"agg_g_ms_pcat_sales_fact_1997\"/>\n"
                + "    <AggExclude pattern=\"agg_ll_01_sales_fact_1997\"/>\n"
                + "    <AggExclude pattern=\"agg_c_special_sales_fact_1997\"/>\n"
                + "    <AggExclude pattern=\"agg_l_04_sales_fact_1997\"/>\n"
                + "    <AggExclude pattern=\"agg_pl_01_sales_fact_1997\"/>\n"
                + "    <AggExclude pattern=\"agg_c_10_sales_fact_1997\"/>\n"
                + "    <AggExclude pattern=\"agg_lc_06_sales_fact_1997\"/>\n"
                + "    <AggName name=\"agg_l_03_sales_fact_1997\">\n"
                + "      <AggFactCount column=\"fact_count\"/>\n"
                + "      <AggMeasure name=\"[Measures].[Store Cost]\" column=\"store_cost\" />\n"
                + "      <AggMeasure name=\"[Measures].[Store Sales]\" column=\"store_sales\" />\n"
                + "      <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"unit_sales\" />\n"
                + "      <AggLevel name=\"[Customer].[Customer ID]\" column=\"customer_id\" />\n"
                + "      <AggForeignKey factColumn=\"time_id\" aggColumn=\"time_id\" />\n"
                + "     </AggName>\n"
                + "  </Table>\n"
                + "  <Dimension name=\"Time\" type=\"TimeDimension\" foreignKey=\"time_id\">\n"
                + "    <Hierarchy hasAll=\"false\" primaryKey=\"time_id\">\n"
                + "      <Table name=\"time_by_day\"/>\n"
                + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
                + "          levelType=\"TimeYears\"/>\n"
                + "      <Level name=\"Quarter\" column=\"quarter\" uniqueMembers=\"false\"\n"
                + "          levelType=\"TimeQuarters\"/>\n"
                + "      <Level name=\"Month\" column=\"month_of_year\" uniqueMembers=\"false\" type=\"Numeric\"\n"
                + "          levelType=\"TimeMonths\"/>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"
                + "  <Dimension name=\"Customer\">\n"
                + "    <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
                + "      <Level name=\"Customer ID\" column=\"customer_id\"/>\n"
                + "    </Hierarchy>"
                + "  </Dimension>"
                + "  <Dimension name=\"Product\">\n"
                + "    <Hierarchy hasAll=\"true\" primaryKey=\"product_id\">\n"
                + "      <Level name=\"Product ID\" column=\"product_id\"/>\n"
                + "    </Hierarchy>"
                + "  </Dimension>"
                + "  <Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\"\n"
                + "      formatString=\"#,###.00\"/>\n"
                + "  <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
                + "      formatString=\"#,###.00\"/>\n"
                + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
                + "      formatString=\"#,###\"/>\n"
                + "</Cube>\n"
                + "</Schema>");
             */
            withSchema(context, TestUnknownUsages1Modifier::new);
            assertQueryReturns(context.getConnectionWithDefaultRole(),
                "select from [Denormalized Sales]",
                "Axis #0:\n"
                + "{}\n"
                + "225,627.23");
        } finally {
            //loggerConfig.removeAppender( appender.getName() );
            //ctx.updateLoggers();
        }
        assertEqualsVerbose(
            "WARN - Recognizer.checkUnusedColumns: Candidate aggregate table 'agg_l_03_sales_fact_1997' for fact table 'sales_fact_1997' has a column 'time_id' with unknown usage.\n",
            sw.toString());
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testPropertyFormatter(Context<?> context) {
        class TestPropertyFormatterModifier extends PojoMappingModifier {
            public TestPropertyFormatterModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    LevelMappingImpl level = LevelMappingImpl
                            .builder()
                            .withName("Store2")
                            .withColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE_RAGGED)
                            .withCaptionColumn(FoodmartMappingSupplier.STORE_NAME_COLUMN_IN_STORE_RAGGED)
                            .withUniqueMembers(true)
                            .withMemberProperties(List.of(
                            	MemberPropertyMappingImpl
                                    .builder()
                                    .withName("Store Type")
                                    .withColumn(FoodmartMappingSupplier.STORE_TYPE_COLUMN_IN_STORE_RAGGED)
                                    .withFormatter(MemberPropertyFormatterMappingImpl.builder().withRef(DummyPropertyFormatter.class.getName()).build())
                                    .build(),
                                MemberPropertyMappingImpl
                                    .builder()
                                    .withName("Store Manager")
                                    .withColumn(FoodmartMappingSupplier.STORE_MANAGER_COLUMN_IN_STORE_RAGGED)
                                    .build()
                            ))
                            .build();
                        HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withName("Store2")
                            .withHasAll(true)
                            .withAllMemberName("All Stores")
                            .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE_RAGGED)
                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_RAGGED_TABLE).build())
                            .withLevels(List.of(level))
                            .build();

                        DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl.builder()
                        		.withOverrideDimensionName("Store2")
                        		.withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                        		.withDimension(StandardDimensionMappingImpl.builder()
                        			.withName("Store2")
                        			.withHierarchies(List.of(hierarchy)).build())
                            .build();
                        result.add(dimension);
                    }
                    return result;
                }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                "Sales",
                "  <Dimension name=\"Store2\" foreignKey=\"store_id\">\n"
                + "    <Hierarchy name=\"Store2\" hasAll=\"true\" allMemberName=\"All Stores\" primaryKey=\"store_id\">\n"
                + "      <Table name=\"store_ragged\"/>\n"
                + "      <Level name=\"Store2\" table=\"store_ragged\" column=\"store_id\" captionColumn=\"store_name\" uniqueMembers=\"true\">\n"
                + "           <Property name=\"Store Type\" column=\"store_type\" formatter=\""
                + DummyPropertyFormatter.class.getName()
                + "\"/>"
                + "           <Property name=\"Store Manager\" column=\"store_manager\"/>"
                + "     </Level>"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"));
         */
        try {
            withSchema(context, TestPropertyFormatterModifier::new);
            assertSimpleQuery(context.getConnectionWithDefaultRole());
            fail("expected exception");
        } catch (RuntimeException e) {
            checkThrowable(
                e,
                "Failed to load formatter class 'mondrian.test.SchemaTest$DummyPropertyFormatter' for property 'Store Type'.");
        }
    }

    /**
     * Bug <a href="http://jira.pentaho.com/browse/MONDRIAN-233">MONDRIAN-233,
     * "ClassCastException in AggQuerySpec"</a> occurs when two cubes
     * have the same fact table, distinct aggregate tables, and measures with
     * the same name.
     *
     * <p>This test case attempts to reproduce this issue by creating that
     * environment, but it found a different issue: a measure came back with a
     * cell value which was from a different measure. The root cause is
     * probably the same: when measures are registered in a star, they should
     * be qualified by cube name.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBugMondrian233(Context<?> context) {
        class TestBugMondrian233Modifier extends PojoMappingModifier {
            public TestBugMondrian233Modifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();

                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                		.withOverrideDimensionName("Time")
                		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                        .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                        .build();

                DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Product")
                        .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_PRODUCT))
                        .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                        .build();

                SumMeasureMappingImpl unitSalesMeasure = SumMeasureMappingImpl
                .builder()
                .withName("Unit Sales")
                .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                .withFormatString("Standard")
                .build();

                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("Sales2")
                    .withDefaultMeasure(unitSalesMeasure)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
                    		.build())
                    .withDimensionConnectors(List.of(d1, d2))
                    .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder()
                        	.withMeasures(List.of(unitSalesMeasure,
                        SumMeasureMappingImpl
                            .builder()
                            .withName("Store Cost")
                            .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_SALES_FACT_1997)

                            .withFormatString("#,###.00")
                            .build()
                    ))
                    .build()
                ))
                .build();
                result.add(c);
                result.addAll(super.cubes(cubes));
                return result;
            }
        }
        withSchema(context, TestBugMondrian233Modifier::new);
        // With bug, and with aggregates enabled, query against Sales returns
        // 565,238, which is actually the total for [Store Sales]. I think the
        // aggregate tables are getting crossed.
        final String expected =
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Row #0: 266,773\n";
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures]} on 0 from [Sales2]",
            expected);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures]} on 0 from [Sales]",
            expected);
    }

    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-303">
     * MONDRIAN-303, "Property column shifting when use captionColumn"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBugMondrian303(Context<?> context) {
        class TestBugMondrian303Modifier extends PojoMappingModifier {
            public TestBugMondrian303Modifier(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {

                    LevelMappingImpl level = LevelMappingImpl
                            .builder()
                            .withName("Store2")
                            .withColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE_RAGGED)
                            .withCaptionColumn(FoodmartMappingSupplier.STORE_NAME_COLUMN_IN_STORE_RAGGED)
                            .withUniqueMembers(true)
                            .withMemberProperties(List.of(
                            	MemberPropertyMappingImpl
                                    .builder()
                                    .withName("Store Type")
                                    .withColumn(FoodmartMappingSupplier.STORE_TYPE_COLUMN_IN_STORE_RAGGED)
                                    .build(),
                                MemberPropertyMappingImpl
                                    .builder()
                                    .withName("Store Manager")
                                    .withColumn(FoodmartMappingSupplier.STORE_MANAGER_COLUMN_IN_STORE_RAGGED)
                                    .build()
                            ))
                            .build();

                    HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withName("Store2")
                            .withHasAll(true)
                            .withAllMemberName("All Stores")
                            .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE_RAGGED)
                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_RAGGED_TABLE).build())
                            .withLevels(List.of(level))
                            .build();

                    DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl
                            .builder()
                            .withOverrideDimensionName("Store2")
                            .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("Store2")
                            	.withHierarchies(List.of(hierarchy)).build())
                            .build();
                        result.add(dimension);
                    }
                    return result;
            }

        }
        // In order to reproduce the problem a dimension specifying
        // captionColumn and Properties were required.
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "  <Dimension name=\"Store2\" foreignKey=\"store_id\">\n"
            + "    <Hierarchy name=\"Store2\" hasAll=\"true\" allMemberName=\"All Stores\" primaryKey=\"store_id\">\n"
            + "      <Table name=\"store_ragged\"/>\n"
            + "      <Level name=\"Store2\" table=\"store_ragged\" column=\"store_id\" captionColumn=\"store_name\" uniqueMembers=\"true\">\n"
            + "           <Property name=\"Store Type\" column=\"store_type\"/>"
            + "           <Property name=\"Store Manager\" column=\"store_manager\"/>"
            + "     </Level>"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"));
         */
        withSchema(context, TestBugMondrian303Modifier::new);
        // In the query below Mondrian (prior to the fix) would
        // return the store name instead of the store type.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH\n"
            + "   MEMBER [Measures].[StoreType] AS \n"
            + "   '[Store2].CurrentMember.Properties(\"Store Type\")'\n"
            + "SELECT\n"
            + "   NonEmptyCrossJoin({[Store2].[All Stores].children}, {[Product].[All Products]}) ON ROWS,\n"
            + "   { [Measures].[Store Sales], [Measures].[StoreType]} ON COLUMNS\n"
            + "FROM Sales",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Store Sales]}\n"
            + "{[Measures].[StoreType]}\n"
            + "Axis #2:\n"
            + "{[Store2].[Store2].[2], [Product].[Product].[All Products]}\n"
            + "{[Store2].[Store2].[3], [Product].[Product].[All Products]}\n"
            + "{[Store2].[Store2].[6], [Product].[Product].[All Products]}\n"
            + "{[Store2].[Store2].[7], [Product].[Product].[All Products]}\n"
            + "{[Store2].[Store2].[11], [Product].[Product].[All Products]}\n"
            + "{[Store2].[Store2].[13], [Product].[Product].[All Products]}\n"
            + "{[Store2].[Store2].[14], [Product].[Product].[All Products]}\n"
            + "{[Store2].[Store2].[15], [Product].[Product].[All Products]}\n"
            + "{[Store2].[Store2].[16], [Product].[Product].[All Products]}\n"
            + "{[Store2].[Store2].[17], [Product].[Product].[All Products]}\n"
            + "{[Store2].[Store2].[22], [Product].[Product].[All Products]}\n"
            + "{[Store2].[Store2].[23], [Product].[Product].[All Products]}\n"
            + "{[Store2].[Store2].[24], [Product].[Product].[All Products]}\n"
            + "Row #0: 4,739.23\n"
            + "Row #0: Small Grocery\n"
            + "Row #1: 52,896.30\n"
            + "Row #1: Supermarket\n"
            + "Row #2: 45,750.24\n"
            + "Row #2: Gourmet Supermarket\n"
            + "Row #3: 54,545.28\n"
            + "Row #3: Supermarket\n"
            + "Row #4: 55,058.79\n"
            + "Row #4: Supermarket\n"
            + "Row #5: 87,218.28\n"
            + "Row #5: Deluxe Supermarket\n"
            + "Row #6: 4,441.18\n"
            + "Row #6: Small Grocery\n"
            + "Row #7: 52,644.07\n"
            + "Row #7: Supermarket\n"
            + "Row #8: 49,634.46\n"
            + "Row #8: Supermarket\n"
            + "Row #9: 74,843.96\n"
            + "Row #9: Deluxe Supermarket\n"
            + "Row #10: 4,705.97\n"
            + "Row #10: Small Grocery\n"
            + "Row #11: 24,329.23\n"
            + "Row #11: Mid-Size Grocery\n"
            + "Row #12: 54,431.14\n"
            + "Row #12: Supermarket\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCubeWithOneDimensionOneMeasure(Context<?> context) {
        class TestCubeWithOneDimensionOneMeasureModifier extends PojoMappingModifier {
            public TestCubeWithOneDimensionOneMeasureModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();

                HierarchyMappingImpl h1 = ExplicitHierarchyMappingImpl.builder()
                        .withHasAll(true)
                        .withAllMemberName("All Media")
                        .withPrimaryKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_PROMOTION)
                        .withDefaultMember("All Media")
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PROMOTION_TABLE).build())
                        .withLevels(List.of(
                            LevelMappingImpl.builder()
                                .withName("Media Type")
                                .withColumn(FoodmartMappingSupplier.MEDIA_TYPE_COLUMN_IN_PROMOTION)
                                .withUniqueMembers(true)
                                .build()
                        ))
                        .build();

                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Promotion Media")
                        .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        		.withName("Promotion Media")
                        		.withHierarchies(List.of(h1))
                        		.build())
                        .build();

                SumMeasureMappingImpl unitSalesMeasure = SumMeasureMappingImpl
                    .builder()
                    .withName("Unit Sales")
                    .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                    .withFormatString("Standard")
                .build();

                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("OneDim")
                    .withDefaultMeasure(unitSalesMeasure)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
                    		.build())
                    .withDimensionConnectors(List.of(d1))
                    .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder()
                        	.withMeasures(List.of(unitSalesMeasure))
                    .build()
                ))
                .build();
                result.add(c);
                result.addAll(super.cubes(cubes));
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"OneDim\" defaultMeasure=\"Unit Sales\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <Dimension name=\"Promotion Media\" foreignKey=\"promotion_id\">\n"
            + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Media\" primaryKey=\"promotion_id\" defaultMember=\"All Media\">\n"
            + "      <Table name=\"promotion\"/>\n"
            + "      <Level name=\"Media Type\" column=\"media_type\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "</Cube>",
            null, null, null, null);
        withSchema(context, schema);
         */
        withSchema(context, TestCubeWithOneDimensionOneMeasureModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Promotion Media]} on columns from [OneDim]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Promotion Media].[Promotion Media].[All Media]}\n"
            + "Row #0: 266,773\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCubeWithOneDimensionUsageOneMeasure(Context<?> context) {
        class TestCubeWithOneDimensionUsageOneMeasureModifier extends PojoMappingModifier {
            public TestCubeWithOneDimensionUsageOneMeasureModifier(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));

                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Product")
                        .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_PRODUCT))
                        .build();


                SumMeasureMappingImpl unitSalesMeasure = SumMeasureMappingImpl
                    .builder()
                    .withName("Unit Sales")
                    .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                    .withFormatString("Standard")
                .build();

                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("OneDimUsage")
                    .withDefaultMeasure(unitSalesMeasure)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
                    		.build())
                    .withDimensionConnectors(List.of(d1))
                    .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder()
                        	.withMeasures(List.of(unitSalesMeasure))
                    .build()
                ))
                .build();
                result.add(c);
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"OneDimUsage\" defaultMeasure=\"Unit Sales\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <DimensionUsage name=\"Product\" source=\"Product\" foreignKey=\"product_id\"/>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "</Cube>",
            null, null, null, null);
        withSchema(context, schema);
        */
        withSchema(context, TestCubeWithOneDimensionUsageOneMeasureModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Product].Children} on columns from [OneDimUsage]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Product].[Product].[Drink]}\n"
            + "{[Product].[Product].[Food]}\n"
            + "{[Product].[Product].[Non-Consumable]}\n"
            + "Row #0: 24,597\n"
            + "Row #0: 191,940\n"
            + "Row #0: 50,236\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCubeHasFact(Context<?> context) {
        class TestCubeHasFactModifier extends PojoMappingModifier {
            public TestCubeHasFactModifier(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("Cube with caption")
                    //.withCaption("Cube with name")
                    .build();
                result.add(c);
                result.addAll(super.cubes(cubes));
                return result;
            }
        }

        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"Cube with caption\" caption=\"Cube with name\"/>\n",
            null, null, null, null);
        withSchema(context, schema);
         */
        withSchema(context, TestCubeHasFactModifier::new);
        Throwable throwable = null;
        try {
            assertSimpleQuery(context.getConnectionWithDefaultRole());
        } catch (Throwable e) {
            throwable = e;
        }
        checkThrowable(
            throwable,
            "Must specify fact table of cube 'Cube with caption'");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCubeCaption(Context<?> context) throws SQLException {
        class TestCubeCaptionModifier extends PojoMappingModifier {
            public TestCubeCaptionModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("Cube with caption")
                    //.withCaption("Cube with name")
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
                    		.build())
                    .build();
                VirtualCubeMappingImpl vc = VirtualCubeMappingImpl
                .builder()
                .withName("Warehouse and Sales with caption")
                //.withCaption("Warehouse and Sales with name")
                .withDefaultMeasure((MemberMappingImpl) look(FoodmartMappingSupplier.MEASURE_STORE_SALES))
                .withDimensionConnectors(List.of(
                	DimensionConnectorMappingImpl.builder()
                	.withOverrideDimensionName("Customers")
                	.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_CUSTOMERS))
                	.withPhysicalCube((PhysicalCubeMappingImpl) look(FoodmartMappingSupplier.CUBE_SALES))
                	.build()
                ))
                .build();
                result.add(c);
                result.add(vc);
                result.addAll(super.cubes(cubes));
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"Cube with caption\" caption=\"Cube with name\">"
            + "  <Table name='sales_fact_1997'/>"
            + "</Cube>\n",
            "<VirtualCube name=\"Warehouse and Sales with caption\" "
            + " caption=\"Warehouse and Sales with name\" "
            + "defaultMeasure=\"Store Sales\">\n"
            + "  <VirtualCubeDimension cubeName=\"Sales\" name=\"Customers\"/>\n"
            + "</VirtualCube>",
            null, null, null);
        withSchema(context, schema);
         */
        withSchema(context, TestCubeCaptionModifier::new);
        final List<Cube> cubes =
            context.getConnectionWithDefaultRole().getCatalog().getCubes();
        Optional<Cube> optionalCube1 = cubes.stream().filter(c -> "Cube with caption".equals(c.getName())).findFirst();
        final Cube cube = optionalCube1.orElseThrow(() -> new RuntimeException("Cube with name \"Cube with caption\" is absent"));
        assertEquals("Cube with caption", cube.getCaption());
        Optional<Cube> optionalCube2 = cubes.stream().filter(c -> "Warehouse and Sales with caption".equals(c.getName())).findFirst();
        final Cube cube2 =
            optionalCube2.orElseThrow(() -> new RuntimeException("Cube with name \"Warehouse and Sales with caption\" is absent"));
        assertEquals("Warehouse and Sales with caption", cube2.getCaption());
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCubeWithNoDimensions(Context<?> context) {
        class TestCubeWithNoDimensionsModifier extends PojoMappingModifier {
            public TestCubeWithNoDimensionsModifier(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                SumMeasureMappingImpl m = SumMeasureMappingImpl
                .builder()
                .withName("Unit Sales")
                .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                .withFormatString("Standard")
                .build();

                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("NoDim")
                    .withDefaultMeasure(m)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withMeasureGroups(List.of(
                    	MeasureGroupMappingImpl.builder().withMeasures(List.of(m)).build()
                    ))
                    .build();
                result.add(c);
                result.addAll(super.cubes(cubes));
                return result;
            }
        }
        withSchema(context, TestCubeWithNoDimensionsModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures].[Unit Sales]} on columns from [NoDim]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Row #0: 266,773\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCubeWithNoMeasuresFails(Context<?> context) {
        class TestCubeWithNoMeasuresFailsModifier extends PojoMappingModifier {
            public TestCubeWithNoMeasuresFailsModifier(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                HierarchyMappingImpl h1 = ExplicitHierarchyMappingImpl.builder()
                    .withHasAll(true)
                    .withAllMemberName("All Media")
                    .withPrimaryKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_PROMOTION)
                    .withDefaultMember("All Media")
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PROMOTION_TABLE).build())
                    .withLevels(List.of(
                        LevelMappingImpl.builder()
                            .withName("Media Type")
                            .withColumn(FoodmartMappingSupplier.MEDIA_TYPE_COLUMN_IN_PROMOTION)
                            .withUniqueMembers(true)
                            .build()
                    ))
                    .build();

                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                	.withOverrideDimensionName("Promotion Media")
                    .withForeignKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997)
                    .withDimension(StandardDimensionMappingImpl.builder()
                    .withName("Promotion Media")
                    .withHierarchies(List.of(h1)).build())
                    .build();

                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("NoMeasures")
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(d1))
                    .withMeasureGroups(List.of(
                    	MeasureGroupMappingImpl.builder()
                    	.withMeasures(List.of(
                                SumMeasureMappingImpl
                                .builder()
                                .withName("Unit Sales")
                                .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                                .withFormatString("Standard")
                                .build()
                        ))
                    	.build()
                    ))
                    .build();
                result.add(c);
                result.addAll(super.cubes(cubes));
                return result;
            }

        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"NoMeasures\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <Dimension name=\"Promotion Media\" foreignKey=\"promotion_id\">\n"
            + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Media\" primaryKey=\"promotion_id\" defaultMember=\"All Media\">\n"
            + "      <Table name=\"promotion\"/>\n"
            + "      <Level name=\"Media Type\" column=\"media_type\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "</Cube>",
            null, null, null, null);
        withSchema(context, schema);
         */
        withSchema(context, TestCubeWithNoMeasuresFailsModifier::new);
        // Does not fail with
        //    "Hierarchy '[Measures]' is invalid (has no members)"
        // because of the implicit [Fact Count] measure.
        assertSimpleQuery(context.getConnectionWithDefaultRole());
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCubeWithOneCalcMeasure(Context<?> context) {
        class TestCubeWithOneCalcMeasureModifier extends PojoMappingModifier {
            public TestCubeWithOneCalcMeasureModifier(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                HierarchyMappingImpl h1 = ExplicitHierarchyMappingImpl.builder()
                    .withHasAll(true)
                    .withAllMemberName("All Media")
                    .withPrimaryKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_PROMOTION)
                    .withDefaultMember("All Media")
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PROMOTION_TABLE).build())
                    .withLevels(List.of(
                        LevelMappingImpl.builder()
                            .withName("Media Type")
                            .withColumn(FoodmartMappingSupplier.MEDIA_TYPE_COLUMN_IN_PROMOTION)
                            .withUniqueMembers(true)
                            .build()
                    ))
                    .build();

                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                    	.withOverrideDimensionName("Promotion Media")
                        .withForeignKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        .withName("Promotion Media")
                        .withHierarchies(List.of(h1)).build())
                        .build();

                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("OneCalcMeasure")
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(d1))
                    .withCalculatedMembers(List.of(
                        CalculatedMemberMappingImpl
                            .builder()
                            .withName("One")
                            //.withDimension("Measures")
                            .withFormula("1")
                            .build()
                    ))
                    .build();
                result.add(c);
                result.addAll(super.cubes(cubes));
                return result;

            }

        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"OneCalcMeasure\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <Dimension name=\"Promotion Media\" foreignKey=\"promotion_id\">\n"
            + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Media\" primaryKey=\"promotion_id\" defaultMember=\"All Media\">\n"
            + "      <Table name=\"promotion\"/>\n"
            + "      <Level name=\"Media Type\" column=\"media_type\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <CalculatedMember\n"
            + "      name=\"One\"\n"
            + "      dimension=\"Measures\"\n"
            + "      formula=\"1\"/>\n"
            + "</Cube>",
            null, null, null, null);
        withSchema(context, schema);
        */

        withSchema(context, TestCubeWithOneCalcMeasureModifier::new);
        // Because there are no explicit stored measures, the default measure is
        // the implicit stored measure, [Fact Count]. Stored measures, even
        // non-visible ones, come before calculated measures.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures]} on columns from [OneCalcMeasure]\n"
            + "where [Promotion Media].[TV]",
            "Axis #0:\n"
            + "{[Promotion Media].[Promotion Media].[TV]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Fact Count]}\n"
            + "Row #0: 1,171\n");
    }

    /**
     * Test case for feature
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-960">MONDRIAN-960,
     * "Ability to define non-measure calculated members in a cube under a
     * specifc parent"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCalcMemberInCube(Context<?> context) {
        class TestCalcMemberInCubeModifier1 extends PojoMappingModifier {
            public TestCalcMemberInCubeModifier1(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends CalculatedMemberMapping> cubeCalculatedMembers(CubeMapping cube) {
                List<CalculatedMemberMapping> result = new ArrayList<>();
                result.addAll(super.cubeCalculatedMembers(cube));
                if ("Sales".equals(cube.getName())) {
                    CalculatedMemberMappingImpl calculatedMember = CalculatedMemberMappingImpl
                        .builder()
                        .withName("SF and LA")
                        .withHierarchy((HierarchyMappingImpl) look(FoodmartMappingSupplier.storeHierarchy))
                        .withParent("[Store].[Store].[USA].[CA]")
                        .withFormula("[Store].[Store].[USA].[CA].[San Francisco] + [Store].[Store].[USA].[CA].[Los Angeles]")
                        .build();
                    result.add(calculatedMember);
                }
                return result;
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                "Sales",
                null,
                null,
                "<CalculatedMember\n"
                + "      name='SF and LA'\n"
                + "      hierarchy='[Store]'\n"
                + "      parent='[Store].[USA].[CA]'>\n"
                + "  <Formula>\n"
                + "    [Store].[USA].[CA].[San Francisco]\n"
                + "    + [Store].[USA].[CA].[Los Angeles]\n"
                + "  </Formula>\n"
                + "</CalculatedMember>",
                null, false));
        */
        withSchema(context, TestCalcMemberInCubeModifier1::new);
        // Because there are no explicit stored measures, the default measure is
        // the implicit stored measure, [Fact Count]. Stored measures, even
        // non-visible ones, come before calculated measures.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Store].[Store].[USA].[CA].[SF and LA]} on columns from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[Store].[USA].[CA].[SF and LA]}\n"
            + "Row #0: 27,780\n");

        // Now access the same member using a path that is not its unique name.
        // Only works with new name resolver (if ssas = true).

            assertQueryReturns(context.getConnectionWithDefaultRole(),
                "select {[Store].[Store].[All Stores].[USA].[CA].[SF and LA]} on columns from [Sales]",
                "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Store].[Store].[USA].[CA].[SF and LA]}\n"
                + "Row #0: 27,780\n");

        // Test where hierarchy & dimension both specified. should fail
        try {
            class TestCalcMemberInCubeModifier2 extends PojoMappingModifier {
                public TestCalcMemberInCubeModifier2(CatalogMapping catalogMapping) {
                    super(catalogMapping);
                }

                @Override
                protected List<? extends CalculatedMemberMapping> cubeCalculatedMembers(CubeMapping cube) {
                    List<CalculatedMemberMapping> result = new ArrayList<>();
                    result.addAll(super.cubeCalculatedMembers(cube).stream().filter(cm -> !"SF and LA".equals(cm.getName())).toList());
                    if ("Sales".equals(cube.getName())) {
                    	CalculatedMemberMappingImpl calculatedMember = CalculatedMemberMappingImpl
                            .builder()
                            .withName("SF and LA")
                            .withHierarchy((HierarchyMappingImpl) look(FoodmartMappingSupplier.storeHierarchy))
                            //.dimension("[Store]")
                            .withParent("[Store].[Store].[USA].[CA]")
                            .withFormula("[Store].[Store].[USA].[CA].[San Francisco] + [Store].[Store].[USA].[CA].[Los Angeles]")
                            .build();
                        result.add(calculatedMember);
                    }
                    return result;
                }
            }
            /*
            ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                    "Sales",
                    null,
                    null,
                    "<CalculatedMember\n"
                    + "      name='SF and LA'\n"
                    + "      hierarchy='[Store]'\n"
                    + "      dimension='[Store]'\n"
                    + "      parent='[Store].[USA].[CA]'>\n"
                    + "  <Formula>\n"
                    + "    [Store].[USA].[CA].[San Francisco]\n"
                    + "    + [Store].[USA].[CA].[Los Angeles]\n"
                    + "  </Formula>\n"
                    + "</CalculatedMember>",
                    null, false));
             */
            ((TestContext)context).setCatalogMappingSupplier(new FoodmartMappingSupplier());
            withSchema(context, TestCalcMemberInCubeModifier2::new);
            assertQueryReturns(context.getConnectionWithDefaultRole(),
                "select {[Store].[Store].[All Stores].[USA].[CA].[SF and LA]} on columns from [Sales]",
                "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Store].[Store].[USA].[CA].[SF and LA]}\n"
                + "Row #0: 27,780\n");
            //fail();
        } catch (OlapRuntimeException e) {
        	//dimension was removed in new model
            //assertTrue(
            //    e.getMessage().contains(
            //        "Cannot specify both a dimension and hierarchy"
            //        + " for calculated member 'SF and LA' in cube 'Sales'"));
        }

        // test where hierarchy is not uname of valid hierarchy. should fail
        try {
            class TestCalcMemberInCubeModifier3 extends PojoMappingModifier {
                public TestCalcMemberInCubeModifier3(CatalogMapping catalogMapping) {
                    super(catalogMapping);
                }

                @Override
                protected List<? extends CalculatedMemberMapping> cubeCalculatedMembers(CubeMapping cube) {
                    List<CalculatedMemberMapping> result = new ArrayList<>();
                    result.addAll(super.cubeCalculatedMembers(cube).stream().filter(cm -> !"SF and LA".equals(cm.getName())).toList());
                    if ("Sales".equals(cube.getName())) {
                    	CalculatedMemberMappingImpl calculatedMember = CalculatedMemberMappingImpl
                            .builder()
                            .withName("SF and LA")
                            .withHierarchy((HierarchyMappingImpl) look(FoodmartMappingSupplier.HIERARCHY_STORE_TYPE))
                            .withParent("[Store].[Store].[USA].[CA]")
                            .withFormula("[Store].[Store].[USA].[CA].[San Francisco] + [Store].[Store].[USA].[CA].[Los Angeles]")
                            .build();
                        result.add(calculatedMember);
                    }
                    return result;
                }
            }
            /*
            ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                    "Sales",
                    null,
                    null,
                    "<CalculatedMember\n"
                    + "      name='SF and LA'\n"
                    + "      hierarchy='[Bacon]'\n"
                    + "      parent='[Store].[USA].[CA]'>\n"
                    + "  <Formula>\n"
                    + "    [Store].[USA].[CA].[San Francisco]\n"
                    + "    + [Store].[USA].[CA].[Los Angeles]\n"
                    + "  </Formula>\n"
                    + "</CalculatedMember>",
                    null, false));
             */
            ((TestContext)context).setCatalogMappingSupplier(new FoodmartMappingSupplier());
            withSchema(context, TestCalcMemberInCubeModifier3::new);
            assertQueryReturns(context.getConnectionWithDefaultRole(),
                "select {[Store].[Store].[All Stores].[USA].[CA].[SF and LA]} on columns from [Sales]",
                "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Store].[Store].[USA].[CA].[SF and LA]}\n"
                + "Row #0: 27,780\n");
            fail();
        } catch (OlapRuntimeException e) {
            assertTrue(
                e.getMessage().contains(
                    "The calculated member 'SF and LA' in cube 'Sales' is defined for hierarchy '[Store Type].[Store Type]' but its parent member is not part of that hierarchy"));
        }

        // test where formula is invalid. should fail
        try {
            class TestCalcMemberInCubeModifier4 extends PojoMappingModifier {
                public TestCalcMemberInCubeModifier4(CatalogMapping catalogMapping) {
                    super(catalogMapping);
                }

                @Override
                protected List<? extends CalculatedMemberMapping> cubeCalculatedMembers(CubeMapping cube) {
                    List<CalculatedMemberMapping> result = new ArrayList<>();
                    result.addAll(super.cubeCalculatedMembers(cube).stream().filter(cm -> !"SF and LA".equals(cm.getName())).toList());
                    if ("Sales".equals(cube.getName())) {
                    	CalculatedMemberMappingImpl calculatedMember = CalculatedMemberMappingImpl
                            .builder()
                            .withName("SF and LA")
                            .withHierarchy((HierarchyMappingImpl) look(FoodmartMappingSupplier.storeHierarchy))
                            .withParent("[Store].[Store].[USA].[CA]")
                            .withFormula("Baconating!")
                            .build();
                        result.add(calculatedMember);
                    }
                    return result;
                }
            }
            /*
            ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                    "Sales",
                    null,
                    null,
                    "<CalculatedMember\n"
                    + "      name='SF and LA'\n"
                    + "      hierarchy='[Store]'\n"
                    + "      parent='[Store].[USA].[CA]'>\n"
                    + "  <Formula>\n"
                    + "    Baconating!\n"
                    + "  </Formula>\n"
                    + "</CalculatedMember>",
                    null, false));
             */
            ((TestContext)context).setCatalogMappingSupplier(new FoodmartMappingSupplier());
            withSchema(context, TestCalcMemberInCubeModifier4::new);
            assertQueryReturns(context.getConnectionWithDefaultRole(),
                "select {[Store].[Store].[All Stores].[USA].[CA].[SF and LA]} on columns from [Sales]",
                "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Store].[Store].[USA].[CA].[SF and LA]}\n"
                + "Row #0: 27,780\n");
            fail();
        } catch (OlapRuntimeException e) {
            assertTrue(
                e.getMessage().contains(
                    "Named set in cube 'Sales' has bad formula"));
        }

        // Test where parent is invalid. should fail
        try {
            class TestCalcMemberInCubeModifier5 extends PojoMappingModifier {
                public TestCalcMemberInCubeModifier5(CatalogMapping catalogMapping) {
                    super(catalogMapping);
                }

                @Override
                protected List<? extends CalculatedMemberMapping> cubeCalculatedMembers(CubeMapping cube) {
                    List<CalculatedMemberMapping> result = new ArrayList<>();
                    result.addAll(super.cubeCalculatedMembers(cube).stream().filter(cm -> !"SF and LA".equals(cm.getName())).toList());
                    if ("Sales".equals(cube.getName())) {
                    	CalculatedMemberMappingImpl calculatedMember = CalculatedMemberMappingImpl
                            .builder()
                            .withName("SF and LA")
                            .withHierarchy((HierarchyMappingImpl) look(FoodmartMappingSupplier.storeHierarchy))
                            .withParent("[Store].[USA].[CA].[Baconville]")
                            .withFormula("[Store].[USA].[CA].[San Francisco] + [Store].[USA].[CA].[Los Angeles]")
                            .build();
                        result.add(calculatedMember);
                    }
                    return result;
                }
            }
            /*
            ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                    "Sales",
                    null,
                    null,
                    "<CalculatedMember\n"
                    + "      name='SF and LA'\n"
                    + "      hierarchy='[Store]'\n"
                    + "      parent='[Store].[USA].[CA].[Baconville]'>\n"
                    + "  <Formula>\n"
                    + "    [Store].[USA].[CA].[San Francisco]\n"
                    + "    + [Store].[USA].[CA].[Los Angeles]\n"
                    + "  </Formula>\n"
                    + "</CalculatedMember>",
                    null, false));
             */
            ((TestContext)context).setCatalogMappingSupplier(new FoodmartMappingSupplier());
            withSchema(context, TestCalcMemberInCubeModifier5::new);
            assertQueryReturns(context.getConnectionWithDefaultRole(),
                "select {[Store].[All Stores].[USA].[CA].[SF and LA]} on columns from [Sales]",
                "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Store].[Store].[USA].[CA].[SF and LA]}\n"
                + "Row #0: 27,780\n");
            fail();
        } catch (OlapRuntimeException e) {
            assertTrue(
                e.getMessage().contains(
                    "Cannot find a parent with name '[Store].[USA].[CA]"
                    + ".[Baconville]' for calculated member 'SF and LA'"
                    + " in cube 'Sales'"));
        }

        // test where parent is not in same hierarchy as hierarchy. should fail
        try {
            class TestCalcMemberInCubeModifier6 extends PojoMappingModifier {
                public TestCalcMemberInCubeModifier6(CatalogMapping catalogMapping) {
                    super(catalogMapping);
                }

                @Override
                protected List<? extends CalculatedMemberMapping> cubeCalculatedMembers(CubeMapping cube) {
                    List<CalculatedMemberMapping> result = new ArrayList<>();
                    result.addAll(super.cubeCalculatedMembers(cube).stream().filter(cm -> !"SF and LA".equals(cm.getName())).toList());
                    if ("Sales".equals(cube.getName())) {
                    	CalculatedMemberMappingImpl calculatedMember = CalculatedMemberMappingImpl
                            .builder()
                            .withName("SF and LA")
                            .withHierarchy((HierarchyMappingImpl) look(FoodmartMappingSupplier.HIERARCHY_STORE_TYPE))
                            .withParent("[Store].[Store].[USA].[CA]")
                            .withFormula("[Store].[Store].[USA].[CA].[San Francisco] + [Store].[Store].[USA].[CA].[Los Angeles]")
                            .build();
                        result.add(calculatedMember);
                    }
                    return result;
                }

            }
            /*
            ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                    "Sales",
                    null,
                    null,
                    "<CalculatedMember\n"
                    + "      name='SF and LA'\n"
                    + "      hierarchy='[Store Type]'\n"
                    + "      parent='[Store].[USA].[CA]'>\n"
                    + "  <Formula>\n"
                    + "    [Store].[USA].[CA].[San Francisco]\n"
                    + "    + [Store].[USA].[CA].[Los Angeles]\n"
                    + "  </Formula>\n"
                    + "</CalculatedMember>",
                    null, false));
             */
            ((TestContext)context).setCatalogMappingSupplier(new FoodmartMappingSupplier());
            withSchema(context, TestCalcMemberInCubeModifier6::new);
            assertQueryReturns(context.getConnectionWithDefaultRole(),
                "select {[Store].[Store].[All Stores].[USA].[CA].[SF and LA]} on columns from [Sales]",
                "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Store].[Store].[Store].[USA].[CA].[SF and LA]}\n"
                + "Row #0: 27,780\n");
            fail();
        } catch (OlapRuntimeException e) {
            assertTrue(
                e.getMessage().contains(
                    "The calculated member 'SF and LA' in cube 'Sales'"
                    + " is defined for hierarchy '[Store Type].[Store Type]' but its"
                    + " parent member is not part of that hierarchy"));
        }

        // test where calc member has no formula (formula attribute or
        //   embedded element); should fail
        try {
            class TestCalcMemberInCubeModifier7 extends PojoMappingModifier {
                public TestCalcMemberInCubeModifier7(CatalogMapping catalogMapping) {
                    super(catalogMapping);
                }

                @Override
                protected List<? extends CalculatedMemberMapping> cubeCalculatedMembers(CubeMapping cube) {
                    List<CalculatedMemberMapping> result = new ArrayList<>();
                    result.addAll(super.cubeCalculatedMembers(cube).stream().filter(cm -> !"SF and LA".equals(cm.getName())).toList());
                    if ("Sales".equals(cube.getName())) {
                    	CalculatedMemberMappingImpl calculatedMember = CalculatedMemberMappingImpl
                            .builder()
                            .withName("SF and LA")
                            .withHierarchy((HierarchyMappingImpl) look(FoodmartMappingSupplier.storeHierarchy))
                            .withParent("[Store].[Store].[USA].[CA]")
                            .withFormula("")
                            .build();
                        result.add(calculatedMember);
                    }
                    return result;
                }

            }
            /*
            ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                    "Sales",
                    null,
                    null,
                    "<CalculatedMember\n"
                    + "      name='SF and LA'\n"
                    + "      hierarchy='[Store]'\n"
                    + "      parent='[Store].[USA].[CA]'>\n"
                    + "  <Formula>\n"
                    + "  </Formula>\n"
                    + "</CalculatedMember>",
                    null, false));
             */
            ((TestContext)context).setCatalogMappingSupplier(new FoodmartMappingSupplier());
            withSchema(context, TestCalcMemberInCubeModifier7::new);
            assertQueryReturns(context.getConnectionWithDefaultRole(),
                "select {[Store].[Store].[All Stores].[USA].[CA].[SF and LA]} on columns from [Sales]",
                "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Store].[Store].[USA].[CA].[SF and LA]}\n"
                + "Row #0: 27,780\n");
            fail();
        } catch (OlapRuntimeException e) {
            assertTrue(
                e.getMessage().contains(
                    "Named set in cube 'Sales' has bad formula"));
        }
    }

    /**
     * this test triggers an exception out of the aggregate table manager
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAggTableSupportOfSharedDims(Context<?> context) {
        class TestAggTableSupportOfSharedDimsModifier extends PojoMappingModifier {
            public TestAggTableSupportOfSharedDimsModifier(CatalogMapping catalogMapping) {
                    super(catalogMapping);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();

                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                	.withOverrideDimensionName("Time")
                	.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                    .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                    .build();
                DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                    .withOverrideDimensionName("Time2")
                    .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                    .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                    .build();
                DimensionConnectorMappingImpl d3 = DimensionConnectorMappingImpl.builder()
                    .withOverrideDimensionName("Store")
                    .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_STORE_WITH_QUERY_STORE))
                    .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                    .build();

                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("Sales Two Dimensions")
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(d1, d2, d3))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                        SumMeasureMappingImpl.builder()
                            .withName("Unit Sales")
                            .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                            .withFormatString("Standard")
                            .build(),
                        SumMeasureMappingImpl.builder()
                            .withName("Store Cost")
                            .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_SALES_FACT_1997)

                            .withFormatString("#,###.00")
                            .build()
                    		)).build()))
                    .build();
                result.add(c);
                result.addAll(super.cubes(cubes));
                return result;
            }
        }
        if (Bug.BugMondrian361Fixed) {
            /*
            String baseSchema = TestUtil.getRawSchema(context);
            String schema = SchemaUtil.getSchema(baseSchema,
                null,
                "<Cube name=\"Sales Two Dimensions\">\n"
                + "  <Table name=\"sales_fact_1997\"/>\n"
                + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
                + "  <DimensionUsage name=\"Time2\" source=\"Time\" foreignKey=\"product_id\"/>\n"
                + "  <DimensionUsage name=\"Store\" source=\"Store\" foreignKey=\"store_id\"/>\n"
                + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" "
                + "   formatString=\"Standard\"/>\n"
                + "  <Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\""
                + "   formatString=\"#,###.00\"/>\n"
                + "</Cube>",
                null,
                null,
                null,
                null);

            withSchema(context, schema);
             */
            withSchema(context, TestAggTableSupportOfSharedDimsModifier::new);
            assertQueryReturns(context.getConnectionWithDefaultRole(),
                "select\n"
                + " {[Time2].[1997]} on columns,\n"
                + " {[Time].[1997].[Q3]} on rows\n"
                + "From [Sales Two Dimensions]",
                "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Time2].[1997]}\n"
                + "Axis #2:\n"
                + "{[Time].[1997].[Q3]}\n"
                + "Row #0: 16,266\n");

            SystemWideProperties props = SystemWideProperties.instance();

            // turn off caching
            ((TestContextImpl)context).setDisableCaching(true);

            // re-read aggregates
            ((TestContextImpl)context).setUseAggregates(true);
            ((TestContextImpl)context).setReadAggregates(false);
            ((TestContextImpl)context).setReadAggregates(true);

            // force reloading of aggregates, which currently throws an
            // exception
        }
    }

    /**
     * Verifies that RolapHierarchy.tableExists() supports views.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLevelTableAttributeAsView(Context<?> context) {
        class TestLevelTableAttributeAsViewModifier extends PojoMappingModifier {
            public TestLevelTableAttributeAsViewModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                PhysicalColumnMappingImpl GENDER_COLUMN_IN_CUSTOMER = PhysicalColumnMappingImpl.builder().withName("gender").withDataType(ColumnDataType.VARCHAR).withColumnSize(30).build();
                PhysicalColumnMappingImpl CUSTOMER_ID_COLUMN_IN_CUSTOMER = PhysicalColumnMappingImpl.builder().withName("customer_id").withDataType(ColumnDataType.INTEGER).build();
                SqlSelectQueryMappingImpl v1 = SqlSelectQueryMappingImpl.builder()
                    .withAlias("customer")
                    .withSql(
                            ((SqlViewMappingImpl.Builder) SqlViewMappingImpl.builder()
                            .withColumns(List.of(GENDER_COLUMN_IN_CUSTOMER, CUSTOMER_ID_COLUMN_IN_CUSTOMER)))
                            .withSqlStatements(List.of(
                                SqlStatementMappingImpl.builder().withSql("SELECT * FROM customer").withDialects(List.of("generic")).build(),
                                SqlStatementMappingImpl.builder().withSql("SELECT * FROM \"customer\"").withDialects(
                                List.of("oracle", "derby", "hsqldb", "luciddb", "neoview", "netezza", "db2")).build()
                            ))
                    .build()).build();

                HierarchyMappingImpl h1 = ExplicitHierarchyMappingImpl.builder()
                    .withHasAll(true)
                    .withAllMemberName("All Gender")
                    .withPrimaryKey(CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                    .withQuery(v1)
                    .withLevels(List.of(
                        LevelMappingImpl.builder()
                            .withName("Gender")
                            //.withTable("gender2") //TODO use alias
                            .withColumn(GENDER_COLUMN_IN_CUSTOMER)
                            .withUniqueMembers(true)
                            .build()
                    ))
                    .build();

                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                	.withOverrideDimensionName("Gender2")
                    .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                    .withDimension(StandardDimensionMappingImpl.builder()
                    		.withName("Gender2")
                    		.withHierarchies(List.of(h1))
                    		.build())
                    .build();

                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("GenderCube")
                    .withQuery(TableQueryMappingImpl.builder()
                    		.withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
                    		.withAggregationExcludes(List.of(
                    			AggregationExcludeMappingImpl.builder().withPattern("agg_g_ms_pcat_sales_fact_1997").build()
                    		))
                    		.build())
                    .withDimensionConnectors(List.of(d1))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                    		.withMeasures(List.of(
                                SumMeasureMappingImpl.builder()
                                    .withName("Unit Sales")
                                    .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                                    .withFormatString("Standard")
                                    .build(),
                                SumMeasureMappingImpl.builder()
                                    .withName("Store Cost")
                                    .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_SALES_FACT_1997)

                                    .withFormatString("#,###.00")
                                    .build()
                            ))
                    		.build()))
                    .build();
                result.add(c);
                result.addAll(super.cubes(cubes));
                return result;
            }

        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"GenderCube\">\n"
            + "  <Table name=\"sales_fact_1997\">\n"
            + "    <AggExclude pattern=\"agg_g_ms_pcat_sales_fact_1997\"/>\n"
            + "  </Table>\n"
            + "<Dimension name=\"Gender2\" foreignKey=\"customer_id\">\n"
            + "  <Hierarchy hasAll=\"true\" allMemberName=\"All Gender\" primaryKey=\"customer_id\">\n"
            + "    <View alias=\"gender2\">\n"
            + "      <SQL dialect=\"generic\">\n"
            + "        <![CDATA[SELECT * FROM customer]]>\n"
            + "      </SQL>\n"
            + "      <SQL dialect=\"oracle\">\n"
            + "        <![CDATA[SELECT * FROM \"customer\"]]>\n"
            + "      </SQL>\n"
            + "      <SQL dialect=\"derby\">\n"
            + "        <![CDATA[SELECT * FROM \"customer\"]]>\n"
            + "      </SQL>\n"
            + "      <SQL dialect=\"hsqldb\">\n"
            + "        <![CDATA[SELECT * FROM \"customer\"]]>\n"
            + "      </SQL>\n"
            + "      <SQL dialect=\"luciddb\">\n"
            + "        <![CDATA[SELECT * FROM \"customer\"]]>\n"
            + "      </SQL>\n"
            + "      <SQL dialect=\"neoview\">\n"
            + "        <![CDATA[SELECT * FROM \"customer\"]]>\n"
            + "      </SQL>\n"
            + "      <SQL dialect=\"netezza\">\n"
            + "        <![CDATA[SELECT * FROM \"customer\"]]>\n"
            + "      </SQL>\n"
            + "      <SQL dialect=\"db2\">\n"
            + "        <![CDATA[SELECT * FROM \"customer\"]]>\n"
            + "      </SQL>\n"
            + "    </View>\n"
            + "    <Level name=\"Gender\" table=\"gender2\" column=\"gender\" uniqueMembers=\"true\"/>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "</Cube>",
            null, null, null, null);
        withSchema(context, schema);
         */

        withSchema(context, TestLevelTableAttributeAsViewModifier::new);
        if (!getDialect(context.getConnectionWithDefaultRole()).allowsFromQuery()) {
            return;
        }

        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "select {[Gender2].members} on columns from [GenderCube]");

        assertEqualsVerbose(
            "[Gender2].[Gender2].[All Gender]\n"
            + "[Gender2].[Gender2].[F]\n"
            + "[Gender2].[Gender2].[M]",
            TestUtil.toString(
                result.getAxes()[0].getPositions()));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testInvalidSchemaAccess(Context<?> context) {
        class TestInvalidSchemaAccess extends PojoMappingModifier {
            public TestInvalidSchemaAccess(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected List<? extends AccessRoleMapping> catalogAccessRoles(CatalogMapping schema) {
                List<AccessRoleMapping> result = new ArrayList<>();
                result.addAll(super.catalogAccessRoles(schema));
                result.add(AccessRoleMappingImpl.builder()
                    .withName("Role1")
                    .withAccessCatalogGrants(List.of(AccessCatalogGrantMappingImpl.builder().build()))
                    .build());
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null, null, null, null, null,
            "<Role name=\"Role1\">\n"
            + "  <SchemaGrant access=\"invalid\"/>\n"
            + "</Role>");
        withSchema(context, schema);
         */
        withSchema(context, TestInvalidSchemaAccess::new);
        assertQueryThrows(context, List.of("Role1"),
            "select from [Sales]",
            "Cannot invoke \"org.eclipse.daanse.rolap.mapping.api.model.enums.AccessCatalog.getValue()\" because the return value of \"org.eclipse.daanse.rolap.mapping.api.model.AccessCatalogGrantMapping.getAccess()\" is null");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAllMemberNoStringReplace(Context<?> context) {
        class TestAllMemberNoStringReplaceModifier extends PojoMappingModifier {
            public TestAllMemberNoStringReplaceModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();

                LevelMappingImpl l11 = LevelMappingImpl.builder().withName("Years").withColumn(FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY).withUniqueMembers(true).withLevelType(LevelType.TIME_YEARS).build();
                LevelMappingImpl l12 = LevelMappingImpl.builder().withName("Quarters").withColumn(FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY).withUniqueMembers(false).withLevelType(LevelType.TIME_QUARTERS).build();
                LevelMappingImpl l13 = LevelMappingImpl.builder().withName("Months").withColumn(FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY).withUniqueMembers(false).withLevelType(LevelType.TIME_MONTHS).build();
                HierarchyMappingImpl h1 = ExplicitHierarchyMappingImpl.builder().withName("CALENDAR")
                    .withHasAll(true).withAllMemberName("All TIME(CALENDAR)").withPrimaryKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.TIME_BY_DAY_TABLE).build())
                    .withLevels(List.of(l11, l12, l13))
                    .build();

                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                    	.withOverrideDimensionName("TIME")
                        .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(TimeDimensionMappingImpl.builder()
                        	.withName("TIME")
                        	.withHierarchies(List.of(h1)).build())
                        .build();

                DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                    	.withOverrideDimensionName("Store")
                        .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(FoodmartMappingSupplier.DIMENSION_STORE_WITH_QUERY_STORE)
                        .build();

                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("Sales Special Time")
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(d1, d2))
                    .withMeasureGroups(List.of(
                    	MeasureGroupMappingImpl.builder()
                    	.withMeasures(List.of(
                            SumMeasureMappingImpl
                                .builder()
                                .withName("Unit Sales")
                                .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                                .withFormatString("Standard")
                                .build(),
                            SumMeasureMappingImpl
                                .builder()
                                .withName("Store Cost")
                                .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_SALES_FACT_1997)

                                .withFormatString("#,###.00")
                                .build()
                    	))
                    	.build()
                    ))
                    .build();
                result.add(c);
                result.addAll(super.cubes(cubes));
                return result;
            }
        }

        withSchema(context, TestAllMemberNoStringReplaceModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select [TIME].[CALENDAR].[All TIME(CALENDAR)] on columns\n"
            + "from [Sales Special Time]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[TIME].[CALENDAR].[All TIME(CALENDAR)]}\n"
            + "Row #0: 266,773\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUnionRole(Context<?> context) {
        class TestUnionRoleModifier extends PojoMappingModifier {
            public TestUnionRoleModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends AccessRoleMapping> catalogAccessRoles(CatalogMapping schema) {
            	AccessRoleMappingImpl role1;
            	AccessRoleMappingImpl role2;
            	AccessRoleMappingImpl role1Plus2;
                List<AccessRoleMapping> result = new ArrayList<>();
                result.addAll(super.catalogAccessRoles(schema));
                result.add(role1 = AccessRoleMappingImpl.builder()
                    .withName("Role1")
                    .withAccessCatalogGrants(List.of(AccessCatalogGrantMappingImpl.builder().withAccess(AccessCatalog.ALL).build()))
                    .build());
                result.add(role2 = AccessRoleMappingImpl.builder().withName("Role2")
                        .withAccessCatalogGrants(List.of(AccessCatalogGrantMappingImpl.builder().withAccess(AccessCatalog.ALL).build()))
                        .build());
                    result.add(role1Plus2 = AccessRoleMappingImpl.builder().withName("Role1Plus2")
                    	.withReferencedAccessRoles(List.of(
                    			role1,
                    			role2
                        ))
                        .build());
                    result.add(AccessRoleMappingImpl.builder().withName("Role1Plus2Plus1")
                        	.withReferencedAccessRoles(List.of(
                        		role1Plus2,
                        		role1
                            ))
                        .build());
                    return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null, null, null, null, null,
            "<Role name=\"Role1\">\n"
            + "  <SchemaGrant access=\"all\"/>\n"
            + "</Role>\n"
            + "<Role name=\"Role2\">\n"
            + "  <SchemaGrant access=\"all\"/>\n"
            + "</Role>\n"
            + "<Role name=\"Role1Plus2\">\n"
            + "  <Union>\n"
            + "    <RoleUsage roleName=\"Role1\"/>\n"
            + "    <RoleUsage roleName=\"Role2\"/>\n"
            + "  </Union>\n"
            + "</Role>\n"
            + "<Role name=\"Role1Plus2Plus1\">\n"
            + "  <Union>\n"
            + "    <RoleUsage roleName=\"Role1Plus2\"/>\n"
            + "    <RoleUsage roleName=\"Role1\"/>\n"
            + "  </Union>\n"
            + "</Role>\n");
        withSchema(context, schema);
         */
        withSchema(context, TestUnionRoleModifier::new);
        assertQueryReturns(((TestContext)context).getConnection(new ConnectionProps(List.of("Role1Plus2Plus1"))),
            "select from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "266,773");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUnionRoleContainsGrants(Context<?> context) {
        class TestUnionRoleContainsGrantsModifier extends PojoMappingModifier {
            public TestUnionRoleContainsGrantsModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends AccessRoleMapping> catalogAccessRoles(CatalogMapping schema) {
            	AccessRoleMappingImpl role1;
                List<AccessRoleMapping> result = new ArrayList<>();
                result.addAll(super.catalogAccessRoles(schema));
                result.add(role1 = AccessRoleMappingImpl.builder()
                    .withName("Role1")
                    .withAccessCatalogGrants(List.of(AccessCatalogGrantMappingImpl.builder().withAccess(AccessCatalog.ALL).build()))
                    .build());
                result.add(AccessRoleMappingImpl.builder().withName("Role1Plus2")
                    .withAccessCatalogGrants(List.of(AccessCatalogGrantMappingImpl.builder().withAccess(AccessCatalog.ALL).build()))
                    .withReferencedAccessRoles(List.of(role1, role1))
                    .build());
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null, null, null, null, null,
            "<Role name=\"Role1\">\n"
            + "  <SchemaGrant access=\"all\"/>\n"
            + "</Role>\n"
            + "<Role name=\"Role1Plus2\">\n"
            + "  <SchemaGrant access=\"all\"/>\n"
            + "  <Union>\n"
            + "    <RoleUsage roleName=\"Role1\"/>\n"
            + "    <RoleUsage roleName=\"Role1\"/>\n"
            + "  </Union>\n"
            + "</Role>\n");
        withSchema(context, schema);
         */
        withSchema(context, TestUnionRoleContainsGrantsModifier::new);
        assertQueryThrows(context, List.of("Role1Plus2"),
            "select from [Sales]", "Union role must not contain grants");
    }

    @Disabled // Disabled role now as reference to role not a string. we not aable set wrong string role. this test will delete in future
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUnionRoleIllegalForwardRef(Context<?> context) {
        class TestUnionRoleIllegalForwardRefModifier extends PojoMappingModifier {
            public TestUnionRoleIllegalForwardRefModifier(CatalogMapping catalog) {
                super(catalog);
            }

            protected List<? extends AccessRoleMapping> catalogAccessRoles(CatalogMapping schema) {
            	AccessRoleMappingImpl role1;
            	AccessRoleMappingImpl role2;
                List<AccessRoleMapping> result = new ArrayList<>();
                result.addAll(super.catalogAccessRoles(schema));
                result.add(role1 = AccessRoleMappingImpl.builder()
                    .withName("Role1")
                    .withAccessCatalogGrants(List.of(AccessCatalogGrantMappingImpl.builder().withAccess(AccessCatalog.ALL).build()))
                    .build());
                result.add(role2 = AccessRoleMappingImpl.builder()
                    .withName("Role2")
                    .withAccessCatalogGrants(List.of(AccessCatalogGrantMappingImpl.builder().withAccess(AccessCatalog.ALL).build()))
                    .build());
                result.add(AccessRoleMappingImpl.builder().withName("Role1Plus2")
                    .withAccessCatalogGrants(List.of(AccessCatalogGrantMappingImpl.builder().withAccess(AccessCatalog.ALL).build()))
                    .withReferencedAccessRoles(List.of(role1, role2))
                    .build());
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null, null, null, null, null,
            "<Role name=\"Role1\">\n"
            + "  <SchemaGrant access=\"all\"/>\n"
            + "</Role>\n"
            + "<Role name=\"Role1Plus2\">\n"
            + "  <Union>\n"
            + "    <RoleUsage roleName=\"Role1\"/>\n"
            + "    <RoleUsage roleName=\"Role2\"/>\n"
            + "  </Union>\n"
            + "</Role>\n"
            + "<Role name=\"Role2\">\n"
            + "  <SchemaGrant access=\"all\"/>\n"
            + "</Role>");
        withSchema(context, schema);
         */
        withSchema(context, TestUnionRoleIllegalForwardRefModifier::new);
        assertQueryThrows(context, List.of("Role1Plus2"),
            "select from [Sales]", "Unknown role 'Role2'");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testVirtualCubeNamedSetSupportInSchema(Context<?> context) {
        class TestVirtualCubeNamedSetSupportInSchemaModifier extends PojoMappingModifier {
            public TestVirtualCubeNamedSetSupportInSchemaModifier(CatalogMapping catalog) {
                super(catalog);
            }

            protected List<? extends NamedSetMapping> cubeNamedSets(CubeMapping cube) {
            	List<NamedSetMapping> result = new ArrayList<>();
            	result.addAll(super.cubeNamedSets(cube));
            	if ("Warehouse and Sales".equals(cube.getName())) {
                    result.add(NamedSetMappingImpl.builder().withName("Non CA State Stores")
                            .withFormula("EXCEPT({[Store].[Store Country].[USA].children},{[Store].[Store Country].[USA].[CA]})")
                            .build());
            	}
                return result;
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Warehouse and Sales",
            null, null, null,
            "<NamedSet name=\"Non CA State Stores\" "
            + "formula=\"EXCEPT({[Store].[Store Country].[USA].children},{[Store].[Store Country].[USA].[CA]})\"/>"));
         */
        withSchema(context, TestVirtualCubeNamedSetSupportInSchemaModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH "
            + "SET [Non CA State Stores] AS 'EXCEPT({[Store].[Store Country].[USA].children},"
            + "{[Store].[Store Country].[USA].[CA]})'\n"
            + "MEMBER "
            + "[Store].[Total Non CA State] AS \n"
            + "'SUM({[Non CA State Stores]})'\n"
            + "SELECT {[Store].[Store Country].[USA],[Store].[Total Non CA State]} ON 0,"
            + "{[Measures].[Unit Sales]} ON 1 FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[Store].[USA]}\n"
            + "{[Store].[Store].[Total Non CA State]}\n"
            + "Axis #2:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: 192,025\n");
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH "
            + "MEMBER "
            + "[Store].[Total Non CA State] AS \n"
            + "'SUM({[Non CA State Stores]})'\n"
            + "SELECT {[Store].[Store Country].[USA],[Store].[Total Non CA State]} ON 0,"
            + "{[Measures].[Unit Sales]} ON 1 FROM [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[Store].[USA]}\n"
            + "{[Store].[Store].[Total Non CA State]}\n"
            + "Axis #2:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: 192,025\n");
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testVirtualCubeNamedSetSupportInSchemaError(Context<?> context) {
        class TestVirtualCubeNamedSetSupportInSchemaErrorModifier extends PojoMappingModifier {
            public TestVirtualCubeNamedSetSupportInSchemaErrorModifier(CatalogMapping catalog) {
                super(catalog);
            }
            protected List<? extends NamedSetMapping> cubeNamedSets(CubeMapping cube) {
            	List<NamedSetMapping> result = new ArrayList<>();
            	result.addAll(super.cubeNamedSets(cube));
            	if ("Warehouse and Sales".equals(cube.getName())) {
                    result.add(NamedSetMappingImpl.builder().withName("Non CA State Stores")
                            .withFormula("EXCEPT({[Store].[Store State].[USA].children},{[Store].[Store Country].[USA].[CA]})")
                            .build());
            	}
                return namedSets(cube.getNamedSets());
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Warehouse and Sales",
            null, null, null,
            "<NamedSet name=\"Non CA State Stores\" "
            + "formula=\"EXCEPT({[Store].[Store State].[USA].children},{[Store].[Store Country].[USA].[CA]})\"/>"));
         */

        try {
            withSchema(context, TestVirtualCubeNamedSetSupportInSchemaErrorModifier::new);
            assertQueryReturns(context.getConnectionWithDefaultRole(),
                "WITH "
                + "SET [Non CA State Stores] AS 'EXCEPT({[Store].[Store Country].[USA].children},"
                + "{[Store].[Store Country].[USA].[CA]})'\n"
                + "MEMBER "
                + "[Store].[Total Non CA State] AS \n"
                + "'SUM({[Non CA State Stores]})'\n"
                + "SELECT {[Store].[Store Country].[USA],[Store].[Total Non CA State]} ON 0,"
                + "{[Measures].[Unit Sales]} ON 1 FROM [Sales]",
                "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Store].[USA]}\n"
                + "{[Store].[Total Non CA State]}\n"
                + "Axis #2:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "Row #0: 266,773\n"
                + "Row #0: 192,025\n");
            fail();
        } catch (OlapRuntimeException e) {
            assertTrue(e.getMessage().indexOf("bad formula") >= 0);
        }
    }

    @Disabled //not implemented yet
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void _testValidatorFindsNumericLevel(Context<?> context) {

        class TestValidatorFindsNumericLevelModifier extends PojoMappingModifier {
            public TestValidatorFindsNumericLevelModifier(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    LevelMappingImpl level = LevelMappingImpl
                        .builder()
                        .withName("Store Sqft")
                        .withColumn(FoodmartMappingSupplier.STORE_SQFT_COLUMN_IN_STORE)
                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                        .withUniqueMembers(true)
                        .build();
                    HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                        .builder()
                        .withHasAll(true)
                        .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                        .withLevels(List.of(level))
                        .build();
                    DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Store Size in SQFT")
                        .withDimension(StandardDimensionMappingImpl.builder()
                           	.withName("Store Size in SQFT")
                           	.withHierarchies(List.of(hierarchy))
                           	.build())
                        .build();
                    result.add(dimension);
                }
                return result;
            }
        }
        // In the real foodmart, the level has type="Numeric"
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                "Sales",
                "  <Dimension name=\"Store Size in SQFT\">\n"
                + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">\n"
                + "      <Table name=\"store\"/>\n"
                + "      <Level name=\"Store Sqft\" column=\"store_sqft\" type=\"Numeric\" uniqueMembers=\"true\"/>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>"));
         */
        withSchema(context, TestValidatorFindsNumericLevelModifier::new);
        final List<Exception> exceptionList = TestUtil.getSchemaWarnings(context);
        assertContains(exceptionList, "todo xxxxx");
    }

    @Disabled // Disabled role now as reference to role not a string. we not aable set wrong string role. this test will delete in future
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testInvalidRoleError(Context<?> context) {
        class TestInvalidRoleErrorModifier extends PojoMappingModifier {
            public TestInvalidRoleErrorModifier(CatalogMapping catalog) {
                super(catalog);
            }
            protected AccessRoleMapping catalogDefaultAccessRole(CatalogMapping schema) {
            	AccessRoleMapping role = super.catalogDefaultAccessRole(schema);
                if ("FoodMart".equals(schema.getName())) {
                    return AccessRoleMappingImpl.builder().withName("Unknown").build();
                }
                return role;
            }
        }
        //String schema = TestContext.getRawFoodMartSchema();
        /*
        String schema = TestUtil.getRawSchema(context);
        schema =
            schema.replaceFirst(
                "<Schema name=\"FoodMart\"",
                "<Schema name=\"FoodMart\" defaultRole=\"Unknown\"");
        withSchema(context, schema);
         */
        withSchema(context, TestInvalidRoleErrorModifier::new);
        try {
        	TestUtil.getSchemaWarnings(context);
        	fail("should be exception with \"Role 'Unknown'\" ");
        }
        catch (Exception e) {
        	assertTrue(e.getMessage().contains("Role 'Unknown' not found"));
        }
    }

    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-413">
     * MONDRIAN-413, "RolapMember causes ClassCastException in compare()"</a>,
     * caused by binary column value.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBinaryLevelKey(Context<?> context) {
        switch (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())) {
        case DERBY:
        case MARIADB:
        case MYSQL:
            break;
        default:
            // Not all databases support binary literals (e.g. X'AB01'). Only
            // Derby returns them as byte[] values from its JDBC driver and
            // therefore experiences bug MONDRIAN-413.
            return;
        }
        class TestBinaryLevelKeyModifier extends PojoMappingModifier {
            public TestBinaryLevelKeyModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    PhysicalColumnMappingImpl id = PhysicalColumnMappingImpl.builder().withName("id").withDataType(ColumnDataType.INTEGER).build();
                    PhysicalColumnMappingImpl bin = PhysicalColumnMappingImpl.builder().withName("bin").withDataType(ColumnDataType.INTEGER).build();
                    PhysicalColumnMappingImpl name = PhysicalColumnMappingImpl.builder().withName("name").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
                    InlineTableMappingImpl t = InlineTableMappingImpl.builder()
                    .withColumns(List.of(id, bin, name))
                    .withRows(List.of(
                           RowMappingImpl.builder().withRowValues(List.of(
                                RowValueMappingImpl.builder().withColumn(id).withValue("2").build(),
                                RowValueMappingImpl.builder().withColumn(bin).withValue("X'4546'").build(),
                                RowValueMappingImpl.builder().withColumn(name).withValue("Ben").build())).build(),
                           RowMappingImpl.builder().withRowValues(List.of(
                        		   RowValueMappingImpl.builder().withColumn(id).withValue("3").build(),
                        		   RowValueMappingImpl.builder().withColumn(bin).withValue("X'424344'").build(),
                        		   RowValueMappingImpl.builder().withColumn(name).withValue("Bill").build())).build(),
                           RowMappingImpl.builder().withRowValues(List.of(
                        		   RowValueMappingImpl.builder().withColumn(id).withValue("4").build(),
                        		   RowValueMappingImpl.builder().withColumn(bin).withValue("X'424344'").build(),
                        		   RowValueMappingImpl.builder().withColumn(name).withValue("Bill").build())).build()
                    ))
                    .build();
                    InlineTableQueryMappingImpl inlineTable = InlineTableQueryMappingImpl
                            .builder()
                            .withAlias("binary")
                            .withTable(t)
                            .build();
                        LevelMappingImpl level1 = LevelMappingImpl
                            .builder()
                            .withName("Level1")
                            .withColumn(bin)
                            .withNameColumn(name)
                            .withOrdinalColumn(name)
                            .build();
                        LevelMappingImpl level2 = LevelMappingImpl
                            .builder()
                            .withName("Level2")
                            .withColumn(id)
                            .build();
                        HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withHasAll(false)
                            .withPrimaryKey(id)
                            .withQuery(inlineTable)
                            .withLevels(List.of(level1, level2))
                            .build();
                        DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Binary")
                                .withForeignKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Binary")
                                	.withHierarchies(List.of(hierarchy)).build())
                            .build();
                        result.add(dimension);
                }
                return result;
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "  <Dimension name=\"Binary\" foreignKey=\"promotion_id\">\n"
            + "    <Hierarchy hasAll=\"false\" primaryKey=\"id\">\n"
            + "      <InlineTable alias=\"binary\">\n"
            + "        <ColumnDefs>\n"
            + "          <ColumnDef name=\"id\" type=\"Integer\"/>\n"
            + "          <ColumnDef name=\"bin\" type=\"Integer\"/>\n"
            + "          <ColumnDef name=\"name\" type=\"String\"/>\n"
            + "        </ColumnDefs>\n"
            + "        <Rows>\n"
            + "          <Row>\n"
            + "            <Value column=\"id\">2</Value>\n"
            + "            <Value column=\"bin\">X'4546'</Value>\n"
            + "            <Value column=\"name\">Ben</Value>\n"
            + "          </Row>\n"
            + "          <Row>\n"
            + "            <Value column=\"id\">3</Value>\n"
            + "            <Value column=\"bin\">X'424344'</Value>\n"
            + "            <Value column=\"name\">Bill</Value>\n"
            + "          </Row>\n"
            + "          <Row>\n"
            + "            <Value column=\"id\">4</Value>\n"
            + "            <Value column=\"bin\">X'424344'</Value>\n"
            + "            <Value column=\"name\">Bill</Value>\n"
            + "          </Row>\n"
            + "        </Rows>\n"
            + "      </InlineTable>\n"
            + "      <Level name=\"Level1\" column=\"bin\" nameColumn=\"name\" ordinalColumn=\"name\" />\n"
            + "      <Level name=\"Level2\" column=\"id\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"));
         */

        withSchema(context, TestBinaryLevelKeyModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Binary].members} on 0 from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Binary].[Binary].[Ben]}\n"
            + "{[Binary].[Binary].[Ben].[2]}\n"
            + "{[Binary].[Binary].[Bill]}\n"
            + "{[Binary].[Binary].[Bill].[3]}\n"
            + "{[Binary].[Binary].[Bill].[4]}\n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n");
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select hierarchize({[Binary].members}) on 0 from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Binary].[Binary].[Ben]}\n"
            + "{[Binary].[Binary].[Ben].[2]}\n"
            + "{[Binary].[Binary].[Bill]}\n"
            + "{[Binary].[Binary].[Bill].[3]}\n"
            + "{[Binary].[Binary].[Bill].[4]}\n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n");
    }

    /**
     * Test case for the Level@internalType attribute.
     *
     * <p>See bug <a href="http://jira.pentaho.com/browse/MONDRIAN-896">
     * MONDRIAN-896, "Oracle integer columns overflow if value &gt;>2^31"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLevelInternalType(Context<?> context) {
        // One of the keys is larger than Integer.MAX_VALUE (2 billion), so
        // will only work if we use long values.
        class TestLevelInternalTypeModifier extends PojoMappingModifier {
            public TestLevelInternalTypeModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    PhysicalColumnMappingImpl id = PhysicalColumnMappingImpl.builder().withName("id").withDataType(ColumnDataType.INTEGER).build();
                    PhysicalColumnMappingImpl bigNum = PhysicalColumnMappingImpl.builder().withName("big_num").withDataType(ColumnDataType.INTEGER).build();
                    PhysicalColumnMappingImpl name = PhysicalColumnMappingImpl.builder().withName("name").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
                    InlineTableMappingImpl t = InlineTableMappingImpl.builder()
                    .withColumns(List.of(id, bigNum, name))
                    .withRows(List.of(
                           RowMappingImpl.builder().withRowValues(List.of(
                        		   RowValueMappingImpl.builder().withColumn(id).withValue("0").build(),
                        		   RowValueMappingImpl.builder().withColumn(bigNum).withValue("1234").build(),
                        		   RowValueMappingImpl.builder().withColumn(name).withValue("Ben").build())).build(),
                           RowMappingImpl.builder().withRowValues(List.of(
                        		   RowValueMappingImpl.builder().withColumn(id).withValue("519").build(),
                        		   RowValueMappingImpl.builder().withColumn(bigNum).withValue("1234567890123").build(),
                        		   RowValueMappingImpl.builder().withColumn(name).withValue("Bill").build())).build()
                    ))
                    .build();

                    InlineTableQueryMappingImpl inlineTable = InlineTableQueryMappingImpl
                            .builder()
                            .withAlias("binary")
                            .withTable(t)
                            .build();
                        LevelMappingImpl level1 = LevelMappingImpl
                            .builder()
                            .withName("Level1")
                            .withColumn(bigNum)
                            .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.INTEGER)
                            .build();
                        LevelMappingImpl level2 = LevelMappingImpl
                            .builder()
                            .withName("Level2")
                            .withColumn(id)
                            .build();
                        HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withHasAll(false)
                            .withPrimaryKey(id)
                            .withQuery(inlineTable)
                            .withLevels(List.of(level1, level2))
                            .build();
                        DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Big numbers")
                                .withForeignKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                    .withName("Big numbers")
                                    .withHierarchies(List.of(hierarchy)).build())
                            .build();
                        result.add(dimension);
                }
                return result;
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "  <Dimension name=\"Big numbers\" foreignKey=\"promotion_id\">\n"
            + "    <Hierarchy hasAll=\"false\" primaryKey=\"id\">\n"
            + "      <InlineTable alias=\"t\">\n"
            + "        <ColumnDefs>\n"
            + "          <ColumnDef name=\"id\" type=\"Integer\"/>\n"
            + "          <ColumnDef name=\"big_num\" type=\"Integer\"/>\n"
            + "          <ColumnDef name=\"name\" type=\"String\"/>\n"
            + "        </ColumnDefs>\n"
            + "        <Rows>\n"
            + "          <Row>\n"
            + "            <Value column=\"id\">0</Value>\n"
            + "            <Value column=\"big_num\">1234</Value>\n"
            + "            <Value column=\"name\">Ben</Value>\n"
            + "          </Row>\n"
            + "          <Row>\n"
            + "            <Value column=\"id\">519</Value>\n"
            + "            <Value column=\"big_num\">1234567890123</Value>\n"
            + "            <Value column=\"name\">Bill</Value>\n"
            + "          </Row>\n"
            + "        </Rows>\n"
            + "      </InlineTable>\n"
            + "      <Level name=\"Level1\" column=\"big_num\" internalType=\"long\"/>\n"
            + "      <Level name=\"Level2\" column=\"id\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"));
         */
        withSchema(context, TestLevelInternalTypeModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Big numbers].members} on 0 from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Big numbers].[Big numbers].[1234]}\n"
            + "{[Big numbers].[Big numbers].[1234].[0]}\n"
            + "{[Big numbers].[Big numbers].[1.234567890123E12]}\n"
            + "{[Big numbers].[Big numbers].[1.234567890123E12].[519]}\n"
            + "Row #0: 195,448\n"
            + "Row #0: 195,448\n"
            + "Row #0: 739\n"
            + "Row #0: 739\n");
    }

    /**
     * Negative test for Level@internalType attribute.
     */
    @Disabled // level internalTypedoesn't support now
    @ParameterizedTest
    @DisabledIfSystemProperty(named = "tempIgnoreStrageTests",matches = "true")
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLevelInternalTypeErr(Context<?> context) {
        class TestLevelInternalTypeErrModifier extends PojoMappingModifier {
            public TestLevelInternalTypeErrModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    PhysicalColumnMappingImpl id = PhysicalColumnMappingImpl.builder().withName("id").withDataType(ColumnDataType.INTEGER).build();
                    PhysicalColumnMappingImpl bigNum = PhysicalColumnMappingImpl.builder().withName("big_num").withDataType(ColumnDataType.INTEGER).build();
                    PhysicalColumnMappingImpl name = PhysicalColumnMappingImpl.builder().withName("name").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
                    InlineTableMappingImpl t = InlineTableMappingImpl.builder()
                    .withColumns(List.of(id, bigNum, name))
                    .withRows(List.of(
                           RowMappingImpl.builder().withRowValues(List.of(
                                RowValueMappingImpl.builder().withColumn(id).withValue("0").build(),
                                RowValueMappingImpl.builder().withColumn(bigNum).withValue("1234").build(),
                                RowValueMappingImpl.builder().withColumn(name).withValue("Ben").build())).build()
                    ))
                    .build();

                    InlineTableQueryMappingImpl inlineTable = InlineTableQueryMappingImpl
                            .builder()
                            .withAlias("binary")
                            .withTable(t)
                            .build();
                        LevelMappingImpl level1 = LevelMappingImpl
                            .builder()
                            .withName("Level1")
                            .withColumn(bigNum)
                            .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.INTEGER)
                            //.internalType(InternalTypeEnum.fromValue("char"))
                            .build();
                        LevelMappingImpl level2 = LevelMappingImpl
                            .builder()
                            .withName("Level2")
                            .withColumn(id)
                            .build();
                        HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withHasAll(false)
                            .withPrimaryKey(id)
                            .withQuery(inlineTable)
                            .withLevels(List.of(level1, level2))
                            .build();
                        DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Big numbers")
                                .withForeignKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Big numbers")
                                	.withHierarchies(List.of(hierarchy)).build())
                            .build();
                        result.add(dimension);
                }
                return result;
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "  <Dimension name=\"Big numbers\" foreignKey=\"promotion_id\">\n"
            + "    <Hierarchy hasAll=\"false\" primaryKey=\"id\">\n"
            + "      <InlineTable alias=\"t\">\n"
            + "        <ColumnDefs>\n"
            + "          <ColumnDef name=\"id\" type=\"Integer\"/>\n"
            + "          <ColumnDef name=\"big_num\" type=\"Integer\"/>\n"
            + "          <ColumnDef name=\"name\" type=\"String\"/>\n"
            + "        </ColumnDefs>\n"
            + "        <Rows>\n"
            + "          <Row>\n"
            + "            <Value column=\"id\">0</Value>\n"
            + "            <Value column=\"big_num\">1234</Value>\n"
            + "            <Value column=\"name\">Ben</Value>\n"
            + "          </Row>\n"
            + "        </Rows>\n"
            + "      </InlineTable>\n"
            + "      <Level name=\"Level1\" column=\"big_num\" type=\"Integer\" internalType=\"char\"/>\n"
            + "      <Level name=\"Level2\" column=\"id\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"));
         */
        withSchema(context, TestLevelInternalTypeErrModifier::new);
        assertQueryThrows(context,
            "select {[Big numbers].members} on 0 from [Sales]",
        		"Illegal value 'char'.  Legal values: {int, long, Object, String}");
            //"In Schema: In Cube: In Dimension: In Hierarchy: In Level: Value 'char' of attribute 'internalType' has illegal value 'char'.  Legal values: {int, long, Object, String}");
    }

    @Disabled // Adventure Works schema not found
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void _testAttributeHierarchy(Context<?> context) {
        // from email from peter tran dated 2008/9/8
        // TODO: schema syntax to create attribute hierarchy
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH \n"
            + " MEMBER\n"
            + "  Measures.SalesPerWorkingDay AS \n"
            + "    IIF(\n"
            + "     Count(\n"
            + "      Filter(\n"
            + "        Descendants(\n"
            + "          [Date].[Calendar].CurrentMember\n"
            + "          ,[Date].[Calendar].[Date]\n"
            + "          ,SELF)\n"
            + "       ,  [Date].[Day of Week].CurrentMember.Name <> \"1\"\n"
            + "      )\n"
            + "    ) = 0\n"
            + "     ,NULL\n"
            + "     ,[Measures].[Internet Sales Amount]\n"
            + "      /\n"
            + "       Count(\n"
            + "         Filter(\n"
            + "           Descendants(\n"
            + "             [Date].[Calendar].CurrentMember\n"
            + "             ,[Date].[Calendar].[Date]\n"
            + "             ,SELF)\n"
            + "          ,  [Date].[Day of Week].CurrentMember.Name <> \"1\"\n"
            + "         )\n"
            + "       )\n"
            + "    )\n"
            + "SELECT [Measures].[SalesPerWorkingDay] ON 0\n"
            + ", [Date].[Calendar].[Month].MEMBERS ON 1\n"
            + "FROM [Adventure Works]",
            "x");
    }

    /**
     * Testcase for a problem which involved a slowly changing dimension.
     * Not actually a slowly-changing dimension - we don't have such a thing in
     * the foodmart schema - but the same structure. The dimension is a two
     * table snowflake, and the table nearer to the fact table is not used by
     * any level.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testScdJoin(Context<?> context) {
        class TestScdJoinModifier extends PojoMappingModifier {
            public TestScdJoinModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    JoinQueryMappingImpl join = JoinQueryMappingImpl.builder()
                    		.withLeft(JoinedQueryElementMappingImpl.builder()
                    			.withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT)
                    			.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_TABLE).build())
                    			.build())
                    		.withRight(JoinedQueryElementMappingImpl.builder()
                                .withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT_CLASS)
                                .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE).build())
                                .build())
                    		.build();

                        LevelMappingImpl level = LevelMappingImpl
                            .builder()
                            .withName("Product Class")
                            .withNameColumn(FoodmartMappingSupplier.PRODUCT_SUBCATEGORY_COLUMN_IN_PRODUCT_CLASS)
                            .withColumn(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT_CLASS)
                            .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                            .withUniqueMembers(true)
                            .build();
                        HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withHasAll(true)
                            .withPrimaryKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                            .withQuery(join)
                            .withLevels(List.of(level))
                            .build();

                        DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl.builder()
                            .withOverrideDimensionName("Product truncated")
                            .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            .withName("Product truncated")
                            .withHierarchies(List.of(hierarchy)).build())
                            .build();
                        result.add(dimension);
                    }
                    return result;
                }
        }
        withSchema(context, TestScdJoinModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select non empty {[Measures].[Unit Sales]} on 0,\n"
            + " non empty Filter({[Product truncated].Members}, [Measures].[Unit Sales] > 10000) on 1\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Product truncated].[Product truncated].[All Product truncateds]}\n"
            + "{[Product truncated].[Product truncated].[Fresh Vegetables]}\n"
            + "{[Product truncated].[Product truncated].[Fresh Fruit]}\n"
            + "Row #0: 266,773\n"
            + "Row #1: 20,739\n"
            + "Row #2: 11,767\n");
    }

    // TODO: enable this test as part of PhysicalSchema work
    // TODO: also add a test that Table.alias, Join.leftAlias and
    // Join.rightAlias cannot be the empty string.
    @Disabled //not implemented yet
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void _testNonUniqueAlias(Context<?> context) {
        class TestNonUniqueAliasModifier extends PojoMappingModifier {
            public TestNonUniqueAliasModifier(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    JoinQueryMappingImpl join = JoinQueryMappingImpl.builder()
                    		.withLeft(JoinedQueryElementMappingImpl.builder()
                    			.withKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                    			.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_TABLE).withAlias("product_class").build())
                    			.build())
                    		.withRight(JoinedQueryElementMappingImpl.builder()
                        			.withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT_CLASS)
                        			.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE).build())
                    				.build())
                    		.build();

                        LevelMappingImpl level = LevelMappingImpl
                            .builder()
                            .withName("Product Class")
                            .withNameColumn(FoodmartMappingSupplier.PRODUCT_SUBCATEGORY_COLUMN_IN_PRODUCT_CLASS)
                            .withColumn(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT_CLASS)
                            .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                            .withUniqueMembers(true)
                            .build();
                        HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withHasAll(true)
                            .withPrimaryKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                            .withQuery(join)
                            .withLevels(List.of(level))
                            .build();

                        DimensionConnectorMappingImpl dimension = DimensionConnectorMappingImpl.builder()
                            .withOverrideDimensionName("Product truncated")
                            .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            .withName("Product truncated")
                            .withHierarchies(List.of(hierarchy)).build())
                            .build();
                        result.add(dimension);
                    }
                    return result;
                }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                "Sales",
                "  <Dimension name=\"Product truncated\" foreignKey=\"product_id\">\n"
                + "    <Hierarchy hasAll=\"true\" primaryKey=\"product_id\" primaryKeyTable=\"product\">\n"
                + "      <Join leftKey=\"product_class_id\" rightKey=\"product_class_id\">\n"
                + "        <Table name=\"product\" alias=\"product_class\"/>\n"
                + "        <Table name=\"product_class\"/>\n"
                + "      </Join>\n"
                + "      <Level name=\"Product Class\" table=\"product_class\" nameColumn=\"product_subcategory\"\n"
                + "          column=\"product_class_id\" type=\"Numeric\" uniqueMembers=\"true\"/>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n",
                null, null, null));
         */
        withSchema(context, TestNonUniqueAliasModifier::new);
        Throwable throwable = null;
        try {
            assertSimpleQuery(context.getConnectionWithDefaultRole());
        } catch (Throwable e) {
            throwable = e;
        }
        // neither a source column or source expression specified
        checkThrowable(
            throwable,
            "Alias not unique");
    }

    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-482">
     * MONDRIAN-482, "ClassCastException when obtaining RolapCubeLevel"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBugMondrian482(Context<?> context) {
        // until bug MONDRIAN-495, "Table filter concept does not support
        // dialects." is fixed, this test case only works on MySQL
        if (!Bug.BugMondrian495Fixed
            && getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())
            != MYSQL)
        {
            return;
        }

        // skip this test if using aggregates, the agg tables do not
        // enforce the SQL element in the fact table
        if (context.getConfigValue(ConfigConstants.USE_AGGREGATES, ConfigConstants.USE_AGGREGATES_DEFAULT_VALUE ,Boolean.class)) {
            return;
        }
        class TestBugMondrian482Modifier extends PojoMappingModifier {
            public TestBugMondrian482Modifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> cs = new ArrayList<>();
                cs.addAll(super.cubes(cubes));
                TableQueryMappingImpl table = TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
                	.withSqlWhereExpression(SqlStatementMappingImpl.builder()
                			.withSql("`sales_fact_1997`.`store_id` in (select distinct `store_id` from `store` where `store`.`store_state` = \"CA\")")
                			.withDialects(List.of("generic"))
                			.build()).build();
                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                    .withOverrideDimensionName("Store")
                    .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                    .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_STORE_WITH_QUERY_STORE))
                    .build();

                DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Product")
                        .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_PRODUCT))
                        .build();

                SumMeasureMappingImpl measure1 = SumMeasureMappingImpl
                    .builder()
                    .withName("Unit Sales")
                    .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                    .withFormatString("Standard")
                    .build();
                SumMeasureMappingImpl measure2 = SumMeasureMappingImpl
                    .builder()
                    .withName("Store Sales")
                    .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_SALES_FACT_1997)

                    .withFormatString("Standard")
                    .build();
                PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl
                    .builder()
                    .withName("Sales2")
                    .withDefaultMeasure(measure1)
                    .withQuery(table)
                    .withDimensionConnectors(List.of(d1, d2))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(measure1, measure2)).build()))
                    .build();
                cs.add(c);
                return cs;
            }
        }
        // In order to reproduce the problem it was necessary to only have one
        // non empty member under USA. In the cube definition below we create a
        // cube with only CA data to achieve this.
        /*
        String salesCube1 =
            "<Cube name=\"Sales2\" defaultMeasure=\"Unit Sales\">\n"
            + "  <Table name=\"sales_fact_1997\" >\n"
            + "    <SQL dialect=\"default\">\n"
            + "     <![CDATA[`sales_fact_1997`.`store_id` in (select distinct `store_id` from `store` where `store`.`store_state` = \"CA\")]]>\n"
            + "    </SQL>\n"
            + "  </Table>\n"
            + "  <DimensionUsage name=\"Store\" source=\"Store\" foreignKey=\"store_id\"/>\n"
            + "  <DimensionUsage name=\"Product\" source=\"Product\" foreignKey=\"product_id\"/>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" formatString=\"Standard\"/>\n"
            + "  <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\" formatString=\"Standard\"/>\n"
            + "</Cube>\n";

        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            salesCube1,
            null,
            null,
            null,
            null);
        withSchema(context, schema);
         */

        withSchema(context, TestBugMondrian482Modifier::new);
        // First query all children of the USA. This should only return CA since
        // all the other states were filtered out. CA will be put in the member
        // cache
        String query1 =
            "WITH SET [#DataSet#] as "
            + "'NonEmptyCrossjoin({[Product].[All Products]}, {[Store].[All Stores].[USA].Children})' "
            + "SELECT {[Measures].[Unit Sales]} on columns, "
            + "NON EMPTY Hierarchize({[#DataSet#]}) on rows FROM [Sales2]";

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            query1,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Product].[Product].[All Products], [Store].[Store].[USA].[CA]}\n"
            + "Row #0: 74,748\n");

        // Now query the children of CA using the descendants function
        // This is where the ClassCastException occurs
        String query2 =
            "WITH SET [#DataSet#] as "
            + "'{Descendants([Store].[Store].[All Stores], 3)}' "
            + "SELECT {[Measures].[Unit Sales]} on columns, "
            + "NON EMPTY Hierarchize({[#DataSet#]}) on rows FROM [Sales2]";

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            query2,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[CA].[Beverly Hills]}\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[Store].[USA].[CA].[San Francisco]}\n"
            + "Row #0: 21,333\n"
            + "Row #1: 25,663\n"
            + "Row #2: 25,635\n"
            + "Row #3: 2,117\n");
    }

    /**
     * Test case for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-355">Bug MONDRIAN-355,
     * "adding hours/mins as levelType for level of type Dimension"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBugMondrian355(Context<?> context) {
    	context.getCatalogCache().clear();
        checkBugMondrian355(context, "TimeHalfYears");

        // make sure that the deprecated name still works
        checkBugMondrian355(context, "TimeHalfYears");
    }

    public void checkBugMondrian355(Context<?> context, String timeHalfYear) {
    	context.getCatalogCache().clear();
        class CheckBugMondrian355Modifier1 extends PojoMappingModifier {
            public CheckBugMondrian355Modifier1(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube).stream().filter(dc -> !dc.getOverrideDimensionName().equals("Time2")).toList());
                if ("Sales".equals(cube.getName())) {
                        LevelMappingImpl l1 = LevelMappingImpl.builder()
                            .withName("Years").withColumn(FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY).withUniqueMembers(true)
                            .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                            .withLevelType(LevelType.TIME_YEARS).build();
                        LevelMappingImpl l2 = LevelMappingImpl.builder()
                            .withName("Half year").withColumn(FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY).withUniqueMembers(false)
                            .withLevelType(LevelType.fromValue(timeHalfYear)).build();
                        LevelMappingImpl l3 = LevelMappingImpl.builder()
                            .withName("Hours").withColumn(FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY).withUniqueMembers(false)
                            .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                            .withLevelType(LevelType.TIME_HOURS).build();
                        LevelMappingImpl l4 = LevelMappingImpl.builder()
                            .withName("Quarter hours").withColumn(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY).withUniqueMembers(false)
                            .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                            .withLevelType(LevelType.TIME_UNDEFINED).build();
                        HierarchyMappingImpl h = ExplicitHierarchyMappingImpl.builder()
                            .withHasAll(true).withPrimaryKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY)
                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.TIME_BY_DAY_TABLE).build())
                            .withLevels(List.of(l1, l2, l3, l4))
                            .build();
                        DimensionConnectorMappingImpl d = DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Time2")
                                .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(TimeDimensionMappingImpl.builder()
                                		.withName("Time2")
                                		.withHierarchies(List.of(h))
                                		.build())
                                .build();
                        result.add(d);
               }
               return result;
            }
        }

        class CheckBugMondrian355Modifier2 extends PojoMappingModifier {
            public CheckBugMondrian355Modifier2(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube).stream().filter(dc -> !dc.getOverrideDimensionName().equals("Time2")).toList());
                if ("Sales".equals(cube.getName())) {
                		String type = timeHalfYear.equals("TimeUndefined") ? "TimeUnspecified" : timeHalfYear;
                        LevelMappingImpl l1 = LevelMappingImpl.builder()
                            .withName("Years").withColumn(FoodmartMappingSupplier.THE_DATE_COLUMN_IN_TIME_BY_DAY).withUniqueMembers(true)
                            .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                            .withLevelType(LevelType.TIME_YEARS).build();
                        LevelMappingImpl l2 = LevelMappingImpl.builder()
                            .withName("Half year").withColumn(FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY).withUniqueMembers(false)
                            .withLevelType(LevelType.fromValue(timeHalfYear)).build();
                        LevelMappingImpl l3 = LevelMappingImpl.builder()
                            .withName("Hours").withColumn(FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY).withUniqueMembers(false)
                            .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                            .withLevelType(LevelType.TIME_HOURS).build();
                        LevelMappingImpl l4 = LevelMappingImpl.builder()
                            .withName("Quarter hours").withColumn(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY).withUniqueMembers(false)
                            .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                            .withLevelType(LevelType.fromValue("TimeUnspecified")).build();
                        HierarchyMappingImpl h = ExplicitHierarchyMappingImpl.builder()
                            .withHasAll(true).withPrimaryKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY)
                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.TIME_BY_DAY_TABLE).build())
                            .withLevels(List.of(l1, l2, l3, l4))
                            .build();
                        DimensionConnectorMappingImpl d = DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Time2")
                                .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(TimeDimensionMappingImpl.builder()
                                		.withName("Time2")
                                		.withHierarchies(List.of(h))
                                		.build())
                                .build();
                        result.add(d);
               }
               return result;
            }
        }
        /*
        final String xml =
            "<Dimension name=\"Time2\" foreignKey=\"time_id\" type=\"TimeDimension\">\n"
            + "<Hierarchy hasAll=\"true\" primaryKey=\"time_id\">\n"
            + "  <Table name=\"time_by_day\"/>\n"
            + "  <Level name=\"Years\" column=\"the_year\" uniqueMembers=\"true\" type=\"Numeric\" levelType=\"TimeYears\"/>\n"
            + "  <Level name=\"Half year\" column=\"quarter\" uniqueMembers=\"false\" levelType=\""
            + timeHalfYear
            + "\"/>\n"
            + "  <Level name=\"Hours\" column=\"month_of_year\" uniqueMembers=\"false\" type=\"Numeric\" levelType=\"TimeHours\"/>\n"
            + "  <Level name=\"Quarter hours\" column=\"time_id\" uniqueMembers=\"false\" type=\"Numeric\" levelType=\"TimeUnspecified\"/>\n"
            + "</Hierarchy>\n"
            + "</Dimension>";
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales", xml, false));
         */
        withSchema(context, CheckBugMondrian355Modifier1::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select Head([Time2].[Quarter hours].Members, 3) on columns\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Time2].[Time2].[1997].[Q1].[1].[367]}\n"
            + "{[Time2].[Time2].[1997].[Q1].[1].[368]}\n"
            + "{[Time2].[Time2].[1997].[Q1].[1].[369]}\n"
            + "Row #0: 348\n"
            + "Row #0: 635\n"
            + "Row #0: 589\n");

        // Check that can apply ParallelPeriod to a TimeUndefined level.
        Connection connection = context.getConnectionWithDefaultRole();
        assertAxisReturns(connection, "Sales",
            "PeriodsToDate([Time2].[Quarter hours], [Time2].[1997].[Q1].[1].[368])",
            "[Time2].[Time2].[1997].[Q1].[1].[368]");

        assertAxisReturns(connection, "Sales",
            "PeriodsToDate([Time2].[Half year], [Time2].[1997].[Q1].[1].[368])",
            "[Time2].[Time2].[1997].[Q1].[1].[367]\n"
            + "[Time2].[Time2].[1997].[Q1].[1].[368]");
        // Check that get an error if give invalid level type
        try {
            /*
            ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                    "Sales",
                xml.replace("TimeUndefined", "TimeUnspecified"), false));
            */
        	withSchema(context, CheckBugMondrian355Modifier2::new);
            assertSimpleQuery(context.getConnectionWithDefaultRole());
            fail("expected error");
        } catch (Throwable e) {
            //((TestContext)context).setDatabaseMappingSchemaProviders(
        	//		List.of(provider));
            checkThrowable(
                e,
           		"level-type must be  'Regular', 'TimeYears', 'TimeHalfYears', 'TimeHalfYear', 'TimeQuarters', 'TimeMonths', 'TimeWeeks', 'TimeDays', 'TimeHours', 'TimeMinutes', 'TimeSeconds', 'TimeUndefined'.");
                //"Mondrian Error:Level '[Time2].[Quarter hours]' belongs to a time hierarchy, so its level-type must be  'Regular', 'TimeYears', 'TimeHalfYears', 'TimeHalfYear', 'TimeQuarters', 'TimeMonths', 'TimeWeeks', 'TimeDays', 'TimeHours', 'TimeMinutes', 'TimeSeconds', 'TimeUndefined'.");
        }
    }

    /**
     * Test for descriptions, captions and annotations of various schema
     * elements.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCaptionDescriptionAndAnnotation(Context<?> context) {
        final String schemaName = "Description schema";
        final String salesCubeName = "DescSales";
        final String virtualCubeName = "DescWarehouseAndSales";
        final String warehouseCubeName = "Warehouse";
        class TestCaptionDescriptionAndAnnotationModifier extends PojoMappingModifier {
            public TestCaptionDescriptionAndAnnotationModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected CatalogMapping modifyCatalog(CatalogMapping catalog2) {
                PhysicalColumnMappingImpl region_id = PhysicalColumnMappingImpl.builder().withName("region_id").withDataType(ColumnDataType.INTEGER).build();
                PhysicalColumnMappingImpl sales_region = PhysicalColumnMappingImpl.builder().withName("sales_region").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(30).build();
                PhysicalColumnMappingImpl sales_district_id = PhysicalColumnMappingImpl.builder().withName("sales_district_id").withDataType(ColumnDataType.INTEGER).build();
                PhysicalTableMappingImpl region = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("region")
                        .withColumns(List.of(
                                region_id, sales_region, sales_district_id
                                ))).build();

                TimeDimensionMappingImpl sd1 = TimeDimensionMappingImpl.builder()
                        .withName("Time")
                        //.withCaption("Time shared caption")
                        .withDescription("Time shared description")
                        .withAnnotations(List.of(
                            AnnotationMappingImpl.builder().withName("a").withValue("Time shared").build()
                        ))
                        .withHierarchies(List.of(
                            ExplicitHierarchyMappingImpl.builder()
                                .withHasAll(false)
                                .withPrimaryKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY)
                                //.withCaption("Time shared hierarchy caption")
                                .withDescription("Time shared hierarchy description")
                                .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.TIME_BY_DAY_TABLE).build())
                                .withLevels(List.of(
                                    LevelMappingImpl.builder()
                                        .withName("Year")
                                        .withColumn(FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY)
                                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                                        .withUniqueMembers(true)
                                        .withLevelType(LevelType.TIME_YEARS)
                                        .build(),
                                    LevelMappingImpl.builder()
                                        .withName("Quarter")
                                        .withColumn(FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY)
                                        .withUniqueMembers(true)
                                        .withLevelType(LevelType.TIME_QUARTERS)
                                        .build(),
                                    LevelMappingImpl.builder()
                                        .withName("Month")
                                        .withColumn(FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY)
                                        .withUniqueMembers(false)
                                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                                        .withLevelType(LevelType.TIME_MONTHS)
                                        .build()
                                    ))
                                .build()
                        ))
                        .build();

                    StandardDimensionMappingImpl sd2 = StandardDimensionMappingImpl.builder()
                        .withName("Warehouse")
                        .withHierarchies(List.of(
                            ExplicitHierarchyMappingImpl.builder()
                                .withHasAll(false)
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
                        ))
                        .build();
                    JoinQueryMappingImpl j111 = JoinQueryMappingImpl.builder()
                    		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(sales_district_id)
                    				.withQuery(TableQueryMappingImpl.builder().withTable(region).build()).build())
                    		.withRight(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_PROMOTION)
                    				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PROMOTION_TABLE).build()).build())
                    		.build();

                    JoinQueryMappingImpl j11 = JoinQueryMappingImpl.builder()
                    		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.REGION_ID_COLUMN_IN_STORE)
                    				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build()).build())
                    		.withRight(JoinedQueryElementMappingImpl.builder().withKey(region_id)
                    				.withQuery(j111).build())
                    		.build();


                    HierarchyMappingImpl h11 = ExplicitHierarchyMappingImpl.builder()
                        .withHasAll(true)
                        .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                        //.withCaption("Hierarchy caption")
                        .withDescription("Hierarchy description")
                        .withAnnotations(List.of(
                            AnnotationMappingImpl.builder()
                                .withName("a")
                                .withValue("Hierarchy")
                                .build()
                        ))
                        .withQuery(j11)
                        .withLevels(List.of(
                            LevelMappingImpl.builder()
                                .withName("Store Country")
                                .withColumn(FoodmartMappingSupplier.STORE_COUNTRY_COLUMN_IN_STORE)
                                .withDescription("Level description")
                                //.withCaption("Level caption")
                                .withAnnotations(List.of(
                                    AnnotationMappingImpl.builder()
                                        .withName("a")
                                        .withValue("Level")
                                        .build()
                                ))
                                .build(),
                            LevelMappingImpl.builder()
                                .withName("Store Region")
                                .withColumn(sales_region)
                                .build(),
                            LevelMappingImpl.builder()
                                .withName("Store Name")
                                .withColumn(FoodmartMappingSupplier.STORE_NAME_COLUMN_IN_STORE)
                                .build()
                        ))
                        .build();

                    DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                    	.withOverrideDimensionName("Store")
                        .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                        //.withCaption("Dimension caption")
                        .withDimension(StandardDimensionMappingImpl.builder()
                        		.withName("Store")
                                .withAnnotations(List.of(
                                        AnnotationMappingImpl.builder().withName("a").withValue("Dimension").build()
                                 ))
                                .withHierarchies(List.of(h11))
                                .withDescription("Dimension description")
                                //.withCaption("Dimension caption")
                        		.build())
                        .build();

                    DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Time1")
                        .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(sd1)
                        //.withCaption("Time usage caption")
                        //.withDescription("Time usage description")
                        //.withAnnotations(List.of(
                        //    AnnotationMappingImpl.builder().withName("a").withValue("Time usage").build()
                        //))
                        .build();

                    DimensionConnectorMappingImpl d3 = DimensionConnectorMappingImpl.builder()
                    	.withOverrideDimensionName("Time2")
                    	.withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(sd1)
                        .build();

                    SumMeasureMappingImpl unitSalesMeasure;
                    PhysicalCubeMappingImpl c1 = PhysicalCubeMappingImpl.builder()
                        .withName(salesCubeName)
                        .withDescription("Cube description")
                        .withAnnotations(List.of(
                            AnnotationMappingImpl.builder()
                                .withName("a")
                                .withValue("Cube")
                                .build()
                            )
                        )
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                        .withDimensionConnectors(List.of(
                            d1, d2, d3
                        ))
                        .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder()
                        	.withMeasures(List.of(
                        		unitSalesMeasure = SumMeasureMappingImpl.builder()
                                    .withName("Unit Sales")
                                    .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                                    .withFormatString("Standard")
                                    //.withCaption("Measure caption")
                                    .withDescription("Measure description")
                                    .withAnnotations(List.of(
                                        AnnotationMappingImpl.builder()
                                            .withName("a")
                                            .withValue("Measure")
                                            .build()
                                    ))
                                    .build()
                            ))
                        	.build()
                        ))
                        .withCalculatedMembers(List.of(
                            CalculatedMemberMappingImpl.builder()
                                .withName("Foo")
                                //.withDimension("Measures")
                                //.withCaption("Calc member caption")
                                .withDescription("Calc member description")
                                .withAnnotations(List.of(
                                    AnnotationMappingImpl.builder()
                                        .withName("a")
                                        .withValue("Calc member")
                                        .build()
                                ))
                                .withFormula("[Measures].[Unit Sales] + 1")
                                .withCalculatedMemberProperties(List.of(
                                    CalculatedMemberPropertyMappingImpl.builder()
                                        .withName("FORMAT_STRING")
                                        .withValue("$#,##0.00")
                                        .build()
                                ))
                                .build()
                        ))
                        .withNamedSets(List.of(
                            NamedSetMappingImpl.builder()
                                .withName("Top Periods")
                                //.withCcaption("Named set caption")
                                .withDescription("Named set description")
                                .withAnnotations(List.of(
                                    AnnotationMappingImpl.builder()
                                        .withName("a")
                                        .withValue("Named set")
                                        .build()
                                ))
                                .withFormula("TopCount([Time1].MEMBERS, 5, [Measures].[Foo])")
                                .build()
                        ))
                        .build();

                    SumMeasureMappingImpl unitsShippedMeasure;

                    PhysicalCubeMappingImpl c2 = PhysicalCubeMappingImpl.builder()
                        .withName(warehouseCubeName)
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.INVENTORY_FACKT_1997_TABLE).build())
                        .withDimensionConnectors(List.of(
                            DimensionConnectorMappingImpl.builder()
                            	.withOverrideDimensionName("Time")
                            	.withDimension(sd1)
                                .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                            	.withOverrideDimensionName("Warehouse")
                            	.withDimension(sd2)
                                .withForeignKey(FoodmartMappingSupplier.WAREHOUSE_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                                .build()
                        ))
                        .withMeasureGroups(List.of(
                            	MeasureGroupMappingImpl.builder()
                            	.withMeasures(List.of(
                                unitsShippedMeasure = SumMeasureMappingImpl.builder()
                                .withName("Units Shipped")
                                .withColumn(FoodmartMappingSupplier.UNITS_SHIPPED_COLUMN_IN_INVENTORY_FACKT_1997)

                                .withFormatString("#.0")
                                .build())).build()
                        ))
                        .build();

                    VirtualCubeMappingImpl vc1 = VirtualCubeMappingImpl.builder()
                        .withName(virtualCubeName)
                        //.withCaption("Virtual cube caption")
                        .withDescription("Virtual cube description")
                        .withAnnotations(List.of(
                            AnnotationMappingImpl.builder()
                                .withName("a")
                                .withValue("Virtual cube")
                                .build()
                        ))
                        .withDimensionConnectors(List.of(
                        	DimensionConnectorMappingImpl.builder()
                        		.withOverrideDimensionName("Time")
                        		.withDimension(sd1)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                            	.withOverrideDimensionName("Warehouse")
                            	.withDimension(sd2)
                            	.withPhysicalCube(c2)
                                .build()
                        ))
                        .withReferencedMeasures(List.of(
                        	unitSalesMeasure,
                            //.withAnnotations(List.of(
                            //        AnnotationMappingImpl.builder()
                            //            .withName("a")
                            //            .withValue("Virtual cube measure")
                            //            .build()
                            //    ))
                            //    .build(),
                        	unitsShippedMeasure
                        ))
                        .withCalculatedMembers(List.of(
                            CalculatedMemberMappingImpl.builder()
                                .withName("Profit Per Unit Shipped")
                                //.withDimension("Measures")
                                .withFormula("1 / [Measures].[Units Shipped]")
                                .build()
                        ))
                        .build();

           	 	return CatalogMappingImpl.builder()
        			 .withName(schemaName)
                     .withDbSchemas((List<DatabaseSchemaMappingImpl>) catalogDatabaseSchemas( catalog2))
        			 .withDescription("Schema to test descriptions and captions")
        			 .withAnnotations(List.of(
                         AnnotationMappingImpl.builder()
                             .withName("a")
                             .withValue("Schema")
                             .build(),
                         AnnotationMappingImpl.builder()
                             .withName("b")
                             .withValue("Xyz")
                             .build()
                     ))
                     .withCubes(List.of(c1, c2, vc1))
                     .build();
            }
        }
        /*
        withSchema(context,
            "<Schema name=\"" + schemaName + "\"\n"
            + " description=\"Schema to test descriptions and captions\">\n"
            + "  <Annotations>\n"
            + "    <Annotation name=\"a\">Schema</Annotation>\n"
            + "    <Annotation name=\"b\">Xyz</Annotation>\n"
            + "  </Annotations>\n"
            + "  <Dimension name=\"Time\" type=\"TimeDimension\"\n"
            + "      caption=\"Time shared caption\"\n"
            + "      description=\"Time shared description\">\n"
            + "    <Annotations><Annotation name=\"a\">Time shared</Annotation></Annotations>\n"
            + "    <Hierarchy hasAll=\"false\" primaryKey=\"time_id\"\n"
            + "        caption=\"Time shared hierarchy caption\"\n"
            + "        description=\"Time shared hierarchy description\">\n"
            + "      <Table name=\"time_by_day\"/>\n"
            + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
            + "          levelType=\"TimeYears\"/>\n"
            + "      <Level name=\"Quarter\" column=\"quarter\" uniqueMembers=\"false\"\n"
            + "          levelType=\"TimeQuarters\"/>\n"
            + "      <Level name=\"Month\" column=\"month_of_year\" uniqueMembers=\"false\" type=\"Numeric\"\n"
            + "          levelType=\"TimeMonths\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Dimension name=\"Warehouse\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"warehouse_id\">\n"
            + "      <Table name=\"warehouse\"/>\n"
            + "      <Level name=\"Country\" column=\"warehouse_country\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"State Province\" column=\"warehouse_state_province\"\n"
            + "          uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"City\" column=\"warehouse_city\" uniqueMembers=\"false\"/>\n"
            + "      <Level name=\"Warehouse Name\" column=\"warehouse_name\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Cube name=\"" + salesCubeName + "\"\n"
            + "    description=\"Cube description\">\n"
            + "  <Annotations><Annotation name=\"a\">Cube</Annotation></Annotations>\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <Dimension name=\"Store\" foreignKey=\"store_id\"\n"
            + "      caption=\"Dimension caption\"\n"
            + "      description=\"Dimension description\">\n"
            + "    <Annotations><Annotation name=\"a\">Dimension</Annotation></Annotations>\n"
            + "    <Hierarchy hasAll=\"true\" primaryKeyTable=\"store\" primaryKey=\"store_id\"\n"
            + "        caption=\"Hierarchy caption\"\n"
            + "        description=\"Hierarchy description\">\n"
            + "      <Annotations><Annotation name=\"a\">Hierarchy</Annotation></Annotations>\n"
            + "      <Join leftKey=\"region_id\" rightKey=\"region_id\">\n"
            + "        <Table name=\"store\"/>\n"
            + "        <Join leftKey=\"sales_district_id\" rightKey=\"promotion_id\">\n"
            + "          <Table name=\"region\"/>\n"
            + "          <Table name=\"promotion\"/>\n"
            + "        </Join>\n"
            + "      </Join>\n"
            + "      <Level name=\"Store Country\" table=\"store\" column=\"store_country\"\n"
            + "          description=\"Level description\""
            + "          caption=\"Level caption\">\n"
            + "        <Annotations><Annotation name=\"a\">Level</Annotation></Annotations>\n"
            + "      </Level>\n"
            + "      <Level name=\"Store Region\" table=\"region\" column=\"sales_region\" />\n"
            + "      <Level name=\"Store Name\" table=\"store\" column=\"store_name\" />\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <DimensionUsage name=\"Time1\"\n"
            + "    caption=\"Time usage caption\"\n"
            + "    description=\"Time usage description\"\n"
            + "    source=\"Time\" foreignKey=\"time_id\">\n"
            + "    <Annotations><Annotation name=\"a\">Time usage</Annotation></Annotations>\n"
            + "  </DimensionUsage>\n"
            + "  <DimensionUsage name=\"Time2\"\n"
            + "    source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "<Measure name=\"Unit Sales\" column=\"unit_sales\"\n"
            + "    aggregator=\"sum\" formatString=\"Standard\"\n"
            + "    caption=\"Measure caption\"\n"
            + "    description=\"Measure description\">\n"
            + "  <Annotations><Annotation name=\"a\">Measure</Annotation></Annotations>\n"
            + "</Measure>\n"
            + "<CalculatedMember name=\"Foo\" dimension=\"Measures\" \n"
            + "    caption=\"Calc member caption\"\n"
            + "    description=\"Calc member description\">\n"
            + "    <Annotations><Annotation name=\"a\">Calc member</Annotation></Annotations>\n"
            + "    <Formula>[Measures].[Unit Sales] + 1</Formula>\n"
            + "    <CalculatedMemberProperty name=\"FORMAT_STRING\" value=\"$#,##0.00\"/>\n"
            + "  </CalculatedMember>\n"
            + "  <NamedSet name=\"Top Periods\"\n"
            + "      caption=\"Named set caption\"\n"
            + "      description=\"Named set description\">\n"
            + "    <Annotations><Annotation name=\"a\">Named set</Annotation></Annotations>\n"
            + "    <Formula>TopCount([Time1].MEMBERS, 5, [Measures].[Foo])</Formula>\n"
            + "  </NamedSet>\n"
            + "</Cube>\n"
            + "<Cube name=\"" + warehouseCubeName + "\">\n"
            + "  <Table name=\"inventory_fact_1997\"/>\n"
            + "\n"
            + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "  <DimensionUsage name=\"Warehouse\" source=\"Warehouse\" foreignKey=\"warehouse_id\"/>\n"
            + "\n"
            + "  <Measure name=\"Units Shipped\" column=\"units_shipped\" aggregator=\"sum\" formatString=\"#.0\"/>\n"
            + "</Cube>\n"
            + "<VirtualCube name=\"" + virtualCubeName + "\"\n"
            + "    caption=\"Virtual cube caption\"\n"
            + "    description=\"Virtual cube description\">\n"
            + "  <Annotations><Annotation name=\"a\">Virtual cube</Annotation></Annotations>\n"
            + "  <VirtualCubeDimension name=\"Time\"/>\n"
            + "  <VirtualCubeDimension cubeName=\"" + warehouseCubeName
            + "\" name=\"Warehouse\"/>\n"
            + "  <VirtualCubeMeasure cubeName=\"" + salesCubeName
            + "\" name=\"[Measures].[Unit Sales]\">\n"
            + "    <Annotations><Annotation name=\"a\">Virtual cube measure</Annotation></Annotations>\n"
            + "  </VirtualCubeMeasure>\n"
            + "  <VirtualCubeMeasure cubeName=\"" + warehouseCubeName
            + "\" name=\"[Measures].[Units Shipped]\"/>\n"
            + "  <CalculatedMember name=\"Profit Per Unit Shipped\" dimension=\"Measures\">\n"
            + "    <Formula>1 / [Measures].[Units Shipped]</Formula>\n"
            + "  </CalculatedMember>\n"
            + "</VirtualCube>"
            + "</Schema>");
         */
        withSchema(context, TestCaptionDescriptionAndAnnotationModifier::new);
        final Result result =
            executeQuery(context.getConnectionWithDefaultRole(), "select from [" + salesCubeName + "]");
        final Cube cube = result.getQuery().getCube();
        assertEquals("Cube description", cube.getDescription());
        checkAnnotations(cube.getMetaData(), "a", "Cube");

        final Catalog schema = cube.getCatalog();
        checkAnnotations(schema.getMetaData(), "a", "Schema", "b", "Xyz");

        final Dimension dimension = cube.getDimensions().get(1);
        assertEquals("Dimension description", dimension.getDescription());
        assertEquals("Store", dimension.getCaption());
        checkAnnotations(dimension.getMetaData(), "a", "Dimension");

        final Hierarchy hierarchy = dimension.getHierarchies().getFirst();
        assertEquals("Hierarchy description", hierarchy.getDescription());
        assertEquals("Store", hierarchy.getCaption());
        checkAnnotations(hierarchy.getMetaData(), "a", "Hierarchy");

        final Level level = hierarchy.getLevels().get(1);
        assertEquals("Level description", level.getDescription());
        assertEquals("Store Country", level.getCaption());
        checkAnnotations(level.getMetaData(), "a", "Level");

        // Caption comes from the CAPTION member property, defaults to name.
        // Description comes from the DESCRIPTION member property.
        // Annotations are always empty for regular members.
        final List<Member> memberList =
            cube.getCatalogReader(null).withLocus()
                .getLevelMembers(level, false);
        final Member member = memberList.getFirst();
        assertEquals("Canada", member.getName());
        assertEquals("Canada", member.getCaption());
        assertNull(member.getDescription());
        checkAnnotations(member.getMetaData());

        // All member. Caption defaults to name; description is null.
        final Member allMember = member.getParentMember();
        assertEquals("All Stores", allMember.getName());
        assertEquals("All Stores", allMember.getCaption());
        assertNull(allMember.getDescription());

        // All level.
        final Level allLevel = hierarchy.getLevels().getFirst();
        assertEquals("(All)", allLevel.getName());
        assertNull(allLevel.getDescription());
        assertEquals(allLevel.getName(), allLevel.getCaption());
        checkAnnotations(allLevel.getMetaData());

        // the first time dimension overrides the caption and description of the
        // shared time dimension
        final Dimension timeDimension = cube.getDimensions().get(2);
        assertEquals("Time1", timeDimension.getName());
        assertEquals("Time shared description", timeDimension.getDescription());
        assertEquals("Time1", timeDimension.getCaption());
        checkAnnotations(timeDimension.getMetaData(), "a", "Time shared");

        // Time1 is a usage of a shared dimension Time.
        // Now look at the hierarchy usage within that dimension usage.
        // Because the dimension usage has a name, use that as a prefix for
        // name, caption and description of the hierarchy usage.
        final Hierarchy timeHierarchy = timeDimension.getHierarchies().getFirst();
        // The hierarchy in the shared dimension does not have a name, so the
        // hierarchy usage inherits the name of the dimension usage, Time1.
        //final boolean ssasCompatibleNaming =
        //    SystemWideProperties.instance().SsasCompatibleNaming;
        assertEquals("Time1", timeHierarchy.getName());
        assertEquals("Time1", timeHierarchy.getDimension().getName());
        // The description is prefixed by the dimension usage name.
        assertEquals(
            "Time shared hierarchy description",
            timeHierarchy.getDescription());
        // The hierarchy caption is prefixed by the caption of the dimension
        // usage.
        assertEquals(
            "Time1",
            timeHierarchy.getCaption());
        // No annotations.
        checkAnnotations(timeHierarchy.getMetaData());

        // the second time dimension does not overrides caption and description
        final Dimension time2Dimension = cube.getDimensions().get(3);
        assertEquals("Time2", time2Dimension.getName());
        assertEquals(
            "Time shared description", time2Dimension.getDescription());
        assertEquals("Time2", time2Dimension.getCaption());
        checkAnnotations(time2Dimension.getMetaData(), "a", "Time shared");

        final Hierarchy time2Hierarchy = time2Dimension.getHierarchies().get(0);
        // The hierarchy in the shared dimension does not have a name, so the
        // hierarchy usage inherits the name of the dimension usage, Time2.

        assertEquals("Time2", time2Hierarchy.getName());
        assertEquals("Time2", time2Hierarchy.getDimension().getName());
        // The description is prefixed by the dimension usage name (because
        // dimension usage has no caption).
        assertEquals(
            "Time shared hierarchy description",
            time2Hierarchy.getDescription());
        // The hierarchy caption is prefixed by the dimension usage name
        // (because the dimension usage has no caption.
        assertEquals(
            "Time2",
            time2Hierarchy.getCaption());
        // No annotations.
        checkAnnotations(time2Hierarchy.getMetaData());

        final Dimension measuresDimension = cube.getDimensions().getFirst();
        final Hierarchy measuresHierarchy =
            measuresDimension.getHierarchies().getFirst();
        final Level measuresLevel =
            measuresHierarchy.getLevels().getFirst();
        final CatalogReader schemaReader = cube.getCatalogReader(null);
        final List<Member> measures =
            schemaReader.getLevelMembers(measuresLevel, true);
        final Member measure = measures.getFirst();
        assertEquals("Unit Sales", measure.getName());
        assertEquals("Unit Sales", measure.getCaption());
        assertEquals("Measure description", measure.getDescription());
        assertEquals(
            measure.getDescription(),
            measure.getPropertyValue(StandardProperty.DESCRIPTION_PROPERTY.getName()));
        assertEquals(
            measure.getCaption(),
            measure.getPropertyValue(StandardProperty.CAPTION.getName()));
        assertEquals(
            measure.getCaption(),
            measure.getPropertyValue(StandardProperty.MEMBER_CAPTION.getName()));
        checkAnnotations(measure.getMetaData(), "a", "Measure");

        // The implicitly created [Fact Count] measure
        final Member factCountMeasure = measures.get(1);
        assertEquals("Fact Count", factCountMeasure.getName());
        assertEquals(
            false,
            factCountMeasure.getPropertyValue(StandardProperty.VISIBLE.getName()));
        checkAnnotations(
            factCountMeasure.getMetaData(), "Internal Use",
            "For internal use");

        final Member calcMeasure = measures.get(2);
        assertEquals("Foo", calcMeasure.getName());
        assertEquals("Foo", calcMeasure.getCaption());
        assertEquals("Calc member description", calcMeasure.getDescription());
        assertEquals(
            calcMeasure.getDescription(),
            calcMeasure.getPropertyValue(StandardProperty.DESCRIPTION_PROPERTY.getName()));
        assertEquals(
            calcMeasure.getCaption(),
            calcMeasure.getPropertyValue(StandardProperty.CAPTION.getName()));
        assertEquals(
            calcMeasure.getCaption(),
            calcMeasure.getPropertyValue(StandardProperty.MEMBER_CAPTION.getName()));
        checkAnnotations(calcMeasure.getMetaData(), "a", "Calc member");

        final NamedSet namedSet = cube.getNamedSets()[0];
        assertEquals("Top Periods", namedSet.getName());
        assertEquals("Top Periods", namedSet.getCaption());
        assertEquals("Named set description", namedSet.getDescription());
        checkAnnotations(namedSet.getMetaData(), "a", "Named set");

        final Result result2 =
            executeQuery(context.getConnectionWithDefaultRole(), "select from [" + virtualCubeName + "]");
        final Cube cube2 = result2.getQuery().getCube();
        assertEquals("Virtual cube description", cube2.getDescription());
        checkAnnotations(cube2.getMetaData(), "a", "Virtual cube");

        final CatalogReader schemaReader2 = cube2.getCatalogReader(null);
        final Dimension measuresDimension2 = cube2.getDimensions().getFirst();
        final Hierarchy measuresHierarchy2 =
            measuresDimension2.getHierarchies().getFirst();
        final Level measuresLevel2 =
            measuresHierarchy2.getLevels().getFirst();
        final List<Member> measures2 =
            schemaReader2.getLevelMembers(measuresLevel2, true);
        final Member measure2 = measures2.get(0);
        assertEquals("Unit Sales", measure2.getName());
        assertEquals("Unit Sales", measure2.getCaption());
        assertEquals("Measure description", measure2.getDescription());
        assertEquals(
            measure2.getDescription(),
            measure2.getPropertyValue(StandardProperty.DESCRIPTION_PROPERTY.getName()));
        assertEquals(
            measure2.getCaption(),
            measure2.getPropertyValue(StandardProperty.CAPTION.getName()));
        assertEquals(
            measure2.getCaption(),
            measure2.getPropertyValue(StandardProperty.MEMBER_CAPTION.getName()));
        checkAnnotations(
            measure2.getMetaData(), "a", "Measure");
    }

    private static void checkAnnotations(
        MetaData metaData,
        String... nameVal)
    {
        assertNotNull(metaData);
        assertEquals(0, nameVal.length % 2);
        assertEquals(nameVal.length / 2, metaData.size());
        int i = 0;
        for (int j = 0; j < metaData.size(); j++) {
            String key=nameVal[i++];
            String val=nameVal[i++];
            assertEquals(val, metaData.get(key));
         }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCaption(Context<?> context) {
        class TestCaptionModifier extends PojoMappingModifier {

            public TestCaptionModifier(CatalogMapping catalog) {
                super(catalog);
            }

            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> ds = new ArrayList<>();
                if ("Sales".equals(cube.getName())) {
                    LevelMappingImpl l = LevelMappingImpl.builder()
                        .withName("Gender")
                        .withColumn(FoodmartMappingSupplier.GENDER_COLUMN_IN_CUSTOMER)
                        .withUniqueMembers(true)
                        .withCaptionColumn(SQLExpressionMappingColumnImpl.builder()
                        	.withSqls(List.of(SqlStatementMappingImpl.builder().withSql("'foobar'").withDialects(List.of("generic")).build()))
                            .withDataType(ColumnDataType.VARCHAR).build()).build();

                    HierarchyMappingImpl h = ExplicitHierarchyMappingImpl.builder()
                        .withHasAll(true).withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                        .withLevels(List.of(l))
                        .build();

                    DimensionConnectorMappingImpl d = DimensionConnectorMappingImpl.builder()
                    	.withOverrideDimensionName("Gender2")
                        .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        	.withName("Gender2")
                        	.withHierarchies(List.of(h)).build())
                        .build();
                    ds.add(d);
                }
                ds.addAll(super.cubeDimensionConnectors(cube));
                return ds;
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "  <Dimension name=\"Gender2\" foreignKey=\"customer_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\" >\n"
            + "      <Table name=\"customer\"/>\n"
            + "      <Level name=\"Gender\" column=\"gender\" uniqueMembers=\"true\" >\n"
            + "        <CaptionExpression>\n"
            + "          <SQL dialect='generic'>'foobar'</SQL>\n"
            + "        </CaptionExpression>\n"
            + "      </Level>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>"));
         */

        withSchema(context, TestCaptionModifier::new);


        switch (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())) {
        case POSTGRES:
            // Postgres fails with:
            //   Internal error: while building member cache; sql=[select
            //     "customer"."gender" as "c0", 'foobar' as "c1" from "customer"
            //     as "customer" group by "customer"."gender", 'foobar' order by
            //     "customer"."\ gender" ASC NULLS LAST]
            //   Caused by: org.postgresql.util.PSQLException: ERROR:
            //     non-integer constant in GROUP BY
            //
            // It's difficult for mondrian to spot that it's been given a
            // constant expression. We can live with this bug. Postgres
            // shouldn't be so picky, and people shouldn't be so daft.
            return;
        }
        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "select {[Gender2].Children} on columns from [Sales]");
        assertEquals(
            "foobar",
            result.getAxes()[0].getPositions().get(0).get(0).getCaption());
    }

    /**
     * Implementation of {@link MemberPropertyFormatter} that throws.
     */
    public static class DummyPropertyFormatter implements MemberPropertyFormatter {
        public DummyPropertyFormatter(Context<?> context) {
            throw new RuntimeException("oops");
        }

        @Override
		public String format(
            Member member, Property propertyName, Object propertyValue)
        {
            return null;
        }
    }

    /**
     * Unit test for bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-747">
     * MONDRIAN-747, "When joining a shared dimension into a cube at a level
     * other than its leaf level, Mondrian gives wrong results"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBugMondrian747(Context<?> context) {
        // Test case requires a pecular inline view, and it works on dialects
        // that scalar subqery, viz oracle. I believe that the mondrian code
        // being works in all dialects.
        switch (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())) {
        case ORACLE:
            break;
        default:
            return;
        }

        class TestBugMondrian747Modifier extends PojoMappingModifier {

            public TestBugMondrian747Modifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected CatalogMapping modifyCatalog(CatalogMapping schemaMappingOriginal) {
            	LevelMappingImpl stateLevel;
            	SumMeasureMappingImpl unitsales1Measure;
            	SumMeasureMappingImpl unitsales2Measure;
                StandardDimensionMappingImpl sd1 = StandardDimensionMappingImpl.builder()
                        .withName("Store")
                        .withHierarchies(List.of(
                            ExplicitHierarchyMappingImpl.builder()
                                .withHasAll(true)
                                .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                                .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                                .withLevels(List.of(
                                    LevelMappingImpl.builder()
                                        .withName("country")
                                        .withColumn(FoodmartMappingSupplier.STORE_CITY_COLUMN_IN_STORE)
                                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.STRING)
                                        .withUniqueMembers(false)
                                        .withLevelType(LevelType.REGULAR)
                                        .withHideMemberIfType(HideMemberIfType.NEVER)
                                        .build(),
                                    stateLevel = LevelMappingImpl.builder()
                                        .withName("state")
                                        .withColumn(FoodmartMappingSupplier.STORE_STATE_COLUMN_IN_STORE)
                                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.STRING)
                                        .withUniqueMembers(false)
                                        .withLevelType(LevelType.REGULAR)
                                        .withHideMemberIfType(HideMemberIfType.NEVER)
                                        .build(),
                                    LevelMappingImpl.builder()
                                        .withName("city")
                                        .withColumn(FoodmartMappingSupplier.STORE_CITY_COLUMN_IN_STORE)
                                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.STRING)
                                        .withUniqueMembers(false)
                                        .withLevelType(LevelType.REGULAR)
                                        .withHideMemberIfType(HideMemberIfType.NEVER)
                                        .build()
                                ))
                                .build()
                        ))
                        .build();

                StandardDimensionMappingImpl sd2 = StandardDimensionMappingImpl.builder()
                        .withName("Product")
                        .withHierarchies(List.of(
                            ExplicitHierarchyMappingImpl.builder()
                                .withName("New Hierarchy 0")
                                .withHasAll(true)
                                .withPrimaryKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                                .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_TABLE).build())
                                .withLevels(List.of(
                                    LevelMappingImpl.builder()
                                        .withName("product_name")
                                        .withColumn(FoodmartMappingSupplier.PRODUCT_NAME_COLUMN_IN_PRODUCT)
                                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.STRING)
                                        .withUniqueMembers(false)
                                        .withLevelType(LevelType.REGULAR)
                                        .withHideMemberIfType(HideMemberIfType.NEVER)
                                        .build()
                                ))
                                .build()
                        ))
                        .build();

                    PhysicalCubeMappingImpl c1 = PhysicalCubeMappingImpl.builder()
                        .withName("cube1")
                        .withCache(true)
                        .withEnabled(true)
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                        .withDimensionConnectors(List.of(
                        	DimensionConnectorMappingImpl.builder()
                                .withDimension(sd1)
                                .withOverrideDimensionName("Store")
                                .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withDimension(sd2)
                                .withOverrideDimensionName("Product")
                                .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                                .build()
                            ))
                        .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                        		.withMeasures(List.of(
                        				unitsales1Measure = SumMeasureMappingImpl.builder()
                                        .withName("unitsales1")
                                        .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                        .withDatatype(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)

                                        .withVisible(true)
                                        .build()
                                ))
                        		.build()))
                        .build();
                    PhysicalColumnMappingImpl salesStateProvince = PhysicalColumnMappingImpl.builder().withName("sales_state_province").build();
                    SqlSelectQueryMappingImpl vv = SqlSelectQueryMappingImpl.builder()
                            .withAlias("sales_fact_1997_test")
                            .withSql(
                                    ((SqlViewMappingImpl.Builder) SqlViewMappingImpl.builder()
                                    .withColumns(List.of(
                                        FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997,
                                        FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997,
                                        FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997,
                                        FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997,
                                        FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997,
                                        FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_SALES_FACT_1997,
                                        FoodmartMappingSupplier.STORE_COST_COLUMN_IN_SALES_FACT_1997,
                                        FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997,
                                        salesStateProvince
                                    )))
                                    .withSqlStatements(List.of(
                                        SqlStatementMappingImpl.builder().withSql("select \"product_id\", \"time_id\", \"customer_id\", \"promotion_id\", " +
                                                "\"store_id\", \"store_sales\", \"store_cost\", \"unit_sales\", (select \"store_state\" " +
                                                "from \"store\" where \"store_id\" = \"sales_fact_1997\".\"store_id\") as " +
                                                                               "\"sales_state_province\" from \"sales_fact_1997\"").withDialects(List.of("generic")).build()
                                    ))
                            .build()).build();


                    PhysicalCubeMappingImpl c2 = PhysicalCubeMappingImpl.builder()
                        .withName("cube2")
                        .withCache(true)
                        .withEnabled(true)
                        .withQuery(vv)
                        .withDimensionConnectors(List.of(
                            	DimensionConnectorMappingImpl.builder()
                                    .withDimension(sd1)
                                    .withOverrideDimensionName("Store")
                                    .withForeignKey(salesStateProvince)
                                    .withLevel(stateLevel)
                                    .build(),
                                DimensionConnectorMappingImpl.builder()
                                    .withDimension(sd2)
                                    .withOverrideDimensionName("Product")
                                    .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                                    .build()
                        ))
                        .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                        		.withMeasures(List.of(
                        				unitsales2Measure = SumMeasureMappingImpl.builder()
                                        .withName("unitsales2")
                                        .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                        .withDatatype(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)

                                        .withVisible(true)
                                        .build()
                                ))
                        		.build()))
                        .build();

                    VirtualCubeMappingImpl vc = VirtualCubeMappingImpl.builder()
                        .withEnabled(true)
                        .withName("virtual_cube")
                        .withDimensionConnectors(List.of(
                            DimensionConnectorMappingImpl.builder()
                            	.withOverrideDimensionName("Store")
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                            	.withOverrideDimensionName("Product")
                                .build()
                        ))
                        .withReferencedMeasures(List.of(
                        	unitsales1Measure,
                        	unitsales2Measure
                        ))
                        .build();
                    return CatalogMappingImpl.builder()
                        .withName("Test_DimensionUsage")
                        .withDbSchemas((List<DatabaseSchemaMappingImpl>) catalogDatabaseSchemas( schemaMappingOriginal))
                        .withCubes(List.of(
                            c1, c2, vc
                        ))
                        .build();

            }
        }
        /*
        withSchema(context,
            "<Schema name='Test_DimensionUsage'> \n"
            + "  <Dimension type='StandardDimension' name='Store'> \n"
            + "    <Hierarchy hasAll='true' primaryKey='store_id'> \n"
            + "      <Table name='store'> \n"
            + "      </Table> \n"
            + "      <Level name='country' column='store_country' type='String' uniqueMembers='false' levelType='Regular' hideMemberIf='Never'> \n"
            + "      </Level> \n"
            + "      <Level name='state' column='store_state' type='String' uniqueMembers='false' levelType='Regular' hideMemberIf='Never'> \n"
            + "      </Level> \n"
            + "      <Level name='city' column='store_city' type='String' uniqueMembers='false' levelType='Regular' hideMemberIf='Never'> \n"
            + "      </Level> \n"
            + "    </Hierarchy> \n"
            + "  </Dimension> \n"
            + "  <Dimension type='StandardDimension' name='Product'> \n"
            + "    <Hierarchy name='New Hierarchy 0' hasAll='true' primaryKey='product_id'> \n"
            + "      <Table name='product'> \n"
            + "      </Table> \n"
            + "      <Level name='product_name' column='product_name' type='String' uniqueMembers='false' levelType='Regular' hideMemberIf='Never'> \n"
            + "      </Level> \n"
            + "    </Hierarchy> \n"
            + "  </Dimension> \n"
            + "  <Cube name='cube1' cache='true' enabled='true'> \n"
            + "    <Table name='sales_fact_1997'> \n"
            + "    </Table> \n"
            + "    <DimensionUsage source='Store' name='Store' foreignKey='store_id'> \n"
            + "    </DimensionUsage> \n"
            + "    <DimensionUsage source='Product' name='Product' foreignKey='product_id'> \n"
            + "    </DimensionUsage> \n"
            + "    <Measure name='unitsales1' column='unit_sales' datatype='Numeric' aggregator='sum' visible='true'> \n"
            + "    </Measure> \n"
            + "  </Cube> \n"
            + "  <Cube name='cube2' cache='true' enabled='true'> \n"
//            + "    <Table name='sales_fact_1997_test'/> \n"
            + "    <View alias='sales_fact_1997_test'> \n"
            + "      <SQL dialect='generic'>select \"product_id\", \"time_id\", \"customer_id\", \"promotion_id\", \"store_id\", \"store_sales\", \"store_cost\", \"unit_sales\", (select \"store_state\" from \"store\" where \"store_id\" = \"sales_fact_1997\".\"store_id\") as \"sales_state_province\" from \"sales_fact_1997\"</SQL>\n"
            + "    </View> \n"
            + "    <DimensionUsage source='Store' level='state' name='Store' foreignKey='sales_state_province'> \n"
            + "    </DimensionUsage> \n"
            + "    <DimensionUsage source='Product' name='Product' foreignKey='product_id'> \n"
            + "    </DimensionUsage> \n"
            + "    <Measure name='unitsales2' column='unit_sales' datatype='Numeric' aggregator='sum' visible='true'> \n"
            + "    </Measure> \n"
            + "  </Cube> \n"
            + "  <VirtualCube enabled='true' name='virtual_cube'> \n"
            + "    <VirtualCubeDimension name='Store'> \n"
            + "    </VirtualCubeDimension> \n"
            + "    <VirtualCubeDimension name='Product'> \n"
            + "    </VirtualCubeDimension> \n"
            + "    <VirtualCubeMeasure cubeName='cube1' name='[Measures].[unitsales1]' visible='true'> \n"
            + "    </VirtualCubeMeasure> \n"
            + "    <VirtualCubeMeasure cubeName='cube2' name='[Measures].[unitsales2]' visible='true'> \n"
            + "    </VirtualCubeMeasure> \n"
            + "  </VirtualCube> \n"
            + "</Schema>");
        */

        if (!Bug.BugMondrian747Fixed
            && context.getConfigValue(ConfigConstants.ENABLE_GROUPING_SETS, ConfigConstants.ENABLE_GROUPING_SETS_DEFAULT_VALUE, Boolean.class))
        {
            // With grouping sets enabled, MONDRIAN-747 behavior is even worse.
            return;
        }

        withSchema(context, TestBugMondrian747Modifier::new);

        // [Store].[All Stores] and [Store].[USA] should be 266,773. A higher
        // value would indicate that there is a cartesian product going on --
        // because "store_state" is not unique in "store" table.
        final String x = !Bug.BugMondrian747Fixed
            ? "1,379,620"
            : "266,773";
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select non empty {[Measures].[unitsales2]} on 0,\n"
            + " non empty [Store].members on 1\n"
            + "from [cube2]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[unitsales2]}\n"
            + "Axis #2:\n"
            + "{[Store].[All Stores]}\n"
            + "{[Store].[USA]}\n"
            + "{[Store].[USA].[CA]}\n"
            + "{[Store].[USA].[OR]}\n"
            + "{[Store].[USA].[WA]}\n"
            + "Row #0: 266,773\n"
            + "Row #1: " + x + "\n"
            + "Row #2: 373,740\n"
            + "Row #3: 135,318\n"
            + "Row #4: 870,562\n");

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select non empty {[Measures].[unitsales1]} on 0,\n"
            + " non empty [Store].members on 1\n"
            + "from [cube1]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[unitsales1]}\n"
            + "Axis #2:\n"
            + "{[Store].[All Stores]}\n"
            + "{[Store].[USA]}\n"
            + "{[Store].[USA].[CA]}\n"
            + "{[Store].[USA].[CA].[Beverly Hills]}\n"
            + "{[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[USA].[CA].[San Francisco]}\n"
            + "{[Store].[USA].[OR]}\n"
            + "{[Store].[USA].[OR].[Portland]}\n"
            + "{[Store].[USA].[OR].[Salem]}\n"
            + "{[Store].[USA].[WA]}\n"
            + "{[Store].[USA].[WA].[Bellingham]}\n"
            + "{[Store].[USA].[WA].[Bremerton]}\n"
            + "{[Store].[USA].[WA].[Seattle]}\n"
            + "{[Store].[USA].[WA].[Spokane]}\n"
            + "{[Store].[USA].[WA].[Tacoma]}\n"
            + "{[Store].[USA].[WA].[Walla Walla]}\n"
            + "{[Store].[USA].[WA].[Yakima]}\n"
            + "Row #0: 266,773\n"
            + "Row #1: 266,773\n"
            + "Row #2: 74,748\n"
            + "Row #3: 21,333\n"
            + "Row #4: 25,663\n"
            + "Row #5: 25,635\n"
            + "Row #6: 2,117\n"
            + "Row #7: 67,659\n"
            + "Row #8: 26,079\n"
            + "Row #9: 41,580\n"
            + "Row #10: 124,366\n"
            + "Row #11: 2,237\n"
            + "Row #12: 24,576\n"
            + "Row #13: 25,011\n"
            + "Row #14: 23,591\n"
            + "Row #15: 35,257\n"
            + "Row #16: 2,203\n"
            + "Row #17: 11,491\n");

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select non empty {[Measures].[unitsales2], [Measures].[unitsales1]} on 0,\n"
            + " non empty [Store].members on 1\n"
            + "from [virtual_cube]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[unitsales2]}\n"
            + "{[Measures].[unitsales1]}\n"
            + "Axis #2:\n"
            + "{[Store].[All Stores]}\n"
            + "{[Store].[USA]}\n"
            + "{[Store].[USA].[CA]}\n"
            + "{[Store].[USA].[CA].[Beverly Hills]}\n"
            + "{[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[USA].[CA].[San Francisco]}\n"
            + "{[Store].[USA].[OR]}\n"
            + "{[Store].[USA].[OR].[Portland]}\n"
            + "{[Store].[USA].[OR].[Salem]}\n"
            + "{[Store].[USA].[WA]}\n"
            + "{[Store].[USA].[WA].[Bellingham]}\n"
            + "{[Store].[USA].[WA].[Bremerton]}\n"
            + "{[Store].[USA].[WA].[Seattle]}\n"
            + "{[Store].[USA].[WA].[Spokane]}\n"
            + "{[Store].[USA].[WA].[Tacoma]}\n"
            + "{[Store].[USA].[WA].[Walla Walla]}\n"
            + "{[Store].[USA].[WA].[Yakima]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: 266,773\n"
            + "Row #1: 1,379,620\n"
            + "Row #1: 266,773\n"
            + "Row #2: 373,740\n"
            + "Row #2: 74,748\n"
            + "Row #3: \n"
            + "Row #3: 21,333\n"
            + "Row #4: \n"
            + "Row #4: 25,663\n"
            + "Row #5: \n"
            + "Row #5: 25,635\n"
            + "Row #6: \n"
            + "Row #6: 2,117\n"
            + "Row #7: 135,318\n"
            + "Row #7: 67,659\n"
            + "Row #8: \n"
            + "Row #8: 26,079\n"
            + "Row #9: \n"
            + "Row #9: 41,580\n"
            + "Row #10: 870,562\n"
            + "Row #10: 124,366\n"
            + "Row #11: \n"
            + "Row #11: 2,237\n"
            + "Row #12: \n"
            + "Row #12: 24,576\n"
            + "Row #13: \n"
            + "Row #13: 25,011\n"
            + "Row #14: \n"
            + "Row #14: 23,591\n"
            + "Row #15: \n"
            + "Row #15: 35,257\n"
            + "Row #16: \n"
            + "Row #16: 2,203\n"
            + "Row #17: \n"
            + "Row #17: 11,491\n");
    }

    /**
     * Unit test for bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-463">
     * MONDRIAN-463, "Snowflake dimension with 3-way join."</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBugMondrian463(Context<?> context) {
        if (!SystemWideProperties.instance().FilterChildlessSnowflakeMembers)
        {
            // Similar to aggregates. If we turn off filtering,
            // we get wild stuff because of referential integrity.
            return;
        }
        class TestBugMondrian463Modifier1 extends PojoMappingModifier {

            public TestBugMondrian463Modifier1(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                if ("Sales".equals(cube.getName())) {
                    LevelMappingImpl l1 = LevelMappingImpl.builder()
                            .withName("Product Family")
                            .withColumn(FoodmartMappingSupplier.PRODUCT_FAMILY_COLUMN_IN_PRODUCT_CLASS)
                            .withUniqueMembers(true)
                            .build();
                    LevelMappingImpl l2 = LevelMappingImpl.builder()
                            .withName("Product Department")
                            .withColumn(FoodmartMappingSupplier.PRODUCT_DEPARTMENT_COLUMN_IN_PRODUCT_CLASS)
                            .withUniqueMembers(false)
                            .build();
                    LevelMappingImpl l3 = LevelMappingImpl.builder()
                            .withName("Product Category")
                            .withColumn(FoodmartMappingSupplier.PRODUCT_CATEGORY_COLUMN_IN_PRODUCT_CLASS)
                            .withUniqueMembers(false)
                            .build();
                    LevelMappingImpl l4 = LevelMappingImpl.builder()
                            .withName("Product Subcategory")
                            .withColumn(FoodmartMappingSupplier.PRODUCT_SUBCATEGORY_COLUMN_IN_PRODUCT_CLASS)
                            .withUniqueMembers(false)
                            .build();
                    LevelMappingImpl l5 = LevelMappingImpl.builder()
                            .withName("Product Class")
                            .withColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                            .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                            .withUniqueMembers(true)
                            .build();
                    LevelMappingImpl l6 = LevelMappingImpl.builder()
                            .withName("Brand Name")
                            .withColumn(FoodmartMappingSupplier.BRAND_NAME_COLUMN_IN_PRODUCT)
                            .withUniqueMembers(false)
                            .build();
                    LevelMappingImpl l7 = LevelMappingImpl.builder()
                            .withName("Product Name")
                            .withColumn(FoodmartMappingSupplier.PRODUCT_NAME_COLUMN_IN_PRODUCT)
                            .withUniqueMembers(true)
                            .build();

                        JoinQueryMappingImpl j1 = JoinQueryMappingImpl.builder()
                        		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.REGION_ID_COLUMN_IN_STORE)
                        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build()).build())
                        		.withRight(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT_CLASS)
                        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE).build()).build())
                        		.build();

                        JoinQueryMappingImpl j = JoinQueryMappingImpl.builder()
                        		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT)
                        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_TABLE).build()).build())
                        		.withRight(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                        				.withQuery(j1).build())
                        		.build();

                        HierarchyMappingImpl h = ExplicitHierarchyMappingImpl.builder()
                            .withHasAll(true).withPrimaryKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                            .withQuery(j)
                            .withLevels(List.of(l1, l2, l3, l4, l5, l6, l7))
                            .build();

                        DimensionConnectorMappingImpl d = DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Product3")
                                .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Product3")
                                	.withHierarchies(List.of(h)).build())
                            .build();
                        result.add(d);

                    }
                	result.addAll(super.cubeDimensionConnectors(cube));
                    return result;
                }
        }

        // To build a dimension that is a 3-way snowflake, take the 2-way
        // product -> product_class join and convert to product -> store ->
        // product_class.
        //
        // It works because product_class_id covers the range 1 .. 110;
        // store_id covers every value in 0 .. 24;
        // region_id has 24 distinct values in the range 0 .. 106 (region_id 25
        // occurs twice).
        // Therefore in store, store_id -> region_id is a 25 to 24 mapping.
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                "Sales",
                "<Dimension name='Product3' foreignKey='product_id'>\n"
                + "  <Hierarchy hasAll='true' primaryKey='product_id' primaryKeyTable='product'>\n"
                + "    <Join leftKey='product_class_id' rightKey='store_id'>\n"
                + "      <Table name='product'/>\n"
                + "      <Join leftKey='region_id' rightKey='product_class_id'>\n"
                + "        <Table name='store'/>\n"
                + "        <Table name='product_class'/>\n"
                + "      </Join>\n"
                + "    </Join>\n"
                + "    <Level name='Product Family' table='product_class' column='product_family' uniqueMembers='true'/>\n"
                + "    <Level name='Product Department' table='product_class' column='product_department' uniqueMembers='false'/>\n"
                + "    <Level name='Product Category' table='product_class' column='product_category' uniqueMembers='false'/>\n"
                + "    <Level name='Product Subcategory' table='product_class' column='product_subcategory' uniqueMembers='false'/>\n"
                + "    <Level name='Product Class' table='store' column='store_id' type='Numeric' uniqueMembers='true'/>\n"
                + "    <Level name='Brand Name' table='product' column='brand_name' uniqueMembers='false'/>\n"
                + "    <Level name='Product Name' table='product' column='product_name' uniqueMembers='true'/>\n"
                + "  </Hierarchy>\n"
                + "</Dimension>"));
         */
        withSchema(context, TestBugMondrian463Modifier1::new);
        checkBugMondrian463(context);
        // As above, but using shared dimension.
        if (context.getConfigValue(ConfigConstants.READ_AGGREGATES, ConfigConstants.READ_AGGREGATES_DEFAULT_VALUE ,Boolean.class)
            && context.getConfigValue(ConfigConstants.USE_AGGREGATES, ConfigConstants.USE_AGGREGATES_DEFAULT_VALUE ,Boolean.class))
        {
            // With aggregates enabled, query gives different answer. This is
            // expected because some of the foreign keys have referential
            // integrity problems.
            return;
        }
        class TestBugMondrian463Modifier2 extends PojoMappingModifier {

            public TestBugMondrian463Modifier2(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected CatalogMapping modifyCatalog(CatalogMapping catalog2) {
                LevelMappingImpl l1 = LevelMappingImpl.builder()
                        .withName("Product Family")
                        .withColumn(FoodmartMappingSupplier.PRODUCT_FAMILY_COLUMN_IN_PRODUCT_CLASS)
                        .withUniqueMembers(true)
                        .build();
                    LevelMappingImpl l2 = LevelMappingImpl.builder()
                        .withName("Product Department")
                        .withColumn(FoodmartMappingSupplier.PRODUCT_DEPARTMENT_COLUMN_IN_PRODUCT_CLASS)
                        .withUniqueMembers(false)
                        .build();
                    LevelMappingImpl l3 = LevelMappingImpl.builder()
                        .withName("Product Category")
                        .withColumn(FoodmartMappingSupplier.PRODUCT_CATEGORY_COLUMN_IN_PRODUCT_CLASS)
                        .withUniqueMembers(false)
                        .build();
                    LevelMappingImpl l4 = LevelMappingImpl.builder()
                        .withName("Product Subcategory")
                        .withColumn(FoodmartMappingSupplier.PRODUCT_SUBCATEGORY_COLUMN_IN_PRODUCT_CLASS)
                        .withUniqueMembers(false)
                        .build();
                    LevelMappingImpl l5 = LevelMappingImpl.builder()
                        .withName("Product Class")
                        .withColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                        .withUniqueMembers(true)
                        .build();
                    LevelMappingImpl l6 = LevelMappingImpl.builder()
                        .withName("Brand Name")
                        .withColumn(FoodmartMappingSupplier.BRAND_NAME_COLUMN_IN_PRODUCT)
                        .withUniqueMembers(false)
                        .build();
                    LevelMappingImpl l7 = LevelMappingImpl.builder()
                        .withName("Product Name")
                        .withColumn(FoodmartMappingSupplier.PRODUCT_NAME_COLUMN_IN_PRODUCT)
                        .withUniqueMembers(true)
                        .build();

                	JoinQueryMappingImpl j1 = JoinQueryMappingImpl.builder()
                    		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.REGION_ID_COLUMN_IN_STORE)
                    				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                    				.build())
                    		.withRight(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT_CLASS)
                    				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE).build())
                    				.build()).build();

                	JoinQueryMappingImpl j = JoinQueryMappingImpl.builder()
                    		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT)
                    				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_TABLE).build())
                    				.build())
                    		.withRight(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                    				.withQuery(j1)
                    				.build()).build();

                    HierarchyMappingImpl h1 = ExplicitHierarchyMappingImpl.builder()
                        .withHasAll(true)
                        .withPrimaryKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                        .withQuery(j)
                        .withLevels(List.of(l1, l2, l3, l4, l5, l6, l7))
                        .build();

                    DimensionMappingImpl product3Dimension = StandardDimensionMappingImpl.builder()
                    	.withName("Product3")
                    	.withHierarchies(List.of(h1)).build();

                    PhysicalCubeMappingImpl c = PhysicalCubeMappingImpl.builder()
                        .withName("Sales")
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                        .withDimensionConnectors(List.of(
                        	DimensionConnectorMappingImpl.builder()
                        		.withOverrideDimensionName("Time")
                        		.withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                        		.withDimension(TimeDimensionMappingImpl.builder()
                        			.withName("Time")
                        			.withHierarchies(List.of(
                        				ExplicitHierarchyMappingImpl.builder()
                                        .withHasAll(false)
                                        .withPrimaryKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.TIME_BY_DAY_TABLE).build())
                                        .withLevels(List.of(
                                        	LevelMappingImpl.builder()
                                                .withName("Year")
                                                .withColumn(FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY)
                                                .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                                                .withUniqueMembers(true)
                                                .withLevelType(LevelType.TIME_YEARS)
                                                .build(),
                                            LevelMappingImpl.builder()
                                                .withName("Quarter")
                                                .withColumn(FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY)
                                                .withUniqueMembers(false)
                                                .withLevelType(LevelType.TIME_QUARTERS)
                                                .build(),
                                            LevelMappingImpl.builder()
                                                .withName("Month")
                                                .withColumn(FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY)
                                                .withUniqueMembers(false)
                                                .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                                                .withLevelType(LevelType.TIME_MONTHS)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                            	.withOverrideDimensionName("Product3")
                            	.withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                            	.withDimension(product3Dimension)
                                .build()
                        ))
                        .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                        		.withMeasures(List.of(
                                    SumMeasureMappingImpl.builder()
                                        .withName("Unit Sales")
                                        .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                                        .withFormatString("#,###")
                                        .build()
                                ))
                        		.build()))
                        .build();
           	 	return CatalogMappingImpl.builder()
        			 .withName("FoodMart")
                     .withDbSchemas((List<DatabaseSchemaMappingImpl>) catalogDatabaseSchemas( catalog2))
                     .withCubes(List.of(c))
                     .build();

            }

        }
        /*
        withSchema(context,
                "<?xml version='1.0'?>\n"
                + "<Schema name='FoodMart'>\n"
                + "<Dimension name='Product3'>\n"
                + "  <Hierarchy hasAll='true' primaryKey='product_id' primaryKeyTable='product'>\n"
                + "    <Join leftKey='product_class_id' rightKey='store_id'>\n"
                + "      <Table name='product'/>\n"
                + "      <Join leftKey='region_id' rightKey='product_class_id'>\n"
                + "        <Table name='store'/>\n"
                + "        <Table name='product_class'/>\n"
                + "      </Join>\n"
                + "    </Join>\n"
                + "    <Level name='Product Family' table='product_class' column='product_family' uniqueMembers='true'/>\n"
                + "    <Level name='Product Department' table='product_class' column='product_department' uniqueMembers='false'/>\n"
                + "    <Level name='Product Category' table='product_class' column='product_category' uniqueMembers='false'/>\n"
                + "    <Level name='Product Subcategory' table='product_class' column='product_subcategory' uniqueMembers='false'/>\n"
                + "    <Level name='Product Class' table='store' column='store_id' type='Numeric' uniqueMembers='true'/>\n"
                + "    <Level name='Brand Name' table='product' column='brand_name' uniqueMembers='false'/>\n"
                + "    <Level name='Product Name' table='product' column='product_name' uniqueMembers='true'/>\n"
                + "  </Hierarchy>\n"
                + "</Dimension>\n"
                + "<Cube name='Sales'>\n"
                + "  <Table name='sales_fact_1997'/>\n"
                + "  <Dimension name='Time' type='TimeDimension' foreignKey='time_id'>\n"
                + "    <Hierarchy hasAll='false' primaryKey='time_id'>\n"
                + "      <Table name='time_by_day'/>\n"
                + "      <Level name='Year' column='the_year' type='Numeric' uniqueMembers='true'\n"
                + "          levelType='TimeYears'/>\n"
                + "      <Level name='Quarter' column='quarter' uniqueMembers='false'\n"
                + "          levelType='TimeQuarters'/>\n"
                + "      <Level name='Month' column='month_of_year' uniqueMembers='false' type='Numeric'\n"
                + "          levelType='TimeMonths'/>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"
                + "  <DimensionUsage source='Product3' name='Product3' foreignKey='product_id'/>\n"
                + "  <Measure name='Unit Sales' column='unit_sales' aggregator='sum'\n"
                + "      formatString='#,###'/>\n"
                + "</Cube>\n"
                + "</Schema>");
         */
        withSchema(context, TestBugMondrian463Modifier2::new);
        checkBugMondrian463(context);
    }

    private void checkBugMondrian463(Context<?> context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select [Measures] on 0,\n"
            + " head([Product3].members, 10) on 1\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Product3].[Product3].[All Product3s]}\n"
            + "{[Product3].[Product3].[Drink]}\n"
            + "{[Product3].[Product3].[Drink].[Baking Goods]}\n"
            + "{[Product3].[Product3].[Drink].[Baking Goods].[Dry Goods]}\n"
            + "{[Product3].[Product3].[Drink].[Baking Goods].[Dry Goods].[Coffee]}\n"
            + "{[Product3].[Product3].[Drink].[Baking Goods].[Dry Goods].[Coffee].[24]}\n"
            + "{[Product3].[Product3].[Drink].[Baking Goods].[Dry Goods].[Coffee].[24].[Amigo]}\n"
            + "{[Product3].[Product3].[Drink].[Baking Goods].[Dry Goods].[Coffee].[24].[Amigo].[Amigo Lox]}\n"
            + "{[Product3].[Product3].[Drink].[Baking Goods].[Dry Goods].[Coffee].[24].[Curlew]}\n"
            + "{[Product3].[Product3].[Drink].[Baking Goods].[Dry Goods].[Coffee].[24].[Curlew].[Curlew Lox]}\n"
            + "Row #0: 266,773\n"
            + "Row #1: 2,647\n"
            + "Row #2: 835\n"
            + "Row #3: 835\n"
            + "Row #4: 835\n"
            + "Row #5: 835\n"
            + "Row #6: 175\n"
            + "Row #7: 175\n"
            + "Row #8: 186\n"
            + "Row #9: 186\n");
    }

    /**
     * Tests that a join nested left-deep, that is (Join (Join A B) C), fails.
     * The correct way to use a join is right-deep, that is (Join A (Join B C)).
     * Same schema as {@link #testBugMondrian463}, except left-deep.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLeftDeepJoinFails(Context<?> context) {
        class TestLeftDeepJoinFailsModifier extends PojoMappingModifier {

            public TestLeftDeepJoinFailsModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> ds = new ArrayList<>();
                if ("Sales".equals(cube.getName())) {
                    LevelMappingImpl l1 = LevelMappingImpl.builder()
                            .withName("Product Family")
                            .withColumn(FoodmartMappingSupplier.PRODUCT_FAMILY_COLUMN_IN_PRODUCT_CLASS)
                            .withUniqueMembers(true)
                            .build();
                    LevelMappingImpl l2 = LevelMappingImpl.builder()
                            .withName("Product Department")
                            .withColumn(FoodmartMappingSupplier.PRODUCT_DEPARTMENT_COLUMN_IN_PRODUCT_CLASS)
                            .withUniqueMembers(false)
                            .build();
                    LevelMappingImpl l3 = LevelMappingImpl.builder()
                            .withName("Product Category")
                            .withColumn(FoodmartMappingSupplier.PRODUCT_CATEGORY_COLUMN_IN_PRODUCT_CLASS)
                            .withUniqueMembers(false)
                            .build();
                    LevelMappingImpl l4 = LevelMappingImpl.builder()
                            .withName("Product Subcategory")
                            .withColumn(FoodmartMappingSupplier.PRODUCT_SUBCATEGORY_COLUMN_IN_PRODUCT_CLASS)
                            .withUniqueMembers(false)
                            .build();
                    LevelMappingImpl l5 = LevelMappingImpl.builder()
                            .withName("Product Class")
                            .withColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                            .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                            .withUniqueMembers(true)
                            .build();
                    LevelMappingImpl l6 = LevelMappingImpl.builder()
                            .withName("Brand Name")
                            .withColumn(FoodmartMappingSupplier.BRAND_NAME_COLUMN_IN_PRODUCT)
                            .withUniqueMembers(false)
                            .build();
                    LevelMappingImpl l7 = LevelMappingImpl.builder()
                            .withName("Product Name")
                            .withColumn(FoodmartMappingSupplier.PRODUCT_NAME_COLUMN_IN_PRODUCT)
                            .withUniqueMembers(true)
                            .build();

                	JoinQueryMappingImpl j1 = JoinQueryMappingImpl.builder()
                    		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT)
                    				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_TABLE).build())
                    				.build())
                    		.withRight(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.REGION_ID_COLUMN_IN_STORE)
                    				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                    				.build()).build();

                	JoinQueryMappingImpl j = JoinQueryMappingImpl.builder()
                    		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                    				.withQuery(j1)
                    				.build())
                    		.withRight(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT_CLASS)
                    				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE).build())
                    				.build()).build();

                    HierarchyMappingImpl h = ExplicitHierarchyMappingImpl.builder()
                            .withHasAll(true).withPrimaryKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                            .withQuery(j)
                            .withLevels(List.of(l1, l2, l3, l4, l5, l6, l7))
                            .build();

                        DimensionConnectorMappingImpl d = DimensionConnectorMappingImpl.builder()
                        	.withOverrideDimensionName(CUBES_AB)
                            .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            .withName("Product3")
                            .withHierarchies(List.of(h)).build())
                            .build();
                        ds.add(d);
                    }
                    return ds;
                }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "<Dimension name='Product3' foreignKey='product_id'>\n"
            + "  <Hierarchy hasAll='true' primaryKey='product_id' primaryKeyTable='product'>\n"
            + "    <Join leftKey='store_id' rightKey='product_class_id'>\n"
            + "      <Join leftKey='product_class_id' rightKey='region_id'>\n"
            + "        <Table name='product'/>\n"
            + "        <Table name='store'/>\n"
            + "      </Join>\n"
            + "      <Table name='product_class'/>\n"
            + "    </Join>\n"
            + "    <Level name='Product Family' table='product_class' column='product_family' uniqueMembers='true'/>\n"
            + "    <Level name='Product Department' table='product_class' column='product_department' uniqueMembers='false'/>\n"
            + "    <Level name='Product Category' table='product_class' column='product_category' uniqueMembers='false'/>\n"
            + "    <Level name='Product Subcategory' table='product_class' column='product_subcategory' uniqueMembers='false'/>\n"
            + "    <Level name='Product Class' table='store' column='store_id' uniqueMembers='true'/>\n"
            + "    <Level name='Brand Name' table='product' column='brand_name' uniqueMembers='false'/>\n"
            + "    <Level name='Product Name' table='product' column='product_name' uniqueMembers='true'/>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>"));
         */
        try {
            withSchema(context, TestLeftDeepJoinFailsModifier::new);
            assertSimpleQuery(context.getConnectionWithDefaultRole());
            fail("expected error");
        } catch (OlapRuntimeException e) {
            assertEquals(
                "Left side of join must not be a join; mondrian only supports right-deep joins.",
                e.getMessage());
        }
    }

    /**
     * Test for MONDRIAN-943 and MONDRIAN-465.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCaptionWithOrdinalColumn(Context<?> context) {
    	context.getCatalogCache().clear();
        class TestCaptionWithOrdinalColumnModifier extends PojoMappingModifier {

            public TestCaptionWithOrdinalColumnModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> ds = new ArrayList<>();
                ds.addAll(super.cubeDimensionConnectors(cube));
                if ("HR".equals(cube.getName())) {
                	DimensionConnectorMappingImpl d = DimensionConnectorMappingImpl.builder()
                		.withOverrideDimensionName("Position").withForeignKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_SALARY)
                		.withDimension(StandardDimensionMappingImpl.builder()
                			.withName("Position")
                			.withHierarchies(List.of(
                            ExplicitHierarchyMappingImpl.builder()
                                .withHasAll(true)
                                .withAllMemberName("All Position")
                                .withPrimaryKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_EMPLOYEE)
                                .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.EMPLOYEE_TABLE).build())
                                .withLevels(List.of(
                                    LevelMappingImpl.builder()
                                        .withName("Management Role")
                                        .withUniqueMembers(true)
                                        .withColumn(FoodmartMappingSupplier.MANAGEMENT_ROLE_COLUMN_IN_EMPLOYEE)
                                        .build(),
                                    LevelMappingImpl.builder()
                                        .withName("Position Title")
                                        .withUniqueMembers(false)
                                        .withColumn(FoodmartMappingSupplier.POSITION_TITLE_COLUMN_IN_EMPLOYEE)
                                        .withOrdinalColumn(FoodmartMappingSupplier.POSITION_ID_COLUMN_IN_EMPLOYEE)
                                        .withCaptionColumn(FoodmartMappingSupplier.POSITION_TITLE_COLUMN_IN_EMPLOYEE)
                                        .build()
                                ))
                                .build()
                        )).build())
                        .build();
                    ds.add(d);
                }
                return ds;
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                "HR",
                "<Dimension name=\"Position\" foreignKey=\"employee_id\">\n"
                + "  <Hierarchy hasAll=\"true\" allMemberName=\"All Position\" primaryKey=\"employee_id\">\n"
                + "    <Table name=\"employee\"/>\n"
                + "    <Level name=\"Management Role\" uniqueMembers=\"true\" column=\"management_role\"/>\n"
                + "    <Level name=\"Position Title\" uniqueMembers=\"false\" column=\"position_title\" ordinalColumn=\"position_id\" captionColumn=\"position_title\"/>\n"
                + "  </Hierarchy>\n"
                + "</Dimension>\n"));
         */
        withSchema(context, TestCaptionWithOrdinalColumnModifier::new);

        String mdxQuery =
            "WITH SET [#DataSet#] as '{Descendants([Position].[All Position], 2)}' "
            + "SELECT {[Measures].[Org Salary]} on columns, "
            + "NON EMPTY Hierarchize({[#DataSet#]}) on rows FROM [HR]";
        Result result = executeQuery(context.getConnectionWithDefaultRole(), mdxQuery);
        Axis[] axes = result.getAxes();
        List<Position> positions = axes[1].getPositions();
        Member mall = positions.get(0).get(0);
        String caption = mall.getHierarchy().getCaption();
        assertEquals("Position", caption);
        String captionValue = mall.getCaption();
        assertEquals("HQ Information Systems", captionValue);
        mall = positions.get(14).get(0);
        captionValue = mall.getCaption();
        assertEquals("Store Manager", captionValue);
        mall = positions.get(15).get(0);
        captionValue = mall.getCaption();
        assertEquals("Store Assistant Manager", captionValue);
    }

    /**
     * This is a test case for bug Mondrian-923. When a virtual cube included
     * calculated members in its schema, they were not included in the list of
     * existing measures because of an override of the hierarchy schema reader
     * which was done at cube init time when resolving the calculated members
     * of the base cubes.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBugMondrian923(Context<?> context) throws Exception {
        class TestBugMondrian923Modifier extends PojoMappingModifier {

            public TestBugMondrian923Modifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends CalculatedMemberMapping> cubeCalculatedMembers(CubeMapping cube) {
                List<CalculatedMemberMapping> result = new ArrayList<>();
                result.addAll(super.cubeCalculatedMembers(cube));
                if ("Warehouse and Sales".equals(cube.getName())) {
                    result.add(
                    	CalculatedMemberMappingImpl.builder()
                            .withName("Image Unit Sales")
                            //.withDimension("Measures")
                            .withFormula("[Measures].[Unit Sales]")
                    		.withCalculatedMemberProperties(List.of(
                                CalculatedMemberPropertyMappingImpl.builder()
                                    .withName("FORMAT_STRING")
                                    .withValue("|$#,###.00|image=icon_chart\\.gif|link=http://www\\.pentaho\\.com")
                                    .build()
                            ))
                        .build());
                    result.add(
                        CalculatedMemberMappingImpl.builder()
                            .withName("Arrow Unit Sales")
                            //.withDimension("Measures")
                            .withFormula("[Measures].[Unit Sales]")
                            .withCalculatedMemberProperties(List.of(
                            	CalculatedMemberPropertyMappingImpl.builder()
                                    .withName("FORMAT_STRING")
                                    .withValue("IIf([Measures].[Unit Sales] > 10000,'|#,###|arrow=up',IIf([Measures].[Unit Sales] > 5000,'|#,###|arrow=down','|#,###|arrow=none'))")
                                    .build()
                            ))
                            .build());
                    result.add(
                        CalculatedMemberMappingImpl.builder()
                            .withName("Style Unit Sales")
                            //.withDimension("Measures")
                            .withFormula("[Measures].[Unit Sales]")
                            .withCalculatedMemberProperties(List.of(
                            	CalculatedMemberPropertyMappingImpl.builder()
                                    .withName("FORMAT_STRING")
                                    .withValue("IIf([Measures].[Unit Sales] > 100000,'|#,###|style=green',IIf([Measures].[Unit Sales] > 50000,'|#,###|style=yellow','|#,###|style=red'))")
                                    .build()
                            ))
                            .build());
                }
                return result;
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                "Warehouse and Sales",
                null,
                null,
                "<CalculatedMember name=\"Image Unit Sales\" dimension=\"Measures\"><Formula>[Measures].[Unit Sales]</Formula><CalculatedMemberProperty name=\"FORMAT_STRING\" value=\"|$#,###.00|image=icon_chart\\.gif|link=http://www\\.pentaho\\.com\"/></CalculatedMember>"
                + "<CalculatedMember name=\"Arrow Unit Sales\" dimension=\"Measures\"><Formula>[Measures].[Unit Sales]</Formula><CalculatedMemberProperty name=\"FORMAT_STRING\" expression=\"IIf([Measures].[Unit Sales] > 10000,'|#,###|arrow=up',IIf([Measures].[Unit Sales] > 5000,'|#,###|arrow=down','|#,###|arrow=none'))\"/></CalculatedMember>"
                + "<CalculatedMember name=\"Style Unit Sales\" dimension=\"Measures\"><Formula>[Measures].[Unit Sales]</Formula><CalculatedMemberProperty name=\"FORMAT_STRING\" expression=\"IIf([Measures].[Unit Sales] > 100000,'|#,###|style=green',IIf([Measures].[Unit Sales] > 50000,'|#,###|style=yellow','|#,###|style=red'))\"/></CalculatedMember>",
                null));
         */
        withSchema(context, TestBugMondrian923Modifier::new);
        for (Cube cube
                : context.getConnectionWithDefaultRole().getCatalogReader().getCubes())
        {
            if (cube.getName().equals("Warehouse and Sales")) {
                for (Dimension dim : cube.getDimensions()) {
                    if (dim.isMeasures()) {
                        List<Member> members =
                            context.getConnectionWithDefaultRole()
                                .getCatalogReader().getLevelMembers(
                                    dim.getHierarchy().getLevels().getFirst(),
                                    true);
                        assertTrue(
                            members.toString().contains(
                                "[Measures].[Profit Per Unit Shipped]"));
                        assertTrue(
                            members.toString().contains(
                                "[Measures].[Image Unit Sales]"));
                        assertTrue(
                            members.toString().contains(
                                "[Measures].[Arrow Unit Sales]"));
                        assertTrue(
                            members.toString().contains(
                                "[Measures].[Style Unit Sales]"));
                        assertTrue(
                            members.toString().contains(
                                "[Measures].[Average Warehouse Sale]"));
                        return;
                    }
                }
            }
        }
        fail("Didn't find measures in sales cube.");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCubesVisibility(Context<?> context) throws Exception {
        for (Boolean testValue : new Boolean[] {true, false}) {
            class TestCubesVisibilityModifier extends PojoMappingModifier {

                public TestCubesVisibilityModifier(CatalogMapping catalogMapping) {
                    super(catalogMapping);
                }

                @Override
                protected List<? extends CubeMapping> catalogCubes(CatalogMapping schema) {
                    List<CubeMapping> result = new ArrayList<>();
                    result.add(PhysicalCubeMappingImpl.builder()
                        .withName("Foo")
                        .withVisible(testValue)
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                        .withDimensionConnectors(List.of(
                            DimensionConnectorMappingImpl.builder()
                            	.withOverrideDimensionName("Store Type")
                            	.withDimension(StandardDimensionMappingImpl.builder()
                            		.withName("Store Type")
                            		.withHierarchies(List.of(
                            			ExplicitHierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Store Type")
                                                .withColumn(FoodmartMappingSupplier.STORE_TYPE_COLUMN_IN_STORE)
                                                .withUniqueMembers(true)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build()
                        ))
                        .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                        	.withMeasures(List.of(
                            SumMeasureMappingImpl.builder()
                                .withName("Store Sqft")
                                .withColumn(FoodmartMappingSupplier.STORE_SQFT_COLUMN_IN_STORE)

                                .withFormatString("#,###")
                                .build()
                        )).build()))
                        .build());
                    result.addAll(super.catalogCubes(schema).stream().filter(c -> !"Foo".equals(c.getName())).toList());
                    return result;
                }
            }
            /*
            String cubeDef =
                "<Cube name=\"Foo\" visible=\"@REPLACE_ME@\">\n"
                + "  <Table name=\"store\"/>\n"
                + "  <Dimension name=\"Store Type\">\n"
                + "    <Hierarchy hasAll=\"true\">\n"
                + "      <Level name=\"Store Type\" column=\"store_type\" uniqueMembers=\"true\"/>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"
                + "  <Measure name=\"Store Sqft\" column=\"store_sqft\" aggregator=\"sum\"\n"
                + "      formatString=\"#,###\"/>\n"
                + "</Cube>\n";
            cubeDef = cubeDef.replace(
                "@REPLACE_ME@",
                String.valueOf(testValue));
            String baseSchema = TestUtil.getRawSchema(context);
            String schema = SchemaUtil.getSchema(baseSchema,
                    null, cubeDef, null, null, null, null);
            withSchema(context, schema);
             */
            withSchema(context, TestCubesVisibilityModifier::new);
            final Cube cube =
                context.getConnectionWithDefaultRole().getCatalog()
                    .lookupCube("Foo").orElseThrow();
            assertTrue(testValue.equals(cube.isVisible()));
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testVirtualCubesVisibility(Context<?> context) throws Exception {
        for (Boolean testValue : new Boolean[] {true, false}) {
            class TestVirtualCubesVisibilityModifier extends PojoMappingModifier {

                public TestVirtualCubesVisibilityModifier(CatalogMapping catalogMapping) {
                    super(catalogMapping);
                }
                @Override
                protected List<? extends CubeMapping> catalogCubes(CatalogMapping schema) {
                    List<CubeMapping> result = new ArrayList<>();
                    result.addAll(super.catalogCubes(schema).stream().filter(c -> !"Foo".equals(c.getName())).toList());
                    result.add(VirtualCubeMappingImpl.builder()
                        .withName("Foo")
                        .withDefaultMeasure((MemberMappingImpl) look(FoodmartMappingSupplier.MEASURE_STORE_SALES))
                        .withVisible(testValue)
                        .withDimensionConnectors(List.of(
                        	DimensionConnectorMappingImpl.builder()
                        		.withOverrideDimensionName("Customers")
                        		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_CUSTOMERS))
                        		.withPhysicalCube((PhysicalCubeMappingImpl) look(FoodmartMappingSupplier.CUBE_SALES))
                                .build()
                        ))
                        .withReferencedMeasures(List.of(
                        	look(FoodmartMappingSupplier.MEASURE_STORE_SALES)
                        ))
                        .build());
                    return result;
                }
            }
            /*
            String cubeDef =
                "<VirtualCube name=\"Foo\" defaultMeasure=\"Store Sales\" visible=\"@REPLACE_ME@\">\n"
                + "  <VirtualCubeDimension cubeName=\"Sales\" name=\"Customers\"/>\n"
                + "  <VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Store Sales]\"/>\n"
                + "</VirtualCube>\n";
            cubeDef = cubeDef.replace(
                "@REPLACE_ME@",
                String.valueOf(testValue));
            String baseSchema = TestUtil.getRawSchema(context);
            String schema = SchemaUtil.getSchema(baseSchema,
                    null, null, cubeDef, null, null, null);
            withSchema(context, schema);
             */
            ((TestContext)context).setCatalogMappingSupplier(new FoodmartMappingSupplier());
            withSchema(context, TestVirtualCubesVisibilityModifier::new);
            final Cube cube =
                context.getConnectionWithDefaultRole().getCatalog()
                    .lookupCube("Foo").orElseThrow();
            assertTrue(testValue.equals(cube.isVisible()));
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionVisibility(Context<?> context) throws Exception {
        for (Boolean testValue : new Boolean[] {true, false}) {
            class TestDimensionVisibilityModifier extends PojoMappingModifier {

                public TestDimensionVisibilityModifier(CatalogMapping catalogMapping) {
                    super(catalogMapping);
                }
                @Override
                protected List<? extends CubeMapping> catalogCubes(CatalogMapping schema) {
                    List<CubeMapping> result = new ArrayList<>();
                    result.add(PhysicalCubeMappingImpl.builder()
                        .withName("Foo")
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                        .withDimensionConnectors(List.of(
                            DimensionConnectorMappingImpl.builder()
                            	.withOverrideDimensionName("Bar")
                                .withVisible(testValue)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Bar")
                                	.withHierarchies(List.of(
                                    ExplicitHierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Store Type")
                                                .withColumn(FoodmartMappingSupplier.STORE_TYPE_COLUMN_IN_STORE)
                                                .withUniqueMembers(true)
                                                .build()
                                        ))
                                        .build()
                                ))
                                .build()).build()
                        ))
                        .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder()
                        	.withMeasures(List.of(
                                SumMeasureMappingImpl.builder()
                                    .withName("Store Sqft")
                                    .withColumn(FoodmartMappingSupplier.STORE_SQFT_COLUMN_IN_STORE)

                                    .withFormatString("#,###")
                                    .build()
                            ))
                        	.build()
                        ))
                        .build());
                    result.addAll(super.catalogCubes(schema).stream().filter(c -> !"Foo".equals(c.getName())).toList());
                    return result;
                }
            }
            /*
            String cubeDef =
                "<Cube name=\"Foo\">\n"
                + "  <Table name=\"store\"/>\n"
                + "  <Dimension name=\"Bar\" visible=\"@REPLACE_ME@\">\n"
                + "    <Hierarchy hasAll=\"true\">\n"
                + "      <Level name=\"Store Type\" column=\"store_type\" uniqueMembers=\"true\"/>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"
                + "  <Measure name=\"Store Sqft\" column=\"store_sqft\" aggregator=\"sum\"\n"
                + "      formatString=\"#,###\"/>\n"
                + "</Cube>\n";
            cubeDef = cubeDef.replace(
                "@REPLACE_ME@",
                String.valueOf(testValue));
            String baseSchema = TestUtil.getRawSchema(context);
            String schema = SchemaUtil.getSchema(baseSchema,
                    null, cubeDef, null, null, null, null);
            withSchema(context, schema);
             */
            withSchema(context, TestDimensionVisibilityModifier::new);
            final Cube cube =
                context.getConnectionWithDefaultRole().getCatalog()
                    .lookupCube("Foo").orElseThrow();
            Dimension dim = null;
            for (Dimension dimCheck : cube.getDimensions()) {
                if (dimCheck.getName().equals("Bar")) {
                    dim = dimCheck;
                }
            }
            assertNotNull(dim);
            assertTrue(testValue.equals(dim.isVisible()));
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testVirtualDimensionVisibility(Context<?> context) throws Exception {
        for (Boolean testValue : new Boolean[] {true, false}) {
            class TestVirtualDimensionVisibilityModifier extends PojoMappingModifier {

                public TestVirtualDimensionVisibilityModifier(CatalogMapping catalogMapping) {
                    super(catalogMapping);
                }
                @Override
                protected List<CubeMapping> catalogCubes(CatalogMapping schema) {
                    List<CubeMapping> result = new ArrayList<>();
                    result.addAll(super.catalogCubes(schema).stream().filter(c -> !"Foo".equals(c.getName())).toList());
                    result.add(VirtualCubeMappingImpl.builder()
                        .withName("Foo")
                        .withDefaultMeasure((MemberMappingImpl) look(FoodmartMappingSupplier.MEASURE_STORE_SALES))
                        .withDimensionConnectors(List.of(
                        	DimensionConnectorMappingImpl.builder()
                        		.withPhysicalCube((PhysicalCubeMappingImpl) look(FoodmartMappingSupplier.CUBE_SALES))
                        		.withOverrideDimensionName("Customers")
                                .withVisible(testValue)
                                .build()
                        ))
                        .withReferencedMeasures(List.of(
                        		look(FoodmartMappingSupplier.MEASURE_STORE_SALES)
                        ))
                        .build());
                    return result;
                }
            }
            /*
            String cubeDef =
                "<VirtualCube name=\"Foo\" defaultMeasure=\"Store Sales\">\n"
                + "  <VirtualCubeDimension cubeName=\"Sales\" name=\"Customers\" visible=\"@REPLACE_ME@\"/>\n"
                + "  <VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Store Sales]\"/>\n"
                + "</VirtualCube>\n";
            cubeDef = cubeDef.replace(
                "@REPLACE_ME@",
                String.valueOf(testValue));
            String baseSchema = TestUtil.getRawSchema(context);
            String schema = SchemaUtil.getSchema(baseSchema,
                    null, null, cubeDef, null, null, null);
            withSchema(context, schema);
             */
            ((TestContext)context).setCatalogMappingSupplier(new FoodmartMappingSupplier());
            withSchema(context, TestVirtualDimensionVisibilityModifier::new);
            final Cube cube =
                context.getConnectionWithDefaultRole().getCatalog()
                    .lookupCube("Foo").orElseThrow();
            Dimension dim = null;
            for (Dimension dimCheck : cube.getDimensions()) {
                if (dimCheck.getName().equals("Customers")) {
                    dim = dimCheck;
                }
            }
            assertNotNull(dim);
            assertTrue(testValue.equals(dim.isVisible()));
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionUsageVisibility(Context<?> context) throws Exception {
        for (Boolean testValue : new Boolean[] {true, false}) {
            class TestDimensionUsageVisibilityModifier extends PojoMappingModifier {
                private Boolean value;
                public TestDimensionUsageVisibilityModifier(CatalogMapping catalogMapping, Boolean value) {
                    super(catalogMapping);
                    this.value = value;
                }

                @Override
                protected List<CubeMapping> catalogCubes(CatalogMapping schema) {
                    List<CubeMapping> result = new ArrayList<>();
                    result.add(PhysicalCubeMappingImpl.builder()
                        .withName("Foo")
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                        .withDimensionConnectors(List.of(
                            DimensionConnectorMappingImpl.builder()
                            	.withOverrideDimensionName("Bacon")
                            	.withDimension(StandardDimensionMappingImpl.builder()
                            		.withName("Bacon")
                            		.withHierarchies(List.of(
                                    ExplicitHierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Store Type")
                                                .withColumn(FoodmartMappingSupplier.STORE_TYPE_COLUMN_IN_STORE)
                                                .withUniqueMembers(true)
                                                .build()
                                        ))
                                        .build()
                            		))
                            	.build())
                            .build(),
                            DimensionConnectorMappingImpl.builder()
                            	.withOverrideDimensionName("Bar")
                                .withDimension(FoodmartMappingSupplier.DIMENSION_TIME)
                                .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY)
                                .withVisible(this.value)
                                .build()
                        ))
                        .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder()
                        	.withMeasures(List.of(
                                    SumMeasureMappingImpl.builder()
                                    .withName("Store Sqft")
                                    .withColumn(FoodmartMappingSupplier.STORE_SQFT_COLUMN_IN_STORE)

                                    .withFormatString("#,###")
                                    .build()
                        	))
                        	.build()
                        ))
                        .build());
                    result.addAll(super.catalogCubes(schema).stream().filter(c -> !"Foo".equals(c.getName())).toList());
                    return result;
                }
            }
            /*
            String cubeDef =
                "<Cube name=\"Foo\">\n"
                + "  <Table name=\"store\"/>\n"
                + "  <Dimension name=\"Bacon\">\n"
                + "    <Hierarchy hasAll=\"true\">\n"
                + "      <Level name=\"Store Type\" column=\"store_type\" uniqueMembers=\"true\"/>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"
                + "  <Measure name=\"Store Sqft\" column=\"store_sqft\" aggregator=\"sum\"\n"
                + "      formatString=\"#,###\"/>\n"
                + "</Cube>\n";
            String baseSchema = TestUtil.getRawSchema(context);
            String schema = SchemaUtil.getSchema(baseSchema,
                    null, cubeDef, null, null, null, null);
            withSchema(context, schema);
             */
            context.getCatalogCache().clear();
            CatalogMapping catalogMapping = ((RolapContext) context).getCatalogMapping();
            TestDimensionUsageVisibilityModifier testDimensionUsageVisibilityModifier =
            		new TestDimensionUsageVisibilityModifier(catalogMapping, testValue);
            ((TestContext)context).setCatalogMappingSupplier(testDimensionUsageVisibilityModifier);

            final Cube cube =
                context.getConnectionWithDefaultRole().getCatalog()
                    .lookupCube("Foo").orElseThrow();

            Dimension dim = null;
            for (Dimension dimCheck : cube.getDimensions()) {
                if (dimCheck.getName().equals("Bar")) {
                    dim = dimCheck;
                }
            }
            assertNotNull(dim);
            assertTrue(testValue.equals(dim.isVisible()));
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHierarchyVisibility(Context<?> context) throws Exception {
        for (Boolean testValue : new Boolean[] {true, false}) {
        	context.getCatalogCache().clear();
            class TestHierarchyVisibilityModifier extends PojoMappingModifier {

                public TestHierarchyVisibilityModifier(CatalogMapping catalogMapping) {
                    super(catalogMapping);
                }

                @Override
                protected List<CubeMapping> catalogCubes(CatalogMapping schema) {
                    List<CubeMapping> result = new ArrayList<>();
                    result.add(PhysicalCubeMappingImpl.builder()
                        .withName("Foo")
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                        .withDimensionConnectors(List.of(
                        	DimensionConnectorMappingImpl.builder()
                        		.withOverrideDimensionName("Bar")
                        		.withDimension(StandardDimensionMappingImpl.builder()
                        			.withName("Bar")
                        			.withHierarchies(List.of(
                                       ExplicitHierarchyMappingImpl.builder()
                                        .withName("Bacon")
                                        .withHasAll(true)
                                        .withVisible(testValue)
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Store Type")
                                                .withColumn(FoodmartMappingSupplier.STORE_TYPE_COLUMN_IN_STORE)
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
                            		.withName("Store Sqft")
                            		.withColumn(FoodmartMappingSupplier.STORE_SQFT_COLUMN_IN_STORE)

                            		.withFormatString("#,###")
                            		.build()
                            	))
                            	.build()
                        ))
                        .build());
                    result.addAll(super.catalogCubes(schema).stream().filter(c -> !"Foo".equals(c.getName())).toList());
                    return result;
                }
            }
            /*
            String cubeDef =
                "<Cube name=\"Foo\">\n"
                + "  <Table name=\"store\"/>\n"
                + "  <Dimension name=\"Bar\">\n"
                + "    <Hierarchy name=\"Bacon\" hasAll=\"true\" visible=\"@REPLACE_ME@\">\n"
                + "      <Level name=\"Store Type\" column=\"store_type\" uniqueMembers=\"true\"/>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"
                + "  <Measure name=\"Store Sqft\" column=\"store_sqft\" aggregator=\"sum\"\n"
                + "      formatString=\"#,###\"/>\n"
                + "</Cube>\n";
            cubeDef = cubeDef.replace(
                "@REPLACE_ME@",
                String.valueOf(testValue));
            String baseSchema = TestUtil.getRawSchema(context);
            String schema = SchemaUtil.getSchema(baseSchema,
                    null, cubeDef, null, null, null, null);
            withSchema(context, schema);
             */
            withSchema(context, TestHierarchyVisibilityModifier::new);
            final Cube cube =
                context.getConnectionWithDefaultRole().getCatalog()
                    .lookupCube("Foo").orElseThrow();
            Dimension dim = null;
            for (Dimension dimCheck : cube.getDimensions()) {
                if (dimCheck.getName().equals("Bar")) {
                    dim = dimCheck;
                }
            }
            assertNotNull(dim);
            final Hierarchy hier = dim.getHierarchy();
            assertNotNull(hier);
            assertEquals("Bacon",
                hier.getName());
            assertTrue(testValue.equals(hier.isVisible()));
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLevelVisibility(Context<?> context) throws Exception {
        for (Boolean testValue : new Boolean[] {true, false}) {
            class TestLevelVisibilityModifier extends PojoMappingModifier {

                public TestLevelVisibilityModifier(CatalogMapping catalogMapping) {
                    super(catalogMapping);
                }

                @Override
                protected List<CubeMapping> catalogCubes(CatalogMapping schema) {
                    List<CubeMapping> result = new ArrayList<>();
                    result.add(PhysicalCubeMappingImpl.builder()
                        .withName("Foo")
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                        .withDimensionConnectors(List.of(
                        	DimensionConnectorMappingImpl.builder()
                        		.withOverrideDimensionName("Bar")
                        		.withDimension(StandardDimensionMappingImpl.builder()
                                .withHierarchies(List.of(
                                    ExplicitHierarchyMappingImpl.builder()
                                        .withName("Bacon")
                                        .withHasAll(false)
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Samosa")
                                                .withColumn(FoodmartMappingSupplier.STORE_TYPE_COLUMN_IN_STORE)
                                                .withUniqueMembers(true)
                                                .withVisible(testValue)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build()
                        ))
                        .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                        		.withMeasures(List.of(
                                    SumMeasureMappingImpl.builder()
                                        .withName("Store Sqft")
                                        .withColumn(FoodmartMappingSupplier.STORE_SQFT_COLUMN_IN_STORE)

                                        .withFormatString("#,###")
                                        .build()
                                )).build()))
                        .build());
                    result.addAll(super.catalogCubes(schema).stream().filter(c -> !"Foo".equals(c.getName())).toList());
                    return result;
                }
            }
            /*
            String cubeDef =
                "<Cube name=\"Foo\">\n"
                + "  <Table name=\"store\"/>\n"
                + "  <Dimension name=\"Bar\">\n"
                + "    <Hierarchy name=\"Bacon\" hasAll=\"false\">\n"
                + "      <Level name=\"Samosa\" column=\"store_type\" uniqueMembers=\"true\" visible=\"@REPLACE_ME@\"/>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"
                + "  <Measure name=\"Store Sqft\" column=\"store_sqft\" aggregator=\"sum\"\n"
                + "      formatString=\"#,###\"/>\n"
                + "</Cube>\n";
            cubeDef = cubeDef.replace(
                "@REPLACE_ME@",
                String.valueOf(testValue));
            String baseSchema = TestUtil.getRawSchema(context);
            String schema = SchemaUtil.getSchema(baseSchema,
                    null, cubeDef, null, null, null, null);
            withSchema(context, schema);
             */
            withSchema(context, TestLevelVisibilityModifier::new);
            final Cube cube =
                context.getConnectionWithDefaultRole().getCatalog()
                    .lookupCube("Foo").orElseThrow();
            Dimension dim = null;
            for (Dimension dimCheck : cube.getDimensions()) {
                if (dimCheck.getName().equals("Bar")) {
                    dim = dimCheck;
                }
            }
            assertNotNull(dim);
            final Hierarchy hier = dim.getHierarchy();
            assertNotNull(hier);
            assertEquals("Bacon",
                hier.getName());
            final Level level = hier.getLevels().getFirst();
            assertEquals("Samosa", level.getName());
            assertTrue(testValue.equals(level.isVisible()));
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonCollapsedAggregate(Context<?> context) throws Exception {
        if (context.getConfigValue(ConfigConstants.USE_AGGREGATES, ConfigConstants.USE_AGGREGATES_DEFAULT_VALUE ,Boolean.class) == false
            && context.getConfigValue(ConfigConstants.READ_AGGREGATES, ConfigConstants.READ_AGGREGATES_DEFAULT_VALUE ,Boolean.class) == false)
        {
            return;
        }
        context.getCatalogCache().clear();
        class TestNonCollapsedAggregateModifier extends PojoMappingModifier {

            public TestNonCollapsedAggregateModifier(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected List<? extends CubeMapping> catalogCubes(CatalogMapping schema) {
                List<CubeMapping> result = new ArrayList<>();
            	TableQueryMappingImpl t = TableQueryMappingImpl.builder()
            			.withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
            			.withAggregationExcludes(
            				List.of(
            					AggregationExcludeMappingImpl.builder().withName("agg_g_ms_pcat_sales_fact_1997").build(),
            					AggregationExcludeMappingImpl.builder().withName("agg_c_14_sales_fact_1997").build(),
            					AggregationExcludeMappingImpl.builder().withName("agg_pl_01_sales_fact_1997").build(),
            					AggregationExcludeMappingImpl.builder().withName("agg_ll_01_sales_fact_1997").build()
                            )
            			)
            			.withAggregationTables(List.of(
            				AggregationNameMappingImpl.builder()
                                .withName(FoodmartMappingSupplier.AGG_L_05_SALES_FACT_1997)
                                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.FACT_COUNT_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build())
                                .withAggregationIgnoreColumns(List.of(
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build(),
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build(),
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build(),
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build(),
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build()
                                ))
                                .withAggregationMeasures(List.of(
                                		AggregationMeasureMappingImpl.builder().withName("[Measures].[Unit Sales]").withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build()
                                ))
                                .withAggregationLevels(List.of(
                                	AggregationLevelMappingImpl.builder()
                                        .withName("[Product].[Product Id]")
                                        .withColumn(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                        .build()
                                ))
                                .build()
            			))
            			.build();

                LevelMappingImpl l1 = LevelMappingImpl.builder()
                    .withName("Product Family")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_FAMILY_COLUMN_IN_PRODUCT_CLASS)
                    .withUniqueMembers(true)
                    .build();
                LevelMappingImpl l2 = LevelMappingImpl.builder()
                    .withName("Product Department")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_DEPARTMENT_COLUMN_IN_PRODUCT_CLASS)
                    .withUniqueMembers(false)
                    .build();
                LevelMappingImpl l3 = LevelMappingImpl.builder()
                    .withName("Product Category")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_CATEGORY_COLUMN_IN_PRODUCT_CLASS)
                    .withUniqueMembers(false)
                    .build();

                LevelMappingImpl l4 = LevelMappingImpl.builder()
                    .withName("Product Subcategory")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_SUBCATEGORY_COLUMN_IN_PRODUCT_CLASS)
                    .withUniqueMembers(false)
                    .build();
                LevelMappingImpl l5 = LevelMappingImpl.builder()
                    .withName("Brand Name")
                    .withColumn(FoodmartMappingSupplier.BRAND_NAME_COLUMN_IN_PRODUCT)
                    .withUniqueMembers(false)
                    .build();
                LevelMappingImpl l6 = LevelMappingImpl.builder()
                    .withName("Product Name")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_NAME_COLUMN_IN_PRODUCT)
                    .withUniqueMembers(true)
                    .build();

                LevelMappingImpl l7 = LevelMappingImpl.builder()
                    .withName("Product Id")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                    .withUniqueMembers(true)
                    .build();

                SumMeasureMappingImpl m = SumMeasureMappingImpl.builder()
                .withName("Unit Sales")
                .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                .withFormatString("Standard")
                .build();

                result.add(PhysicalCubeMappingImpl.builder()
                    .withName("Foo")
                    .withDefaultMeasure(m)
                    .withQuery(t)
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                            .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                            .withOverrideDimensionName("Product")
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("Product")
                            	.withHierarchies(List.of(
                                ExplicitHierarchyMappingImpl.builder()
                                    .withHasAll(true)
                                    .withPrimaryKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                                    .withQuery(JoinQueryMappingImpl.builder()
                                    		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT)
                                    				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_TABLE).build())
                                    				.build())
                                    		.withRight(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT_CLASS)
                                    				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE).build())
                                    				.build()).build())
                                    .withLevels(List.of(
                                        l1, l2, l3, l4, l5, l6, l7
                                    ))
                                    .build()
                            ))
                            .build()).build()
                    ))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                    		.withMeasures(List.of(m))
                    		.build()))
                    .build());
                result.addAll(super.catalogCubes(schema).stream().filter(c -> !"Foo".equals(c.getName())).toList());
                return result;
            }
        }
        /*
        final String cube =
            "<Cube name=\"Foo\" defaultMeasure=\"Unit Sales\">\n"
            + "  <Table name=\"sales_fact_1997\">\n"
            + "    <AggExclude name=\"agg_g_ms_pcat_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_c_14_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_pl_01_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_ll_01_sales_fact_1997\"/>"
            + "    <AggName name=\"agg_l_05_sales_fact_1997\">"
            + "        <AggFactCount column=\"fact_count\"/>\n"
            + "        <AggIgnoreColumn column=\"customer_id\"/>\n"
            + "        <AggIgnoreColumn column=\"store_id\"/>\n"
            + "        <AggIgnoreColumn column=\"promotion_id\"/>\n"
            + "        <AggIgnoreColumn column=\"store_sales\"/>\n"
            + "        <AggIgnoreColumn column=\"store_cost\"/>\n"
            + "        <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"unit_sales\" />\n"
            + "        <AggLevel name=\"[Product].[Product Id]\" column=\"product_id\" collapsed=\"false\"/>\n"
            + "    </AggName>\n"
            + "</Table>\n"
            + "<Dimension foreignKey=\"product_id\" name=\"Product\">\n"
            + "<Hierarchy hasAll=\"true\" primaryKey=\"product_id\" primaryKeyTable=\"product\">\n"
            + "  <Join leftKey=\"product_class_id\" rightKey=\"product_class_id\">\n"
            + " <Table name=\"product\"/>\n"
            + " <Table name=\"product_class\"/>\n"
            + "  </Join>\n"
            + "  <Level name=\"Product Family\" table=\"product_class\" column=\"product_family\"\n"
            + "   uniqueMembers=\"true\"/>\n"
            + "  <Level name=\"Product Department\" table=\"product_class\" column=\"product_department\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Category\" table=\"product_class\" column=\"product_category\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Subcategory\" table=\"product_class\" column=\"product_subcategory\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Brand Name\" table=\"product\" column=\"brand_name\" uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Name\" table=\"product\" column=\"product_name\"\n"
            + "   uniqueMembers=\"true\"/>\n"
            + "  <Level name=\"Product Id\" table=\"product\" column=\"product_id\"\n"
            + "   uniqueMembers=\"true\"/>\n"
            + "</Hierarchy>\n"
            + "</Dimension>\n"
            + "<Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "</Cube>\n";
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
                null, cube, null, null, null, null);
        withSchema(context, schema);
         */
        withSchema(context, TestNonCollapsedAggregateModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Product].[Product Family].Members} on rows, {[Measures].[Unit Sales]} on columns from [Foo]",
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

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonCollapsedAggregateOnNonUniqueLevelFails(Context<?> context)
        throws Exception
    {
        if (context.getConfigValue(ConfigConstants.USE_AGGREGATES, ConfigConstants.USE_AGGREGATES_DEFAULT_VALUE ,Boolean.class) == false
            && context.getConfigValue(ConfigConstants.READ_AGGREGATES, ConfigConstants.READ_AGGREGATES_DEFAULT_VALUE ,Boolean.class) == false)
        {
            return;
        }
        class TestNonCollapsedAggregateOnNonUniqueLevelFailsModifier extends PojoMappingModifier {

            public TestNonCollapsedAggregateOnNonUniqueLevelFailsModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> catalogCubes(CatalogMapping schema) {
                List<CubeMapping> result = new ArrayList<>();

            	TableQueryMappingImpl t = TableQueryMappingImpl.builder()
            			.withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
            			.withAggregationExcludes(
            				List.of(
            					AggregationExcludeMappingImpl.builder().withName("agg_g_ms_pcat_sales_fact_1997").build(),
            					AggregationExcludeMappingImpl.builder().withName("agg_c_14_sales_fact_1997").build(),
            					AggregationExcludeMappingImpl.builder().withName("agg_pl_01_sales_fact_1997").build(),
            					AggregationExcludeMappingImpl.builder().withName("agg_ll_01_sales_fact_1997").build()
                            )
            			)
            			.withAggregationTables(List.of(
            				AggregationNameMappingImpl.builder()
                                .withName(FoodmartMappingSupplier.AGG_L_05_SALES_FACT_1997)
                                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.FACT_COUNT_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build())
                                .withAggregationIgnoreColumns(List.of(
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build(),
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build(),
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build(),
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build(),
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build()
                                ))
                                .withAggregationMeasures(List.of(
                                		AggregationMeasureMappingImpl.builder().withName("[Measures].[Unit Sales]").withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build()
                                ))
                                .withAggregationLevels(List.of(
                                	AggregationLevelMappingImpl.builder()
                                        .withName("[Product].[Product Id]")
                                        .withColumn(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                        .build()
                                ))
                                .build()
            			))
            			.build();

                LevelMappingImpl l1 = LevelMappingImpl.builder()
                    .withName("Product Family")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_FAMILY_COLUMN_IN_PRODUCT_CLASS)
                    .withUniqueMembers(true)
                    .build();
                LevelMappingImpl l2 = LevelMappingImpl.builder()
                    .withName("Product Department")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_DEPARTMENT_COLUMN_IN_PRODUCT_CLASS)
                    .withUniqueMembers(false)
                    .build();
                LevelMappingImpl l3 = LevelMappingImpl.builder()
                    .withName("Product Category")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_CATEGORY_COLUMN_IN_PRODUCT_CLASS)
                    .withUniqueMembers(false)
                    .build();
                LevelMappingImpl l4 = LevelMappingImpl.builder()
                    .withName("Product Subcategory")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_SUBCATEGORY_COLUMN_IN_PRODUCT_CLASS)
                    .withUniqueMembers(false)
                    .build();
                LevelMappingImpl l5 = LevelMappingImpl.builder()
                    .withName("Brand Name")
                    .withColumn(FoodmartMappingSupplier.BRAND_NAME_COLUMN_IN_PRODUCT)
                    .withUniqueMembers(false)
                    .build();
                LevelMappingImpl l6 = LevelMappingImpl.builder()
                    .withName("Product Name")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_NAME_COLUMN_IN_PRODUCT)
                    .withUniqueMembers(true)
                    .build();

                LevelMappingImpl l7 = LevelMappingImpl.builder()
                    .withName("Product Id")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                    .withUniqueMembers(true)
                    .build();

                SumMeasureMappingImpl m = SumMeasureMappingImpl.builder()
                .withName("Unit Sales")
                .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                .withFormatString("Standard")
                .build();

                result.add(PhysicalCubeMappingImpl.builder()
                    .withName("Foo")
                    .withDefaultMeasure(m)
                    .withQuery(t)
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                            .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                            .withOverrideDimensionName("Product")
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("Product")
                            	.withHierarchies(List.of(
                                   ExplicitHierarchyMappingImpl.builder()
                                    .withHasAll(true)
                                    .withPrimaryKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                                    .withQuery(JoinQueryMappingImpl.builder()
                                    	.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT)
                            				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_TABLE).build())
                            				.build())
                                    	.withRight(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT_CLASS)
                            				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE).build())
                            				.build()).build())
                                    .withLevels(List.of(
                                        l1, l2, l3, l4, l5, l6, l7
                                    ))
                                    .build()
                            ))
                            .build()
                        )
                        .build()
                    ))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                    		.withMeasures(List.of(m)).build()))
                    .build());
                result.addAll(super.catalogCubes(schema).stream().filter(c -> !"Foo".equals(c.getName())).toList());
                return result;
            }
        }
        /*
        final String cube =
            "<Cube name=\"Foo\" defaultMeasure=\"Unit Sales\">\n"
            + "  <Table name=\"sales_fact_1997\">\n"
            + "    <AggExclude name=\"agg_g_ms_pcat_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_c_14_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_pl_01_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_ll_01_sales_fact_1997\"/>"
            + "    <AggName name=\"agg_l_05_sales_fact_1997\">"
            + "        <AggFactCount column=\"fact_count\"/>\n"
            + "        <AggIgnoreColumn column=\"customer_id\"/>\n"
            + "        <AggIgnoreColumn column=\"store_id\"/>\n"
            + "        <AggIgnoreColumn column=\"promotion_id\"/>\n"
            + "        <AggIgnoreColumn column=\"store_sales\"/>\n"
            + "        <AggIgnoreColumn column=\"store_cost\"/>\n"
            + "        <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"unit_sales\" />\n"
            + "        <AggLevel name=\"[Product].[Product Name]\" column=\"product_id\" collapsed=\"false\"/>\n"
            + "    </AggName>\n"
            + "</Table>\n"
            + "<Dimension foreignKey=\"product_id\" name=\"Product\">\n"
            + "<Hierarchy hasAll=\"true\" primaryKey=\"product_id\" primaryKeyTable=\"product\">\n"
            + "  <Join leftKey=\"product_class_id\" rightKey=\"product_class_id\">\n"
            + " <Table name=\"product\"/>\n"
            + " <Table name=\"product_class\"/>\n"
            + "  </Join>\n"
            + "  <Level name=\"Product Family\" table=\"product_class\" column=\"product_family\"\n"
            + "   uniqueMembers=\"true\"/>\n"
            + "  <Level name=\"Product Department\" table=\"product_class\" column=\"product_department\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Category\" table=\"product_class\" column=\"product_category\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Subcategory\" table=\"product_class\" column=\"product_subcategory\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Brand Name\" table=\"product\" column=\"brand_name\" uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Name\" table=\"product\" column=\"product_name\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Id\" table=\"product\" column=\"product_id\"\n"
            + "   uniqueMembers=\"true\"/>\n"
            + "</Hierarchy>\n"
            + "</Dimension>\n"
            + "<Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "</Cube>\n";
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
                null, cube, null, null, null, null);
        withSchema(context, schema);
         */
        withSchema(context, TestNonCollapsedAggregateOnNonUniqueLevelFailsModifier::new);
        assertQueryThrows(context,
            "select {[Product].[Product Family].Members} on rows, {[Measures].[Unit Sales]} on columns from [Foo]",
            "mondrian.olap.MondrianException: Mondrian Error:Too many errors, '1', while loading/reloading aggregates.");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTwoNonCollapsedAggregate(Context<?> context) throws Exception {
        if (context.getConfigValue(ConfigConstants.USE_AGGREGATES, ConfigConstants.USE_AGGREGATES_DEFAULT_VALUE ,Boolean.class) == false
            && context.getConfigValue(ConfigConstants.READ_AGGREGATES, ConfigConstants.READ_AGGREGATES_DEFAULT_VALUE ,Boolean.class) == false)
        {
            return;
        }
        class TestTwoNonCollapsedAggregateModifier extends PojoMappingModifier {

            public TestTwoNonCollapsedAggregateModifier(CatalogMapping catalog) {
                super(catalog);
            }
            SumMeasureMappingImpl m = SumMeasureMappingImpl.builder()
            .withName("Unit Sales")
            .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

            .withFormatString("Standard")
            .build();

            @Override
            protected List<CubeMapping> catalogCubes(CatalogMapping schema) {
                List<CubeMapping> result = new ArrayList<>();
                PhysicalColumnMappingImpl region_id = PhysicalColumnMappingImpl.builder().withName("region_id").withDataType(ColumnDataType.INTEGER).build();
                PhysicalColumnMappingImpl sales_region = PhysicalColumnMappingImpl.builder().withName("sales_region").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(30).build();
                PhysicalColumnMappingImpl sales_city = PhysicalColumnMappingImpl.builder().withName("sales_city").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(30).build();
                PhysicalColumnMappingImpl sales_district_id = PhysicalColumnMappingImpl.builder().withName("sales_district_id").withDataType(ColumnDataType.INTEGER).build();
                PhysicalTableMappingImpl region = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("region")
                        .withColumns(List.of(
                                region_id, sales_region, sales_district_id
                                ))).build();

            	TableQueryMappingImpl t = TableQueryMappingImpl.builder()
            			.withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
            			.withAggregationExcludes(
            				List.of(
            					AggregationExcludeMappingImpl.builder().withName("agg_g_ms_pcat_sales_fact_1997").build(),
            					AggregationExcludeMappingImpl.builder().withName("agg_c_14_sales_fact_1997").build(),
            					AggregationExcludeMappingImpl.builder().withName("agg_pl_01_sales_fact_1997").build(),
            					AggregationExcludeMappingImpl.builder().withName("agg_ll_01_sales_fact_1997").build()
                            )
            			)
            			.withAggregationTables(List.of(
            				AggregationNameMappingImpl.builder()
                                .withName(FoodmartMappingSupplier.AGG_L_05_SALES_FACT_1997)
                                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.FACT_COUNT_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build())
                                .withAggregationIgnoreColumns(List.of(
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build(),
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build(),
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build(),
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build()
                                ))
                                .withAggregationMeasures(List.of(
                                		AggregationMeasureMappingImpl.builder().withName("[Measures].[Unit Sales]").withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build()
                                ))
                                .withAggregationLevels(List.of(
                                	AggregationLevelMappingImpl.builder()
                                        .withName("[Product].[Product Id]")
                                        .withColumn(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                        .build(),
                                    AggregationLevelMappingImpl.builder()
                                        .withName("[Store].[Store Id]")
                                        .withColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                        .withCollapsed(false)
                                        .build()

                                ))
                                .build()
            			))
            			.build();

                LevelMappingImpl l1 = LevelMappingImpl.builder()
                    .withName("Product Family")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_FAMILY_COLUMN_IN_PRODUCT_CLASS)
                    .withUniqueMembers(true)
                    .build();
                LevelMappingImpl l2 = LevelMappingImpl.builder()
                    .withName("Product Department")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_DEPARTMENT_COLUMN_IN_PRODUCT_CLASS)
                    .withUniqueMembers(false)
                    .build();
                LevelMappingImpl l3 = LevelMappingImpl.builder()
                    .withName("Product Category")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_CATEGORY_COLUMN_IN_PRODUCT_CLASS)
                    .withUniqueMembers(false)
                    .build();
                LevelMappingImpl l4 = LevelMappingImpl.builder()
                    .withName("Product Subcategory")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_SUBCATEGORY_COLUMN_IN_PRODUCT_CLASS)
                    .withUniqueMembers(false)
                    .build();
                LevelMappingImpl l5 = LevelMappingImpl.builder()
                    .withName("Brand Name")
                    .withColumn(FoodmartMappingSupplier.BRAND_NAME_COLUMN_IN_PRODUCT)
                    .withUniqueMembers(false)
                    .build();
                LevelMappingImpl l6 = LevelMappingImpl.builder()
                    .withName("Product Name")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_NAME_COLUMN_IN_PRODUCT)
                    .withUniqueMembers(true)
                    .build();

                LevelMappingImpl l7 = LevelMappingImpl.builder()
                    .withName("Product Id")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                    .withUniqueMembers(true)
                    .build();

                result.add(PhysicalCubeMappingImpl.builder()
                    .withName("Foo")
                    .withDefaultMeasure(m)
                    .withQuery(t)
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                            .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                            .withOverrideDimensionName("Product")
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("Product")
                            	.withHierarchies(List.of(
                                ExplicitHierarchyMappingImpl.builder()
                                    .withHasAll(true)
                                    .withPrimaryKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                                    .withQuery(
                                    	JoinQueryMappingImpl.builder()
                                        	.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT)
                                				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_TABLE).build())
                                				.build())
                                        	.withRight(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT_CLASS)
                                				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE).build())
                                				.build()).build()
                                    )
                                    .withLevels(List.of(
                                        l1, l2, l3, l4, l5, l6, l7
                                    ))
                                    .build()
                            )).build())
                            .build(),
                        DimensionConnectorMappingImpl.builder()
                        	.withOverrideDimensionName("Store")
                            .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("Store")
                            	.withHierarchies(List.of(
                                ExplicitHierarchyMappingImpl.builder()
                                    .withHasAll(true)
                                    .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                                    .withQuery(
                                        	JoinQueryMappingImpl.builder()
                                            	.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.REGION_ID_COLUMN_IN_STORE)
                                    				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                                    				.build())
                                            	.withRight(JoinedQueryElementMappingImpl.builder().withKey(region_id)
                                    				.withQuery(TableQueryMappingImpl.builder().withTable(region).build())
                                    				.build()).build()
                                    )
                                    .withLevels(List.of(
                                        LevelMappingImpl.builder()
                                            .withName("Store Region")
                                            .withColumn(sales_city)
                                            .withUniqueMembers(false)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("Store Id")
                                            .withColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                                            .withUniqueMembers(true)
                                            .build()
                                    ))
                                    .build()
                            ))
                            .build()
                    ).build()))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                    		.withMeasures(List.of(m))
                    		.build()))
                    .build());
                result.addAll(super.catalogCubes(schema).stream().filter(c -> !"Foo".equals(c.getName())).toList());
                return result;
            }
        }
        /*
        final String cube =
            "<Cube name=\"Foo\" defaultMeasure=\"Unit Sales\">\n"
            + "  <Table name=\"sales_fact_1997\">\n"
            + "    <AggExclude name=\"agg_g_ms_pcat_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_c_14_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_pl_01_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_ll_01_sales_fact_1997\"/>"
            + "    <AggName name=\"agg_l_05_sales_fact_1997\">"
            + "        <AggFactCount column=\"fact_count\"/>\n"
            + "        <AggIgnoreColumn column=\"customer_id\"/>\n"
            + "        <AggIgnoreColumn column=\"promotion_id\"/>\n"
            + "        <AggIgnoreColumn column=\"store_sales\"/>\n"
            + "        <AggIgnoreColumn column=\"store_cost\"/>\n"
            + "        <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"unit_sales\" />\n"
            + "        <AggLevel name=\"[Product].[Product Id]\" column=\"product_id\" collapsed=\"false\"/>\n"
            + "        <AggLevel name=\"[Store].[Store Id]\" column=\"store_id\" collapsed=\"false\"/>\n"
            + "    </AggName>\n"
            + "</Table>\n"
            + "<Dimension foreignKey=\"product_id\" name=\"Product\">\n"
            + "<Hierarchy hasAll=\"true\" primaryKey=\"product_id\" primaryKeyTable=\"product\">\n"
            + "  <Join leftKey=\"product_class_id\" rightKey=\"product_class_id\">\n"
            + " <Table name=\"product\"/>\n"
            + " <Table name=\"product_class\"/>\n"
            + "  </Join>\n"
            + "  <Level name=\"Product Family\" table=\"product_class\" column=\"product_family\"\n"
            + "   uniqueMembers=\"true\"/>\n"
            + "  <Level name=\"Product Department\" table=\"product_class\" column=\"product_department\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Category\" table=\"product_class\" column=\"product_category\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Subcategory\" table=\"product_class\" column=\"product_subcategory\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Brand Name\" table=\"product\" column=\"brand_name\" uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Name\" table=\"product\" column=\"product_name\"\n"
            + "   uniqueMembers=\"true\"/>\n"
            + "  <Level name=\"Product Id\" table=\"product\" column=\"product_id\"\n"
            + "   uniqueMembers=\"true\"/>\n"
            + "</Hierarchy>\n"
            + "</Dimension>\n"
            + "  <Dimension name=\"Store\" foreignKey=\"store_id\" >\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\"\n"
            + "        primaryKeyTable=\"store\">\n"
            + "      <Join leftKey=\"region_id\" rightKey=\"region_id\">\n"
            + "        <Table name=\"store\"/>\n"
            + "        <Table name=\"region\"/>\n"
            + "      </Join>\n"
            + "      <Level name=\"Store Region\" table=\"region\" column=\"sales_city\"\n"
            + "          uniqueMembers=\"false\"/>\n"
            + "      <Level name=\"Store Id\" table=\"store\" column=\"store_id\"\n"
            + "          uniqueMembers=\"true\">\n"
            + "      </Level>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "<Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "</Cube>\n";
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
                null, cube, null, null, null, null);
        withSchema(context, schema);
         */
        withSchema(context, TestTwoNonCollapsedAggregateModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {Crossjoin([Product].[Product Family].Members, [Store].[Store Id].Members)} on rows, {[Measures].[Unit Sales]} on columns from [Foo]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Product].[Drink], [Store].[Acapulco].[1]}\n"
            + "{[Product].[Drink], [Store].[Bellingham].[2]}\n"
            + "{[Product].[Drink], [Store].[Beverly Hills].[6]}\n"
            + "{[Product].[Drink], [Store].[Bremerton].[3]}\n"
            + "{[Product].[Drink], [Store].[Camacho].[4]}\n"
            + "{[Product].[Drink], [Store].[Guadalajara].[5]}\n"
            + "{[Product].[Drink], [Store].[Hidalgo].[12]}\n"
            + "{[Product].[Drink], [Store].[Hidalgo].[18]}\n"
            + "{[Product].[Drink], [Store].[Los Angeles].[7]}\n"
            + "{[Product].[Drink], [Store].[Merida].[8]}\n"
            + "{[Product].[Drink], [Store].[Mexico City].[9]}\n"
            + "{[Product].[Drink], [Store].[None].[0]}\n"
            + "{[Product].[Drink], [Store].[Orizaba].[10]}\n"
            + "{[Product].[Drink], [Store].[Portland].[11]}\n"
            + "{[Product].[Drink], [Store].[Salem].[13]}\n"
            + "{[Product].[Drink], [Store].[San Andres].[21]}\n"
            + "{[Product].[Drink], [Store].[San Diego].[24]}\n"
            + "{[Product].[Drink], [Store].[San Francisco].[14]}\n"
            + "{[Product].[Drink], [Store].[Seattle].[15]}\n"
            + "{[Product].[Drink], [Store].[Spokane].[16]}\n"
            + "{[Product].[Drink], [Store].[Tacoma].[17]}\n"
            + "{[Product].[Drink], [Store].[Vancouver].[19]}\n"
            + "{[Product].[Drink], [Store].[Victoria].[20]}\n"
            + "{[Product].[Drink], [Store].[Walla Walla].[22]}\n"
            + "{[Product].[Drink], [Store].[Yakima].[23]}\n"
            + "{[Product].[Food], [Store].[Acapulco].[1]}\n"
            + "{[Product].[Food], [Store].[Bellingham].[2]}\n"
            + "{[Product].[Food], [Store].[Beverly Hills].[6]}\n"
            + "{[Product].[Food], [Store].[Bremerton].[3]}\n"
            + "{[Product].[Food], [Store].[Camacho].[4]}\n"
            + "{[Product].[Food], [Store].[Guadalajara].[5]}\n"
            + "{[Product].[Food], [Store].[Hidalgo].[12]}\n"
            + "{[Product].[Food], [Store].[Hidalgo].[18]}\n"
            + "{[Product].[Food], [Store].[Los Angeles].[7]}\n"
            + "{[Product].[Food], [Store].[Merida].[8]}\n"
            + "{[Product].[Food], [Store].[Mexico City].[9]}\n"
            + "{[Product].[Food], [Store].[None].[0]}\n"
            + "{[Product].[Food], [Store].[Orizaba].[10]}\n"
            + "{[Product].[Food], [Store].[Portland].[11]}\n"
            + "{[Product].[Food], [Store].[Salem].[13]}\n"
            + "{[Product].[Food], [Store].[San Andres].[21]}\n"
            + "{[Product].[Food], [Store].[San Diego].[24]}\n"
            + "{[Product].[Food], [Store].[San Francisco].[14]}\n"
            + "{[Product].[Food], [Store].[Seattle].[15]}\n"
            + "{[Product].[Food], [Store].[Spokane].[16]}\n"
            + "{[Product].[Food], [Store].[Tacoma].[17]}\n"
            + "{[Product].[Food], [Store].[Vancouver].[19]}\n"
            + "{[Product].[Food], [Store].[Victoria].[20]}\n"
            + "{[Product].[Food], [Store].[Walla Walla].[22]}\n"
            + "{[Product].[Food], [Store].[Yakima].[23]}\n"
            + "{[Product].[Non-Consumable], [Store].[Acapulco].[1]}\n"
            + "{[Product].[Non-Consumable], [Store].[Bellingham].[2]}\n"
            + "{[Product].[Non-Consumable], [Store].[Beverly Hills].[6]}\n"
            + "{[Product].[Non-Consumable], [Store].[Bremerton].[3]}\n"
            + "{[Product].[Non-Consumable], [Store].[Camacho].[4]}\n"
            + "{[Product].[Non-Consumable], [Store].[Guadalajara].[5]}\n"
            + "{[Product].[Non-Consumable], [Store].[Hidalgo].[12]}\n"
            + "{[Product].[Non-Consumable], [Store].[Hidalgo].[18]}\n"
            + "{[Product].[Non-Consumable], [Store].[Los Angeles].[7]}\n"
            + "{[Product].[Non-Consumable], [Store].[Merida].[8]}\n"
            + "{[Product].[Non-Consumable], [Store].[Mexico City].[9]}\n"
            + "{[Product].[Non-Consumable], [Store].[None].[0]}\n"
            + "{[Product].[Non-Consumable], [Store].[Orizaba].[10]}\n"
            + "{[Product].[Non-Consumable], [Store].[Portland].[11]}\n"
            + "{[Product].[Non-Consumable], [Store].[Salem].[13]}\n"
            + "{[Product].[Non-Consumable], [Store].[San Andres].[21]}\n"
            + "{[Product].[Non-Consumable], [Store].[San Diego].[24]}\n"
            + "{[Product].[Non-Consumable], [Store].[San Francisco].[14]}\n"
            + "{[Product].[Non-Consumable], [Store].[Seattle].[15]}\n"
            + "{[Product].[Non-Consumable], [Store].[Spokane].[16]}\n"
            + "{[Product].[Non-Consumable], [Store].[Tacoma].[17]}\n"
            + "{[Product].[Non-Consumable], [Store].[Vancouver].[19]}\n"
            + "{[Product].[Non-Consumable], [Store].[Victoria].[20]}\n"
            + "{[Product].[Non-Consumable], [Store].[Walla Walla].[22]}\n"
            + "{[Product].[Non-Consumable], [Store].[Yakima].[23]}\n"
            + "Row #0: \n"
            + "Row #1: 208\n"
            + "Row #2: 1,945\n"
            + "Row #3: 2,288\n"
            + "Row #4: \n"
            + "Row #5: \n"
            + "Row #6: \n"
            + "Row #7: \n"
            + "Row #8: 2,422\n"
            + "Row #9: \n"
            + "Row #10: \n"
            + "Row #11: \n"
            + "Row #12: \n"
            + "Row #13: 2,371\n"
            + "Row #14: 3,735\n"
            + "Row #15: \n"
            + "Row #16: 2,560\n"
            + "Row #17: 175\n"
            + "Row #18: 2,213\n"
            + "Row #19: 2,238\n"
            + "Row #20: 3,092\n"
            + "Row #21: \n"
            + "Row #22: \n"
            + "Row #23: 191\n"
            + "Row #24: 1,159\n"
            + "Row #25: \n"
            + "Row #26: 1,587\n"
            + "Row #27: 15,438\n"
            + "Row #28: 17,809\n"
            + "Row #29: \n"
            + "Row #30: \n"
            + "Row #31: \n"
            + "Row #32: \n"
            + "Row #33: 18,294\n"
            + "Row #34: \n"
            + "Row #35: \n"
            + "Row #36: \n"
            + "Row #37: \n"
            + "Row #38: 18,632\n"
            + "Row #39: 29,905\n"
            + "Row #40: \n"
            + "Row #41: 18,369\n"
            + "Row #42: 1,555\n"
            + "Row #43: 18,159\n"
            + "Row #44: 16,925\n"
            + "Row #45: 25,453\n"
            + "Row #46: \n"
            + "Row #47: \n"
            + "Row #48: 1,622\n"
            + "Row #49: 8,192\n"
            + "Row #50: \n"
            + "Row #51: 442\n"
            + "Row #52: 3,950\n"
            + "Row #53: 4,479\n"
            + "Row #54: \n"
            + "Row #55: \n"
            + "Row #56: \n"
            + "Row #57: \n"
            + "Row #58: 4,947\n"
            + "Row #59: \n"
            + "Row #60: \n"
            + "Row #61: \n"
            + "Row #62: \n"
            + "Row #63: 5,076\n"
            + "Row #64: 7,940\n"
            + "Row #65: \n"
            + "Row #66: 4,706\n"
            + "Row #67: 387\n"
            + "Row #68: 4,639\n"
            + "Row #69: 4,428\n"
            + "Row #70: 6,712\n"
            + "Row #71: \n"
            + "Row #72: \n"
            + "Row #73: 390\n"
            + "Row #74: 2,140\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCollapsedError(Context<?> context) throws Exception {
        if (context.getConfigValue(ConfigConstants.USE_AGGREGATES, ConfigConstants.USE_AGGREGATES_DEFAULT_VALUE ,Boolean.class) == false
            && context.getConfigValue(ConfigConstants.READ_AGGREGATES, ConfigConstants.READ_AGGREGATES_DEFAULT_VALUE ,Boolean.class) == false)
        {
            return;
        }
        class TestCollapsedErrorModifier extends PojoMappingModifier {

            public TestCollapsedErrorModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> catalogCubes(CatalogMapping schema) {
                List<CubeMapping> result = new ArrayList<>();

            	TableQueryMappingImpl t = TableQueryMappingImpl.builder()
            			.withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
            			.withAggregationExcludes(
            				List.of(
            					AggregationExcludeMappingImpl.builder().withName("agg_g_ms_pcat_sales_fact_1997").build(),
            					AggregationExcludeMappingImpl.builder().withName("agg_c_14_sales_fact_1997").build(),
            					AggregationExcludeMappingImpl.builder().withName("agg_pl_01_sales_fact_1997").build(),
            					AggregationExcludeMappingImpl.builder().withName("agg_ll_01_sales_fact_1997").build()
                            )
            			)
            			.withAggregationTables(List.of(
            				AggregationNameMappingImpl.builder()
                                .withName(FoodmartMappingSupplier.AGG_L_05_SALES_FACT_1997)
                                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.FACT_COUNT_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build())
                                .withAggregationIgnoreColumns(List.of(
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build(),
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build(),
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build(),
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build(),
                                	AggregationColumnNameMappingImpl.builder().withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build()
                                ))
                                .withAggregationMeasures(List.of(
                                		AggregationMeasureMappingImpl.builder().withName("[Measures].[Unit Sales]").withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_AGG_L_05_SALES_FACT_1997).build()
                                ))
                                .withAggregationLevels(List.of(
                                	AggregationLevelMappingImpl.builder()
                                        .withName("[Product].[Product Id]")
                                        .withColumn(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_AGG_L_05_SALES_FACT_1997)
                                        .build()
                                ))
                                .build()
            			))
            			.build();

                LevelMappingImpl l1 = LevelMappingImpl.builder()
                    .withName("Product Family")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_FAMILY_COLUMN_IN_PRODUCT_CLASS)
                    .withUniqueMembers(true)
                    .build();
                LevelMappingImpl l2 = LevelMappingImpl.builder()
                    .withName("Product Department")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_DEPARTMENT_COLUMN_IN_PRODUCT_CLASS)
                    .withUniqueMembers(false)
                    .build();
                LevelMappingImpl l3 = LevelMappingImpl.builder()
                    .withName("Product Category")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_CATEGORY_COLUMN_IN_PRODUCT_CLASS)
                    .withUniqueMembers(false)
                    .build();
                LevelMappingImpl l4 = LevelMappingImpl.builder()
                    .withName("Product Subcategory")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_SUBCATEGORY_COLUMN_IN_PRODUCT_CLASS)
                    .withUniqueMembers(false)
                    .build();
                LevelMappingImpl l5 = LevelMappingImpl.builder()
                    .withName("Brand Name")
                    .withColumn(FoodmartMappingSupplier.BRAND_NAME_COLUMN_IN_PRODUCT)
                    .withUniqueMembers(false)
                    .build();
                LevelMappingImpl l6 = LevelMappingImpl.builder()
                    .withName("Product Name")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_NAME_COLUMN_IN_PRODUCT)
                    .withUniqueMembers(true)
                    .build();
                LevelMappingImpl l7 = LevelMappingImpl.builder()
                    .withName("Product Id")
                    .withColumn(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                    .withUniqueMembers(true)
                    .build();

                SumMeasureMappingImpl m = SumMeasureMappingImpl.builder()
                .withName("Unit Sales")
                .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                .withFormatString("Standard")
                .build();

                result.add(PhysicalCubeMappingImpl.builder()
                    .withName("Foo")
                    .withDefaultMeasure(m)
                    .withQuery(t)
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                            .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                            .withOverrideDimensionName("Product")
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("Product")
                            	.withHierarchies(List.of(
                                ExplicitHierarchyMappingImpl.builder()
                                    .withHasAll(true)
                                    .withPrimaryKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                                    .withQuery(
                                    		JoinQueryMappingImpl.builder()
                                        	.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT)
                                				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_TABLE).build())
                                				.build())
                                        	.withRight(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT_CLASS)
                                				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE).build())
                                				.build()).build()
                                    )
                                    .withLevels(List.of(
                                        l1, l2, l3, l4, l5, l6, l7
                                    ))
                                    .build()
                            )).build())
                            .build()
                    ))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                    		.withMeasures(List.of(m))
                    		.build()))
                    .build());
                result.addAll(super.catalogCubes(schema).stream().filter(c -> !"Foo".equals(c.getName())).toList());
                return result;
            }
        }
        /*
        final String cube =
            "<Cube name=\"Foo\" defaultMeasure=\"Unit Sales\">\n"
            + "  <Table name=\"sales_fact_1997\">\n"
            + "    <AggExclude name=\"agg_g_ms_pcat_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_c_14_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_pl_01_sales_fact_1997\"/>"
            + "    <AggExclude name=\"agg_ll_01_sales_fact_1997\"/>"
            + "    <AggName name=\"agg_l_05_sales_fact_1997\">"
            + "        <AggFactCount column=\"fact_count\"/>\n"
            + "        <AggIgnoreColumn column=\"customer_id\"/>\n"
            + "        <AggIgnoreColumn column=\"store_id\"/>\n"
            + "        <AggIgnoreColumn column=\"promotion_id\"/>\n"
            + "        <AggIgnoreColumn column=\"store_sales\"/>\n"
            + "        <AggIgnoreColumn column=\"store_cost\"/>\n"
            + "        <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"unit_sales\" />\n"
            + "        <AggLevel name=\"[Product].[Product Id]\" column=\"product_id\" collapsed=\"true\"/>\n"
            + "    </AggName>\n"
            + "</Table>\n"
            + "<Dimension foreignKey=\"product_id\" name=\"Product\">\n"
            + "<Hierarchy hasAll=\"true\" primaryKey=\"product_id\" primaryKeyTable=\"product\">\n"
            + "  <Join leftKey=\"product_class_id\" rightKey=\"product_class_id\">\n"
            + " <Table name=\"product\"/>\n"
            + " <Table name=\"product_class\"/>\n"
            + "  </Join>\n"
            + "  <Level name=\"Product Family\" table=\"product_class\" column=\"product_family\"\n"
            + "   uniqueMembers=\"true\"/>\n"
            + "  <Level name=\"Product Department\" table=\"product_class\" column=\"product_department\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Category\" table=\"product_class\" column=\"product_category\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Subcategory\" table=\"product_class\" column=\"product_subcategory\"\n"
            + "   uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Brand Name\" table=\"product\" column=\"brand_name\" uniqueMembers=\"false\"/>\n"
            + "  <Level name=\"Product Name\" table=\"product\" column=\"product_name\"\n"
            + "   uniqueMembers=\"true\"/>\n"
            + "  <Level name=\"Product Id\" table=\"product\" column=\"product_id\"\n"
            + "   uniqueMembers=\"true\"/>\n"
            + "</Hierarchy>\n"
            + "</Dimension>\n"
            + "<Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\"/>\n"
            + "</Cube>\n";
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
                null, cube, null, null, null, null);
        withSchema(context, schema);
         */
        withSchema(context, TestCollapsedErrorModifier::new);
        assertQueryThrows(context,
            "select {[Product].[Product Family].Members} on rows, {[Measures].[Unit Sales]} on columns from [Foo]",
            "Too many errors, '1', while loading/reloading aggregates.");
    }

    /**
     * Test case for bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1047">MONDRIAN-1047,
     * "IllegalArgumentException when cube has closure tables and many
     * levels"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBugMondrian1047(Context<?> context) {
        // Test case only works under MySQL, due to how columns are quoted.
        switch (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())) {
        case MARIADB:
        case MYSQL:
            break;
        default:
            return;
        }
        checkBugMondrian1047(context, 100); // 115 bits
        checkBugMondrian1047(context, 50); // 65 bits
        checkBugMondrian1047(context, 49); // 64 bits
        checkBugMondrian1047(context, 48); // 63 bits
        checkBugMondrian1047(context, 113); // 128 bits
        checkBugMondrian1047(context, 114); // 129 bits
    }


    public void checkBugMondrian1047(Context<?> context, int n) {
        class CheckBugMondrian1047Modifier extends PojoMappingModifier{
            public CheckBugMondrian1047Modifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("HR".equals(cube.getName())) {
                        SqlStatementMappingImpl sql = SqlStatementMappingImpl.builder().withSql("`position_title` + " + n).withDialects(
                            List.of("generic")).build();
                        SQLExpressionMappingColumnImpl ex = SQLExpressionMappingColumnImpl.builder().withSqls(List.of(sql)).withDataType(ColumnDataType.VARCHAR).build();
                        LevelMappingImpl level = LevelMappingImpl.builder()
                        	.withName("Position Title")
                        	.withUniqueMembers(false)
                        	.withOrdinalColumn(FoodmartMappingSupplier.POSITION_ID_COLUMN_IN_EMPLOYEE)
                            .withColumn(ex).build();
                        HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withHasAll(true)
                            .withAllMemberName("All Position")
                            .withPrimaryKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_EMPLOYEE)
                            .withLevels (List.of(level))
                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.EMPLOYEE_TABLE).build())
                            .build();
                        result.add(
                        	DimensionConnectorMappingImpl
                                .builder()
                                .withOverrideDimensionName("Position" + n)
                                .withForeignKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_SALARY)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Position" + n)
                                	.withHierarchies(List.of(hierarchy))
                                	.build()).build()
                        );
                }
                return result;
            }
        }
        withSchema(context, CheckBugMondrian1047Modifier::new);
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                "HR",
                TestUtil.repeatString(
                    n,
                    "<Dimension name='Position %1$d' foreignKey='employee_id'>\n"
                    + "  <Hierarchy hasAll='true' allMemberName='All Position' primaryKey='employee_id'>\n"
                    + "    <Table name='employee'/>\n"
                    + "    <Level name='Position Title' uniqueMembers='false' ordinalColumn='position_id'>\n"
                    + "      <KeyExpression><SQL dialect='generic'>`position_title` + %1$d</SQL></KeyExpression>\n"
                    + "    </Level>\n"
                    + "  </Hierarchy>\n"
                    + "</Dimension>"),
                null, false));
         */
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select from [HR]",
            "Axis #0:\n"
            + "{}\n"
            + "$39,431.67");
    }

    /**
     * Test case for bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1065">MONDRIAN-1065,
     * Incorrect data column is used in the WHERE clause of the SQL when
     * using Oracle DB</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBugMondrian1065(Context<?> context) {
        // Test case only works under Oracle
        switch (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())) {
        case ORACLE:
            break;
        default:
            return;
        }
        class TestBugMondrian1065Modifier extends PojoMappingModifier{
            public TestBugMondrian1065Modifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    PhysicalColumnMappingImpl lvl1Id = PhysicalColumnMappingImpl.builder().withName("lvl_1_id").withDataType(ColumnDataType.INTEGER).build();
                    PhysicalColumnMappingImpl lvl1Name = PhysicalColumnMappingImpl.builder().withName("lvl_1_name").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
                    PhysicalColumnMappingImpl lvl2Id = PhysicalColumnMappingImpl.builder().withName("lvl_2_id").withDataType(ColumnDataType.INTEGER).build();
                    PhysicalColumnMappingImpl lvl2Name = PhysicalColumnMappingImpl.builder().withName("lvl_2_name").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
                    PhysicalColumnMappingImpl lvl3Id = PhysicalColumnMappingImpl.builder().withName("lvl_3_id").withDataType(ColumnDataType.INTEGER).build();
                    PhysicalColumnMappingImpl lvl3Name = PhysicalColumnMappingImpl.builder().withName("lvl_3_name").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
                    InlineTableMappingImpl t = InlineTableMappingImpl.builder()
                    .withColumns(List.of(lvl1Id, lvl1Name, lvl2Id, lvl2Name, lvl3Id, lvl3Name))
                    .withRows(List.of(
                           RowMappingImpl.builder().withRowValues(List.of(
                                RowValueMappingImpl.builder().withColumn(lvl1Id).withValue("1").build(),
                                RowValueMappingImpl.builder().withColumn(lvl1Name).withValue("level 1").build(),
                                RowValueMappingImpl.builder().withColumn(lvl1Id).withValue("1").build(),
                                RowValueMappingImpl.builder().withColumn(lvl1Name).withValue("level 2 - 1").build(),
                                RowValueMappingImpl.builder().withColumn(lvl1Id).withValue("112").build(),
                                RowValueMappingImpl.builder().withColumn(lvl1Name).withValue("level 3 - 1").build())).build(),
                           RowMappingImpl.builder().withRowValues(List.of(
                                   RowValueMappingImpl.builder().withColumn(lvl1Id).withValue("1").build(),
                                   RowValueMappingImpl.builder().withColumn(lvl1Name).withValue("level 1").build(),
                                   RowValueMappingImpl.builder().withColumn(lvl1Id).withValue("1").build(),
                                   RowValueMappingImpl.builder().withColumn(lvl1Name).withValue("level 2 - 1").build(),
                                   RowValueMappingImpl.builder().withColumn(lvl1Id).withValue("114").build(),
                                   RowValueMappingImpl.builder().withColumn(lvl1Name).withValue("level 3 - 1").build())).build()
                    ))
                    .build();
                        InlineTableQueryMappingImpl i = InlineTableQueryMappingImpl.builder()
                            .withAlias("meatShack")
                            .withTable(t)
                            .build();
                    HierarchyMappingImpl hierarchy = ExplicitHierarchyMappingImpl
                            .builder()
                            .withHasAll(false)
                            .withPrimaryKey(lvl3Id)
                            .withLevels (List.of(
                                LevelMappingImpl.builder()
                                    .withName("Level1")
                                    .withColumn(lvl1Id)
                                    .withNameColumn(lvl1Name)
                                    .build(),
                                LevelMappingImpl.builder()
                                    .withName("Level2")
                                    .withColumn(lvl2Id)
                                    .withNameColumn(lvl2Name)
                                    .build(),
                                LevelMappingImpl.builder()
                                    .withName("Level3")
                                    .withColumn(lvl3Id)
                                    .withNameColumn(lvl3Name)
                                    .build()
                                ))
                            .withQuery(i)
                            .build();
                    		result.add(
                            DimensionConnectorMappingImpl
                                .builder()
                                .withOverrideDimensionName("PandaSteak")
                                .withForeignKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("PandaSteak")
                                	.withHierarchies(List.of(hierarchy)).build())
                                .build()
                        );
                }
                return result;
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "  <Dimension name=\"PandaSteak\" foreignKey=\"promotion_id\">\n"
            + "    <Hierarchy hasAll=\"false\" primaryKey=\"lvl_3_id\">\n"
            + "      <InlineTable alias=\"meatShack\">\n"
            + "        <ColumnDefs>\n"
            + "          <ColumnDef name=\"lvl_1_id\" type=\"Integer\"/>\n"
            + "          <ColumnDef name=\"lvl_1_name\" type=\"String\"/>\n"
            + "          <ColumnDef name=\"lvl_2_id\" type=\"Integer\"/>\n"
            + "          <ColumnDef name=\"lvl_2_name\" type=\"String\"/>\n"
            + "          <ColumnDef name=\"lvl_3_id\" type=\"Integer\"/>\n"
            + "          <ColumnDef name=\"lvl_3_name\" type=\"String\"/>\n"
            + "        </ColumnDefs>\n"
            + "        <Rows>\n"
            + "          <Row>\n"
            + "            <Value column=\"lvl_1_id\">1</Value>\n"
            + "            <Value column=\"lvl_1_name\">level 1</Value>\n"
            + "            <Value column=\"lvl_2_id\">1</Value>\n"
            + "            <Value column=\"lvl_2_name\">level 2 - 1</Value>\n"
            + "            <Value column=\"lvl_3_id\">112</Value>\n"
            + "            <Value column=\"lvl_3_name\">level 3 - 1</Value>\n"
            + "          </Row>\n"
            + "          <Row>\n"
            + "            <Value column=\"lvl_1_id\">1</Value>\n"
            + "            <Value column=\"lvl_1_name\">level 1</Value>\n"
            + "            <Value column=\"lvl_2_id\">1</Value>\n"
            + "            <Value column=\"lvl_2_name\">level 2 - 1</Value>\n"
            + "            <Value column=\"lvl_3_id\">114</Value>\n"
            + "            <Value column=\"lvl_3_name\">level 3 - 2</Value>\n"
            + "          </Row>\n"
            + "        </Rows>\n"
            + "      </InlineTable>\n"
            + "      <Level name=\"Level1\" column=\"lvl_1_id\" nameColumn=\"lvl_1_name\" />\n"
            + "      <Level name=\"Level2\" column=\"lvl_2_id\" nameColumn=\"lvl_2_name\" />\n"
            + "      <Level name=\"Level3\" column=\"lvl_3_id\" nameColumn=\"lvl_3_name\" />\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"));
         */

        withSchema(context, TestBugMondrian1065Modifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select non empty crossjoin({[PandaSteak].[Level3].[level 3 - 1], [PandaSteak].[Level3].[level 3 - 2]}, {[Measures].[Unit Sales], [Measures].[Store Cost]}) on columns, {[Product].[Product Family].Members} on rows from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[PandaSteak].[level 1].[level 2 - 1].[level 3 - 1], [Measures].[Unit Sales]}\n"
            + "{[PandaSteak].[level 1].[level 2 - 1].[level 3 - 1], [Measures].[Store Cost]}\n"
            + "{[PandaSteak].[level 1].[level 2 - 1].[level 3 - 2], [Measures].[Unit Sales]}\n"
            + "{[PandaSteak].[level 1].[level 2 - 1].[level 3 - 2], [Measures].[Store Cost]}\n"
            + "Axis #2:\n"
            + "{[Product].[Drink]}\n"
            + "{[Product].[Food]}\n"
            + "{[Product].[Non-Consumable]}\n"
            + "Row #0: 5\n"
            + "Row #0: 3.50\n"
            + "Row #0: 9\n"
            + "Row #0: 7.70\n"
            + "Row #1: 27\n"
            + "Row #1: 20.77\n"
            + "Row #1: 46\n"
            + "Row #1: 39.88\n"
            + "Row #2: 10\n"
            + "Row #2: 9.63\n"
            + "Row #2: 17\n"
            + "Row #2: 16.21\n");
    }

    /**
     * This is a test for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1390">MONDRIAN-1390</a>
     *
     * <p>Calling {@link CatalogReader#getLevelMembers(Level, boolean)}
     * directly would return the null members at the end, since it was
     * using TupleReader#readTuples instead of TupleReader#readMembers.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMondrian1390(Context<?> context) throws Exception {
        Catalog schema = context.getConnectionWithDefaultRole().getCatalog();
        Cube salesCube = schema.lookupCube("Sales").orElseThrow();
        CatalogReader sr = salesCube.getCatalogReader(null).withLocus();
        List<Member> members = sr.getLevelMembers(
            (Level)Util.lookupCompound(
                sr,
                salesCube,
                Util.parseIdentifier(
                    "[Store Size in SQFT].[Store Sqft]"),
                true,
                DataType.LEVEL),
            true);
        assertEquals(
            "[[Store Size in SQFT].[Store Size in SQFT].[#null], "
            + "[Store Size in SQFT].[Store Size in SQFT].[20319], "
            + "[Store Size in SQFT].[Store Size in SQFT].[21215], "
            + "[Store Size in SQFT].[Store Size in SQFT].[22478], "
            + "[Store Size in SQFT].[Store Size in SQFT].[23112], "
            + "[Store Size in SQFT].[Store Size in SQFT].[23593], "
            + "[Store Size in SQFT].[Store Size in SQFT].[23598], "
            + "[Store Size in SQFT].[Store Size in SQFT].[23688], "
            + "[Store Size in SQFT].[Store Size in SQFT].[23759], "
            + "[Store Size in SQFT].[Store Size in SQFT].[24597], "
            + "[Store Size in SQFT].[Store Size in SQFT].[27694], "
            + "[Store Size in SQFT].[Store Size in SQFT].[28206], "
            + "[Store Size in SQFT].[Store Size in SQFT].[30268], "
            + "[Store Size in SQFT].[Store Size in SQFT].[30584], "
            + "[Store Size in SQFT].[Store Size in SQFT].[30797], "
            + "[Store Size in SQFT].[Store Size in SQFT].[33858], "
            + "[Store Size in SQFT].[Store Size in SQFT].[34452], "
            + "[Store Size in SQFT].[Store Size in SQFT].[34791], "
            + "[Store Size in SQFT].[Store Size in SQFT].[36509], "
            + "[Store Size in SQFT].[Store Size in SQFT].[38382], "
            + "[Store Size in SQFT].[Store Size in SQFT].[39696]]",
            members.toString());
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMondrian1499(Context<?> context) throws Exception {
        ((TestContextImpl)context).setUseAggregates(false);
        ((TestContextImpl)context).setReadAggregates(false);
        class TestMondrian1499Modifier extends PojoMappingModifier {

            public TestMondrian1499Modifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected CatalogMapping modifyCatalog(CatalogMapping schemaMappingOriginal) {
                DimensionConnectorMappingImpl d1 = DimensionConnectorMappingImpl.builder()
                		.withOverrideDimensionName("Store")
                        .withForeignKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_SALARY)
                        .withDimension(StandardDimensionMappingImpl.builder()
                        	.withName("Store")
                        	.withHierarchies(List.of(
                            ExplicitHierarchyMappingImpl.builder()
                                .withHasAll(true)
                                .withPrimaryKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_EMPLOYEE)
                                .withQuery(
                                		JoinQueryMappingImpl.builder()
                                    	.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_EMPLOYEE)
                            				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.EMPLOYEE_TABLE)
                            						.withSqlWhereExpression(SqlStatementMappingImpl.builder().withSql("1 = 1").withDialects(List.of("generic")).build())
                            						.build())
                            				.build())
                                    	.withRight(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                            				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                            				.build())
                                    	.build()
                                )
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
                                                .withName("Store Type")
                                                .withColumn(FoodmartMappingSupplier.STORE_TYPE_COLUMN_IN_STORE)
                                                .build(),
                                            MemberPropertyMappingImpl.builder()
                                                .withName("Store Manager")
                                                .withColumn(FoodmartMappingSupplier.STORE_MANAGER_COLUMN_IN_STORE)
                                                .build(),
                                            MemberPropertyMappingImpl.builder()
                                                .withName("Store Sqft")
                                                .withColumn(FoodmartMappingSupplier.STORE_SQFT_COLUMN_IN_STORE)
                                                .withDataType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                                                .build(),
                                            MemberPropertyMappingImpl.builder()
                                                .withName("Grocery Sqft")
                                                .withColumn(FoodmartMappingSupplier.GROCERY_SQFT_COLUMN_IN_STORE)
                                                .withDataType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                                                .build(),
                                            MemberPropertyMappingImpl.builder()
                                                .withName("Frozen Sqft")
                                                .withColumn(FoodmartMappingSupplier.FROZEN_SQFT_COLUMN_IN_STORE)
                                                .withDataType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                                                .build(),
                                            MemberPropertyMappingImpl.builder()
                                                .withName("Meat Sqft")
                                                .withColumn(FoodmartMappingSupplier.MEAT_SQFT_COLUMN_IN_STORE)
                                                .withDataType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                                                .build(),
                                            MemberPropertyMappingImpl.builder()
                                                .withName("Has coffee bar")
                                                .withColumn(FoodmartMappingSupplier.COFFEE_BAR_COLUMN_IN_STORE)
                                                .withDataType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.BOOLEAN)
                                                .build(),
                                            MemberPropertyMappingImpl.builder()
                                                .withName("Street address")
                                                .withColumn(FoodmartMappingSupplier.STREET_ADDRESS_COLUMN_IN_STORE)
                                                .withDataType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.STRING)
                                                .build()
                                        ))
                                        .build()
                                ))
                                .build()
                        )).build()).build();

                DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                		.withOverrideDimensionName("Pay Type")
                        .withForeignKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_SALARY)
                        .withDimension(StandardDimensionMappingImpl.builder()
                            .withName("Pay Type")
                            .withHierarchies(List.of(
                            	ExplicitHierarchyMappingImpl.builder()
                                .withHasAll(true)
                                .withPrimaryKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_EMPLOYEE)
                                .withQuery(
                                		JoinQueryMappingImpl.builder()
                                    	.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.POSITION_ID_COLUMN_IN_EMPLOYEE)
                            				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.EMPLOYEE_TABLE)
                            						.withSqlWhereExpression(SqlStatementMappingImpl.builder().withSql("1 = 1").build())
                            						.build())
                            				.build())
                                    	.withRight(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.POSITION_ID_COLUMN_IN_POSITION)
                            				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.POSITION_TABLE).build())
                            				.build())
                                    	.build()
                                )
                                .withLevels(List.of(
                                    LevelMappingImpl.builder()
                                        .withName("Pay Type")
                                        .withColumn(FoodmartMappingSupplier.PAY_TYPE_COLUMN_IN_POSITION)
                                        .withUniqueMembers(true)
                                        .build()
                                ))
                                .build()
                        )).build())
                        .build();

                DimensionConnectorMappingImpl d3 = DimensionConnectorMappingImpl.builder()
                		.withOverrideDimensionName("Store Type")
                        .withForeignKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_SALARY)
                        .withDimension(StandardDimensionMappingImpl.builder()
                                .withName("Store Type")
                                .withHierarchies(List.of(
                                ExplicitHierarchyMappingImpl.builder()
                                .withHasAll(true)
                                .withPrimaryKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_EMPLOYEE)
                                .withQuery(
                                		JoinQueryMappingImpl.builder()
                                    	.withLeft(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_EMPLOYEE)
                            				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.EMPLOYEE_TABLE)
                            						.withSqlWhereExpression(SqlStatementMappingImpl.builder().withSql("1 = 1").build())
                            						.build())
                            				.build())
                                    	.withRight(JoinedQueryElementMappingImpl.builder().withKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                            				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                            				.build())
                                    	.build()
                                )
                                .withLevels(List.of(
                                    LevelMappingImpl.builder()
                                        .withName("Store Type")
                                        .withColumn(FoodmartMappingSupplier.STORE_TYPE_COLUMN_IN_STORE)
                                        .build()
                                ))
                                .build()
                        )).build())
                        .build();

                DimensionConnectorMappingImpl d4 = DimensionConnectorMappingImpl.builder()
                		.withOverrideDimensionName("Position")
                        .withForeignKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_SALARY)
                        .withDimension(StandardDimensionMappingImpl.builder()
                                .withName("Position")
                                .withHierarchies(List.of(
                                ExplicitHierarchyMappingImpl.builder()
                                .withHasAll(true)
                                .withAllMemberName("All Position")
                                .withPrimaryKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_EMPLOYEE)
                                .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.EMPLOYEE_TABLE)
                						.withSqlWhereExpression(SqlStatementMappingImpl.builder().withSql("1 = 1").build())
                						.build())
                                .withLevels(List.of(
                                    LevelMappingImpl.builder()
                                        .withName("Management Role")
                                        .withUniqueMembers(true)
                                        .withColumn(FoodmartMappingSupplier.MANAGEMENT_ROLE_COLUMN_IN_EMPLOYEE)
                                        .build(),
                                    LevelMappingImpl.builder()
                                        .withName("Position Title")
                                        .withUniqueMembers(false)
                                        .withColumn(FoodmartMappingSupplier.POSITION_TITLE_COLUMN_IN_EMPLOYEE)
                                        .withOrdinalColumn(FoodmartMappingSupplier.POSITION_ID_COLUMN_IN_EMPLOYEE)
                                        .build()
                                ))
                                .build()
                        )).build())
                        .build();

                DimensionConnectorMappingImpl d5 = DimensionConnectorMappingImpl.builder()
                		.withOverrideDimensionName("Employees")
                        .withForeignKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_SALARY)
                        .withDimension(StandardDimensionMappingImpl.builder()
                                .withName("Employees")
                                .withHierarchies(List.of(
                                ParentChildHierarchyMappingImpl.builder()
                                .withHasAll(true)
                                .withAllMemberName("All Employees")
                                .withPrimaryKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_EMPLOYEE)
                                .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.EMPLOYEE_TABLE)
                						.withSqlWhereExpression(SqlStatementMappingImpl.builder().withSql("1 = 1").build())
                						.build())
                                .withParentColumn(FoodmartMappingSupplier.SUPERVISOR_ID_COLUMN_IN_EMPLOYEE)
                                .withNullParentValue("0")
                                .withParentChildLink(ParentChildLinkMappingImpl.builder()
                                        .withParentColumn(FoodmartMappingSupplier.SUPERVISOR_ID_COLUMN_IN_EMPLOYEE_CLOSURE)
                                        .withChildColumn(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_EMPLOYEE_CLOSURE)
                                        .withTable(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.EMPLOYEE_CLOSURE_TABLE).build())
                                        .build())
                                .withLevel(
                                    LevelMappingImpl.builder()
                                        .withName("Employee Id")
                                        .withType(org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType.NUMERIC)
                                        .withUniqueMembers(true)
                                        .withColumn(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_EMPLOYEE)
                                        .withNameColumn(FoodmartMappingSupplier.FULL_NAME_COLUMN_IN_EMPLOYEE)
                                        .withMemberProperties(List.of(
                                            MemberPropertyMappingImpl.builder()
                                                .withName("Marital Status")
                                                .withColumn(FoodmartMappingSupplier.MARITAL_STATUS_COLUMN_IN_EMPLOYEE)
                                                .build(),
                                            MemberPropertyMappingImpl.builder()
                                                .withName("Position Title")
                                                .withColumn(FoodmartMappingSupplier.POSITION_TITLE_COLUMN_IN_EMPLOYEE)
                                                .build(),
                                            MemberPropertyMappingImpl.builder()
                                                .withName("Gender")
                                                .withColumn(FoodmartMappingSupplier.GENDER_COLUMN_IN_EMPLOYEE)
                                                .build(),
                                            MemberPropertyMappingImpl.builder()
                                                .withName("Salary")
                                                .withColumn(FoodmartMappingSupplier.SALARY_COLUMN_IN_EMPLOYEE)
                                                .build(),
                                            MemberPropertyMappingImpl.builder()
                                                .withName("Education Level")
                                                .withColumn(FoodmartMappingSupplier.EDUCATION_LEVEL_COLUMN_IN_EMPLOYEE)
                                                .build(),
                                            MemberPropertyMappingImpl.builder()
                                                .withName("Management Role")
                                                .withColumn(FoodmartMappingSupplier.MANAGEMENT_ROLE_COLUMN_IN_EMPLOYEE)
                                                .build()
                                        ))
                                        .build()
                                )
                                .build()
                        )).build())
                        .build();
                    return CatalogMappingImpl.builder()
                        .withName("FoodMart")
                        .withDbSchemas((List<DatabaseSchemaMappingImpl>) catalogDatabaseSchemas( schemaMappingOriginal))
                        .withCubes(List.of(
                            PhysicalCubeMappingImpl.builder()
                                .withName("HR")
                                .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALARY_TABLE).build())
                                .withDimensionConnectors(List.of(
                                    d1, d2, d3, d4, d5
                                ))
                                .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                                		.withMeasures(List.of(
                                                SumMeasureMappingImpl.builder()
                                                .withName("Org Salary")
                                                .withColumn(FoodmartMappingSupplier.SALARY_PAID_COLUMN_IN_SALARY)

                                                .withFormatString("Currency")
                                                .build()
                                        ))
                                		.build()))
                                .build()
                        ))
                        .build();

            }
        }
        /*
        withSchema(context,
                "<?xml version='1.0'?>\n"
                + "<Schema name='FoodMart'>\n"
                + "<Cube name=\"HR\">\n"
                + "  <Table name=\"salary\"/>\n"
                + "  <Dimension name=\"Store\" foreignKey=\"employee_id\" >\n"
                + "    <Hierarchy hasAll=\"true\" primaryKey=\"employee_id\"\n"
                + "        primaryKeyTable=\"employee\">\n"
                + "      <Join leftKey=\"store_id\" rightKey=\"store_id\">\n"
                + "        <Table name=\"employee\">\n"
                + "         <SQL>1 = 1</SQL>\n"
                + "     </Table>\n"
                + "        <Table name=\"store\"/>\n"
                + "      </Join>\n"
                + "      <Level name=\"Store Country\" table=\"store\" column=\"store_country\"\n"
                + "          uniqueMembers=\"true\"/>\n"
                + "      <Level name=\"Store State\" table=\"store\" column=\"store_state\"\n"
                + "          uniqueMembers=\"true\"/>\n"
                + "      <Level name=\"Store City\" table=\"store\" column=\"store_city\"\n"
                + "          uniqueMembers=\"false\"/>\n"
                + "      <Level name=\"Store Name\" table=\"store\" column=\"store_name\"\n"
                + "          uniqueMembers=\"true\">\n"
                + "        <Property name=\"Store Type\" column=\"store_type\"/>\n"
                + "        <Property name=\"Store Manager\" column=\"store_manager\"/>\n"
                + "        <Property name=\"Store Sqft\" column=\"store_sqft\" type=\"Numeric\"/>\n"
                + "        <Property name=\"Grocery Sqft\" column=\"grocery_sqft\" type=\"Numeric\"/>\n"
                + "        <Property name=\"Frozen Sqft\" column=\"frozen_sqft\" type=\"Numeric\"/>\n"
                + "        <Property name=\"Meat Sqft\" column=\"meat_sqft\" type=\"Numeric\"/>\n"
                + "        <Property name=\"Has coffee bar\" column=\"coffee_bar\" type=\"Boolean\"/>\n"
                + "        <Property name=\"Street address\" column=\"store_street_address\"\n"
                + "            type=\"String\"/>\n"
                + "      </Level>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"
                + "  <Dimension name=\"Pay Type\" foreignKey=\"employee_id\">\n"
                + "    <Hierarchy hasAll=\"true\" primaryKey=\"employee_id\"\n"
                + "        primaryKeyTable=\"employee\">\n"
                + "      <Join leftKey=\"position_id\" rightKey=\"position_id\">\n"
                + "        <Table name=\"employee\">\n"
                + "       <SQL>1 = 1</SQL>\n"
                + "     </Table>\n"
                + "        <Table name=\"position\"/>\n"
                + "      </Join>\n"
                + "      <Level name=\"Pay Type\" table=\"position\" column=\"pay_type\"\n"
                + "          uniqueMembers=\"true\"/>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"
                + "  <Dimension name=\"Store Type\" foreignKey=\"employee_id\">\n"
                + "    <Hierarchy hasAll=\"true\" primaryKeyTable=\"employee\" primaryKey=\"employee_id\">\n"
                + "      <Join leftKey=\"store_id\" rightKey=\"store_id\">\n"
                + "        <Table name=\"employee\">\n"
                + "       <SQL>1 = 1</SQL>\n"
                + "     </Table>\n"
                + "        <Table name=\"store\"/>\n"
                + "      </Join>\n"
                + "      <Level name=\"Store Type\" table=\"store\" column=\"store_type\"\n"
                + "          uniqueMembers=\"true\"/>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"

                + "  <Dimension name=\"Position\" foreignKey=\"employee_id\">\n"
                + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Position\"\n"
                + "        primaryKey=\"employee_id\">\n"
                + "      <Table name=\"employee\">\n"
                + "     <SQL>1 = 1</SQL>\n"
                + "   </Table>\n"
                + "      <Level name=\"Management Role\" uniqueMembers=\"true\"\n"
                + "          column=\"management_role\"/>\n"
                + "      <Level name=\"Position Title\" uniqueMembers=\"false\"\n"
                + "          column=\"position_title\" ordinalColumn=\"position_id\"/>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"
                + "  <Dimension name=\"Employees\" foreignKey=\"employee_id\">\n"
                + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Employees\"\n"
                + "        primaryKey=\"employee_id\">\n"
                + "      <Table name=\"employee\">\n"
                + "     <SQL>1 = 1</SQL>\n"
                + "   </Table>\n"
                + "      <Level name=\"Employee Id\" type=\"Numeric\" uniqueMembers=\"true\"\n"
                + "          column=\"employee_id\" parentColumn=\"supervisor_id\"\n"
                + "          nameColumn=\"full_name\" nullParentValue=\"0\">\n"
                + "        <Closure parentColumn=\"supervisor_id\" childColumn=\"employee_id\">\n"
                + "          <Table name=\"employee_closure\"/>\n"
                + "        </Closure>\n"
                + "        <Property name=\"Marital Status\" column=\"marital_status\"/>\n"
                + "        <Property name=\"Position Title\" column=\"position_title\"/>\n"
                + "        <Property name=\"Gender\" column=\"gender\"/>\n"
                + "        <Property name=\"Salary\" column=\"salary\"/>\n"
                + "        <Property name=\"Education Level\" column=\"education_level\"/>\n"
                + "        <Property name=\"Management Role\" column=\"management_role\"/>\n"
                + "      </Level>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"
                + "  <Measure name=\"Org Salary\" column=\"salary_paid\" aggregator=\"sum\"\n"
                + "      formatString=\"Currency\"/>\n"
                + "</Cube>\n"
                + "</Schema>");
         */
        withSchema(context, TestMondrian1499Modifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures].[Org Salary]} on columns,\n"
            + "{[Store].[Store Country].Members} on rows from [HR]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Org Salary]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[Canada]}\n"
            + "{[Store].[Store].[Mexico]}\n"
            + "{[Store].[Store].[USA]}\n"
            + "Row #0: $7,473.54\n"
            + "Row #1: $180,599.76\n"
            + "Row #2: $83,479.14\n");
    }

    /**
    * Testcase for bug
    * <a href="http://jira.pentaho.com/browse/MONDRIAN-1073">MONDRIAN-1073,
    * "Two cubes operating on same fact table gives wrong WHERE clause"</a>.
    */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMondrian1073(Context<?> context) throws Exception {
        class TestMondrian1073Modifier extends PojoMappingModifier {

            public TestMondrian1073Modifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> catalogCubes(CatalogMapping schema) {
                List<CubeMapping> result = new ArrayList<>();
                SumMeasureMappingImpl mA = SumMeasureMappingImpl.builder()
                .withName("Unit Sales")
                .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                .withFormatString("Standard")
                .build();
                SumMeasureMappingImpl mB = SumMeasureMappingImpl.builder()
                .withName("Unit Sales")
                .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                .withFormatString("Standard")
                .build();

                result.add(PhysicalCubeMappingImpl.builder()
                    .withName("CubeA")
                    .withDefaultMeasure(mA)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).withAlias("TableAlias")
                    		.withSqlWhereExpression(SqlStatementMappingImpl.builder()
                    				.withSql("`TableAlias`.`promotion_id` = 108")
                    				.withDialects(List.of("mysql"))
                    				.build()).build())
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Store Type")
                    		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_STORE_TYPE_WITH_QUERY_STORE))
                            .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                            .build()
                        )
                    )
                    .withMeasureGroups(List.of(
                    	MeasureGroupMappingImpl.builder()
                    	.withMeasures(List.of(
                    		mA,
                            CountMeasureMappingImpl.builder()
                                .withName("Customer Count")
                                .withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDistinct(true)
                                .withFormatString("#,###")
                                .build(),
                            CountMeasureMappingImpl.builder()
                                .withName("Fantastic Count for Different Types of Promotion")
                                .withColumn(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997)
                                .withFormatString("Standard")
                                .build()
                    	))
                    	.build()
                    ))
                    .build());
                result.add(PhysicalCubeMappingImpl.builder()
                    .withName("CubeB")
                    .withDefaultMeasure(mB)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).withAlias("TableAlias")
                    		.withSqlWhereExpression(SqlStatementMappingImpl.builder()
                    				.withSql("`TableAlias`.`promotion_id` = 112")
                    				.withDialects(List.of("mysql"))
                    				.build()).build())
                    .withDimensionConnectors(List.of(
                        	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Store Type")
                    		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_STORE_TYPE_WITH_QUERY_STORE))
                            .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                            .build()
                        )
                    )
                    .withMeasureGroups(List.of(
                        	MeasureGroupMappingImpl.builder()
                        	.withMeasures(List.of(
                        mB,
                        CountMeasureMappingImpl.builder()
                            .withName("Fantastic Count for Different Types of Promotion")
                            .withColumn(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997)
                            .withFormatString("Standard")
                            .build()
                    )).build()))
                    .build());
                result.addAll(super.catalogCubes(schema));
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
                    null, CUBES_AB,
                    null, null, null, null);
        withSchema(context, schema);
        */
        withSchema(context, TestMondrian1073Modifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(), "CubeB",
            "SELECT [Measures].[Fantastic Count for Different Types of Promotion] ON COLUMNS\n"
            + "FROM [CubeB]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Fantastic Count for Different Types of Promotion]}\n"
            + "Row #0: 22\n");
  }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMultiByteSchemaReadFromFile(Context<?> context) throws IOException {
        //String rawSchema = TestContext.getRawFoodMartSchema().replace(
        class TestMultiByteSchemaReadFromFile extends PojoMappingModifier {

            public TestMultiByteSchemaReadFromFile(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected HierarchyMapping hierarchy(HierarchyMapping hierarchy) {
                HierarchyMapping h = super.hierarchy(hierarchy);
                if (h != null && h.isHasAll()
                    && "All Gender".equals(h.getAllMemberName())
                    && "customer_id".equals(h.getPrimaryKey().getName())) {
                	((HierarchyMappingImpl)h).setName("地域");
                }
                return h;
            }
        }
        /*
        String rawSchema = TestUtil.getRawSchema(context).replace(
            "<Hierarchy hasAll=\"true\" allMemberName=\"All Gender\" primaryKey=\"customer_id\">",
            "<Hierarchy name=\"地域\" hasAll=\"true\" allMemberName=\"All Gender\" primaryKey=\"customer_id\">");
        File schemaFile = File.createTempFile("multiByteSchema", ",xml");
        schemaFile.deleteOnExit();
        FileOutputStream output = new FileOutputStream(schemaFile);
        output.write(rawSchema.getBytes());
        output.close();

        //final Util.PropertyList properties =
        //    getConnectionProperties().clone();
        //properties.put(
        //    RolapConnectionProperties.Catalog.name(),
        //    schemaFile.getAbsolutePath());
        context.setProperty(RolapConnectionProperties.Catalog.name(),
                schemaFile.getAbsolutePath());
         */
        withSchema(context, TestMultiByteSchemaReadFromFile::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select [Gender].members on 0 from sales",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Gender].[地域].[All Gender]}\n"
            + "{[Gender].[地域].[F]}\n"
            + "{[Gender].[地域].[M]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: 131,558\n"
            + "Row #0: 135,215\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBugMonrian2528(Context<?> context) {
        class TestBugMonrian2528Modifier extends PojoMappingModifier {

            public TestBugMonrian2528Modifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<AccessRoleMapping> catalogAccessRoles(CatalogMapping schema) {
                List<AccessRoleMapping> result = new ArrayList<>();
                result.addAll(super.catalogAccessRoles(schema));
                result.add(
                	AccessRoleMappingImpl.builder()
                        .withName("admin")
                        .withAccessCatalogGrants(
                            List.of(
                            AccessCatalogGrantMappingImpl.builder()
                                .withAccess(AccessCatalog.ALL)
                                .build()))
                        .build());
                result.add(
                    AccessRoleMappingImpl.builder()
                        .withName("dev")
                        .withAccessCatalogGrants(
                            List.of(
                                AccessCatalogGrantMappingImpl
                                    .builder()
                                    .withAccess(AccessCatalog.ALL)
                                    .withCubeGrant(List.of(
                                        AccessCubeGrantMappingImpl.builder()
                                            .withCube((CubeMappingImpl) look(FoodmartMappingSupplier.CUBE_SALES))
                                            .withAccess(AccessCube.ALL)
                                            .withHierarchyGrants(
                                                List.of(
                                                	AccessHierarchyGrantMappingImpl
                                                        .builder()
                                                        //.withHierarchy("[Measures]")
                                                        .withAccess(AccessHierarchy.CUSTOM)
                                                        .withMemberGrants(List.of(
                                                            AccessMemberGrantMappingImpl.builder()
                                                                .withMember("[Measures].[Store Cost]")
                                                                .withAccess(AccessMember.ALL)
                                                                .build(),
                                                            AccessMemberGrantMappingImpl.builder()
                                                                .withMember("[Measures].[Store Sales]")
                                                                .withAccess(AccessMember.ALL)
                                                                .build(),
                                                            AccessMemberGrantMappingImpl.builder()
                                                                .withMember("[Measures].[Sales Count]")
                                                                .withAccess(AccessMember.ALL)
                                                                .build()
                                                        ))
                                                        .build()
                                                )
                                            )
                                            .build()
                                    )).build()
                            )
                        )
                        .build()
                    );
                return result;
            }
        }
                // Default member [Measures].[Unit Sales] is denied for the current role.
      // Before the fix ClassCastException was thrown on query.

      /*
      String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
          null, null, null, null, null,
          "<Role name=\"admin\">\n"
          + "  <SchemaGrant access=\"all\">\n"
          + "  </SchemaGrant>\n"
          + "</Role>\n"
          + "<Role name=\"dev\">\n"
          + "  <SchemaGrant access=\"all\">\n"
          + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
          + "      <HierarchyGrant hierarchy=\"[Measures]\""
          + " access=\"custom\">\n"
          + "        <MemberGrant member=\"[Measures].[Store Cost]\""
          + " access=\"all\">\n"
          + "        </MemberGrant>\n"
          + "        <MemberGrant member=\"[Measures].[Store Sales]\""
          + " access=\"all\">\n"
          + "        </MemberGrant>\n"
          + "        <MemberGrant member=\"[Measures].[Sales Count]\""
          + " access=\"all\">\n"
          + "        </MemberGrant>\n"
          + "      </HierarchyGrant>\n"
          + "    </CubeGrant>\n"
          + "  </SchemaGrant>\n"
          + "</Role>\n");
        withSchema(context, schema);
       */
        withSchema(context, TestBugMonrian2528Modifier::new);

      assertQueryReturns(((TestContext)context).getConnection(new ConnectionProps(List.of("dev"))),
          "SELECT\n"
          + "[Product].[All Products] ON 0,\n"
          + "[Measures].[Store Sales] ON 1\n"
          + "FROM [Sales]\n"
          + "WHERE FILTER([Store Type].children, [Store Type].CURRENTMEMBER NOT IN {[Store Type].[Deluxe Supermarket], [Store Type].[Gourmet Supermarket]})\n",
          "Axis #0:\n"
          + "{[Store Type].[Store Type].[HeadQuarters]}\n"
          + "{[Store Type].[Store Type].[Mid-Size Grocery]}\n"
          + "{[Store Type].[Store Type].[Small Grocery]}\n"
          + "{[Store Type].[Store Type].[Supermarket]}\n"
          + "Axis #1:\n"
          + "{[Product].[Product].[All Products]}\n"
          + "Axis #2:\n"
          + "{[Measures].[Store Sales]}\n"
          + "Row #0: 357,425.65\n"
);
  }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMondrian1275(Context<?> context) throws Exception {
        class TestMondrian1275Modifier extends PojoMappingModifier {

            public TestMondrian1275Modifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected CatalogMapping modifyCatalog(CatalogMapping schemaMappingOriginal) {
                HierarchyMappingImpl h1 = ExplicitHierarchyMappingImpl.builder()
                        .withHasAll(true)
                        .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                        .withLevels(List.of(LevelMappingImpl.builder()
                            .withName("Store Type").withColumn(FoodmartMappingSupplier.STORE_TYPE_COLUMN_IN_STORE).withUniqueMembers(true).build()))
                        .build();

                    StandardDimensionMappingImpl d1 = StandardDimensionMappingImpl.builder()
                        .withName("Store Type")
                        .withAnnotations(List.of(
                            AnnotationMappingImpl.builder()
                                .withName("foo")
                                .withValue("bar")
                                .build()
                        ))
                        .withHierarchies(List.of(h1))
                        .build();

                    DimensionConnectorMappingImpl d2 = DimensionConnectorMappingImpl.builder()
                    	.withOverrideDimensionName("Store Type")
                    	.withDimension(d1)
                        .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                        .build();

                    SumMeasureMappingImpl m = SumMeasureMappingImpl.builder()
                    .withName("Unit Sales")
                    .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)

                    .withFormatString("Standard")
                    .build();

                    PhysicalCubeMappingImpl c1 = PhysicalCubeMappingImpl.builder()
                        .withName("Sales")
                        .withDefaultMeasure(m)
                        .withQuery(
                        TableQueryMappingImpl.builder()
            			.withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)
            			.withAggregationExcludes(
            				List.of(
                        		AggregationExcludeMappingImpl.builder().withName("agg_c_special_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_lc_100_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_lc_10_sales_fact_1997").build(),
                        		AggregationExcludeMappingImpl.builder().withName("agg_pc_10_sales_fact_1997").build()
                            )
            			).build())
                        .withDimensionConnectors(List.of(
                            d2
                        ))
                        .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                        		.withMeasures(List.of(m))
                        		.build()))
                        .build();

                    return CatalogMappingImpl.builder()
                        .withName("FoodMart")
                        .withDbSchemas((List<DatabaseSchemaMappingImpl>) catalogDatabaseSchemas( schemaMappingOriginal))
                        .withCubes(List.of(c1))
                        .build();

            }
        }
        /*
        withSchema(context,
                                "<?xml version=\"1.0\"?>\n"
                                        + "<Schema name=\"FoodMart\">\n"
                                        + "  <Dimension name=\"Store Type\">\n"
                                        + "    <Annotations>\n"
                                        + "      <Annotation name=\"foo\">bar</Annotation>\n"
                                        + "    </Annotations>\n"
                                        + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">\n"
                                        + "      <Table name=\"store\"/>\n"
                                        + "      <Level name=\"Store Type\" column=\"store_type\" uniqueMembers=\"true\"/>\n"
                                        + "    </Hierarchy>\n"
                                        + "  </Dimension>\n"
                                        + "<Cube name=\"Sales\" defaultMeasure=\"Unit Sales\">\n"
                                        + "  <Table name=\"sales_fact_1997\">\n"
                                        + "    <AggExclude name=\"agg_c_special_sales_fact_1997\" />\n"
                                        + "    <AggExclude name=\"agg_lc_100_sales_fact_1997\" />\n"
                                        + "    <AggExclude name=\"agg_lc_10_sales_fact_1997\" />\n"
                                        + "    <AggExclude name=\"agg_pc_10_sales_fact_1997\" />\n"
                                        + "  </Table>\n"
                                        + "  <DimensionUsage name=\"Store Type\" source=\"Store Type\" foreignKey=\"store_id\"/>\n"
                                        + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
                                        + "      formatString=\"Standard\"/>\n"
                                        + "</Cube>\n"
                                        + "</Schema>\n");
        */
        withSchema(context, TestMondrian1275Modifier::new);
        final RolapConnection rolapConn = (RolapConnection) context.getConnectionWithDefaultRole();
        final CatalogReader schemaReader = rolapConn.getCatalogReader();
        final Catalog schema = schemaReader.getCatalog();
        for (Cube cube : schema.getCubes()) {
            Dimension dim = cube.getDimensions().get(1);
            final MetaData metaData = dim.getMetaData();
            assertEquals(1, metaData.size());
            assertEquals("bar", metaData.get("foo"));
        }
    }

}
