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
package org.eclipse.daanse.olap.function.def.vba.syd;

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

public class SYDFunDef  extends AbstractFunctionDefinition {

    static FunctionOperationAtom atom = new FunctionOperationAtom("SYD");
    static String description = """
        Returns a Double specifying the sum-of-years' digits depreciation of
        an asset for a specified period.""";
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, description,
            DataType.NUMERIC, new FunctionParameterR[] {
                    new FunctionParameterR( DataType.NUMERIC, "Cost" ),
                    new FunctionParameterR( DataType.NUMERIC, "Salvage" ),
                    new FunctionParameterR( DataType.NUMERIC, "Life" ),
                    new FunctionParameterR( DataType.NUMERIC, "Period" )});

    public SYDFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final DoubleCalc costCalc = compiler.compileDouble(call.getArg(0));
        final DoubleCalc salvageCalc = compiler.compileDouble(call.getArg(1));
        final DoubleCalc lifeCalc = compiler.compileDouble(call.getArg(2));
        final DoubleCalc periodCalc = compiler.compileDouble(call.getArg(3));
        return new SYDCalc(call.getType(), costCalc, salvageCalc, lifeCalc, periodCalc);
    }

}
