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
package org.eclipse.daanse.olap.function.def.set.range;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.TupleCollections;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.olap.fun.FunUtil;

public class RangeCalc extends AbstractListCalc {

    protected RangeCalc(Type type, MemberCalc... calcs) {
        super(type, calcs);
    }

    @Override
    public TupleList evaluateList(Evaluator evaluator) {
        final Member member0 = getChildCalc(0, MemberCalc.class).evaluate(evaluator);
        final Member member1 = getChildCalc(1, MemberCalc.class).evaluate(evaluator);
        if (member0.isNull() || member1.isNull()) {
            return TupleCollections.emptyList(1);
        }
        if (member0.getLevel() != member1.getLevel()) {
            throw evaluator.newEvalException(RangeFunDef.functionMetaData, "Members must belong to the same level");
        }
        return new UnaryTupleList(FunUtil.memberRange(evaluator, member0, member1));
    }

}
