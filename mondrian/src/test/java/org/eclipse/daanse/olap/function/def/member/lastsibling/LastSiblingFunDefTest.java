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
package org.eclipse.daanse.olap.function.def.member.lastsibling;

import static mondrian.olap.Util.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opencube.junit5.TestUtil.executeSingletonAxis;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Member;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class LastSiblingFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLastSibling(Context context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].[F].LastSibling" );
        assertEquals( "M", member.getName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLastSiblingFirstInLevel(Context context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Time].[1997].[Q1].LastSibling" );
        assertEquals( "Q4", member.getName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLastSiblingAll(Context context) {
        Member member =
            executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].[All Gender].LastSibling" );
        assertTrue( member.isAll() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLastSiblingRoot(Context context) {
        // The [Time] hierarchy does not have an 'all' member, so
        // [1997], [1998] do not have parents.
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Time].[1998].LastSibling" );
        assertEquals( "1998", member.getName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLastSiblingNull(Context context) {
        Member member =
            executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].[F].FirstChild.LastSibling" );
        assertNull( member );
    }

}
