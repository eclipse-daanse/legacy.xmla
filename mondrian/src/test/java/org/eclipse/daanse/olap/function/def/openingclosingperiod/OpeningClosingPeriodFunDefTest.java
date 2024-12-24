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
package org.eclipse.daanse.olap.function.def.openingclosingperiod;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Member;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertAxisThrows;
import static org.opencube.junit5.TestUtil.assertBooleanExprReturns;
import static org.opencube.junit5.TestUtil.assertExprDependsOn;
import static org.opencube.junit5.TestUtil.assertExprThrows;
import static org.opencube.junit5.TestUtil.assertMemberExprDependsOn;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertQueryThrows;
import static org.opencube.junit5.TestUtil.assertSetExprDependsOn;
import static org.opencube.junit5.TestUtil.assertStubbedEqualsVerbose;
import static org.opencube.junit5.TestUtil.compileExpression;
import static org.opencube.junit5.TestUtil.executeAxis;
import static org.opencube.junit5.TestUtil.executeExpr;
import static org.opencube.junit5.TestUtil.executeExprRaw;
import static org.opencube.junit5.TestUtil.executeQuery;
import static org.opencube.junit5.TestUtil.executeSingletonAxis;
import static org.opencube.junit5.TestUtil.hierarchyName;
import static org.opencube.junit5.TestUtil.isDefaultNullMemberRepresentation;
import static org.opencube.junit5.TestUtil.withSchema;

import mondrian.olap.fun.FunctionTest;

class OpeningClosingPeriodFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testClosingPeriodNoArgs(Context context) {
        Connection connection = context.getConnection();
        assertMemberExprDependsOn(connection,
            "ClosingPeriod()", "{[Time]}" );
        // MSOLAP returns [1997].[Q4], because [Time].CurrentMember =
        // [1997].
        Member member = executeSingletonAxis(connection, "ClosingPeriod()" );
        assertEquals( "[Time].[1997].[Q4]", member.getUniqueName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testClosingPeriodLevel(Context context) {
        Connection connection = context.getConnection();
        assertMemberExprDependsOn(connection,
            "ClosingPeriod([Time].[Year])", "{[Time]}" );
        assertMemberExprDependsOn(connection,
            "([Measures].[Unit Sales], ClosingPeriod([Time].[Month]))",
            "{[Time]}" );

        Member member;

        member = executeSingletonAxis(connection, "ClosingPeriod([Year])" );
        assertEquals( "[Time].[1997]", member.getUniqueName() );

        member = executeSingletonAxis(connection, "ClosingPeriod([Quarter])" );
        assertEquals( "[Time].[1997].[Q4]", member.getUniqueName() );

        member = executeSingletonAxis(connection, "ClosingPeriod([Month])" );
        assertEquals( "[Time].[1997].[Q4].[12]", member.getUniqueName() );

        assertQueryReturns(connection,
            "with member [Measures].[Closing Unit Sales] as "
                + "'([Measures].[Unit Sales], ClosingPeriod([Time].[Month]))'\n"
                + "select non empty {[Measures].[Closing Unit Sales]} on columns,\n"
                + " {Descendants([Time].[1997])} on rows\n"
                + "from [Sales]",

            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Closing Unit Sales]}\n"
                + "Axis #2:\n"
                + "{[Time].[1997]}\n"
                + "{[Time].[1997].[Q1]}\n"
                + "{[Time].[1997].[Q1].[1]}\n"
                + "{[Time].[1997].[Q1].[2]}\n"
                + "{[Time].[1997].[Q1].[3]}\n"
                + "{[Time].[1997].[Q2]}\n"
                + "{[Time].[1997].[Q2].[4]}\n"
                + "{[Time].[1997].[Q2].[5]}\n"
                + "{[Time].[1997].[Q2].[6]}\n"
                + "{[Time].[1997].[Q3]}\n"
                + "{[Time].[1997].[Q3].[7]}\n"
                + "{[Time].[1997].[Q3].[8]}\n"
                + "{[Time].[1997].[Q3].[9]}\n"
                + "{[Time].[1997].[Q4]}\n"
                + "{[Time].[1997].[Q4].[10]}\n"
                + "{[Time].[1997].[Q4].[11]}\n"
                + "{[Time].[1997].[Q4].[12]}\n"
                + "Row #0: 26,796\n"
                + "Row #1: 23,706\n"
                + "Row #2: 21,628\n"
                + "Row #3: 20,957\n"
                + "Row #4: 23,706\n"
                + "Row #5: 21,350\n"
                + "Row #6: 20,179\n"
                + "Row #7: 21,081\n"
                + "Row #8: 21,350\n"
                + "Row #9: 20,388\n"
                + "Row #10: 23,763\n"
                + "Row #11: 21,697\n"
                + "Row #12: 20,388\n"
                + "Row #13: 26,796\n"
                + "Row #14: 19,958\n"
                + "Row #15: 25,270\n"
                + "Row #16: 26,796\n" );

        assertQueryReturns(connection,
            "with member [Measures].[Closing Unit Sales] as '([Measures].[Unit Sales], ClosingPeriod([Time].[Month]))'\n"
                + "select {[Measures].[Unit Sales], [Measures].[Closing Unit Sales]} on columns,\n"
                + " {[Time].[1997], [Time].[1997].[Q1], [Time].[1997].[Q1].[1], [Time].[1997].[Q1].[3], [Time].[1997].[Q4]"
                + ".[12]} on rows\n"
                + "from [Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "{[Measures].[Closing Unit Sales]}\n"
                + "Axis #2:\n"
                + "{[Time].[1997]}\n"
                + "{[Time].[1997].[Q1]}\n"
                + "{[Time].[1997].[Q1].[1]}\n"
                + "{[Time].[1997].[Q1].[3]}\n"
                + "{[Time].[1997].[Q4].[12]}\n"
                + "Row #0: 266,773\n"
                + "Row #0: 26,796\n"
                + "Row #1: 66,291\n"
                + "Row #1: 23,706\n"
                + "Row #2: 21,628\n"
                + "Row #2: 21,628\n"
                + "Row #3: 23,706\n"
                + "Row #3: 23,706\n"
                + "Row #4: 26,796\n"
                + "Row #4: 26,796\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testClosingPeriodLevelNotInTimeFails(Context context) {
        assertAxisThrows(context.getConnection(),
            "ClosingPeriod([Store].[Store City])",
            "The <level> and <member> arguments to ClosingPeriod must be from "
                + "the same hierarchy. The level was from '[Store]' but the member "
                + "was from '[Time]'" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testClosingPeriodMember(Context context) {
        if ( false ) {
            // This test is mistaken. Valid forms are ClosingPeriod(<level>)
            // and ClosingPeriod(<level>, <member>), but not
            // ClosingPeriod(<member>)
            Member member = executeSingletonAxis( context.getConnection(),"ClosingPeriod([USA])" );
            assertEquals( "WA", member.getName() );
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testClosingPeriodMemberLeaf(Context context) {
        Member member;
        if ( false ) {
            // This test is mistaken. Valid forms are ClosingPeriod(<level>)
            // and ClosingPeriod(<level>, <member>), but not
            // ClosingPeriod(<member>)
            member = executeSingletonAxis(context.getConnection(),
                "ClosingPeriod([Time].[1997].[Q3].[8])" );
            assertNull( member );
        } else if ( isDefaultNullMemberRepresentation() ) {
            assertQueryReturns(context.getConnection(),
                "with member [Measures].[Foo] as ClosingPeriod().uniquename\n"
                    + "select {[Measures].[Foo]} on columns,\n"
                    + "  {[Time].[1997],\n"
                    + "   [Time].[1997].[Q2],\n"
                    + "   [Time].[1997].[Q2].[4]} on rows\n"
                    + "from Sales",
                "Axis #0:\n"
                    + "{}\n"
                    + "Axis #1:\n"
                    + "{[Measures].[Foo]}\n"
                    + "Axis #2:\n"
                    + "{[Time].[1997]}\n"
                    + "{[Time].[1997].[Q2]}\n"
                    + "{[Time].[1997].[Q2].[4]}\n"
                    + "Row #0: [Time].[1997].[Q4]\n"
                    + "Row #1: [Time].[1997].[Q2].[6]\n"
                    + "Row #2: [Time].[#null]\n"
                    // MSAS returns "" here.
                    + "" );
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testClosingPeriod(Context context) {
        Connection connection = context.getConnection();
        assertMemberExprDependsOn(connection,
            "ClosingPeriod([Time].[Month], [Time].[Time].CurrentMember)",
            "{[Time]}" );

        String s1 = FunctionTest.allHiersExcept( "[Measures]" );
        assertExprDependsOn(connection,
            "(([Measures].[Store Sales],"
                + " ClosingPeriod([Time].[Month], [Time].[Time].CurrentMember)) - "
                + "([Measures].[Store Cost],"
                + " ClosingPeriod([Time].[Month], [Time].[Time].CurrentMember)))",
            s1 );

        assertMemberExprDependsOn(connection,
            "ClosingPeriod([Time].[Month], [Time].[1997].[Q3])", "{}" );

        assertAxisReturns(connection,
            "ClosingPeriod([Time].[Year], [Time].[1997].[Q3])", "" );

        assertAxisReturns(connection,
            "ClosingPeriod([Time].[Quarter], [Time].[1997].[Q3])",
            "[Time].[1997].[Q3]" );

        assertAxisReturns(connection,
            "ClosingPeriod([Time].[Month], [Time].[1997].[Q3])",
            "[Time].[1997].[Q3].[9]" );

        assertAxisReturns(connection,
            "ClosingPeriod([Time].[Quarter], [Time].[1997])",
            "[Time].[1997].[Q4]" );

        assertAxisReturns(connection,
            "ClosingPeriod([Time].[Year], [Time].[1997])", "[Time].[1997]" );

        assertAxisReturns(connection,
            "ClosingPeriod([Time].[Month], [Time].[1997])",
            "[Time].[1997].[Q4].[12]" );

        // leaf member

        assertAxisReturns(connection,
            "ClosingPeriod([Time].[Year], [Time].[1997].[Q3].[8])", "" );

        assertAxisReturns(connection,
            "ClosingPeriod([Time].[Quarter], [Time].[1997].[Q3].[8])", "" );

        assertAxisReturns(connection,
            "ClosingPeriod([Time].[Month], [Time].[1997].[Q3].[8])",
            "[Time].[1997].[Q3].[8]" );

        // non-Time dimension

        assertAxisReturns(connection,
            "ClosingPeriod([Product].[Product Name], [Product].[All Products].[Drink])",
            "[Product].[Drink].[Dairy].[Dairy].[Milk].[Gorilla].[Gorilla Whole Milk]" );

        assertAxisReturns(connection,
            "ClosingPeriod([Product].[Product Family], [Product].[All Products].[Drink])",
            "[Product].[Drink]" );

        // 'all' level

        assertAxisReturns(connection,
            "ClosingPeriod([Product].[(All)], [Product].[All Products].[Drink])",
            "" );

        // ragged
        //getContext().withCube( "[Sales Ragged]" ).
        assertAxisReturns(connection, "[Sales Ragged]",
            "ClosingPeriod([Store].[Store City], [Store].[All Stores].[Israel])",
            "[Store].[Israel].[Israel].[Tel Aviv]" );

        // Default member is [Time].[1997].
        assertAxisReturns(connection,
            "ClosingPeriod([Time].[Month])", "[Time].[1997].[Q4].[12]" );

        assertAxisReturns(connection, "ClosingPeriod()", "[Time].[1997].[Q4]" );

        //Context testContext = getContext().withCube( "[Sales Ragged]" );
        assertAxisReturns(connection, "[Sales Ragged]",
            "ClosingPeriod([Store].[Store State], [Store].[All Stores].[Israel])",
            "" );

        assertAxisThrows(connection,
            "ClosingPeriod([Time].[Year], [Store].[All Stores].[Israel])",
            "The <level> and <member> arguments to ClosingPeriod must be "
                + "from the same hierarchy. The level was from '[Time]' but "
                + "the member was from '[Store]'.", "[Sales Ragged]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testClosingPeriodBelow(Context context) {
        Member member = executeSingletonAxis(context.getConnection(),
            "ClosingPeriod([Quarter],[1997].[Q3].[8])" );
        assertNull( member );
    }


}
