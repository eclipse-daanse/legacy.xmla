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
package org.eclipse.daanse.olap.function.def.set;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.TupleCalc;
import org.eclipse.daanse.olap.calc.api.VoidCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedVoidCalc;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.TupleCollections;
import mondrian.olap.fun.FunUtil;
import mondrian.olap.type.MemberType;
import mondrian.olap.type.SetType;

/**
 * Compiled expression to implement the MDX set function, <code>{ ...
 * }</code>, applied to a set of tuples, as a list.
 *
 * <p>The set function can contain expressions which yield sets together
 * with expressions which yield individual tuples, provided that
 * they all have the same type. It automatically removes null
 * or partially-null tuples from the list.
 *
 * <p>Also, does not process high-cardinality dimensions specially.
 */
public class SetListCalc  extends AbstractListCalc {
    private TupleList result;
    private final VoidCalc[] voidCalcs;

    public SetListCalc(
        Type type,
        Expression[] args,
        ExpressionCompiler compiler,
        List<ResultStyle> resultStyles)
    {
        super(type, null);
        voidCalcs = compileSelf(args, compiler, resultStyles);
        result = TupleCollections.createList(getType().getArity());
    }

    @Override
    public Calc<?>[] getChildCalcs() {
        return voidCalcs;
    }

    private VoidCalc[] compileSelf(
        Expression[] args,
        ExpressionCompiler compiler,
        List<ResultStyle> resultStyles)
    {
        VoidCalc[] voidCalcs = new VoidCalc[args.length];
        for (int i = 0; i < args.length; i++) {
            voidCalcs[i] = createCalc(args[i], compiler, resultStyles);
        }
        return voidCalcs;
    }

    private VoidCalc createCalc(
        Expression arg,
        ExpressionCompiler compiler,
        List<ResultStyle> resultStyles)
    {
        final Type type = arg.getType();
        if (type instanceof SetType) {
            // TODO use resultStyles
            final TupleListCalc tupleListCalc = compiler.compileList(arg);
            return new AbstractProfilingNestedVoidCalc(type, new Calc[] {tupleListCalc}) {
                // name "Sublist..."
                @Override
                public Void evaluate(Evaluator evaluator) {
                    TupleList list =
                        tupleListCalc.evaluateList(evaluator);
                    // Add only tuples which are not null. Tuples with
                    // any null members are considered null.
                    outer:
                    for (List<Member> members : list) {
                        for (Member member : members) {
                            if (member == null || member.isNull()) {
                                continue outer;
                            }
                        }
                        result.add(members);
                    }
                    return null;
                }

            };
        } else if (type instanceof mondrian.olap.type.LevelType) {
            mondrian.mdx.UnresolvedFunCallImpl unresolvedFunCall = new mondrian.mdx.UnresolvedFunCallImpl(
                    new PlainPropertyOperationAtom("Members"),
                    new Expression[] {arg});
            final TupleListCalc tupleListCalc = compiler.compileList(unresolvedFunCall.accept(compiler.getValidator()));
            return new AbstractProfilingNestedVoidCalc(type, new Calc[] {tupleListCalc}) {
                @Override
                public Void evaluate(Evaluator evaluator) {
                    TupleList list =
                            tupleListCalc.evaluateList(evaluator);
                    result = list;
                    return null;
                }
            };
        } else if (type.getArity() == 1 && arg instanceof MemberType) {
            final MemberCalc memberCalc = compiler.compileMember(arg);
            return new AbstractProfilingNestedVoidCalc(type, new Calc[]{memberCalc}) {
                final Member[] members = {null};
                @Override
                public Void evaluate(Evaluator evaluator) {
                    // Don't add null or partially null tuple to result.
                    Member member = memberCalc.evaluate(evaluator);
                    if (member == null || member.isNull()) {
                        return null;
                    }
                    members[0] = member;
                    result.addTuple(members);
                    return null;
                }
            };
        } else {
            final TupleCalc tupleCalc = compiler.compileTuple(arg);
            return new AbstractProfilingNestedVoidCalc(type, new Calc[]{tupleCalc}) {
                @Override
                public Void evaluate(Evaluator evaluator) {
                    // Don't add null or partially null tuple to result.
                    Member[] members = tupleCalc.evaluate(evaluator);
                    if (members == null
                        || FunUtil.tupleContainsNullMember(members))
                    {
                        return null;
                    }
                    result.addTuple(members);
                    return null;
                }
            };
        }
    }

    @Override
    public TupleList evaluateList(final Evaluator evaluator) {
        result.clear();
        for (VoidCalc voidCalc : voidCalcs) {
            voidCalc.evaluate(evaluator);
        }
        return result.copyList(-1);
    }
}
