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
import java.util.Optional;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractFunctionDefinitionMultiResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class DescendantsSetResolver extends AbstractFunctionDefinitionMultiResolver {
    private static String descFlagDescription = "SELF, AFTER, BEFORE, BEFORE_AND_AFTER, SELF_AND_AFTER, SELF_AND_BEFORE, SELF_BEFORE_AFTER, LEAVES";
    private static FunctionOperationAtom atom = new FunctionOperationAtom("Descendants");
    private static String DESCRIPTION = "Returns the set of descendants of a set of members at a specified level, optionally including or excluding descendants in other levels.";
    private static FunctionParameterR[] x = { new FunctionParameterR(DataType.SET) };
    private static FunctionParameterR[] xl = { new FunctionParameterR(DataType.SET),
            new FunctionParameterR(DataType.LEVEL) };
    private static FunctionParameterR[] xly = { new FunctionParameterR(DataType.SET),
            new FunctionParameterR(DataType.LEVEL), new FunctionParameterR(DataType.SYMBOL, Optional.of("Desc_flag"),
                    Optional.of(descFlagDescription), Optional.of(Flag.asReservedWords())) };
    private static FunctionParameterR[] xn = { new FunctionParameterR(DataType.SET),
            new FunctionParameterR(DataType.NUMERIC) };
    private static FunctionParameterR[] xny = { new FunctionParameterR(DataType.SET),
            new FunctionParameterR(DataType.NUMERIC), new FunctionParameterR(DataType.SYMBOL, "Desc_flag", descFlagDescription) };
    private static FunctionParameterR[] xey = { new FunctionParameterR(DataType.SET),
            new FunctionParameterR(DataType.EMPTY), new FunctionParameterR(DataType.SYMBOL, "Desc_flag", descFlagDescription) };
    // {"fxx", "fxxl", "fxxly", "fxxn", "fxxny", "fxxey"}

    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION, DataType.SET,
            x);
    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.SET, xl);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.SET, xly);
    private static FunctionMetaData functionMetaData3 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.SET, xn);
    private static FunctionMetaData functionMetaData4 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.SET, xny);
    private static FunctionMetaData functionMetaData5 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.SET, xey);

    @Override
    public List<String> getReservedWords() {
        return Flag.asReservedWords();
    }


    public DescendantsSetResolver() {
        super(List.of(new DescendantsByLevelFunDef(functionMetaData), new DescendantsByLevelFunDef(functionMetaData1),
                new DescendantsByLevelFunDef(functionMetaData2), new DescendantsByLevelFunDef(functionMetaData3),
                new DescendantsByLevelFunDef(functionMetaData4), new DescendantsByLevelFunDef(functionMetaData5)));
    }
}
