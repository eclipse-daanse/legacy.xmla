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
package org.eclipse.daanse.olap.function.def.vba.instr;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedIntegerCalc;

import mondrian.olap.InvalidArgumentException;

public class InStrCalc extends AbstractProfilingNestedIntegerCalc {
    protected InStrCalc(Type type, final IntegerCalc startCalc, final StringCalc stringCheckCalc, final StringCalc stringMatchCalc, final IntegerCalc compareCalc) {
        super(type, startCalc, stringCheckCalc, stringMatchCalc, compareCalc);
    }

    @Override
    public Integer evaluate(Evaluator evaluator) {
        Integer start = getChildCalc(0, IntegerCalc.class).evaluate(evaluator);
        String stringCheck = getChildCalc(1, StringCalc.class).evaluate(evaluator);
        String stringMatch = getChildCalc(2, StringCalc.class).evaluate(evaluator);
        Integer compare = getChildCalc(3, IntegerCalc.class).evaluate(evaluator);
        return inStr(start, stringCheck, stringMatch, compare);
    }

    public static int inStr(
            int start /* default 1 */,
            String stringCheck,
            String stringMatch,
            int compare /* default BinaryCompare */)
        {
            // todo: implement binary vs. text compare
            if (start == 0 || start < -1) {
                throw new InvalidArgumentException(
                    "start must be -1 or a location in the string to start");
            }
            String lwStringCheck = stringCheck;
            String lwStringMatch = stringMatch;
            if(!mondrian.olap.SystemWideProperties.instance().CaseSensitiveMdxInstr) {
                if(stringCheck != null) {
                    lwStringCheck = stringCheck.toLowerCase();
                }
                if(stringMatch != null) {
                    lwStringMatch = stringMatch.toLowerCase();
                }
            }
            if (start != -1) {
                return lwStringCheck == null ? 0 : lwStringCheck.indexOf(lwStringMatch, start - 1) + 1;
            } else {
                return lwStringCheck == null ? 0 : lwStringCheck.indexOf(lwStringMatch) + 1;
            }
        }

}
