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
package org.eclipse.daanse.olap.function.def.drilldownlevel;

import java.util.HashMap;
import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.TupleCollections;

public class DrilldownLevelWithIndexCalc extends AbstractListCalc {

    private final int arity;
    private final boolean includeCalcMembers;

    public DrilldownLevelWithIndexCalc(Type type, TupleListCalc tupleListCalc, IntegerCalc indexCalc, final int arity,
            final boolean includeCalcMembers) {
        super(type, tupleListCalc, indexCalc);
        this.arity = arity;
        this.includeCalcMembers = includeCalcMembers;
    }

    @Override
    public TupleList evaluate(Evaluator evaluator) {
        TupleListCalc tupleListCalc = getChildCalc(0, TupleListCalc.class);
        IntegerCalc indexCalc = getChildCalc(1, IntegerCalc.class);
        TupleList list = tupleListCalc.evaluate(evaluator);
        if (list.isEmpty()) {
            return list;
        }
        final Integer index = indexCalc.evaluate(evaluator);
        if (index < 0 || index >= arity) {
            return list;
        }
        HashMap<Member, List<Member>> calcMembersByParent = DrilldownLevelCalc
                .getCalcMembersByParent(list.get(0).get(index).getHierarchy(), evaluator, includeCalcMembers);
        TupleList result = TupleCollections.createList(arity);
        final CatalogReader schemaReader = evaluator.getCatalogReader();
        final Member[] tupleClone = new Member[arity];
        for (List<Member> tuple : list) {
            result.add(tuple);
            final List<Member> children = schemaReader.getMemberChildren(tuple.get(index));
            for (Member child : children) {
                tuple.toArray(tupleClone);
                tupleClone[index] = child;
                result.addTuple(tupleClone);
            }
            List<Member> childrenCalcMembers = calcMembersByParent.get(tuple.get(index));
            if (childrenCalcMembers != null) {
                for (Member childMember : childrenCalcMembers) {
                    tuple.toArray(tupleClone);
                    tupleClone[index] = childMember;
                    result.addTuple(tupleClone);
                }
            }
        }
        return result;
    }

}
