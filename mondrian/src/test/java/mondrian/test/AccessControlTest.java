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

import mondrian.olap.*;
import mondrian.olap.Role.HierarchyAccess;
import mondrian.rolap.RolapHierarchy.LimitedRollupMember;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.olap4j.mdx.IdentifierNode;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.Context;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.SchemaUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalogAsFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

/**
 * <code>AccessControlTest</code> is a set of unit-tests for access-control.
 * For these tests, all of the roles are of type RoleImpl.
 *
 * @see Role
 *
 * @author jhyde
 * @since Feb 21, 2003
 */
public class AccessControlTest {

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

    private PropertySaver5 propSaver;

	@BeforeEach
	public void beforeEach() {
		propSaver = new PropertySaver5();
	}

	@AfterEach
	public void afterEach() {
		propSaver.reset();
	}
    
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testSchemaReader(Context foodMartContext) {
        final Connection connection = foodMartContext.createConnection(); 
        Schema schema = connection.getSchema();
        final boolean fail = true;
        Cube cube = schema.lookupCube("Sales", fail);
        final SchemaReader schemaReader =
            cube.getSchemaReader(connection.getRole());
        final SchemaReader schemaReader1 = schemaReader.withoutAccessControl();
        assertNotNull(schemaReader1);
        final SchemaReader schemaReader2 = schemaReader1.withoutAccessControl();
        assertNotNull(schemaReader2);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testGrantDimensionNone(Context foodMartContext) {
        final Connection connection = foodMartContext.createConnection();
        RoleImpl role = ((RoleImpl) connection.getRole()).makeMutableClone();
        Schema schema = connection.getSchema();
        Cube salesCube = schema.lookupCube("Sales", true);
        // todo: add Schema.lookupDimension
        final SchemaReader schemaReader = salesCube.getSchemaReader(role);
        Dimension genderDimension =
            (Dimension) schemaReader.lookupCompound(
                salesCube, Id.Segment.toList("Gender"), true,
                Category.Dimension);
        role.grant(genderDimension, Access.NONE);
        role.makeImmutable();
        connection.setRole(role);
        TestUtil.assertAxisThrows(
    		connection,
            "[Gender].children",
            "MDX object '[Gender]' not found in cube 'Sales'");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testRestrictMeasures(Context foodMartContext) {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema( 
			baseSchema, null, null, null, null, null,
            "<Role name=\"Role1\">\n"
            + "  <SchemaGrant access=\"all\">\n"
            + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
            + "      <HierarchyGrant hierarchy=\"[Measures]\" access=\"all\">\n"
            + "      </HierarchyGrant>\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>"
            + "<Role name=\"Role2\">\n"
            + "  <SchemaGrant access=\"all\">\n"
            + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
            + "      <HierarchyGrant hierarchy=\"[Measures]\" access=\"custom\">\n"
            + "        <MemberGrant member=\"[Measures].[Unit Sales]\" access=\"all\"/>\n"
            + "      </HierarchyGrant>\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>");

    	TestUtil.withSchema(foodMartContext, schema);
    	
    	TestUtil.withRole(foodMartContext, "Role1");
    	Connection connection = foodMartContext.createConnection();
    	
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
        
    	TestUtil.withRole(foodMartContext, "Role2");
    	connection = foodMartContext.createConnection();
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testRestrictMeasuresHierarchy_InTwoRoles(Context foodMartContext) {
      String schema =
          "<Schema name=\"FoodMart.DimAndMeasure.Role\">\n"
          + " <Dimension name=\"WarehouseShared\">\n"
          + "   <Hierarchy hasAll=\"true\" primaryKey=\"warehouse_id\">\n"
          + "     <Table name=\"warehouse\"/>\n"
          + "     <Level name=\"Country\" column=\"warehouse_country\" uniqueMembers=\"true\"/>\n"
          + "     <Level name=\"State Province\" column=\"warehouse_state_province\"\n"
          + "          uniqueMembers=\"true\"/>\n"
          + "     <Level name=\"City\" column=\"warehouse_city\" uniqueMembers=\"false\"/>\n"
          + "     <Level name=\"Warehouse Name\" column=\"warehouse_name\" uniqueMembers=\"true\"/>\n"
          + "   </Hierarchy>\n"
          + " </Dimension>\n"
          + " <Cube name=\"Warehouse1\">\n"
          + "   <Table name=\"inventory_fact_1997\"/>\n"
          + "   <DimensionUsage name=\"WarehouseShared\" source=\"WarehouseShared\" foreignKey=\"warehouse_id\"/>\n"
          + "   <Measure name=\"Measure1_0\" column=\"warehouse_cost\" aggregator=\"sum\"/>\n"
          + "   <Measure name=\"Measure1_1\" column=\"warehouse_sales\" aggregator=\"sum\"/>\n"
          + "   <CalculatedMember name=\"Calculated Measure1\" dimension=\"Measures\">\n"
          + "     <Formula>[Measures].[Measure1_1] / [Measures].[Measure1_0]</Formula>\n"
          + "     <CalculatedMemberProperty name=\"FORMAT_STRING\" value=\"$#,##0.00\"/>\n"
          + "   </CalculatedMember>\n"
          + " </Cube>\n"
          + " <Cube name=\"Warehouse2\">\n"
          + "   <Table name=\"inventory_fact_1997\"/>\n"
          + "   <DimensionUsage name=\"WarehouseShared\" source=\"WarehouseShared\" foreignKey=\"warehouse_id\"/>\n"
          + "   <Measure name=\"Measure2_0\" column=\"warehouse_cost\" aggregator=\"sum\"/>\n"
          + "   <Measure name=\"Measure2_1\" column=\"warehouse_sales\" aggregator=\"sum\"/>\n"
          + "   <CalculatedMember name=\"Calculated Measure2\" dimension=\"Measures\">\n"
          + "     <Formula>[Measures].[Measure2_1] / [Measures].[Measure2_0]</Formula>\n"
          + "     <CalculatedMemberProperty name=\"FORMAT_STRING\" value=\"$#,##0.00\"/>\n"
          + "   </CalculatedMember>\n"
          + " </Cube>\n"
          + " <Role name=\"Administrator\">\n"
          + "   <SchemaGrant access=\"none\">\n"
          + "     <CubeGrant cube=\"Warehouse1\" access=\"custom\">\n"
          + "       <HierarchyGrant hierarchy=\"[WarehouseShared]\" access=\"all\">\n"
          + "       </HierarchyGrant>\n"
          + "       <HierarchyGrant hierarchy=\"[Measures]\" access=\"all\">\n"
          + "       </HierarchyGrant>\n"
          + "     </CubeGrant>\n"
          + "     <CubeGrant cube=\"Warehouse2\" access=\"custom\">\n"
          + "       <HierarchyGrant hierarchy=\"[WarehouseShared]\" access=\"all\">\n"
          + "       </HierarchyGrant>\n"
          + "       <HierarchyGrant hierarchy=\"[Measures]\" access=\"all\">\n"
          + "       </HierarchyGrant>\n"
          + "     </CubeGrant>\n"
          + "   </SchemaGrant>\n"
          + " </Role>\n"
          + "</Schema>";
      
      TestUtil.withRole(foodMartContext, "Administrator"); 
      TestUtil.withSchema(foodMartContext, schema); 

      Connection connection = foodMartContext.createConnection();

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
      } catch (MondrianException e) {
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
      } catch (MondrianException e) {
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

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testRestrictLevelsAnalyzer3283(Context foodMartContext) {
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
        TestUtil.withSchema(foodMartContext, schema);
        TestUtil.withRole(foodMartContext, "MR,DBPentUsers"); 
        Connection connection = foodMartContext.createConnection();

        final Role.HierarchyAccess hierarchyAccess =
          getHierarchyAccess(connection, "Sales1", "[Customers]");

        assertEquals(2, hierarchyAccess.getTopLevelDepth());
        assertEquals(3, hierarchyAccess.getBottomLevelDepth());
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testRoleMemberAccessNonExistentMemberFails(Context foodMartContext) {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
            null, null, null, null, null,
            "<Role name=\"Role1\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
            + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"custom\" rollupPolicy=\"partial\">\n"
            + "        <MemberGrant member=\"[Store].[USA].[Non Existent]\" access=\"all\"/>\n"
            + "      </HierarchyGrant>\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>");
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "Role1");    	
        TestUtil.assertQueryThrows(
        	foodMartContext,
            "select {[Store].Children} on 0 from [Sales]",
            "Member '[Store].[USA].[Non Existent]' not found");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testRoleMemberAccess(Context foodMartContext) {
        final Connection connection = getRestrictedConnection(foodMartContext);
        // because CA has access
        assertMemberAccess(connection, Access.CUSTOM, "[Store].[USA]");
        assertMemberAccess(connection, Access.CUSTOM, "[Store].[Mexico]");
        assertMemberAccess(connection, Access.NONE, "[Store].[Mexico].[DF]");
        assertMemberAccess(
            connection, Access.NONE, "[Store].[Mexico].[DF].[Mexico City]");
        assertMemberAccess(connection, Access.NONE, "[Store].[Canada]");
        assertMemberAccess(
            connection, Access.NONE, "[Store].[Canada].[BC].[Vancouver]");
        assertMemberAccess(
            connection, Access.ALL, "[Store].[USA].[CA].[Los Angeles]");
        assertMemberAccess(
            connection, Access.NONE, "[Store].[USA].[CA].[San Diego]");
        // USA deny supercedes OR grant
        assertMemberAccess(
            connection, Access.NONE, "[Store].[USA].[OR].[Portland]");
        assertMemberAccess(
            connection, Access.NONE, "[Store].[USA].[WA].[Seattle]");
        assertMemberAccess(connection, Access.NONE, "[Store].[USA].[WA]");
        // above top level
        assertMemberAccess(connection, Access.NONE, "[Store].[All Stores]");
    }

    private void assertMemberAccess(
        final Connection connection,
        Access expectedAccess,
        String memberName)
    {
        final Role role = connection.getRole(); // restricted
        Schema schema = connection.getSchema();
        final boolean fail = true;
        Cube salesCube = schema.lookupCube("Sales", fail);
        final SchemaReader schemaReader =
            salesCube.getSchemaReader(null).withLocus();
        final Member member =
            schemaReader.getMemberByUniqueName(
                Util.parseIdentifier(memberName), true);
        final Access actualAccess = role.getAccess(member);
        assertEquals(expectedAccess, actualAccess, memberName);
    }

    private void assertCubeAccess(
        final Connection connection,
        Access expectedAccess,
        String cubeName)
    {
        final Role role = connection.getRole();
        Schema schema = connection.getSchema();
        final boolean fail = true;
        Cube cube = schema.lookupCube(cubeName, fail);
        final Access actualAccess = role.getAccess(cube);
        assertEquals(expectedAccess, actualAccess, cubeName);
    }

    private void assertHierarchyAccess(
        final Connection connection,
        Access expectedAccess,
        String cubeName,
        String hierarchyName)
    {
        final Role role = connection.getRole();
        Schema schema = connection.getSchema();
        final boolean fail = true;
        Cube cube = schema.lookupCube(cubeName, fail);
        final SchemaReader schemaReader =
            cube.getSchemaReader(null); // unrestricted
        final Hierarchy hierarchy =
            (Hierarchy) schemaReader.lookupCompound(
                cube, Util.parseIdentifier(hierarchyName), fail,
                Category.Hierarchy);

        final Access actualAccess = role.getAccess(hierarchy);
        assertEquals(expectedAccess, actualAccess, cubeName);
    }

    private Role.HierarchyAccess getHierarchyAccess(
        final Connection connection,
        String cubeName,
        String hierarchyName)
    {
        final Role role = connection.getRole();
        Schema schema = connection.getSchema();
        final boolean fail = true;
        Cube cube = schema.lookupCube(cubeName, fail);
        final SchemaReader schemaReader =
            cube.getSchemaReader(null); // unrestricted
        final Hierarchy hierarchy =
            (Hierarchy) schemaReader.lookupCompound(
                cube, Util.parseIdentifier(hierarchyName), fail,
                Category.Hierarchy);

        return role.getAccessDetails(hierarchy);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testGrantHierarchy1a(Context foodMartContext) {
        // assert: can access Mexico (explicitly granted)
        // assert: can not access Canada (explicitly denied)
        // assert: can access USA (rule 3 - parent of allowed member San
        // Francisco)
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisReturns(
    		connection,
            "[Store].level.members",
            "[Store].[Mexico]\n" + "[Store].[USA]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testGrantHierarchy1aAllMembers(Context foodMartContext) {
        // assert: can access Mexico (explicitly granted)
        // assert: can not access Canada (explicitly denied)
        // assert: can access USA (rule 3 - parent of allowed member San
        // Francisco)
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisReturns(
    		connection,
            "[Store].level.allmembers",
            "[Store].[Mexico]\n" + "[Store].[USA]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testGrantHierarchy1b(Context foodMartContext) {
        // can access Mexico (explicitly granted) which is the first accessible
        // one
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisReturns(
    		connection,
            "[Store].defaultMember",
            "[Store].[Mexico]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testGrantHierarchy1c(Context foodMartContext) {
        // the root element is All Customers
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisReturns(
    		connection,
            "[Customers].defaultMember",
            "[Customers].[Canada].[BC]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testGrantHierarchy2(Context foodMartContext) {
        // assert: can access California (parent of allowed member)
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisReturns(
    		connection,
            "[Store].[USA].children",
            "[Store].[USA].[CA]");
        TestUtil.assertAxisReturns(
    		connection,
            "[Store].[USA].children",
            "[Store].[USA].[CA]");
        TestUtil.assertAxisReturns(
    		connection,
            "[Store].[USA].[CA].children",
            "[Store].[USA].[CA].[Los Angeles]\n"
            + "[Store].[USA].[CA].[San Francisco]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testGrantHierarchy3(Context foodMartContext) {
        // assert: can not access Washington (child of denied member)
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisThrows(connection, "[Store].[USA].[WA]", "not found");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testGrantHierarchy4(Context foodMartContext) {
        // assert: can not access Oregon (rule 1 - order matters)
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisThrows(connection, 
            "[Store].[USA].[OR].children", "not found");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testGrantHierarchy5(Context foodMartContext) {
        // assert: can not access All (above top level)
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisThrows(connection, "[Store].[All Stores]", "not found");
        TestUtil.assertAxisReturns(connection,
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
            "[Store].[Mexico]\n"
            + "[Store].[Mexico].[Guerrero]\n"
            + "[Store].[Mexico].[Guerrero].[Acapulco]\n"
            + "[Store].[Mexico].[Guerrero].[Acapulco].[Store 1]\n"
            + "[Store].[Mexico].[Jalisco]\n"
            + "[Store].[Mexico].[Jalisco].[Guadalajara]\n"
            + "[Store].[Mexico].[Jalisco].[Guadalajara].[Store 5]\n"
            + "[Store].[Mexico].[Veracruz]\n"
            + "[Store].[Mexico].[Veracruz].[Orizaba]\n"
            + "[Store].[Mexico].[Veracruz].[Orizaba].[Store 10]\n"
            + "[Store].[Mexico].[Yucatan]\n"
            + "[Store].[Mexico].[Yucatan].[Merida]\n"
            + "[Store].[Mexico].[Yucatan].[Merida].[Store 8]\n"
            + "[Store].[Mexico].[Zacatecas]\n"
            + "[Store].[Mexico].[Zacatecas].[Camacho]\n"
            + "[Store].[Mexico].[Zacatecas].[Camacho].[Store 4]\n"
            + "[Store].[Mexico].[Zacatecas].[Hidalgo]\n"
            + "[Store].[Mexico].[Zacatecas].[Hidalgo].[Store 12]\n"
            + "[Store].[Mexico].[Zacatecas].[Hidalgo].[Store 18]\n"
            + "[Store].[USA]\n"
            + "[Store].[USA].[CA]\n"
            + "[Store].[USA].[CA].[Los Angeles]\n"
            + "[Store].[USA].[CA].[Los Angeles].[Store 7]\n"
            + "[Store].[USA].[CA].[San Francisco]\n"
            + "[Store].[USA].[CA].[San Francisco].[Store 14]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testGrantHierarchy6(Context foodMartContext) {
        // assert: parent if at top level is null
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisReturns(
    		connection,
            "[Customers].[USA].[CA].parent",
            "");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testGrantHierarchy7(Context foodMartContext) {
        // assert: members above top level do not exist
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisThrows(
    		connection,
    		"[Customers].[Canada].children",
            "MDX object '[Customers].[Canada]' not found in cube 'Sales'");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testGrantHierarchy8(Context foodMartContext) {
        // assert: can not access Catherine Abel in San Francisco (below bottom
        // level)
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisThrows(
    		connection,
            "[Customers].[USA].[CA].[San Francisco].[Catherine Abel]",
            "not found");
        TestUtil.assertAxisReturns(
    		connection,
            "[Customers].[USA].[CA].[San Francisco].children",
            "");
        Axis axis = TestUtil.executeAxis(connection, "[Customers].members");
        // 13 states, 109 cities
        assertEquals(122, axis.getPositions().size());
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testGrantHierarchy8AllMembers(Context foodMartContext) {
        // assert: can not access Catherine Abel in San Francisco (below bottom
        // level)
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertAxisThrows(
    		connection,
            "[Customers].[USA].[CA].[San Francisco].[Catherine Abel]",
            "not found");
        TestUtil.assertAxisReturns(
    		connection,
            "[Customers].[USA].[CA].[San Francisco].children",
            "");
        Axis axis = TestUtil.executeAxis(connection, "[Customers].allmembers");
        // 13 states, 109 cities
        assertEquals(122, axis.getPositions().size());
    }

    /**
     * Tests for Mondrian BUG 1201 - Native Rollups did not handle
     * access-control with more than one member where granted access=all
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testBugMondrian_1201_MultipleMembersInRoleAccessControl(Context foodMartContext) {
        String test_1201_Roles =
            "<Role name=\"Role1\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
            + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"custom\" rollupPolicy=\"partial\">\n"
            + "        <MemberGrant member=\"[Store].[USA].[WA]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[USA].[OR]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[USA].[CA].[San Francisco]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[USA].[CA].[Los Angeles]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[Mexico]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[Mexico].[DF]\" access=\"none\"/>\n"
            + "        <MemberGrant member=\"[Store].[Canada]\" access=\"none\"/>\n"
            + "      </HierarchyGrant>\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>\n"
            + "<Role name=\"Role2\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
            + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"custom\" rollupPolicy=\"full\">\n"
            + "        <MemberGrant member=\"[Store].[USA].[WA]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[USA].[OR]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[USA].[CA].[San Francisco]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[USA].[CA].[Los Angeles]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[Mexico]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[Mexico].[DF]\" access=\"none\"/>\n"
            + "        <MemberGrant member=\"[Store].[Canada]\" access=\"none\"/>\n"
            + "      </HierarchyGrant>\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>";

        String baseSchema = TestUtil.getRawSchema(foodMartContext);
        String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null, test_1201_Roles);
        TestUtil.withSchema(foodMartContext, schema);
        TestUtil.withRole(foodMartContext, "Role1");
        
        Connection connection = foodMartContext.createConnection();

        // Must return only 2 [USA].[CA] stores
        TestUtil.assertQueryReturns(
    		connection,
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
            + "  Filter( [Store].[USA].[CA].children,"
            + "          [Measures].[Unit Sales]>0) ON ROWS \n"
            + "from [Sales] \n"
            + "where ([Time].[1997].[Q1].[2])",
            "Axis #0:\n"
            + "{[Time].[1997].[Q1].[2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[USA].[CA].[San Francisco]}\n"
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
            + "{[Time].[1997].[Q1].[2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[USA].[CA].[San Francisco]}\n"
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
            + "{[Time].[1997].[Q1].[2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[USA].[OR]}\n"
            + "{[Store].[USA].[WA]}\n"
            + "Row #0: 4,617\n"
            + "Row #1: 10,319\n");

        schema = SchemaUtil.getSchema(baseSchema, 
                    null, null, null, null, null, test_1201_Roles);
        TestUtil.withSchema(foodMartContext, schema);
        TestUtil.withRole(foodMartContext, "Role2");
        
        connection = foodMartContext.createConnection();

        // Full Rollup: [USA].[CA] rolls up to 6.021
        TestUtil.assertQueryReturns(
    		connection,
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
            + "  Filter( [Store].[Store State].Members,"
            + "          [Measures].[Unit Sales]>4000) ON ROWS \n"
            + "from [Sales] \n"
            + "where ([Time].[1997].[Q1].[2])",
            "Axis #0:\n"
            + "{[Time].[1997].[Q1].[2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[USA].[CA]}\n"
            + "{[Store].[USA].[OR]}\n"
            + "{[Store].[USA].[WA]}\n"
            + "Row #0: 6,021\n"
            + "Row #1: 4,617\n"
            + "Row #2: 10,319\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testBugMondrian_2586_RaggedDimMembersShouldBeVisible(Context foodMartContext) {
      String raggedUser =
          "<Role name=\"Sales Ragged\">\n"
          + "  <SchemaGrant access=\"none\">\n"
          + "    <CubeGrant cube=\"Sales Ragged\" access=\"all\" />\n"
          + "  </SchemaGrant>\n"
          + "</Role>";
      String baseSchema = TestUtil.getRawSchema(foodMartContext);
      String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null, raggedUser);
      TestUtil.withSchema(foodMartContext, schema);
      TestUtil.withRole(foodMartContext, "Sales Ragged"); 
    //[Geography].[Country]
      Connection connection = foodMartContext.createConnection();
      TestUtil.assertQueryReturns(
		  connection,
        "select {[Measures].[Unit Sales]} ON COLUMNS, {[Geography].[Country].MEMBERS} ON ROWS from [Sales Ragged]",
        "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Geography].[Canada]}\n"
        + "{[Geography].[Israel]}\n"
        + "{[Geography].[Mexico]}\n"
        + "{[Geography].[USA]}\n"
        + "{[Geography].[Vatican]}\n"
        + "Row #0: \n"
        + "Row #1: 13,694\n"
        + "Row #2: \n"
        + "Row #3: 217,822\n"
        + "Row #4: 35,257\n");
    }



    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testBugMondrian_1201_CacheAwareOfRoleAccessControl(Context foodMartContext) {
        String test_1201_Roles =
            "<Role name=\"Role1\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
            + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"custom\" rollupPolicy=\"partial\">\n"
            + "        <MemberGrant member=\"[Store].[USA].[WA]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[USA].[OR]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[USA].[CA].[San Francisco]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[USA].[CA].[Los Angeles]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[Mexico]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[Mexico].[DF]\" access=\"none\"/>\n"
            + "        <MemberGrant member=\"[Store].[Canada]\" access=\"none\"/>\n"
            + "      </HierarchyGrant>\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>\n"
            + "<Role name=\"Role2\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
            + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"custom\" rollupPolicy=\"partial\">\n"
            + "        <MemberGrant member=\"[Store].[USA].[WA]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[USA].[OR]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[USA].[CA].[San Francisco]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[Mexico]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[Mexico].[DF]\" access=\"none\"/>\n"
            + "        <MemberGrant member=\"[Store].[Canada]\" access=\"none\"/>\n"
            + "      </HierarchyGrant>\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>";

        String baseSchema = TestUtil.getRawSchema(foodMartContext);
        String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null, test_1201_Roles);
        TestUtil.withSchema(foodMartContext, schema);
        TestUtil.withRole(foodMartContext, "Role1"); 
        Connection connection = foodMartContext.createConnection();

        // Put query into cache
        TestUtil.assertQueryReturns(
    		connection,
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
            + "  Filter( [Store].[USA].[CA].children,"
            + "          [Measures].[Unit Sales]>0) ON ROWS \n"
            + "from [Sales] \n"
            + "where ([Time].[1997].[Q1].[2])",
            "Axis #0:\n"
            + "{[Time].[1997].[Q1].[2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[USA].[CA].[San Francisco]}\n"
            + "Row #0: 2,614\n"
            + "Row #1: 187\n");

        schema = SchemaUtil.getSchema(baseSchema, 
                    null, null, null, null, null, test_1201_Roles);
        TestUtil.withSchema(foodMartContext, schema);
        TestUtil.withRole(foodMartContext, "Role2"); 
        connection = foodMartContext.createConnection();
        
        // Run same query using another role with different access controls
        TestUtil.assertQueryReturns(
    		connection,
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
            + "  TopCount( [Store].[USA].[CA].children, 20,"
            + "            [Measures].[Unit Sales]) ON ROWS \n"
            + "from [Sales] \n"
            + "where ([Time].[1997].[Q1].[2])",
            "Axis #0:\n"
            + "{[Time].[1997].[Q1].[2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[USA].[CA].[San Francisco]}\n"
            + "Row #0: 187\n");
    }

    /**
     * Tests for Mondrian BUG 1127 - Native Top Count was not taking into
     * account user roles
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testBugMondrian1127OneSlicerOnly(Context foodMartContext) {
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertQueryReturns(
    		connection,
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
            + "  TopCount([Store].[USA].[CA].Children, 10,"
            + "           [Measures].[Unit Sales]) ON ROWS \n"
            + "from [Sales] \n"
            + "where ([Time].[1997].[Q1].[2])",
            "Axis #0:\n"
            + "{[Time].[1997].[Q1].[2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[USA].[CA].[San Francisco]}\n"
            + "Row #0: 2,614\n"
            + "Row #1: 187\n");

        TestUtil.assertQueryReturns(
        	foodMartContext.createConnection(),
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
            + "  NON EMPTY TopCount([Store].[USA].[CA].Children, 10, "
            + "           [Measures].[Unit Sales]) ON ROWS \n"
            + "from [Sales] \n"
            + "where ([Time].[1997].[Q1].[2])",
            "Axis #0:\n"
            + "{[Time].[1997].[Q1].[2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[USA].[CA].[Beverly Hills]}\n"
            + "{[Store].[USA].[CA].[San Francisco]}\n"
            + "Row #0: 2,614\n"
            + "Row #1: 1,879\n"
            + "Row #2: 1,341\n"
            + "Row #3: 187\n");
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testBugMondrian1127MultipleSlicers(Context foodMartContext) {
        Connection connection = getRestrictedConnection(foodMartContext);
        TestUtil.assertQueryReturns(
    		connection,
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
            + "  TopCount([Store].[USA].[CA].Children, 10,"
            + "           [Measures].[Unit Sales]) ON ROWS \n"
            + "from [Sales] \n"
            + "where ([Time].[1997].[Q1].[2] : [Time].[1997].[Q1].[3])",
            "Axis #0:\n"
            + "{[Time].[1997].[Q1].[2]}\n"
            + "{[Time].[1997].[Q1].[3]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[USA].[CA].[San Francisco]}\n"
            + "Row #0: 4,497\n"
            + "Row #1: 337\n");

        TestUtil.assertQueryReturns(
        	foodMartContext.createConnection(),
            "select NON EMPTY {[Measures].[Unit Sales]} ON COLUMNS, \n"
            + "  NON EMPTY TopCount([Store].[USA].[CA].Children, 10, "
            + "           [Measures].[Unit Sales]) ON ROWS \n"
            + "from [Sales] \n"
            + "where ([Time].[1997].[Q1].[2] : [Time].[1997].[Q1].[3])",
            "Axis #0:\n"
            + "{[Time].[1997].[Q1].[2]}\n"
            + "{[Time].[1997].[Q1].[3]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[USA].[CA].[Beverly Hills]}\n"
            + "{[Store].[USA].[CA].[San Francisco]}\n"
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testGrantHierarchy9(Context foodMartContext) {
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
    	//Connection connection = foodMartContext.createConnection();
    	TestUtil.assertQueryReturns(
			connection,
            "with member [Measures].[California Unit Sales] as "
            + " 'Aggregate({[Store].[USA].[CA].children}, [Measures].[Unit Sales])'\n"
            + "select {[Measures].[California Unit Sales]} on columns,\n"
            + " {[Gender].children} on rows\n"
            + "from Sales\n"
            + "where ([Marital Status].[S])",
            "Axis #0:\n"
            + "{[Marital Status].[S]}\n"
            + "Axis #1:\n"
            + "{[Measures].[California Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Gender].[F]}\n"
            + "{[Gender].[M]}\n"
            + "Row #0: 6,636\n"
            + "Row #1: 7,329\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testGrantHierarchyA(Context foodMartContext) {
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
            + "{[Store].[USA]}\n"
            + "{[Store].[USA].[CA]}\n"
            + "Row #0: 266,773\n"
            + "Row #1: 74,748\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void _testSharedObjectsInGrantMappingsBug(Context foodMartContext) {
        boolean mustGet = true;
        Connection connection = foodMartContext.createConnection();
        Schema schema = connection.getSchema();
        Cube salesCube = schema.lookupCube("Sales", mustGet);
        Cube warehouseCube = schema.lookupCube("Warehouse", mustGet);
        Hierarchy measuresInSales = salesCube.lookupHierarchy(
            new Id.NameSegment("Measures", Id.Quoting.UNQUOTED), false);
        Hierarchy storeInWarehouse = warehouseCube.lookupHierarchy(
            new Id.NameSegment("Store", Id.Quoting.UNQUOTED), false);

        RoleImpl role = new RoleImpl();
        role.grant(schema, Access.NONE);
        role.grant(salesCube, Access.NONE);
        // For using hierarchy Measures in #assertExprThrows
        Role.RollupPolicy rollupPolicy = Role.RollupPolicy.FULL;
        role.grant(
            measuresInSales, Access.ALL, null, null, rollupPolicy);
        role.grant(warehouseCube, Access.NONE);
        role.grant(storeInWarehouse.getDimension(), Access.ALL);

        role.makeImmutable();
        connection.setRole(role);

        TestUtil.assertExprThrows(
    		connection,
            "[Store].DefaultMember",
            "cube 'Sales' not found");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testNoAccessToCube(Context foodMartContext) {
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
        Connection connection = foodMartContext.createConnection();
        RoleImpl role = new RoleImpl();
        Schema schema = connection.getSchema();
        final boolean fail = true;
        Cube salesCube = schema.lookupCube("Sales", fail);
        final SchemaReader schemaReader =
            salesCube.getSchemaReader(null).withLocus();
        Hierarchy storeHierarchy = salesCube.lookupHierarchy(
            new Id.NameSegment("Store", Id.Quoting.UNQUOTED), false);
        role.grant(schema, Access.ALL_DIMENSIONS);
        role.grant(salesCube, Access.ALL);
        Level nationLevel =
            Util.lookupHierarchyLevel(storeHierarchy, "Store Country");
        Role.RollupPolicy rollupPolicy = Role.RollupPolicy.FULL;
        role.grant(
            storeHierarchy, Access.CUSTOM, nationLevel, null, rollupPolicy);
        role.grant(
            schemaReader.getMemberByUniqueName(
                Util.parseIdentifier("[Store].[All Stores].[USA].[OR]"), fail),
            Access.ALL);
        role.grant(
            schemaReader.getMemberByUniqueName(
                Util.parseIdentifier("[Store].[All Stores].[USA]"), fail),
            Access.CUSTOM);
        role.grant(
            schemaReader.getMemberByUniqueName(
                Util.parseIdentifier(
                    "[Store].[All Stores].[USA].[CA].[San Francisco]"), fail),
            Access.ALL);
        role.grant(
            schemaReader.getMemberByUniqueName(
                Util.parseIdentifier(
                    "[Store].[All Stores].[USA].[CA].[Los Angeles]"), fail),
            Access.ALL);
        role.grant(
            schemaReader.getMemberByUniqueName(
                Util.parseIdentifier(
                    "[Store].[All Stores].[Mexico]"), fail),
            Access.ALL);
        role.grant(
            schemaReader.getMemberByUniqueName(
                Util.parseIdentifier(
                    "[Store].[All Stores].[Mexico].[DF]"), fail),
            Access.NONE);
        role.grant(
            schemaReader.getMemberByUniqueName(
                Util.parseIdentifier(
                    "[Store].[All Stores].[Canada]"), fail),
            Access.NONE);
        if (restrictCustomers) {
            Hierarchy customersHierarchy =
                salesCube.lookupHierarchy(
                    new Id.NameSegment("Customers", Id.Quoting.UNQUOTED),
                    false);
            Level stateProvinceLevel =
                Util.lookupHierarchyLevel(customersHierarchy, "State Province");
            Level customersCityLevel =
                Util.lookupHierarchyLevel(customersHierarchy, "City");
            role.grant(
                customersHierarchy,
                Access.CUSTOM,
                stateProvinceLevel,
                customersCityLevel,
                rollupPolicy);
        }

        // No access to HR cube.
        Cube hrCube = schema.lookupCube("HR", fail);
        role.grant(hrCube, Access.NONE);

        role.makeImmutable();
        connection.setRole(role);
        return connection;
    }

    /**
     * Test context where the [Store] hierarchy has restricted access
     * and cell values are rolled up with 'partial' policy.
     */
    private void setRollupTestContext(Context foodMartContext) {
    	Connection connection = foodMartContext.createConnection();
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
            null, null, null, null, null,
            "<Role name=\"Role1\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"Sales\" access=\"custom\">\n"
            + "      <DimensionGrant dimension=\"[Measures]\" access=\"all\"/>\n"
            + "      <DimensionGrant dimension=\"[Gender]\" access=\"all\"/>\n"
            + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"custom\" rollupPolicy=\"partial\">\n"
            + "        <MemberGrant member=\"[Store].[USA]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[USA].[CA]\" access=\"none\"/>\n"
            + "      </HierarchyGrant>\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>");
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "Role1"); 
    }

    /**
     * Basic test of partial rollup policy. [USA] = [OR] + [WA], not
     * the usual [CA] + [OR] + [WA].
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testRollupPolicyBasic(Context foodMartContext) {
        setRollupTestContext(foodMartContext);
        Connection connection = foodMartContext.createConnection();
        TestUtil.assertQueryReturns(
    		connection,
            "select {[Store].[USA], [Store].[USA].Children} on 0\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[USA]}\n"
            + "{[Store].[USA].[OR]}\n"
            + "{[Store].[USA].[WA]}\n"
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testRollupPolicyAll(Context foodMartContext) {
        setRollupTestContext(foodMartContext);
        Connection connection = foodMartContext.createConnection();
        TestUtil.assertExprReturns(
    		connection,
            "([Store].[All Stores])",
            "192,025");
    }

    /**
     * Access [Store].[All Stores] implicitly as it is the default member
     * of the [Stores] hierarchy.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testRollupPolicyAllAsDefault(Context foodMartContext) {
        setRollupTestContext(foodMartContext);
        Connection connection = foodMartContext.createConnection();
        TestUtil.assertExprReturns(
    		connection,
            "([Store])",
            "192,025");
    }

    /**
     * Access [Store].[All Stores] via the Parent relationship (to check
     * that this doesn't circumvent access control).
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testRollupPolicyAllAsParent(Context foodMartContext) {
        setRollupTestContext(foodMartContext);
        Connection connection = foodMartContext.createConnection();
        TestUtil.assertExprReturns(
    		connection,
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testUnusedAccessControlledDimension(Context foodMartContext) {
        Connection connection = foodMartContext.createConnection();
        TestUtil.assertQueryReturns(
    		connection,
            "select [Gender].Children on 0 from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Gender].[F]}\n"
            + "{[Gender].[M]}\n"
            + "Row #0: 131,558\n"
            + "Row #0: 135,215\n");
        
        setRollupTestContext(foodMartContext);
        connection = foodMartContext.createConnection();
        TestUtil.assertQueryReturns(
    		connection,
            "select [Gender].Children on 0 from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Gender].[F]}\n"
            + "{[Gender].[M]}\n"
            + "Row #0: 94,799\n"
            + "Row #0: 97,226\n");
    }

    /**
     * Tests that members below bottom level are regarded as visible.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testRollupBottomLevel(Context foodMartContext) {
        rollupPolicyBottom(
    		foodMartContext, Role.RollupPolicy.FULL, "74,748", "36,759", "266,773");
        rollupPolicyBottom(
        		foodMartContext, Role.RollupPolicy.PARTIAL, "72,739", "35,775", "264,764");
        rollupPolicyBottom(foodMartContext, Role.RollupPolicy.HIDDEN, "", "", "");
    }

    private void rollupPolicyBottom(
		Context foodMartContext,
        Role.RollupPolicy rollupPolicy,
        String v1,
        String v2,
        String v3)
    {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null,
                "<Role name=\"Role1\">\n"
                + "  <SchemaGrant access=\"none\">\n"
                + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
                + "      <HierarchyGrant hierarchy=\"[Customers]\" access=\"custom\" rollupPolicy=\""
                    + rollupPolicy
                    + "\" bottomLevel=\"[Customers].[City]\">\n"
                    + "        <MemberGrant member=\"[Customers].[USA]\" access=\"all\"/>\n"
                    + "        <MemberGrant member=\"[Customers].[USA].[CA]\" access=\"all\"/>\n"
                    + "        <MemberGrant member=\"[Customers].[USA].[CA].[Los Angeles]\" access=\"none\"/>\n"
                    + "      </HierarchyGrant>\n"
                    + "    </CubeGrant>\n"
                    + "  </SchemaGrant>\n"
                    + "</Role>");
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "Role1"); 
    	Connection connection = foodMartContext.createConnection();
        // All of the children of [San Francisco] are invisible, because [City]
        // is the bottom level, but that shouldn't affect the total.
    	TestUtil.assertExprReturns(
			connection,
            "([Customers].[USA].[CA].[San Francisco])", "88");
    	TestUtil.assertExprThrows(
			connection,
			"([Customers].[USA].[CA].[Los Angeles])",
            "MDX object '[Customers].[USA].[CA].[Los Angeles]' not found in cube 'Sales'");

    	TestUtil.assertExprReturns(connection,"([Customers].[USA].[CA])", v1);
    	TestUtil.assertExprReturns(
			connection,
            "([Customers].[USA].[CA], [Gender].[F])", v2);
    	TestUtil.assertExprReturns(connection,"([Customers].[USA])", v3);

    	checkQuery(
    			connection,
            "select [Customers].Children on 0, "
            + "[Gender].Members on 1 from [Sales]");
    }

    /**
     * Calls various {@link SchemaReader} methods on the members returned in
     * a result set.
     *
     * @param testContext Test context
     * @param mdx MDX query
     */
    private void checkQuery(Connection connection, String mdx) {
        Result result = TestUtil.executeQuery(connection, mdx);
        final SchemaReader schemaReader =
        		connection.getSchemaReader().withLocus();
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
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testRollupPolicyNegative(Context foodMartContext) {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null,
                "<Role name=\"Role1\">\n"
                + "  <SchemaGrant access=\"none\">\n"
                + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
                + "      <HierarchyGrant hierarchy=\"[Customers]\" access=\"custom\" rollupPolicy=\"bad\" bottomLevel=\"[Customers].[City]\">\n"
                + "        <MemberGrant member=\"[Customers].[USA]\" access=\"all\"/>\n"
                + "        <MemberGrant member=\"[Customers].[USA].[CA].[Los Angeles]\" access=\"none\"/>\n"
                + "      </HierarchyGrant>\n"
                + "    </CubeGrant>\n"
                + "  </SchemaGrant>\n"
                + "</Role>");
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "Role1"); 
    	TestUtil.assertQueryThrows(
    			foodMartContext,
    			"select from [Sales]",
    			"Illegal rollupPolicy value 'bad'");
    }

    /**
     * Tests where all children are visible but a grandchild is not.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testRollupPolicyGreatGrandchildInvisible(Context foodMartContext) {
        rollupPolicyGreatGrandchildInvisible(
    		foodMartContext, Role.RollupPolicy.FULL, "266,773", "74,748");
        rollupPolicyGreatGrandchildInvisible(
    		foodMartContext, Role.RollupPolicy.PARTIAL, "266,767", "74,742");
        rollupPolicyGreatGrandchildInvisible(
    		foodMartContext, Role.RollupPolicy.HIDDEN, "", "");
    }

    private void rollupPolicyGreatGrandchildInvisible(
		Context foodMartContext,
        Role.RollupPolicy policy,
        String v1,
        String v2)
    {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null,
                "<Role name=\"Role1\">\n"
                + "  <SchemaGrant access=\"none\">\n"
                + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
                + "      <HierarchyGrant hierarchy=\"[Customers]\" access=\"custom\" rollupPolicy=\""
                + policy
                + "\">\n"
                + "        <MemberGrant member=\"[Customers].[USA]\" access=\"all\"/>\n"
                + "        <MemberGrant member=\"[Customers].[USA].[CA].[San Francisco].[Gladys Evans]\" access=\"none\"/>\n"
                + "      </HierarchyGrant>\n"
                + "    </CubeGrant>\n"
                + "  </SchemaGrant>\n"
                + "</Role>");
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "Role1"); 
    	Connection connection = foodMartContext.createConnection();
    	TestUtil.assertExprReturns(connection, "[Measures].[Unit Sales]", v1);
    	TestUtil.assertExprReturns(
			connection, "([Measures].[Unit Sales], [Customers].[USA])",
            v1);
    	TestUtil.assertExprReturns(
			connection,
            "([Measures].[Unit Sales], [Customers].[USA].[CA])",
            v2);
    }

    /**
     * Tests where two hierarchies are simultaneously access-controlled.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testRollupPolicySimultaneous(Context foodMartContext) {
//         note that v2 is different for full vs partial, v3 is the same
        rollupPolicySimultaneous(
    		foodMartContext, Role.RollupPolicy.FULL, "266,773", "74,748", "25,635");
        rollupPolicySimultaneous(
    		foodMartContext, Role.RollupPolicy.PARTIAL, "72,631", "72,631", "25,635");
        rollupPolicySimultaneous(
    		foodMartContext, Role.RollupPolicy.HIDDEN, "", "", "");
    }

    private void rollupPolicySimultaneous(
		Context foodMartContext,
        Role.RollupPolicy policy,
        String v1,
        String v2,
        String v3)
    {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null,
                "<Role name=\"Role1\">\n"
                + "  <SchemaGrant access=\"none\">\n"
                + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
                + "      <HierarchyGrant hierarchy=\"[Customers]\" access=\"custom\" rollupPolicy=\""
                + policy
                + "\">\n"
                + "        <MemberGrant member=\"[Customers].[USA]\" access=\"all\"/>\n"
                + "        <MemberGrant member=\"[Customers].[USA].[CA].[San Francisco].[Gladys Evans]\" access=\"none\"/>\n"
                + "      </HierarchyGrant>\n"
                + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"custom\" rollupPolicy=\""
                + policy
                + "\">\n"
                + "        <MemberGrant member=\"[Store].[USA].[CA]\" access=\"all\"/>\n"
                + "        <MemberGrant member=\"[Store].[USA].[CA].[San Francisco].[Store 14]\" access=\"none\"/>\n"
                + "      </HierarchyGrant>\n"
                + "    </CubeGrant>\n"
                + "  </SchemaGrant>\n"
                + "</Role>");
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "Role1"); 
    	Connection connection = foodMartContext.createConnection();
    	TestUtil.assertExprReturns(connection, "[Measures].[Unit Sales]", v1);
    	TestUtil.assertExprReturns(
			connection,
            "([Measures].[Unit Sales], [Customers].[USA])",
            v1);
    	TestUtil.assertExprReturns(
			connection,
            "([Measures].[Unit Sales], [Customers].[USA].[CA])",
            v2);
    	TestUtil.assertExprReturns(
			connection,
            "([Measures].[Unit Sales], "
            + "[Customers].[USA].[CA], [Store].[USA].[CA])",
            v2);
    	TestUtil.assertExprReturns(
			connection,
            "([Measures].[Unit Sales], "
            + "[Customers].[USA].[CA], "
            + "[Store].[USA].[CA].[San Diego])",
            v3);
    }

    // todo: performance test where 1 of 1000 children is not visible

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testUnionRole(Context foodMartContext) {
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
    	TestUtil.withSchema(foodMartContext, schema);

        Connection connection;

        try {
        	TestUtil.withRole(foodMartContext, "Role3,Role2"); 
        	connection = foodMartContext.createConnection();
        	fail("expected exception, got " + connection);
        } catch (RuntimeException e) {
            final String message = e.getMessage();
            assertTrue(message.indexOf("Role 'Role3' not found") >= 0, message);
        }

        try {
        	TestUtil.withRole(foodMartContext, "Role1,Role3"); 
        	connection = foodMartContext.createConnection();
            fail("expected exception, got " + connection);
        } catch (RuntimeException e) {
            final String message = e.getMessage();
            assertTrue(message.indexOf("Role 'Role3' not found") >= 0, message);
        }

    	TestUtil.withRole(foodMartContext, "Role1,Role2"); 
    	connection = foodMartContext.createConnection();

        // Cube access:
        // Both can see [Sales]
        // Role1 only see [Warehouse]
        // Neither can see [Warehouse and Sales]
        assertCubeAccess(connection, Access.ALL, "Sales");
        assertCubeAccess(connection, Access.ALL, "Warehouse");
        assertCubeAccess(connection, Access.NONE, "Warehouse and Sales");

        // Hierarchy access:
        // Both can see [Customers] with Custom access
        // Both can see [Store], Role1 with Custom access, Role2 with All access
        // Role1 can see [Promotion Media], Role2 cannot
        // Neither can see [Marital Status]
        assertHierarchyAccess(
            connection, Access.CUSTOM, "Sales", "[Customers]");
        assertHierarchyAccess(
            connection, Access.ALL, "Sales", "[Store]");
        assertHierarchyAccess(
            connection, Access.ALL, "Sales", "[Promotion Media]");
        assertHierarchyAccess(
            connection, Access.NONE, "Sales", "[Marital Status]");

        // Rollup policy is the greater of Role1's partian and Role2's hidden
        final Role.HierarchyAccess hierarchyAccess =
            getHierarchyAccess(connection, "Sales", "[Store]");
        assertEquals(
            Role.RollupPolicy.PARTIAL,
            hierarchyAccess.getRollupPolicy());
        // One of the roles is restricting the levels, so we
        // expect only the levels from 2 to 4 to be available.
        assertEquals(2, hierarchyAccess.getTopLevelDepth());
        assertEquals(4, hierarchyAccess.getBottomLevelDepth());

        // Member access:
        // both can see [USA]
        assertMemberAccess(connection, Access.CUSTOM, "[Customers].[USA]");
        // Role1 can see [CA], Role2 cannot
        assertMemberAccess(connection, Access.CUSTOM, "[Customers].[USA].[CA]");
        // Role1 cannoy see [USA].[OR].[Portland], Role2 can
        assertMemberAccess(
            connection, Access.ALL, "[Customers].[USA].[OR].[Portland]");
        // Role1 cannot see [USA].[OR], Role2 can see it by virtue of [Portland]
        assertMemberAccess(
            connection, Access.CUSTOM, "[Customers].[USA].[OR]");
        // Neither can see Beaverton
        assertMemberAccess(
            connection, Access.NONE, "[Customers].[USA].[OR].[Beaverton]");

        // Rollup policy
        String mdx = "select Hierarchize(\n"
            + "{[Customers].[USA].Children,\n"
            + " [Customers].[USA].[OR].Children}) on 0\n"
            + "from [Sales]";
        TestUtil.withRole(foodMartContext, null);
        connection = foodMartContext.createConnection();
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

    	TestUtil.withRole(foodMartContext, "Role1"); 
    	connection = foodMartContext.createConnection();
    	TestUtil.assertQueryThrows(
			connection,
            mdx,
            "MDX object '[Customers].[USA].[OR]' not found in cube 'Sales'");

    	TestUtil.withRole(foodMartContext, "Role2"); 
    	connection = foodMartContext.createConnection();
    	TestUtil.assertQueryThrows(
			connection,
            mdx,
            "MDX cube 'Sales' not found");

        // Compared to above:
        // a. cities in Oregon are missing besides Portland
        // b. total for Oregon = total for Portland
    	TestUtil.withRole(foodMartContext, "Role1,Role2");    	
    	connection = foodMartContext.createConnection();
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testUnionOfUnionRole(Context foodMartContext) {
        String roleDefs =
            "<Role name=\"USA manager\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
            + "      <DimensionGrant access=\"all\" dimension=\"[Measures]\"/>\n"
            + "      <HierarchyGrant access=\"custom\" hierarchy=\"[Customers]\">\n"
            + "        <MemberGrant access=\"all\" member=\"[Customers].[USA]\"/>\n"
            + "      </HierarchyGrant>\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>\n"
            + "<Role name=\"parent of USA manager\">\n"
            + "  <Union>\n"
            + "    <RoleUsage roleName=\"USA manager\"/>\n"
            + "  </Union>\n"
            + "</Role>"
            + "<Role name=\"grandparent of USA manager\">\n"
            + "  <Union>\n"
            + "    <RoleUsage roleName=\"parent of USA manager\"/>\n"
            + "  </Union>\n"
            + "</Role>";

        String baseSchema = TestUtil.getRawSchema(foodMartContext);
        String schema = SchemaUtil.getSchema(baseSchema, null, null, null, null, null, roleDefs);
        TestUtil.withSchema(foodMartContext, schema);
        TestUtil.withRole(foodMartContext, "grandparent of USA manager"); 
        Connection connection = foodMartContext.createConnection();

        // Can access [Sales]?
        assertCubeAccess(connection, Access.ALL, "Sales");

        // Has custom access to [Customers]?
        assertHierarchyAccess
                (connection, Access.CUSTOM, "Sales", "[Customers]");

        final Role.HierarchyAccess hierarchyAccess =
                getHierarchyAccess(connection, "Sales", "[Customers]");

        // Can access all levels of the [Customers] hierarchy?
        assertEquals(0, hierarchyAccess.getTopLevelDepth());
        assertEquals(4, hierarchyAccess.getBottomLevelDepth());

        // Can see [USA]?
        assertMemberAccess(connection, Access.ALL, "[Customers].[USA]");

        // Cannot see [Mexico]?
        assertMemberAccess(connection, Access.NONE, "[Customers].[Mexico]");
    }

    /**
     * This is a test for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1384">MONDRIAN-1384</a>
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testUnionRoleHasInaccessibleDescendants(Context foodMartContext) throws Exception {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null,
                "<Role name=\"Role1\">\n"
                + "  <SchemaGrant access=\"none\">\n"
                + "  </SchemaGrant>\n"
                + "</Role>\n"
                + "<Role name=\"Role2\">\n"
                + "  <SchemaGrant access=\"all\">\n"
                + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
                + "      <HierarchyGrant hierarchy=\"[Customers]\" access=\"custom\" rollupPolicy=\"partial\">\n"
                + "        <MemberGrant member=\"[Customers].[USA].[OR]\" access=\"all\"/>\n"
                + "      </HierarchyGrant>\n"
                + "    </CubeGrant>\n"
                + "  </SchemaGrant>\n"
                + "</Role>\n");
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "Role1,Role2"); 
    	Connection connection = foodMartContext.createConnection();
        final Cube cube =
            connection.getSchema()
                .lookupCube("Sales", true);
        final HierarchyAccess accessDetails =
            connection.getRole().getAccessDetails(
                cube.lookupHierarchy(
                    new Id.NameSegment("Customers", Id.Quoting.UNQUOTED),
                    false));
        final SchemaReader scr =
            cube.getSchemaReader(null).withLocus();
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
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testRoleUnionWithLevelRestrictions(Context foodMartContext)  throws Exception {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null,
                "<Role name=\"Role1\">\n"
                + "  <SchemaGrant access=\"all\">\n"
                + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
                + "      <HierarchyGrant hierarchy=\"[Customers]\" access=\"custom\" rollupPolicy=\"Partial\" topLevel=\"[Customers].[State Province]\" bottomLevel=\"[Customers].[State Province]\">\n"
                + "        <MemberGrant member=\"[Customers].[USA].[CA]\" access=\"all\"/>\n"
                + "      </HierarchyGrant>\n"
                + "    </CubeGrant>\n"
                + "  </SchemaGrant>\n"
                + "</Role>\n"
                + "<Role name=\"Role2\">\n"
                + "  <SchemaGrant access=\"none\">\n"
                + "  </SchemaGrant>\n"
                + "</Role>\n");
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "Role1,Role2"); 
    	Connection connection = foodMartContext.createConnection();

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

        SchemaReader reader =
        		connection.getSchemaReader().withLocus();
        Cube cube = null;
        for (Cube c : reader.getCubes()) {
            if (c.getName().equals("Sales")) {
                cube = c;
            }
        }
        assertNotNull(cube);
        reader =
            cube.getSchemaReader(connection.getRole());
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testNonEmptyAccess(Context foodMartContext) {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null,
                "<Role name=\"Role1\">\n"
                + "  <SchemaGrant access=\"none\">\n"
                + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
                + "      <HierarchyGrant hierarchy=\"[Product]\" access=\"custom\">\n"
                + "        <MemberGrant member=\"[Product].[Drink]\" access=\"all\"/>\n"
                + "      </HierarchyGrant>\n"
                + "    </CubeGrant>\n"
                + "  </SchemaGrant>\n"
                + "</Role>");
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "Role1"); 
    	Connection connection = foodMartContext.createConnection();

        // regular crossjoin returns the correct list of product children
        final String expected =
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Gender].[All Gender], [Product].[Drink]}\n"
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testNonEmptyAccessLevelMembers(Context foodMartContext) {
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
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "Role1"); 
    	Connection connection = foodMartContext.createConnection();

        // <Level>.members inside regular crossjoin returns the correct list of
        // product members
        final String expected =
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Gender].[All Gender], [Product].[Drink]}\n"
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testGoodman(Context foodMartContext) {
        final String query = "select {[Measures].[Unit Sales]} ON COLUMNS,\n"
            + "Hierarchize(Union(Union(Union({[Store].[All Stores]},"
            + " [Store].[All Stores].Children),"
            + " [Store].[All Stores].[USA].Children),"
            + " [Store].[All Stores].[USA].[CA].Children)) ON ROWS\n"
            + "from [Sales]\n"
            + "where [Time].[1997]";

        // Note that total for [Store].[All Stores] and [Store].[USA] is sum
        // of visible children [Store].[CA] and [Store].[OR].[Portland].
        setGoodmanContext(foodMartContext, Role.RollupPolicy.PARTIAL);
        Connection connection = foodMartContext.createConnection();
        TestUtil.assertQueryReturns(
    		connection,
            query,
            "Axis #0:\n"
            + "{[Time].[1997]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[All Stores]}\n"
            + "{[Store].[USA]}\n"
            + "{[Store].[USA].[CA]}\n"
            + "{[Store].[USA].[CA].[Alameda]}\n"
            + "{[Store].[USA].[CA].[Beverly Hills]}\n"
            + "{[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[USA].[CA].[San Francisco]}\n"
            + "{[Store].[USA].[OR]}\n"
            + "Row #0: 100,827\n"
            + "Row #1: 100,827\n"
            + "Row #2: 74,748\n"
            + "Row #3: \n"
            + "Row #4: 21,333\n"
            + "Row #5: 25,663\n"
            + "Row #6: 25,635\n"
            + "Row #7: 2,117\n"
            + "Row #8: 26,079\n");

        setGoodmanContext(foodMartContext, Role.RollupPolicy.FULL);
        connection = foodMartContext.createConnection();
        TestUtil.assertQueryReturns(
    		connection,
            query,
            "Axis #0:\n"
            + "{[Time].[1997]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[All Stores]}\n"
            + "{[Store].[USA]}\n"
            + "{[Store].[USA].[CA]}\n"
            + "{[Store].[USA].[CA].[Alameda]}\n"
            + "{[Store].[USA].[CA].[Beverly Hills]}\n"
            + "{[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[USA].[CA].[San Francisco]}\n"
            + "{[Store].[USA].[OR]}\n"
            + "Row #0: 266,773\n"
            + "Row #1: 266,773\n"
            + "Row #2: 74,748\n"
            + "Row #3: \n"
            + "Row #4: 21,333\n"
            + "Row #5: 25,663\n"
            + "Row #6: 25,635\n"
            + "Row #7: 2,117\n"
            + "Row #8: 67,659\n");

        setGoodmanContext(foodMartContext, Role.RollupPolicy.HIDDEN);
        connection = foodMartContext.createConnection();
        TestUtil.assertQueryReturns(
    		connection,
            query,
            "Axis #0:\n"
            + "{[Time].[1997]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[All Stores]}\n"
            + "{[Store].[USA]}\n"
            + "{[Store].[USA].[CA]}\n"
            + "{[Store].[USA].[CA].[Alameda]}\n"
            + "{[Store].[USA].[CA].[Beverly Hills]}\n"
            + "{[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[USA].[CA].[San Francisco]}\n"
            + "{[Store].[USA].[OR]}\n"
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

    private static void setGoodmanContext(Context foodMartContext, final Role.RollupPolicy policy) {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null,
                "<Role name=\"California manager\">\n"
                + "  <SchemaGrant access=\"none\">\n"
                + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
                + "      <HierarchyGrant hierarchy=\"[Store]\" rollupPolicy=\""
                + policy.name().toLowerCase()
                + "\" access=\"custom\">\n"
                + "        <MemberGrant member=\"[Store].[USA].[CA]\" access=\"all\"/>\n"
                + "        <MemberGrant member=\"[Store].[USA].[OR].[Portland]\" access=\"all\"/>\n"
                + "      </HierarchyGrant>"
                + "    </CubeGrant>\n"
                + "  </SchemaGrant>\n"
                + "</Role>");
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "California manager"); 
    }

    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-402">
     * MONDRIAN-402, "Bug in RolapCubeHierarchy.hashCode() ?"</a>.
     * Access-control elements for hierarchies with
     * same name in different cubes could not be distinguished.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testBugMondrian402(Context foodMartContext) {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null,
                "<Role name=\"California manager\">\n"
                + "  <SchemaGrant access=\"none\">\n"
                + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
                + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"none\" />\n"
                + "    </CubeGrant>\n"
                + "    <CubeGrant cube=\"Sales Ragged\" access=\"all\">\n"
                + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"custom\" />\n"
                + "    </CubeGrant>\n"
                + "  </SchemaGrant>\n"
                + "</Role>");
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "California manager"); 
    	Connection connection = foodMartContext.createConnection();
        assertHierarchyAccess(
    		connection, Access.NONE, "Sales", "Store");
        assertHierarchyAccess(
    		connection,
            Access.CUSTOM,
            "Sales Ragged",
            "Store");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testPartialRollupParentChildHierarchy(Context foodMartContext) {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
            null, null, null, null, null,
            "<Role name=\"Buggy Role\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"HR\" access=\"all\">\n"
            + "      <HierarchyGrant hierarchy=\"[Employees]\" access=\"custom\"\n"
            + "                      rollupPolicy=\"partial\">\n"
            + "        <MemberGrant\n"
            + "            member=\"[Employees].[All Employees].[Sheri Nowmer].[Darren Stanz]\"\n"
            + "            access=\"all\"/>\n"
            + "      </HierarchyGrant>\n"
            + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"custom\"\n"
            + "                      rollupPolicy=\"partial\">\n"
            + "        <MemberGrant member=\"[Store].[All Stores].[USA].[CA]\" access=\"all\"/>\n"
            + "      </HierarchyGrant>\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>");
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "Buggy Role"); 
    	Connection connection = foodMartContext.createConnection();

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
            + "{[Store].[All Stores]}\n"
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
            + "{[Employees].[All Employees]}\n"
            + "Row #0: 1\n");
        checkQuery(connection, mdx2);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testParentChildUserDefinedRole(Context foodMartContext)
    {
        final Connection connection = foodMartContext.createConnection();
        final Role savedRole = connection.getRole();
        try {
            // Run queries as top-level employee.
            connection.setRole(
                new PeopleRole(
                    savedRole, connection.getSchema(), "Sheri Nowmer"));
            TestUtil.assertExprReturns(
        		connection,
        		"HR",
                "[Employees].Members.Count",
                "1,156");

            // Level 2 employee
            connection.setRole(
                new PeopleRole(
                    savedRole, connection.getSchema(), "Derrick Whelply"));
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
                "[Employees].[All Employees]\n"
                + "[Employees].[Sheri Nowmer]\n"
                + "[Employees].[Sheri Nowmer].[Derrick Whelply]\n"
                + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Beverly Baker]\n"
                + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Ed Young].[Gregory Whiting].[Merrill Steel]\n"
                + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Ed Young].[Gregory Whiting].[Melissa Marple]");

            // Leaf employee
            connection.setRole(
                new PeopleRole(
                    savedRole, connection.getSchema(), "Ann Weyerhaeuser"));
            TestUtil.assertExprReturns(
        		connection,
        		"HR",
                "[Employees].Members.Count",
                "7");
            TestUtil.assertAxisReturns(
        		connection,
        		"HR",
                "[Employees].Members",
                "[Employees].[All Employees]\n"
                + "[Employees].[Sheri Nowmer]\n"
                + "[Employees].[Sheri Nowmer].[Derrick Whelply]\n"
                + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges]\n"
                + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Cody Goldey]\n"
                + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Cody Goldey].[Shanay Steelman]\n"
                + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Cody Goldey].[Shanay Steelman].[Ann Weyerhaeuser]");
        } finally {
            connection.setRole(savedRole);
        }
    }

    /**
     * Test case for
     * <a href="http://jira.pentaho.com/browse/BISERVER-1574">BISERVER-1574,
     * "Cube role rollupPolicy='partial' failure"</a>. The problem was a
     * NullPointerException in
     * {@link SchemaReader#getMemberParent(mondrian.olap.Member)} when called
     * on a members returned in a result set. JPivot calls that method but
     * Mondrian normally does not.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testBugBiserver1574(Context foodMartContext) {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null, BiServer1574Role1);
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "role1"); 
    	Connection connection = foodMartContext.createConnection();
        final String mdx =
            "select {([Measures].[Store Invoice], [Store Size in SQFT].[All Store Size in SQFTs])} ON COLUMNS,\n"
            + "  {[Warehouse].[All Warehouses]} ON ROWS\n"
            + "from [Warehouse]";
        checkQuery(connection, mdx);
        TestUtil.assertQueryReturns(
    		connection,
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Store Invoice], [Store Size in SQFT].[All Store Size in SQFTs]}\n"
            + "Axis #2:\n"
            + "{[Warehouse].[All Warehouses]}\n"
            + "Row #0: 4,042.96\n");
    }

    /**
     * Testcase for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-435">
     * MONDRIAN-435, "Internal error in HierarchizeArrayComparator"</a>. Occurs
     * when apply Hierarchize function to tuples on a hierarchy with
     * partial-rollup.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testBugMondrian435(Context foodMartContext) {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null, BiServer1574Role1);
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "role1"); 
    	Connection connection = foodMartContext.createConnection();

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
            + "{[Store Size in SQFT].[All Store Size in SQFTs], [Product].[All Products]}\n"
            + "{[Store Size in SQFT].[20319], [Product].[All Products]}\n"
            + "Axis #2:\n"
            + "{[Store Type].[All Store Types]}\n"
            + "{[Store Type].[Supermarket]}\n"
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
            + "{[Store Size in SQFT].[All Store Size in SQFTs], [Product].[All Products]}\n"
            + "{[Store Size in SQFT].[All Store Size in SQFTs], [Product].[Drink].[Dairy]}\n"
            + "{[Store Size in SQFT].[20319], [Product].[All Products]}\n"
            + "{[Store Size in SQFT].[20319], [Product].[Food]}\n"
            + "Axis #2:\n"
            + "{[Store Type].[All Store Types]}\n"
            + "{[Store Type].[Supermarket]}\n"
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
            + "{[Store Size in SQFT].[All Store Size in SQFTs], [Product].[All Products]}\n"
            + "{[Store Size in SQFT].[All Store Size in SQFTs], [Product].[All Products]}\n"
            + "{[Store Size in SQFT].[20319], [Product].[All Products]}\n"
            + "{[Store Size in SQFT].[20319], [Product].[Food]}\n"
            + "Axis #2:\n"
            + "{[Store Type].[All Store Types]}\n"
            + "{[Store Type].[Supermarket]}\n"
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
            + "{[Product].[All Products], [Store Type].[All Store Types], [Store Size in SQFT].[All Store Size in SQFTs]}\n"
            + "{[Product].[All Products], [Store Type].[All Store Types], [Store Size in SQFT].[20319]}\n"
            + "{[Product].[All Products], [Store Type].[Supermarket], [Store Size in SQFT].[All Store Size in SQFTs]}\n"
            + "{[Product].[All Products], [Store Type].[Supermarket], [Store Size in SQFT].[20319]}\n"
            + "{[Product].[Drink].[Dairy], [Store Type].[All Store Types], [Store Size in SQFT].[All Store Size in SQFTs]}\n"
            + "{[Product].[Drink].[Dairy], [Store Type].[All Store Types], [Store Size in SQFT].[20319]}\n"
            + "{[Product].[Drink].[Dairy], [Store Type].[Supermarket], [Store Size in SQFT].[All Store Size in SQFTs]}\n"
            + "{[Product].[Drink].[Dairy], [Store Type].[Supermarket], [Store Size in SQFT].[20319]}\n"
            + "{[Product].[Food], [Store Type].[All Store Types], [Store Size in SQFT].[All Store Size in SQFTs]}\n"
            + "{[Product].[Food], [Store Type].[All Store Types], [Store Size in SQFT].[20319]}\n"
            + "{[Product].[Food], [Store Type].[Supermarket], [Store Size in SQFT].[All Store Size in SQFTs]}\n"
            + "{[Product].[Food], [Store Type].[Supermarket], [Store Size in SQFT].[20319]}\n"
            + "{[Product].[Food].[Eggs], [Store Type].[All Store Types], [Store Size in SQFT].[All Store Size in SQFTs]}\n"
            + "{[Product].[Food].[Eggs], [Store Type].[All Store Types], [Store Size in SQFT].[20319]}\n"
            + "{[Product].[Food].[Eggs], [Store Type].[Supermarket], [Store Size in SQFT].[All Store Size in SQFTs]}\n"
            + "{[Product].[Food].[Eggs], [Store Type].[Supermarket], [Store Size in SQFT].[20319]}\n"
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testBugMondrian436(Context foodMartContext) {
        propSaver.set(propSaver.properties.EnableNativeCrossJoin, true);
        propSaver.set(propSaver.properties.EnableNativeFilter, true);
        propSaver.set(propSaver.properties.EnableNativeNonEmpty, true);
        propSaver.set(propSaver.properties.EnableNativeTopCount, true);
        propSaver.set(propSaver.properties.ExpandNonNative, true);

        // Run with native enabled, then with whatever properties are set for
        // this test run.
        checkBugMondrian436(foodMartContext);
        propSaver.reset();
        checkBugMondrian436(foodMartContext);
    }

    private void checkBugMondrian436(Context foodMartContext) {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null, BiServer1574Role1);
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "role1"); 
    	Connection connection = foodMartContext.createConnection();

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
            + "{[Store Size in SQFT].[All Store Size in SQFTs], [Product].[Drink]}\n"
            + "{[Store Size in SQFT].[All Store Size in SQFTs], [Product].[Drink].[Dairy]}\n"
            + "{[Store Size in SQFT].[All Store Size in SQFTs], [Product].[Food]}\n"
            + "{[Store Size in SQFT].[20319], [Product].[Drink]}\n"
            + "{[Store Size in SQFT].[20319], [Product].[Food]}\n"
            + "{[Store Size in SQFT].[20319], [Product].[Non-Consumable]}\n"
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
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testVirtualCube(Context foodMartContext) {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
            null, null, null, null, null,
            "<Role name=\"VCRole\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"Warehouse and Sales\" access=\"all\">\n"
            + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"custom\">\n"
            + "        <MemberGrant member=\"[Store].[USA].[CA]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Store].[USA].[CA].[Los Angeles]\" access=\"none\"/>\n"
            + "      </HierarchyGrant>\n"
            + "      <HierarchyGrant hierarchy=\"[Customers]\" access=\"custom\"\n"
            + "          topLevel=\"[Customers].[State Province]\" bottomLevel=\"[Customers].[City]\">\n"
            + "        <MemberGrant member=\"[Customers].[USA].[CA]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Customers].[USA].[CA].[Los Angeles]\" access=\"none\"/>\n"
            + "      </HierarchyGrant>\n"
            + "      <HierarchyGrant hierarchy=\"[Gender]\" access=\"none\"/>\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>");
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "VCRole"); 
    	Connection connection = foodMartContext.createConnection();
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testBugBiserver2491(Context foodMartContext) {
        final String BiServer2491Role2 =
            "<Role name=\"role2\">"
            + " <SchemaGrant access=\"none\">"
            + "  <CubeGrant cube=\"Sales\" access=\"all\">"
            + "   <HierarchyGrant hierarchy=\"[Store]\" access=\"custom\" rollupPolicy=\"partial\">"
            + "    <MemberGrant member=\"[Store].[USA].[CA]\" access=\"all\"/>"
            + "    <MemberGrant member=\"[Store].[USA].[CA].[Los Angeles]\" access=\"none\"/>"
            + "   </HierarchyGrant>"
            + "  </CubeGrant>"
            + " </SchemaGrant>"
            + "</Role>";

        String baseSchema = TestUtil.getRawSchema(foodMartContext);
        String schema = SchemaUtil.getSchema(baseSchema, null, null, null, null, null, BiServer2491Role2);
        TestUtil.withSchema(foodMartContext, schema);
        TestUtil.withRole(foodMartContext, "role2"); 
        Connection connection = foodMartContext.createConnection();

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
            + "{[Store].[USA]}\n"
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
            + "{[Store].[USA].[CA].[Alameda].[HQ]}\n"
            + "{[Store].[USA].[CA].[Beverly Hills].[Store 6]}\n"
            + "{[Store].[USA].[CA].[San Diego].[Store 24]}\n"
            + "{[Store].[USA].[CA].[San Francisco].[Store 14]}\n"
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testBugMondrian622(Context foodMartContext) {
        StringBuilder buf = new StringBuilder();
        StringBuilder buf2 = new StringBuilder();
        final String cubeName = "Sales with multiple customers";
        Connection connection = foodMartContext.createConnection();
        final Result result = TestUtil.executeQuery(
    		connection,
            "select [Customers].[City].Members on 0 from [Sales]");
        for (Position position : result.getAxes()[0].getPositions()) {
            Member member = position.get(0);
            String name = member.getParentMember().getName()
                + "."
                + member.getName(); // e.g. "BC.Burnaby"
            // e.g. "[Customers].[State Province].[BC].[Burnaby]"
            String uniqueName =
                Util.replace(member.getUniqueName(), ".[All Customers]", "");
            // e.g. "[Customers2].[State Province].[BC].[Burnaby]"
            String uniqueName2 =
                Util.replace(uniqueName, "Customers", "Customers2");
            // e.g. "[Customers3].[State Province].[BC].[Burnaby]"
            String uniqueName3 =
                Util.replace(uniqueName, "Customers", "Customers3");
            buf.append(
                "  <Role name=\"" + name + "\"> \n"
                + "    <SchemaGrant access=\"none\"> \n"
                + "      <CubeGrant access=\"all\" cube=\"" + cubeName
                + "\"> \n"
                + "        <HierarchyGrant access=\"custom\" hierarchy=\"[Customers]\" rollupPolicy=\"partial\"> \n"
                + "          <MemberGrant access=\"all\" member=\""
                + uniqueName + "\"/> \n"
                + "        </HierarchyGrant> \n"
                + "        <HierarchyGrant access=\"custom\" hierarchy=\"[Customers2]\" rollupPolicy=\"partial\"> \n"
                + "          <MemberGrant access=\"all\" member=\""
                + uniqueName2 + "\"/> \n"
                + "        </HierarchyGrant> \n"
                + "        <HierarchyGrant access=\"custom\" hierarchy=\"[Customers3]\" rollupPolicy=\"partial\"> \n"
                + "          <MemberGrant access=\"all\" member=\""
                + uniqueName3 + "\"/> \n"
                + "        </HierarchyGrant> \n"
                + "      </CubeGrant> \n"
                + "    </SchemaGrant> \n"
                + "  </Role> \n");

            buf2.append("    <RoleUsage roleName=\"" + name + "\"/>\n");
        }
        String baseSchema = TestUtil.getRawSchema(foodMartContext);
        String schema = SchemaUtil.getSchema(baseSchema, 
            " <Dimension name=\"Customers\"> \n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\"> \n"
            + "      <Table name=\"customer\"/> \n"
            + "      <Level name=\"Country\" column=\"country\" uniqueMembers=\"true\"/> \n"
            + "      <Level name=\"State Province\" column=\"state_province\" uniqueMembers=\"true\"/> \n"
            + "      <Level name=\"City\" column=\"city\" uniqueMembers=\"false\"/> \n"
            + "      <Level name=\"Name\" column=\"customer_id\" type=\"Numeric\" uniqueMembers=\"true\"/> \n"
            + "    </Hierarchy> \n"
            + "  </Dimension> ",
            "  <Cube name=\"" + cubeName + "\"> \n"
            + "    <Table name=\"sales_fact_1997\"/> \n"
            + "    <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/> \n"
            + "    <DimensionUsage name=\"Product\" source=\"Product\" foreignKey=\"product_id\"/> \n"
            + "    <DimensionUsage name=\"Customers\" source=\"Customers\" foreignKey=\"customer_id\"/> \n"
            + "    <DimensionUsage name=\"Customers2\" source=\"Customers\" foreignKey=\"customer_id\"/> \n"
            + "    <DimensionUsage name=\"Customers3\" source=\"Customers\" foreignKey=\"customer_id\"/> \n"
            + "    <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" formatString=\"Standard\"/> \n"
            + "  </Cube> \n",
            null, null, null,
            buf.toString()
            + "  <Role name=\"Test\"> \n"
            + "    <Union>\n"
            + buf2.toString()
            + "    </Union>\n"
            + "  </Role>\n");
        final long t0 = System.currentTimeMillis();
        TestUtil.withSchema(foodMartContext, schema);
        TestUtil.withRole(foodMartContext, "Test"); 
        connection = foodMartContext.createConnection();
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testBugMondrian694(Context foodMartContext) {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null,
                "<Role name=\"REG1\"> \n"
                + "  <SchemaGrant access=\"none\"> \n"
                + "    <CubeGrant cube=\"HR\" access=\"all\"> \n"
                + "      <HierarchyGrant hierarchy=\"Employees\" access=\"custom\" rollupPolicy=\"partial\"> \n"
                + "        <MemberGrant member=\"[Employees].[All Employees]\" access=\"none\"/>\n"
                + "        <MemberGrant member=\"[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Cody Goldey].[Shanay Steelman].[Steven Betsekas]\" access=\"all\"/> \n"
                + "        <MemberGrant member=\"[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Cody Goldey].[Shanay Steelman].[Arvid Ziegler]\" access=\"all\"/> \n"
                + "        <MemberGrant member=\"[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Cody Goldey].[Shanay Steelman].[Ann Weyerhaeuser]\" access=\"all\"/> \n"
                + "      </HierarchyGrant> \n"
                + "    </CubeGrant> \n"
                + "  </SchemaGrant> \n"
                + "</Role>");
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "REG1"); 
    	Connection connection = foodMartContext.createConnection();

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
            + "{[Department].[14], [Employees].[All Employees]}\n"
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
            + "{[Department].[14], [Employees].[All Employees]}\n"
            + "{[Department].[14], [Employees].[Sheri Nowmer]}\n"
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
            + "{[Department].[14], [Employees].[All Employees]}\n"
            + "{[Department].[14], [Employees].[Sheri Nowmer]}\n"
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
            + "{[Employees].[All Employees], [Department].[14]}\n"
            + "{[Employees].[Sheri Nowmer], [Department].[14]}\n"
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testBugMondrian722(Context foodMartContext) {
        propSaver.set(
            MondrianProperties.instance().IgnoreInvalidMembers,
            true);
        String baseSchema = TestUtil.getRawSchema(foodMartContext);
        String schema = SchemaUtil.getSchema(baseSchema, 
            null, null, null, null, null,
            "<Role name=\"CTO\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
            + "      <HierarchyGrant hierarchy=\"[Customers]\" access=\"custom\">\n"
            + "        <MemberGrant member=\"[Customers].[USA].[XX]\" access=\"none\"/>\n"
            + "        <MemberGrant member=\"[Customers].[USA].[XX].[Yyy Yyyyyyy]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Customers].[USA]\" access=\"none\"/>\n"
            + "        <MemberGrant member=\"[Customers].[USA].[CA]\" access=\"none\"/>\n"
            + "        <MemberGrant member=\"[Customers].[USA].[CA].[Los Angeles]\" access=\"all\"/>\n"
            + "        <MemberGrant member=\"[Customers].[USA].[CA].[Zzz Zzzz]\" access=\"none\"/>\n"
            + "        <MemberGrant member=\"[Customers].[USA].[CA].[San Francisco]\" access=\"all\"/>\n"
            + "      </HierarchyGrant>\n"
            + "      <HierarchyGrant hierarchy=\"[Gender]\" access=\"none\"/>\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>");
        TestUtil.withSchema(foodMartContext, schema);
        TestUtil.withRole(foodMartContext, "CTO"); 
        Connection connection = foodMartContext.createConnection();
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
                + "{[Customers].[USA].[CA]}\n"
                + "{[Customers].[USA].[CA].[Los Angeles]}\n"
                + "{[Customers].[USA].[CA].[San Francisco]}\n"
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testCalcMemberLevel(Context foodMartContext) {
    	Connection connection = foodMartContext.createConnection();
        checkCalcMemberLevel(connection);
        
        String baseSchema = TestUtil.getRawSchema(foodMartContext);
        String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null,
                "<Role name=\"Role1\">\n"
                + "  <SchemaGrant access=\"none\">\n"
                + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
                + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"custom\" rollupPolicy=\"Partial\" topLevel=\"[Store].[Store State]\">\n"
                + "      </HierarchyGrant>\n"
                + "    </CubeGrant>\n"
                + "  </SchemaGrant>\n"
                + "</Role>\n");
        TestUtil.withSchema(foodMartContext, schema);
        TestUtil.withRole(foodMartContext, "Role1"); 
        connection = foodMartContext.createConnection();
        checkCalcMemberLevel(connection);
    }

    /**
     * Test for bug MONDRIAN-568. Grants on OLAP elements are validated
     * by name, thus granting implicit access to all cubes which have
     * a dimension with the same name.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testBugMondrian568(Context foodMartContext) {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null,
                "<Role name=\"Role1\">\n"
                + "  <SchemaGrant access=\"none\">\n"
                + "    <CubeGrant cube=\"Sales\" access=\"none\">\n"
                + "      <HierarchyGrant hierarchy=\"[Measures]\" access=\"custom\">\n"
                + "        <MemberGrant member=\"[Measures].[Unit Sales]\" access=\"all\"/>\n"
                + "      </HierarchyGrant>"
                + "    </CubeGrant>\n"
                + "  </SchemaGrant>\n"
                + "</Role>\n"
                + "<Role name=\"Role2\">\n"
                + "  <SchemaGrant access=\"none\">\n"
                + "    <CubeGrant cube=\"Sales Ragged\" access=\"all\"/>\n"
                + "  </SchemaGrant>\n"
                + "</Role>");
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "Role1"); 
    	Connection connection = foodMartContext.createConnection();
        assertMemberAccess(
        		connection,
                Access.NONE,
                "[Measures].[Store Cost]");
        
    	TestUtil.withRole(foodMartContext, "Role1,Role2"); 
    	connection = foodMartContext.createConnection();
        assertMemberAccess(
        		connection,
            Access.NONE,
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testBugMondrian935(Context foodMartContext) {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null,
                "<Role name='Role1'>\n"
                + "  <SchemaGrant access='none'>\n"
                + "    <CubeGrant cube='Sales' access='all'>\n"
                + "      <HierarchyGrant hierarchy='[Store Type]' access='custom' rollupPolicy='partial'>\n"
                + "        <MemberGrant member='[Store Type].[All Store Types]' access='none'/>\n"
                + "        <MemberGrant member='[Store Type].[Supermarket]' access='all'/>\n"
                + "      </HierarchyGrant>\n"
                + "      <HierarchyGrant hierarchy='[Customers]' access='custom' rollupPolicy='partial' >\n"
                + "        <MemberGrant member='[Customers].[All Customers]' access='none'/>\n"
                + "        <MemberGrant member='[Customers].[USA].[WA]' access='all'/>\n"
                + "        <MemberGrant member='[Customers].[USA].[CA]' access='none'/>\n"
                + "        <MemberGrant member='[Customers].[USA].[CA].[Los Angeles]' access='all'/>\n"
                + "      </HierarchyGrant>\n"
                + "    </CubeGrant>\n"
                + "  </SchemaGrant>\n"
                + "</Role>\n");

    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "Role1"); 
    	Connection connection = foodMartContext.createConnection();
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
            + "{[Customers].[USA].[CA], [Store Type].[Supermarket]}\n"
            + "{[Customers].[USA].[WA], [Store Type].[Supermarket]}\n"
            + "Row #0: 1,118\n"
            + "Row #1: 73,178\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testDimensionGrant(Context foodMartContext) throws Exception {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
            null, null, null, null, null,
            "<Role name=\"Role1\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"Sales\" access=\"custom\">\n"
            + "      <DimensionGrant dimension=\"[Measures]\" access=\"all\" />\n"
            + "      <DimensionGrant dimension=\"[Education Level]\" access=\"all\" />\n"
            + "      <DimensionGrant dimension=\"[Gender]\" access=\"all\" />\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>\n"
            + "<Role name=\"Role2\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"Sales\" access=\"custom\">\n"
            + "      <DimensionGrant dimension=\"[Measures]\" access=\"all\" />\n"
            + "      <DimensionGrant dimension=\"[Education Level]\" access=\"all\" />\n"
            + "      <DimensionGrant dimension=\"[Customers]\" access=\"none\" />\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>\n"
            + "<Role name=\"Role3\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"Sales\" access=\"custom\">\n"
            + "      <DimensionGrant dimension=\"[Education Level]\" access=\"all\" />\n"
            + "      <DimensionGrant dimension=\"[Measures]\" access=\"custom\" />\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>\n");
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "Role1"); 
    	Connection connection = foodMartContext.createConnection();
    	TestUtil.assertAxisReturns(
			connection,
            "[Education Level].Members",
            "[Education Level].[All Education Levels]\n"
            + "[Education Level].[Bachelors Degree]\n"
            + "[Education Level].[Graduate Degree]\n"
            + "[Education Level].[High School Degree]\n"
            + "[Education Level].[Partial College]\n"
            + "[Education Level].[Partial High School]");
    	TestUtil.assertAxisThrows(
			connection,
            "[Customers].Members",
            "Mondrian Error:Failed to parse query 'select {[Customers].Members} on columns from Sales'");
    	TestUtil.assertQueryReturns(
			connection,
            "select {[Education Level].Members} on columns, {[Measures].[Unit Sales]} on rows from Sales",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Education Level].[All Education Levels]}\n"
            + "{[Education Level].[Bachelors Degree]}\n"
            + "{[Education Level].[Graduate Degree]}\n"
            + "{[Education Level].[High School Degree]}\n"
            + "{[Education Level].[Partial College]}\n"
            + "{[Education Level].[Partial High School]}\n"
            + "Axis #2:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: 68,839\n"
            + "Row #0: 15,570\n"
            + "Row #0: 78,664\n"
            + "Row #0: 24,545\n"
            + "Row #0: 79,155\n");
    	TestUtil.withRole(foodMartContext, "Role2"); 
    	connection = foodMartContext.createConnection();
    	TestUtil.assertAxisThrows(
			connection,
            "[Customers].Members",
            "Mondrian Error:Failed to parse query 'select {[Customers].Members} on columns from Sales'");
    	TestUtil.withRole(foodMartContext, "Role3"); 
    	connection = foodMartContext.createConnection();
    	TestUtil.assertQueryThrows(
			connection,
            "select {[Education Level].Members} on columns, {[Measures].[Unit Sales]} on rows from Sales",
            "Mondrian Error:Failed to parse query 'select {[Education Level].Members} on columns, {[Measures].[Unit Sales]} on rows from Sales'");
    }

    // ~ Inner classes =========================================================

    public static class PeopleRole extends DelegatingRole {
        private final String repName;

        public PeopleRole(Role role, Schema schema, String repName) {
            super(((RoleImpl)role).makeMutableClone());
            this.repName = repName;
            defineGrantsForUser(schema);
        }

        private void defineGrantsForUser(Schema schema) {
            RoleImpl role = (RoleImpl)this.role;
            role.grant(schema, Access.NONE);

            Cube cube = schema.lookupCube("HR", true);
            role.grant(cube, Access.ALL);

            Hierarchy hierarchy = cube.lookupHierarchy(
                new Id.NameSegment("Employees"), false);

            Level[] levels = hierarchy.getLevels();
            Level topLevel = levels[1];

            role.grant(hierarchy, Access.CUSTOM, null, null, RollupPolicy.FULL);
            role.grant(hierarchy.getAllMember(), Access.NONE);

            boolean foundMember = false;

            List <Member> members =
                schema.getSchemaReader().withLocus()
                    .getLevelMembers(topLevel, true);

            for (Member member : members) {
                if (member.getUniqueName().contains("[" + repName + "]")) {
                    foundMember = true;
                    role.grant(member, Access.ALL);
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testMondrian1030(Context foodMartContext) throws Exception {
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
        String baseSchema = TestUtil.getRawSchema(foodMartContext);
        String schema = SchemaUtil.getSchema(baseSchema, 
                    null, null, null, null, null,
                    "  <Role name=\"Role1\">\n"
                    + "    <SchemaGrant access=\"all\">\n"
                    + "      <CubeGrant cube=\"Sales\" access=\"all\">\n"
                    + "        <HierarchyGrant hierarchy=\"[Customers]\" access=\"custom\" topLevel=\"[Customers].[City]\" bottomLevel=\"[Customers].[City]\" rollupPolicy=\"partial\">\n"
                    + "          <MemberGrant member=\"[City].[Coronado]\" access=\"all\">\n"
                    + "          </MemberGrant>\n"
                    + "        </HierarchyGrant>\n"
                    + "      </CubeGrant>\n"
                    + "    </SchemaGrant>\n"
                    + "  </Role>\n"
                    + "  <Role name=\"Role2\">\n"
                    + "    <SchemaGrant access=\"all\">\n"
                    + "      <CubeGrant cube=\"Sales\" access=\"all\">\n"
                    + "        <HierarchyGrant hierarchy=\"[Customers]\" access=\"custom\" topLevel=\"[Customers].[City]\" bottomLevel=\"[Customers].[City]\" rollupPolicy=\"partial\">\n"
                    + "          <MemberGrant member=\"[City].[Burbank]\" access=\"all\">\n"
                    + "          </MemberGrant>\n"
                    + "        </HierarchyGrant>\n"
                    + "      </CubeGrant>\n"
                    + "    </SchemaGrant>\n"
                    + "  </Role>\n");
        // Control tests
        TestUtil.withSchema(foodMartContext, schema);
        TestUtil.withRole(foodMartContext, "Role1"); 
        Connection connection = foodMartContext.createConnection();
        TestUtil.assertQueryReturns(
    		connection,
            mdx1,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Customers].[USA].[CA].[Coronado], [Education Level].[All Education Levels]}\n"
            + "{[Customers].[USA].[CA].[Coronado], [Education Level].[Bachelors Degree]}\n"
            + "{[Customers].[USA].[CA].[Coronado], [Education Level].[Graduate Degree]}\n"
            + "{[Customers].[USA].[CA].[Coronado], [Education Level].[High School Degree]}\n"
            + "{[Customers].[USA].[CA].[Coronado], [Education Level].[Partial College]}\n"
            + "{[Customers].[USA].[CA].[Coronado], [Education Level].[Partial High School]}\n"
            + "Row #0: 2,391\n"
            + "Row #1: 559\n"
            + "Row #2: 205\n"
            + "Row #3: 551\n"
            + "Row #4: 253\n"
            + "Row #5: 823\n");
        TestUtil.withRole(foodMartContext, "Role2"); 
        connection = foodMartContext.createConnection();
        TestUtil.assertQueryReturns(
    		connection,
            mdx1,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Customers].[USA].[CA].[Burbank], [Education Level].[All Education Levels]}\n"
            + "{[Customers].[USA].[CA].[Burbank], [Education Level].[Bachelors Degree]}\n"
            + "{[Customers].[USA].[CA].[Burbank], [Education Level].[Graduate Degree]}\n"
            + "{[Customers].[USA].[CA].[Burbank], [Education Level].[High School Degree]}\n"
            + "{[Customers].[USA].[CA].[Burbank], [Education Level].[Partial College]}\n"
            + "{[Customers].[USA].[CA].[Burbank], [Education Level].[Partial High School]}\n"
            + "Row #0: 3,086\n"
            + "Row #1: 914\n"
            + "Row #2: 126\n"
            + "Row #3: 1,029\n"
            + "Row #4: 286\n"
            + "Row #5: 731\n");
        TestUtil.withRole(foodMartContext, "Role1,Role2"); 
        connection = foodMartContext.createConnection();
        TestUtil.assertQueryReturns(
    		connection,
            mdx1,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Customers].[USA].[CA].[Burbank], [Education Level].[All Education Levels]}\n"
            + "{[Customers].[USA].[CA].[Burbank], [Education Level].[Bachelors Degree]}\n"
            + "{[Customers].[USA].[CA].[Burbank], [Education Level].[Graduate Degree]}\n"
            + "{[Customers].[USA].[CA].[Burbank], [Education Level].[High School Degree]}\n"
            + "{[Customers].[USA].[CA].[Burbank], [Education Level].[Partial College]}\n"
            + "{[Customers].[USA].[CA].[Burbank], [Education Level].[Partial High School]}\n"
            + "{[Customers].[USA].[CA].[Coronado], [Education Level].[All Education Levels]}\n"
            + "{[Customers].[USA].[CA].[Coronado], [Education Level].[Bachelors Degree]}\n"
            + "{[Customers].[USA].[CA].[Coronado], [Education Level].[Graduate Degree]}\n"
            + "{[Customers].[USA].[CA].[Coronado], [Education Level].[High School Degree]}\n"
            + "{[Customers].[USA].[CA].[Coronado], [Education Level].[Partial College]}\n"
            + "{[Customers].[USA].[CA].[Coronado], [Education Level].[Partial High School]}\n"
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
        TestUtil.withRole(foodMartContext, "Role1"); 
        connection = foodMartContext.createConnection();
        TestUtil.assertQueryReturns(
    		connection,
            mdx2,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Education Level].[All Education Levels]}\n"
            + "{[Education Level].[Bachelors Degree]}\n"
            + "{[Education Level].[Graduate Degree]}\n"
            + "{[Education Level].[High School Degree]}\n"
            + "{[Education Level].[Partial College]}\n"
            + "{[Education Level].[Partial High School]}\n"
            + "Row #0: 2,391\n"
            + "Row #1: 559\n"
            + "Row #2: 205\n"
            + "Row #3: 551\n"
            + "Row #4: 253\n"
            + "Row #5: 823\n");
        TestUtil.withRole(foodMartContext, "Role2"); 
        connection = foodMartContext.createConnection();
        TestUtil.assertQueryReturns(
    		connection,
            mdx2,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Education Level].[All Education Levels]}\n"
            + "{[Education Level].[Bachelors Degree]}\n"
            + "{[Education Level].[Graduate Degree]}\n"
            + "{[Education Level].[High School Degree]}\n"
            + "{[Education Level].[Partial College]}\n"
            + "{[Education Level].[Partial High School]}\n"
            + "Row #0: 3,086\n"
            + "Row #1: 914\n"
            + "Row #2: 126\n"
            + "Row #3: 1,029\n"
            + "Row #4: 286\n"
            + "Row #5: 731\n");
        TestUtil.withRole(foodMartContext, "Role1,Role2"); 
        connection = foodMartContext.createConnection();
        TestUtil.assertQueryReturns(
    		connection,
            mdx2,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Education Level].[All Education Levels]}\n"
            + "{[Education Level].[Bachelors Degree]}\n"
            + "{[Education Level].[Graduate Degree]}\n"
            + "{[Education Level].[High School Degree]}\n"
            + "{[Education Level].[Partial College]}\n"
            + "{[Education Level].[Partial High School]}\n"
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testBugMondrian1030_2(Context foodMartContext) {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
            null, null, null, null, null,
            "<Role name=\"Bacon\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
            + "      <HierarchyGrant hierarchy=\"[Customers]\" access=\"custom\" rollupPolicy=\"partial\">\n"
            + "        <MemberGrant member=\"[Customers].[USA].[CA].[Los Angeles]\" access=\"all\"/>\n"
            + "      </HierarchyGrant>\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>");
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "Bacon"); 
    	Connection connection = foodMartContext.createConnection();
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
                + "{[Customers].[USA]}\n"
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testMondrian1091(Context foodMartContext) throws Exception {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
            null, null, null, null, null,
            "<Role name=\"Role1\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
            + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"custom\" rollupPolicy=\"partial\">\n"
            + "        <MemberGrant member=\"[Store].[USA].[CA]\" access=\"all\"/>\n"
            + "      </HierarchyGrant>\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>");
    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "Role1"); 
    	Connection connection = foodMartContext.createConnection();
    	TestUtil.assertQueryReturns(
			connection,
            "select {[Store].Members} on columns from [Sales]",
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
            + "{[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[USA].[CA].[Los Angeles].[Store 7]}\n"
            + "{[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[USA].[CA].[San Diego].[Store 24]}\n"
            + "{[Store].[USA].[CA].[San Francisco]}\n"
            + "{[Store].[USA].[CA].[San Francisco].[Store 14]}\n"
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
        org.olap4j.metadata.Cube cube =
        		foodMartContext.createOlap4jConnection()
                .getOlapSchema().getCubes().get("Sales");
        org.olap4j.metadata.Member allMember =
            cube.lookupMember(
                IdentifierNode.parseIdentifier("[Store].[All Stores]")
                    .getSegmentList());
        assertNotNull(allMember);
        assertEquals(1, allMember.getHierarchy().getRootMembers().size());
        assertEquals(
            "[Store].[All Stores]",
            allMember.getHierarchy().getRootMembers().get(0).getUniqueName());
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testMondrian1259(Context foodMartContext) throws Exception {
        final String mdx =
            "select non empty {[Store].Members} on columns from [Sales]";
        String baseSchema = TestUtil.getRawSchema(foodMartContext);
        String schema = SchemaUtil.getSchema(baseSchema, 
            null, null, null, null, null,
            "<Role name=\"Role1\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
            + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"custom\" rollupPolicy=\"partial\">\n"
            + "        <MemberGrant member=\"[Store].[USA].[CA]\" access=\"all\"/>\n"
            + "      </HierarchyGrant>\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>"
            + "<Role name=\"Role2\">\n"
            + "  <SchemaGrant access=\"none\">\n"
            + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
            + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"custom\" rollupPolicy=\"partial\">\n"
            + "        <MemberGrant member=\"[Store].[USA].[OR]\" access=\"all\"/>\n"
            + "      </HierarchyGrant>\n"
            + "    </CubeGrant>\n"
            + "  </SchemaGrant>\n"
            + "</Role>");
        TestUtil.withSchema(foodMartContext, schema);
        TestUtil.withRole(foodMartContext, "Role1"); 
        Connection connection = foodMartContext.createConnection();
        TestUtil.assertQueryReturns(
    		connection,
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[All Stores]}\n"
            + "{[Store].[USA]}\n"
            + "{[Store].[USA].[CA]}\n"
            + "{[Store].[USA].[CA].[Beverly Hills]}\n"
            + "{[Store].[USA].[CA].[Beverly Hills].[Store 6]}\n"
            + "{[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[USA].[CA].[Los Angeles].[Store 7]}\n"
            + "{[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[USA].[CA].[San Diego].[Store 24]}\n"
            + "{[Store].[USA].[CA].[San Francisco]}\n"
            + "{[Store].[USA].[CA].[San Francisco].[Store 14]}\n"
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
        TestUtil.withRole(foodMartContext, "Role2"); 
        connection = foodMartContext.createConnection();
        TestUtil.assertQueryReturns(
    		connection,
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[All Stores]}\n"
            + "{[Store].[USA]}\n"
            + "{[Store].[USA].[OR]}\n"
            + "{[Store].[USA].[OR].[Portland]}\n"
            + "{[Store].[USA].[OR].[Portland].[Store 11]}\n"
            + "{[Store].[USA].[OR].[Salem]}\n"
            + "{[Store].[USA].[OR].[Salem].[Store 13]}\n"
            + "Row #0: 67,659\n"
            + "Row #0: 67,659\n"
            + "Row #0: 67,659\n"
            + "Row #0: 26,079\n"
            + "Row #0: 26,079\n"
            + "Row #0: 41,580\n"
            + "Row #0: 41,580\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testMondrian1295(Context foodMartContext) throws Exception {
        final String mdx =
            "With\n"
            + "Set [*NATIVE_CJ_SET] as 'NonEmptyCrossJoin([*BASE_MEMBERS_Time],[*BASE_MEMBERS_Product])'\n"
            + "Set [*SORTED_ROW_AXIS] as 'Order([*CJ_ROW_AXIS],Ancestor([Time].CurrentMember, [Time].[Year]).OrderKey,BASC,Ancestor([Time].CurrentMember, [Time].[Quarter]).OrderKey,BASC,[Time].CurrentMember.OrderKey,BASC,[Product].CurrentMember.OrderKey,BASC)'\n"
            + "Set [*BASE_MEMBERS_Product] as '{[Product].[All Products]}'\n"
            + "Set [*BASE_MEMBERS_Measures] as '{[Measures].[*FORMATTED_MEASURE_0]}'\n"
            + "Set [*CJ_ROW_AXIS] as 'Generate([*NATIVE_CJ_SET], {([Time].currentMember,[Product].currentMember)})'\n"
            + "Set [*BASE_MEMBERS_Time] as '[Time].[Year].Members'\n"
            + "Set [*CJ_COL_AXIS] as '[*NATIVE_CJ_SET]'\n"
            + "Member [Measures].[*FORMATTED_MEASURE_0] as '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=400\n"
            + "Select\n"
            + "[*BASE_MEMBERS_Measures] on columns,\n"
            + "Non Empty [*SORTED_ROW_AXIS] on rows\n"
            + "From [Sales]\n";

        String baseSchema = TestUtil.getRawSchema(foodMartContext);
        String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null,
                "<Role name=\"Admin\">\n"
                + "  <SchemaGrant access=\"none\">\n"
                + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
                + "      <HierarchyGrant hierarchy=\"[Store]\" rollupPolicy=\"partial\" access=\"custom\">\n"
                + "        <MemberGrant member=\"[Store].[USA].[CA]\" access=\"all\">\n"
                + "        </MemberGrant>\n"
                + "      </HierarchyGrant>\n"
                + "      <HierarchyGrant hierarchy=\"[Customers]\" rollupPolicy=\"partial\" access=\"custom\">\n"
                + "        <MemberGrant member=\"[Customers].[USA].[CA]\" access=\"all\">\n"
                + "        </MemberGrant>\n"
                + "      </HierarchyGrant>\n"
                + "    </CubeGrant>\n"
                + "  </SchemaGrant>\n"
                + "</Role> \n");
        TestUtil.withSchema(foodMartContext, schema);
        Connection connection = foodMartContext.createConnection();

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
        TestUtil.withRole(foodMartContext, "Admin"); 
        connection = foodMartContext.createConnection();
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
                + "{[Time].[1997], [Product].[All Products]}\n"
                + "Row #0: 74,748\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testMondrian936(Context foodMartContext) throws Exception {
    	String baseSchema = TestUtil.getRawSchema(foodMartContext);
    	String schema = SchemaUtil.getSchema(baseSchema, 
            null, null, null, null, null,
            "<Role name=\"test\">\n"
            + " <SchemaGrant access=\"none\">\n"
            + "   <CubeGrant cube=\"Sales\" access=\"all\">\n"
            + "     <HierarchyGrant hierarchy=\"[Store]\" access=\"custom\"\n"
            + "         topLevel=\"[Store].[Store Country]\" rollupPolicy=\"partial\">\n"
            + "       <MemberGrant member=\"[Store].[All Stores]\" access=\"none\"/>\n"
            + "       <MemberGrant member=\"[Store].[USA].[CA].[Los Angeles]\" access=\"all\"/>\n"
            + "       <MemberGrant member=\"[Store].[USA].[CA].[Alameda]\" access=\"all\"/>\n"
            + "       <MemberGrant member=\"[Store].[USA].[CA].[Beverly Hills]\"\n"
            + "access=\"all\"/>\n"
            + "       <MemberGrant member=\"[Store].[USA].[CA].[San Francisco]\"\n"
            + "access=\"all\"/>\n"
            + "       <MemberGrant member=\"[Store].[USA].[CA].[San Diego]\" access=\"all\"/>\n"
            + "\n"
            + "       <MemberGrant member=\"[Store].[USA].[OR].[Portland]\" access=\"all\"/>\n"
            + "       <MemberGrant member=\"[Store].[USA].[OR].[Salem]\" access=\"all\"/>\n"
            + "     </HierarchyGrant>\n"
            + "   </CubeGrant>\n"
            + " </SchemaGrant>\n"
            + "</Role>");

    	TestUtil.withSchema(foodMartContext, schema);
    	TestUtil.withRole(foodMartContext, "test"); 
    	Connection connection = foodMartContext.createConnection();
    	TestUtil.assertQueryReturns(
			connection,
            "select {[Measures].[Unit Sales]} on columns, "
            + "                 {[Product].[Food].[Baked Goods].[Bread]} on rows "
            + "                 from [Sales] "
            + " where { [Store].[USA].[OR], [Store].[USA].[CA]} ", "Axis #0:\n"
            + "{[Store].[USA].[OR]}\n"
            + "{[Store].[USA].[CA]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Product].[Food].[Baked Goods].[Bread]}\n"
            + "Row #0: 4,163\n");

        // changing ordering of members in the slicer should not change
        // result
    	TestUtil.assertQueryReturns(
			connection,
            "select {[Measures].[Unit Sales]} on columns, "
            + "                 {[Product].[Food].[Baked Goods].[Bread]} on rows "
            + "                 from [Sales] "
            + " where { [Store].[USA].[CA], [Store].[USA].[OR]} ", "Axis #0:\n"
            + "{[Store].[USA].[CA]}\n"
            + "{[Store].[USA].[OR]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Product].[Food].[Baked Goods].[Bread]}\n"
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testMondrian1434(Context foodMartContext) {
        String roleDef =
            "<Role name=\"dev\">"
            + "    <SchemaGrant access=\"all\">"
            + "      <CubeGrant cube=\"Sales\" access=\"all\">"
            + "      </CubeGrant>"
            + "      <CubeGrant cube=\"HR\" access=\"all\">"
            + "      </CubeGrant>"
            + "      <CubeGrant cube=\"Warehouse and Sales\" access=\"all\">"
            + "         <HierarchyGrant hierarchy=\"Measures\" access=\"custom\">"
            + "            <MemberGrant member=\"[Measures].[Warehouse Sales]\" access=\"all\">"
            + "            </MemberGrant>"
            + "         </HierarchyGrant>"
            + "     </CubeGrant>"
            + "  </SchemaGrant>"
            + "</Role>";
        String baseSchema = TestUtil.getRawSchema(foodMartContext);
        String schema = SchemaUtil.getSchema(baseSchema, null, null, null, null, null, roleDef);
        TestUtil.withSchema(foodMartContext, schema);
        TestUtil.withRole(foodMartContext, "dev"); 
        Connection connection = foodMartContext.createConnection();
        TestUtil.executeQuery(
    		connection,
            " select from [Sales] where {[Measures].[Unit Sales]}");

        roleDef =
            "<Role name=\"dev\">"
            + "    <SchemaGrant access=\"all\">"
            + "      <CubeGrant cube=\"Sales\" access=\"all\">"
            + "         <HierarchyGrant hierarchy=\"Measures\" access=\"custom\">"
            + "            <MemberGrant member=\"[Measures].[Unit Sales]\" access=\"all\">"
            + "            </MemberGrant>"
            + "         </HierarchyGrant>"
            + "      </CubeGrant>"
            + "      <CubeGrant cube=\"HR\" access=\"all\">"
            + "      </CubeGrant>"
            + "      <CubeGrant cube=\"Warehouse and Sales\" access=\"all\">"
            + "     </CubeGrant>"
            + "  </SchemaGrant>"
            + "</Role>";
        schema = SchemaUtil.getSchema(baseSchema, null, null, null, null, null, roleDef);
        TestUtil.withSchema(foodMartContext, schema);
        TestUtil.withRole(foodMartContext, "dev"); 
        connection = foodMartContext.createConnection();
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
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testMondrian1486(Context foodMartContext) throws Exception {
        final String mdx =
            "With\n"
            + "Set [*NATIVE_CJ_SET] as 'NonEmptyCrossJoin([*BASE_MEMBERS_Gender],[*BASE_MEMBERS_Marital Status])'\n"
            + "Set [*SORTED_ROW_AXIS] as 'Order([*CJ_ROW_AXIS],[Gender].CurrentMember.OrderKey,BASC,[Marital Status].CurrentMember.OrderKey,BASC)'\n"
            + "Set [*BASE_MEMBERS_Gender] as '[Gender].[Gender].Members'\n"
            + "Set [*BASE_MEMBERS_Measures] as '{[Measures].[*FORMATTED_MEASURE_0]}'\n"
            + "Set [*CJ_ROW_AXIS] as 'Generate([*NATIVE_CJ_SET], {([Gender].currentMember,[Marital Status].currentMember)})'\n"
            + "Set [*BASE_MEMBERS_Marital Status] as '[Marital Status].[Marital Status].Members'\n"
            + "Set [*CJ_COL_AXIS] as '[*NATIVE_CJ_SET]'\n"
            + "Member [Measures].[*FORMATTED_MEASURE_0] as '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=400\n"
            + "Select\n"
            + "[*BASE_MEMBERS_Measures] on columns,\n"
            + "Non Empty [*SORTED_ROW_AXIS] on rows\n"
            + "From [Sales]\n";
        String baseSchema = TestUtil.getRawSchema(foodMartContext);
        String schema = SchemaUtil.getSchema(baseSchema, 
                null, null, null, null, null,
                "<Role name=\"Admin\">\n"
                + "    <SchemaGrant access=\"none\">\n"
                + "      <CubeGrant cube=\"Sales\" access=\"all\">\n"
                + "        <HierarchyGrant hierarchy=\"[Gender]\" rollupPolicy=\"partial\" access=\"custom\">\n"
                + "          <MemberGrant member=\"[Gender].[F]\" access=\"all\">\n"
                + "          </MemberGrant>\n"
                + "        </HierarchyGrant>\n"
                + "      </CubeGrant>\n"
                + "    </SchemaGrant>\n"
                + "  </Role>\n");
        TestUtil.withSchema(foodMartContext, schema);
        TestUtil.withRole(foodMartContext, "Admin"); 
        Connection connection = foodMartContext.createConnection();
        TestUtil.assertQueryReturns(
    		connection,
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Gender].[F], [Marital Status].[M]}\n"
            + "{[Gender].[F], [Marital Status].[S]}\n"
            + "Row #0: 65,336\n"
            + "Row #1: 66,222\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testRollupPolicyWithNative(Context foodMartContext) {
        // Verifies limited role-restricted results using
        // all variations of rollup policy
        // Also verifies consistent results with a non-all default member.
        // connected with MONDRIAN-1568
        propSaver.set(propSaver.properties.EnableNativeCrossJoin, true);
        propSaver.set(propSaver.properties.EnableNativeFilter, true);
        propSaver.set(propSaver.properties.EnableNativeNonEmpty, true);
        propSaver.set(propSaver.properties.EnableNativeTopCount, true);
        propSaver.set(propSaver.properties.ExpandNonNative, true);

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

        String nonAllDefaultMem = "defaultMember=\"[Store2].[USA].[CA]\"";

        for (Role.RollupPolicy policy : Role.RollupPolicy.values()) {
            for (String defaultMember : new String[]{nonAllDefaultMem, "" }) {
                for (boolean hasAll : new Boolean[]{true, false}) {
                    // Results in this test should be the same regardless
                    // of rollupPolicy, default member, and whether there
                    // is an all member, since the rollup is not included
                    // in the test queries and context is explicitly set
                    // for [Store2].
                    // MONDRIAN-1568 showed different results with different
                    // rollup policies and different default members
                	String baseSchema = TestUtil.getRawSchema(foodMartContext);
                	String schema = SchemaUtil.getSchema(baseSchema, 
                        // swap in hasAll and defaultMember
                        String.format(dimension, hasAll, defaultMember),
                        cube, null, null, null,
                        // swap in policy
                        String.format(roleDefs, policy));
                    // RolapNativeCrossjoin
                	TestUtil.withSchema(foodMartContext, schema);
                	TestUtil.withRole(foodMartContext, "test"); 
                	Connection connection = foodMartContext.createConnection();
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
                        + "{[Store2].[USA].[CA], [Product].[Drink]}\n"
                        + "{[Store2].[USA].[CA], [Product].[Food]}\n"
                        + "{[Store2].[USA].[CA], [Product].[Non-Consumable]}\n"
                        + "{[Store2].[USA].[OR], [Product].[Drink]}\n"
                        + "{[Store2].[USA].[OR], [Product].[Food]}\n"
                        + "{[Store2].[USA].[OR], [Product].[Non-Consumable]}\n"
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
                        + "{[Store2].[USA].[CA]}\n"
                        + "{[Store2].[USA].[OR]}\n"
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
                        + "{[Store2].[USA].[CA]}\n"
                        + "{[Store2].[USA].[OR]}\n"
                        + "Row #0: 74,748\n"
                        + "Row #1: 67,659\n");
                }
            }
        }

        propSaver.reset();
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testValidMeasureWithRestrictedCubes(Context foodMartContext) {
        //http://jira.pentaho.com/browse/MONDRIAN-1616
        final String roleDefs =
            "<Role name=\"noBaseCubes\">\n"
            + " <SchemaGrant access=\"all\">\n"
            + "  <CubeGrant cube=\"Sales\" access=\"none\" />\n"
            + "  <CubeGrant cube=\"Sales Ragged\" access=\"none\" />\n"
            + "  <CubeGrant cube=\"Sales 2\" access=\"none\" />\n"
            + "  <CubeGrant cube=\"Warehouse\" access=\"none\" />\n"
            + " </SchemaGrant>\n"
            + "</Role> ";

        String baseSchema = TestUtil.getRawSchema(foodMartContext);
        String schema = SchemaUtil.getSchema(baseSchema, null, null, null, null, null, roleDefs);
        TestUtil.withSchema(foodMartContext, schema);
        TestUtil.withRole(foodMartContext, "noBaseCubes"); 
        Connection connection = foodMartContext.createConnection();

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
            + "{[Measures].[vm], [Gender].[F]}\n"
            + "Row #0: 89,043.253\n");
    }

}

// End AccessControlTest.java
