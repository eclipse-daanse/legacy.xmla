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
package org.eclipse.daanse.olap.function.def.percentile;

import static mondrian.olap.fun.FunctionTest.assertExprReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class PercentileFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testPercentile(Context<?> context) {
        // same result as median
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Percentile({[Store].[All Stores].[USA].children}, [Measures].[Store Sales], 50)",
            "159,167.84" );
        // same result as min
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Percentile({[Store].[All Stores].[USA].children}, [Measures].[Store Sales], 0)",
            "142,277.07" );
        // same result as max
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Percentile({[Store].[All Stores].[USA].children}, [Measures].[Store Sales], 100)",
            "263,793.22" );
        // check some real percentile cases
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Percentile({[Store].[All Stores].[USA].[WA].children}, [Measures].[Store Sales], 50)",
            "49,634.46" );
        // the next two results correspond to MS Excel 2013.
        // See MONDRIAN-2343 jira issue.
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Percentile({[Store].[All Stores].[USA].[WA].children}, [Measures].[Store Sales], 100/7*2)",
            "18,732.09" );
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Percentile({[Store].[All Stores].[USA].[WA].children}, [Measures].[Store Sales], 95)",
            "68,259.66" );
    }

    /**
     * Testcase for bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1045">MONDRIAN-1045,
     * "When I use the Percentile function it cracks when there's only 1 register"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testPercentileBugMondrian1045(Context<?> context) {
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Percentile({[Store].[All Stores].[USA]}, [Measures].[Store Sales], 50)",
            "565,238.13" );
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Percentile({[Store].[All Stores].[USA]}, [Measures].[Store Sales], 40)",
            "565,238.13" );
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Percentile({[Store].[All Stores].[USA]}, [Measures].[Store Sales], 95)",
            "565,238.13" );
    }

}
