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

package org.eclipse.daanse.olap.calc.base.type.integer;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedIntegerCalc;

public class DoubleToIntegerCalc extends AbstractProfilingNestedIntegerCalc {

	public DoubleToIntegerCalc(Type type, DoubleCalc doubleCalc) {
		super(type, doubleCalc);
	}

	@Override
	public Integer evaluate(Evaluator evaluator) {
		Double d = getChildCalc(0, DoubleCalc.class).evaluate(evaluator);
		if (d == null) {
			return null;
		}
		return d.intValue();
	}
}