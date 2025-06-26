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

/**
 * Enumeration of the policies by which a cell is calculated if children of a
 * member are not accessible.
 */
public enum RollupPolicy {
    /**
     * The value of the cell is null if any of the children are inaccessible.
     */
    HIDDEN,

    /**
     * The value of the cell is obtained by rolling up the values of accessible
     * children.
     */
    PARTIAL,

    /**
     * The value of the cell is obtained by rolling up the values of all children.
     */
    FULL,
}