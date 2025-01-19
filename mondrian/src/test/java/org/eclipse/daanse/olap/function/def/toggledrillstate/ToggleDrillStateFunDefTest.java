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
package org.eclipse.daanse.olap.function.def.toggledrillstate;

import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertQueryThrows;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class ToggleDrillStateFunDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testToggleDrillState(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "ToggleDrillState({[Customers].[USA],[Customers].[Canada]},"
                + "{[Customers].[USA],[Customers].[USA].[CA]})",
            "[Customers].[USA]\n"
                + "[Customers].[USA].[CA]\n"
                + "[Customers].[USA].[OR]\n"
                + "[Customers].[USA].[WA]\n"
                + "[Customers].[Canada]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testToggleDrillState2(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "ToggleDrillState([Product].[Product Department].members, "
                + "{[Product].[All Products].[Food].[Snack Foods]})",
            "[Product].[Drink].[Alcoholic Beverages]\n"
                + "[Product].[Drink].[Beverages]\n"
                + "[Product].[Drink].[Dairy]\n"
                + "[Product].[Food].[Baked Goods]\n"
                + "[Product].[Food].[Baking Goods]\n"
                + "[Product].[Food].[Breakfast Foods]\n"
                + "[Product].[Food].[Canned Foods]\n"
                + "[Product].[Food].[Canned Products]\n"
                + "[Product].[Food].[Dairy]\n"
                + "[Product].[Food].[Deli]\n"
                + "[Product].[Food].[Eggs]\n"
                + "[Product].[Food].[Frozen Foods]\n"
                + "[Product].[Food].[Meat]\n"
                + "[Product].[Food].[Produce]\n"
                + "[Product].[Food].[Seafood]\n"
                + "[Product].[Food].[Snack Foods]\n"
                + "[Product].[Food].[Snack Foods].[Snack Foods]\n"
                + "[Product].[Food].[Snacks]\n"
                + "[Product].[Food].[Starchy Foods]\n"
                + "[Product].[Non-Consumable].[Carousel]\n"
                + "[Product].[Non-Consumable].[Checkout]\n"
                + "[Product].[Non-Consumable].[Health and Hygiene]\n"
                + "[Product].[Non-Consumable].[Household]\n"
                + "[Product].[Non-Consumable].[Periodicals]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testToggleDrillState3(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "ToggleDrillState("
                + "{[Time].[1997].[Q1],"
                + " [Time].[1997].[Q2],"
                + " [Time].[1997].[Q2].[4],"
                + " [Time].[1997].[Q2].[6],"
                + " [Time].[1997].[Q3]},"
                + "{[Time].[1997].[Q2]})",
            "[Time].[1997].[Q1]\n"
                + "[Time].[1997].[Q2]\n"
                + "[Time].[1997].[Q3]" );
    }

    // bug 634860
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testToggleDrillStateTuple(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "ToggleDrillState(\n"
                + "{([Store].[USA].[CA],"
                + "  [Product].[All Products].[Drink].[Alcoholic Beverages]),\n"
                + " ([Store].[USA],"
                + "  [Product].[All Products].[Drink])},\n"
                + "{[Store].[All stores].[USA].[CA]})",
            "{[Store].[USA].[CA], [Product].[Drink].[Alcoholic Beverages]}\n"
                + "{[Store].[USA].[CA].[Alameda], [Product].[Drink].[Alcoholic Beverages]}\n"
                + "{[Store].[USA].[CA].[Beverly Hills], [Product].[Drink].[Alcoholic Beverages]}\n"
                + "{[Store].[USA].[CA].[Los Angeles], [Product].[Drink].[Alcoholic Beverages]}\n"
                + "{[Store].[USA].[CA].[San Diego], [Product].[Drink].[Alcoholic Beverages]}\n"
                + "{[Store].[USA].[CA].[San Francisco], [Product].[Drink].[Alcoholic Beverages]}\n"
                + "{[Store].[USA], [Product].[Drink]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testToggleDrillStateRecursive(Context context) {
        // We expect this to fail.
        assertQueryThrows(context,
            "Select \n"
                + "    ToggleDrillState(\n"
                + "        {[Store].[USA]}, \n"
                + "        {[Store].[USA]}, recursive) on Axis(0) \n"
                + "from [Sales]\n",
            "'RECURSIVE' is not supported in ToggleDrillState." );
    }

}
