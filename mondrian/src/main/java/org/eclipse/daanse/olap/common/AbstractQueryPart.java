/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 1998-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
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
 */


package org.eclipse.daanse.olap.common;

import java.io.PrintWriter;

import org.eclipse.daanse.olap.api.Walkable;

/**
 * Component of an MDX query (derived classes include Query, Axis, Exp, Level).
 *
 * @author jhyde, 23 January, 1999
 */
public abstract class AbstractQueryPart implements Walkable<Object> {
    /**
     * Creates a QueryPart.
     */
    public AbstractQueryPart() {
    }

    /**
     * Writes a string representation of this parse tree
     * node to the given writer.
     *
     * @param pw writer
     */
    public void unparse(PrintWriter pw) {
        pw.print(toString());
    }

    // implement Walkable
    @Override
	public Object[] getChildren() {
        // By default, a QueryPart is atomic (has no children).
        return null;
    }

    /**
     * Returns the plan that Mondrian intends to use to execute this query.
     *
     * @param pw Print writer
     */
    public void explain(PrintWriter pw) {
        throw new UnsupportedOperationException(
            new StringBuilder("explain not implemented for ").append(this).append(" (").append(getClass()).append(")").toString());
    }
}
