/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2003-2005 Julian Hyde
 * Copyright (C) 2005-2021 Hitachi Vantara
 * All Rights Reserved.
 *
 * jhyde, Feb 14, 2003
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */
package mondrian.test;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.eclipse.daanse.rolap.common.util.ExpressionUtil.getExpression;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.assertSqlEquals;
import static org.opencube.junit5.TestUtil.checkThrowable;
import static org.opencube.junit5.TestUtil.executeQuery;
import static org.opencube.junit5.TestUtil.executeStatement;
import static org.opencube.junit5.TestUtil.getDialect;
import static org.opencube.junit5.TestUtil.withSchema;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Quoting;
import org.eclipse.daanse.olap.api.SqlExpression;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.olap.query.component.IdImpl;
import org.eclipse.daanse.rolap.element.RolapCube;
import org.eclipse.daanse.rolap.element.RolapLevel;
import org.eclipse.daanse.rolap.common.RolapStar;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.enums.DatabaseProduct;
import mondrian.rolap.SchemaModifiers;

/**
 * Test generation of SQL to access the fact table data underlying an MDX
 * result set.
 *
 * @author jhyde
 * @since May 10, 2006
 */
class DrillThroughTest {
    // DRILLTHROUGH MDX queries
    private static final String CURRENT_MEMBER_CUSTOMER_FULL_NAME =
        "Jeanne Derry";
    private static final String CURRENT_MEMBER_CUSTOMER_ID = "3";
    private static final String DRILLTHROUGH_QUERY_TEMPLATE = "DRILLTHROUGH \n"
        + "// Request ID: 9520e524-4d2c-11e7-a0e2-a0d3c11aa164 - RUN_REPORT\n"
        + "WITH\n"
        + "SET [*NATIVE_CJ_SET] AS ''FILTER([Customers Dimension].[Customer Level Name].MEMBERS, NOT ISEMPTY ([Measures].[Store Sales]))''\n"
        + "SET [*SORTED_ROW_AXIS] AS ''ORDER([*CJ_ROW_AXIS],[Customers Dimension].CURRENTMEMBER.ORDERKEY,BASC)''\n"
        + "SET [*BASE_MEMBERS__Customers Dimension_] AS ''[Customers Dimension].[Customer Level Name].MEMBERS''\n"
        + "SET [*BASE_MEMBERS__Measures_] AS '''{[Measures].[*FORMATTED_MEASURE_0]}'''\n"
        + "SET [*CJ_ROW_AXIS] AS ''GENERATE([*NATIVE_CJ_SET], '{([Customers Dimension].CURRENTMEMBER)}')''\n"
        + "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS ''[Measures].[Store Sales]'', FORMAT_STRING = ''#,###.00'', SOLVE_ORDER=500\n"
        + "SELECT\n"
        + "FILTER([*BASE_MEMBERS__Measures_],([Measures].CurrentMember Is [Measures].[*FORMATTED_MEASURE_0])) ON COLUMNS\n"
        + ",FILTER( [*SORTED_ROW_AXIS],([Customers Dimension].CurrentMember Is [Customers Dimension].[{0}])) ON ROWS\n"
        + "FROM [SalesShort] RETURN [Customers Dimension].[Customer Level Name], [Product Dimension].[Product Level Name], [Measures].[Store Sales]";

    private static final String DRILLTHROUGH_QUERY_WITH_CUSTOMER_FULL_NAME =
        MessageFormat.format(
            DRILLTHROUGH_QUERY_TEMPLATE, CURRENT_MEMBER_CUSTOMER_FULL_NAME);
    private static final String DRILLTHROUGH_QUERY_WITH_CUSTOMER_ID =
        MessageFormat.format(
            DRILLTHROUGH_QUERY_TEMPLATE, CURRENT_MEMBER_CUSTOMER_ID);

    // Schemas contained Levels with and without nameColumn attribute.
    private static final String NAME_COLUMN_FULL_NAME =
        " nameColumn=\"fullname\" ";
    private static final String NAME_COLUMN_PRODUCT_NAME =
        " nameColumn=\"product_name\" ";

    private static final String SALES_ONLY_TEMPLATE =
        "<Schema name=\"FoodMartSalesOnly\">\n"
        + " <Cube name=\"SalesShort\">\n"
        + "   <Table name=\"sales_fact_1997\"/>\n"
        + "   <Dimension name=\"Customers Dimension\" foreignKey=\"customer_id\">\n"
        + "     <Hierarchy hasAll=\"true\" allMemberName=\"All Customers hierarchy name\" primaryKey=\"customer_id\">\n"
        + "       <Table name=\"customer\"/>\n"
        + "       <Level name=\"Customer Level Name\" caption=\"Customer Level Caption\" description=\"Customer Level Description\" column=\"customer_id\"{0}type=\"String\" uniqueMembers=\"true\" />\n"
        + "     </Hierarchy>\n"
        + "   </Dimension>\n"
        + "   <Dimension name=\"Product Dimension\" foreignKey=\"product_id\">\n"
        + "     <Hierarchy hasAll=\"true\" allMemberName=\"All products hierarchy name\" primaryKey=\"product_id\">\n"
        + "       <Table name=\"product\"/>\n"
        + "       <Level name=\"Product Level Name\" caption=\"Product Level Caption\" description=\"Product Level Description\" column=\"product_id\"{1}type=\"String\" uniqueMembers=\"true\" />\n"
        + "     </Hierarchy>\n"
        + "   </Dimension>\n"
        + "   <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\" formatString=\"#,###.00\"/>\n"
        + " </Cube>\n"
        + "</Schema>\n";

    private static final String SALES_ONLY_WITHOUT_NAME_COLUMN =
        MessageFormat.format(SALES_ONLY_TEMPLATE, " ", " ");
    private static final String SALES_ONLY_WITH_NAME_COLUMN =
        MessageFormat.format(
            SALES_ONLY_TEMPLATE,
            NAME_COLUMN_FULL_NAME,
            NAME_COLUMN_PRODUCT_NAME);


    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }
    // ~ Tests ================================================================

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testTrivialCalcMemberDrillThrough(Context<?> context) {
    	context.getCatalogCache().clear();
        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Measures].[Formatted Unit Sales]"
            + " AS '[Measures].[Unit Sales]', FORMAT_STRING='$#,###.000'\n"
            + "MEMBER [Measures].[Twice Unit Sales]"
            + " AS '[Measures].[Unit Sales] * 2'\n"
            + "MEMBER [Measures].[Twice Unit Sales Plus Store Sales] "
            + " AS '[Measures].[Twice Unit Sales] + [Measures].[Store Sales]',"
            + "  FOMRAT_STRING='#'\n"
            + "MEMBER [Measures].[Foo] "
            + " AS '[Measures].[Unit Sales] + ([Measures].[Unit Sales], [Time].[Time].PrevMember)'\n"
            + "MEMBER [Measures].[Unit Sales Percentage] "
            + " AS '[Measures].[Unit Sales] / [Measures].[Twice Unit Sales]'\n"
            + "SELECT {[Measures].[Unit Sales],\n"
            + "  [Measures].[Formatted Unit Sales],\n"
            + "  [Measures].[Twice Unit Sales],\n"
            + "  [Measures].[Twice Unit Sales Plus Store Sales],\n"
            + "  [Measures].[Foo],\n"
            + "  [Measures].[Unit Sales Percentage]} on columns,\n"
            + " {[Product].Children} on rows\n"
            + "from Sales");

        // can drill through [Formatted Unit Sales]
        final Cell cell = result.getCell(new int[]{0, 0});
        assertTrue(cell.canDrillThrough());
        // can drill through [Unit Sales]
        assertTrue(result.getCell(new int[]{1, 0}).canDrillThrough());
        // can drill through [Twice Unit Sales]
        assertTrue(result.getCell(new int[]{2, 0}).canDrillThrough());
        // can drill through [Twice Unit Sales Plus Store Sales]
        assertTrue(result.getCell(new int[]{3, 0}).canDrillThrough());
        // can not drill through [Foo]
        assertFalse(result.getCell(new int[]{4, 0}).canDrillThrough());
        // can drill through [Unit Sales Percentage]
        assertTrue(result.getCell(new int[]{5, 0}).canDrillThrough());
        assertNotNull(
            result.getCell(
                new int[]{
                    5, 0
                }).getDrillThroughSQL(false));

        String sql = cell.getDrillThroughSQL(false);
        String expectedSql =
            "select `time_by_day`.`the_year` as `Year`,"
            + " `product_class`.`product_family` as `Product Family`,"
            + " `sales_fact_1997`.`unit_sales` as `Unit Sales` "
            + "from `sales_fact_1997` =as= `sales_fact_1997`,"
            + " `time_by_day` =as= `time_by_day`,"
            + " `product_class` =as= `product_class`,"
            + " `product` =as= `product` "
            + "where `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`"
            + " and `time_by_day`.`the_year` = 1997"
            + " and `sales_fact_1997`.`product_id` = `product`.`product_id`"
            + " and `product`.`product_class_id` = `product_class`.`product_class_id`"
            + " and `product_class`.`product_family` = 'Drink' "
            + "order by "
            + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                ? "`Year` ASC,"
                + " `Product Family` ASC"
                : "`time_by_day`.`the_year` ASC,"
                + " `product_class`.`product_family` ASC");

        assertSqlEquals(context.getConnectionWithDefaultRole(), expectedSql, sql, 7978);

        // Can drill through a trivial calc member.
        final Cell calcCell = result.getCell(new int[]{1, 0});
        assertTrue(calcCell.canDrillThrough());
        sql = calcCell.getDrillThroughSQL(false);
        assertNotNull(sql);
        expectedSql =
            "select `time_by_day`.`the_year` as `Year`,"
            + " `product_class`.`product_family` as `Product Family`,"
            + " `sales_fact_1997`.`unit_sales` as `Unit Sales` "
            + "from `sales_fact_1997` =as= `sales_fact_1997`,"
            + " `time_by_day` =as= `time_by_day`,"
            + " `product_class` =as= `product_class`,"
            + " `product` =as= `product` "
            + "where `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`"
            + " and `time_by_day`.`the_year` = 1997"
            + " and `sales_fact_1997`.`product_id` = `product`.`product_id`"
            + " and `product`.`product_class_id` = `product_class`.`product_class_id`"
            + " and `product_class`.`product_family` = 'Drink' "
            + "order by "
            + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                ? "`Year` ASC,"
                + " `Product Family` ASC"
                : "`time_by_day`.`the_year` ASC,"
                + " `product_class`.`product_family` ASC");

        assertSqlEquals(context.getConnectionWithDefaultRole(), expectedSql, sql, 7978);

        assertEquals(7978, calcCell.getDrillThroughCount() );
    }
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testTrivialCalcMemberNotMeasure(Context<?> context) {
        // [Product].[My Food] is trivial because it maps to a single member.
        // First, on ROWS axis.
        Connection connection = context.getConnectionWithDefaultRole();
        Result result = executeQuery(connection,
            "with member [Product].[My Food]\n"
            + " AS [Product].[Food], FORMAT_STRING = '#,###'\n"
            + "SELECT [Measures].[Unit Sales] * [Gender].[M] on 0,\n"
            + " [Marital Status].[S] * [Product].[My Food] on 1\n"
            + "from [Sales]");
        Cell cell = result.getCell(new int[] {0, 0});
        assertTrue(cell.canDrillThrough());
        assertEquals(16129, cell.getDrillThroughCount());

        // Next, on filter axis.
        result = executeQuery(connection,
            "with member [Product].[My Food]\n"
            + " AS [Product].[Food], FORMAT_STRING = '#,###'\n"
            + "SELECT [Measures].[Unit Sales] * [Gender].[M] on 0,\n"
            + " [Marital Status].[S] on 1\n"
            + "from [Sales]\n"
            + "where [Product].[My Food]");
        cell = result.getCell(new int[] {0, 0});
        assertTrue(cell.canDrillThrough());
        assertEquals(16129, cell.getDrillThroughCount());

        // Trivial member with Aggregate.
        result = executeQuery(connection,
            "with member [Product].[My Food]\n"
            + " AS Aggregate({[Product].[Food]}), FORMAT_STRING = '#,###'\n"
            + "SELECT [Measures].[Unit Sales] * [Gender].[M] on 0,\n"
            + " [Marital Status].[S] * [Product].[My Food] on 1\n"
            + "from [Sales]");
        cell = result.getCell(new int[] {0, 0});
        assertTrue(cell.canDrillThrough());
        assertEquals(16129, cell.getDrillThroughCount());

        // Non-trivial member on rows.
        result = executeQuery(connection,
            "with member [Product].[My Food Drink]\n"
            + " AS Aggregate({[Product].[Food], [Product].[Drink]}),\n"
            + "       FORMAT_STRING = '#,###'\n"
            + "SELECT [Measures].[Unit Sales] * [Gender].[M] on 0,\n"
            + " [Marital Status].[S] * [Product].[My Food Drink] on 1\n"
            + "from [Sales]");
        cell = result.getCell(new int[] {0, 0});
        assertFalse(cell.canDrillThrough());

        // drop the constraint when we drill through
        assertEquals(22479, cell.getDrillThroughCount());

        // Non-trivial member on filter axis.
        result = executeQuery(connection,
            "with member [Product].[My Food Drink]\n"
            + " AS Aggregate({[Product].[Food], [Product].[Drink]}),\n"
            + "       FORMAT_STRING = '#,###'\n"
            + "SELECT [Measures].[Unit Sales] * [Gender].[M] on 0,\n"
            + " [Marital Status].[S] on 1\n"
            + "from [Sales]\n"
            + "where [Product].[My Food Drink]");
        cell = result.getCell(new int[] {0, 0});
        assertFalse(cell.canDrillThrough());
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testDrillthroughCompoundSlicer(Context<?> context) {
        // Tests a case associated with
        // http://jira.pentaho.com/browse/MONDRIAN-1587
        // hsqldb was failing with SQL that included redundant parentheses
        // around IN list items.
    	context.getCatalogCache().clear();
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "select from sales where "
            + "{[Promotion Media].[Bulk Mail],[Promotion Media].[Cash Register Handout]}");
        final Cell cell = result.getCell(new int[]{});
        assertTrue(cell.canDrillThrough());
        assertEquals(3584, cell.getDrillThroughCount());
        assertSqlEquals(context.getConnectionWithDefaultRole(),
            "select\n"
            + "    time_by_day.the_year as Year,\n"
            + "    promotion.media_type as Media Type,\n"
            + "    sales_fact_1997.unit_sales as Unit Sales\n"
            + "from\n"
            + "    sales_fact_1997 =as= sales_fact_1997,\n"
            + "    time_by_day =as= time_by_day,\n"
            + "    promotion =as= promotion\n"
            + "where\n"
            + "    sales_fact_1997.time_id = time_by_day.time_id\n"
            + "and\n"
            + "    time_by_day.the_year = 1997\n"
            + "and\n"
            + "    sales_fact_1997.promotion_id = promotion.promotion_id\n"
            + "and\n"
            + "    ((promotion.media_type in "
            + "('Bulk Mail', 'Cash Register Handout')))\n"
            + "order by\n"
            + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                ? "    Year ASC"
                : "    time_by_day.the_year ASC"),
            cell.getDrillThroughSQL(false), 3584);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testDrillThrough(Context<?> context) {
        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Measures].[Price] AS '[Measures].[Store Sales] / ([Measures].[Store Sales], [Time].[Time].PrevMember)'\n"
            + "SELECT {[Measures].[Unit Sales], [Measures].[Price]} on columns,\n"
            + " {[Product].Children} on rows\n"
            + "from Sales");
        final Cell cell = result.getCell(new int[]{0, 0});
        assertTrue(cell.canDrillThrough());
        String sql = cell.getDrillThroughSQL(false);

        String expectedSql =
            "select `time_by_day`.`the_year` as `Year`,"
            + " `product_class`.`product_family` as `Product Family`,"
            + " `sales_fact_1997`.`unit_sales` as `Unit Sales` "
            + "from `sales_fact_1997` =as= `sales_fact_1997`,"
            + " `time_by_day` =as= `time_by_day`,"
            + " `product_class` =as= `product_class`,"
            + " `product` =as= `product` "
            + "where `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`"
            + " and `time_by_day`.`the_year` = 1997"
            + " and `sales_fact_1997`.`product_id` = `product`.`product_id`"
            + " and `product`.`product_class_id` = `product_class`.`product_class_id`"
            + " and `product_class`.`product_family` = 'Drink' "
            + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                ? "order by `Year` ASC,"
                + " `Product Family` ASC"
                : "order by `time_by_day`.`the_year` ASC,"
                + " `product_class`.`product_family` ASC");

        assertSqlEquals(context.getConnectionWithDefaultRole(), expectedSql, sql, 7978);

        // Cannot drill through a calc member.
        final Cell calcCell = result.getCell(new int[]{1, 1});
        assertFalse(calcCell.canDrillThrough());
        sql = calcCell.getDrillThroughSQL(false);
        assertNull(sql);
    }

    private String getNameExp(
        Result result,
        String hierName,
        String levelName)
    {
        final Cube cube = result.getQuery().getCube();
        RolapStar star = ((RolapCube) cube).getStar();

        Hierarchy h =
            cube.lookupHierarchy(
                new IdImpl.NameSegmentImpl(hierName, Quoting.UNQUOTED), false);
        if (h == null) {
            return null;
        }
        for (Level l : h.getLevels()) {
            if (l.getName().equals(levelName)) {
            	SqlExpression exp = ((RolapLevel) l).getNameExp();
                String nameExpStr = getExpression(exp, star.getSqlQuery());
                nameExpStr = nameExpStr.replace('"', '`') ;
                return nameExpStr;
            }
        }
        return null;
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testDrillThrough2(Context<?> context) {
        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Measures].[Price] AS '[Measures].[Store Sales] / ([Measures].[Unit Sales], [Time].[Time].PrevMember)'\n"
            + "SELECT {[Measures].[Unit Sales], [Measures].[Price]} on columns,\n"
            + " {[Product].Children} on rows\n"
            + "from Sales");
        String sql = result.getCell(new int[]{0, 0}).getDrillThroughSQL(true);

        String nameExpStr = getNameExp(result, "Customers", "Name");
        String expectedSql =
            "select `store`.`store_country` as `Store Country`,"
            + " `store`.`store_state` as `Store State`,"
            + " `store`.`store_city` as `Store City`,"
            + " `store`.`store_name` as `Store Name`,"
            + " `store`.`store_type` as `Store Type`,"
            + " `store`.`store_sqft` as `Store Sqft`,"
            + " `time_by_day`.`the_year` as `Year`,"
            + " `time_by_day`.`quarter` as `Quarter`,"
            + " `time_by_day`.`month_of_year` as `Month`,"
            + " `time_by_day`.`week_of_year` as `Week`,"
            + " `time_by_day`.`day_of_month` as `Day`,"
            + " `product_class`.`product_family` as `Product Family`,"
            + " `product_class`.`product_department` as `Product Department`,"
            + " `product_class`.`product_category` as `Product Category`,"
            + " `product_class`.`product_subcategory` as `Product Subcategory`,"
            + " `product`.`brand_name` as `Brand Name`,"
            + " `product`.`product_name` as `Product Name`,"
            + " `promotion`.`media_type` as `Media Type`,"
            + " `promotion`.`promotion_name` as `Promotion Name`,"
            + " `customer`.`country` as `Country`,"
            + " `customer`.`state_province` as `State Province`,"
            + " `customer`.`city` as `City`, "
            + nameExpStr
            + " as `Name`,"
            + " `customer`.`customer_id` as `Name (Key)`,"
            + " `customer`.`gender` as `Gender`,"
            + " `customer`.`marital_status` as `Marital Status`,"
            + " `customer`.`education` as `Education`,"
            + " `customer`.`yearly_income` as `Yearly Income`,"
            + " `sales_fact_1997`.`unit_sales` as `Unit Sales` "
            + "from `sales_fact_1997` =as= `sales_fact_1997`,"
            + " `store` =as= `store`,"
            + " `time_by_day` =as= `time_by_day`,"
            + " `product_class` =as= `product_class`,"
            + " `product` =as= `product`,"
            + " `promotion` =as= `promotion`,"
            + " `customer` =as= `customer` "
            + "where `sales_fact_1997`.`store_id` = `store`.`store_id`"
            + " and `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`"
            + " and `time_by_day`.`the_year` = 1997"
            + " and `sales_fact_1997`.`product_id` = `product`.`product_id`"
            + " and `product`.`product_class_id` = `product_class`.`product_class_id`"
            + " and `product_class`.`product_family` = 'Drink'"
            + " and `sales_fact_1997`.`promotion_id` = `promotion`.`promotion_id`"
            + " and `sales_fact_1997`.`customer_id` = `customer`.`customer_id` "
            + "order by"
            + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                ? " `Store Country` ASC,"
                + " `Store State` ASC,"
                + " `Store City` ASC,"
                + " `Store Name` ASC,"
                + " `Store Type` ASC,"
                + " `Store Sqft` ASC,"
                + " `Year` ASC,"
                + " `Quarter` ASC,"
                + " `Month` ASC,"
                + " `Week` ASC,"
                + " `Day` ASC,"
                + " `Product Family` ASC,"
                + " `Product Department` ASC,"
                + " `Product Category` ASC,"
                + " `Product Subcategory` ASC,"
                + " `Brand Name` ASC,"
                + " `Product Name` ASC,"
                + " `Media Type` ASC,"
                + " `Promotion Name` ASC,"
                + " `Country` ASC,"
                + " `State Province` ASC,"
                + " `City` ASC,"
                + " `Name` ASC,"
                + " `Name (Key)` ASC,"
                + " `Gender` ASC,"
                + " `Marital Status` ASC,"
                + " `Education` ASC,"
                + " `Yearly Income` ASC"
                : " `store`.`store_country` ASC,"
                + " `store`.`store_state` ASC,"
                + " `store`.`store_city` ASC,"
                + " `store`.`store_name` ASC,"
                + " `store`.`store_type` ASC,"
                + " `store`.`store_sqft` ASC,"
                + " `time_by_day`.`the_year` ASC,"
                + " `time_by_day`.`quarter` ASC,"
                + " `time_by_day`.`month_of_year` ASC,"
                + " `time_by_day`.`week_of_year` ASC,"
                + " `time_by_day`.`day_of_month` ASC,"
                + " `product_class`.`product_family` ASC,"
                + " `product_class`.`product_department` ASC,"
                + " `product_class`.`product_category` ASC,"
                + " `product_class`.`product_subcategory` ASC,"
                + " `product`.`brand_name` ASC,"
                + " `product`.`product_name` ASC,"
                + " `promotion`.`media_type` ASC,"
                + " `promotion`.`promotion_name` ASC,"
                + " `customer`.`country` ASC,"
                + " `customer`.`state_province` ASC,"
                + " `customer`.`city` ASC, "
                + nameExpStr
                + " ASC,"
                + " `customer`.`customer_id` ASC,"
                + " `customer`.`education` ASC,"
                + " `customer`.`gender` ASC,"
                + " `customer`.`marital_status` ASC,"
                + " `customer`.`yearly_income` ASC");

        assertSqlEquals(context.getConnectionWithDefaultRole(), expectedSql, sql, 7978);

        // Drillthrough SQL is null for cell based on calc member
        sql = result.getCell(new int[]{1, 1}).getDrillThroughSQL(true);
        assertNull(sql);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testDrillThrough3(Context<?> context) {
        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "select {[Measures].[Unit Sales], [Measures].[Store Cost], [Measures].[Store Sales]} ON COLUMNS, \n"
            + "Hierarchize(Union(Union(Crossjoin({[Promotion Media].[All Media]}, {[Product].[All Products]}), \n"
            + "Crossjoin({[Promotion Media].[All Media]}, [Product].[All Products].Children)), Crossjoin({[Promotion Media].[All Media]}, [Product].[All Products].[Drink].Children))) ON ROWS \n"
            + "from [Sales] where [Time].[1997].[Q4].[12]");

        // [Promotion Media].[All Media], [Product].[All
        // Products].[Drink].[Dairy], [Measures].[Store Cost]
        Cell cell = result.getCell(new int[]{0, 4});

        String sql = cell.getDrillThroughSQL(true);

        String nameExpStr = getNameExp(result, "Customers", "Name");

        String expectedSql =
            "select"
            + " `store`.`store_country` as `Store Country`,"
            + " `store`.`store_state` as `Store State`,"
            + " `store`.`store_city` as `Store City`,"
            + " `store`.`store_name` as `Store Name`,"
            + " `store`.`store_type` as `Store Type`,"
            + " `store`.`store_sqft` as `Store Sqft`,"
            + " `time_by_day`.`the_year` as `Year`,"
            + " `time_by_day`.`quarter` as `Quarter`,"
            + " `time_by_day`.`month_of_year` as `Month`,"
            + " `time_by_day`.`week_of_year` as `Week`,"
            + " `time_by_day`.`day_of_month` as `Day`,"
            + " `product_class`.`product_family` as `Product Family`,"
            + " `product_class`.`product_department` as `Product Department`,"
            + " `product_class`.`product_category` as `Product Category`,"
            + " `product_class`.`product_subcategory` as `Product Subcategory`,"
            + " `product`.`brand_name` as `Brand Name`,"
            + " `product`.`product_name` as `Product Name`,"
            + " `promotion`.`media_type` as `Media Type`,"
            + " `promotion`.`promotion_name` as `Promotion Name`,"
            + " `customer`.`country` as `Country`,"
            + " `customer`.`state_province` as `State Province`,"
            + " `customer`.`city` as `City`,"
            + " " + nameExpStr + " as `Name`,"
            + " `customer`.`customer_id` as `Name (Key)`,"
            + " `customer`.`gender` as `Gender`,"
            + " `customer`.`marital_status` as `Marital Status`,"
            + " `customer`.`education` as `Education`,"
            + " `customer`.`yearly_income` as `Yearly Income`,"
            + " `sales_fact_1997`.`unit_sales` as `Unit Sales` "
            + "from `sales_fact_1997` =as= `sales_fact_1997`, "
            + "`store =as= `store`, "
            + "`time_by_day` =as= `time_by_day`, "
            + "`product_class` =as= `product_class`, "
            + "`product` =as= `product`, "
            + "`promotion` =as= `promotion`, "
            + "`customer` =as= `customer` "
            + "where `sales_fact_1997`.`store_id` = `store`.`store_id` and "
            + "`sales_fact_1997`.`time_id` = `time_by_day`.`time_id` and "
            + "`time_by_day`.`the_year` = 1997 and "
            + "`time_by_day`.`quarter` = 'Q4' and "
            + "`time_by_day`.`month_of_year` = 12 and "
            + "`sales_fact_1997`.`product_id` = `product`.`product_id` and "
            + "`product`.`product_class_id` = `product_class`.`product_class_id` and "
            + "`product_class`.`product_family` = 'Drink' and "
            + "`product_class`.`product_department` = 'Dairy' and "
            + "`sales_fact_1997`.`promotion_id` = `promotion`.`promotion_id` and "
            + "`sales_fact_1997`.`customer_id` = `customer`.`customer_id` "
            + "order by"
            + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                ? " `Store Country` ASC,"
                + " `Store State` ASC,"
                + " `Store City` ASC,"
                + " `Store Name` ASC,"
                + " `Store Type` ASC,"
                + " `Store Sqft` ASC,"
                + " `Year` ASC,"
                + " `Quarter` ASC,"
                + " `Month` ASC,"
                + " `Week` ASC,"
                + " `Day` ASC,"
                + " `Product Family` ASC,"
                + " `Product Department` ASC,"
                + " `Product Category` ASC,"
                + " `Product Subcategory` ASC,"
                + " `Brand Name` ASC,"
                + " `Product Name` ASC,"
                + " `Media Type` ASC,"
                + " `Promotion Name` ASC,"
                + " `Country` ASC,"
                + " `State Province` ASC,"
                + " `City` ASC,"
                + " `Name` ASC,"
                + " `Name (Key)` ASC,"
                + " `Gender` ASC,"
                + " `Marital Status` ASC,"
                + " `Education` ASC,"
                + " `Yearly Income` ASC"
                : " `store`.`store_country` ASC,"
                + " `store`.`store_state` ASC,"
                + " `store`.`store_city` ASC,"
                + " `store`.`store_name` ASC,"
                + " `store`.`store_sqft` ASC,"
                + " `store`.`store_type` ASC,"
                + " `time_by_day`.`the_year` ASC,"
                + " `time_by_day`.`quarter` ASC,"
                + " `time_by_day`.`month_of_year` ASC,"
                + " `time_by_day`.`week_of_year` ASC,"
                + " `time_by_day`.`day_of_month` ASC,"
                + " `product_class`.`product_family` ASC,"
                + " `product_class`.`product_department` ASC,"
                + " `product_class`.`product_category` ASC,"
                + " `product_class`.`product_subcategory` ASC,"
                + " `product`.`brand_name` ASC,"
                + " `product`.`product_name` ASC,"
                + " `promotion.media_type` ASC,"
                + " `promotion`.`promotion_name` ASC,"
                + " `customer`.`country` ASC,"
                + " `customer`.`state_province` ASC,"
                + " `customer`.`city` ASC,"
                + " " + nameExpStr + " ASC,"
                + " `customer`.`customer_id` ASC,"
                + " `customer`.gender` ASC,"
                + " `customer`.`marital_status` ASC,"
                + " `customer`.`education` ASC,"
                + " `customer`.`yearly_income` ASC");

        assertSqlEquals(context.getConnectionWithDefaultRole(), expectedSql, sql, 141);
    }

    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-180">
     * MONDRIAN-180, "Drillthrough fails, if Aggregate in
     * MDX-query"</a>. The problem actually occurs with any calculated member,
     * not just Aggregate. The bug was causing a syntactically invalid
     * constraint to be added to the WHERE clause; after the fix, we do
     * not constrain on the member at all.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testDrillThroughBugMondrian180(Context<?> context) {
        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "with set [Date Range] as '{[Time].[1997].[Q1], [Time].[1997].[Q2]}'\n"
            + "member [Time].[Time].[Date Range] as 'Aggregate([Date Range])'\n"
            + "select {[Measures].[Unit Sales]} ON COLUMNS,\n"
            + "Hierarchize(Union(Union(Union({[Store].[All Stores]}, [Store].[All Stores].Children), [Store].[All Stores].[USA].Children), [Store].[All Stores].[USA].[CA].Children)) ON ROWS\n"
            + "from [Sales]\n"
            + "where [Time].[Date Range]");

        final Cell cell = result.getCell(new int[]{0, 6});

        // It is not valid to drill through this cell, because it contains a
        // non-trivial calculated member.
        assertFalse(cell.canDrillThrough());

        // For backwards compatibility, generate drill-through SQL (ignoring
        // calculated members) even though we said we could not drill through.
        String sql = cell.getDrillThroughSQL(true);

        String nameExpStr = getNameExp(result, "Customers", "Name");

        final String expectedSql =
            "select"
            + " `store`.`store_state` as `Store State`,"
            + " `store`.`store_city` as `Store City`,"
            + " `store`.`store_name` as `Store Name`,"
            + " `store`.`store_type` as `Store Type`,"
            + " `store`.`store_sqft` as `Store Sqft`,"
            + " `time_by_day`.`the_year` as `Year`,"
            + " `time_by_day`.`quarter` as `Quarter`,"
            + " `time_by_day`.`month_of_year` as `Month`,"
            + " `time_by_day`.`week_of_year` as `Week`,"
            + " `time_by_day`.`day_of_month` as `Day`,"
            + " `product_class`.`product_family` as `Product Family`,"
            + " `product_class`.`product_department` as `Product Department`,"
            + " `product_class`.`product_category` as `Product Category`,"
            + " `product_class`.`product_subcategory` as `Product Subcategory`,"
            + " `product`.`brand_name` as `Brand Name`,"
            + " `product`.`product_name` as `Product Name`,"
            + " `promotion`.`media_type` as `Media Type`,"
            + " `promotion`.`promotion_name` as `Promotion Name`,"
            + " `customer`.`country` as `Country`,"
            + " `customer`.`state_province` as `State Province`,"
            + " `customer`.`city` as `City`, "
            + nameExpStr
            + " as `Name`,"
            + " `customer`.`customer_id` as `Name (Key)`,"
            + " `customer`.`gender` as `Gender`,"
            + " `customer`.`marital_status` as `Marital Status`,"
            + " `customer`.`education` as `Education`,"
            + " `customer`.`yearly_income` as `Yearly Income`,"
            + " `sales_fact_1997`.`unit_sales` as `Unit Sales` "
            + "from `sales_fact_1997` =as= `sales_fact_1997`,"
            + " `store` =as= `store`,"
            + " `time_by_day` =as= `time_by_day`,"
            + " `product_class` =as= `product_class`,"
            + " `product` =as= `product`,"
            + " `promotion` =as= `promotion`,"
            + " `customer` =as= `customer` "
            + "where `sales_fact_1997`.`store_id` = `store`.`store_id` and"
            + " `store`.`store_state` = 'CA' and"
            + " `store`.`store_city` = 'Beverly Hills' and"
            + " `sales_fact_1997`.time_id` = `time_by_day`.`time_id` and"
            + " `sales_fact_1997`.`product_id` = `product`.`product_id` and"
            + " `product`.`product_class_id` = `product_class`.`product_class_id` and"
            + " `sales_fact_1997`.`promotion_id` = `promotion`.`promotion_id` and"
            + " `sales_fact_1997`.`customer_id` = `customer`.`customer_id`"
            + " order by"
            + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                ? " `Store State` ASC,"
                + " `Store City` ASC,"
                + " `Store Name` ASC,"
                + " `Store Type` ASC,"
                + " `Store Sqft` ASC,"
                + " `Year` ASC,"
                + " `Quarter` ASC,"
                + " `Month` ASC,"
                + " `Week` ASC,"
                + " `Day` ASC,"
                + " `Product Family` ASC,"
                + " `Product Department` ASC,"
                + " `Product Category` ASC,"
                + " `Product Subcategory` ASC,"
                + " `Brand Name` ASC,"
                + " `Product Name` ASC,"
                + " `Media Type` ASC,"
                + " `Promotion Name` ASC,"
                + " `Country` ASC,"
                + " `State Province` ASC,"
                + " `City` ASC,"
                + " `Name` ASC,"
                + " `Name (Key)` ASC,"
                + " `Gender` ASC,"
                + " `Marital Status` ASC,"
                + " `Education` ASC,"
                + " `Yearly Income` ASC"
                : " `store`.`store_state` ASC,"
                + " `store`.`store_city` ASC,"
                + " `store`.`store_name` ASC,"
                + " `store`.`store_sqft` ASC,"
                + " `store`.`store_type` ASC,"
                + " `time_by_day`.`the_year` ASC,"
                + " `time_by_day`.`quarter` ASC,"
                + " `time_by_day`.`month_of_year` ASC,"
                + " `time_by_day`.`week_of_year` ASC,"
                + " `time_by_day`.`day_of_month` ASC,"
                + " `product_class`.`product_family` ASC,"
                + " `product_class`.`product_department` ASC,"
                + " `product_class`.`product_category` ASC,"
                + " `product_class`.`product_subcategory` ASC,"
                + " `product`.`brand_name` ASC,"
                + " `product`.`product_name` ASC,"
                + " `promotion`.`media_type` ASC,"
                + " `promotion`.`promotion_name` ASC,"
                + " `customer`.`country` ASC,"
                + " `customer`.`state_province` ASC,"
                + " `customer`.`city` ASC, "
                + nameExpStr
                + " ASC,"
                + " `customer`.`customer_id` ASC,"
                + " `customer`.`gender` ASC,"
                + " `customer`.`marital_status` ASC,"
                + " `customer`.`education` ASC,"
                + " `customer`.`yearly_income` ASC");

        assertSqlEquals(context.getConnectionWithDefaultRole(), expectedSql, sql, 6815);
    }

    /**
     * Tests that proper SQL is being generated for a Measure specified
     * as an expression.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testDrillThroughMeasureExp(Context<?> context) {
        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "SELECT {[Measures].[Promotion Sales]} on columns,\n"
            + " {[Product].Children} on rows\n"
            + "from Sales");
        String sql = result.getCell(new int[] {0, 0}).getDrillThroughSQL(false);

        String expectedSql =
            "select"
            + " `time_by_day`.`the_year` as `Year`,"
            + " `product_class`.`product_family` as `Product Family`,"
            + " (case when `sales_fact_1997`.`promotion_id` = 0 then 0"
            + " else `sales_fact_1997`.`store_sales` end)"
            + " as `Promotion Sales` "
            + "from `sales_fact_1997` =as= `sales_fact_1997`,"
            + " `time_by_day` =as= `time_by_day`,"
            + " `product_class` =as= `product_class`,"
            + " `product` =as= `product` "
            + "where `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`"
            + " and `time_by_day`.`the_year` = 1997"
            + " and `sales_fact_1997`.`product_id` = `product`.`product_id`"
            + " and `product`.`product_class_id` = `product_class`.`product_class_id`"
            + " and `product_class`.`product_family` = 'Drink' "
            + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                ? "order by `Year` ASC, `Product Family` ASC"
                : "order by `time_by_day`.`the_year` ASC, `product_class`.`product_family` ASC");

        final Cube cube = result.getQuery().getCube();
        RolapStar star = ((RolapCube) cube).getStar();

        // Adjust expected SQL for dialect differences in FoodMart.xml.
        Dialect dialect = star.getSqlQueryDialect();
        final String caseStmt =
            " \\(case when `sales_fact_1997`.`promotion_id` = 0 then 0"
            + " else `sales_fact_1997`.`store_sales` end\\)";
        switch (getDatabaseProduct(dialect.getDialectName())) {
        case ACCESS:
            expectedSql = expectedSql.replaceAll(
                caseStmt,
                " Iif(`sales_fact_1997`.`promotion_id` = 0, 0,"
                + " `sales_fact_1997`.`store_sales`)");
            break;
        case INFOBRIGHT:
            expectedSql = expectedSql.replaceAll(
                caseStmt, " `sales_fact_1997`.`store_sales`");
            break;
        }

        assertSqlEquals(context.getConnectionWithDefaultRole(), expectedSql, sql, 7978);
    }

    /**
     * Tests that drill-through works if two dimension tables have primary key
     * columns with the same name. Related to bug 1592556, "XMLA Drill through
     * bug".
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void  testDrillThroughDupKeys(Context<?> context) {
         // Note here that the type on the Store Id level is Integer or
         // Numeric. The default, of course, would be String.
         //
         // For DB2 and Derby, we need the Integer type, otherwise the
         // generated SQL will be something like:
         //
         //      `store_ragged`.`store_id` = '19'
         //
         //  and DB2 and Derby don't like converting from CHAR to INTEGER

        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "  <Dimension name=\"Store2\" foreignKey=\"store_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">\n"
            + "      <Table name=\"store_ragged\"/>\n"
            + "      <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"Store Id\" column=\"store_id\" captionColumn=\"store_name\" uniqueMembers=\"true\" type=\"Integer\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Dimension name=\"Store3\" foreignKey=\"store_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">\n"
            + "      <Table name=\"store\"/>\n"
            + "      <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"Store Id\" column=\"store_id\" captionColumn=\"store_name\" uniqueMembers=\"true\" type=\"Numeric\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"));
         */
        withSchema(context, SchemaModifiers.DrillThroughTestModifier1::new);

        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "SELECT {[Store2].[Store Id].Members} on columns,\n"
            + " NON EMPTY([Store3].[Store Id].Members) on rows\n"
            + "from Sales");
        String sql = result.getCell(new int[] {0, 0}).getDrillThroughSQL(false);

        String expectedSql =
            "select `time_by_day`.`the_year` as `Year`,"
            + " `store_ragged`.`store_id` as `Store Id`,"
            + " `store`.`store_id` as `Store Id_0`,"
            + " `sales_fact_1997`.`unit_sales` as `Unit Sales` "
            + "from `sales_fact_1997` =as= `sales_fact_1997`,"
            + " `time_by_day` =as= `time_by_day`,"
            + " `store_ragged` =as= `store_ragged`,"
            + " `store` =as= `store` "
            + "where `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`"
            + " and `time_by_day`.`the_year` = 1997"
            + " and `sales_fact_1997`.`store_id` = `store_ragged`.`store_id`"
            + " and `store_ragged`.`store_id` = 19"
            + " and `sales_fact_1997`.`store_id` = `store`.`store_id`"
            + " and `store`.`store_id` = 2 "
            + "order by "
            + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                ? "`Year` ASC, `Store Id` ASC, `Store Id_0` ASC"
                : "`time_by_day`.`the_year` ASC, `store_ragged`.`store_id` ASC, `store`.`store_id` ASC");

        assertSqlEquals(context.getConnectionWithDefaultRole(), expectedSql, sql, 0);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void  testDrillThroughDupKeysAndMeasure(Context<?> context) throws Exception {
        if (!getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())
            .equals(DatabaseProduct.MYSQL))
        {
            // This test only works on MySQL because we
            // check the SQL generated by drillthrough.
            return;
        }
        withSchema(context, SchemaModifiers.DrillThroughTestModifier4::new);
       final Result result = executeQuery(context.getConnectionWithDefaultRole(),
           "WITH\n"
           + "SET [*NATIVE_CJ_SET] AS 'FILTER([Frozen sqft].[Frozen sqft].MEMBERS, NOT ISEMPTY ([Measures].[Store sqft]))'\n"
           + "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],[Frozen sqft].CURRENTMEMBER.ORDERKEY,BASC)'\n"
           + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[Store sqft]}'\n"
           + "SET [*BASE_MEMBERS__Frozen sqft_] AS '[Frozen sqft].[Frozen sqft].MEMBERS'\n"
           + "SET [*CJ_ROW_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Frozen sqft].CURRENTMEMBER)})'\n"
           + "SELECT\n"
           + "FILTER([*BASE_MEMBERS__Measures_],([Measures].CurrentMember Is [Measures].[Store sqft])) ON COLUMNS\n"
           + ",FILTER( [*SORTED_ROW_AXIS],([Frozen sqft].CurrentMember Is [Frozen sqft].[2452])) ON ROWS\n"
           + "FROM [dsad]");

       final String sql =
           result.getCell(new int[]{0, 0}).getDrillThroughSQL(true);

       assertSqlEquals(context.getConnectionWithDefaultRole(),
           "select store.frozen_sqft as Frozen sqft, store.grocery_sqft as Grocery sqft, store.meat_sqft as Meat sqft, store.store_sqft as Store sqft, store.store_sqft as Store sqft_0 from store as store where store.frozen_sqft = 2452 order by Frozen sqft ASC, Grocery sqft ASC, Meat sqft ASC, Store sqft ASC",
           sql,
           1);
   }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void  testDrillThroughDupKeysAndMeasure_2(Context<?> context) throws Exception {
        withSchema(context, SchemaModifiers.DrillThroughTestModifier4::new);
        String drillThroughMdx =
            "DRILLTHROUGH WITH\n"
            + "SET [*NATIVE_CJ_SET] AS 'FILTER([Store sqft].[Store sqft].MEMBERS, NOT ISEMPTY ([Measures].[Meat sqft]))'\n"
            + "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],[Store sqft].CURRENTMEMBER.ORDERKEY,BASC)'\n"
            + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[Store sqft]}'\n"
            + "SET [*BASE_MEMBERS__Store sqft_] AS '[Store sqft].[Store sqft].MEMBERS'\n"
            + "SET [*CJ_ROW_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Store sqft].CURRENTMEMBER)})'\n"
            + "SELECT\n"
            + " [Measures].[Meat sqft] ON COLUMNS\n"
            + ", [*SORTED_ROW_AXIS]  ON ROWS\n"
            + "FROM [dsad] WHERE ( [Grocery Sqft].[24390] ) RETURN [Store sqft].[Store sqft], [Measures].[Grocery sqft]";
        Connection connection = context.getConnectionWithDefaultRole();
        ResultSet resultSet = null;
        try {
            resultSet = connection.createStatement()
                .executeQuery(drillThroughMdx, Optional.empty(), null);
            assertEquals(2, resultSet.getMetaData().getColumnCount());
            assertEquals
                ("Store sqft", resultSet.getMetaData().getColumnLabel(1));
            assertEquals
                ("Grocery sqft", resultSet.getMetaData().getColumnLabel(2));
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            connection.close();
        }
        context.getCatalogCache().clear();
    }

    /**
     * Tests that cells in a virtual cube say they can be drilled through.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void  testDrillThroughVirtualCube(Context<?> context) {
        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "select Crossjoin([Customers].[All Customers].[USA].[OR].Children, {[Measures].[Unit Sales]}) ON COLUMNS, "
            + " [Gender].[All Gender].Children ON ROWS"
            + " from [Warehouse and Sales]"
            + " where [Time].[1997].[Q4].[12]");

        String sql = result.getCell(new int[]{0, 0}).getDrillThroughSQL(false);

        String expectedSql =
            "select `time_by_day`.`the_year` as `Year`,"
            + " `time_by_day`.`quarter` as `Quarter`,"
            + " `time_by_day`.month_of_year` as `Month`,"
            + " `customer`.`state_province` as `State Province`,"
            + " `customer`.`city` as `City`,"
            + " `customer`.`gender` as `Gender`,"
            + " `sales_fact_1997`.`unit_sales` as `Unit Sales`"
            + " from `sales_fact_1997` =as= `sales_fact_1997`,"
            + " `time_by_day` =as= `time_by_day`,"
            + " `customer` =as= `customer`"
            + " where `sales_fact_1997`.`time_id` = `time_by_day`.`time_id` and"
            + " `time_by_day`.`the_year` = 1997 and"
            + " `time_by_day`.`quarter` = 'Q4' and"
            + " `time_by_day`.`month_of_year` = 12 and"
            + " `sales_fact_1997`.`customer_id` = `customer`.customer_id` and"
            + " `customer`.`state_province` = 'OR' and"
            + " `customer`.`city` = 'Albany' and"
            + " `customer`.`gender` = 'F'"
            + " order by "
            + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                ? "`Year` ASC,"
                + " `Quarter` ASC,"
                + " `Month` ASC,"
                + " `State Province` ASC,"
                + " `City` ASC,"
                + " `Gender` ASC"
                : "`time_by_day`.`the_year` ASC,"
                + " `time_by_day`.`quarter` ASC,"
                + " `time_by_day`.`month_of_year` ASC,"
                + " `customer`.`state_province` ASC,"
                + " `customer`.`city` ASC,"
                + " `customer`.`gender` ASC");

        assertSqlEquals(context.getConnectionWithDefaultRole(), expectedSql, sql, 73);
    }

    /**
     * This tests for bug 1438285, "nameColumn cannot be column in level
     * definition".
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void  testBug1438285(Context<?> context) {
        final Dialect dialect = getDialect(context.getConnectionWithDefaultRole());
        if (getDatabaseProduct(dialect.getDialectName()) == DatabaseProduct.TERADATA) {
            // On default Teradata express instance there isn't enough spool
            // space to run this query.
            return;
        }

        // Specify the column and nameColumn to be the same
        // in order to reproduce the problem

        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                "Sales",
                "  <Dimension name=\"Store2\" foreignKey=\"store_id\">\n"
                + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Stores\" >\n"
                + "      <Table name=\"store_ragged\"/>\n"
                + "      <Level name=\"Store Id\" column=\"store_id\" nameColumn=\"store_id\" ordinalColumn=\"region_id\" uniqueMembers=\"true\">\n"
                + "     </Level>"
                + "    </Hierarchy>\n"
                + "  </Dimension>\n"));

         */
        withSchema(context, SchemaModifiers.DrillThroughTestModifier2::new);

        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "SELECT {[Measures].[Unit Sales]} on columns, "
            + "{[Store2].members} on rows FROM [Sales]");

        // Prior to fix the request for the drill through SQL would result in
        // an assertion error
        String sql = result.getCell(new int[] {0, 0}).getDrillThroughSQL(true);
        String nameExpStr = getNameExp(result, "Customers", "Name");
        String expectedSql =
            "select "
            + "`store`.`store_country` as `Store Country`,"
            + " `store`.`store_state` as `Store State`,"
            + " `store`.`store_city` as `Store City`,"
            + " `store`.`store_name` as `Store Name`,"
            + " `store`.`store_type` as `Store Type`,"
            + " `store`.`store_sqft` as `Store Sqft`,"
            + " `time_by_day`.`the_year` as `Year`,"
            + " `time_by_day`.`quarter` as `Quarter`,"
            + " `time_by_day`.`month_of_year` as `Month`,"
            + " `time_by_day`.`week_of_year` as `Week`,"
            + " `time_by_day`.`day_of_month` as `Day`,"
            + " `product_class`.`product_family` as `Product Family`,"
            + " `product_class`.`product_department` as `Product Department`,"
            + " `product_class`.`product_category` as `Product Category`,"
            + " `product_class`.`product_subcategory` as `Product Subcategory`,"
            + " `product`.`brand_name` as `Brand Name`,"
            + " `product`.`product_name` as `Product Name`,"
            + " `store_ragged`.`store_id` as `Store Id`,"
            + " `promotion`.`media_type` as `Media Type`,"
            + " `promotion`.`promotion_name` as `Promotion Name`,"
            + " `customer`.`country` as `Country`,"
            + " `customer`.`state_province` as `State Province`,"
            + " `customer`.`city` as `City`,"
            + " " + nameExpStr + " as `Name`,"
            + " `customer`.`customer_id` as `Name (Key)`,"
            + " `customer`.`gender` as `Gender`,"
            + " `customer`.`marital_status` as `Marital Status`,"
            + " `customer`.`education` as `Education`,"
            + " `customer`.`yearly_income` as `Yearly Income`,"
            + " `sales_fact_1997`.`unit_sales` as `Unit Sales`"
            + " from `sales_fact_1997` =as= `sales_fact_1997`,"
            + " `store` =as= `store`,"
            + " `time_by_day` =as= `time_by_day`,"
            + " `product_class` =as= `product_class`,"
            + " `product` =as= `product`,"
            + " `store_ragged` =as= `store_ragged`,"
            + " `promotion` =as= `promotion`,"
            + " `customer` =as= `customer`"
            + " where `sales_fact_1997`.`store_id` = `store`.`store_id`"
            + " and `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`"
            + " and `time_by_day`.`the_year` = 1997"
            + " and `sales_fact_1997`.`product_id` = `product`.`product_id`"
            + " and `product`.`product_class_id` = `product_class`.`product_class_id`"
            + " and `sales_fact_1997`.`store_id` = `store_ragged`.`store_id`"
            + " and `sales_fact_1997`.`promotion_id` = `promotion`.`promotion_id`"
            + " and `sales_fact_1997`.`customer_id` = `customer`.`customer_id`"
            + " order by"
            + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                ? " `Store Country` ASC,"
                + " `Store State` ASC,"
                + " `Store City` ASC,"
                + " `Store Name` ASC,"
                + " `Store Type` ASC,"
                + " `Store Sqft` ASC,"
                + " `Year` ASC,"
                + " `Quarter` ASC,"
                + " `Month` ASC,"
                + " `Week` ASC,"
                + " `Day` ASC,"
                + " `Product Family` ASC,"
                + " `Product Department` ASC,"
                + " `Product Category` ASC,"
                + " `Product Subcategory` ASC,"
                + " `Brand Name` ASC,"
                + " `Product Name` ASC,"
                + " `Store Id` ASC,"
                + " `Media Type` ASC,"
                + " `Promotion Name` ASC,"
                + " `Country` ASC,"
                + " `State Province` ASC,"
                + " `City` ASC,"
                + " `Name` ASC,"
                + " `Name (Key)` ASC,"
                + " `Gender` ASC,"
                + " `Marital Status` ASC,"
                + " `Education` ASC,"
                + " `Yearly Income` ASC"
                : " `store`.`store_country` ASC,"
                + " `store`.`store_state` ASC,"
                + " `store`.`store_city` ASC,"
                + " `store`.`store_name` ASC,"
                + " `store`.`store_sqft` ASC,"
                + " `store`.`store_type` ASC,"
                + " `time_by_day`.`the_year` ASC,"
                + " `time_by_day`.`quarter` ASC,"
                + " `time_by_day`.`month_of_year` ASC,"
                + " `time_by_day`.`week_of_year` ASC,"
                + " `time_by_day`.`day_of_month` ASC,"
                + " `product_class`.`product_family` ASC,"
                + " `product_class`.`product_department` ASC,"
                + " `product_class`.`product_category` ASC,"
                + " `product_class`.`product_subcategory` ASC,"
                + " `product`.`brand_name` ASC,"
                + " `product`.`product_name` ASC,"
                + " `store_ragged`.`store_id` ASC,"
                + " `promotion`.`media_type` ASC,"
                + " `promotion`.`promotion_name` ASC,"
                + " `customer`.`country` ASC,"
                + " `customer`.`state_province` ASC,"
                + " `customer`.`city` ASC,"
                + " " + nameExpStr + " ASC,"
                + " `customer`.`customer_id` ASC,"
                + " `customer`.`gender` ASC,"
                + " `customer`.`marital_status` ASC,"
                + " `customer`.`education` ASC,"
                + " `customer`.`yearly_income` ASC");

        assertSqlEquals(context.getConnectionWithDefaultRole(), expectedSql, sql, 86837);
    }

    /**
     * Tests that long levels do not result in column aliases larger than the
     * database can handle. For example, Access allows maximum of 64; Oracle
     * allows 30.
     *
     * <p>Testcase for bug 1893959, "Generated drill-through columns too long
     * for DBMS".
     *
     * @throws Exception on error
     */
    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void  testTruncateLevelName(Context<?> context) throws Exception {
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "  <Dimension name=\"Education Level2\" foreignKey=\"customer_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
            + "      <Table name=\"customer\"/>\n"
            + "      <Level name=\"Education Level but with a very long name that will be too long if converted directly into a column\" column=\"education\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>",
            null));
         */
    	withSchema(context, SchemaModifiers.DrillThroughTestModifier3::new);

        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "SELECT {[Measures].[Unit Sales]} on columns,\n"
            + "{[Education Level2].Children} on rows\n"
            + "FROM [Sales]\n"
            + "WHERE ([Time].[1997].[Q1].[1], [Product].[Non-Consumable].[Carousel].[Specialty].[Sunglasses].[ADJ].[ADJ Rosy Sunglasses]) ");

        String sql = result.getCell(new int[] {0, 0}).getDrillThroughSQL(false);

        // Check that SQL is valid.
        java.sql.Connection connection = null;
        try {
            DataSource dataSource = context.getConnectionWithDefaultRole().getDataSource();
            connection = dataSource.getConnection();
            final Statement statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery(sql);
            final int columnCount = resultSet.getMetaData().getColumnCount();
            final Dialect dialect = getDialect(context.getConnectionWithDefaultRole());
            if (getDatabaseProduct(dialect.getDialectName()) == DatabaseProduct.DERBY) {
                // derby counts ORDER BY columns as columns. insane!
                assertEquals(11, columnCount);
            } else {
                assertEquals(6, columnCount);
            }
            final String columnName = resultSet.getMetaData().getColumnLabel(5);
            assertTrue(
                columnName.startsWith("Education Level but with a"), columnName);
            int n = 0;
            while (resultSet.next()) {
                ++n;
            }
            assertEquals(2, n);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void  testDrillThroughExprs(Context<?> context) {
    	context.getCatalogCache().clear();
        Connection connection = context.getConnectionWithDefaultRole();
        assertCanDrillThrough(connection,
            true,
            "Sales",
            "CoalesceEmpty([Measures].[Unit Sales], [Measures].[Store Sales])");
        assertCanDrillThrough(connection,
            true,
            "Sales",
            "[Measures].[Unit Sales] + [Measures].[Unit Sales]");
        assertCanDrillThrough(connection,
            true,
            "Sales",
            "[Measures].[Unit Sales] / ([Measures].[Unit Sales] - 5.0)");
        assertCanDrillThrough(connection,
            true,
            "Sales",
            "[Measures].[Unit Sales] * [Measures].[Unit Sales]");
        // constants are drillable - in a virtual cube it means take the first
        // cube
        assertCanDrillThrough(connection,
            true,
            "Warehouse and Sales",
            "2.0");
        assertCanDrillThrough(connection,
            true,
            "Warehouse and Sales",
            "[Measures].[Unit Sales] * 2.0");
        // in virtual cube, mixture of measures from two cubes is not drillable
        assertCanDrillThrough(connection,
            false,
            "Warehouse and Sales",
            "[Measures].[Unit Sales] + [Measures].[Units Ordered]");
        // expr with measures both from [Sales] is drillable
        assertCanDrillThrough(connection,
            true,
            "Warehouse and Sales",
            "[Measures].[Unit Sales] + [Measures].[Store Sales]");
        // expr with measures both from [Warehouse] is drillable
        assertCanDrillThrough(connection,
            true,
            "Warehouse and Sales",
            "[Measures].[Warehouse Cost] + [Measures].[Units Ordered]");
        // <Member>.Children not drillable
        assertCanDrillThrough(connection,
            false,
            "Sales",
            "Sum([Product].Children)");
        // Sets of members not drillable
        assertCanDrillThrough(connection,
            false,
            "Sales",
            "Sum({[Store].[USA], [Store].[Canada].[BC]})");
        // Tuples not drillable
        assertCanDrillThrough(connection,
            false,
            "Sales",
            "([Time].[1997].[Q1], [Measures].[Unit Sales])");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void  testDrillthroughMaxRows(Context<?> context) throws SQLException {
        Connection connection = context.getConnectionWithDefaultRole();
        assertMaxRows(connection, "", 29);
        assertMaxRows(connection, "maxrows 1000", 29);
        assertMaxRows(connection, "maxrows 0", 29);
        assertMaxRows(connection, "maxrows 3", 3);
        assertMaxRows(connection, "maxrows 10 firstrowset 6", 4);
        assertMaxRows(connection, "firstrowset 20", 9);
        assertMaxRows(connection, "firstrowset 30", 0);
    }

    private void assertMaxRows(Connection connection, String firstMaxRow, int expectedCount)
        throws SQLException
    {
        final ResultSet resultSet = executeStatement(connection,
            "drillthrough\n"
            + firstMaxRow
            + " select\n"
            + "non empty{[Customers].[USA].[CA]} on 0,\n"
            + "non empty {[Product].[Drink].[Beverages].[Pure Juice Beverages].[Juice]} on 1\n"
            + "from\n"
            + "[Sales]\n"
            + "where([Measures].[Sales Count], [Time].[1997].[Q3].[8])");
        int actualCount = 0;
        while (resultSet.next()) {
            ++actualCount;
        }
        assertEquals(expectedCount, actualCount);
        resultSet.close();
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void  testDrillthroughNegativeMaxRowsFails(Context<?> context) throws SQLException {
        Connection connection = context.getConnectionWithDefaultRole();
        try {
            final ResultSet resultSet = executeStatement(connection,
                "DRILLTHROUGH MAXROWS -3\n"
                + "SELECT {[Customers].[USA].[CA].[Berkeley]} ON 0,\n"
                + "{[Time].[1997]} ON 1\n"
                + "FROM Sales");
            fail("expected error, got " + resultSet);
        } catch (Exception e) {
            checkThrowable(
                e, "Encountered an error at (or somewhere around) input:1:22");
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void  testDrillThroughCalculatedMemberMeasure(Context<?> context) throws SQLException {
        Connection connection = context.getConnectionWithDefaultRole();
        try {
            final ResultSet resultSet = executeStatement(connection,
                "DRILLTHROUGH\n"
                + "SELECT {[Customers].[USA].[CA].[Berkeley]} ON 0,\n"
                + "{[Time].[1997]} ON 1\n"
                + "FROM Sales\n"
                + "RETURN  [Measures].[Profit]");
            fail("expected error, got " + resultSet);
        } catch (Exception e) {
            checkThrowable(
                e,
                "Can't perform drillthrough operations because '[Measures].[Profit]' is a calculated member.");
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testDrillThroughNotDrillableFails(Context<?> context) throws SQLException {
        Connection connection = context.getConnectionWithDefaultRole();
        try {
            final ResultSet resultSet = executeStatement(connection,
                "DRILLTHROUGH\n"
                + "WITH MEMBER [Measures].[Foo] "
                + " AS [Measures].[Unit Sales]\n"
                + "   + ([Measures].[Unit Sales], [Time].[Time].PrevMember)\n"
                + "SELECT {[Customers].[USA].[CA].[Berkeley]} ON 0,\n"
                + "{[Time].[1997]} ON 1\n"
                + "FROM Sales\n"
                + "WHERE [Measures].[Foo]");
            fail("expected error, got " + resultSet);
        } catch (Exception e) {
            checkThrowable(
                e, "Cannot do DrillThrough operation on the cell");
        }
    }

    /**
     * Asserts that a cell based on the given measure expression has a given
     * drillability.
     *
     * @param canDrillThrough Whether we expect the cell to be drillable
     * @param cubeName Cube name
     * @param expr Scalar expression
     */
    private void assertCanDrillThrough(Connection connection,
        boolean canDrillThrough,
        String cubeName,
        String expr)
    {
        Result result = executeQuery(connection,
            "WITH MEMBER [Measures].[Foo] AS '" + expr + "'\n"
            + "SELECT {[Measures].[Foo]} on columns,\n"
            + " {[Product].Children} on rows\n"
            + "from [" + cubeName + "]");
        final Cell cell = result.getCell(new int[] {0, 0});
        assertEquals(canDrillThrough, cell.canDrillThrough());
        final String sql = cell.getDrillThroughSQL(false);
        if (canDrillThrough) {
            assertNotNull(sql);
        } else {
            assertNull(sql);
        }
    }

    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-752">
     * MONDRIAN-752, "cell.getDrillCount returns 0".
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void  testDrillThroughOneAxis(Context<?> context) {
        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "SELECT [Measures].[Unit Sales] on 0\n"
            + "from Sales");

        final Cell cell = result.getCell(new int[]{0});
        assertTrue(cell.canDrillThrough());
        assertEquals(86837, cell.getDrillThroughCount());
    }

    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-751">
     * MONDRIAN-751, "Drill SQL does not include slicer members in WHERE
     * clause".
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void  testDrillThroughCalcMemberInSlicer(Context<?> context) {
        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Product].[Aggregate Food Drink] AS \n"
            + " Aggregate({[Product].[Food], [Product].[Drink]})\n"
            + "SELECT [Measures].[Unit Sales] on 0\n"
            + "from Sales\n"
            + "WHERE [Product].[Aggregate Food Drink]");

        final Cell cell = result.getCell(new int[]{0});
        assertFalse(cell.canDrillThrough());
    }



    /**
     * Test case for MONDRIAN-791.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void  testDrillThroughMultiPositionCompoundSlicer(Context<?> context) {
    	context.getCatalogCache().clear();
        ((TestContextImpl)context).setGenerateFormattedSql(true);
        // A query with a simple multi-position compound slicer
        Result result =
            executeQuery(context.getConnectionWithDefaultRole(),
                "SELECT {[Measures].[Unit Sales]} ON COLUMNS,\n"
                + " {[Product].[All Products]} ON ROWS\n"
                + "FROM [Sales]\n"
                + "WHERE {[Time].[1997].[Q1], [Time].[1997].[Q2]}");
        Cell cell = result.getCell(new int[]{0, 0});
        assertTrue(cell.canDrillThrough());
        String sql = cell.getDrillThroughSQL(false);
        String expectedSql;
        switch (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())) {
        case MARIADB:
        case MYSQL:
            expectedSql =
                "select\n"
                + "    time_by_day.the_year as Year,\n"
                + "    time_by_day.quarter as Quarter,\n"
                + "    sales_fact_1997.unit_sales as Unit Sales\n"
                + "from\n"
                + "    sales_fact_1997 as sales_fact_1997,\n"
                + "    time_by_day as time_by_day\n"
                + "where\n"
                + "    sales_fact_1997.time_id = time_by_day.time_id\n"
                + "and\n"
                + "    (((time_by_day.the_year, time_by_day.quarter) in ((1997, 'Q1'), (1997, 'Q2'))))";
            break;
        case ORACLE:
            expectedSql =
                "select\n"
                + "    time_by_day.the_year as Year,\n"
                + "    time_by_day.quarter as Quarter,\n"
                + "    sales_fact_1997.unit_sales as Unit Sales\n"
                + "from\n"
                + "    sales_fact_1997 sales_fact_1997,\n"
                + "    time_by_day time_by_day\n"
                + "where\n"
                + "    sales_fact_1997.time_id = time_by_day.time_id\n"
                + "and\n"
                + "    ((time_by_day.quarter = 'Q1' and time_by_day.the_year = 1997) or (time_by_day.quarter = 'Q2' and time_by_day.the_year = 1997))";
            break;
        default:
                return;
        }
        assertSqlEquals(context.getConnectionWithDefaultRole(), expectedSql, sql, 41956);

        // A query with a slightly more complex multi-position compound slicer
        result =
            executeQuery(context.getConnectionWithDefaultRole(),
                "SELECT {[Measures].[Unit Sales]} ON COLUMNS,\n"
                + " {[Product].[All Products]} ON ROWS\n"
                + "FROM [Sales]\n"
                + "WHERE Crossjoin(Crossjoin({[Gender].[F]}, {[Marital Status].[M]}),"
                + "                {[Time].[1997].[Q1], [Time].[1997].[Q2]})");
        cell = result.getCell(new int[]{0, 0});
        assertTrue(cell.canDrillThrough());
        sql = cell.getDrillThroughSQL(false);

        // Note that gender and marital status get their own predicates,
        // independent of the time portion of the slicer
        switch (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())) {
        case MARIADB:
        case MYSQL:
            expectedSql =
                "select\n"
                + "    customer.gender as Gender,\n"
                + "    customer.marital_status as Marital Status,\n"
                + "    time_by_day.the_year as Year,\n"
                + "    time_by_day.quarter as Quarter,\n"
                + "    sales_fact_1997.unit_sales as Unit Sales\n"
                + "from\n"
                + "    sales_fact_1997 as sales_fact_1997,\n"
                + "    customer as customer,\n"
                + "    time_by_day as time_by_day\n"
                + "where\n"
                + "    sales_fact_1997.customer_id = customer.customer_id\n"
                + "and\n"
                + "    customer.gender = 'F'\n"
                + "and\n"
                + "    customer.marital_status = 'M'\n"
                + "and\n"
                + "    sales_fact_1997.time_id = time_by_day.time_id\n"
                + "and\n"
                + "    (((time_by_day.the_year, time_by_day.quarter) in ((1997, 'Q1'), (1997, 'Q2'))))\n"
                + "order by\n"
                + (getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias()
                    ? "    Gender ASC,\n"
                    + "    Marital Status ASC"
                    : "    customer.gender ASC,\n"
                    + "    customer.marital_status ASC");
            break;
        case ORACLE:
            expectedSql =
                "select\n"
                + "    customer.gender as Gender,\n"
                + "    customer.marital_status as Marital Status,\n"
                + "    time_by_day.the_year as Year,\n"
                + "    time_by_day.quarter as Quarter,\n"
                + "    sales_fact_1997.unit_sales as Unit Sales\n"
                + "from\n"
                + "    sales_fact_1997 sales_fact_1997,\n"
                + "    customer customer,\n"
                + "    time_by_day time_by_day\n"
                + "where\n"
                + "    sales_fact_1997.customer_id = customer.customer_id\n"
                + "and\n"
                + "    customer.gender = 'F'\n"
                + "and\n"
                + "    customer.marital_status = 'M'\n"
                + "and\n"
                + "    sales_fact_1997.time_id = time_by_day.time_id\n"
                + "and\n"
                + "    ((time_by_day.quarter = 'Q1' and time_by_day.the_year = 1997) or (time_by_day.quarter = 'Q2' and time_by_day.the_year = 1997))\n"
                + "order by\n"
                + "    customer.gender ASC,\n"
                + "    customer.marital_status ASC";
            break;
        default:
            return;
        }
        assertSqlEquals(context.getConnectionWithDefaultRole(), expectedSql, sql, 10430);

        // A query with an even more complex multi-position compound slicer
        // (gender must be in the slicer predicate along with time)
        result =
            executeQuery(context.getConnectionWithDefaultRole(),
                "SELECT {[Measures].[Unit Sales]} ON COLUMNS,\n"
                + " {[Product].[All Products]} ON ROWS\n"
                + "FROM [Sales]\n"
                + "WHERE Union(Crossjoin({[Gender].[F]}, {[Time].[1997].[Q1]}),"
                + "            Crossjoin({[Gender].[M]}, {[Time].[1997].[Q2]}))");
        cell = result.getCell(new int[]{0, 0});
        assertTrue(cell.canDrillThrough());
        sql = cell.getDrillThroughSQL(false);

        // Note that gender and marital status get their own predicates,
        // independent of the time portion of the slicer
        switch (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())) {
        case MARIADB:
        case MYSQL:
            expectedSql =
                "select\n"
                + "    time_by_day.the_year as Year,\n"
                + "    time_by_day.quarter as Quarter,\n"
                + "    customer.gender as Gender,\n"
                + "    sales_fact_1997.unit_sales as Unit Sales\n"
                + "from\n"
                + "    sales_fact_1997 as sales_fact_1997,\n"
                + "    time_by_day as time_by_day,\n"
                + "    customer as customer\n"
                + "where\n"
                + "    sales_fact_1997.time_id = time_by_day.time_id\n"
                + "and\n"
                + "    sales_fact_1997.customer_id = customer.customer_id\n"
                + "and\n"
                + "    (((time_by_day.the_year, time_by_day.quarter, customer.gender) in ((1997, 'Q1', 'F'), (1997, 'Q2', 'M'))))";
            break;
        case ORACLE:
            expectedSql =
                "select\n"
                + "    time_by_day.the_year as Year,\n"
                + "    time_by_day.quarter as Quarter,\n"
                + "    customer.gender as Gender,\n"
                + "    sales_fact_1997.unit_sales as Unit Sales\n"
                + "from\n"
                + "    sales_fact_1997 sales_fact_1997,\n"
                + "    time_by_day time_by_day,\n"
                + "    customer customer\n"
                + "where\n"
                + "    sales_fact_1997.time_id = time_by_day.time_id\n"
                + "and\n"
                + "    sales_fact_1997.customer_id = customer.customer_id\n"
                + "and\n"
                + "    ((customer.gender = 'F' and time_by_day.quarter = 'Q1' and time_by_day.the_year = 1997) or (customer.gender = 'M' and time_by_day.quarter = 'Q2' and time_by_day.the_year = 1997))";
            break;
        default:
            return;
        }
        assertSqlEquals(context.getConnectionWithDefaultRole(), expectedSql, sql, 20971);

        // A query with a simple multi-position compound slicer with
        // different levels (overlapping)
        result =
            executeQuery(context.getConnectionWithDefaultRole(),
                "SELECT {[Measures].[Unit Sales]} ON COLUMNS,\n"
                + " {[Product].[All Products]} ON ROWS\n"
                + "FROM [Sales]\n"
                + "WHERE {[Time].[1997].[Q1], [Time].[1997].[Q1].[1]}");
        cell = result.getCell(new int[]{0, 0});
        assertTrue(cell.canDrillThrough());
        sql = cell.getDrillThroughSQL(false);

        // With overlapping slicer members, the first slicer predicate is
        // redundant, but does not affect the query's results
        switch (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())) {
        case MARIADB:
        case MYSQL:
            expectedSql =
                "select\n"
                + "    time_by_day.the_year as Year,\n"
                + "    time_by_day.quarter as Quarter,\n"
                + "    time_by_day.month_of_year as Month,\n"
                + "    sales_fact_1997.unit_sales as Unit Sales\n"
                + "from\n"
                + "    sales_fact_1997 as sales_fact_1997,\n"
                + "    time_by_day as time_by_day\n"
                + "where\n"
                + "    sales_fact_1997.time_id = time_by_day.time_id\n"
                + "and\n"
                + "    ((time_by_day.quarter = 'Q1' and time_by_day.the_year = 1997) or (time_by_day.month_of_year = 1 and time_by_day.quarter = 'Q1' and time_by_day.the_year = 1997))";
            break;
        case ORACLE:
            expectedSql =
                "select\n"
                + "    time_by_day.the_year as Year,\n"
                + "    time_by_day.quarter as Quarter,\n"
                + "    time_by_day.month_of_year as Month,\n"
                + "    sales_fact_1997.unit_sales as Unit Sales\n"
                + "from\n"
                + "    sales_fact_1997 sales_fact_1997,\n"
                + "    time_by_day time_by_day\n"
                + "where\n"
                + "    sales_fact_1997.time_id = time_by_day.time_id\n"
                + "and\n"
                + "    ((time_by_day.quarter = 'Q1' and time_by_day.the_year = 1997) or (time_by_day.month_of_year = 1 and time_by_day.quarter = 'Q1' and time_by_day.the_year = 1997))";
            break;
        default:
            return;
        }
        assertSqlEquals(context.getConnectionWithDefaultRole(), expectedSql, sql, 21588);

        // A query with a simple multi-position compound slicer with
        // different levels (non-overlapping)
        result =
            executeQuery(context.getConnectionWithDefaultRole(),
                "SELECT {[Measures].[Unit Sales]} ON COLUMNS,\n"
                + " {[Product].[All Products]} ON ROWS\n"
                + "FROM [Sales]\n"
                + "WHERE {[Time].[1997].[Q1].[1], [Time].[1997].[Q2]}");
        cell = result.getCell(new int[]{0, 0});
        assertTrue(cell.canDrillThrough());
        sql = cell.getDrillThroughSQL(false);
        switch (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())) {
        case MARIADB:
        case MYSQL:
            expectedSql =
                "select\n"
                + "    time_by_day.the_year as Year,\n"
                + "    time_by_day.quarter as Quarter,\n"
                + "    time_by_day.month_of_year as Month,\n"
                + "    sales_fact_1997.unit_sales as Unit Sales\n"
                + "from\n"
                + "    sales_fact_1997 as sales_fact_1997,\n"
                + "    time_by_day as time_by_day\n"
                + "where\n"
                + "    sales_fact_1997.time_id = time_by_day.time_id\n"
                + "and\n"
                + "    ((time_by_day.month_of_year = 1 and time_by_day.quarter = 'Q1' and time_by_day.the_year = 1997) or (time_by_day.quarter = 'Q2' and time_by_day.the_year = 1997))";
            break;
        case ORACLE:
            expectedSql =
                "select\n"
                + "    time_by_day.the_year as Year,\n"
                + "    time_by_day.quarter as Quarter,\n"
                + "    time_by_day.month_of_year as Month,\n"
                + "    sales_fact_1997.unit_sales as Unit Sales\n"
                + "from\n"
                + "    sales_fact_1997 sales_fact_1997,\n"
                + "    time_by_day time_by_day\n"
                + "where\n"
                + "    sales_fact_1997.time_id = time_by_day.time_id\n"
                + "and\n"
                + "    ((time_by_day.month_of_year = 1 and time_by_day.quarter = 'Q1' and time_by_day.the_year = 1997) or (time_by_day.quarter = 'Q2' and time_by_day.the_year = 1997))";
            break;
        default:
            return;
        }
        assertSqlEquals(context.getConnectionWithDefaultRole(), expectedSql, sql, 27402);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void  testDrillthroughDisable(Context<?> context) {
        ((TestContextImpl)context).setEnableDrillThrough(true);
        Result result =
            executeQuery(context.getConnectionWithDefaultRole(),
                "SELECT {[Measures].[Unit Sales]} ON COLUMNS,\n"
                + " {[Product].[All Products]} ON ROWS\n"
                + "FROM [Sales]\n"
                + "WHERE {[Time].[1997].[Q1], [Time].[1997].[Q2]}");
        Cell cell = result.getCell(new int[]{0, 0});
        assertTrue(cell.canDrillThrough());
        ((TestContextImpl)context).setEnableDrillThrough(false);
        result =
            executeQuery(context.getConnectionWithDefaultRole(),
                "SELECT {[Measures].[Unit Sales]} ON COLUMNS,\n"
                + " {[Product].[All Products]} ON ROWS\n"
                + "FROM [Sales]\n"
                + "WHERE {[Time].[1997].[Q1], [Time].[1997].[Q2]}");
        cell = result.getCell(new int[]{0, 0});
        assertFalse(cell.canDrillThrough());
        try {
            cell.getDrillThroughSQL(false);
            fail();
        } catch (OlapRuntimeException e) {
            assertTrue(
                e.getMessage().contains(
                    "Can't perform drillthrough operations because"));
        }
    }

    /**
     * Tests that dialects that require alias in order by are correctly quoted
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1983">MONDRIAN-1983</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void  testColumnAliasQuotedInOrderBy(Context<?> context) throws Exception {
        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "WITH\n"
            + "SET [*NATIVE_CJ_SET] AS 'FILTER([*BASE_MEMBERS__Customers_], NOT ISEMPTY ([Measures].[Unit Sales]))'\n"
            + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0]}'\n"
            + "SET [*BASE_MEMBERS__Customers_] AS '[Customers].[Name].MEMBERS'\n"
            + "SET [*CJ_ROW_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Customers].CURRENTMEMBER)})'\n"
            + "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],"
            + "[Customers].CURRENTMEMBER.ORDERKEY,BASC,"
            + "ANCESTOR([Customers].CURRENTMEMBER,[Customers].[City]).ORDERKEY,BASC)'\n"
            + "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Unit Sales]',"
            + " FORMAT_STRING = 'Standard', SOLVE_ORDER=500\n"
            + "SELECT\n"
            + "FILTER([*BASE_MEMBERS__Measures_],([Measures].CurrentMember Is [Measures].[*FORMATTED_MEASURE_0])) ON COLUMNS\n"
            + ",FILTER([*SORTED_ROW_AXIS],([Customers].CurrentMember Is [Customers].[USA].[CA].[San Gabriel].[A. Joyce Jarvis])) ON ROWS\n"
            + "FROM [Sales]");
        Cell cell = result.getCell(new int[]{0, 0});
        String sql = cell.getDrillThroughSQL(true);
        String expectedSql;
        switch (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())) {
        case VECTORWISE:
            expectedSql =
                "select \"store\".\"store_country\" as \"Store Country\","
                + " \"store\".\"store_state\" as \"Store State\","
                + " \"store\".\"store_city\" as \"Store City\","
                + " \"store\".\"store_name\" as \"Store Name\","
                + " \"store\".\"store_sqft\" as \"Store Sqft\","
                + " \"store\".\"store_type\" as \"Store Type\","
                + " \"time_by_day\".\"the_year\" as \"Year\","
                + " \"time_by_day\".\"quarter\" as \"Quarter\","
                + " \"time_by_day\".\"month_of_year\" as \"Month\","
                + " \"time_by_day\".\"week_of_year\" as \"Week\","
                + " \"time_by_day\".\"day_of_month\" as \"Day\","
                + " \"product_class\".\"product_family\" as \"Product Family\","
                + " \"product_class\".\"product_department\" as \"Product Department\", "
                + "\"product_class\".\"product_category\" as \"Product Category\","
                + " \"product_class\".\"product_subcategory\" as \"Product Subcategory\","
                + " \"product\".\"brand_name\" as \"Brand Name\","
                + " \"product\".\"product_name\" as \"Product Name\","
                + " \"promotion\".\"media_type\" as \"Media Type\","
                + " \"promotion\".\"promotion_name\" as \"Promotion Name\", "
                + "fullname as \"Name\", "
                + "\"customer\".\"customer_id\" as \"Name (Key)\","
                + " \"customer\".\"education\" as \"Education Level\","
                + " \"customer\".\"gender\" as \"Gender\","
                + " \"customer\".\"marital_status\" as \"Marital Status\","
                + " \"customer\".\"yearly_income\" as \"Yearly Income\","
                + " \"sales_fact_1997\".\"unit_sales\" as \"Unit Sales\""
                + " from \"store\" as \"store\", \"sales_fact_1997\" as \"sales_fact_1997\","
                + " \"time_by_day\" as \"time_by_day\", \"product_class\" as \"product_class\","
                + " \"product\" as \"product\", \"promotion\" as \"promotion\","
                + " \"customer\" as \"customer\" "
                + "where \"sales_fact_1997\".\"store_id\" = \"store\".\"store_id\" "
                + "and \"sales_fact_1997\".\"time_id\" = \"time_by_day\".\"time_id\" "
                + "and \"time_by_day\".\"the_year\" = 1997 "
                + "and \"sales_fact_1997\".\"product_id\" = \"product\".\"product_id\" "
                + "and \"product\".\"product_class_id\" = \"product_class\".\"product_class_id\" "
                + "and \"sales_fact_1997\".\"promotion_id\" = \"promotion\".\"promotion_id\" "
                + "and \"sales_fact_1997\".\"customer_id\" = \"customer\".\"customer_id\" "
                + "and \"customer\".\"customer_id\" = 665 "
                + "order by \"Store Country\" ASC, "
                + "\"Store State\" ASC, \"Store City\" ASC, "
                + "\"Store Name\" ASC, \"Store Sqft\" ASC, "
                + "\"Store Type\" ASC, \"Year\" ASC,"
                + " \"Quarter\" ASC, "
                + "\"Month\" ASC, "
                + "\"Week\" ASC, "
                + "\"Day\" ASC, "
                + "\"Product Family\" ASC, "
                + "\"Product Department\" ASC, "
                + "\"Product Category\" ASC, "
                + "\"Product Subcategory\" ASC, "
                + "\"Brand Name\" ASC, "
                + "\"Product Name\" ASC, "
                + "\"Media Type\" ASC, "
                + "\"Promotion Name\" ASC, "
                + "\"Name\" ASC, "
                + "\"Name (Key)\" ASC, "
                + "\"Education Level\" ASC, "
                + "\"Gender\" ASC, "
                + "\"Marital Status\" ASC, "
                + "\"Yearly Income\" ASC";
            break;
        default:
            return;
        }
        assertSqlEquals(context.getConnectionWithDefaultRole(), expectedSql, sql, 11);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void  testDrillthroughVirtualCubeWithReturnClause(Context<?> context)
        throws SQLException
    {
        // Validates that a RETURN clause including a mix of applicable
        // and inapplicable fields will not cause an error (ANALYZER-3017)
        // Should return w/o error and have NULL values for inapplicable
        // columns.
        ResultSet rs = null;
        try {
            rs = executeStatement(context.getConnectionWithDefaultRole(),
                "DRILLTHROUGH \n"
                + "// Request ID: d73ea21c-2a29-11e5-ba1d-d4bed923da37 - RUN_REPORT\n"
                + "WITH\n"
                + "SET [*NATIVE_CJ_SET] AS 'FILTER([*BASE_MEMBERS__Gender_], NOT ISEMPTY ([Measures].[Unit Sales]))'\n"
                + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0]}'\n"
                + "SET [*CJ_SLICER_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Gender].CURRENTMEMBER)})'\n"
                + "SET [*BASE_MEMBERS__Gender_] AS '{[Gender].[F]}'\n"
                + "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=500\n"
                + "SELECT\n"
                + "FILTER([*BASE_MEMBERS__Measures_],([Measures].CurrentMember Is [Measures].[*FORMATTED_MEASURE_0])) ON COLUMNS\n"
                + "FROM [Warehouse and Sales]\n"
                + "WHERE ([*CJ_SLICER_AXIS]) RETURN [Gender].[Gender], [Measures].[Unit Sales], [Measures].[Warehouse Sales], [Time].[Year], [Warehouse].[Country]");
            assertEquals(
                5, rs.getMetaData().getColumnCount());
            Object expectedYear;
            switch (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())) {
            case MARIADB:
            case MYSQL:
                expectedYear = new Integer(1997);
                break;
            case ORACLE:
                expectedYear = new BigDecimal(1997);
                break;
            default:
                return;
            }
            while (rs.next()) {
                assertEquals(
                    expectedYear, rs.getObject(1),
                        "Each year in results should be 1997");
                assertEquals(
                    "F", rs.getObject(2),
                        "Each gender in results should be F");
                assertNotNull(
                    rs.getObject(3),
                        "Should be a non-null value for unit sales");
                assertEquals(
                    null, rs.getObject(4),
                        "Non applicable fields should be null");
                assertEquals(
                    null, rs.getObject(5),
                        "Non applicable fields should be null");
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void  testDrillThroughWithReturnClause_ReturnsNameColumn(Context<?> context)
        throws SQLException
    {
        ResultSet rs = null;
        withSchema(context, SchemaModifiers.DrillThroughTestModifier5::new);
        int rowCount = 0;
        try {
            rs = executeStatement(context.getConnectionWithDefaultRole(),
                DRILLTHROUGH_QUERY_WITH_CUSTOMER_FULL_NAME);
            assertEquals(
                5, rs.getMetaData().getColumnCount());
            assertEquals(
                "Customer Level Name", rs.getMetaData().getColumnLabel(1));
            assertEquals(
                "Customer Level Name (Key)",
                rs.getMetaData().getColumnLabel(2));
            assertEquals(
                "Product Level Name",
                rs.getMetaData().getColumnLabel(3));
            assertEquals(
                "Product Level Name (Key)",
                rs.getMetaData().getColumnLabel(4));
            assertEquals(
                "Store Sales", rs.getMetaData().getColumnLabel(5));

            while (rs.next()) {
                ++rowCount;
                assertEquals(
                    "Jeanne Derry", rs.getObject(1),
                        "Each Customer full name in results should be Jeanne Derry");
                assertEquals(
                    3, rs.getObject(2),
                        "Each customer key should be 3");
                assertNotNull(
                        rs.getObject(3),
                        "Should be a non-null value for product name");
                assertNotNull(
                        rs.getObject(4),
                        "Should be a non-null value for product key");
                assertNotNull(
                        rs.getObject(5),
                        "Should be a non-null value for store sales");
            }
            assertEquals(
                17, rowCount);
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void  testDrillThroughWithReturnClause_ReturnsNoNameColumn(Context<?> context)
            throws SQLException
    {
        ResultSet rs = null;
        withSchema(context, SchemaModifiers.DrillThroughTestModifier6::new);
        int rowCount = 0;
        try {
            rs = executeStatement(context.getConnectionWithDefaultRole(),
                DRILLTHROUGH_QUERY_WITH_CUSTOMER_ID);
            assertEquals(
                3, rs.getMetaData().getColumnCount());
            assertEquals(
                "Customer Level Name", rs.getMetaData().getColumnLabel(1));
            assertEquals(
                "Product Level Name", rs.getMetaData().getColumnLabel(2));
            assertEquals(
                "Store Sales", rs.getMetaData().getColumnLabel(3));

            while (rs.next()) {
                ++rowCount;
                assertEquals(3, rs.getObject(1), "Each customer key should be 3");
                assertNotNull(rs.getObject(2), "Should be a non-null value for product key");
                assertNotNull(rs.getObject(3), "Should be a non-null value for store sales");
            }
            assertEquals(
                17, rowCount);
        } finally {
            if (rs != null) {
                rs.close();
            }
            context.getCatalogCache().clear();
        }
    }
    /**
    * Testcase for bug
    * <a href="http://jira.pentaho.com/browse/MONDRIAN-2551">MONDRIAN-2551,
    * "Drill-through filtering not working properly
    * when level is used as filter"</a>.
    */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testMultipleFilterByLevel_NoDuplicatedColumnsInResult(Context<?> context)
        throws SQLException
    {
        String[] expectedColumnValues = {"Gourmet Supermarket",
          "Small Grocery"};
        Set<String> expectedValues =
            new HashSet<>(Arrays.asList(expectedColumnValues));
        int expectedRowCount = 10859;
        int rowCount = 0;
        ResultSet rs = null;
        try {
            rs = executeStatement(context.getConnectionWithDefaultRole(),
                "DRILLTHROUGH \n"
                + "WITH\n"
                + "SET [*NATIVE_CJ_SET_WITH_SLICER] AS 'FILTER({[Store Type].[All Store Types].[Gourmet Supermarket],[Store Type].[All Store Types].[Small Grocery]}, NOT ISEMPTY ([Measures].[Store Sales]))'\n"
                + "SET [*NATIVE_CJ_SET] AS '[*NATIVE_CJ_SET_WITH_SLICER]'\n"
                + "SET [*BASE_MEMBERS__Store Type_] AS '{[Store Type].[All Store Types].[Gourmet Supermarket],[Store Type].[All Store Types].[Small Grocery]}'\n"
                + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0]}'\n"
                + "SET [*CJ_SLICER_AXIS] AS 'GENERATE([*NATIVE_CJ_SET_WITH_SLICER], {([Store Type].CURRENTMEMBER)})'\n"
                + "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Store Sales]', FORMAT_STRING = '#,###.00', SOLVE_ORDER=500\n"
                + "SELECT\n"
                + "FILTER([*BASE_MEMBERS__Measures_],([Measures].CurrentMember Is [Measures].[*FORMATTED_MEASURE_0])) ON COLUMNS\n"
                + "FROM [Sales]\n"
                + "WHERE ([*CJ_SLICER_AXIS]) RETURN [Store Type].[Store Type]");
            assertEquals(
                1, rs.getMetaData().getColumnCount(),
                    "This DRILLTHROUGH Result should contain only one column - ");
            assertEquals(
                "Store Type", rs.getMetaData().getColumnLabel(1));
            while (rs.next()) {
                ++rowCount;
                assertTrue(
                        expectedValues.contains(rs.getObject(1)),
                        "Store Type in results should be either Small Grocery or Gourmet Supermarket");
            }
            assertEquals(
                expectedRowCount, rowCount);
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
    }
}
