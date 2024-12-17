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
package org.eclipse.daanse.olap.function.def.nonempty;

import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.util.HirarchyDependsChecker;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.TupleCollections;

public class NonEmptyCalc extends AbstractListCalc {

    public NonEmptyCalc(
            Type type,
            TupleListCalc listCalc1,
            TupleListCalc listCalc2)
    {
        super(type, new Calc[]{listCalc1, listCalc2});
    }

    @Override
    public TupleList evaluateList(Evaluator evaluator) {
        final int savepoint = evaluator.savepoint();
        TupleListCalc listCalc1 = getChildCalc(0, TupleListCalc.class);
        TupleListCalc listCalc2 = getChildCalc(1, TupleListCalc.class);
        try {
            evaluator.setNonEmpty(true);

            TupleList leftTuples = listCalc1.evaluateList(evaluator);
            if (leftTuples.isEmpty()) {
                return TupleCollections.emptyList(leftTuples.getArity());
            }
            TupleList rightTuples = null;
            if(listCalc2 != null) {
                rightTuples = listCalc2.evaluateList(evaluator);
            }
            TupleList result =
                    TupleCollections.createList(leftTuples.getArity());


            for (List<Member> leftTuple : leftTuples) {
                if(rightTuples != null && !rightTuples.isEmpty()) {
                    for (List<Member> rightTuple : rightTuples) {
                        evaluator.setContext(leftTuple);
                        evaluator.setContext(rightTuple);
                        Object tupleResult = evaluator.evaluateCurrent();
                        if (tupleResult != null)
                        {
                            result.add(leftTuple);
                            break;
                        }
                    }
                }
                else {
                    evaluator.setContext(leftTuple);
                    Object tupleResult = evaluator.evaluateCurrent();
                    if (tupleResult != null)
                    {
                        result.add(leftTuple);
                    }
                }
            }
            return result;
        } finally {
            evaluator.restore(savepoint);
        }
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        return HirarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy);
    }
}
