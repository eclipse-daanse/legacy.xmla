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
package org.eclipse.daanse.olap.function.def.union;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.executeQuery;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Result;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class UnionFunDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUnionAll(Context context) {
        assertAxisReturns(context.getConnection(),
            "Union({[Gender].[M]}, {[Gender].[F]}, ALL)",
            "[Gender].[M]\n"
                + "[Gender].[F]" ); // order is preserved
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUnionAllTuple(Context context) {
        // With the bug, the last 8 rows are repeated.
        assertQueryReturns(context.getConnection(),
            "with \n"
                + "set [Set1] as 'Crossjoin({[Time].[1997].[Q1]:[Time].[1997].[Q4]},{[Store].[USA].[CA]:[Store].[USA].[OR]})'\n"
                + "set [Set2] as 'Crossjoin({[Time].[1997].[Q2]:[Time].[1997].[Q3]},{[Store].[Mexico].[DF]:[Store].[Mexico]"
                + ".[Veracruz]})'\n"
                + "select \n"
                + "{[Measures].[Unit Sales]} ON COLUMNS,\n"
                + "Union([Set1], [Set2], ALL) ON ROWS\n"
                + "from [Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "Axis #2:\n"
                + "{[Time].[1997].[Q1], [Store].[USA].[CA]}\n"
                + "{[Time].[1997].[Q1], [Store].[USA].[OR]}\n"
                + "{[Time].[1997].[Q2], [Store].[USA].[CA]}\n"
                + "{[Time].[1997].[Q2], [Store].[USA].[OR]}\n"
                + "{[Time].[1997].[Q3], [Store].[USA].[CA]}\n"
                + "{[Time].[1997].[Q3], [Store].[USA].[OR]}\n"
                + "{[Time].[1997].[Q4], [Store].[USA].[CA]}\n"
                + "{[Time].[1997].[Q4], [Store].[USA].[OR]}\n"
                + "{[Time].[1997].[Q2], [Store].[Mexico].[DF]}\n"
                + "{[Time].[1997].[Q2], [Store].[Mexico].[Guerrero]}\n"
                + "{[Time].[1997].[Q2], [Store].[Mexico].[Jalisco]}\n"
                + "{[Time].[1997].[Q2], [Store].[Mexico].[Veracruz]}\n"
                + "{[Time].[1997].[Q3], [Store].[Mexico].[DF]}\n"
                + "{[Time].[1997].[Q3], [Store].[Mexico].[Guerrero]}\n"
                + "{[Time].[1997].[Q3], [Store].[Mexico].[Jalisco]}\n"
                + "{[Time].[1997].[Q3], [Store].[Mexico].[Veracruz]}\n"
                + "Row #0: 16,890\n"
                + "Row #1: 19,287\n"
                + "Row #2: 18,052\n"
                + "Row #3: 15,079\n"
                + "Row #4: 18,370\n"
                + "Row #5: 16,940\n"
                + "Row #6: 21,436\n"
                + "Row #7: 16,353\n"
                + "Row #8: \n"
                + "Row #9: \n"
                + "Row #10: \n"
                + "Row #11: \n"
                + "Row #12: \n"
                + "Row #13: \n"
                + "Row #14: \n"
                + "Row #15: \n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUnion(Context context) {
        assertAxisReturns(context.getConnection(),
            "Union({[Store].[USA], [Store].[USA], [Store].[USA].[OR]}, "
                + "{[Store].[USA].[CA], [Store].[USA]})",
            "[Store].[USA]\n"
                + "[Store].[USA].[OR]\n"
                + "[Store].[USA].[CA]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUnionEmptyBoth(Context context) {
        assertAxisReturns(context.getConnection(),
            "Union({}, {})",
            "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUnionEmptyRight(Context context) {
        assertAxisReturns(context.getConnection(),
            "Union({[Gender].[M]}, {})",
            "[Gender].[M]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUnionTuple(Context context) {
        assertAxisReturns(context.getConnection(),
            "Union({"
                + " ([Gender].[M], [Marital Status].[S]),"
                + " ([Gender].[F], [Marital Status].[S])"
                + "}, {"
                + " ([Gender].[M], [Marital Status].[M]),"
                + " ([Gender].[M], [Marital Status].[S])"
                + "})",

            "{[Gender].[M], [Marital Status].[S]}\n"
                + "{[Gender].[F], [Marital Status].[S]}\n"
                + "{[Gender].[M], [Marital Status].[M]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUnionTupleDistinct(Context context) {
        assertAxisReturns(context.getConnection(),
            "Union({"
                + " ([Gender].[M], [Marital Status].[S]),"
                + " ([Gender].[F], [Marital Status].[S])"
                + "}, {"
                + " ([Gender].[M], [Marital Status].[M]),"
                + " ([Gender].[M], [Marital Status].[S])"
                + "}, Distinct)",

            "{[Gender].[M], [Marital Status].[S]}\n"
                + "{[Gender].[F], [Marital Status].[S]}\n"
                + "{[Gender].[M], [Marital Status].[M]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUnionQuery(Context context) {
        Result result = executeQuery(context.getConnection(),
            "select {[Measures].[Unit Sales], "
                + "[Measures].[Store Cost], "
                + "[Measures].[Store Sales]} on columns,\n"
                + " Hierarchize(\n"
                + "   Union(\n"
                + "     Crossjoin(\n"
                + "       Crossjoin([Gender].[All Gender].children,\n"
                + "                 [Marital Status].[All Marital Status].children),\n"
                + "       Crossjoin([Customers].[All Customers].children,\n"
                + "                 [Product].[All Products].children) ),\n"
                + "     Crossjoin({([Gender].[All Gender].[M], [Marital Status].[All Marital Status].[M])},\n"
                + "       Crossjoin(\n"
                + "         [Customers].[All Customers].[USA].children,\n"
                + "         [Product].[All Products].children) ) )) on rows\n"
                + "from Sales where ([Time].[1997])" );
        final Axis rowsAxis = result.getAxes()[ 1 ];
        assertEquals( 45, rowsAxis.getPositions().size() );
    }


}
