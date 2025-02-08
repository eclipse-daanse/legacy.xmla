/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2002-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * jhyde, Oct 5, 2002
 *
 * Contributors:
 *   SmartCity Jena - refactor, clean API
 */
package org.eclipse.daanse.olap.api.access;

import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.element.Catalog;

/**
 * A <code>Role</code> is a collection of access rights to cubes, permissions,
 * and so forth.
 *
 * <p>At present, the only way to create a role is programmatically. You then
 * add appropriate permissions, and associate the role with a connection.
 * Queries executed for the duration of the connection will be using the role
 * for security control.
 *
 * <p>Mondrian does not have any notion of a 'user'. It is the client
 * application's responsibility to create a role appropriate for the user who
 * is establishing the connection.
 *
 * @author jhyde
 * @since Oct 5, 2002
 */
public interface Role {

    /**
     * Returns the access this role has to a given schema.
     *
     * @pre schema != null
     * @post return == Access.ALL
     * || return == Access.NONE
     * || return == Access.ALL_DIMENSIONS
     */
    Access getAccess(Catalog schema);

    /**
     * Returns the access this role has to a given cube.
     *
     * @pre cube != null
     * @post return == Access.ALL || return == Access.NONE
     */
    Access getAccess(Cube cube);


    /**
     * Returns the access this role has to a given dimension.
     *
     * @pre dimension != null
     * @post Access.instance().isValid(return)
     */
    Access getAccess(Dimension dimension);

    /**
     * Returns the access this role has to a given hierarchy.
     *
     * @pre hierarchy != null
     * @post return == Access.NONE
     *   || return == Access.ALL
     *   || return == Access.CUSTOM
     */
    Access getAccess(Hierarchy hierarchy);

    /**
     * Returns the details of this hierarchy's access, or null if the hierarchy
     * has not been given explicit access.
     *
     * @pre hierarchy != null
     */
    HierarchyAccess getAccessDetails(Hierarchy hierarchy);

    /**
     * Returns the access this role has to a given level.
     *
     * @pre level != null
     * @post Access.instance().isValid(return)
     */
    Access getAccess(Level level);

    /**
     * Returns the access this role has to a given member.
     *
     * @pre member != null
     * @pre isMutable()
     * @post return == Access.NONE
     *    || return == Access.ALL
     *    || return == Access.CUSTOM
     */
    Access getAccess(Member member);

    /**
     * Returns the access this role has to a given named set.
     *
     * @pre set != null
     * @pre isMutable()
     * @post return == Access.NONE || return == Access.ALL
     */
    Access getAccess(NamedSet set);

    /**
     * Returns whether this role is allowed to see a given element.
     * @pre olapElement != null
     */
    boolean canAccess(OlapElement olapElement);


}

