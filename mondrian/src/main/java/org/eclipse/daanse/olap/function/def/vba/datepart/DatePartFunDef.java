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

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.DecimalType;
import org.eclipse.daanse.olap.calc.base.constant.ConstantIntegerCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class DatePartFunDef  extends AbstractFunctionDefinition {


    public DatePartFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final StringCalc stringCalc = compiler.compileString(call.getArg(0));
        final DateTimeCalc dateTimeCalc = compiler.compileDateTime(call.getArg(1));
        IntegerCalc firstDayOfWeek = null;
        IntegerCalc firstWeekOfYear = null;
        if (call.getArgCount() == 2) {
            firstDayOfWeek = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), Calendar.SUNDAY);
            firstWeekOfYear = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), 1);
        }
        if (call.getArgCount() == 3) {
            firstDayOfWeek = compiler.compileInteger(call.getArg(3));
            firstWeekOfYear = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), 1);
        }
        if (call.getArgCount() == 4) {
            firstDayOfWeek = compiler.compileInteger(call.getArg(3));
            firstWeekOfYear = compiler.compileInteger(call.getArg(4));
        }
        return new DatePartCalc(call.getType(), stringCalc, dateTimeCalc, firstDayOfWeek, firstWeekOfYear);
    }

}
