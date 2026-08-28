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

import static org.eclipse.daanse.olap.common.Util.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opencube.junit5.TestUtil.executeSingletonAxis;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.FoodmartTestInstance;
import org.eclipse.daanse.rolap.testkit.junit.api.RolapContextTest;
import org.junit.jupiter.api.Test;

@RolapContextTest(FoodmartTestInstance.class)
class FirstSiblingFunDefTest {


    @Test
    void testFirstSiblingFirstInLevel(Context<?> context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].[F].FirstSibling", "Sales" );
        assertEquals( "F", member.getName() );
    }

    @Test
    void testFirstSiblingLastInLevel(Context<?> context) {
        Member member =
            executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Time].[1997].[Q4].FirstSibling", "Sales" );
        assertEquals( "Q1", member.getName() );
    }

    @Test
    void testFirstSiblingAll(Context<?> context) {
        Member member =
            executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].[All Gender].FirstSibling", "Sales" );
        assertTrue( member.isAll() );
    }

    @Test
    void testFirstSiblingRoot(Context<?> context) {
        // The [Measures] hierarchy does not have an 'all' member, so
        // [Unit Sales] does not have a parent.
        Member member =
            executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Measures].[Store Sales].FirstSibling", "Sales" );
        assertEquals( "Unit Sales", member.getName() );
    }

    @Test
    void testFirstSiblingNull(Context<?> context) {
        Member member =
            executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Gender].[F].FirstChild.FirstSibling", "Sales" );
        assertNull( member );
    }


}
