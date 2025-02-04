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
package org.eclipse.daanse.olap.function.def.numeric.value;

import static org.opencube.junit5.TestUtil.assertExprDependsOn;
import static org.opencube.junit5.TestUtil.assertExprThrows;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.fun.FunctionTest;


class ValueFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testValue(Context context) {
        // VALUE is usually a cell property, not a member property.
        // We allow it because MS documents it as a function, <Member>.VALUE.
        TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales", "[Measures].[Store Sales].VALUE", "565,238.13" );

        // Depends upon almost everything.
        String s1 = FunctionTest.allHiersExcept( "[Measures]" );
        assertExprDependsOn(context.getConnectionWithDefaultRole(),
            "[Measures].[Store Sales].VALUE", s1 );

        // We do not allow FORMATTED_VALUE.
        assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
            "[Measures].[Store Sales].FORMATTED_VALUE",
            "MDX object '[Measures].[Store Sales].FORMATTED_VALUE' not found in cube 'Sales'" );

        TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales", "[Measures].[Store Sales].NAME", "Store Sales" );
        // MS says that ID and KEY are standard member properties for
        // OLE DB for OLAP, but not for XML/A. We don't support them.
        assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
            "[Measures].[Store Sales].ID",
            "MDX object '[Measures].[Store Sales].ID' not found in cube 'Sales'" );

        // Error for KEY is slightly different than for ID. It doesn't matter
        // very much.
        //
        // The error is different because KEY is registered as a Mondrian
        // builtin property, but ID isn't. KEY cannot be evaluated in
        // "<MEMBER>.KEY" syntax because there is not function defined. For
        // other builtin properties, such as NAME, CAPTION there is a builtin
        // function.
        assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
            "[Measures].[Store Sales].KEY",
            "No function matches signature '<Member>.KEY'" );

        TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales", "[Measures].[Store Sales].CAPTION", "Store Sales" );
    }

}
