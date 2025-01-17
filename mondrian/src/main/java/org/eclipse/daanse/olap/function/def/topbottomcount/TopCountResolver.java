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
package org.eclipse.daanse.olap.function.def.topbottomcount;

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
public class TopCountResolver extends AbstractFunctionDefinitionMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("TopCount");
    private static String DESCRIPTION = "Returns a specified number of items from the top of a set, optionally ordering the set first.";
    private static FunctionParameterR[] xnn = { new FunctionParameterR(DataType.SET),
            new FunctionParameterR(DataType.NUMERIC, "Count"), new FunctionParameterR(DataType.NUMERIC) };
    private static FunctionParameterR[] xn = { new FunctionParameterR(DataType.SET),
            new FunctionParameterR(DataType.NUMERIC, "Count") };
    // {"fxxnn", "fxxn"}

    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION, DataType.SET,
            xnn);
    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.SET, xn);

    public TopCountResolver() {
        super(List.of(new TopBottomCountFunDef(functionMetaData, true),
                new TopBottomCountFunDef(functionMetaData1, true)));
    }
}
