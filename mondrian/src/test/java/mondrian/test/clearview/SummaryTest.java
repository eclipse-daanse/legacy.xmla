/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/
package mondrian.test.clearview;

import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.test.DiffRepository;
import mondrian.util.Bug;

/**
 * <code>SummaryTest</code> is a test suite which tests scenarios of
 * summing unit sales against the FoodMart database.
 * MDX queries and their expected results are maintained separately in
 * SummaryTest.ref.xml file.If you would prefer to see them as inlined
 * Java string literals, run ant target "generateDiffRepositoryJUnit" and
 * then use file SummaryTestJUnit.java which will be generated in
 * this directory.
 *
 * @author Khanh Vu
 */
public class SummaryTest extends ClearViewBase {

    @Override
	public DiffRepository getDiffRepos() {
        return getDiffReposStatic();
    }

    private static DiffRepository getDiffReposStatic() {
        return DiffRepository.lookup(SummaryTest.class);
    }


    @Override
	@ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    protected void runTest(Context context) {
        DiffRepository diffRepos = getDiffRepos();
        for (String name : diffRepos.getTestCaseNames()) {
            setName(name);
            diffRepos.setCurrentTestCaseName(name);

            if (!Bug.BugMondrian785Fixed
                    && (getName().equals("testRankExpandNonNative")
                    || getName().equals("testCountExpandNonNative")
                    || getName().equals("testCountOverTimeExpandNonNative"))
                    && context.getConfigValue(ConfigConstants.ENABLE_NATIVE_CROSS_JOIN, ConfigConstants.ENABLE_NATIVE_CROSS_JOIN_DEFAULT_VALUE, Boolean.class)) {
                // Tests give wrong results if native crossjoin is disabled.
                return;
            }
            if (!Bug.BugMondrian2452Fixed
                    && (getName().equals("testRankExpandNonNative"))
                    && !context.getConfigValue(ConfigConstants.ENABLE_NATIVE_CROSS_JOIN, ConfigConstants.ENABLE_NATIVE_CROSS_JOIN_DEFAULT_VALUE, Boolean.class)) {
                // Tests give wrong results if native crossjoin is disabled.
                return;
            }
            super.runTest(context);
        }
    }
}
