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
package org.eclipse.daanse.olap.function.def.operators.or;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.opencube.junit5.TestUtil.assertBooleanExprReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;

import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class OrOperatorDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOr2(Context context) {
        assertBooleanExprReturns(context.getConnectionWithDefaultRole(), "Sales", " 1=0 OR 0=0 ", true );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOrAssociativity1(Context context) {
        // Would give 'false' if OR were stronger than AND (wrong!)
        assertBooleanExprReturns(context.getConnectionWithDefaultRole(), "Sales", " 1=1 AND 1=0 OR 1=1 ", true );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOrAssociativity2(Context context) {
        // Would give 'false' if OR were stronger than AND (wrong!)
        assertBooleanExprReturns(context.getConnectionWithDefaultRole(), "Sales", " 1=1 OR 1=0 AND 1=1 ", true );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOrAssociativity3(Context context) {
        assertBooleanExprReturns(context.getConnectionWithDefaultRole(), "Sales", " (1=0 OR 1=1) AND 1=1 ", true );
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testComplexOrExpr(Context context) {
        Connection connection = context.getConnectionWithDefaultRole();
        switch (getDatabaseProduct(TestUtil.getDialect(connection).getDialectName())) {
            case INFOBRIGHT:
                // Skip this test on Infobright, because [Promotion Sales] is
                // defined wrong.
                return;
        }

        // make sure all aggregates referenced in the OR expression are
        // processed in a single load request by setting the eval depth to
        // a value smaller than the number of measures
        int origDepth = context.getConfigValue(ConfigConstants.MAX_EVAL_DEPTH, ConfigConstants.MAX_EVAL_DEPTH_DEFAULT_VALUE, Integer.class);
        ((TestContextImpl)context).setMaxEvalDepth( 3 );
        assertQueryReturns(connection,
            "with set [*NATIVE_CJ_SET] as '[Store].[Store Country].members' "
                + "set [*GENERATED_MEMBERS_Measures] as "
                + "    '{[Measures].[Unit Sales], [Measures].[Store Cost], "
                + "    [Measures].[Sales Count], [Measures].[Customer Count], "
                + "    [Measures].[Promotion Sales]}' "
                + "set [*GENERATED_MEMBERS] as "
                + "    'Generate([*NATIVE_CJ_SET], {[Store].CurrentMember})' "
                + "member [Store].[*SUBTOTAL_MEMBER_SEL~SUM] as 'Sum([*GENERATED_MEMBERS])' "
                + "select [*GENERATED_MEMBERS_Measures] ON COLUMNS, "
                + "NON EMPTY "
                + "    Filter("
                + "        Generate("
                + "        [*NATIVE_CJ_SET], "
                + "        {[Store].CurrentMember}), "
                + "        (((((NOT IsEmpty([Measures].[Unit Sales])) OR "
                + "            (NOT IsEmpty([Measures].[Store Cost]))) OR "
                + "            (NOT IsEmpty([Measures].[Sales Count]))) OR "
                + "            (NOT IsEmpty([Measures].[Customer Count]))) OR "
                + "            (NOT IsEmpty([Measures].[Promotion Sales])))) "
                + "on rows "
                + "from [Sales]",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "{[Measures].[Store Cost]}\n"
                + "{[Measures].[Sales Count]}\n"
                + "{[Measures].[Customer Count]}\n"
                + "{[Measures].[Promotion Sales]}\n"
                + "Axis #2:\n"
                + "{[Store].[Store].[USA]}\n"
                + "Row #0: 266,773\n"
                + "Row #0: 225,627.23\n"
                + "Row #0: 86,837\n"
                + "Row #0: 5,581\n"
                + "Row #0: 151,211.21\n" );
        ((TestContextImpl)context).setMaxEvalDepth( origDepth );
    }

}
