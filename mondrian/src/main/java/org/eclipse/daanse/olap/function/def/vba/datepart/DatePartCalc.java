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
package org.eclipse.daanse.olap.function.def.vba.datepart;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.DateTimeCalc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedIntegerCalc;
import org.eclipse.daanse.olap.function.def.vba.dateadd.Interval;
import org.eclipse.daanse.olap.function.def.vba.datediff.FirstWeekOfYear;

public class DatePartCalc extends AbstractProfilingNestedIntegerCalc {
    protected DatePartCalc(Type type, StringCalc stringCalc, DateTimeCalc dateTimeCalc,
            IntegerCalc firstDayOfWeek, IntegerCalc firstWeekOfYear) {
        super(type, stringCalc, dateTimeCalc, firstDayOfWeek, firstWeekOfYear);
    }

    @Override
    public Integer evaluate(Evaluator evaluator) {
        String intervalName = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        Date date = getChildCalc(1, DateTimeCalc.class).evaluate(evaluator);
        int firstDayOfWeek = getChildCalc(2, IntegerCalc.class).evaluate(evaluator);
        int fwofy = getChildCalc(3, IntegerCalc.class).evaluate(evaluator);
        FirstWeekOfYear firstWeekOfYear = FirstWeekOfYear.values()[fwofy];
        return datePart(intervalName, date, firstDayOfWeek, firstWeekOfYear);
    }

    private static int datePart(String intervalName, Date date, int firstDayOfWeek, FirstWeekOfYear firstWeekOfYear) {
        Interval interval = Interval.valueOf(intervalName);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if (Interval.w.equals(interval) || Interval.ww.equals(interval)) {
            // firstWeekOfYear and firstDayOfWeek only matter for 'w' and 'ww'
            firstWeekOfYear.apply(calendar);
            calendar.setFirstDayOfWeek(firstDayOfWeek);
        }
        return interval.datePart(calendar);
    }

}
