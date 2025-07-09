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
package org.eclipse.daanse.olap.fun;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;

public record FunctionAtomCompareKey(String name, Class<?> clazz) {

    public FunctionAtomCompareKey(String name, Class<?> clazz) {
        this.name = name.toUpperCase();
        this.clazz = clazz;
    }

    public FunctionAtomCompareKey(OperationAtom functionAtom) {
        this(functionAtom.name(), functionAtom.getClass());
    }

}
