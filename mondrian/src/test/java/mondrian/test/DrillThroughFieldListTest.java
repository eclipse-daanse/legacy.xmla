/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2003-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara
// All Rights Reserved.
//
*/
package mondrian.test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencube.junit5.TestUtil.assertSqlEquals;
import static org.opencube.junit5.TestUtil.executeQuery;
import static org.opencube.junit5.TestUtil.getDialect;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.Context;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalogAsFile;

import mondrian.olap.Connection;
import mondrian.olap.OlapElement;
import mondrian.olap.Result;
import mondrian.rolap.RolapCell;

/**
 * Test drillthrought operation with specified field list.
 * If a field list was specified that means that
 * the MDX is a DRILLTHROUGH operation and
 * includes a RETURN clause.
 *
 * @author Yury_Bakhmutski
 * @since Nov 18, 2015
 */
public class DrillThroughFieldListTest {
  private PropertySaver5 propSaver;

  @BeforeEach
  public void beforeEach() {
    propSaver = new PropertySaver5();
    propSaver.set(propSaver.properties.GenerateFormattedSql, true);
  }

  @AfterEach
  public void afterEach() {
    propSaver.reset();
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
  public void testOneJoin(Context context) {
    String mdx = "SELECT\n"
        + "[Measures].[Unit Sales] ON COLUMNS,\n"
        + "[Time].[Quarter].[Q1] ON ROWS\n"
        + "FROM [Sales]";
    Result result = executeQuery(context.createConnection(), mdx);

    RolapCell rCell = (RolapCell)result.getCell(new int[] { 0, 0 });

    assertTrue(rCell.canDrillThrough());

    OlapElement returnMeasureAttribute = result.getAxes()[0]
        .getPositions().get(0).get(0);
    OlapElement returnLevelAttribute = result.getAxes()[1]
        .getPositions().get(0).get(0).getLevel();

    List<OlapElement> attributes = Arrays
        .asList(returnMeasureAttribute, returnLevelAttribute);

    String expectedSql;
    Connection connection = context.createConnection();
    switch (getDialect(connection).getDatabaseProduct()) {
    case MARIADB:
    case MYSQL:
        expectedSql =
            "select\n"
            + "    time_by_day.quarter as Quarter,\n"
            + "    sales_fact_1997.unit_sales as Unit Sales\n"
            + "from\n"
            + "    sales_fact_1997 as sales_fact_1997,\n"
            + "    time_by_day as time_by_day\n"         
            + "where\n"
            + "    sales_fact_1997.time_id = time_by_day.time_id\n"
            + "and\n"
            + "    time_by_day.the_year = 1997\n"
            + "and\n"
            + "    time_by_day.quarter = 'Q1'\n"
            + "order by\n"
                + (getDialect(connection).requiresOrderByAlias()
                ? "    Quarter ASC"
                : "    time_by_day.quarter ASC");
        break;
    case ORACLE:
        expectedSql =
            "select\n"
            + "    time_by_day.quarter as Quarter,\n"
            + "    sales_fact_1997.unit_sales as Unit Sales\n"
            + "from\n"
            + "    sales_fact_1997 sales_fact_1997,\n"
            + "    time_by_day time_by_day\n"
            + "where\n"
            + "    sales_fact_1997.time_id = time_by_day.time_id\n"
            + "and\n"
            + "    time_by_day.the_year = 1997\n"
            + "and\n"
            + "    time_by_day.quarter = 'Q1'\n"
            + "order by\n"
            + "    time_by_day.quarter ASC";
        break;
    default:
        return;
    }

    String actual = rCell.getDrillThroughSQL(attributes, true);
    int expectedRowsNumber = 21588;

    assertSqlEquals(connection, expectedSql, actual, expectedRowsNumber);
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
  public void testOneJoinTwoMeasures(Context context) {
    String mdx = "SELECT\n"
        + "{[Measures].[Unit Sales], [Measures].[Store Cost]} ON COLUMNS,\n"
        + "[Time].[Quarter].[Q1] ON ROWS\n"
        + "FROM [Sales]";
    Connection connection = context.createConnection();
    Result result = executeQuery(connection, mdx);

    RolapCell rCell = (RolapCell)result.getCell(new int[] { 0, 0 });

    assertTrue(rCell.canDrillThrough());

    OlapElement unitSalesAttribute = result.getAxes()[0]
        .getPositions().get(0).get(0);
    OlapElement storeCostAttribute = result.getAxes()[0]
        .getPositions().get(1).get(0);
    OlapElement quarterAttribute = result.getAxes()[1]
        .getPositions().get(0).get(0).getLevel();

    List<OlapElement> attributes = Arrays
        .asList(unitSalesAttribute, storeCostAttribute, quarterAttribute);

    String expectedSql;
    switch (getDialect(connection).getDatabaseProduct()) {
    case MARIADB:
    case MYSQL:
        expectedSql =
            "select\n"
            + "    time_by_day.quarter as Quarter,\n"
            + "    sales_fact_1997.unit_sales as Unit Sales,\n"
            + "    sales_fact_1997.store_cost as Store Cost\n"
            + "from\n"
            + "    sales_fact_1997 as sales_fact_1997,\n"
            + "    time_by_day as time_by_day\n"
            + "where\n"
            + "    sales_fact_1997.time_id = time_by_day.time_id\n"
            + "and\n"
            + "    time_by_day.the_year = 1997\n"
            + "and\n"
            + "    time_by_day.quarter = 'Q1'\n"
            + "order by\n"
                + (getDialect(connection).requiresOrderByAlias()
                ? "    Quarter ASC"
                : "    time_by_day.quarter ASC");
        break;
    case ORACLE:
        expectedSql =
            "select\n"
            + "    time_by_day.quarter as Quarter,\n"
            + "    sales_fact_1997.unit_sales as Unit Sales,\n"
            + "    sales_fact_1997.store_cost as Store Cost\n"
            + "from\n"
            + "    sales_fact_1997 sales_fact_1997,\n"
            + "    time_by_day time_by_day\n"
            + "where\n"
            + "    sales_fact_1997.time_id = time_by_day.time_id\n"
            + "and\n"
            + "    time_by_day.the_year = 1997\n"
            + "and\n"
            + "    time_by_day.quarter = 'Q1'\n"
            + "order by\n"
            + "    time_by_day.quarter ASC";
        break;
    default:
        return;
    }

    String actual = rCell.getDrillThroughSQL(attributes, true);
    int expectedRowsNumber = 21588;

    assertSqlEquals(connection, expectedSql, actual, expectedRowsNumber);
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
  public void testTwoJoins(Context context) {
    String mdx = "SELECT\n"
        + "{[Measures].[Unit Sales], [Measures].[Store Cost]} ON COLUMNS,\n"
        + "NONEMPTYCROSSJOIN({[Time].[Quarter].[Q1]},"
        + " {[Product].[Product Name].[Good Imported Beer]}) ON ROWS\n"
        + "FROM [Sales]";
    Connection connection = context.createConnection();
    Result result = executeQuery(connection, mdx);

    RolapCell rCell = (RolapCell)result.getCell(new int[] { 0, 0 });

    assertTrue(rCell.canDrillThrough());

    OlapElement unitSalesAttribute = result.getAxes()[0]
        .getPositions().get(0).get(0);
    OlapElement storeCostAttribute = result.getAxes()[0]
        .getPositions().get(1).get(0);
    OlapElement quarterAttribute = result.getAxes()[1]
        .getPositions().get(0).get(0).getLevel();

    List<OlapElement> attributes = Arrays
        .asList(unitSalesAttribute, storeCostAttribute, quarterAttribute);

    String expectedSql;
    switch (getDialect(connection).getDatabaseProduct()) {
    case MARIADB:
    case MYSQL:
        expectedSql = "select\n"
            + "    time_by_day.quarter as Quarter,\n"
            + "    sales_fact_1997.unit_sales as Unit Sales,\n"
            + "    sales_fact_1997.store_cost as Store Cost\n"
            + "from\n"
            + "    sales_fact_1997 as sales_fact_1997,\n"
            + "    time_by_day as time_by_day,\n"
            + "    product as product\n"
            + "where\n"
            + "    sales_fact_1997.time_id = time_by_day.time_id\n"
            + "and\n"
            + "    time_by_day.the_year = 1997\n"
            + "and\n"
            + "    time_by_day.quarter = 'Q1'\n"
            + "and\n"
            + "    sales_fact_1997.product_id = product.product_id\n"
            + "and\n"
            + "    product.product_name = 'Good Imported Beer'\n"
            + "order by\n"
            + (getDialect(connection).requiresOrderByAlias()
                ? "    Quarter ASC"
                : "    time_by_day.quarter ASC");
        break;
    case ORACLE:
        expectedSql = "select\n"
            + "    time_by_day.quarter as Quarter,\n"
            + "    sales_fact_1997.unit_sales as Unit Sales,\n"
            + "    sales_fact_1997.store_cost as Store Cost\n"
            + "from\n"
            + "    sales_fact_1997 sales_fact_1997,\n"
            + "    time_by_day time_by_day,\n"
            + "    product product\n"
            + "where\n"
            + "    sales_fact_1997.time_id = time_by_day.time_id\n"
            + "and\n"
            + "    time_by_day.the_year = 1997\n"
            + "and\n"
            + "    time_by_day.quarter = 'Q1'\n"
            + "and\n"
            + "    sales_fact_1997.product_id = product.product_id\n"
            + "and\n"
            + "    product.product_name = 'Good Imported Beer'\n"
            + "order by\n"
            + "    time_by_day.quarter ASC";
        break;
    default:
        return;
    }

    String actual = rCell.getDrillThroughSQL(attributes, true);
    int expectedRowsNumber = 7;

    assertSqlEquals(connection, expectedSql, actual, expectedRowsNumber);
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
  public void testNoJoin(Context context) {
    String mdx = "SELECT\n"
        + "Measures.[Store Sqft] on COLUMNS\n"
        + "FROM [Store]";
    Connection connection = context.createConnection();
    Result result = executeQuery(connection, mdx);

    RolapCell rCell = (RolapCell)result.getCell(new int[] { 0 });

    assertTrue(rCell.canDrillThrough());

    OlapElement StoreSqftAttribute = result.getAxes()[0]
        .getPositions().get(0).get(0);

    List<OlapElement> attributes = Arrays
        .asList(StoreSqftAttribute);

    String expectedSql;
    switch (getDialect(connection).getDatabaseProduct()) {
    case MARIADB:
    case MYSQL:
        expectedSql =
            "select\n"
            + "    store.store_sqft as Store Sqft\n"
            + "from\n"
            + "    store as store";
        break;
    case ORACLE:
        expectedSql =
            "select\n"
            + "    store.store_sqft as Store Sqft\n"
            + "from\n"
            + "    store store";
        break;
    default:
        return;
    }

    String actual = rCell.getDrillThroughSQL(attributes, true);
    int expectedRowsNumber = 25;

    assertSqlEquals(connection, expectedSql, actual, expectedRowsNumber);
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
  public void testVirtualCube(Context context) {
    String mdx = " SELECT\n"
        + " [Measures].[Unit Sales] ON COLUMNS\n"
        + " FROM [Warehouse and Sales]\n"
        + " WHERE [Gender].[F]";
    Connection connection = context.createConnection();
    Result result = executeQuery(connection, mdx);

    RolapCell rCell = (RolapCell)result.getCell(new int[] { 0 });

    assertTrue(rCell.canDrillThrough());

    OlapElement StoreSqftAttribute = result.getAxes()[0]
        .getPositions().get(0).get(0);

    List<OlapElement> attributes = Arrays
        .asList(StoreSqftAttribute);

    String expectedSql;
    switch (getDialect(connection).getDatabaseProduct()) {
    case MARIADB:
    case MYSQL:
        expectedSql = "select\n"
            + "    sales_fact_1997.unit_sales as Unit Sales\n"
            + "from\n"
            + "    sales_fact_1997 as sales_fact_1997,\n"
            + "    time_by_day as time_by_day,\n"
            + "    customer as customer\n"
            + "where\n"
            + "    sales_fact_1997.time_id = time_by_day.time_id\n"
            + "and\n"
            + "    time_by_day.the_year = 1997\n"
            + "and\n"
            + "    sales_fact_1997.customer_id = customer.customer_id\n"
            + "and\n"
            + "    customer.gender = 'F'";
        break;
    case ORACLE:
        expectedSql = "select\n"
             + "    sales_fact_1997.unit_sales as Unit Sales\n"
             + "from\n"
             + "    sales_fact_1997 sales_fact_1997,\n"
             + "    time_by_day time_by_day,\n"
             + "    customer customer\n"
             + "where\n"
             + "    sales_fact_1997.time_id = time_by_day.time_id\n"
             + "and\n"
             + "    time_by_day.the_year = 1997\n"
             + "and\n"
             + "    sales_fact_1997.customer_id = customer.customer_id\n"
             + "and\n"
             + "    customer.gender = 'F'";
        break;
    default:
        return;
    }

    String actual = rCell.getDrillThroughSQL(attributes, true);
    int expectedRowsNumber = 42831;

    assertSqlEquals(connection, expectedSql, actual, expectedRowsNumber);
  }
}

// End DrillThroughFieldListTest.java
