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
package org.eclipse.daanse.olap.function.def.nonstandard;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.TupleCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class CachedExistsFunDef extends AbstractFunctionDefinition {

    static OperationAtom functionAtom = new FunctionOperationAtom("CachedExists");

    static FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtom,
            "Returns tuples from a non-dynamic <Set> that exists in the specified <Tuple>.  This function will build a query level cache named <String> based on the <Tuple> type.",
            DataType.SET,
            new FunctionParameterR[] { new FunctionParameterR(  DataType.SET ), new FunctionParameterR( DataType.TUPLE ), new FunctionParameterR( DataType.STRING, "String") });

    CachedExistsFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc listCalc1 = compiler.compileList(call.getArg(0));
        final TupleCalc tupleCalc1 = compiler.compileTuple(call.getArg(1));
        final StringCalc stringCalc = compiler.compileString(call.getArg(2));

        return new CachedExistsCalc(call.getType(), listCalc1, tupleCalc1, stringCalc);
    }

}
