/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * History:
 *  This files came from the mondrian project. Some of the Flies
 *  (mostly the Tests) did not have License Header.
 *  But the Project is EPL Header. 2002-2022 Hitachi Vantara.
 *
 * Contributors:
 *   Hitachi Vantara.
 *   SmartCity Jena - initial  Java 8, Junit5
 */
package org.opencube.junit5;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.olap.api.CacheControl;
import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.ConnectionProps;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Quoting;
import org.eclipse.daanse.olap.api.Segment;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.profile.ProfilingCalc;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.CellSet;
import org.eclipse.daanse.olap.api.result.CellSetAxis;
import org.eclipse.daanse.olap.api.result.Position;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.calc.base.profile.SimpleCalculationProfileWriter;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;
import org.eclipse.daanse.olap.impl.CoordinateIterator;
import org.eclipse.daanse.olap.impl.TraditionalCellSetFormatter;
import org.eclipse.daanse.olap.query.component.IdImpl;
import org.eclipse.daanse.olap.rolap.api.RolapContext;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.context.TestContextImpl;

import mondrian.enums.DatabaseProduct;
import mondrian.olap.SystemWideProperties;
import mondrian.olap.Util;
import mondrian.olap.fun.FunUtil;
import mondrian.rolap.MemberCacheHelper;
import mondrian.rolap.RolapCube;
import mondrian.rolap.RolapHierarchy;
import mondrian.rolap.RolapUtil;
import mondrian.rolap.SmartMemberReader;
import mondrian.server.ExecutionImpl;
import mondrian.test.SqlPattern;
import mondrian.util.DelegatingInvocationHandler;

//import mondrian.spi.DialectManager;

public class TestUtil {

    protected static final String nl = Util.NL;
	private static final String lineBreak = "\"," + nl + "\"";
	  /**
	   * Executes the expression in the context of the cube indicated by
	   * <code>cubeName</code>, and returns the result as a Cell.
	   *
	   * @param expression The expression to evaluate
	   * @return Cell which is the result of the expression
	   */
	  public static Cell executeExprRaw(Connection connection, String cubeName , String expression ) {
	    final String queryString = generateExpression( cubeName,expression );
	    Result result = executeQuery( connection,queryString );
	    return result.getCell( new int[] { 0 } );
	  }

	  public static String generateExpression(String cubeName , String expression ) {
	    if ( cubeName.indexOf( ' ' ) >= 0 ) {
	      cubeName = Util.quoteMdxIdentifier( cubeName );
	    }
	    return
	      "with member [Measures].[Foo] as "
	        + Util.singleQuoteString( expression )
	        + " select {[Measures].[Foo]} on columns from " + cubeName;
	  }

	   public static TupleList productMembersPotScrubbersPotsAndPans(
		        CatalogReader salesCubeCatalogReader)
		    {
		        return new UnaryTupleList(Arrays.asList(
		            member(
		                IdImpl.toList(
		                    "Product", "All Products", "Non-Consumable", "Household",
		                    "Kitchen Products", "Pot Scrubbers", "Cormorant"),
		                salesCubeCatalogReader),
		            member(
		                IdImpl.toList(
		                    "Product", "All Products", "Non-Consumable", "Household",
		                    "Kitchen Products", "Pot Scrubbers", "Denny"),
		                salesCubeCatalogReader),
		            member(
		                IdImpl.toList(
		                    "Product", "All Products", "Non-Consumable", "Household",
		                    "Kitchen Products", "Pot Scrubbers", "Red Wing"),
		                salesCubeCatalogReader),
		            member(
		                IdImpl.toList(
		                    "Product", "All Products", "Non-Consumable", "Household",
		                    "Kitchen Products", "Pots and Pans", "Cormorant"),
		                salesCubeCatalogReader),
		            member(
		                IdImpl.toList(
		                    "Product", "All Products", "Non-Consumable", "Household",
		                    "Kitchen Products", "Pots and Pans", "Denny"),
		                salesCubeCatalogReader),
		            member(
		                IdImpl.toList(
		                    "Product", "All Products", "Non-Consumable", "Household",
		                    "Kitchen Products", "Pots and Pans", "High Quality"),
		                salesCubeCatalogReader),
		            member(
		                IdImpl.toList(
		                    "Product", "All Products", "Non-Consumable", "Household",
		                    "Kitchen Products", "Pots and Pans", "Red Wing"),
		                salesCubeCatalogReader),
		            member(
		                IdImpl.toList(
		                    "Product", "All Products", "Non-Consumable", "Household",
		                    "Kitchen Products", "Pots and Pans", "Sunset"),
		                salesCubeCatalogReader)));
		    }
	    public static Member member(
	            List<Segment> segmentList,
	            CatalogReader salesCubeCatalogReader)
	        {
	            return salesCubeCatalogReader.getMemberByUniqueName(segmentList, true);
	        }
	    /**
	     * Executes a query with a given expression on an axis, and asserts that it throws an error which matches a particular
	     * pattern. The expression is evaulated against the default cube.
	     */
	    public static void assertAxisThrows(Connection connection,
	      String expression,
	      String pattern ,
	      String cubeName) {
	      Throwable throwable = null;
	      try {
	        final String queryString =
	          "select {" + expression + "} on columns from " + cubeName;
	        executeQuery(connection, queryString);
	      } catch ( Throwable e ) {
	        throwable = e;
	      }
	      checkThrowable( throwable, pattern );
	    }

		/**
		 * Executes a query, and asserts that it throws an exception which contains the
		 * given pattern.
		 *
		 * @param queryString Query string
		 * @param pattern     Pattern which exception must match
		 */
		public static void assertQueryThrows(Connection connection, String queryString, String pattern) {
			Throwable throwable;
			try {
				Result result = executeQuery(connection, queryString);
//				discard(result);
				throwable = null;
			} catch (Throwable e) {
				throwable = e;
			}
			checkThrowable(throwable, pattern);
		}

	/**
	 * Executes a query, and asserts that it throws an exception which contains the
	 * given pattern.
	 *
	 * @param queryString Query string
	 * @param pattern     Pattern which exception must match
	 */
	public static void assertQueryThrows(Context context, String queryString, String pattern) {
		Throwable throwable;
		try {
			Result result = executeQuery(context.getConnectionWithDefaultRole(), queryString);
//			discard(result);
			throwable = null;
		} catch (Throwable e) {
			throwable = e;
		}
		checkThrowable(throwable, pattern);
	}

    public static void assertQueryThrows(Context context, List<String> roles, String queryString, String pattern) {
        Throwable throwable;
        try {
            Result result = executeQuery(((TestContext)context).getConnection(roles), queryString);
//            discard(result);
            throwable = null;
        } catch (Throwable e) {
            throwable = e;
        }
        checkThrowable(throwable, pattern);
    }

    /**
     * Executes a query, and asserts that it throws an exception which contains the
     * given pattern.
     *
     * @param queryString Query string
     * @param pattern     Pattern which exception must match
     */
    public static void assertQueryThrows(Context context, ConnectionProps props, String queryString, String pattern) {
        Throwable throwable;
        try {
            Result result = executeQuery(context.getConnection(props), queryString);
//            discard(result);
            throwable = null;
        } catch (Throwable e) {
            throwable = e;
        }
        checkThrowable(throwable, pattern);
    }

    /**
		 * Executes an expression, and asserts that it gives an error which contains a
		 * particular pattern. The error might occur during parsing, or might be
		 * contained within the cell value.
		 */
		public static void assertExprThrows(Connection connection, String cubeName, String expression, String pattern) {
			Throwable throwable = null;
			try {
				if (cubeName.indexOf(' ') >= 0) {
					cubeName = Util.quoteMdxIdentifier(cubeName);
				}
				expression = expression.replace("'", "''");
				Result result = executeQuery(connection, "with member [Measures].[Foo] as '" + expression
						+ "' select {[Measures].[Foo]} on columns from " + cubeName);
				Cell cell = result.getCell(new int[] { 0 });
				if (cell.isError()) {
					throwable = (Throwable) cell.getValue();
				}
			} catch (Throwable e) {
				throwable = e;
			}
			checkThrowable(throwable, pattern);
		}

	/**
	 * Executes an expression, and asserts that it gives an error which contains a
	 * particular pattern. The error might occur during parsing, or might be
	 * contained within the cell value.
	 */
	public static void assertExprThrows(Context context, String cubeName, String expression, String pattern) {
		Throwable throwable = null;
		try {
			if (cubeName.indexOf(' ') >= 0) {
				cubeName = Util.quoteMdxIdentifier(cubeName);
			}
			expression = expression.replace("'", "''");
			Result result = executeQuery(context.getConnectionWithDefaultRole(), "with member [Measures].[Foo] as '" + expression
					+ "' select {[Measures].[Foo]} on columns from " + cubeName);
			Cell cell = result.getCell(new int[] { 0 });
			if (cell.isError()) {
				throwable = (Throwable) cell.getValue();
			}
		} catch (Throwable e) {
			throwable = e;
		}
		checkThrowable(throwable, pattern);
	}

    /**
		 * Checks that an actual string matches an expected string.
		 *
		 * <p>
		 * If they do not, throws a {@link junit.framework.ComparisonFailure} and prints
		 * the difference, including the actual string as an easily pasted Java string
		 * literal.
		 */
		public static void assertEqualsVerbose(String expected, String actual) {
			assertEqualsVerbose(expected, actual, true, null);
		}

		/**
		 * Checks that an actual string matches an expected string.
		 *
		 * <p>
		 * If they do not, throws a {@link ComparisonFailure} and prints the difference,
		 * including the actual string as an easily pasted Java string literal.
		 *
		 * @param expected Expected string
		 * @param actual   Actual string
		 * @param java     Whether to generate actual string as a Java string literal if
		 *                 the values are not equal
		 * @param message  Message to display, optional
		 */
		public static void assertEqualsVerbose(String expected, String actual, boolean java, String message) {
			assertEqualsVerbose(fold(expected), actual, java, message);
		}

	/**
	 * Returns count copies of a string. Format strings within string are substituted, per {@link
	 * java.lang.String#format}.
	 *
	 * @param count  Number of copies
	 * @param format String template
	 * @return Multiple copies of a string
	 */
	public static String repeatString(
			final int count,
			String format ) {
		final Formatter formatter = new Formatter();
		for ( int i = 0; i < count; i++ ) {
			formatter.format( format, i );
		}
		return formatter.toString();
	}

	public static void assertSqlEquals(Connection connection,
			String expectedSql,
			String actualSql,
			int expectedRows ) {
		// if the actual SQL isn't in the current dialect we have some
		// problems... probably with the dialectize method
		assertEqualsVerbose( actualSql, dialectize(connection, actualSql));

		String transformedExpectedSql = removeQuotes( dialectize(connection, expectedSql))
				.replaceAll( "\r\n", "\n" );
		String transformedActualSql = removeQuotes( actualSql )
				.replaceAll( "\r\n", "\n" );
		assertEquals( transformedExpectedSql, transformedActualSql );

		checkSqlAgainstDatasource(connection, actualSql, expectedRows );
	}

	private static void checkSqlAgainstDatasource(Connection connection,
			String actualSql,
			int expectedRows ) {

		java.sql.Connection jdbcConn = null;
		java.sql.Statement stmt = null;
		ResultSet rs = null;

		try {

			jdbcConn = connection.getDataSource().getConnection();
			stmt = jdbcConn.createStatement();

			if ( RolapUtil.SQL_LOGGER.isDebugEnabled() ) {
				StringBuffer sqllog = new StringBuffer();
				sqllog.append( "mondrian.test.TestContext: executing sql [" );
				if ( actualSql.indexOf( '\n' ) >= 0 ) {
					// SQL appears to be formatted as multiple lines. Make it
					// start on its own line.
					sqllog.append( "\n" );
				}
				sqllog.append( actualSql );
				sqllog.append( ']' );
				RolapUtil.SQL_LOGGER.debug( sqllog.toString() );
			}

			long startTime = System.currentTimeMillis();
			rs = stmt.executeQuery( actualSql );
			long time = System.currentTimeMillis();
			final long execMs = time - startTime;

			RolapUtil.SQL_LOGGER.debug( ", exec " + execMs + " ms" );

			int rows = 0;
			while ( rs.next() ) {
				rows++;
			}

			assertEquals(expectedRows, rows, "row count");
		} catch ( SQLException e ) {
			throw new RuntimeException(
					"ERROR in SQL - invalid for database: "
							+ ""
							+ "\n" + actualSql,
					e );
		} finally {
			try {
				if ( rs != null ) {
					rs.close();
				}
			} catch ( Exception e1 ) {
				// ignore
			}
			try {
				if ( stmt != null ) {
					stmt.close();
				}
			} catch ( Exception e1 ) {
				// ignore
			}
			try {
				if ( jdbcConn != null ) {
					jdbcConn.close();
				}
			} catch ( Exception e1 ) {
				// ignore
			}
		}
	}

	private static String removeQuotes( String actualSql ) {
		String transformedActualSql = actualSql.replaceAll( "`", "" );
		transformedActualSql = transformedActualSql.replaceAll( "\"", "" );
		return transformedActualSql;
	}

	/**
	 * Converts a SQL string into the current dialect.
	 *
	 * <p>This is not intended to be a general purpose method: it looks for
	 * specific patterns known to occur in tests, in particular "=as=" and "fname + ' ' + lname".
	 *
	 * @param sql SQL string in generic dialect
	 * @return SQL string converted into current dialect
	 */
	private static String dialectize(Connection connection, String sql ) {
		final String search = "fname \\+ ' ' \\+ lname";
		final Dialect dialect = getDialect(connection);
		final DatabaseProduct databaseProduct =
				getDatabaseProduct(dialect.getDialectName());
		switch ( databaseProduct ) {
			case MYSQL:
			case MARIADB:
				// Mysql would generate "CONCAT(...)"
				sql = sql.replaceAll(
						search,
						"CONCAT(`customer`.`fname`, ' ', `customer`.`lname`)" );
				break;
			case POSTGRES:
			case ORACLE:
			case LUCIDDB:
			case TERADATA:
				sql = sql.replaceAll(
						search,
						"`fname` || ' ' || `lname`" );
				break;
			case DERBY:
				sql = sql.replaceAll(
						search,
						"`customer`.`fullname`" );
				break;
			case INGRES:
				sql = sql.replaceAll(
						search,
						"fullname" );
				break;
			case DB2:
			case DB2_AS400:
			case DB2_OLD_AS400:
				sql = sql.replaceAll(
						search,
						"CONCAT(CONCAT(`customer`.`fname`, ' '), `customer`.`lname`)" );
				break;
		}

		if ( getDatabaseProduct(dialect.getDialectName()) == DatabaseProduct.ORACLE ) {
			// " + tableQualifier + "
			sql = sql.replaceAll( " =as= ", " " );
		} else {
			sql = sql.replaceAll( " =as= ", " as " );
		}
		return sql;
	}

	public static ResultSet executeStatement(Connection connection, String queryString ) throws SQLException {
        org.eclipse.daanse.olap.api.Statement stmt = connection.createStatement();
		return stmt.executeQuery( queryString, Optional.empty(), null );
	}





	public static void assertParameterizedExprReturns(Connection connection, String cubeName,
			String expr,
			String expected,
			Object... paramValues ) {
		String queryString = generateExpression(cubeName, expr);
		Query query = connection.parseQuery( queryString );
		assert paramValues.length % 2 == 0;
		for ( int i = 0; i < paramValues.length; ) {
			final String paramName = (String) paramValues[ i++ ];
			final Object value = paramValues[ i++ ];
			query.setParameter( paramName, value );
		}
		final Result result = connection.execute( query );
		final Cell cell = result.getCell( new int[] { 0 } );

		if ( expected == null ) {
			expected = ""; // null values are formatted as empty string
		}
		assertEqualsVerbose( expected, cell.getFormattedValue() );
	}

	/**
	 * Reverses the effect of {@link #fold}; converts platform-specific line endings in a string info linefeeds.
	 *
	 * @param string String where all linefeeds have been converted to platform-specific (CR+LF on Windows, LF on
	 *               Unix/Linux)
	 * @return String where line endings are represented as linefeed "\n"
	 */
	public static String unfold( String string ) {
		if ( !nl.equals( "\n" ) ) {
			string = string.replace(nl, "\n" );
		}
		if ( string == null ) {
			return null;
		} else {
			return string;
		}
	}

	/**
	 * Creates a FoodMart connection with "Ignore=true" and returns the list of warnings in the schema.
	 *
	 * @return Warnings encountered while loading schema
	 */
	public static List<Exception> getSchemaWarnings(Context context) {
		//final Util.PropertyList propertyList =
		//		getConnectionProperties().clone();
		//propertyList.put(
		//		RolapConnectionProperties.Ignore.name(),
		//		"true" );
        //TODO
		//context.setProperty(RolapConnectionProperties.Ignore.name(),
		//		"true");
		final Connection connection = context.getConnectionWithDefaultRole();
				//withProperties( propertyList ).getConnection();
		return connection.getCatalog().getWarnings();
	}

	/**
	 * Creates a dialect without using a connection.
	 *
	 * @param product Database product
	 * @return dialect of an required persuasion
	 */
	public static Dialect getFakeDialect( DatabaseProduct product ) {
		final DatabaseMetaData metaData =
				(DatabaseMetaData) Proxy.newProxyInstance(
						TestUtil.class.getClassLoader(),
						new Class<?>[] { DatabaseMetaData.class },
						new DatabaseMetaDataInvocationHandler( product ) );
		final java.sql.Connection connection =
				(java.sql.Connection) Proxy.newProxyInstance(
						TestUtil.class.getClassLoader(),
						new Class<?>[] { java.sql.Connection.class },
						new ConnectionInvocationHandler( metaData ) );
        //TODO Commented by reason context implementation
		//final Dialect dialect = DialectManager.createDialect( null, connection );
        final Dialect dialect = null;
        assert getDatabaseProduct(dialect.getDialectName()) == product;
		return dialect;
	}

	public static boolean databaseIsValid(Connection connection, String cubeName) {
		try {
			if ( cubeName.indexOf( ' ' ) >= 0 ) {
				cubeName = Util.quoteMdxIdentifier( cubeName );
			}
			Query query = connection.parseQuery( "select from " + cubeName );
			Result result = connection.execute( query );
//			discard( result );
			connection.close();
			return true;
		} catch ( RuntimeException e ) {
//			discard( e );
			return false;
		}
	}


	public static class ConnectionInvocationHandler
			extends DelegatingInvocationHandler {
		private final DatabaseMetaData metaData;

		ConnectionInvocationHandler( DatabaseMetaData metaData ) {
			this.metaData = metaData;
		}

		/**
		 * Proxy for {@link java.sql.Connection#getMetaData()}.
		 */
		public DatabaseMetaData getMetaData() {
			return metaData;
		}

		/**
		 * Proxy for {@link java.sql.Connection#createStatement()}
		 */
		public java.sql.Statement createStatement() throws SQLException {
			throw new SQLException();
		}
	}

	// Public only because required for reflection to work.
	@SuppressWarnings( "UnusedDeclaration" )
	public static class DatabaseMetaDataInvocationHandler
			extends DelegatingInvocationHandler {
		private final DatabaseProduct product;

		DatabaseMetaDataInvocationHandler(
				DatabaseProduct product ) {
			this.product = product;
		}

		/**
		 * Proxy for {@link DatabaseMetaData#supportsResultSetConcurrency(int, int)}.
		 */
		public boolean supportsResultSetConcurrency( int type, int concurrency ) {
			return false;
		}

		/**
		 * Proxy for {@link DatabaseMetaData#getDatabaseProductName()}.
		 */
		public String getDatabaseProductName() {
			switch ( product ) {
				case GREENPLUM:
					return "postgres greenplum";
				default:
					return product.name();
			}
		}

		/**
		 * Proxy for {@link DatabaseMetaData#getIdentifierQuoteString()}.
		 */
		public String getIdentifierQuoteString() {
			return "\"";
		}

		/**
		 * Proxy for {@link DatabaseMetaData#getDatabaseProductVersion()}.
		 */
		public String getDatabaseProductVersion() {
			return "1.0";
		}

		/**
		 * Proxy for {@link DatabaseMetaData#isReadOnly()}.
		 */
		public boolean isReadOnly() {
			return true;
		}

		/**
		 * Proxy for {@link DatabaseMetaData#getMaxColumnNameLength()}.
		 */
		public int getMaxColumnNameLength() {
			return 30;
		}

		/**
		 * Proxy for {@link DatabaseMetaData#getDriverName()}.
		 */
		public String getDriverName() {
			switch ( product ) {
				case GREENPLUM:
					return "Mondrian fake dialect for Greenplum";
				default:
					return "Mondrian fake dialect";
			}
		}
	}

	/**
		 * Wrapper around a string that indicates that all line endings have been
		 * converted to platform-specific line endings.
		 *
		 * @see NotUseOldTestContext#fold
		 */
		public static class SafeString {
			public final String s;

			private SafeString(String s) {
				this.s = s;
			}
		}

		/**
		 * Replaces line-endings in a string with the platform-dependent equivalent. If
		 * the input string already has platform-dependent line endings, no replacements
		 * are made.
		 *
		 * @param string String whose line endings are to be made platform- dependent.
		 *               Typically these are constant "expected value" string
		 *               expressions where the linefeed is represented as linefeed "\n",
		 *               but sometimes this method will receive strings created
		 *               dynamically where the line endings are already appropriate for
		 *               the platform.
		 * @return String where all linefeeds have been converted to platform-specific
		 *         (CR+LF on Windows, LF on Unix/Linux)
		 */
		public static SafeString fold(String string) {
			if (string == null) {
				return null;
			}
			if (nl.equals("\n") || string.indexOf(nl) != -1) {
				return new SafeString(string);
			}
			return new SafeString(string.replace( "\n", nl));
		}

		private static final String indent = "                ";
		private static final Pattern LineBreakPattern = Pattern.compile("\r\n|\r|\n");
		private static final Pattern TabPattern = Pattern.compile("\t");
		private static final String lineBreak2 = "\\\\n\"" + nl + indent + "+ \"";

		private static String toJavaString(String s) {
			// Convert [string with "quotes" split
			// across lines]
			// into ["string with \"quotes\" split\n"
			// + "across lines
			//
			s = s.replace("\"", "\\\"");
			s = LineBreakPattern.matcher(s).replaceAll(lineBreak2);
			s = TabPattern.matcher(s).replaceAll("\\\\t");
			s = "\"" + s + "\"";
			String spurious = nl + indent + "+ \"\"";
			if (s.endsWith(spurious)) {
				s = s.substring(0, s.length() - spurious.length());
			}
			return s;
		}

		/**
		 * Checks that an actual string matches an expected string.
		 *
		 * <p>
		 * If they do not, throws a {@link ComparisonFailure} and prints the difference,
		 * including the actual string as an easily pasted Java string literal.
		 *
		 * @param safeExpected Expected string, where all line endings have been
		 *                     converted into platform-specific line endings
		 * @param actual       Actual string
		 * @param java         Whether to generate actual string as a Java string
		 *                     literal if the values are not equal
		 * @param message      Message to display, optional
		 */
		public static void assertEqualsVerbose(SafeString safeExpected, String actual, boolean java, String message) {
			String expected = safeExpected == null ? null : safeExpected.s;
			if ((expected == null) && (actual == null)) {
				return;
			}
			if ((expected != null) && expected.equals(actual)) {
				return;
			}
			if (message == null) {
				message = "";
			} else {
				message += nl;
			}
			message += "Expected:" + nl + expected + nl + "Actual:" + nl + actual + nl;
			if (java) {
				message += "Actual java:" + nl + toJavaString(actual) + nl;
			}
			assertEquals(expected, actual, message);
		}

		/**
		 * Executes an expression and asserts that it returns a given result.
		 */
		public static void assertExprReturns(Connection connection, String cubeName, String expression, String expected) {
			final Cell cell = executeExprRaw(connection, cubeName, expression);
			if (expected == null) {
				expected = ""; // null values are formatted as empty string
			}
			assertEqualsVerbose(expected, cell.getFormattedValue());
		}

	    public static void checkThrowable( Throwable throwable, String pattern ) {
	      if ( throwable == null ) {
	        fail( "query did not yield an exception" );
	      }
	      String stackTrace = getStackTrace( throwable );
	      if ( stackTrace.indexOf( pattern ) < 0 ) {
	    	  fail(
	          "query's error does not match pattern '" + pattern
	            + "'; error is [" + stackTrace + "]" );
	      }
	    }
	    /**
	     * Converts a {@link Throwable} to a stack trace.
	     */
	    //TODO: using stream api
	    public static String getStackTrace( Throwable e ) {
	    	ByteArrayOutputStream out = new ByteArrayOutputStream();
	        e.printStackTrace(new PrintStream(out));
	        return new String(out.toByteArray());
	    }

	public static Cube cubeByName(Connection connection, String cubeName) {
        CatalogReader reader = connection.getCatalogReader().withLocus();

        List<Cube> cubes = reader.getCubes();
        return cubeByName(cubeName, cubes);
    }

    public static Cube cubeByName(String cubeName, List<Cube> cubes) {
        Cube resultCube = null;
        for (Cube cube : cubes) {
            if (cubeName.equals(cube.getName())) {
                resultCube = cube;
                break;
            }
        }
        return resultCube;
    }


	public static synchronized void flushSchemaCache(Connection connection) {
		// it's pointless to flush the schema cache if we
		// have a handle on the connection object already

		if (connection == null) {
			return;
		}

		CacheControl cc = connection.getCacheControl(null);

		if (cc == null) {
			return;
		}
		cc.flushSchemaCache();
	}

	public static Result executeQuery(Connection connection, String queryString) {
		return executeQuery(connection, queryString, 300000l);
	}

	public static Result executeQueryTimeoutTest(Connection connection, String queryString ) {
	    Query query = connection.parseQuery( queryString );
	    Statement statement = query.getStatement();
	    assertThat(statement).isNotNull();
	    final Result result = statement.getMondrianConnection().execute(new ExecutionImpl(statement, Optional.of(Duration.ofMillis(statement.getQueryTimeoutMillis()))));
	    return result;
	  }

	public static Result executeQuery(Connection connection, String queryString, long timeoutIntervalMillis) {
		Query query = parseQuery(connection, queryString);
		assertThat(query).isNotNull();
		Statement statement = query.getStatement();
		assertThat(statement).isNotNull();

		Result result = statement.getMondrianConnection().execute(new ExecutionImpl(statement, Optional.of(Duration.ofMillis(timeoutIntervalMillis))));
		return result;
	}

	public static Query parseQuery(Connection connection, String queryString) {

		assertThat(connection).isNotNull();
		Query query = connection.parseQuery(queryString);
		return query;
	}

	/**
	 * Executes a query with a given expression on an axis, and returns the whole
	 * axis.
	 */
	public static Axis executeAxis(Connection connection, String cubeName, String expression) {
		Result result = executeQuery(connection, "select {" + expression + "} on columns from " + cubeName);
		return result.getAxes()[0];
	}

	/**
	 * Converts a set of positions into a string. Useful if you want to check that
	 * an axis has the results you expected.
	 */
	public static String toString(List<Position> positions) {
		StringBuilder buf = new StringBuilder();
		int i = 0;
		for (Position position : positions) {
			if (i > 0) {
				buf.append(nl);
			}
			if (position.size() != 1) {
				buf.append("{");
			}
			for (int j = 0; j < position.size(); j++) {
				Member member = position.get(j);
				if (j > 0) {
					buf.append(", ");
				}
				buf.append(member.getUniqueName());
			}
			if (position.size() != 1) {
				buf.append("}");
			}
			i++;
		}
		return buf.toString();
	}


	public static String toString( CellSet cellSet ) {
		final StringWriter sw = new StringWriter();
		new TraditionalCellSetFormatter().format(
				cellSet,
				new PrintWriter( sw ) );
		return sw.toString();
	}

    /**
	 * Converts a {@link Result} to text in traditional format.
	 *
	 * <p>
	 * For more exotic formats, see {@link CellSetFormatter}.
	 *
	 * @param result Query result
	 * @return Result as text
	 */
	public static String toString(Result result) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		result.print(pw);
		pw.flush();
		return sw.toString();
	}

	/**
	 * Executes a query with a given expression on an axis, and asserts that it
	 * returns the expected string.
	 */
	public static void assertAxisReturns(Connection connection, String cubeName, String expression, String expected) {
		Axis axis = executeAxis(connection, cubeName, expression);
		assertEqualsVerbose(expected, upgradeActual(toString(axis.getPositions())));
	}

	/**
	 * Massages the actual result of executing a query to handle differences in
	 * unique names betweeen old and new behavior.
	 *
	 * <p>
	 * Even though the new naming is not enabled by default, reference logs should
	 * be in terms of the new naming.
	 *
	 * @param actual Actual result
	 * @return Expected result massaged for backwards compatibility
	 * @see mondrian.olap.SystemWideProperties#SsasCompatibleNaming
	 */
	public static String upgradeActual(String actual) {
		return actual;
	}

	public static void assertQueryReturns(Connection connection, String queryString, String expectedResult, long timeoutIntervalMillis) {

		Result result = executeQuery(connection, queryString, timeoutIntervalMillis);

		assertThat(result).isNotNull();

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		// TODO: switch so other than printwriter
		result.print(pw);

		assertThat(sw.getBuffer().toString()).isNotNull().isEqualTo(expectedResult);

	}

	public static void assertQueryReturns(Connection connection, String queryString, String expectedResult) {
		assertQueryReturns(connection, queryString, expectedResult, 600000l);
	}

	/**
	 * Executes a query and checks that the result is a given string, displaying a
	 * message if result does not match desiredResult.
	 */
	public static void assertQueryReturns(Connection connection, String message, String query, String desiredResult) {
		Result result = executeQuery(connection, query);
		String resultString = toString(result);
		if (desiredResult != null) {
			assertEqualsVerbose(desiredResult, upgradeActual(resultString), true, message);
		}
	}

	public static org.eclipse.daanse.olap.api.result.CellSet executeOlap4jQuery(Connection connection, String queryString ) throws SQLException {
	//TODO: may better fix querys then use upgradeQuery
	  //  queryString = upgradeQuery( queryString );

		assertThat(connection).isNotNull();
		assertThat(queryString).isNotNull().isNotBlank();

        org.eclipse.daanse.olap.api.Statement stmt = connection.createStatement();

	    assertThat(stmt).isNotNull();

	    final org.eclipse.daanse.olap.api.result.CellSet cellSet = stmt.executeQuery( queryString );

	    assertThat(cellSet).isNotNull();

	    // If we're deep testing, check that we never return the dummy null
	    // value when cells are null. TestExpDependencies isn't the perfect
	    // switch to enable this, but it will do for now.
	    //TODO: activate this for all tests
	    if ( connection.getContext().getConfigValue(ConfigConstants.TEST_EXP_DEPENDENCIES, ConfigConstants.TEST_EXP_DEPENDENCIES_DEFAULT_VALUE, Integer.class) == 1 ) {
	      assertCellSetValid( cellSet );
	    }
	    return cellSet;
	  }

    public static org.eclipse.daanse.olap.api.result.CellSet executeQueryWithCellSetResult(Connection connection, String queryString ) throws SQLException {

        assertThat(connection).isNotNull();
        assertThat(queryString).isNotNull().isNotBlank();

        org.eclipse.daanse.olap.api.Statement stmt = connection.createStatement();

        assertThat(stmt).isNotNull();

        final org.eclipse.daanse.olap.api.result.CellSet cellSet = stmt.executeQuery( queryString );

        assertThat(cellSet).isNotNull();

        // If we're deep testing, check that we never return the dummy null
        // value when cells are null. TestExpDependencies isn't the perfect
        // switch to enable this, but it will do for now.
        //TODO: activate this for all tests
        if ( connection.getContext().getConfigValue(ConfigConstants.TEST_EXP_DEPENDENCIES, ConfigConstants.TEST_EXP_DEPENDENCIES_DEFAULT_VALUE, Integer.class) == 1 ) {
            assertCellSetValid( cellSet );
        }
        return cellSet;
    }

	/**
	 * Checks that an actual string matches an expected pattern. If they do not, throws a {@link ComparisonFailure} and
	 * prints the difference, including the actual string as an easily pasted Java string literal.
	 */
	public static void assertMatchesVerbose(
			Pattern expected,
			String actual ) {
		Util.assertPrecondition( expected != null, "expected != null" );
		if ( expected.matcher( actual ).matches() ) {
			return;
		}
		String s = actual;

		// Convert [string with "quotes" split
		// across lines]
		// into ["string with \"quotes\" split" + nl +
		// "across lines
		//
		s = s.replace("\"", "\\\"" );
		s = LineBreakPattern.matcher( s ).replaceAll( lineBreak );
		s = TabPattern.matcher( s ).replaceAll( "\\\\t" );
		s = "\"" + s + "\"";
		final String spurious = " + " + nl + "\"\"";
		if ( s.endsWith( spurious ) ) {
			s = s.substring( 0, s.length() - spurious.length() );
		}
		String message =
				"Expected pattern:" + nl + expected + nl
						+ "Actual: " + nl + actual + nl
						+ "Actual java: " + nl + s + nl;
		//throw new ComparisonFailure( message, expected.pattern(), actual );
		throw new RuntimeException(message);
	}

    /**
     * Checks that a {@link CellSet} is valid.
     *
     */
    public static void assertCellSetValid( CellSet cellSet ) {
        for ( Cell cell : cellIter( cellSet ) ) {
            final Object value = cell.getValue();

            // Check that the dummy value used to represent null cells never
            // leaks into the outside world.
            assertNotSame( value, Util.nullValue );
            assertFalse(
                value instanceof Number
                    && ( (Number) value ).doubleValue() == FunUtil.DOUBLE_NULL);

            // Similarly empty values.
            assertNotSame( value, Util.EmptyValue );
            assertFalse(
                value instanceof Number
                    && ( (Number) value ).doubleValue() == FunUtil.DOUBLE_EMPTY);

            // Cells should be null if and only if they are null or empty.
            if ( cell.getValue() == null ) {
                assertTrue( cell.isNull() );
            } else {
                assertFalse( cell.isNull() );
            }
        }
    }

    /**
     * Returns an iterator over cells in an olap4j cell set.
     */
    static Iterable<Cell> cellIter( final CellSet cellSet ) {
        return new Iterable<>() {
            @Override
            public Iterator<Cell> iterator() {
                int[] axisDimensions = new int[ cellSet.getAxes().size() ];
                int k = 0;
                for ( CellSetAxis axis : cellSet.getAxes() ) {
                    axisDimensions[ k++ ] = axis.getPositions().size();
                }
                final CoordinateIterator
                    coordIter = new CoordinateIterator( axisDimensions );
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return coordIter.hasNext();
                    }

                    @Override
                    public Cell next() {
                        final int[] ints = coordIter.next();
                        final List<Integer> list =
                            new AbstractList<>() {
                                @Override
                                public Integer get( int index ) {
                                    return ints[ index ];
                                }

                                @Override
                                public int size() {
                                    return ints.length;
                                }
                            };
                        return cellSet.getCell(
                            list );
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    static public Dialect getDialect(Connection connection){
		return connection.getContext().getDialect();
    }

	public static Member executeSingletonAxis(Connection connection, String expression, String cubeName) {
		Result result = executeQuery(connection, "select {" + expression + "} on columns from " + cubeName);
		Axis axis = result.getAxes()[0];
		switch (axis.getPositions().size()) {
			case 0:
				// The mdx "{...}" operator eliminates null members (that is,
				// members for which member.isNull() is true). So if "expression"
				// yielded just the null member, the array will be empty.
				return null;
			case 1:
				// Java nulls should never happen during expression evaluation.
				Position position = axis.getPositions().get(0);
				Util.assertTrue(position.size() == 1);
				Member member = position.get(0);
				Util.assertTrue(member != null);
				return member;
			default:
				throw Util.newInternal(
						"expression " + expression + " yielded " + axis.getPositions().size() + " positions");
		}
	}

	public static void withSchema(Context context, Function<CatalogMapping, org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier> f) {
          context.getCatalogCache().clear();
          CatalogMapping catalogMapping = ((RolapContext) context).getCatalogMapping();
          ((TestContext)context).setCatalogMappingSupplier(f.apply(catalogMapping));
    }

	public static void assertExprDependsOn(Connection connection, String expr, String hierList ) {
		// Construct a query, and mine it for a parsed expression.
		// Use a fresh connection, because some tests define their own dims.
		final String queryString =
				"WITH MEMBER [Measures].[Foo] AS "
						+ Util.singleQuoteString( expr )
						+ " SELECT FROM [Sales]";
		final Query query = connection.parseQuery( queryString );
		query.resolve();
		final Formula formula = query.getFormulas()[ 0 ];
		final Expression expression = formula.getExpression();

		// Build a list of the dimensions which the expression depends upon,
		// and check that it is as expected.
		checkDependsOn( query, expression, hierList, true );
	}

	public static void assertMemberExprDependsOn(Connection connection, String expr, String dimList ) {
		assertSetExprDependsOn(connection, "{" + expr + "}", dimList );
	}

	public static void assertSetExprDependsOn(Connection connection, String expr, String dimList ) {
		// Construct a query, and mine it for a parsed expression.
		// Use a fresh connection, because some tests define their own dims.
		final String queryString =
				"SELECT {" + expr + "} ON COLUMNS FROM [Sales]";
		final Query query = connection.parseQuery( queryString );
		query.resolve();
		final Expression expression = query.getAxes()[ 0 ].getSet();

		// Build a list of the dimensions which the expression depends upon,
		// and check that it is as expected.
		checkDependsOn( query, expression, dimList, false );
	}

	/**
	 * Executes an expression which yields a boolean result, and asserts that
	 * the result is the expected one.
	 */
	public static void assertBooleanExprReturns(Connection connection, String cubeName, String expression, boolean expected) {
		final String iifExpression =
				"Iif (" + expression + ",\"true\",\"false\")";
		final String actual = executeExpr(connection, cubeName, iifExpression);
		final String expectedString = expected ? "true" : "false";
		assertEquals(expectedString, actual);
	}


	/**
	 * Executes an expression against the Sales cube in the FoodMart database
	 * to form a single cell result set, then returns that cell's formatted
	 * value.
	 */
	public static String executeExpr(Connection connection, String cubeName, String expression) {
		return executeExprRaw(connection, cubeName, expression).getFormattedValue();
	}

	public static boolean isDefaultNullMemberRepresentation() {
		return SystemWideProperties.instance().NullMemberRepresentation
				.equals("#null");
	}

	public static String compileExpression(Connection connection, String expression, final boolean scalar, String cubeName ) {
		if ( cubeName.indexOf( ' ' ) >= 0 ) {
			cubeName = Util.quoteMdxIdentifier( cubeName );
		}
		final String queryString;
		if ( scalar ) {
			queryString =
					"with member [Measures].[Foo] as "
							+ Util.singleQuoteString( expression )
							+ " select {[Measures].[Foo]} on columns from " + cubeName;
		} else {
			queryString =
					"SELECT {" + expression + "} ON COLUMNS FROM " + cubeName;
		}
		Query query = connection.parseQuery( queryString );
		final Expression exp;
		if ( scalar ) {
			exp = query.getFormulas()[ 0 ].getExpression();
		} else {
			exp = query.getAxes()[ 0 ].getSet();
		}
		final Calc calc = query.compileExpression( exp, scalar, null );
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter( sw );

		   SimpleCalculationProfileWriter w=new SimpleCalculationProfileWriter(pw);

			if (calc instanceof ProfilingCalc pc) {
				w.write(pc.getCalculationProfile());
			}else {
				throw new RuntimeException("must be profiling calc");
			}
		pw.flush();
		return sw.toString();
	}

	/**
	 * Executes the expression in the context of the cube indicated by
	 * <code>cubeName</code>, and returns the result as a Cell.
	 *
	 * @param expression The expression to evaluate
	 * @return Cell which is the result of the expression
	 */
	//public static Cell executeExprRaw(Connection connection, String expression ) {
	//	final String queryString = generateExpression( expression, "Sales" );
	//	Result result = executeQuery(connection, queryString);
	//	return result.getCell( new int[] { 0 } );
	//}

	private static void checkDependsOn(
			final Query query,
			final Expression expression,
			String expectedHierList,
			final boolean scalar ) {
		final Calc calc =
				query.compileExpression(
						expression,
						scalar,
						scalar ? null : ResultStyle.ITERABLE );
		final List<Hierarchy> hierarchies =
				query.getCube().getHierarchies();
		StringBuilder buf = new StringBuilder( "{" );
		int dependCount = 0;
		for ( Hierarchy hierarchy : hierarchies ) {
			if ( calc.dependsOn( hierarchy ) ) {
				if ( dependCount++ > 0 ) {
					buf.append( ", " );
				}
				buf.append( hierarchy.getUniqueName() );
			}
		}
		buf.append( "}" );
		String actualHierList = buf.toString();
		assertEquals( expectedHierList, actualHierList );
	}

	/**
	 * Checks that an actual string matches an expected string. Ignores the difference of anonymous class names in
	 * "mondrian...." package.
	 *
	 * <p>If they do not, throws a {@link junit.framework.ComparisonFailure} and
	 * prints the difference, including the actual string as an easily pasted Java string literal.
	 */
	public static void assertStubbedEqualsVerbose(
			String expected,
			String actual ) {
		assertEqualsVerbose(
				stubAnonymousClasses( expected ),
				stubAnonymousClasses( actual ) );
	}

	/**
	 * Replaces anonymous class names (/\$\d+/) with a stub "$-anonymous-class-" in constructions
	 * "class&nbsp;mondrian.rest.package.name.ClassName$InnerClassNames". <br/> e.g. <br/>
	 * <code>stubAnonymousClasses("class mondrian.fun.Fun$21$1")</code>
	 * results
	 * <code>
	 * "class mondrian.fun.Fun$-anonymous-class-$-anonymous-class-"
	 * </code>.
	 * <br/> Within a Strings comparison <br/> applying this to both compared <code>String</code>s makes the comparison
	 * independent on anonymous class names.
	 * </br>
	 */
	public static String stubAnonymousClasses( String str ) {
		if ( !str.contains( "$" ) ) {
			return str;
		}
		final String regex =
				"(class mondrian(?:\\.\\w+)*(?:\\$(?:\\w+|-anonymous-class-))*?)(?:\\$\\d+)\\b";
		final String replacement = "$1\\$-anonymous-class-";
		Pattern p = Pattern.compile( regex );
		String str1 = p.matcher( str ).replaceAll( replacement );
		while ( !str.equals( str1 ) ) {
			str = str1;
			str1 = p.matcher( str ).replaceAll( replacement );
		}
		return str1;
	}

	public static String hierarchyName( String dimension, String hierarchy ) {
		return "[" + dimension + "].[" + hierarchy + "]";
	}

	/**
	 * Runs a query, and asserts that the result has a given number of columns
	 * and rows.
	 */
	public static void assertSize(
			Connection connection,
			String queryString,
			int columnCount,
			int rowCount)
	{
		Result result = executeQuery(connection, queryString);
		Axis[] axes = result.getAxes();
		assertTrue(axes.length == 2);
		assertTrue(axes[0].getPositions().size() == columnCount);
		assertTrue(axes[1].getPositions().size() == rowCount);
	}

	public static void assertSimpleQuery(Connection connection) {
		assertQueryReturns(connection,
				"select from [Sales]",
				"Axis #0:\n"
						+ "{}\n"
						+ "266,773" );
	}

	/**
	 * Executes query1 and query2 and Compares the obtained measure values.
	 */
	public static void assertQueriesReturnSimilarResults(Connection connection,
			String query1,
			String query2)
	{
		String resultString1 =
				toString(executeQuery(connection, query1));
		String resultString2 =
				TestUtil.toString(executeQuery(connection, query2));
		assertEquals(
				measureValues(resultString1),
				measureValues(resultString2));
	}

	/**
	 * Truncates the query result to return only measure values.
	 */
	private static String measureValues(String resultString) {
		int index = resultString.indexOf("}");
		return index != -1 ? resultString.substring(index) : resultString;
	}

	public static int getRowCount(Result result) {
		return result.getAxes()[result.getAxes().length - 1]
				.getPositions().size();
	}

	/**
	 * Checks that a given MDX query results in a particular SQL statement
	 * being generated.
	 *
	 * @param mdxQuery MDX query
	 * @param patterns Set of patterns for expected SQL statements
	 */
	public static void assertQuerySql(
			Connection connection,
			String mdxQuery,
			SqlPattern[] patterns)
	{
		assertQuerySqlOrNot(
				connection, mdxQuery, patterns, false, false, true);
	}

	/**
	 * Checks that a given MDX query does not result in a particular SQL
	 * statement being generated.
	 *
	 * @param mdxQuery MDX query
	 * @param patterns Set of patterns for expected SQL statements
	 */
	public static void assertNoQuerySql(
			Connection connection,
			String mdxQuery,
			SqlPattern[] patterns)
	{
		assertQuerySqlOrNot(
				connection, mdxQuery, patterns, true, false, true);
	}


	/**
	 * During MDX query parse and execution, checks that the query results
	 * (or does not result) in a particular SQL statement being generated.
	 *
	 * <p>Parses and executes the MDX query once for each SQL
	 * pattern in the current dialect. If there are multiple patterns, runs the
	 * MDX query multiple times, and expects to see each SQL statement appear.
	 * If there are no patterns in this dialect, the test trivially succeeds.
	 *
	 * @param connection connection
	 * @param mdxQuery MDX query
	 * @param patterns Set of patterns
	 * @param negative false to assert if SQL is generated;
	 *                 true to assert if SQL is NOT generated
	 * @param bypassSchemaCache whether to grab a new connection and bypass the
	 *        schema cache before parsing the MDX query
	 * @param clearCache whether to clear cache before executing the MDX query
	 */
	public static void assertQuerySqlOrNot(
			Connection connection,
			String mdxQuery,
			SqlPattern[] patterns,
			boolean negative,
			boolean bypassSchemaCache,
			boolean clearCache)
	{
		// Run the test once for each pattern in this dialect.
		// (We could optimize and run it once, collecting multiple queries, and
		// comparing all queries at the end.)
		Dialect dialect = getDialect(connection);
		DatabaseProduct d = getDatabaseProduct(dialect.getDialectName());
		boolean patternFound = false;
		for (SqlPattern sqlPattern : patterns) {
			if (!sqlPattern.hasDatabaseProduct(d)) {
				// If the dialect is not one in the pattern set, skip the
				// test. If in the end no pattern is located, print a warning
				// message if required.
				continue;
			}

			patternFound = true;

			String sql = sqlPattern.getSql();
			String trigger = sqlPattern.getTriggerSql();

			sql = dialectize(d, sql);
			trigger = dialectize(d, trigger);

			// Create a dummy DataSource which will throw a 'bomb' if it is
			// asked to execute a particular SQL statement, but will otherwise
			// behave exactly the same as the current DataSource.
			final TriggerHook hook = new TriggerHook(trigger);
			RolapUtil.setHook(hook);
			Bomb bomb = null;
			try {
				if (bypassSchemaCache) {
				//	connection =
				//			testContext.withSchemaPool(false).getConnection();
				}
				final Query query = connection.parseQuery(mdxQuery);
				if (clearCache) {
					clearCache(connection, (RolapCube)query.getCube());
				}
				final Result result = connection.execute(query);
//				discard(result);
				bomb = null;
			} catch (Bomb e) {
				bomb = e;
			} catch (RuntimeException e) {
				// Walk up the exception tree and see if the root cause
				// was a SQL bomb.
				bomb = Util.getMatchingCause(e, Bomb.class);
				if (bomb == null) {
					throw e;
				}
			} finally {
				RolapUtil.setHook(null);
			}
			if (negative) {
				if (bomb != null || hook.foundMatch()) {
					fail("forbidden query [" + sql + "] detected");
				}
			} else {
				if (bomb == null && !hook.foundMatch()) {
					fail("expected query [" + sql + "] did not occur");
				}
				if (bomb != null) {
					assertEquals(
							replaceQuotes(
									sql.replaceAll("\r\n", "\n")),
							replaceQuotes(
									bomb.sql.replaceAll("\r\n", "\n")));
				}
			}
		}

		// Print warning message that no pattern was specified for the current
		// dialect.
		if (!patternFound) {
			String warnDialect =
					connection.getContext().getConfigValue(ConfigConstants.WARN_IF_NO_PATTERN_FOR_DIALECT, ConfigConstants.WARN_IF_NO_PATTERN_FOR_DIALECT_DEFAULT_VALUE, String.class);

			if (warnDialect.equals(d.toString())) {
				System.out.println(
						"[No expected SQL statements found for dialect \""
								+ dialect.toString()
								+ "\" and test not run]");
			}
		}
	}

	public static String dialectize(DatabaseProduct d, String sql) {
		sql = sql.replaceAll("\r\n", "\n");
		switch (d) {
			case ORACLE:
				return sql.replaceAll(" =as= ", " ");
			case GREENPLUM:
			case POSTGRES:
			case TERADATA:
				return sql.replaceAll(" =as= ", " as ");
			case DERBY:
				return sql.replaceAll("`", "\"");
			case ACCESS:
				return sql.replaceAll(
						"ISNULL\\(([^)]*)\\)",
						"Iif($1 IS NULL, 1, 0)");
			default:
				return sql;
		}
	}


	/**
	 * Flushes the entire contents of the cache. Utility method used to ensure
	 * that cache control tests are starting with a blank page.
	 *
	 * @param connection Connection
	 */
	public static void flushCache(Connection connection) {
		final CacheControl cacheControl = connection.getCacheControl(null);

		// Flush the entire cache.
		CacheControl.CellRegion measuresRegion = null;
		for (Cube cube
				: connection.getCatalog().getCubes())
		{
			measuresRegion =
					cacheControl.createMeasuresRegion(cube);
			cacheControl.flush(measuresRegion);
		}

		// Check the cache is empty.
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		cacheControl.printCacheState(pw, measuresRegion);
		pw.flush();
		assertEquals("", sw.toString());
	}

	private static String replaceQuotes(String s) {
		s = s.replace('`', '\"');
		s = s.replace('\'', '\"');
		return s;
	}

	public static void clearCache(Connection connection, RolapCube cube) {
		// Clear the cache for the Sales cube, so the query runs as if
		// for the first time. (TODO: Cleaner way to do this.)
		final Cube salesCube =
				connection.getCatalog().lookupCube(cube.getName()).orElseThrow();
		RolapHierarchy hierarchy =
				(RolapHierarchy) salesCube.lookupHierarchy(
						new IdImpl.NameSegmentImpl("Store", Quoting.UNQUOTED),
						false);
		if (hierarchy != null) {
			SmartMemberReader memberReader =
					(SmartMemberReader) hierarchy.getMemberReader();
			MemberCacheHelper cacheHelper = memberReader.cacheHelper;
			cacheHelper.mapLevelToMembers.cache.clear();
			cacheHelper.mapMemberToChildren.cache.clear();
		}
		// Flush the cache, to ensure that the query gets executed.
		cube.clearCachedAggregations(true);

		CacheControl cacheControl = connection.getCacheControl(null);
		final CacheControl.CellRegion measuresRegion =
				cacheControl.createMeasuresRegion(cube);
		cacheControl.flush(measuresRegion);
		waitForFlush(cacheControl, measuresRegion, cube.getName());
	}

	public static Member allMember(String dimensionName, Cube salesCube) {
		Dimension genderDimension = getDimension(dimensionName, salesCube);
		return genderDimension.getHierarchy().getAllMember();
	}

	public static CellSet executeOlap4jXmlaQuery(Context context, String queryString )
			throws SQLException {
		/*
		Connection connection = context.getConnection();
		String schema = getConnectionProperties(connection)
				.get( RolapConnectionProperties.CatalogContent.name() );
		if ( schema == null ) {
			schema = getRawSchema(context);
		}
		// TODO:  Need to better handle semicolons in schema content.
		// Util.parseValue does not appear to allow escaping them.
		schema = schema.replace( "&quot;", "" ).replace( ";", "" );

		String Jdbc = getConnectionProperties(connection)
				.get( RolapConnectionProperties.Jdbc.name() );

		String cookie = XmlaOlap4jDriver.nextCookie();
		Map<String, String> catalogs = new HashMap<String, String>();
		catalogs.put( "FoodMart", "" );
		XmlaOlap4jDriver.PROXY_MAP.put(
				cookie, new MondrianInprocProxy(
						catalogs,
						"jdbc:mondrian:Server=http://whatever;"
								+ "Jdbc=" + Jdbc + ";TestProxyCookie="
								+ cookie
								+ ";CatalogContent=" + schema ) );
		try {
			Class.forName( "org.olap4j.driver.xmla.XmlaOlap4jDriver" );
		} catch ( ClassNotFoundException e ) {
			throw new RuntimeException( "oops", e );
		}
		Properties info = new Properties();
		info.setProperty(
				XmlaOlap4jDriver.Property.CATALOG.name(), "FoodMart" );
		java.sql.Connection con = java.sql.DriverManager.getConnection(
				"jdbc:xmla:Server=http://whatever;Catalog=FoodMart;TestProxyCookie="
						+ cookie,
				info );
		*/
		Connection connection = context.getConnectionWithDefaultRole();
		//		con.unwrap( OlapConnection.class );

        org.eclipse.daanse.olap.api.Statement statement = connection.createStatement();
		return statement.executeQuery( queryString );
	}


	private static Dimension getDimension(String dimensionName, Cube salesCube) {
		return getDimensionWithName(dimensionName, salesCube.getDimensions());
	}

	public static Dimension getDimensionWithName(
			String name,
			Dimension[] dimensions)
	{
		Dimension resultDimension = null;
		for (Dimension dimension : dimensions) {
			if (dimension.getName().equals(name)) {
				resultDimension = dimension;
				break;
			}
		}
		return resultDimension;
	}

	private static void waitForFlush(
			final CacheControl cacheControl,
			final CacheControl.CellRegion measuresRegion,
			final String cubeName)
	{
		int i = 100;
		while (true) {
			try {
				Thread.sleep(i);
			} catch (InterruptedException e) {
				fail(e.getMessage());
			}
			String cacheState = getCacheState(cacheControl, measuresRegion);
			if (regionIsEmpty(cacheState, cubeName)) {
				break;
			}
			i *= 2;
			if (i > 6400) {
				fail(
						"Cache didn't flush in sufficient time\nCache Was: \n"
								+ cacheState);
				break;
			}
		}
	}

	private static String getCacheState(
			final CacheControl cacheControl,
			final CacheControl.CellRegion measuresRegion)
	{
		StringWriter out = new StringWriter();
		cacheControl.printCacheState(new PrintWriter(out), measuresRegion);
		return out.toString();
	}

	private static boolean regionIsEmpty(
			final String cacheState, final String cubeName)
	{
		return !cacheState.contains("Cube:[" + cubeName + "]");
	}

	private static SqlPattern[] sqlPattern(DatabaseProduct db, String sql) {
		return new SqlPattern[]{new SqlPattern(db, sql, sql.length())};
	}

	public static SqlPattern[] mysqlPattern(String sql) {
		return sqlPattern(DatabaseProduct.MYSQL, sql);
	}

	/**
	 * Checks whether query produces the same results with the native.* props
	 * enabled as it does with the props disabled
	 * @param query query to run
	 * @param message Message to output on test failure
	 * @param connection Connection
	 */
	public static void verifySameNativeAndNot(Connection connection,
			String query, String message)
	{
	    SystemWideProperties properties = SystemWideProperties.instance();
        ((TestContextImpl)(connection.getContext())).setEnableNativeCrossJoin(true);
        ((TestContextImpl)(connection.getContext())).setEnableNativeFilter(true);
        properties.EnableNativeNonEmpty= true;
        ((TestContextImpl)(connection.getContext())).setEnableNativeTopCount(true);

		Result resultNative = executeQuery(connection, query);

        ((TestContextImpl)(connection.getContext())).setEnableNativeCrossJoin(false);
        ((TestContextImpl)(connection.getContext())).setEnableNativeFilter(false);
        properties.EnableNativeNonEmpty = false;
        ((TestContextImpl)(connection.getContext())).setEnableNativeTopCount(false);

		Result resultNonNative = executeQuery(connection, query);

		assertEquals(
				toString(resultNative),
				toString(resultNonNative),
				message);


	}

/**
	 * Fake exception to interrupt the test when we see the desired query.
	 * It is an {@link Error} because we need it to be unchecked
	 * ({@link Exception} is checked), and we don't want handlers to handle
	 * it.
	 */
	static class Bomb extends Error {
		final String sql;

		Bomb(final String sql) {
			this.sql = sql;
		}
	}

	private static class TriggerHook implements RolapUtil.ExecuteQueryHook {
		private final String trigger;
		private boolean foundMatch = false;

		public TriggerHook(String trigger) {
			this.trigger =
					trigger
							.replaceAll("\r\n", "")
							.replaceAll("\r", "")
							.replaceAll("\n", "");
		}

		private boolean matchTrigger(String sql) {
			if (trigger == null) {
				return true;
			}
			// Cleanup the endlines.
			sql =
					sql
							.replaceAll("\r\n", "")
							.replaceAll("\r", "")
							.replaceAll("\n", "");
			// different versions of mysql drivers use different quoting, so
			// ignore quotes
			String s = replaceQuotes(sql);
			String t = replaceQuotes(trigger);
			if (s.startsWith(t) && !foundMatch) {
				foundMatch = true;
			}
			return s.startsWith(t);
		}

		@Override
		public void onExecuteQuery(String sql) {
			if (matchTrigger(sql)) {
				throw new Bomb(sql);
			}
		}

		public boolean foundMatch() {
			return foundMatch;
		}
	}

    public static Optional<Cube> getCubeByNameFromArray(List<Cube> cubes, String name){
	    return cubes.stream().filter(c -> name.equals(c.getName())).findFirst();
    }

    public static Optional<Dimension> getDimensionByNameFromArray(Dimension[] dimensions, String name){
        return Arrays.stream(dimensions).filter(d -> name.equals(d.getName())).findFirst();
    }

    public static Optional<Hierarchy> getHierarchyByNameFromArray(Hierarchy[] hierarchies, String name){
        return Arrays.stream(hierarchies).filter(h -> name.equals(h.getName())).findFirst();
    }

    public static Optional<Level> getLevelByNameFromArray(Level[] hierarchies, String name){
        return Arrays.stream(hierarchies).filter(l -> name.equals(l.getName())).findFirst();
    }
}
