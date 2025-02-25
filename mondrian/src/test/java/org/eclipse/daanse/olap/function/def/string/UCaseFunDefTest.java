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
package org.eclipse.daanse.olap.function.def.string;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.executeQuery;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.fun.MondrianEvaluationException;


class UCaseFunDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUCaseWithNonEmptyString(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select filter([Store].MEMBERS, "
                + " UCase([Store].CURRENTMEMBER.Name) = \"BELLINGHAM\") "
                + "on 0 from sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Store].[Store].[USA].[WA].[Bellingham]}\n"
                + "Row #0: 2,237\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUCaseWithEmptyString(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select filter([Store].MEMBERS, "
                + " UCase(\"\") = \"\" "
                + "And [Store].CURRENTMEMBER.Name = \"Bellingham\") "
                + "on 0 from sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Store].[Store].[USA].[WA].[Bellingham]}\n"
                + "Row #0: 2,237\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUCaseWithNullString(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select filter([Store].MEMBERS, "
                + " UCase(\"NULL\") = \"\" "
                + "And [Store].CURRENTMEMBER.Name = \"Bellingham\") "
                + "on 0 from sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUCaseWithNull(Context context) {
        try {
            executeQuery(context.getConnectionWithDefaultRole(),
                "select filter([Store].MEMBERS, "
                    + " UCase(NULL) = \"\" "
                    + "And [Store].CURRENTMEMBER.Name = \"Bellingham\") "
                    + "on 0 from sales" );
        } catch ( OlapRuntimeException e ) {
            Throwable mondrianEvaluationException = e.getCause();
            assertEquals(
                mondrianEvaluationException.getClass(),
                ( MondrianEvaluationException.class ) );
            assertEquals( "No method with the signature UCase(NULL) matches known functions.",
                mondrianEvaluationException.getMessage() );
            return;
        }
        fail( "MondrianEvaluationException is expected here" );
    }

}
