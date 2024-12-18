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
package org.eclipse.daanse.olap.function.def.drilldownmember;

import java.util.List;

import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.fun.FunUtil;

public class DrilldownMemberFunDef extends AbstractFunctionDefinition {
    static final List<String> reservedWords = List.of("RECURSIVE");

    public DrilldownMemberFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc listCalc1 = compiler.compileList(call.getArg(0));
        final TupleListCalc listCalc2 = compiler.compileList(call.getArg(1));
        final String literalArg = FunUtil.getLiteralArg(call, 2, "", DrilldownMemberFunDef.reservedWords);
        final boolean recursive = literalArg.equals("RECURSIVE");

        return new DrilldownMemberCalc(call.getType(), listCalc1, listCalc2, recursive);
    }
}
