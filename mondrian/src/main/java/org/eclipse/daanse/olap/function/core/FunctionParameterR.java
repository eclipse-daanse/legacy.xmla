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
*/
package org.eclipse.daanse.olap.function.core;

import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionParameter;

public record FunctionParameterR(DataType dataType, Optional<String> name, Optional<String> description, Optional<List<String>> reservedWords) implements FunctionParameter{

	public FunctionParameterR(DataType dataType) {
		this(dataType, Optional.empty(), Optional.empty(), Optional.empty());
	}

	public FunctionParameterR(DataType dataType, String name) {
		this(dataType, Optional.ofNullable(name), Optional.empty(), Optional.empty());
	}

	public FunctionParameterR(DataType dataType, String name, String description) {
		this(dataType, Optional.ofNullable(name), Optional.ofNullable(description), Optional.empty());
	}
	
    public FunctionParameterR(DataType dataType, String name, Optional<List<String>> reservedWords) {
        this(dataType, Optional.ofNullable(name), Optional.empty(), reservedWords);
    }

}
