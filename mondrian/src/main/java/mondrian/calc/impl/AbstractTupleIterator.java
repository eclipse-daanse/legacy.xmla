/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package mondrian.calc.impl;

import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.daanse.olap.api.calc.todo.TupleIterator;
import org.eclipse.daanse.olap.api.element.Member;

/**
 * Abstract implementation of {@link TupleIterator}.
 *
 * <p>Derived classes need to implement only {@link #forward()}.
 * {@code forward} must set the {@link #current}
 * field, and derived classes can use it.
 *
 * @author jhyde
 */
public abstract class AbstractTupleIterator
extends AbstractTupleCursor
implements TupleIterator
{
    protected boolean hasNext;

    protected AbstractTupleIterator(int arity) {
        super(arity);
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public List<Member> next() {
        if(!hasNext()){
            throw new NoSuchElementException();
        }
        final List<Member> o = current();
        hasNext = forward();
        return o;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

}
