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
package org.eclipse.daanse.olap.function.def.ancestor;

import static mondrian.olap.Util.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opencube.junit5.TestUtil.assertAxisThrows;
import static org.opencube.junit5.TestUtil.assertExprDependsOn;
import static org.opencube.junit5.TestUtil.executeSingletonAxis;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Member;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

public class AncestorTest {

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAncestor(Context context) {
		Connection con = context.getConnection();
		Member member = TestUtil.executeSingletonAxis(con,
				"Ancestor([Store].[USA].[CA].[Los Angeles],[Store Country])");
		assertEquals("USA", member.getName());

		TestUtil.assertAxisThrows(con, "Ancestor([Store].[USA].[CA].[Los Angeles],[Promotions].[Promotion Name])",
				"Error while executing query");
	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	//
	void testAncestorNumeric(Context context) {
		Connection con = context.getConnection();

		Member member = executeSingletonAxis(con, "Ancestor([Store].[USA].[CA].[Los Angeles],1)");
		assertEquals("CA", member.getName());

		member = executeSingletonAxis(con, "Ancestor([Store].[USA].[CA].[Los Angeles], 0)");
		assertEquals("Los Angeles", member.getName());

		member = executeSingletonAxis(con, "Ancestor([Store].[All Stores].[Vatican], 1)", "[Sales Ragged]");
		assertEquals("All Stores", member.getName());

		member = executeSingletonAxis(con, "Ancestor([Store].[USA].[Washington], 1)", "[Sales Ragged]");
		assertEquals("USA", member.getName());

		// complicated way to say "1".
		member = executeSingletonAxis(con, "Ancestor([Store].[USA].[Washington], 7 * 6 - 41)", "[Sales Ragged]");
		assertEquals("USA", member.getName());

		member = executeSingletonAxis(con, "Ancestor([Store].[All Stores].[Vatican], 2)", "[Sales Ragged]");
		assertNull(member, "Ancestor at 2 must be null");

		member = executeSingletonAxis(con, "Ancestor([Store].[All Stores].[Vatican], -5)", "[Sales Ragged]");
		assertNull(member, "Ancestor at -5 must be null");
	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAncestorHigher(Context context) {
		Member member = executeSingletonAxis(context.getConnection(), "Ancestor([Store].[USA],[Store].[Store City])");
		assertNull(member); // MSOLAP returns null
	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAncestorSameLevel(Context context) {
		Member member = executeSingletonAxis(context.getConnection(),
				"Ancestor([Store].[Canada],[Store].[Store Country])");
		assertEquals("Canada", member.getName());
	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAncestorWrongHierarchy(Context context) {
		// MSOLAP gives error "Formula error - dimensions are not
		// valid (they do not match) - in the Ancestor function"
		assertAxisThrows(context.getConnection(), "Ancestor([Gender].[M],[Store].[Store Country])",
				"Error while executing query");
	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAncestorAllLevel(Context context) {
		Member member = executeSingletonAxis(context.getConnection(), "Ancestor([Store].[USA].[CA],[Store].Levels(0))");
		assertTrue(member.isAll());
	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAncestorWithHiddenParent(Context context) {
		// final Context testContext =
		// getContext().withCube( "[Sales Ragged]" );
		Member member = executeSingletonAxis(context.getConnection(),
				"Ancestor([Store].[All Stores].[Israel].[Haifa], [Store].[Store Country])", "[Sales Ragged]");

		assertNotNull(member, "Member must not be null.");
		assertEquals("Israel", member.getName());
	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAncestorDepends(Context context) {
		Connection con = context.getConnection();
		assertExprDependsOn(con, "Ancestor([Store].CurrentMember, [Store].[Store Country]).Name", "{[Store]}");

		assertExprDependsOn(con, "Ancestor([Store].[All Stores].[USA], [Store].CurrentMember.Level).Name", "{[Store]}");

		assertExprDependsOn(con, "Ancestor([Store].[All Stores].[USA], [Store].[Store Country]).Name", "{}");

		assertExprDependsOn(con, "Ancestor([Store].CurrentMember, 2+1).Name", "{[Store]}");
	}

}
