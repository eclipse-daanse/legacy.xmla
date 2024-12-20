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
package org.eclipse.daanse.olap.function.def.set.setitem;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractFunctionDefinitionMultiResolver;

public class SetItemIntResolver extends AbstractFunctionDefinitionMultiResolver {
    private static MethodOperationAtom atom = new MethodOperationAtom("Item");
    private static String SIGNATURE = "<Set>.Item(<Index>)";
    private static String DESCRIPTION = "Returns a tuple from the set specified in <Set>. The tuple to be returned is specified by the zero-based position of the tuple in the set in <Index>.";
    private static FunctionParameterR[] xn = { new FunctionParameterR(DataType.SET, "Set"), new FunctionParameterR(DataType.NUMERIC, "Index") };
    // {"mmxn"}


    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.MEMBER, xn);

    public SetItemIntResolver() {
        super(List.of(new SetItemFunDef(functionMetaData)));
    }
}
