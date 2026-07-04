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

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
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

	@Override
	public String id() {
		return "postgres";
	}

	@Override
	protected List<String> cmd() {
		// the official entrypoint prepends "postgres" when the first arg starts with '-'
		return List.of("-c", "max_connections=500");
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
		return envs;
	}

	@Override
	protected Entry<DataSource, Dialect> createDataSource() {
		PGSimpleDataSource dataSource = new PGSimpleDataSource();
		dataSource.setServerNames(new String[] { serverName });
		dataSource.setPortNumbers(new int[] { port });
		dataSource.setDatabaseName(POSTGRES_DB);
		dataSource.setUser(POSTGRES_USER);
		dataSource.setPassword(POSTGRES_PASSWORD);
		// batched INSERTs are rewritten into multi-row INSERT ... VALUES (),(),...
		// at the protocol level (pgjdbc's analog of MySQL's
		// rewriteBatchedStatements, which MySqlDatabaseProvider already sets);
		// documented 2-3x on bulk-insert batches, no semantic change for the
		// CSV loader (it never reads per-row update counts).
		dataSource.setReWriteBatchedInserts(true);

		for (int i = 0; i < readinessSeconds() * 10; i++) {
			System.out.print(".");
			try {
				Thread.sleep(100);
				Connection connection = dataSource.getConnection(POSTGRES_USER, POSTGRES_PASSWORD);
				// must survive an actual statement, see class javadoc
				try (Statement statement = connection.createStatement()) {
					statement.executeQuery("SELECT 1").close();
				}
				Dialect dialect = TestDialects.resolve(connection);
				connection.close();
				return new SimpleEntry<>(dataSource, dialect);
			} catch (Exception e) {
				System.out.print("-");
			}
		}
		return null;
	}

	@Override
	protected String image() {
		return "postgres:16";
	}

}
