/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 1999-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
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
 */
package org.eclipse.daanse.olap.api.element;

import java.util.List;

import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.SqlExpression;
import org.eclipse.daanse.olap.api.formatter.MemberFormatter;

/**
 * A <code>Level</code> is a group of {@link Member}s in a {@link Hierarchy},
 * all with the same attributes and at the same depth in the hierarchy.
 *
 * @author jhyde, 1 March, 1999
 */
public interface Level extends OlapElement, MetaElement {

    /**
     * Returns the depth of this level.
     *
     * <p>
     * Note #1: In an access-controlled context, the first visible level of a
     * hierarchy (as returned by {@link CatalogReader#getHierarchyLevels}) may not
     * have a depth of 0.
     * </p>
     *
     * <p>
     * Note #2: In a parent-child hierarchy, the depth of a member (as returned by
     * {@link CatalogReader#getMemberDepth}) may not be the same as the depth of its
     * level.
     */
    int getDepth();

    @Override
    Hierarchy getHierarchy();

    Level getChildLevel();

    Level getParentLevel();

    boolean isAll();

    boolean areMembersUnique();

    LevelType getLevelType();

    /** Returns properties defined against this level. */
    Property[] getProperties();

    /** Returns properties defined against this level and parent levels. */
    Property[] getInheritedProperties();

    /**
     * Returns the object that is used to format members of this level.
     */
    MemberFormatter getMemberFormatter();

    /**
     * Returns the approximate number of members in this level, or
     * {@link Integer#MIN_VALUE} if no approximation is known.
     */
    int getApproxRowCount();

    int getCardinality();

    List<Member> getMembers();

    boolean isUnique();

    SqlExpression getOrdinalExp();

    boolean isParentChild();

    boolean isShowParentAsLeaf();

    String getNameFormat();
}
