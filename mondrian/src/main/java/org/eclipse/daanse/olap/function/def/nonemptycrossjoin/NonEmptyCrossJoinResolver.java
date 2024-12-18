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
package org.eclipse.daanse.olap.function.def.nonemptycrossjoin;

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
public class NonEmptyCrossJoinResolver extends AbstractFunctionDefinitionMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("NonEmptyCrossJoin");
    private static String SIGNATURE = "NonEmptyCrossJoin(<Set1>, <Set2>)";
    private static String DESCRIPTION = "Returns the cross product of two sets, excluding empty tuples and tuples without associated fact table data.";
    private static FunctionParameterR set1 = new FunctionParameterR(DataType.SET, "Set1");
    private static FunctionParameterR set2 = new FunctionParameterR(DataType.SET, "Set2");
    private static FunctionParameterR[] xx = { set1, set2};
    // {"fxxx"}
    
    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, xx);

    public NonEmptyCrossJoinResolver() {
        super(List.of(new NonEmptyCrossJoinFunDef(functionMetaData)));
    }
}