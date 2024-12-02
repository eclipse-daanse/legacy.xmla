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
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDimensionCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

/**
 * Definition of the <code>&lt;Measure&gt;.Dimension</code>
 * MDX builtin function.
 *
 * @author jhyde
 * @since Jul 20, 2009
 */
class MemberDimensionFunDef extends AbstractFunctionDefinition {
    public static final AbstractFunctionDefinition INSTANCE = new MemberDimensionFunDef();

    private MemberDimensionFunDef() {
        super(
            "Dimension",
            "Returns the dimension that contains a specified member.", "pdm");
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler)
    {
        final MemberCalc memberCalc =
            compiler.compileMember(call.getArg(0));
        return new AbstractProfilingNestedDimensionCalc(call.getType(), new Calc[] {memberCalc})
        {
            @Override
			public Dimension evaluate(Evaluator evaluator) {
                Member member = memberCalc.evaluate(evaluator);
                return member.getDimension();
            }
        };
    }
}
