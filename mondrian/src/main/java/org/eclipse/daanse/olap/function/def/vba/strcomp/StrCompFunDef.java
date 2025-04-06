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
package org.eclipse.daanse.olap.function.def.vba.strcomp;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.DecimalType;
import org.eclipse.daanse.olap.calc.base.constant.ConstantIntegerCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class StrCompFunDef  extends AbstractFunctionDefinition {


    public StrCompFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final StringCalc string1Calc = compiler.compileString(call.getArg(0));
        final StringCalc string2Calc = compiler.compileString(call.getArg(1));

        IntegerCalc compareCalc = null;
        if (call.getArgCount() == 2) {
            compareCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), 0);
        }
        if (call.getArgCount() == 3) {
            compareCalc = compiler.compileInteger(call.getArg(2));;
        }

        return new StrCompCalc(call.getType(), string1Calc, string2Calc, compareCalc);
    }

}
