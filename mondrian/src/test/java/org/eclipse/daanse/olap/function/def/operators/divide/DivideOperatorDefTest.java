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

import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class DivideOperatorDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDivide(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(), "10 / 5", "2" );
        assertExprReturns(context.getConnectionWithDefaultRole(), NullNumericExpr + " / - 2", "" );
        assertExprReturns(context.getConnectionWithDefaultRole(), NullNumericExpr + " / " + NullNumericExpr, "" );

        boolean origNullDenominatorProducesNull =
            context.getConfigValue(ConfigConstants.NULL_DENOMINATOR_PRODUCES_NULL, ConfigConstants.NULL_DENOMINATOR_PRODUCES_NULL_DEFAULT_VALUE, Boolean.class);
        try {
            // default behavior
            ((TestContextImpl)context).setNullDenominatorProducesNull(false);

            assertExprReturns(context.getConnectionWithDefaultRole(), "-2 / " + NullNumericExpr, "Infinity" );
            assertExprReturns(context.getConnectionWithDefaultRole(), "0 / 0", "NaN" );
            assertExprReturns(context.getConnectionWithDefaultRole(), "-3 / (2 - 2)", "-Infinity" );

            assertExprReturns(context.getConnectionWithDefaultRole(), "NULL/1", "" );
            assertExprReturns(context.getConnectionWithDefaultRole(), "NULL/NULL", "" );
            assertExprReturns(context.getConnectionWithDefaultRole(), "1/NULL", "Infinity" );

            // when NullOrZeroDenominatorProducesNull is set to true
            ((TestContextImpl)context).setNullDenominatorProducesNull( true );

            assertExprReturns(context.getConnectionWithDefaultRole(), "-2 / " + NullNumericExpr, "" );
            assertExprReturns(context.getConnectionWithDefaultRole(), "0 / 0", "NaN" );
            assertExprReturns(context.getConnectionWithDefaultRole(), "-3 / (2 - 2)", "-Infinity" );

            assertExprReturns(context.getConnectionWithDefaultRole(), "NULL/1", "" );
            assertExprReturns(context.getConnectionWithDefaultRole(), "NULL/NULL", "" );
            assertExprReturns(context.getConnectionWithDefaultRole(), "1/NULL", "" );
        } finally {
            ((TestContextImpl)context).setNullDenominatorProducesNull( origNullDenominatorProducesNull );
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDividePrecedence(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(), "24 / 4 / 2 * 10 - -1", "31" );
    }

}
