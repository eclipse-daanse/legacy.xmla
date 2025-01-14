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
package org.eclipse.daanse.olap.function.def.vba.formatdatetime;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.DateTimeCalc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedStringCalc;

public class FormatDateTimeCalc extends AbstractProfilingNestedStringCalc {
    protected FormatDateTimeCalc(Type type, final DateTimeCalc dateCalc, final IntegerCalc namedFormatCalc) {
        super(type, dateCalc, namedFormatCalc);
    }

    @Override
    public String evaluate(Evaluator evaluator) {
        Date date = getChildCalc(0, DateTimeCalc.class).evaluate(evaluator);
        Integer namedFormat = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        return formatDateTime(date, namedFormat);
    }

    public static String formatDateTime(
            Date date,
            int namedFormat /* default 0, GeneralDate */)
        {
            // todo: test
            // todo: how do we support VB Constants? Strings or Ints?
            switch (namedFormat) {
                // vbLongDate, 1
                // Display a date using the long date format specified in your
                // computer's regional settings.

            case 1:
                return DateFormat.getDateInstance(DateFormat.LONG).format(date);

                // vbShortDate, 2
                // Display a date using the short date format specified in your
                // computer's regional settings.
            case 2:
                return DateFormat.getDateInstance(DateFormat.SHORT).format(date);

                // vbLongTime, 3
                // Display a time using the time format specified in your computer's
                // regional settings.
            case 3:
                return DateFormat.getTimeInstance(DateFormat.LONG).format(date);

                // vbShortTime, 4
                // Display a time using the 24-hour format (hh:mm).
            case 4:
                return DateFormat.getTimeInstance(DateFormat.SHORT).format(date);

                // vbGeneralDate, 0
                // Display a date and/or time. If there is a date part,
                // display it as a short date. If there is a time part,
                // display it as a long time. If present, both parts are
                // displayed.
                //
                // todo: how do we determine if there is a "time part" in java?
            case 0:
            default:
                return DateFormat.getDateTimeInstance().format(date);
            }
        }

}
