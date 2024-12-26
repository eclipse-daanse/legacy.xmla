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
package org.eclipse.daanse.olap.function.def.caption.member;

import static org.opencube.junit5.TestUtil.assertQueryReturns;
import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

class CaptionFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMemberCaption(Context context) {
        TestUtil.assertExprReturns(context.getConnection(), "[Time].[1997].Caption", "1997" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGetCaptionUsingMemberDotCaption(Context context) {
        assertQueryReturns(context.getConnection(),
            "SELECT Filter(Store.allmembers, "
                + "[store].currentMember.caption = \"USA\") on 0 FROM SALES",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Store].[USA]}\n"
                + "Row #0: 266,773\n" );
    }

}
