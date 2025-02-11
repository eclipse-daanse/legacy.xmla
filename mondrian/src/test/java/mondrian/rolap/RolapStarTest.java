/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2018 Hitachi Vantara..  All rights reserved.
*/

package mondrian.rolap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.rolap.mapping.api.model.QueryMapping;
import org.eclipse.daanse.rolap.mapping.api.model.RelationalQueryMapping;
import org.eclipse.daanse.rolap.mapping.pojo.DatabaseSchemaMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalTableMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SqlStatementMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.rolap.RolapStar.Column;
import mondrian.rolap.util.RelationUtil;

/**
 * Unit test for {@link RolapStar}.
 *
 * @author pedrovale
 */
class RolapStarTest {

    static class RolapStarForTests extends RolapStar {
        public RolapStarForTests(
            final RolapCatalog schema,
            final Context context,
            final RelationalQueryMapping fact)
        {
            super(schema, context, fact);
        }

        public QueryMapping cloneRelationForTests(
            RelationalQueryMapping rel,
            String possibleName)
        {
            return cloneRelation(rel, possibleName);
        }
    }

    RolapStar getRolapStar(Connection con, String starName) {
        RolapCube cube =
            (RolapCube) con.getCatalog().lookupCube(starName, true);
        return cube.getStar();
    }

    RolapStarForTests getStar(Connection connection, String starName) {
        RolapStar rs =  getRolapStar(connection, starName);

        return new RolapStarForTests(
            rs.getCatalog(),
            rs.getContext(),
            rs.getFactTable().getRelation());
    }

    /**
     * Tests that given a {@link MappingTableQuery}, cloneRelation
     * respects the existing filters.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCloneRelationWithFilteredTable(Context context) {
      RolapStarForTests rs = getStar(context.getConnectionWithDefaultRole(), "sales");
      TableQueryMappingImpl original = TableQueryMappingImpl.builder()
    		  .withTable(((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("TestTable")
    				  .withsSchema(DatabaseSchemaMappingImpl.builder().withName("Sechema").build())).build())
    		  .withAlias("Alias")
    		  .withSqlWhereExpression(SqlStatementMappingImpl.builder()
    				  .withSql("Alias.clicked = 'true'")
    				  .withDialects(List.of("generic"))
    				  .build())
    		  .build();


      TableQueryMappingImpl cloned = (TableQueryMappingImpl)rs.cloneRelationForTests(
          original,
          "NewAlias");

      assertEquals("NewAlias", RelationUtil.getAlias(cloned));
      assertEquals("TestTable", cloned.getTable().getName());
      assertNotNull(cloned.getSqlWhereExpression());
      assertEquals("NewAlias.clicked = 'true'", cloned.getSqlWhereExpression().getSql());
  }

   //Below there are tests for mondrian.rolap.RolapStar.ColumnComparator
   @Test
   void testTwoColumnsWithDifferentNamesNotEquals() {
     RolapStar.ColumnComparator colComparator =
         RolapStar.ColumnComparator.instance;
     Column column1 = getColumnMock("Column1", "Table1");
     Column column2 = getColumnMock("Column2", "Table1");
     assertNotSame(column1, column2);
     assertEquals(-1, colComparator.compare(column1, column2));
   }

   @Test
   void testTwoColumnsWithEqualsNamesButDifferentTablesNotEquals() {
     RolapStar.ColumnComparator colComparator =
         RolapStar.ColumnComparator.instance;
     Column column1 = getColumnMock("Column1", "Table1");
     Column column2 = getColumnMock("Column1", "Table2");
     assertNotSame(column1, column2);
     assertEquals(-1, colComparator.compare(column1, column2));
   }

   @Test
   void testTwoColumnsEquals() {
     RolapStar.ColumnComparator colComparator =
         RolapStar.ColumnComparator.instance;
     Column column1 = getColumnMock("Column1", "Table1");
     Column column2 = getColumnMock("Column1", "Table1");
     assertNotSame(column1, column2);
     assertEquals(0, colComparator.compare(column1, column2));
   }

   private static Column getColumnMock(
       String columnName,
       String tableName)
   {
     Column colMock = mock(Column.class);
     RolapStar.Table tableMock = mock(RolapStar.Table.class);
     when(colMock.getName()).thenReturn(columnName);
     when(colMock.getTable()).thenReturn(tableMock);
     when(tableMock.getAlias()).thenReturn(tableName);
    return colMock;
   }
}
