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
package org.eclipse.daanse.olap.function.def.parallelperiod;

import static mondrian.olap.fun.FunctionTest.*;
import static org.opencube.junit5.TestUtil.*;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertQueryThrows;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class ParallelPeriodFunDefTest {

    /**
     * Tests that Integeer.MIN_VALUE(-2147483648) does not cause NPE.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testParallelPeriodMinValue(Context context) {
        // By running the query and getting a result without an exception, we should assert the return value which will
        // have empty rows, because the parallel period value is too large, so rows will be empty
        // data, but it will still return a result.
        String query = "with "
            + "member [measures].[foo] as "
            + "'([Measures].[unit sales],"
            + "ParallelPeriod([Time].[Quarter], -2147483648))' "
            + "select "
            + "[measures].[foo] on columns, "
            + "[time].[1997].children on rows "
            + "from [sales]";
        String expected = "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[foo]}\n"
            + "Axis #2:\n"
            + "{[Time].[1997].[Q1]}\n"
            + "{[Time].[1997].[Q2]}\n"
            + "{[Time].[1997].[Q3]}\n"
            + "{[Time].[1997].[Q4]}\n"
            + "Row #0: \n"
            + "Row #1: \n"
            + "Row #2: \n"
            + "Row #3: \n";
        TestUtil.assertQueryReturns(context.getConnectionWithDefaultRole(), query, expected);
    }

    /**
     * Tests that ParallelPeriod with Aggregate function works
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testParallelPeriodWithSlicer(Context context) {
      TestUtil.assertQueryReturns(context.getConnectionWithDefaultRole(),
        "With "
          + "Set [*NATIVE_CJ_SET] as 'NonEmptyCrossJoin([*BASE_MEMBERS_Time],[*BASE_MEMBERS_Product])' "
          + "Set [*BASE_MEMBERS_Measures] as '{[Measures].[*FORMATTED_MEASURE_0], [Measures].[*FORMATTED_MEASURE_1]}' "
          + "Set [*BASE_MEMBERS_Time] as '{[Time].[1997].[Q2].[6]}' "
          + "Set [*NATIVE_MEMBERS_Time] as 'Generate([*NATIVE_CJ_SET], {[Time].[Time].CurrentMember})' "
          + "Set [*BASE_MEMBERS_Product] as '{[Product].[All Products].[Drink],[Product].[All Products].[Food]}' "
          + "Set [*NATIVE_MEMBERS_Product] as 'Generate([*NATIVE_CJ_SET], {[Product].CurrentMember})' "
          + "Member [Measures].[*FORMATTED_MEASURE_0] as '[Measures].[Customer Count]', FORMAT_STRING = '#,##0', "
          + "SOLVE_ORDER=400 "
          + "Member [Measures].[*FORMATTED_MEASURE_1] as "
          + "'([Measures].[Customer Count], ParallelPeriod([Time].[Quarter], 1, [Time].[Time].currentMember))', "
          + "FORMAT_STRING = '#,##0', SOLVE_ORDER=-200 "
          + "Member [Product].[*FILTER_MEMBER] as 'Aggregate ([*NATIVE_MEMBERS_Product])', SOLVE_ORDER=-300 "
          + "Select "
          + "[*BASE_MEMBERS_Measures] on columns, Non Empty Generate([*NATIVE_CJ_SET], {([Time].[Time].CurrentMember)})"
          + " on rows "
          + "From [Sales] "
          + "Where ([Product].[*FILTER_MEMBER])",
        "Axis #0:\n"
          + "{[Product].[*FILTER_MEMBER]}\n"
          + "Axis #1:\n"
          + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
          + "{[Measures].[*FORMATTED_MEASURE_1]}\n"
          + "Axis #2:\n"
          + "{[Time].[1997].[Q2].[6]}\n"
          + "Row #0: 1,314\n"
          + "Row #0: 1,447\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testParallelperiodOnLevelsString(Context context) {
      TestUtil.assertQueryReturns(context.getConnectionWithDefaultRole(),
        "with member Measures.[Prev Unit Sales] as 'parallelperiod(Levels(\"[Time].[Month]\"))'\n"
          + "select {[Measures].[Unit Sales], Measures.[Prev Unit Sales]} ON COLUMNS,\n"
          + "[Gender].members ON ROWS\n"
          + "from [Sales]\n"
          + "where [Time].[1997].[Q2].[5]",
        "Axis #0:\n"
          + "{[Time].[1997].[Q2].[5]}\n"
          + "Axis #1:\n"
          + "{[Measures].[Unit Sales]}\n"
          + "{[Measures].[Prev Unit Sales]}\n"
          + "Axis #2:\n"
          + "{[Gender].[All Gender]}\n"
          + "{[Gender].[F]}\n"
          + "{[Gender].[M]}\n"
          + "Row #0: 21,081\n"
          + "Row #0: 20,179\n"
          + "Row #1: 10,536\n"
          + "Row #1: 9,990\n"
          + "Row #2: 10,545\n"
          + "Row #2: 10,189\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testParallelperiodOnStrToMember(Context context) {
      assertQueryReturns(context.getConnectionWithDefaultRole(),
        "with member Measures.[Prev Unit Sales] as 'parallelperiod(strToMember(\"[Time].[1997].[Q2]\"))'\n"
          + "select {[Measures].[Unit Sales], Measures.[Prev Unit Sales]} ON COLUMNS,\n"
          + "[Gender].members ON ROWS\n"
          + "from [Sales]\n"
          + "where [Time].[1997].[Q2].[5]",
        "Axis #0:\n"
          + "{[Time].[1997].[Q2].[5]}\n"
          + "Axis #1:\n"
          + "{[Measures].[Unit Sales]}\n"
          + "{[Measures].[Prev Unit Sales]}\n"
          + "Axis #2:\n"
          + "{[Gender].[All Gender]}\n"
          + "{[Gender].[F]}\n"
          + "{[Gender].[M]}\n"
          + "Row #0: 21,081\n"
          + "Row #0: 20,957\n"
          + "Row #1: 10,536\n"
          + "Row #1: 10,266\n"
          + "Row #2: 10,545\n"
          + "Row #2: 10,691\n" );

      assertQueryThrows(context.getConnectionWithDefaultRole(),
        "with member Measures.[Prev Unit Sales] as 'parallelperiod(strToMember(\"[Time].[Quarter]\"))'\n"
          + "select {[Measures].[Unit Sales], Measures.[Prev Unit Sales]} ON COLUMNS,\n"
          + "[Gender].members ON ROWS\n"
          + "from [Sales]\n"
          + "where [Time].[1997].[Q2].[5]",
        "Cannot find MDX member '[Time].[Quarter]'. Make sure it is indeed a member and not a level or a hierarchy." );
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testParallelPeriod(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "parallelperiod([Time].[Quarter], 1, [Time].[1998].[Q1])",
            "[Time].[1997].[Q4]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "parallelperiod([Time].[Quarter], -1, [Time].[1997].[Q1])",
            "[Time].[1997].[Q2]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "parallelperiod([Time].[Year], 1, [Time].[1998].[Q1])",
            "[Time].[1997].[Q1]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "parallelperiod([Time].[Year], 1, [Time].[1998].[Q1].[1])",
            "[Time].[1997].[Q1].[1]" );

        // No args, therefore finds parallel period to [Time].[1997], which
        // would be [Time].[1996], except that that doesn't exist, so null.
        assertAxisReturns(context.getConnectionWithDefaultRole(), "ParallelPeriod()", "" );

        // Parallel period to [Time].[1997], which would be [Time].[1996],
        // except that that doesn't exist, so null.
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "ParallelPeriod([Time].[Year], 1, [Time].[1997])", "" );

        // one parameter, level 2 above member
        if ( isDefaultNullMemberRepresentation() ) {
            assertQueryReturns(context.getConnectionWithDefaultRole(),
                "WITH MEMBER [Measures].[Foo] AS \n"
                    + " ' ParallelPeriod([Time].[Year]).UniqueName '\n"
                    + "SELECT {[Measures].[Foo]} ON COLUMNS\n"
                    + "FROM [Sales]\n"
                    + "WHERE [Time].[1997].[Q3].[8]",
                "Axis #0:\n"
                    + "{[Time].[1997].[Q3].[8]}\n"
                    + "Axis #1:\n"
                    + "{[Measures].[Foo]}\n"
                    + "Row #0: [Time].[#null]\n" );
        }

        // one parameter, level 1 above member
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Measures].[Foo] AS \n"
                + " ' ParallelPeriod([Time].[Quarter]).UniqueName '\n"
                + "SELECT {[Measures].[Foo]} ON COLUMNS\n"
                + "FROM [Sales]\n"
                + "WHERE [Time].[1997].[Q3].[8]",
            "Axis #0:\n"
                + "{[Time].[1997].[Q3].[8]}\n"
                + "Axis #1:\n"
                + "{[Measures].[Foo]}\n"
                + "Row #0: [Time].[1997].[Q2].[5]\n" );

        // one parameter, level same as member
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Measures].[Foo] AS \n"
                + " ' ParallelPeriod([Time].[Month]).UniqueName '\n"
                + "SELECT {[Measures].[Foo]} ON COLUMNS\n"
                + "FROM [Sales]\n"
                + "WHERE [Time].[1997].[Q3].[8]",
            "Axis #0:\n"
                + "{[Time].[1997].[Q3].[8]}\n"
                + "Axis #1:\n"
                + "{[Measures].[Foo]}\n"
                + "Row #0: [Time].[1997].[Q3].[7]\n" );

        //  one parameter, level below member
        if ( isDefaultNullMemberRepresentation() ) {
            assertQueryReturns(context.getConnectionWithDefaultRole(),
                "WITH MEMBER [Measures].[Foo] AS \n"
                    + " ' ParallelPeriod([Time].[Month]).UniqueName '\n"
                    + "SELECT {[Measures].[Foo]} ON COLUMNS\n"
                    + "FROM [Sales]\n"
                    + "WHERE [Time].[1997].[Q3]",
                "Axis #0:\n"
                    + "{[Time].[1997].[Q3]}\n"
                    + "Axis #1:\n"
                    + "{[Measures].[Foo]}\n"
                    + "Row #0: [Time].[#null]\n" );
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void _testParallelPeriodThrowsException(Context context) {
        assertQueryThrows(context,
            "select {parallelperiod([Time].[Year], 1)} on columns "
                + "from [Sales] where ([Time].[1998].[Q1].[2])",
            "Hierarchy '[Time]' appears in more than one independent axis" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testParallelPeriodDepends(Context context) {
        assertMemberExprDependsOn(context.getConnectionWithDefaultRole(),
            "ParallelPeriod([Time].[Quarter], 2.0)", "{[Time]}" );
        assertMemberExprDependsOn(context.getConnectionWithDefaultRole(),
            "ParallelPeriod([Time].[Quarter], 2.0, [Time].[1997].[Q3])", "{}" );
        assertMemberExprDependsOn(context.getConnectionWithDefaultRole(),
            "ParallelPeriod()",
            "{[Time]}" );
        assertMemberExprDependsOn(context.getConnectionWithDefaultRole(),
            "ParallelPeriod([Product].[Food])", "{[Product]}" );
        // [Gender].[M] is used here as a numeric expression!
        // The numeric expression DOES depend upon [Product].
        // The expression as a whole depends upon everything except [Gender].
        String s1 = allHiersExcept( "[Gender]" );
        assertMemberExprDependsOn(context.getConnectionWithDefaultRole(),
            "ParallelPeriod([Product].[Product Family], [Gender].[M], [Product].[Food])",
            s1 );
        // As above
        String s11 = allHiersExcept( "[Gender]" );
        assertMemberExprDependsOn(context.getConnectionWithDefaultRole(),
            "ParallelPeriod([Product].[Product Family], [Gender].[M])", s11 );
        assertSetExprDependsOn(context.getConnectionWithDefaultRole(),
            "parallelperiod([Time].[Time].CurrentMember)",
            "{[Time]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testParallelPeriodLevelLag(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Measures].[Prev Unit Sales] as "
                + "        '([Measures].[Unit Sales], parallelperiod([Time].[Quarter], 2))' "
                + "select "
                + "    crossjoin({[Measures].[Unit Sales], [Measures].[Prev Unit Sales]}, {[Marital Status].[All Marital "
                + "Status].children}) on columns, "
                + "    {[Time].[1997].[Q3]} on rows "
                + "from  "
                + "    [Sales] ",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales], [Marital Status].[M]}\n"
                + "{[Measures].[Unit Sales], [Marital Status].[S]}\n"
                + "{[Measures].[Prev Unit Sales], [Marital Status].[M]}\n"
                + "{[Measures].[Prev Unit Sales], [Marital Status].[S]}\n"
                + "Axis #2:\n"
                + "{[Time].[1997].[Q3]}\n"
                + "Row #0: 32,815\n"
                + "Row #0: 33,033\n"
                + "Row #0: 33,101\n"
                + "Row #0: 33,190\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testParallelPeriodLevel(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with "
                + "    member [Measures].[Prev Unit Sales] as "
                + "        '([Measures].[Unit Sales], parallelperiod([Time].[Quarter]))' "
                + "select "
                + "    crossjoin({[Measures].[Unit Sales], [Measures].[Prev Unit Sales]}, {[Marital Status].[All Marital "
                + "Status].[M]}) on columns, "
                + "    {[Time].[1997].[Q3].[8]} on rows "
                + "from  "
                + "    [Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales], [Marital Status].[M]}\n"
                + "{[Measures].[Prev Unit Sales], [Marital Status].[M]}\n"
                + "Axis #2:\n"
                + "{[Time].[1997].[Q3].[8]}\n"
                + "Row #0: 10,957\n"
                + "Row #0: 10,280\n" );
    }

}
