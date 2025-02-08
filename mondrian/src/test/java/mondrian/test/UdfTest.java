/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara
// All Rights Reserved.
*/

package mondrian.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertExprReturns;
import static org.opencube.junit5.TestUtil.assertMatchesVerbose;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertQueryThrows;
import static org.opencube.junit5.TestUtil.executeExpr;
import static org.opencube.junit5.TestUtil.executeExprRaw;
import static org.opencube.junit5.TestUtil.executeOlap4jQuery;
import static org.opencube.junit5.TestUtil.executeQuery;
import static org.opencube.junit5.TestUtil.withSchema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.Syntax;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.MemberFormatter;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.CellSet;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.api.type.HierarchyType;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.StringType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.impl.StatementImpl;
import org.eclipse.daanse.olap.rolap.api.RolapContext;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.Property;
import mondrian.olap.SystemWideProperties;
import mondrian.olap.Util;
import mondrian.rolap.RolapCatalogCache;
import mondrian.rolap.SchemaModifiers;
import mondrian.spi.CellFormatter;
import mondrian.spi.PropertyFormatter;
import mondrian.spi.UserDefinedFunction;

/**
 * Unit-test for {@link UserDefinedFunction user-defined functions}.
 * Also tests {@link CellFormatter cell formatters}
 * and {@link MemberFormatter member formatters}.
 *
 * <p>TODO:
 * 1. test that function which does not return a name, description etc.
 *    gets a sensible error
 * 2. document UDFs
 *
 * @author jhyde
 * @since Apr 29, 2005
 */
public class UdfTest {



    private void prepareContext(Context context) {
        updateTestContext(context, SchemaModifiers.UdfTestModifier11::new);
    }

    /**
     * Shorthand for containing a test context that consists of the standard
     * FoodMart schema plus a UDF.
     *
     * @param xmlUdf UDF definition
     * @return Test context
     */
    /*
    private void udfTestContext(Context context, String xmlUdf) {
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null, null, null, null, xmlUdf, null);
        TestUtil.withSchema(context, schema);
    }
     */

    /**
     * Shorthand for containing a test context that consists of the standard
     * FoodMart Sales cube plus one measure.
     *
     * @param xmlMeasure Measure definition
     * @return Test context
     */
    private void updateTestContext(Context context, Function<CatalogMapping, PojoMappingModifier> f) {
        withSchema(context, f);
    }

    private void updateTestContext(Context context, PojoMappingModifier m) {
        context.getCatalogCache().clear();
        ((TestContext)context).setCatalogMappingSupplier(m);
    }


    // ~ Tests follow ----------------------------------------------------------

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSanity(Context context) {
        // sanity check, make sure the schema is loading correctly
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT {[Measures].[Store Sqft]} ON COLUMNS, {[Store Type]} ON ROWS FROM [Store]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Store Sqft]}\n"
            + "Axis #2:\n"
            + "{[Store Type].[All Store Types]}\n"
            + "Row #0: 571,596\n");
    }

    @Disabled //TODO: UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFun(Context context) {
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Measures].[Sqft Plus One] AS 'PlusOne([Measures].[Store Sqft])'\n"
            + "SELECT {[Measures].[Sqft Plus One]} ON COLUMNS, \n"
            + "  {[Store Type].children} ON ROWS \n"
            + "FROM [Store]",

            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Sqft Plus One]}\n"
            + "Axis #2:\n"
            + "{[Store Type].[Deluxe Supermarket]}\n"
            + "{[Store Type].[Gourmet Supermarket]}\n"
            + "{[Store Type].[HeadQuarters]}\n"
            + "{[Store Type].[Mid-Size Grocery]}\n"
            + "{[Store Type].[Small Grocery]}\n"
            + "{[Store Type].[Supermarket]}\n"
            + "Row #0: 146,046\n"
            + "Row #1: 47,448\n"
            + "Row #2: \n"
            + "Row #3: 109,344\n"
            + "Row #4: 75,282\n"
            + "Row #5: 193,481\n");
    }

    /**
     * Test case for bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-1200">MONDRIAN-1200,
     * "User-defined function + profiling causes NPE in CalcWriter"</a>.
     * The bug only occurs if manually enable "mondrian.profile" logger before
     * running this test. (The bug requires olap4j, plus profiling, plus a
     * query that calls a UDF with one or more arguments on an axis.)
     *
     * @throws SQLException on error
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFunWithProfiling(Context context) throws SQLException {
        prepareContext(context);
        Connection connection = null;
        Statement statement = null;
        CellSet x = null;
        try {
            connection = context.getConnectionWithDefaultRole();
            statement = connection.createStatement();
            x = statement.executeQuery(
                "SELECT { CurrentDateMember([Time].[Time], "
                + "\"[Ti\\me]\\.[yyyy]\\.[Qq]\\.[m]\", BEFORE)} "
                + "ON COLUMNS FROM [Sales]");
            TestUtil.toString(x);
        } finally {
            Util.close(x, ((StatementImpl) statement), connection);
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLastNonEmpty(Context context) {
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Measures].[Last Unit Sales] AS \n"
            + " '([Measures].[Unit Sales], \n"
            + "   LastNonEmpty(Descendants([Time].[Time]), [Measures].[Unit Sales]))'\n"
            + "SELECT {[Measures].[Last Unit Sales]} ON COLUMNS,\n"
            + " CrossJoin(\n"
            + "  {[Time].[1997], [Time].[1997].[Q1], [Time].[1997].[Q1].Children},\n"
            + "  {[Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].children}) ON ROWS\n"
            + "FROM [Sales]\n"
            + "WHERE ([Store].[All Stores].[USA].[OR].[Portland].[Store 11])",
            "Axis #0:\n"
            + "{[Store].[USA].[OR].[Portland].[Store 11]}\n"
            + "Axis #1:\n"
            + "{[Measures].[Last Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Time].[1997], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]}\n"
            + "{[Time].[1997], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Pearl]}\n"
            + "{[Time].[1997], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth]}\n"
            + "{[Time].[1997], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Top Measure]}\n"
            + "{[Time].[1997], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Walrus]}\n"
            + "{[Time].[1997].[Q1], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]}\n"
            + "{[Time].[1997].[Q1], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Pearl]}\n"
            + "{[Time].[1997].[Q1], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth]}\n"
            + "{[Time].[1997].[Q1], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Top Measure]}\n"
            + "{[Time].[1997].[Q1], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Walrus]}\n"
            + "{[Time].[1997].[Q1].[1], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]}\n"
            + "{[Time].[1997].[Q1].[1], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Pearl]}\n"
            + "{[Time].[1997].[Q1].[1], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth]}\n"
            + "{[Time].[1997].[Q1].[1], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Top Measure]}\n"
            + "{[Time].[1997].[Q1].[1], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Walrus]}\n"
            + "{[Time].[1997].[Q1].[2], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]}\n"
            + "{[Time].[1997].[Q1].[2], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Pearl]}\n"
            + "{[Time].[1997].[Q1].[2], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth]}\n"
            + "{[Time].[1997].[Q1].[2], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Top Measure]}\n"
            + "{[Time].[1997].[Q1].[2], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Walrus]}\n"
            + "{[Time].[1997].[Q1].[3], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Good]}\n"
            + "{[Time].[1997].[Q1].[3], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Pearl]}\n"
            + "{[Time].[1997].[Q1].[3], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth]}\n"
            + "{[Time].[1997].[Q1].[3], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Top Measure]}\n"
            + "{[Time].[1997].[Q1].[3], [Product].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Walrus]}\n"
            + "Row #0: 2\n"
            + "Row #1: 7\n"
            + "Row #2: 6\n"
            + "Row #3: 7\n"
            + "Row #4: 4\n"
            + "Row #5: 3\n"
            + "Row #6: 4\n"
            + "Row #7: 3\n"
            + "Row #8: 4\n"
            + "Row #9: 2\n"
            + "Row #10: \n"
            + "Row #11: 4\n"
            + "Row #12: \n"
            + "Row #13: 2\n"
            + "Row #14: \n"
            + "Row #15: \n"
            + "Row #16: 2\n"
            + "Row #17: \n"
            + "Row #18: 4\n"
            + "Row #19: \n"
            + "Row #20: 3\n"
            + "Row #21: 4\n"
            + "Row #22: 3\n"
            + "Row #23: 4\n"
            + "Row #24: 2\n");
    }

    /**
     * Tests a performance issue with LastNonEmpty (bug 1533677). The naive
     * implementation of LastNonEmpty crawls backward one period at a time,
     * generates a cache miss, and the next iteration reads precisely one cell.
     * So the query soon exceeds the MaxEvalDepth
     * property.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLastNonEmptyBig(Context context) {
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with\n"
            + "     member\n"
            + "     [Measures].[Last Sale] as ([Measures].[Unit Sales],\n"
            + "         LastNonEmpty(Descendants([Time].[Time].CurrentMember, [Time].[Month]),\n"
            + "         [Measures].[Unit Sales]))\n"
            + "select\n"
            + "     NON EMPTY {[Measures].[Last Sale]} ON columns,\n"
            + "     NON EMPTY Order([Store].[All Stores].Children,\n"
            + "         [Measures].[Last Sale], DESC) ON rows\n"
            + "from [Sales]\n"
            + "where [Time].[Time].LastSibling",
            "Axis #0:\n"
            + "{[Time].[1998]}\n"
            + "Axis #1:\n"
            + "Axis #2:\n");
    }

    @Disabled //TODO: UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBadFun(Context context) {
        updateTestContext(context, SchemaModifiers.UdfTestModifier12::new);
        /*
        udfTestContext(context,
            "<UserDefinedFunction name=\"BadPlusOne\" className=\""
            + BadPlusOneUdf.class.getName()
            + "\"/>\n");
         */
        try {
            executeQuery(context.getConnectionWithDefaultRole(), "SELECT {} ON COLUMNS FROM [Sales]");
            fail("Expected exception");
        } catch (Exception e) {
            final String s = e.getMessage();
            assertEquals(
                "Mondrian Error:Internal error: Invalid "
                + "user-defined function 'BadPlusOne': return type is null", s);
        }
    }

    @Disabled //TODO: UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGenericFun(Context context) {
        updateTestContext(context, SchemaModifiers.UdfTestModifier14::new);
        /*
        udfTestContext(context,
            "<UserDefinedFunction name=\"GenericPlusOne\" className=\""
            + PlusOrMinusOneUdf.class.getName()
            + "\"/>\n"
            + "<UserDefinedFunction name=\"GenericMinusOne\" className=\""
            + PlusOrMinusOneUdf.class.getName()
            + "\"/>\n");
         */
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales", "GenericPlusOne(3)", "4");
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales", "GenericMinusOne(3)", "2");
    }

    @Disabled //TODO: UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testComplexFun(Context context) {
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Measures].[InverseNormal] AS 'InverseNormal([Measures].[Grocery Sqft] / [Measures].[Store Sqft])', FORMAT_STRING = \"0.000\"\n"
            + "SELECT {[Measures].[InverseNormal]} ON COLUMNS, \n"
            + "  {[Store Type].children} ON ROWS \n"
            + "FROM [Store]",

            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[InverseNormal]}\n"
            + "Axis #2:\n"
            + "{[Store Type].[Deluxe Supermarket]}\n"
            + "{[Store Type].[Gourmet Supermarket]}\n"
            + "{[Store Type].[HeadQuarters]}\n"
            + "{[Store Type].[Mid-Size Grocery]}\n"
            + "{[Store Type].[Small Grocery]}\n"
            + "{[Store Type].[Supermarket]}\n"
            + "Row #0: 0.467\n"
            + "Row #1: 0.463\n"
            + "Row #2: \n"
            + "Row #3: 0.625\n"
            + "Row #4: 0.521\n"
            + "Row #5: 0.504\n");
    }

    @Disabled //TODO: UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testException(Context context) {
        prepareContext(context);
        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Measures].[InverseNormal] "
            + " AS 'InverseNormal([Measures].[Store Sqft] / [Measures].[Grocery Sqft])',"
            + " FORMAT_STRING = \"0.000000\"\n"
            + "SELECT {[Measures].[InverseNormal]} ON COLUMNS, \n"
            + "  {[Store Type].children} ON ROWS \n"
            + "FROM [Store]");
        Axis rowAxis = result.getAxes()[0];
        assertTrue(rowAxis.getPositions().size() == 1);
        Axis colAxis = result.getAxes()[1];
        assertTrue(colAxis.getPositions().size() == 6);
        Cell cell = result.getCell(new int[]{0, 0});
        assertTrue(cell.isError());
        assertMatchesVerbose(
            Pattern.compile(
                "(?s).*Invalid value for inverse normal distribution: 1.4708.*"),
            cell.getValue().toString());
        cell = result.getCell(new int[]{0, 5});
        assertTrue(cell.isError());
        assertMatchesVerbose(
            Pattern.compile(
                "(?s).*Invalid value for inverse normal distribution: 1.4435.*"),
            cell.getValue().toString());
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCurrentDateString(Context context)
    {
        prepareContext(context);
        String actual = executeExpr(context.getConnectionWithDefaultRole(), "Sales", "CurrentDateString(\"Ddd mmm dd yyyy\")");
        Date currDate = new Date();
        String dateString = currDate.toString();
        String expected =
            dateString.substring(0, 11)
            + dateString.substring(dateString.length() - 4);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCurrentDateMemberBefore(Context context) {
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT { CurrentDateMember([Time].[Time], "
            + "\"[Ti\\me]\\.[yyyy]\\.[Qq]\\.[m]\", BEFORE)} "
            + "ON COLUMNS FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Time].[1998].[Q4].[12]}\n"
            + "Row #0: \n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCurrentDateMemberBeforeUsingQuotes(Context context)
    {
        prepareContext(context);
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            SystemWideProperties.instance().SsasCompatibleNaming
            ? "CurrentDateMember([Time].[Time], "
            + "'\"[Time].[Time].[\"yyyy\"].[Q\"q\"].[\"m\"]\"', BEFORE)"
            : "CurrentDateMember([Time], "
            + "'\"[Time].[\"yyyy\"].[Q\"q\"].[\"m\"]\"', BEFORE)",
            "[Time].[1998].[Q4].[12]");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCurrentDateMemberAfter(Context context)
    {
        prepareContext(context);
        // CurrentDateMember will return null member since the latest date in
        // FoodMart is from '98
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT { CurrentDateMember([Time].[Time], "
            + "\"[Ti\\me]\\.[yyyy]\\.[Qq]\\.[m]\", AFTER)} "
            + "ON COLUMNS FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCurrentDateMemberExact(Context context)
    {
        prepareContext(context);
        // CurrentDateMember will return null member since the latest date in
        // FoodMart is from '98; apply a function on the return value to
        // ensure null member instead of null is returned
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT { CurrentDateMember([Time].[Time], "
            + "\"[Ti\\me]\\.[yyyy]\\.[Qq]\\.[m]\", EXACT).lag(1)} "
            + "ON COLUMNS FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCurrentDateMemberNoFindArg(Context context)
    {
        prepareContext(context);
        // CurrentDateMember will return null member since the latest date in
        // FoodMart is from '98
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT { CurrentDateMember([Time].[Time], "
            + "\"[Ti\\me]\\.[yyyy]\\.[Qq]\\.[m]\", EXACT)} "
            + "ON COLUMNS FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCurrentDateMemberHierarchy(Context context) {
        prepareContext(context);
        final String query =
            SystemWideProperties.instance().SsasCompatibleNaming
                ? "SELECT { CurrentDateMember([Time.Weekly], "
                  + "\"[Ti\\me\\.Weekl\\y]\\.[All Weekl\\y\\s]\\.[yyyy]\\.[ww]\", BEFORE)} "
                  + "ON COLUMNS FROM [Sales]"
                : "SELECT { CurrentDateMember([Time.Weekly], "
                  + "\"[Ti\\me\\.Weekl\\y]\\.[All Ti\\me\\.Weekl\\y\\s]\\.[yyyy]\\.[ww]\", BEFORE)} "
                  + "ON COLUMNS FROM [Sales]";
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            query,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Time.Weekly].[1998].[52]}\n"
            + "Row #0: \n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCurrentDateMemberHierarchyNullReturn(Context context) {
        prepareContext(context);
        // CurrentDateMember will return null member since the latest date in
        // FoodMart is from '98; note that first arg is a hierarchy rather
        // than a dimension
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT { CurrentDateMember([Time.Weekly], "
            + "\"[Ti\\me]\\.[yyyy]\\.[Qq]\\.[m]\", EXACT)} "
            + "ON COLUMNS FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCurrentDateMemberRealAfter(Context context) {
        prepareContext(context);
        // omit formatting characters from the format so the current date
        // is hard-coded to actual value in the database so we can test the
        // after logic
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT { CurrentDateMember([Time].[Time], "
            + "\"[Ti\\me]\\.[1996]\\.[Q4]\", after)} "
            + "ON COLUMNS FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Time].[1997].[Q1]}\n"
            + "Row #0: 66,291\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCurrentDateMemberRealExact1(Context context) {
        prepareContext(context);
        // omit formatting characters from the format so the current date
        // is hard-coded to actual value in the database so we can test the
        // exact logic
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT { CurrentDateMember([Time].[Time], "
            + "\"[Ti\\me]\\.[1997]\", EXACT)} "
            + "ON COLUMNS FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Time].[1997]}\n"
            + "Row #0: 266,773\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCurrentDateMemberRealExact2(Context context) {
        prepareContext(context);
        // omit formatting characters from the format so the current date
        // is hard-coded to actual value in the database so we can test the
        // exact logic
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT { CurrentDateMember([Time].[Time], "
            + "\"[Ti\\me]\\.[1997]\\.[Q2]\\.[5]\", EXACT)} "
            + "ON COLUMNS FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Time].[1997].[Q2].[5]}\n"
            + "Row #0: 21,081\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCurrentDateMemberPrev(Context context) {
        prepareContext(context);
        // apply a function on the result of the UDF
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT { CurrentDateMember([Time].[Time], "
            + "\"[Ti\\me]\\.[yyyy]\\.[Qq]\\.[m]\", BEFORE).PrevMember} "
            + "ON COLUMNS FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Time].[1998].[Q4].[11]}\n"
            + "Row #0: \n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCurrentDateLag(Context context) {
        prepareContext(context);
        // Also, try a different style of quoting, because single quote followed
        // by double quote (used in other examples) is difficult to read.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT\n"
            + "    { [Measures].[Unit Sales] } ON COLUMNS,\n"
            + "    { CurrentDateMember([Time].[Time], '[\"Time\"]\\.[yyyy]\\.[\"Q\"q]\\.[m]', BEFORE).Lag(3) : "
            + "      CurrentDateMember([Time].[Time], '[\"Time\"]\\.[yyyy]\\.[\"Q\"q]\\.[m]', BEFORE) } ON ROWS\n"
            + "FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Time].[1998].[Q3].[9]}\n"
            + "{[Time].[1998].[Q4].[10]}\n"
            + "{[Time].[1998].[Q4].[11]}\n"
            + "{[Time].[1998].[Q4].[12]}\n"
            + "Row #0: \n"
            + "Row #1: \n"
            + "Row #2: \n"
            + "Row #3: \n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMatches(Context context) {
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT {[Measures].[Org Salary]} ON COLUMNS, "
            + "Filter({[Employees].MEMBERS}, "
            + "[Employees].CurrentMember.Name MATCHES '(?i)sam.*') ON ROWS "
            + "FROM [HR]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Org Salary]}\n"
            + "Axis #2:\n"
            + "{[Employees].[Sheri Nowmer].[Derrick Whelply].[Beverly Baker].[Jacqueline Wyllie].[Ralph Mccoy].[Anne Tuck].[Samuel Johnson]}\n"
            + "{[Employees].[Sheri Nowmer].[Derrick Whelply].[Pedro Castillo].[Jose Bernard].[Mary Hunt].[Bonnie Bruno].[Sam Warren]}\n"
            + "{[Employees].[Sheri Nowmer].[Derrick Whelply].[Pedro Castillo].[Charles Macaluso].[Barbara Wallin].[Michael Suggs].[Sam Adair]}\n"
            + "{[Employees].[Sheri Nowmer].[Derrick Whelply].[Pedro Castillo].[Lois Wood].[Dell Gras].[Kristine Aldred].[Sam Zeller]}\n"
            + "{[Employees].[Sheri Nowmer].[Derrick Whelply].[Laurie Borges].[Cody Goldey].[Shanay Steelman].[Neal Hasty].[Sam Wheeler]}\n"
            + "{[Employees].[Sheri Nowmer].[Maya Gutierrez].[Brenda Blumberg].[Wayne Banack].[Samuel Agcaoili]}\n"
            + "{[Employees].[Sheri Nowmer].[Maya Gutierrez].[Jonathan Murraiin].[James Thompson].[Samantha Weller]}\n"
            + "Row #0: $40.62\n"
            + "Row #1: $40.31\n"
            + "Row #2: $75.60\n"
            + "Row #3: $40.35\n"
            + "Row #4: $47.52\n"
            + "Row #5: \n"
            + "Row #6: \n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNotMatches(Context context) {
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT {[Measures].[Store Sales]} ON COLUMNS, "
            + "Filter({[Store Type].MEMBERS}, "
            + "[Store Type].CurrentMember.Name NOT MATCHES "
            + "'.*Grocery.*') ON ROWS "
            + "FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Store Sales]}\n"
            + "Axis #2:\n"
            + "{[Store Type].[All Store Types]}\n"
            + "{[Store Type].[Deluxe Supermarket]}\n"
            + "{[Store Type].[Gourmet Supermarket]}\n"
            + "{[Store Type].[HeadQuarters]}\n"
            + "{[Store Type].[Supermarket]}\n"
            + "Row #0: 565,238.13\n"
            + "Row #1: 162,062.24\n"
            + "Row #2: 45,750.24\n"
            + "Row #3: \n"
            + "Row #4: 319,210.04\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIn(Context context) {
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT {[Measures].[Unit Sales]} ON COLUMNS, "
            + "FILTER([Product].[Product Family].MEMBERS, "
            + "[Product].[Product Family].CurrentMember IN "
            + "{[Product].[All Products].firstChild, "
            + "[Product].[All Products].lastChild}) ON ROWS "
            + "FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Product].[Drink]}\n"
            + "{[Product].[Non-Consumable]}\n"
            + "Row #0: 24,597\n"
            + "Row #1: 50,236\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNotIn(Context context) {
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT {[Measures].[Unit Sales]} ON COLUMNS, "
            + "FILTER([Product].[Product Family].MEMBERS, "
            + "[Product].[Product Family].CurrentMember NOT IN "
            + "{[Product].[All Products].firstChild, "
            + "[Product].[All Products].lastChild}) ON ROWS "
            + "FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[Product].[Food]}\n"
            + "Row #0: 191,940\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testChildMemberIn(Context context) {
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT {[Measures].[Store Sales]} ON COLUMNS, "
            + "{[Store].[Store Name].MEMBERS} ON ROWS "
            + "FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Store Sales]}\n"
            + "Axis #2:\n"
            + "{[Store].[Canada].[BC].[Vancouver].[Store 19]}\n"
            + "{[Store].[Canada].[BC].[Victoria].[Store 20]}\n"
            + "{[Store].[Mexico].[DF].[Mexico City].[Store 9]}\n"
            + "{[Store].[Mexico].[DF].[San Andres].[Store 21]}\n"
            + "{[Store].[Mexico].[Guerrero].[Acapulco].[Store 1]}\n"
            + "{[Store].[Mexico].[Jalisco].[Guadalajara].[Store 5]}\n"
            + "{[Store].[Mexico].[Veracruz].[Orizaba].[Store 10]}\n"
            + "{[Store].[Mexico].[Yucatan].[Merida].[Store 8]}\n"
            + "{[Store].[Mexico].[Zacatecas].[Camacho].[Store 4]}\n"
            + "{[Store].[Mexico].[Zacatecas].[Hidalgo].[Store 12]}\n"
            + "{[Store].[Mexico].[Zacatecas].[Hidalgo].[Store 18]}\n"
            + "{[Store].[USA].[CA].[Alameda].[HQ]}\n"
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
            + "Row #0: \n"
            + "Row #1: \n"
            + "Row #2: \n"
            + "Row #3: \n"
            + "Row #4: \n"
            + "Row #5: \n"
            + "Row #6: \n"
            + "Row #7: \n"
            + "Row #8: \n"
            + "Row #9: \n"
            + "Row #10: \n"
            + "Row #11: \n"
            + "Row #12: 45,750.24\n"
            + "Row #13: 54,545.28\n"
            + "Row #14: 54,431.14\n"
            + "Row #15: 4,441.18\n"
            + "Row #16: 55,058.79\n"
            + "Row #17: 87,218.28\n"
            + "Row #18: 4,739.23\n"
            + "Row #19: 52,896.30\n"
            + "Row #20: 52,644.07\n"
            + "Row #21: 49,634.46\n"
            + "Row #22: 74,843.96\n"
            + "Row #23: 4,705.97\n"
            + "Row #24: 24,329.23\n");

        // test when the member arg is at a different level
        // from the set argument
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT {[Measures].[Store Sales]} ON COLUMNS, "
            + "Filter({[Store].[Store Name].MEMBERS}, "
            + "[Store].[Store Name].CurrentMember IN "
            + "{[Store].[All Stores].[Mexico], "
            + "[Store].[All Stores].[USA]}) ON ROWS "
            + "FROM [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Store Sales]}\n"
            + "Axis #2:\n");
    }

    /**
     * Tests that the inferred return type is correct for a UDF whose return
     * type is not the same as would be guessed by the default implementation
     * of {@link org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition#getResultType}, which simply
     * guesses based on the type of the first argument.
     */
    @Disabled //TODO: UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonGuessableReturnType(Context context) {
        /*
        udfTestContext(context,
            "<UserDefinedFunction name=\"StringMult\" className=\""
            + StringMultUdf.class.getName()
            + "\"/>\n");
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier15::new);
        // The default implementation of getResultType would assume that
        // StringMult(int, string) returns an int, whereas it returns a string.
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "StringMult(5, 'foo') || 'bar'", "foofoofoofoofoobar");
    }

    /**
     * Test case for the problem where a string expression gave a
     * ClassCastException because it was evaluating to a member, whereas the
     * member should have been evaluated to a scalar.
     */
    @Disabled //TODO: UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUdfToString(Context context) {
        /*
        udfTestContext(context,
            "<UserDefinedFunction name=\"StringMult\" className=\""
            + StringMultUdf.class.getName()
            + "\"/>\n");
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier15::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Measures].[ABC] as StringMult(1, 'A')\n"
            + "member [Measures].[Unit Sales Formatted] as\n"
            + "  [Measures].[Unit Sales],\n"
            + "  FORMAT_STRING = '#,###|color=' ||\n"
            + "      Iif([Measures].[ABC] = 'A', 'red', 'green')\n"
            + "select [Measures].[Unit Sales Formatted] on 0\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales Formatted]}\n"
            + "Row #0: 266,773|color=red\n");
    }

    /**
     * Tests a UDF whose return type is not the same as its first
     * parameter. The return type needs to have full dimensional information;
     * in this case, HierarchyType(dimension=Time, hierarchy=unknown).
     *
     * <p>Also tests applying a UDF to arguments of coercible type. In this
     * case, applies f(member,dimension) to args(member,hierarchy).
     */
    @Disabled //TODO: UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testAnotherMemberFun(Context context) {
        /*
        udfTestContext(context,
            "<UserDefinedFunction name=\"PlusOne\" className=\""
            + PlusOneUdf.class.getName() + "\"/>\n"
            + "<UserDefinedFunction name=\"AnotherMemberError\" className=\""
            + AnotherMemberErrorUdf.class.getName() + "\"/>");
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier16::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [Measures].[Test] AS "
            + "'([Measures].[Store Sales],[Product].[Food],AnotherMemberError([Product].[Drink],[Time].[Time]))'\n"
            + "SELECT {[Measures].[Test]} ON COLUMNS, \n"
            + "  {[Customers].DefaultMember} ON ROWS \n"
            + "FROM [Sales]",

            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Test]}\n"
            + "Axis #2:\n"
            + "{[Customers].[All Customers]}\n"
            + "Row #0: 409,035.59\n");
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCachingCurrentDate(Context context) {
        prepareContext(context);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "SELECT {filter([Time].[Month].Members, "
            + "[Time].[Time].CurrentMember in {CurrentDateMember([Time]"
            + ".[Time], '[\"Time\"]\\.[yyyy]\\.[\"Q\"q]\\.[m]', "
            + "BEFORE)})} ON COLUMNS "
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Time].[1998].[Q4].[12]}\n"
            + "Row #0: \n");
    }

    /**
     * Test case for a UDF that returns a list.
     *
     * <p>Test case for bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-588">MONDRIAN-588,
     * "UDF returning List works under 2.4, fails under 3.1.1"</a>.
     *
     * <p>Also test case for bug
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-589">MONDRIAN-589,
     * "UDF expecting List gets anonymous
     * mondrian.rolap.RolapNamedSetEvaluator$1 instead"</a>.
     */
    @Disabled //TODO: UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testListUdf(Context context) {
        prepareContext(context);
        checkListUdf(context, ReverseFunction.class);
        checkListUdf(context, ReverseIterableFunction.class);
    }

    /**
     * Helper for {@link #testListUdf()}.
     *
     * @param functionClass Class that implements the "Reverse" function.
     */
    private void checkListUdf(Context context,
        final Class<? extends ReverseFunction> functionClass)
    {
        /*
        udfTestContext(context,
            "<UserDefinedFunction name=\"Reverse\" className=\""
            + functionClass.getName()
            + "\"/>\n");
         */
    	CatalogMapping catalogMapping = ((RolapContext) context).getCatalogMapping();
    	PojoMappingModifier modifier = new SchemaModifiers.UdfTestModifier17(catalogMapping, functionClass);
        updateTestContext(context, modifier);
        final String expectedResult =
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Gender].[M]}\n"
            + "{[Gender].[F]}\n"
            + "{[Gender].[All Gender]}\n"
            + "Row #0: 135,215\n"
            + "Row #0: 131,558\n"
            + "Row #0: 266,773\n";
        // UDF called directly in axis expression.
        Connection connection = context.getConnectionWithDefaultRole();
        assertQueryReturns(connection,
            "select Reverse([Gender].Members) on 0\n"
            + "from [Sales]",
            expectedResult);
        // UDF as calc set definition
        assertQueryReturns(connection,
            "with set [Foo] as Reverse([Gender].Members)\n"
            + "select [Foo] on 0\n"
            + "from [Sales]",
            expectedResult);
        // UDF applied to calc set -- exhibited MONDRIAN-589
        assertQueryReturns(connection,
            "with set [Foo] as [Gender].Members\n"
            + "select Reverse([Foo]) on 0\n"
            + "from [Sales]", expectedResult);
    }

    /**
     * Tests that a non-static function gives an error.
     */
    @Disabled //TODO: UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonStaticUdfFails(Context context) {
        /*
        udfTestContext(context,
            "<UserDefinedFunction name=\"Reverse2\" className=\""
            + ReverseFunctionNotStatic.class.getName()
            + "\"/>\n");
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier18::new);
        assertQueryThrows(context,
            "select Reverse2([Gender].Members) on 0\n" + "from [Sales]",
            "No function matches signature 'Reverse2(<Set>)'");
    }

    /**
     * Tests a function that takes a member as argument. Want to make sure that
     * Mondrian leaves it as a member, does not try to evaluate it to a scalar
     * value.
     */
    @Disabled //TODO: UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMemberUdfDoesNotEvaluateToScalar(Context context) {
        /*
        udfTestContext(context,
            "<UserDefinedFunction name=\"MemberName\" className=\""
            + MemberNameFunction.class.getName()
            + "\"/>\n");
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier19::new);
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "MemberName([Gender].[F])", "F");
    }

    /**
     * Unit test that ensures that a UDF has either a script or a className.
     */
    @Disabled //disabled for CI build TODO: UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUdfNeitherScriptNorClassname(Context context) {
        /*
        udfTestContext(context,
            "<UserDefinedFunction name='StringMult'/>\n");
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier20::new);
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select from [Sales]",
            "Must specify either className attribute or Script element");
    }

    /**
     * Unit test that ensures that a UDF does not have both a script
     * and a className.
     */
    @Disabled //disabled for CI build TODO: UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUdfBothScriptAndClassname(Context context) {
       /*
       udfTestContext(context,
            "<UserDefinedFunction name='StringMult' className='foo'>\n"
            + " <Script>bar</Script>\n"
            + "</UserDefinedFunction>");
        */
        updateTestContext(context, SchemaModifiers.UdfTestModifier21::new);
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select from [Sales]",
            "Must not specify both className attribute and Script element");
    }

    /**
     * Unit test that ensures that a UDF has either a script or a className.
     */
    @Disabled //disabled for CI build TODO: UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testUdfScriptBadLanguage(Context context) {
        /*
        udfTestContext(context,
            "<UserDefinedFunction name='StringMult'>\n"
            + " <Script language='bad'>bar</Script>\n"
            + "</UserDefinedFunction>");
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier22::new);
        assertQueryThrows(context.getConnectionWithDefaultRole(),
            "select from [Sales]",
            "Invalid script language 'bad'");
    }

    /**
     * Unit test for a UDF defined in JavaScript.
     */
    @Disabled //TODO: UserDefinedFunction
    @ParameterizedTest
    @DisabledIfSystemProperty(named = "tempIgnoreStrageTests",matches = "true")
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testScriptUdf(Context context) {
        /*
        udfTestContext(context,
            "<UserDefinedFunction name='StringMult'>\n"
            + "  <Script language='JavaScript'>\n"
            + "    function getParameterTypes() {\n"
            + "      return new Array(\n"
            + "        new mondrian.olap.type.NumericType(),\n"
            + "        new mondrian.olap.type.StringType());\n"
            + "    }\n"
            + "    function getReturnType(parameterTypes) {\n"
            + "      return new mondrian.olap.type.StringType();\n"
            + "    }\n"
            + "    function execute(evaluator, arguments) {\n"
            + "      var n = arguments[0].evaluateScalar(evaluator);\n"
            + "      var s = arguments[1].evaluateScalar(evaluator);\n"
            + "      var r = \"\";\n"
            + "      while (n-- > 0) {\n"
            + "        r = r + s;\n"
            + "      }\n"
            + "      return r;\n"
            + "    }\n"
            + "  </Script>\n"
            + "</UserDefinedFunction>\n");
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier23::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Measures].[ABC] as StringMult(1, 'A')\n"
            + "member [Measures].[Unit Sales Formatted] as\n"
            + "  [Measures].[Unit Sales],\n"
            + "  FORMAT_STRING = '#,###|color=' ||\n"
            + "      Iif([Measures].[ABC] = 'A', 'red', 'green')\n"
            + "select [Measures].[Unit Sales Formatted] on 0\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales Formatted]}\n"
            + "Row #0: 266,773|color=red\n");
    }

    /**
     * Unit test for a UDF defined in JavaScript, this time the factorial
     * function. We also use 'CDATA' section to mask the '&lt;' symbol.
     */
    @Disabled //TODO: UserDefinedFunction
    @ParameterizedTest
    @DisabledIfSystemProperty(named = "tempIgnoreStrageTests",matches = "true")
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testScriptUdfFactorial(Context context) {
    	//prepareContext(context);
        /*
        udfTestContext(context,
            "<UserDefinedFunction name='Factorial'>\n"
            + "  <Script language='JavaScript'><![CDATA[\n"
            + "    function getParameterTypes() {\n"
            + "      return new Array(\n"
            + "        new mondrian.olap.type.NumericType());\n"
            + "    }\n"
            + "    function getReturnType(parameterTypes) {\n"
            + "      return new mondrian.olap.type.NumericType();\n"
            + "    }\n"
            + "    function execute(evaluator, arguments) {\n"
            + "      var n = arguments[0].evaluateScalar(evaluator);\n"
            + "      return factorial(n);\n"
            + "    }\n"
            + "    function factorial(n) {\n"
            + "      return n <= 1 ? 1 : n * factorial(n - 1);\n"
            + "    }\n"
            + "  ]]>\n"
            + "  </Script>\n"
            + "</UserDefinedFunction>\n");
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier24::new);
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Factorial(4 + 2)",
            "720");
    }

    /**
     * Unit test that we get a nice error if a script UDF contains an error.
     */
    @Disabled //TODO: UserDefinedFunction
    @ParameterizedTest
    @DisabledIfSystemProperty(named = "tempIgnoreStrageTests",matches = "true")
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testScriptUdfInvalid(Context context) {
        /*
        udfTestContext(context,
            "<UserDefinedFunction name='Factorial'>\n"
            + "  <Script language='JavaScript'><![CDATA[\n"
            + "    function getParameterTypes() {\n"
            + "      return new Array(\n"
            + "        new mondrian.olap.type.NumericType());\n"
            + "    }\n"
            + "    function getReturnType(parameterTypes) {\n"
            + "      return new mondrian.olap.type.NumericType();\n"
            + "    }\n"
            + "    function execute(evaluator, arguments) {\n"
            + "      var n = arguments[0].evaluateScalar(evaluator);\n"
            + "      return factorial(n);\n"
            + "    }\n"
            + "    function factorial(n) {\n"
            + "      return n <= 1 ? 1 : n * factorial_xx(n - 1);\n"
            + "    }\n"
            + "  ]]>\n"
            + "  </Script>\n"
            + "</UserDefinedFunction>\n");
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier25::new);
        final Cell cell = executeExprRaw(context.getConnectionWithDefaultRole(), "Sales", "Factorial(4 + 2)");
        assertMatchesVerbose(
            Pattern.compile(
                "(?s).*ReferenceError: \"factorial_xx\" is not defined..*"),
            cell.getValue().toString());
    }

    /**
     * Unit test for a cell formatter defined in the old way -- a 'formatter'
     * attribute of a Measure element.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCellFormatter(Context context) {
        prepareContext(context);
        // Note that
        //   formatString="Standard"
        // is ignored.
        /*
        measureTestContext(context,
            "<Measure name='Unit Sales Foo Bar' column='unit_sales'\n"
            + "    aggregator='sum' formatString='Standard' formatter='"
            + FooBarCellFormatter.class.getName()
            + "'/>");
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier1::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures].[Unit Sales],\n"
            + "      [Measures].[Unit Sales Foo Bar]} on 0\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Unit Sales Foo Bar]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: foo266773.0bar\n");
    }

    /**
     * As {@link #testCellFormatter()}, but using new-style nested
     * CellFormatter element.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCellFormatterNested(Context context) {
        prepareContext(context);
        // Note that
        //   formatString="Standard"
        // is ignored.
        /*
        measureTestContext(context,
            "<Measure name='Unit Sales Foo Bar' column='unit_sales'\n"
            + "    aggregator='sum' formatString='Standard'>\n"
            + "  <CellFormatter className='"
            + FooBarCellFormatter.class.getName()
            + "'/>\n"
            + "</Measure>");
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier1::new);

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures].[Unit Sales],\n"
            + "      [Measures].[Unit Sales Foo Bar]} on 0\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Unit Sales Foo Bar]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: foo266773.0bar\n");
    }

    /**
     * As {@link #testCellFormatterNested()}, but using a script.
     */
    @Disabled //TODO: UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCellFormatterScript(Context context) {
        /*
        measureTestContext(context,
            "<Measure name='Unit Sales Foo Bar' column='unit_sales'\n"
            + "    aggregator='sum' formatString='Standard'>\n"
            + "  <CellFormatter>\n"
            + "    <Script>\n"
            + "      return \"foo\" + value + \"bar\";\n"
            + "    </Script>\n"
            + "  </CellFormatter>\n"
            + "</Measure>");
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier2::new);

        // Note that the result is slightly different to above (a missing ".0").
        // Not a great concern -- in fact it proves that the scripted UDF is
        // being used.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures].[Unit Sales],\n"
            + "      [Measures].[Unit Sales Foo Bar]} on 0\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Unit Sales Foo Bar]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: foo266773bar\n");
    }

    /**
     * Unit test for a cell formatter defined against a calculated member,
     * using the old syntax (a member property called "CELL_FORMATTER").
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCellFormatterOnCalcMember(Context context) {
        /*
        calcMemberTestContext(context,
            "<CalculatedMember\n"
            + "  name='Unit Sales Foo Bar'\n"
            + "      dimension='Measures'>\n"
            + "  <Formula>[Measures].[Unit Sales]</Formula>\n"
            + "  <CalculatedMemberProperty name='CELL_FORMATTER' value='"
            + FooBarCellFormatter.class.getName()
            + "'/>\n"
            + "</CalculatedMember>");
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier3::new);

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures].[Unit Sales],\n"
            + "      [Measures].[Unit Sales Foo Bar]} on 0\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Unit Sales Foo Bar]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: foo266773.0bar\n");
    }

    /**
     * Unit test for a cell formatter defined against a calculated member,
     * using the new syntax (a nested CellFormatter element).
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCellFormatterOnCalcMemberNested(Context context) {
        /*
        calcMemberTestContext(context,
            "<CalculatedMember\n"
            + "  name='Unit Sales Foo Bar'\n"
            + "      dimension='Measures'>\n"
            + "  <Formula>[Measures].[Unit Sales]</Formula>\n"
            + "  <CellFormatter className='"
            + FooBarCellFormatter.class.getName()
            + "'/>\n"
            + "</CalculatedMember>");
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier4::new);

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures].[Unit Sales],\n"
            + "      [Measures].[Unit Sales Foo Bar]} on 0\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Unit Sales Foo Bar]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: foo266773.0bar\n");
    }

    /**
     * Unit test for a cell formatter defined against a calculated member,
     * using a script.
     */
    @Disabled //TODO: UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCellFormatterOnCalcMemberScript(Context context) {
        prepareContext(context);
        /*
        calcMemberTestContext(context,
            "<CalculatedMember\n"
            + "  name='Unit Sales Foo Bar'\n"
            + "      dimension='Measures'>\n"
            + "  <Formula>[Measures].[Unit Sales]</Formula>\n"
            + "  <CellFormatter>\n"
            + "    <Script>\n"
            + "      return \"foo\" + value + \"bar\";\n"
            + "    </Script>\n"
            + "  </CellFormatter>\n"
            + "</CalculatedMember>");
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier5::new);

        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Measures].[Unit Sales],\n"
            + "      [Measures].[Unit Sales Foo Bar]} on 0\n"
            + "from [Sales]",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Unit Sales Foo Bar]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: foo266773bar\n");
    }

    /**
     * Unit test for a member formatter defined in the old way -- a 'formatter'
     * attribute of a Measure element.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMemberFormatter(Context context) {
        prepareContext(context);
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "  <Dimension name='Promotion Media2' foreignKey='promotion_id'>\n"
            + "    <Hierarchy hasAll='true' allMemberName='All Media' primaryKey='promotion_id'>\n"
            + "      <Table name='promotion'/>\n"
            + "      <Level name='Media Type' column='media_type'\n"
            + "          uniqueMembers='true' formatter='"
            + FooBarMemberFormatter.class.getName()
            + "'/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>"));
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier6::new);
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "[Promotion Media2].FirstChild.Caption",
            "fooBulk Mailbar");
    }

    /**
     * As {@link #testMemberFormatter()}, but using new-style nested
     * memberFormatter element.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMemberFormatterNested(Context context) {
        prepareContext(context);
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "  <Dimension name='Promotion Media2' foreignKey='promotion_id'>\n"
            + "    <Hierarchy hasAll='true' allMemberName='All Media' primaryKey='promotion_id'>\n"
            + "      <Table name='promotion'/>\n"
            + "      <Level name='Media Type' column='media_type'\n"
            + "          uniqueMembers='true'>\n"
            + "        <MemberFormatter className='"
            + FooBarMemberFormatter.class.getName()
            + "'/>\n"
            + "      </Level>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>"));
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier6::new);
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "[Promotion Media2].FirstChild.Caption",
            "fooBulk Mailbar");
    }

    /**
     * As {@link #testMemberFormatterNested()}, but using a script.
     */
    @Disabled //TODO: UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMemberFormatterScript(Context context) {
        prepareContext(context);
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "  <Dimension name='Promotion Media2' foreignKey='promotion_id'>\n"
            + "    <Hierarchy hasAll='true' allMemberName='All Media' primaryKey='promotion_id'>\n"
            + "      <Table name='promotion'/>\n"
            + "      <Level name='Media Type' column='media_type'\n"
            + "          uniqueMembers='true'>\n"
            + "        <MemberFormatter>\n"
            + "          <Script language='JavaScript'>\n"
            + "             return \"foo\" + member.getName() + \"bar\"\n"
            + "          </Script>\n"
            + "        </MemberFormatter>\n"
            + "      </Level>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>"));
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier7::new);
        assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
            "[Promotion Media2].FirstChild.Caption",
            "fooBulk Mailbar");
    }

    /**
     * Unit test for a property formatter defined in the old way -- a
     * 'formatter' attribute of a Property element.
     *
     * @throws SQLException on error
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testPropertyFormatter(Context context) throws SQLException {
        prepareContext(context);
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "<Dimension name='Promotions2' foreignKey='promotion_id'>\n"
            + "  <Hierarchy hasAll='true' allMemberName='All Promotions' primaryKey='promotion_id' defaultMember='[All Promotions]'>\n"
            + "    <Table name='promotion'/>\n"
            + "    <Level name='Promotion Name' column='promotion_id' uniqueMembers='true'>\n"
            + "      <Property name='Medium' column='media_type' formatter='"
            + FooBarPropertyFormatter.class.getName()
            + "'/>\n"
            + "    </Level>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>"));
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier8::new);
        final CellSet result =
            executeOlap4jQuery(context.getConnectionWithDefaultRole(),
                "select [Promotions2].Children on 0\n"
                + "from [Sales]");
        final Member member =
            result.getAxes().get(0).getPositions().get(0).getMembers().get(0);
        final mondrian.olap.Property property = Arrays.stream(member.getProperties()).filter(p -> "Medium".equals(p.name)).findFirst()
            .orElseThrow(() -> new RuntimeException("property with name \"Medium\" is absent"));
        assertEquals(
            "foo0/Medium/No Mediabar",
            member.getPropertyFormattedValue(property.name));
    }

    /**
     * As {@link #testPropertyFormatter()}, but using new-style nested
     * PropertyFormatter element.
     *
     * @throws SQLException on error
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testPropertyFormatterNested(Context context) throws SQLException {
        prepareContext(context);
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "<Dimension name='Promotions2' foreignKey='promotion_id'>\n"
            + "  <Hierarchy hasAll='true' allMemberName='All Promotions' primaryKey='promotion_id' defaultMember='[All Promotions]'>\n"
            + "    <Table name='promotion'/>\n"
            + "    <Level name='Promotion Name' column='promotion_id' uniqueMembers='true'>\n"
            + "      <Property name='Medium' column='media_type'>\n"
            + "        <PropertyFormatter className='"
            + FooBarPropertyFormatter.class.getName()
            + "'/>\n"
            + "      </Property>\n"
            + "    </Level>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>"));
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier9::new);

        final CellSet result =
            executeOlap4jQuery(context.getConnectionWithDefaultRole(),
                "select [Promotions2].Children on 0\n"
                + "from [Sales]");
        final Member member =
            result.getAxes().get(0).getPositions().get(0).getMembers().get(0);
        final Property property = Arrays.stream(member.getProperties())
            .filter(p -> "Medium".equals(p.name)).findFirst()
            .orElseThrow(() -> new RuntimeException("Property with name \"Medium\" is absent"));
        assertEquals(
            "foo0/Medium/No Mediabar",
            member.getPropertyFormattedValue(property.name));
    }

    /**
     * As {@link #testPropertyFormatterNested()}, but using a script.
     *
     * @throws SQLException on error
     */
    @Disabled //TODO: UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testPropertyFormatterScript(Context context) throws SQLException {
        prepareContext(context);
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
            "Sales",
            "<Dimension name='Promotions2' foreignKey='promotion_id'>\n"
            + "  <Hierarchy hasAll='true' allMemberName='All Promotions' primaryKey='promotion_id' defaultMember='[All Promotions]'>\n"
            + "    <Table name='promotion'/>\n"
            + "    <Level name='Promotion Name' column='promotion_id' uniqueMembers='true'>\n"
            + "      <Property name='Medium' column='media_type'>\n"
            + "        <PropertyFormatter>\n"
            + "          <Script language='JavaScript'>\n"
            + "            return \"foo\" + member.getName() + \"/\"\n"
            + "                   + propertyName + \"/\"\n"
            + "                   + propertyValue + \"bar\";\n"
            + "          </Script>\n"
            + "        </PropertyFormatter>\n"
            + "      </Property>\n"
            + "    </Level>\n"
            + "  </Hierarchy>\n"
            + "</Dimension>"));
         */
        updateTestContext(context, SchemaModifiers.UdfTestModifier10::new);

        final CellSet result =
            executeOlap4jQuery(context.getConnectionWithDefaultRole(),
                "select [Promotions2].Children on 0\n"
                + "from [Sales]");
        final Member member =
            result.getAxes().get(0).getPositions().get(0).getMembers().get(0);
        final
        mondrian.olap.Property property = Arrays.stream(member.getProperties()).filter(p -> "Medium".equals(p.name))
            .findFirst().orElseThrow(() -> new RuntimeException("Propertywith name \"Medium\" is absent"));
        assertEquals(
            "foo0/Medium/No Mediabar",
            member.getPropertyFormattedValue(property.name));
    }


    /**
     * A simple user-defined function which adds one to its argument.
     */
    public static class PlusOneUdf implements UserDefinedFunction {
        @Override
		public String getName() {
            return "PlusOne";
        }

        @Override
		public String getDescription() {
            return "Returns its argument plus one";
        }

        @Override
		public Syntax getSyntax() {
            return Syntax.Function;
        }

        @Override
		public Type getReturnType(Type[] parameterTypes) {
            return NumericType.INSTANCE;
        }

        @Override
		public Type[] getParameterTypes() {
            return new Type[] {NumericType.INSTANCE};
        }

        @Override
		public Object execute(Evaluator evaluator, Argument[] arguments) {
            final Object argValue = arguments[0].evaluateScalar(evaluator);
            if (argValue instanceof Number) {
                return ((Number) argValue).doubleValue() + 1.0;
            } else {
                // Argument might be a RuntimeException indicating that
                // the cache does not yet have the required cell value. The
                // function will be called again when the cache is loaded.
                return null;
            }
        }


    }

    /**
     * A simple user-defined function which adds one to its argument.
     */
    public static class BadPlusOneUdf extends PlusOneUdf {
        private final String name;

        public BadPlusOneUdf(String name) {
            this.name = name;
        }

        @Override
		public String getName() {
            return name;
        }

        @Override
		public Type getReturnType(Type[] parameterTypes) {
            // Will cause error.
            return null;
        }
    }

    /**
     * A user-defined function which, depending on its given name, either adds
     * one to, or subtracts one from, its argument.
     */
    public static class PlusOrMinusOneUdf implements UserDefinedFunction {
        private final String name;

        public PlusOrMinusOneUdf(String name) {
            if (!(name.equals("GenericPlusOne")
                  || name.equals("GenericMinusOne")))
            {
                throw new IllegalArgumentException();
            }
            this.name = name;
        }

        @Override
		public String getName() {
            return name;
        }

        @Override
		public String getDescription() {
            return
                "A user-defined function which, depending on its given name, "
                + "either addsone to, or subtracts one from, its argument";
        }

        @Override
		public Syntax getSyntax() {
            return Syntax.Function;
        }

        @Override
		public Type getReturnType(Type[] parameterTypes) {
            return NumericType.INSTANCE;
        }



        @Override
		public Type[] getParameterTypes() {
            return new Type[] {NumericType.INSTANCE};
        }

        @Override
		public Object execute(Evaluator evaluator, Argument[] arguments) {
            final Object argValue = arguments[0].evaluateScalar(evaluator);
            if (argValue instanceof Number) {
                return ((Number) argValue).doubleValue()
                   + (name.equals("GenericPlusOne") ? 1.0 : -1.0);
            } else {
                // Argument might be a RuntimeException indicating that
                // the cache does not yet have the required cell value. The
                // function will be called again when the cache is loaded.
                return null;
            }
        }
    }

    /**
     * The "TimesString" user-defined function. We wanted a function whose
     * actual return type (string) is not the same as the guessed return type
     * (integer).
     */
    public static class StringMultUdf implements UserDefinedFunction {
        @Override
		public String getName() {
            return "StringMult";
        }

        @Override
		public String getDescription() {
            return "Returns N copies of its string argument";
        }

        @Override
		public Syntax getSyntax() {
            return Syntax.Function;
        }

        @Override
		public Type getReturnType(Type[] parameterTypes) {
            return StringType.INSTANCE;
        }

        @Override
		public Type[] getParameterTypes() {
            return new Type[] {
                NumericType.INSTANCE, StringType.INSTANCE
            };
        }

        @Override
		public Object execute(Evaluator evaluator, Argument[] arguments) {
            final Object argValue = arguments[0].evaluateScalar(evaluator);
            int n;
            if (argValue instanceof Number) {
                n = ((Number) argValue).intValue();
            } else {
                // Argument might be a RuntimeException indicating that
                // the cache does not yet have the required cell value. The
                // function will be called again when the cache is loaded.
                return null;
            }
            String s;
            final Object argValue2 = arguments[1].evaluateScalar(evaluator);
            if (argValue2 instanceof String) {
                s = (String) argValue2;
            } else {
                return null;
            }
            if (n < 0) {
                return null;
            }
            StringBuilder buf = new StringBuilder(s.length() * n);
            for (int i = 0; i < n; i++) {
                buf.append(s);
            }
            return buf.toString();
        }


    }

    /**
     * A user-defined function which returns ignores its first parameter (a
     * member) and returns the default member from the second parameter (a
     * hierarchy).
     */
    public static class AnotherMemberErrorUdf implements UserDefinedFunction {
        @Override
		public String getName() {
            return "AnotherMemberError";
        }

        @Override
		public String getDescription() {
            return "Returns default member from hierarchy, "
                + "specified as a second parameter. "
                + "First parameter - any member from any hierarchy";
        }

        @Override
		public Syntax getSyntax() {
            return Syntax.Function;
        }

        @Override
		public Type getReturnType(Type[] parameterTypes) {
            HierarchyType hierType = (HierarchyType) parameterTypes[1];
            return MemberType.forType(hierType);
        }

        @Override
		public Type[] getParameterTypes() {
            return new Type[] {
                // The first argument must be a member.
                MemberType.Unknown,
                // The second argument must be a hierarchy.
                HierarchyType.Unknown
            };
        }

        @Override
		public Object execute(Evaluator evaluator, Argument[] arguments) {
            // Simply ignore first parameter
            Member member = (Member)arguments[0].evaluate(evaluator);
//            discard(member);
            Hierarchy hierarchy = (Hierarchy)arguments[1].evaluate(evaluator);
            return hierarchy.getDefaultMember();
        }


    }

    /**
     * Function that reverses a list of members.
     */
    public static class ReverseFunction implements UserDefinedFunction {
        @Override
		public Object execute(Evaluator eval, Argument[] args) {
            // Note: must call Argument.evaluateList. If we call
            // Argument.evaluate we may get an Iterable.
            List<?> list = args[0].evaluateList(eval);
            // We do need to copy before we reverse. The list is not guaranteed
            // to be mutable.
            list = new ArrayList(list);
            Collections.reverse(list);
            return list;
        }

        @Override
		public String getDescription() {
            return "Reverses the order of a set";
        }

        @Override
		public String getName() {
            return "Reverse";
        }

        @Override
		public Type[] getParameterTypes() {
            return new Type[] {new SetType(MemberType.Unknown)};
        }



        @Override
		public Type getReturnType(Type[] arg0) {
            return arg0[0];
        }

        @Override
		public Syntax getSyntax() {
            return Syntax.Function;
        }
    }

    /**
     * Function that is non-static.
     */
    public class ReverseFunctionNotStatic extends ReverseFunction {
    }

    /**
     * Function that takes a set of members as argument, and returns a set of
     * members.
     */
    public static class ReverseIterableFunction extends ReverseFunction {
        @Override
		public Object execute(Evaluator eval, Argument[] args) {
            // Note: must call Argument.evaluateList. If we call
            // Argument.evaluate we may get an Iterable.
            Iterable iterable = args[0].evaluateIterable(eval);
            List<Object> list = new ArrayList<>();
            for (Object o : iterable) {
                list.add(o);
            }
            Collections.reverse(list);
            return list;
        }
    }

    /**
     * Function that takes a member and returns a name.
     */
    public static class MemberNameFunction implements UserDefinedFunction {
        @Override
		public Object execute(Evaluator eval, Argument[] args) {
            Member member = (Member) args[0].evaluate(eval);
            return member.getName();
        }

        @Override
		public String getDescription() {
            return "Returns the name of a member";
        }

        @Override
		public String getName() {
            return "MemberName";
        }

        @Override
		public Type[] getParameterTypes() {
            return new Type[] {MemberType.Unknown};
        }

        @Override
		public Type getReturnType(Type[] arg0) {
            return StringType.INSTANCE;
        }

        @Override
		public Syntax getSyntax() {
            return Syntax.Function;
        }
    }

    /**
     * Member formatter for test purposes. Returns name of the member prefixed
     * with "foo" and suffixed with "bar".
     */
    public static class FooBarMemberFormatter implements MemberFormatter {
        @Override
		public String formatMember(Member member) {
            return "foo" + member.getName() + "bar";
        }
    }

    /**
     * Cell formatter for test purposes. Returns value of the cell prefixed
     * with "foo" and suffixed with "bar".
     */
    public static class FooBarCellFormatter implements CellFormatter {
        @Override
		public String formatCell(Object value) {
            return "foo" + value + "bar";
        }
    }

    /**
     * Property formatter for test purposes. Returns name of the member and
     * property, then the value, prefixed with "foo" and suffixed with "bar".
     */
    public static class FooBarPropertyFormatter implements PropertyFormatter {
        @Override
		public String formatProperty(
            Member member, String propertyName, Object propertyValue)
        {
            return "foo" + member.getName() + "/" + propertyName + "/"
                   + propertyValue + "bar";
        }
    }
}
