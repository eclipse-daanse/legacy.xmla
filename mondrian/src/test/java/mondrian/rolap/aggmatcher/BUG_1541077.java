/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.rolap.aggmatcher;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

/**
 * Testcase for
 * <a href="http://jira.pentaho.com/browse/MONDRIAN-214">MONDRIAN-214</a>
 * (formerly SourceForge bug 1541077)
 * and a couple of other aggregate table ExplicitRecognizer conditions.
 *
 * @author Richard M. Emberson
 */
public class BUG_1541077 extends AggTableTestCase {

    private static final String BUG_1541077 = "BUG_1541077.csv";

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    public void testStoreCount(Context<?> context) throws Exception {
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        prepareContext(context);
        if (!isApplicable(context.getConnectionWithDefaultRole())) {
            return;
        }

        // get value without aggregates
        ((TestContextImpl)context).setUseAggregates(false);

        String mdx =
            "select {[Measures].[Store Count]} on columns from Cheques";
        Result result = executeQuery(mdx, context.getConnectionWithDefaultRole());
        Object v = result.getCell(new int[]{0}).getValue();

        ((TestContextImpl)context).setUseAggregates(true);

        Result result1 = executeQuery(mdx, context.getConnectionWithDefaultRole());
        Object v1 = result1.getCell(new int[]{0}).getValue();

        assertTrue(v.equals(v1));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    public void testSalesCount(Context<?> context) throws Exception {
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        prepareContext(context);
        if (!isApplicable(context.getConnectionWithDefaultRole())) {
            return;
        }

        // get value without aggregates
        ((TestContextImpl)context).setUseAggregates(false);

        String mdx =
            "select {[Measures].[Sales Count]} on columns from Cheques";
        Result result = executeQuery(mdx, context.getConnectionWithDefaultRole());
        Object v = result.getCell(new int[]{0}).getValue();

        ((TestContextImpl)context).setUseAggregates(true);

        Result result1 = executeQuery(mdx, context.getConnectionWithDefaultRole());
        Object v1 = result1.getCell(new int[]{0}).getValue();

        assertTrue(v.equals(v1));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    public void testTotalAmount(Context<?> context) throws Exception {
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        prepareContext(context);
        if (!isApplicable(context.getConnectionWithDefaultRole())) {
            return;
        }

        // get value without aggregates
        ((TestContextImpl)context).setUseAggregates(false);

        String mdx =
            "select {[Measures].[Total Amount]} on columns from Cheques";
        Result result = executeQuery(mdx, context.getConnectionWithDefaultRole());
        Object v = result.getCell(new int[]{0}).getValue();

        ((TestContextImpl)context).setUseAggregates(false);

        Result result1 = executeQuery(mdx, context.getConnectionWithDefaultRole());
        Object v1 = result1.getCell(new int[]{0}).getValue();

        assertTrue(v.equals(v1));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    public void testBug1541077(Context<?> context) throws Exception {
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        prepareContext(context);
        if (!isApplicable(context.getConnectionWithDefaultRole())) {
            return;
        }

        // get value without aggregates
        ((TestContextImpl)context).setUseAggregates(false);

        String mdx = "select {[Measures].[Avg Amount]} on columns from Cheques";

        Result result = executeQuery(mdx, context.getConnectionWithDefaultRole());
        Object v = result.getCell(new int[]{0}).getFormattedValue();

        // get value with aggregates
        ((TestContextImpl)context).setUseAggregates(true);

        Result result1 = executeQuery(mdx, context.getConnectionWithDefaultRole());
        Object v1 = result1.getCell(new int[]{0}).getFormattedValue();

        assertTrue(v.equals(v1));
    }

    @Override
	protected String getFileName() {
        return BUG_1541077;
    }

    protected Function<CatalogMapping, PojoMappingModifier> getModifierFunction(){
        return BUG_1541077Modifier::new;
    }

}
