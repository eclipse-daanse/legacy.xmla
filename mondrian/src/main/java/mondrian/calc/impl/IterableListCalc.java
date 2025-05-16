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

/**
 * Adapter that converts a
 * {@link org.eclipse.daanse.olap.api.calc.todo.TupleIteratorCalc} to a
 * {@link org.eclipse.daanse.olap.api.calc.todo.TupleListCalc}.
 *
 * @author jhyde
 * @since Oct 23, 2008
 */
public class IterableListCalc extends AbstractListCalc {
    private final TupleIteratorCalc<?> tupleIteratorCalc;

    /**
     * Creates an IterableListCalc.
     *
     * @param tupleIteratorCalc Calculation that returns an iterable.
     */
    public IterableListCalc(TupleIteratorCalc tupleIteratorCalc) {
        super(tupleIteratorCalc.getType(), new Calc[] { tupleIteratorCalc });
        this.tupleIteratorCalc = tupleIteratorCalc;
    }

    @Override
    public TupleList evaluate(Evaluator evaluator) {
        // A TupleIterCalc is allowed to return a list. If so, save the copy.
        final TupleIterable iterable = tupleIteratorCalc.evaluate(evaluator);
        if (iterable instanceof TupleList tupleList) {
            return tupleList;
        }

        final TupleList list = TupleCollections.createList(iterable.getArity());
        final TupleCursor tupleCursor = iterable.tupleCursor();
        while (tupleCursor.forward()) {
            // REVIEW: Worth creating TupleList.addAll(TupleCursor)?
            list.addCurrent(tupleCursor);
        }
        return list;
    }
}
