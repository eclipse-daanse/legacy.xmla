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
package org.opencube.junit5.dataloader;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.duckdb.DuckDBAppender;
import org.duckdb.DuckDBConnection;
import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.olap.common.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import mondrian.enums.DatabaseProduct;

public class DataLoaderUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataLoaderUtil.class);

	public static final String nl = Util.NL;

	/**
	 * Rows per {@link PreparedStatement#executeBatch()} flush in
	 * {@link #importTable}. Historically the whole CSV was one giant batch
	 * (87k rows for sales_fact_1997); flushing in chunks caps the driver-side
	 * batch buffer across the parallel per-table connections and measured
	 * slightly faster on H2 (docs/multi-dialect-activation/driver-tuning.md).
	 * All chunks of one table still commit in ONE transaction (unchanged).
	 * 10k rows is also comfortably above ClickHouse's recommended minimum
	 * insert-block size, so chunking does not fragment MergeTree parts.
	 */
	private static final int BATCH_CHUNK_ROWS = 10_000;

	/**
	 * System property gating the DuckDB native-appender fast path in
	 * {@link #importTable} (default: enabled). DuckDB's JDBC
	 * {@code executeBatch()} is a loop of single-row engine executions — there
	 * is no batch protocol — which makes the generic path ~113x slower than
	 * the native {@link DuckDBAppender} on the fact tables
	 * (docs/multi-dialect-activation/driver-tuning.md §3). Set
	 * {@code -Dmondrian.test.duckdb.appender=false} to force the generic JDBC
	 * batch path (used by the standalone row-count verification).
	 */
	private static final String DUCKDB_APPENDER_PROPERTY = "mondrian.test.duckdb.appender";

	/** WARN once (not per table) if the appender fast path has to fall back. */
	private static final AtomicBoolean DUCKDB_APPENDER_WARNED = new AtomicBoolean();

	/**
	 * Creates an index.
	 *
	 * <p>
	 * If we are outputting to JDBC, executes the CREATE INDEX statement; otherwise,
	 * outputs the statement to a file.
	 */
	public static List<String> createIndexSqls(Table table, Dialect dialect) {

		if (table.constraints == null) {
			return List.of();
		}

		if (!supportsIndexDdl(dialect)) {
			// index creation is OPTIONAL per database product — see supportsIndexDdl
			return List.of();
		}

		return table.constraints.stream().map(constraint -> {

			StringBuilder buf = new StringBuilder();

			buf.append(constraint.unique ? "CREATE UNIQUE INDEX " : "CREATE INDEX ")
					.append(dialect.quoteIdentifier(constraint.name));
			if (getDatabaseProduct(dialect.name()) != DatabaseProduct.TERADATA) {
				buf.append(" ON ").append(dialect.quoteIdentifier(table.schemaName, table.tableName));
			}
			buf.append(" (");

			boolean first = true;
			for (String columnName : constraint.columnNames) {

				if (first) {
					first = false;
				} else {
					buf.append(", ");
				}
				buf.append(dialect.quoteIdentifier(columnName));
			}
			buf.append(")");
			if (getDatabaseProduct(dialect.name()) == DatabaseProduct.TERADATA) {
				buf.append(" ON ").append(dialect.quoteIdentifier(table.schemaName, table.tableName));
			}
			final String createDDL = buf.toString();
			return createDDL;

		}).toList();
	}

	/**
	 * Whether the dialect supports (needs) classic secondary-index DDL. Delegates to the
	 * dialect capability ({@code Dialect.supportsIndexDdl()}, lifted from the former
	 * per-product switch here — the ClickHouse knowledge now lives in ClickHouseDialect).
	 */
	public static boolean supportsIndexDdl(Dialect dialect) {
		return dialect.supportsIndexDdl();
	}

	/**
	 * Creates a table definition.
	 *
	 * @param table   Table
	 * @param dialect dialect
	 */
	public static String dropTableSQL(Table table, Dialect dialect) {
		String schemaTable = dialect.quoteIdentifier(table.schemaName, table.tableName);

		return "DROP TABLE IF EXISTS " + schemaTable;
	}

	/**
	 * Creates a table definition.
	 *
	 * @param table   Table
	 * @param dialect dialect
	 */
	public static String createTableSQL(Table table, Dialect dialect) {
		String schemaTable = dialect.quoteIdentifier(table.schemaName, table.tableName);
//                    try {
//                        executeDDL("DROP TABLE IF EXISTS " + schemaTable);
//                    } catch (SQLException e) {}

		// Define the table.
		StringBuilder buf = new StringBuilder();
		buf.append("CREATE TABLE ").append(schemaTable).append("(");

		boolean first = true;
		final boolean clickhouse =
				getDatabaseProduct(dialect.name()) == DatabaseProduct.CLICKHOUSE;
		for (Column column : table.columns) {
			if (first) {
				first = false;
			} else {
				buf.append(", ");
			}
			buf.append(nl);
			buf.append("    ").append(dialect.quoteIdentifier(column.name));
			String physical = column.type.toPhysical(dialect);
			if (clickhouse && column.constraint.equals("")) {
				// ClickHouse columns are non-Nullable by default; NULLs bound into them
				// are silently converted to defaults (0/'') at insert, so nullable CSV
				// columns must be declared Nullable(...) explicitly or NULL-keyed members
				// ([#null]) never exist.
				physical = "Nullable(" + physical + ")";
			}
			buf.append(" ").append(physical);
			if (!column.constraint.equals("")) {
				buf.append(" ").append(column.constraint);
			}
		}

//            if (table.constraints != null) {
//                for (Constraint uniqueConstraint : table.constraints) {
//                   if( !uniqueConstraint.unique) {
//                       continue;
//                   }
//                    buf.append(",");
//                    buf.append(nl);
//                    buf.append("    ");
//                    buf.append("CONSTRAINT ");
//                    buf.append(dialect.quoteIdentifier(uniqueConstraint.name));
//                    buf.append(" UNIQUE(");
//                    String [] columnNames = uniqueConstraint.columnNames;
//                    for (int i = 0; i < columnNames.length; i++) {
//                        if (i > 0) {
//                            buf.append(",");
//                        }
//                        buf.append(dialect.quoteIdentifier(columnNames[i]));
//                    }
//                    buf.append(")");
//                }
//            }

		buf.append(")");
		switch (getDatabaseProduct(dialect.name())) {
		case NEOVIEW:
			// no unique keys defined
			buf.append(" NO PARTITION");
		}

		final String ddl = buf.toString();
		return ddl;

	}

	public static class Table {
		public final String schemaName;
		public final String tableName;
		List<Constraint> constraints;
		public final Column[] columns;

		public Table(String schemaName, String tableName, List<Constraint> constraints, Column... columns) {
			this.schemaName = schemaName;
			this.tableName = tableName;
			this.constraints = constraints;
			this.columns = columns;
		}

	}

	public static class Column {
		public final String name;
		public final Type type;
		public final String constraint;

		public Column(String name, Type type, boolean nullsAllowed) {
			this.name = name;
			this.type = type;
			this.constraint = nullsAllowed ? "" : "NOT NULL";
		}

	}

	public static class Constraint {
		final String name;
		final String[] columnNames;
		final boolean unique;

		public Constraint(String name, boolean unique, String... columnNames) {
			this.name = name;
			this.unique = unique;
			this.columnNames = columnNames;
		}
	}

	/**
	 * Represents a logical type, such as "BOOLEAN".
	 * <p/>
	 *
	 * Specific databases will represent this with their own particular physical
	 * type, for example "TINYINT(1)", "BOOLEAN" or "BIT"; see
	 * {@link #toPhysical(mondrian.spi.Dialect)}.
	 */
	public static class Type {
		/**
		 * The name of this type. Immutable, and independent of the RDBMS.
		 */
		public final String name;

		public static final Type Integer = new Type("INTEGER");
		public static final Type Currency = new Type("DECIMAL(10,4)");
		public static final Type Smallint = new Type("SMALLINT");
		public static final Type Varchar30 = new Type("VARCHAR(30)");
		public static final Type Varchar255 = new Type("VARCHAR(255)");
		public static final Type Varchar60 = new Type("VARCHAR(60)");
		public static final Type Real = new Type("REAL");
		public static final Type Boolean = new Type("BOOLEAN");
		public static final Type Bigint = new Type("BIGINT");
		public static final Type Date = new Type("DATE");
		public static final Type Timestamp = new Type("TIMESTAMP");

		public Type(String name) {
			this.name = name;
		}

		/**
		 * Returns the physical type which a given RDBMS (dialect) uses to represent
		 * this logical type.
		 */
		String toPhysical(Dialect dialect) {
			if (this == Integer || this == Currency || this == Smallint || this == Varchar30 || this == Varchar60
					|| this == Varchar255 || this == Real) {
				return name;
			}
			if (this == Boolean) {
				switch (getDatabaseProduct(dialect.name())) {
				case POSTGRES:
				case GREENPLUM:
				case LUCIDDB:
				case NETEZZA:
				case HSQLDB:
					return name;
				case MARIADB:
				case MYSQL:
				case INFOBRIGHT:
					return "TINYINT(1)";
				case MSSQL:
				case SYBASE:
					return "BIT";
				default:
					return Smallint.name;
				}
			}
			if (this == Bigint) {
				switch (getDatabaseProduct(dialect.name())) {
				case ORACLE:
				case FIREBIRD:
					return "DECIMAL(15,0)";
				default:
					return name;
				}
			}
			if (this == Date) {
				switch (getDatabaseProduct(dialect.name())) {
				case MSSQL:
					return "DATETIME";
				case INGRES:
					return "INGRESDATE";
				default:
					return name;
				}
			}
			if (this == Timestamp) {
				switch (getDatabaseProduct(dialect.name())) {
				case MSSQL:
				case MARIADB:
				case MYSQL:
				case INFOBRIGHT:
				case SYBASE:
					return "DATETIME";
				case INGRES:
					return "INGRESDATE";
				case INFORMIX:
					return "DATETIME YEAR TO FRACTION(1)";
				default:
					return name;
				}
			}
			throw new AssertionError("unexpected type: " + name);
		}
	}

	/**
	 * After data has been loaded from a file or via JDBC, creates any derived data.
	 */
	public static void loadFromSqlInserts(Connection connection, Dialect dialect, InputStream sqlFile)
			throws Exception {

		try {
			final InputStreamReader reader = new InputStreamReader(sqlFile);
			final BufferedReader bufferedReader = new BufferedReader(reader);

			String line;
			int lineNumber = 0;
//			discard(lineNumber);

			StringBuilder buf = new StringBuilder();

			String fromQuoteChar = null;
			String toQuoteChar = dialect.getQuoteIdentifierString();
			while ((line = bufferedReader.readLine()) != null) {
				++lineNumber;

				line = line.trim();
				if (line.startsWith("#") || line.length() == 0) {
					continue;
				}

				if (fromQuoteChar == null) {
					if (line.indexOf('`') >= 0) {
						fromQuoteChar = "`";
					} else if (line.indexOf('"') >= 0) {
						fromQuoteChar = "\"";
					}
				}

				if (fromQuoteChar != null && !fromQuoteChar.equals(toQuoteChar)) {
					line = line.replaceAll(fromQuoteChar, toQuoteChar);
				}

				// End of buf
				if (line.charAt(line.length() - 1) == ';') {
					buf.append(" ").append(line.substring(0, line.length() - 1));

					executeDDL(connection, buf.toString());
					buf.setLength(0);

				} else {
					buf.append(" ").append(line.substring(0, line.length()));
				}
			}

			if (buf.length() > 0) {
				executeDDL(connection, buf.toString());
			}
		} finally {
			if (sqlFile != null) {
				sqlFile.close();
			}
		}
	}

	/**
	 * Executes a DDL statement.
	 *
	 * @param ddl DDL statement
	 * @throws Exception on error
	 */
	public static void executeDDL(Connection connection, String ddl) throws Exception {

		Statement statement = null;
		try {
			statement = connection.createStatement();
			statement.execute(ddl);
		} finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					// ignore
				}
			}
		}
	}

	public static long importCSV(DataSource dataSource, Dialect dialect, List<Table> tables, Path csvDir)
			throws SQLException {
		long tCsv = System.nanoTime();
        CsvReader.CsvReaderBuilder builder = CsvReader.builder()
            .fieldSeparator(',')
            .quoteCharacter('"')
            .skipEmptyLines(true)
            .commentCharacter('#')
            .ignoreDifferentFieldCount(false);


		if (isSqlite(dialect)) {
			// SQLite: single writer — load the tables SEQUENTIALLY over ONE connection
			// (per-table parallel connections deadlock/starve on the shared cache).
			long seqRows = 0;
			try (Connection connection = dataSource.getConnection()) {
				for (Table table : tables) {
					seqRows += importTable(connection, dialect, table, builder, csvDir);
				}
			}
			logCsvLoad(dialect, tCsv, seqRows);
			return seqRows;
		}

		AtomicLong rows = new AtomicLong();
		tables.parallelStream().forEach(table -> {
			try (Connection connection = dataSource.getConnection();) {
				rows.addAndGet(importTable(connection, dialect, table, builder, csvDir));
			} catch (Exception e) {
				e.printStackTrace();
			}

		});
		logCsvLoad(dialect, tCsv, rows.get());
		return rows.get();
	}

	/**
	 * DBTIMING db=&lt;id&gt; phase=&lt;name&gt; ms=&lt;duration&gt; [detail=...] — stable grep
	 * format for the harness collector (dbtiming_report.sh); one line per phase occurrence.
	 */
	private static void logCsvLoad(Dialect dialect, long startNanos, long rows) {
		LOGGER.warn("DBTIMING db={} phase=csv-load ms={} detail=rows:{}", dbId(dialect),
				(System.nanoTime() - startNanos) / 1_000_000, rows);
	}

	/** lowercase db id matching {@code DatabaseProvider#id()} (mysql, postgres, ...). */
	static String dbId(Dialect dialect) {
		return dialect == null || dialect.name() == null ? "unknown" : dialect.name().toLowerCase(Locale.ROOT);
	}

	/** @return the number of CSV rows batched for this table (0 on failure) */
	private static long importTable(Connection connection, Dialect dialect, Table table,
			CsvReader.CsvReaderBuilder builder, Path csvDir) {
		System.out.println("+" + table.tableName);

		Path p = csvDir.resolve(table.tableName + ".csv");

		if (!p.toFile().exists()) {
			System.out.println("file does not exist-" + table.tableName);
		}

		if (isDuckDb(dialect) && Boolean.parseBoolean(System.getProperty(DUCKDB_APPENDER_PROPERTY, "true"))) {
			try {
				return importTableDuckDbAppender(connection, table, builder, p);
			} catch (Exception e) {
				if (DUCKDB_APPENDER_WARNED.compareAndSet(false, true)) {
					LOGGER.warn("DuckDB appender fast path failed for table {} — falling back to generic JDBC batch",
							table.tableName, e);
				}
				try {
					// drop partially appended rows before re-loading generically
					executeDDL(connection,
							"DELETE FROM " + dialect.quoteIdentifier(table.schemaName, table.tableName));
				} catch (Exception cleanupFailure) {
					LOGGER.warn("DuckDB appender fallback: could not clean partially loaded table {}",
							table.tableName, cleanupFailure);
					return 0;
				}
			}
		}
		return importTableJdbcBatch(connection, dialect, table, builder, p);
	}

	/**
	 * DuckDB-only CSV import through the native {@link DuckDBAppender}
	 * (measured 113x faster than the JDBC batch path on sales_fact_1997 —
	 * docs/multi-dialect-activation/driver-tuning.md §3). Value semantics
	 * mirror {@link #importTableJdbcBatch} exactly: {@code null}/"NULL" → SQL
	 * NULL, empty string → 0/false for numeric/boolean columns. Appender rows
	 * are positional in physical table-column order, which is
	 * {@code table.columns} (the generic path already relies on the CSV header
	 * having that same order when it binds by {@code table.columns}). Boolean
	 * columns are physically SMALLINT on DuckDB ({@link Type#toPhysical}
	 * default branch), so booleans append as 0/1 shorts — same coercion the
	 * driver applies to {@code setBoolean}. No transaction handling: the
	 * appender commits on {@code close()}.
	 */
	private static long importTableDuckDbAppender(Connection connection, Table table,
			CsvReader.CsvReaderBuilder builder, Path csvFile) throws Exception {
		DuckDBConnection duck = connection.unwrap(DuckDBConnection.class);
		String schema = table.schemaName != null ? table.schemaName : DuckDBConnection.DEFAULT_SCHEMA;
		long rows = 0;
		try (CloseableIterator<NamedCsvRecord> it = builder.ofNamedCsvRecord(csvFile).iterator();
				DuckDBAppender appender = duck.createAppender(schema, table.tableName)) {
			while (it.hasNext()) {
				NamedCsvRecord r = it.next();
				appender.beginRow();
				for (Column col : table.columns) {
					String field = r.getField(col.name);
					if (field == null || field.equals("NULL")) {
						appender.appendNull();
					} else if (col.type.equals(Type.Bigint)) {
						appender.append(field.equals("") ? 0L : Long.parseLong(field));
					} else if (col.type.equals(Type.Boolean)) {
						appender.append((short) (Boolean.parseBoolean(field) ? 1 : 0));
					} else if (col.type.equals(Type.Currency)) {
						// the appender requires the exact column scale
						// (DECIMAL(10,4)); CSV values carry 0-4 decimals.
						// HALF_UP matches the double->DECIMAL cast rounding
						// of the generic setDouble path.
						appender.append((field.equals("") ? BigDecimal.ZERO : new BigDecimal(field)).setScale(4,
								RoundingMode.HALF_UP));
					} else if (col.type.equals(Type.Date)) {
						appender.append(Date.valueOf(field).toLocalDate());
					} else if (col.type.equals(Type.Integer)) {
						appender.append(field.equals("") ? 0 : Integer.parseInt(field));
					} else if (col.type.equals(Type.Real)) {
						// generic path binds via setDouble; DuckDB REAL is float32
						appender.append((float) (field.equals("") ? 0.0 : Double.parseDouble(field)));
					} else if (col.type.equals(Type.Smallint)) {
						appender.append(field.equals("") ? (short) 0 : Short.parseShort(field));
					} else if (col.type.equals(Type.Timestamp)) {
						appender.append(Timestamp.valueOf(field).toLocalDateTime());
					} else {
						// Varchar30/60/255
						appender.append(field);
					}
				}
				appender.endRow();
				rows++;
			}
		}
		return rows;
	}

	/** Generic per-row JDBC prepared-statement batch import (all databases). */
	private static long importTableJdbcBatch(Connection connection, Dialect dialect, Table table,
			CsvReader.CsvReaderBuilder builder, Path p) {
		long rows = 0;
		// ClickHouse (jdbc-v2) throws SQLFeatureNotSupportedException on
		// setAutoCommit(false) — transactions are unsupported and the metadata says
		// so; gate the whole autocommit/commit dance on supportsTransactions().
		final boolean transactional = supportsTransactions(connection);
		try {
//				if (table.tableName.startsWith("agg_")) {
//					//aggregation tables are calculated
//					//TODO: also load them
//					return;
//				}
                try (CloseableIterator<NamedCsvRecord> it = builder.ofNamedCsvRecord(p).iterator()) {
                    if (!it.hasNext()) {
                        throw new IllegalStateException("No header found");
                    }
                    PreparedStatement ps = null;
                    boolean first = true;
                    int pending = 0;
                    while (it.hasNext()) {
                        NamedCsvRecord r = it.next();
                        if (first) {
                            first = false;
                            List<String> headers = r.getHeader();

                            StringBuilder b = new StringBuilder();
                            b.append("INSERT INTO ");
                            b.append(dialect.quoteIdentifier(table.schemaName, table.tableName));
                            b.append(" ( ");
                            b.append(headers.stream().map(h -> dialect.quoteIdentifier(h)).collect(Collectors.joining(",")));
                            b.append(" ) VALUES ");
                            b.append(" ( ");
                            b.append(headers.stream().map(h -> "?").collect(Collectors.joining(",")));
                            b.append(" ) ");
                            ps = connection.prepareStatement(b.toString());
                            if (transactional) {
                                ps.getConnection().setAutoCommit(false);
                            }

                        } else {
                         //   ps.clearParameters();
                        }
                        int i = 1;
                        for (Column col : table.columns) {
                            String field = r.getField(col.name);
                            if (field == null || field.equals("NULL")) {
                                ps.setObject(i, null);
                            } else if (col.type.equals(DataLoaderUtil.Type.Bigint)) {
                                ps.setLong(i, field.equals("") ? 0l : Long.valueOf(field));

                            } else if (col.type.equals(DataLoaderUtil.Type.Boolean)) {
                                ps.setBoolean(i, field.equals("") ? Boolean.FALSE : Boolean.valueOf(field));

                            } else if (col.type.equals(DataLoaderUtil.Type.Currency)) {
                                ps.setDouble(i, field.equals("") ? 0.0 : Double.valueOf(field));

                            } else if (col.type.equals(DataLoaderUtil.Type.Date)) {
                                ps.setDate(i, Date.valueOf(field));

                            } else if (col.type.equals(DataLoaderUtil.Type.Integer)) {
                                ps.setInt(i, field.equals("") ? 0 : Integer.valueOf(field));

                            } else if (col.type.equals(DataLoaderUtil.Type.Real)) {
                                ps.setDouble(i, field.equals("") ? 0.0 : Double.valueOf(field));

                            } else if (col.type.equals(DataLoaderUtil.Type.Smallint)) {
                                ps.setShort(i, field.equals("") ? 0 : Short.valueOf(field));

                            } else if (col.type.equals(DataLoaderUtil.Type.Timestamp)) {
                                ps.setTimestamp(i, Timestamp.valueOf(field));

                            } else if (col.type.equals(DataLoaderUtil.Type.Varchar255)) {
                                ps.setString(i, field);

                            } else if (col.type.equals(DataLoaderUtil.Type.Varchar30)) {
                                ps.setString(i, field);

                            } else if (col.type.equals(DataLoaderUtil.Type.Varchar60)) {
                                ps.setString(i, field);
                            }

                            i++;
                        }
                        ps.addBatch();
                        rows++;
                        pending++;
                        if (pending >= BATCH_CHUNK_ROWS) {
                            ps.executeBatch();
                            pending = 0;
                        }
                    }

                    long start = System.currentTimeMillis();
                    System.out.println("---");
                    ps.executeBatch();
                    System.out.println(System.currentTimeMillis() - start);

                    if (transactional) {
                        connection.commit();
                    }
                    System.out.println(System.currentTimeMillis() - start);
                    if (transactional) {
                        connection.setAutoCommit(true);
                    }
                }
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
		return rows;
	}

	public static void executeSql(Connection connection, List<String> sqls, boolean paralel) throws SQLException {
		// SQLite serializes writers; run its DDL/DML sequentially on the one connection.
		Stream<String> s = (paralel && !isSqlite(connection)) ? sqls.parallelStream() : sqls.stream();
		AtomicInteger failed = new AtomicInteger();
		AtomicReference<String> firstFailure = new AtomicReference<>();
		s.forEach(sql -> {
			try (Statement statement = connection.createStatement();) {
				System.out.println(sql);
				statement.execute(sql);
			} catch (Exception e) {
				failed.incrementAndGet();
				firstFailure.compareAndSet(null, sql + " -> " + e);
				LOGGER.debug("DDL statement failed: {}", sql, e);
			}
		});
		if (failed.get() > 0) {
			LOGGER.warn("DDL: {}/{} statements failed (expected on dialects without DROP TABLE IF EXISTS); first: {}",
					failed.get(), sqls.size(), firstFailure.get());
		}

	}

	/**
	 * Whether the connection's database supports transactions; databases that do
	 * not (ClickHouse) reject {@link Connection#setAutoCommit setAutoCommit(false)}
	 * with a {@link java.sql.SQLFeatureNotSupportedException}, so the CSV import
	 * must not touch the transaction API there.
	 */
	private static boolean supportsTransactions(Connection connection) {
		try {
			return connection.getMetaData().supportsTransactions();
		} catch (SQLException e) {
			// keep the pre-existing (transactional) behavior when metadata is coy
			return true;
		}
	}

	private static boolean isSqlite(Connection connection) {
		try {
			String url = connection.getMetaData().getURL();
			if (url != null && url.startsWith("jdbc:sqlite")) {
				return true;
			}
			String product = connection.getMetaData().getDatabaseProductName();
			return product != null && product.toLowerCase().contains("sqlite");
		} catch (SQLException e) {
			return false;
		}
	}

	private static boolean isSqlite(Dialect dialect) {
		return dialect != null && dialect.name() != null && dialect.name().equalsIgnoreCase("SQLITE");
	}

	private static boolean isDuckDb(Dialect dialect) {
		return dialect != null && dialect.name() != null && dialect.name().equalsIgnoreCase("DUCKDB");
	}
}
