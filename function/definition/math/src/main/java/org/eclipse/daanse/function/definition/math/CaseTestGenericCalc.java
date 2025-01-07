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
package org.eclipse.daanse.function.definition.math;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.BooleanCalc;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedUnknownCalc;

class CaseTestGenericCalc extends AbstractProfilingNestedUnknownCalc {
	private Map<BooleanCalc, Calc<?>> calcPairs;
	final Calc<?> defaultCalc;

	public CaseTestGenericCalc(Type type, Map<BooleanCalc, Calc<?>> calcPairs, Calc<?> defaultCalc,
			Calc<?>[] allCalcsArray) {
		super(type, allCalcsArray);
		this.calcPairs = calcPairs;
		this.defaultCalc = defaultCalc;
	}

	@Override
	public Object evaluate(Evaluator evaluator) {
		for (Entry<BooleanCalc, Calc<?>> entry : calcPairs.entrySet()) {
			if (entry.getKey().evaluate(evaluator)) {
				return entry.getValue().evaluate(evaluator);
			}
		}
		return defaultCalc.evaluate(evaluator);
	}
}