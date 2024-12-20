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
package org.eclipse.daanse.olap.function.def.coalesceempty;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;

public class CoalesceEmptyResolver implements FunctionResolver {

    @Override
    public FunctionDefinition resolve(
        Expression[] args,
        Validator validator,
        List<Conversion> conversions)
    {
        if (args.length < 1) {
            return null;
        }
        final DataType[] types = {DataType.NUMERIC, DataType.STRING};
        final FunctionParameterR[] argTypes = new FunctionParameterR[args.length];
        for (DataType type : types) {
            int matchingArgs = 0;
            conversions.clear();
            for (int i = 0; i < args.length; i++) {
                if (validator.canConvert(i, args[i], type, conversions)) {
                    matchingArgs++;
                }
                argTypes[i] = new FunctionParameterR(type);
            }
            if (matchingArgs == args.length) {

                FunctionMetaData functionMetaData=new FunctionMetaDataR( CoalesceEmptyFunDef.functionAtom, "Coalesces an empty cell value to a different value. All of the expressions must be of the same type (number or string).", "CoalesceEmpty(<Value Expression>[, <Value Expression>...])",  type, argTypes);
                return new CoalesceEmptyFunDef(functionMetaData);
            }
        }
        return null;
    }

    @Override
    public boolean requiresScalarExpressionOnArgument(int k) {
        return true;
    }

    @Override
    public OperationAtom getFunctionAtom() {
        return CoalesceEmptyFunDef.functionAtom;
    }
}
