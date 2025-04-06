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
package org.eclipse.daanse.olap.function.def.linreg;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.value.CurrentValueDoubleCalc;


public class PointFunDef extends LinRegFunDef {

    public PointFunDef(FunctionMetaData functionMetaData, int regType) {
        super(functionMetaData, regType);
    }

    @Override
    public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final DoubleCalc xPointCalc =
            compiler.compileDouble(call.getArg(0));
        final TupleListCalc tupleListCalc = compiler.compileList(call.getArg(1));
        final DoubleCalc yCalc = compiler.compileDouble(call.getArg(2));
        final DoubleCalc xCalc =
            call.getArgCount() > 3
            ? compiler.compileDouble(call.getArg(3))
            : new CurrentValueDoubleCalc(call.getType());
        return new PointCalc(
            call, xPointCalc, tupleListCalc, yCalc, xCalc);
    }
}
