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

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.todo.TupleIterable;
import org.eclipse.daanse.olap.api.calc.todo.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.AbstractProfilingNestedCalc;

/**
 * Abstract implementation of the
 * {@link org.eclipse.daanse.olap.api.calc.todo.TupleIteratorCalc} interface.
 *
 * <p>
 * The derived class must implement the
 * {@link #evaluateIterable(org.eclipse.daanse.olap.api.Evaluator)} method, and
 * the {@link #evaluate(org.eclipse.daanse.olap.api.Evaluator)} method will call
 * it.
 *
 * @see org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractProfilingNestedTupleListCalc
 *
 * @author jhyde
 * @since Oct 24, 2008
 */
public abstract class AbstractProfilingNestedTupleIteratorCalc extends AbstractProfilingNestedCalc<TupleIterable> implements TupleIteratorCalc<TupleIterable> {
    /**
     * Creates an abstract implementation of a compiled expression which returns a
     * {@link TupleIterable}.
     *
     * @param exp   Expression which was compiled
     * @param calcs List of child compiled expressions (for dependency analysis)
     */
    protected AbstractProfilingNestedTupleIteratorCalc(Type type, Calc<?>... calcs) {
        super(type, calcs);
    }

    @Override
    public SetType getType() {
        return (SetType) super.getType();
    }

    @Override
    public ResultStyle getResultStyle() {
        return ResultStyle.ITERABLE;
    }

    @Override
    public String toString() {
        return "AbstractIterCalc object";
    }
}
