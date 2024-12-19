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
package org.eclipse.daanse.olap.function.def.union;

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
public class UnionResolver extends AbstractFunctionDefinitionMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("Union");
    private static String SIGNATURE = "Union(<Set1>, <Set2>[, ALL])";
    private static String DESCRIPTION = "Returns the union of two sets, optionally retaining duplicates.";
    private static FunctionParameterR[] xx = { new FunctionParameterR(DataType.SET, "Set1"), new FunctionParameterR(DataType.SET, "Set2") };
    private static FunctionParameterR[] xxy = { new FunctionParameterR(DataType.SET, "Set1"), new FunctionParameterR(DataType.SET, "Set2"),
            new FunctionParameterR(DataType.SYMBOL, "ALL") };
    // {"fxxx", "fxxxy"}

    static final List<String> ReservedWords = List.of("ALL", "DISTINCT");


    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, xx);
    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, xxy);

    public UnionResolver() {
        super(List.of(new UnionFunDef(functionMetaData), new UnionFunDef(functionMetaData1)));
    }
    
    @Override
    public List<String> getReservedWords() {
        return ReservedWords;
    }

}