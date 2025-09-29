/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2004-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/

package mondrian.rolap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.hierarchyName;
import static org.opencube.junit5.TestUtil.withSchemaEmf;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.connection.ConnectionProps;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.rolap.mapping.model.AccessCatalogGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessCubeGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessDimensionGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessHierarchyGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessRole;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.CatalogAccess;
import org.eclipse.daanse.rolap.mapping.model.Cube;
import org.eclipse.daanse.rolap.mapping.model.CubeAccess;
import org.eclipse.daanse.rolap.mapping.model.Dimension;
import org.eclipse.daanse.rolap.mapping.model.DimensionAccess;
import org.eclipse.daanse.rolap.mapping.model.Hierarchy;
import org.eclipse.daanse.rolap.mapping.model.HierarchyAccess;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

/**
 * Unit test for {@link CatalogReader}.
 */
class RolapCatalogReaderTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGetCubesWithNoHrCubes(Context<?> context) {
        String[] expectedCubes = new String[] {
                "Sales", "Warehouse", "Warehouse and Sales", "Store",
                "Sales Ragged", "Sales 2"
        };

        Connection connection =
            ((TestContext)context).getConnection(new ConnectionProps(List.of("No HR Cube")));
        try {
            CatalogReader reader = connection.getCatalogReader().withLocus();

            List<org.eclipse.daanse.olap.api.element.Cube> cubes = reader.getCubes();

            assertEquals(expectedCubes.length, cubes.size());

            assertCubeExists(expectedCubes, cubes);
        } finally {
            connection.close();
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGetCubesWithNoRole(Context<?> context) {
        String[] expectedCubes = new String[] {
                "Sales", "Warehouse", "Warehouse and Sales", "Store",
                "Sales Ragged", "Sales 2", "HR"
        };

        Connection connection = context.getConnectionWithDefaultRole();
        try {
            CatalogReader reader = connection.getCatalogReader().withLocus();

            List<org.eclipse.daanse.olap.api.element.Cube> cubes = reader.getCubes();

            assertEquals(expectedCubes.length, cubes.size());

            assertCubeExists(expectedCubes, cubes);
        } finally {
            connection.close();
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGetCubesForCaliforniaManager(Context<?> context) {
        String[] expectedCubes = new String[] {
                "Sales"
        };

        Connection connection = ((TestContext)context).getConnection(new ConnectionProps(List.of("California manager")));
        try {
            CatalogReader reader = connection.getCatalogReader().withLocus();

            List<org.eclipse.daanse.olap.api.element.Cube> cubes = reader.getCubes();

            assertEquals(expectedCubes.length, cubes.size());

            assertCubeExists(expectedCubes, cubes);
        } finally {
            connection.close();
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testConnectUseContentChecksum(Context<?> context) {
//    	context.setProperty(RolapConnectionProperties.UseContentChecksum.name(), "true");
        //Util.PropertyList properties =
        //       TestUtil.getConnectionProperties().clone();
        // properties.put(
        //    RolapConnectionProperties.UseContentChecksum.name(),
        //    "true");

        try {
        	context.getConnectionWithDefaultRole();
            //DriverManager.getConnection(
            //    properties,
            //    null);
        } catch (OlapRuntimeException e) {
            e.printStackTrace();
            fail("unexpected exception for UseContentChecksum");
        }
    }

    private void assertCubeExists(String[] expectedCubes, List<org.eclipse.daanse.olap.api.element.Cube> cubes) {
        List cubesAsList = Arrays.asList(expectedCubes);

        for (org.eclipse.daanse.olap.api.element.Cube cube : cubes) {
            String cubeName = cube.getName();
            assertTrue(cubesAsList.contains(cubeName), "Cube name not found: " + cubeName);
        }
    }

    /**
     * Test case for {@link CatalogReader#getCubeDimensions(Cube)}
     * and {@link CatalogReader#getDimensionHierarchies(Dimension)}
     * methods.
     *
     * <p>Test case for bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-691">MONDRIAN-691,
     * "RolapCatalogReader is not enforcing access control on two APIs"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGetCubeDimensions(Context<?> context) {
        final String timeWeekly =
            hierarchyName("Time", "Weekly");
        final String timeTime =
            hierarchyName("Time", "Time");
        /*
        class TestGetCubeDimensionsModifier extends PojoMappingModifier {

            public TestGetCubeDimensionsModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends AccessRoleMapping> catalogAccessRoles(CatalogMapping catalogMapping) {
            	List<AccessRoleMapping> result = new ArrayList<>();
                result.addAll(super.catalogAccessRoles(catalogMapping));
                result.add(AccessRoleMappingImpl.builder()
                    .withName("REG1")
                    .withAccessCatalogGrants(List.of(
                    	AccessCatalogGrantMappingImpl.builder()
                            .withAccess(AccessCatalog.NONE)
                            .withCubeGrant(List.of(
                            	AccessCubeGrantMappingImpl.builder()
                                    .withCube((CubeMappingImpl) look(FoodmartMappingSupplier.CUBE_SALES))
                                    .withAccess(AccessCube.ALL)
                                    .withDimensionGrants(List.of(
                                    	AccessDimensionGrantMappingImpl.builder()
                                            .withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_STORE_WITH_QUERY_STORE))
                                            .withAccess(AccessDimension.NONE)
                                            .build()
                                    ))
                                    .withHierarchyGrants(List.of(
                                    	AccessHierarchyGrantMappingImpl.builder()
                                            .withHierarchy((HierarchyMappingImpl) look(FoodmartMappingSupplier.HIERARCHY_TIME1))
                                            .withAccess(AccessHierarchy.NONE)
                                            .build(),
                                        AccessHierarchyGrantMappingImpl.builder()
                                        .withHierarchy((HierarchyMappingImpl) look(FoodmartMappingSupplier.HIERARCHY_TIME2))
                                            .withAccess(AccessHierarchy.ALL)
                                            .build()
                                    ))
                                    .build()
                            ))
                            .build()
                    ))
                    .build());
                return result;
            }
        }
        */
        /**
         * EMF version of TestGetCubeDimensionsModifier
         * Creates access role 'REG1' with dimension and hierarchy grants
         */
        class TestGetCubeDimensionsModifierEmf implements CatalogMappingSupplier {

            private CatalogImpl catalog;

            public TestGetCubeDimensionsModifierEmf(Catalog cat) {
                // Copy catalog using EcoreUtil
                EcoreUtil.Copier copier = org.opencube.junit5.EmfUtil.copier((CatalogImpl) cat);
                catalog = (CatalogImpl) copier.get(cat);


                // Create dimension grant for Store (access = NONE) using RolapMappingFactory
                AccessDimensionGrant dimensionGrant =
                    RolapMappingFactory.eINSTANCE.createAccessDimensionGrant();
                dimensionGrant.setDimension((Dimension) copier.get(CatalogSupplier.DIMENSION_STORE));
                dimensionGrant.setDimensionAccess(DimensionAccess.NONE);

                // Create hierarchy grant for Time (access = NONE) using RolapMappingFactory
                AccessHierarchyGrant hierarchyGrant1 =
                    RolapMappingFactory.eINSTANCE.createAccessHierarchyGrant();
                hierarchyGrant1.setHierarchy((Hierarchy) copier.get(CatalogSupplier.HIERARCHY_TIME));
                hierarchyGrant1.setHierarchyAccess(HierarchyAccess.NONE);

                // Create hierarchy grant for Weekly (access = ALL) using RolapMappingFactory
                AccessHierarchyGrant hierarchyGrant2 =
                    RolapMappingFactory.eINSTANCE.createAccessHierarchyGrant();
                hierarchyGrant2.setHierarchy((Hierarchy) copier.get(CatalogSupplier.HIERARCHY_TIME2));
                hierarchyGrant2.setHierarchyAccess(HierarchyAccess.ALL);

                // Create cube grant using RolapMappingFactory
                AccessCubeGrant cubeGrant =
                    RolapMappingFactory.eINSTANCE.createAccessCubeGrant();
                cubeGrant.setCube((Cube) copier.get(CatalogSupplier.CUBE_SALES));
                cubeGrant.setCubeAccess(CubeAccess.ALL);
                cubeGrant.getDimensionGrants().add(dimensionGrant);
                cubeGrant.getHierarchyGrants().add(hierarchyGrant1);
                cubeGrant.getHierarchyGrants().add(hierarchyGrant2);

                // Create catalog grant using RolapMappingFactory
                AccessCatalogGrant catalogGrant =
                    RolapMappingFactory.eINSTANCE.createAccessCatalogGrant();
                catalogGrant.setCatalogAccess(CatalogAccess.NONE);
                catalogGrant.getCubeGrants().add(cubeGrant);

                // Create role using RolapMappingFactory
                AccessRole role =
                    RolapMappingFactory.eINSTANCE.createAccessRole();
                role.setName("REG1");
                role.getAccessCatalogGrants().add(catalogGrant);

                // Add role to catalog
                catalog.getAccessRoles().add(role);
            }

            @Override
            public Catalog get() {
                return catalog;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
                null, null, null, null, null,
                "<Role name=\"REG1\">\n"
                + "  <SchemaGrant access=\"none\">\n"
                + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
                + "      <DimensionGrant dimension=\"Store\" access=\"none\"/>\n"
                + "      <HierarchyGrant hierarchy=\""
                + timeTime
                + "\" access=\"none\"/>\n"
                + "      <HierarchyGrant hierarchy=\""
                + timeWeekly
                + "\" access=\"all\"/>\n"
                + "    </CubeGrant>\n"
                + "  </SchemaGrant>\n"
                + "</Role>");
        withSchema(context, schema);
         */
        withSchemaEmf(context, TestGetCubeDimensionsModifierEmf::new);
        Connection connection = ((TestContext)context).getConnection(new ConnectionProps(List.of("REG1")));
        try {
            CatalogReader reader = connection.getCatalogReader().withLocus();
            final Map<String, org.eclipse.daanse.olap.api.element.Cube> cubes = new HashMap<>();
            for (org.eclipse.daanse.olap.api.element.Cube cube : reader.getCubes()) {
                cubes.put(cube.getName(), cube);
            }
            assertTrue(cubes.containsKey("Sales")); // granted access
            assertFalse(cubes.containsKey("HR")); // denied access
            assertFalse(cubes.containsKey("Bad")); // not exist

            final org.eclipse.daanse.olap.api.element.Cube salesCube = cubes.get("Sales");
            final Map<String, org.eclipse.daanse.olap.api.element.Dimension> dimensions =
                new HashMap<>();
            final Map<String, org.eclipse.daanse.olap.api.element.Hierarchy> hierarchies =
                new HashMap<>();
            for (org.eclipse.daanse.olap.api.element.Dimension dimension : reader.getCubeDimensions(salesCube)) {
                dimensions.put(dimension.getName(), dimension);
                for (org.eclipse.daanse.olap.api.element.Hierarchy hierarchy
                    : reader.getDimensionHierarchies(dimension))
                {
                    hierarchies.put(hierarchy.getUniqueName(), hierarchy);
                }
            }
            assertFalse(dimensions.containsKey("Store")); // denied access
            assertTrue(dimensions.containsKey("Marital Status")); // implicit
            assertTrue(dimensions.containsKey("Time")); // implicit
            assertFalse(dimensions.containsKey("Bad dimension")); // not exist

            assertFalse(hierarchies.containsKey("[Foo]"));
            assertTrue(hierarchies.containsKey("[Product].[Product]"));
            assertTrue(hierarchies.containsKey(timeWeekly));
            assertFalse(hierarchies.containsKey("[Time].[Time]"));
        } finally {
            connection.close();
        }
    }
}
