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
package org.eclipse.daanse.olap.rolap.dbmapper.model.api;

import java.util.List;


public interface VirtualCube {

    List<Annotation> annotations();

    List<CubeUsage> cubeUsages();

    List<VirtualCubeDimension> virtualCubeDimension();

    List<VirtualCubeMeasure> virtualCubeMeasure();

    List<CalculatedMember> calculatedMember();

    List<NamedSet> namedSet();

    boolean enabled();

    String name();

    String defaultMeasure();

    String caption();

    String description();

    boolean visible();
}
