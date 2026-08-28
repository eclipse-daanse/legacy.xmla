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
package org.eclipse.daanse.olap.function.def.numeric.ordinal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencube.junit5.TestUtil.executeExprRaw;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.FoodmartTestInstance;
import org.eclipse.daanse.rolap.testkit.junit.api.RolapContextTest;
import org.junit.jupiter.api.Test;

@RolapContextTest(FoodmartTestInstance.class)
class OrdinalFunDefTest {

    @Test
    void testOrdinal(Context<?> context) {
        //final Context<?> testContext<?> =
        //  getContext().withCube( "Sales Ragged" );
        Connection connection = context.getConnectionWithDefaultRole();
        Cell cell =
            executeExprRaw(connection, "Sales Ragged",
                "[Store].[All Stores].[Vatican].ordinal" );
        assertEquals(
            1,
            ( (Number) cell.getValue() ).intValue(), "Vatican is at level 1.");

        cell = executeExprRaw(connection, "Sales Ragged",
            "[Store].[All Stores].[USA].[Washington].ordinal" );
        assertEquals(
            3,
            ( (Number) cell.getValue() ).intValue(), "Washington is at level 3.");
    }

}
