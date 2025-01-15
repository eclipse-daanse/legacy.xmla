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
package org.eclipse.daanse.olap.function.def.vba.space;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedStringCalc;
import org.eclipse.daanse.olap.function.def.vba.string.StringCalc;

public class SpaceCalc extends AbstractProfilingNestedStringCalc {

    protected SpaceCalc(Type type, final IntegerCalc numberCalc) {
        super(type, numberCalc);
    }

    @Override
    public String evaluate(Evaluator evaluator) {
        Integer number = getChildCalc(0, IntegerCalc.class).evaluate(evaluator);
        return space(number);
    }

    public static String space(int number) {
        return StringCalc.string(number, ' ');
    }


}
