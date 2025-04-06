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
package org.eclipse.daanse.olap.function.def.union;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.fun.FunUtil;

public class UnionFunDef extends AbstractFunctionDefinition {

    public UnionFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        String allString = FunUtil.getLiteralArg(call, 2, "DISTINCT", UnionResolver.ReservedWords);
        final boolean all = allString.equalsIgnoreCase("ALL");
        // todo: do at validate time
        FunUtil.checkCompatible(call.getArg(0), call.getArg(1), null);
        final TupleListCalc listCalc0 = compiler.compileList(call.getArg(0));
        final TupleListCalc listCalc1 = compiler.compileList(call.getArg(1));
        return new UnionCalc(call.getType(), listCalc0, listCalc1, all);
    }
}
