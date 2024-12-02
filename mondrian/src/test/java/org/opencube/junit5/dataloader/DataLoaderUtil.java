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
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import mondrian.enums.DatabaseProduct;
import mondrian.olap.Util;

public class DataLoaderUtil {

	public static final String nl = Util.NL;

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

		return table.constraints.stream().map(constraint -> {

			StringBuilder buf = new StringBuilder();

			buf.append(constraint.unique ? "CREATE UNIQUE INDEX " : "CREATE INDEX ")
					.append(dialect.quoteIdentifier(constraint.name));
			if (getDatabaseProduct(dialect.getDialectName()) != DatabaseProduct.TERADATA) {
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
			if (getDatabaseProduct(dialect.getDialectName()) == DatabaseProduct.TERADATA) {
				buf.append(" ON ").append(dialect.quoteIdentifier(table.schemaName, table.tableName));
			}
			final String createDDL = buf.toString();
			return createDDL;

		}).toList();
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
		for (Column column : table.columns) {
			if (first) {
				first = false;
			} else {
				buf.append(", ");
			}
			buf.append(nl);
			buf.append("    ").append(dialect.quoteIdentifier(column.name));
			buf.append(" ").append(column.type.toPhysical(dialect));
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
		switch (getDatabaseProduct(dialect.getDialectName())) {
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
				switch (getDatabaseProduct(dialect.getDialectName())) {
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
				switch (getDatabaseProduct(dialect.getDialectName())) {
				case ORACLE:
				case FIREBIRD:
					return "DECIMAL(15,0)";
				default:
					return name;
				}
			}
			if (this == Date) {
				switch (getDatabaseProduct(dialect.getDialectName())) {
				case MSSQL:
					return "DATETIME";
				case INGRES:
					return "INGRESDATE";
				default:
					return name;
				}
			}
			if (this == Timestamp) {
				switch (getDatabaseProduct(dialect.getDialectName())) {
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

	public static void importCSV(DataSource dataSource, Dialect dialect, List<Table> tables, Path csvDir)
			throws SQLException {
        CsvReader.CsvReaderBuilder builder = CsvReader.builder()
            .fieldSeparator(',')
            .quoteCharacter('"')
            .skipEmptyLines(true)
            .commentCharacter('#')
            .ignoreDifferentFieldCount(false);


		tables.parallelStream().forEach(table -> {
			try (Connection connection = dataSource.getConnection();) {
				System.out.println("+" + table.tableName);

				Path p = csvDir.resolve(table.tableName + ".csv");

				if (!p.toFile().exists()) {
					System.out.println("file does not exist-" + table.tableName);

				}

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
                            ps.getConnection().setAutoCommit(false);

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
                    }

                    long start = System.currentTimeMillis();
                    System.out.println("---");
                    ps.executeBatch();
                    System.out.println(System.currentTimeMillis() - start);

                    connection.commit();
                    System.out.println(System.currentTimeMillis() - start);
                    connection.setAutoCommit(true);
                }
			} catch (Exception e) {
				e.printStackTrace();
			}

		});
	}

	public static void executeSql(Connection connection, List<String> sqls, boolean paralel) throws SQLException {
		Stream<String> s = paralel ? sqls.parallelStream() : sqls.stream();
		s.forEach(sql -> {
			try (Statement statement = connection.createStatement();) {
				System.out.println(sql);
				statement.execute(sql);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

	}
}
