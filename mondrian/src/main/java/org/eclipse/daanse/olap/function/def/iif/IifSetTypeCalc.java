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
package org.eclipse.daanse.olap.function.def.iif;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.type.Type;

import mondrian.calc.impl.GenericIterCalc;

public class IifSetTypeCalc extends GenericIterCalc {

    private final BooleanCalc booleanCalc;
    private final Calc<?> calc1;
    private final Calc<?> calc2;

    protected IifSetTypeCalc(Type type, BooleanCalc booleanCalc, Calc<?> calc1, Calc<?> calc2) {
        super(type);
        this.booleanCalc = booleanCalc;
        this.calc1 = calc1;
        this.calc2 = calc2;
    }

    @Override
    public Object evaluate(Evaluator evaluator) {
        final boolean b = booleanCalc.evaluate(evaluator);
        Calc<?> calc = b ? calc1 : calc2;
        return calc.evaluate(evaluator);
    }

    @Override
    public Calc<?>[] getChildCalcs() {
        return new Calc[] { booleanCalc, calc1, calc2 };
    }

    @Override
    public ResultStyle getResultStyle() {
        return calc1.getResultStyle();
    }

}
