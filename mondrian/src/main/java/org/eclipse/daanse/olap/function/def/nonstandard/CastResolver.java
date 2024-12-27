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
package org.eclipse.daanse.olap.function.def.nonstandard;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Literal;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.resolver.NoExpressionRequiredFunctionResolver;
import org.eclipse.daanse.olap.query.base.Expressions;
import org.osgi.service.component.annotations.Component;

import mondrian.olap.exceptions.CastInvalidTypeException;

@Component(service = FunctionResolver.class)
public class CastResolver extends NoExpressionRequiredFunctionResolver {

    @Override
    public FunctionDefinition resolve(
        Expression[] args, Validator validator, List<Conversion> conversions)
    {
        if (args.length != 2) {
            return null;
        }
        if (!(args[1] instanceof Literal literal)) {
            return null;
        }
        String typeName = (String) literal.getValue();
        DataType returnCategory;
        if (typeName.equalsIgnoreCase("String")) {
            returnCategory = DataType.STRING;
        } else if (typeName.equalsIgnoreCase("Numeric")) {
            returnCategory = DataType.NUMERIC;
        } else if (typeName.equalsIgnoreCase("Boolean")) {
            returnCategory = DataType.LOGICAL;
        } else if (typeName.equalsIgnoreCase("Integer")) {
            returnCategory = DataType.INTEGER;
        } else {
            throw new CastInvalidTypeException(typeName);
        }


        FunctionMetaData functionMetaData = new FunctionMetaDataR(CastFunDef.functionAtom, "Converts values to another type.",
                "Cast(<Expression> AS <Type>)", returnCategory, Expressions.functionParameterOf(args));
        return new CastFunDef(functionMetaData);
    }

    @Override
    public OperationAtom getFunctionAtom() {
        return CastFunDef.functionAtom;
    }
}
