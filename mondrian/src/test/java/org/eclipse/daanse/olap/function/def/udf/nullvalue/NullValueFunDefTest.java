/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.function.def.udf.nullvalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.result.Cell;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

class NullValueFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testNullValue(Context<?> context) {
        Connection connection=context.getConnectionWithDefaultRole();
        String cubeName="Sales";
        Cell c=  TestUtil.executeExprRaw(connection,cubeName," NullValue()/NullValue() ");
        String s=c.getFormattedValue();
        assertEquals("", s);

        c = TestUtil.executeExprRaw(connection,cubeName," NullValue()/NullValue() = NULL ");
        s=c.getFormattedValue();

        assertEquals("false", s);

        boolean hasException = false;
        try {
            c = TestUtil.executeExprRaw(connection,cubeName," NullValue() IS NULL ");
            s=c.getFormattedValue();
        } catch (Exception ex) {
            hasException = true;
        }
        assertTrue(hasException);

        // I believe that these IsEmpty results are correct.
        // The NullValue function does not represent a cell.
        c = TestUtil.executeExprRaw(connection,cubeName," IsEmpty(NullValue()) ");
        s=c.getFormattedValue();

        assertEquals("false", s);

        // NullValue()/NullValue() evaluates to DoubleNull
        // but DoubleNull evaluates to null, so this seems
        // to be broken??
        // s = executeExpr(" IsEmpty(NullValue()/NullValue()) ");
        // assertEquals("false", s);

        c = TestUtil.executeExprRaw(connection,cubeName," 4 + NullValue() ");
        s=c.getFormattedValue();
        assertEquals("4", s);

        c = TestUtil.executeExprRaw(connection,cubeName," NullValue() - 4 ");
        s=c.getFormattedValue();
        assertEquals("-4", s);

        c = TestUtil.executeExprRaw(connection,cubeName," 4*NullValue() ");
        s=c.getFormattedValue();
        assertEquals("", s);

        c = TestUtil.executeExprRaw(connection,cubeName," NullValue()*4 ");
        s=c.getFormattedValue();
        assertEquals("", s);

        c = TestUtil.executeExprRaw(connection,cubeName," 4/NullValue() ");
        s=c.getFormattedValue();
        assertEquals("Infinity", s);

        c = TestUtil.executeExprRaw(connection,cubeName," NullValue()/4 ");
        s=c.getFormattedValue();
        assertEquals("", s);
        /*
         */
    }

}
