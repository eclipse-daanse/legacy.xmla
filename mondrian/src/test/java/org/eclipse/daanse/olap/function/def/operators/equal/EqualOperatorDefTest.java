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
package org.eclipse.daanse.olap.function.def.operators.equal;

import static mondrian.olap.fun.FunctionTest.checkNullOp;
import static org.opencube.junit5.TestUtil.assertBooleanExprReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class EqualOperatorDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testEq(Context context) {
        assertBooleanExprReturns(context.getConnectionWithDefaultRole(), " 1.0 = 1 ", true );

        assertBooleanExprReturns(context.getConnectionWithDefaultRole(),
            "[Product].CurrentMember.Level.Ordinal = 2.0", false );
        checkNullOp(context.getConnectionWithDefaultRole(), "=" );
    }

}
