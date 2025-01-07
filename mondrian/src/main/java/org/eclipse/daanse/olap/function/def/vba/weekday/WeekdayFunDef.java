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

import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.DecimalType;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.DateTimeCalc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.constant.ConstantIntegerCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class WeekdayFunDef  extends AbstractFunctionDefinition {


    public WeekdayFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final DateTimeCalc dateTimeCalc = compiler.compileDateTime(call.getArg(0));
        IntegerCalc firstDayOfWeek = null;
        if (call.getArgCount() == 1) {
            firstDayOfWeek = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), Calendar.SUNDAY);
        }
        if (call.getArgCount() == 2) {
            firstDayOfWeek = compiler.compileInteger(call.getArg(3));
        }
        return new WeekdayCalc(call.getType(), dateTimeCalc, firstDayOfWeek);
    }

}
