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

import java.sql.Connection;
import java.sql.Statement;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.eclipse.daanse.sql.dialect.api.Dialect;
import org.h2.jdbcx.JdbcConnectionPool;
import org.postgresql.ds.PGConnectionPoolDataSource;
import org.postgresql.ds.PGSimpleDataSource;

import com.github.dockerjava.api.model.PortBinding;

import aQute.bnd.annotation.spi.ServiceProvider;

/**
 * PostgreSQL provider (Batch C). User/password/database names are aligned
 * with {@link MySqlDatabaseProvider}.
 *
 * <p>Readiness: the poll executes an actual {@code SELECT 1}, not just
 * {@code getConnection()}. Postgres' initdb entrypoint starts the server once
 * for bootstrap and then RESTARTS it; a connection obtained in the
 * pre-restart window would die under the first real statement, so the poll
 * only succeeds once a statement round-trips.
 */
@ServiceProvider(value = DatabaseProvider.class)
public class PostgresDatabaseProvider extends AbstractDockerBasesDatabaseProvider implements DatabaseProvider {

	public static String POSTGRES_DB = "the.db";
	public static String POSTGRES_USER = "the.user";
	public static String POSTGRES_PASSWORD = "the.pw";
	public static int PORT = 5432;
	public static String serverName = "localhost";

	/** Clears mondrian's 100 concurrent sql threads; see {@link #pooled()}. */
	private static final int MAX_POOLED_CONNECTIONS = 128;

	private volatile JdbcConnectionPool pool;

	@Override
	public String id() {
		return "postgres";
	}

	@Override
	protected List<String> cmd() {
		// the official entrypoint prepends "postgres" when the first arg starts with '-'
		//
		// The container has no cpu or memory limit, but postgres ships defaults that ignore
		// the host: shared_buffers 128 MB and work_mem 4 MB. FoodMart does not fit in 128 MB,
		// so every one of mondrian's ~158k aggregate scans reads it back from disk, and the
		// star-schema GROUP BYs spill their hash tables into temp files. Three test groups
		// each run one of these containers, so the numbers below stay well inside 29 GB.
		return List.of("-c", "max_connections=500", //
				"-c", "shared_buffers=2GB", //
				"-c", "work_mem=32MB", //
				"-c", "maintenance_work_mem=512MB", //
				"-c", "effective_cache_size=6GB");
	}

	@Override
	protected PortBinding portBinding() {
		return PortBinding.parse(port + ":" + PORT);
	}

	@Override
	protected List<String> env() {
		ArrayList<String> envs = new ArrayList<>();
		envs.add("POSTGRES_USER=" + POSTGRES_USER);
		envs.add("POSTGRES_PASSWORD=" + POSTGRES_PASSWORD);
		envs.add("POSTGRES_DB=" + POSTGRES_DB);
		// The image initialises the cluster with en_US.utf8, whose glibc collation ignores
		// spaces and punctuation at the primary level: it orders "Lakewood" before "La Mesa"
		// and "Newton" before "N. Vancouver". Mondrian compares the native SQL result against
		// what its own interpreter produced with String.compareTo, which is code-point order,
		// so eight tests reported "Native implementation returned different result than
		// interpreter" for what was the same 45 rows in a different order. The C locale is
		// code-point order, which is what every other dialect here already gives us.
		envs.add("POSTGRES_INITDB_ARGS=--locale=C --encoding=UTF8");
		return envs;
	}

	@Override
	protected Entry<DataSource, Dialect> createDataSource() {
		PGSimpleDataSource probe = new PGSimpleDataSource();
		probe.setServerNames(new String[] { serverName });
		probe.setPortNumbers(new int[] { port });
		probe.setDatabaseName(POSTGRES_DB);
		probe.setUser(POSTGRES_USER);
		probe.setPassword(POSTGRES_PASSWORD);

		for (int i = 0; i < readinessSeconds() * 10; i++) {
			System.out.print(".");
			try {
				Thread.sleep(100);
				Connection connection = probe.getConnection(POSTGRES_USER, POSTGRES_PASSWORD);
				// must survive an actual statement, see class javadoc
				try (Statement statement = connection.createStatement()) {
					statement.executeQuery("SELECT 1").close();
				}
				Dialect dialect = TestDialects.resolve(connection);
				connection.close();
				// Pool only once the server has stopped restarting -- a pooled connection from
				// the pre-restart window would be handed out dead.
				return new SimpleEntry<>(pooled(), dialect);
			} catch (Exception e) {
				System.out.print("-");
			}
		}
		return null;
	}

	/**
	 * Callers take a connection per SQL statement and close it
	 * ({@code SqlStatement.execute()}), and a postgres connect is a TCP handshake, a startup
	 * message, password auth and a backend fork. Measured against this image: connect+close
	 * 5.58 ms, a representative query on a held connection 5.48 ms -- so half of every
	 * statement was spent getting to the database. From the pool a checkout costs 0.01 ms.
	 *
	 * <p>
	 * {@link JdbcConnectionPool} is h2's, but it pools any
	 * {@link javax.sql.ConnectionPoolDataSource}, and h2 is already a runtime bundle here --
	 * no new dependency. pgjdbc's own
	 * {@code PGPoolingDataSource} is deprecated. Verified against h2 2.2.224: a connection
	 * returned with autoCommit=false and an open transaction is rolled back and reset before
	 * the next checkout, which is what {@code DataLoaderUtil}'s batch loader needs.
	 */
	private DataSource pooled() {
		PGConnectionPoolDataSource cpds = new PGConnectionPoolDataSource();
		cpds.setServerNames(new String[] { serverName });
		cpds.setPortNumbers(new int[] { port });
		cpds.setDatabaseName(POSTGRES_DB);
		cpds.setUser(POSTGRES_USER);
		cpds.setPassword(POSTGRES_PASSWORD);
		// batched INSERTs are rewritten into multi-row INSERT ... VALUES (),(),...
		// at the protocol level (pgjdbc's analog of MySQL's
		// rewriteBatchedStatements, which MySqlDatabaseProvider already sets);
		// documented 2-3x on bulk-insert batches, no semantic change for the
		// CSV loader (it never reads per-row update counts).
		cpds.setReWriteBatchedInserts(true);

		JdbcConnectionPool created = JdbcConnectionPool.create(cpds);
		// Mondrian's segment loader runs SEGMENT_CACHE_MANAGER_NUMBER_SQL_THREADS (default 100)
		// statements at once, each holding a connection. An exhausted pool does not degrade
		// gracefully -- it blocks for the login timeout and then throws "Login timeout" -- so
		// the cap has to clear that number with room to spare. The container allows 500, and
		// three test groups use one pool each.
		created.setMaxConnections(MAX_POOLED_CONNECTIONS);
		pool = created;
		return created;
	}

	@Override
	public void close() {
		JdbcConnectionPool open = pool;
		pool = null;
		if (open != null) {
			open.dispose();
		}
		super.close();
	}

	@Override
	protected String image() {
		return "postgres:16";
	}

}
