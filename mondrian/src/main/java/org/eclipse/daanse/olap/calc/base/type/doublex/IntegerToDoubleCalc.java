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
package org.eclipse.daanse.olap.calc.base.type.doublex;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;

import mondrian.olap.fun.FunUtil;

public class IntegerToDoubleCalc extends AbstractProfilingNestedDoubleCalc {

	public IntegerToDoubleCalc(Type type, IntegerCalc integerCalc) {
		super(type, integerCalc);
	}

	@Override
	public Double evaluate(Evaluator evaluator) {

		Integer i = getChildCalc(0, IntegerCalc.class).evaluate(evaluator);
		if (i == null) {
			return FunUtil.DOUBLE_NULL;
			// null;
			// TODO: !!! JUST REFACTORING 0 must be null
		}
		return i.doubleValue();
	}
}