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
import static mondrian.rolap.RolapNativeTopCountTestCases.CUSTOM_COUNT_MEASURE_RESULT;
import static mondrian.rolap.RolapNativeTopCountTestCases.EMPTY_CELLS_ARE_HIDDEN_WHEN_NON_EMPTY_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.EMPTY_CELLS_ARE_HIDDEN_WHEN_NON_EMPTY_RESULT;
import static mondrian.rolap.RolapNativeTopCountTestCases.EMPTY_CELLS_ARE_SHOWN_COUNTRIES_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.EMPTY_CELLS_ARE_SHOWN_COUNTRIES_RESULT;
import static mondrian.rolap.RolapNativeTopCountTestCases.EMPTY_CELLS_ARE_SHOWN_NOT_MORE_THAN_EXIST_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.EMPTY_CELLS_ARE_SHOWN_NOT_MORE_THAN_EXIST_RESULT;
import static mondrian.rolap.RolapNativeTopCountTestCases.EMPTY_CELLS_ARE_SHOWN_STATES_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.EMPTY_CELLS_ARE_SHOWN_STATES_RESULT;
import static mondrian.rolap.RolapNativeTopCountTestCases.IMPLICIT_COUNT_MEASURE_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.IMPLICIT_COUNT_MEASURE_RESULT;
import static mondrian.rolap.RolapNativeTopCountTestCases.NON_EMPTY_IS_NOT_IGNORED_WHEN_TWO_PARAMS_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.NON_EMPTY_IS_NOT_IGNORED_WHEN_TWO_PARAMS_RESULT;
import static mondrian.rolap.RolapNativeTopCountTestCases.RESULTS_ARE_SHOWN_NOT_MORE_THAN_EXIST_2_PARAMS_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.RESULTS_ARE_SHOWN_NOT_MORE_THAN_EXIST_2_PARAMS_RESULT;
import static mondrian.rolap.RolapNativeTopCountTestCases.ROLE_RESTRICTION_WORKS_DF_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.ROLE_RESTRICTION_WORKS_DF_RESULT;
import static mondrian.rolap.RolapNativeTopCountTestCases.ROLE_RESTRICTION_WORKS_DF_ROLE_NAME;
import static mondrian.rolap.RolapNativeTopCountTestCases.ROLE_RESTRICTION_WORKS_WA_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.ROLE_RESTRICTION_WORKS_WA_RESULT;
import static mondrian.rolap.RolapNativeTopCountTestCases.ROLE_RESTRICTION_WORKS_WA_ROLE_NAME;
import static mondrian.rolap.RolapNativeTopCountTestCases.SUM_MEASURE_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.SUM_MEASURE_RESULT;
import static mondrian.rolap.RolapNativeTopCountTestCases.TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_CITIES_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_CITIES_RESULT;
import static mondrian.rolap.RolapNativeTopCountTestCases.TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_STATES_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_STATES_RESULT;
import static org.opencube.junit5.TestUtil.assertQueryReturns;

import java.net.URL;
import java.util.Map;

import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.FoodmartDatabaseSupplier;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.FoodmartTestInstance;
import org.eclipse.daanse.rolap.testkit.junit.api.RolapContextTest;
import org.eclipse.daanse.rolap.testkit.junit.api.Roles;
import org.junit.jupiter.api.Test;

/**
 * @author Andrey Khayrutdinov
 * @see RolapNativeTopCountTestCases
 */
@RolapContextTest(FoodmartTestInstance.class)
class RolapNativeTopCountTest extends BatchTestCase {

    @Test
    void testTopCount_ImplicitCountMeasure(Connection connection) throws Exception {
        assertQueryReturns(connection,
            IMPLICIT_COUNT_MEASURE_QUERY, IMPLICIT_COUNT_MEASURE_RESULT);
    }

    @Test
    @RolapContextTest(catalog = { CatalogSupplier.class, SchemaModifiersEmf.CustomCountMeasureCubeName.class },
            database = FoodmartDatabaseSupplier.class, data = FoodmartData.class)
    void testTopCount_CountMeasure(Connection connection) throws Exception {
        assertQueryReturns(connection,
            CUSTOM_COUNT_MEASURE_QUERY, CUSTOM_COUNT_MEASURE_RESULT);
    }

    @Test
    void testTopCount_SumMeasure(Connection connection) throws Exception {
        assertQueryReturns(connection, SUM_MEASURE_QUERY, SUM_MEASURE_RESULT);
    }

    @Test
    void testEmptyCellsAreShown_Countries(Connection connection) {
        assertQueryReturns(connection,
            EMPTY_CELLS_ARE_SHOWN_COUNTRIES_QUERY,
            EMPTY_CELLS_ARE_SHOWN_COUNTRIES_RESULT);
    }

    @Test
    void testEmptyCellsAreShown_States(Connection connection) {
        assertQueryReturns(connection,
            EMPTY_CELLS_ARE_SHOWN_STATES_QUERY,
            EMPTY_CELLS_ARE_SHOWN_STATES_RESULT);
    }

    @Test
    void testEmptyCellsAreShown_ButNoMoreThanReallyExist(Connection connection) {
        assertQueryReturns(connection,
            EMPTY_CELLS_ARE_SHOWN_NOT_MORE_THAN_EXIST_QUERY,
            EMPTY_CELLS_ARE_SHOWN_NOT_MORE_THAN_EXIST_RESULT);
    }

    @Test
    void testEmptyCellsAreHidden_WhenNonEmptyIsDeclaredExplicitly(Connection connection) {
        assertQueryReturns(connection,
            EMPTY_CELLS_ARE_HIDDEN_WHEN_NON_EMPTY_QUERY,
            EMPTY_CELLS_ARE_HIDDEN_WHEN_NON_EMPTY_RESULT);
    }


    @Test
    @RolapContextTest(catalog = { CatalogSupplier.class, SchemaModifiersEmf.RoleRestrictionWorksWaRoleDef.class },
            database = FoodmartDatabaseSupplier.class, data = FoodmartData.class)
    void testRoleRestrictionWorks_ForRowWithData(
        @Roles(ROLE_RESTRICTION_WORKS_WA_ROLE_NAME) Connection connection) throws Exception
    {
        assertQueryReturns(connection,
            ROLE_RESTRICTION_WORKS_WA_QUERY,
            ROLE_RESTRICTION_WORKS_WA_RESULT);
    }

    @Test
    @RolapContextTest(catalog = { CatalogSupplier.class, SchemaModifiersEmf.RoleRestrictionWorksDfRoleDef.class },
            database = FoodmartDatabaseSupplier.class, data = FoodmartData.class)
    void testRoleRestrictionWorks_ForRowWithOutData(
        @Roles(ROLE_RESTRICTION_WORKS_DF_ROLE_NAME) Connection connection) throws Exception
    {
        assertQueryReturns(connection,
            ROLE_RESTRICTION_WORKS_DF_QUERY,
            ROLE_RESTRICTION_WORKS_DF_RESULT);
    }

    @Test
    void testMimicsHeadWhenTwoParams_States(Connection connection) {
        assertQueryReturns(connection,
            TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_STATES_QUERY,
            TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_STATES_RESULT);
    }

    @Test
    void testMimicsHeadWhenTwoParams_Cities(Connection connection) {
        assertQueryReturns(connection,
            TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_CITIES_QUERY,
            TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_CITIES_RESULT);
    }

    @Test
    void testMimicsHeadWhenTwoParams_ShowsNotMoreThanExist(Connection connection) {
        assertQueryReturns(connection,
            RESULTS_ARE_SHOWN_NOT_MORE_THAN_EXIST_2_PARAMS_QUERY,
            RESULTS_ARE_SHOWN_NOT_MORE_THAN_EXIST_2_PARAMS_RESULT);
    }

    @Test
    void testMimicsHeadWhenTwoParams_DoesNotIgnoreNonEmpty(Connection connection) {
        assertQueryReturns(connection,
            NON_EMPTY_IS_NOT_IGNORED_WHEN_TWO_PARAMS_QUERY,
            NON_EMPTY_IS_NOT_IGNORED_WHEN_TWO_PARAMS_RESULT);
    }

    /** Named bridge onto the FoodMart CSVs (for the {@code data =} supplier form). */
    public static class FoodmartData implements org.eclipse.daanse.cwm.testkit.api.DataSupplier {
        @Override
        public Map<String, URL> csvResources() {
            return new FoodmartTestInstance().dataSupplier().csvResources();
        }
    }
}
