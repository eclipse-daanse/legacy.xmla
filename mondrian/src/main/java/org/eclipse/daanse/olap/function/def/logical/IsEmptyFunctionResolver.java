/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.function.def.logical;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractFunctionDefinitionMultiResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class IsEmptyFunctionResolver extends AbstractFunctionDefinitionMultiResolver {

    private static OperationAtom fAtom = new FunctionOperationAtom("IsEmpty");

    private static FunctionMetaData functionMetaDataString = new FunctionMetaDataR(fAtom,
        "Determines if an expression evaluates to the empty cell value.",
        DataType.LOGICAL, new FunctionParameterR[]{ new FunctionParameterR(DataType.STRING, "String")});

    private static FunctionMetaData functionMetaDataNumeric = new FunctionMetaDataR(fAtom,
        "Determines if an expression evaluates to the empty cell value.",
        DataType.LOGICAL, new FunctionParameterR[]{ new FunctionParameterR(DataType.NUMERIC, "Numeric")});

    public IsEmptyFunctionResolver() {
        super(List.of(new IsEmptyFunDef(functionMetaDataString), new IsEmptyFunDef(functionMetaDataNumeric)));
    }

}
