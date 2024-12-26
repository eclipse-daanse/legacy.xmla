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

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;
import static org.opencube.junit5.TestUtil.assertQueryReturns;


class IsNullFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIsNull(Context context) {
        assertBooleanExprReturns(context.getConnection(), " Measures.[Profit] IS NULL ", false );
        assertBooleanExprReturns(context.getConnection(), " Store.[All Stores] IS NULL ", false );
        assertBooleanExprReturns(context.getConnection(), " Store.[All Stores].parent IS NULL ", true );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIsNullWithCalcMem(Context context) {
        assertQueryReturns(context.getConnection(),
            "with member Store.foo as '1010' "
                + "member measures.bar as 'Store.currentmember IS NULL' "
                + "SELECT measures.bar on 0, {Store.foo} on 1 from sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[bar]}\n"
                + "Axis #2:\n"
                + "{[Store].[foo]}\n"
                + "Row #0: false\n" );
    }

}
