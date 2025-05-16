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
package org.eclipse.daanse.olap.function.def.set;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.todo.TupleCursor;
import org.eclipse.daanse.olap.api.calc.todo.TupleIterable;
import org.eclipse.daanse.olap.api.calc.todo.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.Type;

import mondrian.calc.impl.AbstractIterCalc;
import mondrian.calc.impl.AbstractTupleCursor;
import mondrian.calc.impl.AbstractTupleIterable;
import mondrian.calc.impl.TupleCollections;

/**
 * Compiled expression that evaluates one or more expressions, each of which
 * yields a tuple or a set of tuples, and returns the result as a tuple
 * iterator.
 */
public class ExprIterCalc  extends AbstractIterCalc {
    private final TupleIteratorCalc[] tupleIteratorCalcs;

    public ExprIterCalc(
        Type type,
        Expression[] args,
        ExpressionCompiler compiler,
        List<ResultStyle> resultStyles)
    {
        super(type);
        final List<Calc<?>> calcList =
            SetFunDef.compileSelf(args, compiler, resultStyles);
        tupleIteratorCalcs = calcList.toArray(new TupleIteratorCalc[calcList.size()]);
    }

    // override return type
    @Override
    public TupleIteratorCalc[] getChildCalcs() {
        return tupleIteratorCalcs;
    }

    @Override
    public TupleIterable evaluate(final Evaluator evaluator) {
        return new AbstractTupleIterable(getType().getArity()) {
            @Override
            public TupleCursor tupleCursor() {
                return new AbstractTupleCursor(arity) {
                    Iterator<TupleIteratorCalc> calcIterator =
                        Arrays.asList(tupleIteratorCalcs).iterator();
                    TupleCursor currentCursor =
                        TupleCollections.emptyList(1).tupleCursor();

                    @Override
                    public boolean forward() {
                        while (true) {
                            if (currentCursor.forward()) {
                                return true;
                            }
                            if (!calcIterator.hasNext()) {
                                return false;
                            }
                            currentCursor =
                                ((TupleIterable) calcIterator.next()
                                    .evaluate(evaluator))
                                    .tupleCursor();
                        }
                    }

                    @Override
                    public List<Member> current() {
                        return currentCursor.current();
                    }

                    @Override
                    public void setContext(Evaluator evaluator) {
                        currentCursor.setContext(evaluator);
                    }

                    @Override
                    public void currentToArray(
                        Member[] members, int offset)
                    {
                        currentCursor.currentToArray(members, offset);
                    }

                    @Override
                    public Member member(int column) {
                        return currentCursor.member(column);
                    }
                };
            }
        };
    }
}
