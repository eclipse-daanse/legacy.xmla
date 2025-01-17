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
package org.eclipse.daanse.olap.function.def.logical;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PostfixOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractFunctionDefinitionMultiResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class IsNullResolver extends AbstractFunctionDefinitionMultiResolver {
    private static String DESCRIPTION = "Returns whether an object is null";

    private static OperationAtom atom = new PostfixOperationAtom("IS NULL");

    private static FunctionMetaData functionMetaDataWithMember = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.LOGICAL, new FunctionParameterR[] { new FunctionParameterR(  DataType.MEMBER ) });

    private static FunctionMetaData functionMetaDataWithLevel = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.LOGICAL, new FunctionParameterR[] { new FunctionParameterR(  DataType.LEVEL ) });

    private static FunctionMetaData functionMetaDataWithHierrchy = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.LOGICAL, new FunctionParameterR[] { new FunctionParameterR(  DataType.HIERARCHY ) });

    private static FunctionMetaData functionMetaDataWithDimension = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.LOGICAL, new FunctionParameterR[] { new FunctionParameterR(  DataType.DIMENSION ) });

    public IsNullResolver() {
        super(List.of(new IsNullFunDef(functionMetaDataWithMember), new IsNullFunDef(functionMetaDataWithLevel),
                new IsNullFunDef(functionMetaDataWithHierrchy), new IsNullFunDef(functionMetaDataWithDimension)));
    }

}
