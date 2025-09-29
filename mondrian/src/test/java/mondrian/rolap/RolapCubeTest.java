/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2001-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.rolap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencube.junit5.TestUtil.cubeByName;
import static org.opencube.junit5.TestUtil.getDimensionWithName;
import static org.opencube.junit5.TestUtil.withSchemaEmf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Segment;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.connection.ConnectionProps;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.query.component.IdImpl;
import org.eclipse.daanse.rolap.element.RolapCube;
import org.eclipse.daanse.rolap.element.RolapVirtualCube;
import org.eclipse.daanse.rolap.mapping.model.CalculatedMember;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

/**
 * Unit test for {@link RolapCube}.
 *
 * @author mkambol
 * @since 25 January, 2007
 */
class RolapCubeTest {

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testProcessFormatStringAttributeToIgnoreNullFormatString(Context<?> context) {
        RolapCube cube =
            (RolapCube) context.getConnectionWithDefaultRole().getCatalog().lookupCube("Sales").orElseThrow();
        StringBuilder builder = new StringBuilder();
        cube.processFormatStringAttribute(
                RolapMappingFactory.eINSTANCE.createCalculatedMember(), builder);
        assertEquals(0, builder.length());
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testProcessFormatStringAttribute(Context<?> context) {
        RolapCube cube =
            (RolapCube) context.getConnectionWithDefaultRole().getCatalog().lookupCube("Sales").orElseThrow();
        StringBuilder builder = new StringBuilder();
        CalculatedMember xmlCalcMember =
                RolapMappingFactory.eINSTANCE.createCalculatedMember();
        String format = "FORMAT";
        xmlCalcMember.setFormatString(format);
        cube.processFormatStringAttribute(xmlCalcMember, builder);
        assertEquals(
            "," + Util.NL + "FORMAT_STRING = \"" + format + "\"",
            builder.toString());
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGetCalculatedMembersWithNoRole(Context<?> context) {
        String[] expectedCalculatedMembers = {
            "[Measures].[Profit]",
            "[Measures].[Average Warehouse Sale]",
            "[Measures].[Profit Growth]",
            "[Measures].[Profit Per Unit Shipped]"
        };
        Connection connection = context.getConnectionWithDefaultRole();
        try {
            Cube warehouseAndSalesCube =
                cubeByName(connection, "Warehouse and Sales");
            CatalogReader schemaReader =
                warehouseAndSalesCube.getCatalogReader(null);

            List<Member> calculatedMembers =
                schemaReader.getCalculatedMembers();
            assertEquals(
                expectedCalculatedMembers.length,
                calculatedMembers.size());
            assertCalculatedMemberExists(
                expectedCalculatedMembers,
                calculatedMembers);
        } finally {
            connection.close();
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGetCalculatedMembersForCaliforniaManager(Context<?> context) {
    	context.getCatalogCache().clear();
        String[] expectedCalculatedMembers = new String[] {
            "[Measures].[Profit]", "[Measures].[Profit last Period]",
            "[Measures].[Profit Growth]"
        };
        Connection connection = ((TestContext)context).getConnection(new ConnectionProps(List.of("California manager")));

        try {
            Cube salesCube = cubeByName(connection, "Sales");
            CatalogReader schemaReader = salesCube
                .getCatalogReader(connection.getRole());

            List<Member> calculatedMembers =
                schemaReader.getCalculatedMembers();
            assertEquals(
                expectedCalculatedMembers.length,
                calculatedMembers.size());
            assertCalculatedMemberExists(
                expectedCalculatedMembers,
                calculatedMembers);
        } finally {
            connection.close();
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGetCalculatedMembersReturnsOnlyAccessibleMembers(Context<?> context) {
        String[] expectedCalculatedMembers = {
            "[Measures].[Profit]",
            "[Measures].[Profit last Period]",
            "[Measures].[Profit Growth]",
            "[Product].[Product].[~Missing]"
        };


        createTestContextWithAdditionalMembersAndARole(context);
        Connection connection = ((TestContext)context).getConnection(new ConnectionProps(List.of("California manager")));

        try {
            Cube salesCube = cubeByName(connection, "Sales");
            CatalogReader schemaReader =
                salesCube.getCatalogReader(connection.getRole());
            List<Member> calculatedMembers =
                schemaReader.getCalculatedMembers();
            assertEquals(
                expectedCalculatedMembers.length,
                calculatedMembers.size());
            assertCalculatedMemberExists(
                expectedCalculatedMembers,
                calculatedMembers);
        } finally {
            connection.close();
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGetCalculatedMembersReturnsOnlyAccessibleMembersForHierarchy(Context<?> context)
    {
        String[] expectedCalculatedMembersFromProduct = {
            "[Product].[Product].[~Missing]"
        };
        //TestContext<?> testContext<?> =
        //    createTestContextWithAdditionalMembersAndARole();
        createTestContextWithAdditionalMembersAndARole(context);
        Connection connection = ((TestContext)context).getConnection(new ConnectionProps(List.of("California manager")));

        try {
            Cube salesCube = cubeByName(connection, "Sales");
            CatalogReader schemaReader =
                salesCube.getCatalogReader(connection.getRole());

            // Product.~Missing accessible
            List<Member> calculatedMembers =
                schemaReader.getCalculatedMembers(
                    getDimensionWithName(
                        "Product",
                        salesCube.getDimensions()).getHierarchy());

            assertEquals(
                expectedCalculatedMembersFromProduct.length,
                calculatedMembers.size());

            assertCalculatedMemberExists(
                expectedCalculatedMembersFromProduct,
                calculatedMembers);

            // Gender.~Missing not accessible
            calculatedMembers =
                schemaReader.getCalculatedMembers(
                    getDimensionWithName(
                        "Gender",
                        salesCube.getDimensions()).getHierarchy());
            assertEquals(0, calculatedMembers.size());
        } finally {
            connection.close();
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGetCalculatedMembersReturnsOnlyAccessibleMembersForLevel(Context<?> context) {
        String[] expectedCalculatedMembersFromProduct = new String[]{
            "[Product].[Product].[~Missing]"
        };


        createTestContextWithAdditionalMembersAndARole(context);
        Connection connection = ((TestContext)context).getConnection(new ConnectionProps(List.of("California manager")));

        try {
            Cube salesCube = cubeByName(connection, "Sales");
            CatalogReader schemaReader =
                salesCube.getCatalogReader(connection.getRole());

            // Product.~Missing accessible
            List<Member> calculatedMembers =
                schemaReader.getCalculatedMembers(
                    getDimensionWithName(
                        "Product",
                        salesCube.getDimensions())
                    .getHierarchy().getLevels().getFirst());

            assertEquals(
                expectedCalculatedMembersFromProduct.length,
                calculatedMembers.size());
            assertCalculatedMemberExists(
                expectedCalculatedMembersFromProduct,
                calculatedMembers);

            // Gender.~Missing not accessible
            calculatedMembers =
                schemaReader.getCalculatedMembers(
                    getDimensionWithName(
                        "Gender",
                        salesCube.getDimensions())
                    .getHierarchy().getLevels().getFirst());
            assertEquals(0, calculatedMembers.size());
        } finally {
            connection.close();
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonJoiningDimensions(Context<?> context) {

        Connection connection = context.getConnectionWithDefaultRole();

        try {
            RolapCube salesCube = (RolapCube) cubeByName(connection, "Sales");

            RolapCube warehouseAndSalesCube =
                (RolapCube) cubeByName(connection, "Warehouse and Sales");
            CatalogReader readerWarehouseAndSales =
                warehouseAndSalesCube.getCatalogReader().withLocus();

            List<Member> members = new ArrayList<>();
            List<Member> warehouseMembers =
                warehouseMembersCanadaMexicoUsa(readerWarehouseAndSales);
            Dimension warehouseDim = warehouseMembers.get(0).getDimension();
            members.addAll(warehouseMembers);

            List<Member> storeMembers =
                storeMembersCAAndOR(readerWarehouseAndSales).slice(0);
            Dimension storeDim = storeMembers.get(0).getDimension();
            members.addAll(storeMembers);

            Set<Dimension> nonJoiningDims =
                salesCube.nonJoiningDimensions(members.toArray(new Member[0]));
            assertFalse(nonJoiningDims.contains(storeDim));
            assertTrue(nonJoiningDims.contains(warehouseDim));
        } finally {
            connection.close();
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRolapCubeDimensionEquality(Context<?> context) {


        Connection connection1 = context.getConnectionWithDefaultRole();
        //withSchema(context, null);
        Connection connection2 = context.getConnectionWithDefaultRole();

        try {
            RolapCube salesCube1 = (RolapCube) cubeByName(connection1, "Sales");
            CatalogReader readerSales1 =
                salesCube1.getCatalogReader().withLocus();
            List<Member> storeMembersSales =
                storeMembersCAAndOR(readerSales1).slice(0);
            Dimension storeDim1 = storeMembersSales.get(0).getDimension();
            assertEquals(storeDim1, storeDim1);

            RolapCube salesCube2 = (RolapCube) cubeByName(connection2, "Sales");
            CatalogReader readerSales2 =
                salesCube2.getCatalogReader().withLocus();
            List<Member> storeMembersSales2 =
                storeMembersCAAndOR(readerSales2).slice(0);
            Dimension storeDim2 = storeMembersSales2.get(0).getDimension();
            assertEquals(storeDim1, storeDim2);


            RolapCube warehouseAndSalesCube =
                (RolapCube) cubeByName(connection1, "Warehouse and Sales");
            CatalogReader readerWarehouseAndSales =
                warehouseAndSalesCube.getCatalogReader().withLocus();
            List<Member> storeMembersWarehouseAndSales =
                storeMembersCAAndOR(readerWarehouseAndSales).slice(0);
            Dimension storeDim3 =
                storeMembersWarehouseAndSales.get(0).getDimension();
            assertNotEquals(storeDim1, storeDim3);

            List<Member> warehouseMembers =
                warehouseMembersCanadaMexicoUsa(readerWarehouseAndSales);
            Dimension warehouseDim = warehouseMembers.get(0).getDimension();
            assertNotEquals(storeDim3, warehouseDim);
        } finally {
            connection1.close();
            connection2.close();
        }
    }

    void createTestContextWithAdditionalMembersAndARole(Context<?> context) {
    	withSchemaEmf(context, SchemaModifiersEmf.RolapCubeTestModifier1::new);
    }

    private void assertCalculatedMemberExists(
        String[] expectedCalculatedMembers,
        List<Member> calculatedMembers)
    {
        List expectedCalculatedMemberNames =
            Arrays.asList(expectedCalculatedMembers);
        for (Member calculatedMember : calculatedMembers) {
            String calculatedMemberName = calculatedMember.getUniqueName();
            assertTrue(expectedCalculatedMemberNames.contains(calculatedMemberName),
                    "Calculated member name not found: " + calculatedMemberName);
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBasedCubesForVirtualCube(Context<?> context) {
      Connection connection = context.getConnectionWithDefaultRole();
      RolapCube cubeSales =
          (RolapCube) connection.getCatalog().lookupCube("Sales").orElseThrow();
      RolapCube cubeWarehouse =
          (RolapCube) connection.getCatalog().lookupCube(
              "Warehouse").orElseThrow();
      RolapCube cube =
          (RolapCube) connection.getCatalog().lookupCube(
              "Warehouse and Sales").orElseThrow();
      assertNotNull(cube);
      assertNotNull(cubeSales);
      assertNotNull(cubeWarehouse);
      assertEquals(true, cube instanceof RolapVirtualCube);
      List<RolapCube> baseCubes = cube.getBaseCubes();
      assertNotNull(baseCubes);
      assertEquals(2, baseCubes.size());
      assertSame(cubeSales, baseCubes.get(0));
      assertEquals(cubeWarehouse, baseCubes.get(1));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBasedCubesForNotVirtualCubeIsThisCube(Context<?> context) {
      RolapCube cubeSales =
          (RolapCube) context.getConnectionWithDefaultRole().getCatalog().lookupCube("Sales").orElseThrow();
      assertNotNull(cubeSales);
      assertEquals(false, cubeSales instanceof RolapVirtualCube);
      List<RolapCube> baseCubes = cubeSales.getBaseCubes();
      assertNotNull(baseCubes);
      assertEquals(1, baseCubes.size());
      assertSame(cubeSales, baseCubes.get(0));
    }

    private List<Member> warehouseMembersCanadaMexicoUsa(CatalogReader reader)
    {
        return Arrays.asList(
                member(IdImpl.toList(
                        "Warehouse", "All Warehouses", "Canada"), reader),
                member(IdImpl.toList(
                        "Warehouse", "All Warehouses", "Mexico"), reader),
                member(IdImpl.toList(
                        "Warehouse", "All Warehouses", "USA"), reader));
    }

    private Member member(
            List<Segment> segmentList,
            CatalogReader salesCubeCatalogReader)
    {
        return salesCubeCatalogReader.getMemberByUniqueName(segmentList, true);
    }

    private TupleList storeMembersCAAndOR(
            CatalogReader salesCubeCatalogReader)
    {
        return new UnaryTupleList(Arrays.asList(
                member(
                		IdImpl.toList(
                                "Store", "All Stores", "USA", "CA", "Alameda"),
                        salesCubeCatalogReader),
                member(
                		IdImpl.toList(
                                "Store", "All Stores", "USA", "CA", "Alameda", "HQ"),
                        salesCubeCatalogReader),
                member(
                		IdImpl.toList(
                                "Store", "All Stores", "USA", "CA", "Beverly Hills"),
                        salesCubeCatalogReader),
                member(
                		IdImpl.toList(
                                "Store", "All Stores", "USA", "CA", "Beverly Hills",
                                "Store 6"),
                        salesCubeCatalogReader),
                member(
                		IdImpl.toList(
                                "Store", "All Stores", "USA", "CA", "Los Angeles"),
                        salesCubeCatalogReader),
                member(
                		IdImpl.toList(
                                "Store", "All Stores", "USA", "OR", "Portland"),
                        salesCubeCatalogReader),
                member(
                		IdImpl.toList(
                                "Store", "All Stores", "USA", "OR", "Portland", "Store 11"),
                        salesCubeCatalogReader),
                member(
                		IdImpl.toList(
                                "Store", "All Stores", "USA", "OR", "Salem"),
                        salesCubeCatalogReader),
                member(
                		IdImpl.toList(
                                "Store", "All Stores", "USA", "OR", "Salem", "Store 13"),
                        salesCubeCatalogReader)));
    }
}
