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
package org.eclipse.daanse.olap.function.def.udf.matches;

import java.util.regex.Pattern;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedBooleanCalc;

public class MatchesCalc extends AbstractProfilingNestedBooleanCalc {

    protected MatchesCalc(Type type, StringCalc s0, StringCalc s1) {
        super(type, s0, s1);
    }

    @Override
    public Boolean evaluate(Evaluator evaluator) {
        final StringCalc s0 = getChildCalc(0, StringCalc.class);
        final StringCalc s1 = getChildCalc(1, StringCalc.class);
        String str0 = s0.evaluate(evaluator);
        String str1 = s1.evaluate(evaluator);
        return Boolean.valueOf(Pattern.matches(str1, str0));

    }

}
