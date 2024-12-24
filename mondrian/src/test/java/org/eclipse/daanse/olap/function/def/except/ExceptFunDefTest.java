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
package org.eclipse.daanse.olap.function.def.except;

import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertAxisThrows;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class ExceptFunDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testExceptEmpty(Context context) {
        // If left is empty, result is empty.
        assertAxisReturns(context.getConnection(),
            "Except(Filter([Gender].Members, 1=0), {[Gender].[M]})", "" );

        // If right is empty, result is left.
        assertAxisReturns(context.getConnection(),
            "Except({[Gender].[M]}, Filter([Gender].Members, 1=0))",
            "[Gender].[M]" );
    }

    /**
     * Tests that Except() successfully removes crossjoined tuples from the axis results.  Previously, this would fail by
     * returning all tuples in the first argument to Except.  bug 1439627
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testExceptCrossjoin(Context context) {
        assertAxisReturns(context.getConnection(),
            "Except(CROSSJOIN({[Promotion Media].[All Media]},\n"
                + "                  [Product].[All Products].Children),\n"
                + "       CROSSJOIN({[Promotion Media].[All Media]},\n"
                + "                  {[Product].[All Products].[Drink]}))",
            "{[Promotion Media].[All Media], [Product].[Food]}\n"
                + "{[Promotion Media].[All Media], [Product].[Non-Consumable]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testExtract(Context context) {
        assertAxisReturns(context.getConnection(),
            "Extract(\n"
                + "Crossjoin({[Gender].[F], [Gender].[M]},\n"
                + "          {[Marital Status].Members}),\n"
                + "[Gender])",
            "[Gender].[F]\n" + "[Gender].[M]" );

        // Extract(<set>) with no dimensions is not valid
        assertAxisThrows(context.getConnection(),
            "Extract(Crossjoin({[Gender].[F], [Gender].[M]}, {[Marital Status].Members}))",
            "No function matches signature 'Extract(<Set>)'" );

        // Extract applied to non-constant dimension should fail
        assertAxisThrows(context.getConnection(),
            "Extract(Crossjoin([Gender].Members, [Store].Children), [Store].Hierarchy.Dimension)",
            "not a constant hierarchy: [Store].Hierarchy.Dimension" );

        // Extract applied to non-constant hierarchy should fail
        assertAxisThrows(context.getConnection(),
            "Extract(Crossjoin([Gender].Members, [Store].Children), [Store].Hierarchy)",
            "not a constant hierarchy: [Store].Hierarchy" );

        // Extract applied to set of members is OK (if silly). Duplicates are
        // removed, as always.
        assertAxisReturns(context.getConnection(),
            "Extract({[Gender].[M], [Gender].Members}, [Gender])",
            "[Gender].[M]\n"
                + "[Gender].[All Gender]\n"
                + "[Gender].[F]" );

        // Extract of hierarchy not in set fails
        assertAxisThrows(context.getConnection(),
            "Extract(Crossjoin([Gender].Members, [Store].Children), [Marital Status])",
            "hierarchy [Marital Status] is not a hierarchy of the expression Crossjoin([Gender].Members, [Store].Children)" );

        // Extract applied to empty set returns empty set
        assertAxisReturns(context.getConnection(),
            "Extract(Crossjoin({[Gender].Parent}, [Store].Children), [Store])",
            "" );

        // Extract applied to asymmetric set
        assertAxisReturns(context.getConnection(),
            "Extract(\n"
                + "{([Gender].[M], [Marital Status].[M]),\n"
                + " ([Gender].[F], [Marital Status].[M]),\n"
                + " ([Gender].[M], [Marital Status].[S])},\n"
                + "[Gender])",
            "[Gender].[M]\n" + "[Gender].[F]" );

        // Extract applied to asymmetric set (other side)
        assertAxisReturns(context.getConnection(),
            "Extract(\n"
                + "{([Gender].[M], [Marital Status].[M]),\n"
                + " ([Gender].[F], [Marital Status].[M]),\n"
                + " ([Gender].[M], [Marital Status].[S])},\n"
                + "[Marital Status])",
            "[Marital Status].[M]\n"
                + "[Marital Status].[S]" );

        // Extract more than one hierarchy
        assertAxisReturns(context.getConnection(),
            "Extract(\n"
                + "[Gender].Children * [Marital Status].Children * [Time].[1997].Children * [Store].[USA].Children,\n"
                + "[Time], [Marital Status])",
            "{[Time].[1997].[Q1], [Marital Status].[M]}\n"
                + "{[Time].[1997].[Q2], [Marital Status].[M]}\n"
                + "{[Time].[1997].[Q3], [Marital Status].[M]}\n"
                + "{[Time].[1997].[Q4], [Marital Status].[M]}\n"
                + "{[Time].[1997].[Q1], [Marital Status].[S]}\n"
                + "{[Time].[1997].[Q2], [Marital Status].[S]}\n"
                + "{[Time].[1997].[Q3], [Marital Status].[S]}\n"
                + "{[Time].[1997].[Q4], [Marital Status].[S]}" );

        // Extract duplicate hierarchies fails
        assertAxisThrows(context.getConnection(),
            "Extract(\n"
                + "{([Gender].[M], [Marital Status].[M]),\n"
                + " ([Gender].[F], [Marital Status].[M]),\n"
                + " ([Gender].[M], [Marital Status].[S])},\n"
                + "[Gender], [Gender])",
            "hierarchy [Gender] is extracted more than once" );
    }

}
