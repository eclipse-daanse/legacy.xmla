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

import mondrian.olap.SystemWideProperties;


class TupleToStrFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTupleToStr(Context context) {
        // Applied to a dimension (which becomes a member)
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "TupleToStr([Product])",
            "[Product].[All Products]" );

        // Applied to a dimension (invalid because has no default hierarchy)
        if ( SystemWideProperties.instance().SsasCompatibleNaming ) {
            assertExprThrows(context.getConnectionWithDefaultRole(),
                "TupleToStr([Time])",
                "The 'Time' dimension contains more than one hierarchy, "
                    + "therefore the hierarchy must be explicitly specified." );
        } else {
            assertExprReturns(context.getConnectionWithDefaultRole(),
                "TupleToStr([Time])",
                "[Time].[1997]" );
        }

        // Applied to a hierarchy
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "TupleToStr([Time].[Time])",
            "[Time].[1997]" );

        // Applied to a member
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "TupleToStr([Store].[USA].[OR])",
            "[Store].[USA].[OR]" );

        // Applied to a member (extra set of parens)
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "TupleToStr(([Store].[USA].[OR]))",
            "([Store].[USA].[OR])" );

        // Now, applied to a tuple
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "TupleToStr(([Marital Status], [Gender].[M]))",
            "([Marital Status].[All Marital Status], [Gender].[M])" );

        // Applied to a tuple containing a null member
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "TupleToStr(([Marital Status], [Gender].Parent))",
            "" );

        // Applied to a null member
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "TupleToStr([Marital Status].Parent)",
            "" );
    }

}
