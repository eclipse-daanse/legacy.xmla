/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// Copyright (C) 2021 Sergei Semenkov
// All Rights Reserved.
*/

package mondrian.olap.fun;
import java.util.List;

import mondrian.olap.exceptions.FunctionMbrAndLevelHierarchyMismatchException;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.SchemaReader;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.LevelCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedMemberCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.Util;
import mondrian.olap.type.MemberType;
import mondrian.rolap.RolapCube;
import mondrian.rolap.RolapHierarchy;

/**
 * Definition of the <code>OpeningPeriod</code> and <code>ClosingPeriod</code>
 * builtin functions.
 *
 * @author jhyde
 * @since 2005/8/14
 */
class OpeningClosingPeriodFunDef extends AbstractFunctionDefinition {
    private final boolean opening;

    static final FunctionResolver OpeningPeriodResolver =
        new MultiResolver(
            "OpeningPeriod",
            "OpeningPeriod([<Level>[, <Member>]])",
            "Returns the first descendant of a member at a level.",
            new String[] {"fm", "fml", "fmlm"})
    {
        @Override
		protected FunctionDefinition createFunDef(Expression[] args, FunctionMetaData functionMetaData ) {
            return new OpeningClosingPeriodFunDef(functionMetaData, true);
        }
    };

    static final FunctionResolver ClosingPeriodResolver =
        new MultiResolver(
            "ClosingPeriod",
            "ClosingPeriod([<Level>[, <Member>]])",
            "Returns the last descendant of a member at a level.",
            new String[] {"fm", "fml", "fmlm", "fmm"})
    {
        @Override
		protected FunctionDefinition createFunDef(Expression[] args, FunctionMetaData functionMetaData ) {
            return new OpeningClosingPeriodFunDef(functionMetaData, false);
        }
    };

    public OpeningClosingPeriodFunDef(
        FunctionMetaData functionMetaData ,
        boolean opening)
    {
        super(functionMetaData);
        this.opening = opening;
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
        final Expression[] args = call.getArgs();
        final LevelCalc levelCalc;
        final MemberCalc memberCalc;
        RolapHierarchy defaultTimeHierarchy = null;
        switch (args.length) {
        case 0:
            defaultTimeHierarchy =
                ((RolapCube) compiler.getEvaluator().getCube())
                    .getTimeHierarchy(getFunctionMetaData().operationAtom().name());
            memberCalc =
                new HierarchyCurrentMemberFunDef.CurrentMemberFixedCalc(
                        MemberType.forHierarchy(defaultTimeHierarchy),
                    defaultTimeHierarchy);
            levelCalc = null;
            break;
        case 1:
            defaultTimeHierarchy =
                ((RolapCube) compiler.getEvaluator().getCube())
                    .getTimeHierarchy(getFunctionMetaData().operationAtom().name());
            levelCalc = compiler.compileLevel(call.getArg(0));
            memberCalc =
                new HierarchyCurrentMemberFunDef.CurrentMemberFixedCalc(

                        MemberType.forHierarchy(defaultTimeHierarchy),
                    defaultTimeHierarchy);
            break;
        default:
            levelCalc = compiler.compileLevel(call.getArg(0));
            memberCalc = compiler.compileMember(call.getArg(1));
            break;
        }

        // Make sure the member and the level come from the same dimension.
        if (levelCalc != null) {
            final Dimension memberDimension =
                memberCalc.getType().getDimension();
            final Dimension levelDimension = levelCalc.getType().getDimension();
            if (!memberDimension.equals(levelDimension)) {
                throw new FunctionMbrAndLevelHierarchyMismatchException(
                        opening ? "OpeningPeriod" : "ClosingPeriod",
                        levelDimension.getUniqueName(),
                        memberDimension.getUniqueName());
            }
        }
        return new AbstractProfilingNestedMemberCalc(
        		call.getType(), new Calc[] {levelCalc, memberCalc})
        {
            @Override
			public Member evaluate(Evaluator evaluator) {
                Member member = memberCalc.evaluate(evaluator);

                // If the level argument is present, use it. Otherwise use the
                // level immediately after that of the member argument.
                Level level;
                if (levelCalc == null) {
                    int targetDepth = member.getLevel().getDepth() + 1;
                    Level[] levels = member.getHierarchy().getLevels();

                    if (levels.length <= targetDepth) {
                        return member.getHierarchy().getNullMember();
                    }
                    level = levels[targetDepth];
                } else {
                    level = levelCalc.evaluate(evaluator);
                }

                // Shortcut if the level is above the member.
                if (level.getDepth() < member.getLevel().getDepth()) {
                    return member.getHierarchy().getNullMember();
                }

                // Shortcut if the level is the same as the member
                if (level == member.getLevel()) {
                    return member;
                }

                return OpeningClosingPeriodFunDef.getDescendant(
                    evaluator.getSchemaReader(), member,
                    level, opening, evaluator);
            }
        };
    }

    /**
     * Returns the first or last descendant of the member at the target level.
     * This method is the implementation of both OpeningPeriod and
     * ClosingPeriod.
     *
     * @param schemaReader The schema reader to use to evaluate the function.
     * @param member The member from which the descendant is to be found.
     * @param targetLevel The level to stop at.
     * @param returnFirstDescendant Flag indicating whether to return the first
     * or last descendant of the member.
     * @return A member.
     * @pre member.getLevel().getDepth() < level.getDepth();
     */
    static Member getDescendant(
        SchemaReader schemaReader,
        Member member,
        Level targetLevel,
        boolean returnFirstDescendant,
        Evaluator evaluator)
    {
        List<Member> children;

        final int targetLevelDepth = targetLevel.getDepth();
        Util.assertPrecondition(member.getLevel().getDepth() < targetLevelDepth,
                "member.getLevel().getDepth() < targetLevel.getDepth()");

        for (;;) {
            children = schemaReader.getMemberChildren(member);

            if (children.size() == 0) {
                return targetLevel.getHierarchy().getNullMember();
            }

            final int index =
                returnFirstDescendant ? 0 : (children.size() - 1);
            member = children.get(index);
            if (member.getLevel().getDepth() == targetLevelDepth) {
                if (member.isHidden()) {
                    return member.getHierarchy().getNullMember();
                } else {
                    return member;
                }
            }
        }
    }

}
