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
package org.eclipse.daanse.olap.function.def.set.extract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.query.component.DimensionExpression;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.function.def.set.distinct.DistinctCalc;

import mondrian.mdx.HierarchyExpressionImpl;
import mondrian.olap.Util;
import mondrian.olap.fun.FunctionException;

public class ExtractFunDef extends AbstractFunctionDefinition {

    static OperationAtom functionAtom = new FunctionOperationAtom("Extract");

    public ExtractFunDef(FunctionParameterR[] parameterTypes) {
        super(new FunctionMetaDataR(functionAtom,
                "Returns a set of tuples from extracted hierarchy elements. The opposite of Crossjoin.",
                "Extract(<Set>, <Hierarchy>[, <Hierarchy>...])", DataType.SET, parameterTypes));
    }

    @Override
    public Type getResultType(Validator validator, Expression[] args) {
        final List<Hierarchy> extractedHierarchies = new ArrayList<>();
        final List<Integer> extractedOrdinals = new ArrayList<>();
        findExtractedHierarchies(args, extractedHierarchies, extractedOrdinals);
        if (extractedHierarchies.size() == 1) {
            return new SetType(MemberType.forHierarchy(extractedHierarchies.get(0)));
        } else {
            List<Type> typeList = new ArrayList<>();
            for (Hierarchy extractedHierarchy : extractedHierarchies) {
                typeList.add(MemberType.forHierarchy(extractedHierarchy));
            }
            return new SetType(new TupleType(typeList.toArray(new Type[typeList.size()])));
        }
    }

    public static void findExtractedHierarchies(Expression[] args, List<Hierarchy> extractedHierarchies,
            List<Integer> extractedOrdinals) {
        SetType type = (SetType) args[0].getType();
        final List<Hierarchy> hierarchies;
        if (type.getElementType() instanceof TupleType tupleType) {
            hierarchies = tupleType.getHierarchies();
        } else {
            hierarchies = Collections.singletonList(type.getHierarchy());
        }
        for (Hierarchy hierarchy : hierarchies) {
            if (hierarchy == null) {
                throw new FunctionException("hierarchy of argument not known");
            }
        }

        for (int i = 1; i < args.length; i++) {
            Expression arg = args[i];
            Hierarchy extractedHierarchy = null;
            if (arg instanceof HierarchyExpressionImpl hierarchyExpr) {
                extractedHierarchy = hierarchyExpr.getHierarchy();
            } else if (arg instanceof DimensionExpression dimensionExpr) {
                extractedHierarchy = dimensionExpr.getDimension().getHierarchy();
            }
            if (extractedHierarchy == null) {
                throw new FunctionException("not a constant hierarchy: " + arg);
            }
            int ordinal = hierarchies.indexOf(extractedHierarchy);
            if (ordinal == -1) {
                throw new FunctionException(new StringBuilder("hierarchy ").append(extractedHierarchy.getUniqueName())
                        .append(" is not a hierarchy of the expression ").append(args[0]).toString());
            }
            if (extractedOrdinals.indexOf(ordinal) >= 0) {
                throw new FunctionException(new StringBuilder("hierarchy ").append(extractedHierarchy.getUniqueName())
                        .append(" is extracted more than once").toString());
            }
            extractedOrdinals.add(ordinal);
            extractedHierarchies.add(extractedHierarchy);
        }
    }

    private static int[] toIntArray(List<Integer> integerList) {
        final int[] ints = new int[integerList.size()];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = integerList.get(i);
        }
        return ints;
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        List<Hierarchy> extractedHierarchyList = new ArrayList<>();
        List<Integer> extractedOrdinalList = new ArrayList<>();
        ExtractFunDef.findExtractedHierarchies(call.getArgs(), extractedHierarchyList, extractedOrdinalList);
        Util.assertTrue(extractedOrdinalList.size() == extractedHierarchyList.size());
        Expression arg = call.getArg(0);
        final TupleListCalc tupleListCalc = compiler.compileList(arg, false);
        int inArity = arg.getType().getArity();
        final int outArity = extractedOrdinalList.size();
        if (inArity == 1) {
            // LHS is a set of members, RHS is the same hierarchy. Extract boils
            // down to eliminating duplicate members.
            Util.assertTrue(outArity == 1);
            return new DistinctCalc(call, tupleListCalc);
        }
        final int[] extractedOrdinals = ExtractFunDef.toIntArray(extractedOrdinalList);
        return new ExtractCalc(call.getType(), outArity, extractedOrdinals, tupleListCalc);
    }
}
