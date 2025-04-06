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
package org.eclipse.daanse.olap.function.def.excel.asin;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

import mondrian.olap.fun.FunUtil;

public class AsinhCalc extends AbstractProfilingNestedDoubleCalc {

    protected AsinhCalc(Type type, DoubleCalc doubleCalc) {
        super(type, doubleCalc);
    }

    @Override
    public Double evaluate(Evaluator evaluator) {
        Double x = getChildCalc(0, DoubleCalc.class).evaluate(evaluator);
        if (x == FunUtil.DOUBLE_NULL) {
            return null;
        }
        return Math.log(x + Math.sqrt(1.0 + (x * x)));
    }

}
