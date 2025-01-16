/*
* Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.function.def.vba.strcomp;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractFunctionDefinitionMultiResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class StrCompResolver extends AbstractFunctionDefinitionMultiResolver {

    private static FunctionOperationAtom atom = new FunctionOperationAtom("StrComp");
    private static String DESCRIPTION = """
        Returns a Variant (Integer) indicating the result of a string
        comparison.""";

    private static FunctionParameterR[] p1 = { new FunctionParameterR(DataType.STRING, "String1"), new FunctionParameterR(DataType.STRING, "String2") };
    private static FunctionParameterR[] p2 = { new FunctionParameterR(DataType.STRING, "String1"), new FunctionParameterR(DataType.STRING, "String2"),
            new FunctionParameterR(DataType.INTEGER, "Compare")  };

    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.INTEGER, p1);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.INTEGER, p2);

    public StrCompResolver() {
        super(List.of(new StrCompFunDef(functionMetaData1), new StrCompFunDef(functionMetaData2)));
    }
}
