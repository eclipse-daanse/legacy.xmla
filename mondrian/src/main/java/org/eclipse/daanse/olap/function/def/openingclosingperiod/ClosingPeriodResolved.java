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
package org.eclipse.daanse.olap.function.def.openingclosingperiod;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractFunctionDefinitionMultiResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class ClosingPeriodResolved extends AbstractFunctionDefinitionMultiResolver {

    private static OperationAtom atom = new FunctionOperationAtom("ClosingPeriod");
    private static String SIGNATURE = "ClosingPeriod([<Level>[, <Member>]])";
    private static String DESCRIPTION = "Returns the last descendant of a member at a level.";
    // {"fm", "fml", "fmlm", "fmm"}

    private static FunctionMetaData functionMetaDataWithoutParam = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.MEMBER, new FunctionParameterR[] {});
    private static FunctionMetaData functionMetaDataWithLevel = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.MEMBER, new FunctionParameterR[] { new FunctionParameterR(DataType.LEVEL) });
    private static FunctionMetaData functionMetaDataWithLevelMember = new FunctionMetaDataR(atom, DESCRIPTION,
            SIGNATURE, DataType.MEMBER, new FunctionParameterR[] { new FunctionParameterR(DataType.LEVEL),
                    new FunctionParameterR(DataType.MEMBER) });
    private static FunctionMetaData functionMetaDataWithMember = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.MEMBER, new FunctionParameterR[] { new FunctionParameterR(DataType.MEMBER) });

    public ClosingPeriodResolved() {
        super(List.of(new OpeningClosingPeriodFunDef(functionMetaDataWithoutParam, false),
                new OpeningClosingPeriodFunDef(functionMetaDataWithLevel, false),
                new OpeningClosingPeriodFunDef(functionMetaDataWithLevelMember, false),
                new OpeningClosingPeriodFunDef(functionMetaDataWithMember, false)));
    }
}
