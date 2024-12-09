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

import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Execution;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.BooleanCalc;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.todo.TupleCursor;
import org.eclipse.daanse.olap.calc.api.todo.TupleIterable;
import org.eclipse.daanse.olap.calc.api.todo.TupleIteratorCalc;

import mondrian.calc.impl.AbstractTupleCursor;
import mondrian.calc.impl.AbstractTupleIterable;
import mondrian.server.LocusImpl;
import mondrian.util.CancellationChecker;

public class IterIterFilterCalc extends BaseIterFilterCalc
{
    public IterIterFilterCalc(ResolvedFunCall call, Calc<?>[] calcs) {
        super(call, calcs);
        assert calcs[0] instanceof TupleIteratorCalc;
        assert calcs[1] instanceof BooleanCalc;
    }

    @Override
    protected TupleIterable makeIterable(Evaluator evaluator) {
        Calc<?>[] calcs = getChildCalcs();
        TupleIteratorCalc icalc = (TupleIteratorCalc) calcs[0];
        final BooleanCalc bcalc = (BooleanCalc) calcs[1];

        // This does dynamics, just in time,
        // as needed filtering
        final TupleIterable iterable =
            icalc.evaluateIterable(evaluator);
        final Evaluator evaluator2 = evaluator.push();
        evaluator2.setNonEmpty(false);
        return new AbstractTupleIterable(iterable.getArity()) {
            @Override
            public TupleCursor tupleCursor() {
                return new AbstractTupleCursor(iterable.getArity()) {
                    final TupleCursor cursor = iterable.tupleCursor();

                    @Override
                    public boolean forward() {
                        int currentIteration = 0;
                        Execution execution = LocusImpl.peek().getExecution();
                        while (cursor.forward()) {
                            CancellationChecker.checkCancelOrTimeout(
                                currentIteration++, execution);
                            cursor.setContext(evaluator2);
                            if (bcalc.evaluate(evaluator2)) {
                                return true;
                            }
                        }
                        return false;
                    }

                    @Override
                    public List<Member> current() {
                        return cursor.current();
                    }
                };
            }
        };
    }
}
