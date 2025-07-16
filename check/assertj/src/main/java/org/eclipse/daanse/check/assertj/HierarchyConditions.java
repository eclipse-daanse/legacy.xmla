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
import org.eclipse.daanse.check.predicate.HierarchyPredicates;
import org.eclipse.daanse.olap.api.element.Hierarchy;

public class HierarchyConditions {

    public static Condition<Hierarchy> isExist = new Condition<>(HierarchyPredicates.isExist(), "is exist");

    public static Condition<Hierarchy> hasCaption = new Condition<>(HierarchyPredicates.hasCaption(), "has caption");

    public static Condition<Hierarchy> hasDescription = new Condition<>(HierarchyPredicates.hasDescription(), "has description");

    public static Condition<Hierarchy> hasDimension = new Condition<>(HierarchyPredicates.hasDimension(), "has dimension");

    public static Condition<Hierarchy> hasDisplayFolder = new Condition<>(HierarchyPredicates.hasDisplayFolder(), "has display folder");
    
    public static Condition<Hierarchy> hasHierarchy = new Condition<>(HierarchyPredicates.hasHierarchy(), "has hierarchy");

    public static Condition<Hierarchy> hasLevels = new Condition<>(HierarchyPredicates.hasLevels(), "has levels");

    public static Condition<Hierarchy> hasMetaData = new Condition<>(HierarchyPredicates.hasMetaData(), "has meta data");

    public static Condition<Hierarchy> hasName = new Condition<>(HierarchyPredicates.hasName(), "has name");

    public static Condition<Hierarchy> hasNullMember = new Condition<>(HierarchyPredicates.hasNullMember(), "has null member");

    public static Condition<Hierarchy> hasQualifiedName = new Condition<>(HierarchyPredicates.hasQualifiedName(), "has qualified name");

    public static Condition<Hierarchy> hasUniqueName = new Condition<>(HierarchyPredicates.hasUniqueName(), "has unique name");

    public static Condition<Hierarchy> hasRootMembers = new Condition<>(HierarchyPredicates.hasRootMembers(), "has root members");

}
