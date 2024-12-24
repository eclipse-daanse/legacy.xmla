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
package org.eclipse.daanse.olap.function.def.minmax;

import static org.opencube.junit5.TestUtil.assertQueryReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.fun.FunctionTest;


class MinMaxFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMax(Context context) {
        FunctionTest.assertExprReturns(context.getConnection(),
            "MAX({[Store].[All Stores].[USA].children},[Measures].[Store Sales])",
            "263,793.22" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMaxNegative(Context context) {
        // Bug 1771928, "Max() works incorrectly with negative values"
        assertQueryReturns(context.getConnection(),
            "with \n"
                + "  member [Customers].[Neg] as '-1'\n"
                + "  member [Customers].[Min] as 'Min({[Customers].[Neg]})'\n"
                + "  member [Customers].[Max] as 'Max({[Customers].[Neg]})'\n"
                + "select {[Customers].[Neg],[Customers].[Min],[Customers].[Max]} on 0\n"
                + "from Sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Customers].[Neg]}\n"
                + "{[Customers].[Min]}\n"
                + "{[Customers].[Max]}\n"
                + "Row #0: -1\n"
                + "Row #0: -1\n"
                + "Row #0: -1\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMin(Context context) {
        FunctionTest.assertExprReturns(context.getConnection(),
            "MIN({[Store].[All Stores].[USA].children},[Measures].[Store Sales])",
            "142,277.07" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMinTuple(Context context) {
        FunctionTest.assertExprReturns(context.getConnection(),
            "Min([Customers].[All Customers].[USA].Children, ([Measures].[Unit Sales], [Gender].[All Gender].[F]))",
            "33,036" );
    }

}
