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
package org.eclipse.daanse.olap.function.def.generate;

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
public class GenerateStringResolver extends AbstractFunctionDefinitionMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("Generate");
    private static String SIGNATURE = "Generate(<Set>, <String>[, <String>])";
    private static String DESCRIPTION = "Applies a set to a string expression and joins resulting sets by string concatenation.";
    private static FunctionParameterR[] xS = { new FunctionParameterR(DataType.SET, "Set"),
            new FunctionParameterR(DataType.STRING) };
    private static FunctionParameterR[] xSS = { new FunctionParameterR(DataType.SET, "Set"),
            new FunctionParameterR(DataType.STRING), new FunctionParameterR(DataType.STRING) };
    private static FunctionParameterR[] xnS = { new FunctionParameterR(DataType.SET, "Set"),
            new FunctionParameterR(DataType.NUMERIC), new FunctionParameterR(DataType.STRING) };
    // {"fSxS", "fSxSS", "fSxnS"}


    
    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.STRING, xS);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.STRING, xSS);
    private static FunctionMetaData functionMetaData3 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.STRING, xnS);

    public GenerateStringResolver() {
        super(List.of(new GenerateFunDef(functionMetaData1), new GenerateFunDef(functionMetaData2), new GenerateFunDef(functionMetaData3)));
    }
}