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
package org.eclipse.daanse.olap.function.def.aggregate.avg;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

public class AvgTest {

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAvg(Context<?> context) {
		TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
				"AVG({[Store].[All Stores].[USA].children})", "88,924");
	}

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAvgNumeric(Context<?> context) {
		TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "Sales",
				"AVG({[Store].[All Stores].[USA].children},[Measures].[Store Sales])", "188,412.71");
	}

	// todo: testAvgWithNulls

}
