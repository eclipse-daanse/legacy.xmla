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
package org.eclipse.daanse.olap.function.def.headtail;

import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class HeadTailFunDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHead(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Head([Store].Children, 2)",
            "[Store].[Canada]\n"
                + "[Store].[Mexico]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHeadNegative(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Head([Store].Children, 2 - 3)",
            "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHeadDefault(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Head([Store].Children)",
            "[Store].[Canada]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHeadOvershoot(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Head([Store].Children, 2 + 2)",
            "[Store].[Canada]\n"
                + "[Store].[Mexico]\n"
                + "[Store].[USA]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHeadEmpty(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Head([Gender].[F].Children, 2)",
            "" );

        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Head([Gender].[F].Children)",
            "" );
    }

    /**
     * Test case for bug 2488492, "Union between calc mem and head function throws exception"
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHeadBug(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT\n"
                + "                        UNION(\n"
                + "                            {([Customers].CURRENTMEMBER)},\n"
                + "                            HEAD(\n"
                + "                                {([Customers].CURRENTMEMBER)},\n"
                + "                                IIF(\n"
                + "                                    COUNT(\n"
                + "                                        FILTER(\n"
                + "                                            DESCENDANTS(\n"
                + "                                                [Customers].CURRENTMEMBER,\n"
                + "                                                [Customers].[Country]),\n"
                + "                                            [Measures].[Unit Sales] >= 66),\n"
                + "                                        INCLUDEEMPTY)> 0,\n"
                + "                                    1,\n"
                + "                                    0)),\n"
                + "                            ALL)\n"
                + "    ON AXIS(0)\n"
                + "FROM\n"
                + "    [Sales]\n",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Customers].[All Customers]}\n"
                + "{[Customers].[All Customers]}\n"
                + "Row #0: 266,773\n"
                + "Row #0: 266,773\n" );

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH\n"
                + "    MEMBER\n"
                + "        [Customers].[COG_OQP_INT_t2]AS '1',\n"
                + "        SOLVE_ORDER = 65535\n"
                + "SELECT\n"
                + "                        UNION(\n"
                + "                            {([Customers].[COG_OQP_INT_t2])},\n"
                + "                            HEAD(\n"
                + "                                {([Customers].CURRENTMEMBER)},\n"
                + "                                IIF(\n"
                + "                                    COUNT(\n"
                + "                                        FILTER(\n"
                + "                                            DESCENDANTS(\n"
                + "                                                [Customers].CURRENTMEMBER,\n"
                + "                                                [Customers].[Country]),\n"
                + "                                            [Measures].[Unit Sales]>= 66),\n"
                + "                                        INCLUDEEMPTY)> 0,\n"
                + "                                    1,\n"
                + "                                    0)),\n"
                + "                            ALL)\n"
                + "    ON AXIS(0)\n"
                + "FROM\n"
                + "    [Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Customers].[COG_OQP_INT_t2]}\n"
                + "{[Customers].[All Customers]}\n"
                + "Row #0: 1\n"
                + "Row #0: 266,773\n" );

        // More minimal test case. Also demonstrates similar problem with Tail.
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Union(\n"
                + "  Union(\n"
                + "    Tail([Customers].[USA].[CA].Children, 2),\n"
                + "    Head([Customers].[USA].[WA].Children, 2),\n"
                + "    ALL),\n"
                + "  Tail([Customers].[USA].[OR].Children, 2),"
                + "  ALL)",
            "[Customers].[USA].[CA].[West Covina]\n"
                + "[Customers].[USA].[CA].[Woodland Hills]\n"
                + "[Customers].[USA].[WA].[Anacortes]\n"
                + "[Customers].[USA].[WA].[Ballard]\n"
                + "[Customers].[USA].[OR].[W. Linn]\n"
                + "[Customers].[USA].[OR].[Woodburn]" );
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTail(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Tail([Store].Children, 2)",
            "[Store].[Mexico]\n"
                + "[Store].[USA]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTailNegative(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Tail([Store].Children, 2 - 3)",
            "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTailDefault(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Tail([Store].Children)",
            "[Store].[USA]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTailOvershoot(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Tail([Store].Children, 2 + 2)",
            "[Store].[Canada]\n"
                + "[Store].[Mexico]\n"
                + "[Store].[USA]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTailEmpty(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Tail([Gender].[F].Children, 2)",
            "" );

        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Tail([Gender].[F].Children)",
            "" );
    }

}
