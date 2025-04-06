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
package org.eclipse.daanse.olap.function.def.hierarchy.member;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.NamedSetExpression;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedMemberCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedTupleCalc;

public class NamedSetCurrentNestedTupleCalc extends AbstractProfilingNestedTupleCalc {
    private NamedSetExpression namedSetExpr;

    protected NamedSetCurrentNestedTupleCalc(Type type, final NamedSetExpression namedSetExpr, Calc<?>[] calcs) {
        super(type, calcs);
        this.namedSetExpr = namedSetExpr;
    }

    @Override
    public Member[] evaluate(Evaluator evaluator) {
        return namedSetExpr.getEval(evaluator).currentTuple();
    }

}
