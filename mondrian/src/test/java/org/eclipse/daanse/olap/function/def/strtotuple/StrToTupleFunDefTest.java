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
package org.eclipse.daanse.olap.function.def.strtotuple;

import static mondrian.olap.fun.FunctionTest.allHiersExcept;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertAxisThrows;
import static org.opencube.junit5.TestUtil.assertExprDependsOn;
import static org.opencube.junit5.TestUtil.assertMemberExprDependsOn;
import static org.opencube.junit5.TestUtil.hierarchyName;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

class StrToTupleFunDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testStrToTuple(Context<?> context) {
        // single dimension yields member
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "{StrToTuple(\"[Time].[1997].[Q2]\", [Time])}",
            "[Time].[Time].[1997].[Q2]" );

        // multiple dimensions yield tuple
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "{StrToTuple(\"([Gender].[F], [Time].[1997].[Q2])\", [Gender], [Time])}",
            "{[Gender].[Gender].[F], [Time].[Time].[1997].[Q2]}" );

        // todo: test for garbage at end of string
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testStrToTupleIgnoreInvalidMembers(Context<?> context) {
        context.getCatalogCache().clear();
        ((TestContextImpl)context).setIgnoreInvalidMembersDuringQuery(true);
        // If any member is invalid, the whole tuple is null.
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "StrToTuple(\"([Gender].[M], [Marital Status].[Separated])\","
                + " [Gender], [Marital Status])",
            "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testStrToTupleDuHierarchiesFails(Context<?> context) {
        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "{StrToTuple(\"([Gender].[F], [Time].[1997].[Q2], [Gender].[M])\", [Gender], [Time], [Gender])}",
            "Tuple contains more than one member of hierarchy '[Gender].[Gender]'.", "Sales" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testStrToTupleDupHierInSameDimensions(Context<?> context) {
        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "{StrToTuple("
                + "\"([Gender].[F], "
                + "[Time].[1997].[Q2], "
                + "[Time].[Weekly].[1997].[10])\","
                + " [Gender], "
                + hierarchyName( "Time", "Weekly" )
                + ", [Gender])}",
            "Tuple contains more than one member of hierarchy '[Gender].[Gender]'.", "Sales" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testStrToTupleDepends(Context<?> context) {
        assertMemberExprDependsOn(context.getConnectionWithDefaultRole(),
            "StrToTuple(\"[Time].[1997].[Q2]\", [Time])",
            "{}" );

        // converted to scalar, depends set is larger
        assertExprDependsOn(context.getConnectionWithDefaultRole(),
            "StrToTuple(\"[Time].[1997].[Q2]\", [Time])",
            allHiersExcept( "[Time].[Time]" ) );

        assertMemberExprDependsOn(context.getConnectionWithDefaultRole(),
            "StrToTuple(\"[Time].[1997].[Q2], [Gender].[F]\", [Time], [Gender])",
            "{}" );

        assertExprDependsOn(context.getConnectionWithDefaultRole(),
            "StrToTuple(\"[Time].[1997].[Q2], [Gender].[F]\", [Time], [Gender])",
            allHiersExcept( "[Time].[Time]", "[Gender].[Gender]" ) );
    }

}
