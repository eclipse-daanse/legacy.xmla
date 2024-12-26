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
package org.eclipse.daanse.olap.function.def.logical;

import static org.opencube.junit5.TestUtil.assertBooleanExprReturns;
import static org.opencube.junit5.TestUtil.assertExprThrows;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class IsFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIsMember(Context context) {
        assertBooleanExprReturns(context.getConnection(),
            " Store.[USA].parent IS Store.[All Stores]", true );
        assertBooleanExprReturns(context.getConnection(),
            " [Store].[USA].[CA].parent IS [Store].[Mexico]", false );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIsString(Context context) {
        assertExprThrows(context.getConnection(),
            " [Store].[USA].Name IS \"USA\" ",
            "No function matches signature '<String> IS <String>'" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIsNumeric(Context context) {
        assertExprThrows(context.getConnection(),
            " [Store].[USA].Level.Ordinal IS 25 ",
            "No function matches signature '<Numeric Expression> IS <Numeric Expression>'" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIsTuple(Context context) {
        assertBooleanExprReturns(context.getConnection(),
            " (Store.[USA], Gender.[M]) IS (Store.[USA], Gender.[M])", true );
        assertBooleanExprReturns(context.getConnection(),
            " (Store.[USA], Gender.[M]) IS (Gender.[M], Store.[USA])", true );
        assertBooleanExprReturns(context.getConnection(),
            " (Store.[USA], Gender.[M]) IS (Gender.[M], Store.[USA]) "
                + "OR [Gender] IS NULL",
            true );
        assertBooleanExprReturns(context.getConnection(),
            " (Store.[USA], Gender.[M]) IS (Gender.[M], Store.[USA]) "
                + "AND [Gender] IS NULL",
            false );
        assertBooleanExprReturns(context.getConnection(),
            " (Store.[USA], Gender.[M]) IS (Store.[USA], Gender.[F])",
            false );
        assertBooleanExprReturns(context.getConnection(),
            " (Store.[USA], Gender.[M]) IS (Store.[USA])",
            false );
        assertBooleanExprReturns(context.getConnection(),
            " (Store.[USA], Gender.[M]) IS Store.[USA]",
            false );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIsLevel(Context context) {
        assertBooleanExprReturns(context.getConnection(),
            " Store.[USA].level IS Store.[Store Country] ", true );
        assertBooleanExprReturns(context.getConnection(),
            " Store.[USA].[CA].level IS Store.[Store Country] ", false );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIsHierarchy(Context context) {
        assertBooleanExprReturns(context.getConnection(),
            " Store.[USA].hierarchy IS Store.[Mexico].hierarchy ", true );
        assertBooleanExprReturns(context.getConnection(),
            " Store.[USA].hierarchy IS Gender.[M].hierarchy ", false );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIsDimension(Context context) {
        assertBooleanExprReturns(context.getConnection(), " Store.[USA].dimension IS Store ", true );
        assertBooleanExprReturns(context.getConnection()," Gender.[M].dimension IS Store ", false );
    }

}
