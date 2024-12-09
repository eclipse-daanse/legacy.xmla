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
package org.eclipse.daanse.olap.function.def.aggregate.children;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.rolap.agg.Aggregator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.HierarchyCalc;

import mondrian.calc.impl.GenericCalc;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.calc.impl.ValueCalc;
import mondrian.olap.Property;
import mondrian.olap.fun.FunUtil;

public class AggregateChildrenCalc extends GenericCalc {

    protected AggregateChildrenCalc(Type type, HierarchyCalc hierarchyCalc, ValueCalc valueCalc) {
        super(type, hierarchyCalc, valueCalc);
    }

    @Override
    public Object evaluate(Evaluator evaluator) {
        Hierarchy hierarchy = getChildCalc(0, HierarchyCalc.class).evaluate(evaluator);
        return aggregateChildren(evaluator, hierarchy, getChildCalc(1, ValueCalc.class));
    }

    @Override
    public Calc<?>[] getChildCalcs() {
        return new Calc[] { getChildCalc(0, HierarchyCalc.class), getChildCalc(1, ValueCalc.class) };
    }

    Object aggregateChildren(Evaluator evaluator, Hierarchy hierarchy, final Calc<?> valueFunCall) {
        Member member = evaluator.getPreviousContext(hierarchy);
        List<Member> members = new ArrayList<>();
        evaluator.getSchemaReader().getParentChildContributingChildren(member.getDataMember(), hierarchy, members);
        Aggregator aggregator = (Aggregator) evaluator.getProperty(Property.AGGREGATION_TYPE.name, null);
        if (aggregator == null) {
            throw FunUtil.newEvalException(null, new StringBuilder("Could not find an aggregator in the current ")
                    .append("evaluation context").toString());
        }
        Aggregator rollup = aggregator.getRollup();
        if (rollup == null) {
            throw FunUtil.newEvalException(null, new StringBuilder("Don't know how to rollup aggregator '")
                    .append(aggregator).append("'").toString());
        }
        final int savepoint = evaluator.savepoint();
        try {
            return rollup.aggregate(evaluator, new UnaryTupleList(members), valueFunCall);
        } finally {
            evaluator.restore(savepoint);
        }
    }

}
