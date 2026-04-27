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
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.isDefaultNullMemberRepresentation;
import static org.opencube.junit5.TestUtil.withSchemaEmf;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.olap.key.CellKey;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.model.catalog.Catalog;
import org.eclipse.daanse.rolap.mapping.model.catalog.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.database.source.SourceFactory;
import org.eclipse.daanse.rolap.mapping.model.database.source.TableSource;
import org.eclipse.daanse.rolap.mapping.model.olap.cube.CubeFactory;
import org.eclipse.daanse.rolap.mapping.model.olap.cube.MeasureGroup;
import org.eclipse.daanse.rolap.mapping.model.olap.cube.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.olap.cube.measure.MeasureFactory;
import org.eclipse.daanse.rolap.mapping.model.olap.cube.measure.SumMeasure;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.DimensionFactory;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.StandardDimension;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.hierarchy.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.hierarchy.HierarchyFactory;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.hierarchy.level.Level;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.hierarchy.level.LevelFactory;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
        /**
         * EMF version of TestCellLookupModifier
         * Creates SalesTest cube with City, Gender, and Address2 dimensions
         */
        class TestCellLookupModifierEmf implements CatalogMappingSupplier {

            private CatalogImpl catalog;

            public TestCellLookupModifierEmf(Catalog cat) {
                // Copy catalog using EcoreUtil
                catalog = org.opencube.junit5.EmfUtil.copy((CatalogImpl) cat);

                // Create measure "Unit Sales" using RolapMappingFactory
                SumMeasure measure =
                    MeasureFactory.eINSTANCE.createSumMeasure();
                measure.setName("Unit Sales");
                measure.setColumn(CatalogSupplier.COLUMN_UNIT_SALES_SALESFACT);
                measure.setFormatString("Standard");

                // Create level "city" for City dimension
                Level cityLevel =
                    LevelFactory.eINSTANCE.createLevel();
                cityLevel.setName("city");
                cityLevel.setColumn(CatalogSupplier.COLUMN_CITY_CUSTOMER);
                cityLevel.setUniqueMembers(true);

                TableSource customerQuery = SourceFactory.eINSTANCE.createTableSource();
                customerQuery.setTable(CatalogSupplier.TABLE_CUSTOMER);

                // Create hierarchy for City dimension
                ExplicitHierarchy cityHierarchy =
                    HierarchyFactory.eINSTANCE.createExplicitHierarchy();
                cityHierarchy.setHasAll(true);
                cityHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_CUSTOMER_ID_CUSTOMER);
                cityHierarchy.setQuery(customerQuery);
                cityHierarchy.getLevels().add(cityLevel);

                // Create City dimension
                StandardDimension cityDimension =
                    DimensionFactory.eINSTANCE.createStandardDimension();
                cityDimension.setName("City");
                cityDimension.getHierarchies().add(cityHierarchy);

                // Create dimension connector for City
                DimensionConnector cityConnector =
                    DimensionFactory.eINSTANCE.createDimensionConnector();
                cityConnector.setForeignKey(CatalogSupplier.COLUMN_CUSTOMER_ID_SALESFACT);
                cityConnector.setOverrideDimensionName("City");
                cityConnector.setDimension(cityDimension);

                // Create level "gender" for Gender dimension
                Level genderLevel =
                    LevelFactory.eINSTANCE.createLevel();
                genderLevel.setName("gender");
                genderLevel.setColumn(CatalogSupplier.COLUMN_GENDER_CUSTOMER);
                genderLevel.setUniqueMembers(true);

                TableSource customerQuery1 = SourceFactory.eINSTANCE.createTableSource();
                customerQuery1.setTable(CatalogSupplier.TABLE_CUSTOMER);

                // Create hierarchy for Gender dimension
                ExplicitHierarchy genderHierarchy =
                    HierarchyFactory.eINSTANCE.createExplicitHierarchy();
                genderHierarchy.setHasAll(true);
                genderHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_CUSTOMER_ID_CUSTOMER);
                genderHierarchy.setQuery(customerQuery1);
                genderHierarchy.getLevels().add(genderLevel);

                // Create Gender dimension
                StandardDimension genderDimension =
                    DimensionFactory.eINSTANCE.createStandardDimension();
                genderDimension.setName("Gender");
                genderDimension.getHierarchies().add(genderHierarchy);

                // Create dimension connector for Gender
                DimensionConnector genderConnector =
                    DimensionFactory.eINSTANCE.createDimensionConnector();
                genderConnector.setForeignKey(CatalogSupplier.COLUMN_CUSTOMER_ID_SALESFACT);
                genderConnector.setOverrideDimensionName("Gender");
                genderConnector.setDimension(genderDimension);

                // Create level "addr" for Address2 dimension
                Level addrLevel =
                    LevelFactory.eINSTANCE.createLevel();
                addrLevel.setName("addr");
                addrLevel.setColumn(CatalogSupplier.COLUMN_ADDRESS2_CUSTOMER);
                addrLevel.setUniqueMembers(true);

                TableSource customerQuery2 = SourceFactory.eINSTANCE.createTableSource();
                customerQuery2.setTable(CatalogSupplier.TABLE_CUSTOMER);

                // Create hierarchy for Address2 dimension
                ExplicitHierarchy addrHierarchy =
                    HierarchyFactory.eINSTANCE.createExplicitHierarchy();
                addrHierarchy.setHasAll(true);
                addrHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_CUSTOMER_ID_CUSTOMER);
                addrHierarchy.setQuery(customerQuery2);
                addrHierarchy.getLevels().add(addrLevel);

                // Create Address2 dimension
                StandardDimension addrDimension =
                    DimensionFactory.eINSTANCE.createStandardDimension();
                addrDimension.setName("Address2");
                addrDimension.getHierarchies().add(addrHierarchy);

                // Create dimension connector for Address2
                DimensionConnector addrConnector =
                    DimensionFactory.eINSTANCE.createDimensionConnector();
                addrConnector.setForeignKey(CatalogSupplier.COLUMN_CUSTOMER_ID_SALESFACT);
                addrConnector.setOverrideDimensionName("Address2");
                addrConnector.setDimension(addrDimension);

                // Create measure group
                MeasureGroup measureGroup =
                    CubeFactory.eINSTANCE.createMeasureGroup();
                measureGroup.getMeasures().add(measure);

                TableSource cubeQuery = SourceFactory.eINSTANCE.createTableSource();
                cubeQuery.setTable(CatalogSupplier.TABLE_SALES_FACT);

                // Create SalesTest cube
                PhysicalCube salesTestCube =
                    CubeFactory.eINSTANCE.createPhysicalCube();
                salesTestCube.setName("SalesTest");
                salesTestCube.setDefaultMeasure(measure);
                salesTestCube.setQuery(cubeQuery);
                salesTestCube.getDimensionConnectors().add(cityConnector);
                salesTestCube.getDimensionConnectors().add(genderConnector);
                salesTestCube.getDimensionConnectors().add(addrConnector);
                salesTestCube.getMeasureGroups().add(measureGroup);

                catalog.getCubes().add(salesTestCube);
            }

            @Override
            public Catalog get() {
                return catalog;
            }
        }
        /*
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
        */
        withSchemaEmf(context, TestCellLookupModifierEmf::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(), query, result);
    }

    void testSize() {
        for (int i = 1; i < 20; i++) {
            assertEquals(i, CellKey.Generator.newCellKey(new int[i]).size());
            assertEquals(i, CellKey.Generator.newCellKey(i).size());
        }
    }
}
