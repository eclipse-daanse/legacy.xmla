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
package org.eclipse.daanse.olap.function.def.operators.divide;

import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class DivideOperatorDef extends AbstractFunctionDefinition {

    // <Numeric Expression> / <Numeric Expression>
    static InfixOperationAtom infixOperationAtom = new InfixOperationAtom("/");
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Divides two numbers.",
            "<NUMERIC> / <NUMERIC>", DataType.NUMERIC, new FunctionParameterR[] { new FunctionParameterR( DataType.NUMERIC ), new FunctionParameterR( DataType.NUMERIC ) });

    public DivideOperatorDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final DoubleCalc calc0 = compiler.compileDouble(call.getArg(0));
        final DoubleCalc calc1 = compiler.compileDouble(call.getArg(1));
        final boolean isNullDenominatorProducesNull = compiler.getEvaluator().getQuery().getConnection().getContext()
                .getConfig().nullDenominatorProducesNull();

        // If the mondrian property
        // mondrian.olap.NullOrZeroDenominatorProducesNull
        // is false(default), Null in denominator with numeric numerator
        // returns infinity. This is consistent with MSAS.
        //
        // If this property is true, Null or zero in denominator returns
        // Null. This is only used by certain applications and does not
        // conform to MSAS behavior.
        return new DivideCalc(call.getType(), calc0, calc1, isNullDenominatorProducesNull);
    }

}
