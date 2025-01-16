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
package org.eclipse.daanse.olap.function.def.cache;

import java.io.PrintWriter;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.ExpCacheDescriptor;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.ExpCacheDescriptorImpl;

public class CacheFunDef extends AbstractFunctionDefinition {
    public static final String NAME = "Cache";
    private static final String DESCRIPTION = "Evaluates and returns its sole argument, applying statement-level caching";
    static OperationAtom functionAtom = new FunctionOperationAtom(NAME);

    public CacheFunDef(DataType category) {
        super(new FunctionMetaDataR(functionAtom, DESCRIPTION, category, new FunctionParameterR[] { new FunctionParameterR(  category )}));
    }

    @Override
    public void unparse(Expression[] args, PrintWriter pw) {
        args[0].unparse(pw);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final Expression exp = call.getArg(0);
        final ExpCacheDescriptor cacheDescriptor = new ExpCacheDescriptorImpl(exp, compiler);
        if (call.getType() instanceof SetType) {
            return new CacheGenericIterCalc(call.getType(), cacheDescriptor);
        } else {
            return new CacheGenericCalc(call.getType(), cacheDescriptor);
        }
    }
}
