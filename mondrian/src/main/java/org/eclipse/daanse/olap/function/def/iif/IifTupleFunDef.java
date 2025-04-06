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
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;

public class IifTupleFunDef extends IifFunDef {

    static OperationAtom TUPLE_INSTANCE_FUNCTION_ATOM = new FunctionOperationAtom("IIf");
    static FunctionParameterR[] params = { new FunctionParameterR(DataType.LOGICAL, "Condition"),
            new FunctionParameterR(DataType.TUPLE, "Tuple1"), new FunctionParameterR(DataType.TUPLE, "Tuple2") };
    static FunctionMetaData TUPLE_INSTANCE_FUNCTION_META_DATA = new FunctionMetaDataR(TUPLE_INSTANCE_FUNCTION_ATOM,
            "Returns one of two tuples determined by a logical test.",
            DataType.TUPLE, params);
    // IIf(<Logical Expression>, <Tuple Expression>, <Tuple Expression>)

    public IifTupleFunDef() {
        super(TUPLE_INSTANCE_FUNCTION_META_DATA);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final BooleanCalc booleanCalc = compiler.compileBoolean(call.getArg(0));
        final Calc<?> calc1 = compiler.compileTuple(call.getArg(1));
        final Calc<?> calc2 = compiler.compileTuple(call.getArg(2));
        return new IifTupleCalc(call.getType(), booleanCalc, calc1, calc2) {
        };
    }

}
