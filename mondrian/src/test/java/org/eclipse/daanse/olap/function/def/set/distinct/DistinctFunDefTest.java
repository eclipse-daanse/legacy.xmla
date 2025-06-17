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
package org.eclipse.daanse.olap.function.def.set.distinct;

import static org.opencube.junit5.TestUtil.assertAxisReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class DistinctFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDistinctTwoMembers(Context<?> context) {
        //getTestContext().withCube( "HR" ).
        assertAxisReturns(context.getConnectionWithDefaultRole(), "HR",
            "Distinct({[Employees].[All Employees].[Sheri Nowmer].[Donna Arnold],"
                + "[Employees].[Sheri Nowmer].[Donna Arnold]})",
            "[Employees].[Employees].[Sheri Nowmer].[Donna Arnold]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDistinctThreeMembers(Context<?> context) {
        //getTestContext().withCube( "HR" ).
        assertAxisReturns(context.getConnectionWithDefaultRole(), "HR",
            "Distinct({[Employees].[All Employees].[Sheri Nowmer].[Donna Arnold],"
                + "[Employees].[All Employees].[Sheri Nowmer].[Darren Stanz],"
                + "[Employees].[All Employees].[Sheri Nowmer].[Donna Arnold]})",
            "[Employees].[Employees].[Sheri Nowmer].[Donna Arnold]\n"
                + "[Employees].[Employees].[Sheri Nowmer].[Darren Stanz]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDistinctFourMembers(Context<?> context) {
        //getTestContext().withCube( "HR" ).
        assertAxisReturns(context.getConnectionWithDefaultRole(), "HR",
            "Distinct({[Employees].[All Employees].[Sheri Nowmer].[Donna Arnold],"
                + "[Employees].[All Employees].[Sheri Nowmer].[Darren Stanz],"
                + "[Employees].[All Employees].[Sheri Nowmer].[Donna Arnold],"
                + "[Employees].[All Employees].[Sheri Nowmer].[Darren Stanz]})",
            "[Employees].[Employees].[Sheri Nowmer].[Donna Arnold]\n"
                + "[Employees].[Employees].[Sheri Nowmer].[Darren Stanz]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDistinctTwoTuples(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Distinct({([Time].[1997],[Store].[All Stores].[Mexico]), "
                + "([Time].[1997], [Store].[All Stores].[Mexico])})",
            "{[Time].[Time].[1997], [Store].[Store].[Mexico]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDistinctSomeTuples(Context<?> context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Distinct({([Time].[1997],[Store].[All Stores].[Mexico]), "
                + "crossjoin({[Time].[1997]},{[Store].[All Stores].children})})",
            "{[Time].[Time].[1997], [Store].[Store].[Mexico]}\n"
                + "{[Time].[Time].[1997], [Store].[Store].[Canada]}\n"
                + "{[Time].[Time].[1997], [Store].[Store].[USA]}" );
    }

}
