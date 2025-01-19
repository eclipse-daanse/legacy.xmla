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
package org.eclipse.daanse.olap.function.def.aggregate.count;

import static mondrian.olap.fun.FunctionTest.allHiersExcept;
import static mondrian.olap.fun.FunctionTest.assertExprReturns;
import static org.opencube.junit5.TestUtil.assertExprDependsOn;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.hierarchyName;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class CountFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCount(Context context) {
        assertExprDependsOn(context.getConnectionWithDefaultRole(),
            "count(Crossjoin([Store].[All Stores].[USA].Children, {[Gender].children}), INCLUDEEMPTY)",
            "{[Gender]}" );

        String s1 = allHiersExcept( "[Store]" );
        assertExprDependsOn(context.getConnectionWithDefaultRole(),
            "count(Crossjoin([Store].[All Stores].[USA].Children, "
                + "{[Gender].children}), EXCLUDEEMPTY)",
            s1 );

        assertExprReturns(context.getConnectionWithDefaultRole(),
            "count({[Promotion Media].[Media Type].members})", "14" );

        // applied to an empty set
        assertExprReturns(context.getConnectionWithDefaultRole(), "count({[Gender].Parent}, IncludeEmpty)", "0" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCountExcludeEmpty(Context context) {
        String s1 = allHiersExcept( "[Store]" );
        assertExprDependsOn(context.getConnectionWithDefaultRole(),
            "count(Crossjoin([Store].[USA].Children, {[Gender].children}), EXCLUDEEMPTY)",
            s1 );

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Measures].[Promo Count] as \n"
                + " ' Count(Crossjoin({[Measures].[Unit Sales]},\n"
                + " {[Promotion Media].[Media Type].members}), EXCLUDEEMPTY)'\n"
                + "select {[Measures].[Unit Sales], [Measures].[Promo Count]} on columns,\n"
                + " {[Product].[Drink].[Beverages].[Carbonated Beverages].[Soda].children} on rows\n"
                + "from Sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "{[Measures].[Promo Count]}\n"
                + "Axis #2:\n"
                + "{[Product].[Drink].[Beverages].[Carbonated Beverages].[Soda].[Excellent]}\n"
                + "{[Product].[Drink].[Beverages].[Carbonated Beverages].[Soda].[Fabulous]}\n"
                + "{[Product].[Drink].[Beverages].[Carbonated Beverages].[Soda].[Skinner]}\n"
                + "{[Product].[Drink].[Beverages].[Carbonated Beverages].[Soda].[Token]}\n"
                + "{[Product].[Drink].[Beverages].[Carbonated Beverages].[Soda].[Washington]}\n"
                + "Row #0: 738\n"
                + "Row #0: 14\n"
                + "Row #1: 632\n"
                + "Row #1: 13\n"
                + "Row #2: 655\n"
                + "Row #2: 14\n"
                + "Row #3: 735\n"
                + "Row #3: 14\n"
                + "Row #4: 647\n"
                + "Row #4: 12\n" );

        // applied to an empty set
        assertExprReturns(context.getConnectionWithDefaultRole(), "count({[Gender].Parent}, ExcludeEmpty)", "0" );
    }

    /**
     * Tests that the 'null' value is regarded as empty, even if the underlying cell has fact table rows.
     *
     * <p>For a fuller test case, see

     */
    // {@link mondrian.xmla.XmlaCognosTest#testCognosMDXSuiteConvertedAdventureWorksToFoodMart_015()}
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCountExcludeEmptyNull(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Measures].[Foo] AS\n"
                + "    Iif("
                + hierarchyName( "Time", "Time" )
                + ".CurrentMember.Name = 'Q2', 1, NULL)\n"
                + "  MEMBER [Measures].[Bar] AS\n"
                + "    Iif("
                + hierarchyName( "Time", "Time" )
                + ".CurrentMember.Name = 'Q2', 1, 0)\n"
                + "  Member [Time].[Time].[CountExc] AS\n"
                + "    Count([Time].[1997].Children, EXCLUDEEMPTY),\n"
                + "    SOLVE_ORDER = 2\n"
                + "  Member [Time].[Time].[CountInc] AS\n"
                + "    Count([Time].[1997].Children, INCLUDEEMPTY),\n"
                + "    SOLVE_ORDER = 2\n"
                + "SELECT {[Measures].[Foo],\n"
                + "   [Measures].[Bar],\n"
                + "   [Measures].[Unit Sales]} ON 0,\n"
                + "  {[Time].[1997].Children,\n"
                + "   [Time].[CountExc],\n"
                + "   [Time].[CountInc]} ON 1\n"
                + "FROM [Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Foo]}\n"
                + "{[Measures].[Bar]}\n"
                + "{[Measures].[Unit Sales]}\n"
                + "Axis #2:\n"
                + "{[Time].[1997].[Q1]}\n"
                + "{[Time].[1997].[Q2]}\n"
                + "{[Time].[1997].[Q3]}\n"
                + "{[Time].[1997].[Q4]}\n"
                + "{[Time].[CountExc]}\n"
                + "{[Time].[CountInc]}\n"
                + "Row #0: \n"
                + "Row #0: 0\n"
                + "Row #0: 66,291\n"
                + "Row #1: 1\n"
                + "Row #1: 1\n"
                + "Row #1: 62,610\n"
                + "Row #2: \n"
                + "Row #2: 0\n"
                + "Row #2: 65,848\n"
                + "Row #3: \n"
                + "Row #3: 0\n"
                + "Row #3: 72,024\n"
                + "Row #4: 1\n"
                + "Row #4: 4\n"
                + "Row #4: 4\n"
                + "Row #5: 4\n"
                + "Row #5: 4\n"
                + "Row #5: 4\n" );
    }

    /**
     * Testcase for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-710">
     * bug MONDRIAN-710, "Count with ExcludeEmpty throws an exception when the cube does not have a
     * factCountMeasure"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCountExcludeEmptyOnCubeWithNoCountFacts(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH "
                + "  MEMBER [Measures].[count] AS '"
                + "    COUNT([Store Type].[Store Type].MEMBERS, EXCLUDEEMPTY)'"
                + " SELECT "
                + "  {[Measures].[count]} ON AXIS(0)"
                + " FROM [Warehouse]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[count]}\n"
                + "Row #0: 5\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCountExcludeEmptyOnVirtualCubeWithNoCountFacts(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH "
                + "  MEMBER [Measures].[count] AS '"
                + "    COUNT([Store].MEMBERS, EXCLUDEEMPTY)'"
                + " SELECT "
                + "  {[Measures].[count]} ON AXIS(0)"
                + " FROM [Warehouse and Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[count]}\n"
                + "Row #0: 31\n" );
    }

    // todo: testCountNull, testCountNoExp

}
