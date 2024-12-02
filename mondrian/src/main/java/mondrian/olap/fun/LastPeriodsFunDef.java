/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.olap.type.MemberType;
import mondrian.olap.type.SetType;
import mondrian.olap.type.TypeUtil;
import mondrian.rolap.RolapCube;
import mondrian.rolap.RolapHierarchy;

/**
 * Definition of the <code>LastPeriods</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class LastPeriodsFunDef extends AbstractFunctionDefinition {
    static final ReflectiveMultiResolver Resolver =
        new ReflectiveMultiResolver(
            "LastPeriods",
            "LastPeriods(<Index> [, <Member>])",
            "Returns a set of members prior to and including a specified member.",
            new String[] {"fxn", "fxnm"},
            LastPeriodsFunDef.class);

    public LastPeriodsFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
	public Type getResultType(Validator validator, Expression[] args) {
        if (args.length == 1) {
            // If Member is not specified,
            // it is Time.CurrentMember.
            RolapHierarchy defaultTimeHierarchy =
                ((RolapCube) validator.getQuery().getCube()).getTimeHierarchy(
                		getFunctionMetaData().operationAtom().name());
            return new SetType(MemberType.forHierarchy(defaultTimeHierarchy));
        } else {
            Type type = args[1].getType();
            Type memberType =
            TypeUtil.toMemberOrTupleType(type);
            return new SetType(memberType);
        }
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        // Member defaults to [Time].currentmember
        Expression[] args = call.getArgs();
        final MemberCalc memberCalc;
        if (args.length == 1) {
            final RolapHierarchy timeHierarchy =
                ((RolapCube) compiler.getEvaluator().getCube())
                    .getTimeHierarchy(getFunctionMetaData().operationAtom().name());
            memberCalc =
                new HierarchyCurrentMemberFunDef.CurrentMemberFixedCalc(
                		call.getType(), timeHierarchy);
        } else {
            memberCalc = compiler.compileMember(args[1]);
        }

        // Numeric Expression.
        final IntegerCalc indexValueCalc =
                compiler.compileInteger(args[0]);

        return new AbstractListCalc(
call.getType(), new Calc[] {memberCalc, indexValueCalc})
        {
            @Override
			public TupleList evaluateList(Evaluator evaluator) {
                Member member = memberCalc.evaluate(evaluator);
                Integer indexValue = indexValueCalc.evaluate(evaluator);

                return new UnaryTupleList(
                    lastPeriods(member, evaluator, indexValue));
            }
        };
    }

    /**
     * If Index is positive, returns the set of Index
     * members ending with Member and starting with the
     * member lagging Index - 1 from Member.
     *
     * <p>If Index is negative, returns the set of (- Index)
     * members starting with Member and ending with the
     * member leading (- Index - 1) from Member.
     *
     * <p>If Index is zero, the empty set is returned.
    */
    List<Member> lastPeriods(
        Member member,
        Evaluator evaluator,
        Integer indexValue)
    {
        // empty set
        if ((indexValue == 0) || member.isNull()) {
            return Collections.emptyList();
        }
        List<Member> list = new ArrayList<>();

        // set with just member
        if ((indexValue == 1) || (indexValue == -1)) {
            list.add(member);
            return list;
        }

        // When null is found, getting the first/last
        // member at a given level is not particularly
        // fast.
        Member startMember;
        Member endMember;
        if (indexValue > 0) {
            startMember = evaluator.getSchemaReader()
                .getLeadMember(member, - (indexValue - 1));
            endMember = member;
            if (startMember.isNull()) {
                List<Member> members = evaluator.getSchemaReader()
                    .getLevelMembers(member.getLevel(), false);
                startMember = members.get(0);
            }
        } else {
            startMember = member;
            endMember = evaluator.getSchemaReader()
                .getLeadMember(member, -(indexValue + 1));
            if (endMember.isNull()) {
                List<Member> members = evaluator.getSchemaReader()
                    .getLevelMembers(member.getLevel(), false);
                endMember = members.get(members.size() - 1);
            }
        }

        evaluator.getSchemaReader().getMemberRange(
            member.getLevel(),
            startMember,
            endMember,
            list);
        return list;
    }
}
