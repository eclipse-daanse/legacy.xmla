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
package org.eclipse.daanse.olap.function.def.descendants;

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
public class DescendantsMemberResolver extends AbstractFunctionDefinitionMultiResolver {
    private static String descFlagDescription = "SELF, AFTER, BEFORE, BEFORE_AND_AFTER, SELF_AND_AFTER, SELF_AND_BEFORE, SELF_BEFORE_AFTER, LEAVES";
    private static FunctionOperationAtom atom = new FunctionOperationAtom("Descendants");
    private static String SIGNATURE = "Descendants(<Member>[, <Level>[, <Desc_flag>]])";
    private static String DESCRIPTION = "Returns the set of descendants of a member at a specified level, optionally including or excluding descendants in other levels.";
    private static FunctionParameterR[] m = { new FunctionParameterR(DataType.MEMBER, "Member") };
    private static FunctionParameterR[] ml = { new FunctionParameterR(DataType.MEMBER, "Member"),
            new FunctionParameterR(DataType.LEVEL, "Level") };
    private static FunctionParameterR[] mly = { new FunctionParameterR(DataType.MEMBER, "Member"),
            new FunctionParameterR(DataType.LEVEL, "Level"), new FunctionParameterR(DataType.SYMBOL, "Desc_flag", descFlagDescription) };
    private static FunctionParameterR[] mn = { new FunctionParameterR(DataType.MEMBER, "Member"),
            new FunctionParameterR(DataType.NUMERIC) };
    private static FunctionParameterR[] mny = { new FunctionParameterR(DataType.MEMBER, "Member"),
            new FunctionParameterR(DataType.NUMERIC), new FunctionParameterR(DataType.SYMBOL, "Desc_flag", descFlagDescription) };
    private static FunctionParameterR[] mey = { new FunctionParameterR(DataType.MEMBER, "Member"),
            new FunctionParameterR(DataType.EMPTY), new FunctionParameterR(DataType.SYMBOL, "Desc_flag", descFlagDescription) };
    // {"fxm", "fxml", "fxmly", "fxmn", "fxmny", "fxmey"}

    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE, DataType.SET,
            m);
    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, ml);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, mly);
    private static FunctionMetaData functionMetaData3 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, mn);
    private static FunctionMetaData functionMetaData4 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, mny);
    private static FunctionMetaData functionMetaData5 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, mey);

    public DescendantsMemberResolver() {
        super(List.of(new DescendantsByLevelFunDef(functionMetaData), new DescendantsByLevelFunDef(functionMetaData1),
                new DescendantsByLevelFunDef(functionMetaData2), new DescendantsByLevelFunDef(functionMetaData3),
                new DescendantsByLevelFunDef(functionMetaData4), new DescendantsByLevelFunDef(functionMetaData5)));
    }
}