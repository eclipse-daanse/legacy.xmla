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
package org.eclipse.daanse.olap.function.def.operators.greater;

import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class GreaterOrEqualStringOperatorDef extends AbstractFunctionDefinition {

    // <String Expression> >= <String Expression>
    static InfixOperationAtom infixOperationAtom = new InfixOperationAtom(">=");
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Returns whether an expression is greater than or equal to another.",
            "<STRING> >= <STRING>", DataType.LOGICAL, new DataType[] { DataType.STRING, DataType.STRING });

    public GreaterOrEqualStringOperatorDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final StringCalc calc0 = compiler.compileString(call.getArg(0));
        final StringCalc calc1 = compiler.compileString(call.getArg(1));
        return new GreaterOrEqualStringCalc(call.getType(), calc0, calc1);
    }

}
