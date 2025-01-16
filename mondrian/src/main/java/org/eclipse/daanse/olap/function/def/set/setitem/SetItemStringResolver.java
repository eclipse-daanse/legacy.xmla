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
package org.eclipse.daanse.olap.function.def.set.setitem;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.resolver.NoExpressionRequiredFunctionResolver;
import org.eclipse.daanse.olap.query.base.Expressions;
import org.osgi.service.component.annotations.Component;

import mondrian.olap.Util;

@Component(service = FunctionResolver.class)
public class SetItemStringResolver extends NoExpressionRequiredFunctionResolver {
    @Override
    public FunctionDefinition resolve(Expression[] args, Validator validator, List<Conversion> conversions) {
        if (args.length < 1) {
            return null;
        }
        final Expression setExp = args[0];
        if (!(setExp.getType() instanceof SetType)) {
            return null;
        }
        final SetType setType = (SetType) setExp.getType();
        final int arity = setType.getArity();
        // All args must be strings.
        for (int i = 1; i < args.length; i++) {
            if (!validator.canConvert(i, args[i], DataType.STRING, conversions)) {
                return null;
            }
        }
        if (args.length - 1 != arity) {
            throw Util.newError("Argument count does not match set's cardinality " + arity);
        }
        final DataType category = arity == 1 ? DataType.MEMBER : DataType.TUPLE;

        FunctionMetaData functionMetaData = new FunctionMetaDataR(SetItemFunDef.functionAtom,
                "Returns a tuple from the set specified in <Set>. The tuple to be returned is specified by the member name (or names) in <String>.",
                category, Expressions.functionParameterOf(args));

        return new SetItemFunDef(functionMetaData);
    }

    @Override
    public OperationAtom getFunctionAtom() {
        return SetItemFunDef.functionAtom;
    }

}
