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
package org.eclipse.daanse.olap.xmla.bridge;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.ContextGroup;

public class ContextsSupplyerImpl implements ContextListSupplyer {

	private final ContextGroup contextsGroup;

	// Accepts Null as Empty List
	public ContextsSupplyerImpl(ContextGroup contextsGroup) {

		this.contextsGroup = contextsGroup;
	}

	@Override
	public List<Context> get() {
		List<Context> list=	contextsGroup.getValidContexts();
		System.err.println(list);
		return list;
	}

	@Override
	public List<Context> get(Predicate<Context> predicate) {
		return get().stream().filter(predicate).toList();
	}

	@Override
	public Optional<Context> tryGetFirstByName(String catalogName) {
		return get().stream().filter(c -> c.getCatalogMapping().getName().equals(catalogName)).findFirst();
	}

	@Override
	public Optional<Context> tryGetFirst(Predicate<Context> predicate) {
		return get().stream().filter(predicate).findFirst();
	}

	// all Filter Work

}
