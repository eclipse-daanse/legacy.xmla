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
package org.eclipse.daanse.olap.function.core.resolver;

import java.util.List;

import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;

public class AbstractFunctionDefinitionMultiResolver extends AbstractMetaDataMultiResolver {

	private List<FunctionDefinition> functionDefinitions;

	public AbstractFunctionDefinitionMultiResolver(List<FunctionDefinition> functionDefinitions) {
		super(functionDefinitions.stream().map(FunctionDefinition::getFunctionMetaData).toList());
		this.functionDefinitions = functionDefinitions;
	}

	@Override
	protected FunctionDefinition createFunDef(Expression[] args, FunctionMetaData functionMetaData,
			FunctionMetaData fmdTarget) {

		if (functionMetaData == null) {
			return null;
		}
		return functionDefinitions.stream().filter(fd -> fd.getFunctionMetaData().equals(functionMetaData)).findAny()
				.orElse(null);
	}

}
