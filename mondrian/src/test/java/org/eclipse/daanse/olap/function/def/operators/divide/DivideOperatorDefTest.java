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
package org.eclipse.daanse.olap.function.def.operators.divide;

import static mondrian.olap.fun.FunctionTest.NullNumericExpr;
import static mondrian.olap.fun.FunctionTest.assertExprReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestConfig;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class DivideOperatorDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDivide(Context context) {
        assertExprReturns(context.getConnection(), "10 / 5", "2" );
        assertExprReturns(context.getConnection(), NullNumericExpr + " / - 2", "" );
        assertExprReturns(context.getConnection(), NullNumericExpr + " / " + NullNumericExpr, "" );

        boolean origNullDenominatorProducesNull =
            context.getConfig().nullDenominatorProducesNull();
        try {
            // default behavior
            ((TestConfig)context.getConfig()).setNullDenominatorProducesNull(false);

            assertExprReturns(context.getConnection(), "-2 / " + NullNumericExpr, "Infinity" );
            assertExprReturns(context.getConnection(), "0 / 0", "NaN" );
            assertExprReturns(context.getConnection(), "-3 / (2 - 2)", "-Infinity" );

            assertExprReturns(context.getConnection(), "NULL/1", "" );
            assertExprReturns(context.getConnection(), "NULL/NULL", "" );
            assertExprReturns(context.getConnection(), "1/NULL", "Infinity" );

            // when NullOrZeroDenominatorProducesNull is set to true
            ((TestConfig)context.getConfig()).setNullDenominatorProducesNull( true );

            assertExprReturns(context.getConnection(), "-2 / " + NullNumericExpr, "" );
            assertExprReturns(context.getConnection(), "0 / 0", "NaN" );
            assertExprReturns(context.getConnection(), "-3 / (2 - 2)", "-Infinity" );

            assertExprReturns(context.getConnection(), "NULL/1", "" );
            assertExprReturns(context.getConnection(), "NULL/NULL", "" );
            assertExprReturns(context.getConnection(), "1/NULL", "" );
        } finally {
            ((TestConfig)context.getConfig()).setNullDenominatorProducesNull( origNullDenominatorProducesNull );
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDividePrecedence(Context context) {
        assertExprReturns(context.getConnection(), "24 / 4 / 2 * 10 - -1", "31" );
    }

}
