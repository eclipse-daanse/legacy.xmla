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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MedianAggregator extends AbstractAggregator {

    public static final MedianAggregator INSTANCE = new MedianAggregator();

    public MedianAggregator() {
        super("median", false);
    }

    @Override
    public Object aggregate(Evaluator evaluator, TupleList members, Calc<?> exp) {
        // Collect numeric values
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            evaluator.setContext(members.get(i));
            Object val = exp.evaluate(evaluator);
            if (val instanceof Number) {
                values.add(((Number) val).doubleValue());
            }
        }

        if (values.isEmpty()) {
            return null;
        }

        // Sort the collected values
        Collections.sort(values);
        int size = values.size();
        if (size % 2 == 1) {
            // Odd count => exact middle
            return values.get(size / 2);
        } else {
            // Even count => average of the two central values
            double val1 = values.get((size / 2) - 1);
            double val2 = values.get(size / 2);
            return (val1 + val2) / 2.0;
        }
    }

    @Override
    public boolean supportsFastAggregates(DataTypeJdbc dataType) {
        // Typically cannot compute median from pre-aggregated data alone
        return false;
    }
}
