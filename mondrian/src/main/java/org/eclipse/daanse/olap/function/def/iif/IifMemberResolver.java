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
package org.eclipse.daanse.olap.function.def.iif;

import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.ParametersCheckingFunctionDefinitionResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class IifMemberResolver extends ParametersCheckingFunctionDefinitionResolver {

    // IIf(<Logical Expression>, <Member Expression>, <Member Expression>)
    private static OperationAtom atom = new InfixOperationAtom("IIf");
    private static String DESCRIPTION = "Returns one of two member values determined by a logical test.";
    private static String SIGNATURE = "IIf(<LOGICAL>, <MEMBER>, <MEMBER>)";

    private static FunctionParameterR[] params = new FunctionParameterR[] {
            new FunctionParameterR(DataType.LOGICAL, "Condition"), new FunctionParameterR(DataType.MEMBER, "Member1"),
            new FunctionParameterR(DataType.MEMBER, "Member2") };
    static FunctionMetaData metadata = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE, DataType.MEMBER, params);

    public IifMemberResolver() {
        super(new IifFunDef(metadata));
    }
}
