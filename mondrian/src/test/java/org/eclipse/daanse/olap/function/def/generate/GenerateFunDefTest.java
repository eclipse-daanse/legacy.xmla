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
package org.eclipse.daanse.olap.function.def.generate;

import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertSetExprDependsOn;
import static org.opencube.junit5.TestUtil.executeAxis;

import java.util.concurrent.CancellationException;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.QueryTimeoutException;
import mondrian.olap.SystemWideProperties;


class GenerateFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGenerateDepends(Context context) {
        assertSetExprDependsOn(context.getConnectionWithDefaultRole(),
            "Generate([Product].CurrentMember.Children, Crossjoin({[Product].CurrentMember}, Crossjoin([Store].[Store "
                + "State].Members, [Store Type].Members)), ALL)",
            "{[Product].[Product]}" );
        assertSetExprDependsOn(context.getConnectionWithDefaultRole(),
            "Generate([Product].[All Products].Children, Crossjoin({[Product].CurrentMember}, Crossjoin([Store].[Store "
                + "State].Members, [Store Type].Members)), ALL)",
            "{}" );
        assertSetExprDependsOn(context.getConnectionWithDefaultRole(),
            "Generate({[Store].[USA], [Store].[USA].[CA]}, {[Store].CurrentMember.Children})",
            "{}" );
        assertSetExprDependsOn(context.getConnectionWithDefaultRole(),
            "Generate({[Store].[USA], [Store].[USA].[CA]}, {[Gender].CurrentMember})",
            "{[Gender].[Gender]}" );
        assertSetExprDependsOn(context.getConnectionWithDefaultRole(),
            "Generate({[Store].[USA], [Store].[USA].[CA]}, {[Gender].[M]})",
            "{}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGenerate(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Generate({[Store].[USA], [Store].[USA].[CA]}, {[Store].CurrentMember.Children})",
            "[Store].[Store].[USA].[CA]\n"
                + "[Store].[Store].[USA].[OR]\n"
                + "[Store].[Store].[USA].[WA]\n"
                + "[Store].[Store].[USA].[CA].[Alameda]\n"
                + "[Store].[Store].[USA].[CA].[Beverly Hills]\n"
                + "[Store].[Store].[USA].[CA].[Los Angeles]\n"
                + "[Store].[Store].[USA].[CA].[San Diego]\n"
                + "[Store].[Store].[USA].[CA].[San Francisco]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGenerateNonSet(Context context) {
        // SSAS implicitly converts arg #2 to a set
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Generate({[Store].[USA], [Store].[USA].[CA]}, [Store].PrevMember, ALL)",
            "[Store].[Store].[Mexico]\n"
                + "[Store].[Store].[Mexico].[Zacatecas]" );

        // SSAS implicitly converts arg #1 to a set
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Generate([Store].[USA], [Store].PrevMember, ALL)",
            "[Store].[Store].[Mexico]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGenerateAll(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Generate({[Store].[USA].[CA], [Store].[USA].[OR].[Portland]},"
                + " Ascendants([Store].CurrentMember),"
                + " ALL)",
            "[Store].[Store].[USA].[CA]\n"
                + "[Store].[Store].[USA]\n"
                + "[Store].[Store].[All Stores]\n"
                + "[Store].[Store].[USA].[OR].[Portland]\n"
                + "[Store].[Store].[USA].[OR]\n"
                + "[Store].[Store].[USA]\n"
                + "[Store].[Store].[All Stores]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGenerateUnique(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Generate({[Store].[USA].[CA], [Store].[USA].[OR].[Portland]},"
                + " Ascendants([Store].CurrentMember))",
            "[Store].[Store].[USA].[CA]\n"
                + "[Store].[Store].[USA]\n"
                + "[Store].[Store].[All Stores]\n"
                + "[Store].[Store].[USA].[OR].[Portland]\n"
                + "[Store].[Store].[USA].[OR]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGenerateUniqueTuple(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Generate({([Store].[USA].[CA],[Product].[All Products]), "
                + "([Store].[USA].[CA],[Product].[All Products])},"
                + "{([Store].CurrentMember, [Product].CurrentMember)})",
            "{[Store].[Store].[USA].[CA], [Product].[Product].[All Products]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGenerateCrossJoin(Context context) {
        // Note that the different regions have different Top 2.
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Generate({[Store].[USA].[CA], [Store].[USA].[CA].[San Francisco]},\n"
                + "  CrossJoin({[Store].CurrentMember},\n"
                + "    TopCount([Product].[Brand Name].members, \n"
                + "    2,\n"
                + "    [Measures].[Unit Sales])))",
            "{[Store].[Store].[USA].[CA], [Product].[Product].[Food].[Produce].[Vegetables].[Fresh Vegetables].[Hermanos]}\n"
                + "{[Store].[Store].[USA].[CA], [Product].[Product].[Food].[Produce].[Vegetables].[Fresh Vegetables].[Tell Tale]}\n"
                + "{[Store].[Store].[USA].[CA].[San Francisco], [Product].[Product].[Food].[Produce].[Vegetables].[Fresh Vegetables].[Ebony]}\n"
                + "{[Store].[Store].[USA].[CA].[San Francisco], [Product].[Product].[Food].[Produce].[Vegetables].[Fresh Vegetables].[High "
                + "Top]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGenerateString(Context context) {
        TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Generate({Time.[1997], Time.[1998]},"
                + " Time.[Time].CurrentMember.Name)",
            "19971998" );
        TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Generate({Time.[1997], Time.[1998]},"
                + " Time.[Time].CurrentMember.Name, \" and \")",
            "1997 and 1998" );
    }

    //TODO: URGENT!!!!!
    //TODO: remove disable reset timeout time
    @Disabled
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGenerateWillTimeout(Context context) {
        ((TestContextImpl)context).setQueryTimeout(5);
        SystemWideProperties.instance().EnableNativeNonEmpty = false;
        try {
            executeAxis(context.getConnectionWithDefaultRole(), "Sales",
                "Generate([Product].[Product Name].members,"
                    + "  Generate([Customers].[Name].members, "
                    + "    {([Store].CurrentMember, [Product].CurrentMember, [Customers].CurrentMember)}))" );
        } catch ( QueryTimeoutException e ) {
            return;
        } catch ( CancellationException e ) {
            return;
        }
        fail( "should have timed out" );
    }

    // The test case for the issue: MONDRIAN-2402
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGenerateForStringMemberProperty(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Store].[Store].[Lineage of Time] AS\n"
                + " Generate(Ascendants([Time].[Time].CurrentMember), [Time].[Time].CurrentMember.Properties(\"MEMBER_CAPTION\"), \",\")\n"
                + " SELECT\n"
                + "  {[Time].[Time].[1997]} ON Axis(0),\n"
                + "  Union(\n"
                + "   {([Store].[Store].[Lineage of Time])},\n"
                + "   {[Store].[Store].[All Stores]}) ON Axis(1)\n"
                + " FROM [Sales]\n",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Time].[Time].[1997]}\n"
                + "Axis #2:\n"
                + "{[Store].[Store].[Lineage of Time]}\n"
                + "{[Store].[Store].[All Stores]}\n"
                + "Row #0: 1997\n"
                + "Row #1: 266,773\n" );
    }

}
