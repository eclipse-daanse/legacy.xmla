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
package org.eclipse.daanse.olap.function.def.crossjoin;

import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertQueryThrows;
import static org.opencube.junit5.TestUtil.hierarchyName;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class CrossJoinFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinNested(Context context) {
        assertAxisReturns(context.getConnection(),
            "  CrossJoin(\n"
                + "    CrossJoin(\n"
                + "      [Gender].members,\n"
                + "      [Marital Status].members),\n"
                + "   {[Store], [Store].children})",

            "{[Gender].[All Gender], [Marital Status].[All Marital Status], [Store].[All Stores]}\n"
                + "{[Gender].[All Gender], [Marital Status].[All Marital Status], [Store].[Canada]}\n"
                + "{[Gender].[All Gender], [Marital Status].[All Marital Status], [Store].[Mexico]}\n"
                + "{[Gender].[All Gender], [Marital Status].[All Marital Status], [Store].[USA]}\n"
                + "{[Gender].[All Gender], [Marital Status].[M], [Store].[All Stores]}\n"
                + "{[Gender].[All Gender], [Marital Status].[M], [Store].[Canada]}\n"
                + "{[Gender].[All Gender], [Marital Status].[M], [Store].[Mexico]}\n"
                + "{[Gender].[All Gender], [Marital Status].[M], [Store].[USA]}\n"
                + "{[Gender].[All Gender], [Marital Status].[S], [Store].[All Stores]}\n"
                + "{[Gender].[All Gender], [Marital Status].[S], [Store].[Canada]}\n"
                + "{[Gender].[All Gender], [Marital Status].[S], [Store].[Mexico]}\n"
                + "{[Gender].[All Gender], [Marital Status].[S], [Store].[USA]}\n"
                + "{[Gender].[F], [Marital Status].[All Marital Status], [Store].[All Stores]}\n"
                + "{[Gender].[F], [Marital Status].[All Marital Status], [Store].[Canada]}\n"
                + "{[Gender].[F], [Marital Status].[All Marital Status], [Store].[Mexico]}\n"
                + "{[Gender].[F], [Marital Status].[All Marital Status], [Store].[USA]}\n"
                + "{[Gender].[F], [Marital Status].[M], [Store].[All Stores]}\n"
                + "{[Gender].[F], [Marital Status].[M], [Store].[Canada]}\n"
                + "{[Gender].[F], [Marital Status].[M], [Store].[Mexico]}\n"
                + "{[Gender].[F], [Marital Status].[M], [Store].[USA]}\n"
                + "{[Gender].[F], [Marital Status].[S], [Store].[All Stores]}\n"
                + "{[Gender].[F], [Marital Status].[S], [Store].[Canada]}\n"
                + "{[Gender].[F], [Marital Status].[S], [Store].[Mexico]}\n"
                + "{[Gender].[F], [Marital Status].[S], [Store].[USA]}\n"
                + "{[Gender].[M], [Marital Status].[All Marital Status], [Store].[All Stores]}\n"
                + "{[Gender].[M], [Marital Status].[All Marital Status], [Store].[Canada]}\n"
                + "{[Gender].[M], [Marital Status].[All Marital Status], [Store].[Mexico]}\n"
                + "{[Gender].[M], [Marital Status].[All Marital Status], [Store].[USA]}\n"
                + "{[Gender].[M], [Marital Status].[M], [Store].[All Stores]}\n"
                + "{[Gender].[M], [Marital Status].[M], [Store].[Canada]}\n"
                + "{[Gender].[M], [Marital Status].[M], [Store].[Mexico]}\n"
                + "{[Gender].[M], [Marital Status].[M], [Store].[USA]}\n"
                + "{[Gender].[M], [Marital Status].[S], [Store].[All Stores]}\n"
                + "{[Gender].[M], [Marital Status].[S], [Store].[Canada]}\n"
                + "{[Gender].[M], [Marital Status].[S], [Store].[Mexico]}\n"
                + "{[Gender].[M], [Marital Status].[S], [Store].[USA]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinSingletonTuples(Context context) {
        assertAxisReturns(context.getConnection(),
            "CrossJoin({([Gender].[M])}, {([Marital Status].[S])})",
            "{[Gender].[M], [Marital Status].[S]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinSingletonTuplesNested(Context context) {
        assertAxisReturns(context.getConnection(),
            "CrossJoin({([Gender].[M])}, CrossJoin({([Marital Status].[S])}, [Store].children))",
            "{[Gender].[M], [Marital Status].[S], [Store].[Canada]}\n"
                + "{[Gender].[M], [Marital Status].[S], [Store].[Mexico]}\n"
                + "{[Gender].[M], [Marital Status].[S], [Store].[USA]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinAsterisk(Context context) {
        assertAxisReturns(context.getConnection(),
            "{[Gender].[M]} * {[Marital Status].[S]}",
            "{[Gender].[M], [Marital Status].[S]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinAsteriskTuple(Context context) {
        assertQueryReturns(context.getConnection(),
            "select {[Measures].[Unit Sales]} ON COLUMNS, "
                + "NON EMPTY [Store].[All Stores] "
                + " * ([Product].[All Products], [Gender]) "
                + " * [Customers].[All Customers] ON ROWS "
                + "from [Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "Axis #2:\n"
                + "{[Store].[All Stores], [Product].[All Products], [Gender].[All Gender], [Customers].[All Customers]}\n"
                + "Row #0: 266,773\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinAsteriskAssoc(Context context) {
        assertAxisReturns(context.getConnection(),
            "Order({[Gender].Children} * {[Marital Status].Children} * {[Time].[1997].[Q2].Children},"
                + "[Measures].[Unit Sales])",
            "{[Gender].[F], [Marital Status].[M], [Time].[1997].[Q2].[4]}\n"
                + "{[Gender].[F], [Marital Status].[M], [Time].[1997].[Q2].[6]}\n"
                + "{[Gender].[F], [Marital Status].[M], [Time].[1997].[Q2].[5]}\n"
                + "{[Gender].[F], [Marital Status].[S], [Time].[1997].[Q2].[4]}\n"
                + "{[Gender].[F], [Marital Status].[S], [Time].[1997].[Q2].[5]}\n"
                + "{[Gender].[F], [Marital Status].[S], [Time].[1997].[Q2].[6]}\n"
                + "{[Gender].[M], [Marital Status].[M], [Time].[1997].[Q2].[4]}\n"
                + "{[Gender].[M], [Marital Status].[M], [Time].[1997].[Q2].[5]}\n"
                + "{[Gender].[M], [Marital Status].[M], [Time].[1997].[Q2].[6]}\n"
                + "{[Gender].[M], [Marital Status].[S], [Time].[1997].[Q2].[6]}\n"
                + "{[Gender].[M], [Marital Status].[S], [Time].[1997].[Q2].[4]}\n"
                + "{[Gender].[M], [Marital Status].[S], [Time].[1997].[Q2].[5]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinAsteriskInsideBraces(Context context) {
        assertAxisReturns(context.getConnection(),
            "{[Gender].[M] * [Marital Status].[S] * [Time].[1997].[Q2].Children}",
            "{[Gender].[M], [Marital Status].[S], [Time].[1997].[Q2].[4]}\n"
                + "{[Gender].[M], [Marital Status].[S], [Time].[1997].[Q2].[5]}\n"
                + "{[Gender].[M], [Marital Status].[S], [Time].[1997].[Q2].[6]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossJoinAsteriskQuery(Context context) {
        assertQueryReturns(context.getConnection(),
            "SELECT {[Measures].members * [1997].children} ON COLUMNS,\n"
                + " {[Store].[USA].children * [Position].[All Position].children} DIMENSION PROPERTIES [Store].[Store SQFT] "
                + "ON ROWS\n"
                + "FROM [HR]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Org Salary], [Time].[1997].[Q1]}\n"
                + "{[Measures].[Org Salary], [Time].[1997].[Q2]}\n"
                + "{[Measures].[Org Salary], [Time].[1997].[Q3]}\n"
                + "{[Measures].[Org Salary], [Time].[1997].[Q4]}\n"
                + "{[Measures].[Count], [Time].[1997].[Q1]}\n"
                + "{[Measures].[Count], [Time].[1997].[Q2]}\n"
                + "{[Measures].[Count], [Time].[1997].[Q3]}\n"
                + "{[Measures].[Count], [Time].[1997].[Q4]}\n"
                + "{[Measures].[Number of Employees], [Time].[1997].[Q1]}\n"
                + "{[Measures].[Number of Employees], [Time].[1997].[Q2]}\n"
                + "{[Measures].[Number of Employees], [Time].[1997].[Q3]}\n"
                + "{[Measures].[Number of Employees], [Time].[1997].[Q4]}\n"
                + "Axis #2:\n"
                + "{[Store].[USA].[CA], [Position].[Middle Management]}\n"
                + "{[Store].[USA].[CA], [Position].[Senior Management]}\n"
                + "{[Store].[USA].[CA], [Position].[Store Full Time Staf]}\n"
                + "{[Store].[USA].[CA], [Position].[Store Management]}\n"
                + "{[Store].[USA].[CA], [Position].[Store Temp Staff]}\n"
                + "{[Store].[USA].[OR], [Position].[Middle Management]}\n"
                + "{[Store].[USA].[OR], [Position].[Senior Management]}\n"
                + "{[Store].[USA].[OR], [Position].[Store Full Time Staf]}\n"
                + "{[Store].[USA].[OR], [Position].[Store Management]}\n"
                + "{[Store].[USA].[OR], [Position].[Store Temp Staff]}\n"
                + "{[Store].[USA].[WA], [Position].[Middle Management]}\n"
                + "{[Store].[USA].[WA], [Position].[Senior Management]}\n"
                + "{[Store].[USA].[WA], [Position].[Store Full Time Staf]}\n"
                + "{[Store].[USA].[WA], [Position].[Store Management]}\n"
                + "{[Store].[USA].[WA], [Position].[Store Temp Staff]}\n"
                + "Row #0: $275.40\n"
                + "Row #0: $275.40\n"
                + "Row #0: $275.40\n"
                + "Row #0: $275.40\n"
                + "Row #0: 27\n"
                + "Row #0: 27\n"
                + "Row #0: 27\n"
                + "Row #0: 27\n"
                + "Row #0: 9\n"
                + "Row #0: 9\n"
                + "Row #0: 9\n"
                + "Row #0: 9\n"
                + "Row #1: $837.00\n"
                + "Row #1: $837.00\n"
                + "Row #1: $837.00\n"
                + "Row #1: $837.00\n"
                + "Row #1: 24\n"
                + "Row #1: 24\n"
                + "Row #1: 24\n"
                + "Row #1: 24\n"
                + "Row #1: 8\n"
                + "Row #1: 8\n"
                + "Row #1: 8\n"
                + "Row #1: 8\n"
                + "Row #2: $1,728.45\n"
                + "Row #2: $1,727.02\n"
                + "Row #2: $1,727.72\n"
                + "Row #2: $1,726.55\n"
                + "Row #2: 357\n"
                + "Row #2: 357\n"
                + "Row #2: 357\n"
                + "Row #2: 357\n"
                + "Row #2: 119\n"
                + "Row #2: 119\n"
                + "Row #2: 119\n"
                + "Row #2: 119\n"
                + "Row #3: $473.04\n"
                + "Row #3: $473.04\n"
                + "Row #3: $473.04\n"
                + "Row #3: $473.04\n"
                + "Row #3: 51\n"
                + "Row #3: 51\n"
                + "Row #3: 51\n"
                + "Row #3: 51\n"
                + "Row #3: 17\n"
                + "Row #3: 17\n"
                + "Row #3: 17\n"
                + "Row #3: 17\n"
                + "Row #4: $401.35\n"
                + "Row #4: $405.73\n"
                + "Row #4: $400.61\n"
                + "Row #4: $402.31\n"
                + "Row #4: 120\n"
                + "Row #4: 120\n"
                + "Row #4: 120\n"
                + "Row #4: 120\n"
                + "Row #4: 40\n"
                + "Row #4: 40\n"
                + "Row #4: 40\n"
                + "Row #4: 40\n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #7: $1,343.62\n"
                + "Row #7: $1,342.61\n"
                + "Row #7: $1,342.57\n"
                + "Row #7: $1,343.65\n"
                + "Row #7: 279\n"
                + "Row #7: 279\n"
                + "Row #7: 279\n"
                + "Row #7: 279\n"
                + "Row #7: 93\n"
                + "Row #7: 93\n"
                + "Row #7: 93\n"
                + "Row #7: 93\n"
                + "Row #8: $286.74\n"
                + "Row #8: $286.74\n"
                + "Row #8: $286.74\n"
                + "Row #8: $286.74\n"
                + "Row #8: 30\n"
                + "Row #8: 30\n"
                + "Row #8: 30\n"
                + "Row #8: 30\n"
                + "Row #8: 10\n"
                + "Row #8: 10\n"
                + "Row #8: 10\n"
                + "Row #8: 10\n"
                + "Row #9: $333.20\n"
                + "Row #9: $332.65\n"
                + "Row #9: $331.28\n"
                + "Row #9: $332.43\n"
                + "Row #9: 99\n"
                + "Row #9: 99\n"
                + "Row #9: 99\n"
                + "Row #9: 99\n"
                + "Row #9: 33\n"
                + "Row #9: 33\n"
                + "Row #9: 33\n"
                + "Row #9: 33\n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #12: $2,768.60\n"
                + "Row #12: $2,769.18\n"
                + "Row #12: $2,766.78\n"
                + "Row #12: $2,769.50\n"
                + "Row #12: 579\n"
                + "Row #12: 579\n"
                + "Row #12: 579\n"
                + "Row #12: 579\n"
                + "Row #12: 193\n"
                + "Row #12: 193\n"
                + "Row #12: 193\n"
                + "Row #12: 193\n"
                + "Row #13: $736.29\n"
                + "Row #13: $736.29\n"
                + "Row #13: $736.29\n"
                + "Row #13: $736.29\n"
                + "Row #13: 81\n"
                + "Row #13: 81\n"
                + "Row #13: 81\n"
                + "Row #13: 81\n"
                + "Row #13: 27\n"
                + "Row #13: 27\n"
                + "Row #13: 27\n"
                + "Row #13: 27\n"
                + "Row #14: $674.70\n"
                + "Row #14: $674.54\n"
                + "Row #14: $676.26\n"
                + "Row #14: $676.48\n"
                + "Row #14: 201\n"
                + "Row #14: 201\n"
                + "Row #14: 201\n"
                + "Row #14: 201\n"
                + "Row #14: 67\n"
                + "Row #14: 67\n"
                + "Row #14: 67\n"
                + "Row #14: 67\n" );
    }

    /**
     * Testcase for bug 1889745, "StackOverflowError while resolving crossjoin". The problem occurs when a calculated
     * member that references itself is referenced in a crossjoin.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinResolve(Context context) {
        assertQueryReturns(context.getConnection(),
            "with\n"
                + "member [Measures].[Filtered Unit Sales] as\n"
                + " 'IIf((([Measures].[Unit Sales] > 50000.0)\n"
                + "      OR ([Product].CurrentMember.Level.UniqueName <>\n"
                + "          \"[Product].[Product Family]\")),\n"
                + "      IIf(((Count([Product].CurrentMember.Children) = 0.0)),\n"
                + "          [Measures].[Unit Sales],\n"
                + "          Sum([Product].CurrentMember.Children,\n"
                + "              [Measures].[Filtered Unit Sales])),\n"
                + "      NULL)'\n"
                + "select NON EMPTY {crossjoin({[Measures].[Filtered Unit Sales]},\n"
                + "{[Gender].[M], [Gender].[F]})} ON COLUMNS,\n"
                + "NON EMPTY {[Product].[All Products]} ON ROWS\n"
                + "from [Sales]\n"
                + "where [Time].[1997]",
            "Axis #0:\n"
                + "{[Time].[1997]}\n"
                + "Axis #1:\n"
                + "{[Measures].[Filtered Unit Sales], [Gender].[M]}\n"
                + "{[Measures].[Filtered Unit Sales], [Gender].[F]}\n"
                + "Axis #2:\n"
                + "{[Product].[All Products]}\n"
                + "Row #0: 97,126\n"
                + "Row #0: 94,814\n" );
    }

    /**
     * Test case for bug 1911832, "Exception converting immutable list to array in JDK 1.5".
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinOrder(Context context) {
        assertQueryReturns(context.getConnection(),
            "WITH\n"
                + "\n"
                + "SET [S1] AS 'CROSSJOIN({[Time].[1997]}, {[Gender].[Gender].MEMBERS})'\n"
                + "\n"
                + "SELECT CROSSJOIN(ORDER([S1], [Measures].[Unit Sales], BDESC),\n"
                + "{[Measures].[Unit Sales]}) ON AXIS(0)\n"
                + "FROM [Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Time].[1997], [Gender].[M], [Measures].[Unit Sales]}\n"
                + "{[Time].[1997], [Gender].[F], [Measures].[Unit Sales]}\n"
                + "Row #0: 135,215\n"
                + "Row #0: 131,558\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinDupHierarchyFails(Context context) {
        assertQueryThrows(context.getConnection(),
            "select [Measures].[Unit Sales] ON COLUMNS,\n"
                + " CrossJoin({[Time].[Quarter].[Q1]}, {[Time].[Month].[5]}) ON ROWS\n"
                + "from [Sales]",
            "Tuple contains more than one member of hierarchy '[Time]'." );

        // now with Item, for kicks
        assertQueryThrows(context.getConnection(),
            "select [Measures].[Unit Sales] ON COLUMNS,\n"
                + " CrossJoin({[Time].[Quarter].[Q1]}, {[Time].[Month].[5]}).Item(0) ON ROWS\n"
                + "from [Sales]",
            "Tuple contains more than one member of hierarchy '[Time]'." );

        // same query using explicit tuple
        assertQueryThrows(context.getConnection(),
            "select [Measures].[Unit Sales] ON COLUMNS,\n"
                + " ([Time].[Quarter].[Q1], [Time].[Month].[5]) ON ROWS\n"
                + "from [Sales]",
            "Tuple contains more than one member of hierarchy '[Time]'." );
    }

    /**
     *
     * Not an error.
     */
    //* Tests cases of different hierarchies in the same dimension. (Compare to {@link #testCrossjoinDupHierarchyFails()}).
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinDupDimensionOk(Context context) {
        final String expectedResult =
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "Axis #2:\n"
                + "{[Time].[1997].[Q1], [Time.Weekly].[1997].[10]}\n"
                + "Row #0: 4,395\n";
        final String timeWeekly = hierarchyName( "Time", "Weekly" );
        assertQueryReturns(context.getConnection(),
            "select [Measures].[Unit Sales] ON COLUMNS,\n"
                + " CrossJoin({[Time].[Quarter].[Q1]}, {"
                + timeWeekly + ".[1997].[10]}) ON ROWS\n"
                + "from [Sales]",
            expectedResult );

        // now with Item, for kicks
        assertQueryReturns(context.getConnection(),
            "select [Measures].[Unit Sales] ON COLUMNS,\n"
                + " CrossJoin({[Time].[Quarter].[Q1]}, {"
                + timeWeekly + ".[1997].[10]}).Item(0) ON ROWS\n"
                + "from [Sales]",
            expectedResult );

        // same query using explicit tuple
        assertQueryReturns(context.getConnection(),
            "select [Measures].[Unit Sales] ON COLUMNS,\n"
                + " ([Time].[Quarter].[Q1], "
                + timeWeekly + ".[1997].[10]) ON ROWS\n"
                + "from [Sales]",
            expectedResult );
    }

}
