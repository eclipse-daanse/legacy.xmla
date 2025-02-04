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
package org.eclipse.daanse.olap.function.def.topbottompercentsum;

import static org.opencube.junit5.TestUtil.*;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.result.Result;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class TopBottomPercentSumFunDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBottomPercent(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "BottomPercent(Filter({[Store].[All Stores].[USA].[CA].Children, [Store].[All Stores].[USA].[OR].Children, "
                + "[Store].[All Stores].[USA].[WA].Children}, ([Measures].[Unit Sales] > 0.0)), 100.0, [Measures].[Store "
                + "Sales])",
            "[Store].[USA].[CA].[San Francisco]\n"
                + "[Store].[USA].[WA].[Walla Walla]\n"
                + "[Store].[USA].[WA].[Bellingham]\n"
                + "[Store].[USA].[WA].[Yakima]\n"
                + "[Store].[USA].[CA].[Beverly Hills]\n"
                + "[Store].[USA].[WA].[Spokane]\n"
                + "[Store].[USA].[WA].[Seattle]\n"
                + "[Store].[USA].[WA].[Bremerton]\n"
                + "[Store].[USA].[CA].[San Diego]\n"
                + "[Store].[USA].[CA].[Los Angeles]\n"
                + "[Store].[USA].[OR].[Portland]\n"
                + "[Store].[USA].[WA].[Tacoma]\n"
                + "[Store].[USA].[OR].[Salem]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "BottomPercent({[Promotion Media].[Media Type].members}, 1, [Measures].[Unit Sales])",
            "[Promotion Media].[Radio]\n"
                + "[Promotion Media].[Sunday Paper, Radio, TV]" );
    }

    // todo: test precision

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBottomSum(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "BottomSum({[Promotion Media].[Media Type].members}, 5000, [Measures].[Unit Sales])",
            "[Promotion Media].[Radio]\n"
                + "[Promotion Media].[Sunday Paper, Radio, TV]" );
    }

    /**
     * Tests that TopPercent() operates succesfully on a axis of crossjoined tuples.  previously, this would fail with a
     * ClassCastException in FunUtil.java.  bug 1440306
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTopPercentCrossjoin(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "{TopPercent(Crossjoin([Product].[Product Department].members,\n"
                + "[Time].[1997].children),10,[Measures].[Store Sales])}",
            "{[Product].[Food].[Produce], [Time].[1997].[Q4]}\n"
                + "{[Product].[Food].[Produce], [Time].[1997].[Q1]}\n"
                + "{[Product].[Food].[Produce], [Time].[1997].[Q3]}" );
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTopPercent(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "TopPercent({[Promotion Media].[Media Type].members}, 70, [Measures].[Unit Sales])",
            "[Promotion Media].[No Media]" );
    }

    // todo: test precision

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTopSum(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "TopSum({[Promotion Media].[Media Type].members}, 200000, [Measures].[Unit Sales])",
            "[Promotion Media].[No Media]\n"
                + "[Promotion Media].[Daily Paper, Radio, TV]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTopSumEmpty(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "TopSum(Filter({[Promotion Media].[Media Type].members}, 1=0), "
                + "200000, [Measures].[Unit Sales])",
            "" );
    }


    /**
     * This is a test for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-2157">MONDRIAN-2157</a>
     * <p/>
     * <p>The results should be equivalent either we use aliases or not</p>
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTopPercentWithAlias(Context context) {
        final String queryWithoutAlias =
            "select\n"
                + " {[Measures].[Store Cost]}on rows,\n"
                + " TopPercent([Product].[Brand Name].Members*[Time].[1997].children,"
                + " 50, [Measures].[Unit Sales]) on columns\n"
                + "from Sales";
        String queryWithAlias =
            "with\n"
                + " set [*aaa] as '[Product].[Brand Name].Members*[Time].[1997].children'\n"
                + "select\n"
                + " {[Measures].[Store Cost]}on rows,\n"
                + " TopPercent([*aaa], 50, [Measures].[Unit Sales]) on columns\n"
                + "from Sales";

        final Result result = executeQuery(context.getConnectionWithDefaultRole(), queryWithoutAlias );
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            queryWithAlias,
            TestUtil.toString( result ) );
    }
}
