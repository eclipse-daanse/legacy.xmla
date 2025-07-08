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

import java.util.List;

import org.eclipse.daanse.check.api.dimension.DimensionSelector;


public record DimensionSelectorR(String cubeName, String name, List checkList) implements DimensionSelector{

    public DimensionSelectorR(String cubeName, String name) {
        this(cubeName, name, List.of(
                new DescriptionCheckR(),
                new NameCheckR()
                //...
                ));
    }

}
