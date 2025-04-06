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
package org.eclipse.daanse.olap.function.def.vba.fv;

import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.BooleanType;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.calc.base.constant.ConstantBooleanCalc;
import org.eclipse.daanse.olap.calc.base.constant.ConstantDoubleCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class FVFunDef  extends AbstractFunctionDefinition {


    public FVFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final DoubleCalc rateCalc = compiler.compileDouble(call.getArg(0));
        final DoubleCalc nPerCalc = compiler.compileDouble(call.getArg(1));
        final DoubleCalc pmtCalc = compiler.compileDouble(call.getArg(2));
        DoubleCalc pvCalc = null;
        BooleanCalc typeCalc = null;
        if (call.getArgCount() == 3) {
            pvCalc = new ConstantDoubleCalc(NumericType.INSTANCE, 0d);
            typeCalc = new ConstantBooleanCalc(BooleanType.INSTANCE, false);
        }
        if (call.getArgCount() == 4) {
            pvCalc = compiler.compileDouble(call.getArg(3));
            typeCalc = new ConstantBooleanCalc(BooleanType.INSTANCE, false);
        }
        if (call.getArgCount() == 5) {
            pvCalc = compiler.compileDouble(call.getArg(3));
            typeCalc = compiler.compileBoolean(call.getArg(4));
        }

        return new FVCalc(call.getType(), rateCalc, nPerCalc, pmtCalc, pvCalc, typeCalc);
    }

}
