 /*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 * 
 * Contributors:
 *  SmartCity Jena - refactor, clean API
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */

package org.eclipse.daanse.olap.api.element;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.IdentifierSegment;
import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.access.Role;

/**
 * A <code>Catalog</code> is a collection of cubes, shared dimensions, and
 * roles.
 *
 * @author jhyde
 */
public interface Catalog extends MetaElement {

    /**
     * Returns the name of this catalog.
     * 
     * @post return != null
     * @post return.length() > 0
     */
    String getName();

    /**
     * Returns the description of this catalog.
     * 
     * @return
     */
    String getDescription();

    /**
     * Returns the uniquely generated id of this catalog.
     */
    String getId();

    /**
     * Returns a list of all cubes in this catalog.
     */
    List<Cube> getCubes();

    /**
     * Creates a {@link CatalogReader} without any access control.
     */
    CatalogReader getCatalogReaderWithDefaultRole();

    /**
     * Finds a role with a given name in the current catalog, or returns
     * <code>null</code> if no such role exists.
     */
    Role lookupRole(String role);

    /**
     * Returns this schema's parameters.
     */
    Parameter[] getParameters();

    /**
     * Returns when this schema was last loaded.
     *
     * @return Date and time when this schema was last loaded
     */
    Instant getCatalogLoadDate();

    /**
     * Returns a list of warnings and errors that occurred while loading this
     * schema.
     *
     * @return list of warnings
     */
    List<Exception> getWarnings();

    /**
     * looks up the cubes of this catalog for a cube with the given name.
     * @param cubeName
     * @return Optional of Cube
     */
    Optional<? extends Cube> lookupCube(String cubeName);

    Role getDefaultRole();

    NamedSet getNamedSet(String name);

    List<? extends DatabaseSchema> getDatabaseSchemas();

    /**
     * Connection for purposes of parsing and validation. Careful! It won't have the
     * correct locale or access-control profile.
     */
    Connection getInternalConnection();

    NamedSet getNamedSet(IdentifierSegment segment);
}
