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
package org.eclipse.daanse.olap.function.def.vba.ddb;

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
public class DDBResolver extends AbstractFunctionDefinitionMultiResolver {
    
    private static FunctionOperationAtom atom = new FunctionOperationAtom("DDB");
    private static String DESCRIPTION = """
            Returns a Double specifying the depreciation of an asset for a
            specific time period using the double-declining balance method or
            some other method you specify.""";
    
    private static FunctionParameterR[] p1 = { new FunctionParameterR( DataType.NUMERIC, "Cost" ), new FunctionParameterR( DataType.NUMERIC, "Salvage" ), new FunctionParameterR( DataType.NUMERIC, "Life" ) , new FunctionParameterR( DataType.NUMERIC, "Period" )};
    private static FunctionParameterR[] p2 = { new FunctionParameterR( DataType.NUMERIC, "Cost" ), new FunctionParameterR( DataType.NUMERIC, "Salvage" ), new FunctionParameterR( DataType.NUMERIC, "Life" ), new FunctionParameterR( DataType.NUMERIC, "Period" ), new FunctionParameterR( DataType.NUMERIC, "Factor" ) };

    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.NUMERIC, p1);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.NUMERIC, p2);

    public DDBResolver() {
        super(List.of(new DDBFunDef(functionMetaData1), new DDBFunDef(functionMetaData2)));
    }
}    