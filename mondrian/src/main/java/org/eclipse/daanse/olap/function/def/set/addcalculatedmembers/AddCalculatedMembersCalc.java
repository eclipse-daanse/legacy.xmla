/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.function.def.set.addcalculatedmembers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractProfilingNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;

import mondrian.olap.fun.MondrianEvaluationException;

public class AddCalculatedMembersCalc extends AbstractProfilingNestedTupleListCalc {

    protected AddCalculatedMembersCalc(Type type, final TupleListCalc tupleListCalc) {
        super(type, tupleListCalc);
    }

    @Override
    public TupleList evaluate(Evaluator evaluator) {
        final TupleList list = getChildCalc(0, TupleListCalc.class).evaluate(evaluator);
        return new UnaryTupleList(addCalculatedMembers(list.slice(0), evaluator));
    }

    private List<Member> addCalculatedMembers(List<Member> memberList, Evaluator evaluator) {
        // Determine unique levels in the set
        final Set<Level> levels = new LinkedHashSet<>();
        Hierarchy hierarchy = null;

        for (Member member : memberList) {
            if (hierarchy == null) {
                hierarchy = member.getHierarchy();
            } else if (hierarchy != member.getHierarchy()) {
                throw new MondrianEvaluationException(
                        new StringBuilder("Only members from the same hierarchy are allowed in the ")
                                .append("AddCalculatedMembers set: ").append(hierarchy).append(" vs ")
                                .append(member.getHierarchy()).toString());
            }
            levels.add(member.getLevel());
        }

        // For each level, add the calculated members from both
        // the schema and the query
        List<Member> workingList = new ArrayList<>(memberList);
        final CatalogReader schemaReader = evaluator.getQuery().getCatalogReader(true);
        for (Level level : levels) {
            List<Member> calcMemberList = schemaReader.getCalculatedMembers(level);
            for (Member calcMember : calcMemberList) {
                Member parentMember = calcMember.getParentMember();
                if (parentMember == null || memberList.stream().anyMatch(m -> m.getParentMember() != null
                        && m.getParentMember().getUniqueName().equals(parentMember.getUniqueName()))) {
                    workingList.add(calcMember);
                }
            }
        }
        return workingList;
    }

}
