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
package org.eclipse.daanse.olap.function.def.nonemptycrossjoinx;

import static mondrian.olap.fun.FunctionTest.allHiersExcept;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertAxisThrows;
import static org.opencube.junit5.TestUtil.assertSetExprDependsOn;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class NonEmptyCrossJoinFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonEmptyCrossJoin(Context<?> context) {
        // NonEmptyCrossJoin needs to evaluate measures to find out whether
        // cells are empty, so it implicitly depends upon all dimensions.
        String s1 = allHiersExcept( "[Store].[Store]" );
        assertSetExprDependsOn(context.getConnectionWithDefaultRole(),
            "NonEmptyCrossJoin([Store].[USA].Children, [Gender].Children)", s1 );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "NonEmptyCrossJoin("
                + "[Customers].[All Customers].[USA].[CA].Children, "
                + "[Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good].Children)",
            "{[Customers].[Customers].[USA].[CA].[Bellflower], [Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]"
                + ".[Good Light Beer]}\n"
                + "{[Customers].[Customers].[USA].[CA].[Downey], [Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]"
                + ".[Good Imported Beer]}\n"
                + "{[Customers].[Customers].[USA].[CA].[Glendale], [Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]"
                + ".[Good Imported Beer]}\n"
                + "{[Customers].[Customers].[USA].[CA].[Glendale], [Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]"
                + ".[Good Light Beer]}\n"
                + "{[Customers].[Customers].[USA].[CA].[Grossmont], [Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]"
                + ".[Good Light Beer]}\n"
                + "{[Customers].[Customers].[USA].[CA].[Imperial Beach], [Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
                + ".[Good].[Good Light Beer]}\n"
                + "{[Customers].[Customers].[USA].[CA].[La Jolla], [Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]"
                + ".[Good Imported Beer]}\n"
                + "{[Customers].[Customers].[USA].[CA].[Lincoln Acres], [Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
                + ".[Good].[Good Imported Beer]}\n"
                + "{[Customers].[Customers].[USA].[CA].[Lincoln Acres], [Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
                + ".[Good].[Good Light Beer]}\n"
                + "{[Customers].[Customers].[USA].[CA].[Long Beach], [Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
                + ".[Good].[Good Light Beer]}\n"
                + "{[Customers].[Customers].[USA].[CA].[Los Angeles], [Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
                + ".[Good].[Good Imported Beer]}\n"
                + "{[Customers].[Customers].[USA].[CA].[Newport Beach], [Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
                + ".[Good].[Good Imported Beer]}\n"
                + "{[Customers].[Customers].[USA].[CA].[Pomona], [Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]"
                + ".[Good Imported Beer]}\n"
                + "{[Customers].[Customers].[USA].[CA].[Pomona], [Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]"
                + ".[Good Light Beer]}\n"
                + "{[Customers].[Customers].[USA].[CA].[San Gabriel], [Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
                + ".[Good].[Good Light Beer]}\n"
                + "{[Customers].[Customers].[USA].[CA].[West Covina], [Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
                + ".[Good].[Good Imported Beer]}\n"
                + "{[Customers].[Customers].[USA].[CA].[West Covina], [Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
                + ".[Good].[Good Light Beer]}\n"
                + "{[Customers].[Customers].[USA].[CA].[Woodland Hills], [Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
                + ".[Good].[Good Imported Beer]}" );

        // empty set
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "NonEmptyCrossJoin({Gender.Parent}, {Store.Parent})", "" );
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "NonEmptyCrossJoin({Store.Parent}, Gender.Children)", "" );
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales", "NonEmptyCrossJoin(Store.Members, {})", "" );

        // same dimension twice
        // todo: should throw
        if ( false ) {
            assertAxisThrows(context.getConnectionWithDefaultRole(),
                "NonEmptyCrossJoin({Store.[USA]}, {Store.[USA].[CA]})",
                "xxx", "Sales" );
        }
    }

}
