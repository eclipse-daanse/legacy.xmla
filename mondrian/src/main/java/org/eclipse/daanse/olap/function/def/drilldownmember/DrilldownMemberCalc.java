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
package org.eclipse.daanse.olap.function.def.drilldownmember;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.TupleCollections;

public class DrilldownMemberCalc extends AbstractListCalc {

    private final boolean recursive;

    public DrilldownMemberCalc(final Type type, final TupleListCalc listCalc1, final TupleListCalc listCalc2,
            final boolean recursive) {
        super(type, listCalc1, listCalc2);
        this.recursive = recursive;
    }

    @Override
    public TupleList evaluate(Evaluator evaluator) {
        final TupleList list1 = getChildCalc(0, TupleListCalc.class).evaluate(evaluator);
        final TupleList list2 = getChildCalc(1, TupleListCalc.class).evaluate(evaluator);
        return drilldownMember(list1, list2, evaluator);
    }

    /**
     * Drills down an element.
     *
     * <p>
     * Algorithm: If object is present in {@code memberSet} adds to result children
     * of the object. If flag {@code recursive} is set then this method is called
     * recursively for the children.
     *
     * @param evaluator  Evaluator
     * @param tuple      Tuple (may have arity 1)
     * @param memberSet  Set of members
     * @param resultList Result
     */
    protected void drillDownObj(Evaluator evaluator, Member[] tuple, Set<Member> memberSet, TupleList resultList) {
        for (int k = 0; k < tuple.length; k++) {
            Member member = tuple[k];
            if (memberSet.contains(member)) {
                List<Member> children = evaluator.getCatalogReader().getMemberChildren(member);
                final Member[] tuple2 = tuple.clone();
                for (Member childMember : children) {
                    tuple2[k] = childMember;
                    resultList.addTuple(tuple2);
                    if (recursive) {
                        drillDownObj(evaluator, tuple2, memberSet, resultList);
                    }
                }
                break;
            }
        }
    }

    private TupleList drilldownMember(TupleList v0, TupleList v1, Evaluator evaluator) {
        assert v1.getArity() == 1;
        if (v0.isEmpty() || v1.isEmpty()) {
            return v0;
        }

        Set<Member> set1 = new HashSet<>(v1.slice(0));

        TupleList result = TupleCollections.createList(v0.getArity());
        int i = 0;
        int n = v0.size();
        final Member[] members = new Member[v0.getArity()];
        while (i < n) {
            List<Member> o = v0.get(i++);
            o.toArray(members);
            result.add(o);
            drillDownObj(evaluator, members, set1, result);
        }
        return result;
    }

}
