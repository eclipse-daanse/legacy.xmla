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
 * Defines a Case-Operation.
 * 
 * <ul>
 * <li>{@code CASE WHEN true THEN <Expression> END}</li>
 * <li>{@code CASE WHEN a=1 THEN <Expression1> ELSE WHEN a>1 THEN <Expression1> END}</li>
 * </ul>
 */
public record CaseOperationAtom(String name) implements OperationAtom {

}
