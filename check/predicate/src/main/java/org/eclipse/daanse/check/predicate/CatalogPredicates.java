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

import org.eclipse.daanse.olap.api.element.Catalog;

public class CatalogPredicates {

    public static Predicate<Catalog> isExist() {
        return c -> c != null;
    }

    public static Predicate<Catalog> hasCubes() {
        return c -> c.getCubes() != null && c.getCubes().size() > 0;
    }

    public static Predicate<Catalog> hasDatabaseSchemas() {
        return c -> c.getDatabaseSchemas() != null && c.getDatabaseSchemas().size() > 0;
    }

    public static Predicate<Catalog> hasDescription() {
        return c -> c.getDescription() != null;
    }

    public static Predicate<Catalog> hasId() {
        return c -> c.getId() != null;
    }

    public static Predicate<Catalog> hasMetaData() {
        return c -> c.getMetaData() != null;
    }

    public static Predicate<Catalog> hasName() {
        return c -> c.getName() != null;
    }

    public static Predicate<Catalog> hasParameters() {
        return c -> c.getParameters() != null && c.getParameters().length > 0;
    }

    public static Predicate<Catalog> hasWarnings() {
        return c -> c.getWarnings() != null && c.getWarnings().size() > 0;
    }

    public static Predicate<Catalog> hasCubeWithName(String name) {
        return c -> c != null && c.getCubes() != null && c.getCubes().stream().anyMatch(cube -> name.equals(cube.getName()));
    }

}
