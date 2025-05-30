/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 * 
 * For more information please visit the Project: Hitachi Vantara - Mondrian
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


package org.eclipse.daanse.olap.api.calc.todo;

import java.util.List;

import org.eclipse.daanse.olap.api.element.Member;

/**
 * Extension to {@link Iterable} that returns a {@link TupleIterator}.
 *
 * <p>If efficiency is important, call {@link #tupleCursor()} rather than
 * {@link #tupleIterator()} if possible. Because {@link TupleCursor} is a
 * simpler API to implement than {@link TupleIterator}, in some cases the
 * implementation may be more efficient.
 *
 * @author jhyde
 */
public interface TupleIterable extends Iterable<List<Member>> {
    /**
     * Creates an iterator over the contents of this iterable.
     *
     * <p>Always has the same effect as calling {@link #iterator()}.
     *
     * @see #tupleCursor()
     *
     * @return cursor over the tuples returned by this iterable
     */
    TupleIterator tupleIterator();

    /**
     * Creates a cursor over the contents of this iterable.
     *
     * <p>The contents of the cursor will always be the same as those returned
     * by {@link #tupleIterator()}. Because {@link TupleCursor} is a simpler API
     * to implement than {@link TupleIterator}, in some cases the implementation
     * may be more efficient.
     *
     * @return cursor over the tuples returned by this iterable
     */
    TupleCursor tupleCursor();

    /**
     * Returns the number of members in each tuple.
     *
     * @return The number of members in each tuple
     */
    int getArity();

    /**
     * Returns an iterable over the members at a given column.
     *
     * <p>The iteratble returns an interator that is modifiable if and only if
     * this TupleIterable is modifiable.
     *
     * <p>If this {@code TupleIterable} happens to be a {@link TupleList},
     * the method is overridden to return a {@link List}&lt;{@link Member}&gt;.
     *
     * @param column Ordinal of the member in each tuple to project
     * @return Iterable that returns an iterator over members
     * @throws IllegalArgumentException if column is not less than arity
     */
    Iterable<Member> slice(int column);
}
