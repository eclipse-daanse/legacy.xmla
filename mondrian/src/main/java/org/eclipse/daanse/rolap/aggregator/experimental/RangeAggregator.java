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
public class RangeAggregator extends AbstractAggregator {

    public static final RangeAggregator INSTANCE = new RangeAggregator();

    public RangeAggregator() {
        super("range", false);
    }

    @Override
    public Object aggregate(Evaluator evaluator, TupleList members, Calc<?> exp) {
        Double minVal = null;
        Double maxVal = null;
        for (int i = 0; i < members.size(); i++) {
            evaluator.setContext(members.get(i));
            Object value = exp.evaluate(evaluator);
            if (value instanceof Number) {
                double d = ((Number) value).doubleValue();
                if (minVal == null || d < minVal) {
                    minVal = d;
                }
                if (maxVal == null || d > maxVal) {
                    maxVal = d;
                }
            }
        }
        if (minVal == null || maxVal == null) {
            return null;
        }
        return maxVal - minVal;
    }

    @Override
    public boolean supportsFastAggregates(DataTypeJdbc dataType) {
        return false;
    }
}
