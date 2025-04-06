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

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class TimeSerialFunDef  extends AbstractFunctionDefinition {

    static FunctionOperationAtom atom = new FunctionOperationAtom("TimeSerial");
    static String description = """
        Returns a Variant (Date) containing the time for a specific hour,
        minute, and second.""";
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, description,
            DataType.DATE_TIME, new FunctionParameterR[] { new FunctionParameterR( DataType.INTEGER, "Hour" ),
                    new FunctionParameterR( DataType.INTEGER, "Minute" ), new FunctionParameterR( DataType.INTEGER, "Second" ) });

    public TimeSerialFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final IntegerCalc hourCalc = compiler.compileInteger(call.getArg(0));
        final IntegerCalc minuteCalc = compiler.compileInteger(call.getArg(1));
        final IntegerCalc secondCalc = compiler.compileInteger(call.getArg(2));
        return new TimeSerialCalc(call.getType(), hourCalc, minuteCalc, secondCalc);
    }

}
