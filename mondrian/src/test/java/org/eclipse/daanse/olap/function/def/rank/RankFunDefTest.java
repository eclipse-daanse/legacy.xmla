/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.function.def.rank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencube.junit5.TestUtil.assertExprReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.executeQuery;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.Result;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.SystemWideProperties;


class RankFunDefTest {


    /**
     * Tests the <code>Rank(member, set)</code> MDX function.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRank(Context context) {
        // Member within set
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank([Store].[USA].[CA], "
                + "{[Store].[USA].[OR],"
                + " [Store].[USA].[CA],"
                + " [Store].[USA]})", "2" );
        // Member not in set
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank([Store].[USA].[WA], "
                + "{[Store].[USA].[OR],"
                + " [Store].[USA].[CA],"
                + " [Store].[USA]})", "0" );
        // Member not in empty set
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank([Store].[USA].[WA], {})", "0" );
        // Null member not in set returns null.
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank([Store].Parent, "
                + "{[Store].[USA].[OR],"
                + " [Store].[USA].[CA],"
                + " [Store].[USA]})", "" );
        // Null member in empty set. (MSAS returns an error "Formula error -
        // dimension count is not valid - in the Rank function" but I think
        // null is the correct behavior.)
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank([Gender].Parent, {})", "" );
        // Member occurs twice in set -- pick first
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank([Store].[USA].[WA], \n"
                + "{[Store].[USA].[WA],"
                + " [Store].[USA].[CA],"
                + " [Store].[USA],"
                + " [Store].[USA].[WA]})", "1" );
        // Tuple not in set
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank(([Gender].[F], [Marital Status].[M]), \n"
                + "{([Gender].[F], [Marital Status].[S]),\n"
                + " ([Gender].[M], [Marital Status].[S]),\n"
                + " ([Gender].[M], [Marital Status].[M])})", "0" );
        // Tuple in set
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank(([Gender].[F], [Marital Status].[M]), \n"
                + "{([Gender].[F], [Marital Status].[S]),\n"
                + " ([Gender].[M], [Marital Status].[S]),\n"
                + " ([Gender].[F], [Marital Status].[M])})", "3" );
        // Tuple not in empty set
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank(([Gender].[F], [Marital Status].[M]), \n" + "{})", "0" );
        // Partially null tuple in set, returns null
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank(([Gender].[F], [Marital Status].Parent), \n"
                + "{([Gender].[F], [Marital Status].[S]),\n"
                + " ([Gender].[M], [Marital Status].[S]),\n"
                + " ([Gender].[F], [Marital Status].[M])})", "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRankWithExpr(Context context) {
        // Note that [Good] and [Top Measure] have the same [Unit Sales]
        // value (5), but [Good] ranks 1 and [Top Measure] ranks 2. Even though
        // they are sorted descending on unit sales, they remain in their
        // natural order (member name) because MDX sorts are stable.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Measures].[Sibling Rank] as ' Rank([Product].CurrentMember, [Product].CurrentMember.Siblings) '\n"
                + "  member [Measures].[Sales Rank] as ' Rank([Product].CurrentMember, Order([Product].Parent.Children, "
                + "[Measures].[Unit Sales], DESC)) '\n"
                + "  member [Measures].[Sales Rank2] as ' Rank([Product].CurrentMember, [Product].Parent.Children, [Measures]"
                + ".[Unit Sales]) '\n"
                + "select {[Measures].[Unit Sales], [Measures].[Sales Rank], [Measures].[Sales Rank2]} on columns,\n"
                + " {[Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].children} on rows\n"
                + "from [Sales]\n"
                + "WHERE ([Store].[USA].[OR].[Portland].[Store 11], [Time].[1997].[Q2].[6])",
            "Axis #0:\n"
                + "{[Store].[USA].[OR].[Portland].[Store 11], [Time].[1997].[Q2].[6]}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "{[Measures].[Sales Rank]}\n"
                + "{[Measures].[Sales Rank2]}\n"
                + "Axis #2:\n"
                + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]}\n"
                + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Pearl]}\n"
                + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth]}\n"
                + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Top Measure]}\n"
                + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Walrus]}\n"
                + "Row #0: 5\n"
                + "Row #0: 1\n"
                + "Row #0: 1\n"
                + "Row #1: \n"
                + "Row #1: 5\n"
                + "Row #1: 5\n"
                + "Row #2: 3\n"
                + "Row #2: 3\n"
                + "Row #2: 3\n"
                + "Row #3: 5\n"
                + "Row #3: 2\n"
                + "Row #3: 1\n"
                + "Row #4: 3\n"
                + "Row #4: 4\n"
                + "Row #4: 3\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRankMembersWithTiedExpr(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with "
                + " Set [Beers] as {[Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].children} "
                + "  member [Measures].[Sales Rank] as ' Rank([Product].CurrentMember, [Beers], [Measures].[Unit Sales]) '\n"
                + "select {[Measures].[Unit Sales], [Measures].[Sales Rank]} on columns,\n"
                + " Generate([Beers], {[Product].CurrentMember}) on rows\n"
                + "from [Sales]\n"
                + "WHERE ([Store].[USA].[OR].[Portland].[Store 11], [Time].[1997].[Q2].[6])",
            "Axis #0:\n"
                + "{[Store].[USA].[OR].[Portland].[Store 11], [Time].[1997].[Q2].[6]}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "{[Measures].[Sales Rank]}\n"
                + "Axis #2:\n"
                + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]}\n"
                + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Pearl]}\n"
                + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth]}\n"
                + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Top Measure]}\n"
                + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Walrus]}\n"
                + "Row #0: 5\n"
                + "Row #0: 1\n"
                + "Row #1: \n"
                + "Row #1: 5\n"
                + "Row #2: 3\n"
                + "Row #2: 3\n"
                + "Row #3: 5\n"
                + "Row #3: 1\n"
                + "Row #4: 3\n"
                + "Row #4: 3\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRankTuplesWithTiedExpr(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with "
                + " Set [Beers for Store] as 'NonEmptyCrossJoin("
                + "[Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].children, "
                + "{[Store].[USA].[OR].[Portland].[Store 11]})' "
                + "  member [Measures].[Sales Rank] as ' Rank(([Product].CurrentMember,[Store].CurrentMember), [Beers for "
                + "Store], [Measures].[Unit Sales]) '\n"
                + "select {[Measures].[Unit Sales], [Measures].[Sales Rank]} on columns,\n"
                + " Generate([Beers for Store], {([Product].CurrentMember, [Store].CurrentMember)}) on rows\n"
                + "from [Sales]\n"
                + "WHERE ([Time].[1997].[Q2].[6])",
            "Axis #0:\n"
                + "{[Time].[1997].[Q2].[6]}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "{[Measures].[Sales Rank]}\n"
                + "Axis #2:\n"
                + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good], [Store].[USA].[OR].[Portland]"
                + ".[Store 11]}\n"
                + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth], [Store].[USA].[OR]"
                + ".[Portland].[Store 11]}\n"
                + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Top Measure], [Store].[USA].[OR]"
                + ".[Portland].[Store 11]}\n"
                + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Walrus], [Store].[USA].[OR].[Portland]"
                + ".[Store 11]}\n"
                + "Row #0: 5\n"
                + "Row #0: 1\n"
                + "Row #1: 3\n"
                + "Row #1: 3\n"
                + "Row #2: 5\n"
                + "Row #2: 1\n"
                + "Row #3: 3\n"
                + "Row #3: 3\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRankWithExpr2(Context context) {
        // Data: Unit Sales
        // All gender 266,733
        // F          131,558
        // M          135,215
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank([Gender].[All Gender],"
                + " {[Gender].Members},"
                + " [Measures].[Unit Sales])", "1" );
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank([Gender].[F],"
                + " {[Gender].Members},"
                + " [Measures].[Unit Sales])", "3" );
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank([Gender].[M],"
                + " {[Gender].Members},"
                + " [Measures].[Unit Sales])", "2" );
        // Null member. Expression evaluates to null, therefore value does
        // not appear in the list of values, therefore the rank is null.
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank([Gender].[All Gender].Parent,"
                + " {[Gender].Members},"
                + " [Measures].[Unit Sales])", "" );
        // Empty set. Value would appear after all elements in the empty set,
        // therefore rank is 1.
        // Note that SSAS gives error 'The first argument to the Rank function,
        // a tuple expression, should reference the same hierachies as the
        // second argument, a set expression'. I think that's because it can't
        // deduce a type for '{}'. SSAS's problem, not Mondrian's. :)
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank([Gender].[M],"
                + " {},"
                + " [Measures].[Unit Sales])",
            "1" );
        // As above, but SSAS can type-check this.
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank([Gender].[M],"
                + " Filter(Gender.Members, 1 = 0),"
                + " [Measures].[Unit Sales])",
            "1" );
        // Member is not in set
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank([Gender].[M]," + " {[Gender].[All Gender], [Gender].[F]})",
            "0" );
        // Even though M is not in the set, its value lies between [All Gender]
        // and [F].
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank([Gender].[M],"
                + " {[Gender].[All Gender], [Gender].[F]},"
                + " [Measures].[Unit Sales])", "2" );
        // Expr evaluates to null for some values of set.
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank([Product].[Non-Consumable].[Household],"
                + " {[Product].[Food], [Product].[All Products], [Product].[Drink].[Dairy]},"
                + " [Product].CurrentMember.Parent)", "2" );
        // Expr evaluates to null for all values in the set.
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Rank([Gender].[M],"
                + " {[Gender].[All Gender], [Gender].[F]},"
                + " [Marital Status].[All Marital Status].Parent)", "1" );
    }

    /**
     * Tests the 3-arg version of the RANK function with a value which returns null within a set of nulls.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRankWithNulls(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Measures].[X] as "
                + "'iif([Measures].[Store Sales]=777,"
                + "[Measures].[Store Sales],Null)'\n"
                + "member [Measures].[Y] as 'Rank([Gender].[M],"
                + "{[Measures].[X],[Measures].[X],[Measures].[X]},"
                + " [Marital Status].[All Marital Status].Parent)'"
                + "select {[Measures].[Y]} on columns from Sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Y]}\n"
                + "Row #0: 1\n" );
    }

    /**
     * Tests a RANK function which is so large that we need to use caching in order to execute it efficiently.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRankHuge(Context context) {
        // If caching is disabled, don't even try -- it will take too long.
        if ( !SystemWideProperties.instance().EnableExpCache ) {
            return;
        }

        checkRankHuge(context.getConnectionWithDefaultRole(),
            "WITH \n"
                + "  MEMBER [Measures].[Rank among products] \n"
                + "    AS ' Rank([Product].CurrentMember, "
                + "            Order([Product].members, "
                + "            [Measures].[Unit Sales], BDESC)) '\n"
                + "SELECT CrossJoin(\n"
                + "  [Gender].members,\n"
                + "  {[Measures].[Unit Sales],\n"
                + "   [Measures].[Rank among products]}) ON COLUMNS,\n"
                // + "  {[Product], [Product].[All Products].[Non-Consumable].
                // [Periodicals].[Magazines].[Sports Magazines].[Robust].
                // [Robust Monthly Sports Magazine]} ON ROWS\n"
                + "  {[Product].members} ON ROWS\n"
                + "FROM [Sales]",
            false );
    }

    /**
     * As {@link #testRankHuge()}, but for the 3-argument form of the
     * <code>RANK</code> function.
     *
     * <p>Disabled by jhyde, 2006/2/14. Bug 1431316 logged.
     */
    @Disabled //disabled for CI build
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void _testRank3Huge(Context context) {
        // If caching is disabled, don't even try -- it will take too long.
        if ( !SystemWideProperties.instance().EnableExpCache ) {
            return;
        }

        checkRankHuge(context.getConnectionWithDefaultRole(),
            "WITH \n"
                + "  MEMBER [Measures].[Rank among products] \n"
                + "    AS ' Rank([Product].CurrentMember, [Product].members, [Measures].[Unit Sales]) '\n"
                + "SELECT CrossJoin(\n"
                + "  [Gender].members,\n"
                + "  {[Measures].[Unit Sales],\n"
                + "   [Measures].[Rank among products]}) ON COLUMNS,\n"
                + "  {[Product],"
                + "   [Product].[All Products].[Non-Consumable].[Periodicals]"
                + ".[Magazines].[Sports Magazines].[Robust]"
                + ".[Robust Monthly Sports Magazine]} ON ROWS\n"
                // + "  {[Product].members} ON ROWS\n"
                + "FROM [Sales]",
            true );
    }

    private void checkRankHuge(Connection connection, String query, boolean rank3 ) {
        final Result result = executeQuery(connection, query );
        final Axis[] axes = result.getAxes();
        final Axis rowsAxis = axes[ 1 ];
        final int rowCount = rowsAxis.getPositions().size();
        assertEquals( 2256, rowCount );
        // [All Products], [All Gender], [Rank]
        Cell cell = result.getCell( new int[] { 1, 0 } );
        assertEquals( "1", cell.getFormattedValue() );
        // [Robust Monthly Sports Magazine]
        Member member = rowsAxis.getPositions().get( rowCount - 1 ).get( 0 );
        assertEquals( "Robust Monthly Sports Magazine", member.getName() );
        // [Robust Monthly Sports Magazine], [All Gender], [Rank]
        cell = result.getCell( new int[] { 0, rowCount - 1 } );
        assertEquals( "152", cell.getFormattedValue() );
        cell = result.getCell( new int[] { 1, rowCount - 1 } );
        assertEquals( rank3 ? "1,854" : "1,871", cell.getFormattedValue() );
        // [Robust Monthly Sports Magazine], [Gender].[F], [Rank]
        cell = result.getCell( new int[] { 2, rowCount - 1 } );
        assertEquals( "90", cell.getFormattedValue() );
        cell = result.getCell( new int[] { 3, rowCount - 1 } );
        assertEquals( rank3 ? "1,119" : "1,150", cell.getFormattedValue() );
        // [Robust Monthly Sports Magazine], [Gender].[M], [Rank]
        cell = result.getCell( new int[] { 4, rowCount - 1 } );
        assertEquals( "62", cell.getFormattedValue() );
        cell = result.getCell( new int[] { 5, rowCount - 1 } );
        assertEquals( rank3 ? "2,131" : "2,147", cell.getFormattedValue() );
    }

}
