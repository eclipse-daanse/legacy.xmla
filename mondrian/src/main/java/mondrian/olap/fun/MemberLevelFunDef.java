/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedLevelCalc;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.type.LevelType;

/**
 * Definition of the <code>&lt;Member&gt;.Level</code> MDX builtin function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
public class MemberLevelFunDef extends AbstractFunctionDefinition {

    static OperationAtom plainPropertyOperationAtom = new PlainPropertyOperationAtom("Level");

    static FunctionMetaData functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns a member's level.",
            "<MEMBER>.Level", DataType.LEVEL, new DataType[] { DataType.MEMBER });

    static final MemberLevelFunDef instance = new MemberLevelFunDef();

    private MemberLevelFunDef() {
        super(functionMetaData);
    }

    @Override
	public Type getResultType(Validator validator, Expression[] args) {
        final Type argType = args[0].getType();
        return LevelType.forType(argType);
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final MemberCalc memberCalc =
                compiler.compileMember(call.getArg(0));
        return new MemberLevelCalcImpl(call.getType(), memberCalc);
    }

    public static class MemberLevelCalcImpl extends AbstractProfilingNestedLevelCalc {
        private final MemberCalc memberCalc;

        public MemberLevelCalcImpl(Type type, MemberCalc memberCalc) {
            super(type, new Calc[] {memberCalc});
            this.memberCalc = memberCalc;
        }



		@Override
		public Level evaluate(Evaluator evaluator) {
            Member member = memberCalc.evaluate(evaluator);
            return member.getLevel();
        }
    }
}
