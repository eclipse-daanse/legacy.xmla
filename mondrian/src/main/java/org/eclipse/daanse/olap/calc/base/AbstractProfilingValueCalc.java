/*
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.calc.base;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.type.Type;

public abstract class AbstractProfilingValueCalc<T> extends AbstractProfilingCalc<T> {

	public AbstractProfilingValueCalc(Type type) {
		super(type);
	}

	@Override
	public T evaluate(Evaluator evaluator) {
		return convertCurrentValue(evaluator.evaluateCurrent());
	}

	protected abstract T convertCurrentValue(Object evaluateCurrent);

	@Override
	public boolean dependsOn(Hierarchy hierarchy) {
		return true;
	}

	@Override
	public ResultStyle getResultStyle() {
		return ResultStyle.VALUE;
	}

}
