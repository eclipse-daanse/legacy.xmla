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
package org.eclipse.daanse.olap.function.def.unorder;

import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertAxisThrows;
import static org.opencube.junit5.TestUtil.assertQueryReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class UnorderFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUnorder(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Unorder([Gender].members)",
            "[Gender].[All Gender]\n"
                + "[Gender].[F]\n"
                + "[Gender].[M]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Unorder(Order([Gender].members, -[Measures].[Unit Sales]))",
            "[Gender].[All Gender]\n"
                + "[Gender].[M]\n"
                + "[Gender].[F]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Unorder(Crossjoin([Gender].members, [Marital Status].Children))",
            "{[Gender].[All Gender], [Marital Status].[M]}\n"
                + "{[Gender].[All Gender], [Marital Status].[S]}\n"
                + "{[Gender].[F], [Marital Status].[M]}\n"
                + "{[Gender].[F], [Marital Status].[S]}\n"
                + "{[Gender].[M], [Marital Status].[M]}\n"
                + "{[Gender].[M], [Marital Status].[S]}" );

        // implicitly convert member to set
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Unorder([Gender].[M])",
            "[Gender].[M]" );

        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "Unorder(1 + 3)",
            "No function matches signature 'Unorder(<Numeric Expression>)'" );
        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "Unorder([Gender].[M], 1 + 3)",
            "No function matches signature 'Unorder(<Member>, <Numeric Expression>)'" );
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures].[Store Sales], [Measures].[Unit Sales]} on 0,\n"
                + "  Unorder([Gender].Members) on 1\n"
                + "from [Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Store Sales]}\n"
                + "{[Measures].[Unit Sales]}\n"
                + "Axis #2:\n"
                + "{[Gender].[All Gender]}\n"
                + "{[Gender].[F]}\n"
                + "{[Gender].[M]}\n"
                + "Row #0: 565,238.13\n"
                + "Row #0: 266,773\n"
                + "Row #1: 280,226.21\n"
                + "Row #1: 131,558\n"
                + "Row #2: 285,011.92\n"
                + "Row #2: 135,215\n" );
    }

}
