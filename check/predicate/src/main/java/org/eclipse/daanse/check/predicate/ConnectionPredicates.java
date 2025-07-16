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
package org.eclipse.daanse.check.predicate;

import java.util.function.Predicate;

import org.eclipse.daanse.olap.api.Connection;

public class ConnectionPredicates {

    public static Predicate<Connection> isExist() {
        return c -> c != null;
    }

    public static Predicate<Connection> hasCatalog() {
        return c -> c.getCatalog() != null;
    }

    public static Predicate<Connection> hasCatalogReader() {
        return c -> c.getCatalogReader() != null;
    }

    public static Predicate<Connection> hasContext() {
        return c -> c.getContext() != null;
    }

    public static Predicate<Connection> hasDataSource() {
        return c -> c.getDataSource() != null;
    }

    public static Predicate<Connection> hasRole() {
        return c -> c.getRole() != null;
    }

    //...
}
