/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.function.def.vba.instr;

import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.DecimalType;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.DateTimeCalc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.constant.ConstantIntegerCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class InStrFunDef  extends AbstractFunctionDefinition {


    public InStrFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        IntegerCalc startCalc = null; /* default 1 */
        StringCalc stringCheckCalc = null;
        StringCalc stringMatchCalc = null;
        IntegerCalc compareCalc = null;
        if (call.getArgCount() == 2) {
            startCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), 1);
            stringCheckCalc = compiler.compileString(call.getArg(0));
            stringMatchCalc = compiler.compileString(call.getArg(1));
            compareCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), 0);
        }
        if (call.getArgCount() == 3) {
            startCalc = compiler.compileInteger(call.getArg(0));
            stringCheckCalc = compiler.compileString(call.getArg(1));
            stringMatchCalc = compiler.compileString(call.getArg(2));
            compareCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), 0);
        }
        if (call.getArgCount() == 4) {
            startCalc = compiler.compileInteger(call.getArg(0));
            stringCheckCalc = compiler.compileString(call.getArg(1));
            stringMatchCalc = compiler.compileString(call.getArg(2));
            compareCalc = compiler.compileInteger(call.getArg(3));
        }

        return new InStrCalc(call.getType(), startCalc, stringCheckCalc, stringMatchCalc, compareCalc);
    }

}
