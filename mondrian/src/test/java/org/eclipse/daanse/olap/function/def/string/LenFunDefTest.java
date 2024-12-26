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
package org.eclipse.daanse.olap.function.def.string;

import static mondrian.olap.fun.FunctionTest.assertExprReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class LenFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLenFunctionWithNullString(Context context) {
        // SSAS2005 returns 0
        assertQueryReturns(context.getConnection(),
            "with member [Measures].[Foo] as ' NULL '\n"
                + " member [Measures].[Bar] as ' len([Measures].[Foo]) '\n"
                + "select [Measures].[Bar] on 0\n"
                + "from [Warehouse and Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Bar]}\n"
                + "Row #0: 0\n" );
        // same, but inline
        assertExprReturns(context.getConnection(), "len(null)", 0, 0 );
    }

}
