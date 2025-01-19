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
package org.eclipse.daanse.olap.function.def.exists;

import static org.opencube.junit5.TestUtil.assertQueryReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class ExistsFunDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testExistsMembersLevel2(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select exists(\n"
                + "  {[Customers].[All Customers],\n"
                + "   [Customers].[Country].Members,\n"
                + "   [Customers].[State Province].[CA],\n"
                + "   [Customers].[Canada].[BC].[Richmond]},\n"
                + "  {[Customers].[Country].[USA]})\n"
                + "on 0 from Sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Customers].[All Customers]}\n"
                + "{[Customers].[USA]}\n"
                + "{[Customers].[USA].[CA]}\n"
                + "Row #0: 266,773\n"
                + "Row #0: 266,773\n"
                + "Row #0: 74,748\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testExistsWithImplicitAllMember(Context context) {
        // the tuple in the second arg in this case should implicitly
        // contain [Customers].[All Customers], so the whole tuple list
        // from the first arg should be returned.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select non empty exists(\n"
                + "  {[Customers].[All Customers],\n"
                + "   [Customers].[All Customers].Children,\n"
                + "   [Customers].[State Province].Members},\n"
                + "  {[Product].Members})\n"
                + "on 0 from Sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Customers].[All Customers]}\n"
                + "{[Customers].[USA]}\n"
                + "{[Customers].[USA].[CA]}\n"
                + "{[Customers].[USA].[OR]}\n"
                + "{[Customers].[USA].[WA]}\n"
                + "Row #0: 266,773\n"
                + "Row #0: 266,773\n"
                + "Row #0: 74,748\n"
                + "Row #0: 67,659\n"
                + "Row #0: 124,366\n" );

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select exists( "
                + "[Customers].[USA].[CA], (Store.[USA], Gender.[F])) "
                + "on 0 from sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Customers].[USA].[CA]}\n"
                + "Row #0: 74,748\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testExistsWithMultipleHierarchies(Context context) {
        // tests queries w/ a multi-hierarchy dim in either or both args.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select exists( "
                + "crossjoin( time.[1997], {[Time.Weekly].[1997].[16]}), "
                + " { Gender.F } ) on 0 from sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Time].[1997], [Time.Weekly].[1997].[16]}\n"
                + "Row #0: 3,839\n" );

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select exists( "
                + "time.[1997].[Q1], {[Time.Weekly].[1997].[4]}) "
                + " on 0 from sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Time].[1997].[Q1]}\n"
                + "Row #0: 66,291\n" );

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select exists( "
                + "{ Gender.F }, "
                + "crossjoin( time.[1997], {[Time.Weekly].[1997].[16]})  ) "
                + "on 0 from sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Gender].[F]}\n"
                + "Row #0: 131,558\n" );

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select exists( "
                + "{ time.[1998] }, "
                + "crossjoin( time.[1997], {[Time.Weekly].[1997].[16]})  ) "
                + "on 0 from sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testExistsWithDefaultNonAllMember(Context context) {
        // default mem for Time is 1997

        // non-all default on right side.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select exists( [Time].[1998].[Q1], Gender.[All Gender]) on 0 from sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n" );

        // switching to an explicit member on the hierarchy chain should return
        // 1998.Q1
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select exists( [Time].[1998].[Q1], ([Time].[1998], Gender.[All Gender])) on 0 from sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Time].[1998].[Q1]}\n"
                + "Row #0: \n" );


        // non-all default on left side
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select exists( "
                + "Gender.[All Gender], (Gender.[F], [Time].[1998].[Q1])) "
                + "on 0 from sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n" );

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select exists( "
                + "(Time.[1998].[Q1].[1], Gender.[All Gender]), (Gender.[F], [Time].[1998].[Q1])) "
                + "on 0 from sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Time].[1998].[Q1].[1], [Gender].[All Gender]}\n"
                + "Row #0: \n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testExistsMembers2Hierarchies(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select exists(\n"
                + "  {[Customers].[All Customers],\n"
                + "   [Customers].[All Customers].Children,\n"
                + "   [Customers].[State Province].Members,\n"
                + "   [Customers].[Country].[Canada],\n"
                + "   [Customers].[Country].[Mexico]},\n"
                + "  {[Customers].[Country].[USA],\n"
                + "   [Customers].[State Province].[Veracruz]})\n"
                + "on 0 from Sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Customers].[All Customers]}\n"
                + "{[Customers].[Mexico]}\n"
                + "{[Customers].[USA]}\n"
                + "{[Customers].[Mexico].[Veracruz]}\n"
                + "{[Customers].[USA].[CA]}\n"
                + "{[Customers].[USA].[OR]}\n"
                + "{[Customers].[USA].[WA]}\n"
                + "{[Customers].[Mexico]}\n"
                + "Row #0: 266,773\n"
                + "Row #0: \n"
                + "Row #0: 266,773\n"
                + "Row #0: \n"
                + "Row #0: 74,748\n"
                + "Row #0: 67,659\n"
                + "Row #0: 124,366\n"
                + "Row #0: \n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testExistsTuplesAll(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select exists(\n"
                + "  crossjoin({[Product].[All Products]},{[Customers].[All Customers]}),\n"
                + "  {[Customers].[All Customers]})\n"
                + "on 0 from Sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Product].[All Products], [Customers].[All Customers]}\n"
                + "Row #0: 266,773\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testExistsTuplesLevel2(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select exists(\n"
                + "  crossjoin({[Product].[All Products]},{[Customers].[All Customers].Children}),\n"
                + "  {[Customers].[All Customers].[USA]})\n"
                + "on 0 from Sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Product].[All Products], [Customers].[USA]}\n"
                + "Row #0: 266,773\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testExistsTuplesLevel23(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select exists(\n"
                + "  crossjoin({[Customers].[State Province].Members}, {[Product].[All Products]}),\n"
                + "  {[Customers].[All Customers].[USA]})\n"
                + "on 0 from Sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Customers].[USA].[CA], [Product].[All Products]}\n"
                + "{[Customers].[USA].[OR], [Product].[All Products]}\n"
                + "{[Customers].[USA].[WA], [Product].[All Products]}\n"
                + "Row #0: 74,748\n"
                + "Row #0: 67,659\n"
                + "Row #0: 124,366\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testExistsTuples2Dim(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select exists(\n"
                + "  crossjoin({[Customers].[State Province].Members}, {[Product].[Product Family].Members}),\n"
                + "  {([Product].[Product Department].[Dairy],[Customers].[All Customers].[USA])})\n"
                + "on 0 from Sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Customers].[USA].[CA], [Product].[Drink]}\n"
                + "{[Customers].[USA].[OR], [Product].[Drink]}\n"
                + "{[Customers].[USA].[WA], [Product].[Drink]}\n"
                + "Row #0: 7,102\n"
                + "Row #0: 6,106\n"
                + "Row #0: 11,389\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testExistsTuplesDiffDim(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select exists(\n"
                + "  crossjoin(\n"
                + "    crossjoin({[Customers].[State Province].Members},\n"
                + "              {[Time].[Year].[1997]}), \n"
                + "    {[Product].[Product Family].Members}),\n"
                + "  {([Product].[Product Department].[Dairy],\n"
                + "    [Promotions].[All Promotions], \n"
                + "    [Customers].[All Customers].[USA])})\n"
                + "on 0 from Sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Customers].[USA].[CA], [Time].[1997], [Product].[Drink]}\n"
                + "{[Customers].[USA].[OR], [Time].[1997], [Product].[Drink]}\n"
                + "{[Customers].[USA].[WA], [Time].[1997], [Product].[Drink]}\n"
                + "Row #0: 7,102\n"
                + "Row #0: 6,106\n"
                + "Row #0: 11,389\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testExistsMembersAll(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select exists(\n"
                + "  {[Customers].[All Customers],\n"
                + "   [Customers].[Country].Members,\n"
                + "   [Customers].[State Province].[CA],\n"
                + "   [Customers].[Canada].[BC].[Richmond]},\n"
                + "  {[Customers].[All Customers]})\n"
                + "on 0 from Sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Customers].[All Customers]}\n"
                + "{[Customers].[Canada]}\n"
                + "{[Customers].[Mexico]}\n"
                + "{[Customers].[USA]}\n"
                + "{[Customers].[USA].[CA]}\n"
                + "{[Customers].[Canada].[BC].[Richmond]}\n"
                + "Row #0: 266,773\n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: 266,773\n"
                + "Row #0: 74,748\n"
                + "Row #0: \n" );
    }

}
