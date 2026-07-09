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
import org.eclipse.daanse.rolap.common.agg.CompoundPredicateInfo;
import org.eclipse.daanse.rolap.common.evaluator.RolapEvaluator;
import org.eclipse.daanse.rolap.common.result.RolapResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

class RolapEvaluatorTest {

    /** The slicer predicate string with identifier quoting stripped — the structural pin stays
     *  (columns, tuples, values, operators) while the assertion works on every dialect (the
     *  string is rendered with the star's LIVE dialect: backticks on MySQL, double quotes on H2,
     *  brackets on MSSQL). */
    private static String unquoted(CompoundPredicateInfo info) {
        return info.getPredicateString().replaceAll("[`\"\\[\\]]", "");
    }

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
        // SEMANTIC pin (the compound STRUCTURE legitimately differs per dialect: tuple-IN on
        // multi-value-IN dialects, OR-expansion elsewhere): every constrained column and every
        // slicer value must appear; satisfiability must hold.
        String p1 = unquoted(slicerPredicateInfo);
        for (String required : new String[] {"store.store_state", "time_by_day.the_year",
                "time_by_day.quarter", "'CA'", "'WA'", "1997", "'Q1'", "'Q2'"}) {
            org.junit.jupiter.api.Assertions.assertTrue(p1.contains(required),
                "missing " + required + " in: " + p1);
        }
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
          "product_class.product_family in ('Drink', 'Non-Consumable')",
          unquoted(slicerPredicateInfo));
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
      // SEMANTIC pin — see testGetSlicerPredicateInfo.
      String p3 = unquoted(slicerPredicateInfo);
      for (String required : new String[] {"product_class.product_family",
              "product_class.product_department", "'Drink'", "'Beverages'", "'Food'",
              "'Produce'", "'Non-Consumable'"}) {
          org.junit.jupiter.api.Assertions.assertTrue(p3.contains(required),
              "missing " + required + " in: " + p3);
      }
      assertTrue(slicerPredicateInfo.isSatisfiable());
    }

}
