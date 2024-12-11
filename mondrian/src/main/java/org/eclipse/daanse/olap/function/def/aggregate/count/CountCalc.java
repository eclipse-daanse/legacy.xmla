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
package org.eclipse.daanse.olap.function.def.aggregate.count;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedIntegerCalc;

import mondrian.olap.fun.FunUtil;

public class CountCalc extends AbstractProfilingNestedIntegerCalc {

    protected CountCalc(Type type, final TupleListCalc tupleListCalc) {
        super(type, tupleListCalc);
    }

    @Override
    public Integer evaluate(Evaluator evaluator) {
        TupleList list = getChildCalc(0, TupleListCalc.class).evaluateList(evaluator);
        return FunUtil.count(evaluator, list, true);
    }

}
