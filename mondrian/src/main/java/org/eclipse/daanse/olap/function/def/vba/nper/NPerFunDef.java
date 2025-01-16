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
package org.eclipse.daanse.olap.function.def.vba.nper;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.BooleanCalc;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.DateTimeCalc;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.function.def.vba.pmt.PmtCalc;

public class NPerFunDef  extends AbstractFunctionDefinition {

    static FunctionOperationAtom atom = new FunctionOperationAtom("NPer");
    static String description = """
        Returns a Double specifying the number of periods for an annuity
        based on periodic, fixed payments and a fixed interest rate.""";
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, description,
            DataType.NUMERIC, new FunctionParameterR[] { 
                    new FunctionParameterR( DataType.NUMERIC, "Rate" ),
                    new FunctionParameterR( DataType.NUMERIC, "Pmt" ),
                    new FunctionParameterR( DataType.NUMERIC, "Pv" ),
                    new FunctionParameterR( DataType.NUMERIC, "Fv" ),
                    new FunctionParameterR( DataType.LOGICAL, "Due" ) });

    public NPerFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final DoubleCalc rateCalc = compiler.compileDouble(call.getArg(0));
        final DoubleCalc pmtCalc = compiler.compileDouble(call.getArg(1));
        final DoubleCalc pvCalc = compiler.compileDouble(call.getArg(2));
        final DoubleCalc fvCalc = compiler.compileDouble(call.getArg(3));
        final BooleanCalc dueCalc = compiler.compileBoolean(call.getArg(3));
        return new NPerCalc(call.getType(), rateCalc, pmtCalc, pvCalc, fvCalc, dueCalc);
    }

}
