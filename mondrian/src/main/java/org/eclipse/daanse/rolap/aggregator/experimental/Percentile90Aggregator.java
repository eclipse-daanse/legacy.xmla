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

public class Percentile90Aggregator extends AbstractAggregator {

    private static final double P = 0.90;

    public static final Percentile90Aggregator INSTANCE = new Percentile90Aggregator();

    public Percentile90Aggregator() {
        super("percentile90", false);
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

        Collections.sort(values);
        int n = values.size();
        // Index-based approach
        double index = (n - 1) * P;
        int iLow = (int) Math.floor(index);
        int iHigh = (int) Math.ceil(index);
        if (iLow == iHigh) {
            // Exact match
            return values.get(iLow);
        } else {
            // Linear interpolation between positions iLow and iHigh
            double valLow = values.get(iLow);
            double valHigh = values.get(iHigh);
            double fraction = index - iLow;
            return valLow + fraction * (valHigh - valLow);
        }
    }

    @Override
    public boolean supportsFastAggregates(DataTypeJdbc dataType) {
        // Not easily rollup-friendly without special approximation
        return false;
    }
}
