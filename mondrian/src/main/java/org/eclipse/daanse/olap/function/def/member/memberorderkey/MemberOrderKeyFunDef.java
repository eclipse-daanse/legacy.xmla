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
package org.eclipse.daanse.olap.function.def.member.memberorderkey;

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;

public class MemberOrderKeyFunDef extends AbstractFunctionDefinition {
    static PlainPropertyOperationAtom plainPropertyOperationAtom = new PlainPropertyOperationAtom("OrderKey");

    static FunctionMetaData functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom,
            "Returns the member order key.", "<MEMBER>.OrderKey", DataType.VALUE, new DataType[] { DataType.MEMBER });

    /**
     * Creates the singleton MemberOrderKeyFunDef.
     */
    public MemberOrderKeyFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final MemberCalc memberCalc = compiler.compileMember(call.getArg(0));
        return new MemberOrderKeyCalc(call.getType(), memberCalc);
    }

}
