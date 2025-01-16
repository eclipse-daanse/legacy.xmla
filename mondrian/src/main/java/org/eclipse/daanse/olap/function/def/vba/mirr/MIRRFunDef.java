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
package org.eclipse.daanse.olap.function.def.vba.mirr;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class MIRRFunDef  extends AbstractFunctionDefinition {

    static FunctionOperationAtom atom = new FunctionOperationAtom("MIRR");
    static String description = """
        Returns a Double specifying the modified internal rate of return for
        a series of periodic cash flows (payments and receipts).""";
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, description,
            DataType.NUMERIC, new FunctionParameterR[] {
                    new FunctionParameterR( DataType.ARRAY, "ValueArray" ),
                    new FunctionParameterR( DataType.NUMERIC, "FinanceRate" ),
                    new FunctionParameterR( DataType.NUMERIC, "ReinvestRate" ) });

    public MIRRFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final Calc<?> valueArrayCalc = compiler.compileScalar(call.getArg(0), false);
        final DoubleCalc financeRateCalc = compiler.compileDouble(call.getArg(1));
        final DoubleCalc reinvestRateCalc = compiler.compileDouble(call.getArg(2));
        return new MIRRCalc(call.getType(), valueArrayCalc, financeRateCalc, reinvestRateCalc);
    }

}
