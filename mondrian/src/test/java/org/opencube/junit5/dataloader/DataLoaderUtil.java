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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.duckdb.DuckDBAppender;
import org.duckdb.DuckDBConnection;
import org.eclipse.daanse.sql.dialect.api.Dialect;
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

		if (isDuckDb(dialect)) {
			// DuckDB is columnar: mondrian's scans never use these secondary indexes, but
			// every one of the ~876k rows the loader appends has to maintain all 94 of them.
			// Skipped here rather than by making DuckDbDialect.supportsIndexDdl() report
			// false, which would be a lie -- DuckDB does support CREATE INDEX, and that
			// capability is read outside the tests too.
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
		public static final Type Varchar1024 = new Type("VARCHAR(1024)");
		public static final Type Varchar60 = new Type("VARCHAR(60)");
		public static final Type Real = new Type("REAL");
		/**
		 * 64-bit float. {@link #Real} is single precision -- postgres maps it to float4, whose
		 * seven significant digits cannot hold SteelWheels' TOTALPRICE sum of 10,645,949.18.
		 */
		public static final Type Double = new Type("DOUBLE");
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
					|| this == Varchar255 || this == Varchar1024 || this == Real) {
				return name;
			}
			if (this == Double) {
				switch (getDatabaseProduct(dialect.name())) {
				case POSTGRES:
				case GREENPLUM:
				case DERBY:
					return "DOUBLE PRECISION";
				case MSSQL:
				case SYBASE:
					return "FLOAT";
				case ORACLE:
					return "BINARY_DOUBLE";
				default:
					return name;
				}
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

	/**
	 * Where a table's CSV comes from. Every call opens a fresh reader: the DuckDB appender path
	 * falls back to the JDBC batch path and then has to read the file a second time.
	 */
	@FunctionalInterface
	public interface CsvSource {
		Reader open(String tableName) throws IOException;
	}

	/** CSVs from a directory on disk. */
	public static CsvSource fromDirectory(Path csvDir) {
		return tableName -> Files.newBufferedReader(csvDir.resolve(tableName + ".csv"), StandardCharsets.UTF_8);
	}

	/**
	 * CSVs from a rolap.mapping instance bundle, where they sit at {@code /data/<table>.csv} next
	 * to the mapping that describes them. That copy is the canonical one; {@code anchor} is any
	 * class of the bundle that carries it (its {@code CatalogSupplier}, say).
	 */
	public static CsvSource fromClasspath(Class<?> anchor) {
		return tableName -> {
			// The published bundles carry the CSVs at the jar root; a local build leaves them
			// beside the class. Accept both rather than depend on which one is installed.
			String relative = "data/" + tableName + ".csv";
			InputStream in = anchor.getResourceAsStream(relative);
			if (in == null) {
				in = anchor.getResourceAsStream("/" + relative);
			}
			if (in == null) {
				throw new FileNotFoundException(relative + " is not on the classpath of " + anchor.getName());
			}
			return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
		};
	}

	/**
	 * The canonical FoodMart CSVs encode booleans as 1/0; the copy that used to live under
	 * testfiles wrote TRUE/FALSE. {@code Boolean.parseBoolean("1")} is false, so neither
	 * {@code parseBoolean} nor {@code Boolean.valueOf} will do.
	 */
	private static boolean parseBoolean(String field) {
		return "1".equals(field) || "true".equalsIgnoreCase(field);
	}

	public static long importCSV(DataSource dataSource, Dialect dialect, List<Table> tables, CsvSource csvSource)
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
					seqRows += importTable(connection, dialect, table, builder, csvSource);
				}
			}
			logCsvLoad(dialect, tCsv, seqRows);
			return seqRows;
		}

		AtomicLong rows = new AtomicLong();
		// A stack trace on stderr is not a failure. Collect what went wrong and say so:
		// four tables once loaded zero rows here and the run reported success.
		List<String> failures = Collections.synchronizedList(new ArrayList<>());
		tables.parallelStream().forEach(table -> {
			try (Connection connection = dataSource.getConnection();) {
				long imported = importTable(connection, dialect, table, builder, csvSource);
				rows.addAndGet(imported);
				verifyRowCount(connection, dialect, table, imported, failures);
			} catch (Exception e) {
				failures.add(table.tableName + ": " + e);
				LOGGER.error("CSV import failed for table {}", table.tableName, e);
			}

		});
		if (!failures.isEmpty()) {
			throw new SQLException("CSV import failed for " + failures.size() + " of " + tables.size()
					+ " tables: " + failures);
		}
		logCsvLoad(dialect, tCsv, rows.get());
		return rows.get();
	}

	/**
	 * Nothing ever gathered optimizer statistics after the load. H2 collects column selectivity
	 * only when told to, and a freshly loaded postgres table has no entry in pg_statistic until
	 * autovacuum gets around to it -- the tests start long before that. Both then plan the
	 * star-schema joins from defaults.
	 *
	 * <p>
	 * Measured on the dbheavy subset. h2: the run drops from ~174s to ~127s, summed query time
	 * from ~95s to ~71s, and its slowest query from 7301 ms to 1544 ms -- the signature of a
	 * bad plan, not of a slow engine. postgres: 100s to 92s, summed query time 38.9s to 28.4s.
	 * ANALYZE itself costs 61-85 ms on h2 and 850 ms on postgres.
	 *
	 * <p>
	 * Only h2 and postgres for now. DuckDB keeps its statistics itself (ANALYZE returns in 0 ms
	 * and changes nothing), and
	 * MySQL and MariaDB have no bare ANALYZE -- theirs is {@code ANALYZE TABLE <name>}, which
	 * no one has measured yet. DAANSE_ANALYZE=false switches it off.
	 *
	 * <p>
	 * Call this after the indexes exist, not from inside the import: Derby's statistics describe
	 * index cardinalities, so gathering them on an unindexed table tells the optimizer nothing.
	 */
	static void analyze(DataSource dataSource, Dialect dialect, List<Table> tables) {
		if (!Boolean.parseBoolean(System.getenv().getOrDefault("DAANSE_ANALYZE", "true"))) {
			return;
		}
		// DatabaseProduct has no H2 constant, so compare the dialect name directly
		String name = dialect == null || dialect.name() == null ? "" : dialect.name();
		long t = System.nanoTime();
		try (Connection connection = dataSource.getConnection()) {
			List<String> sqls = analyzeSqls(connection, name, tables);
			if (sqls.isEmpty()) {
				return;
			}
			try (Statement statement = connection.createStatement()) {
				for (String sql : sqls) {
					statement.execute(sql);
				}
			}
		} catch (SQLException e) {
			LOGGER.warn("db={} ANALYZE failed", dbId(dialect), e);
			return;
		}
		LOGGER.warn("DBTIMING db={} phase=analyze ms={}", dbId(dialect), (System.nanoTime() - t) / 1_000_000);
	}

	/** @return the statements that gather optimizer statistics, empty if the dialect has none */
	private static List<String> analyzeSqls(Connection connection, String dialectName, List<Table> tables)
			throws SQLException {
		if (dialectName.equalsIgnoreCase("H2") || dialectName.equalsIgnoreCase("POSTGRES")
				|| dialectName.equalsIgnoreCase("POSTGRESQL")) {
			// h2 samples 10000 rows per table by default; DAANSE_ANALYZE_SQL="ANALYZE SAMPLE_SIZE 0"
			// makes it read every row. Overridable so the trade can be measured.
			return List.of(System.getenv().getOrDefault("DAANSE_ANALYZE_SQL", "ANALYZE"));
		}
		return List.of();
	}

	/**
	 * Reads back what the import claims to have written. The loader counts CSV rows, not
	 * stored rows, so a table can be reported as loaded while the database holds nothing —
	 * exactly how four tables went missing without a single failing test.
	 *
	 * <p>
	 * A mismatch fails the load on DuckDB, whose native-appender path this guards and whose
	 * behaviour is covered by the h2/mysql/duckdb gate. On the other dialects it only warns:
	 * widening it to a hard failure needs a run against all ten of them first.
	 */
	private static void verifyRowCount(Connection connection, Dialect dialect, Table table, long expected,
			List<String> failures) {
		String sql = "SELECT count(*) FROM " + dialect.quoteIdentifier(table.schemaName, table.tableName);
		long actual;
		try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
			actual = rs.next() ? rs.getLong(1) : -1;
		} catch (Exception e) {
			LOGGER.warn("could not verify the row count of {}", table.tableName, e);
			return;
		}
		if (actual == expected) {
			return;
		}
		String message = table.tableName + ": imported " + expected + " rows but the table holds " + actual;
		if (isDuckDb(dialect)) {
			failures.add(message);
		} else {
			LOGGER.warn("row-count mismatch after CSV import -- {}", message);
		}
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
			CsvReader.CsvReaderBuilder builder, CsvSource csvSource) {
		System.out.println("+" + table.tableName);

		if (isDuckDb(dialect) && Boolean.parseBoolean(System.getProperty(DUCKDB_APPENDER_PROPERTY, "true"))) {
			try {
				return importTableDuckDbAppender(connection, table, builder, csvSource);
			} catch (Exception e) {
				// Warn per table, not once per JVM: a single flag hid three of the four
				// tables that silently ended up empty.
				LOGGER.warn("DuckDB appender fast path failed for table {} — falling back to generic JDBC batch",
						table.tableName, e);
				try {
					// drop partially appended rows before re-loading generically
					executeDDL(connection,
							"DELETE FROM " + dialect.quoteIdentifier(table.schemaName, table.tableName));
				} catch (Exception cleanupFailure) {
					// The appender may have written part of the table. Falling back now would
					// duplicate those rows; returning 0 (what this used to do) left the table
					// empty and reported success. Neither is acceptable -- fail loudly.
					throw new IllegalStateException("DuckDB appender failed for table " + table.tableName
							+ " and the partially loaded rows could not be removed, so the table"
							+ " can be neither reloaded nor trusted", cleanupFailure);
				}
			}
		}
		return importTableJdbcBatch(connection, dialect, table, builder, csvSource);
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
			CsvReader.CsvReaderBuilder builder, CsvSource csvSource) throws Exception {
		DuckDBConnection duck = connection.unwrap(DuckDBConnection.class);
		String schema = table.schemaName != null ? table.schemaName : DuckDBConnection.DEFAULT_SCHEMA;
		long rows = 0;
		try (Reader reader = csvSource.open(table.tableName);
				CloseableIterator<NamedCsvRecord> it = builder.ofNamedCsvRecord(reader).iterator();
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
						appender.append((short) (parseBoolean(field) ? 1 : 0));
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
					} else if (col.type.equals(Type.Double)) {
						appender.append(field.equals("") ? 0.0 : Double.parseDouble(field));
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
			CsvReader.CsvReaderBuilder builder, CsvSource csvSource) {
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
                try (Reader reader = csvSource.open(table.tableName);
                        CloseableIterator<NamedCsvRecord> it = builder.ofNamedCsvRecord(reader).iterator()) {
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
                                ps.setBoolean(i, !field.equals("") && parseBoolean(field));

                            } else if (col.type.equals(DataLoaderUtil.Type.Currency)) {
                                ps.setDouble(i, field.equals("") ? 0.0 : Double.valueOf(field));

                            } else if (col.type.equals(DataLoaderUtil.Type.Date)) {
                                ps.setDate(i, Date.valueOf(field));

                            } else if (col.type.equals(DataLoaderUtil.Type.Integer)) {
                                ps.setInt(i, field.equals("") ? 0 : Integer.valueOf(field));

                            } else if (col.type.equals(DataLoaderUtil.Type.Double)) {
                                ps.setDouble(i, field.equals("") ? 0.0 : Double.valueOf(field));
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

                            } else if (col.type.equals(DataLoaderUtil.Type.Varchar60)
                                    || col.type.equals(DataLoaderUtil.Type.Varchar1024)) {
                                ps.setString(i, field);
                            } else {
                                // The chain used to end here. A type without a branch left its
                                // parameter unbound, and the driver reported it far away as
                                // "No value specified for parameter N".
                                throw new SQLException("no JDBC binding for column type "
                                        + col.type.name + " on " + table.tableName + "." + col.name);
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
		// All statements share ONE connection, and a JDBC connection is not required to be
		// thread-safe. SQLite serializes writers; DuckDB fails outright, one racing statement
		// poisoning the next with "Attempting to execute an unsuccessful or closed pending
		// query" -- which silently cost four tables their CREATE TABLE and then their data.
		Stream<String> s = (paralel && !isSqlite(connection) && !isDuckDb(connection)) ? sqls.parallelStream()
				: sqls.stream();
		AtomicInteger failed = new AtomicInteger();
		AtomicReference<String> firstFailure = new AtomicReference<>();
		AtomicReference<String> firstCreateTableFailure = new AtomicReference<>();
		s.forEach(sql -> {
			try (Statement statement = connection.createStatement();) {
				System.out.println(sql);
				statement.execute(sql);
			} catch (Exception e) {
				failed.incrementAndGet();
				firstFailure.compareAndSet(null, sql + " -> " + e);
				if (startsWithIgnoreCase(sql, "CREATE TABLE")) {
					firstCreateTableFailure.compareAndSet(null, sql + " -> " + e);
				}
				LOGGER.debug("DDL statement failed: {}", sql, e);
			}
		});
		if (failed.get() > 0) {
			LOGGER.warn("DDL: {}/{} statements failed (expected on dialects without DROP TABLE IF EXISTS); first: {}",
					failed.get(), sqls.size(), firstFailure.get());
		}
		// A failed DROP TABLE IF EXISTS is routine, and a failed CREATE INDEX costs only speed.
		// A failed CREATE TABLE is fatal on every dialect: the table the tests are about to
		// query does not exist, and the load that follows quietly writes nothing.
		if (firstCreateTableFailure.get() != null) {
			throw new SQLException("CREATE TABLE failed, the load would silently be empty: "
					+ firstCreateTableFailure.get());
		}
	}

	private static boolean startsWithIgnoreCase(String sql, String prefix) {
		return sql != null && sql.stripLeading().regionMatches(true, 0, prefix, 0, prefix.length());
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

	private static boolean isDuckDb(Connection connection) {
		try {
			String url = connection.getMetaData().getURL();
			if (url != null && url.startsWith("jdbc:duckdb")) {
				return true;
			}
			String product = connection.getMetaData().getDatabaseProductName();
			return product != null && product.toLowerCase().contains("duckdb");
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
