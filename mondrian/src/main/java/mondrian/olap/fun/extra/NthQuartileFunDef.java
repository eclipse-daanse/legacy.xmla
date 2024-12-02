/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun.extra;

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
import org.eclipse.daanse.olap.calc.base.value.CurrentValueDoubleCalc;
import org.eclipse.daanse.olap.function.def.aggregate.AbstractAggregateFunDef;

import mondrian.olap.fun.FunUtil;
import mondrian.olap.fun.MultiResolver;
import mondrian.olap.fun.ReflectiveMultiResolver;

/**
 * Definition of the <code>FirstQ</code> and <code>ThirdQ</code> MDX extension
 * functions.
 *
 * <p>These functions are not standard MDX.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
public class NthQuartileFunDef extends AbstractAggregateFunDef {
    private final int range;

    public static final MultiResolver ThirdQResolver =
        new ReflectiveMultiResolver(
            "ThirdQ",
            "ThirdQ(<Set>[, <Numeric Expression>])",
            "Returns the 3rd quartile value of a numeric expression evaluated over a set.",
            new String[]{"fnx", "fnxn"},
        NthQuartileFunDef.class);

    public static final MultiResolver FirstQResolver =
        new ReflectiveMultiResolver(
            "FirstQ",
            "FirstQ(<Set>[, <Numeric Expression>])",
            "Returns the 1st quartile value of a numeric expression evaluated over a set.",
            new String[]{"fnx", "fnxn"},
            NthQuartileFunDef.class);

    public NthQuartileFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
        this.range = functionMetaData.operationAtom().name().equals("FirstQ") ? 1 : 3;
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc =
            compiler.compileList(call.getArg(0));
        final DoubleCalc doubleCalc =
            call.getArgCount() > 1
            ? compiler.compileDouble(call.getArg(1))
            : new CurrentValueDoubleCalc(call.getType());
        return new AbstractProfilingNestedDoubleCalc(call.getType(), new Calc[] {tupleListCalc, doubleCalc}) {
            @Override
			public Double evaluate(Evaluator evaluator) {
                final int savepoint = evaluator.savepoint();
                try {
                    evaluator.setNonEmpty(false);
                    TupleList members =
                        evaluateCurrentList(tupleListCalc, evaluator);
                    return
                        FunUtil.quartile(
                            evaluator, members, doubleCalc, range);
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
