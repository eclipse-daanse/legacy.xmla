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
package org.eclipse.daanse.olap.function.def.set.addcalculatedmembers;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class AddCalculatedMembersFunDef extends AbstractFunctionDefinition {

    static OperationAtom functionAtom = new FunctionOperationAtom("AddCalculatedMembers");

    static FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtom, "Adds calculated members to a set.",
            DataType.SET, new FunctionParameterR[] { new FunctionParameterR(  DataType.SET ) });

    public AddCalculatedMembersFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc = compiler.compileList(call.getArg(0));
        return new AddCalculatedMembersCalc(call.getType(), tupleListCalc) {
        };
    }

}
