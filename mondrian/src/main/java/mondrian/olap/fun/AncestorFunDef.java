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
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.LevelCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedMemberCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.type.LevelType;

/**
 * Definition of the <code>Ancestor</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class AncestorFunDef extends AbstractFunctionDefinition {
    static final ReflectiveMultiResolver Resolver =
        new ReflectiveMultiResolver(
            "Ancestor",
            "Ancestor(<Member>, {<Level>|<Numeric Expression>})",
            "Returns the ancestor of a member at a specified level.",
            new String[] {"fmml", "fmmn"},
            AncestorFunDef.class);

    public AncestorFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
	public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final MemberCalc memberCalc =
            compiler.compileMember(call.getArg(0));
        final Type type1 = call.getArg(1).getType();
        if (type1 instanceof LevelType) {
            final LevelCalc levelCalc =
                compiler.compileLevel(call.getArg(1));
            return new AbstractProfilingNestedMemberCalc(
                call.getType(), new Calc[] {memberCalc, levelCalc})
            {
                @Override
				public Member evaluate(Evaluator evaluator) {
                    Level level = levelCalc.evaluate(evaluator);
                    Member member = memberCalc.evaluate(evaluator);
                    int distance =
                        member.getLevel().getDepth() - level.getDepth();
                    return FunUtil.ancestor(evaluator, member, distance, level);
                }
            };
        } else {
            final IntegerCalc distanceCalc =
                compiler.compileInteger(call.getArg(1));
            return new AbstractProfilingNestedMemberCalc(
            		call.getType(), new Calc[] {memberCalc, distanceCalc})
            {
                @Override
				public Member evaluate(Evaluator evaluator) {
                    Integer distance = distanceCalc.evaluate(evaluator);
                    Member member = memberCalc.evaluate(evaluator);
                    return FunUtil.ancestor(evaluator, member, distance, null);
                }
            };
        }
    }
}
