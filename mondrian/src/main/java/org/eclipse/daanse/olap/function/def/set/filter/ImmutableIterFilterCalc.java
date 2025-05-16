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
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.todo.TupleCursor;
import org.eclipse.daanse.olap.api.calc.todo.TupleIterable;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;

import mondrian.util.CancellationChecker;

public class ImmutableIterFilterCalc extends BaseIterFilterCalc {

    public ImmutableIterFilterCalc(ResolvedFunCall call, Calc<?>[] calcs) {
        super(call, calcs);
        assert calcs[0] instanceof TupleListCalc;
        assert calcs[1] instanceof BooleanCalc;
    }

    @Override
    protected TupleIterable makeIterable(Evaluator evaluator) {
        Calc<?>[] calcs = getChildCalcs();
        TupleListCalc lcalc = (TupleListCalc) calcs[0];
        BooleanCalc bcalc = (BooleanCalc) calcs[1];
        TupleList members = lcalc.evaluate(evaluator);

        // Not mutable, must create new list
        TupleList result = members.copyList(members.size() / 2);
        final int savepoint = evaluator.savepoint();
        try {
            evaluator.setNonEmpty(false);
            TupleCursor cursor = members.tupleCursor();
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
        }
    }
}
