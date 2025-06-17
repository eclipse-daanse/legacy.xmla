/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.test.clearview;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.test.DiffRepository;

/**
 * <code>PartialCacheVCTest</code> is a test suite which tests complex queries
 * against the FoodMart database. MDX queries and their expected results are
 * maintained separately in PartialCacheVCTest.ref.xml file.  If you would
 * prefer to see them as inlined Java string literals, run ant target
 * "generateDiffRepositoryJUnit" and then use file PartialCacheVCTestJUnit.java
 * which will be generated in this directory.
 *
 * @author Khanh Vu
 */
public class PartialCacheVCTest extends ClearViewBase {

    @Override
	public DiffRepository getDiffRepos() {
        return getDiffReposStatic();
    }

    private static DiffRepository getDiffReposStatic() {
        return DiffRepository.lookup(PartialCacheVCTest.class);
    }


    @Override
	@Tag("cacheHitSuit1Test")
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    protected void runTest(Context<?> context) {
        DiffRepository diffRepos = getDiffRepos();
        for (String name : diffRepos.getTestCaseNames()) {
            setName(name);
            diffRepos.setCurrentTestCaseName(name);
            super.runTest(context);
        }
    }
}
