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
package org.eclipse.daanse.olap.function.def.set.extract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.NoExpressionRequiredFunctionResolver;

public class ExtractResolver extends NoExpressionRequiredFunctionResolver {
    @Override
    public FunctionDefinition resolve(Expression[] args, Validator validator, List<Conversion> conversions) {
        if (args.length < 2) {
            return null;
        }
        if (!validator.canConvert(0, args[0], DataType.SET, conversions)) {
            return null;
        }
        for (int i = 1; i < args.length; ++i) {
            if (!validator.canConvert(0, args[i], DataType.HIERARCHY, conversions)) {
                return null;
            }
        }

        // Find the dimensionality of the set expression.

        // Form a list of ordinals of the hierarchies being extracted.
        // For example, in
        // Extract(X.Members * Y.Members * Z.Members, Z, X)
        // the hierarchy ordinals are X=0, Y=1, Z=2, and the extracted
        // ordinals are {2, 0}.
        //
        // Each hierarchy extracted must exist in the LHS,
        // and no hierarchy may be extracted more than once.
        List<Integer> extractedOrdinals = new ArrayList<>();
        final List<Hierarchy> extractedHierarchies = new ArrayList<>();
        ExtractFunDef.findExtractedHierarchies(args, extractedHierarchies, extractedOrdinals);
        FunctionParameterR[] parameterTypes = new FunctionParameterR[args.length];
        parameterTypes[0] = new FunctionParameterR(DataType.SET);
        Arrays.fill(parameterTypes, 1, parameterTypes.length, new FunctionParameterR(DataType.HIERARCHY));

        return new ExtractFunDef(parameterTypes);
    }

    @Override
    public OperationAtom getFunctionAtom() {
        return ExtractFunDef.functionAtom;
    }

}
