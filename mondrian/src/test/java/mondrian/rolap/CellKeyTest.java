/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/

package mondrian.rolap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.isDefaultNullMemberRepresentation;
import static org.opencube.junit5.TestUtil.withSchema;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.key.CellKey;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.instance.rec.complex.foodmart.FoodmartMappingSupplier;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.ExplicitHierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SumMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

/**
 * Test that the implementations of the CellKey interface are correct.
 *
 * @author Richard M. Emberson
 */
class CellKeyTest  {

    @BeforeEach
    public void beforeEach() {
    }

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCellLookup(Context<?> context) {
        if (!isDefaultNullMemberRepresentation()) {
            return;
        }
        String cubeDef =
            "<Cube name = \"SalesTest\" defaultMeasure=\"Unit Sales\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <Dimension name=\"City\" foreignKey=\"customer_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
            + "      <Table name=\"customer\"/>\n"
            + "      <Level name=\"city\" column=\"city\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Dimension name=\"Gender\" foreignKey=\"customer_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
            + "      <Table name=\"customer\"/>\n"
            + "      <Level name=\"gender\" column=\"gender\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Dimension name=\"Address2\" foreignKey=\"customer_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
            + "      <Table name=\"customer\"/>\n"
            + "      <Level name=\"addr\" column=\"address2\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" formatString=\"Standard\"/>\n"
            + "</Cube>";

        String query =
            "With Set [*NATIVE_CJ_SET] as NonEmptyCrossJoin([Gender].Children, [Address2].Children) "
            + "Select Generate([*NATIVE_CJ_SET], {([Gender].CurrentMember, [Address2].CurrentMember)}) on columns "
            + "From [SalesTest] where ([City].[Redwood City])";

        String result =
            "Axis #0:\n"
            + "{[City].[City].[Redwood City]}\n"
            + "Axis #1:\n"
            + "{[Gender].[Gender].[F], [Address2].[Address2].[#null]}\n"
            + "{[Gender].[Gender].[F], [Address2].[Address2].[#2]}\n"
            + "{[Gender].[Gender].[F], [Address2].[Address2].[Unit H103]}\n"
            + "{[Gender].[Gender].[M], [Address2].[Address2].[#null]}\n"
            + "{[Gender].[Gender].[M], [Address2].[Address2].[#208]}\n"
            + "Row #0: 71\n"
            + "Row #0: 10\n"
            + "Row #0: 3\n"
            + "Row #0: 52\n"
            + "Row #0: 8\n";

        /*
         * Make sure ExpandNonNative is not set. Otherwise, the query is
         * evaluated natively. For the given data set(which contains NULL
         * members), native evaluation produces results in a different order
         * from the non-native evaluation.
         */
        ((TestContextImpl)context).setExpandNonNative(false);
        class TestCellLookupModifier extends PojoMappingModifier {

        	private static MeasureMappingImpl m = SumMeasureMappingImpl.builder()
            .withName("Unit Sales")
            .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
            .withFormatString("Standard")
            .build();

            public TestCellLookupModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected  List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                result.add(PhysicalCubeMappingImpl.builder()
                    .withName("SalesTest")
                    .withDefaultMeasure(m)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                    		.withOverrideDimensionName("City")
                    		.withDimension(
                    			StandardDimensionMappingImpl.builder()
                    				.withName("City")
                    				.withHierarchies(List.of(
                                            ExplicitHierarchyMappingImpl.builder()
                                            .withHasAll(true)
                                            .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                                            .withLevels(List.of(
                                                LevelMappingImpl.builder()
                                                    .withName("city")
                                                    .withColumn(FoodmartMappingSupplier.CITY_COLUMN_IN_CUSTOMER)
                                                    .withUniqueMembers(true)
                                                    .build()
                                            )).build()
                    						))
                    				.build())
                    		.build(),
                        DimensionConnectorMappingImpl.builder()
                    		.withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                    		.withOverrideDimensionName("Gender")
                    		.withDimension(
                    			StandardDimensionMappingImpl.builder()
                    				.withName("Gender")
                    				.withHierarchies(List.of(
                                            ExplicitHierarchyMappingImpl.builder()
                                            .withHasAll(true)
                                            .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                                            .withLevels(List.of(
                                                LevelMappingImpl.builder()
                                                    .withName("gender")
                                                    .withColumn(FoodmartMappingSupplier.GENDER_COLUMN_IN_CUSTOMER)
                                                    .withUniqueMembers(true)
                                                    .build()
                                            )).build()
                    						))
                    				.build())
                    		.build(),
                         DimensionConnectorMappingImpl.builder()
                         	.withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                         	.withOverrideDimensionName("Address2")
                         	.withDimension(
                         		StandardDimensionMappingImpl.builder()
                         			.withName("Address2")
                         			.withHierarchies(List.of(
                         					ExplicitHierarchyMappingImpl.builder()
                         					.withHasAll(true)
                         					.withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                         					.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                         					.withLevels(List.of(
                         						LevelMappingImpl.builder()
                         							.withName("addr")
                         							.withColumn(FoodmartMappingSupplier.ADDRESS2_COLUMN_IN_CUSTOMER)
                         							.withUniqueMembers(true)
                         							.build()
                                        )).build()
                						))
                				.build())
                		.build()
                		)
                    	)
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                    		.withMeasures(List.of(m))
                    		.build()))
                    .build());
                return result;
            }
        }
        withSchema(context, TestCellLookupModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(), query, result);
    }

    void testSize() {
        for (int i = 1; i < 20; i++) {
            assertEquals(i, CellKey.Generator.newCellKey(new int[i]).size());
            assertEquals(i, CellKey.Generator.newCellKey(i).size());
        }
    }
}
