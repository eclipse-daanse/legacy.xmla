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
    void testDrilldownLevel(Context<?> context) {
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
            "{[Store].[Store].[USA].[CA], [Gender].[Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[CA].[Alameda], [Gender].[Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[CA].[Beverly Hills], [Gender].[Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[CA].[Los Angeles], [Gender].[Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[CA].[San Diego], [Gender].[Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[CA].[San Francisco], [Gender].[Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[CA], [Gender].[Gender].[F]}\n"
                + "{[Store].[Store].[USA].[CA].[Alameda], [Gender].[Gender].[F]}\n"
                + "{[Store].[Store].[USA].[CA].[Beverly Hills], [Gender].[Gender].[F]}\n"
                + "{[Store].[Store].[USA].[CA].[Los Angeles], [Gender].[Gender].[F]}\n"
                + "{[Store].[Store].[USA].[CA].[San Diego], [Gender].[Gender].[F]}\n"
                + "{[Store].[Store].[USA].[CA].[San Francisco], [Gender].[Gender].[F]}\n"
                + "{[Store].[Store].[USA].[CA], [Gender].[Gender].[M]}\n"
                + "{[Store].[Store].[USA].[CA].[Alameda], [Gender].[Gender].[M]}\n"
                + "{[Store].[Store].[USA].[CA].[Beverly Hills], [Gender].[Gender].[M]}\n"
                + "{[Store].[Store].[USA].[CA].[Los Angeles], [Gender].[Gender].[M]}\n"
                + "{[Store].[Store].[USA].[CA].[San Diego], [Gender].[Gender].[M]}\n"
                + "{[Store].[Store].[USA].[CA].[San Francisco], [Gender].[Gender].[M]}\n"
                + "{[Store].[Store].[USA], [Gender].[Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[CA], [Gender].[Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[OR], [Gender].[Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[WA], [Gender].[Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA], [Gender].[Gender].[F]}\n"
                + "{[Store].[Store].[USA].[CA], [Gender].[Gender].[F]}\n"
                + "{[Store].[Store].[USA].[OR], [Gender].[Gender].[F]}\n"
                + "{[Store].[Store].[USA].[WA], [Gender].[Gender].[F]}\n"
                + "{[Store].[Store].[USA], [Gender].[Gender].[M]}\n"
                + "{[Store].[Store].[USA].[CA], [Gender].[Gender].[M]}\n"
                + "{[Store].[Store].[USA].[OR], [Gender].[Gender].[M]}\n"
                + "{[Store].[Store].[USA].[WA], [Gender].[Gender].[M]}" );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownLevel({[Store].[USA].[CA],[Store].[USA]} * {[Gender].Members},, 1)",
            "{[Store].[Store].[USA].[CA], [Gender].[Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA].[CA], [Gender].[Gender].[F]}\n"
                + "{[Store].[Store].[USA].[CA], [Gender].[Gender].[M]}\n"
                + "{[Store].[Store].[USA].[CA], [Gender].[Gender].[F]}\n"
                + "{[Store].[Store].[USA].[CA], [Gender].[Gender].[M]}\n"
                + "{[Store].[Store].[USA], [Gender].[Gender].[All Gender]}\n"
                + "{[Store].[Store].[USA], [Gender].[Gender].[F]}\n"
                + "{[Store].[Store].[USA], [Gender].[Gender].[M]}\n"
                + "{[Store].[Store].[USA], [Gender].[Gender].[F]}\n"
                + "{[Store].[Store].[USA], [Gender].[Gender].[M]}" );
    }

}
