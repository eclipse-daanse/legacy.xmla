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
package org.eclipse.daanse.olap.function.def.logical;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractFunctionDefinitionMultiResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class IsResolver extends AbstractFunctionDefinitionMultiResolver {

    private static String DESCRIPTION = "Returns whether two objects are the same";
    private static String SIGNATURE = "<Expression> IS <Expression>";
    private static OperationAtom atom = new InfixOperationAtom("IS");
    //{"ibmm", "ibll", "ibhh", "ibdd", "ibtt"}

    private static FunctionMetaData functionMetaDataWithMember = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.LOGICAL, new DataType[] { DataType.MEMBER, DataType.MEMBER });

    private static FunctionMetaData functionMetaDataWithLevel = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.LOGICAL, new DataType[] { DataType.LEVEL, DataType.LEVEL});

    private static FunctionMetaData functionMetaDataWithHierrchy = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.LOGICAL, new DataType[] { DataType.HIERARCHY, DataType.HIERARCHY });

    private static FunctionMetaData functionMetaDataWithDimension = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.LOGICAL, new DataType[] { DataType.DIMENSION, DataType.DIMENSION });

    private static FunctionMetaData functionMetaDataWithTuple = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.LOGICAL, new DataType[] { DataType.TUPLE, DataType.TUPLE });

    public IsResolver() {
        super(List.of(new IsFunDef(functionMetaDataWithMember), new IsFunDef(functionMetaDataWithLevel),
                new IsFunDef(functionMetaDataWithHierrchy), new IsFunDef(functionMetaDataWithDimension), new IsFunDef(functionMetaDataWithTuple)));
    }

}
