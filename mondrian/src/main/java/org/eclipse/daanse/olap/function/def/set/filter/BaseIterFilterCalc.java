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
import org.eclipse.daanse.olap.api.NativeEvaluator;
import org.eclipse.daanse.olap.api.SchemaReader;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.todo.TupleIterable;
import org.eclipse.daanse.olap.calc.base.util.HirarchyDependsChecker;

import mondrian.calc.impl.AbstractIterCalc;

public abstract class BaseIterFilterCalc extends AbstractIterCalc {
    private ResolvedFunCall call;

    protected BaseIterFilterCalc(ResolvedFunCall call, Calc<?>[] calcs) {
        super(call.getType(), calcs);
        this.call=call;
    }

    @Override
    public TupleIterable evaluateIterable(Evaluator evaluator) {
        evaluator.getTiming().markStart(FilterFunDef.TIMING_NAME);
        try {
            // Use a native evaluator, if more efficient.
            // TODO: Figure this out at compile time.
            SchemaReader schemaReader = evaluator.getSchemaReader();
            NativeEvaluator nativeEvaluator =
                schemaReader.getNativeSetEvaluator(
                    call.getFunDef(), call.getArgs(), evaluator, this);
            if (nativeEvaluator != null) {
                return (TupleIterable)
                    nativeEvaluator.execute(ResultStyle.ITERABLE);
            } else {
                return makeIterable(evaluator);
            }
        } finally {
            evaluator.getTiming().markEnd(FilterFunDef.TIMING_NAME);
        }
    }

    protected abstract TupleIterable makeIterable(Evaluator evaluator);

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        return HirarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy);
    }

}
