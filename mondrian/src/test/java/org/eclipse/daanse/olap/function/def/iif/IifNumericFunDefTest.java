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
package org.eclipse.daanse.olap.function.def.iif;

import static mondrian.olap.fun.FunctionTest.assertExprReturns;
import static org.opencube.junit5.TestUtil.*;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class IifNumericFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIIfNumeric(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "IIf(([Measures].[Unit Sales],[Product].[Drink].[Alcoholic Beverages].[Beer and Wine]) > 100, 45, 32)",
            "45" );

        // Compare two members. The system needs to figure out that they are
        // both numeric, and use the right overloaded version of ">", otherwise
        // we'll get a ClassCastException at runtime.
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "IIf([Measures].[Unit Sales] > [Measures].[Store Sales], 45, 32)",
            "32" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIIfWithNullAndNumber(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "IIf(([Measures].[Unit Sales],[Product].[Drink].[Alcoholic Beverages].[Beer and Wine]) > 100, null,20)",
            "" );
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "IIf(([Measures].[Unit Sales],[Product].[Drink].[Alcoholic Beverages].[Beer and Wine]) > 100, 20,null)",
            "20" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIifFWithBooleanBooleanAndNumericParameterForReturningTruePart(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT Filter(Store.allmembers, "
                + "iif(measures.profit < 400000,"
                + "[store].currentMember.NAME = \"USA\", 0)) on 0 FROM SALES",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Store].[USA]}\n"
                + "Row #0: 266,773\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIifWithBooleanBooleanAndNumericParameterForReturningFalsePart(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT Filter([Store].[USA].[CA].[Beverly Hills].children, "
                + "iif(measures.profit > 400000,"
                + "[store].currentMember.NAME = \"USA\", 1)) on 0 FROM SALES",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Store].[USA].[CA].[Beverly Hills].[Store 6]}\n"
                + "Row #0: 21,333\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIIFWithBooleanBooleanAndNumericParameterForReturningZero(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT Filter(Store.allmembers, "
                + "iif(measures.profit > 400000,"
                + "[store].currentMember.NAME = \"USA\", 0)) on 0 FROM SALES",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n" );
    }

}
