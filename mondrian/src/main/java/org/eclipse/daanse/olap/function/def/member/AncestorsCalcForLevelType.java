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
package org.eclipse.daanse.olap.function.def.member;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.LevelCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.TupleCollections;
import mondrian.olap.fun.FunUtil;

public class AncestorsCalcForLevelType extends AbstractListCalc{

    protected AncestorsCalcForLevelType(Type type, final MemberCalc memberCalc, final LevelCalc levelCalc ) {
        super(type, memberCalc, levelCalc);
    }

    @Override
    public TupleList evaluateList(Evaluator evaluator) {
        Level level = getChildCalc(1, LevelCalc.class).evaluate(evaluator);
        Member member = getChildCalc(0, MemberCalc.class).evaluate(evaluator);
        int distance = member.getDepth() - level.getDepth();
        List<Member> ancestors = new ArrayList<>();
        for (int curDist = 1; curDist <= distance; curDist++) {
            ancestors.add(FunUtil.ancestor(evaluator, member, curDist, null));
        }
        return TupleCollections.asTupleList(ancestors);
    }

}
