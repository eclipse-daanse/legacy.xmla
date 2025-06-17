/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.test;

import static mondrian.olap.SolveOrderMode.ABSOLUTE;
import static mondrian.olap.SolveOrderMode.SCOPED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.withSchema;

import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.SolveOrderMode;
import mondrian.olap.Util;
import mondrian.rolap.SchemaModifiers;

/**
 * <code>SolveOrderScopeIsolationTest</code> Test conformance to SSAS2005 solve
 * order scope isolation behavior.
 * Scope Isolation: In SQL Server 2005 Analysis Services, when a cube
 * Multidimensional Expressions (MDX) script contains calculated members,
 * by default the calculated members are resolved before any session-scoped
 * calculations are resolved and before any query-defined calculations are
 * resolved. This is different from SQL Server 2000 Analysis Services behavior,
 * where solve order can explicitly be used to insert a session-scoped or
 * query-defined calculation in between two cube-level calculations.
 * Further details at: http://msdn2.microsoft.com/en-us/library/ms144787.aspx
 *
 * This initial set of tests are added to indicate the kind of behavior that is
 * expected to support this SSAS 2005 feature. All tests start with an
 * underscore so as to not to execute even if the test class is added to Main
 *
 * @author ajogleka
 * @since Apr 04, 2008
 */
class SolveOrderScopeIsolationTest {
    //SolveOrderMode defaultSolveOrderMode;

    //


    //public void beforeEach(Context<?> context) {
    //    //propSaver = new PropertySaver5();
    //    defaultSolveOrderMode = getSolveOrderMode(context);
    //}

    //public void afterEach(Context<?> context) {
    //    //SystemWideProperties.instance().populateInitial();
    //    setSolveOrderMode(context, defaultSolveOrderMode);
    //}


    private static final String memberDefs =
        "<CalculatedMember\n"
        + "    name=\"maleMinusFemale\"\n"
        + "    dimension=\"gender\"\n"
        + "    visible=\"false\"\n"
        + "    formula=\"gender.m - gender.f\">\n"
        + "  <CalculatedMemberProperty name=\"SOLVE_ORDER\" value=\"3000\"/>\n"
        + "</CalculatedMember>"
        + "<CalculatedMember\n"
        + "    name=\"ProfitSolveOrder3000\"\n"
        + "    dimension=\"Measures\">\n"
        + "  <Formula>[Measures].[Store Sales] - [Measures].[Store Cost]</Formula>\n"
        + "  <CalculatedMemberProperty name=\"SOLVE_ORDER\" value=\"3000\"/>\n"
        + "  <CalculatedMemberProperty name=\"FORMAT_STRING\" value=\"$#,##0.000000\"/>\n"
        + "</CalculatedMember>"
        + "<CalculatedMember\n"
        + "    name=\"ratio\"\n"
        + "    dimension=\"measures\"\n"
        + "    visible=\"false\"\n"
        + "    formula=\"measures.[unit sales] / measures.[sales count]\">\n"
        + "  <CalculatedMemberProperty name=\"FORMAT_STRING\" value=\"0.0#\"/>\n"
        + "  <CalculatedMemberProperty name=\"SOLVE_ORDER\" value=\"10\"/>\n"
        + "</CalculatedMember>"
        + "<CalculatedMember\n"
        + "    name=\"Total\"\n"
        + "    hierarchy=\"[Time].[Time]\"\n"
        + "    visible=\"false\"\n"
        + "    formula=\"AGGREGATE({[Time].[1997].[Q1],[Time].[1997].[Q2]})\">\n"
        + "  <CalculatedMemberProperty name=\"SOLVE_ORDER\" value=\"20\"/>\n"
        + "</CalculatedMember>";

    private SolveOrderMode getSolveOrderMode(Context<?> context)
    {
        return Util.lookup(
            SolveOrderMode.class,
            context.getConfigValue(ConfigConstants.SOLVE_ORDER_MODE, ConfigConstants.SOLVE_ORDER_MODE_DEFAULT_VALUE, String.class).toUpperCase());
    }

    final void setSolveOrderMode(Context<?> context, SolveOrderMode mode) {
        ((TestContextImpl)context).setSolveOrderMode(mode.toString());
    }

    public void prepareContext(Context<?> context) {
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales", null, memberDefs));

         */
        withSchema(context, SchemaModifiers.SolveOrderScopeIsolationTestModifier::new);

    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAllSolveOrderModesHandled(Context<?> context)
    {
        for (SolveOrderMode mode : SolveOrderMode.values()) {
            switch (mode) {
            case ABSOLUTE:
            case SCOPED:
                break;
            default:
                fail(
                    "Tests for solve order mode " + mode.toString()
                    + " have not been implemented.");
            }
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSetSolveOrderMode(Context<?> context)
    {
        setSolveOrderMode(context, ABSOLUTE);
        assertEquals(ABSOLUTE, getSolveOrderMode(context));

        setSolveOrderMode(context, SCOPED);
        assertEquals(SCOPED, getSolveOrderMode(context));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOverrideCubeMemberDoesNotHappenAbsolute(Context<?> context) {
    	prepareContext(context);
        final String mdx =
            "with\n"
            + "member gender.override as 'gender.maleMinusFemale', "
            + "SOLVE_ORDER=5\n"
            + "select {measures.[ratio], measures.[unit sales], "
            + "measures.[sales count]} on 0,\n"
            + "{gender.override,gender.maleMinusFemale} on 1\n"
            + "from sales";

        setSolveOrderMode(context, SolveOrderMode.ABSOLUTE);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[ratio]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "Axis #2:\n"
            + "{[Gender].[Gender].[override]}\n"
            + "{[Gender].[Gender].[maleMinusFemale]}\n"
            + "Row #0: 3.11\n"
            + "Row #0: 3,657\n"
            + "Row #0: 1,175\n"
            + "Row #1: 0.0\n"
            + "Row #1: 3,657\n"
            + "Row #1: 1,175\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOverrideCubeMemberDoesNotHappenScoped(Context<?> context) {
    	prepareContext(context);
        final String mdx =
            "with\n"
            + "member gender.override as 'gender.maleMinusFemale', "
            + "SOLVE_ORDER=5\n"
            + "select {measures.[ratio], measures.[unit sales], "
            + "measures.[sales count]} on 0,\n"
            + "{gender.override,gender.maleMinusFemale} on 1\n"
            + "from sales";

        setSolveOrderMode(context, SolveOrderMode.SCOPED);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[ratio]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "Axis #2:\n"
            + "{[Gender].[Gender].[override]}\n"
            + "{[Gender].[Gender].[maleMinusFemale]}\n"
            + "Row #0: 0.0\n"
            + "Row #0: 3,657\n"
            + "Row #0: 1,175\n"
            + "Row #1: 0.0\n"
            + "Row #1: 3,657\n"
            + "Row #1: 1,175\n");
    }

    /**
     * Test for future capability: SCOPE_ISOLATION=CUBE which is implemented in
     * Analysis Services but not yet in Mondrian.
     */
    @Disabled
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    public void _future_testOverrideCubeMemberHappensWithScopeIsolation(Context<?> context) {
    	prepareContext(context);
        setSolveOrderMode(context, SCOPED);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with\n"
            + "member gender.maleMinusFemale as 'Gender.M - gender.f', "
            + "SOLVE_ORDER=3000, FORMAT_STRING='#.##'\n"
            + "member gender.override as 'gender.maleMinusFemale', "
            + "SOLVE_ORDER=5, FORMAT_STRING='#.##', SCOPE_ISOLATION=CUBE \n"
            + "member measures.[ratio] as "
            + "'measures.[unit sales] / measures.[sales count]', SOLVE_ORDER=10\n"
            + "select {measures.[ratio],\n"
            + "measures.[unit sales],\n"
            + "measures.[sales count]} on 0,\n"
            + "{gender.override, gender.maleMinusFemale} on 1\n"
            + "from sales",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[ratio]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "Axis #2:\n"
            + "{[Gender].[override]}\n"
            + "{[Gender].[maleMinusFemale]}\n"
            + "Row #0: 3.11"
            + "Row #0: 3657\n"
            + "Row #0: 1175\n"
            + "Row #1: \n"
            + "Row #1: 3657\n"
            + "Row #1: 1175\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCubeMemberEvalBeforeQueryMemberAbsolute(Context<?> context) {
    	prepareContext(context);
        final String mdx =
            "WITH MEMBER [Customers].USAByWA AS\n"
            + "'[Customers].[Country].[USA] / [Customers].[State Province].[WA]', "
            + "SOLVE_ORDER=5\n"
            + "SELECT {[Country].[USA],[State Province].[WA], [Customers].USAByWA} ON 0, "
            + " {[Measures].[Store Sales], [Measures].[Store Cost], [Measures].[ProfitSolveOrder3000]} ON 1 "
            + "FROM SALES\n";
        setSolveOrderMode(context, ABSOLUTE);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Customers].[Customers].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA]}\n"
            + "{[Customers].[Customers].[USAByWA]}\n"
            + "Axis #2:\n"
            + "{[Measures].[Store Sales]}\n"
            + "{[Measures].[Store Cost]}\n"
            + "{[Measures].[ProfitSolveOrder3000]}\n"
            + "Row #0: 565,238.13\n"
            + "Row #0: 263,793.22\n"
            + "Row #0: 2.14\n"
            + "Row #1: 225,627.23\n"
            + "Row #1: 105,324.31\n"
            + "Row #1: 2.14\n"
            + "Row #2: $339,610.896400\n"
            + "Row #2: $158,468.912100\n"
            + "Row #2: $0.000518\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCubeMemberEvalBeforeQueryMemberScoped(Context<?> context) {
    	prepareContext(context);
        final String mdx =
            "WITH MEMBER [Customers].USAByWA AS\n"
            + "'[Customers].[Country].[USA] / [Customers].[State Province].[WA]', "
            + "SOLVE_ORDER=5\n"
            + "SELECT {[Country].[USA],[State Province].[WA], [Customers].USAByWA} ON 0 "
            + "FROM SALES\n"
            + "WHERE ProfitSolveOrder3000";
        setSolveOrderMode(context, SCOPED);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{[Measures].[ProfitSolveOrder3000]}\n"
            + "Axis #1:\n"
            + "{[Customers].[Customers].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA]}\n"
            + "{[Customers].[Customers].[USAByWA]}\n"
            + "Row #0: $339,610.896400\n"
            + "Row #0: $158,468.912100\n"
            + "Row #0: $2.143076\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOverrideCubeMemberInTupleDoesNotHappenAbsolute(Context<?> context) {
    	prepareContext(context);
        final String mdx =
            "with\n"
            + "member gender.override as "
            + "'([Gender].[maleMinusFemale], [Product].[Food])', SOLVE_ORDER=5\n"
            + "select {measures.[ratio],\n"
            + "measures.[unit sales],\n"
            + "measures.[sales count]} on 0,\n"
            + "{gender.override, gender.maleMinusFemale} on 1\n"
            + "from sales";
        setSolveOrderMode(context, ABSOLUTE);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[ratio]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "Axis #2:\n"
            + "{[Gender].[Gender].[override]}\n"
            + "{[Gender].[Gender].[maleMinusFemale]}\n"
            + "Row #0: 3.09\n"
            + "Row #0: 2,312\n"
            + "Row #0: 749\n"
            + "Row #1: 0.0\n"
            + "Row #1: 3,657\n"
            + "Row #1: 1,175\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOverrideCubeMemberInTupleDoesNotHappenScoped(Context<?> context) {
    	prepareContext(context);
        final String mdx =
            "with\n"
            + "member gender.override as "
            + "'([Gender].[maleMinusFemale], [Product].[Food])', SOLVE_ORDER=5\n"
            + "select {measures.[ratio],\n"
            + "measures.[unit sales],\n"
            + "measures.[sales count]} on 0,\n"
            + "{gender.override, gender.maleMinusFemale} on 1\n"
            + "from sales";
        setSolveOrderMode(context, SCOPED);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[ratio]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "Axis #2:\n"
            + "{[Gender].[Gender].[override]}\n"
            + "{[Gender].[Gender].[maleMinusFemale]}\n"
            + "Row #0: 0.0\n"
            + "Row #0: 2,312\n"
            + "Row #0: 749\n"
            + "Row #1: 0.0\n"
            + "Row #1: 3,657\n"
            + "Row #1: 1,175\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testConditionalCubeMemberEvalBeforeOtherMembersAbsolute(Context<?> context) {
    	prepareContext(context);
        final String mdx =
            "with\n"
            + "member gender.override as 'iif(1=0,"
            + "[gender].[all gender].[m], [Gender].[maleMinusFemale])', "
            + "SOLVE_ORDER=5\n"
            + "select {measures.[ratio],\n"
            + "measures.[unit sales],\n"
            + "measures.[sales count]} on 0,\n"
            + "{[Gender].[override], gender.maleMinusFemale} on 1\n"
            + "from sales";
        setSolveOrderMode(context, ABSOLUTE);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[ratio]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "Axis #2:\n"
            + "{[Gender].[Gender].[override]}\n"
            + "{[Gender].[Gender].[maleMinusFemale]}\n"
            + "Row #0: 3.11\n"
            + "Row #0: 3,657\n"
            + "Row #0: 1,175\n"
            + "Row #1: 0.0\n"
            + "Row #1: 3,657\n"
            + "Row #1: 1,175\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testConditionalCubeMemberEvalBeforeOtherMembersScoped(Context<?> context) {
    	prepareContext(context);
        final String mdx =
            "with\n"
            + "member gender.override as 'iif(1=0,"
            + "[gender].[all gender].[m], [Gender].[maleMinusFemale])', "
            + "SOLVE_ORDER=5\n"
            + "select {measures.[ratio],\n"
            + "measures.[unit sales],\n"
            + "measures.[sales count]} on 0,\n"
            + "{[Gender].[override], gender.maleMinusFemale} on 1\n"
            + "from sales";
        setSolveOrderMode(context, SCOPED);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[ratio]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "Axis #2:\n"
            + "{[Gender].[Gender].[override]}\n"
            + "{[Gender].[Gender].[maleMinusFemale]}\n"
            + "Row #0: 0.0\n"
            + "Row #0: 3,657\n"
            + "Row #0: 1,175\n"
            + "Row #1: 0.0\n"
            + "Row #1: 3,657\n"
            + "Row #1: 1,175\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOverrideCubeMemberUsingStrToMemberDoesNotHappenAbsolute(Context<?> context) {
        prepareContext(context);
        final String mdx =
            "with\n"
            + "member gender.override as 'iif(1=0,[gender].[all gender].[m], "
            + "StrToMember(\"[Gender].[maleMinusFemale]\"))', "
            + "SOLVE_ORDER=5\n"
            + "select {measures.[ratio],\n"
            + "measures.[unit sales],\n"
            + "measures.[sales count]} on 0,\n"
            + "{[Gender].[override], gender.maleMinusFemale} on 1\n"
            + "from sales";
        setSolveOrderMode(context, ABSOLUTE);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[ratio]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "Axis #2:\n"
            + "{[Gender].[Gender].[override]}\n"
            + "{[Gender].[Gender].[maleMinusFemale]}\n"
            + "Row #0: 3.11\n"
            + "Row #0: 3,657\n"
            + "Row #0: 1,175\n"
            + "Row #1: 0.0\n"
            + "Row #1: 3,657\n"
            + "Row #1: 1,175\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOverrideCubeMemberUsingStrToMemberDoesNotHappenScoped(Context<?> context) {
    	prepareContext(context);
        final String mdx =
            "with\n"
            + "member gender.override as 'iif(1=0,[gender].[all gender].[m], "
            + "StrToMember(\"[Gender].[maleMinusFemale]\"))', "
            + "SOLVE_ORDER=5\n"
            + "select {measures.[ratio],\n"
            + "measures.[unit sales],\n"
            + "measures.[sales count]} on 0,\n"
            + "{[Gender].[override], gender.maleMinusFemale} on 1\n"
            + "from sales";
        setSolveOrderMode(context, SCOPED);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[ratio]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "Axis #2:\n"
            + "{[Gender].[Gender].[override]}\n"
            + "{[Gender].[Gender].[maleMinusFemale]}\n"
            + "Row #0: 0.0\n"
            + "Row #0: 3,657\n"
            + "Row #0: 1,175\n"
            + "Row #1: 0.0\n"
            + "Row #1: 3,657\n"
            + "Row #1: 1,175\n");
    }

    /**
     * This test validates that behavior is consistent with Analysis Services
     * 2000 when Solve order scope is ABSOLUTE.  AS2K will throw an error
     * whenever attempting to aggregate over calculated members (i.e. when the
     * solve order of the Aggregate member is higher than the calculations it
     * intersects with).
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAggregateMemberEvalAfterOtherMembersAbsolute(Context<?> context) {
    	prepareContext(context);
        final String mdx =
            "With\n"
            + "member Time.Time.Total1 as "
            + "'AGGREGATE({[Time].[1997].[Q1],[Time].[1997].[Q2]})' , SOLVE_ORDER=20 \n"
            + ", FORMAT_STRING='#,###'\n"
            + "member measures.[ratio1] as "
            + "'measures.[unit sales] / measures.[sales count]', "
            + "SOLVE_ORDER=10 , FORMAT_STRING='#.##'\n"
            + "select {measures.[ratio1],\n"
            + "measures.[unit sales],\n"
            + "measures.[sales count]} on 0,\n"
            + "{Time.Total, Time.Total1, [Time].[1997].[Q1], [Time].[1997].[Q2]} on 1\n"
            + "from sales";
        setSolveOrderMode(context, ABSOLUTE);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[ratio1]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "Axis #2:\n"
            + "{[Time].[Time].[Total]}\n"
            + "{[Time].[Time].[Total1]}\n"
            + "{[Time].[Time].[1997].[Q1]}\n"
            + "{[Time].[Time].[1997].[Q2]}\n"
            + "Row #0: #ERR: mondrian.olap.fun.MondrianEvaluationException: Could not find an aggregator in the current evaluation context\n"
            + "Row #0: 128,901\n"
            + "Row #0: 41,956\n"
            + "Row #1: #ERR: mondrian.olap.fun.MondrianEvaluationException: Could not find an aggregator in the current evaluation context\n"
            + "Row #1: 128,901\n"
            + "Row #1: 41,956\n"
            + "Row #2: 3.07\n"
            + "Row #2: 66,291\n"
            + "Row #2: 21,588\n"
            + "Row #3: 3.07\n"
            + "Row #3: 62,610\n"
            + "Row #3: 20,368\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAggregateMemberEvalAfterOtherMembersScoped(Context<?> context) {
    	prepareContext(context);
        final String mdx =
            "With\n"
            + "member Time.Time.Total1 as "
            + "'AGGREGATE({[Time].[1997].[Q1],[Time].[1997].[Q2]})' , SOLVE_ORDER=20 \n"
            + ", FORMAT_STRING='#,###'\n"
            + "member measures.[ratio1] as "
            + "'measures.[unit sales] / measures.[sales count]', "
            + "SOLVE_ORDER=10 , FORMAT_STRING='#.##'\n"
            + "select {measures.[ratio1],\n"
            + "measures.[unit sales],\n"
            + "measures.[sales count]} on 0,\n"
            + "{Time.Total, Time.Total1, [Time].[1997].[Q1], [Time].[1997].[Q2]} on 1\n"
            + "from sales";
        setSolveOrderMode(context, SCOPED);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[ratio1]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "Axis #2:\n"
            + "{[Time].[Time].[Total]}\n"
            + "{[Time].[Time].[Total1]}\n"
            + "{[Time].[Time].[1997].[Q1]}\n"
            + "{[Time].[Time].[1997].[Q2]}\n"
            + "Row #0: 3.07\n"
            + "Row #0: 128,901\n"
            + "Row #0: 41,956\n"
            + "Row #1: 3\n"
            + "Row #1: 128,901\n"
            + "Row #1: 41,956\n"
            + "Row #2: 3.07\n"
            + "Row #2: 66,291\n"
            + "Row #2: 21,588\n"
            + "Row #3: 3.07\n"
            + "Row #3: 62,610\n"
            + "Row #3: 20,368\n");
    }

    /**
     * This test validates that behavior is consistent with Analysis Services
     * 2000 when Solve order scope is ABSOLUTE.  AS2K will throw an error
     * whenever attempting to aggregate over calculated members (i.e. when the
     * solve order of the Aggregate member is higher than the calculations it
     * intersects with).
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testConditionalAggregateMemberEvalAfterOtherMembersAbsolute(Context<?> context) {
    	prepareContext(context);
        final String mdx =
            "With\n"
            + "member Time.Time.Total1 as 'IIF(Measures.CURRENTMEMBER IS Measures.Profit, 1, "
            + "AGGREGATE({[Time].[1997].[Q1],[Time].[1997].[Q2]}))' , SOLVE_ORDER=20 \n"
            + "\n"
            + "member measures.[ratio1] as 'measures.[unit sales] / measures.[sales count]', "
            + "SOLVE_ORDER=10\n"
            + "select {measures.[ratio1],\n"
            + "measures.[unit sales],\n"
            + "measures.[sales count]} on 0,\n"
            + "{Time.Total, Time.Total1, [Time].[1997].[Q1], [Time].[1997].[Q2]} on 1\n"
            + "from sales";
        setSolveOrderMode(context, ABSOLUTE);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[ratio1]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "Axis #2:\n"
            + "{[Time].[Time].[Total]}\n"
            + "{[Time].[Time].[Total1]}\n"
            + "{[Time].[Time].[1997].[Q1]}\n"
            + "{[Time].[Time].[1997].[Q2]}\n"
            + "Row #0: #ERR: mondrian.olap.fun.MondrianEvaluationException: Could not find an aggregator in the current evaluation context\n"
            + "Row #0: 128,901\n"
            + "Row #0: 41,956\n"
            + "Row #1: #ERR: mondrian.olap.fun.MondrianEvaluationException: Could not find an aggregator in the current evaluation context\n"
            + "Row #1: 128,901\n"
            + "Row #1: 41,956\n"
            + "Row #2: 3\n"
            + "Row #2: 66,291\n"
            + "Row #2: 21,588\n"
            + "Row #3: 3\n"
            + "Row #3: 62,610\n"
            + "Row #3: 20,368\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testConditionalAggregateMemberEvalAfterOtherMembersScoped(Context<?> context) {
    	prepareContext(context);
        final String mdx =
            "With\n"
            + "member Time.Time.Total1 as 'IIF(Measures.CURRENTMEMBER IS Measures.Profit, 1, "
            + "AGGREGATE({[Time].[1997].[Q1],[Time].[1997].[Q2]}))' , SOLVE_ORDER=20 \n"
            + "\n"
            + "member measures.[ratio1] as 'measures.[unit sales] / measures.[sales count]', "
            + "SOLVE_ORDER=10, FORMAT_STRING='#.##'\n"
            + "select {measures.[ratio1],\n"
            + "measures.[unit sales],\n"
            + "measures.[sales count]} on 0,\n"
            + "{Time.Total, Time.Total1, [Time].[1997].[Q1], [Time].[1997].[Q2]} on 1\n"
            + "from sales";
        setSolveOrderMode(context, SCOPED);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[ratio1]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "Axis #2:\n"
            + "{[Time].[Time].[Total]}\n"
            + "{[Time].[Time].[Total1]}\n"
            + "{[Time].[Time].[1997].[Q1]}\n"
            + "{[Time].[Time].[1997].[Q2]}\n"
            + "Row #0: 3.07\n"
            + "Row #0: 128,901\n"
            + "Row #0: 41,956\n"
            + "Row #1: 3.07\n"
            + "Row #1: 128,901\n"
            + "Row #1: 41,956\n"
            + "Row #2: 3.07\n"
            + "Row #2: 66,291\n"
            + "Row #2: 21,588\n"
            + "Row #3: 3.07\n"
            + "Row #3: 62,610\n"
            + "Row #3: 20,368\n");
    }

    /**
     * This test validates that behavior is consistent with Analysis Services
     * 2000 when Solve order scope is ABSOLUTE.  AS2K will throw an error
     * whenever attempting to aggregate over calculated members (i.e. when the
     * solve order of the Aggregate member is higher than the calculations it
     * intersects with).
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testStrToMemberReturningAggEvalAfterOtherMembersAbsolute(Context<?> context) {
    	prepareContext(context);
        final String mdx =
            "With\n"
            + "member Time.Time.StrTotal as 'AGGREGATE({[Time].[Time].[1997].[Q1],[Time].[Time].[1997].[Q2]})', "
            + "SOLVE_ORDER=100\n"
            + "member Time.Time.Total as 'IIF(Measures.CURRENTMEMBER IS Measures.Profit, 1, \n"
            + "StrToMember(\"[Time].[Time].[StrTotal]\"))' , SOLVE_ORDER=20 \n"
            + "member measures.[ratio1] as 'measures.[unit sales] / measures.[sales count]', "
            + "SOLVE_ORDER=10, FORMAT_STRING='#.##'\n"
            + "select {measures.[ratio1],\n"
            + "measures.[unit sales],\n"
            + "measures.[sales count]} on 0,\n"
            + "{Time.Time.Total, [Time].[Time].[1997].[Q1], [Time].[Time].[1997].[Q2]} on 1\n"
            + "from sales";
        setSolveOrderMode(context, ABSOLUTE);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[ratio1]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "Axis #2:\n"
            + "{[Time].[Time].[Total]}\n"
            + "{[Time].[Time].[1997].[Q1]}\n"
            + "{[Time].[Time].[1997].[Q2]}\n"
            + "Row #0: #ERR: mondrian.olap.fun.MondrianEvaluationException: Could not find an aggregator in the current evaluation context\n"
            + "Row #0: 128,901\n"
            + "Row #0: 41,956\n"
            + "Row #1: 3.07\n"
            + "Row #1: 66,291\n"
            + "Row #1: 21,588\n"
            + "Row #2: 3.07\n"
            + "Row #2: 62,610\n"
            + "Row #2: 20,368\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testStrToMemberReturningAggEvalAfterOtherMembersScoped(Context<?> context) {
    	prepareContext(context);
        final String mdx =
            "With\n"
            + "member Time.Time.StrTotal as 'AGGREGATE({[Time].[Time].[1997].[Q1],[Time].[Time].[1997].[Q2]})', "
            + "SOLVE_ORDER=100\n"
            + "member Time.Time.Total as 'IIF(Measures.CURRENTMEMBER IS Measures.Profit, 1, \n"
            + "StrToMember(\"[Time].[Time].[StrTotal]\"))' , SOLVE_ORDER=20 \n"
            + "member measures.[ratio1] as 'measures.[unit sales] / measures.[sales count]', "
            + "SOLVE_ORDER=10, FORMAT_STRING='#.##'\n"
            + "select {measures.[ratio1],\n"
            + "measures.[unit sales],\n"
            + "measures.[sales count]} on 0,\n"
            + "{Time.Time.Total, [Time].[Time].[1997].[Q1], [Time].[Time].[1997].[Q2]} on 1\n"
            + "from sales";
        setSolveOrderMode(context, SCOPED);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[ratio1]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "Axis #2:\n"
            + "{[Time].[Time].[Total]}\n"
            + "{[Time].[Time].[1997].[Q1]}\n"
            + "{[Time].[Time].[1997].[Q2]}\n"
            + "Row #0: 3.07\n"
            + "Row #0: 128,901\n"
            + "Row #0: 41,956\n"
            + "Row #1: 3.07\n"
            + "Row #1: 66,291\n"
            + "Row #1: 21,588\n"
            + "Row #2: 3.07\n"
            + "Row #2: 62,610\n"
            + "Row #2: 20,368\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void test2LevelOfOverrideCubeMemberDoesNotHappenAbsolute(Context<?> context) {
    	prepareContext(context);
        final String mdx =
            "With member gender.override1 as 'gender.maleMinusFemale',\n"
            + "SOLVE_ORDER=20\n"
            + "member gender.override2 as 'gender.override1', SOLVE_ORDER=2\n"
            + "member measures.[ratio1] as 'measures.[unit sales] / measures.[sales count]', "
            + "SOLVE_ORDER=50, FORMAT_STRING='0.0#'\n"
            + "select {measures.[ratio], measures.[ratio1],\n"
            + "measures.[unit sales],\n"
            + "measures.[sales count]} on 0,\n"
            + "{gender.override1, gender.override2, gender.maleMinusFemale} on 1\n"
            + "from sales";
        setSolveOrderMode(context, ABSOLUTE);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[ratio]}\n"
            + "{[Measures].[ratio1]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "Axis #2:\n"
            + "{[Gender].[Gender].[override1]}\n"
            + "{[Gender].[Gender].[override2]}\n"
            + "{[Gender].[Gender].[maleMinusFemale]}\n"
            + "Row #0: 0.0\n"
            + "Row #0: 3.11\n"
            + "Row #0: 3,657\n"
            + "Row #0: 1,175\n"
            + "Row #1: 3.11\n"
            + "Row #1: 3.11\n"
            + "Row #1: 3,657\n"
            + "Row #1: 1,175\n"
            + "Row #2: 0.0\n"
            + "Row #2: 0.0\n"
            + "Row #2: 3,657\n"
            + "Row #2: 1,175\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void test2LevelOfOverrideCubeMemberDoesNotHappenScoped(Context<?> context) {
    	prepareContext(context);
        final String mdx =
            "With member gender.override1 as 'gender.maleMinusFemale',\n"
            + "SOLVE_ORDER=20\n"
            + "member gender.override2 as 'gender.override1', SOLVE_ORDER=2\n"
            + "member measures.[ratio1] as 'measures.[unit sales] / measures.[sales count]', "
            + "SOLVE_ORDER=50, FORMAT_STRING='0.0#'\n"
            + "select {measures.[ratio], measures.[ratio1],\n"
            + "measures.[unit sales],\n"
            + "measures.[sales count]} on 0,\n"
            + "{gender.override1, gender.override2, gender.maleMinusFemale} on 1\n"
            + "from sales";
        setSolveOrderMode(context, SCOPED);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[ratio]}\n"
            + "{[Measures].[ratio1]}\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "Axis #2:\n"
            + "{[Gender].[Gender].[override1]}\n"
            + "{[Gender].[Gender].[override2]}\n"
            + "{[Gender].[Gender].[maleMinusFemale]}\n"
            + "Row #0: 0.0\n"
            + "Row #0: 3.11\n"
            + "Row #0: 3,657\n"
            + "Row #0: 1,175\n"
            + "Row #1: 0.0\n"
            + "Row #1: 3.11\n"
            + "Row #1: 3,657\n"
            + "Row #1: 1,175\n"
            + "Row #2: 0.0\n"
            + "Row #2: 3.11\n"
            + "Row #2: 3,657\n"
            + "Row #2: 1,175\n");
    }
}
