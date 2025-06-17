/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
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

package mondrian.olap.fun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.CellSet;
import org.eclipse.daanse.olap.api.result.Position;
import org.eclipse.daanse.rolap.function.def.visualtotals.VisualTotalsCalc;
import org.eclipse.daanse.olap.impl.CellImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

/**
 * <code>VisualTotalsTest</code> tests the internal functions defined in
 * {@link VisualTotalsFunDef}. Right now, only tests substitute().
 *
 * @author efine
 */
class VisualTotalsTest {
	@Test
    void testSubstituteEmpty() {
        final String actual = VisualTotalsCalc.substitute("", "anything");
        final String expected = "";
        assertEquals(expected, actual);
    }

	@Test
    void testSubstituteOneStarOnly() {
        final String actual = VisualTotalsCalc.substitute("*", "anything");
        final String expected = "anything";
        assertEquals(expected, actual);
    }

	@Test
    void testSubstituteOneStarBegin() {
        final String actual =
        VisualTotalsCalc.substitute("* is the word.", "Grease");
        final String expected = "Grease is the word.";
        assertEquals(expected, actual);
    }

	@Test
    void testSubstituteOneStarEnd() {
        final String actual =
            VisualTotalsCalc.substitute(
                "Lies, damned lies, and *!", "statistics");
        final String expected = "Lies, damned lies, and statistics!";
        assertEquals(expected, actual);
    }

	@Test
    void testSubstituteTwoStars() {
        final String actual = VisualTotalsCalc.substitute("**", "anything");
        final String expected = "*";
        assertEquals(expected, actual);
    }

	@Test
    void testSubstituteCombined() {
        final String actual =
            VisualTotalsCalc.substitute(
                "*: see small print**** for *", "disclaimer");
        final String expected = "disclaimer: see small print** for disclaimer";
        assertEquals(expected, actual);
    }

    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-925">
     * MONDRIAN-925, "VisualTotals + drillthrough throws Exception"</a>.
     *
     * @throws java.sql.SQLException on error
     */
	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testDrillthroughVisualTotal(Context<?> foodMartContext) throws SQLException {
        Connection conn = foodMartContext.getConnectionWithDefaultRole();
        CellSet cellSet =
    		TestUtil.executeQueryWithCellSetResult(conn,
                "select {[Measures].[Unit Sales]} on columns, "
                + "{VisualTotals("
                + "    {[Product].[Food].[Baked Goods].[Bread],"
                + "     [Product].[Food].[Baked Goods].[Bread].[Bagels],"
                + "     [Product].[Food].[Baked Goods].[Bread].[Muffins]},"
                + "     \"**Subtotal - *\")} on rows "
                + "from [Sales]");
        List<Position> positions = cellSet.getAxes().get(1).getPositions();
        Cell cell;
        ResultSet resultSet;
        Member member;

        cell = cellSet.getCell(Arrays.asList(0, 0));
        member = positions.get(0).getMembers().get(0);
        assertEquals("*Subtotal - Bread", member.getCaption());
        resultSet = ((CellImpl)cell).drillThrough();
        assertNull(resultSet);

        cell = cellSet.getCell(Arrays.asList(0, 1));
        member = positions.get(1).getMembers().get(0);
        assertEquals("Bagels", member.getName());
        resultSet = ((CellImpl)cell).drillThrough();
        assertNotNull(resultSet);
        resultSet.close();
    }

    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-1279">
     * MONDRIAN-1279, "VisualTotals name only applies to member name not
     * caption"</a>.
     *
     * @throws java.sql.SQLException on error
     */
	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testVisualTotalCaptionBug(Context<?> foodMartContext) throws SQLException {
        CellSet cellSet =
    		TestUtil.executeQueryWithCellSetResult(foodMartContext.getConnectionWithDefaultRole(),
                "select {[Measures].[Unit Sales]} on columns, "
                + "VisualTotals("
                + "    {[Product].[Food].[Baked Goods].[Bread],"
                + "     [Product].[Food].[Baked Goods].[Bread].[Bagels],"
                + "     [Product].[Food].[Baked Goods].[Bread].[Muffins]},"
                + "     \"**Subtotal - *\") on rows "
                + "from [Sales]");
        List<Position> positions = cellSet.getAxes().get(1).getPositions();
        Cell cell;
        Member member;

        cell = cellSet.getCell(Arrays.asList(0, 0));
        member = positions.get(0).getMembers().get(0);
        assertEquals("Bread", member.getName());
        assertEquals("*Subtotal - Bread", member.getCaption());
    }

    /**
     * Test case for bug <a href="http://jira.pentaho.com/browse/MONDRIAN-939">
     * MONDRIAN-939, "VisualTotals returning incorrect values with aggregate members"</a>.
     *
     * @throws java.sql.SQLException on error
     */
	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testVisualTotalsAggregatedMemberBug(Context<?> foodMartContext) throws SQLException {
        CellSet cellSet =
    		TestUtil.executeQueryWithCellSetResult(foodMartContext.getConnectionWithDefaultRole(),
                " with  member [Gender].[YTD] as 'AGGREGATE(YTD(),[Gender].[M])'"
            	+ "  select "
            	+ " {[Time].[1997],"
            	+ " [Time].[1997].[Q1],[Time].[1997].[Q2],[Time].[1997].[Q3],[Time].[1997].[Q4]} ON COLUMNS, "
            	+ " {[Gender].[M],[Gender].[YTD]} ON ROWS"
            	+ " FROM [Sales]");
        //fail("Not yet implemented");
        String s = TestUtil.toString(cellSet);
        TestUtil.assertEqualsVerbose(
        	     "Axis #0:\n"
        	     + "{}\n"
        	     + "Axis #1:\n"
        	     + "{[Time].[Time].[1997]}\n"
        	     + "{[Time].[Time].[1997].[Q1]}\n"
        	     + "{[Time].[Time].[1997].[Q2]}\n"
        	     + "{[Time].[Time].[1997].[Q3]}\n"
        	     + "{[Time].[Time].[1997].[Q4]}\n"
        	     + "Axis #2:\n"
        	     + "{[Gender].[Gender].[M]}\n"
        	     + "{[Gender].[Gender].[YTD]}\n"
        	     + "Row #0: 135,215\n"
        	     + "Row #0: 33,381\n"
        	     + "Row #0: 31,618\n"
        	     + "Row #0: 33,249\n"
        	     + "Row #0: 36,967\n"
        	     + "Row #1: 135,215\n"
        	     + "Row #1: 33,381\n"
        	     + "Row #1: 64,999\n"
        	     + "Row #1: 98,248\n"
        	     + "Row #1: 135,215\n"
        		,s);
    }

}
