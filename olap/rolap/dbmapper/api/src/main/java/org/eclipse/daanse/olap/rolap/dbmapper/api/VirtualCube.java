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
package org.eclipse.daanse.olap.rolap.dbmapper.api;

import java.util.List;


public interface VirtualCube {

    List<? extends Annotation> annotations();

    List<? extends CubeUsage> cubeUsages();

    List<? extends VirtualCubeDimension> virtualCubeDimension();

    List<? extends VirtualCubeMeasure> virtualCubeMeasure();

    List<? extends CalculatedMember> calculatedMember();

    List<? extends NamedSet> namedSet();

    boolean enabled();

    String name();

    String defaultMeasure();

    String caption();

    String description();

}