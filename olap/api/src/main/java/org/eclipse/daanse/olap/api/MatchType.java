/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2003-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara
 * All Rights Reserved.
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

package org.eclipse.daanse.olap.api;

/**
 * <code>MatchType</code> enumerates the allowable match modes when
 * searching for a member based on its unique name.
 *
 * @author Zelaine Fong
 */
public enum MatchType {
    /** Match the unique name exactly, do not query database for members */
    EXACT_SCHEMA,
    /** Match the unique name exactly */
    EXACT,
    /** If no exact match, return the preceding member */
    BEFORE,
    /** If no exact match, return the next member */
    AFTER,
    /** Return the first child */
    FIRST,
    /** Return the last child */
    LAST;

    /**
     * Return true if either Exact or Exact Schema value
     * is selected.
     *
     * @return true if exact
     */
    public boolean isExact() {
        return this == EXACT || this == EXACT_SCHEMA;
    }
}
