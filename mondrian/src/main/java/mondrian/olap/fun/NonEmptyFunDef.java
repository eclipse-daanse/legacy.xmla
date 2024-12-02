/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2021 Sergei Semenkov
// All Rights Reserved.
*/
package mondrian.olap.fun;

import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.util.HirarchyDependsChecker;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.TupleCollections;

class NonEmptyFunDef extends AbstractFunctionDefinition {
    private static final Logger LOGGER = LoggerFactory.getLogger( NonEmptyFunDef.class );
    static final ReflectiveMultiResolver Resolver =
            new ReflectiveMultiResolver(
                    "NonEmpty",
                    "NonEmpty(<Set1>[, <Set2>])",
                    "Returns the set of tuples that are not empty from a specified set, based on the cross product " +
                            "of the specified set with a second set.",
                    new String[] {"fxx", "fxxx"},
                    NonEmptyFunDef.class);

    public NonEmptyFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
	public Type getResultType(Validator validator, Expression[] args) {
        return args[0].getType();
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc listCalc1 = compiler.compileList(call.getArg(0));
        TupleListCalc listCalc2 = null;
        if(call.getArgCount() == 2) {
            listCalc2 = compiler.compileList(call.getArg(1));
        }

        return new NonEmptyListCalcImpl(call, listCalc1, listCalc2);
    }

    private static class NonEmptyListCalcImpl extends AbstractListCalc {
        private final TupleListCalc listCalc1;
        private final TupleListCalc listCalc2;

        public NonEmptyListCalcImpl(
                ResolvedFunCall call,
                TupleListCalc listCalc1,
                TupleListCalc listCalc2)
        {
            super(call.getType(), new Calc[]{listCalc1, listCalc2});
            this.listCalc1 = listCalc1;
            this.listCalc2 = listCalc2;
        }

        @Override
		public TupleList evaluateList(Evaluator evaluator) {
            final int savepoint = evaluator.savepoint();
            try {
                evaluator.setNonEmpty(true);

                TupleList leftTuples = listCalc1.evaluateList(evaluator);
                if (leftTuples.isEmpty()) {
                    return TupleCollections.emptyList(leftTuples.getArity());
                }
                TupleList rightTuples = null;
                if(this.listCalc2 != null) {
                    rightTuples = listCalc2.evaluateList(evaluator);
                }
                TupleList result =
                        TupleCollections.createList(leftTuples.getArity());


                for (List<Member> leftTuple : leftTuples) {
                    if(rightTuples != null && !rightTuples.isEmpty()) {
                        for (List<Member> rightTuple : rightTuples) {
                            evaluator.setContext(leftTuple);
                            evaluator.setContext(rightTuple);
                            Object tupleResult = evaluator.evaluateCurrent();
                            if (tupleResult != null)
                            {
                                result.add(leftTuple);
                                break;
                            }
                        }
                    }
                    else {
                        evaluator.setContext(leftTuple);
                        Object tupleResult = evaluator.evaluateCurrent();
                        if (tupleResult != null)
                        {
                            result.add(leftTuple);
                        }
                    }
                }
                return result;
            } finally {
                evaluator.restore(savepoint);
            }
        }

        @Override
		public boolean dependsOn(Hierarchy hierarchy) {
            return HirarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy);
        }
    }
}

