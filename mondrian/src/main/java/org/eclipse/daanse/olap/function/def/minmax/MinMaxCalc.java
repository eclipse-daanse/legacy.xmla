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
package org.eclipse.daanse.olap.function.def.minmax;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;
import org.eclipse.daanse.olap.calc.base.util.HirarchyDependsChecker;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.function.def.aggregate.AbstractAggregateFunDef;

public class MinMaxCalc extends AbstractProfilingNestedDoubleCalc {

    private static final String TIMING_NAME = MinMaxFunDef.class.getSimpleName();
    private final boolean max;

    protected MinMaxCalc(Type type, final TupleListCalc tupleListCalc, final Calc<?> calc, boolean max) {
        super(type, tupleListCalc, calc);
        this.max = max;
    }

    @Override
    public Double evaluate(Evaluator evaluator) {
        evaluator.getTiming().markStart(TIMING_NAME);
        final int savepoint = evaluator.savepoint();
        Calc<?> calc = getChildCalc(1, Calc.class);
        try {
            TupleList memberList = AbstractAggregateFunDef.evaluateCurrentList(getChildCalc(0, TupleListCalc.class),
                    evaluator);
            evaluator.setNonEmpty(false);
            return (Double) (max ? FunUtil.max(evaluator, memberList, calc) : FunUtil.min(evaluator, memberList, calc));
        } finally {
            evaluator.restore(savepoint);
            evaluator.getTiming().markEnd(TIMING_NAME);
        }
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        return HirarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy);
    }

}
