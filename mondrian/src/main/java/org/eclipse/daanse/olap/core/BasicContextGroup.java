/*
* Copyright (c) 2022 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   SmartCity Jena - initial
*   Stefan Bischof (bipolis.org) - initial
*/
package org.eclipse.daanse.olap.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.ContextGroup;
import org.osgi.namespace.unresolvable.UnresolvableNamespace;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.metatype.annotations.Designate;

@Designate(ocd = BasicContextGroupConfig.class, factory = true)
@Component(service = ContextGroup.class, scope = ServiceScope.SINGLETON)
public class BasicContextGroup implements ContextGroup {

	private static Logger LOGGER = LoggerFactory.getLogger(BasicContextGroup.class);
	public static final String PID = "org.eclipse.daanse.olap.core.BasicContextGroup";
	public static final String REF_NAME_CONTEXTS = "context";


	private List<Context<?>> contexts = new CopyOnWriteArrayList<Context<?>>();
	private List<Context<?>> contextsValid = List.of();
	private List<Context<?>> contextsFailing = List.of();

	private BasicContextGroupConfig config;

	@Activate
	public void activate(BasicContextGroupConfig config,Map<String, Object> coniguration) throws Exception {
		this.config = config;
	}

//	
	@Reference(name = REF_NAME_CONTEXTS,cardinality = ReferenceCardinality.MULTIPLE, target = UnresolvableNamespace.UNRESOLVABLE_FILTER, policy = ReferencePolicy.DYNAMIC)
	public void bindContext(Context context) {
		contexts.add(context);
		createUniqueCheckedLists();

	}

	public void unbindContext(Context context) {
		contexts.remove(context);
		createUniqueCheckedLists();

	}

	private void createUniqueCheckedLists() {
		// TODO: MUST: Unique check!
		// TODO: NICE: Quality check! (description) NONE;INFO;WARN;IGNORE,UNIQUE
		// TODO Auto-generated method stub

		Set<String> duplicats = contexts.stream()
				// Group the elements along
				// with their frequency in a map
				.collect(Collectors.groupingBy(Context::getName, Collectors.counting()))
				// Convert this map into a stream
				.entrySet().stream()
				// Check if frequency > 1
				// for duplicate elements
				.filter(m -> m.getValue() > 1)
				// Find such elements
				.map(Map.Entry::getKey)
				// And Collect them in a Set
				.collect(Collectors.toSet());

		List<Context<?>> tmpValid = new ArrayList<>();
		List<Context<?>> tmpFailing = new ArrayList<>();
		contexts.stream().forEach(c -> {
			if (duplicats.contains(c.getName())) {
				tmpFailing.add(c);
			} else {
				tmpValid.add(c);
			}
		});
		contextsValid = List.copyOf(tmpValid);
		contextsFailing = tmpFailing;
	}

	public List<Context<?>> getValidContexts() {
		return contextsValid;
	}

}
