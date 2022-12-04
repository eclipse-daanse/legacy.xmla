/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.rolap.dbmapper.record;

import java.util.List;

import org.eclipse.daanse.olap.rolap.dbmapper.api.VirtualCube;

public record VirtualCubeR(String name,
                           String caption,
                           String description,
                           String defaultMeasure,
                           boolean enabled,
                           List<AnnotationR> annotations,
                           List<CubeUsageR> cubeUsages,
                           List<VirtualCubeDimensionR> virtualCubeDimension,
                           List<VirtualCubeMeasureR> virtualCubeMeasure,
                           List<CalculatedMemberR> calculatedMember,
                           List<NamedSetR> namedSet
) implements VirtualCube {

}
