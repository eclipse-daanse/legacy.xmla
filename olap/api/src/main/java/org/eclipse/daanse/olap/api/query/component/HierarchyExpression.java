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
package org.eclipse.daanse.olap.api.query.component;

import org.eclipse.daanse.olap.api.element.Hierarchy;

public non-sealed interface HierarchyExpression extends QueryComponent {

	/**
	 * Returns the hierarchy.
	 */
	Hierarchy getHierarchy();

}
