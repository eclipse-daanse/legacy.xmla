/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
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

package org.eclipse.daanse.olap.calc.base.type.tuplebase;

import java.util.Iterator;
import java.util.List;

import org.eclipse.daanse.olap.api.calc.todo.TupleIterable;
import org.eclipse.daanse.olap.api.calc.todo.TupleIterator;
import org.eclipse.daanse.olap.api.element.Member;

/**
 * Abstract implementation of {@link org.eclipse.daanse.olap.api.calc.todo.TupleIterable}.
 *
 * <p>Derived classes need to implement only {@link #tupleCursor()},
 * and this implementation will implement {@link #tupleIterator()} and
 * {@link #iterator()} by creating a wrapper around that cursor. (The cursor
 * interface is easier to implement efficiently than the wider iterator
 * interface.) If you have a more efficient implementation of cursor, override
 * the {@code tupleIterator} method.
 *
 * @author jhyde
 */
public abstract class AbstractTupleIterable
implements TupleIterable
{
    protected final int arity;

    /**
     * Creates an AbstractTupleIterable.
     *
     * @param arity Arity (number of members per tuple)
     */
    protected AbstractTupleIterable(int arity) {
        this.arity = arity;
    }

    @Override
    public int getArity() {
        return arity;
    }

    @Override
    public Iterable<Member> slice(int column) {
        return TupleCollections.slice(this, column);
    }

    @Override
    public final Iterator<List<Member>> iterator() {
        return tupleIterator();
    }

    @Override
    public TupleIterator tupleIterator() {
        return TupleCollections.iterator(tupleCursor());
    }
}
