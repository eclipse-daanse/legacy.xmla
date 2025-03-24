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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertAxisThrows;
import static org.opencube.junit5.TestUtil.assertExprDependsOn;
import static org.opencube.junit5.TestUtil.assertMemberExprDependsOn;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.executeSingletonAxis;
import static org.opencube.junit5.TestUtil.isDefaultNullMemberRepresentation;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Member;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.fun.FunctionTest;

class OpeningClosingPeriodFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testClosingPeriodNoArgs(Context context) {
        Connection connection = context.getConnectionWithDefaultRole();
        assertMemberExprDependsOn(connection,
            "ClosingPeriod()", "{[Time].[Time]}" );
        // MSOLAP returns [1997].[Q4], because [Time].CurrentMember =
        // [1997].
        Member member = executeSingletonAxis(connection, "ClosingPeriod()", "Sales" );
        assertEquals( "[Time].[Time].[1997].[Q4]", member.getUniqueName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testClosingPeriodLevel(Context context) {
        Connection connection = context.getConnectionWithDefaultRole();
        assertMemberExprDependsOn(connection,
            "ClosingPeriod([Time].[Year])", "{[Time].[Time]}" );
        assertMemberExprDependsOn(connection,
            "([Measures].[Unit Sales], ClosingPeriod([Time].[Month]))",
            "{[Time].[Time]}" );

        Member member;

        member = executeSingletonAxis(connection, "ClosingPeriod([Year])", "Sales" );
        assertEquals( "[Time].[Time].[1997]", member.getUniqueName() );

        member = executeSingletonAxis(connection, "ClosingPeriod([Quarter])", "Sales" );
        assertEquals( "[Time].[Time].[1997].[Q4]", member.getUniqueName() );

        member = executeSingletonAxis(connection, "ClosingPeriod([Month])", "Sales" );
        assertEquals( "[Time].[Time].[1997].[Q4].[12]", member.getUniqueName() );

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
                + "{[Time].[Time].[1997]}\n"
                + "{[Time].[Time].[1997].[Q1]}\n"
                + "{[Time].[Time].[1997].[Q1].[1]}\n"
                + "{[Time].[Time].[1997].[Q1].[2]}\n"
                + "{[Time].[Time].[1997].[Q1].[3]}\n"
                + "{[Time].[Time].[1997].[Q2]}\n"
                + "{[Time].[Time].[1997].[Q2].[4]}\n"
                + "{[Time].[Time].[1997].[Q2].[5]}\n"
                + "{[Time].[Time].[1997].[Q2].[6]}\n"
                + "{[Time].[Time].[1997].[Q3]}\n"
                + "{[Time].[Time].[1997].[Q3].[7]}\n"
                + "{[Time].[Time].[1997].[Q3].[8]}\n"
                + "{[Time].[Time].[1997].[Q3].[9]}\n"
                + "{[Time].[Time].[1997].[Q4]}\n"
                + "{[Time].[Time].[1997].[Q4].[10]}\n"
                + "{[Time].[Time].[1997].[Q4].[11]}\n"
                + "{[Time].[Time].[1997].[Q4].[12]}\n"
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
                + "{[Time].[Time].[1997]}\n"
                + "{[Time].[Time].[1997].[Q1]}\n"
                + "{[Time].[Time].[1997].[Q1].[1]}\n"
                + "{[Time].[Time].[1997].[Q1].[3]}\n"
                + "{[Time].[Time].[1997].[Q4].[12]}\n"
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
        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "ClosingPeriod([Store].[Store City])",
            "The <level> and <member> arguments to ClosingPeriod must be from "
                + "the same hierarchy. The level was from '[Store]' but the member "
                + "was from '[Time]'", "Sales" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testClosingPeriodMember(Context context) {
        if ( false ) {
            // This test is mistaken. Valid forms are ClosingPeriod(<level>)
            // and ClosingPeriod(<level>, <member>), but not
            // ClosingPeriod(<member>)
            Member member = executeSingletonAxis( context.getConnectionWithDefaultRole(),"ClosingPeriod([USA])", "Sales" );
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
            member = executeSingletonAxis(context.getConnectionWithDefaultRole(),
                "ClosingPeriod([Time].[1997].[Q3].[8])", "Sales" );
            assertNull( member );
        } else if ( isDefaultNullMemberRepresentation() ) {
            assertQueryReturns(context.getConnectionWithDefaultRole(),
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
                    + "{[Time].[Time].[1997]}\n"
                    + "{[Time].[Time].[1997].[Q2]}\n"
                    + "{[Time].[Time].[1997].[Q2].[4]}\n"
                    + "Row #0: [Time].[Time].[1997].[Q4]\n"
                    + "Row #1: [Time].[Time].[1997].[Q2].[6]\n"
                    + "Row #2: [Time].[Time].[#null]\n"
                    // MSAS returns "" here.
                    + "" );
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testClosingPeriod(Context context) {
        Connection connection = context.getConnectionWithDefaultRole();
        assertMemberExprDependsOn(connection,
            "ClosingPeriod([Time].[Month], [Time].[Time].CurrentMember)",
            "{[Time].[Time]}" );

        String s1 = FunctionTest.allHiersExcept( "[Measures]" );
        assertExprDependsOn(connection,
            "(([Measures].[Store Sales],"
                + " ClosingPeriod([Time].[Month], [Time].[Time].CurrentMember)) - "
                + "([Measures].[Store Cost],"
                + " ClosingPeriod([Time].[Time].[Month], [Time].[Time].CurrentMember)))",
            s1 );

        assertMemberExprDependsOn(connection,
            "ClosingPeriod([Time].[Month], [Time].[1997].[Q3])", "{}" );

        assertAxisReturns(connection, "Sales",
            "ClosingPeriod([Time].[Year], [Time].[1997].[Q3])", "" );

        assertAxisReturns(connection, "Sales",
            "ClosingPeriod([Time].[Quarter], [Time].[1997].[Q3])",
            "[Time].[Time].[1997].[Q3]" );

        assertAxisReturns(connection, "Sales",
            "ClosingPeriod([Time].[Month], [Time].[1997].[Q3])",
            "[Time].[Time].[1997].[Q3].[9]" );

        assertAxisReturns(connection, "Sales",
            "ClosingPeriod([Time].[Quarter], [Time].[1997])",
            "[Time].[Time].[1997].[Q4]" );

        assertAxisReturns(connection, "Sales",
            "ClosingPeriod([Time].[Year], [Time].[1997])", "[Time].[Time].[1997]" );

        assertAxisReturns(connection, "Sales",
            "ClosingPeriod([Time].[Month], [Time].[1997])",
            "[Time].[Time].[1997].[Q4].[12]" );

        // leaf member

        assertAxisReturns(connection, "Sales",
            "ClosingPeriod([Time].[Year], [Time].[1997].[Q3].[8])", "" );

        assertAxisReturns(connection, "Sales",
            "ClosingPeriod([Time].[Quarter], [Time].[1997].[Q3].[8])", "" );

        assertAxisReturns(connection, "Sales",
            "ClosingPeriod([Time].[Month], [Time].[1997].[Q3].[8])",
            "[Time].[Time].[1997].[Q3].[8]" );

        // non-Time dimension

        assertAxisReturns(connection, "Sales",
            "ClosingPeriod([Product].[Product Name], [Product].[All Products].[Drink])",
            "[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Gorilla].[Gorilla Whole Milk]" );

        assertAxisReturns(connection, "Sales",
            "ClosingPeriod([Product].[Product Family], [Product].[All Products].[Drink])",
            "[Product].[Product].[Drink]" );

        // 'all' level

        assertAxisReturns(connection, "Sales",
            "ClosingPeriod([Product].[(All)], [Product].[All Products].[Drink])",
            "" );

        // ragged
        //getContext().withCube( "[Sales Ragged]" ).
        assertAxisReturns(connection, "[Sales Ragged]",
            "ClosingPeriod([Store].[Store City], [Store].[All Stores].[Israel])",
            "[Store].[Store].[Israel].[Israel].[Tel Aviv]" );

        // Default member is [Time].[1997].
        assertAxisReturns(connection, "Sales",
            "ClosingPeriod([Time].[Month])", "[Time].[Time].[1997].[Q4].[12]" );

        assertAxisReturns(connection, "Sales", "ClosingPeriod()", "[Time].[Time].[1997].[Q4]" );

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
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(),
            "ClosingPeriod([Quarter],[1997].[Q3].[8])", "Sales" );
        assertNull( member );
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOpeningPeriod(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "OpeningPeriod([Time].[Month], [Time].[1997].[Q3])",
            "[Time].[Time].[1997].[Q3].[7]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "OpeningPeriod([Time].[Quarter], [Time].[1997])",
            "[Time].[Time].[1997].[Q1]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "OpeningPeriod([Time].[Year], [Time].[1997])", "[Time].[Time].[1997]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "OpeningPeriod([Time].[Month], [Time].[1997])",
            "[Time].[Time].[1997].[Q1].[1]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "OpeningPeriod([Product].[Product Name], [Product].[All Products].[Drink])",
            "[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good].[Good Imported Beer]" );

        //getTestContext().withCube( "[Sales Ragged]" ).
        assertAxisReturns(context.getConnectionWithDefaultRole(), "[Sales Ragged]",
            "OpeningPeriod([Store].[Store City], [Store].[All Stores].[Israel])",
            "[Store].[Store].[Israel].[Israel].[Haifa]" );

        //getTestContext().withCube( "[Sales Ragged]" ).
        assertAxisReturns(context.getConnectionWithDefaultRole(), "[Sales Ragged]",
            "OpeningPeriod([Store].[Store State], [Store].[All Stores].[Israel])",
            "" );

        // Default member is [Time].[1997].
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "OpeningPeriod([Time].[Month])", "[Time].[Time].[1997].[Q1].[1]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales", "OpeningPeriod()", "[Time].[Time].[1997].[Q1]" );

        //TestContext testContext = getTestContext().withCube( "[Sales Ragged]" );
        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "OpeningPeriod([Time].[Year], [Store].[All Stores].[Israel])",
            "The <level> and <member> arguments to OpeningPeriod must be "
                + "from the same hierarchy. The level was from '[Time]' but "
                + "the member was from '[Store]'.", "[Sales Ragged]");

        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "OpeningPeriod([Store].[Store City])",
            "The <level> and <member> arguments to OpeningPeriod must be "
                + "from the same hierarchy. The level was from '[Store]' but "
                + "the member was from '[Time]'.", "[Sales Ragged]");
    }

    /**
     * This tests new NULL functionality exception throwing
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOpeningPeriodNull(Context context) {
        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "OpeningPeriod([Time].[Month], NULL)",
            "Function does not support NULL member parameter", "Sales" );
    }

}
