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
package org.eclipse.daanse.olap.function.def.cache;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.ExpCacheDescriptor;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.todo.TupleCursor;
import org.eclipse.daanse.olap.api.calc.todo.TupleIterable;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedTupleListCalc;

import mondrian.calc.impl.TupleCollections;

public class CacheGenericListCalc extends AbstractProfilingNestedTupleListCalc {
    private final ExpCacheDescriptor cacheDescriptor;

    protected CacheGenericListCalc(Type type, ExpCacheDescriptor cacheDescriptor) {
        super(type);
        this.cacheDescriptor = cacheDescriptor;
    }

    @Override
    public TupleList evaluate(Evaluator evaluator) {
        final Object o = evaluator.getCachedResult(cacheDescriptor);
        if (o instanceof TupleList tupleList) {
            return tupleList;
        }
        // Iterable
        final TupleIterable iterable = (TupleIterable) o;
        final TupleList tupleList = TupleCollections.createList(iterable.getArity());
        final TupleCursor cursor = iterable.tupleCursor();
        while (cursor.forward()) {
            tupleList.addCurrent(cursor);
        }
        return tupleList;
    }

    @Override
    public Calc<?>[] getChildCalcs() {
        return new Calc[] { cacheDescriptor.getCalc() };
    }

    @Override
    public ResultStyle getResultStyle() {
        // cached lists are immutable
        return ResultStyle.LIST;
    }
}
