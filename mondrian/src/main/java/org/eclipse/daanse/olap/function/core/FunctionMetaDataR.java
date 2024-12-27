/*
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
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

package org.eclipse.daanse.olap.function.core;

import java.util.stream.Stream;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;

public record FunctionMetaDataR(OperationAtom operationAtom, String description, String signature,
        DataType returnCategory, FunctionParameterR[] parameters) implements FunctionMetaData {

    public DataType[] parameterDataTypes() {
        return Stream.of(parameters()).map(FunctionParameterR::dataType).toArray(DataType[]::new);
    }

}
