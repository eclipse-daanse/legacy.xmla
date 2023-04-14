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

import java.io.PrintWriter;
import java.util.List;

/**
 * A schema is a collection of cubes and virtual cubes. It can also contain
 * shared dimensions (for use by those cubes), named sets, roles, and
 * declarations of user-defined functions.
 *
 */
public interface Schema {

    List<Annotation> annotations();

    List<Parameter> parameters();

    List<PrivateDimension> dimensions();

    List<Cube> cubes();

    List<VirtualCube> virtualCubes();

    List<NamedSet> namedSets();

    List<Role> roles();

    /**
     * @return @deprecated
     */
    @Deprecated(since="new version")
    List<UserDefinedFunction> userDefinedFunctions();

    String name();

    String description();

    String measuresCaption();

    String defaultRole();

    void display(PrintWriter pw, int i);
}
