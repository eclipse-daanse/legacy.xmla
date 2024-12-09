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
package org.eclipse.daanse.olap.function.def.numeric.value;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;

import mondrian.calc.impl.GenericCalc;

public class ValueCalc extends GenericCalc {

    protected ValueCalc(Type type, final MemberCalc memberCalc) {
        super(type, memberCalc);
    }

    @Override
    public Object evaluate(Evaluator evaluator) {
        Member member = getChildCalc(0, MemberCalc.class).evaluate(evaluator);
        final int savepoint = evaluator.savepoint();
        evaluator.setContext(member);
        try {
            return evaluator.evaluateCurrent();
        } finally {
            evaluator.restore(savepoint);
        }
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        if (super.dependsOn(hierarchy)) {
            return true;
        }
        return (!(getChildCalc(0, MemberCalc.class).getType().usesHierarchy(hierarchy, true)));
    }

    @Override
    public Calc<?>[] getChildCalcs() {
        return new Calc[] { getChildCalc(0, MemberCalc.class) };
    }

}
