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
package org.eclipse.daanse.olap.function.def.rank;

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
public class RankResolver extends AbstractFunctionDefinitionMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("Rank");
    private static String DESCRIPTION = "Returns the one-based rank of a tuple in a set.";
    private static FunctionParameterR[] tx = { new FunctionParameterR(DataType.TUPLE), new FunctionParameterR(DataType.SET) };
    private static FunctionParameterR[] txn = { new FunctionParameterR(DataType.TUPLE), new FunctionParameterR(DataType.SET),
            new FunctionParameterR(DataType.NUMERIC) };
    private static FunctionParameterR[] mx = { new FunctionParameterR(DataType.MEMBER), new FunctionParameterR(DataType.SET) };
    private static FunctionParameterR[] mxn = { new FunctionParameterR(DataType.MEMBER), new FunctionParameterR(DataType.SET),
            new FunctionParameterR(DataType.NUMERIC) };

    // {"fitx", "fitxn", "fimx", "fimxn"}


    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.INTEGER, tx);
    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.INTEGER, txn);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.INTEGER, mx);
    private static FunctionMetaData functionMetaData3 = new FunctionMetaDataR(atom, DESCRIPTION,
            DataType.INTEGER, mxn);

    public RankResolver() {
        super(List.of(new RankFunDef(functionMetaData), new RankFunDef(functionMetaData1), new RankFunDef(functionMetaData2), new RankFunDef(functionMetaData3)));
    }
}
