/*
* Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.function.def.vba.lcase;

import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.function.core.resolver.ParametersCheckingFunctionDefinitionResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class LCaseResolver extends ParametersCheckingFunctionDefinitionResolver {

    public LCaseResolver() {
        super(new LCaseFunDef());
    }
}
