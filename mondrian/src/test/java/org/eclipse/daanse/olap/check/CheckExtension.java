package org.eclipse.daanse.olap.check;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.sql.DataSource;

import org.eclipse.daanse.jdbc.db.api.SqlStatementGenerator;
import org.eclipse.daanse.jdbc.db.api.meta.MetaInfo;
import org.eclipse.daanse.jdbc.db.core.DatabaseServiceImpl;
import org.eclipse.daanse.jdbc.db.core.SqlStatementGeneratorImpl;
import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.check.runtime.api.OlapCheckSuiteSupplier;
import org.eclipse.daanse.rolap.common.aggregator.AggregationFactoryImpl;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestWatcher;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dbprovider.H2DatabaseProvider;

public class CheckExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback,
		TestWatcher, ParameterResolver {
	private TestContextImpl context;
	private static final Namespace NS = Namespace.create(CheckExecution.class);

	/* ===== SUITE / CLASS ===== */

	@Override
	public void beforeAll(ExtensionContext context) {
		CheckSuite suite = CheckSuite.start(context.getRequiredTestClass().getName());
		context.getStore(NS).put("suite", suite);
	}

	@Override
	public void afterAll(ExtensionContext context) {
		CheckSuite suite = context.getStore(NS).remove("suite", CheckSuite.class);
		if (suite != null)
			suite.printResult();
	}

	/* ===== TEST ===== */

	@Override
	public void beforeEach(ExtensionContext context) {
		CheckSuite suite = context.getStore(NS).get("suite", CheckSuite.class);

		CheckExecution exec = suite.startExecution(context.getUniqueId(), context.getDisplayName());

		context.getStore(NS).put(execKey(context), exec);
	}

	@Override
	public void afterEach(ExtensionContext context) {
		CheckExecution exec = context.getStore(NS).remove(execKey(context), CheckExecution.class);
		if (exec != null) {
			exec.printResult(context);
		}
	}

	/* ===== RESULT ===== */

	@Override
	public void testFailed(ExtensionContext context, Throwable cause) {
		CheckExecution exec = context.getStore(NS).get(execKey(context), CheckExecution.class);
		if (exec != null) {
			// exec.markFailed(cause);
		}
	}

	/* ===== CALLBACK ===== */

	@Override
	public boolean supportsParameter(ParameterContext pc, ExtensionContext ec) {
		Class<?>[] classes = pc.getParameter().getType().getInterfaces();
		return pc.getParameter().getType() == CheckExecution.class
				|| checkClasses(classes, OlapCheckSuiteSupplier.class) || pc.getParameter().getType() == Context.class
				|| checkClasses(classes, CatalogMappingSupplier.class);
	}

	private boolean checkClasses(Class<?>[] classes, Class<?> cl) {
		if (classes != null) {
			for (Class<?> c : classes) {
				if (c == cl) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Object resolveParameter(ParameterContext pc, ExtensionContext ec) {
		
		if (pc.getParameter().getType() == CheckExecution.class) {
			CheckExecution exec = ec.getStore(NS).get(execKey(ec), CheckExecution.class);
			return exec;
		}
		if (pc.getParameter().getType() == Context.class && pc.getIndex() == 1) {
			H2DatabaseProvider dp = new H2DatabaseProvider(); // TODO use other database type too
			Entry<DataSource, Dialect> dataBaseInfo = dp.activate();
			context = new TestContextImpl();
			context.setDataSource(dataBaseInfo.getKey());
			context.setDialect(dataBaseInfo.getValue());
			context.setAggragationFactory(
					new AggregationFactoryImpl(dataBaseInfo.getValue(), context.getCustomAggregators()));
			context.setName("TestContext");
			return context;
		}
		if (checkClasses(pc.getParameter().getType().getInterfaces(), OlapCheckSuiteSupplier.class) && pc.getIndex() == 2) {
			try {
				return pc.getParameter().getType().getConstructor().newInstance();
			} catch (Exception e) {
				new ParameterResolutionException("Unsupported parameter");
			}
		}
		if (checkClasses(pc.getParameter().getType().getInterfaces(), CatalogMappingSupplier.class) && pc.getIndex() == 3) {
			try {
				CatalogMappingSupplier catalogMappingSupplier = (CatalogMappingSupplier) pc.getParameter().getType()
						.getConstructor().newInstance();
				context.setCatalogMappingSupplier(catalogMappingSupplier);

				// load data
				loadDatafromPackage(pc.getParameter().getType(), "data");

				return catalogMappingSupplier;
			} catch (Exception e) {
				new ParameterResolutionException("Unsupported parameter");
			}
		}
		throw new ParameterResolutionException("Unsupported parameter");
	}

	public void loadDatafromPackage(Class<?> c, String path) throws Exception {
		java.net.URL resource = c.getResource("");

		if (resource == null) {
			return; // resource not find
		}

		// important: JAR-файлы use protocol "jar:"
		if ("jar".equals(resource.getProtocol())) {
			// get path to JAR from URL
			String jarPath = resource.getPath().substring(0, resource.getPath().indexOf("!"));
			java.net.URL jarUrl = new java.net.URL(jarPath);
			try (java.util.jar.JarFile jar = new JarFile(jarUrl.getFile())) {
				Enumeration<JarEntry> entries = jar.entries();

				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					String entryName = entry.getName();
					// filter by path
					if (entryName.startsWith(path) && !entryName.equals(path + "/")) {
						// remove '/'
						String resourceName = entryName.substring(path.length() + 1);
						if (!resourceName.isEmpty()) {
							InputStream is = jar.getInputStream(entry);
							try (Connection connection = context.getDataSource().getConnection()) {
								DatabaseServiceImpl databaseService = new DatabaseServiceImpl();
								MetaInfo metaInfo = databaseService.createMetaInfo(connection);
								SqlStatementGenerator sqlStatementGenerator = new SqlStatementGeneratorImpl(metaInfo);
								DataLoadUtil.loadTable(connection, sqlStatementGenerator, is, resourceName);
							} catch (SQLException e) {
								throw new RuntimeException("Database connection error", e);
							}
						}
					}
				}
			}
		}

	}

	private String execKey(ExtensionContext ctx) {
		return "exec:" + ctx.getUniqueId();
	}
}
