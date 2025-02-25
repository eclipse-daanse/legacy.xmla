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
package org.eclipse.daanse.olap.function.def.drilldownlevel;

import static org.opencube.junit5.TestUtil.assertAxisReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class DrilldownLevelFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDrilldownLevel(Context context) {
        // Expect all children of USA
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownLevel({[Store].[USA]}, [Store].[Store Country])",
            "[Store].[Store].[USA]\n"
                + "[Store].[Store].[USA].[CA]\n"
                + "[Store].[Store].[USA].[OR]\n"
                + "[Store].[Store].[USA].[WA]" );

        // Expect same set, because [USA] is already drilled
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownLevel({[Store].[USA], [Store].[USA].[CA]}, [Store].[Store Country])",
            "[Store].[Store].[USA]\n"
                + "[Store].[Store].[USA].[CA]" );

        // Expect drill, because [USA] isn't already drilled. You can't
        // drill down on [CA] and get to [USA]
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownLevel({[Store].[USA].[CA],[Store].[USA]}, [Store].[Store Country])",
            "[Store].[Store].[USA].[CA]\n"
                + "[Store].[Store].[USA]\n"
                + "[Store].[Store].[USA].[CA]\n"
                + "[Store].[Store].[USA].[OR]\n"
                + "[Store].[Store].[USA].[WA]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownLevel({[Store].[USA].[CA],[Store].[USA]},, 0)",
            "[Store].[Store].[USA].[CA]\n"
                + "[Store].[Store].[USA].[CA].[Alameda]\n"
                + "[Store].[Store].[USA].[CA].[Beverly Hills]\n"
                + "[Store].[Store].[USA].[CA].[Los Angeles]\n"
                + "[Store].[Store].[USA].[CA].[San Diego]\n"
                + "[Store].[Store].[USA].[CA].[San Francisco]\n"
                + "[Store].[Store].[USA]\n"
                + "[Store].[Store].[USA].[CA]\n"
                + "[Store].[Store].[USA].[OR]\n"
                + "[Store].[Store].[USA].[WA]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownLevel({[Store].[USA].[CA],[Store].[USA]} * {[Gender].Members},, 0)",
            "{[Store].[Store].[USA].[CA], [Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[CA].[Alameda], [Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[CA].[Beverly Hills], [Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[CA].[Los Angeles], [Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[CA].[San Diego], [Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[CA].[San Francisco], [Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[CA], [Gender].[F]}\n"
                + "{[Store].[Store].[USA].[CA].[Alameda], [Gender].[F]}\n"
                + "{[Store].[Store].[USA].[CA].[Beverly Hills], [Gender].[F]}\n"
                + "{[Store].[Store].[USA].[CA].[Los Angeles], [Gender].[F]}\n"
                + "{[Store].[Store].[USA].[CA].[San Diego], [Gender].[F]}\n"
                + "{[Store].[Store].[USA].[CA].[San Francisco], [Gender].[F]}\n"
                + "{[Store].[Store].[USA].[CA], [Gender].[M]}\n"
                + "{[Store].[Store].[USA].[CA].[Alameda], [Gender].[M]}\n"
                + "{[Store].[Store].[USA].[CA].[Beverly Hills], [Gender].[M]}\n"
                + "{[Store].[Store].[USA].[CA].[Los Angeles], [Gender].[M]}\n"
                + "{[Store].[Store].[USA].[CA].[San Diego], [Gender].[M]}\n"
                + "{[Store].[Store].[USA].[CA].[San Francisco], [Gender].[M]}\n"
                + "{[Store].[Store].[USA], [Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[CA], [Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[OR], [Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[WA], [Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA], [Gender].[F]}\n"
                + "{[Store].[Store].[USA].[CA], [Gender].[F]}\n"
                + "{[Store].[Store].[USA].[OR], [Gender].[F]}\n"
                + "{[Store].[Store].[USA].[WA], [Gender].[F]}\n"
                + "{[Store].[Store].[USA], [Gender].[M]}\n"
                + "{[Store].[Store].[USA].[CA], [Gender].[M]}\n"
                + "{[Store].[Store].[USA].[OR], [Gender].[M]}\n"
                + "{[Store].[Store].[USA].[WA], [Gender].[M]}" );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownLevel({[Store].[USA].[CA],[Store].[USA]} * {[Gender].Members},, 1)",
            "{[Store].[Store].[USA].[CA], [Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[CA], [Gender].[F]}\n"
                + "{[Store].[Store].[USA].[CA], [Gender].[M]}\n"
                + "{[Store].[Store].[USA].[CA], [Gender].[F]}\n"
                + "{[Store].[Store].[USA].[CA], [Gender].[M]}\n"
                + "{[Store].[Store].[USA], [Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA], [Gender].[F]}\n"
                + "{[Store].[Store].[USA], [Gender].[M]}\n"
                + "{[Store].[Store].[USA], [Gender].[F]}\n"
                + "{[Store].[Store].[USA], [Gender].[M]}" );
    }

}
