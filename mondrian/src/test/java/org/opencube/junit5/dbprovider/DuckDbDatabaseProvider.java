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
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.duckdb.DuckDBConnection;
import org.duckdb.DuckDBDriver;
import org.eclipse.daanse.sql.dialect.api.Dialect;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.spi.ServiceProvider;

/**
 * Embedded DuckDB, in memory. Every {@code driver.connect("jdbc:duckdb:")} opens
 * its own private, empty database, so this provider connects ONCE and hands out
 * {@link DuckDBConnection#duplicate()} siblings of that connection — they all
 * share the one in-memory database, and it lives as long as the keeper does.
 *
 * <p>
 * This used to be a temp FILE database ({@code <tmpdir>/daanse-duckdb-<UUID>.db})
 * purely to get that sharing. The file bought nothing else: nothing is read back
 * after the JVM exits, and each fork had its own UUID, so there was never any
 * cross-JVM sharing to lose. Dropping it removes the database and WAL I/O, and
 * {@code duplicate()} is ~7x cheaper than a fresh connect (0.010 ms vs 0.07 ms),
 * over ~158k connection checkouts in a full TCK run.
 */
@ServiceProvider(value = DatabaseProvider.class)
public class DuckDbDatabaseProvider implements DatabaseProvider {

	// java.util.logging.Logger is imported for the DataSource shim below, so the
	// slf4j class logger is declared with its fully qualified type.
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DuckDbDatabaseProvider.class);

	private static final String URL = "jdbc:duckdb:";

	@Override
	public String id() {
		return "duckdb";
	}

	private volatile DuckDbDataSource dataSource;

	@Override
	public void close() throws IOException {
		DuckDbDataSource ds = dataSource;
		if (ds != null) {
			// closing the keeper drops the in-memory database; there is no file to unlink
			ds.close();
		}
	}

	@Override
	public Entry<DataSource, Dialect> activate() {
		long tActivate = System.nanoTime();
		DuckDbDataSource dataSource = new DuckDbDataSource(URL);
		this.dataSource = dataSource;
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

		/**
		 * An embedded DuckDB database lives exactly as long as its last open
		 * connection: closing it tears the database down. Callers here take a
		 * connection per statement, so without one held open for the provider's
		 * lifetime every query paid a full database startup+shutdown -- measured at
		 * 8.2 ms against 0.4 ms of actual query work, a 19x tax on the whole TCK.
		 *
		 * <p>
		 * It is also the database every other connection duplicates, so it must be
		 * the connection that carries the settings (see {@link #getConnection()}).
		 */
		private volatile DuckDBConnection keeper;

		private DuckDbDataSource(String url) {
			this.url = url;
		}

		private void close() {
			DuckDBConnection k = keeper;
			keeper = null;
			if (k != null) {
				try {
					k.close();
				} catch (SQLException e) {
					LOGGER.warn("closing the duckdb keeper connection failed", e);
				}
			}
		}

		@Override
		public Connection getConnection() throws SQLException {
			// Benchmarked (duckbench matrix, 90-query real TCK workload): the embedded default
			// (threads = all cores) oversubscribes the shared JVM/machine on the small OLAP
			// queries this suite runs - threads=2 + preserve_insertion_order=false was ~21%
			// faster overall and ~2x on sub-millisecond queries. Passed as connection
			// properties (a per-connection SET would cost a round-trip on every checkout).
			// That benchmark ran under three concurrent test groups sharing one machine; a
			// run that owns the box may want more. Hence threads and memory_limit are
			// overridable per run, defaulting to the benchmarked values.
			Properties props = new Properties();
			props.setProperty("threads", cfg("DAANSE_DUCKDB_THREADS", "daanse.duckdb.threads", "2"));
			String memoryLimit = cfg("DAANSE_DUCKDB_MEMORY_LIMIT", "daanse.duckdb.memory_limit", null);
			if (memoryLimit != null) {
				props.setProperty("memory_limit", memoryLimit);
			}
			props.setProperty("preserve_insertion_order", "false");
			// DuckDB 1.3.2 and 1.5.4 both fail a star-schema query that combines a WHERE of
			// OR-ed equality tuples with a GROUP BY over those same columns:
			//   INTERNAL Error: Failed to bind column reference "product_category" [3.3]
			// (1.5.4 hides it behind "unsuccessful or closed pending query result"). It is the
			// column_lifetime pass -- the one that remaps column bindings -- and disabling it
			// makes the five NonEmptyTest#testMon2202* cases pass.
			//
			// We do NOT disable it. column_lifetime is what prunes unused columns out of the
			// plan; without it every intermediate carries every column. Measured over the full
			// TCK: 2.74 ms per query becomes 4.06 ms, p95 14 ms becomes 20 ms, and the whole
			// duckdb run grows by about half. (An earlier "costs nothing" claim came from timing
			// one star-join aggregate -- a sample of one.) Five tests are not worth that.
			//
			// Set DAANSE_DUCKDB_DISABLED_OPTIMIZERS=column_lifetime to reproduce the engine bug
			// or to check whether a newer DuckDB has fixed it.
			String disabledOptimizers = cfg("DAANSE_DUCKDB_DISABLED_OPTIMIZERS",
					"daanse.duckdb.disabled_optimizers", null);
			if (disabledOptimizers != null) {
				props.setProperty("disabled_optimizers", disabledOptimizers);
			}
			if (keeper == null) {
				synchronized (this) {
					if (keeper == null) {
						// The settings above only take effect on the connect that starts the
						// database, so the keeper must carry them.
						keeper = (DuckDBConnection) driver.connect(url, props);
					}
				}
			}
			// A fresh connect would open a fresh, empty in-memory database. duplicate()
			// returns an independent connection onto the keeper's database.
			return keeper.duplicate();
		}

		/** Environment variable wins over system property; blank counts as unset. */
		private static String cfg(String envName, String sysprop, String defaultValue) {
			String value = System.getenv(envName);
			if (value == null || value.isBlank()) {
				value = System.getProperty(sysprop);
			}
			return (value == null || value.isBlank()) ? defaultValue : value;
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
