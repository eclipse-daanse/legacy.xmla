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
package org.eclipse.daanse.olap.function.def.vba.instrrev;

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
public class InStrRevResolver extends AbstractFunctionDefinitionMultiResolver {

    private static FunctionOperationAtom atom = new FunctionOperationAtom("InStrRev");
    private static String SIGNATURE = "InStrRev(stringcheck, stringmatch[, start[, compare]])";
    private static String DESCRIPTION = """
        Returns the position of an occurrence of one string within another,
        from the end of string.""";

    private static FunctionParameterR[] p1 = { new FunctionParameterR(DataType.STRING, "String Check"), new FunctionParameterR(DataType.STRING, "String Match") };
    private static FunctionParameterR[] p2 = { new FunctionParameterR(DataType.STRING, "String Check"),
            new FunctionParameterR(DataType.STRING, "String Match"), new FunctionParameterR(DataType.INTEGER, "Start") };
    private static FunctionParameterR[] p3 = { new FunctionParameterR(DataType.STRING, "String Check"),
            new FunctionParameterR(DataType.STRING, "String Match"), new FunctionParameterR(DataType.INTEGER, "Start"),
            new FunctionParameterR(DataType.INTEGER, "Compare") };

    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.INTEGER, p1);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.INTEGER, p2);
    private static FunctionMetaData functionMetaData3 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.INTEGER, p3);

    public InStrRevResolver() {
        super(List.of(new InStrRevFunDef(functionMetaData1), new InStrRevFunDef(functionMetaData2), new InStrRevFunDef(functionMetaData3)));
    }
}
