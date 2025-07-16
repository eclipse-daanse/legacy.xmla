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
import org.eclipse.daanse.check.predicate.CatalogPredicates;
import org.eclipse.daanse.olap.api.element.Catalog;

public class CatalogConditions {

    public static Condition<Catalog> isExist = new Condition<>(CatalogPredicates.isExist(), "is exist");
    
    public static Condition<Catalog> hasCubes = new Condition<>(CatalogPredicates.hasCubes(), "has cubes");

    public static Condition<Catalog> hasDatabaseSchemas = new Condition<>(CatalogPredicates.hasDatabaseSchemas(), "has database schemas");

    public static Condition<Catalog> hasDescription = new Condition<>(CatalogPredicates.hasDescription(), "has description");

    public static Condition<Catalog> hasId = new Condition<>(CatalogPredicates.hasId(), "has id");

    public static Condition<Catalog> hasMetaData = new Condition<>(CatalogPredicates.hasMetaData(), "has meta data");

    public static Condition<Catalog> hasName = new Condition<>(CatalogPredicates.hasName(), "has name");
    
    public static Condition<Catalog> hasParameters = new Condition<>(CatalogPredicates.hasParameters(), "has parameters");

    public static Condition<Catalog> hasWarnings = new Condition<>(CatalogPredicates.hasWarnings(), "has warnings");
}
