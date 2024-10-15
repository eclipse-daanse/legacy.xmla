/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/

package mondrian.olap.fun;

import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.base.constant.ConstantMemberCalc;
import org.eclipse.daanse.olap.function.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.function.FunctionMetaDataR;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.TupleCollections;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.olap.MondrianException;
import mondrian.olap.type.NullType;
import mondrian.rolap.RolapMember;

/**
 * Definition of the MDX <code>&lt;Member&gt : &lt;Member&gt;</code> operator,
 * which returns the set of members between a given pair of members.
 *
 * @author jhyde
 * @since 3 March, 2002
 */
class RangeFunDef extends AbstractFunctionDefinition {


	static OperationAtom functionAtom = new InfixOperationAtom(":");
	static final RangeFunDef instance = new RangeFunDef();
    private final static String twoNullsNotSupported = "Function does not support two NULL member parameters";

    private RangeFunDef() {
        super(new FunctionMetaDataR(functionAtom,
    			"Infix colon operator returns the set of members between a given pair of members.", "<Member> : <Member>",  DataType.SET,
    			new DataType[] { DataType.MEMBER,DataType.MEMBER }));
    }


    /**
     * Returns two membercalc objects, substituting nulls with the hierarchy
     * null member of the other expression.
     *
     * @param exp0 first expression
     * @param exp1 second expression
     *
     * @return two member calcs
     */
    private MemberCalc[] compileMembers(
        Expression exp0, Expression exp1, ExpressionCompiler compiler)
    {
        MemberCalc[] members = new MemberCalc[2];

        if (exp0.getType() instanceof NullType) {
            members[0] = null;
        } else {
            members[0] = compiler.compileMember(exp0);
        }

        if (exp1.getType() instanceof NullType) {
            members[1] = null;
        } else {
            members[1] = compiler.compileMember(exp1);
        }

        // replace any null types with hierachy null member
        // if both objects are null, throw exception

        if (members[0] == null && members[1] == null) {
            throw new MondrianException(twoNullsNotSupported);
        } else if (members[0] == null) {
            Member nullMember =
                ((RolapMember) members[1].evaluate(null)).getHierarchy()
                .getNullMember();
            members[0] = ConstantMemberCalc.of(nullMember);
        } else if (members[1] == null) {
            Member nullMember =
                ((RolapMember) members[0].evaluate(null)).getHierarchy()
                .getNullMember();
            members[1] = ConstantMemberCalc.of(nullMember);
        }

        return members;
    }

    @Override
	public Calc compileCall(final ResolvedFunCall call, ExpressionCompiler compiler) {
        final MemberCalc[] memberCalcs =
            compileMembers(call.getArg(0), call.getArg(1), compiler);
        return new AbstractListCalc(
        		call.getType(), new Calc[] {memberCalcs[0], memberCalcs[1]})
        {
            @Override
			public TupleList evaluateList(Evaluator evaluator) {
                final Member member0 = memberCalcs[0].evaluate(evaluator);
                final Member member1 = memberCalcs[1].evaluate(evaluator);
                if (member0.isNull() || member1.isNull()) {
                    return TupleCollections.emptyList(1);
                }
                if (member0.getLevel() != member1.getLevel()) {
                    throw evaluator.newEvalException(
                        call.getFunDef().getFunctionMetaData(),
                        "Members must belong to the same level");
                }
                return new UnaryTupleList(
                    FunUtil.memberRange(evaluator, member0, member1));
            }
        };
    }
}
