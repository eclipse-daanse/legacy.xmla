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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.olap.api.Context;
import org.glassfish.jaxb.runtime.v2.JAXBContextFactory;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.DataLoader;
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
import mondrian.rolap.aggregator.AggregationFactoryImpl;

@ServiceConsumer(cardinality = Cardinality.MULTIPLE, value = DatabaseProvider.class)
public class ContextArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<ContextSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContextArgumentsProvider.class);
	private ContextSource contextSource;

	private static Map<Class<? extends DatabaseProvider>, Map<Class<? extends DataLoader>,  Entry<DataSource, Dialect>>> store = new HashMap<>();
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
			for (Context context : contexts) {

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
			providers = ServiceLoader.load(DatabaseProvider.class, this.getClass().getClassLoader()).stream()
					.map(Provider::get).toList();
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
									dataBaseInfo = dbp.activate();
									DataLoader dataLoader = dataLoaderClass.getConstructor().newInstance();
									dataLoader.loadData(dataBaseInfo);
									storedLoaders.clear();
									storedLoaders.put(dataLoaderClass, dataBaseInfo);
									dockerWasChanged = false;
								}

							} catch (Exception e) {

                                LOGGER.error("prepareContexts error", e);
								throw new RuntimeException(e);
							}

							TestContextImpl testContextImpl = new TestContextImpl();
							testContextImpl.setDataSource(dataBaseInfo.getKey());
							testContextImpl.setDialect(dataBaseInfo.getValue());
							testContextImpl.setAggragationFactory(new AggregationFactoryImpl(dataBaseInfo.getValue()));
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
