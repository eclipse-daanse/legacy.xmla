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
package org.eclipse.daanse.olap.function.def.operators.minus;

import static mondrian.olap.fun.FunctionTest.assertExprReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class MinusOperatorDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMinus_bug1234759(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Customers].[USAMinusMexico]\n"
                + "AS '([Customers].[All Customers].[USA] - [Customers].[All Customers].[Mexico])'\n"
                + "SELECT {[Measures].[Unit Sales]} ON COLUMNS,\n"
                + "{[Customers].[All Customers].[USA], [Customers].[All Customers].[Mexico],\n"
                + "[Customers].[USAMinusMexico]} ON ROWS\n"
                + "FROM [Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "Axis #2:\n"
                + "{[Customers].[Customers].[USA]}\n"
                + "{[Customers].[Customers].[Mexico]}\n"
                + "{[Customers].[Customers].[USAMinusMexico]}\n"
                + "Row #0: 266,773\n"
                + "Row #1: \n"
                + "Row #2: 266,773\n"
                // with bug 1234759, this was null
                + "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMinusAssociativity(Context context) {
        // right-associative would give 11-(7-5) = 9, which is wrong
        assertExprReturns(context.getConnectionWithDefaultRole(), "11-7-5", "-1" );
    }

}
