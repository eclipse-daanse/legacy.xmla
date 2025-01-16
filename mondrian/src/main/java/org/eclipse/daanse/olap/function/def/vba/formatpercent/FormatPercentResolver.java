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
package org.eclipse.daanse.olap.function.def.vba.formatpercent;

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
public class FormatPercentResolver extends AbstractFunctionDefinitionMultiResolver {

    private static FunctionOperationAtom atom = new FunctionOperationAtom("FormatPercent");
    private static String DESCRIPTION = """
        Returns an expression formatted as a percentage (multipled by 100)
        with a trailing % character.""";

    private static FunctionParameterR[] p1 = { new FunctionParameterR(DataType.VALUE, "Expression") };
    private static FunctionParameterR[] p2 = { new FunctionParameterR(DataType.VALUE, "Expression"), new FunctionParameterR(DataType.INTEGER, "Digits After Decimal") };
    private static FunctionParameterR[] p3 = { new FunctionParameterR(DataType.VALUE, "Expression"), new FunctionParameterR(DataType.INTEGER, "Digits After Decimal"),
            new FunctionParameterR(DataType.INTEGER, "Include Leading Digit")};
    private static FunctionParameterR[] p4 = { new FunctionParameterR(DataType.VALUE, "Expression"), new FunctionParameterR(DataType.INTEGER, "Digits After Decimal"),
            new FunctionParameterR(DataType.INTEGER, "Include Leading Digit"), new FunctionParameterR(DataType.INTEGER, "Use Parens For Negative Numbers")};
    private static FunctionParameterR[] p5 = { new FunctionParameterR(DataType.VALUE, "Expression"), new FunctionParameterR(DataType.INTEGER, "Digits After Decimal"),
            new FunctionParameterR(DataType.INTEGER, "Include Leading Digit"), new FunctionParameterR(DataType.INTEGER, "Use Parens For Negative Numbers"),
            new FunctionParameterR(DataType.INTEGER, "Group Digits")};

    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.STRING, p1);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.STRING, p2);
    private static FunctionMetaData functionMetaData3 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.STRING, p3);
    private static FunctionMetaData functionMetaData4 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.STRING, p4);
    private static FunctionMetaData functionMetaData5 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.STRING, p5);

    public FormatPercentResolver() {
        super(List.of(new FormatPercentFunDef(functionMetaData1), new FormatPercentFunDef(functionMetaData2),
                new FormatPercentFunDef(functionMetaData3), new FormatPercentFunDef(functionMetaData4),
                new FormatPercentFunDef(functionMetaData5)));
    }
}
