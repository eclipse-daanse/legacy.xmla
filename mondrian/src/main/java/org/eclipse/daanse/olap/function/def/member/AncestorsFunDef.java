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
package org.eclipse.daanse.olap.function.def.member;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.LevelCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.LevelType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.TupleCollections;
import mondrian.olap.fun.FunUtil;

public class AncestorsFunDef extends AbstractFunctionDefinition {

    public AncestorsFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final MemberCalc memberCalc = compiler.compileMember(call.getArg(0));
        final Type type1 = call.getArg(1).getType();
        if (type1 instanceof LevelType) {
            final LevelCalc levelCalc = compiler.compileLevel(call.getArg(1));
            return new AncestorsCalcForLevelType(call.getType(), memberCalc, levelCalc);
        } else {
            final IntegerCalc distanceCalc = compiler.compileInteger(call.getArg(1));
            return new AncestorsCalc(call.getType(), memberCalc, distanceCalc) {
                @Override
                public TupleList evaluateList(Evaluator evaluator) {
                    Member member = memberCalc.evaluate(evaluator);
                    Integer distance = distanceCalc.evaluate(evaluator);
                    List<Member> ancestors = new ArrayList<>();
                    for (int curDist = 1; curDist <= distance; curDist++) {
                        ancestors.add(FunUtil.ancestor(evaluator, member, curDist, null));
                    }
                    return TupleCollections.asTupleList(ancestors);
                }
            };
        }
    }

}