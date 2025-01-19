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
package org.eclipse.daanse.olap.function.def.as;

import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertQueryThrows;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.util.Bug;

public class AsAliasFunDefTest {

	/**
	 * Tests the AS operator, that gives an expression an alias.
	 */
	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAs(Context context) {
		assertAxisReturns(context.getConnectionWithDefaultRole(), "Filter([Customers].Children as t,\n" + "t.Current.Name = 'USA')",
				"[Customers].[USA]");
	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAsWithColon(Context context) {
		// 'AS' and the ':' operator have similar precedence, so it's worth
		// checking that they play nice.
		assertQueryReturns(context.getConnectionWithDefaultRole(), """
				select
				  filter(
				    [Time].[1997].[Q1].[2] : [Time].[1997].[Q3].[9] as t,\
				    mod(t.CurrentOrdinal, 2) = 0) on 0
				from [Sales]""", """
				Axis #0:
				{}
				Axis #1:
				{[Time].[1997].[Q1].[2]}
				{[Time].[1997].[Q2].[4]}
				{[Time].[1997].[Q2].[6]}
				{[Time].[1997].[Q3].[8]}
				Row #0: 20,957
				Row #0: 20,179
				Row #0: 21,350
				Row #0: 21,697
				""");
	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAsFailsMember(Context context) {
		// AS member fails on SSAS with "The CHILDREN function expects a member
		// expression for the 0 argument. A tuple set expression was used."
		assertQueryThrows(context, """
				select
				 {([Time].[1997].[Q1] as t).Children,\s
				  t.Parent } on 0\s
				from [Sales]""", "No function matches signature '<Set>.Children'");

	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAsWithSetOfMembers(Context context) {
		// Set of members. OK.
		assertQueryReturns(context.getConnectionWithDefaultRole(), """
				select Measures.[Unit Sales] on 0,\s
				  {[Time].[1997].Children as t,\s
				   Descendants(t, [Time].[Month])} on 1\s
				from [Sales]""", """
				Axis #0:
				{}
				Axis #1:
				{[Measures].[Unit Sales]}
				Axis #2:
				{[Time].[1997].[Q1]}
				{[Time].[1997].[Q2]}
				{[Time].[1997].[Q3]}
				{[Time].[1997].[Q4]}
				{[Time].[1997].[Q1].[1]}
				{[Time].[1997].[Q1].[2]}
				{[Time].[1997].[Q1].[3]}
				{[Time].[1997].[Q2].[4]}
				{[Time].[1997].[Q2].[5]}
				{[Time].[1997].[Q2].[6]}
				{[Time].[1997].[Q3].[7]}
				{[Time].[1997].[Q3].[8]}
				{[Time].[1997].[Q3].[9]}
				{[Time].[1997].[Q4].[10]}
				{[Time].[1997].[Q4].[11]}
				{[Time].[1997].[Q4].[12]}
				Row #0: 66,291
				Row #1: 62,610
				Row #2: 65,848
				Row #3: 72,024
				Row #4: 21,628
				Row #5: 20,957
				Row #6: 23,706
				Row #7: 20,179
				Row #8: 21,081
				Row #9: 21,350
				Row #10: 23,763
				Row #11: 21,697
				Row #12: 20,388
				Row #13: 19,958
				Row #14: 25,270
				Row #15: 26,796
				""");

	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAsWithAliasMemberImplicitSet(Context context) {
		// Alias a member. Implicitly becomes set. OK.
		assertQueryReturns(context.getConnectionWithDefaultRole(), """
				select Measures.[Unit Sales] on 0,
				  {[Time].[1997] as t,
				   Descendants(t, [Time].[Month])} on 1
				from [Sales]""", """
				Axis #0:
				{}
				Axis #1:
				{[Measures].[Unit Sales]}
				Axis #2:
				{[Time].[1997]}
				{[Time].[1997].[Q1].[1]}
				{[Time].[1997].[Q1].[2]}
				{[Time].[1997].[Q1].[3]}
				{[Time].[1997].[Q2].[4]}
				{[Time].[1997].[Q2].[5]}
				{[Time].[1997].[Q2].[6]}
				{[Time].[1997].[Q3].[7]}
				{[Time].[1997].[Q3].[8]}
				{[Time].[1997].[Q3].[9]}
				{[Time].[1997].[Q4].[10]}
				{[Time].[1997].[Q4].[11]}
				{[Time].[1997].[Q4].[12]}
				Row #0: 266,773
				Row #1: 21,628
				Row #2: 20,957
				Row #3: 23,706
				Row #4: 20,179
				Row #5: 21,081
				Row #6: 21,350
				Row #7: 23,763
				Row #8: 21,697
				Row #9: 20,388
				Row #10: 19,958
				Row #11: 25,270
				Row #12: 26,796
				""");

	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAsWithFailOnAliasTuple(Context context) {
		// Alias a tuple. Implicitly becomes set. The error confirms that the
		// named set's type is a set of tuples. SSAS gives error "Descendants
		// function expects a member or set ..."
		assertQueryThrows(context, """
				select Measures.[Unit Sales] on 0,
				  {([Time].[1997], [Customers].[USA].[CA]) as t,
				   Descendants(t, [Time].[Month])} on 1
				from [Sales]""",
				"Argument to Descendants function must be a member or set of members, not a set of tuples");
	}

	final String result = """
			Axis #0:
			{}
			Axis #1:
			{[Measures].[Unit Sales], [Gender].[F]}
			{[Measures].[Unit Sales], [Gender].[M]}
			Axis #2:
			{[Time].[1997].[Q1]}
			{[Time].[1997].[Q2]}
			{[Time].[1997].[Q3]}
			{[Time].[1997].[Q4]}
			{[Time].[1997].[Q1].[1]}
			{[Time].[1997].[Q2].[6]}
			{[Time].[1997].[Q4].[11]}
			Row #0: 32,910
			Row #0: 33,381
			Row #1: 30,992
			Row #1: 31,618
			Row #2: 32,599
			Row #2: 33,249
			Row #3: 35,057
			Row #3: 36,967
			Row #4: 10,932
			Row #4: 10,696
			Row #5: 10,466
			Row #5: 10,884
			Row #6: 12,320
			Row #6: 12,950
			""";

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAs2(Context context) {
		// Named set and alias with same name (t) and a second alias (t2).
		// Reference to t from within descendants resolves to alias, of type
		// [Time], because it is nearer.
		assertQueryReturns(context.getConnectionWithDefaultRole(), """
				with set t as [Gender].Children
				select
				  Measures.[Unit Sales] * t on 0,
				  {
				    [Time].[1997].Children as t,
				    Filter(
				      Descendants(t, [Time].[Month]) as t2,
				      Mod(t2.CurrentOrdinal, 5) = 0)
				  } on 1
				from [Sales]""", result);

	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAsWith2AliasesOfSameName(Context context) {
		// Two aliases with same name. OK.
		assertQueryReturns(context.getConnectionWithDefaultRole(), """
				select
				  Measures.[Unit Sales] * [Gender].Children as t on 0,
				  {[Time].[1997].Children as t,
				    Filter(
				      Descendants(t, [Time].[Month]) as t2,
				      Mod(t2.CurrentOrdinal, 5) = 0)
				  } on 1
				from [Sales]""", result);

	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAsWithAsterics(Context context) {
		// Bug MONDRIAN-648 causes 'AS' to have lower precedence than '*'.
		if (Bug.BugMondrian648Fixed) {
			// Note that 'as' has higher precedence than '*'.
			assertQueryReturns(context.getConnectionWithDefaultRole(), """
					select
					  Measures.[Unit Sales] * [Gender].Members as t on 0,
					  {t} on 1
					from [Sales]""", "xxxxx");
		}
	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAsWithFailingReferenceTiOtherHierarchy(Context context) {
		// Reference to hierarchy on other axis.
		// On SSAS 2005, finds t, and gives error,
		// "The Gender hierarchy already appears in the Axis0 axis."
		// On Mondrian, cannot find t. FIXME.
		assertQueryThrows(context, """
				select
				  Measures.[Unit Sales] * ([Gender].Members as t) on 0,
				  {t} on 1
				from [Sales]""", "MDX object 't' not found in cube 'Sales'");

		// As above, with parentheses. Tuple valued.
		// On SSAS 2005, finds t, and gives error,
		// "The Measures hierarchy already appears in the Axis0 axis."
		// On Mondrian, cannot find t. FIXME.
		assertQueryThrows(context, """
				select
				  (Measures.[Unit Sales] * [Gender].Members) as t on 0,
				  {t} on 1
				from [Sales]""", "MDX object 't' not found in cube 'Sales'");

	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAsWithCalcSetCurrMember(Context context) {
		// Calculated set, CurrentMember
		assertQueryReturns(context.getConnectionWithDefaultRole(), """
				select Measures.[Unit Sales] on 0,
				  filter(
				    (Time.Month.Members * Gender.Members) as s,
				    (s.Current.Item(0).Parent, [Marital Status].[S], [Gender].[F]) > 17000) on 1
				from [Sales]""", """
				Axis #0:
				{}
				Axis #1:
				{[Measures].[Unit Sales]}
				Axis #2:
				{[Time].[1997].[Q4].[10], [Gender].[All Gender]}
				{[Time].[1997].[Q4].[10], [Gender].[F]}
				{[Time].[1997].[Q4].[10], [Gender].[M]}
				{[Time].[1997].[Q4].[11], [Gender].[All Gender]}
				{[Time].[1997].[Q4].[11], [Gender].[F]}
				{[Time].[1997].[Q4].[11], [Gender].[M]}
				{[Time].[1997].[Q4].[12], [Gender].[All Gender]}
				{[Time].[1997].[Q4].[12], [Gender].[F]}
				{[Time].[1997].[Q4].[12], [Gender].[M]}
				Row #0: 19,958
				Row #1: 9,506
				Row #2: 10,452
				Row #3: 25,270
				Row #4: 12,320
				Row #5: 12,950
				Row #6: 26,796
				Row #7: 13,231
				Row #8: 13,565
				""");

		// As above, but don't override [Gender] in filter condition. Note that
		// the filter condition is evaluated in the context created by the
		// filter set. So, only items with [All Gender] pass the filter.
		assertQueryReturns(context.getConnectionWithDefaultRole(), """
				select Measures.[Unit Sales] on 0,
				  filter(
				    (Time.Month.Members * Gender.Members) as s,
				    (s.Current.Item(0).Parent, [Marital Status].[S]) > 35000) on 1
				from [Sales]""", """
				Axis #0:
				{}
				Axis #1:
				{[Measures].[Unit Sales]}
				Axis #2:
				{[Time].[1997].[Q4].[10], [Gender].[All Gender]}
				{[Time].[1997].[Q4].[11], [Gender].[All Gender]}
				{[Time].[1997].[Q4].[12], [Gender].[All Gender]}
				Row #0: 19,958
				Row #1: 25,270
				Row #2: 26,796
				""");

	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAsWithMultiDefOfAliasInSameAxis(Context context) {
		// Multiple definitions of alias within same axis
		assertQueryReturns(context.getConnectionWithDefaultRole(), """
				select Measures.[Unit Sales] on 0,
				  generate(
				    [Marital Status].Children as s,
				    filter(
				      (Time.Month.Members * Gender.Members) as s,
				      (s.Current.Item(0).Parent, [Marital Status].[S], [Gender].[F]) > 17000),
				    ALL) on 1
				from [Sales]""", """
				Axis #0:
				{}
				Axis #1:
				{[Measures].[Unit Sales]}
				Axis #2:
				{[Time].[1997].[Q4].[10], [Gender].[All Gender]}
				{[Time].[1997].[Q4].[10], [Gender].[F]}
				{[Time].[1997].[Q4].[10], [Gender].[M]}
				{[Time].[1997].[Q4].[11], [Gender].[All Gender]}
				{[Time].[1997].[Q4].[11], [Gender].[F]}
				{[Time].[1997].[Q4].[11], [Gender].[M]}
				{[Time].[1997].[Q4].[12], [Gender].[All Gender]}
				{[Time].[1997].[Q4].[12], [Gender].[F]}
				{[Time].[1997].[Q4].[12], [Gender].[M]}
				{[Time].[1997].[Q4].[10], [Gender].[All Gender]}
				{[Time].[1997].[Q4].[10], [Gender].[F]}
				{[Time].[1997].[Q4].[10], [Gender].[M]}
				{[Time].[1997].[Q4].[11], [Gender].[All Gender]}
				{[Time].[1997].[Q4].[11], [Gender].[F]}
				{[Time].[1997].[Q4].[11], [Gender].[M]}
				{[Time].[1997].[Q4].[12], [Gender].[All Gender]}
				{[Time].[1997].[Q4].[12], [Gender].[F]}
				{[Time].[1997].[Q4].[12], [Gender].[M]}
				Row #0: 19,958
				Row #1: 9,506
				Row #2: 10,452
				Row #3: 25,270
				Row #4: 12,320
				Row #5: 12,950
				Row #6: 26,796
				Row #7: 13,231
				Row #8: 13,565
				Row #9: 19,958
				Row #10: 9,506
				Row #11: 10,452
				Row #12: 25,270
				Row #13: 12,320
				Row #14: 12,950
				Row #15: 26,796
				Row #16: 13,231
				Row #17: 13,565
				""");

		// Multiple definitions of alias within same axis.
		//
		// On SSAS 2005, gives error, "The CURRENT function cannot be called in
		// current context because the 'x' set is not in scope". SSAS 2005 gives
		// same error even if set does not exist.
		assertQueryThrows(context, """
				with member Measures.Foo as 'x.Current.Name'
				select
				  {Measures.[Unit Sales], Measures.Foo} on 0,
				  generate(
				    [Marital Status].
				    Children as x,
				    filter(
				      Gender.Members as x,
				      (x.Current, [Marital Status].[S]) > 50000),
				    ALL) on 1
				from [Sales]""", "MDX object 'x' not found in cube 'Sales'");

		// As above, but set is not out of scope; it does not exist; but error
		// should be the same.
		assertQueryThrows(context, """
				with member Measures.Foo as 'z.Current.Name'
				select
				  {Measures.[Unit Sales], Measures.Foo} on 0,
				  generate(
				    [Marital Status].
				    Children as s,
				    filter(
				      Gender.Members as s,
				      (s.Current, [Marital Status].[S]) > 50000),
				    ALL) on 1
				from [Sales]""", "MDX object 'z' not found in cube 'Sales'");

	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAsWithFailingSetAsString(Context context) {
		// 'set AS string' is invalid
		assertQueryThrows(context.getConnectionWithDefaultRole(), """
				select Measures.[Unit Sales] on 0,
				  filter(
				    (Time.Month.Members * Gender.Members) as 'foo',
				    (s.Current.Item(0).Parent, [Marital Status].[S]) > 50000) on 1
				from [Sales]""", "Encountered an error at (or somewhere around) input:3:46");

	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAsWithFailingSetAsNumeric(Context context) {
		// 'set AS numeric' is invalid
		assertQueryThrows(context.getConnectionWithDefaultRole(), """
				select Measures.[Unit Sales] on 0,
				  filter(
				    (Time.Month.Members * Gender.Members) as 1234,
				    (s.Current.Item(0).Parent, [Marital Status].[S]) > 50000) on 1
				from [Sales]""", "Encountered an error at (or somewhere around) input:3:46");

	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAsWithFailingNumericAsIdentifier(Context context) {
		// 'numeric AS identifier' is invalid
		assertQueryThrows(context.getConnectionWithDefaultRole(), """
				select Measures.[Unit Sales] on 0,
				  filter(
				    123 * 456 as s,
				    (s.Current.Item(0).Parent, [Marital Status].[S]) > 50000) on 1
				from [Sales]""", "No function matches signature '<Numeric Expression> AS <Set>'");
	}

}
