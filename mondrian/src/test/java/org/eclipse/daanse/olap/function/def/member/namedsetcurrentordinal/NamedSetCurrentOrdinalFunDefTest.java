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
package org.eclipse.daanse.olap.function.def.member.namedsetcurrentordinal;

import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertQueryThrows;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.Util;


class NamedSetCurrentOrdinalFunDefTest {
    /**
     * Tests NamedSet.CurrentOrdinal combined with the Order function.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSetCurrentOrdinalWithOrder(Context context) {
        // The <Named Set>.CurrentOrdinal only works correctly when named sets
        // are evaluated as iterables, and JDK 1.4 only supports lists.
        if ( Util.RETROWOVEN) {
            return;
        }
        assertQueryReturns(context.getConnection(),
            "with set [Time Regular] as [Time].[Time].Members\n"
                + " set [Time Reversed] as"
                + " Order([Time Regular], [Time Regular].CurrentOrdinal, BDESC)\n"
                + "select [Time Reversed] on 0\n"
                + "from [Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Time].[1998].[Q4].[12]}\n"
                + "{[Time].[1998].[Q4].[11]}\n"
                + "{[Time].[1998].[Q4].[10]}\n"
                + "{[Time].[1998].[Q4]}\n"
                + "{[Time].[1998].[Q3].[9]}\n"
                + "{[Time].[1998].[Q3].[8]}\n"
                + "{[Time].[1998].[Q3].[7]}\n"
                + "{[Time].[1998].[Q3]}\n"
                + "{[Time].[1998].[Q2].[6]}\n"
                + "{[Time].[1998].[Q2].[5]}\n"
                + "{[Time].[1998].[Q2].[4]}\n"
                + "{[Time].[1998].[Q2]}\n"
                + "{[Time].[1998].[Q1].[3]}\n"
                + "{[Time].[1998].[Q1].[2]}\n"
                + "{[Time].[1998].[Q1].[1]}\n"
                + "{[Time].[1998].[Q1]}\n"
                + "{[Time].[1998]}\n"
                + "{[Time].[1997].[Q4].[12]}\n"
                + "{[Time].[1997].[Q4].[11]}\n"
                + "{[Time].[1997].[Q4].[10]}\n"
                + "{[Time].[1997].[Q4]}\n"
                + "{[Time].[1997].[Q3].[9]}\n"
                + "{[Time].[1997].[Q3].[8]}\n"
                + "{[Time].[1997].[Q3].[7]}\n"
                + "{[Time].[1997].[Q3]}\n"
                + "{[Time].[1997].[Q2].[6]}\n"
                + "{[Time].[1997].[Q2].[5]}\n"
                + "{[Time].[1997].[Q2].[4]}\n"
                + "{[Time].[1997].[Q2]}\n"
                + "{[Time].[1997].[Q1].[3]}\n"
                + "{[Time].[1997].[Q1].[2]}\n"
                + "{[Time].[1997].[Q1].[1]}\n"
                + "{[Time].[1997].[Q1]}\n"
                + "{[Time].[1997]}\n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: 26,796\n"
                + "Row #0: 25,270\n"
                + "Row #0: 19,958\n"
                + "Row #0: 72,024\n"
                + "Row #0: 20,388\n"
                + "Row #0: 21,697\n"
                + "Row #0: 23,763\n"
                + "Row #0: 65,848\n"
                + "Row #0: 21,350\n"
                + "Row #0: 21,081\n"
                + "Row #0: 20,179\n"
                + "Row #0: 62,610\n"
                + "Row #0: 23,706\n"
                + "Row #0: 20,957\n"
                + "Row #0: 21,628\n"
                + "Row #0: 66,291\n"
                + "Row #0: 266,773\n" );
    }

    /**
     * Tests NamedSet.CurrentOrdinal combined with the Generate function.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSetCurrentOrdinalWithGenerate(Context context) {
        // The <Named Set>.CurrentOrdinal only works correctly when named sets
        // are evaluated as iterables, and JDK 1.4 only supports lists.
        if ( Util.RETROWOVEN) {
            return;
        }
        assertQueryReturns(context.getConnection(),
            " with set [Time Regular] as [Time].[Time].Members\n"
                + "set [Every Other Time] as\n"
                + "  Generate(\n"
                + "    [Time Regular],\n"
                + "    {[Time].[Time].Members.Item(\n"
                + "      [Time Regular].CurrentOrdinal * 2)})\n"
                + "select [Every Other Time] on 0\n"
                + "from [Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Time].[1997]}\n"
                + "{[Time].[1997].[Q1].[1]}\n"
                + "{[Time].[1997].[Q1].[3]}\n"
                + "{[Time].[1997].[Q2].[4]}\n"
                + "{[Time].[1997].[Q2].[6]}\n"
                + "{[Time].[1997].[Q3].[7]}\n"
                + "{[Time].[1997].[Q3].[9]}\n"
                + "{[Time].[1997].[Q4].[10]}\n"
                + "{[Time].[1997].[Q4].[12]}\n"
                + "{[Time].[1998].[Q1]}\n"
                + "{[Time].[1998].[Q1].[2]}\n"
                + "{[Time].[1998].[Q2]}\n"
                + "{[Time].[1998].[Q2].[5]}\n"
                + "{[Time].[1998].[Q3]}\n"
                + "{[Time].[1998].[Q3].[8]}\n"
                + "{[Time].[1998].[Q4]}\n"
                + "{[Time].[1998].[Q4].[11]}\n"
                + "Row #0: 266,773\n"
                + "Row #0: 21,628\n"
                + "Row #0: 23,706\n"
                + "Row #0: 20,179\n"
                + "Row #0: 21,350\n"
                + "Row #0: 23,763\n"
                + "Row #0: 20,388\n"
                + "Row #0: 19,958\n"
                + "Row #0: 26,796\n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSetCurrentOrdinalWithFilter(Context context) {
        // The <Named Set>.CurrentOrdinal only works correctly when named sets
        // are evaluated as iterables, and JDK 1.4 only supports lists.
        if ( Util.RETROWOVEN) {
            return;
        }
        assertQueryReturns(context.getConnection(),
            "with set [Time Regular] as [Time].[Time].Members\n"
                + " set [Time Subset] as "
                + "   Filter([Time Regular], [Time Regular].CurrentOrdinal = 3"
                + "                       or [Time Regular].CurrentOrdinal = 5)\n"
                + "select [Time Subset] on 0\n"
                + "from [Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Time].[1997].[Q1].[2]}\n"
                + "{[Time].[1997].[Q2]}\n"
                + "Row #0: 20,957\n"
                + "Row #0: 62,610\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSetCurrentOrdinalWithCrossjoin(Context context) {
        // TODO:
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSetCurrentOrdinalWithNonNamedSetFails(Context context) {
        // a named set wrapped in {...} is not a named set, so CurrentOrdinal
        // fails
        assertQueryThrows(context,
            "with set [Time Members] as [Time].Members\n"
                + "member [Measures].[Foo] as ' {[Time Members]}.CurrentOrdinal '\n"
                + "select {[Measures].[Unit Sales], [Measures].[Foo]} on 0,\n"
                + " {[Product].Children} on 1\n"
                + "from [Sales]",
            "Not a named set" );

        // as above for Current function
        assertQueryThrows(context,
            "with set [Time Members] as [Time].Members\n"
                + "member [Measures].[Foo] as ' {[Time Members]}.Current.Name '\n"
                + "select {[Measures].[Unit Sales], [Measures].[Foo]} on 0,\n"
                + " {[Product].Children} on 1\n"
                + "from [Sales]",
            "Not a named set" );

        // a set expression is not a named set, so CurrentOrdinal fails
        assertQueryThrows(context,
            "with member [Measures].[Foo] as\n"
                + " ' Head([Time].Members, 5).CurrentOrdinal '\n"
                + "select {[Measures].[Unit Sales], [Measures].[Foo]} on 0,\n"
                + " {[Product].Children} on 1\n"
                + "from [Sales]",
            "Not a named set" );

        // as above for Current function
        assertQueryThrows(context,
            "with member [Measures].[Foo] as\n"
                + " ' Crossjoin([Time].Members, [Gender].Members).Current.Name '\n"
                + "select {[Measures].[Unit Sales], [Measures].[Foo]} on 0,\n"
                + " {[Product].Children} on 1\n"
                + "from [Sales]",
            "Not a named set" );
    }

}
