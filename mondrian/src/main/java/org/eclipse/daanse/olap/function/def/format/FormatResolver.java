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
package org.eclipse.daanse.olap.function.def.format;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractFunctionDefinitionMultiResolver;

public class FormatResolver extends AbstractFunctionDefinitionMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("Format");
    private static String SIGNATURE = "Format(<Expression>, <String Expression>)";
    private static String DESCRIPTION = "Formats a number or date to a string.";
    private static FunctionParameterR[] mS = { new FunctionParameterR(DataType.MEMBER, "Value"), new FunctionParameterR(DataType.STRING, "Format") };
    private static FunctionParameterR[] nS = { new FunctionParameterR(DataType.NUMERIC, "Value"), new FunctionParameterR(DataType.STRING, "Format") };
    private static FunctionParameterR[] DS = { new FunctionParameterR(DataType.DATE_TIME, "Value"), new FunctionParameterR(DataType.STRING, "Format") };
    // {"fSmS", "fSnS", "fSDS"}


    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, mS);
    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, nS);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, DS);

    public FormatResolver() {
        super(List.of(new FormatFunDef(functionMetaData), new FormatFunDef(functionMetaData1), new FormatFunDef(functionMetaData2)));
    }
}