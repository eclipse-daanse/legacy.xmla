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
package org.eclipse.daanse.olap.function.def.vba.npv;

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

public class NPVFunDef  extends AbstractFunctionDefinition {

    static FunctionOperationAtom atom = new FunctionOperationAtom("NPV");
    static String description = """
        Returns a Double specifying the net present value of an investment
        based on a series of periodic cash flows (payments and receipts)
        and a discount rate.""";
    static String signature = "NPV(rate, values())";
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, description,
            signature, DataType.NUMERIC, new FunctionParameterR[] {
                    new FunctionParameterR( DataType.NUMERIC, "R" ),
                    new FunctionParameterR( DataType.ARRAY, "CFS" ) });

    public NPVFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final DoubleCalc rCalc = compiler.compileDouble(call.getArg(0));
        final Calc<?> cfsCalc = compiler.compileScalar(call.getArg(0), false);
        return new NPVCalc(call.getType(), rCalc, cfsCalc);
    }

}
