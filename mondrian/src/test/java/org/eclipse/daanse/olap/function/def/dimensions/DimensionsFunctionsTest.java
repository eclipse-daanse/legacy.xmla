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
package org.eclipse.daanse.olap.function.def.dimensions;

import static org.opencube.junit5.TestUtil.assertAxisReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.fun.FunctionTest;

public class DimensionsFunctionsTest {

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testDimensionsNumeric(Context<?> context) {
		TestUtil.assertExprDependsOn(context.getConnectionWithDefaultRole(), "Dimensions(2).Name", "{}");
		TestUtil.assertMemberExprDependsOn(context.getConnectionWithDefaultRole(), "Dimensions(3).CurrentMember",
				FunctionTest.allHiers());
		TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales", "Dimensions(2).Name", "Store Size in SQFT");
		// bug 1426134 -- Dimensions(0) throws 'Index '0' out of bounds'
		TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales", "Dimensions(0).Name", "Measures");
		TestUtil.assertExprThrows(context.getConnectionWithDefaultRole(), "Sales", "Dimensions(-1).Name", "Index '-1' out of bounds");
		TestUtil.assertExprThrows(context.getConnectionWithDefaultRole(), "Sales", "Dimensions(100).Name", "Index '100' out of bounds");
		// Since Dimensions returns a Hierarchy, can apply CurrentMember.
		assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales", "Dimensions(3).CurrentMember", "[Store Type].[Store Type].[All Store Types]");
	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testDimensionsString(Context<?> context) {
		TestUtil.assertExprDependsOn(context.getConnectionWithDefaultRole(), "Dimensions(\"foo\").UniqueName", "{}");
		TestUtil.assertMemberExprDependsOn(context.getConnectionWithDefaultRole(), "Dimensions(\"foo\").CurrentMember",
				FunctionTest.allHiers());
		TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales", "Dimensions(\"Store\").UniqueName", "[Store].[Store]");
		// Since Dimensions returns a Hierarchy, can apply Children.
		assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales", "Dimensions(\"Store\").Children", """
				[Store].[Store].[Canada]
				[Store].[Store].[Mexico]
				[Store].[Store].[USA]""");
	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testDimensionsDepends(Context<?> context) {
		final String expression = """
				Crossjoin(
				{Dimensions("Measures").CurrentMember.Hierarchy.CurrentMember},
				{Dimensions("Product")})""";
		assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales", expression, "{[Measures].[Unit Sales], [Product].[Product].[All Products]}");
		TestUtil.assertSetExprDependsOn(context.getConnectionWithDefaultRole(), expression, FunctionTest.allHiers());
	}

}
