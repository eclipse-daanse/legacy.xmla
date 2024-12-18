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
package org.eclipse.daanse.olap.function.def.drilldownleveltopbottom;

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
public class DrilldownLevelTopResolver extends AbstractFunctionDefinitionMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("DrilldownLevelTop");
    private static String SIGNATURE = "DrilldownLevelTop(Set_Expression, Count [, [Level_Expression][, Numeric_Expression]])";
    private static String DESCRIPTION = "Drills down the topmost members of a set, at a specified level, to one level below.";
    private static FunctionParameterR[] xn = { new FunctionParameterR(DataType.SET, "Set"),
            new FunctionParameterR(DataType.NUMERIC) };
    private static FunctionParameterR[] xnl = { new FunctionParameterR(DataType.SET, "Set"),
            new FunctionParameterR(DataType.NUMERIC), new FunctionParameterR(DataType.LEVEL, "Level") };
    private static FunctionParameterR[] xnln = { new FunctionParameterR(DataType.SET, "Set"),
            new FunctionParameterR(DataType.NUMERIC), new FunctionParameterR(DataType.LEVEL),
            new FunctionParameterR(DataType.NUMERIC) };
    private static FunctionParameterR[] xnen = { new FunctionParameterR(DataType.SET, "Set"),
            new FunctionParameterR(DataType.NUMERIC), new FunctionParameterR(DataType.EMPTY),
            new FunctionParameterR(DataType.NUMERIC) };
    // {"fxxn", "fxxnl", "fxxnln", "fxxnen"}

    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, xn);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, xnl);
    private static FunctionMetaData functionMetaData3 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, xnln);
    private static FunctionMetaData functionMetaData4 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, xnen);

    public DrilldownLevelTopResolver() {
        super(List.of(new DrilldownLevelTopBottomFunDef(functionMetaData1, true),
                new DrilldownLevelTopBottomFunDef(functionMetaData2, true),
                new DrilldownLevelTopBottomFunDef(functionMetaData3, true),
                new DrilldownLevelTopBottomFunDef(functionMetaData4, true)));
    }
}