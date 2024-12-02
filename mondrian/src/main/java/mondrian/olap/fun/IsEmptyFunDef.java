/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedBooleanCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

/**
 * Definition of the <code>IsEmpty</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class IsEmptyFunDef extends AbstractFunctionDefinition {
    static final ReflectiveMultiResolver FunctionResolver =
        new ReflectiveMultiResolver(
            "IsEmpty",
            "IsEmpty(<Value Expression>)",
            "Determines if an expression evaluates to the empty cell value.",
            new String[] {"fbS", "fbn"},
            IsEmptyFunDef.class);

    static final ReflectiveMultiResolver PostfixResolver =
        new ReflectiveMultiResolver(
            "IS EMPTY",
            "<Value Expression> IS EMPTY",
            "Determines if an expression evaluates to the empty cell value.",
            new String[] {"Qbm", "Qbt"},
            IsEmptyFunDef.class);

    public IsEmptyFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final Calc calc = compiler.compileScalar(call.getArg(0), true);
        return new AbstractProfilingNestedBooleanCalc(call.getType(), new Calc[] {calc}) {
            @Override
			public Boolean evaluate(Evaluator evaluator) {
                Object o = calc.evaluate(evaluator);
                return o == null;
            }
        };
    }
}
