/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.olap.fun;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;
import org.eclipse.daanse.olap.calc.base.util.HirarchyDependsChecker;
import org.eclipse.daanse.olap.function.def.aggregate.AbstractAggregateFunDef;

/**
 * Definition of the <code>Percentile</code> MDX function.
 * <p>There is some discussion about what the "right" percentile function is.
 * Here is a <a href="http://cnx.org/content/m10805/latest/">good overview</a>.
 * Wikipedia also lists
 * <a href="http://en.wikipedia.org/wiki/Percentile">
 * methods of calculating percentile</a>.
 * </p>
 * <p>
 * This class based on MS Excel formulae:
 * </p>
 * <blockquote>rank = P / 100 * (N - 1) + 1</blockquote>
 * <blockquote>percentile = A[n]+d*(A[n+1]-A[n])</blockquote>
 * <p>Definition can also be found on
 * <a href="http://en.wikipedia.org/wiki/Percentile">Wikipedia</a></p>
 */
class PercentileFunDef extends AbstractAggregateFunDef {
    static final ReflectiveMultiResolver Resolver =
        new ReflectiveMultiResolver(
            "Percentile",
            "Percentile(<Set>, <Numeric Expression>, <Percent>)",
            "Returns the value of the tuple that is at a given percentile of a set.",
            new String[] {"fnxnn"},
            PercentileFunDef.class);

    public PercentileFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc =
            compiler.compileList(call.getArg(0));
        final Calc calc =
            compiler.compileScalar(call.getArg(1), true);
        final DoubleCalc percentCalc =
            compiler.compileDouble(call.getArg(2));
        return new AbstractProfilingNestedDoubleCalc(
        		call.getType(), new Calc[] {tupleListCalc, calc, percentCalc})
        {
            @Override
			public Double evaluate(Evaluator evaluator) {
                TupleList list = AbstractAggregateFunDef.evaluateCurrentList(tupleListCalc, evaluator);
                Double percent = percentCalc.evaluate(evaluator) * 0.01;
                final int savepoint = evaluator.savepoint();
                try {
                    evaluator.setNonEmpty(false);
                    final Double percentile =
                        FunUtil.percentile(evaluator, list, calc, percent);
                    return percentile;
                } finally {
                    evaluator.restore(savepoint);
                }
            }

            @Override
			public boolean dependsOn(Hierarchy hierarchy) {
                return HirarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy);
            }
        };
    }
}
