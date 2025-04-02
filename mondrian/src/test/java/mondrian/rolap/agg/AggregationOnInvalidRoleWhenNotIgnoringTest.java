/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2015-2017 Hitachi Vantara..  All rights reserved.
*/
package mondrian.rolap.agg;

import static mondrian.rolap.agg.AggregationOnInvalidRoleTest.executeAnalyzerQuery;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.function.Function;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.SystemWideProperties;
import mondrian.test.loader.CsvDBTestCase;

/**
 * @author Andrey Khayrutdinov
 */
class AggregationOnInvalidRoleWhenNotIgnoringTest extends CsvDBTestCase {

    @Override
    protected String getFileName() {
        return "mondrian_2225.csv";
    }

    @BeforeEach
    public void beforeEach() {
    }

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }


    protected void prepareContext(Context context) {
        super.prepareContext(context);
        //TestUtil.withRole(context,  "Test");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void test_ThrowsException_WhenNonIgnoringInvalidMembers(Context context) {
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        prepareContext(context);
        try {
            executeAnalyzerQuery(((TestContext)context).getConnection(List.of("Test")));
        } catch (Exception e) {
            // that's ok, junit's assertion errors are derived from Error,
            // hence they will not be caught here
            return;
        }
        fail("Schema should not load when restriction is invalid");
    }

    protected Function<CatalogMapping, PojoMappingModifier> getModifierFunction(){
        return AggregationOnInvalidRoleTestModifier::new;
    }

}
