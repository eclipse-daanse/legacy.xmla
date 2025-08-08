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
import org.eclipse.daanse.check.predicate.DimensionPredicates;
import org.eclipse.daanse.olap.api.element.Dimension;

public class DimensionConditions {
    public static Condition<Dimension> isExist = new Condition<>(DimensionPredicates.isExist(), "has dimension");
    
    public static Condition<Dimension> hasName = new Condition<>(DimensionPredicates.hasName(), "has name");

    public static Condition<Dimension> hasDescription = new Condition<>(DimensionPredicates.hasDescription(), "has description");

    public static Condition<Dimension> hasCatalog = new Condition<>(DimensionPredicates.hasCatalog(), "has catalog");

    public static Condition<Dimension> hasHierarchies = new Condition<>(DimensionPredicates.hasHierarchies(), "has hierarchies");

    public static Condition<Dimension> hasDimension = new Condition<>(DimensionPredicates.hasDimension(), "has dimension");

    public static Condition<Dimension> hasCube = new Condition<>(DimensionPredicates.hasCube(), "has cube");

    public static Condition<Dimension> hasDimensionType = new Condition<>(DimensionPredicates.hasDimensionType(), "has dimensionType");

    public static Condition<Dimension> hasHierarchy = new Condition<>(DimensionPredicates.hasHierarchy(), "has hierarchy");

    public static Condition<Dimension> hasMetaData = new Condition<>(DimensionPredicates.hasMetaData(), "has meta data");

    public static Condition<Dimension> hasQualifiedName = new Condition<>(DimensionPredicates.hasQualifiedName(), "has qualified name");

    public static Condition<Dimension> hasUniqueName = new Condition<>(DimensionPredicates.hasUniqueName(), "has unique name");
}
