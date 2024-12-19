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
package org.eclipse.daanse.olap.function.def.strtotuple;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.DimensionExpression;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.NoExpressionRequiredFunctionResolver;
import org.osgi.service.component.annotations.Component;

import mondrian.mdx.HierarchyExpressionImpl;
import mondrian.olap.type.NullType;
import mondrian.olap.type.StringType;

@Component(service = FunctionResolver.class)
public class StrToTupleResolver extends NoExpressionRequiredFunctionResolver {


    @Override
    public FunctionDefinition resolve(
        Expression[] args,
        Validator validator,
        List<Conversion> conversions)
    {
        if (args.length < 1) {
            return null;
        }
        Type type = args[0].getType();
        if (!(type instanceof StringType)
            && !(type instanceof NullType))
        {
            return null;
        }
        for (int i = 1; i < args.length; i++) {
            Expression exp = args[i];
            if (!(exp instanceof DimensionExpression
                || exp instanceof HierarchyExpressionImpl))
            {
                return null;
            }
        }
        FunctionParameterR[] argTypes = new FunctionParameterR[args.length];
        argTypes[0] = new FunctionParameterR(DataType.STRING);
        for (int i = 1; i < argTypes.length; i++) {
            argTypes[i] = new FunctionParameterR(DataType.HIERARCHY);
        }
        return new StrToTupleFunDef(functionMetaDataFor(argTypes));
    }


    @Override
    public List<FunctionMetaData> getRepresentativeFunctionMetaDatas() {
        return List.of( functionMetaDataFor(new FunctionParameterR[] {new FunctionParameterR(DataType.STRING)}));
    }

    private FunctionMetaData functionMetaDataFor(FunctionParameterR[] argTypes) {
        FunctionMetaData functionMetaData = new FunctionMetaDataR(StrToTupleFunDef.functionAtom,
                "Constructs a tuple from a string.", "StrToTuple(<String Expression>)",
                 DataType.TUPLE, argTypes);
        return functionMetaData;
    }


    @Override
    public OperationAtom getFunctionAtom() {
        return StrToTupleFunDef.functionAtom;
    }

}
