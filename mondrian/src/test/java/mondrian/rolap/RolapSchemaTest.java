/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2015-2017 Hitachi Vantara..  All rights reserved.
*/
package mondrian.rolap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.SchemaReader;
import org.eclipse.daanse.olap.api.access.Access;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.rdb.structure.pojo.PhysicalTableImpl;
import org.eclipse.daanse.rdb.structure.pojo.PhysicalTableImpl.Builder;
import org.eclipse.daanse.rolap.mapping.api.model.AccessCubeGrantMapping;
import org.eclipse.daanse.rolap.mapping.api.model.AccessHierarchyGrantMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.HierarchyMapping;
import org.eclipse.daanse.rolap.mapping.api.model.RelationalQueryMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessCube;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessDimension;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessHierarchy;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessMember;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessSchema;
import org.eclipse.daanse.rolap.mapping.api.model.enums.RollupPolicyType;
import org.eclipse.daanse.rolap.mapping.pojo.AccessCubeGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessDimensionGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessHierarchyGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessMemberGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessRoleMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessSchemaGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SQLMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencube.junit5.context.TestConfig;

import mondrian.olap.MondrianException;
import mondrian.olap.RoleImpl;
import mondrian.olap.SystemWideProperties;
import mondrian.olap.exceptions.RoleUnionGrantsException;
import mondrian.olap.exceptions.UnknownRoleException;
import mondrian.rolap.RolapSchema.RolapStarRegistry;
import mondrian.rolap.agg.AggregationManager;
import mondrian.rolap.agg.SegmentCacheManager;
import mondrian.rolap.util.RelationUtil;


/**
 * @author Andrey Khayrutdinov
 */
class RolapSchemaTest {
  private RolapSchema schemaSpy;
  private static RolapStar rlStarMock = mock(RolapStar.class);


    @BeforeEach
    public void beforeEach() {

        schemaSpy = spy(createSchema());
    }

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

    private RolapSchema createSchema() {
        SchemaKey key = new SchemaKey(
            mock(SchemaContentKey.class), mock(ConnectionKey.class));

        //noinspection deprecation
        //mock rolap connection to eliminate calls for cache loading
        Context contextMock = mock(Context.class);
        RolapConnection rolapConnectionMock = mock(RolapConnection.class);
        AggregationManager aggManagerMock = mock(AggregationManager.class);
        SegmentCacheManager scManagerMock = mock(SegmentCacheManager.class);
        when(rolapConnectionMock.getContext()).thenReturn(contextMock);
        when(contextMock.getAggregationManager()).thenReturn(aggManagerMock);
        when(contextMock.getConfig()).thenReturn(new TestConfig());
        when(aggManagerMock.getCacheMgr(rolapConnectionMock)).thenReturn(scManagerMock);
        return new RolapSchema(key,  rolapConnectionMock, contextMock);
    }

    private SchemaReader mockSchemaReader(DataType category, OlapElement element) {
        SchemaReader reader = mock(SchemaReader.class);
        when(reader.withLocus()).thenReturn(reader);
        when(reader.lookupCompound(
            any(OlapElement.class), anyList(),
            anyBoolean(), eq(category)))
            .thenReturn(element);
        return reader;
    }

    private RolapCube mockCube(RolapSchema schema) {
        RolapCube cube = mock(RolapCube.class);
        when(cube.getSchema()).thenReturn(schema);
        return cube;
    }

    @Test
    void testCreateUnionRole_ThrowsException_WhenSchemaGrantsExist() {
    	AccessRoleMappingImpl role = AccessRoleMappingImpl.builder()
    			.withAccessSchemaGrants(List.of(
    					AccessSchemaGrantMappingImpl.builder().build()))
    			.withReferencedAccessRoles(List.of())
    			.build();

        try {
            createSchema().createUnionRole(role);
        } catch (MondrianException ex) {
            assertMondrianException(
                new RoleUnionGrantsException(), ex);
            return;
        }
        fail("Should fail if union and schema grants exist simultaneously");
    }

    @Test
    void testCreateUnionRole_ThrowsException_WhenRoleNameIsUnknown() {
        final String roleName = "non-existing role name";
        AccessRoleMappingImpl usage = AccessRoleMappingImpl.builder()
        		.withName(roleName)
        		.build();       

        AccessRoleMappingImpl role = AccessRoleMappingImpl.builder().build();
        role.setReferencedAccessRoles(List.of(usage));

        try {
            createSchema().createUnionRole(role);
        } catch (MondrianException ex) {
            assertMondrianException(
                new UnknownRoleException(roleName), ex);
            return;
        }
        fail("Should fail if union and schema grants exist simultaneously");
    }


    @Test
    void testHandleSchemaGrant() {
        RolapSchema schema = createSchema();
        schema = spy(schema);
        doNothing().when(schema)
            .handleCubeGrant(
                any(RoleImpl.class), any( AccessCubeGrantMapping.class));        

        AccessSchemaGrantMappingImpl grant = AccessSchemaGrantMappingImpl.builder()
        		.withAccess(AccessSchema.CUSTOM)
        		.withCubeGrant(null)
        		.build();
        
        grant.setCubeGrant(List.of(AccessCubeGrantMappingImpl.builder().build(), AccessCubeGrantMappingImpl.builder().build()));

        mondrian.olap.RoleImpl role = new mondrian.olap.RoleImpl();

        schema.handleSchemaGrant(role, grant);
        assertEquals(Access.CUSTOM, role.getAccess(schema));
        verify(schema, times(2))
            .handleCubeGrant(eq(role), any(AccessCubeGrantMapping.class));
    }


    @Test
    void testHandleCubeGrant_ThrowsException_WhenCubeIsUnknown() {
        RolapSchema schema = createSchema();
        schema = spy(schema);
        doReturn(null).when(schema).lookupCube(anyString());

        AccessCubeGrantMappingImpl grant = AccessCubeGrantMappingImpl.builder().build();        
        grant.setCube(PhysicalCubeMappingImpl.builder().withName("cube").build());

        try {
            schema.handleCubeGrant(new mondrian.olap.RoleImpl(), grant);
        } catch (MondrianException e) {
            String message = e.getMessage();
            assertTrue(message.contains(grant.getCube().getName()), message);
            return;
        }
        fail("Should fail if cube is unknown");
    }

    @Test
    void testHandleCubeGrant_GrantsCubeDimensionsAndHierarchies() {
        RolapSchema schema = createSchema();
        schema = spy(schema);
        doNothing().when(schema)
            .handleHierarchyGrant(
                any(mondrian.olap.RoleImpl.class),
                any(RolapCube.class),
                any(SchemaReader.class),
                any(AccessHierarchyGrantMappingImpl.class));

        final Dimension dimension = mock(Dimension.class);
        SchemaReader reader = mockSchemaReader(org.eclipse.daanse.olap.api.DataType.DIMENSION, dimension);

        RolapCube cube = mockCube(schema);
        when(cube.getSchemaReader(any())).thenReturn(reader);
        doReturn(cube).when(schema).lookupCube(any(CubeMapping.class));

        AccessDimensionGrantMappingImpl dimensionGrant =
        		AccessDimensionGrantMappingImpl.builder().build();
        dimensionGrant.setDimension(StandardDimensionMappingImpl.builder().withName("dimension").build());
        dimensionGrant.setAccess(AccessDimension.NONE);

        AccessCubeGrantMappingImpl grant = AccessCubeGrantMappingImpl.builder().build();
        grant.setCube(PhysicalCubeMappingImpl.builder().withName("cube").build());
        grant.setAccess(AccessCube.CUSTOM);
        grant.getDimensionGrants().addAll(List.of(dimensionGrant));
        grant.getHierarchyGrants().addAll(List.of(AccessHierarchyGrantMappingImpl.builder().build()));

        mondrian.olap.RoleImpl role = new mondrian.olap.RoleImpl();

        schema.handleCubeGrant(role, grant);

        assertEquals(Access.CUSTOM, role.getAccess(cube));
        assertEquals(Access.NONE, role.getAccess(dimension));
        verify(schema, times(1))
            .handleHierarchyGrant(
                eq(role),
                eq(cube),
                eq(reader),
                any(AccessHierarchyGrantMapping.class));
    }

    @Test
    void testHandleHierarchyGrant_ValidMembers() {
        doTestHandleHierarchyGrant(Access.CUSTOM, Access.ALL);
    }

    @Test
    void testHandleHierarchyGrant_NoValidMembers() {
        doTestHandleHierarchyGrant(Access.NONE, null);
    }

    @Test
    void testEmptyRolapStarRegistryCreatedForTheNewSchema()
        throws Exception {
      RolapSchema schema = createSchema();
      RolapStarRegistry rolapStarRegistry = schema.getRolapStarRegistry();
      assertNotNull(rolapStarRegistry);
      assertTrue(rolapStarRegistry.getStars().isEmpty());
    }

    @Test
    void testGetOrCreateStar_StarCreatedAndUsed()
        throws Exception {
      //Create the test fact
      RelationalQueryMapping fact = TableQueryMappingImpl.builder()
      		  .withTable(((Builder) PhysicalTableImpl.builder().withName("getFactTable())")).build())
      		  .withAlias("TableAlias")
      		  .withSqlWhereExpression(SQLMappingImpl.builder()
      				  .withDialects(List.of("mysql"))
      				  .withStatement("`TableAlias`.`promotion_id` = 112")
      				  .build())
      		  .build();
      List<String> rolapStarKey = RolapUtil.makeRolapStarKey(fact);
      //Expected result star
      RolapStar expectedStar = rlStarMock;
      RolapStarRegistry rolapStarRegistry =
          getStarRegistryLinkedToRolapSchemaSpy(schemaSpy, fact);


      //Test that a new rolap star has created and put to the registry
      RolapStar actualStar = rolapStarRegistry.getOrCreateStar(fact);
      assertSame(expectedStar, actualStar);
      assertEquals(1, rolapStarRegistry.getStars().size());
      assertEquals(expectedStar, rolapStarRegistry.getStar(rolapStarKey));
      verify(schemaSpy, times(1)).makeRolapStar(fact);
      //test that no new rolap star has created,
      //but extracted already existing one from the registry
      RolapStar actualStar2 = rolapStarRegistry.getOrCreateStar(fact);
      verify(schemaSpy, times(1)).makeRolapStar(fact);
      assertSame(expectedStar, actualStar2);
      assertEquals(1, rolapStarRegistry.getStars().size());
      assertEquals(expectedStar, rolapStarRegistry.getStar(rolapStarKey));
    }

    @Test
    void testGetStarFromRegistryByStarKey() throws Exception {
      //Create the test fact
      RelationalQueryMapping fact = TableQueryMappingImpl.builder()
    		  .withTable(((Builder) PhysicalTableImpl.builder().withName("getFactTable())")).build())
    		  .withAlias("TableAlias")
    		  .withSqlWhereExpression(SQLMappingImpl.builder()
    				  .withDialects(List.of("mysql"))
    				  .withStatement("`TableAlias`.`promotion_id` = 112")
    				  .build())
    		  .build();
      List<String> rolapStarKey = RolapUtil.makeRolapStarKey(fact);
      //Expected result star
      RolapStarRegistry rolapStarRegistry =
          getStarRegistryLinkedToRolapSchemaSpy(schemaSpy, fact);
      //Put rolap star to the registry
      rolapStarRegistry.getOrCreateStar(fact);

      RolapStar actualStar = schemaSpy.getStar(rolapStarKey);
      assertSame(rlStarMock, actualStar);
    }

    @Test
    void testGetStarFromRegistryByFactTableName() throws Exception {
      //Create the test fact
      RelationalQueryMapping fact = TableQueryMappingImpl.builder()
    		  .withTable(((Builder) PhysicalTableImpl.builder().withName("getFactTable())")).build())
    		  .withAlias("TableAlias")
    		  .build();

      //Expected result star
      RolapStarRegistry rolapStarRegistry =
          getStarRegistryLinkedToRolapSchemaSpy(schemaSpy, fact);
      //Put rolap star to the registry
      rolapStarRegistry.getOrCreateStar(fact);

      RolapStar actualStar = schemaSpy.getStar(RelationUtil.getAlias(fact));
      assertSame(rlStarMock, actualStar);
    }

    private static RolapStarRegistry getStarRegistryLinkedToRolapSchemaSpy(
        RolapSchema schemaSpy, RelationalQueryMapping fact) throws Exception
    {
      //the rolap star registry is linked to the origin rolap schema,
      //not to the schemaSpy
      RolapStarRegistry rolapStarRegistry = schemaSpy.getRolapStarRegistry();
      //the star mock
      doReturn(rlStarMock).when(schemaSpy).makeRolapStar(fact);
      //Set the schema spy to be linked with the rolap star registry
      assertTrue(
              replaceRolapSchemaLinkedToStarRegistry(
              rolapStarRegistry,
              schemaSpy),
              "For testing purpose object this$0 in the inner class "
                      + "should be replaced to the rolap schema spy "
                      + "but this not happend");
      verify(schemaSpy, times(0)).makeRolapStar(fact);
      return rolapStarRegistry;
    }

     private static boolean replaceRolapSchemaLinkedToStarRegistry(
         RolapStarRegistry innerClass,
         RolapSchema sSpy) throws Exception
     {
       Field field = innerClass.getClass().getDeclaredField("this$0");
       if (field != null) {
         field.setAccessible(true);
         field.set(innerClass, sSpy);
         RolapSchema outerMocked = (RolapSchema) field.get(innerClass);
         return outerMocked == sSpy;
       }
       return false;
      }

    private static String getFactTableWithSQLFilter() {
      String fact =
          "<Table name=\"sales_fact_1997\" alias=\"TableAlias\">\n"
          + " <SQL dialect=\"mysql\">\n"
          + "     `TableAlias`.`promotion_id` = 112\n"
          + " </SQL>\n"
          + "</Table>";
      return fact;
    }

    private static String getFactTable() {
      String fact =
          "<Table name=\"sales_fact_1997\" alias=\"TableAlias\"/>";
      return fact;
    }


    private void doTestHandleHierarchyGrant(
        Access expectedHierarchyAccess,
        Access expectedMemberAccess)
    {
        RolapSchema schema = createSchema();
        RolapCube cube = mockCube(schema);
        mondrian.olap.RoleImpl role = new mondrian.olap.RoleImpl();

        AccessMemberGrantMappingImpl memberGrant = AccessMemberGrantMappingImpl.builder().withMember("member").withAccess(AccessMember.ALL).build();

        HierarchyMappingImpl h = HierarchyMappingImpl.builder().build(); 
        AccessHierarchyGrantMappingImpl grant = AccessHierarchyGrantMappingImpl.builder().build();
        grant.setAccess(AccessHierarchy.CUSTOM);
        grant.setRollupPolicyType(RollupPolicyType.FULL);
        grant.setHierarchy(h);
        grant.setMemberGrants(List.of(memberGrant));

        Level level = mock(Level.class);
        Hierarchy hierarchy = mock(Hierarchy.class);
        when(hierarchy.getLevels()).thenReturn(new Level[]{level});
        when(level.getHierarchy()).thenReturn(hierarchy);
        when(cube.lookupHierarchy(any(HierarchyMapping.class))).thenReturn(hierarchy);
        Dimension dimension = mock(Dimension.class);
        when(hierarchy.getDimension()).thenReturn(dimension);

        SchemaReader reader = mockSchemaReader(DataType.HIERARCHY, hierarchy);
        Context context = mock(Context.class);
        TestConfig config = new TestConfig();
        config.setIgnoreInvalidMembers(true);
        when(context.getConfig()).thenReturn(config);
        when(reader.getContext()).thenReturn(context);


        Member member = mock(Member.class);
        when(member.getHierarchy()).thenReturn(hierarchy);
        when(member.getLevel()).thenReturn(level);

        if (expectedMemberAccess != null) {
            when(reader.getMemberByUniqueName(
                anyList(), anyBoolean())).thenReturn(member);
        }

        schema.handleHierarchyGrant(role, cube, reader, grant);
        assertEquals(expectedHierarchyAccess, role.getAccess(hierarchy));
        if (expectedMemberAccess != null) {
            assertEquals(expectedMemberAccess, role.getAccess(member));
        }
    }

    private void assertMondrianException(
        Exception expected,
        Exception actual)
    {
        assertEquals(expected.getMessage(), actual.getMessage());
    }
}
