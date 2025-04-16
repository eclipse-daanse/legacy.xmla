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
import java.util.List;

public class StdDevAggregator extends AbstractAggregator {

    public static final StdDevAggregator INSTANCE = new StdDevAggregator();

    public StdDevAggregator() {
        super("stddev", false);
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

        int n = values.size();
        if (n == 0) {
            return null;
        }

        // Calculate mean
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        double mean = sum / n;

        // Sum of squared deviations
        double sumSq = 0.0;
        for (double v : values) {
            double diff = v - mean;
            sumSq += diff * diff;
        }

        // Population variance
        double variance = sumSq / n;
        // Standard deviation = sqrt(variance)
        return Math.sqrt(variance);
    }

    @Override
    public boolean supportsFastAggregates(DataTypeJdbc dataType) {
        return false;
    }
}