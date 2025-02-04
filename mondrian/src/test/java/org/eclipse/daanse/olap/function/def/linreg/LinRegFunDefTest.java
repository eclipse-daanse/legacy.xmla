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
package org.eclipse.daanse.olap.function.def.linreg;

import static org.opencube.junit5.TestUtil.assertExprReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.fun.FunctionTest;


class LinRegFunDefTest {

    /**
     * Tests all of the linear regression functions, as suggested by
     * <a href="http://support.microsoft.com/kb/q307276/">a Microsoft knowledge
     * base article</a>.
     */
    @Disabled //disabled for CI build
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void _testLinRegAll(Context context) {
        // We have not implemented the LastPeriods function, so we use
        //   [Time].CurrentMember.Lag(9) : [Time].CurrentMember
        // is equivalent to
        //   LastPeriods(10)
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER \n"
                + "[Measures].[Intercept] AS \n"
                + "  'LinRegIntercept([Time].CurrentMember.Lag(10) : [Time].CurrentMember, [Measures].[Unit Sales], "
                + "[Measures].[Store Sales])' \n"
                + "MEMBER [Measures].[Regression Slope] AS\n"
                + "  'LinRegSlope([Time].CurrentMember.Lag(9) : [Time].CurrentMember,[Measures].[Unit Sales],[Measures]"
                + ".[Store Sales]) '\n"
                + "MEMBER [Measures].[Predict] AS\n"
                + "  'LinRegPoint([Measures].[Unit Sales],[Time].CurrentMember.Lag(9) : [Time].CurrentMember,[Measures].[Unit"
                + " Sales],[Measures].[Store Sales])',\n"
                + "  FORMAT_STRING = 'Standard' \n"
                + "MEMBER [Measures].[Predict Formula] AS\n"
                + "  '([Measures].[Regression Slope] * [Measures].[Unit Sales]) + [Measures].[Intercept]',\n"
                + "  FORMAT_STRING='Standard'\n"
                + "MEMBER [Measures].[Good Fit] AS\n"
                + "  'LinRegR2([Time].CurrentMember.Lag(9) : [Time].CurrentMember, [Measures].[Unit Sales],[Measures].[Store "
                + "Sales])',\n"
                + "  FORMAT_STRING='#,#.00'\n"
                + "MEMBER [Measures].[Variance] AS\n"
                + "  'LinRegVariance([Time].CurrentMember.Lag(9) : [Time].CurrentMember,[Measures].[Unit Sales],[Measures]"
                + ".[Store Sales])'\n"
                + "SELECT \n"
                + "  {[Measures].[Store Sales], \n"
                + "   [Measures].[Intercept], \n"
                + "   [Measures].[Regression Slope], \n"
                + "   [Measures].[Predict], \n"
                + "   [Measures].[Predict Formula], \n"
                + "   [Measures].[Good Fit], \n"
                + "   [Measures].[Variance] } ON COLUMNS, \n"
                + "  Descendants([Time].[1997], [Time].[Month]) ON ROWS\n"
                + "FROM Sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Store Sales]}\n"
                + "{[Measures].[Intercept]}\n"
                + "{[Measures].[Regression Slope]}\n"
                + "{[Measures].[Predict]}\n"
                + "{[Measures].[Predict Formula]}\n"
                + "{[Measures].[Good Fit]}\n"
                + "{[Measures].[Variance]}\n"
                + "Axis #2:\n"
                + "{[Time].[1997].[Q1].[1]}\n"
                + "{[Time].[1997].[Q1].[2]}\n"
                + "{[Time].[1997].[Q1].[3]}\n"
                + "{[Time].[1997].[Q2].[4]}\n"
                + "{[Time].[1997].[Q2].[5]}\n"
                + "{[Time].[1997].[Q2].[6]}\n"
                + "{[Time].[1997].[Q3].[7]}\n"
                + "{[Time].[1997].[Q3].[8]}\n"
                + "{[Time].[1997].[Q3].[9]}\n"
                + "{[Time].[1997].[Q4].[10]}\n"
                + "{[Time].[1997].[Q4].[11]}\n"
                + "{[Time].[1997].[Q4].[12]}\n"
                + "Row #0: 45,539.69\n"
                + "Row #0: 68711.40\n"
                + "Row #0: -1.033\n"
                + "Row #0: 46,350.26\n"
                + "Row #0: 46.350.26\n"
                + "Row #0: -1.#INF\n"
                + "Row #0: 5.17E-08\n"
                + "...\n"
                + "Row #11: 15343.67\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLinRegPointMonth(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER \n"
                + "[Measures].[Test] as \n"
                + "  'LinRegPoint(\n"
                + "    Rank(Time.[Time].CurrentMember, Time.[Time].CurrentMember.Level.Members),\n"
                + "    Descendants([Time].[1997], [Time].[Month]), \n"
                + "    [Measures].[Store Sales], \n"
                + "    Rank(Time.[Time].CurrentMember, Time.[Time].CurrentMember.Level.Members)\n"
                + " )' \n"
                + "SELECT \n"
                + "  {[Measures].[Test],[Measures].[Store Sales]} ON ROWS, \n"
                + "  Descendants([Time].[1997], [Time].[Month]) ON COLUMNS \n"
                + "FROM Sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Time].[1997].[Q1].[1]}\n"
                + "{[Time].[1997].[Q1].[2]}\n"
                + "{[Time].[1997].[Q1].[3]}\n"
                + "{[Time].[1997].[Q2].[4]}\n"
                + "{[Time].[1997].[Q2].[5]}\n"
                + "{[Time].[1997].[Q2].[6]}\n"
                + "{[Time].[1997].[Q3].[7]}\n"
                + "{[Time].[1997].[Q3].[8]}\n"
                + "{[Time].[1997].[Q3].[9]}\n"
                + "{[Time].[1997].[Q4].[10]}\n"
                + "{[Time].[1997].[Q4].[11]}\n"
                + "{[Time].[1997].[Q4].[12]}\n"
                + "Axis #2:\n"
                + "{[Measures].[Test]}\n"
                + "{[Measures].[Store Sales]}\n"
                + "Row #0: 43,824.36\n"
                + "Row #0: 44,420.51\n"
                + "Row #0: 45,016.66\n"
                + "Row #0: 45,612.81\n"
                + "Row #0: 46,208.95\n"
                + "Row #0: 46,805.10\n"
                + "Row #0: 47,401.25\n"
                + "Row #0: 47,997.40\n"
                + "Row #0: 48,593.55\n"
                + "Row #0: 49,189.70\n"
                + "Row #0: 49,785.85\n"
                + "Row #0: 50,382.00\n"
                + "Row #1: 45,539.69\n"
                + "Row #1: 44,058.79\n"
                + "Row #1: 50,029.87\n"
                + "Row #1: 42,878.25\n"
                + "Row #1: 44,456.29\n"
                + "Row #1: 45,331.73\n"
                + "Row #1: 50,246.88\n"
                + "Row #1: 46,199.04\n"
                + "Row #1: 43,825.97\n"
                + "Row #1: 42,342.27\n"
                + "Row #1: 53,363.71\n"
                + "Row #1: 56,965.64\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLinRegIntercept(Context context) {
        FunctionTest.assertExprReturns(context.getConnectionWithDefaultRole(),
            "LinRegIntercept([Time].[Month].members,"
                + " [Measures].[Unit Sales], [Measures].[Store Sales])",
            -126.65,
            0.50 );

/*
-1#IND missing data
*/
/*
1#INF division by zero
*/
/*
The following table shows query return values from using different
FORMAT_STRING's in an expression involving 'division by zero' (tested on
Intel platforms):

+===========================+=====================+
| Format Strings            | Query Return Values |
+===========================+=====================+
| FORMAT_STRING="           | 1.#INF              |
+===========================+=====================+
| FORMAT_STRING='Standard'  | 1.#J                |
+===========================+=====================+
| FORMAT_STRING='Fixed'     | 1.#J                |
+===========================+=====================+
| FORMAT_STRING='Percent'   | 1#I.NF%             |
+===========================+=====================+
| FORMAT_STRING='Scientific'| 1.JE+00             |
+===========================+=====================+
*/

        // Mondrian can not return "missing data" value -1.#IND
        // empty set
        if ( false ) {
            assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
                "LinRegIntercept({[Time].Parent},"
                    + " [Measures].[Unit Sales], [Measures].[Store Sales])",
                "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)
        }

        // first expr constant
        if ( false ) {
            assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
                "LinRegIntercept([Time].[Month].members,"
                    + " 7, [Measures].[Store Sales])",
                "$7.00" );
        }

        // format does not add '$'
        FunctionTest.assertExprReturns(context.getConnectionWithDefaultRole(),
            "LinRegIntercept([Time].[Month].members,"
                + " 7, [Measures].[Store Sales])",
            7.00,
            0.01 );

        // Mondrian can not return "missing data" value -1.#IND
        // second expr constant
        if ( false ) {
            assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
                "LinRegIntercept([Time].[Month].members,"
                    + " [Measures].[Unit Sales], 4)",
                "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)
        }
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLinRegSlope(Context context) {
        FunctionTest.assertExprReturns(context.getConnectionWithDefaultRole(),
            "LinRegSlope([Time].[Month].members,"
                + " [Measures].[Unit Sales], [Measures].[Store Sales])",
            0.4746,
            0.50 );

        // Mondrian can not return "missing data" value -1.#IND
        // empty set
        if ( false ) {
            assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
                "LinRegSlope({[Time].Parent},"
                    + " [Measures].[Unit Sales], [Measures].[Store Sales])",
                "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)
        }

        // first expr constant
        if ( false ) {
            assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
                "LinRegSlope([Time].[Month].members,"
                    + " 7, [Measures].[Store Sales])",
                "$7.00" );
        }
        // ^^^^
        // copy and paste error

        FunctionTest.assertExprReturns(context.getConnectionWithDefaultRole(),
            "LinRegSlope([Time].[Month].members,"
                + " 7, [Measures].[Store Sales])",
            0.00,
            0.01 );

        // Mondrian can not return "missing data" value -1.#IND
        // second expr constant
        if ( false ) {
            assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
                "LinRegSlope([Time].[Month].members,"
                    + " [Measures].[Unit Sales], 4)",
                "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLinRegPoint(Context context) {
        // NOTE: mdx does not parse
        if ( false ) {
            assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
                "LinRegPoint([Measures].[Unit Sales],"
                    + " [Time].CurrentMember[Time].[Month].members,"
                    + " [Measures].[Unit Sales], [Measures].[Store Sales])",
                "0.4746" );
        }

        // Mondrian can not return "missing data" value -1.#IND
        // empty set
        if ( false ) {
            assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
                "LinRegPoint([Measures].[Unit Sales],"
                    + " {[Time].Parent},"
                    + " [Measures].[Unit Sales], [Measures].[Store Sales])",
                "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)
        }

        // Expected value is wrong
        // zeroth expr constant
        if ( false ) {
            assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
                "LinRegPoint(-1,"
                    + " [Time].[Month].members,"
                    + " 7, [Measures].[Store Sales])", "-127.124" );
        }

        // first expr constant
        if ( false ) {
            assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
                "LinRegPoint([Measures].[Unit Sales],"
                    + " [Time].[Month].members,"
                    + " 7, [Measures].[Store Sales])", "$7.00" );
        }

        // format does not add '$'
        FunctionTest.assertExprReturns(context.getConnectionWithDefaultRole(),
            "LinRegPoint([Measures].[Unit Sales],"
                + " [Time].[Month].members,"
                + " 7, [Measures].[Store Sales])",
            7.00,
            0.01 );

        // Mondrian can not return "missing data" value -1.#IND
        // second expr constant
        if ( false ) {
            assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
                "LinRegPoint([Measures].[Unit Sales],"
                    + " [Time].[Month].members,"
                    + " [Measures].[Unit Sales], 4)",
                "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)
        }
    }

    @Disabled //disabled for CI build
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void _testLinRegR2(Context context) {
        // Why would R2 equal the slope
        if ( false ) {
            assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
                "LinRegR2([Time].[Month].members,"
                    + " [Measures].[Unit Sales], [Measures].[Store Sales])",
                "0.4746" );
        }

        // Mondrian can not return "missing data" value -1.#IND
        // empty set
        if ( false ) {
            assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
                "LinRegR2({[Time].Parent},"
                    + " [Measures].[Unit Sales], [Measures].[Store Sales])",
                "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)
        }

        // first expr constant
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "LinRegR2([Time].[Month].members,"
                + " 7, [Measures].[Store Sales])",
            "$7.00" );

        // Mondrian can not return "missing data" value -1.#IND
        // second expr constant
        if ( false ) {
            assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
                "LinRegR2([Time].[Month].members,"
                    + " [Measures].[Unit Sales], 4)",
                "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)
        }
    }


    @Disabled //disabled for CI build
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void _testLinRegVariance(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "LinRegVariance([Time].[Month].members,"
                + " [Measures].[Unit Sales], [Measures].[Store Sales])",
            "0.4746" );

        // empty set
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "LinRegVariance({[Time].Parent},"
                + " [Measures].[Unit Sales], [Measures].[Store Sales])",
            "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)

        // first expr constant
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "LinRegVariance([Time].[Month].members,"
                + " 7, [Measures].[Store Sales])",
            "$7.00" );

        // second expr constant
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "LinRegVariance([Time].[Month].members,"
                + " [Measures].[Unit Sales], 4)",
            "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)
    }

}
