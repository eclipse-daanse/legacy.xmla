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
// jhyde, Feb 14, 2003
*/
package mondrian.test;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertQueryThrows;
import static org.opencube.junit5.TestUtil.assertSetExprDependsOn;
import static org.opencube.junit5.TestUtil.executeQuery;
import static org.opencube.junit5.TestUtil.flushSchemaCache;
import static org.opencube.junit5.TestUtil.getDialect;
import static org.opencube.junit5.TestUtil.verifySameNativeAndNot;
import static org.opencube.junit5.TestUtil.withSchema;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.rolap.mapping.api.model.CalculatedMemberMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.NamedSetMapping;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.CalculatedMemberMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.CalculatedMemberPropertyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.NamedSetMappingImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.SystemWideProperties;

/**
 * Unit-test for named sets, in all their various forms: <code>WITH SET</code>,
 * sets defined against cubes, virtual cubes, and at the schema level.
 *
 * @author jhyde
 * @since April 30, 2005
 */
class NamedSetTest {

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

    /**
     * Set defined in query according measures, hence context-dependent.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSet(Context<?> context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH\n"
            + "    SET [Top Sellers]\n"
            + "AS \n"
            + "    'TopCount([Warehouse].[Warehouse Name].MEMBERS, 10, \n"
            + "        [Measures].[Warehouse Sales])'\n"
            + "SELECT \n"
            + "    {[Measures].[Warehouse Sales]} ON COLUMNS,\n"
            + "        {[Top Sellers]} ON ROWS\n"
            + "FROM \n"
            + "    [Warehouse]\n"
            + "WHERE \n"
            + "    [Time].[Year].[1997]",
            "Axis #0:\n"
            + "{[Time].[Time].[1997]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Warehouse Sales]}\n"
            + "Axis #2:\n"
            + "{[Warehouse].[Warehouse].[USA].[OR].[Salem].[Treehouse Distribution]}\n"
            + "{[Warehouse].[Warehouse].[USA].[WA].[Tacoma].[Jorge Garcia, Inc.]}\n"
            + "{[Warehouse].[Warehouse].[USA].[CA].[Los Angeles].[Artesia Warehousing, Inc.]}\n"
            + "{[Warehouse].[Warehouse].[USA].[CA].[San Diego].[Jorgensen Service Storage]}\n"
            + "{[Warehouse].[Warehouse].[USA].[WA].[Bremerton].[Destination, Inc.]}\n"
            + "{[Warehouse].[Warehouse].[USA].[WA].[Seattle].[Quality Warehousing and Trucking]}\n"
            + "{[Warehouse].[Warehouse].[USA].[WA].[Spokane].[Jones International]}\n"
            + "{[Warehouse].[Warehouse].[USA].[WA].[Yakima].[Maddock Stored Foods]}\n"
            + "{[Warehouse].[Warehouse].[USA].[CA].[Beverly Hills].[Big  Quality Warehouse]}\n"
            + "{[Warehouse].[Warehouse].[USA].[OR].[Portland].[Quality Distribution, Inc.]}\n"
            + "Row #0: 31,116.375\n"
            + "Row #1: 30,743.772\n"
            + "Row #2: 22,907.959\n"
            + "Row #3: 22,869.79\n"
            + "Row #4: 22,187.418\n"
            + "Row #5: 22,046.942\n"
            + "Row #6: 10,879.674\n"
            + "Row #7: 10,212.201\n"
            + "Row #8: 10,156.496\n"
            + "Row #9: 7,718.678\n");
    }

    /**
     * Set defined on top of calc member.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSetOnMember(Context<?> context) {
        switch (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())) {
        case INFOBRIGHT:
            // Mondrian generates 'select ... sum(warehouse_sales) -
            // sum(warehouse_cost) as c ... order by c4', correctly, but
            // Infobright gives error "'c4' isn't in GROUP BY".
            return;
        }
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH\n"
            + "    MEMBER [Measures].[Profit]\n"
            + "AS '[Measures].[Warehouse Sales] - [Measures].[Warehouse Cost] '\n"
            + "    SET [Top Performers]\n"
            + "AS \n"
            + "    'TopCount([Warehouse].[Warehouse Name].MEMBERS, 5, \n"
            + "        [Measures].[Profit])'\n"
            + "SELECT \n"
            + "    {[Measures].[Profit]} ON COLUMNS,\n"
            + "        {[Top Performers]} ON ROWS\n"
            + "FROM \n"
            + "    [Warehouse]\n"
            + "WHERE \n"
            + "    [Time].[Year].[1997].[Q2]",
            "Axis #0:\n"
            + "{[Time].[Time].[1997].[Q2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Profit]}\n"
            + "Axis #2:\n"
            + "{[Warehouse].[Warehouse].[USA].[WA].[Bremerton].[Destination, Inc.]}\n"
            + "{[Warehouse].[Warehouse].[USA].[CA].[San Diego].[Jorgensen Service Storage]}\n"
            + "{[Warehouse].[Warehouse].[USA].[OR].[Salem].[Treehouse Distribution]}\n"
            + "{[Warehouse].[Warehouse].[USA].[CA].[Los Angeles].[Artesia Warehousing, Inc.]}\n"
            + "{[Warehouse].[Warehouse].[USA].[WA].[Seattle].[Quality Warehousing and Trucking]}\n"
            + "Row #0: 4,516.756\n"
            + "Row #1: 4,189.36\n"
            + "Row #2: 4,169.318\n"
            + "Row #3: 3,848.647\n"
            + "Row #4: 3,708.717\n");
    }

    /**
     * Set defined by explicit tlist in query.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSetAsList(Context<?> context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH SET [ChardonnayChablis] AS\n"
            + "   '{[Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Good].[Good Chardonnay],\n"
            + "   [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Pearl].[Pearl Chardonnay],\n"
            + "   [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Portsmouth].[Portsmouth Chardonnay],\n"
            + "   [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Top Measure].[Top Measure Chardonnay],\n"
            + "   [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Walrus].[Walrus Chardonnay],\n"
            + "   [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Good].[Good Chablis Wine],\n"
            + "   [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Pearl].[Pearl Chablis Wine],\n"
            + "   [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Portsmouth].[Portsmouth Chablis Wine],\n"
            + "   [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Top Measure].[Top Measure Chablis Wine],\n"
            + "   [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Walrus].[Walrus Chablis Wine]}'\n"
            + "SELECT\n"
            + "   [ChardonnayChablis] ON COLUMNS,\n"
            + "   {Measures.[Unit Sales]} ON ROWS\n"
            + "FROM Sales",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Good].[Good Chardonnay]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Pearl].[Pearl Chardonnay]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Portsmouth].[Portsmouth Chardonnay]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Top Measure].[Top Measure Chardonnay]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Walrus].[Walrus Chardonnay]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Good].[Good Chablis Wine]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Pearl].[Pearl Chablis Wine]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Portsmouth].[Portsmouth Chablis Wine]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Top Measure].[Top Measure Chablis Wine]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Wine].[Walrus].[Walrus Chablis Wine]}\n"
            + "Axis #2:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Row #0: 192\n"
            + "Row #0: 189\n"
            + "Row #0: 170\n"
            + "Row #0: 164\n"
            + "Row #0: 173\n"
            + "Row #0: 163\n"
            + "Row #0: 209\n"
            + "Row #0: 136\n"
            + "Row #0: 140\n"
            + "Row #0: 185\n");
    }

    /**
     * Set defined using filter expression.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIntrinsic(Context<?> context) {
    	SystemWideProperties.instance().CaseSensitiveMdxInstr = true;
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH SET [ChardonnayChablis] AS\n"
            + "   'Filter([Product].Members, (InStr(1, [Product].CurrentMember.Name, \"chardonnay\") <> 0) OR (InStr(1, [Product].CurrentMember.Name, \"chablis\") <> 0))'\n"
            + "SELECT\n"
            + "   [ChardonnayChablis] ON COLUMNS,\n"
            + "   {Measures.[Unit Sales]} ON ROWS\n"
            + "FROM Sales",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "Axis #2:\n"
            + "{[Measures].[Unit Sales]}\n");
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH SET [BeerMilk] AS\n"
            + "   'Filter([Product].Members, (InStr(1, [Product].CurrentMember.Name, \"Beer\") <> 0) OR (InStr(1, LCase([Product].CurrentMember.Name), \"milk\") <> 0))'\n"
            + "SELECT\n"
            + "   [BeerMilk] ON COLUMNS,\n"
            + "   {Measures.[Unit Sales]} ON ROWS\n"
            + "FROM Sales",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good].[Good Imported Beer]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good].[Good Light Beer]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Pearl].[Pearl Imported Beer]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Pearl].[Pearl Light Beer]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth].[Portsmouth Imported Beer]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth].[Portsmouth Light Beer]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Top Measure].[Top Measure Imported Beer]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Top Measure].[Top Measure Light Beer]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Walrus].[Walrus Imported Beer]}\n"
            + "{[Product].[Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Walrus].[Walrus Light Beer]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Booker].[Booker 1% Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Booker].[Booker 2% Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Booker].[Booker Buttermilk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Booker].[Booker Chocolate Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Booker].[Booker Whole Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Carlson].[Carlson 1% Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Carlson].[Carlson 2% Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Carlson].[Carlson Buttermilk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Carlson].[Carlson Chocolate Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Carlson].[Carlson Whole Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Club].[Club 1% Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Club].[Club 2% Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Club].[Club Buttermilk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Club].[Club Chocolate Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Club].[Club Whole Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Even Better].[Even Better 1% Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Even Better].[Even Better 2% Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Even Better].[Even Better Buttermilk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Even Better].[Even Better Chocolate Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Even Better].[Even Better Whole Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Gorilla].[Gorilla 1% Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Gorilla].[Gorilla 2% Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Gorilla].[Gorilla Buttermilk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Gorilla].[Gorilla Chocolate Milk]}\n"
            + "{[Product].[Product].[Drink].[Dairy].[Dairy].[Milk].[Gorilla].[Gorilla Whole Milk]}\n"
            + "{[Product].[Product].[Food].[Snacks].[Candy].[Chocolate Candy].[Atomic].[Atomic Malted Milk Balls]}\n"
            + "{[Product].[Product].[Food].[Snacks].[Candy].[Chocolate Candy].[Choice].[Choice Malted Milk Balls]}\n"
            + "{[Product].[Product].[Food].[Snacks].[Candy].[Chocolate Candy].[Gulf Coast].[Gulf Coast Malted Milk Balls]}\n"
            + "{[Product].[Product].[Food].[Snacks].[Candy].[Chocolate Candy].[Musial].[Musial Malted Milk Balls]}\n"
            + "{[Product].[Product].[Food].[Snacks].[Candy].[Chocolate Candy].[Thresher].[Thresher Malted Milk Balls]}\n"
            + "Axis #2:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Row #0: 6,838\n"
            + "Row #0: 1,683\n"
            + "Row #0: 154\n"
            + "Row #0: 115\n"
            + "Row #0: 175\n"
            + "Row #0: 210\n"
            + "Row #0: 187\n"
            + "Row #0: 175\n"
            + "Row #0: 145\n"
            + "Row #0: 161\n"
            + "Row #0: 174\n"
            + "Row #0: 187\n"
            + "Row #0: 4,186\n"
            + "Row #0: 189\n"
            + "Row #0: 177\n"
            + "Row #0: 110\n"
            + "Row #0: 133\n"
            + "Row #0: 163\n"
            + "Row #0: 212\n"
            + "Row #0: 131\n"
            + "Row #0: 175\n"
            + "Row #0: 175\n"
            + "Row #0: 234\n"
            + "Row #0: 155\n"
            + "Row #0: 145\n"
            + "Row #0: 140\n"
            + "Row #0: 159\n"
            + "Row #0: 168\n"
            + "Row #0: 190\n"
            + "Row #0: 177\n"
            + "Row #0: 227\n"
            + "Row #0: 197\n"
            + "Row #0: 168\n"
            + "Row #0: 160\n"
            + "Row #0: 133\n"
            + "Row #0: 174\n"
            + "Row #0: 151\n"
            + "Row #0: 143\n"
            + "Row #0: 188\n"
            + "Row #0: 176\n"
            + "Row #0: 192\n"
            + "Row #0: 157\n"
            + "Row #0: 164\n");
    }

    /**
     * Tests a named set defined in a query which consists of tuples.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSetCrossJoin(Context<?> context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH\n"
            + "    SET [Store Types by Country]\n"
            + "AS\n"
            + "    'CROSSJOIN({[Store].[Store].[Store Country].MEMBERS},\n"
            + "               {[Store Type].[Store Type].[Store Type].MEMBERS})'\n"
            + "SELECT\n"
            + "    {[Measures].[Units Ordered]} ON COLUMNS,\n"
            + "    NON EMPTY {[Store Types by Country]} ON ROWS\n"
            + "FROM\n"
            + "    [Warehouse]\n"
            + "WHERE\n"
            + "    [Time].[Time].[1997].[Q2]",
            "Axis #0:\n"
            + "{[Time].[Time].[1997].[Q2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Units Ordered]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA], [Store Type].[Store Type].[Deluxe Supermarket]}\n"
            + "{[Store].[Store].[USA], [Store Type].[Store Type].[Mid-Size Grocery]}\n"
            + "{[Store].[Store].[USA], [Store Type].[Store Type].[Supermarket]}\n"
            + "Row #0: 16843.0\n"
            + "Row #1: 2295.0\n"
            + "Row #2: 34856.0\n");
    }

    // Disabled because fails with error '<Value> = <String> is not a function'
    // Also, don't know whether [oNormal] will correctly resolve to
    // [Store Type].[oNormal].
    @Disabled
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    public void _testXxx(Context<?> context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Store Type].[All Store Type].[oNormal] AS 'Aggregate(Filter([Customers].[Name].Members, [Customers].CurrentMember.Properties(\"Member Card\") = \"Normal\") * {[Store Type].[All Store Type]})'\n"
            + "MEMBER [Store Type].[All Store Type].[oBronze] AS 'Aggregate(Filter([Customers].[Name].Members, [Customers].CurrentMember.Properties(\"Member Card\") = \"Bronze\") * {[Store Type].[All Store Type]})'\n"
            + "MEMBER [Store Type].[All Store Type].[oGolden] AS 'Aggregate(Filter([Customers].[Name].Members, [Customers].CurrentMember.Properties(\"Member Card\") = \"Golden\") * {[Store Type].[All Store Type]})'\n"
            + "MEMBER [Store Type].[All Store Type].[oSilver] AS 'Aggregate(Filter([Customers].[Name].Members, [Customers].CurrentMember.Properties(\"Member Card\") = \"Silver\") * {[Store Type].[All Store Type]})'\n"
            + "SET CardTypes AS '{[oNormal], [oBronze], [oGolden], [oSilver]}'\n"
            + "SELECT {[Unit Sales]} ON COLUMNS, CardTypes ON ROWS\n"
            + "FROM Sales",
            "xxxx");
    }

    /**
     * Set used inside expression (Crossjoin).
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSetUsedInCrossJoin(Context<?> context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH\n"
            + "  SET [TopMedia] AS 'TopCount([Promotion Media].children, 5, [Measures].[Store Sales])' \n"
            + "SELECT {[Time].[1997].[Q1], [Time].[1997].[Q2]} ON COLUMNS,\n"
            + " {CrossJoin([TopMedia], [Product].children)} ON ROWS\n"
            + "FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Time].[Time].[1997].[Q1]}\n"
            + "{[Time].[Time].[1997].[Q2]}\n"
            + "Axis #2:\n"
            + "{[Promotion Media].[Promotion Media].[No Media], [Product].[Product].[Drink]}\n"
            + "{[Promotion Media].[Promotion Media].[No Media], [Product].[Product].[Food]}\n"
            + "{[Promotion Media].[Promotion Media].[No Media], [Product].[Product].[Non-Consumable]}\n"
            + "{[Promotion Media].[Promotion Media].[Daily Paper, Radio, TV], [Product].[Product].[Drink]}\n"
            + "{[Promotion Media].[Promotion Media].[Daily Paper, Radio, TV], [Product].[Product].[Food]}\n"
            + "{[Promotion Media].[Promotion Media].[Daily Paper, Radio, TV], [Product].[Product].[Non-Consumable]}\n"
            + "{[Promotion Media].[Promotion Media].[Daily Paper], [Product].[Product].[Drink]}\n"
            + "{[Promotion Media].[Promotion Media].[Daily Paper], [Product].[Product].[Food]}\n"
            + "{[Promotion Media].[Promotion Media].[Daily Paper], [Product].[Product].[Non-Consumable]}\n"
            + "{[Promotion Media].[Promotion Media].[Product Attachment], [Product].[Product].[Drink]}\n"
            + "{[Promotion Media].[Promotion Media].[Product Attachment], [Product].[Product].[Food]}\n"
            + "{[Promotion Media].[Promotion Media].[Product Attachment], [Product].[Product].[Non-Consumable]}\n"
            + "{[Promotion Media].[Promotion Media].[Cash Register Handout], [Product].[Product].[Drink]}\n"
            + "{[Promotion Media].[Promotion Media].[Cash Register Handout], [Product].[Product].[Food]}\n"
            + "{[Promotion Media].[Promotion Media].[Cash Register Handout], [Product].[Product].[Non-Consumable]}\n"
            + "Row #0: 3,970\n"
            + "Row #0: 4,287\n"
            + "Row #1: 32,939\n"
            + "Row #1: 33,238\n"
            + "Row #2: 8,650\n"
            + "Row #2: 9,057\n"
            + "Row #3: 142\n"
            + "Row #3: 364\n"
            + "Row #4: 975\n"
            + "Row #4: 2,523\n"
            + "Row #5: 250\n"
            + "Row #5: 603\n"
            + "Row #6: 464\n"
            + "Row #6: 66\n"
            + "Row #7: 3,173\n"
            + "Row #7: 464\n"
            + "Row #8: 862\n"
            + "Row #8: 121\n"
            + "Row #9: 171\n"
            + "Row #9: 106\n"
            + "Row #10: 1,344\n"
            + "Row #10: 814\n"
            + "Row #11: 362\n"
            + "Row #11: 165\n"
            + "Row #12: \n"
            + "Row #12: 92\n"
            + "Row #13: \n"
            + "Row #13: 933\n"
            + "Row #14: \n"
            + "Row #14: 229\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAggOnCalcMember(Context<?> context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH\n"
            + "  SET [TopMedia] AS 'TopCount([Promotion Media].children, 5, [Measures].[Store Sales])' \n"
            + "  MEMBER [Measures].[California sales for Top Media] AS 'Sum([TopMedia], ([Store].[USA].[CA], [Measures].[Store Sales]))'\n"
            + "SELECT {[Time].[1997].[Q1], [Time].[1997].[Q2]} ON COLUMNS,\n"
            + " {[Product].children} ON ROWS\n"
            + "FROM [Sales]\n"
            + "WHERE [Measures].[California sales for Top Media]",
            "Axis #0:\n"
            + "{[Measures].[California sales for Top Media]}\n"
            + "Axis #1:\n"
            + "{[Time].[Time].[1997].[Q1]}\n"
            + "{[Time].[Time].[1997].[Q2]}\n"
            + "Axis #2:\n"
            + "{[Product].[Product].[Drink]}\n"
            + "{[Product].[Product].[Food]}\n"
            + "{[Product].[Product].[Non-Consumable]}\n"
            + "Row #0: 2,725.85\n"
            + "Row #0: 2,715.56\n"
            + "Row #1: 21,200.84\n"
            + "Row #1: 23,263.72\n"
            + "Row #2: 5,598.71\n"
            + "Row #2: 6,111.74\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testContextSensitiveNamedSet(Context<?> context) {
        // For reference.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT {[Measures].[Unit Sales]} ON COLUMNS,\n"
            + "Order([Promotion Media].Children, [Measures].[Unit Sales], DESC) ON ROWS\n"
            + "FROM [Sales]\n"
            + "WHERE [Time].[1997]",

            "Axis #0:\n"
            + "{[Time].[Time].[1997]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Promotion Media].[Promotion Media].[No Media]}\n"
            + "{[Promotion Media].[Promotion Media].[Daily Paper, Radio, TV]}\n"
            + "{[Promotion Media].[Promotion Media].[Daily Paper]}\n"
            + "{[Promotion Media].[Promotion Media].[Product Attachment]}\n"
            + "{[Promotion Media].[Promotion Media].[Daily Paper, Radio]}\n"
            + "{[Promotion Media].[Promotion Media].[Cash Register Handout]}\n"
            + "{[Promotion Media].[Promotion Media].[Sunday Paper, Radio]}\n"
            + "{[Promotion Media].[Promotion Media].[Street Handout]}\n"
            + "{[Promotion Media].[Promotion Media].[Sunday Paper]}\n"
            + "{[Promotion Media].[Promotion Media].[Bulk Mail]}\n"
            + "{[Promotion Media].[Promotion Media].[In-Store Coupon]}\n"
            + "{[Promotion Media].[Promotion Media].[TV]}\n"
            + "{[Promotion Media].[Promotion Media].[Sunday Paper, Radio, TV]}\n"
            + "{[Promotion Media].[Promotion Media].[Radio]}\n"
            + "Row #0: 195,448\n"
            + "Row #1: 9,513\n"
            + "Row #2: 7,738\n"
            + "Row #3: 7,544\n"
            + "Row #4: 6,891\n"
            + "Row #5: 6,697\n"
            + "Row #6: 5,945\n"
            + "Row #7: 5,753\n"
            + "Row #8: 4,339\n"
            + "Row #9: 4,320\n"
            + "Row #10: 3,798\n"
            + "Row #11: 3,607\n"
            + "Row #12: 2,726\n"
            + "Row #13: 2,454\n");

        // For reference.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT {[Measures].[Unit Sales]} ON COLUMNS,\n"
            + "Order([Promotion Media].Children, [Measures].[Unit Sales], DESC) ON ROWS\n"
            + "FROM [Sales]\n"
            + "WHERE [Time].[1997].[Q2]",

            "Axis #0:\n"
            + "{[Time].[Time].[1997].[Q2]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Promotion Media].[Promotion Media].[No Media]}\n"
            + "{[Promotion Media].[Promotion Media].[Daily Paper, Radio, TV]}\n"
            + "{[Promotion Media].[Promotion Media].[Daily Paper, Radio]}\n"
            + "{[Promotion Media].[Promotion Media].[Sunday Paper, Radio]}\n"
            + "{[Promotion Media].[Promotion Media].[TV]}\n"
            + "{[Promotion Media].[Promotion Media].[Cash Register Handout]}\n"
            + "{[Promotion Media].[Promotion Media].[Sunday Paper, Radio, TV]}\n"
            + "{[Promotion Media].[Promotion Media].[Product Attachment]}\n"
            + "{[Promotion Media].[Promotion Media].[Sunday Paper]}\n"
            + "{[Promotion Media].[Promotion Media].[Bulk Mail]}\n"
            + "{[Promotion Media].[Promotion Media].[Daily Paper]}\n"
            + "{[Promotion Media].[Promotion Media].[Street Handout]}\n"
            + "{[Promotion Media].[Promotion Media].[Radio]}\n"
            + "{[Promotion Media].[Promotion Media].[In-Store Coupon]}\n"
            + "Row #0: 46,582\n"
            + "Row #1: 3,490\n"
            + "Row #2: 2,704\n"
            + "Row #3: 2,327\n"
            + "Row #4: 1,344\n"
            + "Row #5: 1,254\n"
            + "Row #6: 1,108\n"
            + "Row #7: 1,085\n"
            + "Row #8: 784\n"
            + "Row #9: 733\n"
            + "Row #10: 651\n"
            + "Row #11: 473\n"
            + "Row #12: 40\n"
            + "Row #13: 35\n");

        // The bottom medium in 1997 is Radio, with $2454 in sales.
        // The bottom medium in 1997.Q2 is In-Store Coupon, with $35 in sales,
        //  whereas Radio has $40 in sales in 1997.Q2.

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH\n"
            + "  SET [Bottom Media] AS 'BottomCount([Promotion Media].children, 1, [Measures].[Unit Sales])' \n"
            + "  MEMBER [Measures].[Unit Sales for Bottom Media] AS 'Sum([Bottom Media], [Measures].[Unit Sales])'\n"
            + "SELECT {[Measures].[Unit Sales for Bottom Media]} ON COLUMNS,\n"
            + " {[Time].[1997], [Time].[1997].[Q2]} ON ROWS\n"
            + "FROM [Sales]",
            // Note that Row #1 gives 40. 35 would be wrong.
            // [In-Store Coupon], which was bottom for 1997.Q2 but not for
            // 1997.
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales for Bottom Media]}\n"
            + "Axis #2:\n"
            + "{[Time].[Time].[1997]}\n"
            + "{[Time].[Time].[1997].[Q2]}\n"
            + "Row #0: 2,454\n"
            + "Row #1: 40\n");

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH\n"
            + "  SET [TopMedia] AS 'TopCount([Promotion Media].children, 3, [Measures].[Store Sales])' \n"
            + "  MEMBER [Measures].[California sales for Top Media] AS 'Sum([TopMedia], [Measures].[Store Sales])'\n"
            + "SELECT \n"
            + "  CrossJoin({[Store], [Store].[USA].[CA]},\n"
            + "    {[Time].[1997].[Q1], [Time].[1997].[Q2]}) ON COLUMNS,\n"
            + " {[Product], [Product].children} ON ROWS\n"
            + "FROM [Sales]\n"
            + "WHERE [Measures].[California sales for Top Media]",

            "Axis #0:\n"
            + "{[Measures].[California sales for Top Media]}\n"
            + "Axis #1:\n"
            + "{[Store].[Store].[All Stores], [Time].[Time].[1997].[Q1]}\n"
            + "{[Store].[Store].[All Stores], [Time].[Time].[1997].[Q2]}\n"
            + "{[Store].[Store].[USA].[CA], [Time].[Time].[1997].[Q1]}\n"
            + "{[Store].[Store].[USA].[CA], [Time].[Time].[1997].[Q2]}\n"
            + "Axis #2:\n"
            + "{[Product].[Product].[All Products]}\n"
            + "{[Product].[Product].[Drink]}\n"
            + "{[Product].[Product].[Food]}\n"
            + "{[Product].[Product].[Non-Consumable]}\n"
            + "Row #0: 108,249.52\n"
            + "Row #0: 107,649.93\n"
            + "Row #0: 29,482.53\n"
            + "Row #0: 28,953.02\n"
            + "Row #1: 8,930.95\n"
            + "Row #1: 9,551.93\n"
            + "Row #1: 2,721.23\n"
            + "Row #1: 2,444.78\n"
            + "Row #2: 78,375.66\n"
            + "Row #2: 77,219.13\n"
            + "Row #2: 21,165.50\n"
            + "Row #2: 20,924.43\n"
            + "Row #3: 20,942.91\n"
            + "Row #3: 20,878.87\n"
            + "Row #3: 5,595.80\n"
            + "Row #3: 5,583.81\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOrderedNamedSet(Context<?> context) {
        // From http://www.developersdex.com
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH SET [SET1] AS\n"
            + "'ORDER ({[Education Level].[Education Level].[Education Level].Members}, [Gender].[Gender].[All Gender].[F], ASC)'\n"
            + "MEMBER [Gender].[Gender].[RANK1] AS 'rank([Education Level].[Education Level].[Education Level].currentmember, [SET1])'\n"
            + "select\n"
            + "{[Gender].[Gender].[All Gender].[F], [Gender].[Gender].[RANK1]} on columns,\n"
            + "{[Education Level].[Education Level].[Education Level].Members} on rows\n"
            + "from Sales\n"
            + "where ([Measures].[Store Sales])",
            // MSAS gives results as below, except ranks are displayed as
            // integers, e.g. '1'.
            "Axis #0:\n"
            + "{[Measures].[Store Sales]}\n"
            + "Axis #1:\n"
            + "{[Gender].[Gender].[F]}\n"
            + "{[Gender].[Gender].[RANK1]}\n"
            + "Axis #2:\n"
            + "{[Education Level].[Education Level].[Bachelors Degree]}\n"
            + "{[Education Level].[Education Level].[Graduate Degree]}\n"
            + "{[Education Level].[Education Level].[High School Degree]}\n"
            + "{[Education Level].[Education Level].[Partial College]}\n"
            + "{[Education Level].[Education Level].[Partial High School]}\n"
            + "Row #0: 72,119.26\n"
            + "Row #0: 3\n"
            + "Row #1: 17,641.64\n"
            + "Row #1: 1\n"
            + "Row #2: 81,112.23\n"
            + "Row #2: 4\n"
            + "Row #3: 27,175.97\n"
            + "Row #3: 2\n"
            + "Row #4: 82,177.11\n"
            + "Row #4: 5\n");

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH SET [SET1] AS\n"
            + "'ORDER ({[Education Level].[Education Level].[Education Level].Members}, [Gender].[All Gender].[F], ASC)'\n"
            + "MEMBER [Gender].[Gender].[RANK1] AS 'rank([Education Level].[Education Level].[Education Level].currentmember, [SET1])'\n"
            + "select\n"
            + "{[Gender].[Gender].[All Gender].[F], [Gender].[RANK1]} on columns,\n"
            + "{[Education Level].[Education Level].[Education Level].Members} on rows\n"
            + "from Sales\n"
            + "where ([Measures].[Profit])",
            // MSAS gives results as below. The ranks are (correctly) 0
            // because profit is a calc member.
            "Axis #0:\n"
            + "{[Measures].[Profit]}\n"
            + "Axis #1:\n"
            + "{[Gender].[Gender].[F]}\n"
            + "{[Gender].[Gender].[RANK1]}\n"
            + "Axis #2:\n"
            + "{[Education Level].[Education Level].[Bachelors Degree]}\n"
            + "{[Education Level].[Education Level].[Graduate Degree]}\n"
            + "{[Education Level].[Education Level].[High School Degree]}\n"
            + "{[Education Level].[Education Level].[Partial College]}\n"
            + "{[Education Level].[Education Level].[Partial High School]}\n"
            + "Row #0: $43,382.33\n"
            + "Row #0: $0.00\n"
            + "Row #1: $10,599.59\n"
            + "Row #1: $0.00\n"
            + "Row #2: $48,766.50\n"
            + "Row #2: $0.00\n"
            + "Row #3: $16,306.05\n"
            + "Row #3: $0.00\n"
            + "Row #4: $49,394.27\n"
            + "Row #4: $0.00\n");

        // Solve order fixes the problem.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH SET [SET1] AS\n"
            + "'ORDER ({[Education Level].[Education Level].[Education Level].Members}, [Gender].[F], ASC)'\n"
            + "MEMBER [Gender].[Gender].[RANK1] AS 'rank([Education Level].[Education Level].[Education Level].currentmember, [SET1])', \n"
            + "  SOLVE_ORDER = 10\n"
            + "select\n"
            + "{[Gender].[Gender].[F], [Gender].[Gender].[RANK1]} on columns,\n"
            + "{[Education Level].[Education Level].[Education Level].Members} on rows\n"
            + "from Sales\n"
            + "where ([Measures].[Profit])",
            // MSAS gives results as below.
            "Axis #0:\n"
            + "{[Measures].[Profit]}\n"
            + "Axis #1:\n"
            + "{[Gender].[Gender].[F]}\n"
            + "{[Gender].[Gender].[RANK1]}\n"
            + "Axis #2:\n"
            + "{[Education Level].[Education Level].[Bachelors Degree]}\n"
            + "{[Education Level].[Education Level].[Graduate Degree]}\n"
            + "{[Education Level].[Education Level].[High School Degree]}\n"
            + "{[Education Level].[Education Level].[Partial College]}\n"
            + "{[Education Level].[Education Level].[Partial High School]}\n"
            + "Row #0: $43,382.33\n"
            + "Row #0: 3\n"
            + "Row #1: $10,599.59\n"
            + "Row #1: 1\n"
            + "Row #2: $48,766.50\n"
            + "Row #2: 4\n"
            + "Row #3: $16,306.05\n"
            + "Row #3: 2\n"
            + "Row #4: $49,394.27\n"
            + "Row #4: 5\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGenerate(Context<?> context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with \n"
            + "  member [Measures].[DateName] as \n"
            + "    'Generate({[Time].[1997].[Q1], [Time].[1997].[Q2]}, [Time].[Time].CurrentMember.Name) '\n"
            + "select {[Measures].[DateName]} on columns,\n"
            + " {[Time].[1997].[Q1], [Time].[1997].[Q2]} on rows\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[DateName]}\n"
            + "Axis #2:\n"
            + "{[Time].[Time].[1997].[Q1]}\n"
            + "{[Time].[Time].[1997].[Q2]}\n"
            + "Row #0: Q1Q2\n"
            + "Row #1: Q1Q2\n");

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with \n"
            + "  member [Measures].[DateName] as \n"
            + "    'Generate({[Time].[1997].[Q1], [Time].[1997].[Q2]}, [Time].[Time].CurrentMember.Name, \" and \") '\n"
            + "select {[Measures].[DateName]} on columns,\n"
            + " {[Time].[1997].[Q1], [Time].[1997].[Q2]} on rows\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[DateName]}\n"
            + "Axis #2:\n"
            + "{[Time].[Time].[1997].[Q1]}\n"
            + "{[Time].[Time].[1997].[Q2]}\n"
            + "Row #0: Q1 and Q2\n"
            + "Row #1: Q1 and Q2\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSetAgainstCube(Context<?> context) {

        withSchema(context,
                NamedSetsInCubeModifier::new);
        // Set defined against cube, using 'formula' attribute.
        Connection connection = context.getConnectionWithDefaultRole();
        assertQueryReturns(connection,
            "SELECT {[Measures].[Unit Sales]} ON COLUMNS,\n"
            + " {[CA Cities]} ON ROWS\n"
            + "FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[CA].[Alameda]}\n"
            + "{[Store].[Store].[USA].[CA].[Beverly Hills]}\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[Store].[USA].[CA].[San Diego]}\n"
            + "{[Store].[Store].[USA].[CA].[San Francisco]}\n"
            + "Row #0: \n"
            + "Row #1: 21,333\n"
            + "Row #2: 25,663\n"
            + "Row #3: 25,635\n"
            + "Row #4: 2,117\n");

        // Set defined against cube, in terms of another set, and using
        // '<Formula>' element.
        assertQueryReturns(connection,
            "SELECT {[Measures].[Unit Sales]} ON COLUMNS,\n"
            + " {[Top CA Cities]} ON ROWS\n"
            + "FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[Store].[USA].[CA].[San Diego]}\n"
            + "Row #0: 25,663\n"
            + "Row #1: 25,635\n");

        // Override named set in query.
        assertQueryReturns(connection,
            "WITH SET [CA Cities] AS '{[Store].[USA].[OR].[Portland]}' "
            + "SELECT {[Measures].[Unit Sales]} ON COLUMNS,\n"
            + " {[CA Cities]} ON ROWS\n"
            + "FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[OR].[Portland]}\n"
            + "Row #0: 26,079\n");

        // When [CA Cities] is overridden, does the named set [Top CA Cities],
        // which is derived from it, use the new definition? No. It stays
        // bound to the original definition.
        assertQueryReturns(connection,
            "WITH SET [CA Cities] AS '{[Store].[USA].[OR].[Portland]}' "
            + "SELECT {[Measures].[Unit Sales]} ON COLUMNS,\n"
            + " {[Top CA Cities]} ON ROWS\n"
            + "FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles]}\n"
            + "{[Store].[Store].[USA].[CA].[San Diego]}\n"
            + "Row #0: 25,663\n"
            + "Row #1: 25,635\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSetAgainstSchema(Context<?> context) {
    	Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        withSchema(context,
        		NamedSetTest.NamedSetsInCubeAndSchemaModifier::new);
        Connection connection = context.getConnectionWithDefaultRole();
        assertQueryReturns(connection,
            "SELECT {[Measures].[Store Sales]} on columns,\n"
            + " Intersect([Top CA Cities], [Top USA Stores]) on rows\n"
            + "FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Store Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Store].[USA].[CA].[Los Angeles]}\n"
            + "Row #0: 54,545.28\n");
        // Use non-existent set.
        assertQueryThrows(connection,
            "SELECT {[Measures].[Store Sales]} on columns,\n"
            + " Intersect([Top CA Cities], [Top Ukrainian Cities]) on rows\n"
            + "FROM [Sales]",
            "MDX object '[Top Ukrainian Cities]' not found in cube 'Sales'");
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBadNamedSet(Context<?> context) {
        class TestBadNamedSetModifier extends PojoMappingModifier {

            public TestBadNamedSetModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends NamedSetMapping> catalogNamedSets(CatalogMapping schema) {
                List<NamedSetMapping> result = new ArrayList<>();
                result.addAll(super.catalogNamedSets(schema));
                result.add(NamedSetMappingImpl.builder()
                    .withName("Bad")
                    .withFormula("{[Store].[USA].[WA].Children}}")
                    .build());
                return result;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            null,
            null,
            "<NamedSet name=\"Bad\" formula=\"{[Store].[USA].[WA].Children}}\"/>",
            null,
            null);
        withSchema(context, schema);
         */
        withSchema(context, TestBadNamedSetModifier::new);
        assertQueryThrows(context,
            "SELECT {[Measures].[Store Sales]} on columns,\n"
            + " {[Bad]} on rows\n"
            + "FROM [Sales]", "Named set 'Bad' has bad formula");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSetMustBeSet(Context<?> context) {
        Result result;
        String queryString;
        String pattern;
        Connection connection = context.getConnectionWithDefaultRole();
        // Formula for a named set must not be a member.
        queryString =
            "with set [Foo] as ' [Store].CurrentMember  '"
            + "select {[Foo]} on columns from [Sales]";
        pattern = "Set expression '[Foo]' must be a set";
        assertQueryThrows(connection, queryString, pattern);

        // Formula for a named set must not be a dimension.
        queryString =
            "with set [Foo] as ' [Store] '"
            + "select {[Foo]} on columns from [Sales]";
        assertQueryThrows(connection, queryString, pattern);

        // Formula for a named set must not be a level.
        queryString =
            "with set [Foo] as ' [Store].[Store Country] '"
            + "select {[Foo]} on columns from [Sales]";
        assertQueryThrows(connection, queryString, pattern);

        // Formula for a named set must not be a cube name.
        queryString =
            "with set [Foo] as ' [Sales] '"
            + "select {[Foo]} on columns from [Sales]";
        assertQueryThrows(connection,
            queryString,
            "MDX object '[Sales]' not found in cube 'Sales'");

        // Formula for a named set must not be a string.
        queryString =
            "with set [Foo] as ' \"foobar\" '"
            + "select {[Foo]} on columns from [Sales]";
        assertQueryThrows(connection, queryString, pattern);

        // Formula for a named set must not be a number.
        queryString =
            "with set [Foo] as ' -1.45 '"
            + "select {[Foo]} on columns from [Sales]";
        assertQueryThrows(connection, queryString, pattern);

        // Formula for a named set must not be a tuple.
        queryString =
            "with set [Foo] as ' ([Gender].[M], [Marital Status].[S]) '"
            + "select {[Foo]} on columns from [Sales]";
        assertQueryThrows(connection, queryString, pattern);

        // Formula for a named set may be a set of tuples.
        queryString =
            "with set [Foo] as ' CrossJoin([Gender].members, [Marital Status].members) '"
            + "select {[Foo]} on columns from [Sales]";
        result = executeQuery(connection, queryString);
//        discard(result);

        // Formula for a named set may be a set of members.
        queryString =
            "with set [Foo] as ' [Gender].members '"
            + "select {[Foo]} on columns from [Sales]";
        result = executeQuery(connection, queryString);
//        discard(result);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSetsMixedWithCalcMembers(Context<?> context)
    {

        withSchema(context,
                MixedNamedSetSchemaModifier::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {\n"
            + "    [Measures].[Unit Sales],\n"
            + "    [Measures].[CA City Sales]} on columns,\n"
            + "  Crossjoin(\n"
            + "    [Time].[1997].Children,\n"
            + "    [Top Products In CA]) on rows\n"
            + "from [Sales]\n"
            + "where [Marital Status].[S]",
            "Axis #0:\n"
            + "{[Marital Status].[Marital Status].[S]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[CA City Sales]}\n"
            + "Axis #2:\n"
            + "{[Time].[Time].[1997].[Q1], [Product].[Product].[Food].[Produce]}\n"
            + "{[Time].[Time].[1997].[Q1], [Product].[Product].[Food].[Snack Foods]}\n"
            + "{[Time].[Time].[1997].[Q1], [Product].[Product].[Non-Consumable].[Household]}\n"
            + "{[Time].[Time].[1997].[Q2], [Product].[Product].[Food].[Produce]}\n"
            + "{[Time].[Time].[1997].[Q2], [Product].[Product].[Food].[Snack Foods]}\n"
            + "{[Time].[Time].[1997].[Q2], [Product].[Product].[Non-Consumable].[Household]}\n"
            + "{[Time].[Time].[1997].[Q3], [Product].[Product].[Food].[Produce]}\n"
            + "{[Time].[Time].[1997].[Q3], [Product].[Product].[Food].[Snack Foods]}\n"
            + "{[Time].[Time].[1997].[Q3], [Product].[Product].[Non-Consumable].[Household]}\n"
            + "{[Time].[Time].[1997].[Q4], [Product].[Product].[Food].[Produce]}\n"
            + "{[Time].[Time].[1997].[Q4], [Product].[Product].[Food].[Snack Foods]}\n"
            + "{[Time].[Time].[1997].[Q4], [Product].[Product].[Non-Consumable].[Household]}\n"
            + "Row #0: 4,872\n"
            + "Row #0: $1,218.0\n"
            + "Row #1: 3,746\n"
            + "Row #1: $840.0\n"
            + "Row #2: 3,425\n"
            + "Row #2: $817.0\n"
            + "Row #3: 4,633\n"
            + "Row #3: $1,320.0\n"
            + "Row #4: 3,588\n"
            + "Row #4: $1,058.0\n"
            + "Row #5: 3,149\n"
            + "Row #5: $938.0\n"
            + "Row #6: 4,651\n"
            + "Row #6: $1,353.0\n"
            + "Row #7: 3,895\n"
            + "Row #7: $1,134.0\n"
            + "Row #8: 3,395\n"
            + "Row #8: $1,029.0\n"
            + "Row #9: 5,160\n"
            + "Row #9: $1,550.0\n"
            + "Row #10: 4,160\n"
            + "Row #10: $1,301.0\n"
            + "Row #11: 3,808\n"
            + "Row #11: $1,166.0\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSetAndUnion(Context<?> context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with set [Set Education Level] as\n"
            + "   '{([Education Level].[All Education Levels].[Bachelors Degree]),\n"
            + "     ([Education Level].[All Education Levels].[Graduate Degree])}'\n"
            + "select\n"
            + "   {[Measures].[Unit Sales],\n"
            + "    [Measures].[Store Cost],\n"
            + "    [Measures].[Store Sales]} ON COLUMNS,\n"
            + "   UNION(\n"
            + "      CROSSJOIN(\n"
            + "         {[Time].[1997].[Q1]},\n"
            + "          [Set Education Level]), \n"
            + "      {([Time].[1997].[Q1],\n"
            + "        [Education Level].[All Education Levels].[Graduate Degree])}) ON ROWS\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Store Cost]}\n"
            + "{[Measures].[Store Sales]}\n"
            + "Axis #2:\n"
            + "{[Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Bachelors Degree]}\n"
            + "{[Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Graduate Degree]}\n"
            + "Row #0: 17,066\n"
            + "Row #0: 14,234.10\n"
            + "Row #0: 35,699.43\n"
            + "Row #1: 3,637\n"
            + "Row #1: 3,030.82\n"
            + "Row #1: 7,583.71\n");
    }

    /**
     * Tests that named sets never depend on anything.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSetDependencies(Context<?> context) {
        withSchema(context,
                NamedSetsInCubeModifier::new);
        assertSetExprDependsOn(context.getConnectionWithDefaultRole(), "[Top CA Cities]", "{}");
    }

    /**
     * Test csae for bug 1971080, "hierarchize(named set) causes attempt to
     * sort immutable list".
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHierarchizeNamedSetImmutable(Context<?> context) {
        Connection connection = context.getConnectionWithDefaultRole();
        flushSchemaCache(connection);
        assertQueryReturns(connection,
            "with set necj as\n"
            + "NonEmptyCrossJoin([Customers].[Name].members,[Store].[Store Name].members)\n"
            + "select\n"
            + "{[Measures].[Unit Sales]} on columns,\n"
            + "Tail(hierarchize(necj),5) on rows\n"
            + "from sales",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Customers].[Customers].[USA].[WA].[Yakima].[Tracy Meyer], [Store].[Store].[USA].[WA].[Yakima].[Store 23]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Yakima].[Vanessa Thompson], [Store].[Store].[USA].[WA].[Yakima].[Store 23]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Yakima].[Velma Lykes], [Store].[Store].[USA].[WA].[Yakima].[Store 23]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Yakima].[William Battaglia], [Store].[Store].[USA].[WA].[Yakima].[Store 23]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Yakima].[Wilma Fink], [Store].[Store].[USA].[WA].[Yakima].[Store 23]}\n"
            + "Row #0: 44\n"
            + "Row #1: 128\n"
            + "Row #2: 55\n"
            + "Row #3: 149\n"
            + "Row #4: 89\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCurrentAndCurrentOrdinal(Context<?> context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with set [Gender Marital Status] as\n"
            + " [Gender].members * [Marital Status].members\n"
            + "member [Measures].[GMS Ordinal] as\n"
            + " [Gender Marital Status].CurrentOrdinal\n"
            + "member [Measures].[GMS Name]\n"
            + " as TupleToStr([Gender Marital Status].Current)\n"
            + "select {\n"
            + "  [Measures].[Unit Sales],\n"
            + "  [Measures].[GMS Ordinal],\n"
            + "  [Measures].[GMS Name]} on 0,\n"
            + " {[Gender Marital Status]} on 1\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[GMS Ordinal]}\n"
            + "{[Measures].[GMS Name]}\n"
            + "Axis #2:\n"
            + "{[Gender].[Gender].[All Gender], [Marital Status].[Marital Status].[All Marital Status]}\n"
            + "{[Gender].[Gender].[All Gender], [Marital Status].[Marital Status].[M]}\n"
            + "{[Gender].[Gender].[All Gender], [Marital Status].[Marital Status].[S]}\n"
            + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[All Marital Status]}\n"
            + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
            + "{[Gender].[Gender].[F], [Marital Status].[Marital Status].[S]}\n"
            + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[All Marital Status]}\n"
            + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[M]}\n"
            + "{[Gender].[Gender].[M], [Marital Status].[Marital Status].[S]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: 0\n"
            + "Row #0: ([Gender].[Gender].[All Gender], [Marital Status].[Marital Status].[All Marital Status])\n"
            + "Row #1: 131,796\n"
            + "Row #1: 1\n"
            + "Row #1: ([Gender].[Gender].[All Gender], [Marital Status].[Marital Status].[M])\n"
            + "Row #2: 134,977\n"
            + "Row #2: 2\n"
            + "Row #2: ([Gender].[Gender].[All Gender], [Marital Status].[Marital Status].[S])\n"
            + "Row #3: 131,558\n"
            + "Row #3: 3\n"
            + "Row #3: ([Gender].[Gender].[F], [Marital Status].[Marital Status].[All Marital Status])\n"
            + "Row #4: 65,336\n"
            + "Row #4: 4\n"
            + "Row #4: ([Gender].[Gender].[F], [Marital Status].[Marital Status].[M])\n"
            + "Row #5: 66,222\n"
            + "Row #5: 5\n"
            + "Row #5: ([Gender].[Gender].[F], [Marital Status].[Marital Status].[S])\n"
            + "Row #6: 135,215\n"
            + "Row #6: 6\n"
            + "Row #6: ([Gender].[Gender].[M], [Marital Status].[Marital Status].[All Marital Status])\n"
            + "Row #7: 66,460\n"
            + "Row #7: 7\n"
            + "Row #7: ([Gender].[Gender].[M], [Marital Status].[Marital Status].[M])\n"
            + "Row #8: 68,755\n"
            + "Row #8: 8\n"
            + "Row #8: ([Gender].[Gender].[M], [Marital Status].[Marital Status].[S])\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSetWithCompoundSlicer(Context<?> context) {
        // MONDRIAN-1654
        final String mdx = "with set [FilteredNamedSet] as "
            + "'Filter([Customers].[Name].Members, "
            + "measures.[Unit Sales] > 200)' select FilteredNamedSet on 0 from "
            + "sales where {Time.[1997].Q1, TIme.[1997].Q2}";
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{[Time].[Time].[1997].[Q1]}\n"
            + "{[Time].[Time].[1997].[Q2]}\n"
            + "Axis #1:\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Daniel Thompson]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Dauna Barton]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Emily Barela]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Grace McLaughlin]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Joann Mramor]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Mary Francis Benigar]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Matt Bellah]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Wildon Cameron]}\n"
            + "Row #0: 202\n"
            + "Row #0: 218\n"
            + "Row #0: 215\n"
            + "Row #0: 228\n"
            + "Row #0: 227\n"
            + "Row #0: 257\n"
            + "Row #0: 258\n"
            + "Row #0: 227\n");
        verifySameNativeAndNot(context.getConnectionWithDefaultRole(), mdx, "");
    }

    /**
     * Test case for issue on developers list which involves a named set and a
     * range in the WHERE clause. Current Mondrian behavior appears to be
     * correct.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSetRangeInSlicer(Context<?> context) {
        String expected =
            "Axis #0:\n"
            + "{[Time].[Time].[1997].[Q1].[1]}\n"
            + "{[Time].[Time].[1997].[Q1].[2]}\n"
            + "{[Time].[Time].[1997].[Q1].[3]}\n"
            + "{[Time].[Time].[1997].[Q2].[4]}\n"
            + "{[Time].[Time].[1997].[Q2].[5]}\n"
            + "{[Time].[Time].[1997].[Q2].[6]}\n"
            + "{[Time].[Time].[1997].[Q3].[7]}\n"
            + "{[Time].[Time].[1997].[Q3].[8]}\n"
            + "{[Time].[Time].[1997].[Q3].[9]}\n"
            + "{[Time].[Time].[1997].[Q4].[10]}\n"
            + "Axis #1:\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Mary Francis Benigar], [Measures].[Unit Sales]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[James Horvat], [Measures].[Unit Sales]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Matt Bellah], [Measures].[Unit Sales]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Ida Rodriguez], [Measures].[Unit Sales]}\n"
            + "{[Customers].[Customers].[USA].[WA].[Spokane].[Kristin Miller], [Measures].[Unit Sales]}\n"
            + "Row #0: 422\n"
            + "Row #0: 369\n"
            + "Row #0: 363\n"
            + "Row #0: 344\n"
            + "Row #0: 323\n";
        Connection connection = context.getConnectionWithDefaultRole();
        assertQueryReturns(connection,
            "SELECT\n"
            + "NON EMPTY TopCount([Customers].[Name].Members, 5, [Measures].[Unit Sales]) * [Measures].[Unit Sales] on 0\n"
            + "FROM [Sales]\n"
            + "WHERE [Time].[1997].[Q1].[1]:[Time].[1997].[Q4].[10]",
            expected);
        // as above, but remove NON EMPTY
        assertQueryReturns(connection,
            "SELECT\n"
            + "TopCount([Customers].[Name].Members, 5, [Measures].[Unit Sales]) * [Measures].[Unit Sales] on 0\n"
            + "FROM [Sales]\n"
            + "WHERE [Time].[1997].[Q1].[1]:[Time].[1997].[Q4].[10]",
            expected);
        // as above, but with DISTINCT
        assertQueryReturns(connection,
            "SELECT\n"
            + "TopCount(Distinct([Customers].[Name].Members), 5, [Measures].[Unit Sales]) * [Measures].[Unit Sales] on 0\n"
            + "FROM [Sales]\n"
            + "WHERE [Time].[1997].[Q1].[1]:[Time].[1997].[Q4].[10]",
            expected);
        // As above, but convert TopCount expression to a named set. Named
        // sets are evaluated after the slicer but before any axes. I.e. not
        // in the context of any particular position on ROWS or COLUMNS, nor
        // inheriting the NON EMPTY constraint on the axis.
        assertQueryReturns(connection,
            "WITH SET [Top Count] AS\n"
            + "  TopCount([Customers].[Name].Members, 5, [Measures].[Unit Sales])\n"
            + "SELECT [Top Count] * [Measures].[Unit Sales] on 0\n"
            + "FROM [Sales]\n"
            + "WHERE [Time].[1997].[Q1].[1]:[Time].[1997].[Q4].[10]",
            expected);
        // as above, but with DISTINCT
        if (false)
        assertQueryReturns(connection,
            "WITH SET [Top Count] AS\n"
            + "{\n"
            + "  TopCount(\n"
            + "    Distinct([Customers].[Name].Members),\n"
            + "    5,\n"
            + "    [Measures].[Unit Sales])\n"
            + "}\n"
            + "SELECT [Top Count] * [Measures].[Unit Sales] on 0\n"
            + "FROM [Sales]\n"
            + "WHERE [Time].[1997].[Q1].[1]:[Time].[1997].[Q4].[10]",
            expected);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMondrian2424(Context<?> context) {

        //SystemWideProperties.instance().SsasCompatibleNaming = false;
        assertQueryReturns(context.getConnectionWithDefaultRole(),
                "WITH SET Gender1 as '[Gender].[Gender].[Gender].members' \n" +
                        "select {Gender1} ON 0 from [Sales]",
                "Axis #0:\n" +
                        "{}\n" +
                        "Axis #1:\n" +
                        "{[Gender].[Gender].[F]}\n" +
                        "{[Gender].[Gender].[M]}\n" +
                        "Row #0: 131,558\n" +
                        "Row #0: 135,215\n"
        );
    }

    /**
     * Variant of {@link #testNamedSetRangeInSlicer()} that calls
     * {@link CompoundSlicerTest#testBugMondrian899()} to
     * prime the cache and therefore fails even when run standalone.
     *
     * <p>Test case for <a href="http://jira.pentaho.com/browse/MONDRIAN-1203">
     * MONDRIAN-1203, "Error 'Failed to load all aggregations after 10 passes'
     * while evaluating composite slicer"</a>.</p>
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNamedSetRangeInSlicerPrimed(Context<?> context) {
        new CompoundSlicerTest().testBugMondrian899(context);
        testNamedSetRangeInSlicer(context);
    }

    /**
     * Dynamic schema processor which adds two named sets to a the first cube
     * in a schema.
     */
    public static class NamedSetsInCubeModifier extends PojoMappingModifier {

        public NamedSetsInCubeModifier(CatalogMapping catalogMapping) {
            super(catalogMapping);
        }

        @Override
        protected List<? extends NamedSetMapping> catalogNamedSets(CatalogMapping schema) {
            List<NamedSetMapping> result = new ArrayList<>();
            result.addAll(super.catalogNamedSets(schema));
            result.add(NamedSetMappingImpl.builder()
                    .withName("CA Cities")
                    .withFormula("{[Store].[USA].[CA].Children}")
                    .build());
                result.add(NamedSetMappingImpl.builder()
                    .withName("Top CA Cities")
                    .withFormula("TopCount([CA Cities], 2, [Measures].[Unit Sales])")
                    .build());
                return result;
        }
    }




    public static class NamedSetsInCubeAndSchemaModifier extends PojoMappingModifier {

        public NamedSetsInCubeAndSchemaModifier(CatalogMapping catalogMapping) {
            super(catalogMapping);
        }

        @Override
        protected List<? extends NamedSetMapping> catalogNamedSets(CatalogMapping schema) {
            List<NamedSetMapping> result = new ArrayList<>();
            result.addAll(super.catalogNamedSets(schema));
            result.add(NamedSetMappingImpl.builder()
                    .withName("CA Cities")
                    .withFormula("{[Store].[USA].[CA].Children}")
                    .build());
                result.add(NamedSetMappingImpl.builder()
                    .withName("Top CA Cities")
                    .withFormula("TopCount([CA Cities], 2, [Measures].[Unit Sales])")
                    .build());
                result.add(NamedSetMappingImpl.builder()
                        .withName("Top USA Stores")
                        .withFormula("TopCount(Descendants([Store].[USA]), 7)")
                        .build());
                return result;
        }
    }





    public static class MixedNamedSetSchemaModifier extends PojoMappingModifier {

        public MixedNamedSetSchemaModifier(CatalogMapping catalogMapping) {
            super(catalogMapping);
        }

        @Override
        protected List<? extends NamedSetMapping> cubeNamedSets(CubeMapping cube) {
            List<NamedSetMapping> result = new ArrayList<>();
            result.addAll(super.cubeNamedSets(cube));
            if ("Sales".equals(cube.getName())) {
                result.add(NamedSetMappingImpl.builder()
                    .withName("Top Products In CA")
                    .withFormula("TopCount([Product].[Product Department].MEMBERS, 3, ([Time].[1997].[Q3], [Measures].[CA City Sales]))")
                    .build());
                result.add(NamedSetMappingImpl.builder()
                    .withName("CA Cities")
                    .withFormula("{[Store].[USA].[CA].Children}")
                    .build());
            }
            return result;
        }

        protected List<? extends CalculatedMemberMapping> cubeCalculatedMembers(CubeMapping cube) {
            List<CalculatedMemberMapping> result = new ArrayList<>();
            result.addAll(super.cubeCalculatedMembers(cube));
            if ("Sales".equals(cube.getName())) {
                result.add(CalculatedMemberMappingImpl.builder()
                    .withName("CA City Sales")
                    //.withDimension("Measures")
                    .withVisible(false)
                    .withFormula("Aggregate([CA Cities], [Measures].[Unit Sales])")
                    .withCalculatedMemberProperties(List.of(
                    	CalculatedMemberPropertyMappingImpl.builder()
                            .withName("FORMAT_STRING")
                            .withValue("$#,##0.0")
                            .build()
                    ))
                    .build());
            }
            return result;
        }
    }



}
