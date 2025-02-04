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
    void testDrilldownMember(Context context) {
        // Expect all children of USA
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownMember({[Store].[USA]}, {[Store].[USA]})",
            "[Store].[USA]\n"
                + "[Store].[USA].[CA]\n"
                + "[Store].[USA].[OR]\n"
                + "[Store].[USA].[WA]" );

        // Expect all children of USA.CA and USA.OR
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownMember({[Store].[USA].[CA], [Store].[USA].[OR]}, "
                + "{[Store].[USA].[CA], [Store].[USA].[OR], [Store].[USA].[WA]})",
            "[Store].[USA].[CA]\n"
                + "[Store].[USA].[CA].[Alameda]\n"
                + "[Store].[USA].[CA].[Beverly Hills]\n"
                + "[Store].[USA].[CA].[Los Angeles]\n"
                + "[Store].[USA].[CA].[San Diego]\n"
                + "[Store].[USA].[CA].[San Francisco]\n"
                + "[Store].[USA].[OR]\n"
                + "[Store].[USA].[OR].[Portland]\n"
                + "[Store].[USA].[OR].[Salem]" );


        // Second set is empty
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownMember({[Store].[USA]}, {})",
            "[Store].[USA]" );

        // Drill down a leaf member
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownMember({[Store].[All Stores].[USA].[CA].[San Francisco].[Store 14]}, "
                + "{[Store].[USA].[CA].[San Francisco].[Store 14]})",
            "[Store].[USA].[CA].[San Francisco].[Store 14]" );

        // Complex case with option recursive
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownMember({[Store].[All Stores].[USA]}, "
                + "{[Store].[All Stores].[USA], [Store].[All Stores].[USA].[CA], "
                + "[Store].[All Stores].[USA].[CA].[San Diego], [Store].[All Stores].[USA].[WA]}, "
                + "RECURSIVE)",
            "[Store].[USA]\n"
                + "[Store].[USA].[CA]\n"
                + "[Store].[USA].[CA].[Alameda]\n"
                + "[Store].[USA].[CA].[Beverly Hills]\n"
                + "[Store].[USA].[CA].[Los Angeles]\n"
                + "[Store].[USA].[CA].[San Diego]\n"
                + "[Store].[USA].[CA].[San Diego].[Store 24]\n"
                + "[Store].[USA].[CA].[San Francisco]\n"
                + "[Store].[USA].[OR]\n"
                + "[Store].[USA].[WA]\n"
                + "[Store].[USA].[WA].[Bellingham]\n"
                + "[Store].[USA].[WA].[Bremerton]\n"
                + "[Store].[USA].[WA].[Seattle]\n"
                + "[Store].[USA].[WA].[Spokane]\n"
                + "[Store].[USA].[WA].[Tacoma]\n"
                + "[Store].[USA].[WA].[Walla Walla]\n"
                + "[Store].[USA].[WA].[Yakima]" );

        // Sets of tuples
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "DrilldownMember({([Store Type].[Supermarket], [Store].[USA])}, {[Store].[USA]})",
            "{[Store Type].[Supermarket], [Store].[USA]}\n"
                + "{[Store Type].[Supermarket], [Store].[USA].[CA]}\n"
                + "{[Store Type].[Supermarket], [Store].[USA].[OR]}\n"
                + "{[Store Type].[Supermarket], [Store].[USA].[WA]}" );
    }

}
