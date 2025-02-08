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

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.ContextGroup;
import org.eclipse.daanse.olap.api.element.Catalog;

public class ContextsSupplyerImpl implements ContextListSupplyer {

	private final ContextGroup contextsGroup;

	// Accepts Null as Empty List
	public ContextsSupplyerImpl(ContextGroup contextsGroup) {

		this.contextsGroup = contextsGroup;
	}

	@Override
	public List<Catalog> get(List<String> roles) {
		return getContexts().stream().map(context -> context.getConnection(roles)).map(Connection::getCatalog).toList();
	}

	@Override
	public Optional<Catalog> tryGetFirstByName(String catalogName, List<String> roles) {
		return getContext(catalogName).map(co -> co.getConnection(roles).getCatalog());
	}

	@Override
	public List<Context> getContexts() {
		return contextsGroup.getValidContexts();
	}

	@Override
	public Optional<Context> getContext(String name) {
		return getContexts().stream().filter(c -> c.getName().equals(name)).findFirst();

	}

}
