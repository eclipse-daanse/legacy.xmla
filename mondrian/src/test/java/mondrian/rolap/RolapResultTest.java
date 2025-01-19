/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.rolap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.withSchema;

import java.util.function.Function;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.TestConfig;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.rolap.aggmatcher.AggTableTestCase;

/**
 * Testcase for
 *
 *
 * @author <a>Richard M. Emberson</a>
 * @since Feb 21 2007
 */
class RolapResultTest extends AggTableTestCase {

    private static final String RolapResultTest = "RolapResultTest.csv";

    private static final String RESULTS_ALL =
        "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[D1].[a]}\n"
        + "{[D1].[b]}\n"
        + "{[D1].[c]}\n"
        + "Axis #2:\n"
        + "{[D2].[x]}\n"
        + "{[D2].[y]}\n"
        + "{[D2].[z]}\n"
        + "Row #0: 5\n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #1: \n"
        + "Row #1: 10\n"
        + "Row #1: \n"
        + "Row #2: \n"
        + "Row #2: \n"
        + "Row #2: 15\n";

    private static final String RESULTS =
        "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[D1].[a]}\n"
        + "{[D1].[b]}\n"
        + "{[D1].[c]}\n"
        + "Axis #2:\n"
        + "{[D2].[x]}\n"
        + "{[D2].[y]}\n"
        + "{[D2].[z]}\n"
        + "Row #0: 5\n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #1: \n"
        + "Row #1: 10\n"
        + "Row #1: \n"
        + "Row #2: \n"
        + "Row #2: \n"
        + "Row #2: 15\n";



    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAll(Context context) throws Exception {
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        prepareContext(context);
        if (!isApplicable(context.getConnectionWithDefaultRole())) {
            return;
        }

        String mdx =
            "select "
            + " filter({[D1].[a],[D1].[b],[D1].[c]}, "
            + "    [Measures].[Value] > 0) "
            + " ON COLUMNS, "
            + " {[D2].[x],[D2].[y],[D2].[z]} "
            + " ON ROWS "
            + "from FTAll";

        assertQueryReturns(context.getConnectionWithDefaultRole(), mdx, RESULTS_ALL);
/*
        Result result = getCubeTestContext().executeQuery(mdx);
        String resultString = TestContext.toString(result);
//System.out.println(resultString);

        assertTrue(resultString.equals(RESULTS_ALL));
*/
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testD1(Context context) throws Exception {
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        prepareContext(context);
        if (!isApplicable(context.getConnectionWithDefaultRole())) {
            return;
        }
        String mdx =
            "select "
            + " filter({[D1].[a],[D1].[b],[D1].[c]}, "
            + "    [Measures].[Value] > 0) "
            + " ON COLUMNS, "
            + " {[D2].[x],[D2].[y],[D2].[z]} "
            + " ON ROWS "
            + "from FT1";

        //getCubeTestContext().assertQueryReturns(mdx, RESULTS);
        Result result = executeQuery(mdx, context.getConnectionWithDefaultRole());
        String resultString = TestUtil.toString(result);
//System.out.println(resultString);
/*
 This is what is produced
Axis #0:
{}
Axis #1:
Axis #2:
{[D2].[x]}
{[D2].[y]}
{[D2].[z]}
*/
        assertEquals(resultString, RESULTS);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testD2(Context context) throws Exception {
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        prepareContext(context);
        if (!isApplicable(context.getConnectionWithDefaultRole())) {
            return;
        }
        String mdx =
            "select "
            + " NON EMPTY filter({[D1].[a],[D1].[b],[D1].[c]}, "
            + "    [Measures].[Value] > 0) "
            + " ON COLUMNS, "
            + " {[D2].[x],[D2].[y],[D2].[z]} "
            + " ON ROWS "
            + "from FT2";

        assertQueryReturns(context.getConnectionWithDefaultRole(), mdx, RESULTS);
/*
        Result result = getCubeTestContext().executeQuery(mdx);
        String resultString = TestContext.toString(result);
//System.out.println(resultString);
        assertTrue(resultString.equals(RESULTS));
*/
    }

    /**
     * This ought to give the same result as the above testD2() method.
     * In this case, the FT2Extra cube has a default measure with no
     * data (null) for all members. This default measure is used
     * in the evaluation even though there is an implicit use of the
     * measure [Measures].[Value].
     *
     * @throws Exception
     */
    @Disabled //disabled for CI build
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    public void _testNullDefaultMeasure(Context context) throws Exception {
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        prepareContext(context);
        if (!isApplicable(context.getConnectionWithDefaultRole())) {
            return;
        }

        String mdx =
            "select "
            + " NON EMPTY filter({[D1].[a],[D1].[b],[D1].[c]}, "
            + "    [Measures].[Value] > 0) "
            + " ON COLUMNS, "
            + " {[D2].[x],[D2].[y],[D2].[z]} "
            + " ON ROWS "
            + "from FT2Extra";

        //getCubeTestContext().assertQueryReturns(mdx, RESULTS);
        Result result = executeQuery(mdx, context.getConnectionWithDefaultRole());
        String resultString = TestUtil.toString(result);
        assertTrue(resultString.equals(RESULTS));
    }




    @Override
	protected String getFileName() {
        return RolapResultTest;
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonAllPromotionMembers(Context context) {
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        prepareContext(context);
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "<Dimension name=\"Promotions2\" foreignKey=\"promotion_id\">\n"
            + "  <Hierarchy hasAll=\"false\" primaryKey=\"promotion_id\">\n"
            + "    <Table name=\"promotion\"/>\n"
            + "    <Level name=\"Promotion2 Name\" column=\"promotion_name\" uniqueMembers=\"true\"/>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>"));
         */
        withSchema(context, SchemaModifiers.RolapResultTestModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Promotion2 Name].[Price Winners], [Promotion2 Name].[Sale Winners]} * {Tail([Time].[Year].Members,3)} ON COLUMNS, "
            + "NON EMPTY Crossjoin({[Store].CurrentMember.Children},  {[Store Type].[All Store Types].Children}) ON ROWS "
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Promotions2].[Price Winners], [Time].[1997]}\n"
            + "{[Promotions2].[Price Winners], [Time].[1998]}\n"
            + "{[Promotions2].[Sale Winners], [Time].[1997]}\n"
            + "{[Promotions2].[Sale Winners], [Time].[1998]}\n"
            + "Axis #2:\n"
            + "{[Store].[USA], [Store Type].[Mid-Size Grocery]}\n"
            + "{[Store].[USA], [Store Type].[Small Grocery]}\n"
            + "{[Store].[USA], [Store Type].[Supermarket]}\n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: 444\n"
            + "Row #0: \n"
            + "Row #1: 23\n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #1: \n"
            + "Row #2: 1,271\n"
            + "Row #2: \n"
            + "Row #2: \n"
            + "Row #2: \n");
    }

    protected Function<CatalogMapping, PojoMappingModifier> getModifierFunction(){
        return RolapResultTestModifier::new;
    }

}
