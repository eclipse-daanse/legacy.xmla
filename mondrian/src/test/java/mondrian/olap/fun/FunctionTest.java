/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2003-2005 Julian Hyde
// Copyright (C) 2005-2020 Hitachi Vantara and others
// Copyright (C) 2022 Sergei Semenkov
// All Rights Reserved.
*/
package mondrian.olap.fun;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static mondrian.olap.Util.assertTrue;
import static mondrian.olap.exceptions.CousinHierarchyMismatchException.cousinHierarchyMismatch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertAxisThrows;
import static org.opencube.junit5.TestUtil.assertBooleanExprReturns;
import static org.opencube.junit5.TestUtil.assertExprDependsOn;
import static org.opencube.junit5.TestUtil.assertExprThrows;
import static org.opencube.junit5.TestUtil.assertMemberExprDependsOn;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertQueryThrows;
import static org.opencube.junit5.TestUtil.assertSetExprDependsOn;
import static org.opencube.junit5.TestUtil.assertStubbedEqualsVerbose;
import static org.opencube.junit5.TestUtil.compileExpression;
import static org.opencube.junit5.TestUtil.executeAxis;
import static org.opencube.junit5.TestUtil.executeExpr;
import static org.opencube.junit5.TestUtil.executeExprRaw;
import static org.opencube.junit5.TestUtil.executeQuery;
import static org.opencube.junit5.TestUtil.executeSingletonAxis;
import static org.opencube.junit5.TestUtil.hierarchyName;
import static org.opencube.junit5.TestUtil.isDefaultNullMemberRepresentation;
import static org.opencube.junit5.TestUtil.withSchema;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.Position;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.DimensionConnectorMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.DataType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.LevelType;
import org.eclipse.daanse.rolap.mapping.instance.complex.foodmart.FoodmartMappingSupplier;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TimeDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.VirtualCubeMappingImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.TestConfig;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mondrian.olap.MondrianException;
import mondrian.olap.QueryTimeoutException;
import mondrian.olap.SystemWideProperties;
import mondrian.olap.Util;
import mondrian.rolap.RolapSchemaPool;
import mondrian.rolap.SchemaModifiers;
import mondrian.util.Bug;

//import mondrian.spi.DialectManager;

/**
 * <code>FunctionTest</code> tests the functions defined in
 * {@link BuiltinFunTable}.
 *
 * @author gjohnson
 */
class FunctionTest {//extends FoodMartTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger( FunctionTest.class );
  private static final int NUM_EXPECTED_FUNCTIONS = 239;

  private static final String[] AllHiers = {
          "[Measures]",
          "[Store]",
          "[Store Size in SQFT]",
          "[Store Type]",
          "[Time]",
          SystemWideProperties.instance().SsasCompatibleNaming ? "[Time].[Weekly]" : "[Time.Weekly]",
          "[Product]",
          "[Promotion Media]",
          "[Promotions]",
          "[Customers]",
          "[Education Level]",
          "[Gender]",
          "[Marital Status]",
          "[Yearly Income]"
  };

  private static final String months =
    "[Time].[1997].[Q1].[1]\n"
      + "[Time].[1997].[Q1].[2]\n"
      + "[Time].[1997].[Q1].[3]\n"
      + "[Time].[1997].[Q2].[4]\n"
      + "[Time].[1997].[Q2].[5]\n"
      + "[Time].[1997].[Q2].[6]\n"
      + "[Time].[1997].[Q3].[7]\n"
      + "[Time].[1997].[Q3].[8]\n"
      + "[Time].[1997].[Q3].[9]\n"
      + "[Time].[1997].[Q4].[10]\n"
      + "[Time].[1997].[Q4].[11]\n"
      + "[Time].[1997].[Q4].[12]";

  private static final String quarters =
    "[Time].[1997].[Q1]\n"
      + "[Time].[1997].[Q2]\n"
      + "[Time].[1997].[Q3]\n"
      + "[Time].[1997].[Q4]";

  private static final String year1997 = "[Time].[1997]";

  private static final String hierarchized1997 =
    year1997
      + "\n"
      + "[Time].[1997].[Q1]\n"
      + "[Time].[1997].[Q1].[1]\n"
      + "[Time].[1997].[Q1].[2]\n"
      + "[Time].[1997].[Q1].[3]\n"
      + "[Time].[1997].[Q2]\n"
      + "[Time].[1997].[Q2].[4]\n"
      + "[Time].[1997].[Q2].[5]\n"
      + "[Time].[1997].[Q2].[6]\n"
      + "[Time].[1997].[Q3]\n"
      + "[Time].[1997].[Q3].[7]\n"
      + "[Time].[1997].[Q3].[8]\n"
      + "[Time].[1997].[Q3].[9]\n"
      + "[Time].[1997].[Q4]\n"
      + "[Time].[1997].[Q4].[10]\n"
      + "[Time].[1997].[Q4].[11]\n"
      + "[Time].[1997].[Q4].[12]";

  private static final String NullNumericExpr =
    " ([Measures].[Unit Sales],"
      + "   [Customers].[All Customers].[USA].[CA].[Bellflower], "
      + "   [Product].[All Products].[Drink].[Alcoholic Beverages]."
      + "[Beer and Wine].[Beer].[Good].[Good Imported Beer])";

  private static final String TimeWeekly =
    SystemWideProperties.instance().SsasCompatibleNaming
      ? "[Time].[Weekly]"
      : "[Time.Weekly]";



  @BeforeEach
  public void beforeEach() {

  }

  @AfterEach
  public void afterEach() {
    SystemWideProperties.instance().populateInitial();
  }

  /**
   * Tests that Integeer.MIN_VALUE(-2147483648) does not cause NPE.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testParallelPeriodMinValue(Context context) {
    // By running the query and getting a result without an exception, we should assert the return value which will
    // have empty rows, because the parallel period value is too large, so rows will be empty
    // data, but it will still return a result.
    String query = "with "
      + "member [measures].[foo] as "
      + "'([Measures].[unit sales],"
      + "ParallelPeriod([Time].[Quarter], -2147483648))' "
      + "select "
      + "[measures].[foo] on columns, "
      + "[time].[1997].children on rows "
      + "from [sales]";
    String expected = "Axis #0:\n"
      + "{}\n"
      + "Axis #1:\n"
      + "{[Measures].[foo]}\n"
      + "Axis #2:\n"
      + "{[Time].[1997].[Q1]}\n"
      + "{[Time].[1997].[Q2]}\n"
      + "{[Time].[1997].[Q3]}\n"
      + "{[Time].[1997].[Q4]}\n"
      + "Row #0: \n"
      + "Row #1: \n"
      + "Row #2: \n"
      + "Row #3: \n";
    TestUtil.assertQueryReturns(context.getConnection(), query, expected);
  }

  /**
   * Tests that Integeer.MIN_VALUE(-2147483648) in Lag is handled correctly.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLagMinValue(Context context) {
    // By running the query and getting a result without an exception, we should assert the return value which will
    // have empty rows, because the lag value is too large for the traversal it needs to make, so rows will be empty
    // data, but it will still return a result.
    String query = "with "
      + "member [measures].[foo] as "
      + "'([Measures].[unit sales], [Time].[1997].[Q1].Lag(-2147483648))' "
      + "select "
      + "[measures].[foo] on columns, "
      + "[time].[1997].children on rows "
      + "from [sales]";
    String expected = "Axis #0:\n"
      + "{}\n"
      + "Axis #1:\n"
      + "{[Measures].[foo]}\n"
      + "Axis #2:\n"
      + "{[Time].[1997].[Q1]}\n"
      + "{[Time].[1997].[Q2]}\n"
      + "{[Time].[1997].[Q3]}\n"
      + "{[Time].[1997].[Q4]}\n"
      + "Row #0: \n"
      + "Row #1: \n"
      + "Row #2: \n"
      + "Row #3: \n";
    TestUtil.assertQueryReturns(context.getConnection(), query, expected );
  }

  /**
   * Tests that ParallelPeriod with Aggregate function works
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testParallelPeriodWithSlicer(Context context) {
    TestUtil.assertQueryReturns(context.getConnection(),
      "With "
        + "Set [*NATIVE_CJ_SET] as 'NonEmptyCrossJoin([*BASE_MEMBERS_Time],[*BASE_MEMBERS_Product])' "
        + "Set [*BASE_MEMBERS_Measures] as '{[Measures].[*FORMATTED_MEASURE_0], [Measures].[*FORMATTED_MEASURE_1]}' "
        + "Set [*BASE_MEMBERS_Time] as '{[Time].[1997].[Q2].[6]}' "
        + "Set [*NATIVE_MEMBERS_Time] as 'Generate([*NATIVE_CJ_SET], {[Time].[Time].CurrentMember})' "
        + "Set [*BASE_MEMBERS_Product] as '{[Product].[All Products].[Drink],[Product].[All Products].[Food]}' "
        + "Set [*NATIVE_MEMBERS_Product] as 'Generate([*NATIVE_CJ_SET], {[Product].CurrentMember})' "
        + "Member [Measures].[*FORMATTED_MEASURE_0] as '[Measures].[Customer Count]', FORMAT_STRING = '#,##0', "
        + "SOLVE_ORDER=400 "
        + "Member [Measures].[*FORMATTED_MEASURE_1] as "
        + "'([Measures].[Customer Count], ParallelPeriod([Time].[Quarter], 1, [Time].[Time].currentMember))', "
        + "FORMAT_STRING = '#,##0', SOLVE_ORDER=-200 "
        + "Member [Product].[*FILTER_MEMBER] as 'Aggregate ([*NATIVE_MEMBERS_Product])', SOLVE_ORDER=-300 "
        + "Select "
        + "[*BASE_MEMBERS_Measures] on columns, Non Empty Generate([*NATIVE_CJ_SET], {([Time].[Time].CurrentMember)})"
        + " on rows "
        + "From [Sales] "
        + "Where ([Product].[*FILTER_MEMBER])",
      "Axis #0:\n"
        + "{[Product].[*FILTER_MEMBER]}\n"
        + "Axis #1:\n"
        + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
        + "{[Measures].[*FORMATTED_MEASURE_1]}\n"
        + "Axis #2:\n"
        + "{[Time].[1997].[Q2].[6]}\n"
        + "Row #0: 1,314\n"
        + "Row #0: 1,447\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testParallelperiodOnLevelsString(Context context) {
    TestUtil.assertQueryReturns(context.getConnection(),
      "with member Measures.[Prev Unit Sales] as 'parallelperiod(Levels(\"[Time].[Month]\"))'\n"
        + "select {[Measures].[Unit Sales], Measures.[Prev Unit Sales]} ON COLUMNS,\n"
        + "[Gender].members ON ROWS\n"
        + "from [Sales]\n"
        + "where [Time].[1997].[Q2].[5]",
      "Axis #0:\n"
        + "{[Time].[1997].[Q2].[5]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Prev Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Gender].[All Gender]}\n"
        + "{[Gender].[F]}\n"
        + "{[Gender].[M]}\n"
        + "Row #0: 21,081\n"
        + "Row #0: 20,179\n"
        + "Row #1: 10,536\n"
        + "Row #1: 9,990\n"
        + "Row #2: 10,545\n"
        + "Row #2: 10,189\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testParallelperiodOnStrToMember(Context context) {
    assertQueryReturns(context.getConnection(),
      "with member Measures.[Prev Unit Sales] as 'parallelperiod(strToMember(\"[Time].[1997].[Q2]\"))'\n"
        + "select {[Measures].[Unit Sales], Measures.[Prev Unit Sales]} ON COLUMNS,\n"
        + "[Gender].members ON ROWS\n"
        + "from [Sales]\n"
        + "where [Time].[1997].[Q2].[5]",
      "Axis #0:\n"
        + "{[Time].[1997].[Q2].[5]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Prev Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Gender].[All Gender]}\n"
        + "{[Gender].[F]}\n"
        + "{[Gender].[M]}\n"
        + "Row #0: 21,081\n"
        + "Row #0: 20,957\n"
        + "Row #1: 10,536\n"
        + "Row #1: 10,266\n"
        + "Row #2: 10,545\n"
        + "Row #2: 10,691\n" );

    assertQueryThrows(context.getConnection(),
      "with member Measures.[Prev Unit Sales] as 'parallelperiod(strToMember(\"[Time].[Quarter]\"))'\n"
        + "select {[Measures].[Unit Sales], Measures.[Prev Unit Sales]} ON COLUMNS,\n"
        + "[Gender].members ON ROWS\n"
        + "from [Sales]\n"
        + "where [Time].[1997].[Q2].[5]",
      "Cannot find MDX member '[Time].[Quarter]'. Make sure it is indeed a member and not a level or a hierarchy." );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNumericLiteral(Context context) {
    TestUtil.assertExprReturns(context.getConnection(), "2", "2" );
    if ( false ) {
      // The test is currently broken because the value 2.5 is formatted
      // as "2". TODO: better default format string
      TestUtil.assertExprReturns(context.getConnection(),"2.5", "2.5" );
    }
     TestUtil.assertExprReturns(context.getConnection(), "-10.0", "-10" );
    TestUtil.assertExprDependsOn(context.getConnection(), "1.5", "{}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStringLiteral(Context context) {
    // single-quoted string
    if ( false ) {
      // TODO: enhance parser so that you can include a quoted string
      //   inside a WITH MEMBER clause
      TestUtil.assertExprReturns(context.getConnection(), "'foobar'", "foobar" );
    }
    // double-quoted string
    TestUtil.assertExprReturns(context.getConnection(), "\"foobar\"", "foobar" );
    // literals don't depend on any dimensions
    TestUtil.assertExprDependsOn(context.getConnection(), "\"foobar\"", "{}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDimensionHierarchy(Context context) {
    TestUtil.assertExprReturns(context.getConnection(), "[Time].Dimension.Name", "Time" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLevelDimension(Context context) {
    TestUtil.assertExprReturns(context.getConnection(), "[Time].[Year].Dimension.UniqueName", "[Time]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMemberDimension(Context context) {
     TestUtil.assertExprReturns(context.getConnection(), "[Time].[1997].[Q2].Dimension.UniqueName", "[Time]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDimensionsNumeric(Context context) {
    TestUtil.assertExprDependsOn(context.getConnection(), "Dimensions(2).Name", "{}" );
    TestUtil.assertMemberExprDependsOn(context.getConnection(),
            "Dimensions(3).CurrentMember",
      allHiers());
    TestUtil.assertExprReturns(context.getConnection(), "Dimensions(2).Name", "Store Size in SQFT" );
    // bug 1426134 -- Dimensions(0) throws 'Index '0' out of bounds'
    TestUtil.assertExprReturns(context.getConnection(), "Dimensions(0).Name", "Measures" );
    TestUtil.assertExprThrows(context.getConnection(), "Dimensions(-1).Name", "Index '-1' out of bounds" );
    TestUtil.assertExprThrows(context.getConnection(), "Dimensions(100).Name", "Index '100' out of bounds" );
    // Since Dimensions returns a Hierarchy, can apply CurrentMember.
    assertAxisReturns(context.getConnection(),
      "Dimensions(3).CurrentMember",
      "[Store Type].[All Store Types]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDimensionsString(Context context) {
    TestUtil.assertExprDependsOn(context.getConnection(),
      "Dimensions(\"foo\").UniqueName",
      "{}" );
    TestUtil.assertMemberExprDependsOn(context.getConnection(),
      "Dimensions(\"foo\").CurrentMember", allHiers() );
    TestUtil.assertExprReturns(context.getConnection(), "Dimensions(\"Store\").UniqueName", "[Store]" );
    // Since Dimensions returns a Hierarchy, can apply Children.
    assertAxisReturns(context.getConnection(),
      "Dimensions(\"Store\").Children",
      "[Store].[Canada]\n"
        + "[Store].[Mexico]\n"
        + "[Store].[USA]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDimensionsDepends(Context context) {
    final String expression =
      "Crossjoin("
        + "{Dimensions(\"Measures\").CurrentMember.Hierarchy.CurrentMember}, "
        + "{Dimensions(\"Product\")})";
    assertAxisReturns(context.getConnection(),
      expression, "{[Measures].[Unit Sales], [Product].[All Products]}" );
    TestUtil.assertSetExprDependsOn(context.getConnection(),
      expression, allHiers() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTime(Context context) {
    TestUtil.assertExprReturns(context.getConnection(),
      "[Time].[1997].[Q1].[1].Hierarchy.UniqueName", "[Time]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBasic9(Context context) {
    TestUtil.assertExprReturns(context.getConnection(),
      "[Gender].[All Gender].[F].Hierarchy.UniqueName", "[Gender]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFirstInLevel9(Context context) {
    TestUtil.assertExprReturns(context.getConnection(),
      "[Education Level].[All Education Levels].[Bachelors Degree].Hierarchy.UniqueName",
      "[Education Level]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHierarchyAll(Context context) {
    TestUtil.assertExprReturns(context.getConnection(),
      "[Gender].[All Gender].Hierarchy.UniqueName", "[Gender]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNullMember(Context context) {
    // MSAS fails here, but Mondrian doesn't.
    TestUtil.assertExprReturns(context.getConnection(),
      "[Gender].[All Gender].Parent.Level.UniqueName",
      "[Gender].[(All)]" );

    // MSAS fails here, but Mondrian doesn't.
    TestUtil.assertExprReturns(context.getConnection(),
      "[Gender].[All Gender].Parent.Hierarchy.UniqueName", "[Gender]" );

    // MSAS fails here, but Mondrian doesn't.
    TestUtil.assertExprReturns(context.getConnection(),
      "[Gender].[All Gender].Parent.Dimension.UniqueName", "[Gender]" );

    // MSAS succeeds too
    TestUtil.assertExprReturns(context.getConnection(),
      "[Gender].[All Gender].Parent.Children.Count", "0" );

    if ( isDefaultNullMemberRepresentation() ) {
      // MSAS returns "" here.
      TestUtil.assertExprReturns(context.getConnection(),
        "[Gender].[All Gender].Parent.UniqueName", "[Gender].[#null]" );

      // MSAS returns "" here.
      TestUtil.assertExprReturns(context.getConnection(),
        "[Gender].[All Gender].Parent.Name", "#null" );
    }
  }

  /**
   * Tests use of NULL literal to generate a null cell value. Testcase is from bug 1440344.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNullValue(Context context) {
    TestUtil.assertQueryReturns(context.getConnection(),
      "with member [Measures].[X] as 'IIF([Measures].[Store Sales]>10000,[Measures].[Store Sales],Null)'\n"
        + "select\n"
        + "{[Measures].[X]} on columns,\n"
        + "{[Product].[Product Department].members} on rows\n"
        + "from Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[X]}\n"
        + "Axis #2:\n"
        + "{[Product].[Drink].[Alcoholic Beverages]}\n"
        + "{[Product].[Drink].[Beverages]}\n"
        + "{[Product].[Drink].[Dairy]}\n"
        + "{[Product].[Food].[Baked Goods]}\n"
        + "{[Product].[Food].[Baking Goods]}\n"
        + "{[Product].[Food].[Breakfast Foods]}\n"
        + "{[Product].[Food].[Canned Foods]}\n"
        + "{[Product].[Food].[Canned Products]}\n"
        + "{[Product].[Food].[Dairy]}\n"
        + "{[Product].[Food].[Deli]}\n"
        + "{[Product].[Food].[Eggs]}\n"
        + "{[Product].[Food].[Frozen Foods]}\n"
        + "{[Product].[Food].[Meat]}\n"
        + "{[Product].[Food].[Produce]}\n"
        + "{[Product].[Food].[Seafood]}\n"
        + "{[Product].[Food].[Snack Foods]}\n"
        + "{[Product].[Food].[Snacks]}\n"
        + "{[Product].[Food].[Starchy Foods]}\n"
        + "{[Product].[Non-Consumable].[Carousel]}\n"
        + "{[Product].[Non-Consumable].[Checkout]}\n"
        + "{[Product].[Non-Consumable].[Health and Hygiene]}\n"
        + "{[Product].[Non-Consumable].[Household]}\n"
        + "{[Product].[Non-Consumable].[Periodicals]}\n"
        + "Row #0: 14,029.08\n"
        + "Row #1: 27,748.53\n"
        + "Row #2: \n"
        + "Row #3: 16,455.43\n"
        + "Row #4: 38,670.41\n"
        + "Row #5: \n"
        + "Row #6: 39,774.34\n"
        + "Row #7: \n"
        + "Row #8: 30,508.85\n"
        + "Row #9: 25,318.93\n"
        + "Row #10: \n"
        + "Row #11: 55,207.50\n"
        + "Row #12: \n"
        + "Row #13: 82,248.42\n"
        + "Row #14: \n"
        + "Row #15: 67,609.82\n"
        + "Row #16: 14,550.05\n"
        + "Row #17: 11,756.07\n"
        + "Row #18: \n"
        + "Row #19: \n"
        + "Row #20: 32,571.86\n"
        + "Row #21: 60,469.89\n"
        + "Row #22: \n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNullInMultiplication(Context context) {
    Connection connection = context.getConnection();
    TestUtil.assertExprReturns(connection, "NULL*1", "" );
    TestUtil.assertExprReturns(connection, "1*NULL", "" );
    TestUtil.assertExprReturns(connection, "NULL*NULL", "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNullInAddition(Context context) {
    Connection connection = context.getConnection();
    TestUtil.assertExprReturns(connection, "1+NULL", "1" );
    TestUtil.assertExprReturns(connection, "NULL+1", "1" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNullInSubtraction(Context context) {
    Connection connection = context.getConnection();
    TestUtil.assertExprReturns(connection, "1-NULL", "1" );
    TestUtil.assertExprReturns(connection, "NULL-1", "-1" );
  }


  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMemberLevel(Context context) {
    TestUtil.assertExprReturns(context.getConnection(),
      "[Time].[1997].[Q1].[1].Level.UniqueName",
      "[Time].[Month]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLevelsNumeric(Context context) {
    Connection connection = context.getConnection();
    TestUtil.assertExprReturns(connection, "[Time].[Time].Levels(2).Name", "Month" );
    TestUtil.assertExprReturns(connection, "[Time].[Time].Levels(0).Name", "Year" );
    TestUtil.assertExprReturns(connection, "[Product].Levels(0).Name", "(All)" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLevelsTooSmall(Context context) {
    TestUtil.assertExprThrows(context.getConnection(),
      "[Time].[Time].Levels(-1).Name", "Index '-1' out of bounds" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLevelsTooLarge(Context context) {
    TestUtil.assertExprThrows(context.getConnection(),
      "[Time].[Time].Levels(8).Name", "Index '8' out of bounds" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHierarchyLevelsString(Context context) {
    TestUtil.assertExprReturns(context.getConnection(),
      "[Time].[Time].Levels(\"Year\").UniqueName", "[Time].[Year]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHierarchyLevelsStringFail(Context context) {
    TestUtil.assertExprThrows(context.getConnection(),
      "[Time].[Time].Levels(\"nonexistent\").UniqueName",
      "Level 'nonexistent' not found in hierarchy '[Time]'" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLevelsString(Context context) {
    TestUtil.assertExprReturns(context.getConnection(),
      "Levels(\"[Time].[Year]\").UniqueName",
      "[Time].[Year]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLevelsStringFail(Context context) {
    TestUtil.assertExprThrows(context.getConnection(),
      "Levels(\"nonexistent\").UniqueName",
      "Level 'nonexistent' not found" );
  }

  @Disabled //TODO need investigate
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIsEmptyQuery(Context context) {
    String desiredResult =
      "Axis #0:\n"
        + "{[Time].[1997].[Q4].[12], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth]"
        + ".[Portsmouth Imported Beer], [Measures].[Foo]}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA].[Bellingham]}\n"
        + "{[Store].[USA].[WA].[Bremerton]}\n"
        + "{[Store].[USA].[WA].[Seattle]}\n"
        + "{[Store].[USA].[WA].[Spokane]}\n"
        + "{[Store].[USA].[WA].[Tacoma]}\n"
        + "{[Store].[USA].[WA].[Walla Walla]}\n"
        + "{[Store].[USA].[WA].[Yakima]}\n"
        + "Row #0: 5\n"
        + "Row #0: 5\n"
        + "Row #0: 2\n"
        + "Row #0: 5\n"
        + "Row #0: 11\n"
        + "Row #0: 5\n"
        + "Row #0: 4\n";

    TestUtil.assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Measures].[Foo] AS 'Iif(IsEmpty([Measures].[Unit Sales]), 5, [Measures].[Unit Sales])'\n"
        + "SELECT {[Store].[USA].[WA].children} on columns\n"
        + "FROM Sales\n"
        + "WHERE ([Time].[1997].[Q4].[12],\n"
        + " [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth].[Portsmouth "
        + "Imported Beer],\n"
        + " [Measures].[Foo])",
      desiredResult );

    TestUtil.assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Measures].[Foo] AS 'Iif([Measures].[Unit Sales] IS EMPTY, 5, [Measures].[Unit Sales])'\n"
        + "SELECT {[Store].[USA].[WA].children} on columns\n"
        + "FROM Sales\n"
        + "WHERE ([Time].[1997].[Q4].[12],\n"
        + " [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth].[Portsmouth "
        + "Imported Beer],\n"
        + " [Measures].[Foo])",
      desiredResult );

    TestUtil.assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Measures].[Foo] AS 'Iif([Measures].[Bar] IS EMPTY, 1, [Measures].[Bar])'\n"
        + "MEMBER [Measures].[Bar] AS 'CAST(\"42\" AS INTEGER)'\n"
        + "SELECT {[Measures].[Unit Sales], [Measures].[Foo]} on columns\n"
        + "FROM Sales\n"
        + "WHERE ([Time].[1998].[Q4].[12])",
      "Axis #0:\n"
        + "{[Time].[1998].[Q4].[12]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Foo]}\n"
        + "Row #0: \n"
        + "Row #0: 42\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIsEmptyWithAggregate(Context context) {
    TestUtil.assertQueryReturns(context.getConnection(),
      "WITH MEMBER [gender].[foo] AS 'isEmpty(Aggregate({[Gender].m}))' "
        + "SELECT {Gender.foo} on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Gender].[foo]}\n"
        + "Row #0: false\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIsEmpty(Context context) {
    Connection connection = context.getConnection();
    assertBooleanExprReturns(connection, "[Gender].[All Gender].Parent IS NULL", true );

    // Any functions that return a member from parameters that
    // include a member and that member is NULL also give a NULL.
    // Not a runtime exception.
    assertBooleanExprReturns(connection,
      "[Gender].CurrentMember.Parent.NextMember IS NULL",
      true );

    if ( !Bug.BugMondrian207Fixed ) {
      return;
    }

    // When resolving a tuple's value in the cube, if there is
    // at least one NULL member in the tuple should return a
    // NULL cell value.
    assertBooleanExprReturns(connection,
      "IsEmpty(([Time].currentMember.Parent, [Measures].[Unit Sales]))",
      false );
    assertBooleanExprReturns(connection,
      "IsEmpty(([Time].currentMember, [Measures].[Unit Sales]))",
      false );

    // EMPTY refers to a genuine cell value that exists in the cube space,
    // and has no NULL members in the tuple,
    // but has no fact data at that crossing,
    // so it evaluates to EMPTY as a cell value.
    assertBooleanExprReturns(connection,
      "IsEmpty(\n"
        + " ([Time].[1997].[Q4].[12],\n"
        + "  [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth].[Portsmouth "
        + "Imported Beer],\n"
        + "  [Store].[All Stores].[USA].[WA].[Bellingham]))", true );
    assertBooleanExprReturns(connection,
      "IsEmpty(\n"
        + " ([Time].[1997].[Q4].[11],\n"
        + "  [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth].[Portsmouth "
        + "Imported Beer],\n"
        + "  [Store].[All Stores].[USA].[WA].[Bellingham]))", false );

    // The empty set is neither EMPTY nor NULL.
    // should give 0 as a result, not NULL and not EMPTY.
    TestUtil.assertQueryReturns(connection,
      "WITH SET [empty set] AS '{}'\n"
        + " MEMBER [Measures].[Set Size] AS 'Count([empty set])'\n"
        + " MEMBER [Measures].[Set Size Is Empty] AS 'CASE WHEN IsEmpty([Measures].[Set Size]) THEN 1 ELSE 0 END '\n"
        + "SELECT [Measures].[Set Size] on columns", "" );

    TestUtil.assertQueryReturns(connection,
      "WITH SET [empty set] AS '{}'\n"
        + "WITH MEMBER [Measures].[Set Size] AS 'Count([empty set])'\n"
        + "SELECT [Measures].[Set Size] on columns", "" );

    // Run time errors are BAD things.  They should not occur
    // in almost all cases.  In fact there should be no
    // logically formed MDX that generates them.  An ERROR
    // value in a cell though is perfectly legal - e.g. a
    // divide by 0.
    // E.g.
    String foo =
      "WITH [Measures].[Ratio This Period to Previous] as\n"
        + "'([Measures].[Sales],[Time].CurrentMember/([Measures].[Sales],[Time].CurrentMember.PrevMember)'\n"
        + "SELECT [Measures].[Ratio This Period to Previous] ON COLUMNS,\n"
        + "[Time].Members ON ROWS\n"
        + "FROM ...";

    // For the [Time].[All Time] row as well as the first
    // year, first month etc, the PrevMember will evaluate to
    // NULL, the tuple will evaluate to NULL and the division
    // will implicitly convert the NULL to 0 and then evaluate
    // to an ERROR value due to a divide by 0.

    // This leads to another point: NULL and EMPTY values get
    // implicitly converted to 0 when treated as numeric
    // values for division and multiplication but for addition
    // and subtraction, NULL is treated as NULL (5+NULL yields
    // NULL).
    // I have no idea about how EMPTY works.  I.e. is does
    // 5+EMPTY yield 5 or EMPTY or NULL or what?
    // E.g.
    String foo2 =
      "WITH MEMBER [Measures].[5 plus empty] AS\n"
        + "'5+([Product].[All Products].[Ski boots],[Geography].[All Geography].[Hawaii])'\n"
        + "SELECT [Measures].[5 plus empty] ON COLUMNS\n"
        + "FROM ...";
    // Does this yield EMPTY, 5, NULL or ERROR?

    // Lastly, IS NULL and IS EMPTY are both legal and
    // distinct.  <<Object>> IS {<<Object>> | NULL}  and
    // <<Value>> IS EMPTY.
    // E.g.
    // a)  [Time].CurrentMember.Parent IS [Time].[Year].[2004]
    // is also a perfectly legal expression and better than
    // [Time].CurrentMember.Parent.Name="2004".
    // b) ([Measures].[Sales],[Time].FirstSibling) IS EMPTY is
    // a legal expression.


    // Microsoft's site says that the EMPTY value participates in 3 value
    // logic e.g. TRUE AND EMPTY gives EMPTY, FALSE AND EMPTY gives FALSE.
    // todo: test for this
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testQueryWithoutValidMeasure(Context context) {
    TestUtil.assertQueryReturns(context.getConnection(),
      "with\n"
        + "member measures.[without VM] as ' [measures].[unit sales] '\n"
        + "select {measures.[without VM] } on 0,\n"
        + "[Warehouse].[Country].members on 1 from [warehouse and sales]\n",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[without VM]}\n"
        + "Axis #2:\n"
        + "{[Warehouse].[Canada]}\n"
        + "{[Warehouse].[Mexico]}\n"
        + "{[Warehouse].[USA]}\n"
        + "Row #0: \n"
        + "Row #1: \n"
        + "Row #2: \n" );
  }

  /**
   * Tests the <code>ValidMeasure</code> function.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testValidMeasure(Context context) {
    TestUtil.assertQueryReturns(context.getConnection(),
      "with\n"
        + "member measures.[with VM] as 'validmeasure([measures].[unit sales])'\n"
        + "select { measures.[with VM]} on 0,\n"
        + "[Warehouse].[Country].members on 1 from [warehouse and sales]\n",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[with VM]}\n"
        + "Axis #2:\n"
        + "{[Warehouse].[Canada]}\n"
        + "{[Warehouse].[Mexico]}\n"
        + "{[Warehouse].[USA]}\n"
        + "Row #0: 266,773\n"
        + "Row #1: 266,773\n"
        + "Row #2: 266,773\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void _testValidMeasureNonEmpty(Context context) {
    // Note that [with VM2] is NULL where it needs to be - and therefore
    // does not prevent NON EMPTY from eliminating empty rows.
    TestUtil.assertQueryReturns(context.getConnection(),
      "with set [Foo] as ' Crossjoin({[Time].Children}, {[Measures].[Warehouse Sales]}) '\n"
        + " member [Measures].[with VM] as 'ValidMeasure([Measures].[Unit Sales])'\n"
        + " member [Measures].[with VM2] as 'Iif(Count(Filter([Foo], not isempty([Measures].CurrentMember))) > 0, "
        + "ValidMeasure([Measures].[Unit Sales]), NULL)'\n"
        + "select NON EMPTY Crossjoin({[Time].Children}, {[Measures].[with VM2], [Measures].[Warehouse Sales]}) ON "
        + "COLUMNS,\n"
        + "  NON EMPTY {[Warehouse].[All Warehouses].[USA].[WA].Children} ON ROWS\n"
        + "from [Warehouse and Sales]\n"
        + "where [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good].[Good Light "
        + "Beer]",
      "Axis #0:\n"
        + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good].[Good Light Beer]}\n"
        + "Axis #1:\n"
        + "{[Time].[1997].[Q1], [Measures].[with VM2]}\n"
        + "{[Time].[1997].[Q1], [Measures].[Warehouse Sales]}\n"
        + "{[Time].[1997].[Q2], [Measures].[with VM2]}\n"
        + "{[Time].[1997].[Q2], [Measures].[Warehouse Sales]}\n"
        + "{[Time].[1997].[Q3], [Measures].[with VM2]}\n"
        + "{[Time].[1997].[Q4], [Measures].[with VM2]}\n"
        + "Axis #2:\n"
        + "{[Warehouse].[USA].[WA].[Seattle]}\n"
        + "{[Warehouse].[USA].[WA].[Tacoma]}\n"
        + "{[Warehouse].[USA].[WA].[Yakima]}\n"
        + "Row #0: 26\n"
        + "Row #0: 34.793\n"
        + "Row #0: 25\n"
        + "Row #0: \n"
        + "Row #0: 36\n"
        + "Row #0: 28\n"
        + "Row #1: 26\n"
        + "Row #1: \n"
        + "Row #1: 25\n"
        + "Row #1: 64.615\n"
        + "Row #1: 36\n"
        + "Row #1: 28\n"
        + "Row #2: 26\n"
        + "Row #2: 79.657\n"
        + "Row #2: 25\n"
        + "Row #2: \n"
        + "Row #2: 36\n"
        + "Row #2: 28\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testValidMeasureTupleHasAnotherMember(Context context) {
    TestUtil.assertQueryReturns(context.getConnection(),
      "with\n"
        + "member measures.[with VM] as 'validmeasure(([measures].[unit sales],[customers].[all customers]))'\n"
        + "select { measures.[with VM]} on 0,\n"
        + "[Warehouse].[Country].members on 1 from [warehouse and sales]\n",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[with VM]}\n"
        + "Axis #2:\n"
        + "{[Warehouse].[Canada]}\n"
        + "{[Warehouse].[Mexico]}\n"
        + "{[Warehouse].[USA]}\n"
        + "Row #0: 266,773\n"
        + "Row #1: 266,773\n"
        + "Row #2: 266,773\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testValidMeasureDepends(Context context) {
    Connection connection = context.getConnection();
    String s12 = allHiersExcept( "[Measures]" );
    TestUtil.assertExprDependsOn(connection,
      "ValidMeasure([Measures].[Unit Sales])", s12 );

    String s11 = allHiersExcept( "[Measures]", "[Time]" );
    TestUtil.assertExprDependsOn(connection,
      "ValidMeasure(([Measures].[Unit Sales], [Time].[1997].[Q1]))", s11 );

    String s1 = allHiersExcept( "[Measures]" );
    TestUtil.assertExprDependsOn(connection,
      "ValidMeasure(([Measures].[Unit Sales], "
        + "[Time].[Time].CurrentMember.Parent))",
      s1 );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testValidMeasureNonVirtualCube(Context context) {
    // verify ValidMeasure used outside of a virtual cube
    // is effectively a no-op.
    Connection connection = context.getConnection();
    TestUtil.assertQueryReturns(connection,
      "with member measures.vm as 'ValidMeasure(measures.[Store Sales])'"
        + " select measures.[vm] on 0 from Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[vm]}\n"
        + "Row #0: 565,238.13\n" );
     TestUtil.assertQueryReturns(connection,
      "with member measures.vm as 'ValidMeasure((gender.f, measures.[Store Sales]))'"
        + " select measures.[vm] on 0 from Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[vm]}\n"
        + "Row #0: 280,226.21\n" );
  }

  /**
   * This is a test for
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-2109">MONDRIAN-2109</a>
   *
   * <p>We can't allow calculated members in ValidMeasure so a proper message
   * must be returned.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testValidMeasureCalculatedMemberMeasure(Context context) {
    // Check for failure.
     TestUtil.assertQueryThrows(context,
      "with member measures.calc as 'measures.[Warehouse sales]' \n"
        + "member measures.vm as 'ValidMeasure(measures.calc)' \n"
        + "select from [warehouse and sales]\n"
        + "where (measures.vm ,gender.f) \n",
      "The function ValidMeasure cannot be used with the measure '[Measures].[calc]' because it is a calculated "
        + "member." );
    // Check the working version
    TestUtil.assertQueryReturns(context.getConnection(),
      "with \n"
        + "member measures.vm as 'ValidMeasure(measures.[warehouse sales])' \n"
        + "select from [warehouse and sales] where (measures.vm, gender.f) \n",
      "Axis #0:\n"
        + "{[Measures].[vm], [Gender].[F]}\n"
        + "196,770.888" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAncestor(Context context) {
    Connection connection = context.getConnection();
    Member member =
            TestUtil.executeSingletonAxis(connection,
        "Ancestor([Store].[USA].[CA].[Los Angeles],[Store Country])" );
    assertEquals( "USA", member.getName() );

    TestUtil.assertAxisThrows(connection,
      "Ancestor([Store].[USA].[CA].[Los Angeles],[Promotions].[Promotion Name])",
      "Error while executing query" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  //
  void testAncestorNumeric(Context context) {
    Connection connection = context.getConnection();
    Member member =
      executeSingletonAxis(connection,
        "Ancestor([Store].[USA].[CA].[Los Angeles],1)" );
    assertEquals( "CA", member.getName() );

    member =
      executeSingletonAxis(connection,
        "Ancestor([Store].[USA].[CA].[Los Angeles], 0)" );
    assertEquals( "Los Angeles", member.getName() );

    member =
      executeSingletonAxis(connection,
        "Ancestor([Store].[All Stores].[Vatican], 1)" , "[Sales Ragged]");
    assertEquals( "All Stores", member.getName() );

    member =
      executeSingletonAxis(connection,
        "Ancestor([Store].[USA].[Washington], 1)", "[Sales Ragged]");
    assertEquals( "USA", member.getName() );

    // complicated way to say "1".
    member =
      executeSingletonAxis(connection,
        "Ancestor([Store].[USA].[Washington], 7 * 6 - 41)", "[Sales Ragged]");
    assertEquals( "USA", member.getName() );

    member =
      executeSingletonAxis(connection,
        "Ancestor([Store].[All Stores].[Vatican], 2)", "[Sales Ragged]");
    assertNull(member, "Ancestor at 2 must be null");

    member =
      executeSingletonAxis(connection,
        "Ancestor([Store].[All Stores].[Vatican], -5)", "[Sales Ragged]");
    assertNull(member, "Ancestor at -5 must be null" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAncestorHigher(Context context) {
    Member member =
      executeSingletonAxis(context.getConnection(),
        "Ancestor([Store].[USA],[Store].[Store City])" );
    assertNull( member ); // MSOLAP returns null
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAncestorSameLevel(Context context) {
    Member member =
      executeSingletonAxis(context.getConnection(),
        "Ancestor([Store].[Canada],[Store].[Store Country])" );
    assertEquals( "Canada", member.getName());
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAncestorWrongHierarchy(Context context) {
    // MSOLAP gives error "Formula error - dimensions are not
    // valid (they do not match) - in the Ancestor function"
    assertAxisThrows(context.getConnection(),
      "Ancestor([Gender].[M],[Store].[Store Country])",
      "Error while executing query" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAncestorAllLevel(Context context) {
    Member member =
      executeSingletonAxis(context.getConnection(),
        "Ancestor([Store].[USA].[CA],[Store].Levels(0))" );
    assertTrue( member.isAll() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAncestorWithHiddenParent(Context context) {
    //final Context testContext =
    //  getContext().withCube( "[Sales Ragged]" );
    Member member =
      executeSingletonAxis(context.getConnection(),
        "Ancestor([Store].[All Stores].[Israel].[Haifa], "
          + "[Store].[Store Country])", "[Sales Ragged]");

    assertNotNull(member, "Member must not be null.");
    assertEquals( "Israel", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAncestorDepends(Context context) {
    Connection connection = context.getConnection();
    assertExprDependsOn(connection,
      "Ancestor([Store].CurrentMember, [Store].[Store Country]).Name",
      "{[Store]}" );

    assertExprDependsOn(connection,
      "Ancestor([Store].[All Stores].[USA], "
        + "[Store].CurrentMember.Level).Name",
      "{[Store]}" );

    assertExprDependsOn(connection,
      "Ancestor([Store].[All Stores].[USA], "
        + "[Store].[Store Country]).Name",
      "{}" );

    assertExprDependsOn(connection,
      "Ancestor([Store].CurrentMember, 2+1).Name", "{[Store]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAncestors(Context context) {
    // Test that we can execute Ancestors by passing a level as
    // the depth argument (PC hierarchy)
    Connection connection = context.getConnection();
    assertQueryReturns(connection,
      "with\n"
        + "set [*ancestors] as\n"
        + "  'Ancestors([Employees].[All Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Eric Long]"
        + ".[Adam Reynolds].[Joshua Huff].[Teanna Cobb], [Employees].[All Employees].Level)'\n"
        + "select\n"
        + "  [*ancestors] on columns\n"
        + "from [HR]\n",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Eric Long].[Adam Reynolds].[Joshua Huff]}\n"
        + "{[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Eric Long].[Adam Reynolds]}\n"
        + "{[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Eric Long]}\n"
        + "{[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges]}\n"
        + "{[Employees].[Sheri Nowmer].[Derrick Whelply]}\n"
        + "{[Employees].[Sheri Nowmer]}\n"
        + "{[Employees].[All Employees]}\n"
        + "Row #0: $984.45\n"
        + "Row #0: $3,426.54\n"
        + "Row #0: $3,610.14\n"
        + "Row #0: $17,099.20\n"
        + "Row #0: $36,494.07\n"
        + "Row #0: $39,431.67\n"
        + "Row #0: $39,431.67\n" );
    // Test that we can execute Ancestors by passing a level as
    // the depth argument (non PC hierarchy)
    assertQueryReturns(connection,
      "with\n"
        + "set [*ancestors] as\n"
        + "  'Ancestors([Store].[USA].[CA].[Los Angeles], [Store].[Store Country])'\n"
        + "select\n"
        + "  [*ancestors] on columns\n"
        + "from [Sales]\n",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[CA]}\n"
        + "{[Store].[USA]}\n"
        + "Row #0: 74,748\n"
        + "Row #0: 266,773\n" );
    // Test that we can execute Ancestors by passing an integer as
    // the depth argument (PC hierarchy)
    assertQueryReturns(connection,
      "with\n"
        + "set [*ancestors] as\n"
        + "  'Ancestors([Employees].[All Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Eric Long]"
        + ".[Adam Reynolds].[Joshua Huff].[Teanna Cobb], 3)'\n"
        + "select\n"
        + "  [*ancestors] on columns\n"
        + "from [HR]\n",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Eric Long].[Adam Reynolds].[Joshua Huff]}\n"
        + "{[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Eric Long].[Adam Reynolds]}\n"
        + "{[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Eric Long]}\n"
        + "Row #0: $984.45\n"
        + "Row #0: $3,426.54\n"
        + "Row #0: $3,610.14\n" );
    // Test that we can execute Ancestors by passing an integer as
    // the depth argument (non PC hierarchy)
    assertQueryReturns(connection,
      "with\n"
        + "set [*ancestors] as\n"
        + "  'Ancestors([Store].[USA].[CA].[Los Angeles], 2)'\n"
        + "select\n"
        + "  [*ancestors] on columns\n"
        + "from [Sales]\n",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[CA]}\n"
        + "{[Store].[USA]}\n"
        + "Row #0: 74,748\n"
        + "Row #0: 266,773\n" );
    // Test that we can count the number of ancestors.
    assertQueryReturns(connection,
      "with\n"
        + "set [*ancestors] as\n"
        + "  'Ancestors([Employees].[All Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Eric Long]"
        + ".[Adam Reynolds].[Joshua Huff].[Teanna Cobb], [Employees].[All Employees].Level)'\n"
        + "member [Measures].[Depth] as\n"
        + "  'Count([*ancestors])'\n"
        + "select\n"
        + "  [Measures].[Depth] on columns\n"
        + "from [HR]\n",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Depth]}\n"
        + "Row #0: 7\n" );
    // test depth argument not a level
    assertAxisThrows(connection,
      "Ancestors([Store].[USA].[CA].[Los Angeles],[Store])",
      "Error while executing query" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrdinal(Context context) {
    //final Context testContext =
    //  getContext().withCube( "Sales Ragged" );
    Connection connection = context.getConnection();
    Cell cell =
      executeExprRaw(connection, "Sales Ragged",
        "[Store].[All Stores].[Vatican].ordinal" );
    assertEquals(
      1,
      ( (Number) cell.getValue() ).intValue(), "Vatican is at level 1.");

    cell = executeExprRaw(connection, "Sales Ragged",
      "[Store].[All Stores].[USA].[Washington].ordinal" );
    assertEquals(
      3,
      ( (Number) cell.getValue() ).intValue(), "Washington is at level 3.");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testClosingPeriodNoArgs(Context context) {
    Connection connection = context.getConnection();
    assertMemberExprDependsOn(connection,
      "ClosingPeriod()", "{[Time]}" );
    // MSOLAP returns [1997].[Q4], because [Time].CurrentMember =
    // [1997].
    Member member = executeSingletonAxis(connection, "ClosingPeriod()" );
    assertEquals( "[Time].[1997].[Q4]", member.getUniqueName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testClosingPeriodLevel(Context context) {
    Connection connection = context.getConnection();
    assertMemberExprDependsOn(connection,
      "ClosingPeriod([Time].[Year])", "{[Time]}" );
    assertMemberExprDependsOn(connection,
      "([Measures].[Unit Sales], ClosingPeriod([Time].[Month]))",
      "{[Time]}" );

    Member member;

    member = executeSingletonAxis(connection, "ClosingPeriod([Year])" );
    assertEquals( "[Time].[1997]", member.getUniqueName() );

    member = executeSingletonAxis(connection, "ClosingPeriod([Quarter])" );
    assertEquals( "[Time].[1997].[Q4]", member.getUniqueName() );

    member = executeSingletonAxis(connection, "ClosingPeriod([Month])" );
    assertEquals( "[Time].[1997].[Q4].[12]", member.getUniqueName() );

    assertQueryReturns(connection,
      "with member [Measures].[Closing Unit Sales] as "
        + "'([Measures].[Unit Sales], ClosingPeriod([Time].[Month]))'\n"
        + "select non empty {[Measures].[Closing Unit Sales]} on columns,\n"
        + " {Descendants([Time].[1997])} on rows\n"
        + "from [Sales]",

      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Closing Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Time].[1997]}\n"
        + "{[Time].[1997].[Q1]}\n"
        + "{[Time].[1997].[Q1].[1]}\n"
        + "{[Time].[1997].[Q1].[2]}\n"
        + "{[Time].[1997].[Q1].[3]}\n"
        + "{[Time].[1997].[Q2]}\n"
        + "{[Time].[1997].[Q2].[4]}\n"
        + "{[Time].[1997].[Q2].[5]}\n"
        + "{[Time].[1997].[Q2].[6]}\n"
        + "{[Time].[1997].[Q3]}\n"
        + "{[Time].[1997].[Q3].[7]}\n"
        + "{[Time].[1997].[Q3].[8]}\n"
        + "{[Time].[1997].[Q3].[9]}\n"
        + "{[Time].[1997].[Q4]}\n"
        + "{[Time].[1997].[Q4].[10]}\n"
        + "{[Time].[1997].[Q4].[11]}\n"
        + "{[Time].[1997].[Q4].[12]}\n"
        + "Row #0: 26,796\n"
        + "Row #1: 23,706\n"
        + "Row #2: 21,628\n"
        + "Row #3: 20,957\n"
        + "Row #4: 23,706\n"
        + "Row #5: 21,350\n"
        + "Row #6: 20,179\n"
        + "Row #7: 21,081\n"
        + "Row #8: 21,350\n"
        + "Row #9: 20,388\n"
        + "Row #10: 23,763\n"
        + "Row #11: 21,697\n"
        + "Row #12: 20,388\n"
        + "Row #13: 26,796\n"
        + "Row #14: 19,958\n"
        + "Row #15: 25,270\n"
        + "Row #16: 26,796\n" );

    assertQueryReturns(connection,
      "with member [Measures].[Closing Unit Sales] as '([Measures].[Unit Sales], ClosingPeriod([Time].[Month]))'\n"
        + "select {[Measures].[Unit Sales], [Measures].[Closing Unit Sales]} on columns,\n"
        + " {[Time].[1997], [Time].[1997].[Q1], [Time].[1997].[Q1].[1], [Time].[1997].[Q1].[3], [Time].[1997].[Q4]"
        + ".[12]} on rows\n"
        + "from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Closing Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Time].[1997]}\n"
        + "{[Time].[1997].[Q1]}\n"
        + "{[Time].[1997].[Q1].[1]}\n"
        + "{[Time].[1997].[Q1].[3]}\n"
        + "{[Time].[1997].[Q4].[12]}\n"
        + "Row #0: 266,773\n"
        + "Row #0: 26,796\n"
        + "Row #1: 66,291\n"
        + "Row #1: 23,706\n"
        + "Row #2: 21,628\n"
        + "Row #2: 21,628\n"
        + "Row #3: 23,706\n"
        + "Row #3: 23,706\n"
        + "Row #4: 26,796\n"
        + "Row #4: 26,796\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testClosingPeriodLevelNotInTimeFails(Context context) {
    assertAxisThrows(context.getConnection(),
      "ClosingPeriod([Store].[Store City])",
      "The <level> and <member> arguments to ClosingPeriod must be from "
        + "the same hierarchy. The level was from '[Store]' but the member "
        + "was from '[Time]'" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testClosingPeriodMember(Context context) {
    if ( false ) {
      // This test is mistaken. Valid forms are ClosingPeriod(<level>)
      // and ClosingPeriod(<level>, <member>), but not
      // ClosingPeriod(<member>)
      Member member = executeSingletonAxis( context.getConnection(),"ClosingPeriod([USA])" );
      assertEquals( "WA", member.getName() );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testClosingPeriodMemberLeaf(Context context) {
    Member member;
    if ( false ) {
      // This test is mistaken. Valid forms are ClosingPeriod(<level>)
      // and ClosingPeriod(<level>, <member>), but not
      // ClosingPeriod(<member>)
      member = executeSingletonAxis(context.getConnection(),
        "ClosingPeriod([Time].[1997].[Q3].[8])" );
      assertNull( member );
    } else if ( isDefaultNullMemberRepresentation() ) {
      assertQueryReturns(context.getConnection(),
        "with member [Measures].[Foo] as ClosingPeriod().uniquename\n"
          + "select {[Measures].[Foo]} on columns,\n"
          + "  {[Time].[1997],\n"
          + "   [Time].[1997].[Q2],\n"
          + "   [Time].[1997].[Q2].[4]} on rows\n"
          + "from Sales",
        "Axis #0:\n"
          + "{}\n"
          + "Axis #1:\n"
          + "{[Measures].[Foo]}\n"
          + "Axis #2:\n"
          + "{[Time].[1997]}\n"
          + "{[Time].[1997].[Q2]}\n"
          + "{[Time].[1997].[Q2].[4]}\n"
          + "Row #0: [Time].[1997].[Q4]\n"
          + "Row #1: [Time].[1997].[Q2].[6]\n"
          + "Row #2: [Time].[#null]\n"
          // MSAS returns "" here.
          + "" );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testClosingPeriod(Context context) {
	Connection connection = context.getConnection();
    assertMemberExprDependsOn(connection,
      "ClosingPeriod([Time].[Month], [Time].[Time].CurrentMember)",
      "{[Time]}" );

    String s1 = allHiersExcept( "[Measures]" );
    assertExprDependsOn(connection,
      "(([Measures].[Store Sales],"
        + " ClosingPeriod([Time].[Month], [Time].[Time].CurrentMember)) - "
        + "([Measures].[Store Cost],"
        + " ClosingPeriod([Time].[Month], [Time].[Time].CurrentMember)))",
      s1 );

    assertMemberExprDependsOn(connection,
      "ClosingPeriod([Time].[Month], [Time].[1997].[Q3])", "{}" );

    assertAxisReturns(connection,
      "ClosingPeriod([Time].[Year], [Time].[1997].[Q3])", "" );

    assertAxisReturns(connection,
      "ClosingPeriod([Time].[Quarter], [Time].[1997].[Q3])",
      "[Time].[1997].[Q3]" );

    assertAxisReturns(connection,
      "ClosingPeriod([Time].[Month], [Time].[1997].[Q3])",
      "[Time].[1997].[Q3].[9]" );

    assertAxisReturns(connection,
      "ClosingPeriod([Time].[Quarter], [Time].[1997])",
      "[Time].[1997].[Q4]" );

    assertAxisReturns(connection,
      "ClosingPeriod([Time].[Year], [Time].[1997])", "[Time].[1997]" );

    assertAxisReturns(connection,
      "ClosingPeriod([Time].[Month], [Time].[1997])",
      "[Time].[1997].[Q4].[12]" );

    // leaf member

    assertAxisReturns(connection,
      "ClosingPeriod([Time].[Year], [Time].[1997].[Q3].[8])", "" );

    assertAxisReturns(connection,
      "ClosingPeriod([Time].[Quarter], [Time].[1997].[Q3].[8])", "" );

    assertAxisReturns(connection,
      "ClosingPeriod([Time].[Month], [Time].[1997].[Q3].[8])",
      "[Time].[1997].[Q3].[8]" );

    // non-Time dimension

    assertAxisReturns(connection,
      "ClosingPeriod([Product].[Product Name], [Product].[All Products].[Drink])",
      "[Product].[Drink].[Dairy].[Dairy].[Milk].[Gorilla].[Gorilla Whole Milk]" );

    assertAxisReturns(connection,
      "ClosingPeriod([Product].[Product Family], [Product].[All Products].[Drink])",
      "[Product].[Drink]" );

    // 'all' level

    assertAxisReturns(connection,
      "ClosingPeriod([Product].[(All)], [Product].[All Products].[Drink])",
      "" );

    // ragged
    //getContext().withCube( "[Sales Ragged]" ).
    assertAxisReturns(connection, "[Sales Ragged]",
      "ClosingPeriod([Store].[Store City], [Store].[All Stores].[Israel])",
      "[Store].[Israel].[Israel].[Tel Aviv]" );

    // Default member is [Time].[1997].
    assertAxisReturns(connection,
      "ClosingPeriod([Time].[Month])", "[Time].[1997].[Q4].[12]" );

    assertAxisReturns(connection, "ClosingPeriod()", "[Time].[1997].[Q4]" );

    //Context testContext = getContext().withCube( "[Sales Ragged]" );
    assertAxisReturns(connection, "[Sales Ragged]",
      "ClosingPeriod([Store].[Store State], [Store].[All Stores].[Israel])",
      "" );

    assertAxisThrows(connection,
      "ClosingPeriod([Time].[Year], [Store].[All Stores].[Israel])",
      "The <level> and <member> arguments to ClosingPeriod must be "
        + "from the same hierarchy. The level was from '[Time]' but "
        + "the member was from '[Store]'.", "[Sales Ragged]");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testClosingPeriodBelow(Context context) {
    Member member = executeSingletonAxis(context.getConnection(),
      "ClosingPeriod([Quarter],[1997].[Q3].[8])" );
    assertNull( member );
  }


  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCousin1(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "Cousin([1997].[Q4],[1998])" );
    assertEquals( "[Time].[1998].[Q4]", member.getUniqueName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCousin2(Context context) {
    Member member = executeSingletonAxis(context.getConnection(),
      "Cousin([1997].[Q4].[12],[1998].[Q1])" );
    assertEquals( "[Time].[1998].[Q1].[3]", member.getUniqueName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCousinOverrun(Context context) {
    Member member = executeSingletonAxis(context.getConnection(),
      "Cousin([Customers].[USA].[CA].[San Jose],"
        + " [Customers].[USA].[OR])" );
    // CA has more cities than OR
    assertNull( member );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCousinThreeDown(Context context) {
    Member member =
      executeSingletonAxis(context.getConnection(),
        "Cousin([Customers].[USA].[CA].[Berkeley].[Barbara Combs],"
          + " [Customers].[Mexico])" );
    // Barbara Combs is the 6th child
    // of the 4th child (Berkeley)
    // of the 1st child (CA)
    // of USA
    // Annmarie Hill is the 6th child
    // of the 4th child (Tixapan)
    // of the 1st child (DF)
    // of Mexico
    assertEquals(
      "[Customers].[Mexico].[DF].[Tixapan].[Annmarie Hill]",
      member.getUniqueName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCousinSameLevel(Context context) {
    Member member =
      executeSingletonAxis(context.getConnection(), "Cousin([Gender].[M], [Gender].[F])" );
    assertEquals( "F", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCousinHigherLevel(Context context) {
    Member member =
      executeSingletonAxis(context.getConnection(), "Cousin([Time].[1997], [Time].[1998].[Q1])" );
    assertNull( member );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCousinWrongHierarchy(Context context) {
    assertAxisThrows(context.getConnection(),
      "Cousin([Time].[1997], [Gender].[M])",
        MessageFormat.format(cousinHierarchyMismatch,
        "[Time].[1997]",
        "[Gender].[M]" ) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testParent(Context context) {
    Connection connection = context.getConnection();
    assertMemberExprDependsOn(connection,
      "[Gender].Parent",
      "{[Gender]}" );
    assertMemberExprDependsOn(connection, "[Gender].[M].Parent", "{}" );
    assertAxisReturns(connection,
      "{[Store].[USA].[CA].Parent}", "[Store].[USA]" );
    // root member has null parent
    assertAxisReturns(connection, "{[Store].[All Stores].Parent}", "" );
    // parent of null member is null
    assertAxisReturns(connection, "{[Store].[All Stores].Parent.Parent}", "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testParentPC(Context context) {
    //final Context testContext = getContext().withCube( "HR" );
    Connection connection = context.getConnection();
    assertAxisReturns(connection, "HR",
      "[Employees].Parent",
      "" );
    assertAxisReturns(connection, "HR",
      "[Employees].[Sheri Nowmer].Parent",
      "[Employees].[All Employees]" );
    assertAxisReturns(connection, "HR",
      "[Employees].[Sheri Nowmer].[Derrick Whelply].Parent",
      "[Employees].[Sheri Nowmer]" );
    assertAxisReturns(connection, "HR",
      "[Employees].Members.Item(3)",
      "[Employees].[Sheri Nowmer].[Derrick Whelply].[Beverly Baker]" );
    assertAxisReturns(connection, "HR",
      "[Employees].Members.Item(3).Parent",
      "[Employees].[Sheri Nowmer].[Derrick Whelply]" );
    assertAxisReturns(connection, "HR",
      "[Employees].AllMembers.Item(3).Parent",
      "[Employees].[Sheri Nowmer].[Derrick Whelply]" );

    // Ascendants(<Member>) applied to parent-child hierarchy accessed via
    // <Level>.Members
    assertAxisReturns(connection, "HR",
      "Ascendants([Employees].Members.Item(73))",
      "[Employees].[Sheri Nowmer].[Derrick Whelply].[Beverly Baker].[Jacqueline Wyllie].[Ralph Mccoy].[Bertha "
        + "Jameson].[James Bailey]\n"
        + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Beverly Baker].[Jacqueline Wyllie].[Ralph Mccoy].[Bertha "
        + "Jameson]\n"
        + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Beverly Baker].[Jacqueline Wyllie].[Ralph Mccoy]\n"
        + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Beverly Baker].[Jacqueline Wyllie]\n"
        + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Beverly Baker]\n"
        + "[Employees].[Sheri Nowmer].[Derrick Whelply]\n"
        + "[Employees].[Sheri Nowmer]\n"
        + "[Employees].[All Employees]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMembers(Context context) {
    Connection connection = context.getConnection();
    // <Level>.members
    assertAxisReturns(connection,
      "{[Customers].[Country].Members}",
      "[Customers].[Canada]\n"
        + "[Customers].[Mexico]\n"
        + "[Customers].[USA]" );

    // <Level>.members applied to 'all' level
    assertAxisReturns(connection,
      "{[Customers].[(All)].Members}", "[Customers].[All Customers]" );

    // <Level>.members applied to measures dimension
    // Note -- no cube-level calculated members are present
    assertAxisReturns(connection,
      "{[Measures].[MeasuresLevel].Members}",
      "[Measures].[Unit Sales]\n"
        + "[Measures].[Store Cost]\n"
        + "[Measures].[Store Sales]\n"
        + "[Measures].[Sales Count]\n"
        + "[Measures].[Customer Count]\n"
        + "[Measures].[Promotion Sales]" );

    // <Dimension>.members applied to Measures
    assertAxisReturns(connection,
      "{[Measures].Members}",
      "[Measures].[Unit Sales]\n"
        + "[Measures].[Store Cost]\n"
        + "[Measures].[Store Sales]\n"
        + "[Measures].[Sales Count]\n"
        + "[Measures].[Customer Count]\n"
        + "[Measures].[Promotion Sales]" );

    // <Dimension>.members applied to a query with calc measures
    // Again, no calc measures are returned
    switch (getDatabaseProduct(TestUtil.getDialect(connection).getDialectName())) {
      case INFOBRIGHT:
        // Skip this test on Infobright, because [Promotion Sales] is
        // defined wrong.
        break;
      default:
        assertQueryReturns(connection,
          "with member [Measures].[Xxx] AS ' [Measures].[Unit Sales] '"
            + "select {[Measures].members} on columns from [Sales]",
          "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Store Cost]}\n"
            + "{[Measures].[Store Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "{[Measures].[Customer Count]}\n"
            + "{[Measures].[Promotion Sales]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: 225,627.23\n"
            + "Row #0: 565,238.13\n"
            + "Row #0: 86,837\n"
            + "Row #0: 5,581\n"
            + "Row #0: 151,211.21\n" );
    }

    // <Level>.members applied to a query with calc measures
    // Again, no calc measures are returned
    switch (getDatabaseProduct(TestUtil.getDialect(connection).getDialectName())) {
      case INFOBRIGHT:
        // Skip this test on Infobright, because [Promotion Sales] is
        // defined wrong.
        break;
      default:
        assertQueryReturns(connection,
          "with member [Measures].[Xxx] AS ' [Measures].[Unit Sales] '"
            + "select {[Measures].[Measures].members} on columns from [Sales]",
          "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Store Cost]}\n"
            + "{[Measures].[Store Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "{[Measures].[Customer Count]}\n"
            + "{[Measures].[Promotion Sales]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: 225,627.23\n"
            + "Row #0: 565,238.13\n"
            + "Row #0: 86,837\n"
            + "Row #0: 5,581\n"
            + "Row #0: 151,211.21\n" );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHierarchyMembers(Context context) {
    Connection connection = context.getConnection();
    assertAxisReturns(connection,
      "Head({[Time.Weekly].Members}, 10)",
      "[Time].[Weekly].[All Weeklys]\n"
        + "[Time].[Weekly].[1997]\n"
        + "[Time].[Weekly].[1997].[1]\n"
        + "[Time].[Weekly].[1997].[1].[15]\n"
        + "[Time].[Weekly].[1997].[1].[16]\n"
        + "[Time].[Weekly].[1997].[1].[17]\n"
        + "[Time].[Weekly].[1997].[1].[18]\n"
        + "[Time].[Weekly].[1997].[1].[19]\n"
        + "[Time].[Weekly].[1997].[1].[20]\n"
        + "[Time].[Weekly].[1997].[2]" );
    assertAxisReturns(connection,
      "Tail({[Time.Weekly].Members}, 5)",
      "[Time].[Weekly].[1998].[51].[5]\n"
        + "[Time].[Weekly].[1998].[51].[29]\n"
        + "[Time].[Weekly].[1998].[51].[30]\n"
        + "[Time].[Weekly].[1998].[52]\n"
        + "[Time].[Weekly].[1998].[52].[6]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAllMembers(Context context) {
    Connection connection = context.getConnection();
    // <Level>.allmembers
    assertAxisReturns(connection,
      "{[Customers].[Country].allmembers}",
      "[Customers].[Canada]\n"
        + "[Customers].[Mexico]\n"
        + "[Customers].[USA]" );

    // <Level>.allmembers applied to 'all' level
    assertAxisReturns(connection,
      "{[Customers].[(All)].allmembers}", "[Customers].[All Customers]" );

    // <Level>.allmembers applied to measures dimension
    // Note -- cube-level calculated members ARE present
    assertAxisReturns(connection,
      "{[Measures].[MeasuresLevel].allmembers}",
      "[Measures].[Unit Sales]\n"
        + "[Measures].[Store Cost]\n"
        + "[Measures].[Store Sales]\n"
        + "[Measures].[Sales Count]\n"
        + "[Measures].[Customer Count]\n"
        + "[Measures].[Promotion Sales]\n"
        + "[Measures].[Profit]\n"
        + "[Measures].[Profit Growth]\n"
        + "[Measures].[Profit last Period]" );

    // <Dimension>.allmembers applied to Measures
    assertAxisReturns(connection,
      "{[Measures].allmembers}",
      "[Measures].[Unit Sales]\n"
        + "[Measures].[Store Cost]\n"
        + "[Measures].[Store Sales]\n"
        + "[Measures].[Sales Count]\n"
        + "[Measures].[Customer Count]\n"
        + "[Measures].[Promotion Sales]\n"
        + "[Measures].[Profit]\n"
        + "[Measures].[Profit Growth]\n"
        + "[Measures].[Profit last Period]" );

    // <Dimension>.allmembers applied to a query with calc measures
    // Calc measures are returned
    switch (getDatabaseProduct(TestUtil.getDialect(connection).getDialectName())) {
      case INFOBRIGHT:
        // Skip this test on Infobright, because [Promotion Sales] is
        // defined wrong.
        break;
      default:
        assertQueryReturns(connection,
          "with member [Measures].[Xxx] AS ' [Measures].[Unit Sales] '"
            + "select {[Measures].allmembers} on columns from [Sales]",
          "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Store Cost]}\n"
            + "{[Measures].[Store Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "{[Measures].[Customer Count]}\n"
            + "{[Measures].[Promotion Sales]}\n"
            + "{[Measures].[Profit]}\n"
            + "{[Measures].[Profit Growth]}\n"
            + "{[Measures].[Profit last Period]}\n"
            + "{[Measures].[Xxx]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: 225,627.23\n"
            + "Row #0: 565,238.13\n"
            + "Row #0: 86,837\n"
            + "Row #0: 5,581\n"
            + "Row #0: 151,211.21\n"
            + "Row #0: $339,610.90\n"
            + "Row #0: 0.0%\n"
            + "Row #0: $339,610.90\n"
            + "Row #0: 266,773\n" );
    }

    // Calc measure members from schema and from query
    switch (getDatabaseProduct(TestUtil.getDialect(connection).getDialectName())) {
      case INFOBRIGHT:
        // Skip this test on Infobright, because [Promotion Sales] is
        // defined wrong.
        break;
      default:
        assertQueryReturns(context.getConnection(),
          "WITH MEMBER [Measures].[Unit to Sales ratio] as\n"
            + " '[Measures].[Unit Sales] / [Measures].[Store Sales]', FORMAT_STRING='0.0%' "
            + "SELECT {[Measures].AllMembers} ON COLUMNS,"
            + "non empty({[Store].[Store State].Members}) ON ROWS "
            + "FROM Sales "
            + "WHERE ([1997].[Q1])",
          "Axis #0:\n"
            + "{[Time].[1997].[Q1]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Store Cost]}\n"
            + "{[Measures].[Store Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "{[Measures].[Customer Count]}\n"
            + "{[Measures].[Promotion Sales]}\n"
            + "{[Measures].[Profit]}\n"
            + "{[Measures].[Profit Growth]}\n"
            + "{[Measures].[Profit last Period]}\n"
            + "{[Measures].[Unit to Sales ratio]}\n"
            + "Axis #2:\n"
            + "{[Store].[USA].[CA]}\n"
            + "{[Store].[USA].[OR]}\n"
            + "{[Store].[USA].[WA]}\n"
            + "Row #0: 16,890\n"
            + "Row #0: 14,431.09\n"
            + "Row #0: 36,175.20\n"
            + "Row #0: 5,498\n"
            + "Row #0: 1,110\n"
            + "Row #0: 14,447.16\n"
            + "Row #0: $21,744.11\n"
            + "Row #0: 0.0%\n"
            + "Row #0: $21,744.11\n"
            + "Row #0: 46.7%\n"
            + "Row #1: 19,287\n"
            + "Row #1: 16,081.07\n"
            + "Row #1: 40,170.29\n"
            + "Row #1: 6,184\n"
            + "Row #1: 767\n"
            + "Row #1: 10,829.64\n"
            + "Row #1: $24,089.22\n"
            + "Row #1: 0.0%\n"
            + "Row #1: $24,089.22\n"
            + "Row #1: 48.0%\n"
            + "Row #2: 30,114\n"
            + "Row #2: 25,240.08\n"
            + "Row #2: 63,282.86\n"
            + "Row #2: 9,906\n"
            + "Row #2: 1,104\n"
            + "Row #2: 18,459.60\n"
            + "Row #2: $38,042.78\n"
            + "Row #2: 0.0%\n"
            + "Row #2: $38,042.78\n"
            + "Row #2: 47.6%\n" );
    }

    // Calc member in query and schema not seen
    switch (getDatabaseProduct(TestUtil.getDialect(connection).getDialectName())) {
      case INFOBRIGHT:
        // Skip this test on Infobright, because [Promotion Sales] is
        // defined wrong.
        break;
      default:
        assertQueryReturns(context.getConnection(),
          "WITH MEMBER [Measures].[Unit to Sales ratio] as '[Measures].[Unit Sales] / [Measures].[Store Sales]', "
            + "FORMAT_STRING='0.0%' "
            + "SELECT {[Measures].AllMembers} ON COLUMNS,"
            + "non empty({[Store].[Store State].Members}) ON ROWS "
            + "FROM Sales "
            + "WHERE ([1997].[Q1])",
          "Axis #0:\n"
            + "{[Time].[1997].[Q1]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Store Cost]}\n"
            + "{[Measures].[Store Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "{[Measures].[Customer Count]}\n"
            + "{[Measures].[Promotion Sales]}\n"
            + "{[Measures].[Profit]}\n"
            + "{[Measures].[Profit Growth]}\n"
            + "{[Measures].[Profit last Period]}\n"
            + "{[Measures].[Unit to Sales ratio]}\n"
            + "Axis #2:\n"
            + "{[Store].[USA].[CA]}\n"
            + "{[Store].[USA].[OR]}\n"
            + "{[Store].[USA].[WA]}\n"
            + "Row #0: 16,890\n"
            + "Row #0: 14,431.09\n"
            + "Row #0: 36,175.20\n"
            + "Row #0: 5,498\n"
            + "Row #0: 1,110\n"
            + "Row #0: 14,447.16\n"
            + "Row #0: $21,744.11\n"
            + "Row #0: 0.0%\n"
            + "Row #0: $21,744.11\n"
            + "Row #0: 46.7%\n"
            + "Row #1: 19,287\n"
            + "Row #1: 16,081.07\n"
            + "Row #1: 40,170.29\n"
            + "Row #1: 6,184\n"
            + "Row #1: 767\n"
            + "Row #1: 10,829.64\n"
            + "Row #1: $24,089.22\n"
            + "Row #1: 0.0%\n"
            + "Row #1: $24,089.22\n"
            + "Row #1: 48.0%\n"
            + "Row #2: 30,114\n"
            + "Row #2: 25,240.08\n"
            + "Row #2: 63,282.86\n"
            + "Row #2: 9,906\n"
            + "Row #2: 1,104\n"
            + "Row #2: 18,459.60\n"
            + "Row #2: $38,042.78\n"
            + "Row #2: 0.0%\n"
            + "Row #2: $38,042.78\n"
            + "Row #2: 47.6%\n" );
    }

    // Calc member in query and schema not seen
    switch (getDatabaseProduct(TestUtil.getDialect(connection).getDialectName())) {
      case INFOBRIGHT:
        // Skip this test on Infobright, because [Promotion Sales] is
        // defined wrong.
        break;
      default:
        assertQueryReturns(context.getConnection(),
          "WITH MEMBER [Measures].[Unit to Sales ratio] as '[Measures].[Unit Sales] / [Measures].[Store Sales]', "
            + "FORMAT_STRING='0.0%' "
            + "SELECT {[Measures].Members} ON COLUMNS,"
            + "non empty({[Store].[Store State].Members}) ON ROWS "
            + "FROM Sales "
            + "WHERE ([1997].[Q1])",
          "Axis #0:\n"
            + "{[Time].[1997].[Q1]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Store Cost]}\n"
            + "{[Measures].[Store Sales]}\n"
            + "{[Measures].[Sales Count]}\n"
            + "{[Measures].[Customer Count]}\n"
            + "{[Measures].[Promotion Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[USA].[CA]}\n"
            + "{[Store].[USA].[OR]}\n"
            + "{[Store].[USA].[WA]}\n"
            + "Row #0: 16,890\n"
            + "Row #0: 14,431.09\n"
            + "Row #0: 36,175.20\n"
            + "Row #0: 5,498\n"
            + "Row #0: 1,110\n"
            + "Row #0: 14,447.16\n"
            + "Row #1: 19,287\n"
            + "Row #1: 16,081.07\n"
            + "Row #1: 40,170.29\n"
            + "Row #1: 6,184\n"
            + "Row #1: 767\n"
            + "Row #1: 10,829.64\n"
            + "Row #2: 30,114\n"
            + "Row #2: 25,240.08\n"
            + "Row #2: 63,282.86\n"
            + "Row #2: 9,906\n"
            + "Row #2: 1,104\n"
            + "Row #2: 18,459.60\n" );
    }

    // Calc member in dimension based on level
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Store].[USA].[CA plus OR] AS 'AGGREGATE({[Store].[USA].[CA], [Store].[USA].[OR]})' "
        + "SELECT {[Measures].[Unit Sales], [Measures].[Store Sales]} ON COLUMNS,"
        + "non empty({[Store].[Store State].AllMembers}) ON ROWS "
        + "FROM Sales "
        + "WHERE ([1997].[Q1])",
      "Axis #0:\n"
        + "{[Time].[1997].[Q1]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Store Sales]}\n"
        + "Axis #2:\n"
        + "{[Store].[USA].[CA]}\n"
        + "{[Store].[USA].[OR]}\n"
        + "{[Store].[USA].[WA]}\n"
        + "{[Store].[USA].[CA plus OR]}\n"
        + "Row #0: 16,890\n"
        + "Row #0: 36,175.20\n"
        + "Row #1: 19,287\n"
        + "Row #1: 40,170.29\n"
        + "Row #2: 30,114\n"
        + "Row #2: 63,282.86\n"
        + "Row #3: 36,177\n"
        + "Row #3: 76,345.49\n" );

    // Calc member in dimension based on level not seen
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Store].[USA].[CA plus OR] AS 'AGGREGATE({[Store].[USA].[CA], [Store].[USA].[OR]})' "
        + "SELECT {[Measures].[Unit Sales], [Measures].[Store Sales]} ON COLUMNS,"
        + "non empty({[Store].[Store Country].AllMembers}) ON ROWS "
        + "FROM Sales "
        + "WHERE ([1997].[Q1])",
      "Axis #0:\n"
        + "{[Time].[1997].[Q1]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Store Sales]}\n"
        + "Axis #2:\n"
        + "{[Store].[USA]}\n"
        + "Row #0: 66,291\n"
        + "Row #0: 139,628.35\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAddCalculatedMembers(Context context) {
    //----------------------------------------------------
    // AddCalculatedMembers: Calc member in dimension based on level
    // included
    //----------------------------------------------------
    Connection connection = context.getConnection();
    assertQueryReturns(connection,
      "WITH MEMBER [Store].[USA].[CA plus OR] AS 'AGGREGATE({[Store].[USA].[CA], [Store].[USA].[OR]})' "
        + "SELECT {[Measures].[Unit Sales], [Measures].[Store Sales]} ON COLUMNS,"
        + "AddCalculatedMembers([Store].[USA].Children) ON ROWS "
        + "FROM Sales "
        + "WHERE ([1997].[Q1])",
      "Axis #0:\n"
        + "{[Time].[1997].[Q1]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Store Sales]}\n"
        + "Axis #2:\n"
        + "{[Store].[USA].[CA]}\n"
        + "{[Store].[USA].[OR]}\n"
        + "{[Store].[USA].[WA]}\n"
        + "{[Store].[USA].[CA plus OR]}\n"
        + "Row #0: 16,890\n"
        + "Row #0: 36,175.20\n"
        + "Row #1: 19,287\n"
        + "Row #1: 40,170.29\n"
        + "Row #2: 30,114\n"
        + "Row #2: 63,282.86\n"
        + "Row #3: 36,177\n"
        + "Row #3: 76,345.49\n" );
    //----------------------------------------------------
    // Calc member in dimension based on level included
    // Calc members in measures in schema included
    //----------------------------------------------------
    assertQueryReturns(connection,
      "WITH MEMBER [Store].[USA].[CA plus OR] AS 'AGGREGATE({[Store].[USA].[CA], [Store].[USA].[OR]})' "
        + "SELECT AddCalculatedMembers({[Measures].[Unit Sales], [Measures].[Store Sales]}) ON COLUMNS,"
        + "AddCalculatedMembers([Store].[USA].Children) ON ROWS "
        + "FROM Sales "
        + "WHERE ([1997].[Q1])",
      "Axis #0:\n"
        + "{[Time].[1997].[Q1]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Store Sales]}\n"
        + "{[Measures].[Profit]}\n"
        + "{[Measures].[Profit last Period]}\n"
        + "{[Measures].[Profit Growth]}\n"
        + "Axis #2:\n"
        + "{[Store].[USA].[CA]}\n"
        + "{[Store].[USA].[OR]}\n"
        + "{[Store].[USA].[WA]}\n"
        + "{[Store].[USA].[CA plus OR]}\n"
        + "Row #0: 16,890\n"
        + "Row #0: 36,175.20\n"
        + "Row #0: $21,744.11\n"
        + "Row #0: $21,744.11\n"
        + "Row #0: 0.0%\n"
        + "Row #1: 19,287\n"
        + "Row #1: 40,170.29\n"
        + "Row #1: $24,089.22\n"
        + "Row #1: $24,089.22\n"
        + "Row #1: 0.0%\n"
        + "Row #2: 30,114\n"
        + "Row #2: 63,282.86\n"
        + "Row #2: $38,042.78\n"
        + "Row #2: $38,042.78\n"
        + "Row #2: 0.0%\n"
        + "Row #3: 36,177\n"
        + "Row #3: 76,345.49\n"
        + "Row #3: $45,833.33\n"
        + "Row #3: $45,833.33\n"
        + "Row #3: 0.0%\n" );
    //----------------------------------------------------
    // Two dimensions
    //----------------------------------------------------
    assertQueryReturns(connection,
      "SELECT AddCalculatedMembers({[Measures].[Unit Sales], [Measures].[Store Sales]}) ON COLUMNS,"
        + "{([Store].[USA].[CA], [Gender].[F])} ON ROWS "
        + "FROM Sales "
        + "WHERE ([1997].[Q1])",
      "Axis #0:\n"
        + "{[Time].[1997].[Q1]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Store Sales]}\n"
        + "{[Measures].[Profit]}\n"
        + "{[Measures].[Profit last Period]}\n"
        + "{[Measures].[Profit Growth]}\n"
        + "Axis #2:\n"
        + "{[Store].[USA].[CA], [Gender].[F]}\n"
        + "Row #0: 8,218\n"
        + "Row #0: 17,928.37\n"
        + "Row #0: $10,771.98\n"
        + "Row #0: $10,771.98\n"
        + "Row #0: 0.0%\n" );
    //----------------------------------------------------
    // Should throw more than one dimension error
    //----------------------------------------------------

    assertAxisThrows(connection,
      "AddCalculatedMembers({([Store].[USA].[CA], [Gender].[F])})",
      "Only single dimension members allowed in Set for AddCalculatedMembers");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStripCalculatedMembers(Context context) {
    Connection connection = context.getConnection();
    assertAxisReturns(connection,
      "StripCalculatedMembers({[Measures].AllMembers})",
      "[Measures].[Unit Sales]\n"
        + "[Measures].[Store Cost]\n"
        + "[Measures].[Store Sales]\n"
        + "[Measures].[Sales Count]\n"
        + "[Measures].[Customer Count]\n"
        + "[Measures].[Promotion Sales]" );

    // applied to empty set
    assertAxisReturns(connection, "StripCalculatedMembers({[Gender].Parent})", "" );

    assertSetExprDependsOn(connection,
      "StripCalculatedMembers([Customers].CurrentMember.Children)",
      "{[Customers]}" );

    // ----------------------------------------------------
    // Calc members in dimension based on level stripped
    // Actual members in measures left alone
    // ----------------------------------------------------
    assertQueryReturns(connection,
      "WITH MEMBER [Store].[USA].[CA plus OR] AS "
        + "'AGGREGATE({[Store].[USA].[CA], [Store].[USA].[OR]})' "
        + "SELECT StripCalculatedMembers({[Measures].[Unit Sales], "
        + "[Measures].[Store Sales]}) ON COLUMNS,"
        + "StripCalculatedMembers("
        + "AddCalculatedMembers([Store].[USA].Children)) ON ROWS "
        + "FROM Sales "
        + "WHERE ([1997].[Q1])",
      "Axis #0:\n"
        + "{[Time].[1997].[Q1]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Store Sales]}\n"
        + "Axis #2:\n"
        + "{[Store].[USA].[CA]}\n"
        + "{[Store].[USA].[OR]}\n"
        + "{[Store].[USA].[WA]}\n"
        + "Row #0: 16,890\n"
        + "Row #0: 36,175.20\n"
        + "Row #1: 19,287\n"
        + "Row #1: 40,170.29\n"
        + "Row #2: 30,114\n"
        + "Row #2: 63,282.86\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCurrentMember(Context context) {
    // <Dimension>.CurrentMember
    Connection connection = context.getConnection();
    assertAxisReturns(connection, "[Gender].CurrentMember", "[Gender].[All Gender]" );
    // <Hierarchy>.CurrentMember
    assertAxisReturns(connection,
      "[Gender].Hierarchy.CurrentMember", "[Gender].[All Gender]" );

    // <Level>.CurrentMember
    // MSAS doesn't allow this, but Mondrian does: it implicitly casts
    // level to hierarchy.
    assertAxisReturns(connection, "[Store Name].CurrentMember", "[Store].[All Stores]" );
  }

  @Disabled //disabled for CI build
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCurrentMemberDepends(Context context) {
    Connection connection = context.getConnection();
    assertMemberExprDependsOn(connection,
      "[Gender].CurrentMember",
      "{[Gender]}" );

    assertExprDependsOn(connection,
      "[Gender].[M].Dimension.Name", "{}" );
    // implicit call to .CurrentMember when dimension is used as a member
    // expression
    assertMemberExprDependsOn(connection,
      "[Gender].[M].Dimension",
      "{[Gender]}" );

    assertMemberExprDependsOn(connection,
      "[Gender].[M].Dimension.CurrentMember", "{[Gender]}" );
    assertMemberExprDependsOn(connection,
      "[Gender].[M].Dimension.CurrentMember.Parent", "{[Gender]}" );

    // [Customers] is short for [Customers].CurrentMember, so
    // depends upon everything
    assertExprDependsOn(connection,
      "[Customers]", allHiers() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCurrentMemberFromSlicer(Context context) {
    Result result = executeQuery(context.getConnection(),
      "with member [Measures].[Foo] as '[Gender].CurrentMember.Name'\n"
        + "select {[Measures].[Foo]} on columns\n"
        + "from Sales where ([Gender].[F])" );
    assertEquals( "F", result.getCell( new int[] { 0 } ).getValue() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCurrentMemberFromDefaultMember(Context context) {
    Result result = executeQuery(context.getConnection(),
      "with member [Measures].[Foo] as"
        + " '[Time].[Time].CurrentMember.Name'\n"
        + "select {[Measures].[Foo]} on columns\n"
        + "from Sales" );
    assertEquals( "1997", result.getCell( new int[] { 0 } ).getValue() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCurrentMemberMultiHierarchy(Context context) {
    final String hierarchyName =
      SystemWideProperties.instance().SsasCompatibleNaming
        ? "Weekly"
        : "Time.Weekly";
    final String queryString =
      "with member [Measures].[Foo] as\n"
        + " 'IIf(([Time].[Time].CurrentMember.Hierarchy.Name = \""
        + hierarchyName
        + "\"), \n"
        + "[Measures].[Unit Sales], \n"
        + "- [Measures].[Unit Sales])'\n"
        + "select {[Measures].[Unit Sales], [Measures].[Foo]} ON COLUMNS,\n"
        + "  {[Product].[Food].[Dairy]} ON ROWS\n"
        + "from [Sales]";
    Connection connection = context.getConnection();
    Result result =
      executeQuery(connection,
        queryString + " where [Time].[1997]" );
    final int[] coords = { 1, 0 };
    assertEquals(
      "-12,885",
      result.getCell( coords ).getFormattedValue() );

    // As above, but context provided on rows axis as opposed to slicer.
    final String queryString1 =
      "with member [Measures].[Foo] as\n"
        + " 'IIf(([Time].[Time].CurrentMember.Hierarchy.Name = \""
        + hierarchyName
        + "\"), \n"
        + "[Measures].[Unit Sales], \n"
        + "- [Measures].[Unit Sales])'\n"
        + "select {[Measures].[Unit Sales], [Measures].[Foo]} ON COLUMNS,";

    final String queryString2 =
      "from [Sales]\n"
        + "  where [Product].[Food].[Dairy] ";

    result =
      executeQuery(connection,
        queryString1 + " {[Time].[1997]} ON ROWS " + queryString2 );
    assertEquals(
      "-12,885",
      result.getCell( coords ).getFormattedValue() );

    result =
      executeQuery(connection,
        queryString + " where [Time.Weekly].[1997]" );
    assertEquals(
      "-12,885",
      result.getCell( coords ).getFormattedValue() );

    result =
      executeQuery(connection,
        queryString1 + " {[Time.Weekly].[1997]} ON ROWS "
          + queryString2 );
    assertEquals(
      "-12,885",
      result.getCell( coords ).getFormattedValue() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDefaultMember(Context context) {
    // [Time] has no default member and no all, so the default member is
    // the first member of the first level.
    Result result =
      executeQuery(context.getConnection(),
        "select {[Time].[Time].DefaultMember} on columns\n"
          + "from Sales" );
    assertEquals(
      "1997",
      result.getAxes()[ 0 ].getPositions().get( 0 ).get( 0 ).getName() );

    // [Time].[Weekly] has an all member and no explicit default.
    result =
      executeQuery(context.getConnection(),
        "select {[Time.Weekly].DefaultMember} on columns\n"
          + "from Sales" );
    assertEquals(
      SystemWideProperties.instance().SsasCompatibleNaming
        ? "All Weeklys"
        : "All Time.Weeklys",
      result.getAxes()[ 0 ].getPositions().get( 0 ).get( 0 ).getName() );

    final String memberUname =
      SystemWideProperties.instance().SsasCompatibleNaming
        ? "[Time2].[Weekly].[1997].[23]"
        : "[Time2.Weekly].[1997].[23]";
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
      "Sales",
      "  <Dimension name=\"Time2\" type=\"TimeDimension\" foreignKey=\"time_id\">\n"
        + "    <Hierarchy hasAll=\"false\" primaryKey=\"time_id\">\n"
        + "      <Table name=\"time_by_day\"/>\n"
        + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
        + "          levelType=\"TimeYears\"/>\n"
        + "      <Level name=\"Quarter\" column=\"quarter\" uniqueMembers=\"false\"\n"
        + "          levelType=\"TimeQuarters\"/>\n"
        + "      <Level name=\"Month\" column=\"month_of_year\" uniqueMembers=\"false\" type=\"Numeric\"\n"
        + "          levelType=\"TimeMonths\"/>\n"
        + "    </Hierarchy>\n"
        + "    <Hierarchy hasAll=\"true\" name=\"Weekly\" primaryKey=\"time_id\"\n"
        + "          defaultMember=\""
        + memberUname
        + "\">\n"
        + "      <Table name=\"time_by_day\"/>\n"
        + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
        + "          levelType=\"TimeYears\"/>\n"
        + "      <Level name=\"Week\" column=\"week_of_year\" type=\"Numeric\" uniqueMembers=\"false\"\n"
        + "          levelType=\"TimeWeeks\"/>\n"
        + "      <Level name=\"Day\" column=\"day_of_month\" uniqueMembers=\"false\" type=\"Numeric\"\n"
        + "          levelType=\"TimeDays\"/>\n"
        + "    </Hierarchy>\n"
        + "  </Dimension>" ));
      */
      class TestDefaultMemberModifier extends PojoMappingModifier {

          public TestDefaultMemberModifier(CatalogMapping catalogMapping) {
              super(catalogMapping);
          }
          
          protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
              List<DimensionConnectorMapping> result = new ArrayList<>();
              result.addAll(super.cubeDimensionConnectors(cube));
              if ("Sales".equals(cube.getName())) {
            	  TimeDimensionMappingImpl dimension = TimeDimensionMappingImpl
                      .builder()
                      .withName("Time2")
                      //.withForeignKey("time_id")
                      .withHierarchies(List.of(
                          HierarchyMappingImpl.builder()
                              .withHasAll(false)
                              .withPrimaryKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY)
                              .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.TIME_BY_DAY_TABLE).build())
                              .withLevels(List.of(
                                  LevelMappingImpl.builder()
                                      .withName("Year")
                                      .withColumn(FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY)
                                      .withType(DataType.NUMERIC)
                                      .withUniqueMembers(true)
                                      .withLevelType(LevelType.TIME_YEARS)
                                      .build(),
                                  LevelMappingImpl.builder()
                                      .withName("Quarter")
                                      .withColumn(FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY)
                                      .withUniqueMembers(false)
                                      .withLevelType(LevelType.TIME_QUARTERS)
                                      .build(),
                                  LevelMappingImpl.builder()
                                      .withName("Month")
                                      .withColumn(FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY)
                                      .withUniqueMembers(false)
                                      .withType(DataType.NUMERIC)
                                      .withLevelType(LevelType.TIME_MONTHS)
                                      .build()
                              ))
                              .build(),
                          HierarchyMappingImpl.builder()
                              .withHasAll(true)
                              .withName("Weekly")
                              .withPrimaryKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY)
                              .withDefaultMember(memberUname)
                              .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.TIME_BY_DAY_TABLE).build())
                              .withLevels(List.of(
                                  LevelMappingImpl.builder()
                                      .withName("Year")
                                      .withColumn(FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY)
                                      .withType(DataType.NUMERIC)
                                      .withUniqueMembers(true)
                                      .withLevelType(LevelType.TIME_YEARS)
                                      .build(),
                                  LevelMappingImpl.builder()
                                      .withName("Week")
                                      .withColumn(FoodmartMappingSupplier.WEEK_OF_YEAR_COLUMN_IN_TIME_BY_DAY)
                                      .withType(DataType.NUMERIC)
                                      .withUniqueMembers(false)
                                      .withLevelType(LevelType.TIME_WEEKS)
                                      .build(),
                                  LevelMappingImpl.builder()
                                      .withName("Day")
                                      .withColumn(FoodmartMappingSupplier.DAY_OF_MONTH_COLUMN_TIME_BY_DAY)
                                      .withUniqueMembers(false)
                                      .withType(DataType.NUMERIC)
                                      .withLevelType(LevelType.TIME_DAYS)
                                      .build()
                              ))
                              .build()
                      ))
                      .build();
                  result.add(DimensionConnectorMappingImpl.builder()
                		  .withOverrideDimensionName("Time2")
                		  .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                		  .withDimension(dimension)
                		  .build());
              }
              return result;
          }          
      }
      withSchema(context, TestDefaultMemberModifier::new);

      // In this variant of the schema, Time2.Weekly has an explicit default
    // member.
    result =
      executeQuery(context.getConnection(),
        "select {[Time2.Weekly].DefaultMember} on columns\n"
          + "from Sales" );
    assertEquals(
      "23",
      result.getAxes()[ 0 ].getPositions().get( 0 ).get( 0 ).getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCurrentMemberFromAxis(Context context) {
    Result result = executeQuery(context.getConnection(),
      "with member [Measures].[Foo] as"
        + " '[Gender].CurrentMember.Name"
        + " || [Marital Status].CurrentMember.Name'\n"
        + "select {[Measures].[Foo]} on columns,\n"
        + " CrossJoin({[Gender].children},"
        + "           {[Marital Status].children}) on rows\n"
        + "from Sales" );
    assertEquals( "FM", result.getCell( new int[] { 0, 0 } ).getValue() );
  }

  /**
   * When evaluating a calculated member, MSOLAP regards that calculated member as the current member of that dimension,
   * so it cycles in this case. But I disagree; it is the previous current member, before the calculated member was
   * expanded.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCurrentMemberInCalcMember(Context context) {
    Result result = executeQuery(context.getConnection(),
      "with member [Measures].[Foo] as '[Measures].CurrentMember.Name'\n"
        + "select {[Measures].[Foo]} on columns\n"
        + "from Sales" );
    assertEquals(
      "Unit Sales", result.getCell( new int[] { 0 } ).getValue() );
  }

  /**
   * Tests NamedSet.CurrentOrdinal combined with the Order function.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNamedSetCurrentOrdinalWithOrder(Context context) {
    // The <Named Set>.CurrentOrdinal only works correctly when named sets
    // are evaluated as iterables, and JDK 1.4 only supports lists.
    if ( Util.RETROWOVEN) {
      return;
    }
    assertQueryReturns(context.getConnection(),
      "with set [Time Regular] as [Time].[Time].Members\n"
        + " set [Time Reversed] as"
        + " Order([Time Regular], [Time Regular].CurrentOrdinal, BDESC)\n"
        + "select [Time Reversed] on 0\n"
        + "from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Time].[1998].[Q4].[12]}\n"
        + "{[Time].[1998].[Q4].[11]}\n"
        + "{[Time].[1998].[Q4].[10]}\n"
        + "{[Time].[1998].[Q4]}\n"
        + "{[Time].[1998].[Q3].[9]}\n"
        + "{[Time].[1998].[Q3].[8]}\n"
        + "{[Time].[1998].[Q3].[7]}\n"
        + "{[Time].[1998].[Q3]}\n"
        + "{[Time].[1998].[Q2].[6]}\n"
        + "{[Time].[1998].[Q2].[5]}\n"
        + "{[Time].[1998].[Q2].[4]}\n"
        + "{[Time].[1998].[Q2]}\n"
        + "{[Time].[1998].[Q1].[3]}\n"
        + "{[Time].[1998].[Q1].[2]}\n"
        + "{[Time].[1998].[Q1].[1]}\n"
        + "{[Time].[1998].[Q1]}\n"
        + "{[Time].[1998]}\n"
        + "{[Time].[1997].[Q4].[12]}\n"
        + "{[Time].[1997].[Q4].[11]}\n"
        + "{[Time].[1997].[Q4].[10]}\n"
        + "{[Time].[1997].[Q4]}\n"
        + "{[Time].[1997].[Q3].[9]}\n"
        + "{[Time].[1997].[Q3].[8]}\n"
        + "{[Time].[1997].[Q3].[7]}\n"
        + "{[Time].[1997].[Q3]}\n"
        + "{[Time].[1997].[Q2].[6]}\n"
        + "{[Time].[1997].[Q2].[5]}\n"
        + "{[Time].[1997].[Q2].[4]}\n"
        + "{[Time].[1997].[Q2]}\n"
        + "{[Time].[1997].[Q1].[3]}\n"
        + "{[Time].[1997].[Q1].[2]}\n"
        + "{[Time].[1997].[Q1].[1]}\n"
        + "{[Time].[1997].[Q1]}\n"
        + "{[Time].[1997]}\n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: 26,796\n"
        + "Row #0: 25,270\n"
        + "Row #0: 19,958\n"
        + "Row #0: 72,024\n"
        + "Row #0: 20,388\n"
        + "Row #0: 21,697\n"
        + "Row #0: 23,763\n"
        + "Row #0: 65,848\n"
        + "Row #0: 21,350\n"
        + "Row #0: 21,081\n"
        + "Row #0: 20,179\n"
        + "Row #0: 62,610\n"
        + "Row #0: 23,706\n"
        + "Row #0: 20,957\n"
        + "Row #0: 21,628\n"
        + "Row #0: 66,291\n"
        + "Row #0: 266,773\n" );
  }

  /**
   * Tests NamedSet.CurrentOrdinal combined with the Generate function.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNamedSetCurrentOrdinalWithGenerate(Context context) {
    // The <Named Set>.CurrentOrdinal only works correctly when named sets
    // are evaluated as iterables, and JDK 1.4 only supports lists.
    if ( Util.RETROWOVEN) {
      return;
    }
    assertQueryReturns(context.getConnection(),
      " with set [Time Regular] as [Time].[Time].Members\n"
        + "set [Every Other Time] as\n"
        + "  Generate(\n"
        + "    [Time Regular],\n"
        + "    {[Time].[Time].Members.Item(\n"
        + "      [Time Regular].CurrentOrdinal * 2)})\n"
        + "select [Every Other Time] on 0\n"
        + "from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Time].[1997]}\n"
        + "{[Time].[1997].[Q1].[1]}\n"
        + "{[Time].[1997].[Q1].[3]}\n"
        + "{[Time].[1997].[Q2].[4]}\n"
        + "{[Time].[1997].[Q2].[6]}\n"
        + "{[Time].[1997].[Q3].[7]}\n"
        + "{[Time].[1997].[Q3].[9]}\n"
        + "{[Time].[1997].[Q4].[10]}\n"
        + "{[Time].[1997].[Q4].[12]}\n"
        + "{[Time].[1998].[Q1]}\n"
        + "{[Time].[1998].[Q1].[2]}\n"
        + "{[Time].[1998].[Q2]}\n"
        + "{[Time].[1998].[Q2].[5]}\n"
        + "{[Time].[1998].[Q3]}\n"
        + "{[Time].[1998].[Q3].[8]}\n"
        + "{[Time].[1998].[Q4]}\n"
        + "{[Time].[1998].[Q4].[11]}\n"
        + "Row #0: 266,773\n"
        + "Row #0: 21,628\n"
        + "Row #0: 23,706\n"
        + "Row #0: 20,179\n"
        + "Row #0: 21,350\n"
        + "Row #0: 23,763\n"
        + "Row #0: 20,388\n"
        + "Row #0: 19,958\n"
        + "Row #0: 26,796\n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNamedSetCurrentOrdinalWithFilter(Context context) {
    // The <Named Set>.CurrentOrdinal only works correctly when named sets
    // are evaluated as iterables, and JDK 1.4 only supports lists.
    if ( Util.RETROWOVEN) {
      return;
    }
    assertQueryReturns(context.getConnection(),
      "with set [Time Regular] as [Time].[Time].Members\n"
        + " set [Time Subset] as "
        + "   Filter([Time Regular], [Time Regular].CurrentOrdinal = 3"
        + "                       or [Time Regular].CurrentOrdinal = 5)\n"
        + "select [Time Subset] on 0\n"
        + "from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Time].[1997].[Q1].[2]}\n"
        + "{[Time].[1997].[Q2]}\n"
        + "Row #0: 20,957\n"
        + "Row #0: 62,610\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNamedSetCurrentOrdinalWithCrossjoin(Context context) {
    // TODO:
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNamedSetCurrentOrdinalWithNonNamedSetFails(Context context) {
    // a named set wrapped in {...} is not a named set, so CurrentOrdinal
    // fails
    assertQueryThrows(context,
      "with set [Time Members] as [Time].Members\n"
        + "member [Measures].[Foo] as ' {[Time Members]}.CurrentOrdinal '\n"
        + "select {[Measures].[Unit Sales], [Measures].[Foo]} on 0,\n"
        + " {[Product].Children} on 1\n"
        + "from [Sales]",
      "Not a named set" );

    // as above for Current function
    assertQueryThrows(context,
      "with set [Time Members] as [Time].Members\n"
        + "member [Measures].[Foo] as ' {[Time Members]}.Current.Name '\n"
        + "select {[Measures].[Unit Sales], [Measures].[Foo]} on 0,\n"
        + " {[Product].Children} on 1\n"
        + "from [Sales]",
      "Not a named set" );

    // a set expression is not a named set, so CurrentOrdinal fails
    assertQueryThrows(context,
      "with member [Measures].[Foo] as\n"
        + " ' Head([Time].Members, 5).CurrentOrdinal '\n"
        + "select {[Measures].[Unit Sales], [Measures].[Foo]} on 0,\n"
        + " {[Product].Children} on 1\n"
        + "from [Sales]",
      "Not a named set" );

    // as above for Current function
    assertQueryThrows(context,
      "with member [Measures].[Foo] as\n"
        + " ' Crossjoin([Time].Members, [Gender].Members).Current.Name '\n"
        + "select {[Measures].[Unit Sales], [Measures].[Foo]} on 0,\n"
        + " {[Product].Children} on 1\n"
        + "from [Sales]",
      "Not a named set" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDimensionDefaultMember(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Measures].DefaultMember" );
    assertEquals( "Unit Sales", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDrilldownLevel(Context context) {
    // Expect all children of USA
    assertAxisReturns(context.getConnection(),
      "DrilldownLevel({[Store].[USA]}, [Store].[Store Country])",
      "[Store].[USA]\n"
        + "[Store].[USA].[CA]\n"
        + "[Store].[USA].[OR]\n"
        + "[Store].[USA].[WA]" );

    // Expect same set, because [USA] is already drilled
    assertAxisReturns(context.getConnection(),
      "DrilldownLevel({[Store].[USA], [Store].[USA].[CA]}, [Store].[Store Country])",
      "[Store].[USA]\n"
        + "[Store].[USA].[CA]" );

    // Expect drill, because [USA] isn't already drilled. You can't
    // drill down on [CA] and get to [USA]
    assertAxisReturns(context.getConnection(),
      "DrilldownLevel({[Store].[USA].[CA],[Store].[USA]}, [Store].[Store Country])",
      "[Store].[USA].[CA]\n"
        + "[Store].[USA]\n"
        + "[Store].[USA].[CA]\n"
        + "[Store].[USA].[OR]\n"
        + "[Store].[USA].[WA]" );

    assertAxisReturns(context.getConnection(),
      "DrilldownLevel({[Store].[USA].[CA],[Store].[USA]},, 0)",
      "[Store].[USA].[CA]\n"
        + "[Store].[USA].[CA].[Alameda]\n"
        + "[Store].[USA].[CA].[Beverly Hills]\n"
        + "[Store].[USA].[CA].[Los Angeles]\n"
        + "[Store].[USA].[CA].[San Diego]\n"
        + "[Store].[USA].[CA].[San Francisco]\n"
        + "[Store].[USA]\n"
        + "[Store].[USA].[CA]\n"
        + "[Store].[USA].[OR]\n"
        + "[Store].[USA].[WA]" );

    assertAxisReturns(context.getConnection(),
      "DrilldownLevel({[Store].[USA].[CA],[Store].[USA]} * {[Gender].Members},, 0)",
      "{[Store].[USA].[CA], [Gender].[All Gender]}\n"
        + "{[Store].[USA].[CA].[Alameda], [Gender].[All Gender]}\n"
        + "{[Store].[USA].[CA].[Beverly Hills], [Gender].[All Gender]}\n"
        + "{[Store].[USA].[CA].[Los Angeles], [Gender].[All Gender]}\n"
        + "{[Store].[USA].[CA].[San Diego], [Gender].[All Gender]}\n"
        + "{[Store].[USA].[CA].[San Francisco], [Gender].[All Gender]}\n"
        + "{[Store].[USA].[CA], [Gender].[F]}\n"
        + "{[Store].[USA].[CA].[Alameda], [Gender].[F]}\n"
        + "{[Store].[USA].[CA].[Beverly Hills], [Gender].[F]}\n"
        + "{[Store].[USA].[CA].[Los Angeles], [Gender].[F]}\n"
        + "{[Store].[USA].[CA].[San Diego], [Gender].[F]}\n"
        + "{[Store].[USA].[CA].[San Francisco], [Gender].[F]}\n"
        + "{[Store].[USA].[CA], [Gender].[M]}\n"
        + "{[Store].[USA].[CA].[Alameda], [Gender].[M]}\n"
        + "{[Store].[USA].[CA].[Beverly Hills], [Gender].[M]}\n"
        + "{[Store].[USA].[CA].[Los Angeles], [Gender].[M]}\n"
        + "{[Store].[USA].[CA].[San Diego], [Gender].[M]}\n"
        + "{[Store].[USA].[CA].[San Francisco], [Gender].[M]}\n"
        + "{[Store].[USA], [Gender].[All Gender]}\n"
        + "{[Store].[USA].[CA], [Gender].[All Gender]}\n"
        + "{[Store].[USA].[OR], [Gender].[All Gender]}\n"
        + "{[Store].[USA].[WA], [Gender].[All Gender]}\n"
        + "{[Store].[USA], [Gender].[F]}\n"
        + "{[Store].[USA].[CA], [Gender].[F]}\n"
        + "{[Store].[USA].[OR], [Gender].[F]}\n"
        + "{[Store].[USA].[WA], [Gender].[F]}\n"
        + "{[Store].[USA], [Gender].[M]}\n"
        + "{[Store].[USA].[CA], [Gender].[M]}\n"
        + "{[Store].[USA].[OR], [Gender].[M]}\n"
        + "{[Store].[USA].[WA], [Gender].[M]}" );

    assertAxisReturns(context.getConnection(),
      "DrilldownLevel({[Store].[USA].[CA],[Store].[USA]} * {[Gender].Members},, 1)",
      "{[Store].[USA].[CA], [Gender].[All Gender]}\n"
        + "{[Store].[USA].[CA], [Gender].[F]}\n"
        + "{[Store].[USA].[CA], [Gender].[M]}\n"
        + "{[Store].[USA].[CA], [Gender].[F]}\n"
        + "{[Store].[USA].[CA], [Gender].[M]}\n"
        + "{[Store].[USA], [Gender].[All Gender]}\n"
        + "{[Store].[USA], [Gender].[F]}\n"
        + "{[Store].[USA], [Gender].[M]}\n"
        + "{[Store].[USA], [Gender].[F]}\n"
        + "{[Store].[USA], [Gender].[M]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDrilldownLevelTop(Context context) {
    // <set>, <n>, <level>
    assertAxisReturns(context.getConnection(),
      "DrilldownLevelTop({[Store].[USA]}, 2, [Store].[Store Country])",
      "[Store].[USA]\n"
        + "[Store].[USA].[WA]\n"
        + "[Store].[USA].[CA]" );

    // similarly DrilldownLevelBottom
    assertAxisReturns(context.getConnection(),
      "DrilldownLevelBottom({[Store].[USA]}, 2, [Store].[Store Country])",
      "[Store].[USA]\n"
        + "[Store].[USA].[OR]\n"
        + "[Store].[USA].[CA]" );

    // <set>, <n>
    assertAxisReturns(context.getConnection(),
      "DrilldownLevelTop({[Store].[USA]}, 2)",
      "[Store].[USA]\n"
        + "[Store].[USA].[WA]\n"
        + "[Store].[USA].[CA]" );

    // <n> greater than number of children
    assertAxisReturns(context.getConnection(),
      "DrilldownLevelTop({[Store].[USA], [Store].[Canada]}, 4)",
      "[Store].[USA]\n"
        + "[Store].[USA].[WA]\n"
        + "[Store].[USA].[CA]\n"
        + "[Store].[USA].[OR]\n"
        + "[Store].[Canada]\n"
        + "[Store].[Canada].[BC]" );

    // <n> negative
    assertAxisReturns(context.getConnection(),
      "DrilldownLevelTop({[Store].[USA]}, 2 - 3)",
      "[Store].[USA]" );

    // <n> zero
    assertAxisReturns(context.getConnection(),
      "DrilldownLevelTop({[Store].[USA]}, 2 - 2)",
      "[Store].[USA]" );

    // <n> null
    assertAxisReturns(context.getConnection(),
      "DrilldownLevelTop({[Store].[USA]}, null)",
      "[Store].[USA]" );

    // mixed bag, no level, all expanded
    assertAxisReturns(context.getConnection(),
      "DrilldownLevelTop({[Store].[USA], "
        + "[Store].[USA].[CA].[San Francisco], "
        + "[Store].[All Stores], "
        + "[Store].[Canada].[BC]}, "
        + "2)",
      "[Store].[USA]\n"
        + "[Store].[USA].[WA]\n"
        + "[Store].[USA].[CA]\n"
        + "[Store].[USA].[CA].[San Francisco]\n"
        + "[Store].[USA].[CA].[San Francisco].[Store 14]\n"
        + "[Store].[All Stores]\n"
        + "[Store].[USA]\n"
        + "[Store].[Canada]\n"
        + "[Store].[Canada].[BC]\n"
        + "[Store].[Canada].[BC].[Vancouver]\n"
        + "[Store].[Canada].[BC].[Victoria]" );

    // mixed bag, only specified level expanded
    assertAxisReturns(context.getConnection(),
      "DrilldownLevelTop({[Store].[USA], "
        + "[Store].[USA].[CA].[San Francisco], "
        + "[Store].[All Stores], "
        + "[Store].[Canada].[BC]}, 2, [Store].[Store City])",
      "[Store].[USA]\n"
        + "[Store].[USA].[CA].[San Francisco]\n"
        + "[Store].[USA].[CA].[San Francisco].[Store 14]\n"
        + "[Store].[All Stores]\n"
        + "[Store].[Canada].[BC]" );

    // bad level
    assertAxisThrows(context.getConnection(),
      "DrilldownLevelTop({[Store].[USA]}, 2, [Customers].[Country])",
      "Level '[Customers].[Country]' not compatible with "
        + "member '[Store].[USA]'" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDrilldownMemberEmptyExpr(Context context) {
    // no level, with expression
    assertAxisReturns(context.getConnection(),
      "DrilldownLevelTop({[Store].[USA]}, 2, , [Measures].[Unit Sales])",
      "[Store].[USA]\n"
        + "[Store].[USA].[WA]\n"
        + "[Store].[USA].[CA]" );

    // reverse expression
    assertAxisReturns(context.getConnection(),
      "DrilldownLevelTop("
        + "{[Store].[USA]}, 2, , - [Measures].[Unit Sales])",
      "[Store].[USA]\n"
        + "[Store].[USA].[OR]\n"
        + "[Store].[USA].[CA]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDrilldownMember(Context context) {
    // Expect all children of USA
    assertAxisReturns(context.getConnection(),
      "DrilldownMember({[Store].[USA]}, {[Store].[USA]})",
      "[Store].[USA]\n"
        + "[Store].[USA].[CA]\n"
        + "[Store].[USA].[OR]\n"
        + "[Store].[USA].[WA]" );

    // Expect all children of USA.CA and USA.OR
    assertAxisReturns(context.getConnection(),
      "DrilldownMember({[Store].[USA].[CA], [Store].[USA].[OR]}, "
        + "{[Store].[USA].[CA], [Store].[USA].[OR], [Store].[USA].[WA]})",
      "[Store].[USA].[CA]\n"
        + "[Store].[USA].[CA].[Alameda]\n"
        + "[Store].[USA].[CA].[Beverly Hills]\n"
        + "[Store].[USA].[CA].[Los Angeles]\n"
        + "[Store].[USA].[CA].[San Diego]\n"
        + "[Store].[USA].[CA].[San Francisco]\n"
        + "[Store].[USA].[OR]\n"
        + "[Store].[USA].[OR].[Portland]\n"
        + "[Store].[USA].[OR].[Salem]" );


    // Second set is empty
    assertAxisReturns(context.getConnection(),
      "DrilldownMember({[Store].[USA]}, {})",
      "[Store].[USA]" );

    // Drill down a leaf member
    assertAxisReturns(context.getConnection(),
      "DrilldownMember({[Store].[All Stores].[USA].[CA].[San Francisco].[Store 14]}, "
        + "{[Store].[USA].[CA].[San Francisco].[Store 14]})",
      "[Store].[USA].[CA].[San Francisco].[Store 14]" );

    // Complex case with option recursive
    assertAxisReturns(context.getConnection(),
      "DrilldownMember({[Store].[All Stores].[USA]}, "
        + "{[Store].[All Stores].[USA], [Store].[All Stores].[USA].[CA], "
        + "[Store].[All Stores].[USA].[CA].[San Diego], [Store].[All Stores].[USA].[WA]}, "
        + "RECURSIVE)",
      "[Store].[USA]\n"
        + "[Store].[USA].[CA]\n"
        + "[Store].[USA].[CA].[Alameda]\n"
        + "[Store].[USA].[CA].[Beverly Hills]\n"
        + "[Store].[USA].[CA].[Los Angeles]\n"
        + "[Store].[USA].[CA].[San Diego]\n"
        + "[Store].[USA].[CA].[San Diego].[Store 24]\n"
        + "[Store].[USA].[CA].[San Francisco]\n"
        + "[Store].[USA].[OR]\n"
        + "[Store].[USA].[WA]\n"
        + "[Store].[USA].[WA].[Bellingham]\n"
        + "[Store].[USA].[WA].[Bremerton]\n"
        + "[Store].[USA].[WA].[Seattle]\n"
        + "[Store].[USA].[WA].[Spokane]\n"
        + "[Store].[USA].[WA].[Tacoma]\n"
        + "[Store].[USA].[WA].[Walla Walla]\n"
        + "[Store].[USA].[WA].[Yakima]" );

    // Sets of tuples
    assertAxisReturns(context.getConnection(),
      "DrilldownMember({([Store Type].[Supermarket], [Store].[USA])}, {[Store].[USA]})",
      "{[Store Type].[Supermarket], [Store].[USA]}\n"
        + "{[Store Type].[Supermarket], [Store].[USA].[CA]}\n"
        + "{[Store Type].[Supermarket], [Store].[USA].[OR]}\n"
        + "{[Store Type].[Supermarket], [Store].[USA].[WA]}" );
  }


  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFirstChildFirstInLevel(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Time].[1997].[Q4].FirstChild" );
    assertEquals( "10", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFirstChildAll(Context context) {
    Member member =
      executeSingletonAxis(context.getConnection(), "[Gender].[All Gender].FirstChild" );
    assertEquals( "F", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFirstChildOfChildless(Context context) {
    Member member =
      executeSingletonAxis(context.getConnection(), "[Gender].[All Gender].[F].FirstChild" );
    assertNull( member );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFirstSiblingFirstInLevel(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Gender].[F].FirstSibling" );
    assertEquals( "F", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFirstSiblingLastInLevel(Context context) {
    Member member =
      executeSingletonAxis(context.getConnection(), "[Time].[1997].[Q4].FirstSibling" );
    assertEquals( "Q1", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFirstSiblingAll(Context context) {
    Member member =
      executeSingletonAxis(context.getConnection(), "[Gender].[All Gender].FirstSibling" );
    assertTrue( member.isAll() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFirstSiblingRoot(Context context) {
    // The [Measures] hierarchy does not have an 'all' member, so
    // [Unit Sales] does not have a parent.
    Member member =
      executeSingletonAxis(context.getConnection(), "[Measures].[Store Sales].FirstSibling" );
    assertEquals( "Unit Sales", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFirstSiblingNull(Context context) {
    Member member =
      executeSingletonAxis(context.getConnection(), "[Gender].[F].FirstChild.FirstSibling" );
    assertNull( member );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLag(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Time].[1997].[Q4].[12].Lag(4)" );
    assertEquals( "8", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLagFirstInLevel(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Gender].[F].Lag(1)" );
    assertNull( member );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLagAll(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Gender].DefaultMember.Lag(2)" );
    assertNull( member );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLagRoot(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Time].[1998].Lag(1)" );
    assertEquals( "1997", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLagRootTooFar(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Time].[1998].Lag(2)" );
    assertNull( member );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLastChild(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Gender].LastChild" );
    assertEquals( "M", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLastChildLastInLevel(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Time].[1997].[Q4].LastChild" );
    assertEquals( "12", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLastChildAll(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Gender].[All Gender].LastChild" );
    assertEquals( "M", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLastChildOfChildless(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Gender].[M].LastChild" );
    assertNull( member );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLastSibling(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Gender].[F].LastSibling" );
    assertEquals( "M", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLastSiblingFirstInLevel(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Time].[1997].[Q1].LastSibling" );
    assertEquals( "Q4", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLastSiblingAll(Context context) {
    Member member =
      executeSingletonAxis(context.getConnection(), "[Gender].[All Gender].LastSibling" );
    assertTrue( member.isAll() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLastSiblingRoot(Context context) {
    // The [Time] hierarchy does not have an 'all' member, so
    // [1997], [1998] do not have parents.
    Member member = executeSingletonAxis(context.getConnection(), "[Time].[1998].LastSibling" );
    assertEquals( "1998", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLastSiblingNull(Context context) {
    Member member =
      executeSingletonAxis(context.getConnection(), "[Gender].[F].FirstChild.LastSibling" );
    assertNull( member );
  }


  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLead(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Time].[1997].[Q2].[4].Lead(4)" );
    assertEquals( "8", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLeadNegative(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Gender].[M].Lead(-1)" );
    assertEquals( "F", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLeadLastInLevel(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Gender].[M].Lead(3)" );
    assertNull( member );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLeadNull(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Gender].Parent.Lead(1)" );
    assertNull( member );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLeadZero(Context context) {
    Member member = executeSingletonAxis(context.getConnection(), "[Gender].[F].Lead(0)" );
    assertEquals( "F", member.getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBasic2(Context context) {
    Result result =
      executeQuery(context.getConnection(),
        "select {[Gender].[F].NextMember} ON COLUMNS from Sales" );
    assertEquals(
      "M",
      result.getAxes()[ 0 ].getPositions().get( 0 ).get( 0 ).getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFirstInLevel2(Context context) {
    Result result =
      executeQuery(context.getConnection(),
        "select {[Gender].[M].NextMember} ON COLUMNS from Sales" );
    assertEquals( 0, result.getAxes()[ 0 ].getPositions().size() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAll2(Context context) {
    Result result =
      executeQuery(context.getConnection(), "select {[Gender].PrevMember} ON COLUMNS from Sales" );
    // previous to [Gender].[All] is null, so no members are returned
    assertEquals( 0, result.getAxes()[ 0 ].getPositions().size() );
  }


  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBasic5(Context context) {
    Result result =
      executeQuery(context.getConnection(),
        "select{ [Product].[All Products].[Drink].Parent} on columns "
          + "from Sales" );
    assertEquals(
      "All Products",
      result.getAxes()[ 0 ].getPositions().get( 0 ).get( 0 ).getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFirstInLevel5(Context context) {
    Result result =
      executeQuery(context.getConnection(),
        "select {[Time].[1997].[Q2].[4].Parent} on columns,"
          + "{[Gender].[M]} on rows from Sales" );
    assertEquals(
      "Q2",
      result.getAxes()[ 0 ].getPositions().get( 0 ).get( 0 ).getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAll5(Context context) {
    Result result =
      executeQuery(context.getConnection(),
        "select {[Time].[1997].[Q2].Parent} on columns,"
          + "{[Gender].[M]} on rows from Sales" );
    // previous to [Gender].[All] is null, so no members are returned
    assertEquals(
      "1997",
      result.getAxes()[ 0 ].getPositions().get( 0 ).get( 0 ).getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBasic(Context context) {
    Result result =
      executeQuery(context.getConnection(),
        "select {[Gender].[M].PrevMember} ON COLUMNS from Sales" );
    assertEquals(
      "F",
      result.getAxes()[ 0 ].getPositions().get( 0 ).get( 0 ).getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFirstInLevel(Context context) {
    Result result =
      executeQuery(context.getConnection(),
        "select {[Gender].[F].PrevMember} ON COLUMNS from Sales" );
    assertEquals( 0, result.getAxes()[ 0 ].getPositions().size() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAll(Context context) {
    Result result =
      executeQuery(context.getConnection(), "select {[Gender].PrevMember} ON COLUMNS from Sales" );
    // previous to [Gender].[All] is null, so no members are returned
    assertEquals( 0, result.getAxes()[ 0 ].getPositions().size() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAggregateDepends(Context context) {
    // Depends on everything except Measures, Gender
    String s12 = allHiersExcept("[Measures]", "[Gender]" );
    assertExprDependsOn(context.getConnection(),
      "([Measures].[Unit Sales], [Gender].[F])", s12 );
    // Depends on everything except Customers, Measures, Gender
    String s13 = allHiersExcept( "[Customers]", "[Gender]" );
    assertExprDependsOn(context.getConnection(),
      "Aggregate([Customers].Members, ([Measures].[Unit Sales], [Gender].[F]))",
      s13 );
    // Depends on everything except Customers
    String s11 = allHiersExcept( "[Customers]" );
    assertExprDependsOn(context.getConnection(),
      "Aggregate([Customers].Members)",
      s11 );
    // Depends on the current member of the Product dimension, even though
    // [Product].[All Products] is referenced from the expression.
    String s1 = allHiersExcept( "[Customers]" );
    assertExprDependsOn(context.getConnection(),
      "Aggregate(Filter([Customers].[City].Members, (([Measures].[Unit Sales] / ([Measures].[Unit Sales], [Product]"
        + ".[All Products])) > 0.1)))",
      s1 );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAggregate(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Store].[CA plus OR] AS 'AGGREGATE({[Store].[USA].[CA], [Store].[USA].[OR]})'\n"
        + "SELECT {[Measures].[Unit Sales], [Measures].[Store Sales]} ON COLUMNS,\n"
        + "      {[Store].[USA].[CA], [Store].[USA].[OR], [Store].[CA plus OR]} ON ROWS\n"
        + "FROM Sales\n"
        + "WHERE ([1997].[Q1])",
      "Axis #0:\n"
        + "{[Time].[1997].[Q1]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Store Sales]}\n"
        + "Axis #2:\n"
        + "{[Store].[USA].[CA]}\n"
        + "{[Store].[USA].[OR]}\n"
        + "{[Store].[CA plus OR]}\n"
        + "Row #0: 16,890\n"
        + "Row #0: 36,175.20\n"
        + "Row #1: 19,287\n"
        + "Row #1: 40,170.29\n"
        + "Row #2: 36,177\n"
        + "Row #2: 76,345.49\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAggregate2(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH\n"
        + "  Member [Time].[Time].[1st Half Sales] AS 'Aggregate({Time.[1997].[Q1], Time.[1997].[Q2]})'\n"
        + "  Member [Time].[Time].[2nd Half Sales] AS 'Aggregate({Time.[1997].[Q3], Time.[1997].[Q4]})'\n"
        + "  Member [Time].[Time].[Difference] AS 'Time.[2nd Half Sales] - Time.[1st Half Sales]'\n"
        + "SELECT\n"
        + "   { [Store].[Store State].Members} ON COLUMNS,\n"
        + "   { Time.[1st Half Sales], Time.[2nd Half Sales], Time.[Difference]} ON ROWS\n"
        + "FROM Sales\n"
        + "WHERE [Measures].[Store Sales]",
      "Axis #0:\n"
        + "{[Measures].[Store Sales]}\n"
        + "Axis #1:\n"
        + "{[Store].[Canada].[BC]}\n"
        + "{[Store].[Mexico].[DF]}\n"
        + "{[Store].[Mexico].[Guerrero]}\n"
        + "{[Store].[Mexico].[Jalisco]}\n"
        + "{[Store].[Mexico].[Veracruz]}\n"
        + "{[Store].[Mexico].[Yucatan]}\n"
        + "{[Store].[Mexico].[Zacatecas]}\n"
        + "{[Store].[USA].[CA]}\n"
        + "{[Store].[USA].[OR]}\n"
        + "{[Store].[USA].[WA]}\n"
        + "Axis #2:\n"
        + "{[Time].[1st Half Sales]}\n"
        + "{[Time].[2nd Half Sales]}\n"
        + "{[Time].[Difference]}\n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: 74,571.95\n"
        + "Row #0: 71,943.17\n"
        + "Row #0: 125,779.50\n"
        + "Row #1: \n"
        + "Row #1: \n"
        + "Row #1: \n"
        + "Row #1: \n"
        + "Row #1: \n"
        + "Row #1: \n"
        + "Row #1: \n"
        + "Row #1: 84,595.89\n"
        + "Row #1: 70,333.90\n"
        + "Row #1: 138,013.72\n"
        + "Row #2: \n"
        + "Row #2: \n"
        + "Row #2: \n"
        + "Row #2: \n"
        + "Row #2: \n"
        + "Row #2: \n"
        + "Row #2: \n"
        + "Row #2: 10,023.94\n"
        + "Row #2: -1,609.27\n"
        + "Row #2: 12,234.22\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAggregateWithIIF(Context context) {
    assertQueryReturns(context.getConnection(),
      "with member store.foo as 'iif(3>1,"
        + "aggregate({[Store].[All Stores].[USA].[OR]}),"
        + "aggregate({[Store].[All Stores].[USA].[CA]}))' "
        + "select {store.foo} on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[foo]}\n"
        + "Row #0: 67,659\n" );
  }


  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAggregate2AllMembers(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH\n"
        + "  Member [Time].[Time].[1st Half Sales] AS 'Aggregate({Time.[1997].[Q1], Time.[1997].[Q2]})'\n"
        + "  Member [Time].[Time].[2nd Half Sales] AS 'Aggregate({Time.[1997].[Q3], Time.[1997].[Q4]})'\n"
        + "  Member [Time].[Time].[Difference] AS 'Time.[2nd Half Sales] - Time.[1st Half Sales]'\n"
        + "SELECT\n"
        + "   { [Store].[Store State].AllMembers} ON COLUMNS,\n"
        + "   { Time.[1st Half Sales], Time.[2nd Half Sales], Time.[Difference]} ON ROWS\n"
        + "FROM Sales\n"
        + "WHERE [Measures].[Store Sales]",
      "Axis #0:\n"
        + "{[Measures].[Store Sales]}\n"
        + "Axis #1:\n"
        + "{[Store].[Canada].[BC]}\n"
        + "{[Store].[Mexico].[DF]}\n"
        + "{[Store].[Mexico].[Guerrero]}\n"
        + "{[Store].[Mexico].[Jalisco]}\n"
        + "{[Store].[Mexico].[Veracruz]}\n"
        + "{[Store].[Mexico].[Yucatan]}\n"
        + "{[Store].[Mexico].[Zacatecas]}\n"
        + "{[Store].[USA].[CA]}\n"
        + "{[Store].[USA].[OR]}\n"
        + "{[Store].[USA].[WA]}\n"
        + "Axis #2:\n"
        + "{[Time].[1st Half Sales]}\n"
        + "{[Time].[2nd Half Sales]}\n"
        + "{[Time].[Difference]}\n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: 74,571.95\n"
        + "Row #0: 71,943.17\n"
        + "Row #0: 125,779.50\n"
        + "Row #1: \n"
        + "Row #1: \n"
        + "Row #1: \n"
        + "Row #1: \n"
        + "Row #1: \n"
        + "Row #1: \n"
        + "Row #1: \n"
        + "Row #1: 84,595.89\n"
        + "Row #1: 70,333.90\n"
        + "Row #1: 138,013.72\n"
        + "Row #2: \n"
        + "Row #2: \n"
        + "Row #2: \n"
        + "Row #2: \n"
        + "Row #2: \n"
        + "Row #2: \n"
        + "Row #2: \n"
        + "Row #2: 10,023.94\n"
        + "Row #2: -1,609.27\n"
        + "Row #2: 12,234.22\n" );
  }


  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAggregateToSimulateCompoundSlicer(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Time].[Time].[1997 H1] as 'Aggregate({[Time].[1997].[Q1], [Time].[1997].[Q2]})'\n"
        + "  MEMBER [Education Level].[College or higher] as 'Aggregate({[Education Level].[Bachelors Degree], "
        + "[Education Level].[Graduate Degree]})'\n"
        + "SELECT {[Measures].[Unit Sales], [Measures].[Store Sales]} on columns,\n"
        + "  {[Product].children} on rows\n"
        + "FROM [Sales]\n"
        + "WHERE ([Time].[1997 H1], [Education Level].[College or higher], [Gender].[F])",
      "Axis #0:\n"
        + "{[Time].[1997 H1], [Education Level].[College or higher], [Gender].[F]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Store Sales]}\n"
        + "Axis #2:\n"
        + "{[Product].[Drink]}\n"
        + "{[Product].[Food]}\n"
        + "{[Product].[Non-Consumable]}\n"
        + "Row #0: 1,797\n"
        + "Row #0: 3,620.49\n"
        + "Row #1: 15,002\n"
        + "Row #1: 31,931.88\n"
        + "Row #2: 3,845\n"
        + "Row #2: 8,173.22\n" );
  }

  /**
   * Tests behavior where CurrentMember occurs in calculated members and that member is a set.
   *
   * <p>Mosha discusses this behavior in the article
   * <a href="http://www.mosha.com/msolap/articles/mdxmultiselectcalcs.htm">
   * Multiselect friendly MDX calculations</a>.
   *
   * <p>Mondrian's behavior is consistent with MSAS 2K: it returns zeroes.
   * SSAS 2005 returns an error, which can be fixed by reformulating the calculated members.
   *
   */
  //* @see mondrian.rolap.FastBatchingCellReaderTest#testAggregateDistinctCount()
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMultiselectCalculations(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH\n"
        + "MEMBER [Measures].[Declining Stores Count] AS\n"
        + " ' Count(Filter(Descendants(Store.CurrentMember, Store.[Store Name]), [Store Sales] < ([Store Sales],Time"
        + ".Time.PrevMember))) '\n"
        + " MEMBER \n"
        + "  [Store].[XL_QZX] AS 'Aggregate ({ [Store].[All Stores].[USA].[WA] , [Store].[All Stores].[USA].[CA] })' \n"
        + "SELECT \n"
        + "  NON EMPTY HIERARCHIZE(AddCalculatedMembers({DrillDownLevel({[Product].[All Products]})})) \n"
        + "    DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS \n"
        + "FROM [Sales] \n"
        + "WHERE ([Measures].[Declining Stores Count], [Time].[1998].[Q3], [Store].[XL_QZX])",
      "Axis #0:\n"
        + "{[Measures].[Declining Stores Count], [Time].[1998].[Q3], [Store].[XL_QZX]}\n"
        + "Axis #1:\n"
        + "{[Product].[All Products]}\n"
        + "{[Product].[Drink]}\n"
        + "{[Product].[Food]}\n"
        + "{[Product].[Non-Consumable]}\n"
        + "Row #0: .00\n"
        + "Row #0: .00\n"
        + "Row #0: .00\n"
        + "Row #0: .00\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAvg(Context context) {
    assertExprReturns(context.getConnection(),
      "AVG({[Store].[All Stores].[USA].children},[Measures].[Store Sales])",
      "188,412.71" );
  }

  // todo: testAvgWithNulls

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCorrelation(Context context) {
    assertExprReturns(context.getConnection(),
      "Correlation({[Store].[All Stores].[USA].children}, [Measures].[Unit Sales], [Measures].[Store Sales]) * 1000000",
      "999,906" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCount(Context context) {
    assertExprDependsOn(context.getConnection(),
      "count(Crossjoin([Store].[All Stores].[USA].Children, {[Gender].children}), INCLUDEEMPTY)",
      "{[Gender]}" );

    String s1 = allHiersExcept( "[Store]" );
    assertExprDependsOn(context.getConnection(),
      "count(Crossjoin([Store].[All Stores].[USA].Children, "
        + "{[Gender].children}), EXCLUDEEMPTY)",
      s1 );

    assertExprReturns(context.getConnection(),
      "count({[Promotion Media].[Media Type].members})", "14" );

    // applied to an empty set
    assertExprReturns(context.getConnection(), "count({[Gender].Parent}, IncludeEmpty)", "0" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCountExcludeEmpty(Context context) {
    String s1 = allHiersExcept( "[Store]" );
    assertExprDependsOn(context.getConnection(),
      "count(Crossjoin([Store].[USA].Children, {[Gender].children}), EXCLUDEEMPTY)",
      s1 );

    assertQueryReturns(context.getConnection(),
      "with member [Measures].[Promo Count] as \n"
        + " ' Count(Crossjoin({[Measures].[Unit Sales]},\n"
        + " {[Promotion Media].[Media Type].members}), EXCLUDEEMPTY)'\n"
        + "select {[Measures].[Unit Sales], [Measures].[Promo Count]} on columns,\n"
        + " {[Product].[Drink].[Beverages].[Carbonated Beverages].[Soda].children} on rows\n"
        + "from Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Promo Count]}\n"
        + "Axis #2:\n"
        + "{[Product].[Drink].[Beverages].[Carbonated Beverages].[Soda].[Excellent]}\n"
        + "{[Product].[Drink].[Beverages].[Carbonated Beverages].[Soda].[Fabulous]}\n"
        + "{[Product].[Drink].[Beverages].[Carbonated Beverages].[Soda].[Skinner]}\n"
        + "{[Product].[Drink].[Beverages].[Carbonated Beverages].[Soda].[Token]}\n"
        + "{[Product].[Drink].[Beverages].[Carbonated Beverages].[Soda].[Washington]}\n"
        + "Row #0: 738\n"
        + "Row #0: 14\n"
        + "Row #1: 632\n"
        + "Row #1: 13\n"
        + "Row #2: 655\n"
        + "Row #2: 14\n"
        + "Row #3: 735\n"
        + "Row #3: 14\n"
        + "Row #4: 647\n"
        + "Row #4: 12\n" );

    // applied to an empty set
    assertExprReturns(context.getConnection(), "count({[Gender].Parent}, ExcludeEmpty)", "0" );
  }

  /**
   * Tests that the 'null' value is regarded as empty, even if the underlying cell has fact table rows.
   *
   * <p>For a fuller test case, see

   */
  // {@link mondrian.xmla.XmlaCognosTest#testCognosMDXSuiteConvertedAdventureWorksToFoodMart_015()}
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCountExcludeEmptyNull(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Measures].[Foo] AS\n"
        + "    Iif("
        + hierarchyName( "Time", "Time" )
        + ".CurrentMember.Name = 'Q2', 1, NULL)\n"
        + "  MEMBER [Measures].[Bar] AS\n"
        + "    Iif("
        + hierarchyName( "Time", "Time" )
        + ".CurrentMember.Name = 'Q2', 1, 0)\n"
        + "  Member [Time].[Time].[CountExc] AS\n"
        + "    Count([Time].[1997].Children, EXCLUDEEMPTY),\n"
        + "    SOLVE_ORDER = 2\n"
        + "  Member [Time].[Time].[CountInc] AS\n"
        + "    Count([Time].[1997].Children, INCLUDEEMPTY),\n"
        + "    SOLVE_ORDER = 2\n"
        + "SELECT {[Measures].[Foo],\n"
        + "   [Measures].[Bar],\n"
        + "   [Measures].[Unit Sales]} ON 0,\n"
        + "  {[Time].[1997].Children,\n"
        + "   [Time].[CountExc],\n"
        + "   [Time].[CountInc]} ON 1\n"
        + "FROM [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Foo]}\n"
        + "{[Measures].[Bar]}\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Time].[1997].[Q1]}\n"
        + "{[Time].[1997].[Q2]}\n"
        + "{[Time].[1997].[Q3]}\n"
        + "{[Time].[1997].[Q4]}\n"
        + "{[Time].[CountExc]}\n"
        + "{[Time].[CountInc]}\n"
        + "Row #0: \n"
        + "Row #0: 0\n"
        + "Row #0: 66,291\n"
        + "Row #1: 1\n"
        + "Row #1: 1\n"
        + "Row #1: 62,610\n"
        + "Row #2: \n"
        + "Row #2: 0\n"
        + "Row #2: 65,848\n"
        + "Row #3: \n"
        + "Row #3: 0\n"
        + "Row #3: 72,024\n"
        + "Row #4: 1\n"
        + "Row #4: 4\n"
        + "Row #4: 4\n"
        + "Row #5: 4\n"
        + "Row #5: 4\n"
        + "Row #5: 4\n" );
  }

  /**
   * Testcase for
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-710">
   * bug MONDRIAN-710, "Count with ExcludeEmpty throws an exception when the cube does not have a
   * factCountMeasure"</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCountExcludeEmptyOnCubeWithNoCountFacts(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH "
        + "  MEMBER [Measures].[count] AS '"
        + "    COUNT([Store Type].[Store Type].MEMBERS, EXCLUDEEMPTY)'"
        + " SELECT "
        + "  {[Measures].[count]} ON AXIS(0)"
        + " FROM [Warehouse]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[count]}\n"
        + "Row #0: 5\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCountExcludeEmptyOnVirtualCubeWithNoCountFacts(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH "
        + "  MEMBER [Measures].[count] AS '"
        + "    COUNT([Store].MEMBERS, EXCLUDEEMPTY)'"
        + " SELECT "
        + "  {[Measures].[count]} ON AXIS(0)"
        + " FROM [Warehouse and Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[count]}\n"
        + "Row #0: 31\n" );
  }

  // todo: testCountNull, testCountNoExp

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCovariance(Context context) {
    assertExprReturns(context.getConnection(),
      "Covariance({[Store].[All Stores].[USA].children}, [Measures].[Unit Sales], [Measures].[Store Sales])",
      "1,355,761,899" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCovarianceN(Context context) {
    assertExprReturns(context.getConnection(),
      "CovarianceN({[Store].[All Stores].[USA].children}, [Measures].[Unit Sales], [Measures].[Store Sales])",
      "2,033,642,849" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIIfNumeric(Context context) {
    assertExprReturns(context.getConnection(),
      "IIf(([Measures].[Unit Sales],[Product].[Drink].[Alcoholic Beverages].[Beer and Wine]) > 100, 45, 32)",
      "45" );

    // Compare two members. The system needs to figure out that they are
    // both numeric, and use the right overloaded version of ">", otherwise
    // we'll get a ClassCastException at runtime.
    assertExprReturns(context.getConnection(),
      "IIf([Measures].[Unit Sales] > [Measures].[Store Sales], 45, 32)",
      "32" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMax(Context context) {
    assertExprReturns(context.getConnection(),
      "MAX({[Store].[All Stores].[USA].children},[Measures].[Store Sales])",
      "263,793.22" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMaxNegative(Context context) {
    // Bug 1771928, "Max() works incorrectly with negative values"
    assertQueryReturns(context.getConnection(),
      "with \n"
        + "  member [Customers].[Neg] as '-1'\n"
        + "  member [Customers].[Min] as 'Min({[Customers].[Neg]})'\n"
        + "  member [Customers].[Max] as 'Max({[Customers].[Neg]})'\n"
        + "select {[Customers].[Neg],[Customers].[Min],[Customers].[Max]} on 0\n"
        + "from Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[Neg]}\n"
        + "{[Customers].[Min]}\n"
        + "{[Customers].[Max]}\n"
        + "Row #0: -1\n"
        + "Row #0: -1\n"
        + "Row #0: -1\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMedian(Context context) {
    assertExprReturns(context.getConnection(),
      "MEDIAN({[Store].[All Stores].[USA].children},"
        + "[Measures].[Store Sales])",
      "159,167.84" );
    // single value
    assertExprReturns(context.getConnection(),
      "MEDIAN({[Store].[All Stores].[USA]}, [Measures].[Store Sales])",
      "565,238.13" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMedian2(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH\n"
        + "   Member [Time].[Time].[1st Half Sales] AS 'Sum({[Time].[1997].[Q1], [Time].[1997].[Q2]})'\n"
        + "   Member [Time].[Time].[2nd Half Sales] AS 'Sum({[Time].[1997].[Q3], [Time].[1997].[Q4]})'\n"
        + "   Member [Time].[Time].[Median] AS 'Median(Time.[Time].Members)'\n"
        + "SELECT\n"
        + "   NON EMPTY { [Store].[Store Name].Members} ON COLUMNS,\n"
        + "   { [Time].[1st Half Sales], [Time].[2nd Half Sales], [Time].[Median]} ON ROWS\n"
        + "FROM Sales\n"
        + "WHERE [Measures].[Store Sales]",

      "Axis #0:\n"
        + "{[Measures].[Store Sales]}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[CA].[Beverly Hills].[Store 6]}\n"
        + "{[Store].[USA].[CA].[Los Angeles].[Store 7]}\n"
        + "{[Store].[USA].[CA].[San Diego].[Store 24]}\n"
        + "{[Store].[USA].[CA].[San Francisco].[Store 14]}\n"
        + "{[Store].[USA].[OR].[Portland].[Store 11]}\n"
        + "{[Store].[USA].[OR].[Salem].[Store 13]}\n"
        + "{[Store].[USA].[WA].[Bellingham].[Store 2]}\n"
        + "{[Store].[USA].[WA].[Bremerton].[Store 3]}\n"
        + "{[Store].[USA].[WA].[Seattle].[Store 15]}\n"
        + "{[Store].[USA].[WA].[Spokane].[Store 16]}\n"
        + "{[Store].[USA].[WA].[Tacoma].[Store 17]}\n"
        + "{[Store].[USA].[WA].[Walla Walla].[Store 22]}\n"
        + "{[Store].[USA].[WA].[Yakima].[Store 23]}\n"
        + "Axis #2:\n"
        + "{[Time].[1st Half Sales]}\n"
        + "{[Time].[2nd Half Sales]}\n"
        + "{[Time].[Median]}\n"
        + "Row #0: 20,801.04\n"
        + "Row #0: 25,421.41\n"
        + "Row #0: 26,275.11\n"
        + "Row #0: 2,074.39\n"
        + "Row #0: 28,519.18\n"
        + "Row #0: 43,423.99\n"
        + "Row #0: 2,140.99\n"
        + "Row #0: 25,502.08\n"
        + "Row #0: 25,293.50\n"
        + "Row #0: 23,265.53\n"
        + "Row #0: 34,926.91\n"
        + "Row #0: 2,159.60\n"
        + "Row #0: 12,490.89\n"
        + "Row #1: 24,949.20\n"
        + "Row #1: 29,123.87\n"
        + "Row #1: 28,156.03\n"
        + "Row #1: 2,366.79\n"
        + "Row #1: 26,539.61\n"
        + "Row #1: 43,794.29\n"
        + "Row #1: 2,598.24\n"
        + "Row #1: 27,394.22\n"
        + "Row #1: 27,350.57\n"
        + "Row #1: 26,368.93\n"
        + "Row #1: 39,917.05\n"
        + "Row #1: 2,546.37\n"
        + "Row #1: 11,838.34\n"
        + "Row #2: 4,577.35\n"
        + "Row #2: 5,211.38\n"
        + "Row #2: 4,722.87\n"
        + "Row #2: 398.24\n"
        + "Row #2: 5,039.50\n"
        + "Row #2: 7,374.59\n"
        + "Row #2: 410.22\n"
        + "Row #2: 4,924.04\n"
        + "Row #2: 4,569.13\n"
        + "Row #2: 4,511.68\n"
        + "Row #2: 6,630.91\n"
        + "Row #2: 419.51\n"
        + "Row #2: 2,169.48\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testPercentile(Context context) {
    // same result as median
    assertExprReturns(context.getConnection(),
      "Percentile({[Store].[All Stores].[USA].children}, [Measures].[Store Sales], 50)",
      "159,167.84" );
    // same result as min
    assertExprReturns(context.getConnection(),
      "Percentile({[Store].[All Stores].[USA].children}, [Measures].[Store Sales], 0)",
      "142,277.07" );
    // same result as max
    assertExprReturns(context.getConnection(),
      "Percentile({[Store].[All Stores].[USA].children}, [Measures].[Store Sales], 100)",
      "263,793.22" );
    // check some real percentile cases
    assertExprReturns(context.getConnection(),
      "Percentile({[Store].[All Stores].[USA].[WA].children}, [Measures].[Store Sales], 50)",
      "49,634.46" );
    // the next two results correspond to MS Excel 2013.
    // See MONDRIAN-2343 jira issue.
    assertExprReturns(context.getConnection(),
      "Percentile({[Store].[All Stores].[USA].[WA].children}, [Measures].[Store Sales], 100/7*2)",
      "18,732.09" );
    assertExprReturns(context.getConnection(),
      "Percentile({[Store].[All Stores].[USA].[WA].children}, [Measures].[Store Sales], 95)",
      "68,259.66" );
  }

  /**
   * Testcase for bug
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-1045">MONDRIAN-1045,
   * "When I use the Percentile function it cracks when there's only 1 register"</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testPercentileBugMondrian1045(Context context) {
    assertExprReturns(context.getConnection(),
      "Percentile({[Store].[All Stores].[USA]}, [Measures].[Store Sales], 50)",
      "565,238.13" );
    assertExprReturns(context.getConnection(),
      "Percentile({[Store].[All Stores].[USA]}, [Measures].[Store Sales], 40)",
      "565,238.13" );
    assertExprReturns(context.getConnection(),
      "Percentile({[Store].[All Stores].[USA]}, [Measures].[Store Sales], 95)",
      "565,238.13" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMin(Context context) {
    assertExprReturns(context.getConnection(),
      "MIN({[Store].[All Stores].[USA].children},[Measures].[Store Sales])",
      "142,277.07" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMinTuple(Context context) {
    assertExprReturns(context.getConnection(),
      "Min([Customers].[All Customers].[USA].Children, ([Measures].[Unit Sales], [Gender].[All Gender].[F]))",
      "33,036" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStdev(Context context) {
    assertExprReturns(context.getConnection(),
      "STDEV({[Store].[All Stores].[USA].children},[Measures].[Store Sales])",
      "65,825.45" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStdevP(Context context) {
    assertExprReturns(context.getConnection(),
      "STDEVP({[Store].[All Stores].[USA].children},[Measures].[Store Sales])",
      "53,746.26" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSumNoExp(Context context) {
    assertExprReturns(context.getConnection(),
      "SUM({[Promotion Media].[Media Type].members})", "266,773" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testValue(Context context) {
    // VALUE is usually a cell property, not a member property.
    // We allow it because MS documents it as a function, <Member>.VALUE.
    assertExprReturns(context.getConnection(), "[Measures].[Store Sales].VALUE", "565,238.13" );

    // Depends upon almost everything.
    String s1 = allHiersExcept( "[Measures]" );
    assertExprDependsOn(context.getConnection(),
      "[Measures].[Store Sales].VALUE", s1 );

    // We do not allow FORMATTED_VALUE.
    assertExprThrows(context.getConnection(),
      "[Measures].[Store Sales].FORMATTED_VALUE",
      "MDX object '[Measures].[Store Sales].FORMATTED_VALUE' not found in cube 'Sales'" );

    assertExprReturns(context.getConnection(), "[Measures].[Store Sales].NAME", "Store Sales" );
    // MS says that ID and KEY are standard member properties for
    // OLE DB for OLAP, but not for XML/A. We don't support them.
    assertExprThrows(context.getConnection(),
      "[Measures].[Store Sales].ID",
      "MDX object '[Measures].[Store Sales].ID' not found in cube 'Sales'" );

    // Error for KEY is slightly different than for ID. It doesn't matter
    // very much.
    //
    // The error is different because KEY is registered as a Mondrian
    // builtin property, but ID isn't. KEY cannot be evaluated in
    // "<MEMBER>.KEY" syntax because there is not function defined. For
    // other builtin properties, such as NAME, CAPTION there is a builtin
    // function.
    assertExprThrows(context.getConnection(),
      "[Measures].[Store Sales].KEY",
      "No function matches signature '<Member>.KEY'" );

    assertExprReturns(context.getConnection(), "[Measures].[Store Sales].CAPTION", "Store Sales" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVar(Context context) {
    assertExprReturns(context.getConnection(),
      "VAR({[Store].[All Stores].[USA].children},[Measures].[Store Sales])",
      "4,332,990,493.69" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVarP(Context context) {
    assertExprReturns(context.getConnection(),
      "VARP({[Store].[All Stores].[USA].children},[Measures].[Store Sales])",
      "2,888,660,329.13" );
  }


  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAscendants(Context context) {
    assertAxisReturns(context.getConnection(),
      "Ascendants([Store].[USA].[CA])",
      "[Store].[USA].[CA]\n"
        + "[Store].[USA]\n"
        + "[Store].[All Stores]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAscendantsAll(Context context) {
    assertAxisReturns(context.getConnection(),
      "Ascendants([Store].DefaultMember)", "[Store].[All Stores]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAscendantsNull(Context context) {
    assertAxisReturns(context.getConnection(),
      "Ascendants([Gender].[F].PrevMember)", "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBottomCount(Context context) {
    assertAxisReturns(context.getConnection(),
      "BottomCount({[Promotion Media].[Media Type].members}, 2, [Measures].[Unit Sales])",
      "[Promotion Media].[Radio]\n"
        + "[Promotion Media].[Sunday Paper, Radio, TV]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBottomCountUnordered(Context context) {
    assertAxisReturns(context.getConnection(),
      "BottomCount({[Promotion Media].[Media Type].members}, 2)",
      "[Promotion Media].[Sunday Paper, Radio, TV]\n"
        + "[Promotion Media].[TV]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBottomPercent(Context context) {
    assertAxisReturns(context.getConnection(),
      "BottomPercent(Filter({[Store].[All Stores].[USA].[CA].Children, [Store].[All Stores].[USA].[OR].Children, "
        + "[Store].[All Stores].[USA].[WA].Children}, ([Measures].[Unit Sales] > 0.0)), 100.0, [Measures].[Store "
        + "Sales])",
      "[Store].[USA].[CA].[San Francisco]\n"
        + "[Store].[USA].[WA].[Walla Walla]\n"
        + "[Store].[USA].[WA].[Bellingham]\n"
        + "[Store].[USA].[WA].[Yakima]\n"
        + "[Store].[USA].[CA].[Beverly Hills]\n"
        + "[Store].[USA].[WA].[Spokane]\n"
        + "[Store].[USA].[WA].[Seattle]\n"
        + "[Store].[USA].[WA].[Bremerton]\n"
        + "[Store].[USA].[CA].[San Diego]\n"
        + "[Store].[USA].[CA].[Los Angeles]\n"
        + "[Store].[USA].[OR].[Portland]\n"
        + "[Store].[USA].[WA].[Tacoma]\n"
        + "[Store].[USA].[OR].[Salem]" );

    assertAxisReturns(context.getConnection(),
      "BottomPercent({[Promotion Media].[Media Type].members}, 1, [Measures].[Unit Sales])",
      "[Promotion Media].[Radio]\n"
        + "[Promotion Media].[Sunday Paper, Radio, TV]" );
  }

  // todo: test precision

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBottomSum(Context context) {
    assertAxisReturns(context.getConnection(),
      "BottomSum({[Promotion Media].[Media Type].members}, 5000, [Measures].[Unit Sales])",
      "[Promotion Media].[Radio]\n"
        + "[Promotion Media].[Sunday Paper, Radio, TV]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExceptEmpty(Context context) {
    // If left is empty, result is empty.
    assertAxisReturns(context.getConnection(),
      "Except(Filter([Gender].Members, 1=0), {[Gender].[M]})", "" );

    // If right is empty, result is left.
    assertAxisReturns(context.getConnection(),
      "Except({[Gender].[M]}, Filter([Gender].Members, 1=0))",
      "[Gender].[M]" );
  }

  /**
   * Tests that Except() successfully removes crossjoined tuples from the axis results.  Previously, this would fail by
   * returning all tuples in the first argument to Except.  bug 1439627
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExceptCrossjoin(Context context) {
    assertAxisReturns(context.getConnection(),
      "Except(CROSSJOIN({[Promotion Media].[All Media]},\n"
        + "                  [Product].[All Products].Children),\n"
        + "       CROSSJOIN({[Promotion Media].[All Media]},\n"
        + "                  {[Product].[All Products].[Drink]}))",
      "{[Promotion Media].[All Media], [Product].[Food]}\n"
        + "{[Promotion Media].[All Media], [Product].[Non-Consumable]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExtract(Context context) {
    assertAxisReturns(context.getConnection(),
      "Extract(\n"
        + "Crossjoin({[Gender].[F], [Gender].[M]},\n"
        + "          {[Marital Status].Members}),\n"
        + "[Gender])",
      "[Gender].[F]\n" + "[Gender].[M]" );

    // Extract(<set>) with no dimensions is not valid
    assertAxisThrows(context.getConnection(),
      "Extract(Crossjoin({[Gender].[F], [Gender].[M]}, {[Marital Status].Members}))",
      "No function matches signature 'Extract(<Set>)'" );

    // Extract applied to non-constant dimension should fail
    assertAxisThrows(context.getConnection(),
      "Extract(Crossjoin([Gender].Members, [Store].Children), [Store].Hierarchy.Dimension)",
      "not a constant hierarchy: [Store].Hierarchy.Dimension" );

    // Extract applied to non-constant hierarchy should fail
    assertAxisThrows(context.getConnection(),
      "Extract(Crossjoin([Gender].Members, [Store].Children), [Store].Hierarchy)",
      "not a constant hierarchy: [Store].Hierarchy" );

    // Extract applied to set of members is OK (if silly). Duplicates are
    // removed, as always.
    assertAxisReturns(context.getConnection(),
      "Extract({[Gender].[M], [Gender].Members}, [Gender])",
      "[Gender].[M]\n"
        + "[Gender].[All Gender]\n"
        + "[Gender].[F]" );

    // Extract of hierarchy not in set fails
    assertAxisThrows(context.getConnection(),
      "Extract(Crossjoin([Gender].Members, [Store].Children), [Marital Status])",
      "hierarchy [Marital Status] is not a hierarchy of the expression Crossjoin([Gender].Members, [Store].Children)" );

    // Extract applied to empty set returns empty set
    assertAxisReturns(context.getConnection(),
      "Extract(Crossjoin({[Gender].Parent}, [Store].Children), [Store])",
      "" );

    // Extract applied to asymmetric set
    assertAxisReturns(context.getConnection(),
      "Extract(\n"
        + "{([Gender].[M], [Marital Status].[M]),\n"
        + " ([Gender].[F], [Marital Status].[M]),\n"
        + " ([Gender].[M], [Marital Status].[S])},\n"
        + "[Gender])",
      "[Gender].[M]\n" + "[Gender].[F]" );

    // Extract applied to asymmetric set (other side)
    assertAxisReturns(context.getConnection(),
      "Extract(\n"
        + "{([Gender].[M], [Marital Status].[M]),\n"
        + " ([Gender].[F], [Marital Status].[M]),\n"
        + " ([Gender].[M], [Marital Status].[S])},\n"
        + "[Marital Status])",
      "[Marital Status].[M]\n"
        + "[Marital Status].[S]" );

    // Extract more than one hierarchy
    assertAxisReturns(context.getConnection(),
      "Extract(\n"
        + "[Gender].Children * [Marital Status].Children * [Time].[1997].Children * [Store].[USA].Children,\n"
        + "[Time], [Marital Status])",
      "{[Time].[1997].[Q1], [Marital Status].[M]}\n"
        + "{[Time].[1997].[Q2], [Marital Status].[M]}\n"
        + "{[Time].[1997].[Q3], [Marital Status].[M]}\n"
        + "{[Time].[1997].[Q4], [Marital Status].[M]}\n"
        + "{[Time].[1997].[Q1], [Marital Status].[S]}\n"
        + "{[Time].[1997].[Q2], [Marital Status].[S]}\n"
        + "{[Time].[1997].[Q3], [Marital Status].[S]}\n"
        + "{[Time].[1997].[Q4], [Marital Status].[S]}" );

    // Extract duplicate hierarchies fails
    assertAxisThrows(context.getConnection(),
      "Extract(\n"
        + "{([Gender].[M], [Marital Status].[M]),\n"
        + " ([Gender].[F], [Marital Status].[M]),\n"
        + " ([Gender].[M], [Marital Status].[S])},\n"
        + "[Gender], [Gender])",
      "hierarchy [Gender] is extracted more than once" );
  }

  /**
   * Tests that TopPercent() operates succesfully on a axis of crossjoined tuples.  previously, this would fail with a
   * ClassCastException in FunUtil.java.  bug 1440306
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTopPercentCrossjoin(Context context) {
    assertAxisReturns(context.getConnection(),
      "{TopPercent(Crossjoin([Product].[Product Department].members,\n"
        + "[Time].[1997].children),10,[Measures].[Store Sales])}",
      "{[Product].[Food].[Produce], [Time].[1997].[Q4]}\n"
        + "{[Product].[Food].[Produce], [Time].[1997].[Q1]}\n"
        + "{[Product].[Food].[Produce], [Time].[1997].[Q3]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCrossjoinNested(Context context) {
    assertAxisReturns(context.getConnection(),
      "  CrossJoin(\n"
        + "    CrossJoin(\n"
        + "      [Gender].members,\n"
        + "      [Marital Status].members),\n"
        + "   {[Store], [Store].children})",

      "{[Gender].[All Gender], [Marital Status].[All Marital Status], [Store].[All Stores]}\n"
        + "{[Gender].[All Gender], [Marital Status].[All Marital Status], [Store].[Canada]}\n"
        + "{[Gender].[All Gender], [Marital Status].[All Marital Status], [Store].[Mexico]}\n"
        + "{[Gender].[All Gender], [Marital Status].[All Marital Status], [Store].[USA]}\n"
        + "{[Gender].[All Gender], [Marital Status].[M], [Store].[All Stores]}\n"
        + "{[Gender].[All Gender], [Marital Status].[M], [Store].[Canada]}\n"
        + "{[Gender].[All Gender], [Marital Status].[M], [Store].[Mexico]}\n"
        + "{[Gender].[All Gender], [Marital Status].[M], [Store].[USA]}\n"
        + "{[Gender].[All Gender], [Marital Status].[S], [Store].[All Stores]}\n"
        + "{[Gender].[All Gender], [Marital Status].[S], [Store].[Canada]}\n"
        + "{[Gender].[All Gender], [Marital Status].[S], [Store].[Mexico]}\n"
        + "{[Gender].[All Gender], [Marital Status].[S], [Store].[USA]}\n"
        + "{[Gender].[F], [Marital Status].[All Marital Status], [Store].[All Stores]}\n"
        + "{[Gender].[F], [Marital Status].[All Marital Status], [Store].[Canada]}\n"
        + "{[Gender].[F], [Marital Status].[All Marital Status], [Store].[Mexico]}\n"
        + "{[Gender].[F], [Marital Status].[All Marital Status], [Store].[USA]}\n"
        + "{[Gender].[F], [Marital Status].[M], [Store].[All Stores]}\n"
        + "{[Gender].[F], [Marital Status].[M], [Store].[Canada]}\n"
        + "{[Gender].[F], [Marital Status].[M], [Store].[Mexico]}\n"
        + "{[Gender].[F], [Marital Status].[M], [Store].[USA]}\n"
        + "{[Gender].[F], [Marital Status].[S], [Store].[All Stores]}\n"
        + "{[Gender].[F], [Marital Status].[S], [Store].[Canada]}\n"
        + "{[Gender].[F], [Marital Status].[S], [Store].[Mexico]}\n"
        + "{[Gender].[F], [Marital Status].[S], [Store].[USA]}\n"
        + "{[Gender].[M], [Marital Status].[All Marital Status], [Store].[All Stores]}\n"
        + "{[Gender].[M], [Marital Status].[All Marital Status], [Store].[Canada]}\n"
        + "{[Gender].[M], [Marital Status].[All Marital Status], [Store].[Mexico]}\n"
        + "{[Gender].[M], [Marital Status].[All Marital Status], [Store].[USA]}\n"
        + "{[Gender].[M], [Marital Status].[M], [Store].[All Stores]}\n"
        + "{[Gender].[M], [Marital Status].[M], [Store].[Canada]}\n"
        + "{[Gender].[M], [Marital Status].[M], [Store].[Mexico]}\n"
        + "{[Gender].[M], [Marital Status].[M], [Store].[USA]}\n"
        + "{[Gender].[M], [Marital Status].[S], [Store].[All Stores]}\n"
        + "{[Gender].[M], [Marital Status].[S], [Store].[Canada]}\n"
        + "{[Gender].[M], [Marital Status].[S], [Store].[Mexico]}\n"
        + "{[Gender].[M], [Marital Status].[S], [Store].[USA]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCrossjoinSingletonTuples(Context context) {
    assertAxisReturns(context.getConnection(),
      "CrossJoin({([Gender].[M])}, {([Marital Status].[S])})",
      "{[Gender].[M], [Marital Status].[S]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCrossjoinSingletonTuplesNested(Context context) {
    assertAxisReturns(context.getConnection(),
      "CrossJoin({([Gender].[M])}, CrossJoin({([Marital Status].[S])}, [Store].children))",
      "{[Gender].[M], [Marital Status].[S], [Store].[Canada]}\n"
        + "{[Gender].[M], [Marital Status].[S], [Store].[Mexico]}\n"
        + "{[Gender].[M], [Marital Status].[S], [Store].[USA]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCrossjoinAsterisk(Context context) {
    assertAxisReturns(context.getConnection(),
      "{[Gender].[M]} * {[Marital Status].[S]}",
      "{[Gender].[M], [Marital Status].[S]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCrossjoinAsteriskTuple(Context context) {
    assertQueryReturns(context.getConnection(),
      "select {[Measures].[Unit Sales]} ON COLUMNS, "
        + "NON EMPTY [Store].[All Stores] "
        + " * ([Product].[All Products], [Gender]) "
        + " * [Customers].[All Customers] ON ROWS "
        + "from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Store].[All Stores], [Product].[All Products], [Gender].[All Gender], [Customers].[All Customers]}\n"
        + "Row #0: 266,773\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCrossjoinAsteriskAssoc(Context context) {
    assertAxisReturns(context.getConnection(),
      "Order({[Gender].Children} * {[Marital Status].Children} * {[Time].[1997].[Q2].Children},"
        + "[Measures].[Unit Sales])",
      "{[Gender].[F], [Marital Status].[M], [Time].[1997].[Q2].[4]}\n"
        + "{[Gender].[F], [Marital Status].[M], [Time].[1997].[Q2].[6]}\n"
        + "{[Gender].[F], [Marital Status].[M], [Time].[1997].[Q2].[5]}\n"
        + "{[Gender].[F], [Marital Status].[S], [Time].[1997].[Q2].[4]}\n"
        + "{[Gender].[F], [Marital Status].[S], [Time].[1997].[Q2].[5]}\n"
        + "{[Gender].[F], [Marital Status].[S], [Time].[1997].[Q2].[6]}\n"
        + "{[Gender].[M], [Marital Status].[M], [Time].[1997].[Q2].[4]}\n"
        + "{[Gender].[M], [Marital Status].[M], [Time].[1997].[Q2].[5]}\n"
        + "{[Gender].[M], [Marital Status].[M], [Time].[1997].[Q2].[6]}\n"
        + "{[Gender].[M], [Marital Status].[S], [Time].[1997].[Q2].[6]}\n"
        + "{[Gender].[M], [Marital Status].[S], [Time].[1997].[Q2].[4]}\n"
        + "{[Gender].[M], [Marital Status].[S], [Time].[1997].[Q2].[5]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCrossjoinAsteriskInsideBraces(Context context) {
    assertAxisReturns(context.getConnection(),
      "{[Gender].[M] * [Marital Status].[S] * [Time].[1997].[Q2].Children}",
      "{[Gender].[M], [Marital Status].[S], [Time].[1997].[Q2].[4]}\n"
        + "{[Gender].[M], [Marital Status].[S], [Time].[1997].[Q2].[5]}\n"
        + "{[Gender].[M], [Marital Status].[S], [Time].[1997].[Q2].[6]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCrossJoinAsteriskQuery(Context context) {
    assertQueryReturns(context.getConnection(),
      "SELECT {[Measures].members * [1997].children} ON COLUMNS,\n"
        + " {[Store].[USA].children * [Position].[All Position].children} DIMENSION PROPERTIES [Store].[Store SQFT] "
        + "ON ROWS\n"
        + "FROM [HR]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Org Salary], [Time].[1997].[Q1]}\n"
        + "{[Measures].[Org Salary], [Time].[1997].[Q2]}\n"
        + "{[Measures].[Org Salary], [Time].[1997].[Q3]}\n"
        + "{[Measures].[Org Salary], [Time].[1997].[Q4]}\n"
        + "{[Measures].[Count], [Time].[1997].[Q1]}\n"
        + "{[Measures].[Count], [Time].[1997].[Q2]}\n"
        + "{[Measures].[Count], [Time].[1997].[Q3]}\n"
        + "{[Measures].[Count], [Time].[1997].[Q4]}\n"
        + "{[Measures].[Number of Employees], [Time].[1997].[Q1]}\n"
        + "{[Measures].[Number of Employees], [Time].[1997].[Q2]}\n"
        + "{[Measures].[Number of Employees], [Time].[1997].[Q3]}\n"
        + "{[Measures].[Number of Employees], [Time].[1997].[Q4]}\n"
        + "Axis #2:\n"
        + "{[Store].[USA].[CA], [Position].[Middle Management]}\n"
        + "{[Store].[USA].[CA], [Position].[Senior Management]}\n"
        + "{[Store].[USA].[CA], [Position].[Store Full Time Staf]}\n"
        + "{[Store].[USA].[CA], [Position].[Store Management]}\n"
        + "{[Store].[USA].[CA], [Position].[Store Temp Staff]}\n"
        + "{[Store].[USA].[OR], [Position].[Middle Management]}\n"
        + "{[Store].[USA].[OR], [Position].[Senior Management]}\n"
        + "{[Store].[USA].[OR], [Position].[Store Full Time Staf]}\n"
        + "{[Store].[USA].[OR], [Position].[Store Management]}\n"
        + "{[Store].[USA].[OR], [Position].[Store Temp Staff]}\n"
        + "{[Store].[USA].[WA], [Position].[Middle Management]}\n"
        + "{[Store].[USA].[WA], [Position].[Senior Management]}\n"
        + "{[Store].[USA].[WA], [Position].[Store Full Time Staf]}\n"
        + "{[Store].[USA].[WA], [Position].[Store Management]}\n"
        + "{[Store].[USA].[WA], [Position].[Store Temp Staff]}\n"
        + "Row #0: $275.40\n"
        + "Row #0: $275.40\n"
        + "Row #0: $275.40\n"
        + "Row #0: $275.40\n"
        + "Row #0: 27\n"
        + "Row #0: 27\n"
        + "Row #0: 27\n"
        + "Row #0: 27\n"
        + "Row #0: 9\n"
        + "Row #0: 9\n"
        + "Row #0: 9\n"
        + "Row #0: 9\n"
        + "Row #1: $837.00\n"
        + "Row #1: $837.00\n"
        + "Row #1: $837.00\n"
        + "Row #1: $837.00\n"
        + "Row #1: 24\n"
        + "Row #1: 24\n"
        + "Row #1: 24\n"
        + "Row #1: 24\n"
        + "Row #1: 8\n"
        + "Row #1: 8\n"
        + "Row #1: 8\n"
        + "Row #1: 8\n"
        + "Row #2: $1,728.45\n"
        + "Row #2: $1,727.02\n"
        + "Row #2: $1,727.72\n"
        + "Row #2: $1,726.55\n"
        + "Row #2: 357\n"
        + "Row #2: 357\n"
        + "Row #2: 357\n"
        + "Row #2: 357\n"
        + "Row #2: 119\n"
        + "Row #2: 119\n"
        + "Row #2: 119\n"
        + "Row #2: 119\n"
        + "Row #3: $473.04\n"
        + "Row #3: $473.04\n"
        + "Row #3: $473.04\n"
        + "Row #3: $473.04\n"
        + "Row #3: 51\n"
        + "Row #3: 51\n"
        + "Row #3: 51\n"
        + "Row #3: 51\n"
        + "Row #3: 17\n"
        + "Row #3: 17\n"
        + "Row #3: 17\n"
        + "Row #3: 17\n"
        + "Row #4: $401.35\n"
        + "Row #4: $405.73\n"
        + "Row #4: $400.61\n"
        + "Row #4: $402.31\n"
        + "Row #4: 120\n"
        + "Row #4: 120\n"
        + "Row #4: 120\n"
        + "Row #4: 120\n"
        + "Row #4: 40\n"
        + "Row #4: 40\n"
        + "Row #4: 40\n"
        + "Row #4: 40\n"
        + "Row #5: \n"
        + "Row #5: \n"
        + "Row #5: \n"
        + "Row #5: \n"
        + "Row #5: \n"
        + "Row #5: \n"
        + "Row #5: \n"
        + "Row #5: \n"
        + "Row #5: \n"
        + "Row #5: \n"
        + "Row #5: \n"
        + "Row #5: \n"
        + "Row #6: \n"
        + "Row #6: \n"
        + "Row #6: \n"
        + "Row #6: \n"
        + "Row #6: \n"
        + "Row #6: \n"
        + "Row #6: \n"
        + "Row #6: \n"
        + "Row #6: \n"
        + "Row #6: \n"
        + "Row #6: \n"
        + "Row #6: \n"
        + "Row #7: $1,343.62\n"
        + "Row #7: $1,342.61\n"
        + "Row #7: $1,342.57\n"
        + "Row #7: $1,343.65\n"
        + "Row #7: 279\n"
        + "Row #7: 279\n"
        + "Row #7: 279\n"
        + "Row #7: 279\n"
        + "Row #7: 93\n"
        + "Row #7: 93\n"
        + "Row #7: 93\n"
        + "Row #7: 93\n"
        + "Row #8: $286.74\n"
        + "Row #8: $286.74\n"
        + "Row #8: $286.74\n"
        + "Row #8: $286.74\n"
        + "Row #8: 30\n"
        + "Row #8: 30\n"
        + "Row #8: 30\n"
        + "Row #8: 30\n"
        + "Row #8: 10\n"
        + "Row #8: 10\n"
        + "Row #8: 10\n"
        + "Row #8: 10\n"
        + "Row #9: $333.20\n"
        + "Row #9: $332.65\n"
        + "Row #9: $331.28\n"
        + "Row #9: $332.43\n"
        + "Row #9: 99\n"
        + "Row #9: 99\n"
        + "Row #9: 99\n"
        + "Row #9: 99\n"
        + "Row #9: 33\n"
        + "Row #9: 33\n"
        + "Row #9: 33\n"
        + "Row #9: 33\n"
        + "Row #10: \n"
        + "Row #10: \n"
        + "Row #10: \n"
        + "Row #10: \n"
        + "Row #10: \n"
        + "Row #10: \n"
        + "Row #10: \n"
        + "Row #10: \n"
        + "Row #10: \n"
        + "Row #10: \n"
        + "Row #10: \n"
        + "Row #10: \n"
        + "Row #11: \n"
        + "Row #11: \n"
        + "Row #11: \n"
        + "Row #11: \n"
        + "Row #11: \n"
        + "Row #11: \n"
        + "Row #11: \n"
        + "Row #11: \n"
        + "Row #11: \n"
        + "Row #11: \n"
        + "Row #11: \n"
        + "Row #11: \n"
        + "Row #12: $2,768.60\n"
        + "Row #12: $2,769.18\n"
        + "Row #12: $2,766.78\n"
        + "Row #12: $2,769.50\n"
        + "Row #12: 579\n"
        + "Row #12: 579\n"
        + "Row #12: 579\n"
        + "Row #12: 579\n"
        + "Row #12: 193\n"
        + "Row #12: 193\n"
        + "Row #12: 193\n"
        + "Row #12: 193\n"
        + "Row #13: $736.29\n"
        + "Row #13: $736.29\n"
        + "Row #13: $736.29\n"
        + "Row #13: $736.29\n"
        + "Row #13: 81\n"
        + "Row #13: 81\n"
        + "Row #13: 81\n"
        + "Row #13: 81\n"
        + "Row #13: 27\n"
        + "Row #13: 27\n"
        + "Row #13: 27\n"
        + "Row #13: 27\n"
        + "Row #14: $674.70\n"
        + "Row #14: $674.54\n"
        + "Row #14: $676.26\n"
        + "Row #14: $676.48\n"
        + "Row #14: 201\n"
        + "Row #14: 201\n"
        + "Row #14: 201\n"
        + "Row #14: 201\n"
        + "Row #14: 67\n"
        + "Row #14: 67\n"
        + "Row #14: 67\n"
        + "Row #14: 67\n" );
  }

  /**
   * Testcase for bug 1889745, "StackOverflowError while resolving crossjoin". The problem occurs when a calculated
   * member that references itself is referenced in a crossjoin.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCrossjoinResolve(Context context) {
    assertQueryReturns(context.getConnection(),
      "with\n"
        + "member [Measures].[Filtered Unit Sales] as\n"
        + " 'IIf((([Measures].[Unit Sales] > 50000.0)\n"
        + "      OR ([Product].CurrentMember.Level.UniqueName <>\n"
        + "          \"[Product].[Product Family]\")),\n"
        + "      IIf(((Count([Product].CurrentMember.Children) = 0.0)),\n"
        + "          [Measures].[Unit Sales],\n"
        + "          Sum([Product].CurrentMember.Children,\n"
        + "              [Measures].[Filtered Unit Sales])),\n"
        + "      NULL)'\n"
        + "select NON EMPTY {crossjoin({[Measures].[Filtered Unit Sales]},\n"
        + "{[Gender].[M], [Gender].[F]})} ON COLUMNS,\n"
        + "NON EMPTY {[Product].[All Products]} ON ROWS\n"
        + "from [Sales]\n"
        + "where [Time].[1997]",
      "Axis #0:\n"
        + "{[Time].[1997]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Filtered Unit Sales], [Gender].[M]}\n"
        + "{[Measures].[Filtered Unit Sales], [Gender].[F]}\n"
        + "Axis #2:\n"
        + "{[Product].[All Products]}\n"
        + "Row #0: 97,126\n"
        + "Row #0: 94,814\n" );
  }

  /**
   * Test case for bug 1911832, "Exception converting immutable list to array in JDK 1.5".
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCrossjoinOrder(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH\n"
        + "\n"
        + "SET [S1] AS 'CROSSJOIN({[Time].[1997]}, {[Gender].[Gender].MEMBERS})'\n"
        + "\n"
        + "SELECT CROSSJOIN(ORDER([S1], [Measures].[Unit Sales], BDESC),\n"
        + "{[Measures].[Unit Sales]}) ON AXIS(0)\n"
        + "FROM [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Time].[1997], [Gender].[M], [Measures].[Unit Sales]}\n"
        + "{[Time].[1997], [Gender].[F], [Measures].[Unit Sales]}\n"
        + "Row #0: 135,215\n"
        + "Row #0: 131,558\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCrossjoinDupHierarchyFails(Context context) {
    assertQueryThrows(context.getConnection(),
      "select [Measures].[Unit Sales] ON COLUMNS,\n"
        + " CrossJoin({[Time].[Quarter].[Q1]}, {[Time].[Month].[5]}) ON ROWS\n"
        + "from [Sales]",
      "Tuple contains more than one member of hierarchy '[Time]'." );

    // now with Item, for kicks
    assertQueryThrows(context.getConnection(),
      "select [Measures].[Unit Sales] ON COLUMNS,\n"
        + " CrossJoin({[Time].[Quarter].[Q1]}, {[Time].[Month].[5]}).Item(0) ON ROWS\n"
        + "from [Sales]",
      "Tuple contains more than one member of hierarchy '[Time]'." );

    // same query using explicit tuple
    assertQueryThrows(context.getConnection(),
      "select [Measures].[Unit Sales] ON COLUMNS,\n"
        + " ([Time].[Quarter].[Q1], [Time].[Month].[5]) ON ROWS\n"
        + "from [Sales]",
      "Tuple contains more than one member of hierarchy '[Time]'." );
  }

  /**
   *
   * Not an error.
   */
  //* Tests cases of different hierarchies in the same dimension. (Compare to {@link #testCrossjoinDupHierarchyFails()}).
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCrossjoinDupDimensionOk(Context context) {
    final String expectedResult =
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Time].[1997].[Q1], [Time.Weekly].[1997].[10]}\n"
        + "Row #0: 4,395\n";
    final String timeWeekly = hierarchyName( "Time", "Weekly" );
    assertQueryReturns(context.getConnection(),
      "select [Measures].[Unit Sales] ON COLUMNS,\n"
        + " CrossJoin({[Time].[Quarter].[Q1]}, {"
        + timeWeekly + ".[1997].[10]}) ON ROWS\n"
        + "from [Sales]",
      expectedResult );

    // now with Item, for kicks
    assertQueryReturns(context.getConnection(),
      "select [Measures].[Unit Sales] ON COLUMNS,\n"
        + " CrossJoin({[Time].[Quarter].[Q1]}, {"
        + timeWeekly + ".[1997].[10]}).Item(0) ON ROWS\n"
        + "from [Sales]",
      expectedResult );

    // same query using explicit tuple
    assertQueryReturns(context.getConnection(),
      "select [Measures].[Unit Sales] ON COLUMNS,\n"
        + " ([Time].[Quarter].[Q1], "
        + timeWeekly + ".[1997].[10]) ON ROWS\n"
        + "from [Sales]",
      expectedResult );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsM(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997].[Q1])",
      "[Time].[1997].[Q1]\n"
        + "[Time].[1997].[Q1].[1]\n"
        + "[Time].[1997].[Q1].[2]\n"
        + "[Time].[1997].[Q1].[3]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsDepends(Context context) {
    assertSetExprDependsOn(context.getConnection(),
      "Descendants([Time].[Time].CurrentMember)",
      "{[Time]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsML(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], [Time].[Month])",
      months );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsMLSelf(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], [Time].[Quarter], SELF)",
      quarters );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsMLLeaves(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], [Time].[Year], LEAVES)",
      "[Time].[1997]" );
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], [Time].[Quarter], LEAVES)",
      "[Time].[1997].[Q1]\n" + "[Time].[1997].[Q2]\n" + "[Time].[1997].[Q3]\n" + "[Time].[1997].[Q4]" );
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], [Time].[Month], LEAVES)",
      months );

    assertAxisReturns(context.getConnection(),
      "Descendants([Gender], [Gender].[Gender], leaves)",
      "[Gender].[F]\n" + "[Gender].[M]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsMLLeavesRagged(Context context) {
    // no cities are at leaf level
    //final TestContext raggedContext =
    //  getTestContext().withCube( "[Sales Ragged]" );
    assertAxisReturns(context.getConnection(), "[Sales Ragged]",
      "Descendants([Store].[Israel], [Store].[Store City], leaves)",
      "[Store].[Israel].[Israel].[Haifa]\n" + "[Store].[Israel].[Israel].[Tel Aviv]" );

    // all cities are leaves
    assertAxisReturns(context.getConnection(), "[Sales Ragged]",
      "Descendants([Geography].[Israel], [Geography].[City], leaves)",
      "[Geography].[Israel].[Israel].[Haifa]\n"
        + "[Geography].[Israel].[Israel].[Tel Aviv]" );

    // No state is a leaf (not even Israel, which is both a country and a
    // a state, or Vatican, with is a country/state/city)
    assertAxisReturns(context.getConnection(), "[Sales Ragged]",
      "Descendants([Geography], [Geography].[State], leaves)",
      "[Geography].[Canada].[BC]\n" +
              "[Geography].[Mexico].[DF]\n" +
              "[Geography].[Mexico].[Guerrero]\n" +
              "[Geography].[Mexico].[Jalisco]\n" +
              "[Geography].[Mexico].[Veracruz]\n" +
              "[Geography].[Mexico].[Yucatan]\n" +
              "[Geography].[Mexico].[Zacatecas]\n" +
              "[Geography].[USA].[CA]\n" +
              "[Geography].[USA].[OR]\n" +
              "[Geography].[USA].[WA]\n" +
              "[Geography].[Vatican]"
    );

  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsMNLeaves(Context context) {
    // leaves at depth 0 returns the member itself
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997].[Q2].[4], 0, Leaves)",
      "[Time].[1997].[Q2].[4]" );

    // leaves at depth > 0 returns the member itself
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997].[Q2].[4], 100, Leaves)",
      "[Time].[1997].[Q2].[4]" );

    // leaves at depth < 0 returns all descendants
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997].[Q2], -1, Leaves)",
      "[Time].[1997].[Q2].[4]\n"
        + "[Time].[1997].[Q2].[5]\n"
        + "[Time].[1997].[Q2].[6]" );

    // leaves at depth 0 returns the member itself
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997].[Q2], 0, Leaves)",
      "[Time].[1997].[Q2].[4]\n"
        + "[Time].[1997].[Q2].[5]\n"
        + "[Time].[1997].[Q2].[6]" );

    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997].[Q2], 3, Leaves)",
      "[Time].[1997].[Q2].[4]\n"
        + "[Time].[1997].[Q2].[5]\n"
        + "[Time].[1997].[Q2].[6]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsMLSelfBefore(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], [Time].[Quarter], SELF_AND_BEFORE)",
      year1997 + "\n" + quarters );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsMLSelfBeforeAfter(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], [Time].[Quarter], SELF_BEFORE_AFTER)",
      hierarchized1997 );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsMLBefore(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], [Time].[Quarter], BEFORE)", year1997 );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsMLBeforeAfter(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], [Time].[Quarter], BEFORE_AND_AFTER)",
      year1997 + "\n" + months );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsMLAfter(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], [Time].[Quarter], AFTER)", months );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsMLAfterEnd(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], [Time].[Month], AFTER)", "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsM0(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], 0)", year1997 );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsM2(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], 2)", months );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsM2Self(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], 2, Self)", months );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsM2Leaves(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], 2, Leaves)", months );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsMFarLeaves(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], 10000, Leaves)", months );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsMEmptyLeaves(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], , Leaves)",
      months );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsMEmptyLeavesFail(Context context) {
    assertAxisThrows(context.getConnection(),
      "Descendants([Time].[1997],)",
      "No function matches signature 'Descendants(<Member>, <Empty>)" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsMEmptyLeavesFail2(Context context) {
    assertAxisThrows(context.getConnection(),
      "Descendants([Time].[1997], , AFTER)",
      "depth must be specified unless DESC_FLAG is LEAVES" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsMFarSelf(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], 10000, Self)",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsMNY(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], 1, BEFORE_AND_AFTER)",
      year1997 + "\n" + months );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendants2ndHier(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time.Weekly].[1997].[10], [Time.Weekly].[Day])",
      "[Time].[Weekly].[1997].[10].[1]\n"
        + "[Time].[Weekly].[1997].[10].[23]\n"
        + "[Time].[Weekly].[1997].[10].[24]\n"
        + "[Time].[Weekly].[1997].[10].[25]\n"
        + "[Time].[Weekly].[1997].[10].[26]\n"
        + "[Time].[Weekly].[1997].[10].[27]\n"
        + "[Time].[Weekly].[1997].[10].[28]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsParentChild(Context context) {
    //getTestContext().withCube( "HR" ).
    assertAxisReturns(context.getConnection(), "HR",
      "Descendants([Employees], 2)",
      "[Employees].[Sheri Nowmer].[Derrick Whelply]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence]\n"
        + "[Employees].[Sheri Nowmer].[Maya Gutierrez]\n"
        + "[Employees].[Sheri Nowmer].[Roberta Damstra]\n"
        + "[Employees].[Sheri Nowmer].[Rebecca Kanagaki]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz]\n"
        + "[Employees].[Sheri Nowmer].[Donna Arnold]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsParentChildBefore(Context context) {
    assertAxisReturns(context.getConnection(), "HR",
      "Descendants([Employees], 2, BEFORE)",
      "[Employees].[All Employees]\n"
        + "[Employees].[Sheri Nowmer]" );
  }

  @Disabled //disabled for CI build
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsParentChildLeaves(Context context) {
    //final TestContext testContext = getTestContext().withCube( "HR" );
    DataSource dataSource = context.getConnection().getDataSource();
    if (Bug.avoidSlowTestOnLucidDB( context.getDialect())) {
      return;
    }

    // leaves, restricted by level
    assertAxisReturns(context.getConnection(), "HR",
      "Descendants([Employees].[All Employees].[Sheri Nowmer].[Michael Spence], [Employees].[Employee Id], LEAVES)",
      "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[John "
        + "Brooks]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Todd "
        + "Logan]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Joshua "
        + "Several]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[James "
        + "Thomas]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Robert "
        + "Vessa]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Bronson"
        + " Jacobs]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Rebecca"
        + " Barley]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Emilio "
        + "Alvaro]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Becky "
        + "Waters]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[A. "
        + "Joyce Jarvis]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Ruby "
        + "Sue Styles]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Lisa "
        + "Roy]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Ingrid "
        + "Burkhardt]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Todd "
        + "Whitney]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Barbara"
        + " Wisnewski]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Karren "
        + "Burkhardt]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[John "
        + "Long]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Edwin "
        + "Olenzek]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Jessie "
        + "Valerio]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Robert "
        + "Ahlering]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Megan "
        + "Burke]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Karel "
        + "Bates]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[James "
        + "Tran]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Shelley"
        + " Crow]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Anne "
        + "Sims]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard]"
        + ".[Clarence Tatman]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Jan "
        + "Nelsen]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Jeanie "
        + "Glenn]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Peggy "
        + "Smith]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Tish "
        + "Duff]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Anita "
        + "Lucero]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Stephen"
        + " Burton]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Amy "
        + "Consentino]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Stacie "
        + "Mcanich]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Mary "
        + "Browning]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard]"
        + ".[Alexandra Wellington]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Cory "
        + "Bacugalupi]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Stacy "
        + "Rizzi]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Mike "
        + "White]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Marty "
        + "Simpson]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Robert "
        + "Jones]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Raul "
        + "Casts]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Bridget"
        + " Browqett]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Kay "
        + "Kartz]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
        + ".[Jeanette Cole]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
        + ".[Phyllis Huntsman]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
        + ".[Hannah Arakawa]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
        + ".[Wathalee Steuber]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
        + ".[Pamela Cox]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
        + ".[Helen Lutes]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
        + ".[Linda Ecoffey]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
        + ".[Katherine Swint]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
        + ".[Dianne Slattengren]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
        + ".[Ronald Heymsfield]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
        + ".[Steven Whitehead]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
        + ".[William Sotelo]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck].[Beth"
        + " Stanley]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck].[Jill"
        + " Markwood]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
        + ".[Mildred Valentine]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
        + ".[Suzann Reams]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
        + ".[Audrey Wold]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
        + ".[Susan French]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
        + ".[Trish Pederson]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck].[Eric"
        + " Renn]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
        + ".[Elizabeth Catalano]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck].[Eric"
        + " Coleman]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Catherine Abel]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Emilo Miller]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Hazel Walker]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Sara Pettengill].[Linda "
        + "Blasingame]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Sara Pettengill].[Jackie "
        + "Blackwell]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Sara Pettengill].[John "
        + "Ortiz]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Sara Pettengill].[Stacey "
        + "Tearpak]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Sara Pettengill].[Fannye "
        + "Weber]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Sara Pettengill].[Diane "
        + "Kabbes]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Sara Pettengill].[Brenda "
        + "Heaney]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Sara Pettengill].[Judith "
        + "Karavites]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Jauna Elson]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Nancy Hirota]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Marie Moya]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Nicky Chesnut]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Karen Hall]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Greg Narberes]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Anna Townsend]\n"
        + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Carol Ann Rockne]" );

    // leaves, restricted by depth
    assertAxisReturns(context.getConnection(), "HR",
      "Descendants([Employees], 1, LEAVES)", "" );
    assertAxisReturns(context.getConnection(), "HR",
      "Descendants([Employees], 2, LEAVES)",
      "[Employees].[Sheri Nowmer].[Roberta Damstra].[Jennifer Cooper]\n"
        + "[Employees].[Sheri Nowmer].[Roberta Damstra].[Peggy Petty]\n"
        + "[Employees].[Sheri Nowmer].[Roberta Damstra].[Jessica Olguin]\n"
        + "[Employees].[Sheri Nowmer].[Roberta Damstra].[Phyllis Burchett]\n"
        + "[Employees].[Sheri Nowmer].[Rebecca Kanagaki].[Juanita Sharp]\n"
        + "[Employees].[Sheri Nowmer].[Rebecca Kanagaki].[Sandra Brunner]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz].[Ernest Staton]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz].[Rose Sims]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz].[Lauretta De Carlo]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz].[Mary Williams]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz].[Terri Burke]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz].[Audrey Osborn]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz].[Brian Binai]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz].[Concepcion Lozada]\n"
        + "[Employees].[Sheri Nowmer].[Donna Arnold].[Howard Bechard]\n"
        + "[Employees].[Sheri Nowmer].[Donna Arnold].[Doris Carter]" );

    assertAxisReturns(context.getConnection(), "HR",
      "Descendants([Employees], 3, LEAVES)",
      "[Employees].[Sheri Nowmer].[Roberta Damstra].[Jennifer Cooper]\n"
        + "[Employees].[Sheri Nowmer].[Roberta Damstra].[Peggy Petty]\n"
        + "[Employees].[Sheri Nowmer].[Roberta Damstra].[Jessica Olguin]\n"
        + "[Employees].[Sheri Nowmer].[Roberta Damstra].[Phyllis Burchett]\n"
        + "[Employees].[Sheri Nowmer].[Rebecca Kanagaki].[Juanita Sharp]\n"
        + "[Employees].[Sheri Nowmer].[Rebecca Kanagaki].[Sandra Brunner]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz].[Ernest Staton]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz].[Rose Sims]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz].[Lauretta De Carlo]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz].[Mary Williams]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz].[Terri Burke]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz].[Audrey Osborn]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz].[Brian Binai]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz].[Concepcion Lozada]\n"
        + "[Employees].[Sheri Nowmer].[Donna Arnold].[Howard Bechard]\n"
        + "[Employees].[Sheri Nowmer].[Donna Arnold].[Doris Carter]" );

    // note that depth is RELATIVE to the starting member
    assertAxisReturns(context.getConnection(), "HR",
      "Descendants([Employees].[Sheri Nowmer].[Roberta Damstra], 1, LEAVES)",
      "[Employees].[Sheri Nowmer].[Roberta Damstra].[Jennifer Cooper]\n"
        + "[Employees].[Sheri Nowmer].[Roberta Damstra].[Peggy Petty]\n"
        + "[Employees].[Sheri Nowmer].[Roberta Damstra].[Jessica Olguin]\n"
        + "[Employees].[Sheri Nowmer].[Roberta Damstra].[Phyllis Burchett]" );

    // Howard Bechard is a leaf member -- appears even at depth 0
    assertAxisReturns(context.getConnection(), "HR",
      "Descendants([Employees].[All Employees].[Sheri Nowmer].[Donna Arnold].[Howard Bechard], 0, LEAVES)",
      "[Employees].[Sheri Nowmer].[Donna Arnold].[Howard Bechard]" );
    assertAxisReturns(context.getConnection(), "HR",
      "Descendants([Employees].[All Employees].[Sheri Nowmer].[Donna Arnold].[Howard Bechard], 1, LEAVES)",
      "[Employees].[Sheri Nowmer].[Donna Arnold].[Howard Bechard]" );

    TestUtil.assertExprReturns(context.getConnection(), "HR",
      "Count(Descendants([Employees], 2, LEAVES))", "16" );
    TestUtil.assertExprReturns(context.getConnection(), "HR",
      "Count(Descendants([Employees], 3, LEAVES))", "16" );
    TestUtil.assertExprReturns(context.getConnection(), "HR",
      "Count(Descendants([Employees], 4, LEAVES))", "63" );
    TestUtil.assertExprReturns(context.getConnection(), "HR",
      "Count(Descendants([Employees], 999, LEAVES))", "1,044" );

    // Negative depth acts like +infinity (per MSAS).  Run the test several
    // times because we had a non-deterministic bug here.
    for ( int i = 0; i < 100; ++i ) {
      TestUtil.assertExprReturns(context.getConnection(), "HR",
        "Count(Descendants([Employees], -1, LEAVES))", "1,044" );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsSBA(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants([Time].[1997], 1, SELF_BEFORE_AFTER)",
      hierarchized1997 );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsSet(Context context) {
    assertAxisReturns(context.getConnection(),
      "Descendants({[Time].[1997].[Q4], [Time].[1997].[Q2]}, 1)",
      "[Time].[1997].[Q4].[10]\n"
        + "[Time].[1997].[Q4].[11]\n"
        + "[Time].[1997].[Q4].[12]\n"
        + "[Time].[1997].[Q2].[4]\n"
        + "[Time].[1997].[Q2].[5]\n"
        + "[Time].[1997].[Q2].[6]" );
    assertAxisReturns(context.getConnection(),
      "Descendants({[Time].[1997]}, [Time].[Month], LEAVES)",
      months );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDescendantsSetEmpty(Context context) {
    assertAxisThrows(context.getConnection(),
      "Descendants({}, 1)",
      "Cannot deduce type of set" );
    assertAxisReturns(context.getConnection(),
      "Descendants(Filter({[Time].[Time].Members}, 1=0), 1)",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRange(Context context) {
    assertAxisReturns(context.getConnection(),
      "[Time].[1997].[Q1].[2] : [Time].[1997].[Q2].[5]",
      "[Time].[1997].[Q1].[2]\n"
        + "[Time].[1997].[Q1].[3]\n"
        + "[Time].[1997].[Q2].[4]\n"
        + "[Time].[1997].[Q2].[5]" ); // not parents

    // testcase for bug XXXXX: braces required
    assertQueryReturns(context.getConnection(),
      "with set [Set1] as '[Product].[Drink]:[Product].[Food]' \n"
        + "\n"
        + "select [Set1] on columns, {[Measures].defaultMember} on rows \n"
        + "\n"
        + "from Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Product].[Drink]}\n"
        + "{[Product].[Food]}\n"
        + "Axis #2:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Row #0: 24,597\n"
        + "Row #0: 191,940\n" );
  }

  /**
   * tests that a null passed in returns an empty set in range function
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNullRange(Context context) {
    assertAxisReturns(context.getConnection(),
      "[Time].[1997].[Q1].[2] : NULL", //[Time].[1997].[Q2].[5]
      "" ); // Empty Set
  }

  /**
   * tests that an exception is thrown if both parameters in a range function are null.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTwoNullRange(Context context) {
    assertAxisThrows(context.getConnection(),
      "NULL : NULL",
      "Mondrian Error:Failed to parse query 'select {NULL : NULL} on columns from Sales'" );
  }

  /**
   * Large dimensions use a different member reader, therefore need to be tested separately.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRangeLarge(Context context) {
    assertAxisReturns(context.getConnection(),
      "[Customers].[USA].[CA].[San Francisco] : [Customers].[USA].[WA].[Bellingham]",
      "[Customers].[USA].[CA].[San Francisco]\n"
        + "[Customers].[USA].[CA].[San Gabriel]\n"
        + "[Customers].[USA].[CA].[San Jose]\n"
        + "[Customers].[USA].[CA].[Santa Cruz]\n"
        + "[Customers].[USA].[CA].[Santa Monica]\n"
        + "[Customers].[USA].[CA].[Spring Valley]\n"
        + "[Customers].[USA].[CA].[Torrance]\n"
        + "[Customers].[USA].[CA].[West Covina]\n"
        + "[Customers].[USA].[CA].[Woodland Hills]\n"
        + "[Customers].[USA].[OR].[Albany]\n"
        + "[Customers].[USA].[OR].[Beaverton]\n"
        + "[Customers].[USA].[OR].[Corvallis]\n"
        + "[Customers].[USA].[OR].[Lake Oswego]\n"
        + "[Customers].[USA].[OR].[Lebanon]\n"
        + "[Customers].[USA].[OR].[Milwaukie]\n"
        + "[Customers].[USA].[OR].[Oregon City]\n"
        + "[Customers].[USA].[OR].[Portland]\n"
        + "[Customers].[USA].[OR].[Salem]\n"
        + "[Customers].[USA].[OR].[W. Linn]\n"
        + "[Customers].[USA].[OR].[Woodburn]\n"
        + "[Customers].[USA].[WA].[Anacortes]\n"
        + "[Customers].[USA].[WA].[Ballard]\n"
        + "[Customers].[USA].[WA].[Bellingham]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRangeStartEqualsEnd(Context context) {
    assertAxisReturns(context.getConnection(),
      "[Time].[1997].[Q3].[7] : [Time].[1997].[Q3].[7]",
      "[Time].[1997].[Q3].[7]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRangeStartEqualsEndLarge(Context context) {
    assertAxisReturns(context.getConnection(),
      "[Customers].[USA].[CA] : [Customers].[USA].[CA]",
      "[Customers].[USA].[CA]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRangeEndBeforeStart(Context context) {
    assertAxisReturns(context.getConnection(),
      "[Time].[1997].[Q3].[7] : [Time].[1997].[Q2].[5]",
      "[Time].[1997].[Q2].[5]\n"
        + "[Time].[1997].[Q2].[6]\n"
        + "[Time].[1997].[Q3].[7]" ); // same as if reversed
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRangeEndBeforeStartLarge(Context context) {
    assertAxisReturns(context.getConnection(),
      "[Customers].[USA].[WA] : [Customers].[USA].[CA]",
      "[Customers].[USA].[CA]\n"
        + "[Customers].[USA].[OR]\n"
        + "[Customers].[USA].[WA]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRangeBetweenDifferentLevelsIsError(Context context) {
    assertAxisThrows(context.getConnection(),
      "[Time].[1997].[Q2] : [Time].[1997].[Q2].[5]",
      "Members must belong to the same level" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRangeBoundedByAll(Context context) {
    assertAxisReturns(context.getConnection(),
      "[Gender] : [Gender]",
      "[Gender].[All Gender]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRangeBoundedByAllLarge(Context context) {
    assertAxisReturns(context.getConnection(),
      "[Customers].DefaultMember : [Customers]",
      "[Customers].[All Customers]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRangeBoundedByNull(Context context) {
    assertAxisReturns(context.getConnection(),
      "[Gender].[F] : [Gender].[M].NextMember",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRangeBoundedByNullLarge(Context context) {
    assertAxisReturns(context.getConnection(),
      "[Customers].PrevMember : [Customers].[USA].[OR]",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBug715177(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Product].[Non-Consumable].[Other] AS\n"
        + " 'Sum(Except( [Product].[Product Department].Members,\n"
        + "       TopCount([Product].[Product Department].Members, 3)),\n"
        + "       Measures.[Unit Sales])'\n"
        + "SELECT\n"
        + "  { [Measures].[Unit Sales] } ON COLUMNS,\n"
        + "  { TopCount([Product].[Product Department].Members,3),\n"
        + "              [Product].[Non-Consumable].[Other] } ON ROWS\n"
        + "FROM [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Product].[Drink].[Alcoholic Beverages]}\n"
        + "{[Product].[Drink].[Beverages]}\n"
        + "{[Product].[Drink].[Dairy]}\n"
        + "{[Product].[Non-Consumable].[Other]}\n"
        + "Row #0: 6,838\n"
        + "Row #1: 13,573\n"
        + "Row #2: 4,186\n"
        + "Row #3: 242,176\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBug714707(Context context) {
    // Same issue as bug 715177 -- "children" returns immutable
    // list, which set operator must make mutable.
    assertAxisReturns(context.getConnection(),
      "{[Store].[USA].[CA].children, [Store].[USA]}",
      "[Store].[USA].[CA].[Alameda]\n"
        + "[Store].[USA].[CA].[Beverly Hills]\n"
        + "[Store].[USA].[CA].[Los Angeles]\n"
        + "[Store].[USA].[CA].[San Diego]\n"
        + "[Store].[USA].[CA].[San Francisco]\n"
        + "[Store].[USA]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBug715177c(Context context) {
    assertAxisReturns(context.getConnection(),
      "Order(TopCount({[Store].[USA].[CA].children},"
        + " [Measures].[Unit Sales], 2), [Measures].[Unit Sales])",
      "[Store].[USA].[CA].[Alameda]\n"
        + "[Store].[USA].[CA].[San Francisco]\n"
        + "[Store].[USA].[CA].[Beverly Hills]\n"
        + "[Store].[USA].[CA].[San Diego]\n"
        + "[Store].[USA].[CA].[Los Angeles]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFormatFixed(Context context) {
    assertExprReturns(context.getConnection(),
      "Format(12.2, \"#,##0.00\")",
      "12.20" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFormatVariable(Context context) {
    assertExprReturns(context.getConnection(),
      "Format(1234.5, \"#,#\" || \"#0.00\")",
      "1,234.50" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFormatMember(Context context) {
    assertExprReturns(context.getConnection(),
      "Format([Store].[USA].[CA], \"#,#\" || \"#0.00\")",
      "74,748.00" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIIf(Context context) {
    assertExprReturns(context.getConnection(),
      "IIf(([Measures].[Unit Sales],[Product].[Drink].[Alcoholic Beverages].[Beer and Wine]) > 100, \"Yes\",\"No\")",
      "Yes" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIIfWithNullAndNumber(Context context) {
    assertExprReturns(context.getConnection(),
      "IIf(([Measures].[Unit Sales],[Product].[Drink].[Alcoholic Beverages].[Beer and Wine]) > 100, null,20)",
      "" );
    assertExprReturns(context.getConnection(),
      "IIf(([Measures].[Unit Sales],[Product].[Drink].[Alcoholic Beverages].[Beer and Wine]) > 100, 20,null)",
      "20" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIIfWithStringAndNull(Context context) {
    assertExprReturns(context.getConnection(),
      "IIf(([Measures].[Unit Sales],[Product].[Drink].[Alcoholic Beverages].[Beer and Wine]) > 100, null,\"foo\")",
      "" );
    assertExprReturns(context.getConnection(),
      "IIf(([Measures].[Unit Sales],[Product].[Drink].[Alcoholic Beverages].[Beer and Wine]) > 100, \"foo\",null)",
      "foo" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIsEmptyWithNull(Context context) {
    assertExprReturns(context.getConnection(),
      "iif (isempty(null), \"is empty\", \"not is empty\")",
      "is empty" );
    assertExprReturns(context.getConnection(), "iif (isempty(null), 1, 2)", "1" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIIfMember(Context context) {
    assertAxisReturns(context.getConnection(),
      "IIf(1 > 2,[Store].[USA],[Store].[Canada].[BC])",
      "[Store].[Canada].[BC]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIIfLevel(Context context) {
    assertExprReturns(context.getConnection(),
      "IIf(1 > 2, [Store].[Store Country],[Store].[Store City]).Name",
      "Store City" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIIfHierarchy(Context context) {
    assertExprReturns(context.getConnection(),
      "IIf(1 > 2, [Time], [Store]).Name",
      "Store" );

    // Call Iif(<Logical>, <Dimension>, <Hierarchy>). Argument #3, the
    // hierarchy [Time.Weekly] is implicitly converted to
    // the dimension [Time] to match argument #2 which is a dimension.
    assertExprReturns(context.getConnection(),
      "IIf(1 > 2, [Time], [Time.Weekly]).Name",
      "Time" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIIfDimension(Context context) {
    assertExprReturns(context.getConnection(),
      "IIf(1 > 2, [Store], [Time]).Name",
      "Time" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIIfSet(Context context) {
    assertAxisReturns(context.getConnection(),
      "IIf(1 > 2, {[Store].[USA], [Store].[USA].[CA]}, {[Store].[Mexico], [Store].[USA].[OR]})",
      "[Store].[Mexico]\n"
        + "[Store].[USA].[OR]" );
  }

  // MONDRIAN-2408 - Consumer wants ITERABLE or ANY in CrossJoinFunDef.compileCall(ResolvedFunCall, ExpCompiler)
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIIfSetType_InCrossJoin(Context context) {
    assertAxisReturns(context.getConnection(),
      "CROSSJOIN([Store Type].[Deluxe Supermarket],IIf(1 = 1, {[Store].[USA], [Store].[USA].[CA]}, {[Store].[Mexico],"
        + " [Store].[USA].[OR]}))",
      "{[Store Type].[Deluxe Supermarket], [Store].[USA]}\n"
        + "{[Store Type].[Deluxe Supermarket], [Store].[USA].[CA]}" );
  }

  // MONDRIAN-2408 - Consumer wants (immutable) LIST in CrossJoinFunDef.compileCall(ResolvedFunCall, ExpCompiler)
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIIfSetType_InCrossJoinAndAvg(Context context) {
    assertExprReturns(context.getConnection(),
      "Avg(CROSSJOIN([Store Type].[Deluxe Supermarket],IIf(1 = 1, {[Store].[USA].[OR], [Store].[USA].[WA]}, {[Store]"
        + ".[Mexico], [Store].[USA].[CA]})), [Measures].[Store Sales])",
      "81,031.12" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDimensionCaption(Context context) {
    assertExprReturns(context.getConnection(), "[Time].[1997].Dimension.Caption", "Time" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHierarchyCaption(Context context) {
    assertExprReturns(context.getConnection(), "[Time].[1997].Hierarchy.Caption", "Time" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLevelCaption(Context context) {
    assertExprReturns(context.getConnection(), "[Time].[1997].Level.Caption", "Year" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMemberCaption(Context context) {
    assertExprReturns(context.getConnection(), "[Time].[1997].Caption", "1997" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDimensionName(Context context) {
    assertExprReturns(context.getConnection(), "[Time].[1997].Dimension.Name", "Time" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHierarchyName(Context context) {
    assertExprReturns(context.getConnection(), "[Time].[1997].Hierarchy.Name", "Time" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLevelName(Context context) {
    assertExprReturns(context.getConnection(), "[Time].[1997].Level.Name", "Year" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMemberName(Context context) {
    assertExprReturns(context.getConnection(), "[Time].[1997].Name", "1997" );
    // dimension name
    assertExprReturns(context.getConnection(), "[Store].Name", "Store" );
    // member name
    assertExprReturns(context.getConnection(), "[Store].DefaultMember.Name", "All Stores" );
    if ( isDefaultNullMemberRepresentation() ) {
      // name of null member
      assertExprReturns(context.getConnection(), "[Store].Parent.Name", "#null" );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDimensionUniqueName(Context context) {
    assertExprReturns(context.getConnection(),
      "[Gender].DefaultMember.Dimension.UniqueName",
      "[Gender]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHierarchyUniqueName(Context context) {
    assertExprReturns(context.getConnection(),
      "[Gender].DefaultMember.Hierarchy.UniqueName",
      "[Gender]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLevelUniqueName(Context context) {
    assertExprReturns(context.getConnection(),
      "[Gender].DefaultMember.Level.UniqueName",
      "[Gender].[(All)]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMemberUniqueName(Context context) {
    assertExprReturns(context.getConnection(),
      "[Gender].DefaultMember.UniqueName",
      "[Gender].[All Gender]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMemberUniqueNameOfNull(Context context) {
    if ( isDefaultNullMemberRepresentation() ) {
      assertExprReturns(context.getConnection(),
        "[Measures].[Unit Sales].FirstChild.UniqueName",
        "[Measures].[#null]" ); // MSOLAP gives "" here
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCoalesceEmptyDepends(Context context) {
    assertExprDependsOn(context.getConnection(),
      "coalesceempty([Time].[1997], [Gender].[M])",
      allHiers() );
    String s1 = allHiersExcept( "[Measures]", "[Time]" );
    assertExprDependsOn(context.getConnection(),
      "coalesceempty(([Measures].[Unit Sales], [Time].[1997]),"
        + " ([Measures].[Store Sales], [Time].[1997].[Q2]))",
      s1 );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCoalesceEmpty(Context context) {
    // [DF] is all null and [WA] has numbers for 1997 but not for 1998.
    Result result = executeQuery(context.getConnection(),
      "with\n"
        + "    member Measures.[Coal1] as 'coalesceempty(([Time].[1997], Measures.[Store Sales]), ([Time].[1998], "
        + "Measures.[Store Sales]))'\n"
        + "    member Measures.[Coal2] as 'coalesceempty(([Time].[1997], Measures.[Unit Sales]), ([Time].[1998], "
        + "Measures.[Unit Sales]))'\n"
        + "select \n"
        + "    {Measures.[Coal1], Measures.[Coal2]} on columns,\n"
        + "    {[Store].[All Stores].[Mexico].[DF], [Store].[All Stores].[USA].[WA]} on rows\n"
        + "from \n"
        + "    [Sales]" );

    checkDataResults(
      new Double[][] {
        new Double[] { null, null },
        new Double[] { new Double( 263793.22 ), new Double( 124366 ) }
      },
      result,
      0.001 );

    result = executeQuery(context.getConnection(),
      "with\n"
        + "    member Measures.[Sales Per Customer] as 'Measures.[Sales Count] / Measures.[Customer Count]'\n"
        + "    member Measures.[Coal] as 'coalesceempty(([Measures].[Sales Per Customer], [Store].[All Stores]"
        + ".[Mexico].[DF]),\n"
        + "        Measures.[Sales Per Customer])'\n"
        + "select \n"
        + "    {Measures.[Sales Per Customer], Measures.[Coal]} on columns,\n"
        + "    {[Store].[All Stores].[Mexico].[DF], [Store].[All Stores].[USA].[WA]} on rows\n"
        + "from \n"
        + "    [Sales]\n"
        + "where\n"
        + "    ([Time].[1997].[Q2])" );

    checkDataResults(
      new Double[][] {
        new Double[] { null, null },
        new Double[] { new Double( 8.963 ), new Double( 8.963 ) }
      },
      result,
      0.001 );

    result = executeQuery(context.getConnection(),
      "with\n"
        + "    member Measures.[Sales Per Customer] as 'Measures.[Sales Count] / Measures.[Customer Count]'\n"
        + "    member Measures.[Coal] as 'coalesceempty(([Measures].[Sales Per Customer], [Store].[All Stores]"
        + ".[Mexico].[DF]),\n"
        + "        ([Measures].[Sales Per Customer], [Store].[All Stores].[Mexico].[DF]),\n"
        + "        ([Measures].[Sales Per Customer], [Store].[All Stores].[Mexico].[DF]),\n"
        + "        ([Measures].[Sales Per Customer], [Store].[All Stores].[Mexico].[DF]),\n"
        + "        ([Measures].[Sales Per Customer], [Store].[All Stores].[Mexico].[DF]),\n"
        + "        ([Measures].[Sales Per Customer], [Store].[All Stores].[Mexico].[DF]),\n"
        + "        ([Measures].[Sales Per Customer], [Store].[All Stores].[Mexico].[DF]),\n"
        + "        ([Measures].[Sales Per Customer], [Store].[All Stores].[Mexico].[DF]),\n"
        + "        Measures.[Sales Per Customer])'\n"
        + "select \n"
        + "    {Measures.[Sales Per Customer], Measures.[Coal]} on columns,\n"
        + "    {[Store].[All Stores].[Mexico].[DF], [Store].[All Stores].[USA].[WA]} on rows\n"
        + "from \n"
        + "    [Sales]\n"
        + "where\n"
        + "    ([Time].[1997].[Q2])" );

    checkDataResults(
      new Double[][] {
        new Double[] { null, null },
        new Double[] { new Double( 8.963 ), new Double( 8.963 ) }
      },
      result,
      0.001 );
  }
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCoalesceEmpty2(Context context) {
    Result result = executeQuery(context.getConnection(),
      "with\n"
        + "    member Measures.[Sales Per Customer] as 'Measures.[Sales Count] / Measures.[Customer Count]'\n"
        + "    member Measures.[Coal] as 'coalesceempty(([Measures].[Sales Per Customer], [Store].[All Stores]"
        + ".[Mexico].[DF]),\n"
        + "        Measures.[Sales Per Customer])'\n"
        + "select \n"
        + "    {Measures.[Sales Per Customer], Measures.[Coal]} on columns,\n"
        + "    {[Store].[All Stores].[Mexico].[DF], [Store].[All Stores].[USA].[WA]} on rows\n"
        + "from \n"
        + "    [Sales]\n"
        + "where\n"
        + "    ([Time].[1997].[Q2])" );

    checkDataResults(
      new Double[][] {
        new Double[] { null, null },
        new Double[] { 8.963D, 8.963D }
      },
      result,
      0.001 );
}

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBrokenContextBug(Context context) {
    Result result = executeQuery(context.getConnection(),
      "with\n"
        + "    member Measures.[Sales Per Customer] as 'Measures.[Sales Count] / Measures.[Customer Count]'\n"
        + "    member Measures.[Coal] as 'coalesceempty(([Measures].[Sales Per Customer], [Store].[All Stores]"
        + ".[Mexico].[DF]),\n"
        + "        Measures.[Sales Per Customer])'\n"
        + "select \n"
        + "    {Measures.[Coal]} on columns,\n"
        + "    {[Store].[All Stores].[USA].[WA]} on rows\n"
        + "from \n"
        + "    [Sales]\n"
        + "where\n"
        + "    ([Time].[1997].[Q2])" );

    checkDataResults( new Double[][] { { new Double( 8.963 ) } }, result, 0.001 );
  }

  /**
   * Tests the function <code>&lt;Set&gt;.Item(&lt;Integer&gt;)</code>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSetItemInt(Context context) {
    assertAxisReturns(context.getConnection(),
      "{[Customers].[All Customers].[USA].[OR].[Lebanon].[Mary Frances Christian]}.Item(0)",
      "[Customers].[USA].[OR].[Lebanon].[Mary Frances Christian]" );

    assertAxisReturns(context.getConnection(),
      "{[Customers].[All Customers].[USA],"
        + "[Customers].[All Customers].[USA].[WA],"
        + "[Customers].[All Customers].[USA].[CA],"
        + "[Customers].[All Customers].[USA].[OR].[Lebanon].[Mary Frances Christian]}.Item(2)",
      "[Customers].[USA].[CA]" );

    assertAxisReturns(context.getConnection(),
      "{[Customers].[All Customers].[USA],"
        + "[Customers].[All Customers].[USA].[WA],"
        + "[Customers].[All Customers].[USA].[CA],"
        + "[Customers].[All Customers].[USA].[OR].[Lebanon].[Mary Frances Christian]}.Item(100 / 50 - 1)",
      "[Customers].[USA].[WA]" );

    assertAxisReturns(context.getConnection(),
      "{([Time].[1997].[Q1].[1], [Customers].[All Customers].[USA]),"
        + "([Time].[1997].[Q1].[2], [Customers].[All Customers].[USA].[WA]),"
        + "([Time].[1997].[Q1].[3], [Customers].[All Customers].[USA].[CA]),"
        + "([Time].[1997].[Q2].[4], [Customers].[All Customers].[USA].[OR].[Lebanon].[Mary Frances Christian])}"
        + ".Item(100 / 50 - 1)",
      "{[Time].[1997].[Q1].[2], [Customers].[USA].[WA]}" );

    // given index out of bounds, item returns null
    assertAxisReturns(context.getConnection(),
      "{[Customers].[All Customers].[USA],"
        + "[Customers].[All Customers].[USA].[WA],"
        + "[Customers].[All Customers].[USA].[CA],"
        + "[Customers].[All Customers].[USA].[OR].[Lebanon].[Mary Frances Christian]}.Item(-1)",
      "" );

    // given index out of bounds, item returns null
    assertAxisReturns(context.getConnection(),
      "{[Customers].[All Customers].[USA],"
        + "[Customers].[All Customers].[USA].[WA],"
        + "[Customers].[All Customers].[USA].[CA],"
        + "[Customers].[All Customers].[USA].[OR].[Lebanon].[Mary Frances Christian]}.Item(4)",
      "" );
  }

  /**
   * Tests the function <code>&lt;Set&gt;.Item(&lt;String&gt; [,...])</code>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSetItemString(Context context) {
    assertAxisReturns(context.getConnection(),
      "{[Gender].[M], [Gender].[F]}.Item(\"M\")",
      "[Gender].[M]" );

    assertAxisReturns(context.getConnection(),
      "{CrossJoin([Gender].Members, [Marital Status].Members)}.Item(\"M\", \"S\")",
      "{[Gender].[M], [Marital Status].[S]}" );

    // MSAS fails with "duplicate dimensions across (independent) axes".
    // (That's a bug in MSAS.)
    assertAxisReturns(context.getConnection(),
      "{CrossJoin([Gender].Members, [Marital Status].Members)}.Item(\"M\", \"M\")",
      "{[Gender].[M], [Marital Status].[M]}" );

    // None found.
    assertAxisReturns(context.getConnection(),
      "{[Gender].[M], [Gender].[F]}.Item(\"X\")", "" );
    assertAxisReturns(context.getConnection(),
      "{CrossJoin([Gender].Members, [Marital Status].Members)}.Item(\"M\", \"F\")",
      "" );
    assertAxisReturns(context.getConnection(),
      "CrossJoin([Gender].Members, [Marital Status].Members).Item(\"S\", \"M\")",
      "" );

    assertAxisThrows(context.getConnection(),
      "CrossJoin([Gender].Members, [Marital Status].Members).Item(\"M\")",
      "Argument count does not match set's cardinality 2" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testTuple(Context context) {
		assertExprReturns(context.getConnection(),
				"([Gender].[M], " + "[Time].[Time].Children.Item(2), " + "[Measures].[Unit Sales])", "33,249");
		// Calc calls MemberValue with 3 args -- more efficient than
		// constructing a tuple.
		String expr = "([Gender].[M], [Time].[Time].Children.Item(2), [Measures].[Unit Sales])";
		String expectedCalc = """
mondrian.calc.impl.MemberArrayValueCalc(type=SCALAR, resultStyle=VALUE, callCount=0, callMillis=0)
    org.eclipse.daanse.olap.calc.base.constant.ConstantMemberCalc(type=MemberType<member=[Gender].[M]>, resultStyle=VALUE_NOT_NULL, callCount=0, callMillis=0)
    mondrian.olap.fun.SetItemFunDef$5(type=MemberType<hierarchy=[Time]>, resultStyle=VALUE, callCount=0, callMillis=0)
        mondrian.olap.fun.BuiltinFunTable$20$1(type=SetType<MemberType<hierarchy=[Time]>>, resultStyle=LIST, callCount=0, callMillis=0)
            mondrian.olap.fun.HierarchyCurrentMemberFunDef$CurrentMemberFixedCalc(type=MemberType<hierarchy=[Time]>, resultStyle=VALUE, callCount=0, callMillis=0)
        org.eclipse.daanse.olap.calc.base.constant.ConstantIntegerCalc(type=DecimalType(0), resultStyle=VALUE_NOT_NULL, callCount=0, callMillis=0)
    org.eclipse.daanse.olap.calc.base.constant.ConstantMemberCalc(type=MemberType<member=[Measures].[Unit Sales]>, resultStyle=VALUE_NOT_NULL, callCount=0, callMillis=0)
							""";
		assertExprCompilesTo(context.getConnection(), expr, expectedCalc);
  }

  /**
   * Tests whether the tuple operator can be applied to arguments of various types. See bug 1491699 "ClassCastException
   * in mondrian.calc.impl.GenericCalc.evaluat".
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTupleArgTypes(Context context) {
    // can coerce dimensions (if they have a unique hierarchy) and
    // hierarchies to members
    assertExprReturns(context.getConnection(),
      "([Gender], [Time].[Time])",
      "266,773" );

    // can coerce hierarchy to member
    assertExprReturns(context.getConnection(),
      "([Gender].[M], " + TimeWeekly + ")", "135,215" );

    // coerce args (hierarchy, member, member, dimension)
    assertAxisReturns(context.getConnection(),
      "{([Time.Weekly], [Measures].[Store Sales], [Marital Status].[M], [Promotion Media])}",
      "{[Time].[Weekly].[All Weeklys], [Measures].[Store Sales], [Marital Status].[M], [Promotion Media].[All "
        + "Media]}" );

    // usage of different hierarchies in the [Time] dimension
    assertAxisReturns(context.getConnection(),
      "{([Time.Weekly], [Measures].[Store Sales], [Marital Status].[M], [Time].[Time])}",
      "{[Time].[Weekly].[All Weeklys], [Measures].[Store Sales], [Marital Status].[M], [Time].[1997]}" );

    // two usages of the [Time].[Weekly] hierarchy
    if ( SystemWideProperties.instance().SsasCompatibleNaming ) {
      assertAxisThrows(context.getConnection(),
        "{([Time].[Weekly], [Measures].[Store Sales], [Marital Status].[M], [Time].[Weekly])}",
        "Tuple contains more than one member of hierarchy '[Time].[Weekly]'." );
    } else {
      assertAxisThrows(context.getConnection(),
        "{([Time.Weekly], [Measures].[Store Sales], [Marital Status].[M], [Time.Weekly])}",
        "Tuple contains more than one member of hierarchy '[Time.Weekly]'." );
    }

    // cannot coerce integer to member
    assertAxisThrows(context.getConnection(),
      "{([Gender].[M], 123)}",
      "No function matches signature '(<Member>, <Numeric Expression>)'" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTupleItem(Context context) {
    assertAxisReturns(context.getConnection(),
      "([Time].[1997].[Q1].[1], [Customers].[All Customers].[USA].[OR], [Gender].[All Gender].[M]).item(2)",
      "[Gender].[M]" );

    assertAxisReturns(context.getConnection(),
      "([Time].[1997].[Q1].[1], [Customers].[All Customers].[USA].[OR], [Gender].[All Gender].[M]).item(1)",
      "[Customers].[USA].[OR]" );

    assertAxisReturns(context.getConnection(),
      "{[Time].[1997].[Q1].[1]}.item(0)",
      "[Time].[1997].[Q1].[1]" );

    assertAxisReturns(context.getConnection(),
      "{[Time].[1997].[Q1].[1]}.Item(0).Item(0)",
      "[Time].[1997].[Q1].[1]" );

    // given out of bounds index, item returns null
    assertAxisReturns(context.getConnection(),
      "([Time].[1997].[Q1].[1], [Customers].[All Customers].[USA].[OR], [Gender].[All Gender].[M]).item(-1)",
      "" );

    // given out of bounds index, item returns null
    assertAxisReturns(context.getConnection(),
      "([Time].[1997].[Q1].[1], [Customers].[All Customers].[USA].[OR], [Gender].[All Gender].[M]).item(500)",
      "" );

    // empty set
    assertExprReturns(context.getConnection(),
      "Filter([Gender].members, 1 = 0).Item(0)",
      "" );

    // empty set of unknown type
    assertExprReturns(context.getConnection(),
      "{}.Item(3)",
      "" );

    // past end of set
    assertExprReturns(context.getConnection(),
      "{[Gender].members}.Item(4)",
      "" );

    // negative index
    assertExprReturns(context.getConnection(),
      "{[Gender].members}.Item(-50)",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTupleAppliedToUnknownHierarchy(Context context) {
    // manifestation of bug 1735821
    assertQueryReturns(context.getConnection(),
      "with \n"
        + "member [Product].[Test] as '([Product].[Food],Dimensions(0).defaultMember)' \n"
        + "select \n"
        + "{[Product].[Test], [Product].[Food]} on columns, \n"
        + "{[Measures].[Store Sales]} on rows \n"
        + "from Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Product].[Test]}\n"
        + "{[Product].[Food]}\n"
        + "Axis #2:\n"
        + "{[Measures].[Store Sales]}\n"
        + "Row #0: 191,940.00\n"
        + "Row #0: 409,035.59\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTupleDepends(Context context) {
    assertMemberExprDependsOn(context.getConnection(),
      "([Store].[USA], [Gender].[F])", "{}" );

    assertMemberExprDependsOn(context.getConnection(),
      "([Store].[USA], [Gender])", "{[Gender]}" );

    // in a scalar context, the expression depends on everything except
    // the explicitly stated dimensions
    assertExprDependsOn(context.getConnection(),
      "([Store].[USA], [Gender])",
      allHiersExcept( "[Store]" ) );

    // The result should be all dims except [Gender], but there's a small
    // bug in MemberValueCalc.dependsOn where we escalate 'might depend' to
    // 'depends' and we return that it depends on all dimensions.
    assertExprDependsOn(context.getConnection(),
      "(Dimensions('Store').CurrentMember, [Gender].[F])",
      allHiers() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testItemNull(Context context) {
    // In the following queries, MSAS returns 'Formula error - object type
    // is not valid - in an <object> base class. An error occurred during
    // attempt to get cell value'. This is because in MSAS, Item is a COM
    // function, and COM doesn't like null pointers.
    //
    // Mondrian represents null members as actual objects, so its behavior
    // is different.

    // MSAS returns error here.
    assertExprReturns(context.getConnection(),
      "Filter([Gender].members, 1 = 0).Item(0).Dimension.Name",
      "Gender" );

    // MSAS returns error here.
    assertExprReturns(context.getConnection(),
      "Filter([Gender].members, 1 = 0).Item(0).Parent",
      "" );
    assertExprReturns(context.getConnection(),
      "(Filter([Store].members, 0 = 0).Item(0).Item(0),"
        + "Filter([Store].members, 0 = 0).Item(0).Item(0))",
      "266,773" );

    if ( isDefaultNullMemberRepresentation() ) {
      // MSAS returns error here.
      assertExprReturns(context.getConnection(),
        "Filter([Gender].members, 1 = 0).Item(0).Name",
        "#null" );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTupleNull(Context context) {
    // if a tuple contains any null members, it evaluates to null
    assertQueryReturns(context.getConnection(),
      "select {[Measures].[Unit Sales]} on columns,\n"
        + " { ([Gender].[M], [Store]),\n"
        + "   ([Gender].[F], [Store].parent),\n"
        + "   ([Gender].parent, [Store])} on rows\n"
        + "from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Gender].[M], [Store].[All Stores]}\n"
        + "Row #0: 135,215\n" );

    // the set function eliminates tuples which are wholly or partially
    // null
    assertAxisReturns(context.getConnection(),
      "([Gender].parent, [Marital Status]),\n" // part null
        + " ([Gender].[M], [Marital Status].parent),\n" // part null
        + " ([Gender].parent, [Marital Status].parent),\n" // wholly null
        + " ([Gender].[M], [Marital Status])", // not null
      "{[Gender].[M], [Marital Status].[All Marital Status]}" );

    if ( isDefaultNullMemberRepresentation() ) {
      // The tuple constructor returns a null tuple if one of its
      // arguments is null -- and the Item function returns null if the
      // tuple is null.
      assertExprReturns(context.getConnection(),
        "([Gender].parent, [Marital Status]).Item(0).Name",
        "#null" );
      assertExprReturns(context.getConnection(),
        "([Gender].parent, [Marital Status]).Item(1).Name",
        "#null" );
    }
  }

  private void checkDataResults(
    Double[][] expected,
    Result result,
    final double tolerance ) {
    int[] coords = new int[ 2 ];

    for ( int row = 0; row < expected.length; row++ ) {
      coords[ 1 ] = row;
      for ( int col = 0; col < expected[ 0 ].length; col++ ) {
        coords[ 0 ] = col;

        Cell cell = result.getCell( coords );
        final Double expectedValue = expected[ row ][ col ];
        if ( expectedValue == null ) {
          assertTrue(cell.isNull(),  "Expected null value");
        } else if ( cell.isNull() ) {
          fail(
            "Cell at (" + row + ", " + col
              + ") was null, but was expecting "
              + expectedValue );
        } else {
          assertEquals(
            expectedValue,
            ( (Number) cell.getValue() ).doubleValue(),
            tolerance, "Incorrect value returned at (" + row + ", " + col + ")" );
        }
      }
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLevelMemberExpressions(Context context) {
	RolapSchemaPool.instance().clear();
    // Should return Beverly Hills in California.
    assertAxisReturns(context.getConnection(),
      "[Store].[Store City].[Beverly Hills]",
      "[Store].[USA].[CA].[Beverly Hills]" );

    // There are two months named "1" in the time dimension: one
    // for 1997 and one for 1998.  <Level>.<Member> should return
    // the first one.
    assertAxisReturns(context.getConnection(), "[Time].[Month].[1]", "[Time].[1997].[Q1].[1]" );

    // Shouldn't be able to find a member named "Q1" on the month level.
    assertAxisThrows(context.getConnection(),
      "[Time].[Month].[Q1]",
      "MDX object '[Time].[Month].[Q1]' not found in cube" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCaseTestMatch(Context context) {
    assertExprReturns(context.getConnection(),
      "CASE WHEN 1=0 THEN \"first\" WHEN 1=1 THEN \"second\" WHEN 1=2 THEN \"third\" ELSE \"fourth\" END",
      "second" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCaseTestMatchElse(Context context) {
    assertExprReturns(context.getConnection(),
      "CASE WHEN 1=0 THEN \"first\" ELSE \"fourth\" END",
      "fourth" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCaseTestMatchNoElse(Context context) {
    assertExprReturns(context.getConnection(),
      "CASE WHEN 1=0 THEN \"first\" END",
      "" );
  }

  /**
   * Testcase for bug 1799391, "Case Test function throws class cast exception"
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCaseTestReturnsMemberBug1799391(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH\n"
        + " MEMBER [Product].[CaseTest] AS\n"
        + " 'CASE\n"
        + " WHEN [Gender].CurrentMember IS [Gender].[M] THEN [Gender].[F]\n"
        + " ELSE [Gender].[F]\n"
        + " END'\n"
        + "                \n"
        + "SELECT {[Product].[CaseTest]} ON 0, {[Gender].[M]} ON 1 FROM Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Product].[CaseTest]}\n"
        + "Axis #2:\n"
        + "{[Gender].[M]}\n"
        + "Row #0: 131,558\n" );

    assertAxisReturns(context.getConnection(),
      "CASE WHEN 1+1 = 2 THEN [Gender].[F] ELSE [Gender].[F].Parent END",
      "[Gender].[F]" );

    // try case match for good measure
    assertAxisReturns(context.getConnection(),
      "CASE 1 WHEN 2 THEN [Gender].[F] ELSE [Gender].[F].Parent END",
      "[Gender].[All Gender]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCaseMatch(Context context) {
    assertExprReturns(context.getConnection(),
      "CASE 2 WHEN 1 THEN \"first\" WHEN 2 THEN \"second\" WHEN 3 THEN \"third\" ELSE \"fourth\" END",
      "second" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCaseMatchElse(Context context) {
    assertExprReturns(context.getConnection(),
      "CASE 7 WHEN 1 THEN \"first\" ELSE \"fourth\" END",
      "fourth" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCaseMatchNoElse(Context context) {
    assertExprReturns(context.getConnection(),
      "CASE 8 WHEN 0 THEN \"first\" END",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCaseTypeMismatch(Context context) {
    // type mismatch between case and else
    assertAxisThrows(context.getConnection(),
      "CASE 1 WHEN 1 THEN 2 ELSE \"foo\" END",
      "No function matches signature" );
    // type mismatch between case and case
    assertAxisThrows(context.getConnection(),
      "CASE 1 WHEN 1 THEN 2 WHEN 2 THEN \"foo\" ELSE 3 END",
      "No function matches signature" );
    // type mismatch between value and case
    assertAxisThrows(context.getConnection(),
      "CASE 1 WHEN \"foo\" THEN 2 ELSE 3 END",
      "No function matches signature" );
    // non-boolean condition
    assertAxisThrows(context.getConnection(),
      "CASE WHEN 1 = 2 THEN 3 WHEN 4 THEN 5 ELSE 6 END",
      "No function matches signature" );
  }

  /**
   * Testcase for
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-853">
   * bug MONDRIAN-853, "When using CASE WHEN in a CalculatedMember values are not returned the way expected"</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCaseTuple(Context context) {
    // The case in the bug, simplified. With the bug, returns a member array
    // "[Lmondrian.olap.Member;@151b0a5". Type deduction should realize
    // that the result is a scalar, therefore a tuple (represented by a
    // member array) needs to be evaluated to a scalar. I think that if we
    // get the type deduction right, the MDX exp compiler will handle the
    // rest.
    if ( false ) {
      assertExprReturns(context.getConnection(),
        "case 1 when 0 then 1.5\n"
          + " else ([Gender].[M], [Measures].[Unit Sales]) end",
        "135,215" );
    }

    // "case when" variant always worked
    assertExprReturns(context.getConnection(),
      "case when 1=0 then 1.5\n"
        + " else ([Gender].[M], [Measures].[Unit Sales]) end",
      "135,215" );

    // case 2: cannot deduce type (tuple x) vs. (tuple y). Should be able
    // to deduce that the result type is tuple-type<member-type<Gender>,
    // member-type<Measures>>.
    if ( false ) {
      assertExprReturns(context.getConnection(),
        "case when 1=0 then ([Gender].[M], [Measures].[Store Sales])\n"
          + " else ([Gender].[M], [Measures].[Unit Sales]) end",
        "xxx" );
    }

    // case 3: mixture of member & tuple. Should be able to deduce that
    // result type is an expression.
    if ( false ) {
      assertExprReturns(context.getConnection(),
        "case when 1=0 then ([Measures].[Store Sales])\n"
          + " else ([Gender].[M], [Measures].[Unit Sales]) end",
        "xxx" );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testPropertiesExpr(Context context) {
    assertExprReturns(context.getConnection(),
      "[Store].[USA].[CA].[Beverly Hills].[Store 6].Properties(\"Store Type\")",
      "Gourmet Supermarket" );
  }

  /**
   * Test case for bug
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-1227">MONDRIAN-1227,
   * "Properties function does not implicitly convert dimension to member; has documentation typos"</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testPropertiesOnDimension(Context context) {
    // [Store] is a dimension. When called with a property like FirstChild,
    // it is implicitly converted to a member.
    assertAxisReturns(context.getConnection(), "[Store].FirstChild", "[Store].[Canada]" );

    // The same should happen with the <Member>.Properties(<String>)
    // function; now the bug is fixed, it does. Dimension is implicitly
    // converted to member.
    assertExprReturns(context.getConnection(),
      "[Store].Properties('MEMBER_UNIQUE_NAME')",
      "[Store].[All Stores]" );

    // Hierarchy is implicitly converted to member.
    assertExprReturns(context.getConnection(),
      "[Store].[USA].Hierarchy.Properties('MEMBER_UNIQUE_NAME')",
      "[Store].[All Stores]" );
  }

  /**
   * Tests that non-existent property throws an error. *
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testPropertiesNonExistent(Context context) {
    assertExprThrows(context.getConnection(),
      "[Store].[USA].[CA].[Beverly Hills].[Store 6].Properties(\"Foo\")",
      "Property 'Foo' is not valid for" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testPropertiesFilter(Context context) {
    Result result = executeQuery(context.getConnection(),
      "SELECT { [Store Sales] } ON COLUMNS,\n"
        + " TOPCOUNT(Filter( [Store].[Store Name].Members,\n"
        + "                   [Store].CurrentMember.Properties(\"Store Type\") = \"Supermarket\"),\n"
        + "           10, [Store Sales]) ON ROWS\n"
        + "FROM [Sales]" );
    assertEquals( 8, result.getAxes()[ 1 ].getPositions().size() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testPropertyInCalculatedMember(Context context) {
    Result result = executeQuery(context.getConnection(),
      "WITH MEMBER [Measures].[Store Sales per Sqft]\n"
        + "AS '[Measures].[Store Sales] / "
        + "  [Store].CurrentMember.Properties(\"Store Sqft\")'\n"
        + "SELECT \n"
        + "  {[Measures].[Unit Sales], [Measures].[Store Sales per Sqft]} ON COLUMNS,\n"
        + "  {[Store].[Store Name].members} ON ROWS\n"
        + "FROM Sales" );
    Member member;
    Cell cell;
    member = result.getAxes()[ 1 ].getPositions().get( 18 ).get( 0 );
    assertEquals(
      "[Store].[USA].[WA].[Bellingham].[Store 2]",
      member.getUniqueName() );
    cell = result.getCell( new int[] { 0, 18 } );
    assertEquals( "2,237", cell.getFormattedValue() );
    cell = result.getCell( new int[] { 1, 18 } );
    assertEquals( ".17", cell.getFormattedValue() );
    member = result.getAxes()[ 1 ].getPositions().get( 3 ).get( 0 );
    assertEquals(
      "[Store].[Mexico].[DF].[San Andres].[Store 21]",
      member.getUniqueName() );
    cell = result.getCell( new int[] { 0, 3 } );
    assertEquals( "", cell.getFormattedValue() );
    cell = result.getCell( new int[] { 1, 3 } );
    assertEquals( "", cell.getFormattedValue() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOpeningPeriod(Context context) {
    assertAxisReturns(context.getConnection(),
      "OpeningPeriod([Time].[Month], [Time].[1997].[Q3])",
      "[Time].[1997].[Q3].[7]" );

    assertAxisReturns(context.getConnection(),
      "OpeningPeriod([Time].[Quarter], [Time].[1997])",
      "[Time].[1997].[Q1]" );

    assertAxisReturns(context.getConnection(),
      "OpeningPeriod([Time].[Year], [Time].[1997])", "[Time].[1997]" );

    assertAxisReturns(context.getConnection(),
      "OpeningPeriod([Time].[Month], [Time].[1997])",
      "[Time].[1997].[Q1].[1]" );

    assertAxisReturns(context.getConnection(),
      "OpeningPeriod([Product].[Product Name], [Product].[All Products].[Drink])",
      "[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good].[Good Imported Beer]" );

    //getTestContext().withCube( "[Sales Ragged]" ).
    assertAxisReturns(context.getConnection(), "[Sales Ragged]",
      "OpeningPeriod([Store].[Store City], [Store].[All Stores].[Israel])",
      "[Store].[Israel].[Israel].[Haifa]" );

    //getTestContext().withCube( "[Sales Ragged]" ).
    assertAxisReturns(context.getConnection(), "[Sales Ragged]",
      "OpeningPeriod([Store].[Store State], [Store].[All Stores].[Israel])",
      "" );

    // Default member is [Time].[1997].
    assertAxisReturns(context.getConnection(),
      "OpeningPeriod([Time].[Month])", "[Time].[1997].[Q1].[1]" );

    assertAxisReturns(context.getConnection(), "OpeningPeriod()", "[Time].[1997].[Q1]" );

    //TestContext testContext = getTestContext().withCube( "[Sales Ragged]" );
    assertAxisThrows(context.getConnection(),
      "OpeningPeriod([Time].[Year], [Store].[All Stores].[Israel])",
      "The <level> and <member> arguments to OpeningPeriod must be "
        + "from the same hierarchy. The level was from '[Time]' but "
        + "the member was from '[Store]'.", "[Sales Ragged]");

    assertAxisThrows(context.getConnection(),
      "OpeningPeriod([Store].[Store City])",
      "The <level> and <member> arguments to OpeningPeriod must be "
        + "from the same hierarchy. The level was from '[Store]' but "
        + "the member was from '[Time]'.", "[Sales Ragged]");
  }

  /**
   * This tests new NULL functionality exception throwing
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOpeningPeriodNull(Context context) {
    assertAxisThrows(context.getConnection(),
      "OpeningPeriod([Time].[Month], NULL)",
      "Mondrian Error:Failed to parse query 'select {OpeningPeriod([Time].[Month], NULL)} on columns from Sales'" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLastPeriods(Context context) {
    assertAxisReturns(context.getConnection(),
      "LastPeriods(0, [Time].[1998])", "" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(1, [Time].[1998])", "[Time].[1998]" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(-1, [Time].[1998])", "[Time].[1998]" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(2, [Time].[1998])",
      "[Time].[1997]\n" + "[Time].[1998]" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(-2, [Time].[1997])",
      "[Time].[1997]\n" + "[Time].[1998]" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(5000, [Time].[1998])",
      "[Time].[1997]\n" + "[Time].[1998]" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(-5000, [Time].[1997])",
      "[Time].[1997]\n" + "[Time].[1998]" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(2, [Time].[1998].[Q2])",
      "[Time].[1998].[Q1]\n" + "[Time].[1998].[Q2]" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(4, [Time].[1998].[Q2])",
      "[Time].[1997].[Q3]\n"
        + "[Time].[1997].[Q4]\n"
        + "[Time].[1998].[Q1]\n"
        + "[Time].[1998].[Q2]" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(-2, [Time].[1997].[Q2])",
      "[Time].[1997].[Q2]\n" + "[Time].[1997].[Q3]" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(-4, [Time].[1997].[Q2])",
      "[Time].[1997].[Q2]\n"
        + "[Time].[1997].[Q3]\n"
        + "[Time].[1997].[Q4]\n"
        + "[Time].[1998].[Q1]" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(5000, [Time].[1998].[Q2])",
      "[Time].[1997].[Q1]\n"
        + "[Time].[1997].[Q2]\n"
        + "[Time].[1997].[Q3]\n"
        + "[Time].[1997].[Q4]\n"
        + "[Time].[1998].[Q1]\n"
        + "[Time].[1998].[Q2]" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(-5000, [Time].[1998].[Q2])",
      "[Time].[1998].[Q2]\n"
        + "[Time].[1998].[Q3]\n"
        + "[Time].[1998].[Q4]" );

    assertAxisReturns(context.getConnection(),
      "LastPeriods(2, [Time].[1998].[Q2].[5])",
      "[Time].[1998].[Q2].[4]\n" + "[Time].[1998].[Q2].[5]" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(12, [Time].[1998].[Q2].[5])",
      "[Time].[1997].[Q2].[6]\n"
        + "[Time].[1997].[Q3].[7]\n"
        + "[Time].[1997].[Q3].[8]\n"
        + "[Time].[1997].[Q3].[9]\n"
        + "[Time].[1997].[Q4].[10]\n"
        + "[Time].[1997].[Q4].[11]\n"
        + "[Time].[1997].[Q4].[12]\n"
        + "[Time].[1998].[Q1].[1]\n"
        + "[Time].[1998].[Q1].[2]\n"
        + "[Time].[1998].[Q1].[3]\n"
        + "[Time].[1998].[Q2].[4]\n"
        + "[Time].[1998].[Q2].[5]" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(-2, [Time].[1998].[Q2].[4])",
      "[Time].[1998].[Q2].[4]\n" + "[Time].[1998].[Q2].[5]" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(-12, [Time].[1997].[Q2].[6])",
      "[Time].[1997].[Q2].[6]\n"
        + "[Time].[1997].[Q3].[7]\n"
        + "[Time].[1997].[Q3].[8]\n"
        + "[Time].[1997].[Q3].[9]\n"
        + "[Time].[1997].[Q4].[10]\n"
        + "[Time].[1997].[Q4].[11]\n"
        + "[Time].[1997].[Q4].[12]\n"
        + "[Time].[1998].[Q1].[1]\n"
        + "[Time].[1998].[Q1].[2]\n"
        + "[Time].[1998].[Q1].[3]\n"
        + "[Time].[1998].[Q2].[4]\n"
        + "[Time].[1998].[Q2].[5]" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(2, [Gender].[M])",
      "[Gender].[F]\n" + "[Gender].[M]" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(-2, [Gender].[F])",
      "[Gender].[F]\n" + "[Gender].[M]" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(2, [Gender])", "[Gender].[All Gender]" );
    assertAxisReturns(context.getConnection(),
      "LastPeriods(2, [Gender].Parent)", "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testParallelPeriod(Context context) {
    assertAxisReturns(context.getConnection(),
      "parallelperiod([Time].[Quarter], 1, [Time].[1998].[Q1])",
      "[Time].[1997].[Q4]" );

    assertAxisReturns(context.getConnection(),
      "parallelperiod([Time].[Quarter], -1, [Time].[1997].[Q1])",
      "[Time].[1997].[Q2]" );

    assertAxisReturns(context.getConnection(),
      "parallelperiod([Time].[Year], 1, [Time].[1998].[Q1])",
      "[Time].[1997].[Q1]" );

    assertAxisReturns(context.getConnection(),
      "parallelperiod([Time].[Year], 1, [Time].[1998].[Q1].[1])",
      "[Time].[1997].[Q1].[1]" );

    // No args, therefore finds parallel period to [Time].[1997], which
    // would be [Time].[1996], except that that doesn't exist, so null.
    assertAxisReturns(context.getConnection(), "ParallelPeriod()", "" );

    // Parallel period to [Time].[1997], which would be [Time].[1996],
    // except that that doesn't exist, so null.
    assertAxisReturns(context.getConnection(),
      "ParallelPeriod([Time].[Year], 1, [Time].[1997])", "" );

    // one parameter, level 2 above member
    if ( isDefaultNullMemberRepresentation() ) {
      assertQueryReturns(context.getConnection(),
        "WITH MEMBER [Measures].[Foo] AS \n"
          + " ' ParallelPeriod([Time].[Year]).UniqueName '\n"
          + "SELECT {[Measures].[Foo]} ON COLUMNS\n"
          + "FROM [Sales]\n"
          + "WHERE [Time].[1997].[Q3].[8]",
        "Axis #0:\n"
          + "{[Time].[1997].[Q3].[8]}\n"
          + "Axis #1:\n"
          + "{[Measures].[Foo]}\n"
          + "Row #0: [Time].[#null]\n" );
    }

    // one parameter, level 1 above member
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Measures].[Foo] AS \n"
        + " ' ParallelPeriod([Time].[Quarter]).UniqueName '\n"
        + "SELECT {[Measures].[Foo]} ON COLUMNS\n"
        + "FROM [Sales]\n"
        + "WHERE [Time].[1997].[Q3].[8]",
      "Axis #0:\n"
        + "{[Time].[1997].[Q3].[8]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Foo]}\n"
        + "Row #0: [Time].[1997].[Q2].[5]\n" );

    // one parameter, level same as member
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Measures].[Foo] AS \n"
        + " ' ParallelPeriod([Time].[Month]).UniqueName '\n"
        + "SELECT {[Measures].[Foo]} ON COLUMNS\n"
        + "FROM [Sales]\n"
        + "WHERE [Time].[1997].[Q3].[8]",
      "Axis #0:\n"
        + "{[Time].[1997].[Q3].[8]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Foo]}\n"
        + "Row #0: [Time].[1997].[Q3].[7]\n" );

    //  one parameter, level below member
    if ( isDefaultNullMemberRepresentation() ) {
      assertQueryReturns(context.getConnection(),
        "WITH MEMBER [Measures].[Foo] AS \n"
          + " ' ParallelPeriod([Time].[Month]).UniqueName '\n"
          + "SELECT {[Measures].[Foo]} ON COLUMNS\n"
          + "FROM [Sales]\n"
          + "WHERE [Time].[1997].[Q3]",
        "Axis #0:\n"
          + "{[Time].[1997].[Q3]}\n"
          + "Axis #1:\n"
          + "{[Measures].[Foo]}\n"
          + "Row #0: [Time].[#null]\n" );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void _testParallelPeriodThrowsException(Context context) {
    assertQueryThrows(context,
      "select {parallelperiod([Time].[Year], 1)} on columns "
        + "from [Sales] where ([Time].[1998].[Q1].[2])",
      "Hierarchy '[Time]' appears in more than one independent axis" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testParallelPeriodDepends(Context context) {
    assertMemberExprDependsOn(context.getConnection(),
      "ParallelPeriod([Time].[Quarter], 2.0)", "{[Time]}" );
    assertMemberExprDependsOn(context.getConnection(),
      "ParallelPeriod([Time].[Quarter], 2.0, [Time].[1997].[Q3])", "{}" );
    assertMemberExprDependsOn(context.getConnection(),
      "ParallelPeriod()",
      "{[Time]}" );
    assertMemberExprDependsOn(context.getConnection(),
      "ParallelPeriod([Product].[Food])", "{[Product]}" );
    // [Gender].[M] is used here as a numeric expression!
    // The numeric expression DOES depend upon [Product].
    // The expression as a whole depends upon everything except [Gender].
    String s1 = allHiersExcept( "[Gender]" );
    assertMemberExprDependsOn(context.getConnection(),
      "ParallelPeriod([Product].[Product Family], [Gender].[M], [Product].[Food])",
      s1 );
    // As above
    String s11 = allHiersExcept( "[Gender]" );
    assertMemberExprDependsOn(context.getConnection(),
      "ParallelPeriod([Product].[Product Family], [Gender].[M])", s11 );
    assertSetExprDependsOn(context.getConnection(),
      "parallelperiod([Time].[Time].CurrentMember)",
      "{[Time]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testParallelPeriodLevelLag(Context context) {
    assertQueryReturns(context.getConnection(),
      "with member [Measures].[Prev Unit Sales] as "
        + "        '([Measures].[Unit Sales], parallelperiod([Time].[Quarter], 2))' "
        + "select "
        + "    crossjoin({[Measures].[Unit Sales], [Measures].[Prev Unit Sales]}, {[Marital Status].[All Marital "
        + "Status].children}) on columns, "
        + "    {[Time].[1997].[Q3]} on rows "
        + "from  "
        + "    [Sales] ",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales], [Marital Status].[M]}\n"
        + "{[Measures].[Unit Sales], [Marital Status].[S]}\n"
        + "{[Measures].[Prev Unit Sales], [Marital Status].[M]}\n"
        + "{[Measures].[Prev Unit Sales], [Marital Status].[S]}\n"
        + "Axis #2:\n"
        + "{[Time].[1997].[Q3]}\n"
        + "Row #0: 32,815\n"
        + "Row #0: 33,033\n"
        + "Row #0: 33,101\n"
        + "Row #0: 33,190\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testParallelPeriodLevel(Context context) {
    assertQueryReturns(context.getConnection(),
      "with "
        + "    member [Measures].[Prev Unit Sales] as "
        + "        '([Measures].[Unit Sales], parallelperiod([Time].[Quarter]))' "
        + "select "
        + "    crossjoin({[Measures].[Unit Sales], [Measures].[Prev Unit Sales]}, {[Marital Status].[All Marital "
        + "Status].[M]}) on columns, "
        + "    {[Time].[1997].[Q3].[8]} on rows "
        + "from  "
        + "    [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales], [Marital Status].[M]}\n"
        + "{[Measures].[Prev Unit Sales], [Marital Status].[M]}\n"
        + "Axis #2:\n"
        + "{[Time].[1997].[Q3].[8]}\n"
        + "Row #0: 10,957\n"
        + "Row #0: 10,280\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testPlus(Context context) {
    assertExprDependsOn(context.getConnection(), "1 + 2", "{}" );
    String s1 = allHiersExcept( "[Measures]", "[Gender]" );
    assertExprDependsOn(context.getConnection(),
      "([Measures].[Unit Sales], [Gender].[F]) + 2", s1 );

    assertExprReturns(context.getConnection(), "1+2", "3" );
    assertExprReturns(context.getConnection(), "5 + " + NullNumericExpr, "5" ); // 5 + null --> 5
    assertExprReturns(context.getConnection(), NullNumericExpr + " + " + NullNumericExpr, "" );
    assertExprReturns(context.getConnection(), NullNumericExpr + " + 0", "0" );
  }


  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testPlus_NULL_plus_1(Context context) {
	  assertExprReturns(context.getConnection(),  "null + 1", "1" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testPlus_NULL_plus_0(Context context) {
	  assertExprReturns(context.getConnection(),  "null + 0", "0" );
  }
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testPlus_NULL_plus_NULL(Context context) {
	  assertExprReturns(context.getConnection(),  "null + null", "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMinus(Context context) {
    assertExprReturns(context.getConnection(), "1-3", "-2" );
    assertExprReturns(context.getConnection(), "5 - " + NullNumericExpr, "5" ); // 5 - null --> 5
    assertExprReturns(context.getConnection(), NullNumericExpr + " - - 2", "2" );
    assertExprReturns(context.getConnection(), NullNumericExpr + " - " + NullNumericExpr, "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMinus_bug1234759(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Customers].[USAMinusMexico]\n"
        + "AS '([Customers].[All Customers].[USA] - [Customers].[All Customers].[Mexico])'\n"
        + "SELECT {[Measures].[Unit Sales]} ON COLUMNS,\n"
        + "{[Customers].[All Customers].[USA], [Customers].[All Customers].[Mexico],\n"
        + "[Customers].[USAMinusMexico]} ON ROWS\n"
        + "FROM [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Customers].[USA]}\n"
        + "{[Customers].[Mexico]}\n"
        + "{[Customers].[USAMinusMexico]}\n"
        + "Row #0: 266,773\n"
        + "Row #1: \n"
        + "Row #2: 266,773\n"
        // with bug 1234759, this was null
        + "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMinusAssociativity(Context context) {
    // right-associative would give 11-(7-5) = 9, which is wrong
    assertExprReturns(context.getConnection(), "11-7-5", "-1" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMultiply(Context context) {
    assertExprReturns(context.getConnection(), "4*7", "28" );
    assertExprReturns(context.getConnection(), "5 * " + NullNumericExpr, "" ); // 5 * null --> null
    assertExprReturns(context.getConnection(), NullNumericExpr + " * - 2", "" );
    assertExprReturns(context.getConnection(), NullNumericExpr + " - " + NullNumericExpr, "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMultiplyPrecedence(Context context) {
    assertExprReturns(context.getConnection(), "3 + 4 * 5 + 6", "29" );
    assertExprReturns(context.getConnection(), "5 * 24 / 4 * 2", "60" );
    assertExprReturns(context.getConnection(), "48 / 4 / 2", "6" );
  }

  /**
   * Bug 774807 caused expressions to be mistaken for the crossjoin operator.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMultiplyBug774807(Context context) {
    final String desiredResult =
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[All Stores]}\n"
        + "Axis #2:\n"
        + "{[Measures].[Store Sales]}\n"
        + "{[Measures].[A]}\n"
        + "Row #0: 565,238.13\n"
        + "Row #1: 319,494,143,605.90\n";
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Measures].[A] AS\n"
        + " '([Measures].[Store Sales] * [Measures].[Store Sales])'\n"
        + "SELECT {[Store]} ON COLUMNS,\n"
        + " {[Measures].[Store Sales], [Measures].[A]} ON ROWS\n"
        + "FROM Sales", desiredResult );
    // as above, no parentheses
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Measures].[A] AS\n"
        + " '[Measures].[Store Sales] * [Measures].[Store Sales]'\n"
        + "SELECT {[Store]} ON COLUMNS,\n"
        + " {[Measures].[Store Sales], [Measures].[A]} ON ROWS\n"
        + "FROM Sales", desiredResult );
    // as above, plus 0
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Measures].[A] AS\n"
        + " '[Measures].[Store Sales] * [Measures].[Store Sales] + 0'\n"
        + "SELECT {[Store]} ON COLUMNS,\n"
        + " {[Measures].[Store Sales], [Measures].[A]} ON ROWS\n"
        + "FROM Sales", desiredResult );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDivide(Context context) {
    assertExprReturns(context.getConnection(), "10 / 5", "2" );
    assertExprReturns(context.getConnection(), NullNumericExpr + " / - 2", "" );
    assertExprReturns(context.getConnection(), NullNumericExpr + " / " + NullNumericExpr, "" );

    boolean origNullDenominatorProducesNull =
      context.getConfig().nullDenominatorProducesNull();
    try {
      // default behavior
      ((TestConfig)context.getConfig()).setNullDenominatorProducesNull(false);

      assertExprReturns(context.getConnection(), "-2 / " + NullNumericExpr, "Infinity" );
      assertExprReturns(context.getConnection(), "0 / 0", "NaN" );
      assertExprReturns(context.getConnection(), "-3 / (2 - 2)", "-Infinity" );

      assertExprReturns(context.getConnection(), "NULL/1", "" );
      assertExprReturns(context.getConnection(), "NULL/NULL", "" );
      assertExprReturns(context.getConnection(), "1/NULL", "Infinity" );

      // when NullOrZeroDenominatorProducesNull is set to true
      ((TestConfig)context.getConfig()).setNullDenominatorProducesNull( true );

      assertExprReturns(context.getConnection(), "-2 / " + NullNumericExpr, "" );
      assertExprReturns(context.getConnection(), "0 / 0", "NaN" );
      assertExprReturns(context.getConnection(), "-3 / (2 - 2)", "-Infinity" );

      assertExprReturns(context.getConnection(), "NULL/1", "" );
      assertExprReturns(context.getConnection(), "NULL/NULL", "" );
      assertExprReturns(context.getConnection(), "1/NULL", "" );
    } finally {
      ((TestConfig)context.getConfig()).setNullDenominatorProducesNull( origNullDenominatorProducesNull );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDividePrecedence(Context context) {
    assertExprReturns(context.getConnection(), "24 / 4 / 2 * 10 - -1", "31" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMod(Context context) {
    // the following tests are consistent with excel xp

    assertExprReturns(context.getConnection(), "mod(11, 3)", "2" );
    assertExprReturns(context.getConnection(), "mod(-12, 3)", "0" );

    // can handle non-ints, using the formula MOD(n, d) = n - d * INT(n / d)
    assertExprReturns(context.getConnection(), "mod(7.2, 3)", 1.2, 0.0001 );
    assertExprReturns(context.getConnection(), "mod(7.2, 3.2)", .8, 0.0001 );
    assertExprReturns(context.getConnection(), "mod(7.2, -3.2)", -2.4, 0.0001 );

    // per Excel doc "sign of result is same as divisor"
    assertExprReturns(context.getConnection(), "mod(3, 2)", "1" );
    assertExprReturns(context.getConnection(), "mod(-3, 2)", "1" );
    assertExprReturns(context.getConnection(), "mod(3, -2)", "-1" );
    assertExprReturns(context.getConnection(), "mod(-3, -2)", "-1" );

    assertExprThrows(context.getConnection(),
      "mod(4, 0)",
      "java.lang.ArithmeticException: / by zero" );
    assertExprThrows(context.getConnection(),
      "mod(0, 0)",
      "java.lang.ArithmeticException: / by zero" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testUnaryMinus(Context context) {
    assertExprReturns(context.getConnection(), "-3", "-3" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testUnaryMinusMember(Context context) {
    assertExprReturns(context.getConnection(),
      "- ([Measures].[Unit Sales],[Gender].[F])",
      "-131,558" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testUnaryMinusPrecedence(Context context) {
    assertExprReturns(context.getConnection(), "1 - -10.5 * 2 -3", "19" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNegativeZero(Context context) {
    assertExprReturns(context.getConnection(), "-0.0", "0" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNegativeZero1(Context context) {
    assertExprReturns(context.getConnection(), "-(0.0)", "0" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNegativeZeroSubtract(Context context) {
    assertExprReturns(context.getConnection(), "-0.0 - 0.0", "0" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNegativeZeroMultiply(Context context) {
    assertExprReturns(context.getConnection(), "-1 * 0", "0" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNegativeZeroDivide(Context context) {
    assertExprReturns(context.getConnection(), "-0.0 / 2", "0" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testString(Context context) {
    // The String(Integer,Char) function requires us to implicitly cast a
    // string to a char.
    assertQueryReturns(context.getConnection(),
      "with member measures.x as 'String(3, \"yahoo\")'\n"
        + "select measures.x on 0 from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[x]}\n"
        + "Row #0: yyy\n" );
    // String is converted to char by taking first character
    assertExprReturns(context.getConnection(), "String(3, \"yahoo\")", "yyy" ); // SSAS agrees
    // Integer is converted to char by converting to string and taking first
    // character
    if ( Bug.Ssas2005Compatible ) {
      // SSAS2005 can implicitly convert an integer (32) to a string, and
      // then to a char by taking the first character. Mondrian requires
      // an explicit cast.
      assertExprReturns(context.getConnection(), "String(3, 32)", "333" );
      assertExprReturns(context.getConnection(), "String(8, -5)", "--------" );
    } else {
      assertExprReturns(context.getConnection(), "String(3, Cast(32 as string))", "333" );
      assertExprReturns(context.getConnection(), "String(8, Cast(-5 as string))", "--------" );
    }
    // Error if length<0
    assertExprReturns(context.getConnection(), "String(0, 'x')", "" ); // SSAS agrees
    assertExprThrows(context.getConnection(),
      "String(-1, 'x')", "NegativeArraySizeException" ); // SSAS agrees
    assertExprThrows(context.getConnection(),
      "String(-200, 'x')", "NegativeArraySizeException" ); // SSAS agrees
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStringConcat(Context context) {
    assertExprReturns(context.getConnection(),
      " \"foo\" || \"bar\"  ",
      "foobar" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStringConcat2(Context context) {
    assertExprReturns(context.getConnection(),
      " \"foo\" || [Gender].[M].Name || \"\" ",
      "fooM" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAnd(Context context) {
      assertBooleanExprReturns(context.getConnection(), " 1=1 AND 2=2 ", true );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAnd2(Context context) {
      assertBooleanExprReturns(context.getConnection(), " 1=1 AND 2=0 ", false );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOr(Context context) {
      assertBooleanExprReturns(context.getConnection(), " 1=0 OR 2=0 ", false );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOr2(Context context) {
      assertBooleanExprReturns(context.getConnection(), " 1=0 OR 0=0 ", true );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrAssociativity1(Context context) {
    // Would give 'false' if OR were stronger than AND (wrong!)
      assertBooleanExprReturns(context.getConnection(), " 1=1 AND 1=0 OR 1=1 ", true );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrAssociativity2(Context context) {
    // Would give 'false' if OR were stronger than AND (wrong!)
      assertBooleanExprReturns(context.getConnection(), " 1=1 OR 1=0 AND 1=1 ", true );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrAssociativity3(Context context) {
      assertBooleanExprReturns(context.getConnection(), " (1=0 OR 1=1) AND 1=1 ", true );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testXor(Context context) {
      assertBooleanExprReturns(context.getConnection(), " 1=1 XOR 2=2 ", false );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testXorAssociativity(Context context) {
    // Would give 'false' if XOR were stronger than AND (wrong!)
      assertBooleanExprReturns(context.getConnection(), " 1 = 1 AND 1 = 1 XOR 1 = 0 ", true );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNonEmptyCrossJoin(Context context) {
    // NonEmptyCrossJoin needs to evaluate measures to find out whether
    // cells are empty, so it implicitly depends upon all dimensions.
    String s1 = allHiersExcept( "[Store]" );
    assertSetExprDependsOn(context.getConnection(),
      "NonEmptyCrossJoin([Store].[USA].Children, [Gender].Children)", s1 );

    assertAxisReturns(context.getConnection(),
      "NonEmptyCrossJoin("
        + "[Customers].[All Customers].[USA].[CA].Children, "
        + "[Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good].Children)",
      "{[Customers].[USA].[CA].[Bellflower], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]"
        + ".[Good Light Beer]}\n"
        + "{[Customers].[USA].[CA].[Downey], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]"
        + ".[Good Imported Beer]}\n"
        + "{[Customers].[USA].[CA].[Glendale], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]"
        + ".[Good Imported Beer]}\n"
        + "{[Customers].[USA].[CA].[Glendale], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]"
        + ".[Good Light Beer]}\n"
        + "{[Customers].[USA].[CA].[Grossmont], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]"
        + ".[Good Light Beer]}\n"
        + "{[Customers].[USA].[CA].[Imperial Beach], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
        + ".[Good].[Good Light Beer]}\n"
        + "{[Customers].[USA].[CA].[La Jolla], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]"
        + ".[Good Imported Beer]}\n"
        + "{[Customers].[USA].[CA].[Lincoln Acres], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
        + ".[Good].[Good Imported Beer]}\n"
        + "{[Customers].[USA].[CA].[Lincoln Acres], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
        + ".[Good].[Good Light Beer]}\n"
        + "{[Customers].[USA].[CA].[Long Beach], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
        + ".[Good].[Good Light Beer]}\n"
        + "{[Customers].[USA].[CA].[Los Angeles], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
        + ".[Good].[Good Imported Beer]}\n"
        + "{[Customers].[USA].[CA].[Newport Beach], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
        + ".[Good].[Good Imported Beer]}\n"
        + "{[Customers].[USA].[CA].[Pomona], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]"
        + ".[Good Imported Beer]}\n"
        + "{[Customers].[USA].[CA].[Pomona], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]"
        + ".[Good Light Beer]}\n"
        + "{[Customers].[USA].[CA].[San Gabriel], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
        + ".[Good].[Good Light Beer]}\n"
        + "{[Customers].[USA].[CA].[West Covina], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
        + ".[Good].[Good Imported Beer]}\n"
        + "{[Customers].[USA].[CA].[West Covina], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
        + ".[Good].[Good Light Beer]}\n"
        + "{[Customers].[USA].[CA].[Woodland Hills], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]"
        + ".[Good].[Good Imported Beer]}" );

    // empty set
    assertAxisReturns(context.getConnection(),
      "NonEmptyCrossJoin({Gender.Parent}, {Store.Parent})", "" );
    assertAxisReturns(context.getConnection(),
      "NonEmptyCrossJoin({Store.Parent}, Gender.Children)", "" );
    assertAxisReturns(context.getConnection(), "NonEmptyCrossJoin(Store.Members, {})", "" );

    // same dimension twice
    // todo: should throw
    if ( false ) {
      assertAxisThrows(context.getConnection(),
        "NonEmptyCrossJoin({Store.[USA]}, {Store.[USA].[CA]})",
        "xxx" );
    }
  }


  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNot(Context context) {
      assertBooleanExprReturns(context.getConnection(), " NOT 1=1 ", false );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNotNot(Context context) {
      assertBooleanExprReturns(context.getConnection(), " NOT NOT 1=1 ", true );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNotAssociativity(Context context) {
      assertBooleanExprReturns(context.getConnection(), " 1=1 AND NOT 1=1 OR NOT 1=1 AND 1=1 ", false );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIsNull(Context context) {
      assertBooleanExprReturns(context.getConnection(), " Measures.[Profit] IS NULL ", false );
      assertBooleanExprReturns(context.getConnection(), " Store.[All Stores] IS NULL ", false );
      assertBooleanExprReturns(context.getConnection(), " Store.[All Stores].parent IS NULL ", true );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIsMember(Context context) {
      assertBooleanExprReturns(context.getConnection(),
      " Store.[USA].parent IS Store.[All Stores]", true );
      assertBooleanExprReturns(context.getConnection(),
      " [Store].[USA].[CA].parent IS [Store].[Mexico]", false );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIsString(Context context) {
    assertExprThrows(context.getConnection(),
      " [Store].[USA].Name IS \"USA\" ",
      "No function matches signature '<String> IS <String>'" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIsNumeric(Context context) {
    assertExprThrows(context.getConnection(),
      " [Store].[USA].Level.Ordinal IS 25 ",
      "No function matches signature '<Numeric Expression> IS <Numeric Expression>'" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIsTuple(Context context) {
      assertBooleanExprReturns(context.getConnection(),
      " (Store.[USA], Gender.[M]) IS (Store.[USA], Gender.[M])", true );
      assertBooleanExprReturns(context.getConnection(),
      " (Store.[USA], Gender.[M]) IS (Gender.[M], Store.[USA])", true );
      assertBooleanExprReturns(context.getConnection(),
      " (Store.[USA], Gender.[M]) IS (Gender.[M], Store.[USA]) "
        + "OR [Gender] IS NULL",
      true );
      assertBooleanExprReturns(context.getConnection(),
      " (Store.[USA], Gender.[M]) IS (Gender.[M], Store.[USA]) "
        + "AND [Gender] IS NULL",
      false );
      assertBooleanExprReturns(context.getConnection(),
      " (Store.[USA], Gender.[M]) IS (Store.[USA], Gender.[F])",
      false );
      assertBooleanExprReturns(context.getConnection(),
      " (Store.[USA], Gender.[M]) IS (Store.[USA])",
      false );
      assertBooleanExprReturns(context.getConnection(),
      " (Store.[USA], Gender.[M]) IS Store.[USA]",
      false );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIsLevel(Context context) {
      assertBooleanExprReturns(context.getConnection(),
      " Store.[USA].level IS Store.[Store Country] ", true );
      assertBooleanExprReturns(context.getConnection(),
      " Store.[USA].[CA].level IS Store.[Store Country] ", false );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIsHierarchy(Context context) {
      assertBooleanExprReturns(context.getConnection(),
      " Store.[USA].hierarchy IS Store.[Mexico].hierarchy ", true );
      assertBooleanExprReturns(context.getConnection(),
      " Store.[USA].hierarchy IS Gender.[M].hierarchy ", false );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIsDimension(Context context) {
      assertBooleanExprReturns(context.getConnection(), " Store.[USA].dimension IS Store ", true );
      assertBooleanExprReturns(context.getConnection()," Gender.[M].dimension IS Store ", false );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStringEquals(Context context) {
      assertBooleanExprReturns(context.getConnection(), " \"foo\" = \"bar\" ", false );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStringEqualsAssociativity(Context context) {
      assertBooleanExprReturns(context.getConnection(), " \"foo\" = \"fo\" || \"o\" ", true );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStringEqualsEmpty(Context context) {
      assertBooleanExprReturns(context.getConnection(), " \"\" = \"\" ", true );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testEq(Context context) {
      assertBooleanExprReturns(context.getConnection(), " 1.0 = 1 ", true );

      assertBooleanExprReturns(context.getConnection(),
      "[Product].CurrentMember.Level.Ordinal = 2.0", false );
    checkNullOp(context.getConnection(), "=" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStringNe(Context context) {
      assertBooleanExprReturns(context.getConnection(), " \"foo\" <> \"bar\" ", true );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNe(Context context) {
      assertBooleanExprReturns(context.getConnection(), " 2 <> 1.0 + 1.0 ", false );
    checkNullOp(context.getConnection(), "<>" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNeInfinity(Context context) {
    // Infinity does not equal itself
      assertBooleanExprReturns(context.getConnection(), "(1 / 0) <> (1 / 0)", false );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLt(Context context) {
      assertBooleanExprReturns(context.getConnection(), " 2 < 1.0 + 1.0 ", false );
    checkNullOp(context.getConnection(), "<" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLe(Context context) {
      assertBooleanExprReturns(context.getConnection(), " 2 <= 1.0 + 1.0 ", true );
    checkNullOp(context.getConnection(), "<=" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGt(Context context) {
      assertBooleanExprReturns(context.getConnection(), " 2 > 1.0 + 1.0 ", false );
    checkNullOp(context.getConnection(), ">" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGe(Context context) {
      assertBooleanExprReturns(context.getConnection(), " 2 > 1.0 + 1.0 ", false );
    checkNullOp(context.getConnection(), ">=" );
  }

  private void checkNullOp(Connection connection, final String op ) {
      assertBooleanExprReturns(connection, " 0 " + op + " " + NullNumericExpr, false );
      assertBooleanExprReturns(connection, NullNumericExpr + " " + op + " 0", false );
      assertBooleanExprReturns(connection,
      NullNumericExpr + " " + op + " " + NullNumericExpr, false );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDistinctTwoMembers(Context context) {
    //getTestContext().withCube( "HR" ).
    assertAxisReturns(context.getConnection(), "HR",
      "Distinct({[Employees].[All Employees].[Sheri Nowmer].[Donna Arnold],"
        + "[Employees].[Sheri Nowmer].[Donna Arnold]})",
      "[Employees].[Sheri Nowmer].[Donna Arnold]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDistinctThreeMembers(Context context) {
    //getTestContext().withCube( "HR" ).
    assertAxisReturns(context.getConnection(), "HR",
      "Distinct({[Employees].[All Employees].[Sheri Nowmer].[Donna Arnold],"
        + "[Employees].[All Employees].[Sheri Nowmer].[Darren Stanz],"
        + "[Employees].[All Employees].[Sheri Nowmer].[Donna Arnold]})",
      "[Employees].[Sheri Nowmer].[Donna Arnold]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDistinctFourMembers(Context context) {
    //getTestContext().withCube( "HR" ).
    assertAxisReturns(context.getConnection(), "HR",
      "Distinct({[Employees].[All Employees].[Sheri Nowmer].[Donna Arnold],"
        + "[Employees].[All Employees].[Sheri Nowmer].[Darren Stanz],"
        + "[Employees].[All Employees].[Sheri Nowmer].[Donna Arnold],"
        + "[Employees].[All Employees].[Sheri Nowmer].[Darren Stanz]})",
      "[Employees].[Sheri Nowmer].[Donna Arnold]\n"
        + "[Employees].[Sheri Nowmer].[Darren Stanz]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDistinctTwoTuples(Context context) {
    assertAxisReturns(context.getConnection(),
      "Distinct({([Time].[1997],[Store].[All Stores].[Mexico]), "
        + "([Time].[1997], [Store].[All Stores].[Mexico])})",
      "{[Time].[1997], [Store].[Mexico]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDistinctSomeTuples(Context context) {
    assertAxisReturns(context.getConnection(),
      "Distinct({([Time].[1997],[Store].[All Stores].[Mexico]), "
        + "crossjoin({[Time].[1997]},{[Store].[All Stores].children})})",
      "{[Time].[1997], [Store].[Mexico]}\n"
        + "{[Time].[1997], [Store].[Canada]}\n"
        + "{[Time].[1997], [Store].[USA]}" );
  }

  /**
   * Make sure that slicer is in force when expression is applied on axis, E.g. select filter([Customers].members, [Unit
   * Sales] > 100) from sales where ([Time].[1998])
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFilterWithSlicer(Context context) {
    Result result = executeQuery(context.getConnection(),
      "select {[Measures].[Unit Sales]} on columns,\n"
        + " filter([Customers].[USA].children,\n"
        + "        [Measures].[Unit Sales] > 20000) on rows\n"
        + "from Sales\n"
        + "where ([Time].[1997].[Q1])" );
    Axis rows = result.getAxes()[ 1 ];
    // if slicer were ignored, there would be 3 rows
    assertEquals( 1, rows.getPositions().size() );
    Cell cell = result.getCell( new int[] { 0, 0 } );
    assertEquals( "30,114", cell.getFormattedValue() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIsNullWithCalcMem(Context context) {
    assertQueryReturns(context.getConnection(),
      "with member Store.foo as '1010' "
        + "member measures.bar as 'Store.currentmember IS NULL' "
        + "SELECT measures.bar on 0, {Store.foo} on 1 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[bar]}\n"
        + "Axis #2:\n"
        + "{[Store].[foo]}\n"
        + "Row #0: false\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFilterCompound(Context context) {
    Result result = executeQuery(context.getConnection(),
      "select {[Measures].[Unit Sales]} on columns,\n"
        + "  Filter(\n"
        + "    CrossJoin(\n"
        + "      [Gender].Children,\n"
        + "      [Customers].[USA].Children),\n"
        + "    [Measures].[Unit Sales] > 9500) on rows\n"
        + "from Sales\n"
        + "where ([Time].[1997].[Q1])" );
    List<Position> rows = result.getAxes()[ 1 ].getPositions();
    assertEquals( 3, rows.size() );
    assertEquals( "F", rows.get( 0 ).get( 0 ).getName() );
    assertEquals( "WA", rows.get( 0 ).get( 1 ).getName() );
    assertEquals( "M", rows.get( 1 ).get( 0 ).getName() );
    assertEquals( "OR", rows.get( 1 ).get( 1 ).getName() );
    assertEquals( "M", rows.get( 2 ).get( 0 ).getName() );
    assertEquals( "WA", rows.get( 2 ).get( 1 ).getName() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGenerateDepends(Context context) {
    assertSetExprDependsOn(context.getConnection(),
      "Generate([Product].CurrentMember.Children, Crossjoin({[Product].CurrentMember}, Crossjoin([Store].[Store "
        + "State].Members, [Store Type].Members)), ALL)",
      "{[Product]}" );
    assertSetExprDependsOn(context.getConnection(),
      "Generate([Product].[All Products].Children, Crossjoin({[Product].CurrentMember}, Crossjoin([Store].[Store "
        + "State].Members, [Store Type].Members)), ALL)",
      "{}" );
    assertSetExprDependsOn(context.getConnection(),
      "Generate({[Store].[USA], [Store].[USA].[CA]}, {[Store].CurrentMember.Children})",
      "{}" );
    assertSetExprDependsOn(context.getConnection(),
      "Generate({[Store].[USA], [Store].[USA].[CA]}, {[Gender].CurrentMember})",
      "{[Gender]}" );
    assertSetExprDependsOn(context.getConnection(),
      "Generate({[Store].[USA], [Store].[USA].[CA]}, {[Gender].[M]})",
      "{}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGenerate(Context context) {
    assertAxisReturns(context.getConnection(),
      "Generate({[Store].[USA], [Store].[USA].[CA]}, {[Store].CurrentMember.Children})",
      "[Store].[USA].[CA]\n"
        + "[Store].[USA].[OR]\n"
        + "[Store].[USA].[WA]\n"
        + "[Store].[USA].[CA].[Alameda]\n"
        + "[Store].[USA].[CA].[Beverly Hills]\n"
        + "[Store].[USA].[CA].[Los Angeles]\n"
        + "[Store].[USA].[CA].[San Diego]\n"
        + "[Store].[USA].[CA].[San Francisco]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGenerateNonSet(Context context) {
    // SSAS implicitly converts arg #2 to a set
    assertAxisReturns(context.getConnection(),
      "Generate({[Store].[USA], [Store].[USA].[CA]}, [Store].PrevMember, ALL)",
      "[Store].[Mexico]\n"
        + "[Store].[Mexico].[Zacatecas]" );

    // SSAS implicitly converts arg #1 to a set
    assertAxisReturns(context.getConnection(),
      "Generate([Store].[USA], [Store].PrevMember, ALL)",
      "[Store].[Mexico]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGenerateAll(Context context) {
    assertAxisReturns(context.getConnection(),
      "Generate({[Store].[USA].[CA], [Store].[USA].[OR].[Portland]},"
        + " Ascendants([Store].CurrentMember),"
        + " ALL)",
      "[Store].[USA].[CA]\n"
        + "[Store].[USA]\n"
        + "[Store].[All Stores]\n"
        + "[Store].[USA].[OR].[Portland]\n"
        + "[Store].[USA].[OR]\n"
        + "[Store].[USA]\n"
        + "[Store].[All Stores]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGenerateUnique(Context context) {
    assertAxisReturns(context.getConnection(),
      "Generate({[Store].[USA].[CA], [Store].[USA].[OR].[Portland]},"
        + " Ascendants([Store].CurrentMember))",
      "[Store].[USA].[CA]\n"
        + "[Store].[USA]\n"
        + "[Store].[All Stores]\n"
        + "[Store].[USA].[OR].[Portland]\n"
        + "[Store].[USA].[OR]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGenerateUniqueTuple(Context context) {
    assertAxisReturns(context.getConnection(),
      "Generate({([Store].[USA].[CA],[Product].[All Products]), "
        + "([Store].[USA].[CA],[Product].[All Products])},"
        + "{([Store].CurrentMember, [Product].CurrentMember)})",
      "{[Store].[USA].[CA], [Product].[All Products]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGenerateCrossJoin(Context context) {
    // Note that the different regions have different Top 2.
    assertAxisReturns(context.getConnection(),
      "Generate({[Store].[USA].[CA], [Store].[USA].[CA].[San Francisco]},\n"
        + "  CrossJoin({[Store].CurrentMember},\n"
        + "    TopCount([Product].[Brand Name].members, \n"
        + "    2,\n"
        + "    [Measures].[Unit Sales])))",
      "{[Store].[USA].[CA], [Product].[Food].[Produce].[Vegetables].[Fresh Vegetables].[Hermanos]}\n"
        + "{[Store].[USA].[CA], [Product].[Food].[Produce].[Vegetables].[Fresh Vegetables].[Tell Tale]}\n"
        + "{[Store].[USA].[CA].[San Francisco], [Product].[Food].[Produce].[Vegetables].[Fresh Vegetables].[Ebony]}\n"
        + "{[Store].[USA].[CA].[San Francisco], [Product].[Food].[Produce].[Vegetables].[Fresh Vegetables].[High "
        + "Top]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGenerateString(Context context) {
    assertExprReturns(context.getConnection(),
      "Generate({Time.[1997], Time.[1998]},"
        + " Time.[Time].CurrentMember.Name)",
      "19971998" );
    assertExprReturns(context.getConnection(),
      "Generate({Time.[1997], Time.[1998]},"
        + " Time.[Time].CurrentMember.Name, \" and \")",
      "1997 and 1998" );
  }

  //TODO: URGENT!!!!!
  //TODO: remove disable reset timeout time
  @Disabled
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGenerateWillTimeout(Context context) {
    ((TestConfig)context.getConfig()).setQueryTimeout(5);
    SystemWideProperties.instance().EnableNativeNonEmpty = false;
    try {
      executeAxis(context.getConnection(),
        "Generate([Product].[Product Name].members,"
          + "  Generate([Customers].[Name].members, "
          + "    {([Store].CurrentMember, [Product].CurrentMember, [Customers].CurrentMember)}))" );
    } catch ( QueryTimeoutException e ) {
      return;
    } catch ( CancellationException e ) {
      return;
    }
    fail( "should have timed out" );
  }

  // The test case for the issue: MONDRIAN-2402
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGenerateForStringMemberProperty(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Store].[Lineage of Time] AS\n"
        + " Generate(Ascendants([Time].CurrentMember), [Time].CurrentMember.Properties(\"MEMBER_CAPTION\"), \",\")\n"
        + " SELECT\n"
        + "  {[Time].[1997]} ON Axis(0),\n"
        + "  Union(\n"
        + "   {([Store].[Lineage of Time])},\n"
        + "   {[Store].[All Stores]}) ON Axis(1)\n"
        + " FROM [Sales]\n",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Time].[1997]}\n"
        + "Axis #2:\n"
        + "{[Store].[Lineage of Time]}\n"
        + "{[Store].[All Stores]}\n"
        + "Row #0: 1997\n"
        + "Row #1: 266,773\n" );
  }

  //TODO: reanable
  @Disabled //UserDefinedFunction
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFilterWillTimeout(Context context) {
    ((TestConfig)context.getConfig()).setQueryTimeout(3);
    SystemWideProperties.instance().EnableNativeNonEmpty = false;
    try {
        class TestFilterWillTimeoutModifier extends PojoMappingModifier {

            public TestFilterWillTimeoutModifier(CatalogMapping catalog) {
                super(catalog);
            }
            /* TODO: UserDefinedFunction
            @Override
            protected List<MappingUserDefinedFunction> schemaUserDefinedFunctions(MappingSchema schema) {
                List<MappingUserDefinedFunction> result = new ArrayList<>();
                result.addAll(super.schemaUserDefinedFunctions(schema));
                result.add(UserDefinedFunctionRBuilder.builder()
                    .name("SleepUdf")
                    .className(BasicQueryTest.SleepUdf.class.getName())
                    .build());
                return result;
            }*/
        }
      /*
      String baseSchema = TestUtil.getRawSchema(context);
      String schema = SchemaUtil.getSchema(baseSchema,
        null, null, null, null,
        "<UserDefinedFunction name=\"SleepUdf\" className=\""
          + BasicQueryTest.SleepUdf.class.getName()
          + "\"/>", null );
      TestUtil.withSchema(context, schema);
       */
      withSchema(context, TestFilterWillTimeoutModifier::new);
      executeAxis(context.getConnection(),
        "Filter("
          + "Filter(CrossJoin([Customers].[Name].members, [Product].[Product Name].members), SleepUdf([Measures]"
          + ".[Unit Sales]) > 0),"
          + " SleepUdf([Measures].[Sales Count]) > 5) " );
    } catch ( QueryTimeoutException e ) {
      return;
    }
    fail( "should have timed out" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHead(Context context) {
    assertAxisReturns(context.getConnection(),
      "Head([Store].Children, 2)",
      "[Store].[Canada]\n"
        + "[Store].[Mexico]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHeadNegative(Context context) {
    assertAxisReturns(context.getConnection(),
      "Head([Store].Children, 2 - 3)",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHeadDefault(Context context) {
    assertAxisReturns(context.getConnection(),
      "Head([Store].Children)",
      "[Store].[Canada]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHeadOvershoot(Context context) {
    assertAxisReturns(context.getConnection(),
      "Head([Store].Children, 2 + 2)",
      "[Store].[Canada]\n"
        + "[Store].[Mexico]\n"
        + "[Store].[USA]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHeadEmpty(Context context) {
    assertAxisReturns(context.getConnection(),
      "Head([Gender].[F].Children, 2)",
      "" );

    assertAxisReturns(context.getConnection(),
      "Head([Gender].[F].Children)",
      "" );
  }

  /**
   * Test case for bug 2488492, "Union between calc mem and head function throws exception"
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHeadBug(Context context) {
    assertQueryReturns(context.getConnection(),
      "SELECT\n"
        + "                        UNION(\n"
        + "                            {([Customers].CURRENTMEMBER)},\n"
        + "                            HEAD(\n"
        + "                                {([Customers].CURRENTMEMBER)},\n"
        + "                                IIF(\n"
        + "                                    COUNT(\n"
        + "                                        FILTER(\n"
        + "                                            DESCENDANTS(\n"
        + "                                                [Customers].CURRENTMEMBER,\n"
        + "                                                [Customers].[Country]),\n"
        + "                                            [Measures].[Unit Sales] >= 66),\n"
        + "                                        INCLUDEEMPTY)> 0,\n"
        + "                                    1,\n"
        + "                                    0)),\n"
        + "                            ALL)\n"
        + "    ON AXIS(0)\n"
        + "FROM\n"
        + "    [Sales]\n",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[All Customers]}\n"
        + "{[Customers].[All Customers]}\n"
        + "Row #0: 266,773\n"
        + "Row #0: 266,773\n" );

    assertQueryReturns(context.getConnection(),
      "WITH\n"
        + "    MEMBER\n"
        + "        [Customers].[COG_OQP_INT_t2]AS '1',\n"
        + "        SOLVE_ORDER = 65535\n"
        + "SELECT\n"
        + "                        UNION(\n"
        + "                            {([Customers].[COG_OQP_INT_t2])},\n"
        + "                            HEAD(\n"
        + "                                {([Customers].CURRENTMEMBER)},\n"
        + "                                IIF(\n"
        + "                                    COUNT(\n"
        + "                                        FILTER(\n"
        + "                                            DESCENDANTS(\n"
        + "                                                [Customers].CURRENTMEMBER,\n"
        + "                                                [Customers].[Country]),\n"
        + "                                            [Measures].[Unit Sales]>= 66),\n"
        + "                                        INCLUDEEMPTY)> 0,\n"
        + "                                    1,\n"
        + "                                    0)),\n"
        + "                            ALL)\n"
        + "    ON AXIS(0)\n"
        + "FROM\n"
        + "    [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[COG_OQP_INT_t2]}\n"
        + "{[Customers].[All Customers]}\n"
        + "Row #0: 1\n"
        + "Row #0: 266,773\n" );

    // More minimal test case. Also demonstrates similar problem with Tail.
    assertAxisReturns(context.getConnection(),
      "Union(\n"
        + "  Union(\n"
        + "    Tail([Customers].[USA].[CA].Children, 2),\n"
        + "    Head([Customers].[USA].[WA].Children, 2),\n"
        + "    ALL),\n"
        + "  Tail([Customers].[USA].[OR].Children, 2),"
        + "  ALL)",
      "[Customers].[USA].[CA].[West Covina]\n"
        + "[Customers].[USA].[CA].[Woodland Hills]\n"
        + "[Customers].[USA].[WA].[Anacortes]\n"
        + "[Customers].[USA].[WA].[Ballard]\n"
        + "[Customers].[USA].[OR].[W. Linn]\n"
        + "[Customers].[USA].[OR].[Woodburn]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHierarchize(Context context) {
    assertAxisReturns(context.getConnection(),
      "Hierarchize(\n"
        + "    {[Product].[All Products], "
        + "     [Product].[Food],\n"
        + "     [Product].[Drink],\n"
        + "     [Product].[Non-Consumable],\n"
        + "     [Product].[Food].[Eggs],\n"
        + "     [Product].[Drink].[Dairy]})",

      "[Product].[All Products]\n"
        + "[Product].[Drink]\n"
        + "[Product].[Drink].[Dairy]\n"
        + "[Product].[Food]\n"
        + "[Product].[Food].[Eggs]\n"
        + "[Product].[Non-Consumable]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHierarchizePost(Context context) {
    assertAxisReturns(context.getConnection(),
      "Hierarchize(\n"
        + "    {[Product].[All Products], "
        + "     [Product].[Food],\n"
        + "     [Product].[Food].[Eggs],\n"
        + "     [Product].[Drink].[Dairy]},\n"
        + "  POST)",

      "[Product].[Drink].[Dairy]\n"
        + "[Product].[Food].[Eggs]\n"
        + "[Product].[Food]\n"
        + "[Product].[All Products]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHierarchizePC(Context context) {
    //getTestContext().withCube( "HR" ).
    assertAxisReturns(context.getConnection(), "HR",
      "Hierarchize(\n"
        + "   { Subset([Employees].Members, 90, 10),\n"
        + "     Head([Employees].Members, 5) })",
      "[Employees].[All Employees]\n"
        + "[Employees].[Sheri Nowmer]\n"
        + "[Employees].[Sheri Nowmer].[Derrick Whelply]\n"
        + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Beverly Baker]\n"
        + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Beverly Baker].[Shauna Wyro]\n"
        + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Pedro Castillo].[Lin Conley].[Paul Tays].[Cheryl Thorton]"
        + ".[Leopoldo Renfro]\n"
        + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Pedro Castillo].[Lin Conley].[Paul Tays].[Cheryl Thorton]"
        + ".[Donna Brockett]\n"
        + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Pedro Castillo].[Lin Conley].[Paul Tays].[Cheryl Thorton]"
        + ".[Laurie Anderson]\n"
        + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Pedro Castillo].[Lin Conley].[Paul Tays].[Cheryl Thorton]"
        + ".[Louis Gomez]\n"
        + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Pedro Castillo].[Lin Conley].[Paul Tays].[Cheryl Thorton]"
        + ".[Melvin Glass]\n"
        + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Pedro Castillo].[Lin Conley].[Paul Tays].[Cheryl Thorton]"
        + ".[Kristin Cohen]\n"
        + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Pedro Castillo].[Lin Conley].[Paul Tays].[Cheryl Thorton]"
        + ".[Susan Kharman]\n"
        + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Pedro Castillo].[Lin Conley].[Paul Tays].[Cheryl Thorton]"
        + ".[Gordon Kirschner]\n"
        + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Pedro Castillo].[Lin Conley].[Paul Tays].[Cheryl Thorton]"
        + ".[Geneva Kouba]\n"
        + "[Employees].[Sheri Nowmer].[Derrick Whelply].[Pedro Castillo].[Lin Conley].[Paul Tays].[Cheryl Thorton]"
        + ".[Tricia Clark]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHierarchizeCrossJoinPre(Context context) {
    assertAxisReturns(context.getConnection(),
      "Hierarchize(\n"
        + "  CrossJoin(\n"
        + "    {[Product].[All Products], "
        + "     [Product].[Food],\n"
        + "     [Product].[Food].[Eggs],\n"
        + "     [Product].[Drink].[Dairy]},\n"
        + "    [Gender].MEMBERS),\n"
        + "  PRE)",

      "{[Product].[All Products], [Gender].[All Gender]}\n"
        + "{[Product].[All Products], [Gender].[F]}\n"
        + "{[Product].[All Products], [Gender].[M]}\n"
        + "{[Product].[Drink].[Dairy], [Gender].[All Gender]}\n"
        + "{[Product].[Drink].[Dairy], [Gender].[F]}\n"
        + "{[Product].[Drink].[Dairy], [Gender].[M]}\n"
        + "{[Product].[Food], [Gender].[All Gender]}\n"
        + "{[Product].[Food], [Gender].[F]}\n"
        + "{[Product].[Food], [Gender].[M]}\n"
        + "{[Product].[Food].[Eggs], [Gender].[All Gender]}\n"
        + "{[Product].[Food].[Eggs], [Gender].[F]}\n"
        + "{[Product].[Food].[Eggs], [Gender].[M]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHierarchizeCrossJoinPost(Context context) {
    assertAxisReturns(context.getConnection(),
      "Hierarchize(\n"
        + "  CrossJoin(\n"
        + "    {[Product].[All Products], "
        + "     [Product].[Food],\n"
        + "     [Product].[Food].[Eggs],\n"
        + "     [Product].[Drink].[Dairy]},\n"
        + "    [Gender].MEMBERS),\n"
        + "  POST)",

      "{[Product].[Drink].[Dairy], [Gender].[F]}\n"
        + "{[Product].[Drink].[Dairy], [Gender].[M]}\n"
        + "{[Product].[Drink].[Dairy], [Gender].[All Gender]}\n"
        + "{[Product].[Food].[Eggs], [Gender].[F]}\n"
        + "{[Product].[Food].[Eggs], [Gender].[M]}\n"
        + "{[Product].[Food].[Eggs], [Gender].[All Gender]}\n"
        + "{[Product].[Food], [Gender].[F]}\n"
        + "{[Product].[Food], [Gender].[M]}\n"
        + "{[Product].[Food], [Gender].[All Gender]}\n"
        + "{[Product].[All Products], [Gender].[F]}\n"
        + "{[Product].[All Products], [Gender].[M]}\n"
        + "{[Product].[All Products], [Gender].[All Gender]}" );
  }

  /**
   * Tests that the Hierarchize function works correctly when applied to a level whose ordering is determined by an
   * 'ordinal' property. TODO: fix this test (bug 1220787)
   * <p>
   * WG: Note that this is disabled right now due to its impact on other tests later on within the test suite,
   * specifically XMLA tests that return a list of cubes.  We could run this test after XMLA, or clear out the cache to
   * solve this.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHierarchizeOrdinal(Context context) {
    //TestContext testContext = getTestContext().withCube( "[Sales_Hierarchize]" );
    /*
    connection.getSchema().createCube(
      "<Cube name=\"Sales_Hierarchize\">\n"
        + "  <Table name=\"sales_fact_1997\"/>\n"
        + "  <Dimension name=\"Time_Alphabetical\" type=\"TimeDimension\" foreignKey=\"time_id\">\n"
        + "    <Hierarchy hasAll=\"false\" primaryKey=\"time_id\">\n"
        + "      <Table name=\"time_by_day\"/>\n"
        + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
        + "          levelType=\"TimeYears\"/>\n"
        + "      <Level name=\"Quarter\" column=\"quarter\" uniqueMembers=\"false\"\n"
        + "          levelType=\"TimeQuarters\"/>\n"
        + "      <Level name=\"Month\" column=\"month_of_year\" uniqueMembers=\"false\" type=\"Numeric\"\n"
        + "          ordinalColumn=\"the_month\"\n"
        + "          levelType=\"TimeMonths\"/>\n"
        + "    </Hierarchy>\n"
        + "  </Dimension>\n"
        + "\n"
        + "  <Dimension name=\"Month_Alphabetical\" type=\"TimeDimension\" foreignKey=\"time_id\">\n"
        + "    <Hierarchy hasAll=\"false\" primaryKey=\"time_id\">\n"
        + "      <Table name=\"time_by_day\"/>\n"
        + "      <Level name=\"Month\" column=\"month_of_year\" uniqueMembers=\"false\" type=\"Numeric\"\n"
        + "          ordinalColumn=\"the_month\"\n"
        + "          levelType=\"TimeMonths\"/>\n"
        + "    </Hierarchy>\n"
        + "  </Dimension>\n"
        + "\n"
        + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
        + "      formatString=\"Standard\"/>\n"
        + "</Cube>" );
     */
    withSchema(context, SchemaModifiers.FunctionTestModifier3::new);
    final Connection connection = context.getConnection();

    // The [Time_Alphabetical] is ordered alphabetically by month
    assertAxisReturns(connection, "[Sales_Hierarchize]",
      "Hierarchize([Time_Alphabetical].members)",
      "[Time_Alphabetical].[1997]\n"
        + "[Time_Alphabetical].[1997].[Q1]\n"
        + "[Time_Alphabetical].[1997].[Q1].[2]\n"
        + "[Time_Alphabetical].[1997].[Q1].[1]\n"
        + "[Time_Alphabetical].[1997].[Q1].[3]\n"
        + "[Time_Alphabetical].[1997].[Q2]\n"
        + "[Time_Alphabetical].[1997].[Q2].[4]\n"
        + "[Time_Alphabetical].[1997].[Q2].[6]\n"
        + "[Time_Alphabetical].[1997].[Q2].[5]\n"
        + "[Time_Alphabetical].[1997].[Q3]\n"
        + "[Time_Alphabetical].[1997].[Q3].[8]\n"
        + "[Time_Alphabetical].[1997].[Q3].[7]\n"
        + "[Time_Alphabetical].[1997].[Q3].[9]\n"
        + "[Time_Alphabetical].[1997].[Q4]\n"
        + "[Time_Alphabetical].[1997].[Q4].[12]\n"
        + "[Time_Alphabetical].[1997].[Q4].[11]\n"
        + "[Time_Alphabetical].[1997].[Q4].[10]\n"
        + "[Time_Alphabetical].[1998]\n"
        + "[Time_Alphabetical].[1998].[Q1]\n"
        + "[Time_Alphabetical].[1998].[Q1].[2]\n"
        + "[Time_Alphabetical].[1998].[Q1].[1]\n"
        + "[Time_Alphabetical].[1998].[Q1].[3]\n"
        + "[Time_Alphabetical].[1998].[Q2]\n"
        + "[Time_Alphabetical].[1998].[Q2].[4]\n"
        + "[Time_Alphabetical].[1998].[Q2].[6]\n"
        + "[Time_Alphabetical].[1998].[Q2].[5]\n"
        + "[Time_Alphabetical].[1998].[Q3]\n"
        + "[Time_Alphabetical].[1998].[Q3].[8]\n"
        + "[Time_Alphabetical].[1998].[Q3].[7]\n"
        + "[Time_Alphabetical].[1998].[Q3].[9]\n"
        + "[Time_Alphabetical].[1998].[Q4]\n"
        + "[Time_Alphabetical].[1998].[Q4].[12]\n"
        + "[Time_Alphabetical].[1998].[Q4].[11]\n"
        + "[Time_Alphabetical].[1998].[Q4].[10]" );

    // The [Month_Alphabetical] is a single-level hierarchy ordered
    // alphabetically by month.
    assertAxisReturns(connection, "[Sales_Hierarchize]",
      "Hierarchize([Month_Alphabetical].members)",
      "[Month_Alphabetical].[4]\n"
        + "[Month_Alphabetical].[8]\n"
        + "[Month_Alphabetical].[12]\n"
        + "[Month_Alphabetical].[2]\n"
        + "[Month_Alphabetical].[1]\n"
        + "[Month_Alphabetical].[7]\n"
        + "[Month_Alphabetical].[6]\n"
        + "[Month_Alphabetical].[3]\n"
        + "[Month_Alphabetical].[5]\n"
        + "[Month_Alphabetical].[11]\n"
        + "[Month_Alphabetical].[10]\n"
        + "[Month_Alphabetical].[9]" );

    // clear the cache so that future tests don't fail that expect a
    // specific set of cubes
    TestUtil.flushSchemaCache(connection);
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIntersectAll(Context context) {
    // Note: duplicates retained from left, not from right; and order is
    // preserved.
    assertAxisReturns(context.getConnection(),
      "Intersect({[Time].[1997].[Q2], [Time].[1997], [Time].[1997].[Q1], [Time].[1997].[Q2]}, "
        + "{[Time].[1998], [Time].[1997], [Time].[1997].[Q2], [Time].[1997]}, "
        + "ALL)",
      "[Time].[1997].[Q2]\n"
        + "[Time].[1997]\n"
        + "[Time].[1997].[Q2]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIntersect(Context context) {
    // Duplicates not preserved. Output in order that first duplicate
    // occurred.
    assertAxisReturns(context.getConnection(),
      "Intersect(\n"
        + "  {[Time].[1997].[Q2], [Time].[1997], [Time].[1997].[Q1], [Time].[1997].[Q2]}, "
        + "{[Time].[1998], [Time].[1997], [Time].[1997].[Q2], [Time].[1997]})",
      "[Time].[1997].[Q2]\n"
        + "[Time].[1997]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIntersectTuples(Context context) {
    assertAxisReturns(context.getConnection(),
      "Intersect(\n"
        + "  {([Time].[1997].[Q2], [Gender].[M]),\n"
        + "   ([Time].[1997], [Gender].[F]),\n"
        + "   ([Time].[1997].[Q1], [Gender].[M]),\n"
        + "   ([Time].[1997].[Q2], [Gender].[M])},\n"
        + "  {([Time].[1998], [Gender].[F]),\n"
        + "   ([Time].[1997], [Gender].[F]),\n"
        + "   ([Time].[1997].[Q2], [Gender].[M]),\n"
        + "   ([Time].[1997], [Gender])})",
      "{[Time].[1997].[Q2], [Gender].[M]}\n"
        + "{[Time].[1997], [Gender].[F]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIntersectRightEmpty(Context context) {
    assertAxisReturns(context.getConnection(),
      "Intersect({[Time].[1997]}, {})",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIntersectLeftEmpty(Context context) {
    assertAxisReturns(context.getConnection(),
      "Intersect({}, {[Store].[USA].[CA]})",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderDepends(Context context) {
    // Order(<Set>, <Value Expression>) depends upon everything
    // <Value Expression> depends upon, except the dimensions of <Set>.

    // Depends upon everything EXCEPT [Product], [Measures],
    // [Marital Status], [Gender].
    String s11 = allHiersExcept(
      "[Product]", "[Measures]", "[Marital Status]", "[Gender]" );
    assertSetExprDependsOn(context.getConnection(),
      "Order("
        + " Crossjoin([Gender].MEMBERS, [Product].MEMBERS),"
        + " ([Measures].[Unit Sales], [Marital Status].[S]),"
        + " ASC)",
      s11 );

    // Depends upon everything EXCEPT [Product], [Measures],
    // [Marital Status]. Does depend upon [Gender].
    String s12 = allHiersExcept(
      "[Product]", "[Measures]", "[Marital Status]" );
    assertSetExprDependsOn(context.getConnection(),
      "Order("
        + " Crossjoin({[Gender].CurrentMember}, [Product].MEMBERS),"
        + " ([Measures].[Unit Sales], [Marital Status].[S]),"
        + " ASC)",
      s12 );

    // Depends upon everything except [Measures].
    String s13 = allHiersExcept( "[Measures]" );
    assertSetExprDependsOn(context.getConnection(),
      "Order("
        + "  Crossjoin("
        + "    [Gender].CurrentMember.Children, "
        + "    [Marital Status].CurrentMember.Children), "
        + "  [Measures].[Unit Sales], "
        + "  BDESC)",
      s13 );

    String s1 = allHiersExcept(
      "[Measures]", "[Store]", "[Product]", "[Time]" );
    assertSetExprDependsOn(context.getConnection(),
      "  Order(\n"
        + "    CrossJoin(\n"
        + "      {[Product].[All Products].[Food].[Eggs],\n"
        + "       [Product].[All Products].[Food].[Seafood],\n"
        + "       [Product].[All Products].[Drink].[Alcoholic Beverages]},\n"
        + "      {[Store].[USA].[WA].[Seattle],\n"
        + "       [Store].[USA].[CA],\n"
        + "       [Store].[USA].[OR]}),\n"
        + "    ([Time].[1997].[Q1], [Measures].[Unit Sales]),\n"
        + "    ASC)",
      s1 );
  }

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testOrderCalc1(Context context) {

		// [Measures].[Unit Sales] is a constant member, so it is evaluated in
		// a ContextCalc.
		Connection connection = context.getConnection();

		String expr = "order([Product].children, [Measures].[Unit Sales])";
		String expected = """
			  mondrian.olap.fun.OrderFunDef$CurrentMemberCalc(type=SetType<MemberType<hierarchy=[Product]>>, resultStyle=MUTABLE_LIST, callCount=0, callMillis=0, direction=ASC)
         mondrian.olap.fun.BuiltinFunTable$20$1(type=SetType<MemberType<hierarchy=[Product]>>, resultStyle=LIST, callCount=0, callMillis=0)
             mondrian.olap.fun.HierarchyCurrentMemberFunDef$CurrentMemberFixedCalc(type=MemberType<hierarchy=[Product]>, resultStyle=VALUE, callCount=0, callMillis=0)
         mondrian.calc.impl.MemberValueCalc(type=SCALAR, resultStyle=VALUE, callCount=0, callMillis=0)
             org.eclipse.daanse.olap.calc.base.constant.ConstantMemberCalc(type=MemberType<member=[Measures].[Unit Sales]>, resultStyle=VALUE_NOT_NULL, callCount=0, callMillis=0)
					""";
		assertAxisCompilesTo(connection, expr, expected);
	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testOrderCalc2(Context context) {
		Connection connection = context.getConnection();

		String expr = "order([Product].children, ([Time].[1997], [Product].CurrentMember.Parent))";
		String expected = """
mondrian.olap.fun.OrderFunDef$CurrentMemberCalc(type=SetType<MemberType<hierarchy=[Product]>>, resultStyle=MUTABLE_LIST, callCount=0, callMillis=0, direction=ASC)
    mondrian.olap.fun.BuiltinFunTable$20$1(type=SetType<MemberType<hierarchy=[Product]>>, resultStyle=LIST, callCount=0, callMillis=0)
        mondrian.olap.fun.HierarchyCurrentMemberFunDef$CurrentMemberFixedCalc(type=MemberType<hierarchy=[Product]>, resultStyle=VALUE, callCount=0, callMillis=0)
    mondrian.calc.impl.MemberArrayValueCalc(type=SCALAR, resultStyle=VALUE, callCount=0, callMillis=0)
        org.eclipse.daanse.olap.calc.base.constant.ConstantMemberCalc(type=MemberType<member=[Time].[1997]>, resultStyle=VALUE_NOT_NULL, callCount=0, callMillis=0)
        mondrian.olap.fun.BuiltinFunTable$14$1(type=MemberType<hierarchy=[Product]>, resultStyle=VALUE, callCount=0, callMillis=0)
            mondrian.olap.fun.HierarchyCurrentMemberFunDef$CurrentMemberFixedCalc(type=MemberType<hierarchy=[Product]>, resultStyle=VALUE, callCount=0, callMillis=0)
				""";
		// [Time].[1997] is constant, and is evaluated in a ContextCalc.
		// [Product].Parent is variable, and is evaluated inside the loop.
		assertAxisCompilesTo(connection, expr, expected);
	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testOrderCalc3(Context context) {
		Connection connection = context.getConnection();
		// No ContextCalc this time. All members are non-variable.
		String expr = "order([Product].children, [Product].CurrentMember.Parent)";
		String expected = """
mondrian.olap.fun.OrderFunDef$CurrentMemberCalc(type=SetType<MemberType<hierarchy=[Product]>>, resultStyle=MUTABLE_LIST, callCount=0, callMillis=0, direction=ASC)
    mondrian.olap.fun.BuiltinFunTable$20$1(type=SetType<MemberType<hierarchy=[Product]>>, resultStyle=LIST, callCount=0, callMillis=0)
        mondrian.olap.fun.HierarchyCurrentMemberFunDef$CurrentMemberFixedCalc(type=MemberType<hierarchy=[Product]>, resultStyle=VALUE, callCount=0, callMillis=0)
    mondrian.calc.impl.MemberValueCalc(type=SCALAR, resultStyle=VALUE, callCount=0, callMillis=0)
        mondrian.olap.fun.BuiltinFunTable$14$1(type=MemberType<hierarchy=[Product]>, resultStyle=VALUE, callCount=0, callMillis=0)
            mondrian.olap.fun.HierarchyCurrentMemberFunDef$CurrentMemberFixedCalc(type=MemberType<hierarchy=[Product]>, resultStyle=VALUE, callCount=0, callMillis=0)
						""";
		assertAxisCompilesTo(connection, expr, expected);
	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testOrderCalc4(Context context) {
		Connection connection = context.getConnection();

		String expected = """
				mondrian.olap.fun.OrderFunDef$CurrentMemberCalc(type=SetType<MemberType<hierarchy=[Product]>>, resultStyle=MUTABLE_LIST, callCount=0, callMillis=0, direction=ASC)
        mondrian.olap.fun.FilterFunDef$ImmutableIterCalc(type=SetType<MemberType<hierarchy=[Product]>>, resultStyle=ITERABLE, callCount=0, callMillis=0)
            mondrian.olap.fun.BuiltinFunTable$20$1(type=SetType<MemberType<hierarchy=[Product]>>, resultStyle=LIST, callCount=0, callMillis=0)
                mondrian.olap.fun.HierarchyCurrentMemberFunDef$CurrentMemberFixedCalc(type=MemberType<hierarchy=[Product]>, resultStyle=VALUE, callCount=0, callMillis=0)
            mondrian.olap.fun.BuiltinFunTable$60$1(type=BOOLEAN, resultStyle=VALUE, callCount=0, callMillis=0)
                mondrian.calc.impl.AbstractExpCompiler$UnknownToDoubleCalc(type=NUMERIC, resultStyle=VALUE, callCount=0, callMillis=0)
                    mondrian.calc.impl.MemberValueCalc(type=SCALAR, resultStyle=VALUE, callCount=0, callMillis=0)
                        org.eclipse.daanse.olap.calc.base.constant.ConstantMemberCalc(type=MemberType<member=[Measures].[Unit Sales]>, resultStyle=VALUE_NOT_NULL, callCount=0, callMillis=0)
                org.eclipse.daanse.olap.calc.base.constant.ConstantDoubleCalc(type=NUMERIC, resultStyle=VALUE_NOT_NULL, callCount=0, callMillis=0)
        mondrian.calc.impl.MemberArrayValueCalc(type=SCALAR, resultStyle=VALUE, callCount=0, callMillis=0)
            org.eclipse.daanse.olap.calc.base.constant.ConstantMemberCalc(type=MemberType<member=[Gender].[M]>, resultStyle=VALUE_NOT_NULL, callCount=0, callMillis=0)
            org.eclipse.daanse.olap.calc.base.constant.ConstantMemberCalc(type=MemberType<member=[Measures].[Store Sales]>, resultStyle=VALUE_NOT_NULL, callCount=0, callMillis=0)
								""";
		String expr = "order(filter([Product].children, [Measures].[Unit Sales] > 1000), ([Gender].[M], [Measures].[Store Sales]))";
		assertAxisCompilesTo(connection, expr, expected);
	}

  /**
   * Verifies that the order function works with a defined member. See this forum post for additional information:
   * http://forums.pentaho.com/showthread.php?p=179473#post179473
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderWithMember(Context context) {
    assertQueryReturns(context.getConnection(),
      "with member [Measures].[Product Name Length] as "
        + "'LEN([Product].CurrentMember.Name)'\n"
        + "select {[Measures].[Product Name Length]} ON COLUMNS,\n"
        + "Order([Product].[All Products].Children, "
        + "[Measures].[Product Name Length], BASC) ON ROWS\n"
        + "from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Product Name Length]}\n"
        + "Axis #2:\n"
        + "{[Product].[Food]}\n"
        + "{[Product].[Drink]}\n"
        + "{[Product].[Non-Consumable]}\n"
        + "Row #0: 4\n"
        + "Row #1: 5\n"
        + "Row #2: 14\n" );
  }

  /**
   * test case for bug # 1797159, Potential MDX Order Non Empty Problem
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderNonEmpty(Context context) {
    assertQueryReturns(context.getConnection(),
      "select NON EMPTY [Gender].Members ON COLUMNS,\n"
        + "NON EMPTY Order([Product].[All Products].[Drink].Children,\n"
        + "[Gender].[All Gender].[F], ASC) ON ROWS\n"
        + "from [Sales]\n"
        + "where ([Customers].[All Customers].[USA].[CA].[San Francisco],\n"
        + " [Time].[1997])",

      "Axis #0:\n"
        + "{[Customers].[USA].[CA].[San Francisco], [Time].[1997]}\n"
        + "Axis #1:\n"
        + "{[Gender].[All Gender]}\n"
        + "{[Gender].[F]}\n"
        + "{[Gender].[M]}\n"
        + "Axis #2:\n"
        + "{[Product].[Drink].[Beverages]}\n"
        + "{[Product].[Drink].[Alcoholic Beverages]}\n"
        + "Row #0: 2\n"
        + "Row #0: \n"
        + "Row #0: 2\n"
        + "Row #1: 4\n"
        + "Row #1: 2\n"
        + "Row #1: 2\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrder(Context context) {
    assertQueryReturns(context.getConnection(),
      "select {[Measures].[Unit Sales]} on columns,\n"
        + " order({\n"
        + "  [Product].[All Products].[Drink],\n"
        + "  [Product].[All Products].[Drink].[Beverages],\n"
        + "  [Product].[All Products].[Drink].[Dairy],\n"
        + "  [Product].[All Products].[Food],\n"
        + "  [Product].[All Products].[Food].[Baked Goods],\n"
        + "  [Product].[All Products].[Food].[Eggs],\n"
        + "  [Product].[All Products]},\n"
        + " [Measures].[Unit Sales]) on rows\n"
        + "from Sales",

      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Product].[All Products]}\n"
        + "{[Product].[Drink]}\n"
        + "{[Product].[Drink].[Dairy]}\n"
        + "{[Product].[Drink].[Beverages]}\n"
        + "{[Product].[Food]}\n"
        + "{[Product].[Food].[Eggs]}\n"
        + "{[Product].[Food].[Baked Goods]}\n"
        + "Row #0: 266,773\n"
        + "Row #1: 24,597\n"
        + "Row #2: 4,186\n"
        + "Row #3: 13,573\n"
        + "Row #4: 191,940\n"
        + "Row #5: 4,132\n"
        + "Row #6: 7,870\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderParentsMissing(Context context) {
    // Paradoxically, [Alcoholic Beverages] comes before
    // [Eggs] even though it has a larger value, because
    // its parent [Drink] has a smaller value than [Food].
    assertQueryReturns(context.getConnection(),
      "select {[Measures].[Unit Sales]} on columns,"
        + " order({\n"
        + "  [Product].[All Products].[Drink].[Alcoholic Beverages],\n"
        + "  [Product].[All Products].[Food].[Eggs]},\n"
        + " [Measures].[Unit Sales], ASC) on rows\n"
        + "from Sales",

      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Product].[Drink].[Alcoholic Beverages]}\n"
        + "{[Product].[Food].[Eggs]}\n"
        + "Row #0: 6,838\n"
        + "Row #1: 4,132\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderCrossJoinBreak(Context context) {
    assertQueryReturns(context.getConnection(),
      "select {[Measures].[Unit Sales]} on columns,\n"
        + "  Order(\n"
        + "    CrossJoin(\n"
        + "      [Gender].children,\n"
        + "      [Marital Status].children),\n"
        + "    [Measures].[Unit Sales],\n"
        + "    BDESC) on rows\n"
        + "from Sales\n"
        + "where [Time].[1997].[Q1]",

      "Axis #0:\n"
        + "{[Time].[1997].[Q1]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Gender].[M], [Marital Status].[S]}\n"
        + "{[Gender].[F], [Marital Status].[M]}\n"
        + "{[Gender].[M], [Marital Status].[M]}\n"
        + "{[Gender].[F], [Marital Status].[S]}\n"
        + "Row #0: 17,070\n"
        + "Row #1: 16,790\n"
        + "Row #2: 16,311\n"
        + "Row #3: 16,120\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderCrossJoin(Context context) {
    // Note:
    // 1. [Alcoholic Beverages] collates before [Eggs] and
    //    [Seafood] because its parent, [Drink], is less
    //    than [Food]
    // 2. [Seattle] generally sorts after [CA] and [OR]
    //    because invisible parent [WA] is greater.
    assertQueryReturns(context.getConnection(),
      "select CrossJoin(\n"
        + "    {[Time].[1997],\n"
        + "     [Time].[1997].[Q1]},\n"
        + "    {[Measures].[Unit Sales]}) on columns,\n"
        + "  Order(\n"
        + "    CrossJoin(\n"
        + "      {[Product].[All Products].[Food].[Eggs],\n"
        + "       [Product].[All Products].[Food].[Seafood],\n"
        + "       [Product].[All Products].[Drink].[Alcoholic Beverages]},\n"
        + "      {[Store].[USA].[WA].[Seattle],\n"
        + "       [Store].[USA].[CA],\n"
        + "       [Store].[USA].[OR]}),\n"
        + "    ([Time].[1997].[Q1], [Measures].[Unit Sales]),\n"
        + "    ASC) on rows\n"
        + "from Sales",

      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Time].[1997], [Measures].[Unit Sales]}\n"
        + "{[Time].[1997].[Q1], [Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Product].[Drink].[Alcoholic Beverages], [Store].[USA].[OR]}\n"
        + "{[Product].[Drink].[Alcoholic Beverages], [Store].[USA].[CA]}\n"
        + "{[Product].[Drink].[Alcoholic Beverages], [Store].[USA].[WA].[Seattle]}\n"
        + "{[Product].[Food].[Seafood], [Store].[USA].[CA]}\n"
        + "{[Product].[Food].[Seafood], [Store].[USA].[OR]}\n"
        + "{[Product].[Food].[Seafood], [Store].[USA].[WA].[Seattle]}\n"
        + "{[Product].[Food].[Eggs], [Store].[USA].[CA]}\n"
        + "{[Product].[Food].[Eggs], [Store].[USA].[OR]}\n"
        + "{[Product].[Food].[Eggs], [Store].[USA].[WA].[Seattle]}\n"
        + "Row #0: 1,680\n"
        + "Row #0: 393\n"
        + "Row #1: 1,936\n"
        + "Row #1: 431\n"
        + "Row #2: 635\n"
        + "Row #2: 142\n"
        + "Row #3: 441\n"
        + "Row #3: 91\n"
        + "Row #4: 451\n"
        + "Row #4: 107\n"
        + "Row #5: 217\n"
        + "Row #5: 44\n"
        + "Row #6: 1,116\n"
        + "Row #6: 240\n"
        + "Row #7: 1,119\n"
        + "Row #7: 251\n"
        + "Row #8: 373\n"
        + "Row #8: 57\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderHierarchicalDesc(Context context) {
    assertAxisReturns(context.getConnection(),
      "Order(\n"
        + "    {[Product].[All Products], "
        + "     [Product].[Food],\n"
        + "     [Product].[Drink],\n"
        + "     [Product].[Non-Consumable],\n"
        + "     [Product].[Food].[Eggs],\n"
        + "     [Product].[Drink].[Dairy]},\n"
        + "  [Measures].[Unit Sales],\n"
        + "  DESC)",

      "[Product].[All Products]\n"
        + "[Product].[Food]\n"
        + "[Product].[Food].[Eggs]\n"
        + "[Product].[Non-Consumable]\n"
        + "[Product].[Drink]\n"
        + "[Product].[Drink].[Dairy]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderCrossJoinDesc(Context context) {
    assertAxisReturns(context.getConnection(),
      "Order(\n"
        + "  CrossJoin(\n"
        + "    {[Gender].[M], [Gender].[F]},\n"
        + "    {[Product].[All Products], "
        + "     [Product].[Food],\n"
        + "     [Product].[Drink],\n"
        + "     [Product].[Non-Consumable],\n"
        + "     [Product].[Food].[Eggs],\n"
        + "     [Product].[Drink].[Dairy]}),\n"
        + "  [Measures].[Unit Sales],\n"
        + "  DESC)",

      "{[Gender].[M], [Product].[All Products]}\n"
        + "{[Gender].[M], [Product].[Food]}\n"
        + "{[Gender].[M], [Product].[Food].[Eggs]}\n"
        + "{[Gender].[M], [Product].[Non-Consumable]}\n"
        + "{[Gender].[M], [Product].[Drink]}\n"
        + "{[Gender].[M], [Product].[Drink].[Dairy]}\n"
        + "{[Gender].[F], [Product].[All Products]}\n"
        + "{[Gender].[F], [Product].[Food]}\n"
        + "{[Gender].[F], [Product].[Food].[Eggs]}\n"
        + "{[Gender].[F], [Product].[Non-Consumable]}\n"
        + "{[Gender].[F], [Product].[Drink]}\n"
        + "{[Gender].[F], [Product].[Drink].[Dairy]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderBug656802(Context context) {
    // Note:
    // 1. [Alcoholic Beverages] collates before [Eggs] and
    //    [Seafood] because its parent, [Drink], is less
    //    than [Food]
    // 2. [Seattle] generally sorts after [CA] and [OR]
    //    because invisible parent [WA] is greater.
    assertQueryReturns(context.getConnection(),
      "select {[Measures].[Unit Sales], [Measures].[Store Cost], [Measures].[Store Sales]} ON columns, \n"
        + "Order(\n"
        + "  ToggleDrillState(\n"
        + "    {([Promotion Media].[All Media], [Product].[All Products])},\n"
        + "    {[Product].[All Products]}), \n"
        + "  [Measures].[Unit Sales], DESC) ON rows \n"
        + "from [Sales] where ([Time].[1997])",

      "Axis #0:\n"
        + "{[Time].[1997]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Store Cost]}\n"
        + "{[Measures].[Store Sales]}\n"
        + "Axis #2:\n"
        + "{[Promotion Media].[All Media], [Product].[All Products]}\n"
        + "{[Promotion Media].[All Media], [Product].[Food]}\n"
        + "{[Promotion Media].[All Media], [Product].[Non-Consumable]}\n"
        + "{[Promotion Media].[All Media], [Product].[Drink]}\n"
        + "Row #0: 266,773\n"
        + "Row #0: 225,627.23\n"
        + "Row #0: 565,238.13\n"
        + "Row #1: 191,940\n"
        + "Row #1: 163,270.72\n"
        + "Row #1: 409,035.59\n"
        + "Row #2: 50,236\n"
        + "Row #2: 42,879.28\n"
        + "Row #2: 107,366.33\n"
        + "Row #3: 24,597\n"
        + "Row #3: 19,477.23\n"
        + "Row #3: 48,836.21\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderBug712702_Simplified(Context context) {
    assertQueryReturns(context.getConnection(),
      "SELECT Order({[Time].[Year].members}, [Measures].[Unit Sales]) on columns\n"
        + "from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Time].[1998]}\n"
        + "{[Time].[1997]}\n"
        + "Row #0: \n"
        + "Row #0: 266,773\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderBug712702_Original(Context context) {
    assertQueryReturns(context.getConnection(),
      "with member [Measures].[Average Unit Sales] as 'Avg(Descendants([Time].[Time].CurrentMember, [Time].[Month]), \n"
        + "[Measures].[Unit Sales])' \n"
        + "member [Measures].[Max Unit Sales] as 'Max(Descendants([Time].[Time].CurrentMember, [Time].[Month]), "
        + "[Measures].[Unit Sales])' \n"
        + "select {[Measures].[Average Unit Sales], [Measures].[Max Unit Sales], [Measures].[Unit Sales]} ON columns,"
        + " \n"
        + "  NON EMPTY Order(\n"
        + "    Crossjoin(\n"
        + "      {[Store].[USA].[OR].[Portland],\n"
        + "       [Store].[USA].[OR].[Salem],\n"
        + "       [Store].[USA].[OR].[Salem].[Store 13],\n"
        + "       [Store].[USA].[CA].[San Francisco],\n"
        + "       [Store].[USA].[CA].[San Diego],\n"
        + "       [Store].[USA].[CA].[Beverly Hills],\n"
        + "       [Store].[USA].[CA].[Los Angeles],\n"
        + "       [Store].[USA].[WA].[Walla Walla],\n"
        + "       [Store].[USA].[WA].[Bellingham],\n"
        + "       [Store].[USA].[WA].[Yakima],\n"
        + "       [Store].[USA].[WA].[Spokane],\n"
        + "       [Store].[USA].[WA].[Seattle], \n"
        + "       [Store].[USA].[WA].[Bremerton],\n"
        + "       [Store].[USA].[WA].[Tacoma]},\n"
        + "     [Time].[Year].Members), \n"
        + "  [Measures].[Average Unit Sales], ASC) ON rows\n"
        + "from [Sales] ",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Average Unit Sales]}\n"
        + "{[Measures].[Max Unit Sales]}\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Store].[USA].[OR].[Portland], [Time].[1997]}\n"
        + "{[Store].[USA].[OR].[Salem], [Time].[1997]}\n"
        + "{[Store].[USA].[OR].[Salem].[Store 13], [Time].[1997]}\n"
        + "{[Store].[USA].[CA].[San Francisco], [Time].[1997]}\n"
        + "{[Store].[USA].[CA].[Beverly Hills], [Time].[1997]}\n"
        + "{[Store].[USA].[CA].[San Diego], [Time].[1997]}\n"
        + "{[Store].[USA].[CA].[Los Angeles], [Time].[1997]}\n"
        + "{[Store].[USA].[WA].[Walla Walla], [Time].[1997]}\n"
        + "{[Store].[USA].[WA].[Bellingham], [Time].[1997]}\n"
        + "{[Store].[USA].[WA].[Yakima], [Time].[1997]}\n"
        + "{[Store].[USA].[WA].[Spokane], [Time].[1997]}\n"
        + "{[Store].[USA].[WA].[Bremerton], [Time].[1997]}\n"
        + "{[Store].[USA].[WA].[Seattle], [Time].[1997]}\n"
        + "{[Store].[USA].[WA].[Tacoma], [Time].[1997]}\n"
        + "Row #0: 2,173\n"
        + "Row #0: 2,933\n"
        + "Row #0: 26,079\n"
        + "Row #1: 3,465\n"
        + "Row #1: 5,891\n"
        + "Row #1: 41,580\n"
        + "Row #2: 3,465\n"
        + "Row #2: 5,891\n"
        + "Row #2: 41,580\n"
        + "Row #3: 176\n"
        + "Row #3: 222\n"
        + "Row #3: 2,117\n"
        + "Row #4: 1,778\n"
        + "Row #4: 2,545\n"
        + "Row #4: 21,333\n"
        + "Row #5: 2,136\n"
        + "Row #5: 2,686\n"
        + "Row #5: 25,635\n"
        + "Row #6: 2,139\n"
        + "Row #6: 2,669\n"
        + "Row #6: 25,663\n"
        + "Row #7: 184\n"
        + "Row #7: 301\n"
        + "Row #7: 2,203\n"
        + "Row #8: 186\n"
        + "Row #8: 275\n"
        + "Row #8: 2,237\n"
        + "Row #9: 958\n"
        + "Row #9: 1,163\n"
        + "Row #9: 11,491\n"
        + "Row #10: 1,966\n"
        + "Row #10: 2,634\n"
        + "Row #10: 23,591\n"
        + "Row #11: 2,048\n"
        + "Row #11: 2,623\n"
        + "Row #11: 24,576\n"
        + "Row #12: 2,084\n"
        + "Row #12: 2,304\n"
        + "Row #12: 25,011\n"
        + "Row #13: 2,938\n"
        + "Row #13: 3,818\n"
        + "Row #13: 35,257\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderEmpty(Context context) {
    assertQueryReturns(context.getConnection(),
      "select \n"
        + "  Order("
        + "    {},"
        + "    [Customers].currentMember, BDESC) \n"
        + "on 0 from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderOne(Context context) {
    assertQueryReturns(context.getConnection(),
      "select \n"
        + "  Order("
        + "    {[Customers].[All Customers].[USA].[CA].[Woodland Hills].[Abel Young]},"
        + "    [Customers].currentMember, BDESC) \n"
        + "on 0 from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[USA].[CA].[Woodland Hills].[Abel Young]}\n"
        + "Row #0: 75\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderKeyEmpty(Context context) {
    assertQueryReturns(context.getConnection(),
      "select \n"
        + "  Order("
        + "    {},"
        + "    [Customers].currentMember.OrderKey, BDESC) \n"
        + "on 0 from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderKeyOne(Context context) {
    assertQueryReturns(context.getConnection(),
      "select \n"
        + "  Order("
        + "    {[Customers].[All Customers].[USA].[CA].[Woodland Hills].[Abel Young]},"
        + "    [Customers].currentMember.OrderKey, BDESC) \n"
        + "on 0 from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[USA].[CA].[Woodland Hills].[Abel Young]}\n"
        + "Row #0: 75\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderDesc(Context context) {
    // based on olap4j's OlapTest.testSortDimension
    assertQueryReturns(context.getConnection(),
      "SELECT\n"
        + "{[Measures].[Store Sales]} ON COLUMNS,\n"
        + "{Order(\n"
        + "  {{[Product].[Drink], [Product].[Drink].Children}},\n"
        + "  [Product].CurrentMember.Name,\n"
        + "  DESC)} ON ROWS\n"
        + "FROM [Sales]\n"
        + "WHERE {[Time].[1997].[Q3].[7]}",
      "Axis #0:\n"
        + "{[Time].[1997].[Q3].[7]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Store Sales]}\n"
        + "Axis #2:\n"
        + "{[Product].[Drink]}\n"
        + "{[Product].[Drink].[Dairy]}\n"
        + "{[Product].[Drink].[Beverages]}\n"
        + "{[Product].[Drink].[Alcoholic Beverages]}\n"
        + "Row #0: 4,409.58\n"
        + "Row #1: 629.69\n"
        + "Row #2: 2,477.02\n"
        + "Row #3: 1,302.87\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderMemberMemberValueExpNew(Context context) {

      SystemWideProperties.instance().CompareSiblingsByOrderKey = true;
    // Use a fresh connection to make sure bad member ordinals haven't
    // been assigned by previous tests.
    //final Context context = getTestContext().withFreshConnection();
    Connection connection = context.getConnection();
    try {
      assertQueryReturns(context.getConnection(),
        "select \n"
          + "  Order("
          + "    {[Customers].[All Customers].[USA].[CA].[Woodland Hills].[Abel Young],"
          + "     [Customers].[All Customers].[USA].[CA].[Santa Monica].[Adeline Chun]},"
          + "    [Customers].currentMember.OrderKey, BDESC) \n"
          + "on 0 from [Sales]",
        "Axis #0:\n"
          + "{}\n"
          + "Axis #1:\n"
          + "{[Customers].[USA].[CA].[Woodland Hills].[Abel Young]}\n"
          + "{[Customers].[USA].[CA].[Santa Monica].[Adeline Chun]}\n"
          + "Row #0: 75\n"
          + "Row #0: 33\n" );
    } finally {
      if ( connection != null ) {
        connection.close();
      }
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderMemberMemberValueExpNew1(Context context) {
    // sort by default measure

    SystemWideProperties.instance().CompareSiblingsByOrderKey = true;
    // Use a fresh connection to make sure bad member ordinals haven't
    // been assigned by previous tests.
    Connection connection = context.getConnection();
    try {
      assertQueryReturns(connection,
        "select \n"
          + "  Order("
          + "    {[Customers].[All Customers].[USA].[CA].[Woodland Hills].[Abel Young],"
          + "     [Customers].[All Customers].[USA].[CA].[Santa Monica].[Adeline Chun]},"
          + "    [Customers].currentMember, BDESC) \n"
          + "on 0 from [Sales]",
        "Axis #0:\n"
          + "{}\n"
          + "Axis #1:\n"
          + "{[Customers].[USA].[CA].[Woodland Hills].[Abel Young]}\n"
          + "{[Customers].[USA].[CA].[Santa Monica].[Adeline Chun]}\n"
          + "Row #0: 75\n"
          + "Row #0: 33\n" );
    } finally {
      connection.close();
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderMemberDefaultFlag1(Context context) {
    // flags not specified default to ASC - sort by default measure
    assertQueryReturns(context.getConnection(),
      "with \n"
        + "  Member [Measures].[Zero] as '0' \n"
        + "select \n"
        + "  Order("
        + "    {[Customers].[All Customers].[USA].[CA].[Woodland Hills].[Abel Young],"
        + "     [Customers].[All Customers].[USA].[CA].[Santa Monica].[Adeline Chun]},"
        + "    [Customers].currentMember.OrderKey) \n"
        + "on 0 from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[USA].[CA].[Santa Monica].[Adeline Chun]}\n"
        + "{[Customers].[USA].[CA].[Woodland Hills].[Abel Young]}\n"
        + "Row #0: 33\n"
        + "Row #0: 75\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderMemberDefaultFlag2(Context context) {
    // flags not specified default to ASC
    assertQueryReturns(context.getConnection(),
      "with \n"
        + "  Member [Measures].[Zero] as '0' \n"
        + "select \n"
        + "  Order("
        + "    {[Customers].[All Customers].[USA].[CA].[Woodland Hills].[Abel Young],"
        + "     [Customers].[All Customers].[USA].[CA].[Santa Monica].[Adeline Chun]},"
        + "    [Measures].[Store Cost]) \n"
        + "on 0 from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[USA].[CA].[Woodland Hills].[Abel Young]}\n"
        + "{[Customers].[USA].[CA].[Santa Monica].[Adeline Chun]}\n"
        + "Row #0: 75\n"
        + "Row #0: 33\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderMemberMemberValueExpHierarchy(Context context) {
    // Santa Monica and Woodland Hills both don't have orderkey
    // members are sorted by the order of their keys
    assertQueryReturns(context.getConnection(),
      "select \n"
        + "  Order("
        + "    {[Customers].[All Customers].[USA].[CA].[Woodland Hills].[Abel Young],"
        + "     [Customers].[All Customers].[USA].[CA].[Santa Monica].[Adeline Chun]},"
        + "    [Customers].currentMember.OrderKey, DESC) \n"
        + "on 0 from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[USA].[CA].[Woodland Hills].[Abel Young]}\n"
        + "{[Customers].[USA].[CA].[Santa Monica].[Adeline Chun]}\n"
        + "Row #0: 75\n"
        + "Row #0: 33\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderMemberMultiKeysMemberValueExp1(Context context) {
    // sort by unit sales and then customer id (Adeline = 6442, Abe = 570)
    assertQueryReturns(context.getConnection(),
      "select \n"
        + "  Order("
        + "    {[Customers].[USA].[WA].[Issaquah].[Abe Tramel],"
        + "     [Customers].[All Customers].[USA].[CA].[Woodland Hills].[Abel Young],"
        + "     [Customers].[All Customers].[USA].[CA].[Santa Monica].[Adeline Chun]},"
        + "    [Measures].[Unit Sales], BDESC, [Customers].currentMember.OrderKey, BDESC) \n"
        + "on 0 from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[USA].[CA].[Woodland Hills].[Abel Young]}\n"
        + "{[Customers].[USA].[CA].[Santa Monica].[Adeline Chun]}\n"
        + "{[Customers].[USA].[WA].[Issaquah].[Abe Tramel]}\n"
        + "Row #0: 75\n"
        + "Row #0: 33\n"
        + "Row #0: 33\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderMemberMultiKeysMemberValueExp2(Context context) {

    SystemWideProperties.instance().CompareSiblingsByOrderKey = true;
    // Use a fresh connection to make sure bad member ordinals haven't
    // been assigned by previous tests.
    Connection connection = context.getConnection();
    try {
      assertQueryReturns(connection,
        "select \n"
          + "  Order("
          + "    {[Customers].[USA].[WA].[Issaquah].[Abe Tramel],"
          + "     [Customers].[All Customers].[USA].[CA].[Woodland Hills].[Abel Young],"
          + "     [Customers].[All Customers].[USA].[CA].[Santa Monica].[Adeline Chun]},"
          + "    [Customers].currentMember.Parent.Parent.OrderKey, BASC, [Customers].currentMember.OrderKey, BDESC) \n"
          + "on 0 from [Sales]",
        "Axis #0:\n"
          + "{}\n"
          + "Axis #1:\n"
          + "{[Customers].[USA].[CA].[Woodland Hills].[Abel Young]}\n"
          + "{[Customers].[USA].[CA].[Santa Monica].[Adeline Chun]}\n"
          + "{[Customers].[USA].[WA].[Issaquah].[Abe Tramel]}\n"
          + "Row #0: 75\n"
          + "Row #0: 33\n"
          + "Row #0: 33\n" );
    } finally {
      connection.close();
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderMemberMultiKeysMemberValueExpDepends(Context context) {
    // should preserve order of Abe and Adeline (note second key is [Time])
    assertQueryReturns(context.getConnection(),
      "select \n"
        + "  Order("
        + "    {[Customers].[USA].[WA].[Issaquah].[Abe Tramel],"
        + "     [Customers].[All Customers].[USA].[CA].[Woodland Hills].[Abel Young],"
        + "     [Customers].[All Customers].[USA].[CA].[Santa Monica].[Adeline Chun]},"
        + "    [Measures].[Unit Sales], BDESC, [Time].[Time].currentMember, BDESC) \n"
        + "on 0 from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[USA].[CA].[Woodland Hills].[Abel Young]}\n"
        + "{[Customers].[USA].[WA].[Issaquah].[Abe Tramel]}\n"
        + "{[Customers].[USA].[CA].[Santa Monica].[Adeline Chun]}\n"
        + "Row #0: 75\n"
        + "Row #0: 33\n"
        + "Row #0: 33\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderTupleSingleKeysNew(Context context) {

    SystemWideProperties.instance().CompareSiblingsByOrderKey = true;
    // Use a fresh connection to make sure bad member ordinals haven't
    // been assigned by previous tests.
    final Connection connection = context.getConnection();
    try {
      assertQueryReturns(connection,
        "with \n"
          + "  set [NECJ] as \n"
          + "    'NonEmptyCrossJoin( \n"
          + "    {[Customers].[USA].[WA].[Issaquah].[Abe Tramel],"
          + "     [Customers].[All Customers].[USA].[CA].[Woodland Hills].[Abel Young],"
          + "     [Customers].[All Customers].[USA].[CA].[Santa Monica].[Adeline Chun]},"
          + "    {[Store].[USA].[WA].[Seattle],\n"
          + "     [Store].[USA].[CA],\n"
          + "     [Store].[USA].[OR]})'\n"
          + "select \n"
          + " Order([NECJ], [Customers].currentMember.OrderKey, BDESC) \n"
          + "on 0 from [Sales]",
        "Axis #0:\n"
          + "{}\n"
          + "Axis #1:\n"
          + "{[Customers].[USA].[CA].[Woodland Hills].[Abel Young], [Store].[USA].[CA]}\n"
          + "{[Customers].[USA].[CA].[Santa Monica].[Adeline Chun], [Store].[USA].[CA]}\n"
          + "{[Customers].[USA].[WA].[Issaquah].[Abe Tramel], [Store].[USA].[WA].[Seattle]}\n"
          + "Row #0: 75\n"
          + "Row #0: 33\n"
          + "Row #0: 33\n" );
    } finally {
      connection.close();
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderTupleSingleKeysNew1(Context context) {

    SystemWideProperties.instance().CompareSiblingsByOrderKey = true;
    // Use a fresh connection to make sure bad member ordinals haven't
    // been assigned by previous tests.
    Connection connection = context.getConnection();
    try {
      assertQueryReturns(connection,
        "with \n"
          + "  set [NECJ] as \n"
          + "    'NonEmptyCrossJoin( \n"
          + "    {[Customers].[USA].[WA].[Issaquah].[Abe Tramel],"
          + "     [Customers].[All Customers].[USA].[CA].[Woodland Hills].[Abel Young],"
          + "     [Customers].[All Customers].[USA].[CA].[Santa Monica].[Adeline Chun]},"
          + "    {[Store].[USA].[WA].[Seattle],\n"
          + "     [Store].[USA].[CA],\n"
          + "     [Store].[USA].[OR]})'\n"
          + "select \n"
          + " Order([NECJ], [Store].currentMember.OrderKey, DESC) \n"
          + "on 0 from [Sales]",
        "Axis #0:\n"
          + "{}\n"
          + "Axis #1:\n"
          + "{[Customers].[USA].[WA].[Issaquah].[Abe Tramel], [Store].[USA].[WA].[Seattle]}\n"
          + "{[Customers].[USA].[CA].[Woodland Hills].[Abel Young], [Store].[USA].[CA]}\n"
          + "{[Customers].[USA].[CA].[Santa Monica].[Adeline Chun], [Store].[USA].[CA]}\n"
          + "Row #0: 33\n"
          + "Row #0: 75\n"
          + "Row #0: 33\n" );
    } finally {
      connection.close();
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderTupleMultiKeys1(Context context) {
    assertQueryReturns(context.getConnection(),
      "with \n"
        + "  set [NECJ] as \n"
        + "    'NonEmptyCrossJoin( \n"
        + "    {[Store].[USA].[CA],\n"
        + "     [Store].[USA].[WA]},\n"
        + "    {[Customers].[USA].[WA].[Issaquah].[Abe Tramel],"
        + "     [Customers].[All Customers].[USA].[CA].[Woodland Hills].[Abel Young],"
        + "     [Customers].[All Customers].[USA].[CA].[Santa Monica].[Adeline Chun]})' \n"
        + "select \n"
        + " Order([NECJ], [Store].currentMember.OrderKey, BDESC, [Measures].[Unit Sales], BDESC) \n"
        + "on 0 from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA], [Customers].[USA].[WA].[Issaquah].[Abe Tramel]}\n"
        + "{[Store].[USA].[CA], [Customers].[USA].[CA].[Woodland Hills].[Abel Young]}\n"
        + "{[Store].[USA].[CA], [Customers].[USA].[CA].[Santa Monica].[Adeline Chun]}\n"
        + "Row #0: 33\n"
        + "Row #0: 75\n"
        + "Row #0: 33\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderTupleMultiKeys2(Context context) {
    assertQueryReturns(context.getConnection(),
      "with \n"
        + "  set [NECJ] as \n"
        + "    'NonEmptyCrossJoin( \n"
        + "    {[Store].[USA].[CA],\n"
        + "     [Store].[USA].[WA]},\n"
        + "    {[Customers].[USA].[WA].[Issaquah].[Abe Tramel],"
        + "     [Customers].[All Customers].[USA].[CA].[Woodland Hills].[Abel Young],"
        + "     [Customers].[All Customers].[USA].[CA].[Santa Monica].[Adeline Chun]})' \n"
        + "select \n"
        + " Order([NECJ], [Measures].[Unit Sales], BDESC, Ancestor([Customers].currentMember, [Customers].[Name])"
        + ".OrderKey, BDESC) \n"
        + "on 0 from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[CA], [Customers].[USA].[CA].[Woodland Hills].[Abel Young]}\n"
        + "{[Store].[USA].[CA], [Customers].[USA].[CA].[Santa Monica].[Adeline Chun]}\n"
        + "{[Store].[USA].[WA], [Customers].[USA].[WA].[Issaquah].[Abe Tramel]}\n"
        + "Row #0: 75\n"
        + "Row #0: 33\n"
        + "Row #0: 33\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderTupleMultiKeys3(Context context) {
    // WA unit sales is greater than CA unit sales
    // Santa Monica unit sales (2660) is greater that Woodland hills (2516)
    assertQueryReturns(context.getConnection(),
      "with \n"
        + "  set [NECJ] as \n"
        + "    'NonEmptyCrossJoin( \n"
        + "    {[Store].[USA].[CA],\n"
        + "     [Store].[USA].[WA]},\n"
        + "    {[Customers].[USA].[WA].[Issaquah].[Abe Tramel],"
        + "     [Customers].[All Customers].[USA].[CA].[Woodland Hills].[Abel Young],"
        + "     [Customers].[All Customers].[USA].[CA].[Santa Monica].[Adeline Chun]})' \n"
        + "select \n"
        + " Order([NECJ], [Measures].[Unit Sales], DESC, Ancestor([Customers].currentMember, [Customers].[Name]), "
        + "BDESC) \n"
        + "on 0 from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA], [Customers].[USA].[WA].[Issaquah].[Abe Tramel]}\n"
        + "{[Store].[USA].[CA], [Customers].[USA].[CA].[Santa Monica].[Adeline Chun]}\n"
        + "{[Store].[USA].[CA], [Customers].[USA].[CA].[Woodland Hills].[Abel Young]}\n"
        + "Row #0: 33\n"
        + "Row #0: 33\n"
        + "Row #0: 75\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderTupleMultiKeyswithVCube(Context context) {
    // WA unit sales is greater than CA unit sales

     SystemWideProperties.instance().CompareSiblingsByOrderKey = true;

    // Use a fresh connection to make sure bad member ordinals haven't
    // been assigned by previous tests.
    // a non-sense cube just to test ordering by order key

      RolapSchemaPool.instance().clear();
      class TestOrderTupleMultiKeyswithVCubeModifier extends PojoMappingModifier {

          public TestOrderTupleMultiKeyswithVCubeModifier(CatalogMapping catalog) {
              super(catalog);
          }
          protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
              List<CubeMapping> result = new ArrayList<>();
              result.addAll(super.cubes(cubes));
              result.add(VirtualCubeMappingImpl.builder()
                  .withName("Sales vs HR")
                  .withDimensionConnectors(List.of(
                      DimensionConnectorMappingImpl.builder()
                      	  .withPhysicalCube((PhysicalCubeMappingImpl) look(FoodmartMappingSupplier.CUBE_SALES))	
                      	  .withOverrideDimensionName("Customers")
                          .build(),
                      DimensionConnectorMappingImpl.builder()
                      	  .withPhysicalCube((PhysicalCubeMappingImpl) look(FoodmartMappingSupplier.CUBE_HR))
                      	  .withOverrideDimensionName("Position")
                          .build()
                  ))
                  .withReferencedMeasures(List.of(look(FoodmartMappingSupplier.MEASURE_ORG_SALARY)))
                  .build());
              return result;        	  
          }      
      }
    /*
    String baseSchema = TestUtil.getRawSchema(context);
    String schema = SchemaUtil.getSchema(baseSchema,
      null,
      null,
      "<VirtualCube name=\"Sales vs HR\">\n"
        + "<VirtualCubeDimension cubeName=\"Sales\" name=\"Customers\"/>\n"
        + "<VirtualCubeDimension cubeName=\"HR\" name=\"Position\"/>\n"
        + "<VirtualCubeMeasure cubeName=\"HR\" name=\"[Measures].[Org Salary]\"/>\n"
        + "</VirtualCube>",
      null, null, null );
    TestUtil.withSchema(context, schema);
     */
    withSchema(context, TestOrderTupleMultiKeyswithVCubeModifier::new);
    assertQueryReturns(context.getConnection(),
      "with \n"
        + "  set [CJ] as \n"
        + "    'CrossJoin( \n"
        + "    {[Position].[Store Management].children},\n"
        + "    {[Customers].[USA].[WA].[Issaquah].[Abe Tramel],"
        + "     [Customers].[All Customers].[USA].[CA].[Woodland Hills].[Abel Young],"
        + "     [Customers].[All Customers].[USA].[CA].[Santa Monica].[Adeline Chun]})' \n"
        + "select \n"
        + "  [Measures].[Org Salary] on columns, \n"
        + "  Order([CJ], [Position].currentMember.OrderKey, BASC, Ancestor([Customers].currentMember, [Customers]"
        + ".[Name]).OrderKey, BDESC) \n"
        + "on rows \n"
        + "from [Sales vs HR]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Org Salary]}\n"
        + "Axis #2:\n"
        + "{[Position].[Store Management].[Store Manager], [Customers].[USA].[CA].[Santa Monica].[Adeline Chun]}\n"
        + "{[Position].[Store Management].[Store Manager], [Customers].[USA].[CA].[Woodland Hills].[Abel Young]}\n"
        + "{[Position].[Store Management].[Store Manager], [Customers].[USA].[WA].[Issaquah].[Abe Tramel]}\n"
        + "{[Position].[Store Management].[Store Assistant Manager], [Customers].[USA].[CA].[Santa Monica].[Adeline "
        + "Chun]}\n"
        + "{[Position].[Store Management].[Store Assistant Manager], [Customers].[USA].[CA].[Woodland Hills].[Abel "
        + "Young]}\n"
        + "{[Position].[Store Management].[Store Assistant Manager], [Customers].[USA].[WA].[Issaquah].[Abe Tramel]}\n"
        + "{[Position].[Store Management].[Store Shift Supervisor], [Customers].[USA].[CA].[Santa Monica].[Adeline "
        + "Chun]}\n"
        + "{[Position].[Store Management].[Store Shift Supervisor], [Customers].[USA].[CA].[Woodland Hills].[Abel "
        + "Young]}\n"
        + "{[Position].[Store Management].[Store Shift Supervisor], [Customers].[USA].[WA].[Issaquah].[Abe Tramel]}\n"
        + "Row #0: \n"
        + "Row #1: \n"
        + "Row #2: \n"
        + "Row #3: \n"
        + "Row #4: \n"
        + "Row #5: \n"
        + "Row #6: \n"
        + "Row #7: \n"
        + "Row #8: \n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderConstant1(Context context) {
    // sort by customerId (Abel = 7851, Adeline = 6442, Abe = 570)
    assertQueryReturns(context.getConnection(),
      "select \n"
        + "  Order("
        + "    {[Customers].[USA].[WA].[Issaquah].[Abe Tramel],"
        + "     [Customers].[All Customers].[USA].[CA].[Woodland Hills].[Abel Young],"
        + "     [Customers].[All Customers].[USA].[CA].[Santa Monica].[Adeline Chun]},"
        + "    [Customers].[USA].OrderKey, BDESC, [Customers].currentMember.OrderKey, BASC) \n"
        + "on 0 from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[USA].[WA].[Issaquah].[Abe Tramel]}\n"
        + "{[Customers].[USA].[CA].[Santa Monica].[Adeline Chun]}\n"
        + "{[Customers].[USA].[CA].[Woodland Hills].[Abel Young]}\n"
        + "Row #0: 33\n"
        + "Row #0: 33\n"
        + "Row #0: 75\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testOrderDiffrentDim(Context context) {
    assertQueryReturns(context.getConnection(),
      "select \n"
        + "  Order("
        + "    {[Customers].[USA].[WA].[Issaquah].[Abe Tramel],"
        + "     [Customers].[All Customers].[USA].[CA].[Woodland Hills].[Abel Young],"
        + "     [Customers].[All Customers].[USA].[CA].[Santa Monica].[Adeline Chun]},"
        + "    [Product].currentMember.OrderKey, BDESC, [Gender].currentMember.OrderKey, BDESC) \n"
        + "on 0 from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[USA].[WA].[Issaquah].[Abe Tramel]}\n"
        + "{[Customers].[USA].[CA].[Woodland Hills].[Abel Young]}\n"
        + "{[Customers].[USA].[CA].[Santa Monica].[Adeline Chun]}\n"
        + "Row #0: 33\n"
        + "Row #0: 75\n"
        + "Row #0: 33\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testUnorder(Context context) {
    assertAxisReturns(context.getConnection(),
      "Unorder([Gender].members)",
      "[Gender].[All Gender]\n"
        + "[Gender].[F]\n"
        + "[Gender].[M]" );
    assertAxisReturns(context.getConnection(),
      "Unorder(Order([Gender].members, -[Measures].[Unit Sales]))",
      "[Gender].[All Gender]\n"
        + "[Gender].[M]\n"
        + "[Gender].[F]" );
    assertAxisReturns(context.getConnection(),
      "Unorder(Crossjoin([Gender].members, [Marital Status].Children))",
      "{[Gender].[All Gender], [Marital Status].[M]}\n"
        + "{[Gender].[All Gender], [Marital Status].[S]}\n"
        + "{[Gender].[F], [Marital Status].[M]}\n"
        + "{[Gender].[F], [Marital Status].[S]}\n"
        + "{[Gender].[M], [Marital Status].[M]}\n"
        + "{[Gender].[M], [Marital Status].[S]}" );

    // implicitly convert member to set
    assertAxisReturns(context.getConnection(),
      "Unorder([Gender].[M])",
      "[Gender].[M]" );

    assertAxisThrows(context.getConnection(),
      "Unorder(1 + 3)",
      "No function matches signature 'Unorder(<Numeric Expression>)'" );
    assertAxisThrows(context.getConnection(),
      "Unorder([Gender].[M], 1 + 3)",
      "No function matches signature 'Unorder(<Member>, <Numeric Expression>)'" );
    assertQueryReturns(context.getConnection(),
      "select {[Measures].[Store Sales], [Measures].[Unit Sales]} on 0,\n"
        + "  Unorder([Gender].Members) on 1\n"
        + "from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Store Sales]}\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Gender].[All Gender]}\n"
        + "{[Gender].[F]}\n"
        + "{[Gender].[M]}\n"
        + "Row #0: 565,238.13\n"
        + "Row #0: 266,773\n"
        + "Row #1: 280,226.21\n"
        + "Row #1: 131,558\n"
        + "Row #2: 285,011.92\n"
        + "Row #2: 135,215\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSiblingsA(Context context) {
    assertAxisReturns(context.getConnection(),
      "{[Time].[1997].Siblings}",
      "[Time].[1997]\n"
        + "[Time].[1998]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSiblingsB(Context context) {
    assertAxisReturns(context.getConnection(),
      "{[Store].Siblings}",
      "[Store].[All Stores]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSiblingsC(Context context) {
    assertAxisReturns(context.getConnection(),
      "{[Store].[USA].[CA].Siblings}",
      "[Store].[USA].[CA]\n"
        + "[Store].[USA].[OR]\n"
        + "[Store].[USA].[WA]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSiblingsD(Context context) {
    // The null member has no siblings -- not even itself
    assertAxisReturns(context.getConnection(), "{[Gender].Parent.Siblings}", "" );

    assertExprReturns(context.getConnection(),
      "count ([Gender].parent.siblings, includeempty)", "0" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSubset(Context context) {
    assertAxisReturns(context.getConnection(),
      "Subset([Promotion Media].Children, 7, 2)",
      "[Promotion Media].[Product Attachment]\n"
        + "[Promotion Media].[Radio]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSubsetNegativeCount(Context context) {
    assertAxisReturns(context.getConnection(),
      "Subset([Promotion Media].Children, 3, -1)",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSubsetNegativeStart(Context context) {
    assertAxisReturns(context.getConnection(),
      "Subset([Promotion Media].Children, -2, 4)",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSubsetDefault(Context context) {
    assertAxisReturns(context.getConnection(),
      "Subset([Promotion Media].Children, 11)",
      "[Promotion Media].[Sunday Paper, Radio]\n"
        + "[Promotion Media].[Sunday Paper, Radio, TV]\n"
        + "[Promotion Media].[TV]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSubsetOvershoot(Context context) {
    assertAxisReturns(context.getConnection(),
      "Subset([Promotion Media].Children, 15)",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSubsetEmpty(Context context) {
    assertAxisReturns(context.getConnection(),
      "Subset([Gender].[F].Children, 1)",
      "" );

    assertAxisReturns(context.getConnection(),
      "Subset([Gender].[F].Children, 1, 3)",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTail(Context context) {
    assertAxisReturns(context.getConnection(),
      "Tail([Store].Children, 2)",
      "[Store].[Mexico]\n"
        + "[Store].[USA]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTailNegative(Context context) {
    assertAxisReturns(context.getConnection(),
      "Tail([Store].Children, 2 - 3)",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTailDefault(Context context) {
    assertAxisReturns(context.getConnection(),
      "Tail([Store].Children)",
      "[Store].[USA]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTailOvershoot(Context context) {
    assertAxisReturns(context.getConnection(),
      "Tail([Store].Children, 2 + 2)",
      "[Store].[Canada]\n"
        + "[Store].[Mexico]\n"
        + "[Store].[USA]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTailEmpty(Context context) {
    assertAxisReturns(context.getConnection(),
      "Tail([Gender].[F].Children, 2)",
      "" );

    assertAxisReturns(context.getConnection(),
      "Tail([Gender].[F].Children)",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testToggleDrillState(Context context) {
    assertAxisReturns(context.getConnection(),
      "ToggleDrillState({[Customers].[USA],[Customers].[Canada]},"
        + "{[Customers].[USA],[Customers].[USA].[CA]})",
      "[Customers].[USA]\n"
        + "[Customers].[USA].[CA]\n"
        + "[Customers].[USA].[OR]\n"
        + "[Customers].[USA].[WA]\n"
        + "[Customers].[Canada]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testToggleDrillState2(Context context) {
    assertAxisReturns(context.getConnection(),
      "ToggleDrillState([Product].[Product Department].members, "
        + "{[Product].[All Products].[Food].[Snack Foods]})",
      "[Product].[Drink].[Alcoholic Beverages]\n"
        + "[Product].[Drink].[Beverages]\n"
        + "[Product].[Drink].[Dairy]\n"
        + "[Product].[Food].[Baked Goods]\n"
        + "[Product].[Food].[Baking Goods]\n"
        + "[Product].[Food].[Breakfast Foods]\n"
        + "[Product].[Food].[Canned Foods]\n"
        + "[Product].[Food].[Canned Products]\n"
        + "[Product].[Food].[Dairy]\n"
        + "[Product].[Food].[Deli]\n"
        + "[Product].[Food].[Eggs]\n"
        + "[Product].[Food].[Frozen Foods]\n"
        + "[Product].[Food].[Meat]\n"
        + "[Product].[Food].[Produce]\n"
        + "[Product].[Food].[Seafood]\n"
        + "[Product].[Food].[Snack Foods]\n"
        + "[Product].[Food].[Snack Foods].[Snack Foods]\n"
        + "[Product].[Food].[Snacks]\n"
        + "[Product].[Food].[Starchy Foods]\n"
        + "[Product].[Non-Consumable].[Carousel]\n"
        + "[Product].[Non-Consumable].[Checkout]\n"
        + "[Product].[Non-Consumable].[Health and Hygiene]\n"
        + "[Product].[Non-Consumable].[Household]\n"
        + "[Product].[Non-Consumable].[Periodicals]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testToggleDrillState3(Context context) {
    assertAxisReturns(context.getConnection(),
      "ToggleDrillState("
        + "{[Time].[1997].[Q1],"
        + " [Time].[1997].[Q2],"
        + " [Time].[1997].[Q2].[4],"
        + " [Time].[1997].[Q2].[6],"
        + " [Time].[1997].[Q3]},"
        + "{[Time].[1997].[Q2]})",
      "[Time].[1997].[Q1]\n"
        + "[Time].[1997].[Q2]\n"
        + "[Time].[1997].[Q3]" );
  }

  // bug 634860
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testToggleDrillStateTuple(Context context) {
    assertAxisReturns(context.getConnection(),
      "ToggleDrillState(\n"
        + "{([Store].[USA].[CA],"
        + "  [Product].[All Products].[Drink].[Alcoholic Beverages]),\n"
        + " ([Store].[USA],"
        + "  [Product].[All Products].[Drink])},\n"
        + "{[Store].[All stores].[USA].[CA]})",
      "{[Store].[USA].[CA], [Product].[Drink].[Alcoholic Beverages]}\n"
        + "{[Store].[USA].[CA].[Alameda], [Product].[Drink].[Alcoholic Beverages]}\n"
        + "{[Store].[USA].[CA].[Beverly Hills], [Product].[Drink].[Alcoholic Beverages]}\n"
        + "{[Store].[USA].[CA].[Los Angeles], [Product].[Drink].[Alcoholic Beverages]}\n"
        + "{[Store].[USA].[CA].[San Diego], [Product].[Drink].[Alcoholic Beverages]}\n"
        + "{[Store].[USA].[CA].[San Francisco], [Product].[Drink].[Alcoholic Beverages]}\n"
        + "{[Store].[USA], [Product].[Drink]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testToggleDrillStateRecursive(Context context) {
    // We expect this to fail.
    assertQueryThrows(context,
      "Select \n"
        + "    ToggleDrillState(\n"
        + "        {[Store].[USA]}, \n"
        + "        {[Store].[USA]}, recursive) on Axis(0) \n"
        + "from [Sales]\n",
      "'RECURSIVE' is not supported in ToggleDrillState." );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTopCount(Context context) {
    assertAxisReturns(context.getConnection(),
      "TopCount({[Promotion Media].[Media Type].members}, 2, [Measures].[Unit Sales])",
      "[Promotion Media].[No Media]\n"
        + "[Promotion Media].[Daily Paper, Radio, TV]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTopCountUnordered(Context context) {
    assertAxisReturns(context.getConnection(),
      "TopCount({[Promotion Media].[Media Type].members}, 2)",
      "[Promotion Media].[Bulk Mail]\n"
        + "[Promotion Media].[Cash Register Handout]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTopCountTuple(Context context) {
    assertAxisReturns(context.getConnection(),
      "TopCount([Customers].[Name].members,2,(Time.[1997].[Q1],[Measures].[Store Sales]))",
      "[Customers].[USA].[WA].[Spokane].[Grace McLaughlin]\n"
        + "[Customers].[USA].[WA].[Spokane].[Matt Bellah]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTopCountEmpty(Context context) {
    assertAxisReturns(context.getConnection(),
      "TopCount(Filter({[Promotion Media].[Media Type].members}, 1=0), 2, [Measures].[Unit Sales])",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTopCountDepends(Context context) {
    Connection connection = context.getConnection();
    checkTopBottomCountPercentDepends(connection, "TopCount" );
    checkTopBottomCountPercentDepends(connection, "TopPercent" );
    checkTopBottomCountPercentDepends(connection, "TopSum" );
    checkTopBottomCountPercentDepends(connection, "BottomCount" );
    checkTopBottomCountPercentDepends(connection, "BottomPercent" );
    checkTopBottomCountPercentDepends(connection, "BottomSum" );
  }

  private void checkTopBottomCountPercentDepends(Connection connection, String fun) {
    String s1 =
      allHiersExcept( "[Measures]", "[Promotion Media]" );
    assertSetExprDependsOn(connection,
      fun
        + "({[Promotion Media].[Media Type].members}, "
        + "2, [Measures].[Unit Sales])",
      s1 );

    if ( fun.endsWith( "Count" ) ) {
      assertSetExprDependsOn(connection,
        fun + "({[Promotion Media].[Media Type].members}, 2)",
        "{}" );
    }
  }

  /**
   * Tests TopCount applied to a large result set.
   *
   * <p>Before optimizing (see FunUtil.partialSort), on a 2-core 32-bit 2.4GHz
   * machine, the 1st query took 14.5 secs, the 2nd query took 5.0 secs. After optimizing, who knows?
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTopCountHuge(Context context) {
    // TODO convert printfs to trace
    final String query =
      "SELECT [Measures].[Store Sales] ON 0,\n"
        + "TopCount([Time].[Month].members * "
        + "[Customers].[Name].members, 3, [Measures].[Store Sales]) ON 1\n"
        + "FROM [Sales]";
    final String desiredResult =
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Store Sales]}\n"
        + "Axis #2:\n"
        + "{[Time].[1997].[Q1].[3], [Customers].[USA].[WA].[Spokane].[George Todero]}\n"
        + "{[Time].[1997].[Q3].[7], [Customers].[USA].[WA].[Spokane].[James Horvat]}\n"
        + "{[Time].[1997].[Q4].[11], [Customers].[USA].[WA].[Olympia].[Charles Stanley]}\n"
        + "Row #0: 234.83\n"
        + "Row #1: 199.46\n"
        + "Row #2: 191.90\n";
    long now = System.currentTimeMillis();
    assertQueryReturns(context.getConnection(), query, desiredResult );
    LOGGER.info( "first query took " + ( System.currentTimeMillis() - now ) );
    now = System.currentTimeMillis();
    assertQueryReturns(context.getConnection(), query, desiredResult );
    LOGGER.info( "second query took " + ( System.currentTimeMillis() - now ) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTopPercent(Context context) {
    assertAxisReturns(context.getConnection(),
      "TopPercent({[Promotion Media].[Media Type].members}, 70, [Measures].[Unit Sales])",
      "[Promotion Media].[No Media]" );
  }

  // todo: test precision

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTopSum(Context context) {
    assertAxisReturns(context.getConnection(),
      "TopSum({[Promotion Media].[Media Type].members}, 200000, [Measures].[Unit Sales])",
      "[Promotion Media].[No Media]\n"
        + "[Promotion Media].[Daily Paper, Radio, TV]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTopSumEmpty(Context context) {
    assertAxisReturns(context.getConnection(),
      "TopSum(Filter({[Promotion Media].[Media Type].members}, 1=0), "
        + "200000, [Measures].[Unit Sales])",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testUnionAll(Context context) {
    assertAxisReturns(context.getConnection(),
      "Union({[Gender].[M]}, {[Gender].[F]}, ALL)",
      "[Gender].[M]\n"
        + "[Gender].[F]" ); // order is preserved
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testUnionAllTuple(Context context) {
    // With the bug, the last 8 rows are repeated.
    assertQueryReturns(context.getConnection(),
      "with \n"
        + "set [Set1] as 'Crossjoin({[Time].[1997].[Q1]:[Time].[1997].[Q4]},{[Store].[USA].[CA]:[Store].[USA].[OR]})'\n"
        + "set [Set2] as 'Crossjoin({[Time].[1997].[Q2]:[Time].[1997].[Q3]},{[Store].[Mexico].[DF]:[Store].[Mexico]"
        + ".[Veracruz]})'\n"
        + "select \n"
        + "{[Measures].[Unit Sales]} ON COLUMNS,\n"
        + "Union([Set1], [Set2], ALL) ON ROWS\n"
        + "from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Time].[1997].[Q1], [Store].[USA].[CA]}\n"
        + "{[Time].[1997].[Q1], [Store].[USA].[OR]}\n"
        + "{[Time].[1997].[Q2], [Store].[USA].[CA]}\n"
        + "{[Time].[1997].[Q2], [Store].[USA].[OR]}\n"
        + "{[Time].[1997].[Q3], [Store].[USA].[CA]}\n"
        + "{[Time].[1997].[Q3], [Store].[USA].[OR]}\n"
        + "{[Time].[1997].[Q4], [Store].[USA].[CA]}\n"
        + "{[Time].[1997].[Q4], [Store].[USA].[OR]}\n"
        + "{[Time].[1997].[Q2], [Store].[Mexico].[DF]}\n"
        + "{[Time].[1997].[Q2], [Store].[Mexico].[Guerrero]}\n"
        + "{[Time].[1997].[Q2], [Store].[Mexico].[Jalisco]}\n"
        + "{[Time].[1997].[Q2], [Store].[Mexico].[Veracruz]}\n"
        + "{[Time].[1997].[Q3], [Store].[Mexico].[DF]}\n"
        + "{[Time].[1997].[Q3], [Store].[Mexico].[Guerrero]}\n"
        + "{[Time].[1997].[Q3], [Store].[Mexico].[Jalisco]}\n"
        + "{[Time].[1997].[Q3], [Store].[Mexico].[Veracruz]}\n"
        + "Row #0: 16,890\n"
        + "Row #1: 19,287\n"
        + "Row #2: 18,052\n"
        + "Row #3: 15,079\n"
        + "Row #4: 18,370\n"
        + "Row #5: 16,940\n"
        + "Row #6: 21,436\n"
        + "Row #7: 16,353\n"
        + "Row #8: \n"
        + "Row #9: \n"
        + "Row #10: \n"
        + "Row #11: \n"
        + "Row #12: \n"
        + "Row #13: \n"
        + "Row #14: \n"
        + "Row #15: \n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testUnion(Context context) {
    assertAxisReturns(context.getConnection(),
      "Union({[Store].[USA], [Store].[USA], [Store].[USA].[OR]}, "
        + "{[Store].[USA].[CA], [Store].[USA]})",
      "[Store].[USA]\n"
        + "[Store].[USA].[OR]\n"
        + "[Store].[USA].[CA]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testUnionEmptyBoth(Context context) {
    assertAxisReturns(context.getConnection(),
      "Union({}, {})",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testUnionEmptyRight(Context context) {
    assertAxisReturns(context.getConnection(),
      "Union({[Gender].[M]}, {})",
      "[Gender].[M]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testUnionTuple(Context context) {
    assertAxisReturns(context.getConnection(),
      "Union({"
        + " ([Gender].[M], [Marital Status].[S]),"
        + " ([Gender].[F], [Marital Status].[S])"
        + "}, {"
        + " ([Gender].[M], [Marital Status].[M]),"
        + " ([Gender].[M], [Marital Status].[S])"
        + "})",

      "{[Gender].[M], [Marital Status].[S]}\n"
        + "{[Gender].[F], [Marital Status].[S]}\n"
        + "{[Gender].[M], [Marital Status].[M]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testUnionTupleDistinct(Context context) {
    assertAxisReturns(context.getConnection(),
      "Union({"
        + " ([Gender].[M], [Marital Status].[S]),"
        + " ([Gender].[F], [Marital Status].[S])"
        + "}, {"
        + " ([Gender].[M], [Marital Status].[M]),"
        + " ([Gender].[M], [Marital Status].[S])"
        + "}, Distinct)",

      "{[Gender].[M], [Marital Status].[S]}\n"
        + "{[Gender].[F], [Marital Status].[S]}\n"
        + "{[Gender].[M], [Marital Status].[M]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testUnionQuery(Context context) {
    Result result = executeQuery(context.getConnection(),
      "select {[Measures].[Unit Sales], "
        + "[Measures].[Store Cost], "
        + "[Measures].[Store Sales]} on columns,\n"
        + " Hierarchize(\n"
        + "   Union(\n"
        + "     Crossjoin(\n"
        + "       Crossjoin([Gender].[All Gender].children,\n"
        + "                 [Marital Status].[All Marital Status].children),\n"
        + "       Crossjoin([Customers].[All Customers].children,\n"
        + "                 [Product].[All Products].children) ),\n"
        + "     Crossjoin({([Gender].[All Gender].[M], [Marital Status].[All Marital Status].[M])},\n"
        + "       Crossjoin(\n"
        + "         [Customers].[All Customers].[USA].children,\n"
        + "         [Product].[All Products].children) ) )) on rows\n"
        + "from Sales where ([Time].[1997])" );
    final Axis rowsAxis = result.getAxes()[ 1 ];
    assertEquals( 45, rowsAxis.getPositions().size() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testItemMember(Context context) {
    assertExprReturns(context.getConnection(),
      "Descendants([Time].[1997], [Time].[Month]).Item(1).Item(0).UniqueName",
      "[Time].[1997].[Q1].[2]" );

    // Access beyond the list yields the Null member.
    if ( isDefaultNullMemberRepresentation() ) {
      assertExprReturns(context.getConnection(),
        "[Time].[1997].Children.Item(6).UniqueName", "[Time].[#null]" );
      assertExprReturns(context.getConnection(),
        "[Time].[1997].Children.Item(-1).UniqueName", "[Time].[#null]" );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testItemTuple(Context context) {
    assertExprReturns(context.getConnection(),
      "CrossJoin([Gender].[All Gender].children, "
        + "[Time].[1997].[Q2].children).Item(0).Item(1).UniqueName",
      "[Time].[1997].[Q2].[4]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStrToMember(Context context) {
    assertExprReturns(context.getConnection(),
      "StrToMember(\"[Time].[1997].[Q2].[4]\").Name",
      "4" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStrToMemberUniqueName(Context context) {
    assertExprReturns(context.getConnection(),
      "StrToMember(\"[Store].[USA].[CA]\").Name",
      "CA" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStrToMemberFullyQualifiedName(Context context) {
    assertExprReturns(context.getConnection(),
      "StrToMember(\"[Store].[All Stores].[USA].[CA]\").Name",
      "CA" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStrToMemberNull(Context context) {
    // SSAS 2005 gives "#Error An MDX expression was expected. An empty
    // expression was specified."
    assertExprThrows(context.getConnection(),
      "StrToMember(null).Name",
      "An MDX expression was expected. An empty expression was specified" );
    assertExprThrows(context.getConnection(),
      "StrToSet(null, [Gender]).Count",
      "An MDX expression was expected. An empty expression was specified" );
    assertExprThrows(context.getConnection(),
      "StrToTuple(null, [Gender]).Name",
      "An MDX expression was expected. An empty expression was specified" );
  }

  /**
   * Testcase for
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-560">
   * bug MONDRIAN-560, "StrToMember function doesn't use IgnoreInvalidMembers option"</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStrToMemberIgnoreInvalidMembers(Context context) {
	RolapSchemaPool.instance().clear();
    ((TestConfig)context.getConfig()).setIgnoreInvalidMembersDuringQuery(true);

    // [Product].[Drugs] is invalid, becomes null member, and is dropped
    // from list
    assertQueryReturns(context.getConnection(),
      "select \n"
        + "  {[Product].[Food],\n"
        + "    StrToMember(\"[Product].[Drugs]\")} on columns,\n"
        + "  {[Measures].[Unit Sales]} on rows\n"
        + "from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Product].[Food]}\n"
        + "Axis #2:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Row #0: 191,940\n" );

    // Hierarchy is inferred from leading edge
    assertExprReturns(context.getConnection(),
      "StrToMember(\"[Marital Status].[Separated]\").Hierarchy.Name",
      "Marital Status" );

    // Null member is returned
    assertExprReturns(context.getConnection(),
      "StrToMember(\"[Marital Status].[Separated]\").Name",
      "#null" );

    // Use longest valid prefix, so get [Time].[Weekly] rather than just
    // [Time].
    final String timeWeekly = hierarchyName( "Time", "Weekly" );
    assertExprReturns(context.getConnection(),
      "StrToMember(\"" + timeWeekly
        + ".[1996].[Q1]\").Hierarchy.UniqueName",
      timeWeekly );

    // If hierarchy is invalid, throw an error even though
    // IgnoreInvalidMembersDuringQuery is set.
    assertExprThrows(context.getConnection(),
      "StrToMember(\"[Unknown Hierarchy].[Invalid].[Member]\").Name",
      "MDX object '[Unknown Hierarchy].[Invalid].[Member]' not found in cube 'Sales'" );
    assertExprThrows(context.getConnection(),
      "StrToMember(\"[Unknown Hierarchy].[Invalid]\").Name",
      "MDX object '[Unknown Hierarchy].[Invalid]' not found in cube 'Sales'" );
    assertExprThrows(context.getConnection(),
      "StrToMember(\"[Unknown Hierarchy]\").Name",
      "MDX object '[Unknown Hierarchy]' not found in cube 'Sales'" );

    assertAxisThrows(context.getConnection(),
      "StrToMember(\"\")",
      "MDX object '' not found in cube 'Sales'" );

    ((TestConfig)context.getConfig()).setIgnoreInvalidMembersDuringQuery(false);
    assertQueryThrows(context,
      "select \n"
        + "  {[Product].[Food],\n"
        + "    StrToMember(\"[Product].[Drugs]\")} on columns,\n"
        + "  {[Measures].[Unit Sales]} on rows\n"
        + "from [Sales]",
      "Member '[Product].[Drugs]' not found" );
    assertExprThrows(context,
      "StrToMember(\"[Marital Status].[Separated]\").Hierarchy.Name",
      "Member '[Marital Status].[Separated]' not found" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStrToTuple(Context context) {
    // single dimension yields member
    assertAxisReturns(context.getConnection(),
      "{StrToTuple(\"[Time].[1997].[Q2]\", [Time])}",
      "[Time].[1997].[Q2]" );

    // multiple dimensions yield tuple
    assertAxisReturns(context.getConnection(),
      "{StrToTuple(\"([Gender].[F], [Time].[1997].[Q2])\", [Gender], [Time])}",
      "{[Gender].[F], [Time].[1997].[Q2]}" );

    // todo: test for garbage at end of string
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStrToTupleIgnoreInvalidMembers(Context context) {
	RolapSchemaPool.instance().clear();
    ((TestConfig)context.getConfig()).setIgnoreInvalidMembersDuringQuery(true);
    // If any member is invalid, the whole tuple is null.
    assertAxisReturns(context.getConnection(),
      "StrToTuple(\"([Gender].[M], [Marital Status].[Separated])\","
        + " [Gender], [Marital Status])",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStrToTupleDuHierarchiesFails(Context context) {
    assertAxisThrows(context.getConnection(),
      "{StrToTuple(\"([Gender].[F], [Time].[1997].[Q2], [Gender].[M])\", [Gender], [Time], [Gender])}",
      "Tuple contains more than one member of hierarchy '[Gender]'." );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStrToTupleDupHierInSameDimensions(Context context) {
    assertAxisThrows(context.getConnection(),
      "{StrToTuple("
        + "\"([Gender].[F], "
        + "[Time].[1997].[Q2], "
        + "[Time].[Weekly].[1997].[10])\","
        + " [Gender], "
        + hierarchyName( "Time", "Weekly" )
        + ", [Gender])}",
      "Tuple contains more than one member of hierarchy '[Gender]'." );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStrToTupleDepends(Context context) {
    assertMemberExprDependsOn(context.getConnection(),
      "StrToTuple(\"[Time].[1997].[Q2]\", [Time])",
      "{}" );

    // converted to scalar, depends set is larger
    assertExprDependsOn(context.getConnection(),
      "StrToTuple(\"[Time].[1997].[Q2]\", [Time])",
      allHiersExcept( "[Time]" ) );

    assertMemberExprDependsOn(context.getConnection(),
      "StrToTuple(\"[Time].[1997].[Q2], [Gender].[F]\", [Time], [Gender])",
      "{}" );

    assertExprDependsOn(context.getConnection(),
      "StrToTuple(\"[Time].[1997].[Q2], [Gender].[F]\", [Time], [Gender])",
      allHiersExcept( "[Time]", "[Gender]" ) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStrToSet(Context context) {
    // TODO: handle text after '}'
    // TODO: handle string which ends too soon
    // TODO: handle spaces before first '{'
    // TODO: test spaces before unbracketed names,
    //       e.g. "{Gender. M, Gender. F   }".

    assertAxisReturns(context.getConnection(),
      "StrToSet("
        + " \"{[Gender].[F], [Gender].[M]}\","
        + " [Gender])",
      "[Gender].[F]\n"
        + "[Gender].[M]" );

    assertAxisThrows(context.getConnection(),
      "StrToSet("
        + " \"{[Gender].[F], [Time].[1997]}\","
        + " [Gender])",
      "member is of wrong hierarchy" );

    // whitespace ok
    assertAxisReturns(context.getConnection(),
      "StrToSet("
        + " \"  {   [Gender] .  [F]  ,[Gender].[M] }  \","
        + " [Gender])",
      "[Gender].[F]\n"
        + "[Gender].[M]" );

    // tuples
    assertAxisReturns(context.getConnection(),
      "StrToSet("
        + "\""
        + "{"
        + " ([Gender].[F], [Time].[1997].[Q2]), "
        + " ([Gender].[M], [Time].[1997])"
        + "}"
        + "\","
        + " [Gender],"
        + " [Time])",
      "{[Gender].[F], [Time].[1997].[Q2]}\n"
        + "{[Gender].[M], [Time].[1997]}" );

    // matches unique name
    assertAxisReturns(context.getConnection(),
      "StrToSet("
        + "\""
        + "{"
        + " [Store].[USA].[CA], "
        + " [Store].[All Stores].[USA].OR,"
        + " [Store].[All Stores]. [USA] . [WA]"
        + "}"
        + "\","
        + " [Store])",
      "[Store].[USA].[CA]\n"
        + "[Store].[USA].[OR]\n"
        + "[Store].[USA].[WA]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStrToSetDupDimensionsFails(Context context) {
    assertAxisThrows(context.getConnection(),
      "StrToSet("
        + "\""
        + "{"
        + " ([Gender].[F], [Time].[1997].[Q2], [Gender].[F]), "
        + " ([Gender].[M], [Time].[1997], [Gender].[F])"
        + "}"
        + "\","
        + " [Gender],"
        + " [Time],"
        + " [Gender])",
      "Tuple contains more than one member of hierarchy '[Gender]'." );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStrToSetIgnoreInvalidMembers(Context context) {
	RolapSchemaPool.instance().clear();
    ((TestConfig)context.getConfig()).setIgnoreInvalidMembersDuringQuery(true);
    assertAxisReturns(context.getConnection(),
      "StrToSet("
        + "\""
        + "{"
        + " [Product].[Food],"
        + " [Product].[Food].[You wouldn't like],"
        + " [Product].[Drink].[You would like],"
        + " [Product].[Drink].[Dairy]"
        + "}"
        + "\","
        + " [Product])",
      "[Product].[Food]\n"
        + "[Product].[Drink].[Dairy]" );

    assertAxisReturns(context.getConnection(),
      "StrToSet("
        + "\""
        + "{"
        + " ([Gender].[M], [Product].[Food]),"
        + " ([Gender].[F], [Product].[Food].[You wouldn't like]),"
        + " ([Gender].[M], [Product].[Drink].[You would like]),"
        + " ([Gender].[F], [Product].[Drink].[Dairy])"
        + "}"
        + "\","
        + " [Gender], [Product])",
      "{[Gender].[M], [Product].[Food]}\n"
        + "{[Gender].[F], [Product].[Drink].[Dairy]}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testYtd(Context context) {
    assertAxisReturns(context.getConnection(),
      "Ytd()",
      "[Time].[1997]" );
    assertAxisReturns(context.getConnection(),
      "Ytd([Time].[1997].[Q3])",
      "[Time].[1997].[Q1]\n"
        + "[Time].[1997].[Q2]\n"
        + "[Time].[1997].[Q3]" );
    assertAxisReturns(context.getConnection(),
      "Ytd([Time].[1997].[Q2].[4])",
      "[Time].[1997].[Q1].[1]\n"
        + "[Time].[1997].[Q1].[2]\n"
        + "[Time].[1997].[Q1].[3]\n"
        + "[Time].[1997].[Q2].[4]" );
    assertAxisThrows(context.getConnection(),
      "Ytd([Store])",
      "Argument to function 'Ytd' must belong to Time hierarchy" );
    assertSetExprDependsOn(context.getConnection(),
      "Ytd()",
      "{[Time], " + TimeWeekly + "}" );
    assertSetExprDependsOn(context.getConnection(),
      "Ytd([Time].[1997].[Q2])",
      "{}" );
  }

  /**
   * Testcase for
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-458">
   * bug MONDRIAN-458, "error deducing type of Ytd/Qtd/Mtd functions within Generate"</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGeneratePlusXtd(Context context) {
    assertAxisReturns(context.getConnection(),
      "generate(\n"
        + "  {[Time].[1997].[Q1].[2], [Time].[1997].[Q3].[7]},\n"
        + " {Ytd( [Time].[Time].currentMember)})",
      "[Time].[1997].[Q1].[1]\n"
        + "[Time].[1997].[Q1].[2]\n"
        + "[Time].[1997].[Q1].[3]\n"
        + "[Time].[1997].[Q2].[4]\n"
        + "[Time].[1997].[Q2].[5]\n"
        + "[Time].[1997].[Q2].[6]\n"
        + "[Time].[1997].[Q3].[7]" );
    assertAxisReturns(context.getConnection(),
      "generate(\n"
        + "  {[Time].[1997].[Q1].[2], [Time].[1997].[Q3].[7]},\n"
        + " {Ytd( [Time].[Time].currentMember)}, ALL)",
      "[Time].[1997].[Q1].[1]\n"
        + "[Time].[1997].[Q1].[2]\n"
        + "[Time].[1997].[Q1].[1]\n"
        + "[Time].[1997].[Q1].[2]\n"
        + "[Time].[1997].[Q1].[3]\n"
        + "[Time].[1997].[Q2].[4]\n"
        + "[Time].[1997].[Q2].[5]\n"
        + "[Time].[1997].[Q2].[6]\n"
        + "[Time].[1997].[Q3].[7]" );
    assertExprReturns(context.getConnection(),
      "count(generate({[Time].[1997].[Q4].[11]},"
        + " {Qtd( [Time].[Time].currentMember)}))",
      2, 0 );
    assertExprReturns(context.getConnection(),
      "count(generate({[Time].[1997].[Q4].[11]},"
        + " {Mtd( [Time].[Time].currentMember)}))",
      1, 0 );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testQtd(Context context) {
    // zero args
    assertQueryReturns(context.getConnection(),
      "with member [Measures].[Foo] as ' SetToStr(Qtd()) '\n"
        + "select {[Measures].[Foo]} on columns\n"
        + "from [Sales]\n"
        + "where [Time].[1997].[Q2].[5]",
      "Axis #0:\n"
        + "{[Time].[1997].[Q2].[5]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Foo]}\n"
        + "Row #0: {[Time].[1997].[Q2].[4], [Time].[1997].[Q2].[5]}\n" );

    // one arg, a month
    assertAxisReturns(context.getConnection(),
      "Qtd([Time].[1997].[Q2].[5])",
      "[Time].[1997].[Q2].[4]\n"
        + "[Time].[1997].[Q2].[5]" );

    // one arg, a quarter
    assertAxisReturns(context.getConnection(),
      "Qtd([Time].[1997].[Q2])",
      "[Time].[1997].[Q2]" );

    // one arg, a year
    assertAxisReturns(context.getConnection(),
      "Qtd([Time].[1997])",
      "" );

    assertAxisThrows(context.getConnection(),
      "Qtd([Store])",
      "Argument to function 'Qtd' must belong to Time hierarchy" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMtd(Context context) {
    // zero args
    assertQueryReturns(context.getConnection(),
      "with member [Measures].[Foo] as ' SetToStr(Mtd()) '\n"
        + "select {[Measures].[Foo]} on columns\n"
        + "from [Sales]\n"
        + "where [Time].[1997].[Q2].[5]",
      "Axis #0:\n"
        + "{[Time].[1997].[Q2].[5]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Foo]}\n"
        + "Row #0: {[Time].[1997].[Q2].[5]}\n" );

    // one arg, a month
    assertAxisReturns(context.getConnection(),
      "Mtd([Time].[1997].[Q2].[5])",
      "[Time].[1997].[Q2].[5]" );

    // one arg, a quarter
    assertAxisReturns(context.getConnection(),
      "Mtd([Time].[1997].[Q2])",
      "" );

    // one arg, a year
    assertAxisReturns(context.getConnection(),
      "Mtd([Time].[1997])",
      "" );

    assertAxisThrows(context.getConnection(),
      "Mtd([Store])",
      "Argument to function 'Mtd' must belong to Time hierarchy" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testPeriodsToDate(Context context) {
    assertSetExprDependsOn(context.getConnection(), "PeriodsToDate()", "{[Time]}" );
    assertSetExprDependsOn(context.getConnection(),
      "PeriodsToDate([Time].[Year])",
      "{[Time]}" );
    assertSetExprDependsOn(context.getConnection(),
      "PeriodsToDate([Time].[Year], [Time].[1997].[Q2].[5])", "{}" );

    // two args
    assertAxisReturns(context.getConnection(),
      "PeriodsToDate([Time].[Quarter], [Time].[1997].[Q2].[5])",
      "[Time].[1997].[Q2].[4]\n" + "[Time].[1997].[Q2].[5]" );

    // equivalent to above
    assertAxisReturns(context.getConnection(),
      "TopCount("
        + "  Descendants("
        + "    Ancestor("
        + "      [Time].[1997].[Q2].[5], [Time].[Quarter]),"
        + "    [Time].[1997].[Q2].[5].Level),"
        + "  1).Item(0) : [Time].[1997].[Q2].[5]",
      "[Time].[1997].[Q2].[4]\n" + "[Time].[1997].[Q2].[5]" );

    // one arg
    assertQueryReturns(context.getConnection(),
      "with member [Measures].[Foo] as ' SetToStr(PeriodsToDate([Time].[Quarter])) '\n"
        + "select {[Measures].[Foo]} on columns\n"
        + "from [Sales]\n"
        + "where [Time].[1997].[Q2].[5]",
      "Axis #0:\n"
        + "{[Time].[1997].[Q2].[5]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Foo]}\n"
        + "Row #0: {[Time].[1997].[Q2].[4], [Time].[1997].[Q2].[5]}\n" );

    // zero args
    assertQueryReturns(context.getConnection(),
      "with member [Measures].[Foo] as ' SetToStr(PeriodsToDate()) '\n"
        + "select {[Measures].[Foo]} on columns\n"
        + "from [Sales]\n"
        + "where [Time].[1997].[Q2].[5]",
      "Axis #0:\n"
        + "{[Time].[1997].[Q2].[5]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Foo]}\n"
        + "Row #0: {[Time].[1997].[Q2].[4], [Time].[1997].[Q2].[5]}\n" );

    // zero args, evaluated at a member which is at the top level.
    // The default level is the level above the current member -- so
    // choosing a member at the highest level might trip up the
    // implementation.
    assertQueryReturns(context.getConnection(),
      "with member [Measures].[Foo] as ' SetToStr(PeriodsToDate()) '\n"
        + "select {[Measures].[Foo]} on columns\n"
        + "from [Sales]\n"
        + "where [Time].[1997]",
      "Axis #0:\n"
        + "{[Time].[1997]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Foo]}\n"
        + "Row #0: {}\n" );

    // Testcase for bug 1598379, which caused NPE because the args[0].type
    // knew its dimension but not its hierarchy.
    assertQueryReturns(context.getConnection(),
      "with member [Measures].[Position] as\n"
        + " 'Sum("
        + "PeriodsToDate([Time].[Time].Levels(0),"
        + " [Time].[Time].CurrentMember), "
        + "[Measures].[Store Sales])'\n"
        + "select {[Time].[1997],\n"
        + " [Time].[1997].[Q1],\n"
        + " [Time].[1997].[Q1].[1],\n"
        + " [Time].[1997].[Q1].[2],\n"
        + " [Time].[1997].[Q1].[3]} ON COLUMNS,\n"
        + "{[Measures].[Store Sales], [Measures].[Position] } ON ROWS\n"
        + "from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Time].[1997]}\n"
        + "{[Time].[1997].[Q1]}\n"
        + "{[Time].[1997].[Q1].[1]}\n"
        + "{[Time].[1997].[Q1].[2]}\n"
        + "{[Time].[1997].[Q1].[3]}\n"
        + "Axis #2:\n"
        + "{[Measures].[Store Sales]}\n"
        + "{[Measures].[Position]}\n"
        + "Row #0: 565,238.13\n"
        + "Row #0: 139,628.35\n"
        + "Row #0: 45,539.69\n"
        + "Row #0: 44,058.79\n"
        + "Row #0: 50,029.87\n"
        + "Row #1: 565,238.13\n"
        + "Row #1: 139,628.35\n"
        + "Row #1: 45,539.69\n"
        + "Row #1: 89,598.48\n"
        + "Row #1: 139,628.35\n" );

    assertQueryReturns(context.getConnection(),
      "select\n"
        + "{[Measures].[Unit Sales]} on columns,\n"
        + "periodstodate(\n"
        + "    [Product].[Product Category],\n"
        + "    [Product].[Food].[Baked Goods].[Bread].[Muffins]) on rows\n"
        + "from [Sales]\n"
        + "",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Bagels]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Muffins]}\n"
        + "Row #0: 815\n"
        + "Row #1: 3,497\n"
        + "" );

    // TODO: enable
    if ( false ) {
      assertExprThrows(context.getConnection(),
        "Sum(PeriodsToDate([Time.Weekly].[Year], [Time].CurrentMember), [Measures].[Unit Sales])",
        "wrong dimension" );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSetToStr(Context context) {
    assertExprReturns(context.getConnection(),
      "SetToStr([Time].[Time].children)",
      "{[Time].[1997].[Q1], [Time].[1997].[Q2], [Time].[1997].[Q3], [Time].[1997].[Q4]}" );

    // Now, applied to tuples
    assertExprReturns(context.getConnection(),
      "SetToStr({CrossJoin([Marital Status].children, {[Gender].[M]})})",
      "{"
        + "([Marital Status].[M], [Gender].[M]), "
        + "([Marital Status].[S], [Gender].[M])"
        + "}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTupleToStr(Context context) {
    // Applied to a dimension (which becomes a member)
    assertExprReturns(context.getConnection(),
      "TupleToStr([Product])",
      "[Product].[All Products]" );

    // Applied to a dimension (invalid because has no default hierarchy)
    if ( SystemWideProperties.instance().SsasCompatibleNaming ) {
      assertExprThrows(context.getConnection(),
        "TupleToStr([Time])",
        "The 'Time' dimension contains more than one hierarchy, "
          + "therefore the hierarchy must be explicitly specified." );
    } else {
      assertExprReturns(context.getConnection(),
        "TupleToStr([Time])",
        "[Time].[1997]" );
    }

    // Applied to a hierarchy
    assertExprReturns(context.getConnection(),
      "TupleToStr([Time].[Time])",
      "[Time].[1997]" );

    // Applied to a member
    assertExprReturns(context.getConnection(),
      "TupleToStr([Store].[USA].[OR])",
      "[Store].[USA].[OR]" );

    // Applied to a member (extra set of parens)
    assertExprReturns(context.getConnection(),
      "TupleToStr(([Store].[USA].[OR]))",
      "([Store].[USA].[OR])" );

    // Now, applied to a tuple
    assertExprReturns(context.getConnection(),
      "TupleToStr(([Marital Status], [Gender].[M]))",
      "([Marital Status].[All Marital Status], [Gender].[M])" );

    // Applied to a tuple containing a null member
    assertExprReturns(context.getConnection(),
      "TupleToStr(([Marital Status], [Gender].Parent))",
      "" );

    // Applied to a null member
    assertExprReturns(context.getConnection(),
      "TupleToStr([Marital Status].Parent)",
      "" );
  }

  /**
   * Executes a scalar expression, and asserts that the result is as expected. For example, <code>assertExprReturns ("1
   * + 2", "3")</code> should succeed.
   */
  void assertExprReturns(Connection connection, String expr, String expected ) {
    String actual = executeExpr(connection, expr);
    assertEquals( expected, actual );
  }

  /**
   * Executes a scalar expression, and asserts that the result is within delta of the expected result.
   *
   * @param expr     MDX scalar expression
   * @param expected Expected value
   * @param delta    Maximum allowed deviation from expected value
   */
  void assertExprReturns(Connection connection,
    String expr, double expected, double delta ) {
    Object value = executeExprRaw(connection, expr).getValue();

    try {
      double actual = ( (Number) value ).doubleValue();
      if ( Double.isNaN( expected ) && Double.isNaN( actual ) ) {
        return;
      }
      assertEquals(
        expected,
        actual,
        delta, "");
    } catch ( ClassCastException ex ) {
      String msg = "Actual value \"" + value + "\" is not a number.";
      throw new AssertionFailedError(
        msg, Double.toString( expected ), String.valueOf( value ) );
    }
  }

  /**
   * Compiles a scalar expression, and asserts that the program looks as expected.
   */
  void assertExprCompilesTo(Connection connection,
    String expr,
    String expectedCalc ) {
    final String actualCalc =
      compileExpression(connection, expr, true);
    final int expDeps =
      connection.getContext().getConfig().testExpDependencies();
    if ( expDeps > 0 ) {
      // Don't bother checking the compiled output if we are also
      // testing dependencies. The compiled code will have extra
      // 'DependencyTestingCalc' instances embedded in it.
      return;
    }
    assertStubbedEqualsVerbose( expectedCalc, actualCalc );
  }

  /**
   * Compiles a set expression, and asserts that the program looks as expected.
   */
  void assertAxisCompilesTo(Connection connection,
    String expr,
    String expectedCalc ) {
    final String actualCalc =
      compileExpression(connection, expr, false);
    final int expDeps =
      connection.getContext().getConfig().testExpDependencies();
    if ( expDeps > 0 ) {
      // Don't bother checking the compiled output if we are also
      // testing dependencies. The compiled code will have extra
      // 'DependencyTestingCalc' instances embedded in it.
      return;
    }
    assertStubbedEqualsVerbose( expectedCalc, actualCalc );
  }

  /**
   * Tests the <code>Rank(member, set)</code> MDX function.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRank(Context context) {
    // Member within set
    assertExprReturns(context.getConnection(),
      "Rank([Store].[USA].[CA], "
        + "{[Store].[USA].[OR],"
        + " [Store].[USA].[CA],"
        + " [Store].[USA]})", "2" );
    // Member not in set
    assertExprReturns(context.getConnection(),
      "Rank([Store].[USA].[WA], "
        + "{[Store].[USA].[OR],"
        + " [Store].[USA].[CA],"
        + " [Store].[USA]})", "0" );
    // Member not in empty set
    assertExprReturns(context.getConnection(),
      "Rank([Store].[USA].[WA], {})", "0" );
    // Null member not in set returns null.
    assertExprReturns(context.getConnection(),
      "Rank([Store].Parent, "
        + "{[Store].[USA].[OR],"
        + " [Store].[USA].[CA],"
        + " [Store].[USA]})", "" );
    // Null member in empty set. (MSAS returns an error "Formula error -
    // dimension count is not valid - in the Rank function" but I think
    // null is the correct behavior.)
    assertExprReturns(context.getConnection(),
      "Rank([Gender].Parent, {})", "" );
    // Member occurs twice in set -- pick first
    assertExprReturns(context.getConnection(),
      "Rank([Store].[USA].[WA], \n"
        + "{[Store].[USA].[WA],"
        + " [Store].[USA].[CA],"
        + " [Store].[USA],"
        + " [Store].[USA].[WA]})", "1" );
    // Tuple not in set
    assertExprReturns(context.getConnection(),
      "Rank(([Gender].[F], [Marital Status].[M]), \n"
        + "{([Gender].[F], [Marital Status].[S]),\n"
        + " ([Gender].[M], [Marital Status].[S]),\n"
        + " ([Gender].[M], [Marital Status].[M])})", "0" );
    // Tuple in set
    assertExprReturns(context.getConnection(),
      "Rank(([Gender].[F], [Marital Status].[M]), \n"
        + "{([Gender].[F], [Marital Status].[S]),\n"
        + " ([Gender].[M], [Marital Status].[S]),\n"
        + " ([Gender].[F], [Marital Status].[M])})", "3" );
    // Tuple not in empty set
    assertExprReturns(context.getConnection(),
      "Rank(([Gender].[F], [Marital Status].[M]), \n" + "{})", "0" );
    // Partially null tuple in set, returns null
    assertExprReturns(context.getConnection(),
      "Rank(([Gender].[F], [Marital Status].Parent), \n"
        + "{([Gender].[F], [Marital Status].[S]),\n"
        + " ([Gender].[M], [Marital Status].[S]),\n"
        + " ([Gender].[F], [Marital Status].[M])})", "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRankWithExpr(Context context) {
    // Note that [Good] and [Top Measure] have the same [Unit Sales]
    // value (5), but [Good] ranks 1 and [Top Measure] ranks 2. Even though
    // they are sorted descending on unit sales, they remain in their
    // natural order (member name) because MDX sorts are stable.
    assertQueryReturns(context.getConnection(),
      "with member [Measures].[Sibling Rank] as ' Rank([Product].CurrentMember, [Product].CurrentMember.Siblings) '\n"
        + "  member [Measures].[Sales Rank] as ' Rank([Product].CurrentMember, Order([Product].Parent.Children, "
        + "[Measures].[Unit Sales], DESC)) '\n"
        + "  member [Measures].[Sales Rank2] as ' Rank([Product].CurrentMember, [Product].Parent.Children, [Measures]"
        + ".[Unit Sales]) '\n"
        + "select {[Measures].[Unit Sales], [Measures].[Sales Rank], [Measures].[Sales Rank2]} on columns,\n"
        + " {[Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].children} on rows\n"
        + "from [Sales]\n"
        + "WHERE ([Store].[USA].[OR].[Portland].[Store 11], [Time].[1997].[Q2].[6])",
      "Axis #0:\n"
        + "{[Store].[USA].[OR].[Portland].[Store 11], [Time].[1997].[Q2].[6]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Sales Rank]}\n"
        + "{[Measures].[Sales Rank2]}\n"
        + "Axis #2:\n"
        + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]}\n"
        + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Pearl]}\n"
        + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth]}\n"
        + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Top Measure]}\n"
        + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Walrus]}\n"
        + "Row #0: 5\n"
        + "Row #0: 1\n"
        + "Row #0: 1\n"
        + "Row #1: \n"
        + "Row #1: 5\n"
        + "Row #1: 5\n"
        + "Row #2: 3\n"
        + "Row #2: 3\n"
        + "Row #2: 3\n"
        + "Row #3: 5\n"
        + "Row #3: 2\n"
        + "Row #3: 1\n"
        + "Row #4: 3\n"
        + "Row #4: 4\n"
        + "Row #4: 3\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRankMembersWithTiedExpr(Context context) {
    assertQueryReturns(context.getConnection(),
      "with "
        + " Set [Beers] as {[Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].children} "
        + "  member [Measures].[Sales Rank] as ' Rank([Product].CurrentMember, [Beers], [Measures].[Unit Sales]) '\n"
        + "select {[Measures].[Unit Sales], [Measures].[Sales Rank]} on columns,\n"
        + " Generate([Beers], {[Product].CurrentMember}) on rows\n"
        + "from [Sales]\n"
        + "WHERE ([Store].[USA].[OR].[Portland].[Store 11], [Time].[1997].[Q2].[6])",
      "Axis #0:\n"
        + "{[Store].[USA].[OR].[Portland].[Store 11], [Time].[1997].[Q2].[6]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Sales Rank]}\n"
        + "Axis #2:\n"
        + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]}\n"
        + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Pearl]}\n"
        + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth]}\n"
        + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Top Measure]}\n"
        + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Walrus]}\n"
        + "Row #0: 5\n"
        + "Row #0: 1\n"
        + "Row #1: \n"
        + "Row #1: 5\n"
        + "Row #2: 3\n"
        + "Row #2: 3\n"
        + "Row #3: 5\n"
        + "Row #3: 1\n"
        + "Row #4: 3\n"
        + "Row #4: 3\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRankTuplesWithTiedExpr(Context context) {
    assertQueryReturns(context.getConnection(),
      "with "
        + " Set [Beers for Store] as 'NonEmptyCrossJoin("
        + "[Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].children, "
        + "{[Store].[USA].[OR].[Portland].[Store 11]})' "
        + "  member [Measures].[Sales Rank] as ' Rank(([Product].CurrentMember,[Store].CurrentMember), [Beers for "
        + "Store], [Measures].[Unit Sales]) '\n"
        + "select {[Measures].[Unit Sales], [Measures].[Sales Rank]} on columns,\n"
        + " Generate([Beers for Store], {([Product].CurrentMember, [Store].CurrentMember)}) on rows\n"
        + "from [Sales]\n"
        + "WHERE ([Time].[1997].[Q2].[6])",
      "Axis #0:\n"
        + "{[Time].[1997].[Q2].[6]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Sales Rank]}\n"
        + "Axis #2:\n"
        + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good], [Store].[USA].[OR].[Portland]"
        + ".[Store 11]}\n"
        + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth], [Store].[USA].[OR]"
        + ".[Portland].[Store 11]}\n"
        + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Top Measure], [Store].[USA].[OR]"
        + ".[Portland].[Store 11]}\n"
        + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Walrus], [Store].[USA].[OR].[Portland]"
        + ".[Store 11]}\n"
        + "Row #0: 5\n"
        + "Row #0: 1\n"
        + "Row #1: 3\n"
        + "Row #1: 3\n"
        + "Row #2: 5\n"
        + "Row #2: 1\n"
        + "Row #3: 3\n"
        + "Row #3: 3\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRankWithExpr2(Context context) {
    // Data: Unit Sales
    // All gender 266,733
    // F          131,558
    // M          135,215
    assertExprReturns(context.getConnection(),
      "Rank([Gender].[All Gender],"
        + " {[Gender].Members},"
        + " [Measures].[Unit Sales])", "1" );
    assertExprReturns(context.getConnection(),
      "Rank([Gender].[F],"
        + " {[Gender].Members},"
        + " [Measures].[Unit Sales])", "3" );
    assertExprReturns(context.getConnection(),
      "Rank([Gender].[M],"
        + " {[Gender].Members},"
        + " [Measures].[Unit Sales])", "2" );
    // Null member. Expression evaluates to null, therefore value does
    // not appear in the list of values, therefore the rank is null.
    assertExprReturns(context.getConnection(),
      "Rank([Gender].[All Gender].Parent,"
        + " {[Gender].Members},"
        + " [Measures].[Unit Sales])", "" );
    // Empty set. Value would appear after all elements in the empty set,
    // therefore rank is 1.
    // Note that SSAS gives error 'The first argument to the Rank function,
    // a tuple expression, should reference the same hierachies as the
    // second argument, a set expression'. I think that's because it can't
    // deduce a type for '{}'. SSAS's problem, not Mondrian's. :)
    assertExprReturns(context.getConnection(),
      "Rank([Gender].[M],"
        + " {},"
        + " [Measures].[Unit Sales])",
      "1" );
    // As above, but SSAS can type-check this.
    assertExprReturns(context.getConnection(),
      "Rank([Gender].[M],"
        + " Filter(Gender.Members, 1 = 0),"
        + " [Measures].[Unit Sales])",
      "1" );
    // Member is not in set
    assertExprReturns(context.getConnection(),
      "Rank([Gender].[M]," + " {[Gender].[All Gender], [Gender].[F]})",
      "0" );
    // Even though M is not in the set, its value lies between [All Gender]
    // and [F].
    assertExprReturns(context.getConnection(),
      "Rank([Gender].[M],"
        + " {[Gender].[All Gender], [Gender].[F]},"
        + " [Measures].[Unit Sales])", "2" );
    // Expr evaluates to null for some values of set.
    assertExprReturns(context.getConnection(),
      "Rank([Product].[Non-Consumable].[Household],"
        + " {[Product].[Food], [Product].[All Products], [Product].[Drink].[Dairy]},"
        + " [Product].CurrentMember.Parent)", "2" );
    // Expr evaluates to null for all values in the set.
    assertExprReturns(context.getConnection(),
      "Rank([Gender].[M],"
        + " {[Gender].[All Gender], [Gender].[F]},"
        + " [Marital Status].[All Marital Status].Parent)", "1" );
  }

  /**
   * Tests the 3-arg version of the RANK function with a value which returns null within a set of nulls.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRankWithNulls(Context context) {
    assertQueryReturns(context.getConnection(),
      "with member [Measures].[X] as "
        + "'iif([Measures].[Store Sales]=777,"
        + "[Measures].[Store Sales],Null)'\n"
        + "member [Measures].[Y] as 'Rank([Gender].[M],"
        + "{[Measures].[X],[Measures].[X],[Measures].[X]},"
        + " [Marital Status].[All Marital Status].Parent)'"
        + "select {[Measures].[Y]} on columns from Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Y]}\n"
        + "Row #0: 1\n" );
  }

  /**
   * Tests a RANK function which is so large that we need to use caching in order to execute it efficiently.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRankHuge(Context context) {
    // If caching is disabled, don't even try -- it will take too long.
    if ( !SystemWideProperties.instance().EnableExpCache ) {
      return;
    }

    checkRankHuge(context.getConnection(),
      "WITH \n"
        + "  MEMBER [Measures].[Rank among products] \n"
        + "    AS ' Rank([Product].CurrentMember, "
        + "            Order([Product].members, "
        + "            [Measures].[Unit Sales], BDESC)) '\n"
        + "SELECT CrossJoin(\n"
        + "  [Gender].members,\n"
        + "  {[Measures].[Unit Sales],\n"
        + "   [Measures].[Rank among products]}) ON COLUMNS,\n"
        // + "  {[Product], [Product].[All Products].[Non-Consumable].
        // [Periodicals].[Magazines].[Sports Magazines].[Robust].
        // [Robust Monthly Sports Magazine]} ON ROWS\n"
        + "  {[Product].members} ON ROWS\n"
        + "FROM [Sales]",
      false );
  }

  /**
   * As {@link #testRankHuge()}, but for the 3-argument form of the
   * <code>RANK</code> function.
   *
   * <p>Disabled by jhyde, 2006/2/14. Bug 1431316 logged.
   */
  @Disabled //disabled for CI build
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void _testRank3Huge(Context context) {
    // If caching is disabled, don't even try -- it will take too long.
    if ( !SystemWideProperties.instance().EnableExpCache ) {
      return;
    }

    checkRankHuge(context.getConnection(),
      "WITH \n"
        + "  MEMBER [Measures].[Rank among products] \n"
        + "    AS ' Rank([Product].CurrentMember, [Product].members, [Measures].[Unit Sales]) '\n"
        + "SELECT CrossJoin(\n"
        + "  [Gender].members,\n"
        + "  {[Measures].[Unit Sales],\n"
        + "   [Measures].[Rank among products]}) ON COLUMNS,\n"
        + "  {[Product],"
        + "   [Product].[All Products].[Non-Consumable].[Periodicals]"
        + ".[Magazines].[Sports Magazines].[Robust]"
        + ".[Robust Monthly Sports Magazine]} ON ROWS\n"
        // + "  {[Product].members} ON ROWS\n"
        + "FROM [Sales]",
      true );
  }

  private void checkRankHuge(Connection connection, String query, boolean rank3 ) {
    final Result result = executeQuery(connection, query );
    final Axis[] axes = result.getAxes();
    final Axis rowsAxis = axes[ 1 ];
    final int rowCount = rowsAxis.getPositions().size();
    assertEquals( 2256, rowCount );
    // [All Products], [All Gender], [Rank]
    Cell cell = result.getCell( new int[] { 1, 0 } );
    assertEquals( "1", cell.getFormattedValue() );
    // [Robust Monthly Sports Magazine]
    Member member = rowsAxis.getPositions().get( rowCount - 1 ).get( 0 );
    assertEquals( "Robust Monthly Sports Magazine", member.getName() );
    // [Robust Monthly Sports Magazine], [All Gender], [Rank]
    cell = result.getCell( new int[] { 0, rowCount - 1 } );
    assertEquals( "152", cell.getFormattedValue() );
    cell = result.getCell( new int[] { 1, rowCount - 1 } );
    assertEquals( rank3 ? "1,854" : "1,871", cell.getFormattedValue() );
    // [Robust Monthly Sports Magazine], [Gender].[F], [Rank]
    cell = result.getCell( new int[] { 2, rowCount - 1 } );
    assertEquals( "90", cell.getFormattedValue() );
    cell = result.getCell( new int[] { 3, rowCount - 1 } );
    assertEquals( rank3 ? "1,119" : "1,150", cell.getFormattedValue() );
    // [Robust Monthly Sports Magazine], [Gender].[M], [Rank]
    cell = result.getCell( new int[] { 4, rowCount - 1 } );
    assertEquals( "62", cell.getFormattedValue() );
    cell = result.getCell( new int[] { 5, rowCount - 1 } );
    assertEquals( rank3 ? "2,131" : "2,147", cell.getFormattedValue() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLinRegPointQuarter(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Measures].[Test] as \n"
        + "  'LinRegPoint(\n"
        + "    Rank(Time.[Time].CurrentMember, Time.[Time].CurrentMember.Level.Members),\n"
        + "    Descendants([Time].[1997], [Time].[Quarter]), \n"
        + "[Measures].[Store Sales], \n"
        + "    Rank(Time.[Time].CurrentMember, Time.[Time].CurrentMember.Level.Members))' \n"
        + "SELECT \n"
        + "{[Measures].[Test],[Measures].[Store Sales]} ON ROWS, \n"
        + "{[Time].[1997].Children} ON COLUMNS \n"
        + "FROM Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Time].[1997].[Q1]}\n"
        + "{[Time].[1997].[Q2]}\n"
        + "{[Time].[1997].[Q3]}\n"
        + "{[Time].[1997].[Q4]}\n"
        + "Axis #2:\n"
        + "{[Measures].[Test]}\n"
        + "{[Measures].[Store Sales]}\n"
        + "Row #0: 134,299.22\n"
        + "Row #0: 138,972.76\n"
        + "Row #0: 143,646.30\n"
        + "Row #0: 148,319.85\n"
        + "Row #1: 139,628.35\n"
        + "Row #1: 132,666.27\n"
        + "Row #1: 140,271.89\n"
        + "Row #1: 152,671.62\n" );
  }

  /**
   * Tests all of the linear regression functions, as suggested by
   * <a href="http://support.microsoft.com/kb/q307276/">a Microsoft knowledge
   * base article</a>.
   */
  @Disabled //disabled for CI build
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void _testLinRegAll(Context context) {
    // We have not implemented the LastPeriods function, so we use
    //   [Time].CurrentMember.Lag(9) : [Time].CurrentMember
    // is equivalent to
    //   LastPeriods(10)
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER \n"
        + "[Measures].[Intercept] AS \n"
        + "  'LinRegIntercept([Time].CurrentMember.Lag(10) : [Time].CurrentMember, [Measures].[Unit Sales], "
        + "[Measures].[Store Sales])' \n"
        + "MEMBER [Measures].[Regression Slope] AS\n"
        + "  'LinRegSlope([Time].CurrentMember.Lag(9) : [Time].CurrentMember,[Measures].[Unit Sales],[Measures]"
        + ".[Store Sales]) '\n"
        + "MEMBER [Measures].[Predict] AS\n"
        + "  'LinRegPoint([Measures].[Unit Sales],[Time].CurrentMember.Lag(9) : [Time].CurrentMember,[Measures].[Unit"
        + " Sales],[Measures].[Store Sales])',\n"
        + "  FORMAT_STRING = 'Standard' \n"
        + "MEMBER [Measures].[Predict Formula] AS\n"
        + "  '([Measures].[Regression Slope] * [Measures].[Unit Sales]) + [Measures].[Intercept]',\n"
        + "  FORMAT_STRING='Standard'\n"
        + "MEMBER [Measures].[Good Fit] AS\n"
        + "  'LinRegR2([Time].CurrentMember.Lag(9) : [Time].CurrentMember, [Measures].[Unit Sales],[Measures].[Store "
        + "Sales])',\n"
        + "  FORMAT_STRING='#,#.00'\n"
        + "MEMBER [Measures].[Variance] AS\n"
        + "  'LinRegVariance([Time].CurrentMember.Lag(9) : [Time].CurrentMember,[Measures].[Unit Sales],[Measures]"
        + ".[Store Sales])'\n"
        + "SELECT \n"
        + "  {[Measures].[Store Sales], \n"
        + "   [Measures].[Intercept], \n"
        + "   [Measures].[Regression Slope], \n"
        + "   [Measures].[Predict], \n"
        + "   [Measures].[Predict Formula], \n"
        + "   [Measures].[Good Fit], \n"
        + "   [Measures].[Variance] } ON COLUMNS, \n"
        + "  Descendants([Time].[1997], [Time].[Month]) ON ROWS\n"
        + "FROM Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Store Sales]}\n"
        + "{[Measures].[Intercept]}\n"
        + "{[Measures].[Regression Slope]}\n"
        + "{[Measures].[Predict]}\n"
        + "{[Measures].[Predict Formula]}\n"
        + "{[Measures].[Good Fit]}\n"
        + "{[Measures].[Variance]}\n"
        + "Axis #2:\n"
        + "{[Time].[1997].[Q1].[1]}\n"
        + "{[Time].[1997].[Q1].[2]}\n"
        + "{[Time].[1997].[Q1].[3]}\n"
        + "{[Time].[1997].[Q2].[4]}\n"
        + "{[Time].[1997].[Q2].[5]}\n"
        + "{[Time].[1997].[Q2].[6]}\n"
        + "{[Time].[1997].[Q3].[7]}\n"
        + "{[Time].[1997].[Q3].[8]}\n"
        + "{[Time].[1997].[Q3].[9]}\n"
        + "{[Time].[1997].[Q4].[10]}\n"
        + "{[Time].[1997].[Q4].[11]}\n"
        + "{[Time].[1997].[Q4].[12]}\n"
        + "Row #0: 45,539.69\n"
        + "Row #0: 68711.40\n"
        + "Row #0: -1.033\n"
        + "Row #0: 46,350.26\n"
        + "Row #0: 46.350.26\n"
        + "Row #0: -1.#INF\n"
        + "Row #0: 5.17E-08\n"
        + "...\n"
        + "Row #11: 15343.67\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLinRegPointMonth(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER \n"
        + "[Measures].[Test] as \n"
        + "  'LinRegPoint(\n"
        + "    Rank(Time.[Time].CurrentMember, Time.[Time].CurrentMember.Level.Members),\n"
        + "    Descendants([Time].[1997], [Time].[Month]), \n"
        + "    [Measures].[Store Sales], \n"
        + "    Rank(Time.[Time].CurrentMember, Time.[Time].CurrentMember.Level.Members)\n"
        + " )' \n"
        + "SELECT \n"
        + "  {[Measures].[Test],[Measures].[Store Sales]} ON ROWS, \n"
        + "  Descendants([Time].[1997], [Time].[Month]) ON COLUMNS \n"
        + "FROM Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Time].[1997].[Q1].[1]}\n"
        + "{[Time].[1997].[Q1].[2]}\n"
        + "{[Time].[1997].[Q1].[3]}\n"
        + "{[Time].[1997].[Q2].[4]}\n"
        + "{[Time].[1997].[Q2].[5]}\n"
        + "{[Time].[1997].[Q2].[6]}\n"
        + "{[Time].[1997].[Q3].[7]}\n"
        + "{[Time].[1997].[Q3].[8]}\n"
        + "{[Time].[1997].[Q3].[9]}\n"
        + "{[Time].[1997].[Q4].[10]}\n"
        + "{[Time].[1997].[Q4].[11]}\n"
        + "{[Time].[1997].[Q4].[12]}\n"
        + "Axis #2:\n"
        + "{[Measures].[Test]}\n"
        + "{[Measures].[Store Sales]}\n"
        + "Row #0: 43,824.36\n"
        + "Row #0: 44,420.51\n"
        + "Row #0: 45,016.66\n"
        + "Row #0: 45,612.81\n"
        + "Row #0: 46,208.95\n"
        + "Row #0: 46,805.10\n"
        + "Row #0: 47,401.25\n"
        + "Row #0: 47,997.40\n"
        + "Row #0: 48,593.55\n"
        + "Row #0: 49,189.70\n"
        + "Row #0: 49,785.85\n"
        + "Row #0: 50,382.00\n"
        + "Row #1: 45,539.69\n"
        + "Row #1: 44,058.79\n"
        + "Row #1: 50,029.87\n"
        + "Row #1: 42,878.25\n"
        + "Row #1: 44,456.29\n"
        + "Row #1: 45,331.73\n"
        + "Row #1: 50,246.88\n"
        + "Row #1: 46,199.04\n"
        + "Row #1: 43,825.97\n"
        + "Row #1: 42,342.27\n"
        + "Row #1: 53,363.71\n"
        + "Row #1: 56,965.64\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLinRegIntercept(Context context) {
    assertExprReturns(context.getConnection(),
      "LinRegIntercept([Time].[Month].members,"
        + " [Measures].[Unit Sales], [Measures].[Store Sales])",
      -126.65,
      0.50 );

/*
-1#IND missing data
*/
/*
1#INF division by zero
*/
/*
The following table shows query return values from using different
FORMAT_STRING's in an expression involving 'division by zero' (tested on
Intel platforms):

+===========================+=====================+
| Format Strings            | Query Return Values |
+===========================+=====================+
| FORMAT_STRING="           | 1.#INF              |
+===========================+=====================+
| FORMAT_STRING='Standard'  | 1.#J                |
+===========================+=====================+
| FORMAT_STRING='Fixed'     | 1.#J                |
+===========================+=====================+
| FORMAT_STRING='Percent'   | 1#I.NF%             |
+===========================+=====================+
| FORMAT_STRING='Scientific'| 1.JE+00             |
+===========================+=====================+
*/

    // Mondrian can not return "missing data" value -1.#IND
    // empty set
    if ( false ) {
      assertExprReturns(context.getConnection(),
        "LinRegIntercept({[Time].Parent},"
          + " [Measures].[Unit Sales], [Measures].[Store Sales])",
        "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)
    }

    // first expr constant
    if ( false ) {
      assertExprReturns(context.getConnection(),
        "LinRegIntercept([Time].[Month].members,"
          + " 7, [Measures].[Store Sales])",
        "$7.00" );
    }

    // format does not add '$'
    assertExprReturns(context.getConnection(),
      "LinRegIntercept([Time].[Month].members,"
        + " 7, [Measures].[Store Sales])",
      7.00,
      0.01 );

    // Mondrian can not return "missing data" value -1.#IND
    // second expr constant
    if ( false ) {
      assertExprReturns(context.getConnection(),
        "LinRegIntercept([Time].[Month].members,"
          + " [Measures].[Unit Sales], 4)",
        "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLinRegSlope(Context context) {
    assertExprReturns(context.getConnection(),
      "LinRegSlope([Time].[Month].members,"
        + " [Measures].[Unit Sales], [Measures].[Store Sales])",
      0.4746,
      0.50 );

    // Mondrian can not return "missing data" value -1.#IND
    // empty set
    if ( false ) {
      assertExprReturns(context.getConnection(),
        "LinRegSlope({[Time].Parent},"
          + " [Measures].[Unit Sales], [Measures].[Store Sales])",
        "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)
    }

    // first expr constant
    if ( false ) {
      assertExprReturns(context.getConnection(),
        "LinRegSlope([Time].[Month].members,"
          + " 7, [Measures].[Store Sales])",
        "$7.00" );
    }
    // ^^^^
    // copy and paste error

    assertExprReturns(context.getConnection(),
      "LinRegSlope([Time].[Month].members,"
        + " 7, [Measures].[Store Sales])",
      0.00,
      0.01 );

    // Mondrian can not return "missing data" value -1.#IND
    // second expr constant
    if ( false ) {
      assertExprReturns(context.getConnection(),
        "LinRegSlope([Time].[Month].members,"
          + " [Measures].[Unit Sales], 4)",
        "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLinRegPoint(Context context) {
    // NOTE: mdx does not parse
    if ( false ) {
      assertExprReturns(context.getConnection(),
        "LinRegPoint([Measures].[Unit Sales],"
          + " [Time].CurrentMember[Time].[Month].members,"
          + " [Measures].[Unit Sales], [Measures].[Store Sales])",
        "0.4746" );
    }

    // Mondrian can not return "missing data" value -1.#IND
    // empty set
    if ( false ) {
      assertExprReturns(context.getConnection(),
        "LinRegPoint([Measures].[Unit Sales],"
          + " {[Time].Parent},"
          + " [Measures].[Unit Sales], [Measures].[Store Sales])",
        "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)
    }

    // Expected value is wrong
    // zeroth expr constant
    if ( false ) {
      assertExprReturns(context.getConnection(),
        "LinRegPoint(-1,"
          + " [Time].[Month].members,"
          + " 7, [Measures].[Store Sales])", "-127.124" );
    }

    // first expr constant
    if ( false ) {
      assertExprReturns(context.getConnection(),
        "LinRegPoint([Measures].[Unit Sales],"
          + " [Time].[Month].members,"
          + " 7, [Measures].[Store Sales])", "$7.00" );
    }

    // format does not add '$'
    assertExprReturns(context.getConnection(),
      "LinRegPoint([Measures].[Unit Sales],"
        + " [Time].[Month].members,"
        + " 7, [Measures].[Store Sales])",
      7.00,
      0.01 );

    // Mondrian can not return "missing data" value -1.#IND
    // second expr constant
    if ( false ) {
      assertExprReturns(context.getConnection(),
        "LinRegPoint([Measures].[Unit Sales],"
          + " [Time].[Month].members,"
          + " [Measures].[Unit Sales], 4)",
        "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)
    }
  }

  @Disabled //disabled for CI build
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void _testLinRegR2(Context context) {
    // Why would R2 equal the slope
    if ( false ) {
      assertExprReturns(context.getConnection(),
        "LinRegR2([Time].[Month].members,"
          + " [Measures].[Unit Sales], [Measures].[Store Sales])",
        "0.4746" );
    }

    // Mondrian can not return "missing data" value -1.#IND
    // empty set
    if ( false ) {
      assertExprReturns(context.getConnection(),
        "LinRegR2({[Time].Parent},"
          + " [Measures].[Unit Sales], [Measures].[Store Sales])",
        "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)
    }

    // first expr constant
    assertExprReturns(context.getConnection(),
      "LinRegR2([Time].[Month].members,"
        + " 7, [Measures].[Store Sales])",
      "$7.00" );

    // Mondrian can not return "missing data" value -1.#IND
    // second expr constant
    if ( false ) {
      assertExprReturns(context.getConnection(),
        "LinRegR2([Time].[Month].members,"
          + " [Measures].[Unit Sales], 4)",
        "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)
    }
  }

  @Disabled //disabled for CI build
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void _testLinRegVariance(Context context) {
    assertExprReturns(context.getConnection(),
      "LinRegVariance([Time].[Month].members,"
        + " [Measures].[Unit Sales], [Measures].[Store Sales])",
      "0.4746" );

    // empty set
    assertExprReturns(context.getConnection(),
      "LinRegVariance({[Time].Parent},"
        + " [Measures].[Unit Sales], [Measures].[Store Sales])",
      "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)

    // first expr constant
    assertExprReturns(context.getConnection(),
      "LinRegVariance([Time].[Month].members,"
        + " 7, [Measures].[Store Sales])",
      "$7.00" );

    // second expr constant
    assertExprReturns(context.getConnection(),
      "LinRegVariance([Time].[Month].members,"
        + " [Measures].[Unit Sales], 4)",
      "-1.#IND" ); // MSAS returns -1.#IND (whatever that means)
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVisualTotalsBasic(Context context) {
    assertQueryReturns(context.getConnection(),
      "select {[Measures].[Unit Sales]} on columns, "
        + "{VisualTotals("
        + "    {[Product].[All Products].[Food].[Baked Goods].[Bread],"
        + "     [Product].[All Products].[Food].[Baked Goods].[Bread].[Bagels],"
        + "     [Product].[All Products].[Food].[Baked Goods].[Bread].[Muffins]},"
        + "     \"**Subtotal - *\")} on rows "
        + "from [Sales]",

      // note that Subtotal - Bread only includes 2 displayed children
      // in member with visual totals name is the same but caption is changed
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Product].[Food].[Baked Goods].[Bread]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Bagels]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Muffins]}\n"
        + "Row #0: 4,312\n"
        + "Row #1: 815\n"
        + "Row #2: 3,497\n" );
  }

  @Disabled //disabled for CI build
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVisualTotalsConsecutively(Context context) {
    assertQueryReturns(context.getConnection(),
      "select {[Measures].[Unit Sales]} on columns, "
        + "{VisualTotals("
        + "    {[Product].[All Products].[Food].[Baked Goods].[Bread],"
        + "     [Product].[All Products].[Food].[Baked Goods].[Bread].[Bagels],"
        + "     [Product].[All Products].[Food].[Baked Goods].[Bread].[Bagels],"
        + "     [Product].[All Products].[Food].[Baked Goods].[Bread].[Bagels].[Colony],"
        + "     [Product].[All Products].[Food].[Baked Goods].[Bread].[Bagels],"
        + "     [Product].[All Products].[Food].[Baked Goods].[Bread].[Muffins]},"
        + "     \"**Subtotal - *\")} on rows "
        + "from [Sales]",

      // Note that [Bagels] occurs 3 times, but only once does it
      // become a subtotal. Note that the subtotal does not include
      // the following [Bagels] member.
      // in member with visual totals name is the same but caption is changed
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Product].[Food].[Baked Goods].[Bread]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Bagels]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Bagels]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Bagels].[Colony]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Bagels]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Muffins]}\n"
        + "Row #0: 3,660\n"
        + "Row #1: 163\n"
        + "Row #2: 163\n"
        + "Row #3: 163\n"
        + "Row #4: 163\n"
        + "Row #5: 3,497\n" );
    	// test is working incorrect. should be 163, 163, 163, 163, 3497  = 3497 + 163.  This is Analysis Services behavior.
    	// We should use only [Product].[Food].[Baked Goods].[Bread].[Bagels].[Colony] for  [Food].[Baked Goods].[Bread].[Bagels]
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVisualTotalsNoPattern(Context context) {
    assertAxisReturns(context.getConnection(),
      "VisualTotals("
        + "    {[Product].[All Products].[Food].[Baked Goods].[Bread],"
        + "     [Product].[All Products].[Food].[Baked Goods].[Bread].[Bagels],"
        + "     [Product].[All Products].[Food].[Baked Goods].[Bread].[Muffins]})",

      // Note that the [Bread] visual member is just called [Bread].
      "[Product].[Food].[Baked Goods].[Bread]\n"
        + "[Product].[Food].[Baked Goods].[Bread].[Bagels]\n"
        + "[Product].[Food].[Baked Goods].[Bread].[Muffins]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVisualTotalsWithFilter(Context context) {
    assertQueryReturns(context.getConnection(),
      "select {[Measures].[Unit Sales]} on columns, "
        + "{Filter("
        + "    VisualTotals("
        + "        {[Product].[All Products].[Food].[Baked Goods].[Bread],"
        + "         [Product].[All Products].[Food].[Baked Goods].[Bread].[Bagels],"
        + "         [Product].[All Products].[Food].[Baked Goods].[Bread].[Muffins]},"
        + "        \"**Subtotal - *\"),"
        + "[Measures].[Unit Sales] > 3400)} on rows "
        + "from [Sales]",

      // Note that [*Subtotal - Bread] still contains the
      // contribution of [Bagels] 815, which was filtered out.
      // in member with visual totals name is the same but caption is changed
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Product].[Food].[Baked Goods].[Bread]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Muffins]}\n"
        + "Row #0: 4,312\n"
        + "Row #1: 3,497\n" );
  }

  @Disabled //disabled for CI build
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVisualTotalsNested(Context context) {
    assertQueryReturns(context.getConnection(),
      "select {[Measures].[Unit Sales]} on columns, "
        + "{VisualTotals("
        + "    Filter("
        + "        VisualTotals("
        + "            {[Product].[All Products].[Food].[Baked Goods].[Bread],"
        + "             [Product].[All Products].[Food].[Baked Goods].[Bread].[Bagels],"
        + "             [Product].[All Products].[Food].[Baked Goods].[Bread].[Muffins]},"
        + "            \"**Subtotal - *\"),"
        + "    [Measures].[Unit Sales] > 3400),"
        + "    \"Second total - *\")} on rows "
        + "from [Sales]",

      // Yields the same -- no extra total.
      // in member with visual totals name is the same but caption is changed
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Product].[Food].[Baked Goods].[Bread]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Muffins]}\n"
        + "Row #0: 4,312\n"
        + "Row #1: 3,497\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVisualTotalsFilterInside(Context context) {
    assertQueryReturns(context.getConnection(),
      "select {[Measures].[Unit Sales]} on columns, "
        + "{VisualTotals("
        + "    Filter("
        + "        {[Product].[All Products].[Food].[Baked Goods].[Bread],"
        + "         [Product].[All Products].[Food].[Baked Goods].[Bread].[Bagels],"
        + "         [Product].[All Products].[Food].[Baked Goods].[Bread].[Muffins]},"
        + "        [Measures].[Unit Sales] > 3400),"
        + "    \"**Subtotal - *\")} on rows "
        + "from [Sales]",

      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Product].[Food].[Baked Goods].[Bread]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Muffins]}\n"
        + "Row #0: 3,497\n"
        + "Row #1: 3,497\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVisualTotalsOutOfOrder(Context context) {
    assertQueryReturns(context.getConnection(),
      "select {[Measures].[Unit Sales]} on columns, "
        + "{VisualTotals("
        + "    {[Product].[All Products].[Food].[Baked Goods].[Bread].[Bagels],"
        + "     [Product].[All Products].[Food].[Baked Goods].[Bread],"
        + "     [Product].[All Products].[Food].[Baked Goods].[Bread].[Muffins]},"
        + "    \"**Subtotal - *\")} on rows "
        + "from [Sales]",

      // Note that [*Subtotal - Bread] 3497 does not include 815 for
      // bagels.
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Bagels]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Muffins]}\n"
        + "Row #0: 815\n"
        + "Row #1: 3,497\n"
        + "Row #2: 3,497\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVisualTotalsGrandparentsAndOutOfOrder(Context context) {
    assertQueryReturns(context.getConnection(),
      "select {[Measures].[Unit Sales]} on columns, "
        + "{VisualTotals("
        + "    {[Product].[All Products].[Food],"
        + "     [Product].[All Products].[Food].[Baked Goods].[Bread],"
        + "     [Product].[All Products].[Food].[Baked Goods].[Bread].[Bagels],"
        + "     [Product].[All Products].[Food].[Frozen Foods].[Breakfast Foods],"
        + "     [Product].[All Products].[Food].[Frozen Foods].[Breakfast Foods].[Pancake Mix].[Golden],"
        + "     [Product].[All Products].[Food].[Frozen Foods].[Breakfast Foods].[Pancake Mix].[Big Time],"
        + "     [Product].[All Products].[Food].[Baked Goods].[Bread].[Muffins]},"
        + "    \"**Subtotal - *\")} on rows "
        + "from [Sales]",

      // Note:
      // [*Subtotal - Food]  = 4513 = 815 + 311 + 3497
      // [*Subtotal - Bread] = 815, does not include muffins
      // [*Subtotal - Breakfast Foods] = 311 = 110 + 201, includes
      //     grandchildren
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Product].[Food]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Bagels]}\n"
        + "{[Product].[Food].[Frozen Foods].[Breakfast Foods]}\n"
        + "{[Product].[Food].[Frozen Foods].[Breakfast Foods].[Pancake Mix].[Golden]}\n"
        + "{[Product].[Food].[Frozen Foods].[Breakfast Foods].[Pancake Mix].[Big Time]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Muffins]}\n"
        + "Row #0: 4,623\n"
        + "Row #1: 815\n"
        + "Row #2: 815\n"
        + "Row #3: 311\n"
        + "Row #4: 110\n"
        + "Row #5: 201\n"
        + "Row #6: 3,497\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVisualTotalsCrossjoin(Context context) {
    assertAxisThrows(context.getConnection(),
      "VisualTotals(Crossjoin([Gender].Members, [Store].children))",
      "Argument to 'VisualTotals' function must be a set of members; got set of tuples." );
  }

  /**
   * Test case for bug
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-615">MONDRIAN-615</a>,
   * "VisualTotals doesn't work for the all member".
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVisualTotalsAll(Context context) {
    final String query =
      "SELECT \n"
        + "  {[Measures].[Unit Sales]} ON 0, \n"
        + "  VisualTotals(\n"
        + "    {[Customers].[All Customers],\n"
        + "     [Customers].[USA],\n"
        + "     [Customers].[USA].[CA],\n"
        + "     [Customers].[USA].[OR]}) ON 1\n"
        + "FROM [Sales]";
    assertQueryReturns(context.getConnection(),
      query,
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Customers].[All Customers]}\n"
        + "{[Customers].[USA]}\n"
        + "{[Customers].[USA].[CA]}\n"
        + "{[Customers].[USA].[OR]}\n"
        + "Row #0: 142,407\n"
        + "Row #1: 142,407\n"
        + "Row #2: 74,748\n"
        + "Row #3: 67,659\n" );

    // Check captions
    final Result result = executeQuery(context.getConnection(), query);
    final List<Position> positionList = result.getAxes()[ 1 ].getPositions();
    assertEquals( "All Customers", positionList.get( 0 ).get( 0 ).getCaption() );
    assertEquals( "USA", positionList.get( 1 ).get( 0 ).getCaption() );
    assertEquals( "CA", positionList.get( 2 ).get( 0 ).getCaption() );
  }

  /**
   * Test case involving a named set and query pivoted. Suggested in
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-615">MONDRIAN-615</a>,
   * "VisualTotals doesn't work for the all member".
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVisualTotalsWithNamedSetAndPivot(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH SET [CA_OR] AS\n"
        + "    VisualTotals(\n"
        + "        {[Customers].[All Customers],\n"
        + "         [Customers].[USA],\n"
        + "         [Customers].[USA].[CA],\n"
        + "         [Customers].[USA].[OR]})\n"
        + "SELECT \n"
        + "    Drilldownlevel({[Time].[1997]}) ON 0, \n"
        + "    [CA_OR] ON 1 \n"
        + "FROM [Sales] ",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Time].[1997]}\n"
        + "{[Time].[1997].[Q1]}\n"
        + "{[Time].[1997].[Q2]}\n"
        + "{[Time].[1997].[Q3]}\n"
        + "{[Time].[1997].[Q4]}\n"
        + "Axis #2:\n"
        + "{[Customers].[All Customers]}\n"
        + "{[Customers].[USA]}\n"
        + "{[Customers].[USA].[CA]}\n"
        + "{[Customers].[USA].[OR]}\n"
        + "Row #0: 142,407\n"
        + "Row #0: 36,177\n"
        + "Row #0: 33,131\n"
        + "Row #0: 35,310\n"
        + "Row #0: 37,789\n"
        + "Row #1: 142,407\n"
        + "Row #1: 36,177\n"
        + "Row #1: 33,131\n"
        + "Row #1: 35,310\n"
        + "Row #1: 37,789\n"
        + "Row #2: 74,748\n"
        + "Row #2: 16,890\n"
        + "Row #2: 18,052\n"
        + "Row #2: 18,370\n"
        + "Row #2: 21,436\n"
        + "Row #3: 67,659\n"
        + "Row #3: 19,287\n"
        + "Row #3: 15,079\n"
        + "Row #3: 16,940\n"
        + "Row #3: 16,353\n" );

    // same query, swap axes
    assertQueryReturns(context.getConnection(),
      "WITH SET [CA_OR] AS\n"
        + "    VisualTotals(\n"
        + "        {[Customers].[All Customers],\n"
        + "         [Customers].[USA],\n"
        + "         [Customers].[USA].[CA],\n"
        + "         [Customers].[USA].[OR]})\n"
        + "SELECT \n"
        + "    [CA_OR] ON 0,\n"
        + "    Drilldownlevel({[Time].[1997]}) ON 1\n"
        + "FROM [Sales] ",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[All Customers]}\n"
        + "{[Customers].[USA]}\n"
        + "{[Customers].[USA].[CA]}\n"
        + "{[Customers].[USA].[OR]}\n"
        + "Axis #2:\n"
        + "{[Time].[1997]}\n"
        + "{[Time].[1997].[Q1]}\n"
        + "{[Time].[1997].[Q2]}\n"
        + "{[Time].[1997].[Q3]}\n"
        + "{[Time].[1997].[Q4]}\n"
        + "Row #0: 142,407\n"
        + "Row #0: 142,407\n"
        + "Row #0: 74,748\n"
        + "Row #0: 67,659\n"
        + "Row #1: 36,177\n"
        + "Row #1: 36,177\n"
        + "Row #1: 16,890\n"
        + "Row #1: 19,287\n"
        + "Row #2: 33,131\n"
        + "Row #2: 33,131\n"
        + "Row #2: 18,052\n"
        + "Row #2: 15,079\n"
        + "Row #3: 35,310\n"
        + "Row #3: 35,310\n"
        + "Row #3: 18,370\n"
        + "Row #3: 16,940\n"
        + "Row #4: 37,789\n"
        + "Row #4: 37,789\n"
        + "Row #4: 21,436\n"
        + "Row #4: 16,353\n" );
  }

  /**
   * Tests that members generated by VisualTotals have correct identity.
   *
   * <p>Testcase for <a href="http://jira.pentaho.com/browse/MONDRIAN-295">
   * bug MONDRIAN-295, "Query generated by Excel 2007 gives incorrect results"</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVisualTotalsIntersect(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH\n"
        + "SET [XL_Row_Dim_0] AS 'VisualTotals(Distinct(Hierarchize({Ascendants([Customers].[All Customers].[USA]), "
        + "Descendants([Customers].[All Customers].[USA])})))' \n"
        + "SELECT \n"
        + "NON EMPTY Hierarchize({[Time].[Year].members}) ON COLUMNS , \n"
        + "NON EMPTY Hierarchize(Intersect({DrilldownLevel({[Customers].[All Customers]})}, [XL_Row_Dim_0])) ON "
        + "ROWS \n"
        + "FROM [Sales] \n"
        + "WHERE ([Measures].[Store Sales])",
      "Axis #0:\n"
        + "{[Measures].[Store Sales]}\n"
        + "Axis #1:\n"
        + "{[Time].[1997]}\n"
        + "Axis #2:\n"
        + "{[Customers].[All Customers]}\n"
        + "{[Customers].[USA]}\n"
        + "Row #0: 565,238.13\n"
        + "Row #1: 565,238.13\n" );
  }

  /**
   * <p>Testcase for <a href="http://jira.pentaho.com/browse/MONDRIAN-668">
   * bug MONDRIAN-668, "Intersect should return any VisualTotals members in right-hand set"</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVisualTotalsWithNamedSetAndPivotSameAxis(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH SET [XL_Row_Dim_0] AS\n"
        + " VisualTotals(\n"
        + "   Distinct(\n"
        + "     Hierarchize(\n"
        + "       {Ascendants([Store].[USA].[CA]),\n"
        + "        Descendants([Store].[USA].[CA])})))\n"
        + "select NON EMPTY \n"
        + "  Hierarchize(\n"
        + "    Intersect(\n"
        + "      {DrilldownLevel({[Store].[USA]})},\n"
        + "      [XL_Row_Dim_0])) ON COLUMNS\n"
        + "from [Sales] "
        + "where [Measures].[Sales count]\n",
      "Axis #0:\n"
        + "{[Measures].[Sales Count]}\n"
        + "Axis #1:\n"
        + "{[Store].[USA]}\n"
        + "{[Store].[USA].[CA]}\n"
        + "Row #0: 24,442\n"
        + "Row #0: 24,442\n" );

    // now with tuples
    assertQueryReturns(context.getConnection(),
      "WITH SET [XL_Row_Dim_0] AS\n"
        + " VisualTotals(\n"
        + "   Distinct(\n"
        + "     Hierarchize(\n"
        + "       {Ascendants([Store].[USA].[CA]),\n"
        + "        Descendants([Store].[USA].[CA])})))\n"
        + "select NON EMPTY \n"
        + "  Hierarchize(\n"
        + "    Intersect(\n"
        + "     [Marital Status].[M]\n"
        + "     * {DrilldownLevel({[Store].[USA]})}\n"
        + "     * [Gender].[F],\n"
        + "     [Marital Status].[M]\n"
        + "     * [XL_Row_Dim_0]\n"
        + "     * [Gender].[F])) ON COLUMNS\n"
        + "from [Sales] "
        + "where [Measures].[Sales count]\n",
      "Axis #0:\n"
        + "{[Measures].[Sales Count]}\n"
        + "Axis #1:\n"
        + "{[Marital Status].[M], [Store].[USA], [Gender].[F]}\n"
        + "{[Marital Status].[M], [Store].[USA].[CA], [Gender].[F]}\n"
        + "Row #0: 6,054\n"
        + "Row #0: 6,054\n" );
  }

  /**
   * <p>Testcase for <a href="http://jira.pentaho.com/browse/MONDRIAN-682">
   * bug MONDRIAN-682, "VisualTotals + Distinct-count measure gives wrong results"</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVisualTotalsDistinctCountMeasure(Context context) {
    // distinct measure
    assertQueryReturns(context.getConnection(),
      "WITH SET [XL_Row_Dim_0] AS\n"
        + " VisualTotals(\n"
        + "   Distinct(\n"
        + "     Hierarchize(\n"
        + "       {Ascendants([Store].[USA].[CA]),\n"
        + "        Descendants([Store].[USA].[CA])})))\n"
        + "select NON EMPTY \n"
        + "  Hierarchize(\n"
        + "    Intersect(\n"
        + "      {DrilldownLevel({[Store].[All Stores]})},\n"
        + "      [XL_Row_Dim_0])) ON COLUMNS\n"
        + "from [HR] "
        + "where [Measures].[Number of Employees]\n",
      "Axis #0:\n"
        + "{[Measures].[Number of Employees]}\n"
        + "Axis #1:\n"
        + "{[Store].[All Stores]}\n"
        + "{[Store].[USA]}\n"
        + "Row #0: 193\n"
        + "Row #0: 193\n" );

    // distinct measure
    assertQueryReturns(context.getConnection(),
      "WITH SET [XL_Row_Dim_0] AS\n"
        + " VisualTotals(\n"
        + "   Distinct(\n"
        + "     Hierarchize(\n"
        + "       {Ascendants([Store].[USA].[CA].[Beverly Hills]),\n"
        + "        Descendants([Store].[USA].[CA].[Beverly Hills]),\n"
        + "        Ascendants([Store].[USA].[CA].[Los Angeles]),\n"
        + "        Descendants([Store].[USA].[CA].[Los Angeles])})))"
        + "select NON EMPTY \n"
        + "  Hierarchize(\n"
        + "    Intersect(\n"
        + "      {DrilldownLevel({[Store].[All Stores]})},\n"
        + "      [XL_Row_Dim_0])) ON COLUMNS\n"
        + "from [HR] "
        + "where [Measures].[Number of Employees]\n",
      "Axis #0:\n"
        + "{[Measures].[Number of Employees]}\n"
        + "Axis #1:\n"
        + "{[Store].[All Stores]}\n"
        + "{[Store].[USA]}\n"
        + "Row #0: 110\n"
        + "Row #0: 110\n" );

    // distinct measure on columns
    assertQueryReturns(context.getConnection(),
      "WITH SET [XL_Row_Dim_0] AS\n"
        + " VisualTotals(\n"
        + "   Distinct(\n"
        + "     Hierarchize(\n"
        + "       {Ascendants([Store].[USA].[CA]),\n"
        + "        Descendants([Store].[USA].[CA])})))\n"
        + "select {[Measures].[Count], [Measures].[Number of Employees]} on COLUMNS,"
        + " NON EMPTY \n"
        + "  Hierarchize(\n"
        + "    Intersect(\n"
        + "      {DrilldownLevel({[Store].[All Stores]})},\n"
        + "      [XL_Row_Dim_0])) ON ROWS\n"
        + "from [HR] ",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Count]}\n"
        + "{[Measures].[Number of Employees]}\n"
        + "Axis #2:\n"
        + "{[Store].[All Stores]}\n"
        + "{[Store].[USA]}\n"
        + "Row #0: 2,316\n"
        + "Row #0: 193\n"
        + "Row #1: 2,316\n"
        + "Row #1: 193\n" );

    // distinct measure with tuples
    assertQueryReturns(context.getConnection(),
      "WITH SET [XL_Row_Dim_0] AS\n"
        + " VisualTotals(\n"
        + "   Distinct(\n"
        + "     Hierarchize(\n"
        + "       {Ascendants([Store].[USA].[CA]),\n"
        + "        Descendants([Store].[USA].[CA])})))\n"
        + "select NON EMPTY \n"
        + "  Hierarchize(\n"
        + "    Intersect(\n"
        + "     [Marital Status].[M]\n"
        + "     * {DrilldownLevel({[Store].[USA]})}\n"
        + "     * [Gender].[F],\n"
        + "     [Marital Status].[M]\n"
        + "     * [XL_Row_Dim_0]\n"
        + "     * [Gender].[F])) ON COLUMNS\n"
        + "from [Sales] "
        + "where [Measures].[Customer count]\n",
      "Axis #0:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Axis #1:\n"
        + "{[Marital Status].[M], [Store].[USA], [Gender].[F]}\n"
        + "{[Marital Status].[M], [Store].[USA].[CA], [Gender].[F]}\n"
        + "Row #0: 654\n"
        + "Row #0: 654\n" );
  }

  /**
   * <p>Testcase for <a href="http://jira.pentaho.com/browse/MONDRIAN-761">
   * bug MONDRIAN-761, "VisualTotalMember cannot be cast to RolapCubeMember"</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVisualTotalsClassCast(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH  SET [XL_Row_Dim_0] AS\n"
        + " VisualTotals(\n"
        + "   Distinct(\n"
        + "     Hierarchize(\n"
        + "       {Ascendants([Store].[USA].[WA].[Yakima]), \n"
        + "        Descendants([Store].[USA].[WA].[Yakima]), \n"
        + "        Ascendants([Store].[USA].[WA].[Walla Walla]), \n"
        + "        Descendants([Store].[USA].[WA].[Walla Walla]), \n"
        + "        Ascendants([Store].[USA].[WA].[Tacoma]), \n"
        + "        Descendants([Store].[USA].[WA].[Tacoma]), \n"
        + "        Ascendants([Store].[USA].[WA].[Spokane]), \n"
        + "        Descendants([Store].[USA].[WA].[Spokane]), \n"
        + "        Ascendants([Store].[USA].[WA].[Seattle]), \n"
        + "        Descendants([Store].[USA].[WA].[Seattle]), \n"
        + "        Ascendants([Store].[USA].[WA].[Bremerton]), \n"
        + "        Descendants([Store].[USA].[WA].[Bremerton]), \n"
        + "        Ascendants([Store].[USA].[OR]), \n"
        + "        Descendants([Store].[USA].[OR])}))) \n"
        + " SELECT NON EMPTY \n"
        + " Hierarchize(\n"
        + "   Intersect(\n"
        + "     DrilldownMember(\n"
        + "       {{DrilldownMember(\n"
        + "         {{DrilldownMember(\n"
        + "           {{DrilldownLevel(\n"
        + "             {[Store].[All Stores]})}},\n"
        + "           {[Store].[USA]})}},\n"
        + "         {[Store].[USA].[WA]})}},\n"
        + "       {[Store].[USA].[WA].[Bremerton]}),\n"
        + "       [XL_Row_Dim_0]))\n"
        + "DIMENSION PROPERTIES \n"
        + "  PARENT_UNIQUE_NAME, \n"
        + "  [Store].[Store Name].[Store Type],\n"
        + "  [Store].[Store Name].[Store Manager],\n"
        + "  [Store].[Store Name].[Store Sqft],\n"
        + "  [Store].[Store Name].[Grocery Sqft],\n"
        + "  [Store].[Store Name].[Frozen Sqft],\n"
        + "  [Store].[Store Name].[Meat Sqft],\n"
        + "  [Store].[Store Name].[Has coffee bar],\n"
        + "  [Store].[Store Name].[Street address] ON COLUMNS \n"
        + "FROM [HR]\n"
        + "WHERE \n"
        + "  ([Measures].[Number of Employees])\n"
        + "CELL PROPERTIES\n"
        + "  VALUE,\n"
        + "  FORMAT_STRING,\n"
        + "  LANGUAGE,\n"
        + "  BACK_COLOR,\n"
        + "  FORE_COLOR,\n"
        + "  FONT_FLAGS",
      "Axis #0:\n"
        + "{[Measures].[Number of Employees]}\n"
        + "Axis #1:\n"
        + "{[Store].[All Stores]}\n"
        + "{[Store].[USA]}\n"
        + "{[Store].[USA].[OR]}\n"
        + "{[Store].[USA].[WA]}\n"
        + "{[Store].[USA].[WA].[Bremerton]}\n"
        + "{[Store].[USA].[WA].[Bremerton].[Store 3]}\n"
        + "{[Store].[USA].[WA].[Seattle]}\n"
        + "{[Store].[USA].[WA].[Spokane]}\n"
        + "{[Store].[USA].[WA].[Tacoma]}\n"
        + "{[Store].[USA].[WA].[Walla Walla]}\n"
        + "{[Store].[USA].[WA].[Yakima]}\n"
        + "Row #0: 419\n"
        + "Row #0: 419\n"
        + "Row #0: 136\n"
        + "Row #0: 283\n"
        + "Row #0: 62\n"
        + "Row #0: 62\n"
        + "Row #0: 62\n"
        + "Row #0: 62\n"
        + "Row #0: 74\n"
        + "Row #0: 4\n"
        + "Row #0: 19\n" );
  }

  /**
   * <p>Testcase for <a href="http://jira.pentaho.com/browse/MONDRIAN-678">
   * bug MONDRIAN-678, "VisualTotals gives UnsupportedOperationException calling getOrdinal"</a>. Key difference from
   * previous test is that there are multiple hierarchies in Named set.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVisualTotalsWithNamedSetOfTuples(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH SET [XL_Row_Dim_0] AS\n"
        + " VisualTotals(\n"
        + "   Distinct(\n"
        + "     Hierarchize(\n"
        + "       {Ascendants([Customers].[All Customers].[USA].[CA].[Beverly Hills].[Ari Tweten]),\n"
        + "        Descendants([Customers].[All Customers].[USA].[CA].[Beverly Hills].[Ari Tweten]),\n"
        + "        Ascendants([Customers].[All Customers].[Mexico]),\n"
        + "        Descendants([Customers].[All Customers].[Mexico])})))\n"
        + "select NON EMPTY \n"
        + "  Hierarchize(\n"
        + "    Intersect(\n"
        + "      (DrilldownMember(\n"
        + "        {{DrilldownMember(\n"
        + "          {{DrilldownLevel(\n"
        + "            {[Customers].[All Customers]})}},\n"
        + "          {[Customers].[All Customers].[USA]})}},\n"
        + "        {[Customers].[All Customers].[USA].[CA]})),\n"
        + "        [XL_Row_Dim_0])) ON COLUMNS\n"
        + "from [Sales]\n"
        + "where [Measures].[Sales count]\n",
      "Axis #0:\n"
        + "{[Measures].[Sales Count]}\n"
        + "Axis #1:\n"
        + "{[Customers].[All Customers]}\n"
        + "{[Customers].[USA]}\n"
        + "{[Customers].[USA].[CA]}\n"
        + "{[Customers].[USA].[CA].[Beverly Hills]}\n"
        + "Row #0: 4\n"
        + "Row #0: 4\n"
        + "Row #0: 4\n"
        + "Row #0: 4\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVisualTotalsLevel(Context context) {
    Result result = executeQuery(context.getConnection(),
      "select {[Measures].[Unit Sales]} on columns,\n"
        + "{[Product].[All Products],\n"
        + " [Product].[All Products].[Food].[Baked Goods].[Bread],\n"
        + " VisualTotals(\n"
        + "    {[Product].[All Products].[Food].[Baked Goods].[Bread],\n"
        + "     [Product].[All Products].[Food].[Baked Goods].[Bread].[Bagels],\n"
        + "     [Product].[All Products].[Food].[Baked Goods].[Bread].[Muffins]},\n"
        + "     \"**Subtotal - *\")} on rows\n"
        + "from [Sales]" );
    final List<Position> rowPos = result.getAxes()[ 1 ].getPositions();
    final Member member0 = rowPos.get( 0 ).get( 0 );
    assertEquals( "All Products", member0.getName() );
    assertEquals( "(All)", member0.getLevel().getName() );
    final Member member1 = rowPos.get( 1 ).get( 0 );
    assertEquals( "Bread", member1.getName() );
    assertEquals( "Product Category", member1.getLevel().getName() );
    final Member member2 = rowPos.get( 2 ).get( 0 );
    assertEquals( "Bread", member2.getName() );
    assertEquals( "*Subtotal - Bread", member2.getCaption() );
    assertEquals( "Product Category", member2.getLevel().getName() );
    final Member member3 = rowPos.get( 3 ).get( 0 );
    assertEquals( "Bagels", member3.getName() );
    assertEquals( "Product Subcategory", member3.getLevel().getName() );
    final Member member4 = rowPos.get( 4 ).get( 0 );
    assertEquals( "Muffins", member4.getName() );
    assertEquals( "Product Subcategory", member4.getLevel().getName() );
  }

  /**
   * Testcase for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-749"> MONDRIAN-749, "Cannot use visual totals
   * members in calculations"</a>.
   *
   * <p>The bug is not currently fixed, so it is a negative test case. Row #2
   * cell #1 contains an exception, but should be "**Subtotal - Bread : Product Subcategory".
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVisualTotalsMemberInCalculation(Context context) {
    assertQueryReturns(context.getConnection(),
      "with member [Measures].[Foo] as\n"
        + " [Product].CurrentMember.Name || ' : ' || [Product].Level.Name\n"
        + "select {[Measures].[Unit Sales], [Measures].[Foo]} on columns,\n"
        + "{[Product].[All Products],\n"
        + " [Product].[All Products].[Food].[Baked Goods].[Bread],\n"
        + " VisualTotals(\n"
        + "    {[Product].[All Products].[Food].[Baked Goods].[Bread],\n"
        + "     [Product].[All Products].[Food].[Baked Goods].[Bread].[Bagels],\n"
        + "     [Product].[All Products].[Food].[Baked Goods].[Bread].[Muffins]},\n"
        + "     \"**Subtotal - *\")} on rows\n"
        + "from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Foo]}\n"
        + "Axis #2:\n"
        + "{[Product].[All Products]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Bagels]}\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Muffins]}\n"
        + "Row #0: 266,773\n"
        + "Row #0: All Products : (All)\n"
        + "Row #1: 7,870\n"
        + "Row #1: Bread : Product Category\n"
        + "Row #2: 4,312\n"
        + "Row #2: #ERR: mondrian.olap.fun.MondrianEvaluationException: Could not find an aggregator in the current "
        + "evaluation context\n"
        + "Row #3: 815\n"
        + "Row #3: Bagels : Product Subcategory\n"
        + "Row #4: 3,497\n"
        + "Row #4: Muffins : Product Subcategory\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCalculatedChild(Context context) {
    // Construct calculated children with the same name for both [Drink] and
    // [Non-Consumable].  Then, create a metric to select the calculated
    // child based on current product member.
    assertQueryReturns(context.getConnection(),
      "with\n"
        + " member [Product].[All Products].[Drink].[Calculated Child] as '[Product].[All Products].[Drink]"
        + ".[Alcoholic Beverages]'\n"
        + " member [Product].[All Products].[Non-Consumable].[Calculated Child] as '[Product].[All Products]"
        + ".[Non-Consumable].[Carousel]'\n"
        + " member [Measures].[Unit Sales CC] as '([Measures].[Unit Sales],[Product].currentmember.CalculatedChild"
        + "(\"Calculated Child\"))'\n"
        + " select non empty {[Measures].[Unit Sales CC]} on columns,\n"
        + " non empty {[Product].[Drink], [Product].[Non-Consumable]} on rows\n"
        + " from [Sales]",

      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales CC]}\n"
        + "Axis #2:\n"
        + "{[Product].[Drink]}\n"
        + "{[Product].[Non-Consumable]}\n"
        + "Row #0: 6,838\n" // Calculated child for [Drink]
        + "Row #1: 841\n" ); // Calculated child for [Non-Consumable]
    Member member = executeSingletonAxis(context.getConnection(),
      "[Product].[All Products].CalculatedChild(\"foobar\")" );
    assertEquals( null, member );
  }

  @Disabled //disabled for CI build
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCalculatedChildUsingItem(Context context) {
    // Construct calculated children with the same name for both [Drink] and
    // [Non-Consumable].  Then, create a metric to select the first
    // calculated child.
    assertQueryReturns(context.getConnection(),
      "with\n"
        + " member [Product].[All Products].[Drink].[Calculated Child] as '[Product].[All Products].[Drink]"
        + ".[Alcoholic Beverages]'\n"
        + " member [Product].[All Products].[Non-Consumable].[Calculated Child] as '[Product].[All Products]"
        + ".[Non-Consumable].[Carousel]'\n"
        + " member [Measures].[Unit Sales CC] as '([Measures].[Unit Sales],AddCalculatedMembers([Product]"
        + ".currentmember.children).Item(\"Calculated Child\"))'\n"
        + " select non empty {[Measures].[Unit Sales CC]} on columns,\n"
        + " non empty {[Product].[Drink], [Product].[Non-Consumable]} on rows\n"
        + " from [Sales]",

      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales CC]}\n"
        + "Axis #2:\n"
        + "{[Product].[Drink]}\n"
        + "{[Product].[Non-Consumable]}\n"
        + "Row #0: 6,838\n"
        // Note: For [Non-Consumable], the calculated child for [Drink] was
        // selected!
        + "Row #1: 6,838\n" );
    Member member = executeSingletonAxis(context.getConnection(),
      "[Product].[All Products].CalculatedChild(\"foobar\")" );
    assertEquals( null, member );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCalculatedChildOnMemberWithNoChildren(Context context) {
    Member member =
      executeSingletonAxis(context.getConnection(),
        "[Measures].[Store Sales].CalculatedChild(\"foobar\")" );
    assertEquals( null, member );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCalculatedChildOnNullMember(Context context) {
    Member member =
      executeSingletonAxis(context.getConnection(),
        "[Measures].[Store Sales].parent.CalculatedChild(\"foobar\")" );
    assertEquals( null, member);
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCast(Context context) {
    // NOTE: Some of these tests fail with 'cannot convert ...', and they
    // probably shouldn't. Feel free to fix the conversion.
    // -- jhyde, 2006/9/3

    // From double to integer.  MONDRIAN-1631
    Cell cell = executeExprRaw(context.getConnection(), "Cast(1.4 As Integer)" );
    assertEquals(Integer.class, cell.getValue().getClass(),
            "Cast to Integer resulted in wrong datatype\n"
                    + cell.getValue().getClass().toString());
    assertEquals(1, cell.getValue() );

    // From integer
    // To integer (trivial)
    assertExprReturns(context.getConnection(), "0 + Cast(1 + 2 AS Integer)", "3" );
    // To String
    assertExprReturns(context.getConnection(), "'' || Cast(1 + 2 AS String)", "3.0" );
    // To Boolean
    assertExprReturns(context.getConnection(), "1=1 AND Cast(1 + 2 AS Boolean)", "true" );
    assertExprReturns(context.getConnection(), "1=1 AND Cast(1 - 1 AS Boolean)", "false" );


    // From boolean
    // To String
    assertExprReturns(context.getConnection(), "'' || Cast((1 = 1 AND 1 = 2) AS String)", "false" );

    // This case demonstrates the relative precedence of 'AS' in 'CAST'
    // and 'AS' for creating inline named sets. See also bug MONDRIAN-648.
//    discard( Bug.BugMondrian648Fixed );
    assertExprReturns(context.getConnection(),
      "'xxx' || Cast(1 = 1 AND 1 = 2 AS String)",
      "xxxfalse" );

    // To boolean (trivial)
    assertExprReturns(context.getConnection(),
      "1=1 AND Cast((1 = 1 AND 1 = 2) AS Boolean)",
      "false" );

    assertExprReturns(context.getConnection(),
      "1=1 OR Cast(1 = 1 AND 1 = 2 AS Boolean)",
      "true" );

    // From null : should not throw exceptions since RolapResult.executeBody
    // can receive NULL values when the cell value is not loaded yet, so
    // should return null instead.
    // To Integer : Expect to return NULL

    // Expect to return NULL
    assertExprReturns(context.getConnection(), "0 * Cast(NULL AS Integer)", "" );

    // To Numeric : Expect to return NULL
    // Expect to return NULL
    assertExprReturns(context.getConnection(), "0 * Cast(NULL AS Numeric)", "" );

    // To String : Expect to return "null"
    assertExprReturns(context.getConnection(), "'' || Cast(NULL AS String)", "null" );

    // To Boolean : Expect to return NULL, but since FunUtil.BooleanNull
    // does not implement three-valued boolean logic yet, this will return
    // false
    assertExprReturns(context.getConnection(), "1=1 AND Cast(NULL AS Boolean)", "false" );

    // Double is not allowed as a type
    assertExprThrows(context.getConnection(),
      "Cast(1 AS Double)",
      "Unknown type 'Double'; values are NUMERIC, STRING, BOOLEAN" );

    // An integer constant is not allowed as a type
    assertExprThrows(context.getConnection(),
      "Cast(1 AS 5)",
      "Encountered an error at (or somewhere around) input:1:11" );

    assertExprReturns(context.getConnection(), "Cast('tr' || 'ue' AS boolean)", "true" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCastAndNull(Context context) {
	    // To Boolean : Expect to return NULL, but since FunUtil.BooleanNull
	    // does not implement three-valued boolean logic yet, this will return
	    // false
	    assertExprReturns(context.getConnection(), "1=1 AND Cast(NULL AS Boolean)", "false" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCastNull(Context context) {
	    // To Boolean : Expect to return NULL, but since FunUtil.BooleanNull
	    // does not implement three-valued boolean logic yet, this will return
	    // false
	    assertExprReturns(context.getConnection(), "Cast(NULL AS Boolean)", "false" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBool1(Context context) {

	    assertExprReturns(context.getConnection(), "1=1 AND 1=0", "false" );


  }
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBool2(Context context) {

	    assertExprReturns(context.getConnection(), "1=1 AND 1=1", "true" );


  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBool3(Context context) {

	    assertExprReturns(context.getConnection(), "1=1 AND null", "false" );


  }
  /**
   * Testcase for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-524"> MONDRIAN-524, "VB functions: expected
   * primitive type, got java.lang.Object"</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCastBug524(Context context) {
    assertExprReturns(context.getConnection(),
      "Cast(Int([Measures].[Store Sales] / 3600) as String)",
      "157" );
  }

  /**
   * Tests {@link org.eclipse.daanse.olap.api.function.FunctionTable#getFunctionInfos()}, but more importantly, generates an HTML table of all
   * implemented functions into a file called "functions.html". You can manually include that table in the <a
   * href="{@docRoot}/../mdx.html">MDX specification</a>.
   */
  @Test
  void testDumpFunctions() throws IOException {
    final List<FunctionMetaData> functionMetaDatas = new ArrayList<>(BuiltinFunTable.instance().getFunctionMetaDatas());
    assertEquals( NUM_EXPECTED_FUNCTIONS, functionMetaDatas.size() );

    // Add some UDFs.
//    functionMetaDatas.add(
//      new FunInfo(
//        new UdfResolver(
//          new UdfResolver.ClassUdfFactory(
//            CurrentDateMemberExactUdf.class,
//            null ) ) ) );
//    functionMetaDatas.add(
//      new FunInfo(
//        new UdfResolver(
//          new UdfResolver.ClassUdfFactory(
//            CurrentDateMemberUdf.class,
//            null ) ) ) );
//    functionMetaDatas.add(
//      new FunInfo(
//        new UdfResolver(
//          new UdfResolver.ClassUdfFactory(
//            CurrentDateStringUdf.class,
//            null ) ) ) );
//    Collections.sort( functionMetaDatas );

    final File file = new File( "functions.html" );
    final FileOutputStream os = new FileOutputStream( file );
    final PrintWriter pw = new PrintWriter( os );
    pw.println( "<table border='1'>" );
    pw.println( "<tr>" );
    pw.println( "<td><b>Name</b></td>" );
    pw.println( "<td><b>Description</b></td>" );
    pw.println( "</tr>" );
    for ( FunctionMetaData funInfo : functionMetaDatas ) {
      pw.println( "<tr>" );
      pw.print( "  <td valign=top><code>" );
      printHtml( pw, funInfo.operationAtom().name() );
      pw.println( "</code></td>" );
      pw.print( "  <td>" );
      if ( funInfo.description() != null ) {
        printHtml( pw, funInfo.description() );
      }
      pw.println();
      final String signature = funInfo.signature();
      if ( signature != null ) {
			pw.println("    <h1>Syntax</h1>");

			pw.print("    ");
			printHtml(pw, signature);
			pw.println();
      }
      pw.println( "  </td>" );
      pw.println( "</tr>" );
    }
    pw.println( "</table>" );
    pw.close();
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexOrExpr(Context context) {
    Connection connection = context.getConnection();
    switch (getDatabaseProduct(TestUtil.getDialect(connection).getDialectName())) {
      case INFOBRIGHT:
        // Skip this test on Infobright, because [Promotion Sales] is
        // defined wrong.
        return;
    }

    // make sure all aggregates referenced in the OR expression are
    // processed in a single load request by setting the eval depth to
    // a value smaller than the number of measures
    int origDepth = context.getConfig().maxEvalDepth();
    ((TestConfig)context.getConfig()).setMaxEvalDepth( 3 );
    assertQueryReturns(connection,
      "with set [*NATIVE_CJ_SET] as '[Store].[Store Country].members' "
        + "set [*GENERATED_MEMBERS_Measures] as "
        + "    '{[Measures].[Unit Sales], [Measures].[Store Cost], "
        + "    [Measures].[Sales Count], [Measures].[Customer Count], "
        + "    [Measures].[Promotion Sales]}' "
        + "set [*GENERATED_MEMBERS] as "
        + "    'Generate([*NATIVE_CJ_SET], {[Store].CurrentMember})' "
        + "member [Store].[*SUBTOTAL_MEMBER_SEL~SUM] as 'Sum([*GENERATED_MEMBERS])' "
        + "select [*GENERATED_MEMBERS_Measures] ON COLUMNS, "
        + "NON EMPTY "
        + "    Filter("
        + "        Generate("
        + "        [*NATIVE_CJ_SET], "
        + "        {[Store].CurrentMember}), "
        + "        (((((NOT IsEmpty([Measures].[Unit Sales])) OR "
        + "            (NOT IsEmpty([Measures].[Store Cost]))) OR "
        + "            (NOT IsEmpty([Measures].[Sales Count]))) OR "
        + "            (NOT IsEmpty([Measures].[Customer Count]))) OR "
        + "            (NOT IsEmpty([Measures].[Promotion Sales])))) "
        + "on rows "
        + "from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Store Cost]}\n"
        + "{[Measures].[Sales Count]}\n"
        + "{[Measures].[Customer Count]}\n"
        + "{[Measures].[Promotion Sales]}\n"
        + "Axis #2:\n"
        + "{[Store].[USA]}\n"
        + "Row #0: 266,773\n"
        + "Row #0: 225,627.23\n"
        + "Row #0: 86,837\n"
        + "Row #0: 5,581\n"
        + "Row #0: 151,211.21\n" );
      ((TestConfig)context.getConfig()).setMaxEvalDepth( origDepth );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLeftFunctionWithValidArguments(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS,"
        + "Left([Store].CURRENTMEMBER.Name, 4)=\"Bell\") on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLeftFunctionWithLengthValueZero(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS,"
        + "Left([Store].CURRENTMEMBER.Name, 0)=\"\" And "
        + "[Store].CURRENTMEMBER.Name = \"Bellingham\") on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLeftFunctionWithLengthValueEqualToStringLength(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS,"
        + "Left([Store].CURRENTMEMBER.Name, 10)=\"Bellingham\") "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLeftFunctionWithLengthMoreThanStringLength(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS,"
        + "Left([Store].CURRENTMEMBER.Name, 20)=\"Bellingham\") "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLeftFunctionWithZeroLengthString(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS,Left(\"\", 20)=\"\" "
        + "And [Store].CURRENTMEMBER.Name = \"Bellingham\") "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLeftFunctionWithNegativeLength(Context context) {
    assertQueryThrows(context,
      "select filter([Store].MEMBERS,"
        + "Left([Store].CURRENTMEMBER.Name, -20)=\"Bellingham\") "
        + "on 0 from sales",
      Util.IBM_JVM
        ? "StringIndexOutOfBoundsException: null"
        : "StringIndexOutOfBoundsException: Range [0, -20) out of bounds for length 10" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMidFunctionWithValidArguments(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS,"
        + "[Store].CURRENTMEMBER.Name = \"Bellingham\""
        + "And Mid(\"Bellingham\", 4, 6) = \"lingha\")"
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMidFunctionWithZeroLengthStringArgument(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS,"
        + "[Store].CURRENTMEMBER.Name = \"Bellingham\""
        + "And Mid(\"\", 4, 6) = \"\")"
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMidFunctionWithLengthArgumentLargerThanStringLength(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS,"
        + "[Store].CURRENTMEMBER.Name = \"Bellingham\""
        + "And Mid(\"Bellingham\", 4, 20) = \"lingham\")"
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMidFunctionWithStartIndexGreaterThanStringLength(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS,"
        + "[Store].CURRENTMEMBER.Name = \"Bellingham\""
        + "And Mid(\"Bellingham\", 20, 2) = \"\")"
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMidFunctionWithStartIndexZeroFails(Context context) {
    // Note: SSAS 2005 treats start<=0 as 1, therefore gives different
    // result for this query. We favor the VBA spec over SSAS 2005.
    if ( Bug.Ssas2005Compatible ) {
      assertQueryReturns(context.getConnection(),
        "select filter([Store].MEMBERS,"
          + "[Store].CURRENTMEMBER.Name = \"Bellingham\""
          + "And Mid(\"Bellingham\", 0, 2) = \"Be\")"
          + "on 0 from sales",
        "Axis #0:\n"
          + "{}\n"
          + "Axis #1:\n"
          + "{[Store].[USA].[WA].[Bellingham]}\n"
          + "Row #0: 2,237\n" );
    } else {
      assertQueryThrows(context,
        "select filter([Store].MEMBERS,"
          + "[Store].CURRENTMEMBER.Name = \"Bellingham\""
          + "And Mid(\"Bellingham\", 0, 2) = \"Be\")"
          + "on 0 from sales",
        "Invalid parameter. Start parameter of Mid function must be "
          + "positive" );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMidFunctionWithStartIndexOne(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS,"
        + "[Store].CURRENTMEMBER.Name = \"Bellingham\""
        + "And Mid(\"Bellingham\", 1, 2) = \"Be\")"
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMidFunctionWithNegativeStartIndex(Context context) {
    assertQueryThrows(context,
      "select filter([Store].MEMBERS,"
        + "[Store].CURRENTMEMBER.Name = \"Bellingham\""
        + "And Mid(\"Bellingham\", -20, 2) = \"\")"
        + "on 0 from sales",
      "Invalid parameter. "
        + "Start parameter of Mid function must be positive" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMidFunctionWithNegativeLength(Context context) {
    assertQueryThrows(context,
      "select filter([Store].MEMBERS,"
        + "[Store].CURRENTMEMBER.Name = \"Bellingham\""
        + "And Mid(\"Bellingham\", 2, -2) = \"\")"
        + "on 0 from sales",
      "Invalid parameter. "
        + "Length parameter of Mid function must be non-negative" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMidFunctionWithoutLength(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS,"
        + "[Store].CURRENTMEMBER.Name = \"Bellingham\""
        + "And Mid(\"Bellingham\", 2) = \"ellingham\")"
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLenFunctionWithNonEmptyString(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS, "
        + "Len([Store].CURRENTMEMBER.Name) = 3) on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA]}\n"
        + "Row #0: 266,773\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLenFunctionWithAnEmptyString(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS,Len(\"\")=0 "
        + "And [Store].CURRENTMEMBER.Name = \"Bellingham\") "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLenFunctionWithNullString(Context context) {
    // SSAS2005 returns 0
    assertQueryReturns(context.getConnection(),
      "with member [Measures].[Foo] as ' NULL '\n"
        + " member [Measures].[Bar] as ' len([Measures].[Foo]) '\n"
        + "select [Measures].[Bar] on 0\n"
        + "from [Warehouse and Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Bar]}\n"
        + "Row #0: 0\n" );
    // same, but inline
    assertExprReturns(context.getConnection(), "len(null)", 0, 0 );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testUCaseWithNonEmptyString(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS, "
        + " UCase([Store].CURRENTMEMBER.Name) = \"BELLINGHAM\") "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testUCaseWithEmptyString(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS, "
        + " UCase(\"\") = \"\" "
        + "And [Store].CURRENTMEMBER.Name = \"Bellingham\") "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testUCaseWithNullString(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS, "
        + " UCase(\"NULL\") = \"\" "
        + "And [Store].CURRENTMEMBER.Name = \"Bellingham\") "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testUCaseWithNull(Context context) {
    try {
      executeQuery(context.getConnection(),
        "select filter([Store].MEMBERS, "
          + " UCase(NULL) = \"\" "
          + "And [Store].CURRENTMEMBER.Name = \"Bellingham\") "
          + "on 0 from sales" );
    } catch ( MondrianException e ) {
      Throwable mondrianEvaluationException = e.getCause();
      assertEquals(
        mondrianEvaluationException.getClass(),
        ( MondrianEvaluationException.class ) );
      assertEquals( "No method with the signature UCase(NULL) matches known functions.",
          mondrianEvaluationException.getMessage() );
      return;
    }
    fail( "MondrianEvaluationException is expected here" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testInStrFunctionWithValidArguments(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS,InStr(\"Bellingham\", \"ingha\")=5 "
        + "And [Store].CURRENTMEMBER.Name = \"Bellingham\") "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIifFWithBooleanBooleanAndNumericParameterForReturningTruePart(Context context) {
    assertQueryReturns(context.getConnection(),
      "SELECT Filter(Store.allmembers, "
        + "iif(measures.profit < 400000,"
        + "[store].currentMember.NAME = \"USA\", 0)) on 0 FROM SALES",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA]}\n"
        + "Row #0: 266,773\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIifWithBooleanBooleanAndNumericParameterForReturningFalsePart(Context context) {
    assertQueryReturns(context.getConnection(),
      "SELECT Filter([Store].[USA].[CA].[Beverly Hills].children, "
        + "iif(measures.profit > 400000,"
        + "[store].currentMember.NAME = \"USA\", 1)) on 0 FROM SALES",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[CA].[Beverly Hills].[Store 6]}\n"
        + "Row #0: 21,333\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testIIFWithBooleanBooleanAndNumericParameterForReturningZero(Context context) {
    assertQueryReturns(context.getConnection(),
      "SELECT Filter(Store.allmembers, "
        + "iif(measures.profit > 400000,"
        + "[store].currentMember.NAME = \"USA\", 0)) on 0 FROM SALES",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testInStrFunctionWithEmptyString1(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS,InStr(\"\", \"ingha\")=0 "
        + "And [Store].CURRENTMEMBER.Name = \"Bellingham\") "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testInStrFunctionWithEmptyString2(Context context) {
    assertQueryReturns(context.getConnection(),
      "select filter([Store].MEMBERS,InStr(\"Bellingham\", \"\")=1 "
        + "And [Store].CURRENTMEMBER.Name = \"Bellingham\") "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGetCaptionUsingMemberDotCaption(Context context) {
    assertQueryReturns(context.getConnection(),
      "SELECT Filter(Store.allmembers, "
        + "[store].currentMember.caption = \"USA\") on 0 FROM SALES",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[USA]}\n"
        + "Row #0: 266,773\n" );
  }

  private static void printHtml( PrintWriter pw, String s ) {
    final String escaped = StringEscapeUtils.escapeHtml4(s);
    pw.print( escaped );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCache(Context context) {
    // test various data types: integer, string, member, set, tuple
    assertExprReturns(context.getConnection(), "Cache(1 + 2)", "3" );
    assertExprReturns(context.getConnection(), "Cache('foo' || 'bar')", "foobar" );
    assertAxisReturns(context.getConnection(),
      "[Gender].Children",
      "[Gender].[F]\n"
        + "[Gender].[M]" );
    assertAxisReturns(context.getConnection(),
      "([Gender].[M], [Marital Status].[S].PrevMember)",
      "{[Gender].[M], [Marital Status].[M]}" );

    // inside another expression
    assertAxisReturns(context.getConnection(),
      "Order(Cache([Gender].Children), Cache(([Measures].[Unit Sales], [Time].[1997].[Q1])), BDESC)",
      "[Gender].[M]\n"
        + "[Gender].[F]" );

    // doesn't work with multiple args
    assertExprThrows(context.getConnection(),
      "Cache(1, 2)",
      "No function matches signature 'Cache(<Numeric Expression>, <Numeric Expression>)'" );
  }

  // The following methods test VBA functions. They don't test all of them,
  // because the raw methods are tested in VbaTest, but they test the core
  // functionalities like error handling and operator overloading.

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVbaBasic(Context context) {
    // Exp is a simple function: one arg.
    assertExprReturns(context.getConnection(), "exp(0)", "1" );
    assertExprReturns(context.getConnection(), "exp(1)", Math.E, 0.00000001 );
    assertExprReturns(context.getConnection(), "exp(-2)", 1d / ( Math.E * Math.E ), 0.00000001 );

    }
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVbaBasic1(Context context) {
	  // If any arg is null, result is null.
	    assertExprReturns(context.getConnection(), "exp(null)", "" );

  }
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVbaBasic2(Context context) {
	  // If any arg is null, result is null.
	    assertExprReturns(context.getConnection(), "exp(cast(null as numeric))", "" );

  }

  // Test a VBA function with variable number of args.
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVbaOverloading(Context context) {
    assertExprReturns(context.getConnection(), "replace('xyzxyz', 'xy', 'a')", "azaz" );
    assertExprReturns(context.getConnection(), "replace('xyzxyz', 'xy', 'a', 2)", "xyzaz" );
    assertExprReturns(context.getConnection(), "replace('xyzxyz', 'xy', 'a', 1, 1)", "azxyz" );
  }

  // Test VBA exception handling
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVbaExceptions(Context context) {
    assertExprThrows(context.getConnection(),
      "right(\"abc\", -4)",
      Util.IBM_JVM
        ? "StringIndexOutOfBoundsException: null"
        : "StringIndexOutOfBoundsException: Range [7, 3) out of bounds for length 3");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVbaDateTime(Context context) {
    // function which returns date
    assertExprReturns(context.getConnection(),
      "Format(DateSerial(2006, 4, 29), \"Long Date\")",
      "Saturday, April 29, 2006" );
    // function with date parameter
    assertExprReturns(context.getConnection(), "Year(DateSerial(2006, 4, 29))", "2,006" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExcelPi(Context context) {
    // The PI function is defined in the Excel class.
    assertExprReturns(context.getConnection(), "Pi()", "3" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExcelPower(Context context) {
    assertExprReturns(context.getConnection(), "Power(8, 0.333333)", 2.0, 0.01 );
    assertExprReturns(context.getConnection(), "Power(-2, 0.5)", Double.NaN, 0.001 );
  }

  // Comment from the bug: the reason for this is that in AbstractExpCompiler
  // in the compileInteger method we are casting an IntegerCalc into a
  // DoubleCalc and there is no check for IntegerCalc in the NumericType
  // conditional path.
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBug1881739(Context context) {
    assertExprReturns(context.getConnection(), "LEFT(\"TEST\", LEN(\"TEST\"))", "TEST" );
  }

  /**
   * Testcase for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-296"> MONDRIAN-296, "Cube getTimeDimension use
   * when Cube has no Time dimension"</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCubeTimeDimensionFails(Context context) {
    assertQueryThrows(context.getConnection(),
      "select LastPeriods(1) on columns from [Store]",
      "'LastPeriods', no time dimension" );
    assertQueryThrows(context.getConnection(),
      "select OpeningPeriod() on columns from [Store]",
      "'OpeningPeriod', no time dimension" );
    assertQueryThrows(context.getConnection(),
      "select OpeningPeriod([Store Type]) on columns from [Store]",
      "'OpeningPeriod', no time dimension" );
    assertQueryThrows(context.getConnection(),
      "select ClosingPeriod() on columns from [Store]",
      "'ClosingPeriod', no time dimension" );
    assertQueryThrows(context.getConnection(),
      "select ClosingPeriod([Store Type]) on columns from [Store]",
      "'ClosingPeriod', no time dimension" );
    assertQueryThrows(context.getConnection(),
      "select ParallelPeriod() on columns from [Store]",
      "'ParallelPeriod', no time dimension" );
    assertQueryThrows(context.getConnection(),
      "select PeriodsToDate() on columns from [Store]",
      "'PeriodsToDate', no time dimension" );
    assertQueryThrows(context.getConnection(),
      "select Mtd() on columns from [Store]",
      "'Mtd', no time dimension" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFilterEmpty(Context context) {
    // Unlike "Descendants(<set>, ...)", we do not need to know the precise
    // type of the set, therefore it is OK if the set is empty.
    assertAxisReturns(context.getConnection(),
      "Filter({}, 1=0)",
      "" );
    assertAxisReturns(context.getConnection(),
      "Filter({[Time].[Time].Children}, 1=0)",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testFilterCalcSlicer(Context context) {
    assertQueryReturns(context.getConnection(),
      "with member [Time].[Time].[Date Range] as \n"
        + "'Aggregate({[Time].[1997].[Q1]:[Time].[1997].[Q3]})'\n"
        + "select\n"
        + "{[Measures].[Unit Sales],[Measures].[Store Cost],\n"
        + "[Measures].[Store Sales]} ON columns,\n"
        + "NON EMPTY Filter ([Store].[Store State].members,\n"
        + "[Measures].[Store Cost] > 75000) ON rows\n"
        + "from [Sales] where [Time].[Date Range]",
      "Axis #0:\n"
        + "{[Time].[Date Range]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Store Cost]}\n"
        + "{[Measures].[Store Sales]}\n"
        + "Axis #2:\n"
        + "{[Store].[USA].[WA]}\n"
        + "Row #0: 90,131\n"
        + "Row #0: 76,151.59\n"
        + "Row #0: 190,776.88\n" );
    assertQueryReturns(context.getConnection(),
      "with member [Time].[Time].[Date Range] as \n"
        + "'Aggregate({[Time].[1997].[Q1]:[Time].[1997].[Q3]})'\n"
        + "select\n"
        + "{[Measures].[Unit Sales],[Measures].[Store Cost],\n"
        + "[Measures].[Store Sales]} ON columns,\n"
        + "NON EMPTY Order (Filter ([Store].[Store State].members,\n"
        + "[Measures].[Store Cost] > 100),[Measures].[Store Cost], DESC) ON rows\n"
        + "from [Sales] where [Time].[Date Range]",
      "Axis #0:\n"
        + "{[Time].[Date Range]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Store Cost]}\n"
        + "{[Measures].[Store Sales]}\n"
        + "Axis #2:\n"
        + "{[Store].[USA].[WA]}\n"
        + "{[Store].[USA].[CA]}\n"
        + "{[Store].[USA].[OR]}\n"
        + "Row #0: 90,131\n"
        + "Row #0: 76,151.59\n"
        + "Row #0: 190,776.88\n"
        + "Row #1: 53,312\n"
        + "Row #1: 45,435.93\n"
        + "Row #1: 113,966.00\n"
        + "Row #2: 51,306\n"
        + "Row #2: 43,033.82\n"
        + "Row #2: 107,823.63\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExistsMembersAll(Context context) {
    assertQueryReturns(context.getConnection(),
      "select exists(\n"
        + "  {[Customers].[All Customers],\n"
        + "   [Customers].[Country].Members,\n"
        + "   [Customers].[State Province].[CA],\n"
        + "   [Customers].[Canada].[BC].[Richmond]},\n"
        + "  {[Customers].[All Customers]})\n"
        + "on 0 from Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[All Customers]}\n"
        + "{[Customers].[Canada]}\n"
        + "{[Customers].[Mexico]}\n"
        + "{[Customers].[USA]}\n"
        + "{[Customers].[USA].[CA]}\n"
        + "{[Customers].[Canada].[BC].[Richmond]}\n"
        + "Row #0: 266,773\n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: 266,773\n"
        + "Row #0: 74,748\n"
        + "Row #0: \n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExistsMembersLevel2(Context context) {
    assertQueryReturns(context.getConnection(),
      "select exists(\n"
        + "  {[Customers].[All Customers],\n"
        + "   [Customers].[Country].Members,\n"
        + "   [Customers].[State Province].[CA],\n"
        + "   [Customers].[Canada].[BC].[Richmond]},\n"
        + "  {[Customers].[Country].[USA]})\n"
        + "on 0 from Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[All Customers]}\n"
        + "{[Customers].[USA]}\n"
        + "{[Customers].[USA].[CA]}\n"
        + "Row #0: 266,773\n"
        + "Row #0: 266,773\n"
        + "Row #0: 74,748\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExistsWithImplicitAllMember(Context context) {
    // the tuple in the second arg in this case should implicitly
    // contain [Customers].[All Customers], so the whole tuple list
    // from the first arg should be returned.
    assertQueryReturns(context.getConnection(),
      "select non empty exists(\n"
        + "  {[Customers].[All Customers],\n"
        + "   [Customers].[All Customers].Children,\n"
        + "   [Customers].[State Province].Members},\n"
        + "  {[Product].Members})\n"
        + "on 0 from Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[All Customers]}\n"
        + "{[Customers].[USA]}\n"
        + "{[Customers].[USA].[CA]}\n"
        + "{[Customers].[USA].[OR]}\n"
        + "{[Customers].[USA].[WA]}\n"
        + "Row #0: 266,773\n"
        + "Row #0: 266,773\n"
        + "Row #0: 74,748\n"
        + "Row #0: 67,659\n"
        + "Row #0: 124,366\n" );

    assertQueryReturns(context.getConnection(),
      "select exists( "
        + "[Customers].[USA].[CA], (Store.[USA], Gender.[F])) "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[USA].[CA]}\n"
        + "Row #0: 74,748\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExistsWithMultipleHierarchies(Context context) {
    // tests queries w/ a multi-hierarchy dim in either or both args.
    assertQueryReturns(context.getConnection(),
      "select exists( "
        + "crossjoin( time.[1997], {[Time.Weekly].[1997].[16]}), "
        + " { Gender.F } ) on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Time].[1997], [Time.Weekly].[1997].[16]}\n"
        + "Row #0: 3,839\n" );

    assertQueryReturns(context.getConnection(),
      "select exists( "
        + "time.[1997].[Q1], {[Time.Weekly].[1997].[4]}) "
        + " on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Time].[1997].[Q1]}\n"
        + "Row #0: 66,291\n" );

    assertQueryReturns(context.getConnection(),
      "select exists( "
        + "{ Gender.F }, "
        + "crossjoin( time.[1997], {[Time.Weekly].[1997].[16]})  ) "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Gender].[F]}\n"
        + "Row #0: 131,558\n" );

    assertQueryReturns(context.getConnection(),
      "select exists( "
        + "{ time.[1998] }, "
        + "crossjoin( time.[1997], {[Time.Weekly].[1997].[16]})  ) "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExistsWithDefaultNonAllMember(Context context) {
    // default mem for Time is 1997

    // non-all default on right side.
    assertQueryReturns(context.getConnection(),
      "select exists( [Time].[1998].[Q1], Gender.[All Gender]) on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n" );

    // switching to an explicit member on the hierarchy chain should return
    // 1998.Q1
    assertQueryReturns(context.getConnection(),
      "select exists( [Time].[1998].[Q1], ([Time].[1998], Gender.[All Gender])) on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Time].[1998].[Q1]}\n"
        + "Row #0: \n" );


    // non-all default on left side
    assertQueryReturns(context.getConnection(),
      "select exists( "
        + "Gender.[All Gender], (Gender.[F], [Time].[1998].[Q1])) "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n" );

    assertQueryReturns(context.getConnection(),
      "select exists( "
        + "(Time.[1998].[Q1].[1], Gender.[All Gender]), (Gender.[F], [Time].[1998].[Q1])) "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Time].[1998].[Q1].[1], [Gender].[All Gender]}\n"
        + "Row #0: \n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExistsMembers2Hierarchies(Context context) {
    assertQueryReturns(context.getConnection(),
      "select exists(\n"
        + "  {[Customers].[All Customers],\n"
        + "   [Customers].[All Customers].Children,\n"
        + "   [Customers].[State Province].Members,\n"
        + "   [Customers].[Country].[Canada],\n"
        + "   [Customers].[Country].[Mexico]},\n"
        + "  {[Customers].[Country].[USA],\n"
        + "   [Customers].[State Province].[Veracruz]})\n"
        + "on 0 from Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[All Customers]}\n"
        + "{[Customers].[Mexico]}\n"
        + "{[Customers].[USA]}\n"
        + "{[Customers].[Mexico].[Veracruz]}\n"
        + "{[Customers].[USA].[CA]}\n"
        + "{[Customers].[USA].[OR]}\n"
        + "{[Customers].[USA].[WA]}\n"
        + "{[Customers].[Mexico]}\n"
        + "Row #0: 266,773\n"
        + "Row #0: \n"
        + "Row #0: 266,773\n"
        + "Row #0: \n"
        + "Row #0: 74,748\n"
        + "Row #0: 67,659\n"
        + "Row #0: 124,366\n"
        + "Row #0: \n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExistsTuplesAll(Context context) {
    assertQueryReturns(context.getConnection(),
      "select exists(\n"
        + "  crossjoin({[Product].[All Products]},{[Customers].[All Customers]}),\n"
        + "  {[Customers].[All Customers]})\n"
        + "on 0 from Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Product].[All Products], [Customers].[All Customers]}\n"
        + "Row #0: 266,773\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExistsTuplesLevel2(Context context) {
    assertQueryReturns(context.getConnection(),
      "select exists(\n"
        + "  crossjoin({[Product].[All Products]},{[Customers].[All Customers].Children}),\n"
        + "  {[Customers].[All Customers].[USA]})\n"
        + "on 0 from Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Product].[All Products], [Customers].[USA]}\n"
        + "Row #0: 266,773\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExistsTuplesLevel23(Context context) {
    assertQueryReturns(context.getConnection(),
      "select exists(\n"
        + "  crossjoin({[Customers].[State Province].Members}, {[Product].[All Products]}),\n"
        + "  {[Customers].[All Customers].[USA]})\n"
        + "on 0 from Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[USA].[CA], [Product].[All Products]}\n"
        + "{[Customers].[USA].[OR], [Product].[All Products]}\n"
        + "{[Customers].[USA].[WA], [Product].[All Products]}\n"
        + "Row #0: 74,748\n"
        + "Row #0: 67,659\n"
        + "Row #0: 124,366\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExistsTuples2Dim(Context context) {
    assertQueryReturns(context.getConnection(),
      "select exists(\n"
        + "  crossjoin({[Customers].[State Province].Members}, {[Product].[Product Family].Members}),\n"
        + "  {([Product].[Product Department].[Dairy],[Customers].[All Customers].[USA])})\n"
        + "on 0 from Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[USA].[CA], [Product].[Drink]}\n"
        + "{[Customers].[USA].[OR], [Product].[Drink]}\n"
        + "{[Customers].[USA].[WA], [Product].[Drink]}\n"
        + "Row #0: 7,102\n"
        + "Row #0: 6,106\n"
        + "Row #0: 11,389\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExistsTuplesDiffDim(Context context) {
    assertQueryReturns(context.getConnection(),
      "select exists(\n"
        + "  crossjoin(\n"
        + "    crossjoin({[Customers].[State Province].Members},\n"
        + "              {[Time].[Year].[1997]}), \n"
        + "    {[Product].[Product Family].Members}),\n"
        + "  {([Product].[Product Department].[Dairy],\n"
        + "    [Promotions].[All Promotions], \n"
        + "    [Customers].[All Customers].[USA])})\n"
        + "on 0 from Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[USA].[CA], [Time].[1997], [Product].[Drink]}\n"
        + "{[Customers].[USA].[OR], [Time].[1997], [Product].[Drink]}\n"
        + "{[Customers].[USA].[WA], [Time].[1997], [Product].[Drink]}\n"
        + "Row #0: 7,102\n"
        + "Row #0: 6,106\n"
        + "Row #0: 11,389\n" );
  }


  /**
   * Executes a query that has a complex parse tree. Goal is to find algorithmic complexity bugs in the validator which
   * would make the query run extremely slowly.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexQuery(Context context) {
    final String expected =
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Gender].[All Gender]}\n"
        + "{[Gender].[F]}\n"
        + "{[Gender].[M]}\n"
        + "Row #0: 266,773\n"
        + "Row #1: 131,558\n"
        + "Row #2: 135,215\n";

    // hand written case
    assertQueryReturns(context.getConnection(),
      "select\n"
        + "   [Measures].[Unit Sales] on 0,\n"
        + "   Distinct({\n"
        + "     [Gender],\n"
        + "     Tail(\n"
        + "       Head({\n"
        + "         [Gender],\n"
        + "         [Gender].[F],\n"
        + "         [Gender].[M]},\n"
        + "         2),\n"
        + "       1),\n"
        + "     Tail(\n"
        + "       Head({\n"
        + "         [Gender],\n"
        + "         [Gender].[F],\n"
        + "         [Gender].[M]},\n"
        + "         2),\n"
        + "       1),\n"
        + "     [Gender].[M]}) on 1\n"
        + "from [Sales]", expected );

    // generated equivalent
    StringBuilder buf = new StringBuilder();
    buf.append(
      "select\n"
        + "   [Measures].[Unit Sales] on 0,\n" );
    generateComplex( buf, "   ", 0, 7, 3 );
    buf.append(
      " on 1\n"
        + "from [Sales]" );
    if ( false ) {
      System.out.println( buf.toString().length() + ": " + buf.toString() );
    }
    assertQueryReturns(context.getConnection(), buf.toString(), expected );
  }

  /**
   * Recursive routine to generate a complex MDX expression.
   *
   * @param buf        String builder
   * @param indent     Indent
   * @param depth      Current depth
   * @param depthLimit Max recursion depth
   * @param breadth    Number of iterations at each depth
   */
  void generateComplex(
    StringBuilder buf,
    String indent,
    int depth,
    int depthLimit,
    int breadth ) {
    buf.append( indent + "Distinct({\n" );
    buf.append( indent + "  [Gender],\n" );
    for ( int i = 0; i < breadth; i++ ) {
      if ( depth < depthLimit ) {
        buf.append( indent + "  Tail(\n" );
        buf.append( indent + "    Head({\n" );
        generateComplex(
          buf,
          indent + "      ",
          depth + 1,
          depthLimit,
          breadth );
        buf.append( "},\n" );
        buf.append( indent + "      2),\n" );
        buf.append( indent + "    1),\n" );
      } else {
        buf.append( indent + "  [Gender].[F],\n" );
      }
    }
    buf.append( indent + "  [Gender].[M]})" );
  }

  /**
   * Testcase for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-1050"> MONDRIAN-1050, "MDX Order function fails
   * when using DateTime expression for ordering"</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDateParameter(Context context) throws Exception {
    String query = "SELECT"
      + " {[Measures].[Unit Sales]} ON COLUMNS,"
      + " Order([Gender].Members,"
      + " Now(), ASC) ON ROWS"
      + " FROM [Sales]";
    String expected = "Axis #0:\n"
      + "{}\n"
      + "Axis #1:\n"
      + "{[Measures].[Unit Sales]}\n"
      + "Axis #2:\n"
      + "{[Gender].[All Gender]}\n"
      + "{[Gender].[F]}\n"
      + "{[Gender].[M]}\n"
      + "Row #0: 266,773\n"
      + "Row #1: 131,558\n"
      + "Row #2: 135,215\n";
    assertQueryReturns(context.getConnection(), query, expected );
  }

  /**
   * Testcase for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-1043"> MONDRIAN-1043, "Hierarchize with Except
   * sort set members differently than in Mondrian 3.2.1"</a>.
   *
   * <p>This test makes sure that
   * Hierarchize and Except can be used within each other and that the sort order is maintained.</p>
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHierarchizeExcept(Context context) throws Exception {
    final String[] mdxA =
      new String[] {
        "SELECT {[Measures].[Unit Sales], [Measures].[Store Sales]} ON COLUMNS, Hierarchize(Except({[Customers].[USA]"
          + ".Children, [Customers].[USA].[CA].Children}, [Customers].[USA].[CA])) ON ROWS FROM [Sales]",
        "SELECT {[Measures].[Unit Sales], [Measures].[Store Sales]} ON COLUMNS, Except(Hierarchize({[Customers].[USA]"
          + ".Children, [Customers].[USA].[CA].Children}), [Customers].[USA].[CA]) ON ROWS FROM [Sales] "
      };
    for ( String mdx : mdxA ) {
      assertQueryReturns(context.getConnection(),
        mdx,
        "Axis #0:\n"
          + "{}\n"
          + "Axis #1:\n"
          + "{[Measures].[Unit Sales]}\n"
          + "{[Measures].[Store Sales]}\n"
          + "Axis #2:\n"
          + "{[Customers].[USA].[CA].[Altadena]}\n"
          + "{[Customers].[USA].[CA].[Arcadia]}\n"
          + "{[Customers].[USA].[CA].[Bellflower]}\n"
          + "{[Customers].[USA].[CA].[Berkeley]}\n"
          + "{[Customers].[USA].[CA].[Beverly Hills]}\n"
          + "{[Customers].[USA].[CA].[Burbank]}\n"
          + "{[Customers].[USA].[CA].[Burlingame]}\n"
          + "{[Customers].[USA].[CA].[Chula Vista]}\n"
          + "{[Customers].[USA].[CA].[Colma]}\n"
          + "{[Customers].[USA].[CA].[Concord]}\n"
          + "{[Customers].[USA].[CA].[Coronado]}\n"
          + "{[Customers].[USA].[CA].[Daly City]}\n"
          + "{[Customers].[USA].[CA].[Downey]}\n"
          + "{[Customers].[USA].[CA].[El Cajon]}\n"
          + "{[Customers].[USA].[CA].[Fremont]}\n"
          + "{[Customers].[USA].[CA].[Glendale]}\n"
          + "{[Customers].[USA].[CA].[Grossmont]}\n"
          + "{[Customers].[USA].[CA].[Imperial Beach]}\n"
          + "{[Customers].[USA].[CA].[La Jolla]}\n"
          + "{[Customers].[USA].[CA].[La Mesa]}\n"
          + "{[Customers].[USA].[CA].[Lakewood]}\n"
          + "{[Customers].[USA].[CA].[Lemon Grove]}\n"
          + "{[Customers].[USA].[CA].[Lincoln Acres]}\n"
          + "{[Customers].[USA].[CA].[Long Beach]}\n"
          + "{[Customers].[USA].[CA].[Los Angeles]}\n"
          + "{[Customers].[USA].[CA].[Mill Valley]}\n"
          + "{[Customers].[USA].[CA].[National City]}\n"
          + "{[Customers].[USA].[CA].[Newport Beach]}\n"
          + "{[Customers].[USA].[CA].[Novato]}\n"
          + "{[Customers].[USA].[CA].[Oakland]}\n"
          + "{[Customers].[USA].[CA].[Palo Alto]}\n"
          + "{[Customers].[USA].[CA].[Pomona]}\n"
          + "{[Customers].[USA].[CA].[Redwood City]}\n"
          + "{[Customers].[USA].[CA].[Richmond]}\n"
          + "{[Customers].[USA].[CA].[San Carlos]}\n"
          + "{[Customers].[USA].[CA].[San Diego]}\n"
          + "{[Customers].[USA].[CA].[San Francisco]}\n"
          + "{[Customers].[USA].[CA].[San Gabriel]}\n"
          + "{[Customers].[USA].[CA].[San Jose]}\n"
          + "{[Customers].[USA].[CA].[Santa Cruz]}\n"
          + "{[Customers].[USA].[CA].[Santa Monica]}\n"
          + "{[Customers].[USA].[CA].[Spring Valley]}\n"
          + "{[Customers].[USA].[CA].[Torrance]}\n"
          + "{[Customers].[USA].[CA].[West Covina]}\n"
          + "{[Customers].[USA].[CA].[Woodland Hills]}\n"
          + "{[Customers].[USA].[OR]}\n"
          + "{[Customers].[USA].[WA]}\n"
          + "Row #0: 2,574\n"
          + "Row #0: 5,585.59\n"
          + "Row #1: 2,440\n"
          + "Row #1: 5,136.59\n"
          + "Row #2: 3,106\n"
          + "Row #2: 6,633.97\n"
          + "Row #3: 136\n"
          + "Row #3: 320.17\n"
          + "Row #4: 2,907\n"
          + "Row #4: 6,194.37\n"
          + "Row #5: 3,086\n"
          + "Row #5: 6,577.33\n"
          + "Row #6: 198\n"
          + "Row #6: 407.38\n"
          + "Row #7: 2,999\n"
          + "Row #7: 6,284.30\n"
          + "Row #8: 129\n"
          + "Row #8: 287.78\n"
          + "Row #9: 105\n"
          + "Row #9: 219.77\n"
          + "Row #10: 2,391\n"
          + "Row #10: 5,051.15\n"
          + "Row #11: 129\n"
          + "Row #11: 271.60\n"
          + "Row #12: 3,440\n"
          + "Row #12: 7,367.06\n"
          + "Row #13: 2,543\n"
          + "Row #13: 5,460.42\n"
          + "Row #14: 163\n"
          + "Row #14: 350.22\n"
          + "Row #15: 3,284\n"
          + "Row #15: 7,082.91\n"
          + "Row #16: 2,131\n"
          + "Row #16: 4,458.60\n"
          + "Row #17: 1,616\n"
          + "Row #17: 3,409.34\n"
          + "Row #18: 1,938\n"
          + "Row #18: 4,081.37\n"
          + "Row #19: 1,834\n"
          + "Row #19: 3,908.26\n"
          + "Row #20: 2,487\n"
          + "Row #20: 5,174.12\n"
          + "Row #21: 2,651\n"
          + "Row #21: 5,636.82\n"
          + "Row #22: 2,176\n"
          + "Row #22: 4,691.94\n"
          + "Row #23: 2,973\n"
          + "Row #23: 6,422.37\n"
          + "Row #24: 2,009\n"
          + "Row #24: 4,312.99\n"
          + "Row #25: 58\n"
          + "Row #25: 109.36\n"
          + "Row #26: 2,031\n"
          + "Row #26: 4,237.46\n"
          + "Row #27: 3,098\n"
          + "Row #27: 6,696.06\n"
          + "Row #28: 163\n"
          + "Row #28: 335.98\n"
          + "Row #29: 70\n"
          + "Row #29: 145.90\n"
          + "Row #30: 133\n"
          + "Row #30: 272.08\n"
          + "Row #31: 2,712\n"
          + "Row #31: 5,595.62\n"
          + "Row #32: 144\n"
          + "Row #32: 312.43\n"
          + "Row #33: 110\n"
          + "Row #33: 212.45\n"
          + "Row #34: 145\n"
          + "Row #34: 289.80\n"
          + "Row #35: 1,535\n"
          + "Row #35: 3,348.69\n"
          + "Row #36: 88\n"
          + "Row #36: 195.28\n"
          + "Row #37: 2,631\n"
          + "Row #37: 5,663.60\n"
          + "Row #38: 161\n"
          + "Row #38: 343.20\n"
          + "Row #39: 185\n"
          + "Row #39: 367.78\n"
          + "Row #40: 2,660\n"
          + "Row #40: 5,739.63\n"
          + "Row #41: 1,790\n"
          + "Row #41: 3,862.79\n"
          + "Row #42: 2,570\n"
          + "Row #42: 5,405.02\n"
          + "Row #43: 2,503\n"
          + "Row #43: 5,302.08\n"
          + "Row #44: 2,516\n"
          + "Row #44: 5,406.21\n"
          + "Row #45: 67,659\n"
          + "Row #45: 142,277.07\n"
          + "Row #46: 124,366\n"
          + "Row #46: 263,793.22\n" );
    }
  }

  /**
   * This is a test for
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-2157">MONDRIAN-2157</a>
   * <p/>
   * <p>The results should be equivalent either we use aliases or not</p>
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTopPercentWithAlias(Context context) {
    final String queryWithoutAlias =
      "select\n"
        + " {[Measures].[Store Cost]}on rows,\n"
        + " TopPercent([Product].[Brand Name].Members*[Time].[1997].children,"
        + " 50, [Measures].[Unit Sales]) on columns\n"
        + "from Sales";
    String queryWithAlias =
      "with\n"
        + " set [*aaa] as '[Product].[Brand Name].Members*[Time].[1997].children'\n"
        + "select\n"
        + " {[Measures].[Store Cost]}on rows,\n"
        + " TopPercent([*aaa], 50, [Measures].[Unit Sales]) on columns\n"
        + "from Sales";

    final Result result = executeQuery(context.getConnection(), queryWithoutAlias );
    assertQueryReturns(context.getConnection(),
      queryWithAlias,
      TestUtil.toString( result ) );
  }

  /**
   * This is a test for
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-1187">MONDRIAN-1187</a>
   * <p/>
   * <p>The results should be equivalent</p>
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMondrian_1187(Context context) {
    final String queryWithoutAlias =
      "WITH\n" + "SET [Top Count] AS\n"
        + "{\n" + "TOPCOUNT(\n" + "DISTINCT([Customers].[Name].Members),\n"
        + "5,\n" + "[Measures].[Unit Sales]\n" + ")\n" + "}\n" + "SELECT\n"
        + "[Top Count] * [Measures].[Unit Sales] on 0\n" + "FROM [Sales]\n"
        + "WHERE [Time].[1997].[Q1].[1] : [Time].[1997].[Q3].[8]";
    String queryWithAlias =
      "SELECT\n"
        + "TOPCOUNT( DISTINCT( [Customers].[Name].Members), 5, [Measures].[Unit Sales]) * [Measures].[Unit Sales] on "
        + "0\n"
        + "FROM [Sales]\n"
        + "WHERE [Time].[1997].[Q1].[1]:[Time].[1997].[Q3].[8]";
    final Result result = executeQuery(context.getConnection(), queryWithoutAlias );
    assertQueryReturns(context.getConnection(),
      queryWithAlias,
      TestUtil.toString(result));
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexSlicer_BaseBase(Context context) {
    String query =
      "SELECT "
        + "{[Measures].[Customer Count]} ON 0, "
        + "{[Education Level].Members} ON 1 "
        + "FROM [Sales] "
        + "WHERE {[Time].[1997].[Q2],[Time].[1998].[Q1]}";
    String expectedResult =
      "Axis #0:\n"
        + "{[Time].[1997].[Q2]}\n"
        + "{[Time].[1998].[Q1]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Axis #2:\n"
        + "{[Education Level].[All Education Levels]}\n"
        + "{[Education Level].[Bachelors Degree]}\n"
        + "{[Education Level].[Graduate Degree]}\n"
        + "{[Education Level].[High School Degree]}\n"
        + "{[Education Level].[Partial College]}\n"
        + "{[Education Level].[Partial High School]}\n"
        + "Row #0: 2,973\n"
        + "Row #1: 760\n"
        + "Row #2: 178\n"
        + "Row #3: 853\n"
        + "Row #4: 273\n"
        + "Row #5: 909\n";
    assertQueryReturns(context.getConnection(), query, expectedResult );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexSlicer_Calc(Context context) {
      withSchema(context, SchemaModifiers.FunctionTestModifier::new);
      /*
      ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
      "Sales",
      null,
      "<CalculatedMember "
        + "name='H1 1997' "
        + "formula='Aggregate([Time].[1997].[Q1]:[Time].[1997].[Q2])' "
        + "dimension='Time' />" ));
       */
    String query =
      "SELECT "
        + "{[Measures].[Customer Count]} ON 0, "
        + "{[Education Level].Members} ON 1 "
        + "FROM [Sales] "
        + "WHERE {[Time].[H1 1997]}";
    String expectedResult =
      "Axis #0:\n"
        + "{[Time].[H1 1997]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Axis #2:\n"
        + "{[Education Level].[All Education Levels]}\n"
        + "{[Education Level].[Bachelors Degree]}\n"
        + "{[Education Level].[Graduate Degree]}\n"
        + "{[Education Level].[High School Degree]}\n"
        + "{[Education Level].[Partial College]}\n"
        + "{[Education Level].[Partial High School]}\n"
        + "Row #0: 4,257\n"
        + "Row #1: 1,109\n"
        + "Row #2: 240\n"
        + "Row #3: 1,237\n"
        + "Row #4: 394\n"
        + "Row #5: 1,277\n";
    assertQueryReturns(context.getConnection(), query, expectedResult );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexSlicer_CalcBase(Context context) {
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
      "Sales",
      null,
      "<CalculatedMember "
        + "name='H1 1997' "
        + "formula='Aggregate([Time].[1997].[Q1]:[Time].[1997].[Q2])' "
        + "dimension='Time' />" ));
     */
      withSchema(context, SchemaModifiers.FunctionTestModifier::new);

      String query =
      "SELECT "
        + "{[Measures].[Customer Count]} ON 0, "
        + "{[Education Level].Members} ON 1 "
        + "FROM [Sales] "
        + "WHERE {[Time].[H1 1997],[Time].[1998].[Q1]}";
    String expectedResult =
      "Axis #0:\n"
        + "{[Time].[H1 1997]}\n"
        + "{[Time].[1998].[Q1]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Axis #2:\n"
        + "{[Education Level].[All Education Levels]}\n"
        + "{[Education Level].[Bachelors Degree]}\n"
        + "{[Education Level].[Graduate Degree]}\n"
        + "{[Education Level].[High School Degree]}\n"
        + "{[Education Level].[Partial College]}\n"
        + "{[Education Level].[Partial High School]}\n"
        + "Row #0: 4,257\n"
        + "Row #1: 1,109\n"
        + "Row #2: 240\n"
        + "Row #3: 1,237\n"
        + "Row #4: 394\n"
        + "Row #5: 1,277\n";
    assertQueryReturns(context.getConnection(), query, expectedResult );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexSlicer_BaseCalc(Context context) {
     /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
      "Sales",
      null,
      "<CalculatedMember "
        + "name='H1 1997' "
        + "formula='Aggregate([Time].[1997].[Q1]:[Time].[1997].[Q2])' "
        + "dimension='Time' />" ));
    */
      withSchema(context, SchemaModifiers.FunctionTestModifier::new);

      String query =
      "SELECT "
        + "{[Measures].[Customer Count]} ON 0, "
        + "{[Education Level].Members} ON 1 "
        + "FROM [Sales] "
        + "WHERE {[Time].[1998].[Q1], [Time].[H1 1997]}";
    String expectedResult =
      "Axis #0:\n"
        + "{[Time].[1998].[Q1]}\n"
        + "{[Time].[H1 1997]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Axis #2:\n"
        + "{[Education Level].[All Education Levels]}\n"
        + "{[Education Level].[Bachelors Degree]}\n"
        + "{[Education Level].[Graduate Degree]}\n"
        + "{[Education Level].[High School Degree]}\n"
        + "{[Education Level].[Partial College]}\n"
        + "{[Education Level].[Partial High School]}\n"
        + "Row #0: 4,257\n"
        + "Row #1: 1,109\n"
        + "Row #2: 240\n"
        + "Row #3: 1,237\n"
        + "Row #4: 394\n"
        + "Row #5: 1,277\n";
    assertQueryReturns(context.getConnection(), query, expectedResult );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexSlicer_Calc_Base(Context context) {
     /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
      "Sales",
      null,
      "<CalculatedMember "
        + "name='H1 1997' "
        + "formula='Aggregate([Time].[1997].[Q1]:[Time].[1997].[Q2])' "
        + "dimension='Time' />" ));
      */
      withSchema(context, SchemaModifiers.FunctionTestModifier::new);
      String query =
      "SELECT "
        + "{[Measures].[Customer Count]} ON 0 "
        + "FROM [Sales] "
        + "WHERE ([Time].[H1 1997],[Education Level].[Partial College])";
    String expectedResult =
      "Axis #0:\n"
        + "{[Time].[H1 1997], [Education Level].[Partial College]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 394\n";
    assertQueryReturns(context.getConnection(), query, expectedResult );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexSlicer_Calc_Calc(Context context) {
      /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
      "Sales",
      null,
      "<CalculatedMember "
        + "name='H1 1997' "
        + "formula='Aggregate([Time].[1997].[Q1]:[Time].[1997].[Q2])' "
        + "dimension='Time' />"
        + "<CalculatedMember "
        + "name='Partial' "
        + "formula='Aggregate([Education Level].[Partial College]:[Education Level].[Partial High School])' "
        + "dimension='Education Level' />"));
       */
      withSchema(context, SchemaModifiers.FunctionTestModifier2::new);

      String query =
      "SELECT "
        + "{[Measures].[Customer Count]} ON 0 "
        + "FROM [Sales] "
        + "WHERE ([Time].[H1 1997],[Education Level].[Partial])";
    String expectedResult =
      "Axis #0:\n"
        + "{[Time].[H1 1997], [Education Level].[Partial]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 1,671\n";
    assertQueryReturns(context.getConnection(), query, expectedResult );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexSlicerWith_Calc(Context context) {
    String query =
      "with "
        + "member [Time].[H1 1997] as 'Aggregate([Time].[1997].[Q1] : [Time].[1997].[Q2])', $member_scope = \"CUBE\","
        + " MEMBER_ORDINAL = 6 "
        + "SELECT "
        + "{[Measures].[Customer Count]} ON 0, "
        + "{[Education Level].Members} ON 1 "
        + "FROM [Sales] "
        + "WHERE {[Time].[H1 1997]}";
    String expectedResult =
      "Axis #0:\n"
        + "{[Time].[H1 1997]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Axis #2:\n"
        + "{[Education Level].[All Education Levels]}\n"
        + "{[Education Level].[Bachelors Degree]}\n"
        + "{[Education Level].[Graduate Degree]}\n"
        + "{[Education Level].[High School Degree]}\n"
        + "{[Education Level].[Partial College]}\n"
        + "{[Education Level].[Partial High School]}\n"
        + "Row #0: 4,257\n"
        + "Row #1: 1,109\n"
        + "Row #2: 240\n"
        + "Row #3: 1,237\n"
        + "Row #4: 394\n"
        + "Row #5: 1,277\n";
    assertQueryReturns(context.getConnection(), query, expectedResult );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexSlicerWith_CalcBase(Context context) {
    String query =
      "with "
        + "member [Time].[H1 1997] as 'Aggregate([Time].[1997].[Q1] : [Time].[1997].[Q2])', $member_scope = \"CUBE\","
        + " MEMBER_ORDINAL = 6 "
        + "SELECT "
        + "{[Measures].[Customer Count]} ON 0, "
        + "{[Education Level].Members} ON 1 "
        + "FROM [Sales] "
        + "WHERE {[Time].[H1 1997],[Time].[1998].[Q1]}";
    String expectedResult =
      "Axis #0:\n"
        + "{[Time].[H1 1997]}\n"
        + "{[Time].[1998].[Q1]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Axis #2:\n"
        + "{[Education Level].[All Education Levels]}\n"
        + "{[Education Level].[Bachelors Degree]}\n"
        + "{[Education Level].[Graduate Degree]}\n"
        + "{[Education Level].[High School Degree]}\n"
        + "{[Education Level].[Partial College]}\n"
        + "{[Education Level].[Partial High School]}\n"
        + "Row #0: 4,257\n"
        + "Row #1: 1,109\n"
        + "Row #2: 240\n"
        + "Row #3: 1,237\n"
        + "Row #4: 394\n"
        + "Row #5: 1,277\n";
    assertQueryReturns(context.getConnection(), query, expectedResult );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexSlicerWith_Calc_Calc(Context context) {
    String query =
      "with "
        + "member [Time].[H1 1997] as 'Aggregate([Time].[1997].[Q1] : [Time].[1997].[Q2])', $member_scope = \"CUBE\","
        + " MEMBER_ORDINAL = 6 "
        + "member [Education Level].[Partial] as 'Aggregate([Education Level].[Partial College]:[Education Level]"
        + ".[Partial High School])', $member_scope = \"CUBE\", MEMBER_ORDINAL = 7 "
        + "SELECT "
        + "{[Measures].[Customer Count]} ON 0 "
        + "FROM [Sales] "
        + "WHERE ([Time].[H1 1997],[Education Level].[Partial])";
    String expectedResult =
      "Axis #0:\n"
        + "{[Time].[H1 1997], [Education Level].[Partial]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 1,671\n";
    assertQueryReturns(context.getConnection(), query, expectedResult );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexSlicer_X_Base_Base(Context context) {
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
      "Sales",
      null,
      "<CalculatedMember "
        + "name='H1 1997' "
        + "formula='Aggregate([Time].[1997].[Q1]:[Time].[1997].[Q2])' "
        + "dimension='Time' />" ));
      */
      withSchema(context, SchemaModifiers.FunctionTestModifier::new);

      String query =
      "SELECT "
        + "{[Measures].[Customer Count]} ON 0 "
        + "FROM [Sales] "
        + "WHERE CROSSJOIN ([Time].[1997].[Q1] , [Education Level].[Partial College])";
    String expectedResult =
      "Axis #0:\n"
        + "{[Time].[1997].[Q1], [Education Level].[Partial College]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 278\n";
    assertQueryReturns(context.getConnection(), query, expectedResult );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexSlicer_X_Calc_Base(Context context) {
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
      "Sales",
      null,
      "<CalculatedMember "
        + "name='H1 1997' "
        + "formula='Aggregate([Time].[1997].[Q1]:[Time].[1997].[Q2])' "
        + "dimension='Time' />" ));
    */
      withSchema(context, SchemaModifiers.FunctionTestModifier::new);

      String query =
      "SELECT "
        + "{[Measures].[Customer Count]} ON 0 "
        + "FROM [Sales] "
        + "WHERE CROSSJOIN ([Time].[H1 1997] , [Education Level].[Partial College])";
    String expectedResult =
      "Axis #0:\n"
        + "{[Time].[H1 1997], [Education Level].[Partial College]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 394\n";
    assertQueryReturns(context.getConnection(), query, expectedResult );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexSlicer_X_Calc_Calc(Context context) {
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
      "Sales",
      null,
      "<CalculatedMember "
        + "name='H1 1997' "
        + "formula='Aggregate([Time].[1997].[Q1]:[Time].[1997].[Q2])' "
        + "dimension='Time' />"
        + "<CalculatedMember "
        + "name='Partial' "
        + "formula='Aggregate([Education Level].[Partial College]:[Education Level].[Partial High School])' "
        + "dimension='Education Level' />" ));
      */
      withSchema(context, SchemaModifiers.FunctionTestModifier2::new);
      String query =
      "SELECT "
        + "{[Measures].[Customer Count]} ON 0 "
        + "FROM [Sales] "
        + "WHERE CROSSJOIN ([Time].[H1 1997] , [Education Level].[Partial])";
    String expectedResult =
      "Axis #0:\n"
        + "{[Time].[H1 1997], [Education Level].[Partial]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 1,671\n";
    assertQueryReturns(context.getConnection(), query, expectedResult );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexSlicer_X_BaseBase_Base(Context context) {
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
      "Sales",
      null,
      "<CalculatedMember "
        + "name='H1 1997' "
        + "formula='Aggregate([Time].[1997].[Q1]:[Time].[1997].[Q2])' "
        + "dimension='Time' />" ));
    */
      withSchema(context, SchemaModifiers.FunctionTestModifier::new);

      String query =
      "SELECT "
        + "{[Measures].[Customer Count]} ON 0 "
        + "FROM [Sales] "
        + "WHERE CROSSJOIN ({[Time].[1997].[Q1], [Time].[1997].[Q2]} , [Education Level].[Partial College])";
    String expectedResult =
      "Axis #0:\n"
        + "{[Time].[1997].[Q1], [Education Level].[Partial College]}\n"
        + "{[Time].[1997].[Q2], [Education Level].[Partial College]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 394\n";
    assertQueryReturns(context.getConnection(), query, expectedResult );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexSlicer_X_BaseBaseBase_BaseBase(Context context) {
    String query =
      "SELECT "
        + "{[Measures].[Customer Count]} ON 0 "
        + "FROM [Sales] "
        + "WHERE CROSSJOIN ({[Time].[1997].[Q1],[Time].[1997].[Q2],[Time].[1998].[Q1]} , {[Education Level].[Partial "
        + "College],[Education Level].[Partial High School]})";
    String expectedResult =
      "Axis #0:\n"
        + "{[Time].[1997].[Q1], [Education Level].[Partial College]}\n"
        + "{[Time].[1997].[Q1], [Education Level].[Partial High School]}\n"
        + "{[Time].[1997].[Q2], [Education Level].[Partial College]}\n"
        + "{[Time].[1997].[Q2], [Education Level].[Partial High School]}\n"
        + "{[Time].[1998].[Q1], [Education Level].[Partial College]}\n"
        + "{[Time].[1998].[Q1], [Education Level].[Partial High School]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 1,671\n";
    assertQueryReturns(context.getConnection(), query, expectedResult );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexSlicer_X_CalcBase_Base(Context context) {
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
      "Sales",
      null,
      "<CalculatedMember "
        + "name='H1 1997' "
        + "formula='Aggregate([Time].[1997].[Q1]:[Time].[1997].[Q2])' "
        + "dimension='Time' />" ));
     */
      withSchema(context, SchemaModifiers.FunctionTestModifier::new);

      String query =
      "SELECT "
        + "{[Measures].[Customer Count]} ON 0 "
        + "FROM [Sales] "
        + "WHERE CROSSJOIN ({[Time].[H1 1997],[Time].[1998].[Q1]} , [Education Level].[Partial College])";
    String expectedResult =
      "Axis #0:\n"
        + "{[Time].[H1 1997], [Education Level].[Partial College]}\n"
        + "{[Time].[1998].[Q1], [Education Level].[Partial College]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 394\n";
    assertQueryReturns(context.getConnection(), query, expectedResult );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexSlicer_X_CalcBase_BaseBase(Context context) {
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
      "Sales",
      null,
      "<CalculatedMember "
        + "name='H1 1997' "
        + "formula='Aggregate([Time].[1997].[Q1]:[Time].[1997].[Q2])' "
        + "dimension='Time' />" ));
     */
      withSchema(context, SchemaModifiers.FunctionTestModifier::new);
      String query =
      "SELECT "
        + "{[Measures].[Customer Count]} ON 0 "
        + "FROM [Sales] "
        + "WHERE CROSSJOIN ({[Time].[H1 1997],[Time].[1998].[Q1]} , {[Education Level].[Partial College],[Education "
        + "Level].[Partial High School]})";
    String expectedResult =
      "Axis #0:\n"
        + "{[Time].[H1 1997], [Education Level].[Partial College]}\n"
        + "{[Time].[H1 1997], [Education Level].[Partial High School]}\n"
        + "{[Time].[1998].[Q1], [Education Level].[Partial College]}\n"
        + "{[Time].[1998].[Q1], [Education Level].[Partial High School]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 1,671\n";
    assertQueryReturns(context.getConnection(), query, expectedResult );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexSlicer_Calc_ComplexAxis(Context context) {
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
      "Sales",
      null,
      "<CalculatedMember "
        + "name='H1 1997' "
        + "formula='Aggregate([Time].[1997].[Q1]:[Time].[1997].[Q2])' "
        + "dimension='Time' />"
        + "<CalculatedMember "
        + "name='Partial' "
        + "formula='Aggregate([Education Level].[Partial College]:[Education Level].[Partial High School])' "
        + "dimension='Education Level' />" ));
      */
      withSchema(context, SchemaModifiers.FunctionTestModifier2::new);

      String query =
      "SELECT "
        + "{[Measures].[Customer Count]} ON 0, "
        + "{[Time].[H1 1997], [Time].[1997].[Q1]} ON 1 "
        + "FROM [Sales] "
        + "WHERE "
        + "{[Education Level].[Partial]} ";
    String expectedResult =
      "Axis #0:\n"
        + "{[Education Level].[Partial]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Axis #2:\n"
        + "{[Time].[H1 1997]}\n"
        + "{[Time].[1997].[Q1]}\n"
        + "Row #0: 1,671\n"
        + "Row #1: 1,173\n";
    assertQueryReturns(context.getConnection(), query, expectedResult );
  }

  @Disabled //TODO need investigate
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testComplexSlicer_Unsupported(Context context) {
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
      "Sales",
      null,
      "<CalculatedMember "
        + "name='H1 1997' "
        + "formula='([Time].[1997].[Q1] - [Time].[1997].[Q2])' "
        + "dimension='Time' />" ));
     */
      withSchema(context, SchemaModifiers.FunctionTestModifier::new);
      String query =
      "SELECT "
        + "{[Measures].[Customer Count]} ON 0, "
        + "{[Education Level].Members} ON 1 "
        + "FROM [Sales] "
        + "WHERE {[Time].[H1 1997],[Time].[1998].[Q1]}";
    final String errorMessagePattern =
      "Calculated member 'H1 1997' is not supported within a compound predicate";
    assertQueryThrows(context, query, errorMessagePattern );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExisting(Context context) {
    // basic test
    assertQueryReturns(context.getConnection(),
      "with \n"
        + "  member measures.ExistingCount as\n"
        + "  count(Existing [Product].[Product Subcategory].Members)\n"
        + "  select {measures.ExistingCount} on 0,\n"
        + "  [Product].[Product Family].Members on 1\n"
        + "  from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[ExistingCount]}\n"
        + "Axis #2:\n"
        + "{[Product].[Drink]}\n"
        + "{[Product].[Food]}\n"
        + "{[Product].[Non-Consumable]}\n"
        + "Row #0: 8\n"
        + "Row #1: 62\n"
        + "Row #2: 32\n" );
    // same as exists+currentMember
    assertQueryReturns(context.getConnection(),
      "with member measures.StaticCount as\n"
        + "  count([Product].[Product Subcategory].Members)\n"
        + "  member measures.WithExisting as\n"
        + "  count(Existing [Product].[Product Subcategory].Members)\n"
        + "  member measures.WithExists as\n"
        + "  count(Exists([Product].[Product Subcategory].Members, [Product].CurrentMember))\n"
        + "  select {measures.StaticCount, measures.WithExisting, measures.WithExists} on 0,\n"
        + "  [Product].[Product Family].Members on 1\n"
        + "  from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[StaticCount]}\n"
        + "{[Measures].[WithExisting]}\n"
        + "{[Measures].[WithExists]}\n"
        + "Axis #2:\n"
        + "{[Product].[Drink]}\n"
        + "{[Product].[Food]}\n"
        + "{[Product].[Non-Consumable]}\n"
        + "Row #0: 102\n"
        + "Row #0: 8\n"
        + "Row #0: 8\n"
        + "Row #1: 102\n"
        + "Row #1: 62\n"
        + "Row #1: 62\n"
        + "Row #2: 102\n"
        + "Row #2: 32\n"
        + "Row #2: 32\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExistingCalculatedMeasure(Context context) {
    // sorry about the mess, this came from Analyzer
    assertQueryReturns(context.getConnection(),
      "WITH \n"
        + "SET [*NATIVE_CJ_SET] AS 'FILTER({[Time.Weekly].[All Time.Weeklys].[1997].[2],[Time.Weekly].[All Time"
        + ".Weeklys].[1997].[24]}, NOT ISEMPTY ([Measures].[Store Sales]) OR NOT ISEMPTY ([Measures]"
        + ".[CALCULATED_MEASURE_1]))' \n"
        + "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],[Time.Weekly].CURRENTMEMBER.ORDERKEY,BASC,ANCESTOR([Time"
        + ".Weekly].CURRENTMEMBER,[Time.Weekly].[Year]).ORDERKEY,BASC)'\n"
        + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0],[Measures].[CALCULATED_MEASURE_1]}'\n"
        + "SET [*BASE_MEMBERS__Time.Weekly_] AS '{[Time.Weekly].[All Time.Weeklys].[1997].[2],[Time.Weekly].[All Time"
        + ".Weeklys].[1997].[24]}'\n"
        + "SET [*CJ_ROW_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Time.Weekly].CURRENTMEMBER)})'\n"
        + "MEMBER [Measures].[CALCULATED_MEASURE_1] AS 'SetToStr( EXISTING [Time.Weekly].[Week].Members )'\n"
        + "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Store Sales]', FORMAT_STRING = '#,###.00', "
        + "SOLVE_ORDER=500\n"
        + "SELECT\n"
        + "[*BASE_MEMBERS__Measures_] ON COLUMNS\n"
        + ", NON EMPTY\n"
        + "[*SORTED_ROW_AXIS] ON ROWS\n"
        + "FROM [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[*FORMATTED_MEASURE_0]}\n"
        + "{[Measures].[CALCULATED_MEASURE_1]}\n"
        + "Axis #2:\n"
        + "{[Time.Weekly].[1997].[2]}\n"
        + "{[Time.Weekly].[1997].[24]}\n"
        + "Row #0: 19,756.43\n"
        + "Row #0: {[Time.Weekly].[1997].[2]}\n"
        + "Row #1: 11,371.84\n"
        + "Row #1: {[Time.Weekly].[1997].[24]}\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExistingCalculatedMeasureCompoundSlicer(Context context) {
    // basic test
    assertQueryReturns(context.getConnection(),
      "with \n"
        + "  member measures.subcategorystring as SetToStr( EXISTING [Product].[Product Subcategory].Members)\n"
        + "  select { measures.subcategorystring } on 0\n"
        + "  from [Sales]\n"
        + "  where {[Product].[Drink].[Alcoholic Beverages].[Beer and Wine]} ",
      "Axis #0:\n"
        + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine]}\n"
        + "Axis #1:\n"
        + "{[Measures].[subcategorystring]}\n"
        + "Row #0: {[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer], [Product].[Drink].[Alcoholic "
        + "Beverages].[Beer and Wine].[Wine]}\n" );

    assertQueryReturns(context.getConnection(),
      "with MEMBER [Measures].[*CALCULATED_MEASURE_1] AS 'SetToStr( EXISTING [Product].[Product Category].Members )'\n"
        + " SELECT {[Measures].[*CALCULATED_MEASURE_1]} ON COLUMNS\n"
        + " FROM [Sales]\n"
        + " WHERE {[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer], [Product].[Drink].[Alcoholic "
        + "Beverages].[Beer and Wine].[Wine], [Product].[Food].[Eggs].[Eggs] } ",
      "Axis #0:\n"
        + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]}\n"
        + "{[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine]}\n"
        + "{[Product].[Food].[Eggs].[Eggs]}\n"
        + "Axis #1:\n"
        + "{[Measures].[*CALCULATED_MEASURE_1]}\n"
        + "Row #0: {[Product].[Drink].[Alcoholic Beverages].[Beer and Wine], [Product].[Food].[Eggs].[Eggs]}\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExistingAggSet(Context context) {
    // aggregate simple set
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Measures].[Edible Sales] AS \n"
        + "Aggregate( Existing {[Product].[Drink], [Product].[Food]}, Measures.[Unit Sales] )\n"
        + "SELECT {Measures.[Unit Sales], Measures.[Edible Sales]} ON 0,\n"
        + "{ [Product].[Product Family].Members, [Product].[All Products] } ON 1\n"
        + "FROM [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Edible Sales]}\n"
        + "Axis #2:\n"
        + "{[Product].[Drink]}\n"
        + "{[Product].[Food]}\n"
        + "{[Product].[Non-Consumable]}\n"
        + "{[Product].[All Products]}\n"
        + "Row #0: 24,597\n"
        + "Row #0: 24,597\n"
        + "Row #1: 191,940\n"
        + "Row #1: 191,940\n"
        + "Row #2: 50,236\n"
        + "Row #2: \n"
        + "Row #3: 266,773\n"
        + "Row #3: 216,537\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExistingGenerateAgg(Context context) {
    // generate overrides existing context
    assertQueryReturns(context.getConnection(),
      "WITH SET BestOfFamilies AS\n"
        + "  Generate( [Product].[Product Family].Members,\n"
        + "            TopCount( Existing [Product].[Brand Name].Members, 10, Measures.[Unit Sales]) ) \n"
        + "MEMBER Measures.[Top 10 Brand Sales] AS Aggregate(Existing BestOfFamilies, Measures.[Unit Sales])"
        + "MEMBER Measures.[Rest Brand Sales] AS Aggregate( Except(Existing [Product].[Brand Name].Members, Existing "
        + "BestOfFamilies), Measures.[Unit Sales])"
        + "SELECT { Measures.[Unit Sales], Measures.[Top 10 Brand Sales], Measures.[Rest Brand Sales] } ON 0,\n"
        + "       {[Product].[Product Family].Members} ON 1\n"
        + "FROM [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "{[Measures].[Top 10 Brand Sales]}\n"
        + "{[Measures].[Rest Brand Sales]}\n"
        + "Axis #2:\n"
        + "{[Product].[Drink]}\n"
        + "{[Product].[Food]}\n"
        + "{[Product].[Non-Consumable]}\n"
        + "Row #0: 24,597\n"
        + "Row #0: 9,448\n"
        + "Row #0: 15,149\n"
        + "Row #1: 191,940\n"
        + "Row #1: 32,506\n"
        + "Row #1: 159,434\n"
        + "Row #2: 50,236\n"
        + "Row #2: 8,936\n"
        + "Row #2: 41,300\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExistingGenerateOverrides(Context context) {
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER Measures.[StaticSumNC] AS\n"
        + " 'Sum(Generate([Product].[Non-Consumable],"
        + "    Existing [Product].[Product Department].Members), Measures.[Unit Sales])'\n"
        + "SELECT { Measures.[StaticSumNC], Measures.[Unit Sales] } ON 0,\n"
        + "    NON EMPTY {[Product].[Product Family].Members} ON 1\n"
        + "FROM [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[StaticSumNC]}\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Product].[Drink]}\n"
        + "{[Product].[Food]}\n"
        + "{[Product].[Non-Consumable]}\n"
        + "Row #0: 50,236\n"
        + "Row #0: 24,597\n"
        + "Row #1: 50,236\n"
        + "Row #1: 191,940\n"
        + "Row #2: 50,236\n"
        + "Row #2: 50,236\n" );
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER Measures.[StaticSumNC] AS\n"
        + " 'Sum(Generate([Product].[Product Family].Members,"
        + "    Existing [Product].[Product Department].Members), Measures.[Unit Sales])'\n"
        + "SELECT { Measures.[StaticSumNC], Measures.[Unit Sales] } ON 0,\n"
        + "    NON EMPTY {[Product].[Non-Consumable]} ON 1\n"
        + "FROM [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[StaticSumNC]}\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Product].[Non-Consumable]}\n"
        + "Row #0: 266,773\n"
        + "Row #0: 50,236\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExistingVirtualCube(Context context) {
    // this should ideally return 14 for both,
    // but being coherent with exists is good enough
    assertQueryReturns(context.getConnection(),
      "WITH MEMBER [Measures].[Count Exists] AS Count(exists( [Time.Weekly].[Week].Members, [Time.Weekly]"
        + ".CurrentMember ) )\n"
        + " MEMBER [Measures].[Count Existing] AS Count(existing [Time.Weekly].[Week].Members)\n"
        + "SELECT\n"
        + "{[Measures].[Count Exists], [Measures].[Count Existing]}\n"
        + "ON 0\n"
        + "FROM [Warehouse and Sales]\n"
        + "WHERE [Time].[1997].[Q2]",
      "Axis #0:\n"
        + "{[Time].[1997].[Q2]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Count Exists]}\n"
        + "{[Measures].[Count Existing]}\n"
        + "Row #0: 104\n"
        + "Row #0: 104\n" );
  }

  /**
   * Generates a string containing all dimensions except those given. Useful as an argument to {@link
   * #assertExprDependsOn(String, String)}.
   *
   * @return string containing all dimensions except those given
   */
  String allHiersExcept( String... hiers ) {
    for ( String hier : hiers ) {
      assert contains( AllHiers, hier ) : "unknown hierarchy " + hier;
    }
    StringBuilder buf = new StringBuilder( "{" );
    int j = 0;
    for ( String hier : AllHiers ) {
      if ( !contains( hiers, hier ) ) {
        if ( j++ > 0 ) {
          buf.append( ", " );
        }
        buf.append( hier );
      }
    }
    buf.append( "}" );
    return buf.toString();
  }

  private boolean contains( String[] a, String s ) {
    for ( String anA : a ) {
      if ( anA.equals( s ) ) {
        return true;
      }
    }
    return false;
  }

  private String allHiers() {
    return allHiersExcept();
  }
}
