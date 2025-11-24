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
package org.opencube.junit5.dbprovider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.jdbc.db.dialect.db.mysql.MySqlDialect;

import com.github.dockerjava.api.model.PortBinding;
import com.mysql.cj.jdbc.MysqlDataSource;
import com.mysql.jdbc.Driver;

import aQute.bnd.annotation.spi.ServiceProvider;

@ServiceProvider(value = DatabaseProvider.class)
public class MySqlDatabaseProvider extends AbstractDockerBasesDatabaseProvider implements DatabaseProvider{

	public static String MYSQL_ROOT_PASSWORD = "the.root.pw";
	public static String MYSQL_DATABASE = "the.db";
	public static String MYSQL_USER = "the.user";
	public static String MYSQL_PASSWORD = "the.pw";
	public static int PORT = 3306;
	public static String serverName = "0.0.0.0";

	@Override
	protected PortBinding portBinding() {
		return PortBinding.parse(port + ":" + PORT);
	}

	@Override
	protected List<String> env() {
		ArrayList<String> envs = new ArrayList<>();
		envs.add("MYSQL_ROOT_PASSWORD=" + MYSQL_ROOT_PASSWORD);
		envs.add("MYSQL_USER=" + MYSQL_USER);
		envs.add("MYSQL_PASSWORD=" + MYSQL_PASSWORD);
		envs.add("MYSQL_DATABASE=" + MYSQL_DATABASE);

		return envs;
	}

	@Override
	protected Entry<DataSource, Dialect> createDataSource() {
		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setServerName(serverName);
		dataSource.setPort(port);
		dataSource.setPassword(MYSQL_PASSWORD);
		dataSource.setUser(MYSQL_USER);
		dataSource.setDatabaseName(MYSQL_DATABASE);
		try {
			java.sql.DriverManager.registerDriver(new Driver());
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		try {
			dataSource.setRewriteBatchedStatements(true);
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		for (int i = 0; i < 1000; i++) {
			System.out.print(".");

			try {
				Thread.sleep(100);

				Connection connection = dataSource.getConnection(MYSQL_USER, MYSQL_PASSWORD);
				// wait until connection is possible
				Dialect dialect = new MySqlDialect(connection);
				connection.close();
				return new SimpleEntry<>(dataSource, dialect);

			} catch (Exception e) {
//				e.printStackTrace();
				System.out.print("-");
			}

            System.out.print("");
		}
		return null;
	}

	@Override
	protected String image() {
		return "mysql:latest";
	}

}
