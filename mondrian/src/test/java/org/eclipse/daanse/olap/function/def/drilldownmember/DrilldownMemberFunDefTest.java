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
package org.eclipse.daanse.olap.function.def.drilldownmember;

import static org.opencube.junit5.TestUtil.assertAxisReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class DrilldownMemberFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDrilldownMember(Context<?> context) {
        // Expect all children of USA
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownMember({[Store].[USA]}, {[Store].[USA]})",
            "[Store].[Store].[USA]\n"
                + "[Store].[Store].[USA].[CA]\n"
                + "[Store].[Store].[USA].[OR]\n"
                + "[Store].[Store].[USA].[WA]" );

        // Expect all children of USA.CA and USA.OR
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownMember({[Store].[USA].[CA], [Store].[USA].[OR]}, "
                + "{[Store].[USA].[CA], [Store].[USA].[OR], [Store].[USA].[WA]})",
            "[Store].[Store].[USA].[CA]\n"
                + "[Store].[Store].[USA].[CA].[Alameda]\n"
                + "[Store].[Store].[USA].[CA].[Beverly Hills]\n"
                + "[Store].[Store].[USA].[CA].[Los Angeles]\n"
                + "[Store].[Store].[USA].[CA].[San Diego]\n"
                + "[Store].[Store].[USA].[CA].[San Francisco]\n"
                + "[Store].[Store].[USA].[OR]\n"
                + "[Store].[Store].[USA].[OR].[Portland]\n"
                + "[Store].[Store].[USA].[OR].[Salem]" );


        // Second set is empty
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownMember({[Store].[USA]}, {})",
            "[Store].[Store].[USA]" );

        // Drill down a leaf member
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownMember({[Store].[All Stores].[USA].[CA].[San Francisco].[Store 14]}, "
                + "{[Store].[USA].[CA].[San Francisco].[Store 14]})",
            "[Store].[Store].[USA].[CA].[San Francisco].[Store 14]" );

        // Complex case with option recursive
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownMember({[Store].[All Stores].[USA]}, "
                + "{[Store].[All Stores].[USA], [Store].[All Stores].[USA].[CA], "
                + "[Store].[All Stores].[USA].[CA].[San Diego], [Store].[All Stores].[USA].[WA]}, "
                + "RECURSIVE)",
            "[Store].[Store].[USA]\n"
                + "[Store].[Store].[USA].[CA]\n"
                + "[Store].[Store].[USA].[CA].[Alameda]\n"
                + "[Store].[Store].[USA].[CA].[Beverly Hills]\n"
                + "[Store].[Store].[USA].[CA].[Los Angeles]\n"
                + "[Store].[Store].[USA].[CA].[San Diego]\n"
                + "[Store].[Store].[USA].[CA].[San Diego].[Store 24]\n"
                + "[Store].[Store].[USA].[CA].[San Francisco]\n"
                + "[Store].[Store].[USA].[OR]\n"
                + "[Store].[Store].[USA].[WA]\n"
                + "[Store].[Store].[USA].[WA].[Bellingham]\n"
                + "[Store].[Store].[USA].[WA].[Bremerton]\n"
                + "[Store].[Store].[USA].[WA].[Seattle]\n"
                + "[Store].[Store].[USA].[WA].[Spokane]\n"
                + "[Store].[Store].[USA].[WA].[Tacoma]\n"
                + "[Store].[Store].[USA].[WA].[Walla Walla]\n"
                + "[Store].[Store].[USA].[WA].[Yakima]" );

        // Sets of tuples
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownMember({([Store Type].[Supermarket], [Store].[USA])}, {[Store].[USA]})",
            "{[Store Type].[Store Type].[Supermarket], [Store].[Store].[USA]}\n"
                + "{[Store Type].[Store Type].[Supermarket], [Store].[Store].[USA].[CA]}\n"
                + "{[Store Type].[Store Type].[Supermarket], [Store].[Store].[USA].[OR]}\n"
                + "{[Store Type].[Store Type].[Supermarket], [Store].[Store].[USA].[WA]}" );
    }

}
