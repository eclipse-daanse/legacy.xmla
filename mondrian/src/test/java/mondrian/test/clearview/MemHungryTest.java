/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.test.clearview;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestingContext;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalogAsFile;

import mondrian.test.DiffRepository;

/**
 * <code>MemHungryTest</code> is a test suite which tests
 * complex queries against the FoodMart database. MDX queries and their
 * expected results are maintained separately in MemHungryTest.ref.xml file.
 * If you would prefer to see them as inlined Java string literals, run
 * ant target "generateDiffRepositoryJUnit" and then use
 * file MemHungryTestJUnit.java which will be generated in this directory.
 *
 * @author Khanh Vu
 */
@Disabled //disabled for CI build
public class MemHungryTest extends ClearViewBase {

    public DiffRepository getDiffRepos() {
        return getDiffReposStatic();
    }

    private static DiffRepository getDiffReposStatic() {
        return DiffRepository.lookup(MemHungryTest.class);
    }

    @Disabled //disabled for CI build
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    protected void runTest(TestingContext context) {
        DiffRepository diffRepos = getDiffRepos();
        for (String name : diffRepos.getTestCaseNames()) {
            setName(name);
            diffRepos.setCurrentTestCaseName(name);
            super.runTest(context);
        }
    }
}
// End MemHungryTest.java
