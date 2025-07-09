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
package org.eclipse.daanse.olap.function.def.cast;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.query.base.Expressions;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class CaseTestResolver  implements FunctionResolver {
    @Override
    public FunctionDefinition resolve(Expression[] args, Validator validator, List<Conversion> conversions) {
        if (args.length < 1) {
            return null;
        }
        int j = 0;
        int clauseCount = args.length / 2;
        int mismatchingArgs = 0;
        DataType returnType = args[1].getCategory();
        for (int i = 0; i < clauseCount; i++) {
            if (!validator.canConvert(j, args[j++], DataType.LOGICAL, conversions)) {
                mismatchingArgs++;
            }
            if (!validator.canConvert(j, args[j++], returnType, conversions)) {
                mismatchingArgs++;
            }
        }

        if (j < args.length && !validator.canConvert(j, args[j++], returnType, conversions)) {
            mismatchingArgs++;
        }
        Util.assertTrue(j == args.length);
        if (mismatchingArgs != 0) {
            return null;
        }

        return new CaseTestFunDef(returnType, Expressions.functionParameterOf(args));
    }

    @Override
    public boolean requiresScalarExpressionOnArgument(int k) {
        return true;
    }

    @Override
    public OperationAtom getFunctionAtom() {

        return CaseTestFunDef.functionAtom;
    }

}
