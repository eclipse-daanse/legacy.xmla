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
package org.eclipse.daanse.olap.function.def.drilldownlevel;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.LevelCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Literal;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class DrilldownLevelFunDef extends AbstractFunctionDefinition {
    private static final String INCLUDE_CALC_MEMBERS = "INCLUDE_CALC_MEMBERS";
    //private static final List<String> RESERVED_WORDS=List.of(INCLUDE_CALC_MEMBERS);
    
    public DrilldownLevelFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc =
            compiler.compileList(call.getArg(0));
        final LevelCalc levelCalc =
            call.getArgCount() > 1
                && call.getArg(1).getType()
                instanceof org.eclipse.daanse.olap.api.type.LevelType
                ? compiler.compileLevel(call.getArg(1))
                : null;
        final IntegerCalc indexCalc =
            call.getArgCount() > 2
                && call.getArg(2) != null
                && !(call.getArg(2).getType() instanceof org.eclipse.daanse.olap.api.type.EmptyType)
                ? compiler.compileInteger(call.getArg(2))
                : null;
        final int arity = tupleListCalc.getType().getArity();
        final boolean includeCalcMembers =
            call.getArgCount() == 4
                && call.getArg(3) != null
                && call.getArg(3) instanceof Literal literal
                && DrilldownLevelFunDef.INCLUDE_CALC_MEMBERS.equals(literal.getValue());
        if (indexCalc == null) {
            return new DrilldownLevelCalc(call.getType(), tupleListCalc, levelCalc, includeCalcMembers);
        } else {
            return new DrilldownLevelWithIndexCalc(call.getType(), tupleListCalc, indexCalc, arity, includeCalcMembers)
            {
            };
        }
    }

}
