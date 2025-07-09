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
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessCube;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessDimension;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessHierarchy;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessMember;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessCatalog;
import org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.HideMemberIfType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.LevelType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.RollupPolicyType;
import org.eclipse.daanse.rolap.mapping.instance.rec.complex.steelwheels.SteelwheelsSupplier;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.AccessCubeGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessDimensionGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessHierarchyGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessMemberGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessRoleMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessCatalogGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.CatalogMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.ExplicitHierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SumMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.context.TestContext;
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

    private static final LevelMappingImpl nameLevel = LevelMappingImpl.builder()
    .withName("Name")
    .withVisible(true)
    .withColumn(SteelwheelsSupplier.CONTACTLASTNAME_COLUMN_IN_CUSTOMER_W_TER)
    .withType(InternalDataType.STRING)
    .withUniqueMembers(false)
    .withLevelType(LevelType.REGULAR)
    .withHideMemberIfType(HideMemberIfType.NEVER)
    //.withCaption("Contact Last Name")
    .build();

    private static final LevelMappingImpl addressLevel = LevelMappingImpl.builder()
    .withName("Address")
    .withVisible(true)
    .withColumn(SteelwheelsSupplier.ADDRESSLINE1_COLUMN_IN_CUSTOMER_W_TER)
    .withType(InternalDataType.STRING)
    .withUniqueMembers(false)
    .withLevelType(LevelType.REGULAR)
    .withHideMemberIfType(HideMemberIfType.NEVER)
    //.withCaption("Address Line 1")
    .build();

    private static final HierarchyMappingImpl customersHierarchy = ExplicitHierarchyMappingImpl.builder()
    .withName("Customers Hierarchy")
    .withVisible(true)
    .withHasAll(true)
    .withPrimaryKey(SteelwheelsSupplier.CUSTOMERNUMBER_COLUMN_IN_CUSTOMER_W_TER)
    //.withCaption("Customer Hierarchy")
    .withQuery(TableQueryMappingImpl.builder().withTable(SteelwheelsSupplier.CUSTOMER_W_TER_TABLE).build())
    .withLevels(List.of(
    	addressLevel,
        nameLevel
    ))
    .build();

    private static final StandardDimensionMappingImpl customersDimension = StandardDimensionMappingImpl.builder()
            .withVisible(true)
            .withName("Customers Dimension")
            .withHierarchies(List.of(customersHierarchy))
            .build();

    private static final PhysicalCubeMappingImpl customersCube = PhysicalCubeMappingImpl.builder()
    .withName("Customers Cube")
    .withVisible(true)
    .withCache(true)
    .withEnabled(true)
    .withQuery(TableQueryMappingImpl.builder().withTable(SteelwheelsSupplier.ORDER_FACT_TABLE).build())
    .withDimensionConnectors(List.of(
    	DimensionConnectorMappingImpl.builder()
    		.withDimension(customersDimension)
    		.withOverrideDimensionName("Customer_DimUsage")
            .withVisible(true)
            .withForeignKey(SteelwheelsSupplier.CUSTOMERNUMBER_COLUMN_IN_ORDER_FACT)
            .build()
    ))
    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
           SumMeasureMappingImpl.builder()
            .withName("Price Each")
            .withColumn(SteelwheelsSupplier.PRICEEACH_COLUMN_IN_ORDER_FACT)
            .withVisible(true)
            .build(),
        SumMeasureMappingImpl.builder()
            .withName("Total Price")
            .withColumn(SteelwheelsSupplier.TOTALPRICE_COLUMN_IN_ORDER_FACT)
            .withVisible(true)
            .build()

    )).build()))
    .build();

    @BeforeEach
    public void beforeEach() {
        //propertySaver.set(propertySaver.properties.UseAggregates, true);
        //propertySaver.set(propertySaver.properties.ReadAggregates, true);
    }

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }



    private CatalogMapping getSchemaWith(List<AccessRoleMappingImpl> roles) {

    	return
    					CatalogMappingImpl.builder()
    		            .withName("SteelWheels")
    		            .withDescription("1 admin role, 1 user role. For testing MemberGrant with caching in 5.1.2")
    		            .withCubes(List.of(customersCube))
    		            .withAccessRoles(roles)
    		            .withDbSchemas(List.of(SteelwheelsSupplier.DATABASE_SCHEMA))
    		            .build();
    }

    @Disabled //disabled for CI build
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandSteelWheelsCatalog.class, dataloader = SteelWheelsDataLoader.class)
    void testWithAggregation(Context<?> context) throws Exception {
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);

        final CatalogMapping schema = getSchemaWith(
                List.of(AccessRoleMappingImpl.builder()
                        .withName("Power User")
                        .withAccessCatalogGrants(List.of(
                        	AccessCatalogGrantMappingImpl.builder()
                                .withAccess(AccessCatalog.NONE)
                                .withCubeGrant(List.of(
                                	AccessCubeGrantMappingImpl.builder()
                                        .withCube(customersCube)
                                        .withAccess(AccessCube.ALL)
                                        .withDimensionGrants(List.of(
                                            AccessDimensionGrantMappingImpl.builder()
                                                //.withDimension("Measures")
                                                .withAccess(AccessDimension.ALL)
                                                .build()
                                        ))
                                        .withHierarchyGrants(List.of(
                                            AccessHierarchyGrantMappingImpl.builder()
                                                .withHierarchy(customersHierarchy)
                                                .withTopLevel(nameLevel)
                                                .withRollupPolicyType(RollupPolicyType.PARTIAL)
                                                .withAccess(AccessHierarchy.CUSTOM)
                                                .withMemberGrants(List.of(
                                                	AccessMemberGrantMappingImpl.builder()
                                                        .withMember("[Customer_DimUsage.Customers Hierarchy].[1 rue Alsace-Lorraine]")
                                                        .withAccess(AccessMember.NONE)
                                                        .build(),
                                                    AccessMemberGrantMappingImpl.builder()
                                                        .withMember("[Customer_DimUsage.Customers Hierarchy].[1 rue Alsace-Lorraine].[Roulet]")
                                                        .withAccess(AccessMember.ALL)
                                                        .build()
                                                ))
                                                .build()
                                        ))
                                        .build()
                                ))
                                .build()
                        ))
                        .build())
                );

        context.getCatalogCache().clear();
        ((TestContext)context).setCatalogMappingSupplier(new PojoMappingModifier(schema));
        assertQueryReturns(((TestContext)context).getConnection(List.of("Power User")), QUERY, EXPECTED);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandSteelWheelsCatalog.class, dataloader = SteelWheelsDataLoader.class)
    void testWithAggregationNoRestrictionsOnTopLevel(Context<?> context) throws Exception {
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        final CatalogMapping schema = getSchemaWith(
                List.of(AccessRoleMappingImpl.builder()
                        .withName("Power User")
                        .withAccessCatalogGrants(List.of(
                        	AccessCatalogGrantMappingImpl.builder()
                                .withAccess(AccessCatalog.NONE)
                                .withCubeGrant(List.of(
                                	AccessCubeGrantMappingImpl.builder()
                                        .withCube(customersCube)
                                        .withAccess(AccessCube.ALL)
                                        .withDimensionGrants(List.of(
                                            AccessDimensionGrantMappingImpl.builder()
                                                //.withDimension("Measures")
                                                .withAccess(AccessDimension.ALL)
                                                .build()
                                        ))
                                        .withHierarchyGrants(List.of(
                                            AccessHierarchyGrantMappingImpl.builder()
                                                .withHierarchy(customersHierarchy)
                                                .withTopLevel(nameLevel)
                                                .withRollupPolicyType(RollupPolicyType.PARTIAL)
                                                .withAccess(AccessHierarchy.CUSTOM)
                                                .withMemberGrants(List.of(
                                                	AccessMemberGrantMappingImpl.builder()
                                                        .withMember("[Customer_DimUsage].[Customers Hierarchy].[1 rue Alsace-Lorraine]")
                                                        .withAccess(AccessMember.ALL)
                                                        .build()
                                                ))
                                                .build()
                                        ))
                                        .build()
                                ))
                                .build()
                        ))
                        .build())
        	 );
        context.getCatalogCache().clear();
        ((TestContext)context).setCatalogMappingSupplier(new PojoMappingModifier(schema));
        assertQueryReturns(((TestContext)context).getConnection(List.of("Power User")), QUERY, EXPECTED);
    }

    @Disabled //disabled for CI build
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandSteelWheelsCatalog.class, dataloader = SteelWheelsDataLoader.class)
    void testUnionWithAggregation(Context<?> context) throws Exception {
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        AccessRoleMappingImpl powerUserRole;
        AccessRoleMappingImpl fooRole;
        final CatalogMapping schema = getSchemaWith
           	 (List.of(
           		fooRole = AccessRoleMappingImpl.builder()
           			.withName("Foo")
           			.withAccessCatalogGrants(List.of(
           				AccessCatalogGrantMappingImpl.builder()
           					.withAccess(AccessCatalog.NONE)
                            .build()))
                     .build(),
                powerUserRole = AccessRoleMappingImpl.builder()
                     .withName("Power User")
                     .withAccessCatalogGrants(List.of(
                     	AccessCatalogGrantMappingImpl.builder()
                             .withAccess(AccessCatalog.NONE)
                             .withCubeGrant(List.of(
                             	AccessCubeGrantMappingImpl.builder()
                                     .withCube(customersCube)
                                     .withAccess(AccessCube.ALL)
                                     .withDimensionGrants(List.of(
                                         AccessDimensionGrantMappingImpl.builder()
                                             //.withDimension("Measures")
                                             .withAccess(AccessDimension.ALL)
                                             .build()
                                     ))
                                     .withHierarchyGrants(List.of(
                                         AccessHierarchyGrantMappingImpl.builder()
                                             .withHierarchy(customersHierarchy)
                                             .withTopLevel(nameLevel)
                                             .withRollupPolicyType(RollupPolicyType.PARTIAL)
                                             .withAccess(AccessHierarchy.CUSTOM)
                                             .withMemberGrants(List.of(
                                             	AccessMemberGrantMappingImpl.builder()
                                                     .withMember("[Customer_DimUsage.Customers Hierarchy].[1 rue Alsace-Lorraine].[Roulet]")
                                                     .withAccess(AccessMember.ALL)
                                                     .build()
                                             ))
                                             .build()
                                     ))
                                     .build()
                             ))
                             .build()
                     ))
                     .build(),
                AccessRoleMappingImpl.builder()
                     .withName("Power User Union")
                     .withReferencedAccessRoles(List.of(
                    	powerUserRole,
                    	fooRole
                     ))
                     .build()

           	));
        context.getCatalogCache().clear();
        ((TestContext)context).setCatalogMappingSupplier(new PojoMappingModifier(schema));
        assertQueryReturns(((TestContext)context).getConnection(List.of("Power User Union")), QUERY, EXPECTED);
    }

    @Disabled //disabled for CI build
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandSteelWheelsCatalog.class, dataloader = SteelWheelsDataLoader.class)
    void testWithAggregationUnionRolesWithSameGrants(Context<?> context) throws Exception {
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        AccessRoleMappingImpl powerUserRole;
        AccessRoleMappingImpl fooRole;
        final CatalogMapping schema = getSchemaWith
            (List.of(
           		fooRole = AccessRoleMappingImpl.builder()
           			.withName("Foo")
           			.withAccessCatalogGrants(List.of(
           				AccessCatalogGrantMappingImpl.builder()
           					.withAccess(AccessCatalog.NONE)
           					.withCubeGrant(List.of(
           							AccessCubeGrantMappingImpl.builder()
                                        .withCube(customersCube)
                                        .withAccess(AccessCube.ALL)
                                        .withDimensionGrants(List.of(
                                        	AccessDimensionGrantMappingImpl.builder()
                                                //.withDimension("Measures")
                                                .withAccess(AccessDimension.ALL)
                                                .build()
                                        ))
                                        .withHierarchyGrants(List.of(
                                        	AccessHierarchyGrantMappingImpl.builder()
                                                .withHierarchy(customersHierarchy)
                                                .withTopLevel(nameLevel)
                                                .withRollupPolicyType(RollupPolicyType.PARTIAL)
                                                .withAccess(AccessHierarchy.CUSTOM)
                                                .withMemberGrants(List.of(
                                                	AccessMemberGrantMappingImpl.builder()
                                                        .withMember("[Customer_DimUsage.Customers Hierarchy].[1 rue Alsace-Lorraine].[Roulet]")
                                                        .withAccess(AccessMember.ALL)
                                                        .build()
                                                ))
                                                .build()
                                        ))
                                        .build()
                                ))
                            .build()))
                     .build(),
                powerUserRole = AccessRoleMappingImpl.builder()
                     .withName("Power User")
                     .withAccessCatalogGrants(List.of(
                     	AccessCatalogGrantMappingImpl.builder()
                             .withAccess(AccessCatalog.NONE)
                             .withCubeGrant(List.of(
                             	AccessCubeGrantMappingImpl.builder()
                                     .withCube(customersCube)
                                     .withAccess(AccessCube.ALL)
                                     .withDimensionGrants(List.of(
                                         AccessDimensionGrantMappingImpl.builder()
                                             //.withDimension("Measures")
                                             .withAccess(AccessDimension.ALL)
                                             .build()
                                     ))
                                     .withHierarchyGrants(List.of(
                                         AccessHierarchyGrantMappingImpl.builder()
                                             .withHierarchy(customersHierarchy)
                                             .withTopLevel(nameLevel)
                                             .withRollupPolicyType(RollupPolicyType.PARTIAL)
                                             .withAccess(AccessHierarchy.CUSTOM)
                                             .withMemberGrants(List.of(
                                             	AccessMemberGrantMappingImpl.builder()
                                                     .withMember("[Customer_DimUsage.Customers Hierarchy].[1 rue Alsace-Lorraine].[Roulet]")
                                                     .withAccess(AccessMember.ALL)
                                                     .build()
                                             ))
                                             .build()
                                     ))
                                     .build()
                             ))
                             .build()
                     ))
                     .build(),
                AccessRoleMappingImpl.builder()
                     .withName("Power User Union")
                     .withReferencedAccessRoles(List.of(
                    	powerUserRole,
                    	fooRole
                     ))
                     .build()

           	));
        context.getCatalogCache().clear();
        ((TestContext)context).setCatalogMappingSupplier(new PojoMappingModifier(schema));
        assertQueryReturns(((TestContext)context).getConnection(List.of("Power User Union")), QUERY, EXPECTED);
    }
}
