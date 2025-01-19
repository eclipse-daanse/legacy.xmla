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
import static org.opencube.junit5.TestUtil.assertAxisReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class IifFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIIfMember(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "IIf(1 > 2,[Store].[USA],[Store].[Canada].[BC])",
            "[Store].[Canada].[BC]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIIfLevel(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "IIf(1 > 2, [Store].[Store Country],[Store].[Store City]).Name",
            "Store City" );
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIIfHierarchy(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "IIf(1 > 2, [Time], [Store]).Name",
            "Store" );

        // Call Iif(<Logical>, <Dimension>, <Hierarchy>). Argument #3, the
        // hierarchy [Time.Weekly] is implicitly converted to
        // the dimension [Time] to match argument #2 which is a dimension.
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "IIf(1 > 2, [Time], [Time.Weekly]).Name",
            "Time" );
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIIfDimension(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "IIf(1 > 2, [Store], [Time]).Name",
            "Time" );
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIIfSet(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "IIf(1 > 2, {[Store].[USA], [Store].[USA].[CA]}, {[Store].[Mexico], [Store].[USA].[OR]})",
            "[Store].[Mexico]\n"
                + "[Store].[USA].[OR]" );
    }

    // MONDRIAN-2408 - Consumer wants ITERABLE or ANY in CrossJoinFunDef.compileCall(ResolvedFunCall, ExpCompiler)
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIIfSetType_InCrossJoin(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "CROSSJOIN([Store Type].[Deluxe Supermarket],IIf(1 = 1, {[Store].[USA], [Store].[USA].[CA]}, {[Store].[Mexico],"
                + " [Store].[USA].[OR]}))",
            "{[Store Type].[Deluxe Supermarket], [Store].[USA]}\n"
                + "{[Store Type].[Deluxe Supermarket], [Store].[USA].[CA]}" );
    }
}
