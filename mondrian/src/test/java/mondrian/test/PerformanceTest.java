/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2020 Hitachi Vantara..  All rights reserved.
 */

package mondrian.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.executeAxis;
import static org.opencube.junit5.TestUtil.executeQuery;
import static org.opencube.junit5.TestUtil.hierarchyName;
import static org.opencube.junit5.TestUtil.withSchema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Position;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.olap.fun.sort.Sorter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mondrian.rolap.SchemaModifiers;
import  org.eclipse.daanse.olap.util.Bug;

/**
 * Various unit tests concerned with performance.
 *
 * @author jhyde
 * @since August 7, 2006
 */
public class PerformanceTest {
  /**
   * Certain tests are enabled only if logging is enabled at debug level or higher.
   */
  public static final Logger LOGGER =
    LoggerFactory.getLogger( PerformanceTest.class );

  @BeforeEach
  public void beforeEach() {
  }

  @AfterEach
  public void afterEach() {
      SystemWideProperties.instance().populateInitial();
  }
  /**
   * Test case for
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-550">
   * Bug MONDRIAN-550, "Performance bug with NON EMPTY and large axes"</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void  testBugMondrian550(Context<?> context) {
    getBugMondrian550Schema(context);
    final Statistician statistician =
      new Statistician( "testBugMondrian550" );
    for ( int i = 0; i < 10; i++ ) {
      checkBugMondrian550(context.getConnectionWithDefaultRole(), statistician );
    }
    statistician.printDurations();
  }

  private void checkBugMondrian550(
          Connection connection,
          Statistician statistician ) {
    long start = System.currentTimeMillis();
    // On my Latitude D630:
    // Takes 137 seconds before bug fixed.
    // Takes 13 seconds after bug fixed.
    // jdk1.6 marmalade 3.2 14036  17,899 12,889 ms
    // jdk1.6 marmalade main 14036 15,845 15,180 ms
    // jdk1.6 marmalade main 14052 14,284 ms first, 1419 +- 12 ms
    // jdk1.7 marmite   main 14770 3,527 ms first, 1325 +- 18 ms
    // jdk1.7 marmite   main 14771 3,319 ms first, 1305 +- 26 ms
    // jdk1.7 marmite   main 14772 3,721 ms first, 1321 +- 28 ms
    // jdk1.7 marmite   main 14773 3,421 ms first, 1298 +- 11 ms
    final Result result = executeQuery(connection,
      "select NON EMPTY {[Store Name sans All].Members} ON COLUMNS,\n"
        + "  NON EMPTY Hierarchize(Union({[ACC].[All]}, [ACC].[All].Children)) ON ROWS\n"
        + "from [Sales]\n"
        + "where ([Time].[1997].[Q4], [Measures].[EXP2])" );
    statistician.record( start );
    assertEquals( 13, result.getAxes()[ 0 ].getPositions().size() );
    assertEquals( 3262, result.getAxes()[ 1 ].getPositions().size() );
  }

  /**
   * As testBugMondrian550() but with tuples on the rows axis.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBugMondrian550Tuple(Context<?> context) {
    getBugMondrian550Schema(context);
    final Statistician statistician =
      new Statistician( "testBugMondrian550Tuple" );
    int n = LOGGER.isDebugEnabled() ? 10 : 2;
    Connection connection = context.getConnectionWithDefaultRole();
    for ( int i = 0; i < n; i++ ) {
      checkBugMondrian550Tuple(connection, statistician );
    }
    statistician.printDurations();
  }

  private void checkBugMondrian550Tuple(
    Connection connection, Statistician statistician) {
    long start = System.currentTimeMillis();
    // On my Latitude D630:
    // Takes 252 seconds before bug fixed.
    // Takes 45 seconds after bug fixed.
    // jdk1.6 marmalade 3.2 14036  14,799 14,986 ms
    // jdk1.6 marmalade main 14036 20,839 20,331 ms
    // jdk1.6 marmalade main 14052  9,664 +- 49
    // jdk1.7 marmite   main 14770 10,228 +- 60 ms
    // jdk1.7 marmite   main 14771  9,742 +- 111 ms
    // jdk1.7 marmite   main 14772 10,512 +- 38 ms
    // jdk1.7 marmite   main 14773  9,544 +- 118 ms
    final Result result2 = executeQuery(connection,
      "select NON EMPTY {[Store Name sans All].Members} ON COLUMNS,\n"
        + "  NON EMPTY Hierarchize(Union({[ACC].[All]}, [ACC].[All].Children))\n"
        + "   * [Gender].Children ON ROWS\n"
        + "from [Sales]\n"
        + "where ([Time].[1997].[Q4], [Measures].[EXP2])" );
    statistician.record( start );
    assertEquals( 13, result2.getAxes()[ 0 ].getPositions().size() );
    assertEquals( 3263, result2.getAxes()[ 1 ].getPositions().size() );
  }

  void getBugMondrian550Schema(Context<?> context) {
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
      "Sales",
      "      <Dimension name=\"ACC\" caption=\"Account\" type=\"StandardDimension\" foreignKey=\"customer_id\">\n"
        + "         <Hierarchy hasAll=\"true\" allMemberName=\"All\" primaryKey=\"customer_id\">\n"
        + "            <Table name=\"customer\"/>\n"
        + "            <Level name=\"CODE\" caption=\"Account\" uniqueMembers=\"true\" column=\"account_num\" "
        + "type=\"String\"/>\n"
        + "         </Hierarchy>\n"
        + "      </Dimension>\n"
        + "      <Dimension name=\"Store Name sans All\" type=\"StandardDimension\" foreignKey=\"store_id\">\n"
        + "         <Hierarchy hasAll=\"false\" primaryKey=\"store_id\">\n"
        + "            <Table name=\"store\" />\n"
        + "            <Level name=\"Store Name\" uniqueMembers=\"true\" column=\"store_number\" type=\"Numeric\" "
        + "ordinalColumn=\"store_name\"/>\n"
        + "         </Hierarchy>\n"
        + "      </Dimension>\n",
      "      <CalculatedMember dimension=\"Measures\" name=\"EXP2_4\" formula=\"IIf([ACC].CurrentMember.Level.Ordinal"
        + " = [ACC].[All].Ordinal, Sum([ACC].[All].Children, [Measures].[Unit Sales]),     [Measures].[Unit Sales])"
        + "\"/>\n"
        + "      <CalculatedMember dimension=\"Measures\" name=\"EXP2\" formula=\"IIf(0 &#60; [Measures].[EXP2_4], "
        + "[Measures].[EXP2_4], NULL)\"/>\n" ));
     */
      withSchema(context, SchemaModifiers.PerformanceTestModifier1::new);
  }

  /**
   * Test case for
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-641">
   * Bug MONDRIAN-641</a>, "Large NON EMPTY result performs poorly with ResultStyle.ITERABLE".  Runs in ~10 seconds
   * with
   * ResultStyle.LIST, 99+ seconds with ITERABLE (on DELL Latitude D630).
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMondrianBug641(Context<?> context) {
    if ( !Bug.BugMondrian641Fixed ) {
      return;
    }
    long start = System.currentTimeMillis();
    Result result =
      executeQuery(context.getConnectionWithDefaultRole(),
        "select  non empty  {  crossjoin( customers.[city].members, "
          + "crossjoin( [store type].[store type].members,  "
          + "product.[product name].members)) }"
          + " on 0 from sales" );
    // jdk1.6 marmalade main 14036 287,940 518,349 ms
    printDuration( "testBugMondrian641", start );
    assertEquals( 51148, result.getAxes()[ 0 ].getPositions().size() );
  }

  /**
   * Tests performance when an MDX query contains a very large explicit set.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testVeryLargeExplicitSet(Context<?> context) {
    final Statistician[] statisticians = {
      // jdk1.6 mackerel access main old    5,000 ms
      // jdk1.6 marmalade 3.2 14036   4,376 4,055 ms
      // jdk1.6 marmalade main 14036  4,471 3,589 ms
      // jdk1.6 marmalade main 14037  4,400 ms
      // jdk1.6 marmalade main 14052  5,280 ms
      // jdk1.7 marmite   main 14770  1,189 ms first, 27 +- 4 ms
      // jdk1.7 marmite   main 14771  1,369 ms first, 24 +- 2 ms
      // jdk1.7 marmite   main 14773  1,003 ms first, 23 +- 1 ms
      new Statistician( "testVeryLargeExplicitSet: Execute axis" ),

      // Execute:
      // first:
      // jdk1.6 mackerel access old  75,000 ms
      // jdk1.6 marmalade main 14036 19,262 18,493 ms
      // jdk1.6 marmalade main 14037 19,000 ms
      // jdk1.6 marmalade 3.2 14036  18,710 19,077 ms
      // jdk1.6 marmalade main 14052 21,739 ms
      //
      // second:
      // jdk1.6 mackerel access old   65,000 ms
      // jdk1.6 marmalade main 14036     526 429 ms
      // jdk1.6 marmalade main 14037     800 400 ms
      // jdk1.6 marmalade 3.2 14036      313 406 ms
      // jdk1.6 marmalade main 14052     577 ms
      //
      // jdk1.7 marmite   main 14770 520 ms first, 88 +- 22 ms
      // jdk1.7 marmite   main 14771 555 ms first, 84 +- 20 ms
      // jdk1.7 marmite   main 14773 654 ms first, 76 +- 19 ms
      new Statistician( "testVeryLargeExplicitSet: Execute" ),

      // Param query:
      // first:
      // unknown revision mackerel  2,424 ms
      // jdk1.6 marmalade 3.2 14036    34 115 72 ms
      // jdk1.6 marmalade main 14036   66 107 ms
      // jdk1.6 marmalade main 14037  117 ms
      // jdk1.6 marmalade main 14052   47 ms
      //
      // second:
      // unknown revision mackerel    51 ms
      // jdk1.6 marmalade 3.2 14036   18 102 ms
      // jdk1.6 marmalade main 14036  86 105 95 ms
      // jdk1.6 marmalade main 14037 106 ms
      // jdk1.6 marmalade main 14052  21 ms
      //
      // jdk1.7 marmite   main 14770 46 ms first, 12 +- 2 ms
      // jdk1.7 marmite   main 14771 26 ms first, 10 +- 2 ms
      // jdk1.7 marmite   main 14773 43 ms first, 10 +- 2 ms
      new Statistician( "testVeryLargeExplicitSet: Param query" ),
    };
    for ( int i = 0; i < 10; i++ ) {
      checkVeryLargeExplicitSet(context.getConnectionWithDefaultRole(), statisticians);
    }
    for ( Statistician statistician : statisticians ) {
      statistician.printDurations();
    }
  }

  private void checkVeryLargeExplicitSet(Connection connection,
    Statistician[] statisticians) {
    Result result;
    long start = System.currentTimeMillis();
    final Axis axis = executeAxis(connection, "Sales", "Customers.Members" );
    statisticians[ 0 ].record( start );
    final List<Position> positionList = axis.getPositions();
    assertEquals( 10407, positionList.size() );

    // Take customers 0-2000 and 5000-7000. Using contiguous bursts,
    // Mondrian has a chance to optimize how it reads cells from the
    // database.
    List<Member> memberList = new ArrayList<>();
    for ( int i = 0; i < positionList.size(); i++ ) {
      Position position = positionList.get( i );
      ++i;
      if ( i < 2000 || i >= 5000 && i < 7000 ) {
        memberList.add( position.get( 0 ) );
      }
    }

    // Build a query with an explcit member list.
    if ( LOGGER.isDebugEnabled() ) {
      StringBuilder buf = new StringBuilder();
      for ( Member member : memberList ) {
        if ( buf.length() > 0 ) {
          buf.append( ", " );
        }
        buf.append( member );
      }
      final String mdx =
        "WITH SET [Selected Customers] AS {" + buf + "}\n"
          + "SELECT {[Measures].[Unit Sales],\n"
          + "        [Measures].[Store Sales]} on 0,\n"
          + "  [Selected Customers] on 1\n"
          + "FROM [Sales]";
      start = System.currentTimeMillis();
      result = executeQuery(connection, mdx);

      statisticians[ 1 ].record( start );
      assertEquals(
        memberList.size(),
        result.getAxes()[ 1 ].getPositions().size() );
    }

    // Much more efficient technique. Use a parameter, and bind to array.
    // Cuts out a lot of parsing, so takes 2.4s as opposed to 65s.
    Query query =
            connection.parseQuery(
        "WITH SET [Selected Customers]\n"
          + "  AS Parameter('Foo', [Customers], {}, 'Description')\n"
          + "SELECT {[Measures].[Unit Sales],\n"
          + "        [Measures].[Store Sales]} on 0,\n"
          + "  [Selected Customers] on 1\n"
          + "FROM [Sales]" );
    query.setParameter( "Foo", memberList );
    start = System.currentTimeMillis();
    result = connection.execute( query );

    statisticians[ 2 ].record( start );
    assertEquals(
      memberList.size(),
      result.getAxes()[ 1 ].getPositions().size() );
  }

  /**
   * Test case for
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-639">
   * Bug MONDRIAN-639, "RolapNamedSetEvaluator anon classes implement Iterable, causing performance regression from 2.4
   * in FunUtil.count()"</a>.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBugMondrian639(Context<?> context) {
    // unknown revision before fix mac-mini 233,000 ms
    // unknown revision after fix mac-mini    4,500 ms
    // jdk1.6 marmalade 3.2 14036             1,821 1,702 ms
    // jdk1.6 marmalade main 14036            2,185 3,208 1,431 ms
    // jdk1.6 marmalade main 14037            1,801 ms
    // jdk1.6 marmalade main 14052              396 +- 28 ms
    // jdk1.7 marmite   main 14770 478 ms first, 150 +- 8 ms
    // jdk1.7 marmite   main 14771 513 ms first, 152 +- 14 ms
    // jdk1.7 marmite   main 14773 523 ms first, 150 +- 5 ms
    final Statistician statistician =
      new Statistician( "testBugMondrian639" );
    for ( int i = 0; i < 20; i++ ) {
      checkBugMondrian639(context.getConnectionWithDefaultRole(), statistician);
    }
    statistician.printDurations();
  }

  private void checkBugMondrian639(Connection connection, Statistician statistician ) {
    long start = System.currentTimeMillis();
    Result result = executeQuery(connection,
      "WITH SET [cjoin] AS "
        + "crossjoin(customers.members, "
        + hierarchyName( "store type", "store type" )
        + ".[store type].members) "
        + "MEMBER [Measures].[total_available_count] "
        + "AS Format(COUNT([cjoin]), \"#####\") "
        + "SELECT"
        + "{[cjoin]} ON COLUMNS, "
        + "{[Measures].[total_available_count]} ON ROWS "
        + "FROM sales" );

    statistician.record( start );
    assertEquals( 62442, result.getAxes()[ 0 ].getPositions().size() );
    assertEquals( 1, result.getAxes()[ 1 ].getPositions().size() );
  }

  /**
   * Tests performance of a larger schema with a large number of result cells. Runs in 186 seconds without
   * nonAllPositions array in RolapEvaluator. Runs in 14 seconds when RolapEvaluator.getProperty uses
   * getNonAllMembers.
   * The performance boost gets more significant as the schema size grows.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBigResultsWithBigSchemaPerforms(Context<?> context) {
    if ( !LOGGER.isDebugEnabled() ) {
      return;
    }
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
        "Sales",
        TestUtil.repeatString(
          1000,
          "<Dimension name=\"Gender%d \" foreignKey=\"customer_id\">"
            + "  <Hierarchy hasAll=\"true\" allMemberName=\"All Gender\" primaryKey=\"customer_id\">"
            + "    <Table name=\"customer\"/>"
            + "    <Level name=\"Gender\" column=\"gender\" uniqueMembers=\"true\"/>"
            + "  </Hierarchy>"
            + "</Dimension>" ),
        null ));
     */
      withSchema(context, SchemaModifiers.PerformanceTestModifier2::new);
      String mdx =
      "with "
        + " member [Measures].[one] as '1'"
        + " member [Measures].[two] as '2'"
        + " member [Measures].[three] as '3'"
        + " member [Measures].[four] as '4'"
        + " member [Measures].[five] as '5'"
        + " select "
        + "{[Measures].[one],[Measures].[two],[Measures].[three],[Measures].[four],[Measures].[five]}"
        + " on 0, "
        + "Crossjoin([Customers].[name].members,[Store].[Store Name].members)"
        + " on 1 from sales";
    long start = System.currentTimeMillis();
    executeQuery(context.getConnectionWithDefaultRole(), mdx);

    // jdk1.6 marmalade 3.2 14036  23,588 23,426 ms
    // jdk1.6 marmalade main 14036 26,430 27,045 25,497 ms
    // jdk1.6 marmalade main 14037 26,893 ms
    // jdk1.6 marmalade main 14052 29,870 ms
    // jdk1.7 marmite   main 14770  2,877 ms
    // jdk1.7 marmite   main 14773  3,353 ms
    printDuration( "testBigResultsWithBigSchemaPerforms", start );
    assertTrue(true);
  }

  /**
   * Runs a query that performs a lot of in-memory calculation.
   *
   * <p>Timings (branch / change / host / DBMS / jdk / timings (s) / mean):
   * <ul>
   * <li>mondrian-3.2 13366 marmalade oracle jdk1.6 592 588 581 571 avg 583
   * <li>mondrian-3.2 13367 marmalade oracle jdk1.6 643 620 631 671 avg 641
   * <li>mondrian-3.2 13397 marmalade oracle jdk1.6 604 626
   * <li>mondrian-3.2 13467 marmalade oracle jdk1.6 610 574
   * <li>mondrian-3.2 13489 marmalade oracle jdk1.6 565 561 579 596 avg 575
   * <li>mondrian-3.2 13490 marmalade oracle jdk1.6 607 611 581 605 avg 601
   * <li>mondrian-3.2 xxxxx marmalade oracle jdk1.6 562 583 541 522 avg 552
   * <li>mondrian-3.2 14036 marmalade oracle jdk1.6 451 433
   * <li>mondrian     14036 marmalade oracle jdk1.6 598 552
   * <li>mondrian     14037 marmalade oracle jdk1.6 626 596
   * <li>mondrian     14052 marmalade oracle jdk1.6 454
   * <li>mondrian     14770 marmite   mysql  jdk1.7 &gt; 30 minutes
   * </ul>
   * Disabled by performance reason run >10 h
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  @Disabled
  void testInMemoryCalc(Context<?> context) {
    if ( !LOGGER.isDebugEnabled() ) {
      // Test is too expensive to run as part of standard regress.
      // Take 10h on hudson (MySQL)!!!
      return;
    }
    final String result =
      "Axis #0:\n"
        + "{[Time].[1997].[Q3]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Store Sales]}\n"
        + "{[Measures].[Typical Store Sales]}\n"
        + "{[Measures].[Ratio]}\n"
        + "Axis #2:\n"
        + "{[Product].[Food].[Baked Goods].[Bread].[Sliced Bread].[Modell].[Modell Rye Bread], [Customers].[USA].[OR]"
        + ".[Salem].[Joan Johnson]}\n"
        + "{[Product].[Non-Consumable].[Household].[Plastic Products].[Plastic Utensils].[Denny].[Denny Plastic "
        + "Knives], [Customers].[USA].[OR].[Lebanon].[Pat Pinkston]}\n"
        + "{[Product].[Food].[Starchy Foods].[Starchy Foods].[Rice].[Shady Lake].[Shady Lake Thai Rice], [Customers]"
        + ".[USA].[CA].[Grossmont].[Anne Silva]}\n"
        + "{[Product].[Food].[Canned Foods].[Canned Soup].[Soup].[Better].[Better Regular Ramen Soup], [Customers]"
        + ".[USA].[CA].[Coronado].[Robert Brink]}\n"
        + "{[Product].[Non-Consumable].[Health and Hygiene].[Bathroom Products].[Mouthwash].[Bird Call].[Bird Call "
        + "Laundry Detergent], [Customers].[USA].[CA].[Downey].[Eric Renn]}\n"
        + "Row #0: 19.65\n"
        + "Row #0: 3.12\n"
        + "Row #0: 6.30\n"
        + "Row #1: 15.56\n"
        + "Row #1: 2.80\n"
        + "Row #1: 5.56\n"
        + "Row #2: 11.24\n"
        + "Row #2: 2.10\n"
        + "Row #2: 5.35\n"
        + "Row #3: 11.22\n"
        + "Row #3: 2.46\n"
        + "Row #3: 4.56\n"
        + "Row #4: 6.33\n"
        + "Row #4: 1.71\n"
        + "Row #4: 3.70\n";
    final String mdx =
      "with member [Measures].[Typical Store Sales] as\n"
        + "  Max(\n"
        + "    [Customers].Siblings,\n"
        + "    Min(\n"
        + "      [Product].Siblings,\n"
        + "      Avg(\n"
        + "        [Time].Siblings,\n"
        + "        [Measures].[Store Sales])))\n"
        + "member [Measures].[Ratio] as\n"
        + "  [Measures].[Store Sales]\n"
        + "   / [Measures].[Typical Store Sales]\n"
        + "select\n"
        + "  {\n"
        + "    [Measures].[Store Sales],\n"
        + "    [Measures].[Typical Store Sales],\n"
        + "    [Measures].[Ratio]\n"
        + "  } on 0,\n"
        + "  TopCount(\n"
        + "    Filter(\n"
        + "      NonEmptyCrossJoin("
        + "        [Product].[Product Name].Members,\n"
        + "        [Customers].[Name].Members),\n"
        + "      [Measures].[Ratio] > 1.1\n"
        + "      and [Measures].[Store Sales] > 5),\n"
        + "    5,\n"
        + "    [Measures].[Ratio]) on 1\n"
        + "from [Sales]\n"
        + "where [Time].[1997].[Q3]";
    final long start = System.currentTimeMillis();
    assertQueryReturns(context.getConnectionWithDefaultRole(), mdx, result );
    printDuration( "in-memory calc", start );
  }

  /**
   * Test case for
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-843">
   * Bug MONDRIAN-843, where Filter is inefficient.</a>
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBugMondrian843(Context<?> context) {
    // On my core i7 laptop:
    // takes 2.5 seconds before bug fixed
    // takes 0.4 seconds after bug fixed
    // jdk1.6 marmalade 3.2 14036     826 ms
    // jdk1.6 marmalade main 14036  4,427 3,894 ms
    // jdk1.6 marmalade main 14052    800 ms
    // jdk1.7 marmite   main 14770   2159 ms (standalone)
    // jdk1.7 marmite   main 14771    266 ms (as part of suite)
    // jdk1.7 marmite   main 14773    181 ms
    long start = System.currentTimeMillis();
    executeQuery(context.getConnectionWithDefaultRole(),
      "WITH SET [filtered] AS "
        + "FILTER({customers.members, customers.members, customers.members, customers.members, customers.members}, "
        + "[Measures].[Unit Sales] > 100) "
        + "SELECT"
        + "{[Measures].[Unit Sales]} ON COLUMNS, "
        + "{[filtered]} ON ROWS "
        + "FROM sales" );
    printDuration( "testBugMondrian843", start );
    assertTrue(true);
  }

  /**
   * Testcase for bug
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-981">MONDRIAN-981,
   * "Poor performance when >=2 hierarchies are access-controlled with rollupPolicy=partial"</a>.
   * Disabled by performance reason run 1.8 h
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  @Disabled
  void testBugMondrian981(Context<?> context) {
    if ( !LOGGER.isDebugEnabled() ) {
      // Too slow to run as part of standard regress until bug is fixed.
      return;
    }
    // To see the cartesian-product nature of this bug, try commenting out
    // various of the following HierarchyGrants.
    // The query runs in about 2s with no access-controlled hierarchies,
    // then appromixately doubles as each is added (48s with 5 hierarchies).
    //
    // jdk1.7 marmite   main 14770   30,857 ms
    // jdk1.7 marmite   main 14771   29,083 ms
    withSchema(context, SchemaModifiers.PerformanceTestModifier3::new);
    assertQueryReturns(((TestContext)context).getConnection(List.of("Role1")),
      "with member [Measures].[Foo] as\n"
        + "Aggregate([Gender].Members * [Marital Status].Members * [Time].Members)\n"
        + "select from [Sales] where [Measures].[Foo]",
      "Axis #0:\n"
        + "{[Measures].[Foo]}\n"
        + "1,184,028" );
  }

  private static long printDuration( String desc, long t0 ) {
    final long t1 = System.currentTimeMillis();
    final long duration = t1 - t0;
    LOGGER.debug( desc + " took " + duration + " millis" );
    return duration;
  }



  /**
   * Tests performance of {@link Sorter#stablePartialSort}.
   *
   * <p>"Pedro's algorithm" was supplied as
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-1288">MONDRIAN-1288,
   * "Optimize stable partial sort when dataset is huge and limit is small"</a>.</p>
   *
   * <p>Parameters: N (number of elements in list), L (limit; number of
   * elements to return)</p>
   *
   * <p>Conclusions:</p>
   * <ul>
   * <li>Array sort is better when L is almost as large as N</li>
   * <li>If L is small, Pedro's algorithm is best; but it grows
   *     with O(L^2) and is unusable for L &gt; 10,000</li>
   * <li>If L is less than N / 20, Julian's algorithm is best</li>
   * <li>For L larger than N / 20, array sort is best</li>
   * <li>Each algorithm improves number of comparisons: Julian's algorithm
   *     is the best, with N + L log L + comparisons</li>
   * </ul>
   */
  @Test
  void testStablePartialSort() {
    final int N = 1000000; // should be 1M in checked-in code
    final int limit = 10;  // should be 10 in checked-in code
    final int runCount = 10;

    final List<Integer> originals = new ArrayList<>( N );
    Random random = new Random( 1235 );
    for ( int i = 0; i < N; i++ ) {
      originals.add( random.nextInt( N ) );
    }

    for ( StableSortAlgorithm algorithm : StableSortAlgorithm.values() ) {
      algorithm.foo( runCount, originals, limit );
    }
  }

  private enum StableSortAlgorithm {
    // First, regular array sort.
    // N=1M, L=10: 338 first; 247.4 +- 0.8; 246 min; 251 max
    // N=10M, L=10: 3988 first; 3934 +- 10; 3923 min; 4705 max
    // N=10M, L=10K: 4037 first; 3974 +- 51; 3878 min; 4848 max
    // N=10M, L=1M: 4006 first; 3919 +- 39; 3868 min; 4774 max
    // N=50M, L=2.5M: 36350 first; 27977 +- 446; 27603 min; 29452 max
    ARRAY( 18640353 ) {
      @Override
	<T extends Comparable<T>> List<T> sort(
        List<T> list, Comparator<T> comp, int limit ) {
        return Sorter.stablePartialSortArray( list, comp, limit );
      }
    },

    // Marc's original partial sort algorithm based on Quicksort.
    // N=1M, L=10: 194 first; 53.0 +- 1.8; 50 min; 73 max
    // N=10M, L=10: 2998 first; 622 +- 83; 465 min; 1843 max
    // N=10M, L=10K: 867 first; 608 +- 38; 558 min; 706 max
    // N=10M, L=1M: 3179 first; 1357 +- 77; 1227 min; 1479 max
    MARC( 2153571 ) {
      @Override
	<T extends Comparable<T>> List<T> sort(
        List<T> list, Comparator<T> comp, int limit ) {
        return Sorter.stablePartialSortMarc( list, comp, limit );
      }
    },

    // Julian's improved partial sort based on a heap.
    // N=1M, L=10: 88 first; 6.0 +- 0.0; 6 min; 7 max
    // N=10M, L=10: 71 first; 92.6 +- 0.4898979485566356; 92 min; 93 max
    // N=10M, L=10K: 158 first; 133.6 +- 0.8; 132 min; 137 max
    // N=10M, L=1M: 6896 first; 6896 +- 85; 6806 min; 7233 max
    // N=10M, L=500K: 6896 first; 6896 +- 85; 6806 min; 7233 max
    JULIAN( 1000919 ) {
      @Override
	<T extends Comparable<T>> List<T> sort(
        List<T> list, Comparator<T> comp, int limit ) {
        return Sorter.stablePartialSortJulian( list, comp, limit );
      }
    };

    /**
     * Comparison count for N=1M and L=10K. Note that N log N is 19,931,568; array sort does slightly better than that,
     * and each successive algorithm does better still.
     */
    private final int compCount;

    StableSortAlgorithm( int compCount ) {
      this.compCount = compCount;
    }

    abstract <T extends Comparable<T>> List<T> sort(
      List<T> list, Comparator<T> comp, int limit );

    void foo( int runCount, List<Integer> list, int limit ) {
      Statistician statistician = new Statistician( name() );
      Integer first = list.get( 0 );

      for ( int i = 0; i < runCount; i++ ) {
        switch ( this ) {
          case JULIAN:
            if ( limit > 10000 ) {
              continue;
            }
        }
        @SuppressWarnings( "unchecked" ) final Comparator<Integer> comp =
          Comparator.naturalOrder();
        final long start = System.currentTimeMillis();
        List<Integer> x = sort( list, comp, limit );
        statistician.record( start );
        assertEquals(first, list.get( 0 ), "non-destructive");
        if ( limit == 10 && list.size() == 1000000 ) {
          assertEquals(
            "[0, 1, 1, 2, 3, 5, 5, 5, 6, 9]", x.toString(), name());
        }
      }

      switch ( this ) {
        case JULIAN:
          if ( limit > 10000 ) {
            break;
          }
        default:
          final CountingComparator<Integer> comp =
            new CountingComparator<>();
          List<Integer> x = sort( list, comp, limit );
          if ( limit == 10 && list.size() == 1000000 && false ) {
            assertEquals(compCount, comp.count, name());
          }
      }

      statistician.printDurations();
    }
  }

  /**
   * Collects statistics for a test that is run multiple times.
   */
  static class Statistician {
    private final String desc;
    private final List<Long> durations = new ArrayList<>();

    public Statistician( String desc ) {
      super();
      this.desc = desc;
    }

    private void record( long start ) {
      durations.add(
        printDuration(
          desc + " iteration #" + ( durations.size() + 1 ),
          start ) );
    }

    private void printDurations() {
      if ( !LOGGER.isDebugEnabled() ) {
        return;
      }

      List<Long> coreDurations = durations;
      String durationsString = durations.toString(); // save before sort

      // Ignore the first 3 readings. (JIT compilation takes a while to
      // kick in.)
      if ( coreDurations.size() > 3 ) {
        coreDurations = durations.subList( 3, durations.size() );
      }
      Collections.sort( coreDurations );
      // Further ignore the max and min.
      List<Long> coreCoreDurations = coreDurations;
      if ( coreDurations.size() > 4 ) {
        coreCoreDurations =
          coreDurations.subList( 1, coreDurations.size() - 1 );
      }
      long sum = 0;
      int count = coreCoreDurations.size();
      for ( long duration : coreCoreDurations ) {
        sum += duration;
      }
      final double avg = ( (double) sum ) / count;
      double y = 0;
      for ( long duration : coreCoreDurations ) {
        double x = duration - avg;
        y += x * x;
      }
      final double stddev = Math.sqrt( y / count );
      LOGGER.debug(
        desc + ": "
          + ( durations.size() == 0
          ? "no runs"
          : durations.get( 0 ) + " first; "
          + avg + " +- "
          + stddev + "; "
          + coreDurations.get( 0 ) + " min; "
          + coreDurations.get( coreDurations.size() - 1 ) + " max; "
          + durationsString + " millis" ) );
    }
  }


  private static class CountingComparator<T extends Comparable<T>>
    implements Comparator<T> {
    int count;

    @Override
	public int compare( T e0, T e1 ) {
      ++count;
      return e0.compareTo( e1 );
    }
  }
}
