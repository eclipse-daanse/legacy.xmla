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
package org.eclipse.daanse.olap.function.def.vba.round;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

public class RoundCalc extends AbstractProfilingNestedDoubleCalc {
    protected RoundCalc(Type type, DoubleCalc numberCalc, final IntegerCalc numDigitsAfterDecimalCalc) {
        super(type, numberCalc, numDigitsAfterDecimalCalc);
    }

    @Override
    public Double evaluate(Evaluator evaluator) {
        Double number = getChildCalc(0, DoubleCalc.class).evaluate(evaluator);
        Integer numDigitsAfterDecimal = getChildCalc(0, IntegerCalc.class).evaluate(evaluator);

        if (numDigitsAfterDecimal == 0) {
            return (double) Math.round(number);
        }
        final double shift = Math.pow(10d, numDigitsAfterDecimal);
        double numberScaled = number * shift;
        double resultScaled = Math.round(numberScaled);
        return resultScaled / shift;
    }

}
