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
package org.eclipse.daanse.olap.function.def.percentile;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;
import org.eclipse.daanse.olap.calc.base.util.HirarchyDependsChecker;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.function.def.aggregate.AbstractAggregateFunDef;

public class PercentileCalc extends AbstractProfilingNestedDoubleCalc{

    public PercentileCalc(Type type, TupleListCalc tupleListCalc, Calc<?> calc, DoubleCalc percentCalc) {
        super(type, tupleListCalc, calc, percentCalc);
    }

    @Override
    public Double evaluate(Evaluator evaluator) {
        TupleList list = AbstractAggregateFunDef.evaluateCurrentList(getChildCalc(0, TupleListCalc.class), evaluator);
        Double percent = getChildCalc(2, DoubleCalc.class).evaluate(evaluator) * 0.01;
        final int savepoint = evaluator.savepoint();
        try {
            evaluator.setNonEmpty(false);
            final Double percentile =
                FunUtil.percentile(evaluator, list, getChildCalc(1, Calc.class), percent);
            return percentile;
        } finally {
            evaluator.restore(savepoint);
        }
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        return HirarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy);
    }

}
