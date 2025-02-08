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
*/

package org.eclipse.daanse.olap.api.element;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.access.Access;
import org.eclipse.daanse.olap.api.access.Role;

/**
 * A <code>Catalog</code> is a collection of cubes, shared dimensions, and roles.
 *
 * @author jhyde
 */
public interface Catalog extends MetaElement {

     static final Set<Access> schemaAllowed =
        EnumSet.of(
            Access.NONE,
            Access.ALL,
            Access.ALL_DIMENSIONS,
            Access.CUSTOM);

     static final Set<Access> cubeAllowed =
        EnumSet.of(Access.NONE, Access.ALL, Access.CUSTOM);

     static final Set<Access> dimensionAllowed =
        EnumSet.of(Access.NONE, Access.ALL, Access.CUSTOM);

     static final Set<Access> hierarchyAllowed =
        EnumSet.of(Access.NONE, Access.ALL, Access.CUSTOM);

     static final Set<Access> memberAllowed =
        EnumSet.of(Access.NONE, Access.ALL);
    
    /**
     * Returns the name of this schema.
     * @post return != null
     * @post return.length() > 0
     */
     String getName();

     
     String getDescription();

    /**
     * Returns the uniquely generated id of this schema.
     */
    String getId();


    /**
     * Returns a list of all cubes in this schema.
     */
    Cube[] getCubes();

    /**
     * Returns a list of shared dimensions in this schema.
     */
    Hierarchy[] getSharedHierarchies();

    /**
     * Creates a {@link CatalogReader} without any access control.
     */
    CatalogReader getCatalogReader();

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
    @Deprecated
    Date getCatalogLoadDate();

    /**
     * Returns a list of warnings and errors that occurred while loading this
     * schema.
     *
     * @return list of warnings
     */
    List<Exception> getWarnings();

    
    @Deprecated
    Cube lookupCube(String cubeName, boolean failIfNotFound);
    
    @Deprecated
    Cube lookupCube(String cubeName);

	Role getDefaultRole();

	NamedSet getNamedSet(String name);


	List<String> getAccessRoles();
	
	List<? extends DbSchema> getDbSchemas();
}
