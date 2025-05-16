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
package org.eclipse.daanse.olap.function.def.nativizeset;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;

public class NonNativeListCalc extends NonNativeCalc<TupleList> implements TupleListCalc {
    protected NonNativeListCalc(TupleListCalc parent, boolean highCardinality) {
        super(parent, highCardinality);
    }

    TupleListCalc parent() {
        return (TupleListCalc) parent;
    }

    @Override
    public TupleList evaluate(Evaluator evaluator) {
        evaluator.setNativeEnabled(nativeEnabled);
        return parent().evaluate(evaluator);
    }

//    @Override
//    public TupleIterable evaluateIterable(Evaluator evaluator) {
//        return evaluateList(evaluator);
//    }
}