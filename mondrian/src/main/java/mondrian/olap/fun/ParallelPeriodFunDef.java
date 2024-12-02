/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import mondrian.olap.exceptions.FunctionMbrAndLevelHierarchyMismatchException;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.LevelCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.constant.ConstantIntegerCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedMemberCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.type.DecimalType;
import mondrian.olap.type.MemberType;
import mondrian.rolap.RolapCube;
import mondrian.rolap.RolapHierarchy;

/**
 * Definition of the <code>ParallelPeriod</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class ParallelPeriodFunDef extends AbstractFunctionDefinition {
    static final ReflectiveMultiResolver Resolver =
        new ReflectiveMultiResolver(
            "ParallelPeriod",
            "ParallelPeriod([<Level>[, <Numeric Expression>[, <Member>]]])",
            "Returns a member from a prior period in the same relative position as a specified member.",
            new String[] {"fm", "fml", "fmln", "fmlnm"},
            ParallelPeriodFunDef.class);

    public ParallelPeriodFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
	public Type getResultType(Validator validator, Expression[] args) {
        if (args.length == 0) {
            // With no args, the default implementation cannot
            // guess the hierarchy, so we supply the Time
            // dimension.
            RolapHierarchy defaultTimeHierarchy =
                ((RolapCube) validator.getQuery().getCube()).getTimeHierarchy(
                		getFunctionMetaData().operationAtom().name());
            return MemberType.forHierarchy(defaultTimeHierarchy);
        }
        return super.getResultType(validator, args);
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        // Member defaults to [Time].currentmember
        Expression[] args = call.getArgs();

        // Numeric Expression defaults to 1.
        final IntegerCalc lagValueCalc =
            (args.length >= 2)
            ? compiler.compileInteger(args[1])
            : new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), 1);

        // If level is not specified, we compute it from
        // member at runtime.
        final LevelCalc ancestorLevelCalc =
            args.length >= 1
            ? compiler.compileLevel(args[0])
            : null;

        final MemberCalc memberCalc;
        switch (args.length) {
        case 3:
            memberCalc = compiler.compileMember(args[2]);
            break;
        case 1:
            final Hierarchy hierarchy = args[0].getType().getHierarchy();
            if (hierarchy != null) {
                // For some functions, such as Levels(<string expression>),
                // the dimension cannot be determined at compile time.
                memberCalc =
                    new HierarchyCurrentMemberFunDef.CurrentMemberFixedCalc(
                        call.getType(), hierarchy);
            } else {
                memberCalc = null;
            }
            break;
        default:
            final RolapHierarchy timeHierarchy =
                ((RolapCube) compiler.getEvaluator().getCube())
                    .getTimeHierarchy(getFunctionMetaData().operationAtom().name());
            memberCalc =
                new HierarchyCurrentMemberFunDef.CurrentMemberFixedCalc(
                		call.getType(), timeHierarchy);
            break;
        }

        return new AbstractProfilingNestedMemberCalc(
        		call.getType(),
            new Calc[] {memberCalc, lagValueCalc, ancestorLevelCalc})
        {
            @Override
			public Member evaluate(Evaluator evaluator) {
                Member member;
                Integer lagValue = lagValueCalc.evaluate(evaluator);
                Level ancestorLevel;
                if (ancestorLevelCalc != null) {
                    ancestorLevel = ancestorLevelCalc.evaluate(evaluator);
                    if (memberCalc == null) {
                        member =
                            evaluator.getContext(ancestorLevel.getHierarchy());
                    } else {
                        member = memberCalc.evaluate(evaluator);
                    }
                } else {
                    member = memberCalc.evaluate(evaluator);
                    Member parent = member.getParentMember();
                    if (parent == null) {
                        // This is a root member,
                        // so there is no parallelperiod.
                        return member.getHierarchy().getNullMember();
                    }
                    ancestorLevel = parent.getLevel();
                }
                return parallelPeriod(
                    member, ancestorLevel, evaluator, lagValue);
            }
        };
    }

    Member parallelPeriod(
        Member member,
        Level ancestorLevel,
        Evaluator evaluator,
        Integer lagValue)
    {
        // Now do some error checking.
        // The ancestorLevel and the member must be from the
        // same hierarchy.
        if (member.getHierarchy() != ancestorLevel.getHierarchy()) {
            new FunctionMbrAndLevelHierarchyMismatchException(
                "ParallelPeriod",
                ancestorLevel.getHierarchy().getUniqueName(),
                member.getHierarchy().getUniqueName());
        }

        if (lagValue == Integer.MIN_VALUE) {
            // Bump up lagValue by one; otherwise -lagValue (used in
            // the getleadMember call below) is out of range because
            // Integer.MAX_VALUE == -(Integer.MIN_VALUE + 1)
            lagValue +=  1;
        }

        int distance =
            member.getLevel().getDepth()
            - ancestorLevel.getDepth();
        Member ancestor = FunUtil.ancestor(
            evaluator, member, distance, ancestorLevel);
        Member inLaw = evaluator.getSchemaReader()
            .getLeadMember(ancestor, -lagValue);
        return FunUtil.cousin(
            evaluator.getSchemaReader(), member, inLaw);
    }
}
