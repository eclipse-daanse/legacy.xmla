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
package org.eclipse.daanse.olap.function.def.subset;

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
public class SubsetResolver extends AbstractFunctionDefinitionMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("Subset");
    private static String DESCRIPTION = "Returns a subset of elements from a set.";
    private static FunctionParameterR[] xn = { new FunctionParameterR(DataType.SET, "Set"), new FunctionParameterR(DataType.NUMERIC, "Start") };
    private static FunctionParameterR[] xnn = { new FunctionParameterR(DataType.SET, "Set"),
            new FunctionParameterR(DataType.NUMERIC, "Start"), new FunctionParameterR(DataType.NUMERIC, "Count") };
    // {"fxxn", "fxxnn"}

    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.SET, xn);
    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.SET, xnn);

    public SubsetResolver() {
        super(List.of(new SubsetFunDef(functionMetaData), new SubsetFunDef(functionMetaData1)));
    }
}
