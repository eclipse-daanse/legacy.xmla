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

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.DecimalType;
import org.eclipse.daanse.olap.calc.base.constant.ConstantIntegerCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class FormatDateTimeFunDef  extends AbstractFunctionDefinition {


    public FormatDateTimeFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final DateTimeCalc dateCalc = compiler.compileDateTime(call.getArg(0));

        IntegerCalc namedFormatCalc = null;

        if (call.getArgCount() == 1) {
            namedFormatCalc = new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), 0);
        }
        if (call.getArgCount() == 2) {
            namedFormatCalc = compiler.compileInteger(call.getArg(1));
        }

        return new FormatDateTimeCalc(call.getType(), dateCalc, namedFormatCalc);
    }

}
