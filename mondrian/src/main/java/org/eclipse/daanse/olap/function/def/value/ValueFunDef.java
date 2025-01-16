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
package org.eclipse.daanse.olap.function.def.value;

import java.io.PrintWriter;
import java.util.stream.Stream;

import org.eclipse.daanse.mdx.model.api.expression.operation.ParenthesesOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.query.base.Expressions;

public class ValueFunDef extends AbstractFunctionDefinition {
    private final DataType[] argTypes;

    ValueFunDef(DataType[] argTypes) {
        super(
                new FunctionMetaDataR(new ParenthesesOperationAtom(), "Pseudo-function which evaluates a tuple.",
                        DataType.NUMERIC, Stream.of(argTypes).map(dt -> new FunctionParameterR(dt)).toArray(FunctionParameterR[]::new)));
        this.argTypes = argTypes;
    }

    @Override
    public void unparse(Expression[] args, PrintWriter pw) {
        Expressions.unparseExpressions(pw, args, "(", ", ", ")");
    }

    @Override
    public Type getResultType(Validator validator, Expression[] args) {
        return null;
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        throw new UnsupportedOperationException();
    }

}
