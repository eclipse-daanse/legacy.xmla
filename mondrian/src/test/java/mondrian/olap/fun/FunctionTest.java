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

import static mondrian.olap.Util.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertAxisThrows;
import static org.opencube.junit5.TestUtil.assertBooleanExprReturns;
import static org.opencube.junit5.TestUtil.assertExprDependsOn;
import static org.opencube.junit5.TestUtil.assertExprThrows;
import static org.opencube.junit5.TestUtil.assertMemberExprDependsOn;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertQueryThrows;
import static org.opencube.junit5.TestUtil.assertStubbedEqualsVerbose;
import static org.opencube.junit5.TestUtil.compileExpression;
import static org.opencube.junit5.TestUtil.executeExpr;
import static org.opencube.junit5.TestUtil.executeExprRaw;
import static org.opencube.junit5.TestUtil.isDefaultNullMemberRepresentation;
import static org.opencube.junit5.TestUtil.withSchema;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.function.core.FunctionPrinter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mondrian.olap.SystemWideProperties;
import mondrian.olap.Util;
import mondrian.rolap.RolapCatalogCache;
import mondrian.rolap.SchemaModifiers;
import mondrian.util.Bug;

//import mondrian.spi.DialectManager;

/**
 * <code>FunctionTest</code> tests the functions defined in
 * {@link BuiltinFunTable}.
 *
 * @author gjohnson
 */
public class FunctionTest {//extends FoodMartTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger( FunctionTest.class );
  private static final int NUM_EXPECTED_FUNCTIONS = 301;

  public static final String[] AllHiers = {
          "[Measures]",
          "[Store].[Store]",
          "[Store Size in SQFT].[Store Size in SQFT]",
          "[Store Type].[Store Type]",
          "[Time].[Time]",
          "[Time].[Weekly]",
          "[Product].[Product]",
          "[Promotion Media].[Promotion Media]",
          "[Promotions].[Promotions]",
          "[Customers].[Customers]",
          "[Education Level].[Education Level]",
          "[Gender].[Gender]",
          "[Marital Status].[Marital Status]",
          "[Yearly Income].[Yearly Income]"
  };

  public static final String months =
    "[Time].[Time].[1997].[Q1].[1]\n"
      + "[Time].[Time].[1997].[Q1].[2]\n"
      + "[Time].[Time].[1997].[Q1].[3]\n"
      + "[Time].[Time].[1997].[Q2].[4]\n"
      + "[Time].[Time].[1997].[Q2].[5]\n"
      + "[Time].[Time].[1997].[Q2].[6]\n"
      + "[Time].[Time].[1997].[Q3].[7]\n"
      + "[Time].[Time].[1997].[Q3].[8]\n"
      + "[Time].[Time].[1997].[Q3].[9]\n"
      + "[Time].[Time].[1997].[Q4].[10]\n"
      + "[Time].[Time].[1997].[Q4].[11]\n"
      + "[Time].[Time].[1997].[Q4].[12]";

  public static final String quarters =
    "[Time].[Time].[1997].[Q1]\n"
      + "[Time].[Time].[1997].[Q2]\n"
      + "[Time].[Time].[1997].[Q3]\n"
      + "[Time].[Time].[1997].[Q4]";

  public static final String year1997 = "[Time].[Time].[1997]";

  public static final String hierarchized1997 =
    year1997
      + "\n"
      + "[Time].[Time].[1997].[Q1]\n"
      + "[Time].[Time].[1997].[Q1].[1]\n"
      + "[Time].[Time].[1997].[Q1].[2]\n"
      + "[Time].[Time].[1997].[Q1].[3]\n"
      + "[Time].[Time].[1997].[Q2]\n"
      + "[Time].[Time].[1997].[Q2].[4]\n"
      + "[Time].[Time].[1997].[Q2].[5]\n"
      + "[Time].[Time].[1997].[Q2].[6]\n"
      + "[Time].[Time].[1997].[Q3]\n"
      + "[Time].[Time].[1997].[Q3].[7]\n"
      + "[Time].[Time].[1997].[Q3].[8]\n"
      + "[Time].[Time].[1997].[Q3].[9]\n"
      + "[Time].[Time].[1997].[Q4]\n"
      + "[Time].[Time].[1997].[Q4].[10]\n"
      + "[Time].[Time].[1997].[Q4].[11]\n"
      + "[Time].[Time].[1997].[Q4].[12]";

  public static final String NullNumericExpr =
    " ([Measures].[Unit Sales],"
      + "   [Customers].[All Customers].[USA].[CA].[Bellflower], "
      + "   [Product].[All Products].[Drink].[Alcoholic Beverages]."
      + "[Beer and Wine].[Beer].[Good].[Good Imported Beer])";

  private static final String TimeWeekly = "[Time].[Weekly]";



  @BeforeEach
  public void beforeEach() {

  }

  @AfterEach
  public void afterEach() {
    SystemWideProperties.instance().populateInitial();
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
      + "{[Time].[Time].[1997].[Q1]}\n"
      + "{[Time].[Time].[1997].[Q2]}\n"
      + "{[Time].[Time].[1997].[Q3]}\n"
      + "{[Time].[Time].[1997].[Q4]}\n"
      + "Row #0: \n"
      + "Row #1: \n"
      + "Row #2: \n"
      + "Row #3: \n";
    TestUtil.assertQueryReturns(context.getConnectionWithDefaultRole(), query, expected );
  }


  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNumericLiteral(Context context) {
    TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales", "2", "2" );
    if ( false ) {
      // The test is currently broken because the value 2.5 is formatted
      // as "2". TODO: better default format string
      TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales", "2.5", "2.5" );
    }
     TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales", "-10.0", "-10" );
    TestUtil.assertExprDependsOn(context.getConnectionWithDefaultRole(), "1.5", "{}" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testStringLiteral(Context context) {
    // single-quoted string
    if ( false ) {
      // TODO: enhance parser so that you can include a quoted string
      //   inside a WITH MEMBER clause
      TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales", "'foobar'", "foobar" );
    }
    // double-quoted string
    TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales", "\"foobar\"", "foobar" );
    // literals don't depend on any dimensions
    TestUtil.assertExprDependsOn(context.getConnectionWithDefaultRole(), "\"foobar\"", "{}" );
  }




  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNullMember(Context context) {
    // MSAS fails here, but Mondrian doesn't.
    TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
      "[Gender].[All Gender].Parent.Level.UniqueName",
      "[Gender].[Gender].[(All)]" );

    // MSAS fails here, but Mondrian doesn't.
    TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
      "[Gender].[All Gender].Parent.Hierarchy.UniqueName", "[Gender].[Gender]" );

    // MSAS fails here, but Mondrian doesn't.
    TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
      "[Gender].[All Gender].Parent.Dimension.UniqueName", "[Gender]" );

    // MSAS succeeds too
    TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
      "[Gender].[All Gender].Parent.Children.Count", "0" );

    if ( isDefaultNullMemberRepresentation() ) {
      // MSAS returns "" here.
      TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
        "[Gender].[All Gender].Parent.UniqueName", "[Gender].[Gender].[#null]" );

      // MSAS returns "" here.
      TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
        "[Gender].[All Gender].Parent.Name", "#null" );
    }
  }

  /**
   * Tests use of NULL literal to generate a null cell value. Testcase is from bug 1440344.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNullValue(Context context) {
    TestUtil.assertQueryReturns(context.getConnectionWithDefaultRole(),
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
        + "{[Product].[Product].[Drink].[Alcoholic Beverages]}\n"
        + "{[Product].[Product].[Drink].[Beverages]}\n"
        + "{[Product].[Product].[Drink].[Dairy]}\n"
        + "{[Product].[Product].[Food].[Baked Goods]}\n"
        + "{[Product].[Product].[Food].[Baking Goods]}\n"
        + "{[Product].[Product].[Food].[Breakfast Foods]}\n"
        + "{[Product].[Product].[Food].[Canned Foods]}\n"
        + "{[Product].[Product].[Food].[Canned Products]}\n"
        + "{[Product].[Product].[Food].[Dairy]}\n"
        + "{[Product].[Product].[Food].[Deli]}\n"
        + "{[Product].[Product].[Food].[Eggs]}\n"
        + "{[Product].[Product].[Food].[Frozen Foods]}\n"
        + "{[Product].[Product].[Food].[Meat]}\n"
        + "{[Product].[Product].[Food].[Produce]}\n"
        + "{[Product].[Product].[Food].[Seafood]}\n"
        + "{[Product].[Product].[Food].[Snack Foods]}\n"
        + "{[Product].[Product].[Food].[Snacks]}\n"
        + "{[Product].[Product].[Food].[Starchy Foods]}\n"
        + "{[Product].[Product].[Non-Consumable].[Carousel]}\n"
        + "{[Product].[Product].[Non-Consumable].[Checkout]}\n"
        + "{[Product].[Product].[Non-Consumable].[Health and Hygiene]}\n"
        + "{[Product].[Product].[Non-Consumable].[Household]}\n"
        + "{[Product].[Product].[Non-Consumable].[Periodicals]}\n"
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
    Connection connection = context.getConnectionWithDefaultRole();
    TestUtil.assertExprReturns(connection, "Sales", "NULL*1", "" );
    TestUtil.assertExprReturns(connection, "Sales", "1*NULL", "" );
    TestUtil.assertExprReturns(connection, "Sales", "NULL*NULL", "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNullInAddition(Context context) {
    Connection connection = context.getConnectionWithDefaultRole();
    TestUtil.assertExprReturns(connection, "Sales", "1+NULL", "1" );
    TestUtil.assertExprReturns(connection, "Sales", "NULL+1", "1" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNullInSubtraction(Context context) {
    Connection connection = context.getConnectionWithDefaultRole();
    TestUtil.assertExprReturns(connection, "Sales", "1-NULL", "1" );
    TestUtil.assertExprReturns(connection, "Sales", "NULL-1", "-1" );
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

    TestUtil.assertQueryReturns(context.getConnectionWithDefaultRole(),
      "WITH MEMBER [Measures].[Foo] AS 'Iif(IsEmpty([Measures].[Unit Sales]), 5, [Measures].[Unit Sales])'\n"
        + "SELECT {[Store].[USA].[WA].children} on columns\n"
        + "FROM Sales\n"
        + "WHERE ([Time].[1997].[Q4].[12],\n"
        + " [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth].[Portsmouth "
        + "Imported Beer],\n"
        + " [Measures].[Foo])",
      desiredResult );

    TestUtil.assertQueryReturns(context.getConnectionWithDefaultRole(),
      "WITH MEMBER [Measures].[Foo] AS 'Iif([Measures].[Unit Sales] IS EMPTY, 5, [Measures].[Unit Sales])'\n"
        + "SELECT {[Store].[USA].[WA].children} on columns\n"
        + "FROM Sales\n"
        + "WHERE ([Time].[1997].[Q4].[12],\n"
        + " [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth].[Portsmouth "
        + "Imported Beer],\n"
        + " [Measures].[Foo])",
      desiredResult );

    TestUtil.assertQueryReturns(context.getConnectionWithDefaultRole(),
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
  void testQueryWithoutValidMeasure(Context context) {
    TestUtil.assertQueryReturns(context.getConnectionWithDefaultRole(),
      "with\n"
        + "member measures.[without VM] as ' [measures].[unit sales] '\n"
        + "select {measures.[without VM] } on 0,\n"
        + "[Warehouse].[Country].members on 1 from [warehouse and sales]\n",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[without VM]}\n"
        + "Axis #2:\n"
        + "{[Warehouse].[Warehouse].[Canada]}\n"
        + "{[Warehouse].[Warehouse].[Mexico]}\n"
        + "{[Warehouse].[Warehouse].[USA]}\n"
        + "Row #0: \n"
        + "Row #1: \n"
        + "Row #2: \n" );
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
    assertQueryReturns(context.getConnectionWithDefaultRole(),
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
        + "{[Measures].[Declining Stores Count], [Time].[Time].[1998].[Q3], [Store].[Store].[XL_QZX]}\n"
        + "Axis #1:\n"
        + "{[Product].[Product].[All Products]}\n"
        + "{[Product].[Product].[Drink]}\n"
        + "{[Product].[Product].[Food]}\n"
        + "{[Product].[Product].[Non-Consumable]}\n"
        + "Row #0: .00\n"
        + "Row #0: .00\n"
        + "Row #0: .00\n"
        + "Row #0: .00\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBug715177(Context context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
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
        + "{[Product].[Product].[Drink].[Alcoholic Beverages]}\n"
        + "{[Product].[Product].[Drink].[Beverages]}\n"
        + "{[Product].[Product].[Drink].[Dairy]}\n"
        + "{[Product].[Product].[Non-Consumable].[Other]}\n"
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
    assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
      "{[Store].[USA].[CA].children, [Store].[USA]}",
      "[Store].[Store].[USA].[CA].[Alameda]\n"
        + "[Store].[Store].[USA].[CA].[Beverly Hills]\n"
        + "[Store].[Store].[USA].[CA].[Los Angeles]\n"
        + "[Store].[Store].[USA].[CA].[San Diego]\n"
        + "[Store].[Store].[USA].[CA].[San Francisco]\n"
        + "[Store].[Store].[USA]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testTuple(Context context) {
		assertExprReturns(context.getConnectionWithDefaultRole(),
				"([Gender].[M], " + "[Time].[Time].Children.Item(2), " + "[Measures].[Unit Sales])", "33,249");
		// Calc calls MemberValue with 3 args -- more efficient than
		// constructing a tuple.
		String expr = "([Gender].[M], [Time].[Time].Children.Item(2), [Measures].[Unit Sales])";
		String expectedCalc = """
org.eclipse.daanse.olap.calc.base.type.tuplebase.MemberArrayValueCalc(type=SCALAR, resultStyle=VALUE, callCount=0, callMillis=0)
    org.eclipse.daanse.olap.calc.base.constant.ConstantMemberCalc(type=MemberType<member=[Gender].[Gender].[M]>, resultStyle=VALUE_NOT_NULL, callCount=0, callMillis=0)
    org.eclipse.daanse.olap.function.def.set.setitem.SetItemFunDef$3(type=MemberType<hierarchy=[Time].[Time]>, resultStyle=VALUE, callCount=0, callMillis=0)
        org.eclipse.daanse.olap.function.def.set.children.ChildrenCalc(type=SetType<MemberType<hierarchy=[Time].[Time]>>, resultStyle=LIST, callCount=0, callMillis=0)
            org.eclipse.daanse.olap.function.def.hierarchy.member.HierarchyCurrentMemberFixedCalc(type=MemberType<hierarchy=[Time].[Time]>, resultStyle=VALUE, callCount=0, callMillis=0)
        org.eclipse.daanse.olap.calc.base.constant.ConstantIntegerCalc(type=DecimalType(0), resultStyle=VALUE_NOT_NULL, callCount=0, callMillis=0)
    org.eclipse.daanse.olap.calc.base.constant.ConstantMemberCalc(type=MemberType<member=[Measures].[Unit Sales]>, resultStyle=VALUE_NOT_NULL, callCount=0, callMillis=0)
							""";
		assertExprCompilesTo(context.getConnectionWithDefaultRole(), expr, expectedCalc);
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
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "([Gender], [Time].[Time])",
      "266,773" );

    // can coerce hierarchy to member
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "([Gender].[M], " + TimeWeekly + ")", "135,215" );

    // coerce args (hierarchy, member, member, dimension)
    assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
      "{([Time].[Weekly], [Measures].[Store Sales], [Marital Status].[M], [Promotion Media])}",
      "{[Time].[Weekly].[All Weeklys], [Measures].[Store Sales], [Marital Status].[Marital Status].[M], [Promotion Media].[Promotion Media].[All "
        + "Media]}" );

    // usage of different hierarchies in the [Time] dimension
    assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
      "{([Time].[Weekly], [Measures].[Store Sales], [Marital Status].[M], [Time].[Time])}",
      "{[Time].[Weekly].[All Weeklys], [Measures].[Store Sales], [Marital Status].[Marital Status].[M], [Time].[Time].[1997]}" );

    // two usages of the [Time].[Weekly] hierarchy

    assertAxisThrows(context.getConnectionWithDefaultRole(),
      "{([Time].[Weekly], [Measures].[Store Sales], [Marital Status].[M], [Time].[Weekly])}",
      "Tuple contains more than one member of hierarchy '[Time].[Weekly]'." , "Sales");

    // cannot coerce integer to member
    assertAxisThrows(context.getConnectionWithDefaultRole(),
      "{([Gender].[M], 123)}",
      "No function matches signature '(<Member>, <Numeric Expression>)'" , "Sales");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTupleItem(Context context) {
    assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
      "([Time].[1997].[Q1].[1], [Customers].[All Customers].[USA].[OR], [Gender].[All Gender].[M]).item(2)",
      "[Gender].[Gender].[M]" );

    assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
      "([Time].[1997].[Q1].[1], [Customers].[All Customers].[USA].[OR], [Gender].[All Gender].[M]).item(1)",
      "[Customers].[Customers].[USA].[OR]" );

    assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
      "{[Time].[1997].[Q1].[1]}.item(0)",
      "[Time].[Time].[1997].[Q1].[1]" );

    assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
      "{[Time].[1997].[Q1].[1]}.Item(0).Item(0)",
      "[Time].[Time].[1997].[Q1].[1]" );

    // given out of bounds index, item returns null
    assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
      "([Time].[1997].[Q1].[1], [Customers].[All Customers].[USA].[OR], [Gender].[All Gender].[M]).item(-1)",
      "" );

    // given out of bounds index, item returns null
    assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
      "([Time].[1997].[Q1].[1], [Customers].[All Customers].[USA].[OR], [Gender].[All Gender].[M]).item(500)",
      "" );

    // empty set
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "Filter([Gender].members, 1 = 0).Item(0)",
      "" );

    // empty set of unknown type
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "{}.Item(3)",
      "" );

    // past end of set
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "{[Gender].members}.Item(4)",
      "" );

    // negative index
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "{[Gender].members}.Item(-50)",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTupleAppliedToUnknownHierarchy(Context context) {
    // manifestation of bug 1735821
    assertQueryReturns(context.getConnectionWithDefaultRole(),
      "with \n"
        + "member [Product].[Test] as '([Product].[Food],Dimensions(0).defaultMember)' \n"
        + "select \n"
        + "{[Product].[Test], [Product].[Food]} on columns, \n"
        + "{[Measures].[Store Sales]} on rows \n"
        + "from Sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Product].[Product].[Test]}\n"
        + "{[Product].[Product].[Food]}\n"
        + "Axis #2:\n"
        + "{[Measures].[Store Sales]}\n"
        + "Row #0: 191,940.00\n"
        + "Row #0: 409,035.59\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTupleDepends(Context context) {
    assertMemberExprDependsOn(context.getConnectionWithDefaultRole(),
      "([Store].[USA], [Gender].[F])", "{}" );

    assertMemberExprDependsOn(context.getConnectionWithDefaultRole(),
      "([Store].[USA], [Gender])", "{[Gender].[Gender]}" );

    // in a scalar context, the expression depends on everything except
    // the explicitly stated dimensions
    assertExprDependsOn(context.getConnectionWithDefaultRole(),
      "([Store].[USA], [Gender])",
      allHiersExcept( "[Store].[Store]" ) );

    // The result should be all dims except [Gender], but there's a small
    // bug in MemberValueCalc.dependsOn where we escalate 'might depend' to
    // 'depends' and we return that it depends on all dimensions.
    assertExprDependsOn(context.getConnectionWithDefaultRole(),
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
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "Filter([Gender].members, 1 = 0).Item(0).Dimension.Name",
      "Gender" );

    // MSAS returns error here.
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "Filter([Gender].members, 1 = 0).Item(0).Parent",
      "" );
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "(Filter([Store].members, 0 = 0).Item(0).Item(0),"
        + "Filter([Store].members, 0 = 0).Item(0).Item(0))",
      "266,773" );

    if ( isDefaultNullMemberRepresentation() ) {
      // MSAS returns error here.
      assertExprReturns(context.getConnectionWithDefaultRole(),
        "Filter([Gender].members, 1 = 0).Item(0).Name",
        "#null" );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testTupleNull(Context context) {
    // if a tuple contains any null members, it evaluates to null
    assertQueryReturns(context.getConnectionWithDefaultRole(),
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
        + "{[Gender].[Gender].[M], [Store].[Store].[All Stores]}\n"
        + "Row #0: 135,215\n" );

    // the set function eliminates tuples which are wholly or partially
    // null
    assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
      "([Gender].parent, [Marital Status]),\n" // part null
        + " ([Gender].[M], [Marital Status].parent),\n" // part null
        + " ([Gender].parent, [Marital Status].parent),\n" // wholly null
        + " ([Gender].[M], [Marital Status])", // not null
      "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[All Marital Status]}" );

    if ( isDefaultNullMemberRepresentation() ) {
      // The tuple constructor returns a null tuple if one of its
      // arguments is null -- and the Item function returns null if the
      // tuple is null.
      assertExprReturns(context.getConnectionWithDefaultRole(),
        "([Gender].parent, [Marital Status]).Item(0).Name",
        "#null" );
      assertExprReturns(context.getConnectionWithDefaultRole(),
        "([Gender].parent, [Marital Status]).Item(1).Name",
        "#null" );
    }
  }

  public static void checkDataResults(
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
	context.getCatalogCache().clear();
    // Should return Beverly Hills in California.
    assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
      "[Store].[Store City].[Beverly Hills]",
      "[Store].[Store].[USA].[CA].[Beverly Hills]" );

    // There are two months named "1" in the time dimension: one
    // for 1997 and one for 1998.  <Level>.<Member> should return
    // the first one.
    assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales", "[Time].[Month].[1]", "[Time].[Time].[1997].[Q1].[1]" );

    // Shouldn't be able to find a member named "Q1" on the month level.
    assertAxisThrows(context.getConnectionWithDefaultRole(),
      "[Time].[Month].[Q1]",
      "MDX object '[Time].[Month].[Q1]' not found in cube", "Sales");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCaseTestMatch(Context context) {
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "CASE WHEN 1=0 THEN \"first\" WHEN 1=1 THEN \"second\" WHEN 1=2 THEN \"third\" ELSE \"fourth\" END",
      "second" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCaseTestMatchElse(Context context) {
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "CASE WHEN 1=0 THEN \"first\" ELSE \"fourth\" END",
      "fourth" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCaseTestMatchNoElse(Context context) {
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "CASE WHEN 1=0 THEN \"first\" END",
      "" );
  }

  /**
   * Testcase for bug 1799391, "Case Test function throws class cast exception"
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCaseTestReturnsMemberBug1799391(Context context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
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
        + "{[Product].[Product].[CaseTest]}\n"
        + "Axis #2:\n"
        + "{[Gender].[Gender].[M]}\n"
        + "Row #0: 131,558\n" );

    assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
      "CASE WHEN 1+1 = 2 THEN [Gender].[F] ELSE [Gender].[F].Parent END",
      "[Gender].[Gender].[F]" );

    // try case match for good measure
    assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
      "CASE 1 WHEN 2 THEN [Gender].[F] ELSE [Gender].[F].Parent END",
      "[Gender].[Gender].[All Gender]" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCaseMatch(Context context) {
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "CASE 2 WHEN 1 THEN \"first\" WHEN 2 THEN \"second\" WHEN 3 THEN \"third\" ELSE \"fourth\" END",
      "second" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCaseMatchElse(Context context) {
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "CASE 7 WHEN 1 THEN \"first\" ELSE \"fourth\" END",
      "fourth" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCaseMatchNoElse(Context context) {
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "CASE 8 WHEN 0 THEN \"first\" END",
      "" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCaseTypeMismatch(Context context) {
    // type mismatch between case and else
    assertAxisThrows(context.getConnectionWithDefaultRole(),
      "CASE 1 WHEN 1 THEN 2 ELSE \"foo\" END",
      "No function matches signature", "Sales" );
    // type mismatch between case and case
    assertAxisThrows(context.getConnectionWithDefaultRole(),
      "CASE 1 WHEN 1 THEN 2 WHEN 2 THEN \"foo\" ELSE 3 END",
      "No function matches signature", "Sales" );
    // type mismatch between value and case
    assertAxisThrows(context.getConnectionWithDefaultRole(),
      "CASE 1 WHEN \"foo\" THEN 2 ELSE 3 END",
      "No function matches signature", "Sales" );
    // non-boolean condition
    assertAxisThrows(context.getConnectionWithDefaultRole(),
      "CASE WHEN 1 = 2 THEN 3 WHEN 4 THEN 5 ELSE 6 END",
      "No function matches signature", "Sales" );
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
      assertExprReturns(context.getConnectionWithDefaultRole(),
        "case 1 when 0 then 1.5\n"
          + " else ([Gender].[M], [Measures].[Unit Sales]) end",
        "135,215" );
    }

    // "case when" variant always worked
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "case when 1=0 then 1.5\n"
        + " else ([Gender].[M], [Measures].[Unit Sales]) end",
      "135,215" );

    // case 2: cannot deduce type (tuple x) vs. (tuple y). Should be able
    // to deduce that the result type is tuple-type<member-type<Gender>,
    // member-type<Measures>>.
    if ( false ) {
      assertExprReturns(context.getConnectionWithDefaultRole(),
        "case when 1=0 then ([Gender].[M], [Measures].[Store Sales])\n"
          + " else ([Gender].[M], [Measures].[Unit Sales]) end",
        "xxx" );
    }

    // case 3: mixture of member & tuple. Should be able to deduce that
    // result type is an expression.
    if ( false ) {
      assertExprReturns(context.getConnectionWithDefaultRole(),
        "case when 1=0 then ([Measures].[Store Sales])\n"
          + " else ([Gender].[M], [Measures].[Unit Sales]) end",
        "xxx" );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMod(Context context) {
    // the following tests are consistent with excel xp

    assertExprReturns(context.getConnectionWithDefaultRole(), "mod(11, 3)", "2" );
    assertExprReturns(context.getConnectionWithDefaultRole(), "mod(-12, 3)", "0" );

    // can handle non-ints, using the formula MOD(n, d) = n - d * INT(n / d)
    assertExprReturns(context.getConnectionWithDefaultRole(), "mod(7.2, 3)", 1.2, 0.0001 );
    assertExprReturns(context.getConnectionWithDefaultRole(), "mod(7.2, 3.2)", .8, 0.0001 );
    assertExprReturns(context.getConnectionWithDefaultRole(), "mod(7.2, -3.2)", -2.4, 0.0001 );

    // per Excel doc "sign of result is same as divisor"
    assertExprReturns(context.getConnectionWithDefaultRole(), "mod(3, 2)", "1" );
    assertExprReturns(context.getConnectionWithDefaultRole(), "mod(-3, 2)", "1" );
    assertExprReturns(context.getConnectionWithDefaultRole(), "mod(3, -2)", "-1" );
    assertExprReturns(context.getConnectionWithDefaultRole(), "mod(-3, -2)", "-1" );

    assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
      "mod(4, 0)",
      "java.lang.ArithmeticException: / by zero" );
    assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
      "mod(0, 0)",
      "java.lang.ArithmeticException: / by zero" );
  }


  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testString(Context context) {
    // The String(Integer,Char) function requires us to implicitly cast a
    // string to a char.
    assertQueryReturns(context.getConnectionWithDefaultRole(),
      "with member measures.x as 'String(3, \"yahoo\")'\n"
        + "select measures.x on 0 from [Sales]",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[x]}\n"
        + "Row #0: yyy\n" );
    // String is converted to char by taking first character
    assertExprReturns(context.getConnectionWithDefaultRole(), "String(3, \"yahoo\")", "yyy" ); // SSAS agrees
    // Integer is converted to char by converting to string and taking first
    // character
    if ( Bug.Ssas2005Compatible ) {
      // SSAS2005 can implicitly convert an integer (32) to a string, and
      // then to a char by taking the first character. Mondrian requires
      // an explicit cast.
      assertExprReturns(context.getConnectionWithDefaultRole(), "String(3, 32)", "333" );
      assertExprReturns(context.getConnectionWithDefaultRole(), "String(8, -5)", "--------" );
    } else {
      assertExprReturns(context.getConnectionWithDefaultRole(), "String(3, Cast(32 as string))", "333" );
      assertExprReturns(context.getConnectionWithDefaultRole(), "String(8, Cast(-5 as string))", "--------" );
    }
    // Error if length<0
    assertExprReturns(context.getConnectionWithDefaultRole(), "String(0, 'x')", "" ); // SSAS agrees
    assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
      "String(-1, 'x')", "NegativeArraySizeException" ); // SSAS agrees
    assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
      "String(-200, 'x')", "NegativeArraySizeException" ); // SSAS agrees
  }

    public static void checkNullOp(Connection connection, final String op ) {
        assertBooleanExprReturns(connection, "Sales", " 0 " + op + " " + NullNumericExpr, false );
        assertBooleanExprReturns(connection, "Sales", NullNumericExpr + " " + op + " 0", false );
        assertBooleanExprReturns(connection, "Sales",
            NullNumericExpr + " " + op + " " + NullNumericExpr, false );
    }

  /**
   * Executes a scalar expression, and asserts that the result is as expected. For example, <code>assertExprReturns ("1
   * + 2", "3")</code> should succeed.
   */
   public static void assertExprReturns(Connection connection, String expr, String expected ) {
    String actual = executeExpr(connection, "Sales", expr);
    assertEquals( expected, actual );
  }

  /**
   * Executes a scalar expression, and asserts that the result is within delta of the expected result.
   *
   * @param expr     MDX scalar expression
   * @param expected Expected value
   * @param delta    Maximum allowed deviation from expected value
   */
 public static void assertExprReturns(Connection connection,
    String expr, double expected, double delta ) {
    Object value = executeExprRaw(connection, "Sales", expr).getValue();

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
      compileExpression(connection, expr, true, "Sales");
    final int expDeps =
      connection.getContext().getConfigValue(ConfigConstants.TEST_EXP_DEPENDENCIES, ConfigConstants.TEST_EXP_DEPENDENCIES_DEFAULT_VALUE, Integer.class);
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
  public static void assertAxisCompilesTo(Connection connection,
    String expr,
    String expectedCalc ) {
    final String actualCalc =
      compileExpression(connection, expr, false, "Sales");
    final int expDeps =
      connection.getContext().getConfigValue(ConfigConstants.TEST_EXP_DEPENDENCIES, ConfigConstants.TEST_EXP_DEPENDENCIES_DEFAULT_VALUE, Integer.class);
    if ( expDeps > 0 ) {
      // Don't bother checking the compiled output if we are also
      // testing dependencies. The compiled code will have extra
      // 'DependencyTestingCalc' instances embedded in it.
      return;
    }
    assertStubbedEqualsVerbose( expectedCalc, actualCalc );
  }


  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCast(Context context) {
    // NOTE: Some of these tests fail with 'cannot convert ...', and they
    // probably shouldn't. Feel free to fix the conversion.
    // -- jhyde, 2006/9/3

    // From double to integer.  MONDRIAN-1631
    Cell cell = executeExprRaw(context.getConnectionWithDefaultRole(), "Sales", "Cast(1.4 As Integer)" );
    assertEquals(Integer.class, cell.getValue().getClass(),
            "Cast to Integer resulted in wrong datatype\n"
                    + cell.getValue().getClass().toString());
    assertEquals(1, cell.getValue() );

    // From integer
    // To integer (trivial)
    assertExprReturns(context.getConnectionWithDefaultRole(), "0 + Cast(1 + 2 AS Integer)", "3" );
    // To String
    assertExprReturns(context.getConnectionWithDefaultRole(), "'' || Cast(1 + 2 AS String)", "3.0" );
    // To Boolean
    assertExprReturns(context.getConnectionWithDefaultRole(), "1=1 AND Cast(1 + 2 AS Boolean)", "true" );
    assertExprReturns(context.getConnectionWithDefaultRole(), "1=1 AND Cast(1 - 1 AS Boolean)", "false" );


    // From boolean
    // To String
    assertExprReturns(context.getConnectionWithDefaultRole(), "'' || Cast((1 = 1 AND 1 = 2) AS String)", "false" );

    // This case demonstrates the relative precedence of 'AS' in 'CAST'
    // and 'AS' for creating inline named sets. See also bug MONDRIAN-648.
//    discard( Bug.BugMondrian648Fixed );
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "'xxx' || Cast(1 = 1 AND 1 = 2 AS String)",
      "xxxfalse" );

    // To boolean (trivial)
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "1=1 AND Cast((1 = 1 AND 1 = 2) AS Boolean)",
      "false" );

    assertExprReturns(context.getConnectionWithDefaultRole(),
      "1=1 OR Cast(1 = 1 AND 1 = 2 AS Boolean)",
      "true" );

    // From null : should not throw exceptions since RolapResult.executeBody
    // can receive NULL values when the cell value is not loaded yet, so
    // should return null instead.
    // To Integer : Expect to return NULL

    // Expect to return NULL
    assertExprReturns(context.getConnectionWithDefaultRole(), "0 * Cast(NULL AS Integer)", "" );

    // To Numeric : Expect to return NULL
    // Expect to return NULL
    assertExprReturns(context.getConnectionWithDefaultRole(), "0 * Cast(NULL AS Numeric)", "" );

    // To String : Expect to return "null"
    assertExprReturns(context.getConnectionWithDefaultRole(), "'' || Cast(NULL AS String)", "null" );

    // To Boolean : Expect to return NULL, but since FunUtil.BooleanNull
    // does not implement three-valued boolean logic yet, this will return
    // false
    assertExprReturns(context.getConnectionWithDefaultRole(), "1=1 AND Cast(NULL AS Boolean)", "false" );

    // Double is not allowed as a type
    assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
      "Cast(1 AS Double)",
      "Unknown type 'Double'; values are NUMERIC, STRING, BOOLEAN" );

    // An integer constant is not allowed as a type
    assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
      "Cast(1 AS 5)",
      "Encountered an error at (or somewhere around) input:1:11" );

    assertExprReturns(context.getConnectionWithDefaultRole(), "Cast('tr' || 'ue' AS boolean)", "true" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCastAndNull(Context context) {
	    // To Boolean : Expect to return NULL, but since FunUtil.BooleanNull
	    // does not implement three-valued boolean logic yet, this will return
	    // false
	    assertExprReturns(context.getConnectionWithDefaultRole(), "1=1 AND Cast(NULL AS Boolean)", "false" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCastNull(Context context) {
	    // To Boolean : Expect to return NULL, but since FunUtil.BooleanNull
	    // does not implement three-valued boolean logic yet, this will return
	    // false
	    assertExprReturns(context.getConnectionWithDefaultRole(), "Cast(NULL AS Boolean)", "false" );
  }
  /**
   * Testcase for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-524"> MONDRIAN-524, "VB functions: expected
   * primitive type, got java.lang.Object"</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCastBug524(Context context) {
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "Cast(Int([Measures].[Store Sales] / 3600) as String)",
      "157" );
  }

  /**
   * Tests {@link org.eclipse.daanse.olap.api.function.FunctionTable#getFunctionInfos()}, but more importantly, generates an HTML table of all
   * implemented functions into a file called "functions.html". You can manually include that table in the <a
   * href="{@docRoot}/../mdx.html">MDX specification</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDumpFunctions(Context context) throws IOException {
    FunctionService functionService = context.getFunctionService();
    assertEquals( NUM_EXPECTED_FUNCTIONS, functionService.getResolvers().size() );

  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLeftFunctionWithValidArguments(Context context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
      "select filter([Store].MEMBERS,"
        + "Left([Store].CURRENTMEMBER.Name, 4)=\"Bell\") on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLeftFunctionWithLengthValueZero(Context context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
      "select filter([Store].MEMBERS,"
        + "Left([Store].CURRENTMEMBER.Name, 0)=\"\" And "
        + "[Store].CURRENTMEMBER.Name = \"Bellingham\") on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLeftFunctionWithLengthValueEqualToStringLength(Context context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
      "select filter([Store].MEMBERS,"
        + "Left([Store].CURRENTMEMBER.Name, 10)=\"Bellingham\") "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLeftFunctionWithLengthMoreThanStringLength(Context context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
      "select filter([Store].MEMBERS,"
        + "Left([Store].CURRENTMEMBER.Name, 20)=\"Bellingham\") "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLeftFunctionWithZeroLengthString(Context context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
      "select filter([Store].MEMBERS,Left(\"\", 20)=\"\" "
        + "And [Store].CURRENTMEMBER.Name = \"Bellingham\") "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[Store].[USA].[WA].[Bellingham]}\n"
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
    assertQueryReturns(context.getConnectionWithDefaultRole(),
      "select filter([Store].MEMBERS,"
        + "[Store].CURRENTMEMBER.Name = \"Bellingham\""
        + "And Mid(\"Bellingham\", 4, 6) = \"lingha\")"
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMidFunctionWithZeroLengthStringArgument(Context context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
      "select filter([Store].MEMBERS,"
        + "[Store].CURRENTMEMBER.Name = \"Bellingham\""
        + "And Mid(\"\", 4, 6) = \"\")"
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMidFunctionWithLengthArgumentLargerThanStringLength(Context context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
      "select filter([Store].MEMBERS,"
        + "[Store].CURRENTMEMBER.Name = \"Bellingham\""
        + "And Mid(\"Bellingham\", 4, 20) = \"lingham\")"
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMidFunctionWithStartIndexGreaterThanStringLength(Context context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
      "select filter([Store].MEMBERS,"
        + "[Store].CURRENTMEMBER.Name = \"Bellingham\""
        + "And Mid(\"Bellingham\", 20, 2) = \"\")"
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMidFunctionWithStartIndexZeroFails(Context context) {
    // Note: SSAS 2005 treats start<=0 as 1, therefore gives different
    // result for this query. We favor the VBA spec over SSAS 2005.
    if ( Bug.Ssas2005Compatible ) {
      assertQueryReturns(context.getConnectionWithDefaultRole(),
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
    assertQueryReturns(context.getConnectionWithDefaultRole(),
      "select filter([Store].MEMBERS,"
        + "[Store].CURRENTMEMBER.Name = \"Bellingham\""
        + "And Mid(\"Bellingham\", 1, 2) = \"Be\")"
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[Store].[USA].[WA].[Bellingham]}\n"
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
    assertQueryReturns(context.getConnectionWithDefaultRole(),
      "select filter([Store].MEMBERS,"
        + "[Store].CURRENTMEMBER.Name = \"Bellingham\""
        + "And Mid(\"Bellingham\", 2) = \"ellingham\")"
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLenFunctionWithNonEmptyString(Context context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
      "select filter([Store].MEMBERS, "
        + "Len([Store].CURRENTMEMBER.Name) = 3) on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[Store].[USA]}\n"
        + "Row #0: 266,773\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLenFunctionWithAnEmptyString(Context context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
      "select filter([Store].MEMBERS,Len(\"\")=0 "
        + "And [Store].CURRENTMEMBER.Name = \"Bellingham\") "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testInStrFunctionWithValidArguments(Context context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
      "select filter([Store].MEMBERS,InStr(\"Bellingham\", \"ingha\")=5 "
        + "And [Store].CURRENTMEMBER.Name = \"Bellingham\") "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }


  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testInStrFunctionWithEmptyString1(Context context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
      "select filter([Store].MEMBERS,InStr(\"\", \"ingha\")=0 "
        + "And [Store].CURRENTMEMBER.Name = \"Bellingham\") "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testInStrFunctionWithEmptyString2(Context context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
      "select filter([Store].MEMBERS,InStr(\"Bellingham\", \"\")=1 "
        + "And [Store].CURRENTMEMBER.Name = \"Bellingham\") "
        + "on 0 from sales",
      "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Store].[Store].[USA].[WA].[Bellingham]}\n"
        + "Row #0: 2,237\n" );
  }

  private static void printHtml( PrintWriter pw, String s ) {
    final String escaped = StringEscapeUtils.escapeHtml4(s);
    pw.print( escaped );
  }

  // The following methods test VBA functions. They don't test all of them,
  // because the raw methods are tested in VbaTest, but they test the core
  // functionalities like error handling and operator overloading.

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVbaBasic(Context context) {
    // Exp is a simple function: one arg.
    assertExprReturns(context.getConnectionWithDefaultRole(), "exp(0)", "1" );
    assertExprReturns(context.getConnectionWithDefaultRole(), "exp(1)", Math.E, 0.00000001 );
    assertExprReturns(context.getConnectionWithDefaultRole(), "exp(-2)", 1d / ( Math.E * Math.E ), 0.00000001 );

    }
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVbaBasic1(Context context) {
	  // If any arg is null, result is null.
	    assertExprReturns(context.getConnectionWithDefaultRole(), "exp(null)", "" );

  }
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVbaBasic2(Context context) {
	  // If any arg is null, result is null.
	    assertExprReturns(context.getConnectionWithDefaultRole(), "exp(cast(null as numeric))", "" );

  }

  // Test a VBA function with variable number of args.
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVbaOverloading(Context context) {
    assertExprReturns(context.getConnectionWithDefaultRole(), "replace('xyzxyz', 'xy', 'a')", "azaz" );
    assertExprReturns(context.getConnectionWithDefaultRole(), "replace('xyzxyz', 'xy', 'a', 2)", "xyzaz" );
    assertExprReturns(context.getConnectionWithDefaultRole(), "replace('xyzxyz', 'xy', 'a', 1, 1)", "azxyz" );
  }

  // Test VBA exception handling
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVbaExceptions(Context context) {
    assertExprThrows(context.getConnectionWithDefaultRole(), "Sales",
      "right(\"abc\", -4)",
      Util.IBM_JVM
        ? "StringIndexOutOfBoundsException: null"
        : "StringIndexOutOfBoundsException: Range [7, 3) out of bounds for length 3");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVbaDateTime(Context context) {
    // function which returns date
    assertExprReturns(context.getConnectionWithDefaultRole(),
      "Format(DateSerial(2006, 4, 29), \"Long Date\")",
      "Saturday, April 29, 2006" );
    // function with date parameter
    assertExprReturns(context.getConnectionWithDefaultRole(), "Year(DateSerial(2006, 4, 29))", "2,006" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExcelPi(Context context) {
    // The PI function is defined in the Excel class.
    assertExprReturns(context.getConnectionWithDefaultRole(), "Pi()", "3" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testExcelPower(Context context) {
    assertExprReturns(context.getConnectionWithDefaultRole(), "Power(8, 0.333333)", 2.0, 0.01 );
    assertExprReturns(context.getConnectionWithDefaultRole(), "Power(-2, 0.5)", Double.NaN, 0.001 );
  }

  // Comment from the bug: the reason for this is that in AbstractExpCompiler
  // in the compileInteger method we are casting an IntegerCalc into a
  // DoubleCalc and there is no check for IntegerCalc in the NumericType
  // conditional path.
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBug1881739(Context context) {
    assertExprReturns(context.getConnectionWithDefaultRole(), "LEFT(\"TEST\", LEN(\"TEST\"))", "TEST" );
  }

  /**
   * Testcase for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-296"> MONDRIAN-296, "Cube getTimeDimension use
   * when Cube has no Time dimension"</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCubeTimeDimensionFails(Context context) {
    assertQueryThrows(context.getConnectionWithDefaultRole(),
      "select LastPeriods(1) on columns from [Store]",
      "'LastPeriods', no time dimension" );
    assertQueryThrows(context.getConnectionWithDefaultRole(),
      "select OpeningPeriod() on columns from [Store]",
      "'OpeningPeriod', no time dimension" );
    assertQueryThrows(context.getConnectionWithDefaultRole(),
      "select OpeningPeriod([Store Type]) on columns from [Store]",
      "'OpeningPeriod', no time dimension" );
    assertQueryThrows(context.getConnectionWithDefaultRole(),
      "select ClosingPeriod() on columns from [Store]",
      "'ClosingPeriod', no time dimension" );
    assertQueryThrows(context.getConnectionWithDefaultRole(),
      "select ClosingPeriod([Store Type]) on columns from [Store]",
      "'ClosingPeriod', no time dimension" );
    assertQueryThrows(context.getConnectionWithDefaultRole(),
      "select ParallelPeriod() on columns from [Store]",
      "'ParallelPeriod', no time dimension" );
    assertQueryThrows(context.getConnectionWithDefaultRole(),
      "select PeriodsToDate() on columns from [Store]",
      "'PeriodsToDate', no time dimension" );
    assertQueryThrows(context.getConnectionWithDefaultRole(),
      "select Mtd() on columns from [Store]",
      "'Mtd', no time dimension" );
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
        + "{[Gender].[Gender].[All Gender]}\n"
        + "{[Gender].[Gender].[F]}\n"
        + "{[Gender].[Gender].[M]}\n"
        + "Row #0: 266,773\n"
        + "Row #1: 131,558\n"
        + "Row #2: 135,215\n";

    // hand written case
    assertQueryReturns(context.getConnectionWithDefaultRole(),
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
    assertQueryReturns(context.getConnectionWithDefaultRole(), buf.toString(), expected );
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
      + "{[Gender].[Gender].[All Gender]}\n"
      + "{[Gender].[Gender].[F]}\n"
      + "{[Gender].[Gender].[M]}\n"
      + "Row #0: 266,773\n"
      + "Row #1: 131,558\n"
      + "Row #2: 135,215\n";
    assertQueryReturns(context.getConnectionWithDefaultRole(), query, expected );
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
        + "{[Time].[Time].[1997].[Q2]}\n"
        + "{[Time].[Time].[1998].[Q1]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Axis #2:\n"
        + "{[Education Level].[Education Level].[All Education Levels]}\n"
        + "{[Education Level].[Education Level].[Bachelors Degree]}\n"
        + "{[Education Level].[Education Level].[Graduate Degree]}\n"
        + "{[Education Level].[Education Level].[High School Degree]}\n"
        + "{[Education Level].[Education Level].[Partial College]}\n"
        + "{[Education Level].[Education Level].[Partial High School]}\n"
        + "Row #0: 2,973\n"
        + "Row #1: 760\n"
        + "Row #2: 178\n"
        + "Row #3: 853\n"
        + "Row #4: 273\n"
        + "Row #5: 909\n";
    assertQueryReturns(context.getConnectionWithDefaultRole(), query, expectedResult );
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
        + "{[Time].[Time].[H1 1997]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Axis #2:\n"
        + "{[Education Level].[Education Level].[All Education Levels]}\n"
        + "{[Education Level].[Education Level].[Bachelors Degree]}\n"
        + "{[Education Level].[Education Level].[Graduate Degree]}\n"
        + "{[Education Level].[Education Level].[High School Degree]}\n"
        + "{[Education Level].[Education Level].[Partial College]}\n"
        + "{[Education Level].[Education Level].[Partial High School]}\n"
        + "Row #0: 4,257\n"
        + "Row #1: 1,109\n"
        + "Row #2: 240\n"
        + "Row #3: 1,237\n"
        + "Row #4: 394\n"
        + "Row #5: 1,277\n";
    assertQueryReturns(context.getConnectionWithDefaultRole(), query, expectedResult );
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
        + "{[Time].[Time].[H1 1997]}\n"
        + "{[Time].[Time].[1998].[Q1]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Axis #2:\n"
        + "{[Education Level].[Education Level].[All Education Levels]}\n"
        + "{[Education Level].[Education Level].[Bachelors Degree]}\n"
        + "{[Education Level].[Education Level].[Graduate Degree]}\n"
        + "{[Education Level].[Education Level].[High School Degree]}\n"
        + "{[Education Level].[Education Level].[Partial College]}\n"
        + "{[Education Level].[Education Level].[Partial High School]}\n"
        + "Row #0: 4,257\n"
        + "Row #1: 1,109\n"
        + "Row #2: 240\n"
        + "Row #3: 1,237\n"
        + "Row #4: 394\n"
        + "Row #5: 1,277\n";
    assertQueryReturns(context.getConnectionWithDefaultRole(), query, expectedResult );
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
        + "{[Time].[Time].[1998].[Q1]}\n"
        + "{[Time].[Time].[H1 1997]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Axis #2:\n"
        + "{[Education Level].[Education Level].[All Education Levels]}\n"
        + "{[Education Level].[Education Level].[Bachelors Degree]}\n"
        + "{[Education Level].[Education Level].[Graduate Degree]}\n"
        + "{[Education Level].[Education Level].[High School Degree]}\n"
        + "{[Education Level].[Education Level].[Partial College]}\n"
        + "{[Education Level].[Education Level].[Partial High School]}\n"
        + "Row #0: 4,257\n"
        + "Row #1: 1,109\n"
        + "Row #2: 240\n"
        + "Row #3: 1,237\n"
        + "Row #4: 394\n"
        + "Row #5: 1,277\n";
    assertQueryReturns(context.getConnectionWithDefaultRole(), query, expectedResult );
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
        + "{[Time].[Time].[H1 1997], [Education Level].[Education Level].[Partial College]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 394\n";
    assertQueryReturns(context.getConnectionWithDefaultRole(), query, expectedResult );
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
        + "{[Time].[Time].[H1 1997], [Education Level].[Education Level].[Partial]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 1,671\n";
    assertQueryReturns(context.getConnectionWithDefaultRole(), query, expectedResult );
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
        + "{[Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial College]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 278\n";
    assertQueryReturns(context.getConnectionWithDefaultRole(), query, expectedResult );
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
        + "{[Time].[Time].[H1 1997], [Education Level].[Education Level].[Partial College]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 394\n";
    assertQueryReturns(context.getConnectionWithDefaultRole(), query, expectedResult );
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
        + "{[Time].[Time].[H1 1997], [Education Level].[Education Level].[Partial]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 1,671\n";
    assertQueryReturns(context.getConnectionWithDefaultRole(), query, expectedResult );
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
        + "{[Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial College]}\n"
        + "{[Time].[Time].[1997].[Q2], [Education Level].[Education Level].[Partial College]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 394\n";
    assertQueryReturns(context.getConnectionWithDefaultRole(), query, expectedResult );
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
        + "{[Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial College]}\n"
        + "{[Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial High School]}\n"
        + "{[Time].[Time].[1997].[Q2], [Education Level].[Education Level].[Partial College]}\n"
        + "{[Time].[Time].[1997].[Q2], [Education Level].[Education Level].[Partial High School]}\n"
        + "{[Time].[Time].[1998].[Q1], [Education Level].[Education Level].[Partial College]}\n"
        + "{[Time].[Time].[1998].[Q1], [Education Level].[Education Level].[Partial High School]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 1,671\n";
    assertQueryReturns(context.getConnectionWithDefaultRole(), query, expectedResult );
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
        + "{[Time].[Time].[H1 1997], [Education Level].[Education Level].[Partial College]}\n"
        + "{[Time].[Time].[1998].[Q1], [Education Level].[Education Level].[Partial College]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 394\n";
    assertQueryReturns(context.getConnectionWithDefaultRole(), query, expectedResult );
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
        + "{[Time].[Time].[H1 1997], [Education Level].[Education Level].[Partial College]}\n"
        + "{[Time].[Time].[H1 1997], [Education Level].[Education Level].[Partial High School]}\n"
        + "{[Time].[Time].[1998].[Q1], [Education Level].[Education Level].[Partial College]}\n"
        + "{[Time].[Time].[1998].[Q1], [Education Level].[Education Level].[Partial High School]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Row #0: 1,671\n";
    assertQueryReturns(context.getConnectionWithDefaultRole(), query, expectedResult );
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
        + "{[Education Level].[Education Level].[Partial]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n"
        + "Axis #2:\n"
        + "{[Time].[Time].[H1 1997]}\n"
        + "{[Time].[Time].[1997].[Q1]}\n"
        + "Row #0: 1,671\n"
        + "Row #1: 1,173\n";
    assertQueryReturns(context.getConnectionWithDefaultRole(), query, expectedResult );
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

  /**
   * Generates a string containing all dimensions except those given. Useful as an argument to {@link
   * #assertExprDependsOn(String, String)}.
   *
   * @return string containing all dimensions except those given
   */
  public static String allHiersExcept( String... hiers ) {
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

  private static boolean contains( String[] a, String s ) {
    for ( String anA : a ) {
      if ( anA.equals( s ) ) {
        return true;
      }
    }
    return false;
  }

  public static String allHiers() {
    return allHiersExcept();
  }
}
