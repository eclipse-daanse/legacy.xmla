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
package org.eclipse.daanse.olap.function.def.settostr;

import static org.opencube.junit5.TestUtil.assertExprReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class SetToStrFunDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSetToStr(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "SetToStr([Time].[Time].children)",
            "{[Time].[Time].[1997].[Q1], [Time].[Time].[1997].[Q2], [Time].[Time].[1997].[Q3], [Time].[Time].[1997].[Q4]}" );

        // Now, applied to tuples
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "SetToStr({CrossJoin([Marital Status].children, {[Gender].[M]})})",
            "{"
                + "([Marital Status].[Marital Status].[M], [Gender].[Gender].[M]), "
                + "([Marital Status].[Marital Status].[S], [Gender].[Gender].[M])"
                + "}" );
    }

}
