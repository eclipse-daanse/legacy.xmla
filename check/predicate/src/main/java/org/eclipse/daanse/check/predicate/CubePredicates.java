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

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;

public final class CubePredicates {

    public static Predicate<Cube> isExist() {
        return c -> c != null;
    }

    public static Predicate<Cube> hasName() {
        return c -> c.getName() != null;
    }

    public static Predicate<Cube> hasCaption() {
        return c -> c.getCaption() != null;
    }

    public static Predicate<Cube> hasCatalog() {
        return c -> c.getCatalog() != null;
    }

    public static Predicate<Cube> hasDescription() {
        return c -> c.getDescription() != null;
    }

    public static Predicate<Cube> hasDimensions() {
        return c -> c.getDimensions() != null && !c.getDimensions().isEmpty();
    }

    public static Function<Cube, Optional<? extends Dimension>> selectDimensionsByName(String name) {
        return c -> c.getDimensions() != null ? c.getDimensions().stream().filter(d -> name.equals(d.getName())).findFirst() : Optional.empty();
    }

    public static Function<Cube, Optional<? extends Dimension>> selectDimensionsByUniqueName(String uniqueName) {
        return c -> c.getDimensions() != null ? c.getDimensions().stream().filter(d -> uniqueName.equals(d.getUniqueName())).findFirst() : Optional.empty();
    }

    public static Predicate<Cube> hasHierarchies() {
        return c -> c.getHierarchies() != null && !c.getHierarchies().isEmpty();
    }

    public static Predicate<Cube> hasDimensionWithName(String name) {
        return c -> c.getDimensions() != null && c.getDimensions().stream().anyMatch(d -> name.equals(d.getName()));
    }

    public static Predicate<Cube> hasDimensionsCount(Integer count) {
        return c -> c.getDimensions() != null && c.getDimensions().size() == count;
    }

}
