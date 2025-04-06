/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package mondrian.calc.impl;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.todo.TupleCursor;
import org.eclipse.daanse.olap.api.calc.todo.TupleIterable;
import org.eclipse.daanse.olap.api.calc.todo.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.AbstractProfilingNestedCalc;

/**
 * Adapter which computes a set expression and converts it to any list or
 * iterable type.
 *
 * @author jhyde
 * @since Nov 7, 2008
 */
public abstract class GenericIterCalc
extends AbstractProfilingNestedCalc<Object>
implements TupleListCalc, TupleIteratorCalc
{
    /**
     * Creates a GenericIterCalc without specifying child calculated
     * expressions.
     *
     * <p>Subclass should override {@link #getChildCalcs()}.
     *
     * @param exp Source expression
     */
    protected GenericIterCalc( Type type) {
        super(   type);
    }

    /**
     * Creates an GenericIterCalc.
     *
     * @param exp Source expression
     * @param calcs Child compiled expressions
     */
    protected GenericIterCalc( Type type, Calc[] calcs) {
        super(type, calcs);
    }

    @Override
    public SetType getType() {
        return (SetType) super.getType();
    }

    @Override
    public TupleList evaluateList(Evaluator evaluator) {
        final Object o = evaluate(evaluator);
        if (o instanceof TupleList tupleList) {
            return tupleList;
        }
        // Iterable
        final TupleIterable iterable = (TupleIterable) o;
        final TupleList tupleList =
                TupleCollections.createList(iterable.getArity());
        final TupleCursor cursor = iterable.tupleCursor();
        while (cursor.forward()) {
            tupleList.addCurrent(cursor);
        }
        return tupleList;
    }

    @Override
    public TupleIterable evaluateIterable(Evaluator evaluator) {
        final Object o = evaluate(evaluator);
        return (TupleIterable) o;
    }

}
