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
package org.eclipse.daanse.olap.function.def.operators.plus;

import static mondrian.olap.fun.FunctionTest.NullNumericExpr;
import static mondrian.olap.fun.FunctionTest.allHiersExcept;
import static mondrian.olap.fun.FunctionTest.assertExprReturns;
import static org.opencube.junit5.TestUtil.assertExprDependsOn;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class PlusOperatorDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testPlus(Context context) {
        assertExprDependsOn(context.getConnectionWithDefaultRole(), "1 + 2", "{}" );
        String s1 = allHiersExcept( "[Measures]", "[Gender].[Gender]" );
        assertExprDependsOn(context.getConnectionWithDefaultRole(),
            "([Measures].[Unit Sales], [Gender].[F]) + 2", s1 );

        assertExprReturns(context.getConnectionWithDefaultRole(), "1+2", "3" );
        assertExprReturns(context.getConnectionWithDefaultRole(), "5 + " + NullNumericExpr, "5" ); // 5 + null --> 5
        assertExprReturns(context.getConnectionWithDefaultRole(), NullNumericExpr + " + " + NullNumericExpr, "" );
        assertExprReturns(context.getConnectionWithDefaultRole(), NullNumericExpr + " + 0", "0" );
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testPlus_NULL_plus_1(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(),  "null + 1", "1" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testPlus_NULL_plus_0(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(),  "null + 0", "0" );
    }
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testPlus_NULL_plus_NULL(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(),  "null + null", "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMinus(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(), "1-3", "-2" );
        assertExprReturns(context.getConnectionWithDefaultRole(), "5 - " + NullNumericExpr, "5" ); // 5 - null --> 5
        assertExprReturns(context.getConnectionWithDefaultRole(), NullNumericExpr + " - - 2", "2" );
        assertExprReturns(context.getConnectionWithDefaultRole(), NullNumericExpr + " - " + NullNumericExpr, "" );
    }

}
