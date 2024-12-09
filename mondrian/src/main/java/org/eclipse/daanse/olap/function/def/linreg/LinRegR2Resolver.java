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

public class LinRegR2Resolver extends AbstractFunctionDefinitionMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("LinRegR2");
    private static String SIGNATURE = "LinRegR2(<Set>, <Numeric Expression>[, <Numeric Expression>])";
    private static String DESCRIPTION = "Calculates the linear regression of a set and returns R2 (the coefficient of determination).";
    private static FunctionParameterR[] xn = { new FunctionParameterR(DataType.SET),
            new FunctionParameterR(DataType.NUMERIC) };
    private static FunctionParameterR[] xnn = { new FunctionParameterR(DataType.SET),
            new FunctionParameterR(DataType.NUMERIC), new FunctionParameterR(DataType.NUMERIC) };
    // {"fnxn", "fnxnn"}

    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, xn);
    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, xnn);

    public LinRegR2Resolver() {
        super(List.of(new LinRegFunDef(functionMetaData, LinRegFunDef.R2),
                new LinRegFunDef(functionMetaData1, LinRegFunDef.R2)));
    }
}