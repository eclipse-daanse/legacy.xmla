/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package mondrian.test;

import static org.opencube.junit5.TestUtil.assertQueryReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.ExpressiveNamesDataLoader;
import org.opencube.junit5.propupdator.AppandExpressiveNamesCatalog;

class DemoTest {

    private static final QueryAndResult[] sampleQueries = {
        // 0
        new QueryAndResult("select {[Measures].[Measure1]} on columns\n" + " from [Cube1]",
            "Axis #0:\n" + "{}\n" + "Axis #1:\n" + "{[Measures].[Measure1]}\n" + "Row #0: 54\n")
    };

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandExpressiveNamesCatalog.class, dataloader = ExpressiveNamesDataLoader.class )
    void testSample0(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(), sampleQueries[0].query, sampleQueries[0].result );
    }

}
