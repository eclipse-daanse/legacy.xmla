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

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.DateTimeCalc;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class DateAddFunDef  extends AbstractFunctionDefinition {

    static FunctionOperationAtom atom = new FunctionOperationAtom("DateAdd");
    static String description = """
        Returns a Variant (Date) containing a date to which a specified time
        interval has been added.""";
    static String signature = "DateAdd(interval, number, date)";
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, description,
            signature, DataType.DATE_TIME, new FunctionParameterR[] { new FunctionParameterR( DataType.STRING, "IntervalName" ), new FunctionParameterR( DataType.NUMERIC, "Number" ), new FunctionParameterR( DataType.DATE_TIME, "Date" ) });

    public DateAddFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final StringCalc stringCalc = compiler.compileString(call.getArg(0));
        final DoubleCalc doubleCalc = compiler.compileDouble(call.getArg(1));
        final DateTimeCalc dateTimeCalc = compiler.compileDateTime(call.getArg(2));
        return new DateAddCalc(call.getType(), stringCalc, doubleCalc, dateTimeCalc);
    }

}
