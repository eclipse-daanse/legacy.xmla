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
package org.eclipse.daanse.olap.function.def.udf.matches;

import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class MatchesFunDef  extends AbstractFunctionDefinition {

    static InfixOperationAtom atom = new InfixOperationAtom("MATCHES");
    static String description = """
        Returns true if the string matches the regular expression.""";
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, description,
            DataType.LOGICAL , new FunctionParameterR[] { new FunctionParameterR( DataType.STRING, "String1" ), new FunctionParameterR( DataType.STRING, "String2" ) });

    public MatchesFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final StringCalc s1 = compiler.compileString(call.getArg(0));
        final StringCalc s2 = compiler.compileString(call.getArg(1));
        return new MatchesCalc(call.getType(), s1, s2);
    }

}
