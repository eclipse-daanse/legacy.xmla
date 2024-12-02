/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import java.util.Locale;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Literal;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedStringCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.util.Format;

/**
 * Definition of the <code>Format</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class FormatFunDef extends AbstractFunctionDefinition {
    static final ReflectiveMultiResolver Resolver =
        new ReflectiveMultiResolver(
            "Format",
            "Format(<Expression>, <String Expression>)",
            "Formats a number or date to a string.",
            new String[] { "fSmS", "fSnS", "fSDS" },
            FormatFunDef.class);

    public FormatFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final Expression[] args = call.getArgs();
        final Calc calc = compiler.compileScalar(call.getArg(0), true);
        final Locale locale = compiler.getEvaluator().getConnectionLocale();
        if (args[1] instanceof Literal) {
            // Constant string expression: optimize by
            // compiling format string.
            String formatString = (String) ((Literal) args[1]).getValue();
            final Format format = new Format(formatString, locale);
            return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {calc}) {
                @Override
				public String evaluate(Evaluator evaluator) {
                    final Object o = calc.evaluate(evaluator);
                    return format.format(o);
                }
            };
        } else {
            // Variable string expression
            final StringCalc stringCalc =
                    compiler.compileString(call.getArg(1));
            return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {calc, stringCalc}) {
                @Override
				public String evaluate(Evaluator evaluator) {
                    final Object o = calc.evaluate(evaluator);
                    final String formatString =
                            stringCalc.evaluate(evaluator);
                    final Format format =
                            new Format(formatString, locale);
                    return format.format(o);
                }
            };
        }
    }
}
