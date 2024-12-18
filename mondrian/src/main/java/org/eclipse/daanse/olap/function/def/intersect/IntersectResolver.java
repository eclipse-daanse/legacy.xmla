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
package org.eclipse.daanse.olap.function.def.intersect;

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
public class IntersectResolver extends AbstractFunctionDefinitionMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("Intersect");
    private static String SIGNATURE = "Intersect(<Set1>, <Set2>[, ALL])";
    private static String DESCRIPTION = "Returns the intersection of two input sets, optionally retaining duplicates.";
    private static FunctionParameterR[] xxy = { new FunctionParameterR(DataType.SET, "Set1"),
            new FunctionParameterR(DataType.SET, "Set2"), new FunctionParameterR(DataType.SYMBOL, "All") };
    private static FunctionParameterR[] xx = { new FunctionParameterR(DataType.SET, "Set1"),
            new FunctionParameterR(DataType.SET, "Set2") };
    // {"fxxxy", "fxxx"}

    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, xxy);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, xx);

    @Override
    public List<String> getReservedWords() {
        return List.of("ALL");
    }

    
    public IntersectResolver() {
        super(List.of(new IntersectFunDef(functionMetaData1), new IntersectFunDef(functionMetaData2)));
    }
}