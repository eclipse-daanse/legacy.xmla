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
import org.eclipse.daanse.olap.calc.api.todo.TupleIterable;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;

public class NonNativeListCalc extends NonNativeCalc implements TupleListCalc {
    protected NonNativeListCalc(TupleListCalc parent, boolean highCardinality) {
        super(parent, highCardinality);
    }

    TupleListCalc parent() {
        return (TupleListCalc) parent;
    }

    @Override
    public TupleList evaluateList(Evaluator evaluator) {
        evaluator.setNativeEnabled(nativeEnabled);
        return parent().evaluateList(evaluator);
    }

    @Override
    public TupleIterable evaluateIterable(Evaluator evaluator) {
        return evaluateList(evaluator);
    }
}