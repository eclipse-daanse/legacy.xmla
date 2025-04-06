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
package org.eclipse.daanse.olap.function.def.vba.trim;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedStringCalc;

public class TrimCalc extends AbstractProfilingNestedStringCalc {

    protected TrimCalc(Type type, final StringCalc stringCalc) {
        super(type, stringCalc);
    }

    @Override
    public String evaluate(Evaluator evaluator) {
        String string = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        return trim(string);
    }

    public static String trim(String string) {
        // JDK has a method for trim, but not ltrim or rtrim
        return string.trim();
    }

}
