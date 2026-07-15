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
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.eclipse.daanse.sql.dialect.api.Dialect;
import org.mariadb.jdbc.MariaDbDataSource;

import com.github.dockerjava.api.model.PortBinding;

import aQute.bnd.annotation.spi.ServiceProvider;

/**
 * MariaDB provider (Batch C). User/password/database names are aligned with
 * {@link MySqlDatabaseProvider} so the CSV data-load path is identical.
 */
@ServiceProvider(value = DatabaseProvider.class)
public class MariaDbDatabaseProvider extends AbstractDockerBasesDatabaseProvider implements DatabaseProvider {

	public static String MARIADB_ROOT_PASSWORD = "the.root.pw";
	public static String MARIADB_DATABASE = "the.db";
	public static String MARIADB_USER = "the.user";
	public static String MARIADB_PASSWORD = "the.pw";
	public static int PORT = 3306;
	public static String serverName = "localhost";

	@Override
	public String id() {
		return "mariadb";
	}

	@Override
	protected List<String> cmd() {
		return List.of("--max-connections=10000");
	}

	@Override
	protected PortBinding portBinding() {
		return PortBinding.parse(port + ":" + PORT);
	}

	@Override
	protected List<String> env() {
		ArrayList<String> envs = new ArrayList<>();
		envs.add("MARIADB_ROOT_PASSWORD=" + MARIADB_ROOT_PASSWORD);
		envs.add("MARIADB_USER=" + MARIADB_USER);
		envs.add("MARIADB_PASSWORD=" + MARIADB_PASSWORD);
		envs.add("MARIADB_DATABASE=" + MARIADB_DATABASE);
		return envs;
	}

	@Override
	protected Entry<DataSource, Dialect> createDataSource() {
		MariaDbDataSource dataSource = new MariaDbDataSource();
		try {
			dataSource.setUrl("jdbc:mariadb://" + serverName + ":" + port + "/" + MARIADB_DATABASE);
			dataSource.setUser(MARIADB_USER);
			dataSource.setPassword(MARIADB_PASSWORD);
		} catch (Exception e) {
			throw new IllegalStateException("MariaDbDatabaseProvider: DataSource setup failed", e);
		}

		for (int i = 0; i < readinessSeconds() * 10; i++) {
			System.out.print(".");
			try {
				Thread.sleep(100);
				Connection connection = dataSource.getConnection(MARIADB_USER, MARIADB_PASSWORD);
				// wait until connection is possible
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
		return "mariadb:11";
	}

}
