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
package org.eclipse.daanse.olap.function.def.subset;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.TupleCollections;

public class SubsetCalc extends AbstractListCalc{

    public SubsetCalc(Type type, TupleListCalc tupleListCalc, IntegerCalc startCalc, IntegerCalc countCalc) {
        super(type, tupleListCalc, startCalc, countCalc);
    }

    @Override
    public TupleList evaluateList(Evaluator evaluator) {
        TupleListCalc tupleListCalc = getChildCalc(0, TupleListCalc.class);
        IntegerCalc startCalc = getChildCalc(1, IntegerCalc.class);
        IntegerCalc countCalc = getChildCalc(2, IntegerCalc.class);
        final int savepoint = evaluator.savepoint();
        try {
            evaluator.setNonEmpty(false);
            final TupleList list = tupleListCalc.evaluateList(evaluator);
            final Integer start = startCalc.evaluate(evaluator);
            int end;
            if (countCalc != null) {
                final Integer count = countCalc.evaluate(evaluator);
                end = start + count;
            } else {
                end = list.size();
            }
            if (end > list.size()) {
                end = list.size();
            }
            if (start >= end || start < 0) {
                return TupleCollections.emptyList(list.getArity());
            }
            if (start == 0 && end == list.size()) {
                return list;
            }
            assert 0 <= start;
            assert start < end;
            assert end <= list.size();
            return list.subList(start, end);
        } finally {
            evaluator.restore(savepoint);
        }
    }

}
