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
package org.eclipse.daanse.olap.function.def.set.members;

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.HierarchyCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class MembersFunDef extends AbstractFunctionDefinition {

    // <Hierarchy>.Members
    static PlainPropertyOperationAtom plainPropertyOperationAtom = new PlainPropertyOperationAtom("Members");
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom,
            "Returns the set of members in a hierarchy.", "<HIERARCHY>.Members", DataType.SET,
            new DataType[] { DataType.HIERARCHY });

    public MembersFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final HierarchyCalc hierarchyCalc = compiler.compileHierarchy(call.getArg(0));
        return new MembersCalc(call.getType(), hierarchyCalc);
    }

}
