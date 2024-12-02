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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.eclipse.daanse.jdbc.db.dialect.db.mysql.MySqlDialect;
import org.eclipse.daanse.jdbc.db.api.meta.MetaInfo;
import org.eclipse.daanse.jdbc.db.core.DatabaseServiceImpl;
import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

//@ServiceProvider(value = DatabaseProvider.class)
public class SQLiteDatabaseProvider implements DatabaseProvider {

	private static String getTempFile() {
		try {
			Path temp = Path.of("sqlite.db");
			if (Files.exists(temp)) {
				Files.delete(temp);
			}
			return temp.toFile().getAbsolutePath().toString();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	private static final String JDBC_SQLITE_MEMORY = "jdbc:sqlite:" + getTempFile();

	@Override
	public void close() throws IOException {

	}

	@Override
	public Entry<DataSource, Dialect> activate() {
		SQLiteConfig cfg = new SQLiteConfig();
		SQLiteDataSource dataSource = new SQLiteDataSource(cfg);
		dataSource.setUrl(JDBC_SQLITE_MEMORY);
		try {
			Connection connection = dataSource.getConnection();
			DatabaseServiceImpl databaseServiceImpl = new DatabaseServiceImpl();
			MetaInfo metaInfo = databaseServiceImpl.createMetaInfo(connection);
			Dialect dialect = new MySqlDialect(metaInfo);
			connection.close();
			return new SimpleEntry<>(dataSource, dialect);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

}
