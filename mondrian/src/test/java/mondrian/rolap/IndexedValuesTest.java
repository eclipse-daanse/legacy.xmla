/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.rolap;

import static org.opencube.junit5.TestUtil.assertQueryReturns;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

/**
 * Test case for '&amp;[..]' capability in MDX identifiers.
 *
 * <p>This feature used
 * <a href="http://jira.pentaho.com/browse/MONDRIAN-485">bug MONDRIAN-485,
 * "Member key treated as member name in WHERE"</a>
 * as a placeholder.
 *
 * @author pierluiggi@users.sourceforge.net
 */
class IndexedValuesTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    @DisabledIfSystemProperty(named = "test.disable.knownFails", matches = "true")
    void testQueryWithIndex(Context context) {
        final String desiredResult =
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Org Salary]}\n"
            + "{[Measures].[Count]}\n"
            + "Axis #2:\n"
            + "{[Employees].[Employees].[Sheri Nowmer]}\n"
            + "Row #0: $39,431.67\n"
            + "Row #0: 7,392\n";
        Connection connection = context.getConnectionWithDefaultRole();
        // Query using name
        assertQueryReturns(connection,
            "SELECT {[Measures].[Org Salary], [Measures].[Count]} "
            + "ON COLUMNS, "
            + "{[Employees].[Employees].[Sheri Nowmer]} "
            + "ON ROWS FROM [HR]",
            desiredResult);

        // Member keys only work with SsasCompatibleNaming=true
        //if (!SystemWideProperties.instance().SsasCompatibleNaming) {
        //    return;
        //}

        // Query using key; expect same result.
        assertQueryReturns(connection,
            "SELECT {[Measures].[Org Salary], [Measures].[Count]} "
            + "ON COLUMNS, "
            + "{[Employees].[Employees].[Employees].&[1]} "
            + "ON ROWS FROM [HR]",
            desiredResult);

        // Cannot find members that are not at root of hierarchy.
        // (We should fix this.)
        assertQueryReturns(connection,
            "SELECT {[Measures].[Org Salary], [Measures].[Count]} "
            + "ON COLUMNS, "
            + "{[Employees].[Employees].&[4]} "
            + "ON ROWS FROM [HR]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Org Salary]}\n"
            + "{[Measures].[Count]}\n"
            + "Axis #2:\n"
            + "{[Employees].[Employees].[Sheri Nowmer].[Michael Spence]}\n"
            + "Row #0: \n"
            + "Row #0: \n");

        // "level.&key" syntax
        assertQueryReturns(connection,
            "SELECT [Measures] ON COLUMNS, "
            + "{[Product].[Product].[Product Name].&[9]} "
            + "ON ROWS FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Product].[Product].[Drink].[Beverages].[Pure Juice Beverages].[Juice].[Washington].[Washington Cranberry Juice]}\n"
            + "Row #0: 130\n");
    }
}
