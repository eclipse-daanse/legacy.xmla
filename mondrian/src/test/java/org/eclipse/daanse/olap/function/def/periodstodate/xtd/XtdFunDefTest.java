/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 *
 * For more information please visit the Project: Hitachi Vantara - Mondrian
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
package org.eclipse.daanse.olap.function.def.periodstodate.xtd;

import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertAxisThrows;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertSetExprDependsOn;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.fun.FunctionTest;

public class XtdFunDefTest {

	private static final String TimeWeekly = "[Time].[Weekly]";

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testYtd(Context<?> context) {

		assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales", "Ytd()", "[Time].[Time].[1997]");

		assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales", "Ytd([Time].[1997].[Q3])", """
				[Time].[Time].[1997].[Q1]
				[Time].[Time].[1997].[Q2]
				[Time].[Time].[1997].[Q3]""");

		assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales", "Ytd([Time].[1997].[Q2].[4])", """
				[Time].[Time].[1997].[Q1].[1]
				[Time].[Time].[1997].[Q1].[2]
				[Time].[Time].[1997].[Q1].[3]
				[Time].[Time].[1997].[Q2].[4]""");

		assertAxisThrows(context.getConnectionWithDefaultRole(), "Ytd([Store])",
				"Argument to function 'Ytd' must belong to Time hierarchy", "Sales");

		assertSetExprDependsOn(context.getConnectionWithDefaultRole(), "Ytd()", "{[Time].[Time], " + TimeWeekly + "}");

		assertSetExprDependsOn(context.getConnectionWithDefaultRole(), "Ytd([Time].[1997].[Q2])", "{}");
	}

	/**
	 * Testcase for <a href="http://jira.pentaho.com/browse/MONDRIAN-458"> bug
	 * MONDRIAN-458, "error deducing type of Ytd/Qtd/Mtd functions within
	 * Generate"</a>.
	 */
	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testGeneratePlusXtd(Context<?> context) {

		assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales", """
				generate(
				  {[Time].[1997].[Q1].[2], [Time].[1997].[Q3].[7]},
				 {Ytd( [Time].[Time].currentMember)})""", """
				[Time].[Time].[1997].[Q1].[1]
				[Time].[Time].[1997].[Q1].[2]
				[Time].[Time].[1997].[Q1].[3]
				[Time].[Time].[1997].[Q2].[4]
				[Time].[Time].[1997].[Q2].[5]
				[Time].[Time].[1997].[Q2].[6]
				[Time].[Time].[1997].[Q3].[7]""");

		assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales", """
				generate(
				  {[Time].[1997].[Q1].[2], [Time].[1997].[Q3].[7]},
				 {Ytd( [Time].[Time].currentMember)}, ALL)""", """
				[Time].[Time].[1997].[Q1].[1]
				[Time].[Time].[1997].[Q1].[2]
				[Time].[Time].[1997].[Q1].[1]
				[Time].[Time].[1997].[Q1].[2]
				[Time].[Time].[1997].[Q1].[3]
				[Time].[Time].[1997].[Q2].[4]
				[Time].[Time].[1997].[Q2].[5]
				[Time].[Time].[1997].[Q2].[6]
				[Time].[Time].[1997].[Q3].[7]""");

		FunctionTest.assertExprReturns(context.getConnectionWithDefaultRole(),
				"count(generate({[Time].[1997].[Q4].[11]}, {Qtd( [Time].[Time].currentMember)}))", 2, 0);

		FunctionTest.assertExprReturns(context.getConnectionWithDefaultRole(),
				"count(generate({[Time].[1997].[Q4].[11]}, {Mtd( [Time].[Time].currentMember)}))", 1, 0);
	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testQtd(Context<?> context) {
		// zero args
		assertQueryReturns(context.getConnectionWithDefaultRole(), """
				with member [Measures].[Foo] as ' SetToStr(Qtd()) '
				select {[Measures].[Foo]} on columns
				from [Sales]
				where [Time].[1997].[Q2].[5]""", """
				Axis #0:
				{[Time].[Time].[1997].[Q2].[5]}
				Axis #1:
				{[Measures].[Foo]}
				Row #0: {[Time].[Time].[1997].[Q2].[4], [Time].[Time].[1997].[Q2].[5]}
				""");

		// one arg, a month
		assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales", "Qtd([Time].[1997].[Q2].[5])",
				"[Time].[Time].[1997].[Q2].[4]\n" + "[Time].[Time].[1997].[Q2].[5]");

		// one arg, a quarter
		assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales", "Qtd([Time].[1997].[Q2])", "[Time].[Time].[1997].[Q2]");

		// one arg, a year
		assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales", "Qtd([Time].[1997])", "");

		assertAxisThrows(context.getConnectionWithDefaultRole(), "Qtd([Store])",
				"Argument to function 'Qtd' must belong to Time hierarchy", "Sales");
	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testMtd(Context<?> context) {
		// zero args
		assertQueryReturns(context.getConnectionWithDefaultRole(), """
				with member [Measures].[Foo] as ' SetToStr(Mtd()) '
				select {[Measures].[Foo]} on columns
				from [Sales]
				where [Time].[1997].[Q2].[5]""", """
				Axis #0:
				{[Time].[Time].[1997].[Q2].[5]}
				Axis #1:
				{[Measures].[Foo]}
				Row #0: {[Time].[Time].[1997].[Q2].[5]}
				""");

		// one arg, a month
		assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales", "Mtd([Time].[1997].[Q2].[5])", "[Time].[Time].[1997].[Q2].[5]");

		// one arg, a quarter
		assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales", "Mtd([Time].[1997].[Q2])", "");

		// one arg, a year
		assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales", "Mtd([Time].[1997])", "");

		assertAxisThrows(context.getConnectionWithDefaultRole(), "Mtd([Store])",
				"Argument to function 'Mtd' must belong to Time hierarchy", "Sales");
	}

}
