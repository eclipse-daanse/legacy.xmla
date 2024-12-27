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
package org.eclipse.daanse.olap.function.core.resolver;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.EmptyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class NullReservedWordsResolver implements FunctionResolver {
    private static OperationAtom atom = new EmptyOperationAtom();
    @Override
    public OperationAtom getFunctionAtom() {
        return atom; //need atom for NPE
    }

    @Override
    public FunctionDefinition resolve(Expression[] args, Validator validator, List<Conversion> conversions) {
        return null;
    }

    @Override
    public boolean requiresScalarExpressionOnArgument(int positionOfArgument) {
        return false;
    }

    @Override
    public List<String> getReservedWords() {
        return List.of("NULL");
    }
}
