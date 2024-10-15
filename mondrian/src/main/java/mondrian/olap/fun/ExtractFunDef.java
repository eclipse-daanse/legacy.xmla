/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.DimensionExpression;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.function.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.function.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.resolver.NoExpressionRequiredFunctionResolver;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.TupleCollections;
import mondrian.mdx.HierarchyExpressionImpl;
import mondrian.olap.Util;
import mondrian.olap.type.MemberType;
import mondrian.olap.type.SetType;
import mondrian.olap.type.TupleType;

/**
 * Definition of the <code>Extract</code> MDX function.
 *
 * <p>Syntax:
 * <blockquote><code>Extract(&lt;Set&gt;, &lt;Hierarchy&gt;[,
 * &lt;Hierarchy&gt;...])</code></blockquote>
 *
 * @author jhyde
 * @since Jun 10, 2007
 */
class ExtractFunDef extends AbstractFunctionDefinition {
	
	static OperationAtom functionAtom = new FunctionOperationAtom("Extract");

    static final NoExpressionRequiredFunctionResolver Resolver = new NoExpressionRequiredFunctionResolver()
    {
        @Override
		public FunctionDefinition resolve(
            Expression[] args,
            Validator validator,
            List<Conversion> conversions)
        {
            if (args.length < 2) {
                return null;
            }
            if (!validator.canConvert(0, args[0], DataType.SET, conversions)) {
                return null;
            }
            for (int i = 1; i < args.length; ++i) {
                if (!validator.canConvert(
                        0, args[i], DataType.HIERARCHY, conversions))
                {
                    return null;
                }
            }

            // Find the dimensionality of the set expression.

            // Form a list of ordinals of the hierarchies being extracted.
            // For example, in
            //   Extract(X.Members * Y.Members * Z.Members, Z, X)
            // the hierarchy ordinals are X=0, Y=1, Z=2, and the extracted
            // ordinals are {2, 0}.
            //
            // Each hierarchy extracted must exist in the LHS,
            // and no hierarchy may be extracted more than once.
            List<Integer> extractedOrdinals = new ArrayList<>();
            final List<Hierarchy> extractedHierarchies =
                new ArrayList<>();
            ExtractFunDef.findExtractedHierarchies(
                args, extractedHierarchies, extractedOrdinals);
            DataType[] parameterTypes = new DataType[args.length];
            parameterTypes[0] = DataType.SET;
            Arrays.fill(
                parameterTypes, 1, parameterTypes.length, DataType.HIERARCHY);
            

			FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtom,
					"Returns a set of tuples from extracted hierarchy elements. The opposite of Crossjoin.",
					"Extract(<Set>, <Hierarchy>[, <Hierarchy>...])",  DataType.SET, parameterTypes);
            return new ExtractFunDef(functionMetaData);
        }

		@Override
		public OperationAtom getFunctionAtom() {
			return functionAtom;
		}
    };

    private ExtractFunDef(
    		FunctionMetaData functionMetaData)
    {
        super(functionMetaData);
    }

    @Override
	public Type getResultType(Validator validator, Expression[] args) {
        final List<Hierarchy> extractedHierarchies =
            new ArrayList<>();
        final List<Integer> extractedOrdinals = new ArrayList<>();
        ExtractFunDef.findExtractedHierarchies(args, extractedHierarchies, extractedOrdinals);
        if (extractedHierarchies.size() == 1) {
            return new SetType(
                MemberType.forHierarchy(
                    extractedHierarchies.get(0)));
        } else {
            List<Type> typeList = new ArrayList<>();
            for (Hierarchy extractedHierarchy : extractedHierarchies) {
                typeList.add(
                    MemberType.forHierarchy(
                        extractedHierarchy));
            }
            return new SetType(
                new TupleType(
                    typeList.toArray(new Type[typeList.size()])));
        }
    }

    private static void findExtractedHierarchies(
        Expression[] args,
        List<Hierarchy> extractedHierarchies,
        List<Integer> extractedOrdinals)
    {
        SetType type = (SetType) args[0].getType();
        final List<Hierarchy> hierarchies;
        if (type.getElementType() instanceof TupleType tupleType) {
            hierarchies = tupleType.getHierarchies();
        } else {
            hierarchies = Collections.singletonList(type.getHierarchy());
        }
        for (Hierarchy hierarchy : hierarchies) {
            if (hierarchy == null) {
                throw new FunctionException(
                    "hierarchy of argument not known");
            }
        }

        for (int i = 1; i < args.length; i++) {
            Expression arg = args[i];
            Hierarchy extractedHierarchy = null;
            if (arg instanceof HierarchyExpressionImpl hierarchyExpr) {
                extractedHierarchy = hierarchyExpr.getHierarchy();
            } else if (arg instanceof DimensionExpression dimensionExpr) {
                extractedHierarchy =
                    dimensionExpr.getDimension().getHierarchy();
            }
            if (extractedHierarchy == null) {
                throw new FunctionException("not a constant hierarchy: " + arg);
            }
            int ordinal = hierarchies.indexOf(extractedHierarchy);
            if (ordinal == -1) {
                throw new FunctionException(
                    new StringBuilder("hierarchy ")
                    .append(extractedHierarchy.getUniqueName())
                    .append(" is not a hierarchy of the expression ").append(args[0]).toString());
            }
            if (extractedOrdinals.indexOf(ordinal) >= 0) {
                throw new FunctionException(
                    new StringBuilder("hierarchy ")
                    .append(extractedHierarchy.getUniqueName())
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
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        List<Hierarchy> extractedHierarchyList = new ArrayList<>();
        List<Integer> extractedOrdinalList = new ArrayList<>();
        ExtractFunDef.findExtractedHierarchies(
            call.getArgs(),
            extractedHierarchyList,
            extractedOrdinalList);
        Util.assertTrue(
            extractedOrdinalList.size() == extractedHierarchyList.size());
        Expression arg = call.getArg(0);
        final TupleListCalc tupleListCalc = compiler.compileList(arg, false);
        int inArity = arg.getType().getArity();
        final int outArity = extractedOrdinalList.size();
        if (inArity == 1) {
            // LHS is a set of members, RHS is the same hierarchy. Extract boils
            // down to eliminating duplicate members.
            Util.assertTrue(outArity == 1);
            return new DistinctFunDef.CalcImpl(call, tupleListCalc);
        }
        final int[] extractedOrdinals = ExtractFunDef.toIntArray(extractedOrdinalList);
        return new AbstractListCalc(call.getType(), new Calc[]{tupleListCalc}) {
            @Override
			public TupleList evaluateList(Evaluator evaluator) {
                TupleList result = TupleCollections.createList(outArity);
                TupleList list = tupleListCalc.evaluateList(evaluator);
                Set<List<Member>> emittedTuples = new HashSet<>();
                for (List<Member> members : list.project(extractedOrdinals)) {
                    if (emittedTuples.add(members)) {
                        result.add(members);
                    }
                }
                return result;
            }
        };
    }
}
