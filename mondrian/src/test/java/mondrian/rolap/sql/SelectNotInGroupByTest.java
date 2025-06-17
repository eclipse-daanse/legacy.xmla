/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.rolap.sql;

import static org.opencube.junit5.TestUtil.getDialect;
import static org.opencube.junit5.TestUtil.withSchema;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.enums.DatabaseProduct;
import mondrian.olap.SystemWideProperties;
import mondrian.rolap.BatchTestCase;
import mondrian.rolap.SchemaModifiers;
import mondrian.test.SqlPattern;

/**
 * Test that various values of {@link Dialect#allowsSelectNotInGroupBy}
 * produce correctly optimized SQL.
 *
 * @author Eric McDermid
 */
class SelectNotInGroupByTest extends BatchTestCase {

    public static final String storeDimensionLevelIndependent =
        "<Dimension name=\"CustomStore\">\n"
        + "  <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">\n"
        + "    <Table name=\"store\"/>\n"
        + "    <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"/>\n"
        + "    <Level name=\"Store City\" column=\"store_city\" uniqueMembers=\"false\">\n"
        + "      <Property name=\"Store State\" column=\"store_state\"/>\n"
        + "    </Level>\n"
        + "    <Level name=\"Store Name\" column=\"store_name\" uniqueMembers=\"true\"/>\n"
        + "  </Hierarchy>\n"
        + "</Dimension>";

    public static final String storeDimensionLevelDependent =
        "<Dimension name=\"CustomStore\">\n"
        + "  <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">\n"
        + "    <Table name=\"store\"/>\n"
        + "    <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"/>\n"
        + "    <Level name=\"Store City\" column=\"store_city\" uniqueMembers=\"false\">\n"
        + "      <Property name=\"Store State\" column=\"store_state\" dependsOnLevelValue=\"true\"/>\n"
        + "    </Level>\n"
        + "    <Level name=\"Store Name\" column=\"store_name\" uniqueMembers=\"true\"/>\n"
        + "  </Hierarchy>\n"
        + "</Dimension>";

    public static final String storeDimensionUniqueLevelDependentProp =
        "<Dimension name=\"CustomStore\">\n"
        + "  <Hierarchy hasAll=\"true\" primaryKey=\"store_id\" uniqueKeyLevelName=\"Store Name\">\n"
        + "    <Table name=\"store\"/>\n"
        + "    <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"/>\n"
        + "    <Level name=\"Store City\" column=\"store_city\" uniqueMembers=\"false\">\n"
        + "      <Property name=\"Store State\" column=\"store_state\" dependsOnLevelValue=\"true\"/>\n"
        + "    </Level>\n"
        + "    <Level name=\"Store Name\" column=\"store_name\" uniqueMembers=\"true\"/>\n"
        + "  </Hierarchy>\n"
        + "</Dimension>";

    public static final String storeDimensionUniqueLevelIndependentProp =
        "<Dimension name=\"CustomStore\">\n"
        + "  <Hierarchy hasAll=\"true\" primaryKey=\"store_id\" uniqueKeyLevelName=\"Store Name\">\n"
        + "    <Table name=\"store\"/>\n"
        + "    <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"/>\n"
        + "    <Level name=\"Store City\" column=\"store_city\" uniqueMembers=\"false\">\n"
        + "      <Property name=\"Store State\" column=\"store_state\" dependsOnLevelValue=\"false\"/>\n"
        + "    </Level>\n"
        + "    <Level name=\"Store Name\" column=\"store_name\" uniqueMembers=\"true\"/>\n"
        + "  </Hierarchy>\n"
        + "</Dimension>";


    public static final String cubeA =
        "<Cube name=\"CustomSales\">\n"
        + "  <Table name=\"sales_fact_1997\"/>\n"
        + "  <DimensionUsage name=\"CustomStore\" source=\"CustomStore\" foreignKey=\"store_id\"/>\n"
        + "  <Measure name=\"Custom Store Sales\" column=\"store_sales\" aggregator=\"sum\" formatString=\"#,###.00\"/>\n"
        + "  <Measure name=\"Custom Store Cost\" column=\"store_cost\" aggregator=\"sum\"/>\n"
        + "  <Measure name=\"Sales Count\" column=\"product_id\" aggregator=\"count\"/>\n"
        + "</Cube>";

    public static final String queryCubeA =
        "select {[Measures].[Custom Store Sales],[Measures].[Custom Store Cost]} on columns, {[CustomStore].[Store Name].Members} on rows from CustomSales";

    public static final String sqlWithAllGroupBy =
        "select\n"
        + "    `store`.`store_country` as `c0`,\n"
        + "    `store`.`store_city` as `c1`,\n"
        + "    `store`.`store_state` as `c2`,\n"
        + "    `store`.`store_name` as `c3`\n"
        + "from\n"
        + "    `store` as `store`\n"
        + "group by\n"
        + "    `store`.`store_country`,\n"
        + "    `store`.`store_city`,\n"
        + "    `store`.`store_state`,\n"
        + "    `store`.`store_name`\n"
        + "order by\n"
        + "    ISNULL(`c0`) ASC, `c0` ASC,\n"
        + "    ISNULL(`c1`) ASC, `c1` ASC,\n"
        + "    ISNULL(`c3`) ASC, `c3` ASC";

    public static final String sqlWithNoGroupBy =
        "select\n"
        + "    `store`.`store_country` as `c0`,\n"
        + "    `store`.`store_city` as `c1`,\n"
        + "    `store`.`store_state` as `c2`,\n"
        + "    `store`.`store_name` as `c3`\n"
        + "from\n"
        + "    `store` as `store`\n"
        + "order by\n"
        + "    ISNULL(`c0`) ASC, `c0` ASC,\n"
        + "    ISNULL(`c1`) ASC, `c1` ASC,\n"
        + "    ISNULL(`c3`) ASC, `c3` ASC";

    public static final String sqlWithLevelGroupBy =
        "select\n"
        + "    `store`.`store_country` as `c0`,\n"
        + "    `store`.`store_city` as `c1`,\n"
        + "    `store`.`store_state` as `c2`,\n"
        + "    `store`.`store_name` as `c3`\n"
        + "from\n"
        + "    `store` as `store`\n"
        + "group by \n"
        + "    `store`.`store_country`,\n"
        + "    `store`.`store_city`,\n"
        + "    `store`.`store_name`\n"
        + "order by\n"
        + "    ISNULL(`c0`) ASC, `c0` ASC,\n"
        + "    ISNULL(`c1`) ASC, `c1` ASC,\n"
        + "    ISNULL(`c3`) ASC, `c3` ASC";



    @BeforeEach
    public void beforeEach() {
    }

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDependentPropertySkipped(Context<?> context) {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        // Property group by should be skipped only if dialect supports it
        String sqlpat;
        if (dialectAllowsSelectNotInGroupBy(context.getConnectionWithDefaultRole())) {
            sqlpat = sqlWithLevelGroupBy;
        } else {
            sqlpat = sqlWithAllGroupBy;
        }
        SqlPattern[] sqlPatterns = {
            new SqlPattern(DatabaseProduct.MYSQL, sqlpat, sqlpat)
        };

        // Use dimension with level-dependent property
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            storeDimensionLevelDependent,
            cubeA,
            null,
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, SchemaModifiers.SelectNotInGroupByTestModifier1::new);
        assertQuerySqlOrNot(context.getConnectionWithDefaultRole(), queryCubeA, sqlPatterns, false, false, true);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIndependentPropertyNotSkipped(Context<?> context) {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        SqlPattern[] sqlPatterns = {
            new SqlPattern(
                DatabaseProduct.MYSQL,
                sqlWithAllGroupBy,
                sqlWithAllGroupBy)
        };

        // Use dimension with level-independent property
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            storeDimensionLevelIndependent,
            cubeA,
            null,
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, SchemaModifiers.SelectNotInGroupByTestModifier2::new);
        assertQuerySqlOrNot(context.getConnectionWithDefaultRole(), queryCubeA, sqlPatterns, false, false, true);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGroupBySkippedIfUniqueLevel(Context<?> context) {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        // If unique level is included and all properties are level
        // dependent, then group by can be skipped regardless of dialect
        SqlPattern[] sqlPatterns = {
            new SqlPattern(
                DatabaseProduct.MYSQL,
                sqlWithNoGroupBy,
                sqlWithNoGroupBy)
        };

        // Use dimension with unique level & level-dependent properties
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            storeDimensionUniqueLevelDependentProp,
            cubeA,
            null,
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, SchemaModifiers.SelectNotInGroupByTestModifier3::new);
        assertQuerySqlOrNot(context.getConnectionWithDefaultRole(), queryCubeA, sqlPatterns, false, false, true);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGroupByNotSkippedIfIndependentProperty(Context<?> context) {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        SqlPattern[] sqlPatterns = {
            new SqlPattern(
                DatabaseProduct.MYSQL,
                sqlWithAllGroupBy,
                sqlWithAllGroupBy)
        };

        // Use dimension with unique level but level-indpendent property
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            storeDimensionUniqueLevelIndependentProp,
            cubeA,
            null,
            null,
            null,
            null);
        withSchema(context, schema);
        */
        withSchema(context, SchemaModifiers.SelectNotInGroupByTestModifier4::new);
        assertQuerySqlOrNot(context.getConnectionWithDefaultRole(), queryCubeA, sqlPatterns, false, false, true);
    }

    private boolean dialectAllowsSelectNotInGroupBy(Connection connection) {
        final Dialect dialect = getDialect(connection);
        return dialect.allowsSelectNotInGroupBy();
    }
}
