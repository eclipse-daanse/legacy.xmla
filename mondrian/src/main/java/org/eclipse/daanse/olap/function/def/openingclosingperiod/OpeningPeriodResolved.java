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
public class OpeningPeriodResolved extends AbstractFunctionDefinitionMultiResolver {

    private static OperationAtom atom = new FunctionOperationAtom("OpeningPeriod");
    private static String DESCRIPTION = "Returns the first descendant of a member at a level.";
    // {"fm", "fml", "fmlm"}

    private static FunctionMetaData functionMetaDataWithoutParam = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.MEMBER, new FunctionParameterR[] {});
    private static FunctionMetaData functionMetaDataWithLevel = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.MEMBER, new FunctionParameterR[] { new FunctionParameterR(DataType.LEVEL, "Level") });
    private static FunctionMetaData functionMetaDataWithLevelMember = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.MEMBER, new FunctionParameterR[] { new FunctionParameterR(DataType.LEVEL, "Level"),
                    new FunctionParameterR(DataType.MEMBER, "Member") });

    public OpeningPeriodResolved() {
        super(List.of(new OpeningClosingPeriodFunDef(functionMetaDataWithoutParam, true),
                new OpeningClosingPeriodFunDef(functionMetaDataWithLevel, true),
                new OpeningClosingPeriodFunDef(functionMetaDataWithLevelMember, true)));
    }
}
