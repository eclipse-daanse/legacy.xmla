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
 * Defines the Empty Operation.
 * 
 * Used for an empty expression. Empty expressions can occur within function
 * calls, and are denoted by a pair of commas with only whitespace between them,
 * for example
 *
 * <blockquote> <code>DrillDownLevelTop({[Product].[All Products]}, 3, ,
 *  [Measures].[Unit Sales])</code> </blockquote>
 */
public record EmptyOperationAtom() implements OperationAtom {

	public static final String NAME = "";

	@Override
	public String name() {
		return NAME;
	}
}
