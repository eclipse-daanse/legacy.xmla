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
package org.eclipse.daanse.olap.function.def.operators.multiply;

import static mondrian.olap.fun.FunctionTest.NullNumericExpr;
import static mondrian.olap.fun.FunctionTest.assertExprReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class MultiplyOperatorDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMultiply(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(), "4*7", "28" );
        assertExprReturns(context.getConnectionWithDefaultRole(), "5 * " + NullNumericExpr, "" ); // 5 * null --> null
        assertExprReturns(context.getConnectionWithDefaultRole(), NullNumericExpr + " * - 2", "" );
        assertExprReturns(context.getConnectionWithDefaultRole(), NullNumericExpr + " - " + NullNumericExpr, "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMultiplyPrecedence(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(), "3 + 4 * 5 + 6", "29" );
        assertExprReturns(context.getConnectionWithDefaultRole(), "5 * 24 / 4 * 2", "60" );
        assertExprReturns(context.getConnectionWithDefaultRole(), "48 / 4 / 2", "6" );
    }

    /**
     * Bug 774807 caused expressions to be mistaken for the crossjoin operator.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMultiplyBug774807(Context context) {
        final String desiredResult =
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Store].[All Stores]}\n"
                + "Axis #2:\n"
                + "{[Measures].[Store Sales]}\n"
                + "{[Measures].[A]}\n"
                + "Row #0: 565,238.13\n"
                + "Row #1: 319,494,143,605.90\n";
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Measures].[A] AS\n"
                + " '([Measures].[Store Sales] * [Measures].[Store Sales])'\n"
                + "SELECT {[Store]} ON COLUMNS,\n"
                + " {[Measures].[Store Sales], [Measures].[A]} ON ROWS\n"
                + "FROM Sales", desiredResult );
        // as above, no parentheses
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Measures].[A] AS\n"
                + " '[Measures].[Store Sales] * [Measures].[Store Sales]'\n"
                + "SELECT {[Store]} ON COLUMNS,\n"
                + " {[Measures].[Store Sales], [Measures].[A]} ON ROWS\n"
                + "FROM Sales", desiredResult );
        // as above, plus 0
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Measures].[A] AS\n"
                + " '[Measures].[Store Sales] * [Measures].[Store Sales] + 0'\n"
                + "SELECT {[Store]} ON COLUMNS,\n"
                + " {[Measures].[Store Sales], [Measures].[A]} ON ROWS\n"
                + "FROM Sales", desiredResult );
    }

}
