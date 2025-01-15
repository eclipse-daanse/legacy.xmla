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
package org.eclipse.daanse.olap.function.def.vba.right;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedStringCalc;

public class RightCalc extends AbstractProfilingNestedStringCalc {

    protected RightCalc(Type type, final StringCalc stringCalc, final IntegerCalc lengthCalc) {
        super(type, stringCalc, lengthCalc);
    }

    @Override
    public String evaluate(Evaluator evaluator) {
        String string = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        Integer length = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        return right(string, length);
    }

    public static String right(String string, int length) {
        final int stringLength = string.length();
        if (length >= stringLength) {
            return string;
        }
        return string.substring(stringLength - length, stringLength);
    }

}
