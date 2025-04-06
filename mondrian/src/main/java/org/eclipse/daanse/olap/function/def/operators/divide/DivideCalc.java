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

package org.eclipse.daanse.olap.function.def.operators.divide;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

import mondrian.olap.fun.FunUtil;

public class DivideCalc extends AbstractProfilingNestedDoubleCalc {

    private boolean nullDenominatorProducesNull;
    
    protected DivideCalc(Type type, final DoubleCalc calc0, final DoubleCalc calc1, boolean nullDenominatorProducesNull) {
        super(type, calc0, calc1);
        this.nullDenominatorProducesNull = nullDenominatorProducesNull;
    }

    
    @Override
    public Double evaluate(Evaluator evaluator) {
        final Double v0 = getChildCalc(0, DoubleCalc.class).evaluate(evaluator);
        final Double v1 = getChildCalc(1, DoubleCalc.class).evaluate(evaluator);
        // If the mondrian property
        //   mondrian.olap.NullOrZeroDenominatorProducesNull
        // is false(default), Null in denominator with numeric numerator
        // returns infinity. This is consistent with MSAS.
        //
        // If this property is true, Null or zero in denominator returns
        // Null. This is only used by certain applications and does not
        // conform to MSAS behavior.
        if (!nullDenominatorProducesNull) {
            if (v0 == FunUtil.DOUBLE_NULL || v0 == null) {
                return FunUtil.DOUBLE_NULL;
            } else if (v1 == FunUtil.DOUBLE_NULL || v1 == null) {
                // Null only in denominator returns Infinity.
                return Double.POSITIVE_INFINITY;
            } else {
                return v0 / v1;
            }
        } else {
            // Null in numerator or denominator returns
            // DoubleNull.
            if (v0 == FunUtil.DOUBLE_NULL || v1 == FunUtil.DOUBLE_NULL || v0 == null || v1 == null) {
                return FunUtil.DOUBLE_NULL;
            } else {
                return v0 / v1;
            }
        }
    }

}
