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

import static mondrian.rolap.RolapNativeTopCountTestCases.NON_EMPTY_IS_NOT_IGNORED_WHEN_TWO_PARAMS_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.RESULTS_ARE_SHOWN_NOT_MORE_THAN_EXIST_2_PARAMS_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_CITIES_QUERY;
import static mondrian.rolap.RolapNativeTopCountTestCases.TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_STATES_QUERY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencube.junit5.TestUtil.flushSchemaCache;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.result.Result;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

/**
 * According to
 * <a href="https://msdn.microsoft.com/en-us/library/ms144792.aspx">MSDN
 * page</a>, when {@code TOPCOUNT} function is called with two parameters
 * it should mimic the behaviour of {@code HEAD} function.
 * <p/>
 * The idea of these tests is to compare results of both function being
 * called with same parameters - they should be equal.
 * <p/>
 * Queries with {@code HEAD} are made by mere substitution of the name
 * instead of {@code TOPCOUNT}.
 *
 * @author Andrey Khayrutdinov
 */
class TopCountWithTwoParamsVersusHeadTest extends BatchTestCase {

    private void assertResultsAreEqual(
        Connection connection,
        String testCase,
            String topCountQuery)
    {
        if (!topCountQuery.contains("TOPCOUNT")) {
            throw new IllegalArgumentException(
                "'TOPCOUNT' was not found. Please ensure you are using upper case:\n\t\t"
                    + topCountQuery);
        }

        String headQuery = topCountQuery.replace("TOPCOUNT", "HEAD");

        Result topCountResult = executeQuery(topCountQuery, connection);
        flushSchemaCache(connection);
        Result headResult = executeQuery(headQuery, connection);
        assertEquals(
                TestUtil.toString(topCountResult),
                TestUtil.toString(headResult),
                String.format(
                        "[%s]: TOPCOUNT() and HEAD() results of the query differ. The query:\n\t\t%s",
                        testCase,
                        topCountQuery));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void test_States(Context<?> context) throws Exception {
        assertResultsAreEqual(context.getConnectionWithDefaultRole(),
            "States",
            TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_STATES_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void test_Cities(Context<?> context) throws Exception {
        assertResultsAreEqual(context.getConnectionWithDefaultRole(),
            "Cities",
            TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_CITIES_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void test_ShowsNotMoreThanExist(Context<?> context) {
        assertResultsAreEqual(context.getConnectionWithDefaultRole(),
            "Not more than exists",
            RESULTS_ARE_SHOWN_NOT_MORE_THAN_EXIST_2_PARAMS_QUERY);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void test_DoesNotIgnoreNonEmpty(Context<?> context) {
        assertResultsAreEqual(context.getConnectionWithDefaultRole(),
            "Does not ignore NON EMPTY",
            NON_EMPTY_IS_NOT_IGNORED_WHEN_TWO_PARAMS_QUERY);
    }
}
