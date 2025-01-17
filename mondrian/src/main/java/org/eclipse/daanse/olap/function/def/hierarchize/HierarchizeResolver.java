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
package org.eclipse.daanse.olap.function.def.hierarchize;

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
public class HierarchizeResolver extends AbstractFunctionDefinitionMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("Hierarchize");
    private static List<String> reservedWords = List.of( "PRE", "POST" );
    private static String DESCRIPTION = "Orders the members of a set in a hierarchy.";
    private static FunctionParameterR[] x = { new FunctionParameterR(DataType.SET) };
    private static FunctionParameterR[] xy = { new FunctionParameterR(DataType.SET),
            new FunctionParameterR(DataType.SYMBOL, "PrePost", Optional.of(reservedWords)) };
    // {"fxx", "fxxy"}

    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.SET, x);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.SET, xy);

    public HierarchizeResolver() {
        super(List.of(new HierarchizeFunDef(functionMetaData1), new HierarchizeFunDef(functionMetaData2)));
    }

    @Override
    public List<String> getReservedWords() {
        return reservedWords;
    }

}
