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
package org.eclipse.daanse.olap.function.def.vba.timevalue;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDateTimeCalc;

public class TimeValueCalc extends AbstractProfilingNestedDateTimeCalc {

    protected TimeValueCalc(Type type, final DateTimeCalc dateCalc) {
        super(type, dateCalc);
    }

    @Override
    public Date evaluate(Evaluator evaluator) {
        Date time = getChildCalc(0, DateTimeCalc.class).evaluate(evaluator);
        final Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTime(time);
        calendar.set(1970, 0, 1);
        return calendar.getTime();
    }

}
