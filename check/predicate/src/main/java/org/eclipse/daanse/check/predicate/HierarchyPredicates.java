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

import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;

public class HierarchyPredicates {

    public static Predicate<Hierarchy> isExist() {
        return h -> h != null;
    }

    public static Predicate<Hierarchy> hasName() {
        return h -> h.getName() != null;
    }

    public static Predicate<Hierarchy> hasCaption() {
        return h -> h.getCaption() != null;
    }

    public static Predicate<Hierarchy> hasDescription() {
        return h -> h.getDescription() != null;
    }

    public static Predicate<Hierarchy> hasLevels() {
        return h -> h.getLevels() != null && !h.getLevels().isEmpty();
    }

    public static Predicate<Hierarchy> hasDimension() {
        return h -> h.getDimension() != null;
    }

    public static Predicate<Hierarchy> hasDisplayFolder() {
        return h -> h.getDisplayFolder() != null;
    }

    public static Predicate<Hierarchy> hasHierarchy() {
        return h -> h.getHierarchy() != null;
    }

    public static Predicate<Hierarchy> hasMetaData() {
        return h -> h.getMetaData() != null;
    }

    public static Predicate<Hierarchy> hasNullMember() {
        return h -> h.getNullMember() != null;
    }

    public static Predicate<Hierarchy> isOrdinalInCube(int order) {
        return h -> h.getOrdinalInCube() == order;
    }

    public static Predicate<Hierarchy> hasQualifiedName() {
        return h -> h.getQualifiedName() != null;
    }

    public static Predicate<Hierarchy> hasRootMembers() {
        return h -> h.getRootMembers() != null && !h.getRootMembers().isEmpty();
    }

    public static Predicate<Hierarchy> hasUniqueName() {
        return h -> h.getUniqueName() != null;
    }

    public static Predicate<Hierarchy> hasLevelWithName(String name) {
        return h -> h.getLevels() != null && h.getLevels().stream().anyMatch(l -> name.equals(l.getName()));
    }

}
