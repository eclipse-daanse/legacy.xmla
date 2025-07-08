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

import org.eclipse.daanse.olap.api.element.Level;

public class LevelPredicates {

    public static Predicate<Level> isExist() {
        return l -> l != null;
    }

    public static Predicate<Level> hasName() {
        return l -> l.getName() != null;
    }

    public static Predicate<Level> hasCaption() {
        return l -> l.getCaption() != null;
    }

    public static Predicate<Level> hasChaildLevel() {
        return l -> l.getChildLevel() != null;
    }

    public static Predicate<Level> hasDescription() {
        return l -> l.getDescription() != null;
    }

    public static Predicate<Level> hasDimension() {
        return l -> l.getDimension() != null;
    }

    public static Predicate<Level> hasHierarchy() {
        return l -> l.getHierarchy() != null;
    }

    public static Predicate<Level> hasInheritedProperties() {
        return l -> l.getInheritedProperties() != null && l.getInheritedProperties().length > 0;
    }

    public static Predicate<Level> hasLevelType() {
        return l -> l.getLevelType() != null;
    }

    public static Predicate<Level> hasMemberFormatter() {
        return l -> l.getMemberFormatter() != null;
    }

    public static Predicate<Level> hasMembers() {
        return l -> l.getMembers() != null && l.getMembers().size() > 0;
    }

    public static Predicate<Level> hasMetaData() {
        return l -> l.getMetaData() != null;
    }

    public static Predicate<Level> hasOrdinalExp() {
        return l -> l.getOrdinalExp() != null;
    }

    public static Predicate<Level> hasParentAsLeafNameFormat() {
        return l -> l.getParentAsLeafNameFormat() != null;
    }

    public static Predicate<Level> hasProperties() {
        return l -> l.getProperties() != null && l.getProperties().length > 0;
    }

    public static Predicate<Level> hasQualifiedName() {
        return l -> l.getQualifiedName() != null;
    }

    public static Predicate<Level> hasUniqueName() {
        return l -> l.getUniqueName() != null;
    }
}
