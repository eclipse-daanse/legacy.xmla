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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import mondrian.rolap.RolapConnectionProperties;
import org.glassfish.jaxb.runtime.v2.JAXBContextFactory;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;
import org.opencube.junit5.context.BaseTestContext;
import org.opencube.junit5.context.Context;
import org.opencube.junit5.dataloader.DataLoader;
import org.opencube.junit5.dbprovider.DatabaseProvider;
import org.opencube.junit5.xmltests.ResourceTestCase;
import org.opencube.junit5.xmltests.XmlResourceRoot;
import org.opencube.junit5.xmltests.XmlResourceTestCase;

import aQute.bnd.annotation.Cardinality;
import aQute.bnd.annotation.spi.ServiceConsumer;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import mondrian.olap.Util.PropertyList;

@ServiceConsumer(cardinality = Cardinality.MULTIPLE, value = DatabaseProvider.class)
public class ContextArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<ContextSource> {
	private ContextSource contextSource;

	private static Map<Class<? extends DatabaseProvider>, Map<Class<? extends DataLoader>, Entry<PropertyList, DataSource>>> store = new HashMap<>();
	public static boolean dockerWasChanged = true;

	@Override
	public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {

		// TODO: parallel
		List<Context> contexts = prepareContexts(extensionContext);
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
								e.printStackTrace();
							}

						}
					}
				}
			}
		}

		return null;
	}

	private List<Context> prepareContexts(ExtensionContext extensionContext) {
		Stream<DatabaseProvider> providers;
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader()); //for withSchemaProcessor(context, MyFoodmart.class);
		Class<? extends DatabaseProvider>[] dbHandlerClasses = contextSource.database();
		if (dbHandlerClasses == null || dbHandlerClasses.length == 0) {
			providers = ServiceLoader.load(DatabaseProvider.class, this.getClass().getClassLoader()).stream()
					.map(Provider::get);
		} else {
			providers = Stream.of(dbHandlerClasses).map(c -> {
				try {
					return c.getConstructor().newInstance();
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			});
		}
		List<Context> args = providers.parallel().map(dbp -> {

			Entry<PropertyList, DataSource> dataSource = null;
			Class<? extends DatabaseProvider> clazzProvider = dbp.getClass();

			if (!store.containsKey(clazzProvider)) {
				store.put(clazzProvider, new HashMap<>());
			} else {

			}

			List<Context> aaa = new ArrayList<>();

			Optional<AnnotatedElement> oElement = extensionContext.getElement();
			if (oElement.isPresent()) {
				if (oElement.get() instanceof Method) {
					Method method = (Method) oElement.get();
					for (Parameter param : method.getParameters()) {
						if (Context.class.isAssignableFrom(param.getType())) {
							ContextSource contextSource=method.getAnnotation(ContextSource.class);

							try {

								Class<? extends DataLoader> dataLoaderClass = contextSource.dataloader();

								Map<Class<? extends DataLoader>, Entry<PropertyList, DataSource>> storedLoaders = store
										.get(clazzProvider);
								if (storedLoaders.containsKey(dataLoaderClass) && !dockerWasChanged) {
									dataSource = storedLoaders.get(dataLoaderClass);
									dataSource.getKey().put(RolapConnectionProperties.Jdbc.name(), dbp.getJdbcUrl());
								} else {
									dataSource = dbp.activate();
									DataLoader dataLoader = dataLoaderClass.getConstructor().newInstance();
									dataLoader.loadData(dataSource.getValue());
									storedLoaders.clear();
									storedLoaders.put(dataLoaderClass, dataSource);
									dockerWasChanged = false;
								}

							} catch (Exception e) {

								e.printStackTrace();
								throw new RuntimeException(e);
							}

							BaseTestContext context=new BaseTestContext();
							context.init(dataSource);
							
							Stream.of(contextSource.propertyUpdater()).map(c -> {
								try {
									return c.getConstructor().newInstance();
								} catch (Exception e) {
									e.printStackTrace();
									throw new RuntimeException(e);
								}
							}).forEachOrdered(u->context.update(u));

							aaa.add(context);
						}
					}
				}
			}
			return aaa;
		}).flatMap(Collection::stream).collect(Collectors.toList());
		return args;
	}

	@Override
	public void accept(ContextSource annotation) {
		this.contextSource = annotation;

	}

	class NamedArguments implements Arguments {

		public NamedArguments(Context context, ResourceTestCase res) {
			super();
			this.context = context;
			this.res = res;
		}

		Context context;
		ResourceTestCase res;

		@Override
		public Object[] get() {

			if (context == null) {
				if (res != null) {
					return new Object[] { context, res };
				}
			}

			if (res == null) {
				if (context != null) {
					return new Object[] { context };
				}
			}
			return new Object[] { context, res };
		}

	}

}