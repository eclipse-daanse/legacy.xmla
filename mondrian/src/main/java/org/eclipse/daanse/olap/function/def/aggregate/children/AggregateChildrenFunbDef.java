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
package org.eclipse.daanse.olap.function.def.aggregate.children;

import java.io.PrintWriter;

import org.eclipse.daanse.mdx.model.api.expression.operation.InternalOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.ValueCalc;

public class AggregateChildrenFunbDef extends AbstractFunctionDefinition {
    static OperationAtom functionAtom$AggregateChildren = new InternalOperationAtom("$AggregateChildren");
    static FunctionMetaData functionMetaData$AggregateChildren = new FunctionMetaDataR(functionAtom$AggregateChildren,
            "Equivalent to 'Aggregate(<Hierarchy>.CurrentMember.Children); for internal use.",
            "$AggregateChildren(<Hierarchy>)", DataType.NUMERIC, new DataType[] { DataType.HIERARCHY });

    public AggregateChildrenFunbDef() {
        super(functionMetaData$AggregateChildren);
    }

    @Override
    public void unparse(Expression[] args, PrintWriter pw) {
        pw.print(getFunctionMetaData().operationAtom().name());
        pw.print("(");
        args[0].unparse(pw);
        pw.print(")");
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        return new AggregateChildrenCalc(call.getType(), compiler.compileHierarchy(call.getArg(0)),
                new ValueCalc(call.getType()));
    }

}
