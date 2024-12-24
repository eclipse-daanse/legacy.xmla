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
    void testRange(Context context) {
        assertAxisReturns(context.getConnection(),
            "[Time].[1997].[Q1].[2] : [Time].[1997].[Q2].[5]",
            "[Time].[1997].[Q1].[2]\n"
                + "[Time].[1997].[Q1].[3]\n"
                + "[Time].[1997].[Q2].[4]\n"
                + "[Time].[1997].[Q2].[5]" ); // not parents

        // testcase for bug XXXXX: braces required
        assertQueryReturns(context.getConnection(),
            "with set [Set1] as '[Product].[Drink]:[Product].[Food]' \n"
                + "\n"
                + "select [Set1] on columns, {[Measures].defaultMember} on rows \n"
                + "\n"
                + "from Sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Product].[Drink]}\n"
                + "{[Product].[Food]}\n"
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
    void testNullRange(Context context) {
        assertAxisReturns(context.getConnection(),
            "[Time].[1997].[Q1].[2] : NULL", //[Time].[1997].[Q2].[5]
            "" ); // Empty Set
    }

    /**
     * tests that an exception is thrown if both parameters in a range function are null.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTwoNullRange(Context context) {
        assertAxisThrows(context.getConnection(),
            "NULL : NULL",
            "Mondrian Error:Failed to parse query 'select {NULL : NULL} on columns from Sales'" );
    }

    /**
     * Large dimensions use a different member reader, therefore need to be tested separately.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeLarge(Context context) {
        assertAxisReturns(context.getConnection(),
            "[Customers].[USA].[CA].[San Francisco] : [Customers].[USA].[WA].[Bellingham]",
            "[Customers].[USA].[CA].[San Francisco]\n"
                + "[Customers].[USA].[CA].[San Gabriel]\n"
                + "[Customers].[USA].[CA].[San Jose]\n"
                + "[Customers].[USA].[CA].[Santa Cruz]\n"
                + "[Customers].[USA].[CA].[Santa Monica]\n"
                + "[Customers].[USA].[CA].[Spring Valley]\n"
                + "[Customers].[USA].[CA].[Torrance]\n"
                + "[Customers].[USA].[CA].[West Covina]\n"
                + "[Customers].[USA].[CA].[Woodland Hills]\n"
                + "[Customers].[USA].[OR].[Albany]\n"
                + "[Customers].[USA].[OR].[Beaverton]\n"
                + "[Customers].[USA].[OR].[Corvallis]\n"
                + "[Customers].[USA].[OR].[Lake Oswego]\n"
                + "[Customers].[USA].[OR].[Lebanon]\n"
                + "[Customers].[USA].[OR].[Milwaukie]\n"
                + "[Customers].[USA].[OR].[Oregon City]\n"
                + "[Customers].[USA].[OR].[Portland]\n"
                + "[Customers].[USA].[OR].[Salem]\n"
                + "[Customers].[USA].[OR].[W. Linn]\n"
                + "[Customers].[USA].[OR].[Woodburn]\n"
                + "[Customers].[USA].[WA].[Anacortes]\n"
                + "[Customers].[USA].[WA].[Ballard]\n"
                + "[Customers].[USA].[WA].[Bellingham]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeStartEqualsEnd(Context context) {
        assertAxisReturns(context.getConnection(),
            "[Time].[1997].[Q3].[7] : [Time].[1997].[Q3].[7]",
            "[Time].[1997].[Q3].[7]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeStartEqualsEndLarge(Context context) {
        assertAxisReturns(context.getConnection(),
            "[Customers].[USA].[CA] : [Customers].[USA].[CA]",
            "[Customers].[USA].[CA]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeEndBeforeStart(Context context) {
        assertAxisReturns(context.getConnection(),
            "[Time].[1997].[Q3].[7] : [Time].[1997].[Q2].[5]",
            "[Time].[1997].[Q2].[5]\n"
                + "[Time].[1997].[Q2].[6]\n"
                + "[Time].[1997].[Q3].[7]" ); // same as if reversed
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeEndBeforeStartLarge(Context context) {
        assertAxisReturns(context.getConnection(),
            "[Customers].[USA].[WA] : [Customers].[USA].[CA]",
            "[Customers].[USA].[CA]\n"
                + "[Customers].[USA].[OR]\n"
                + "[Customers].[USA].[WA]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeBetweenDifferentLevelsIsError(Context context) {
        assertAxisThrows(context.getConnection(),
            "[Time].[1997].[Q2] : [Time].[1997].[Q2].[5]",
            "Members must belong to the same level" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeBoundedByAll(Context context) {
        assertAxisReturns(context.getConnection(),
            "[Gender] : [Gender]",
            "[Gender].[All Gender]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeBoundedByAllLarge(Context context) {
        assertAxisReturns(context.getConnection(),
            "[Customers].DefaultMember : [Customers]",
            "[Customers].[All Customers]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeBoundedByNull(Context context) {
        assertAxisReturns(context.getConnection(),
            "[Gender].[F] : [Gender].[M].NextMember",
            "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRangeBoundedByNullLarge(Context context) {
        assertAxisReturns(context.getConnection(),
            "[Customers].PrevMember : [Customers].[USA].[OR]",
            "" );
    }

}
