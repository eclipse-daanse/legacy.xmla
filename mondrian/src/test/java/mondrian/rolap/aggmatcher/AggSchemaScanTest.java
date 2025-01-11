/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2019 Hitachi Vantara..  All rights reserved.
 */

package mondrian.rolap.aggmatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.rolap.api.RolapContext;
import org.eclipse.daanse.rdb.structure.api.model.DatabaseSchema;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextArgumentsProvider;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.rolap.RolapConnection;
import mondrian.rolap.RolapConnectionPropsR;

/**
  * Test if AggSchemaScan and AggCatalogScan properties are used in JdbcSchema loadTablesOfType
  *
  */
class AggSchemaScanTest {

  @BeforeAll
  public static void beforeAll() {
      ContextArgumentsProvider.dockerWasChanged = true;
  }


  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
  void testAggScanPropertiesEmptySchema(Context context) throws Exception {
    final RolapConnection rolapConn = (RolapConnection) context.getConnection();
    final DataSource dataSource = rolapConn.getDataSource();
    Connection sqlConnection = null;
    try {
      sqlConnection = dataSource.getConnection();

      CatalogMapping catalogMapping = ((RolapContext) context).getCatalogMapping();
      List<? extends DatabaseSchema> schemas = catalogMapping.getDbschemas();
      DatabaseSchema databaseSchema = schemas.getFirst();
      
      //RolapConnectionPropsR rc =  new RolapConnectionPropsR(List.of(), false, Locale.getDefault(), 0l, TimeUnit.SECONDS, Optional.of("bogus"),Optional.of("bogus"));
      JdbcSchema jdbcSchema = new JdbcSchema(databaseSchema);
      //jdbcSchema.resetAllTablesLoaded();
      jdbcSchema.getTablesMap().clear();

      //jdbcSchema.loadTables( rc );
      assertEquals( 0, jdbcSchema.getTablesMap().size() );
    } finally {
      if (sqlConnection != null) {
        try {
          sqlConnection.close();
        } catch ( SQLException e) {
          // ignore
        }
      }
    }
  }


  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
  void testAggScanPropertiesPopulatedSchema(Context context) throws Exception {
    final RolapConnection rolapConn = (RolapConnection) context.getConnection();
    final DataSource dataSource = rolapConn.getDataSource();
    Connection sqlConnection = null;
    try {
      sqlConnection = dataSource.getConnection();
      DatabaseMetaData dbmeta = sqlConnection.getMetaData();
      if ( !dbmeta.supportsSchemasInTableDefinitions() && !dbmeta.supportsCatalogsInTableDefinitions() ) {
        System.out.println( "Database does not support schema or catalog in table definitions.  Cannot run test." );
        return;
      }
		String propCatalog = null;
		String propSchema = null;
      boolean foundSchema = false;
      // Different databases treat catalogs and schemas differently.  Figure out whether foodmart is a schema or catalog in this database
      try {
        String schema = sqlConnection.getSchema();
        String catalog = sqlConnection.getCatalog();
        if ( schema != null || catalog != null ) {
          foundSchema = true;
          propCatalog= catalog ;
          propSchema= schema ;
        }
      } catch ( AbstractMethodError | Exception ex ) {
        // Catch if the JDBC client throws an exception.  Do nothing.
      }

      // Some databases like Oracle do not implement getSchema and getCatalog with the connection, so try the dbmeta instead
      if ( !foundSchema && dbmeta.supportsSchemasInTableDefinitions() ) {
        try ( ResultSet resultSet = dbmeta.getSchemas() ) {
           if ( resultSet.getMetaData().getColumnCount() == 2 ) {
             while ( resultSet.next() ) {
               if ( resultSet.getString( 1 ).equalsIgnoreCase( "foodmart" ) ) {

                   propCatalog= resultSet.getString( 2 ) ;
                   propSchema= resultSet.getString( 1 ) ;

                 foundSchema = true;
                 break;
               }
             }
           }

        }
      }

      if (dbmeta.supportsCatalogsInTableDefinitions() && !foundSchema) {
        try ( ResultSet resultSet = dbmeta.getCatalogs() ) {
          if ( resultSet.getMetaData().getColumnCount() == 1 ) {
            while ( resultSet.next() ) {
              if ( resultSet.getString( 1 ).equalsIgnoreCase( "foodmart" ) ) {
                propCatalog= resultSet.getString( 1 ) ;
                foundSchema = true;
                break;
              }
            }
          }
        }
      }

      if ( !foundSchema ) {
        System.out.println( "Cannot find foodmart schema or catalog in database.  Cannot run test." );
        return;
      }
      CatalogMapping catalogMapping = ((RolapContext) context).getCatalogMapping();
      List<? extends DatabaseSchema> schemas = catalogMapping.getDbschemas();
      DatabaseSchema databaseSchema = schemas.getFirst();
      JdbcSchema jdbcSchema = new JdbcSchema(databaseSchema);
      //The foodmart schema has 26 tables in module.
      assertEquals( 26, jdbcSchema.getTablesMap().size() );
    } finally {
      if (sqlConnection != null) {
        try {
          sqlConnection.close();
        } catch ( SQLException e) {
          // ignore
        }
      }
    }
  }
}
