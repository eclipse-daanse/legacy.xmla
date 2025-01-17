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
package org.eclipse.daanse.olap.function.def.member;

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
public class AncestorsResolver extends AbstractFunctionDefinitionMultiResolver {

    private static String DESCRIPTION = "Returns the set of all ancestors of a specified member at a specified level or at a specified distance from the member";
    private static OperationAtom atom = new FunctionOperationAtom("Ancestors");
    //{"fxml", "fxmn"}

    private static FunctionMetaData functionMetaDataWithLevel = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.SET, new FunctionParameterR[] { new FunctionParameterR(  DataType.MEMBER ), new FunctionParameterR( DataType.LEVEL ) });

    private static FunctionMetaData functionMetaDataWithNumeric = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.SET, new FunctionParameterR[] { new FunctionParameterR(  DataType.MEMBER ), new FunctionParameterR( DataType.NUMERIC ) });


    public AncestorsResolver() {
        super(List.of(new AncestorsFunDef(functionMetaDataWithLevel),
                new AncestorsFunDef(functionMetaDataWithNumeric)));
    }

}
