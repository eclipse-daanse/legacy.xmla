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
package org.eclipse.daanse.olap.function.def.set;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.NoExpressionRequiredFunctionResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class SetResolver  extends NoExpressionRequiredFunctionResolver {

    @Override
    public FunctionDefinition resolve(
        Expression[] args,
        Validator validator,
        List<Conversion> conversions)
    {
        FunctionParameterR[] parameterTypes = new FunctionParameterR[args.length];
        for (int i = 0; i < args.length; i++) {
            if (validator.canConvert(
                    i, args[i], DataType.MEMBER, conversions))
            {
                parameterTypes[i] = new FunctionParameterR(DataType.MEMBER);
                continue;
            }
            if (validator.canConvert(
                    i, args[i], DataType.TUPLE, conversions))
            {
                parameterTypes[i] = new FunctionParameterR(DataType.TUPLE);
                continue;
            }
            if (validator.canConvert(
                    i, args[i], DataType.SET, conversions))
            {
                parameterTypes[i] = new FunctionParameterR(DataType.SET);
                continue;
            }
            return null;
        }

        return new SetFunDef(parameterTypes);
    }

    @Override
    public OperationAtom getFunctionAtom() {
        return SetFunDef.functionAtom;
    }
}
