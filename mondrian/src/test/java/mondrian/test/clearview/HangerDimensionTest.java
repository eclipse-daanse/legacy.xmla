/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.test.clearview;

import static org.mockito.Mockito.withSettings;

import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.Context;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalogAsFile;

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
public class HangerDimensionTest extends ClearViewBase {

    public DiffRepository getDiffRepos() {
        return getDiffReposStatic();
    }

    private static DiffRepository getDiffReposStatic() {
        return DiffRepository.lookup(HangerDimensionTest.class);
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

// End HangerDimensionTest.java
