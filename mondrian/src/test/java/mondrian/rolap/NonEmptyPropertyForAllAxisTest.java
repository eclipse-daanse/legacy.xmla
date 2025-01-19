/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * History:
 *  This files came from the mondrian project. Some of the Flies
 *  (mostly the Tests) did not have License Header.
 *  But the Project is EPL Header. 2002-2022 Hitachi Vantara.
 *
 * Contributors:
 *   Hitachi Vantara.
 *   SmartCity Jena - initial  Java 8, Junit5
 */

package mondrian.rolap;

import static org.opencube.junit5.TestUtil.assertEqualsVerbose;
import static org.opencube.junit5.TestUtil.assertQueryReturns;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.SystemWideProperties;

/**
 * Tests the {@link SystemWideProperties#EnableNonEmptyOnAllAxis} property.
 */
class NonEmptyPropertyForAllAxisTest {

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonEmptyForAllAxesWithPropertySet(Context context) {

        SystemWideProperties.instance().EnableNonEmptyOnAllAxis = true;
        final String MDX_QUERY =
            "select {[Country].[USA].[OR].Children} on 0,"
            + " {[Promotions].Members} on 1 "
            + "from [Sales] "
            + "where [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good].[Good Light Beer]";
        final String EXPECTED_RESULT =
            "Axis #0:\n"
            + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good].[Good Light Beer]}\n"
            + "Axis #1:\n"
            + "{[Customers].[USA].[OR].[Albany]}\n"
            + "{[Customers].[USA].[OR].[Corvallis]}\n"
            + "{[Customers].[USA].[OR].[Lake Oswego]}\n"
            + "{[Customers].[USA].[OR].[Lebanon]}\n"
            + "{[Customers].[USA].[OR].[Portland]}\n"
            + "{[Customers].[USA].[OR].[Woodburn]}\n"
            + "Axis #2:\n"
            + "{[Promotions].[All Promotions]}\n"
            + "{[Promotions].[Cash Register Lottery]}\n"
            + "{[Promotions].[No Promotion]}\n"
            + "{[Promotions].[Saving Days]}\n"
            + "Row #0: 4\n"
            + "Row #0: 6\n"
            + "Row #0: 5\n"
            + "Row #0: 10\n"
            + "Row #0: 6\n"
            + "Row #0: 3\n"
            + "Row #1: \n"
            + "Row #1: 2\n"
            + "Row #1: \n"
            + "Row #1: 2\n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #2: 4\n"
            + "Row #2: 4\n"
            + "Row #2: 3\n"
            + "Row #2: 8\n"
            + "Row #2: 6\n"
            + "Row #2: 3\n"
            + "Row #3: \n"
            + "Row #3: \n"
            + "Row #3: 2\n"
            + "Row #3: \n"
            + "Row #3: \n"
            + "Row #3: \n";
        assertQueryReturns(context.getConnectionWithDefaultRole(), MDX_QUERY, EXPECTED_RESULT);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonEmptyForAllAxesWithOutPropertySet(Context context) {
        final String MDX_QUERY =
            "SELECT {customers.USA.CA.[Santa Cruz].[Brian Merlo]} on 0, "
            + "[product].[product category].members on 1 FROM [sales]";
        final String EXPECTED_RESULT =
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Customers].[USA].[CA].[Santa Cruz].[Brian Merlo]}\n"
            + "Axis #2:\n"
            + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine]}\n"
            + "{[Product].[Drink].[Beverages].[Carbonated Beverages]}\n"
            + "{[Product].[Drink].[Beverages].[Drinks]}\n"
            + "{[Product].[Drink].[Beverages].[Hot Beverages]}\n"
            + "{[Product].[Drink].[Beverages].[Pure Juice Beverages]}\n"
            + "{[Product].[Drink].[Dairy].[Dairy]}\n"
            + "{[Product].[Food].[Baked Goods].[Bread]}\n"
            + "{[Product].[Food].[Baking Goods].[Baking Goods]}\n"
            + "{[Product].[Food].[Baking Goods].[Jams and Jellies]}\n"
            + "{[Product].[Food].[Breakfast Foods].[Breakfast Foods]}\n"
            + "{[Product].[Food].[Canned Foods].[Canned Anchovies]}\n"
            + "{[Product].[Food].[Canned Foods].[Canned Clams]}\n"
            + "{[Product].[Food].[Canned Foods].[Canned Oysters]}\n"
            + "{[Product].[Food].[Canned Foods].[Canned Sardines]}\n"
            + "{[Product].[Food].[Canned Foods].[Canned Shrimp]}\n"
            + "{[Product].[Food].[Canned Foods].[Canned Soup]}\n"
            + "{[Product].[Food].[Canned Foods].[Canned Tuna]}\n"
            + "{[Product].[Food].[Canned Foods].[Vegetables]}\n"
            + "{[Product].[Food].[Canned Products].[Fruit]}\n"
            + "{[Product].[Food].[Dairy].[Dairy]}\n"
            + "{[Product].[Food].[Deli].[Meat]}\n"
            + "{[Product].[Food].[Deli].[Side Dishes]}\n"
            + "{[Product].[Food].[Eggs].[Eggs]}\n"
            + "{[Product].[Food].[Frozen Foods].[Breakfast Foods]}\n"
            + "{[Product].[Food].[Frozen Foods].[Frozen Desserts]}\n"
            + "{[Product].[Food].[Frozen Foods].[Frozen Entrees]}\n"
            + "{[Product].[Food].[Frozen Foods].[Meat]}\n"
            + "{[Product].[Food].[Frozen Foods].[Pizza]}\n"
            + "{[Product].[Food].[Frozen Foods].[Vegetables]}\n"
            + "{[Product].[Food].[Meat].[Meat]}\n"
            + "{[Product].[Food].[Produce].[Fruit]}\n"
            + "{[Product].[Food].[Produce].[Packaged Vegetables]}\n"
            + "{[Product].[Food].[Produce].[Specialty]}\n"
            + "{[Product].[Food].[Produce].[Vegetables]}\n"
            + "{[Product].[Food].[Seafood].[Seafood]}\n"
            + "{[Product].[Food].[Snack Foods].[Snack Foods]}\n"
            + "{[Product].[Food].[Snacks].[Candy]}\n"
            + "{[Product].[Food].[Starchy Foods].[Starchy Foods]}\n"
            + "{[Product].[Non-Consumable].[Carousel].[Specialty]}\n"
            + "{[Product].[Non-Consumable].[Checkout].[Hardware]}\n"
            + "{[Product].[Non-Consumable].[Checkout].[Miscellaneous]}\n"
            + "{[Product].[Non-Consumable].[Health and Hygiene].[Bathroom Products]}\n"
            + "{[Product].[Non-Consumable].[Health and Hygiene].[Cold Remedies]}\n"
            + "{[Product].[Non-Consumable].[Health and Hygiene].[Decongestants]}\n"
            + "{[Product].[Non-Consumable].[Health and Hygiene].[Hygiene]}\n"
            + "{[Product].[Non-Consumable].[Health and Hygiene].[Pain Relievers]}\n"
            + "{[Product].[Non-Consumable].[Household].[Bathroom Products]}\n"
            + "{[Product].[Non-Consumable].[Household].[Candles]}\n"
            + "{[Product].[Non-Consumable].[Household].[Cleaning Supplies]}\n"
            + "{[Product].[Non-Consumable].[Household].[Electrical]}\n"
            + "{[Product].[Non-Consumable].[Household].[Hardware]}\n"
            + "{[Product].[Non-Consumable].[Household].[Kitchen Products]}\n"
            + "{[Product].[Non-Consumable].[Household].[Paper Products]}\n"
            + "{[Product].[Non-Consumable].[Household].[Plastic Products]}\n"
            + "{[Product].[Non-Consumable].[Periodicals].[Magazines]}\n"
            + "Row #0: 2\n"
            + "Row #1: 2\n"
            + "Row #2: \n"
            + "Row #3: \n"
            + "Row #4: \n"
            + "Row #5: \n"
            + "Row #6: \n"
            + "Row #7: \n"
            + "Row #8: \n"
            + "Row #9: \n"
            + "Row #10: \n"
            + "Row #11: \n"
            + "Row #12: \n"
            + "Row #13: \n"
            + "Row #14: \n"
            + "Row #15: \n"
            + "Row #16: \n"
            + "Row #17: \n"
            + "Row #18: \n"
            + "Row #19: \n"
            + "Row #20: \n"
            + "Row #21: \n"
            + "Row #22: 1\n"
            + "Row #23: \n"
            + "Row #24: \n"
            + "Row #25: \n"
            + "Row #26: \n"
            + "Row #27: \n"
            + "Row #28: 1\n"
            + "Row #29: \n"
            + "Row #30: \n"
            + "Row #31: \n"
            + "Row #32: \n"
            + "Row #33: \n"
            + "Row #34: \n"
            + "Row #35: \n"
            + "Row #36: \n"
            + "Row #37: \n"
            + "Row #38: \n"
            + "Row #39: \n"
            + "Row #40: \n"
            + "Row #41: \n"
            + "Row #42: \n"
            + "Row #43: \n"
            + "Row #44: \n"
            + "Row #45: \n"
            + "Row #46: \n"
            + "Row #47: \n"
            + "Row #48: \n"
            + "Row #49: \n"
            + "Row #50: 1\n"
            + "Row #51: \n"
            + "Row #52: \n"
            + "Row #53: \n"
            + "Row #54: 2\n";
        assertQueryReturns(context.getConnectionWithDefaultRole(), MDX_QUERY, EXPECTED_RESULT);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSlicerAxisDoesNotGetNonEmptyApplied(Context context) {

        SystemWideProperties.instance().EnableNonEmptyOnAllAxis = true;
        String mdxQuery = "select from [Sales]\n"
            + "where [Time].[1997]\n";
        Connection connection = context.getConnectionWithDefaultRole();
        Query query = connection.parseQuery(mdxQuery);
        assertEqualsVerbose(mdxQuery, query.toString());
     }
}
