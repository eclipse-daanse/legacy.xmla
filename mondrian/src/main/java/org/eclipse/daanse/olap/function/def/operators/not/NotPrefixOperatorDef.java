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
package org.eclipse.daanse.olap.function.def.operators.not;

import org.eclipse.daanse.mdx.model.api.expression.operation.PrefixOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.BooleanCalc;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class NotPrefixOperatorDef extends AbstractFunctionDefinition {

    // NOT <Logical Expression>
    static PrefixOperationAtom prefixOperationAtom = new PrefixOperationAtom("NOT");
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(prefixOperationAtom,
            "Returns the negation of a condition.", "NOT <LOGICAL>", DataType.LOGICAL,
            new FunctionParameterR[] { new FunctionParameterR( DataType.LOGICAL ) });

    public NotPrefixOperatorDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final BooleanCalc calc = compiler.compileBoolean(call.getArg(0));
        return new NotPrefixCalc(call.getType(), calc);
    }

}
