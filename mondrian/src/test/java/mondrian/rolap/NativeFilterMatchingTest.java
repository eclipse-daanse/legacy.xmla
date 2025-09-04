/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2017 Hitachi Vantara.  All rights reserved.
*/
package mondrian.rolap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.getDialect;
import static org.opencube.junit5.TestUtil.verifySameNativeAndNot;
import static org.opencube.junit5.TestUtil.withSchema;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.connection.ConnectionProps;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.rolap.mapping.api.model.AccessRoleMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessCatalog;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessCube;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessHierarchy;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessMember;
import org.eclipse.daanse.rolap.mapping.api.model.enums.RollupPolicyType;
import org.eclipse.daanse.rolap.mapping.instance.rec.complex.foodmart.FoodmartMappingSupplier;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.AccessCatalogGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessCubeGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessHierarchyGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessMemberGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessRoleMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.ExplicitHierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SumMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.enums.DatabaseProduct;
import mondrian.test.SqlPattern;

/**
 * Test case for pushing MDX filter conditions down to SQL.
 */
class NativeFilterMatchingTest extends BatchTestCase {


    @BeforeEach
    public void beforeEach() {

    }

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testPositiveMatching(Context<?> context) throws Exception {
    	context.getCatalogCache().clear();
        if (!context.getConfigValue(ConfigConstants.ENABLE_NATIVE_FILTER, ConfigConstants.ENABLE_NATIVE_FILTER_DEFAULT_VALUE, Boolean.class)) {
            // No point testing these if the native filters
            // are turned off.
            return;
        }
        final String sqlOracle =
            "select \"customer\".\"country\" as \"c0\", \"customer\".\"state_province\" as \"c1\", \"customer\".\"city\" as \"c2\", \"customer\".\"customer_id\" as \"c3\", \"fname\" || ' ' || \"lname\" as \"c4\", \"fname\" || ' ' || \"lname\" as \"c5\", \"customer\".\"gender\" as \"c6\", \"customer\".\"marital_status\" as \"c7\", \"customer\".\"education\" as \"c8\", \"customer\".\"yearly_income\" as \"c9\" from \"customer\" \"customer\" group by \"customer\".\"country\", \"customer\".\"state_province\", \"customer\".\"city\", \"customer\".\"customer_id\", \"fname\" || ' ' || \"lname\", \"customer\".\"gender\", \"customer\".\"marital_status\", \"customer\".\"education\", \"customer\".\"yearly_income\" having \"fname\" || ' ' || \"lname\" IS NOT NULL AND REGEXP_LIKE(\"fname\" || ' ' || \"lname\", '.*jeanne.*', 'i') order by \"customer\".\"country\" ASC NULLS LAST, \"customer\".\"state_province\" ASC NULLS LAST, \"customer\".\"city\" ASC NULLS LAST, \"fname\" || ' ' || \"lname\" ASC NULLS LAST";
        final String sqlPgsql =
            "select \"customer\".\"country\" as \"c0\", \"customer\".\"state_province\" as \"c1\", \"customer\".\"city\" as \"c2\", \"customer\".\"customer_id\" as \"c3\", fullname as \"c4\", fullname as \"c5\", \"customer\".\"gender\" as \"c6\", \"customer\".\"marital_status\" as \"c7\", \"customer\".\"education\" as \"c8\", \"customer\".\"yearly_income\" as \"c9\" from \"customer\" as \"customer\" group by \"customer\".\"country\", \"customer\".\"state_province\", \"customer\".\"city\", \"customer\".\"customer_id\", fullname, \"customer\".\"gender\", \"customer\".\"marital_status\", \"customer\".\"education\", \"customer\".\"yearly_income\" having cast(fullname as text) is not null and cast(fullname as text) ~ '(?i).*jeanne.*'  order by \"customer\".\"country\" ASC NULLS LAST, \"customer\".\"state_province\" ASC NULLS LAST, \"customer\".\"city\" ASC NULLS LAST, fullname ASC NULLS LAST";
        final String sqlMysql =
            "select `customer`.`country` as `c0`, `customer`.`state_province` as `c1`, `customer`.`city` as `c2`, `customer`.`customer_id` as `c3`, CONCAT(`customer`.`fname`, ' ', `customer`.`lname`) as `c4`, CONCAT(`customer`.`fname`, ' ', `customer`.`lname`) as `c5`, `customer`.`gender` as `c6`, `customer`.`marital_status` as `c7`, `customer`.`education` as `c8`, `customer`.`yearly_income` as `c9` from `customer` as `customer` group by `customer`.`country`, `customer`.`state_province`, `customer`.`city`, `customer`.`customer_id`, CONCAT(`customer`.`fname`, ' ', `customer`.`lname`), `customer`.`gender`, `customer`.`marital_status`, `customer`.`education`, `customer`.`yearly_income` having c5 IS NOT NULL AND UPPER(c5) REGEXP '.*JEANNE.*' order by "
                + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                    ? "ISNULL(`c0`) ASC, `c0` ASC, "
                    + "ISNULL(`c1`) ASC, `c1` ASC, "
                    + "ISNULL(`c2`) ASC, `c2` ASC, "
                    + "ISNULL(`c4`) ASC, `c4` ASC"
                    : "ISNULL(`customer`.`country`) ASC, `customer`.`country` ASC, ISNULL(`customer`.`state_province`) ASC, `customer`.`state_province` ASC, ISNULL(`customer`.`city`) ASC, `customer`.`city` ASC, ISNULL(CONCAT(`customer`.`fname`, ' ', `customer`.`lname`)) ASC, CONCAT(`customer`.`fname`, ' ', `customer`.`lname`) ASC");

        SqlPattern[] patterns = {
            new SqlPattern(
                DatabaseProduct.ORACLE,
                sqlOracle,
                sqlOracle.length()),
            new SqlPattern(
                DatabaseProduct.MYSQL,
                sqlMysql,
                sqlMysql.length()),
            new SqlPattern(
                DatabaseProduct.POSTGRES,
                sqlPgsql,
                sqlPgsql.length())
        };
        final String queryResults =
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Customers].[Customers].[USA].[WA].[Issaquah].[Jeanne Derry], [Measures].[*FORMATTED_MEASURE_0]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Los Angeles].[Jeannette Eldridge], [Measures].[*FORMATTED_MEASURE_0]}\n"
            + "{[Customers].[Customers].[USA].[CA].[Burbank].[Jeanne Bohrnstedt], [Measures].[*FORMATTED_MEASURE_0]}\n"
            + "{[Customers].[Customers].[USA].[OR].[Portland].[Jeanne Zysko], [Measures].[*FORMATTED_MEASURE_0]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Everett].[Jeanne McDill], [Measures].[*FORMATTED_MEASURE_0]}\n"
            + "{[Customers].[Customers].[USA].[CA].[West Covina].[Jeanne Whitaker], [Measures].[*FORMATTED_MEASURE_0]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Everett].[Jeanne Turner], [Measures].[*FORMATTED_MEASURE_0]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Puyallup].[Jeanne Wentz], [Measures].[*FORMATTED_MEASURE_0]}\n"
            + "{[Customers].[Customers].[USA].[OR].[Albany].[Jeannette Bura], [Measures].[*FORMATTED_MEASURE_0]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Lynnwood].[Jeanne Ibarra], [Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Row #0: 50\n"
            + "Row #0: 21\n"
            + "Row #0: 31\n"
            + "Row #0: 42\n"
            + "Row #0: 110\n"
            + "Row #0: 59\n"
            + "Row #0: 42\n"
            + "Row #0: 157\n"
            + "Row #0: 146\n"
            + "Row #0: 78\n";
        final String query =
            "With\n"
            + "Set [*NATIVE_CJ_SET] as 'Filter([*BASE_MEMBERS_Customers], Not IsEmpty ([Measures].[Unit Sales]))'\n"
            + "Set [*SORTED_COL_AXIS] as 'Order([*CJ_COL_AXIS],[Customers].CurrentMember.OrderKey,BASC,Ancestor([Customers].CurrentMember,[Customers].[City]).OrderKey,BASC)'\n"
            + "Set [*BASE_MEMBERS_Customers] as 'Filter([Customers].[Name].Members,[Customers].CurrentMember.Caption Matches (\"(?i).*\\Qjeanne\\E.*\"))'\n"
            + "Set [*BASE_MEMBERS_Measures] as '{[Measures].[*FORMATTED_MEASURE_0]}'\n"
            + "Set [*CJ_COL_AXIS] as 'Generate([*NATIVE_CJ_SET], {([Customers].currentMember)})'\n"
            + "Member [Measures].[*FORMATTED_MEASURE_0] as '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=400\n"
            + "Select\n"
            + "CrossJoin([*SORTED_COL_AXIS],[*BASE_MEMBERS_Measures]) on columns\n"
            + "From [Sales]";
        assertQuerySqlOrNot(
            context.getConnectionWithDefaultRole(),
            query,
            patterns,
            false,
            true,
            true);
        assertQueryReturns(
            context.getConnectionWithDefaultRole(),
            query,
            queryResults);
        verifySameNativeAndNot(context.getConnectionWithDefaultRole(), query, null);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNegativeMatching(Context<?> context) throws Exception {
    	context.getCatalogCache().clear();
        if (!context.getConfigValue(ConfigConstants.ENABLE_NATIVE_FILTER, ConfigConstants.ENABLE_NATIVE_FILTER_DEFAULT_VALUE, Boolean.class)) {
             // No point testing these if the native filters
             // are turned off.
            return;
        }
        final String sqlOracle =
            "select \"customer\".\"country\" as \"c0\", \"customer\".\"state_province\" as \"c1\", \"customer\".\"city\" as \"c2\", \"customer\".\"customer_id\" as \"c3\", \"fname\" || ' ' || \"lname\" as \"c4\", \"fname\" || ' ' || \"lname\" as \"c5\", \"customer\".\"gender\" as \"c6\", \"customer\".\"marital_status\" as \"c7\", \"customer\".\"education\" as \"c8\", \"customer\".\"yearly_income\" as \"c9\" from \"customer\" \"customer\" group by \"customer\".\"country\", \"customer\".\"state_province\", \"customer\".\"city\", \"customer\".\"customer_id\", \"fname\" || ' ' || \"lname\", \"customer\".\"gender\", \"customer\".\"marital_status\", \"customer\".\"education\", \"customer\".\"yearly_income\" having NOT(\"fname\" || ' ' || \"lname\" IS NOT NULL AND REGEXP_LIKE(\"fname\" || ' ' || \"lname\", '.*jeanne.*', 'i'))  order by \"customer\".\"country\" ASC NULLS LAST, \"customer\".\"state_province\" ASC NULLS LAST, \"customer\".\"city\" ASC NULLS LAST, \"fname\" || ' ' || \"lname\" ASC NULLS LAST";
        final String sqlPgsql =
            "select \"customer\".\"country\" as \"c0\", \"customer\".\"state_province\" as \"c1\", \"customer\".\"city\" as \"c2\", \"customer\".\"customer_id\" as \"c3\", fullname as \"c4\", fullname as \"c5\", \"customer\".\"gender\" as \"c6\", \"customer\".\"marital_status\" as \"c7\", \"customer\".\"education\" as \"c8\", \"customer\".\"yearly_income\" as \"c9\" from \"customer\" as \"customer\" group by \"customer\".\"country\", \"customer\".\"state_province\", \"customer\".\"city\", \"customer\".\"customer_id\", fullname, \"customer\".\"gender\", \"customer\".\"marital_status\", \"customer\".\"education\", \"customer\".\"yearly_income\" having NOT(cast(fullname as text) is not null and cast(fullname as text) ~ '(?i).*jeanne.*')  order by \"customer\".\"country\" ASC NULLS LAST, \"customer\".\"state_province\" ASC NULLS LAST, \"customer\".\"city\" ASC NULLS LAST, fullname ASC NULLS LAST";
        final String sqlMysql =
            "select `customer`.`country` as `c0`, `customer`.`state_province` as `c1`, `customer`.`city` as `c2`, `customer`.`customer_id` as `c3`, CONCAT(`customer`.`fname`, ' ', `customer`.`lname`) as `c4`, CONCAT(`customer`.`fname`, ' ', `customer`.`lname`) as `c5`, `customer`.`gender` as `c6`, `customer`.`marital_status` as `c7`, `customer`.`education` as `c8`, `customer`.`yearly_income` as `c9` from `customer` as `customer` group by `customer`.`country`, `customer`.`state_province`, `customer`.`city`, `customer`.`customer_id`, CONCAT(`customer`.`fname`, ' ', `customer`.`lname`), `customer`.`gender`, `customer`.`marital_status`, `customer`.`education`, `customer`.`yearly_income` having NOT(c5 IS NOT NULL AND UPPER(c5) REGEXP '.*JEANNE.*')  order by "
                + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                    ? "ISNULL(`c0`) ASC, `c0` ASC, "
                    + "ISNULL(`c1`) ASC, `c1` ASC, "
                    + "ISNULL(`c2`) ASC, `c2` ASC, "
                    + "ISNULL(`c4`) ASC, `c4` ASC"
                    : "ISNULL(`customer`.`country`) ASC, `customer`.`country` ASC, ISNULL(`customer`.`state_province`) ASC, `customer`.`state_province` ASC, ISNULL(`customer`.`city`) ASC, `customer`.`city` ASC, ISNULL(CONCAT(`customer`.`fname`, ' ', `customer`.`lname`)) ASC, CONCAT(`customer`.`fname`, ' ', `customer`.`lname`) ASC");
        SqlPattern[] patterns = {
            new SqlPattern(
                DatabaseProduct.ORACLE,
                sqlOracle,
                sqlOracle.length()),
            new SqlPattern(
                DatabaseProduct.MYSQL,
                sqlMysql,
                sqlMysql.length()),
            new SqlPattern(
                DatabaseProduct.POSTGRES,
                sqlPgsql,
                sqlPgsql.length())
        };

        final String query =
            "With\n"
            + "Set [*NATIVE_CJ_SET] as 'Filter([*BASE_MEMBERS_Customers], Not IsEmpty ([Measures].[Unit Sales]))'\n"
            + "Set [*SORTED_COL_AXIS] as 'Order([*CJ_COL_AXIS],[Customers].CurrentMember.OrderKey,BASC,Ancestor([Customers].CurrentMember,[Customers].[City]).OrderKey,BASC)'\n"
            + "Set [*BASE_MEMBERS_Customers] as 'Filter([Customers].[Name].Members,[Customers].CurrentMember.Caption Not Matches (\"(?i).*\\Qjeanne\\E.*\"))'\n"
            + "Set [*BASE_MEMBERS_Measures] as '{[Measures].[*FORMATTED_MEASURE_0]}'\n"
            + "Set [*CJ_COL_AXIS] as 'Generate([*NATIVE_CJ_SET], {([Customers].currentMember)})'\n"
            + "Member [Measures].[*FORMATTED_MEASURE_0] as '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=400\n"
            + "Select\n"
            + "CrossJoin([*SORTED_COL_AXIS],[*BASE_MEMBERS_Measures]) on columns\n"
            + "From [Sales]";

        assertQuerySqlOrNot(
            context.getConnectionWithDefaultRole(),
            query,
            patterns,
            false,
            true,
            true);

        final Result result = executeQuery(query, context.getConnectionWithDefaultRole());
        final String resultString = TestUtil.toString(result);
        assertFalse(resultString.contains("Jeanne"));
        verifySameNativeAndNot(context.getConnectionWithDefaultRole(), query, null);
    }

    /**
     * <p>System test case for bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-983">MONDRIAN-983,
     * "Regression: Unable to execute MDX statement with native MATCHES"</a>.
     *
     * @see mondrian.test.DialectTest#testRegularExpressionSqlInjection()
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMatchBugMondrian983(Context<?> context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "With\n"
            + "Set [*NATIVE_CJ_SET] as 'Filter([*BASE_MEMBERS_Product], Not IsEmpty ([Measures].[Unit Sales]))' \n"
            + "Set [*SORTED_ROW_AXIS] as 'Order([*CJ_ROW_AXIS],[Product].CurrentMember.OrderKey,BASC,Ancestor([Product].CurrentMember,[Product].[Product Department]).OrderKey,BASC)' \n"
            + "Set [*NATIVE_MEMBERS_Product] as 'Generate([*NATIVE_CJ_SET], {[Product].CurrentMember})' \n"
            + "Set [*BASE_MEMBERS_Product] as 'Filter([Product].[Product Category].Members,[Product].CurrentMember.Caption Matches (\"(?i).*\\Qa\"\"\\); window.alert(\"\"woot'');\\E.*\"))' \n"
            + "Set [*BASE_MEMBERS_Measures] as '{[Measures].[*FORMATTED_MEASURE_0]}' \n"
            + "Set [*CJ_ROW_AXIS] as 'Generate([*NATIVE_CJ_SET], {([Product].currentMember)})' \n"
            + "Set [*CJ_COL_AXIS] as '[*NATIVE_CJ_SET]' \n"
            + "Member [Product].[*TOTAL_MEMBER_SEL~SUM] as 'Sum([*NATIVE_MEMBERS_Product])', SOLVE_ORDER=-100 \n"
            + "Member [Measures].[*FORMATTED_MEASURE_0] as '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=400 \n"
            + "Select\n"
            + "[*BASE_MEMBERS_Measures] on columns,\n"
            + "Union({[Product].[*TOTAL_MEMBER_SEL~SUM]},[*SORTED_ROW_AXIS]) on rows\n"
            + "From [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Product].[Product].[*TOTAL_MEMBER_SEL~SUM]}\n"
            + "Row #0: \n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNativeFilterSameAsNonNative(Context<?> context) {
        // http://jira.pentaho.com/browse/MONDRIAN-1694
        // In some cases native filter would includes an unnecessary fact table
        // join which incorrectly eliminated some tuples from the set
        Connection connection = context.getConnectionWithDefaultRole();
        verifySameNativeAndNot(connection,
            "select Filter([Store].[Store Name].Members, Store.CurrentMember.Name matches \"Store.*\") "
            + " on 0 from sales",
            "Filter w/ regex.");

        verifySameNativeAndNot(connection,
            "select Filter([Store].[Store Name].Members, Measures.[Unit Sales] > 100 and Store.CurrentMember.Name matches \"Store.*\") "
            + " on 0 from sales",
            "Filter w/ regex and measure constraint.");

        verifySameNativeAndNot(connection,
            "select Filter([Store].[Store Name].Members, measures.[Unit Sales] > 100) "
            + " on 0 from sales",
            "Filter w/ measure constraint.");

        verifySameNativeAndNot(connection,
            "select non empty Filter([Store].[Store Name].Members, Store.CurrentMember.Name matches \"Store.*\") "
            + " on 0 from sales",
            "Filter w/ regex in non-empty context.");

        verifySameNativeAndNot(connection,
            "with set [filterSet] as 'Filter([Store].[Store Name].Members, Store.CurrentMember.Name matches \"Store.*\")'"
            + " select [filterSet] on 0 from sales",
            "Filter w/ regex defined in named set.");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCachedNativeFilter(Context<?> context) {
        // http://jira.pentaho.com/browse/MONDRIAN-1694

        // verify that the RolapNativeSet cached values from NON EMPTY context
        // are not reused when not NON EMPTY.
        Connection connection = context.getConnectionWithDefaultRole();
        verifySameNativeAndNot(connection,
            "select NON EMPTY Filter([Store].[Store Name].Members, Store.CurrentMember.Name matches \"Store.*\") "
            + " on 0 from sales",
            "Filter w/ regex.");
        verifySameNativeAndNot(connection,
            "select Filter([Store].[Store Name].Members, Store.CurrentMember.Name matches \"Store.*\") "
            + " on 0 from sales",
            "Filter w/ regex.");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMatchesWithAccessControl(Context<?> context) {

        class TestCachedNativeFilterModifier extends PojoMappingModifier {

        	private static final HierarchyMappingImpl h =ExplicitHierarchyMappingImpl.builder()
            .withHasAll(true)
            .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
            .withLevels(List.of(
                LevelMappingImpl.builder()
                    .withName("Store Country")
                    .withColumn(FoodmartMappingSupplier.STORE_COUNTRY_COLUMN_IN_STORE)
                    .withUniqueMembers(true)
                    .build(),
                LevelMappingImpl.builder()
                    .withName("Store State")
                    .withColumn(FoodmartMappingSupplier.STORE_STATE_COLUMN_IN_STORE)
                    .withUniqueMembers(true)
                    .build()
            ))
            .build();


        	private static final StandardDimensionMappingImpl dimensionStore2 = StandardDimensionMappingImpl.builder()
            .withName("Store2")
            .withHierarchies(List.of(h))
            .build();

        	private static final PhysicalCubeMappingImpl cube = PhysicalCubeMappingImpl.builder()
            .withName("TinySales")
            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
            .withDimensionConnectors(List.of(
            		DimensionConnectorMappingImpl.builder()
            		.withOverrideDimensionName("Product")
            		.withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
            		.withDimension(FoodmartMappingSupplier.DIMENSION_PRODUCT)
            		.build(),
            		DimensionConnectorMappingImpl.builder()
            		.withOverrideDimensionName("Store2")
            		.withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
            		.withDimension(dimensionStore2)
            		.build()
            		))
            .withMeasureGroups(
            	List.of(MeasureGroupMappingImpl.builder()
            		.withMeasures(List.of(
            		        SumMeasureMappingImpl.builder()
                            .withName("Unit Sales")
                            .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                            .withFormatString("Standard")
                            .build()
            				))
            		.build()))
            .build();

            public TestCachedNativeFilterModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
            	List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                result.add(cube);
                return result;
            }

            @Override
            protected List<? extends AccessRoleMapping> catalogAccessRoles(CatalogMapping catalogMapping) {
                List<AccessRoleMapping> result = new ArrayList<>();
                result.addAll(super.catalogAccessRoles(catalogMapping));
                result.add(AccessRoleMappingImpl.builder()
                    .withName("test")
                    .withAccessCatalogGrants(List.of(
                    		AccessCatalogGrantMappingImpl.builder()
                            .withAccess(AccessCatalog.NONE)
                            .withCubeGrant(List.of(
                            		AccessCubeGrantMappingImpl.builder()
                                    .withCube(cube)
                                    .withAccess(AccessCube.ALL)
                                    .withHierarchyGrants(List.of(
                                    	AccessHierarchyGrantMappingImpl.builder()
                                            .withHierarchy(h)
                                            .withAccess(AccessHierarchy.CUSTOM)
                                            .withRollupPolicyType(RollupPolicyType.PARTIAL)
                                            .withMemberGrants(List.of(
                                            	AccessMemberGrantMappingImpl.builder()
                                                    .withMember("[Store2].[USA].[CA]")
                                                    .withAccess(AccessMember.ALL)
                                                    .build(),
                                                AccessMemberGrantMappingImpl.builder()
                                                    .withMember("[Store2].[USA].[OR]")
                                                    .withAccess(AccessMember.ALL)
                                                    .build(),
                                                AccessMemberGrantMappingImpl.builder()
                                                    .withMember("[Store2].[Canada]")
                                                    .withAccess(AccessMember.ALL)
                                                    .build()
                                            ))
                                            .build()
                                    ))
                                    .build()
                            ))
                            .build()
                    ))
                    .build());
                return result;

            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
                dimension,
            cube, null, null, null,
            roleDefs);
        withSchema(context, schema);
         */
        withSchema(context, TestCachedNativeFilterModifier::new);
        Connection connection = ((TestContext)context).getConnection(new ConnectionProps(List.of("test")));
        verifySameNativeAndNot(connection,
            "select Filter([Product].[Product Category].Members, [Product].CurrentMember.Name matches \"(?i).*Food.*\")"
            + " on 0 from tinysales",
            "Filter on dim with full access.");
        verifySameNativeAndNot(connection,
            "select Filter([Store2].[USA].Children, [Store2].CurrentMember.Name matches \"WA.*\")"
            + " on 0 from tinysales",
            "Filter on restricted dimension.  Should be empty set.");
        verifySameNativeAndNot(connection,
            "select Filter(CrossJoin({[Store2].[USA].Children}, [Product].[Product Category].Members), [Store2].CurrentMember.Name matches \".*A.*\")"
            + " on 0 from tinysales",
            "Filter on partially accessible set of tuples.");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    @DisabledIfSystemProperty(named = "test.disable.knownFails", matches = "true")
    void testNativeFilterWithCompoundSlicer(Context<?> context) {
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        final String mdx =
            "with member measures.avgQtrs as 'avg( filter( time.time.quarter.members, measures.[unit sales] > 80))' "
            + "select measures.avgQtrs * gender.gender.members on 0 from sales where head( product.product.[product name].members, 3)";

        if (context.getConfigValue(ConfigConstants.ENABLE_NATIVE_FILTER, ConfigConstants.ENABLE_NATIVE_FILTER_DEFAULT_VALUE, Boolean.class)
            && SystemWideProperties.instance().EnableNativeNonEmpty)
        {
            boolean requiresOrderByAlias =
                    getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias();
            final String sqlMysql =
                context.getConfigValue(ConfigConstants.USE_AGGREGATES, ConfigConstants.USE_AGGREGATES_DEFAULT_VALUE ,Boolean.class) == false
                    ? "select\n"
                    + "    `time_by_day`.`the_year` as `c0`,\n"
                    + "    `time_by_day`.`quarter` as `c1`\n"
                    + "from\n"
                    + "    `time_by_day` as `time_by_day`,\n"
                    + "    `sales_fact_1997` as `sales_fact_1997`,\n"
                    + "    `product` as `product`,\n"
                    + "    `customer` as `customer`\n"
                    + "where\n"
                    + "    `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`\n"
                    + "and\n"
                    + "    `sales_fact_1997`.`product_id` = `product`.`product_id`\n"
                    + "and\n"
                    + "    `product`.`product_name` in ('Good Imported Beer', 'Good Light Beer', 'Pearl Imported Beer')\n"
                    + "and\n"
                    + "    `sales_fact_1997`.`customer_id` = `customer`.`customer_id`\n"
                    + "and\n"
                    + "    `customer`.`gender` = 'M'\n"
                    + "group by\n"
                    + "    `time_by_day`.`the_year`,\n"
                    + "    `time_by_day`.`quarter`\n"
                    + "having\n"
                    + "    (sum(`sales_fact_1997`.`unit_sales`) > 80)\n"
                    + "order by\n"
                    + (requiresOrderByAlias
                        ? "    ISNULL(`c0`) ASC, `c0` ASC,\n"
                        + "    ISNULL(`c1`) ASC, `c1` ASC"
                        : "    ISNULL(`time_by_day`.`the_year`) ASC, `time_by_day`.`the_year` ASC,\n"
                        + "    ISNULL(`time_by_day`.`quarter`) ASC, `time_by_day`.`quarter` ASC")

                    : "select\n"
                    + "    `agg_c_14_sales_fact_1997`.`the_year` as `c0`,\n"
                    + "    `agg_c_14_sales_fact_1997`.`quarter` as `c1`\n"
                    + "from\n"
                    + "    `agg_c_14_sales_fact_1997` as `agg_c_14_sales_fact_1997`,\n"
                    + "    `product` as `product`,\n"
                    + "    `customer` as `customer`\n"
                    + "where\n"
                    + "    `agg_c_14_sales_fact_1997`.`product_id` = `product`.`product_id`\n"
                    + "and\n"
                    + "    `product`.`product_name` in ('Good Imported Beer', 'Good Light Beer', 'Pearl Imported Beer')\n"
                    + "and\n"
                    + "    `agg_c_14_sales_fact_1997`.`customer_id` = `customer`.`customer_id`\n"
                    + "and\n"
                    + "    `customer`.`gender` = 'M'\n"
                    + "group by\n"
                    + "    `agg_c_14_sales_fact_1997`.`the_year`,\n"
                    + "    `agg_c_14_sales_fact_1997`.`quarter`\n"
                    + "having\n"
                    + "    (sum(`agg_c_14_sales_fact_1997`.`unit_sales`) > 80)\n"
                    + "order by\n"
                    + (requiresOrderByAlias
                        ? "    ISNULL(`c0`) ASC, `c0` ASC,\n"
                        + "    ISNULL(`c1`) ASC, `c1` ASC"
                        : "    ISNULL(`agg_c_14_sales_fact_1997`.`the_year`) ASC, `agg_c_14_sales_fact_1997`.`the_year` ASC,\n"
                        + "    ISNULL(`agg_c_14_sales_fact_1997`.`quarter`) ASC, `agg_c_14_sales_fact_1997`.`quarter` ASC");
            final SqlPattern[] patterns = mysqlPattern(sqlMysql);
            context.getConnectionWithDefaultRole().getCacheControl(null).flushSchemaCache();
            // Make sure the tuples list is using the HAVING clause.
            assertQuerySqlOrNot(
            	context.getConnectionWithDefaultRole(),
                mdx,
                patterns,
                false,
                true,
                true);
        }
        // Make sure the numbers are right
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good].[Good Imported Beer]}\n"
            + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good].[Good Light Beer]}\n"
            + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Pearl].[Pearl Imported Beer]}\n"
            + "Axis #1:\n"
            + "{[Measures].[avgQtrs], [Gender].[All Gender]}\n"
            + "{[Measures].[avgQtrs], [Gender].[F]}\n"
            + "{[Measures].[avgQtrs], [Gender].[M]}\n"
            + "Row #0: 111\n"
            + "Row #0: \n"
            + "Row #0: \n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNativeFilterWithCompoundSlicerWithAggs(Context<?> context) {
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        final String mdx =
            "with member measures.avgQtrs as 'avg( filter( time.quarter.members, measures.[unit sales] > 80))' "
            + "select measures.avgQtrs * gender.members on 0 from sales where head( product.[product name].members, 3)";
        if (context.getConfigValue(ConfigConstants.ENABLE_NATIVE_FILTER, ConfigConstants.ENABLE_NATIVE_FILTER_DEFAULT_VALUE, Boolean.class)
            && SystemWideProperties.instance().EnableNativeNonEmpty)
        {
            final String sqlMysql =
                "select\n"
                + "    `agg_c_14_sales_fact_1997`.`the_year` as `c0`,\n"
                + "    `agg_c_14_sales_fact_1997`.`quarter` as `c1`\n"
                + "from\n"
                + "    `agg_c_14_sales_fact_1997` as `agg_c_14_sales_fact_1997`,\n"
                + "    `product` as `product`,\n"
                + "    `customer` as `customer`\n"
                + "where\n"
                + "    `agg_c_14_sales_fact_1997`.`product_id` = `product`.`product_id`\n"
                + "and\n"
                + "    `product`.`product_name` in ('Good Imported Beer', 'Good Light Beer', 'Pearl Imported Beer')\n"
                + "and\n"
                + "    `agg_c_14_sales_fact_1997`.`customer_id` = `customer`.`customer_id`\n"
                + "and\n"
                + "    `customer`.`gender` = 'M'\n"
                + "group by\n"
                + "    `agg_c_14_sales_fact_1997`.`the_year`,\n"
                + "    `agg_c_14_sales_fact_1997`.`quarter`\n"
                + "having\n"
                + "    (sum(`agg_c_14_sales_fact_1997`.`unit_sales`) > 80)\n"
                + "order by\n"
                + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                    ? "    ISNULL(`c0`) ASC, `c0` ASC,\n"
                    + "    ISNULL(`c1`) ASC, `c1` ASC"
                    : "    ISNULL(`agg_c_14_sales_fact_1997`.`the_year`) ASC, `agg_c_14_sales_fact_1997`.`the_year` ASC,\n"
                    + "    ISNULL(`agg_c_14_sales_fact_1997`.`quarter`) ASC, `agg_c_14_sales_fact_1997`.`quarter` ASC");
            final SqlPattern[] patterns = mysqlPattern(sqlMysql);

            // Make sure the tuples list is using the HAVING clause.
            context.getConnectionWithDefaultRole().getCacheControl(null).flushSchemaCache();
            assertQuerySqlOrNot(
                context.getConnectionWithDefaultRole(),
                mdx,
                patterns,
                false,
                true,
                true);
        }
        // Make sure the numbers are right
        assertQueryReturns(
            context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good].[Good Imported Beer]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good].[Good Light Beer]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Pearl].[Pearl Imported Beer]}\n"
            + "Axis #1:\n"
            + "{[Measures].[avgQtrs], [Gender].[Gender].[All Gender]}\n"
            + "{[Measures].[avgQtrs], [Gender].[Gender].[F]}\n"
            + "{[Measures].[avgQtrs], [Gender].[Gender].[M]}\n"
            + "Row #0: 111\n"
            + "Row #0: \n"
            + "Row #0: \n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNativeFilterWithCompoundSlicer_1(Context<?> context) {
    	context.getCatalogCache().clear();
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        final String mdx =
            "with member [measures].[avgQtrs] as 'count(filter([Customers].[Name].Members, [Measures].[Unit Sales] > 0))' "
            + "select [measures].[avgQtrs] on 0 from sales where ( {[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer], [Product].[Food].[Baked Goods].[Bread].[Muffins]} )";
        if (context.getConfigValue(ConfigConstants.ENABLE_NATIVE_FILTER, ConfigConstants.ENABLE_NATIVE_FILTER_DEFAULT_VALUE, Boolean.class)
            && SystemWideProperties.instance().EnableNativeNonEmpty)
        {
            boolean requiresOrderByAlias =
                    getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias();
            final String sqlMysql =
                context.getConfigValue(ConfigConstants.USE_AGGREGATES, ConfigConstants.USE_AGGREGATES_DEFAULT_VALUE ,Boolean.class) == false
                    ? "select\n"
                    + "    `customer`.`country` as `c0`,\n"
                    + "    `customer`.`state_province` as `c1`,\n"
                    + "    `customer`.`city` as `c2`,\n"
                    + "    `customer`.`customer_id` as `c3`,\n"
                    + "    CONCAT(`customer`.`fname`, ' ', `customer`.`lname`) as `c4`,\n"
                    + "    CONCAT(`customer`.`fname`, ' ', `customer`.`lname`) as `c5`,\n"
                    + "    `customer`.`gender` as `c6`,\n"
                    + "    `customer`.`marital_status` as `c7`,\n"
                    + "    `customer`.`education` as `c8`,\n"
                    + "    `customer`.`yearly_income` as `c9`\n"
                    + "from\n"
                    + "    `customer` as `customer`,\n"
                    + "    `sales_fact_1997` as `sales_fact_1997`,\n"
                    + "    `time_by_day` as `time_by_day`,\n"
                    + "    `product_class` as `product_class`,\n"
                    + "    `product` as `product`\n"
                    + "where\n"
                    + "    `sales_fact_1997`.`customer_id` = `customer`.`customer_id`\n"
                    + "and\n"
                    + "    `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`\n"
                    + "and\n"
                    + "    `time_by_day`.`the_year` = 1997\n"
                    + "and\n"
                    + "    `sales_fact_1997`.`product_id` = `product`.`product_id`\n"
                    //
                    + "and\n"
                    + "    `product`.`product_class_id` = `product_class`.`product_class_id`\n"
                    + "and\n"
                    + "    `product_class`.`product_family` in ('Drink', 'Food')\n"
                    + "and\n"
                    + "    `product_class`.`product_department` in ('Alcoholic Beverages', 'Baked Goods')\n"
                    + "and\n"
                    + "    `product_class`.`product_category` in ('Beer and Wine', 'Bread')\n"
                    + "and\n"
                    + "    `product_class`.`product_subcategory` in ('Beer', 'Muffins')\n"
                    + "group by\n"
                    + "    `customer`.`country`,\n"
                    + "    `customer`.`state_province`,\n"
                    + "    `customer`.`city`,\n"
                    + "    `customer`.`customer_id`,\n"
                    + "    CONCAT(`customer`.`fname`, ' ', `customer`.`lname`),\n"
                    + "    `customer`.`gender`,\n"
                    + "    `customer`.`marital_status`,\n"
                    + "    `customer`.`education`,\n"
                    + "    `customer`.`yearly_income`\n"
                    + "having\n"
                    + "    (sum(`sales_fact_1997`.`unit_sales`) > 0)\n"
                    // ^^^^ This is what we are interested in. ^^^^
                    + "order by\n"
                    + (requiresOrderByAlias
                        ? "    ISNULL(`c0`) ASC, `c0` ASC,\n"
                        + "    ISNULL(`c1`) ASC, `c1` ASC,\n"
                        + "    ISNULL(`c2`) ASC, `c2` ASC,\n"
                        + "    ISNULL(`c4`) ASC, `c4` ASC"
                        : "    ISNULL(`customer`.`country`) ASC, `customer`.`country` ASC,\n"
                        + "    ISNULL(`customer`.`state_province`) ASC, `customer`.`state_province` ASC,\n"
                        + "    ISNULL(`customer`.`city`) ASC, `customer`.`city` ASC,\n"
                        + "    ISNULL(CONCAT(`customer`.`fname`, ' ', `customer`.`lname`)) ASC, CONCAT(`customer`.`fname`, ' ', `customer`.`lname`) ASC")

                    : "select\n"
                    + "    `customer`.`country` as `c0`,\n"
                    + "    `customer`.`state_province` as `c1`,\n"
                    + "    `customer`.`city` as `c2`,\n"
                    + "    `customer`.`customer_id` as `c3`,\n"
                    + "    CONCAT(`customer`.`fname`, ' ', `customer`.`lname`) as `c4`,\n"
                    + "    CONCAT(`customer`.`fname`, ' ', `customer`.`lname`) as `c5`,\n"
                    + "    `customer`.`gender` as `c6`,\n"
                    + "    `customer`.`marital_status` as `c7`,\n"
                    + "    `customer`.`education` as `c8`,\n"
                    + "    `customer`.`yearly_income` as `c9`\n"
                    + "from\n"
                    + "    `customer` as `customer`,\n"
                    + "    `agg_c_14_sales_fact_1997` as `agg_c_14_sales_fact_1997`,\n"
                    + "    `product_class` as `product_class`,\n"
                    + "    `product` as `product`\n"
                    + "where\n"
                    + "    `agg_c_14_sales_fact_1997`.`customer_id` = `customer`.`customer_id`\n"
                    + "and\n"
                    + "    `agg_c_14_sales_fact_1997`.`the_year` = 1997\n"
                    + "and\n"
                    + "    `agg_c_14_sales_fact_1997`.`product_id` = `product`.`product_id`\n"
                    + "and\n"
                    + "    `product`.`product_class_id` = `product_class`.`product_class_id`\n"
                    + "and\n"
                    + "    `product_class`.`product_family` in ('Drink', 'Food')\n"
                    + "and\n"
                    + "    `product_class`.`product_department` in ('Alcoholic Beverages', 'Baked Goods')\n"
                    + "and\n"
                    + "    `product_class`.`product_category` in ('Beer and Wine', 'Bread')\n"
                    + "and\n"
                    + "    `product_class`.`product_subcategory` in ('Beer', 'Muffins')\n"
                    + "group by\n"
                    + "    `customer`.`country`,\n"
                    + "    `customer`.`state_province`,\n"
                    + "    `customer`.`city`,\n"
                    + "    `customer`.`customer_id`,\n"
                    + "    CONCAT(`customer`.`fname`, ' ', `customer`.`lname`),\n"
                    + "    `customer`.`gender`,\n"
                    + "    `customer`.`marital_status`,\n"
                    + "    `customer`.`education`,\n"
                    + "    `customer`.`yearly_income`\n"
                    + "having\n"
                    + "    (sum(`agg_c_14_sales_fact_1997`.`unit_sales`) > 0)\n"
                    // ^^^^ This is what we are interested in. ^^^^
                    + "order by\n"
                    + "    ISNULL(`customer`.`country`) ASC, `customer`.`country` ASC,\n"
                    + "    ISNULL(`customer`.`state_province`) ASC, `customer`.`state_province` ASC,\n"
                    + "    ISNULL(`customer`.`city`) ASC, `customer`.`city` ASC,\n"
                    + "    ISNULL(CONCAT(`customer`.`fname`, ' ', `customer`.`lname`)) ASC, CONCAT(`customer`.`fname`, ' ', `customer`.`lname`) ASC";
            final SqlPattern[] patterns = mysqlPattern(sqlMysql);

            // Make sure the tuples list is using the HAVING clause.
            assertQuerySqlOrNot(
                context.getConnectionWithDefaultRole(),
                mdx,
                patterns,
                false,
                true,
                true);
        }
        // Make sure the numbers are right
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]}\n"
            + "{[Product].[Product].[Food].[Baked Goods].[Bread].[Muffins]}\n"
            + "Axis #1:\n"
            + "{[Measures].[avgQtrs]}\n"
            + "Row #0: 1,281\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNativeFilterWithCompoundSlicer_2(Context<?> context) {
        verifySameNativeAndNot(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Measures].[TotalVal] AS 'Aggregate(Filter({[Store].[Store City].members}, ([Measures].[Unit Sales] > 1000 OR ( [Measures].[Unit Sales] > 40 AND [Store].[Store City].CurrentMember.Name = \"San Francisco\" ) ) ) )'\n"
            + "SELECT [Measures].[TotalVal] ON 0, [Product].[All Products].Children on 1 FROM [Sales] WHERE {[Time].[1997].[Q1],[Time].[1997].[Q2]}",
            "Failed.");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNativeFilterWithCompoundSlicer_3(Context<?> context) {
        verifySameNativeAndNot(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Measures].[TotalVal] AS 'Aggregate(Filter({[Store].[Store City].members}, [Measures].[Unit Sales] > 1000 ) )'\n"
            + "SELECT [Measures].[TotalVal] ON 0, [Product].[All Products].Children on 1 FROM [Sales] WHERE {[Time].[1997].[Q1],[Time].[1997].[Q2]}",
            "Failed.");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNativeFilterWithCompoundSlicer_4(Context<?> context) {
        verifySameNativeAndNot(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Measures].[TotalVal] AS 'Aggregate(Filter({[Store].[Store City].members}, ([Measures].[Unit Sales] > 1000 OR ( [Measures].[Unit Sales] > 500 AND [Store].[Store City].CurrentMember.Name = \"San Francisco\" ) ) ) )'\n"
            + "SELECT [Measures].[TotalVal] ON 0, [Product].[All Products].Children on 1 FROM [Sales] WHERE {[Time].[1997].[Q1],[Time].[1997].[Q2]}",
            "Failed.");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNativeFilterWithCompoundSlicerDifferentProducts(Context<?> context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member measures.avgQtrs as 'count(filter(Customers.[Name].members, [Unit Sales] > 0))' "
            + "select measures.avgQtrs on 0 from sales where ( {[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer], [Product].[Food].[Baked Goods].[Bread].[Muffins]} )",
            "Axis #0:\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]}\n"
            + "{[Product].[Product].[Food].[Baked Goods].[Bread].[Muffins]}\n"
            + "Axis #1:\n"
            + "{[Measures].[avgQtrs]}\n"
            + "Row #0: 1,281\n");
    }
}
