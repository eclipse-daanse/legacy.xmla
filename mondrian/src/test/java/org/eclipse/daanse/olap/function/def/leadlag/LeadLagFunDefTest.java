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
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.FoodmartTestInstance;
import org.eclipse.daanse.rolap.testkit.junit.api.RolapContextTest;
import org.junit.jupiter.api.Test;

@RolapContextTest(FoodmartTestInstance.class)
class LeadLagFunDefTest {

    @Test
    void testLag(Context<?> context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Time].[1997].[Q4].[12].Lag(4)", "Sales" );
        assertEquals( "8", member.getName() );
    }

    @Test
    void testLagFirstInLevel(Context<?> context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].[F].Lag(1)", "Sales" );
        assertNull( member );
    }

    @Test
    void testLagAll(Context<?> context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].DefaultMember.Lag(2)", "Sales" );
        assertNull( member );
    }

    @Test
    void testLagRoot(Context<?> context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Time].[1998].Lag(1)", "Sales" );
        assertEquals( "1997", member.getName() );
    }

    @Test
    void testLagRootTooFar(Context<?> context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Time].[1998].Lag(2)", "Sales" );
        assertNull( member );
    }

    @Test
    void testLead(Context<?> context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Time].[1997].[Q2].[4].Lead(4)", "Sales" );
        assertEquals( "8", member.getName() );
    }

    @Test
    void testLeadNegative(Context<?> context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].[M].Lead(-1)", "Sales" );
        assertEquals( "F", member.getName() );
    }

    @Test
    void testLeadLastInLevel(Context<?> context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].[M].Lead(3)", "Sales" );
        assertNull( member );
    }

    @Test
    void testLeadNull(Context<?> context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].Parent.Lead(1)", "Sales" );
        assertNull( member );
    }

    @Test
    void testLeadZero(Context<?> context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].[F].Lead(0)", "Sales" );
        assertEquals( "F", member.getName() );
    }

}
