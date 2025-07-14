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
package org.eclipse.daanse.olap.function.def.set.membersx;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class MembersFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMembers(Context<?> context) {
        Connection connection = context.getConnectionWithDefaultRole();
        // <Level>.members
        assertAxisReturns(connection, "Sales",
            "{[Customers].[Country].Members}",
            "[Customers].[Customers].[Canada]\n"
                + "[Customers].[Customers].[Mexico]\n"
                + "[Customers].[Customers].[USA]" );

        // <Level>.members applied to 'all' level
        assertAxisReturns(connection, "Sales",
            "{[Customers].[(All)].Members}", "[Customers].[Customers].[All Customers]" );

        // <Level>.members applied to measures dimension
        // Note -- no cube-level calculated members are present
        assertAxisReturns(connection, "Sales",
            "{[Measures].[MeasuresLevel].Members}",
            "[Measures].[Unit Sales]\n"
                + "[Measures].[Store Cost]\n"
                + "[Measures].[Store Sales]\n"
                + "[Measures].[Sales Count]\n"
                + "[Measures].[Customer Count]\n"
                + "[Measures].[Promotion Sales]" );

        // <Dimension>.members applied to Measures
        assertAxisReturns(connection, "Sales",
            "{[Measures].Members}",
            "[Measures].[Unit Sales]\n"
                + "[Measures].[Store Cost]\n"
                + "[Measures].[Store Sales]\n"
                + "[Measures].[Sales Count]\n"
                + "[Measures].[Customer Count]\n"
                + "[Measures].[Promotion Sales]" );

        // <Dimension>.members applied to a query with calc measures
        // Again, no calc measures are returned
        switch (getDatabaseProduct(TestUtil.getDialect(connection).getDialectName())) {
            case INFOBRIGHT:
                // Skip this test on Infobright, because [Promotion Sales] is
                // defined wrong.
                break;
            default:
                assertQueryReturns(connection,
                    "with member [Measures].[Xxx] AS ' [Measures].[Unit Sales] '"
                        + "select {[Measures].members} on columns from [Sales]",
                    "Axis #0:\n"
                        + "{}\n"
                        + "Axis #1:\n"
                        + "{[Measures].[Unit Sales]}\n"
                        + "{[Measures].[Store Cost]}\n"
                        + "{[Measures].[Store Sales]}\n"
                        + "{[Measures].[Sales Count]}\n"
                        + "{[Measures].[Customer Count]}\n"
                        + "{[Measures].[Promotion Sales]}\n"
                        + "Row #0: 266,773\n"
                        + "Row #0: 225,627.23\n"
                        + "Row #0: 565,238.13\n"
                        + "Row #0: 86,837\n"
                        + "Row #0: 5,581\n"
                        + "Row #0: 151,211.21\n" );
        }

        // <Level>.members applied to a query with calc measures
        // Again, no calc measures are returned
        switch (getDatabaseProduct(TestUtil.getDialect(connection).getDialectName())) {
            case INFOBRIGHT:
                // Skip this test on Infobright, because [Promotion Sales] is
                // defined wrong.
                break;
            default:
                assertQueryReturns(connection,
                    "with member [Measures].[Xxx] AS ' [Measures].[Unit Sales] '"
                        + "select {[Measures].[Measures].members} on columns from [Sales]",
                    "Axis #0:\n"
                        + "{}\n"
                        + "Axis #1:\n"
                        + "{[Measures].[Unit Sales]}\n"
                        + "{[Measures].[Store Cost]}\n"
                        + "{[Measures].[Store Sales]}\n"
                        + "{[Measures].[Sales Count]}\n"
                        + "{[Measures].[Customer Count]}\n"
                        + "{[Measures].[Promotion Sales]}\n"
                        + "Row #0: 266,773\n"
                        + "Row #0: 225,627.23\n"
                        + "Row #0: 565,238.13\n"
                        + "Row #0: 86,837\n"
                        + "Row #0: 5,581\n"
                        + "Row #0: 151,211.21\n" );
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHierarchyMembers(Context<?> context) {
        Connection connection = context.getConnectionWithDefaultRole();
        assertAxisReturns(connection, "Sales",
            "Head({[Time].[Weekly].Members}, 10)",
            "[Time].[Weekly].[All Weeklys]\n"
                + "[Time].[Weekly].[1997]\n"
                + "[Time].[Weekly].[1997].[1]\n"
                + "[Time].[Weekly].[1997].[1].[15]\n"
                + "[Time].[Weekly].[1997].[1].[16]\n"
                + "[Time].[Weekly].[1997].[1].[17]\n"
                + "[Time].[Weekly].[1997].[1].[18]\n"
                + "[Time].[Weekly].[1997].[1].[19]\n"
                + "[Time].[Weekly].[1997].[1].[20]\n"
                + "[Time].[Weekly].[1997].[2]" );
        assertAxisReturns(connection, "Sales",
            "Tail({[Time].[Weekly].Members}, 5)",
            "[Time].[Weekly].[1998].[51].[5]\n"
                + "[Time].[Weekly].[1998].[51].[29]\n"
                + "[Time].[Weekly].[1998].[51].[30]\n"
                + "[Time].[Weekly].[1998].[52]\n"
                + "[Time].[Weekly].[1998].[52].[6]" );
    }

}
