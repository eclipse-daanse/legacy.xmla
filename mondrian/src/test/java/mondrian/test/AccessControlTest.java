/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2003-2005 Julian Hyde
// Copyright (C) 2005-2018 Hitachi Vantara
// All Rights Reserved.
*/
package mondrian.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.ConnectionProps;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Quoting;
import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.access.AccessCatalog;
import org.eclipse.daanse.olap.api.access.AccessCube;
import org.eclipse.daanse.olap.api.access.AccessDimension;
import org.eclipse.daanse.olap.api.access.AccessHierarchy;
import org.eclipse.daanse.olap.api.access.AccessMember;
import org.eclipse.daanse.olap.api.access.HierarchyAccess;
import org.eclipse.daanse.olap.api.access.Role;
import org.eclipse.daanse.olap.api.access.RollupPolicy;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Position;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.rolap.api.RolapContext;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.RollupPolicyType;
import org.eclipse.daanse.rolap.mapping.instance.rec.complex.foodmart.FoodmartMappingSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.TestConfig;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.DelegatingRole;
import mondrian.olap.IdImpl;
import mondrian.olap.RoleImpl;
import mondrian.olap.SystemWideProperties;
import mondrian.olap.Util;
import mondrian.rolap.RolapConnectionPropsR;
import mondrian.rolap.RolapHierarchy.LimitedRollupMember;
import mondrian.rolap.SchemaModifiers;

/**
 * <code>AccessControlTest</code> is a set of unit-tests for access-control.
 * For these tests, all of the roles are of type RoleImpl.
 *
 * @see Role
 *
 * @author jhyde
 * @since Feb 21, 2003
 */
class AccessControlTest {
    /*
    private static final String BiServer1574Role1 =
        "<Role name=\"role1\">\n"
        + " <SchemaGrant access=\"none\">\n"
        + "  <CubeGrant cube=\"Warehouse\" access=\"all\">\n"
        + "   <HierarchyGrant hierarchy=\"[Store Size in SQFT]\" access=\"custom\" rollupPolicy=\"partial\">\n"
        + "    <MemberGrant member=\"[Store Size in SQFT].[20319]\" access=\"all\"/>\n"
        + "    <MemberGrant member=\"[Store Size in SQFT].[21215]\" access=\"none\"/>\n"
        + "   </HierarchyGrant>\n"
        + "   <HierarchyGrant hierarchy=\"[Store Type]\" access=\"custom\" rollupPolicy=\"partial\">\n"
        + "    <MemberGrant member=\"[Store Type].[Supermarket]\" access=\"all\"/>\n"
        + "   </HierarchyGrant>\n"
        + "  </CubeGrant>\n"
        + " </SchemaGrant>\n"
        + "</Role>";
    */

	@AfterEach
	public void afterEach() {
		SystemWideProperties.instance().populateInitial();
	}

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testCatalogReader(Context foodMartContext) {
        final Connection connection = foodMartContext.getConnectionWithDefaultRole();
        Catalog schema = connection.getCatalog();
        Cube cube = schema.lookupCube("Sales").orElseThrow();
        final CatalogReader schemaReader =
            cube.getCatalogReader(connection.getRole());
        final CatalogReader schemaReader1 = schemaReader.withoutAccessControl();
        assertNotNull(schemaReader1);
        final CatalogReader schemaReader2 = schemaReader1.withoutAccessControl();
        assertNotNull(schemaReader2);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testGrantDimensionNone(Context foodMartContext) {
        final Connection connection = foodMartContext.getConnectionWithDefaultRole();
        RoleImpl role = ((RoleImpl) connection.getRole()).makeMutableClone();
        Catalog schema = connection.getCatalog();
        Cube salesCube = schema.lookupCube("Sales").orElseThrow();
        // todo: add Schema.lookupDimension
        final CatalogReader schemaReader = salesCube.getCatalogReader(role);
        Dimension genderDimension =
            (Dimension) schemaReader.lookupCompound(
                salesCube, IdImpl.toList("Gender"), true,
                DataType.DIMENSION);
        role.grant(genderDimension, AccessDimension.NONE);
        role.makeImmutable();
        connection.setRole(role);
        TestUtil.assertAxisThrows(
    		connection,
            "[Gender].children",
            "MDX object '[Gender]' not found in cube 'Sales'", "Sales");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testRestrictMeasures(Context foodMartContext) {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier31::new);

        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = foodMartContext.getConnectionWithDefaultRole();

        TestUtil.assertQueryReturns(
    		connection,
            "SELECT {[Measures].Members} ON COLUMNS FROM [SALES]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Store Cost]}\n"
            + "{[Measures].[Store Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "{[Measures].[Customer Count]}\n"
            + "{[Measures].[Promotion Sales]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: 225,627.23\n"
            + "Row #0: 565,238.13\n"
            + "Row #0: 86,837\n"
            + "Row #0: 5,581\n"
            + "Row #0: 151,211.21\n");

        props = new RolapConnectionPropsR(List.of("Role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	connection = foodMartContext.getConnection(props);
    	TestUtil.assertQueryReturns(
    		connection,
            "SELECT {[Measures].Members} ON COLUMNS FROM [SALES]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Row #0: 266,773\n");
    }

    /**Test for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-2603">MONDRIAN-2603</a>
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testRestrictMeasuresHierarchy_InTwoRoles(Context foodMartContext) {


        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier32::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Administrator"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
      Connection connection = foodMartContext.getConnection(props);

      try {
	  TestUtil.assertQueryReturns(
		  connection,
          "SELECT {[Measures].Members} ON COLUMNS FROM [Warehouse2]",
          "Axis #0:\n"
          + "{}\n"
          + "Axis #1:\n"
          + "{[Measures].[Measure2_0]}\n"
          + "{[Measures].[Measure2_1]}\n"
          + "{[Measures].[Fact Count]}\n"
          + "Row #0: 89,043.253\n"
          + "Row #0: 196,770.888\n"
          + "Row #0: 4,070\n");
      } catch (OlapRuntimeException e) {
        if (e.getCause().getLocalizedMessage()
            .contains(
                "MDX object '[Measures]' not found in cube 'Warehouse2'"))
        {
          fail(
              "[Measures] should be displayed in 'Warehouse2' cube but they are not! ");
        }
        throw e;
      }

      try {
	  TestUtil.assertQueryReturns(
		  connection,
          "SELECT {[Measures].Members} ON COLUMNS FROM [Warehouse1]",
          "Axis #0:\n"
          + "{}\n"
          + "Axis #1:\n"
          + "{[Measures].[Measure1_0]}\n"
          + "{[Measures].[Measure1_1]}\n"
          + "{[Measures].[Fact Count]}\n"
          + "Row #0: 89,043.253\n"
          + "Row #0: 196,770.888\n"
          + "Row #0: 4,070\n");
      } catch (OlapRuntimeException e) {
        if (e.getCause().getLocalizedMessage()
            .contains(
                "MDX object '[Measures]' not found in cube 'Warehouse1'"))
        {
          fail(
              "[Measures] should be displayed in 'Warehouse1' cube but they are not! ");
        }
        throw e;
      }
  }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testRestrictLevelsAnalyzer3283(Context context) {
        /*
        String dimensionsDef =
            "    <Dimension visible=\"true\" foreignKey=\"customer_id\" highCardinality=\"false\" name=\"Customers\">\n"
            + "      <Hierarchy visible=\"true\" hasAll=\"true\" allMemberName=\"All Customers\" primaryKey=\"customer_id\">\n"
            + "        <Table name=\"customer\">\n"
            + "        </Table>\n"
            + "        <Level name=\"Country\" visible=\"true\" column=\"country\" type=\"String\" uniqueMembers=\"true\" levelType=\"Regular\" hideMemberIf=\"Never\">\n"
            + "        </Level>\n"
            + "        <Level name=\"State Province\" visible=\"true\" column=\"state_province\" type=\"String\" uniqueMembers=\"true\" levelType=\"Regular\" hideMemberIf=\"Never\">\n"
            + "        </Level>\n"
            + "        <Level name=\"City\" visible=\"true\" column=\"city\" type=\"String\" uniqueMembers=\"false\" levelType=\"Regular\" hideMemberIf=\"Never\">\n"
            + "        </Level>\n"
            + "        <Level name=\"Name1\" visible=\"true\" column=\"fname\" type=\"String\" uniqueMembers=\"false\" levelType=\"Regular\" hideMemberIf=\"Never\">\n"
            + "          <Property name=\"Gender\" column=\"gender\" type=\"String\">\n"
            + "          </Property>\n"
            + "          <Property name=\"Marital Status\" column=\"marital_status\" type=\"String\">\n"
            + "          </Property>\n"
            + "          <Property name=\"Education\" column=\"education\" type=\"String\">\n"
            + "          </Property>\n"
            + "          <Property name=\"Yearly Income\" column=\"yearly_income\" type=\"String\">\n"
            + "          </Property>\n"
            + "        </Level>\n"
            + "        <Level name=\"First Name\" visible=\"true\" column=\"fname\" type=\"String\" uniqueMembers=\"false\" levelType=\"Regular\" hideMemberIf=\"Never\">\n"
            + "        </Level>\n"
            + "      </Hierarchy>\n"
            + "      <Hierarchy name=\"Gender\" visible=\"true\" hasAll=\"true\" primaryKey=\"customer_id\">\n"
            + "        <Table name=\"customer\">\n"
            + "        </Table>\n"
            + "        <Level name=\"Gender\" visible=\"true\" column=\"gender\" type=\"String\" uniqueMembers=\"true\" levelType=\"Regular\" hideMemberIf=\"Never\">\n"
            + "          <Annotations>\n"
            + "            <Annotation name=\"AnalyzerBusinessGroup\">\n"
            + "              <![CDATA[Customers]]>\n"
            + "            </Annotation>\n"
            + "          </Annotations>\n"
            + "        </Level>\n"
            + "      </Hierarchy>\n"
            + "      <Hierarchy name=\"Marital Status\" visible=\"true\" hasAll=\"true\" primaryKey=\"customer_id\">\n"
            + "        <Table name=\"customer\">\n"
            + "        </Table>\n"
            + "        <Level name=\"Marital Status\" visible=\"true\" column=\"marital_status\" type=\"String\" uniqueMembers=\"true\" levelType=\"Regular\" hideMemberIf=\"Never\">\n"
            + "          <Annotations>\n"
            + "            <Annotation name=\"AnalyzerBusinessGroup\">\n"
            + "              <![CDATA[Customers]]>\n"
            + "            </Annotation>\n"
            + "          </Annotations>\n"
            + "        </Level>\n"
            + "      </Hierarchy>\n"
            + "    </Dimension>\n"
            + "  <Dimension visible=\"true\" highCardinality=\"false\" name=\"Store\" foreignKey=\"store_id\">\n"
            + "    <Hierarchy visible=\"true\" hasAll=\"true\" primaryKey=\"store_id\">\n"
            + "      <Table name=\"store\">\n"
            + "      </Table>\n"
            + "      <Level name=\"Store ID\" visible=\"true\" column=\"store_id\" type=\"String\" uniqueMembers=\"true\" levelType=\"Regular\" hideMemberIf=\"Never\">\n"
            + "      </Level>\n"
            + "      <Level name=\"Store Country\" visible=\"true\" column=\"store_country\" type=\"String\" uniqueMembers=\"true\" levelType=\"Regular\" hideMemberIf=\"Never\">\n"
            + "      </Level>\n"
            + "      <Level name=\"Store State\" visible=\"true\" column=\"store_state\" type=\"String\" uniqueMembers=\"true\" levelType=\"Regular\" hideMemberIf=\"Never\">\n"
            + "      </Level>\n"
            + "      <Level name=\"Store City\" visible=\"true\" column=\"store_city\" type=\"String\" uniqueMembers=\"false\" levelType=\"Regular\" hideMemberIf=\"Never\">\n"
            + "      </Level>\n"
            + "      <Level name=\"Store Name\" visible=\"true\" column=\"store_name\" type=\"String\" uniqueMembers=\"true\" levelType=\"Regular\" hideMemberIf=\"Never\">\n"
            + "        <Property name=\"Store Type\" column=\"store_type\" type=\"String\">\n"
            + "        </Property>\n"
            + "        <Property name=\"Store Manager\" column=\"store_manager\" type=\"String\">\n"
            + "        </Property>\n"
            + "        <Property name=\"Store Sqft\" column=\"store_sqft\" type=\"Numeric\">\n"
            + "        </Property>\n"
            + "        <Property name=\"Grocery Sqft\" column=\"grocery_sqft\" type=\"Numeric\">\n"
            + "        </Property>\n"
            + "        <Property name=\"Frozen Sqft\" column=\"frozen_sqft\" type=\"Numeric\">\n"
            + "        </Property>\n"
            + "        <Property name=\"Meat Sqft\" column=\"meat_sqft\" type=\"Numeric\">\n"
            + "        </Property>\n"
            + "        <Property name=\"Has coffee bar\" column=\"coffee_bar\" type=\"Boolean\">\n"
            + "        </Property>\n"
            + "        <Property name=\"Street address\" column=\"store_street_address\" type=\"String\">\n"
            + "        </Property>\n"
            + "      </Level>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n";
        String cubeDef = "<Cube name=\"Sales1\">"
          + "  <Table name=\"sales_fact_1997\"/>\n"
          + dimensionsDef
          + "</Cube>";
        String baseSchema = TestUtil.getRawSchema(foodMartContext);
        String schema = SchemaUtil.getSchema(baseSchema,
            null, null, cubeDef, null, null,
            "<Role name=\"MR\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"Sales1\" access=\"all\">\n"
            + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"all\">\n"
            + "      </HierarchyGrant>\n"
            + "      <HierarchyGrant hierarchy=\"[Customers]\" access=\"custom\" topLevel=\"[Customers].[State Province]\" bottomLevel=\"[Customers].[City]\">\n"
            + "\t  </HierarchyGrant>\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>\n"
            + "<Role name=\"DBPentUsers\">\n"
            + "   <SchemaGrant access=\"none\">\n"
            + "   </SchemaGrant>\n"
            + "</Role>");
         */
        TestUtil.withSchema(context, SchemaModifiers.AccessControlTestModifier33::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("MR", "DBPentUsers"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        Connection connection = context.getConnection(props);

        final HierarchyAccess hierarchyAccess =
          getHierarchyAccess(connection, "Sales1", "[Customers]");

        assertEquals(2, hierarchyAccess.getTopLevelDepth());
        assertEquals(3, hierarchyAccess.getBottomLevelDepth());
        context.getCatalogCache().clear();
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testRoleMemberAccessNonExistentMemberFails(Context context) {
        TestUtil.withSchema(context, SchemaModifiers.AccessControlTestModifier34::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        TestUtil.assertQueryThrows(
        	context,
            props,
            "select {[Store].[Store].Children} on 0 from [Sales]",
            "Member '[Store].[Store].[USA].[Non Existent]' not found");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testRoleMemberAccess(Context context) {
    	context.getCatalogCache().clear();
        final Connection connection = getRestrictedConnection(context);
        // because CA has access
        assertMemberAccess(connection, AccessMember.CUSTOM, "[Store].[USA]");
        assertMemberAccess(connection, AccessMember.CUSTOM, "[Store].[Mexico]");
        assertMemberAccess(connection, AccessMember.NONE, "[Store].[Mexico].[DF]");
        assertMemberAccess(
            connection, AccessMember.NONE, "[Store].[Mexico].[DF].[Mexico City]");
        assertMemberAccess(connection, AccessMember.NONE, "[Store].[Canada]");
        assertMemberAccess(
            connection, AccessMember.NONE, "[Store].[Canada].[BC].[Vancouver]");
        assertMemberAccess(
            connection, AccessMember.ALL, "[Store].[USA].[CA].[Los Angeles]");
        assertMemberAccess(
            connection, AccessMember.NONE, "[Store].[USA].[CA].[San Diego]");
        // USA deny supercedes OR grant
        assertMemberAccess(
            connection, AccessMember.NONE, "[Store].[USA].[OR].[Portland]");
        assertMemberAccess(
            connection, AccessMember.NONE, "[Store].[USA].[WA].[Seattle]");
        assertMemberAccess(connection, AccessMember.NONE, "[Store].[USA].[WA]");
        // above top level
        assertMemberAccess(connection, AccessMember.NONE, "[Store].[All Stores]");
    }

    private void assertMemberAccess(
        final Connection connection,
        AccessMember expectedAccess,
        String memberName)
    {
        final Role role = connection.getRole(); // restricted
        Catalog schema = connection.getCatalog();
        final boolean fail = true;
        Cube salesCube = schema.lookupCube("Sales").orElseThrow();
        final CatalogReader schemaReader =
            salesCube.getCatalogReader(null).withLocus();
        final Member member =
            schemaReader.getMemberByUniqueName(
                Util.parseIdentifier(memberName), true);
        final AccessMember actualAccess = role.getAccess(member);
        assertEquals(expectedAccess, actualAccess, memberName);
    }

    private void assertCubeAccess(
        final Connection connection,
        AccessCube expectedAccess,
        String cubeName)
    {
        final Role role = connection.getRole();
        Catalog schema = connection.getCatalog();
        Cube cube = schema.lookupCube(cubeName).orElseThrow();
        final AccessCube actualAccess = role.getAccess(cube);
        assertEquals(expectedAccess, actualAccess, cubeName);
    }

    private void assertHierarchyAccess(
        final Connection connection,
        AccessHierarchy expectedAccess,
        String cubeName,
        String hierarchyName)
    {
        final Role role = connection.getRole();
        Catalog schema = connection.getCatalog();
        final boolean fail = true;
        Cube cube = schema.lookupCube(cubeName).orElseThrow();
        final CatalogReader schemaReader =
            cube.getCatalogReader(null); // unrestricted
        final Hierarchy hierarchy =
            (Hierarchy) schemaReader.lookupCompound(
                cube, Util.parseIdentifier(hierarchyName), fail,
                DataType.HIERARCHY);

        final AccessHierarchy actualAccess = role.getAccess(hierarchy);
        assertEquals(expectedAccess, actualAccess, cubeName);
    }

    private HierarchyAccess getHierarchyAccess(
        final Connection connection,
        String cubeName,
        String hierarchyName)
    {
        final Role role = connection.getRole();
        Catalog schema = connection.getCatalog();
        final boolean fail = true;
        Cube cube = schema.lookupCube(cubeName).orElseThrow();
        final CatalogReader schemaReader =
            cube.getCatalogReader(null); // unrestricted
        final Hierarchy hierarchy =
            (Hierarchy) schemaReader.lookupCompound(
                cube, Util.parseIdentifier(hierarchyName), fail,
                DataType.HIERARCHY);

        return role.getAccessDetails(hierarchy);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testGrantHierarchy1a(Context foodMartContext) {
        // assert: can access Mexico (explicitly granted)
        // assert: can not access Canada (explicitly denied)
        // assert: can access USA (rule 3 - parent of allowed member San
        // Francisco)
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisReturns(
    		connection, "Sales",
            "[Store].[Store].level.members",
            "[Store].[Store].[Mexico]\n" + "[Store].[Store].[USA]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testGrantHierarchy1aAllMembers(Context foodMartContext) {
        // assert: can access Mexico (explicitly granted)
        // assert: can not access Canada (explicitly denied)
        // assert: can access USA (rule 3 - parent of allowed member San
        // Francisco)
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisReturns(
    		connection, "Sales",
            "[Store].level.allmembers",
            "[Store].[Store].[Mexico]\n" + "[Store].[Store].[USA]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testGrantHierarchy1b(Context foodMartContext) {
        // can access Mexico (explicitly granted) which is the first accessible
        // one
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisReturns(
    		connection, "Sales",
            "[Store].defaultMember",
            "[Store].[Store].[Mexico]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testGrantHierarchy1c(Context foodMartContext) {
        // the root element is All Customers
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisReturns(
    		connection, "Sales",
            "[Customers].defaultMember",
            "[Customers].[Customers].[Canada].[BC]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testGrantHierarchy2(Context foodMartContext) {
        // assert: can access California (parent of allowed member)
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisReturns(
    		connection, "Sales",
            "[Store].[USA].children",
            "[Store].[Store].[USA].[CA]");
        TestUtil.assertAxisReturns(
    		connection, "Sales",
            "[Store].[USA].children",
            "[Store].[Store].[USA].[CA]");
        TestUtil.assertAxisReturns(
    		connection, "Sales",
            "[Store].[USA].[CA].children",
            "[Store].[Store].[USA].[CA].[Los Angeles]\n"
            + "[Store].[Store].[USA].[CA].[San Francisco]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testGrantHierarchy3(Context foodMartContext) {
        // assert: can not access Washington (child of denied member)
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisThrows(connection, "[Store].[USA].[WA]", "not found", "Sales");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testGrantHierarchy4(Context foodMartContext) {
        // assert: can not access Oregon (rule 1 - order matters)
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisThrows(connection,
            "[Store].[USA].[OR].children", "not found", "Sales");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testGrantHierarchy5(Context foodMartContext) {
        // assert: can not access All (above top level)
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisThrows(connection, "[Store].[All Stores]", "not found", "Sales");
        TestUtil.assertAxisReturns(connection, "Sales",
            "[Store].members",
                // note:
                // no: [All Stores] -- above top level
                // no: [Canada] -- not explicitly allowed
                // yes: [Mexico] -- explicitly allowed -- and all its children
                //      except [DF]
                // no: [Mexico].[DF]
                // yes: [USA] -- implicitly allowed
                // yes: [CA] -- implicitly allowed
                // no: [OR], [WA]
                // yes: [San Francisco] -- explicitly allowed
                // no: [San Diego]
            "[Store].[Store].[Mexico]\n"
            + "[Store].[Store].[Mexico].[Guerrero]\n"
            + "[Store].[Store].[Mexico].[Guerrero].[Acapulco]\n"
            + "[Store].[Store].[Mexico].[Guerrero].[Acapulco].[Store 1]\n"
            + "[Store].[Store].[Mexico].[Jalisco]\n"
            + "[Store].[Store].[Mexico].[Jalisco].[Guadalajara]\n"
            + "[Store].[Store].[Mexico].[Jalisco].[Guadalajara].[Store 5]\n"
            + "[Store].[Store].[Mexico].[Veracruz]\n"
            + "[Store].[Store].[Mexico].[Veracruz].[Orizaba]\n"
            + "[Store].[Store].[Mexico].[Veracruz].[Orizaba].[Store 10]\n"
            + "[Store].[Store].[Mexico].[Yucatan]\n"
            + "[Store].[Store].[Mexico].[Yucatan].[Merida]\n"
            + "[Store].[Store].[Mexico].[Yucatan].[Merida].[Store 8]\n"
            + "[Store].[Store].[Mexico].[Zacatecas]\n"
            + "[Store].[Store].[Mexico].[Zacatecas].[Camacho]\n"
            + "[Store].[Store].[Mexico].[Zacatecas].[Camacho].[Store 4]\n"
            + "[Store].[Store].[Mexico].[Zacatecas].[Hidalgo]\n"
            + "[Store].[Store].[Mexico].[Zacatecas].[Hidalgo].[Store 12]\n"
            + "[Store].[Store].[Mexico].[Zacatecas].[Hidalgo].[Store 18]\n"
            + "[Store].[Store].[USA]\n"
            + "[Store].[Store].[USA].[CA]\n"
            + "[Store].[Store].[USA].[CA].[Los Angeles]\n"
            + "[Store].[Store].[USA].[CA].[Los Angeles].[Store 7]\n"
            + "[Store].[Store].[USA].[CA].[San Francisco]\n"
            + "[Store].[Store].[USA].[CA].[San Francisco].[Store 14]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testGrantHierarchy6(Context foodMartContext) {
        // assert: parent if at top level is null
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisReturns(
    		connection, "Sales",
            "[Customers].[USA].[CA].parent",
            "");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testGrantHierarchy7(Context foodMartContext) {
        // assert: members above top level do not exist
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisThrows(
    		connection,
    		"[Customers].[Canada].children",
            "MDX object '[Customers].[Canada]' not found in cube 'Sales'", "Sales");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testGrantHierarchy8(Context foodMartContext) {
        // assert: can not access Catherine Abel in San Francisco (below bottom
        // level)
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisThrows(
    		connection,
            "[Customers].[USA].[CA].[San Francisco].[Catherine Abel]",
            "not found", "Sales");
        TestUtil.assertAxisReturns(
    		connection, "Sales",
            "[Customers].[USA].[CA].[San Francisco].children",
            "");
        Axis axis = TestUtil.executeAxis(connection, "Sales", "[Customers].members");
        // 13 states, 109 cities
        assertEquals(122, axis.getPositions().size());
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testGrantHierarchy8AllMembers(Context foodMartContext) {
        // assert: can not access Catherine Abel in San Francisco (below bottom
        // level)
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisThrows(
    		connection,
            "[Customers].[USA].[CA].[San Francisco].[Catherine Abel]",
            "not found", "Sales");
        TestUtil.assertAxisReturns(
    		connection, "Sales",
            "[Customers].[USA].[CA].[San Francisco].children",
            "");
        Axis axis = TestUtil.executeAxis(connection, "Sales", "[Customers].allmembers");
        // 13 states, 109 cities
        assertEquals(122, axis.getPositions().size());
    }

    /**
     * Tests for Mondrian BUG 1201 - Native Rollups did not handle
     * access-control with more than one member where granted access=all
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testBugMondrian_1201_MultipleMembersInRoleAccessControl(Context foodMartContext) {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier35::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        Connection connection = foodMartContext.getConnection(props);

        // Must return only 2 [USA].[CA] stores
        TestUtil.assertQueryReturns(
    		connection,
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
            + "  Filter( [Store].[USA].[CA].children,"
            + "          [Measures].[Unit Sales]>0) ON ROWS \n"
            + "from [Sales] \n"
            + "where ([Time].[1997].[Q1].[2])",
            "Axis #0:\n"
            + "{[Time].[Time].[1997].[Q1].[2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[Store].[USA].[CA].[San Francisco]}\n"
            + "Row #0: 2,614\n"
            + "Row #1: 187\n");

        // Must return only 2 [USA].[CA] stores
        TestUtil.assertQueryReturns(
    		connection,
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
            + "  TopCount( [Store].[USA].[CA].children, 20,"
            + "            [Measures].[Unit Sales]) ON ROWS \n"
            + "from [Sales] \n"
            + "where ([Time].[1997].[Q1].[2])",
            "Axis #0:\n"
            + "{[Time].[Time].[1997].[Q1].[2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[Store].[USA].[CA].[San Francisco]}\n"
            + "Row #0: 2,614\n"
            + "Row #1: 187\n");


        // Partial Rollup: [USA].[CA] rolls up only up to 2.801
        TestUtil.assertQueryReturns(
    		connection,
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
            + "  Filter( [Store].[Store State].Members,"
            + "          [Measures].[Unit Sales]>4000) ON ROWS \n"
            + "from [Sales] \n"
            + "where ([Time].[1997].[Q1].[2])",
            "Axis #0:\n"
            + "{[Time].[Time].[1997].[Q1].[2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[OR]}\n"
            + "{[Store].[Store].[USA].[WA]}\n"
            + "Row #0: 4,617\n"
            + "Row #1: 10,319\n");

        ((TestContext)foodMartContext).setCatalogMappingSupplier(new FoodmartMappingSupplier());
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier35::new);
        props = new RolapConnectionPropsR(List.of("Role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        connection = foodMartContext.getConnection(props);

        // Full Rollup: [USA].[CA] rolls up to 6.021
        TestUtil.assertQueryReturns(
    		connection,
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
            + "  Filter( [Store].[Store State].Members,"
            + "          [Measures].[Unit Sales]>4000) ON ROWS \n"
            + "from [Sales] \n"
            + "where ([Time].[1997].[Q1].[2])",
            "Axis #0:\n"
            + "{[Time].[Time].[1997].[Q1].[2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[CA]}\n"
            + "{[Store].[Store].[USA].[OR]}\n"
            + "{[Store].[Store].[USA].[WA]}\n"
            + "Row #0: 6,021\n"
            + "Row #1: 4,617\n"
            + "Row #2: 10,319\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testBugMondrian_2586_RaggedDimMembersShouldBeVisible(Context foodMartContext) {
      TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier38::new);
    //[Geography].[Country]
      ConnectionProps props = new RolapConnectionPropsR(List.of("Sales Ragged"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
      Connection connection = foodMartContext.getConnection(props);
      TestUtil.assertQueryReturns(
		  connection,
        "select {[Measures].[Unit Sales]} ON COLUMNS, {[Geography].[Country].MEMBERS} ON ROWS from [Sales Ragged]",
        "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Geography].[Geography].[Canada]}\n"
        + "{[Geography].[Geography].[Israel]}\n"
        + "{[Geography].[Geography].[Mexico]}\n"
        + "{[Geography].[Geography].[USA]}\n"
        + "{[Geography].[Geography].[Vatican]}\n"
        + "Row #0: \n"
        + "Row #1: 13,694\n"
        + "Row #2: \n"
        + "Row #3: 217,822\n"
        + "Row #4: 35,257\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testBugMondrian_1201_CacheAwareOfRoleAccessControl(Context foodMartContext) {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier36::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        Connection connection = foodMartContext.getConnection(props);

        // Put query into cache
        TestUtil.assertQueryReturns(
    		connection,
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
            + "  Filter( [Store].[USA].[CA].children,"
            + "          [Measures].[Unit Sales]>0) ON ROWS \n"
            + "from [Sales] \n"
            + "where ([Time].[1997].[Q1].[2])",
            "Axis #0:\n"
            + "{[Time].[Time].[1997].[Q1].[2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[Store].[USA].[CA].[San Francisco]}\n"
            + "Row #0: 2,614\n"
            + "Row #1: 187\n");

        ((TestContext)foodMartContext).setCatalogMappingSupplier(new FoodmartMappingSupplier());
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier36::new);
        props = new RolapConnectionPropsR(List.of("Role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        connection = foodMartContext.getConnection(props);

        // Run same query using another role with different access controls
        TestUtil.assertQueryReturns(
    		connection,
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
            + "  TopCount( [Store].[USA].[CA].children, 20,"
            + "            [Measures].[Unit Sales]) ON ROWS \n"
            + "from [Sales] \n"
            + "where ([Time].[1997].[Q1].[2])",
            "Axis #0:\n"
            + "{[Time].[Time].[1997].[Q1].[2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[CA].[San Francisco]}\n"
            + "Row #0: 187\n");
    }

    /**
     * Tests for Mondrian BUG 1127 - Native Top Count was not taking into
     * account user roles
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testBugMondrian1127OneSlicerOnly(Context foodMartContext) {
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertQueryReturns(
    		connection,
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
            + "  TopCount([Store].[USA].[CA].Children, 10,"
            + "           [Measures].[Unit Sales]) ON ROWS \n"
            + "from [Sales] \n"
            + "where ([Time].[1997].[Q1].[2])",
            "Axis #0:\n"
            + "{[Time].[Time].[1997].[Q1].[2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[Store].[USA].[CA].[San Francisco]}\n"
            + "Row #0: 2,614\n"
            + "Row #1: 187\n");

        TestUtil.assertQueryReturns(
        	foodMartContext.getConnectionWithDefaultRole(),
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
            + "  NON EMPTY TopCount([Store].[USA].[CA].Children, 10, "
            + "           [Measures].[Unit Sales]) ON ROWS \n"
            + "from [Sales] \n"
            + "where ([Time].[1997].[Q1].[2])",
            "Axis #0:\n"
            + "{[Time].[Time].[1997].[Q1].[2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[Store].[USA].[CA].[Beverly Hills]}\n"
            + "{[Store].[Store].[USA].[CA].[San Francisco]}\n"
            + "Row #0: 2,614\n"
            + "Row #1: 1,879\n"
            + "Row #2: 1,341\n"
            + "Row #3: 187\n");
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testBugMondrian1127MultipleSlicers(Context foodMartContext) {
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertQueryReturns(
    		connection,
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
            + "  TopCount([Store].[USA].[CA].Children, 10,"
            + "           [Measures].[Unit Sales]) ON ROWS \n"
            + "from [Sales] \n"
            + "where ([Time].[1997].[Q1].[2] : [Time].[1997].[Q1].[3])",
            "Axis #0:\n"
            + "{[Time].[Time].[1997].[Q1].[2]}\n"
            + "{[Time].[Time].[1997].[Q1].[3]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[Store].[USA].[CA].[San Francisco]}\n"
            + "Row #0: 4,497\n"
            + "Row #1: 337\n");

        TestUtil.assertQueryReturns(
        	foodMartContext.getConnectionWithDefaultRole(),
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
            + "  NON EMPTY TopCount([Store].[USA].[CA].Children, 10, "
            + "           [Measures].[Unit Sales]) ON ROWS \n"
            + "from [Sales] \n"
            + "where ([Time].[1997].[Q1].[2] : [Time].[1997].[Q1].[3])",
            "Axis #0:\n"
            + "{[Time].[Time].[1997].[Q1].[2]}\n"
            + "{[Time].[Time].[1997].[Q1].[3]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[Store].[USA].[CA].[Beverly Hills]}\n"
            + "{[Store].[Store].[USA].[CA].[San Francisco]}\n"
            + "Row #0: 4,497\n"
            + "Row #1: 4,094\n"
            + "Row #2: 2,585\n"
            + "Row #3: 337\n");
    }

    /**
     * Tests that we only aggregate over SF, LA, even when called from
     * functions.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testGrantHierarchy9(Context foodMartContext) {
        // Analysis services doesn't allow aggregation within calculated
        // measures, so use the following query to generate the results:
        //
        //   with member [Store].[SF LA] as
        //     'Aggregate({[USA].[CA].[San Francisco], [Store].[USA].[CA].[Los
        //     Angeles]})'
        //   select {[Measures].[Unit Sales]} on columns,
        //    {[Gender].children} on rows
        //   from Sales
        //   where ([Marital Status].[S], [Store].[SF LA])
    	final Connection connection = getRestrictedConnection(foodMartContext);
    	//Connection connection = foodMartContext.getConnection();
    	TestUtil.assertQueryReturns(
			connection,
            "with member [Measures].[California Unit Sales] as "
            + " 'Aggregate({[Store].[USA].[CA].children}, [Measures].[Unit Sales])'\n"
            + "select {[Measures].[California Unit Sales]} on columns,\n"
            + " {[Gender].children} on rows\n"
            + "from Sales\n"
            + "where ([Marital Status].[S])",
            "Axis #0:\n"
            + "{[Marital Status].[Marital Status].[S]}\n"
            + "Axis #1:\n"
            + "{[Measures].[California Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Gender].[Gender].[F]}\n"
            + "{[Gender].[Gender].[M]}\n"
            + "Row #0: 6,636\n"
            + "Row #1: 7,329\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testGrantHierarchyA(Context foodMartContext) {
    	final Connection connection = getRestrictedConnection(foodMartContext);
        // assert: totals for USA include missing cells
    	TestUtil.assertQueryReturns(
			connection,
            "select {[Unit Sales]} on columns,\n"
            + "{[Store].[USA], [Store].[USA].children} on rows\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA]}\n"
            + "{[Store].[Store].[USA].[CA]}\n"
            + "Row #0: 266,773\n"
            + "Row #1: 74,748\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    public void _testSharedObjectsInGrantMappingsBug(Context foodMartContext) {
        boolean mustGet = true;
        Connection connection = foodMartContext.getConnectionWithDefaultRole();
        Catalog schema = connection.getCatalog();
        Cube salesCube = schema.lookupCube("Sales").orElseThrow();
        Cube warehouseCube = schema.lookupCube("Warehouse").orElseThrow();
        Hierarchy measuresInSales = salesCube.lookupHierarchy(
            new IdImpl.NameSegmentImpl("Measures", Quoting.UNQUOTED), false);
        Hierarchy storeInWarehouse = warehouseCube.lookupHierarchy(
            new IdImpl.NameSegmentImpl("Store", Quoting.UNQUOTED), false);

        RoleImpl role = new RoleImpl();
        role.grant(schema, AccessCatalog.NONE);
        role.grant(salesCube, AccessCube.NONE);
        // For using hierarchy Measures in #assertExprThrows
        RollupPolicy rollupPolicy = RollupPolicy.FULL;
        role.grant(
            measuresInSales, AccessHierarchy.ALL, null, null, rollupPolicy);
        role.grant(warehouseCube, AccessCube.NONE);
        role.grant(storeInWarehouse.getDimension(), AccessDimension.ALL);

        role.makeImmutable();
        connection.setRole(role);

        TestUtil.assertExprThrows(
    		connection, "Sales",
            "[Store].DefaultMember",
            "cube 'Sales' not found");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testNoAccessToCube(Context foodMartContext) {
        final Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertQueryThrows(connection, "select from [HR]", "MDX cube 'HR' not found");
    }

    private Connection getRestrictedConnection(Context foodMartContext) {
        return getRestrictedConnection(foodMartContext, true);
    }

    /**
     * Returns a connection with limited access to the schema.
     *
     * @param restrictCustomers true to restrict access to the customers
     * dimension. This will change the defaultMember of the dimension,
     * all cell values will be null because there are no sales data
     * for Canada
     *
     * @return restricted connection
     */
    private Connection getRestrictedConnection(Context foodMartContext, boolean restrictCustomers) {
        Connection connection = foodMartContext.getConnectionWithDefaultRole();
        RoleImpl role = new RoleImpl();
        Catalog schema = connection.getCatalog();
        final boolean fail = true;
        Cube salesCube = schema.lookupCube("Sales").orElseThrow();
        final CatalogReader schemaReader =
            salesCube.getCatalogReader(null).withLocus();
        Hierarchy storeHierarchy = salesCube.lookupHierarchy(
            new IdImpl.NameSegmentImpl("Store", Quoting.UNQUOTED), false);
        role.grant(schema, AccessCatalog.ALL_DIMENSIONS);
        role.grant(salesCube, AccessCube.ALL);
        Level nationLevel =
            Util.lookupHierarchyLevel(storeHierarchy, "Store Country");
        RollupPolicy rollupPolicy = RollupPolicy.FULL;
        role.grant(
            storeHierarchy, AccessHierarchy.CUSTOM, nationLevel, null, rollupPolicy);
        role.grant(
            schemaReader.getMemberByUniqueName(
                Util.parseIdentifier("[Store].[All Stores].[USA].[OR]"), fail),
            AccessMember.ALL);
        role.grant(
            schemaReader.getMemberByUniqueName(
                Util.parseIdentifier("[Store].[All Stores].[USA]"), fail),
            AccessMember.CUSTOM);
        role.grant(
            schemaReader.getMemberByUniqueName(
                Util.parseIdentifier(
                    "[Store].[All Stores].[USA].[CA].[San Francisco]"), fail),
            AccessMember.ALL);
        role.grant(
            schemaReader.getMemberByUniqueName(
                Util.parseIdentifier(
                    "[Store].[All Stores].[USA].[CA].[Los Angeles]"), fail),
            AccessMember.ALL);
        role.grant(
            schemaReader.getMemberByUniqueName(
                Util.parseIdentifier(
                    "[Store].[All Stores].[Mexico]"), fail),
            AccessMember.ALL);
        role.grant(
            schemaReader.getMemberByUniqueName(
                Util.parseIdentifier(
                    "[Store].[All Stores].[Mexico].[DF]"), fail),
            AccessMember.NONE);
        role.grant(
            schemaReader.getMemberByUniqueName(
                Util.parseIdentifier(
                    "[Store].[All Stores].[Canada]"), fail),
            AccessMember.NONE);
        if (restrictCustomers) {
            Hierarchy customersHierarchy =
                salesCube.lookupHierarchy(
                    new IdImpl.NameSegmentImpl("Customers", Quoting.UNQUOTED),
                    false);
            Level stateProvinceLevel =
                Util.lookupHierarchyLevel(customersHierarchy, "State Province");
            Level customersCityLevel =
                Util.lookupHierarchyLevel(customersHierarchy, "City");
            role.grant(
                customersHierarchy,
                AccessHierarchy.CUSTOM,
                stateProvinceLevel,
                customersCityLevel,
                rollupPolicy);
        }

        // No access to HR cube.
        Cube hrCube = schema.lookupCube("HR").orElseThrow();
        role.grant(hrCube, AccessCube.NONE);

        role.makeImmutable();
        connection.setRole(role);
        return connection;
    }

    /**
     * Test context where the [Store] hierarchy has restricted access
     * and cell values are rolled up with 'partial' policy.
     */
    private void setRollupTestContext(Context foodMartContext) {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier37::new);
    }

    /**
     * Basic test of partial rollup policy. [USA] = [OR] + [WA], not
     * the usual [CA] + [OR] + [WA].
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testRollupPolicyBasic(Context foodMartContext) {
        setRollupTestContext(foodMartContext);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        Connection connection = foodMartContext.getConnection(props);
        TestUtil.assertQueryReturns(
    		connection,
            "select {[Store].[USA], [Store].[USA].Children} on 0\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[Store].[USA]}\n"
            + "{[Store].[Store].[USA].[OR]}\n"
            + "{[Store].[Store].[USA].[WA]}\n"
            + "Row #0: 192,025\n"
            + "Row #0: 67,659\n"
            + "Row #0: 124,366\n");
    }

    /**
     * The total for [Store].[All Stores] is similarly reduced. All
     * children of [All Stores] are visible, but one grandchild is not.
     * Normally the total is 266,773.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testRollupPolicyAll(Context foodMartContext) {
        setRollupTestContext(foodMartContext);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        Connection connection = foodMartContext.getConnection(props);
        TestUtil.assertExprReturns(
    		connection, "Sales",
            "([Store].[All Stores])",
            "192,025");
    }

    /**
     * Access [Store].[All Stores] implicitly as it is the default member
     * of the [Stores] hierarchy.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testRollupPolicyAllAsDefault(Context foodMartContext) {
        setRollupTestContext(foodMartContext);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        Connection connection = foodMartContext.getConnection(props);
        TestUtil.assertExprReturns(
    		connection, "Sales",
            "([Store])",
            "192,025");
    }

    /**
     * Access [Store].[All Stores] via the Parent relationship (to check
     * that this doesn't circumvent access control).
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testRollupPolicyAllAsParent(Context foodMartContext) {
        setRollupTestContext(foodMartContext);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        Connection connection = foodMartContext.getConnection(props);
        TestUtil.assertExprReturns(
    		connection, "Sales",
            "([Store].[USA].Parent)",
            "192,025");
    }

    /**
     * Tests that an access-controlled dimension affects results even if not
     * used in the query. Unit test for
     * <a href="http://jira.pentaho.com/browse/mondrian-1283">MONDRIAN-1283,
     * "Mondrian doesn't restrict dimension members when dimension isn't
     * included"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testUnusedAccessControlledDimension(Context foodMartContext) {
        Connection connection = foodMartContext.getConnectionWithDefaultRole();
        TestUtil.assertQueryReturns(
    		connection,
            "select [Gender].Children on 0 from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Gender].[Gender].[F]}\n"
            + "{[Gender].[Gender].[M]}\n"
            + "Row #0: 131,558\n"
            + "Row #0: 135,215\n");

        setRollupTestContext(foodMartContext);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        connection = foodMartContext.getConnection(props);
        TestUtil.assertQueryReturns(
    		connection,
            "select [Gender].Children on 0 from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Gender].[Gender].[F]}\n"
            + "{[Gender].[Gender].[M]}\n"
            + "Row #0: 94,799\n"
            + "Row #0: 97,226\n");
    }

    /**
     * Tests that members below bottom level are regarded as visible.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testRollupBottomLevel(Context context) {
        rollupPolicyBottom(
            context, RollupPolicyType.FULL, "74,748", "36,759", "266,773");
        ((TestContext)context).setCatalogMappingSupplier(new FoodmartMappingSupplier());
        rollupPolicyBottom(
        		context, RollupPolicyType.PARTIAL, "72,739", "35,775", "264,764");
        ((TestContext)context).setCatalogMappingSupplier(new FoodmartMappingSupplier());
        rollupPolicyBottom(context, RollupPolicyType.HIDDEN, "", "", "");
    }

    private void rollupPolicyBottom(
		Context context,
        RollupPolicyType rollupPolicy,
        String v1,
        String v2,
        String v3)
    {
        context.getCatalogCache().clear();
        CatalogMapping catalog = ((RolapContext) context).getCatalogMapping();
        ((TestContext)context).setCatalogMappingSupplier(new SchemaModifiers.AccessControlTestModifier39(catalog, rollupPolicy));
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = context.getConnection(props);
        // All of the children of [San Francisco] are invisible, because [City]
        // is the bottom level, but that shouldn't affect the total.
    	TestUtil.assertExprReturns(
			connection, "Sales",
            "([Customers].[USA].[CA].[San Francisco])", "88");
    	TestUtil.assertExprThrows(
			connection, "Sales",
			"([Customers].[USA].[CA].[Los Angeles])",
            "MDX object '[Customers].[USA].[CA].[Los Angeles]' not found in cube 'Sales'");

    	TestUtil.assertExprReturns(connection, "Sales", "([Customers].[USA].[CA])", v1);
    	TestUtil.assertExprReturns(
			connection, "Sales",
            "([Customers].[USA].[CA], [Gender].[F])", v2);
    	TestUtil.assertExprReturns(connection, "Sales", "([Customers].[USA])", v3);

    	checkQuery(
    			connection,
            "select [Customers].Children on 0, "
            + "[Gender].Members on 1 from [Sales]");
    }

    /**
     * Calls various {@link CatalogReader} methods on the members returned in
     * a result set.
     *
     * @param connection connection
     * @param mdx MDX query
     */
    private void checkQuery(Connection connection, String mdx) {
        Result result = TestUtil.executeQuery(connection, mdx);
        final CatalogReader schemaReader =
        		connection.getCatalogReader().withLocus();
        for (Axis axis : result.getAxes()) {
            for (Position position : axis.getPositions()) {
                for (Member member : position) {
                    final Member accessControlledParent =
                        schemaReader.getMemberParent(member);
                    if (member.getParentMember() == null) {
                        assertNull(accessControlledParent);
                    }
                    final List<Member> accessControlledChildren =
                        schemaReader.getMemberChildren(member);
                    assertNotNull(accessControlledChildren);
                }
            }
        }
    }

    /**
     * Tests that a bad value for the rollupPolicy attribute gives the
     * appropriate error.
     */
    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testRollupPolicyNegative(Context foodMartContext) {
//    	String schema = SchemaUtil.getSchema(baseSchema,
//                null, null, null, null, null,
//                "<Role name=\"Role1\">\n"
//                + "  <SchemaGrant access=\"none\">\n"
//                + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
//                + "      <HierarchyGrant hierarchy=\"[Customers]\" access=\"custom\" rollupPolicy=\"bad\" bottomLevel=\"[Customers].[City]\">\n"
//                + "        <MemberGrant member=\"[Customers].[USA]\" access=\"all\"/>\n"
//                + "        <MemberGrant member=\"[Customers].[USA].[CA].[Los Angeles]\" access=\"none\"/>\n"
//                + "      </HierarchyGrant>\n"
//                + "    </CubeGrant>\n"
//                + "  </SchemaGrant>\n"
//                + "</Role>");
    	TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier1::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	TestUtil.assertQueryThrows(
    			foodMartContext,
                props,
    			"select from [Sales]",
    			"Illegal rollupPolicy value 'bad'");
    }

    /**
     * Tests where all children are visible but a grandchild is not.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testRollupPolicyGreatGrandchildInvisible(Context context) {
        rollupPolicyGreatGrandchildInvisible(
    		context, RollupPolicyType.FULL, "266,773", "74,748");
        ((TestContext)context).setCatalogMappingSupplier(new FoodmartMappingSupplier());
        rollupPolicyGreatGrandchildInvisible(
    		context, RollupPolicyType.PARTIAL, "266,767", "74,742");
        ((TestContext)context).setCatalogMappingSupplier(new FoodmartMappingSupplier());
        rollupPolicyGreatGrandchildInvisible(
    		context, RollupPolicyType.HIDDEN, "", "");
    }

    private void rollupPolicyGreatGrandchildInvisible(
		Context context,
		RollupPolicyType policy,
        String v1,
        String v2)
    {
        context.getCatalogCache().clear();
        CatalogMapping catalog = ((RolapContext) context).getCatalogMapping();
        ((TestContext)context).setCatalogMappingSupplier(new SchemaModifiers.AccessControlTestModifier40(catalog, policy));
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = context.getConnection(props);
    	TestUtil.assertExprReturns(connection, "Sales", "[Measures].[Unit Sales]", v1);
    	TestUtil.assertExprReturns(
			connection, "Sales", "([Measures].[Unit Sales], [Customers].[USA])",
            v1);
    	TestUtil.assertExprReturns(
			connection, "Sales",
            "([Measures].[Unit Sales], [Customers].[USA].[CA])",
            v2);
    }

    /**
     * Tests where two hierarchies are simultaneously access-controlled.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testRollupPolicySimultaneous(Context foodMartContext) {
//         note that v2 is different for full vs partial, v3 is the same
        rollupPolicySimultaneous(
    		foodMartContext, RollupPolicyType.FULL, "266,773", "74,748", "25,635");
        ((TestContext)foodMartContext).setCatalogMappingSupplier(new FoodmartMappingSupplier());
        rollupPolicySimultaneous(
    		foodMartContext, RollupPolicyType.PARTIAL, "72,631", "72,631", "25,635");
        ((TestContext)foodMartContext).setCatalogMappingSupplier(new FoodmartMappingSupplier());
        rollupPolicySimultaneous(
    		foodMartContext, RollupPolicyType.HIDDEN, "", "", "");
    }

    private void rollupPolicySimultaneous(
		Context context,
        RollupPolicyType policy,
        String v1,
        String v2,
        String v3)
    {
        context.getCatalogCache().clear();
        CatalogMapping catalogMapping = ((RolapContext) context).getCatalogMapping();
        ((TestContext)context).setCatalogMappingSupplier(new SchemaModifiers.AccessControlTestModifier41(catalogMapping, policy));
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = context.getConnection(props);
    	TestUtil.assertExprReturns(connection, "Sales", "[Measures].[Unit Sales]", v1);
    	TestUtil.assertExprReturns(
			connection, "Sales",
            "([Measures].[Unit Sales], [Customers].[USA])",
            v1);
    	TestUtil.assertExprReturns(
			connection, "Sales",
            "([Measures].[Unit Sales], [Customers].[USA].[CA])",
            v2);
    	TestUtil.assertExprReturns(
			connection, "Sales",
            "([Measures].[Unit Sales], "
            + "[Customers].[USA].[CA], [Store].[USA].[CA])",
            v2);
    	TestUtil.assertExprReturns(
			connection, "Sales",
            "([Measures].[Unit Sales], "
            + "[Customers].[USA].[CA], "
            + "[Store].[USA].[CA].[San Diego])",
            v3);
    }

    // todo: performance test where 1 of 1000 children is not visible

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testUnionRole(Context foodMartContext) {
    	/*
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema,
                null, null, null, null, null,
                "<Role name=\"Role1\">\n"
                + "  <SchemaGrant access=\"none\">\n"
                + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
                + "      <HierarchyGrant hierarchy=\"[Customers]\" access=\"custom\" rollupPolicy=\"Partial\">\n"
                + "        <MemberGrant member=\"[Customers].[USA].[CA]\" access=\"all\"/>\n"
                + "        <MemberGrant member=\"[Customers].[USA].[CA].[San Francisco].[Gladys Evans]\" access=\"none\"/>\n"
                + "      </HierarchyGrant>\n"
                + "      <HierarchyGrant hierarchy=\"[Promotion Media]\" access=\"all\"/>\n"
                + "      <HierarchyGrant hierarchy=\"[Marital Status]\" access=\"none\"/>\n"
                + "      <HierarchyGrant hierarchy=\"[Gender]\" access=\"none\"/>\n"
                + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"custom\" rollupPolicy=\"Partial\" topLevel=\"[Store].[Store State]\"/>\n"
                + "    </CubeGrant>\n"
                + "    <CubeGrant cube=\"Warehouse\" access=\"all\"/>\n"
                + "  </SchemaGrant>\n"
                + "</Role>\n"
                + "<Role name=\"Role2\">\n"
                + "  <SchemaGrant access=\"none\">\n"
                + "    <CubeGrant cube=\"Sales\" access=\"none\">\n"
                + "      <HierarchyGrant hierarchy=\"[Customers]\" access=\"custom\" rollupPolicy=\"Hidden\">\n"
                + "        <MemberGrant member=\"[Customers].[USA]\" access=\"all\"/>\n"
                + "        <MemberGrant member=\"[Customers].[USA].[CA]\" access=\"none\"/>\n"
                + "        <MemberGrant member=\"[Customers].[USA].[OR]\" access=\"none\"/>\n"
                + "        <MemberGrant member=\"[Customers].[USA].[OR].[Portland]\" access=\"all\"/>\n"
                + "      </HierarchyGrant>\n"
                + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"all\" rollupPolicy=\"Hidden\"/>\n"
                + "    </CubeGrant>\n"
                + "  </SchemaGrant>\n"
                + "</Role>\n");
        */
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier2::new);

        Connection connection;

        try {
            ConnectionProps props = new RolapConnectionPropsR(List.of("Role3", "Role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        	connection = foodMartContext.getConnection(props);
        	fail("expected exception, got " + connection);
        } catch (RuntimeException e) {
            final String message = e.getMessage();
            assertTrue(message.indexOf("Role 'Role3' not found") >= 0, message);
        }

        try {
            ConnectionProps props = new RolapConnectionPropsR(List.of("Role1", "Role3"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        	connection = foodMartContext.getConnection(props);
            fail("expected exception, got " + connection);
        } catch (RuntimeException e) {
            final String message = e.getMessage();
            assertTrue(message.indexOf("Role 'Role3' not found") >= 0, message);
        }

        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1", "Role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	connection = foodMartContext.getConnection(props);

        // Cube access:
        // Both can see [Sales]
        // Role1 only see [Warehouse]
        // Neither can see [Warehouse and Sales]
        assertCubeAccess(connection, AccessCube.ALL, "Sales");
        assertCubeAccess(connection, AccessCube.ALL, "Warehouse");
        assertCubeAccess(connection, AccessCube.NONE, "Warehouse and Sales");

        // Hierarchy access:
        // Both can see [Customers] with Custom access
        // Both can see [Store], Role1 with Custom access, Role2 with All access
        // Role1 can see [Promotion Media], Role2 cannot
        // Neither can see [Marital Status]
        assertHierarchyAccess(
            connection, AccessHierarchy.CUSTOM, "Sales", "[Customers]");
        assertHierarchyAccess(
            connection, AccessHierarchy.ALL, "Sales", "[Store]");
        assertHierarchyAccess(
            connection, AccessHierarchy.ALL, "Sales", "[Promotion Media]");
        assertHierarchyAccess(
            connection, AccessHierarchy.NONE, "Sales", "[Marital Status]");

        // Rollup policy is the greater of Role1's partian and Role2's hidden
        final HierarchyAccess hierarchyAccess =
            getHierarchyAccess(connection, "Sales", "[Store]");
        assertEquals(
            RollupPolicy.PARTIAL,
            hierarchyAccess.getRollupPolicy());
        // One of the roles is restricting the levels, so we
        // expect only the levels from 2 to 4 to be available.
        assertEquals(2, hierarchyAccess.getTopLevelDepth());
        assertEquals(4, hierarchyAccess.getBottomLevelDepth());

        // Member access:
        // both can see [USA]
        assertMemberAccess(connection, AccessMember.CUSTOM, "[Customers].[USA]");
        // Role1 can see [CA], Role2 cannot
        assertMemberAccess(connection, AccessMember.CUSTOM, "[Customers].[USA].[CA]");
        // Role1 cannoy see [USA].[OR].[Portland], Role2 can
        assertMemberAccess(
            connection, AccessMember.ALL, "[Customers].[USA].[OR].[Portland]");
        // Role1 cannot see [USA].[OR], Role2 can see it by virtue of [Portland]
        assertMemberAccess(
            connection, AccessMember.CUSTOM, "[Customers].[USA].[OR]");
        // Neither can see Beaverton
        assertMemberAccess(
            connection, AccessMember.NONE, "[Customers].[USA].[OR].[Beaverton]");

        // Rollup policy
        String mdx = "select Hierarchize(\n"
            + "{[Customers].[USA].Children,\n"
            + " [Customers].[USA].[OR].Children}) on 0\n"
            + "from [Sales]";
        connection = foodMartContext.getConnectionWithDefaultRole();
        TestUtil.assertQueryReturns(
    		connection,
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Customers].[USA].[CA]}\n"
            + "{[Customers].[USA].[OR]}\n"
            + "{[Customers].[USA].[OR].[Albany]}\n"
            + "{[Customers].[USA].[OR].[Beaverton]}\n"
            + "{[Customers].[USA].[OR].[Corvallis]}\n"
            + "{[Customers].[USA].[OR].[Lake Oswego]}\n"
            + "{[Customers].[USA].[OR].[Lebanon]}\n"
            + "{[Customers].[USA].[OR].[Milwaukie]}\n"
            + "{[Customers].[USA].[OR].[Oregon City]}\n"
            + "{[Customers].[USA].[OR].[Portland]}\n"
            + "{[Customers].[USA].[OR].[Salem]}\n"
            + "{[Customers].[USA].[OR].[W. Linn]}\n"
            + "{[Customers].[USA].[OR].[Woodburn]}\n"
            + "{[Customers].[USA].[WA]}\n"
            + "Row #0: 74,748\n"
            + "Row #0: 67,659\n"
            + "Row #0: 6,806\n"
            + "Row #0: 4,558\n"
            + "Row #0: 9,539\n"
            + "Row #0: 4,910\n"
            + "Row #0: 9,596\n"
            + "Row #0: 5,145\n"
            + "Row #0: 3,708\n"
            + "Row #0: 3,583\n"
            + "Row #0: 7,678\n"
            + "Row #0: 4,175\n"
            + "Row #0: 7,961\n"
            + "Row #0: 124,366\n");

        props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	connection = foodMartContext.getConnection(props);
    	TestUtil.assertQueryThrows(
			connection,
            mdx,
            "MDX object '[Customers].[USA].[OR]' not found in cube 'Sales'");

        props = new RolapConnectionPropsR(List.of("Role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	connection = foodMartContext.getConnection(props);
    	TestUtil.assertQueryThrows(
			connection,
            mdx,
            "MDX cube 'Sales' not found");

        // Compared to above:
        // a. cities in Oregon are missing besides Portland
        // b. total for Oregon = total for Portland
        props = new RolapConnectionPropsR(List.of("Role1", "Role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	connection = foodMartContext.getConnection(props);
    	TestUtil.assertQueryReturns(
			connection,
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Customers].[USA].[CA]}\n"
            + "{[Customers].[USA].[OR]}\n"
            + "{[Customers].[USA].[OR].[Portland]}\n"
            + "{[Customers].[USA].[WA]}\n"
            + "Row #0: 74,742\n"
            + "Row #0: 3,583\n"
            + "Row #0: 3,583\n"
            + "Row #0: 124,366\n");
        checkQuery(connection, mdx);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testUnionOfUnionRole(Context foodMartContext) {

        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier3::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("grandparent of USA manager"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        Connection connection = foodMartContext.getConnection(props);

        // Can access [Sales]?
        assertCubeAccess(connection, AccessCube.ALL, "Sales");

        // Has custom access to [Customers]?
        assertHierarchyAccess
                (connection, AccessHierarchy.CUSTOM, "Sales", "[Customers]");

        final HierarchyAccess hierarchyAccess =
                getHierarchyAccess(connection, "Sales", "[Customers]");

        // Can access all levels of the [Customers] hierarchy?
        assertEquals(0, hierarchyAccess.getTopLevelDepth());
        assertEquals(4, hierarchyAccess.getBottomLevelDepth());

        // Can see [USA]?
        assertMemberAccess(connection, AccessMember.ALL, "[Customers].[USA]");

        // Cannot see [Mexico]?
        assertMemberAccess(connection, AccessMember.NONE, "[Customers].[Mexico]");
    }

    /**
     * This is a test for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1384">MONDRIAN-1384</a>
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testUnionRoleHasInaccessibleDescendants(Context foodMartContext) throws Exception {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier4::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1","Role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = foodMartContext.getConnection(props);
        final Cube cube =
            connection.getCatalog()
                .lookupCube("Sales").orElseThrow();
        final HierarchyAccess accessDetails =
            connection.getRole().getAccessDetails(
                cube.lookupHierarchy(
                    new IdImpl.NameSegmentImpl("Customers", Quoting.UNQUOTED),
                    false));
        final CatalogReader scr =
            cube.getCatalogReader(null).withLocus();
        assertEquals(
            true,
            accessDetails.hasInaccessibleDescendants(
                scr.getMemberByUniqueName(
                    Util.parseIdentifier("[Customers].[USA]"),
                    true)));
    }

    /**
     * This is a test for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1168">MONDRIAN-1168</a>
     * Union of roles would sometimes return levels which should be restricted
     * by ACL.
     */
    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testRoleUnionWithLevelRestrictions(Context foodMartContext)  throws Exception {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier5::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1","Role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = foodMartContext.getConnection(props);

    	TestUtil.assertQueryReturns(
			connection,
            "select {[Customers].[State Province].Members} on columns from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Customers].[USA].[CA]}\n"
            + "Row #0: 74,748\n");

    	TestUtil.assertQueryReturns(
			connection,
            "select {[Customers].[Country].Members} on columns from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n");

        CatalogReader reader =
        		connection.getCatalogReader().withLocus();
        Cube cube = null;
        for (Cube c : reader.getCubes()) {
            if (c.getName().equals("Sales")) {
                cube = c;
            }
        }
        assertNotNull(cube);
        reader =
            cube.getCatalogReader(connection.getRole());
        final List<Dimension> dimensions =
            reader.getCubeDimensions(cube);
        Dimension dimension = null;
        for (Dimension dim : dimensions) {
            if (dim.getName().equals("Customers")) {
                dimension = dim;
            }
        }
        assertNotNull(dimension);
        Hierarchy hierarchy =
            reader.getDimensionHierarchies(dimension).get(0);
        assertNotNull(hierarchy);
        final List<Level> levels =
            reader.getHierarchyLevels(hierarchy);

        // Do some tests
        assertEquals(1, levels.size());
        assertEquals(
            2,
            connection
                .getRole().getAccessDetails(hierarchy)
                    .getBottomLevelDepth());
        assertEquals(
            2,
            connection
                .getRole().getAccessDetails(hierarchy)
                    .getTopLevelDepth());
    }

    /**
     * Test to verify that non empty crossjoins enforce role access.
     * Testcase for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-369">
     * MONDRIAN-369, "Non Empty Crossjoin fails to enforce role access".
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testNonEmptyAccess(Context foodMartContext) {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier6::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = foodMartContext.getConnection(props);

        // regular crossjoin returns the correct list of product children
        final String expected =
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Gender].[Gender].[All Gender], [Product].[Product].[Drink]}\n"
            + "Row #0: 24,597\n";

        final String mdx =
            "select {[Measures].[Unit Sales]} ON COLUMNS, "
            + " Crossjoin({[Gender].[All Gender]}, "
            + "[Product].Children) ON ROWS "
            + "from [Sales]";
        TestUtil.assertQueryReturns(connection,mdx, expected);
        checkQuery(connection, mdx);

        // with bug MONDRIAN-397, non empty crossjoin did not return the correct
        // list
        final String mdx2 =
            "select {[Measures].[Unit Sales]} ON COLUMNS, "
            + "NON EMPTY Crossjoin({[Gender].[All Gender]}, "
            + "[Product].[All Products].Children) ON ROWS "
            + "from [Sales]";
        TestUtil.assertQueryReturns(connection,mdx2, expected);
        checkQuery(connection, mdx2);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testNonEmptyAccessLevelMembers(Context foodMartContext) {
        /*
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema,
            null,
            null,
            null,
            null,
            null,
            "<Role name=\"Role1\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
            + "      <HierarchyGrant hierarchy=\"[Product]\" access=\"custom\">\n"
            + "        <MemberGrant member=\"[Product].[Drink]\" access=\"all\"/>\n"
            + "      </HierarchyGrant>\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>");
         */
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier6::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = foodMartContext.getConnection(props);

        // <Level>.members inside regular crossjoin returns the correct list of
        // product members
        final String expected =
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Gender].[Gender].[All Gender], [Product].[Product].[Drink]}\n"
            + "Row #0: 24,597\n";

        final String mdx =
            "select {[Measures].[Unit Sales]} ON COLUMNS, "
            + " Crossjoin({[Gender].[All Gender]}, "
            + "[Product].[Product Family].Members) ON ROWS "
            + "from [Sales]";
        TestUtil.assertQueryReturns(connection, mdx, expected);
        checkQuery(connection, mdx);

        // with bug MONDRIAN-397, <Level>.members inside non empty crossjoin did
        // not return the correct list
        final String mdx2 =
            "select {[Measures].[Unit Sales]} ON COLUMNS, "
            + "NON EMPTY Crossjoin({[Gender].[All Gender]}, "
            + "[Product].[Product Family].Members) ON ROWS "
            + "from [Sales]";
        TestUtil.assertQueryReturns(connection, mdx2, expected);
        checkQuery(connection, mdx2);
    }

    /**
     * Testcase for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-406">
     * MONDRIAN-406, "Rollup policy doesn't work for members
     * that are implicitly visible"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testGoodman(Context foodMartContext) {
        final String query = "select {[Measures].[Unit Sales]} ON COLUMNS,\n"
            + "Hierarchize(Union(Union(Union({[Store].[All Stores]},"
            + " [Store].[All Stores].Children),"
            + " [Store].[All Stores].[USA].Children),"
            + " [Store].[All Stores].[USA].[CA].Children)) ON ROWS\n"
            + "from [Sales]\n"
            + "where [Time].[1997]";

        // Note that total for [Store].[All Stores] and [Store].[USA] is sum
        // of visible children [Store].[CA] and [Store].[OR].[Portland].
        setGoodmanContext(foodMartContext, RollupPolicyType.PARTIAL);
        ConnectionProps props = new RolapConnectionPropsR(List.of("California manager"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        Connection connection = foodMartContext.getConnection(props);
        TestUtil.assertQueryReturns(
    		connection,
            query,
            "Axis #0:\n"
            + "{[Time].[Time].[1997]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[All Stores]}\n"
            + "{[Store].[Store].[USA]}\n"
            + "{[Store].[Store].[USA].[CA]}\n"
            + "{[Store].[Store].[USA].[CA].[Alameda]}\n"
            + "{[Store].[Store].[USA].[CA].[Beverly Hills]}\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[Store].[USA].[CA].[San Francisco]}\n"
            + "{[Store].[Store].[USA].[OR]}\n"
            + "Row #0: 100,827\n"
            + "Row #1: 100,827\n"
            + "Row #2: 74,748\n"
            + "Row #3: \n"
            + "Row #4: 21,333\n"
            + "Row #5: 25,663\n"
            + "Row #6: 25,635\n"
            + "Row #7: 2,117\n"
            + "Row #8: 26,079\n");

        ((TestContext)foodMartContext).setCatalogMappingSupplier(new FoodmartMappingSupplier());
        setGoodmanContext(foodMartContext, RollupPolicyType.FULL);
        props = new RolapConnectionPropsR(List.of("California manager"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        connection = foodMartContext.getConnection(props);
        TestUtil.assertQueryReturns(
    		connection,
            query,
            "Axis #0:\n"
            + "{[Time].[Time].[1997]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[All Stores]}\n"
            + "{[Store].[Store].[USA]}\n"
            + "{[Store].[Store].[USA].[CA]}\n"
            + "{[Store].[Store].[USA].[CA].[Alameda]}\n"
            + "{[Store].[Store].[USA].[CA].[Beverly Hills]}\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[Store].[USA].[CA].[San Francisco]}\n"
            + "{[Store].[Store].[USA].[OR]}\n"
            + "Row #0: 266,773\n"
            + "Row #1: 266,773\n"
            + "Row #2: 74,748\n"
            + "Row #3: \n"
            + "Row #4: 21,333\n"
            + "Row #5: 25,663\n"
            + "Row #6: 25,635\n"
            + "Row #7: 2,117\n"
            + "Row #8: 67,659\n");

        ((TestContext)foodMartContext).setCatalogMappingSupplier(new FoodmartMappingSupplier());
        setGoodmanContext(foodMartContext, RollupPolicyType.HIDDEN);
        props = new RolapConnectionPropsR(List.of("California manager"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        connection = foodMartContext.getConnection(props);
        TestUtil.assertQueryReturns(
    		connection,
            query,
            "Axis #0:\n"
            + "{[Time].[Time].[1997]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[All Stores]}\n"
            + "{[Store].[Store].[USA]}\n"
            + "{[Store].[Store].[USA].[CA]}\n"
            + "{[Store].[Store].[USA].[CA].[Alameda]}\n"
            + "{[Store].[Store].[USA].[CA].[Beverly Hills]}\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[Store].[USA].[CA].[San Francisco]}\n"
            + "{[Store].[Store].[USA].[OR]}\n"
            + "Row #0: \n"
            + "Row #1: \n"
            + "Row #2: 74,748\n"
            + "Row #3: \n"
            + "Row #4: 21,333\n"
            + "Row #5: 25,663\n"
            + "Row #6: 25,635\n"
            + "Row #7: 2,117\n"
            + "Row #8: \n");
        checkQuery(connection, query);
    }

    private static void setGoodmanContext(Context context, final RollupPolicyType policy) {
        context.getCatalogCache().clear();
        CatalogMapping catalogMapping = ((RolapContext) context).getCatalogMapping();
        ((TestContext)context).setCatalogMappingSupplier(new SchemaModifiers.AccessControlTestModifier42(catalogMapping, policy));
    }

    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-402">
     * MONDRIAN-402, "Bug in RolapCubeHierarchy.hashCode() ?"</a>.
     * Access-control elements for hierarchies with
     * same name in different cubes could not be distinguished.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testBugMondrian402(Context foodMartContext) {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier7::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("California manager"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = foodMartContext.getConnection(props);
        assertHierarchyAccess(
    		connection, AccessHierarchy.NONE, "Sales", "Store");
        assertHierarchyAccess(
    		connection,
            AccessHierarchy.CUSTOM,
            "Sales Ragged",
            "Store");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testPartialRollupParentChildHierarchy(Context foodMartContext) {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier8::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Buggy Role"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = foodMartContext.getConnection(props);

        final String mdx = "select\n"
            + "  {[Measures].[Number of Employees]} on columns,\n"
            + "  {[Store]} on rows\n"
            + "from HR";
        TestUtil.assertQueryReturns(
    		connection,
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Number of Employees]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[All Stores]}\n"
            + "Row #0: 1\n");
        checkQuery(connection, mdx);

        final String mdx2 = "select\n"
            + "  {[Measures].[Number of Employees]} on columns,\n"
            + "  {[Employees]} on rows\n"
            + "from HR";
        TestUtil.assertQueryReturns(
    		connection,
            mdx2,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Number of Employees]}\n"
            + "Axis #2:\n"
            + "{[Employees].[Employees].[All Employees]}\n"
            + "Row #0: 1\n");
        checkQuery(connection, mdx2);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testParentChildUserDefinedRole(Context foodMartContext)
    {
        final Connection connection = foodMartContext.getConnectionWithDefaultRole();
        final Role savedRole = connection.getRole();
        try {
            // Run queries as top-level employee.
            connection.setRole(
                new PeopleRole(
                    savedRole, connection.getCatalog(), "Sheri Nowmer"));
            TestUtil.assertExprReturns(
        		connection,
        		"HR",
                "[Employees].Members.Count",
                "1,156");

            // Level 2 employee
            connection.setRole(
                new PeopleRole(
                    savedRole, connection.getCatalog(), "Derrick Whelply"));
            TestUtil.assertExprReturns(
        		connection,
        		"HR",
                "[Employees].Members.Count",
                "605");
            TestUtil.assertAxisReturns(
        		connection,
        		"HR",
                "Head([Employees].Members, 4),"
                + "Tail([Employees].Members, 2)",
                "[Employees].[Employees].[All Employees]\n"
                + "[Employees].[Employees].[Sheri Nowmer]\n"
                + "[Employees].[Employees].[Sheri Nowmer].[Derrick Whelply]\n"
                + "[Employees].[Employees].[Sheri Nowmer].[Derrick Whelply].[Beverly Baker]\n"
                + "[Employees].[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Ed Young].[Gregory Whiting].[Merrill Steel]\n"
                + "[Employees].[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Ed Young].[Gregory Whiting].[Melissa Marple]");

            // Leaf employee
            connection.setRole(
                new PeopleRole(
                    savedRole, connection.getCatalog(), "Ann Weyerhaeuser"));
            TestUtil.assertExprReturns(
        		connection,
        		"HR",
                "[Employees].[Employees].Members.Count",
                "7");
            TestUtil.assertAxisReturns(
        		connection,
        		"HR",
                "[Employees].Members",
                "[Employees].[Employees].[All Employees]\n"
                + "[Employees].[Employees].[Sheri Nowmer]\n"
                + "[Employees].[Employees].[Sheri Nowmer].[Derrick Whelply]\n"
                + "[Employees].[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges]\n"
                + "[Employees].[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Cody Goldey]\n"
                + "[Employees].[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Cody Goldey].[Shanay Steelman]\n"
                + "[Employees].[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Cody Goldey].[Shanay Steelman].[Ann Weyerhaeuser]");
        } finally {
            connection.setRole(savedRole);
        }
    }

    /**
     * Test case for
     * <a href="http://jira.pentaho.com/browse/BISERVER-1574">BISERVER-1574,
     * "Cube role rollupPolicy='partial' failure"</a>. The problem was a
     * NullPointerException in
     * {@link CatalogReader#getMemberParent(org.eclipse.daanse.olap.api.element.Member)} when called
     * on a members returned in a result set. JPivot calls that method but
     * Mondrian normally does not.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testBugBiserver1574(Context foodMartContext) {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier9::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = foodMartContext.getConnection(props);
        final String mdx =
            "select {([Measures].[Store Invoice], [Store Size in SQFT].[Store Size in SQFT].[All Store Size in SQFTs])} ON COLUMNS,\n"
            + "  {[Warehouse].[Warehouse].[All Warehouses]} ON ROWS\n"
            + "from [Warehouse]";
        checkQuery(connection, mdx);
        TestUtil.assertQueryReturns(
    		connection,
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Store Invoice], [Store Size in SQFT].[Store Size in SQFT].[All Store Size in SQFTs]}\n"
            + "Axis #2:\n"
            + "{[Warehouse].[Warehouse].[All Warehouses]}\n"
            + "Row #0: 4,042.96\n");
    }

    /**
     * Testcase for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-435">
     * MONDRIAN-435, "Internal error in HierarchizeArrayComparator"</a>. Occurs
     * when apply Hierarchize function to tuples on a hierarchy with
     * partial-rollup.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testBugMondrian435(Context foodMartContext) {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier9::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = foodMartContext.getConnection(props);

        // minimal testcase
    	TestUtil.assertQueryReturns(
			connection,
            "select hierarchize("
            + "    crossjoin({[Store Size in SQFT], [Store Size in SQFT].Children}, {[Product]})"
            + ") on 0,"
            + "[Store Type].Members on 1 from [Warehouse]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store Size in SQFT].[Store Size in SQFT].[All Store Size in SQFTs], [Product].[Product].[All Products]}\n"
            + "{[Store Size in SQFT].[Store Size in SQFT].[20319], [Product].[Product].[All Products]}\n"
            + "Axis #2:\n"
            + "{[Store Type].[Store Type].[All Store Types]}\n"
            + "{[Store Type].[Store Type].[Supermarket]}\n"
            + "Row #0: 4,042.96\n"
            + "Row #0: 4,042.96\n"
            + "Row #1: 4,042.96\n"
            + "Row #1: 4,042.96\n");

        // explicit tuples, not crossjoin
    	TestUtil.assertQueryReturns(
			connection,
            "select hierarchize("
            + "    { ([Store Size in SQFT], [Product]),\n"
            + "      ([Store Size in SQFT].[20319], [Product].[Food]),\n"
            + "      ([Store Size in SQFT], [Product].[Drink].[Dairy]),\n"
            + "      ([Store Size in SQFT].[20319], [Product]) }\n"
            + ") on 0,"
            + "[Store Type].Members on 1 from [Warehouse]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store Size in SQFT].[Store Size in SQFT].[All Store Size in SQFTs], [Product].[Product].[All Products]}\n"
            + "{[Store Size in SQFT].[Store Size in SQFT].[All Store Size in SQFTs], [Product].[Product].[Drink].[Dairy]}\n"
            + "{[Store Size in SQFT].[Store Size in SQFT].[20319], [Product].[Product].[All Products]}\n"
            + "{[Store Size in SQFT].[Store Size in SQFT].[20319], [Product].[Product].[Food]}\n"
            + "Axis #2:\n"
            + "{[Store Type].[Store Type].[All Store Types]}\n"
            + "{[Store Type].[Store Type].[Supermarket]}\n"
            + "Row #0: 4,042.96\n"
            + "Row #0: 82.454\n"
            + "Row #0: 4,042.96\n"
            + "Row #0: 2,696.758\n"
            + "Row #1: 4,042.96\n"
            + "Row #1: 82.454\n"
            + "Row #1: 4,042.96\n"
            + "Row #1: 2,696.758\n");

        // extended testcase; note that [Store Size in SQFT].Parent is null,
        // so disappears
    	TestUtil.assertQueryReturns(
			connection,
            "select non empty hierarchize("
            + "union("
            + "  union("
            + "    crossjoin({[Store Size in SQFT]}, {[Product]}),"
            + "    crossjoin({[Store Size in SQFT], [Store Size in SQFT].Children}, {[Product]}),"
            + "    all),"
            + "  union("
            + "    crossjoin({[Store Size in SQFT].Parent}, {[Product].[Drink]}),"
            + "    crossjoin({[Store Size in SQFT].Children}, {[Product].[Food]}),"
            + "    all),"
            + "  all)) on 0,"
            + "[Store Type].Members on 1 from [Warehouse]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store Size in SQFT].[Store Size in SQFT].[All Store Size in SQFTs], [Product].[Product].[All Products]}\n"
            + "{[Store Size in SQFT].[Store Size in SQFT].[All Store Size in SQFTs], [Product].[Product].[All Products]}\n"
            + "{[Store Size in SQFT].[Store Size in SQFT].[20319], [Product].[Product].[All Products]}\n"
            + "{[Store Size in SQFT].[Store Size in SQFT].[20319], [Product].[Product].[Food]}\n"
            + "Axis #2:\n"
            + "{[Store Type].[Store Type].[All Store Types]}\n"
            + "{[Store Type].[Store Type].[Supermarket]}\n"
            + "Row #0: 4,042.96\n"
            + "Row #0: 4,042.96\n"
            + "Row #0: 4,042.96\n"
            + "Row #0: 2,696.758\n"
            + "Row #1: 4,042.96\n"
            + "Row #1: 4,042.96\n"
            + "Row #1: 4,042.96\n"
            + "Row #1: 2,696.758\n");

    	TestUtil.assertQueryReturns(
			connection,
            "select Hierarchize(\n"
            + "  CrossJoin\n("
            + "    CrossJoin(\n"
            + "      {[Product].[All Products], "
            + "       [Product].[Food],\n"
            + "       [Product].[Food].[Eggs],\n"
            + "       [Product].[Drink].[Dairy]},\n"
            + "      [Store Type].MEMBERS),\n"
            + "    [Store Size in SQFT].MEMBERS),\n"
            + "  PRE) on 0\n"
            + "from [Warehouse]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Product].[Product].[All Products], [Store Type].[Store Type].[All Store Types], [Store Size in SQFT].[Store Size in SQFT].[All Store Size in SQFTs]}\n"
            + "{[Product].[Product].[All Products], [Store Type].[Store Type].[All Store Types], [Store Size in SQFT].[Store Size in SQFT].[20319]}\n"
            + "{[Product].[Product].[All Products], [Store Type].[Store Type].[Supermarket], [Store Size in SQFT].[Store Size in SQFT].[All Store Size in SQFTs]}\n"
            + "{[Product].[Product].[All Products], [Store Type].[Store Type].[Supermarket], [Store Size in SQFT].[Store Size in SQFT].[20319]}\n"
            + "{[Product].[Product].[Drink].[Dairy], [Store Type].[Store Type].[All Store Types], [Store Size in SQFT].[Store Size in SQFT].[All Store Size in SQFTs]}\n"
            + "{[Product].[Product].[Drink].[Dairy], [Store Type].[Store Type].[All Store Types], [Store Size in SQFT].[Store Size in SQFT].[20319]}\n"
            + "{[Product].[Product].[Drink].[Dairy], [Store Type].[Store Type].[Supermarket], [Store Size in SQFT].[Store Size in SQFT].[All Store Size in SQFTs]}\n"
            + "{[Product].[Product].[Drink].[Dairy], [Store Type].[Store Type].[Supermarket], [Store Size in SQFT].[Store Size in SQFT].[20319]}\n"
            + "{[Product].[Product].[Food], [Store Type].[Store Type].[All Store Types], [Store Size in SQFT].[Store Size in SQFT].[All Store Size in SQFTs]}\n"
            + "{[Product].[Product].[Food], [Store Type].[Store Type].[All Store Types], [Store Size in SQFT].[Store Size in SQFT].[20319]}\n"
            + "{[Product].[Product].[Food], [Store Type].[Store Type].[Supermarket], [Store Size in SQFT].[Store Size in SQFT].[All Store Size in SQFTs]}\n"
            + "{[Product].[Product].[Food], [Store Type].[Store Type].[Supermarket], [Store Size in SQFT].[Store Size in SQFT].[20319]}\n"
            + "{[Product].[Product].[Food].[Eggs], [Store Type].[Store Type].[All Store Types], [Store Size in SQFT].[Store Size in SQFT].[All Store Size in SQFTs]}\n"
            + "{[Product].[Product].[Food].[Eggs], [Store Type].[Store Type].[All Store Types], [Store Size in SQFT].[Store Size in SQFT].[20319]}\n"
            + "{[Product].[Product].[Food].[Eggs], [Store Type].[Store Type].[Supermarket], [Store Size in SQFT].[Store Size in SQFT].[All Store Size in SQFTs]}\n"
            + "{[Product].[Product].[Food].[Eggs], [Store Type].[Store Type].[Supermarket], [Store Size in SQFT].[Store Size in SQFT].[20319]}\n"
            + "Row #0: 4,042.96\n"
            + "Row #0: 4,042.96\n"
            + "Row #0: 4,042.96\n"
            + "Row #0: 4,042.96\n"
            + "Row #0: 82.454\n"
            + "Row #0: 82.454\n"
            + "Row #0: 82.454\n"
            + "Row #0: 82.454\n"
            + "Row #0: 2,696.758\n"
            + "Row #0: 2,696.758\n"
            + "Row #0: 2,696.758\n"
            + "Row #0: 2,696.758\n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n");
    }

    /**
     * Testcase for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-436">
     * MONDRIAN-436, "SubstitutingMemberReader.getMemberBuilder gives
     * UnsupportedOperationException"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testBugMondrian436(Context foodMartContext) {
        ((TestConfig)foodMartContext.getConfig()).setEnableNativeCrossJoin(true);
        ((TestConfig)foodMartContext.getConfig()).setEnableNativeFilter(true);
        SystemWideProperties.instance().EnableNativeNonEmpty = true;
        ((TestConfig)foodMartContext.getConfig()).setEnableNativeTopCount(true);
        ((TestConfig)foodMartContext.getConfig()).setExpandNonNative(true);

        // Run with native enabled, then with whatever properties are set for
        // this test run.
        checkBugMondrian436(foodMartContext);
        SystemWideProperties.instance().populateInitial();
        ((TestContext)foodMartContext).setCatalogMappingSupplier(new FoodmartMappingSupplier());
        checkBugMondrian436(foodMartContext);
    }

    private void checkBugMondrian436(Context foodMartContext) {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier9::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = foodMartContext.getConnection(props);

    	TestUtil.assertQueryReturns(
			connection,
            "select non empty {[Measures].[Units Ordered],\n"
            + "            [Measures].[Units Shipped]} on 0,\n"
            + "non empty hierarchize(\n"
            + "    union(\n"
            + "        crossjoin(\n"
            + "            {[Store Size in SQFT]},\n"
            + "            {[Product].[Drink],\n"
            + "             [Product].[Food],\n"
            + "             [Product].[Drink].[Dairy]}),\n"
            + "        crossjoin(\n"
            + "            {[Store Size in SQFT].[20319]},\n"
            + "            {[Product].Children}))) on 1\n"
            + "from [Warehouse]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Units Ordered]}\n"
            + "{[Measures].[Units Shipped]}\n"
            + "Axis #2:\n"
            + "{[Store Size in SQFT].[Store Size in SQFT].[All Store Size in SQFTs], [Product].[Product].[Drink]}\n"
            + "{[Store Size in SQFT].[Store Size in SQFT].[All Store Size in SQFTs], [Product].[Product].[Drink].[Dairy]}\n"
            + "{[Store Size in SQFT].[Store Size in SQFT].[All Store Size in SQFTs], [Product].[Product].[Food]}\n"
            + "{[Store Size in SQFT].[Store Size in SQFT].[20319], [Product].[Product].[Drink]}\n"
            + "{[Store Size in SQFT].[Store Size in SQFT].[20319], [Product].[Product].[Food]}\n"
            + "{[Store Size in SQFT].[Store Size in SQFT].[20319], [Product].[Product].[Non-Consumable]}\n"
            + "Row #0: 865.0\n"
            + "Row #0: 767.0\n"
            + "Row #1: 195.0\n"
            + "Row #1: 182.0\n"
            + "Row #2: 6065.0\n"
            + "Row #2: 5723.0\n"
            + "Row #3: 865.0\n"
            + "Row #3: 767.0\n"
            + "Row #4: 6065.0\n"
            + "Row #4: 5723.0\n"
            + "Row #5: 2179.0\n"
            + "Row #5: 2025.0\n");
    }

    /**
     * Tests that hierarchy-level access control works on a virtual cube.
     * See bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-456">
     * MONDRIAN-456, "Roles and virtual cubes"</a>.
     */
    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testVirtualCube(Context foodMartContext) {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier10::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("VCRole"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = foodMartContext.getConnection(props);
    	TestUtil.assertQueryReturns(
			connection,
            "select [Store].Members on 0 from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[All Stores]}\n"
            + "{[Store].[USA]}\n"
            + "{[Store].[USA].[CA]}\n"
            + "{[Store].[USA].[CA].[Alameda]}\n"
            + "{[Store].[USA].[CA].[Alameda].[HQ]}\n"
            + "{[Store].[USA].[CA].[Beverly Hills]}\n"
            + "{[Store].[USA].[CA].[Beverly Hills].[Store 6]}\n"
            + "{[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[USA].[CA].[San Diego].[Store 24]}\n"
            + "{[Store].[USA].[CA].[San Francisco]}\n"
            + "{[Store].[USA].[CA].[San Francisco].[Store 14]}\n"
            + "Row #0: 159,167.84\n"
            + "Row #0: 159,167.84\n"
            + "Row #0: 159,167.84\n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: 45,750.24\n"
            + "Row #0: 45,750.24\n"
            + "Row #0: 54,431.14\n"
            + "Row #0: 54,431.14\n"
            + "Row #0: 4,441.18\n"
            + "Row #0: 4,441.18\n");
    }

    /**
     * this tests the fix for
     * http://jira.pentaho.com/browse/BISERVER-2491
     * rollupPolicy=partial and queries to upper members don't work
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testBugBiserver2491(Context foodMartContext) {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier11::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        Connection connection = foodMartContext.getConnection(props);

        final String firstBrokenMdx =
            "select [Measures].[Unit Sales] ON COLUMNS, {[Store].[Store Country].Members} ON ROWS from [Sales]";

        checkQuery(connection, firstBrokenMdx);
        TestUtil.assertQueryReturns(
    		connection,
            firstBrokenMdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA]}\n"
            + "Row #0: 49,085\n");

        final String secondBrokenMdx =
            "select [Measures].[Unit Sales] ON COLUMNS, "
            + "Descendants([Store],[Store].[Store Name]) ON ROWS from [Sales]";
        checkQuery(connection, secondBrokenMdx);
        TestUtil.assertQueryReturns(
    		connection,
            secondBrokenMdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[CA].[Alameda].[HQ]}\n"
            + "{[Store].[Store].[USA].[CA].[Beverly Hills].[Store 6]}\n"
            + "{[Store].[Store].[USA].[CA].[San Diego].[Store 24]}\n"
            + "{[Store].[Store].[USA].[CA].[San Francisco].[Store 14]}\n"
            + "Row #0: \n"
            + "Row #1: 21,333\n"
            + "Row #2: 25,635\n"
            + "Row #3: 2,117\n");
    }

    /**
     * Test case for bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-622">MONDRIAN-622,
     * "Poor performance with large union role"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testBugMondrian622(Context context) {
        StringBuilder buf = new StringBuilder();
        StringBuilder buf2 = new StringBuilder();
        final String cubeName = "Sales with multiple customers";
        Connection connection = context.getConnectionWithDefaultRole();
        final Result result = TestUtil.executeQuery(
    		connection,
            "select [Customers].[City].Members on 0 from [Sales]");

        final long t0 = System.currentTimeMillis();
        context.getCatalogCache().clear();
        CatalogMapping schema = ((RolapContext) context).getCatalogMapping();
        ((TestContext)context).setCatalogMappingSupplier(new SchemaModifiers.AccessControlTestModifier12(schema, result));
        ConnectionProps props = new RolapConnectionPropsR(List.of("Test"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        connection = context.getConnection(props);
        TestUtil.executeQuery(connection, "select from [" + cubeName + "]");
        final long t1 = System.currentTimeMillis();
//      System.out.println("Elapsed=" + (t1 - t0) + " millis");
//      System.out.println(
//          "RoleImpl.accessCount=" + RoleImpl.accessCallCount);
//      testContext1.executeQuery(
//          "select from [Sales with multiple customers]");
//      final long t2 = System.currentTimeMillis();
//      System.out.println("Elapsed=" + (t2 - t1) + " millis");
//      System.out.println("RoleImpl.accessCount=" + RoleImpl.accessCallCount);
    }

    /**
     * Test case for bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-694">MONDRIAN-694,
     * "Incorrect handling of child/parent relationship with hierarchy
     * grants"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testBugMondrian694(Context foodMartContext) {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier14::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("REG1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = foodMartContext.getConnection(props);

        // With bug MONDRIAN-694 returns 874.80, should return 79.20.
        // Test case is minimal: doesn't happen without the Crossjoin, or
        // without the NON EMPTY, or with [Employees] as opposed to
        // [Employees].[All Employees], or with [Department].[All Departments].
    	TestUtil.assertQueryReturns(
			connection,
            "select NON EMPTY {[Measures].[Org Salary]} ON COLUMNS,\n"
            + "NON EMPTY Crossjoin({[Department].[14]}, {[Employees].[All Employees]}) ON ROWS\n"
            + "from [HR]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Org Salary]}\n"
            + "Axis #2:\n"
            + "{[Department].[Department].[14], [Employees].[Employees].[All Employees]}\n"
            + "Row #0: $97.20\n");

        // This query gave the right answer, even with MONDRIAN-694.
    	TestUtil.assertQueryReturns(
			connection,
            "select NON EMPTY {[Measures].[Org Salary]} ON COLUMNS, \n"
            + "NON EMPTY Hierarchize(Crossjoin({[Department].[14]}, {[Employees].[All Employees], [Employees].Children})) ON ROWS \n"
            + "from [HR] ",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Org Salary]}\n"
            + "Axis #2:\n"
            + "{[Department].[Department].[14], [Employees].[Employees].[All Employees]}\n"
            + "{[Department].[Department].[14], [Employees].[Employees].[Sheri Nowmer]}\n"
            + "Row #0: $97.20\n"
            + "Row #1: $97.20\n");

        // Original test case, not quite minimal. With MONDRIAN-694, returns
        // $874.80 for [All Employees].
    	TestUtil.assertQueryReturns(
			connection,
            "select NON EMPTY {[Measures].[Org Salary]} ON COLUMNS, \n"
            + "NON EMPTY Hierarchize(Union(Crossjoin({[Department].[All Departments].[14]}, {[Employees].[All Employees]}), Crossjoin({[Department].[All Departments].[14]}, [Employees].[All Employees].Children))) ON ROWS \n"
            + "from [HR]  ",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Org Salary]}\n"
            + "Axis #2:\n"
            + "{[Department].[Department].[14], [Employees].[Employees].[All Employees]}\n"
            + "{[Department].[Department].[14], [Employees].[Employees].[Sheri Nowmer]}\n"
            + "Row #0: $97.20\n"
            + "Row #1: $97.20\n");

    	TestUtil.assertQueryReturns(
			connection,
            "select NON EMPTY {[Measures].[Org Salary]} ON COLUMNS, \n"
            + "NON EMPTY Crossjoin(Hierarchize(Union({[Employees].[All Employees]}, [Employees].[All Employees].Children)), {[Department].[14]}) ON ROWS \n"
            + "from [HR] ",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Org Salary]}\n"
            + "Axis #2:\n"
            + "{[Employees].[Employees].[All Employees], [Department].[Department].[14]}\n"
            + "{[Employees].[Employees].[Sheri Nowmer], [Department].[Department].[14]}\n"
            + "Row #0: $97.20\n"
            + "Row #1: $97.20\n");
    }

    /**
     * Test case for bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-722">MONDRIAN-722, "If
     * ignoreInvalidMembers=true, should ignore grants with invalid
     * members"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testBugMondrian722(Context foodMartContext) {
        ((TestConfig)foodMartContext.getConfig()).setIgnoreInvalidMembers(true);
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier15::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("CTO"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        Connection connection = foodMartContext.getConnection(props);
        TestUtil.assertQueryReturns(
        		connection,
                "select [Measures] on 0,\n"
                + " Hierarchize(\n"
                + "   {[Customers].[USA].Children,\n"
                + "    [Customers].[USA].[CA].Children}) on 1\n"
                + "from [Sales]",
                "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "Axis #2:\n"
                + "{[Customers].[Customers].[USA].[CA]}\n"
                + "{[Customers].[Customers].[USA].[CA].[Los Angeles]}\n"
                + "{[Customers].[Customers].[USA].[CA].[San Francisco]}\n"
                + "Row #0: 74,748\n"
                + "Row #1: 2,009\n"
                + "Row #2: 88\n");
    }

    /**
     * Test case for bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-746">MONDRIAN-746,
     * "Report returns stack trace when turning on subtotals on a hierarchy with
     * top level hidden"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testCalcMemberLevel(Context foodMartContext) {
    	Connection connection = foodMartContext.getConnectionWithDefaultRole();
        checkCalcMemberLevel(connection);
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier16::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        connection = foodMartContext.getConnection(props);
        checkCalcMemberLevel(connection);
    }

    /**
     * Test for bug MONDRIAN-568. Grants on OLAP elements are validated
     * by name, thus granting implicit access to all cubes which have
     * a dimension with the same name.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testBugMondrian568(Context foodMartContext) {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier17::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = foodMartContext.getConnection(props);
        assertMemberAccess(
        		connection,
                AccessMember.NONE,
                "[Measures].[Store Cost]");

        props = new RolapConnectionPropsR(List.of("Role1", "Role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	connection = foodMartContext.getConnection(props);
        assertMemberAccess(
        		connection,
            AccessMember.NONE,
            "[Measures].[Store Cost]");
    }

    private void checkCalcMemberLevel(Connection connection) {
        Result result = TestUtil.executeQuery(
    		connection,
            "with member [Store].[USA].[CA].[Foo] as\n"
            + " 1\n"
            + "select {[Measures].[Unit Sales]} on columns,\n"
            + "{[Store].[USA].[CA],\n"
            + " [Store].[USA].[CA].[Los Angeles],\n"
            + " [Store].[USA].[CA].[Foo]} on rows\n"
            + "from [Sales]");
        final List<Position> rowPos = result.getAxes()[1].getPositions();
        final Member member0 = rowPos.get(0).get(0);
        assertEquals("CA", member0.getName());
        assertEquals("Store State", member0.getLevel().getName());
        final Member member1 = rowPos.get(1).get(0);
        assertEquals("Los Angeles", member1.getName());
        assertEquals("Store City", member1.getLevel().getName());
        final Member member2 = rowPos.get(2).get(0);
        assertEquals("Foo", member2.getName());
        assertEquals("Store City", member2.getLevel().getName());
    }

    /**
     * Testcase for bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-935">MONDRIAN-935,
     * "no results when some level members in a member grant have no data"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testBugMondrian935(Context foodMartContext) {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier18::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = foodMartContext.getConnection(props);
    	TestUtil.assertQueryReturns(
			connection,
            "select [Measures] on 0,\n"
            + "[Customers].[USA].Children * [Store Type].Children on 1\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Customers].[Customers].[USA].[CA], [Store Type].[Store Type].[Supermarket]}\n"
            + "{[Customers].[Customers].[USA].[WA], [Store Type].[Store Type].[Supermarket]}\n"
            + "Row #0: 1,118\n"
            + "Row #1: 73,178\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testDimensionGrant(Context foodMartContext) throws Exception {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier19::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = foodMartContext.getConnection(props);
    	TestUtil.assertAxisReturns(
			connection, "Sales",
            "[Education Level].[Education Level].Members",
            "[Education Level].[Education Level].[All Education Levels]\n"
            + "[Education Level].[Education Level].[Bachelors Degree]\n"
            + "[Education Level].[Education Level].[Graduate Degree]\n"
            + "[Education Level].[Education Level].[High School Degree]\n"
            + "[Education Level].[Education Level].[Partial College]\n"
            + "[Education Level].[Education Level].[Partial High School]");
    	TestUtil.assertAxisThrows(
			connection,
            "[Customers].Members",
            "Failed to parse query 'select {[Customers].Members} on columns from Sales'", "Sales");
    	TestUtil.assertQueryReturns(
			connection,
            "select {[Education Level].Members} on columns, {[Measures].[Unit Sales]} on rows from Sales",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Education Level].[Education Level].[All Education Levels]}\n"
            + "{[Education Level].[Education Level].[Bachelors Degree]}\n"
            + "{[Education Level].[Education Level].[Graduate Degree]}\n"
            + "{[Education Level].[Education Level].[High School Degree]}\n"
            + "{[Education Level].[Education Level].[Partial College]}\n"
            + "{[Education Level].[Education Level].[Partial High School]}\n"
            + "Axis #2:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: 68,839\n"
            + "Row #0: 15,570\n"
            + "Row #0: 78,664\n"
            + "Row #0: 24,545\n"
            + "Row #0: 79,155\n");
        props = new RolapConnectionPropsR(List.of("Role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	connection = foodMartContext.getConnection(props);
    	TestUtil.assertAxisThrows(
			connection,
            "[Customers].Members",
            "Failed to parse query 'select {[Customers].Members} on columns from Sales'", "Sales");
        props = new RolapConnectionPropsR(List.of("Role3"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        connection = foodMartContext.getConnection(props);
    	TestUtil.assertQueryThrows(
			connection,
            "select {[Education Level].Members} on columns, {[Measures].[Unit Sales]} on rows from Sales",
            "Failed to parse query 'select {[Education Level].Members} on columns, {[Measures].[Unit Sales]} on rows from Sales'");
    }

    // ~ Inner classes =========================================================

    public static class PeopleRole extends DelegatingRole {
        private final String repName;

        public PeopleRole(Role role, Catalog schema, String repName) {
            super(((RoleImpl)role).makeMutableClone());
            this.repName = repName;
            defineGrantsForUser(schema);
        }

        private void defineGrantsForUser(Catalog schema) {
            RoleImpl role = (RoleImpl)this.role;
            role.grant(schema, AccessCatalog.NONE);

            Cube cube = schema.lookupCube("HR").orElseThrow();
            role.grant(cube, AccessCube.ALL);

            Hierarchy hierarchy = cube.lookupHierarchy(
                new IdImpl.NameSegmentImpl("Employees"), false);

            Level[] levels = hierarchy.getLevels();
            Level topLevel = levels[1];

            role.grant(hierarchy, AccessHierarchy.CUSTOM, null, null, RollupPolicy.FULL);
            role.grant(hierarchy.getAllMember(), AccessMember.NONE);

            boolean foundMember = false;

            List <Member> members =
                schema.getCatalogReaderWithDefaultRole().withLocus()
                    .getLevelMembers(topLevel, true);

            for (Member member : members) {
                if (member.getUniqueName().contains("[" + repName + "]")) {
                    foundMember = true;
                    role.grant(member, AccessMember.ALL);
                }
            }
            assertTrue(foundMember);
        }
    }

    /**
     * This is a test for MONDRIAN-1030. When the top level of a hierarchy
     * is not accessible and a partial rollup policy is used, the results would
     * be returned as those of the first member of those accessible only.
     *
     * <p>ie: If a union of roles give access to two two sibling root members
     * and the level to which they belong is not included in a query, the
     * returned cell data would be that of the first sibling and would exclude
     * those of the second.
     *
     * <p>This is because the RolapEvaluator cannot represent default members
     * as multiple members (only a single member is the default member) and
     * because the default member is not the 'all member', it adds a constrain
     * to the SQL for the first member only.
     *
     * <p>Currently, Mondrian disguises the root member in the evaluator as a
     * RestrictedMemberReader.MultiCardinalityDefaultMember. Later,
     * RolapHierarchy.LimitedRollupSubstitutingMemberReader will recognize it
     * and use the correct rollup policy on the parent member to generate
     * correct SQL.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testMondrian1030(Context foodMartContext) throws Exception {
        final String mdx1 =
            "With\n"
            + "Set [*NATIVE_CJ_SET] as 'NonEmptyCrossJoin([*BASE_MEMBERS_Customers],[*BASE_MEMBERS_Product])'\n"
            + "Set [*SORTED_ROW_AXIS] as 'Order([*CJ_ROW_AXIS],[Customers].CurrentMember.OrderKey,BASC,[Education Level].CurrentMember.OrderKey,BASC)'\n"
            + "Set [*BASE_MEMBERS_Customers] as '[Customers].[City].Members'\n"
            + "Set [*BASE_MEMBERS_Product] as '[Education Level].Members'\n"
            + "Set [*BASE_MEMBERS_Measures] as '{[Measures].[*FORMATTED_MEASURE_0]}'\n"
            + "Set [*CJ_ROW_AXIS] as 'Generate([*NATIVE_CJ_SET], {([Customers].currentMember,[Education Level].currentMember)})'\n"
            + "Set [*CJ_COL_AXIS] as '[*NATIVE_CJ_SET]'\n"
            + "Member [Measures].[*FORMATTED_MEASURE_0] as '[Measures].[Unit Sales]', FORMAT_STRING = '#,###', SOLVE_ORDER=400\n"
            + "Select\n"
            + "[*BASE_MEMBERS_Measures] on columns,\n"
            + "Non Empty [*SORTED_ROW_AXIS] on rows\n"
            + "From [Sales] \n";
        final String mdx2 =
            "With\n"
            + "Set [*BASE_MEMBERS_Product] as '[Education Level].Members'\n"
            + "Set [*BASE_MEMBERS_Measures] as '{[Measures].[*FORMATTED_MEASURE_0]}'\n"
            + "Member [Measures].[*FORMATTED_MEASURE_0] as '[Measures].[Unit Sales]', FORMAT_STRING = '#,###', SOLVE_ORDER=400\n"
            + "Select\n"
            + "[*BASE_MEMBERS_Measures] on columns,\n"
            + "Non Empty [*BASE_MEMBERS_Product] on rows\n"
            + "From [Sales] \n";
        // Control tests
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier20::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        Connection connection = foodMartContext.getConnection(props);
        TestUtil.assertQueryReturns(
    		connection,
            mdx1,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Customers].[Customers].[USA].[CA].[Coronado], [Education Level].[Education Level].[All Education Levels]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Coronado], [Education Level].[Education Level].[Bachelors Degree]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Coronado], [Education Level].[Education Level].[Graduate Degree]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Coronado], [Education Level].[Education Level].[High School Degree]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Coronado], [Education Level].[Education Level].[Partial College]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Coronado], [Education Level].[Education Level].[Partial High School]}\n"
            + "Row #0: 2,391\n"
            + "Row #1: 559\n"
            + "Row #2: 205\n"
            + "Row #3: 551\n"
            + "Row #4: 253\n"
            + "Row #5: 823\n");
        props = new RolapConnectionPropsR(List.of("Role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        connection = foodMartContext.getConnection(props);
        TestUtil.assertQueryReturns(
    		connection,
            mdx1,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Customers].[Customers].[USA].[CA].[Burbank], [Education Level].[Education Level].[All Education Levels]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Burbank], [Education Level].[Education Level].[Bachelors Degree]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Burbank], [Education Level].[Education Level].[Graduate Degree]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Burbank], [Education Level].[Education Level].[High School Degree]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Burbank], [Education Level].[Education Level].[Partial College]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Burbank], [Education Level].[Education Level].[Partial High School]}\n"
            + "Row #0: 3,086\n"
            + "Row #1: 914\n"
            + "Row #2: 126\n"
            + "Row #3: 1,029\n"
            + "Row #4: 286\n"
            + "Row #5: 731\n");
        props = new RolapConnectionPropsR(List.of("Role1", "Role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        connection = foodMartContext.getConnection(props);
        TestUtil.assertQueryReturns(
    		connection,
            mdx1,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Customers].[Customers].[USA].[CA].[Burbank], [Education Level].[Education Level].[All Education Levels]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Burbank], [Education Level].[Education Level].[Bachelors Degree]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Burbank], [Education Level].[Education Level].[Graduate Degree]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Burbank], [Education Level].[Education Level].[High School Degree]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Burbank], [Education Level].[Education Level].[Partial College]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Burbank], [Education Level].[Education Level].[Partial High School]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Coronado], [Education Level].[Education Level].[All Education Levels]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Coronado], [Education Level].[Education Level].[Bachelors Degree]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Coronado], [Education Level].[Education Level].[Graduate Degree]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Coronado], [Education Level].[Education Level].[High School Degree]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Coronado], [Education Level].[Education Level].[Partial College]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Coronado], [Education Level].[Education Level].[Partial High School]}\n"
            + "Row #0: 3,086\n"
            + "Row #1: 914\n"
            + "Row #2: 126\n"
            + "Row #3: 1,029\n"
            + "Row #4: 286\n"
            + "Row #5: 731\n"
            + "Row #6: 2,391\n"
            + "Row #7: 559\n"
            + "Row #8: 205\n"
            + "Row #9: 551\n"
            + "Row #10: 253\n"
            + "Row #11: 823\n");
        // Actual tests
        props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        connection = foodMartContext.getConnection(props);
        TestUtil.assertQueryReturns(
    		connection,
            mdx2,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Education Level].[Education Level].[All Education Levels]}\n"
            + "{[Education Level].[Education Level].[Bachelors Degree]}\n"
            + "{[Education Level].[Education Level].[Graduate Degree]}\n"
            + "{[Education Level].[Education Level].[High School Degree]}\n"
            + "{[Education Level].[Education Level].[Partial College]}\n"
            + "{[Education Level].[Education Level].[Partial High School]}\n"
            + "Row #0: 2,391\n"
            + "Row #1: 559\n"
            + "Row #2: 205\n"
            + "Row #3: 551\n"
            + "Row #4: 253\n"
            + "Row #5: 823\n");

        props = new RolapConnectionPropsR(List.of("Role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        connection = foodMartContext.getConnection(props);
        TestUtil.assertQueryReturns(
    		connection,
            mdx2,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Education Level].[Education Level].[All Education Levels]}\n"
            + "{[Education Level].[Education Level].[Bachelors Degree]}\n"
            + "{[Education Level].[Education Level].[Graduate Degree]}\n"
            + "{[Education Level].[Education Level].[High School Degree]}\n"
            + "{[Education Level].[Education Level].[Partial College]}\n"
            + "{[Education Level].[Education Level].[Partial High School]}\n"
            + "Row #0: 3,086\n"
            + "Row #1: 914\n"
            + "Row #2: 126\n"
            + "Row #3: 1,029\n"
            + "Row #4: 286\n"
            + "Row #5: 731\n");
        props = new RolapConnectionPropsR(List.of("Role1", "Role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        connection = foodMartContext.getConnection(props);
        TestUtil.assertQueryReturns(
    		connection,
            mdx2,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Education Level].[Education Level].[All Education Levels]}\n"
            + "{[Education Level].[Education Level].[Bachelors Degree]}\n"
            + "{[Education Level].[Education Level].[Graduate Degree]}\n"
            + "{[Education Level].[Education Level].[High School Degree]}\n"
            + "{[Education Level].[Education Level].[Partial College]}\n"
            + "{[Education Level].[Education Level].[Partial High School]}\n"
            + "Row #0: 5,477\n"
            + "Row #1: 1,473\n"
            + "Row #2: 331\n"
            + "Row #3: 1,580\n"
            + "Row #4: 539\n"
            + "Row #5: 1,554\n");
    }

    /**
     * This is a test for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1030">MONDRIAN-1030</a>
     * When a query is based on a level higher than one in the same hierarchy
     * which has access controls, it would only constrain at the current level
     * if the rollup policy of partial is used.
     *
     * <p>Example. A query on USA where only Los-Angeles is accessible would
     * return the values for California instead of only LA.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testBugMondrian1030_2(Context foodMartContext) {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier21::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Bacon"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = foodMartContext.getConnection(props);
    	TestUtil.assertQueryReturns(
    			connection,
                "select {[Measures].[Unit Sales]} on 0,\n"
                + "   {[Customers].[USA]} on 1\n"
                + "from [Sales]",
                "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "Axis #2:\n"
                + "{[Customers].[Customers].[USA]}\n"
                + "Row #0: 2,009\n");
    }

    /**
     * Test for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1091">MONDRIAN-1091</a>
     * The RoleImpl would try to search for member grants by object identity
     * rather than unique name. When using the partial rollup policy, the
     * members are wrapped, so identity checks would fail.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testMondrian1091(Context foodMartContext) throws Exception {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier22::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
    	Connection connection = foodMartContext.getConnection(props);
    	TestUtil.assertQueryReturns(
			connection,
            "select {[Store].Members} on columns from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[Store].[All Stores]}\n"
            + "{[Store].[Store].[USA]}\n"
            + "{[Store].[Store].[USA].[CA]}\n"
            + "{[Store].[Store].[USA].[CA].[Alameda]}\n"
            + "{[Store].[Store].[USA].[CA].[Alameda].[HQ]}\n"
            + "{[Store].[Store].[USA].[CA].[Beverly Hills]}\n"
            + "{[Store].[Store].[USA].[CA].[Beverly Hills].[Store 6]}\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles].[Store 7]}\n"
            + "{[Store].[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[Store].[USA].[CA].[San Diego].[Store 24]}\n"
            + "{[Store].[Store].[USA].[CA].[San Francisco]}\n"
            + "{[Store].[Store].[USA].[CA].[San Francisco].[Store 14]}\n"
            + "Row #0: 74,748\n"
            + "Row #0: 74,748\n"
            + "Row #0: 74,748\n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: 21,333\n"
            + "Row #0: 21,333\n"
            + "Row #0: 25,663\n"
            + "Row #0: 25,663\n"
            + "Row #0: 25,635\n"
            + "Row #0: 25,635\n"
            + "Row #0: 2,117\n"
            + "Row #0: 2,117\n");
        Cube cube =
        		TestUtil.getCubeByNameFromArray(connection
                .getCatalog().getCubes(), "Sales").orElseThrow(() -> new RuntimeException("Cube with name \"Sales\" is absent"));

        Member allMember =
            cube.getCatalogReader(connection.getRole()).withLocus().getMemberByUniqueName(
                Util.parseIdentifier("[Store].[All Stores]"), false);
        //org.olap4j.metadata.Member allMember =
        //    cube.lookupMember(
        //        IdentifierNode.parseIdentifier("[Store].[All Stores]")
        ///            .getSegmentList());

        assertNotNull(allMember);
        assertNotNull(allMember.getHierarchy().getAllMember());
        assertEquals(
            "[Store].[Store].[All Stores]",
            allMember.getHierarchy().getAllMember().getUniqueName());
    }

    /**
     * Unit test for
     * <a href="http://jira.pentaho.com/browse/mondrian-1259">MONDRIAN-1259,
     * "Mondrian security: access leaks from one user to another"</a>.
     *
     * <p>Enhancements made to the SmartRestrictedMemberReader were causing
     * security leaks between roles and potential class cast exceptions.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testMondrian1259(Context foodMartContext) throws Exception {
        final String mdx =
            "select non empty {[Store].Members} on columns from [Sales]";
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier23::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Role1"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        Connection connection = foodMartContext.getConnection(props);
        TestUtil.assertQueryReturns(
    		connection,
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[Store].[All Stores]}\n"
            + "{[Store].[Store].[USA]}\n"
            + "{[Store].[Store].[USA].[CA]}\n"
            + "{[Store].[Store].[USA].[CA].[Beverly Hills]}\n"
            + "{[Store].[Store].[USA].[CA].[Beverly Hills].[Store 6]}\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles].[Store 7]}\n"
            + "{[Store].[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[Store].[USA].[CA].[San Diego].[Store 24]}\n"
            + "{[Store].[Store].[USA].[CA].[San Francisco]}\n"
            + "{[Store].[Store].[USA].[CA].[San Francisco].[Store 14]}\n"
            + "Row #0: 74,748\n"
            + "Row #0: 74,748\n"
            + "Row #0: 74,748\n"
            + "Row #0: 21,333\n"
            + "Row #0: 21,333\n"
            + "Row #0: 25,663\n"
            + "Row #0: 25,663\n"
            + "Row #0: 25,635\n"
            + "Row #0: 25,635\n"
            + "Row #0: 2,117\n"
            + "Row #0: 2,117\n");
        props = new RolapConnectionPropsR(List.of("Role2"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        connection = foodMartContext.getConnection(props);
        TestUtil.assertQueryReturns(
    		connection,
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[Store].[All Stores]}\n"
            + "{[Store].[Store].[USA]}\n"
            + "{[Store].[Store].[USA].[OR]}\n"
            + "{[Store].[Store].[USA].[OR].[Portland]}\n"
            + "{[Store].[Store].[USA].[OR].[Portland].[Store 11]}\n"
            + "{[Store].[Store].[USA].[OR].[Salem]}\n"
            + "{[Store].[Store].[USA].[OR].[Salem].[Store 13]}\n"
            + "Row #0: 67,659\n"
            + "Row #0: 67,659\n"
            + "Row #0: 67,659\n"
            + "Row #0: 26,079\n"
            + "Row #0: 26,079\n"
            + "Row #0: 41,580\n"
            + "Row #0: 41,580\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testMondrian1295(Context foodMartContext) throws Exception {
        final String mdx =
            "With\n"
            + "Set [*NATIVE_CJ_SET] as 'NonEmptyCrossJoin([*BASE_MEMBERS_Time],[*BASE_MEMBERS_Product])'\n"
            + "Set [*SORTED_ROW_AXIS] as 'Order([*CJ_ROW_AXIS],Ancestor([Time].[Time].CurrentMember, [Time].[Time].[Year]).OrderKey,BASC,Ancestor([Time].[Time].CurrentMember, [Time].[Time].[Quarter]).OrderKey,BASC,[Time].[Time].CurrentMember.OrderKey,BASC,[Product].[Product].CurrentMember.OrderKey,BASC)'\n"
            + "Set [*BASE_MEMBERS_Product] as '{[Product].[Product].[All Products]}'\n"
            + "Set [*BASE_MEMBERS_Measures] as '{[Measures].[*FORMATTED_MEASURE_0]}'\n"
            + "Set [*CJ_ROW_AXIS] as 'Generate([*NATIVE_CJ_SET], {([Time].[Time].currentMember,[Product].[Product].currentMember)})'\n"
            + "Set [*BASE_MEMBERS_Time] as '[Time].[Time].[Year].Members'\n"
            + "Set [*CJ_COL_AXIS] as '[*NATIVE_CJ_SET]'\n"
            + "Member [Measures].[*FORMATTED_MEASURE_0] as '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=400\n"
            + "Select\n"
            + "[*BASE_MEMBERS_Measures] on columns,\n"
            + "Non Empty [*SORTED_ROW_AXIS] on rows\n"
            + "From [Sales]\n";

        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier24::new);
        Connection connection = foodMartContext.getConnectionWithDefaultRole();

        // Control
        TestUtil
            .assertQueryReturns(
        		connection,
                "select {[Measures].[Unit Sales]} on columns from [Sales]",
                "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "Row #0: 266,773\n");
        ConnectionProps props = new RolapConnectionPropsR(List.of("Admin"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        connection = foodMartContext.getConnection(props);
        TestUtil
            .assertQueryReturns(
        		connection,
                "select {[Measures].[Unit Sales]} on columns from [Sales]",
                "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "Row #0: 74,748\n");

        // Test
        TestUtil
            .assertQueryReturns(
        		connection,
                mdx,
                "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
                + "Axis #2:\n"
                + "{[Time].[Time].[1997], [Product].[Product].[All Products]}\n"
                + "Row #0: 74,748\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testMondrian936(Context foodMartContext) throws Exception {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier25::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("test"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        Connection connection = foodMartContext.getConnection(props);
        TestUtil.assertQueryReturns(
			connection,
            "select {[Measures].[Unit Sales]} on columns, "
            + "                 {[Product].[Food].[Baked Goods].[Bread]} on rows "
            + "                 from [Sales] "
            + " where { [Store].[USA].[OR], [Store].[USA].[CA]} ", "Axis #0:\n"
            + "{[Store].[Store].[USA].[OR]}\n"
            + "{[Store].[Store].[USA].[CA]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Product].[Product].[Food].[Baked Goods].[Bread]}\n"
            + "Row #0: 4,163\n");

        // changing ordering of members in the slicer should not change
        // result
    	TestUtil.assertQueryReturns(
			connection,
            "select {[Measures].[Unit Sales]} on columns, "
            + "                 {[Product].[Food].[Baked Goods].[Bread]} on rows "
            + "                 from [Sales] "
            + " where { [Store].[USA].[CA], [Store].[USA].[OR]} ", "Axis #0:\n"
            + "{[Store].[Store].[USA].[CA]}\n"
            + "{[Store].[Store].[USA].[OR]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Product].[Product].[Food].[Baked Goods].[Bread]}\n"
            + "Row #0: 4,163\n");


        Result result = TestUtil.executeQuery(
    		connection,
            "with member store.aggCaliforniaOregon as "
            + "'aggregate({ [Store].[USA].[CA], [Store].[USA].[OR]})'"
            + " select store.aggCaliforniaOregon on 0 from sales");

        String valueAggMember = result
            .getCell(new int[] {0}).getFormattedValue();

        result = TestUtil.executeQuery(
    		connection,
            " select from sales where "
            + "{ [Store].[USA].[CA], [Store].[USA].[OR]}");

        String valueSlicerAgg = result
            .getCell(new int[] {}).getFormattedValue();

        // aggregating CA & OR in a calc member should produce same result
        // as aggregating in the slicer.
        assertTrue(valueAggMember.equals(valueSlicerAgg));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testMondrian1434(Context foodMartContext) {
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier26::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("dev"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        Connection connection = foodMartContext.getConnection(props);
        TestUtil.executeQuery(
    		connection,
            " select from [Sales] where {[Measures].[Unit Sales]}");

        ((TestContext)foodMartContext).setCatalogMappingSupplier(new FoodmartMappingSupplier());
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier27::new);
        props = new RolapConnectionPropsR(List.of("dev"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        connection = foodMartContext.getConnection(props);
        TestUtil.executeQuery(
    		connection,
            " select from [Warehouse and Sales] where {[Measures].[Store Sales]}");
        // test is that there is no exception
    }

    /**
     * Fix for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1486">MONDRIAN-1486</a>
     *
     * When NECJ was used, a call to RolapNativeCrossJoin.createEvaluator
     * would swap the {@link LimitedRollupMember} for the regular all member
     * of the hierarchy, effectively removing security constraints.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testMondrian1486(Context foodMartContext) throws Exception {
        final String mdx =
            "With\n"
            + "Set [*NATIVE_CJ_SET] as 'NonEmptyCrossJoin([*BASE_MEMBERS_Gender],[*BASE_MEMBERS_Marital Status])'\n"
            + "Set [*SORTED_ROW_AXIS] as 'Order([*CJ_ROW_AXIS],[Gender].CurrentMember.OrderKey,BASC,[Marital Status].[Marital Status].CurrentMember.OrderKey,BASC)'\n"
            + "Set [*BASE_MEMBERS_Gender] as '[Gender].[Gender].[Gender].Members'\n"
            + "Set [*BASE_MEMBERS_Measures] as '{[Measures].[*FORMATTED_MEASURE_0]}'\n"
            + "Set [*CJ_ROW_AXIS] as 'Generate([*NATIVE_CJ_SET], {([Gender].[Gender].currentMember,[Marital Status].[Marital Status].currentMember)})'\n"
            + "Set [*BASE_MEMBERS_Marital Status] as '[Marital Status].[Marital Status].[Marital Status].Members'\n"
            + "Set [*CJ_COL_AXIS] as '[*NATIVE_CJ_SET]'\n"
            + "Member [Measures].[*FORMATTED_MEASURE_0] as '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=400\n"
            + "Select\n"
            + "[*BASE_MEMBERS_Measures] on columns,\n"
            + "Non Empty [*SORTED_ROW_AXIS] on rows\n"
            + "From [Sales]\n";
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier28::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("Admin"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        Connection connection = foodMartContext.getConnection(props);
        TestUtil.assertQueryReturns(
    		connection,
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
            + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[S]}\n"
            + "Row #0: 65,336\n"
            + "Row #1: 66,222\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testRollupPolicyWithNative(Context context) {
        // Verifies limited role-restricted results using
        // all variations of rollup policy
        // Also verifies consistent results with a non-all default member.
        // connected with MONDRIAN-1568
        ((TestConfig)context.getConfig()).setEnableNativeCrossJoin(true);
        ((TestConfig)context.getConfig()).setEnableNativeFilter(true);
        SystemWideProperties.instance().EnableNativeNonEmpty = true;
        ((TestConfig)context.getConfig()).setEnableNativeTopCount(true);
        ((TestConfig)context.getConfig()).setExpandNonNative(true);

        String dimension =
            "<Dimension name=\"Store2\">\n"
            + "  <Hierarchy hasAll=\"%s\" primaryKey=\"store_id\" %s >\n"
            + "    <Table name=\"store\"/>\n"
            + "    <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"/>\n"
            + "    <Level name=\"Store State\" column=\"store_state\" uniqueMembers=\"true\"/>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>\n";

        String cube =
            "<Cube name=\"TinySales\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <DimensionUsage name=\"Product\" source=\"Product\" foreignKey=\"product_id\"/>\n"
            + "  <DimensionUsage name=\"Store2\" source=\"Store2\" foreignKey=\"store_id\"/>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"/>\n"
            + "</Cube>";


        final String roleDefs =
            "<Role name=\"test\">\n"
            + "        <SchemaGrant access=\"none\">\n"
            + "            <CubeGrant cube=\"TinySales\" access=\"all\">\n"
            + "                <HierarchyGrant hierarchy=\"[Store2]\" access=\"custom\"\n"
            + "                                 rollupPolicy=\"%s\">\n"
            + "                    <MemberGrant member=\"[Store2].[USA].[CA]\" access=\"all\"/>\n"
            + "                    <MemberGrant member=\"[Store2].[USA].[OR]\" access=\"all\"/>\n"
            + "                    <MemberGrant member=\"[Store2].[Canada]\" access=\"all\"/>\n"
            + "                </HierarchyGrant>\n"
            + "            </CubeGrant>\n"
            + "        </SchemaGrant>\n"
            + "    </Role> ";

        String nonAllDefaultMem = "[Store2].[USA].[CA]";

        for (RollupPolicyType policy : RollupPolicyType.values()) {
            for (String defaultMember : new String[]{nonAllDefaultMem, null }) {
                for (boolean hasAll : new Boolean[]{true, false}) {
                    // Results in this test should be the same regardless
                    // of rollupPolicy, default member, and whether there
                    // is an all member, since the rollup is not included
                    // in the test queries and context is explicitly set
                    // for [Store2].
                    // MONDRIAN-1568 showed different results with different
                    // rollup policies and different default members
                    // RolapNativeCrossjoin
                	((TestContext)context).setCatalogMappingSupplier(new FoodmartMappingSupplier());
                    context.getCatalogCache().clear();
                    CatalogMapping catalogMapping = ((RolapContext) context).getCatalogMapping();
                    ((TestContext)context).setCatalogMappingSupplier(new SchemaModifiers.AccessControlTestModifier29(catalogMapping, hasAll, defaultMember, policy));

                    ConnectionProps props = new RolapConnectionPropsR(List.of("test"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
                    Connection connection = context.getConnection(props);
                	TestUtil.assertQueryReturns(
            			connection,
                        String.format(
                            "Failure testing RolapNativeCrossJoin with "
                            + " rollupPolicy=%s, "
                            +   "defaultMember=%s, hasAll=%s",
                            policy, defaultMember, hasAll),
                        "select NonEmptyCrossJoin([Store2].[Store State].MEMBERS,"
                        + "[Product].[Product Family].MEMBERS) on 0 from tinysales",
                        "Axis #0:\n"
                        + "{}\n"
                        + "Axis #1:\n"
                        + "{[Store2].[Store2].[USA].[CA], [Product].[Product].[Drink]}\n"
                        + "{[Store2].[Store2].[USA].[CA], [Product].[Product].[Food]}\n"
                        + "{[Store2].[Store2].[USA].[CA], [Product].[Product].[Non-Consumable]}\n"
                        + "{[Store2].[Store2].[USA].[OR], [Product].[Product].[Drink]}\n"
                        + "{[Store2].[Store2].[USA].[OR], [Product].[Product].[Food]}\n"
                        + "{[Store2].[Store2].[USA].[OR], [Product].[Product].[Non-Consumable]}\n"
                        + "Row #0: 7,102\n"
                        + "Row #0: 53,656\n"
                        + "Row #0: 13,990\n"
                        + "Row #0: 6,106\n"
                        + "Row #0: 48,537\n"
                        + "Row #0: 13,016\n");
                    // RolapNativeFilter
                	TestUtil.assertQueryReturns(
            			connection,
                        String.format(
                            "Failure testing RolapNativeFilter with "
                            + "rollupPolicy=%s, "
                            +   "defaultMember=%s, hasAll=%s",
                            policy, defaultMember, hasAll),
                        "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
                        + "  Filter( [Store2].[USA].children,"
                        + "          [Measures].[Unit Sales]>0) ON ROWS \n"
                        + "from [TinySales] \n",
                        "Axis #0:\n"
                        + "{}\n"
                        + "Axis #1:\n"
                        + "{[Measures].[Unit Sales]}\n"
                        + "Axis #2:\n"
                        + "{[Store2].[Store2].[USA].[CA]}\n"
                        + "{[Store2].[Store2].[USA].[OR]}\n"
                        + "Row #0: 74,748\n"
                        + "Row #1: 67,659\n");
                    // RolapNativeTopCount
                	TestUtil.assertQueryReturns(
            			connection,
                        String.format(
                            "Failure testing RolapNativeTopCount with "
                            + " rollupPolicy=%s, "
                            +   "defaultMember=%s, hasAll=%s",
                            policy, defaultMember, hasAll),
                        "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
                        + "  TopCount( [Store2].[USA].children,"
                        + "          2) ON ROWS \n"
                        + "from [TinySales] \n",
                        "Axis #0:\n"
                        + "{}\n"
                        + "Axis #1:\n"
                        + "{[Measures].[Unit Sales]}\n"
                        + "Axis #2:\n"
                        + "{[Store2].[Store2].[USA].[CA]}\n"
                        + "{[Store2].[Store2].[USA].[OR]}\n"
                        + "Row #0: 74,748\n"
                        + "Row #1: 67,659\n");
                }
            }
        }

        SystemWideProperties.instance().populateInitial();
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testValidMeasureWithRestrictedCubes(Context foodMartContext) {
        //http://jira.pentaho.com/browse/MONDRIAN-1616
        TestUtil.withSchema(foodMartContext, SchemaModifiers.AccessControlTestModifier30::new);
        ConnectionProps props = new RolapConnectionPropsR(List.of("noBaseCubes"), true, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty());
        Connection connection = foodMartContext.getConnection(props);

        TestUtil.assertQueryReturns(
    		connection,
            "with member measures.vm as 'validmeasure(measures.[unit sales])' "
            + "select measures.vm on 0 from [warehouse and sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[vm]}\n"
            + "Row #0: 266,773\n");

        TestUtil.assertQueryReturns(
    		connection,
            "with member measures.vm as 'validmeasure(measures.[warehouse cost])' "
            + "select measures.vm * {gender.f} on 0 from [warehouse and sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[vm], [Gender].[Gender].[F]}\n"
            + "Row #0: 89,043.253\n");
    }

}
