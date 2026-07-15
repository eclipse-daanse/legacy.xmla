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
import org.opencube.junit5.dbprovider.AbstractDockerBasesDatabaseProvider;
import org.opencube.junit5.dbprovider.DatabaseProvider;
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

	private static Map<Class<? extends DatabaseProvider>, Map<Class<? extends DataLoader>,  Entry<DataSource, Dialect>>> store = new HashMap<>();
	/**
	 * Data loaders that already failed once per provider in this JVM. Retrying a
	 * deterministic load failure once per test method just repeats it (and churns
	 * one container per test on the docker providers); the markers are wiped when
	 * {@link #dockerWasChanged} announces a deliberately re-provisioned environment.
	 */
	private static final Map<Class<? extends DatabaseProvider>, Set<Class<? extends DataLoader>>> failedLoaders =
			new ConcurrentHashMap<>();
	public static boolean dockerWasChanged = true;

	@Override
	public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {

		// TODO: parallel
		List<TestContext> contexts = prepareContexts(extensionContext);
		List<XmlResourceTestCase> xmlTestCases = readTestcases(extensionContext);

		List<Arguments> argumentss = new ArrayList<>();

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
			// daanse.test.db -> default "mysql". Docker-unavailable still SKIPS the
			// suite via the existing evaluateExecutionCondition path.
			String dbId = System.getenv("DAANSE_TEST_DB");
			if (dbId == null || dbId.isBlank()) {
				dbId = System.getProperty("daanse.test.db", "mysql");
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

			Entry<DataSource, Dialect> dataBaseInfo = null;
			Class<? extends DatabaseProvider> clazzProvider = dbp.getClass();

			if (!store.containsKey(clazzProvider)) {
				store.put(clazzProvider, new HashMap<>());
			} else {

			}

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

								Map<Class<? extends DataLoader>, Entry<DataSource, Dialect>> storedLoaders = store
										.get(clazzProvider);
								if (storedLoaders.containsKey(dataLoaderClass) && !dockerWasChanged) {
									dataBaseInfo = storedLoaders.get(dataLoaderClass);
//									dataSource.getKey().put(RolapConnectionProperties.Jdbc.name(), dbp.getJdbcUrl());
								} else {
									// A dataset load that already failed for this provider stays failed:
									// re-running activate()+loadData() once per test method only repeats
									// the identical failure (and, for docker providers, churns one
									// container per test). dockerWasChanged=true wipes the markers, since
									// it announces a deliberately re-provisioned environment.
									Set<Class<? extends DataLoader>> failed = failedLoaders
											.computeIfAbsent(clazzProvider, k -> new HashSet<>());
									if (dockerWasChanged) {
										failed.clear();
									}
									if (failed.contains(dataLoaderClass)) {
										throw new IllegalStateException("data loader " + dataLoaderClass.getName()
												+ " already failed for provider " + clazzProvider.getName()
												+ " in this run; not retrying");
									}
									try {
										dataBaseInfo = dbp.activate();
										DataLoader dataLoader = dataLoaderClass.getConstructor().newInstance();
										dataLoader.loadData(dataBaseInfo);
										storedLoaders.clear();
										storedLoaders.put(dataLoaderClass, dataBaseInfo);
										dockerWasChanged = false;
									} catch (Exception activateOrLoadFailed) {
										failed.add(dataLoaderClass);
										// The docker providers remove the previously running container
										// (remove-before-create) inside activate(). If activate() or the
										// dataset load fails there, every cached Entry in storedLoaders
										// still points at that removed container's port: reusing it would
										// turn one failed dataset switch into a "connection refused"
										// cascade over the whole remaining run, so drop the stale cache and
										// force re-activation for the next test class. Embedded providers
										// (h2, sqlite, ...) create an independent database per activation
										// and leave the previously cached one fully usable — keep it, a
										// re-load of the large default dataset would be pure waste.
										// (Clearing alone already forces re-activation for the next class;
										// dockerWasChanged stays untouched so it keeps meaning "a test
										// deliberately re-provisioned the environment".)
										if (dbp instanceof AbstractDockerBasesDatabaseProvider) {
											storedLoaders.clear();
										}
										throw activateOrLoadFailed;
									}
								}

							} catch (Exception e) {

                                LOGGER.error("prepareContexts error", e);
								throw new RuntimeException(e);
							}

							TestContextImpl testContextImpl = new TestContextImpl();
							testContextImpl.setDataSource(dataBaseInfo.getKey());
							testContextImpl.setDialect(dataBaseInfo.getValue());
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
