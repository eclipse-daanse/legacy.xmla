/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2021 Hitachi Vantara and others. All rights reserved.
*/
package mondrian.test;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.executeQuery;
import static org.opencube.junit5.TestUtil.getFakeDialect;
import static org.opencube.junit5.TestUtil.withSchema;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.eclipse.daanse.jdbc.db.dialect.db.common.JdbcDialectImpl;
import org.eclipse.daanse.jdbc.db.dialect.api.BestFitColumnType;
import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;
import org.opentest4j.AssertionFailedError;

import mondrian.enums.DatabaseProduct;
import mondrian.olap.Util;
import mondrian.rolap.RolapMember;
import mondrian.rolap.SchemaModifiers;
import mondrian.rolap.sql.SqlQuery;
import mondrian.util.DelegatingInvocationHandler;

/**
 * Unit test which checks that {@link Dialect} accurately represents the capabilities of the underlying
 * database.
 *
 * <p>The existing mondrian tests, when run on various databases and drivers,
 * make sure that Dialect never over-states the capabilities of a particular database. But sometimes they under-state a
 * database's capabilities: for example, MySQL version 3 did not allow subqueries in the FROM clause, but version 4
 * does. This test helps ensure that mondrian is using the full capabilities of each database.
 *
 * <p><strong>NOTE: If you see failures in this test, let the mondrian
 * developers know!</strong> You may be running a version of a database which no one has tried before, and which has
 * more capabilities than we expect. If you tell us about them, we can change mondrian to use those features.</p>
 *
 * @author jhyde
 * @since May 18, 2007
 */
class DialectTest {
  private Connection connection;
  private Dialect dialect;
  private static final String INFOBRIGHT_UNSUPPORTED =
          "The query includes syntax that is not supported by the Infobright"
                  + " Optimizer. Either restructure the query with supported syntax, or"
                  + " enable the MySQL Query Path in the brighthouse.ini file to execute"
                  + " the query with reduced performance.";
  private static final String NEOVIEW_SYNTAX_ERROR =
          "(?s).* ERROR\\[15001\\] A syntax error occurred at or before: .*";


  protected DataSource getDataSource(Context context) {
    return context.getConnection().getDataSource();
  }

  @AfterEach
  public void afterEach() {
    if ( connection != null ) {
      try {
        connection.close();
      } catch ( SQLException e ) {
        // ignore
      } finally {
        connection = null;
      }
    }
  }

  protected Dialect getDialect(Context context) {
    if ( dialect == null ) {
        dialect = context.getDialect();
    }
    return dialect;
  }

  protected Connection getConnection(Context context) {
    if ( connection == null ) {
      try {
        connection = getDataSource(context).getConnection();
      } catch ( SQLException e ) {
        throw Util.newInternal( e, "while creating connection" );
      }
    }
    return connection;
  }

//  @ParameterizedTest
//  @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
//  void testDialectVsDatabaseProduct(Context context) throws SQLException {
//    final Dialect dialect = getDialect(context);
//    final DatabaseProduct databaseProduct =
//            getDatabaseProduct(dialect.getDialectName());
//    final DatabaseMetaData databaseMetaData = getConnection(context).getMetaData();
//    switch ( databaseProduct ) {
//      case MARIADB:
//        // Dialect has identified that it is MariaDB.
//        assertTrue( dialect instanceof MySqlDialect);
//        assertFalse( dialect instanceof InfobrightDialect);
//        assertTrue( dialect instanceof MariaDBDialect);
//        assertFalse( MySqlDialect.isInfobright( databaseMetaData ) );
//        assertEquals( "MariaDB", databaseMetaData.getDatabaseProductName() );
//        break;
//      case MYSQL:
//        // Dialect has identified that it is MySQL.
//        assertTrue( dialect instanceof MySqlDialect );
//        assertFalse( dialect instanceof InfobrightDialect );
//        assertFalse( MySqlDialect.isInfobright( databaseMetaData ) );
//        assertEquals( "MySQL", databaseMetaData.getDatabaseProductName() );
//        break;
//      case HIVE:
//        // Dialect has identified that it is Hive.
//        assertTrue( dialect instanceof HiveDialect);
//        break;
//      case INFOBRIGHT:
//        // Dialect has identified that it is MySQL.
//        assertTrue( dialect instanceof MySqlDialect );
//        assertTrue( dialect instanceof InfobrightDialect );
//        assertTrue( MySqlDialect.isInfobright( databaseMetaData ) );
//        assertEquals( "MySQL", databaseMetaData.getDatabaseProductName() );
//        break;
//      case POSTGRESQL:
//        // Dialect has identified that it is PostgreSQL.
//        assertTrue( dialect instanceof PostgreSqlDialect);
//        assertFalse( dialect instanceof NetezzaDialect);
//        assertTrue(
//                databaseMetaData.getDatabaseProductName()
//                        .indexOf( "PostgreSQL" ) >= 0 );
//        break;
//      case MSSQL:
//        // Dialect has identified that it is MSSQL.
//        assertTrue( dialect instanceof MicrosoftSqlServerDialect);
//        assertTrue(
//                databaseMetaData.getDatabaseProductName()
//                        .contains( "Microsoft" ) );
//        break;
//      case NETEZZA:
//        // Dialect has identified that it is Netezza and a sub class of
//        // PostgreSql.
//        assertTrue( dialect instanceof PostgreSqlDialect );
//        assertTrue( dialect instanceof NetezzaDialect );
//        assertTrue(
//                databaseMetaData.getDatabaseProductName()
//                        .indexOf( "Netezza" ) >= 0 );
//        break;
//      case NUODB:
//        // Dialect has identified that it is NUODB.
//        assertTrue( dialect instanceof NuoDbDialect);
//        assertTrue(
//                databaseMetaData.getDatabaseProductName()
//                        .contains( "NuoDB" ) );
//        break;
//      case GOOGLEBIGQUERY:
//        assertTrue( dialect instanceof GoogleBigQueryDialect );
//        assertTrue(
//                databaseMetaData.getDatabaseProductName()
//                        .contains( "Google BigQuery" ) );
//        break;
//      default:
//        // Neither MySQL nor Infobright.
//        assertFalse( dialect instanceof MySqlDialect );
//        assertFalse( dialect instanceof InfobrightDialect );
//        assertNotSame( "MySQL", databaseMetaData.getDatabaseProductName() );
//        break;
//    }
//  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAllowsCompoundCountDistinct(Context context) {
    String sql =
            dialectize(context,
                    "select count(distinct [customer_id], [product_id])\n"
                            + "from [foodmart.sales_fact_1997]" );
    if ( getDialect(context).allowsCompoundCountDistinct() ) {
      assertQuerySucceeds(context, sql );
    } else {
      String[] errs = {
              // oracle
              "(?s)ORA-00909: invalid number of arguments.*",
              // derby
              "Syntax error: Encountered \",\" at line 1, column 36.",
              // access
              "\\[Microsoft\\]\\[ODBC Microsoft Access Driver\\] Syntax error \\(missing operator\\) in query expression '"
                      + ".*'.",
              // hsqldb
              "Unexpected token in statement \\[select count\\(distinct \"customer_id\",\\]",
              // infobright
              INFOBRIGHT_UNSUPPORTED,
              // neoview
              ".* ERROR\\[3129\\] Function COUNT DISTINCT accepts exactly one operand\\. .*",
              // postgres
              "ERROR: function count\\(integer, integer\\) does not exist.*",
              // LucidDb
              ".*Invalid number of arguments to function 'COUNT'. Was expecting 1 arguments",
              // teradata
              ".*Syntax error: expected something between the word 'customer_id' and ','\\..*",
              // netezza
              "(?s).*ERROR:  Function 'COUNT', number of parameters greater than the maximum \\(1\\).*",
              // Vertica
              "(?s).*ERROR: [Ff]unction count\\(int, int\\) does not exist, or permission is denied for count\\(int, int\\)"
                      + ".*",
              // postgres
              "(?s).*ERROR: function count\\(integer, integer\\) does not exist.*",
              // monetdb
              "syntax error, unexpected ',', expecting '\\)' in: \"select count\\(distinct \"customer_id\",\"",
              // SQL server 2008
              "Incorrect syntax near ','.",
              // NuoDB
              "(?s).*expected closing parenthesis got ,.*",
              // Google BigQuery
              "(?s).*No matching signature for aggregate function COUNT for argument types: INT64, INT64.*"
      };
      assertQueryFails(context, sql, errs );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAllowsCountDistinct(Context context) {
    String sql1 =
            dialectize(context,
                    "select count(distinct [customer_id]) from [foodmart.sales_fact_1997]" );
    // one distinct-count and one nondistinct-agg
    String sql2 =
            dialectize(context,
                    "select count(distinct [customer_id]),\n"
                            + " sum([time_id])\n"
                            + "from [foodmart.sales_fact_1997]" );
    if ( getDialect(context).allowsCountDistinct() ) {
      assertQuerySucceeds(context, sql1 );
      assertQuerySucceeds(context, sql2 );
    } else {
      String[] errs = {
              // access
              "\\[Microsoft\\]\\[ODBC Microsoft Access Driver\\] Syntax error \\(missing operator\\) in query expression '"
                      + ".*'."
      };
      assertQueryFails(context, sql1, errs );
      assertQueryFails(context, sql2, errs );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAllowsMultipleCountDistinct(Context context) {
    // multiple distinct-counts
    String sql1 =
            dialectize(context,
                    "select count(distinct [customer_id]),\n"
                            + " count(distinct [time_id])\n"
                            + "from [foodmart.sales_fact_1997]" );
    // multiple distinct-counts with group by and other aggs
    String sql3 =
            dialectize(context,
                    "select [unit_sales],\n"
                            + " count(distinct [customer_id]),\n"
                            + " count(distinct [product_id])\n"
                            + "from [foodmart.sales_fact_1997]\n"
                            + "where [time_id] in (371, 372)\n"
                            + "group by [unit_sales]" );
    if ( getDialect(context).allowsMultipleCountDistinct() ) {
      assertQuerySucceeds(context, sql1 );
      assertQuerySucceeds(context, sql3 );
      assertTrue( getDialect(context).allowsCountDistinct() );
    } else {
      String[] errs = {
              // derby
              "Multiple DISTINCT aggregates are not supported at this time.",
              // access
              "\\[Microsoft\\]\\[ODBC Microsoft Access Driver\\] Syntax error \\(missing operator\\) in query expression '"
                      + ".*'.",
              // impala -- Returns a whole stack trace in its message
              // requires (?s) to set single line mode
              "(?s).*all DISTINCT aggregate functions need to have the same set of parameters as COUNT\\(DISTINCT "
                      + "customer_id\\)\\; deviating function\\: COUNT\\(DISTINCT time_id\\).*",
              // impala
              "(?s).*GROUP BY expression must have a discrete \\(non-floating point\\) type.*"
      };
      assertQueryFails(context, sql1, errs );
      assertQueryFails(context, sql3, errs );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAllowsDdl(Context context) {
    int phase = 0;
    SQLException e = null;
    Statement stmt = null;
    try {
      String dropSql = dialectize(context, "drop table [foo]" );
      String createSql = dialectize(context, "create table [foo] ([i] integer)" );
      stmt = getConnection(context).createStatement();

      // drop previously existing table, and ignore any errors
      try {
        stmt.execute( dropSql );
      } catch ( SQLException e3 ) {
        // ignore
      }
      // now create and drop a dummy table
      phase = 1;
      stmt.execute( createSql );
      phase = 2;
      stmt.execute( dropSql );
      phase = 3;
    } catch ( SQLException e2 ) {
      e = e2;
    } finally {
      if ( stmt != null ) {
        try {
          stmt.close();
        } catch ( SQLException e1 ) {
          // ignore
        }
      }
    }
    if ( getDialect(context).allowsDdl() ) {
      assertNull(e, e == null ? null : e.getMessage() );
      assertEquals( 3, phase );
    } else {
      assertEquals( 1, phase );
      assertNotNull( e );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAllowsFromQuery(Context context) {
    String sql =
            dialectize(context,
                    "select * from (select * from [foodmart.sales_fact_1997]) as [x]" );
    if ( getDialect(context).allowsFromQuery() ) {
      assertQuerySucceeds(context, sql );
    } else {
      assertQueryFails(context, sql, new String[] {} );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRequiresFromQueryAlias(Context context) {
    if ( getDialect(context).requiresAliasForFromQuery() ) {
      assertTrue( getDialect(context).allowsFromQuery() );
    }
    if ( !getDialect(context).allowsFromQuery() ) {
      return;
    }

    String sql =
            dialectize(context,
                    "select * from (select * from [foodmart.sales_fact_1997])" );
    if ( getDialect(context).requiresAliasForFromQuery() ) {
      String[] errs = {
              // mysql
              "Every derived table must have its own alias",
              // derby
              "Syntax error: Encountered \"<EOF>\" at line 1, column 47.",
              // hive
              "(?s).*mismatched input \'<EOF>\' expecting Identifier in subquery source.*",
              // postgres simular with Greenplum
              "(?s)ERROR: subquery in FROM must have an alias.*",
              // teradata
              ".*Syntax error, expected something like a name or a Unicode "
                      + "delimited identifier or an 'UDFCALLNAME' keyword between "
                      + "'\\)' and ';'\\.",
              // neoview
              NEOVIEW_SYNTAX_ERROR,
              // netezza
              "(?s).*ERROR:  sub-SELECT in FROM must have an alias.*",
              // monetdb
              "subquery table reference needs alias, use AS xxx in:.*",
              // SQL server 2008
              "Incorrect syntax near \\'\\)\\'\\.",
              // Impala
              "(?s).*Encountered: EOF.*Expected: IDENTIFIER.*",
              // Vertica 6
              "(?s).*ERROR: Subquery in FROM must have an alias.*"
      };
      assertQueryFails(context, sql, errs );
    } else {
      assertQuerySucceeds(context, sql );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRequiresOrderByAlias(Context context) {
    String sql =
            dialectize(context,
                    "SELECT [unit_sales]\n"
                            + "FROM [foodmart.sales_fact_1997]\n"
                            + "ORDER BY [unit_sales] + [store_id]" );
    if ( getDialect(context).requiresOrderByAlias() ) {
      final String[] errs = {
              // infobright
              INFOBRIGHT_UNSUPPORTED,
              // hive
              "(?s).*Invalid Table Alias or Column Reference.*",
              // neoview
              NEOVIEW_SYNTAX_ERROR,
              //snowflake
              "(?s).*Processing aborted due to error 300002:2523989150.*"
      };
      assertQueryFails(context, sql, errs );
    } else {
      assertQuerySucceeds(context, sql );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAllowsOrderByAlias(Context context) {
    String sql =
            dialectize(context,
                    "SELECT [unit_sales] as [x],\n"
                            + " [unit_sales] + [store_id] as [y]\n"
                            + "FROM [foodmart.sales_fact_1997]\n"
                            + "ORDER BY [y]" );
    if ( getDialect(context).allowsOrderByAlias() ) {
      assertQuerySucceeds(context, sql );
    } else {
      String[] errs = {
              // oracle
              "(?s)ORA-03001: unimplemented feature.*",
              // access
              "\\[Microsoft\\]\\[ODBC Microsoft Access Driver\\] Too few parameters. Expected 1.",
              // infobright
              INFOBRIGHT_UNSUPPORTED,
      };
      assertQueryFails(context, sql, errs );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRequiresUnionOrderByOrdinal(Context context) {
    final String sql;
    switch ( getDatabaseProduct(getDialect(context).getDialectName()) ) {
      default:
        sql =
                dialectize(context,
                        "select\n"
                                + "    *\n"
                                + "from\n"
                                + "    (select\n"
                                + "    [time_by_day].[the_year] as [c0]\n"
                                + "from\n"
                                + "    [foodmart.time_by_day] as [time_by_day]\n"
                                + "group by\n"
                                + "    [time_by_day].[the_year]\n"
                                + "union all\n"
                                + "select\n"
                                + "    [time_by_day].[the_year] as [c0]\n"
                                + "from\n"
                                + "    [foodmart.time_by_day] as [time_by_day]\n"
                                + "group by\n"
                                + "    [time_by_day].[the_year]) as [unionQuery]\n"
                                + "order by\n"
                                + getDialect(context).generateOrderItem( "1", true, true, true ) );
    }

    if ( getDialect(context).requiresUnionOrderByOrdinal() ) {
      assertQuerySucceeds(context, sql );
    } else {
      String[] errs = {
              // SQL server 2008
              "A constant expression was encountered in the ORDER BY list, position 1."
      };
      assertQueryFails(context, sql, errs );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRequiresUnionOrderByExprToBeInSelectClause(Context context) {
    String sql =
            dialectize(context,
                    "SELECT [unit_sales], [store_sales]\n"
                            + "FROM [foodmart.sales_fact_1997]\n"
                            + "UNION ALL\n"
                            + "SELECT [unit_sales], [store_sales]\n"
                            + "FROM [foodmart.sales_fact_1997]\n"
                            + "ORDER BY [unit_sales] + [store_sales]" );

    if ( !getDialect(context).requiresUnionOrderByExprToBeInSelectClause() ) {
      assertQuerySucceeds(context, sql );
    } else {
      String[] errs = {
              // access
              "\\[Microsoft\\]\\[ODBC Microsoft Access Driver\\] The ORDER "
                      + "BY expression \\(\\[unit_sales\\]\\+\\[store_sales\\]\\) "
                      + "includes fields that are not selected by the query\\.  "
                      + "Only those fields requested in the first query can be "
                      + "included in an ORDER BY expression\\.",
              // derby (yes, lame message)
              "Java exception: ': java.lang.NullPointerException'.",
              // hsqldb
              "(?s)Cannot be in ORDER BY clause in statement .*",
              // neoview
              NEOVIEW_SYNTAX_ERROR,
              // oracle
              "ORA-01785: ORDER BY item must be the number of a SELECT-list "
                      + "expression\n",
              // teradata
              ".*The ORDER BY clause must contain only integer constants.",
              // Greenplum / Postgres
              "ERROR: ORDER BY on a UNION/INTERSECT/EXCEPT result must be on "
                      + "one of the result columns.*",
              // Postgres 9
              "(?s)ERROR: invalid UNION/INTERSECT/EXCEPT ORDER BY clause.*",
              // Vectorwise
              "Parse error in StringBuffer at line 0, column 525\\: \\<missing\\>\\.",
              // SQL server 2008
              "ORDER BY items must appear in the select list if the statement contains a UNION, INTERSECT or EXCEPT "
                      + "operator.",
              // Vertica 6
              "(?s).*ERROR: ORDER BY on a UNION/INTERSECT/EXCEPT result must be on "
                      + "one of the result columns.*",
      };
      assertQueryFails(context, sql, errs );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSupportsGroupByExpressions(Context context) {
    String sql =
            dialectize(context,
                    "SELECT sum([unit_sales] + 3) + 8\n"
                            + "FROM [foodmart.sales_fact_1997]\n"
                            + "GROUP BY [unit_sales] + [store_id]" );
    if ( getDialect(context).supportsGroupByExpressions() ) {
      assertQuerySucceeds(context, sql );
    } else {
      final String[] errs = {
              // mysql
              "'sum\\(`unit_sales` \\+ 3\\) \\+ 8' isn't in GROUP BY",
              // neoview
              ".* ERROR\\[4197\\] This expression cannot be used in the GROUP BY clause\\. .*",
              // monetdb
              "syntax error, unexpected '\\+', expecting SCOLON in: \"select sum\\(\"unit_sales\" \\+ 3\\) \\+ 8",
              // impala
              "(?s).*GROUP BY expression must have a discrete.*"
      };
      assertQueryFails(context, sql, errs );
    }
  }

  /**
   * Tests that the {@link Dialect#supportsGroupingSets()} dialect property is accurate.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAllowsGroupingSets(Context context) {
    String sql =
            dialectize(context,
                    "SELECT [customer_id],\n"
                            + " SUM([store_sales]),\n"
                            + " GROUPING([unit_sales]),\n"
                            + " GROUPING([customer_id])\n"
                            + "FROM [sales_fact_1997]\n"
                            + "GROUP BY GROUPING SETS(\n"
                            + " ([customer_id], [unit_sales]),\n"
                            + " [customer_id],\n"
                            + " ())" );
    if ( getDialect(context).supportsGroupingSets() ) {
      assertQuerySucceeds(context, sql );
    } else {
      String[] errs = {
              // derby
              "Syntax error: Encountered \"SETS\" at line 6, column 19.",
              // hive
              "(?s).*line 6:18 mismatched input 'SETS' expecting EOF.*",
              // hsqldb
              "(?s)Unexpected token: GROUPING in statement .*",
              // mysql
              "(?s)You have an error in your SQL syntax; check .*",
              // access
              "(?s)\\[Microsoft\\]\\[ODBC Microsoft Access Driver\\] Syntax error \\(missing operator\\) in query "
                      + "expression 'GROUPING SETS.*",
              // luciddb
              "(?s).*Encountered \"GROUPING\" at line 3, column 2\\..*",
              // postgres
              "(?s)ERROR: syntax error at or near \"SETS\".*",
              // neoview
              NEOVIEW_SYNTAX_ERROR,
              // netezza
              "(?s).*found \"SETS\" \\(at char 135\\) expecting `EXCEPT' or `FOR' or `INTERSECT' or `ORDER' or `UNION'.*",
              // Vertica
              "line 3, There is no such function as \\'grouping\\'\\.",
              // Vertica 6
              "(?s).*ERROR: Syntax error at or near \"SETS\".*",
              // monetdb
              "syntax error, unexpected IDENT, expecting SCOLON in: \"select \"customer_id\",",
              // impala
              "(?s).*Encountered: IDENTIFIER.*Expected: DIV, HAVING, LIMIT, ORDER, UNION, COMMA.*",
              // NuoDB
              "(?s).*expected end of statement got SETS.*",
              // Google BigQuery
              "(?s).*but got identifier \"SETS\" at \\[6:19\\].*"
      };
      assertQueryFails(context, sql, errs );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSupportsMultiValueInExpr(Context context) {
    String sql =
            dialectize(context,
                    "SELECT [unit_sales]\n"
                            + "FROM [foodmart.sales_fact_1997]\n"
                            + "WHERE ([unit_sales], [time_id]) IN ((1, 371), (2, 394))" );

    if ( getDialect(context).supportsMultiValueInExpr() ) {
      assertQuerySucceeds(context, sql );
    } else {
      String[] errs = {
              // derby
              "Syntax error: Encountered \",\" at line 3, column 20.",
              // access
              "\\[Microsoft\\]\\[ODBC Microsoft Access Driver\\] Syntax error \\(comma\\) in query expression '.*'.",
              // hive
              "(?s).*line 3:19 mismatched input ','.*",
              // hsqldb
              "(?s)Unexpected token: , in statement .*",
              // infobright
              INFOBRIGHT_UNSUPPORTED,
              // neoview
              NEOVIEW_SYNTAX_ERROR,
              // teradata
              ".*Syntax error, expected something like a 'SELECT' keyword or '\\(' between '\\(' and the integer '1'\\.",
              // netezza
              "(?s).*found \"1\" \\(at char 81\\) expecting `SELECT' or `'\\(''.*",
              // monetdb
              "syntax error, unexpected ',', expecting '\\)' in: \"select \"unit_sales\"",
              // SQL server 2008
              "An expression of non-boolean type specified in a context where a condition is expected, near ','.",
              // impala
              "(?s).*Encountered: COMMA.*Expected: BETWEEN, DIV, IS, IN, LIKE, NOT, REGEXP, RLIKE.*",
              // NuoDB
              "(?s).*Operator in list does not support multi-column operands.*"
      };
      assertQueryFails(context, sql, errs );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDateLiteralString(Context context) {
    // verify correct construction of the date literal string.
    // With Oracle this can get interesting, because depending on the
    // driver version the string may be a DATE or a TIMESTAMP.
    // We need to construct a valid date literal in either case.
    // See http://jira.pentaho.com/browse/MONDRIAN-1819 and
    // http://jira.pentaho.com/browse/MONDRIAN-626
    //
    // verify jdbc dialect - some jdbc drivers return TIMESTAMP too
    // http://jira.pentaho.com/browse/MONDRIAN-2038
    Dialect jdbcDialect = new JdbcDialectImpl(){
        @Override
        public String getDialectName() {
            return null;
        }

    };
    StringBuilder buf = new StringBuilder();
    jdbcDialect.quoteDateLiteral( buf, "2003-12-12" );
    assertEquals( "DATE '2003-12-12'", buf.toString() );
    buf = new StringBuilder();
    jdbcDialect.quoteDateLiteral( buf, "2007-01-15 00:00:00.0" );
    assertEquals( "DATE '2007-01-15'", buf.toString() );

    if ( getDatabaseProduct(getDialect(context).getDialectName())
            != DatabaseProduct.ORACLE ) {
      // the following test is specifically for Oracle.
      return;
    }
      withSchema(context, SchemaModifiers.DialectTestModifier1::new);
    // if date literal is incorrect the following query will give the error
    // ORA-01861: literal does not match format string
    Result result = executeQuery(context.getConnection(),
            "select Time.[All Times].FirstChild on 0 from DateLiteralTest" );
    String firstChild =
            result.getAxes()[ 0 ].getPositions().get( 0 ).get( 0 )
                    .getName().toString();
    // the member name may have timestamp info, for example if using
    // Oracle with ojdbc5+.  Make sure it starts w/ the expected date.
    assertTrue( firstChild.startsWith( "1997-01-01" ) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testBigInt(Context context) {
    if ( getDatabaseProduct(getDialect(context).getDialectName())
            != DatabaseProduct.VERTICA ) {
      // currently only checks VERTICA
      // Once MONDRIAN-1890 is fixed this test should minimally cover
      // Oracle and MySQL as well.
      return;
    }
    withSchema(context, SchemaModifiers.DialectTestModifier2::new);
    Result result = executeQuery(context.getConnection(),
            "select StoreSqft.[All StoreSqfts].children on 0 from BigIntTest" );
    RolapMember secondChild =
            (RolapMember) result.getAxes()[ 0 ].getPositions().get( 1 ).get( 0 );

    assertTrue( secondChild.getKey() instanceof Long );
    assertEquals( 2147503966L, ( (Long) secondChild.getKey() ).longValue() );

    assertQueryReturns(context.getConnection(),
            "select StoreSqft.[All StoreSqfts].children on 0, "
                    + "{measures.[Big Unit Sales]} on 1 from BigIntTest",
            "Axis #0:\n"
                    + "{}\n"
                    + "Axis #1:\n"
                    + "{[StoreSqft].[#null]}\n"
                    + "{[StoreSqft].[2147503966]}\n"
                    + "{[StoreSqft].[2147504862]}\n"
                    + "{[StoreSqft].[2147506125]}\n"
                    + "{[StoreSqft].[2147506759]}\n"
                    + "{[StoreSqft].[2147507240]}\n"
                    + "{[StoreSqft].[2147507245]}\n"
                    + "{[StoreSqft].[2147507335]}\n"
                    + "{[StoreSqft].[2147507406]}\n"
                    + "{[StoreSqft].[2147508244]}\n"
                    + "{[StoreSqft].[2147511341]}\n"
                    + "{[StoreSqft].[2147511853]}\n"
                    + "{[StoreSqft].[2147513915]}\n"
                    + "{[StoreSqft].[2147514231]}\n"
                    + "{[StoreSqft].[2147514444]}\n"
                    + "{[StoreSqft].[2147517505]}\n"
                    + "{[StoreSqft].[2147518099]}\n"
                    + "{[StoreSqft].[2147518438]}\n"
                    + "{[StoreSqft].[2147520156]}\n"
                    + "{[StoreSqft].[2147522029]}\n"
                    + "{[StoreSqft].[2147523343]}\n"
                    + "Axis #2:\n"
                    + "{[Measures].[Big Unit Sales]}\n"
                    + "Row #0: 28,101,971,043,971\n"
                    + "Row #0: 17,746,804,884,887\n"
                    + "Row #0: 17,085,379,920,543\n"
                    + "Row #0: 2,845,415,834,392\n"
                    + "Row #0: \n"
                    + "Row #0: \n"
                    + "Row #0: 17,624,398,316,592\n"
                    + "Row #0: 14,635,101,075,638\n"
                    + "Row #0: \n"
                    + "Row #0: \n"
                    + "Row #0: 28,662,464,278,089\n"
                    + "Row #0: 2,963,527,435,097\n"
                    + "Row #0: 15,884,936,560,450\n"
                    + "Row #0: \n"
                    + "Row #0: \n"
                    + "Row #0: 24,017,457,143,305\n"
                    + "Row #0: \n"
                    + "Row #0: \n"
                    + "Row #0: \n"
                    + "Row #0: \n"
                    + "Row #0: 16,913,581,228,348\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testResultSetConcurrency(Context context) {
    int[] Types = {
            ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.TYPE_SCROLL_INSENSITIVE,
            ResultSet.TYPE_SCROLL_SENSITIVE
    };
    int[] Concurs = {
            ResultSet.CONCUR_READ_ONLY,
            ResultSet.CONCUR_UPDATABLE
    };
    String sql =
            dialectize(context, "SELECT [unit_sales] FROM [sales_fact_1997]" );
    for ( int type : Types ) {
      for ( int concur : Concurs ) {
        boolean b =
                getDialect(context).supportsResultSetConcurrency( type, concur );
        Statement stmt = null;
        try {
          stmt = getConnection(context).createStatement( type, concur );
          ResultSet resultSet = stmt.executeQuery( sql );
          assertTrue( resultSet.next() );
          Object col1 = resultSet.getObject( 1 );
//          discard( col1 );
          if ( !b ) {
            // It's a little surprising that the driver said it
            // didn't support this type/concurrency combination,
            // but allowed the statement to be executed anyway.
            // But don't fail.

//            discard(
//                    "expected to fail for type=" + type
//                            + ", concur=" + concur );
          }
        } catch ( SQLException e ) {
          if ( b ) {
            fail(
                    "expected to succeed for type=" + type
                            + ", concur=" + concur );
            throw Util.newInternal( e, "query [" + sql + "] failed" );
          }
        } finally {
          if ( stmt != null ) {
            try {
              stmt.close();
            } catch ( SQLException e ) {
              // ignore
            }
          }
        }
      }
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGenerateInline(Context context) throws SQLException {
    final List<String> typeList = Arrays.asList( "String", "Numeric" );
    final List<String> nameList = Arrays.asList( "x", "y" );
    assertInline(context,
            nameList, typeList,
            new String[] { "a", "1" } );

    assertInline(context,
            nameList, typeList,
            new String[] { "a", "1" }, new String[] { "bb", "2" } );

    // Make sure the handling of the single quote doesn't interfere
    // with double quotes
    assertInline(context,
            nameList, typeList,
            new String[] { "can't \"stop\"", "1" } );

    // string containing single quote (problem for all database) and a
    // backslash (problem for mysql; appears as a double backslash for
    // java's benefit, but is a single backslash by the time it gets to SQL)
    assertInline(context,
            nameList, typeList,
            new String[] { "can't stop", "1" }, new String[] { "back\\slash", "2" } );

    // date value
    final List<String> typeList2 = Arrays.asList( "String", "Date" );
    assertInline(context,
            nameList, typeList2,
            new String[] { "a", "2008-04-29" }, new String[] { "b", "2007-01-02" } );
  }

  /**
   * Tests that the dialect can generate a valid query to sort ascending and descending, with NULL values appearing
   * last
   * in both cases.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testForceNullCollation(Context context) throws SQLException {
    checkForceNullCollation(context, true, true );
    checkForceNullCollation(context, false, true );
    checkForceNullCollation(context, true, false );
    checkForceNullCollation(context, false, false );
  }

  private StringBuilder dialectizeTableName(Context context,  StringBuilder name ) {
    // GBQ needs the schema name, not others.
    switch ( getDatabaseProduct(getDialect(context).getDialectName()) ) {
      case GOOGLEBIGQUERY:
        return new StringBuilder(dialect.quoteIdentifier( "foodmart" )).append(".").append(name);
      default:
        return name;
    }
  }

  /**
   * Checks that the dialect can generate a valid query to sort in a given direction, with NULL values appearing last.
   *
   * @param ascending Whether ascending
   * @param nullsLast Force nulls last or not.
   */
  private void checkForceNullCollation(Context context,
                                       boolean ascending,
                                       boolean nullsLast ) throws SQLException {
    Dialect dialect = getDialect(context);
    String query =
            "select "
                    + dialect.quoteIdentifier( "store_manager" )
                    + " from "
                    + dialectizeTableName(context, dialect.quoteIdentifier( "store" ) )
                    + " order by "
                    + dialect.generateOrderItem(
                    dialect.quoteIdentifier( "store_manager" ),
                    true, ascending, nullsLast );
    if ( ascending ) {
      if ( nullsLast ) {
        assertFirstLast(context, query, "Brown", null );
      } else {
        assertFirstLast(context, query, null, "Williams" );
      }
    } else {
      // Largest value comes first, null comes last.
      switch ( getDatabaseProduct(dialect.getDialectName()) ) {
        case GREENPLUM:
          // Current version cannot force null order, introduced in
          // Postgres 8.3
          return;
        case HIVE:
          // Hive cannot force nulls to appear last
          return;
        case NEOVIEW:
          // Neoview cannot force nulls to appear last
          return;
      }
      if ( nullsLast ) {
        assertFirstLast(context, query, "Williams", null );
      } else {
        assertFirstLast(context, query, null, "Brown" );
      }
    }
  }

  private void assertFirstLast(Context context,
                               String query,
                               String expectedFirst,
                               String expectedLast ) throws SQLException {
    ResultSet resultSet =
            getConnection(context).createStatement().executeQuery( query );
    List<String> values = new ArrayList<>();
    while ( resultSet.next() ) {
      values.add( resultSet.getString( 1 ) );
      if ( resultSet.wasNull() ) {
        values.set( values.size() - 1, null );
      }
    }
    resultSet.close();
    String actualFirst = values.get( 0 );
    String actualLast = values.get( values.size() - 1 );
    assertEquals(expectedFirst, actualFirst, query);
    assertEquals(expectedLast, actualLast,  query);
  }

  private void assertInline(Context context,
                            List<String> nameList,
                            List<String> typeList,
                            String[]... valueList ) throws SQLException {
    String sql =
            getDialect(context).generateInline(
                    nameList,
                    typeList,
                    Arrays.asList( valueList ) ).toString();
    Statement stmt = null;
    try {
      stmt = getConnection(context).createStatement();
      ResultSet resultSet = stmt.executeQuery( sql );
      Set<List<String>> actualValues = new HashSet<>();
      while ( resultSet.next() ) {
        final List<String> row = new ArrayList<>();
        for ( int i = 0; i < typeList.size(); i++ ) {
          final String s;
          final String type = typeList.get( i );
          if ( type.equals( "String" ) ) {
            s = resultSet.getString( i + 1 );
          } else if ( type.equals( "Date" ) ) {
            s = String.valueOf( resultSet.getDate( i + 1 ) );
          } else if ( type.equals( "Numeric" ) ) {
            s = String.valueOf( resultSet.getInt( i + 1 ) );
          } else {
            throw new RuntimeException( "unknown type " + type );
          }
          row.add( s );
        }
        actualValues.add( row );
      }
      Set<List<String>> expectedRows = new HashSet<>();
      for ( String[] strings : valueList ) {
        expectedRows.add( Arrays.asList( strings ) );
      }
      assertEquals( expectedRows, actualValues );
      stmt.close();
      stmt = null;
    } finally {
      if ( stmt != null ) {
        try {
          stmt.close();
        } catch ( SQLException e ) {
          // ignore
        }
      }
    }
  }

  /**
   * Converts query or DDL statement into this dialect.
   *
   * @param s SQL query or DDL statement
   * @return Query or DDL statement translated into this dialect
   */
  private String dialectize(Context context, String s ) {
    if ( dialect == null ) {
      dialect = getDialect(context);
    }
    final DatabaseProduct databaseProduct =
            getDatabaseProduct(dialect.getDialectName());

    // Some DBs require the schema to be a prfix of the table
    switch ( databaseProduct ) {
      case GOOGLEBIGQUERY:
        break;
      default:
        s = s.replace( "[foodmart.", "[" );
        break;
    }

    switch ( databaseProduct ) {
      case ACCESS:
        break;
      case HIVE:
        if ( s.contains( "UNION ALL" ) ) {
          s = "SELECT * FROM (" + s + ") x";
        }
        s = s.replace( '[', '`' );
        s = s.replace( ']', '`' );
        s = s.replaceAll( " as ", " " );
        break;
      case IMPALA:
        s = s.replace( "[", "" );
        s = s.replace( "]", "" );
        s = s.replaceAll( " as ", " " );
        break;
      case MARIADB:
      case MYSQL:
      case INFOBRIGHT:
        s = s.replace( '[', '`' );
        s = s.replace( ']', '`' );
        break;
      case ORACLE:
        s = s.replace( '[', '"' );
        s = s.replace( ']', '"' );
        s = s.replaceAll( " as ", " " );
        break;
      case INFORMIX:
        s = s.replace( "[", "" );
        s = s.replace( "]", "" );
        break;
      default:
        s = s.replace( "[", dialect.getQuoteIdentifierString() );
        s = s.replace( "]", dialect.getQuoteIdentifierString() );
        break;
    }

    return s;
  }

  /**
   * Asserts that a query succeeds and produces at least one row.
   *
   * @param sql SQL query in current dialect
   */
  protected void assertQuerySucceeds(Context context, String sql ) {
    Statement stmt = null;
    try {
      stmt = getConnection(context).createStatement();
      ResultSet resultSet = stmt.executeQuery( sql );
      assertTrue( resultSet.next() );
      Object col1 = resultSet.getObject( 1 );
//      discard( col1 );
    } catch ( SQLException e ) {
      throw Util.newInternal( e, "query [" + sql + "] failed" );
    } finally {
      if ( stmt != null ) {
        try {
          stmt.close();
        } catch ( SQLException e ) {
          // ignore
        }
      }
    }
  }

  /**
   * Asserts that a query fails.
   *
   * @param sql      SQL query
   * @param patterns Array of expected patterns, generally one for each SQL dialect for which the test is expected to
   *                 fail
   */
  protected void assertQueryFails(Context context, String sql, String[] patterns ) {
    Statement stmt = null;
    try {
      stmt = getConnection(context).createStatement();
      ResultSet resultSet;
      try {
        resultSet = stmt.executeQuery( sql );
      } catch ( SQLException e2 ) {
        // execution failed - good
        String message = e2.getMessage();
        for ( String pattern : patterns ) {
          if ( message.matches( pattern ) ) {
            return;
          }
        }
        throw new AssertionFailedError(
                "error [" + message
                        + "] did not match any of the supplied patterns" );
      }
      assertTrue( resultSet.next() );
      Object col1 = resultSet.getObject( 1 );
//      discard( col1 );
    } catch ( SQLException e ) {
      throw Util.newInternal( e, "failed in wrong place" );
    } finally {
      if ( stmt != null ) {
        try {
          stmt.close();
        } catch ( SQLException e ) {
          // ignore
        }
      }
    }
  }

  /**
   * Unit test for {@link Dialect#allowsSelectNotInGroupBy}.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAllowsSelectNotInGroupBy(Context context) throws SQLException {
    Dialect dialect = getDialect(context);
    String sql =
            "select "
                    + dialect.quoteIdentifier( "time_id" )
                    + ", "
                    + dialect.quoteIdentifier( "the_month" )
                    + " from "
                    + dialectizeTableName(context, dialect.quoteIdentifier( "time_by_day" ) )
                    + " group by "
                    + dialect.quoteIdentifier( "time_id" );
    if ( dialect.allowsSelectNotInGroupBy() ) {
      final ResultSet resultSet =
              getConnection(context).createStatement().executeQuery( sql );
      assertTrue( resultSet.next() );
      resultSet.close();
    } else {
      String[] errs = {
              // oracle
              "ORA-00979: not a GROUP BY expression\n",
              // derby
              "The SELECT list of a grouped query contains at least one "
                      + "invalid expression. If a SELECT list has a GROUP BY, the "
                      + "list may only contain valid grouping expressions and valid "
                      + "aggregate expressions.  ",
              // hive
              "(?s).*line 1:18 Expression Not In Group By Key `the_month`.*",
              // hsqldb
              "(?s)Not in aggregate function or group by clause: .*",
              // mysql (if sql_mode contains ONLY_FULL_GROUP_BY)
              "ERROR 1055 (42000): 'foodmart.time_by_day.the_month' isn't in "
                      + "GROUP BY",
              // access
              "\\[Microsoft\\]\\[ODBC Microsoft Access Driver\\] You tried "
                      + "to execute a query that does not include the specified "
                      + "expression 'the_month' as part of an aggregate function.",
              // luciddb
              "From line 1, column 19 to line 1, column 29: Expression "
                      + "'the_month' is not being grouped",
              // neoview
              ".* ERROR\\[4005\\] Column reference \"the_month\" must be a "
                      + "grouping column or be specified within an aggregate. .*",
              // teradata
              ".*Selected non-aggregate values must be part of the "
                      + "associated group.",
              // Greenplum & postgresql
              "(?s).*ERROR: column \"time_by_day.the_month\" must appear in "
                      + "the GROUP BY clause or be used in an aggregate function.*",
              // Vectorwise
              "line 1, The columns in the SELECT clause must be contained in the GROUP BY clause\\.",
              // MonetDB
              "SELECT: cannot use non GROUP BY column 'the_month' in query results without an aggregate function",
              // SQL Server 2008
              "Column 'time_by_day.the_month' is invalid in the select list because it is not contained in either an "
                      + "aggregate function or the GROUP BY clause.",
              // impala
              "(?s).*select list expression not produced by aggregation output.*missing from GROUP BY clause.*",
              // NuoDB
              "(?s).*scolumn mondrian.time_by_day.the_month must appear in the GROUP BY clause or be used in an aggregate "
                      + "function.*",
              // Vertica 6
              "(?s).*ERROR: Column \"time_by_day.the_month\" must appear in "
                      + "the GROUP BY clause or be used in an aggregate function.*",
              // BigQuery
              "(?s).*SELECT list expression references column the_month which is neither grouped nor aggregated.*",
              // Snowflake
              "(?s).*select clause is neither an aggregate nor in the group by clause.*"
      };
      assertQueryFails(context, sql, errs );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testHavingRequiresAlias(Context context) throws Exception {
    Dialect dialect = getDialect(context);
    StringBuilder sb =
            new StringBuilder(
                    "select upper("
                            + dialect.quoteIdentifier( "customer", "fname" )
                            + ") as c from "
                            + dialectizeTableName(context, dialect.quoteIdentifier( "customer" ) )
                            + " group by "
                            + dialect.quoteIdentifier( "customer", "fname" )
                            + " having "
                            + dialect.quoteIdentifier( "customer", "fname" )
                            + " LIKE " );
    dialect.quoteStringLiteral( sb, "%" );
    if ( !dialect.requiresHavingAlias() ) {
      final ResultSet resultSet =
              getConnection(context).createStatement().executeQuery( sb.toString() );
      assertTrue( resultSet.next() );
      resultSet.close();
    } else {
      String[] errs = {
              // mysql
              "Unknown column 'customer\\.fname' in 'having clause'",
              // vectorwise
              "No conversion defined for result data type\\.",
      };
      assertQueryFails(context, sb.toString(), errs );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAllowsRegularExpressionInWhereClause(Context context) throws Exception {
    Dialect dialect = getDialect(context);
    if ( dialect.allowsRegularExpressionInWhereClause() ) {
      assertNotNull(
              dialect.generateRegularExpression(
                      dialect.quoteIdentifier( "customer", "fname" ),
                      "(?i).*\\QJeanne\\E.*" ) );
      StringBuilder sb =
              new StringBuilder(
                      "select "
                              + dialect.quoteIdentifier( "customer", "fname" )
                              + " from "
                              + dialectizeTableName(context, dialect.quoteIdentifier( "customer" ) )
                              + " group by "
                              + dialect.quoteIdentifier( "customer", "fname" )
                              + " having "
                              + dialect.generateRegularExpression(
                              dialect.quoteIdentifier( "customer", "fname" ),
                              "(?i).*\\QJeanne\\E.*" ) );
      final ResultSet resultSet =
              getConnection(context).createStatement().executeQuery( sb.toString() );
      assertTrue( resultSet.next() );
      resultSet.close();
    } else {
      assertNull(
              dialect.generateRegularExpression(
                      "Foo",
                      "(?i).*\\QBar\\E.*" ) );
    }
  }

  /**
   * This is a test for
   * <a href="http://jira.pentaho.com/browse/MONDRIAN-1057">
   * http://jira.pentaho.com/browse/MONDRIAN-1057</a> Some dialects are not removing the \Q and \E markers if they are
   * in the middle of the regexp.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
  void testComplexRegularExpression(Context context) throws Exception {
    final String regexp =
            "(?i).*\\QJeanne\\E.*|.*\\QSheri\\E.*|.*\\QJonathan\\E.*|.*\\QJewel\\E.*";
    Dialect dialect = getDialect(context);
    if ( dialect.allowsRegularExpressionInWhereClause() ) {
      assertNotNull(
              dialect.generateRegularExpression(
                      dialect.quoteIdentifier( "customer", "fname" ),
                      regexp ) );
      StringBuilder sb =
              new StringBuilder(
                      "select "
                              + dialect.quoteIdentifier( "customer", "fname" )
                              + " from "
                              + dialectizeTableName(context, dialect.quoteIdentifier( "customer" ) )
                              + " group by "
                              + dialect.quoteIdentifier( "customer", "fname" )
                              + " having "
                              + dialect.generateRegularExpression(
                              dialect.quoteIdentifier( "customer", "fname" ),
                              regexp ) );
      final ResultSet resultSet =
              getConnection(context).createStatement().executeQuery( sb.toString() );
      int i = 0;
      while ( resultSet.next() ) {
        i++;
      }
      assertEquals( 7, i );
      resultSet.close();
    } else {
      assertNull(
              dialect.generateRegularExpression(
                      "Foo",
                      "(?i).*\\QBar\\E.*" ) );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRegularExpressionSqlInjection(Context context) throws SQLException {
    // bug mondrian-983
    // We know that mysql's dialect can handle this regex
    Throwable throwable = null;
    boolean couldTranslate = true;
    try {
      couldTranslate =
              checkRegex(context,
                      "(?i).*\\Qa\"\"\\); window.alert(\"\"woot');\\E.*" );
    } catch ( SQLException e ) {
      throwable = e;
    }
    switch ( getDatabaseProduct(getDialect(context).getDialectName()) ) {
      case MYSQL:
      case MARIADB:
        assertTrue( couldTranslate );
        assertNull( throwable );
        break;
      case GREENPLUM:
      case POSTGRES:
        assertNotNull( throwable );
        assertTrue( couldTranslate );
        assertTrue(
                throwable.getMessage().contains(
                        "ERROR: invalid regular expression:"
                                + " parentheses () not balanced" ), throwable.getMessage());
        break;
      case GOOGLEBIGQUERY:
        assertNotNull( throwable );
        assertTrue( couldTranslate );
        assertTrue(
                throwable.getMessage().contains(
                        "Error getting job status" ), throwable.getMessage());
        break;
      default:
        // As far as we know, all other databases either handle this regex
        // just fine or our dialect for that database refuses to translate
        // the regex to SQL.
        assertTrue( couldTranslate );
        assertNull( throwable );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testRegularExpressionSqlInjection_Slash(Context context) throws SQLException {
    // On mysql, this gives error:
    //   Got error 'repetition-operator operand invalid' from regexp
    //
    // Ideally, we would detect that the regex cannot be translated to
    // SQL (perhaps because it's not a valid java regex). Currently the
    // database gives an error, and that's better than nothing.
    Throwable throwable = null;
    boolean couldTranslate = true;
    try {
      couldTranslate =
              checkRegex(context,
                      "\"(?i).*\\Qa\"\"\\); window.alert(\"\"woot');\\E.*\"" );
    } catch ( SQLException e ) {
      throwable = e;
    }
    switch ( getDatabaseProduct(getDialect(context).getDialectName()) ) {
      case MYSQL:
        assertNull( throwable );
        assertTrue( couldTranslate );
        break;
      case MARIADB:
        assertNotNull( throwable );
        assertTrue( couldTranslate );
        assertTrue(
                throwable.getMessage().contains(
                        "Got error 'repetition-operator operand invalid' from "
                                + "regexp" ) );
        break;
      case GREENPLUM:
      case POSTGRES:
        assertNotNull( throwable );
        assertTrue( couldTranslate );
        assertTrue(
                throwable.getMessage().contains(
                        "ERROR: invalid regular expression: quantifier operand "
                                + "invalid" ), throwable.getMessage());
        break;
      case GOOGLEBIGQUERY:
        assertNotNull( throwable );
        assertTrue( couldTranslate );
        assertTrue(
                throwable.getMessage().contains(
                        "Error getting job status" ), throwable.getMessage());
        break;
      case SNOWFLAKE:
        assertNotNull( throwable );
        assertTrue( couldTranslate );
        assertTrue(
                throwable.getMessage().contains(
                        "Invalid regular expression" ), throwable.getMessage());
        break;
      default:
        // As far as we know, all other databases either handle this regex
        // just fine or our dialect for that database refuses to translate
        // the regex to SQL.
        assertTrue( couldTranslate );
        assertNull( throwable );
    }

    // Every dialect should refuse to translate an invalid regex.
    couldTranslate = checkRegex(context, "ab[cd" );
    assertFalse( couldTranslate );
  }

  /**
   * Translates a regular expression into SQL and executes the query. Returns whether the dialect was able to
   * translate
   * the regex.
   *
   * @param regex Java regular expression string
   * @return Whether dialect could translate regex to SQL.
   * @throws SQLException on error
   */
  private boolean checkRegex(Context context, String regex ) throws SQLException {
    Dialect dialect = getDialect(context);
    final StringBuilder sqlRegex =
            dialect.generateRegularExpression(
                    dialect.quoteIdentifier( "customer", "fname" ),
                    regex );
    if ( sqlRegex != null ) {
      String sql =
              "select * from "
                      + dialectizeTableName(context, dialect.quoteIdentifier( "customer" ) )
                      + " where "
                      + sqlRegex;
      final ResultSet resultSet =
              getConnection(context).createStatement().executeQuery( sql );
      assertFalse( resultSet.next() );
      resultSet.close();
      return true;
    } else {
      return false;
    }
  }


  @Disabled //disabled for CI build
  @Test
  void testPostgresGreenplumTypeMapQuirks() throws SQLException {
    MockResultSetMetadata mockResultSetMeta = new MockResultSetMetadata();
    Dialect greenplumDialect =
            getFakeDialect( DatabaseProduct.GREENPLUM );
    assertTrue(
            greenplumDialect.getType(
                    mockResultSetMeta.withColumnName( "m0" )
                            .withColumnType( Types.NUMERIC )
                            .withPrecision( 0 )
                            .withScale( 0 )
                            .build(),
                    0 ) == BestFitColumnType.OBJECT, "Postgres/Greenplum dialect NUMBER with precision =0, scale = 0"
                    + ", measure name starts with 'm' maps to OBJECT");
  }

  @Disabled //disabled for CI build
  @Test
  void testSnowflakeTypeMapQuirks() throws SQLException {
    MockResultSetMetadata mockResultSetMeta = new MockResultSetMetadata();
    Dialect greenplumDialect =
            getFakeDialect( DatabaseProduct.SNOWFLAKE );
    assertTrue(
            greenplumDialect.getType(
                    mockResultSetMeta
                            .withColumnType( Types.NUMERIC )
                            .withPrecision( 5 )
                            .withScale( 2 )
                            .build(),
                    0 ) == BestFitColumnType.DECIMAL, "Snowflake dialect NUMBER with precision =X, scale != 0"
                    + ", maps to DECIMAL");
  }

  @Disabled //disabled for CI build
  @Test
  void testNetezzaTypeMapQuirks() throws SQLException {
    MockResultSetMetadata mockResultSetMeta = new MockResultSetMetadata();
    Dialect netezzaDialect =
            getFakeDialect( DatabaseProduct.NETEZZA );
    assertTrue(
            netezzaDialect.getType(
                    mockResultSetMeta
                            .withColumnType( Types.NUMERIC )
                            .withPrecision( 38 )
                            .withScale( 0 )
                            .build(),
                    0 ) == BestFitColumnType.DOUBLE, "Netezza dialect NUMERIC/DECIMAL with precision =38, scale = 0"
                    + " means long.  Should be mapped to DOUBLE");
    assertTrue(
            netezzaDialect.getType(
                    mockResultSetMeta
                            .withColumnType( Types.DECIMAL )
                            .withPrecision( 38 )
                            .withScale( 0 )
                            .build(),
                    0 ) == BestFitColumnType.DOUBLE, "Netezza dialect NUMERIC/DECIMAL with precision =38, scale = 0"
                    + " means long.  Should be mapped to DOUBLE");
  }

  @Disabled //disabled for CI build
  @Test
  void testMonetDBTypeMapQuirks() throws SQLException {
    MockResultSetMetadata mockResultSetMeta = new MockResultSetMetadata();
    Dialect monetDbDialect =
            getFakeDialect( DatabaseProduct.MONETDB );
    assertTrue(
            monetDbDialect.getType(
                    mockResultSetMeta
                            .withColumnType( Types.NUMERIC )
                            .withPrecision( 0 )
                            .withScale( 0 )
                            .build(),
                    0 ) == BestFitColumnType.DOUBLE, "MonetDB dialect NUMERIC with precision =0, scale = 0"
                    + " may be an aggregated decimal, should assume DOUBLE");
  }

  @Test
  void testJdbcDialectTypeMap() throws SQLException {
    MockResultSetMetadata mockResultSetMeta = new MockResultSetMetadata();
    Dialect postgresDialect = new JdbcDialectImpl(){
        @Override
        public String getDialectName() {
            return null;
        }

    };
    assertTrue(
            postgresDialect.getType(
                    mockResultSetMeta
                            .withColumnType( Types.NUMERIC )
                            .withPrecision( 5 )
                            .withScale( 0 )
                            .build(),
                    0 ) == BestFitColumnType.INT, "JdbcDialectImpl NUMERIC/DECIMAL types w/ precision 0-9"
                    + " and scale=0 should return INT");
    assertTrue(
            postgresDialect.getType(
                    mockResultSetMeta
                            .withColumnType( Types.DECIMAL )
                            .withPrecision( 5 )
                            .withScale( 0 )
                            .build(),
                    0 ) == BestFitColumnType.INT, "JdbcDialectImpl NUMERIC/DECIMAL types w/ precision 0-9"
                    + " and scale=0 should return INT");
  }



  @Disabled //disabled for CI build
  @Test
  void testHiveTimestampQuoteLiteral() throws SQLException {
    /*MONDRIAN-2208*/
    Dialect hiveDbDialect =
            getFakeDialect( DatabaseProduct.HIVE );
    StringBuilder buf = new StringBuilder();
    hiveDbDialect.quoteTimestampLiteral( buf, "2014-10-29 10:27:55.12" );
    assertEquals(
    		"cast( '2014-10-29 10:27:55.12' as timestamp )",
    		buf.toString(),
    		"TIMESTAMP literal for Hive requires special syntax (cast)");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testQuoteIdentifierForDividedByDot(Context context) {
    final String TABLE_NAME = "table.one";
    final String FIELD_NAME = "field.one";
    String q = getDialect(context).getQuoteIdentifierString();
    String res = getDialect(context).quoteIdentifier( TABLE_NAME, FIELD_NAME );
    assertEquals( q + TABLE_NAME + q + "." + q + FIELD_NAME + q, res );
  }

  public static class MockResultSetMetadata
          extends DelegatingInvocationHandler {
    private int precision;    private int scale;
    private int columnType;
    private String columnName;

    public MockResultSetMetadata withPrecision( int setPrecision ) {
      precision = setPrecision;
      return this;
    }

    public MockResultSetMetadata withScale( int setScale ) {
      scale = setScale;
      return this;
    }

    public MockResultSetMetadata withColumnType( int setColumnType ) {
      columnType = setColumnType;
      return this;
    }

    public MockResultSetMetadata withColumnName( String setColumnName ) {
      columnName = setColumnName;
      return this;
    }

    public ResultSetMetaData build() {
      return (ResultSetMetaData) Proxy.newProxyInstance(
              this.getClass().getClassLoader(),
              new Class[] { ResultSetMetaData.class },
              this );
    }

    /**
     * Proxy for {@link ResultSetMetaData#getPrecision(int)}.
     */
    public int getPrecision( int column ) throws SQLException {
      return precision;
    }

    /**
     * Proxy for {@link ResultSetMetaData#getPrecision(int)}.
     */
    public String getColumnName( int column ) throws SQLException {
      return columnName;
    }

    /**
     * Proxy for {@link ResultSetMetaData#getPrecision(int)}.
     */
    public int getColumnType( int column ) throws SQLException {
      return columnType;
    }

    /**
     * Proxy for {@link ResultSetMetaData#getPrecision(int)}.
     */
    public int getScale( int column ) throws SQLException {
      return scale;
    }
  }

  //TODO; this tests more SqlQuery. so move it there
  @Test
  void testMondrian2253() throws SQLException {

    String expected = "    1 ASC";
    // "1" is supposed to be a column number
    String expr = "1";
    Dialect dialect = new JdbcDialectImpl() {

        @Override
        public boolean requiresUnionOrderByOrdinal() {
            return false;
        }

        @Override
        public String getDialectName() {
            return null;
        }
    };

    SqlQuery query = new SqlQuery( dialect, true );
    query.addOrderBy(
            expr, null, true, false,
            dialect.requiresUnionOrderByOrdinal(), true );

    assertTrue( query.toString().contains( expected ) );
  }
}
