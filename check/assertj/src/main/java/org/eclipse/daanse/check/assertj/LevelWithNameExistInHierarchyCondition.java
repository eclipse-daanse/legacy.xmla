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


public class LevelWithNameExistInHierarchyCondition  extends Condition<Hierarchy> {

    public LevelWithNameExistInHierarchyCondition(String name) {
        super(HierarchyPredicates.hasLevelWithName(name), "Hierarchy %s exist", name);
    }
}
