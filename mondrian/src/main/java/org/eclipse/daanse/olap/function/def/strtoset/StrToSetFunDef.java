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
package org.eclipse.daanse.olap.function.def.strtoset;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.DimensionExpression;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.util.type.TypeUtil;

import mondrian.mdx.HierarchyExpressionImpl;
import mondrian.olap.exceptions.ArgumentFunctionNotHierarchyException;
import mondrian.olap.exceptions.MdxFuncArgumentsNumException;

public class StrToSetFunDef extends AbstractFunctionDefinition {

    private static final String NAME = "StrToSet";
    static OperationAtom functionAtom = new FunctionOperationAtom(NAME);

    public StrToSetFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final StringCalc stringCalc = compiler.compileString(call.getArg(0));
        SetType type = (SetType) call.getType();
        Type elementType = type.getElementType();
        if (elementType instanceof MemberType) {
            final Hierarchy hierarchy = elementType.getHierarchy();
            return new StrToSetHierarchyCalc(call.getType(), stringCalc, hierarchy);
        } else {
            TupleType tupleType = (TupleType) elementType;
            final List<Hierarchy> hierarchyList = tupleType.getHierarchies();
            return new StrToSetHierarchyListCalc(call.getType(), stringCalc, hierarchyList);
        }
    }

    @Override
    public Expression createCall(Validator validator, Expression[] args) {
        final int argCount = args.length;
        if (argCount <= 1) {
            throw new MdxFuncArgumentsNumException( getFunctionMetaData().operationAtom().name() );
        }
        for (int i = 1; i < argCount; i++) {
            final Expression arg = args[i];
            if (arg instanceof DimensionExpression dimensionExpr) {
                Dimension dimension = dimensionExpr.getDimension();
                args[i] = new HierarchyExpressionImpl(dimension.getHierarchy());
            } else if (arg instanceof HierarchyExpressionImpl) {
                // nothing
            } else {
                throw new ArgumentFunctionNotHierarchyException(
                    i + 1, getFunctionMetaData().operationAtom().name());
            }
        }
        return super.createCall(validator, args);
    }

    @Override
    public Type getResultType(Validator validator, Expression[] args) {
        switch (args.length) {
        case 1:
            // This is a call to the standard version of StrToSet,
            // which doesn't give us any hints about type.
            return new SetType(null);

        case 2:
        {
            final Type argType = args[1].getType();
            return new SetType(
                new MemberType(
                    argType.getDimension(),
                    argType.getHierarchy(),
                    argType.getLevel(),
                    null));
        }

        default:
        {
            // This is a call to Mondrian's extended version of
            // StrToSet, of the form
            //   StrToSet(s, <Hier1>, ... , <HierN>)
            //
            // The result is a set of tuples
            //  (<Hier1>, ... ,  <HierN>)
            final List<MemberType> list = new ArrayList<>();
            for (int i = 1; i < args.length; i++) {
                Expression arg = args[i];
                final Type argType = arg.getType();
                list.add(TypeUtil.toMemberType(argType));
            }
            final MemberType[] types =
                list.toArray(new MemberType[list.size()]);
            TupleType.checkHierarchies(types);
            return new SetType(new TupleType(types));
        }
        }
    }
}
