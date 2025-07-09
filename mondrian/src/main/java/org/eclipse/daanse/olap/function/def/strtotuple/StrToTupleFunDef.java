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
package org.eclipse.daanse.olap.function.def.strtotuple;

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
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.exceptions.ArgumentFunctionNotHierarchyException;
import org.eclipse.daanse.olap.exceptions.MdxFuncArgumentsNumException;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.query.component.HierarchyExpressionImpl;
import org.eclipse.daanse.olap.util.type.TypeUtil;

public class StrToTupleFunDef  extends AbstractFunctionDefinition {

    private static final String NAME = "StrToTuple";
    static OperationAtom functionAtom = new FunctionOperationAtom(NAME);

    public StrToTupleFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final StringCalc stringCalc = compiler.compileString(call.getArg(0));
        Type elementType = call.getType();
        if (elementType instanceof MemberType) {
            final Hierarchy hierarchy = elementType.getHierarchy();
            return new StrToTupleMemberTypeCalc(call.getType(), stringCalc, hierarchy) {
            };
        } else {
            TupleType tupleType = (TupleType) elementType;
            final List<Hierarchy> hierarchies = tupleType.getHierarchies();
            return new StrToTupleCalc(call.getType(), stringCalc, hierarchies);
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
            // This is a call to the standard version of StrToTuple,
            // which doesn't give us any hints about type.
            return new TupleType(null);

        case 2:
            final Type argType = args[1].getType();
            return new MemberType(
                argType.getDimension(),
                argType.getHierarchy(),
                argType.getLevel(),
                null);

        default: {
            // This is a call to Mondrian's extended version of
            // StrToTuple, of the form
            //   StrToTuple(s, <Hier1>, ... , <HierN>)
            //
            // The result is a tuple
            //  (<Hier1>, ... ,  <HierN>)
            final List<MemberType> list = new ArrayList<>();
            for (int i = 1; i < args.length; i++) {
                Expression arg = args[i];
                final Type type = arg.getType();
                list.add(TypeUtil.toMemberType(type));
            }
            final MemberType[] types =
                list.toArray(new MemberType[list.size()]);
            TupleType.checkHierarchies(types);
            return new TupleType(types);
        }
        }
    }
}
