/*
* Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
*
*/
package org.eclipse.daanse.olap.api.element;

import java.util.List;

import org.eclipse.daanse.olap.api.Context;

public interface Catalog extends MetaElement {

	/**
	 * Uniquely identifier of this catalog.
	 * 
	 * @return the unique identifier of this catalog
	 */
	String getId();

	/**
	 * name of this Catalog.
	 * 
	 * @return the name of this catalog
	 */
	String getName();

	/**
	 * description of this Catalog.
	 * 
	 * @return the description of this catalog
	 */
	String getDescription();

	/**
	 * List of all Schemas in this catalog.
	 * 
	 * @return an array of all schemas in this catalog
	 */
	List<? extends Schema> getSchemas();

	Context getContext();

}
