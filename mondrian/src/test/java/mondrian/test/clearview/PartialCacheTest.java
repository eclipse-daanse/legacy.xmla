/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.test.clearview;

import mondrian.test.DiffRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.Context;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalogAsFile;

/**
 * <code>PartialCacheTest</code> is a test suite which tests
 * complex queries against the FoodMart database. MDX queries and their
 * expected results are maintained separately in PartialCacheTest.ref.xml file.
 * If you would prefer to see them as inlined Java string literals, run
 * ant target "generateDiffRepositoryJUnit" and then use
 * file PartialCacheTestJUnit.java which will be generated in this directory.
 *
 * @author Khanh Vu
 */
public class PartialCacheTest extends ClearViewBase {

    public DiffRepository getDiffRepos() {
        return getDiffReposStatic();
    }

    private static DiffRepository getDiffReposStatic() {
        return DiffRepository.lookup(PartialCacheTest.class);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    protected void runTest(Context context) {
        DiffRepository diffRepos = getDiffRepos();
        for (String name : diffRepos.getTestCaseNames()) {
            setName(name);
            diffRepos.setCurrentTestCaseName(name);
            super.runTest(context);
        }
    }
}

// End PartialCacheTest.java
