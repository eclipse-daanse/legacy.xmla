/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

/**
 * Definition of the <code>Unorder</code> MDX function.
 *
 * @author jhyde
 * @since Sep 06, 2008
 */
class UnorderFunDef extends AbstractFunctionDefinition {

    static final ReflectiveMultiResolver Resolver =
        new ReflectiveMultiResolver(
            "Unorder",
            "Unorder(<Set>)",
            "Removes any enforced ordering from a specified set.",
            new String[]{"fxx"},
            UnorderFunDef.class);

    public UnorderFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        // Currently Unorder has no effect. In future, we may use the function
        // as a marker to weaken the ordering required from an expression and
        // therefore allow the compiler to use a more efficient implementation
        // that does not return a strict order.
        return compiler.compile(call.getArg(0));
    }
}
