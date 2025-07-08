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
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.check.assertj;

import org.assertj.core.api.Condition;
import org.eclipse.daanse.check.predicate.ConnectionPredicates;
import org.eclipse.daanse.olap.api.Connection;

public class ConnectionConditions {

    public static Condition<Connection> isExist = new Condition<>(ConnectionPredicates.isExist(), "is exist");

    public static Condition<Connection> hasCatalog = new Condition<>(ConnectionPredicates.hasCatalog(), "has catalog");

    public static Condition<Connection> hasCatalogReader = new Condition<>(ConnectionPredicates.hasCatalogReader(), "has catalog reader");

    public static Condition<Connection> hasContext = new Condition<>(ConnectionPredicates.hasContext(), "has context");

    public static Condition<Connection> hasDataSource = new Condition<>(ConnectionPredicates.hasDataSource(), "has data source");

    public static Condition<Connection> hasRole = new Condition<>(ConnectionPredicates.hasRole(), "has role");
}
