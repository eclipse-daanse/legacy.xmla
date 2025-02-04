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
package org.eclipse.daanse.olap.function.def.member.strtomember;

import static org.opencube.junit5.TestUtil.assertAxisThrows;
import static org.opencube.junit5.TestUtil.assertExprReturns;
import static org.opencube.junit5.TestUtil.assertExprThrows;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertQueryThrows;
import static org.opencube.junit5.TestUtil.hierarchyName;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestConfig;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.rolap.RolapSchemaCache;


class StrToMemberFunDefTest {
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testStrToMember(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "StrToMember(\"[Time].[1997].[Q2].[4]\").Name",
            "4" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testStrToMemberUniqueName(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "StrToMember(\"[Store].[USA].[CA]\").Name",
            "CA" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testStrToMemberFullyQualifiedName(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "StrToMember(\"[Store].[All Stores].[USA].[CA]\").Name",
            "CA" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testStrToMemberNull(Context context) {
        // SSAS 2005 gives "#Error An MDX expression was expected. An empty
        // expression was specified."
        assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
            "StrToMember(null).Name",
            "An MDX expression was expected. An empty expression was specified" );
        assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
            "StrToSet(null, [Gender]).Count",
            "An MDX expression was expected. An empty expression was specified" );
        assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
            "StrToTuple(null, [Gender]).Name",
            "An MDX expression was expected. An empty expression was specified" );
    }

    /**
     * Testcase for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-560">
     * bug MONDRIAN-560, "StrToMember function doesn't use IgnoreInvalidMembers option"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testStrToMemberIgnoreInvalidMembers(Context context) {
        context.getSchemaCache().clear();
        ((TestConfig)context.getConfig()).setIgnoreInvalidMembersDuringQuery(true);

        // [Product].[Drugs] is invalid, becomes null member, and is dropped
        // from list
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select \n"
                + "  {[Product].[Food],\n"
                + "    StrToMember(\"[Product].[Drugs]\")} on columns,\n"
                + "  {[Measures].[Unit Sales]} on rows\n"
                + "from [Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Product].[Food]}\n"
                + "Axis #2:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "Row #0: 191,940\n" );

        // Hierarchy is inferred from leading edge
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "StrToMember(\"[Marital Status].[Separated]\").Hierarchy.Name",
            "Marital Status" );

        // Null member is returned
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "StrToMember(\"[Marital Status].[Separated]\").Name",
            "#null" );

        // Use longest valid prefix, so get [Time].[Weekly] rather than just
        // [Time].
        final String timeWeekly = hierarchyName( "Time", "Weekly" );
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "StrToMember(\"" + timeWeekly
                + ".[1996].[Q1]\").Hierarchy.UniqueName",
            timeWeekly );

        // If hierarchy is invalid, throw an error even though
        // IgnoreInvalidMembersDuringQuery is set.
        assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
            "StrToMember(\"[Unknown Hierarchy].[Invalid].[Member]\").Name",
            "MDX object '[Unknown Hierarchy].[Invalid].[Member]' not found in cube 'Sales'" );
        assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
            "StrToMember(\"[Unknown Hierarchy].[Invalid]\").Name",
            "MDX object '[Unknown Hierarchy].[Invalid]' not found in cube 'Sales'" );
        assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
            "StrToMember(\"[Unknown Hierarchy]\").Name",
            "MDX object '[Unknown Hierarchy]' not found in cube 'Sales'" );

        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "StrToMember(\"\")",
            "MDX object '' not found in cube 'Sales'", "Sales" );

        ((TestConfig)context.getConfig()).setIgnoreInvalidMembersDuringQuery(false);
        assertQueryThrows(context,
            "select \n"
                + "  {[Product].[Food],\n"
                + "    StrToMember(\"[Product].[Drugs]\")} on columns,\n"
                + "  {[Measures].[Unit Sales]} on rows\n"
                + "from [Sales]",
            "Member '[Product].[Drugs]' not found" );
        assertExprThrows(context, "Sales",
            "StrToMember(\"[Marital Status].[Separated]\").Hierarchy.Name",
            "Member '[Marital Status].[Separated]' not found" );
    }

}
