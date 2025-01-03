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
package org.eclipse.daanse.olap.function.def.udf.currentdatemember;

import org.eclipse.daanse.olap.api.MatchType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Literal;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.HierarchyCalc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class CurrentDateMemberFunDef  extends AbstractFunctionDefinition {

    public CurrentDateMemberFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final HierarchyCalc hierarchyCalc = compiler.compileHierarchy(call.getArg(0));
        final StringCalc stringCalc = compiler.compileString(call.getArg(1));
        MatchType matchType = MatchType.EXACT; 
        if (call.getArgCount() == 3) {
            String matchStr = (String) ( (Literal<?>) call.getArg( 2 ) ).getValue();
            matchType = Enum.valueOf(MatchType.class, matchStr);

        }
        return new CurrentDateMemberCalc(call.getType(), hierarchyCalc, stringCalc, matchType);

    }

}
