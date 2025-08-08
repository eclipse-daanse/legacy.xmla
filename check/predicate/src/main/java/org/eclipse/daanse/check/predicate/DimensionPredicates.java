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

import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;

public class DimensionPredicates {

    public static Predicate<Dimension> isExist() {
        return d -> d != null;
    }
    
    public static Predicate<Dimension> hasName() {
        return d -> d.getName() != null;
    }

    public static Predicate<Dimension> hasDescription() {
        return d -> d.getDescription() != null;
    }

    public static Predicate<Dimension> hasHierarchies() {
        return d -> d.getHierarchies() != null && !d.getHierarchies().isEmpty();
    }

    public static Predicate<Dimension> hasHierarchy() {
        return d -> d.getHierarchy() != null;
    }

    public static Predicate<Dimension> hasDimension() {
        return d -> d.getDimension() != null;
    }

    public static Predicate<Dimension> hasDimensionType() {
        return d -> d.getDimensionType() != null;
    }

    public static Predicate<Dimension> hasUniqueName() {
        return d -> d.getUniqueName() != null;
    }

    public static Predicate<Dimension> hasQualifiedName() {
        return d -> d.getQualifiedName() != null;
    }

    public static Predicate<Dimension> hasMetaData() {
        return d -> d.getMetaData() != null;
    }

    public static Predicate<Dimension> hasCatalog() {
        return d -> d.getCatalog() != null;
    }

    public static Predicate<Dimension> hasCube() {
        return d -> d.getCube() != null;
    }

    public static Predicate<Dimension> isVisible() {
        return d -> d.isVisible();
    }

    public static Predicate<Dimension> isMeasures() {
        return d -> d.isMeasures();
    }

    public static Function<Dimension, Optional<? extends Hierarchy>> selectHierarchyByName(String name) {
        return d -> d.getHierarchies() != null ? d.getHierarchies().stream().filter(h -> name.equals(h.getName())).findFirst() : Optional.empty();
    }

    public static Function<Dimension, Optional<? extends Hierarchy>> selectHierarchyByUniqueName(String name) {
        return d -> d.getHierarchies() != null ? d.getHierarchies().stream().filter(h -> name.equals(h.getUniqueName())).findFirst() : Optional.empty();
    }

    public static Predicate<Dimension> hasHierarchyWithName(String name) {
        return d -> d.getHierarchies() != null && d.getHierarchies().stream().anyMatch(h -> name.equals(h.getName()));
    }

}
