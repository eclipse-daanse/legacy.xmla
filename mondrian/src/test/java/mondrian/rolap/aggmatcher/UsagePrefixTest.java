/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.rolap.aggmatcher;

import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.withSchema;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.SystemWideProperties;
import mondrian.rolap.SchemaModifiers;

/**
 * Validates the dimension attribute usagePrefix is correctly
 * applied when querying aggregate tables.
 * http://jira.pentaho.com/browse/MONDRIAN-595
 *
 * @author Matt Campbell
 */
class UsagePrefixTest extends AggTableTestCase {

    private static final String MONDRIAN_595_CSV = "MONDRIAN-595.csv";

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testUsagePrefix(Context<?> context) throws Exception {
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        prepareContext(context);
        if (!isApplicable(context.getConnectionWithDefaultRole())) {
            return;
        }
        SystemWideProperties props = SystemWideProperties.instance();

        // get value without aggregates
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);

        String mdx =
            "select {[StoreX].[Store Value].members} on columns, "
                +   "{ measures.[Amount] } on rows from Cheques";

        withSchema(context, SchemaModifiers.UsagePrefixTestModifier1::new);
        context.getConnectionWithDefaultRole().getCacheControl(null).flushSchemaCache();
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            +    "{}\n"
            +    "Axis #1:\n"
            +    "{[StoreX].[store1]}\n"
            +    "{[StoreX].[store2]}\n"
            +    "{[StoreX].[store3]}\n"
            +    "Axis #2:\n"
            +    "{[Measures].[Amount]}\n"
            +    "Row #0: 05.0\n"
            +    "Row #0: 02.5\n"
            +    "Row #0: 02.0\n");
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testUsagePrefixTwoDims(Context<?> context) throws Exception {
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        prepareContext(context);
        if (!isApplicable(context.getConnectionWithDefaultRole())) {
            return;
        }

        // get value without aggregates
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);

        String mdx =
            "select Crossjoin([StoreX].[Store Value].members, "
            + " [StoreY].[Store Value].members) on columns, "
            +   "{ measures.[Amount] } on rows from Cheques";

        withSchema(context, SchemaModifiers.UsagePrefixTestModifier1::new);
        context.getConnectionWithDefaultRole().getCacheControl(null).flushSchemaCache();
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
                "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[StoreX].[store1], [StoreY].[OtherStore1]}\n"
                + "{[StoreX].[store1], [StoreY].[OtherStore2]}\n"
                + "{[StoreX].[store1], [StoreY].[OtherStore3]}\n"
                + "{[StoreX].[store2], [StoreY].[OtherStore1]}\n"
                + "{[StoreX].[store2], [StoreY].[OtherStore2]}\n"
                + "{[StoreX].[store2], [StoreY].[OtherStore3]}\n"
                + "{[StoreX].[store3], [StoreY].[OtherStore1]}\n"
                + "{[StoreX].[store3], [StoreY].[OtherStore2]}\n"
                + "{[StoreX].[store3], [StoreY].[OtherStore3]}\n"
                + "Axis #2:\n"
                + "{[Measures].[Amount]}\n"
                + "Row #0: 05.0\n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: 02.5\n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: \n"
                + "Row #0: 02.0\n");
    }


    @Override
	protected String getFileName() {
        return MONDRIAN_595_CSV;
    }


}
