/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.rolap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertExprReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.executeQuery;
import static org.opencube.junit5.TestUtil.flushCache;
import static org.opencube.junit5.TestUtil.withSchema;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.daanse.olap.api.CacheControl;
import org.eclipse.daanse.olap.api.CacheControl.MemberEditCommand;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Locus;
import org.eclipse.daanse.olap.api.SchemaReader;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.query.component.AxisOrdinal;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Position;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.core.AbstractBasicContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;
import org.slf4j.Logger;

import mondrian.olap.IdImpl;
import mondrian.olap.Property;
import mondrian.olap.SystemWideProperties;
import mondrian.rolap.agg.AggregationManager;
import mondrian.server.ExecutionImpl;
import mondrian.server.LocusImpl;
import mondrian.test.DiffRepository;

/**
 * Unit tests for flushing member cache and editing cached member properties.
 *
 * <p>The purpose of the cache control API is to clear the cache so that
 * changes made to the DBMS can be seen. However, it is difficult to write
 * tests that modify the database. So these tests just check that the relevant
 * caches have been cleared. It is assumed that the updated values will be
 * loaded next time mondrian goes to the database.
 *
 * @author mberkowitz
 * @since Jan 2008
 */
class MemberCacheControlTest {
    private Locus locus;

    // TODO: add multi-thread tests.
    // TODO: test set properties negative: refer to invalid property
    // TODO: test set properties negative: set prop to invalid value
    // TODO: edit a different member not known to be in cache -- will it be
    //       fetched?

    @BeforeEach
    public void beforeEach() {


        SystemWideProperties.instance().EnableRolapCubeMemberCache = false;
//        RolapSchemaCache.instance().clear();
    }

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
//        RolapSchemaCache.instance().clear();
        LocusImpl.pop(locus);
        locus = null;
    }

    // ~ Utility methods ------------------------------------------------------
    DiffRepository getDiffRepos() {
        return DiffRepository.lookup(MemberCacheControlTest.class);
    }

    private void prepareTestContext(Context context) {
        final RolapConnection conn = (RolapConnection) context.getConnection();
        final Statement statement = conn.getInternalStatement();
        final ExecutionImpl execution = new ExecutionImpl(statement, Optional.empty());
        //locus = new Locus(execution, getName(), null);
        locus = new LocusImpl(execution, "MemberCacheControlTest", null);
        LocusImpl.push(locus);
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            // Reduced size Store dimension. Omits the 'Store Country' level,
            // and adds properties to non-leaf levels.
            "  <Dimension name=\"Retail\" foreignKey=\"store_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">\n"
            + "      <Table name=\"store\"/>\n"
            + "      <Level name=\"State\" column=\"store_state\" uniqueMembers=\"true\">\n"
            + "        <Property name=\"Country\" column=\"store_country\"/>\n"
            + "      </Level>\n"
            + "      <Level name=\"City\" column=\"store_city\" uniqueMembers=\"true\">\n"
            + "        <Property name=\"Population\" column=\"store_postal_code\"/>\n"
            + "      </Level>\n"
            + "      <Level name=\"Name\" column=\"store_name\" uniqueMembers=\"true\">\n"
            + "        <Property name=\"Store Type\" column=\"store_type\"/>\n"
            + "        <Property name=\"Store Manager\" column=\"store_manager\"/>\n"
            + "        <Property name=\"Store Sqft\" column=\"store_sqft\" type=\"Numeric\"/>\n"
            + "        <Property name=\"Has coffee bar\" column=\"coffee_bar\" type=\"Boolean\"/>\n"
            + "        <Property name=\"Street address\" column=\"store_street_address\" type=\"String\"/>\n"
            + "      </Level>\n"
            + "    </Hierarchy>\n"
            + "   </Dimension>"));
         */
        withSchema(context, SchemaModifiers.MemberCacheControlTestModifier::new);

    }

    /**
     * Creates a map.
     *
     * @param keys Keys
     * @param values Values
     * @return Map
     */
    private static <K, V> Map<K, V> createMap(List<K> keys, List<V> values) {
        assert keys.size() == values.size();
        final Map<K, V> map = new HashMap<>(keys.size());
        for (int i = 0; i < keys.size(); ++i) {
            map.put(keys.get(i), values.get(i));
        }
        return map;
    }

    /**
     * Finds a Member by its name and the name of its containing cube.
     *
     * @param connection connection
     * @param cubeName Cube name
     * @param names the full-qualified Member name
     * @return the Member
     * @throws OlapRuntimeException when not found.
     */
    protected static RolapMember findMember(
        Connection connection,
        String cubeName,
        String... names)
    {
        Cube cube = connection.getSchema().lookupCube(cubeName, true);
        SchemaReader scr = cube.getSchemaReader(null).withLocus();
        return (RolapMember)
            scr.getMemberByUniqueName(IdImpl.toList(names), true);
    }

    /**
     * Prints all properties of a Member.
     *
     * @param pw Print writer
     * @param member Member
     * @return the same print writer
     */
    private static PrintWriter printMemberProperties(
        PrintWriter pw,
        Member member)
    {
        pw.print(member.getUniqueName());
        pw.print(" {");
        int k = -1;
        for (Property p : member.getLevel().getProperties()) {
            if (++k > 0) {
                pw.print(",");
            }
            pw.println();
            String name = p.getName();
            Object value = member.getPropertyValue(name);

            // Fixup value for different database representations of boolean and
            // numeric values.
            if (value == null) {
                // no fixup needed
            } else if (name.equals("Has coffee bar")) {
                if (value instanceof Number) {
                    value = ((Number) value).intValue() != 0;
                }
            } else if (name.endsWith(" Sqft")) {
                Number number = (Number) value;
                value =
                    number.equals(number.intValue())
                        ? number.intValue()
                        : Math.round(number.floatValue());
            }
            pw.print("  [");
            pw.print(name);
            pw.print("]=[");
            pw.print(value);
            pw.print("]");
        }
        pw.println("}");
        return pw;
    }

    /**
     * Prints properties of all Members on an Axis.
     *
     * @param pw Print writer
     * @param axis Axis
     * @return the same print writer
     */
    private static PrintWriter printMemberProperties(
        PrintWriter pw,
        Axis axis)
    {
        for (Position pos : axis.getPositions()) {
            for (Member m : pos) {
                printMemberProperties(pw, m).println();
            }
        }
        return pw;
    }

    /**
     * Prints properties of the Row Axis from a Result.
     *
     * @param pw Print writer
     * @param result Result
     * @return the same print writer
     */
    private static PrintWriter printRowMemberProperties(
        PrintWriter pw,
        Result result)
    {
        return printMemberProperties(
            pw,
            result.getAxes()[
                AxisOrdinal.StandardAxisOrdinal.ROWS.logicalOrdinal()]);
    }

    private static String getRowMemberPropertiesAsString(Result r) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        printRowMemberProperties(pw, r);
        pw.flush();
        return sw.toString();
    }

    private CacheControl.MemberSet createInterestingMemberSet(
        Connection connection, CacheControl cc)
    {
        return cc.createUnionSet(
            // all stores in OR
            cc.createMemberSet(findMember(connection, "Sales", "Retail", "OR"), true),
            // all stores in Hidalgo, Zacatecas
            cc.createMemberSet(
                findMember(connection, "Sales", "Retail", "Zacatecas", "Hidalgo"),
                true),
            // a single store
            cc.createMemberSet(
                findMember(connection, "Sales", "Retail", "CA", "Alameda", "HQ"),
                false),
            // a range of stores
            cc.createMemberSet(
                true, findMember(connection, "Sales", "Retail", "WA", "Bremerton"),
                true, findMember(connection, "Sales", "Retail", "Yucatan", "Merida"),
                false),
            // all stores in a range of states
            cc.createMemberSet(
                true, findMember(connection, "Sales", "Retail", "DF"),
                true, findMember(connection, "Sales", "Retail", "Jalisco"),
                true));
    }

    // ~ Tests ----------------------------------------------------------------

    /**
     * Tests operations on member sets, in particular the
     * {@link CacheControl#filter} method.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFilter(Context context) {
    	context.getSchemaCache().clear();
        prepareTestContext(context);
        final Connection conn = context.getConnection();
        final DiffRepository dr = getDiffRepos();
        final CacheControl cc = conn.getCacheControl(null);

        CacheControl.MemberSet memberSet = createInterestingMemberSet(conn, cc);
        dr.assertEquals("before", "${before}", memberSet.toString());
        final Member orMember = findMember(conn, "Sales", "Retail", "OR");
        final CacheControl.MemberSet filteredMemberSet =
            cc.filter(orMember.getLevel(), memberSet);
        dr.assertEquals("after", "${after}", filteredMemberSet.toString());
    }

    /**
     * Tests that member operations fail if cache is enabled.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMemberOpsFailIfCacheEnabled(Context context) {
    	context.getSchemaCache().clear();
        SystemWideProperties.instance().EnableRolapCubeMemberCache = true;
        prepareTestContext(context);
        final Connection conn = context.getConnection();
        final CacheControl cc = conn.getCacheControl(null);
        final MemberEditCommand command =
            cc.createDeleteCommand(findMember(conn, "Sales", "Retail", "OR"));
        try {
            cc.execute(command);
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals(
                "Member cache control operations are not allowed unless "
                + "property mondrian.rolap.EnableRolapCubeMemberCache is "
                + "false",
                e.getMessage());
        }
    }

    /**
     * Test that edits the properties of a single leaf Member.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSetPropertyCommandOnLeafMember(Context context) {
    	context.getSchemaCache().clear();
    	prepareTestContext(context);
        final Connection conn = context.getConnection();
        final DiffRepository dr = getDiffRepos();
        final CacheControl cc = conn.getCacheControl(null);

        // A query that refers to a single leaf Member fetches the Member.
        // Changing Member properties does not affect Cell boundaries, so we
        // check that the MDX results are invariant.
        String mdx =
            "SELECT {[Measures].[Unit Sales]} ON COLUMNS,"
            + " {[Store].[USA].[CA].[San Francisco].[Store 14]}"
            + " ON ROWS FROM [Sales]";
        Query q = conn.parseQuery(mdx);
        Result r = conn.execute(q);
        dr.assertEquals(
            "props before",
            "${props before}",
            getRowMemberPropertiesAsString(r));
        final String resultString = TestUtil.toString(r);
        dr.assertEquals(
            "result before",
            "${result before}",
            resultString);

        // Change properties
        Member m =
            findMember(
                conn, "Sales", "Store", "USA", "CA", "San Francisco", "Store 14");
        cc.execute(cc.createSetPropertyCommand(m, "Store Manager", "Higgins"));
        cc.execute(
            cc.createCompoundCommand(
                Arrays.asList(
                    cc.createSetPropertyCommand(
                        m, "Street address", "770 Mission St"),
                    cc.createSetPropertyCommand(m, "Store Sqft", 6000),
                    cc.createSetPropertyCommand(
                        m, "Has coffee bar", "false"))));

        // Repeat same query; verify properties are changed.
        // Changing properties does not affect measures, so results unchanged.
        r = conn.execute(q);
        dr.assertEquals(
            "props after",
            "${props after}",
            getRowMemberPropertiesAsString(r));
        assertEquals(
            resultString,
            TestUtil.toString(r));
    }

    /**
     * Test that edits properties of Members at various Levels (use Retail
     * Dimension), but leaves grouping unchanged, so results not changed.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSetPropertyCommandOnNonLeafMember(Context context) {
    	context.getSchemaCache().clear();
    	prepareTestContext(context);
        final Connection conn = context.getConnection();
        final DiffRepository dr = getDiffRepos();
        final CacheControl cc = conn.getCacheControl(null);

        String mdx = "SELECT {[Measures].[Unit Sales]} ON COLUMNS,"
            + " {[Retail].Members} ON ROWS "
            + "FROM [Sales]";
        Query q = conn.parseQuery(mdx);
        Result r = conn.execute(q);
        dr.assertEquals(
            "props before",
            "${props before}",
            getRowMemberPropertiesAsString(r));
        final String resultString = TestUtil.toString(r);
        dr.assertEquals(
            "result before",
            "${result before}",
            resultString);

        // Change some properties (TODO: change dimension table first)
        // set 2 properties (TODO: set both with one command)

        // try all ways to construct MemberSets
        CacheControl.MemberSet memberSet = createInterestingMemberSet(conn, cc);

        final Map<String, Object> propertyValues =
            createMap(
                Arrays.asList("Has coffee bar", "Store Sqft"),
                Arrays.asList((Object) "true", 123));
        MemberEditCommand command;

        // first, the member set contains members of various levels
        try {
            command = cc.createSetPropertyCommand(memberSet, propertyValues);
            fail("expected exception, got " + command);
        } catch (IllegalArgumentException e) {
            assertEquals(
                "all members in set must belong to same level",
                e.getMessage());
        }

        // after we filter set to just members of store level we're ok
        final Member hqMember =
            findMember(conn, "Sales", "Retail", "CA", "Alameda", "HQ");
        final CacheControl.MemberSet filteredMemberSet =
            cc.filter(hqMember.getLevel(), memberSet);
        command =
            cc.createSetPropertyCommand(filteredMemberSet, propertyValues);
        cc.execute(command);

        // Repeat same query; verify properties were changed.
        // Changing properties does not affect measures, so results unchanged.
        r = conn.execute(q);
        dr.assertEquals(
            "props after",
            "${props after}",
            getRowMemberPropertiesAsString(r));
        assertEquals(
            resultString,
            TestUtil.toString(r));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAddCommand(Context context) {
    	context.getSchemaCache().clear();
        prepareTestContext(context);
        final Connection conn = context.getConnection();
        final CacheControl cc = conn.getCacheControl(null);
        final RolapCubeMember caCubeMember =
            (RolapCubeMember) findMember(conn, "Sales", "Retail", "CA");
        final RolapMember caMember = caCubeMember.member;
        final RolapMember rootMember = caMember.getParentMember();
        final RolapHierarchy hierarchy = caMember.getHierarchy();
        final RolapMember berkeleyMember =
            (RolapMember) hierarchy.createMember(
                caMember,
                caMember.getLevel().getChildLevel(),
                "Berkeley",
                null);
        final RolapBaseCubeMeasure unitSalesCubeMember =
            (RolapBaseCubeMeasure) findMember(
                conn, "Sales", "Measures", "Unit Sales");
        final RolapCubeMember yearCubeMember =
            (RolapCubeMember) findMember(
                conn, "Sales", "Time", "Year", "1997");
        final Member[] cacheRegionMembers =
            new Member[] {
                unitSalesCubeMember,
                caCubeMember,
                yearCubeMember
            };

        assertQueryReturns(conn,
            "select {[Retail].[City].Members} on columns from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Retail].[BC].[Vancouver]}\n"
            + "{[Retail].[BC].[Victoria]}\n"
            + "{[Retail].[CA].[Alameda]}\n"
            + "{[Retail].[CA].[Beverly Hills]}\n"
            + "{[Retail].[CA].[Los Angeles]}\n"
            + "{[Retail].[CA].[San Diego]}\n"
            + "{[Retail].[CA].[San Francisco]}\n"
            + "{[Retail].[DF].[Mexico City]}\n"
            + "{[Retail].[DF].[San Andres]}\n"
            + "{[Retail].[Guerrero].[Acapulco]}\n"
            + "{[Retail].[Jalisco].[Guadalajara]}\n"
            + "{[Retail].[OR].[Portland]}\n"
            + "{[Retail].[OR].[Salem]}\n"
            + "{[Retail].[Veracruz].[Orizaba]}\n"
            + "{[Retail].[WA].[Bellingham]}\n"
            + "{[Retail].[WA].[Bremerton]}\n"
            + "{[Retail].[WA].[Seattle]}\n"
            + "{[Retail].[WA].[Spokane]}\n"
            + "{[Retail].[WA].[Tacoma]}\n"
            + "{[Retail].[WA].[Walla Walla]}\n"
            + "{[Retail].[WA].[Yakima]}\n"
            + "{[Retail].[Yucatan].[Merida]}\n"
            + "{[Retail].[Zacatecas].[Camacho]}\n"
            + "{[Retail].[Zacatecas].[Hidalgo]}\n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: 21,333\n"
            + "Row #0: 25,663\n"
            + "Row #0: 25,635\n"
            + "Row #0: 2,117\n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: 26,079\n"
            + "Row #0: 41,580\n"
            + "Row #0: \n"
            + "Row #0: 2,237\n"
            + "Row #0: 24,576\n"
            + "Row #0: 25,011\n"
            + "Row #0: 23,591\n"
            + "Row #0: 35,257\n"
            + "Row #0: 2,203\n"
            + "Row #0: 11,491\n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n");
        assertAxisReturns(conn,
            "[Retail].[CA].Children",
            "[Retail].[CA].[Alameda]\n"
            + "[Retail].[CA].[Beverly Hills]\n"
            + "[Retail].[CA].[Los Angeles]\n"
            + "[Retail].[CA].[San Diego]\n"
            + "[Retail].[CA].[San Francisco]");
        final MemberReader memberReader = hierarchy.getMemberReader();
        final MemberCache memberCache =
            ((SmartMemberReader) memberReader).getMemberCache();
        List<RolapMember> caChildren =
            memberCache.getChildrenFromCache(caMember, null);
        assertEquals(5, caChildren.size());

        // Load cell data and check it is in cache
        executeQuery(conn,
            "select {[Measures].[Unit Sales]} on columns, {[Retail].[CA]} on rows from [Sales]");
        
        AbstractBasicContext abc = (AbstractBasicContext) conn.getContext();
        final AggregationManager aggMgr =
          abc.getAggregationManager();
        assertEquals(
            Double.valueOf("74748"),
            aggMgr.getCellFromAllCaches(
                AggregationManager.makeRequest(cacheRegionMembers), (RolapConnection)conn));

        // Now tell the cache that [CA].[Berkeley] is new
        final MemberEditCommand command =
            cc.createAddCommand(berkeleyMember);
        cc.execute(command);

        // test that cells have been removed
        assertNull(
            aggMgr.getCellFromAllCaches(
                AggregationManager.makeRequest(cacheRegionMembers), (RolapConnection)conn));

        assertAxisReturns(conn,
            "[Retail].[CA].Children",
            "[Retail].[CA].[Alameda]\n"
            + "[Retail].[CA].[Beverly Hills]\n"
            + "[Retail].[CA].[Los Angeles]\n"
            + "[Retail].[CA].[San Diego]\n"
            + "[Retail].[CA].[San Francisco]\n"
            + "[Retail].[CA].[Berkeley]");

        assertQueryReturns(conn,
            "select {[Retail].[City].Members} on columns from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Retail].[BC].[Vancouver]}\n"
            + "{[Retail].[BC].[Victoria]}\n"
            + "{[Retail].[CA].[Alameda]}\n"
            + "{[Retail].[CA].[Berkeley]}\n"
            + "{[Retail].[CA].[Beverly Hills]}\n"
            + "{[Retail].[CA].[Los Angeles]}\n"
            + "{[Retail].[CA].[San Diego]}\n"
            + "{[Retail].[CA].[San Francisco]}\n"
            + "{[Retail].[DF].[Mexico City]}\n"
            + "{[Retail].[DF].[San Andres]}\n"
            + "{[Retail].[Guerrero].[Acapulco]}\n"
            + "{[Retail].[Jalisco].[Guadalajara]}\n"
            + "{[Retail].[OR].[Portland]}\n"
            + "{[Retail].[OR].[Salem]}\n"
            + "{[Retail].[Veracruz].[Orizaba]}\n"
            + "{[Retail].[WA].[Bellingham]}\n"
            + "{[Retail].[WA].[Bremerton]}\n"
            + "{[Retail].[WA].[Seattle]}\n"
            + "{[Retail].[WA].[Spokane]}\n"
            + "{[Retail].[WA].[Tacoma]}\n"
            + "{[Retail].[WA].[Walla Walla]}\n"
            + "{[Retail].[WA].[Yakima]}\n"
            + "{[Retail].[Yucatan].[Merida]}\n"
            + "{[Retail].[Zacatecas].[Camacho]}\n"
            + "{[Retail].[Zacatecas].[Hidalgo]}\n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: 21,333\n"
            + "Row #0: 25,663\n"
            + "Row #0: 25,635\n"
            + "Row #0: 2,117\n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: 26,079\n"
            + "Row #0: 41,580\n"
            + "Row #0: \n"
            + "Row #0: 2,237\n"
            + "Row #0: 24,576\n"
            + "Row #0: 25,011\n"
            + "Row #0: 23,591\n"
            + "Row #0: 35,257\n"
            + "Row #0: 2,203\n"
            + "Row #0: 11,491\n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n");

        assertQueryReturns(conn,
            "select [Retail].Children on 0 from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Retail].[BC]}\n"
            + "{[Retail].[CA]}\n"
            + "{[Retail].[DF]}\n"
            + "{[Retail].[Guerrero]}\n"
            + "{[Retail].[Jalisco]}\n"
            + "{[Retail].[OR]}\n"
            + "{[Retail].[Veracruz]}\n"
            + "{[Retail].[WA]}\n"
            + "{[Retail].[Yucatan]}\n"
            + "{[Retail].[Zacatecas]}\n"
            + "Row #0: \n"
            + "Row #0: 74,748\n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: 67,659\n"
            + "Row #0: \n"
            + "Row #0: 124,366\n"
            + "Row #0: \n"
            + "Row #0: \n");

        List<RolapMember> rootChildren =
            memberCache.getChildrenFromCache(rootMember, null);
        if (rootChildren != null) { // might be null due to gc
            assertEquals(
                10, rootChildren.size());
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDeleteCommand(Context context) {
    	context.getSchemaCache().clear();
        prepareTestContext(context);
        final Connection conn = context.getConnection();
        final CacheControl cc = conn.getCacheControl(null);
        final RolapCubeMember sfCubeMember =
            (RolapCubeMember) findMember(
                conn, "Sales", "Retail", "CA", "San Francisco");
        final RolapMember caMember = sfCubeMember.member.getParentMember();
        final RolapHierarchy hierarchy = caMember.getHierarchy();
        final RolapBaseCubeMeasure unitSalesCubeMember =
            (RolapBaseCubeMeasure) findMember(
                conn, "Sales", "Measures", "Unit Sales");
        final RolapCubeMember yearCubeMember =
            (RolapCubeMember) findMember(
                conn, "Sales", "Time", "Year", "1997");
        final Member[] cacheRegionMembers =
            new Member[] {
                unitSalesCubeMember,
                sfCubeMember,
                yearCubeMember
            };

        assertAxisReturns(conn,
            "[Retail].[CA].Children",
            "[Retail].[CA].[Alameda]\n"
            + "[Retail].[CA].[Beverly Hills]\n"
            + "[Retail].[CA].[Los Angeles]\n"
            + "[Retail].[CA].[San Diego]\n"
            + "[Retail].[CA].[San Francisco]");

        final MemberReader memberReader = hierarchy.getMemberReader();
        final MemberCache memberCache =
            ((SmartMemberReader) memberReader).getMemberCache();
        List<RolapMember> caChildren =
            memberCache.getChildrenFromCache(caMember, null);
        assertEquals(5, caChildren.size());

        // Load cell data and check it is in cache
        executeQuery(conn,
            "select {[Measures].[Unit Sales]} on columns, {[Retail].[CA].[Alameda]} on rows from [Sales]");
        AbstractBasicContext abc = (AbstractBasicContext) conn.getContext();
        final AggregationManager aggMgr =
            abc.getAggregationManager();
        assertEquals(
            Double.valueOf("2117"),
            aggMgr.getCellFromAllCaches(
                AggregationManager.makeRequest(cacheRegionMembers), (RolapConnection)conn));

        // Now tell the cache that [CA].[San Francisco] has been removed.
        final MemberEditCommand command =
            cc.createDeleteCommand(sfCubeMember);
        cc.execute(command);

        // Children of CA should be 4
        assertEquals(
            4,
            memberCache.getChildrenFromCache(caMember, null).size());

        // test that cells have been removed
        assertNull(
            aggMgr.getCellFromAllCaches(
                AggregationManager.makeRequest(cacheRegionMembers), (RolapConnection)conn));

        // The list of children should be updated.
        assertAxisReturns(conn,
            "[Retail].[CA].Children",
            "[Retail].[CA].[Alameda]\n"
            + "[Retail].[CA].[Beverly Hills]\n"
            + "[Retail].[CA].[Los Angeles]\n"
            + "[Retail].[CA].[San Diego]");
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMoveCommand(Context context) {
    	context.getSchemaCache().clear();
        prepareTestContext(context);
        final Connection conn = context.getConnection();
        final CacheControl cc = conn.getCacheControl(null);
        final RolapCubeMember caCubeMember =
            (RolapCubeMember) findMember(conn, "Sales", "Retail", "CA");
        final RolapMember caMember = caCubeMember.member;
        final RolapHierarchy hierarchy = caMember.getHierarchy();
        final MemberReader memberReader = hierarchy.getMemberReader();
        final MemberCache memberCache =
            ((SmartMemberReader) memberReader).getMemberCache();
        final RolapMember alamedaMember =
            (RolapMember) hierarchy.createMember(
                caMember,
                caMember.getLevel().getChildLevel(),
                "Alameda",
                null);
        final RolapMember sfMember =
            (RolapMember) hierarchy.createMember(
                caMember,
                caMember.getLevel().getChildLevel(),
                "San Francisco",
                null);
        final RolapMember storeMember =
            (RolapMember) hierarchy.createMember(
                sfMember,
                sfMember.getLevel().getChildLevel(),
                "Store 14",
                null);

        // test axis contents
        assertAxisReturns(conn,
            "[Retail].[CA].Children",
            "[Retail].[CA].[Alameda]\n"
            + "[Retail].[CA].[Beverly Hills]\n"
            + "[Retail].[CA].[Los Angeles]\n"
            + "[Retail].[CA].[San Diego]\n"
            + "[Retail].[CA].[San Francisco]");
        assertAxisReturns(conn,
            "[Retail].[CA].[Alameda].Children",
            "[Retail].[CA].[Alameda].[HQ]");
        assertAxisReturns(conn,
            "[Retail].[CA].[San Francisco].Children",
            "[Retail].[CA].[San Francisco].[Store 14]");

        List<RolapMember> sfChildren =
            memberCache.getChildrenFromCache(sfMember, null);
        assertEquals(1, sfChildren.size());
        List<RolapMember> alamedaChildren =
            memberCache.getChildrenFromCache(alamedaMember, null);
        assertEquals(1, alamedaChildren.size());
        assertTrue(
            storeMember.getParentMember().equals(sfMember));

        // Now tell the cache that Store 14 moved to Alameda
        final MemberEditCommand command =
            cc.createMoveCommand(storeMember, alamedaMember);
        cc.execute(command);

        // The list of SF children should contain 0 elements
        assertEquals(
            0,
            memberCache.getChildrenFromCache(sfMember, null).size());

        // Check Alameda's children. It should be null as the parent's list
        // should be cleared.
        alamedaChildren =
            memberCache.getChildrenFromCache(alamedaMember, null);
        assertEquals(2, alamedaChildren.size());

        // test axis contents
        assertAxisReturns(conn,
            "[Retail].[CA].[San Francisco].Children",
            "");
        assertAxisReturns(conn,
            "[Retail].[CA].[Alameda].Children",
            "[Retail].[CA].[Alameda].[HQ]\n"
            + "[Retail].[CA].[Alameda].[Store 14]");

        // Test parent object
        assertTrue(
            storeMember.getParentMember().equals(alamedaMember));
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMoveFailBadLevel(Context context) {
    	context.getSchemaCache().clear();
        prepareTestContext(context);
        final Connection conn = context.getConnection();
        final CacheControl cc = conn.getCacheControl(null);
        final RolapCubeMember caCubeMember =
            (RolapCubeMember) findMember(conn, "Sales", "Retail", "CA");
        final RolapMember caMember = caCubeMember.member;
        final RolapHierarchy hierarchy = caMember.getHierarchy();
        final MemberReader memberReader = hierarchy.getMemberReader();
        final MemberCache memberCache =
            ((SmartMemberReader) memberReader).getMemberCache();
        final RolapMember sfMember =
            (RolapMember) hierarchy.createMember(
                caMember,
                caMember.getLevel().getChildLevel(),
                "San Francisco",
                null);
        final RolapMember storeMember =
            (RolapMember) hierarchy.createMember(
                sfMember,
                sfMember.getLevel().getChildLevel(),
                "Store 14",
                null);

        // test axis contents
        assertAxisReturns(conn,
            "[Retail].[CA].Children",
            "[Retail].[CA].[Alameda]\n"
            + "[Retail].[CA].[Beverly Hills]\n"
            + "[Retail].[CA].[Los Angeles]\n"
            + "[Retail].[CA].[San Diego]\n"
            + "[Retail].[CA].[San Francisco]");
        assertAxisReturns(conn,
            "[Retail].[CA].[San Francisco].Children",
            "[Retail].[CA].[San Francisco].[Store 14]");

        List<RolapMember> sfChildren =
            memberCache.getChildrenFromCache(sfMember, null);
        assertEquals(1, sfChildren.size());
        assertTrue(
            storeMember.getParentMember().equals(sfMember));

        // Now tell the cache that Store 14 moved to CA
        final MemberEditCommand command =
            cc.createMoveCommand(storeMember, caMember);
        try {
            cc.execute(command);
            fail("Should have failed due to improper level");
        } catch (OlapRuntimeException e) {
            assertEquals(
                "new parent belongs to different level than old",
                e.getCause().getMessage());
        }

        // The list of SF children should still contain 1 element
        assertEquals(
            1,
            memberCache.getChildrenFromCache(sfMember, null).size());

        // test axis contents. should not have been modified
        assertAxisReturns(conn,
            "[Retail].[CA].[San Francisco].Children",
            "[Retail].[CA].[San Francisco].[Store 14]");
        assertAxisReturns(conn,
            "[Retail].[CA].Children",
            "[Retail].[CA].[Alameda]\n"
            + "[Retail].[CA].[Beverly Hills]\n"
            + "[Retail].[CA].[Los Angeles]\n"
            + "[Retail].[CA].[San Diego]\n"
            + "[Retail].[CA].[San Francisco]");

        // Test parent object. should be the same
        assertTrue(
            storeMember.getParentMember().equals(sfMember));
    }

    /**
     * Tests a variety of negative cases including add/delete/move null members
     * add/delete/move members in parent-child hierarchies.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAddCommandNegative(Context context) {
    	context.getSchemaCache().clear();
        prepareTestContext(context);
        final Connection conn = context.getConnection();
        final CacheControl cc = conn.getCacheControl(null);

        MemberEditCommand command;
        try {
            command = cc.createAddCommand(null);
            fail("expected exception, got " + command);
        } catch (IllegalArgumentException e) {
            assertEquals("cannot add null member", e.getMessage());
        }

        final RolapCubeMember alamedaCubeMember =
            (RolapCubeMember) findMember(
                conn, "Sales", "Retail", "CA", "Alameda");
        final RolapMember alamedaMember = alamedaCubeMember.member;
        final RolapMember caMember = alamedaMember.getParentMember();

        final RolapCubeMember empCubeMember =
            (RolapCubeMember) findMember(
                conn, "HR", "Employees", "Sheri Nowmer", "Michael Spence");
        final RolapMember empMember = empCubeMember.member;

        try {
            command = cc.createMoveCommand(null, alamedaMember);
            fail("expected exception, got " + command);
        } catch (IllegalArgumentException e) {
            assertEquals("cannot move null member", e.getMessage());
        }

        try {
            command = cc.createMoveCommand(alamedaMember, null);
            fail("expected exception, got " + command);
        } catch (IllegalArgumentException e) {
            assertEquals("cannot move member to null location", e.getMessage());
        }

        try {
            command = cc.createDeleteCommand((Member) null);
            fail("expected exception, got " + command);
        } catch (IllegalArgumentException e) {
            assertEquals("cannot delete null member", e.getMessage());
        }

        try {
            command = cc.createSetPropertyCommand(null, "foo", 1);
            fail("expected exception, got " + command);
        } catch (IllegalArgumentException e) {
            assertEquals(
                "cannot set properties on null member",
                e.getMessage());
        }

        try {
            command = cc.createAddCommand(empMember);
            fail("expected exception, got " + command);
        } catch (IllegalArgumentException e) {
            assertEquals(
                "add member not supported for parent-child hierarchy",
                e.getMessage());
        }

        try {
            command = cc.createMoveCommand(empMember, null);
            fail("expected exception, got " + command);
        } catch (IllegalArgumentException e) {
            assertEquals(
                "move member not supported for parent-child hierarchy",
                e.getMessage());
        }

        try {
            command = cc.createDeleteCommand(empMember);
            fail("expected exception, got " + command);
        } catch (IllegalArgumentException e) {
            assertEquals(
                "delete member not supported for parent-child hierarchy",
                e.getMessage());
        }

        try {
            command = cc.createSetPropertyCommand(empMember, "foo", "bar");
            fail("expected exception, got " + command);
        } catch (IllegalArgumentException e) {
            assertEquals(
                "set properties not supported for parent-child hierarchy",
                e.getMessage());
        }

        try {
            command = cc.createSetPropertyCommand(
                cc.createUnionSet(
                    cc.createMemberSet(alamedaMember, false),
                    cc.createMemberSet(caMember, false)),
                Collections.<String, Object>emptyMap());
            fail("expected exception, got " + command);
        } catch (IllegalArgumentException e) {
            assertEquals(
                "all members in set must belong to same level",
                e.getMessage());
        }
    }

    /**
     * Test case for bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1076">MONDRIAN-1076,
     * "Add CacheControl API to flush members from dimension cache"</a>.
     */
    @Disabled //disabled for CI build
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFlushHierarchy(Context context) {
    	context.getSchemaCache().clear();
        prepareTestContext(context);
        flushCache(context.getConnection());
        final CacheControl cacheControl =
            context.getConnection().getCacheControl(null);
        final Cube salesCube =
                context.getConnection()
                .getSchema().lookupCube("Sales", true);

        final Logger logger = RolapUtil.SQL_LOGGER;
        final StringWriter sw = new StringWriter();
        //final Appender appender =
        //    Util.makeAppender("testMdcContext", sw, null);
        //Util.addAppender(appender, logger, org.apache.logging.log4j.Level.DEBUG);

        try {
            final Hierarchy storeHierarchy =
                salesCube.getDimensions()[1].getHierarchies()[0];
            assertEquals("Store", storeHierarchy.getName());
            final CacheControl.MemberSet storeMemberSet =
                cacheControl.createMemberSet(
                    storeHierarchy.getAllMember(), true);
            final Runnable storeFlusher =
                new Runnable() {
                    @Override
					public void run() {
                        cacheControl.flush(storeMemberSet);
                    }
                };

            final Result result =
                executeQuery(context.getConnection(),
                    "select [Store].[Mexico].[Yucatan] on 0 from [Sales]");
            final Member storeYucatanMember =
                result.getAxes()[0].getPositions().get(0).get(0);
            final CacheControl.MemberSet storeYucatanMemberSet =
                cacheControl.createMemberSet(
                    storeYucatanMember, true);
            final Runnable storeYucatanFlusher =
                new Runnable() {
                    @Override
					public void run() {
                        cacheControl.flush(storeYucatanMemberSet);
                    }
                };

            checkFlushHierarchy(
                sw, true, storeFlusher,
                new Runnable() {
                    @Override
					public void run() {
                        // Check that <Member>.Children uses cache when applied
                        // to an 'all' member.
                        assertAxisReturns(context.getConnection(),
                            "[Store].Children",
                            "[Store].[Canada]\n"
                            + "[Store].[Mexico]\n"
                            + "[Store].[USA]");
                    }
                });
            checkFlushHierarchy(
                sw, true, storeFlusher,
                new Runnable() {
                    @Override
					public void run() {
                        // Check that <Member>.Children uses cache when applied
                        // to regular member.
                        assertAxisReturns(context.getConnection(),
                            "[Store].[USA].[CA].Children",
                            "[Store].[USA].[CA].[Alameda]\n"
                            + "[Store].[USA].[CA].[Beverly Hills]\n"
                            + "[Store].[USA].[CA].[Los Angeles]\n"
                            + "[Store].[USA].[CA].[San Diego]\n"
                            + "[Store].[USA].[CA].[San Francisco]");
                    }
                });

            // In contrast to preceding, flushing Yucatan should not affect
            // California.
            checkFlushHierarchy(
                sw, false, storeYucatanFlusher,
                new Runnable() {
                    @Override
					public void run() {
                        // Check that <Member>.Children uses cache when applied
                        // to regular member.
                        assertAxisReturns(context.getConnection(),
                            "[Store].[USA].[CA].Children",
                            "[Store].[USA].[CA].[Alameda]\n"
                            + "[Store].[USA].[CA].[Beverly Hills]\n"
                            + "[Store].[USA].[CA].[Los Angeles]\n"
                            + "[Store].[USA].[CA].[San Diego]\n"
                            + "[Store].[USA].[CA].[San Francisco]");
                    }
                });

            checkFlushHierarchy(
                sw, true, storeFlusher, new Runnable() {
                    @Override
					public void run() {
                        // Check that <Hierarchy>.Members uses cache.
                        assertExprReturns(context.getConnection(),
                            "Count([Store].Members)", "63");
                    }
                });
            checkFlushHierarchy(
                sw, true, storeFlusher, new Runnable() {
                    @Override
					public void run() {
                        // Check that <Level>.Members uses cache.
                        assertExprReturns(context.getConnection(),
                            "Count([Store].[Store Name].Members)", "25");
                    }
                });


            // Time hierarchy is interesting because it has public 'all' member.
            // But you can still use the private all member for purposes like
            // flushing.
            final Hierarchy timeHierarchy =
                salesCube.getDimensions()[4].getHierarchies()[0];
            assertEquals("Time", timeHierarchy.getName());
            final CacheControl.MemberSet timeMemberSet =
                cacheControl.createMemberSet(
                    timeHierarchy.getAllMember(), true);
            final Runnable timeFlusher =
                new Runnable() {
                    @Override
					public void run() {
                        cacheControl.flush(timeMemberSet);
                    }
                };

            checkFlushHierarchy(
                sw, true, timeFlusher,
                new Runnable() {
                    @Override
					public void run() {
                        // Check that <Level>.Members uses cache.
                        assertExprReturns(context.getConnection(),
                            "Count([Time].[Month].Members)",
                            "24");
                    }
                });
            checkFlushHierarchy(
                sw, true, timeFlusher,
                new Runnable() {
                    @Override
					public void run() {
                        // Check that <Level>.Members uses cache.
                        assertAxisReturns(context.getConnection(),
                            "[Time].[1997].[Q2].Children",
                            "[Time].[1997].[Q2].[4]\n"
                            + "[Time].[1997].[Q2].[5]\n"
                            + "[Time].[1997].[Q2].[6]");
                    }
                });
        } finally {
            //Util.removeAppender(appender, logger);
        }
    }

    /**
     * Runs the same command ({@code foo(testContext, k)}) three times. Between
     * the 2nd and the 3rd, flushes the cache, and makes sure that the 3rd time
     * causes SQL to be executed.
     *
     * @param writer Writer, written into each time a SQL statement is executed
     * @param affected Whether the cache flush affects the command
     * @param flusher Functor that performs cache flushing action to be tested
     * @param command Command to execute that requires cache contents
     */
    private void checkFlushHierarchy(
        StringWriter writer,
        boolean affected,
        Runnable flusher,
        Runnable command)
    {
        // Run command for first time.
        command.run();

        // Now cache is primed, running the command for second time should not
        // require any additional SQL. (There is a small chance that GC will
        // kick in and we'll lose the cache, but we've never seen that happen
        // in the wild.)
        int length1 = writer.getBuffer().length();
        command.run();
        final String since1 = writer.getBuffer().substring(length1);
        assertEquals("", since1);
        flusher.run();

        // Now cache has been flushed, it should be impossible to execute the
        // command without running additional SQL.
        int length2 = writer.getBuffer().length();
        command.run();
        final String since2 = writer.getBuffer().substring(length2);
        if (affected) {
            assertNotSame("", since2);
        }
    }
}
