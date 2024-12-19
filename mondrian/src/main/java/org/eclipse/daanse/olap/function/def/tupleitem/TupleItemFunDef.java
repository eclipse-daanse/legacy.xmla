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
package org.eclipse.daanse.olap.function.def.tupleitem;

import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.TupleCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.type.MemberType;

public class TupleItemFunDef extends AbstractFunctionDefinition {
    static OperationAtom functionAtom = new MethodOperationAtom("Item");

    static FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtom, "Returns a member from the tuple specified in <Tuple>. The member to be returned is specified by the zero-based position of the member in the set in <Index>.",
            "<TUPLE>.Item(<NUMERIC>)", DataType.MEMBER, new FunctionParameterR[] { new FunctionParameterR(DataType.TUPLE), new FunctionParameterR(DataType.NUMERIC) });

    static final TupleItemFunDef instance = new TupleItemFunDef();

    public TupleItemFunDef() {
        super(functionMetaData);
    }

    @Override
    public Type getResultType(Validator validator, Expression[] args) {
        // Suppose we are called as follows:
        //   ([Gender].CurrentMember, [Store].CurrentMember).Item(n)
        //
        // We know that our result is a member type, but we don't
        // know which dimension.
        return MemberType.Unknown;
    }

    @Override
    public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final Type type = call.getArg(0).getType();
        if (type instanceof MemberType) {
            final MemberCalc memberCalc =
                compiler.compileMember(call.getArg(0));
            final IntegerCalc indexCalc =
                compiler.compileInteger(call.getArg(1));
            return new TupleItemMemberTypeCalc(
                    call.getType(), memberCalc, indexCalc);
        } else {
            final TupleCalc tupleCalc =
                compiler.compileTuple(call.getArg(0));
            final IntegerCalc indexCalc =
                compiler.compileInteger(call.getArg(1));
            return new TupleItemCalc(
                    call.getType(), tupleCalc, indexCalc);
        }
    }
}
