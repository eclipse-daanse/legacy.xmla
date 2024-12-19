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
package org.eclipse.daanse.olap.function.def.set;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.todo.TupleIterable;
import org.eclipse.daanse.olap.calc.api.todo.TupleIteratorCalc;

import mondrian.calc.impl.AbstractIterCalc;

public class SetIterableCalc extends AbstractIterCalc{

    private final TupleIteratorCalc tupleIteratorCalc;
    
    protected SetIterableCalc(Type type, Calc<?> calc, final TupleIteratorCalc tupleIteratorCalc) {
        super(type, calc);
        this.tupleIteratorCalc = tupleIteratorCalc;
    }
    
    // name "Sublist..."
    @Override
    public TupleIterable evaluateIterable(Evaluator evaluator) {
        return tupleIteratorCalc.evaluateIterable(evaluator);
    }

}
