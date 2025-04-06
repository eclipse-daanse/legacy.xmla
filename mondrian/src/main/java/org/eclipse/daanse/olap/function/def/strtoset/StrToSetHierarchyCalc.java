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
package org.eclipse.daanse.olap.function.def.strtoset;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.type.Type;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.olap.exceptions.EmptyExpressionWasSpecifiedException;
import mondrian.olap.fun.FunUtil;

public class StrToSetHierarchyCalc extends AbstractListCalc {

    private Hierarchy hierarchy;

    protected StrToSetHierarchyCalc(Type type, final StringCalc stringCalc, final Hierarchy hierarchy) {
        super(type, stringCalc);
        this.hierarchy = hierarchy;
    }

    @Override
    public TupleList evaluateList(Evaluator evaluator) {
        String string = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        if (string == null) {
            throw FunUtil.newEvalException(new EmptyExpressionWasSpecifiedException());
        }
        return new UnaryTupleList(FunUtil.parseMemberList(evaluator, string, hierarchy));
    }

}
