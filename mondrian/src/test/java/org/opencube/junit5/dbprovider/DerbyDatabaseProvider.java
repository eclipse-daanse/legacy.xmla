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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.spi.ServiceProvider;

/**
 * Embedded Apache Derby, in-memory ({@code memory:foodmart-<UUID>}). The
 * in-memory database lives for the whole JVM regardless of open connections.
 */
@ServiceProvider(value = DatabaseProvider.class)
public class DerbyDatabaseProvider implements DatabaseProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(DerbyDatabaseProvider.class);

	private final String databaseName = "memory:foodmart-" + UUID.randomUUID();

	@Override
	public String id() {
		return "derby";
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public Entry<DataSource, Dialect> activate() {
		long tActivate = System.nanoTime();
		EmbeddedDataSource dataSource = new EmbeddedDataSource();
		dataSource.setDatabaseName(databaseName);
		dataSource.setCreateDatabase("create");
		try {
			Connection connection = dataSource.getConnection();
			Dialect dialect = TestDialects.resolve(connection);
			connection.close();
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

}
