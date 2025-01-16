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
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class IifBooleanFunDef extends AbstractFunctionDefinition {

    static final OperationAtom BOOLEAN_INSTANCE_FUNCTION_ATOM = new FunctionOperationAtom("IIf");
    static FunctionParameterR[] params = { new FunctionParameterR(DataType.LOGICAL, "Condition"),
            new FunctionParameterR(DataType.LOGICAL, "Boolean1"), new FunctionParameterR(DataType.LOGICAL, "Boolean2") };
    static final FunctionMetaData BOOLEAN_INSTANCE_FUNCTION_META_DATA = new FunctionMetaDataR(BOOLEAN_INSTANCE_FUNCTION_ATOM, "Returns boolean determined by a logical test.",
            DataType.LOGICAL, params);
    // IIf(<Logical Expression>, <Boolean Expression>, <Boolean Expression>)

    public IifBooleanFunDef() {
        super(BOOLEAN_INSTANCE_FUNCTION_META_DATA);
    }

    @Override
    public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final BooleanCalc booleanCalc =
            compiler.compileBoolean(call.getArg(0));
        final BooleanCalc booleanCalc1 =
            compiler.compileBoolean(call.getArg(1));
        final BooleanCalc booleanCalc2 =
            compiler.compileBoolean(call.getArg(2));
        return new IifBooleanCalc(call.getType(), booleanCalc, booleanCalc1, booleanCalc2);
    }

}
