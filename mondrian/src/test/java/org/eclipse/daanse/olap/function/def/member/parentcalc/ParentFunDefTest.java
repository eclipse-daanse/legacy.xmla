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
package org.eclipse.daanse.olap.function.def.member.parentcalc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertMemberExprDependsOn;
import static org.opencube.junit5.TestUtil.executeQuery;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.result.Result;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;



class ParentFunDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testParent(Context context) {
        Connection connection = context.getConnectionWithDefaultRole();
        assertMemberExprDependsOn(connection,
            "[Gender].Parent",
            "{[Gender]}" );
        assertMemberExprDependsOn(connection, "[Gender].[M].Parent", "{}" );
        assertAxisReturns(connection,
            "{[Store].[USA].[CA].Parent}", "[Store].[USA]" );
        // root member has null parent
        assertAxisReturns(connection, "{[Store].[All Stores].Parent}", "" );
        // parent of null member is null
        assertAxisReturns(connection, "{[Store].[All Stores].Parent.Parent}", "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testParentPC(Context context) {
        //final Context testContext = getContext().withCube( "HR" );
        Connection connection = context.getConnectionWithDefaultRole();
        assertAxisReturns(connection, "HR",
            "[Employees].Parent",
            "" );
        assertAxisReturns(connection, "HR",
            "[Employees].[Sheri Nowmer].Parent",
            "[Employees].[All Employees]" );
        assertAxisReturns(connection, "HR",
            "[Employees].[Sheri Nowmer].[Derrick Whelply].Parent",
            "[Employees].[Sheri Nowmer]" );
        assertAxisReturns(connection, "HR",
            "[Employees].Members.Item(3)",
            "[Employees].[Sheri Nowmer].[Derrick Whelply].[Beverly Baker]" );
        assertAxisReturns(connection, "HR",
            "[Employees].Members.Item(3).Parent",
            "[Employees].[Sheri Nowmer].[Derrick Whelply]" );
        assertAxisReturns(connection, "HR",
            "[Employees].AllMembers.Item(3).Parent",
            "[Employees].[Sheri Nowmer].[Derrick Whelply]" );

        // Ascendants(<Member>) applied to parent-child hierarchy accessed via
        // <Level>.Members
        assertAxisReturns(connection, "HR",
            "Ascendants([Employees].Members.Item(73))",
            "[Employees].[Sheri Nowmer].[Derrick Whelply].[Beverly Baker].[Jacqueline Wyllie].[Ralph Mccoy].[Bertha "
                + "Jameson].[James Bailey]\n"
                + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Beverly Baker].[Jacqueline Wyllie].[Ralph Mccoy].[Bertha "
                + "Jameson]\n"
                + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Beverly Baker].[Jacqueline Wyllie].[Ralph Mccoy]\n"
                + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Beverly Baker].[Jacqueline Wyllie]\n"
                + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Beverly Baker]\n"
                + "[Employees].[Sheri Nowmer].[Derrick Whelply]\n"
                + "[Employees].[Sheri Nowmer]\n"
                + "[Employees].[All Employees]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBasic5(Context context) {
        Result result =
            executeQuery(context.getConnectionWithDefaultRole(),
                "select{ [Product].[All Products].[Drink].Parent} on columns "
                    + "from Sales" );
        assertEquals(
            "All Products",
            result.getAxes()[ 0 ].getPositions().get( 0 ).get( 0 ).getName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFirstInLevel5(Context context) {
        Result result =
            executeQuery(context.getConnectionWithDefaultRole(),
                "select {[Time].[1997].[Q2].[4].Parent} on columns,"
                    + "{[Gender].[M]} on rows from Sales" );
        assertEquals(
            "Q2",
            result.getAxes()[ 0 ].getPositions().get( 0 ).get( 0 ).getName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAll5(Context context) {
        Result result =
            executeQuery(context.getConnectionWithDefaultRole(),
                "select {[Time].[1997].[Q2].Parent} on columns,"
                    + "{[Gender].[M]} on rows from Sales" );
        // previous to [Gender].[All] is null, so no members are returned
        assertEquals(
            "1997",
            result.getAxes()[ 0 ].getPositions().get( 0 ).get( 0 ).getName() );
    }

}
