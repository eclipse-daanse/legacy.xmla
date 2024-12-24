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
        assertAxisReturns(context.getConnection(),
            "DrilldownLevelTop({[Store].[USA]}, 2, [Store].[Store Country])",
            "[Store].[USA]\n"
                + "[Store].[USA].[WA]\n"
                + "[Store].[USA].[CA]" );

        // similarly DrilldownLevelBottom
        assertAxisReturns(context.getConnection(),
            "DrilldownLevelBottom({[Store].[USA]}, 2, [Store].[Store Country])",
            "[Store].[USA]\n"
                + "[Store].[USA].[OR]\n"
                + "[Store].[USA].[CA]" );

        // <set>, <n>
        assertAxisReturns(context.getConnection(),
            "DrilldownLevelTop({[Store].[USA]}, 2)",
            "[Store].[USA]\n"
                + "[Store].[USA].[WA]\n"
                + "[Store].[USA].[CA]" );

        // <n> greater than number of children
        assertAxisReturns(context.getConnection(),
            "DrilldownLevelTop({[Store].[USA], [Store].[Canada]}, 4)",
            "[Store].[USA]\n"
                + "[Store].[USA].[WA]\n"
                + "[Store].[USA].[CA]\n"
                + "[Store].[USA].[OR]\n"
                + "[Store].[Canada]\n"
                + "[Store].[Canada].[BC]" );

        // <n> negative
        assertAxisReturns(context.getConnection(),
            "DrilldownLevelTop({[Store].[USA]}, 2 - 3)",
            "[Store].[USA]" );

        // <n> zero
        assertAxisReturns(context.getConnection(),
            "DrilldownLevelTop({[Store].[USA]}, 2 - 2)",
            "[Store].[USA]" );

        // <n> null
        assertAxisReturns(context.getConnection(),
            "DrilldownLevelTop({[Store].[USA]}, null)",
            "[Store].[USA]" );

        // mixed bag, no level, all expanded
        assertAxisReturns(context.getConnection(),
            "DrilldownLevelTop({[Store].[USA], "
                + "[Store].[USA].[CA].[San Francisco], "
                + "[Store].[All Stores], "
                + "[Store].[Canada].[BC]}, "
                + "2)",
            "[Store].[USA]\n"
                + "[Store].[USA].[WA]\n"
                + "[Store].[USA].[CA]\n"
                + "[Store].[USA].[CA].[San Francisco]\n"
                + "[Store].[USA].[CA].[San Francisco].[Store 14]\n"
                + "[Store].[All Stores]\n"
                + "[Store].[USA]\n"
                + "[Store].[Canada]\n"
                + "[Store].[Canada].[BC]\n"
                + "[Store].[Canada].[BC].[Vancouver]\n"
                + "[Store].[Canada].[BC].[Victoria]" );

        // mixed bag, only specified level expanded
        assertAxisReturns(context.getConnection(),
            "DrilldownLevelTop({[Store].[USA], "
                + "[Store].[USA].[CA].[San Francisco], "
                + "[Store].[All Stores], "
                + "[Store].[Canada].[BC]}, 2, [Store].[Store City])",
            "[Store].[USA]\n"
                + "[Store].[USA].[CA].[San Francisco]\n"
                + "[Store].[USA].[CA].[San Francisco].[Store 14]\n"
                + "[Store].[All Stores]\n"
                + "[Store].[Canada].[BC]" );

        // bad level
        assertAxisThrows(context.getConnection(),
            "DrilldownLevelTop({[Store].[USA]}, 2, [Customers].[Country])",
            "Level '[Customers].[Country]' not compatible with "
                + "member '[Store].[USA]'" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDrilldownMemberEmptyExpr(Context context) {
        // no level, with expression
        assertAxisReturns(context.getConnection(),
            "DrilldownLevelTop({[Store].[USA]}, 2, , [Measures].[Unit Sales])",
            "[Store].[USA]\n"
                + "[Store].[USA].[WA]\n"
                + "[Store].[USA].[CA]" );

        // reverse expression
        assertAxisReturns(context.getConnection(),
            "DrilldownLevelTop("
                + "{[Store].[USA]}, 2, , - [Measures].[Unit Sales])",
            "[Store].[USA]\n"
                + "[Store].[USA].[OR]\n"
                + "[Store].[USA].[CA]" );
    }

}
