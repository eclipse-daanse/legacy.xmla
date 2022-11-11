/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2006-2017 Hitachi Vantara and others
// All Rights Reserved.
 */
package mondrian.rolap.sql;

import mondrian.olap.Connection;
import mondrian.test.PropertySaver5;
import mondrian.test.SqlPattern;
import org.eclipse.daanse.sql.dialect.api.DatabaseProduct;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.Context;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalogAsFile;

import static org.opencube.junit5.TestUtil.*;

public class EffectiveMemberCacheTest {

    private PropertySaver5 propSaver;

    @BeforeEach
    public void beforeEach() {
        propSaver = new PropertySaver5();
        propSaver.set(propSaver.properties.GenerateFormattedSql, true);
        propSaver.set(propSaver.properties.EnableNativeNonEmpty, true);
    }

    @AfterEach
    public void afterEach() {
        propSaver.reset();
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testCachedLevelMembers(Context context) {
        Connection connection = context.createConnection();
        clearCache(connection);
        // verify query for specific members can be fulfilled by members cached
        // from a level members query.
        String sql = "select\n"
                + "    `product`.`product_name` as `c0`\n"
                + "from\n"
                + "    `product` as `product`,\n"
                + "    `product_class` as `product_class`\n"
                + "where\n"
                + "    `product`.`product_class_id` = `product_class`.`product_class_id`\n"
                + "and\n"
                + "    (`product`.`brand_name` = 'Hermanos' and `product_class`.`product_subcategory` = 'Fresh Fruit' and `product_class`.`product_category` = 'Fruit' and `product_class`.`product_department` = 'Produce' and `product_class`.`product_family` = 'Food')\n"
                + "and\n"
                + "    ( UPPER(`product`.`product_name`) IN (UPPER('Hermanos Fancy Plums'),UPPER('Hermanos Lemons'),UPPER('Hermanos Plums')))\n"
                + "group by\n"
                + "    `product`.`product_name`\n"
                + "order by\n"
                + (getDialect(context.createConnection()).requiresOrderByAlias()
                ? "    ISNULL(`c0`) ASC, `c0` ASC"
                : "    ISNULL(`product`.`product_name`) ASC, "
                + "`product`.`product_name` ASC");
        testWithAndWithoutCachedMembers(connection,
            "select Product.[Product Name].members on 0 from sales",
            "select "
            + " { [Product].[Food].[Produce].[Fruit].[Fresh Fruit].[Hermanos].[Hermanos Fancy Plums], "
            + "[Product].[Food].[Produce].[Fruit].[Fresh Fruit].[Hermanos].[Hermanos Lemons],"
            + "[Product].[Food].[Produce].[Fruit].[Fresh Fruit].[Hermanos].[Hermanos Plums] }"
            + " on 0 from sales",
            new SqlPattern[]{
                new SqlPattern(
                    DatabaseProduct.MYSQL, sql, null)}
        );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testCachedChildMembers(Context context) {
        Connection connection = context.createConnection();
        clearCache(connection);
        // verify query for specific members can be fulfilled by members cached
        // from a child members query.
        String sql = "select\n"
                + "    `product`.`product_name` as `c0`\n"
                + "from\n"
                + "    `product` as `product`,\n"
                + "    `product_class` as `product_class`\n"
                + "where\n"
                + "    `product`.`product_class_id` = `product_class`.`product_class_id`\n"
                + "and\n"
                + "    (`product`.`brand_name` = 'Hermanos' and `product_class`.`product_subcategory` = 'Fresh Fruit' and `product_class`.`product_category` = 'Fruit' and `product_class`.`product_department` = 'Produce' and `product_class`.`product_family` = 'Food')\n"
                + "and\n"
                + "    ( UPPER(`product`.`product_name`) IN "
                + "(UPPER('Hermanos Fancy Plums'),UPPER('Hermanos Lemons'),UPPER('Hermanos Plums')))\n"
                + "group by\n"
                + "    `product`.`product_name`\n"
                + "order by\n"
                + (getDialect(context.createConnection()).requiresOrderByAlias()
                ? "    ISNULL(`c0`) ASC, `c0` ASC"
                : "    ISNULL(`product`.`product_name`) ASC, "
                + "`product`.`product_name` ASC");
        testWithAndWithoutCachedMembers(connection,
            "select [Product].[Food].[Produce].[Fruit].[Fresh Fruit].[Hermanos].Children on 0 from sales",
            "select "
            + " { [Product].[Food].[Produce].[Fruit].[Fresh Fruit].[Hermanos].[Hermanos Fancy Plums], "
            + "[Product].[Food].[Produce].[Fruit].[Fresh Fruit].[Hermanos].[Hermanos Lemons],"
            + "[Product].[Food].[Produce].[Fruit].[Fresh Fruit].[Hermanos].[Hermanos Plums] }"
            + " on 0 from sales",
            new SqlPattern[]{
                new SqlPattern(
                    DatabaseProduct.MYSQL, sql, null) }
        );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testLevelPreCacheThreshold(Context context) {
        Connection connection = context.createConnection();
        clearCache(connection);
        // [Store Type] members cardinality falls well below
        // LevelPreCacheThreshold.  All members should be loaded, not
        // just the 2 referenced.
        propSaver.set(propSaver.properties.LevelPreCacheThreshold, 300);
        String sql = "select\n"
                + "    `store`.`store_type` as `c0`\n"
                + "from\n"
                + "    `store` as `store`\n"
                + "group by\n"
                + "    `store`.`store_type`\n"
                + "order by\n"
                + (getDialect(connection).requiresOrderByAlias()
                ? "    ISNULL(`c0`) ASC, `c0` ASC"
                : "    ISNULL(`store`.`store_type`) ASC, "
                + "`store`.`store_type` ASC");
        assertQuerySql(connection,
            "select {[Store Type].[Gourmet Supermarket], "
            + "[Store Type].[HeadQuarters]} on 0 from sales",
            new SqlPattern[] {
                new SqlPattern(
                    DatabaseProduct.MYSQL, sql, null)
            });
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testLevelPreCacheThresholdDisabled(Context context) {
        // with LevelPreCacheThreshold set to 0, we should not load
        // all [store type] members, we should only retrieve the 2
        // specified.
        Connection connection = context.createConnection();
        clearCache(connection);
        propSaver.set(propSaver.properties.LevelPreCacheThreshold, 0);
        String sql = "select\n"
                + "    `store`.`store_type` as `c0`\n"
                + "from\n"
                + "    `store` as `store`\n"
                + "where\n"
                + "    ( UPPER(`store`.`store_type`) IN "
                + "(UPPER('Gourmet Supermarket'),UPPER('HeadQuarters')))\n"
                + "group by\n"
                + "    `store`.`store_type`\n"
                + "order by\n"
                + (getDialect(connection).requiresOrderByAlias()
                ? "    ISNULL(`c0`) ASC, `c0` ASC"
                : "    ISNULL(`store`.`store_type`) ASC, "
                + "`store`.`store_type` ASC");
        assertQuerySql(
            connection,
            "select {[Store Type].[Gourmet Supermarket], "
            + "[Store Type].[HeadQuarters]} on 0 from sales",
            new SqlPattern[] {
                new SqlPattern(
                    DatabaseProduct.MYSQL, sql, null)
            });
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testLevelPreCacheThresholdParentDegenerate(Context context) {
        // we should avoid pulling all deg members, regardless of cardinality.
        // The cost of doing full scans of the fact table is assumed
        // to be too high.
        Connection connection = context.createConnection();
        clearCache(connection);
        propSaver.set(propSaver.properties.LevelPreCacheThreshold, 1000);
        String sql = "select\n"
                + "    `store`.`coffee_bar` as `c0`\n"
                + "from\n"
                + "    `store` as `store`\n"
                + "where\n"
                + "    `store`.`coffee_bar` = false\n"
                + "group by\n"
                + "    `store`.`coffee_bar`\n"
                + "order by\n"
                + (getDialect(connection).requiresOrderByAlias()
                ? "    ISNULL(`c0`) ASC, `c0` ASC"
                : "    ISNULL(`store`.`coffee_bar`) ASC, "
                + "`store`.`coffee_bar` ASC");
        assertQuerySql(
            connection,
            "select {[Has coffee bar].[All Has coffee bars].[false]} on 0 from Store",
            new SqlPattern[]{
                new SqlPattern(
                    DatabaseProduct.MYSQL, sql, null)});
    }


    /**
     * Execute testMdx both with and without running the cacheMdx first,
     * validating that sqlToLoadTestMdxMembers either fires or doesn't fire,
     * as appropriate.
     *
     * Assumption is that if the cacheMdx has fired, then members shoould
     * already be in cache and there is no need to load them.  If cacheMedx
     * is not fired we should see the sqlToLoadTestMdxMembers.
     */
    private void testWithAndWithoutCachedMembers(Connection connection,
        String cacheMdx, String testMdx, SqlPattern[] sqlToLoadTestMdxMembers)
    {
        for (boolean membersCached : new boolean[] {false, true}) {
            clearCache(connection);
            if (membersCached) {
                executeQuery(connection, cacheMdx);
            }
            assertQuerySqlOrNot(connection,
                testMdx,
                sqlToLoadTestMdxMembers,
                membersCached, false, false);
        }
    }

    private void clearCache(Connection connection) {
        flushSchemaCache(connection);
        //testContext = getTestContext().withFreshConnection();
    }
}
// End EffectiveMemberCacheTest.java
