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
package org.eclipse.daanse.olap.function.def.dimension.current;

import org.eclipse.daanse.olap.function.core.resolver.ParametersCheckingFunctionDefinitionResolver;

//@Component(service = FunctionResolver.class)
public class CurrentResolver extends ParametersCheckingFunctionDefinitionResolver {

	public CurrentResolver() {
		super(new CurrentFunDef());
	}

}
