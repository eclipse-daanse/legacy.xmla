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
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.function.def.vba.mirr;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

public class MIRRCalc extends AbstractProfilingNestedDoubleCalc {

    protected MIRRCalc(Type type, final Calc<?> valueArrayCalc, final DoubleCalc financeRateCalc, final DoubleCalc reinvestRateCalc) {
        super(type, valueArrayCalc, financeRateCalc, reinvestRateCalc);
    }

    @Override
    public Double evaluate(Evaluator evaluator) {
        Double[] valueArray = (Double[]) getChildCalc(0).evaluate(evaluator);
        Double financeRate = getChildCalc(1, DoubleCalc.class).evaluate(evaluator);
        Double reinvestRate = getChildCalc(2, DoubleCalc.class).evaluate(evaluator);
        return mirr(valueArray, financeRate, reinvestRate);
    }

    public static double mirr(
            Double[] valueArray,
            Double financeRate,
            Double reinvestRate)
        {
            // based on
            // http://en.wikipedia.org/wiki/Modified_Internal_Rate_of_Return
            double reNPV = 0.0;
            double fiNPV = 0.0;
            for (int j = 0; j < valueArray.length; j++) {
                if (valueArray[j] > 0) {
                    reNPV += valueArray[j] / Math.pow(1.0 + reinvestRate, j);
                } else {
                    fiNPV += valueArray[j] / Math.pow(1.0 + financeRate, j);
                }
            }

            double ratio = (fiNPV * (1 + financeRate)) == 0 ? 0 :
                (- reNPV * Math.pow(1 + reinvestRate, valueArray.length))
                / (fiNPV * (1 + financeRate));

            return Math.pow(ratio, 1.0 / (valueArray.length - 1)) - 1.0;
        }

}
