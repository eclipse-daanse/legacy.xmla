/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package mondrian.calc.impl;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.ScalarType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedUnknownCalc;
import org.eclipse.daanse.olap.calc.base.value.CurrentValueUnknownCalc;

/**
 * Expression which evaluates a few member expressions,
 * sets the dimensional context to the result of those expressions,
 * then yields the value of the current measure in the current
 * dimensional context.
 *
 * <p>The evaluator's context is preserved.
 *
 * <p>Note that a MemberValueCalc with 0 member expressions is equivalent to a
 * {@link org.eclipse.daanse.olap.calc.base.value.CurrentValueUnknownCalc}; see also {@link mondrian.calc.impl.TupleValueCalc}.
 *
 * @author jhyde
 * @since Sep 27, 2005
 */
public class MemberValueCalc extends AbstractProfilingNestedUnknownCalc {
    private final MemberCalc memberCalc;
    private final boolean nullCheck;

    /**
     * Creates a MemberArrayValueCalc.
     *
     * <p>Clients outside this package should use the
     * {@link MemberValueCalc#create(mondrian.olap.Exp,
     * org.eclipse.daanse.olap.calc.api.MemberCalc[], boolean)}
     * factory method.
     *
     * @param exp Expression
     * @param memberCalc Compiled expression
     * @param nullCheck Whether to check for null values due to non-joining
     *     dimensions in a virtual cube
     */
    public MemberValueCalc(Type type, MemberCalc memberCalc, boolean nullCheck) {
        super(type);
        this.nullCheck = nullCheck;
        if (!(type instanceof ScalarType)) {
            throw new IllegalArgumentException("Invalid type in MemberValueCalc");
        }
        this.memberCalc = memberCalc;
    }

    /**
     * Creates a {@link CurrentValueUnknownCalc}, {@link MemberValueCalc} or
     * {@link MemberArrayValueCalc}.
     *
     * @param exp Expression
     * @param memberCalcs Array of members to evaluate
     * @param nullCheck Whether to check for null values due to non-joining
     *     dimensions in a virtual cube
     * @return Compiled expression to evaluate each member expression, set
     *   evaluator context to each resulting member, then evaluate the current
     *   context
     */
    public static AbstractProfilingNestedUnknownCalc create(
    		 Type type,
            MemberCalc[] memberCalcs,
            boolean nullCheck)
    {
        return switch (memberCalcs.length) {
        case 0 -> new CurrentValueUnknownCalc(type);
        case 1 -> new MemberValueCalc(type, memberCalcs[0], nullCheck);
        default -> new MemberArrayValueCalc(type, memberCalcs, nullCheck);
        };
    }

    @Override
    public Object evaluate(Evaluator evaluator) {
        final int savepoint = evaluator.savepoint();
        try {
            final Member member = memberCalc.evaluate(evaluator);
            if (member == null
                    || member.isNull())
            {
                return null;
            }
            evaluator.setContext(member);
            if (nullCheck
                    && evaluator.needToReturnNullForUnrelatedDimension(
                            new Member[] {member}))
            {
                return null;
            }
            return evaluator.evaluateCurrent();
        } finally {
            evaluator.restore(savepoint);
        }
    }

    @Override
    public Calc[] getChildCalcs() {
        return new MemberCalc[] {memberCalc};
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        if (super.dependsOn(hierarchy)) {
            return true;
        }
        // If the expression definitely includes the dimension (in this
        // case, that means it is a member of that dimension) then we
        // do not depend on the dimension. For example, the scalar value of
        //   [Store].[USA]
        // does not depend on [Store].
        //
        // If the dimensionality of the expression is unknown, then the
        // expression MIGHT include the dimension, so to be safe we have to
        // say that it depends on the given dimension. For example,
        //   Dimensions(3).CurrentMember.Parent
        // may depend on [Store].
        return !memberCalc.getType().usesHierarchy(hierarchy, true);
    }
}
