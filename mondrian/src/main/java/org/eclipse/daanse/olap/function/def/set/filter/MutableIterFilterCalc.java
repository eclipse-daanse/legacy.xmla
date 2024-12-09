/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.function.def.set.filter;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Execution;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.BooleanCalc;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.todo.TupleCursor;
import org.eclipse.daanse.olap.calc.api.todo.TupleIterable;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;

import mondrian.calc.impl.TupleCollections;
import mondrian.util.CancellationChecker;

public class MutableIterFilterCalc extends BaseIterFilterCalc {
    MutableIterFilterCalc(ResolvedFunCall call, Calc<?>[] calcs) {
        super(call, calcs);
        assert calcs[0] instanceof TupleListCalc;
        assert calcs[1] instanceof BooleanCalc;
    }

    @Override
    protected TupleIterable makeIterable(Evaluator evaluator) {
        evaluator.getTiming().markStart(FilterFunDef.TIMING_NAME);
        final int savepoint = evaluator.savepoint();
        try {
            Calc<?>[] calcs = getChildCalcs();
            TupleListCalc lcalc = (TupleListCalc) calcs[0];
            BooleanCalc bcalc = (BooleanCalc) calcs[1];

            TupleList list = lcalc.evaluateList(evaluator);

            // make list mutable; guess selectivity .5
            TupleList result =
                TupleCollections.createList(
                    list.getArity(), list.size() / 2);
            evaluator.setNonEmpty(false);
            TupleCursor cursor = list.tupleCursor();
            int currentIteration = 0;
            Execution execution =
                evaluator.getQuery().getStatement().getCurrentExecution();
            while (cursor.forward()) {
                CancellationChecker.checkCancelOrTimeout(
                    currentIteration++, execution);
                cursor.setContext(evaluator);
                if (bcalc.evaluate(evaluator)) {
                    result.addCurrent(cursor);
                }
            }
            return result;
        } finally {
            evaluator.restore(savepoint);
            evaluator.getTiming().markEnd(FilterFunDef.TIMING_NAME);
        }
    }
}
