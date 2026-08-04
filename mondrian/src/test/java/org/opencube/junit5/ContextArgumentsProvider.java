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
package org.opencube.junit5;

import java.io.InputStream;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.eclipse.daanse.sql.dialect.api.Dialect;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.rolap.common.aggregator.AggregationFactoryImpl;
import org.glassfish.jaxb.runtime.v2.JAXBContextFactory;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.DataLoader;
import org.eclipse.daanse.jdbc.datasource.testkit.api.DatabaseProvider;
import org.eclipse.daanse.jdbc.datasource.testkit.api.ActiveDatabase;
import org.opencube.junit5.xmltests.ResourceTestCase;
import org.opencube.junit5.xmltests.XmlResourceRoot;
import org.opencube.junit5.xmltests.XmlResourceTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.Cardinality;
import aQute.bnd.annotation.spi.ServiceConsumer;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

@ServiceConsumer(cardinality = Cardinality.MULTIPLE, value = DatabaseProvider.class)
public class ContextArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<ContextSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContextArgumentsProvider.class);
	private ContextSource contextSource;

	/** Loaded databases per provider, keyed by isolation key (dataset, plus the test class when it wants its own). */
	/**
	 * Concurrent, and the inner maps too: with parallel test execution several
	 * classes ask for a dataset at the same moment, and the load must happen once.
	 */
	private static final Map<Class<? extends DatabaseProvider>, Map<String, ActiveDatabase>> store =
			new ConcurrentHashMap<>();

	/**
	 * Data loaders that already failed once per provider in this JVM. Retrying a
	 * deterministic load failure once per test method just repeats it (and churns
	 * one container per test on the docker providers); the markers are wiped when
	 * markers are per isolation key, so one dataset's failure leaves the others alone.
	 */
	private static final Map<Class<? extends DatabaseProvider>, Set<String>> failedLoaders =
			new ConcurrentHashMap<>();

	/** Carries a checked load failure out of computeIfAbsent's mapping function. */
	private static final class LoadFailed extends RuntimeException {
		private static final long serialVersionUID = 1L;

		LoadFailed(Exception cause) {
			super(cause);
		}
	}

	/**
	 * How often every test method is invoked, from {@code DAANSE_TEST_REPEAT} or
	 * {@code -Ddaanse.test.repeat}; 1 by default, so an ordinary run is unchanged.
	 *
	 * <p>
	 * For hunting failures that only appear under load. Each repetition gets its
	 * own Context - a fresh {@link org.opencube.junit5.context.TestContextImpl}
	 * over the same, already loaded database - so the repetitions are as
	 * independent of each other as two separate tests are, and configuration one
	 * of them sets cannot leak into the others. With parallel execution on they
	 * run concurrently, which is the point: it multiplies the pressure on the
	 * shared database without needing the whole suite.
	 * </p>
	 */
	private static int repeatCount() {
		String value = System.getenv("DAANSE_TEST_REPEAT");
		if (value == null || value.isBlank()) {
			value = System.getProperty("daanse.test.repeat");
		}
		if (value == null || value.isBlank()) {
			return 1;
		}
		int n = Integer.parseInt(value.trim());
		return n < 1 ? 1 : n;
	}

	@Override
	public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {

		List<XmlResourceTestCase> xmlTestCases = readTestcases(extensionContext);

		List<Arguments> argumentss = new ArrayList<>();

		for (int repetition = 0; repetition < repeatCount(); repetition++) {
			// Inside the loop: prepareContexts builds a new TestContextImpl per call,
			// so every repetition is its own context.
			List<TestContext> contexts = prepareContexts(extensionContext);

			if (contexts == null || contexts.isEmpty()) {

				if (xmlTestCases != null && !xmlTestCases.isEmpty()) {

					for (XmlResourceTestCase xmlTestCase : xmlTestCases) {
						argumentss.add(Arguments.of(xmlTestCase));
					}
				}
			} else {
				for (Context<?> context : contexts) {

					if (xmlTestCases == null || xmlTestCases.isEmpty()) {
						argumentss.add(Arguments.of(context));

					} else {
						for (XmlResourceTestCase xmlTestCase : xmlTestCases) {
							argumentss.add(Arguments.of(context, xmlTestCase));
						}

					}

				}
			}
		}
		return argumentss.stream();

	}


	private List<XmlResourceTestCase> readTestcases(ExtensionContext extensionContext) {

		Optional<AnnotatedElement> oElement = extensionContext.getElement();
		if (oElement.isPresent()) {
			if (oElement.get() instanceof Method) {
				Method method = (Method) oElement.get();
				for (Parameter param : method.getParameters()) {
					if (ResourceTestCase.class.equals(param.getType())) {
						Optional<Class<?>> oTestclass = extensionContext.getTestClass();
						if (oTestclass.isPresent()) {
							Class<?> testclass = oTestclass.get();

							InputStream is = testclass.getResourceAsStream(testclass.getSimpleName() + ".ref.xml");
							JAXBContext jaxbContext = null;
							try {

//								Map.of(JAXBContext.JAXB_CONTEXT_FACTORY, JaxBConFa));

								jaxbContext = new JAXBContextFactory()
										.createContext(new Class[] { XmlResourceRoot.class }, Map.<String, Object>of());

								Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

//								try {
//									System.out.println(new String(is.readAllBytes()));
//								} catch (IOException e) {
//									// TODO Auto-generated catch block
//									e.printStackTrace();
//								}
								XmlResourceRoot o = (XmlResourceRoot) jaxbUnmarshaller.unmarshal(is);

								return o.testCase;
							} catch (JAXBException e) {
							    LOGGER.error("readTestcases error", e);
							}

						}
					}
				}
			}
		}

		return null;
	}

	private List<TestContext> prepareContexts(ExtensionContext extensionContext) {
		List<? extends DatabaseProvider> providers;
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader()); //for withSchemaProcessor(context, MyFoodmart.class);
		Class<? extends DatabaseProvider>[] dbHandlerClasses = contextSource.database();
		if (dbHandlerClasses == null || dbHandlerClasses.length == 0) {
			// select ONE registered provider by id: env DAANSE_TEST_DB -> sysprop
			// daanse.test.db -> default "duckdb" (in-process, no docker needed).
			// Docker-unavailable still SKIPS the suite via the existing
			// evaluateExecutionCondition path.
			String dbId = System.getenv("DAANSE_TEST_DB");
			if (dbId == null || dbId.isBlank()) {
				dbId = System.getProperty("daanse.test.db", "duckdb");
			}
			final String selectedDbId = dbId;
			providers = ServiceLoader.load(DatabaseProvider.class, this.getClass().getClassLoader()).stream()
					.map(Provider::get).filter(p -> selectedDbId.equalsIgnoreCase(p.id())).toList();
			if (providers.isEmpty()) {
				throw new IllegalStateException("No DatabaseProvider id=" + selectedDbId);
			}
		} else {
			providers = Stream.of(dbHandlerClasses).map(c -> {
				try {
					return c.getConstructor().newInstance();
				} catch (Exception e) {
					LOGGER.error("prepareContexts error", e);
					return null;
				}
			}).toList();
		}
		List<TestContext> args = providers.stream().parallel().map(dbp -> {

			ActiveDatabase dataBaseInfo = null;
			Class<? extends DatabaseProvider> clazzProvider = dbp.getClass();


			List<TestContext> testingContexts = new ArrayList<>();

			Optional<AnnotatedElement> oElement = extensionContext.getElement();
			if (oElement.isPresent()) {
				if (oElement.get() instanceof Method method) {
					for (Parameter param : method.getParameters()) {
						if (TestContext.class.isAssignableFrom(param.getType()) ||
                            Context.class.isAssignableFrom(param.getType())) {
							ContextSource contextSource=method.getAnnotation(ContextSource.class);

							try {

								Class<? extends DataLoader> dataLoaderClass = contextSource.dataloader();

								// One database per dataset. A test class that mutates its database
								// asks for a private one, so it neither disturbs the shared dataset
								// nor forces anybody else to reload.
								String isolationKey = dataLoaderClass.getSimpleName();
								IsolationKey ownKey = extensionContext.getTestClass()
										.map(c -> c.getAnnotation(IsolationKey.class)).orElse(null);
								if (ownKey != null) {
									isolationKey = isolationKey + "-" + ownKey.value();
								}

								Map<String, ActiveDatabase> storedLoaders =
										store.computeIfAbsent(clazzProvider, k -> new ConcurrentHashMap<>());

								// A load that already failed for this key stays failed: repeating
								// activate()+loadData() once per test method only repeats the
								// identical failure.
								Set<String> failed = failedLoaders.computeIfAbsent(clazzProvider,
										k -> ConcurrentHashMap.newKeySet());
								if (failed.contains(isolationKey)) {
									throw new IllegalStateException("dataset " + isolationKey
											+ " already failed for provider " + clazzProvider.getName()
											+ " in this run; not retrying");
								}

								// computeIfAbsent, not containsKey-then-put: under parallel execution
								// the check-then-act version let two classes miss at the same moment and
								// each build its own database, which is exactly the duplicate loading
								// this cache exists to prevent. Here the first caller loads while the
								// others block on the same key and are handed the finished database.
								final String key = isolationKey;
								final String datasetName = dataLoaderClass.getSimpleName();
								try {
									boolean[] loadedHere = { false };
									dataBaseInfo = storedLoaders.computeIfAbsent(key, k -> {
										loadedHere[0] = true;
										LOGGER.info("dataset {} on {}: creating and loading (key={})",
												datasetName, dbp.id(), k);
										try {
											ActiveDatabase db = dbp.activate(k);
											dataLoaderClass.getConstructor().newInstance().loadData(db);
											return db;
										} catch (Exception e) {
											throw new LoadFailed(e);
										}
									});
									if (!loadedHere[0]) {
										LOGGER.info("dataset {} on {}: reusing the loaded database (key={})",
												datasetName, dbp.id(), key);
									}
								} catch (LoadFailed wrapped) {
									failed.add(key);
									// Only this key is marked. Every other dataset lives in its own
									// database and is untouched by this failure.
									throw (Exception) wrapped.getCause();
								}

							} catch (Exception e) {

                                LOGGER.error("prepareContexts error", e);
								throw new RuntimeException(e);
							}

							TestContextImpl testContextImpl = new TestContextImpl();
							testContextImpl.setConnectionPool(dataBaseInfo.connectionPool());
							testContextImpl.setDialect(dataBaseInfo.dialect());
							testContextImpl.setAggragationFactory(new AggregationFactoryImpl(testContextImpl.getCustomAggregators()));
							testContextImpl.setName("TestContext");


							Stream.of(contextSource.propertyUpdater()).map(c -> {
								try {
									return c.getConstructor().newInstance();
								} catch (Exception e) {
                                    LOGGER.error("prepareContexts error", e);
									throw new RuntimeException(e);
								}
							}).forEachOrdered(u->{
								u.updateContext(testContextImpl);

							});

							testingContexts.add(testContextImpl);
						}
					}
				}
			}
			return testingContexts;
		}).flatMap(Collection::stream).toList();
		return args;
	}

	@Override
	public void accept(ContextSource annotation) {
		this.contextSource = annotation;

	}

}
