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
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.TupleCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedMemberCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.type.MemberType;
import mondrian.olap.type.TupleType;

/**
 * Definition of the <code>&lt;Tuple&gt;.Item</code> MDX function.
 *
 * <p>Syntax:
 * <blockquote><code>
 * &lt;Tuple&gt;.Item(&lt;Index&gt;)<br/>
 * </code></blockquote>
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class TupleItemFunDef extends AbstractFunctionDefinition {
    static final TupleItemFunDef instance = new TupleItemFunDef();

    private TupleItemFunDef() {
        super(
            "Item",
            "Returns a member from the tuple specified in <Tuple>. The member to be returned is specified by the zero-based position of the member in the set in <Index>.",
            "mmtn");
    }

    @Override
	public Type getResultType(Validator validator, Expression[] args) {
        // Suppose we are called as follows:
        //   ([Gender].CurrentMember, [Store].CurrentMember).Item(n)
        //
        // We know that our result is a member type, but we don't
        // know which dimension.
        return MemberType.Unknown;
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final Type type = call.getArg(0).getType();
        if (type instanceof MemberType) {
            final MemberCalc memberCalc =
                compiler.compileMember(call.getArg(0));
            final IntegerCalc indexCalc =
                compiler.compileInteger(call.getArg(1));
            return new AbstractProfilingNestedMemberCalc(
            		call.getType(), new Calc[] {memberCalc, indexCalc})
            {
                @Override
				public Member evaluate(Evaluator evaluator) {
                    final Member member =
                            memberCalc.evaluate(evaluator);
                    final Integer index =
                            indexCalc.evaluate(evaluator);
                    if (index != 0) {
                        return null;
                    }
                    return member;
                }
            };
        } else {
            final TupleCalc tupleCalc =
                compiler.compileTuple(call.getArg(0));
            final IntegerCalc indexCalc =
                compiler.compileInteger(call.getArg(1));
            return new AbstractProfilingNestedMemberCalc(
            		call.getType(), new Calc[] {tupleCalc, indexCalc})
            {
                final Member[] nullTupleMembers =
                        FunUtil.makeNullTuple((TupleType) tupleCalc.getType());
                @Override
				public Member evaluate(Evaluator evaluator) {
                    final Member[] members =
                            tupleCalc.evaluate(evaluator);
                    assert members == null
                        || members.length == nullTupleMembers.length;
                    final Integer index = indexCalc.evaluate(evaluator);
                    if (members == null) {
                        return nullTupleMembers[index];
                    }
                    if (index >= members.length || index < 0) {
                        return null;
                    }
                    return members[index];
                }
            };
        }
    }
}
