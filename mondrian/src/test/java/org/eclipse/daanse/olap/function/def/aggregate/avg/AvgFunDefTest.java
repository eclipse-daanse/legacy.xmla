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
package org.eclipse.daanse.olap.function.def.aggregate.avg;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

class AvgFunDefTest {

    // MONDRIAN-2408 - Consumer wants (immutable) LIST in CrossJoinFunDef.compileCall(ResolvedFunCall, ExpCompiler)
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIIfSetType_InCrossJoinAndAvg(Context context) {
        TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(),
            "Avg(CROSSJOIN([Store Type].[Deluxe Supermarket],IIf(1 = 1, {[Store].[USA].[OR], [Store].[USA].[WA]}, {[Store]"
                + ".[Mexico], [Store].[USA].[CA]})), [Measures].[Store Sales])",
            "81,031.12" );
    }

}
