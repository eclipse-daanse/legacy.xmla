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
        TestUtil.assertQueryReturns(context.getConnection(), query, expected);
    }

    /**
     * Tests that ParallelPeriod with Aggregate function works
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testParallelPeriodWithSlicer(Context context) {
      TestUtil.assertQueryReturns(context.getConnection(),
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
      TestUtil.assertQueryReturns(context.getConnection(),
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
      assertQueryReturns(context.getConnection(),
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

      assertQueryThrows(context.getConnection(),
        "with member Measures.[Prev Unit Sales] as 'parallelperiod(strToMember(\"[Time].[Quarter]\"))'\n"
          + "select {[Measures].[Unit Sales], Measures.[Prev Unit Sales]} ON COLUMNS,\n"
          + "[Gender].members ON ROWS\n"
          + "from [Sales]\n"
          + "where [Time].[1997].[Q2].[5]",
        "Cannot find MDX member '[Time].[Quarter]'. Make sure it is indeed a member and not a level or a hierarchy." );
    }

}
