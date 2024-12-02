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
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedMemberCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

/**
 * Definition of the <code>Lead</code> and <code>Lag</code> MDX functions.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class LeadLagFunDef extends AbstractFunctionDefinition {
    static final ReflectiveMultiResolver LagResolver =
        new ReflectiveMultiResolver(
            "Lag",
            "<Member>.Lag(<Numeric Expression>)",
            "Returns a member further along the specified member's dimension.",
            new String[]{"mmmn"},
            LeadLagFunDef.class);

    static final ReflectiveMultiResolver LeadResolver =
        new ReflectiveMultiResolver(
            "Lead",
            "<Member>.Lead(<Numeric Expression>)",
            "Returns a member further along the specified member's dimension.",
            new String[]{"mmmn"},
            LeadLagFunDef.class);

    public LeadLagFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final MemberCalc memberCalc =
                compiler.compileMember(call.getArg(0));
        final IntegerCalc integerCalc =
                compiler.compileInteger(call.getArg(1));
        final boolean lag = call.getFunDef().getFunctionMetaData().operationAtom().name().equals("Lag");
        return new AbstractProfilingNestedMemberCalc(
        		call.getType(),
            new Calc[] {memberCalc, integerCalc})
        {
            @Override
			public Member evaluate(Evaluator evaluator) {
                Member member = memberCalc.evaluate(evaluator);
                Integer n = integerCalc.evaluate(evaluator);
                if (lag) {
                    if (n == Integer.MIN_VALUE) {
                        // Bump up lagValue by one, otherwise -n (used
                        // in the getLeadMember call below) is out of
                        // range because Integer.MAX_VALUE ==
                        // -(Integer.MIN_VALUE + 1).
                        n += 1;
                    }

                    n = -n;
                }
                return evaluator.getSchemaReader().getLeadMember(member, n);
            }
        };
    }
}
