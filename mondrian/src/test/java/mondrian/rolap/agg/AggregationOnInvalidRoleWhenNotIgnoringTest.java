/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2015-2017 Hitachi Vantara..  All rights reserved.
*/
package mondrian.rolap.agg;

import mondrian.test.PropertySaver5;
import mondrian.test.loader.CsvDBTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.Context;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalogAsFile;

import static mondrian.rolap.agg.AggregationOnInvalidRoleTest.CUBE;
import static mondrian.rolap.agg.AggregationOnInvalidRoleTest.ROLE;
import static mondrian.rolap.agg.AggregationOnInvalidRoleTest.executeAnalyzerQuery;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Andrey Khayrutdinov
 */
public class AggregationOnInvalidRoleWhenNotIgnoringTest extends CsvDBTestCase {

    @Override
    protected String getFileName() {
        return "mondrian_2225.csv";
    }

    private PropertySaver5 propSaver;

    @BeforeEach
    public void beforeEach() {
        propSaver = new PropertySaver5();
        propSaver.set(propSaver.properties.UseAggregates, true);
        propSaver.set(propSaver.properties.ReadAggregates, true);
        propSaver.set(propSaver.properties.IgnoreInvalidMembers, false);
    }

    @AfterEach
    public void afterEach() {
        propSaver.reset();
    }


    @Override
    protected void prepareContext(Context context) {
        super.prepareContext(context);
        TestUtil.withRole(context,  "Test");
    }

    @Override
    protected String getCubeDescription() {
        return CUBE;
    }

    @Override
    protected String getRoleDescription() {
        return ROLE;
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void test_ThrowsException_WhenNonIgnoringInvalidMembers(Context context) {
        prepareContext(context);
        try {
            executeAnalyzerQuery(context.createConnection());
        } catch (Exception e) {
            // that's ok, junit's assertion errors are derived from Error,
            // hence they will not be caught here
            return;
        }
        fail("Schema should not load when restriction is invalid");
    }
}

// End AggregationOnInvalidRole_IfNotIgnore_Test.java
