/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/
package mondrian.olap.fun;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.query.component.MdxVisitorImpl;

/**
 * Visitor class used to locate a resolved function call within an
 * expression
 */
public class ResolvedFunCallFinder
    extends MdxVisitorImpl
{
    private final ResolvedFunCall call;
    public boolean found;
    private final Set<Member> activeMembers = new HashSet<>();

    public ResolvedFunCallFinder(ResolvedFunCall call)
    {
        this.call = call;
        found = false;
    }

    @Override
	public Object visitResolvedFunCall(ResolvedFunCall funCall)
    {
        if (funCall == call) {
            found = true;
        }
        return null;
    }

    @Override
	public Object visitMemberExpression(MemberExpression memberExpr) {
        Member member = memberExpr.getMember();
        if (member.isCalculated()) {
            if (activeMembers.add(member)) {
                Expression memberExp = member.getExpression();
                memberExp.accept(this);
                activeMembers.remove(member);
            }
        }
        return null;
    }
}
