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
package org.eclipse.daanse.olap.function.def.headtail;

import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.constant.ConstantIntegerCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.type.DecimalType;

public class HeadTailFunDef extends AbstractFunctionDefinition {

    private final boolean head;

    public HeadTailFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
        head = functionMetaData.operationAtom().name().equals("Head");
    }

    @Override
    public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc = compiler.compileList(call.getArg(0));
        final IntegerCalc integerCalc = call.getArgCount() > 1 ? compiler.compileInteger(call.getArg(1))
                : new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), 1);
        if (head) {
            return new HeadCalc(call.getType(), tupleListCalc, integerCalc);
        } else {
            return new TailCalc(call.getType(), tupleListCalc, integerCalc) {
            };
        }
    }

}
