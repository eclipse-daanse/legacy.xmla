package org.eclipse.daanse.olap.function.def.set.stripcalculatedmembers;

import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertSetExprDependsOn;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class StripCalculatedMembersFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testStripCalculatedMembers(Context context) {
        Connection connection = context.getConnectionWithDefaultRole();
        assertAxisReturns(connection,
            "StripCalculatedMembers({[Measures].AllMembers})",
            "[Measures].[Unit Sales]\n"
                + "[Measures].[Store Cost]\n"
                + "[Measures].[Store Sales]\n"
                + "[Measures].[Sales Count]\n"
                + "[Measures].[Customer Count]\n"
                + "[Measures].[Promotion Sales]" );

        // applied to empty set
        assertAxisReturns(connection, "StripCalculatedMembers({[Gender].Parent})", "" );

        assertSetExprDependsOn(connection,
            "StripCalculatedMembers([Customers].CurrentMember.Children)",
            "{[Customers]}" );

        // ----------------------------------------------------
        // Calc members in dimension based on level stripped
        // Actual members in measures left alone
        // ----------------------------------------------------
        assertQueryReturns(connection,
            "WITH MEMBER [Store].[USA].[CA plus OR] AS "
                + "'AGGREGATE({[Store].[USA].[CA], [Store].[USA].[OR]})' "
                + "SELECT StripCalculatedMembers({[Measures].[Unit Sales], "
                + "[Measures].[Store Sales]}) ON COLUMNS,"
                + "StripCalculatedMembers("
                + "AddCalculatedMembers([Store].[USA].Children)) ON ROWS "
                + "FROM Sales "
                + "WHERE ([1997].[Q1])",
            "Axis #0:\n"
                + "{[Time].[1997].[Q1]}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "{[Measures].[Store Sales]}\n"
                + "Axis #2:\n"
                + "{[Store].[USA].[CA]}\n"
                + "{[Store].[USA].[OR]}\n"
                + "{[Store].[USA].[WA]}\n"
                + "Row #0: 16,890\n"
                + "Row #0: 36,175.20\n"
                + "Row #1: 19,287\n"
                + "Row #1: 40,170.29\n"
                + "Row #2: 30,114\n"
                + "Row #2: 63,282.86\n" );
    }

}
