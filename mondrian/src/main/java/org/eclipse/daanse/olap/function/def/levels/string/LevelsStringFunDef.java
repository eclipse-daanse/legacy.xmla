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
package org.eclipse.daanse.olap.function.def.levels.string;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.LevelType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class LevelsStringFunDef extends AbstractFunctionDefinition {

    private static final String LEVELS = "Levels";

    static OperationAtom functionOperationAtom = new FunctionOperationAtom(LEVELS);
    static FunctionMetaData levelsFunctionMetaData = new FunctionMetaDataR(functionOperationAtom,
            "Returns the level whose name is specified by a string expression.", DataType.LEVEL,
            new FunctionParameterR[] { new FunctionParameterR(  DataType.STRING, "String" )});

    public LevelsStringFunDef() {
        super(levelsFunctionMetaData);
    }

    @Override
    public Type getResultType(Validator validator, Expression[] args) {
        final Type argType = args[0].getType();
        return LevelType.forType(argType);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final StringCalc stringCalc = compiler.compileString(call.getArg(0));
        return new LevelsStringCalc(call.getType(), stringCalc);
    }

}
