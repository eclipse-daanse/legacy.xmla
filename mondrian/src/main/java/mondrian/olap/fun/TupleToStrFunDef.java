/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.TupleCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedStringCalc;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.type.TypeUtil;

/**
 * Definition of the <code>TupleToStr</code> MDX function.
 *
 * <p>Syntax:
 * <blockquote><code>
 * TupleToStr(&lt;Tuple&gt;)
 * </code></blockquote>
 *
 * @author jhyde
 * @since Aug 3, 2006
 */
class TupleToStrFunDef extends AbstractFunctionDefinition {

    static OperationAtom functionAtom = new FunctionOperationAtom("TupleToStr");

	static FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtom, "Constructs a string from a tuple.",
            "TupleToStr()", DataType.STRING, new DataType[] { DataType.TUPLE });

    static final TupleToStrFunDef instance = new TupleToStrFunDef();

    private TupleToStrFunDef() {
        super(functionMetaData);
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        if (TypeUtil.couldBeMember(call.getArg(0).getType())) {
            final MemberCalc memberCalc =
                    compiler.compileMember(call.getArg(0));
            return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {memberCalc}) {
                @Override
				public String evaluate(Evaluator evaluator) {
                    final Member member =
                            memberCalc.evaluate(evaluator);
                    if (member.isNull()) {
                        return "";
                    }
                    StringBuilder buf = new StringBuilder();
                    buf.append(member.getUniqueName());
                    return buf.toString();
                }
            };
        } else {
            final TupleCalc tupleCalc =
                    compiler.compileTuple(call.getArg(0));
            return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {tupleCalc}) {
                @Override
				public String evaluate(Evaluator evaluator) {
                    final Member[] members =
                            tupleCalc.evaluate(evaluator);
                    if (members == null) {
                        return "";
                    }
                    StringBuilder buf = new StringBuilder();
                    FunUtil.appendTuple(buf, members);
                    return buf.toString();
                }
            };
        }
    }

}
