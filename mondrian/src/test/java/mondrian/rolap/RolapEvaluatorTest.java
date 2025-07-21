/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2021 Hitachi Vantara.
// All Rights Reserved.
*/
package mondrian.rolap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencube.junit5.TestUtil.executeQuery;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.rolap.common.CompoundPredicateInfo;
import org.eclipse.daanse.rolap.common.RolapEvaluator;
import org.eclipse.daanse.rolap.common.RolapResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

class RolapEvaluatorTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testGetSlicerPredicateInfo(Context<?> context) throws Exception {
        RolapResult result = (RolapResult) executeQuery(context.getConnectionWithDefaultRole(),
            "select  from sales "
            + "WHERE {[Time].[1997].Q1, [Time].[1997].Q2} "
            + "* { Store.[USA].[CA], Store.[USA].[WA]}");
        RolapEvaluator evalulator = (RolapEvaluator) result.getRootEvaluator();
        final CompoundPredicateInfo slicerPredicateInfo =
            evalulator.getSlicerPredicateInfo();
        assertEquals(
            "(((`store`.`store_state`, `time_by_day`.`the_year`, `time_by_day`.`quarter`) in "
            + "(('CA', 1997, 'Q1'), ('WA', 1997, 'Q1'), ('CA', 1997, 'Q2'), ('WA', 1997, 'Q2'))))",
            slicerPredicateInfo.getPredicateString());
        assertTrue(slicerPredicateInfo.isSatisfiable());
    }

    /*
    void testSlicerPredicateUnsatisfiable() {
        assertQueryReturns(
            "select measures.[Customer Count] on 0 from [warehouse and sales] "
            + "WHERE {[Time].[1997].Q1, [Time].[1997].Q2} "
            + "*{[Warehouse].[USA].[CA], Warehouse.[USA].[WA]}", "");
        RolapResult result = (RolapResult) executeQuery(
            "select  from [warehouse and sales] "
            + "WHERE {[Time].[1997].Q1, [Time].[1997].Q2} "
            + "* Head([Warehouse].[Country].members, 2)");
        RolapEvaluator evalulator = (RolapEvaluator) result.getRootEvaluator();
        assertFalse(evalulator.getSlicerPredicateInfo().isSatisfiable());
        assertNull(evalulator.getSlicerPredicateInfo().getPredicate());
    }
    */

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testListColumnPredicateInfo(Context<?> context) throws Exception {
      RolapResult result = (RolapResult) executeQuery(context.getConnectionWithDefaultRole(),
          "select  from sales "
          + "WHERE {[Product].[Drink],[Product].[Non-Consumable]} ");
      RolapEvaluator evalulator = (RolapEvaluator) result.getRootEvaluator();
      final CompoundPredicateInfo slicerPredicateInfo =
          evalulator.getSlicerPredicateInfo();
      assertEquals(
          "`product_class`.`product_family` in ('Drink', 'Non-Consumable')",
          slicerPredicateInfo.getPredicateString());
      assertTrue(slicerPredicateInfo.isSatisfiable());
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testOrPredicateInfo(Context<?> context) throws Exception {
      RolapResult result = (RolapResult) executeQuery(context.getConnectionWithDefaultRole(),
          "select  from sales "
          + "WHERE {[Product].[Drink].[Beverages],[Product].[Food].[Produce],[Product].[Non-Consumable]} ");
      RolapEvaluator evalulator = (RolapEvaluator) result.getRootEvaluator();
      final CompoundPredicateInfo slicerPredicateInfo =
          evalulator.getSlicerPredicateInfo();
      assertEquals(
          "((((`product_class`.`product_family`, `product_class`.`product_department`) in "
        + "(('Drink', 'Beverages'), ('Food', 'Produce')))) or `product_class`.`product_family` = 'Non-Consumable')",
          slicerPredicateInfo.getPredicateString());
      assertTrue(slicerPredicateInfo.isSatisfiable());
    }

}
