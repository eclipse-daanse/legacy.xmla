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
package org.eclipse.daanse.olap.function.def.periodstodate;

import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertExprThrows;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertSetExprDependsOn;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class PeriodsToDateFunDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testPeriodsToDate(Context context) {
        assertSetExprDependsOn(context.getConnectionWithDefaultRole(), "PeriodsToDate()", "{[Time]}" );
        assertSetExprDependsOn(context.getConnectionWithDefaultRole(),
            "PeriodsToDate([Time].[Year])",
            "{[Time]}" );
        assertSetExprDependsOn(context.getConnectionWithDefaultRole(),
            "PeriodsToDate([Time].[Year], [Time].[1997].[Q2].[5])", "{}" );

        // two args
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "PeriodsToDate([Time].[Quarter], [Time].[1997].[Q2].[5])",
            "[Time].[1997].[Q2].[4]\n" + "[Time].[1997].[Q2].[5]" );

        // equivalent to above
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "TopCount("
                + "  Descendants("
                + "    Ancestor("
                + "      [Time].[1997].[Q2].[5], [Time].[Quarter]),"
                + "    [Time].[1997].[Q2].[5].Level),"
                + "  1).Item(0) : [Time].[1997].[Q2].[5]",
            "[Time].[1997].[Q2].[4]\n" + "[Time].[1997].[Q2].[5]" );

        // one arg
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Measures].[Foo] as ' SetToStr(PeriodsToDate([Time].[Quarter])) '\n"
                + "select {[Measures].[Foo]} on columns\n"
                + "from [Sales]\n"
                + "where [Time].[1997].[Q2].[5]",
            "Axis #0:\n"
                + "{[Time].[1997].[Q2].[5]}\n"
                + "Axis #1:\n"
                + "{[Measures].[Foo]}\n"
                + "Row #0: {[Time].[1997].[Q2].[4], [Time].[1997].[Q2].[5]}\n" );

        // zero args
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Measures].[Foo] as ' SetToStr(PeriodsToDate()) '\n"
                + "select {[Measures].[Foo]} on columns\n"
                + "from [Sales]\n"
                + "where [Time].[1997].[Q2].[5]",
            "Axis #0:\n"
                + "{[Time].[1997].[Q2].[5]}\n"
                + "Axis #1:\n"
                + "{[Measures].[Foo]}\n"
                + "Row #0: {[Time].[1997].[Q2].[4], [Time].[1997].[Q2].[5]}\n" );

        // zero args, evaluated at a member which is at the top level.
        // The default level is the level above the current member -- so
        // choosing a member at the highest level might trip up the
        // implementation.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Measures].[Foo] as ' SetToStr(PeriodsToDate()) '\n"
                + "select {[Measures].[Foo]} on columns\n"
                + "from [Sales]\n"
                + "where [Time].[1997]",
            "Axis #0:\n"
                + "{[Time].[1997]}\n"
                + "Axis #1:\n"
                + "{[Measures].[Foo]}\n"
                + "Row #0: {}\n" );

        // Testcase for bug 1598379, which caused NPE because the args[0].type
        // knew its dimension but not its hierarchy.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Measures].[Position] as\n"
                + " 'Sum("
                + "PeriodsToDate([Time].[Time].Levels(0),"
                + " [Time].[Time].CurrentMember), "
                + "[Measures].[Store Sales])'\n"
                + "select {[Time].[1997],\n"
                + " [Time].[1997].[Q1],\n"
                + " [Time].[1997].[Q1].[1],\n"
                + " [Time].[1997].[Q1].[2],\n"
                + " [Time].[1997].[Q1].[3]} ON COLUMNS,\n"
                + "{[Measures].[Store Sales], [Measures].[Position] } ON ROWS\n"
                + "from [Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Time].[1997]}\n"
                + "{[Time].[1997].[Q1]}\n"
                + "{[Time].[1997].[Q1].[1]}\n"
                + "{[Time].[1997].[Q1].[2]}\n"
                + "{[Time].[1997].[Q1].[3]}\n"
                + "Axis #2:\n"
                + "{[Measures].[Store Sales]}\n"
                + "{[Measures].[Position]}\n"
                + "Row #0: 565,238.13\n"
                + "Row #0: 139,628.35\n"
                + "Row #0: 45,539.69\n"
                + "Row #0: 44,058.79\n"
                + "Row #0: 50,029.87\n"
                + "Row #1: 565,238.13\n"
                + "Row #1: 139,628.35\n"
                + "Row #1: 45,539.69\n"
                + "Row #1: 89,598.48\n"
                + "Row #1: 139,628.35\n" );

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select\n"
                + "{[Measures].[Unit Sales]} on columns,\n"
                + "periodstodate(\n"
                + "    [Product].[Product Category],\n"
                + "    [Product].[Food].[Baked Goods].[Bread].[Muffins]) on rows\n"
                + "from [Sales]\n"
                + "",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "Axis #2:\n"
                + "{[Product].[Food].[Baked Goods].[Bread].[Bagels]}\n"
                + "{[Product].[Food].[Baked Goods].[Bread].[Muffins]}\n"
                + "Row #0: 815\n"
                + "Row #1: 3,497\n"
                + "" );

        // TODO: enable
        if ( false ) {
            assertExprThrows(context.getConnectionWithDefaultRole(),
                "Sum(PeriodsToDate([Time.Weekly].[Year], [Time].CurrentMember), [Measures].[Unit Sales])",
                "wrong dimension" );
        }
    }

}
