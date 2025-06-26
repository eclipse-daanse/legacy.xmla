/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.api;

import java.util.List;

public non-sealed interface KeyIdentifierSegment extends IdentifierSegment {

    /**
     * Returns the key components, if this IdentifierSegment is a key. (That is, if
     * {@link #getQuoting()} returns {@link Quoting#KEY}.)
     *
     * Returns null otherwise.
     *
     * @return Components of key, or null if this IdentifierSegment is not a key
     */
    List<NameIdentifierSegment> getKeyParts();

}
