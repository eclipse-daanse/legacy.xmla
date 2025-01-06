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
package org.eclipse.daanse.olap.function.def.vba.irr;

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
public class IRRResolver extends AbstractFunctionDefinitionMultiResolver {

    private static FunctionOperationAtom atom = new FunctionOperationAtom("IRR");
    private static String SIGNATURE = "IRR(values()[, guess])";
    private static String DESCRIPTION = """
            Returns a Double specifying the internal rate of return for a series
            of periodic cash flows (payments and receipts).""";

    private static FunctionParameterR[] p1 = { new FunctionParameterR(DataType.ARRAY, "ValueArray") };
    private static FunctionParameterR[] p2 = { new FunctionParameterR(DataType.ARRAY, "ValueArray"),
            new FunctionParameterR(DataType.NUMERIC, "Guess") };

    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, p1);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, p2);

    public IRRResolver() {
        super(List.of(new IRRFunDef(functionMetaData1), new IRRFunDef(functionMetaData2)));
    }
}