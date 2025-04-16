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

public class MovingAverage3Aggregator extends AbstractAggregator {

    public static final MovingAverage3Aggregator INSTANCE = new MovingAverage3Aggregator();

    public MovingAverage3Aggregator() {
        super("movingAverage3", false);
    }

    @Override
    public Object aggregate(Evaluator evaluator, TupleList members, Calc<?> exp) {
        // We'll assume 'members' is already in the time/sequence order.
        // Then we sum all values and just divide by 3 for demonstration.
        double totalSum = 0.0;
        int count = 0;

        for (int i = 0; i < members.size(); i++) {
            evaluator.setContext(members.get(i));
            Object val = exp.evaluate(evaluator);
            if (val instanceof Number) {
                totalSum += ((Number) val).doubleValue();
                count++;
            }
        }

        if (count == 0) {
            return null;
        }

        // A naive approach: "average of the entire set / 3"
        // Real-world logic would handle time windows carefully.
        return totalSum / 3.0;
    }

    @Override
    public boolean supportsFastAggregates(DataTypeJdbc dataType) {
        // Typically not, since we need a moving window
        return false;
    }
}
