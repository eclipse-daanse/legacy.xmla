/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.test.clearview;

import org.eclipse.daanse.olap.common.ConfigConstants;
import org.eclipse.daanse.rolap.testkit.junit.api.RolapConfig;
import org.junit.jupiter.api.Tag;

import mondrian.test.DiffRepository;

/**
 * <code>MultiDimVCTest</code> is a test suite which tests
 * complex queries against the FoodMart database. MDX queries and their
 * expected results are maintained separately in MultiDimVCTest.ref.xml file.
 * If you would prefer to see them as inlined Java string literals, run
 * ant target "generateDiffRepositoryJUnit" and then use
 * file MultiDimVCTestJUnit.java which will be generated in this directory.
 *
 * @author Khanh Vu
 */
@RolapConfig(key = ConfigConstants.EXPAND_NON_NATIVE, value = "true", type = Boolean.class)
@Tag("cacheHitSuit1Test")
public class MultiDimVCTest extends ClearViewBase {

    @Override
	public DiffRepository getDiffRepos() {
        return getDiffReposStatic();
    }

    private static DiffRepository getDiffReposStatic() {
        return DiffRepository.lookup(MultiDimVCTest.class);
    }
}
