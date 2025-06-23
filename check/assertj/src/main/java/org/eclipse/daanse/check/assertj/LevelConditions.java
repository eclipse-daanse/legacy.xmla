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
import org.eclipse.daanse.check.predicate.LevelPredicates;
import org.eclipse.daanse.olap.api.element.Level;

public class LevelConditions {

    public static Condition<Level> isExist = new Condition<>(LevelPredicates.isExist(), "is exist");

    public static Condition<Level> hasName = new Condition<>(LevelPredicates.hasName(), "has name");

    public static Condition<Level> hasCaption = new Condition<>(LevelPredicates.hasCaption(), "has caption");

    public static Condition<Level> hasChaildLevel = new Condition<>(LevelPredicates.hasChaildLevel(), "has chaild level");

    public static Condition<Level> hasDescription = new Condition<>(LevelPredicates.hasDescription(), "has description");

    public static Condition<Level> hasDimension = new Condition<>(LevelPredicates.hasDimension(), "has dimension");

    public static Condition<Level> hasHierarchy = new Condition<>(LevelPredicates.hasHierarchy(), "has hierarchy");

    public static Condition<Level> hasInheritedProperties = new Condition<>(LevelPredicates.hasInheritedProperties(), "has inherited properties");

    public static Condition<Level> hasLevelType = new Condition<>(LevelPredicates.hasLevelType(), "has level type");

    public static Condition<Level> hasMemberFormatter = new Condition<>(LevelPredicates.hasMemberFormatter(), "has member formatter");

    public static Condition<Level> hasMembers = new Condition<>(LevelPredicates.hasMembers(), "has members");

    public static Condition<Level> hasMetaData = new Condition<>(LevelPredicates.hasMetaData(), "has meta data");

    public static Condition<Level> hasOrdinalExp = new Condition<>(LevelPredicates.hasOrdinalExp(), "has ordinal exp");

    public static Condition<Level> hasParentAsLeafNameFormat = new Condition<>(LevelPredicates.hasParentAsLeafNameFormat(), "has parent as leaf name format");

    public static Condition<Level> hasProperties = new Condition<>(LevelPredicates.hasProperties(), "has properties");

    public static Condition<Level> hasQualifiedName = new Condition<>(LevelPredicates.hasQualifiedName(), "has qualified name");

    public static Condition<Level> hasUniqueName = new Condition<>(LevelPredicates.hasUniqueName(), "has unique name");
    //...
}
