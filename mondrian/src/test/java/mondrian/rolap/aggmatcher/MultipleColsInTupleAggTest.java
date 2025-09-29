/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/
package mondrian.rolap.aggmatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.executeAxis;
import static org.opencube.junit5.TestUtil.getDialect;

import java.util.function.Function;

import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.rolap.common.RolapAxis;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextArgumentsProvider;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.enums.DatabaseProduct;
import mondrian.test.SqlPattern;

/**
 * Testcase for levels that contain multiple columns and are
 * collapsed in the agg table.
 *
 * @author Will Gorman
 */
class MultipleColsInTupleAggTest extends AggTableTestCase {

    @BeforeAll
    public static void beforeAll() {
        ContextArgumentsProvider.dockerWasChanged = true;
    }

    @Override
	@BeforeEach
    public void beforeEach() {
        super.beforeEach();
    }

    @Override
	@AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }



    @Override
	protected String getFileName() {
        return "multiple_cols_in_tuple_agg.csv";
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testTotal(Context<?> context) throws Exception {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);
        if (!isApplicable(context.getConnectionWithDefaultRole())) {
            return;
        }

        // get value without aggregates
        ((TestContextImpl)context).setUseAggregates(false);
        ((TestContextImpl)context).setReadAggregates(false);

        String mdx =
            "select {[Measures].[Total]} on columns from [Fact]";
        Result result = executeQuery(mdx, context.getConnectionWithDefaultRole());
        Object v = result.getCell(new int[]{0}).getValue();

        String mdx2 =
            "select {[Measures].[Total]} on columns from [Fact] where "
            + "{[Product].[Cat One].[Prod Cat One].[One]}";
        Result aresult = executeQuery(mdx2, context.getConnectionWithDefaultRole());
        Object av = aresult.getCell(new int[]{0}).getValue();

        // unless there is a way to flush the cache,
        // I'm skeptical about these results
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(false);

        Result result1 = executeQuery(mdx, context.getConnectionWithDefaultRole());
        Object v1 = result1.getCell(new int[]{0}).getValue();

        assertTrue(v.equals(v1));

        Result aresult2 = executeQuery(mdx2, context.getConnectionWithDefaultRole());
        Object av1 = aresult2.getCell(new int[]{0}).getValue();

        assertTrue(av.equals(av1));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testTupleSelection(Context<?> context) throws Exception {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);
        if (!isApplicable(context.getConnectionWithDefaultRole())) {
            return;
        }

        String mdx =
            "select "
            + "{[Measures].[Total]} on columns, "
            + "non empty CrossJoin({[Product].[Cat One].[Prod Cat One]},"
            + "{[Store].[All Stores]}) on rows "
            + "from [Fact]";

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Total]}\n"
            + "Axis #2:\n"
            + "{[Product].[Product].[Cat One].[Prod Cat One],"
            + " [Store].[Store].[All Stores]}\n"
            + "Row #0: 15\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testNativeFilterWithoutMeasures(Context<?> context) throws Exception {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);
        if (!isApplicable(context.getConnectionWithDefaultRole())) {
            return;
        }
        // Native filter without any measures hit an edge case that
        // could fail to include the Agg star in the WHERE clause,
        // and could also mishandle the field referred to in the native
        // HAVING clause.  ANALYZER-2655
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select "
            + "Filter([Product].[Category].members, "
            + "Product.CurrentMember.Caption MATCHES (\"(?i).*Two.*\") )"
            + " on columns "
            + "from [Fact]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Product].[Product].[Cat Two]}\n"
            + "Row #0: 33\n");
        //  CurrentMember.Name should map to
        // `test_lp_xxx_fact`.`product_category`, with 2 member matches
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select "
            + "Filter([Product].[Product Category].members, "
            + "Product.CurrentMember.Name MATCHES (\"(?i).*Two.*\") )"
            + " on columns "
            + "from [Fact]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Product].[Product].[Cat Two].[Prod Cat Two]}\n"
            + "{[Product].[Product].[Cat One].[Prod Cat Two]}\n"
            + "Row #0: 16\n"
            + "Row #0: 18\n");
        // .Caption is defined as `product_cat`.`cap`.
        // [Cat One].[Prod Cat Two] has just one caption matching -- "PCTwo"
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select "
            + "Filter([Product].[Product Category].Members, "
            + "Product.CurrentMember.Caption MATCHES (\"(?i).*Two.*\") )"
            + " on columns "
            + "from [Fact]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Product].[Product].[Cat One].[Prod Cat Two]}\n"
            + "Row #0: 18\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testNativeFilterWithoutMeasuresAndLevelWithProps(Context<?> context)
        throws Exception
    {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);
        if (!isApplicable(context.getConnectionWithDefaultRole())) {
            return;
        }
        // similar to the previous test, but verifies a case where
        // a level property is the extra column that requires joining
        // agg star back to the dim table.  This test also uses the bottom
        // level of the dim
        final String query = "select "
            + "Filter([Product].[Product].[Product Name].members, "
            + "Product.Product.CurrentMember.Caption MATCHES (\"(?i).*Two.*\") )"
            + " on columns "
            + "from [Fact] ";
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            query,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Product].[Product].[Cat One].[Prod Cat One].[Two]}\n"
            + "Row #0: 6\n");

        // check generated sql only for native evaluation
        if (context.getConfigValue(ConfigConstants.ENABLE_NATIVE_FILTER, ConfigConstants.ENABLE_NATIVE_FILTER_DEFAULT_VALUE, Boolean.class)) {
          assertQuerySql(context.getConnectionWithDefaultRole(),
              query,
              new SqlPattern[] {
                  new SqlPattern(
                      DatabaseProduct.MYSQL,
                  "select\n"
                  + "    `cat`.`cat` as `c0`,\n"
                  + "    `cat`.`cap` as `c1`,\n"
                  + "    `cat`.`ord` as `c2`,\n"
                  + "    `cat`.`name3` as `c3`,\n"
                  + "    `product_cat`.`name2` as `c4`,\n"
                  + "    `product_cat`.`cap` as `c5`,\n"
                  + "    `product_cat`.`ord` as `c6`,\n"
                  + "    `test_lp_xx2_fact`.`prodname` as `c7`,\n"
                  + "    `product_csv`.`color` as `c8`\n"
                  + "from\n"
                  + "    `product_csv` as `product_csv`,\n"
                  + "    `product_cat` as `product_cat`,\n"
                  + "    `cat` as `cat`,\n"
                  + "    `test_lp_xx2_fact` as `test_lp_xx2_fact`\n"
                  + "where\n"
                  + "    `product_cat`.`cat` = `cat`.`cat`\n"
                  + "and\n"
                  + "    `product_csv`.`prod_cat` = `product_cat`.`prod_cat`\n"
                  + "and\n"
                  + "    `product_csv`.`name1` = `test_lp_xx2_fact`.`prodname`\n"
                  + "group by\n"
                  + "    `cat`.`cat`,\n"
                  + "    `cat`.`cap`,\n"
                  + "    `cat`.`ord`,\n"
                  + "    `cat`.`name3`,\n"
                  + "    `product_cat`.`name2`,\n"
                  + "    `product_cat`.`cap`,\n"
                  + "    `product_cat`.`ord`,\n"
                  + "    `test_lp_xx2_fact`.`prodname`,\n"
                  + "    `product_csv`.`color`\n"
                  + "having\n"
                  + "    c7 IS NOT NULL AND UPPER(c7) REGEXP '.*TWO.*'\n"
                  + "order by\n"
                  + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                      ? "    ISNULL(`c2`) ASC, `c2` ASC,\n"
                      + "    ISNULL(`c0`) ASC, `c0` ASC,\n"
                      + "    ISNULL(`c6`) ASC, `c6` ASC,\n"
                      + "    ISNULL(`c4`) ASC, `c4` ASC,\n"
                      + "    ISNULL(`c7`) ASC, `c7` ASC"
                      : "    ISNULL(`cat`.`ord`) ASC, `cat`.`ord` ASC,\n"
                      + "    ISNULL(`product_cat`.`ord`) ASC, `product_cat`.`ord` ASC,\n"
                      + "    ISNULL(`test_lp_xx2_fact`.`prodname`) ASC, "
                      + "`test_lp_xx2_fact`.`prodname` ASC"), null)});
        }
        Axis axis = executeAxis(context.getConnectionWithDefaultRole(), "Fact",
            "Filter([Product].[Product].[Product Name].members, "
            + "Product.Product.CurrentMember.Caption MATCHES (\"(?i).*Two.*\") )");
        assertEquals(
            "Black",
            ((RolapAxis) axis).getTupleList().get(0).get(0)
                .getPropertyValue("Product Color"), "Member property value was not loaded correctly.");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testChildSelection(Context<?> context) throws Exception {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setDisableCaching(true);
        prepareContext(context);
        if (!isApplicable(context.getConnectionWithDefaultRole())) {
            return;
        }

        String mdx = "select {[Measures].[Total]} on columns, "
            + "non empty [Product].[Cat One].Children on rows from [Fact]";
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Total]}\n"
            + "Axis #2:\n"
            + "{[Product].[Product].[Cat One].[Prod Cat Two]}\n"
            + "{[Product].[Product].[Cat One].[Prod Cat One]}\n"
            + "Row #0: 18\n"
            + "Row #1: 15\n");
    }

    @Override
    protected Function<Catalog, CatalogMappingSupplier> getModifierFunction(){
        return MultipleColsInTupleAggTestModifierEmf::new;
    }

}
