/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2021 Hitachi Vantara.  All rights reserved.
*/
package mondrian.olap.fun;

import static org.opencube.junit5.TestUtil.withSchema;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.LevelType;
import org.eclipse.daanse.rolap.mapping.instance.rec.complex.foodmart.FoodmartMappingSupplier;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.ExplicitHierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SumMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TimeDimensionMappingImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

/**
 * Tests the CachedExists function.
 *
 * @author Benny Chow
 */
class CachedExistsTest{


	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testEducationLevelSubtotals(Context<?> context) {
    String query =
        "WITH "
            + "SET [*NATIVE_CJ_SET] AS 'NONEMPTYCROSSJOIN([*BASE_MEMBERS__Education Level_],[*BASE_MEMBERS__Product_])' "
            + "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],[Education Level].CURRENTMEMBER.ORDERKEY,BASC,[Measures].[*SORTED_MEASURE],BDESC)' "
            + "SET [*BASE_MEMBERS__Education Level_] AS '{[Education Level].[All Education Levels].[Bachelors Degree],[Education Level].[All Education Levels].[Graduate Degree]}' "
            + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0]}' "
            + "SET [*BASE_MEMBERS__Product_] AS '{[Product].[All Products].[Drink],[Product].[All Products].[Food]}' "
            + "SET [*CJ_ROW_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Education Level].CURRENTMEMBER,[Product].CURRENTMEMBER)})' "
            + "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=500 "
            + "MEMBER [Measures].[*SORTED_MEASURE] AS '([Measures].[*FORMATTED_MEASURE_0])', SOLVE_ORDER=400 "
            + "MEMBER [Product].[*TOTAL_MEMBER_SEL~SUM] AS 'SUM(CachedExists([*CJ_ROW_AXIS], ([Education Level].CURRENTMEMBER), \"*CJ_ROW_AXIS\"))', SOLVE_ORDER=99 "
            + "SELECT " + "[*BASE_MEMBERS__Measures_] ON COLUMNS " + ", NON EMPTY "
            + "UNION(CROSSJOIN(GENERATE([*CJ_ROW_AXIS], {([Education Level].CURRENTMEMBER)}),{[Product].[*TOTAL_MEMBER_SEL~SUM]}),[*SORTED_ROW_AXIS]) ON ROWS "
            + "FROM [Sales]";
    String expected =
        "Axis #0:\n" + "{}\n" + "Axis #1:\n" + "{[Measures].[*FORMATTED_MEASURE_0]}\n" + "Axis #2:\n"
            + "{[Education Level].[Education Level].[Bachelors Degree], [Product].[Product].[*TOTAL_MEMBER_SEL~SUM]}\n"
            + "{[Education Level].[Education Level].[Graduate Degree], [Product].[Product].[*TOTAL_MEMBER_SEL~SUM]}\n"
            + "{[Education Level].[Education Level].[Bachelors Degree], [Product].[Product].[Food]}\n"
            + "{[Education Level].[Education Level].[Bachelors Degree], [Product].[Product].[Drink]}\n"
            + "{[Education Level].[Education Level].[Graduate Degree], [Product].[Product].[Food]}\n"
            + "{[Education Level].[Education Level].[Graduate Degree], [Product].[Product].[Drink]}\n" + "Row #0: 55,788\n" + "Row #1: 12,580\n"
            + "Row #2: 49,365\n" + "Row #3: 6,423\n" + "Row #4: 11,255\n" + "Row #5: 1,325\n";
    TestUtil.assertQueryReturns( context.getConnectionWithDefaultRole(), query, expected );
  }

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testProductFamilySubtotals(Context<?> context) {
    String query =
        "WITH\r\n"
            + "SET [*NATIVE_CJ_SET] AS 'FILTER(FILTER([Product].[Product Department].MEMBERS,ANCESTOR([Product].CURRENTMEMBER, [Product].[Product Family]) IN {[Product].[All Products].[Drink],[Product].[All Products].[Non-Consumable]}), NOT ISEMPTY ([Measures].[Unit Sales]))'\r\n"
            + "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],ANCESTOR([Product].CURRENTMEMBER, [Product].[Product Family]).ORDERKEY,BASC,[Product].CURRENTMEMBER.ORDERKEY,BASC)'\r\n"
            + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0]}'\r\n"
            + "SET [*BASE_MEMBERS__Product_] AS 'FILTER([Product].[Product Department].MEMBERS,ANCESTOR([Product].CURRENTMEMBER, [Product].[Product Family]) IN {[Product].[All Products].[Drink],[Product].[All Products].[Non-Consumable]})'\r\n"
            + "SET [*CJ_ROW_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Product].CURRENTMEMBER)})'\r\n"
            + "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=500\r\n"
            + "MEMBER [Product].[All Products].[Drink].[*TOTAL_MEMBER_SEL~SUM] AS 'SUM(CachedExists([*CJ_ROW_AXIS], ([Product].[All Products].[Drink]), \"*CJ_ROW_AXIS\"))', SOLVE_ORDER=100\r\n"
            + "MEMBER [Product].[All Products].[Non-Consumable].[*TOTAL_MEMBER_SEL~SUM] AS 'SUM(CachedExists([*CJ_ROW_AXIS], ([Product].[All Products].[Non-Consumable]), \"*CJ_ROW_AXIS\"))', SOLVE_ORDER=100\r\n"
            + "SELECT\r\n" + "[*BASE_MEMBERS__Measures_] ON COLUMNS\r\n" + ", NON EMPTY\r\n"
            + "UNION({[Product].[All Products].[Drink].[*TOTAL_MEMBER_SEL~SUM], [Product].[All Products].[Non-Consumable].[*TOTAL_MEMBER_SEL~SUM]},[*SORTED_ROW_AXIS]) ON ROWS\r\n"
            + "FROM [Sales]";
    String expected =
        "Axis #0:\n" + "{}\n" + "Axis #1:\n" + "{[Measures].[*FORMATTED_MEASURE_0]}\n" + "Axis #2:\n"
            + "{[Product].[Product].[Drink].[*TOTAL_MEMBER_SEL~SUM]}\n"
            + "{[Product].[Product].[Non-Consumable].[*TOTAL_MEMBER_SEL~SUM]}\n" + "{[Product].[Product].[Drink].[Alcoholic Beverages]}\n"
            + "{[Product].[Product].[Drink].[Beverages]}\n" + "{[Product].[Product].[Drink].[Dairy]}\n"
            + "{[Product].[Product].[Non-Consumable].[Carousel]}\n" + "{[Product].[Product].[Non-Consumable].[Checkout]}\n"
            + "{[Product].[Product].[Non-Consumable].[Health and Hygiene]}\n" + "{[Product].[Product].[Non-Consumable].[Household]}\n"
            + "{[Product].[Product].[Non-Consumable].[Periodicals]}\n" + "Row #0: 24,597\n" + "Row #1: 50,236\n"
            + "Row #2: 6,838\n" + "Row #3: 13,573\n" + "Row #4: 4,186\n" + "Row #5: 841\n" + "Row #6: 1,779\n"
            + "Row #7: 16,284\n" + "Row #8: 27,038\n" + "Row #9: 4,294\n";
    TestUtil.assertQueryReturns( context.getConnectionWithDefaultRole(), query, expected );
  }

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testProductFamilyProductDepartmentSubtotals(Context<?> context) {
    String query =
        "WITH\r\n"
            + "SET [*NATIVE_CJ_SET] AS 'NONEMPTYCROSSJOIN([*BASE_MEMBERS__Product_],[*BASE_MEMBERS__Gender_])'\r\n"
            + "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],ANCESTOR([Product].CURRENTMEMBER, [Product].[Product].[Product Family]).ORDERKEY,BASC,[Product].[Product].CURRENTMEMBER.ORDERKEY,BASC,[Gender].[Gender].CURRENTMEMBER.ORDERKEY,BASC)'\r\n"
            + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0]}'\r\n"
            + "SET [*BASE_MEMBERS__Gender_] AS '[Gender].[Gender].[Gender].MEMBERS'\r\n"
            + "SET [*BASE_MEMBERS__Product_] AS 'FILTER([Product].[Product].[Product Department].MEMBERS,(ANCESTOR([Product].[Product].CURRENTMEMBER, [Product].[Product].[Product Family]) IN {[Product].[Product].[All Products].[Drink],[Product].[Product].[All Products].[Non-Consumable]}) AND ([Product].[Product].CURRENTMEMBER IN {[Product].[Product].[All Products].[Drink].[Beverages],[Product].[Product].[All Products].[Drink].[Dairy],[Product].[Product].[All Products].[Non-Consumable].[Periodicals]}))'\r\n"
            + "SET [*CJ_ROW_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Product].CURRENTMEMBER,[Gender].CURRENTMEMBER)})'\r\n"
            + "MEMBER [Gender].[Gender].[*DEFAULT_MEMBER] AS '[Gender].[Gender].DEFAULTMEMBER', SOLVE_ORDER=-400\r\n"
            + "MEMBER [Gender].[Gender].[*TOTAL_MEMBER_SEL~SUM] AS 'SUM(CachedExists([*CJ_ROW_AXIS], ([Product].[Product].CURRENTMEMBER), \"*CJ_ROW_AXIS\"))', SOLVE_ORDER=99\r\n"
            + "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=500\r\n"
            + "MEMBER [Product].[Product].[All Products].[Drink].[*TOTAL_MEMBER_SEL~SUM] AS 'SUM(CachedExists([*CJ_ROW_AXIS], ([Product].[Product].[All Products].[Drink]), \"*CJ_ROW_AXIS\"))', SOLVE_ORDER=100\r\n"
            + "MEMBER [Product].[Product].[All Products].[Non-Consumable].[*TOTAL_MEMBER_SEL~SUM] AS 'SUM(CachedExists([*CJ_ROW_AXIS], ([Product].[Product].[All Products].[Non-Consumable]), \"*CJ_ROW_AXIS\"))', SOLVE_ORDER=100\r\n"
            + "SELECT\r\n" + "[*BASE_MEMBERS__Measures_] ON COLUMNS\r\n" + ", NON EMPTY\r\n"
            + "UNION(CROSSJOIN(GENERATE([*CJ_ROW_AXIS], {([Product].[Product].CURRENTMEMBER)}),{[Gender].[Gender].[*TOTAL_MEMBER_SEL~SUM]}),UNION(CROSSJOIN({[Product].[Product].[All Products].[Drink].[*TOTAL_MEMBER_SEL~SUM], [Product].[Product].[All Products].[Non-Consumable].[*TOTAL_MEMBER_SEL~SUM]},{([Gender].[Gender].[*DEFAULT_MEMBER])}),[*SORTED_ROW_AXIS])) ON ROWS\r\n"
            + "FROM [Sales]";
    String expected =
        "Axis #0:\n" + "{}\n" + "Axis #1:\n" + "{[Measures].[*FORMATTED_MEASURE_0]}\n" + "Axis #2:\n"
            + "{[Product].[Product].[Drink].[Beverages], [Gender].[Gender].[*TOTAL_MEMBER_SEL~SUM]}\n"
            + "{[Product].[Product].[Drink].[Dairy], [Gender].[Gender].[*TOTAL_MEMBER_SEL~SUM]}\n"
            + "{[Product].[Product].[Non-Consumable].[Periodicals], [Gender].[Gender].[*TOTAL_MEMBER_SEL~SUM]}\n"
            + "{[Product].[Product].[Drink].[*TOTAL_MEMBER_SEL~SUM], [Gender].[Gender].[*DEFAULT_MEMBER]}\n"
            + "{[Product].[Product].[Non-Consumable].[*TOTAL_MEMBER_SEL~SUM], [Gender].[Gender].[*DEFAULT_MEMBER]}\n"
            + "{[Product].[Product].[Drink].[Beverages], [Gender].[Gender].[F]}\n" + "{[Product].[Product].[Drink].[Beverages], [Gender].[Gender].[M]}\n"
            + "{[Product].[Product].[Drink].[Dairy], [Gender].[Gender].[F]}\n" + "{[Product].[Product].[Drink].[Dairy], [Gender].[Gender].[M]}\n"
            + "{[Product].[Product].[Non-Consumable].[Periodicals], [Gender].[Gender].[F]}\n"
            + "{[Product].[Product].[Non-Consumable].[Periodicals], [Gender].[Gender].[M]}\n" + "Row #0: 13,573\n" + "Row #1: 4,186\n"
            + "Row #2: 4,294\n" + "Row #3: 17,759\n" + "Row #4: 4,294\n" + "Row #5: 6,776\n" + "Row #6: 6,797\n"
            + "Row #7: 1,987\n" + "Row #8: 2,199\n" + "Row #9: 2,168\n" + "Row #10: 2,126\n";
    TestUtil.assertQueryReturns( context.getConnectionWithDefaultRole(), query, expected );
  }

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testRowColumSubtotals(Context<?> context) {
    String query =
        "WITH\r\n"
            + "SET [*NATIVE_CJ_SET] AS 'NONEMPTYCROSSJOIN([*BASE_MEMBERS__Time_],NONEMPTYCROSSJOIN([*BASE_MEMBERS__Product_],[*BASE_MEMBERS__Gender_]))'\r\n"
            + "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],[Product].[Product].CURRENTMEMBER.ORDERKEY,BASC,[Gender].[Gender].CURRENTMEMBER.ORDERKEY,BASC)'\r\n"
            + "SET [*SORTED_COL_AXIS] AS 'ORDER([*CJ_COL_AXIS],ANCESTOR([Time].[Time].CURRENTMEMBER, [Time].[Time].[Year]).ORDERKEY,BASC,[Time].[Time].CURRENTMEMBER.ORDERKEY,BASC,[Measures].CURRENTMEMBER.ORDERKEY,BASC)'\r\n"
            + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0]}'\r\n"
            + "SET [*BASE_MEMBERS__Gender_] AS '[Gender].[Gender].[Gender].MEMBERS'\r\n"
            + "SET [*CJ_COL_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Time].[Time].CURRENTMEMBER)})'\r\n"
            + "SET [*BASE_MEMBERS__Product_] AS '{[Product].[Product].[All Products].[Drink],[Product].[Product].[All Products].[Non-Consumable]}'\r\n"
            + "SET [*BASE_MEMBERS__Time_] AS '{[Time].[Time].[1997].[Q1],[Time].[Time].[1997].[Q2]}'\r\n"
            + "SET [*CJ_ROW_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Product].[Product].CURRENTMEMBER,[Gender].[Gender].CURRENTMEMBER)})'\r\n"
            + "MEMBER [Gender].[Gender].[*TOTAL_MEMBER_SEL~SUM] AS 'SUM(CachedExists([*CJ_ROW_AXIS], ([Product].[Product].CURRENTMEMBER), \"*CJ_ROW_AXIS\"))', SOLVE_ORDER=99\r\n"
            + "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=500\r\n"
            + "MEMBER [Time].[Time].[1997].[*TOTAL_MEMBER_SEL~SUM] AS 'SUM(CachedExists([*CJ_COL_AXIS], ([Time].[Time].[1997]), \"*CJ_COL_AXIS\"))', SOLVE_ORDER=98\r\n"
            + "SELECT\r\n"
            + "UNION(CROSSJOIN({[Time].[Time].[1997].[*TOTAL_MEMBER_SEL~SUM]},[*BASE_MEMBERS__Measures_]),CROSSJOIN([*SORTED_COL_AXIS],[*BASE_MEMBERS__Measures_])) ON COLUMNS\r\n"
            + ", NON EMPTY\r\n"
            + "UNION(CROSSJOIN(GENERATE([*CJ_ROW_AXIS], {([Product].[Product].CURRENTMEMBER)}),{[Gender].[Gender].[*TOTAL_MEMBER_SEL~SUM]}),[*SORTED_ROW_AXIS]) ON ROWS\r\n"
            + "FROM [Sales]";
    String expected =
        "Axis #0:\n" + "{}\n" + "Axis #1:\n"
            + "{[Time].[Time].[1997].[*TOTAL_MEMBER_SEL~SUM], [Measures].[*FORMATTED_MEASURE_0]}\n"
            + "{[Time].[Time].[1997].[Q1], [Measures].[*FORMATTED_MEASURE_0]}\n"
            + "{[Time].[Time].[1997].[Q2], [Measures].[*FORMATTED_MEASURE_0]}\n" + "Axis #2:\n"
            + "{[Product].[Product].[Drink], [Gender].[Gender].[*TOTAL_MEMBER_SEL~SUM]}\n"
            + "{[Product].[Product].[Non-Consumable], [Gender].[Gender].[*TOTAL_MEMBER_SEL~SUM]}\n"
            + "{[Product].[Product].[Drink], [Gender].[Gender].[F]}\n" + "{[Product].[Product].[Drink], [Gender].[Gender].[M]}\n"
            + "{[Product].[Product].[Non-Consumable], [Gender].[Gender].[F]}\n" + "{[Product].[Product].[Non-Consumable], [Gender].[Gender].[M]}\n"
            + "Row #0: 11,871\n" + "Row #0: 5,976\n" + "Row #0: 5,895\n" + "Row #1: 24,396\n" + "Row #1: 12,506\n"
            + "Row #1: 11,890\n" + "Row #2: 5,806\n" + "Row #2: 2,934\n" + "Row #2: 2,872\n" + "Row #3: 6,065\n"
            + "Row #3: 3,042\n" + "Row #3: 3,023\n" + "Row #4: 11,997\n" + "Row #4: 6,144\n" + "Row #4: 5,853\n"
            + "Row #5: 12,399\n" + "Row #5: 6,362\n" + "Row #5: 6,037\n";
    TestUtil.assertQueryReturns( context.getConnectionWithDefaultRole(), query, expected );
  }

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testProductFamilyDisplayMember(Context<?> context) {
    String query =
        "WITH\r\n" +
        "SET [*NATIVE_CJ_SET] AS 'NONEMPTYCROSSJOIN([*BASE_MEMBERS__Product_],[*BASE_MEMBERS__Gender_])'\r\n" +
        "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],ANCESTOR([Product].[Product].CURRENTMEMBER, [Product].[Product].[Product Family]).ORDERKEY,BASC,[Gender].CURRENTMEMBER.ORDERKEY,BASC)'\r\n" +
        "SET [*NATIVE_MEMBERS__Product_] AS 'GENERATE([*NATIVE_CJ_SET], {[Product].[Product].CURRENTMEMBER})'\r\n" +
        "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0]}'\r\n" +
        "SET [*BASE_MEMBERS__Gender_] AS '[Gender].[Gender].[Gender].MEMBERS'\r\n" +
        "SET [*BASE_MEMBERS__Product_] AS 'FILTER([Product].[Product].[Product Category].MEMBERS,(ANCESTOR([Product].[Product].CURRENTMEMBER, [Product].[Product].[Product Family]) IN {[Product].[Product].[All Products].[Drink],[Product].[Product].[All Products].[Non-Consumable]}) AND ([Product].[Product].CURRENTMEMBER IN {[Product].[Product].[All Products].[Non-Consumable].[Household].[Candles],[Product].[Product].[All Products].[Drink].[Dairy].[Dairy],[Product].[Product].[All Products].[Non-Consumable].[Periodicals].[Magazines],[Product].[Product].[All Products].[Drink].[Beverages].[Pure Juice Beverages]}))'\r\n" +
        "SET [*CJ_ROW_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {(ANCESTOR([Product].[Product].CURRENTMEMBER, [Product].[Product].[Product Family]).CALCULATEDCHILD(\"*DISPLAY_MEMBER\"),[Gender].[Gender].CURRENTMEMBER)})'\r\n" +
        "MEMBER [Gender].[Gender].[*TOTAL_MEMBER_SEL~SUM] AS 'SUM(CachedExists([*CJ_ROW_AXIS], ([Product].[Product].CURRENTMEMBER), \"*CJ_ROW_AXIS\" ))', SOLVE_ORDER=99\r\n" +
        "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=500\r\n" +
        "MEMBER [Product].[Product].[Drink].[*DISPLAY_MEMBER] AS 'AGGREGATE (FILTER([*NATIVE_MEMBERS__Product_],ANCESTOR([Product].[Product].CURRENTMEMBER, [Product].[Product].[Product Family]) IS [Product].[Product].[All Products].[Drink]))', SOLVE_ORDER=-100\r\n" +
        "MEMBER [Product].[Product].[Non-Consumable].[*DISPLAY_MEMBER] AS 'AGGREGATE (FILTER([*NATIVE_MEMBERS__Product_],ANCESTOR([Product].[Product].CURRENTMEMBER, [Product].[Product].[Product Family]) IS [Product].[Product].[All Products].[Non-Consumable]))', SOLVE_ORDER=-100\r\n" +
        "SELECT\r\n" +
        "[*BASE_MEMBERS__Measures_] ON COLUMNS\r\n" +
        ", NON EMPTY\r\n" +
        "UNION(CROSSJOIN(GENERATE([*CJ_ROW_AXIS], {(ANCESTOR([Product].CURRENTMEMBER, [Product].[Product].[Product Family]))}),{[Gender].[Gender].[*TOTAL_MEMBER_SEL~SUM]}),[*SORTED_ROW_AXIS]) ON ROWS\r\n" +
        "FROM [Sales]";
    String expected =
        "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Product].[Product].[Drink], [Gender].[Gender].[*TOTAL_MEMBER_SEL~SUM]}\n"
            + "{[Product].[Product].[Non-Consumable], [Gender].[Gender].[*TOTAL_MEMBER_SEL~SUM]}\n"
            + "{[Product].[Product].[Drink].[*DISPLAY_MEMBER], [Gender].[Gender].[F]}\n"
            + "{[Product].[Product].[Drink].[*DISPLAY_MEMBER], [Gender].[Gender].[M]}\n"
            + "{[Product].[Product].[Non-Consumable].[*DISPLAY_MEMBER], [Gender].[Gender].[F]}\n"
            + "{[Product].[Product].[Non-Consumable].[*DISPLAY_MEMBER], [Gender].[Gender].[M]}\n"
            + "Row #0: 7,582\n"
            + "Row #1: 5,109\n"
            + "Row #2: 3,690\n"
            + "Row #3: 3,892\n"
            + "Row #4: 2,607\n"
            + "Row #5: 2,502\n";
    TestUtil.assertQueryReturns( context.getConnectionWithDefaultRole(), query, expected );
  }

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testTop10Customers(Context<?> context) {
    String query =
        "WITH\r\n" +
        "SET [*NATIVE_CJ_SET] AS 'NONEMPTYCROSSJOIN([*BASE_MEMBERS__Customers_],NONEMPTYCROSSJOIN([*BASE_MEMBERS__Product_],[*BASE_MEMBERS__Store_]))'\r\n" +
        "SET [*METRIC_CJ_SET] AS 'FILTER([*NATIVE_CJ_SET],[Measures].[*TOP_Unit Sales_SEL~SUM] <= 10)'\r\n" +
        "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],[Customers].CURRENTMEMBER.ORDERKEY,BASC,ANCESTOR([Customers].CURRENTMEMBER,[Customers].[City]).ORDERKEY,BASC,[Product].CURRENTMEMBER.ORDERKEY,BASC,[Measures].[*SORTED_MEASURE],BDESC)'\r\n" +
        "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0]}'\r\n" +
        "SET [*BASE_MEMBERS__Store_] AS '[Store].[Store Country].MEMBERS'\r\n" +
        "SET [*BASE_MEMBERS__Customers_] AS '[Customers].[Name].MEMBERS'\r\n" +
        "SET [*TOP_SET] AS 'ORDER(GENERATE([*NATIVE_CJ_SET],{[Customers].CURRENTMEMBER}),([Measures].[Unit Sales],[Education Level].[*TOPBOTTOM_CTX_SET_SUM]),BDESC)'\r\n" +
        "SET [*BASE_MEMBERS__Product_] AS '{[Product].[All Products].[Drink],[Product].[All Products].[Food]}'\r\n" +
        "SET [*CJ_ROW_AXIS] AS 'GENERATE([*METRIC_CJ_SET], {([Customers].CURRENTMEMBER,[Product].CURRENTMEMBER,[Store].CURRENTMEMBER)})'\r\n" +
        "MEMBER [Education Level].[*TOPBOTTOM_CTX_SET_SUM] AS 'SUM(CachedExists([*NATIVE_CJ_SET],([Customers].CURRENTMEMBER), \"*NATIVE_CJ_SET\"))', SOLVE_ORDER=100\r\n" +
        "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=500\r\n" +
        "MEMBER [Measures].[*SORTED_MEASURE] AS '([Measures].[*FORMATTED_MEASURE_0])', SOLVE_ORDER=400\r\n" +
        "MEMBER [Measures].[*TOP_Unit Sales_SEL~SUM] AS 'RANK([Customers].CURRENTMEMBER,[*TOP_SET])', SOLVE_ORDER=400\r\n" +
        "SELECT\r\n" +
        "[*BASE_MEMBERS__Measures_] ON COLUMNS\r\n" +
        ", NON EMPTY\r\n" +
        "[*SORTED_ROW_AXIS] ON ROWS\r\n" +
        "FROM [Sales]";
    String expected =
        "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Joann Mramor], [Product].[Product].[Drink], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Joann Mramor], [Product].[Product].[Food], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Jack Zucconi], [Product].[Product].[Drink], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Jack Zucconi], [Product].[Product].[Food], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Mary Francis Benigar], [Product].[Product].[Drink], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Mary Francis Benigar], [Product].[Product].[Food], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Kristin Miller], [Product].[Product].[Drink], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Kristin Miller], [Product].[Product].[Food], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[James Horvat], [Product].[Product].[Drink], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[James Horvat], [Product].[Product].[Food], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Frank Darrell], [Product].[Product].[Drink], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Frank Darrell], [Product].[Product].[Food], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Ida Rodriguez], [Product].[Product].[Drink], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Ida Rodriguez], [Product].[Product].[Food], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Matt Bellah], [Product].[Product].[Drink], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Matt Bellah], [Product].[Product].[Food], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Emily Barela], [Product].[Product].[Drink], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Emily Barela], [Product].[Product].[Food], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Wildon Cameron], [Product].[Product].[Drink], [Store].[Store].[USA]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Wildon Cameron], [Product].[Product].[Food], [Store].[Store].[USA]}\n"
            + "Row #0: 57\n"
            + "Row #1: 267\n"
            + "Row #2: 26\n"
            + "Row #3: 279\n"
            + "Row #4: 37\n"
            + "Row #5: 390\n"
            + "Row #6: 16\n"
            + "Row #7: 294\n"
            + "Row #8: 40\n"
            + "Row #9: 344\n"
            + "Row #10: 49\n"
            + "Row #11: 252\n"
            + "Row #12: 38\n"
            + "Row #13: 319\n"
            + "Row #14: 36\n"
            + "Row #15: 273\n"
            + "Row #16: 26\n"
            + "Row #17: 291\n"
            + "Row #18: 47\n"
            + "Row #19: 319\n";
    TestUtil.assertQueryReturns( context.getConnectionWithDefaultRole(), query, expected );
  }

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testTop1CustomersWithColumnLevel(Context<?> context) {
    String query =
        "WITH\n"
            + "SET [*NATIVE_CJ_SET] AS 'NONEMPTYCROSSJOIN([*BASE_MEMBERS__Time_],NONEMPTYCROSSJOIN([*BASE_MEMBERS__Product_],NONEMPTYCROSSJOIN([*BASE_MEMBERS__Education Level_],[*BASE_MEMBERS__Customers_])))'\n"
            + "SET [*METRIC_CJ_SET] AS 'FILTER([*NATIVE_CJ_SET],[Measures].[*TOP_Unit Sales_SEL~SUM] <= 1)'\n"
            + "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],[Product].[Product].CURRENTMEMBER.ORDERKEY,BASC,[Education Level].[Education Level].CURRENTMEMBER.ORDERKEY,BASC,[Measures].[*SORTED_MEASURE],BDESC)'\n"
            + "SET [*NATIVE_MEMBERS__Time_] AS 'GENERATE([*NATIVE_CJ_SET], {[Time].[Time].CURRENTMEMBER})'\n"
            + "SET [*SORTED_COL_AXIS] AS 'ORDER([*CJ_COL_AXIS],[Time].[Time].CURRENTMEMBER.ORDERKEY,BASC,[Measures].CURRENTMEMBER.ORDERKEY,BASC)'\n"
            + "SET [*BASE_MEMBERS__Education Level_] AS '{[Education Level].[Education Level].[All Education Levels].[Bachelors Degree]}'\n"
            + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0]}'\n"
            + "SET [*BASE_MEMBERS__Customers_] AS '[Customers].[Customers].[Name].MEMBERS'\n"
            + "SET [*CJ_COL_AXIS] AS 'GENERATE([*METRIC_CJ_SET], {([Time].[Time].CURRENTMEMBER)})'\n"
            + "SET [*BASE_MEMBERS__Product_] AS '{[Product].[Product].[All Products].[Drink]}'\n"
            + "SET [*BASE_MEMBERS__Time_] AS '[Time].[Time].[Year].MEMBERS'\n"
            + "SET [*CJ_ROW_AXIS] AS 'GENERATE([*METRIC_CJ_SET], {([Product].[Product].CURRENTMEMBER,[Education Level].[Education Level].CURRENTMEMBER,[Customers].[Customers].CURRENTMEMBER)})'\n"
            + "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=500\n"
            + "MEMBER [Measures].[*SORTED_MEASURE] AS '([Measures].[*FORMATTED_MEASURE_0],[Time].[Time].[*CTX_MEMBER_SEL~SUM])', SOLVE_ORDER=400\n"
            + "MEMBER [Measures].[*TOP_Unit Sales_SEL~SUM] AS 'RANK([Customers].[Customers].CURRENTMEMBER,ORDER(GENERATE(CACHEDEXISTS([*NATIVE_CJ_SET],([Product].[Product].CURRENTMEMBER, [Education Level].[Education Level].CURRENTMEMBER),\"[*NATIVE_CJ_SET]\"),{[Customers].[Customers].CURRENTMEMBER}),([Measures].[Unit Sales],[Product].[Product].CURRENTMEMBER,[Education Level].[Education Level].CURRENTMEMBER,[Time].[Time].[*CTX_MEMBER_SEL~SUM]),BDESC))', SOLVE_ORDER=400\n"
            + "MEMBER [Time].[Time].[*CTX_MEMBER_SEL~SUM] AS 'SUM([*NATIVE_MEMBERS__Time_])', SOLVE_ORDER=97\n" + "SELECT\n"
            + "CROSSJOIN([*SORTED_COL_AXIS],[*BASE_MEMBERS__Measures_]) ON COLUMNS\n" + ", NON EMPTY\n"
            + "[*SORTED_ROW_AXIS] ON ROWS\n" + "FROM [Sales]";
    String expected =
        "Axis #0:\n" + "{}\n" + "Axis #1:\n" + "{[Time].[Time].[1997], [Measures].[*FORMATTED_MEASURE_0]}\n" + "Axis #2:\n"
            + "{[Product].[Product].[Drink], [Education Level].[Education Level].[Bachelors Degree], [Customers].[Customers].[USA].[WA].[Spokane].[Wildon Cameron]}\n"
            + "Row #0: 47\n";
    TestUtil.assertQueryReturns( context.getConnectionWithDefaultRole(), query, expected );
  }

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class,dataloader = FastFoodmardDataLoader.class )
    void testMondrian2704(Context<?> context) {
    String cube=    "<Cube name=\"Alternate Sales\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "<Dimension name=\"Time\" type=\"TimeDimension\" foreignKey=\"time_id\">\n" +
            "    <Hierarchy name=\"Time\" hasAll=\"true\" primaryKey=\"time_id\">\n" +
            "      <Table name=\"time_by_day\"/>\n" +
            "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n" +
            "          levelType=\"TimeYears\"/>\n" +
            "    </Hierarchy>\n" +
            "    <Hierarchy hasAll=\"true\" name=\"Weekly\" primaryKey=\"time_id\">\n" +
            "      <Table name=\"time_by_day\"/>\n" +
            "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n" +
            "          levelType=\"TimeYears\"/>\n" +
            "    </Hierarchy>\n" +
            "    <Hierarchy hasAll=\"true\" name=\"Weekly2\" primaryKey=\"time_id\">\n" +
            "      <Table name=\"time_by_day\"/>\n" +
            "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n" +
            "          levelType=\"TimeYears\"/>\n" +
            "    </Hierarchy>\n" +
            "  </Dimension>"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" formatString=\"Standard\"/>\n"
            + "</Cube>";

    /*
    PropertyUpdater p=new PropertyUpdater() {

    	@Override
		public PropertyList update(PropertyList propertyList) {

			String schemaOld = propertyList.get(RolapConnectionProperties.CatalogContent.name());
			String schema = SchemaUtil.getSchema(schemaOld, null, cube, null, null, null, null);
			propertyList.put(RolapConnectionProperties.CatalogContent.name(), schema);
			return propertyList;
		}

	};
		((BaseContext) context).update(p);
    */
        class TestMondrian2704Modifier extends PojoMappingModifier {

            public TestMondrian2704Modifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends CubeMapping> catalogCubes(CatalogMapping schema) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.catalogCubes(schema));
                result.add(PhysicalCubeMappingImpl.builder()
                    .withName("Alternate Sales")
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(DimensionConnectorMappingImpl.builder()
                    		.withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                    		.withOverrideDimensionName("Time")
                    		.withDimension(TimeDimensionMappingImpl.builder()
                    				.withName("Time")
                    				.withHierarchies(List.of(
                    					ExplicitHierarchyMappingImpl.builder()
                                            .withName("Time")
                                            .withHasAll(true)
                                            .withPrimaryKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY)
                                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.TIME_BY_DAY_TABLE).build())
                                            .withLevels(List.of(
                                                LevelMappingImpl.builder()
                                                    .withName("Year")
                                                    .withColumn(FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY)
                                                    .withType(InternalDataType.NUMERIC)
                                                    .withUniqueMembers(true)
                                                    .withLevelType(LevelType.TIME_YEARS)
                                                    .build()
                                            ))
                                            .build(),
                    					ExplicitHierarchyMappingImpl.builder()
                                            .withName("Weekly")
                                            .withHasAll(true)
                                            .withPrimaryKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY)
                                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.TIME_BY_DAY_TABLE).build())
                                            .withLevels(List.of(
                                                LevelMappingImpl.builder()
                                                    .withName("Year")
                                                    .withColumn(FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY)
                                                    .withType(InternalDataType.NUMERIC)
                                                    .withUniqueMembers(true)
                                                    .withLevelType(LevelType.TIME_YEARS)
                                                    .build()
                                            ))
                                            .build(),
                    					ExplicitHierarchyMappingImpl.builder()
                                            .withName("Weekly2")
                                            .withHasAll(true)
                                            .withPrimaryKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY)
                                            .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.TIME_BY_DAY_TABLE).build())
                                            .withLevels(List.of(
                                                LevelMappingImpl.builder()
                                                    .withName("Year")
                                                    .withColumn(FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY)
                                                    .withType(InternalDataType.NUMERIC)
                                                    .withUniqueMembers(true)
                                                    .withLevelType(LevelType.TIME_YEARS)
                                                    .build()
                                            ))
                                            .build()
                                            )
                    						)
                    				.build())
                    		.build()))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                            SumMeasureMappingImpl.builder()
                            .withName("Unit Sales")
                            .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                            .withFormatString("Standard")
                            .build()
                    		)).build()))
                    .build());
                return result;
            }
        }
        withSchema(context, TestMondrian2704Modifier::new);



    // Verifies second arg of CachedExists uses a tuple type
    	TestUtil.assertQueryReturns(context.getConnectionWithDefaultRole(),
        "WITH\n" +
        "SET [*NATIVE_CJ_SET] AS 'NONEMPTYCROSSJOIN([*BASE_MEMBERS__Time_],[*BASE_MEMBERS__Time.Weekly_])'\n" +
        "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],[Time].[Time].CURRENTMEMBER.ORDERKEY,BASC,[Time].[Weekly].CURRENTMEMBER.ORDERKEY,BASC)'\n" +
        "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0]}'\n" +
        "SET [*BASE_MEMBERS__Time.Weekly_] AS '[Time].[Weekly].[Year].MEMBERS'\n" +
        "SET [*BASE_MEMBERS__Time_] AS '[Time].[Time].[Year].MEMBERS'\n" +
        "SET [*CJ_ROW_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Time].[Time].CURRENTMEMBER,[Time].[Weekly].CURRENTMEMBER)})'\n" +
        "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=500\n" +
        "MEMBER [Time].[Weekly].[*DEFAULT_MEMBER] AS '[Time].[Weekly].DEFAULTMEMBER', SOLVE_ORDER=-400\n" +
        "MEMBER [Time].[Weekly].[*TOTAL_MEMBER_SEL~SUM] AS 'SUM(CACHEDEXISTS([*CJ_ROW_AXIS],([Time].[Time].CURRENTMEMBER),\"[*CJ_ROW_AXIS]\"))', SOLVE_ORDER=99\n" +
        "MEMBER [Time].[Time].[*TOTAL_MEMBER_SEL~SUM] AS 'SUM([*CJ_ROW_AXIS])', SOLVE_ORDER=100\n" +
        "SELECT\n" +
        "[*BASE_MEMBERS__Measures_] ON COLUMNS\n" +
        ", NON EMPTY\n" +
        "UNION(CROSSJOIN({[Time].[Time].[*TOTAL_MEMBER_SEL~SUM]},{([Time].[Weekly].[*DEFAULT_MEMBER])}),UNION(CROSSJOIN(GENERATE([*CJ_ROW_AXIS], {([Time].[Time].CURRENTMEMBER)}),{[Time].[Weekly].[*TOTAL_MEMBER_SEL~SUM]}),[*SORTED_ROW_AXIS])) ON ROWS\n" +
        "FROM [Alternate Sales]",
        "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Time].[Time].[*TOTAL_MEMBER_SEL~SUM], [Time].[Weekly].[*DEFAULT_MEMBER]}\n"
            + "{[Time].[Time].[1997], [Time].[Weekly].[*TOTAL_MEMBER_SEL~SUM]}\n"
            + "{[Time].[Time].[1997], [Time].[Weekly].[1997]}\n"
            + "Row #0: 266,773\n"
            + "Row #1: 266,773\n"
            + "Row #2: 266,773\n");

    // Verified second arg of CachedExists uses a member type
    TestUtil.assertQueryReturns(context.getConnectionWithDefaultRole(),
        "WITH\n" +
        "SET [*NATIVE_CJ_SET] AS 'NONEMPTYCROSSJOIN([*BASE_MEMBERS__Time.Weekly_],NONEMPTYCROSSJOIN([*BASE_MEMBERS__Time_],[*BASE_MEMBERS__Time.Weekly2_]))'\n" +
        "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],[Time].[Weekly].CURRENTMEMBER.ORDERKEY,BASC,[Time].[Time].CURRENTMEMBER.ORDERKEY,BASC,[Time].[Weekly2].CURRENTMEMBER.ORDERKEY,BASC)'\n" +
        "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0]}'\n" +
        "SET [*BASE_MEMBERS__Time.Weekly2_] AS '[Time].[Weekly2].[Year].MEMBERS'\n" +
        "SET [*BASE_MEMBERS__Time.Weekly_] AS '[Time].[Weekly].[Year].MEMBERS'\n" +
        "SET [*BASE_MEMBERS__Time_] AS '[Time].[Time].[Year].MEMBERS'\n" +
        "SET [*CJ_ROW_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Time].[Weekly].CURRENTMEMBER,[Time].[Time].CURRENTMEMBER,[Time].[Weekly2].CURRENTMEMBER)})'\n" +
        "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=500\n" +
        "MEMBER [Time].[Weekly2].[*TOTAL_MEMBER_SEL~SUM] AS 'SUM(CACHEDEXISTS([*CJ_ROW_AXIS],([Time].[Weekly].CURRENTMEMBER, [Time].[Time].CURRENTMEMBER),\"[*CJ_ROW_AXIS]\"))', SOLVE_ORDER=98\n" +
        "SELECT\n" +
        "[*BASE_MEMBERS__Measures_] ON COLUMNS\n" +
        ", NON EMPTY\n" +
        "UNION(CROSSJOIN(GENERATE([*CJ_ROW_AXIS], {([Time].[Weekly].CURRENTMEMBER,[Time].[Time].CURRENTMEMBER)}),{[Time].[Weekly2].[*TOTAL_MEMBER_SEL~SUM]}),[*SORTED_ROW_AXIS]) ON ROWS\n" +
        "FROM [Alternate Sales]",
        "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
            + "Axis #2:\n"
            + "{[Time].[Weekly].[1997], [Time].[Time].[1997], [Time].[Weekly2].[*TOTAL_MEMBER_SEL~SUM]}\n"
            + "{[Time].[Weekly].[1997], [Time].[Time].[1997], [Time].[Weekly2].[1997]}\n"
            + "Row #0: 266,773\n"
            + "Row #1: 266,773\n");
  }



}

