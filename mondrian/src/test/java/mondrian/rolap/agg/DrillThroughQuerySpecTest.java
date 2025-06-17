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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.jdbc.db.dialect.api.BestFitColumnType;
import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.rolap.RolapStar;
import mondrian.rolap.StarPredicate;
import mondrian.rolap.sql.SqlQuery;

class DrillThroughQuerySpecTest {

  private static DrillThroughCellRequest requestMock;
  private static StarPredicate starPredicateMock;
  private static SqlQuery sqlQueryMock;
  private static DrillThroughQuerySpec drillThroughQuerySpec;
  private static RolapStar.Column includedColumn;
  private static RolapStar.Column excludedColumn;

  @BeforeEach
  public void beforeAll() throws Exception {

    requestMock = mock(DrillThroughCellRequest.class);
    starPredicateMock = mock(StarPredicate.class);
    sqlQueryMock = mock(SqlQuery.class);
    RolapStar.Measure measureMock = mock(RolapStar.Measure.class);
    includedColumn = mock(RolapStar.Column.class);
    excludedColumn = mock(RolapStar.Column.class);
    RolapStar starMock = mock(RolapStar.class);

    when(requestMock.includeInSelect(any(RolapStar.Column.class)))
      .thenReturn(true);
    when(requestMock.getMeasure()).thenReturn(measureMock);
    when(requestMock.getConstrainedColumns())
      .thenReturn(new RolapStar.Column[0]);
    when(measureMock.getStar()).thenReturn(starMock);
    when(starMock.getSqlQueryDialect()).thenReturn(mock(Dialect.class));
    when(starPredicateMock.getConstrainedColumnList())
      .thenReturn(Collections.singletonList(includedColumn));
    when(includedColumn.getTable()).thenReturn(mock(RolapStar.Table.class));
    when(excludedColumn.getTable()).thenReturn(mock(RolapStar.Table.class));
    drillThroughQuerySpec =
      new DrillThroughQuerySpec
        (requestMock, starPredicateMock, new ArrayList<OlapElement> (), false);
  }

  @Test
  void testEmptyColumns() {
    List<RolapStar.Column> columns = Collections.emptyList();
    when(starPredicateMock.getConstrainedColumnList())
      .thenReturn(columns);
    drillThroughQuerySpec.extraPredicates(sqlQueryMock);
    verify(sqlQueryMock, times(0))
      .addSelect(anyString(), any(BestFitColumnType.class), anyString());
  }

  @Test
  void testOneColumnExists() {
    drillThroughQuerySpec.extraPredicates(sqlQueryMock);
    verify(sqlQueryMock, times(1))
      .addSelect(isNull(), isNull(), anyString());
  }

  @Test
  void testTwoColumnsExist() {
    when(starPredicateMock.getConstrainedColumnList())
      .thenReturn(Arrays.asList(includedColumn, excludedColumn));
    drillThroughQuerySpec.extraPredicates(sqlQueryMock);
    verify(sqlQueryMock, times(2))
      .addSelect(isNull(), isNull(), anyString());
  }

  @Test
  void testColumnsNotIncludedInSelect() {
    when(requestMock.includeInSelect(includedColumn)).thenReturn(false);
    drillThroughQuerySpec.extraPredicates(sqlQueryMock);
    verify(sqlQueryMock, times(0))
      .addSelect(anyString(), any(BestFitColumnType.class), anyString());

    when(starPredicateMock.getConstrainedColumnList())
      .thenReturn(Arrays.asList(includedColumn, excludedColumn));
    verify(sqlQueryMock, times(0))
      .addSelect(anyString(), any(BestFitColumnType.class), anyString());
  }

  @Test
  void testColumnsPartiallyIncludedInSelect() {
    when(requestMock.includeInSelect(excludedColumn)).thenReturn(false);
    when(requestMock.includeInSelect(includedColumn)).thenReturn(true);
    when(starPredicateMock.getConstrainedColumnList())
      .thenReturn(Arrays.asList(includedColumn, excludedColumn));

    drillThroughQuerySpec.extraPredicates(sqlQueryMock);
    verify(sqlQueryMock, times(1))
      .addSelect(isNull(), isNull(), anyString());
  }

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
