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
package org.eclipse.daanse.olap.function.def.member.validmeasure;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.fun.FunctionTest;


class ValidMeasureFunDefTest {

    /**
     * Tests the <code>ValidMeasure</code> function.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testValidMeasure(Context context) {
        TestUtil.assertQueryReturns(context.getConnection(),
            "with\n"
                + "member measures.[with VM] as 'validmeasure([measures].[unit sales])'\n"
                + "select { measures.[with VM]} on 0,\n"
                + "[Warehouse].[Country].members on 1 from [warehouse and sales]\n",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[with VM]}\n"
                + "Axis #2:\n"
                + "{[Warehouse].[Canada]}\n"
                + "{[Warehouse].[Mexico]}\n"
                + "{[Warehouse].[USA]}\n"
                + "Row #0: 266,773\n"
                + "Row #1: 266,773\n"
                + "Row #2: 266,773\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void _testValidMeasureNonEmpty(Context context) {
        // Note that [with VM2] is NULL where it needs to be - and therefore
        // does not prevent NON EMPTY from eliminating empty rows.
        TestUtil.assertQueryReturns(context.getConnection(),
            "with set [Foo] as ' Crossjoin({[Time].Children}, {[Measures].[Warehouse Sales]}) '\n"
                + " member [Measures].[with VM] as 'ValidMeasure([Measures].[Unit Sales])'\n"
                + " member [Measures].[with VM2] as 'Iif(Count(Filter([Foo], not isempty([Measures].CurrentMember))) > 0, "
                + "ValidMeasure([Measures].[Unit Sales]), NULL)'\n"
                + "select NON EMPTY Crossjoin({[Time].Children}, {[Measures].[with VM2], [Measures].[Warehouse Sales]}) ON "
                + "COLUMNS,\n"
                + "  NON EMPTY {[Warehouse].[All Warehouses].[USA].[WA].Children} ON ROWS\n"
                + "from [Warehouse and Sales]\n"
                + "where [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good].[Good Light "
                + "Beer]",
            "Axis #0:\n"
                + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good].[Good Light Beer]}\n"
                + "Axis #1:\n"
                + "{[Time].[1997].[Q1], [Measures].[with VM2]}\n"
                + "{[Time].[1997].[Q1], [Measures].[Warehouse Sales]}\n"
                + "{[Time].[1997].[Q2], [Measures].[with VM2]}\n"
                + "{[Time].[1997].[Q2], [Measures].[Warehouse Sales]}\n"
                + "{[Time].[1997].[Q3], [Measures].[with VM2]}\n"
                + "{[Time].[1997].[Q4], [Measures].[with VM2]}\n"
                + "Axis #2:\n"
                + "{[Warehouse].[USA].[WA].[Seattle]}\n"
                + "{[Warehouse].[USA].[WA].[Tacoma]}\n"
                + "{[Warehouse].[USA].[WA].[Yakima]}\n"
                + "Row #0: 26\n"
                + "Row #0: 34.793\n"
                + "Row #0: 25\n"
                + "Row #0: \n"
                + "Row #0: 36\n"
                + "Row #0: 28\n"
                + "Row #1: 26\n"
                + "Row #1: \n"
                + "Row #1: 25\n"
                + "Row #1: 64.615\n"
                + "Row #1: 36\n"
                + "Row #1: 28\n"
                + "Row #2: 26\n"
                + "Row #2: 79.657\n"
                + "Row #2: 25\n"
                + "Row #2: \n"
                + "Row #2: 36\n"
                + "Row #2: 28\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testValidMeasureTupleHasAnotherMember(Context context) {
        TestUtil.assertQueryReturns(context.getConnection(),
            "with\n"
                + "member measures.[with VM] as 'validmeasure(([measures].[unit sales],[customers].[all customers]))'\n"
                + "select { measures.[with VM]} on 0,\n"
                + "[Warehouse].[Country].members on 1 from [warehouse and sales]\n",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[with VM]}\n"
                + "Axis #2:\n"
                + "{[Warehouse].[Canada]}\n"
                + "{[Warehouse].[Mexico]}\n"
                + "{[Warehouse].[USA]}\n"
                + "Row #0: 266,773\n"
                + "Row #1: 266,773\n"
                + "Row #2: 266,773\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testValidMeasureDepends(Context context) {
        Connection connection = context.getConnection();
        String s12 = FunctionTest.allHiersExcept( "[Measures]" );
        TestUtil.assertExprDependsOn(connection,
            "ValidMeasure([Measures].[Unit Sales])", s12 );

        String s11 = FunctionTest.allHiersExcept( "[Measures]", "[Time]" );
        TestUtil.assertExprDependsOn(connection,
            "ValidMeasure(([Measures].[Unit Sales], [Time].[1997].[Q1]))", s11 );

        String s1 = FunctionTest.allHiersExcept( "[Measures]" );
        TestUtil.assertExprDependsOn(connection,
            "ValidMeasure(([Measures].[Unit Sales], "
                + "[Time].[Time].CurrentMember.Parent))",
            s1 );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testValidMeasureNonVirtualCube(Context context) {
        // verify ValidMeasure used outside of a virtual cube
        // is effectively a no-op.
        Connection connection = context.getConnection();
        TestUtil.assertQueryReturns(connection,
            "with member measures.vm as 'ValidMeasure(measures.[Store Sales])'"
                + " select measures.[vm] on 0 from Sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[vm]}\n"
                + "Row #0: 565,238.13\n" );
        TestUtil.assertQueryReturns(connection,
            "with member measures.vm as 'ValidMeasure((gender.f, measures.[Store Sales]))'"
                + " select measures.[vm] on 0 from Sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[vm]}\n"
                + "Row #0: 280,226.21\n" );
    }

    /**
     * This is a test for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-2109">MONDRIAN-2109</a>
     *
     * <p>We can't allow calculated members in ValidMeasure so a proper message
     * must be returned.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testValidMeasureCalculatedMemberMeasure(Context context) {
        // Check for failure.
        TestUtil.assertQueryThrows(context,
            "with member measures.calc as 'measures.[Warehouse sales]' \n"
                + "member measures.vm as 'ValidMeasure(measures.calc)' \n"
                + "select from [warehouse and sales]\n"
                + "where (measures.vm ,gender.f) \n",
            "The function ValidMeasure cannot be used with the measure '[Measures].[calc]' because it is a calculated "
                + "member." );
        // Check the working version
        TestUtil.assertQueryReturns(context.getConnection(),
            "with \n"
                + "member measures.vm as 'ValidMeasure(measures.[warehouse sales])' \n"
                + "select from [warehouse and sales] where (measures.vm, gender.f) \n",
            "Axis #0:\n"
                + "{[Measures].[vm], [Gender].[F]}\n"
                + "196,770.888" );
    }

}
