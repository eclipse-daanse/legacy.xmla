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
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractFunctionDefinitionMultiResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class LinRegPointResolver extends AbstractFunctionDefinitionMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("LinRegPoint");
    private static String DESCRIPTION = "Calculates the linear regression of a set and returns the value of y in the regression line y = ax + b.";
    private static FunctionParameterR[] nxn = { new FunctionParameterR(DataType.NUMERIC, "xPoint"),
            new FunctionParameterR(DataType.SET), new FunctionParameterR(DataType.NUMERIC, "Y") };
    private static FunctionParameterR[] nxnn = { new FunctionParameterR(DataType.NUMERIC, "xPoint"),
            new FunctionParameterR(DataType.SET), new FunctionParameterR(DataType.NUMERIC, "Y"),
            new FunctionParameterR(DataType.NUMERIC, "X") };
    // {"fnnxn", "fnnxnn"}

    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.NUMERIC, nxn);
    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.NUMERIC, nxnn);

    public LinRegPointResolver() {
        super(List.of(new PointFunDef(functionMetaData, LinRegFunDef.POINT),
                new PointFunDef(functionMetaData1, LinRegFunDef.POINT)));
    }
}
