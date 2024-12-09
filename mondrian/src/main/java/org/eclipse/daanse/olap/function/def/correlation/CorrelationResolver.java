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
package org.eclipse.daanse.olap.function.def.correlation;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractFunctionDefinitionMultiResolver;

public class CorrelationResolver extends AbstractFunctionDefinitionMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("Correlation");
    private static String SIGNATURE = "Correlation(<Set>, <Numeric Expression>[, <Numeric Expression>])";
    private static String DESCRIPTION = "Returns the correlation of two series evaluated over a set.";
    // {"fnxn", "fnxnn"}

    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, new FunctionParameterR[] { new FunctionParameterR(DataType.SET),
                    new FunctionParameterR(DataType.NUMERIC) });
    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, new FunctionParameterR[] { new FunctionParameterR(DataType.SET),
                    new FunctionParameterR(DataType.NUMERIC), new FunctionParameterR(DataType.NUMERIC) });

    public CorrelationResolver() {
        super(List.of(new CorrelationFunDef(functionMetaData), new CorrelationFunDef(functionMetaData1)));
    }
}