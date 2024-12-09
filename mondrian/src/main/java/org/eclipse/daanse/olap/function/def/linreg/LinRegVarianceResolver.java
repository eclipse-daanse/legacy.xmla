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
package org.eclipse.daanse.olap.function.def.linreg;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractFunctionDefinitionMultiResolver;

public class LinRegVarianceResolver extends AbstractFunctionDefinitionMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("LinRegVariance");
    private static String SIGNATURE = "LinRegVariance(<Set>, <Numeric Expression>[, <Numeric Expression>])";
    private static String DESCRIPTION = "Calculates the linear regression of a set and returns the variance associated with the regression line y = ax + b.";
    private static FunctionParameterR[] nxn = { new FunctionParameterR(DataType.NUMERIC),
            new FunctionParameterR(DataType.SET), new FunctionParameterR(DataType.NUMERIC) };
    private static FunctionParameterR[] nxnn = { new FunctionParameterR(DataType.NUMERIC),
            new FunctionParameterR(DataType.SET), new FunctionParameterR(DataType.NUMERIC),
            new FunctionParameterR(DataType.NUMERIC) };
    // {"fnnxn", "fnnxnn"}

    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, nxn);
    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, nxnn);

    public LinRegVarianceResolver() {
        super(List.of(new LinRegFunDef(functionMetaData, LinRegFunDef.VARIANCE),
                new LinRegFunDef(functionMetaData1, LinRegFunDef.VARIANCE)));
    }
}