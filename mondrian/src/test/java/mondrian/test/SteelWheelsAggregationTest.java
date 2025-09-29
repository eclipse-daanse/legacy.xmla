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
package mondrian.test;

import static org.opencube.junit5.TestUtil.assertQueryReturns;

import java.util.List;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.connection.ConnectionProps;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.steelwheels.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.model.AccessCatalogGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessCubeGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessDimensionGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessHierarchyGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessMemberGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessRole;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.CatalogAccess;
import org.eclipse.daanse.rolap.mapping.model.ColumnInternalDataType;
import org.eclipse.daanse.rolap.mapping.model.CubeAccess;
import org.eclipse.daanse.rolap.mapping.model.DimensionAccess;
import org.eclipse.daanse.rolap.mapping.model.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.HideMemberIf;
import org.eclipse.daanse.rolap.mapping.model.HierarchyAccess;
import org.eclipse.daanse.rolap.mapping.model.Level;
import org.eclipse.daanse.rolap.mapping.model.LevelDefinition;
import org.eclipse.daanse.rolap.mapping.model.MeasureGroup;
import org.eclipse.daanse.rolap.mapping.model.MemberAccess;
import org.eclipse.daanse.rolap.mapping.model.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.RollupPolicy;
import org.eclipse.daanse.rolap.mapping.model.StandardDimension;
import org.eclipse.daanse.rolap.mapping.model.SumMeasure;
import org.eclipse.daanse.rolap.mapping.model.TableQuery;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.SteelWheelsDataLoader;
import org.opencube.junit5.propupdator.AppandSteelWheelsCatalog;

/**
 * @author Andrey Khayrutdinov
 */
class SteelWheelsAggregationTest {

    private static final String QUERY = ""
            + "WITH\n"
            + "SET [*NATIVE_CJ_SET_WITH_SLICER] AS 'FILTER([*BASE_MEMBERS__Customer_DimUsage.Customers Hierarchy_], NOT ISEMPTY ([Measures].[Price Each]))'\n"
            + "SET [*NATIVE_CJ_SET] AS '[*NATIVE_CJ_SET_WITH_SLICER]'\n"
            + "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],[Customer_DimUsage].[Customers Hierarchy].CURRENTMEMBER.ORDERKEY,"
            + "BASC,ANCESTOR([Customer_DimUsage].[Customers Hierarchy].CURRENTMEMBER,[Customer_DimUsage].[Customers Hierarchy].[Address]).ORDERKEY,BASC)'\n"
            + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[Price Each]}'\n"
            + "SET [*BASE_MEMBERS__Customer_DimUsage.Customers Hierarchy_] AS '[Customer_DimUsage].[Customers Hierarchy].[Name].MEMBERS'\n"
            + "SET [*CJ_ROW_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Customer_DimUsage].[Customers Hierarchy].CURRENTMEMBER)})'\n"
            + "SELECT\n"
            + "[*BASE_MEMBERS__Measures_] ON COLUMNS\n"
            + ",[*SORTED_ROW_AXIS] ON ROWS\n"
            + "FROM [Customers Cube]\n";

    private static final String EXPECTED = ""
            + "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Price Each]}\n"
            + "Axis #2:\n"
            + "{[Customer_DimUsage].[Customers Hierarchy].[1 rue Alsace-Lorraine].[Roulet]}\n"
            + "Row #0: 1,701.95\n";

    private static final TableQuery customerQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
    private static final Level nameLevel = RolapMappingFactory.eINSTANCE.createLevel();
    private static final Level addressLevel = RolapMappingFactory.eINSTANCE.createLevel();
    private static final ExplicitHierarchy customersHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
    private static final StandardDimension customersDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
    private static final TableQuery tq = RolapMappingFactory.eINSTANCE.createTableQuery();
    private static final DimensionConnector dcCustomer = RolapMappingFactory.eINSTANCE.createDimensionConnector();
    private static final SumMeasure measurePriceEach = RolapMappingFactory.eINSTANCE.createSumMeasure();
    private static final SumMeasure measureTotalPrice = RolapMappingFactory.eINSTANCE.createSumMeasure();
    private static final MeasureGroup mg = RolapMappingFactory.eINSTANCE.createMeasureGroup();
    private static final PhysicalCube customersCube = RolapMappingFactory.eINSTANCE.createPhysicalCube();

    static {
    customerQuery.setTable(CatalogSupplier.TABLE_CUSTOMER);


    nameLevel.setName("Name");
    nameLevel.setVisible(true);
    nameLevel.setColumn(CatalogSupplier.COLUMN_CONTACTLASTNAME_CUSTOMER);
    nameLevel.setColumnType(ColumnInternalDataType.STRING);
    nameLevel.setUniqueMembers(false);
    nameLevel.setType(LevelDefinition.REGULAR);
    nameLevel.setHideMemberIf(HideMemberIf.NEVER);
            //.withCaption("Contact Last Name")


    addressLevel.setName("Address");
    addressLevel.setVisible(true);
    addressLevel.setColumn(CatalogSupplier.COLUMN_ADDRESSLINE1_CUSTOMER);
    addressLevel.setColumnType(ColumnInternalDataType.STRING);
    addressLevel.setUniqueMembers(false);
    addressLevel.setType(LevelDefinition.REGULAR);
    addressLevel.setHideMemberIf(HideMemberIf.NEVER);
    //.withCaption("Address Line 1")



    customersHierarchy.setName("Customers Hierarchy");
    customersHierarchy.setVisible(true);
    customersHierarchy.setHasAll(true);
    customersHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_CUSTOMERNUMBER_CUSTOMER);
    //.withCaption("Customer Hierarchy")
    customersHierarchy.setQuery(customerQuery);
    customersHierarchy.getLevels().addAll(List.of(
                addressLevel,
                nameLevel
            ));


    customersDimension.setVisible(true);
    customersDimension.setName("Customers Dimension");
    customersDimension.getHierarchies().add(customersHierarchy);

    tq.setTable(CatalogSupplier.TABLE_ORDERFACT);

    dcCustomer.setDimension(customersDimension);
    dcCustomer.setOverrideDimensionName("Customer_DimUsage");
    dcCustomer.setVisible(true);
    dcCustomer.setForeignKey(CatalogSupplier.COLUMN_CUSTOMERNUMBER_ORDERFACT);

    measurePriceEach.setName("Price Each");
    measurePriceEach.setColumn(CatalogSupplier.COLUMN_PRICEEACH_ORDERFACT);
    measurePriceEach.setVisible(true);

    measureTotalPrice.setName("Total Price");
    measureTotalPrice.setColumn(CatalogSupplier.COLUMN_TOTALPRICE_ORDERFACT);
    measureTotalPrice.setVisible(true);

    mg.getMeasures().add(measurePriceEach);
    mg.getMeasures().add(measureTotalPrice);

    customersCube.setName("Customers Cube");
    customersCube.setVisible(true);
    customersCube.setCache(true);
    customersCube.setEnabled(true);
    customersCube.setQuery(tq);
    customersCube.getDimensionConnectors().add(dcCustomer);
    customersCube.getMeasureGroups().add(mg);
    }

    @BeforeEach
    public void beforeEach() {
        //propertySaver.set(propertySaver.properties.UseAggregates, true);
        //propertySaver.set(propertySaver.properties.ReadAggregates, true);
    }

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }



    private Catalog getSchemaWith(List<AccessRole> roles) {


        Catalog catalog = RolapMappingFactory.eINSTANCE.createCatalog();
        catalog.setName("SteelWheels");
        catalog.setDescription("1 admin role, 1 user role. For testing MemberGrant with caching in 5.1.2");
        catalog.getCubes().add(customersCube);
        catalog.getAccessRoles().addAll(roles);
        catalog.getDbschemas().add(CatalogSupplier.DATABASE_SCHEMA_STEELWHEELS);
    	return catalog;
    }

    @Disabled //disabled for CI build
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandSteelWheelsCatalog.class, dataloader = SteelWheelsDataLoader.class)
    void testWithAggregation(Context<?> context) throws Exception {
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);

        AccessDimensionGrant dimensionGrant = RolapMappingFactory.eINSTANCE.createAccessDimensionGrant();
        //.withDimension("Measures")
        dimensionGrant.setDimensionAccess(DimensionAccess.ALL);

        AccessMemberGrant memberGrant1 = RolapMappingFactory.eINSTANCE.createAccessMemberGrant();
        memberGrant1.setMember("[Customer_DimUsage.Customers Hierarchy].[1 rue Alsace-Lorraine]");
        memberGrant1.setMemberAccess(MemberAccess.NONE);

        AccessMemberGrant memberGrant2 = RolapMappingFactory.eINSTANCE.createAccessMemberGrant();
        memberGrant2.setMember("[Customer_DimUsage.Customers Hierarchy].[1 rue Alsace-Lorraine].[Roulet]");
        memberGrant2.setMemberAccess(MemberAccess.ALL);

        AccessHierarchyGrant hierarchyGrant = RolapMappingFactory.eINSTANCE.createAccessHierarchyGrant();
        hierarchyGrant.setHierarchy(customersHierarchy);
        hierarchyGrant.setTopLevel(nameLevel);
        hierarchyGrant.setRollupPolicy(RollupPolicy.PARTIAL);
        hierarchyGrant.setHierarchyAccess(HierarchyAccess.CUSTOM);
        hierarchyGrant.getMemberGrants().add(memberGrant1);
        hierarchyGrant.getMemberGrants().add(memberGrant2);

        AccessCubeGrant accessCubeGrant = RolapMappingFactory.eINSTANCE.createAccessCubeGrant();
        accessCubeGrant.setCube(customersCube);
        accessCubeGrant.setCubeAccess(CubeAccess.ALL);
        accessCubeGrant.getDimensionGrants().add(dimensionGrant);
        accessCubeGrant.getHierarchyGrants().add(hierarchyGrant);

        final AccessCatalogGrant accessCatalogGrant = RolapMappingFactory.eINSTANCE.createAccessCatalogGrant();
        accessCatalogGrant.setCatalogAccess(CatalogAccess.NONE);
        accessCatalogGrant.getCubeGrants().add(accessCubeGrant);

        final AccessRole powerUserRole = RolapMappingFactory.eINSTANCE.createAccessRole();
        powerUserRole.setName("Power User");
        powerUserRole.getAccessCatalogGrants().add(accessCatalogGrant);

        final Catalog schema = getSchemaWith(
                List.of(powerUserRole));

        context.getCatalogCache().clear();
        ((TestContext)context).setCatalogMappingSupplier(new MyCatalogSuplier(schema));
        assertQueryReturns(((TestContext)context).getConnection(new ConnectionProps(List.of("Power User"))), QUERY, EXPECTED);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandSteelWheelsCatalog.class, dataloader = SteelWheelsDataLoader.class)
    void testWithAggregationNoRestrictionsOnTopLevel(Context<?> context) throws Exception {
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);

        AccessDimensionGrant dimensionGrant = RolapMappingFactory.eINSTANCE.createAccessDimensionGrant();
        //.withDimension("Measures")
        dimensionGrant.setDimensionAccess(DimensionAccess.ALL);

        AccessMemberGrant memberGrant = RolapMappingFactory.eINSTANCE.createAccessMemberGrant();
        memberGrant.setMember("[Customer_DimUsage].[Customers Hierarchy].[1 rue Alsace-Lorraine]");
        memberGrant.setMemberAccess(MemberAccess.ALL);

        AccessHierarchyGrant hierarchyGrant = RolapMappingFactory.eINSTANCE.createAccessHierarchyGrant();
        hierarchyGrant.setHierarchy(customersHierarchy);
        hierarchyGrant.setTopLevel(nameLevel);
        hierarchyGrant.setRollupPolicy(RollupPolicy.PARTIAL);
        hierarchyGrant.setHierarchyAccess(HierarchyAccess.CUSTOM);
        hierarchyGrant.getMemberGrants().add(memberGrant);

        AccessCubeGrant accessCubeGrant = RolapMappingFactory.eINSTANCE.createAccessCubeGrant();
        accessCubeGrant.setCube(customersCube);
        accessCubeGrant.setCubeAccess(CubeAccess.ALL);
        accessCubeGrant.getDimensionGrants().add(dimensionGrant);
        accessCubeGrant.getHierarchyGrants().add(hierarchyGrant);

        final AccessCatalogGrant accessCatalogGrant = RolapMappingFactory.eINSTANCE.createAccessCatalogGrant();
        accessCatalogGrant.setCatalogAccess(CatalogAccess.NONE);
        accessCatalogGrant.getCubeGrants().add(accessCubeGrant);

        final AccessRole powerUserRole = RolapMappingFactory.eINSTANCE.createAccessRole();
        powerUserRole.setName("Power User");
        powerUserRole.getAccessCatalogGrants().add(accessCatalogGrant);

        final Catalog schema = getSchemaWith(
                List.of(powerUserRole));
        context.getCatalogCache().clear();
        ((TestContext)context).setCatalogMappingSupplier(new MyCatalogSuplier(schema));
        assertQueryReturns(((TestContext)context).getConnection(new ConnectionProps(List.of("Power User"))), QUERY, EXPECTED);
    }

    @Disabled //disabled for CI build
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandSteelWheelsCatalog.class, dataloader = SteelWheelsDataLoader.class)
    void testUnionWithAggregation(Context<?> context) throws Exception {
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);

        AccessDimensionGrant dimensionGrant = RolapMappingFactory.eINSTANCE.createAccessDimensionGrant();
        //.withDimension("Measures")
        dimensionGrant.setDimensionAccess(DimensionAccess.ALL);

        AccessMemberGrant memberGrant = RolapMappingFactory.eINSTANCE.createAccessMemberGrant();
        memberGrant.setMember("[Customer_DimUsage.Customers Hierarchy].[1 rue Alsace-Lorraine].[Roulet]");
        memberGrant.setMemberAccess(MemberAccess.NONE);

        AccessHierarchyGrant hierarchyGrant = RolapMappingFactory.eINSTANCE.createAccessHierarchyGrant();
        hierarchyGrant.setHierarchy(customersHierarchy);
        hierarchyGrant.setTopLevel(nameLevel);
        hierarchyGrant.setRollupPolicy(RollupPolicy.PARTIAL);
        hierarchyGrant.setHierarchyAccess(HierarchyAccess.CUSTOM);
        hierarchyGrant.getMemberGrants().add(memberGrant);

        AccessCubeGrant accessCubeGrant = RolapMappingFactory.eINSTANCE.createAccessCubeGrant();
        accessCubeGrant.setCube(customersCube);
        accessCubeGrant.setCubeAccess(CubeAccess.ALL);
        accessCubeGrant.getDimensionGrants().add(dimensionGrant);
        accessCubeGrant.getHierarchyGrants().add(hierarchyGrant);

        final AccessCatalogGrant accessCatalogGrant = RolapMappingFactory.eINSTANCE.createAccessCatalogGrant();
        accessCatalogGrant.setCatalogAccess(CatalogAccess.NONE);
        accessCatalogGrant.getCubeGrants().add(accessCubeGrant);

        final AccessCatalogGrant accessCatalogGrant1 = RolapMappingFactory.eINSTANCE.createAccessCatalogGrant();
        accessCatalogGrant1.setCatalogAccess(CatalogAccess.NONE);

        final AccessRole fooRole = RolapMappingFactory.eINSTANCE.createAccessRole();
        fooRole.setName("Foo");
        fooRole.getAccessCatalogGrants().add(accessCatalogGrant1);

        final AccessRole powerUserRole = RolapMappingFactory.eINSTANCE.createAccessRole();
        powerUserRole.setName("Power User");
        powerUserRole.getAccessCatalogGrants().add(accessCatalogGrant);

        final AccessRole powerUserUnionRole = RolapMappingFactory.eINSTANCE.createAccessRole();
        powerUserRole.setName("Power User Union");
        powerUserRole.getReferencedAccessRoles().add(powerUserUnionRole);
        powerUserRole.getReferencedAccessRoles().add(fooRole);


        final Catalog schema = getSchemaWith(List.of(fooRole, powerUserRole, powerUserUnionRole));
        context.getCatalogCache().clear();
        ((TestContext)context).setCatalogMappingSupplier(new MyCatalogSuplier(schema));
        assertQueryReturns(((TestContext)context).getConnection(new ConnectionProps(List.of("Power User Union"))), QUERY, EXPECTED);
    }

    @Disabled //disabled for CI build
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandSteelWheelsCatalog.class, dataloader = SteelWheelsDataLoader.class)
    void testWithAggregationUnionRolesWithSameGrants(Context<?> context) throws Exception {
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);

        AccessDimensionGrant dimensionGrant = RolapMappingFactory.eINSTANCE.createAccessDimensionGrant();
        //.withDimension("Measures")
        dimensionGrant.setDimensionAccess(DimensionAccess.ALL);

        AccessDimensionGrant dimensionGrant1 = RolapMappingFactory.eINSTANCE.createAccessDimensionGrant();
        //.withDimension("Measures")
        dimensionGrant1.setDimensionAccess(DimensionAccess.ALL);

        AccessMemberGrant memberGrant = RolapMappingFactory.eINSTANCE.createAccessMemberGrant();
        memberGrant.setMember("[Customer_DimUsage.Customers Hierarchy].[1 rue Alsace-Lorraine].[Roulet]");
        memberGrant.setMemberAccess(MemberAccess.NONE);

        AccessMemberGrant memberGrant1 = RolapMappingFactory.eINSTANCE.createAccessMemberGrant();
        memberGrant1.setMember("[Customer_DimUsage.Customers Hierarchy].[1 rue Alsace-Lorraine].[Roulet]");
        memberGrant1.setMemberAccess(MemberAccess.ALL);

        AccessHierarchyGrant hierarchyGrant = RolapMappingFactory.eINSTANCE.createAccessHierarchyGrant();
        hierarchyGrant.setHierarchy(customersHierarchy);
        hierarchyGrant.setTopLevel(nameLevel);
        hierarchyGrant.setRollupPolicy(RollupPolicy.PARTIAL);
        hierarchyGrant.setHierarchyAccess(HierarchyAccess.CUSTOM);
        hierarchyGrant.getMemberGrants().add(memberGrant);

        AccessHierarchyGrant hierarchyGrant1 = RolapMappingFactory.eINSTANCE.createAccessHierarchyGrant();
        hierarchyGrant1.setHierarchy(customersHierarchy);
        hierarchyGrant1.setTopLevel(nameLevel);
        hierarchyGrant1.setRollupPolicy(RollupPolicy.PARTIAL);
        hierarchyGrant1.setHierarchyAccess(HierarchyAccess.CUSTOM);
        hierarchyGrant1.getMemberGrants().add(memberGrant1);

        AccessCubeGrant accessCubeGrant = RolapMappingFactory.eINSTANCE.createAccessCubeGrant();
        accessCubeGrant.setCube(customersCube);
        accessCubeGrant.setCubeAccess(CubeAccess.ALL);
        accessCubeGrant.getDimensionGrants().add(dimensionGrant);
        accessCubeGrant.getHierarchyGrants().add(hierarchyGrant);

        final AccessCatalogGrant accessCatalogGrant = RolapMappingFactory.eINSTANCE.createAccessCatalogGrant();
        accessCatalogGrant.setCatalogAccess(CatalogAccess.NONE);
        accessCatalogGrant.getCubeGrants().add(accessCubeGrant);

        AccessCubeGrant accessCubeGrant1 = RolapMappingFactory.eINSTANCE.createAccessCubeGrant();
        accessCubeGrant1.setCube(customersCube);
        accessCubeGrant1.setCubeAccess(CubeAccess.ALL);
        accessCubeGrant1.getDimensionGrants().add(dimensionGrant1);
        accessCubeGrant1.getHierarchyGrants().add(hierarchyGrant1);

        final AccessCatalogGrant accessCatalogGrant1 = RolapMappingFactory.eINSTANCE.createAccessCatalogGrant();
        accessCatalogGrant1.setCatalogAccess(CatalogAccess.NONE);
        accessCatalogGrant1.getCubeGrants().add(accessCubeGrant1);

        final AccessRole fooRole = RolapMappingFactory.eINSTANCE.createAccessRole();
        fooRole.setName("Foo");
        fooRole.getAccessCatalogGrants().add(accessCatalogGrant1);

        final AccessRole powerUserRole = RolapMappingFactory.eINSTANCE.createAccessRole();
        powerUserRole.setName("Power User");
        powerUserRole.getAccessCatalogGrants().add(accessCatalogGrant);

        final AccessRole powerUserUnionRole = RolapMappingFactory.eINSTANCE.createAccessRole();
        powerUserRole.setName("Power User Union");
        powerUserRole.getReferencedAccessRoles().add(powerUserUnionRole);
        powerUserRole.getReferencedAccessRoles().add(fooRole);

        final Catalog schema = getSchemaWith
            (List.of(
           		fooRole,
                powerUserRole,
                powerUserUnionRole));
        context.getCatalogCache().clear();
        ((TestContext)context).setCatalogMappingSupplier(new MyCatalogSuplier(schema));
        assertQueryReturns(((TestContext)context).getConnection(new ConnectionProps(List.of("Power User Union"))), QUERY, EXPECTED);
    }

    private static class MyCatalogSuplier implements CatalogMappingSupplier {

        private Catalog catalog;

        public MyCatalogSuplier(Catalog catalog) {
            this.catalog = catalog;
        }

        @Override
        public Catalog get() {
            return catalog;
        }

    }
}
