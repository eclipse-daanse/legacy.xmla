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

package org.eclipse.daanse.olap.calc.base.type.string;

import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedStringCalc;

import mondrian.olap.Evaluator;
import mondrian.olap.type.Type;

public class UnknownToStringCalc extends AbstractProfilingNestedStringCalc<Calc<?>> {

	public UnknownToStringCalc(Type type, Calc<?> calc) {
		super(type, new Calc[] { calc });
	}

	@Override
	public String evaluate(Evaluator evaluator) {

		Object o = getFirstChildCalc().evaluate(evaluator);

		if (o != null) {
			return o.toString();
		}
		return null;
	}
}