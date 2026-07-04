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
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.eclipse.daanse.jdbc.datasource.clickhouse.api.Constants;
import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.github.dockerjava.api.model.PortBinding;

import aQute.bnd.annotation.spi.ServiceProvider;

/**
 * ClickHouse PROBE provider (Batch C) — deliberately minimal, over the HTTP
 * interface (port 8123) of the official image.
 *
 * <p>{@code the_db} (underscore): unquoted ClickHouse identifiers must match
 * {@code [a-zA-Z_][0-9a-zA-Z_]*}, so the MySQL provider's dotted names cannot
 * be aligned literally.
 *
 * <p>{@code default_table_engine=MergeTree} is shipped as a per-session
 * server setting via the driver's {@code custom_settings} connection
 * property, so plain {@code CREATE TABLE} statements from the CSV loader
 * need no ENGINE clause.
 *
 * <p>The com.clickhouse:clickhouse-jdbc jar publishes NO OSGi manifest
 * headers, so this provider does not touch {@code com.clickhouse.*} itself.
 * Instead it uses the {@code org.eclipse.daanse.jdbc.datasource.clickhouse}
 * bundle (which EMBEDS the shaded driver on its Bundle-ClassPath, mirroring
 * the Oracle module): a factory configuration for
 * {@link Constants#PID_DATASOURCE} is pushed via ConfigurationAdmin and the
 * resulting {@link DataSource} service is looked up from the service
 * registry. Consequence: this provider only works inside OSGi (the TCK's
 * test.bndrun runtime; surefire is skipped anyway).
 */
@ServiceProvider(value = DatabaseProvider.class)
public class ClickHouseDatabaseProvider extends AbstractDockerBasesDatabaseProvider implements DatabaseProvider {

	public static String CLICKHOUSE_DB = "the_db";
	public static String CLICKHOUSE_USER = "the.user";
	public static String CLICKHOUSE_PASSWORD = "the.pw";
	public static int PORT = 8123;
	public static String serverName = "localhost";

	/** name of the (idempotent) factory configuration, reused per activation */
	private static final String CONFIG_NAME = "mondrian-tck-clickhouse";

	@Override
	public String id() {
		return "clickhouse";
	}

	@Override
	protected PortBinding portBinding() {
		return PortBinding.parse(port + ":" + PORT);
	}

	@Override
	protected List<String> env() {
		ArrayList<String> envs = new ArrayList<>();
		envs.add("CLICKHOUSE_USER=" + CLICKHOUSE_USER);
		envs.add("CLICKHOUSE_PASSWORD=" + CLICKHOUSE_PASSWORD);
		envs.add("CLICKHOUSE_DB=" + CLICKHOUSE_DB);
		envs.add("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT=1");
		return envs;
	}

	@Override
	protected Entry<DataSource, Dialect> createDataSource() {
		BundleContext bundleContext = FrameworkUtil.getBundle(ClickHouseDatabaseProvider.class).getBundleContext();

		ServiceReference<ConfigurationAdmin> caRef = bundleContext.getServiceReference(ConfigurationAdmin.class);
		if (caRef == null) {
			throw new IllegalStateException("ClickHouseDatabaseProvider: no ConfigurationAdmin service");
		}
		ConfigurationAdmin configurationAdmin = bundleContext.getService(caRef);
		try {
			// named factory config: the same configuration is reused/updated on every
			// activation instead of piling up one component instance per activation
			Configuration configuration = configurationAdmin.getFactoryConfiguration(Constants.PID_DATASOURCE,
					CONFIG_NAME, "?");
			Dictionary<String, Object> props = new Hashtable<>();
			props.put(Constants.DATASOURCE_PROPERTY_USER, CLICKHOUSE_USER);
			props.put(Constants.DATASOURCE_PROPERTY_PASSWORD, CLICKHOUSE_PASSWORD);
			props.put(Constants.DATASOURCE_PROPERTY_SERVERNAME, serverName);
			props.put(Constants.DATASOURCE_PROPERTY_PORTNUMBER, port);
			props.put(Constants.DATASOURCE_PROPERTY_DATABASENAME, CLICKHOUSE_DB);
			props.put(Constants.DATASOURCE_PROPERTY_CUSTOM_SETTINGS, "default_table_engine=MergeTree");
			props.put("daanse.test.name", CONFIG_NAME);
			configuration.update(props);
		} catch (Exception e) {
			throw new IllegalStateException("ClickHouseDatabaseProvider: could not push DataSource configuration", e);
		}

		// the port is fresh per activation, so matching on it guarantees we see the
		// component instance for THIS configuration update, not a stale one
		String filter = "(&(daanse.test.name=" + CONFIG_NAME + ")(" + Constants.DATASOURCE_PROPERTY_PORTNUMBER + "="
				+ port + "))";

		for (int i = 0; i < readinessSeconds() * 10; i++) {
			System.out.print(".");
			try {
				Thread.sleep(100);
				Collection<ServiceReference<DataSource>> refs = bundleContext.getServiceReferences(DataSource.class,
						filter);
				if (refs.isEmpty()) {
					System.out.print("-");
					continue;
				}
				DataSource dataSource = bundleContext.getService(refs.iterator().next());
				Connection connection = dataSource.getConnection();
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
		return "clickhouse/clickhouse-server:latest";
	}

}
