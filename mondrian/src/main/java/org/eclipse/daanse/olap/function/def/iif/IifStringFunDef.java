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
package org.eclipse.daanse.olap.function.def.iif;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.BooleanCalc;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class IifStringFunDef extends AbstractFunctionDefinition {

    static OperationAtom STRING_INSTANCE_FUNCTION_ATOM = new FunctionOperationAtom("IIf");
    static FunctionParameterR[] params = { new FunctionParameterR(DataType.LOGICAL, "Condition"),
            new FunctionParameterR(DataType.STRING, "String1"), new FunctionParameterR(DataType.STRING, "String2") };
    static FunctionMetaData STRING_INSTANCE_FUNCTION_META_DATA = new FunctionMetaDataR(STRING_INSTANCE_FUNCTION_ATOM,
            "Returns one of two string values determined by a logical test.",
            DataType.STRING, params);
    // IIf(<Logical Expression>, <String Expression>, <String Expression>)

    public IifStringFunDef() {
        super(STRING_INSTANCE_FUNCTION_META_DATA);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final BooleanCalc booleanCalc = compiler.compileBoolean(call.getArg(0));
        final StringCalc calc1 = compiler.compileString(call.getArg(1));
        final StringCalc calc2 = compiler.compileString(call.getArg(2));
        return new IifStringCalc(call.getType(), booleanCalc, calc1, calc2);
    }

}
