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
package org.eclipse.daanse.olap.function.def.vba.timeserial;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDateTimeCalc;

public class TimeSerialCalc extends AbstractProfilingNestedDateTimeCalc {

    protected TimeSerialCalc(Type type, IntegerCalc hourCalc, IntegerCalc minuteCalc, IntegerCalc secondCalc) {
        super(type, hourCalc, minuteCalc, secondCalc);
    }

    @Override
    public Date evaluate(Evaluator evaluator) {
        Integer hour = getChildCalc(0, IntegerCalc.class).evaluate(evaluator);
        Integer minute = getChildCalc(1, IntegerCalc.class).evaluate(evaluator);
        Integer second = getChildCalc(2, IntegerCalc.class).evaluate(evaluator);
        final Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        return calendar.getTime();
    }

}
