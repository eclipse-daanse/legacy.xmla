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
package org.eclipse.daanse.olap.function.def.set.range;

import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertAxisThrows;
import static org.opencube.junit5.TestUtil.assertQueryReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class RangeFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRange(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "[Time].[1997].[Q1].[2] : [Time].[1997].[Q2].[5]",
            "[Time].[Time].[1997].[Q1].[2]\n"
                + "[Time].[Time].[1997].[Q1].[3]\n"
                + "[Time].[Time].[1997].[Q2].[4]\n"
                + "[Time].[Time].[1997].[Q2].[5]" ); // not parents

        // testcase for bug XXXXX: braces required
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with set [Set1] as '[Product].[Drink]:[Product].[Food]' \n"
                + "\n"
                + "select [Set1] on columns, {[Measures].defaultMember} on rows \n"
                + "\n"
                + "from Sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Product].[Product].[Drink]}\n"
                + "{[Product].[Product].[Food]}\n"
                + "Axis #2:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "Row #0: 24,597\n"
                + "Row #0: 191,940\n" );
    }

    /**
     * tests that a null passed in returns an empty set in range function
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNullRange(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "[Time].[1997].[Q1].[2] : NULL", //[Time].[1997].[Q2].[5]
            "" ); // Empty Set
    }

    /**
     * tests that an exception is thrown if both parameters in a range function are null.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTwoNullRange(Context<?> context) {
        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "NULL : NULL",
            "Cannot deduce type of call to function ':'" , "Sales");
    }

    /**
     * Large dimensions use a different member reader, therefore need to be tested separately.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeLarge(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "[Customers].[USA].[CA].[San Francisco] : [Customers].[USA].[WA].[Bellingham]",
            "[Customers].[Customers].[USA].[CA].[San Francisco]\n"
                + "[Customers].[Customers].[USA].[CA].[San Gabriel]\n"
                + "[Customers].[Customers].[USA].[CA].[San Jose]\n"
                + "[Customers].[Customers].[USA].[CA].[Santa Cruz]\n"
                + "[Customers].[Customers].[USA].[CA].[Santa Monica]\n"
                + "[Customers].[Customers].[USA].[CA].[Spring Valley]\n"
                + "[Customers].[Customers].[USA].[CA].[Torrance]\n"
                + "[Customers].[Customers].[USA].[CA].[West Covina]\n"
                + "[Customers].[Customers].[USA].[CA].[Woodland Hills]\n"
                + "[Customers].[Customers].[USA].[OR].[Albany]\n"
                + "[Customers].[Customers].[USA].[OR].[Beaverton]\n"
                + "[Customers].[Customers].[USA].[OR].[Corvallis]\n"
                + "[Customers].[Customers].[USA].[OR].[Lake Oswego]\n"
                + "[Customers].[Customers].[USA].[OR].[Lebanon]\n"
                + "[Customers].[Customers].[USA].[OR].[Milwaukie]\n"
                + "[Customers].[Customers].[USA].[OR].[Oregon City]\n"
                + "[Customers].[Customers].[USA].[OR].[Portland]\n"
                + "[Customers].[Customers].[USA].[OR].[Salem]\n"
                + "[Customers].[Customers].[USA].[OR].[W. Linn]\n"
                + "[Customers].[Customers].[USA].[OR].[Woodburn]\n"
                + "[Customers].[Customers].[USA].[WA].[Anacortes]\n"
                + "[Customers].[Customers].[USA].[WA].[Ballard]\n"
                + "[Customers].[Customers].[USA].[WA].[Bellingham]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeStartEqualsEnd(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "[Time].[1997].[Q3].[7] : [Time].[1997].[Q3].[7]",
            "[Time].[Time].[1997].[Q3].[7]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeStartEqualsEndLarge(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "[Customers].[USA].[CA] : [Customers].[USA].[CA]",
            "[Customers].[Customers].[USA].[CA]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeEndBeforeStart(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "[Time].[1997].[Q3].[7] : [Time].[1997].[Q2].[5]",
            "[Time].[Time].[1997].[Q2].[5]\n"
                + "[Time].[Time].[1997].[Q2].[6]\n"
                + "[Time].[Time].[1997].[Q3].[7]" ); // same as if reversed
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeEndBeforeStartLarge(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "[Customers].[USA].[WA] : [Customers].[USA].[CA]",
            "[Customers].[Customers].[USA].[CA]\n"
                + "[Customers].[Customers].[USA].[OR]\n"
                + "[Customers].[Customers].[USA].[WA]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeBetweenDifferentLevelsIsError(Context<?> context) {
        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "[Time].[1997].[Q2] : [Time].[1997].[Q2].[5]",
            "Members must belong to the same level", "Sales" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeBoundedByAll(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "[Gender] : [Gender]",
            "[Gender].[Gender].[All Gender]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeBoundedByAllLarge(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "[Customers].DefaultMember : [Customers]",
            "[Customers].[Customers].[All Customers]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeBoundedByNull(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "[Gender].[F] : [Gender].[M].NextMember",
            "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeBoundedByNullLarge(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "[Customers].PrevMember : [Customers].[USA].[OR]",
            "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testComplexSlicerWith_Calc(Context<?> context) {
        String query =
            "with "
                + "member [Time].[Time].[H1 1997] as 'Aggregate([Time].[Time].[1997].[Q1] : [Time].[Time].[1997].[Q2])', $member_scope = \"CUBE\","
                + " MEMBER_ORDINAL = 6 "
                + "SELECT "
                + "{[Measures].[Customer Count]} ON 0, "
                + "{[Education Level].Members} ON 1 "
                + "FROM [Sales] "
                + "WHERE {[Time].[Time].[H1 1997]}";
        String expectedResult =
            "Axis #0:\n"
                + "{[Time].[Time].[H1 1997]}\n"
                + "Axis #1:\n"
                + "{[Measures].[Customer Count]}\n"
                + "Axis #2:\n"
                + "{[Education Level].[Education Level].[All Education Levels]}\n"
                + "{[Education Level].[Education Level].[Bachelors Degree]}\n"
                + "{[Education Level].[Education Level].[Graduate Degree]}\n"
                + "{[Education Level].[Education Level].[High School Degree]}\n"
                + "{[Education Level].[Education Level].[Partial College]}\n"
                + "{[Education Level].[Education Level].[Partial High School]}\n"
                + "Row #0: 4,257\n"
                + "Row #1: 1,109\n"
                + "Row #2: 240\n"
                + "Row #3: 1,237\n"
                + "Row #4: 394\n"
                + "Row #5: 1,277\n";
        assertQueryReturns(context.getConnectionWithDefaultRole(), query, expectedResult );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testComplexSlicerWith_CalcBase(Context<?> context) {
        String query =
            "with "
                + "member [Time].[Time].[H1 1997] as 'Aggregate([Time].[Time].[1997].[Q1] : [Time].[Time].[1997].[Q2])', $member_scope = \"CUBE\","
                + " MEMBER_ORDINAL = 6 "
                + "SELECT "
                + "{[Measures].[Customer Count]} ON 0, "
                + "{[Education Level].[Education Level].Members} ON 1 "
                + "FROM [Sales] "
                + "WHERE {[Time].[Time].[H1 1997],[Time].[Time].[1998].[Q1]}";
        String expectedResult =
            "Axis #0:\n"
                + "{[Time].[Time].[H1 1997]}\n"
                + "{[Time].[Time].[1998].[Q1]}\n"
                + "Axis #1:\n"
                + "{[Measures].[Customer Count]}\n"
                + "Axis #2:\n"
                + "{[Education Level].[Education Level].[All Education Levels]}\n"
                + "{[Education Level].[Education Level].[Bachelors Degree]}\n"
                + "{[Education Level].[Education Level].[Graduate Degree]}\n"
                + "{[Education Level].[Education Level].[High School Degree]}\n"
                + "{[Education Level].[Education Level].[Partial College]}\n"
                + "{[Education Level].[Education Level].[Partial High School]}\n"
                + "Row #0: 4,257\n"
                + "Row #1: 1,109\n"
                + "Row #2: 240\n"
                + "Row #3: 1,237\n"
                + "Row #4: 394\n"
                + "Row #5: 1,277\n";
        assertQueryReturns(context.getConnectionWithDefaultRole(), query, expectedResult );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testComplexSlicerWith_Calc_Calc(Context<?> context) {
        String query =
            "with "
                + "member [Time].[Time].[H1 1997] as 'Aggregate([Time].[Time].[1997].[Q1] : [Time].[Time].[1997].[Q2])', $member_scope = \"CUBE\","
                + " MEMBER_ORDINAL = 6 "
                + "member [Education Level].[Partial] as 'Aggregate([Education Level].[Education Level].[Partial College]:[Education Level].[Education Level]"
                + ".[Partial High School])', $member_scope = \"CUBE\", MEMBER_ORDINAL = 7 "
                + "SELECT "
                + "{[Measures].[Customer Count]} ON 0 "
                + "FROM [Sales] "
                + "WHERE ([Time].[Time].[H1 1997],[Education Level].[Education Level].[Partial])";
        String expectedResult =
            "Axis #0:\n"
                + "{[Time].[Time].[H1 1997], [Education Level].[Education Level].[Partial]}\n"
                + "Axis #1:\n"
                + "{[Measures].[Customer Count]}\n"
                + "Row #0: 1,671\n";
        assertQueryReturns(context.getConnectionWithDefaultRole(), query, expectedResult );
    }

}
