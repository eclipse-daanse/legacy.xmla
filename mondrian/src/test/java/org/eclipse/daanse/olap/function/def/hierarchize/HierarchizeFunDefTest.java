/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.function.def.hierarchize;

import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.withSchema;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.rolap.SchemaModifiers;


class HierarchizeFunDefTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testHierarchize(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(),
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
        assertAxisReturns(context.getConnectionWithDefaultRole(),
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
        assertAxisReturns(context.getConnectionWithDefaultRole(), "HR",
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
        assertAxisReturns(context.getConnectionWithDefaultRole(),
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
        assertAxisReturns(context.getConnectionWithDefaultRole(),
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
        final Connection connection = context.getConnectionWithDefaultRole();

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

}
