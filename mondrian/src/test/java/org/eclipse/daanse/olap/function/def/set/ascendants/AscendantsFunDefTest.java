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
package org.eclipse.daanse.olap.function.def.set.ascendants;

import static org.opencube.junit5.TestUtil.assertAxisReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class AscendantsFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAscendants(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Ascendants([Store].[USA].[CA])",
            "[Store].[Store].[USA].[CA]\n"
                + "[Store].[Store].[USA]\n"
                + "[Store].[Store].[All Stores]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAscendantsAll(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Ascendants([Store].DefaultMember)", "[Store].[Store].[All Stores]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAscendantsNull(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Ascendants([Gender].[F].PrevMember)", "" );
    }

}
