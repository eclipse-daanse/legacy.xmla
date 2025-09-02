/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara
// All Rights Reserved.
*/
package mondrian.rolap.agg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.ResultSet;
import java.util.Optional;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

class DrillThroughQuerySpecTest {

  // test that returns correct number of columns
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMdxQuery(Context<?> foodMartContext) throws Exception {
    String drillThroughMdx = "DRILLTHROUGH WITH "
        + "SET [*NATIVE_CJ_SET_WITH_SLICER] AS 'NONEMPTYCROSSJOIN([*BASE_MEMBERS__Product_],[*BASE_MEMBERS__Store Type_])' "
        + "SET [*NATIVE_CJ_SET] AS 'GENERATE([*NATIVE_CJ_SET_WITH_SLICER], {([Product].[Product].CURRENTMEMBER)})' "
        + "SET [*BASE_MEMBERS__Store Type_] AS 'FILTER([Store Type].[Store Type].[Store Type].MEMBERS,[Store Type].[Store Type].CURRENTMEMBER "
        + "NOT IN {[Store Type].[Store Type].[All Store Types].[Small Grocery]})' "
        + "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],[Product].[Product].CURRENTMEMBER.ORDERKEY,BASC,ANCESTOR([Product].[Product]"
        + ".CURRENTMEMBER,[Product].[Product].[Product Family]).ORDERKEY,BASC)' "
        + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[Warehouse Cost]}' "
        + "SET [*CJ_SLICER_AXIS] AS 'GENERATE([*NATIVE_CJ_SET_WITH_SLICER], {([Store Type].[Store Type].CURRENTMEMBER)})' "
        + "SET [*BASE_MEMBERS__Product_] AS '[Product].[Product].[Product Department].MEMBERS' "
        + "SET [*CJ_ROW_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Product].[Product].CURRENTMEMBER)})' "
        + "SELECT "
        + "FILTER([*BASE_MEMBERS__Measures_],([Measures].CurrentMember Is [Measures].[Warehouse Cost])) ON COLUMNS "
        + ",FILTER([*SORTED_ROW_AXIS],([Product].[Product].CurrentMember Is [Product].[Product].[Drink].[Alcoholic Beverages])) ON ROWS "
        + "FROM [Warehouse] " + "WHERE ([*CJ_SLICER_AXIS]) "
        + "RETURN [Product].[Product].[Product Department]";

    Connection connection = foodMartContext.getConnectionWithDefaultRole();
    ResultSet resultSet = connection.createStatement().executeQuery(drillThroughMdx, Optional.empty(), null);

    assertEquals(1, resultSet.getMetaData().getColumnCount());
    assertEquals
      ("product_department", resultSet.getMetaData().getColumnName(1));
  }

}
