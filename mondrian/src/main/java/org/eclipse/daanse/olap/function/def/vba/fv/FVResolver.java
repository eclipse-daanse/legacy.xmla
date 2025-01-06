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
package org.eclipse.daanse.olap.function.def.vba.fv;

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
public class FVResolver extends AbstractFunctionDefinitionMultiResolver {

    private static FunctionOperationAtom atom = new FunctionOperationAtom("FV");
    private static String SIGNATURE = "FV(rate, nper, pmt[, pv[, type]])";
    private static String DESCRIPTION = """
            Returns a Double specifying the future value of an annuity based on
            periodic, fixed payments and a fixed interest rate.""";

    private static FunctionParameterR[] p1 = { new FunctionParameterR(DataType.NUMERIC, "Rate"),
            new FunctionParameterR(DataType.NUMERIC, "NPer"), new FunctionParameterR(DataType.NUMERIC, "Pmt") };
    private static FunctionParameterR[] p2 = { new FunctionParameterR(DataType.NUMERIC, "Rate"),
            new FunctionParameterR(DataType.NUMERIC, "NPer"), new FunctionParameterR(DataType.NUMERIC, "Pmt"),
            new FunctionParameterR(DataType.NUMERIC, "Pv") };
    private static FunctionParameterR[] p3 = { new FunctionParameterR(DataType.NUMERIC, "Rate"),
            new FunctionParameterR(DataType.NUMERIC, "NPer"), new FunctionParameterR(DataType.NUMERIC, "Pmt"),
            new FunctionParameterR(DataType.NUMERIC, "Pv"), new FunctionParameterR(DataType.LOGICAL, "Type") };

    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, p1);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, p2);
    private static FunctionMetaData functionMetaData3 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, p3);

    public FVResolver() {
        super(List.of(new FVFunDef(functionMetaData1), new FVFunDef(functionMetaData2),
                new FVFunDef(functionMetaData3)));
    }
}