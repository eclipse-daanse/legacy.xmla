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

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import aQute.bnd.annotation.spi.ServiceProvider;

@ServiceProvider(value = DatabaseProvider.class)
public class SQLiteDatabaseProvider implements DatabaseProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(SQLiteDatabaseProvider.class);

	// Named shared in-memory database (NOT a cwd file): every connection to this
	// url sees the same database while at least one connection is open.
	private final String url = "jdbc:sqlite:file:daanse-" + UUID.randomUUID() + "?mode=memory&cache=shared";

	// Pin: a shared-cache in-memory SQLite database is dropped when its LAST
	// connection closes. Keep one connection open for the provider's lifetime so
	// the loaded data survives between data-load and test connections.
	private Connection pin;

	@Override
	public String id() {
		return "sqlite";
	}

	@Override
	public void close() throws IOException {
		if (pin != null) {
			try {
				pin.close();
			} catch (SQLException e) {
				throw new IOException(e);
			} finally {
				pin = null;
			}
		}
	}

	@Override
	public Entry<DataSource, Dialect> activate() {
		long tActivate = System.nanoTime();
		boolean fresh = pin == null;
		SQLiteConfig cfg = new SQLiteConfig();
		// writers block instead of failing immediately with SQLITE_BUSY
		cfg.setBusyTimeout(30000);
		SQLiteDataSource dataSource = new SQLiteDataSource(cfg);
		dataSource.setUrl(url);
		try {
			if (pin == null) {
				pin = dataSource.getConnection();
			}
			Dialect dialect = TestDialects.resolve(pin);
			// embedded: db-ready = the (tiny) connect duration, for comparability with docker DBs
			long ms = (System.nanoTime() - tActivate) / 1_000_000;
			LOGGER.warn("DBTIMING db={} phase=db-ready ms={}", id(), ms);
			LOGGER.warn("DBTIMING db={} phase=context-first ms={} detail={},epoch={}", id(), ms,
					fresh ? "fresh" : "reused", System.currentTimeMillis() / 1000);
			return new SimpleEntry<>(dataSource, dialect);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

}
