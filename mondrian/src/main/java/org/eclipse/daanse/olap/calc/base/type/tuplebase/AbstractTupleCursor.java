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

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.todo.TupleCursor;
import org.eclipse.daanse.olap.api.element.Member;

/**
 * Abstract implementation of {@link org.eclipse.daanse.olap.api.calc.todo.TupleIterator}.
 *
 * <p>Derived classes need to implement only {@link #forward()}.
 *
 * @author jhyde
 */
public abstract class AbstractTupleCursor implements TupleCursor {
    protected final int arity;

    protected AbstractTupleCursor(int arity) {
        this.arity = arity;
    }

    @Override
    public void setContext(Evaluator evaluator) {
        evaluator.setContext(current());
    }

    @Override
    public void currentToArray(Member[] members, int offset) {
        if (offset == 0) {
            current().toArray(members);
        } else {
            //noinspection SuspiciousSystemArraycopy
            System.arraycopy(current().toArray(), 0, members, offset, arity);
        }
    }

    @Override
    public int getArity() {
        return arity;
    }

    @Override
    public Member member(int column) {
        return current().get(column);
    }
}
