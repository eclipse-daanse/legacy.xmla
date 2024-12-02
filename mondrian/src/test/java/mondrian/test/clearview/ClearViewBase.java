/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2003-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara
// All Rights Reserved.
//
// jhyde, Feb 14, 2003
*/

package mondrian.test.clearview;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.opencube.junit5.TestUtil.getDialect;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.TestConfig;

import mondrian.enums.DatabaseProduct;
import mondrian.olap.SystemWideProperties;
import mondrian.olap.Util;
import mondrian.rolap.BatchTestCase;
import mondrian.test.DiffRepository;
import mondrian.test.SqlPattern;

/**
 * <code>ClearViewBase</code> is the base class to build test cases which test
 * queries against the FoodMart database. A concrete sub class and
 * a ref.xml file will be needed for each test suites to be added. MDX queries
 * and their expected results are maintained separately in *.ref.xml files.
 * If you would prefer to see them as inlined Java string literals, run
 * ant target "generateDiffRepositoryJUnit" and then use
 * files *JUnit.java which will be generated in this directory.
 *
 * @author John Sichi
 * @author Richard Emberson
 * @author Khanh Vu
 *
 * @since Jan 25, 2007
 */
 public abstract class ClearViewBase extends BatchTestCase {

    public abstract DiffRepository getDiffRepos();

    private String name;

    // implement TestCase
    @BeforeEach
    protected void beforeEach() throws Exception {
    }


    // implement TestCase
    @AfterEach
    protected void afterEach() {
        SystemWideProperties.instance().populateInitial();
        DiffRepository diffRepos = getDiffRepos();
        diffRepos.setCurrentTestCaseName(null);
    }


    // implement TestCase
    protected void runTest(Context context) {
            DiffRepository diffRepos = getDiffRepos();
            // add calculated member to a cube if specified in the xml file
            /*
            String cubeName = diffRepos.expand(null, "${modifiedCubeName}").trim();
            if (!(cubeName.equals("")
                    || cubeName.equals("${modifiedCubeName}"))) {
                String customDimensions = diffRepos.expand(
                        null, "${customDimensions}");
                customDimensions =
                        (!(customDimensions.equals("")
                                || customDimensions.equals("${customDimensions}")))
                                ? customDimensions : null;
                String measures = diffRepos.expand(
                        null, "${measures}");
                measures =
                        (!(measures.equals("")
                                || measures.equals("${measures}")))
                                ? measures : null;
                String calculatedMembers = diffRepos.expand(
                        null, "${calculatedMembers}");
                calculatedMembers =
                        (!(calculatedMembers.equals("")
                                || calculatedMembers.equals("${calculatedMembers}")))
                                ? calculatedMembers : null;
                String namedSets = diffRepos.expand(
                        null, "${namedSets}");
                namedSets =
                        (!(namedSets.equals("")
                                || namedSets.equals("${namedSets}")))
                                ? namedSets : null;
                ((BaseTestContext) context).update(SchemaUpdater.createSubstitutingCube(
                        cubeName, customDimensions, measures, calculatedMembers,
                        namedSets, false));

            }
             */
             updateSchemaIfNeed(context, diffRepos.getCurrentTestCaseName(true));

            // Set some properties to match the way we configure them
            // for ClearView.
            ((TestConfig)context.getConfig()).setExpandNonNative(true);

            String mdx = diffRepos.expand(null, "${mdx}");
            String result = Util.NL + TestUtil.toString(
                    executeQuery(mdx, context.getConnection()));
            diffRepos.assertEquals("result", "${result}", result);
    }

    protected void assertQuerySql(Connection connection, boolean flushCache)
    {
        DiffRepository diffRepos = getDiffRepos();

        if (buildSqlPatternArray(connection) == null) {
            return;
        }

        super.assertQuerySqlOrNot(
            connection,
            diffRepos.expand(null, "${mdx}"),
            buildSqlPatternArray(connection),
            false,
            false,
            flushCache);
    }

    protected void assertNoQuerySql(Connection connection, boolean flushCache)
        throws Exception
    {
        DiffRepository diffRepos = getDiffRepos();

        if (buildSqlPatternArray(connection) == null) {
            return;
        }

        assertQuerySqlOrNot(
            connection,
            diffRepos.expand(null, "${mdx}"),
            buildSqlPatternArray(connection),
            true,
            false,
            flushCache);
    }

    private SqlPattern[] buildSqlPatternArray(Connection connection) {
        DiffRepository diffRepos = getDiffRepos();
        Dialect d = getDialect(connection);
        DatabaseProduct dialect = getDatabaseProduct(d.getDialectName());
        String testCaseName = getName();
        String sql = diffRepos.get(
            testCaseName, "expectedSql", dialect.name());
        if (sql != null) {
            sql = sql.replaceAll("[ \t\n\f\r]+", " ").trim();
            return new SqlPattern[]{
                new SqlPattern(dialect, sql, null)
            };
        }
        return null;
    }

    public void setName(String name) {
        this.name = name;
    }

    protected String getName() {
        return this.name;
    }
}
