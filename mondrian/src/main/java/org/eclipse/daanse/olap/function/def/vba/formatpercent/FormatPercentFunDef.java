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
package org.eclipse.daanse.olap.function.def.vba.formatpercent;

import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.DecimalType;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.constant.ConstantIntegerCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class FormatPercentFunDef  extends AbstractFunctionDefinition {


    public FormatPercentFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final Calc<?> expressionCalc = compiler.compileDouble(call.getArg(0));

        IntegerCalc numDigitsAfterDecimalCalc = null;
        IntegerCalc includeLeadingDigitCalc = null;
        IntegerCalc useParensForNegativeNumbersCalc = null;
        IntegerCalc groupDigitsCalc = null;

        if (call.getArgCount() == 1) {
            numDigitsAfterDecimalCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -1);
            includeLeadingDigitCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -1);
            useParensForNegativeNumbersCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -1);
            groupDigitsCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -1);
        }
        if (call.getArgCount() == 2) {
            numDigitsAfterDecimalCalc = compiler.compileInteger(call.getArg(1));
            includeLeadingDigitCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -1);
            useParensForNegativeNumbersCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -1);
            groupDigitsCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -1);
        }
        if (call.getArgCount() == 3) {
            numDigitsAfterDecimalCalc = compiler.compileInteger(call.getArg(1));
            includeLeadingDigitCalc = compiler.compileInteger(call.getArg(2));
            useParensForNegativeNumbersCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -1);
            groupDigitsCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -1);
        }

        if (call.getArgCount() == 4) {
            numDigitsAfterDecimalCalc = compiler.compileInteger(call.getArg(1));
            includeLeadingDigitCalc = compiler.compileInteger(call.getArg(2));
            useParensForNegativeNumbersCalc = compiler.compileInteger(call.getArg(3));
            groupDigitsCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), -1);
        }

        if (call.getArgCount() == 5) {
            numDigitsAfterDecimalCalc = compiler.compileInteger(call.getArg(1));
            includeLeadingDigitCalc = compiler.compileInteger(call.getArg(2));
            useParensForNegativeNumbersCalc = compiler.compileInteger(call.getArg(3));
            groupDigitsCalc = compiler.compileInteger(call.getArg(4));
        }

        return new FormatPercentCalc(call.getType(), expressionCalc, numDigitsAfterDecimalCalc, includeLeadingDigitCalc, useParensForNegativeNumbersCalc, groupDigitsCalc);
    }

}
