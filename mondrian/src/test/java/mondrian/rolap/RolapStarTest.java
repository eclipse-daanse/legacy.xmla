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

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.rolap.common.RolapStar;
import org.eclipse.daanse.rolap.common.util.RelationUtil;
import org.eclipse.daanse.rolap.element.RolapCatalog;
import org.eclipse.daanse.rolap.element.RolapCube;
import org.eclipse.daanse.rolap.mapping.model.DatabaseSchema;
import org.eclipse.daanse.rolap.mapping.model.PhysicalTable;
import org.eclipse.daanse.rolap.mapping.model.Query;
import org.eclipse.daanse.rolap.mapping.model.RelationalQuery;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.SqlStatement;
import org.eclipse.daanse.rolap.mapping.model.TableQuery;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

/**
 * Unit test for {@link RolapStar}.
 *
 * @author pedrovale
 */
class RolapStarTest {

    static class RolapStarForTests extends RolapStar {
        public RolapStarForTests(
            final RolapCatalog schema,
            final Context<?> context,
            final RelationalQuery fact)
        {
            super(schema, context, fact);
        }

        public Query cloneRelationForTests(
            RelationalQuery rel,
            String possibleName)
        {
            return cloneRelation(rel, possibleName);
        }
    }

    RolapStar getRolapStar(Connection con, String starName) {
        RolapCube cube =
            (RolapCube) con.getCatalog().lookupCube(starName).orElseThrow();
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
    void testCloneRelationWithFilteredTable(Context<?> context) {
      RolapStarForTests rs = getStar(context.getConnectionWithDefaultRole(), "sales");
      DatabaseSchema ds = RolapMappingFactory.eINSTANCE.createDatabaseSchema();
      ds.setName("Sechema");
      PhysicalTable pt = RolapMappingFactory.eINSTANCE.createPhysicalTable();
      pt.setName("TestTable");
      pt.setSchema(ds);
      SqlStatement ss = RolapMappingFactory.eINSTANCE.createSqlStatement();
      ss.setSql("Alias.clicked = 'true'");
      ss.getDialects().add("generic");
      TableQuery original = RolapMappingFactory.eINSTANCE.createTableQuery();
      original.setTable(pt);
      original.setAlias("Alias");
      original.setSqlWhereExpression(ss);


      TableQuery cloned = (TableQuery)rs.cloneRelationForTests(
          original,
          "NewAlias");

      assertEquals("NewAlias", RelationUtil.getAlias(cloned));
      assertEquals("TestTable", cloned.getTable().getName());
      assertNotNull(cloned.getSqlWhereExpression());
      assertEquals("NewAlias.clicked = 'true'", cloned.getSqlWhereExpression().getSql());
  }

}
