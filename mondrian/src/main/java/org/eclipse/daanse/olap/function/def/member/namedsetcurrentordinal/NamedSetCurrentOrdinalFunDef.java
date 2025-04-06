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
package org.eclipse.daanse.olap.function.def.member.namedsetcurrentordinal;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.NamedSetExpression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.exceptions.NotANamedSetException;

public class NamedSetCurrentOrdinalFunDef extends AbstractFunctionDefinition {

    static OperationAtom plainPropertyOperationAtom = new PlainPropertyOperationAtom("CurrentOrdinal");

    static FunctionMetaData functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom,
            "Returns the ordinal of the current iteration through a named set.",
            DataType.INTEGER, new FunctionParameterR[] { new FunctionParameterR( DataType.SET ) });

    public NamedSetCurrentOrdinalFunDef() {
        super(functionMetaData);
    }

    @Override
    public Expression createCall(Validator validator, Expression[] args) {
        assert args.length == 1;
        final Expression arg0 = args[0];
        if (!(arg0 instanceof NamedSetExpression)) {
            throw new NotANamedSetException();
        }
        return super.createCall(validator, args);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final Expression arg0 = call.getArg(0);
        assert arg0 instanceof NamedSetExpression : "checked this in createCall";
        final NamedSetExpression namedSetExpr = (NamedSetExpression) arg0;
        return new NamedSetCurrentOrdinalCalc(call.getType(), namedSetExpr);
    }
}
