/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import mondrian.olap.exceptions.EmptyExpressionWasSpecifiedException;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedMemberCalc;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

/**
 * Definition of the <code>StrToMember</code> MDX function.
 *
 * <p>Syntax:
 * <blockquote><code>StrToMember(&lt;String Expression&gt;)
 * </code></blockquote>
 */
class StrToMemberFunDef extends AbstractFunctionDefinition {

    static OperationAtom functionAtom = new FunctionOperationAtom("StrToMember");

    static FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtom, "Returns a member from a unique name String in MDX format.",
            "StrToMember(<STRING>)", DataType.MEMBER, new DataType[] { DataType.STRING });

    public static final FunctionDefinition INSTANCE = new StrToMemberFunDef();

    private StrToMemberFunDef() {
        super(functionMetaData);
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final StringCalc memberNameCalc =
            compiler.compileString(call.getArg(0));
        return new AbstractProfilingNestedMemberCalc(call.getType(), new Calc[] {memberNameCalc}) {
            @Override
			public Member evaluate(Evaluator evaluator) {
                String memberName =
                    memberNameCalc.evaluate(evaluator);
                if (memberName == null) {
                    throw FunUtil.newEvalException(
                        new EmptyExpressionWasSpecifiedException());
                }
                return FunUtil.parseMember(evaluator, memberName, null);
            }
        };
    }
}
