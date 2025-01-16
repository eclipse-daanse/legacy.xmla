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
package org.eclipse.daanse.olap.function.def.generate;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.StringType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleIteratorCalc;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.constant.ConstantStringCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.util.type.TypeUtil;

import mondrian.olap.fun.FunUtil;

public class GenerateFunDef extends AbstractFunctionDefinition {

        static final List<String> ReservedWords = List.of("ALL");

        public GenerateFunDef(FunctionMetaData functionMetaData) {
            super(functionMetaData);
        }

        @Override
        public Type getResultType(Validator validator, Expression[] args) {
            final Type type = args[1].getType();
            if (type instanceof StringType || type instanceof NumericType) {
                // Generate(<Set>, <String>[, <String>])
                return StringType.INSTANCE;
            } else {
                final Type memberType = TypeUtil.toMemberOrTupleType(type);
                return new SetType(memberType);
            }
        }

        @Override
        public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
            final TupleIteratorCalc tupleIteratorCalc = compiler.compileIter(call.getArg(0));
            if (call.getArg(1).getType() instanceof StringType
                    || call.getArg(1).getType() instanceof NumericType) {
                final StringCalc stringCalc;
                if(call.getArg(1).getType() instanceof StringType) {
                    stringCalc = compiler.compileString(call.getArg(1));
                } else {
                    //NumericType
                    mondrian.mdx.UnresolvedFunCallImpl unresolvedFunCall = new mondrian.mdx.UnresolvedFunCallImpl(
                            new FunctionOperationAtom("str"),
                            new Expression[] {call.getArg(1)});
                    stringCalc = compiler.compileString(unresolvedFunCall.accept(compiler.getValidator()));
                }
                final StringCalc delimCalc;
                if (call.getArgCount() == 3) {
                    delimCalc = compiler.compileString(call.getArg(2));
                } else {
                    delimCalc = new ConstantStringCalc(StringType.INSTANCE, "");
                }

                return new GenerateStringCalc(
                      call.getType(), tupleIteratorCalc, stringCalc, delimCalc);
            } else {
                final TupleListCalc listCalc2 =
                    compiler.compileList(call.getArg(1));
                final String literalArg = FunUtil.getLiteralArg(call, 2, "", GenerateFunDef.ReservedWords);
                final boolean all = literalArg.equalsIgnoreCase("ALL");
                final int arityOut = call.getType().getArity();
                return new GenerateListCalc(
                    call.getType(), tupleIteratorCalc, listCalc2, arityOut, all);
            }
        }

    }
