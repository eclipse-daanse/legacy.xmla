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
package org.eclipse.daanse.olap.function.def.strtoset;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver.Conversion;
import org.eclipse.daanse.olap.api.query.component.DimensionExpression;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.NullType;
import org.eclipse.daanse.olap.api.type.StringType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.NoExpressionRequiredFunctionResolver;

import mondrian.mdx.HierarchyExpressionImpl;

public class StrToSetResolver extends NoExpressionRequiredFunctionResolver {

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
        argTypes[0] = new FunctionParameterR( DataType.STRING );
        for (int i = 1; i < argTypes.length; i++) {
            argTypes[i] = new FunctionParameterR( DataType.HIERARCHY );
        }

        FunctionMetaData functionMetaData = functionMetaDataFor(argTypes);
        return new StrToSetFunDef(functionMetaData);
    }


    private FunctionMetaData functionMetaDataFor(FunctionParameterR[] argTypes) {
        FunctionMetaData functionMetaData = new FunctionMetaDataR(StrToSetFunDef.functionAtom,
                "Constructs a set from a string expression.", "<Set> StrToSet(<String>[, <Hierarchy>...])",
                 DataType.SET, argTypes);
        return functionMetaData;
    }


    @Override
    public List<FunctionMetaData> getRepresentativeFunctionMetaDatas() {
        return List.of(functionMetaDataFor(new FunctionParameterR[] { new FunctionParameterR( DataType.STRING ) }));
    }


    @Override
    public OperationAtom getFunctionAtom() {
        return StrToSetFunDef.functionAtom;
    }

}
