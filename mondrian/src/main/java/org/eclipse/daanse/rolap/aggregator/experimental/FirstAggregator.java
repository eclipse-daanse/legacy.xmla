/*
* Copyright (c) 2025 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   SmartCity Jena - initial
*/
package org.eclipse.daanse.rolap.aggregator.experimental;

import org.eclipse.daanse.olap.api.DataTypeJdbc;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.rolap.aggregator.AbstractAggregator;

public class FirstAggregator extends AbstractAggregator {

    public static final FirstAggregator INSTANCE = new FirstAggregator();

    public FirstAggregator() {
        super("first", false);
    }

    @Override
    public Object aggregate(Evaluator evaluator, TupleList members, Calc<?> exp) {
        // We iterate in the natural order in which the members appear in the TupleList.
        for (int i = 0; i < members.size(); i++) {
            evaluator.setContext(members.get(i));
            Object value = exp.evaluate(evaluator);
            // Return the first encountered non-null value
            if (value != null) {
                return value;
            }
        }
        // If everything is null or the collection is empty, return null
        return null;
    }

    @Override
    public boolean supportsFastAggregates(DataTypeJdbc dataType) {
        // Usually no, because we need the actual "first" item, which
        // cannot be computed by typical pre-aggregation statistics.
        return false;
    }
}
