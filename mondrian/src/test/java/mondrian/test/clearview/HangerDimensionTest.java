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
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.test.DiffRepository;

/**
 * <code>HangerDimensionTest</code> tests the extended syntax of Order
 * function. See { @link
 * http://pub.eigenbase.org/wiki/MondrianOrderFunctionExtension } for
 * syntax rules.
 * MDX queries and their expected results are maintained separately in
 * HangerDimensionTest.ref.xml file.If you would prefer to see them as inlined
 * Java string literals, run ant target "generateDiffRepositoryJUnit" and
 * then use file HangerDimensionTestJUnit.java which will be generated in
 * this directory.
 *
 * @author Khanh Vu
 */
class HangerDimensionTest extends ClearViewBase {

    @Override
	public DiffRepository getDiffRepos() {
        return getDiffReposStatic();
    }

    private static DiffRepository getDiffReposStatic() {
        return DiffRepository.lookup(HangerDimensionTest.class);
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
            ((TestContext)context).setCatalogMappingSupplier(new CatalogSupplier());
        }
    }

    protected Optional<Function<Catalog, CatalogMappingSupplier>> getModifier(String currentTestCaseName) {
        return Optional.of(HangerDimensionTestModifiers.HangerDimensionTestModifier1::new);
    }

}
