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
    void testToggleDrillState(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "ToggleDrillState({[Customers].[USA],[Customers].[Canada]},"
                + "{[Customers].[USA],[Customers].[USA].[CA]})",
            "[Customers].[Customers].[USA]\n"
                + "[Customers].[Customers].[USA].[CA]\n"
                + "[Customers].[Customers].[USA].[OR]\n"
                + "[Customers].[Customers].[USA].[WA]\n"
                + "[Customers].[Customers].[Canada]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testToggleDrillState2(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "ToggleDrillState([Product].[Product Department].members, "
                + "{[Product].[All Products].[Food].[Snack Foods]})",
            "[Product].[Product].[Drink].[Alcoholic Beverages]\n"
                + "[Product].[Product].[Drink].[Beverages]\n"
                + "[Product].[Product].[Drink].[Dairy]\n"
                + "[Product].[Product].[Food].[Baked Goods]\n"
                + "[Product].[Product].[Food].[Baking Goods]\n"
                + "[Product].[Product].[Food].[Breakfast Foods]\n"
                + "[Product].[Product].[Food].[Canned Foods]\n"
                + "[Product].[Product].[Food].[Canned Products]\n"
                + "[Product].[Product].[Food].[Dairy]\n"
                + "[Product].[Product].[Food].[Deli]\n"
                + "[Product].[Product].[Food].[Eggs]\n"
                + "[Product].[Product].[Food].[Frozen Foods]\n"
                + "[Product].[Product].[Food].[Meat]\n"
                + "[Product].[Product].[Food].[Produce]\n"
                + "[Product].[Product].[Food].[Seafood]\n"
                + "[Product].[Product].[Food].[Snack Foods]\n"
                + "[Product].[Product].[Food].[Snack Foods].[Snack Foods]\n"
                + "[Product].[Product].[Food].[Snacks]\n"
                + "[Product].[Product].[Food].[Starchy Foods]\n"
                + "[Product].[Product].[Non-Consumable].[Carousel]\n"
                + "[Product].[Product].[Non-Consumable].[Checkout]\n"
                + "[Product].[Product].[Non-Consumable].[Health and Hygiene]\n"
                + "[Product].[Product].[Non-Consumable].[Household]\n"
                + "[Product].[Product].[Non-Consumable].[Periodicals]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testToggleDrillState3(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "ToggleDrillState("
                + "{[Time].[1997].[Q1],"
                + " [Time].[1997].[Q2],"
                + " [Time].[1997].[Q2].[4],"
                + " [Time].[1997].[Q2].[6],"
                + " [Time].[1997].[Q3]},"
                + "{[Time].[1997].[Q2]})",
            "[Time].[Time].[1997].[Q1]\n"
                + "[Time].[Time].[1997].[Q2]\n"
                + "[Time].[Time].[1997].[Q3]" );
    }

    // bug 634860
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testToggleDrillStateTuple(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "ToggleDrillState(\n"
                + "{([Store].[USA].[CA],"
                + "  [Product].[All Products].[Drink].[Alcoholic Beverages]),\n"
                + " ([Store].[USA],"
                + "  [Product].[All Products].[Drink])},\n"
                + "{[Store].[All stores].[USA].[CA]})",
            "{[Store].[Store].[USA].[CA], [Product].[Product].[Drink].[Alcoholic Beverages]}\n"
                + "{[Store].[Store].[USA].[CA].[Alameda], [Product].[Product].[Drink].[Alcoholic Beverages]}\n"
                + "{[Store].[Store].[USA].[CA].[Beverly Hills], [Product].[Product].[Drink].[Alcoholic Beverages]}\n"
                + "{[Store].[Store].[USA].[CA].[Los Angeles], [Product].[Product].[Drink].[Alcoholic Beverages]}\n"
                + "{[Store].[Store].[USA].[CA].[San Diego], [Product].[Product].[Drink].[Alcoholic Beverages]}\n"
                + "{[Store].[Store].[USA].[CA].[San Francisco], [Product].[Product].[Drink].[Alcoholic Beverages]}\n"
                + "{[Store].[Store].[USA], [Product].[Product].[Drink]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testToggleDrillStateRecursive(Context<?> context) {
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
