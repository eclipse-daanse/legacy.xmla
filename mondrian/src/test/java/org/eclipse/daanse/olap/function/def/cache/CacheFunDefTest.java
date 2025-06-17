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
package org.eclipse.daanse.olap.function.def.cache;

import static mondrian.olap.fun.FunctionTest.assertExprReturns;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertExprThrows;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class CacheFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCache(Context<?> context) {
        // test various data types: integer, string, member, set, tuple
        assertExprReturns(context.getConnectionWithDefaultRole(), "Cache(1 + 2)", "3" );
        assertExprReturns(context.getConnectionWithDefaultRole(), "Cache('foo' || 'bar')", "foobar" );
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "[Gender].Children",
            "[Gender].[Gender].[F]\n"
                + "[Gender].[Gender].[M]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "([Gender].[M], [Marital Status].[S].PrevMember)",
            "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[M]}" );

        // inside another expression
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Order(Cache([Gender].Children), Cache(([Measures].[Unit Sales], [Time].[1997].[Q1])), BDESC)",
            "[Gender].[Gender].[M]\n"
                + "[Gender].[Gender].[F]" );

        // doesn't work with multiple args
        assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
            "Cache(1, 2)",
            "No function matches signature 'Cache(<Numeric Expression>, <Numeric Expression>)'" );
    }


}
