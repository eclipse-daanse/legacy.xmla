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
package org.eclipse.daanse.olap.function.def.headtail;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.type.Type;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.TupleCollections;

public class TailCalc extends AbstractListCalc {

    public TailCalc(Type type, TupleListCalc tupleListCalc, IntegerCalc integerCalc) {
        super(type, tupleListCalc, integerCalc);
    }

    @Override
    public TupleList evaluate(Evaluator evaluator) {
        final int savepoint = evaluator.savepoint();
        try {
            evaluator.setNonEmpty(false);
            TupleList list = getChildCalc(0, TupleListCalc.class).evaluate(evaluator);
            Integer count = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
            return tail(count, list);
        } finally {
            evaluator.restore(savepoint);
        }
    }

    private TupleList tail(final Integer count, final TupleList members) {
        assert members != null;
        final int memberCount = members.size();
        if (count >= memberCount) {
            return members;
        }
        if (count <= 0) {
            return TupleCollections.emptyList(members.getArity());
        }
        return members.subList(members.size() - count, members.size());
    }

}
