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
package org.eclipse.daanse.olap.function.def.except;

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
public class ExceptResolver extends AbstractFunctionDefinitionMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("Except");
    
    private static List<String> reservedWords = List.of("ALL");
    private static String DESCRIPTION = "Finds the difference between two sets, optionally retaining duplicates.";
    private static FunctionParameterR[] xx = { new FunctionParameterR(DataType.SET, "Set1"),
            new FunctionParameterR(DataType.SET, "Set2") };
    private static FunctionParameterR[] xxy = { new FunctionParameterR(DataType.SET, "Set1"),
            new FunctionParameterR(DataType.SET, "Set2"), new FunctionParameterR(DataType.SYMBOL, "All", Optional.of(reservedWords) ) };
    // {"fxxx", "fxxxy"}

    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.SET, xx);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.SET, xxy);

    public ExceptResolver() {
        super(List.of(new ExceptFunDef(functionMetaData1), new ExceptFunDef(functionMetaData2)));
    }
    
    @Override
    public List<String> getReservedWords() {
        return reservedWords;
    }
}