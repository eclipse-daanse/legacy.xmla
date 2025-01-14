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
package org.eclipse.daanse.olap.function.def.vba.replace;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedStringCalc;

public class ReplaceCalc extends AbstractProfilingNestedStringCalc {
    protected ReplaceCalc(Type type, final StringCalc expressionCalc, final StringCalc findCalc,
            final StringCalc replaceCalc, final IntegerCalc startCalc, final IntegerCalc countCalc,
            final IntegerCalc compareCalc) {
        super(type, expressionCalc, findCalc, replaceCalc, startCalc, countCalc, compareCalc);
    }

    @Override
    public String evaluate(Evaluator evaluator) {
        String expression = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        String find = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        String replace = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        Integer start = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        Integer count = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        Integer compare = getChildCalc(1, IntegerCalc.class).evaluate(evaluator); // compare is currently ignored
        return replace(expression, find, replace, start, count);
    }

    public static String replace(String expression, String find, String replace, int start /* default 1 */,
            int count /* default -1 */) {
        final StringBuilder buf = new StringBuilder(expression);
        int i = 0;
        int pos = start - 1;
        while (true) {
            if (i++ == count) {
                break;
            }
            final int j = buf.indexOf(find, pos);
            if (j == -1) {
                break;
            }
            buf.replace(j, j + find.length(), replace);
            pos = j + replace.length();
        }
        return buf.toString();
    }

}
