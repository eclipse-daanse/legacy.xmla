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
package org.eclipse.daanse.check.impl.dimension;

import org.assertj.core.api.Condition;
import org.eclipse.daanse.check.api.dimension.DescriptionCheck;
import org.eclipse.daanse.check.assertj.DimensionConditions;
import org.eclipse.daanse.olap.api.element.Dimension;

public record DescriptionCheckR() implements DescriptionCheck{

    @Override
    public Condition<Dimension> condition() {
        return DimensionConditions.hasDescription;
    }

}
