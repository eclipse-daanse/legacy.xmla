/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.query.component;

import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.query.component.expression.AbstractExpression;

public abstract class AbstractLiteralImpl<R> extends AbstractExpression {

	private final R value;

	protected AbstractLiteralImpl(R value) {
		this.value = value;
	}

	@Override
	public AbstractLiteralImpl<R> cloneExp() {
		return this;
	}

	@Override
	public Expression accept(Validator validator) {
		return this;
	}

	public R getValue() {
		return value;
	}

}
