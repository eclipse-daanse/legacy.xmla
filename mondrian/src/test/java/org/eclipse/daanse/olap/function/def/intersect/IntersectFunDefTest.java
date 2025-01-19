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
package org.eclipse.daanse.olap.function.def.intersect;

import static org.opencube.junit5.TestUtil.assertAxisReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class IntersectFunDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIntersectAll(Context context) {
        // Note: duplicates retained from left, not from right; and order is
        // preserved.
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Intersect({[Time].[1997].[Q2], [Time].[1997], [Time].[1997].[Q1], [Time].[1997].[Q2]}, "
                + "{[Time].[1998], [Time].[1997], [Time].[1997].[Q2], [Time].[1997]}, "
                + "ALL)",
            "[Time].[1997].[Q2]\n"
                + "[Time].[1997]\n"
                + "[Time].[1997].[Q2]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIntersect(Context context) {
        // Duplicates not preserved. Output in order that first duplicate
        // occurred.
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Intersect(\n"
                + "  {[Time].[1997].[Q2], [Time].[1997], [Time].[1997].[Q1], [Time].[1997].[Q2]}, "
                + "{[Time].[1998], [Time].[1997], [Time].[1997].[Q2], [Time].[1997]})",
            "[Time].[1997].[Q2]\n"
                + "[Time].[1997]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIntersectTuples(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Intersect(\n"
                + "  {([Time].[1997].[Q2], [Gender].[M]),\n"
                + "   ([Time].[1997], [Gender].[F]),\n"
                + "   ([Time].[1997].[Q1], [Gender].[M]),\n"
                + "   ([Time].[1997].[Q2], [Gender].[M])},\n"
                + "  {([Time].[1998], [Gender].[F]),\n"
                + "   ([Time].[1997], [Gender].[F]),\n"
                + "   ([Time].[1997].[Q2], [Gender].[M]),\n"
                + "   ([Time].[1997], [Gender])})",
            "{[Time].[1997].[Q2], [Gender].[M]}\n"
                + "{[Time].[1997], [Gender].[F]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIntersectRightEmpty(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Intersect({[Time].[1997]}, {})",
            "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIntersectLeftEmpty(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Intersect({}, {[Store].[USA].[CA]})",
            "" );
    }

}
