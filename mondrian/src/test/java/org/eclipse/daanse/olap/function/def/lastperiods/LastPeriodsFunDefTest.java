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
package org.eclipse.daanse.olap.function.def.lastperiods;

import static org.opencube.junit5.TestUtil.assertAxisReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class LastPeriodsFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLastPeriods(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(0, [Time].[1998])", "" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(1, [Time].[1998])", "[Time].[1998]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(-1, [Time].[1998])", "[Time].[1998]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(2, [Time].[1998])",
            "[Time].[1997]\n" + "[Time].[1998]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(-2, [Time].[1997])",
            "[Time].[1997]\n" + "[Time].[1998]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(5000, [Time].[1998])",
            "[Time].[1997]\n" + "[Time].[1998]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(-5000, [Time].[1997])",
            "[Time].[1997]\n" + "[Time].[1998]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(2, [Time].[1998].[Q2])",
            "[Time].[1998].[Q1]\n" + "[Time].[1998].[Q2]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(4, [Time].[1998].[Q2])",
            "[Time].[1997].[Q3]\n"
                + "[Time].[1997].[Q4]\n"
                + "[Time].[1998].[Q1]\n"
                + "[Time].[1998].[Q2]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(-2, [Time].[1997].[Q2])",
            "[Time].[1997].[Q2]\n" + "[Time].[1997].[Q3]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(-4, [Time].[1997].[Q2])",
            "[Time].[1997].[Q2]\n"
                + "[Time].[1997].[Q3]\n"
                + "[Time].[1997].[Q4]\n"
                + "[Time].[1998].[Q1]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(5000, [Time].[1998].[Q2])",
            "[Time].[1997].[Q1]\n"
                + "[Time].[1997].[Q2]\n"
                + "[Time].[1997].[Q3]\n"
                + "[Time].[1997].[Q4]\n"
                + "[Time].[1998].[Q1]\n"
                + "[Time].[1998].[Q2]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(-5000, [Time].[1998].[Q2])",
            "[Time].[1998].[Q2]\n"
                + "[Time].[1998].[Q3]\n"
                + "[Time].[1998].[Q4]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(2, [Time].[1998].[Q2].[5])",
            "[Time].[1998].[Q2].[4]\n" + "[Time].[1998].[Q2].[5]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(12, [Time].[1998].[Q2].[5])",
            "[Time].[1997].[Q2].[6]\n"
                + "[Time].[1997].[Q3].[7]\n"
                + "[Time].[1997].[Q3].[8]\n"
                + "[Time].[1997].[Q3].[9]\n"
                + "[Time].[1997].[Q4].[10]\n"
                + "[Time].[1997].[Q4].[11]\n"
                + "[Time].[1997].[Q4].[12]\n"
                + "[Time].[1998].[Q1].[1]\n"
                + "[Time].[1998].[Q1].[2]\n"
                + "[Time].[1998].[Q1].[3]\n"
                + "[Time].[1998].[Q2].[4]\n"
                + "[Time].[1998].[Q2].[5]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(-2, [Time].[1998].[Q2].[4])",
            "[Time].[1998].[Q2].[4]\n" + "[Time].[1998].[Q2].[5]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(-12, [Time].[1997].[Q2].[6])",
            "[Time].[1997].[Q2].[6]\n"
                + "[Time].[1997].[Q3].[7]\n"
                + "[Time].[1997].[Q3].[8]\n"
                + "[Time].[1997].[Q3].[9]\n"
                + "[Time].[1997].[Q4].[10]\n"
                + "[Time].[1997].[Q4].[11]\n"
                + "[Time].[1997].[Q4].[12]\n"
                + "[Time].[1998].[Q1].[1]\n"
                + "[Time].[1998].[Q1].[2]\n"
                + "[Time].[1998].[Q1].[3]\n"
                + "[Time].[1998].[Q2].[4]\n"
                + "[Time].[1998].[Q2].[5]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(2, [Gender].[M])",
            "[Gender].[F]\n" + "[Gender].[M]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(-2, [Gender].[F])",
            "[Gender].[F]\n" + "[Gender].[M]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(2, [Gender])", "[Gender].[All Gender]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "LastPeriods(2, [Gender].Parent)", "" );
    }

}
