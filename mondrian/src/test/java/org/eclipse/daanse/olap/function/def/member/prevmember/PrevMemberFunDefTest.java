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
package org.eclipse.daanse.olap.function.def.member.prevmember;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencube.junit5.TestUtil.executeQuery;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.result.Result;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

class PrevMemberFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAll2(Context context) {
        Result result =
            executeQuery(context.getConnectionWithDefaultRole(), "select {[Gender].PrevMember} ON COLUMNS from Sales" );
        // previous to [Gender].[All] is null, so no members are returned
        assertEquals( 0, result.getAxes()[ 0 ].getPositions().size() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBasic(Context context) {
        Result result =
            executeQuery(context.getConnectionWithDefaultRole(),
                "select {[Gender].[M].PrevMember} ON COLUMNS from Sales" );
        assertEquals(
            "F",
            result.getAxes()[ 0 ].getPositions().get( 0 ).get( 0 ).getName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFirstInLevel(Context context) {
        Result result =
            executeQuery(context.getConnectionWithDefaultRole(),
                "select {[Gender].[F].PrevMember} ON COLUMNS from Sales" );
        assertEquals( 0, result.getAxes()[ 0 ].getPositions().size() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAll(Context context) {
        Result result =
            executeQuery(context.getConnectionWithDefaultRole(), "select {[Gender].PrevMember} ON COLUMNS from Sales" );
        // previous to [Gender].[All] is null, so no members are returned
        assertEquals( 0, result.getAxes()[ 0 ].getPositions().size() );
    }

}
