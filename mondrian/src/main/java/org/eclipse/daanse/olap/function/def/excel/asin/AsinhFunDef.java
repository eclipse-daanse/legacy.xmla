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
package org.eclipse.daanse.olap.function.def.excel.asin;

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

public class AsinhFunDef  extends AbstractFunctionDefinition {

    static FunctionOperationAtom atom = new FunctionOperationAtom("Asinh");
    static String description = """
        Returns the inverse hyperbolic sine of a number. The inverse
        hyperbolic sine is the value whose hyperbolic sine is Arg1,
        so Asinh(Sinh(number)) equals Arg1.""";
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, description,
            DataType.NUMERIC, new FunctionParameterR[] { new FunctionParameterR( DataType.NUMERIC, "Number" ) });

    public AsinhFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final DoubleCalc calc0 = compiler.compileDouble(call.getArg(0));
        return new AsinhCalc(call.getType(), calc0);
    }

}
