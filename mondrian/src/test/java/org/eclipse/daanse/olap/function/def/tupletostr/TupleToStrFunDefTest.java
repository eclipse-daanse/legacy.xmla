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
package org.eclipse.daanse.olap.function.def.tupletostr;

import static org.opencube.junit5.TestUtil.assertExprReturns;
import static org.opencube.junit5.TestUtil.assertExprThrows;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

class TupleToStrFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTupleToStr(Context context) {
        // Applied to a dimension (which becomes a member)
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "TupleToStr([Product])",
            "[Product].[Product].[All Products]" );

        // Applied to a dimension (invalid because has no default hierarchy)

        assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
            "TupleToStr([Time])",
            "Could not Calculate the default hierarchy of the given dimension 'Time'. It may contains more than one hierarchy. Specify the hierarchy explicitly." );

        // Applied to a hierarchy
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "TupleToStr([Time].[Time])",
            "[Time].[Time].[1997]" );

        // Applied to a member
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "TupleToStr([Store].[USA].[OR])",
            "[Store].[Store].[USA].[OR]" );

        // Applied to a member (extra set of parens)
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "TupleToStr(([Store].[USA].[OR]))",
            "([Store].[Store].[USA].[OR])" );

        // Now, applied to a tuple
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "TupleToStr(([Marital Status], [Gender].[M]))",
            "([Marital Status].[Marital Status].[All Marital Status], [Gender].[Gender].[M])" );

        // Applied to a tuple containing a null member
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "TupleToStr(([Marital Status], [Gender].Parent))",
            "" );

        // Applied to a null member
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "TupleToStr([Marital Status].Parent)",
            "" );
    }

}
