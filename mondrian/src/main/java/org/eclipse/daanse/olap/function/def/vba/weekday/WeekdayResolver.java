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
package org.eclipse.daanse.olap.function.def.vba.weekday;

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
public class WeekdayResolver extends AbstractFunctionDefinitionMultiResolver {
    
    private static FunctionOperationAtom atom = new FunctionOperationAtom("Weekday");
    private static String SIGNATURE = "Weekday(date[, firstDayOfWeek])";
    private static String DESCRIPTION = """
        Returns a Variant (Integer) containing a whole number representing
        the day of the week.""";
    private static FunctionParameterR[] p1 = { new FunctionParameterR( DataType.DATE_TIME, "Date" ) };
    private static FunctionParameterR[] p2 = { new FunctionParameterR( DataType.DATE_TIME, "Date" ),
            new FunctionParameterR( DataType.INTEGER, "First Day Of Week" ) };


    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, p1);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, p2);

    public WeekdayResolver() {
        super(List.of(new WeekdayFunDef(functionMetaData1), new WeekdayFunDef(functionMetaData2)));
    }
}    