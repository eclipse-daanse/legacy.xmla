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
package org.eclipse.daanse.olap.function.def.lastperiods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.UnaryTupleList;

public class LastPeriodsCalc extends AbstractListCalc {

    public LastPeriodsCalc(Type type, MemberCalc memberCalc, IntegerCalc indexValueCalc) {
        super(type, memberCalc, indexValueCalc);
    }

    @Override
    public TupleList evaluateList(Evaluator evaluator) {
        Member member = getChildCalc(0, MemberCalc.class).evaluate(evaluator);
        Integer indexValue = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);

        return new UnaryTupleList(lastPeriods(member, evaluator, indexValue));
    }

    /**
     * If Index is positive, returns the set of Index members ending with Member and
     * starting with the member lagging Index - 1 from Member.
     *
     * <p>
     * If Index is negative, returns the set of (- Index) members starting with
     * Member and ending with the member leading (- Index - 1) from Member.
     *
     * <p>
     * If Index is zero, the empty set is returned.
     */
    private List<Member> lastPeriods(Member member, Evaluator evaluator, Integer indexValue) {
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
            startMember = evaluator.getSchemaReader().getLeadMember(member, -(indexValue - 1));
            endMember = member;
            if (startMember.isNull()) {
                List<Member> members = evaluator.getSchemaReader().getLevelMembers(member.getLevel(), false);
                startMember = members.get(0);
            }
        } else {
            startMember = member;
            endMember = evaluator.getSchemaReader().getLeadMember(member, -(indexValue + 1));
            if (endMember.isNull()) {
                List<Member> members = evaluator.getSchemaReader().getLevelMembers(member.getLevel(), false);
                endMember = members.get(members.size() - 1);
            }
        }

        evaluator.getSchemaReader().getMemberRange(member.getLevel(), startMember, endMember, list);
        return list;
    }

}