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
package org.eclipse.daanse.olap.function.def.topbottompercentsum;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.ParametersCheckingFunctionDefinitionResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class BottomPercentResolver extends ParametersCheckingFunctionDefinitionResolver {

    static final OperationAtom atomBottomPercent = new FunctionOperationAtom("BottomPercent");
    private static String SIGNATURE = "BottomPercent(<Set>, <Percentage>, <Numeric Expression>)";
    private static String DESCRIPTION = "Sorts a set and returns the bottom N elements whose cumulative total is at least a specified percentage.";
    private static FunctionParameterR[] params = { new FunctionParameterR(DataType.SET),
            new FunctionParameterR(DataType.NUMERIC), new FunctionParameterR(DataType.NUMERIC) };

    static final FunctionMetaData fmdBottomPercent = new FunctionMetaDataR(atomBottomPercent, DESCRIPTION, SIGNATURE,
            DataType.SET, params);

    public BottomPercentResolver() {
        super(new TopBottomPercentSumFunDef(fmdBottomPercent, false, true));
    }

}