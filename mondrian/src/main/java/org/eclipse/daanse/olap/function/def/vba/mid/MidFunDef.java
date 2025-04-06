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
package org.eclipse.daanse.olap.function.def.vba.mid;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class MidFunDef  extends AbstractFunctionDefinition {


    public MidFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final StringCalc valueCalc = compiler.compileString(call.getArg(0));
        final IntegerCalc beginIndexCalc = compiler.compileInteger(call.getArg(1));

        IntegerCalc lengthCalc = null;
        if (call.getArgCount() == 3) {
            lengthCalc = compiler.compileInteger(call.getArg(2));
        }

        return new MidCalc(call.getType(), valueCalc, beginIndexCalc, lengthCalc);
    }

}
