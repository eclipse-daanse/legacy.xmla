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
package org.eclipse.daanse.olap.function.def.operators.or;

import static org.opencube.junit5.TestUtil.assertBooleanExprReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class OrOperatorDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOr2(Context context) {
        assertBooleanExprReturns(context.getConnection(), " 1=0 OR 0=0 ", true );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOrAssociativity1(Context context) {
        // Would give 'false' if OR were stronger than AND (wrong!)
        assertBooleanExprReturns(context.getConnection(), " 1=1 AND 1=0 OR 1=1 ", true );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOrAssociativity2(Context context) {
        // Would give 'false' if OR were stronger than AND (wrong!)
        assertBooleanExprReturns(context.getConnection(), " 1=1 OR 1=0 AND 1=1 ", true );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOrAssociativity3(Context context) {
        assertBooleanExprReturns(context.getConnection(), " (1=0 OR 1=1) AND 1=1 ", true );
    }


}
