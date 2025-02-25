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
package org.eclipse.daanse.olap.function.def.crossjoin;

import static mondrian.olap.fun.FunctionTest.assertExprReturns;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertQueryThrows;
import static org.opencube.junit5.TestUtil.hierarchyName;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class CrossJoinFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinNested(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "  CrossJoin(\n"
                + "    CrossJoin(\n"
                + "      [Gender].members,\n"
                + "      [Marital Status].members),\n"
                + "   {[Store], [Store].children})",

            "{[Gender].[Gender].[All Gender], [Marital Status].[Marital Status].[All Marital Status], [Store].[Store].[All Stores]}\n"
                + "{[Gender].[Gender].[All Gender], [Marital Status].[Marital Status].[All Marital Status], [Store].[Store].[Canada]}\n"
                + "{[Gender].[Gender].[All Gender], [Marital Status].[Marital Status].[All Marital Status], [Store].[Store].[Mexico]}\n"
                + "{[Gender].[Gender].[All Gender], [Marital Status].[Marital Status].[All Marital Status], [Store].[Store].[USA]}\n"
                + "{[Gender].[Gender].[All Gender], [Marital Status].[Marital Status].[M], [Store].[Store].[All Stores]}\n"
                + "{[Gender].[Gender].[All Gender], [Marital Status].[Marital Status].[M], [Store].[Store].[Canada]}\n"
                + "{[Gender].[Gender].[All Gender], [Marital Status].[Marital Status].[M], [Store].[Store].[Mexico]}\n"
                + "{[Gender].[Gender].[All Gender], [Marital Status].[Marital Status].[M], [Store].[Store].[USA]}\n"
                + "{[Gender].[Gender].[All Gender], [Marital Status].[Marital Status].[S], [Store].[Store].[All Stores]}\n"
                + "{[Gender].[Gender].[All Gender], [Marital Status].[Marital Status].[S], [Store].[Store].[Canada]}\n"
                + "{[Gender].[Gender].[All Gender], [Marital Status].[Marital Status].[S], [Store].[Store].[Mexico]}\n"
                + "{[Gender].[Gender].[All Gender], [Marital Status].[Marital Status].[S], [Store].[Store].[USA]}\n"
                + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[All Marital Status], [Store].[Store].[All Stores]}\n"
                + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[All Marital Status], [Store].[Store].[Canada]}\n"
                + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[All Marital Status], [Store].[Store].[Mexico]}\n"
                + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[All Marital Status], [Store].[Store].[USA]}\n"
                + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[M], [Store].[Store].[All Stores]}\n"
                + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[M], [Store].[Store].[Canada]}\n"
                + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[M], [Store].[Store].[Mexico]}\n"
                + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[M], [Store].[Store].[USA]}\n"
                + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[S], [Store].[Store].[All Stores]}\n"
                + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[S], [Store].[Store].[Canada]}\n"
                + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[S], [Store].[Store].[Mexico]}\n"
                + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[S], [Store].[Store].[USA]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[All Marital Status], [Store].[Store].[All Stores]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[All Marital Status], [Store].[Store].[Canada]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[All Marital Status], [Store].[Store].[Mexico]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[All Marital Status], [Store].[Store].[USA]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[M], [Store].[Store].[All Stores]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[M], [Store].[Store].[Canada]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[M], [Store].[Store].[Mexico]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[M], [Store].[Store].[USA]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[S], [Store].[Store].[All Stores]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[S], [Store].[Store].[Canada]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[S], [Store].[Store].[Mexico]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[S], [Store].[Store].[USA]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinSingletonTuples(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "CrossJoin({([Gender].[M])}, {([Marital Status].[S])})",
            "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[S]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinSingletonTuplesNested(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "CrossJoin({([Gender].[M])}, CrossJoin({([Marital Status].[S])}, [Store].[Store].children))",
            "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[S], [Store].[Store].[Canada]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[S], [Store].[Store].[Mexico]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[S], [Store].[Store].[USA]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinAsterisk(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "{[Gender].[Gender].[M]} * {[Marital Status].[Marital Status].[S]}",
            "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[S]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinAsteriskTuple(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures].[Unit Sales]} ON COLUMNS, "
                + "NON EMPTY [Store].[All Stores] "
                + " * ([Product].[All Products], [Gender]) "
                + " * [Customers].[All Customers] ON ROWS "
                + "from [Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "Axis #2:\n"
                + "{[Store].[Store].[All Stores], [Product].[Product].[All Products], [Gender].[Gender].[All Gender], [Customers].[Customers].[All Customers]}\n"
                + "Row #0: 266,773\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinAsteriskAssoc(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Order({[Gender].Children} * {[Marital Status].Children} * {[Time].[1997].[Q2].Children},"
                + "[Measures].[Unit Sales])",
            "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[M], [Time].[Time].[1997].[Q2].[4]}\n"
                + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[M], [Time].[Time].[1997].[Q2].[6]}\n"
                + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[M], [Time].[Time].[1997].[Q2].[5]}\n"
                + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[S], [Time].[Time].[1997].[Q2].[4]}\n"
                + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[S], [Time].[Time].[1997].[Q2].[5]}\n"
                + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[S], [Time].[Time].[1997].[Q2].[6]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[M], [Time].[Time].[1997].[Q2].[4]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[M], [Time].[Time].[1997].[Q2].[5]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[M], [Time].[Time].[1997].[Q2].[6]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[S], [Time].[Time].[1997].[Q2].[6]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[S], [Time].[Time].[1997].[Q2].[4]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[S], [Time].[Time].[1997].[Q2].[5]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinAsteriskInsideBraces(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "{[Gender].[M] * [Marital Status].[S] * [Time].[1997].[Q2].Children}",
            "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[S], [Time].[Time].[1997].[Q2].[4]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[S], [Time].[Time].[1997].[Q2].[5]}\n"
                + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[S], [Time].[Time].[1997].[Q2].[6]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossJoinAsteriskQuery(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT {[Measures].members * [1997].children} ON COLUMNS,\n"
                + " {[Store].[USA].children * [Position].[All Position].children} DIMENSION PROPERTIES [Store].[Store SQFT] "
                + "ON ROWS\n"
                + "FROM [HR]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Org Salary], [Time].[Time].[1997].[Q1]}\n"
                + "{[Measures].[Org Salary], [Time].[Time].[1997].[Q2]}\n"
                + "{[Measures].[Org Salary], [Time].[Time].[1997].[Q3]}\n"
                + "{[Measures].[Org Salary], [Time].[Time].[1997].[Q4]}\n"
                + "{[Measures].[Count], [Time].[Time].[1997].[Q1]}\n"
                + "{[Measures].[Count], [Time].[Time].[1997].[Q2]}\n"
                + "{[Measures].[Count], [Time].[Time].[1997].[Q3]}\n"
                + "{[Measures].[Count], [Time].[Time].[1997].[Q4]}\n"
                + "{[Measures].[Number of Employees], [Time].[Time].[1997].[Q1]}\n"
                + "{[Measures].[Number of Employees], [Time].[Time].[1997].[Q2]}\n"
                + "{[Measures].[Number of Employees], [Time].[Time].[1997].[Q3]}\n"
                + "{[Measures].[Number of Employees], [Time].[Time].[1997].[Q4]}\n"
                + "Axis #2:\n"
                + "{[Store].[Store].[USA].[CA], [Position].[Position].[Middle Management]}\n"
                + "{[Store].[Store].[USA].[CA], [Position].[Position].[Senior Management]}\n"
                + "{[Store].[Store].[USA].[CA], [Position].[Position].[Store Full Time Staf]}\n"
                + "{[Store].[Store].[USA].[CA], [Position].[Position].[Store Management]}\n"
                + "{[Store].[Store].[USA].[CA], [Position].[Position].[Store Temp Staff]}\n"
                + "{[Store].[Store].[USA].[OR], [Position].[Position].[Middle Management]}\n"
                + "{[Store].[Store].[USA].[OR], [Position].[Position].[Senior Management]}\n"
                + "{[Store].[Store].[USA].[OR], [Position].[Position].[Store Full Time Staf]}\n"
                + "{[Store].[Store].[USA].[OR], [Position].[Position].[Store Management]}\n"
                + "{[Store].[Store].[USA].[OR], [Position].[Position].[Store Temp Staff]}\n"
                + "{[Store].[Store].[USA].[WA], [Position].[Position].[Middle Management]}\n"
                + "{[Store].[Store].[USA].[WA], [Position].[Position].[Senior Management]}\n"
                + "{[Store].[Store].[USA].[WA], [Position].[Position].[Store Full Time Staf]}\n"
                + "{[Store].[Store].[USA].[WA], [Position].[Position].[Store Management]}\n"
                + "{[Store].[Store].[USA].[WA], [Position].[Position].[Store Temp Staff]}\n"
                + "Row #0: $275.40\n"
                + "Row #0: $275.40\n"
                + "Row #0: $275.40\n"
                + "Row #0: $275.40\n"
                + "Row #0: 27\n"
                + "Row #0: 27\n"
                + "Row #0: 27\n"
                + "Row #0: 27\n"
                + "Row #0: 9\n"
                + "Row #0: 9\n"
                + "Row #0: 9\n"
                + "Row #0: 9\n"
                + "Row #1: $837.00\n"
                + "Row #1: $837.00\n"
                + "Row #1: $837.00\n"
                + "Row #1: $837.00\n"
                + "Row #1: 24\n"
                + "Row #1: 24\n"
                + "Row #1: 24\n"
                + "Row #1: 24\n"
                + "Row #1: 8\n"
                + "Row #1: 8\n"
                + "Row #1: 8\n"
                + "Row #1: 8\n"
                + "Row #2: $1,728.45\n"
                + "Row #2: $1,727.02\n"
                + "Row #2: $1,727.72\n"
                + "Row #2: $1,726.55\n"
                + "Row #2: 357\n"
                + "Row #2: 357\n"
                + "Row #2: 357\n"
                + "Row #2: 357\n"
                + "Row #2: 119\n"
                + "Row #2: 119\n"
                + "Row #2: 119\n"
                + "Row #2: 119\n"
                + "Row #3: $473.04\n"
                + "Row #3: $473.04\n"
                + "Row #3: $473.04\n"
                + "Row #3: $473.04\n"
                + "Row #3: 51\n"
                + "Row #3: 51\n"
                + "Row #3: 51\n"
                + "Row #3: 51\n"
                + "Row #3: 17\n"
                + "Row #3: 17\n"
                + "Row #3: 17\n"
                + "Row #3: 17\n"
                + "Row #4: $401.35\n"
                + "Row #4: $405.73\n"
                + "Row #4: $400.61\n"
                + "Row #4: $402.31\n"
                + "Row #4: 120\n"
                + "Row #4: 120\n"
                + "Row #4: 120\n"
                + "Row #4: 120\n"
                + "Row #4: 40\n"
                + "Row #4: 40\n"
                + "Row #4: 40\n"
                + "Row #4: 40\n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #5: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #6: \n"
                + "Row #7: $1,343.62\n"
                + "Row #7: $1,342.61\n"
                + "Row #7: $1,342.57\n"
                + "Row #7: $1,343.65\n"
                + "Row #7: 279\n"
                + "Row #7: 279\n"
                + "Row #7: 279\n"
                + "Row #7: 279\n"
                + "Row #7: 93\n"
                + "Row #7: 93\n"
                + "Row #7: 93\n"
                + "Row #7: 93\n"
                + "Row #8: $286.74\n"
                + "Row #8: $286.74\n"
                + "Row #8: $286.74\n"
                + "Row #8: $286.74\n"
                + "Row #8: 30\n"
                + "Row #8: 30\n"
                + "Row #8: 30\n"
                + "Row #8: 30\n"
                + "Row #8: 10\n"
                + "Row #8: 10\n"
                + "Row #8: 10\n"
                + "Row #8: 10\n"
                + "Row #9: $333.20\n"
                + "Row #9: $332.65\n"
                + "Row #9: $331.28\n"
                + "Row #9: $332.43\n"
                + "Row #9: 99\n"
                + "Row #9: 99\n"
                + "Row #9: 99\n"
                + "Row #9: 99\n"
                + "Row #9: 33\n"
                + "Row #9: 33\n"
                + "Row #9: 33\n"
                + "Row #9: 33\n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #10: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #11: \n"
                + "Row #12: $2,768.60\n"
                + "Row #12: $2,769.18\n"
                + "Row #12: $2,766.78\n"
                + "Row #12: $2,769.50\n"
                + "Row #12: 579\n"
                + "Row #12: 579\n"
                + "Row #12: 579\n"
                + "Row #12: 579\n"
                + "Row #12: 193\n"
                + "Row #12: 193\n"
                + "Row #12: 193\n"
                + "Row #12: 193\n"
                + "Row #13: $736.29\n"
                + "Row #13: $736.29\n"
                + "Row #13: $736.29\n"
                + "Row #13: $736.29\n"
                + "Row #13: 81\n"
                + "Row #13: 81\n"
                + "Row #13: 81\n"
                + "Row #13: 81\n"
                + "Row #13: 27\n"
                + "Row #13: 27\n"
                + "Row #13: 27\n"
                + "Row #13: 27\n"
                + "Row #14: $674.70\n"
                + "Row #14: $674.54\n"
                + "Row #14: $676.26\n"
                + "Row #14: $676.48\n"
                + "Row #14: 201\n"
                + "Row #14: 201\n"
                + "Row #14: 201\n"
                + "Row #14: 201\n"
                + "Row #14: 67\n"
                + "Row #14: 67\n"
                + "Row #14: 67\n"
                + "Row #14: 67\n" );
    }

    /**
     * Testcase for bug 1889745, "StackOverflowError while resolving crossjoin". The problem occurs when a calculated
     * member that references itself is referenced in a crossjoin.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinResolve(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with\n"
                + "member [Measures].[Filtered Unit Sales] as\n"
                + " 'IIf((([Measures].[Unit Sales] > 50000.0)\n"
                + "      OR ([Product].[Product].CurrentMember.Level.UniqueName <>\n"
                + "          \"[Product].[Product].[Product Family]\")),\n"
                + "      IIf(((Count([Product].[Product].CurrentMember.Children) = 0.0)),\n"
                + "          [Measures].[Unit Sales],\n"
                + "          Sum([Product].[Product].CurrentMember.Children,\n"
                + "              [Measures].[Filtered Unit Sales])),\n"
                + "      NULL)'\n"
                + "select NON EMPTY {crossjoin({[Measures].[Filtered Unit Sales]},\n"
                + "{[Gender].[Gender].[M], [Gender].[Gender].[F]})} ON COLUMNS,\n"
                + "NON EMPTY {[Product].[Product].[All Products]} ON ROWS\n"
                + "from [Sales]\n"
                + "where [Time].[Time].[1997]",
            "Axis #0:\n"
                + "{[Time].[Time].[1997]}\n"
                + "Axis #1:\n"
                + "{[Measures].[Filtered Unit Sales], [Gender].[Gender].[M]}\n"
                + "{[Measures].[Filtered Unit Sales], [Gender].[Gender].[F]}\n"
                + "Axis #2:\n"
                + "{[Product].[Product].[All Products]}\n"
                + "Row #0: 97,126\n"
                + "Row #0: 94,814\n" );
    }

    /**
     * Test case for bug 1911832, "Exception converting immutable list to array in JDK 1.5".
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinOrder(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH\n"
                + "\n"
                + "SET [S1] AS 'CROSSJOIN({[Time].[Time].[1997]}, {[Gender].[Gender].[Gender].MEMBERS})'\n"
                + "\n"
                + "SELECT CROSSJOIN(ORDER([S1], [Measures].[Unit Sales], BDESC),\n"
                + "{[Measures].[Unit Sales]}) ON AXIS(0)\n"
                + "FROM [Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Time].[Time].[1997], [Gender].[Gender].[M], [Measures].[Unit Sales]}\n"
                + "{[Time].[Time].[1997], [Gender].[Gender].[F], [Measures].[Unit Sales]}\n"
                + "Row #0: 135,215\n"
                + "Row #0: 131,558\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinDupHierarchyFails(Context context) {
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] ON COLUMNS,\n"
                + " CrossJoin({[Time].[Quarter].[Q1]}, {[Time].[Month].[5]}) ON ROWS\n"
                + "from [Sales]",
            "Tuple contains more than one member of hierarchy '[Time].[Time]'." );

        // now with Item, for kicks
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] ON COLUMNS,\n"
                + " CrossJoin({[Time].[Quarter].[Q1]}, {[Time].[Month].[5]}).Item(0) ON ROWS\n"
                + "from [Sales]",
            "Tuple contains more than one member of hierarchy '[Time].[Time]'." );

        // same query using explicit tuple
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] ON COLUMNS,\n"
                + " ([Time].[Quarter].[Q1], [Time].[Month].[5]) ON ROWS\n"
                + "from [Sales]",
            "Tuple contains more than one member of hierarchy '[Time].[Time]'." );
    }

    /**
     *
     * Not an error.
     */
    //* Tests cases of different hierarchies in the same dimension. (Compare to {@link #testCrossjoinDupHierarchyFails()}).
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCrossjoinDupDimensionOk(Context context) {
        final String expectedResult =
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "Axis #2:\n"
                + "{[Time].[Time].[1997].[Q1], [Time].[Weekly].[1997].[10]}\n"
                + "Row #0: 4,395\n";
        final String timeWeekly = hierarchyName( "Time", "Weekly" );
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] ON COLUMNS,\n"
                + " CrossJoin({[Time].[Quarter].[Q1]}, {"
                + timeWeekly + ".[1997].[10]}) ON ROWS\n"
                + "from [Sales]",
            expectedResult );

        // now with Item, for kicks
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] ON COLUMNS,\n"
                + " CrossJoin({[Time].[Quarter].[Q1]}, {"
                + timeWeekly + ".[1997].[10]}).Item(0) ON ROWS\n"
                + "from [Sales]",
            expectedResult );

        // same query using explicit tuple
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select [Measures].[Unit Sales] ON COLUMNS,\n"
                + " ([Time].[Quarter].[Q1], "
                + timeWeekly + ".[1997].[10]) ON ROWS\n"
                + "from [Sales]",
            expectedResult );
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testItemTuple(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "CrossJoin([Gender].[All Gender].children, "
                + "[Time].[1997].[Q2].children).Item(0).Item(1).UniqueName",
            "[Time].[Time].[1997].[Q2].[4]" );
    }
}
