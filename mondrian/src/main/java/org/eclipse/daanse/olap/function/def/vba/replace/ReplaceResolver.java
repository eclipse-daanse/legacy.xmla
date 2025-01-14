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
package org.eclipse.daanse.olap.function.def.vba.replace;

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
public class ReplaceResolver extends AbstractFunctionDefinitionMultiResolver {

    private static FunctionOperationAtom atom = new FunctionOperationAtom("ReplaceResolver.java");
    private static String SIGNATURE = "Replace(expression, find, replace[, start[, count[, compare]]])";
    private static String DESCRIPTION = """
        Returns a string in which a specified substring has been replaced
        with another substring a specified number of times.""";

    private static FunctionParameterR[] p1 = { new FunctionParameterR(DataType.STRING, "expression"), new FunctionParameterR(DataType.STRING, "find"), new FunctionParameterR(DataType.STRING, "replace") };
    private static FunctionParameterR[] p2 = { new FunctionParameterR(DataType.STRING, "expression"), new FunctionParameterR(DataType.STRING, "find"), new FunctionParameterR(DataType.STRING, "replace"),
            new FunctionParameterR(DataType.INTEGER, "start") };
    private static FunctionParameterR[] p3 = { new FunctionParameterR(DataType.STRING, "expression"), new FunctionParameterR(DataType.STRING, "find"), new FunctionParameterR(DataType.STRING, "replace"),
            new FunctionParameterR(DataType.INTEGER, "start"), new FunctionParameterR(DataType.INTEGER, "coun")};
    private static FunctionParameterR[] p4 = { new FunctionParameterR(DataType.STRING, "expression"), new FunctionParameterR(DataType.STRING, "find"), new FunctionParameterR(DataType.STRING, "replace"),
            new FunctionParameterR(DataType.INTEGER, "start"), new FunctionParameterR(DataType.INTEGER, "coun"), new FunctionParameterR(DataType.INTEGER, "compare") }; // compare is currently ignored

    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, p1);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, p2);
    private static FunctionMetaData functionMetaData3 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, p3);
    private static FunctionMetaData functionMetaData4 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, p4);

    public ReplaceResolver() {
        super(List.of(new ReplaceFunDef(functionMetaData1), new ReplaceFunDef(functionMetaData2),
                new ReplaceFunDef(functionMetaData3), new ReplaceFunDef(functionMetaData4)));
    }
}
