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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModeAggregator extends AbstractAggregator {

    public static final ModeAggregator INSTANCE = new ModeAggregator();

    public ModeAggregator() {
        super("mode", false);
    }

    @Override
    public Object aggregate(Evaluator evaluator, TupleList members, Calc<?> exp) {
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            evaluator.setContext(members.get(i));
            Object val = exp.evaluate(evaluator);
            if (val != null) {
                values.add(val);
            }
        }
        if (values.isEmpty()) {
            return null;
        }

        // Count frequencies
        Map<Object, Integer> frequency = new HashMap<>();
        for (Object v : values) {
            frequency.put(v, frequency.getOrDefault(v, 0) + 1);
        }

        // Find the value with the highest frequency
        Object modeValue = null;
        int maxCount = 0;
        for (Map.Entry<Object, Integer> entry : frequency.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                modeValue = entry.getKey();
            }
        }
        return modeValue;
    }

    @Override
    public boolean supportsFastAggregates(DataTypeJdbc dataType) {
        // Cannot typically compute mode from partial aggregates
        return false;
    }
}
