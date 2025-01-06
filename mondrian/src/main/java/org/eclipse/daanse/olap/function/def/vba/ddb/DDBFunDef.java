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
package org.eclipse.daanse.olap.function.def.vba.ddb;

import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.constant.ConstantDoubleCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.type.NumericType;

public class DDBFunDef  extends AbstractFunctionDefinition {


    public DDBFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final DoubleCalc costCalc = compiler.compileDouble(call.getArg(0));
        final DoubleCalc salvageCalc = compiler.compileDouble(call.getArg(1));
        final DoubleCalc lifeCalc = compiler.compileDouble(call.getArg(2));
        final DoubleCalc periodCalc = compiler.compileDouble(call.getArg(3));
        DoubleCalc factorCalc = null;
        if (call.getArgCount() == 4) {
            factorCalc = new ConstantDoubleCalc(NumericType.INSTANCE, 2.0);
        }
        if (call.getArgCount() == 5) {
            factorCalc = compiler.compileDouble(call.getArg(4));
        }

        return new DDBCalc(call.getType(), costCalc, salvageCalc, lifeCalc, periodCalc, factorCalc);
    }

}
