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
package org.eclipse.daanse.olap.function.def.topbottomcount;

import static org.opencube.junit5.TestUtil.assertAxisReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class TopBottomCountFunDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBottomCount(Context context) {
        assertAxisReturns(context.getConnection(),
            "BottomCount({[Promotion Media].[Media Type].members}, 2, [Measures].[Unit Sales])",
            "[Promotion Media].[Radio]\n"
                + "[Promotion Media].[Sunday Paper, Radio, TV]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBottomCountUnordered(Context context) {
        assertAxisReturns(context.getConnection(),
            "BottomCount({[Promotion Media].[Media Type].members}, 2)",
            "[Promotion Media].[Sunday Paper, Radio, TV]\n"
                + "[Promotion Media].[TV]" );
    }

}
