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
package org.eclipse.daanse.olap.function.def.set.setitem;

import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertAxisThrows;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class SetItemFunDefTest {

    /**
     * Tests the function <code>&lt;Set&gt;.Item(&lt;Integer&gt;)</code>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSetItemInt(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "{[Customers].[All Customers].[USA].[OR].[Lebanon].[Mary Frances Christian]}.Item(0)",
            "[Customers].[Customers].[USA].[OR].[Lebanon].[Mary Frances Christian]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "{[Customers].[All Customers].[USA],"
                + "[Customers].[All Customers].[USA].[WA],"
                + "[Customers].[All Customers].[USA].[CA],"
                + "[Customers].[All Customers].[USA].[OR].[Lebanon].[Mary Frances Christian]}.Item(2)",
            "[Customers].[Customers].[USA].[CA]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "{[Customers].[All Customers].[USA],"
                + "[Customers].[All Customers].[USA].[WA],"
                + "[Customers].[All Customers].[USA].[CA],"
                + "[Customers].[All Customers].[USA].[OR].[Lebanon].[Mary Frances Christian]}.Item(100 / 50 - 1)",
            "[Customers].[Customers].[USA].[WA]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "{([Time].[1997].[Q1].[1], [Customers].[All Customers].[USA]),"
                + "([Time].[1997].[Q1].[2], [Customers].[All Customers].[USA].[WA]),"
                + "([Time].[1997].[Q1].[3], [Customers].[All Customers].[USA].[CA]),"
                + "([Time].[1997].[Q2].[4], [Customers].[All Customers].[USA].[OR].[Lebanon].[Mary Frances Christian])}"
                + ".Item(100 / 50 - 1)",
            "{[Time].[Time].[1997].[Q1].[2], [Customers].[Customers].[USA].[WA]}" );

        // given index out of bounds, item returns null
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "{[Customers].[All Customers].[USA],"
                + "[Customers].[All Customers].[USA].[WA],"
                + "[Customers].[All Customers].[USA].[CA],"
                + "[Customers].[All Customers].[USA].[OR].[Lebanon].[Mary Frances Christian]}.Item(-1)",
            "" );

        // given index out of bounds, item returns null
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "{[Customers].[All Customers].[USA],"
                + "[Customers].[All Customers].[USA].[WA],"
                + "[Customers].[All Customers].[USA].[CA],"
                + "[Customers].[All Customers].[USA].[OR].[Lebanon].[Mary Frances Christian]}.Item(4)",
            "" );
    }

    /**
     * Tests the function <code>&lt;Set&gt;.Item(&lt;String&gt; [,...])</code>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSetItemString(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "{[Gender].[M], [Gender].[F]}.Item(\"M\")",
            "[Gender].[Gender].[M]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "{CrossJoin([Gender].Members, [Marital Status].Members)}.Item(\"M\", \"S\")",
            "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[S]}" );

        // MSAS fails with "duplicate dimensions across (independent) axes".
        // (That's a bug in MSAS.)
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "{CrossJoin([Gender].Members, [Marital Status].Members)}.Item(\"M\", \"M\")",
            "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[M]}" );

        // None found.
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "{[Gender].[M], [Gender].[F]}.Item(\"X\")", "" );
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "{CrossJoin([Gender].[Gender].Members, [Marital Status].[Marital Status].Members)}.Item(\"M\", \"F\")",
            "" );
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "CrossJoin([Gender].[Gender].Members, [Marital Status].[Marital Status].Members).Item(\"S\", \"M\")",
            "" );

        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "CrossJoin([Gender].Members, [Marital Status].Members).Item(\"M\")",
            "Argument count does not match set's cardinality 2", "Sales" );
    }

}
