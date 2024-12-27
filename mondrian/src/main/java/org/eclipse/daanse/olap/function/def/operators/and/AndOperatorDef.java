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
package org.eclipse.daanse.olap.function.def.operators.and;

import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.BooleanCalc;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class AndOperatorDef extends AbstractFunctionDefinition {

    // <Logical Expression> AND <Logical Expression>
    static InfixOperationAtom infixOperationAtom = new InfixOperationAtom("AND");
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(infixOperationAtom,
            "Returns the conjunction of two conditions.", "<LOGICAL> AND <LOGICAL>", DataType.LOGICAL,
            new FunctionParameterR[] { new FunctionParameterR( DataType.LOGICAL ), new FunctionParameterR( DataType.LOGICAL ) });

    public AndOperatorDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final BooleanCalc calc0 = compiler.compileBoolean(call.getArg(0));
        final BooleanCalc calc1 = compiler.compileBoolean(call.getArg(1));
        return new AndCalc(call.getType(), calc0, calc1);
    }

}
