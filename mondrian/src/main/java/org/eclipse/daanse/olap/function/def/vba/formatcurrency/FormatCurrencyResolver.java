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
package org.eclipse.daanse.olap.function.def.vba.formatcurrency;

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
public class FormatCurrencyResolver extends AbstractFunctionDefinitionMultiResolver {

    private static FunctionOperationAtom atom = new FunctionOperationAtom("FormatCurrency");
    private static String DESCRIPTION = """
        Returns an expression formatted as a currency value using the
        currency symbol defined in the system control panel.""";

    private static FunctionParameterR[] p0 = { new FunctionParameterR(DataType.VALUE, "expression") };
    private static FunctionParameterR[] p1 = { new FunctionParameterR(DataType.VALUE, "expression"),
            new FunctionParameterR(DataType.INTEGER, "numDigitsAfterDecimal")};
    private static FunctionParameterR[] p2 = { new FunctionParameterR(DataType.VALUE, "expression"),
            new FunctionParameterR(DataType.INTEGER, "numDigitsAfterDecimal"), new FunctionParameterR(DataType.INTEGER, "includeLeadingDigit") };
    private static FunctionParameterR[] p3 = { new FunctionParameterR(DataType.VALUE, "expression"),
            new FunctionParameterR(DataType.INTEGER, "numDigitsAfterDecimal"), new FunctionParameterR(DataType.INTEGER, "includeLeadingDigit"),
            new FunctionParameterR(DataType.INTEGER, "useParensForNegativeNumbers") };
    private static FunctionParameterR[] p4 = { new FunctionParameterR(DataType.VALUE, "expression"),
            new FunctionParameterR(DataType.INTEGER, "numDigitsAfterDecimal"), new FunctionParameterR(DataType.INTEGER, "includeLeadingDigit"),
            new FunctionParameterR(DataType.INTEGER, "useParensForNegativeNumbers"), new FunctionParameterR(DataType.INTEGER, "groupDigits") };


    private static FunctionMetaData functionMetaData0 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.STRING, p0);
    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.STRING, p1);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.STRING, p2);
    private static FunctionMetaData functionMetaData3 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.STRING, p3);
    private static FunctionMetaData functionMetaData4 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.STRING, p4);

    public FormatCurrencyResolver() {
        super(List.of(new FormatCurrencyFunDef(functionMetaData0), new FormatCurrencyFunDef(functionMetaData1), new FormatCurrencyFunDef(functionMetaData2),
                new FormatCurrencyFunDef(functionMetaData3), new FormatCurrencyFunDef(functionMetaData4)));
    }
}
