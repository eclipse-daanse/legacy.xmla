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
package org.eclipse.daanse.olap.function.def.generate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Execution;
import org.eclipse.daanse.olap.api.calc.todo.TupleCursor;
import org.eclipse.daanse.olap.api.calc.todo.TupleIterable;
import org.eclipse.daanse.olap.api.calc.todo.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.util.HirarchyDependsChecker;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.TupleCollections;
import mondrian.server.LocusImpl;
import mondrian.util.CancellationChecker;

public class GenerateListCalc extends AbstractListCalc {
    private final int arityOut;
    private final boolean all;

    public GenerateListCalc(Type type, TupleIteratorCalc tupleIteratorCalc, TupleListCalc listCalc2, int arityOut,
            boolean all) {
        super(type, tupleIteratorCalc, listCalc2);
        this.arityOut = arityOut;
        this.all = all;
    }

    @Override
    public TupleList evaluateList(Evaluator evaluator) {
        TupleIteratorCalc tupleIteratorCalc = getChildCalc(0, TupleIteratorCalc.class);
        TupleListCalc listCalc2 = getChildCalc(1, TupleListCalc.class);
        final int savepoint = evaluator.savepoint();
        try {
            evaluator.setNonEmpty(false);
            final TupleIterable iterable1 = tupleIteratorCalc.evaluateIterable(evaluator);
            evaluator.restore(savepoint);
            TupleList result = TupleCollections.createList(arityOut);
            Execution execution = LocusImpl.peek().getExecution();
            if (all) {
                final TupleCursor cursor = iterable1.tupleCursor();
                int rowCount = 0;
                while (cursor.forward()) {
                    CancellationChecker.checkCancelOrTimeout(rowCount++, execution);
                    cursor.setContext(evaluator);
                    final TupleList result2 = listCalc2.evaluateList(evaluator);
                    result.addAll(result2);
                }
            } else {
                final Set<List<Member>> emitted = new HashSet<>();
                final TupleCursor cursor = iterable1.tupleCursor();

                int rowCount = 0;
                while (cursor.forward()) {
                    CancellationChecker.checkCancelOrTimeout(rowCount++, execution);
                    cursor.setContext(evaluator);
                    final TupleList result2 = listCalc2.evaluateList(evaluator);
                    addDistinctTuples(result, result2, emitted);
                }
            }
            return result;
        } finally {
            evaluator.restore(savepoint);
        }
    }

    private void addDistinctTuples(TupleList result, TupleList result2, Set<List<Member>> emitted) {
        for (List<Member> row : result2) {
            // wrap array for correct distinctness test
            if (emitted.add(row)) {
                result.add(row);
            }
        }
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        return HirarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy);
    }
}
