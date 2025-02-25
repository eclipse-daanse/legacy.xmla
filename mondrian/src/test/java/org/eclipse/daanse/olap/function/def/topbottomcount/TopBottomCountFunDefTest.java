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
package org.eclipse.daanse.olap.function.def.topbottomcount;

import static mondrian.olap.fun.FunctionTest.allHiersExcept;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertSetExprDependsOn;
import static org.opencube.junit5.TestUtil.executeQuery;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.result.Result;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class TopBottomCountFunDefTest {

    private static final Logger LOGGER = LoggerFactory.getLogger( TopBottomCountFunDefTest.class );

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBottomCount(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "BottomCount({[Promotion Media].[Media Type].members}, 2, [Measures].[Unit Sales])",
            "[Promotion Media].[Promotion Media].[Radio]\n"
                + "[Promotion Media].[Promotion Media].[Sunday Paper, Radio, TV]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBottomCountUnordered(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "BottomCount({[Promotion Media].[Media Type].members}, 2)",
            "[Promotion Media].[Promotion Media].[Sunday Paper, Radio, TV]\n"
                + "[Promotion Media].[Promotion Media].[TV]" );
    }



    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTopCount(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "TopCount({[Promotion Media].[Media Type].members}, 2, [Measures].[Unit Sales])",
            "[Promotion Media].[Promotion Media].[No Media]\n"
                + "[Promotion Media].[Promotion Media].[Daily Paper, Radio, TV]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTopCountUnordered(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "TopCount({[Promotion Media].[Media Type].members}, 2)",
            "[Promotion Media].[Promotion Media].[Bulk Mail]\n"
                + "[Promotion Media].[Promotion Media].[Cash Register Handout]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTopCountTuple(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "TopCount([Customers].[Name].members,2,(Time.[1997].[Q1],[Measures].[Store Sales]))",
            "[Customers].[Customers].[USA].[WA].[Spokane].[Grace McLaughlin]\n"
                + "[Customers].[Customers].[USA].[WA].[Spokane].[Matt Bellah]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTopCountEmpty(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "TopCount(Filter({[Promotion Media].[Media Type].members}, 1=0), 2, [Measures].[Unit Sales])",
            "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTopCountDepends(Context context) {
        Connection connection = context.getConnectionWithDefaultRole();
        checkTopBottomCountPercentDepends(connection, "TopCount" );
        checkTopBottomCountPercentDepends(connection, "TopPercent" );
        checkTopBottomCountPercentDepends(connection, "TopSum" );
        checkTopBottomCountPercentDepends(connection, "BottomCount" );
        checkTopBottomCountPercentDepends(connection, "BottomPercent" );
        checkTopBottomCountPercentDepends(connection, "BottomSum" );
    }

    private void checkTopBottomCountPercentDepends(Connection connection, String fun) {
        String s1 =
            allHiersExcept( "[Measures]", "[Promotion Media].[Promotion Media]" );
        assertSetExprDependsOn(connection,
            fun
                + "({[Promotion Media].[Promotion Media].[Media Type].members}, "
                + "2, [Measures].[Unit Sales])",
            s1 );

        if ( fun.endsWith( "Count" ) ) {
            assertSetExprDependsOn(connection,
                fun + "({[Promotion Media].[Promotion Media].[Media Type].members}, 2)",
                "{}" );
        }
    }

    /**
     * Tests TopCount applied to a large result set.
     *
     * <p>Before optimizing (see FunUtil.partialSort), on a 2-core 32-bit 2.4GHz
     * machine, the 1st query took 14.5 secs, the 2nd query took 5.0 secs. After optimizing, who knows?
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTopCountHuge(Context context) {
        // TODO convert printfs to trace
        final String query =
            "SELECT [Measures].[Store Sales] ON 0,\n"
                + "TopCount([Time].[Month].members * "
                + "[Customers].[Name].members, 3, [Measures].[Store Sales]) ON 1\n"
                + "FROM [Sales]";
        final String desiredResult =
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Store Sales]}\n"
                + "Axis #2:\n"
                + "{[Time].[Time].[1997].[Q1].[3], [Customers].[Customers].[USA].[WA].[Spokane].[George Todero]}\n"
                + "{[Time].[Time].[1997].[Q3].[7], [Customers].[Customers].[USA].[WA].[Spokane].[James Horvat]}\n"
                + "{[Time].[Time].[1997].[Q4].[11], [Customers].[Customers].[USA].[WA].[Olympia].[Charles Stanley]}\n"
                + "Row #0: 234.83\n"
                + "Row #1: 199.46\n"
                + "Row #2: 191.90\n";
        long now = System.currentTimeMillis();
        assertQueryReturns(context.getConnectionWithDefaultRole(), query, desiredResult );
        LOGGER.info( "first query took " + ( System.currentTimeMillis() - now ) );
        now = System.currentTimeMillis();
        assertQueryReturns(context.getConnectionWithDefaultRole(), query, desiredResult );
        LOGGER.info( "second query took " + ( System.currentTimeMillis() - now ) );
    }


    /**
     * This is a test for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1187">MONDRIAN-1187</a>
     * <p/>
     * <p>The results should be equivalent</p>
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMondrian_1187(Context context) {
        final String queryWithoutAlias =
            "WITH\n" + "SET [Top Count] AS\n"
                + "{\n" + "TOPCOUNT(\n" + "DISTINCT([Customers].[Name].Members),\n"
                + "5,\n" + "[Measures].[Unit Sales]\n" + ")\n" + "}\n" + "SELECT\n"
                + "[Top Count] * [Measures].[Unit Sales] on 0\n" + "FROM [Sales]\n"
                + "WHERE [Time].[1997].[Q1].[1] : [Time].[1997].[Q3].[8]";
        String queryWithAlias =
            "SELECT\n"
                + "TOPCOUNT( DISTINCT( [Customers].[Name].Members), 5, [Measures].[Unit Sales]) * [Measures].[Unit Sales] on "
                + "0\n"
                + "FROM [Sales]\n"
                + "WHERE [Time].[1997].[Q1].[1]:[Time].[1997].[Q3].[8]";
        final Result result = executeQuery(context.getConnectionWithDefaultRole(), queryWithoutAlias );
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            queryWithAlias,
            TestUtil.toString(result));
    }


}
