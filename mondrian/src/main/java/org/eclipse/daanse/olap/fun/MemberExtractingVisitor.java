/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */

package org.eclipse.daanse.olap.fun;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.DimensionExpression;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.HierarchyExpression;
import org.eclipse.daanse.olap.api.query.component.LevelExpression;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.ParameterExpression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.query.component.MdxVisitorImpl;

/**
 * Visitor which collects any non-measure base members encountered while
 * traversing an expression.
 *
 * <p>This Visitor is used by the native set classes as well as the crossjoin
 * optimizer (CrossjoinFunDef.nonEmptyList) to identify members within
 * an expression which may conflict with members used as a constraint.</p>
 *
 * <p>If the boolean mapToAllMember is true, then any occurrences of
 * a Dimension/Hierarchy/Level will result in the corresponding [All] member
 * being added to the collection.  Likewise if a specific member is
 * visited, the [All] member of it's corresponding hierarchy will be
 * added.</p>
 *
 * <p>The mapToAllMember behavior will be used for any subexpression under
 * one of the functions in the blacklist collection below.</p>
 */
public class MemberExtractingVisitor extends MdxVisitorImpl {

    private final Set<Member> memberSet;
    private final ResolvedFunCallFinder finder;
    private final Set<Member> activeMembers = new HashSet<>();
    private final ResolvedFunCall call;
    private final boolean mapToAllMember;

    /**
     * This list of functions are "blacklisted" because
     * occurrence of a member/dim/level/hierarchy within
     * one of these expressions would result in a set of members
     * that cannot be determined from the expression itself.
     */
    private static final String[] unsafeFuncNames = new String[] {
        "Ytd", "Mtd", "Qtd", "Wtd", "BottomCount", "TopCount", "ClosingPeriod",
        "Cousin", "FirstChild", "FirstSibling", "LastChild", "LastPeriods",
        "LastSibling", "ParallelPeriod", "PeriodsToDate", "Parent",
        "PrevMember", "NextMember", "Ancestor", "Ancestors"
    };
    private static final List<String> blacklist = Collections.unmodifiableList(
        Arrays.asList(MemberExtractingVisitor.unsafeFuncNames));

    public MemberExtractingVisitor(
        Set<Member> memberSet, ResolvedFunCall call, boolean mapToAllMember)
    {
        this.memberSet = memberSet;
        this.finder = new ResolvedFunCallFinder(call);
        this.call = call;
        this.mapToAllMember = mapToAllMember;
    }

    @Override
	public Object visitParameterExpression(ParameterExpression parameterExpr) {
        final Parameter parameter = parameterExpr.getParameter();
        final Type type = parameter.getType();
        if (type instanceof org.eclipse.daanse.olap.api.type.MemberType) {
            final Object value = parameter.getValue();
            if (value instanceof Member member) {
                if (!member.isMeasure() && !member.isCalculated()) {
                    addMember(member);
                }
            } else {
               parameter.getDefaultExp().accept(this);
            }
        }
        return null;
    }

    @Override
	public Object visitMemberExpression(MemberExpression memberExpr) {
        Member member = memberExpr.getMember();
        if (!member.isMeasure() && !member.isCalculated()) {
            addMember(member);
        } else if (member.isCalculated()) {
            if (activeMembers.add(member)) {
                Expression exp = member.getExpression();
                finder.found = false;
                exp.accept(finder);
                if (! finder.found) {
                    exp.accept(this);
                }
                activeMembers.remove(member);
            }
        }
        return null;
    }

    @Override
	public Object visitDimensionExpression(DimensionExpression dimensionExpr) {
        // add the default hierarchy
        addToDimMemberSet(dimensionExpr.getDimension().getHierarchy());
        return null;
    }

    @Override
	public Object visitHierarchyExpression(HierarchyExpression hierarchyExpr) {
        addToDimMemberSet(hierarchyExpr.getHierarchy());
        return null;
    }

    @Override
	public Object visitLevelExpression(LevelExpression levelExpr) {
        addToDimMemberSet(levelExpr.getLevel().getHierarchy());
        return null;
    }

    @Override
	public Object visitResolvedFunCall(ResolvedFunCall funCall) {
        if (funCall == call) {
            turnOffVisitChildren();
        } else if (MemberExtractingVisitor.blacklist.contains(funCall.getOperationAtom().name())) {
            for (Expression arg : funCall.getArgs()) {
                arg.accept(new MemberExtractingVisitor(memberSet, call, true));
            }
            turnOffVisitChildren();
        }
        return null;
    }

    private void addMember(Member member) {
        if (!mapToAllMember) {
            memberSet.add(member);
        } else {
            memberSet.add(member.getHierarchy().getAllMember());
        }
    }

    private void addToDimMemberSet(Hierarchy hierarchy) {
        if (mapToAllMember && !hierarchy.getDimension().isMeasures()) {
            memberSet.add(hierarchy.getAllMember());
        }
    }
}
