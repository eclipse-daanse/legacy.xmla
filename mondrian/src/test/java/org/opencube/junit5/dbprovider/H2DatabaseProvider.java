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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.UUID;

import javax.sql.DataSource;

import org.eclipse.daanse.jdbc.db.dialect.db.h2.H2Dialect;
import org.eclipse.daanse.jdbc.db.api.meta.MetaInfo;
import org.eclipse.daanse.jdbc.db.core.DatabaseServiceImpl;
import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.h2.jdbcx.JdbcDataSource;

import aQute.bnd.annotation.spi.ServiceProvider;
import mondrian.rolap.RolapCatalogCache;

@ServiceProvider(value = DatabaseProvider.class)
public class H2DatabaseProvider implements DatabaseProvider {

//	private Path testDirPath;
//	private Path testFilePath;
	private Connection connection;

	public H2DatabaseProvider() {

	}

//	private static Path getTempFile() {
//		try {
//
//			Path temp = Files.createTempDirectory("daanse_test_"+UUID.randomUUID().toString()).toAbsolutePath();
//
//
//
//
//			return temp;
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new RuntimeException(e);
//		}
//
//	}

	@Override
	public void close() throws IOException {

//		Files.walk(testDirPath).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
//
//		if (testDirPath != null) {
//			Files.deleteIfExists(testDirPath);
//		}
	}

	@Override
	public Entry<DataSource, Dialect> activate() {



		String JDBC_SQLITE_MEMORY = "jdbc:h2:memFS:" + UUID.randomUUID().toString() + ";DATABASE_TO_UPPER=false";
		JdbcDataSource cpDataSource = new JdbcDataSource();
		cpDataSource.setUrl(JDBC_SQLITE_MEMORY);
		cpDataSource.setUser("sa");
		cpDataSource.setPassword("sa");
		try {
			connection = cpDataSource.getConnection();
			DatabaseServiceImpl databaseServiceImpl = new DatabaseServiceImpl();
			MetaInfo metaInfo = databaseServiceImpl.createMetaInfo(connection);
			Dialect dialect = new H2Dialect(metaInfo);

			connection.close();
			return new SimpleEntry<>(cpDataSource, dialect);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

}
