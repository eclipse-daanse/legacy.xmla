/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2015-2017 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.rolap;

import mondrian.olap.Connection;
import mondrian.test.PropertySaver5;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.SchemaUtil;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.Context;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalogAsFile;

import static mondrian.rolap.RolapNativeTopCountTestCases.*;
import static org.opencube.junit5.TestUtil.verifySameNativeAndNot;
import static org.opencube.junit5.TestUtil.withRole;
import static org.opencube.junit5.TestUtil.withSchema;

/**
 * @author Andrey Khayrutdinov
 */
public class RolapNativeTopCountVersusNonNativeTest extends BatchTestCase {

    private PropertySaver5 propSaver;

    @BeforeEach
    public void beforeEach() {
        propSaver = new PropertySaver5();
    }

    @AfterEach
    public void afterEach() {
        propSaver.reset();
    }

    private void assertResultsAreEqual(
        Connection connection,
        String testCase,
        String query)
    {
        String message = String.format(
            "[%s]: native and non-native results of the query differ. The query:\n\t\t%s",
            testCase,
            query);
        verifySameNativeAndNot(connection, query, message, propSaver);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testTopCount_ImplicitCountMeasure(Context context) throws Exception {
        assertResultsAreEqual(context.createConnection(),
            "Implicit Count Measure", IMPLICIT_COUNT_MEASURE_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testTopCount_SumMeasure(Context context) throws Exception {
        assertResultsAreEqual(context.createConnection(),
            "Sum Measure", SUM_MEASURE_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testTopCount_CountMeasure(Context context) throws Exception {
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,null, CUSTOM_COUNT_MEASURE_CUBE, null, null, null, null);

       withSchema(context, schema);
       //withCube(CUSTOM_COUNT_MEASURE_CUBE_NAME);

        assertResultsAreEqual(context.createConnection(),
            "Custom Count Measure", CUSTOM_COUNT_MEASURE_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testEmptyCellsAreShown_Countries(Context context) throws Exception {
        assertResultsAreEqual(context.createConnection(),
            "Empty Cells Are Shown - Countries",
            EMPTY_CELLS_ARE_SHOWN_COUNTRIES_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testEmptyCellsAreShown_States(Context context) throws Exception {
        assertResultsAreEqual(context.createConnection(),
            "Empty Cells Are Shown - States",
            EMPTY_CELLS_ARE_SHOWN_STATES_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testEmptyCellsAreShown_ButNoMoreThanReallyExist(Context context) {
        assertResultsAreEqual(context.createConnection(),
            "Empty Cells Are Shown - But no more than really exist",
            EMPTY_CELLS_ARE_SHOWN_NOT_MORE_THAN_EXIST_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testEmptyCellsAreHidden_WhenNonEmptyIsDeclaredExplicitly(Context context) {
        assertResultsAreEqual(context.createConnection(),
            "Empty Cells Are Hidden - When NON EMPTY is declared explicitly",
            EMPTY_CELLS_ARE_HIDDEN_WHEN_NON_EMPTY_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testRoleRestrictionWorks_ForRowWithData(Context context) {
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
                null, null, null, null, null,
                ROLE_RESTRICTION_WORKS_WA_ROLE_DEF);
        withSchema(context, schema);
        withRole(context, ROLE_RESTRICTION_WORKS_WA_ROLE_NAME);

        assertResultsAreEqual(context.createConnection(),
            "Role restriction works - For WA state",
            ROLE_RESTRICTION_WORKS_WA_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testRoleRestrictionWorks_ForRowWithOutData(Context context) {
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
                null, null, null, null, null,
                ROLE_RESTRICTION_WORKS_DF_ROLE_DEF);
        withSchema(context, schema);
        withRole(context, ROLE_RESTRICTION_WORKS_DF_ROLE_NAME);

        assertResultsAreEqual(context.createConnection(),
            "Role restriction works - For DF state",
            ROLE_RESTRICTION_WORKS_DF_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testMimicsHeadWhenTwoParams_States(Context context) {
        assertResultsAreEqual(context.createConnection(),
            "Two Parameters - States",
            TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_STATES_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testMimicsHeadWhenTwoParams_Cities(Context context) {
        assertResultsAreEqual(context.createConnection(),
            "Two Parameters - Cities",
            TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_CITIES_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testMimicsHeadWhenTwoParams_ShowsNotMoreThanExist(Context context) {
        assertResultsAreEqual(context.createConnection(),
            "Two Parameters - Shows not more than really exist",
            RESULTS_ARE_SHOWN_NOT_MORE_THAN_EXIST_2_PARAMS_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testMimicsHeadWhenTwoParams_DoesNotIgnoreNonEmpty(Context context) {
        assertResultsAreEqual(context.createConnection(),
            "Two Parameters - Does not ignore NON EMPTY",
            NON_EMPTY_IS_NOT_IGNORED_WHEN_TWO_PARAMS_QUERY);
    }
}

// End RolapNativeTopCountVersusNonNativeTest.java