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
package org.eclipse.daanse.olap.function.def.leadlag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opencube.junit5.TestUtil.executeSingletonAxis;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Member;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class LeadLagFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLag(Context context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Time].[1997].[Q4].[12].Lag(4)" );
        assertEquals( "8", member.getName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLagFirstInLevel(Context context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].[F].Lag(1)" );
        assertNull( member );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLagAll(Context context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].DefaultMember.Lag(2)" );
        assertNull( member );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLagRoot(Context context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Time].[1998].Lag(1)" );
        assertEquals( "1997", member.getName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLagRootTooFar(Context context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Time].[1998].Lag(2)" );
        assertNull( member );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLead(Context context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Time].[1997].[Q2].[4].Lead(4)" );
        assertEquals( "8", member.getName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLeadNegative(Context context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].[M].Lead(-1)" );
        assertEquals( "F", member.getName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLeadLastInLevel(Context context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].[M].Lead(3)" );
        assertNull( member );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLeadNull(Context context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].Parent.Lead(1)" );
        assertNull( member );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLeadZero(Context context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].[F].Lead(0)" );
        assertEquals( "F", member.getName() );
    }

}
