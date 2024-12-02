/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleCursor;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedStringCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

/**
 * Definition of the <code>SetToStr</code> MDX function.
 *
 * @author jhyde
 * @since Aug 3, 2006
 */
class SetToStrFunDef extends AbstractFunctionDefinition {
    public static final AbstractFunctionDefinition instance = new SetToStrFunDef();

    private SetToStrFunDef() {
        super("SetToStr", "Constructs a string from a set.", "fSx");
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        Expression arg = call.getArg(0);
        final TupleListCalc tupleListCalc = compiler.compileList(arg);
        return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[]{tupleListCalc}) {
            @Override
			public String evaluate(Evaluator evaluator) {
                final TupleList list = tupleListCalc.evaluateList(evaluator);
                if (list.getArity() == 1) {
                    return SetToStrFunDef.memberSetToStr(list.slice(0));
                } else {
                    return SetToStrFunDef.tupleSetToStr(list);
                }
            }
        };
    }

    static String memberSetToStr(List<Member> list) {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        int k = 0;
        for (Member member : list) {
            if (k++ > 0) {
                buf.append(", ");
            }
            buf.append(member.getUniqueName());
        }
        buf.append("}");
        return buf.toString();
    }

    static String tupleSetToStr(TupleList list) {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        int k = 0;
        Member[] members = new Member[list.getArity()];
        final TupleCursor cursor = list.tupleCursor();
        while (cursor.forward()) {
            if (k++ > 0) {
                buf.append(", ");
            }
            cursor.currentToArray(members, 0);
            FunUtil.appendTuple(buf, members);
        }
        buf.append("}");
        return buf.toString();
    }
}
