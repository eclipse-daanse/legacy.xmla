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

package org.eclipse.daanse.olap.api.access;

import org.eclipse.daanse.olap.api.element.Member;

/**
 * Represents the access that a role has to a particular hierarchy.
 */
public interface HierarchyAccess {
    /**
     * Returns the access the current role has to a given member.
     *
     * <p>Visibility is:<ul>
     * <li>{@link Access#NONE} if member is not visible,
     * <li>{@link Access#ALL} if member and all children are visible,
     * <li>{@link Access#CUSTOM} if some of the children are not visible.
     * </ul></p>
     *
     * <p>For these purposes, children which are below the bottom level are
     * regarded as visible.</p>
     *
     * @param member Member.
     * @return Return current role's access to member.
     */
    AccessMember getAccess(Member member);

    /**
     * Returns the depth of the highest level to which the current Role has
     * access. The 'all' level, if present, has a depth of zero.
     *
     * @return Returns depth of the highest accessible level.
     */
    int getTopLevelDepth();

    /**
     * Returns the depth of the lowest level to which the current Role has
     * access. The 'all' level, if present, has a depth of zero.
     *
     * @return Returns depth of the lowest accessible level.
     */
    int getBottomLevelDepth();

    /**
     * Returns the policy by which cell values are calculated if not all
     * of a member's children are visible.
     *
     * @return Returns rollup policy.
     */
    RollupPolicy getRollupPolicy();

    /**
     * Returns <code>true</code> if at least one of the descendants of the
     * given Member is inaccessible to this Role.
     *
     * <p>Descendants which are inaccessible because they are below the
     * bottom level are ignored.
     *
     * @param member Member
     * @return Returns whether a descendant is inaccessible.
     */
    boolean hasInaccessibleDescendants(Member member);
}
