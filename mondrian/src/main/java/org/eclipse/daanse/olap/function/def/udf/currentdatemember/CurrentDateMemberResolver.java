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
package org.eclipse.daanse.olap.function.def.udf.currentdatemember;

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
public class CurrentDateMemberResolver extends AbstractFunctionDefinitionMultiResolver {
    private final static FunctionOperationAtom atom = new FunctionOperationAtom("CurrentDateMember");
    private final static String SIGNATURE = "CurrentDateMember(<String>)";
    private final static String DESCRIPTION = """
            Returns the closest or exact member within the specified
            dimension corresponding to the current date, in the format
            specified by the format parameter.
            Format strings are the same as used by the MDX Format function,
            namely the Visual Basic format strings.
            See http://www.apostate.com/programming/vb-format.html.""";

    private final static String DESCRIPTION1 = """
            Returns the exact member within the specified dimension
            corresponding to the current date, in the format specified by
            the format parameter.
            If there is no such date, returns the NULL member.
            Format strings are the same as used by the MDX Format function,
            namely the Visual Basic format strings.
            See http://www.apostate.com/programming/vb-format.html.""";

    private static FunctionParameterR[] fp = { new FunctionParameterR(DataType.HIERARCHY),
            new FunctionParameterR(DataType.STRING), new FunctionParameterR(DataType.SYMBOL) };
    private static FunctionParameterR[] fp1 = { new FunctionParameterR(DataType.HIERARCHY),
            new FunctionParameterR(DataType.STRING) };

    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.MEMBER, fp);
    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION1, SIGNATURE,
            DataType.MEMBER, fp1);

    @Override
    public List<String> getReservedWords() {
        return List.of("EXACT", "BEFORE", "AFTER");
    }

    public CurrentDateMemberResolver() {
        super(List.of(new CurrentDateMemberFunDef(functionMetaData), new CurrentDateMemberFunDef(functionMetaData1)));
    }

}