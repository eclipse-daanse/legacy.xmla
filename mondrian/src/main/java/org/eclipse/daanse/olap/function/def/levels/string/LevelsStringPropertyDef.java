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

import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.LevelType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.HierarchyCalc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class LevelsStringPropertyDef extends AbstractFunctionDefinition {

    private static final String LEVELS = "Levels";

    static OperationAtom hierarchyMethodOperationAtom = new MethodOperationAtom(LEVELS);
    static FunctionMetaData hierarchyLevelsFunctionMetaData = new FunctionMetaDataR(hierarchyMethodOperationAtom,
            "Returns the level whose name is specified by a string expression.", "<HIERARCHY>.Levels(<STRING>)",
            DataType.LEVEL, new FunctionParameterR[] { new FunctionParameterR(  DataType.HIERARCHY ), new FunctionParameterR( DataType.STRING ) });

    public LevelsStringPropertyDef() {
        super(hierarchyLevelsFunctionMetaData);
    }

    @Override
    public Type getResultType(Validator validator, Expression[] args) {
        final Type argType = args[0].getType();
        return LevelType.forType(argType);
    }

    @Override
    public Calc<?> compileCall(final ResolvedFunCall call, ExpressionCompiler compiler) {
        final HierarchyCalc hierarchyCalc = compiler.compileHierarchy(call.getArg(0));
        final StringCalc nameCalc = compiler.compileString(call.getArg(1));
        return new LevelsStringPropertyCalc(call.getType(), hierarchyCalc, nameCalc);
    }

}
