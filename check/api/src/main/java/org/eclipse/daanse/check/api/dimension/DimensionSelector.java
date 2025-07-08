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
package org.eclipse.daanse.check.api.dimension;

import org.eclipse.daanse.check.api.Check;
import org.eclipse.daanse.check.api.Selector;
import org.eclipse.daanse.olap.api.element.Dimension;

public interface DimensionSelector extends Selector<Check<Dimension>>{
    //it's enough to take cube from context
    String cubeName();
    String name();
}
