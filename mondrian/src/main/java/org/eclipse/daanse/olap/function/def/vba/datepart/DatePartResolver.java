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
package org.eclipse.daanse.olap.function.def.vba.datepart;

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
public class DatePartResolver extends AbstractFunctionDefinitionMultiResolver {
    
    private static FunctionOperationAtom atom = new FunctionOperationAtom("DatePart");
    private static String DESCRIPTION = """
            Returns a Variant (Integer) containing the specified part of a given
            date.""";
    private static FunctionParameterR[] p1 = { new FunctionParameterR( DataType.STRING, "IntervalName" ), new FunctionParameterR( DataType.DATE_TIME, "Date1" ),
            new FunctionParameterR( DataType.DATE_TIME, "Date2" ) };
    private static FunctionParameterR[] p2 = { new FunctionParameterR( DataType.STRING, "IntervalName" ), 
            new FunctionParameterR( DataType.DATE_TIME, "Date1" ), new FunctionParameterR( DataType.DATE_TIME, "Date2" ),
            new FunctionParameterR( DataType.INTEGER, "First Day Of Week" ) };
    private static FunctionParameterR[] p3 = { new FunctionParameterR( DataType.STRING, "IntervalName" ),
            new FunctionParameterR( DataType.DATE_TIME, "Date1" ), new FunctionParameterR( DataType.DATE_TIME, "Date2" ),
            new FunctionParameterR( DataType.INTEGER, "First Day Of Week" ), new FunctionParameterR( DataType.INTEGER, "First Week Of Year" ) };


    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.NUMERIC, p1);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.NUMERIC, p2);
    private static FunctionMetaData functionMetaData3 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.NUMERIC, p3);

    public DatePartResolver() {
        super(List.of(new DatePartFunDef(functionMetaData1), new DatePartFunDef(functionMetaData2), new DatePartFunDef(functionMetaData3)));
    }
}    