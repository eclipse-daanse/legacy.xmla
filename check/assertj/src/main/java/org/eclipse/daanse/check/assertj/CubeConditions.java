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
import org.eclipse.daanse.check.predicate.CubePredicates;
import org.eclipse.daanse.olap.api.element.Cube;

public class CubeConditions {

    public static Condition<Cube> isExist = new Condition<>(CubePredicates.isExist(), "has cube");
    
    public static Condition<Cube> hasName = new Condition<>(CubePredicates.hasName(), "has name");

    public static Condition<Cube> hasDescription = new Condition<>(CubePredicates.hasDescription(), "has description");

    public static Condition<Cube> hasCaption = new Condition<>(CubePredicates.hasCaption(), "has caption");

    public static Condition<Cube> hasCatalog = new Condition<>(CubePredicates.hasCatalog(), "has catalog");

    public static Condition<Cube> hasHierarchies = new Condition<>(CubePredicates.hasHierarchies(), "has hierarchies");

    public static Condition<Cube> hasDimensions = new Condition<>(CubePredicates.hasDimensions(), "has dimensions");

}
