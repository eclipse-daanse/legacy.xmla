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
package org.eclipse.daanse.olap.function.def.aggregate;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractFunctionDefinitionMultiResolver;

public class AggregateResolver extends AbstractFunctionDefinitionMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("Aggregate");
    private static String SIGNATURE = "Aggregate(<Set>[, <Numeric Expression>])";
    private static String DESCRIPTION = "Returns a calculated value using the appropriate aggregate function, based on the context of the query.";
    // {"fnx", "fnxn"}

    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, new FunctionParameterR[] { new FunctionParameterR(DataType.SET) });
    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, new FunctionParameterR[] { new FunctionParameterR(DataType.SET),
                    new FunctionParameterR(DataType.NUMERIC) });

    public AggregateResolver() {
        super(List.of(new AggregateFunDef(functionMetaData), new AggregateFunDef(functionMetaData1)));
    }
}