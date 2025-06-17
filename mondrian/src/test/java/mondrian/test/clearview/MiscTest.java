/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.test.clearview;

import java.util.Optional;
import java.util.function.Function;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.test.DiffRepository;

/**
 * <code>MiscTest</code> is a test suite which tests miscellaneous
 * complex queries against the FoodMart database. MDX queries and their
 * expected results are maintained separately in MiscTest.ref.xml file.
 * If you would prefer to see them as inlined Java string literals, run
 * ant target "generateDiffRepositoryJUnit" and then use
 * file MiscTestJUnit.java which will be generated in this directory.
 *
 * @author Khanh Vu
 */
public class MiscTest extends ClearViewBase {

    @Override
	public DiffRepository getDiffRepos() {
        return getDiffReposStatic();
    }

    private static DiffRepository getDiffReposStatic() {
        return DiffRepository.lookup(MiscTest.class);
    }

    @Override
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

    @Override
    protected Optional<Function<CatalogMapping, PojoMappingModifier>> getModifier(String currentTestCaseName) {
        if (currentTestCaseName.equals("testSolveOrder")) {
            return Optional.of(MiscTestModifier::new);
        }
        return Optional.empty();
    }

}
