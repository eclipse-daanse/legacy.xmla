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
package org.eclipse.daanse.olap.function.def.string;

import java.util.Locale;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.NullType;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.fun.FunUtil;

public class UCaseFunDef extends AbstractFunctionDefinition {

    // UCase(<String Expression>)
    static FunctionOperationAtom functionOperationAtom = new FunctionOperationAtom("UCase");
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(functionOperationAtom,
            "Returns a string that has been converted to uppercase", DataType.STRING,
            new FunctionParameterR[] { new FunctionParameterR(  DataType.STRING, "String" ) });

    public UCaseFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final Locale locale = compiler.getEvaluator().getConnectionLocale();
        final StringCalc stringCalc = compiler.compileString(call.getArg(0));
        if (stringCalc.getType().getClass().equals(NullType.class)) {
            throw FunUtil.newEvalException(this.getFunctionMetaData(),
                    "No method with the signature UCase(NULL) matches known functions.");
        }
        return new UCaseCalc(call.getType(), stringCalc, locale);
    }

}
