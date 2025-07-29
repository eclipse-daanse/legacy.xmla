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


import static mondrian.rolap.RolapNativeTopCountTestCases.CUSTOM_COUNT_MEASURE_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.EMPTY_CELLS_ARE_HIDDEN_WHEN_NON_EMPTY_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.EMPTY_CELLS_ARE_SHOWN_COUNTRIES_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.EMPTY_CELLS_ARE_SHOWN_NOT_MORE_THAN_EXIST_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.EMPTY_CELLS_ARE_SHOWN_STATES_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.IMPLICIT_COUNT_MEASURE_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.NON_EMPTY_IS_NOT_IGNORED_WHEN_TWO_PARAMS_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.RESULTS_ARE_SHOWN_NOT_MORE_THAN_EXIST_2_PARAMS_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.ROLE_RESTRICTION_WORKS_DF_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.ROLE_RESTRICTION_WORKS_DF_ROLE_NAME;
import static mondrian.rolap.RolapNativeTopCountTestCases.ROLE_RESTRICTION_WORKS_WA_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.ROLE_RESTRICTION_WORKS_WA_ROLE_NAME;
import static mondrian.rolap.RolapNativeTopCountTestCases.SUM_MEASURE_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_CITIES_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_STATES_QUERY;
import static org.opencube.junit5.TestUtil.verifySameNativeAndNot;
import static org.opencube.junit5.TestUtil.withSchema;

import java.util.List;

import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

/**
 * @author Andrey Khayrutdinov
 */
class RolapNativeTopCountVersusNonNativeTest extends BatchTestCase {



    @BeforeEach
    public void beforeEach() {

    }

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
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
        verifySameNativeAndNot(connection, query, message);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTopCount_ImplicitCountMeasure(Context<?> context) throws Exception {
        assertResultsAreEqual(context.getConnectionWithDefaultRole(),
            "Implicit Count Measure", IMPLICIT_COUNT_MEASURE_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTopCount_SumMeasure(Context<?> context) throws Exception {
        assertResultsAreEqual(context.getConnectionWithDefaultRole(),
            "Sum Measure", SUM_MEASURE_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testTopCount_CountMeasure(Context<?> context) throws Exception {

       /*
       String baseSchema = TestUtil.getRawSchema(context);
       String schema = SchemaUtil.getSchema(baseSchema,null, CUSTOM_COUNT_MEASURE_CUBE, null, null, null, null);
       withSchema(context, schema);
       //withCube(CUSTOM_COUNT_MEASURE_CUBE_NAME);
       */
        withSchema(context, SchemaModifiers.CustomCountMeasureCubeName::new);
        assertResultsAreEqual(context.getConnectionWithDefaultRole(),
            "Custom Count Measure", CUSTOM_COUNT_MEASURE_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testEmptyCellsAreShown_Countries(Context<?> context) throws Exception {
        assertResultsAreEqual(context.getConnectionWithDefaultRole(),
            "Empty Cells Are Shown - Countries",
            EMPTY_CELLS_ARE_SHOWN_COUNTRIES_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testEmptyCellsAreShown_States(Context<?> context) throws Exception {
        assertResultsAreEqual(context.getConnectionWithDefaultRole(),
            "Empty Cells Are Shown - States",
            EMPTY_CELLS_ARE_SHOWN_STATES_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testEmptyCellsAreShown_ButNoMoreThanReallyExist(Context<?> context) {
        assertResultsAreEqual(context.getConnectionWithDefaultRole(),
            "Empty Cells Are Shown - But no more than really exist",
            EMPTY_CELLS_ARE_SHOWN_NOT_MORE_THAN_EXIST_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testEmptyCellsAreHidden_WhenNonEmptyIsDeclaredExplicitly(Context<?> context) {
        assertResultsAreEqual(context.getConnectionWithDefaultRole(),
            "Empty Cells Are Hidden - When NON EMPTY is declared explicitly",
            EMPTY_CELLS_ARE_HIDDEN_WHEN_NON_EMPTY_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRoleRestrictionWorks_ForRowWithData(Context<?> context) {
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
                null, null, null, null, null,
                ROLE_RESTRICTION_WORKS_WA_ROLE_DEF);
        withSchema(context, schema);
         */
        withSchema(context, SchemaModifiers.RoleRestrictionWorksWaRoleDef::new);
        assertResultsAreEqual(((TestContext)context).getConnection(List.of(ROLE_RESTRICTION_WORKS_WA_ROLE_NAME)),
            "Role restriction works - For WA state",
            ROLE_RESTRICTION_WORKS_WA_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testRoleRestrictionWorks_ForRowWithOutData(Context<?> context) {
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
                null, null, null, null, null,
                ROLE_RESTRICTION_WORKS_DF_ROLE_DEF);
        withSchema(context, schema);
         */
        withSchema(context, SchemaModifiers.RoleRestrictionWorksDfRoleDef::new);
        assertResultsAreEqual(((TestContext)context).getConnection(List.of(ROLE_RESTRICTION_WORKS_DF_ROLE_NAME)),
            "Role restriction works - For DF state",
            ROLE_RESTRICTION_WORKS_DF_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMimicsHeadWhenTwoParams_States(Context<?> context) {
        assertResultsAreEqual(context.getConnectionWithDefaultRole(),
            "Two Parameters - States",
            TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_STATES_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMimicsHeadWhenTwoParams_Cities(Context<?> context) {
        assertResultsAreEqual(context.getConnectionWithDefaultRole(),
            "Two Parameters - Cities",
            TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_CITIES_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMimicsHeadWhenTwoParams_ShowsNotMoreThanExist(Context<?> context) {
        assertResultsAreEqual(context.getConnectionWithDefaultRole(),
            "Two Parameters - Shows not more than really exist",
            RESULTS_ARE_SHOWN_NOT_MORE_THAN_EXIST_2_PARAMS_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMimicsHeadWhenTwoParams_DoesNotIgnoreNonEmpty(Context<?> context) {
        assertResultsAreEqual(context.getConnectionWithDefaultRole(),
            "Two Parameters - Does not ignore NON EMPTY",
            NON_EMPTY_IS_NOT_IGNORED_WHEN_TWO_PARAMS_QUERY);
    }
}
