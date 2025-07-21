/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertExprReturns;
import static org.opencube.junit5.TestUtil.assertExprThrows;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertQueryThrows;
import static org.opencube.junit5.TestUtil.getCubeByNameFromArray;
import static org.opencube.junit5.TestUtil.getDimensionByNameFromArray;
import static org.opencube.junit5.TestUtil.getHierarchyByNameFromArray;
import static org.opencube.junit5.TestUtil.getLevelByNameFromArray;
import static org.opencube.junit5.TestUtil.hierarchyName;
import static org.opencube.junit5.TestUtil.withSchema;

import java.sql.SQLException;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.rolap.common.RolapCatalogCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextArgumentsProvider;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.rolap.SchemaModifiers;

/**
 * Unit tests that check compatibility with Microsoft SQL Server Analysis
 * Services 2005.
 *
 * <p>This suite contains a MDX collection of queries that were run on SSAS. The
 * queries cover a variety of issues, including multiple hierarchies in a
 * dimension, attribute hierarchies, and name resolution. Expect to find tests
 * for these areas in dedicated tests also.
 *
 * <p>There are tests for features which are unimplemented or where mondrian's
 * behavior differs from SSAS2005. These tests will appear in this file
 * disabled or with (clearly marked) incorrect results.
 *
 * @author jhyde
 * @since December 15, 2008
 */
class Ssas2005CompatibilityTest {

    /**
     * Whether member naming rules are implemented.
     */
    private static final boolean MEMBER_NAMING_IMPL = false;

    /**
     * Whether attribute hierarchies are implemented.
     */
    public static final boolean ATTR_HIER_IMPL = false;

    /**
     * Whether the AXIS function has been are implemented.
     */
    public static final boolean AXIS_IMPL = false;

    /**
     * Catch-all for tests that depend on something that hasn't been
     * implemented.
     */
    private static final boolean IMPLEMENTED = false;

    @BeforeAll
    public static void beforeAll() {
        ContextArgumentsProvider.dockerWasChanged = true;
    }

    @BeforeEach
    public void beforeEach() {

    }

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

    private void runQ(Context<?> context, String s) {
        prepareContext(context);
        context.getCatalogCache().clear();
        Result result = TestUtil.executeQuery(context.getConnectionWithDefaultRole(), s);
        TestUtil.toString(result);
//        discard();
    }


    private void prepareContext(Context<?> context) {
        // Key features:
        // 1. Dimension [Product] has hierarchies [Products] and at least one
        //    other.
        // 2. Dimension [Currency] has one unnamed hierarchy
        // 3. Dimension [Time] has hierarchies [Time2] and [Time by Week]
        //    (intentionally named hierarchy differently from dimension)
        withSchema(context, SchemaModifiers.Ssas2005CompatibilityTestModifier5::new);
        //withCube("Warehouse and Sales");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUniqueName(Context<?> context) {
        // TODO:
        // Unique mmbers:
        // [Time].[Time2].[Year2].[1997]
        // Non unique:
        // [Time].[Time2].[Quarter].&[Q1]&[1997]
        // All:
        // [Time].[Time2].[All]
        // Unique id:
        // [Currency].[Currency].&[1]
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @DisabledIfSystemProperty(named = "tempIgnoreStrageTests",matches = "true")
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionDotHierarchyAmbiguous(Context<?> context) {
        // If there is a dimension, hierarchy, level with the same name X,
        // then [X].[X] might reasonably resolve to hierarchy or the level.
        // SSAS resolves to hierarchy, old mondrian resolves to level.

            // SSAS gives error with the <Level>.Ordinal function:
            //   The ORDINAL function expects a level expression for
            //   the  argument. A hierarchy expression was used.
            assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
                "[Currency].[Currency].Ordinal",
                "Mondrian Error:MDX object '[Currency].[Currency]' not found in cube 'Sales'");

            // SSAS succeeds with the '<Hierarchy>.Levels(<Numeric Expression>)'
            // function, returns 2
            TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Warehouse and Sales",
                "[Currency].[Currency].Levels(0).Name",
                "(All)");

            // There are 4 hierarchy members (including 'Any currency')
            TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Warehouse and Sales",
                "[Currency].[Currency].Members.Count",
                "15");

            // There are 3 level members
            TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Warehouse and Sales",
                "[Currency].[Currency].[Currency].Members.Count",
                "14");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHierarchyLevelsFunction(Context<?> context) {
        if (!IMPLEMENTED) {
            return;
        }

        // The <Hierarchy>.Levels function is not implemented in mondrian;
        // only <Hierarchy>.Levels(<Numeric Expression>)
        // and <Hierarchy>.Levels(<String Expression>)
        // SSAS returns 7.
        prepareContext(context);
        assertExprReturns(context.getConnectionWithDefaultRole(), "Warehouse and Sales",
            "[Product].[Products].Levels.Count",
            "7");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionDotHierarchyDotLevelDotMembers(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // [dimension].[hierarchy].[level] is valid on dimension with multiple
        // hierarchies;
        // SSAS2005 succeeds
        runQ(context,
            "select [Time].[Time by Week].[Week].MEMBERS on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionDotHierarchyDotLevel(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // [dimension].[hierarchy].[level] is valid on dimension with single
        // hierarchy
        // SSAS2005 succeeds
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(), "Warehouse and Sales",
            "select [Store].[Stores].[Store State].MEMBERS on 0\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[Stores].[Canada].[BC]}\n"
            + "{[Store].[Stores].[Mexico].[DF]}\n"
            + "{[Store].[Stores].[Mexico].[Guerrero]}\n"
            + "{[Store].[Stores].[Mexico].[Jalisco]}\n"
            + "{[Store].[Stores].[Mexico].[Veracruz]}\n"
            + "{[Store].[Stores].[Mexico].[Yucatan]}\n"
            + "{[Store].[Stores].[Mexico].[Zacatecas]}\n"
            + "{[Store].[Stores].[USA].[CA]}\n"
            + "{[Store].[Stores].[USA].[OR]}\n"
            + "{[Store].[Stores].[USA].[WA]}\n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: 74,748\n"
            + "Row #0: 67,659\n"
            + "Row #0: 124,366\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamingDimensionDotLevel(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // [dimension].[level] is valid if level name is unique within all
        // hierarchies. (Note that [Week] is a level in hierarchy
        // [Time].[Time by Week]; here is no attribute [Time].[Week].)
        // SSAS2005 succeeds
        runQ(context,
            "select [Time].[Week].MEMBERS on 0\n"
            + "from [Warehouse and Sales]");

        // [dimension].[level] is valid if level name is unique within all
        // hierarchies. (Note that [Week] is a level in hierarchy
        // [Time].[Time by Week]; here is no attribute [Time].[Week].)
        // SSAS returns "[Time].[Time By Week].[Year2]".
        assertQueryReturns(context.getConnectionWithDefaultRole(), "Warehouse and Sales",
            "with member [Measures].[Foo] as ' [Time].[Year2].UniqueName '\n"
            + "select [Measures].[Foo] on 0\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Foo]}\n"
            + "Row #0: [Time].[Time By Week].[Year2]\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamingDimensionDotLevel2(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // Date2 is a level that occurs in only 1 hierarchy
        // There is no attribute called Date2
        runQ(context,
            "select [Time].[Date2].MEMBERS on 0 from [Warehouse and Sales]");

        // SSAS returns [Time].[Time By Week].[Date2]
        runQ(context,
            "with member [Measures].[Foo] as ' [Time].[Date2].UniqueName '\n"
            + "select [Measures].[Foo] on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamingDimensionDotLevelNotUnique(Context<?> context) {
        if (!IMPLEMENTED) {
            return;
        }

        // Year2 is a level that occurs in only 2 hierarchies:
        // [Time].[Time2].[Year2] and [Time].[Time By Week].[Year2].
        // There is no attribute called Year2
        runQ(context,
            "select [Time].[Year2].MEMBERS on 0 from [Warehouse and Sales]");

        // SSAS2005 returns [Time].[Time By Week].[Year2]
        // (Presumably because it comes first.)
        runQ(context,
            "with member [Measures].[Foo] as ' [Time].[Year2].UniqueName '\n"
            + "select [Measures].[Foo] on 0\n"
            + "from [Warehouse and Sales]");
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @DisabledIfSystemProperty(named = "tempIgnoreStrageTests",matches = "true")
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionMembersOnSingleHierarchyDimension(Context<?> context) {
        // [dimension].members for a dimension with one hierarchy
        // (and no attributes)
        // SSAS2005 succeeds
    	prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(), "Warehouse and Sales",
            "select [Currency].Members on 0\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Currency].[All Currencys]}\n"
            + "{[Currency].[Bulk Mail]}\n"
            + "{[Currency].[Cash Register Handout]}\n"
            + "{[Currency].[Daily Paper]}\n"
            + "{[Currency].[Daily Paper, Radio]}\n"
            + "{[Currency].[Daily Paper, Radio, TV]}\n"
            + "{[Currency].[In-Store Coupon]}\n"
            + "{[Currency].[No Media]}\n"
            + "{[Currency].[Product Attachment]}\n"
            + "{[Currency].[Radio]}\n"
            + "{[Currency].[Street Handout]}\n"
            + "{[Currency].[Sunday Paper]}\n"
            + "{[Currency].[Sunday Paper, Radio]}\n"
            + "{[Currency].[Sunday Paper, Radio, TV]}\n"
            + "{[Currency].[TV]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: 4,320\n"
            + "Row #0: 6,697\n"
            + "Row #0: 7,738\n"
            + "Row #0: 6,891\n"
            + "Row #0: 9,513\n"
            + "Row #0: 3,798\n"
            + "Row #0: 195,448\n"
            + "Row #0: 7,544\n"
            + "Row #0: 2,454\n"
            + "Row #0: 5,753\n"
            + "Row #0: 4,339\n"
            + "Row #0: 5,945\n"
            + "Row #0: 2,726\n"
            + "Row #0: 3,607\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMultipleHierarchyRequiresQualification(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // [dimension].members for a dimension with one hierarchy
        // (and some attributes)
        // SSAS2005 gives error:
        //   Query (1, 8) The 'Product' dimension contains more than
        //   one hierarchy, therefore the hierarchy must be explicitly
        //   specified.
        prepareContext(context);
        TestUtil.assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select [Product].Members on 0\n"
            + "from [Warehouse and Sales]",
            "It may contains more than one hierarchy. Specify the hierarchy explicitly.");
    }

    /**
     * Tests that it is an error to define a calc member in a dimension
     * with multiple hierarchies without specifying hierarchy.
     * Based on {@link BasicQueryTest#testHalfYears()}.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCalcMemberAmbiguousHierarchy(Context<?> context) {
        String mdx =
            "WITH MEMBER [Measures].[ProfitPercent] AS\n"
            + "     '([Measures].[Store Sales]-[Measures].[Store Cost])/"
            + "([Measures].[Store Cost])',\n"
            + " FORMAT_STRING = '#.00%', SOLVE_ORDER = 1\n"
            + " MEMBER [Time].[First Half 97] AS  '[Time].[1997].[Q1] + "
            + "[Time].[1997].[Q2]'\n"
            + " MEMBER [Time].[Second Half 97] AS '[Time].[1997].[Q3] + "
            + "[Time].[1997].[Q4]'\n"
            + " SELECT {[Time].[First Half 97],\n"
            + "     [Time].[Second Half 97],\n"
            + "     [Time].[1997].CHILDREN} ON COLUMNS,\n"
            + " {[Store].[Store Country].[USA].CHILDREN} ON ROWS\n"
            + " FROM [Sales]\n"
            + " WHERE ([Measures].[ProfitPercent])";
        //if (SystemWideProperties.instance().SsasCompatibleNaming) {
        if (true) {
            assertQueryThrows(context.getConnectionWithDefaultRole(),
                mdx,
                "Hierarchy for calculated member '[Time].[First Half 97]' not found");
        } else {
            assertQueryReturns(context.getConnectionWithDefaultRole(), "Warehouse and Sales",
                mdx,
                "Axis #0:\n"
                + "{[Measures].[ProfitPercent]}\n"
                + "Axis #1:\n"
                + "{[Time].[First Half 97]}\n"
                + "{[Time].[Second Half 97]}\n"
                + "{[Time].[1997].[Q1]}\n"
                + "{[Time].[1997].[Q2]}\n"
                + "{[Time].[1997].[Q3]}\n"
                + "{[Time].[1997].[Q4]}\n"
                + "Axis #2:\n"
                + "{[Store].[USA].[CA]}\n"
                + "{[Store].[USA].[OR]}\n"
                + "{[Store].[USA].[WA]}\n"
                + "Row #0: 150.55%\n"
                + "Row #0: 150.53%\n"
                + "Row #0: 150.68%\n"
                + "Row #0: 150.44%\n"
                + "Row #0: 151.35%\n"
                + "Row #0: 149.81%\n"
                + "Row #1: 150.15%\n"
                + "Row #1: 151.08%\n"
                + "Row #1: 149.80%\n"
                + "Row #1: 150.60%\n"
                + "Row #1: 151.37%\n"
                + "Row #1: 150.78%\n"
                + "Row #2: 150.59%\n"
                + "Row #2: 150.34%\n"
                + "Row #2: 150.72%\n"
                + "Row #2: 150.45%\n"
                + "Row #2: 150.39%\n"
                + "Row #2: 150.29%\n");
        }
    }

    // TODO:
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUnqualifiedHierarchy(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // [hierarchy].members for a dimension with one hierarchy
        // (and some attributes)
        // SSAS2005 succeeds
        // Note that 'Product' is the dimension, 'Products' is the hierarchy
        runQ(context,
            "select [Products].Members on 0\n"
            + "from [Warehouse and Sales]");

        runQ(context,
            "select {[Products]} on 0\n"
            + "from [Warehouse and Sales]");

        // TODO: run this in SSAS
        // [Measures] is both a dimension and a hierarchy;
        // [Products] is just a hierarchy.
        // SSAS returns 557863
        runQ(context,
            "select [Measures].[Unit Sales] on 0,\n"
            + "  [Products].[Food] on 1\n"
            + "from [Warehouse and Sales]");
    }

    /**
     * Tests that time functions such as Ytd behave correctly when there are
     * multiple time hierarchies.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testYtd(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // We use 'Generate' to establish context for Ytd without passing it
        // an explicit argument.
        // SSAS returns [Q1], [Q2], [Q3].
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select Generate(\n"
            + "  {[Time].[Time2].[1997].[Q3]},\n"
            + "  {Ytd()}) on 0,\n"
            + " [Products].Children on 1\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "Axis #2:\n"
            + "{[Product].[Products].[Drink]}\n"
            + "{[Product].[Products].[Food]}\n"
            + "{[Product].[Products].[Non-Consumable]}\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAxesOutOfOrder(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // TODO: run this in SSAS
        // Ssas2000 disallowed out-of-order axes. Don't know about Ssas2005.
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] on 1,\n"
            + "[Products].Children on 0\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Product].[Products].[Drink]}\n"
            + "{[Product].[Products].[Food]}\n"
            + "{[Product].[Products].[Non-Consumable]}\n"
            + "Axis #2:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Row #0: 24,597\n"
            + "Row #0: 191,940\n"
            + "Row #0: 50,236\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionMembersRequiresHierarchyQualification(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        context.getCatalogCache().clear();
        // [dimension].members for a dimension with multiple hierarchies
        // SSAS2005 gives error:
        //    Query (1, 8) The 'Time' dimension contains more than one
        //    hierarchy, therefore the hierarchy must be explicitly
        //    specified.
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select [Time].Members on 0\n"
            + "from [Warehouse and Sales]",
            "It may contains more than one hierarchy. Specify the hierarchy explicitly.");
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @DisabledIfSystemProperty(named = "tempIgnoreStrageTests",matches = "true")
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionMemberRequiresHierarchyQualification(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // [dimension].CurrentMember
        // SSAS2005 gives error:
        //   Query (1, 8) The 'Product' dimension contains more than
        //   one hierarchy, therefore the hierarchy must be explicitly
        //   specified.
        final String[] exprs = {
            "[Product].CurrentMember",
            // TODO: Verify that this does indeed fail on SSAS
            "[Product].DefaultMember",
            // TODO: Verify that this does indeed fail on SSAS
            "[Product].AllMembers",
            "Dimensions(3).CurrentMember",
            "Dimensions(3).DefaultMember",
            "Dimensions(3).AllMembers",
        };
        final String expectedException =
            "The 'Product' dimension contains more than one hierarchy, "
            + "therefore the hierarchy must be explicitly specified.";
        Connection connection = context.getConnectionWithDefaultRole();
        assertQueryThrows(connection,
            "select [Product].CurrentMember on 0\n"
            + "from [Warehouse and Sales]",
            expectedException);
        assertQueryThrows(connection,
            "select [Product].DefaultMember on 0\n"
            + "from [Warehouse and Sales]",
            expectedException);
        assertQueryThrows(connection,
            "select [Product].AllMembers on 0\n"
            + "from [Warehouse and Sales]",
            expectedException);

        // The following are OK because Dimensions(<n>) returns a hierarchy.
        final String expectedResult =
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Time].[Time2].[1997]}\n"
            + "Row #0: 266,773\n";
        assertQueryReturns(connection,
            "select Dimensions(3).CurrentMember on 0\n"
            + "from [Warehouse and Sales]",
            expectedResult);
        assertQueryReturns(connection,
            "select Dimensions(3).DefaultMember on 0\n"
            + "from [Warehouse and Sales]",
            expectedResult);
        assertQueryReturns(connection,
            "select Head(Dimensions(7).AllMembers, 3) on 0\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Currency].[All Currencys]}\n"
            + "{[Currency].[Bulk Mail]}\n"
            + "{[Currency].[Cash Register Handout]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: 4,320\n"
            + "Row #0: 6,697\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testImplicitCurrentMemberRequiresHierarchyQualification(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // a function that causes an implicit call to CurrentMember
        // SSAS2005 gives error:
        //   Query (1, 8) The 'Product' dimension contains more than
        //   one hierarchy, therefore the hierarchy must be explicitly
        //   specified.
        prepareContext(context);
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select Ascendants([Product]) on 0\n"
            + "from [Warehouse and Sales]",
            "It may contains more than one hierarchy. Specify the hierarchy explicitly");
        // Works for [Store], which has only one hierarchy.
        // TODO: check SSAS
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select Ascendants([Store]) on 0\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[Stores].[All Storess]}\n"
            + "Row #0: 266,773\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUnqualifiedHierarchyCurrentMember(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // [hierarchy].CurrentMember
        // SSAS2005 succeeds
        runQ(context,
            "select [Products].CurrentMember on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCannotDistinguishMdxFromSql(Context<?> context) {
        // Cannot tell whether statement is MDX or SQL
        // SSAS2005 gives error:
        //   Parser: The statement dialect could not be resolved due
        //   to ambiguity.
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select [Time].Members\n"
            + "from [Warehouse and Sales]",
            "Encountered an error at (or somewhere around) input:2:1");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamingDimensionAttr(Context<?> context) {
        if (!ATTR_HIER_IMPL) {
            return;
        }
        // [dimension].[attribute] succeeds
        // (There is no level called [Store Manager])
        runQ(context,
            "select [Store].[Store Manager].Members on 0 from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamingDimensionAttrVsLevel(Context<?> context) {
        if (!ATTR_HIER_IMPL) {
            return;
        }
        // [dimension].[attribute]
        // (There is a level called [Store City], but the attribute is chosen in
        // preference.)
        // SSAS2005 succeeds
        runQ(context,
            "select [Store].[Store City].Members on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAttrHierarchyMemberParent(Context<?> context) {
        if (!ATTR_HIER_IMPL) {
            return;
        }
        // parent of member of attribute hierarchy
        // SSAS2005 returns "[Store].[Store City].[All]"
        runQ(context,
            "with member [Measures].[Foo] as ' [Store].[Store City].[San Francisco].Parent.UniqueName '\n"
            + "select [Measures].[Foo] on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAttrHierarchyMemberChildren(Context<?> context) {
        if (!ATTR_HIER_IMPL) {
            return;
        }
        // children of member of attribute hierarchy
        // SSAS2005 returns empty set
        runQ(context,
            "select [Store].[Store City].[San Francisco].Children on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAttrHierarchyAllMemberChildren(Context<?> context) {
        if (!ATTR_HIER_IMPL) {
            return;
        }
        // children of all member of attribute hierarchy
        // SSAS2005 succeeds
        runQ(context,
            "select [Store].[Store City].Children on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAttrHierarchyMemberLevel(Context<?> context) {
        if (!ATTR_HIER_IMPL) {
            return;
        }
        // level of member of attribute hierarchy
        // SSAS2005 returns "[Store].[Store City].[Store City]"
        runQ(context,
            "with member [Measures].[Foo] as [Store].[Store City].[San Francisco].Level.UniqueName\n"
            + "select [Measures].[Foo] on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAttrHierarchyUniqueName(Context<?> context) {
        if (!ATTR_HIER_IMPL) {
            return;
        }
        // Returns [Store].[Store City]
        runQ(context,
            "with member [Measures].[Foo] as [Store].[Store City].UniqueName\n"
            + "select [Measures].[Foo] on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMemberAddressedByLevelAndKey(Context<?> context) {
        if (!MEMBER_NAMING_IMPL) {
            return;
        }
        // [dimension].[hierarchy].[level].&[key]
        // (Returns 31, 5368)
        runQ(context,
            "select {[Time].[Time By Week].[Week].[31]} on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMemberAddressedByCompoundKey(Context<?> context) {
        if (!MEMBER_NAMING_IMPL) {
            return;
        }
        // compound key
        // SSAS2005 returns 1 row
        runQ(context,
            "select [Time].[Time By Week].[Year2].[1998].&[30]&[1998] on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMemberAddressedByPartialCompoundKey(Context<?> context) {
        if (!MEMBER_NAMING_IMPL) {
            return;
        }
        // compound key, partially specified
        // SSAS2005 returns 0 rows but no error
        runQ(context,
            "select [Time].[Time By Week].[Year2].[1998].&[30] on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMemberAddressedByNonUniqueName(Context<?> context) {
        if (!MEMBER_NAMING_IMPL) {
            return;
        }
        // address member by non-unique name
        // [dimension].[hierarchy].[level].[name]
        // SSAS2005 returns first member that matches, 1997.January
        runQ(context,
            "select [Time].[Time2].[Month].[January] on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMemberAddressedByLevelAndCompoundKey(Context<?> context) {
        if (!MEMBER_NAMING_IMPL) {
            return;
        }
        // SSAS2005 returns [Time].[Time2].[Month].&[1]&[1997]
        runQ(context,
            "with member [Measures].[Foo] as ' [Time].[Time2].[Month].[January].UniqueName '\n"
            + "select [Measures].[Foo] on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMemberAddressedByLevelAndName(Context<?> context) {
        if (!MEMBER_NAMING_IMPL) {
            return;
        }
        // similarly
        // [dimension].[level].[member name]
        runQ(context,
            "with member [Measures].[Foo] as ' [Store].[Store City].[Month].[January].UniqueName '\n"
            + "select [Measures].[Foo] on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFoo31(Context<?> context) {
        // [dimension].[member name]
        // returns [Product].[Products].[Product Department].[Dairy]
        // note that there are members
        //   [Product].[Drink].[Dairy]
        //   [Product].[Drink].[Dairy].[Dairy]
        //   [Product].[Food].[Dairy]
        //   [Product].[Food].[Dairy].[Dairy]
        runQ(context,
            "select Measures on 0,[Product].[Product Department].Members on 1\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFoo32(Context<?> context) {
        if (!IMPLEMENTED) {
            return;
        }
        // returns [Product].[Products].[Product Department].[Dairy]
        // In my opinion this is weird unique name, because there is a
        // Food.Dairy and a Drink.Dairy. But behavior is consistent with
        // returning first member that matches.
        runQ(context,
            "with member [Measures].[U] as ' [Product].UniqueName '\n"
            + "    member [Measures].[PU] as ' [Product].Parent.UniqueName '\n"
            + "select {[Measures].[U], [Measures].[PU]} on 0,\n"
            + "  [Product].[Dairy] on 1\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamingAttrVsLevel(Context<?> context) {
        if (!ATTR_HIER_IMPL) {
            return;
        }
        // [attribute] vs. [level]
        // SSAS2005 succeeds
        runQ(context,
            "select [Store City].Members on 0\n"
            + "from [Warehouse and Sales]");

        // the attribute hierarchy wins over the level
        // SSAS2005 returns [Store].[Store City]
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Measures].[Foo] as [Store City].UniqueName\n"
            + "select [Measures].[Foo] on 0\n"
            + "from [Warehouse and Sales]", "xxxxx");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUnqualifiedLevel(Context<?> context) {
        if (!IMPLEMENTED) {
            return;
        }
        // [level]
        // SSAS2005 succeeds
        runQ(context,
            "select [Week].Members on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionAsScalarExpression(Context<?> context) {
        if (!IMPLEMENTED) {
            return;
        }
        // Dimension used as scalar expression fails.
        // SSAS2005 gives error:
        //   The function expects a string or numeric expression for
        //    the argument.  A level expression was used.
        runQ(context,
            "with member [Measures].[Foo] as [Date2]\n"
            + "select [Measures].[Foo] on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionWithMultipleHierarchiesDotParent(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // [Dimension].Parent
        // SSAS2005 returns error:
        //   The 'Product' dimension contains more than one hierarchy,
        //   therefore the hierarchy must be explicitly specified.
        assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
            "[Time].Parent.UniqueName",
            "It may contains more than one hierarchy. Specify the hierarchy explicitly");
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @DisabledIfSystemProperty(named = "tempIgnoreStrageTests",matches = "true")
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionDotHierarchyInBrackets(Context<?> context) {
        // [dimension.hierarchy] is valid
        // SSAS2005 succeeds
        runQ(context,
            "select {[Time.Time By Week].Members} on 0\n"
            + "from [Warehouse and Sales]");
    }

    /**
     * Test case for bug 2688790, "Hierarchy Naming Compatibility issue".
     * Occurs when dimension and hierarchy have the same name and are used with
     * [name.name].
     */
    @Disabled //TODO need investigate
    @ParameterizedTest
    @DisabledIfSystemProperty(named = "tempIgnoreStrageTests",matches = "true")
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionDotHierarchySameNameInBrackets(Context<?> context) {
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "<Dimension name=\"Store Type 2\" foreignKey=\"store_id\">"
            + " <Hierarchy name=\"Store Type 2\" hasAll=\"true\" primaryKey=\"store_id\">"
            + " <Table name=\"store\"/>"
            + " <Level name=\"Store Type\" column=\"store_type\" uniqueMembers=\"true\"/>"
            + " </Hierarchy>"
            + "</Dimension>",
            null));
         */
    	withSchema(context, SchemaModifiers.Ssas2005CompatibilityTestModifier1::new);

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select [Store Type 2.Store Type 2].[Store Type].members ON columns "
            + "from [Sales] where [Time].[1997]",
            "Axis #0:\n"
            + "{[Time].[1997]}\n"
            + "Axis #1:\n"
            + "{[Store Type 2].[Deluxe Supermarket]}\n"
            + "{[Store Type 2].[Gourmet Supermarket]}\n"
            + "{[Store Type 2].[HeadQuarters]}\n"
            + "{[Store Type 2].[Mid-Size Grocery]}\n"
            + "{[Store Type 2].[Small Grocery]}\n"
            + "{[Store Type 2].[Supermarket]}\n"
            + "Row #0: 76,837\n"
            + "Row #0: 21,333\n"
            + "Row #0: \n"
            + "Row #0: 11,491\n"
            + "Row #0: 6,557\n"
            + "Row #0: 150,555\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionDotLevelDotHierarchyInBrackets(Context<?> context) {
        // [dimension.hierarchy.level]
        // SSAS2005 gives error:
        //   Query (1, 8) The dimension '[Time.Time2.Quarter]' was not
        //   found in the cube when the string, [Time.Time2.Quarter],
        //   was parsed.
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select [Time.Time2.Quarter].Members on 0\n"
            + "from [Warehouse and Sales]",
            "MDX object '[Time.Time2.Quarter]' not found in cube 'Warehouse and Sales'");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionDotInvalidHierarchyInBrackets(Context<?> context) {
        // invalid hierarchy name
        // SSAS2005 gives error:
        //  Query (1, 9) The dimension '[Time.Time By Week55]' was not
        //  found in the cube when the string, [Time.Time By Week55],
        //  was parsed.
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select {[Time.Time By Week55].Members} on 0\n"
            + "from [Warehouse and Sales]",
            "MDX object '[Time.Time By Week55]' not found in cube 'Warehouse and Sales'");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionDotDimensionInBrackets(Context<?> context) {
        // [dimension.dimension] is invalid.  SSAS2005 gives similar
        // error to above.  (The Time dimension has hierarchies called
        // [Time2] and [Time By Day]. but no hierarchy [Time].)
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select {[Time.Time].Members} on 0\n"
            + "from [Warehouse and Sales]",
            "MDX object '[Time.Time]' not found in cube 'Warehouse and Sales'");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionDotHierarchyDotNonExistentLevel(Context<?> context) {
        if (!IMPLEMENTED) {
            return;
        }
        // Non-existent level of hierarchy.
        // SSAS2005 gives error:
        //  Query (1, 8) The MEMBERS function expects a hierarchy
        //  expression for the argument. A member expression was used.
        //
        // Mondrian currently gives
        //  MDX object '[Time].[Time By Week].[Month]' not found in
        //  cube 'Warehouse and Sales'
        // which is not good enough.
        runQ(context,
            "select [Time].[Time By Week].[Month].Members on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionDotHierarchyDotLevelMembers(Context<?> context) {
        if (!IMPLEMENTED) {
            return;
        }
        // SSAS2005 returns 8 quarters.
        runQ(context,
            "select [Time].[Time2].[Quarter].Members on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDupHierarchyOnAxes(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // same hierarchy on both axes
        // SSAS2005 gives error:
        //   The Products hierarchy already appears in the Axis0 axis.
        // SSAS query:
        //   select [Products] on 0,
        //     [Products] on 1
        //   from [Warehouse and Sales]
        prepareContext(context);
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select {[Products]} on 0,\n"
            + "  {[Products]} on 1\n"
            + "from [Warehouse and Sales]",
            "Hierarchy '[Product].[Products]' appears in more than one independent axis.");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionOnAxis(Context<?> context) {
        if (!IMPLEMENTED) {
            return;
        }
        // Dimension is implicitly converted to member
        // so is OK on axis.
        runQ(context,"select [Product] on 0 from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionDotHierarchyOnAxis(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // Dimension is implicitly converted to member
        // so is OK on axis.
        runQ(context,
            "select [Product].[Products] on 0,\n"
            + "[Customer].[Customer] on 1\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHierarchiesFromSameDimensionOnAxes(Context<?> context) {
        if (!IMPLEMENTED) {
            return;
        }
        // different hierarchies from same dimension
        // SSAS2005 succeeds
        runQ(context,
            "select [Time].[Time2] on 0,\n"
            + "  [Time].[Time By Week] on 1\n"
            + "from [Warehouse and Sales]");
    }

    // TODO:
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDifferentHierarchiesFromSameDimensionOnAxes(Context<?> context) {
        if (!IMPLEMENTED) {
            return;
        }
        // different hierarchies from same dimension
        // SSAS2005 succeeds
        // Note that [Time].[1997] resolves to [Time].[Time2].[1997]
        runQ(context,
            "select [Time].[Time2] on 0,\n"
            + "  [Time].[Time By Week] on 1\n"
            + "from [Warehouse and Sales]\n"
            + "where [Time].[1997]");
    }

    // TODO:
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDifferentHierarchiesFromSameDimensionInCrossjoin(Context<?> context) {
        if (!IMPLEMENTED) {
            return;
        }
        // crossjoin different hierarchies from same dimension
        // SSAS2005 succeeds
        runQ(context,
            "select Crossjoin([Time].[Time By Week].Children, [Time].[Time2].Members) on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHierarchyUsedTwiceInCrossjoin(Context<?> context) {
        if (!IMPLEMENTED) {
            return;
        }
        // SSAS2005 gives error:
        //   Query (2, 4) The Time By Week hierarchy is used more than
        //   once in the Crossjoin function.
        runQ(context,
            "select \n"
            + "   [Time].[Time By Week].Children\n"
            + "     * [Time].[Time2].Children\n"
            + "     * [Time].[Time By Week].Children on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAttributeHierarchyUsedTwiceInCrossjoin(Context<?> context) {
        if (!ATTR_HIER_IMPL) {
            return;
        }
        // Attribute hierarchy used more than once in Crossjoin.
        // SSAS2005 gives error:
        //   Query (2, 4) The SKU hierarchy is used more than once in
        //   the Crossjoin function.
        runQ(context,
            "select \n"
            + "   [Product].[SKU].Children\n"
            + "     * [Product].[Products].Members\n"
            + "     * [Time].[Time By Week].Children\n"
            + "     * [Product].[SKU].Members on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFoo50(Context<?> context) {
        if (!ATTR_HIER_IMPL) {
            return;
        }
        // Mixing attributes in a set
        // SSAS2005 gives error:
        //    Members belong to different hierarchies in the  function.
        runQ(context,
            "select {[Store].[Store Country].[USA], [Store].[Stores].[Store Country].[USA]} on 0\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testQuoteInStringInQuotedFormula(Context<?> context) {
        // Quoted formulas vs. unquoted formulas
        // Single quote in string
        // SSAS2005 returns 5
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Measures].[Foo] as ' len(\"can''t\") '\n"
            + "select [Measures].[Foo] on 0\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Foo]}\n"
            + "Row #0: 5\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testQuoteInStringInUnquotedFormula(Context<?> context) {
        // SSAS2005 returns 6
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Measures].[Foo] as len(\"can''t\")\n"
            + "select [Measures].[Foo] on 0\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Foo]}\n"
            + "Row #0: 6\n");
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMemberIdentifiedByDimensionAndKey(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // Member identified by dimension, key;
        // works on SSAS;
        // gives {[Washington Berry Juice], 231}.
        // Presumably, if level is not specified, it defaults to lowest level?
        runQ(context,
            "select [Measures].[Unit Sales] on 0,\n"
            + "[Product].[Products].&[1] on 1\n"
            + "from [Warehouse and Sales]");
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionHierarchyKey(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // member identified by dimension, hierarchy, key
        // works on SSAS
        // gives {[Washington Berry Juice], 231}
        runQ(context,
            "select [Measures].[Unit Sales] on 0,\n"
            + "[Product].[Products].&[1] on 1\n"
            + "from [Warehouse and Sales]");
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCompoundKey(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // compound key
        // succeeds on SSAS
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] on 0,\n"
            + "[Time].[Time2].[Month].&[12]&Q4&[1997] on 1\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Time].[Time2].[1997].[Q4].[December]}\n"
            + "Row #0: 26,796\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCompoundKeySyntaxError(Context<?> context) {
        // without [] fails on SSAS (syntax error because a number)
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] on 0,\n"
            + "[Product].[Products].[Brand Name].&43&[Walrus] on 1\n"
            + "from [Warehouse and Sales]",
            "Encountered an error at (or somewhere around) input:2:35");
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCompoundKeyStringBad(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // too few values in key
        prepareContext(context);
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] on 0,\n"
            + "[Product].[Products].[Brand Name].&[43]&Walrus&Foo on 1\n"
            + "from [Warehouse and Sales]",
            "MDX object '[Product].[Products].[Brand Name].[43].Walrus.Foo' not found in cube 'Warehouse and Sales'.");

        // too few values in key
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] on 0,\n"
            + "[Time].[Time2].[Quarter].&Q3 on 1\n"
            + "from [Warehouse and Sales]",
            "MDX object '[Time].[Time2].[Quarter].&Q3' not found in cube 'Warehouse and Sales'.");

        // too many values in key
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] on 0,\n"
            + "[Time].[Time2].[Quarter].&Q3&[1997]&ABC on 1\n"
            + "from [Warehouse and Sales]",
            "MDX object '[Time].[Time2].[Quarter].&Q3&[1997]&ABC' not found in cube 'Warehouse and Sales'.");
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCompoundKeyString(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // succeeds on SSAS (gives 1 row)
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] on 0,\n"
            + "[Store].[Stores].[Store City].&[San Francisco]&CA on 1\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Stores].[USA].[CA].[San Francisco]}\n"
            + "Row #0: 2,117\n");
    }

    /**
     * Tests a member where a name segments {@code [San Francisco].[Store 14]}
     * occur after a key segment {@code &amp;&amp;CA}.
     *
     * <p>Needs to work regardless of the value of
     * {@link SystemWideProperties#SsasCompatibleNaming}. Mondrian-3 had this
     * functionality.</p>
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNameAfterKey(Context<?> context) {
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] on 0,\n"
            + hierarchyName("Store", "Stores")
            + ".[Store State].&CA.[San Francisco].[Store 14] on 1\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{"
            + hierarchyName("Store", "Stores")
            + ".[USA].[CA].[San Francisco].[Store 14]}\n"
            + "Row #0: 2,117\n");
    }

    /**
     * Tests a member where a name segment {@code [Store 14]} occurs after a
     * composite key segment {@code &amp;[San Francisco]&amp;CA}.
     */
    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNameAfterCompositeKey(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] on 0,\n"
            + "[Store].[Stores].[Store City].&[San Francisco]&CA.[Store 14] on 1\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Stores].[USA].[CA].[San Francisco].[Store 14]}\n"
            + "Row #0: 2,117\n");
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCompoundKeyAll(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        prepareContext(context);
        assertExprReturns(context.getConnectionWithDefaultRole(), "Warehouse and Sales",
                "[Customer].Level.Name",
                "(All)");
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] on 0,\n"
            + "[Customer].[(All)].&All on 1\n"
            + "from [Warehouse and Sales]",
            "Wrong number of values in member key; &All has 1 values, whereas level's key has 0 columns [].");
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCompoundKeyParent(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        prepareContext(context);
        assertAxisReturns(context.getConnectionWithDefaultRole(), "[Warehouse and Sales]",
                "[Store].[Stores].[Store City].&[San Francisco]&CA.Parent",
                "[Store].[Stores].[USA].[CA]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCompoundKeyNull(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // Note: [Store Size in SQFT].[#null] is the member whose name is null;
        //   [Store Size in SQFT].&[#null] is the member whose key is null.
        // REVIEW: Does SSAS use the same syntax, '&[#null]', for null key?
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] on 0,\n"
            + "[Store Size in SQFT].[Store Size in SQFT].&[#null] on 1\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store Size in SQFT].[Store Size in SQFT].[#null]}\n"
            + "Row #0: 39,329\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFoo56(Context<?> context) {
        if (!IMPLEMENTED) {
            return;
        }
        // succeeds on SSAS (gives 1 row)
        runQ(context,
            "select [Measures].[Unit Sales] on 0,\n"
            + "[Product].[Products].[Brand Name].[Walrus] on 1\n"
            + "from [Warehouse and Sales]");
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @DisabledIfSystemProperty(named = "tempIgnoreStrageTests",matches = "true")
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testKeyNonExistent(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // SSAS gives 1 row
        runQ(context,
            "select [Measures].[Unit Sales] on 0,\n"
            + "[Time].[Time2].[Quarter].&Q3&[1997] on 1\n"
            + "from [Warehouse and Sales]");

        ((TestContextImpl)context).setIgnoreInvalidMembersDuringQuery(true);
        // SSAS gives 0 rows
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] on 0,\n"
            + "[Time].[Time2].[Quarter].&Q5&[1997] on 1\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n");

        ((TestContextImpl)context).setIgnoreInvalidMembersDuringQuery(false);
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] on 0,\n"
            + "[Time].[Time2].[Quarter].&Q5&[1997] on 1\n"
            + "from [Warehouse and Sales]",
            "MDX object '[Time].[Time2].[Quarter].&Q5&[1997]' not found in cube 'Warehouse and Sales'");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAxesLabelsOutOfSequence(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // succeeds on SSAS
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] on 1,\n"
            + "[Product].[Products] on 0\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Product].[Products].[All Productss]}\n"
            + "Axis #2:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Row #0: 266,773\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAxisLabelsNotContiguousFails(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // SSAS gives error:
        //   Query (1, 8) Axis numbers specified in a query must be sequentially
        //   specified, and cannot contain gaps.
        prepareContext(context);
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] on 1,\n"
            + "[Product].[Products].Children on 2\n"
            + "from [Warehouse and Sales]",
            "Axis numbers specified in a query must be sequentially "
            + "specified, and cannot contain gaps. Axis 0 (COLUMNS) is missing.");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLotsOfAxes(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // lots of axes, mixed ways of specifying axes
        // SSAS succeeds, although Studio says:
        //   Results cannot be displayed for cellsets with more than two axes.
        runQ(context,
            "select [Measures].[Unit Sales] on axis(0),\n"
            + "[Product].[Products] on rows,\n"
            + "[Customer].[Customer] on pages,\n"
            + "[Currency] on 3,\n"
            + "[Promotion] on axis(4),\n"
            + "[Time].[Time2] on 5,\n"
            + "[Time].[Time by Week] on 6\n"
            + "from [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOnAxesFails(Context<?> context) {
        // axes(n) is not an acceptable alternative to axis(n)
        // SSAS gives:
        //   Query (1, 35) Parser: The syntax for 'axes' is incorrect.
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] on axes(0)\n"
            + "from [Warehouse and Sales]",
            "Found string \"axes\" of type ID");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOnExpression(Context<?> context) {
        // SSAS gives syntax error
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] on 0 + 1\n"
            + "from [Warehouse and Sales]",
            "Encountered an error at (or somewhere around) input:1:37");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOnFractionFails(Context<?> context) {
        // SSAS gives syntax error
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] on 0.4\n"
            + "from [Warehouse and Sales]",
            "Found string \"0.4\" of type DECIMAL_NUMERIC_LITERAL");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAxisFunction(Context<?> context) {
        // AXIS(n) function as expression
        // SSAS succeeds
        if (!AXIS_IMPL) {
            return;
        }
        runQ(context,
            "WITH MEMBER MEASURES.AXISDEMO AS\n"
            + "  SUM(AXIS(1), [Measures].[Unit Sales])\n"
            + "SELECT {[Measures].[Unit Sales],MEASURES.AXISDEMO} ON 0,\n"
            + "{[Time].[Time by Week].Children} ON 1\n"
            + "FROM [Warehouse and Sales]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAxisAppliedToExpr(Context<?> context) {
        // Axis applied to an expression ('3 - 2' in place of '1' above).
        // SSAS succeeds.
        // When we implement Axis, it may be acceptable for Mondrian to fail in
        // this case - or perhaps struggle on with less type information.
        if (!AXIS_IMPL) {
            return;
        }
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER MEASURES.AXISDEMO AS\n"
            + "  SUM(AXIS(1), [Measures].[Unit Sales])\n"
            + "SELECT {[Measures].[Unit Sales],MEASURES.AXISDEMO} ON 0,\n"
            + "{[Time].[Time by Week].Children} ON 1\n"
            + "FROM [Warehouse and Sales]",
            "xxx");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAxisFunctionReferencesPreviousAxis(Context<?> context) {
        // reference axis 0 while computing axis 1
        // SSAS succeeds
        if (!AXIS_IMPL) {
            return;
        }
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER MEASURES.AXISDEMO AS\n"
            + "  SUM(AXIS(0), [Measures].CurrentMember)\n"
            + "SELECT {[Measures].[Store Sales],MEASURES.AXISDEMO} ON 0,\n"
            + "{Filter([Time].[Time by Week].Members, Measures.AxisDemo > 0)} ON 1\n"
            + "FROM [Warehouse and Sales]",
            "xxx");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAxisFunctionReferencesSameAxisFails(Context<?> context) {
        // reference axis 1 while computing axis 1, not ok
        // SSAS gives:
        //   Infinite recursion detected. The loop of dependencies is: AXISDEMO
        //   -> AXISDEMO.
        if (!AXIS_IMPL) {
            return;
        }
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "WITH MEMBER MEASURES.AXISDEMO AS\n"
            + "  SUM(AXIS(1), [Measures].CurrentMember)\n"
            + "SELECT {[Measures].[Store Sales],MEASURES.AXISDEMO} ON 0,\n"
            + "{Filter([Time].[Time by Week].Members, Measures.AxisDemo > 0)} ON 1\n"
            + "FROM [Warehouse and Sales]",
            "xxx");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAxisFunctionReferencesSameAxisZeroFails(Context<?> context) {
        // reference axis 0 while computing axis 0, not ok
        // SSAS gives:
        //   Infinite recursion detected. The loop of dependencies is: AXISDEMO
        //   -> AXISDEMO.
        if (!AXIS_IMPL) {
            return;
        }
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "WITH MEMBER MEASURES.AXISDEMO AS\n"
            + "  SUM(AXIS(0), [Measures].CurrentMember)\n"
            + "SELECT {[Measures].[Store Sales],MEASURES.AXISDEMO} ON 1,\n"
            + "{Filter([Time].[Time by Week].Members, Measures.AxisDemo > 0)} ON 0\n"
            + "FROM [Warehouse and Sales]",
            "xxx");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAxisFunctionReferencesLaterAxis(Context<?> context) {
        // reference axis 1 while computing axis 0, ok
        // The SSAS online doc says:
        //    An axis can reference only a prior axis. For example, Axis(0) must
        //    occur after the COLUMNS axis has been evaluated, such as on a ROW
        //    or PAGE axis.
        // but nevertheless SSAS does the right thing and allows this query.
        if (!AXIS_IMPL) {
            return;
        }
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER MEASURES.AXISDEMO AS\n"
            + "  SUM(AXIS(1), [Measures].CurrentMember)\n"
            + "SELECT {[Measures].[Store Sales],MEASURES.AXISDEMO} ON 1,\n"
            + "{Filter([Time].[Time by Week].Members, Measures.AxisDemo > 0)} ON 0\n"
            + "FROM [Warehouse and Sales]",
            "xxx");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAxisFunctionReferencesSameAxisInlineFails(Context<?> context) {
        // If we inline the member, SSAS runs out of memory.
        // SSAS gives error:
        //   Memory error: Allocation failure : The paging file is too small for
        //   this operation to complete. .
        // (Should give cyclicity error.)
        if (!AXIS_IMPL) {
            return;
        }
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "SELECT [Measures].[Store Sales] ON 1,\n"
            + "{Filter([Time].[Time by Week].Members, SUM(AXIS(0), [Measures].CurrentMember) > 0)} ON 0\n"
            + "FROM [Warehouse and Sales]",
            "xxx cyclic something");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinMember(Context<?> context) {
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
            // Can't resolve [Products] under old mondrian
        //    return;
        //}
        // Mondrian currently gives error:
        //   No function matches signature 'crossjoin(<Member>, <Set>)'
        if (!IMPLEMENTED) {
            return;
        }
        // Apply crossjoin(Member,Set)
        // SSAS gives 626866, 626866, 626866.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select crossjoin([Products].DefaultMember, [Gender].Members) on 0\n"
            + "from [Warehouse and Sales]",
            "xx");
    }

    /**
     * Tests the ambiguity between a level and a member of the same name,
     * both in SSAS compatible mode and in regular mode.
     * @throws SQLException If the test fails.
     */
    @Disabled //has not been fixed during creating Daanse project
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCanHaveMemberWithSameNameAsLevel(Context<?> context) throws SQLException {
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
             "<Dimension name=\"SameName\" foreignKey=\"customer_id\">\n"
             + " <Hierarchy hasAll=\"true\" primaryKey=\"id\">\n"
             + " <InlineTable alias=\"sn\">\n"
             + " <ColumnDefs>\n"
             + " <ColumnDef name=\"id\" type=\"Numeric\" />\n"
             + " <ColumnDef name=\"desc\" type=\"String\" />\n"
             + " </ColumnDefs>\n"
             + " <Rows>\n"
             + " <Row>\n"
             + " <Value column=\"id\">1</Value>\n"
             + " <Value column=\"desc\">SameName</Value>\n"
             + " </Row>\n"
             + " </Rows>\n"
             + " </InlineTable>\n"
             + " <Level name=\"SameName\" column=\"desc\" uniqueMembers=\"true\" />\n"
             + " </Hierarchy>\n"
             + "</Dimension>"));
         */
        withSchema(context, SchemaModifiers.Ssas2005CompatibilityTestModifier2::new);
        Cube cube = getCubeByNameFromArray(context.getConnectionWithDefaultRole()
            .getCatalog().getCubes(), "Sales").orElseThrow(() -> new RuntimeException("Cube with name \"Sales\" is absent"));
        Dimension dimension =  getDimensionByNameFromArray(cube.getDimensions(), "SameName")
            .orElseThrow(() -> new RuntimeException("Dimension with name \"SameName\" is absent"));
        Hierarchy hierarchy = getHierarchyByNameFromArray(dimension.getHierarchies(), "SameName")
            .orElseThrow(() -> new RuntimeException("Hierarchy with name \"SameName\" is absent"));
        Level level = getLevelByNameFromArray(hierarchy.getLevels(), "SameName")
            .orElseThrow(() -> new RuntimeException("Level with name \"SameName\" is absent"));
        Member member = level.getMembers().get(0);
        assertEquals(
            "[SameName].[SameName].[SameName]",
            member.getUniqueName());

        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select {"
            + "[SameName].[SameName].[SameName]"
            + "} on 0 from Sales",
            "Mondrian Error:No function matches signature '{<Level>}'");

            assertQueryReturns(context.getConnectionWithDefaultRole(),
                "select {[SameName].[SameName].[SameName].[SameName]} on 0 from Sales",
                "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[SameName].[SameName].[SameName]}\n"
                + "Row #0: \n");
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @DisabledIfSystemProperty(named = "tempIgnoreStrageTests",matches = "true")
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMemberNameSortCaseSensitivity(Context<?> context)
    {
        // In SSAS, "MacDougal" occurs between "Maccietto" and "Macha". This
        // would not occur if sort was case-sensitive.
    	//prepareContext(context);
    	/*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                "Sales",
                "  <Dimension name=\"Customer Last Name\" "
                + "foreignKey=\"customer_id\">\n"
                + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Customers\""
                + " primaryKey=\"customer_id\">\n"
                + "      <Table name=\"customer\"/>\n"
                + "      <Level name=\"Last Name\" column=\"lname\" keyColumn=\"customer_id\" uniqueMembers=\"true\"/>\n"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"));
    	 */
    	withSchema(context, SchemaModifiers.Ssas2005CompatibilityTestModifier3::new);
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "head(\n"
            + "  filter(\n"
            + "    [Customer Last Name].[Last Name].Members,"
            + "    Left([Customer Last Name].[Last Name].CurrentMember.Name, "
            + "1) = \"M\"),\n"
            + "  10)",
            "[Customer Last Name].[Mabe]\n"
            + "[Customer Last Name].[Macaluso]\n"
            + "[Customer Last Name].[MacBride]\n"
            + "[Customer Last Name].[Maccietto]\n"
            + "[Customer Last Name].[MacDougal]\n"
            + "[Customer Last Name].[Macha]\n"
            + "[Customer Last Name].[Macias]\n"
            + "[Customer Last Name].[Mack]\n"
            + "[Customer Last Name].[Mackin]\n"
            + "[Customer Last Name].[Maddalena]");

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "order(\n"
            + "  head(\n"
            + "    filter(\n"
            + "      [Customer Last Name].[Last Name].Members,"
            + "      Left([Customer Last Name].[Last Name].CurrentMember.Name, 1) = \"M\"),\n"
            + "  10),\n"
            + " [Customer Last Name].[Last Name].CurrentMember.Name)",
            "[Customer Last Name].[Mabe]\n"
            + "[Customer Last Name].[Macaluso]\n"
            + "[Customer Last Name].[MacBride]\n"
            + "[Customer Last Name].[Maccietto]\n"
            + "[Customer Last Name].[MacDougal]\n"
            + "[Customer Last Name].[Macha]\n"
            + "[Customer Last Name].[Macias]\n"
            + "[Customer Last Name].[Mack]\n"
            + "[Customer Last Name].[Mackin]\n"
            + "[Customer Last Name].[Maddalena]");
    }

    /**
     * SSAS can resolve root members of a hierarchy even if not qualified
     * by hierarchy, and even if the dimension has more than one hierarchy.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRootMembers(Context<?> context) {
    	prepareContext(context);
        // for member defined in the database
        final String timeByWeek =
            hierarchyName("Time", "Time By Week");
        assertExprReturns(context.getConnectionWithDefaultRole(), "Warehouse and Sales",
            "[Time].[1997].Level.UniqueName",
            timeByWeek + ".[Year2]");

        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}
        // now for a calc member defined in a query
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Time].[Time2].[Foo] as\n"
            + "[Time].[Time2].[1997] + [Time].[Time2].[1997].[Q3]\n"
            + "select [Time].[Foo] on 0\n"
            + "from [Warehouse and Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Time].[Time2].[Foo]}\n"
            + "Row #0: 332,621\n");
    }

}
