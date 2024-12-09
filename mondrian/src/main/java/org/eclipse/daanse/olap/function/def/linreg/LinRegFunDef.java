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

import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.value.CurrentValueDoubleCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class LinRegFunDef extends AbstractFunctionDefinition {

    public static final int POINT = 0;
    public static final int R2 = 1;
    public static final int INTERCEPT = 2;
    public static final int SLOPE = 3;
    public static final int VARIANCE = 4;

    private final int regType;
    
    public LinRegFunDef(FunctionMetaData functionMetaData, final int regType) {
        super(functionMetaData);
        this.regType = regType;
    }

    @Override
    public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc = compiler.compileList(call.getArg(0));
        final DoubleCalc yCalc = compiler.compileDouble(call.getArg(1));
        final DoubleCalc xCalc =
            call.getArgCount() > 2
            ? compiler.compileDouble(call.getArg(2))
            : new CurrentValueDoubleCalc(call.getType());
        return new LinRegCalc(call, tupleListCalc, yCalc, xCalc, regType);
    }

}
