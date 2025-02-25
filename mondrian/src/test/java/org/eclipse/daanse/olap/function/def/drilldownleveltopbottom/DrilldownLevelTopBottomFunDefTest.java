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
package org.eclipse.daanse.olap.function.def.drilldownleveltopbottom;

import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertAxisThrows;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class DrilldownLevelTopBottomFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDrilldownLevelTop(Context context) {
        // <set>, <n>, <level>
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownLevelTop({[Store].[USA]}, 2, [Store].[Store Country])",
            "[Store].[Store].[USA]\n"
                + "[Store].[Store].[USA].[WA]\n"
                + "[Store].[Store].[USA].[CA]" );

        // similarly DrilldownLevelBottom
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownLevelBottom({[Store].[USA]}, 2, [Store].[Store Country])",
            "[Store].[Store].[USA]\n"
                + "[Store].[Store].[USA].[OR]\n"
                + "[Store].[Store].[USA].[CA]" );

        // <set>, <n>
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownLevelTop({[Store].[USA]}, 2)",
            "[Store].[Store].[USA]\n"
                + "[Store].[Store].[USA].[WA]\n"
                + "[Store].[Store].[USA].[CA]" );

        // <n> greater than number of children
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownLevelTop({[Store].[USA], [Store].[Canada]}, 4)",
            "[Store].[Store].[USA]\n"
                + "[Store].[Store].[USA].[WA]\n"
                + "[Store].[Store].[USA].[CA]\n"
                + "[Store].[Store].[USA].[OR]\n"
                + "[Store].[Store].[Canada]\n"
                + "[Store].[Store].[Canada].[BC]" );

        // <n> negative
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownLevelTop({[Store].[USA]}, 2 - 3)",
            "[Store].[Store].[USA]" );

        // <n> zero
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownLevelTop({[Store].[USA]}, 2 - 2)",
            "[Store].[Store].[USA]" );

        // <n> null
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownLevelTop({[Store].[USA]}, null)",
            "[Store].[Store].[USA]" );

        // mixed bag, no level, all expanded
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownLevelTop({[Store].[USA], "
                + "[Store].[USA].[CA].[San Francisco], "
                + "[Store].[All Stores], "
                + "[Store].[Canada].[BC]}, "
                + "2)",
            "[Store].[Store].[USA]\n"
                + "[Store].[Store].[USA].[WA]\n"
                + "[Store].[Store].[USA].[CA]\n"
                + "[Store].[Store].[USA].[CA].[San Francisco]\n"
                + "[Store].[Store].[USA].[CA].[San Francisco].[Store 14]\n"
                + "[Store].[Store].[All Stores]\n"
                + "[Store].[Store].[USA]\n"
                + "[Store].[Store].[Canada]\n"
                + "[Store].[Store].[Canada].[BC]\n"
                + "[Store].[Store].[Canada].[BC].[Vancouver]\n"
                + "[Store].[Store].[Canada].[BC].[Victoria]" );

        // mixed bag, only specified level expanded
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownLevelTop({[Store].[USA], "
                + "[Store].[USA].[CA].[San Francisco], "
                + "[Store].[All Stores], "
                + "[Store].[Canada].[BC]}, 2, [Store].[Store City])",
            "[Store].[Store].[USA]\n"
                + "[Store].[Store].[USA].[CA].[San Francisco]\n"
                + "[Store].[Store].[USA].[CA].[San Francisco].[Store 14]\n"
                + "[Store].[Store].[All Stores]\n"
                + "[Store].[Store].[Canada].[BC]" );

        // bad level
        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "DrilldownLevelTop({[Store].[USA]}, 2, [Customers].[Country])",
            "Level '[Customers].[Customers].[Country]' not compatible with "
                + "member '[Store].[Store].[USA]'", "Sales" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDrilldownMemberEmptyExpr(Context context) {
        // no level, with expression
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownLevelTop({[Store].[USA]}, 2, , [Measures].[Unit Sales])",
            "[Store].[Store].[USA]\n"
                + "[Store].[Store].[USA].[WA]\n"
                + "[Store].[Store].[USA].[CA]" );

        // reverse expression
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownLevelTop("
                + "{[Store].[USA]}, 2, , - [Measures].[Unit Sales])",
            "[Store].[Store].[USA]\n"
                + "[Store].[Store].[USA].[OR]\n"
                + "[Store].[Store].[USA].[CA]" );
    }

}
