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
package org.eclipse.daanse.olap.function.def.member.cousin;

import static mondrian.olap.exceptions.CousinHierarchyMismatchException.cousinHierarchyMismatch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opencube.junit5.TestUtil.assertAxisThrows;
import static org.opencube.junit5.TestUtil.executeSingletonAxis;

import java.text.MessageFormat;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Member;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

class CousinFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCousin1(Context context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "Cousin([1997].[Q4],[1998])" );
        assertEquals( "[Time].[1998].[Q4]", member.getUniqueName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCousin2(Context context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(),
            "Cousin([1997].[Q4].[12],[1998].[Q1])" );
        assertEquals( "[Time].[1998].[Q1].[3]", member.getUniqueName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCousinOverrun(Context context) {
        Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(),
            "Cousin([Customers].[USA].[CA].[San Jose],"
                + " [Customers].[USA].[OR])" );
        // CA has more cities than OR
        assertNull( member );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCousinThreeDown(Context context) {
        Member member =
            executeSingletonAxis(context.getConnectionWithDefaultRole(),
                "Cousin([Customers].[USA].[CA].[Berkeley].[Barbara Combs],"
                    + " [Customers].[Mexico])" );
        // Barbara Combs is the 6th child
        // of the 4th child (Berkeley)
        // of the 1st child (CA)
        // of USA
        // Annmarie Hill is the 6th child
        // of the 4th child (Tixapan)
        // of the 1st child (DF)
        // of Mexico
        assertEquals(
            "[Customers].[Mexico].[DF].[Tixapan].[Annmarie Hill]",
            member.getUniqueName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCousinSameLevel(Context context) {
        Member member =
            executeSingletonAxis(context.getConnectionWithDefaultRole(), "Cousin([Gender].[M], [Gender].[F])" );
        assertEquals( "F", member.getName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCousinHigherLevel(Context context) {
        Member member =
            executeSingletonAxis(context.getConnectionWithDefaultRole(), "Cousin([Time].[1997], [Time].[1998].[Q1])" );
        assertNull( member );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCousinWrongHierarchy(Context context) {
        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "Cousin([Time].[1997], [Gender].[M])",
            MessageFormat.format(cousinHierarchyMismatch,
                "[Time].[1997]",
                "[Gender].[M]" ) );
    }

}
