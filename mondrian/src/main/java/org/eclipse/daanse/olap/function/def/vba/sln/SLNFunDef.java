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
package org.eclipse.daanse.olap.function.def.vba.sln;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class SLNFunDef  extends AbstractFunctionDefinition {

    static FunctionOperationAtom atom = new FunctionOperationAtom("SLN");
    static String description = """
        Returns a Double specifying the straight-line depreciation of an
        asset for a single period.""";
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, description,
            DataType.NUMERIC, new FunctionParameterR[] {
                    new FunctionParameterR( DataType.NUMERIC, "Cost" ),
                    new FunctionParameterR( DataType.NUMERIC, "Salvage" ),
                    new FunctionParameterR( DataType.NUMERIC, "Life" )});

    public SLNFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final DoubleCalc costCalc = compiler.compileDouble(call.getArg(0));
        final DoubleCalc salvageCalc = compiler.compileDouble(call.getArg(1));
        final DoubleCalc lifeCalc = compiler.compileDouble(call.getArg(2));
        return new SLNCalc(call.getType(), costCalc, salvageCalc, lifeCalc);
    }

}
