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
package org.eclipse.daanse.olap.function.def.member.defaultmember;

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.NonFunctionResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class NonFunctionDefaultMemberResolver extends NonFunctionResolver {

    public NonFunctionDefaultMemberResolver() {
        // "<Dimension>.DefaultMember". The function is implemented using an
        // implicit cast to hierarchy, and we create a FunInfo for
        // documentation & backwards compatibility.
        super(new FunctionMetaDataR(new PlainPropertyOperationAtom("DefaultMember"), "Returns the default member of a dimension.",
                DataType.MEMBER, new FunctionParameterR[] { new FunctionParameterR(DataType.DIMENSION) }));
    }

}
