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
package org.eclipse.daanse.mdx.model.api.expression.operation;

/**
 * Defines a Postfix-Operation.
 * 
 * <ul>
 * <li>{@code argument OPERATOR}</li>
 * <li>{@code fun() IS EMPTY}</li>
 * </ul>
 */
public record PostfixOperationAtom(String name) implements OperationAtom {

}
