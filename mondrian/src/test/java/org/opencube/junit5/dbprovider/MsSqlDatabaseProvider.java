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

import com.github.dockerjava.api.model.PortBinding;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import aQute.bnd.annotation.spi.ServiceProvider;

/**
 * Microsoft SQL Server provider (Batch C).
 *
 * <p>The image has no "create a database on start" env hook, so readiness is
 * polled as {@code sa} against {@code master} (SQL Server start is slow:
 * ~180s budget), then {@code foodmart} is created if absent, and the returned
 * DataSource is bound to {@code foodmart}.
 *
 * <p>The sa password must satisfy the image's complexity policy (upper,
 * lower, digit, symbol, length >= 8).
 */
@ServiceProvider(value = DatabaseProvider.class)
public class MsSqlDatabaseProvider extends AbstractDockerBasesDatabaseProvider implements DatabaseProvider {

	public static String MSSQL_SA_USER = "sa";
	public static String MSSQL_SA_PASSWORD = "The.Pw123!";
	public static String MSSQL_DATABASE = "foodmart";
	public static int PORT = 1433;
	public static String serverName = "localhost";

	@Override
	public String id() {
		return "mssql";
	}

	@Override
	protected int readinessSeconds() {
		// SQL Server needs noticeably longer than the 100s default to accept logins
		return 180;
	}

	@Override
	protected PortBinding portBinding() {
		return PortBinding.parse(port + ":" + PORT);
	}

	@Override
	protected List<String> env() {
		ArrayList<String> envs = new ArrayList<>();
		envs.add("ACCEPT_EULA=Y");
		envs.add("MSSQL_SA_PASSWORD=" + MSSQL_SA_PASSWORD);
		envs.add("MSSQL_PID=Developer");
		return envs;
	}

	private SQLServerDataSource dataSource(String databaseName) {
		SQLServerDataSource dataSource = new SQLServerDataSource();
		dataSource.setServerName(serverName);
		dataSource.setPortNumber(port);
		dataSource.setUser(MSSQL_SA_USER);
		dataSource.setPassword(MSSQL_SA_PASSWORD);
		dataSource.setDatabaseName(databaseName);
		// the 2022 image presents a self-signed certificate; the driver defaults
		// to encrypt=true since 10.x and would refuse it otherwise
		dataSource.setEncrypt("false");
		dataSource.setTrustServerCertificate(true);
		// batched parameterized INSERTs travel as TDS bulk copy instead of
		// per-row RPC calls — the big FoodMart-load win (measured 1.9x full
		// load, row counts + per-table CHECKSUM_AGG verified identical; see
		// docs/multi-dialect-activation/driver-tuning.md §4, MSSQL row).
		// NOTE: do NOT combine with sendStringParametersAsUnicode=false —
		// that combination corrupts VARCHAR data in mssql-jdbc 12.8.x (bulk
		// copy inserts the byte[] toString, "[B@...", verified 2026-07-03 on
		// exactly the tables where bulk copy engages, i.e. those without a
		// DATETIME column).
		dataSource.setUseBulkCopyForBatchInsert(true);
		return dataSource;
	}

	@Override
	protected Entry<DataSource, Dialect> createDataSource() {
		SQLServerDataSource master = dataSource("master");

		for (int i = 0; i < readinessSeconds() * 10; i++) {
			System.out.print(".");
			try {
				Thread.sleep(100);
				try (Connection connection = master.getConnection();
						Statement statement = connection.createStatement()) {
					statement.executeQuery("SELECT 1").close();
					// server is up: make sure the test database exists
					statement.executeUpdate(
							"IF DB_ID('" + MSSQL_DATABASE + "') IS NULL CREATE DATABASE " + MSSQL_DATABASE);
				}
				SQLServerDataSource dataSource = dataSource(MSSQL_DATABASE);
				try (Connection connection = dataSource.getConnection()) {
					Dialect dialect = TestDialects.resolve(connection);
					return new SimpleEntry<>(dataSource, dialect);
				}
			} catch (Exception e) {
				System.out.print("-");
			}
		}
		return null;
	}

	@Override
	protected String image() {
		return "mcr.microsoft.com/mssql/server:2022-latest";
	}

}
