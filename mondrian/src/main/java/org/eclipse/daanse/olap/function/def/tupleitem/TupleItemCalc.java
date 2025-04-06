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
package org.eclipse.daanse.olap.function.def.tupleitem;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.TupleCalc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedMemberCalc;

import mondrian.olap.fun.FunUtil;

public class TupleItemCalc extends AbstractProfilingNestedMemberCalc{

    private final Member[] nullTupleMembers;
    
    public TupleItemCalc(Type type, TupleCalc tupleCalc, IntegerCalc indexCalc) {
        super(type, tupleCalc, indexCalc);
        this.nullTupleMembers =
                FunUtil.makeNullTuple((TupleType) tupleCalc.getType());

    }

    @Override
    public Member evaluate(Evaluator evaluator) {
        final Member[] members =
                getChildCalc(0, TupleCalc.class).evaluate(evaluator);
        assert members == null
            || members.length == nullTupleMembers.length;
        final Integer index = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        if (members == null) {
            return nullTupleMembers[index];
        }
        if (index >= members.length || index < 0) {
            return null;
        }
        return members[index];
    }

}
