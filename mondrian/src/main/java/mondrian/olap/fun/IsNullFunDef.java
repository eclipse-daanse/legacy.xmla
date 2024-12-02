/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2018 Hitachi Vantara..  All rights reserved.
*/
package mondrian.olap.fun;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedBooleanCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.rolap.RolapMember;
import mondrian.rolap.RolapUtil;

/**
 * Definition of the <code>IS NULL</code> MDX function.
 *
 * @author medstat
 * @since Aug 21, 2006
 */
class IsNullFunDef extends AbstractFunctionDefinition {
    /**
     * Resolves calls to the <code>IS NULL</code> postfix operator.
     */
    static final ReflectiveMultiResolver Resolver =
        new ReflectiveMultiResolver(
            "IS NULL",
            "<Expression> IS NULL",
            "Returns whether an object is null",
            new String[]{"Qbm", "Qbl", "Qbh", "Qbd"},
            IsNullFunDef.class);

    public IsNullFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {

        if (call.getArgCount() != 1) {
            throw new IllegalArgumentException("ArgCount should be 1 ");
        }
        final MemberCalc memberCalc = compiler.compileMember(call.getArg(0));
        return new AbstractProfilingNestedBooleanCalc(call.getType(), new Calc[]{memberCalc}) {
            @Override
			public Boolean evaluate(Evaluator evaluator) {
                Member member = memberCalc.evaluate(evaluator);
                return member.isNull()
                   || nonAllWithNullKey((RolapMember) member);
      }
    };
  }

  /**
   * Dimension members with a null value are treated as the null member.
   */
  private boolean nonAllWithNullKey(RolapMember member) {
    return !member.isAll() && member.getKey() == RolapUtil.sqlNullValue;
  }
}
