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
package org.eclipse.daanse.olap.function.def.strtoset;

import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertAxisThrows;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

class StrToSetFunDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testStrToSet(Context<?> context) {
        // TODO: handle text after '}'
        // TODO: handle string which ends too soon
        // TODO: handle spaces before first '{'
        // TODO: test spaces before unbracketed names,
        //       e.g. "{Gender. M, Gender. F   }".

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "StrToSet("
                + " \"{[Gender].[F], [Gender].[M]}\","
                + " [Gender])",
            "[Gender].[Gender].[F]\n"
                + "[Gender].[Gender].[M]" );

        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "StrToSet("
                + " \"{[Gender].[F], [Time].[1997]}\","
                + " [Gender])",
            "member is of wrong hierarchy", "Sales" );

        // whitespace ok
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "StrToSet("
                + " \"  {   [Gender] .  [F]  ,[Gender].[M] }  \","
                + " [Gender])",
            "[Gender].[Gender].[F]\n"
                + "[Gender].[Gender].[M]" );

        // tuples
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "StrToSet("
                + "\""
                + "{"
                + " ([Gender].[F], [Time].[1997].[Q2]), "
                + " ([Gender].[M], [Time].[1997])"
                + "}"
                + "\","
                + " [Gender],"
                + " [Time])",
            "{[Gender].[Gender].[F], [Time].[Time].[1997].[Q2]}\n"
                + "{[Gender].[Gender].[M], [Time].[Time].[1997]}" );

        // matches unique name
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "StrToSet("
                + "\""
                + "{"
                + " [Store].[USA].[CA], "
                + " [Store].[All Stores].[USA].OR,"
                + " [Store].[All Stores]. [USA] . [WA]"
                + "}"
                + "\","
                + " [Store])",
            "[Store].[Store].[USA].[CA]\n"
                + "[Store].[Store].[USA].[OR]\n"
                + "[Store].[Store].[USA].[WA]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testStrToSetDupDimensionsFails(Context<?> context) {
        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "StrToSet("
                + "\""
                + "{"
                + " ([Gender].[F], [Time].[1997].[Q2], [Gender].[F]), "
                + " ([Gender].[M], [Time].[1997], [Gender].[F])"
                + "}"
                + "\","
                + " [Gender],"
                + " [Time],"
                + " [Gender])",
            "Tuple contains more than one member of hierarchy '[Gender].[Gender]'.", "Sales" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testStrToSetIgnoreInvalidMembers(Context<?> context) {
        context.getCatalogCache().clear();
        ((TestContextImpl)context).setIgnoreInvalidMembersDuringQuery(true);
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "StrToSet("
                + "\""
                + "{"
                + " [Product].[Food],"
                + " [Product].[Food].[You wouldn't like],"
                + " [Product].[Drink].[You would like],"
                + " [Product].[Drink].[Dairy]"
                + "}"
                + "\","
                + " [Product])",
            "[Product].[Product].[Food]\n"
                + "[Product].[Product].[Drink].[Dairy]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "StrToSet("
                + "\""
                + "{"
                + " ([Gender].[M], [Product].[Food]),"
                + " ([Gender].[F], [Product].[Food].[You wouldn't like]),"
                + " ([Gender].[M], [Product].[Drink].[You would like]),"
                + " ([Gender].[F], [Product].[Drink].[Dairy])"
                + "}"
                + "\","
                + " [Gender], [Product])",
            "{[Gender].[Gender].[M], [Product].[Product].[Food]}\n"
                + "{[Gender].[Gender].[F], [Product].[Product].[Drink].[Dairy]}" );
    }


}
