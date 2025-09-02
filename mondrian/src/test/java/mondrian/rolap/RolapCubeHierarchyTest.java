/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2001-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.rolap;

import static org.opencube.junit5.TestUtil.assertQueryReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

class RolapCubeHierarchyTest {

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMONDRIAN2535(Context<?> context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
        "Select\n"
        + "  [Customers].children on rows,\n"
        + "  [Gender].children on columns\n "
        + "From [Warehouse and Sales]",
        "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Gender].[Gender].[F]}\n"
        + "{[Gender].[Gender].[M]}\n"
        + "Axis #2:\n"
        + "{[Customers].[Customers].[Canada]}\n"
        + "{[Customers].[Customers].[Mexico]}\n"
        + "{[Customers].[Customers].[USA]}\n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #1: \n"
        + "Row #1: \n"
        + "Row #2: 280,226.21\n"
        + "Row #2: 285,011.92\n");
  }
}
