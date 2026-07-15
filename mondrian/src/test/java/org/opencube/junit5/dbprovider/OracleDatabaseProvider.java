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
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.eclipse.daanse.jdbc.datasource.oracle.api.Constants;
import org.eclipse.daanse.sql.dialect.api.Dialect;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.github.dockerjava.api.model.PortBinding;

import aQute.bnd.annotation.spi.ServiceProvider;

/**
 * Oracle provider (Batch C), based on the gvenzl "Oracle Free" image
 * (faststart variant: the database is pre-created in the image, not built on
 * first boot). The image creates {@code APP_USER} inside the FREEPDB1
 * pluggable database; tests connect as that user, its schema is the foodmart
 * target.
 *
 * <p>{@code the_user} (underscore, not dot): Oracle identifiers cannot carry
 * a dot without quoting, so the MySQL provider's {@code the.user} cannot be
 * aligned literally.
 *
 * <p>The ojdbc11 driver jar publishes NO OSGi manifest headers, so this
 * provider does not touch {@code oracle.jdbc.*} itself. Instead it uses the
 * {@code org.eclipse.daanse.jdbc.datasource.oracle} bundle (which EMBEDS
 * ojdbc11 on its Bundle-ClassPath): a factory configuration for
 * {@link Constants#PID_DATASOURCE} is pushed via ConfigurationAdmin and the
 * resulting {@link DataSource} service is looked up from the service
 * registry. Consequence: this provider only works inside OSGi (the TCK's
 * test.bndrun runtime; surefire is skipped anyway).
 */
@ServiceProvider(value = DatabaseProvider.class)
public class OracleDatabaseProvider extends AbstractDockerBasesDatabaseProvider implements DatabaseProvider {

	public static String ORACLE_PASSWORD = "the.root.pw";
	public static String APP_USER = "the_user";
	public static String APP_USER_PASSWORD = "the.pw";
	public static String SERVICE = "FREEPDB1";
	public static int PORT = 1521;
	public static String serverName = "localhost";

	/** name of the (idempotent) factory configuration, reused per activation */
	private static final String CONFIG_NAME = "mondrian-tck-oracle";

	@Override
	public String id() {
		return "oracle";
	}

	@Override
	protected int readinessSeconds() {
		// even the faststart image needs several minutes to open the PDB
		return 600;
	}

	@Override
	protected PortBinding portBinding() {
		return PortBinding.parse(port + ":" + PORT);
	}

	@Override
	protected List<String> env() {
		ArrayList<String> envs = new ArrayList<>();
		envs.add("ORACLE_PASSWORD=" + ORACLE_PASSWORD);
		envs.add("APP_USER=" + APP_USER);
		envs.add("APP_USER_PASSWORD=" + APP_USER_PASSWORD);
		return envs;
	}

	@Override
	protected Entry<DataSource, Dialect> createDataSource() {
		BundleContext bundleContext = FrameworkUtil.getBundle(OracleDatabaseProvider.class).getBundleContext();

		ServiceReference<ConfigurationAdmin> caRef = bundleContext.getServiceReference(ConfigurationAdmin.class);
		if (caRef == null) {
			throw new IllegalStateException("OracleDatabaseProvider: no ConfigurationAdmin service");
		}
		ConfigurationAdmin configurationAdmin = bundleContext.getService(caRef);
		try {
			// named factory config: the same configuration is reused/updated on every
			// activation instead of piling up one component instance per activation
			Configuration configuration = configurationAdmin.getFactoryConfiguration(Constants.PID_DATASOURCE,
					CONFIG_NAME, "?");
			Dictionary<String, Object> props = new Hashtable<>();
			props.put(Constants.DATASOURCE_PROPERTY_USER, APP_USER);
			props.put(Constants.DATASOURCE_PROPERTY_PASSWORD, APP_USER_PASSWORD);
			props.put(Constants.DATASOURCE_PROPERTY_SERVICENAME, SERVICE);
			props.put(Constants.DATASOURCE_PROPERTY_SERVERNAME, serverName);
			props.put(Constants.DATASOURCE_PROPERTY_PORTNUMBER, port);
			// oracle.jdbc.defaultRowPrefetch pass-through
			// (Constants.DATASOURCE_PROPERTY_DEFAULTROWPREFETCH in datasource
			// bundles that support it; literal here so this class still
			// compiles against older bundle versions — unknown config
			// properties are simply ignored by the component). Driver default
			// is 10 rows per round trip; member scans (customer: 10 281 rows)
			// are latency-bound at that setting.
			props.put("defaultRowPrefetch", 256);
			props.put("daanse.test.name", CONFIG_NAME);
			configuration.update(props);
		} catch (Exception e) {
			throw new IllegalStateException("OracleDatabaseProvider: could not push DataSource configuration", e);
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
		return "gvenzl/oracle-free:23-slim-faststart";
	}

}
