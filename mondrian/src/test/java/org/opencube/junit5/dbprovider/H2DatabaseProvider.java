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

import org.eclipse.daanse.sql.dialect.api.Dialect;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.spi.ServiceProvider;

@ServiceProvider(value = DatabaseProvider.class)
public class H2DatabaseProvider implements DatabaseProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(H2DatabaseProvider.class);

	@Override
	public String id() {
		return "h2";
	}

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

		long tActivate = System.nanoTime();

		// mem: (pure in-memory) instead of memFS: (H2 file format on an in-memory
		// filesystem): measured 3.4x faster full FoodMart load (14.1s -> 4.1s,
		// docs/multi-dialect-activation/driver-tuning.md). DB_CLOSE_DELAY=-1 keeps
		// the named database alive after the probe connection below closes (memFS
		// survived implicitly because its files stay in the in-memory FS registry).
		// DAANSE_H2_URL_EXTRA appends further ;SETTING=value pairs, so driver settings can be
		// measured against a run without recompiling. Empty by default: the URL is unchanged.
		String extra = System.getenv("DAANSE_H2_URL_EXTRA");
		String JDBC_SQLITE_MEMORY = "jdbc:h2:mem:" + UUID.randomUUID().toString()
				+ ";DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"
				+ (extra == null || extra.isBlank() ? "" : ";" + extra.strip());
		JdbcDataSource cpDataSource = new JdbcDataSource();
		cpDataSource.setUrl(JDBC_SQLITE_MEMORY);
		cpDataSource.setUser("sa");
		cpDataSource.setPassword("sa");
		try {
			connection = cpDataSource.getConnection();
			Dialect dialect = TestDialects.resolve(connection);

			connection.close();
			// embedded: db-ready = the (tiny) connect duration, for comparability with docker DBs
			long ms = (System.nanoTime() - tActivate) / 1_000_000;
			LOGGER.warn("DBTIMING db={} phase=db-ready ms={}", id(), ms);
			LOGGER.warn("DBTIMING db={} phase=context-first ms={} detail=fresh,epoch={}", id(), ms,
					System.currentTimeMillis() / 1000);
			return new SimpleEntry<>(cpDataSource, dialect);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

}
