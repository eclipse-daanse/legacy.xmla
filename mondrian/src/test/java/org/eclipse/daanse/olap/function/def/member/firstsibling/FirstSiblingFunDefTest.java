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
package org.eclipse.daanse.olap.function.def.member.firstsibling;

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


class FirstSiblingFunDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFirstSiblingFirstInLevel(Context context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].[F].FirstSibling" );
        assertEquals( "F", member.getName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFirstSiblingLastInLevel(Context context) {
        Member member =
            executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Time].[1997].[Q4].FirstSibling" );
        assertEquals( "Q1", member.getName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFirstSiblingAll(Context context) {
        Member member =
            executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].[All Gender].FirstSibling" );
        assertTrue( member.isAll() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFirstSiblingRoot(Context context) {
        // The [Measures] hierarchy does not have an 'all' member, so
        // [Unit Sales] does not have a parent.
        Member member =
            executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Measures].[Store Sales].FirstSibling" );
        assertEquals( "Unit Sales", member.getName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFirstSiblingNull(Context context) {
        Member member =
            executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].[F].FirstChild.FirstSibling" );
        assertNull( member );
    }


}
