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
package org.eclipse.daanse.olap.function.def.name.member;

import static mondrian.olap.fun.FunctionTest.assertExprReturns;
import static org.opencube.junit5.TestUtil.isDefaultNullMemberRepresentation;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class NameFunDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMemberName(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(), "[Time].[1997].Name", "1997" );
        // dimension name
        assertExprReturns(context.getConnectionWithDefaultRole(), "[Store].Name", "Store" );
        // member name
        assertExprReturns(context.getConnectionWithDefaultRole(), "[Store].DefaultMember.Name", "All Stores" );
        if ( isDefaultNullMemberRepresentation() ) {
            // name of null member
            assertExprReturns(context.getConnectionWithDefaultRole(), "[Store].Parent.Name", "#null" );
        }
    }

}
