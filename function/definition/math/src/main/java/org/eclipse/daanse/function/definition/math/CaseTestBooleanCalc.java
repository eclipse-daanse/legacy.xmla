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
import org.eclipse.daanse.olap.calc.api.BooleanCalc;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedBooleanCalc;

import mondrian.olap.type.BooleanType;

public class CaseTestBooleanCalc extends AbstractProfilingNestedBooleanCalc {

	private Map<BooleanCalc, Calc<?>> calcPairs;
	final Calc<?> defaultCalc;

	public CaseTestBooleanCalc(Map<BooleanCalc, Calc<?>> calcPairs, Calc<?> defaultCalc, Calc<?>[] allCalcsArray) {
		super(BooleanType.INSTANCE, allCalcsArray);
		this.calcPairs = calcPairs;
		this.defaultCalc = defaultCalc;
	}

	@Override
	public Boolean evaluate(Evaluator evaluator) {
		for (Entry<BooleanCalc, Calc<?>> entry : calcPairs.entrySet()) {
			if (entry.getKey().evaluate(evaluator)) {
				return (Boolean) entry.getValue().evaluate(evaluator);
			}
		}
		return (Boolean) defaultCalc.evaluate(evaluator);
	}
}