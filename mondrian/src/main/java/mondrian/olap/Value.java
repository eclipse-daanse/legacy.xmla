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
package mondrian.olap;

/**
 * A <code>Value</code> represents a member of an enumerated type. If an
 * enumerated type is not based upon an explicit array of values, an
 * array of {@link BasicValue}s will implicitly be created.
 */
public interface Value {
    String getName();

    int getOrdinal();

    String getDescription();
}
