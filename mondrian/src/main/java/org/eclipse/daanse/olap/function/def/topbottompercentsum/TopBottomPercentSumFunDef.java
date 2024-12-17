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
package org.eclipse.daanse.olap.function.def.topbottompercentsum;

import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class TopBottomPercentSumFunDef extends AbstractFunctionDefinition {

    /**
     * Whether to calculate top (as opposed to bottom).
     */
    final boolean top;
    /**
     * Whether to calculate percent (as opposed to sum).
     */
    final boolean percent;

    public TopBottomPercentSumFunDef(FunctionMetaData functionMetaData, boolean top, boolean percent) {
        super(functionMetaData);
        this.top = top;
        this.percent = percent;
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc = compiler.compileList(call.getArg(0), true);
        final DoubleCalc doubleCalc = compiler.compileDouble(call.getArg(1));
        final Calc<?> calc = compiler.compileScalar(call.getArg(2), true);
        return new TopBottomPercentSumCalc(call.getType(), tupleListCalc, doubleCalc, calc, top, percent);
    }

}
