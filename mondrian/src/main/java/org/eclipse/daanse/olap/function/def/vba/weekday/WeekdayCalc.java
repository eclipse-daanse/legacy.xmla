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
package org.eclipse.daanse.olap.function.def.vba.weekday;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.DateTimeCalc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedIntegerCalc;

public class WeekdayCalc extends AbstractProfilingNestedIntegerCalc {
    protected WeekdayCalc(Type type, DateTimeCalc dateTimeCalc,
            IntegerCalc firstDayOfWeek) {
        super(type, dateTimeCalc, firstDayOfWeek);
    }

    @Override
    public Integer evaluate(Evaluator evaluator) {
        Date date = getChildCalc(0, DateTimeCalc.class).evaluate(evaluator);
        int firstDayOfWeek = getChildCalc(2, IntegerCalc.class).evaluate(evaluator);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int weekday = calendar.get(Calendar.DAY_OF_WEEK);
        // adjust for start of week
        weekday -= (firstDayOfWeek - 1);
        // bring into range 1..7
        weekday = (weekday + 6) % 7 + 1;
        return weekday;
    }


}
