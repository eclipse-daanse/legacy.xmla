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
package org.eclipse.daanse.olap.function.def.vba.dateadd;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.DateTimeCalc;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDateTimeCalc;

public class DateAddCalc extends AbstractProfilingNestedDateTimeCalc {

    public static final long MILLIS_IN_A_DAY = 24L * 60 * 60 * 1000;
    
    protected DateAddCalc(Type type, StringCalc stringCalc, DoubleCalc doubleCalc, DateTimeCalc dateTimeCalc ) {
        super(type, stringCalc, doubleCalc, dateTimeCalc);
    }

    @Override
    public Date evaluate(Evaluator evaluator) {
        String intervalName = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        Double number = getChildCalc(1, DoubleCalc.class).evaluate(evaluator);
        Date date = getChildCalc(2, DateTimeCalc.class).evaluate(evaluator);
        
        Interval interval = Interval.valueOf(intervalName);
        final double floor = Math.floor(number);

        // We use the local calendar here. This method will therefore return
        // different results in different locales: it depends whether the
        // initial date and the final date are in DST.
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if (floor != number) {
            final double ceil = Math.ceil(number);
            interval.add(calendar, (int) ceil);
            final long ceilMillis = calendar.getTimeInMillis();

            calendar.setTime(date);
            interval.add(calendar, (int) floor);
            final long floorMillis = calendar.getTimeInMillis();

            final long amount =
                (long)
                    (((double) (ceilMillis - floorMillis)) * (number - floor));
            calendar.add(
                Calendar.DAY_OF_YEAR,
                (int) (amount / MILLIS_IN_A_DAY));
            calendar.add(
                Calendar.MILLISECOND, (int)
                (amount % MILLIS_IN_A_DAY));
        } else {
            interval.add(calendar, (int) floor);
        }
        return calendar.getTime();
    }

}
