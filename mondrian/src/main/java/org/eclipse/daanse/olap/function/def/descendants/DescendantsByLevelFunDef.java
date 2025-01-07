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
package org.eclipse.daanse.olap.function.def.descendants;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.EmptyType;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.LevelCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.mdx.HierarchyExpressionImpl;
import mondrian.mdx.ResolvedFunCallImpl;
import mondrian.mdx.UnresolvedFunCallImpl;
import mondrian.olap.MondrianException;
import mondrian.olap.Util;
import mondrian.olap.fun.FunUtil;

public class DescendantsByLevelFunDef extends AbstractFunctionDefinition {

    public static final String DESCENDANTS = "Descendants";

    private final static String descendantsAppliedToSetOfTuples = "Argument to Descendants function must be a member or set of members, not a set of tuples";
    private final static String cannotDeduceTypeOfSet = "Cannot deduce type of set";

    public DescendantsByLevelFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final Type type0 = call.getArg(0).getType();
        if (type0 instanceof SetType setType) {
            if (setType.getElementType() instanceof TupleType) {
                throw new MondrianException(descendantsAppliedToSetOfTuples);
            }

            MemberType memberType = (MemberType) setType.getElementType();
            final Hierarchy hierarchy = memberType.getHierarchy();
            if (hierarchy == null) {
                throw new MondrianException(cannotDeduceTypeOfSet);
            }
            // Convert
            // Descendants(<set>, <args>)
            // into
            // Generate(<set>, Descendants(<dimension>.CurrentMember, <args>))
            Expression[] descendantsArgs = call.getArgs().clone();
            descendantsArgs[0] = new UnresolvedFunCallImpl(new PlainPropertyOperationAtom("CurrentMember"),
                    new Expression[] { new HierarchyExpressionImpl(hierarchy) });
            final ResolvedFunCallImpl generateCall = (ResolvedFunCallImpl) compiler.getValidator().validate(
                    new UnresolvedFunCallImpl(new FunctionOperationAtom("Generate"), new Expression[] { call.getArg(0),
                            new UnresolvedFunCallImpl(new FunctionOperationAtom(DESCENDANTS), descendantsArgs) }),
                    false);
            return generateCall.accept(compiler);
        }

        final MemberCalc memberCalc = compiler.compileMember(call.getArg(0));
        Flag flag = Flag.SELF;
        if (call.getArgCount() == 1) {
            flag = Flag.SELF_BEFORE_AFTER;
        }
        final boolean depthSpecified = call.getArgCount() >= 2 && call.getArg(1).getType() instanceof NumericType;
        final boolean depthEmpty = call.getArgCount() >= 2 && call.getArg(1).getType() instanceof EmptyType;
        if (call.getArgCount() >= 3) {
            flag = FunUtil.getLiteralArg(call, 2, Flag.SELF, Flag.class);
        }

        if (call.getArgCount() >= 2 && depthEmpty && flag != Flag.LEAVES) {
            throw Util.newError("depth must be specified unless DESC_FLAG is LEAVES");
        }
        if ((depthSpecified || depthEmpty) && flag.leaves) {
            final IntegerCalc depthCalc = depthSpecified ? compiler.compileInteger(call.getArg(1)) : null;
            return new DescendantsLeavesByDepthCalc(call.getType(), memberCalc, depthCalc);
        } else if (depthSpecified) {
            final IntegerCalc depthCalc = compiler.compileInteger(call.getArg(1));
            final Flag flag1 = flag;
            return new DescendantsByDepthCalc(call.getType(), memberCalc, depthCalc, flag1);
        } else {
            final LevelCalc levelCalc = call.getArgCount() > 1 ? compiler.compileLevel(call.getArg(1)) : null;
            final Flag flag2 = flag;
            return new DescendantsCalc(call.getType(), memberCalc, levelCalc, flag2) {
            };
        }
    }
}
