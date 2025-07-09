/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package org.eclipse.daanse.olap.query.component;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.constant.ConstantMemberCalc;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.query.component.expression.AbstractExpression;

/**
 * Usage of a {@link org.eclipse.daanse.olap.api.element.Member} as an MDX expression.
 *
 * @author jhyde
 * @since Sep 26, 2005
 */
public class MemberExpressionImpl extends AbstractExpression implements Expression, MemberExpression {
    private final Member member;
    private MemberType type;

    /**
     * Creates a member expression.
     *
     * @param member Member
     * @pre member != null
     */
    public MemberExpressionImpl(Member member) {
        Util.assertPrecondition(member != null, "member != null");
        this.member = member;
    }

    /**
     * Returns the member.
     *
     * @post return != null
     */
    @Override
    public Member getMember() {
        return member;
    }

    @Override
	public String toString() {
        return member.getUniqueName();
    }

    @Override
	public Type getType() {
        if (type == null) {
            type = MemberType.forMember(member);
        }
        return type;
    }

    @Override
	public MemberExpressionImpl cloneExp() {
        return new MemberExpressionImpl(member);
    }

    @Override
	public DataType getCategory() {
        return DataType.MEMBER;
    }

    @Override
	public Expression accept(Validator validator) {
        return this;
    }

    @Override
	public MemberCalc accept(ExpressionCompiler compiler) {
        return ConstantMemberCalc.of(member);
    }

    @Override
	public Object accept(QueryComponentVisitor visitor) {
        return visitor.visitMemberExpression(this);
    }
}
