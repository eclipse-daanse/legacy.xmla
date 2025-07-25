/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2009-2021 Hitachi Vantara
// All Rights Reserved.
*/
package mondrian.rolap;

import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.verifySameNativeAndNot;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.rolap.common.RolapConnectionPropsR;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.enums.DatabaseProduct;
import mondrian.test.SqlPattern;

class NativeEvalVirtualCubeTest extends BatchTestCase {


  @AfterEach
  public void afterEach() {
    SystemWideProperties.instance().populateInitial();
  }

    /**
     * Both dims fully join to the applicable base cube.
     */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSimpleFullyJoiningCJ(Context<?> context) {
    verifySameNativeAndNot(context.getConnectionWithDefaultRole(),
        "select {measures.[unit sales], measures.[warehouse sales]} on 0, "
        + " nonemptycrossjoin( Gender.Gender.members, product.[product category].members) on 1 "
        + "from [warehouse and sales]",
        "");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testPartiallyJoiningCJ(Context<?> context) {
    String query = "select measures.[warehouse sales] on 0, "
      + " NON EMPTY Crossjoin ( Gender.Gender.gender.members, product.product.[product category].members) on 1 "
      + " from [warehouse and sales]";

      verifySameNativeAndNot(context.getConnectionWithDefaultRole(), query, "");
      assertQueryReturns(context.getConnectionWithDefaultRole(),
          query,
          "Axis #0:\n"
          + "{}\n"
          + "Axis #1:\n"
          + "{[Measures].[Warehouse Sales]}\n"
          + "Axis #2:\n");
  }

  /**
   * Both dims fully join to one of the applicable base cubes.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOneFullyJoiningCube(Context<?> context) {
    verifySameNativeAndNot(context.getConnectionWithDefaultRole(),
        "select {measures.[unit sales], measures.[warehouse sales]} on 0, "
        + " nonemptycrossjoin( Gender.Gender.members, product.[product category].members) on 1 "
        + "from [warehouse and sales]",
        "");
  }

  void testNoApplicableCube(Context<?> context) {
    verifySameNativeAndNot(context.getConnectionWithDefaultRole(),
        "select {measures.[unit sales]} on 0, "
        + " nonemptycrossjoin( Gender.Gender.members, [Warehouse].[All Warehouses].children) on 1 "
        + "from [warehouse and sales]",
        "");
  }

  /**
   * [All Gender] should not impact the nonempty tuple list,
   * even though Gender does not
   * apply to [Warehouse Sales]
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testShouldBeFullyJoiningCJ(Context<?> context) {
    verifySameNativeAndNot(context.getConnectionWithDefaultRole(),
        "select measures.[warehouse Sales] on 0, "
        + " nonemptycrossjoin( Gender.[All Gender], "
        + "product.[product category].members)"
        + " on 1 "
        + " from [warehouse and sales]", "");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMeasureChangesContextOfInapplicableDimension(Context<?> context) {
      verifySameNativeAndNot(context.getConnectionWithDefaultRole(),
          "with member [Measures].[allW] as \n"
          + "'([Measures].[Unit Sales], [Warehouse].[All Warehouses])'\n"
          + "select NON EMPTY Crossjoin(\n"
          + "[Warehouse].[State Province].[CA], [Product].[All Products].children) \n"
          + "ON COLUMNS,\n"
          + "{ [Measures].[allW]}\n"
          + "ON ROWS\n"
          + "from [Warehouse and Sales]", "");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMeasureChangesContextOfApplicableDimension(Context<?> context) {
    String query =
        "with member [Measures].[allW] as \n"
        + "'([Measures].[Warehouse Sales], [Warehouse].[All Warehouses])'\n"
        + "select NON EMPTY Crossjoin(\n"
        + "[Warehouse].[All Warehouses].[USA].Children, [Product].[All Products].children) \n"
        + "ON COLUMNS,\n"
        + "{ [Measures].[allW]}\n"
        + "ON ROWS\n"
        + "from [Warehouse and Sales]";
    verifySameNativeAndNot(context.getConnectionWithDefaultRole(), query, "");
    assertQueryReturns(context.getConnectionWithDefaultRole(),
        query,
        "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Warehouse].[Warehouse].[USA].[CA], [Product].[Product].[Drink]}\n"
        + "{[Warehouse].[Warehouse].[USA].[CA], [Product].[Product].[Food]}\n"
        + "{[Warehouse].[Warehouse].[USA].[CA], [Product].[Product].[Non-Consumable]}\n"
        + "{[Warehouse].[Warehouse].[USA].[OR], [Product].[Product].[Drink]}\n"
        + "{[Warehouse].[Warehouse].[USA].[OR], [Product].[Product].[Food]}\n"
        + "{[Warehouse].[Warehouse].[USA].[OR], [Product].[Product].[Non-Consumable]}\n"
        + "{[Warehouse].[Warehouse].[USA].[WA], [Product].[Product].[Drink]}\n"
        + "{[Warehouse].[Warehouse].[USA].[WA], [Product].[Product].[Food]}\n"
        + "{[Warehouse].[Warehouse].[USA].[WA], [Product].[Product].[Non-Consumable]}\n"
        + "Axis #2:\n"
        + "{[Measures].[allW]}\n"
        + "Row #0: 18,010.602\n"
        + "Row #0: 141,147.92\n"
        + "Row #0: 37,612.366\n"
        + "Row #0: 18,010.602\n"
        + "Row #0: 141,147.92\n"
        + "Row #0: 37,612.366\n"
        + "Row #0: 18,010.602\n"
        + "Row #0: 141,147.92\n"
        + "Row #0: 37,612.366\n");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNECJWithValidMeasureAndInapplicableDimension(Context<?> context) {
    // with this query the crossjoin optimizer also causes issues if
    // evaluated non-natively- so both
    // native on/off will give same results, but wrong unless cjoptimizer
    // doesn't kick in.
    String query =
        "with member [Measures].[validUS] as \n"
        + "'ValidMeasure([Measures].[Unit Sales])'\n"
        + "select NON EMPTY Crossjoin(\n"
        + "{[Warehouse].[USA].children}, [Product].[All Products].children) \n"
        + "ON COLUMNS,\n"
        + "{ [Measures].[validUS]}\n"
        + "ON ROWS\n"
        + "from [Warehouse and Sales]";
    assertQueryReturns(context.getConnectionWithDefaultRole(),
        query,
        "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Warehouse].[Warehouse].[USA].[CA], [Product].[Product].[Drink]}\n"
        + "{[Warehouse].[Warehouse].[USA].[CA], [Product].[Product].[Food]}\n"
        + "{[Warehouse].[Warehouse].[USA].[CA], [Product].[Product].[Non-Consumable]}\n"
        + "{[Warehouse].[Warehouse].[USA].[OR], [Product].[Product].[Drink]}\n"
        + "{[Warehouse].[Warehouse].[USA].[OR], [Product].[Product].[Food]}\n"
        + "{[Warehouse].[Warehouse].[USA].[OR], [Product].[Product].[Non-Consumable]}\n"
        + "{[Warehouse].[Warehouse].[USA].[WA], [Product].[Product].[Drink]}\n"
        + "{[Warehouse].[Warehouse].[USA].[WA], [Product].[Product].[Food]}\n"
        + "{[Warehouse].[Warehouse].[USA].[WA], [Product].[Product].[Non-Consumable]}\n"
        + "Axis #2:\n"
        + "{[Measures].[validUS]}\n"
        + "Row #0: 24,597\n"
        + "Row #0: 191,940\n"
        + "Row #0: 50,236\n"
        + "Row #0: 24,597\n"
        + "Row #0: 191,940\n"
        + "Row #0: 50,236\n"
        + "Row #0: 24,597\n"
        + "Row #0: 191,940\n"
        + "Row #0: 50,236\n");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDisjointDimensionCJ(Context<?> context) {
    // No fully joining dimensions.
    assertQueryReturns(context.getConnectionWithDefaultRole(),
        "with member measures.vmWS as 'ValidMeasure(measures.[Warehouse Sales])'"
        + " select NON EMPTY Crossjoin(\n"
        + "{[Warehouse].[State Province].members}, {Gender.[All Gender].children} ) \n"
        + "ON COLUMNS,\n"
        + "{ [Measures].[Unit Sales], Measures.[vmWS] }\n"
        + "ON ROWS\n"
        + "from [Warehouse and Sales]",
        "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Warehouse].[Warehouse].[USA].[CA], [Gender].[Gender].[F]}\n"
        + "{[Warehouse].[Warehouse].[USA].[CA], [Gender].[Gender].[M]}\n"
        + "{[Warehouse].[Warehouse].[USA].[OR], [Gender].[Gender].[F]}\n"
        + "{[Warehouse].[Warehouse].[USA].[OR], [Gender].[Gender].[M]}\n"
        + "{[Warehouse].[Warehouse].[USA].[WA], [Gender].[Gender].[F]}\n"
        + "{[Warehouse].[Warehouse].[USA].[WA], [Gender].[Gender].[M]}\n"
        + "Axis #2:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[vmWS]}\n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #1: 57,814.858\n"
        + "Row #1: 57,814.858\n"
        + "Row #1: 38,835.053\n"
        + "Row #1: 38,835.053\n"
        + "Row #1: 100,120.976\n"
        + "Row #1: 100,120.976\n");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testWarehouseForcedToAllLevel(Context<?> context) {
    verifySameNativeAndNot(context.getConnectionWithDefaultRole(),
        "with member [Measures].[validUS] as \n"
        + "'ValidMeasure([Measures].[Unit Sales])'\n"
        + "select NON EMPTY Crossjoin(\n"
        + "{[Warehouse].[State Province].[CA],[Warehouse].[State Province].[WA]}, [Product].[All Products].children) \n"
        + "ON COLUMNS,\n"
        + "{ [Measures].[validUS]}\n"
        + "ON ROWS\n"
        + "from [Warehouse and Sales]", "");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMdxCJOfApplicableAndNonApplicable(Context<?> context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
        "WITH\n"
        + "MEMBER Measures.[ValidM Unit Sales] as 'ValidMeasure([Measures].[Unit Sales])' "
        + "SET [*NATIVE_CJ_SET_WITH_SLICER] AS 'NONEMPTYCROSSJOIN([*BASE_MEMBERS__Warehouse_],[*BASE_MEMBERS__Gender_])"
        + "'\n"
        + "SET [*NATIVE_CJ_SET] AS '[*NATIVE_CJ_SET_WITH_SLICER]'\n"
        + "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],[Warehouse].[Warehouse].CURRENTMEMBER.ORDERKEY,BASC,ANCESTOR([Warehouse].[Warehouse]"
        + ".CURRENTMEMBER,[Warehouse].[Country]).ORDERKEY,BASC,[Gender].[Gender].CURRENTMEMBER.ORDERKEY,BASC)'\n"
        + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[ValidM Unit Sales]}'\n"
        + "SET [*BASE_MEMBERS__Gender_] AS '[Gender].[Gender].[Gender].MEMBERS'\n"
        + "SET [*BASE_MEMBERS__Warehouse_] AS '[Warehouse].[Warehouse].[USA].Children'\n"
        + "SET [*CJ_ROW_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Warehouse].[Warehouse].CURRENTMEMBER,[Gender].[Gender].CURRENTMEMBER)})'\n"
        + "SELECT\n"
        + "[*BASE_MEMBERS__Measures_] ON COLUMNS\n"
        + ",NON EMPTY\n"
        + "[*SORTED_ROW_AXIS] ON ROWS\n"
        + "FROM [Warehouse and Sales]",
        "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[ValidM Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Warehouse].[Warehouse].[USA].[CA], [Gender].[Gender].[F]}\n"
        + "{[Warehouse].[Warehouse].[USA].[CA], [Gender].[Gender].[M]}\n"
        + "{[Warehouse].[Warehouse].[USA].[OR], [Gender].[Gender].[F]}\n"
        + "{[Warehouse].[Warehouse].[USA].[OR], [Gender].[Gender].[M]}\n"
        + "{[Warehouse].[Warehouse].[USA].[WA], [Gender].[Gender].[F]}\n"
        + "{[Warehouse].[Warehouse].[USA].[WA], [Gender].[Gender].[M]}\n"
        + "Row #0: 131,558\n"
        + "Row #1: 135,215\n"
        + "Row #2: 131,558\n"
        + "Row #3: 135,215\n"
        + "Row #4: 131,558\n"
        + "Row #5: 135,215\n");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAllMemberTupleInapplicableDim(Context<?> context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
        "WITH\n"
        + "SET [*NATIVE_CJ_SET_WITH_SLICER] AS 'NONEMPTYCROSSJOIN([*BASE_MEMBERS__Warehouse_],"
        + "[*BASE_MEMBERS__Gender_])'\n"
        + "SET [*NATIVE_CJ_SET] AS '[*NATIVE_CJ_SET_WITH_SLICER]'\n"
        + "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],[Warehouse].[Warehouse].CURRENTMEMBER.ORDERKEY,BASC,ANCESTOR"
        + "([Warehouse].CURRENTMEMBER,[Warehouse].[Warehouse].[Country]).ORDERKEY,BASC,[Gender].CURRENTMEMBER.ORDERKEY,BASC)'\n"
        + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0],"
        + "[Measures].[*CALCULATED_MEASURE_1]}'\n"
        + "SET [*BASE_MEMBERS__Gender_] AS '[Gender].[Gender].[Gender].MEMBERS'\n"
        + "SET [*BASE_MEMBERS__Warehouse_] AS '[Warehouse].[Warehouse].[USA].Children'\n"
        + "SET [*CJ_ROW_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Warehouse].[Warehouse].CURRENTMEMBER,[Gender].CURRENTMEMBER)})'\n"
        + "MEMBER [Measures].[*CALCULATED_MEASURE_1] AS '( [Warehouse].[Warehouse].[All Warehouses], [Measures].[Unit Sales] )', "
        + "SOLVE_ORDER=0\n"
        + "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', "
        + "SOLVE_ORDER=500\n"
        + "SELECT\n"
        + "[*BASE_MEMBERS__Measures_] ON COLUMNS\n"
        + ",NON EMPTY\n"
        + "[*SORTED_ROW_AXIS] ON ROWS\n"
        + "FROM [Warehouse and Sales]",
        "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
        + "{[Measures].[*CALCULATED_MEASURE_1]}\n"
        + "Axis #2:\n"
        + "{[Warehouse].[Warehouse].[USA].[CA], [Gender].[Gender].[F]}\n"
        + "{[Warehouse].[Warehouse].[USA].[CA], [Gender].[Gender].[M]}\n"
        + "{[Warehouse].[Warehouse].[USA].[OR], [Gender].[Gender].[F]}\n"
        + "{[Warehouse].[Warehouse].[USA].[OR], [Gender].[Gender].[M]}\n"
        + "{[Warehouse].[Warehouse].[USA].[WA], [Gender].[Gender].[F]}\n"
        + "{[Warehouse].[Warehouse].[USA].[WA], [Gender].[Gender].[M]}\n"
        + "Row #0: \n"
        + "Row #0: 131,558\n"
        + "Row #1: \n"
        + "Row #1: 135,215\n"
        + "Row #2: \n"
        + "Row #2: 131,558\n"
        + "Row #3: \n"
        + "Row #3: 135,215\n"
        + "Row #4: \n"
        + "Row #4: 131,558\n"
        + "Row #5: \n"
        + "Row #5: 135,215\n");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIntermixedDimensionGroupings(Context<?> context) {
    // crossjoin places intermixes applicable and inapplicable
    // attributes, which
    // this verifies that the projected crossjoin is in the correct order,
    // even though the
    // components may not be evaluated together.  (in this case gender
    // and marital status are
    // natively evaluated in a cj, with warehouse evaluated in a separate
    // group.  The sets need to be reassembled and projected correctly).
    assertQueryReturns(context.getConnectionWithDefaultRole(),
        "with member measures.vmUS as 'ValidMeasure(Measures.[Unit Sales])' "
        + "select non empty crossjoin(crossjoin(gender.gender.gender.members, warehouse.warehouse.[USA].[CA]), [marital status].[marital status].[marital status].members) on 0, "
        + " measures.vmUS on 1 from [warehouse and sales]",
        "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Gender].[Gender].[F], [Warehouse].[Warehouse].[USA].[CA], [Marital Status].[Marital Status].[M]}\n"
        + "{[Gender].[Gender].[F], [Warehouse].[Warehouse].[USA].[CA], [Marital Status].[Marital Status].[S]}\n"
        + "{[Gender].[Gender].[M], [Warehouse].[Warehouse].[USA].[CA], [Marital Status].[Marital Status].[M]}\n"
        + "{[Gender].[Gender].[M], [Warehouse].[Warehouse].[USA].[CA], [Marital Status].[Marital Status].[S]}\n"
        + "Axis #2:\n"
        + "{[Measures].[vmUS]}\n"
        + "Row #0: 65,336\n"
        + "Row #0: 66,222\n"
        + "Row #0: 66,460\n"
        + "Row #0: 68,755\n");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCachedShouldNotBeUsed(Context<?> context) {
    // First query doesn't use a measure like ValidMeasure, so results in an
    // empty tuples set being cached.  The second query should not reuse the
    // cache results from the first query, since it *does* use VM.
    executeQuery(
        "select non empty crossjoin(gender.gender.gender.members, warehouse.warehouse.[USA].[CA]) on 0, "
        + "measures.[unit sales] on 1 from [warehouse and sales]", context.getConnectionWithDefaultRole());
    assertQueryReturns(context.getConnectionWithDefaultRole(),
        "with member measures.vm as 'validmeasure(measures.[unit sales])' "
        + "select non empty crossjoin(gender.gender.gender.members, warehouse.warehouse.[USA].[CA]) on 0, "
        + "measures.vm on 1 from [warehouse and sales]",
        "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Gender].[Gender].[F], [Warehouse].[Warehouse].[USA].[CA]}\n"
        + "{[Gender].[Gender].[M], [Warehouse].[Warehouse].[USA].[CA]}\n"
        + "Axis #2:\n"
        + "{[Measures].[vm]}\n"
        + "Row #0: 131,558\n"
        + "Row #0: 135,215\n");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testShouldUseCache(Context<?> context) {
    // verify cache does get used for applicable grouped target tuple queries
    ((TestContextImpl)context).setGenerateFormattedSql(true);
    String mySqlGenderQuery = "select\n"
      + "    `customer`.`gender` as `c0`\n"
      + "from\n"
      + "    `customer` as `customer`,\n"
      + "    `sales_fact_1997` as `sales_fact_1997`\n"
      + "where\n"
      + "    `sales_fact_1997`.`customer_id` = `customer`.`customer_id`\n"
      + "group by\n"
      + "    `customer`.`gender`\n"
      + "order by\n"
      + "    ISNULL(`c0`) ASC, `c0` ASC";
    //TestContext<?> tc = getTestContext().withFreshConnection();
    //context.setProperty(RolapConnectionProperties.UseSchemaPool.name(), Boolean.toString(false));
    SqlPattern mysqlPattern =
      new SqlPattern(
          DatabaseProduct.MYSQL,
          mySqlGenderQuery,
          mySqlGenderQuery);
    String mdx =
        "with member measures.vm as 'validmeasure(measures.[unit sales])' "
        + "select non empty "
        + "crossjoin(gender.gender.gender.members, warehouse.warehouse.[USA].[CA]) on 0, "
        + "measures.vm on 1 from [warehouse and sales]";
    // first MDX with a fresh query should result in gender query.
    Connection connection = context.getConnection(new RolapConnectionPropsR(
        List.of(), false, Locale.getDefault(), Duration.ofSeconds(-1), Optional.empty(), Optional.empty()
    ));
    assertQuerySqlOrNot(connection,
        mdx, new SqlPattern[]{ mysqlPattern }, false, false, false);
    // rerun the MDX, since the previous assert aborts when it hits the SQL.
    executeQuery(mdx, connection);
    // Subsequent query should pull from cache, not rerun gender query.
    assertQuerySqlOrNot(connection,
        mdx, new SqlPattern[]{ mysqlPattern }, true, false, false);
  }

  /**
   * Testcase for bug
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-2597">MONDRIAN-2597,
   * "readTuples and cardinality queries sent twice to the database
   * when using Virtual Cube (Not cached)"</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTupleQueryShouldBeCachedForVirtualCube(Context<?> context) {
	  context.getCatalogCache().clear();
      ((TestContextImpl)context).setGenerateFormattedSql(true);
    String mySqlMembersQuery =
            "select\n"
            + "    *\n"
            + "from\n"
            + "    (select\n"
            + "    `product_class`.`product_family` as `c0`,\n"
            + "    `product_class`.`product_department` as `c1`,\n"
            + "    `time_by_day`.`the_year` as `c2`\n"
            + "from\n"
            + "    `product` as `product`,\n"
            + "    `product_class` as `product_class`,\n"
            + "    `sales_fact_1997` as `sales_fact_1997`,\n"
            + "    `time_by_day` as `time_by_day`\n"
            + "where\n"
            + "    `product`.`product_class_id` = `product_class`.`product_class_id`\n"
            + "and\n"
            + "    `sales_fact_1997`.`product_id` = `product`.`product_id`\n"
            + "and\n"
            + "    `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`\n"
            + "group by\n"
            + "    `product_class`.`product_family`,\n"
            + "    `product_class`.`product_department`,\n"
            + "    `time_by_day`.`the_year`\n"
            + "union\n"
            + "select\n"
            + "    `product_class`.`product_family` as `c0`,\n"
            + "    `product_class`.`product_department` as `c1`,\n"
            + "    `time_by_day`.`the_year` as `c2`\n"
            + "from\n"
            + "    `product` as `product`,\n"
            + "    `product_class` as `product_class`,\n"
            + "    `inventory_fact_1997` as `inventory_fact_1997`,\n"
            + "    `time_by_day` as `time_by_day`\n"
            + "where\n"
            + "    `product`.`product_class_id` = `product_class`.`product_class_id`\n"
            + "and\n"
            + "    `inventory_fact_1997`.`product_id` = `product`.`product_id`\n"
            + "and\n"
            + "    `inventory_fact_1997`.`time_id` = `time_by_day`.`time_id`\n"
            + "group by\n"
            + "    `product_class`.`product_family`,\n"
            + "    `product_class`.`product_department`,\n"
            + "    `time_by_day`.`the_year`) as `unionQuery`\n"
            + "order by\n"
            + "    ISNULL(1) ASC, 1 ASC,\n"
            + "    ISNULL(2) ASC, 2 ASC,\n"
            + "    ISNULL(3) ASC, 3 ASC";
    //final TestContext<?> context = getTestContext().withFreshConnection();
    SqlPattern mysqlPatternMembers =
      new SqlPattern(
          DatabaseProduct.MYSQL,
          mySqlMembersQuery,
          mySqlMembersQuery);
    //The MDX with default measure of [Warehouse and Sales] virtual cube:
    //Store Sales that belongs to the redular [Sale] cube
    String mdx =
            "WITH\n"
            + "SET [*NATIVE_CJ_SET] AS 'NONEMPTYCROSSJOIN([*BASE_MEMBERS__Product_],[*BASE_MEMBERS__Time_])'\n"
            + "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],ANCESTOR([Product].[Product].CURRENTMEMBER, [Product].[Product].[Product Family]).ORDERKEY,BASC,[Product].[Product].CURRENTMEMBER.ORDERKEY,BASC,[Time].[Time].CURRENTMEMBER.ORDERKEY,BASC)'\n"
            + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0]}'\n"
            + "SET [*BASE_MEMBERS__Product_] AS '[Product].[Product].[Product Department].MEMBERS'\n"
            + "SET [*BASE_MEMBERS__Time_] AS '[Time].[Time].[Year].MEMBERS'\n"
            + "SET [*CJ_ROW_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Product].[Product].CURRENTMEMBER,[Time].[Time].CURRENTMEMBER)})'\n"
            + "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Store Sales]', FORMAT_STRING = '#,###.00', SOLVE_ORDER=500\n"
            + "SELECT\n"
            + "[*BASE_MEMBERS__Measures_] ON COLUMNS\n"
            + ", NON EMPTY\n"
            + "[*SORTED_ROW_AXIS] ON ROWS\n"
            + "FROM [Warehouse and Sales]";
    // first MDX with a fresh query should result in product_family,
    //product_department and the_year query.
    assertQuerySqlOrNot(context.getConnectionWithDefaultRole(),
        mdx, new SqlPattern[]{ mysqlPatternMembers }, false, false, false);
    // rerun the MDX, since the previous assert aborts when it hits the SQL.
    executeQuery(mdx, context.getConnectionWithDefaultRole());
    // Subsequent query should pull from cache, not rerun product_family,
    //product_department and the_year query.
    assertQuerySqlOrNot(context.getConnectionWithDefaultRole(),
        mdx, new SqlPattern[]{ mysqlPatternMembers }, true, false, false);
    //The MDX with added Warehouse Sales measure
    //that belongs to the regulr [Warehouse].
    String mdx1 =
            "WITH\n"
            + "SET [*NATIVE_CJ_SET] AS 'NONEMPTYCROSSJOIN([*BASE_MEMBERS__Product_],[*BASE_MEMBERS__Time_])'\n"
            + "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],ANCESTOR([Product].[Product].CURRENTMEMBER, [Product].[Product].[Product Family]).ORDERKEY,BASC,[Product].[Product].CURRENTMEMBER.ORDERKEY,BASC,[Time].[Time].CURRENTMEMBER.ORDERKEY,BASC)'\n"
            + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[Warehouse Sales]}'\n"
            + "SET [*BASE_MEMBERS__Product_] AS '[Product].[Product].[Product Department].MEMBERS'\n"
            + "SET [*BASE_MEMBERS__Time_] AS '[Time].[Time].[Year].MEMBERS'\n"
            + "SET [*CJ_ROW_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Product].[Product].CURRENTMEMBER,[Time].[Time].CURRENTMEMBER)})'\n"
            + "SELECT\n"
            + "[*BASE_MEMBERS__Measures_] ON COLUMNS\n"
            + ", NON EMPTY\n"
            + "[*SORTED_ROW_AXIS] ON ROWS\n"
            + "FROM [Warehouse and Sales]";
    // Subsequent query should pull from cache, not rerun product_family,
    //product_department and the_year query.
    assertQuerySqlOrNot(context.getConnectionWithDefaultRole(),
        mdx1, new SqlPattern[]{ mysqlPatternMembers }, true, false, false);
  }
}
