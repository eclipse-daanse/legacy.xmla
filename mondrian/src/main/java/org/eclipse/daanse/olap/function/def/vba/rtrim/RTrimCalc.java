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
package org.eclipse.daanse.olap.function.def.vba.rtrim;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedStringCalc;

public class RTrimCalc extends AbstractProfilingNestedStringCalc {

    protected RTrimCalc(Type type, final StringCalc strCalc) {
        super(type, strCalc);
    }

    @Override
    public String evaluate(Evaluator evaluator) {
        String string = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        return rTrim(string);
    }

    public static String rTrim(String string) {
        int i = string.length() - 1;
        while (i >= 0) {
            if (string.charAt(i) > ' ') {
                break;
            }
            i--;
        }
        return string.substring(0, i + 1);
    }

}
