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
package org.eclipse.daanse.olap.function.def.vba.formatcurrency;

import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.BooleanType;
import org.eclipse.daanse.olap.api.type.DecimalType;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.calc.api.BooleanCalc;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.constant.ConstantBooleanCalc;
import org.eclipse.daanse.olap.calc.base.constant.ConstantDoubleCalc;
import org.eclipse.daanse.olap.calc.base.constant.ConstantIntegerCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class FormatCurrencyFunDef  extends AbstractFunctionDefinition {


    public FormatCurrencyFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final Calc<?> expression = compiler.compile(call.getArg(0));

        IntegerCalc numDigitsAfterDecimal = null;;
        IntegerCalc includeLeadingDigitCalc = null;
        IntegerCalc useParensForNegativeNumbersCalc = null;
        IntegerCalc groupDigitsCalc = null;

        if (call.getArgCount() == 1) {
            numDigitsAfterDecimal = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -1);
            includeLeadingDigitCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -2);
            useParensForNegativeNumbersCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -2);
            groupDigitsCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -2);
        }

        if (call.getArgCount() == 2) {
            numDigitsAfterDecimal = compiler.compileInteger(call.getArg(1));
            includeLeadingDigitCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -2);
            useParensForNegativeNumbersCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -2);
            groupDigitsCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -2);
        }
        if (call.getArgCount() == 3) {
            numDigitsAfterDecimal = compiler.compileInteger(call.getArg(1));
            includeLeadingDigitCalc = compiler.compileInteger(call.getArg(2));
            useParensForNegativeNumbersCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -2);
            groupDigitsCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -2);
        }
        if (call.getArgCount() == 4) {
            numDigitsAfterDecimal = compiler.compileInteger(call.getArg(1));
            includeLeadingDigitCalc = compiler.compileInteger(call.getArg(2));
            useParensForNegativeNumbersCalc = compiler.compileInteger(call.getArg(3));
            groupDigitsCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -2);
        }
        if (call.getArgCount() == 5) {
            numDigitsAfterDecimal = compiler.compileInteger(call.getArg(1));
            includeLeadingDigitCalc = compiler.compileInteger(call.getArg(2));
            useParensForNegativeNumbersCalc = compiler.compileInteger(call.getArg(3));
            groupDigitsCalc = compiler.compileInteger(call.getArg(4));
        }
        return new FormatCurrencyCalc(call.getType(), expression, numDigitsAfterDecimal, includeLeadingDigitCalc, useParensForNegativeNumbersCalc, groupDigitsCalc);
    }

}
