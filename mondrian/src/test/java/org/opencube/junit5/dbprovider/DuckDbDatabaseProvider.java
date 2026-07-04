/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 */
package org.opencube.junit5.dbprovider;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.duckdb.DuckDBDriver;
import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.spi.ServiceProvider;

/**
 * Embedded DuckDB. A plain {@code jdbc:duckdb:} in-memory database is
 * PER-CONNECTION (every connection gets its own private, empty database), so
 * data loaded over one connection would be invisible to the test connections.
 * Instead each provider instance uses a temp FILE database
 * ({@code jdbc:duckdb:<tmpdir>/daanse-duckdb-<UUID>.db}) — all connections in
 * this JVM to the same file share one database instance, and the UUID gives
 * per-JVM isolation. The file (and its WAL) is deleted on {@link #close()}.
 */
@ServiceProvider(value = DatabaseProvider.class)
public class DuckDbDatabaseProvider implements DatabaseProvider {

	// java.util.logging.Logger is imported for the DataSource shim below, so the
	// slf4j class logger is declared with its fully qualified type.
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DuckDbDatabaseProvider.class);

	private final Path dbFile = Paths.get(System.getProperty("java.io.tmpdir"),
			"daanse-duckdb-" + UUID.randomUUID() + ".db");
	private final String url = "jdbc:duckdb:" + dbFile;

	@Override
	public String id() {
		return "duckdb";
	}

	@Override
	public void close() throws IOException {
		Files.deleteIfExists(dbFile);
		// DuckDB writes ahead into <file>.wal until checkpoint.
		Files.deleteIfExists(Paths.get(dbFile + ".wal"));
	}

	@Override
	public Entry<DataSource, Dialect> activate() {
		long tActivate = System.nanoTime();
		DataSource dataSource = new DuckDbDataSource(url);
		try (Connection connection = dataSource.getConnection()) {
			Dialect dialect = TestDialects.resolve(connection);
			// embedded: db-ready = the (tiny) connect duration, for comparability with docker DBs
			long ms = (System.nanoTime() - tActivate) / 1_000_000;
			LOGGER.warn("DBTIMING db={} phase=db-ready ms={}", id(), ms);
			LOGGER.warn("DBTIMING db={} phase=context-first ms={} detail=fresh,epoch={}", id(), ms,
					System.currentTimeMillis() / 1000);
			return new SimpleEntry<>(dataSource, dialect);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * The DuckDB driver ships no {@link DataSource}; connect through
	 * {@link DuckDBDriver#connect} directly (NOT {@code DriverManager}, which
	 * does not work across OSGi class loaders).
	 */
	private static final class DuckDbDataSource implements DataSource {

		private final DuckDBDriver driver = new DuckDBDriver();
		private final String url;
		private PrintWriter logWriter;
		private int loginTimeout = 0;

		private DuckDbDataSource(String url) {
			this.url = url;
		}

		@Override
		public Connection getConnection() throws SQLException {
			// Benchmarked (duckbench matrix, 90-query real TCK workload): the embedded default
			// (threads = all cores) oversubscribes the shared JVM/machine on the small OLAP
			// queries this suite runs - threads=2 + preserve_insertion_order=false was ~21%
			// faster overall and ~2x on sub-millisecond queries. Passed as connection
			// properties (a per-connection SET would cost a round-trip on every checkout).
			Properties props = new Properties();
			props.setProperty("threads", "2");
			props.setProperty("preserve_insertion_order", "false");
			return driver.connect(url, props);
		}

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			// DuckDB is embedded — no authentication.
			return getConnection();
		}

		@Override
		public PrintWriter getLogWriter() throws SQLException {
			return logWriter;
		}

		@Override
		public void setLogWriter(PrintWriter out) throws SQLException {
			this.logWriter = out;
		}

		@Override
		public void setLoginTimeout(int seconds) throws SQLException {
			this.loginTimeout = seconds;
		}

		@Override
		public int getLoginTimeout() throws SQLException {
			return loginTimeout;
		}

		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			return driver.getParentLogger();
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			if (iface.isInstance(this)) {
				return iface.cast(this);
			}
			throw new SQLException("Cannot unwrap to " + iface.getName());
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return iface.isInstance(this);
		}
	}

}
