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
package org.eclipse.daanse.olap.function.def.uniquename.hierarchy;

import static mondrian.olap.fun.FunctionTest.assertExprReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class UniqueNameFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHierarchyUniqueName(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "[Gender].DefaultMember.Hierarchy.UniqueName",
            "[Gender]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTime(Context context) {
        TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(),
            "[Time].[1997].[Q1].[1].Hierarchy.UniqueName", "[Time]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBasic9(Context context) {
        TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(),
            "[Gender].[All Gender].[F].Hierarchy.UniqueName", "[Gender]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFirstInLevel9(Context context) {
        TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(),
            "[Education Level].[All Education Levels].[Bachelors Degree].Hierarchy.UniqueName",
            "[Education Level]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHierarchyAll(Context context) {
        TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(),
            "[Gender].[All Gender].Hierarchy.UniqueName", "[Gender]" );
    }

}
