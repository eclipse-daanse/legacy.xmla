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
package org.eclipse.daanse.olap.function.def.subset;

import static org.opencube.junit5.TestUtil.assertAxisReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class SubsetFunDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSubset(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Subset([Promotion Media].Children, 7, 2)",
            "[Promotion Media].[Product Attachment]\n"
                + "[Promotion Media].[Radio]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSubsetNegativeCount(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Subset([Promotion Media].Children, 3, -1)",
            "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSubsetNegativeStart(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Subset([Promotion Media].Children, -2, 4)",
            "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSubsetDefault(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Subset([Promotion Media].Children, 11)",
            "[Promotion Media].[Sunday Paper, Radio]\n"
                + "[Promotion Media].[Sunday Paper, Radio, TV]\n"
                + "[Promotion Media].[TV]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSubsetOvershoot(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Subset([Promotion Media].Children, 15)",
            "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSubsetEmpty(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Subset([Gender].[F].Children, 1)",
            "" );

        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Subset([Gender].[F].Children, 1, 3)",
            "" );
    }

}
