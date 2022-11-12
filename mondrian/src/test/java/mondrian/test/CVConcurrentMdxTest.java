/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
//
*/

package mondrian.test;

import mondrian.olap.MondrianProperties;
import mondrian.olap.Util;
import mondrian.test.clearview.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import java.lang.reflect.Constructor;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A copy of {@link ConcurrentMdxTest} with modifications to take
 * as input ref.xml files. This does not fully use {@link DiffRepository}
 * and does not generate log files.
 * This Class is not added to the Main test suite.
 * Purpose of this test is to simulate Concurrent access to Aggregation and data
 * load. Simulation will be more effective if we run this single test again and
 * again with a fresh connection.
 *
 * @author Khanh Vu
 */
public class CVConcurrentMdxTest {
    private MondrianProperties props;

    private PropertySaver5 propSaver;
    private SummaryGeneratingListener listener;

    @BeforeEach
    protected void beforeEach() throws Exception {
        listener = new SummaryGeneratingListener();
        props = MondrianProperties.instance();
        propSaver = new PropertySaver5();
    }

    @AfterEach
    protected void afterEach() throws Exception {
        propSaver.reset();
    }


    @Test
    public void testConcurrentQueriesInRandomOrder() {
        propSaver.set(props.UseAggregates, false);
        propSaver.set(props.ReadAggregates, false);
        propSaver.set(props.DisableCaching, false);

        // test partially filled aggregation cache
        // add test classes
        List<Class> testList = new ArrayList<Class>();
        //List<TestSuite> suiteList = new ArrayList<TestSuite>();

        testList.add(PartialCacheTest.class);
        //suiteList.add(PartialCacheTest.suite());
        testList.add(MultiLevelTest.class);
        //suiteList.add(MultiLevelTest.suite());
        testList.add(QueryAllTest.class);
        //suiteList.add(QueryAllTest.suite());
        testList.add(MultiDimTest.class);
        //suiteList.add(MultiDimTest.suite());

        // sanity check
        //TODO run tests
        //assertTrue(sanityCheck(testList));

        // generate list of queries and results
        QueryAndResult[] queryList = generateQueryArray(testList);

        assertTrue(ConcurrentValidatingQueryRunner.runTest(
            3, 100, true, true, true, queryList).size() == 0);
    }

    @Test
    public void testConcurrentQueriesInRandomOrderOnVirtualCube() {
        propSaver.set(props.UseAggregates, false);
        propSaver.set(props.ReadAggregates, false);
        propSaver.set(props.DisableCaching, false);

        // test partially filled aggregation cache
        // add test classes
        List<Class> testList = new ArrayList<Class>();
        //List<TestSuite> suiteList = new ArrayList<TestSuite>();

        testList.add(PartialCacheVCTest.class);
        //suiteList.add(PartialCacheVCTest.suite());
        testList.add(MultiLevelTest.class);
        //suiteList.add(MultiLevelTest.suite());
        testList.add(QueryAllVCTest.class);
        //suiteList.add(QueryAllVCTest.suite());
        testList.add(MultiDimVCTest.class);
        //suiteList.add(MultiDimVCTest.suite());

        // sanity check
        //TODO run tests
        //assertTrue(sanityCheck(testList));

        // generate list of queries and results
        QueryAndResult[] queryList = generateQueryArray(testList);

        assertTrue(ConcurrentValidatingQueryRunner.runTest(
            3, 100, true, true, true, queryList).size() == 0);
    }

    @Test
    public void testConcurrentCVQueriesInRandomOrder() {
        propSaver.set(props.UseAggregates, false);
        propSaver.set(props.ReadAggregates, false);
        propSaver.set(props.DisableCaching, false);

        // test partially filled aggregation cache
        // add test classes
        List<Class> testList = new ArrayList<>();

        testList.add(CVBasicTest.class);
        testList.add(GrandTotalTest.class);
        testList.add(MetricFilterTest.class);
        testList.add(MiscTest.class);
        testList.add(PredicateFilterTest.class);
        testList.add(SubTotalTest.class);
        testList.add(SummaryMetricPercentTest.class);
        testList.add(SummaryTest.class);
        testList.add(TopBottomTest.class);

        // generate list of queries and results
        QueryAndResult[] queryList = generateQueryArray(testList);

        assertEquals(
            Collections.<Throwable>emptyList(),
            ConcurrentValidatingQueryRunner.runTest(
                3, 100, true, true, true, queryList));
    }


    /**
     * Runs one pass of all tests single-threaded using
     * {@link ClearViewBase} mechanism
     * @param suiteList list of tests to be checked
     * @return true if all tests pass
     */
/*
    private boolean sanityCheck(List<Class> suiteList) {

        for (int i = 0; i < suiteList.size(); i++) {
            runTestCase(suiteList.get(i));
        }
        return listener.getSummary().getTestsFailedCount() == 0
                && listener.getSummary().getTestsAbortedCount() == 0
                && listener.getSummary().getContainersSkippedCount() == 0;
    }


    private void runTestCase(Class testCase)
    {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(testCase))
                .build();
        Launcher launcher = LauncherFactory.create();
        //TestPlan testPlan = launcher.discover(request);
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
    }
    // main method...
*/

    /**
     * Generates an array of QueryAndResult objects from the list of
     * test classes
     * @param testList list of test classes
     * @return array of QueryAndResult
     */
    private QueryAndResult[] generateQueryArray(List<Class> testList) {
        List<QueryAndResult> queryList = new ArrayList<QueryAndResult>();
        for (int i = 0; i < testList.size(); i++) {
            Class testClass = testList.get(i);
            try {
                Constructor cons = testClass.getConstructor();
                Object newCon = cons.newInstance();
                DiffRepository diffRepos =
                    ((ClearViewBase) newCon).getDiffRepos();

                List<String> testCaseNames = diffRepos.getTestCaseNames();
                for (int j = 0; j < testCaseNames.size(); j++) {
                    String testCaseName = testCaseNames.get(j);
                    String query = diffRepos.get(testCaseName, "mdx");
                    String result = diffRepos.get(testCaseName, "result");

                    // current limitation: only run queries if
                    // calculated members are not specified
                    if (diffRepos.get(testCaseName, "calculatedMembers")
                        == null)
                    {
                        // trim the starting newline char only
                        if (result.startsWith(Util.nl)) {
                            result = result.replaceFirst(Util.nl, "");
                        }
                        QueryAndResult queryResult =
                            new QueryAndResult(query, result);
                        queryList.add(queryResult);
                    }
                }
            } catch (Exception e) {
                throw new Error(e.getMessage());
            }
        }
        QueryAndResult[] queryArray = new QueryAndResult[queryList.size()];
        return queryList.toArray(queryArray);
    }
}

// End CVConcurrentMdxTest.java
