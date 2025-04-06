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
package org.eclipse.daanse.olap.function.def.vba.minute;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class MinuteFunDef  extends AbstractFunctionDefinition {

    static FunctionOperationAtom atom = new FunctionOperationAtom("Minute");
    static String description = """
        Returns a Variant (Integer) specifying a whole number between 0 and
        59, inclusive, representing the minute of the hour.""";
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, description,
            DataType.INTEGER, new FunctionParameterR[] { new FunctionParameterR( DataType.DATE_TIME, "Date" ) });

    public MinuteFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final DateTimeCalc dateCalc = compiler.compileDateTime(call.getArg(0));
        return new MinuteCalc(call.getType(), dateCalc);
    }

}
