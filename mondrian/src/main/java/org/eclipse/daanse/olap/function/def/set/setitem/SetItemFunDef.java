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
package org.eclipse.daanse.olap.function.def.set.setitem;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.StringType;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.fun.FunUtil;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class SetItemFunDef extends AbstractFunctionDefinition {
    
    private static final String NAME = "Item";
    static OperationAtom functionAtom = new MethodOperationAtom(NAME);
    
    public SetItemFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Type getResultType(Validator validator, Expression[] args) {
        SetType setType = (SetType) args[0].getType();
        return setType.getElementType();
    }

    @Override
    public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc =
            compiler.compileList(call.getArg(0));
        final Type elementType =
            ((SetType) tupleListCalc.getType()).getElementType();
        final boolean isString =
            call.getArgCount() < 2
            || call.getArg(1).getType() instanceof StringType;
        final IntegerCalc indexCalc;
        final StringCalc[] stringCalcs;
        List<Calc<?>> calcList = new ArrayList<>();
        calcList.add(tupleListCalc);
        if (isString) {
            indexCalc = null;
            stringCalcs = new StringCalc[call.getArgCount() - 1];
            for (int i = 0; i < stringCalcs.length; i++) {
                stringCalcs[i] = compiler.compileString(call.getArg(i + 1));
                calcList.add(stringCalcs[i]);
            }
        } else {
            stringCalcs = null;
            indexCalc = compiler.compileInteger(call.getArg(1));
            calcList.add(indexCalc);
        }
        Calc<?>[] calcs = calcList.toArray(new Calc[calcList.size()]);
        if (elementType instanceof TupleType tupleType) {
            final Member[] nullTuple = FunUtil.makeNullTuple(tupleType);
            if (isString) {
                return new SetItemStringTupleCalc(call.getType(), calcs, tupleListCalc, stringCalcs);
            } else {
                return new SetItemTupleCalc(call.getType(), calcs, tupleListCalc, indexCalc, nullTuple) {
                };
            }
        } else {
            final MemberType memberType = (MemberType) elementType;
            final Member nullMember = FunUtil.makeNullMember(memberType);
            if (isString) {
                return new SetItemStringCalc(call.getType(), calcs, tupleListCalc, stringCalcs, nullMember) {
                };
            } else {
                return new SetItemCalc(call.getType(), calcs, tupleListCalc, indexCalc, nullMember) {
                };
            }
        }
    }

    static boolean matchMember(final Member member, String name) {
        return member.getName().equals(name);
    }
}
