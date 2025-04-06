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

/**
 * Represents the access that a role has to whole particular hierarchy.
 */
public interface AllHierarchyAccess extends HierarchyAccess {

    /**
     * Returns the access to whole hierarchy.
     *
     * @return the access to the hierarchy.
     */
    AccessHierarchy getAccess();
}
