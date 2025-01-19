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
package org.eclipse.daanse.olap.function.def.set.hierarchy;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class AllMembersFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAllMembers(Context context) {
        Connection connection = context.getConnectionWithDefaultRole();
        // <Level>.allmembers
        assertAxisReturns(connection,
            "{[Customers].[Country].allmembers}",
            "[Customers].[Canada]\n"
                + "[Customers].[Mexico]\n"
                + "[Customers].[USA]" );

        // <Level>.allmembers applied to 'all' level
        assertAxisReturns(connection,
            "{[Customers].[(All)].allmembers}", "[Customers].[All Customers]" );

        // <Level>.allmembers applied to measures dimension
        // Note -- cube-level calculated members ARE present
        assertAxisReturns(connection,
            "{[Measures].[MeasuresLevel].allmembers}",
            "[Measures].[Unit Sales]\n"
                + "[Measures].[Store Cost]\n"
                + "[Measures].[Store Sales]\n"
                + "[Measures].[Sales Count]\n"
                + "[Measures].[Customer Count]\n"
                + "[Measures].[Promotion Sales]\n"
                + "[Measures].[Profit]\n"
                + "[Measures].[Profit Growth]\n"
                + "[Measures].[Profit last Period]" );

        // <Dimension>.allmembers applied to Measures
        assertAxisReturns(connection,
            "{[Measures].allmembers}",
            "[Measures].[Unit Sales]\n"
                + "[Measures].[Store Cost]\n"
                + "[Measures].[Store Sales]\n"
                + "[Measures].[Sales Count]\n"
                + "[Measures].[Customer Count]\n"
                + "[Measures].[Promotion Sales]\n"
                + "[Measures].[Profit]\n"
                + "[Measures].[Profit Growth]\n"
                + "[Measures].[Profit last Period]" );

        // <Dimension>.allmembers applied to a query with calc measures
        // Calc measures are returned
        switch (getDatabaseProduct(TestUtil.getDialect(connection).getDialectName())) {
            case INFOBRIGHT:
                // Skip this test on Infobright, because [Promotion Sales] is
                // defined wrong.
                break;
            default:
                assertQueryReturns(connection,
                    "with member [Measures].[Xxx] AS ' [Measures].[Unit Sales] '"
                        + "select {[Measures].allmembers} on columns from [Sales]",
                    "Axis #0:\n"
                        + "{}\n"
                        + "Axis #1:\n"
                        + "{[Measures].[Unit Sales]}\n"
                        + "{[Measures].[Store Cost]}\n"
                        + "{[Measures].[Store Sales]}\n"
                        + "{[Measures].[Sales Count]}\n"
                        + "{[Measures].[Customer Count]}\n"
                        + "{[Measures].[Promotion Sales]}\n"
                        + "{[Measures].[Profit]}\n"
                        + "{[Measures].[Profit Growth]}\n"
                        + "{[Measures].[Profit last Period]}\n"
                        + "{[Measures].[Xxx]}\n"
                        + "Row #0: 266,773\n"
                        + "Row #0: 225,627.23\n"
                        + "Row #0: 565,238.13\n"
                        + "Row #0: 86,837\n"
                        + "Row #0: 5,581\n"
                        + "Row #0: 151,211.21\n"
                        + "Row #0: $339,610.90\n"
                        + "Row #0: 0.0%\n"
                        + "Row #0: $339,610.90\n"
                        + "Row #0: 266,773\n" );
        }

        // Calc measure members from schema and from query
        switch (getDatabaseProduct(TestUtil.getDialect(connection).getDialectName())) {
            case INFOBRIGHT:
                // Skip this test on Infobright, because [Promotion Sales] is
                // defined wrong.
                break;
            default:
                assertQueryReturns(context.getConnectionWithDefaultRole(),
                    "WITH MEMBER [Measures].[Unit to Sales ratio] as\n"
                        + " '[Measures].[Unit Sales] / [Measures].[Store Sales]', FORMAT_STRING='0.0%' "
                        + "SELECT {[Measures].AllMembers} ON COLUMNS,"
                        + "non empty({[Store].[Store State].Members}) ON ROWS "
                        + "FROM Sales "
                        + "WHERE ([1997].[Q1])",
                    "Axis #0:\n"
                        + "{[Time].[1997].[Q1]}\n"
                        + "Axis #1:\n"
                        + "{[Measures].[Unit Sales]}\n"
                        + "{[Measures].[Store Cost]}\n"
                        + "{[Measures].[Store Sales]}\n"
                        + "{[Measures].[Sales Count]}\n"
                        + "{[Measures].[Customer Count]}\n"
                        + "{[Measures].[Promotion Sales]}\n"
                        + "{[Measures].[Profit]}\n"
                        + "{[Measures].[Profit Growth]}\n"
                        + "{[Measures].[Profit last Period]}\n"
                        + "{[Measures].[Unit to Sales ratio]}\n"
                        + "Axis #2:\n"
                        + "{[Store].[USA].[CA]}\n"
                        + "{[Store].[USA].[OR]}\n"
                        + "{[Store].[USA].[WA]}\n"
                        + "Row #0: 16,890\n"
                        + "Row #0: 14,431.09\n"
                        + "Row #0: 36,175.20\n"
                        + "Row #0: 5,498\n"
                        + "Row #0: 1,110\n"
                        + "Row #0: 14,447.16\n"
                        + "Row #0: $21,744.11\n"
                        + "Row #0: 0.0%\n"
                        + "Row #0: $21,744.11\n"
                        + "Row #0: 46.7%\n"
                        + "Row #1: 19,287\n"
                        + "Row #1: 16,081.07\n"
                        + "Row #1: 40,170.29\n"
                        + "Row #1: 6,184\n"
                        + "Row #1: 767\n"
                        + "Row #1: 10,829.64\n"
                        + "Row #1: $24,089.22\n"
                        + "Row #1: 0.0%\n"
                        + "Row #1: $24,089.22\n"
                        + "Row #1: 48.0%\n"
                        + "Row #2: 30,114\n"
                        + "Row #2: 25,240.08\n"
                        + "Row #2: 63,282.86\n"
                        + "Row #2: 9,906\n"
                        + "Row #2: 1,104\n"
                        + "Row #2: 18,459.60\n"
                        + "Row #2: $38,042.78\n"
                        + "Row #2: 0.0%\n"
                        + "Row #2: $38,042.78\n"
                        + "Row #2: 47.6%\n" );
        }

        // Calc member in query and schema not seen
        switch (getDatabaseProduct(TestUtil.getDialect(connection).getDialectName())) {
            case INFOBRIGHT:
                // Skip this test on Infobright, because [Promotion Sales] is
                // defined wrong.
                break;
            default:
                assertQueryReturns(context.getConnectionWithDefaultRole(),
                    "WITH MEMBER [Measures].[Unit to Sales ratio] as '[Measures].[Unit Sales] / [Measures].[Store Sales]', "
                        + "FORMAT_STRING='0.0%' "
                        + "SELECT {[Measures].AllMembers} ON COLUMNS,"
                        + "non empty({[Store].[Store State].Members}) ON ROWS "
                        + "FROM Sales "
                        + "WHERE ([1997].[Q1])",
                    "Axis #0:\n"
                        + "{[Time].[1997].[Q1]}\n"
                        + "Axis #1:\n"
                        + "{[Measures].[Unit Sales]}\n"
                        + "{[Measures].[Store Cost]}\n"
                        + "{[Measures].[Store Sales]}\n"
                        + "{[Measures].[Sales Count]}\n"
                        + "{[Measures].[Customer Count]}\n"
                        + "{[Measures].[Promotion Sales]}\n"
                        + "{[Measures].[Profit]}\n"
                        + "{[Measures].[Profit Growth]}\n"
                        + "{[Measures].[Profit last Period]}\n"
                        + "{[Measures].[Unit to Sales ratio]}\n"
                        + "Axis #2:\n"
                        + "{[Store].[USA].[CA]}\n"
                        + "{[Store].[USA].[OR]}\n"
                        + "{[Store].[USA].[WA]}\n"
                        + "Row #0: 16,890\n"
                        + "Row #0: 14,431.09\n"
                        + "Row #0: 36,175.20\n"
                        + "Row #0: 5,498\n"
                        + "Row #0: 1,110\n"
                        + "Row #0: 14,447.16\n"
                        + "Row #0: $21,744.11\n"
                        + "Row #0: 0.0%\n"
                        + "Row #0: $21,744.11\n"
                        + "Row #0: 46.7%\n"
                        + "Row #1: 19,287\n"
                        + "Row #1: 16,081.07\n"
                        + "Row #1: 40,170.29\n"
                        + "Row #1: 6,184\n"
                        + "Row #1: 767\n"
                        + "Row #1: 10,829.64\n"
                        + "Row #1: $24,089.22\n"
                        + "Row #1: 0.0%\n"
                        + "Row #1: $24,089.22\n"
                        + "Row #1: 48.0%\n"
                        + "Row #2: 30,114\n"
                        + "Row #2: 25,240.08\n"
                        + "Row #2: 63,282.86\n"
                        + "Row #2: 9,906\n"
                        + "Row #2: 1,104\n"
                        + "Row #2: 18,459.60\n"
                        + "Row #2: $38,042.78\n"
                        + "Row #2: 0.0%\n"
                        + "Row #2: $38,042.78\n"
                        + "Row #2: 47.6%\n" );
        }

        // Calc member in query and schema not seen
        switch (getDatabaseProduct(TestUtil.getDialect(connection).getDialectName())) {
            case INFOBRIGHT:
                // Skip this test on Infobright, because [Promotion Sales] is
                // defined wrong.
                break;
            default:
                assertQueryReturns(context.getConnectionWithDefaultRole(),
                    "WITH MEMBER [Measures].[Unit to Sales ratio] as '[Measures].[Unit Sales] / [Measures].[Store Sales]', "
                        + "FORMAT_STRING='0.0%' "
                        + "SELECT {[Measures].Members} ON COLUMNS,"
                        + "non empty({[Store].[Store State].Members}) ON ROWS "
                        + "FROM Sales "
                        + "WHERE ([1997].[Q1])",
                    "Axis #0:\n"
                        + "{[Time].[1997].[Q1]}\n"
                        + "Axis #1:\n"
                        + "{[Measures].[Unit Sales]}\n"
                        + "{[Measures].[Store Cost]}\n"
                        + "{[Measures].[Store Sales]}\n"
                        + "{[Measures].[Sales Count]}\n"
                        + "{[Measures].[Customer Count]}\n"
                        + "{[Measures].[Promotion Sales]}\n"
                        + "Axis #2:\n"
                        + "{[Store].[USA].[CA]}\n"
                        + "{[Store].[USA].[OR]}\n"
                        + "{[Store].[USA].[WA]}\n"
                        + "Row #0: 16,890\n"
                        + "Row #0: 14,431.09\n"
                        + "Row #0: 36,175.20\n"
                        + "Row #0: 5,498\n"
                        + "Row #0: 1,110\n"
                        + "Row #0: 14,447.16\n"
                        + "Row #1: 19,287\n"
                        + "Row #1: 16,081.07\n"
                        + "Row #1: 40,170.29\n"
                        + "Row #1: 6,184\n"
                        + "Row #1: 767\n"
                        + "Row #1: 10,829.64\n"
                        + "Row #2: 30,114\n"
                        + "Row #2: 25,240.08\n"
                        + "Row #2: 63,282.86\n"
                        + "Row #2: 9,906\n"
                        + "Row #2: 1,104\n"
                        + "Row #2: 18,459.60\n" );
        }

        // Calc member in dimension based on level
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Store].[USA].[CA plus OR] AS 'AGGREGATE({[Store].[USA].[CA], [Store].[USA].[OR]})' "
                + "SELECT {[Measures].[Unit Sales], [Measures].[Store Sales]} ON COLUMNS,"
                + "non empty({[Store].[Store State].AllMembers}) ON ROWS "
                + "FROM Sales "
                + "WHERE ([1997].[Q1])",
            "Axis #0:\n"
                + "{[Time].[1997].[Q1]}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "{[Measures].[Store Sales]}\n"
                + "Axis #2:\n"
                + "{[Store].[USA].[CA]}\n"
                + "{[Store].[USA].[OR]}\n"
                + "{[Store].[USA].[WA]}\n"
                + "{[Store].[USA].[CA plus OR]}\n"
                + "Row #0: 16,890\n"
                + "Row #0: 36,175.20\n"
                + "Row #1: 19,287\n"
                + "Row #1: 40,170.29\n"
                + "Row #2: 30,114\n"
                + "Row #2: 63,282.86\n"
                + "Row #3: 36,177\n"
                + "Row #3: 76,345.49\n" );

        // Calc member in dimension based on level not seen
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Store].[USA].[CA plus OR] AS 'AGGREGATE({[Store].[USA].[CA], [Store].[USA].[OR]})' "
                + "SELECT {[Measures].[Unit Sales], [Measures].[Store Sales]} ON COLUMNS,"
                + "non empty({[Store].[Store Country].AllMembers}) ON ROWS "
                + "FROM Sales "
                + "WHERE ([1997].[Q1])",
            "Axis #0:\n"
                + "{[Time].[1997].[Q1]}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "{[Measures].[Store Sales]}\n"
                + "Axis #2:\n"
                + "{[Store].[USA]}\n"
                + "Row #0: 66,291\n"
                + "Row #0: 139,628.35\n" );
    }

}
